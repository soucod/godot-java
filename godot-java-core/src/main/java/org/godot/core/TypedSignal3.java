package org.godot.core;

import org.godot.Godot;

public final class TypedSignal3<A, B, C> extends TypedSignal {

	public TypedSignal3(Godot owner, String name) {
		super(owner, name);
	}

	public void emit(A a, B b, C c) {
		owner.emitSignal(name, a, b, c);
	}
}
