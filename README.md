# Flati Boss Tracker

A [RuneLite](https://runelite.net/) plugin that syncs **boss kill counts** and **kill events** from Old School RuneScape to a web backend. Built for **Group Ironman** teams, but works for any group that shares an API key.

The default backend is [flati.is](https://flati.is/osrs), which provides a shared KC dashboard and kill timeline per group. All API endpoints are configurable if you run your own server.

## Features

- **Kill events** — logs boss kills from in-game chat KC messages (one event per kill)
- **KC sync** — keeps boss kill counts up to date via:
  - **Boss Kill Log** (Ring of Wealth → Features) — bulk sync when the log is opened
  - **Collection Log** — incremental sync while browsing boss pages
- **Offline retry** — failed requests are queued locally and retried on next login
- **Login reminder** — optional nudge when KC has not been synced recently

## Install (recommended)

Install **Flati Boss Tracker** from the RuneLite **Plugin Hub** (Configuration → Plugin Hub → search for the plugin name).

The official RuneLite launcher does not load custom JARs from disk; Plugin Hub is the supported way to use third-party plugins with a Jagex account.

## Configuration

Open **RuneLite → Configuration → Flati Boss Tracker**.

| Setting | Description |
|---------|-------------|
| **Enable external sync** | Master opt-in for sending data to the backend (off by default). Shows a third-party server warning when enabled. |
| **API key** | Shared secret for your GIM group. Your group admin provides this. |
| **Boss kill endpoint** | `POST` URL for individual kill events (default: `https://flati.is/api/osrs/boss-kill`) |
| **KC update endpoint** | `POST` URL for single boss KC updates (default: `https://flati.is/api/osrs/kc-update`) |
| **KC sync endpoint** | `POST` URL for bulk Boss Kill Log sync (default: `https://flati.is/api/osrs/kc-sync`) |
| **Sync KC from chat** | Parse kill-count chat messages and send kill events (off by default) |
| **Sync KC from Collection Log** | Sync KC when viewing Collection Log boss pages (off by default) |

Authentication uses `Authorization: Bearer <api-key>`.

### First-time KC sync

1. Enable **Enable external sync** and enter your group **API key**.
2. Enable **Sync KC from chat** and/or **Sync KC from Collection Log** as needed.
3. **Fast (recommended):** open **Boss Kill Log** from your Ring of Wealth. KC for all tracked bosses is synced automatically.
4. **Gradual:** browse boss pages in the **Collection Log** — KC is synced per page as you view it.

After the first sync, kill events and KC updates are sent automatically while you play (when sync is enabled).

## Building from source

Requires **JDK 11 or newer** to run Gradle. The build uses a **Java 11 toolchain** for compilation (matching Plugin Hub CI). If you only have a newer JDK installed (e.g. Java 26), install [Eclipse Temurin 11](https://adoptium.net/) — Gradle will auto-detect it for compilation.

**Windows:**

```powershell
.\gradlew.bat shadowJar
```

**Linux / macOS:**

```bash
./gradlew shadowJar
```

Output JAR:

```text
build/libs/flati-boss-tracker-1.0.0-all.jar
```

### Running a development client

For local development, use the Gradle `run` task. This starts a RuneLite dev client with the plugin loaded:

```bash
./gradlew run        # Linux / macOS
.\gradlew.bat run    # Windows
```

**Jagex account login in dev mode:** the dev client cannot use the Jagex Launcher OAuth flow directly. Follow the official RuneLite wiki guide [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) to export credentials once via `--insecure-write-credentials`, then remove that flag when done.

Do **not** place a shadow JAR in `%USERPROFILE%\.runelite\sideloaded-plugins\` while using `gradlew run` — the dev client loads the plugin from source, and a stale sideloaded JAR can cause startup errors.

## Backend API

The plugin sends JSON to three endpoints. A compatible backend should accept:

| Endpoint | Purpose |
|----------|---------|
| `POST …/boss-kill` | Individual kill event |
| `POST …/kc-update` | Single boss KC change |
| `POST …/kc-sync` | Bulk sync from Boss Kill Log |

All requests use `Content-Type: application/json` and require a valid group API key:

```http
Authorization: Bearer <api-key>
```

### Example requests

**`POST /api/osrs/boss-kill`** — sent when a kill-count chat message is parsed:

```json
{
  "playerName": "Flatmundur",
  "bossName": "Vorkath",
  "killedAt": "2026-07-24T14:32:05Z",
  "killCount": 142
}
```

**`POST /api/osrs/kc-update`** — sent when KC changes (chat, Collection Log):

```json
{
  "playerName": "Flatmundur",
  "bossName": "Vorkath",
  "killCount": 142,
  "updatedAt": "2026-07-24T14:32:05Z"
}
```

**`POST /api/osrs/kc-sync`** — sent when Boss Kill Log is opened:

```json
{
  "playerName": "Flatmundur",
  "source": "boss_log",
  "updatedAt": "2026-07-24T14:35:00Z",
  "entries": [
    { "bossName": "Vorkath", "killCount": 142 },
    { "bossName": "Zulrah", "killCount": 89 },
    { "bossName": "Theatre of Blood", "killCount": 12 }
  ]
}
```

Timestamps are ISO-8601 UTC strings. `bossName` values are normalized (e.g. `"Theatre of Blood"`, not `"The Theatre of Blood"`).

The reference implementation and dashboard live in the [flati.is](https://flati.is/osrs) project. Self-hosters can point the endpoint settings at their own server implementing the same contract.

## Contributing / Plugin Hub

To submit changes to the official Plugin Hub, open a pull request against [runelite/plugin-hub](https://github.com/runelite/plugin-hub) following their [contribution guide](https://github.com/runelite/plugin-hub#plugin-hub).

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Kills not appearing on the dashboard | Enable **Enable external sync**, check API key and endpoint URLs; enable debug logging and check `%USERPROFILE%\.runelite\logs\` (Windows) or `~/.runelite/logs/` |
| KC out of date | Enable sync settings, then open Boss Kill Log or browse Collection Log |
| `'gradlew' is not recognized` (Windows) | Use `.\gradlew.bat` with the `.\` prefix |
| `JAVA_HOME is not set` | Install JDK 11+ and set `JAVA_HOME` to the JDK install path |
| Build fails on Java 26+ | Install JDK 11 (Temurin) alongside your system JDK; the build toolchain targets Java 11. Plugin Hub CI uses JDK 11 regardless of your local Gradle version. |
| Plugin missing in normal RuneLite | Install from Plugin Hub; sideloaded JARs do not work with the Jagex Launcher |
| Dev client login issues | See [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) |
| `NoClassDefFoundError: …FullScreenAdapter` | Remove any JAR from `sideloaded-plugins` and use `gradlew run` instead |

## License

See repository license file if present. Not affiliated with Jagex or RuneLite.
