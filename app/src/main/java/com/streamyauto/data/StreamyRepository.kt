package com.streamyauto.data

import androidx.room.withTransaction
import com.streamyauto.data.db.AppDatabase
import com.streamyauto.data.model.Account
import com.streamyauto.data.model.Channel
import com.streamyauto.data.model.Favorite
import com.streamyauto.data.xtream.XtreamCategory
import com.streamyauto.data.xtream.XtreamRepository
import kotlinx.coroutines.flow.Flow

/** Point d'accès unique aux données, partagé par le téléphone et Android Auto. */
class StreamyRepository(private val db: AppDatabase) {

    private val xtream = XtreamRepository()
    private val accountDao = db.accountDao()
    private val favoriteDao = db.favoriteDao()

    val accounts: Flow<List<Account>> = accountDao.getAll()
    val favorites: Flow<List<Favorite>> = favoriteDao.getAll()

    suspend fun checkAccount(account: Account): String? = xtream.checkAccount(account)

    suspend fun saveAccount(account: Account): Long =
        accountDao.insert(account.copy(server = XtreamRepository.normalizeServer(account.server)))

    suspend fun deleteAccount(account: Account) = db.withTransaction {
        favoriteDao.deleteByAccount(account.id)
        accountDao.delete(account)
        renumber(favoriteDao.getAllOnce())
    }

    suspend fun categories(account: Account): List<XtreamCategory> =
        xtream.getLiveCategories(account)

    suspend fun channels(account: Account, categoryId: String?): List<Channel> =
        xtream.getLiveStreams(account, categoryId)

    suspend fun topFavorites(): List<Favorite> = favoriteDao.getTopFavorites()

    suspend fun favoriteById(id: Long): Favorite? = favoriteDao.getById(id)

    /** Ajoute la chaîne en fin de liste, ou la retire si elle est déjà favorite. */
    suspend fun toggleFavorite(channel: Channel) = db.withTransaction {
        val existing = favoriteDao.find(channel.accountId, channel.streamId)
        if (existing != null) {
            favoriteDao.delete(existing)
            renumber(favoriteDao.getAllOnce())
        } else {
            favoriteDao.insert(
                Favorite(
                    accountId = channel.accountId,
                    streamId = channel.streamId,
                    name = channel.name,
                    icon = channel.icon,
                    streamUrl = channel.streamUrl,
                    position = favoriteDao.maxPosition() + 1
                )
            )
        }
    }

    suspend fun removeFavorite(favorite: Favorite) = db.withTransaction {
        favoriteDao.delete(favorite)
        renumber(favoriteDao.getAllOnce())
    }

    /** Déplace un favori de [delta] positions (-1 = monter, +1 = descendre). */
    suspend fun moveFavorite(favorite: Favorite, delta: Int) = db.withTransaction {
        val list = favoriteDao.getAllOnce().toMutableList()
        val from = list.indexOfFirst { it.id == favorite.id }
        val to = from + delta
        if (from < 0 || to !in list.indices) return@withTransaction
        val item = list.removeAt(from)
        list.add(to, item)
        renumber(list)
    }

    private suspend fun renumber(list: List<Favorite>) {
        favoriteDao.updateAll(list.mapIndexed { index, fav -> fav.copy(position = index) })
    }
}
