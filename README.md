# Quick36 AutoSolver

Android accessibility service that watches the Quick36 app for math questions of the form `number operator number` and automatically taps the correct answer on the on-screen keypad.

## How it works

1. **Accessibility Service** (`SolverAccessibilityService`) listens for window-content changes in the Quick36 package.
2. It walks the accessibility node tree looking for text that matches a simple arithmetic expression.
3. `ExpressionParser` evaluates the expression.
4. `GestureHelper` finds the digit/operator buttons in the keypad (or falls back to percentage-based coordinates) and performs the taps.
5. A short dedupe window prevents answering the same question twice from duplicate events.

OCR via ML Kit is available as a fallback if the question text is drawn on a canvas and never appears in the accessibility tree. See `OcrHelper.kt`.

## Project layout

```
app/
  src/main/
    java/com/quick36/autosolver/
      MainActivity.kt                 – simple UI + accessibility-settings deep link
      SolverAccessibilityService.kt   – main orchestrator
      ExpressionParser.kt             – math parsing
      GestureHelper.kt                – node-click / coordinate taps
      OcrHelper.kt                    – optional OCR path
    res/
      xml/accessibility_service_config.xml
      layout/activity_main.xml
      values/strings.xml
    AndroidManifest.xml
.github/workflows/build.yml           – builds a debug APK on every push to main
```

## Building

This repo has no committed `gradlew` wrapper jar (binary files don't
transfer well through this channel) — the included GitHub Actions
workflow installs Gradle directly instead, so you don't need one locally.

Just push this repo to GitHub and the workflow at
`.github/workflows/build.yml` will build a debug APK and upload it as a
build artifact automatically. If you do want to build locally in Android
Studio, open the project there — Android Studio will regenerate the
wrapper for you automatically on first sync.

## Installing & enabling

1. Install the built APK on your device.
2. Open the app — it shows whether the Accessibility Service is enabled.
3. Tap "Open Accessibility Settings", find **Quick36 AutoSolver** in the
   list, and turn it on.
4. Open Quick36. As soon as a question appears matching the pattern
   `number operator number`, it should be answered automatically.

## How it decides what to tap

See `ExpressionParser.kt` for the math parsing and `GestureHelper.kt` for
the tap logic — both are heavily commented. `SolverAccessibilityService.kt`
is the orchestrator tying screen-reading → solving → tapping together, with
a dedupe guard (`lastQuestion` / `lastAnswerTime`) so the same question
doesn't get answered twice from duplicate accessibility events.

## Notes on speed

- Node-tree reads are the fast path (~5-10ms) — always preferred.
- OCR (if you add it) should only ever scan a **cropped** region of the
  screen, never the full frame — see `OcrHelper.cropToQuestionRegion()`.
- `OcrHelper.warmUp()` is called once in `onServiceConnected()` to avoid a
  cold-start penalty on the very first real recognition call.
