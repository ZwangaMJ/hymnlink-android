package com.example.divineroster.data.entity

/** Single document at `config/wordOfTheDay`, edited daily by an admin. */
data class WordOfTheDay(
    val text: String = "",
    val reference: String = "",
    val updatedAt: Long = 0L,
    val updatedByName: String = ""
)
