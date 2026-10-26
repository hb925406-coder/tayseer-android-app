package com.tayseer.app

import android.app.Application
import com.tayseer.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class TayseerApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy {
        AppDatabase.getDatabase(this, applicationScope)
    }

    val repository by lazy {
        database.appDao()
    }
}
