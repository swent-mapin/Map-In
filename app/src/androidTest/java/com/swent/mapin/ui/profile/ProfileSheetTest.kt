package com.swent.mapin.ui.profile

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.badge.Badge
import com.swent.mapin.model.badge.BadgeRarity
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class ProfileSheetTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun profileSheet_showsLoadingState() {
    val mockViewModel = mockViewModelWithState(ProfileSheetState.Loading)

    setProfileContent(mockViewModel)

    composeTestRule.onNodeWithTag("profileSheetLoading").assertIsDisplayed()
  }

  @Test
  fun profileSheet_displaysBadgesForLoadedProfile() {
    val mockViewModel =
        mockViewModelWithState(
            buildLoadedState(
                avatarUrl = "http://example.com/avatar.png", badges = listOf(testBadge())))

    setProfileContent(mockViewModel)

    composeTestRule.onNodeWithTag("badgesSection").assertIsDisplayed()
    composeTestRule.onNodeWithTag("badgeCount").assertTextEquals("1/1")
  }

  @Test
  fun profileSheet_usesAsyncImageForHttpAvatar() {
    val mockViewModel =
        mockViewModelWithState(
            buildLoadedState(
                avatarUrl = "http://example.com/avatar.png", badges = listOf(testBadge())))

    setProfileContent(mockViewModel)

    composeTestRule.onNodeWithTag("profileAvatarImage").assertIsDisplayed()
    composeTestRule.onAllNodesWithTag("profileAvatarIcon").assertCountEquals(0)
  }

  @Test
  fun profileSheet_usesIconWhenAvatarIsKeyword() {
    val mockViewModel =
        mockViewModelWithState(
            buildLoadedState(avatarUrl = "face", badges = listOf(testBadge(isUnlocked = false))))

    setProfileContent(mockViewModel)

    composeTestRule.onNodeWithTag("profileAvatarIcon").assertIsDisplayed()
    composeTestRule.onAllNodesWithTag("profileAvatarImage").assertCountEquals(0)
  }

  @Test
  fun profileSheet_showsErrorState() {
    val mockViewModel = mockViewModelWithState(ProfileSheetState.Error("Not found"))

    setProfileContent(mockViewModel)

    composeTestRule.onNodeWithTag("profileSheetError").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profileSheetError").assertTextEquals("Not found")
  }

  @Test
  fun profileSheet_showsBioWhenPresent() {
    val mockViewModel =
        mockViewModelWithState(
            buildLoadedState(
                avatarUrl = "person", badges = listOf(testBadge()), bio = "Compose enthusiast"))

    setProfileContent(mockViewModel)

    composeTestRule.onNodeWithTag("profileBio").assertIsDisplayed()
  }

  @Test
  fun profileSheet_showsUnfollowWhenAlreadyFollowing() {
    val mockViewModel =
        mockViewModelWithState(
            buildLoadedState(
                avatarUrl = "person",
                badges = listOf(testBadge()),
                isFollowing = true,
                isOwnProfile = false))

    setProfileContent(mockViewModel)

    composeTestRule.onNodeWithTag("unfollowButton").assertIsDisplayed()
  }

  @Test
  fun profileSheet_showsAddFriendWhenNotFriend() {
    val vm =
        mockViewModelWithState(
            buildLoadedState(
                avatarUrl = "person",
                badges = listOf(testBadge()),
                friendStatus = FriendStatus.NOT_FRIEND,
                isOwnProfile = false))
    setProfileContent(vm)
    composeTestRule.onNodeWithTag("addFriendButton").assertIsDisplayed()
  }

  @Test
  fun profileSheet_showsPendingFriendState() {
    val vm =
        mockViewModelWithState(
            buildLoadedState(
                avatarUrl = "person",
                badges = listOf(testBadge()),
                friendStatus = FriendStatus.PENDING,
                isOwnProfile = false))
    setProfileContent(vm)
    composeTestRule.onNodeWithTag("pendingFriendButton").assertIsDisplayed()
  }

  @Test
  fun profileSheet_showsFriendsIndicator() {
    val vm =
        mockViewModelWithState(
            buildLoadedState(
                avatarUrl = "person",
                badges = listOf(testBadge()),
                friendStatus = FriendStatus.FRIENDS,
                isOwnProfile = false))
    setProfileContent(vm)
    composeTestRule.onNodeWithTag("friendsIndicator").assertIsDisplayed()
  }

  @Test
  fun profileSheet_showsFollowWhenNotFollowing() {
    val mockViewModel =
        mockViewModelWithState(
            buildLoadedState(
                avatarUrl = "person",
                badges = listOf(testBadge()),
                isFollowing = false,
                isOwnProfile = false))

    setProfileContent(mockViewModel)

    composeTestRule.onNodeWithTag("followButton").assertIsDisplayed()
    composeTestRule.onAllNodesWithTag("unfollowButton").assertCountEquals(0)
  }

  @Test
  fun profileSheet_displaysUpcomingAndPastEvents() {
    val upcoming = testEvent(uid = "up1", title = "Upcoming Owned Event")
    val past = testEvent(uid = "past1", title = "Past Owned Event")
    val mockViewModel =
        mockViewModelWithState(
            buildLoadedState(
                avatarUrl = "person",
                badges = listOf(testBadge()),
                upcoming = listOf(upcoming),
                past = listOf(past)))

    setProfileContent(mockViewModel)

    composeTestRule.onNodeWithTag("eventsRow_Upcoming Owned Events").assertExists()
    composeTestRule.onNodeWithTag("eventsRow_Past Owned Events").assertExists()
    composeTestRule.onNodeWithTag("eventCard_up1").assertExists()
    composeTestRule.onNodeWithTag("eventCard_past1").assertExists()
  }

  @Test
  fun profileSheet_showsEmptyEventsState() {
    val mockViewModel =
        mockViewModelWithState(
            buildLoadedState(
                avatarUrl = "person",
                badges = listOf(testBadge()),
                upcoming = emptyList(),
                past = emptyList()))

    setProfileContent(mockViewModel)

    composeTestRule.onNodeWithTag("noEventsCard").performScrollTo().assertIsDisplayed()
    composeTestRule.onAllNodesWithTag("eventsRow_Upcoming Owned Events").assertCountEquals(0)
    composeTestRule.onAllNodesWithTag("eventsRow_Past Owned Events").assertCountEquals(0)
  }

  @Test
  fun profileSheet_hidesBadgesWhenEmpty() {
    val mockViewModel =
        mockViewModelWithState(
            buildLoadedState(
                avatarUrl = "person",
                badges = emptyList(),
                upcoming = emptyList(),
                past = emptyList()))

    setProfileContent(mockViewModel)

    composeTestRule.onAllNodesWithTag("badgesSection").assertCountEquals(0)
  }

  @Test
  fun profileSheet_showsLocationWhenPresent() {
    val mockViewModel =
        mockViewModelWithState(
            buildLoadedState(
                avatarUrl = "person", badges = listOf(testBadge()), location = "New York"))

    setProfileContent(mockViewModel)

    composeTestRule.onNodeWithTag("profileLocation").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profileLocation").assertTextEquals("New York")
  }

  @Test
  fun profileSheet_hidesLocationWhenUnknown() {
    val mockViewModel =
        mockViewModelWithState(
            buildLoadedState(
                avatarUrl = "person", badges = listOf(testBadge()), location = "Unknown"))

    setProfileContent(mockViewModel)

    composeTestRule.onAllNodesWithTag("profileLocation").assertCountEquals(0)
  }

  @Test
  fun profileSheet_showsHobbiesWhenPresent() {
    val mockViewModel =
        mockViewModelWithState(
            buildLoadedState(
                avatarUrl = "person",
                badges = listOf(testBadge()),
                hobbies = listOf("Reading", "Gaming")))

    setProfileContent(mockViewModel)

    composeTestRule.onNodeWithTag("profileHobbies").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profileHobbies").assertTextEquals("Reading, Gaming")
  }

  @Test
  fun profileSheet_hidesHobbiesWhenEmpty() {
    val mockViewModel =
        mockViewModelWithState(
            buildLoadedState(
                avatarUrl = "person", badges = listOf(testBadge()), hobbies = emptyList()))

    setProfileContent(mockViewModel)

    composeTestRule.onAllNodesWithTag("profileHobbies").assertCountEquals(0)
  }

  @Test
  fun profileSheet_triggersEventClickCallback() {
    val upcoming = testEvent(uid = "click1", title = "Tap me")
    val mockViewModel =
        mockViewModelWithState(
            buildLoadedState(
                avatarUrl = "person",
                badges = listOf(testBadge()),
                upcoming = listOf(upcoming),
                past = emptyList()))
    var clicked = false

    setProfileContent(mockViewModel) { clicked = true }

    // First scroll to the events row (vertical scroll)
    composeTestRule.onNodeWithTag("eventsRow_Upcoming Owned Events").performScrollTo()
    // Then click the card (it's the first item, so it's already visible in the LazyRow)
    composeTestRule.onNodeWithTag("eventCard_click1").performClick()
    composeTestRule.waitForIdle()
    assert(clicked)
  }

  private fun mockViewModelWithState(state: ProfileSheetState): ProfileSheetViewModel {
    val mockViewModel = mockk<ProfileSheetViewModel>(relaxed = true)
    every { mockViewModel.state } returns state
    justRun { mockViewModel.loadProfile(any()) }
    return mockViewModel
  }

  private fun buildLoadedState(
      avatarUrl: String,
      badges: List<Badge>,
      bio: String = "",
      location: String = "Unknown",
      hobbies: List<String> = emptyList(),
      isFollowing: Boolean = false,
      isOwnProfile: Boolean = false,
      friendStatus: FriendStatus = FriendStatus.NOT_FRIEND,
      upcoming: List<com.swent.mapin.model.event.Event> = emptyList(),
      past: List<com.swent.mapin.model.event.Event> = emptyList()
  ): ProfileSheetState.Loaded {
    val profile =
        UserProfile(
            userId = "user123",
            name = "Test User",
            avatarUrl = avatarUrl,
            badges = badges,
            bio = bio,
            location = location,
            hobbies = hobbies,
            followerIds = listOf("follower1"))

    return ProfileSheetState.Loaded(
        profile = profile,
        upcomingEvents = upcoming,
        pastEvents = past,
        isFollowing = isFollowing,
        isOwnProfile = isOwnProfile,
        friendStatus = friendStatus)
  }

  private fun testBadge(isUnlocked: Boolean = true) =
      Badge(
          id = "badge1",
          title = "Test Badge",
          description = "desc",
          iconName = "star",
          rarity = BadgeRarity.RARE,
          isUnlocked = isUnlocked,
          progress = if (isUnlocked) 1f else 0.5f)

  private fun testEvent(uid: String, title: String) =
      com.swent.mapin.model.event.Event(uid = uid, title = title, tags = listOf("tag"))

  private fun setProfileContent(
      viewModel: ProfileSheetViewModel,
      onEventClick: (com.swent.mapin.model.event.Event) -> Unit = {}
  ) {
    composeTestRule.setContent {
      MaterialTheme {
        ProfileSheet(
            userId = "user123", onClose = {}, onEventClick = onEventClick, viewModel = viewModel)
      }
    }
  }
}
