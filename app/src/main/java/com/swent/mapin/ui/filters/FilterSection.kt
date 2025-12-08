package com.swent.mapin.ui.filters

import android.app.DatePickerDialog
import android.view.Gravity
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swent.mapin.model.Location
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.location.LocationViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

// Assisted by AI tools

/**
 * Enum defining the two location selection modes in the "Place" filter.
 * - SEARCH: Text-based location search
 * - USER: Use current user geolocation
 */
enum class AroundOption {
  SEARCH,
  USER
}

/**
 * Centralized test tag registry for FiltersSection. All UI elements are tagged for reliable,
 * maintainable Compose UI testing.
 */
object FiltersSectionTestTags {
  const val RESET_BUTTON = "reset_button"
  const val TITLE = "filters_section_title"

  // ToggleSection
  const val TOGGLE_DATE = "toggle_date"
  const val TOGGLE_LOCATION = "toggle_location"
  const val TOGGLE_PRICE = "toggle_price"
  const val TOGGLE_TAGS = "toggle_tags"
  const val TOGGLE_FRIENDS = "toggle_friends_only"
  const val TOGGLE_POPULAR = "toggle_popular_only"

  // DateRangePicker
  const val DATE_PICKER_BUTTON = "date_picker_button"
  const val FROM_DATE_TEXT = "from_date_text"
  const val TO_DATE_TEXT = "to_date_text"

  // AroundSpotPicker
  const val AROUND_SEARCH = "around_search"
  const val AROUND_USER = "around_user"
  const val RADIUS_INPUT = "radius_input"

  // SearchPlacePicker
  const val SEARCH_PLACE_INPUT = "search_place_input"

  // PricePicker
  const val PRICE_INPUT = "price_input"

  // TagsPicker
  fun tag(tagName: String) = "tag_${tagName.lowercase()}"
}

class FiltersSection {
  /**
   * Main entry point for the filter panel UI. Renders a collapsible, interactive filter interface
   * with 6 sections.
   *
   * @param modifier Layout modifier
   * @param filterViewModel Manages all filter states (default: new instance via viewModel())
   * @param locationViewModel Provides location search results
   * @param userProfile Required for "My location" option
   */
  @Composable
  fun Render(
      modifier: Modifier = Modifier,
      filterViewModel: FiltersSectionViewModel = viewModel(),
      locationViewModel: LocationViewModel,
      userProfile: UserProfile
  ) {
    val filters by filterViewModel.filters.collectAsStateWithLifecycle()
    val isWhenChecked by filterViewModel.isWhenChecked
    val isWhereChecked by filterViewModel.isWhereChecked
    val isPriceChecked by filterViewModel.isPriceChecked
    val isTagsChecked by filterViewModel.isTagsChecked
    val errorMessage by filterViewModel.errorMessage.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth()) {
      // Header with title and reset button
      Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(
            text = "Filter Events by:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp).testTag(FiltersSectionTestTags.TITLE))

        TextButton(
            onClick = { filterViewModel.reset() },
            modifier =
                Modifier.padding(bottom = 8.dp).testTag(FiltersSectionTestTags.RESET_BUTTON)) {
              Text("Reset")
            }
      }

      // Date Filter
      ToggleSection(
          title = "Date",
          isChecked = isWhenChecked,
          onCheckedChange = { checked ->
            if (!checked) filterViewModel.resetWhen() else filterViewModel.setWhenChecked(true)
          },
          content = { DateRangePicker(filterViewModel, filters.startDate, filters.endDate) })

      // Place Filter
      ToggleSection(
          title = "Location",
          isChecked = isWhereChecked,
          onCheckedChange = { checked ->
            if (!checked) filterViewModel.resetWhere() else filterViewModel.setWhereChecked(true)
          },
          content = { AroundSpotPicker(filterViewModel, filters, locationViewModel, userProfile) })

      // Price Filter
      ToggleSection(
          title = "Price",
          isChecked = isPriceChecked,
          onCheckedChange = { checked ->
            if (!checked) filterViewModel.resetPrice() else filterViewModel.setPriceChecked(true)
          },
          content = { PricePicker(filterViewModel, filters.maxPrice) })

      // Tags Filter
      ToggleSection(
          title = "Tags",
          isChecked = isTagsChecked,
          onCheckedChange = { checked ->
            if (!checked) filterViewModel.resetTags() else filterViewModel.setIsTagsChecked(true)
          },
          content = { TagsPicker(filterViewModel, filters.tags) })

      // Friends Only
      ToggleSection(
          title = "Friends only",
          isChecked = filters.friendsOnly,
          onCheckedChange = { checked -> filterViewModel.setFriendsOnly(checked) },
          content = {})

      // Popular Only
      ToggleSection(
          title = "Popular only",
          isChecked = filters.popularOnly,
          onCheckedChange = { checked -> filterViewModel.setPopularOnly(checked) },
          content = {})
    }
  }

  /**
   * Reusable toggle section with checkbox, title, and expandable content.
   *
   * @param title Section title
   * @param isChecked Current checked state
   * @param onCheckedChange Callback when checkbox is toggled
   * @param content Composable content shown when expanded
   */
  @Composable
  fun ToggleSection(
      title: String,
      isChecked: Boolean,
      onCheckedChange: (Boolean) -> Unit,
      content: @Composable () -> Unit
  ) {

    // Pick an icon based on the section title
    val sectionIcon =
        when (title) {
          "Date" -> Icons.Outlined.CalendarMonth
          "Location" -> Icons.Default.Place
          "Price" -> Icons.Default.AttachMoney
          "Tags" -> Icons.Default.Tag
          "Friends only" -> Icons.Default.Group
          "Popular only" -> Icons.Default.Star
          else -> Icons.Default.Settings
        }

    Column(modifier = Modifier.fillMaxWidth()) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onCheckedChange(it) },
            colors =
                CheckboxDefaults.colors(
                    uncheckedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
            modifier =
                Modifier.testTag(
                    when (title) {
                      "Date" -> FiltersSectionTestTags.TOGGLE_DATE
                      "Location" -> FiltersSectionTestTags.TOGGLE_LOCATION
                      "Price" -> FiltersSectionTestTags.TOGGLE_PRICE
                      "Tags" -> FiltersSectionTestTags.TOGGLE_TAGS
                      "Friends only" -> FiltersSectionTestTags.TOGGLE_FRIENDS
                      "Popular only" -> FiltersSectionTestTags.TOGGLE_POPULAR
                      else -> "toggle_${title.lowercase()}"
                    }))

        // Section icon
        Icon(
            imageVector = sectionIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = if (isChecked) 1f else 0.4f),
            modifier = Modifier.padding(end = 8.dp).size(20.dp))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f))
      }
      if (isChecked) {
        content()
      }
    }
  }

  /**
   * Date range picker with two-step dialog flow. First selects start date, then end date (min =
   * start). Defaults to today if no start date set.
   */
  @Composable
  fun DateRangePicker(
      filterViewModel: FiltersSectionViewModel,
      startDate: LocalDate?,
      endDate: LocalDate?
  ) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    /**
     * Helper to show a DatePickerDialog.
     *
     * @param title Dialog title
     * @param minDate Minimum selectable date (in millis)
     * @param onPicked Callback when a date is chosen, receives formatted string "dd/MM/yyyy"
     */
    fun showDatePicker(title: String, minDate: Long, onPicked: (String) -> Unit) {
      val picker =
          DatePickerDialog(
              context,
              { _, y, m, d ->
                val picked = dateFormat.format(Calendar.getInstance().apply { set(y, m, d) }.time)
                onPicked(picked)
              },
              calendar.get(Calendar.YEAR),
              calendar.get(Calendar.MONTH),
              calendar.get(Calendar.DAY_OF_MONTH))

      val tv =
          TextView(context).apply {
            text = title
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
          }

      picker.setCustomTitle(tv)
      picker.datePicker.minDate = minDate
      picker.show()
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {

          // --- FROM DATE BUTTON ---
          Button(
              onClick = {
                showDatePicker("Select Start Date", calendar.timeInMillis) { start ->
                  filterViewModel.setStartDate(start)
                }
              },
              colors =
                  ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)),
              shape = RoundedCornerShape(12.dp),
              modifier =
                  Modifier.weight(1f)
                      .height(56.dp)
                      .testTag(FiltersSectionTestTags.DATE_PICKER_BUTTON)) {
                Text(
                    text = "From: ${startDate ?: "---"}",
                    modifier = Modifier.testTag(FiltersSectionTestTags.FROM_DATE_TEXT))
              }

          // Compute startDateMillis safely
          val startDateMillis =
              remember(startDate) {
                try {
                  startDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                      ?: calendar.timeInMillis
                } catch (_: Exception) {
                  calendar.timeInMillis
                }
              }

          // --- TO DATE BUTTON ---
          Button(
              onClick = {
                showDatePicker("Select End Date", startDateMillis) { end ->
                  filterViewModel.setEndDate(end)
                }
              },
              colors =
                  ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).height(56.dp)) {
                Text(
                    text = "To: ${endDate ?: "---"}",
                    modifier = Modifier.testTag(FiltersSectionTestTags.TO_DATE_TEXT))
              }
        }
  }

  /** Location picker with 2 modes: Search, User. Includes radius input (0–999 km). */
  @Composable
  fun AroundSpotPicker(
      filterViewModel: FiltersSectionViewModel,
      filters: Filters,
      locationViewModel: LocationViewModel,
      userProfile: UserProfile
  ) {
    var selectedOption by rememberSaveable { mutableStateOf(AroundOption.SEARCH) }
    var radiusInput by
        rememberSaveable(filters.radiusKm) { mutableStateOf(filters.radiusKm.toString()) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {

      // --- TOP SELECTOR (Segmented Control) ---
      Row(
          modifier =
              Modifier.fillMaxWidth()
                  .background(
                      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                      shape = MaterialTheme.shapes.medium)
                  .padding(4.dp),
          horizontalArrangement = Arrangement.SpaceEvenly) {
            AroundOption.entries.forEach { option ->
              val isSelected = selectedOption == option
              val label =
                  when (option) {
                    AroundOption.SEARCH -> "Search"
                    AroundOption.USER -> "My location"
                  }

              Box(
                  modifier =
                      Modifier.clip(MaterialTheme.shapes.medium)
                          .background(
                              if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                              else Color.Transparent)
                          .clickable { selectedOption = option }
                          .padding(horizontal = 16.dp, vertical = 10.dp)
                          .testTag(
                              when (option) {
                                AroundOption.SEARCH -> FiltersSectionTestTags.AROUND_SEARCH
                                AroundOption.USER -> FiltersSectionTestTags.AROUND_USER
                              }),
              ) {
                Text(
                    text = label,
                    color =
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.titleSmall)
              }
            }
          }

      Spacer(modifier = Modifier.height(16.dp))

      // --- CONTENT CARD ---
      Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.medium,
          tonalElevation = 2.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
              when (selectedOption) {
                AroundOption.SEARCH -> {
                  SearchPlacePicker(locationViewModel, filterViewModel)
                  Spacer(modifier = Modifier.height(20.dp))
                }
                AroundOption.USER -> UserLocationPicker(filterViewModel, userProfile)
              }

              // --- RADIUS PICKER ---
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Radius",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface)

                    Box(
                        modifier =
                            Modifier.width(60.dp)
                                .height(36.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    MaterialTheme.shapes.small)
                                .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center) {
                          BasicTextField(
                              value = radiusInput,
                              onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }.take(3)
                                radiusInput = filtered
                                filterViewModel.setRadius(filtered.toIntOrNull())
                              },
                              singleLine = true,
                              textStyle =
                                  LocalTextStyle.current.copy(
                                      fontSize = 14.sp,
                                      textAlign = TextAlign.Center,
                                      color = MaterialTheme.colorScheme.onSurface),
                              modifier =
                                  Modifier.fillMaxWidth()
                                      .testTag(FiltersSectionTestTags.RADIUS_INPUT))
                        }

                    Text(
                        "km",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                  }
            }
          }
    }
  }

  /**
   * Location search with autocomplete using LocationViewModel. Shows up to 5 results. Powered by
   * Nominatim/OpenStreetMap.
   */
  @Composable
  fun SearchPlacePicker(
      locationViewModel: LocationViewModel,
      filterViewModel: FiltersSectionViewModel
  ) {
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val results by locationViewModel.locations.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
      OutlinedTextField(
          value = query,
          onValueChange = {
            query = it
            locationViewModel.onQueryChanged(it)
            expanded = it.isNotBlank()
          },
          modifier = Modifier.fillMaxWidth().testTag(FiltersSectionTestTags.SEARCH_PLACE_INPUT),
          singleLine = true,
          label = { Text("Location") },
          placeholder = { Text("Enter an address…") },
          leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
          },
          shape = RoundedCornerShape(12.dp))
      AnimatedVisibility(visible = expanded && results.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
              Column(modifier = Modifier.padding(vertical = 4.dp)) {
                results.take(5).forEachIndexed { index, loc ->
                  Column {
                    Text(
                        text = loc.name,
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable {
                                  filterViewModel.setLocation(loc)
                                  query = loc.name
                                  expanded = false
                                }
                                .padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium)
                    if (index < results.take(5).lastIndex) {
                      HorizontalDivider()
                    }
                  }
                }
              }
            }
      }
    }
  }

  /** Placeholder for future map-based location picker. */
  @Composable
  fun MapPicker() {
    Button(onClick = { /* open map picker */}) { Text("Pick on map to implement") }
  }

  /**
   * Sets user location when selected. Uses dummy coordinates. Note: userProfile.location should be
   * Location type in future.
   */
  @Composable
  fun UserLocationPicker(filterViewModel: FiltersSectionViewModel, userProfile: UserProfile) {
    LaunchedEffect(userProfile.location) {
      // Need to change how user location is stored (string to Location)
      filterViewModel.setLocation(Location("user", 0.0, 0.0))
    }
  }

  /** Max price input (CHF). Accepts only digits, max 100. */
  @Composable
  fun PricePicker(filterViewModel: FiltersSectionViewModel, maxPrice: Int?) {
    var sliderValue by remember(maxPrice) { mutableFloatStateOf((maxPrice ?: 0).toFloat()) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
      Text(
          text = "Max Price",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))

      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()) {
            // SLIDER
            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                  sliderValue = newValue
                  filterViewModel.setMaxPriceCHF(newValue.toInt())
                },
                valueRange = 0f..100f,
                steps = 0, // makes the slider move in continuous-CHF increments
                modifier = Modifier.weight(1f).testTag(FiltersSectionTestTags.PRICE_INPUT))

            // VALUE LABEL
            Text(
                text = "${sliderValue.toInt()} CHF",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp))
          }
    }
  }

  /**
   * Tag selector with FlowRow layout. Supports up to 3 lines. Tags are hardcoded but can be
   * externalized later.
   */
  @OptIn(ExperimentalLayoutApi::class)
  @Composable
  fun TagsPicker(filterViewModel: FiltersSectionViewModel, selectedTags: Set<String>) {
    val allTags = Tags.allTags

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
          allTags.forEach { tag ->
            val isSelected = tag in selectedTags
            val backgroundColor by
                animateColorAsState(
                    targetValue =
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            val textColor by
                animateColorAsState(
                    targetValue =
                        if (isSelected) MaterialTheme.colorScheme.background
                        else MaterialTheme.colorScheme.primary)

            FilterChip(
                selected = isSelected,
                onClick = { filterViewModel.toggleTag(tag) },
                enabled = true,
                label = { Text(tag) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = backgroundColor,
                        selectedLabelColor = textColor,
                        containerColor = backgroundColor,
                        labelColor = textColor),
                border = null,
                shape = RoundedCornerShape(16.dp), // rounded/circular look
                modifier = Modifier.testTag(FiltersSectionTestTags.tag(tag)))
          }
        }
  }
}
