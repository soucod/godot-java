package examples.pong;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.annotation.GodotMethod;
import org.godot.annotation.Signal;
import org.godot.math.Vector2;
import org.godot.node.ColorRect;
import org.godot.node.Label;
import org.godot.node.Node2D;
import org.godot.singleton.Input;

/**
 * Example 09: Pong
 *
 * Classic Pong game with AI opponent. All game logic, UI creation, input
 * handling, and visual synchronization are implemented in pure Java.
 */
@GodotClass(name = "PongGame", parent = "Node2D")
public class PongGame extends Node2D {

	private static final Logger logger = LogManager.getLogger(PongGame.class);

	private static final double WIDTH = 800.0;
	private static final double HEIGHT = 500.0;
	private static final double PADDLE_W = 15.0;
	private static final double PADDLE_H = 100.0;
	private static final double BALL_SIZE = 12.0;
	private static final double PADDLE_OFFSET = 30.0;
	private static final int WIN_SCORE = 5;

	@Export
	public double ballSpeed = 300.0;

	@Export
	public double paddleSpeed = 250.0;

	// Ball state
	private double ballX;
	private double ballY;
	private double ballVX;
	private double ballVY;

	// Paddle positions (Y center)
	private double playerY;
	private double aiY;

	// Score
	private int playerScore;
	private int aiScore;

	private boolean running;
	private boolean initialized;
	private final Random random = new Random();

	// Visual nodes
	private ColorRect ball;
	private ColorRect playerPaddle;
	private ColorRect aiPaddle;
	private Label playerLabel;
	private Label aiLabel;
	private Label messageLabel;
	private Node2D container;

	@Override
	public void _ready() {
		if (initialized) {
			return;
		}
		initialized = true;

		// Offset to center the play area in default viewport (1152x648)
		double offsetX = (1152 - WIDTH) / 2;
		double offsetY = (648 - HEIGHT) / 2;
		container = Node2D.create();
		container.setPosition(new Vector2(offsetX, offsetY));
		addChild(container, false, 0);

		// Play area background
		ColorRect bg = ColorRect.create();
		bg.setColor(new org.godot.math.Color(0.05, 0.05, 0.1));
		bg.setSize(new Vector2(WIDTH, HEIGHT));
		container.addChild(bg, false, 0);

		// Center dashed line
		for (int i = 0; i < HEIGHT; i += 20) {
			ColorRect dash = ColorRect.create();
			dash.setColor(new org.godot.math.Color(0.3, 0.3, 0.3));
			dash.setSize(new Vector2(2, 10));
			dash.setPosition(new Vector2(WIDTH / 2 - 1, i));
			container.addChild(dash, false, 0);
		}

		// Ball
		ball = ColorRect.create();
		ball.setColor(new org.godot.math.Color(1, 1, 1));
		ball.setSize(new Vector2(BALL_SIZE, BALL_SIZE));
		container.addChild(ball, false, 0);

		// Player paddle (left)
		playerPaddle = ColorRect.create();
		playerPaddle.setColor(new org.godot.math.Color(0.3, 0.7, 1.0));
		playerPaddle.setSize(new Vector2(PADDLE_W, PADDLE_H));
		container.addChild(playerPaddle, false, 0);

		// AI paddle (right)
		aiPaddle = ColorRect.create();
		aiPaddle.setColor(new org.godot.math.Color(1.0, 0.4, 0.4));
		aiPaddle.setSize(new Vector2(PADDLE_W, PADDLE_H));
		container.addChild(aiPaddle, false, 0);

		// Player score label
		playerLabel = Label.create();
		playerLabel.setHorizontalAlignment(1); // CENTER
		playerLabel.addThemeFontSizeOverride("font_size", 48);
		playerLabel.setPosition(new Vector2(WIDTH / 4 - 30, 20));
		playerLabel.setSize(new Vector2(60, 60));
		container.addChild(playerLabel, false, 0);

		// AI score label
		aiLabel = Label.create();
		aiLabel.setHorizontalAlignment(1);
		aiLabel.addThemeFontSizeOverride("font_size", 48);
		aiLabel.setPosition(new Vector2(3 * WIDTH / 4 - 30, 20));
		aiLabel.setSize(new Vector2(60, 60));
		container.addChild(aiLabel, false, 0);

		// Message label
		messageLabel = Label.create();
		messageLabel.setHorizontalAlignment(1);
		messageLabel.addThemeFontSizeOverride("font_size", 24);
		messageLabel.setPosition(new Vector2(WIDTH / 2 - 150, HEIGHT / 2 - 20));
		messageLabel.setSize(new Vector2(300, 40));
		messageLabel.setVisible(false);
		container.addChild(messageLabel, false, 0);

		resetGame();
		updateScoreDisplay();
		logger.info("PongGame ready! First to {} wins. Use W/S or Up/Down to move.", WIN_SCORE);
	}

	@Override
	public void _process(double delta) {
		Input input = Input.singleton();

		// Input
		double dir = input.getAxis("move_up", "move_down");

		// Restart
		if (!running && input.isActionJustPressed("ui_accept", false)) {
			resetGame();
			messageLabel.setVisible(false);
			updateScoreDisplay();
		}

		if (!running) {
			return;
		}

		// Move player paddle
		playerY += dir * paddleSpeed * delta;
		playerY = clamp(playerY, PADDLE_H / 2, HEIGHT - PADDLE_H / 2);

		// AI paddle: track ball with smoothing
		double aiDiff = ballY - aiY;
		double aiMove = Math.signum(aiDiff) * Math.min(Math.abs(aiDiff), paddleSpeed * 0.7 * delta);
		aiY += aiMove;
		aiY = clamp(aiY, PADDLE_H / 2, HEIGHT - PADDLE_H / 2);

		// Move ball
		ballX += ballVX * delta;
		ballY += ballVY * delta;

		// Ball bounce off top/bottom
		if (ballY - BALL_SIZE / 2 <= 0) {
			ballY = BALL_SIZE / 2;
			ballVY = Math.abs(ballVY);
		} else if (ballY + BALL_SIZE / 2 >= HEIGHT) {
			ballY = HEIGHT - BALL_SIZE / 2;
			ballVY = -Math.abs(ballVY);
		}

		// Ball vs player paddle (left side)
		double playerPaddleX = PADDLE_OFFSET;
		if (ballVX < 0 && ballX - BALL_SIZE / 2 <= playerPaddleX + PADDLE_W && ballX + BALL_SIZE / 2 >= playerPaddleX
				&& ballY >= playerY - PADDLE_H / 2 - BALL_SIZE / 2 && ballY <= playerY + PADDLE_H / 2 + BALL_SIZE / 2) {
			ballX = playerPaddleX + PADDLE_W + BALL_SIZE / 2;
			bounceOffPaddle(playerY, 1);
		}

		// Ball vs AI paddle (right side)
		double aiPaddleX = WIDTH - PADDLE_OFFSET - PADDLE_W;
		if (ballVX > 0 && ballX + BALL_SIZE / 2 >= aiPaddleX && ballX - BALL_SIZE / 2 <= aiPaddleX + PADDLE_W
				&& ballY >= aiY - PADDLE_H / 2 - BALL_SIZE / 2 && ballY <= aiY + PADDLE_H / 2 + BALL_SIZE / 2) {
			ballX = aiPaddleX - BALL_SIZE / 2;
			bounceOffPaddle(aiY, -1);
		}

		// Scoring
		if (ballX < -BALL_SIZE) {
			aiScore++;
			logger.info("AI scores! Player {} - AI {}", playerScore, aiScore);
			if (aiScore >= WIN_SCORE) {
				running = false;
			} else {
				launchBall(1);
			}
			onScoreUpdate();
		} else if (ballX > WIDTH + BALL_SIZE) {
			playerScore++;
			logger.info("Player scores! Player {} - AI {}", playerScore, aiScore);
			if (playerScore >= WIN_SCORE) {
				running = false;
			} else {
				launchBall(-1);
			}
			onScoreUpdate();
		}

		// Sync visual positions
		ball.setPosition(new Vector2(ballX - BALL_SIZE / 2, ballY - BALL_SIZE / 2));
		playerPaddle.setPosition(new Vector2(PADDLE_OFFSET, playerY - PADDLE_H / 2));
		aiPaddle.setPosition(new Vector2(WIDTH - PADDLE_OFFSET - PADDLE_W, aiY - PADDLE_H / 2));
	}

	private void onScoreUpdate() {
		emitSignal("onScore", playerScore, aiScore);
		updateScoreDisplay();
		if (!running) {
			if (playerScore >= WIN_SCORE) {
				messageLabel.setText("You Win! Press Space");
			} else {
				messageLabel.setText("AI Wins! Press Space");
			}
			messageLabel.setVisible(true);
		}
	}

	private void updateScoreDisplay() {
		playerLabel.setText(String.valueOf(playerScore));
		aiLabel.setText(String.valueOf(aiScore));
	}

	private void bounceOffPaddle(double paddleYCenter, int directionX) {
		double relativeHit = (ballY - paddleYCenter) / (PADDLE_H / 2);
		double maxAngle = Math.PI / 3;
		double angle = relativeHit * maxAngle;
		double speed = Math.sqrt(ballVX * ballVX + ballVY * ballVY);
		speed = Math.min(speed * 1.05, ballSpeed * 2.0);
		ballVX = directionX * speed * Math.cos(angle);
		ballVY = speed * Math.sin(angle);
	}

	private void launchBall(int directionX) {
		ballX = WIDTH / 2;
		ballY = HEIGHT / 2;
		double angle = (random.nextDouble() - 0.5) * Math.PI / 3;
		ballVX = directionX * ballSpeed * Math.cos(angle);
		ballVY = ballSpeed * Math.sin(angle);
	}

	@Signal
	public void onScore(int player, int ai) {
	}

	@GodotMethod
	public void resetGame() {
		playerY = HEIGHT / 2;
		aiY = HEIGHT / 2;
		playerScore = 0;
		aiScore = 0;
		running = true;
		launchBall(random.nextBoolean() ? 1 : -1);
	}

	@GodotMethod
	public boolean isRunning() {
		return running;
	}

	@GodotMethod
	public int getPlayerScore() {
		return playerScore;
	}

	@GodotMethod
	public int getAiScore() {
		return aiScore;
	}

	@GodotMethod
	public double getBallX() {
		return ballX;
	}

	@GodotMethod
	public double getBallY() {
		return ballY;
	}

	@GodotMethod
	public double getPlayerY() {
		return playerY;
	}

	@GodotMethod
	public double getAiY() {
		return aiY;
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
