package com.example.expensetracker.data.backup

import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File

class DriveBackupService(
    private val drive: Drive
) {
    private val backupName = "ExpenseTrackerBackup.json"

    fun upload(json: String) {
        val existing = findFileId()
        val content = ByteArrayContent.fromString("application/json", json)

        if (existing != null) {
            drive.files().update(existing, File().setName(backupName), content)
                .setFields("id, name, modifiedTime")
                .execute()
        } else {
            val meta = File().apply {
                name = backupName
                parents = listOf("appDataFolder")
            }
            drive.files().create(meta, content)
                .setFields("id, name, modifiedTime")
                .execute()
        }
    }

    fun download(): String? {
        val id = findFileId() ?: return null
        val out = java.io.ByteArrayOutputStream()
        drive.files().get(id).executeMediaAndDownloadTo(out)
        return out.toString(Charsets.UTF_8.name())
    }

    private fun findFileId(): String? {
        val q = "name='$backupName' and 'appDataFolder' in parents and trashed=false"
        val res = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ(q)
            .setFields("files(id,name,modifiedTime)")
            .execute()
        return res.files?.firstOrNull()?.id
    }
}
