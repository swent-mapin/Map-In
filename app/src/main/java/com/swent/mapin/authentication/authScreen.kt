package com.swent.mapin.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.swent.mapin.testing.UiTestTags

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
) {
  // Placeholder auth: just a button to simulate successful sign-in
  Box(
      modifier = Modifier.fillMaxSize().testTag(UiTestTags.AUTH_SCREEN),
      contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally) {
              Text("Auth placeholder (Sign in / Sign up)")
              Spacer(Modifier.height(12.dp))
              Button(
                  onClick = onAuthSuccess,
                  modifier = Modifier.testTag(UiTestTags.AUTH_CONTINUE_BUTTON)) {
                    Text("Continue to Main Screen: Map")
                  }
            }
      }
}
