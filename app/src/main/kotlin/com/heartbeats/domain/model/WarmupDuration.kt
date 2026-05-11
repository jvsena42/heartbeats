package com.heartbeats.domain.model

enum class WarmupDuration(val seconds: Int) {
    ONE_MIN(60),
    THREE_MIN(180),
    FIVE_MIN(300),
    TEN_MIN(600),
    ;

    companion object {
        val DEFAULT: WarmupDuration = THREE_MIN
    }
}
