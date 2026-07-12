package com.guitarvault.app

import android.app.Application
import com.guitarvault.app.data.storage.JsonStorage

class DeGASApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize JSON storage — loads collection from disk
        JsonStorage.getInstance(this)
    }
}
