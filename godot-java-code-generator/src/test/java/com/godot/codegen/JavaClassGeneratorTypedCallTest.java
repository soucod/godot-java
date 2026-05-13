package com.godot.codegen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class JavaClassGeneratorTypedCallTest {

	@Test
	void int32NoArgMethodUsesTypedPtrcallHelper() {
		String source = generateSource(
				new MethodInfo("get_child_count", true, false, false, false, 894402480L, List.of(), "int", "int32"));

		assertTrue(source.contains("return callEngineInt32(\"TestNode\", \"get_child_count\", 894402480L);"), source);
	}

	@Test
	void boolMethodWithPrimitiveArgsUsesTypedPtrcallHelper() {
		String source = generateSource(new MethodInfo("are_points_connected", true, false, false, false, 1L,
				List.of(new ArgInfo("id", "int", "int64", null), new ArgInfo("bidirectional", "bool", null, null)),
				"bool", null));

		assertTrue(source.contains(
				"return callEngineBool(\"TestNode\", \"are_points_connected\", 1L, java.lang.Long.valueOf(id), java.lang.Boolean.valueOf(bidirectional));"),
				source);
	}

	@Test
	void unsupportedArgKeepsDynamicCallPath() {
		String source = generateSource(new MethodInfo("intersects", true, false, false, false, 2L,
				List.of(new ArgInfo("position", "Vector2", null, null)), "bool", null));

		assertTrue(source.contains("return (boolean) callEngine(\"TestNode\", \"intersects\", 2L"), source);
	}

	@Test
	void unsupportedReturnKeepsDynamicCallPath() {
		String source = generateSource(
				new MethodInfo("get_name", true, false, false, false, 4L, List.of(), "String", null));

		assertTrue(source.contains("return (String) callEngine(\"TestNode\", \"get_name\", 4L);"), source);
	}

	@Test
	void float32ReturnUsesTypedPtrcallHelper() {
		String source = generateSource(
				new MethodInfo("get_ratio", true, false, false, false, 3L, List.of(), "float", "float"));

		assertTrue(source.contains("return callEngineFloat32(\"TestNode\", \"get_ratio\", 3L);"), source);
	}

	@Test
	void staticPrimitiveReturnUsesStaticTypedPtrcallHelper() {
		String source = generateSource(
				new MethodInfo("max_int", false, true, false, false, 5L, List.of(), "int", "int64"));

		assertTrue(source.contains("public static long maxInt()"), source);
		assertTrue(source.contains("return callStaticInt64(\"TestNode\", \"max_int\", 5L);"), source);
	}

	private String generateSource(MethodInfo method) {
		ClassInfo classInfo = new ClassInfo("TestNode", null, false, false, "core", List.of(), List.of(method),
				List.of(), List.of());
		JavaClassGenerator generator = new JavaClassGenerator("org.godot.node", List.of(classInfo));
		return generator.generateClass(classInfo).toString();
	}
}
