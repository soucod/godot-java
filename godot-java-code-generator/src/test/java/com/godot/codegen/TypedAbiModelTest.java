package com.godot.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
		assertFalse(array.supportsReturn());
		assertTrue(array.supportsArgument());
		assertEquals("typedArrayArg(values)", array.arg("values"));
		assertTrue(array.argumentCompatibleWithProperty("GodotArray"));

		TypedAbiModel.Descriptor packed = TypedAbiModel.descriptor("PackedVector3Array", null);
		assertTrue(packed.supportsReturn());
		assertTrue(packed.supportsArgument());
		assertEquals("PackedVector3Array", packed.returnHelperSuffix());
		assertEquals("typedPackedVector3ArrayArg(points)", packed.arg("points"));
		assertTrue(packed.returnCompatibleWithProperty("double[][]"));
	}

	@Test
	void unsupportedTypedCollectionsRemainUnmodeled() {
		assertNull(TypedAbiModel.descriptor("typedarray::RID", null));
		assertNull(TypedAbiModel.descriptor("typeddictionary::String;int", null));
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
