package com.heartbeats.ui.theme

import androidx.compose.ui.graphics.Color
import com.heartbeats.domain.model.ZoneStatus

internal val HeartRed = Color(0xFFE5484D)
internal val InZoneGreen = Color(0xFF30A46C)
internal val BelowAmber = Color(0xFFFFB224)
internal val AboveRed = Color(0xFFE5484D)

internal val ScreenBackground = Color(0xFF000000)
internal val SurfaceDark = Color(0xFF1C1B1F)
internal val OnDark = Color(0xFFE6E1E5)

/** Color-codes the zone status: green = in zone, amber = below, red = above. */
fun zoneStatusColor(status: ZoneStatus): Color = when (status) {
    ZoneStatus.IN_ZONE -> InZoneGreen
    ZoneStatus.BELOW -> BelowAmber
    ZoneStatus.ABOVE -> AboveRed
}
