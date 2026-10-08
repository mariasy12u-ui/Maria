package com.example.data.model

data class ShortcutItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val iconKey: String = "web"
)
