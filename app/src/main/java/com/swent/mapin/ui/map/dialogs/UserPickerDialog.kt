package com.swent.mapin.ui.map.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

// Assisted by AI

private val USER_DIALOG_LIST_HEIGHT = 250.dp

/**
 * Simple user picker dialog for tagging users in memories.
 *
 * @param onUserSelected Callback when user selects a user to tag
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun UserPickerDialog(onUserSelected: (String) -> Unit, onDismiss: () -> Unit) {
  var userIdInput by remember { mutableStateOf("") }

  // TODO: Replace with actual friend list from repository
  val sampleUsers = listOf("user1", "user2", "user3", "user4", "user5")

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Tag people", style = MaterialTheme.typography.titleLarge) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
              value = userIdInput,
              onValueChange = { userIdInput = it },
              modifier = Modifier.fillMaxWidth(),
              placeholder = { Text("Enter user ID or search...") },
              leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
              },
              singleLine = true)

          Spacer(modifier = Modifier.height(16.dp))

          LazyColumn(modifier = Modifier.fillMaxWidth().height(USER_DIALOG_LIST_HEIGHT)) {
            items(sampleUsers) { userId ->
              Card(
                  modifier =
                      Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                        onUserSelected(userId)
                      },
                  colors =
                      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  shape = RoundedCornerShape(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                          Box(
                              modifier =
                                  Modifier.size(40.dp)
                                      .clip(CircleShape)
                                      .background(MaterialTheme.colorScheme.primaryContainer),
                              contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
                              }
                          Text(
                              text = userId,
                              style = MaterialTheme.typography.bodyLarge,
                              color = MaterialTheme.colorScheme.onSurface)
                        }
                  }
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } })
}
