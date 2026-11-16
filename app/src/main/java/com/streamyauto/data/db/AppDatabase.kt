package com.streamyauto.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.streamyauto.data.model.Account
import com.streamyauto.data.model.Channel
import com.streamyauto.data.model.Favorite
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts")
    fun getAll(): Flow<List<Account>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long

    @Delete
    suspend fun delete(account: Account)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY position ASC")
    fun getAll(): Flow<List<Favorite>>

    @Query("SELECT * FROM favorites ORDER BY position ASC LIMIT 12")
    suspend fun getTopFavorites(): List<Favorite>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: Favorite)

    @Delete
    suspend fun delete(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE streamId = :streamId")
    suspend fun deleteByStreamId(streamId: Int)
}

@Database(
    entities = [Account::class, Channel::class, Favorite::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun favoriteDao(): FavoriteDao
}
