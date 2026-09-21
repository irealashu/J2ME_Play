package ru.playsoftware.j2meloader.demos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.applist.AppItem;
import ru.playsoftware.j2meloader.appsdb.AppRepository;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.util.FileUtils;

public class DemoGamesInstaller {
	private static final String TAG = "DemoGamesInstaller";

	public static void installDemoGames(Context context, AppRepository appRepository, Runnable onComplete) {
		Executors.newSingleThreadExecutor().execute(() -> {
			try {
				installSnakeDemo(context, appRepository);
				installBrickBreakerDemo(context, appRepository);
				new Handler(Looper.getMainLooper()).post(() -> {
					Toast.makeText(context, R.string.demo_games_installed, Toast.LENGTH_SHORT).show();
					if (onComplete != null) {
						onComplete.run();
					}
				});
			} catch (Exception e) {
				Log.e(TAG, "Error installing demo games", e);
				new Handler(Looper.getMainLooper()).post(() ->
						Toast.makeText(context, "Error installing demos: " + e.getMessage(), Toast.LENGTH_SHORT).show()
				);
			}
		});
	}

	private static void installSnakeDemo(Context context, AppRepository appRepository) throws IOException {
		String dirName = "retro_snake_j2me";
		File appDir = new File(Config.getAppDir(), dirName);
		if (!appDir.exists() && !appDir.mkdirs()) {
			throw new IOException("Failed to create demo app dir: " + appDir);
		}

		// Write Manifest Descriptor
		String manifestContent = "MIDlet-Name: Retro Snake\n" +
				"MIDlet-Vendor: J2ME Mobile Classics\n" +
				"MIDlet-Version: 1.0.0\n" +
				"MIDlet-1: Retro Snake, /icon.png, ru.playsoftware.j2meloader.demos.SnakeMIDlet\n" +
				"MicroEdition-Profile: MIDP-2.0\n" +
				"MicroEdition-Configuration: CLDC-1.1\n";

		File manifestFile = new File(appDir, Config.MIDLET_MANIFEST_FILE);
		writeFile(manifestFile, manifestContent);

		// Write empty dex / res placeholders
		File dexFile = new File(appDir, Config.MIDLET_DEX_FILE);
		if (!dexFile.exists()) {
			writeFile(dexFile, "DEX_BUILTIN");
		}
		File resFile = new File(appDir, Config.MIDLET_RES_FILE);
		if (!resFile.exists()) {
			writeFile(resFile, "RES_BUILTIN");
		}

		// Create icon
		File iconFile = new File(appDir, Config.MIDLET_ICON_FILE);
		createSnakeIcon(iconFile);

		AppItem item = new AppItem(dirName, "Retro Snake", "J2ME Mobile Classics", "1.0.0");
		item.setImagePathExt(Config.MIDLET_ICON_FILE);
		appRepository.insert(item);
	}

	private static void installBrickBreakerDemo(Context context, AppRepository appRepository) throws IOException {
		String dirName = "brick_breaker_j2me";
		File appDir = new File(Config.getAppDir(), dirName);
		if (!appDir.exists() && !appDir.mkdirs()) {
			throw new IOException("Failed to create demo app dir: " + appDir);
		}

		String manifestContent = "MIDlet-Name: Brick Breaker Arcade\n" +
				"MIDlet-Vendor: Retro Arcade Studios\n" +
				"MIDlet-Version: 1.2.0\n" +
				"MIDlet-1: Brick Breaker Arcade, /icon.png, ru.playsoftware.j2meloader.demos.BrickBreakerMIDlet\n" +
				"MicroEdition-Profile: MIDP-2.0\n" +
				"MicroEdition-Configuration: CLDC-1.1\n";

		File manifestFile = new File(appDir, Config.MIDLET_MANIFEST_FILE);
		writeFile(manifestFile, manifestContent);

		File dexFile = new File(appDir, Config.MIDLET_DEX_FILE);
		if (!dexFile.exists()) {
			writeFile(dexFile, "DEX_BUILTIN");
		}
		File resFile = new File(appDir, Config.MIDLET_RES_FILE);
		if (!resFile.exists()) {
			writeFile(resFile, "RES_BUILTIN");
		}

		File iconFile = new File(appDir, Config.MIDLET_ICON_FILE);
		createBrickBreakerIcon(iconFile);

		AppItem item = new AppItem(dirName, "Brick Breaker Arcade", "Retro Arcade Studios", "1.2.0");
		item.setImagePathExt(Config.MIDLET_ICON_FILE);
		appRepository.insert(item);
	}

	private static void writeFile(File file, String content) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(content.getBytes(StandardCharsets.UTF_8));
		}
	}

	private static void createSnakeIcon(File targetFile) {
		int size = 96;
		Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);

		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		// Background
		paint.setColor(Color.parseColor("#0F172A"));
		canvas.drawRoundRect(new RectF(0, 0, size, size), 20, 20, paint);

		// Snake Body
		paint.setColor(Color.parseColor("#10B981"));
		canvas.drawRoundRect(new RectF(20, 36, 44, 60), 8, 8, paint);
		canvas.drawRoundRect(new RectF(40, 36, 64, 60), 8, 8, paint);
		canvas.drawRoundRect(new RectF(60, 36, 76, 76), 8, 8, paint);

		// Food
		paint.setColor(Color.parseColor("#EF4444"));
		canvas.drawRoundRect(new RectF(24, 64, 40, 80), 6, 6, paint);

		try (FileOutputStream out = new FileOutputStream(targetFile)) {
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
		} catch (IOException e) {
			Log.e(TAG, "Error saving snake icon", e);
		}
	}

	private static void createBrickBreakerIcon(File targetFile) {
		int size = 96;
		Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);

		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		// Background
		paint.setColor(Color.parseColor("#020617"));
		canvas.drawRoundRect(new RectF(0, 0, size, size), 20, 20, paint);

		// Bricks
		paint.setColor(Color.parseColor("#EF4444"));
		canvas.drawRoundRect(new RectF(16, 20, 44, 34), 4, 4, paint);
		paint.setColor(Color.parseColor("#F59E0B"));
		canvas.drawRoundRect(new RectF(48, 20, 76, 34), 4, 4, paint);

		paint.setColor(Color.parseColor("#10B981"));
		canvas.drawRoundRect(new RectF(16, 38, 44, 52), 4, 4, paint);
		paint.setColor(Color.parseColor("#3B82F6"));
		canvas.drawRoundRect(new RectF(48, 38, 76, 52), 4, 4, paint);

		// Ball
		paint.setColor(Color.WHITE);
		canvas.drawCircle(36, 64, 6, paint);

		// Paddle
		paint.setColor(Color.parseColor("#38BDF8"));
		canvas.drawRoundRect(new RectF(28, 78, 68, 86), 4, 4, paint);

		try (FileOutputStream out = new FileOutputStream(targetFile)) {
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
		} catch (IOException e) {
			Log.e(TAG, "Error saving brick breaker icon", e);
		}
	}
}
