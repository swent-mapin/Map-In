package com.swent.mapin.ui.map

import android.app.DatePickerDialog
import android.view.Gravity
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swent.mapin.model.Location
import com.swent.mapin.model.LocationViewModel
import com.swent.mapin.model.UserProfile
import java.text.SimpleDateFormat
import java.util.*

// Assisted by AI tools

/**
 * Enum defining the three location selection modes in the "Place" filter.
 * - SEARCH: Text-based location search
 * - MAP: Manual selection on map (to be implemented)
 * - USER: Use current user geolocation
 */
enum class AroundOption {
  SEARCH,
  MAP,
  USER
}

/**
 * Centralized test tag registry for FiltersSection. All UI elements are tagged for reliable,
 * maintainable Compose UI testing.
 */
object FiltersSectionTestTags {
  const val RESET_BUTTON = "reset_button"

  // ToggleSection
  const val TOGGLE_TIME = "toggle_time"
  const val TOGGLE_PLACE = "toggle_place"
  const val TOGGLE_PRICE = "toggle_price"
  const val TOGGLE_TAGS = "toggle_tags"
  const val TOGGLE_FRIENDS = "toggle_friends_only"
  const val TOGGLE_POPULAR = "toggle_popular_only"

  const val EXPAND_TIME = "expand_time"
  const val EXPAND_PLACE = "expand_place"
  const val EXPAND_PRICE = "expand_price"
  const val EXPAND_TAGS = "expand_tags"

  // DateRangePicker
  const val DATE_PICKER_BUTTON = "date_picker_button"
  const val FROM_DATE_TEXT = "from_date_text"
  const val TO_DATE_TEXT = "to_date_text"

  // AroundSpotPicker
  const val AROUND_SEARCH = "around_search"
  const val AROUND_MAP = "around_map"
  const val AROUND_USER = "around_user"
  const val RADIUS_INPUT = "radius_input"

  // SearchPlacePicker
  const val SEARCH_PLACE_INPUT = "search_place_input"

  // PricePicker
  const val PRICE_INPUT = "price_input"

  // TagsPicker
  fun tag(tagName: String) = "tag_${tagName.lowercase()}"
}

/**
 * Main entry point for the filter panel UI. Renders a collapsible, interactive filter interface
 * with 6 sections.
 *
 * @param modifier Layout modifier
 * @param filterViewModel Manages all filter states (default: new instance via viewModel())
 * @param locationViewModel Provides location search results
 * @param userProfile Required for "My location" option
 */
class FiltersSection {
  @Composable
  fun Render(
      modifier: Modifier = Modifier,
      filterViewModel: FiltersSectionViewModel = viewModel(),
      locationViewModel: LocationViewModel,
      userProfile: UserProfile
  ) {
    Column(modifier = modifier.fillMaxWidth()) {
      // Header with title and reset button
      Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(
            text = "Filter Events by:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp))

        TextButton(
            onClick = { filterViewModel.reset() },
            modifier = Modifier.testTag(FiltersSectionTestTags.RESET_BUTTON)) {
              Text("Reset")
            }
      }

      // Time Filter
      ToggleSection(
          title = "Time",
          isChecked = filterViewModel.isWhenChecked.value,
          hasContent = true,
          onCheckedChange = { checked ->
            if (!checked) filterViewModel.resetWhen()
            else {
              if (filterViewModel.startDate.value.isBlank()) {
                val today = Calendar.getInstance().time
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                filterViewModel.setStartDate(sdf.format(today))
              }
              filterViewModel.setWhenChecked(true)
            }
          },
          content = { DateRangePicker(filterViewModel) })

      // Place Filter
      ToggleSection(
          title = "Place",
          isChecked = filterViewModel.isWhereChecked.value,
          hasContent = true,
          onCheckedChange = { checked ->
            if (!checked) filterViewModel.resetWhere() else filterViewModel.setWhereChecked(true)
          },
          content = { AroundSpotPicker(filterViewModel, locationViewModel, userProfile) })

      // Price Filter
      ToggleSection(
          title = "Price",
          isChecked = filterViewModel.isPriceChecked.value,
          hasContent = true,
          onCheckedChange = { checked ->
            if (!checked) filterViewModel.resetPrice() else filterViewModel.setPriceChecked(true)
          },
          content = { PricePicker(filterViewModel) })

      // Tags Filter
      ToggleSection(
          title = "Tags",
          isChecked = filterViewModel.tagsEnabled.value,
          hasContent = true,
          onCheckedChange = { checked ->
            if (!checked) filterViewModel.resetTags() else filterViewModel.setTagsEnabled(true)
          },
          content = { TagsPicker(filterViewModel) })

      // Friends Only
      ToggleSection(
          title = "Friends only",
          isChecked = filterViewModel.friendsOnly.value,
          hasContent = false,
          onCheckedChange = { checked -> filterViewModel.setFriendsOnly(checked) },
          content = {})

      // Popular Only
      ToggleSection(
          title = "Popular only",
          isChecked = filterViewModel.popularOnly.value,
          hasContent = false,
          onCheckedChange = { checked -> filterViewModel.setPopularOnly(checked) },
          content = {})
    }
  }

  /**
   * Reusable toggle section with checkbox, title, and expandable content.
   *
   * @param title Section title
   * @param isChecked Current checked state
   * @param hasContent Whether expandable content exists
   * @param onCheckedChange Callback when checkbox is toggled
   * @param content Composable content shown when expanded
   */
  @Composable
  fun ToggleSection(
      title: String,
      isChecked: Boolean,
      hasContent: Boolean,
      onCheckedChange: (Boolean) -> Unit,
      content: @Composable () -> Unit
  ) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = {
                  onCheckedChange(it)
                  if (!it) expanded = false
                },
                modifier =
                    Modifier.testTag(
                        when (title) {
                          "Time" -> FiltersSectionTestTags.TOGGLE_TIME
                          "Place" -> FiltersSectionTestTags.TOGGLE_PLACE
                          "Price" -> FiltersSectionTestTags.TOGGLE_PRICE
                          "Tags" -> FiltersSectionTestTags.TOGGLE_TAGS
                          "Friends only" -> FiltersSectionTestTags.TOGGLE_FRIENDS
                          "Popular only" -> FiltersSectionTestTags.TOGGLE_POPULAR
                          else -> "toggle_${title.lowercase()}"
                        }))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f))
            if (hasContent) {
              Icon(
                  imageVector =
                      if (expanded) Icons.Default.KeyboardArrowDown
                      else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                  contentDescription = null,
                  tint =
                      if (isChecked) MaterialTheme.colorScheme.onSurface
                      else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                  modifier =
                      Modifier.size(24.dp)
                          .testTag(
                              when (title) {
                                "Time" -> FiltersSectionTestTags.EXPAND_TIME
                                "Place" -> FiltersSectionTestTags.EXPAND_PLACE
                                "Price" -> FiltersSectionTestTags.EXPAND_PRICE
                                "Tags" -> FiltersSectionTestTags.EXPAND_TAGS
                                else -> "expand_${title.lowercase()}"
                              })
                          .let { base ->
                            if (isChecked) base.clickable { expanded = !expanded } else base
                          })
            }
          }

      if (isChecked && expanded) {
        content()
      }

      HorizontalDivider()
    }
  }

  /**
   * Date range picker with two-step dialog flow. First selects start date, then end date (min =
   * start). Defaults to today if no start date set.
   */
  @Composable
  fun DateRangePicker(filterViewModel: FiltersSectionViewModel) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    fun showDatePicker(
        selectedDate: MutableState<String>,
        title: String,
        minDate: Long,
        onPicked: (() -> Unit)? = null
    ) {
      val picker =
          DatePickerDialog(
              context,
              { _, y, m, d ->
                selectedDate.value =
                    dateFormat.format(Calendar.getInstance().apply { set(y, m, d) }.time)
                onPicked?.invoke()
              },
              calendar[Calendar.YEAR],
              calendar[Calendar.MONTH],
              calendar[Calendar.DAY_OF_MONTH])

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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 30.dp, bottom = 8.dp)) {
          IconButton(
              onClick = {
                showDatePicker(
                    filterViewModel.startDate, "Select Start Date", calendar.timeInMillis) {
                      val startMillis =
                          filterViewModel.startDate.value
                              .takeIf { it.isNotBlank() }
                              ?.let { dateFormat.parse(it)?.time } ?: calendar.timeInMillis
                      showDatePicker(filterViewModel.endDate, "Select End Date", startMillis)
                    }
              },
              modifier =
                  Modifier.size(36.dp)
                      .background(Color.LightGray, CircleShape)
                      .testTag(FiltersSectionTestTags.DATE_PICKER_BUTTON)) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Calendar")
              }

          Column {
            Text(
                "From: ${filterViewModel.startDate.value.ifBlank { "---" }}",
                modifier = Modifier.testTag(FiltersSectionTestTags.FROM_DATE_TEXT))
            Text(
                "To: ${filterViewModel.endDate.value.ifBlank { "---" }}",
                modifier = Modifier.testTag(FiltersSectionTestTags.TO_DATE_TEXT))
          }
        }
  }

  /** Location picker with 3 modes: Search, Map, User. Includes radius input (0–999 km). */
  @Composable
  fun AroundSpotPicker(
      filterViewModel: FiltersSectionViewModel,
      locationViewModel: LocationViewModel,
      userProfile: UserProfile
  ) {
    var selectedOption by rememberSaveable { mutableStateOf(AroundOption.SEARCH) }

    val optionLabels =
        mapOf(
            AroundOption.SEARCH to "Search a place",
            AroundOption.MAP to "Pick on map",
            AroundOption.USER to "My location")

    Column(modifier = Modifier.fillMaxWidth().padding(start = 30.dp, bottom = 8.dp)) {
      Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        AroundOption.entries.forEach { option ->
          Text(
              text = optionLabels[option]!!,
              modifier =
                  Modifier.clickable { selectedOption = option }
                      .background(
                          color =
                              if (selectedOption == option)
                                  MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                              else Color.Transparent,
                          shape = MaterialTheme.shapes.small)
                      .padding(8.dp)
                      .testTag(
                          when (option) {
                            AroundOption.SEARCH -> FiltersSectionTestTags.AROUND_SEARCH
                            AroundOption.MAP -> FiltersSectionTestTags.AROUND_MAP
                            AroundOption.USER -> FiltersSectionTestTags.AROUND_USER
                          }))
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      when (selectedOption) {
        AroundOption.SEARCH -> SearchPlacePicker(locationViewModel, filterViewModel)
        AroundOption.MAP -> MapPicker()
        AroundOption.USER -> UserLocationPicker(filterViewModel, userProfile)
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Radius:")
            var radiusInput by
                rememberSaveable(filterViewModel.radiusKm.value) {
                  mutableStateOf(filterViewModel.radiusKm.value?.toString() ?: "")
                }
            Box(
                modifier =
                    Modifier.width(40.dp)
                        .height(30.dp)
                        .border(
                            1.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center) {
                  BasicTextField(
                      value = radiusInput,
                      onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }.take(3)
                        radiusInput = filtered
                        if (filtered.isNotBlank()) {
                          filterViewModel.setRadius(filtered.toInt())
                        } else {
                          filterViewModel.setRadius(null)
                        }
                      },
                      singleLine = true,
                      textStyle =
                          LocalTextStyle.current.copy(
                              fontSize = 14.sp,
                              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                              color = MaterialTheme.colorScheme.onSurface),
                      modifier =
                          Modifier.fillMaxWidth().testTag(FiltersSectionTestTags.RADIUS_INPUT))
                }
            Text("km", modifier = Modifier.padding(start = 4.dp))
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
    val results by locationViewModel.locations.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
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
          placeholder = { Text("Enter an address") })

      if (expanded && results.isNotEmpty()) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
          Column {
            results.take(5).forEach { loc ->
              Text(
                  text = loc.name,
                  modifier =
                      Modifier.fillMaxWidth()
                          .clickable {
                            filterViewModel.setLocation(loc)
                            query = loc.name
                            expanded = false
                          }
                          .padding(8.dp))
              HorizontalDivider()
            }
            Text(
                "Search powered by Nominatim and OpenStreetMap.",
                fontSize = 12.sp,
                modifier = Modifier.padding(8.dp))
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

  /** Max price input (CHF). Accepts only digits, max 999. */
  @Composable
  fun PricePicker(filterViewModel: FiltersSectionViewModel) {
    var priceInput by
        remember(filterViewModel.maxPriceCHF.value) {
          mutableStateOf(filterViewModel.maxPriceCHF.value?.toString() ?: "")
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 30.dp, bottom = 8.dp)) {
          Box(
              modifier =
                  Modifier.width(50.dp)
                      .height(30.dp)
                      .border(
                          1.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.small),
              contentAlignment = Alignment.Center) {
                BasicTextField(
                    value = priceInput,
                    onValueChange = { input ->
                      val filtered = input.filter { it.isDigit() }.take(3)
                      priceInput = filtered
                      if (filtered.isNotBlank()) {
                        filterViewModel.setMaxPriceCHF(filtered.toInt())
                      } else {
                        filterViewModel.setMaxPriceCHF(null)
                      }
                    },
                    singleLine = true,
                    textStyle =
                        LocalTextStyle.current.copy(
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth().testTag(FiltersSectionTestTags.PRICE_INPUT))
              }
          Text("CHF", modifier = Modifier.padding(start = 4.dp))
        }
  }

  /**
   * Tag selector with FlowRow layout. Supports up to 3 lines. Tags are hardcoded but can be
   * externalized later.
   */
  @OptIn(ExperimentalLayoutApi::class)
  @Composable
  fun TagsPicker(filterViewModel: FiltersSectionViewModel) {
    val allTags =
        listOf(
            "Music",
            "Party",
            "Sport",
            "Food",
            "Nature",
            "Art",
            "Tech",
            "Dance",
            "Cinema",
            "Festival",
            "Workshop",
            "Club",
            "Volunteering",
            "Travel",
            "Fitness",
            "Board Games")

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 30.dp, bottom = 12.dp),
        maxLines = 3,
        overflow = FlowRowOverflow.Clip) {
          allTags.forEach { tag ->
            val selected = tag in filterViewModel.selectedTags

            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (selected) Color(0xFFBDBDBD) else Color(0xFFF2F2F2),
                modifier =
                    Modifier.clickable { filterViewModel.toggleTag(tag) }
                        .testTag(FiltersSectionTestTags.tag(tag))
                        .border(
                            width = if (selected) 0.dp else 1.dp,
                            color = Color.Gray,
                            shape = MaterialTheme.shapes.small)) {
                  Text(
                      text = tag,
                      fontSize = 13.sp,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
          }
        }
  }
}
