package org.godot.math;

import java.lang.foreign.MemorySegment;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;

/**
 * RGBA color. Values are in the range 0.0–1.0 for each channel. Memory layout:
 * 4 floats (r, g, b, a), 16 bytes total.
 */
public final class Color {
	public double r;
	public double g;
	public double b;
	public double a;

	public Color() {
	}

	public Color(double r, double g, double b) {
		this(r, g, b, 1.0);
	}

	public Color(double r, double g, double b, double a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}

	@Override
	public String toString() {
		return "(" + r + ", " + g + ", " + b + ", " + a + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Color))
			return false;
		Color c = (Color) o;
		return Math.abs(r - c.r) < 1e-9 && Math.abs(g - c.g) < 1e-9 && Math.abs(b - c.b) < 1e-9
				&& Math.abs(a - c.a) < 1e-9;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(r) ^ (Double.hashCode(g) * 31) ^ (Double.hashCode(b) * 961)
				^ (Double.hashCode(a) * 29791);
	}

	public void toSegment(MemorySegment seg) {
		seg.set(JAVA_FLOAT, 0, (float) r);
		seg.set(JAVA_FLOAT, 4, (float) g);
		seg.set(JAVA_FLOAT, 8, (float) b);
		seg.set(JAVA_FLOAT, 12, (float) a);
	}

	public static Color fromSegment(MemorySegment seg) {
		return new Color(seg.get(JAVA_FLOAT, 0), seg.get(JAVA_FLOAT, 4), seg.get(JAVA_FLOAT, 8),
				seg.get(JAVA_FLOAT, 12));
	}
}
