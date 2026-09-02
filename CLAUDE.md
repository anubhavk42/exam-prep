# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build commands

No `gradlew` wrapper exists. Use the cached Gradle distribution directly:

```sh
# Full debug APK (output: app/build/outputs/apk/debug/app-debug.apk)
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ~/.gradle/wrapper/dists/gradle-9.3.1-bin/*/gradle-9.3.1/bin/gradle assembleDebug --no-daemon

# Fast compile-only check (no APK)
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ~/.gradle/wrapper/dists/gradle-9.3.1-bin/*/gradle-9.3.1/bin/gradle :app:compileDebugKotlin --no-daemon
```

Install and test on device:
```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.anubhav.diprep -c android.intent.category.LAUNCHER 1
adb exec-out screencap -p > /tmp/screen.png        # screenshot
adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml /tmp/ui.xml  # UI bounds
```

Always run `assembleDebug` (not just `compileDebugKotlin`) after touching Room entities, DAOs, or `AppDatabase.kt` — KSP must regenerate code.

## Architecture

```
MainActivity
  └── AppNavGraph (Compose Navigation)
        ├── OnboardingScreen (first-launch, 3-step)
        └── MainScreen (Scaffold + bottom nav, 4 tabs)
              ├── HOME  → HomeScreen       ← HomeViewModel
              ├── SUBJECTS → SubjectsScreen ← PrepViewModel (shared)
              ├── GOALS → TimetableScreen  ← TimetableViewModel
              └── STATS → StatsScreen
                    └── (embeds WeeklyScreen) ← WeeklyViewModel / StatsViewModel
        (modal routes)
        ├── LogScoreScreen  ← LogScoreViewModel
        ├── QuizScreen      ← (no dedicated VM, uses PrepViewModel callback)
        └── SettingsScreen  ← SettingsViewModel
```

**Data layer** (`data/`):
- `AppDatabase` (Room, version 2) — entities: `ScoreEntry`, `TaskLog`, `TimetableSlot`, `TimetableCompletion`
- `AppDao` — all queries; timetable queries return `Flow<List<T>>`; write ops are `suspend`
- `PreferencesManager` — DataStore; exposes `userProfileFlow: Flow<UserProfile>`; `UserProfile` holds exam settings, home section order/hidden list, and wellness toggles
- `AppRepository` — single source of truth; wraps Dao + PreferencesManager; all DB writes use `withContext(Dispatchers.IO)`

**ViewModel → UI contract**: every StateFlow uses `SharingStarted.WhileSubscribed(5000)` and is collected with `collectAsStateWithLifecycle()`, never `collectAsState()`.

**Navigation tab state** is held in `PrepViewModel.uiState` (`selectedTab: MainTab`). `PrepViewModel` is scoped to `MainActivity` and passed down; all other VMs use `viewModel()` at the composable call site.

## Theme

`Theme.kt` always uses `DarkPremiumColorScheme` (no light theme, no dynamic color). Color token mapping that matters most:

| Semantic name | Hex | Theme slot |
|---|---|---|
| `DarkBackground` | `#141110` | `background`, `surfaceContainerLowest` |
| `DarkSurface` | `#1E1A17` | `surface`, `surfaceContainer` |
| `DarkBorder` | `#2A2422` | `outline`, `surfaceVariant` |
| `Gold` | `#E8B869` | `primary` |
| `SuccessGreen` | `#5DCAA5` | — (direct import) |
| `DangerCoral` | `#E8896A` | `error` |

**Critical**: `cs.surfaceContainerLowest` and `SleekSurfaceContainerLowest` both map to `DarkBackground` (#141110) — the same as the page background. **Never use either as a card background.** Use `cs.surface` / `DarkSurface` (#1E1A17) for all cards and elevated surfaces.

Legacy `Sleek*` color aliases in `Color.kt` exist for backwards compatibility. New code must use `MaterialTheme.colorScheme` or the semantic names (`DarkSurface`, `Gold`, `SuccessGreen`, etc.) directly.

## Compose pitfalls in this codebase

**`matchParentSize()` overrides `width()`** — In a `Box`, `matchParentSize()` sets both dimensions to the parent's size, ignoring any `.width(Xdp)` modifier before it in the chain. For a colored left-edge accent bar, use `Row + IntrinsicSize.Min` instead:

```kotlin
Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)...) {
    Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(accent))
    Column(modifier = Modifier.weight(1f)...) { ... }
}
```

**Room migrations** — bump `AppDatabase.version` and add a `MIGRATION_X_Y` object to `addMigrations(...)` whenever an entity changes. Never use `fallbackToDestructiveMigration()` — users have live data.

**Timetable completion toggle** — `AppRepository.toggleTimetableCompletion()` reads the existing row first (`getCompletion()`) then inserts or deletes. Both ops must be in the same `withContext(Dispatchers.IO)` block to avoid threading issues.

---

## What this app is
An offline, battery-efficient exam preparation tracker for students
preparing for any competitive exam (not limited to pharmacy/Drug
Inspector — supports any exam via custom entry). Package name:
com.anubhav.diprep

## Core principles — non-negotiable
- 100% offline. Zero network calls, zero internet permission.
- Zero background services. No WorkManager, no repeating alarms.
- Only one-time exact alarms allowed (for the daily reminder),
  rescheduled fresh each time the app opens.
- Battery-efficient: collectAsStateWithLifecycle() everywhere,
  all DB ops on Dispatchers.IO, MVVM + Repository pattern.
- Jetpack Compose only, Material 3, no XML layouts.
- Room database + DataStore Preferences for all storage.

## Existing screens (already built)
- Onboarding: name, exam stream, exam date, subject list setup
- Home: greeting, countdown to exam, streak, personal best,
  today's goals progress, log score CTA, daily quote
- Subjects: list of subjects with % mastery, filterable by status
- Log Score: enter marks obtained/total for a subject, shows live %
- Weekly: week-by-week average score trends, filterable by subject
- Stats: 70-day consistency heatmap + subject strength insights
- Settings: name, exam date, exam stream, subject list, reminders

## Features currently being added
1. Custom Timetable (replaces the old fixed "Goals" tab with
   4 hardcoded tasks). Students build their own weekly schedule —
   day of week + start time + end time + label (subject or task
   name). Home screen shows "What's next" pulled from today's
   schedule based on current time.
   - Room tables already added: TimetableSlot, TimetableCompletion
   - DAO functions already added in AppDao.kt

2. Revision Flashcards — simple flip cards per subject,
   student types their own front/back text, stored locally.

3. Mock Test Timer — set a duration, countdown timer, alert
   when time's up, then prompts to log score. Runs only in
   foreground (LaunchedEffect + delay), stops completely when
   app is backgrounded. No background service.

4. Weak Topic Banner — shows on Home automatically when a
   subject's average score is below 50% OR hasn't been studied
   in 5+ days. Calculated only when the app opens, not via
   polling.

## Wellness habits (recently changed)
- "Walking" task renamed to "Exercise" everywhere (already done
  in code — TaskLog.walkDone renamed to exerciseDone).
- "Multivitamin" is now OPTIONAL — a toggle in Settings, OFF by
  default (vitaminReminderEnabled in UserProfile, default false).
  Only shows in the daily checklist if the student turns it on.
- "Exercise" reminder is ON by default (exerciseReminderEnabled,
  default true) but still a Settings toggle.
- These are separate from Timetable slots — they're optional
  general wellness habits, not tied to specific subjects.
- Both toggle functions already added to PreferencesManager.kt 
  and AppRepository.kt: updateVitaminReminderEnabled, 
  updateExerciseReminderEnabled

## Exam selection (recently changed)
No longer limited to pharmacy exams. Onboarding and Settings show:
- Quick preset chips: UPSC, SSC, Banking, Pharmacy/Drug Inspector,
  NEET/Medical, State PCS
- A free-text field to type any custom exam name
- Subject list auto-populates from preset but is fully editable
  (add/remove subject chips) regardless of which exam was chosen

## Home screen personalization (recently changed)
In Settings, students can customize their Home screen:
- "Greeting" and "Exam countdown" are ALWAYS shown (not toggleable)
- Everything else has a show/hide toggle switch:
  - What's next (from timetable)
  - Weak topic alert
  - Streak & personal best
  - Today's goals progress
  - Log score button
  - Daily quote
- Each toggleable section also has a drag handle to reorder it
- Agreed default order (top to bottom): Greeting, Countdown,
  What's next, Weak topic alert, Streak+PB, Today's goals,
  Log score button, Quote

## Notification behavior (recently decided)
The app does NOT control the phone's ringer/silent/vibrate mode
directly (that would require special "Do Not Disturb access"
permission, which we're avoiding for privacy/trust reasons).
Instead: when a Timetable slot is about to start, fire a normal
notification saying "Time to silence your phone — [Subject]
starts now" using the existing NotificationHelper pattern
(one-time exact alarm, same as the Multivitamin reminder).

## Design system — "Dark Premium" (chosen direction)
Full dark theme with warm gold accent. THIS IS THE FINAL DESIGN 
DECISION — apply it to every screen consistently.

COLORS:
- Background: #141110
- Card/surface: #1E1A17
- Border: #2A2422
- Subtle divider: #211C19
- Accent (gold): #E8B869
- Text primary: #F0ECE6
- Text secondary/muted: #8A8380
- Text on accent: #141110
- Success (green): #5DCAA5
- Warning (amber, same as accent): #E8B869
- Danger/weak (coral): #E8896A
- Weak-alert card bg: #251815
- Weak-alert card border: #4A2E26
- Track/disabled: #3A332E

TYPOGRAPHY (see CORRECTED section below — these early values are wrong):
- Screen title: 15sp SemiBold
- Subtitle: 10sp Normal, muted
- Countdown hero number: 44sp Bold, gold
- Countdown label: 9sp uppercase, muted
- Card label: 8-9sp Bold uppercase, gold
- Card body: 11sp Medium, text primary
- Stat numbers: 16-20sp Bold

COMPONENTS:
- Cards: bg #1E1A17, 1dp border #2A2422, 12dp corner radius
- Weak alert cards: bg #251815, border #4A2E26
- Filter chips: unselected = outlined #3A332E border, muted text.
  Selected = filled gold bg, dark text, bold
- Progress bars: 4dp height, track #2A2422, fill color by
  performance (green >=75%, gold >=50%, coral <50%)
- Task checkboxes: 18dp circle, checked = green fill + dark check
- Bottom nav: bg #141110, top border #2A2422, selected = gold
  icon with 32dp circular tinted bg (primary 15% alpha),
  unselected = #6B645F plain icon, no background
- Toggle switches: ON = gold bg, OFF = #2A2422 bg
- List row dividers: 1dp #211C19
- No gradients, no shadows — flat surfaces differentiated by
  1dp borders only
- Subject rows: dark card (#1E1A17) with a 4dp colored left
  accent bar (Row + IntrinsicSize.Min pattern — see Compose
  pitfalls above). Green=mastered, gold=in progress, coral=needs work.

IMPORTANT: If Color.kt/Theme.kt in ui/theme package do not yet 
match these exact values, update them FIRST before building any 
new screen, so every screen (old and new) stays visually consistent.

## Terminal-only workflow preference
The developer (Anubhav) prefers all code changes to be shown as
clear diffs before applying, and wants to verify builds compile
after every change. Always confirm before large multi-file changes.

## Exact screen layouts (approved designs — follow precisely)

### Home screen — section order (top to bottom, confirmed)
1. Greeting: "Welcome back, [name]" small muted text + today's date bold, 
   settings gear icon top-right
2. Countdown hero: dark card, huge gold number (days remaining), 
   "DAYS REMAINING" label below, weeks/days subtext, exam date + 
   confirmed/rumoured status at bottom in small amber text
3. What's next: small card showing the next timetable slot for today 
   (time range + label), only shown if a slot exists
4. Weak topic alert: shown only if a subject is below 50% average or 
   not studied in 5+ days. Card with warning icon, subject name, 
   reason (e.g. "30% average · not studied in 5 days")
5. Streak + Personal best: two equal-width cards side by side, 
   emoji + number + label each
6. Today's goals: single card, "Today's goals" label + "X of Y" count, 
   progress bar below
7. Log score button: full-width gold button
8. Daily quote: card with italic quote text + author, rotates daily

### Subjects screen layout
- Header: "Your subjects" title + settings icon, subtitle below
- Horizontal filter chips: All / Strong / In progress / Needs work
- Each subject as a row: 4dp colored left accent bar (green=mastered, 
  gold=in progress, coral=needs work) attached to a card. Inside: 
  subject name + percentage on top row, small status badge chip 
  below, progress bar below that, "Quick quiz →" link bottom-right

### Settings screen — Home customization section
- List of toggle rows, each with: drag handle icon (left), 
  section name + description (middle), toggle switch or "Fixed" 
  label (right)
- "Greeting" and "Exam countdown" are marked "Fixed" (not toggleable)
- All other sections (What's next, Weak topic alert, Streak & best, 
  Today's goals, Log score button, Daily quote) have toggle switches 
  and can be reordered via drag handle

### Settings screen — Exam & subjects section
- Exam name: shows quick preset chips (UPSC, SSC, Banking, 
  Pharmacy/Drug Inspector, NEET/Medical, State PCS) OR a free-text 
  field to type custom exam name
- Subject list: editable chips with X to remove, "+ Add subject" 
  chip to add new ones

### Onboarding flow (3 steps, progress dots at top)
Step 1: Name input, target emoji, "Welcome!" heading
Step 2: Exam stream selection — same preset chips + free text pattern 
  as Settings, subjects auto-populate from preset but are editable
Step 3: Exam date picker + subject list preview (editable chips)

### Weekly/Stats screen
- Header: "Weekly progress" title, target date + days-left subtitle
- Segmented control (not two separate buttons): "Weekly trends" | 
  "Heatmap & Insights"
- Weekly trends tab: subject filter chips, then week cards (label, 
  avg %, progress bar, test count) for last 6 weeks
- Heatmap tab: 7-column grid (M-S headers), 70 cells (10 weeks), 
  cell intensity based on task completion, today's cell has accent 
  border outline. Legend below showing 4 intensity levels. Subject 
  strength list below using the same left-accent-bar card pattern 
  as Subjects screen

## CORRECTED Typography (real device sizes — supersedes earlier sp values)

IMPORTANT: Any earlier typography sizes in this file (8sp, 9sp, 
10sp, 11sp) were mockup-scale placeholders and are WRONG for actual 
Android screens. Use these corrected real-device sizes instead:

- Screen title: 22sp, FontWeight.Bold
- Section label (uppercase small headers): 12sp, FontWeight.SemiBold
- Subtitle/description: 14sp, FontWeight.Normal
- Countdown hero number: 64sp, FontWeight.Bold
- Countdown label ("DAYS REMAINING"): 13sp, uppercase, letterSpacing 0.1sp
- Card title/label: 12sp, FontWeight.Bold, uppercase
- Card body text: 15sp, FontWeight.Medium
- Stat numbers (streak, personal best): 24sp, FontWeight.Bold
- Stat label: 12sp, FontWeight.Medium
- Button text: 15sp, FontWeight.SemiBold
- Badge/chip text: 12sp, FontWeight.SemiBold
- Bottom nav label: 11sp, FontWeight.Medium

FONT FAMILY: Use Roboto explicitly throughout the app (Android's 
default system font). In Theme.kt, define the Typography using 
FontFamily.Default (which resolves to Roboto on Android) or 
explicitly import androidx.compose.ui.text.font.FontFamily and 
set fontFamily = FontFamily.SansSerif for all TextStyle definitions 
in the Typography object, ensuring consistency across every screen.

## Top-gap bug (must be fixed on EVERY screen without exception)

There is a persistent gap of empty space at the top of every screen 
before content starts. Root cause: Scaffold's innerPadding is being 
applied AND the screen's own Column is adding additional top padding 
on top of it, OR a stray Spacer exists above the first element.

Fix required on EVERY screen file (Home, Subjects, Goals/Timetable, 
Weekly, Stats, Settings, Onboarding):
1. Find the root Column/Box of each screen
2. Ensure top padding is at most 16.dp total — not innerPadding 
   PLUS additional padding stacked on top
3. Remove any Spacer(height = Xdp) that appears as the very first 
   child before the header/title text
4. Verify by checking: does the screen's title/header appear within 
   ~60dp of the status bar, or is there a large gap of blank space 
   first? If gap exists, padding is still doubled somewhere.

## New features to build (batch 2)

### 1. Mistake Log / Error Journal
New Room entity MistakeEntry: id, dateISO, subject, questionText, 
mistakeType (enum: "Concept Gap", "Silly Mistake", "Time Pressure", 
"Other"), notes, createdAt. New screen accessible from Subjects 
screen (small "Log a mistake" button per subject, or a dedicated 
tab/section) — simple form: subject dropdown, question/topic text 
field, mistake type chips, optional notes. List view shows all 
mistakes grouped by subject, most recent first, filterable by type.

### 2. Syllabus Checklist per Subject
New Room entity SyllabusTopic: id, subject, topicName, isCovered 
(boolean), createdAt. On Subjects screen, tapping a subject opens 
a detail view showing a checklist of topics (student adds their 
own topic names via "+ Add topic"), each with a checkbox for 
"covered". Show "X of Y topics covered" as a simple progress 
indicator, separate from the existing % mastery (which comes from 
test scores) — this tracks coverage, not performance.

### 3. Previous Year Question Tagging
New Room entity PYQTag: id, subject, questionText, yearAppeared 
(optional string, e.g. "2023", "2021, 2019"), notes, createdAt. 
Simple add form (subject, question text, year(s) if known) and a 
list view filterable by subject, sorted with most-repeated 
questions (multiple years tagged) highlighted at top.

### 4. Milestone Celebrations
Extend existing confetti system (already used in Goals screen for 
daily completion). Add milestone checks (calculated on app open, 
not polled) for: 10/25/50/100 tests logged, first subject reaching 
90%+ mastery, 7/30/100-day streak. When a new milestone is crossed, 
show confetti + a small celebratory toast/snackbar message (e.g. 
"🎉 50 tests logged — incredible consistency!"). Store 
lastCelebratedMilestones in DataStore so the same milestone doesn't 
re-trigger every app open.

### 5. Progress Comparison (This week vs Last week)
On Weekly screen, add a small comparison card above or near "This 
week": calculate this week's average % vs last week's average %, 
show as "+12% vs last week" (green, up arrow) or "-5% vs last week" 
(coral, down arrow) or "First week — no comparison yet". Pure 
calculation from existing ScoreEntry data, no new storage needed.

### 6. End-of-Day Recap
New small card/section, shown on Home screen ONLY in the evening 
(e.g. after 6 PM local time, calculated from LocalTime.now()) OR 
accessible via a manual "View today's recap" button. Summarizes: 
hours studied today (from timetable slot completions), subjects 
tested today (from ScoreEntry where dateISO = today), average score 
today if any tests logged, tasks completed (X of Y). Pure 
aggregation of existing data, no new storage.

### 7. Visual Syllabus Ring
On Home screen (as an additional optional toggleable section in 
Settings' Home Screen customization list) OR on Subjects screen 
header — a single circular progress ring (use Canvas API, arc 
drawing) showing overall syllabus completion: average of all 
subjects' % mastery combined into one number, e.g. "68% overall". 
Ring fills proportionally, gold color, with the percentage number 
centered inside it.

### 8. Focus Session Timer (Pomodoro-style)
New screen/dialog accessible from Goals/Timetable screen (e.g. tap 
a timetable slot to "Start focus session" or a standalone button). 
Simple timer: default 25 min focus / 5 min break, but let user 
adjust duration. Uses LaunchedEffect + delay() in Compose — runs 
ONLY while the screen is in foreground, pauses/stops if app is 
backgrounded (no background service, no WorkManager — consistent 
with battery-safe architecture). Shows countdown, a pause/resume 
button, and a completion state with a simple sound/vibration (use 
existing NotificationCompat pattern if a notification is wanted 
when timer completes while phone might be locked). No persistent 
data storage needed — this is a session tool, not tracked history 
(though could optionally log completed focus sessions count to 
DataStore for a simple stat later).

All new database entities go through a new Room migration 
(version bump), following the same pattern as the Timetable 
migration (MIGRATION_1_2 in AppDatabase.kt) — add MIGRATION_2_3 
this time, creating the new tables without touching existing data.

### 9. Daily Mood Check-in
New Room entity MoodEntry: id, dateISO (unique per day), mood 
(enum: "EXCITED", "NEUTRAL", "LOW"), createdAt.

Behavior: On app open (in MainActivity or a top-level LaunchedEffect 
in MainScreen), check DataStore key "last_mood_prompt_date" against 
today's date. If different (or not set), show a dialog:
  Title: "How are you feeling today?"
  Three large tappable options in a row, each with an emoji + label:
    😄 Excited   😐 Neutral   😔 Low
  A "Skip" text button below to dismiss without answering.

On selecting an option: save MoodEntry(dateISO=today, mood=selected) 
to Room, update "last_mood_prompt_date" in DataStore to today, close 
dialog. On Skip: still update "last_mood_prompt_date" to today (so 
it doesn't ask again same day) but don't save a MoodEntry.

Style the dialog using the Dark Premium theme — dark card, gold 
accent on the selected/hovered option, matches existing AlertDialog 
patterns already used elsewhere (e.g. Add Time Slot dialog in 
TimetableScreen.kt).

Add to the same Room migration (MIGRATION_2_3) as the other batch 2 
features.

## Subjects screen redesign (approved — replaces earlier left-accent-bar layout)

Layout: 2-column grid (LazyVerticalGrid, GridCells.Fixed(2), 8dp 
spacing between cells).

Each subject cell:
- Card: dark surface background (#1E1A17), 1dp border (#2A2422), 
  12dp corner radius, padding 10dp, content centered vertically
- Circular progress ring (52dp diameter): draw with Canvas API, 
  arc from -90 degrees, sweep angle = (percentage/100)*360, stroke 
  width ~5dp, track color #2A2422, fill color = status color 
  (green #5DCAA5 if >=75%, gold #E8B869 if >=50%, coral #E8896A 
  if <50%). Percentage number centered inside the ring, 12sp Bold, 
  text primary color.
- Subject name below ring: 10sp SemiBold, text primary, centered, 
  max 2 lines
- Small status badge chip below name: "Mastered" / "In Progress" / 
  "Needs Work", 7sp, colored text on a subtly tinted background 
  matching the status color

Header stays the same: "Your subjects" title + settings icon, 
subtitle below, filter chips row (All/Strong/In Progress/Needs Work) 
above the grid.

## Bug fixes: Light mode issues

### Bug 1: Status bar icons invisible in Light mode
When themeMode = "LIGHT", the system status bar (time, wifi, signal, 
notification icons) becomes invisible — only the battery indicator 
shows. This happens because the status bar icons are still set to 
light/white color, which is invisible against a light background.

Fix: In MainActivity.kt (or wherever the Activity's window/status 
bar is configured), use WindowInsetsControllerCompat to dynamically 
set isAppearanceLightStatusBars based on the current theme:
- When themeMode resolves to LIGHT: isAppearanceLightStatusBars = true 
  (makes status bar icons DARK, visible on light background)
- When themeMode resolves to DARK: isAppearanceLightStatusBars = false 
  (keeps status bar icons LIGHT/white, visible on dark background)

This must react to theme changes at runtime (when user switches in 
Settings), not just be set once at app launch — use a LaunchedEffect 
or DisposableEffect tied to the current theme state in the root 
Composable (likely in the main Activity's setContent or a top-level 
theme wrapper) that updates the WindowInsetsController whenever the 
resolved theme (light vs dark) changes.

### Bug 2: Stats screen broken/not working in Light mode
The Stats screen (Weekly Progress, Heatmap & Insights) does not work 
correctly when themeMode = LIGHT. Likely cause: custom colors used 
specifically on this screen (heatmap cell intensity colors, weak-alert 
card background/border, track colors) were only ever defined as fixed 
dark-mode hex values (e.g. hardcoded #1E1A17, #251815 directly in 
StatsScreen.kt) rather than being part of the light/dark ColorScheme 
that switches automatically.

Fix: Audit StatsScreen.kt (and any other screen with custom/extended 
colors beyond the standard Material3 ColorScheme roles) for hardcoded 
color values. Move these into a proper extended color system — either:
(a) Add them as additional custom colors in the lightColorScheme() 
and darkColorScheme() equivalents (Compose doesn't support custom 
roles directly in ColorScheme, so create a small ExtendedColors data 
class with light and dark variants, provided via CompositionLocal, 
similar to Material3 extended color patterns), or
(b) At minimum, define a light-mode equivalent for every hardcoded 
dark color currently used, and select between them based on 
isSystemInDarkTheme() or the resolved app theme state, exactly the 
same way MaterialTheme.colorScheme already switches.

Apply this same audit to any OTHER screen with hardcoded custom 
colors (heatmap cells, weak-alert banners, subject ring colors, etc.) 
— not just Stats — since the same root cause likely affects multiple 
screens.

## Bug fix: Onboarding exam date step doesn't allow proper selection

During onboarding Step 3 ("When is your exam?"), the exam date shows 
a predefined/hardcoded value (2026-12-20) but the user cannot 
properly change it to their actual exam date. This defeats the 
purpose of the onboarding step entirely.

Expected behavior:
- The date field should display the default (2026-12-20) as a 
  starting placeholder, clearly editable
- Tapping the date field or a "Change" button MUST open a proper 
  Material 3 DatePickerDialog (or DatePicker composable)
- User selects any date they want (their actual rumoured or 
  confirmed exam date)
- Selected date is immediately reflected in the UI on that same 
  onboarding screen (not just saved silently)
- Selected date is saved to DataStore via 
  repository.saveProfile(...) or repository.updateExamDate(...) 
  when onboarding completes ("Start my prep" button)
- The exam_date_confirmed
cat >> ~/Downloads/exam-prep/CLAUDE.md << 'EOF'

## Remove Quick Quiz feature entirely

The "Quick Quiz" button/link on the Subjects screen must be removed 
completely. It was a leftover placeholder from early design mockups 
and should never have generated any interactive question/quiz 
behavior — this app does not include a quiz feature.

The Subjects screen should ONLY show, per subject:
- The circular progress ring / percentage, calculated purely as the 
  average of ScoreEntry.percentage values logged for that subject 
  via the Log Score screen
- Subject name
- Status badge (Mastered / In Progress / Needs Work) based on that 
  same percentage
- Test count (how many scores have been logged for that subject)

Remove any "Quick Quiz" text, button, click handler, or navigation 
route tied to it. If Claude Code previously built an actual quiz 
question/answer flow triggered by this button, delete that code 
entirely — including any related Composables, ViewModel functions, 
or navigation destinations that only exist to support the quiz 
feature. Do not leave orphaned/unused quiz-related files behind.

## Remove Quick Quiz feature entirely

The "Quick Quiz" button/link on the Subjects screen must be removed 
completely. It was a leftover placeholder from early design mockups 
and should never have generated any interactive question/quiz 
behavior — this app does not include a quiz feature.

The Subjects screen should ONLY show, per subject:
- The circular progress ring / percentage, calculated purely as the 
  average of ScoreEntry.percentage values logged for that subject 
  via the Log Score screen
- Subject name
- Status badge (Mastered / In Progress / Needs Work) based on that 
  same percentage
- Test count (how many scores have been logged for that subject)

Remove any "Quick Quiz" text, button, click handler, or navigation 
route tied to it. If Claude Code previously built an actual quiz 
question/answer flow triggered by this button, delete that code 
entirely — including any related Composables, ViewModel functions, 
or navigation destinations that only exist to support the quiz 
feature. Do not leave orphaned/unused quiz-related files behind.

## Bug fix: Onboarding skips the exam date step entirely

Confirmed bug: the exam date step (Step 3, "When is your exam?") in 
OnboardingScreen.kt is being skipped entirely for the user during 
onboarding — they never see this screen at all when going through 
the flow.

Expected onboarding flow (3 steps, in order, none skippable):
1. Name entry ("Welcome! Your first name")
2. Exam stream selection (presets + custom text + subject list)
3. Exam date selection ("When is your exam?" with date picker, 
   defaulting to 2026-12-20, editable)

Only after completing Step 3 should "Start my prep" be available 
to finish onboarding and navigate to Home.

Investigate the root cause — likely causes to check:
- The step/page navigation logic (e.g. a pager or step index state) 
  may be incrementing by 2 instead of 1 somewhere, or the "Next" 
  button on Step 2 may be wired to skip directly past Step 3
- Step 3 might be conditionally rendered behind a condition that's 
  incorrectly false
- The onboarding completion (repository.saveProfile call) might be 
  triggered from Step 2's "Next" button instead of Step 3's "Start 
  my prep" button, causing onboarding to end early

Show me the current onboarding navigation/step logic in full before 
fixing, so the actual cause is confirmed rather than guessed.

## Bug fix: Onboarding exam date step doesn't allow proper selection

During onboarding Step 3 ("When is your exam?"), the exam date shows 
a predefined/hardcoded value (2026-12-20) but the user cannot 
properly change it to their actual exam date. This defeats the 
purpose of the onboarding step entirely.

Expected behavior:
- The date field should display the default (2026-12-20) as a 
  starting placeholder, clearly editable
- Tapping the date field or a "Change" button MUST open a proper 
  Material 3 DatePickerDialog (or DatePicker composable)
- User selects any date they want (their actual rumoured or 
  confirmed exam date)
- Selected date is immediately reflected in the UI on that same 
  onboarding screen (not just saved silently)
- Selected date is saved to DataStore via 
  repository.saveProfile(...) or repository.updateExamDate(...) 
  when onboarding completes ("Start my prep" button)
- The exam_date_confirmed flag should be set to true if the user 
  actively changed the date away from the 2026-12-20 default, 
  false if they left it as default (matches existing logic already 
  used elsewhere in the app for "confirmed" vs "rumoured" display)

Find OnboardingScreen.kt and check the exam date step specifically. 
Verify: is there actually a working DatePickerDialog wired to a 
click handler, or is the date field non-interactive / decorative 
only? Fix so the date is genuinely selectable and updates the 
screen state immediately upon selection.

## Final consolidation batch — 4 items

### 1. Bug fix: Onboarding skips the exam date step entirely
Confirmed bug: users never see Step 3 (exam date selection) during 
onboarding — it gets skipped entirely. Expected flow: Step 1 (name) 
→ Step 2 (exam stream) → Step 3 (exam date, with working date 
picker) → onboarding complete via "Start my prep" on Step 3 only. 
Investigate the step/navigation logic for where it's skipping — 
likely the "Next" button on Step 2 is either incrementing the step 
index by 2, or incorrectly triggering onboarding completion early 
instead of advancing to Step 3.

### 2. Remove Quick Quiz feature entirely
Remove the "Quick Quiz" button/text from SubjectsScreen.kt completely, 
along with any quiz question/answer screen, ViewModel logic, or 
navigation route built to support it. Delete these files entirely — 
don't just hide the button. Each subject card should only show: 
ring/percentage (average of logged ScoreEntry.percentage for that 
subject), name, status badge, and test count.

### 3. Simplify study time logging
Study time should be tracked ONLY through Timetable slot completions 
— marking a scheduled slot as done IS the study log for that time 
block. Remove any leftover legacy "Study — 6 hours" hardcoded task 
or studyDone-based UI element that exists separately from the 
Timetable feature (this was from the original pre-Timetable task 
system and is now redundant). Check TaskLog.studyDone usage across 
the codebase — if it's still referenced anywhere in the UI as a 
standalone toggle (not tied to a timetable slot), remove that UI 
element. The TaskLog.studyDone field itself can remain in the 
database schema (to avoid another migration), but should not be 
shown as a separate manual checkbox anywhere in the UI anymore.

### 4. Single entry point for Log Score — Home screen only
"Log a Practice Test" / "Log Score" buttons currently appear in 
multiple places (Home screen CTA button, and also as a button in 
the Weekly/Stats screen empty state). Remove the Log Score button 
from the Weekly/Stats screen empty state entirely — replace it with 
plain text only (e.g. "Log your first score from the Home screen 
to start tracking weekly progress" — no button, no navigation 
action). The ONLY place a "Log Score" button/CTA should exist 
anywhere in the app is the Home screen. Search all other screens 
for any additional Log Score buttons and remove them the same way, 
leaving only descriptive text if needed.

## New feature: Topic-wise subject tracking (replaces flat subject scoring)

This restructures how subject mastery is calculated. Instead of a 
subject's % being a flat average of all ScoreEntry rows logged under 
that subject name, it now rolls up from individual topics within 
that subject.

### New Room entities

Topic:
- id (autoGenerate)
- subject (String — links to the subject name)
- topicName (String — user-defined, e.g. "Pharmacokinetics")
- createdAt (Long)

Modify ScoreEntry to add an optional topicId field (nullable Int/Long, 
foreign key to Topic.id) — existing subject-level scores without a 
topic remain valid (topicId = null), but new scores will typically 
be logged against a specific topic.

Add a new Room migration (bump to the next version) creating the 
Topic table and adding the topicId column to the existing scores 
table, without destroying existing data.

### New screen: Subject Detail

Tapping a subject card on SubjectsScreen.kt navigates to a new 
SubjectDetailScreen.kt showing:
- Header: subject name, overall % (rollup average of all topics' 
  average %), back button
- List of topics for this subject, each row showing: topic name, 
  test count, that topic's average %
- "+ Add topic" button/row at the bottom — opens a simple dialog 
  to type a new topic name
- Tapping a topic opens the existing Log Score flow, but pre-filled 
  with this subject AND this topic (so the score gets saved with 
  both subject and topicId set)

### Updated calculation logic

Subject's overall % on SubjectsScreen.kt = average of each topic's 
average %, NOT a flat average of all individual test scores. If a 
subject has zero topics yet, show "Add topics to start tracking" 
instead of 0%, so it's clear this is an empty state, not a real 
zero score.

Follow the existing Dark Premium theme, Roboto typography per 
CLAUDE.md's corrected sizes, and existing screen/dialog patterns 
(e.g. AddSlotDialog in TimetableScreen.kt) for consistency.

## New feature: Custom Notification Filter (Focus Mode)

### IMPORTANT ARCHITECTURAL NOTE — deliberate exception
This feature intentionally introduces a persistent 
NotificationListenerService, which is an exception to this app's 
otherwise strict "zero background services" rule. This was a 
conscious, informed decision — do NOT "fix" this by removing the 
listener service in future sessions. It is the only way to actually 
filter/suppress specific notifications on Android; there is no 
battery-safe alternative for this specific capability. Every other 
feature in this app must still follow the zero-background-service 
rule — this is a single, isolated, deliberate exception.

### What it does
Lets the student select specific distracting apps (e.g. Instagram, 
WhatsApp status, Snapchat, browsers) whose notifications get 
suppressed during a chosen time window — either tied to active 
Timetable slots, or a manual "Start Focus Session" toggle. Phone 
calls and SMS/messaging apps are NEVER filterable — explicitly 
excluded from the selectable app list, always allowed through.

### Technical implementation
1. Create NotificationFilterService extending 
   android.service.notification.NotificationListenerService.
   - Declare in AndroidManifest.xml with proper intent-filter for 
     android.service.notification.NotificationListenerService and 
     the BIND_NOTIFICATION_LISTENER_SERVICE permission.
   - In onNotificationPosted(sbn): check if sbn.packageName is in 
     the student's selected "muted apps" list AND current time is 
     within an active filter window (Timetable slot in progress OR 
     manual Focus Session active). If both true, call 
     cancelNotification(sbn.key) to suppress it.
   - HARD EXCLUSION: never suppress notifications from the default 
     phone/dialer app package, or any SMS/messaging app package — 
     check against 
     context.packageManager.resolveActivity(Intent(Intent.ACTION_DIAL)) 
     to identify the phone app dynamically, and explicitly skip it 
     regardless of the student's selection (don't even show it as 
     selectable in the app-picker UI).

2. New DataStore keys: 
   - notificationFilterEnabled (Boolean, default false)
   - mutedAppPackages (JSON string list, similar pattern to 
     customSubjects)
   - filterActivationMode (String: "TIMETABLE" or "MANUAL")

3. New Settings section "Notification Filter (Focus Mode)":
   - Master toggle to enable the whole feature
   - "Select apps to mute" — opens a screen listing installed 
     launcher apps (query PackageManager for apps with a launcher 
     intent, exclude system apps, exclude the phone/messaging apps 
     as described above), each with a checkbox
   - Activation mode selector: "Auto (during Timetable slots)" or 
     "Manual (Start Focus Session button on Goals screen)"
   - Clear explanatory text: "This app will ask for Notification 
     Access permission. Calls and messages are never affected."

4. Permission request flow: when the student enables the master 
   toggle for the first time, show an explanation dialog, then 
   deep-link to Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS so 
   they can grant "Notification access" to this app manually (this 
   cannot be requested via a simple runtime permission dialog — 
   Android requires manual grant in system settings for this 
   specific permission).

5. If activation mode is MANUAL, add a "Start Focus Session" / 
   "Stop Focus Session" toggle button on the Goals/Timetable screen, 
   storing an isFocusSessionActive flag (DataStore boolean) that the 
   NotificationListenerService checks alongside Timetable slot times.

Follow the existing Dark Premium theme for all new UI. Compile and 
verify no manifest/permission declaration errors.

## New feature: Custom Notification Filter (Focus Mode)

### IMPORTANT ARCHITECTURAL NOTE — deliberate exception
This feature intentionally introduces a persistent 
NotificationListenerService, which is an exception to this app's 
otherwise strict "zero background services" rule. This was a 
conscious, informed decision — do NOT "fix" this by removing the 
listener service in future sessions. It is the only way to actually 
filter/suppress specific notifications on Android; there is no 
battery-safe alternative for this specific capability. Every other 
feature in this app must still follow the zero-background-service 
rule — this is a single, isolated, deliberate exception.

### What it does
Lets the student select specific distracting apps (e.g. Instagram, 
WhatsApp status, Snapchat, browsers) whose notifications get 
suppressed during a chosen time window — either tied to active 
Timetable slots, or a manual "Start Focus Session" toggle. Phone 
calls and SMS/messaging apps are NEVER filterable — explicitly 
excluded from the selectable app list, always allowed through.

### Technical implementation
1. Create NotificationFilterService extending 
   android.service.notification.NotificationListenerService.
   - Declare in AndroidManifest.xml with proper intent-filter for 
     android.service.notification.NotificationListenerService and 
     the BIND_NOTIFICATION_LISTENER_SERVICE permission.
   - In onNotificationPosted(sbn): check if sbn.packageName is in 
     the student's selected "muted apps" list AND current time is 
     within an active filter window (Timetable slot in progress OR 
     manual Focus Session active). If both true, call 
     cancelNotification(sbn.key) to suppress it.
   - HARD EXCLUSION: never suppress notifications from the default 
     phone/dialer app package, or any SMS/messaging app package — 
     check against 
     context.packageManager.resolveActivity(Intent(Intent.ACTION_DIAL)) 
     to identify the phone app dynamically, and explicitly skip it 
     regardless of the student's selection (don't even show it as 
     selectable in the app-picker UI).

2. New DataStore keys: 
   - notificationFilterEnabled (Boolean, default false)
   - mutedAppPackages (JSON string list, similar pattern to 
     customSubjects)
   - filterActivationMode (String: "TIMETABLE" or "MANUAL")

3. New Settings section "Notification Filter (Focus Mode)":
   - Master toggle to enable the whole feature
   - "Select apps to mute" — opens a screen listing installed 
     launcher apps (query PackageManager for apps with a launcher 
     intent, exclude system apps, exclude the phone/messaging apps 
     as described above), each with a checkbox
   - Activation mode selector: "Auto (during Timetable slots)" or 
     "Manual (Start Focus Session button on Goals screen)"
   - Clear explanatory text: "This app will ask for Notification 
     Access permission. Calls and messages are never affected."

4. Permission request flow: when the student enables the master 
   toggle for the first time, show an explanation dialog, then 
   deep-link to Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS so 
   they can grant "Notification access" to this app manually (this 
   cannot be requested via a simple runtime permission dialog — 
   Android requires manual grant in system settings for this 
   specific permission).

5. If activation mode is MANUAL, add a "Start Focus Session" / 
   "Stop Focus Session" toggle button on the Goals/Timetable screen, 
   storing an isFocusSessionActive flag (DataStore boolean) that the 
   NotificationListenerService checks alongside Timetable slot times.

Follow the existing Dark Premium theme for all new UI. Compile and 
verify no manifest/permission declaration errors.

## Bug fix: Streak calculation out of sync with Timetable + optional habits

Confirmed issue: TaskLog.allDone was originally defined as 
studyDone && examDone && exerciseDone && vitaminDone — built for the 
old fixed 4-task system. This is now broken/misleading because:
- studyDone is no longer set by anything (study is tracked via 
  Timetable slot completions instead, per the earlier "Simplify 
  study time logging" fix)
- vitaminDone is required even for students who have the Multivitamin 
  toggle OFF in Settings (it's optional, off by default)
- exerciseDone is required even though it's a toggleable habit

### Corrected streak definition

A day counts as "complete" (contributing to streak) if:
1. ALL of that day's scheduled TimetableSlot completions are marked 
   done (via TimetableCompletion) — if a day had zero slots 
   scheduled, this condition is automatically satisfied (don't 
   require slots that don't exist)
2. AND for each wellness habit the student has actively enabled in 
   Settings (check UserProfile.exerciseReminderEnabled and 
   UserProfile.vitaminReminderEnabled): if enabled, that habit's 
   TaskLog field (exerciseDone / vitaminDone) must be true for that 
   day. If a habit is disabled in Settings, it's excluded from the 
   requirement entirely — don't check its TaskLog value at all.
3. "examDone" (test logged that day) is NOT part of streak — this 
   was conflating "did you log a test" with "did you complete your 
   day," which are different things. Test-logging frequency is 
   already tracked separately (weekly test counts, personal best, 
   etc.) — remove examDone from the streak/allDone calculation 
   entirely.

### Where to fix
- Update the streak calculation function (calculateStreak / 
  calculateStreakFromLogs, wherever it currently lives — likely 
  HomeViewModel.kt or a shared location) to implement the corrected 
  logic above instead of the old flat allDone boolean check.
- Consider whether TaskLog.allDone (the stored flag, used for the 
  Stats heatmap) should also be recalculated using this same logic, 
  since the heatmap currently likely has the same stale definition 
  problem — check StatsScreen.kt / wherever heatmap intensity is 
  calculated and apply the same corrected logic there too, so streak 
  and heatmap stay consistent with each other.
- This is a pure logic fix — no new Room migration needed, since 
  we're just changing HOW existing data is interpreted, not the 
  schema itself.

## Redesign: Add Time Slot dialog

### Bug fix: Day chip wrapping
"Sun" chip currently wraps vertically (S/u/n stacked) because it 
runs out of horizontal space in the Row. Fix by using single-letter 
day labels (M, T, W, T, F, S, S) instead of full 3-letter names 
(Mon, Tue, etc.), each chip in a Row with Modifier.weight(1f) so 
all 7 always fit evenly in one line regardless of screen width. 
Use a contentDescription or tooltip for accessibility since letters 
alone can be ambiguous (T for Tue vs Thu).

### Time picker redesign — add AM/PM
Replace the current separate HH/MM raw number OutlinedTextFields 
with a cleaner design:
- A single time display box showing "06:00" format (24hr internally 
  stored, but displayed as 12hr with AM/PM for the student)
- A vertical AM/PM toggle beside it (two stacked selectable options)
- Consider using Android's Material3 TimePicker or TimeInput 
  composable (is24Hour = false) instead of custom text fields for 
  a more native, reliable time-entry experience — evaluate which 
  fits better with the existing AddSlotDialog structure
- Internally, continue storing startTime/endTime as 24-hour "HH:mm" 
  strings in the database (no schema change) — only the DISPLAY 
  and INPUT should show 12hr + AM/PM

### Visual polish
- Add small uppercase section labels (e.g. "DAY OF WEEK", "START 
  TIME", "END TIME", "LABEL") above each input group, 10sp, muted 
  color, consistent with the section-label style used elsewhere in 
  the app
- Match Dark Premium card/border styling for the time display box

### New: Focus Mode toggle in this dialog
Add a toggle row inside the Add Time Slot dialog: "Focus Mode during 
this slot" with description "Muted apps stay silenced automatically" 
and a switch. This ties into the existing Notification Filter 
feature (filterActivationMode = "TIMETABLE") — when this toggle is 
ON for a given slot, that slot's time window becomes an active 
filter window for the NotificationFilterService, in addition to any 
globally-enabled Timetable-based filtering. Store this as a new 
Boolean field on TimetableSlot: focusModeEnabled (default false), 
requiring a small migration to add this column (no new table needed, 
just ALTER TABLE to add the column with default value).

Note: this toggle should only be meaningfully functional if the 
Notification Filter feature has already been built. If it hasn't 
been built yet in this session, still add the UI toggle and the 
database field (for forward compatibility), but the actual filtering 
behavior depends on NotificationFilterService which may be built 
separately.

## App icon: Summit (mountain + flag)

The app icon was manually created as an adaptive icon:
- app/src/main/res/drawable/ic_launcher_background.xml (solid #141110)
- app/src/main/res/drawable/ic_launcher_foreground.xml (gold mountain + flag)
- app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml (adaptive icon reference)
- app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml (same, for round icon)

The old default mipmap-* density folders (mipmap-mdpi, mipmap-hdpi, etc. 
with PNG ic_launcher.png files) may still exist from the original AI 
Studio scaffold and will be used as a fallback on Android versions 
below API 26. Do not delete them unless explicitly asked — they are a 
safe fallback, not a conflict, since mipmap-anydpi-v26 takes priority 
on API 26+ (which covers this app's minSdk 35 anyway, so the old PNGs 
are effectively unused dead weight but harmless).

## Combined final feature batch — 6 items

### 1. In-app Privacy Policy screen
Add a new "Privacy Policy" row in SettingsScreen.kt under a new/
existing "About" section, navigating to PrivacyPolicyScreen.kt. 
Display this exact text as static scrollable content (store as a 
string resource or Kotlin constant, no network fetch needed):

---
Privacy Policy for DI Prep

Last updated: [insert today's date]

DI Prep is designed to work entirely offline. This policy explains 
exactly what data the app handles and how.

DATA WE COLLECT: None.
DI Prep does not collect, transmit, or share any personal data with 
us or any third party. The app has no servers, no analytics, no 
advertising, and no internet permission requested.

DATA STORED ON YOUR DEVICE:
The app stores the following information locally on your phone only, 
never transmitted anywhere:
- Your name and exam preparation details (exam name, date, subjects)
- Test scores and study progress you log
- Your custom timetable and study schedule
- App preferences (theme, notification settings)

This data never leaves your device unless you choose to export it 
yourself. Uninstalling the app permanently deletes all this data.

NOTIFICATION ACCESS PERMISSION:
If you enable the optional Focus Mode feature, DI Prep requests 
Android's Notification Access permission. This is used solely to 
temporarily suppress notifications from apps you select, during 
time periods you schedule. The app only checks which app sent a 
notification (not its content) to decide whether to suppress it. 
No notification content is ever read, stored, or transmitted. Phone 
calls and SMS/messaging apps cannot be muted under any circumstance. 
This permission can be revoked at any time in your phone's Settings.

CAMERA/PHOTOS:
DI Prep does not currently request camera or photo access.

CONTACT US:
If you have questions about this privacy policy, contact: 
[insert your email]
---

Style with existing Dark Premium theme, TopAppBar with back button. 
Add a second "View Online" row below opening a placeholder URL via 
Intent.ACTION_VIEW.

### 2. Refer / Invite a Friend
Add "Invite Friends" row in Settings, same About section as Privacy 
Policy. Tapping triggers Intent.ACTION_SEND (text/plain) with message:

"I'm using DI Prep to stay consistent with my exam preparation — 
it's a simple, fully offline study tracker with zero ads and zero 
data collection. Give it a try: https://play.google.com/store/apps/details?id=com.anubhav.diprep"

Icon: share/people icon. Subtitle: "Share DI Prep with someone 
preparing for exams too".

### 3. App Shortcuts (long-press icon)
Static shortcuts via res/xml/shortcuts.xml, declared in 
AndroidManifest.xml under the main launcher activity's meta-data 
tag. Two shortcuts: "Log Score" (deep links to Log Score screen) and 
"Add Time Slot" (deep links to Goals/Timetable screen with Add Time 
Slot dialog pre-opened). Dark Premium gold-accent icons.

### 4. Home Screen Widget
Native App Widget (Glance API for Compose preferred, or RemoteViews) 
showing: days remaining to exam (large) + current streak (small, 
flame icon). CRITICAL: no periodic/recurring update mechanism — no 
android:updatePeriodMillis set to a nonzero recurring value, no 
WorkManager. Widget updates ONLY when added/resized or when the app 
is opened (trigger AppWidgetManager.updateAppWidget() from 
MainActivity's lifecycle, or piggyback on the existing daily 
Multivitamin reminder alarm to also refresh the widget at that same 
trigger point). Dark background (#141110), gold text (#E8B869).

### 5. Demo Mode
loadDemoData() function in AppRepository.kt seeding realistic sample 
data: UserProfile (name "Demo Student", Drug Inspector stream, exam 
date 90 days from today calculated dynamically), 6 subjects with 
3-4 topics each, ScoreEntry across last 8 weeks with realistic 
mixed performance (some subjects trending up, one trending down), 
TimetableSlot entries across all 7 days (2-3 per day), 
TimetableCompletion + TaskLog for past 2-3 weeks (semi-consistent 
streak/heatmap, not perfect), a few MoodEntry records.

Trigger points: (a) "Try Demo" secondary button on onboarding Step 1 
welcome screen, skips straight to Home with demo loaded, (b) "Load 
Demo Data" row in Settings under Data & Storage.

Add isDemoMode DataStore boolean. Show persistent gold-bordered 
banner on Home screen when active: "Demo Mode — sample data for 
exploration" with "Exit Demo" action. Both "Exit Demo" and existing 
"Reset App" must clear all demo data and route back to real 
onboarding.

### 6. Hindi Language Support
Language toggle in Settings ("App Language: English / Hindi"). 
Extract hardcoded strings into res/values/strings.xml (English 
default), create res/values-hi/strings.xml with Hindi translations. 
Use AppCompatDelegate.setApplicationLocales() for runtime switching 
without restart. New DataStore key: appLanguage ("en"/"hi"). 
Prioritize translating onboarding, Home screen, Settings screen, 
and common buttons (Save/Cancel/Add/Delete) first — full app 
translation can be iterative.

## New: App-wide Haptic Feedback (zero-crash-risk implementation)

### Safety requirements — non-negotiable
1. Use ONLY Jetpack Compose's built-in LocalHapticFeedback API 
   (HapticFeedbackType constants) — never raw android.os.Vibrator 
   calls, since Compose's API safely no-ops on devices without 
   vibration hardware and respects the user's system-level haptic 
   settings automatically, with zero manifest permission needed.
2. EVERY haptic call must be wrapped in try/catch, silently doing 
   nothing on failure — a haptic feedback failure must NEVER crash 
   the app or block the actual button/action it's attached to. The 
   real click handler logic always executes regardless of whether 
   the haptic call succeeds.
3. Centralize through ONE reusable utility — do not scatter raw 
   LocalHapticFeedback.current.performHapticFeedback() calls across 
   every screen individually (inconsistent, error-prone). Create a 
   single Composable helper function that every screen calls into.
4. Add a master Settings toggle ("Haptic Feedback" on/off, default 
   ON) so users who dislike vibration or have accessibility needs 
   can disable it app-wide. When disabled, the centralized utility 
   simply does nothing — no calls fire at all.

### Implementation
Create HapticUtils.kt in a shared util/ package:
wc -l ~/Downloads/exam-prep/CLAUDE.md






wc -l ~/Downloads/exam-prep/CLAUDE.md

New DataStore key: hapticFeedbackEnabled (Boolean, default true). 
Add corresponding Repository/PreferencesManager function.

### Where to apply it (use HapticFeedbackType.LongPress as the 
standard "tap" feedback type throughout, consistent everywhere)
- All primary buttons (Save, Add, Log Score, Start my prep, Continue, 
  Add time slot, Add topic, etc.)
- Task/habit checkboxes when toggled (Goals/Timetable screen)
- Toggle switches in Settings (all of them)
- Bottom navigation tab taps
- Subject card taps, filter chip selections, day-of-week chip 
  selections
- Milestone celebration / confetti trigger moment (a slightly 
  stronger haptic here is appropriate, still via the same safe 
  utility)
- Delete/destructive actions (Clear All Scores, Reset App, delete 
  topic/slot) — use HapticFeedbackType.LongPress here too, kept 
  consistent rather than introducing a different "warning" pattern

Add "Haptic Feedback" toggle row in Settings, placed near other 
app-wide preference toggles (e.g. near Appearance).

## New: App-wide Haptic Feedback (zero-crash-risk implementation)

### Safety requirements — non-negotiable
1. Use ONLY Jetpack Compose's built-in LocalHapticFeedback API 
   (HapticFeedbackType constants) — never raw android.os.Vibrator 
   calls, since Compose's API safely no-ops on devices without 
   vibration hardware and respects the user's system-level haptic 
   settings automatically, with zero manifest permission needed.
2. EVERY haptic call must be wrapped in try/catch, silently doing 
   nothing on failure — a haptic feedback failure must NEVER crash 
   the app or block the actual button/action it's attached to. The 
   real click handler logic always executes regardless of whether 
   the haptic call succeeds.
3. Centralize through ONE reusable utility — do not scatter raw 
   LocalHapticFeedback.current.performHapticFeedback() calls across 
   every screen individually (inconsistent, error-prone). Create a 
   single Composable helper function that every screen calls into.
4. Add a master Settings toggle ("Haptic Feedback" on/off, default 
   ON) so users who dislike vibration or have accessibility needs 
   can disable it app-wide. When disabled, the centralized utility 
   simply does nothing — no calls fire at all.

### Implementation
Create HapticUtils.kt in a shared util/ package:

New DataStore key: hapticFeedbackEnabled (Boolean, default true). 
Add corresponding Repository/PreferencesManager function.

### Where to apply it (use HapticFeedbackType.LongPress as the 
standard "tap" feedback type throughout, consistent everywhere)
- All primary buttons (Save, Add, Log Score, Start my prep, Continue, 
  Add time slot, Add topic, etc.)
- Task/habit checkboxes when toggled (Goals/Timetable screen)
- Toggle switches in Settings (all of them)
- Bottom navigation tab taps
- Subject card taps, filter chip selections, day-of-week chip 
  selections
- Milestone celebration / confetti trigger moment (a slightly 
  stronger haptic here is appropriate, still via the same safe 
  utility)
- Delete/destructive actions (Clear All Scores, Reset App, delete 
  topic/slot) — use HapticFeedbackType.LongPress here too, kept 
  consistent rather than introducing a different "warning" pattern

Add "Haptic Feedback" toggle row in Settings, placed near other 
app-wide preference toggles (e.g. near Appearance).
