package com.streamyauto.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val server: String,
    val username: String,
    val password: String,
    /** Extension des flux live : "m3u8" (HLS) ou "ts" (MPEG-TS). */
    val streamFormat: String = "m3u8"
)

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey val streamId: Int,
    val accountId: Long,
    val name: String,
    val icon: String?,
    val categoryId: String?,
    val streamUrl: String
)

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val streamId: Int,
    val name: String,
    val icon: String?,
    val streamUrl: String,
    val position: Int
)
