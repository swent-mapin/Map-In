// Assisted by AI
package com.swent.mapin.ui.map.directions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swent.mapin.R

/**
 * Composable that displays route information (distance and time) similar to Google Maps style.
 *
 * Shows a compact white card with:
 * - Distance in km or m
 * - Duration in hours and minutes
 *
 * @param routeInfo The route information to display
 * @param modifier Optional modifier for the container
 */
@Composable
fun RouteInfoCard(routeInfo: RouteInfo, modifier: Modifier = Modifier) {
  Box(
      modifier =
          modifier
              .background(color = Color.White, shape = RoundedCornerShape(8.dp))
              .padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
              // Distance
              Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = routeInfo.formatDistance(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F))
                Text(
                    text = stringResource(R.string.route_distance),
                    fontSize = 12.sp,
                    color = Color(0xFF5F6368))
              }

              // Separator
              Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color(0xFFE8EAED)))

              // Duration
              Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = routeInfo.formatDuration(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F))
                Text(
                    text = stringResource(R.string.route_duration),
                    fontSize = 12.sp,
                    color = Color(0xFF5F6368))
              }
            }
      }
}
