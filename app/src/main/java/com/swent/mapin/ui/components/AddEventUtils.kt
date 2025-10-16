package com.swent.mapin.ui.components

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event

/**
 * With help of GPT: Helper function which checks if the user's input string for tags are of valid
 * format. The regex means: “A string that starts with #, followed by letters/numbers/underscores,
 * and optionally continues with more tags separated by spaces or commas, where each tag also starts
 * with #.”
 * * Examples of valid inputs:
 * * - "#food"
 * * - "#food , #travel"
 * * - "#fun_2025,#study"
 *
 * @param input The string entered by the user
 * @return true if the input matches the valid tag format, false if not.
 */
fun isValidTagInput(input: String): Boolean {
  if (input.isBlank()) return true
  val tagRegex = Regex("^(#\\w+)(?:[ ,]+#\\w+)*$")
  return input.matches(tagRegex)
}

/**
 * With Help of GPT: Helper function that extracts individual tags from a valid input string.
 *
 * The input string is expected to follow the same format checked by [isValidTagInput], where each
 * tag starts with a '#' followed by letters, digits, or underscores, and multiple tags can be
 * separated by spaces or commas.
 * * Examples:
 * * - Input: "#food" → Output: ["#food"]
 * * - Input: "#food , #travel" → Output: ["#food", "#travel"]
 * * - Input: "#fun_2025,#study" → Output: ["#fun_2025", "#study"]
 *
 * @param input The user input string containing one or more valid tags.
 * @return A list of strings, each representing an extracted tag (including the '#' prefix).
 */
fun extractTags(input: String): List<String> {
  val tagRegex = Regex("#\\w+")
  return tagRegex.findAll(input).map { it.value }.toList()
}

/**
 * Checks if the input string is a valid location party of the List of locations from the valid
 * location list returned from Nominatim query
 *
 * @param input The string location entered by the user
 * @param locations The list of valid locations
 */
fun isValidLocation(input: String, locations: List<Location>): Boolean {
  return locations.any { it.name.equals(input, ignoreCase = true) }
}

/**
 * Creates a new [Event] object with the provided data and adds it to the given [EventViewModel].
 *
 * This function centralizes the logic of constructing an [Event] from user input and submitting it
 * to the view model. After successfully adding the event, it invokes the [onDone] callback to
 * signal completion.
 *
 * @param viewModel The [EventViewModel] responsible for managing and storing events.
 * @param title The title of the event.
 * @param description A textual description of the event.
 * @param location The [Location] object representing the event’s location.
 * @param tags A list of tags associated with the event.
 * @param isPublic Whether the event is public (`true`) or private (`false`).
 * @param onDone A callback invoked after the event has been added successfully.
 *
 * @see EventViewModel.addEvent
 * @see Event
 */
fun saveEvent(
  viewModel: EventViewModel,
  title: String,
  description: String,
  location: Location,
  tags: List<String>,
  isPublic: Boolean,
  onDone: () -> Unit
) {
  val newEvent = Event(
    uid = viewModel.getNewUid(),
    title = title,
    url = null,
    description = description,
    location = location,
    tags = tags,
    public = isPublic,
    ownerId = Firebase.auth.currentUser?.uid ?: "",
    imageUrl = null,
    capacity = null,
    attendeeCount = ATTENDEES_DEFAULT
  )
  viewModel.addEvent(newEvent)
  onDone()
}