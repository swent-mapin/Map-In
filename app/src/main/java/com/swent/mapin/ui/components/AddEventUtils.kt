package com.swent.mapin.ui.components

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
  val tagRegex = Regex("^(#\\w+)(?:[ ,]+#\\w+)*$")
  return input.matches(tagRegex)
}

/**
 * With Help of GPT: Helper function that extracts individual tags from a valid input string.
 *
 * The input string is expected to follow the same format checked by [isValidTagInput], where each
 * tag starts with a '#' followed by letters, digits, or underscores, and multiple tags can be
 * separated by spaces or commas.
 *
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
