package com.swent.mapin.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * A circular button that allows users to center the map on their current location.
 *
 * Displays a location icon with a blue background, positioned above the compass.
 *
 * @param onClick Callback invoked when the button is clicked
 * @param modifier Modifier for the button
 */
@Composable
fun LocationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0xFF2196F3)) // Blue color
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(40.dp)
                .testTag("locationButton")
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Center on my location",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
