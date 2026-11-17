package com.streamyauto.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.lifecycleScope
import com.streamyauto.R
import com.streamyauto.StreamyApp
import com.streamyauto.artwork.LogoLoader
import com.streamyauto.data.model.Favorite
import kotlinx.coroutines.launch

/**
 * Favoris affichés en tuiles avec le logo de la chaîne (les premiers, dans
 * l'ordre choisi sur le téléphone). Le nombre de tuiles est limité par l'hôte.
 */
class FavoritesScreen(carContext: CarContext) : Screen(carContext) {

    private val repo = (carContext.applicationContext as StreamyApp).repository
    private var favorites: List<Favorite>? = null
    private val inFlight = mutableSetOf<String>()

    private val maxItems: Int = runCatching {
        carContext.getCarService(ConstraintManager::class.java)
            .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_GRID)
    }.getOrDefault(DEFAULT_MAX_ITEMS).coerceIn(1, DEFAULT_MAX_ITEMS)

    private val placeholder by lazy {
        CarIcon.Builder(
            IconCompat.createWithResource(carContext, R.drawable.ic_launcher_foreground)
        ).build()
    }

    init {
        lifecycleScope.launch {
            repo.favorites.collect { list ->
                favorites = list.take(maxItems)
                invalidate()
                loadLogos(favorites.orEmpty())
            }
        }
    }

    private fun loadLogos(list: List<Favorite>) {
        list.mapNotNull { it.icon?.takeIf { url -> url.isNotBlank() } }
            .distinct()
            .filter { LogoLoader.cached(it) == null && !LogoLoader.hasFailed(it) }
            .filter { inFlight.add(it) }
            .forEach { url ->
                lifecycleScope.launch {
                    LogoLoader.load(url)
                    inFlight.remove(url)
                    // Une tuile change d'état (chargement → logo ou icône par défaut)
                    invalidate()
                }
            }
    }

    override fun onGetTemplate(): Template {
        val list = favorites
        val builder = GridTemplate.Builder()
            .setTitle("StreamyAuto")
            .setHeaderAction(Action.APP_ICON)

        if (list == null) return builder.setLoading(true).build()

        val items = ItemList.Builder()
            .setNoItemsMessage("Ajoute des favoris dans StreamyAuto sur ton téléphone")
        list.forEachIndexed { index, fav ->
            items.addItem(tile(fav) { screenManager.push(PlayerScreen(carContext, list, index)) })
        }
        return builder.setSingleList(items.build()).build()
    }

    private fun tile(fav: Favorite, onClick: () -> Unit): GridItem {
        val builder = GridItem.Builder()
            .setTitle(fav.name)
            .setOnClickListener(onClick)

        val url = fav.icon?.takeIf { it.isNotBlank() }
        val logo = url?.let { LogoLoader.cached(it) }
        when {
            logo != null -> builder.setImage(
                CarIcon.Builder(IconCompat.createWithBitmap(logo)).build(),
                GridItem.IMAGE_TYPE_LARGE
            )
            url != null && !LogoLoader.hasFailed(url) -> builder.setLoading(true)
            else -> builder.setImage(placeholder, GridItem.IMAGE_TYPE_LARGE)
        }
        return builder.build()
    }

    companion object {
        private const val DEFAULT_MAX_ITEMS = 12
    }
}
