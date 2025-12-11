package com.swent.mapin.ui.map.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import com.google.gson.JsonPrimitive
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.AnnotationSourceOptions
import com.mapbox.maps.plugin.annotation.ClusterOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.swent.mapin.model.event.Event

fun Context.drawableToBitmap(@DrawableRes drawableResId: Int): Bitmap? {
  val drawable = AppCompatResources.getDrawable(this, drawableResId) ?: return null
  val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
  val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
  val bitmap = createBitmap(width, height)
  val canvas = Canvas(bitmap)
  drawable.setBounds(0, 0, canvas.width, canvas.height)
  drawable.draw(canvas)
  return bitmap
}

/**
 * Holds styling information for map annotations.
 *
 * @property textColorInt Text color for annotation labels (ARGB integer)
 * @property haloColorInt Halo color for text outline (ARGB integer)
 * @property markerBitmap Optional bitmap for the marker icon
 */
@VisibleForTesting
internal data class AnnotationStyle(
    val textColorInt: Int,
    val haloColorInt: Int,
    val markerBitmap: Bitmap?
)

/**
 * Creates annotation styling based on current theme.
 *
 * @param isDarkTheme Whether dark theme is active
 * @param markerBitmap Optional bitmap for marker icon
 * @return AnnotationStyle with theme-appropriate colors
 */
@VisibleForTesting
internal fun createAnnotationStyle(isDarkTheme: Boolean, markerBitmap: Bitmap?): AnnotationStyle {
  val textColor = if (isDarkTheme) Color.White else Color.Black
  val haloColor =
      if (isDarkTheme) {
        Color.Black.copy(alpha = 0.8f)
      } else {
        Color.White.copy(alpha = 0.85f)
      }

  return AnnotationStyle(
      textColorInt = textColor.toArgb(),
      haloColorInt = haloColor.toArgb(),
      markerBitmap = markerBitmap)
}

@VisibleForTesting
internal data class AnnotationVisualParameters(
    val iconSize: Double,
    val textSize: Double,
    val textOffset: List<Double>,
    val textHaloWidth: Double,
    val sortKey: Double
)

@VisibleForTesting
internal fun computeAnnotationVisualParameters(isSelected: Boolean): AnnotationVisualParameters {
  return if (isSelected) {
    AnnotationVisualParameters(
        iconSize = 1.5,
        textSize = 15.0,
        textOffset = listOf(0.0, 0.5),
        textHaloWidth = 2.0,
        sortKey = 0.0)
  } else {
    AnnotationVisualParameters(
        iconSize = 1.0,
        textSize = 12.0,
        textOffset = listOf(0.0, 0.2),
        textHaloWidth = 1.5,
        sortKey = 100.0)
  }
}
/**
 * Converts a list of events to Mapbox point annotation options.
 *
 * Each annotation includes position, icon, label, and custom styling. The index is stored as data
 * for later retrieval. Selected event pins are enlarged.
 *
 * Uses event-specific pins based on the event's first tag and capacity state when eventBitmaps is
 * provided. Falls back to the default marker in the style otherwise.
 *
 * @param events List of events to convert
 * @param style Styling to apply to annotations
 * @param selectedEventId UID of the currently selected event (if any)
 * @param eventBitmaps Optional map of event UID to its specific pin bitmap
 * @return List of configured PointAnnotationOptions
 */
@VisibleForTesting
internal fun createEventAnnotations(
    events: List<Event>,
    style: AnnotationStyle,
    selectedEventId: String? = null,
    eventBitmaps: Map<String, Bitmap>? = null
): List<PointAnnotationOptions> {
  return events
      .filter { it.location.isDefined() }
      .mapIndexed { index, event ->
        val isSelected = event.uid == selectedEventId
        val visual = computeAnnotationVisualParameters(isSelected)

        // Use event-specific bitmap if available, otherwise fall back to default
        val iconBitmap = eventBitmaps?.get(event.uid) ?: style.markerBitmap

        PointAnnotationOptions()
            .withPoint(Point.fromLngLat(event.location.longitude, event.location.latitude))
            .apply { iconBitmap?.let { withIconImage(it) } }
            .withIconSize(visual.iconSize)
            .withIconAnchor(IconAnchor.BOTTOM)
            .withTextAnchor(TextAnchor.TOP)
            .withTextOffset(visual.textOffset)
            .withTextSize(visual.textSize)
            .withTextColor(style.textColorInt)
            .withTextHaloColor(style.haloColorInt)
            .withTextHaloWidth(visual.textHaloWidth)
            .withTextField(event.title)
            .withData(JsonPrimitive(index))
            .withSymbolSortKey(visual.sortKey) // Ensures selected pin is prioritized for visibility
      }
}

/**
 * Creates a map of event UIDs to their corresponding pin bitmaps.
 *
 * Each event gets a pin based on its first tag (determines shape/icon) and its capacity state
 * (determines color: green, orange, or red).
 *
 * @param context The context to load drawable resources
 * @param events List of events to create bitmaps for
 * @return Map of event UID to Bitmap, events with failed bitmap loading are omitted
 */
fun createEventBitmaps(context: Context, events: List<Event>): Map<String, Bitmap> {
  return events
      .mapNotNull { event ->
        getEventPinBitmap(context, event)?.let { bitmap -> event.uid to bitmap }
      }
      .toMap()
}

/**
 * Creates clustering configuration for location annotations.
 *
 * Uses blue gradient colors for cluster sizes and enables touch interaction.
 *
 * @return AnnotationConfig with clustering enabled
 */
@VisibleForTesting
internal fun createClusterConfig(): AnnotationConfig {
  val clusterColorLevels =
      listOf(
          0 to Color(0xFF64B5F6).toArgb(),
          25 to Color(0xFF1E88E5).toArgb(),
          50 to Color(0xFF0D47A1).toArgb())

  return AnnotationConfig(
      annotationSourceOptions =
          AnnotationSourceOptions(
              clusterOptions =
                  ClusterOptions(
                      clusterRadius = 60L,
                      colorLevels = clusterColorLevels,
                      textColor = Color.White.toArgb(),
                      textSize = 12.0)))
}

/**
 * Finds the Event associated with a clicked annotation.
 *
 * First tries to match by stored index data, then falls back to coordinate comparison.
 *
 * @param annotation The clicked point annotation
 * @param events List of all events
 * @return Matching Event or null if not found
 */
@VisibleForTesting
internal fun findEventForAnnotation(
    annotation: com.mapbox.maps.plugin.annotation.generated.PointAnnotation,
    events: List<Event>
): Event? {
  val index = annotation.getData()?.takeIf { it.isJsonPrimitive }?.asInt
  return index?.let { events.getOrNull(it) }
      ?: events.firstOrNull { event ->
        val point = annotation.point
        event.location.latitude == point.latitude() && event.location.longitude == point.longitude()
      }
}
