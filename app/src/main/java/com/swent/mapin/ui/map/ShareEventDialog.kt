package com.swent.mapin.ui.map

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.swent.mapin.model.event.Event

/**
 * Dialog for sharing event with options to copy URL or share to other apps.
 *
 * @param event The event to share
 * @param onDismiss Callback when dialog is dismissed
 */

// Assisted by AI
@Composable
fun ShareEventDialog(event: Event, onDismiss: () -> Unit) {
  val context = LocalContext.current

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(text = "Share Event", modifier = Modifier.testTag("shareDialogTitle")) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          ShareOption(
              icon = {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_save),
                    contentDescription = "Copy link",
                    modifier = Modifier.size(24.dp))
              },
              text = "Copy Link",
              onClick = {
                copyEventUrlToClipboard(context, event)
                onDismiss()
              },
              modifier = Modifier.testTag("copyLinkOption"))

          Spacer(modifier = Modifier.height(8.dp))

          ShareOption(
              icon = {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    modifier = Modifier.size(24.dp))
              },
              text = "Share to...",
              onClick = {
                shareEventToOtherApps(context, event)
                onDismiss()
              },
              modifier = Modifier.testTag("shareToAppsOption"))
        }
      },
      confirmButton = {
        TextButton(onClick = onDismiss, modifier = Modifier.testTag("shareDialogDismiss")) {
          Text("Cancel")
        }
      },
      modifier = Modifier.testTag("shareEventDialog"))
}

/** A single share option row in the dialog */
@Composable
private fun ShareOption(
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
      modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
      }
}

/** Copies the event URL to the clipboard */
private fun copyEventUrlToClipboard(context: Context, event: Event) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  val eventUrl = event.url ?: "https://mapin.app/events/${event.uid}"
  val clip = ClipData.newPlainText("Event URL", eventUrl)
  clipboard.setPrimaryClip(clip)

  android.widget.Toast.makeText(
          context, "Link copied to clipboard", android.widget.Toast.LENGTH_SHORT)
      .show()
}

/** Opens a share intent to share the event to other apps */
private fun shareEventToOtherApps(context: Context, event: Event) {
  val eventUrl = event.url ?: "https://mapin.app/events/${event.uid}"
  val shareText = "Check out this event: ${event.title}\n\n$eventUrl"

  val sendIntent =
      Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
      }

  val shareIntent = Intent.createChooser(sendIntent, "Share event via")
  context.startActivity(shareIntent)
}
