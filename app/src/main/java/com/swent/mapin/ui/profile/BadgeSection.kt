package com.swent.mapin.ui.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swent.mapin.model.badge.Badge
import com.swent.mapin.model.badge.BadgeRarity

/**
 * Maps badge icon names to Material ImageVector icons.
 *
 * @param iconName the icon name stored in the badge
 * @return the corresponding ImageVector, or a default icon if not found
 */
private fun getIconFromName(iconName: String): ImageVector {
  return when (iconName.lowercase()) {
    "star" -> Icons.Default.Star
    "face" -> Icons.Default.Face
    "person" -> Icons.Default.Person
    "emoji_events" -> Icons.Default.EmojiEvents
    else -> Icons.Default.Star // Default fallback
  }
}

/**
 * Displays the badges section in the profile screen.
 *
 * Shows a header with the unlocked count, a grid of badges and a rarity legend. Selecting a badge
 * opens a detail dialog.
 *
 * @param badges list of badges (locked and unlocked) to display
 * @param modifier optional Compose modifier for the section container
 */
@Composable
fun BadgesSection(badges: List<Badge>, modifier: Modifier = Modifier) {
  var selected by remember { mutableStateOf<Badge?>(null) }

  Card(
      modifier = modifier.fillMaxWidth().testTag("badgesSection"),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)

                Text(
                    text = "${badges.count { it.isUnlocked }}/${badges.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("badgeCount"))
              }

          Spacer(modifier = Modifier.height(12.dp))

          LazyVerticalGrid(
              columns = GridCells.Adaptive(minSize = 80.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.fillMaxWidth().height(250.dp)) {
                items(badges) { badge -> BadgeItem(badge) { selected = badge } }
              }

          BadgeRarityLegend()
        }
      }

  selected?.let { BadgeDetailDialog(badge = it) { selected = null } }
}

/**
 * Renders a single badge item inside the badges grid.
 *
 * Shows the badge icon (or a lock if locked), optional progress bar for locked badges with
 * progress, and the badge title.
 *
 * @param badge the Badge model to render
 * @param onClick invoked when the item is clicked
 */
@Composable
private fun BadgeItem(badge: Badge, onClick: () -> Unit) {
  val alpha by
      animateFloatAsState(
          targetValue = if (badge.isUnlocked) 1f else 0.4f,
          animationSpec = tween(300),
          label = "badgeAlpha")
  val rarityColors = getBadgeRarityColors(badge.rarity)

  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.testTag("badgeItem_${badge.id}").clickable { onClick() }) {
        Box(contentAlignment = Alignment.Center) {
          Box(
              modifier =
                  Modifier.size(56.dp)
                      .alpha(alpha)
                      .clip(CircleShape)
                      .background(
                          brush =
                              if (badge.isUnlocked) {
                                Brush.radialGradient(
                                    colors = rarityColors,
                                    center = androidx.compose.ui.geometry.Offset(28f, 28f),
                                    radius = 40f)
                              } else {
                                Brush.radialGradient(
                                    colors =
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surfaceVariant))
                              })
                      .border(
                          width = 2.dp,
                          color =
                              if (badge.isUnlocked) rarityColors.first().copy(alpha = 0.8f)
                              else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                          shape = CircleShape),
              contentAlignment = Alignment.Center) {
                Icon(
                    imageVector =
                        if (badge.isUnlocked) getIconFromName(badge.iconName)
                        else Icons.Default.Lock,
                    contentDescription = badge.title,
                    modifier = Modifier.size(28.dp),
                    tint =
                        if (badge.isUnlocked) Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant)
              }

          if (!badge.isUnlocked && badge.progress > 0) {
            Box(
                modifier = Modifier.size(60.dp).padding(2.dp),
                contentAlignment = Alignment.BottomCenter) {
                  LinearProgressIndicator(
                      progress = badge.progress,
                      modifier =
                          Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                      color = rarityColors.first(),
                      trackColor = Color.Transparent)
                }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = badge.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (badge.isUnlocked) FontWeight.SemiBold else FontWeight.Normal,
            color =
                if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(64.dp))
      }
}

/**
 * Shows a dialog with badge details when a badge is selected.
 *
 * The dialog includes a larger icon, title, rarity label, description and either a progress block
 * (if locked with progress), an "Unlocked" label, or a "Locked" label.
 *
 * @param badge the Badge to display details for
 * @param onDismiss called when the dialog is dismissed
 */
@Composable
private fun BadgeDetailDialog(badge: Badge, onDismiss: () -> Unit = {}) {
  val rarityColors = getBadgeRarityColors(badge.rarity)

  AlertDialog(
      onDismissRequest = onDismiss,
      title = {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()) {
              Box(
                  modifier =
                      Modifier.size(80.dp)
                          .clip(CircleShape)
                          .background(
                              brush =
                                  Brush.radialGradient(
                                      colors =
                                          if (badge.isUnlocked) rarityColors
                                          else
                                              listOf(
                                                  MaterialTheme.colorScheme.surfaceVariant,
                                                  MaterialTheme.colorScheme.surfaceVariant)))
                          .border(
                              width = 3.dp,
                              color =
                                  if (badge.isUnlocked) rarityColors.first().copy(alpha = 0.8f)
                                  else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                              shape = CircleShape),
                  contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector =
                            if (badge.isUnlocked) getIconFromName(badge.iconName)
                            else Icons.Default.Lock,
                        contentDescription = badge.title,
                        modifier = Modifier.size(40.dp),
                        tint =
                            if (badge.isUnlocked) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                  }

              Spacer(modifier = Modifier.height(12.dp))
              Text(
                  text = badge.title,
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                  text = badge.rarity.name.lowercase().replaceFirstChar { it.uppercase() },
                  style = MaterialTheme.typography.labelMedium,
                  color = rarityColors.first(),
                  fontWeight = FontWeight.SemiBold)
            }
      },
      text = {
        Column {
          Text(
              text = badge.description,
              style = MaterialTheme.typography.bodyMedium,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth())

          when {
            !badge.isUnlocked && badge.progress > 0 -> {
              Spacer(modifier = Modifier.height(16.dp))
              Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                      Text(
                          text = "Progress",
                          style = MaterialTheme.typography.labelMedium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant)
                      Text(
                          text = "${(badge.progress * 100).toInt()}%",
                          style = MaterialTheme.typography.labelMedium,
                          fontWeight = FontWeight.SemiBold,
                          color = rarityColors.first())
                    }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = badge.progress,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = rarityColors.first(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant)
              }
            }
            badge.isUnlocked -> {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                  text = "✓ Unlocked",
                  style = MaterialTheme.typography.labelLarge,
                  color = rarityColors.first(),
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.fillMaxWidth())
            }
            else -> {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                  text = "🔒 Locked",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.fillMaxWidth())
            }
          }
        }
      },
      confirmButton = {
        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = rarityColors.first())) {
              Text("Close")
            }
      },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(24.dp))
}

/**
 * Renders the legend for badge rarity levels.
 *
 * Shows a small colored dot and the name for each rarity value.
 */
@Composable
private fun BadgeRarityLegend() {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = "Rarity Levels",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          BadgeRarity.values().forEach { rarity ->
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                  modifier =
                      Modifier.size(12.dp)
                          .clip(CircleShape)
                          .background(brush = Brush.radialGradient(getBadgeRarityColors(rarity))))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                  text = rarity.name.lowercase().replaceFirstChar { it.uppercase() },
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
  }
}

/**
 * Returns a two-color radial gradient for the provided badge rarity.
 *
 * @param rarity the badge rarity
 * @return list of two Colors representing the gradient
 */
private fun getBadgeRarityColors(rarity: BadgeRarity): List<Color> =
    when (rarity) {
      BadgeRarity.COMMON -> listOf(Color(0xFF78909C), Color(0xFF546E7A))
      BadgeRarity.RARE -> listOf(Color(0xFF42A5F5), Color(0xFF1E88E5))
      BadgeRarity.EPIC -> listOf(Color(0xFFAB47BC), Color(0xFF8E24AA))
      BadgeRarity.LEGENDARY -> listOf(Color(0xFFFFB300), Color(0xFFF57C00))
    }
