package com.swent.mapin.ui.map

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Circular location control used on the map screen to jump back to the user position. */
@Composable
fun LocationButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
  FilledIconButton(
      onClick = onClick,
      modifier = modifier.defaultMinSize(48.dp, 48.dp).testTag("locationButton"),
      shape = CircleShape,
      colors =
          IconButtonDefaults.filledIconButtonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary)) {
        Icon(
            imageVector = Icons.Default.MyLocation,
            contentDescription = "Center on my location",
            modifier = Modifier.size(24.dp))
      }
}
