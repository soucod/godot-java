package org.godot.annotation;

import java.lang.annotation.*;

/**
 * Marks the beginning of an export property group in the Godot Inspector.
 * Applied to fields — the group applies to this field and all subsequent
 * {@code @Export} fields until the next {@code @ExportGroup} or
 * {@code @ExportSubgroup}.
 *
 * <p>
 * Example:
 *
 * <pre>
 * &#64;ExportGroup("Movement")
 * &#64;Export
 * float speed = 300.0f;
 * &#64;Export
 * float jumpForce = 500.0f;
 *
 * &#64;ExportGroup("Visual")
 * &#64;Export
 * Color color = new Color(1, 1, 1);
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExportGroup {
	String value();

	String hint() default "";
}
