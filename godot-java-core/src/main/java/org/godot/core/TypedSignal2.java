package org.godot.core;

import org.godot.Godot;

public final class TypedSignal2<A, B> extends TypedSignal {

	public TypedSignal2(Godot owner, String name) {
		super(owner, name);
	}

	public void emit(A a, B b) {
		owner.emitSignal(name, a, b);
	}
}
