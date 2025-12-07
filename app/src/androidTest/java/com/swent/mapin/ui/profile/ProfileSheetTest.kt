package com.swent.mapin.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.event.Event
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileSheetTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val testProfile =
      UserProfile(
          userId = "user-123",
          name = "John Doe",
          bio = "Test bio here",
          followerIds = listOf("f1", "f2", "f3"))

  private val testEvent =
      Event(
          uid = "event-1",
          title = "Test Event",
          ownerId = "user-123",
          location = Location(name = "Paris", latitude = 48.8, longitude = 2.3),
          date = Timestamp.now())

  private fun setProfileSheetContent(
      profile: UserProfile = testProfile,
      upcomingEvents: List<Event> = emptyList(),
      pastEvents: List<Event> = emptyList(),
      isFollowing: Boolean = false,
      isOwnProfile: Boolean = false,
      onFollowToggle: () -> Unit = {},
      onEventClick: (Event) -> Unit = {}
  ) {
    composeTestRule.setContent {
      ProfileSheetContent(
          profile = profile,
          upcomingEvents = upcomingEvents,
          pastEvents = pastEvents,
          isFollowing = isFollowing,
          isOwnProfile = isOwnProfile,
          onFollowToggle = onFollowToggle,
          onEventClick = onEventClick)
    }
  }

  @Test
  fun profileHeader_displaysNameAndBio() {
    setProfileSheetContent()

    composeTestRule.onNodeWithTag("profileHeader").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profileName").assertTextEquals("John Doe")
    composeTestRule.onNodeWithTag("profileBio").assertIsDisplayed()
  }

  @Test
  fun followButton_displayedWhenNotOwnProfile() {
    setProfileSheetContent(isOwnProfile = false, isFollowing = false)
    composeTestRule.onNodeWithTag("followButton").assertIsDisplayed()
  }

  @Test
  fun unfollowButton_displayedWhenFollowing() {
    setProfileSheetContent(isOwnProfile = false, isFollowing = true)
    composeTestRule.onNodeWithTag("unfollowButton").assertIsDisplayed()
  }

  @Test
  fun followButton_triggersCallback() {
    var clicked = false
    setProfileSheetContent(isOwnProfile = false, onFollowToggle = { clicked = true })
    composeTestRule.onNodeWithTag("followButton").performClick()
    assertTrue(clicked)
  }

  @Test
  fun eventsSection_displayedWhenHasUpcomingEvents() {
    setProfileSheetContent(upcomingEvents = listOf(testEvent))
    composeTestRule.onNodeWithTag("eventsRow_Upcoming Events").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventCard_event-1").assertIsDisplayed()
  }
}
