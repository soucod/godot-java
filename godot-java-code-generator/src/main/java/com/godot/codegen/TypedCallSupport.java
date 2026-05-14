package com.godot.codegen;

import java.util.List;
import java.util.Map;

final class TypedCallSupport {

	private static Map<String, ClassInfo> engineClasses = Map.of();

	private TypedCallSupport() {
	}

	static void configure(Map<String, ClassInfo> classMap) {
		engineClasses = Map.copyOf(classMap);
	}

	static String helperName(MethodInfo method, boolean isStatic) {
		String prefix = isStatic ? "callStatic" : "callEngine";
		TypedAbiModel.Descriptor descriptor = TypedAbiModel.descriptor(method.returnType(), method.returnMeta());
		if (descriptor != null && descriptor.supportsReturn()) {
			return prefix + descriptor.returnHelperSuffix();
		}
		if (isEngineClass(method.returnType())) {
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
		TypedAbiModel.Descriptor descriptor = TypedAbiModel.descriptor(type, arg.meta());
		if (descriptor != null && descriptor.supportsArgument()) {
			return descriptor.arg(paramName);
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
		if (!returnCompatibleWithProperty(method, propertyJavaType)) {
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

	private static boolean returnCompatibleWithProperty(MethodInfo method, String propertyJavaType) {
		TypedAbiModel.Descriptor descriptor = TypedAbiModel.descriptor(method.returnType(), method.returnMeta());
		if (descriptor != null && descriptor.supportsReturn()) {
			return descriptor.returnCompatibleWithProperty(propertyJavaType);
		}
		if (isEngineClass(method.returnType())) {
			return isAssignableEngineReturnType(propertyJavaType);
		}
		return false;
	}

	private static boolean argumentCompatibleWithProperty(ArgInfo arg, String propertyJavaType) {
		String type = arg.type();
		String meta = arg.meta();
		TypedAbiModel.Descriptor descriptor = TypedAbiModel.descriptor(type, meta);
		if (descriptor != null && descriptor.supportsArgument()) {
			return descriptor.argumentCompatibleWithProperty(propertyJavaType);
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

	private static String typeKey(String type, String meta) {
		if (type == null || type.isEmpty()) {
			return "void";
		}
		return meta == null || meta.isEmpty() ? type : type + "|" + meta;
	}
}
