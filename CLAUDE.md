# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Sound Triggers is a RuneLite external plugin that plays user-specified sound files when configurable in-game events occur. It targets Java 11 and is built with Gradle.

## API docs

When you need to check a RuneLite API signature or semantics (e.g. what units a parameter expects), fetch the official Javadoc:

- `net.runelite.client.*` — https://static.runelite.net/runelite-client/apidocs/
- `net.runelite.api.*` — https://static.runelite.net/runelite-api/apidocs/

For example, `net.runelite.client.audio.AudioPlayer` is documented at `https://static.runelite.net/runelite-client/apidocs/net/runelite/client/audio/AudioPlayer.html`.

There is also a local version of the RuneLite API Events Reference located at `docs/runelite-events-api.md`.

## UI Docs

For reference when working on the UI (anything using `runelite.ui.*`), look at `docs/runelite-ui-best-practices.md`.

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

The plugin follows a functional-core/imperative-shell pattern:

- **`SoundTriggersPlugin`** — the entry point (`@PluginDescriptor`) and the **imperative shell**. Handles lifecycle (`startUp`/`shutDown`), subscribes to game events, and owns trigger persistence via `ConfigManager` (JSON-serialized to config key `soundtriggers.triggers`). Each handler reads the volatile bits out of its event (and any `Client` state) and delegates the fire-or-not decision to `TriggerMatcher`; it does not embed matching logic itself.

- **`TriggerMatcher`** — the **functional core**: pure, `Client`-free decisions about whether a given `SoundTrigger` should fire. One `matches*` method per event type, each also checking `enabled`/`type` so callers can just iterate all triggers and fire on `true`. Also hosts `matchesStatCondition` and `stepStat`, the per-tick `PLAYER_STAT` state machine. Everything here takes plain data and returns plain data, which is what makes it unit-testable.

- **`SoundTrigger`** — plain Lombok `@Data` model representing one trigger. Fields are union-style: type-specific fields are only relevant when `type` matches.

- **`TriggerStore`** — the pure (`Client`/`ConfigManager`-free) persistence (de)serializer: strings in, triggers out (and back). Owns the on-disk JSON shape and the schema version. The plugin shell hands it the raw config string and gets a `List<SoundTrigger>`; keeping it separate makes the version envelope and fault isolation unit-testable (`TriggerStoreTest`).

- **`StatTriggerState`** — an *immutable* per-tick snapshot of a `PLAYER_STAT` trigger's runtime state (`conditionMet`, `lastFiredMs`). Not persisted. `TriggerMatcher.stepStat` returns a fresh instance plus the fire decision (`StatStep`); the plugin owns the `Map<triggerId, StatTriggerState>` and swaps entries in — keeping the state machine pure.

- **`SoundSource`** — discriminator for how a trigger plays sound: `FILE` (user-picked file via `AudioPlayer`), `BUILTIN` (a curated `SoundEffectID` from `BuiltinSound` via `client.playSoundEffect`), or `CUSTOM` (a raw sound-effect ID via `client.playSoundEffect`). Each trigger carries all three flavors of config; `SoundSource` selects which one is active.

- **`BuiltinSound`** — a curated list of `SoundEffectID` constants with human-readable display names. Adding a new built-in sound means adding a constant here.

- **`TriggerType` and the per-type filter enums** — `TriggerType` is the event-type discriminator; each type has a set of filter enums that narrow when it fires (e.g. which kind of hitsplat, which stat, gained vs. lost, exact vs. substring match).

- **`SoundTriggersPanel`** — the RuneLite side panel (`PluginPanel`). Holds a scrollable list of `TriggerPanel` cards. Call `rebuild()` after any change to the triggers list.

- **`TriggerPanel`** — a collapsible card per trigger. Builds its own Swing UI inline; calls `plugin.saveTriggers()` on every field change. One type-specific sub-panel per `TriggerType` is built up front; `updateSectionVisibility` toggles exactly one visible to match the selected type when the type combo changes.

- **`SoundTriggersConfig`** — a minimal `@ConfigGroup` interface; the triggers JSON is stored as a hidden config item managed entirely by the plugin, not exposed as a user-facing config panel.

### Persistence flow

Triggers are serialized to JSON via Gson (through `TriggerStore`) and stored in RuneLite's `ConfigManager` under group `soundtriggers`, key `triggers`. They are loaded in `startUp()` and saved immediately on every UI interaction.

The stored value is a **versioned envelope** — `{"version":1,"triggers":[…]}` — rather than a bare array. `TriggerStore.SCHEMA_VERSION` is the current version; it exists as a seam for future data migrations (there are none yet — bump the constant and add a transform when a change can't be made purely additively).

**Schema evolution:** because `SoundTrigger` deserializes through Lombok's no-arg constructor, field initializers run first and Gson only overwrites keys present in the JSON. So *additive* changes are free and need no migration: adding a field (it keeps its declared default in old data), adding a `TriggerType`/enum value, or removing a field (the stale key is ignored). Renames, retypes/unit changes, and removing a `TriggerType` are *not* additive and would need a version bump + migration. Loading is fault-isolated per trigger: an element that fails to deserialize is skipped and logged, so one bad trigger can't take down the rest of the list.

**Schema updates**: at the moment, the plugin is not published, and there are no configurations to migrate from. **Until the plugin is published** we don't need to bump the version number or add migrations for schema changes. Once it is, we can remove this note.

### Non-obvious runtime behaviors

- **Stat priming** — on the first game tick after login, `PLAYER_STAT` triggers sample the current stat values but never fire. This prevents a condition that was already true at login (e.g. HP already below threshold) from firing spuriously on arrival. `statsPrimed` resets to `false` on any non-LOGGED_IN game state change.

### Audio playback

Each trigger's `SoundSource` determines the playback path:

- **`FILE`** — played via RuneLite's injected `AudioPlayer`. Only WAV is supported (enforced by the file chooser filter). `AudioPlayer.play(file, gain)` takes gain in **decibels**.
- **`BUILTIN` / `CUSTOM`** — played via `client.playSoundEffect(soundId, volume)` using `SoundEffectVolume` constants.

All three paths share the same 0–4 volume slider on the trigger; `playTrigger` maps that to the appropriate scale for each path.

## Releasing a new version

1. Bump `version` in `runelite-plugin.properties`.
2. Commit, tag it (`git tag <version>`) and push to the repo.
3. Update the `commit` hash in the plugin hub repo's `sound-triggers` entry to the new HEAD, then commit and push.
