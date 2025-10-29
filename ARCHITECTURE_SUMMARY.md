# Map'In - Complete System Architecture Diagram

This document contains a comprehensive, all-in-one architecture diagram showing the entire Map'In application system.

## Complete System Architecture

```mermaid
graph TB
    %% ============================================
    %% USER INTERACTION LAYER
    %% ============================================
    User[👤 User]
    
    %% ============================================
    %% PRESENTATION LAYER (UI)
    %% ============================================
    subgraph UI["📱 PRESENTATION LAYER - Jetpack Compose UI"]
        MainActivity[MainActivity<br/>Entry Point]
        
        subgraph Navigation["Navigation"]
            AppNavHost[AppNavHost<br/>Navigation Controller]
            Routes[Routes<br/>Auth | Map | Profile]
        end
        
        subgraph AuthUI["🔐 Authentication UI"]
            SignInScreen[SignInScreen<br/>Google Sign-In]
        end
        
        subgraph MapUI["🗺️ Map UI"]
            MapScreen[MapScreen<br/>Main Interface]
            MemoryForm[MemoryFormScreen<br/>Add Memories]
            EventDetail[EventDetailSheet<br/>Event Info]
            BottomSheet[BottomSheetContent<br/>Filters & Search]
            AddEvent[AddEventPopUp<br/>Create Events]
        end
        
        subgraph ProfileUI["👤 Profile UI"]
            ProfileScreen[ProfileScreen<br/>User Profile]
        end
        
        subgraph Components["🧩 Reusable Components"]
            LocationDrop[LocationDropDownMenu<br/>Search Locations]
            MapStyle[MapStyleSelector<br/>Map Themes]
            Dialogs[Various Dialogs<br/>User/Event Pickers]
        end
    end
    
    %% ============================================
    %% VIEWMODEL LAYER
    %% ============================================
    subgraph VM["🎯 VIEWMODEL LAYER - State Management & Business Logic"]
        SignInVM[SignInViewModel<br/>Auth State]
        MapScreenVM[MapScreenViewModel<br/>Map Data & Interactions]
        ProfileVM[ProfileViewModel<br/>Profile Management]
        EventVM[EventViewModel<br/>Event Creation]
        LocationVM[LocationViewModel<br/>Location Search]
    end
    
    %% ============================================
    %% REPOSITORY LAYER
    %% ============================================
    subgraph Repo["💾 REPOSITORY LAYER - Data Access Abstraction"]
        subgraph EventRepo["Event Domain"]
            EventInterface[EventRepository<br/>Interface]
            EventFirestore[EventRepositoryFirestore<br/>Production]
            EventLocal[LocalEventRepository<br/>Testing]
            EventProvider[EventRepositoryProvider<br/>DI]
        end
        
        subgraph MemoryRepo["Memory Domain"]
            MemoryInterface[MemoryRepository<br/>Interface]
            MemoryFirestore[MemoryRepositoryFirestore<br/>Production]
            MemoryLocal[LocalMemoryRepository<br/>Testing]
            MemoryProvider[MemoryRepositoryProvider<br/>DI]
        end
        
        subgraph UserRepo["User Domain"]
            UserProfileRepo[UserProfileRepository<br/>Firestore]
        end
        
        subgraph LocationRepo["Location Domain"]
            LocationInterface[LocationRepository<br/>Interface]
            NominatimRepo[NominatimLocationRepository<br/>Geocoding]
            ForwardGeo[NominatimForwardGeocoder<br/>Address → Coords]
            ReverseGeo[NominatimReverseGeocoder<br/>Coords → Address]
        end
        
        subgraph Utilities["🔧 Utilities"]
            ImageHelper[ImageUploadHelper<br/>Media Upload]
            RateLimiter[RateLimiter<br/>API Throttling]
            TimeUtils[TimeUtils<br/>Date Formatting]
            HttpClient[HttpClientProvider<br/>OkHttp Client]
        end
    end
    
    %% ============================================
    %% DATA MODEL LAYER
    %% ============================================
    subgraph Models["📊 DATA MODELS"]
        EventModel[Event<br/>uid, title, location,<br/>date, participants]
        MemoryModel[Memory<br/>uid, eventId, mediaUrls,<br/>taggedUsers]
        UserModel[UserProfile<br/>userId, name, bio,<br/>hobbies, events]
        LocationModel[Location<br/>name, latitude,<br/>longitude]
    end
    
    %% ============================================
    %% EXTERNAL SERVICES
    %% ============================================
    subgraph External["☁️ EXTERNAL SERVICES"]
        subgraph Firebase["Firebase Backend"]
            FireAuth[(Firebase Auth<br/>Google Sign-In)]
            Firestore[(Firestore DB<br/>Events, Memories,<br/>Profiles)]
            FireStorage[(Firebase Storage<br/>Images & Media)]
        end
        
        subgraph MapServices["Map Services"]
            Mapbox[Mapbox SDK<br/>Map Rendering]
            GoogleMaps[Google Maps<br/>Alternative Maps]
        end
        
        subgraph APIs["External APIs"]
            Nominatim[Nominatim API<br/>OpenStreetMap<br/>Geocoding]
        end
    end
    
    %% ============================================
    %% USER INTERACTIONS
    %% ============================================
    User -->|Opens App| MainActivity
    User -->|Authenticates| SignInScreen
    User -->|Views Map| MapScreen
    User -->|Creates Event| AddEvent
    User -->|Adds Memory| MemoryForm
    User -->|Manages Profile| ProfileScreen
    User -->|Searches Location| LocationDrop
    
    %% ============================================
    %% UI LAYER CONNECTIONS
    %% ============================================
    MainActivity --> AppNavHost
    AppNavHost --> Routes
    Routes --> SignInScreen
    Routes --> MapScreen
    Routes --> ProfileScreen
    
    SignInScreen --> SignInVM
    MapScreen --> MapScreenVM
    MapScreen --> EventVM
    MemoryForm --> MapScreenVM
    ProfileScreen --> ProfileVM
    AddEvent --> EventVM
    LocationDrop --> LocationVM
    
    MapScreen --> MemoryForm
    MapScreen --> EventDetail
    MapScreen --> BottomSheet
    MapScreen --> AddEvent
    MapScreen --> LocationDrop
    MapScreen --> MapStyle
    
    %% ============================================
    %% VIEWMODEL TO REPOSITORY CONNECTIONS
    %% ============================================
    SignInVM --> FireAuth
    
    MapScreenVM --> EventInterface
    MapScreenVM --> MemoryInterface
    MapScreenVM --> LocationInterface
    
    ProfileVM --> UserProfileRepo
    ProfileVM --> FireStorage
    
    EventVM --> EventInterface
    EventVM --> FireStorage
    EventVM --> ImageHelper
    
    LocationVM --> LocationInterface
    
    %% ============================================
    %% REPOSITORY IMPLEMENTATION CONNECTIONS
    %% ============================================
    EventInterface -.implements.-> EventFirestore
    EventInterface -.implements.-> EventLocal
    EventProvider --> EventInterface
    
    MemoryInterface -.implements.-> MemoryFirestore
    MemoryInterface -.implements.-> MemoryLocal
    MemoryProvider --> MemoryInterface
    
    LocationInterface -.implements.-> NominatimRepo
    NominatimRepo --> ForwardGeo
    NominatimRepo --> ReverseGeo
    NominatimRepo --> RateLimiter
    NominatimRepo --> HttpClient
    
    %% ============================================
    %% REPOSITORY TO DATA MODEL CONNECTIONS
    %% ============================================
    EventFirestore --> EventModel
    EventLocal --> EventModel
    MemoryFirestore --> MemoryModel
    MemoryLocal --> MemoryModel
    UserProfileRepo --> UserModel
    NominatimRepo --> LocationModel
    
    %% ============================================
    %% REPOSITORY TO EXTERNAL SERVICE CONNECTIONS
    %% ============================================
    EventFirestore --> Firestore
    MemoryFirestore --> Firestore
    UserProfileRepo --> Firestore
    
    ImageHelper --> FireStorage
    EventFirestore --> ImageHelper
    MemoryFirestore --> ImageHelper
    
    ForwardGeo --> Nominatim
    ReverseGeo --> Nominatim
    ForwardGeo --> HttpClient
    ReverseGeo --> HttpClient
    
    MapScreen --> Mapbox
    MapScreen --> GoogleMaps
    
    %% ============================================
    %% STYLING
    %% ============================================
    classDef uiLayer fill:#e1f5ff,stroke:#01579b,stroke-width:2px
    classDef vmLayer fill:#fff4e1,stroke:#e65100,stroke-width:2px
    classDef repoLayer fill:#e8f5e9,stroke:#1b5e20,stroke-width:2px
    classDef implLayer fill:#c8e6c9,stroke:#2e7d32,stroke-width:2px
    classDef modelLayer fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef externalLayer fill:#ffebee,stroke:#c62828,stroke-width:2px
    classDef userLayer fill:#fff9c4,stroke:#f57f17,stroke-width:3px
    
    class MainActivity,AppNavHost,Routes,SignInScreen,MapScreen,MemoryForm,EventDetail,BottomSheet,AddEvent,ProfileScreen,LocationDrop,MapStyle,Dialogs uiLayer
    class SignInVM,MapScreenVM,ProfileVM,EventVM,LocationVM vmLayer
    class EventInterface,MemoryInterface,UserProfileRepo,LocationInterface repoLayer
    class EventFirestore,EventLocal,MemoryFirestore,MemoryLocal,NominatimRepo,ForwardGeo,ReverseGeo,EventProvider,MemoryProvider implLayer
    class EventModel,MemoryModel,UserModel,LocationModel modelLayer
    class FireAuth,Firestore,FireStorage,Mapbox,GoogleMaps,Nominatim externalLayer
    class User userLayer
    class ImageHelper,RateLimiter,TimeUtils,HttpClient implLayer
```

## Architecture Layers Explained

### 1. 👤 User Interaction Layer
The end user interacting with the mobile application through various screens and actions.

### 2. 📱 Presentation Layer (UI)
- **Framework**: Jetpack Compose with Material 3
- **Screens**: SignIn, Map, Profile, MemoryForm
- **Components**: Reusable UI elements (dialogs, dropdowns, sheets)
- **Navigation**: AppNavHost manages screen transitions

### 3. 🎯 ViewModel Layer
- **Pattern**: MVVM architecture
- **Responsibility**: State management, business logic, UI event handling
- **Coroutines**: Async operations with viewModelScope
- **Key VMs**: SignIn, MapScreen, Profile, Event, Location

### 4. 💾 Repository Layer
- **Pattern**: Repository pattern with interfaces
- **Implementations**: Production (Firestore) and Testing (Local in-memory)
- **Domains**: Event, Memory, UserProfile, Location
- **Dependency Injection**: Provider pattern for repository management

### 5. 📊 Data Models
- **Event**: Event information with location and participants
- **Memory**: User memories/photos linked to events
- **UserProfile**: User data and preferences
- **Location**: Geographic coordinates

### 6. ☁️ External Services
- **Firebase**: Auth (Google Sign-In), Firestore (NoSQL DB), Storage (media files)
- **Maps**: Mapbox (primary), Google Maps (alternative)
- **Geocoding**: Nominatim API (OpenStreetMap)

## Data Flow Patterns

### Read Flow (Displaying Events)
```
User → MapScreen → MapScreenViewModel → EventRepository → Firestore → EventModel → Update UI State → Render Markers
```

### Write Flow (Creating Event)
```
User → AddEventPopUp → EventViewModel → ImageUploadHelper → Firebase Storage → EventRepository → Firestore → Success → Refresh UI
```

### Location Search Flow
```
User Types → LocationDropDown → LocationViewModel → LocationRepository → RateLimiter → Nominatim API → Results → Display
```

### Authentication Flow
```
User → SignInScreen → SignInViewModel → Firebase Auth → Success → Navigate to MapScreen
```

## Key Architecture Patterns

| Pattern | Implementation | Purpose |
|---------|---------------|---------|
| **MVVM** | UI ← ViewModel ← Repository | Separation of concerns |
| **Repository** | Interface + Firestore/Local impls | Data source abstraction |
| **Dependency Injection** | Provider pattern | Flexible repository management |
| **State Management** | mutableStateOf in ViewModels | Reactive UI updates |
| **Async Operations** | Kotlin Coroutines + viewModelScope | Non-blocking operations |

## Technology Stack

| Layer | Technologies |
|-------|-------------|
| **Language** | Kotlin |
| **UI** | Jetpack Compose, Material 3 |
| **Navigation** | Jetpack Navigation Compose |
| **Async** | Kotlin Coroutines, Flow |
| **Backend** | Firebase (Auth, Firestore, Storage) |
| **Maps** | Mapbox SDK, Google Maps |
| **Networking** | OkHttp |
| **Testing** | JUnit, Mockito, MockK, Kaspresso, Robolectric |
| **Build** | Gradle (Kotlin DSL) |

## Color Legend

- 🔵 **Blue** - UI Layer (Presentation)
- 🟡 **Yellow** - ViewModel Layer (Business Logic)
- 🟢 **Green** - Repository Layer (Data Access)
- 🟣 **Purple** - Data Models
- 🔴 **Red** - External Services
- ⭐ **Gold** - User

## Project Structure Summary

```
app/src/main/java/com/swent/mapin/
├── 📱 UI Layer
│   ├── MainActivity.kt
│   ├── navigation/ (AppNavHost, Routes)
│   └── ui/ (auth, map, profile, components, theme)
│
├── 🎯 ViewModel Layer
│   └── ui/*ViewModel.kt files
│
├── 💾 Repository Layer
│   └── model/ (event, memory, UserProfile, Location repositories)
│
├── 📊 Data Models
│   └── model/ (Event, Memory, UserProfile, Location data classes)
│
└── 🔧 Utilities
    ├── model/ (ImageUploadHelper, RateLimiter)
    └── util/ (TimeUtils, ContextExtensions)
```

---

**Related Documentation:**
- [ARCHITECTURE.md](ARCHITECTURE.md) - Detailed architecture explanation
- [COMPONENT_ARCHITECTURE.md](COMPONENT_ARCHITECTURE.md) - File-level component mapping
- [USER_FLOWS.md](USER_FLOWS.md) - Sequence diagrams for user interactions
- [QUICK_REFERENCE.md](QUICK_REFERENCE.md) - Quick lookup guide
