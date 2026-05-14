package org.godot.core;

import org.godot.Godot;

public final class TypedSignal1<A> extends TypedSignal {

	public TypedSignal1(Godot owner, String name) {
		super(owner, name);
	}

	public void emit(A a) {
		owner.emitSignal(name, a);
	}
}
