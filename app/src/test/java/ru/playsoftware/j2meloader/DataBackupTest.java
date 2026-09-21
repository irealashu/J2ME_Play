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

package ru.playsoftware.j2meloader;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.util.DataBackupManager;

public class DataBackupTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testBackupZipStructure() throws Exception {
		File emuDir = tempFolder.newFolder("emulator");
		Config.initDirs(emuDir.getAbsolutePath());

		// Create mock converted game folder
		File convertedDir = new File(emuDir, "converted");
		File gameDir = new File(convertedDir, "TestGame");
		gameDir.mkdirs();
		File dexFile = new File(gameDir, Config.MIDLET_DEX_FILE);
		try (FileOutputStream fos = new FileOutputStream(dexFile)) {
			fos.write("mock_dex_bytecode".getBytes(StandardCharsets.UTF_8));
		}

		// Create mock data / save state folder
		File dataDir = new File(emuDir, "data");
		File gameSaveDir = new File(dataDir, "TestGame");
		gameSaveDir.mkdirs();
		File rmsFile = new File(gameSaveDir, "recordStore_1.rsh");
		try (FileOutputStream fos = new FileOutputStream(rmsFile)) {
			fos.write("mock_rms_header_data".getBytes(StandardCharsets.UTF_8));
		}

		// Create mock configs folder
		File configsDir = new File(emuDir, "configs");
		File gameConfigDir = new File(configsDir, "TestGame");
		gameConfigDir.mkdirs();
		File configFile = new File(gameConfigDir, "config.json");
		try (FileOutputStream fos = new FileOutputStream(configFile)) {
			fos.write("{\"screenWidth\":240,\"screenHeight\":320}".getBytes(StandardCharsets.UTF_8));
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataBackupManager.BackupResult result = DataBackupManager.exportBackup(null, baos, null);

		Assert.assertTrue("Export should be successful: " + result.errorMessage, result.success);
		Assert.assertEquals("Should count 1 game", 1, result.gamesCount);
		Assert.assertEquals("Should count 1 save state", 1, result.savesCount);

		// Verify zip entries
		byte[] zipBytes = baos.toByteArray();
		Assert.assertTrue("Zip bytes should not be empty", zipBytes.length > 0);

		boolean foundGame = false;
		boolean foundSave = false;
		boolean foundConfig = false;
		boolean foundMeta = false;

		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				String name = entry.getName();
				if (name.contains("converted/TestGame/converted.dex")) {
					foundGame = true;
				} else if (name.contains("data/TestGame/recordStore_1.rsh")) {
					foundSave = true;
				} else if (name.contains("configs/TestGame/config.json")) {
					foundConfig = true;
				} else if (name.equals("backup_info.json")) {
					foundMeta = true;
				}
				zis.closeEntry();
			}
		}

		Assert.assertTrue("Should contain game dex", foundGame);
		Assert.assertTrue("Should contain save state", foundSave);
		Assert.assertTrue("Should contain config", foundConfig);
		Assert.assertTrue("Should contain backup metadata", foundMeta);

		// Now test round-trip import into a new empty emulator directory
		File newEmuDir = tempFolder.newFolder("restored_emulator");
		Config.initDirs(newEmuDir.getAbsolutePath());

		DataBackupManager.RestoreResult restoreResult = DataBackupManager.importBackup(
				null,
				new ByteArrayInputStream(zipBytes),
				null,
				null
		);

		Assert.assertTrue("Restore should succeed: " + restoreResult.errorMessage, restoreResult.success);
		Assert.assertEquals("Restored games count should match", 1, restoreResult.gamesCount);
		Assert.assertEquals("Restored saves count should match", 1, restoreResult.savesCount);

		// Check restored files exist on disk
		File restoredDex = new File(newEmuDir, "converted/TestGame/converted.dex");
		File restoredRms = new File(newEmuDir, "data/TestGame/recordStore_1.rsh");
		File restoredConfig = new File(newEmuDir, "configs/TestGame/config.json");

		Assert.assertTrue("Restored dex file should exist", restoredDex.exists());
		Assert.assertTrue("Restored RMS file should exist", restoredRms.exists());
		Assert.assertTrue("Restored config file should exist", restoredConfig.exists());
	}
}
