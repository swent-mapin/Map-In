package com.swent.mapin.ui.event

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.compose.runtime.MutableState
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import com.swent.mapin.model.location.Location
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.tasks.await

private const val MAX_CAPACITY = 10_000

/**
 * Helper function that checks if the user's input string for tags is in a valid format.
 *
 * The input is considered valid if:
 * - It is empty or contains only whitespace (tags are optional).
 * - It consists of one or more tags, where each tag is a non-empty word made exclusively of letters
 *   (a-z, A-Z), numbers (0-9) and the 2 symbols '-' and '_'.
 * - Tags are separated by one or more spaces and/or commas (with optional surrounding whitespace).
 *
 * @param input The string entered by the user for the tags field
 * @return true if the input is empty or contains only valid letter-only tags, false otherwise
 */
fun isValidTagInput(input: String): Boolean {
  if (input.isBlank()) return true

  val tagRegex = Regex("""^\s*[a-zA-Z0-9_-]+(?:[\s,]+[a-zA-Z0-9_-]+)*\s*$""")

  return input.matches(tagRegex)
}

/**
 * With help of GPT Helper function to check whether a given string input is a valid price (double)
 *
 * @param input The string input
 */
fun isValidPriceInput(input: String): Boolean {
  if (input.isBlank()) return true
  val regex = """^\d+(\.\d+)?$""".toRegex()
  return regex.matches(input.trim())
}

/**
 * Helper that validates optional capacity input. Accepts blank, otherwise requires a positive
 * integer.
 */
fun isValidCapacityInput(input: String): Boolean {
  if (input.isBlank()) return true
  val trimmed = input.trim()
  val regex = Regex("^\\d+$")
  if (!regex.matches(trimmed)) return false
  val asNumber = trimmed.toLongOrNull() ?: return false
  return asNumber in 1..MAX_CAPACITY
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

/** Uploads the media file to Firebase Storage and returns the download URL. */
suspend fun uploadEventMedia(context: Context, uri: Uri, userId: String): Result<String> {
  return try {
    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
    val isVideo = mimeType.startsWith("video/")
    val folder = if (isVideo) "videos" else "images"
    val extension =
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            ?: if (isVideo) "mp4" else "jpg"
    val storageRef = FirebaseStorage.getInstance().reference
    val fileRef =
        storageRef.child(
            "events/$userId/$folder/${UUID.randomUUID()}_${System.currentTimeMillis()}.$extension")
    val metadata = storageMetadata { contentType = mimeType }
    fileRef.putFile(uri, metadata).await()
    Result.success(fileRef.downloadUrl.await().toString())
  } catch (e: Exception) {
    Log.e("EventMediaUpload", "Failed to upload media", e)
    Result.failure(e)
  }
}

sealed class ParseResult {
  data class Success(val start: Timestamp, val end: Timestamp) : ParseResult()

  data class Error(val message: String) : ParseResult()
}

/**
 * Parse and validate start/end date+time strings using pattern dd/MM/yyyyHHmm (24h). Returns
 * Success(startTs, endTs) if both parse and end > start, otherwise Error with reason.
 */
fun parseAndValidateStartEnd(
    startDateStr: String,
    startTimeStr: String,
    endDateStr: String,
    endTimeStr: String
): ParseResult {
  val sdf = SimpleDateFormat("dd/MM/yyyyHHmm", Locale.getDefault())
  sdf.timeZone = TimeZone.getDefault()

  val rawStart = startDateStr + startTimeStr
  val rawEnd = endDateStr + endTimeStr

  val parsedStart = runCatching { sdf.parse(rawStart) }.getOrNull()
  if (parsedStart == null) return ParseResult.Error("invalid start datetime")
  val parsedEnd = runCatching { sdf.parse(rawEnd) }.getOrNull()
  if (parsedEnd == null) return ParseResult.Error("invalid end datetime")

  val startTs = Timestamp(parsedStart)
  val endTs = Timestamp(parsedEnd)

  return if (!endTs.toDate().after(startTs.toDate())) {
    ParseResult.Error("end must be after start")
  } else {
    ParseResult.Success(startTs, endTs)
  }
}

/**
 * Extracted validation logic so it can be unit-tested.
 *
 * This function mirrors the inner `validateStartEnd()` logic originally inside `AddEventScreen` and
 * operates on the same MutableState types as the composable.
 */
fun validateStartEndLogic(
    date: MutableState<String>,
    time: MutableState<String>,
    endDate: MutableState<String>,
    endTime: MutableState<String>,
    dateError: MutableState<Boolean>,
    endDateError: MutableState<Boolean>,
    timeError: MutableState<Boolean>,
    endTimeError: MutableState<Boolean>
) {
  // Basic presence checks
  dateError.value = date.value.isBlank()
  timeError.value = time.value.isBlank()
  endDateError.value = endDate.value.isBlank()
  endTimeError.value = endTime.value.isBlank()

  // Only proceed if both date strings parse
  if (date.value.isBlank() || endDate.value.isBlank()) return

  val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
  val startDateOnly = runCatching { dateFmt.parse(date.value) }.getOrNull()
  val endDateOnly = runCatching { dateFmt.parse(endDate.value) }.getOrNull()
  if (startDateOnly == null) {
    dateError.value = true
    return
  }
  if (endDateOnly == null) {
    endDateError.value = true
    return
  }

  // If end date is after start date, we're good regardless of time
  if (endDateOnly.time > startDateOnly.time) {
    endDateError.value = false
    endTimeError.value = false
    return
  }

  // If end date is before start date -> mark endDate error
  if (endDateOnly.time < startDateOnly.time) {
    endDateError.value = true
    endTimeError.value = false
    return
  }

  // Dates equal -> need to validate times
  if (time.value.isBlank() || endTime.value.isBlank()) {
    // presence flags already set above
    return
  }

  val rawTime = if (time.value.contains("h")) time.value.replace("h", "") else time.value
  val rawEndTime =
      if (endTime.value.contains("h")) endTime.value.replace("h", "") else endTime.value

  // parse HHmm into minutes since midnight
  val startMinutes =
      runCatching { rawTime.substring(0, 2).toInt() * 60 + rawTime.substring(2, 4).toInt() }
          .getOrNull()
  val endMinutes =
      runCatching { rawEndTime.substring(0, 2).toInt() * 60 + rawEndTime.substring(2, 4).toInt() }
          .getOrNull()
  if (startMinutes == null) {
    timeError.value = true
    return
  }
  if (endMinutes == null) {
    endTimeError.value = true
    return
  }

  if (endMinutes <= startMinutes) {
    endDateError.value = true
    endTimeError.value = false
  } else {
    endDateError.value = false
    endTimeError.value = false
  }
}
