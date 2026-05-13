package com.godot.codegen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TypedCallCoverageReporter {

	private TypedCallCoverageReporter() {
	}

	static Report analyze(List<ClassInfo> classes) {
		Map<String, ClassInfo> classMap = new java.util.HashMap<>();
		for (ClassInfo classInfo : classes) {
			classMap.put(classInfo.name(), classInfo);
		}
		TypedCallSupport.configure(classMap);
		Report report = new Report();
		for (ClassInfo classInfo : classes) {
			for (MethodInfo method : classInfo.methods()) {
				report.methods++;
				String reason = TypedCallSupport.fallbackReason(method);
				if (reason == null) {
					report.typedMethods++;
				} else {
					report.dynamicMethods++;
					report.addReason(reason);
				}
			}
			for (PropertyInfo property : classInfo.properties()) {
				report.properties++;
				String getterName = property.getter();
				if (getterName != null && !getterName.isEmpty()) {
					report.propertyAccessors++;
					MethodInfo getter = findMethod(classes, classInfo, getterName);
					String reason = TypedCallSupport.propertyGetterFallbackReason(getter,
							TypeMapper.toJavaType(property.type()));
					if (reason == null) {
						report.typedPropertyAccessors++;
					} else {
						report.dynamicPropertyAccessors++;
						report.addReason("property-getter:" + reason);
					}
				}
				String setterName = property.setter();
				if (setterName != null && !setterName.isEmpty()) {
					report.propertyAccessors++;
					MethodInfo setter = findMethod(classes, classInfo, setterName);
					String reason = TypedCallSupport.propertySetterFallbackReason(setter,
							TypeMapper.toJavaType(property.type()));
					if (reason == null) {
						report.typedPropertyAccessors++;
					} else {
						report.dynamicPropertyAccessors++;
						report.addReason("property-setter:" + reason);
					}
				}
			}
		}
		return report;
	}

	private static MethodInfo findMethod(List<ClassInfo> classes, ClassInfo classInfo, String methodName) {
		String currentName = classInfo.name();
		while (currentName != null && !currentName.isEmpty()) {
			ClassInfo current = findClass(classes, currentName);
			if (current == null) {
				return null;
			}
			for (MethodInfo method : current.methods()) {
				if (method.name().equals(methodName)) {
					return method;
				}
			}
			currentName = current.inherits();
		}
		return null;
	}

	private static ClassInfo findClass(List<ClassInfo> classes, String className) {
		for (ClassInfo classInfo : classes) {
			if (classInfo.name().equals(className)) {
				return classInfo;
			}
		}
		return null;
	}

	static void print(Report report) {
		System.out.println("Typed wrapper call coverage:");
		System.out.println("  methods.total:              " + report.methods);
		System.out.println("  methods.typed:              " + report.typedMethods);
		System.out.println("  methods.dynamic:            " + report.dynamicMethods);
		System.out.println("  methods.typed.percent:      " + percent(report.typedMethods, report.methods));
		System.out.println("  property_accessors.total:   " + report.propertyAccessors);
		System.out.println("  property_accessors.typed:   " + report.typedPropertyAccessors);
		System.out.println("  property_accessors.dynamic: " + report.dynamicPropertyAccessors);
		System.out.println("  fallback.top_reasons:");
		for (Map.Entry<String, Integer> entry : report.topReasons(12)) {
			System.out.println("    " + entry.getKey() + ": " + entry.getValue());
		}
	}

	private static String percent(int part, int total) {
		if (total == 0) {
			return "0.00";
		}
		return String.format(java.util.Locale.ROOT, "%.2f", part * 100.0 / total);
	}

	static final class Report {
		int methods;
		int typedMethods;
		int dynamicMethods;
		int properties;
		int propertyAccessors;
		int typedPropertyAccessors;
		int dynamicPropertyAccessors;
		private final Map<String, Integer> fallbackReasons = new LinkedHashMap<>();

		void addReason(String reason) {
			fallbackReasons.merge(reason, 1, Integer::sum);
		}

		int reasonCount(String reason) {
			return fallbackReasons.getOrDefault(reason, 0);
		}

		List<Map.Entry<String, Integer>> topReasons(int limit) {
			List<Map.Entry<String, Integer>> entries = new ArrayList<>(fallbackReasons.entrySet());
			entries.sort(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
					.thenComparing(Map.Entry.comparingByKey()));
			return entries.subList(0, Math.min(limit, entries.size()));
		}
	}
}
