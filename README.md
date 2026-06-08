## Sound Triggers

Play a custom sound when a specific event happens in-game. After enabling the
plugin, open the Sound Triggers panel from the side bar to get started. From
there you can add triggers: give it a name, pick an event type, configure its
sound, and hear it the moment it fires. Each event is filterable. Stack as many
triggers as you like.

### Events

- **Hitsplat** — fires on hitsplats, with the ability to narrow it down by
  who's hit (self, others, or any), who dealt it (you, others, or any), what
  kind it is (damage, block, heal, poison, venom, disease, burn, bleed, prayer
  drain, or any), and optionally a damage value to match
- **Item Drop** — fires when a matching item hits the ground.
- **Chat Message** — fires when a chat line matches a given phrase.
- **Player Seen** — fires when a matching player comes into view.
- **NPC Seen** — fires when a matching NPC comes into view.
- **Status Effect** — fires when you gain or lose status effects: poison,
  venom, disease, etc.
- **Player Stat** — fires when health, prayer, run energy, or special attack
  crosses a threshold you set (drops below or rises above)

### Sources

Each trigger can play one of three kinds of sound:

- **File** — any WAV file on your disk; chosen with a file picker
- **Built-in** — a built-in set of ~50 in-game sound effects (hits, prayers,
  skilling sounds, GE sounds, etc.)
- **Custom** — a raw in-game sound effect ID for anything not in the built-in
  list (see [[https://oldschool.runescape.wiki/w/List_of_sound_IDs]])

### Other features

- Per-trigger enable/disable
- Per-trigger volume control (0–4) for file sounds

Sort of a spiritual successor to bwal96/twenty-one-plugin
