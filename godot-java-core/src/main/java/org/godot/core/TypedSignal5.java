package org.godot.core;

import org.godot.Godot;

public final class TypedSignal5<A, B, C, D, E> extends TypedSignal {

	public TypedSignal5(Godot owner, String name) {
		super(owner, name);
	}

	public void emit(A a, B b, C c, D d, E e) {
		owner.emitSignal(name, a, b, c, d, e);
	}
}
