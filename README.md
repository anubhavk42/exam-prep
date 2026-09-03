# Exam Prep

A fully offline Android app that helps students preparing for competitive exams stay consistent, track their progress, and study without distractions — all while keeping every byte of their data on their own device.

## The problem it solves

Most study-tracker apps are built around an account, a cloud sync, and an ad network. For a student preparing for a high-stakes competitive exam, that means their personal study data lives on someone else's server, the app drains their battery with background sync, and the interface is cluttered with things that pull their focus away rather than toward their goal.

**Exam Prep** takes the opposite stance. It is a calm, private, battery-efficient companion for the months of preparation before an exam — a place to plan a timetable, log practice test scores, watch a subject's mastery climb, keep a streak alive, and silence distracting apps during focused study — with no account, no internet, and nothing leaving the phone.

It is not tied to any single exam. UPSC, SSC, Banking, Pharmacy / Drug Inspector, NEET / Medical, State PCS, or any custom exam a student types in — the subject list adapts to whatever they're preparing for.

## What makes it different

- **100% offline** — zero network calls and no internet permission is even requested. The app has no servers and cannot phone home.
- **Zero data collection** — no analytics, no advertising, no telemetry. Your name, scores, schedule, and progress are yours alone.
- **Zero background services** — no `WorkManager`, no repeating alarms, no polling. The app does work only while you have it open. (The one deliberate exception is the optional Focus Mode notification filter, which the user explicitly enables and can revoke at any time.)
- **Battery-efficient by design** — lifecycle-aware state collection, all database work off the main thread, and a strict MVVM + Repository architecture.
- **Fully personalizable** — build your own weekly timetable, choose which sections appear on your Home screen and in what order, pick your exam, and edit your subject and topic lists freely.

## Features

- **Personalized Home** — greeting, live exam countdown, "what's next" from your timetable, weak-topic alerts, streak and personal best, today's goals, and a daily quote — every section reorderable and toggleable.
- **Subjects & topics** — track mastery per subject, drill into individual topics, and see coverage roll up from the topic level.
- **Log practice scores** — record marks for a subject/topic and watch the percentage and trends update.
- **Custom timetable** — build a weekly schedule (day, start/end time, label) and mark slots complete as your study log.
- **Weekly progress & stats** — week-over-week averages, this-week-vs-last-week comparison, and a consistency heatmap.
- **Mistake log, syllabus checklists, and previous-year question tagging** — study tools to close the gaps.
- **Focus Mode** — optionally suppress notifications from distracting apps during study slots (phone calls and messages are never affected).
- **Mood check-ins, milestone celebrations, and an end-of-day recap** — small touches to stay motivated.
- **Home-screen widget** — days remaining and current streak at a glance, with no recurring background updates.
- **Extras** — daily reminders (one-time exact alarms only), haptic feedback, Hindi language support, a home-screen shortcut set, an in-app privacy policy, and a demo mode for exploring the app with sample data.

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

## Privacy

Exam Prep collects nothing and transmits nothing. The full policy is in [PRIVACY.md](PRIVACY.md) and is also viewable inside the app under Settings.

## Development note

This project was built using AI-assisted development with [Claude Code](https://claude.com/claude-code), Anthropic's agentic coding tool.
