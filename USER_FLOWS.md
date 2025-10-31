# Map'In User Flow & Sequence Diagrams

This document illustrates the key user flows and their corresponding sequence diagrams in the Map'In application.

## User Authentication Flow

```mermaid
sequenceDiagram
    participant User
    participant SignInScreen
    participant SignInViewModel
    participant FirebaseAuth
    participant AppNavHost
    
    User->>SignInScreen: Open app
    SignInScreen->>SignInViewModel: Check auth state
    SignInViewModel->>FirebaseAuth: getCurrentUser()
    
    alt User not authenticated
        FirebaseAuth-->>SignInViewModel: null
        SignInViewModel-->>SignInScreen: Show sign-in options
        User->>SignInScreen: Click "Sign in with Google"
        SignInScreen->>SignInViewModel: signInWithGoogle()
        SignInViewModel->>FirebaseAuth: signInWithCredential()
        FirebaseAuth-->>SignInViewModel: Success
        SignInViewModel-->>AppNavHost: Navigate to Map
    else User authenticated
        FirebaseAuth-->>SignInViewModel: User object
        SignInViewModel-->>AppNavHost: Navigate to Map
    end
```

## Event Creation Flow

```mermaid
sequenceDiagram
    participant User
    participant MapScreen
    participant AddEventPopUp
    participant EventViewModel
    participant LocationViewModel
    participant ImageUploadHelper
    participant EventRepository
    participant Firestore
    participant FirebaseStorage
    
    User->>MapScreen: Long press on map
    MapScreen->>AddEventPopUp: Show dialog
    User->>AddEventPopUp: Fill event details
    
    User->>AddEventPopUp: Search location
    AddEventPopUp->>LocationViewModel: searchLocation(query)
    LocationViewModel->>LocationRepository: forwardGeocode(query)
    LocationRepository->>NominatimAPI: API call
    NominatimAPI-->>LocationRepository: Location results
    LocationRepository-->>LocationViewModel: List<Location>
    LocationViewModel-->>AddEventPopUp: Display results
    User->>AddEventPopUp: Select location
    
    User->>AddEventPopUp: Add event image
    AddEventPopUp->>EventViewModel: setImageUri(uri)
    
    User->>AddEventPopUp: Click "Create Event"
    AddEventPopUp->>EventViewModel: createEvent()
    
    alt Image provided
        EventViewModel->>ImageUploadHelper: uploadImage(uri)
        ImageUploadHelper->>FirebaseStorage: uploadBytes()
        FirebaseStorage-->>ImageUploadHelper: Download URL
        ImageUploadHelper-->>EventViewModel: imageUrl
    end
    
    EventViewModel->>EventRepository: addEvent(event)
    EventRepository->>Firestore: document.set(event)
    Firestore-->>EventRepository: Success
    EventRepository-->>EventViewModel: Success
    EventViewModel-->>AddEventPopUp: Close dialog
    AddEventPopUp-->>MapScreen: Refresh events
    MapScreen->>MapScreen: Display new event marker
```

## Memory Creation Flow

```mermaid
sequenceDiagram
    participant User
    participant EventDetailSheet
    participant MemoryFormScreen
    participant MapScreenViewModel
    participant ImageUploadHelper
    participant MemoryRepository
    participant Firestore
    participant FirebaseStorage
    
    User->>EventDetailSheet: View event details
    User->>EventDetailSheet: Click "Add Memory"
    EventDetailSheet->>MemoryFormScreen: Navigate
    
    User->>MemoryFormScreen: Fill memory details
    User->>MemoryFormScreen: Add photos/videos
    MemoryFormScreen->>MapScreenViewModel: setSelectedMedia(uris)
    
    User->>MemoryFormScreen: Tag friends
    MemoryFormScreen->>MapScreenViewModel: setTaggedUsers(userIds)
    
    User->>MemoryFormScreen: Click "Save"
    MemoryFormScreen->>MapScreenViewModel: createMemory()
    
    loop For each media file
        MapScreenViewModel->>ImageUploadHelper: uploadImage(uri)
        ImageUploadHelper->>FirebaseStorage: uploadBytes()
        FirebaseStorage-->>ImageUploadHelper: Download URL
        ImageUploadHelper-->>MapScreenViewModel: mediaUrl
    end
    
    MapScreenViewModel->>MemoryRepository: addMemory(memory)
    MemoryRepository->>Firestore: document.set(memory)
    Firestore-->>MemoryRepository: Success
    MemoryRepository-->>MapScreenViewModel: Success
    MapScreenViewModel-->>MemoryFormScreen: Navigate back
    MemoryFormScreen-->>EventDetailSheet: Show new memory
```

## Map Display & Event Loading Flow

```mermaid
sequenceDiagram
    participant MapScreen
    participant MapScreenViewModel
    participant EventRepository
    participant MemoryRepository
    participant Firestore
    participant Mapbox
    
    MapScreen->>MapScreenViewModel: Initialize
    MapScreenViewModel->>EventRepository: getAllEvents()
    EventRepository->>Firestore: Query events collection
    Firestore-->>EventRepository: List<Event>
    EventRepository-->>MapScreenViewModel: Events data
    
    MapScreenViewModel->>MemoryRepository: getAllMemories()
    MemoryRepository->>Firestore: Query memories collection
    Firestore-->>MemoryRepository: List<Memory>
    MemoryRepository-->>MapScreenViewModel: Memories data
    
    MapScreenViewModel->>MapScreenViewModel: Process data
    MapScreenViewModel-->>MapScreen: Update state
    
    MapScreen->>Mapbox: Render map
    
    loop For each event
        MapScreen->>Mapbox: Add event marker
    end
    
    loop For each memory
        MapScreen->>Mapbox: Add memory marker
    end
    
    Mapbox-->>MapScreen: Display interactive map
```

## Location Search Flow

```mermaid
sequenceDiagram
    participant User
    participant LocationDropDown
    participant LocationViewModel
    participant LocationRepository
    participant RateLimiter
    participant NominatimAPI
    participant OkHttp
    
    User->>LocationDropDown: Type search query
    LocationDropDown->>LocationViewModel: search(query)
    LocationViewModel->>LocationRepository: forwardGeocode(query)
    LocationRepository->>RateLimiter: checkLimit()
    
    alt Rate limit OK
        RateLimiter-->>LocationRepository: Allowed
        LocationRepository->>NominatimAPI: Forward geocode request
        NominatimAPI->>OkHttp: HTTP GET
        OkHttp-->>NominatimAPI: JSON response
        NominatimAPI-->>LocationRepository: Parse results
        LocationRepository-->>LocationViewModel: List<Location>
        LocationViewModel-->>LocationDropDown: Display results
    else Rate limit exceeded
        RateLimiter-->>LocationRepository: Denied
        LocationRepository-->>LocationViewModel: Error
        LocationViewModel-->>LocationDropDown: Show error message
    end
    
    User->>LocationDropDown: Select location
    LocationDropDown->>LocationViewModel: setSelectedLocation(location)
    LocationViewModel-->>LocationDropDown: Update UI
```

## User Profile Update Flow

```mermaid
sequenceDiagram
    participant User
    participant ProfileScreen
    participant ProfileViewModel
    participant UserProfileRepository
    participant ImageUploadHelper
    participant Firestore
    participant FirebaseStorage
    
    User->>ProfileScreen: Navigate to profile
    ProfileScreen->>ProfileViewModel: loadProfile()
    ProfileViewModel->>UserProfileRepository: getUserProfile(userId)
    UserProfileRepository->>Firestore: document.get()
    Firestore-->>UserProfileRepository: UserProfile data
    UserProfileRepository-->>ProfileViewModel: UserProfile
    ProfileViewModel-->>ProfileScreen: Display profile
    
    User->>ProfileScreen: Edit profile fields
    User->>ProfileScreen: Change profile picture
    ProfileScreen->>ProfileViewModel: setProfilePictureUri(uri)
    
    User->>ProfileScreen: Click "Save"
    ProfileScreen->>ProfileViewModel: saveProfile()
    
    alt Profile picture changed
        ProfileViewModel->>ImageUploadHelper: uploadImage(uri)
        ImageUploadHelper->>FirebaseStorage: uploadBytes()
        FirebaseStorage-->>ImageUploadHelper: Download URL
        ImageUploadHelper-->>ProfileViewModel: imageUrl
    end
    
    ProfileViewModel->>UserProfileRepository: saveUserProfile(profile)
    UserProfileRepository->>Firestore: document.set(profile)
    Firestore-->>UserProfileRepository: Success
    UserProfileRepository-->>ProfileViewModel: Success
    ProfileViewModel-->>ProfileScreen: Show success message
```

## Event Participation Flow

```mermaid
sequenceDiagram
    participant User
    participant EventDetailSheet
    participant MapScreenViewModel
    participant EventRepository
    participant UserProfileRepository
    participant Firestore
    
    User->>EventDetailSheet: View event
    EventDetailSheet->>MapScreenViewModel: Display event details
    
    User->>EventDetailSheet: Click "Join Event"
    EventDetailSheet->>MapScreenViewModel: joinEvent(eventId)
    
    MapScreenViewModel->>EventRepository: getEvent(eventId)
    EventRepository->>Firestore: document.get()
    Firestore-->>EventRepository: Event data
    EventRepository-->>MapScreenViewModel: Event
    
    MapScreenViewModel->>MapScreenViewModel: Add user to participants
    MapScreenViewModel->>EventRepository: editEvent(eventId, updatedEvent)
    EventRepository->>Firestore: document.update()
    Firestore-->>EventRepository: Success
    
    MapScreenViewModel->>UserProfileRepository: getUserProfile(userId)
    UserProfileRepository->>Firestore: document.get()
    Firestore-->>UserProfileRepository: UserProfile
    UserProfileRepository-->>MapScreenViewModel: UserProfile
    
    MapScreenViewModel->>MapScreenViewModel: Add event to participatingEvents
    MapScreenViewModel->>UserProfileRepository: saveUserProfile(profile)
    UserProfileRepository->>Firestore: document.update()
    Firestore-->>UserProfileRepository: Success
    
    UserProfileRepository-->>MapScreenViewModel: Success
    MapScreenViewModel-->>EventDetailSheet: Update UI
    EventDetailSheet->>EventDetailSheet: Show "Joined" status
```

## Data Synchronization Flow

```mermaid
sequenceDiagram
    participant MapScreen
    participant MapScreenViewModel
    participant EventRepository
    participant MemoryRepository
    participant Firestore
    participant FirebaseAuth
    
    Note over MapScreen,Firestore: Real-time updates via Firestore listeners
    
    MapScreen->>MapScreenViewModel: onResume()
    MapScreenViewModel->>EventRepository: getAllEvents()
    EventRepository->>Firestore: Add snapshot listener
    
    loop On data change
        Firestore-->>EventRepository: Snapshot update
        EventRepository-->>MapScreenViewModel: Updated events
        MapScreenViewModel-->>MapScreen: Refresh markers
    end
    
    MapScreen->>MapScreen: onPause()
    MapScreen->>MapScreenViewModel: Clear listeners
    MapScreenViewModel->>EventRepository: Remove listeners
```

## Key User Journeys

### 1. First-Time User Journey
```
Launch App → Sign In → View Map → Create Profile → Discover Events → Join Event
```

### 2. Create Event Journey
```
View Map → Long Press Location → Fill Event Form → Search Location → Add Image → Save Event → View on Map
```

### 3. Add Memory Journey
```
View Map → Tap Event Marker → View Event Details → Add Memory → Upload Photos → Tag Friends → Save Memory
```

### 4. Event Discovery Journey
```
View Map → Browse Markers → Filter by Tags → View Event Details → Check Participants → Join Event
```

### 5. Profile Management Journey
```
Navigate to Profile → Edit Bio → Add Hobbies → Upload Profile Picture → Save Changes
```

## Error Handling Flows

### Network Error Flow
```mermaid
sequenceDiagram
    participant UI
    participant ViewModel
    participant Repository
    participant Network
    
    UI->>ViewModel: Request data
    ViewModel->>Repository: Fetch data
    Repository->>Network: API call
    Network-->>Repository: Network error
    Repository-->>ViewModel: Error result
    ViewModel->>ViewModel: Handle error
    ViewModel-->>UI: Show error message
    UI->>UI: Display retry option
```

### Rate Limit Flow
```mermaid
sequenceDiagram
    participant UI
    participant ViewModel
    participant RateLimiter
    
    UI->>ViewModel: Search location (rapid typing)
    ViewModel->>RateLimiter: Check limit
    
    alt Under limit
        RateLimiter-->>ViewModel: Allowed
        ViewModel->>API: Make request
    else Over limit
        RateLimiter-->>ViewModel: Denied
        ViewModel-->>UI: Show "Too many requests"
    end
```

## State Management

### ViewModel State Flow
```
User Action → UI Event → ViewModel → Update State → Recompose UI
```

### Repository State Flow
```
Data Change → Repository Listener → Emit Update → ViewModel → Update UI State
```

## Offline Handling

The app uses local repository implementations for offline support:

```
Network Available:
    UI → ViewModel → FirestoreRepository → Firebase → Update UI

Network Unavailable:
    UI → ViewModel → LocalRepository → In-Memory Storage → Update UI
```

## Concurrency & Threading

```
UI Thread (Main)
    ↓
Compose UI Updates
    ↓
ViewModel (viewModelScope)
    ↓
Coroutines (IO Dispatcher)
    ↓
Repository Operations
    ↓
Firebase/Network (Background)
```

All repository operations are suspend functions executed in coroutine scopes, ensuring non-blocking UI operations.
