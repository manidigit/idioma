# FlashLearn

FlashLearn is an offline-first Android vocabulary trainer built with Kotlin, Jetpack Compose, Material 3, Room/SQLite, Coroutines, Flow and a domain-level spaced-repetition engine.

## Current build

This package contains the Stage 1 foundation plus the first production-oriented visual redesign:

- Original visual identity and theme
- Light/Dark/System theme support
- Responsive Compose UI
- Home learning dashboard
- Review mode selector and immersive flashcard
- Correct/Incorrect interaction model
- Vocabulary library with search
- Progress and gamification surfaces
- Settings surface
- Persian-friendly RTL foundation (`supportsRtl=true`)
- Spanish/English LTR content
- Room schema foundation
- Language pairs and normalized multilingual content model
- Domain-level review engine and tests

## Product rules preserved

Learning stage is separate from difficulty. Learning state is scoped to a language pair, so the same concept can be learned independently in different directions.

Daily correct -> Weekly (7 days)
Weekly correct -> Monthly (30 days)
Monthly correct -> Learned
Wrong at Weekly/Monthly -> Daily with difficulty escalation
Only due cards satisfy `nextReviewAt <= now` in timed review modes.

## Architecture

`Compose UI -> ViewModel -> Use Case -> Repository -> DAO -> Room/SQLite`

The ReviewEngine is deliberately independent from Android UI and database code so the core learning rules can be unit-tested.

## Database model

- languages
- language_pairs
- categories
- concepts
- contents (one content per concept/language)
- tags
- concept_tags
- learning_states
- review_sessions
- review_history
- app_settings

Concept UUIDs are stable import/export identities. Internal numeric IDs remain local database keys.

## Development

Open the project root in Android Studio with a recent Android Gradle Plugin/Kotlin-compatible environment and sync Gradle. The project targets Android API 37 and uses Java 17.

If Gradle wrapper binaries are not present in your environment, use Android Studio's Gradle tooling or generate the wrapper with your installed Gradle distribution.

## Important status note

The supplied Stage 1 archive did not contain a usable compiled APK or Gradle wrapper JAR, so this package does not claim a verified APK build from this runtime. Source code and tests are included; run a Gradle sync/test/build in Android Studio or CI before release.

## Assets

No proprietary Duolingo/Memrise/Quizlet/Babbel/Anki assets are included. The visual identity is implemented from Compose primitives and open Material iconography.

## Adaptive difficulty engine

Difficulty is a derived, materialized value backed by the immutable `review_history` audit log. It is recalculated after every review and is never manually selected by the user.

### Signals

- DAILY incorrect: +1, with progressive repeated-daily penalties.
- WEEKLY incorrect: +3 plus +3 for the return to DAILY; repeated weekly failures add escalation.
- MONTHLY incorrect: +5 plus +5 for the return to DAILY; second and later monthly failures add stronger escalation.
- Consecutive failures at the same stage add additional penalties.
- Recent successful performance supplies bounded recovery credit; history is never deleted.

Baseline score bands are 0–2 EASY, 3–7 MEDIUM, 8–14 HARD, and 15+ VERY_HARD, with evidence floors so a meaningful failure cannot be hidden by one later success. Repeated monthly failures are explicitly escalated to VERY_HARD. After sustained successful recovery, current difficulty can improve while the original review events remain unchanged.

### Persistence

`learning_states` stores materialized counters and the current score for fast filtering. `review_history` stores one immutable row per review event, including previous/new stage and difficulty, answer data, score, and response time. The review repository calculates from the complete history and writes the history row plus state update atomically.

### Duplicate prevention

`concept_language_keys` enforces one concept per normalized source-language text. `translations` allows multiple target-language meanings but enforces uniqueness on `(conceptId, languageCode, normalizedText)`. Original user text is preserved; normalization is used only as an internal duplicate key.

## CI

GitHub Actions is configured in `.github/workflows/android.yml` to run unit tests and `assembleDebug` with JDK 17 and Gradle 9.5. No API keys are required for the adaptive review engine.
