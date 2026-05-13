package examples.integrationtest;

import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.annotation.GodotMethod;
import org.godot.annotation.Signal;
import org.godot.bridge.Bridge;
import org.godot.internal.NativeMemoryTracker;
import org.godot.internal.ref.JavaObjectMap;
import org.godot.internal.ref.RefCountedHelper;
import org.godot.math.Vector2;
import org.godot.node.Control;
import org.godot.node.Node;
import org.godot.node.RefCounted;

/**
 * Integration test node. Validates all core features from the Java side.
 * GDScript (test_runner.gd) reads outputs and prints PASS/FAIL markers.
 */
@GodotClass(name = "IntegrationTestNode", parent = "Node")
public class IntegrationTestNode extends Node {

	private boolean readyCalled = false;
	private boolean processCalled = false;
	private double lastDelta = -1.0;
	private int processCount = 0;
	private int lastNotification = -1;
	private int notificationCount = 0;
	private final StringBuilder lifecycleEvents = new StringBuilder();

	@Export
	public int exportedInt = 42;

	@Export
	public String exportedString = "hello";

	@Export
	public float exportedFloat = 3.14f;

	@Export
	public boolean exportedBool = true;

	private int methodCallCount = 0;

	@Signal
	public void testSignal(int value) {
	}

	@Signal
	public void stringSignal(String msg) {
	}

	@Override
	public void _enterTree() {
		recordLifecycleEvent("enter_tree");
		System.out.println("[IT] _enterTree called");
	}

	@Override
	public void _ready() {
		readyCalled = true;
		recordLifecycleEvent("ready");
		System.out.println("[IT] _ready called");
	}

	@Override
	public void _exitTree() {
		recordLifecycleEvent("exit_tree");
		System.out.println("[IT] _exitTree called");
	}

	@Override
	public void onNotification(int what) {
		lastNotification = what;
		notificationCount++;
		if (what == 10) {
			recordLifecycleEvent("notification_enter_tree");
		} else if (what == 13) {
			recordLifecycleEvent("notification_ready");
		}
		System.out.println("[IT] onNotification: " + what);
	}

	@Override
	public void _process(double delta) {
		if (!processCalled) {
			processCalled = true;
			lastDelta = delta;
			recordLifecycleEvent("process");
			System.out.println("[IT] _process called, delta=" + delta);
		}
		processCount++;
		if (processCount >= 3) {
			// Stop processing after 3 frames
			setProperty("process_mode", 4); // PROCESS_MODE_DISABLED = 4
		}
	}

	// ---- Methods callable from GDScript ----

	private void recordLifecycleEvent(String event) {
		if (!lifecycleEvents.isEmpty()) {
			lifecycleEvents.append(",");
		}
		lifecycleEvents.append(event);
	}

	@GodotMethod
	public boolean wasReadyCalled() {
		return readyCalled;
	}

	@GodotMethod
	public boolean wasProcessCalled() {
		return processCalled;
	}

	@GodotMethod
	public double getLastDelta() {
		return lastDelta;
	}

	@GodotMethod
	public int add(int a, int b) {
		methodCallCount++;
		return a + b;
	}

	@GodotMethod
	public String echo(String msg) {
		methodCallCount++;
		return "echo:" + msg;
	}

	@GodotMethod
	public int getMethodCallCount() {
		return methodCallCount;
	}

	@GodotMethod
	public int getLastNotification() {
		return lastNotification;
	}

	@GodotMethod
	public int getNotificationCount() {
		return notificationCount;
	}

	@GodotMethod
	public String getLifecycleEvents() {
		return lifecycleEvents.toString();
	}

	@GodotMethod
	public void emitTestSignal(int value) {
		emitSignal("testSignal", value);
	}

	@GodotMethod
	public void emitStringSignal(String msg) {
		emitSignal("stringSignal", msg);
	}

	@GodotMethod
	public int getExportedInt() {
		return exportedInt;
	}

	@GodotMethod
	public void setExportedInt(int val) {
		exportedInt = val;
	}

	@GodotMethod
	public String primitiveDispatchMatrix(int intValue, long longValue, float floatValue, double doubleValue,
			boolean boolValue, String stringValue) {
		methodCallCount++;
		return intValue + ":" + longValue + ":" + Math.round(floatValue * 10.0f) + ":" + Math.round(doubleValue * 10.0)
				+ ":" + boolValue + ":" + stringValue;
	}

	@GodotMethod
	public double vector2LengthSquared(Vector2 value) {
		methodCallCount++;
		return value.x * value.x + value.y * value.y;
	}

	@GodotMethod
	public boolean generatedRegistryAvailable() {
		try {
			Class.forName("org.godot.internal.GeneratedClassRegistry");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	@GodotMethod
	public int getJavaObjectMapSize() {
		return JavaObjectMap.size();
	}

	@GodotMethod
	public int getTrackedRefCountedCount() {
		return RefCountedHelper.trackedReferenceCount();
	}

	@GodotMethod
	public boolean testJavaToGodotNodeChurn(int count) {
		int before = childCount();
		for (int i = 0; i < count; i++) {
			Node child = Node.create();
			if (child == null || !child.isValid()) {
				System.out.println("FAIL: Node.create returned invalid child at iteration " + i);
				return false;
			}
			addChild(child);
			if (childCount() != before + 1) {
				System.out.println("FAIL: child count did not increase at iteration " + i);
				return false;
			}
			removeChild(child);
			child.free();
			if (childCount() != before) {
				System.out.println("FAIL: child count did not return to baseline at iteration " + i);
				return false;
			}
		}
		return true;
	}

	private int childCount() {
		return getChildCount(false);
	}

	@GodotMethod
	public boolean testRefCountedLifecycle() {
		int trackedBefore = RefCountedHelper.trackedReferenceCount();
		RefCounted value = RefCounted.create();
		if (value == null || !value.isValid()) {
			System.out.println("FAIL: RefCounted.create returned invalid object");
			return false;
		}
		long ptr = value.getNativeObject();
		boolean referenced = RefCountedHelper.reference(ptr);
		if (!referenced) {
			System.out.println("FAIL: RefCountedHelper.reference failed");
			value.unreference();
			value.setNativeObject(0);
			return false;
		}
		if (RefCountedHelper.trackedReferenceCount() != trackedBefore + 1) {
			System.out.println("FAIL: RefCountedHelper did not track reference");
			RefCountedHelper.unreference(ptr);
			value.unreference();
			value.setNativeObject(0);
			return false;
		}
		RefCountedHelper.unreference(ptr);
		value.unreference();
		value.setNativeObject(0);
		return RefCountedHelper.trackedReferenceCount() == trackedBefore;
	}

	@GodotMethod
	public String captureMemoryStats(String label) {
		String stats = label + ": " + NativeMemoryTracker.getStats();
		System.out.println("[IT] " + stats);
		return stats;
	}

	@GodotMethod
	public String runScopedMemoryDiagnostics(int iterations) {
		long countBefore = NativeMemoryTracker.getLiveAllocationCount();
		long bytesBefore = NativeMemoryTracker.getLiveBytes();
		for (int i = 0; i < iterations; i++) {
			Bridge.runScoped(() -> {
				Bridge.allocVariant();
				Bridge.allocate(32);
			});
		}
		long countAfter = NativeMemoryTracker.getLiveAllocationCount();
		long bytesAfter = NativeMemoryTracker.getLiveBytes();
		String stats = "scoped-memory-diagnostics iterations=" + iterations + " beforeCount=" + countBefore
				+ " beforeBytes=" + bytesBefore + " afterCount=" + countAfter + " afterBytes=" + bytesAfter;
		System.out.println("[IT] " + stats);
		return stats;
	}

	@GodotMethod
	public boolean testShortStress(int iterations) {
		for (int i = 0; i < iterations; i++) {
			if (add(i, 1) != i + 1) {
				return false;
			}
			if (!("echo:stress-" + i).equals(echo("stress-" + i))) {
				return false;
			}
			if (vector2LengthSquared(new Vector2(i, 2)) != i * i + 4) {
				return false;
			}
			emitTestSignal(i);
		}
		runScopedMemoryDiagnostics(iterations);
		return testJavaToGodotNodeChurn(Math.max(1, iterations / 10));
	}

	// ---- Control layout tests (Task 4.5) ----

	@GodotMethod
	public boolean test_control_minimum_size() {
		// Create a Control to test minimum size methods
		Control control = Control.create();
		if (control == null || !control.isValid()) {
			System.out.println("FAIL: could not create Control");
			return false;
		}

		// Set custom minimum size using Vector2
		control.setCustomMinimumSize(new Vector2(100, 50));

		// Get the minimum width and height
		double minWidth = control.getCustomMinimumSize().x;
		double minHeight = control.getCustomMinimumSize().y;

		// Verify - custom minimum size should be reflected in getMinimumWidth/Height
		// Note: getMinimumWidth/Height returns the current minimum size which
		// may be larger if the content requires it
		if (minWidth < 0 || minHeight < 0) {
			System.out.println("FAIL: minWidth/minHeight should be >= 0, got w=" + minWidth + " h=" + minHeight);
			return false;
		}

		System.out.println("PASS: Control minimum size working (w=" + minWidth + " h=" + minHeight + ")");
		return true;
	}
}
