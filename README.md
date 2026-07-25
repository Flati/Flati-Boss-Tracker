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
| **API key** | Shared secret for your GIM group. Your group admin provides this. |
| **Boss kill endpoint** | `POST` URL for individual kill events (default: `https://flati.is/api/osrs/boss-kill`) |
| **KC update endpoint** | `POST` URL for single boss KC updates (default: `https://flati.is/api/osrs/kc-update`) |
| **KC sync endpoint** | `POST` URL for bulk Boss Kill Log sync (default: `https://flati.is/api/osrs/kc-sync`) |

Authentication uses `Authorization: Bearer <api-key>`.

### First-time KC sync

1. **Fast (recommended):** open **Boss Kill Log** from your Ring of Wealth. KC for all tracked bosses is synced automatically.
2. **Gradual:** browse boss pages in the **Collection Log** — KC is synced per page as you view it.

After the first sync, kill events and KC updates are sent automatically while you play.

## Building from source

Requires **JDK 11 or newer** (the plugin targets Java 11 bytecode). Gradle is included via the wrapper — you do not need Gradle installed globally.

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

| Endpoint | Payload | Purpose |
|----------|---------|---------|
| `POST …/boss-kill` | player, boss, killCount, killedAt | Individual kill event |
| `POST …/kc-update` | player, boss, killCount, updatedAt | Single boss KC change |
| `POST …/kc-sync` | player, entries[], updatedAt | Bulk sync from Boss Kill Log |

All requests require a valid group API key in the `Authorization` header.

The reference implementation and dashboard live in the [flati.is](https://flati.is/osrs) project. Self-hosters can point the endpoint settings at their own server implementing the same contract.

## Contributing / Plugin Hub

To submit changes to the official Plugin Hub, open a pull request against [runelite/plugin-hub](https://github.com/runelite/plugin-hub) following their [contribution guide](https://github.com/runelite/plugin-hub#plugin-hub).

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Kills not appearing on the dashboard | Check API key and endpoint URLs in plugin settings; enable debug logging and check `%USERPROFILE%\.runelite\logs\` (Windows) or `~/.runelite/logs/` |
| KC out of date | Open Boss Kill Log or browse Collection Log to trigger a sync |
| `'gradlew' is not recognized` (Windows) | Use `.\gradlew.bat` with the `.\` prefix |
| `JAVA_HOME is not set` | Install JDK 11+ and set `JAVA_HOME` to the JDK install path |
| Build fails on Java 26+ | Use the included Gradle wrapper; do not use an older global Gradle install |
| Plugin missing in normal RuneLite | Install from Plugin Hub; sideloaded JARs do not work with the Jagex Launcher |
| Dev client login issues | See [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) |
| `NoClassDefFoundError: …FullScreenAdapter` | Remove any JAR from `sideloaded-plugins` and use `gradlew run` instead |

## License

See repository license file if present. Not affiliated with Jagex or RuneLite.
