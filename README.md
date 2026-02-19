# A False Memory

> *Your world remembers everything.*

A psychological horror mod for Minecraft (NeoForge 1.21.1).

---

## What Is This?

You play. You build. You survive.

Something is watching.

No jump scares. Just dread.

---

## Installation

1. Install [NeoForge 1.21.1](https://neoforged.net/)
2. Install [Cloth Config](https://modrinth.com/mod/cloth-config) for in-game config screen
3. Drop `falsememory-A-1.0.jar` into your `mods/` folder
4. Play alone

---

## Configuration

Config file: `config/falsememory-common.toml`

Settings are also available in-game via the Mods screen if Cloth Config is installed.

| Setting | Default | Description |
|---|---|---|
| `enabled` | `true` | Master toggle |
| `grace_period_days` | `3` | In-game days before anything starts |
| `intensity` | `5` | Horror intensity (1-10) |
| `frequency` | `5` | Events per night (1-10) |
| `enable_chat_events` | `true` | Chat-based events |
| `enable_block_events` | `true` | Block-based events |
| `enable_memory_book` | `true` | Book and inventory events |
| `enable_only_one` | `true` | Final confrontation |
| `enable_fourth_wall` | `true` | Real-world clock and username references |
| `enable_sleep_horror` | `true` | Sleep-based events |
| `enable_silent_watcher` | `true` | Watcher entity event |
| `enable_escalating_notes` | `true` | Multi-day note sequence |
| `manhunt_speed_multiplier` | `1.0` | Entity speed during manhunt phase |

---

## Recommended Setup

- Singleplayer
- Hard difficulty
- No cheats
- Don't read ahead

---

## Development

### Build

```bash
./gradlew build
```

### PIN

Debug PIN : Just search in the code man..

### Commands

| Command | Description |
|---|---|
| `/fm pin <code>` | Unlock debug session |
| `/fm lock` | Lock session |
| `/fm seed` | Inject full fake memory data |
| `/fm test all` | Run all four phase tests in sequence |
| `/fm test <1-4\|combat\|sleep>` | Run a specific phase test |
| `/fm checklist` | Full QA checklist |
| `/fm memory` | Dump all tracked memory data |
| `/fm data` | Detailed data summary by category |
| `/fm tier <0-3>` | Force knowledge tier |
| `/fm day <n>` | Set tracked day counter |
| `/fm event <name>` | Trigger a single event by name |
| `/fm spawn <entity>` | Spawn entity (obsessed / onlyone / witness / lostmemories) |
| `/fm structure <type>` | Spawn structure (camp / archive) |
| `/fm ending <type>` | Trigger ending (victory / defeat / draw) |
| `/fm manhunt <on\|off>` | Toggle manhunt phase |
| `/fm falsevictory` | Test false-victory grace window |
| `/fm reset` | Wipe all memory data |
| `/fm debug <action>` | Low-level debug actions |

### Lost Memories Dimension

Access via `/fm spawn lostmemories` once debug is unlocked.

If the dimension is registered in the world, the player is teleported in and a memory replay runs — the level reconstructs a version of the player's base from tracked block data. Return position is stored automatically and the player is sent back when the replay ends.

If the dimension is not loaded (world created without the dimension JSON), the mod falls back to a text experience in the overworld, pulling from the player's memory data directly. Both paths work.

To register the dimension properly the world needs `data/falsememory/dimension/lost_memories.json` and a matching dimension type file. Until those are added the fallback handles it.

### Testable Events

```
whisper       blockshift    torch         footsteps
footsteps2    footsteps3    book          join
death         chat          replica       stalks
echo          predictive    invasion      intel
sign          chest         sleep         standing
pastblock     inventory     session       helditem
4thwall       sysmsg        watcher       prechchest
anticipate    escalate      echochat      deathclone
mining        rooftop       wakeroom
```

### Debug Actions

```
obsessed_visible    obsessed_invisible    clear_traps
clear_events        clear_positions
```

### Recommended Test Flow

```
/fm pin 5545
/fm seed
/fm tier 2
/fm test all
/fm checklist

/fm event watcher
/fm event escalate
/fm event escalate
/fm event escalate
/fm event 4thwall
/fm event deathclone

/fm test combat
/fm falsevictory
```

---

*Your world knows more than it should.*