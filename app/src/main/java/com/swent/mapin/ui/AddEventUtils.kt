package com.swent.mapin.ui

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
