# GuitarVault

A comprehensive Android application for guitar collectors to track their collections with deep specifications, AI-powered photo capture, and future-ready architecture.

## Features

### 🎸 Collection Management
- **Deep specifications**: Brand, model, year, body type, woods, pickups, electronics, hardware, scale length, neck profile, finish, serial number, strings, and more
- **Custom fields**: User-defined fields for anything not pre-built (text, number, date, boolean, URL)
- **Multiple view modes**: Flat list, grouped by brand, grid gallery
- **Search, filter & sort**: By name, brand, model, serial number, tags, guitar type; sort by date added, name, brand, year, value, or purchase date
- **Status tabs**: Owned, Wishlist, Sold — unified in one collection view, each with a guitar count badge
- **Collection stats**: Total guitars, total value, total invested, total insured — with a warning when coverage falls short of value
- **Spec completeness**: Progress bar on each guitar card showing how complete its specs are

### 📸 Photo Capture + On-Device AI
- CameraX integration for in-app photo capture
- **ML Kit Subject Segmentation** for on-device background removal (magic wand per photo)
- Manual background removal with **undo** support
- Clipboard paste photos stored as base64 in JSON
- Photo types: General, Front, Back, Headstock, Neck, Body, Pickups, Electronics, Hardware, Case, Damage, Repair
- Full-screen photo viewer with pinch-to-zoom and swipe

### 💰 Valuation & Insurance
- Purchase price, date, and source tracking
- Current value with history log (with delete)
- Estimated value, gain/loss calculation
- Insurance info: provider, policy number, coverage type, deductible, policy dates

### 🔧 Condition & Maintenance
- Condition ratings: Mint, Near Mint, Excellent, Very Good, Good, Fair, Poor
- Condition history with notes and issue tracking
- Maintenance log: string changes, setups, repairs, refrets, refinishes, electronics, hardware, cleaning, inspections

### 🎲 Random Spec Challenge (Gamification)
- Random guitar + random unfilled spec — from owned, sold, or wishlist guitars
- Shows the guitar's primary photo (Sold/Wishlist items get a status badge)
- "Look It Up" button opens a Google search tailored to the challenged spec
- Run it as often as you want; reroll any time
- Collection completeness progress bar
- Fill in specs one at a time, gamified

### 📦 Backup & Restore
- **Export as ZIP**: collection data + all photos in one file (file-based and pasted photos alike)
- **Import with a choice**: Merge (combine collections, nothing deleted) or Replace (wipe and load)
- Merge matches guitars by record id or serial number; unmatched records are added; photos, history and maintenance logs are unioned without duplicates
- Confirmation dialog with live guitar counts before any import touches your data
- Auto cloud backup disabled — your serial numbers and valuations never silently sync off-device

### 🔍 Spec Search
- Search Google, Reverb, or eBay for guitar specs
- Opens browser with pre-filled search query

### 🎨 Material Design 3
- Material You dynamic color (Android 12+)
- Dark/light theme

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
| Target SDK | 36 (Android 16) |

## Architecture

```
app/src/main/java/com/guitarvault/app/
├── data/
│   ├── model/          # Data classes: Guitar, WishlistItem, Valuation, etc.
│   ├── storage/        # JsonStorage — file-backed persistence
│   ├── repository/     # GuitarRepository — CRUD operations
│   └── specs/         # SpecLookupService
├── ai/                 # ML Kit Subject Segmentation + isolated service
├── camera/             # CameraX capture manager
├── ui/
│   ├── theme/          # Material 3 colors, typography, theme
│   ├── components/     # Reusable: GuitarCard, PhotoGallery, SpecRow, etc.
│   ├── screens/        # Collection, Detail (tabs), AddEdit, Camera, Legal, etc.
│   └── viewmodel/      # CollectionViewModel
├── navigation/         # NavHost routes
├── util/               # ClipboardImageReader
├── GuitarVaultApp.kt   # Application class
└── MainActivity.kt     # Single-activity entry point
```

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config in local.properties)
./gradlew assembleRelease

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Requires:
- Android Studio or Gradle 8.10+
- JDK 17
- Android SDK 36
- Kotlin 1.9.22+

## License

Proprietary. All rights reserved. See [LICENSE](LICENSE) for details.

## Legal

- [Privacy Policy](docs/PRIVACY_POLICY.md)
- [Terms of Service](docs/TERMS_OF_SERVICE.md)
