package com.example.teste

import androidx.annotation.DrawableRes

enum class TipoRemedio(
    val id: String,
    @DrawableRes val drawable: Int
) {
    BRANCO_REDONDO("branco_redondo", R.drawable.white_pill),
    BRANCO_OVAL("branco_oval", R.drawable.white_pill2),
    AZUL_REDONDO("azul_redondo", R.drawable.blue_pill),
    AMARELO_REDONDO("amarelo_redondo", R.drawable.yellow_pill),
    POMADA("pomada", R.drawable.pomada),
    INJETAVEL("injetavel", R.drawable.injetavel),
    OLHO("ocular", R.drawable.ocular),
    NASAL("nasal", R.drawable.nasal),
    GOTA("gota", R.drawable.drop_white);

    companion object {
        fun fromId(id: String): TipoRemedio {
            return entries.firstOrNull {it.id == id} ?: BRANCO_REDONDO
        }
    }
}
