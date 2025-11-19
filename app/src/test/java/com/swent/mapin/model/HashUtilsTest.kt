package com.swent.mapin.model

import com.swent.mapin.util.HashUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Test writing and documentation assisted by AI tools
/**
 * Tests for [com.swent.mapin.util.HashUtils].
 *
 * Validates:
 * - Hashing of user ID lists
 * - Consistent hashing regardless of input order
 * - Error handling for invalid inputs
 * - Hash uniqueness for different inputs
 */
class HashUtilsTest {

  @Test
  fun `hashUserIds returns consistent hash for same list`() {
    val userIds = listOf("user1", "user2", "user3")
    val hash1 = HashUtils.hashUserIds(userIds)
    val hash2 = HashUtils.hashUserIds(userIds)

    assertEquals(hash1, hash2)
  }

  @Test
  fun `hashUserIds returns same hash regardless of input order`() {
    val list1 = listOf("user1", "user2", "user3")
    val list2 = listOf("user3", "user1", "user2")
    val list3 = listOf("user2", "user3", "user1")

    val hash1 = HashUtils.hashUserIds(list1)
    val hash2 = HashUtils.hashUserIds(list2)
    val hash3 = HashUtils.hashUserIds(list3)

    assertEquals(hash1, hash2)
    assertEquals(hash2, hash3)
  }

  @Test
  fun `hashUserIds returns valid hexadecimal string of expected length`() {
    val userIds = listOf("user1", "user2")
    val hash = HashUtils.hashUserIds(userIds)

    // SHA-256 produces 64 hex characters (256 bits / 4 bits per hex char)
    assertEquals(64, hash.length)

    // Verify all characters are valid hexadecimal
    val hexPattern = "^[0-9a-f]+$".toRegex()
    assertTrue(hash.matches(hexPattern))
  }

  @Test
  fun `hashUserIds produces different hashes for different lists`() {
    val list1 = listOf("user1", "user2")
    val list2 = listOf("user1", "user3")
    val list3 = listOf("user2", "user3")

    val hash1 = HashUtils.hashUserIds(list1)
    val hash2 = HashUtils.hashUserIds(list2)
    val hash3 = HashUtils.hashUserIds(list3)

    assertNotEquals(hash1, hash2)
    assertNotEquals(hash2, hash3)
    assertNotEquals(hash1, hash3)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `hashUserIds throws exception for empty list`() {
    HashUtils.hashUserIds(emptyList())
  }

  @Test(expected = IllegalArgumentException::class)
  fun `hashUserIds throws exception for single element list`() {
    HashUtils.hashUserIds(listOf("user1"))
  }

  @Test
  fun `hashUserIds works with exactly two elements`() {
    val userIds = listOf("user1", "user2")
    val hash = HashUtils.hashUserIds(userIds)

    assertEquals(64, hash.length)
    assertTrue(hash.matches("^[0-9a-f]+$".toRegex()))
  }

  @Test
  fun `hashUserIds handles special characters in user IDs`() {
    val userIds = listOf("user@example.com", "user#123", "user_with_underscore")
    val hash1 = HashUtils.hashUserIds(userIds)

    // Verify it produces a valid hash
    assertEquals(64, hash1.length)

    // Verify consistency
    val hash2 = HashUtils.hashUserIds(userIds)
    assertEquals(hash1, hash2)
  }

  @Test
  fun `hashUserIds handles unicode characters in user IDs`() {
    val userIds = listOf("用户1", "utilisateur2", "użytkownik3")
    val hash = HashUtils.hashUserIds(userIds)

    assertEquals(64, hash.length)
    assertTrue(hash.matches("^[0-9a-f]+$".toRegex()))
  }

  @Test
  fun `hashUserIds produces different hashes for similar but different lists`() {
    // Test that concatenation with delimiter prevents collisions
    val list1 = listOf("ab", "cd")
    val list2 = listOf("a", "bcd")

    val hash1 = HashUtils.hashUserIds(list1)
    val hash2 = HashUtils.hashUserIds(list2)

    assertNotEquals(hash1, hash2)
  }

  @Test
  fun `hashUserIds is collision resistant with delimiter in IDs`() {
    // Test that IDs containing the delimiter character don't cause collisions
    val list1 = listOf("user|", "id")
    val list2 = listOf("user", "|id")

    val hash1 = HashUtils.hashUserIds(list1)
    val hash2 = HashUtils.hashUserIds(list2)

    // Thanks to length-prefix encoding, these should produce different hashes
    assertNotEquals(hash1, hash2)
  }

  @Test
  fun `hashUserIds is collision resistant with colon in IDs`() {
    // Test that IDs containing the colon character don't cause collisions
    val list1 = listOf("5:user", "id")
    val list2 = listOf("5", "user|2:id")

    val hash1 = HashUtils.hashUserIds(list1)
    val hash2 = HashUtils.hashUserIds(list2)

    // Length-prefix encoding makes these distinguishable
    assertNotEquals(hash1, hash2)
  }

  @Test
  fun `hashUserIds works with large list of user IDs`() {
    val userIds = (1..100).map { "user$it" }
    val hash1 = HashUtils.hashUserIds(userIds)
    val hash2 = HashUtils.hashUserIds(userIds.reversed())

    assertEquals(64, hash1.length)
    assertEquals(hash1, hash2) // Should be consistent regardless of order
  }

  @Test
  fun `hashUserIds is preimage resistant - hash cannot be reversed`() {
    // This test documents that the hash function is one-way (preimage resistant)
    // SHA-256 is cryptographically secure: given a hash, it's computationally
    // infeasible to find the original input

    val userIds = listOf("alice@example.com", "bob@example.com")
    val hash = HashUtils.hashUserIds(userIds)

    // We can compute the hash
    assertEquals(64, hash.length)

    // But we cannot reverse it to get back the original user IDs
    // This is a fundamental property of cryptographic hash functions
    // There is no inverse function: hashUserIds^-1 does not exist

    // The only way to "find" the original input is brute force,
    // which is computationally infeasible for SHA-256 (2^256 possibilities)

    // We can verify that the same input produces the same hash
    val hashAgain = HashUtils.hashUserIds(userIds)
    assertEquals(hash, hashAgain)

    // But given only the hash, we cannot determine the original userIds
    // This property makes it safe to use hashes as identifiers
  }

  @Test
  fun `hashUserIds handles empty strings in list`() {
    // Edge case: empty strings are valid user IDs
    val userIds = listOf("", "user1")
    val hash = HashUtils.hashUserIds(userIds)

    assertEquals(64, hash.length)
    assertTrue(hash.matches("^[0-9a-f]+$".toRegex()))

    // Verify it's different from a list without empty string
    val differentList = listOf("user1", "user2")
    val differentHash = HashUtils.hashUserIds(differentList)
    assertNotEquals(hash, differentHash)
  }

  @Test
  fun `hashUserIds handles whitespace-only strings`() {
    // Edge case: whitespace-only strings
    val userIds = listOf("   ", "user1")
    val hash = HashUtils.hashUserIds(userIds)

    assertEquals(64, hash.length)
    assertTrue(hash.matches("^[0-9a-f]+$".toRegex()))
  }

  @Test
  fun `hashUserIds handles very long user IDs`() {
    // Edge case: very long strings
    val longId = "a".repeat(10000)
    val userIds = listOf(longId, "user1")
    val hash = HashUtils.hashUserIds(userIds)

    assertEquals(64, hash.length)
    assertTrue(hash.matches("^[0-9a-f]+$".toRegex()))
  }

  @Test
  fun `hashUserIds handles identical IDs in list`() {
    // Edge case: duplicate IDs in the list
    val userIds = listOf("user1", "user1", "user2")
    val hash1 = HashUtils.hashUserIds(userIds)

    // Should produce valid hash
    assertEquals(64, hash1.length)

    // Should be different from list without duplicates
    val noDuplicates = listOf("user1", "user2")
    val hash2 = HashUtils.hashUserIds(noDuplicates)
    assertNotEquals(hash1, hash2)
  }

  @Test
  fun `hashUserIds handles newline characters in IDs`() {
    // Edge case: newline and other control characters
    val userIds = listOf("user\n1", "user\t2")
    val hash = HashUtils.hashUserIds(userIds)

    assertEquals(64, hash.length)
    assertTrue(hash.matches("^[0-9a-f]+$".toRegex()))
  }

  @Test
  fun `hashUserIds handles all lowercase hexadecimal output`() {
    // Verify the output is consistently lowercase hexadecimal
    val userIds = listOf("test1", "test2")
    val hash = HashUtils.hashUserIds(userIds)

    // Should not contain any uppercase letters
    assertEquals(hash, hash.lowercase())

    // Should be valid hex
    assertTrue(hash.matches("^[0-9a-f]+$".toRegex()))
  }

  @Test
  fun `hashUserIds handles numeric strings as IDs`() {
    // Edge case: user IDs that are purely numeric
    val userIds = listOf("12345", "67890")
    val hash = HashUtils.hashUserIds(userIds)

    assertEquals(64, hash.length)
    assertTrue(hash.matches("^[0-9a-f]+$".toRegex()))
  }

  @Test
  fun `hashUserIds is deterministic across multiple calls`() {
    // Verify determinism with multiple calls
    val userIds = listOf("user1", "user2", "user3")
    val hashes = (1..10).map { HashUtils.hashUserIds(userIds) }

    // All hashes should be identical
    hashes.forEach { hash -> assertEquals(hashes[0], hash) }
  }

  @Test
  fun `hashUserIds handles mixed case IDs as different`() {
    // Case sensitivity test
    val list1 = listOf("User1", "User2")
    val list2 = listOf("user1", "user2")

    val hash1 = HashUtils.hashUserIds(list1)
    val hash2 = HashUtils.hashUserIds(list2)

    // Case-sensitive: different cases should produce different hashes
    assertNotEquals(hash1, hash2)
  }

  @Test
  fun `hashUserIds error message includes actual size`() {
    // Verify the error message is helpful
    try {
      HashUtils.hashUserIds(listOf("single"))
      org.junit.Assert.fail("Should have thrown IllegalArgumentException")
    } catch (e: IllegalArgumentException) {
      assertTrue(e.message!!.contains("got 1"))
      assertTrue(e.message!!.contains("at least 2"))
    }
  }

  @Test
  fun `hashUserIds handles IDs with length prefix format characters`() {
    // Edge case: IDs that look like our internal format
    val list1 = listOf("5:user1", "6:user2")
    val list2 = listOf("5", "user1", "6", "user2")

    val hash1 = HashUtils.hashUserIds(list1)
    val hash2 = HashUtils.hashUserIds(list2)

    // Should be different because length-prefix encoding disambiguates
    assertNotEquals(hash1, hash2)
  }
}
