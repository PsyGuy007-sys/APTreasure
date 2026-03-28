<p align="center">
  <img src="logo.png" alt="APTreasure Logo" width="300">
</p>

<h1 align="center">APTreasure</h1>

<p align="center">
  <strong>Runaway Items plugin for Paper 1.21+</strong><br>
  Dropped items come alive, run away from players, and reward you if you catch them!
</p>

<p align="center">
  <a href="https://github.com/PsyGuy007-sys/APTreasure/releases"><img src="https://img.shields.io/github/v/tag/PsyGuy007-sys/APTreasure?label=release&style=flat-square&color=brightgreen" alt="Release"></a>
  <img src="https://img.shields.io/badge/API-Paper%201.21+-blue?style=flat-square" alt="Paper 1.21+">
  <img src="https://img.shields.io/badge/Java-21+-orange?style=flat-square" alt="Java 21+">
  <img src="https://img.shields.io/badge/PlaceholderAPI-supported-green?style=flat-square" alt="PlaceholderAPI">
</p>

---

## What is APTreasure?

When a player drops an item, there's a configurable chance (default 10%) that the item **comes alive**:

- It starts **moving on its own** with random velocities
- It plays **funny sounds** (villager hums, chicken squawks, enderman teleports)
- It spawns **enchantment and smoke particles**
- If a player gets close, it **speeds up to flee** like a scared little animal
- If you manage to **catch it**, you get a **bonus** (doubled stack!) and a victory message

Imagine your auto-farm becoming total chaos — carrots and apples running away in every direction while you sprint after them screaming.

## Features

- **ItemSpawnEvent trigger** with configurable chance (1-30%)
- **Animated items** with random movement, particles, and sounds
- **Flee AI** — items detect nearby players and run away
- **Catch bonus** — configurable stack multiplier (default x2)
- **Lifetime limit** — items return to normal after a duration (default 15s)
- **Server-wide cap** — max active treasures to prevent lag (default 20)
- **Material blacklist** — exclude common items (cobblestone, dirt, etc.)
- **Per-player toggle** (`/aptreasure toggle`)
- **Personal stats** (`/aptreasure stats`)
- **PlaceholderAPI** soft integration (3 custom placeholders)
- **Externalized messages** via `messages.yml` with `&` color codes
- **Adventure API** — no deprecated Bukkit APIs

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/aptreasure reload` | Reload configuration and messages | `aptreasure.reload` |
| `/aptreasure toggle` | Toggle treasure items for yourself | `aptreasure.toggle` |
| `/aptreasure stats` | View your personal catch stats | `aptreasure.stats` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `aptreasure.use` | Use APTreasure commands | `true` |
| `aptreasure.reload` | Reload configuration | `op` |
| `aptreasure.toggle` | Toggle treasure items | `true` |
| `aptreasure.stats` | View personal stats | `true` |
| `aptreasure.immune` | Items you drop never come alive | `false` |

## PlaceholderAPI Placeholders

| Placeholder | Description |
|-------------|-------------|
| `%aptreasure_total_caught%` | Total treasures caught |
| `%aptreasure_enabled%` | Whether treasure items are enabled |
| `%aptreasure_active_count%` | Current alive treasures on the server |

## Installation

1. Download the latest JAR from [Releases](https://github.com/PsyGuy007-sys/APTreasure/releases)
2. Drop it in your server's `plugins/` folder
3. Restart or reload the server
4. (Optional) Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) for placeholder support

## Configuration

<details>
<summary><strong>config.yml</strong> — Chance, duration, bonus, blacklist & more</summary>

```yaml
# Chance (in %) for a dropped item to come alive (1-30)
chance: 10

# Duration in seconds before a treasure becomes normal again
duration: 15

# Bonus multiplier when catching a treasure
bonus-multiplier: 2

# Maximum alive treasures at once (server-wide)
max-active: 20

# Flee detection radius (in blocks)
flee-radius: 5

# Material blacklist
blacklist:
  - COBBLESTONE
  - DIRT
  - GRAVEL
  - SAND
```

</details>

<details>
<summary><strong>messages.yml</strong> — UI messages (catch, reload, toggle, stats)</summary>

```yaml
prefix: "&8[&6APTreasure&8] &r"

catch:
  success: "&a&lNice catch! &eYou captured a runaway treasure! &6+{amount} bonus items!"

reload:
  success: "&aConfiguration reloaded successfully!"

toggle:
  enabled: "&aTreasure items enabled!"
  disabled: "&cTreasure items disabled."

stats:
  header: "&6&l--- Your Treasure Stats ---"
  total-caught: "&eTotal treasures caught: &a{count}"
```

</details>

## Building from source

```bash
git clone https://github.com/PsyGuy007-sys/APTreasure.git
cd APTreasure
mvn clean package
```

The JAR will be in `target/APTreasure-1.0.0.jar`.

## License

MIT
