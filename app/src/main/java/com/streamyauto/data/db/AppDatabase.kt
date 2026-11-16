package com.streamyauto.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.streamyauto.data.model.Account
import com.streamyauto.data.model.Channel
import com.streamyauto.data.model.Favorite
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name")
    fun getAll(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long

    @Delete
    suspend fun delete(account: Account)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY position ASC")
    fun getAll(): Flow<List<Favorite>>

    @Query("SELECT * FROM favorites ORDER BY position ASC")
    suspend fun getAllOnce(): List<Favorite>

    @Query("SELECT * FROM favorites ORDER BY position ASC LIMIT 12")
    suspend fun getTopFavorites(): List<Favorite>

    @Query("SELECT * FROM favorites WHERE id = :id")
    suspend fun getById(id: Long): Favorite?

    @Query("SELECT * FROM favorites WHERE accountId = :accountId AND streamId = :streamId LIMIT 1")
    suspend fun find(accountId: Long, streamId: Int): Favorite?

    @Query("SELECT COALESCE(MAX(position), -1) FROM favorites")
    suspend fun maxPosition(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: Favorite)

    @Update
    suspend fun updateAll(favorites: List<Favorite>)

    @Delete
    suspend fun delete(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE streamId = :streamId")
    suspend fun deleteByStreamId(streamId: Int)

    @Query("DELETE FROM favorites WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: Long)
}

@Database(
    entities = [Account::class, Channel::class, Favorite::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun favoriteDao(): FavoriteDao
}
