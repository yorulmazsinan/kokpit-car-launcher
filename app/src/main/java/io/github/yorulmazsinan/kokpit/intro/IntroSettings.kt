package io.github.yorulmazsinan.kokpit.intro

enum class IntroMode { OFF, SELECTED, SEQUENTIAL, RANDOM }

data class IntroSettings(
    val mode: IntroMode = IntroMode.OFF,
    val selectedId: String? = null,
    val everyWake: Boolean = true,
    val withSound: Boolean = false,
    val lastPlayedIndex: Int = -1,
)
