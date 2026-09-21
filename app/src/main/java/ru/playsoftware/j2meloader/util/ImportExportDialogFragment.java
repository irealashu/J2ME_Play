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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.applist.AppListModel;
import ru.playsoftware.j2meloader.appsdb.AppRepository;

public class ImportExportDialogFragment extends DialogFragment {
	private static final String TAG = "ImportExportDialog";

	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final Handler mainHandler = new Handler(Looper.getMainLooper());

	private ActivityResultLauncher<String> exportDocLauncher;
	private ActivityResultLauncher<String[]> importDocLauncher;

	private TextView tvStats;
	private LinearLayout llProgress;
	private TextView tvProgressStatus;
	private LinearLayout llActions;
	private Button btnExportSave;
	private Button btnExportShare;
	private Button btnImportSelect;
	private Button btnDialogClose;

	private AppRepository appRepository;
	private boolean isBusy = false;

	public static ImportExportDialogFragment newInstance() {
		return new ImportExportDialogFragment();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			AppListModel appListModel = new ViewModelProvider(requireActivity()).get(AppListModel.class);
			appRepository = appListModel.getAppRepository();
		} catch (Exception e) {
			// May not be inside MainActivity/AppListModel (e.g. MicroActivity)
			appRepository = null;
		}

		exportDocLauncher = registerForActivityResult(
				new ActivityResultContracts.CreateDocument("application/zip"),
				this::onExportDestinationSelected
		);

		importDocLauncher = registerForActivityResult(
				new ActivityResultContracts.OpenDocument(),
				this::onImportFileSelected
		);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.setCanceledOnTouchOutside(false);
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.dialog_import_export, container, false);

		tvStats = view.findViewById(R.id.tv_stats);
		llProgress = view.findViewById(R.id.ll_progress);
		tvProgressStatus = view.findViewById(R.id.tv_progress_status);
		llActions = view.findViewById(R.id.ll_actions);
		btnExportSave = view.findViewById(R.id.btn_export_save);
		btnExportShare = view.findViewById(R.id.btn_export_share);
		btnImportSelect = view.findViewById(R.id.btn_import_select);
		btnDialogClose = view.findViewById(R.id.btn_dialog_close);

		updateStats();

		btnExportSave.setOnClickListener(v -> {
			if (isBusy) return;
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
			exportDocLauncher.launch("J2ME_Play_Backup_" + timeStamp + ".zip");
		});

		btnExportShare.setOnClickListener(v -> {
			if (isBusy) return;
			startShareExport();
		});

		btnImportSelect.setOnClickListener(v -> {
			if (isBusy) return;
			importDocLauncher.launch(new String[]{
					"application/zip",
					"application/x-zip-compressed",
					"application/octet-stream",
					"*/*"
			});
		});

		btnDialogClose.setOnClickListener(v -> {
			if (!isBusy) {
				dismiss();
			}
		});

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		Dialog dialog = getDialog();
		if (dialog != null && dialog.getWindow() != null) {
			int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
			dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
		}
	}

	private void updateStats() {
		Context context = getContext();
		if (context == null) return;

		executor.execute(() -> {
			DataBackupManager.BackupStats stats = DataBackupManager.getStats(context);
			mainHandler.post(() -> {
				if (isAdded() && tvStats != null) {
					tvStats.setText(getString(R.string.status_current_data, stats.gamesCount, stats.savesCount));
				}
			});
		});
	}

	private void setBusy(boolean busy, String statusMessage) {
		isBusy = busy;
		setCancelable(!busy);
		Dialog d = getDialog();
		if (d != null) {
			d.setCanceledOnTouchOutside(!busy);
		}
		if (llProgress != null) {
			llProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
		}
		if (llActions != null) {
			llActions.setVisibility(busy ? View.GONE : View.VISIBLE);
		}
		if (btnDialogClose != null) {
			btnDialogClose.setEnabled(!busy);
			btnDialogClose.setAlpha(busy ? 0.5f : 1.0f);
		}
		if (tvProgressStatus != null && statusMessage != null) {
			tvProgressStatus.setText(statusMessage);
		}
	}

	private void onExportDestinationSelected(Uri destinationUri) {
		if (destinationUri == null) return;
		Context context = getContext();
		if (context == null) return;

		setBusy(true, getString(R.string.exporting_progress));

		executor.execute(() -> {
			DataBackupManager.BackupResult result = DataBackupManager.exportToUri(
					context,
					destinationUri,
					(current, total, status) -> mainHandler.post(() -> {
						if (isAdded() && tvProgressStatus != null) {
							if (total > 0) {
								tvProgressStatus.setText(getString(R.string.exporting_progress) + " (" + current + "/" + total + ")");
							} else {
								tvProgressStatus.setText(status);
							}
						}
					})
			);

			mainHandler.post(() -> {
				if (!isAdded() || getActivity() == null) return;
				setBusy(false, null);
				if (result.success) {
					new AlertDialog.Builder(getActivity())
							.setTitle(R.string.export_success_title)
							.setMessage(getString(R.string.export_success_msg, result.gamesCount, result.savesCount))
							.setIcon(R.drawable.ic_export_backup)
							.setPositiveButton(android.R.string.ok, null)
							.show();
				} else {
					Toast.makeText(getContext(), "Export failed: " + result.errorMessage, Toast.LENGTH_LONG).show();
				}
				updateStats();
			});
		});
	}

	private void startShareExport() {
		Context context = getContext();
		if (context == null) return;

		setBusy(true, getString(R.string.exporting_progress));

		executor.execute(() -> {
			DataBackupManager.BackupResult result = DataBackupManager.exportToShareableFile(
					context,
					(current, total, status) -> mainHandler.post(() -> {
						if (isAdded() && tvProgressStatus != null) {
							if (total > 0) {
								tvProgressStatus.setText(getString(R.string.exporting_progress) + " (" + current + "/" + total + ")");
							} else {
								tvProgressStatus.setText(status);
							}
						}
					})
			);

			mainHandler.post(() -> {
				if (!isAdded() || getActivity() == null) return;
				setBusy(false, null);
				if (result.success && result.uri != null) {
					Intent shareIntent = new Intent(Intent.ACTION_SEND);
					shareIntent.setType("application/zip");
					shareIntent.putExtra(Intent.EXTRA_STREAM, result.uri);
					shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					startActivity(Intent.createChooser(shareIntent, getString(R.string.share_backup)));
				} else {
					Toast.makeText(getContext(), "Export failed: " + result.errorMessage, Toast.LENGTH_LONG).show();
				}
				updateStats();
			});
		});
	}

	private void onImportFileSelected(Uri sourceUri) {
		if (sourceUri == null || getActivity() == null) return;

		boolean isInGame = getActivity().getClass().getSimpleName().equals("MicroActivity");

		new AlertDialog.Builder(getActivity())
				.setTitle(R.string.import_confirm_title)
				.setMessage(isInGame ? R.string.import_in_game_warning : R.string.import_confirm_msg)
				.setIcon(R.drawable.ic_import_backup)
				.setPositiveButton(R.string.import_data, (dialog, which) -> executeImport(sourceUri, isInGame))
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void executeImport(Uri sourceUri, boolean isInGame) {
		Context context = getContext();
		if (context == null) return;

		setBusy(true, getString(R.string.importing_progress));

		executor.execute(() -> {
			DataBackupManager.RestoreResult result = DataBackupManager.importFromUri(
					context,
					sourceUri,
					appRepository,
					(current, total, status) -> mainHandler.post(() -> {
						if (isAdded() && tvProgressStatus != null) {
							tvProgressStatus.setText(getString(R.string.importing_progress) + " (" + current + " files)");
						}
					})
			);

			mainHandler.post(() -> {
				if (!isAdded() || getActivity() == null) return;
				setBusy(false, null);
				if (result.success) {
					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
							.setTitle(R.string.import_success_title)
							.setMessage(getString(R.string.import_success_msg, result.gamesCount, result.savesCount))
							.setIcon(R.drawable.ic_import_backup);

					if (isInGame) {
						builder.setPositiveButton(R.string.return_to_home, (d, w) -> {
							dismiss();
							if (getActivity() != null) {
								getActivity().finish();
							}
						});
					} else {
						builder.setPositiveButton(android.R.string.ok, (d, w) -> {
							updateStats();
						});
					}
					builder.show();
				} else {
					Toast.makeText(getContext(), "Import failed: " + result.errorMessage, Toast.LENGTH_LONG).show();
				}
				updateStats();
			});
		});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		executor.shutdown();
	}
}
