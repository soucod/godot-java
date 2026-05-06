package com.godot.codegen;

import com.squareup.javapoet.*;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import javax.lang.model.element.Modifier;

/**
 * Generates Java wrapper classes from class information.
 *
 * Generates ALL engine API classes including Object, RefCounted, Node, etc.
 * Each generated class includes: - GODOT_CLASS_NAME constant -
 * getGodotClassName() override - METHOD_HASHES map (embedded) - create()
 * factory for instantiable classes - Nested enums from class enum definitions -
 * Public constructors - Method wrappers and property accessors
 */
public class JavaClassGenerator {

	private final String packageName;
	private final Map<String, ClassInfo> classMap;

	public JavaClassGenerator(String packageName, List<ClassInfo> classes) {
		this.packageName = packageName;
		this.classMap = new HashMap<>();
		for (ClassInfo cls : classes) {
			classMap.put(cls.name(), cls);
		}
	}

	public JavaFile generateClass(ClassInfo classInfo) {
		return buildClass(classInfo).build();
	}

	private JavaFile.Builder buildClass(ClassInfo classInfo) {
		TypeSpec.Builder classBuilder = buildClassHeader(classInfo);

		addGodotClassName(classBuilder, classInfo);
		addConstructors(classBuilder, classInfo);
		addMethodHashTable(classBuilder, classInfo);
		addEnums(classBuilder, classInfo);
		addMethods(classBuilder, classInfo);
		addProperties(classBuilder, classInfo);
		addCreateFactory(classBuilder, classInfo);
		addGetGodotClassName(classBuilder, classInfo);
		addResolveMethodHash(classBuilder, classInfo);
		addConvenienceMethods(classBuilder, classInfo);

		JavaFile.Builder fileBuilder = JavaFile.builder(packageName, classBuilder.build()).skipJavaLangImports(true)
				.indent("\t");
		return fileBuilder;
	}

	/**
	 * Add hand-written convenience methods to specific generated classes. These are
	 * typed helper methods not derived from the Godot API but improve Java
	 * ergonomics by providing typed access to common patterns.
	 */
	private void addConvenienceMethods(TypeSpec.Builder builder, ClassInfo classInfo) {
		String name = classInfo.name();

		if ("Node".equals(name)) {
			ClassName nodeType = ClassName.get(packageName, "Node");
			ClassName objectType = ClassName.get(packageName, "Object");

			builder.addMethod(MethodSpec.methodBuilder("getNodeAs").addModifiers(Modifier.PUBLIC)
					.addTypeVariable(TypeVariableName.get("T", objectType)).returns(TypeVariableName.get("T"))
					.addParameter(String.class, "path")
					.addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("T")),
							"type")
					.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unchecked")
							.build())
					.addStatement("$T node = getNode(path)", nodeType).addStatement("if (node == null) return null")
					.addStatement("return ($T) node", TypeVariableName.get("T")).build());
		}

		if ("PackedScene".equals(name)) {
			ClassName nodeType = ClassName.get(packageName, "Node");

			builder.addMethod(MethodSpec.methodBuilder("instantiateAs").addModifiers(Modifier.PUBLIC)
					.addTypeVariable(TypeVariableName.get("T", nodeType)).returns(TypeVariableName.get("T"))
					.addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("T")),
							"type")
					.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unchecked")
							.build())
					.addStatement("$T node = instantiate()", nodeType).addStatement("if (node == null) return null")
					.addStatement("return ($T) node", TypeVariableName.get("T")).build());
		}
	}

	/**
	 * Build class header with proper extends clause.
	 *
	 * - Object (no parent) extends org.godot.Godot - RefCounted (parent: Object)
	 * extends org.godot.node.Object - All others extend their parent in
	 * org.godot.node
	 */
	private TypeSpec.Builder buildClassHeader(ClassInfo classInfo) {
		ClassName className = ClassName.get(packageName, classInfo.name());
		TypeSpec.Builder builder = TypeSpec.classBuilder(className);
		builder.addModifiers(Modifier.PUBLIC);

		String parentClass = classInfo.getSimpleParentClass();
		if (parentClass != null && !parentClass.isEmpty()) {
			builder.superclass(superClassRef(parentClass));
		} else {
			// Root class (Object) or classes with no parent -> extends Godot
			builder.superclass(ClassName.get("org.godot", "Godot"));
		}

		return builder;
	}

	/**
	 * Add GODOT_CLASS_NAME static constant.
	 */
	private void addGodotClassName(TypeSpec.Builder builder, ClassInfo classInfo) {
		builder.addField(
				FieldSpec.builder(String.class, "GODOT_CLASS_NAME", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
						.initializer("$S", classInfo.name()).build());
	}

	/**
	 * Add getGodotClassName() override.
	 */
	private void addGetGodotClassName(TypeSpec.Builder builder, ClassInfo classInfo) {
		builder.addMethod(MethodSpec.methodBuilder("getGodotClassName").addModifiers(Modifier.PUBLIC)
				.returns(String.class).addAnnotation(Override.class).addStatement("return GODOT_CLASS_NAME").build());
	}

	/**
	 * Add resolveMethodHash() override - the core of zero-reflection dispatch.
	 * Checks this class's METHOD_HASHES, then delegates to super.
	 */
	private void addResolveMethodHash(TypeSpec.Builder builder, ClassInfo classInfo) {
		builder.addMethod(MethodSpec.methodBuilder("resolveMethodHash").addModifiers(Modifier.PROTECTED)
				.returns(ClassName.get("org.godot", "Godot").nestedClass("HashResult")).addAnnotation(Override.class)
				.addParameter(String.class, "methodName")
				.addStatement("$T hash = METHOD_HASHES.get(methodName)", Long.class)
				.addStatement("if (hash != null) return new $T(hash, GODOT_CLASS_NAME)",
						ClassName.get("org.godot", "Godot").nestedClass("HashResult"))
				.addStatement("return super.resolveMethodHash(methodName)").build());
	}

	/**
	 * Add public constructors.
	 */
	private void addConstructors(TypeSpec.Builder builder, ClassInfo classInfo) {
		// Constructor from MemorySegment
		builder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
				.addParameter(MemorySegment.class, "nativePointer").addStatement("super(nativePointer)").build());

		// Constructor from long address
		builder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
				.addParameter(long.class, "nativePointer").addStatement("super(nativePointer)").build());

		// No-arg constructor for @GodotClass subclass instantiation
		builder.addMethod(
				MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).addStatement("super()").build());
	}

	/**
	 * Add create() factory method for instantiable classes.
	 */
	private void addCreateFactory(TypeSpec.Builder builder, ClassInfo classInfo) {
		if (!classInfo.isInstantiable()) {
			return;
		}

		// Check if the class has a method named "create" that would conflict
		boolean hasCreateNoArgs = false;
		for (MethodInfo method : classInfo.methods()) {
			if (method.name().equals("create") && method.arguments().isEmpty()) {
				hasCreateNoArgs = true;
				break;
			}
		}

		String factoryName = hasCreateNoArgs ? "createNew" : "create";
		ClassName returnType = ClassName.get(packageName, classInfo.name());

		builder.addMethod(
				MethodSpec.methodBuilder(factoryName).addModifiers(Modifier.PUBLIC, Modifier.STATIC).returns(returnType)
						.addStatement("$T name = $T.fromJavaString(GODOT_CLASS_NAME)",
								ClassName.get("org.godot.core", "GodotStringName"),
								ClassName.get("org.godot.core", "GodotStringName"))
						.addStatement("$T ptr = $T.callPtr($T.CLASSDB_CONSTRUCT_OBJECT, name.segment())",
								MemorySegment.class, ClassName.get("org.godot.bridge", "Bridge"),
								ClassName.get("org.godot.internal.api", "ApiIndex"))
						.addStatement("return new $T(ptr)", returnType).build());
	}

	/**
	 * Add nested enums from class enum definitions.
	 */
	private void addEnums(TypeSpec.Builder builder, ClassInfo classInfo) {
		for (EnumInfo enumInfo : classInfo.enums()) {
			TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(enumInfo.name()).addModifiers(Modifier.PUBLIC);

			// Value field
			enumBuilder.addField(FieldSpec
					.builder(enumInfo.isBitfield() ? long.class : int.class, "value", Modifier.PUBLIC, Modifier.FINAL)
					.build());

			// Constructor
			enumBuilder.addMethod(MethodSpec.constructorBuilder()
					.addParameter(enumInfo.isBitfield() ? long.class : int.class, "value")
					.addStatement("this.value = value").build());

			// Enum constants - use int for enums, long for bitfields
			for (EnumValue ev : enumInfo.values()) {
				String literal = enumInfo.isBitfield() ? ev.value() + "L" : String.valueOf((int) ev.value());
				enumBuilder.addEnumConstant(ev.name(), TypeSpec.anonymousClassBuilder("$L", literal).build());
			}

			builder.addType(enumBuilder.build());
		}
	}

	/**
	 * Add methods from the method info.
	 */
	private void addMethods(TypeSpec.Builder builder, ClassInfo classInfo) {
		// Track generated method signatures to avoid duplicates
		Set<String> generatedSignatures = new java.util.HashSet<>();

		// Build set of property Java accessor names (getXxx, setXxx, isXxx)
		// to avoid generating duplicate methods when a Godot method name
		// converts to the same Java name as a property accessor.
		Set<String> propertyJavaAccessorNames = new java.util.HashSet<>();

		for (PropertyInfo prop : classInfo.properties()) {
			String javaPropName = toJavaPropertyName(prop.name());
			String capitalized = Character.toUpperCase(javaPropName.charAt(0)) + javaPropName.substring(1);
			if (prop.getter() != null && !prop.getter().isEmpty()) {
				propertyJavaAccessorNames.add("get" + capitalized);
				propertyJavaAccessorNames.add("is" + capitalized);
			}
			if (prop.setter() != null && !prop.setter().isEmpty()) {
				propertyJavaAccessorNames.add("set" + capitalized);
			}
		}

		for (MethodInfo method : classInfo.methods()) {
			// Skip virtual methods (prefixed with _)
			if (method.name().startsWith("_") || method.isVirtual()) {
				continue;
			}

			// Skip call/call_deferred - Godot.call() already handles dynamic
			// dispatch; generating a wrapper intercepts single-arg call("method")
			// away from Godot.call(String, Object...) into the broken vararg
			// method bind path
			if (method.name().equals("call") || method.name().equals("call_deferred")) {
				continue;
			}

			String javaMethodName = toJavaMethodName(method.name());

			// Skip methods whose Java name collides with a property accessor
			if (propertyJavaAccessorNames.contains(javaMethodName)) {
				continue;
			}

			String returnType = getReturnType(method);

			// Build parameter list
			List<ParameterSpec> params = new ArrayList<>();
			List<String> paramNames = new ArrayList<>();
			List<String> paramTypes = new ArrayList<>();
			for (int i = 0; i < method.arguments().size(); i++) {
				ArgInfo arg = method.arguments().get(i);
				String javaType = TypeMapper.toJavaType(arg.type());
				String javaName = toJavaParamName(arg.name());
				params.add(ParameterSpec.builder(toTypeName(javaType), javaName).build());
				paramNames.add(javaName);
				paramTypes.add(javaType);
			}

			// Check for duplicate method signatures
			String sig = javaMethodName + "(" + String.join(",", paramTypes) + ")";
			if (!generatedSignatures.add(sig)) {
				continue;
			}

			// Build call arguments with proper boxing
			String callArgs;
			if (paramNames.isEmpty()) {
				callArgs = "";
			} else {
				StringBuilder args = new StringBuilder();
				for (int i = 0; i < paramNames.size(); i++) {
					if (args.length() > 0)
						args.append(", ");
					args.append(boxToObject(paramNames.get(i), paramTypes.get(i)));
				}
				callArgs = ", new java.lang.Object[] { " + args + " }";
			}

			MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(javaMethodName).addParameters(params);

			if (method.isStatic()) {
				// Static method: use callStatic with class name and hash
				methodBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
				if (!returnType.equals("void")) {
					methodBuilder.returns(toTypeName(returnType));
					methodBuilder.addStatement("return ($T) callStatic($S, $S, $L$L)", toTypeName(returnType),
							classInfo.name(), method.name(), method.hash() + "L", callArgs);
				} else {
					methodBuilder.addStatement("callStatic($S, $S, $L$L)", classInfo.name(), method.name(),
							method.hash() + "L", callArgs);
				}
			} else {
				methodBuilder.addModifiers(Modifier.PUBLIC);
				if (!returnType.equals("void")) {
					methodBuilder.returns(toTypeName(returnType));
					methodBuilder.addStatement("return ($T) super.call($S$L)", toTypeName(returnType), method.name(),
							callArgs);
				} else {
					methodBuilder.addStatement("super.call($S$L)", method.name(), callArgs);
				}
			}

			builder.addMethod(methodBuilder.build());

			// Generate overloads for trailing default-value parameters
			generateDefaultOverloads(builder, classInfo, method, javaMethodName, returnType, params, paramNames,
					paramTypes, generatedSignatures);
		}
	}

	/**
	 * Generate convenience overloads by progressively omitting trailing parameters
	 * that have default values.
	 */
	private void generateDefaultOverloads(TypeSpec.Builder builder, ClassInfo classInfo, MethodInfo method,
			String javaMethodName, String returnType, List<ParameterSpec> fullParams, List<String> paramNames,
			List<String> paramTypes, Set<String> generatedSignatures) {
		List<ArgInfo> args = method.arguments();
		if (args.isEmpty()) {
			return;
		}

		// Find the first trailing argument with a default value
		int firstDefaultIdx = -1;
		for (int i = args.size() - 1; i >= 0; i--) {
			if (args.get(i).defaultValue() != null) {
				firstDefaultIdx = i;
			} else {
				break;
			}
		}
		if (firstDefaultIdx < 0) {
			return;
		}

		// Generate overloads from removing 1, 2, ... trailing defaults
		for (int cutAt = args.size() - 1; cutAt >= firstDefaultIdx; cutAt--) {
			int overloadArgCount = cutAt;

			// Skip this overload if any omitted default value can't be expressed in Java
			// (e.g., null for primitive types like long, or 0 for Godot class types)
			boolean skipOverload = false;
			for (int i = overloadArgCount; i < args.size(); i++) {
				String defVal = defaultValueToJava(args.get(i), paramTypes.get(i));
				String paramType = paramTypes.get(i);
				if ("null".equals(defVal) && isPrimitiveType(paramType)) {
					skipOverload = true;
					break;
				}
				if ("null".equals(defVal) && !isPrimitiveType(paramType) && !"String".equals(paramType)) {
					// null is fine for reference types (except String which maps to java String)
				}
				if (!"null".equals(defVal) && !isPrimitiveType(paramType) && !"String".equals(paramType)
						&& !"double".equals(paramType) && !"float".equals(paramType)) {
					// Numeric default for a Godot class type — can't convert
					if (defVal.matches("-?\\d+L?") || defVal.matches("-?\\d+\\.\\d*f?")) {
						skipOverload = true;
						break;
					}
				}
			}
			if (skipOverload) {
				continue;
			}

			List<ParameterSpec> overloadParams = new ArrayList<>();
			List<String> overloadTypes = new ArrayList<>();
			for (int i = 0; i < overloadArgCount; i++) {
				overloadParams.add(fullParams.get(i));
				overloadTypes.add(paramTypes.get(i));
			}

			String overloadSig = javaMethodName + "(" + String.join(",", overloadTypes) + ")";
			if (!generatedSignatures.add(overloadSig)) {
				continue;
			}

			StringBuilder delegateArgs = new StringBuilder();
			for (int i = 0; i < overloadArgCount; i++) {
				if (delegateArgs.length() > 0)
					delegateArgs.append(", ");
				delegateArgs.append(paramNames.get(i));
			}
			for (int i = overloadArgCount; i < args.size(); i++) {
				if (delegateArgs.length() > 0)
					delegateArgs.append(", ");
				delegateArgs.append(defaultValueToJava(args.get(i), paramTypes.get(i)));
			}

			MethodSpec.Builder overloadBuilder = MethodSpec.methodBuilder(javaMethodName).addParameters(overloadParams);
			if (method.isStatic()) {
				overloadBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
			} else {
				overloadBuilder.addModifiers(Modifier.PUBLIC);
			}

			String delegateCall = javaMethodName + "(" + delegateArgs + ")";
			if (!returnType.equals("void")) {
				overloadBuilder.returns(toTypeName(returnType));
				overloadBuilder.addStatement("return " + delegateCall);
			} else {
				overloadBuilder.addStatement(delegateCall);
			}

			builder.addMethod(overloadBuilder.build());
		}
	}

	private boolean isPrimitiveType(String javaType) {
		return switch (javaType) {
			case "boolean", "byte", "short", "int", "long", "float", "double", "char" -> true;
			default -> false;
		};
	}

	private String defaultValueToJava(ArgInfo arg, String javaType) {
		String dv = arg.defaultValue();
		if (dv == null) {
			return "null";
		}
		switch (dv) {
			case "false" :
				return "false";
			case "true" :
				return "true";
			case "null" :
				return "null";
			default :
				break;
		}
		if (dv.matches("-?\\d+")) {
			switch (javaType) {
				case "long" :
					return dv + "L";
				case "int" :
					return dv;
				case "short" :
					return "(short)" + dv;
				case "byte" :
					return "(byte)" + dv;
				default :
					return dv;
			}
		}
		if (dv.matches("-?\\d+\\.\\d*")) {
			switch (javaType) {
				case "double" :
					return dv;
				case "float" :
					return dv + "f";
				default :
					return dv;
			}
		}
		if (dv.startsWith("\"") && dv.endsWith("\"")) {
			return dv;
		}
		if (dv.endsWith("()")) {
			switch (javaType) {
				case "byte[]" :
					return "new byte[0]";
				case "int[]" :
					return "new int[0]";
				case "long[]" :
					return "new long[0]";
				case "double[]" :
					return "new double[0]";
				case "String[]" :
					return "new String[0]";
				case "double[][]" :
					return "new double[0][]";
				default :
					return "null";
			}
		}
		return "null";
	}

	/**
	 * Build a map of all accessor Java names to their Java return types by walking
	 * up the inheritance chain. Includes both property accessors and regular
	 * methods to detect parent/child conflicts.
	 */
	private Map<String, String> buildParentAccessorTypes(ClassInfo classInfo) {
		Map<String, String> result = new HashMap<>();
		String parentName = classInfo.inherits();
		while (parentName != null && !parentName.isEmpty()) {
			ClassInfo parent = classMap.get(parentName);
			if (parent == null)
				break;
			// Collect property accessor types
			for (PropertyInfo prop : parent.properties()) {
				String propJavaName = toJavaPropertyName(prop.name());
				String propJavaType = toSingleJavaType(prop.type());
				String capitalized = capitalize(propJavaName);
				String getterMethod = prop.getter();
				if (getterMethod != null && getterMethod.startsWith("is_")) {
					result.putIfAbsent("is" + capitalized, propJavaType);
				} else {
					result.putIfAbsent("get" + capitalized, propJavaType);
				}
				result.putIfAbsent("set" + capitalized, propJavaType);
			}
			// Collect method return types (to detect method-property conflicts)
			for (MethodInfo m : parent.methods()) {
				String javaName = toJavaMethodName(m.name());
				String javaRetType = getReturnType(m);
				if (javaRetType != null && !javaRetType.equals("void")) {
					result.putIfAbsent(javaName, javaRetType);
				}
			}
			parentName = parent.inherits();
		}
		return result;
	}

	/**
	 * Add property getter/setter methods.
	 */
	private void addProperties(TypeSpec.Builder builder, ClassInfo classInfo) {
		Map<String, String> parentAccessorTypes = buildParentAccessorTypes(classInfo);

		for (PropertyInfo prop : classInfo.properties()) {
			String javaPropName = toJavaPropertyName(prop.name());
			String javaType = toSingleJavaType(prop.type());

			// Determine getter method name
			String getterMethod = prop.getter();
			if (getterMethod == null || getterMethod.isEmpty()) {
				getterMethod = "get_" + prop.name();
				if (javaType.equals("boolean")) {
					getterMethod = "is_" + prop.name();
				}
			}

			String getterName = "get" + capitalize(javaPropName);
			if (getterMethod.startsWith("is_")) {
				getterName = "is" + capitalize(javaPropName);
			}

			// Skip if parent has same accessor name with different type
			String parentGetterType = parentAccessorTypes.get(getterName);
			if (parentGetterType != null && !parentGetterType.equals(javaType)) {
				continue;
			}

			MethodSpec getterSpec = MethodSpec.methodBuilder(getterName).addModifiers(Modifier.PUBLIC)
					.returns(toTypeName(javaType)).addStatement("return ($T) super.call($S, new java.lang.Object[0])",
							toTypeName(javaType), getterMethod)
					.build();
			builder.addMethod(getterSpec);

			// Determine setter method name
			String setterMethod = prop.setter();
			if (setterMethod == null || setterMethod.isEmpty()) {
				setterMethod = "set_" + prop.name();
			}

			String setterName = "set" + capitalize(javaPropName);

			MethodSpec setterSpec = MethodSpec.methodBuilder(setterName).addModifiers(Modifier.PUBLIC)
					.addParameter(toTypeName(javaType), "value")
					.addStatement("super.call($S, new java.lang.Object[] { " + boxToObject("value", javaType) + " })",
							setterMethod)
					.build();
			builder.addMethod(setterSpec);
		}
	}

	/**
	 * Add method hash lookup table embedded directly in the class.
	 */
	private void addMethodHashTable(TypeSpec.Builder builder, ClassInfo classInfo) {
		Map<String, Long> hashMapEntries = new HashMap<>();
		for (MethodInfo method : classInfo.methods()) {
			if (!method.name().startsWith("_") && method.hash() != 0) {
				hashMapEntries.put(method.name(), method.hash());
			}
		}

		if (!hashMapEntries.isEmpty()) {
			String[] entries = hashMapEntries.entrySet().stream()
					.map(e -> "java.util.Map.entry(\"" + e.getKey() + "\", " + e.getValue() + "L)")
					.toArray(String[]::new);
			String initExpr = "java.util.Map.ofEntries(" + String.join(", ", entries) + ")";

			builder.addField(FieldSpec
					.builder(
							ParameterizedTypeName.get(ClassName.get("java.util", "Map"),
									ClassName.get("java.lang", "String"), ClassName.get("java.lang", "Long")),
							"METHOD_HASHES", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
					.initializer(initExpr).build());
		} else {
			// Empty hash map for classes with no methods
			builder.addField(FieldSpec
					.builder(
							ParameterizedTypeName.get(ClassName.get("java.util", "Map"),
									ClassName.get("java.lang", "String"), ClassName.get("java.lang", "Long")),
							"METHOD_HASHES", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
					.initializer("java.util.Map.of()").build());
		}
	}

	// ------------------------------------------------------------------
	// Type resolution
	// ------------------------------------------------------------------

	private static final Map<String, String> CROSS_PACKAGE_TYPES = Map.ofEntries(
			// Root package
			Map.entry("Godot", "org.godot"),
			// java.lang (needed to avoid shadowing when generating org.godot.node.Object)
			Map.entry("java.lang.Object", "java.lang"),
			// Math package
			Map.entry("Vector2", "org.godot.math"), Map.entry("Vector2i", "org.godot.math"),
			Map.entry("Vector3", "org.godot.math"), Map.entry("Vector3i", "org.godot.math"),
			Map.entry("Vector4", "org.godot.math"), Map.entry("Vector4i", "org.godot.math"),
			Map.entry("Rect2", "org.godot.math"), Map.entry("Rect2i", "org.godot.math"),
			Map.entry("Plane", "org.godot.math"), Map.entry("Quaternion", "org.godot.math"),
			Map.entry("AABB", "org.godot.math"), Map.entry("Basis", "org.godot.math"),
			Map.entry("Transform2D", "org.godot.math"), Map.entry("Transform3D", "org.godot.math"),
			Map.entry("Projection", "org.godot.math"), Map.entry("Color", "org.godot.math"),
			// Core package
			Map.entry("Callable", "org.godot.core"), Map.entry("Signal", "org.godot.core"),
			Map.entry("GodotString", "org.godot.core"), Map.entry("GodotStringName", "org.godot.core"),
			Map.entry("GodotVariant", "org.godot.core"), // Collection package
			Map.entry("GodotArray", "org.godot.collection"), Map.entry("GodotDictionary", "org.godot.collection"));

	private ClassName superClassRef(String godotParentName) {
		// All engine classes (Object, RefCounted, Node, etc.) are in org.godot.node
		return ClassName.get(packageName, godotParentName);
	}

	private TypeName toTypeName(String type) {
		if (type == null || type.isEmpty()) {
			return ClassName.get("java.lang", "Object");
		}

		if (type.endsWith("[]")) {
			String component = type.substring(0, type.length() - 2);
			return ArrayTypeName.of(toTypeName(component));
		}

		// Cross-package type (Vector2, Color, etc.)
		if (CROSS_PACKAGE_TYPES.containsKey(type)) {
			return ClassName.get(CROSS_PACKAGE_TYPES.get(type), type);
		}

		// "Object" from TypeMapper (Variant return) -> java.lang.Object
		if (type.equals("Object")) {
			return ClassName.get("java.lang", "Object");
		}

		return ClassName.get("", type);
	}

	// ------------------------------------------------------------------
	// Name conversion utilities
	// ------------------------------------------------------------------

	private String toJavaMethodName(String godotName) {
		// Methods from java.lang.Object that cannot be overridden
		if (godotName.equals("wait") || godotName.equals("notify") || godotName.equals("notifyAll")
				|| godotName.equals("equals") || godotName.equals("hashCode") || godotName.equals("toString")
				|| godotName.equals("get_class")) {
			return godotName + "_";
		}

		// Convert snake_case to camelCase for Java convention
		String name = godotName;
		if (godotName.contains("_")) {
			StringBuilder result = new StringBuilder();
			boolean nextUpper = false;
			for (char c : godotName.toCharArray()) {
				if (c == '_') {
					nextUpper = true;
				} else if (nextUpper) {
					result.append(Character.toUpperCase(c));
					nextUpper = false;
				} else {
					result.append(c);
				}
			}
			name = result.toString();
		}

		if (JAVA_KEYWORDS.contains(name)) {
			return name + "_";
		}
		return name;
	}

	private String toJavaPropertyName(String godotName) {
		if (godotName.startsWith("theme/")) {
			godotName = godotName.substring(6);
		}
		return toJavaMethodName(godotName);
	}

	private static final Set<String> JAVA_KEYWORDS = Set.of("abstract", "assert", "boolean", "break", "byte", "case",
			"catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends",
			"final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface",
			"long", "native", "new", "package", "private", "protected", "public", "return", "short", "static",
			"strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
			"volatile", "while", "true", "false", "null", "wait", "notify", "notifyAll");

	private String toJavaParamName(String name) {
		if (name == null || name.isEmpty()) {
			return "value";
		}
		String result = name;
		if (Character.isUpperCase(result.charAt(0))) {
			result = Character.toLowerCase(result.charAt(0)) + result.substring(1);
		}
		if (JAVA_KEYWORDS.contains(result)) {
			return result + "_";
		}
		if (!result.isEmpty() && !Character.isJavaIdentifierStart(result.charAt(0))) {
			return "p" + result;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < result.length(); i++) {
			char c = result.charAt(i);
			if (Character.isJavaIdentifierPart(c)) {
				sb.append(c);
			} else {
				sb.append('_');
			}
		}
		result = sb.toString();
		if (result.isEmpty()) {
			result = "p";
		}
		return result;
	}

	private String getReturnType(MethodInfo method) {
		if (method.returnType() == null || method.returnType().isEmpty()) {
			return "void";
		}
		String type = method.returnMeta() != null && !method.returnMeta().isEmpty()
				? TypeMapper.toJavaTypeFromMeta(method.returnMeta())
				: null;
		return type != null ? type : TypeMapper.toJavaType(method.returnType());
	}

	private String toSingleJavaType(String godotType) {
		if (godotType == null || godotType.isEmpty()) {
			return "Object";
		}
		if (godotType.contains(",")) {
			return "Object";
		}
		return TypeMapper.toJavaType(godotType);
	}

	private String boxToObject(String varName, String javaType) {
		switch (javaType) {
			case "long" :
				return "java.lang.Long.valueOf(" + varName + ")";
			case "boolean" :
				return "java.lang.Boolean.valueOf(" + varName + ")";
			case "double" :
				return "java.lang.Double.valueOf(" + varName + ")";
			case "int" :
				return "java.lang.Integer.valueOf(" + varName + ")";
			case "float" :
				return "java.lang.Float.valueOf(" + varName + ")";
			case "byte" :
				return "java.lang.Byte.valueOf(" + varName + ")";
			case "short" :
				return "java.lang.Short.valueOf(" + varName + ")";
			case "char" :
				return "java.lang.Character.valueOf(" + varName + ")";
			default :
				return "(java.lang.Object) " + varName;
		}
	}

	private String capitalize(String s) {
		if (s == null || s.isEmpty())
			return s;
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
