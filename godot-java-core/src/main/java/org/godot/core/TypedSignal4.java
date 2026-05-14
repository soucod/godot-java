package org.godot.core;

import org.godot.Godot;

public final class TypedSignal4<A, B, C, D> extends TypedSignal {

	public TypedSignal4(Godot owner, String name) {
		super(owner, name);
	}

	public void emit(A a, B b, C c, D d) {
		owner.emitSignal(name, a, b, c, d);
	}
}
