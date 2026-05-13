package com.godot.codegen;

import java.util.List;
import java.util.Map;
import java.util.Set;

final class TypedCallSupport {

	private static final Set<String> BUILTIN_STRUCT_TYPES = Set.of("Vector2", "Vector2i", "Rect2", "Rect2i", "Vector3",
			"Vector3i", "Transform2D", "Vector4", "Vector4i", "Plane", "Quaternion", "AABB", "Basis", "Transform3D",
			"Projection", "Color");

	private static final Map<String, String> BUILTIN_STRUCT_HELPERS = Map.ofEntries(Map.entry("Vector2", "Vector2"),
			Map.entry("Vector2i", "Vector2i"), Map.entry("Rect2", "Rect2"), Map.entry("Rect2i", "Rect2i"),
			Map.entry("Vector3", "Vector3"), Map.entry("Vector3i", "Vector3i"), Map.entry("Transform2D", "Transform2D"),
			Map.entry("Vector4", "Vector4"), Map.entry("Vector4i", "Vector4i"), Map.entry("Plane", "Plane"),
			Map.entry("Quaternion", "Quaternion"), Map.entry("AABB", "AABB"), Map.entry("Basis", "Basis"),
			Map.entry("Transform3D", "Transform3D"), Map.entry("Projection", "Projection"), Map.entry("Color", "Color"));

	private static final Map<String, String> PACKED_ARRAY_HELPERS = Map.ofEntries(
			Map.entry("PackedByteArray", "PackedByteArray"), Map.entry("PackedInt32Array", "PackedInt32Array"),
			Map.entry("PackedInt64Array", "PackedInt64Array"), Map.entry("PackedFloat32Array", "PackedFloat32Array"),
			Map.entry("PackedFloat64Array", "PackedFloat64Array"),
			Map.entry("PackedStringArray", "PackedStringArray"),
			Map.entry("PackedVector2Array", "PackedVector2Array"),
			Map.entry("PackedVector3Array", "PackedVector3Array"), Map.entry("PackedColorArray", "PackedColorArray"));

	private static Map<String, ClassInfo> engineClasses = Map.of();

	private TypedCallSupport() {
	}

	static void configure(Map<String, ClassInfo> classMap) {
		engineClasses = Map.copyOf(classMap);
	}

	static String helperName(MethodInfo method, boolean isStatic) {
		String prefix = isStatic ? "callStatic" : "callEngine";
		String type = method.returnType();
		String meta = method.returnMeta();
		if (type == null || type.isEmpty()) {
			return prefix + "Void";
		}
		if (type.startsWith("enum::") || type.startsWith("bitfield::")) {
			return prefix + "Int32";
		}
		if ("bool".equals(type)) {
			return prefix + "Bool";
		}
		if ("int".equals(type)) {
			if ("int8".equals(meta)) {
				return prefix + "Int8";
			}
			if ("uint8".equals(meta)) {
				return prefix + "Uint8";
			}
			if ("int16".equals(meta)) {
				return prefix + "Int16";
			}
			if ("uint16".equals(meta)) {
				return prefix + "Uint16";
			}
			if ("int32".equals(meta)) {
				return prefix + "Int32";
			}
			if ("uint32".equals(meta)) {
				return prefix + "Uint32";
			}
			if ("uint64".equals(meta)) {
				return prefix + "Uint64";
			}
			if (meta == null || meta.isEmpty() || "int64".equals(meta)) {
				return prefix + "Int64";
			}
		}
		if ("float".equals(type)) {
			if ("float".equals(meta) || "float32".equals(meta)) {
				return prefix + "Float32";
			}
			if (meta == null || meta.isEmpty() || "double".equals(meta) || "float64".equals(meta)) {
				return prefix + "Float64";
			}
		}
		if ("RID".equals(type)) {
			return prefix + "Rid";
		}
		if ("String".equals(type)) {
			return prefix + "String";
		}
		if ("StringName".equals(type)) {
			return prefix + "StringName";
		}
		if ("NodePath".equals(type)) {
			return prefix + "NodePath";
		}
		if ("Variant".equals(type)) {
			return prefix + "Variant";
		}
		String packedArrayHelper = PACKED_ARRAY_HELPERS.get(type);
		if (packedArrayHelper != null) {
			return prefix + packedArrayHelper;
		}
		String builtinHelper = BUILTIN_STRUCT_HELPERS.get(type);
		if (builtinHelper != null) {
			return prefix + builtinHelper;
		}
		if (isEngineClass(type)) {
			return prefix + "Object";
		}
		return null;
	}

	static boolean argumentsSupported(MethodInfo method) {
		for (ArgInfo arg : method.arguments()) {
			if (argExpression(arg, "unused") == null) {
				return false;
			}
		}
		return true;
	}

	static String callArgs(MethodInfo method, List<String> paramNames) {
		if (method.arguments().isEmpty()) {
			return "";
		}
		StringBuilder args = new StringBuilder();
		for (int i = 0; i < method.arguments().size(); i++) {
			args.append(", ");
			args.append(argExpression(method.arguments().get(i), paramNames.get(i)));
		}
		return args.toString();
	}

	static String argExpression(ArgInfo arg, String paramName) {
		String type = arg.type();
		String meta = arg.meta();
		if (type.startsWith("enum::") || type.startsWith("bitfield::")) {
			return "java.lang.Integer.valueOf(" + paramName + ")";
		}
		if ("bool".equals(type)) {
			return "java.lang.Boolean.valueOf(" + paramName + ")";
		}
		if ("int".equals(type)) {
			if ("int8".equals(meta)) {
				return "typedInt8Arg(" + paramName + ")";
			}
			if ("uint8".equals(meta)) {
				return "typedUint8Arg(" + paramName + ")";
			}
			if ("int16".equals(meta)) {
				return "typedInt16Arg(" + paramName + ")";
			}
			if ("uint16".equals(meta)) {
				return "typedUint16Arg(" + paramName + ")";
			}
			if ("int32".equals(meta)) {
				return "java.lang.Integer.valueOf((int) " + paramName + ")";
			}
			if ("uint32".equals(meta)) {
				return "typedUint32Arg(" + paramName + ")";
			}
			if ("uint64".equals(meta)) {
				return "typedUint64Arg(" + paramName + ")";
			}
			if (meta == null || meta.isEmpty() || "int64".equals(meta)) {
				return "java.lang.Long.valueOf(" + paramName + ")";
			}
		}
		if ("float".equals(type)) {
			if ("float".equals(meta) || "float32".equals(meta)) {
				return "java.lang.Float.valueOf((float) " + paramName + ")";
			}
			if (meta == null || meta.isEmpty() || "double".equals(meta) || "float64".equals(meta)) {
				return "java.lang.Double.valueOf(" + paramName + ")";
			}
		}
		if ("RID".equals(type)) {
			return "java.lang.Long.valueOf(" + paramName + ")";
		}
		if ("String".equals(type)) {
			return "typedStringArg(" + paramName + ")";
		}
		if ("StringName".equals(type)) {
			return "typedStringNameArg(" + paramName + ")";
		}
		if ("NodePath".equals(type)) {
			return "typedNodePathArg(" + paramName + ")";
		}
		if ("Variant".equals(type)) {
			return "typedVariantArg(" + paramName + ")";
		}
		if ("PackedByteArray".equals(type)) {
			return "typedPackedByteArrayArg(" + paramName + ")";
		}
		if ("PackedInt32Array".equals(type)) {
			return "typedPackedInt32ArrayArg(" + paramName + ")";
		}
		if ("PackedInt64Array".equals(type)) {
			return "typedPackedInt64ArrayArg(" + paramName + ")";
		}
		if ("PackedFloat32Array".equals(type)) {
			return "typedPackedFloat32ArrayArg(" + paramName + ")";
		}
		if ("PackedFloat64Array".equals(type)) {
			return "typedPackedFloat64ArrayArg(" + paramName + ")";
		}
		if ("PackedStringArray".equals(type)) {
			return "typedPackedStringArrayArg(" + paramName + ")";
		}
		if ("PackedVector2Array".equals(type)) {
			return "typedPackedVector2ArrayArg(" + paramName + ")";
		}
		if ("PackedVector3Array".equals(type)) {
			return "typedPackedVector3ArrayArg(" + paramName + ")";
		}
		if ("PackedColorArray".equals(type)) {
			return "typedPackedColorArrayArg(" + paramName + ")";
		}
		if ("Array".equals(type)) {
			return "typedArrayArg(" + paramName + ")";
		}
		if ("Dictionary".equals(type)) {
			return "typedDictionaryArg(" + paramName + ")";
		}
		if ("Callable".equals(type)) {
			return "typedCallableArg(" + paramName + ")";
		}
		if ("Signal".equals(type)) {
			return "typedSignalArg(" + paramName + ")";
		}
		if (BUILTIN_STRUCT_TYPES.contains(type)) {
			return paramName;
		}
		if (isEngineClass(type)) {
			return "typedObjectArg(" + paramName + ", " + isRefCounted(type) + ")";
		}
		return null;
	}

	static String returnMetadataArgs(MethodInfo method) {
		if (isEngineClass(method.returnType())) {
			String type = normalizeClassName(method.returnType());
			return ", \"" + type + "\", " + isRefCounted(type);
		}
		return "";
	}

	static String fallbackReason(MethodInfo method) {
		if (method.isVararg()) {
			return "vararg";
		}
		if (helperName(method, method.isStatic()) == null) {
			return "unsupported-return:" + typeKey(method.returnType(), method.returnMeta());
		}
		for (ArgInfo arg : method.arguments()) {
			if (argExpression(arg, "unused") == null) {
				return "unsupported-arg:" + typeKey(arg.type(), arg.meta());
			}
		}
		return null;
	}

	static String propertyGetterFallbackReason(MethodInfo method, String propertyJavaType) {
		if (method == null) {
			return "unresolved";
		}
		String reason = fallbackReason(method);
		if (reason != null) {
			return reason;
		}
		String helper = helperName(method, method.isStatic());
		if (!returnCompatibleWithProperty(helper, propertyJavaType)) {
			return "incompatible-property-signature";
		}
		return null;
	}

	static String propertySetterFallbackReason(MethodInfo method, String propertyJavaType) {
		if (method == null) {
			return "unresolved";
		}
		String reason = fallbackReason(method);
		if (reason != null) {
			return reason;
		}
		if (method.arguments().size() != 1) {
			return "incompatible-property-signature";
		}
		if (!argumentCompatibleWithProperty(method.arguments().get(0), propertyJavaType)) {
			return "incompatible-property-signature";
		}
		return null;
	}

	private static boolean returnCompatibleWithProperty(String helper, String propertyJavaType) {
		if (helper == null) {
			return false;
		}
		if (helper.endsWith("Bool")) {
			return "boolean".equals(propertyJavaType);
		}
		if (helper.endsWith("Int8")) {
			return "byte".equals(propertyJavaType) || "long".equals(propertyJavaType);
		}
		if (helper.endsWith("Uint8")) {
			return "short".equals(propertyJavaType) || "long".equals(propertyJavaType);
		}
		if (helper.endsWith("Int16")) {
			return "short".equals(propertyJavaType) || "long".equals(propertyJavaType);
		}
		if (helper.endsWith("Uint16")) {
			return "int".equals(propertyJavaType) || "long".equals(propertyJavaType);
		}
		if (helper.endsWith("Int32")) {
			return "int".equals(propertyJavaType) || "long".equals(propertyJavaType);
		}
		if (helper.endsWith("Uint32")) {
			return "long".equals(propertyJavaType);
		}
		if (helper.endsWith("Uint64")) {
			return "java.math.BigInteger".equals(propertyJavaType);
		}
		if (helper.endsWith("Int64")) {
			return "long".equals(propertyJavaType);
		}
		if (helper.endsWith("Rid")) {
			return "long".equals(propertyJavaType);
		}
		if (helper.endsWith("String") || helper.endsWith("StringName") || helper.endsWith("NodePath")) {
			return "String".equals(propertyJavaType);
		}
		if (helper.endsWith("PackedByteArray")) {
			return "byte[]".equals(propertyJavaType);
		}
		if (helper.endsWith("PackedInt32Array")) {
			return "int[]".equals(propertyJavaType);
		}
		if (helper.endsWith("PackedInt64Array")) {
			return "long[]".equals(propertyJavaType);
		}
		if (helper.endsWith("PackedFloat32Array") || helper.endsWith("PackedFloat64Array")) {
			return "double[]".equals(propertyJavaType);
		}
		if (helper.endsWith("PackedStringArray")) {
			return "String[]".equals(propertyJavaType);
		}
		if (helper.endsWith("PackedVector2Array") || helper.endsWith("PackedVector3Array")
				|| helper.endsWith("PackedColorArray")) {
			return "double[][]".equals(propertyJavaType);
		}
		for (String builtinType : BUILTIN_STRUCT_TYPES) {
			if (helper.endsWith(builtinType)) {
				return builtinType.equals(propertyJavaType);
			}
		}
		if (helper.endsWith("Object")) {
			return isAssignableEngineReturnType(propertyJavaType);
		}
		if (helper.endsWith("Float32")) {
			return "float".equals(propertyJavaType) || "double".equals(propertyJavaType);
		}
		if (helper.endsWith("Float64")) {
			return "double".equals(propertyJavaType);
		}
		return false;
	}

	private static boolean argumentCompatibleWithProperty(ArgInfo arg, String propertyJavaType) {
		String type = arg.type();
		String meta = arg.meta();
		if ("bool".equals(type)) {
			return "boolean".equals(propertyJavaType);
		}
		if ("int".equals(type)) {
			if ("uint64".equals(meta)) {
				return "long".equals(propertyJavaType) || "java.math.BigInteger".equals(propertyJavaType);
			}
			return isIntegralJavaType(propertyJavaType)
					&& ("int8".equals(meta) || "uint8".equals(meta) || "int16".equals(meta)
							|| "uint16".equals(meta) || "int32".equals(meta) || "uint32".equals(meta)
							|| "int64".equals(meta) || meta == null || meta.isEmpty());
		}
		if ("float".equals(type)) {
			return isFloatingJavaType(propertyJavaType) && ("float".equals(meta) || "float32".equals(meta)
					|| "double".equals(meta) || "float64".equals(meta) || meta == null || meta.isEmpty());
		}
		if ("RID".equals(type)) {
			return "long".equals(propertyJavaType);
		}
		if ("String".equals(type) || "StringName".equals(type) || "NodePath".equals(type)) {
			return "String".equals(propertyJavaType);
		}
		if ("Callable".equals(type) || "Signal".equals(type)) {
			return type.equals(propertyJavaType);
		}
		if ("Array".equals(type)) {
			return "GodotArray".equals(propertyJavaType);
		}
		if ("Dictionary".equals(type)) {
			return "GodotDictionary".equals(propertyJavaType);
		}
		if ("PackedByteArray".equals(type)) {
			return "byte[]".equals(propertyJavaType);
		}
		if ("PackedInt32Array".equals(type)) {
			return "int[]".equals(propertyJavaType);
		}
		if ("PackedInt64Array".equals(type)) {
			return "long[]".equals(propertyJavaType);
		}
		if ("PackedFloat32Array".equals(type) || "PackedFloat64Array".equals(type)) {
			return "double[]".equals(propertyJavaType);
		}
		if ("PackedStringArray".equals(type)) {
			return "String[]".equals(propertyJavaType);
		}
		if ("PackedVector2Array".equals(type) || "PackedVector3Array".equals(type)
				|| "PackedColorArray".equals(type)) {
			return "double[][]".equals(propertyJavaType);
		}
		if (BUILTIN_STRUCT_TYPES.contains(type)) {
			return type.equals(propertyJavaType);
		}
		if (isEngineClass(type)) {
			return isAssignableEngineArgumentType(propertyJavaType);
		}
		return false;
	}

	private static boolean isEngineClass(String type) {
		return engineClasses.containsKey(normalizeClassName(type));
	}

	private static boolean isRefCounted(String type) {
		ClassInfo classInfo = engineClasses.get(normalizeClassName(type));
		return classInfo != null && classInfo.isRefcounted();
	}

	private static boolean isAssignableEngineReturnType(String javaType) {
		return "Object".equals(javaType) || isAssignableEngineArgumentType(javaType);
	}

	private static boolean isAssignableEngineArgumentType(String javaType) {
		String normalized = normalizeClassName(javaType);
		return "Godot".equals(javaType) || (!"Object".equals(normalized) && engineClasses.containsKey(normalized));
	}

	private static String normalizeClassName(String type) {
		if (type == null) {
			return "";
		}
		int colon = type.indexOf(':');
		if (colon >= 0 && colon + 1 < type.length()) {
			type = type.substring(colon + 1);
		}
		if (type.contains(".")) {
			type = type.substring(type.lastIndexOf('.') + 1);
		}
		return type;
	}

	private static boolean isIntegralJavaType(String javaType) {
		return "byte".equals(javaType) || "short".equals(javaType) || "int".equals(javaType) || "long".equals(javaType);
	}

	private static boolean isFloatingJavaType(String javaType) {
		return "float".equals(javaType) || "double".equals(javaType);
	}

	private static String typeKey(String type, String meta) {
		if (type == null || type.isEmpty()) {
			return "void";
		}
		return meta == null || meta.isEmpty() ? type : type + "|" + meta;
	}
}
