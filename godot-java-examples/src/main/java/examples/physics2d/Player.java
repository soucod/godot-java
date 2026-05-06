package examples.physics2d;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector2;
import org.godot.node.CharacterBody2D;
import org.godot.singleton.Input;

/**
 * Example 07: Physics 2D
 *
 * Demonstrates physics movement with CharacterBody2D. Uses typed methods
 * (moveAndSlide, getVelocity, setVelocity, isOnFloor) directly.
 */
@GodotClass(name = "Player", parent = "CharacterBody2D")
public class Player extends CharacterBody2D {

	private static final Logger logger = LogManager.getLogger(Player.class);

	@Export
	public double speed = 300.0;

	@Export
	public double jumpVelocity = -400.0;

	private double gravity = 980.0;

	@Override
	public void _ready() {
		logger.info("Player ready! Use WASD or Arrow keys to move. speed={} jumpVelocity={}", speed, jumpVelocity);
	}

	@Override
	public void _physicsProcess(double delta) {
		Input input = Input.singleton();

		// Read input direction
		double direction = input.getAxis("move_left", "move_right");

		// Jump
		if (input.isActionJustPressed("jump", false)) {
			if (isOnFloor()) {
				Vector2 vel = getVelocity();
				setVelocity(new Vector2(vel.x, jumpVelocity));
			}
		}

		Vector2 vel = getVelocity();

		// Apply gravity
		double vy = vel.y + gravity * delta;

		// Apply horizontal movement from input
		double vx = direction * speed;

		setVelocity(new Vector2(vx, vy));
		moveAndSlide();
	}
}
