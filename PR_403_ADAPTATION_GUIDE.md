# PR 403 Adaptation Guide

## Objective
Adapt PR 403 to replace the current queue-based deep link system in main with your cleaner architecture. Your approach is superior, but needs to integrate properly with what's already merged.

---

## Files to Modify

### 1. MainActivity.kt

**Replace the existing deep link code with yours:**

```kotlin
class MainActivity : ComponentActivity() {

  // REPLACE: Remove deepLinkQueue, use single state instead
  private var pendingDeepLink by mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // CRITICAL FIX: Check BOTH keys (your code already does this)
    val actionUrl = intent?.getStringExtra("actionUrl") ?: intent?.getStringExtra("action_url")
    if (actionUrl != null) {
      Log.d("MainActivity", "Deep link from notification: $actionUrl")
      pendingDeepLink = actionUrl
    }

    enableEdgeToEdge()
    // ... existing initialization code ...

    setContent {
      // ... existing theme setup ...

      // CHANGE: Pass deepLink instead of deepLinkQueue
      AppNavHost(isLoggedIn = isLoggedIn, deepLink = pendingDeepLink)
    }
  }

  override fun onNewIntent(newIntent: Intent) {
    super.onNewIntent(newIntent)

    // KEEP your dual-key checking
    val actionUrl = newIntent.getStringExtra("actionUrl") ?: newIntent.getStringExtra("action_url")
    actionUrl?.let {
      Log.d("MainActivity", "Deep link from new intent: $it")
      pendingDeepLink = it
      // Note: Don't call recreate(), just update state
    }
  }
}

// REMOVE this helper function entirely (it only checks one key):
// internal fun getDeepLinkUrlFromIntent(intent: Intent?): String?
```

**What to remove from main's code:**
- `private val deepLinkQueue = mutableStateListOf<String>()`
- `getDeepLinkUrlFromIntent(intent)?.let { deepLinkQueue.add(it) }` (both in onCreate and onNewIntent)
- The `getDeepLinkUrlFromIntent()` helper function

---

### 2. AppNavHost.kt

**Replace the queue processing with direct navigation:**

```kotlin
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    isLoggedIn: Boolean,
    deepLink: String? = null, // CHANGE: Single deepLink instead of deepLinkQueue
    renderMap: Boolean = true,
    autoRequestPermissions: Boolean = true
) {
  val startDest = if (isLoggedIn) Route.Map.route else Route.Auth.route

  // REPLACE: Use your LaunchedEffect with DeepLinkHandler
  LaunchedEffect(deepLink) {
    if (deepLink != null) {
      delay(500) // Keep your 500ms delay for cold start
      val route = DeepLinkHandler.parseDeepLink(deepLink)
      route?.let {
        navController.navigate(it) { launchSingleTop = true }
      }
    }
  }

  // Debounce navigation to prevent double-click issues
  var lastNavigationTime by remember { mutableLongStateOf(0L) }
  val navigationDebounceMs = 500L

  fun safePopBackStack() {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastNavigationTime > navigationDebounceMs) {
      lastNavigationTime = currentTime
      navController.popBackStack()
    }
  }

  NavHost(navController = navController, startDestination = startDest) {
    composable(Route.Auth.route) {
      SignInScreen(
          onSignInSuccess = {
            navController.navigate(Route.Map.route) {
              popUpTo(Route.Auth.route) { inclusive = true }
              launchSingleTop = true
            }
          })
    }

    composable(Route.Map.route) {
      MapScreen(
          onNavigateToProfile = { navController.navigate(Route.Profile.route) },
          onNavigateToSettings = { navController.navigate(Route.Settings.route) },
          onNavigateToFriends = { navController.navigate(Route.Friends.route) },
          onNavigateToChat = { navController.navigate(Route.Chat.route) },
          renderMap = renderMap,
          // REMOVE these params from main:
          // deepLinkEventId = currentDeepLinkEventId,
          // onDeepLinkConsumed = { currentDeepLinkEventId = null },
          autoRequestPermissions = autoRequestPermissions)
    }

    // ... Profile, Settings, ChangePassword routes stay the same ...

    // ADD your friends route with tab support
    composable(
        route = "friends?tab={tab}",
        arguments = listOf(
            androidx.navigation.navArgument("tab") {
              type = androidx.navigation.NavType.StringType
              nullable = true
              defaultValue = null
            })) { backStackEntry ->
      val tab = backStackEntry.arguments?.getString("tab")
      val viewModel: com.swent.mapin.ui.friends.FriendsViewModel =
          androidx.lifecycle.viewmodel.compose.viewModel()

      LaunchedEffect(tab) {
        if (tab == "REQUESTS") {
          viewModel.selectTab(com.swent.mapin.ui.friends.FriendsTab.REQUESTS)
        }
      }

      FriendsScreen(onNavigateBack = { safePopBackStack() }, viewModel = viewModel)
    }

    // Keep backward compatibility route
    composable(Route.Friends.route) {
      FriendsScreen(onNavigateBack = { safePopBackStack() })
    }

    // ... Chat, NewConversation, Conversation routes stay the same ...
  }
}
```

**What to remove from main's code:**
- `deepLinkQueue: SnapshotStateList<String>` parameter
- `var currentDeepLinkEventId by remember { mutableStateOf<String?>(null) }` state
- `LaunchedEffect(deepLinkQueue.size) { ... }` queue processing logic
- `parseDeepLinkEventId()` function calls
- The entire `parseDeepLinkEventId()` function definition

---

### 3. DeepLinkHandler.kt

**ADD this new file exactly as you have it in PR 403** - no changes needed.

Location: `app/src/main/java/com/swent/mapin/navigation/DeepLinkHandler.kt`

This file handles routing for:
- `mapin://friendRequests/{id}` → Friends screen (Requests tab)
- `mapin://events/{id}` → Map screen
- `mapin://messages/{conversationId}` → Conversation screen
- `mapin://messages` → Chat list
- `mapin://map` → Map screen

---

### 4. MapScreen.kt

**Remove the deep link parameters that main added:**

Find the MapScreen function signature and remove these two parameters:
- `deepLinkEventId: String? = null`
- `onDeepLinkConsumed: () -> Unit = {}`

Also remove any deep link handling code inside MapScreen that processes these parameters.

---

### 5. MapScreenViewModel.kt

**Remove the deep link queue processing code added by main:**

Remove these if they exist:
- `handleDeepLinkEvent(eventId: String)` function
- Any queue processing logic
- Deep link-specific state management related to the queue

---

## Testing Files

**Keep all your test files:**
- ✅ `DeepLinkHandlerTest.kt`
- ✅ `DeepLinkNavigationTest.kt`
- ✅ `MainActivityDeepLinkTest.kt` (ensure it tests BOTH `actionUrl` and `action_url` keys)

**Update/Remove from main's tests:**
- Remove or update any tests that specifically test the queue mechanism
- Remove tests in `MainActivityDeepLinkTest.kt` that test `getDeepLinkUrlFromIntent()` (if main added them)
- Remove tests in `AppNavHostDeepLinkTest.kt` that test `parseDeepLinkEventId()` (if main added them)

---

## Summary of Changes

| Action | What | Why |
|--------|------|-----|
| **Replace** | Queue (`mutableStateListOf`) → single state (`mutableStateOf`) in MainActivity | Simpler, sufficient for this use case |
| **Add** | Dual-key checking (`actionUrl` + `action_url`) | Critical Firebase compatibility fix |
| **Replace** | `parseDeepLinkEventId()` → `DeepLinkHandler` | Comprehensive multi-screen routing |
| **Remove** | MapScreen deep link params (`deepLinkEventId`, `onDeepLinkConsumed`) | Navigation handles it directly |
| **Add** | Friends tab routing with query params | Your feature for friend request notifications |
| **Keep** | 500ms delay, logging, error handling | Your debugging improvements |

---

## Expected Result

Clean, maintainable architecture that:
- ✅ Handles all notification types (friends, messages, events, map)
- ✅ Fixes the Firebase bug (dual-key checking)
- ✅ Reduces code complexity by ~40%
- ✅ Easier to extend for new notification types
- ✅ Single responsibility: DeepLinkHandler for parsing, NavController for navigation

---

## Key Points

1. **Don't use `recreate()`** - It causes activity to restart unnecessarily. State updates trigger recomposition automatically.

2. **Dual-key checking is critical** - Firebase Cloud Messaging sends `actionUrl` (camelCase), but PendingIntent uses `action_url` (snake_case). Check both.

3. **500ms delay matters** - Cold start requires NavHost initialization time. Keep this delay.

4. **Your architecture is better** - The queue system was over-engineered for edge cases that rarely happen. Your single-state approach is cleaner.

---

## Questions?

If you have questions about the integration, ask Ziyad or ping in the PR discussion.
