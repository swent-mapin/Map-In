package com.swent.mapin.ui.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swent.mapin.model.PreferencesRepositoryProvider

/**
 * Settings screen with theme, map style, map preferences and account management.
 *
 * Features:
 * - Theme mode selection (Light/Dark/System)
 * - Mapbox style selection (Standard/Satellite)
 * - Map element visibility toggles (POIs, road numbers, street names)
 * - Profile button (links to profile)
 * - Logout button
 * - Delete account button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit, onNavigateToSignIn: () -> Unit) {
  val context = LocalContext.current
  val preferencesRepository = remember { PreferencesRepositoryProvider.getInstance(context) }
  val viewModel: SettingsViewModel =
      viewModel(factory = SettingsViewModel.Factory(preferencesRepository))
  val themeMode by viewModel.themeMode.collectAsState()
  val mapPreferences by viewModel.mapPreferences.collectAsState()
  val errorMessage by viewModel.errorMessage.collectAsState()
  var showDeleteConfirmation by remember { mutableStateOf(false) }
  var showLogoutConfirmation by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  // Display error messages in Snackbar
  LaunchedEffect(errorMessage) {
    errorMessage?.let { message ->
      snackbarHostState.showSnackbar(message)
      viewModel.clearErrorMessage()
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag("settingsScreen"),
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        TopAppBar(
            title = {
              Text(
                  "Settings",
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface)
            },
            navigationIcon = {
              IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("backButton")) {
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
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .background(
                        color =
                            if (MaterialTheme.colorScheme.background ==
                                MaterialTheme.colorScheme.surface) {
                              // Light theme: use a slightly gray background
                              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            } else {
                              // Dark theme: use a darker background
                              MaterialTheme.colorScheme.background
                            })
                    .padding(paddingValues)) {
              Column(
                  modifier =
                      Modifier.fillMaxSize()
                          .verticalScroll(rememberScrollState())
                          .padding(16.dp)
                          .animateContentSize(
                              animationSpec =
                                  spring(
                                      dampingRatio = Spring.DampingRatioMediumBouncy,
                                      stiffness = Spring.StiffnessLow))) {
                    // Appearance Settings Section
                    SettingsSectionTitle(title = "Appearance", icon = Icons.Default.Palette)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Theme Mode Selection
                    ThemeModeSelector(
                        currentMode = themeMode,
                        onModeChanged = { viewModel.updateThemeMode(it) },
                        testTag = "themeModeSelector")

                    Spacer(modifier = Modifier.height(24.dp))

                    // Map Settings Section
                    SettingsSectionTitle(title = "Map Settings", icon = Icons.Default.Map)

                    Spacer(modifier = Modifier.height(12.dp))

                    // POIs Visibility Toggle
                    SettingsToggleItem(
                        title = "Points of Interest",
                        subtitle = "Show POI labels on the map",
                        icon = Icons.Default.Visibility,
                        isEnabled = mapPreferences.showPOIs,
                        onToggle = { viewModel.updateShowPOIs(it) },
                        testTag = "poiToggle")

                    Spacer(modifier = Modifier.height(12.dp))

                    // Road Numbers Visibility Toggle
                    SettingsToggleItem(
                        title = "Road Labels",
                        subtitle = "Display road labels on the map",
                        icon = Icons.Default.Visibility,
                        isEnabled = mapPreferences.showRoadNumbers,
                        onToggle = { viewModel.updateShowRoadNumbers(it) },
                        testTag = "roadNumbersToggle")

                    Spacer(modifier = Modifier.height(12.dp))

                    // Street Names Visibility Toggle
                    SettingsToggleItem(
                        title = "Transit Labels",
                        subtitle = "Show transit and street labels",
                        icon = Icons.Default.Visibility,
                        isEnabled = mapPreferences.showStreetNames,
                        onToggle = { viewModel.updateShowStreetNames(it) },
                        testTag = "streetNamesToggle")

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3D View Toggle
                    SettingsToggleItem(
                        title = "3D Buildings",
                        subtitle = "Enable 3D buildings on the map",
                        icon = Icons.Default.ViewInAr,
                        isEnabled = mapPreferences.enable3DView,
                        onToggle = { viewModel.updateEnable3DView(it) },
                        testTag = "threeDViewToggle")

                    Spacer(modifier = Modifier.height(24.dp))

                    // Account Settings Section
                    SettingsSectionTitle(title = "Account", icon = Icons.Default.Settings)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Logout Button
                    SettingsActionButton(
                        label = "Logout",
                        description = "Sign out of your account",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        backgroundColor = Color(0xFF667eea),
                        contentColor = Color.White,
                        onAction = { showLogoutConfirmation = true },
                        testTag = "logoutButton")

                    Spacer(modifier = Modifier.height(12.dp))

                    // Delete Account Button
                    SettingsActionButton(
                        label = "Delete Account",
                        description = "Permanently delete your account and data",
                        icon = Icons.Default.Delete,
                        backgroundColor = Color(0xFFef5350),
                        contentColor = Color.White,
                        onAction = { showDeleteConfirmation = true },
                        testTag = "deleteAccountButton")

                    Spacer(modifier = Modifier.height(32.dp))
                  }
            }
      }

  // Logout Confirmation Dialog
  if (showLogoutConfirmation) {
    ConfirmationDialog(
        title = "Confirm Logout",
        message = "Are you sure you want to log out?",
        confirmButtonText = "Logout",
        confirmButtonColor = Color(0xFF667eea),
        onConfirm = {
          viewModel.signOut()
          onNavigateToSignIn()
        },
        onDismiss = { showLogoutConfirmation = false })
  }

  // Delete Account Confirmation Dialog
  if (showDeleteConfirmation) {
    ConfirmationDialog(
        title = "Delete Account",
        message =
            "Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently deleted.",
        confirmButtonText = "Delete Account",
        confirmButtonColor = Color(0xFFef5350),
        isDangerous = true,
        onConfirm = {
          viewModel.deleteAccount()
          onNavigateToSignIn()
        },
        onDismiss = { showDeleteConfirmation = false })
  }
}

/** Theme mode selector with segmented buttons */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeSelector(
    currentMode: ThemeMode,
    onModeChanged: (ThemeMode) -> Unit,
    testTag: String
) {
  Card(
      modifier = Modifier.fillMaxWidth().testTag(testTag),
      shape = RoundedCornerShape(12.dp),
      colors =
          CardDefaults.cardColors(
              containerColor =
                  if (MaterialTheme.colorScheme.background == MaterialTheme.colorScheme.surface) {
                    // Light theme: white/surface for elevated appearance
                    MaterialTheme.colorScheme.surface
                  } else {
                    // Dark theme: lighter than background for elevation
                    MaterialTheme.colorScheme.surfaceContainerHigh
                  }),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier.size(40.dp)
                        .clip(CircleShape)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center) {
                  Icon(
                      imageVector = Icons.Default.DarkMode,
                      contentDescription = "Theme",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp))
                }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                  text = "Theme Mode",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                  text = "Choose your preferred theme",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
              SegmentedButton(
                  selected = currentMode == mode,
                  onClick = { onModeChanged(mode) },
                  shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                  modifier = Modifier.testTag("${testTag}_${mode.toStorageString()}")) {
                    Text(mode.toDisplayString())
                  }
            }
          }
        }
      }
}

/** Section title for grouping settings */
@Composable
private fun SettingsSectionTitle(title: String, icon: ImageVector) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier.size(32.dp)
                    .clip(CircleShape)
                    .background(
                        brush =
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF667eea), Color(0xFF764ba2)))),
            contentAlignment = Alignment.Center) {
              Icon(
                  imageVector = icon,
                  contentDescription = title,
                  tint = Color.White,
                  modifier = Modifier.size(18.dp))
            }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
      }
}

/** Toggle item for boolean settings */
@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    testTag: String
) {
  Card(
      modifier = Modifier.fillMaxWidth().testTag(testTag),
      shape = RoundedCornerShape(12.dp),
      colors =
          CardDefaults.cardColors(
              containerColor =
                  if (MaterialTheme.colorScheme.background == MaterialTheme.colorScheme.surface) {
                    // Light theme: white/surface for elevated appearance
                    MaterialTheme.colorScheme.surface
                  } else {
                    // Dark theme: lighter than background for elevation
                    MaterialTheme.colorScheme.surfaceContainerHigh
                  }),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { onToggle(!isEnabled) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
              Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.size(40.dp)
                            .clip(CircleShape)
                            .background(
                                color =
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center) {
                      Icon(
                          imageVector =
                              if (isEnabled) Icons.Default.Visibility
                              else Icons.Default.VisibilityOff,
                          contentDescription = title,
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(20.dp))
                    }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                      text = title,
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                      text = subtitle,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
              Spacer(modifier = Modifier.width(8.dp))
              Switch(
                  checked = isEnabled,
                  onCheckedChange = onToggle,
                  colors =
                      SwitchDefaults.colors(
                          checkedThumbColor = Color.White,
                          checkedTrackColor = Color(0xFF667eea),
                          uncheckedThumbColor = Color.White,
                          uncheckedTrackColor = MaterialTheme.colorScheme.outline),
                  modifier = Modifier.testTag("${testTag}_switch"))
            }
      }
}

/** Action button for settings (Logout, Delete Account, etc.) */
@Composable
private fun SettingsActionButton(
    label: String,
    description: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    onAction: () -> Unit,
    testTag: String
) {
  Card(
      modifier = Modifier.fillMaxWidth().testTag(testTag),
      shape = RoundedCornerShape(12.dp),
      colors =
          CardDefaults.cardColors(
              containerColor =
                  if (MaterialTheme.colorScheme.background == MaterialTheme.colorScheme.surface) {
                    MaterialTheme.colorScheme.surface
                  } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                  }),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .background(
                        brush =
                            Brush.horizontalGradient(
                                colors =
                                    listOf(
                                        backgroundColor.copy(alpha = 0.1f),
                                        backgroundColor.copy(alpha = 0.05f))))
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
              Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.size(40.dp)
                            .clip(CircleShape)
                            .background(color = backgroundColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center) {
                      Icon(
                          imageVector = icon,
                          contentDescription = label,
                          tint = backgroundColor,
                          modifier = Modifier.size(20.dp))
                    }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                      text = label,
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.SemiBold,
                      color = backgroundColor)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                      text = description,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
              OutlinedButton(
                  onClick = onAction,
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = backgroundColor),
                  modifier = Modifier.testTag("${testTag}_action")) {
                    Text(
                        "Go",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold)
                  }
            }
      }
}

/** Generic confirmation dialog */
@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmButtonText: String,
    confirmButtonColor: Color,
    isDangerous: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = {
        Text(
            text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
      },
      text = {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
      },
      confirmButton = {
        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(containerColor = confirmButtonColor)) {
              Text(confirmButtonText, color = Color.White, fontWeight = FontWeight.Bold)
            }
      },
      dismissButton = {
        OutlinedButton(
            onClick = onDismiss,
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary)) {
              Text("Cancel", fontWeight = FontWeight.Bold)
            }
      },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(16.dp))
}
