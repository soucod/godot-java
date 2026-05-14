package org.godot.annotation;

/**
 * Godot property usage flags. These control where and how a property appears
 * (editor inspector, storage, etc). Combine with bitwise OR.
 *
 * <p>
 * Used in the {@code usage} attribute of {@link Export &#64;Export}.
 */
public enum PropertyUsage {
	STORAGE(2), EDITOR(4), INTERNAL(8), CHECKABLE(16), SCRIPT_VARIABLE(32), DEFAULT(1 | 2 | 4 | 8), NO_EDITOR(
			1 | 2 | 8);

	public final int value;

	PropertyUsage(int value) {
		this.value = value;
	}
}
