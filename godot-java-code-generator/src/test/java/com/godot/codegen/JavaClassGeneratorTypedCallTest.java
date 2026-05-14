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
	void packedStringArrayArgUsesTypedPtrcallHelper() {
		String source = generateSource(new MethodInfo("intersects", true, false, false, false, 2L,
				List.of(new ArgInfo("values", "PackedStringArray", null, null)), "bool", null));

		assertTrue(source.contains("public boolean intersects(String[] values)"), source);
		assertTrue(
				source.contains(
						"return callEngineBool(\"TestNode\", \"intersects\", 2L, typedPackedStringArrayArg(values));"),
				source);
	}

	@Test
	void stringReturnUsesTypedPtrcallHelper() {
		String source = generateSource(
				new MethodInfo("get_name", true, false, false, false, 4L, List.of(), "String", null));

		assertTrue(source.contains("return callEngineString(\"TestNode\", \"get_name\", 4L);"), source);
	}

	@Test
	void stringArgUsesTypedPtrcallHelper() {
		String source = generateSource(new MethodInfo("set_name", false, false, false, false, 12L,
				List.of(new ArgInfo("name", "String", null, null)), null, null));

		assertTrue(source.contains("callEngineVoid(\"TestNode\", \"set_name\", 12L, typedStringArg(name));"), source);
	}

	@Test
	void stringNameUsesTypeSpecificTypedPtrcallHelper() {
		String source = generateSource(new MethodInfo("has_method", true, false, false, false, 13L,
				List.of(new ArgInfo("method", "StringName", null, null)), "bool", null));

		assertTrue(source.contains("public boolean hasMethod(String method)"), source);
		assertTrue(
				source.contains(
						"return callEngineBool(\"TestNode\", \"has_method\", 13L, typedStringNameArg(method));"),
				source);
	}

	@Test
	void nodePathUsesTypeSpecificTypedPtrcallHelper() {
		String source = generateSource(new MethodInfo("get_node", true, false, false, false, 14L,
				List.of(new ArgInfo("path", "NodePath", null, null)), "NodePath", null));

		assertTrue(source.contains("public String getNode(String path)"), source);
		assertTrue(
				source.contains("return callEngineNodePath(\"TestNode\", \"get_node\", 14L, typedNodePathArg(path));"),
				source);
	}

	@Test
	void globalEnumReturnAndArgUseInt32TypedPtrcallHelper() {
		String source = generateSource(new MethodInfo("set_error", true, false, false, false, 15L,
				List.of(new ArgInfo("error", "enum::Error", null, null)), "enum::Error", null));

		assertTrue(source.contains("public int setError(int error)"), source);
		assertTrue(
				source.contains(
						"return callEngineInt32(\"TestNode\", \"set_error\", 15L, java.lang.Integer.valueOf(error));"),
				source);
	}

	@Test
	void classNestedEnumReturnAndArgUseEnumType() {
		EnumInfo selectMode = new EnumInfo("SelectMode", false,
				List.of(new EnumValue("SELECT_SINGLE", 0), new EnumValue("SELECT_MULTI", 1)));
		ClassInfo tree = new ClassInfo("Tree", "Control", false, true, "core", List.of(selectMode), List.of(),
				List.of(), List.of());
		ClassInfo testNode = new ClassInfo("TestNode", null, false, false, "core", List.of(),
				List.of(new MethodInfo("set_mode", true, false, false, false, 40L,
						List.of(new ArgInfo("mode", "enum::Tree.SelectMode", null, null)), "enum::Tree.SelectMode",
						null)),
				List.of(), List.of());

		String source = new JavaClassGenerator("org.godot.node", List.of(testNode, tree)).generateClass(testNode)
				.toString();

		assertTrue(source.contains("public Tree.SelectMode setMode(Tree.SelectMode mode)"), source);
		assertTrue(source.contains("callEngineInt32(\"TestNode\", \"set_mode\", 40L, mode.value)"), source);
		assertTrue(source.contains("return Tree.SelectMode.fromValue(callEngineInt32("), source);
	}

	@Test
	void uint32ReturnAndArgUseUint32TypedPtrcallHelper() {
		String source = generateSource(new MethodInfo("set_mask", true, false, false, false, 16L,
				List.of(new ArgInfo("mask", "int", "uint32", null)), "int", "uint32"));

		assertTrue(source.contains("public long setMask(long mask)"), source);
		assertTrue(source.contains("return callEngineUint32(\"TestNode\", \"set_mask\", 16L, typedUint32Arg(mask));"),
				source);
	}

	@Test
	void smallIntegerMetadataUsesSizedTypedPtrcallHelpers() {
		String source = generateSource(new MethodInfo("set_small_values", true, false, false, false, 28L,
				List.of(new ArgInfo("a", "int", "int8", null), new ArgInfo("b", "int", "uint8", null),
						new ArgInfo("c", "int", "int16", null), new ArgInfo("d", "int", "uint16", null)),
				"int", "uint16"));

		assertTrue(source.contains("public int setSmallValues(byte a, short b, short c, int d)"), source);
		assertTrue(source.contains(
				"return callEngineUint16(\"TestNode\", \"set_small_values\", 28L, typedInt8Arg(a), typedUint8Arg(b), typedInt16Arg(c), typedUint16Arg(d));"),
				source);
	}

	@Test
	void staticSmallIntegerMetadataUsesStaticSizedTypedPtrcallHelpers() {
		String source = generateSource(new MethodInfo("get_small_value", false, true, false, false, 29L,
				List.of(new ArgInfo("value", "int", "int16", null)), "int", "int8"));

		assertTrue(source.contains("public static byte getSmallValue(short value)"), source);
		assertTrue(
				source.contains("return callStaticInt8(\"TestNode\", \"get_small_value\", 29L, typedInt16Arg(value));"),
				source);
	}

	@Test
	void uint64ReturnAndArgUseUint64TypedPtrcallHelper() {
		String source = generateSource(new MethodInfo("set_large_mask", true, false, false, false, 18L,
				List.of(new ArgInfo("mask", "int", "uint64", null)), "int", "uint64"));

		assertTrue(source.contains("public java.math.BigInteger setLargeMask(java.math.BigInteger mask)"), source);
		assertTrue(
				source.contains(
						"return callEngineUint64(\"TestNode\", \"set_large_mask\", 18L, typedUint64Arg(mask));"),
				source);
	}

	@Test
	void builtinStructReturnAndArgUseTypedPtrcallHelper() {
		String source = generateSource(new MethodInfo("move_local", true, false, false, false, 17L,
				List.of(new ArgInfo("offset", "Vector2", null, null)), "Vector2", null));

		assertTrue(source.contains("public Vector2 moveLocal(Vector2 offset)"), source);
		assertTrue(source.contains("return callEngineVector2(\"TestNode\", \"move_local\", 17L, offset);"), source);
	}

	@Test
	void projectionReturnAndArgUseTypedPtrcallHelper() {
		String source = generateSource(new MethodInfo("set_projection", true, false, false, false, 21L,
				List.of(new ArgInfo("projection", "Projection", null, null)), "Projection", null));

		assertTrue(source.contains("public Projection setProjection(Projection projection)"), source);
		assertTrue(source.contains("return callEngineProjection(\"TestNode\", \"set_projection\", 21L, projection);"),
				source);
	}

	@Test
	void callableAndSignalArgsUseTypedBuiltinArgs() {
		String source = generateSource(new MethodInfo("connect_signal", false, false, false, false, 22L,
				List.of(new ArgInfo("signal", "Signal", null, null), new ArgInfo("callable", "Callable", null, null)),
				null, null));

		assertTrue(source.contains("public void connectSignal(Signal signal, Callable callable)"), source);
		assertTrue(source.contains(
				"callEngineVoid(\"TestNode\", \"connect_signal\", 22L, typedSignalArg(signal), typedCallableArg(callable));"),
				source);
	}

	@Test
	void arrayAndDictionaryArgsUseTypedBuiltinArgs() {
		String source = generateSource(new MethodInfo("merge_values", false, false, false, false, 26L,
				List.of(new ArgInfo("values", "Array", null, null), new ArgInfo("options", "Dictionary", null, null)),
				null, null));

		assertTrue(source.contains("public void mergeValues(GodotArray values, GodotDictionary options)"), source);
		assertTrue(source.contains(
				"callEngineVoid(\"TestNode\", \"merge_values\", 26L, typedArrayArg(values), typedDictionaryArg(options));"),
				source);
	}

	@Test
	void arrayAndDictionaryReturnsUseOwnedTypedBuiltinHelpers() {
		String arraySource = generateSource(
				new MethodInfo("get_values", true, false, false, false, 30L, List.of(), "Array", null));
		assertTrue(arraySource.contains("public GodotArray getValues()"), arraySource);
		assertTrue(arraySource.contains("return callEngineArray(\"TestNode\", \"get_values\", 30L);"), arraySource);

		String dictionarySource = generateSource(
				new MethodInfo("get_options", true, false, false, false, 31L, List.of(), "Dictionary", null));
		assertTrue(dictionarySource.contains("public GodotDictionary getOptions()"), dictionarySource);
		assertTrue(dictionarySource.contains("return callEngineDictionary(\"TestNode\", \"get_options\", 31L);"),
				dictionarySource);
	}

	@Test
	void typedArrayReturnsUseGenericGodotArray() {
		String namesSource = generateSource(new MethodInfo("get_groups", true, false, false, false, 32L, List.of(),
				"typedarray::StringName", null));
		assertTrue(namesSource.contains("public GodotArray<String> getGroups()"), namesSource);
		assertTrue(namesSource.contains("return callEngineArray(\"TestNode\", \"get_groups\", 32L);"), namesSource);

		String intSource = generateSource(
				new MethodInfo("get_ids", true, false, false, false, 33L, List.of(), "typedarray::int", null));
		assertTrue(intSource.contains("public GodotArray<Long> getIds()"), intSource);
		assertTrue(intSource.contains("return callEngineArray(\"TestNode\", \"get_ids\", 33L);"), intSource);
	}

	@Test
	void engineObjectReturnAndArgUseTypedPtrcallHelper() {
		ClassInfo texture = new ClassInfo("Texture2D", "Resource", true, false, "core", List.of(), List.of(), List.of(),
				List.of());
		ClassInfo classInfo = new ClassInfo("TestNode", null, false, false, "core", List.of(),
				List.of(new MethodInfo("set_texture", true, false, false, false, 19L,
						List.of(new ArgInfo("texture", "Texture2D", null, null)), "Texture2D", null)),
				List.of(), List.of());

		String source = new JavaClassGenerator("org.godot.node", List.of(classInfo, texture)).generateClass(classInfo)
				.toString();

		assertTrue(source.contains("public Texture2D setTexture(Texture2D texture)"), source);
		assertTrue(source.contains(
				"return callEngineObject(\"TestNode\", \"set_texture\", 19L, \"Texture2D\", true, typedObjectArg(texture, true));"),
				source);
	}

	@Test
	void variantReturnAndArgUseTypedPtrcallHelper() {
		String source = generateSource(new MethodInfo("echo_variant", true, false, false, false, 20L,
				List.of(new ArgInfo("value", "Variant", null, null)), "Variant", null));

		assertTrue(source.contains("public Object echoVariant(Object value)"), source);
		assertTrue(
				source.contains(
						"return callEngineVariant(\"TestNode\", \"echo_variant\", 20L, typedVariantArg(value));"),
				source);
	}

	@Test
	void packedArrayReturnUsesTypedPtrcallHelperWhenArgsAreSupported() {
		String source = generateSource(new MethodInfo("get_bytes", true, false, false, false, 23L,
				List.of(new ArgInfo("key", "StringName", null, null)), "PackedByteArray", null));

		assertTrue(source.contains("public byte[] getBytes(String key)"), source);
		assertTrue(
				source.contains(
						"return callEnginePackedByteArray(\"TestNode\", \"get_bytes\", 23L, typedStringNameArg(key));"),
				source);
	}

	@Test
	void packedArrayArgUsesTypedPtrcallHelper() {
		String source = generateSource(new MethodInfo("set_bytes", false, false, false, false, 24L,
				List.of(new ArgInfo("bytes", "PackedByteArray", null, null)), null, null));

		assertTrue(source.contains("public void setBytes(byte[] bytes)"), source);
		assertTrue(source.contains("callEngineVoid(\"TestNode\", \"set_bytes\", 24L, typedPackedByteArrayArg(bytes));"),
				source);
	}

	@Test
	void packedVectorArrayArgUsesTypedPtrcallHelper() {
		String source = generateSource(new MethodInfo("set_points", false, false, false, false, 27L,
				List.of(new ArgInfo("points", "PackedVector3Array", null, null)), null, null));

		assertTrue(source.contains("public void setPoints(double[][] points)"), source);
		assertTrue(
				source.contains(
						"callEngineVoid(\"TestNode\", \"set_points\", 27L, typedPackedVector3ArrayArg(points));"),
				source);
	}

	@Test
	void packedVectorArrayReturnUsesTypedPtrcallHelperWhenArgsAreSupported() {
		String source = generateSource(
				new MethodInfo("get_points", true, false, false, false, 25L, List.of(), "PackedVector3Array", null));

		assertTrue(source.contains("public double[][] getPoints()"), source);
		assertTrue(source.contains("return callEnginePackedVector3Array(\"TestNode\", \"get_points\", 25L);"), source);
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

	@Test
	void voidMethodWithPrimitiveArgsUsesTypedPtrcallHelper() {
		String source = generateSource(new MethodInfo("set_process", false, false, false, false, 6L,
				List.of(new ArgInfo("enable", "bool", null, null)), null, null));

		assertTrue(source.contains("public void setProcess(boolean enable)"), source);
		assertTrue(
				source.contains(
						"callEngineVoid(\"TestNode\", \"set_process\", 6L, java.lang.Boolean.valueOf(enable));"),
				source);
	}

	@Test
	void voidMethodWithPackedStringArrayArgUsesTypedPtrcallHelper() {
		String source = generateSource(new MethodInfo("set_position", false, false, false, false, 7L,
				List.of(new ArgInfo("values", "PackedStringArray", null, null)), null, null));

		assertTrue(source.contains("public void setPosition(String[] values)"), source);
		assertTrue(
				source.contains(
						"callEngineVoid(\"TestNode\", \"set_position\", 7L, typedPackedStringArrayArg(values));"),
				source);
	}

	@Test
	void ridReturnUsesTypedPtrcallHelper() {
		String source = generateSource(
				new MethodInfo("get_rid", true, false, false, false, 10L, List.of(), "RID", null));

		assertTrue(source.contains("public long getRid()"), source);
		assertTrue(source.contains("return callEngineRid(\"TestNode\", \"get_rid\", 10L);"), source);
	}

	@Test
	void ridArgUsesTypedPtrcallHelper() {
		String source = generateSource(new MethodInfo("free_rid", false, false, false, false, 11L,
				List.of(new ArgInfo("rid", "RID", null, null)), null, null));

		assertTrue(source.contains("public void freeRid(long rid)"), source);
		assertTrue(source.contains("callEngineVoid(\"TestNode\", \"free_rid\", 11L, java.lang.Long.valueOf(rid));"),
				source);
	}

	@Test
	void propertyGetterUsesTypedMetadataFromResolvedGetterMethod() {
		ClassInfo classInfo = new ClassInfo("TestNode", null, false, false, "core", List.of(),
				List.of(new MethodInfo("get_child_count", true, false, false, false, 8L, List.of(), "int", "int32")),
				List.of(new PropertyInfo("child_count", "int", null, "get_child_count")), List.of());

		String source = new JavaClassGenerator("org.godot.node", List.of(classInfo)).generateClass(classInfo)
				.toString();

		assertTrue(source.contains("public long getChildCount()"), source);
		assertTrue(source.contains("return callEngineInt32(\"TestNode\", \"get_child_count\", 8L);"), source);
	}

	@Test
	void propertySetterUsesTypedMetadataFromResolvedSetterMethod() {
		ClassInfo classInfo = new ClassInfo("TestNode", null, false, false, "core", List.of(),
				List.of(new MethodInfo("set_enabled", false, false, false, false, 9L,
						List.of(new ArgInfo("enabled", "bool", null, null)), null, null)),
				List.of(new PropertyInfo("enabled", "bool", "set_enabled", null)), List.of());

		String source = new JavaClassGenerator("org.godot.node", List.of(classInfo)).generateClass(classInfo)
				.toString();

		assertTrue(source.contains("public void setEnabled(boolean value)"), source);
		assertTrue(
				source.contains("callEngineVoid(\"TestNode\", \"set_enabled\", 9L, java.lang.Boolean.valueOf(value));"),
				source);
	}

	private String generateSource(MethodInfo method) {
		ClassInfo classInfo = new ClassInfo("TestNode", null, false, false, "core", List.of(), List.of(method),
				List.of(), List.of());
		JavaClassGenerator generator = new JavaClassGenerator("org.godot.node", List.of(classInfo));
		return generator.generateClass(classInfo).toString();
	}
}
