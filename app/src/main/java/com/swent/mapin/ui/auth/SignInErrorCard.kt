package com.swent.mapin.ui.auth

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.swent.mapin.R
import com.swent.mapin.testing.UiTestTags

/**
 * A reusable error card component for displaying authentication errors.
 *
 * Features:
 * - Dismissible with a close button
 * - Accessible with proper semantics and live region for screen reader announcements
 * - Uses Material 3 error container colors
 *
 * @param errorMessage The error message to display. If blank, the card will not render.
 * @param onDismiss Callback invoked when the dismiss button is clicked.
 * @param modifier Optional modifier for the card.
 */
@Composable
fun SignInErrorCard(errorMessage: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
  // Guard against empty or blank messages
  if (errorMessage.isBlank()) return

  Card(
      modifier =
          modifier.fillMaxWidth().testTag(UiTestTags.AUTH_ERROR_CARD).semantics {
            // Mark as assertive live region so screen readers announce immediately
            liveRegion = LiveRegionMode.Assertive
          },
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(
              imageVector = Icons.Filled.Error,
              contentDescription = stringResource(R.string.signin_error_icon_cd),
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
              text = errorMessage,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.weight(1f).testTag(UiTestTags.AUTH_ERROR_TEXT))
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(24.dp).testTag(UiTestTags.AUTH_ERROR_DISMISS)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.signin_error_dismiss_cd),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp))
              }
        }
      }
}
