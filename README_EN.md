# Shi Jian Ke Ri (时笺恪日)

An Android daily planner app that divides each day into 48 time slots (30 minutes each), helping users manage their time with precision.

Features an iOS 26 Liquid Glass style UI — clean, translucent, and bright.

---

## Features

### Time Slot Management
- 48 time slots per day (00:00 ~ 23:30), one slot every 30 minutes
- Free-form text input for each time slot
- Auto-expanding text field — content is never clipped
- 7 category tags: Work / Study / Exercise / Rest / Social / Dining / Other, each with a distinct color

### Time Lock
- Only the current half-hour slot and future slots are editable
- Past slots are automatically locked as read-only, displaying "Expired"
- Current slot highlighted with a green border and labeled "Now"
- Editable state refreshes automatically every minute

### Calendar View
- Tap the date in the top bar to open a monthly calendar popup
- Each date cell shows: day number + daily title (first 4 chars) + content indicator dot
- Today is highlighted in blue; dates with content show a blue dot
- Navigate between months; tap any date to jump directly to it

### Date Navigation
- Left/right arrows to switch between days
- "Today" button to quickly return to the current date
- View past and future dates (past = read-only, future = editable)

### Copy Plan
- Copy the current day's plan to future N days with one tap
- Quick-select options: 3 / 7 / 14 / 30 days
- Dates that already have plans are automatically skipped
- Shows success count and skipped count after copying

### Plan Templates
- Save the current plan as a named template (e.g., "Weekday Template", "Weekend Template")
- Confirmation dialog before loading a template (prevents accidental overwrites)
- Confirmation dialog before deleting a template
- Manage multiple templates

### Search
- Search across all historical plan content
- 500ms debounce to avoid excessive queries
- Results display date, time, and content

### Statistics Panel
- **Filled X/48**: Number of time slots with content today
- **Task Progress X/48**: Number of time slots that have passed today
- Two separate progress bars in blue and orange

### Time Progress
- Top bar shows real-time "Day elapsed: XX.XX%"
- Updates every second with 2 decimal places, smooth growth
- Floating button in bottom-right shows current time; tap to scroll to the current slot

---

## UI Design

- iOS 26 Liquid Glass style
- Gradient background: light blue → light purple → light orange
- Translucent cards with soft rounded corners
- Transparent status bar and navigation bar
- Clean, bright, and airy

---

## Tech Stack

| Technology | Purpose |
|------------|---------|
| Kotlin | Language |
| Jetpack Compose | Declarative UI framework |
| Material3 | Design component library |
| Room | Local database persistence |
| StateFlow | Reactive state management |
| MVVM | Architecture pattern |
| Coroutines + Flow | Asynchronous data streams |

---

## Project Structure

```
app/src/main/java/com/example/dailyplanner/
├── MainActivity.kt                    # Entry Activity
├── data/
│   ├── PlanItem.kt                    # Room entity (plan item + category + title)
│   ├── PlanTemplate.kt                # Room entity (template)
│   ├── PlanDao.kt                     # DAO interface (CRUD + search + calendar)
│   └── AppDatabase.kt                 # Room database (with Migration)
├── ui/
│   ├── PlanScreen.kt                  # Main UI (all Composable components)
│   │   ├── PlanScreen                 # Main screen
│   │   ├── GlassTopBar                # Top bar (date + progress + action buttons)
│   │   ├── GlassTimeSlotRow           # Time slot row (text input + category)
│   │   ├── CalendarDialog             # Calendar popup
│   │   ├── CopyPlanDialog             # Copy plan popup
│   │   ├── TemplateDialog             # Template management popup
│   │   ├── SearchDialog               # Search popup
│   │   ├── StatsDialog                # Statistics popup
│   │   └── CategoryPickerDialog       # Category picker popup
│   └── theme/
│       ├── Color.kt                   # Color definitions
│       └── Theme.kt                   # Material3 theme + transparent status bar
└── viewmodel/
    └── PlanViewModel.kt               # Business logic ViewModel
```

---

## Architecture

```
┌─────────────┐     ┌─────────────────┐     ┌──────────────┐
│  PlanScreen  │────>│  PlanViewModel  │────>│   Room DAO   │
│  (Compose)   │<────│  (StateFlow)    │<────│  (Flow)      │
└─────────────┘     └─────────────────┘     └──────────────┘
```

- **Unidirectional data flow**: User input → ViewModel → Room → Flow → UI update
- **Input debounce**: 300ms debounce before writing to Room (avoids per-keystroke DB writes)
- **Edit state isolation**: The currently edited slot is not affected by Room Flow updates (prevents cursor jumping)
- **Search debounce**: 500ms debounce for search queries

---

## Database Design

### plan_items Table

| Field | Type | Description |
|-------|------|-------------|
| id | Int | Auto-increment primary key |
| date | String | Date, format "2026-06-21" |
| slotIndex | Int | Time slot index 0~47 |
| content | String | Plan content |
| category | String | Category identifier |
| title | String | Date title |

Unique index: `(date, slotIndex)`

### plan_templates Table

| Field | Type | Description |
|-------|------|-------------|
| id | Int | Auto-increment primary key |
| name | String | Template name |
| slotsJson | String | Plan data in JSON format |

### Database Versions

- v1 → v2: Added `title` field, removed `isCompleted` field (with Migration)

---

## Build & Run

### Requirements

- Android Studio Hedgehog (2023.1) or later
- JDK 17
- Android SDK 34
- Device with Android 8.0 (API 26) or higher

### Using Android Studio

1. Open the `DailyPlanner` folder
2. Wait for Gradle sync to complete
3. Connect an Android device or start an emulator
4. Click Run

### Using Command Line

```bash
# Build Debug APK
gradle assembleDebug

# Sign
zipalign -f -v 4 app/build/outputs/apk/debug/app-debug.apk app-aligned.apk
apksigner sign --ks release-key.jks --ks-pass pass:yourpassword --out app-release.apk app-aligned.apk
```

### First Build Note

The project does not include `local.properties` (SDK path varies per machine). Android Studio generates it automatically on first open. For command line builds, create it manually:

```bash
echo "sdk.dir=/path/to/your/Android/Sdk" > local.properties
```

---

## Installation

1. Download `DailyPlanner-final.apk`
2. Transfer to your phone (USB recommended — QQ may corrupt the file)
3. Tap to install, allow "Unknown sources" if prompted
4. If installation fails, uninstall the old version and clear app data first

---

## Permissions

This app requires **no permissions**:
- No network access
- No storage permissions
- No camera/microphone
- All data stored locally in Room database only

---

## Known Limitations

- Portrait mode only
- No dark mode support (light theme only)
- No data export/import
- No multi-device sync
- Calendar view does not support lunar calendar

---

## Changelog

### v1.0 (2026-06-21)
- Initial release
- 48 time slot management
- Time lock mechanism
- Calendar view
- Copy plan to future N days
- Plan template management
- Search functionality
- Statistics panel
- Category tags
- iOS 26 Liquid Glass style UI

---

## License

This project is for educational and personal use only.
