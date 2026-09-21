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

public class SnakeMIDlet extends MIDlet implements CommandListener {

	private Display display;
	private SnakeCanvas canvas;
	private Command exitCmd;
	private Command restartCmd;

	@Override
	public void startApp() {
		if (display == null) {
			display = Display.getDisplay(this);
			canvas = new SnakeCanvas();
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

	private static class SnakeCanvas extends Canvas implements Runnable {
		private static final int GRID_SIZE = 12;
		private static final int DIR_UP = 0;
		private static final int DIR_RIGHT = 1;
		private static final int DIR_DOWN = 2;
		private static final int DIR_LEFT = 3;

		private final int[] snakeX = new int[400];
		private final int[] snakeY = new int[400];
		private int snakeLen = 4;
		private int dir = DIR_RIGHT;
		private int nextDir = DIR_RIGHT;

		private int foodX = 5;
		private int foodY = 5;
		private int score = 0;
		private boolean gameOver = false;
		private boolean running = false;
		private Thread gameThread;
		private final Random random = new Random();

		SnakeCanvas() {
			setFullScreenMode(true);
			initGame();
		}

		private void initGame() {
			snakeLen = 4;
			dir = DIR_RIGHT;
			nextDir = DIR_RIGHT;
			score = 0;
			gameOver = false;
			for (int i = 0; i < snakeLen; i++) {
				snakeX[i] = 10 - i;
				snakeY[i] = 10;
			}
			spawnFood();
		}

		private void spawnFood() {
			int cols = Math.max(8, getWidth() / GRID_SIZE);
			int rows = Math.max(8, getHeight() / GRID_SIZE);
			foodX = random.nextInt(cols - 2) + 1;
			foodY = random.nextInt(rows - 2) + 1;
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
				if (!gameOver) {
					update();
				}
				repaint();
				try {
					int delay = Math.max(80, 160 - (score * 4));
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					break;
				}
			}
		}

		private void update() {
			dir = nextDir;
			int cols = Math.max(8, getWidth() / GRID_SIZE);
			int rows = Math.max(8, getHeight() / GRID_SIZE);

			int headX = snakeX[0];
			int headY = snakeY[0];

			if (dir == DIR_UP) headY--;
			else if (dir == DIR_RIGHT) headX++;
			else if (dir == DIR_DOWN) headY++;
			else if (dir == DIR_LEFT) headX--;

			// Screen wrap or wall collision
			if (headX < 0) headX = cols - 1;
			else if (headX >= cols) headX = 0;
			if (headY < 0) headY = rows - 1;
			else if (headY >= rows) headY = 0;

			// Self collision
			for (int i = 0; i < snakeLen; i++) {
				if (snakeX[i] == headX && snakeY[i] == headY) {
					gameOver = true;
					return;
				}
			}

			// Move body
			for (int i = snakeLen; i > 0; i--) {
				snakeX[i] = snakeX[i - 1];
				snakeY[i] = snakeY[i - 1];
			}
			snakeX[0] = headX;
			snakeY[0] = headY;

			// Food collision
			if (headX == foodX && headY == foodY) {
				snakeLen = Math.min(snakeLen + 1, snakeX.length - 1);
				score += 10;
				spawnFood();
			}
		}

		@Override
		protected void keyPressed(int keyCode) {
			int gameAction = getGameAction(keyCode);
			if (keyCode == KEY_NUM2 || gameAction == UP) {
				if (dir != DIR_DOWN) nextDir = DIR_UP;
			} else if (keyCode == KEY_NUM6 || gameAction == RIGHT) {
				if (dir != DIR_LEFT) nextDir = DIR_RIGHT;
			} else if (keyCode == KEY_NUM8 || gameAction == DOWN) {
				if (dir != DIR_UP) nextDir = DIR_DOWN;
			} else if (keyCode == KEY_NUM4 || gameAction == LEFT) {
				if (dir != DIR_RIGHT) nextDir = DIR_LEFT;
			} else if (keyCode == KEY_NUM5 || gameAction == FIRE) {
				if (gameOver) restart();
			}
		}

		@Override
		protected void pointerPressed(int x, int y) {
			if (gameOver) {
				restart();
				return;
			}
			int w = getWidth();
			int h = getHeight();
			int headPixelX = snakeX[0] * GRID_SIZE;
			int headPixelY = snakeY[0] * GRID_SIZE;

			int dx = x - headPixelX;
			int dy = y - headPixelY;

			if (Math.abs(dx) > Math.abs(dy)) {
				if (dx > 0 && dir != DIR_LEFT) nextDir = DIR_RIGHT;
				else if (dx < 0 && dir != DIR_RIGHT) nextDir = DIR_LEFT;
			} else {
				if (dy > 0 && dir != DIR_UP) nextDir = DIR_DOWN;
				else if (dy < 0 && dir != DIR_DOWN) nextDir = DIR_UP;
			}
		}

		@Override
		protected void paint(Graphics g) {
			int w = getWidth();
			int h = getHeight();

			// Dark retro background
			g.setColor(0x0F, 0x17, 0x2A);
			g.fillRect(0, 0, w, h);

			// Grid dots
			g.setColor(0x1E, 0x29, 0x3B);
			for (int x = 0; x < w; x += GRID_SIZE) {
				for (int y = 0; y < h; y += GRID_SIZE) {
					g.fillRect(x, y, 1, 1);
				}
			}

			// Draw Food
			g.setColor(0xEF, 0x44, 0x44);
			g.fillRoundRect(foodX * GRID_SIZE + 1, foodY * GRID_SIZE + 1, GRID_SIZE - 2, GRID_SIZE - 2, 4, 4);

			// Draw Snake
			for (int i = 0; i < snakeLen; i++) {
				if (i == 0) {
					// Head
					g.setColor(0x10, 0xB9, 0x81);
					g.fillRoundRect(snakeX[i] * GRID_SIZE, snakeY[i] * GRID_SIZE, GRID_SIZE, GRID_SIZE, 4, 4);
					// Eyes
					g.setColor(0xFF, 0xFF, 0xFF);
					g.fillRect(snakeX[i] * GRID_SIZE + 3, snakeY[i] * GRID_SIZE + 3, 2, 2);
				} else {
					// Body
					g.setColor(0x05, 0x96, 0x69);
					g.fillRoundRect(snakeX[i] * GRID_SIZE + 1, snakeY[i] * GRID_SIZE + 1, GRID_SIZE - 2, GRID_SIZE - 2, 3, 3);
				}
			}

			// Draw HUD
			g.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_SMALL));
			g.setColor(0x38, 0xBD, 0xF8);
			g.drawString("Score: " + score, 8, 8, Graphics.TOP | Graphics.LEFT);

			if (gameOver) {
				g.setColor(0x00, 0x00, 0x00);
				g.fillRect(w / 2 - 80, h / 2 - 30, 160, 60);
				g.setColor(0xF4, 0x3F, 0x5E);
				g.drawRect(w / 2 - 80, h / 2 - 30, 160, 60);
				g.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
				g.drawString("GAME OVER", w / 2, h / 2 - 18, Graphics.TOP | Graphics.HCENTER);
				g.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL));
				g.setColor(0xF8, 0xFA, 0xFC);
				g.drawString("Tap or Press 5 to Restart", w / 2, h / 2 + 6, Graphics.TOP | Graphics.HCENTER);
			}
		}
	}
}
