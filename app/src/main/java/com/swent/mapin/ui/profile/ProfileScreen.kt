package com.swent.mapin.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.swent.mapin.model.UserProfile

/**
 * Profile screen displaying user information with edit capabilities.
 *
 * Features:
 * - View mode: displays user profile information
 * - Edit mode: allows user to modify their profile
 * - Navigation back to previous screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onNavigateBack: () -> Unit, viewModel: ProfileViewModel = viewModel()) {
  val userProfile by viewModel.userProfile.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag("profileScreen"),
      topBar = {
        TopAppBar(
            title = {
              Text(
                  "Profile",
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold)
            },
            navigationIcon = {
              IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("backButton")) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            },
            actions = {
              if (!viewModel.isEditMode) {
                FloatingActionButton(
                    onClick = { viewModel.startEditing() },
                    modifier = Modifier.testTag("editButton").padding(end = 8.dp).size(48.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                      Icon(
                          imageVector = Icons.Default.Edit,
                          contentDescription = "Edit Profile",
                          modifier = Modifier.size(24.dp))
                    }
              }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary))
      }) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .verticalScroll(rememberScrollState())
                      .imePadding() // Add IME padding to handle keyboard
              ) {
                // Banner Section
                Box(modifier = Modifier.fillMaxWidth()) {
                  ProfileBanner(
                      bannerUrl =
                          if (viewModel.isEditMode && viewModel.selectedBanner.isNotEmpty()) {
                            viewModel.selectedBanner
                          } else {
                            userProfile.bannerUrl
                          },
                      isEditMode = viewModel.isEditMode,
                      onBannerClick = { viewModel.toggleBannerSelector() })

                  // Avatar positionné en haut, chevauchant la bannière
                  Box(
                      modifier = Modifier.align(Alignment.BottomCenter).offset(y = 50.dp),
                      contentAlignment = Alignment.Center) {
                        ProfilePicture(
                            avatarUrl =
                                if (viewModel.isEditMode && viewModel.selectedAvatar.isNotEmpty()) {
                                  viewModel.selectedAvatar
                                } else {
                                  userProfile.avatarUrl
                                },
                            isEditMode = viewModel.isEditMode,
                            onAvatarClick = { viewModel.toggleAvatarSelector() })
                      }
                }

                Column(
                    modifier =
                        Modifier.fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 20.dp)
                            .padding(top = 20.dp)
                            .padding(bottom = 32.dp) // Add extra bottom padding for keyboard
                            .animateContentSize(
                                animationSpec =
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow)),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                      if (viewModel.isEditMode) {
                        EditProfileContent(viewModel = viewModel)
                      } else {
                        ViewProfileContent(userProfile = userProfile, viewModel = viewModel)
                      }

                      Spacer(modifier = Modifier.height(16.dp))
                    }
              }

          // Avatar Selector Dialog
          if (viewModel.showAvatarSelector) {
            AvatarSelectorDialog(
                viewModel = viewModel,
                selectedAvatar = viewModel.selectedAvatar,
                onAvatarSelected = { avatarUrl ->
                  viewModel.updateAvatarSelection(avatarUrl)
                  viewModel.toggleAvatarSelector()
                },
                onDismiss = { viewModel.toggleAvatarSelector() })
          }

          // Banner Selector Dialog
          if (viewModel.showBannerSelector) {
            BannerSelectorDialog(
                viewModel = viewModel,
                selectedBanner = viewModel.selectedBanner,
                onBannerSelected = { bannerUrl ->
                  viewModel.updateBannerSelection(bannerUrl)
                  viewModel.toggleBannerSelector()
                },
                onDismiss = { viewModel.toggleBannerSelector() })
          }

          // Delete Confirmation Dialog
          if (viewModel.showDeleteConfirmation) {
            DeleteProfileConfirmationDialog(
                onConfirm = { viewModel.deleteProfile() },
                onDismiss = { viewModel.hideDeleteDialog() })
          }
        }
      }
}

/** Displays the user's profile picture or a placeholder. */
@Composable
private fun ProfilePicture(avatarUrl: String?, isEditMode: Boolean, onAvatarClick: () -> Unit) {
  Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
    // Outer glow effect
    Box(
        modifier =
            Modifier.size(106.dp)
                .clip(CircleShape)
                .background(
                    brush =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    Color.Transparent))))

    // Gradient border with rainbow effect
    Box(
        modifier =
            Modifier.size(100.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    brush =
                        Brush.sweepGradient(
                            colors =
                                listOf(
                                    Color(0xFF667eea), // Purple
                                    Color(0xFF764ba2), // Dark purple
                                    Color(0xFFf093fb), // Pink
                                    Color(0xFF4facfe), // Blue
                                    Color(0xFF00f2fe), // Cyan
                                    Color(0xFFfa709a), // Rose
                                    Color(0xFF667eea) // Back to purple
                                    )))
                .then(if (isEditMode) Modifier.clickable { onAvatarClick() } else Modifier),
        contentAlignment = Alignment.Center) {
          // Inner circle with profile picture
          Box(
              modifier =
                  Modifier.size(92.dp)
                      .clip(CircleShape)
                      .background(
                          brush =
                              Brush.linearGradient(
                                  colors =
                                      listOf(
                                          MaterialTheme.colorScheme.primaryContainer,
                                          MaterialTheme.colorScheme.tertiaryContainer)))
                      .testTag("profilePicture"),
              contentAlignment = Alignment.Center) {
                // Check if avatarUrl is an HTTP URL (uploaded image) or an icon ID
                if (avatarUrl != null && avatarUrl.startsWith("http")) {
                  // Display uploaded image from Firebase Storage
                  AsyncImage(
                      model = avatarUrl,
                      contentDescription = "Profile Picture",
                      modifier = Modifier.fillMaxSize().clip(CircleShape))
                } else {
                  // Display icon for preset avatars
                  Icon(
                      imageVector = getAvatarIcon(avatarUrl),
                      contentDescription = "Profile Picture",
                      modifier = Modifier.size(46.dp),
                      tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }

                // Edit indicator
                if (isEditMode) {
                  Box(
                      modifier =
                          Modifier.align(Alignment.BottomEnd)
                              .padding(4.dp)
                              .size(28.dp)
                              .clip(CircleShape)
                              .background(Color(0xFF667eea))
                              .shadow(4.dp, CircleShape),
                      contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change Avatar",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp))
                      }
                }
              }
        }
  }
}

/** View mode: displays profile information in cards. */
@Composable
private fun ViewProfileContent(userProfile: UserProfile, viewModel: ProfileViewModel) {
  // Name card with gradient and large prominence
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .shadow(6.dp, RoundedCornerShape(16.dp))
              .testTag("profileCard_Name"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color.Transparent),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .background(
                        brush =
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))))
                    .padding(16.dp),
            contentAlignment = Alignment.Center) {
              Text(
                  text = userProfile.name,
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = Color.White)
            }
      }

  Spacer(modifier = Modifier.height(12.dp))

  // Bio
  ProfileInfoCard(
      title = "Bio",
      content = if (userProfile.bio.isEmpty()) "No bio added" else userProfile.bio,
      icon = Icons.Default.Face,
      gradientColors = listOf(Color(0xFFfa709a), Color(0xFFfee140)))

  Spacer(modifier = Modifier.height(12.dp))

  // Location
  ProfileInfoCard(
      title = "Location",
      content = userProfile.location,
      icon = Icons.Default.LocationOn,
      gradientColors = listOf(Color(0xFF30cfd0), Color(0xFF330867)))

  Spacer(modifier = Modifier.height(12.dp))

  // Hobbies with special styling
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .shadow(4.dp, RoundedCornerShape(16.dp))
              .testTag("profileCard_Hobbies"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color.Transparent),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .background(
                        brush =
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFf093fb), Color(0xFFf5576c))))) {
              Row(
                  modifier = Modifier.padding(16.dp),
                  verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier.size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center) {
                          Icon(
                              imageVector = Icons.Default.FavoriteBorder,
                              contentDescription = null,
                              tint = Color.White,
                              modifier = Modifier.size(20.dp))
                        }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Hobbies",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector =
                                if (userProfile.hobbiesVisible) Icons.Default.Face
                                else Icons.Default.Lock,
                            contentDescription =
                                if (userProfile.hobbiesVisible) "Public" else "Private",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp))
                      }
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                          text =
                              if (userProfile.hobbies.isEmpty()) "No hobbies added"
                              else userProfile.hobbies.joinToString(", "),
                          style = MaterialTheme.typography.bodyMedium,
                          color = Color.White,
                          fontWeight = FontWeight.Medium)
                    }
                  }
            }
      }

  Spacer(modifier = Modifier.height(24.dp))

  // Delete Profile Button
  OutlinedButton(
      onClick = { viewModel.showDeleteDialog() },
      modifier = Modifier.fillMaxWidth().height(48.dp).testTag("deleteProfileButton"),
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFef5350)),
      border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFef5350))) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = "Delete Profile",
                  modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                  "Delete Profile",
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = FontWeight.Bold)
            }
      }
}

/** Reusable card component for displaying profile information. */
@Composable
private fun ProfileInfoCard(
    title: String,
    content: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    isVisible: Boolean = true
) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .shadow(6.dp, RoundedCornerShape(20.dp))
              .testTag("profileCard_$title"),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = Color.Transparent),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .background(brush = Brush.linearGradient(colors = gradientColors))) {
              Row(
                  modifier = Modifier.padding(16.dp),
                  verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier.size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center) {
                          Icon(
                              imageVector = icon,
                              contentDescription = null,
                              tint = Color.White,
                              modifier = Modifier.size(24.dp))
                        }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isVisible) Icons.Default.Face else Icons.Default.Lock,
                            contentDescription = if (isVisible) "Public" else "Private",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp))
                      }
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                          text = content,
                          style = MaterialTheme.typography.bodyLarge,
                          color = Color.White,
                          fontWeight = FontWeight.Medium)
                    }
                  }
            }
      }
}

/** Edit mode: displays editable text fields for profile information. */
@Composable
private fun EditProfileContent(viewModel: ProfileViewModel) {
  Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier =
                    Modifier.size(28.dp)
                        .clip(CircleShape)
                        .background(
                            brush =
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF667eea), Color(0xFF764ba2)))),
                contentAlignment = Alignment.Center) {
                  Icon(
                      imageVector = Icons.Default.Edit,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(14.dp))
                }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Edit Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
              value = viewModel.editName,
              onValueChange = { viewModel.updateEditName(it) },
              label = { Text("Name", fontWeight = FontWeight.SemiBold) },
              leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF667eea),
                    modifier = Modifier.size(20.dp))
              },
              modifier = Modifier.fillMaxWidth().testTag("editNameField"),
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              colors =
                  OutlinedTextFieldDefaults.colors(
                      focusedBorderColor =
                          if (viewModel.nameError != null) Color(0xFFef5350) else Color(0xFF667eea),
                      unfocusedBorderColor =
                          if (viewModel.nameError != null) Color(0xFFef5350)
                          else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                      focusedLabelColor =
                          if (viewModel.nameError != null) Color(0xFFef5350)
                          else Color(0xFF667eea)),
              isError = viewModel.nameError != null,
              supportingText =
                  if (viewModel.nameError != null) {
                    {
                      Text(
                          text = viewModel.nameError!!,
                          color = Color(0xFFef5350),
                          style = MaterialTheme.typography.bodySmall)
                    }
                  } else null)

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
              value = viewModel.editBio,
              onValueChange = { viewModel.updateEditBio(it) },
              label = { Text("Bio", fontWeight = FontWeight.SemiBold) },
              leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = Color(0xFFfa709a),
                    modifier = Modifier.size(20.dp))
              },
              modifier = Modifier.fillMaxWidth().height(120.dp).testTag("editBioField"),
              minLines = 4,
              maxLines = 4,
              shape = RoundedCornerShape(12.dp),
              colors =
                  OutlinedTextFieldDefaults.colors(
                      focusedBorderColor =
                          if (viewModel.bioError != null) Color(0xFFef5350) else Color(0xFFfa709a),
                      unfocusedBorderColor =
                          if (viewModel.bioError != null) Color(0xFFef5350)
                          else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                      focusedLabelColor =
                          if (viewModel.bioError != null) Color(0xFFef5350) else Color(0xFFfa709a)),
              isError = viewModel.bioError != null,
              supportingText =
                  if (viewModel.bioError != null) {
                    {
                      Text(
                          text = viewModel.bioError!!,
                          color = Color(0xFFef5350),
                          style = MaterialTheme.typography.bodySmall)
                    }
                  } else null)

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
              value = viewModel.editLocation,
              onValueChange = { viewModel.updateEditLocation(it) },
              label = { Text("Location", fontWeight = FontWeight.SemiBold) },
              leadingIcon = {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF30cfd0),
                    modifier = Modifier.size(20.dp))
              },
              modifier = Modifier.fillMaxWidth().testTag("editLocationField"),
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              colors =
                  OutlinedTextFieldDefaults.colors(
                      focusedBorderColor =
                          if (viewModel.locationError != null) Color(0xFFef5350)
                          else Color(0xFF30cfd0),
                      unfocusedBorderColor =
                          if (viewModel.locationError != null) Color(0xFFef5350)
                          else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                      focusedLabelColor =
                          if (viewModel.locationError != null) Color(0xFFef5350)
                          else Color(0xFF30cfd0)),
              isError = viewModel.locationError != null,
              supportingText =
                  if (viewModel.locationError != null) {
                    {
                      Text(
                          text = viewModel.locationError!!,
                          color = Color(0xFFef5350),
                          style = MaterialTheme.typography.bodySmall)
                    }
                  } else null)

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
              value = viewModel.editHobbies,
              onValueChange = { viewModel.updateEditHobbies(it) },
              label = { Text("Hobbies", fontWeight = FontWeight.SemiBold) },
              leadingIcon = {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = Color(0xFFf5576c),
                    modifier = Modifier.size(20.dp))
              },
              modifier = Modifier.fillMaxWidth().testTag("editHobbiesField"),
              minLines = 1,
              maxLines = 1,
              placeholder = { Text("e.g., Music, Sports") },
              shape = RoundedCornerShape(12.dp),
              colors =
                  OutlinedTextFieldDefaults.colors(
                      focusedBorderColor =
                          if (viewModel.hobbiesError != null) Color(0xFFef5350)
                          else Color(0xFFf5576c),
                      unfocusedBorderColor =
                          if (viewModel.hobbiesError != null) Color(0xFFef5350)
                          else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                      focusedLabelColor =
                          if (viewModel.hobbiesError != null) Color(0xFFef5350)
                          else Color(0xFFf5576c)),
              isError = viewModel.hobbiesError != null,
              supportingText =
                  if (viewModel.hobbiesError != null) {
                    {
                      Text(
                          text = viewModel.hobbiesError!!,
                          color = Color(0xFFef5350),
                          style = MaterialTheme.typography.bodySmall)
                    }
                  } else null)

          Spacer(modifier = Modifier.height(12.dp))

          // Hobbies visibility toggle - compact version
          Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                      imageVector =
                          if (viewModel.hobbiesVisible) Icons.Default.FavoriteBorder
                          else Icons.Default.Lock,
                      contentDescription = null,
                      tint =
                          if (viewModel.hobbiesVisible) Color(0xFFf5576c)
                          else MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                      text = if (viewModel.hobbiesVisible) "Public" else "Private",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = viewModel.hobbiesVisible,
                    onCheckedChange = { viewModel.toggleHobbiesVisibility() },
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFf5576c),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline),
                    modifier = Modifier.testTag("hobbiesVisibilitySwitch"))
              }

          Spacer(modifier = Modifier.height(32.dp))

          // Action buttons with gradient backgrounds
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.cancelEditing() },
                    modifier = Modifier.weight(1f).height(40.dp).testTag("cancelButton"),
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFef5350))) {
                      Text(
                          "Cancel",
                          fontWeight = FontWeight.Bold,
                          style = MaterialTheme.typography.bodySmall)
                    }

                // Save button with gradient background
                Box(
                    modifier =
                        Modifier.weight(1f)
                            .height(40.dp)
                            .shadow(6.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush =
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))))) {
                      Button(
                          onClick = { viewModel.saveProfile() },
                          modifier = Modifier.fillMaxSize().testTag("saveButton"),
                          shape = RoundedCornerShape(12.dp),
                          colors =
                              ButtonDefaults.buttonColors(
                                  containerColor = Color.Transparent, contentColor = Color.White)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center) {
                                  Icon(
                                      imageVector = Icons.Default.Check,
                                      contentDescription = null,
                                      tint = Color.White,
                                      modifier = Modifier.size(14.dp))
                                  Spacer(modifier = Modifier.width(4.dp))
                                  Text(
                                      "Save",
                                      fontWeight = FontWeight.Bold,
                                      style = MaterialTheme.typography.bodySmall,
                                      color = Color.White)
                                }
                          }
                    }
              }
        }
      }
}

/** Data class for avatar options */
data class AvatarOption(val id: String, val icon: ImageVector)

/** Data class for banner options with color gradients */
data class BannerOption(val id: String, val colors: List<Color>, val name: String)

/** List of available avatars */
private val availableAvatars =
    listOf(
        AvatarOption("person", Icons.Default.Person),
        AvatarOption("face", Icons.Default.Face),
        AvatarOption("star", Icons.Default.Star),
        AvatarOption("favorite", Icons.Default.Favorite))

/** List of available banner gradients */
private val availableBanners =
    listOf(
        BannerOption(
            id = "purple_blue",
            colors = listOf(Color(0xFF667eea), Color(0xFF764ba2)),
            name = "Purple Blue"),
        BannerOption(
            id = "pink_orange",
            colors = listOf(Color(0xFFfa709a), Color(0xFFfee140)),
            name = "Pink Orange"),
        BannerOption(
            id = "cyan_purple",
            colors = listOf(Color(0xFF30cfd0), Color(0xFF330867)),
            name = "Cyan Purple"),
        BannerOption(
            id = "pink_red",
            colors = listOf(Color(0xFFf093fb), Color(0xFFf5576c)),
            name = "Pink Red"),
        BannerOption(
            id = "blue_cyan",
            colors = listOf(Color(0xFF4facfe), Color(0xFF00f2fe)),
            name = "Blue Cyan"),
        BannerOption(
            id = "green_blue",
            colors = listOf(Color(0xFF43e97b), Color(0xFF38f9d7)),
            name = "Green Blue"),
        BannerOption(
            id = "orange_red",
            colors = listOf(Color(0xFFfa8231), Color(0xFFf7464a)),
            name = "Orange Red"),
        BannerOption(
            id = "purple_pink",
            colors = listOf(Color(0xFFa8edea), Color(0xFFfed6e3)),
            name = "Purple Pink"),
        BannerOption(
            id = "yellow_orange",
            colors = listOf(Color(0xFFffecd2), Color(0xFFfcb69f)),
            name = "Yellow Orange"),
        BannerOption(
            id = "blue_indigo",
            colors = listOf(Color(0xFF667eea), Color(0xFF8e9eef)),
            name = "Blue Indigo"),
        BannerOption(
            id = "rose_gold",
            colors = listOf(Color(0xFFf857a6), Color(0xFFff5858)),
            name = "Rose Gold"),
        BannerOption(
            id = "ocean", colors = listOf(Color(0xFF2e3192), Color(0xFF1bffff)), name = "Ocean"))

/** Get avatar icon from URL/ID */
private fun getAvatarIcon(avatarUrl: String?): ImageVector {
  if (avatarUrl.isNullOrEmpty()) return Icons.Default.Person

  return availableAvatars.find { it.id == avatarUrl }?.icon ?: Icons.Default.Person
}

/** Avatar selector dialog with gallery import option */
@Composable
private fun AvatarSelectorDialog(
    viewModel: ProfileViewModel,
    selectedAvatar: String,
    onAvatarSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
  val context = LocalContext.current

  // Image picker launcher
  val imagePickerLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri?
        ->
        uri?.let {
          viewModel.uploadAvatarImage(it)
          onDismiss()
        }
      }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = {
        Text(
            text = "Choose Your Avatar",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Import from gallery button
          OutlinedButton(
              onClick = { imagePickerLauncher.launch("image/*") },
              modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF667eea))) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                          imageVector = Icons.Default.Face,
                          contentDescription = "Import from gallery",
                          modifier = Modifier.size(20.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Import from Gallery", fontWeight = FontWeight.Bold)
                    }
              }

          Text(
              text = "Or choose a preset avatar:",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 8.dp))

          LazyVerticalGrid(
              columns = GridCells.Fixed(4),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.fillMaxWidth().height(300.dp)) {
                items(availableAvatars) { avatar ->
                  val isSelected = selectedAvatar == avatar.id
                  Box(
                      modifier =
                          Modifier.size(70.dp)
                              .clip(CircleShape)
                              .background(
                                  if (isSelected) {
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF667eea), Color(0xFF764ba2)))
                                  } else {
                                    Brush.linearGradient(
                                        colors =
                                            listOf(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                MaterialTheme.colorScheme.surfaceVariant))
                                  })
                              .clickable { onAvatarSelected(avatar.id) }
                              .testTag("avatar_${avatar.id}"),
                      contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = avatar.icon,
                            contentDescription = avatar.id,
                            modifier = Modifier.size(40.dp),
                            tint =
                                if (isSelected) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                }
              }
        }
      },
      confirmButton = {
        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667eea))) {
              Text("Close")
            }
      },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(24.dp))
}

/** Banner section at the top of the profile screen. */
@Composable
fun ProfileBanner(bannerUrl: String?, isEditMode: Boolean, onBannerClick: () -> Unit) {
  // Find the selected banner gradient
  val selectedBannerGradient = availableBanners.find { it.id == bannerUrl }

  val backgroundBrush =
      if (selectedBannerGradient != null) {
        Brush.horizontalGradient(colors = selectedBannerGradient.colors)
      } else {
        Brush.verticalGradient(
            colors =
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)))
      }

  Box(
      modifier =
          Modifier.fillMaxWidth()
              .height(180.dp)
              .background(brush = backgroundBrush)
              .clickable(enabled = isEditMode, onClick = onBannerClick)
              .testTag("profileBanner")) {
        // Optional overlay for better text visibility
        if (bannerUrl.isNullOrEmpty()) {
          Box(
              modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)),
              contentAlignment = Alignment.Center) {
                if (isEditMode) {
                  Text(
                      text = "Tap to choose a banner",
                      style = MaterialTheme.typography.bodyMedium,
                      color = Color.White.copy(alpha = 0.7f),
                      fontWeight = FontWeight.SemiBold)
                }
              }
        }

        // Edit icon overlay
        if (isEditMode) {
          Box(
              modifier =
                  Modifier.align(Alignment.BottomEnd)
                      .padding(16.dp)
                      .size(40.dp)
                      .clip(CircleShape)
                      .background(Color.White.copy(alpha = 0.9f))
                      .shadow(4.dp, CircleShape),
              contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Change Banner",
                    tint = Color(0xFF667eea),
                    modifier = Modifier.size(20.dp))
              }
        }
      }
}

/** Banner selector dialog */
@Composable
private fun BannerSelectorDialog(
    viewModel: ProfileViewModel,
    selectedBanner: String,
    onBannerSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
  val context = LocalContext.current

  // Image picker launcher for banner
  val bannerPickerLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri?
        ->
        uri?.let {
          viewModel.uploadBannerImage(it)
          onDismiss()
        }
      }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = {
        Text(
            text = "Choose Your Banner",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Import from gallery button
          OutlinedButton(
              onClick = { bannerPickerLauncher.launch("image/*") },
              modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF667eea))) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                          imageVector = Icons.Default.Face,
                          contentDescription = "Import from gallery",
                          modifier = Modifier.size(20.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Import from Gallery", fontWeight = FontWeight.Bold)
                    }
              }

          Text(
              text = "Or choose a preset banner:",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 8.dp))

          LazyVerticalGrid(
              columns = GridCells.Fixed(2),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth().height(250.dp)) {
                items(availableBanners) { banner ->
                  val isSelected = selectedBanner == banner.id
                  Column(
                      horizontalAlignment = Alignment.CenterHorizontally,
                      modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(colors = banner.colors))
                                    .clickable { onBannerSelected(banner.id) }
                                    .testTag("banner_${banner.id}"),
                            contentAlignment = Alignment.Center) {
                              if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp))
                              }
                            }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = banner.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.85)
                      }
                }
              }
        }
      },
      confirmButton = {
        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667eea))) {
              Text("Close")
            }
      },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(20.dp))
}

/** Confirmation dialog for profile deletion */
@Composable
fun DeleteProfileConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = {
        Text(
            text = "Confirm Deletion",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)
      },
      text = {
        Text(
            text = "Are you sure you want to delete your profile? This action cannot be undone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
      },
      confirmButton = {
        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFef5350))) {
              Text("Delete Profile", color = Color.White)
            }
      },
      dismissButton = {
        OutlinedButton(
            onClick = onDismiss,
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary)) {
              Text("Cancel")
            }
      },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(16.dp))
}
