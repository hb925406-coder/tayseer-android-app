package com.tayseer.app.util

import android.content.Context
import android.net.Uri
import com.tayseer.app.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    private const val DB_NAME = "tayseer_database"

    // تصدير نسخة احتياطية من قاعدة البيانات إلى مجلد الكاش لمشاركتها
    suspend fun createBackupFile(context: Context): Result<File> = withContext(Dispatchers.IO) {
        try {
            // إغلاق أي اتصالات معلقة وحفظ التغييرات المؤقتة (checkpoint)
            val db = AppDatabase.getDatabase(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

            val currentDbFile = context.getDatabasePath(DB_NAME)
            if (!currentDbFile.exists()) {
                return@withContext Result.failure(Exception("قاعدة البيانات غير موجودة"))
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
            val backupFile = File(context.cacheDir, "نسخة_تيسير_الاحتياطية_$timeStamp.db")

            FileInputStream(currentDbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            Result.success(backupFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // استعادة قاعدة البيانات من ملف خارجي قام المستخدم باختياره
    suspend fun restoreDatabaseFromUri(context: Context, sourceUri: Uri): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val currentDbFile = context.getDatabasePath(DB_NAME)
            val parentDir = currentDbFile.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }

            // حذف ملفات WAL و SHM السابقة لضمان نظافة الاستعادة
            val walFile = File(currentDbFile.path + "-wal")
            val shmFile = File(currentDbFile.path + "-shm")
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(currentDbFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("تعذر فتح ملف النسخة الاحتياطية"))

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
