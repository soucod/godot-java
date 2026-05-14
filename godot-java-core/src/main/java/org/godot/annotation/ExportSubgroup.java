package org.godot.annotation;

import java.lang.annotation.*;

/**
 * Marks the beginning of an export property subgroup within the current group.
 * Applied to fields — the subgroup applies to this field and all subsequent
 * {@code @Export} fields until the next {@code @ExportSubgroup} or
 * {@code @ExportGroup}.
 *
 * <p>
 * Example:
 *
 * <pre>
 * &#64;ExportGroup("Physics")
 * &#64;ExportSubgroup("Velocity")
 * &#64;Export
 * float maxSpeed = 300.0f;
 * &#64;Export
 * float acceleration = 50.0f;
 *
 * &#64;ExportSubgroup("Gravity")
 * &#64;Export
 * float gravityScale = 1.0f;
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExportSubgroup {
	String value();

	String hint() default "";
}
