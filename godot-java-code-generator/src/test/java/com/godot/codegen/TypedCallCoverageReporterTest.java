package com.godot.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class TypedCallCoverageReporterTest {

	@Test
	void reportCountsTypedAndDynamicMethodReasons() {
		ClassInfo classInfo = new ClassInfo("TestNode", null, false, false, "core", List.of(),
				List.of(new MethodInfo("get_child_count", true, false, false, false, 1L, List.of(), "int", "int32"),
						new MethodInfo("get_name", true, false, false, false, 2L, List.of(), "String", null),
						new MethodInfo("set_enabled", false, false, false, false, 3L,
								List.of(new ArgInfo("enabled", "bool", null, null)), null, null),
						new MethodInfo("set_position", false, false, false, false, 4L,
								List.of(new ArgInfo("position", "Vector2", null, null)), null, null),
						new MethodInfo("get_rid", true, false, false, false, 5L, List.of(), "RID", null),
						new MethodInfo("free_rid", false, false, false, false, 6L,
								List.of(new ArgInfo("rid", "RID", null, null)), null, null),
						new MethodInfo("callv", false, false, false, true, 7L, List.of(), "Variant", null)),
				List.of(), List.of());

		TypedCallCoverageReporter.Report report = TypedCallCoverageReporter.analyze(List.of(classInfo));

		assertEquals(7, report.methods);
		assertEquals(6, report.typedMethods);
		assertEquals(1, report.dynamicMethods);
		assertEquals(1, report.reasonCount("vararg"));
	}

	@Test
	void reportCountsPropertyAccessorsAsDynamicInCurrentPhase() {
		ClassInfo classInfo = new ClassInfo("TestNode", null, false, false, "core", List.of(), List.of(),
				List.of(new PropertyInfo("name", "String", "set_name", "get_name"),
						new PropertyInfo("readonly", "int", null, "get_readonly")),
				List.of());

		TypedCallCoverageReporter.Report report = TypedCallCoverageReporter.analyze(List.of(classInfo));

		assertEquals(3, report.propertyAccessors);
		assertEquals(0, report.typedPropertyAccessors);
		assertEquals(3, report.dynamicPropertyAccessors);
		assertEquals(2, report.reasonCount("property-getter:unresolved"));
		assertEquals(1, report.reasonCount("property-setter:unresolved"));
	}

	@Test
	void reportCountsResolvedSupportedPropertyAccessorsAsTyped() {
		ClassInfo classInfo = new ClassInfo("TestNode", null, false, false, "core", List.of(),
				List.of(new MethodInfo("get_enabled", true, false, false, false, 6L, List.of(), "bool", null),
						new MethodInfo("set_enabled", false, false, false, false, 7L,
								List.of(new ArgInfo("enabled", "bool", null, null)), null, null)),
				List.of(new PropertyInfo("enabled", "bool", "set_enabled", "get_enabled")), List.of());

		TypedCallCoverageReporter.Report report = TypedCallCoverageReporter.analyze(List.of(classInfo));

		assertEquals(2, report.propertyAccessors);
		assertEquals(2, report.typedPropertyAccessors);
		assertEquals(0, report.dynamicPropertyAccessors);
	}

	@Test
	void reportKeepsIncompatiblePropertyAccessorsDynamic() {
		ClassInfo classInfo = new ClassInfo("TestNode", null, false, false, "core", List.of(),
				List.of(new MethodInfo("get_ratio", true, false, false, false, 8L, List.of(), "float", "float"),
						new MethodInfo("set_ratio", false, false, false, false, 9L,
								List.of(new ArgInfo("ratio", "float", "float", null)), null, null)),
				List.of(new PropertyInfo("ratio", "bool", "set_ratio", "get_ratio")), List.of());

		TypedCallCoverageReporter.Report report = TypedCallCoverageReporter.analyze(List.of(classInfo));

		assertEquals(2, report.propertyAccessors);
		assertEquals(0, report.typedPropertyAccessors);
		assertEquals(2, report.dynamicPropertyAccessors);
		assertEquals(1, report.reasonCount("property-getter:incompatible-property-signature"));
		assertEquals(1, report.reasonCount("property-setter:incompatible-property-signature"));
	}
}
