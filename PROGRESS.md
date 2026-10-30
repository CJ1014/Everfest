# EverFest — Development Progress

Mod ID `holidayseasons` · package `com.holidayseasons` · Minecraft 1.21.1 · NeoForge 21.1.265 · Java 21

## Verified environment

| Component | Version | How it was verified |
|---|---|---|
| NeoForge | 21.1.265 (latest 21.1.x on maven.neoforged.net) | decompiled + compiled by ModDevGradle 2.0.148 |
| Serene Seasons | 10.1.0.9, NeoForge build (Modrinth version id `pHEgQQUE`) | downloaded, `neoforge.mods.toml` read, public API inspected with `javap` |
| GlitchCore | 2.1.0.2, NeoForge build (Modrinth version id `S2TfWrZR`) | required by Serene Seasons `[2.1.0.0,)` |
| Parchment | 2024.11.17 for 1.21.1 | MDK default |

Note: the Modrinth maven resolves the plain version number `10.1.0.9` to the **Forge** jar. The build therefore
uses Modrinth version ids, which pin the NeoForge files.

### Serene Seasons facts that shaped the design
* Public API: `SeasonHelper.getSeasonState(Level)` → `ISeasonState` (cycle ticks, day/sub-season/cycle durations,
  `SubSeason`). No API exists to change cycle length, so EverFest **reads** SS and adapts to it.
* SS has 12 sub-seasons starting at EARLY_SPRING → EverFest maps them 1:1 onto months (EARLY_SPRING = March).
* SS advances its cycle by `dayTime − lastDayTime`, adding 24000 when negative. A backward `/time set` therefore
  moves seasons forward to the next occurrence of that time of day, **but** on a world many days old,
  `/time set day` (absolute 1000) rewinds SS by roughly the world's age in days. EverFest follows SS faithfully in
  SEASON_SYNCED mode; the holiday engine (Milestone 2) is what prevents duplicate celebrations.
* SS's own `/season set <sub_season>` is the only supported way to move its cycle; EverFest's admin
  `/everfest date set` uses it (plus `/time add`) instead of touching SS internals.
* SS is an **optional** dependency: EverFest compiles against it, runs with it in dev, and falls back to
  calendar-derived seasons without it (verified, see below).

## Current milestone: 1 — Foundation ✅ (acceptance criteria verified)

### Completed systems
* **Project**: ModDevGradle build, Gradle 8.14.3 wrapper, mod metadata, optional SS dependency, JUnit 5,
  run configs `client`, `server`, `clientA`/`clientB` (multiplayer test clients with separate game dirs that
  auto-join `localhost`), `gameTestServer`, `data`. `-PnoSereneSeasons` runs without the integration.
* **Calendar core** (`calendar`, pure Java, no Minecraft imports):
  * `GregorianCalendarSystem` (java.time, leap years), `FixedMonthCalendarSystem` (12 configurable month
    lengths, configurable leap interval/month, 7-day weeks).
  * Gregorian → native mapping so holidays keep their meaning in short months (Halloween = last day of an 8-day
    October, Christmas = Dec 7 of 8).
  * `ClockMath`: tick-delta clock with `TimeJumpPolicy` (FORWARD_TO_NEXT_TIME_OF_DAY default, KEEP_DATE, REWIND),
    cycle-year tracking, wall-clock conversion. Days roll at Minecraft dawn (dayTime 0), matching vanilla and SS.
  * `SeasonCycleMapper`: SS cycle position → EverFest date.
  * `CalendarSettings`: validated config with problem reporting (never throws on bad config).
* **Calendar modes** (`CalendarService`):
  * `SEASON_SYNCED` — SS year split into 12 months; EverFest counts years (SS does not). Without SS it runs its
    own uniform calendar and **continues from the last SS date** instead of jumping.
  * `CUSTOM` — own long year (default Gregorian month lengths, leap every 4 years); warns if SS sub-season length
    doesn't match the average month.
  * `REAL_WORLD` — server timezone (`realWorldTimezone`), admin date override and day offset.
* **Persistence**: `holidayseasons_calendar.dat` (overworld `SavedData`).
* **Serene Seasons adapter**: `seasons.SeasonAdapter` interface + `compat.sereneseasons.SereneSeasonsAdapter`
  (only class-loaded when SS is present; failures are logged once and fall back).
* **Config**: `ServerConfig` (calendar, seasons sync policy, performance) and `ClientConfig` (HUD, clock,
  notifications). Built-in NeoForge config screen registered. Live reload re-applies settings.
* **Networking**: `CalendarSyncPayload` (on login, on every date change, and every `clientSyncIntervalTicks`),
  `OpenScreenPayload` (server-driven screen opening so blocks work on dedicated servers).
* **Client**: `CalendarScreen` (month grid, weekday alignment from the real calendar system, today marker,
  season colour band, month navigation incl. leap years, upcoming-holiday sidebar ready for M2), optional date HUD.
* **Wall Calendar**: craftable (string + paper + ink sac), wall-mounted block that breaks if its support is removed,
  opens the calendar; the item opens it when used in the air. Original procedural textures (`tools/gen_textures.py`).
* **Commands**: `/everfest calendar`, `/everfest date [info]`, operator-only `/everfest date set|add|clearoverride`.

### Test results
* **Unit tests** — 27/27 passing (`./gradlew test`): Gregorian parity with java.time over 30k days, leap years,
  uniform/custom/leap round-trips, strictly increasing dates, Gregorian mapping monotonicity, weekday cycle,
  ISO parse/format, time-jump policies (sleep, `/time add`, `/time set day` on an old world), wall clock,
  cycle-year wrap tracking, SS cycle → date mapping.
* **Dedicated server** (`./gradlew runServer`, driven over RCON with `tools/rcon.py`) — verified:
  * starts with SS integration; date March 1, Year 1 at SS cycle start;
  * `/time add 24000` and `48000` advance 1 and 2 days;
  * `/everfest date set 0001-10-08` → Mid Autumn, SS `season get` agrees; `0002-01-03` → Mid Winter day 3/8;
    `0001-10-31` correctly rejected (8-day months);
  * **restart**: date (incl. EverFest's year counter) identical after `stop` + relaunch;
  * CUSTOM mode via live config edit: mismatch warning logged, `/time set day` moves *forward* to next morning,
    Year 4 Feb 29 valid → +1 day = Mar 1, Year 3 Feb 29 rejected;
  * REAL_WORLD with `America/New_York`: wall time matched `TZ=America/New_York date`; override → Dec 25,
    `+7` → Jan 1 2027, clear → today, Feb 30 rejected;
  * **without Serene Seasons** (`-PnoSereneSeasons`): loads cleanly, seasons from calendar, date continues from the
    last SS date (Feb 4, Year 2 before and after).
* **Real client** (Xvfb + Mesa llvmpipe, `runClientA`) — joined the dedicated server; calendar screen rendered and
  screenshotted; date HUD rendered.
* **Multiplayer sync** — two clients (`FestTesterA`, `FestTesterB`) on the dedicated server; console
  `/everfest date set` changed both HUDs to the same date/minute (Oct 6 → Dec 7 → Feb 4) within one sync.

### Known issues / limitations (M1)
* No sound device exists in the CI container, so the client runs with sound disabled; audio is untested so far.
* In SEASON_SYNCED mode, `/everfest date add <negative>` is refused because SS cannot run backwards through a
  supported interface (use `date set`). `date add`/`set` rely on SS applying `/time add` on its next tick, and on
  SS's `progress_season_while_offline` (default true) when no players are online.
* NeoForge's built-in config screen cannot edit SERVER configs while connected to a remote server; an
  operator-only in-game admin screen is planned (Milestone 8). Operators can use `/everfest` commands meanwhile.

## Next tasks
* Milestone 2 — holiday engine: data-driven holiday definitions, date rules (fixed, nth weekday, Easter, season
  relative), idempotent persisted lifecycle, scheduler on the scheduling calendar (respecting `syncPolicy`),
  per-holiday config, `/everfest holiday` commands, client holiday sync, unit tests + GameTests.
