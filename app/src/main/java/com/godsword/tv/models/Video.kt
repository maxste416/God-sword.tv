package com.godsword.tv.models

data class Video(
    val id: String,
    val title: String,
    val description: String,
    val duration: String,
    val thumbnailUrl: String,
    val videoUrl: String,
    val category: String,
    var isFavorite: Boolean = false
)
