package com.streamyauto.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.lifecycleScope
import com.streamyauto.StreamyApp
import com.streamyauto.data.model.Favorite
import kotlinx.coroutines.launch

/** Liste des favoris (les 12 premiers, dans l'ordre choisi sur le téléphone). */
class FavoritesScreen(carContext: CarContext) : Screen(carContext) {

    private val repo = (carContext.applicationContext as StreamyApp).repository
    private var favorites: List<Favorite>? = null

    init {
        lifecycleScope.launch {
            repo.favorites.collect { list ->
                favorites = list.take(MAX_ITEMS)
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {
        val list = favorites
        val builder = ListTemplate.Builder()
            .setTitle("StreamyAuto")
            .setHeaderAction(Action.APP_ICON)

        if (list == null) return builder.setLoading(true).build()

        val items = ItemList.Builder()
            .setNoItemsMessage("Ajoute des favoris dans StreamyAuto sur ton téléphone")
        list.forEachIndexed { index, fav ->
            items.addItem(
                Row.Builder()
                    .setTitle(fav.name)
                    .setOnClickListener {
                        screenManager.push(PlayerScreen(carContext, list, index))
                    }
                    .build()
            )
        }
        return builder.setSingleList(items.build()).build()
    }

    companion object {
        private const val MAX_ITEMS = 12
    }
}
