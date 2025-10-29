# Architecture Documentation Index

This directory contains comprehensive architecture documentation for the Map'In application.

## 📚 Documentation Files

### 0. [ARCHITECTURE_SUMMARY.md](../ARCHITECTURE_SUMMARY.md)
**🎯 Complete System Architecture Diagram**

This document provides:
- **One comprehensive diagram** showing the entire system
- All layers in a single view (UI → ViewModel → Repository → Data Sources)
- Complete data flow patterns
- User interaction flows
- Technology stack overview
- Color-coded architecture layers

**Best for:** Getting a complete overview of the entire system in one place. **Start here!**

### 1. [ARCHITECTURE.md](../ARCHITECTURE.md)
**High-Level Architecture Overview**

This document provides:
- Complete architecture diagram showing all layers and components
- Layer-by-layer descriptions (Presentation, ViewModel, Model)
- External services integration (Firebase, Mapbox, Nominatim)
- Architecture patterns (MVVM, Repository Pattern)
- Data flow explanations
- Technology stack details
- Testing strategy
- Security considerations

**Best for:** Understanding the overall system design and how components interact.

### 2. [COMPONENT_ARCHITECTURE.md](../COMPONENT_ARCHITECTURE.md)
**Detailed Component Structure**

This document provides:
- Detailed component diagram mapping to actual source files
- Complete package structure with file paths
- Component responsibilities and relationships
- Module dependencies
- Testing structure
- Build configuration

**Best for:** Navigating the codebase and understanding where each component lives.

### 3. [USER_FLOWS.md](../USER_FLOWS.md)
**User Flows & Sequence Diagrams**

This document provides:
- Sequence diagrams for key user flows:
  - User authentication
  - Event creation
  - Memory creation
  - Map display & event loading
  - Location search
  - User profile updates
  - Event participation
- Error handling flows
- State management patterns
- Concurrency & threading model

**Best for:** Understanding how data flows through the system during user interactions.

### 4. [QUICK_REFERENCE.md](../QUICK_REFERENCE.md)
**Quick Reference Guide**

This document provides:
- Quick lookup tables for key classes
- Project structure summary
- Common patterns with code examples
- Build & run commands
- Navigation flow diagram

**Best for:** Quick lookups and code examples for developers.

## 🎯 Quick Navigation Guide

**If you want to...**

- **See the entire system at once** → Start with [ARCHITECTURE_SUMMARY.md](../ARCHITECTURE_SUMMARY.md) ⭐
- **Understand the overall architecture** → Read [ARCHITECTURE.md](../ARCHITECTURE.md)
- **Find a specific component/file** → Check [COMPONENT_ARCHITECTURE.md](../COMPONENT_ARCHITECTURE.md)
- **Understand user interactions** → Review [USER_FLOWS.md](../USER_FLOWS.md)
- **Add a new feature** → Read all three documents in order
- **Debug an issue** → Check [USER_FLOWS.md](../USER_FLOWS.md) for sequence diagrams
- **Onboard to the project** → Start with README.md, then [ARCHITECTURE.md](../ARCHITECTURE.md)

## 🏗️ Architecture Overview

Map'In follows the **MVVM (Model-View-ViewModel)** architecture pattern:

```
┌─────────────────────────────────────────────────────┐
│                   Presentation Layer                 │
│  (Jetpack Compose UI - Screens, Dialogs, Components)│
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│                    ViewModel Layer                   │
│     (State Management, Business Logic, UI Events)   │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│                  Repository Layer                    │
│        (Data Access Abstraction, Interfaces)        │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│                    Data Sources                      │
│   (Firebase Firestore, Firebase Storage, APIs)      │
└─────────────────────────────────────────────────────┘
```

## 🔑 Key Components

### Screens
- **SignInScreen**: User authentication
- **MapScreen**: Interactive map with events and memories
- **ProfileScreen**: User profile management
- **MemoryFormScreen**: Memory creation

### ViewModels
- **SignInViewModel**: Authentication state
- **MapScreenViewModel**: Map interactions and data
- **ProfileViewModel**: Profile data management
- **EventViewModel**: Event creation logic
- **LocationViewModel**: Location search

### Repositories
- **EventRepository**: Event data management
- **MemoryRepository**: Memory data management
- **UserProfileRepository**: User profile data
- **LocationRepository**: Geocoding services

### Data Models
- **Event**: Event information
- **Memory**: User memories/photos
- **UserProfile**: User data
- **Location**: Geographic coordinates

## 🛠️ Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Material 3
- **Architecture**: MVVM
- **Navigation**: Jetpack Navigation Compose
- **Async**: Kotlin Coroutines + Flow
- **Backend**: Firebase (Auth, Firestore, Storage)
- **Maps**: Mapbox + Google Maps
- **Geocoding**: Nominatim API
- **Networking**: OkHttp
- **Testing**: JUnit, Mockito, Kaspresso, Robolectric
- **Build**: Gradle with Kotlin DSL

---

**Last Updated**: 2025-10-29  
**Version**: 1.0.0
