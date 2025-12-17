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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swent.mapin.model.preferences.PreferencesRepositoryProvider
import com.swent.mapin.ui.components.StandardTopAppBar
import com.swent.mapin.util.BiometricAuthManager
import kotlinx.coroutines.launch

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
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    passwordChangeSuccess: Boolean? = null
) {
  val context = LocalContext.current
  val preferencesRepository = remember { PreferencesRepositoryProvider.getInstance(context) }
  val viewModel: SettingsViewModel =
      viewModel(factory = SettingsViewModel.Factory(preferencesRepository))

  // Add ChangePasswordViewModel to check authentication type
  val changePasswordViewModel: ChangePasswordViewModel =
      viewModel(
          factory =
              ChangePasswordViewModel.Factory(
                  com.swent.mapin.model.changepassword.ChangePasswordRepositoryProvider
                      .getRepository()))

  val isEmailPasswordUser = remember { changePasswordViewModel.isEmailPasswordUser() }

  val themeMode by viewModel.themeMode.collectAsState()
  val mapPreferences by viewModel.mapPreferences.collectAsState()
  val biometricUnlockEnabled by viewModel.biometricUnlockEnabled.collectAsState()
  val errorMessage by viewModel.errorMessage.collectAsState()
  var showDeleteConfirmation by remember { mutableStateOf(false) }
  var showLogoutConfirmation by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  // Initialize BiometricAuthManager and get activity context
  val biometricAuthManager = remember { BiometricAuthManager() }
  val activity = context as? FragmentActivity
  val canUseBiometric =
      remember(activity) { activity?.let { biometricAuthManager.canUseBiometric(it) } ?: false }
  val coroutineScope = rememberCoroutineScope()

  // Display error messages in Snackbar
  LaunchedEffect(errorMessage) {
    errorMessage?.let { message ->
      snackbarHostState.showSnackbar(message)
      viewModel.clearErrorMessage()
    }
  }

  // Display success message when password is changed
  LaunchedEffect(passwordChangeSuccess) {
    if (passwordChangeSuccess == true) {
      snackbarHostState.showSnackbar("Password changed successfully")
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag("settingsScreen"),
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = { StandardTopAppBar(title = "Settings", onNavigateBack = onNavigateBack) }) {
          paddingValues ->
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
                        currentMode = themeMode, onModeChanged = { viewModel.updateThemeMode(it) })

                    Spacer(modifier = Modifier.height(24.dp))

                    // Map Settings Section
                    SettingsSectionTitle(title = "Map Settings", icon = Icons.Default.Map)

                    Spacer(modifier = Modifier.height(12.dp))

                    // POIs Visibility Toggle
                    SettingsToggleItem(
                        title = "Points of Interest",
                        subtitle = "Show POI labels on the map",
                        isEnabled = mapPreferences.showPOIs,
                        onToggle = { viewModel.updateShowPOIs(it) },
                        testTag = "poiToggle")

                    Spacer(modifier = Modifier.height(12.dp))

                    // Road Numbers Visibility Toggle
                    SettingsToggleItem(
                        title = "Road Labels",
                        subtitle = "Display road labels on the map",
                        isEnabled = mapPreferences.showRoadNumbers,
                        onToggle = { viewModel.updateShowRoadNumbers(it) },
                        testTag = "roadNumbersToggle")

                    Spacer(modifier = Modifier.height(12.dp))

                    // Street Names Visibility Toggle
                    SettingsToggleItem(
                        title = "Transit Labels",
                        subtitle = "Show transit and street labels",
                        isEnabled = mapPreferences.showStreetNames,
                        onToggle = { viewModel.updateShowStreetNames(it) },
                        testTag = "streetNamesToggle")

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3D View Toggle
                    SettingsToggleItem(
                        title = "3D Buildings",
                        subtitle = "Enable 3D buildings on the map",
                        icon = Icons.Default.ViewInAr,
                        useIcon = true,
                        isEnabled = mapPreferences.enable3DView,
                        onToggle = { viewModel.updateEnable3DView(it) },
                        testTag = "threeDViewToggle")

                    Spacer(modifier = Modifier.height(24.dp))

                    // Account Settings Section
                    SettingsSectionTitle(title = "Account", icon = Icons.Default.Settings)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Biometric Unlock Toggle (only show if device supports it)
                    if (canUseBiometric) {
                      BiometricUnlockToggle(
                          isEnabled = biometricUnlockEnabled,
                          biometricAuthManager = biometricAuthManager,
                          activity = activity,
                          onEnableSuccess = { viewModel.updateBiometricUnlockEnabled(true) },
                          onEnableError = { error ->
                            viewModel.clearErrorMessage()
                            coroutineScope.launch {
                              snackbarHostState.showSnackbar(
                                  "Biometric authentication failed: $error")
                            }
                          },
                          onDisable = { viewModel.updateBiometricUnlockEnabled(false) })

                      Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Change Password Button (only for email/password users)
                    if (isEmailPasswordUser) {
                      SettingsActionButton(
                          label = "Change Password",
                          description = "Update your account password",
                          icon = Icons.Default.Key,
                          backgroundColor = Color(0xFF667eea),
                          onAction = onNavigateToChangePassword,
                          testTag = "changePasswordButton")

                      Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Logout Button
                    SettingsActionButton(
                        label = "Logout",
                        description = "Sign out of your account",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        backgroundColor = Color(0xFF667eea),
                        onAction = { showLogoutConfirmation = true },
                        testTag = "logoutButton")

                    Spacer(modifier = Modifier.height(12.dp))

                    // Delete Account Button
                    SettingsActionButton(
                        label = "Delete Account",
                        description = "Permanently delete your account and data",
                        icon = Icons.Default.Delete,
                        backgroundColor = Color(0xFFef5350),
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
        confirmTestTag = "logoutConfirmButton", // explicit stable tag
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
        confirmTestTag = "deleteAccountConfirmButton", // explicit stable tag
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
private fun ThemeModeSelector(currentMode: ThemeMode, onModeChanged: (ThemeMode) -> Unit) {
  Card(
      modifier = Modifier.fillMaxWidth().testTag("themeModeSelector"),
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
                  modifier = Modifier.testTag("${"themeModeSelector"}_${mode.toStorageString()}")) {
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
internal fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Default.Visibility,
    useIcon: Boolean = false,
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
                              if (useIcon) {
                                icon
                              } else {
                                if (isEnabled) Icons.Default.Visibility
                                else Icons.Default.VisibilityOff
                              },
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
                  modifier = Modifier.testTag("${testTag}_switch"))
            }
      }
}

/** Biometric unlock toggle with authentication verification. */
@Composable
private fun BiometricUnlockToggle(
    isEnabled: Boolean,
    biometricAuthManager: BiometricAuthManager,
    activity: FragmentActivity?,
    onEnableSuccess: () -> Unit,
    onEnableError: (String) -> Unit,
    onDisable: () -> Unit
) {
  SettingsToggleItem(
      title = "Biometric Unlock",
      subtitle = "Use fingerprint or face to unlock the app",
      icon = Icons.Default.Fingerprint,
      useIcon = true,
      isEnabled = isEnabled,
      onToggle = { enabled ->
        handleBiometricToggle(
            enabled = enabled,
            biometricAuthManager = biometricAuthManager,
            activity = activity,
            onEnableSuccess = onEnableSuccess,
            onEnableError = onEnableError,
            onDisable = onDisable)
      },
      testTag = "biometricUnlockToggle")
}

/** Handles the biometric toggle state change with authentication when enabling. */
private fun handleBiometricToggle(
    enabled: Boolean,
    biometricAuthManager: BiometricAuthManager,
    activity: FragmentActivity?,
    onEnableSuccess: () -> Unit,
    onEnableError: (String) -> Unit,
    onDisable: () -> Unit
) {
  if (!enabled) {
    onDisable()
    return
  }

  activity?.let { fragmentActivity ->
    biometricAuthManager.authenticate(
        activity = fragmentActivity,
        onSuccess = onEnableSuccess,
        onError = onEnableError,
        onFallback = { /* User cancelled, don't enable */})
  }
}

/** Action button for settings (Logout, Delete Account, etc.) */
@Composable
private fun SettingsActionButton(
    label: String,
    description: String,
    icon: ImageVector,
    backgroundColor: Color,
    onAction: () -> Unit,
    testTag: String
) {
  Card(
      modifier = Modifier.fillMaxWidth().testTag(testTag).clickable { onAction() },
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
                      color = MaterialTheme.colorScheme.onSurface)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                      text = description,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
              Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                  contentDescription = "Go",
                  tint = backgroundColor.copy(alpha = 0.6f),
                  modifier = Modifier.size(22.dp))
            }
      }
}

/** Generic confirmation dialog */
@Composable
internal fun ConfirmationDialog(
    title: String,
    message: String,
    confirmButtonText: String,
    confirmButtonColor: Color,
    isDangerous: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmTestTag: String? = null // optional explicit stable test tag
) {
  // Prefer an explicit tag; otherwise use a single stable generic tag (do not derive from localized
  // text)
  val resolvedTestTag =
      confirmTestTag
          ?: when {
            title.equals("Confirm Logout", ignoreCase = true) ||
                title.equals("Logout", ignoreCase = true) ||
                confirmButtonText.equals("Logout", ignoreCase = true) -> "logoutConfirmButton"
            title.equals("Delete Account", ignoreCase = true) ||
                title.equals("Delete", ignoreCase = true) ||
                confirmButtonText.contains("Delete", ignoreCase = true) ->
                "deleteAccountConfirmButton"
            else -> "genericConfirmButton"
          }

  // Use isDangerous to optionally override the confirm button color
  val finalConfirmColor = if (isDangerous) MaterialTheme.colorScheme.error else confirmButtonColor

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
            modifier = Modifier.testTag(resolvedTestTag),
            colors = ButtonDefaults.buttonColors(containerColor = finalConfirmColor)) {
              Text(confirmButtonText, color = Color.White, fontWeight = FontWeight.Bold)
            }
      },
      dismissButton = {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("dialogCancelButton"),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary)) {
              Text("Cancel", fontWeight = FontWeight.Bold)
            }
      },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(16.dp))
}
