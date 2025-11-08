package com.swent.mapin.ui.chat

import com.swent.mapin.model.FriendshipStatus
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlin.test.Test

class LocalChatFriendsRepositoryTest {

  @Test
  fun `getAllFriends should return correct friends`() {
    val friends = LocalChatFriendsRepository.getAllFriends()

    assertEquals(3, friends.size)
    assertEquals("Nathan", friends[0].userProfile.name)
    assertEquals("Alex", friends[1].userProfile.name)
    assertEquals("Zoe", friends[2].userProfile.name)

    assertTrue(friends.all { it.friendshipStatus == FriendshipStatus.ACCEPTED })
  }

  @Test
  fun `getSampleConversations should return correct sample data`() {
    val conversations = LocalChatFriendsRepository.getAllConversations()

    assertEquals(3, conversations.size)
    assertEquals("Nathan", conversations[0].name)
    assertEquals("Alex", conversations[1].name)
    assertEquals("Zoe", conversations[2].name)

    assertEquals("Hey there!", conversations[0].lastMessage)
    assertEquals("Shared a photo", conversations[1].lastMessage)
    assertEquals("Let's meet up!", conversations[2].lastMessage)

    assertEquals("Nathan", conversations[0].participants.first().userProfile.name)
    assertEquals("Alex", conversations[1].participants.first().userProfile.name)
    assertEquals("Zoe", conversations[2].participants.first().userProfile.name)
  }
}
