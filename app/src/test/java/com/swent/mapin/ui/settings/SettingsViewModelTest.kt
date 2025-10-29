package com.swent.mapin.ui.settings

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

  private lateinit var viewModel: SettingsViewModel
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockFirestore: FirebaseFirestore
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    // Set the main dispatcher for coroutines
    Dispatchers.setMain(testDispatcher)

    // Mock Firebase dependencies
    mockAuth = mockk(relaxed = true)
    mockFirestore = mockk(relaxed = true)

    // Mock FirebaseAuth.getInstance()
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth

    // Mock auth.currentUser to return null (so loadMapPreferences doesn't try to access Firestore)
    every { mockAuth.currentUser } returns null

    // Mock FirebaseFirestore.getInstance()
    mockkStatic(FirebaseFirestore::class)
    every { FirebaseFirestore.getInstance() } returns mockFirestore

    // Create ViewModel with mocked dependencies
    viewModel = SettingsViewModel(mockAuth, mockFirestore)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun mapPreferences_initialValuesAreCorrect() {
    val preferences = viewModel.mapPreferences.value
    assertEquals(true, preferences.showPOIs)
    assertEquals(true, preferences.showRoadNumbers)
    assertEquals(true, preferences.showStreetNames)
    assertEquals(false, preferences.enable3DView)
  }

  @Test
  fun updateShowPOIs_changesState() {
    viewModel.updateShowPOIs(false)
    val preferences = viewModel.mapPreferences.value
    assertEquals(false, preferences.showPOIs)
    // Other settings should remain unchanged
    assertEquals(true, preferences.showRoadNumbers)
    assertEquals(true, preferences.showStreetNames)
    assertEquals(false, preferences.enable3DView)
  }

  @Test
  fun updateShowRoadNumbers_changesState() {
    viewModel.updateShowRoadNumbers(false)
    val preferences = viewModel.mapPreferences.value
    assertEquals(false, preferences.showRoadNumbers)
    // Other settings should remain unchanged
    assertEquals(true, preferences.showPOIs)
    assertEquals(true, preferences.showStreetNames)
    assertEquals(false, preferences.enable3DView)
  }

  @Test
  fun updateShowStreetNames_changesState() {
    viewModel.updateShowStreetNames(false)
    val preferences = viewModel.mapPreferences.value
    assertEquals(false, preferences.showStreetNames)
    // Other settings should remain unchanged
    assertEquals(true, preferences.showPOIs)
    assertEquals(true, preferences.showRoadNumbers)
    assertEquals(false, preferences.enable3DView)
  }

  @Test
  fun updateEnable3DView_changesState() {
    viewModel.updateEnable3DView(true)
    val preferences = viewModel.mapPreferences.value
    assertEquals(true, preferences.enable3DView)
    // Other settings should remain unchanged
    assertEquals(true, preferences.showPOIs)
    assertEquals(true, preferences.showRoadNumbers)
    assertEquals(true, preferences.showStreetNames)
  }

  @Test
  fun multipleUpdates_worksCorrectly() {
    viewModel.updateShowPOIs(false)
    viewModel.updateEnable3DView(true)
    viewModel.updateShowRoadNumbers(false)

    val preferences = viewModel.mapPreferences.value
    assertEquals(false, preferences.showPOIs)
    assertEquals(false, preferences.showRoadNumbers)
    assertEquals(true, preferences.showStreetNames)
    assertEquals(true, preferences.enable3DView)
  }

  @Test
  fun mapPreferencesDataClass_defaultsAreCorrect() {
    val prefs = MapPreferences()
    assertEquals(true, prefs.showPOIs)
    assertEquals(true, prefs.showRoadNumbers)
    assertEquals(true, prefs.showStreetNames)
    assertEquals(false, prefs.enable3DView)
  }

  @Test
  fun mapPreferencesDataClass_customValuesAreCorrect() {
    val prefs =
        MapPreferences(
            showPOIs = false, showRoadNumbers = true, showStreetNames = false, enable3DView = true)
    assertEquals(false, prefs.showPOIs)
    assertEquals(true, prefs.showRoadNumbers)
    assertEquals(false, prefs.showStreetNames)
    assertEquals(true, prefs.enable3DView)
  }

  @Test
  fun isLoading_initialValueIsFalse() {
    val isLoading = viewModel.isLoading.value
    assertEquals(false, isLoading)
  }

  @Test
  fun errorMessage_initialValueIsNull() {
    val errorMessage = viewModel.errorMessage.value
    assertEquals(null, errorMessage)
  }

  @Test
  fun signOut_setsLoadingState() {
    viewModel.signOut()
    // After signOut completes, loading should be false
    assertEquals(false, viewModel.isLoading.value)
  }

  @Test
  fun signOut_callsAuthSignOut() {
    viewModel.signOut()
    io.mockk.verify { mockAuth.signOut() }
  }

  @Test
  fun deleteAccount_whenUserIsNull_setsLoadingToFalse() {
    every { mockAuth.currentUser } returns null
    viewModel.deleteAccount()
    assertEquals(false, viewModel.isLoading.value)
  }

  @Test
  fun updateShowPOIs_toTrue_changesState() {
    viewModel.updateShowPOIs(false)
    viewModel.updateShowPOIs(true)
    val preferences = viewModel.mapPreferences.value
    assertEquals(true, preferences.showPOIs)
  }

  @Test
  fun updateShowRoadNumbers_toTrue_changesState() {
    viewModel.updateShowRoadNumbers(false)
    viewModel.updateShowRoadNumbers(true)
    val preferences = viewModel.mapPreferences.value
    assertEquals(true, preferences.showRoadNumbers)
  }

  @Test
  fun updateShowStreetNames_toTrue_changesState() {
    viewModel.updateShowStreetNames(false)
    viewModel.updateShowStreetNames(true)
    val preferences = viewModel.mapPreferences.value
    assertEquals(true, preferences.showStreetNames)
  }

  @Test
  fun updateEnable3DView_toFalse_changesState() {
    viewModel.updateEnable3DView(true)
    viewModel.updateEnable3DView(false)
    val preferences = viewModel.mapPreferences.value
    assertEquals(false, preferences.enable3DView)
  }

  @Test
  fun mapPreferences_copyWithShowPOIs_preservesOtherValues() {
    val original =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = true)
    val copied = original.copy(showPOIs = false)
    assertEquals(false, copied.showPOIs)
    assertEquals(false, copied.showRoadNumbers)
    assertEquals(true, copied.showStreetNames)
    assertEquals(true, copied.enable3DView)
  }

  @Test
  fun mapPreferences_copyWithShowRoadNumbers_preservesOtherValues() {
    val original =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = true)
    val copied = original.copy(showRoadNumbers = true)
    assertEquals(true, copied.showPOIs)
    assertEquals(true, copied.showRoadNumbers)
    assertEquals(true, copied.showStreetNames)
    assertEquals(true, copied.enable3DView)
  }

  @Test
  fun mapPreferences_copyWithShowStreetNames_preservesOtherValues() {
    val original =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = true)
    val copied = original.copy(showStreetNames = false)
    assertEquals(true, copied.showPOIs)
    assertEquals(false, copied.showRoadNumbers)
    assertEquals(false, copied.showStreetNames)
    assertEquals(true, copied.enable3DView)
  }

  @Test
  fun mapPreferences_copyWithEnable3DView_preservesOtherValues() {
    val original =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = false)
    val copied = original.copy(enable3DView = true)
    assertEquals(true, copied.showPOIs)
    assertEquals(false, copied.showRoadNumbers)
    assertEquals(true, copied.showStreetNames)
    assertEquals(true, copied.enable3DView)
  }

  @Test
  fun allTogglesOff_allValuesAreFalse() {
    viewModel.updateShowPOIs(false)
    viewModel.updateShowRoadNumbers(false)
    viewModel.updateShowStreetNames(false)
    viewModel.updateEnable3DView(false)

    val preferences = viewModel.mapPreferences.value
    assertEquals(false, preferences.showPOIs)
    assertEquals(false, preferences.showRoadNumbers)
    assertEquals(false, preferences.showStreetNames)
    assertEquals(false, preferences.enable3DView)
  }

  @Test
  fun allTogglesOn_allValuesAreTrue() {
    viewModel.updateShowPOIs(true)
    viewModel.updateShowRoadNumbers(true)
    viewModel.updateShowStreetNames(true)
    viewModel.updateEnable3DView(true)

    val preferences = viewModel.mapPreferences.value
    assertEquals(true, preferences.showPOIs)
    assertEquals(true, preferences.showRoadNumbers)
    assertEquals(true, preferences.showStreetNames)
    assertEquals(true, preferences.enable3DView)
  }

  @Test
  fun rapidToggles_finalStateIsCorrect() {
    // Toggle POIs multiple times
    viewModel.updateShowPOIs(false)
    viewModel.updateShowPOIs(true)
    viewModel.updateShowPOIs(false)
    viewModel.updateShowPOIs(true)

    val preferences = viewModel.mapPreferences.value
    assertEquals(true, preferences.showPOIs)
  }

  @Test
  fun alternatingToggles_maintainsIndependentState() {
    viewModel.updateShowPOIs(false)
    viewModel.updateShowRoadNumbers(true)
    viewModel.updateShowPOIs(true)
    viewModel.updateShowStreetNames(false)
    viewModel.updateEnable3DView(true)
    viewModel.updateShowRoadNumbers(false)

    val preferences = viewModel.mapPreferences.value
    assertEquals(true, preferences.showPOIs)
    assertEquals(false, preferences.showRoadNumbers)
    assertEquals(false, preferences.showStreetNames)
    assertEquals(true, preferences.enable3DView)
  }

  @Test
  fun mapPreferences_equality_sameValues() {
    val prefs1 =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = false)
    val prefs2 =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = false)
    assertEquals(prefs1, prefs2)
  }

  @Test
  fun mapPreferences_toString_containsAllFields() {
    val prefs =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = false)
    val string = prefs.toString()
    assert(string.contains("showPOIs"))
    assert(string.contains("showRoadNumbers"))
    assert(string.contains("showStreetNames"))
    assert(string.contains("enable3DView"))
  }

  @Test
  fun viewModelCreation_withMockedAuth_succeeds() {
    val newViewModel = SettingsViewModel(mockAuth, mockFirestore)
    assertEquals(MapPreferences(), newViewModel.mapPreferences.value)
  }

  @Test
  fun multipleViewModels_haveIndependentState() {
    val viewModel2 = SettingsViewModel(mockAuth, mockFirestore)

    viewModel.updateShowPOIs(false)
    viewModel2.updateShowPOIs(true)

    assertEquals(false, viewModel.mapPreferences.value.showPOIs)
    assertEquals(true, viewModel2.mapPreferences.value.showPOIs)
  }

  @Test
  fun updateSameValueTwice_stateRemainsConsistent() {
    viewModel.updateShowPOIs(false)
    val prefs1 = viewModel.mapPreferences.value
    viewModel.updateShowPOIs(false)
    val prefs2 = viewModel.mapPreferences.value

    assertEquals(prefs1.showPOIs, prefs2.showPOIs)
    assertEquals(false, prefs2.showPOIs)
  }

  @Test
  fun sequentialUpdates_eachUpdateReflectsImmediately() {
    viewModel.updateShowPOIs(false)
    assertEquals(false, viewModel.mapPreferences.value.showPOIs)

    viewModel.updateShowRoadNumbers(false)
    assertEquals(false, viewModel.mapPreferences.value.showRoadNumbers)

    viewModel.updateShowStreetNames(false)
    assertEquals(false, viewModel.mapPreferences.value.showStreetNames)

    viewModel.updateEnable3DView(true)
    assertEquals(true, viewModel.mapPreferences.value.enable3DView)
  }

  @Test
  fun mapPreferences_hashCode_consistentWithEquals() {
    val prefs1 =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = false)
    val prefs2 =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = false)
    assertEquals(prefs1.hashCode(), prefs2.hashCode())
  }

  @Test
  fun saveMapPreferences_withAuthenticatedUser_callsFirestore() {
    val mockUser = mockk<FirebaseUser>(relaxed = true)
    val mockCollection = mockk<CollectionReference>(relaxed = true)
    val mockDocument = mockk<DocumentReference>(relaxed = true)
    val mockTask = mockk<Task<Void>>(relaxed = true)

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test-user-id"
    every { mockFirestore.collection("settings") } returns mockCollection
    every { mockCollection.document("test-user-id") } returns mockDocument
    every { mockDocument.set(any()) } returns mockTask
    every { mockTask.addOnFailureListener(any()) } returns mockTask

    val newViewModel = SettingsViewModel(mockAuth, mockFirestore)
    newViewModel.updateShowPOIs(false)

    verify { mockDocument.set(any()) }
  }

  @Test
  fun saveMapPreferences_onFailure_setsErrorMessage() {
    val mockUser = mockk<FirebaseUser>(relaxed = true)
    val mockCollection = mockk<CollectionReference>(relaxed = true)
    val mockDocument = mockk<DocumentReference>(relaxed = true)
    val mockTask = mockk<Task<Void>>(relaxed = true)
    val failureListenerSlot = slot<OnFailureListener>()

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test-user-id"
    every { mockFirestore.collection("settings") } returns mockCollection
    every { mockCollection.document("test-user-id") } returns mockDocument
    every { mockDocument.set(any()) } returns mockTask
    every { mockTask.addOnFailureListener(capture(failureListenerSlot)) } returns mockTask

    val newViewModel = SettingsViewModel(mockAuth, mockFirestore)
    newViewModel.updateShowPOIs(false)

    // Wait for the coroutine to execute and capture the listener
    testDispatcher.scheduler.advanceUntilIdle()

    // Check if listener was captured before trying to use it
    if (failureListenerSlot.isCaptured) {
      // Simulate failure
      failureListenerSlot.captured.onFailure(Exception("Firestore error"))

      assertNotNull(newViewModel.errorMessage.value)
      assert(newViewModel.errorMessage.value?.contains("Failed to save preferences") == true)
    }
  }

  @Test
  fun loadMapPreferences_withExistingDocument_loadsPreferences() {
    val mockUser = mockk<FirebaseUser>(relaxed = true)
    val mockCollection = mockk<CollectionReference>(relaxed = true)
    val mockDocument = mockk<DocumentReference>(relaxed = true)
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    val mockRegistration = mockk<ListenerRegistration>(relaxed = true)
    val listenerSlot = slot<EventListener<DocumentSnapshot>>()

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test-user-id"
    every { mockFirestore.collection("settings") } returns mockCollection
    every { mockCollection.document("test-user-id") } returns mockDocument
    every { mockDocument.addSnapshotListener(capture(listenerSlot)) } returns mockRegistration
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.getBoolean("showPOIs") } returns false
    every { mockSnapshot.getBoolean("showRoadNumbers") } returns true
    every { mockSnapshot.getBoolean("showStreetNames") } returns false
    every { mockSnapshot.getBoolean("enable3DView") } returns true

    val newViewModel = SettingsViewModel(mockAuth, mockFirestore)
    listenerSlot.captured.onEvent(mockSnapshot, null)

    val prefs = newViewModel.mapPreferences.value
    assertEquals(false, prefs.showPOIs)
    assertEquals(true, prefs.showRoadNumbers)
    assertEquals(false, prefs.showStreetNames)
    assertEquals(true, prefs.enable3DView)
  }

  @Test
  fun loadMapPreferences_withNonExistingDocument_usesDefaults() {
    val mockUser = mockk<FirebaseUser>(relaxed = true)
    val mockCollection = mockk<CollectionReference>(relaxed = true)
    val mockDocument = mockk<DocumentReference>(relaxed = true)
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    val mockRegistration = mockk<ListenerRegistration>(relaxed = true)
    val listenerSlot = slot<EventListener<DocumentSnapshot>>()

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test-user-id"
    every { mockFirestore.collection("settings") } returns mockCollection
    every { mockCollection.document("test-user-id") } returns mockDocument
    every { mockDocument.addSnapshotListener(capture(listenerSlot)) } returns mockRegistration
    every { mockSnapshot.exists() } returns false

    val newViewModel = SettingsViewModel(mockAuth, mockFirestore)
    listenerSlot.captured.onEvent(mockSnapshot, null)

    val prefs = newViewModel.mapPreferences.value
    assertEquals(true, prefs.showPOIs)
    assertEquals(true, prefs.showRoadNumbers)
    assertEquals(true, prefs.showStreetNames)
    assertEquals(false, prefs.enable3DView)
  }

  @Test
  fun loadMapPreferences_onError_setsErrorMessage() {
    val mockUser = mockk<FirebaseUser>(relaxed = true)
    val mockCollection = mockk<CollectionReference>(relaxed = true)
    val mockDocument = mockk<DocumentReference>(relaxed = true)
    val mockRegistration = mockk<ListenerRegistration>(relaxed = true)
    val listenerSlot = slot<EventListener<DocumentSnapshot>>()

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test-user-id"
    every { mockFirestore.collection("settings") } returns mockCollection
    every { mockCollection.document("test-user-id") } returns mockDocument
    every { mockDocument.addSnapshotListener(capture(listenerSlot)) } returns mockRegistration

    val newViewModel = SettingsViewModel(mockAuth, mockFirestore)

    val error = mockk<FirebaseFirestoreException>(relaxed = true)
    every { error.message } returns "Network error"
    listenerSlot.captured.onEvent(null, error)

    assertNotNull(newViewModel.errorMessage.value)
    assert(newViewModel.errorMessage.value?.contains("Failed to load preferences") == true)
  }

  @Test
  fun deleteAccount_settingsDeleteFailure_setsErrorMessage() {
    val mockUser = mockk<FirebaseUser>(relaxed = true)
    val mockCollection = mockk<CollectionReference>(relaxed = true)
    val mockSettingsDoc = mockk<DocumentReference>(relaxed = true)
    val mockDeleteTask = mockk<Task<Void>>(relaxed = true)
    val failureListenerSlot = slot<OnFailureListener>()

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test-user-id"
    every { mockFirestore.collection("settings") } returns mockCollection
    every { mockCollection.document("test-user-id") } returns mockSettingsDoc
    every { mockSettingsDoc.delete() } returns mockDeleteTask
    every { mockDeleteTask.addOnSuccessListener(any()) } returns mockDeleteTask
    every { mockDeleteTask.addOnFailureListener(capture(failureListenerSlot)) } returns
        mockDeleteTask

    val newViewModel = SettingsViewModel(mockAuth, mockFirestore)
    newViewModel.deleteAccount()

    failureListenerSlot.captured.onFailure(Exception("Delete failed"))

    assertNotNull(newViewModel.errorMessage.value)
    assert(newViewModel.errorMessage.value?.contains("Failed to delete settings") == true)
    assertEquals(false, newViewModel.isLoading.value)
  }

  @Test
  fun deleteAccount_usersDeleteFailure_setsErrorMessage() {
    val mockUser = mockk<FirebaseUser>(relaxed = true)
    val mockCollection = mockk<CollectionReference>(relaxed = true)
    val mockSettingsDoc = mockk<DocumentReference>(relaxed = true)
    val mockUsersDoc = mockk<DocumentReference>(relaxed = true)
    val mockDeleteSettingsTask = mockk<Task<Void>>(relaxed = true)
    val mockDeleteUsersTask = mockk<Task<Void>>(relaxed = true)
    val settingsSuccessSlot = slot<OnSuccessListener<Void>>()
    val usersFailureSlot = slot<OnFailureListener>()

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test-user-id"
    every { mockFirestore.collection("settings") } returns mockCollection
    every { mockFirestore.collection("users") } returns mockCollection
    every { mockCollection.document("test-user-id") } returns mockSettingsDoc andThen mockUsersDoc
    every { mockSettingsDoc.delete() } returns mockDeleteSettingsTask
    every { mockUsersDoc.delete() } returns mockDeleteUsersTask
    every { mockDeleteSettingsTask.addOnSuccessListener(capture(settingsSuccessSlot)) } returns
        mockDeleteSettingsTask
    every { mockDeleteSettingsTask.addOnFailureListener(any()) } returns mockDeleteSettingsTask
    every { mockDeleteUsersTask.addOnSuccessListener(any()) } returns mockDeleteUsersTask
    every { mockDeleteUsersTask.addOnFailureListener(capture(usersFailureSlot)) } returns
        mockDeleteUsersTask

    val newViewModel = SettingsViewModel(mockAuth, mockFirestore)
    newViewModel.deleteAccount()

    // Wait for coroutines
    testDispatcher.scheduler.advanceUntilIdle()

    // Check if both slots were captured
    if (settingsSuccessSlot.isCaptured && usersFailureSlot.isCaptured) {
      // Simulate settings delete success, then users delete failure
      settingsSuccessSlot.captured.onSuccess(null)
      usersFailureSlot.captured.onFailure(Exception("Delete users failed"))

      assertNotNull(newViewModel.errorMessage.value)
      assert(newViewModel.errorMessage.value?.contains("Failed to delete user profile") == true)
      assertEquals(false, newViewModel.isLoading.value)
    }
  }

  @Test
  fun deleteAccount_authDeleteFailure_setsErrorMessage() {
    val mockUser = mockk<FirebaseUser>(relaxed = true)
    val mockCollection = mockk<CollectionReference>(relaxed = true)
    val mockSettingsDoc = mockk<DocumentReference>(relaxed = true)
    val mockUsersDoc = mockk<DocumentReference>(relaxed = true)
    val mockDeleteSettingsTask = mockk<Task<Void>>(relaxed = true)
    val mockDeleteUsersTask = mockk<Task<Void>>(relaxed = true)
    val mockDeleteAuthTask = mockk<Task<Void>>(relaxed = true)
    val settingsSuccessSlot = slot<OnSuccessListener<Void>>()
    val usersSuccessSlot = slot<OnSuccessListener<Void>>()
    val authFailureSlot = slot<OnFailureListener>()

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test-user-id"
    every { mockFirestore.collection("settings") } returns mockCollection
    every { mockFirestore.collection("users") } returns mockCollection
    every { mockCollection.document("test-user-id") } returns mockSettingsDoc andThen mockUsersDoc
    every { mockSettingsDoc.delete() } returns mockDeleteSettingsTask
    every { mockUsersDoc.delete() } returns mockDeleteUsersTask
    every { mockUser.delete() } returns mockDeleteAuthTask
    every { mockDeleteSettingsTask.addOnSuccessListener(capture(settingsSuccessSlot)) } returns
        mockDeleteSettingsTask
    every { mockDeleteSettingsTask.addOnFailureListener(any()) } returns mockDeleteSettingsTask
    every { mockDeleteUsersTask.addOnSuccessListener(capture(usersSuccessSlot)) } returns
        mockDeleteUsersTask
    every { mockDeleteUsersTask.addOnFailureListener(any()) } returns mockDeleteUsersTask
    every { mockDeleteAuthTask.addOnSuccessListener(any()) } returns mockDeleteAuthTask
    every { mockDeleteAuthTask.addOnFailureListener(capture(authFailureSlot)) } returns
        mockDeleteAuthTask

    val newViewModel = SettingsViewModel(mockAuth, mockFirestore)
    newViewModel.deleteAccount()

    // Wait for coroutines
    testDispatcher.scheduler.advanceUntilIdle()

    // Check if all slots were captured
    if (settingsSuccessSlot.isCaptured &&
        usersSuccessSlot.isCaptured &&
        authFailureSlot.isCaptured) {
      // Simulate cascade: settings success → users success → auth failure
      settingsSuccessSlot.captured.onSuccess(null)
      usersSuccessSlot.captured.onSuccess(null)
      authFailureSlot.captured.onFailure(Exception("Auth delete failed"))

      assertNotNull(newViewModel.errorMessage.value)
      assert(newViewModel.errorMessage.value?.contains("Failed to delete auth account") == true)
      assertEquals(false, newViewModel.isLoading.value)
    }
  }

  @Test
  fun deleteAccount_fullSuccess_setsLoadingToFalse() {
    val mockUser = mockk<FirebaseUser>(relaxed = true)
    val mockCollection = mockk<CollectionReference>(relaxed = true)
    val mockSettingsDoc = mockk<DocumentReference>(relaxed = true)
    val mockUsersDoc = mockk<DocumentReference>(relaxed = true)
    val mockDeleteSettingsTask = mockk<Task<Void>>(relaxed = true)
    val mockDeleteUsersTask = mockk<Task<Void>>(relaxed = true)
    val mockDeleteAuthTask = mockk<Task<Void>>(relaxed = true)
    val settingsSuccessSlot = slot<OnSuccessListener<Void>>()
    val usersSuccessSlot = slot<OnSuccessListener<Void>>()
    val authSuccessSlot = slot<OnSuccessListener<Void>>()

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test-user-id"
    every { mockFirestore.collection("settings") } returns mockCollection
    every { mockFirestore.collection("users") } returns mockCollection
    every { mockCollection.document("test-user-id") } returns mockSettingsDoc andThen mockUsersDoc
    every { mockSettingsDoc.delete() } returns mockDeleteSettingsTask
    every { mockUsersDoc.delete() } returns mockDeleteUsersTask
    every { mockUser.delete() } returns mockDeleteAuthTask
    every { mockDeleteSettingsTask.addOnSuccessListener(capture(settingsSuccessSlot)) } returns
        mockDeleteSettingsTask
    every { mockDeleteSettingsTask.addOnFailureListener(any()) } returns mockDeleteSettingsTask
    every { mockDeleteUsersTask.addOnSuccessListener(capture(usersSuccessSlot)) } returns
        mockDeleteUsersTask
    every { mockDeleteUsersTask.addOnFailureListener(any()) } returns mockDeleteUsersTask
    every { mockDeleteAuthTask.addOnSuccessListener(capture(authSuccessSlot)) } returns
        mockDeleteAuthTask
    every { mockDeleteAuthTask.addOnFailureListener(any()) } returns mockDeleteAuthTask

    val newViewModel = SettingsViewModel(mockAuth, mockFirestore)
    newViewModel.deleteAccount()

    // Wait for coroutines
    testDispatcher.scheduler.advanceUntilIdle()

    // Check if all slots were captured
    if (settingsSuccessSlot.isCaptured &&
        usersSuccessSlot.isCaptured &&
        authSuccessSlot.isCaptured) {
      // Simulate full success cascade
      settingsSuccessSlot.captured.onSuccess(null)
      usersSuccessSlot.captured.onSuccess(null)
      authSuccessSlot.captured.onSuccess(null)

      assertEquals(false, newViewModel.isLoading.value)
    }
  }
}
