package com.guitarvault.app

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.os.ParcelFileDescriptor
import android.util.Log

/**
 * Gate for Android Auto Backup.
 *
 * allowBackup=true is required for the user to be able to opt IN, but the
 * default is OFF: onFullBackup() checks the user preference and simply does
 * nothing unless they explicitly enabled Google backup. No collection data
 * reaches Google's servers until then.
 *
 * Preference lives in plain SharedPreferences ("backup_prefs") so it can be
 * read synchronously here, in the backup process.
 */
class GuitarVaultBackupAgent : BackupAgent() {

    companion object {
        private const val TAG = "GuitarVaultBackup"
        const val PREFS_NAME = "backup_prefs"
        const val KEY_ENABLED = "google_backup_enabled"
    }

    override fun onFullBackup(data: FullBackupDataOutput?) {
        val enabled = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
        if (enabled) {
            Log.i(TAG, "Auto backup enabled by user — performing full backup")
            super.onFullBackup(data)
        } else {
            Log.i(TAG, "Auto backup disabled (default) — skipping, no data uploaded")
        }
    }

    // Key/value backup is unused (fullBackupOnly=true in the manifest), but
    // BackupAgent requires these implementations.
    override fun onBackup(
        request: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?
    ) = Unit

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?
    ) = Unit
}
