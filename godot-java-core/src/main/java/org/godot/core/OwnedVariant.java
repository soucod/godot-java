package org.godot.core;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.ref.Cleaner;

import org.godot.bridge.Bridge;
import org.godot.internal.api.ApiIndex;

/**
 * Owns a copied Godot Variant for Java wrappers that must outlive a scoped
 * ptrcall return buffer.
 */
public final class OwnedVariant {

	private static final Cleaner CLEANER = Cleaner.create();

	private final State state;
	private final Cleaner.Cleanable cleanable;

	private OwnedVariant(State state) {
		this.state = state;
		this.cleanable = CLEANER.register(this, state);
	}

	public static OwnedVariant copyOf(MemorySegment sourceVariant) {
		Arena arena = Arena.ofShared();
		MemorySegment copy = arena.allocate(Variant.SIZE, 8);
		try {
			Bridge.callVoid(ApiIndex.VARIANT_NEW_COPY, copy, sourceVariant);
			return new OwnedVariant(new State(arena, copy));
		} catch (RuntimeException e) {
			arena.close();
			throw e;
		}
	}

	public MemorySegment segment() {
		return state.segment;
	}

	public void close() {
		cleanable.clean();
	}

	private static final class State implements Runnable {
		private final Arena arena;
		private final MemorySegment segment;
		private boolean closed;

		private State(Arena arena, MemorySegment segment) {
			this.arena = arena;
			this.segment = segment;
		}

		@Override
		public synchronized void run() {
			if (closed) {
				return;
			}
			try {
				Bridge.destroyVariant(segment);
			} finally {
				arena.close();
				closed = true;
			}
		}
	}
}
