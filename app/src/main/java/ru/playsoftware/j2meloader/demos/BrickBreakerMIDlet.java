package ru.playsoftware.j2meloader.demos;

import java.util.Random;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.midlet.MIDlet;

public class BrickBreakerMIDlet extends MIDlet implements CommandListener {

	private Display display;
	private BrickCanvas canvas;
	private Command exitCmd;
	private Command restartCmd;

	@Override
	public void startApp() {
		if (display == null) {
			display = Display.getDisplay(this);
			canvas = new BrickCanvas();
			exitCmd = new Command("Exit", Command.EXIT, 1);
			restartCmd = new Command("Restart", Command.SCREEN, 2);
			canvas.addCommand(exitCmd);
			canvas.addCommand(restartCmd);
			canvas.setCommandListener(this);
		}
		display.setCurrent(canvas);
		canvas.start();
	}

	@Override
	public void pauseApp() {
		if (canvas != null) {
			canvas.pause();
		}
	}

	@Override
	public void destroyApp(boolean unconditional) {
		if (canvas != null) {
			canvas.stop();
		}
	}

	@Override
	public void commandAction(Command c, Displayable d) {
		if (c == exitCmd) {
			destroyApp(false);
			notifyDestroyed();
		} else if (c == restartCmd) {
			canvas.restart();
		}
	}

	private static class BrickCanvas extends Canvas implements Runnable {
		private static final int BRICK_ROWS = 5;
		private static final int BRICK_COLS = 8;

		private final boolean[][] bricks = new boolean[BRICK_ROWS][BRICK_COLS];
		private final int[] rowColors = new int[]{0xEF4444, 0xF97316, 0xFBBF24, 0x10B981, 0x3B82F6};

		private int paddleX = 80;
		private int paddleWidth = 44;
		private int paddleSpeed = 8;

		private float ballX = 100;
		private float ballY = 150;
		private float ballVx = 3.0f;
		private float ballVy = -3.5f;
		private final int ballRadius = 4;

		private int score = 0;
		private int lives = 3;
		private boolean gameOver = false;
		private boolean gameWon = false;
		private boolean running = false;
		private Thread gameThread;

		private boolean moveLeft = false;
		private boolean moveRight = false;

		BrickCanvas() {
			setFullScreenMode(true);
			initGame();
		}

		private void initGame() {
			score = 0;
			lives = 3;
			gameOver = false;
			gameWon = false;
			resetBricks();
			resetBallAndPaddle();
		}

		private void resetBricks() {
			for (int r = 0; r < BRICK_ROWS; r++) {
				for (int c = 0; c < BRICK_COLS; c++) {
					bricks[r][c] = true;
				}
			}
		}

		private void resetBallAndPaddle() {
			int w = Math.max(100, getWidth());
			int h = Math.max(100, getHeight());
			paddleWidth = Math.max(36, w / 6);
			paddleX = (w - paddleWidth) / 2;
			ballX = paddleX + paddleWidth / 2f;
			ballY = h - 36;
			ballVx = 3.0f;
			ballVy = -4.0f;
			moveLeft = false;
			moveRight = false;
		}

		synchronized void start() {
			if (!running) {
				running = true;
				gameThread = new Thread(this);
				gameThread.start();
			}
		}

		synchronized void pause() {
			running = false;
		}

		synchronized void stop() {
			running = false;
		}

		synchronized void restart() {
			initGame();
			repaint();
			if (!running) {
				start();
			}
		}

		@Override
		public void run() {
			while (running) {
				if (!gameOver && !gameWon) {
					update();
				}
				repaint();
				try {
					Thread.sleep(25);
				} catch (InterruptedException e) {
					break;
				}
			}
		}

		private void update() {
			int w = getWidth();
			int h = getHeight();

			// Paddle movement
			if (moveLeft && paddleX > 4) {
				paddleX -= paddleSpeed;
			}
			if (moveRight && paddleX + paddleWidth < w - 4) {
				paddleX += paddleSpeed;
			}

			// Ball movement
			ballX += ballVx;
			ballY += ballVy;

			// Wall bounce
			if (ballX - ballRadius <= 0) {
				ballX = ballRadius;
				ballVx = -ballVx;
			} else if (ballX + ballRadius >= w) {
				ballX = w - ballRadius;
				ballVx = -ballVx;
			}
			if (ballY - ballRadius <= 24) { // top hud area
				ballY = 24 + ballRadius;
				ballVy = -ballVy;
			}

			// Paddle bounce
			int paddleY = h - 24;
			if (ballY + ballRadius >= paddleY && ballY - ballRadius <= paddleY + 8) {
				if (ballX >= paddleX && ballX <= paddleX + paddleWidth) {
					ballVy = -Math.abs(ballVy);
					float hitPos = (ballX - (paddleX + paddleWidth / 2.0f)) / (paddleWidth / 2.0f);
					ballVx = hitPos * 5.0f;
				}
			}

			// Bottom death
			if (ballY > h) {
				lives--;
				if (lives <= 0) {
					gameOver = true;
				} else {
					resetBallAndPaddle();
				}
				return;
			}

			// Brick collision
			int brickMarginTop = 36;
			int brickAreaHeight = 90;
			int brickAreaWidth = w - 16;
			int brickW = brickAreaWidth / BRICK_COLS;
			int brickH = brickAreaHeight / BRICK_ROWS;

			boolean allBroken = true;
			for (int r = 0; r < BRICK_ROWS; r++) {
				for (int c = 0; c < BRICK_COLS; c++) {
					if (bricks[r][c]) {
						allBroken = false;
						int bx = 8 + c * brickW;
						int by = brickMarginTop + r * brickH;

						if (ballX + ballRadius >= bx && ballX - ballRadius <= bx + brickW - 2 &&
								ballY + ballRadius >= by && ballY - ballRadius <= by + brickH - 2) {
							bricks[r][c] = false;
							ballVy = -ballVy;
							score += (BRICK_ROWS - r) * 10;
							break;
						}
					}
				}
			}
			if (allBroken) {
				gameWon = true;
			}
		}

		@Override
		protected void keyPressed(int keyCode) {
			int gameAction = getGameAction(keyCode);
			if (keyCode == KEY_NUM4 || gameAction == LEFT) {
				moveLeft = true;
			} else if (keyCode == KEY_NUM6 || gameAction == RIGHT) {
				moveRight = true;
			} else if (keyCode == KEY_NUM5 || gameAction == FIRE) {
				if (gameOver || gameWon) restart();
			}
		}

		@Override
		protected void keyReleased(int keyCode) {
			int gameAction = getGameAction(keyCode);
			if (keyCode == KEY_NUM4 || gameAction == LEFT) {
				moveLeft = false;
			} else if (keyCode == KEY_NUM6 || gameAction == RIGHT) {
				moveRight = false;
			}
		}

		@Override
		protected void pointerPressed(int x, int y) {
			if (gameOver || gameWon) {
				restart();
				return;
			}
			paddleX = Math.max(4, Math.min(x - paddleWidth / 2, getWidth() - paddleWidth - 4));
		}

		@Override
		protected void pointerDragged(int x, int y) {
			paddleX = Math.max(4, Math.min(x - paddleWidth / 2, getWidth() - paddleWidth - 4));
		}

		@Override
		protected void paint(Graphics g) {
			int w = getWidth();
			int h = getHeight();

			// Dark Arcade canvas
			g.setColor(0x02, 0x06, 0x17);
			g.fillRect(0, 0, w, h);

			// Draw Bricks
			int brickMarginTop = 36;
			int brickAreaHeight = 90;
			int brickAreaWidth = w - 16;
			int brickW = brickAreaWidth / BRICK_COLS;
			int brickH = brickAreaHeight / BRICK_ROWS;

			for (int r = 0; r < BRICK_ROWS; r++) {
				int color = rowColors[r % rowColors.length];
				int rVal = (color >> 16) & 0xFF;
				int gVal = (color >> 8) & 0xFF;
				int bVal = color & 0xFF;

				for (int c = 0; c < BRICK_COLS; c++) {
					if (bricks[r][c]) {
						int bx = 8 + c * brickW;
						int by = brickMarginTop + r * brickH;
						g.setColor(rVal, gVal, bVal);
						g.fillRoundRect(bx, by, brickW - 2, brickH - 2, 4, 4);
					}
				}
			}

			// Draw Paddle
			int paddleY = h - 24;
			g.setColor(0x38, 0xBD, 0xF8);
			g.fillRoundRect(paddleX, paddleY, paddleWidth, 8, 4, 4);

			// Draw Ball
			g.setColor(0xFF, 0xFF, 0xFF);
			g.fillArc((int) ballX - ballRadius, (int) ballY - ballRadius, ballRadius * 2, ballRadius * 2, 0, 360);

			// Draw HUD
			g.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_SMALL));
			g.setColor(0xA5, 0xB4, 0xFC);
			g.drawString("Score: " + score, 8, 8, Graphics.TOP | Graphics.LEFT);
			g.setColor(0xF4, 0x3F, 0x5E);
			g.drawString("Lives: " + lives, w - 8, 8, Graphics.TOP | Graphics.RIGHT);

			if (gameOver || gameWon) {
				g.setColor(0x0F, 0x17, 0x2A);
				g.fillRect(w / 2 - 80, h / 2 - 30, 160, 60);
				g.setColor(gameWon ? 0x10B981 : 0xF43F5E);
				g.drawRect(w / 2 - 80, h / 2 - 30, 160, 60);
				g.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
				g.drawString(gameWon ? "STAGE CLEAR!" : "GAME OVER", w / 2, h / 2 - 18, Graphics.TOP | Graphics.HCENTER);
				g.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL));
				g.setColor(0xF8, 0xFA, 0xFC);
				g.drawString("Tap to Play Again", w / 2, h / 2 + 6, Graphics.TOP | Graphics.HCENTER);
			}
		}
	}
}
