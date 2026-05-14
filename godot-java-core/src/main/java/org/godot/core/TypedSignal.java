package org.godot.core;

import org.godot.Godot;

/**
 * Base class for typed signal facades. Provides type-safe emit and connect
 * without string-based signal names.
 */
public abstract class TypedSignal {

	protected final Godot owner;
	protected final String name;

	protected TypedSignal(Godot owner, String name) {
		this.owner = owner;
		this.name = name;
	}

	public final String getName() {
		return name;
	}

	public final boolean connect(Callable callable, int flags) {
		return owner.connect(name, callable, flags);
	}

	public final boolean connect(Callable callable) {
		return connect(callable, 0);
	}

	public final boolean connect(Godot target, String method, int flags) {
		return connect(new Callable(target, method), flags);
	}

	public final boolean connect(Godot target, String method) {
		return connect(target, method, 0);
	}
}
