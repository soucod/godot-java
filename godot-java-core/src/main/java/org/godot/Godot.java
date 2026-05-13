package org.godot;

import org.godot.internal.api.ApiIndex;
import org.godot.bridge.Bridge;
import org.godot.collection.GodotArray;
import org.godot.core.GodotStringName;
import org.godot.core.Variant;
import org.godot.core.VariantUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class Godot {

	public static final int NOTIFICATION_ACCESSIBILITY_UPDATE = 3000;
	public static final int NOTIFICATION_ACCESSIBILITY_INVALIDATE = 3001;

	private static final Logger logger = LogManager.getLogger(Godot.class);

	/** Cache: "className::methodName" → MethodBind pointer. Never evicted. */
	private static final ConcurrentHashMap<String, MemorySegment> METHOD_BIND_CACHE = new ConcurrentHashMap<>();

	private static MemorySegment getCachedMethodBind(String className, String methodName, long hash) {
		String key = className + "::" + methodName;
		return METHOD_BIND_CACHE.computeIfAbsent(key, k -> {
			GodotStringName methodSn = GodotStringName.fromJavaString(methodName);
			GodotStringName classNameSn = GodotStringName.fromJavaString(className);
			return Bridge.callPtr2S1L(ApiIndex.CLASSDB_GET_METHOD_BIND, classNameSn.segment(), methodSn.segment(),
					hash);
		});
	}

	protected volatile long nativeObject;

	private volatile boolean valid = true;

	protected Godot(long nativeObject) {
		this.nativeObject = nativeObject;
	}

	protected Godot(MemorySegment nativeSegment) {
		this(nativeSegment.address());
	}

	protected Godot() {
		this.nativeObject = 0;
	}

	public long getPtr() {
		return nativeObject;
	}

	public final long getNativeObject() {
		return nativeObject;
	}

	public final void setNativeObject(long ptr) {
		this.nativeObject = ptr;
	}

	public boolean isValid() {
		return valid && nativeObject != 0;
	}

	public void invalidate() {
		this.valid = false;
	}

	protected void checkValid() {
		if (!valid || nativeObject == 0) {
			throw new org.godot.exception.GodotInvalidObjectException(getClass().getSimpleName()
					+ " has been freed (nativeObject=0x" + Long.toHexString(nativeObject) + ")");
		}
	}

	// ----------------------------------------------------------------
	// Method hash resolution
	// ----------------------------------------------------------------

	public static final class HashResult {
		static final HashResult NOT_FOUND = new HashResult(0, null);
		public final long hash;
		public final String className;

		public HashResult(long hash, String className) {
			this.hash = hash;
			this.className = className;
		}

		boolean isFound() {
			return hash != 0;
		}
	}

	protected HashResult resolveMethodHash(String methodName) {
		return HashResult.NOT_FOUND;
	}

	protected String getGodotClassName() {
		return null;
	}

	// ----------------------------------------------------------------
	// Method call
	// ----------------------------------------------------------------

	public java.lang.Object call(String methodName, java.lang.Object... args) {
		checkValid();

		if ("get_instance_id".equals(methodName) && args.length == 0) {
			return Bridge.runDowncall(
					() -> Bridge.callLong(ApiIndex.OBJECT_GET_INSTANCE_ID, MemorySegment.ofAddress(nativeObject)));
		}

		return Bridge.runDowncall(() -> Bridge.runScoped(() -> {
			int depth = Bridge.callDepth();
			if (depth >= Bridge.MAX_CALL_DEPTH) {
				throw new RuntimeException("Maximum call depth exceeded: " + depth);
			}
			int argc = args.length;
			if (argc > Bridge.MAX_CALL_ARGS) {
				throw new RuntimeException("Too many arguments: " + argc + " (max " + Bridge.MAX_CALL_ARGS + ")");
			}

			try {
				// Destroy previous result at this depth to release RefCounted refs.
				// CALL_FRAMES is zero-initialized, so first call is a no-op.
				Bridge.destroyVariant(Bridge.resultSlot(depth));
				MemorySegment argPtrs;
				if (argc > 0) {
					argPtrs = Bridge.argPtrsSlot(depth);
					for (int i = 0; i < argc; i++) {
						MemorySegment slot = Bridge.argSlot(depth, i);
						VariantUtils.fromObjectInto(args[i], slot);
						argPtrs.set(ADDRESS, (long) i * ADDRESS.byteSize(), slot);
					}
				} else {
					argPtrs = MemorySegment.NULL;
				}

				MemorySegment resultVar = Bridge.resultSlot(depth);
				MemorySegment errorVar = Bridge.errorSlot(depth);

				return Bridge.withCallDepth(depth, () -> {
					HashResult hashResult = resolveMethodHash(methodName);
					if (hashResult.isFound()) {
						MemorySegment methodBind = getCachedMethodBind(hashResult.className, methodName,
								hashResult.hash);
						if (methodBind.address() == 0) {
							throw new RuntimeException(
									"Method bind not found: " + methodName + " on " + hashResult.className);
						}
						Bridge.callVoid(ApiIndex.OBJECT_METHOD_BIND_CALL, methodBind,
								MemorySegment.ofAddress(nativeObject), argPtrs, (long) argc, resultVar, errorVar);
					} else {
						GodotStringName methodSn = GodotStringName.fromJavaString(methodName);
						Bridge.callVoid(ApiIndex.OBJECT_CALL_SCRIPT_METHOD, MemorySegment.ofAddress(nativeObject),
								methodSn.segment(), argPtrs, (long) argc, resultVar, errorVar);
					}

					int errorCode = errorVar.get(JAVA_INT, 0);
					if (errorCode != 0) {
						StringBuilder sb = new StringBuilder();
						sb.append("Call error ").append(errorCode).append(" calling ").append(methodName);
						sb.append(" (arg=").append(errorVar.get(JAVA_INT, 4));
						sb.append(", expected=").append(errorVar.get(JAVA_INT, 8)).append(")");
						throw new RuntimeException(sb.toString());
					}

					Variant resultVariant = new Variant(resultVar);
					return VariantUtils.toObject(resultVariant);
				});
			} finally {
				for (int i = 0; i < argc; i++) {
					Bridge.destroyVariant(Bridge.argSlot(depth, i));
				}
			}
		}));
	}

	/**
	 * Call a method on this object at the end of the current frame (deferred).
	 * Equivalent to GDScript's call_deferred().
	 */
	public void callDeferred(String methodName, java.lang.Object... args) {
		java.lang.Object[] combined = new java.lang.Object[(args != null ? args.length : 0) + 1];
		combined[0] = methodName;
		if (args != null && args.length > 0) {
			System.arraycopy(args, 0, combined, 1, args.length);
		}
		call("call_deferred", combined);
	}

	protected java.lang.Object callEngine(String className, String methodName, long hash, java.lang.Object... args) {
		checkValid();
		return callMethodBind(className, methodName, hash, MemorySegment.ofAddress(nativeObject), args);
	}

	protected boolean callEngineBool(String className, String methodName, long hash, Object... typedArgs) {
		return callEnginePtr(className, methodName, hash, 1, ret -> ret.get(JAVA_BYTE, 0) != 0, typedArgs);
	}

	protected int callEngineInt32(String className, String methodName, long hash, Object... typedArgs) {
		return callEnginePtr(className, methodName, hash, 4, ret -> ret.get(JAVA_INT, 0), typedArgs);
	}

	protected long callEngineInt64(String className, String methodName, long hash, Object... typedArgs) {
		return callEnginePtr(className, methodName, hash, 8, ret -> ret.get(JAVA_LONG, 0), typedArgs);
	}

	protected float callEngineFloat32(String className, String methodName, long hash, Object... typedArgs) {
		return callEnginePtr(className, methodName, hash, 4, ret -> ret.get(JAVA_FLOAT, 0), typedArgs);
	}

	protected double callEngineFloat64(String className, String methodName, long hash, Object... typedArgs) {
		return callEnginePtr(className, methodName, hash, 8, ret -> ret.get(JAVA_DOUBLE, 0), typedArgs);
	}

	/**
	 * Call a static method on a Godot engine class. Uses the method bind with a
	 * null object pointer, which is how the GDExtension API dispatches static
	 * methods.
	 */
	protected static java.lang.Object callStatic(String className, String methodName, long hash,
			java.lang.Object... args) {
		return callMethodBind(className, methodName, hash, MemorySegment.NULL, args);
	}

	protected static boolean callStaticBool(String className, String methodName, long hash, Object... typedArgs) {
		return callStaticPtr(className, methodName, hash, 1, ret -> ret.get(JAVA_BYTE, 0) != 0, typedArgs);
	}

	protected static int callStaticInt32(String className, String methodName, long hash, Object... typedArgs) {
		return callStaticPtr(className, methodName, hash, 4, ret -> ret.get(JAVA_INT, 0), typedArgs);
	}

	protected static long callStaticInt64(String className, String methodName, long hash, Object... typedArgs) {
		return callStaticPtr(className, methodName, hash, 8, ret -> ret.get(JAVA_LONG, 0), typedArgs);
	}

	protected static float callStaticFloat32(String className, String methodName, long hash, Object... typedArgs) {
		return callStaticPtr(className, methodName, hash, 4, ret -> ret.get(JAVA_FLOAT, 0), typedArgs);
	}

	protected static double callStaticFloat64(String className, String methodName, long hash, Object... typedArgs) {
		return callStaticPtr(className, methodName, hash, 8, ret -> ret.get(JAVA_DOUBLE, 0), typedArgs);
	}

	private <T> T callEnginePtr(String className, String methodName, long hash, long returnSize,
			Function<MemorySegment, T> reader, Object... typedArgs) {
		checkValid();
		return callMethodBindPtr(className, methodName, hash, MemorySegment.ofAddress(nativeObject), returnSize, reader,
				typedArgs);
	}

	private static <T> T callStaticPtr(String className, String methodName, long hash, long returnSize,
			Function<MemorySegment, T> reader, Object... typedArgs) {
		return callMethodBindPtr(className, methodName, hash, MemorySegment.NULL, returnSize, reader, typedArgs);
	}

	private static <T> T callMethodBindPtr(String className, String methodName, long hash, MemorySegment object,
			long returnSize, Function<MemorySegment, T> reader, Object... typedArgs) {
		return Bridge.runDowncall(() -> Bridge.runScoped(() -> {
			MemorySegment methodBind = getCachedMethodBind(className, methodName, hash);
			if (methodBind.address() == 0) {
				throw new RuntimeException("Method bind not found: " + methodName + " on " + className);
			}

			MemorySegment argPtrs = typedArgPtrs(typedArgs);
			MemorySegment ret = Bridge.allocate(returnSize);
			Bridge.callVoid(ApiIndex.OBJECT_METHOD_BIND_PTRCALL, methodBind, object, argPtrs, ret);
			return reader.apply(ret);
		}));
	}

	private static MemorySegment typedArgPtrs(Object[] typedArgs) {
		int argc = typedArgs != null ? typedArgs.length : 0;
		if (argc == 0) {
			return MemorySegment.NULL;
		}
		MemorySegment argPtrs = Bridge.allocate((long) argc * ADDRESS.byteSize());
		for (int i = 0; i < argc; i++) {
			MemorySegment slot = typedArgSlot(typedArgs[i]);
			argPtrs.set(ADDRESS, (long) i * ADDRESS.byteSize(), slot);
		}
		return argPtrs;
	}

	private static MemorySegment typedArgSlot(Object value) {
		if (value instanceof Boolean b) {
			MemorySegment slot = Bridge.allocate(1);
			slot.set(JAVA_BYTE, 0, b ? (byte) 1 : (byte) 0);
			return slot;
		}
		if (value instanceof Float f) {
			MemorySegment slot = Bridge.allocate(4);
			slot.set(JAVA_FLOAT, 0, f);
			return slot;
		}
		if (value instanceof Double d) {
			MemorySegment slot = Bridge.allocate(8);
			slot.set(JAVA_DOUBLE, 0, d);
			return slot;
		}
		if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
			MemorySegment slot = Bridge.allocate(4);
			slot.set(JAVA_INT, 0, ((Number) value).intValue());
			return slot;
		}
		if (value instanceof Long l) {
			MemorySegment slot = Bridge.allocate(8);
			slot.set(JAVA_LONG, 0, l);
			return slot;
		}
		throw new IllegalArgumentException(
				"Unsupported typed ptrcall argument: " + (value == null ? "null" : value.getClass().getName()));
	}

	private static java.lang.Object callMethodBind(String className, String methodName, long hash, MemorySegment object,
			java.lang.Object... args) {
		return Bridge.runDowncall(() -> Bridge.runScoped(() -> {
			int depth = Bridge.callDepth();
			if (depth >= Bridge.MAX_CALL_DEPTH) {
				throw new RuntimeException("Maximum call depth exceeded: " + depth);
			}
			int argc = args != null ? args.length : 0;
			if (argc > Bridge.MAX_CALL_ARGS) {
				throw new RuntimeException("Too many arguments: " + argc + " (max " + Bridge.MAX_CALL_ARGS + ")");
			}

			try {
				Bridge.destroyVariant(Bridge.resultSlot(depth));
				MemorySegment argPtrs;
				if (argc > 0) {
					argPtrs = Bridge.argPtrsSlot(depth);
					for (int i = 0; i < argc; i++) {
						MemorySegment slot = Bridge.argSlot(depth, i);
						VariantUtils.fromObjectInto(args[i], slot);
						argPtrs.set(ADDRESS, (long) i * ADDRESS.byteSize(), slot);
					}
				} else {
					argPtrs = MemorySegment.NULL;
				}

				MemorySegment resultVar = Bridge.resultSlot(depth);
				MemorySegment errorVar = Bridge.errorSlot(depth);

				return Bridge.withCallDepth(depth, () -> {
					MemorySegment methodBind = getCachedMethodBind(className, methodName, hash);
					if (methodBind.address() == 0) {
						throw new RuntimeException("Method bind not found: " + methodName + " on " + className);
					}
					Bridge.callVoid(ApiIndex.OBJECT_METHOD_BIND_CALL, methodBind, object, argPtrs, (long) argc,
							resultVar, errorVar);

					int errorCode = errorVar.get(JAVA_INT, 0);
					if (errorCode != 0) {
						StringBuilder sb = new StringBuilder();
						sb.append("Call error ").append(errorCode).append(" calling ").append(className).append(".")
								.append(methodName);
						sb.append(" (arg=").append(errorVar.get(JAVA_INT, 4));
						sb.append(", expected=").append(errorVar.get(JAVA_INT, 8)).append(")");
						throw new RuntimeException(sb.toString());
					}

					Variant resultVariant = new Variant(resultVar);
					return VariantUtils.toObject(resultVariant);
				});
			} finally {
				if (args != null) {
					for (int i = 0; i < argc; i++) {
						Bridge.destroyVariant(Bridge.argSlot(depth, i));
					}
				}
			}
		}));
	}

	// ----------------------------------------------------------------
	// Signal emission
	// ----------------------------------------------------------------

	public int emitSignal(String signalName, java.lang.Object... args) {
		checkValid();

		return Bridge.runDowncall(() -> Bridge.runScoped(() -> {
			int depth = Bridge.callDepth();
			if (depth >= Bridge.MAX_CALL_DEPTH) {
				return -1;
			}

			HashResult hr = resolveMethodHash("emit_signal");
			if (!hr.isFound()) {
				return -1;
			}

			MemorySegment methodBind = getCachedMethodBind(hr.className, "emit_signal", hr.hash);
			if (methodBind.address() == 0) {
				return -2;
			}

			int argc = 1 + (args != null ? args.length : 0);
			if (argc > Bridge.MAX_CALL_ARGS) {
				return -1;
			}

			try {
				Bridge.destroyVariant(Bridge.resultSlot(depth));
				MemorySegment argPtrs = Bridge.argPtrsSlot(depth);

				MemorySegment snSlot = Bridge.argSlot(depth, 0);
				Variant.fromStringNameInto(GodotStringName.fromJavaString(signalName), snSlot);
				argPtrs.set(ADDRESS, 0, snSlot);

				for (int i = 0; args != null && i < args.length; i++) {
					MemorySegment slot = Bridge.argSlot(depth, i + 1);
					VariantUtils.fromObjectInto(args[i], slot);
					argPtrs.set(ADDRESS, (long) (i + 1) * ADDRESS.byteSize(), slot);
				}

				MemorySegment resultVar = Bridge.resultSlot(depth);
				MemorySegment errorVar = Bridge.errorSlot(depth);

				return Bridge.withCallDepth(depth, () -> {
					Bridge.callVoid(ApiIndex.OBJECT_METHOD_BIND_CALL, methodBind, MemorySegment.ofAddress(nativeObject),
							argPtrs, (long) argc, resultVar, errorVar);

					int errorCode = errorVar.get(JAVA_INT, 0);
					if (errorCode != 0) {
						logger.error("emit_signal '{}' failed: error={}", signalName, errorCode);
						return -3;
					}

					Variant resultVariant = new Variant(resultVar);
					java.lang.Object result = VariantUtils.toObject(resultVariant);
					if (result instanceof Number) {
						return ((Number) result).intValue();
					}
					return 0;
				});
			} finally {
				for (int i = 0; i < argc; i++) {
					Bridge.destroyVariant(Bridge.argSlot(depth, i));
				}
			}
		}));
	}

	// ----------------------------------------------------------------
	// Virtual lifecycle methods
	// ----------------------------------------------------------------

	public void _ready() {
	}

	public void _process(double delta) {
	}

	public void _physicsProcess(double delta) {
	}

	public void _enterTree() {
	}

	public void _exitTree() {
	}

	public boolean _input(java.lang.Object event) {
		return false;
	}

	public boolean _shortcutInput(java.lang.Object event) {
		return false;
	}

	public boolean _unhandledInput(java.lang.Object event) {
		return false;
	}

	public boolean _unhandledKeyInput(java.lang.Object event) {
		return false;
	}

	public void _draw() {
	}

	public void _integrateForces(java.lang.Object state) {
	}

	public boolean _guiInput(java.lang.Object event) {
		return false;
	}

	public GodotArray _getConfigurationWarnings() {
		return new GodotArray();
	}

	public GodotArray _getAccessibilityConfigurationWarnings() {
		return new GodotArray();
	}

	public Godot _getFocusedAccessibilityElement() {
		return null;
	}

	public void _notification(int what) {
	}

	public void _accessibilityUpdate() {
	}

	public void _accessibilityInvalidate() {
	}

	public void onNotification(int what) {
		_notification(what);
		switch (what) {
			case NOTIFICATION_ACCESSIBILITY_UPDATE -> _accessibilityUpdate();
			case NOTIFICATION_ACCESSIBILITY_INVALIDATE -> _accessibilityInvalidate();
			default -> {
			}
		}
	}

	// ----------------------------------------------------------------
	// Property access
	// ----------------------------------------------------------------

	public java.lang.Object getProperty(String name) {
		checkValid();
		try {
			return call("get_" + name);
		} catch (RuntimeException e) {
			return call(name);
		}
	}

	public void setProperty(String name, java.lang.Object value) {
		checkValid();
		call("set_" + name, value);
	}

	// ----------------------------------------------------------------
	// Signal connection
	// ----------------------------------------------------------------

	public boolean connect(String signalName, org.godot.core.Callable callable, int flags) {
		checkValid();
		java.lang.Object result = call("connect", signalName, callable, flags);
		if (result instanceof Number) {
			return ((Number) result).intValue() == 0;
		}
		return false;
	}

	// ----------------------------------------------------------------
	// Lifecycle
	// ----------------------------------------------------------------

	public void free() {
		if (nativeObject != 0) {
			org.godot.internal.ref.JavaObjectMap.remove(nativeObject);
			nativeObject = 0;
		}
	}

	public void onFreed() {
	}

	@Override
	public String toString() {
		if (nativeObject == 0)
			return getClass().getSimpleName() + "[invalid]";
		return getClass().getSimpleName() + "[@" + Long.toHexString(nativeObject) + "]";
	}
}
