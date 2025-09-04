package com.example.myapplication.data.model

data class ExperienceBulletsRequest(
    val title: String,
    val company: String,
    val location: String,
    val startYear: String? = null,
    val stackHints: String? = null,
    val current: List<String> = emptyList()
)
