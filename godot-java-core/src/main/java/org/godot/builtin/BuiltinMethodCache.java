package org.godot.builtin;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;

import org.godot.bridge.Bridge;
import org.godot.core.GodotStringName;
import org.godot.internal.api.ApiIndex;

/**
 * Cache for builtin method function pointers obtained via
 * variant_get_ptr_builtin_method. Each entry is a MethodHandle with the
 * GDExtensionPtrBuiltInMethod signature: void(base, args, ret, argc)
 */
public final class BuiltinMethodCache {

	private BuiltinMethodCache() {
	}

	private static final FunctionDescriptor BUILTIN_METHOD_DESC = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS,
			ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT);

	private static final ConcurrentHashMap<String, MethodHandle> CACHE = new ConcurrentHashMap<>();

	private static final Linker LINKER = Linker.nativeLinker();

	public static MethodHandle getMethod(int variantType, String methodName, long hash) {
		String key = variantType + "::" + methodName + "::" + hash;
		return CACHE.computeIfAbsent(key, k -> {
			GodotStringName methodSn = GodotStringName.fromJavaString(methodName);
			MemorySegment funcPtr = Bridge.callPtr(ApiIndex.VARIANT_GET_PTR_BUILTIN_METHOD, variantType,
					methodSn.segment(), hash);
			if (funcPtr.address() == 0) {
				throw new RuntimeException(
						"Builtin method not found: variantType=" + variantType + ", method=" + methodName);
			}
			return LINKER.downcallHandle(funcPtr, BUILTIN_METHOD_DESC);
		});
	}

	public static void invoke(MethodHandle mh, MemorySegment base, MemorySegment args, MemorySegment ret, int argc) {
		try {
			mh.invokeExact(base, args, ret, argc);
		} catch (Throwable t) {
			throw new RuntimeException("Builtin method call failed", t);
		}
	}
}
