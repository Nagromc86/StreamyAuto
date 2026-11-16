package com.streamyauto

import android.app.Application
import androidx.room.Room
import com.streamyauto.data.StreamyRepository
import com.streamyauto.data.db.AppDatabase

class StreamyApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: StreamyRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "streamy.db")
            // Prototype : on repart d'une base vide si le schéma change
            .fallbackToDestructiveMigration()
            .build()
        repository = StreamyRepository(database)
    }
}
