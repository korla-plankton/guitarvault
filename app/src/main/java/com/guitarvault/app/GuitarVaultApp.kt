package com.guitarvault.app

import android.app.Application
import com.guitarvault.app.data.storage.JsonStorage

class GuitarVaultApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize JSON storage — loads collection from disk
        JsonStorage.getInstance(this)
    }
}
