# De-GAS — Digital Guitar Asset System

A comprehensive Android application for guitar collectors to track their collections with deep specifications, AI-powered photo capture, and future-ready architecture.

## Features

### 🎸 Collection Management
- **Deep specifications**: Brand, model, year, body type, woods, pickups, electronics, hardware, scale length, neck profile, finish, serial number, strings, and more
- **Custom fields**: User-defined fields for anything not pre-built (text, number, date, boolean, URL)
- **Multiple view modes**: Flat list, grouped by brand, grid gallery — toggle with segmented buttons
- **Search & filter**: By name, brand, model, serial number, tags, guitar type
- **Collection stats**: Total guitars, total value, total invested, total insured

### 📸 Photo Capture + On-Device AI
- CameraX integration for in-app photo capture
- **ML Kit Subject Segmentation** for on-device background removal
- No cloud calls — all AI processing happens locally
- Requires Android 12+ (API 31+)
- Photo types: General, Front, Back, Headstock, Neck, Body, Pickups, Electronics, Hardware, Case, Damage, Repair
- Primary photo designation, "AI processed" badges

### 💰 Valuation & Insurance
- Purchase price, date, and source tracking
- Current value with history log
- Estimated value
- Gain/loss calculation
- Insurance info: provider, policy number, coverage type, deductible, policy dates

### 🔧 Condition & Maintenance
- Condition ratings: Mint, Near Mint, Excellent, Very Good, Good, Fair, Poor
- Condition history with notes and issue tracking
- Maintenance log: string changes, setups, repairs, refrets, refinishes, electronics, hardware, cleaning, inspections
- Cost and technician tracking per maintenance entry

### 📋 Wishlist
- Track guitars you're hunting for
- Priority levels: Low, Medium, High, Grail
- Target price, specific specs required, notes, tags
- One-tap "Acquired" — promotes wishlist item to collection with purchase price

### 🎨 Material Design 3
- Material You dynamic color (Android 12+)
- Dark/light theme
- Segmented buttons, cards, tabs, floating action buttons

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Storage | JSON file-backed (kotlinx.serialization) |
| Camera | CameraX |
| AI | ML Kit Subject Segmentation (on-device, via Google Play services) |
| Image loading | Coil |
| Navigation | Navigation Compose |
| Min SDK | 31 (Android 12) |
| Target SDK | 34 (Android 14) |

## Architecture

```
app/src/main/java/com/guitarvault/app/
├── data/
│   ├── model/          # Data classes: Guitar, WishlistItem, Valuation, etc.
│   ├── storage/        # JsonStorage — file-backed persistence
│   └── repository/     # GuitarRepository — CRUD operations
├── ai/                 # ML Kit Subject Segmentation background remover
├── camera/             # CameraX capture + AI processing pipeline
├── ui/
│   ├── theme/          # Material 3 colors, typography, theme
│   ├── components/     # Reusable: GuitarCard, PhotoGallery, SpecRow, etc.
│   ├── screens/        # Collection, Detail (tabs), AddEdit, Wishlist
│   └── viewmodel/      # CollectionViewModel
├── navigation/         # NavHost routes
├── DeGASApp.kt         # Application class
└── MainActivity.kt     # Single-activity entry point
```

## Data Persistence

All data is stored as JSON in `collection.json` in the app's internal storage:
- Guitars with full specs, photos, condition history, maintenance log, valuation, insurance, custom fields
- Wishlist items
- Versioned format for future migrations

Photos are stored as PNG files in `photos/` directory (PNG preserves transparency from background removal).

## Future AI Features (Architected)

The app is designed to accommodate future AI capabilities:
- **Guitar identification**: Auto-detect brand/model from photo
- **Condition assessment**: AI-powered damage detection
- **Price estimation**: ML-based value predictions from market data
- **Similar guitar discovery**: Visual similarity search
- The `BackgroundRemover` interface allows swapping AI implementations

## Building

```bash
# Open in Android Studio or build from command line:
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

Requires:
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- Kotlin 1.9.22+

## License

Private project. All rights reserved.
