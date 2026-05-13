package org.godot.math;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.JAVA_FLOAT;

/**
 * 4x4 projection matrix. Memory layout: 4 Vector4 columns, 64 bytes total
 * (float_32).
 */
public final class Projection {
	/**
	 * X column of the projection matrix.
	 */
	public Vector4 x;
	/**
	 * Y column of the projection matrix.
	 */
	public Vector4 y;
	/**
	 * Z column of the projection matrix.
	 */
	public Vector4 z;
	/**
	 * W column of the projection matrix.
	 */
	public Vector4 w;

	public Projection() {
		this.x = new Vector4();
		this.y = new Vector4();
		this.z = new Vector4();
		this.w = new Vector4();
	}

	public Projection(Vector4 x, Vector4 y, Vector4 z, Vector4 w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	public static Projection fromSegment(MemorySegment segment) {
		return new Projection(new Vector4(segment.get(JAVA_FLOAT, 0), segment.get(JAVA_FLOAT, 4),
				segment.get(JAVA_FLOAT, 8), segment.get(JAVA_FLOAT, 12)),
				new Vector4(segment.get(JAVA_FLOAT, 16), segment.get(JAVA_FLOAT, 20), segment.get(JAVA_FLOAT, 24),
						segment.get(JAVA_FLOAT, 28)),
				new Vector4(segment.get(JAVA_FLOAT, 32), segment.get(JAVA_FLOAT, 36), segment.get(JAVA_FLOAT, 40),
						segment.get(JAVA_FLOAT, 44)),
				new Vector4(segment.get(JAVA_FLOAT, 48), segment.get(JAVA_FLOAT, 52), segment.get(JAVA_FLOAT, 56),
						segment.get(JAVA_FLOAT, 60)));
	}

	public void toSegment(MemorySegment segment) {
		segment.set(JAVA_FLOAT, 0, (float) x.x);
		segment.set(JAVA_FLOAT, 4, (float) x.y);
		segment.set(JAVA_FLOAT, 8, (float) x.z);
		segment.set(JAVA_FLOAT, 12, (float) x.w);
		segment.set(JAVA_FLOAT, 16, (float) y.x);
		segment.set(JAVA_FLOAT, 20, (float) y.y);
		segment.set(JAVA_FLOAT, 24, (float) y.z);
		segment.set(JAVA_FLOAT, 28, (float) y.w);
		segment.set(JAVA_FLOAT, 32, (float) z.x);
		segment.set(JAVA_FLOAT, 36, (float) z.y);
		segment.set(JAVA_FLOAT, 40, (float) z.z);
		segment.set(JAVA_FLOAT, 44, (float) z.w);
		segment.set(JAVA_FLOAT, 48, (float) w.x);
		segment.set(JAVA_FLOAT, 52, (float) w.y);
		segment.set(JAVA_FLOAT, 56, (float) w.z);
		segment.set(JAVA_FLOAT, 60, (float) w.w);
	}
}
