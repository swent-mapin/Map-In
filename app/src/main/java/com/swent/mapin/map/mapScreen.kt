package com.swent.mapin.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.swent.mapin.testing.UiTestTags

@Composable
fun MapScreen(
    ) {
    // Placeholder map
    Box(Modifier.fillMaxSize().testTag(UiTestTags.MAP_SCREEN), contentAlignment = Alignment.Center) {
        Text("Map placeholder")
    }
}
