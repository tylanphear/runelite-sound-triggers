# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Sound Triggers is a RuneLite external plugin that plays user-specified sound files when configurable in-game events occur. It targets Java 11 and is built with Gradle.

## API docs

When you need to check a RuneLite API signature or semantics (e.g. what units a parameter expects), fetch the official Javadoc:

- `net.runelite.client.*` — https://static.runelite.net/runelite-client/apidocs/
- `net.runelite.api.*` — https://static.runelite.net/runelite-api/apidocs/

For example, `net.runelite.client.audio.AudioPlayer` is documented at `https://static.runelite.net/runelite-client/apidocs/net/runelite/client/audio/AudioPlayer.html`.

## Commands

```bash
# Build
./gradlew build

# Run the plugin inside a local RuneLite client (developer mode)
./gradlew run

# Build a fat/shadow JAR for distribution
./gradlew shadowJar

# Run tests
./gradlew test
```

> **Build or test run fails with an environmental error (`java.io.IOException: Input/output error`, e.g. while creating Gradle's `FileHasher`)?** This usually means a test client launched via `./gradlew run` is still open and holding a lock on the Gradle cache/build files. **ALWAYS** instruct the user to close the running client; this will allow you to successfully re-run the command.

> **Note on tests:** Real automated coverage lives in `TriggerMatcherTest` (JUnit 4). It exercises the trigger-matching and `PLAYER_STAT` state-machine logic with no live `Client`. Be aware that `SoundTriggersPluginTest` is just a `main()` launcher (the RuneLite convention) that boots a dev client via `ExternalPluginManager.loadBuiltin`, and contributes no assertions to `./gradlew test`.

> When adding behavior that warrants verification, follow the **functional core, imperative shell** split already in place: put the pure decision logic in `TriggerMatcher` (no `Client`, no event bus, plain data in and out) and keep the `@Subscribe` handlers a thin shell that feeds it primitives. Add JUnit tests against the core rather than relying on the launcher.

## Architecture

The plugin follows the standard RuneLite plugin pattern:

- **`SoundTriggersPlugin`** — the entry point (`@PluginDescriptor`) and the **imperative shell**. Handles lifecycle (`startUp`/`shutDown`), subscribes to game events, and owns trigger persistence via `ConfigManager` (JSON-serialized to config key `soundtriggers.triggers`). Each handler reads the volatile bits out of its event (and any `Client` state) and delegates the fire-or-not decision to `TriggerMatcher`; it does not embed matching logic itself.

- **`TriggerMatcher`** — the **functional core**: pure, `Client`-free decisions about whether a given `SoundTrigger` should fire. One `matches*` method per event type, each also checking `enabled`/`type` so callers can just iterate all triggers and fire on `true`. Also hosts `matchesStatCondition` and `stepStat`, the per-tick `PLAYER_STAT` state machine. Everything here takes plain data and returns plain data, which is what makes it unit-testable.

- **`SoundTrigger`** — plain Lombok `@Data` model representing one trigger. Fields are union-style: type-specific fields are only relevant when `type` matches.

- **`StatTriggerState`** — an *immutable* per-tick snapshot of a `PLAYER_STAT` trigger's runtime state (`conditionMet`, `lastFiredMs`). Not persisted. `TriggerMatcher.stepStat` returns a fresh instance plus the fire decision (`StatStep`); the plugin owns the `Map<triggerId, StatTriggerState>` and swaps entries in — keeping the state machine pure.

- **`TriggerType` and the per-type filter enums** — `TriggerType` is the discriminator; the remaining enums are the option sets for individual type-specific fields.

- **`SoundTriggersPanel`** — the RuneLite side panel (`PluginPanel`). Holds a scrollable list of `TriggerPanel` cards. Call `rebuild()` after any change to the triggers list.

- **`TriggerPanel`** — a collapsible card per trigger. Builds its own Swing UI inline; calls `plugin.saveTriggers()` on every field change. One type-specific sub-panel per `TriggerType` is built up front; `updateSectionVisibility` toggles exactly one visible to match the selected type when the type combo changes.

- **`SoundTriggersConfig`** — a minimal `@ConfigGroup` interface; the triggers JSON is stored as a hidden config item managed entirely by the plugin, not exposed as a user-facing config panel.

### Persistence flow

Triggers are serialized to JSON via Gson and stored in RuneLite's `ConfigManager` under group `soundtriggers`, key `triggers`. They are loaded in `startUp()` and saved immediately on every UI interaction.

### Audio playback

Sound files are played via RuneLite's injected `AudioPlayer`. Only certain file formats (technically all formats supported by `AudioFileFormat.Type`, but currently only WAV) are supported (enforced by the file chooser filter). NOTE: for `AudioPlayer.play(file, gain)`, the `gain` parameter is in **decibels**.
