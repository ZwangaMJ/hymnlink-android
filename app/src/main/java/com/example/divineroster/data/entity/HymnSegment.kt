package com.example.divineroster.data.entity

/** The parts of a divine service that get a hymn number, in the order they happen. */
enum class HymnSegment(val key: String, val label: String) {
    BEFORE_SERVICE_CHOIR("beforeServiceChoir", "Before service Choir"),
    BEFORE_SERVICE("beforeService", "Before service (BS)"),
    OPENING_HYMN("openingHymn", "Opening Hymn (OH)"),
    AFTER_TEXT("afterText", "After Text (AT)"),
    CALL_UP("callUp", "Call up"),
    COMMUNION_HYMN_1("communionHymn1", "Communion Hymn 1"),
    COMMUNION_HYMN_2("communionHymn2", "Communion Hymn 2"),
    AFTER_SERVICE("afterService", "After Service (AS)")
}
