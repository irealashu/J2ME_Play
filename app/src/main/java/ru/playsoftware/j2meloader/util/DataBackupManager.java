/*
 * Copyright 2026 Ashutosh Singh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.playsoftware.j2meloader.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import ru.playsoftware.j2meloader.appsdb.AppRepository;
import ru.playsoftware.j2meloader.config.Config;

public class DataBackupManager {
	private static final String TAG = "DataBackupManager";
	private static final int BUFFER_SIZE = 8192;

	public interface ProgressListener {
		void onProgress(int current, int total, String status);
	}

	public static class BackupStats {
		public int gamesCount;
		public int savesCount;
		public long totalBytes;
	}

	public static class BackupResult {
		public boolean success;
		public int gamesCount;
		public int savesCount;
		public long bytesWritten;
		public String filePath;
		public Uri uri;
		public String errorMessage;
	}

	public static class RestoreResult {
		public boolean success;
		public int gamesCount;
		public int savesCount;
		public String errorMessage;
	}

	public static BackupStats getStats(Context context) {
		BackupStats stats = new BackupStats();
		try {
			File appDir = new File(Config.getAppDir());
			if (appDir.exists() && appDir.isDirectory()) {
				File[] files = appDir.listFiles();
				if (files != null) {
					for (File f : files) {
						if (f.isDirectory() && new File(f, Config.MIDLET_DEX_FILE).isFile()) {
							stats.gamesCount++;
						}
					}
				}
			}

			File dataDir = new File(Config.getDataDir());
			if (dataDir.exists() && dataDir.isDirectory()) {
				File[] saveFolders = dataDir.listFiles();
				if (saveFolders != null) {
					for (File s : saveFolders) {
						if (s.isDirectory()) {
							File[] rmsFiles = s.listFiles((d, name) -> name.endsWith(".rsh") || name.endsWith(".rsr"));
							if (rmsFiles != null && rmsFiles.length > 0) {
								stats.savesCount++;
							}
						}
					}
				}
			}

			File emulatorDir = new File(Config.getEmulatorDir());
			stats.totalBytes = calculateDirSize(emulatorDir);
		} catch (Exception e) {
			Log.w(TAG, "Failed to compute backup stats", e);
		}
		return stats;
	}

	private static long calculateDirSize(File dir) {
		long size = 0;
		if (dir == null || !dir.exists()) return 0;
		if (dir.isFile()) return dir.length();
		File[] children = dir.listFiles();
		if (children != null) {
			for (File child : children) {
				size += calculateDirSize(child);
			}
		}
		return size;
	}

	private static void collectFiles(File dir, List<File> result) {
		if (dir == null || !dir.exists()) return;
		File[] list = dir.listFiles();
		if (list == null) return;
		for (File f : list) {
			if (f.isDirectory()) {
				collectFiles(f, result);
			} else if (f.isFile() && !f.getName().endsWith(".tmp")) {
				result.add(f);
			}
		}
	}

	public static BackupResult exportToUri(Context context, Uri uri, ProgressListener listener) {
		BackupResult result = new BackupResult();
		try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
			if (os == null) {
				throw new IOException("Could not open destination stream for " + uri);
			}
			result = exportBackup(context, os, listener);
			result.uri = uri;
		} catch (Exception e) {
			Log.e(TAG, "exportToUri failed", e);
			result.success = false;
			result.errorMessage = e.getMessage();
		}
		return result;
	}

	public static BackupResult exportToShareableFile(Context context, ProgressListener listener) {
		BackupResult result = new BackupResult();
		try {
			File backupDir = new File(context.getCacheDir(), "backups");
			if (!backupDir.exists()) {
				backupDir.mkdirs();
			}
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
			File zipFile = new File(backupDir, "J2ME_Play_Backup_" + timeStamp + ".zip");
			try (FileOutputStream fos = new FileOutputStream(zipFile)) {
				result = exportBackup(context, fos, listener);
				if (result.success) {
					result.filePath = zipFile.getAbsolutePath();
					result.uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", zipFile);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "exportToShareableFile failed", e);
			result.success = false;
			result.errorMessage = e.getMessage();
		}
		return result;
	}

	public static BackupResult exportBackup(Context context, OutputStream out, ProgressListener listener) {
		BackupResult result = new BackupResult();
		ZipOutputStream zos = null;
		try {
			File emulatorDir = new File(Config.getEmulatorDir());
			if (!emulatorDir.exists()) {
				emulatorDir.mkdirs();
			}

			List<File> filesToExport = new ArrayList<>();
			String[] subDirs = new String[]{"converted", "data", "configs", "templates", "fs", "shaders"};
			for (String sub : subDirs) {
				File dir = new File(emulatorDir, sub);
				if (dir.exists() && dir.isDirectory()) {
					collectFiles(dir, filesToExport);
				}
			}

			// Include root database if exists
			File dbFile = new File(emulatorDir, "J2ME-apps.db");
			if (dbFile.exists() && dbFile.isFile()) {
				filesToExport.add(dbFile);
			}

			int gamesCount = 0;
			File appDir = new File(Config.getAppDir());
			if (appDir.exists() && appDir.isDirectory()) {
				File[] games = appDir.listFiles();
				if (games != null) {
					for (File g : games) {
						if (g.isDirectory() && new File(g, Config.MIDLET_DEX_FILE).isFile()) {
							gamesCount++;
						}
					}
				}
			}

			int savesCount = 0;
			File dataDir = new File(Config.getDataDir());
			if (dataDir.exists() && dataDir.isDirectory()) {
				File[] saveDirs = dataDir.listFiles();
				if (saveDirs != null) {
					for (File s : saveDirs) {
						if (s.isDirectory()) {
							File[] rshFiles = s.listFiles((d, name) -> name.endsWith(".rsh") || name.endsWith(".rsr"));
							if (rshFiles != null && rshFiles.length > 0) {
								savesCount++;
							}
						}
					}
				}
			}

			zos = new ZipOutputStream(new BufferedOutputStream(out));

			// 1. Write metadata JSON
			Map<String, Object> meta = new HashMap<>();
			meta.put("app", "J2ME Play");
			meta.put("version", 1);
			meta.put("timestamp", System.currentTimeMillis());
			meta.put("gamesCount", gamesCount);
			meta.put("savesCount", savesCount);
			String metaJson = new Gson().toJson(meta);
			writeZipEntry(zos, "backup_info.json", metaJson.getBytes(StandardCharsets.UTF_8));

			// 2. Write preferences JSON
			if (context != null) {
				try {
					SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
					Map<String, ?> prefs = sp.getAll();
					String prefsJson = new Gson().toJson(prefs);
					writeZipEntry(zos, "preferences.json", prefsJson.getBytes(StandardCharsets.UTF_8));
				} catch (Exception e) {
					Log.w(TAG, "Failed to serialize preferences", e);
				}
			}

			// 3. Write all collected files
			String basePath = emulatorDir.getCanonicalPath();
			int totalFiles = filesToExport.size();
			int current = 0;
			byte[] buffer = new byte[BUFFER_SIZE];

			for (File f : filesToExport) {
				current++;
				String canonical = f.getCanonicalPath();
				if (!canonical.startsWith(basePath)) continue;

				String relative = canonical.substring(basePath.length());
				if (relative.startsWith(File.separator)) {
					relative = relative.substring(1);
				}
				relative = relative.replace('\\', '/');

				if (listener != null) {
					listener.onProgress(current, totalFiles, f.getName());
				}

				ZipEntry entry = new ZipEntry(relative);
				entry.setTime(f.lastModified());
				zos.putNextEntry(entry);

				try (FileInputStream fis = new FileInputStream(f)) {
					int len;
					while ((len = fis.read(buffer)) > 0) {
						zos.write(buffer, 0, len);
					}
				} catch (Exception e) {
					Log.w(TAG, "Failed to read file into backup: " + f, e);
				}
				zos.closeEntry();
			}

			zos.finish();
			zos.flush();
			result.success = true;
			result.gamesCount = gamesCount;
			result.savesCount = savesCount;
		} catch (Exception e) {
			Log.e(TAG, "exportBackup error", e);
			result.success = false;
			result.errorMessage = e.getMessage();
		}
		return result;
	}

	public static RestoreResult importFromUri(Context context, Uri uri, AppRepository appRepository, ProgressListener listener) {
		RestoreResult result = new RestoreResult();
		try (InputStream is = context.getContentResolver().openInputStream(uri)) {
			if (is == null) {
				throw new IOException("Could not open input stream for " + uri);
			}
			result = importBackup(context, is, appRepository, listener);
		} catch (Exception e) {
			Log.e(TAG, "importFromUri error", e);
			result.success = false;
			result.errorMessage = e.getMessage();
		}
		return result;
	}

	public static RestoreResult importBackup(Context context, InputStream in, AppRepository appRepository, ProgressListener listener) {
		RestoreResult result = new RestoreResult();
		ZipInputStream zis = null;
		try {
			File emulatorDir = new File(Config.getEmulatorDir());
			if (!emulatorDir.exists()) {
				emulatorDir.mkdirs();
			}
			String baseCanonical = emulatorDir.getCanonicalPath();

			zis = new ZipInputStream(new BufferedInputStream(in));
			ZipEntry entry;
			byte[] buffer = new byte[BUFFER_SIZE];
			int processedEntries = 0;
			Set<String> importedGames = new HashSet<>();
			Set<String> importedSaves = new HashSet<>();

			while ((entry = zis.getNextEntry()) != null) {
				processedEntries++;
				String name = entry.getName();
				if (name == null || name.trim().isEmpty()) {
					zis.closeEntry();
					continue;
				}
				name = name.replace('\\', '/');

				File targetFile = new File(emulatorDir, name);
				String targetCanonical = targetFile.getCanonicalPath();

				// Guard against Zip-Slip path traversal vulnerability
				if (!targetCanonical.startsWith(baseCanonical + File.separator) && !targetCanonical.equals(baseCanonical)) {
					Log.w(TAG, "Skipping entry outside target directory: " + name);
					zis.closeEntry();
					continue;
				}

				if (listener != null) {
					listener.onProgress(processedEntries, -1, targetFile.getName());
				}

				if (entry.isDirectory()) {
					targetFile.mkdirs();
				} else {
					if ("preferences.json".equals(name)) {
						try {
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							int len;
							while ((len = zis.read(buffer)) > 0) {
								baos.write(buffer, 0, len);
							}
							restorePreferences(context, new String(baos.toByteArray(), StandardCharsets.UTF_8));
						} catch (Exception e) {
							Log.w(TAG, "Failed to restore preferences", e);
						}
					} else if ("backup_info.json".equals(name)) {
						// Metadata entry, ignore
					} else {
						targetFile.getParentFile().mkdirs();
						try (FileOutputStream fos = new FileOutputStream(targetFile)) {
							int len;
							while ((len = zis.read(buffer)) > 0) {
								fos.write(buffer, 0, len);
							}
						}

						if (name.startsWith("converted/")) {
							String[] segments = name.split("/");
							if (segments.length >= 2) {
								importedGames.add(segments[1]);
							}
						} else if (name.startsWith("data/")) {
							String[] segments = name.split("/");
							if (segments.length >= 2) {
								importedSaves.add(segments[1]);
							}
						}
					}
				}
				zis.closeEntry();
			}

			// Synchronize Room database to immediately register all restored games
			if (appRepository != null) {
				appRepository.recheckApps();
			}

			result.success = true;
			result.gamesCount = importedGames.size();
			result.savesCount = importedSaves.size();
		} catch (Exception e) {
			Log.e(TAG, "importBackup error", e);
			result.success = false;
			result.errorMessage = e.getMessage();
		} finally {
			if (zis != null) {
				try { zis.close(); } catch (Exception ignored) {}
			}
		}
		return result;
	}

	private static void writeZipEntry(ZipOutputStream zos, String name, byte[] data) throws IOException {
		ZipEntry entry = new ZipEntry(name);
		zos.putNextEntry(entry);
		zos.write(data);
		zos.closeEntry();
	}

	private static void restorePreferences(Context context, String json) {
		try {
			Type type = new TypeToken<Map<String, Object>>() {}.getType();
			Map<String, Object> map = new Gson().fromJson(json, type);
			if (map == null) return;
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
			SharedPreferences.Editor editor = sp.edit();
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				String key = entry.getKey();
				Object val = entry.getValue();
				if (val instanceof Boolean) {
					editor.putBoolean(key, (Boolean) val);
				} else if (val instanceof Double || val instanceof Float) {
					double d = ((Number) val).doubleValue();
					if (d == (int) d) {
						editor.putInt(key, (int) d);
					} else {
						editor.putFloat(key, (float) d);
					}
				} else if (val instanceof Integer) {
					editor.putInt(key, (Integer) val);
				} else if (val instanceof Long) {
					editor.putLong(key, (Long) val);
				} else if (val instanceof String) {
					editor.putString(key, (String) val);
				}
			}
			editor.apply();
		} catch (Exception e) {
			Log.w(TAG, "Error restoring preferences", e);
		}
	}
}
