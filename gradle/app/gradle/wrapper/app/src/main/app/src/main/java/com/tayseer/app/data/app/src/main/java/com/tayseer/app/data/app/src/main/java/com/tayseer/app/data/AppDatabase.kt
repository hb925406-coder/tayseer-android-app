package com.tayseer.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        StudentGroup::class,
        Student::class,
        AttendanceRecord::class,
        EvaluationRecord::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tayseer_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        val dao = database.appDao()
                        // إنشاء حساب الإدارة الافتراضي تلقائياً عند تشغيل التطبيق لأول مرة
                        if (dao.getUsersCount() == 0) {
                            dao.insertUser(
                                User(
                                    username = "admin",
                                    passwordHash = "admin123",
                                    role = "ADMIN"
                                )
                            )
                            // إضافة حلقة/قسم افتراضي كبداية
                            val defaultGroupId = dao.insertGroup(
                                StudentGroup(name = "الحلقة العامة")
                            )
                            // إضافة نموذج طالب للبدء
                            dao.insertStudent(
                                Student(
                                    name = "طالب تجريبي",
                                    groupId = defaultGroupId,
                                    parentPhone = "0500000000"
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
