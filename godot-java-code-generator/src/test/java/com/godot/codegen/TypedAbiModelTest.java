package com.godot.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TypedAbiModelTest {

	@Test
	void numericMetadataDescribesHelperArgumentAndPropertyCompatibility() {
		TypedAbiModel.Descriptor uint16 = TypedAbiModel.descriptor("int", "uint16");
		assertEquals("Uint16", uint16.returnHelperSuffix());
		assertEquals("typedUint16Arg(value)", uint16.arg("value"));
		assertTrue(uint16.returnCompatibleWithProperty("int"));
		assertTrue(uint16.returnCompatibleWithProperty("long"));
		assertTrue(uint16.argumentCompatibleWithProperty("short"));
		assertTrue(uint16.argumentCompatibleWithProperty("long"));

		TypedAbiModel.Descriptor uint64 = TypedAbiModel.descriptor("int", "uint64");
		assertEquals("Uint64", uint64.returnHelperSuffix());
		assertEquals("typedUint64Arg(value)", uint64.arg("value"));
		assertTrue(uint64.returnCompatibleWithProperty("java.math.BigInteger"));
		assertFalse(uint64.returnCompatibleWithProperty("long"));
		assertTrue(uint64.argumentCompatibleWithProperty("long"));
		assertTrue(uint64.argumentCompatibleWithProperty("java.math.BigInteger"));
	}

	@Test
	void collectionDescriptorsSeparateArgumentAndReturnSupport() {
		TypedAbiModel.Descriptor array = TypedAbiModel.descriptor("Array", null);
		assertTrue(array.supportsReturn());
		assertEquals("Array", array.returnHelperSuffix());
		assertTrue(array.supportsArgument());
		assertEquals("typedArrayArg(values)", array.arg("values"));
		assertTrue(array.returnCompatibleWithProperty("GodotArray"));
		assertTrue(array.argumentCompatibleWithProperty("GodotArray"));

		TypedAbiModel.Descriptor dictionary = TypedAbiModel.descriptor("Dictionary", null);
		assertTrue(dictionary.supportsReturn());
		assertEquals("Dictionary", dictionary.returnHelperSuffix());
		assertTrue(dictionary.supportsArgument());
		assertEquals("typedDictionaryArg(options)", dictionary.arg("options"));
		assertTrue(dictionary.returnCompatibleWithProperty("GodotDictionary"));
		assertTrue(dictionary.argumentCompatibleWithProperty("GodotDictionary"));

		TypedAbiModel.Descriptor packed = TypedAbiModel.descriptor("PackedVector3Array", null);
		assertTrue(packed.supportsReturn());
		assertTrue(packed.supportsArgument());
		assertEquals("PackedVector3Array", packed.returnHelperSuffix());
		assertEquals("typedPackedVector3ArrayArg(points)", packed.arg("points"));
		assertTrue(packed.returnCompatibleWithProperty("double[][]"));
	}

	@Test
	void typedCollectionsUseGenericArrayDictionaryDescriptors() {
		TypedAbiModel.Descriptor ridArray = TypedAbiModel.descriptor("typedarray::RID", null);
		assertTrue(ridArray.supportsReturn());
		assertTrue(ridArray.supportsArgument());
		assertEquals("Array", ridArray.returnHelperSuffix());
		assertEquals("typedArrayArg(rids)", ridArray.arg("rids"));
		assertTrue(ridArray.returnCompatibleWithProperty("GodotArray"));
		assertTrue(ridArray.returnCompatibleWithProperty("GodotArray<Long>"));

		TypedAbiModel.Descriptor dict = TypedAbiModel.descriptor("typeddictionary::String;int", null);
		assertTrue(dict.supportsReturn());
		assertTrue(dict.supportsArgument());
		assertEquals("Dictionary", dict.returnHelperSuffix());
		assertEquals("typedDictionaryArg(data)", dict.arg("data"));
		assertTrue(dict.returnCompatibleWithProperty("GodotDictionary"));
		assertTrue(dict.returnCompatibleWithProperty("GodotDictionary<String, Long>"));
	}

	@Test
	void typedArrayDescriptorsSupportReturnAndArgument() {
		TypedAbiModel.Descriptor names = TypedAbiModel.descriptor("typedarray::StringName", null);
		assertTrue(names.supportsReturn());
		assertTrue(names.supportsArgument());
		assertEquals("Array", names.returnHelperSuffix());
		assertEquals("typedArrayArg(names)", names.arg("names"));
		assertTrue(names.returnCompatibleWithProperty("GodotArray<String>"));

		TypedAbiModel.Descriptor ints = TypedAbiModel.descriptor("typedarray::int", null);
		assertTrue(ints.supportsReturn());
		assertTrue(ints.supportsArgument());
		assertEquals("Array", ints.returnHelperSuffix());
		assertTrue(ints.returnCompatibleWithProperty("GodotArray<Long>"));
	}

	@Test
	void propertyCompatibilityKeepsLegacyDynamicFallbacks() {
		TypedAbiModel.Descriptor enumType = TypedAbiModel.descriptor("enum::NavigationMesh.SamplePartitionType", null);
		assertTrue(enumType.supportsReturn());
		assertTrue(enumType.supportsArgument());
		assertTrue(enumType.returnCompatibleWithProperty("long"));
		assertFalse(enumType.argumentCompatibleWithProperty("long"));

		TypedAbiModel.Descriptor variant = TypedAbiModel.descriptor("Variant", null);
		assertTrue(variant.supportsReturn());
		assertTrue(variant.supportsArgument());
		assertFalse(variant.returnCompatibleWithProperty("Object"));
		assertFalse(variant.argumentCompatibleWithProperty("Object"));
	}
}
