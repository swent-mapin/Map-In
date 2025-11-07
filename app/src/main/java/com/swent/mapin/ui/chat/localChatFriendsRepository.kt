package com.swent.mapin.ui.chat

import com.swent.mapin.model.FriendWithProfile
import com.swent.mapin.model.FriendshipStatus
import com.swent.mapin.model.UserProfile

object LocalChatFriendsRepository {

  private val friend1 =
      FriendWithProfile(
          UserProfile(name = "Nathan", bio = "Chill guy", hobbies = listOf("Surf")),
          friendshipStatus = FriendshipStatus.ACCEPTED,
          "")

  private val friend2 =
      FriendWithProfile(
          UserProfile(name = "Alex", bio = "Photographer", hobbies = listOf("Coffee")),
          friendshipStatus = FriendshipStatus.ACCEPTED,
          "")

  private val friend3 =
      FriendWithProfile(
          UserProfile(name = "Zoe", bio = "Runner", hobbies = listOf("Music")),
          friendshipStatus = FriendshipStatus.ACCEPTED,
          "")
  val sampleConversations =
      listOf(
          Conversation("c1", "Nathan", listOf(friend1), "Hey there!", true),
          Conversation("c2", "Alex", listOf(friend2), "Shared a photo", false),
          Conversation("c3", "Zoe", listOf(friend3), "Let's meet up!", true))

  val friendList = listOf(friend1, friend2, friend3)

  fun getAllFriends(): List<FriendWithProfile> = friendList

  fun getAllConversations(): List<Conversation> = sampleConversations
}
