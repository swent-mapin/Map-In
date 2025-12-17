// Assisted by AI
package com.swent.mapin.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight

/**
 * Reusable TopAppBar with back navigation and debounce protection.
 *
 * This component is used across multiple screens to ensure consistent UI and behavior. It includes:
 * - Title with consistent styling
 * - Back button with debounce protection against rapid clicks
 * - Transparent background with proper color scheme integration
 *
 * @param title The title text to display in the TopAppBar
 * @param onNavigateBack Callback invoked when the back button is clicked (debounced)
 * @param modifier Optional modifier for the IconButton (e.g., testTag)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardTopAppBar(title: String, onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
  var hasNavigatedBack by remember { mutableStateOf(false) }

  TopAppBar(
      title = {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
      },
      navigationIcon = {
        IconButton(
            onClick = {
              if (!hasNavigatedBack) {
                hasNavigatedBack = true
                onNavigateBack()
              }
            },
            modifier = modifier.testTag("backButton")) {
              Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Back",
                  tint = MaterialTheme.colorScheme.onSurface)
            }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(
              containerColor = Color.Transparent,
              titleContentColor = MaterialTheme.colorScheme.onSurface,
              navigationIconContentColor = MaterialTheme.colorScheme.onSurface))
}
