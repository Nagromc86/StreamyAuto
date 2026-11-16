package com.streamyauto

import android.app.Application
import androidx.room.Room
import com.streamyauto.data.db.AppDatabase

class StreamyApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "streamy.db").build()
    }
}
