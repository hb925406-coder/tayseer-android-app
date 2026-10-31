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
        Student::class,
        AttendanceRecord::class,
        EvaluationRecord::class,
        User::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tayseer_database"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // إضافة الحسابات الافتراضية تلقائياً عند أول إنشاء لقاعدة البيانات
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    val dao = database.appDao()
                                    if (dao.getUsersCount() == 0) {
                                        dao.insertUser(User(username = "admin", passwordHash = "123456", role = "ADMIN"))
                                        dao.insertUser(User(username = "teacher", passwordHash = "123456", role = "TEACHER"))
                                        dao.insertUser(User(username = "parent", passwordHash = "123456", role = "PARENT"))
                                    }
                                }
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // التأكد الدائم من وجود الحسابات حتى عند التحديثات
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    val dao = database.appDao()
                                    if (dao.getUsersCount() == 0) {
                                        dao.insertUser(User(username = "admin", passwordHash = "123456", role = "ADMIN"))
                                        dao.insertUser(User(username = "teacher", passwordHash = "123456", role = "TEACHER"))
                                        dao.insertUser(User(username = "parent", passwordHash = "123456", role = "PARENT"))
                                    }
                                }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
