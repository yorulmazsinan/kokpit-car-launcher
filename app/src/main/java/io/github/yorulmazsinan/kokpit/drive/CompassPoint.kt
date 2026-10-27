package io.github.yorulmazsinan.kokpit.drive

import androidx.annotation.StringRes
import io.github.yorulmazsinan.kokpit.R

enum class CompassPoint(@StringRes val label: Int) {
    N(R.string.compass_n), NE(R.string.compass_ne), E(R.string.compass_e), SE(R.string.compass_se),
    S(R.string.compass_s), SW(R.string.compass_sw), W(R.string.compass_w), NW(R.string.compass_nw);

    companion object {
        /** Eight 45° sectors, N covering [-22.5°, 22.5°). */
        fun fromBearing(deg: Float): CompassPoint {
            val normalized = ((deg % 360f) + 360f) % 360f
            return entries[((normalized + 22.5f) / 45f).toInt() % 8]
        }
    }
}
