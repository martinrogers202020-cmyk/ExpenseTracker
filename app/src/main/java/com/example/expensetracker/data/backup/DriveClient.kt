package com.example.expensetracker.data.backup

import android.content.Context

/**
 * Minimal placeholder so your project compiles cleanly.
 * Backup/restore is implemented using Storage Access Framework in the BackupRestoreScreen.
 * You can later replace this with a real Google Drive integration.
 */
class DriveClient(
    private val context: Context
) {
    fun isAvailable(): Boolean = true
}
