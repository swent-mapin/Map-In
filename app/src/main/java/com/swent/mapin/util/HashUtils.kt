package com.swent.mapin.util

import java.security.MessageDigest

// Code writing assisted by AI tools

/** Utility object for hashing operations. */
object HashUtils {
  /**
   * Hashes a list of user IDs using SHA-256 algorithm.
   *
   * The function sorts the list before hashing to ensure consistent results regardless of input
   * order. This is useful for creating deterministic identifiers for groups of users.
   *
   * **Cryptographic properties:**
   * - **Collision resistant**: Different user lists produce different hashes. The length-prefix
   *   encoding prevents ambiguities like ["ab", "cd"] vs ["a", "bcd"]
   * - **Preimage resistant**: Given a hash, it's computationally infeasible to find the original
   *   user ID list (useful if hashes are exposed publicly for privacy)
   *
   * The implementation uses length-prefix encoding (format: "length:value|length:value|...") to
   * ensure that even user IDs containing special characters (|, :) cannot cause collisions.
   *
   * @param userIds List of user ID strings. Must contain at least 2 elements.
   * @return A hexadecimal string representation of the SHA-256 hash (64 characters).
   * @throws IllegalArgumentException if the list contains fewer than 2 elements.
   */
  fun hashUserIds(userIds: List<String>): String {
    require(userIds.size >= 2) { "User ID list must contain at least 2 elements, got ${userIds.size}" }

    // Sort the list to ensure consistent hashing regardless of input order
    val sortedIds = userIds.sorted()

    // Use length-prefix encoding to ensure collision resistance
    // Format: "length:value|length:value|..."
    // This prevents ambiguity even if IDs contain the delimiter
    val concatenated = sortedIds.joinToString(separator = "|") { "${it.length}:$it" }

    // Create SHA-256 hash
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(concatenated.toByteArray(Charsets.UTF_8))

    // Convert to hexadecimal string
    return hashBytes.joinToString("") { "%02x".format(it) }
  }
}
