package com.streamyauto.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.streamyauto.data.model.Favorite

class FavoritesScreen(carContext: CarContext) : Screen(carContext) {

    // Liste de démonstration – à remplacer par les favoris Room
    private val demoFavorites = listOf(
        Favorite(1, 1, 1001, "TF1", null, "", 0),
        Favorite(2, 1, 1002, "France 2", null, "", 1),
        Favorite(3, 1, 1003, "M6", null, "", 2),
        Favorite(4, 1, 1004, "BFM TV", null, "", 3),
        Favorite(5, 1, 1005, "RMC Sport", null, "", 4),
        Favorite(6, 1, 1006, "Canal+", null, "", 5),
        Favorite(7, 1, 1007, "Eurosport", null, "", 6),
        Favorite(8, 1, 1008, "info", null, "", 7)
    )

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        demoFavorites.forEach { fav ->
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(fav.name)
                    .setOnClickListener { screenManager.push(PlayerScreen(carContext, fav)) }
                    .build()
            )
        }
        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setTitle("Favoris StreamyAuto")
            .setHeaderAction(Action.APP_ICON)
            .build()
    }
}
