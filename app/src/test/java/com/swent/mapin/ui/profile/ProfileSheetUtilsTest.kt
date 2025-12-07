package com.swent.mapin.ui.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileSheetUtilsTest {

  @Test
  fun `getSheetAvatarIcon returns fallback for null or empty`() {
    assertEquals(Icons.Default.Person, getSheetAvatarIcon(null))
    assertEquals(Icons.Default.Person, getSheetAvatarIcon(""))
  }

  @Test
  fun `getSheetAvatarIcon maps known keywords`() {
    assertEquals(Icons.Default.Person, getSheetAvatarIcon("person"))
    assertEquals(Icons.Default.Face, getSheetAvatarIcon("face"))
    assertEquals(Icons.Default.Star, getSheetAvatarIcon("star"))
    assertEquals(Icons.Default.Favorite, getSheetAvatarIcon("favorite"))
  }

  @Test
  fun `getSheetAvatarIcon defaults to person for unknown values`() {
    assertEquals(Icons.Default.Person, getSheetAvatarIcon("unknown"))
  }
}
