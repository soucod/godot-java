package org.godot.core;

import org.godot.Godot;

public final class TypedSignal0 extends TypedSignal {

	public TypedSignal0(Godot owner, String name) {
		super(owner, name);
	}

	public void emit() {
		owner.emitSignal(name);
	}
}
