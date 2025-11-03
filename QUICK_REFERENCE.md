# Architecture Quick Reference

This is a quick reference guide to the Map'In app architecture. For detailed information, see the full documentation files.

## 📋 Table of Contents
1. [Architecture Pattern](#architecture-pattern)
2. [Project Structure](#project-structure)
3. [Key Classes](#key-classes)
4. [Data Flow](#data-flow)
5. [External Dependencies](#external-dependencies)

## Architecture Pattern

**MVVM (Model-View-ViewModel)**

```
View (Compose UI) ←→ ViewModel ←→ Repository ←→ Data Source
```

## Project Structure

```
app/src/main/java/com/swent/mapin/
│
├── 📱 MainActivity.kt              # App entry point
│
├── 🧭 navigation/
│   ├── AppNavHost.kt              # Navigation controller
│   └── Routes.kt                  # Screen routes
│
├── 🎨 ui/
│   ├── auth/                      # Authentication screens
│   │   ├── SignInScreen.kt
│   │   └── SignInViewModel.kt
│   │
│   ├── map/                       # Map screens
│   │   ├── MapScreen.kt           # Main map interface
│   │   ├── MapScreenViewModel.kt
│   │   ├── MemoryFormScreen.kt    # Memory creation
│   │   ├── FilterSection.kt       # Event filtering
│   │   └── FilterSectionViewModel.kt
│   │
│   ├── profile/                   # Profile screens
│   │   ├── ProfileScreen.kt
│   │   └── ProfileViewModel.kt
│   │
│   ├── event/                     # Event screens
│   │   ├── AddEventScreen.kt      # Create/edit events
│   │   ├── EventViewModel.kt
│   │   └── LocationDropDownMenu.kt
│   │
│   ├── friends/                   # Friends screens
│   │   ├── FriendsScreen.kt       # Friends list
│   │   └── FriendsViewModel.kt
│   │
│   ├── settings/                  # Settings screens
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   │
│   ├── components/                # Reusable UI components
│   │   └── BottomSheet.kt
│   │
│   └── theme/                     # Material theme
│       ├── Theme.kt
│       ├── Color.kt
│       └── Type.kt
│
├── 📊 model/
│   ├── event/                     # Event domain
│   │   ├── event.kt               # Event data class
│   │   ├── EventRepository.kt     # Interface
│   │   ├── EventRepositoryFirestore.kt
│   │   ├── LocalEventRepository.kt
│   │   └── EventRepositoryProvider.kt
│   │
│   ├── memory/                    # Memory domain
│   │   ├── Memory.kt              # Memory data class
│   │   ├── MemoryRepository.kt    # Interface
│   │   ├── MemoryRepositoryFirestore.kt
│   │   ├── LocalMemoryRepository.kt
│   │   └── MemoryRepositoryProvider.kt
│   │
│   ├── UserProfile.kt             # User data class
│   ├── UserProfileRepository.kt   # User repository
│   ├── FriendRequest.kt           # Friend request data class
│   ├── Location.kt                # Location data class
│   ├── LocationRepository.kt      # Geocoding interface
│   ├── LocationViewModel.kt       # Location search logic
│   ├── NominatimLocationRepository.kt
│   ├── ImageUploadHelper.kt       # Image uploads
│   └── RateLimiter.kt             # API rate limiting
│
└── 🔧 util/
    ├── TimeUtils.kt               # Time formatting
    └── ContextExtensions.kt       # Android extensions
```

## Key Classes

### Screens (UI Layer)
| Screen | Purpose | ViewModel |
|--------|---------|-----------|
| `SignInScreen` | User authentication | `SignInViewModel` |
| `MapScreen` | Interactive map with events | `MapScreenViewModel` |
| `ProfileScreen` | User profile management | `ProfileViewModel` |
| `MemoryFormScreen` | Create memories for events | `MapScreenViewModel` |
| `AddEventScreen` | Create and edit events | `EventViewModel` |
| `FriendsScreen` | Manage friends and requests | `FriendsViewModel` |
| `SettingsScreen` | App settings and preferences | `SettingsViewModel` |

### ViewModels (Business Logic)
| ViewModel | Manages | Key Dependencies |
|-----------|---------|------------------|
| `SignInViewModel` | Authentication state | Firebase Auth |
| `MapScreenViewModel` | Map data, events, memories | Event/Memory Repositories |
| `ProfileViewModel` | User profile data | UserProfileRepository |
| `EventViewModel` | Event creation | EventRepository |
| `LocationViewModel` | Location search | LocationRepository |
| `FriendsViewModel` | Friend list, requests | UserProfileRepository |
| `SettingsViewModel` | Settings, preferences | UserProfileRepository |
| `FilterSectionViewModel` | Event filters (tags, dates) | None (UI state) |

### Repositories (Data Access)
| Repository | Interface | Implementations | Data Source |
|------------|-----------|-----------------|-------------|
| Event | `EventRepository` | Firestore, Local | Firebase/Memory |
| Memory | `MemoryRepository` | Firestore, Local | Firebase/Memory |
| User Profile | `UserProfileRepository` | Firestore | Firebase |
| Location | `LocationRepository` | Nominatim | Nominatim API |

### Data Models
| Model | Description | Key Fields |
|-------|-------------|------------|
| `Event` | Event information | uid, title, location, date, participants |
| `Memory` | User memories | uid, eventId, mediaUrls, taggedUsers |
| `UserProfile` | User data | userId, name, bio, hobbies, events |
| `FriendRequest` | Friend requests | senderId, receiverId, status |
| `Location` | Geographic location | name, latitude, longitude |

## Data Flow

### Creating an Event
```
User Input 
  → AddEventPopUp (UI)
    → EventViewModel
      → ImageUploadHelper (if image)
        → Firebase Storage
      → EventRepository
        → Firestore
```

### Loading Events on Map
```
MapScreen Init
  → MapScreenViewModel
    → EventRepository.getAllEvents()
      → Firestore Query
        → Return List<Event>
          → Update UI State
            → Render Markers on Map
```

### Location Search
```
User Types
  → LocationDropDownMenu (UI)
    → LocationViewModel
      → LocationRepository
        → RateLimiter (check)
          → NominatimAPI
            → Return Results
```

### Adding Memory
```
User Selects Photos
  → MemoryFormScreen (UI)
    → MapScreenViewModel
      → ImageUploadHelper (for each photo)
        → Firebase Storage
      → MemoryRepository
        → Firestore
```

## External Dependencies

### Firebase Services
- **Firebase Auth**: User authentication (Google Sign-In)
- **Firebase Firestore**: NoSQL database for events, memories, profiles
- **Firebase Storage**: Cloud storage for images and media

### Map Services
- **Mapbox**: Primary map rendering (NDK 27)
- **Google Maps**: Alternative map provider

### APIs
- **Nominatim API**: OpenStreetMap geocoding (address ↔ coordinates)

### Networking
- **OkHttp**: HTTP client for API requests

## Testing Structure

```
app/src/
├── test/                          # Unit Tests (JVM)
│   └── java/com/swent/mapin/
│       ├── model/                 # Repository tests
│       ├── ui/                    # ViewModel tests
│       └── signinTests/           # Auth tests
│
└── androidTest/                   # Instrumented Tests
    └── java/com/swent/mapin/
        ├── e2e/                   # End-to-end tests
        └── ui/                    # UI component tests
```

### Test Implementations
- **LocalEventRepository**: In-memory event storage for testing
- **LocalMemoryRepository**: In-memory memory storage for testing
- **Mockito/MockK**: Mocking frameworks

## Common Patterns

### Repository Pattern
```kotlin
interface EventRepository {
    suspend fun getAllEvents(): List<Event>
    suspend fun addEvent(event: Event)
    // ...
}

class EventRepositoryFirestore : EventRepository {
    // Firebase implementation
}

class LocalEventRepository : EventRepository {
    // In-memory implementation for tests
}
```

### ViewModel Pattern
```kotlin
class MapScreenViewModel(
    private val eventRepository: EventRepository,
    private val memoryRepository: MemoryRepository
) : ViewModel() {
    
    private var _events by mutableStateOf<List<Event>>(emptyList())
    val events: List<Event> get() = _events
    
    fun loadEvents() = viewModelScope.launch {
        _events = eventRepository.getAllEvents()
    }
}
```

### Compose UI Pattern
```kotlin
@Composable
fun MapScreen(
    viewModel: MapScreenViewModel = viewModel()
) {
    val events = viewModel.events
    
    // Render UI
    MapboxMap {
        events.forEach { event ->
            Marker(position = event.location)
        }
    }
}
```

## Key Configuration Files

| File | Purpose |
|------|---------|
| `build.gradle.kts` | Build configuration, dependencies |
| `google-services.json` | Firebase configuration |
| `local.properties` | API keys (not committed) |
| `AndroidManifest.xml` | App permissions, activities |

## Build & Run

### Gradle Tasks
```bash
./gradlew ktfmtFormat              # Format code
./gradlew testDebugUnitTest        # Unit tests
./gradlew connectedDebugAndroidTest # Instrumentation tests
./gradlew assembleDebug            # Build APK
./gradlew jacocoTestReport         # Coverage report
```

## Navigation Flow

```
Launch App
  → Firebase Auth Check
    ├─ Not Authenticated → SignInScreen → MapScreen
    └─ Authenticated → MapScreen
  
MapScreen
  ├─ Profile Button → ProfileScreen
  │   ├─ Settings Button → SettingsScreen
  │   ├─ Friends Button → FriendsScreen
  │   └─ Sign Out → SignInScreen
  ├─ Add Event → AddEventScreen
  ├─ Marker Tap → EventDetailSheet
  │   └─ Add Memory → MemoryFormScreen
  ├─ Filter Button → FilterSection
  └─ Search → LocationDropDown

ProfileScreen
  ├─ Settings → SettingsScreen
  ├─ Friends → FriendsScreen
  └─ Back → MapScreen

FriendsScreen
  ├─ Send Friend Request
  ├─ Accept/Reject Requests
  └─ Back → ProfileScreen

SettingsScreen
  ├─ Change Preferences
  ├─ Manage Account
  └─ Back → ProfileScreen
```

## State Management

### UI State
- Managed by ViewModels using `mutableStateOf`
- Compose automatically recomposes on state changes

### Data State
- Repository provides single source of truth
- ViewModels cache data from repositories
- Coroutines handle async operations

### Navigation State
- NavController manages navigation stack
- Routes defined in sealed class

## Security

- Firebase Security Rules control data access
- API keys in `local.properties` (gitignored)
- User authentication required for write operations
- Rate limiting prevents API abuse

## Performance

- Coroutines for async operations (non-blocking)
- StateFlow/MutableState for reactive updates
- Local repositories for fast tests
- Image compression before upload
- Pagination support in repositories

---

For detailed information, see:
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Complete architecture guide
- **[COMPONENT_ARCHITECTURE.md](COMPONENT_ARCHITECTURE.md)** - Detailed components
- **[USER_FLOWS.md](USER_FLOWS.md)** - User flow diagrams
- **[docs/README.md](docs/README.md)** - Documentation index
