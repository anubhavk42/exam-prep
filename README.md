# Exam Prep

**A fully offline app that helps students preparing for any competitive exam build a consistent study routine and track their real readiness — without needing internet or draining their phone's battery.**

## The problem

Students preparing for competitive exams — government jobs, licensing exams like Drug Inspector, UPSC, banking — juggle a mess of disconnected tools: a physical notebook for their timetable, a notes app for mistakes, WhatsApp reminders from parents, and generic study apps that assume a fixed syllabus and constant internet access.

Many prepare in areas with patchy connectivity. Many are anxious about their phone's battery lasting through months of daily use if an app runs background services. Nobody has a single, honest daily answer to "am I actually ready for this exam" — just scattered signals across five different places.

## Who it's for

- **Primary:** Self-directed competitive exam aspirants in India — studying independently or alongside coaching — who want a personal tracking companion, not another content platform.
- **Secondary:** Students who want a fully custom weekly timetable and want to log their own mock test performance to see real, evidence-based progress rather than a vague feeling of "I've been studying."
- **Explicitly not for:** Students looking for live classes, video lectures, or a delivered curriculum. That's Unacademy / Physics Wallah territory. This app assumes you already have study material — it exists to organize and track your use of it, not to teach you.

## The key decision

The bet: **readiness should be built from a student's own logged evidence** — real test scores, real timetable adherence — not from passive content-consumption metrics.

Paired with a second bet: that offline-first, zero-background-service architecture is worth its constraints, because trust and battery reliability matter more for a tool meant to stay installed and opened daily for 3–12 months than any convenience a server could add.

## The trade-off

The hard choice was going 100% offline with zero background services. The alternative rejected: a lightweight backend for cloud sync, streaks-as-a-service, or peer leaderboards — the kind of social / competitive layer that Testbook and similar apps use to drive engagement.

What that cost: no cloud backup (losing your phone means losing your data, full stop — no built-in recovery), no peer comparison or leaderboards (even though that's a proven motivator for some students), and no server-pushed notifications — only a single locally-scheduled alarm per day.

A second, related trade-off: a **Notification Filter (Focus Mode)** feature that requires a persistent Android permission (`NotificationListenerService`) — a deliberate, isolated exception to the app's own zero-background-service rule. This raises Play Store review risk and adds a genuine battery / architecture cost, but was kept because the utility (silencing distracting apps during self-scheduled study blocks) outweighed architectural purity for this one feature, while every other part of the app still holds the line.

## What's in v1

- Custom exam + subject setup (not locked to one exam)
- Custom weekly timetable (student-built, not a fixed routine)
- Mock test score logging with live % calculation, rolled up topic → subject
- Weekly trend analytics + a revision-debt-aware consistency heatmap
- Fully personalizable Home screen (toggle and reorder every section)
- Optional wellness habits (exercise, multivitamin — off by default)
- Daily mood check-in
- Focus Mode notification filtering tied to timetable slots
- English + partial Hindi language support
- Home screen widget (countdown + streak, no background polling)
- Static app shortcuts (long-press icon → Log Score, Add Time Slot)
- Refer / Invite a friend (native share, no tracking, no server)
- In-app Privacy Policy screen (mirrors the hosted policy)
- App-wide haptic feedback, centrally controlled with a single Settings toggle and a zero-crash-risk implementation
- Demo Mode for instant, zero-setup exploration
- 100% offline, zero network permission requested

## What's deliberately not in v1

- **Social features / leaderboards** — requires a server, breaks the offline-first trust model, risks demotivating students who are behind.
- **Cloud backup / sync** — cut to keep the architecture shippable. This is the most likely v2 addition, given the real data-loss risk it creates.
- **AI-powered question-bank digitization** (photograph a question book → auto-structured MCQs) — researched, confirmed feasible via on-device Gemini Nano, explicitly rejected because that model only runs on flagship-tier phones with 12GB+ RAM. Shipping a feature invisible to most of the actual target users would be the wrong call.
- **Content delivery** (video lectures, live classes) — deliberately out of scope. This is a tracking and discipline layer, not a coaching platform.
- **Automated exam-weightage prioritization** — cut in favor of simple manual weightage input; true automation would need more data than one exam cycle produces, or a network call not worth adding.

## Tech stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3 (no XML layouts)
- **Architecture:** MVVM + Repository, single source of truth
- **Persistence:** Room (local database) and DataStore Preferences
- **Async:** Kotlin Coroutines + Flow, lifecycle-aware state collection
- **Min / Target SDK:** 35

## Building

There is no Gradle wrapper checked in. Build the debug APK with a local Gradle distribution and the Android Studio JDK:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  gradle assembleDebug
```

The output APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## How success is measured

**North star: 7-day retention rate among users who complete onboarding.** The entire premise is sustained daily use across months, not one powerful session — a student who opens it once and never returns has gotten zero value from it, no matter how polished that first session felt.

Supporting metrics:

- **Average streak length at day 30** — tests whether the consistency mechanic (Timetable + streak + heatmap) is actually functioning, not just present.
- **% of users logging at least 1 test score per week** — tests whether the app is used for its core job (evidence-based readiness tracking), not just as a glorified to-do list.
- **% of users who customize their Home screen layout** — a lightweight proxy for whether personalization is genuinely valued.

## Known limits

An honest list of what is missing or unverified:

- **No cloud backup** — uninstalling or losing the phone means permanent data loss; no export feature has been built yet.
- Focus Mode's Notification Access permission will likely draw Play Store review scrutiny and could require removal if rejected.
- **Hindi translation is partial** (onboarding, Home, Settings) — not the full app.
- **Only tested on one physical device** (Pixel 10, flagship) — real device fragmentation across screen sizes, older Android versions, and budget hardware is unverified.
- **No automated tests** — all verification was manual, phone-in-hand testing throughout the build.
- The home screen widget updates only when the app is opened (a deliberate battery-safety choice), which could read as "stale" to a user expecting live updates.
- Play Store submission (closed testing, developer verification) has not yet been started — the app is built and locally tested but not yet in the required 12-tester / 14-day testing window.

## Privacy

Exam Prep collects nothing and transmits nothing. The full policy is in [PRIVACY.md](PRIVACY.md) and is also viewable inside the app under Settings.

## Development note

This project was built using AI-assisted development with [Claude Code](https://claude.com/claude-code), Anthropic's agentic coding tool.
