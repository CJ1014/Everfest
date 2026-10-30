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

## Milestone 1 — Foundation ✅ (acceptance criteria verified)

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

## Milestone 2 — Holiday engine ✅ (acceptance criteria verified)

### Completed systems
* **Date rules** (`holiday.rule`, pure): `fixed:MM-DD` (Gregorian, scaled to native months), `nth_weekday:MM:day:N`
  (N = -1 for last; missing 5th → last), `easter:OFFSET` (Meeus/Jones/Butcher computus), `native:MM-DD`,
  `season:sub_season:FRACTION`. Text form round-trips and is used in JSON and config.
* **Data-driven holidays**: `data/<ns>/everfest/holiday/<name>.json`, DFU codec (`HolidayDefinition`) with date rule,
  duration, preparation, cleanup, enabled, seasons, colour, behaviours, decorations, activities, ambience, rewards,
  special events and free-form settings. Invalid files are logged and skipped. Datapacks can add or replace holidays.
* **Built-in holidays**: Halloween (Oct 31), Christmas (Dec 25), Easter (computus), New Year's (Dec 31 + Jan 1, for the
  midnight countdown), Valentine's Day (Feb 14), Thanksgiving (4th Thursday of November), Independence Day (Jul 4,
  **disabled by default**).
* **Server config** `[holidays]`: per built-in holiday `enabled`, `dateRule` override, `preparationDays`,
  `durationDays`, `cleanupDays`, `intensity`; plus `announceHolidays`, `disabledHolidays` (datapack ids),
  `historyYears`. Config and datapack reloads re-schedule live.
* **Lifecycle engine** (`HolidayEngineCore`, pure): INACTIVE → PREPARING → ACTIVE → CLEANUP → COMPLETED.
  Deterministic and idempotent. Occurrences are keyed by holiday + calendar signature + year. Forward jumps emit every
  intermediate phase flagged `transientStep`; jumping completely past an occurrence never starts it; COMPLETED is
  terminal; rewinding before preparation ABORTs a live occurrence to INACTIVE; an occurrence that has already been
  celebrated never prepares or activates again; disabled/removed holidays and calendar-system changes ABORT live
  occurrences; `REQUIRE_MATCHING_SEASON` SKIPs out-of-season occurrences; durations are clamped in tiny calendars.
* **Scheduling** respects `syncPolicy`: FOLLOW_SEASON schedules on the Serene Seasons cycle.
* **Persistence**: `holidayseasons_holidays.dat`; history pruned after `historyYears`.
* **Events**: `HolidayPhaseChangedEvent` (game bus) for every transition — the hook Milestone 3+ systems use.
* **Players**: chat announcements + client toasts on preparation/start; calendar shows preparation days and holiday
  days with tooltips; sidebar countdowns; `HolidayStatusPayload` sync on login and on change.
* **Commands**: `/everfest holiday list|info <h>`; operators: `goto <h> [prepare|start]`, `reset <h>`, `finish <h>`.
* **Fix found by GameTest logs**: Serene Seasons date changes take two ticks (season set, then clock delta); date
  events are now held until SS reaches the target so holidays never react to the intermediate date.

### Test results
* **Unit tests — 50/50 passing** (now run inside the FML JUnit environment via ModDevGradle `unitTest`):
  computus for known years + every year 1900–2399 is a Sunday in range; Thanksgiving 2024–2026; last/5th weekday;
  scaled dates; rule parse/describe round trips and rejection of invalid rules; all 7 bundled JSON files parse;
  window phases; day-by-day progression emits each phase once and is idempotent; jump past / jump into;
  rewind after completion; rewind-abort-replay never re-prepares or re-activates; year-boundary occurrence;
  disable / calendar change / season mismatch; tiny calendar clamping; pruning; **20,000-step randomized time
  travel across 4 holidays: every occurrence activated at most once**.
* **GameTests — 4/4 passing** (`./gradlew runGameTestServer`): wall calendar breaks when its support is removed;
  wall calendar cannot be placed floating; full lifecycle in a live world via real Serene Seasons date changes
  (PREPARING → ACTIVE → rewind ABORT → replay COMPLETED, ACTIVE entered exactly once, records survive NBT
  save/load); `/time add 24000` advances exactly one calendar day.
  Note: Gradle's `runGameTestServer` exits 0 even if mod loading crashes — always check the log for
  `All N required tests passed`.
* **Dedicated server + client**: `/everfest holiday list` correct for the 96-day year; `goto halloween prepare` →
  PREPARING; **restart produced zero transitions** and Halloween stayed PREPARING; on the client, `goto halloween
  start` showed the chat announcement, toast and HUD; calendar showed prep days, holiday marker and countdowns.

### Known issues / limitations (M2)
* Holiday occurrences that fall in the past when a world first loads are displayed as COMPLETED but were never
  celebrated (by design — no retroactive celebrations).
* `/everfest holiday goto` is refused under FOLLOW_SEASON when the scheduling calendar differs from the displayed one.

## Milestone 3 — Autonomous villages ✅ (acceptance criteria verified)

### Completed systems
* **Brain integration** (`villager.HolidayBrainInjector`, `ai.CelebrationBehavior`): verified in the 1.21.1 source that
  `Brain.addActivity*` *appends* behaviours and only overwrites the activity's entry requirements, so EverFest adds one
  `CelebrationBehavior` instance each to IDLE, PLAY and MEET while re-supplying vanilla's exact requirements (MEET still
  needs a meeting point). WORK/REST/PANIC/CORE/RAID/HIDE are never touched. Re-injected after `refreshBrain`
  (profession change, growing up) by tracking brain identity. The behaviour yields to panic, sleep, trading, raids,
  work and rest.
* **Festival schedules** (`villager.HolidaySchedules`, registered `Schedule`s): on ACTIVE days only, and only replacing
  vanilla's own default schedules (never another mod's): adults get a short work shift then festivities; a
  New-Year night-party variant; children get more play time and a Halloween dusk variant. Schedules are not saved,
  so a restart or holiday end always returns villagers to vanilla schedules.
* **Personalities** (`VillagerPersonality`): deterministic from UUID + profession + age (sociability, generosity,
  enthusiasm, preferred activity, gift preference, costume). Professions shape them (clerics social, farmers
  feast/scarecrow, librarians book gifts, nitwits enthusiastic); children are more enthusiastic.
* **Activity picking** (`ActivityPicker`, pure): phase × holiday behaviours × personality × profession × age ×
  config intensity. Children never get adult jobs; adults leave trick-or-treating and egg hunts to children.
* **Tasks** run through vanilla walk/look memories so vanilla pathfinding moves villagers: DECORATE (visibly carries
  the item, places it, particles + sounds), GATHER (go to the square, dance/hop, note particles, celebrate sounds),
  SOCIALIZE (pair up, face each other, chat, hearts), FEAST (hold the dish, bites with item particles + eat sounds),
  FIREWORKS (rockets without explosion data so they can never hurt anyone; bursts are drawn with particles),
  TIDY_UP (take decorations down during cleanup). Unreachable/stuck targets fail cleanly.
* **Director** (`CelebrationDirector`): villagers tracked through join/leave events (never scanned), a configurable
  per-tick villager budget (default 6) processed round-robin, one village updated per tick.
* **Villages**: identified by their bell (meeting point) or a 64-block cell; persistent `VillageRecord`s keep abstract
  progress for unloaded villages; style from the villagers' `VillagerType` (plains, desert, savanna, snow, taiga,
  jungle, swamp → materials). Only real villages (bell or ≥4 villagers) decorate.
* **Decorations** (`decoration`): themes for halloween, christmas, easter, new_years, valentines, harvest,
  independence_day, adapted per biome style, incl. composite centrepieces (Christmas tree with ornaments, pumpkin heap,
  scarecrow, feast table, flower arch, hearts, flags, candy canes). Built from **block-display entities** (never replace
  a block, no collision, drop nothing → no farming exploit) plus invisible `minecraft:light` blocks only into air
  (lights the square, spawn-proofs it).
* **Decoration ledger** (`DecorationLedger`): every entity and block change recorded; staggered removal over the
  cleanup days; ABORT/COMPLETED expires immediately; a light block is removed only if it is still exactly the light
  EverFest placed (a player's block is never touched); display entities carry their ledger id and are rejected on load
  if unknown or expired (crash safety); unloaded chunks are never force-loaded — due entries wait for their chunk.
* **Unloaded-village reconciliation**: when a village returns after ≥1 day away during a live holiday, the
  decorations its villagers would have made are put up (bounded per tick, retried).
* **Serene Seasons day alignment**: `/season set` resets SS to the start of a day while the world is mid-day, which
  made SS days roll over at odd hours. EverFest dates now roll over at world dawn using the SS day that overlaps the
  world day most (identical to SS when its clock is aligned); `date set` compensates when run after noon.
* **Diagnostics**: `/everfest villages` (operators): festival, tracked villagers, tasks by type, villagers on festival
  schedules, decorations per village vs target.

### Test results
* **Unit tests — 61/61 passing**, new: personality determinism and ranges, every costume occurs, children more
  enthusiastic; children trick-or-treat and adults hand out candy; gift exchange only where defined; phase→task
  mapping; professions shift weights; nitwits participate; zero intensity disables participation; decoration ramp and
  staggered expiry; SS dawn alignment (aligned = SS day, misaligned rolls at dawn, date-set compensation for every
  time of day and target day).
* **GameTests — 11/11 passing**, new: vanilla behaviour counts preserved (+1 on IDLE/MEET/PLAY only, others identical),
  idempotent injection and re-injection after `refreshBrain`; villager walks to the festival spot and is released with
  empty hands and an untouched non-vanilla schedule; vanilla panic interrupts a running celebration; villager visibly
  carries and places a decoration (displays + light, floor untouched) and cleanup removes it; cleanup keeps a player's
  block placed over a decoration light; blocks and floating spots are never used; unknown decoration entities are
  rejected while other displays are untouched.
* **Live world (dedicated server + client, savanna village)**:
  * Halloween PREPARING: villagers autonomously carried and placed pumpkins, carved pumpkins, bale-and-lantern,
    pumpkin patch; decoration count ramped to the day's target, then villagers switched to socializing.
  * Halloween ACTIVE: all 10 villagers on festival schedules, gathering at the square.
  * Player left (village unloaded, 59 entries pending, nothing force-loaded); date moved past Halloween into
    Thanksgiving preparation; on return the expired Halloween decorations were removed as chunks loaded and the
    village caught up its Thanksgiving decorations.
  * Thanksgiving ACTIVE: the village gathered around the feast table (GATHER/FEAST/SOCIALIZE).
  * Date moved to a quiet day (Jan 6): **0 decorations, 0 tasks, 0 villagers on festival schedules** — back to vanilla.
  * During work hours and at night no tasks were assigned (villagers at work / asleep).

### Bugs found and fixed during M3 verification
* Ledger wrote its entry *after* spawning the display, so its own join guard rejected the decoration (GameTest).
* Villagers kept holding a placed decoration until the next brain tick (GameTest).
* Festival cache never computed: `now - Long.MIN_VALUE` overflow (live console).
* `holiday goto` targeted an already-celebrated occurrence (live).
* Lone wandering villagers formed "villages" and decorated (live screenshot).
* Catch-up placement consumed slots on failed spot searches (live).
* Serene Seasons day boundary drift after `/season set` (live).
* Activity picker never offered TIDY_UP (unit test).

### Known issues / limitations (M3)
* Holiday-specific child activities (trick-or-treat, egg hunts), gift exchange and home visits fall back to gathering
  until Milestones 4–6 implement them.
* Biome styles other than savanna were verified only in code, not visually in a live village yet.
* In SEASON_SYNCED mode `/time set` on an old world makes Serene Seasons rewind by the world's age (SS behaviour); EverFest
  handles it safely (aborts/never re-celebrates) but players may be surprised.

## Milestone 4 — Halloween playable release ✅ (acceptance criteria verified)

### Completed systems
* **Candy**: Candy Corn, Caramel Apple, Chocolate Bar (speed), Lollipop, Gummy Bat (jump) — fast, always-edible food,
  `#holidayseasons:candy` tag. **Trick-or-Treat Bag**: a bundle that only accepts candy.
* **Costumes** (8, wearable on the head by players *and* villagers via vanilla's head layer — no renderer hacks): Pumpkin
  Mask, Ghost Sheet, Skeleton Mask, Witch Hat, Vampire Collar, Scarecrow Hat, Bat Ears, Black Cat Ears; 3D JSON models.
  Villagers get their personality's costume on the last preparation day and Halloween (children always, adults by
  enthusiasm); costumes are marked temporary, never dropped, never replace an existing hat, removed afterwards.
* **Candy Bowl**: stores candy only; right-click with candy to fill; empty hand to take one; owners take freely,
  visitors are limited per day (`candyPerVisitorPerDay`); "owner only" homes; empty-bowl feedback (sound, smoke,
  message); fill level visible (4 models); comparator output; hoppers can refill but never drain it (anti-grief);
  serves village trick-or-treaters and credits the owner's reputation.
* **Doorbell**: chime, 16-tick redstone pulse like a button, tells the home owner who is at the door; children ring
  it when they visit.
* **Holiday Hearth** (home registration): owner, name (from an anvil-renamed hearth or the settings screen),
  entrance auto-detected as the outside of the nearest door, visitor toggle, villager-decoration opt-in, candy policy;
  validation (max homes per player, minimum distance to other players' homes); only the owner/operators can break it
  or edit it (server-validated packets); villagers never decorate inside homes unless allowed. `/everfest home
  list|edit`, operator `/everfest home register <pos> <player>`.
* **Trick-or-treating**: children form small groups (leader + followers mirroring reactions), choose a reachable door
  that this group hasn't visited and that hasn't hit `maxVisitsPerHouse` (villager houses via their beds' doors,
  deduplicated per door, plus registered player homes), walk there, knock or ring the doorbell, wait, and get candy
  from an adult standing at that door, a candy bowl, the home owner (players hand candy by using it on a child), or
  — for villager houses — the residents (deterministic per house). Otherwise a harmless trick: a puff of smoke,
  giggles and, only where decorations are allowed, a temporary cobweb/pumpkin via the ledger. Reactions: candy shown
  in hand, hops, particles, celebrate sounds. Visits persist per occurrence. Children go home at dusk (Halloween
  dusk schedule) or when Halloween ends. Destination search is bounded (4 path checks) and retried, never looping.
* **Adults hand out candy** at their own front doors (or the festival square) holding candy.
* **Pumpkin carving**: Carving Knife carves vanilla pumpkins (dropping seeds) into 5 faces (grin, scary, silly, cat,
  ghost), re-carving cycles faces; a torch lights them into matching jack-o'-lanterns (light 15).
* **Decor**: Tombstone, Hanging Ghost (needs Ectoplasm, an exclusive Halloween drop).
* **Creatures** (only on Halloween nights, spawned by EverFest's own budgeted spawner, capped per player, in the dark):
  *Ghost* (drifts to a player, swipes one candy or nips for half a heart, then flees; shuns light; gives up after 30 s),
  *Gourdling* (weak hopping pumpkin, bursts into seeds, sometimes candy), *Scarecrow* (harmless; turns and shuffles
  only while nobody looks). They fade away at dawn and are removed on load outside Halloween night. Spawn eggs too.
* **Atmosphere** (client-only, configurable intensity): Halloween-night fog pulled in with a violet tint, wisps in dark
  places, falling leaves in autumn, distant bells / eerie wind / whispers.
* **Audio**: 16 original sound effects synthesized with numpy (`tools/gen_sounds.py`): doorbell, knock, candy,
  empty bowl, ghost moan/hurt/death, distant bell, eerie wind, whispers, gourdling hop/hurt/death, scarecrow
  rustle/creak, trick poof. Verified by spectrogram (no audio device in the container to listen).
* **Reputation** (foundation for Milestone 8): per player per village, diminishing returns for repeating an action on
  the same day (anti-farming), tiers. Candy given +, tricks played on you −.
* **Assets**: all textures generated by `tools/gen_textures.py` (deterministic), all JSON by `tools/gen_assets.py`.

### Test results
* **Unit tests — 64/64** (new: reputation gains, diminishing returns, bounds and tiers).
* **GameTests — 20/20, three consecutive runs green**: bowl candy-only, fill level, hopper insert-but-never-extract,
  one candy per visitor per day, children served; empty bowl gives nothing; doorbell pulse powers its wall and
  releases; carving (grin → scary), torch lighting, light 15, torch consumed; hearth entrance outside the door,
  too-close and too-many rules, decoration protection and opt-in, blank names ignored; costumes match personality,
  temporary, never replace a hat, removed afterwards; **full trick-or-treat run**: a group walks to a registered
  home, knocks, is served from the bowl (one candy per child), remembers the visit, the home counts it, hands empty
  afterwards; players hand candy to children (+reputation, adults unaffected, only candy); Halloween creatures
  rejected outside Halloween night, spawn-egg creatures untouched.
* **Live (dedicated server + client, savanna village)**:
  * last preparation day / Halloween: nine villagers in costumes (ghost, skeleton, vampire, bat, pumpkin…), incl.
    the cleric and the nitwit; all 8 costume models verified in a line-up screenshot;
  * trick-or-treat groups formed by themselves, visited the player's home and were served from its candy bowl (bowl
    40 → 38 → 35) until the home reached its 2-visit cap; groups then visited villager house doors with treats and
    the occasional trick; adults stood at their doors handing out candy; children went home at dusk;
  * Halloween night: fog and creatures (ghost, gourdling, scarecrow) rendered; natural spawns are sparse by design;
  * hearth settings screen rendered with detected entrance and visit count.

### Bugs found and fixed during M4 verification
* Mock server players crash on Serene Seasons' sync packet (test harness) → tests use non-networked mock players.
* Trick-or-treat leader judged reachability before landing (navigation refuses while airborne) → bounded retries.
* Witch hat model failed to load: element rotation 11.25° is not allowed (only ±22.5/±45).
* **Children never joined their village**: vanilla only lets adults remember the bell → children resolve to the
  nearest bell village (live test showed groups wandering without houses to visit).
* Texture generator used Python's randomized `hash()` → deterministic CRC32.
* Hearth screen text overlapped buttons → taller panel.
* Panic GameTest was flaky with a synthetic memory → uses a (frozen) zombie so vanilla's own sensor triggers panic.

### Known issues / limitations (M4)
* No audio playback is possible in the container: sounds are verified structurally (spectrograms), not by ear.
* Halloween merchants, haunted structures, mysterious visitors and festival minigames belong to Milestone 7.
* Natural creature spawning is intentionally rare (default 8%/s/player attempt, cap 4); tune in `[halloween]`.
* The candy bag's bundle filtering is not covered by an automated test yet.

## Milestone 5 — Christmas ✅ (acceptance verified, one screen not visually verified — see limitations)

### Completed systems
* **Presents** (block + item, `holidayseasons:present` data component): place an empty box, fill its 9 slots (only the
  placer may open it while unwrapped), sneak + right-click to wrap — recipient (empty = anyone), message, one of 8
  paper colours (tinted on block and item). Wrapped: the recipient opens it with a right-click (contents go to the
  player, the block is removed first — no duplication window); everyone else sees "To / From". Breaking keeps
  contents and wrapping in the item (loot `copy_components`, creative-break handled like shulker boxes); unwrapping
  in hand consumes the present before handing out its contents; presents cannot be nested; stack size 1.
* **Wish lists & preferences**: deterministic per villager and year from its personality's gift preference (7
  categories); sneak + right-click a villager with an empty hand during the Christmas season to hear its wishes.
* **Giving presents to villagers**: a wrapped, non-empty present is accepted; reaction and reputation scale with how
  much it is liked (wish list 2, preferred kind 1, otherwise 0); one reward per player per villager per Christmas
  (persistent claims, survives restarts).
* **Villagers give presents** to nearby players whose village reputation is at least Neighbour: walk over carrying a
  present and hand it over as an item only that player can pick up; contents scale with reputation tier (cookies →
  emeralds → profession gifts → ornaments → diamonds and a Santa hat); once per villager per player per Christmas.
* **Villager↔villager gift exchanges**: walk to a friend with a present, the friend unwraps it with particles/sounds.
* **Mysterious gift delivery** (special event, holiday JSON chance): on Christmas night a present appears at each
  online player's hearth with a chime and sparkles; rolled once per player per Christmas, never repeated.
* **Decor**: two-block Christmas Tree (3 rounds of ornaments, glowstone dust lights it — light 12, sparkling star),
  Wreath and String Lights (wall-hung, lights glow), Ornaments.
* **Festive wear**: Santa Hat, Elf Hat, Reindeer Antlers, Winter Beanie (wearable 3D hats); villagers wear them on the
  last preparation day and Christmas (children: elf hats/antlers), replacing Halloween's temporary costume, never a
  player-given hat, removed afterwards.
* `/everfest reputation` shows your standing in the nearest village; operators `/everfest reputation set <player> <n>`.

### Test results
* **Unit tests — 67/67** (new: wish lists deterministic, distinct, from the catalogue, vary by year; appreciation).
* **GameTests — 26/26, two consecutive runs**: recipient-only opening and exact content transfer with nothing extra
  dropped; breaking keeps contents in one item and in-hand unwrapping gives them exactly once; unwrapped/empty presents
  refused by villagers, wished-for present delights, reputation rises, no extra reward for repeats; villager gives a
  liked player exactly one addressed, non-empty present; tree decorates both halves (max 3 ornaments), lights to 12,
  breaking the lower half removes the upper; children wear elf hats/antlers at Christmas and take them off afterwards.
* **Live**: Christmas Day in the savanna village — 13 villagers gathering, feasting, socializing and exchanging gifts;
  4 Santa hats, 2 elf hats, 2 antlers; the decorated tree centrepiece; with the player's reputation at 60, several
  villagers (incl. the cartographer) autonomously brought the player addressed presents (one already picked up: 5
  emeralds + gifts), shown red/green-tinted in the hotbar; calendar shows Christmas "happening now" and New Year's
  preparing.

### Bugs found and fixed during M5 verification
* The present recipe produced 2 items while presents stack to 1 → Minecraft rejected the recipe (server log).
* Sidebar text "Preparing - starts in N days" overflowed the calendar panel → shortened.
* Test-harness lessons: X display liveness must not be checked with a pattern that matches the checking shell.

### Known issues / limitations (M5)
* The wrap screen's rendering was not screenshot-verified: xdotool mouse clicks did not reach the game for block
  interaction in the headless client. The server-side wrap/unwrap rules are covered by GameTests.
* Original Christmas music is part of Milestone 8 (audio). Christmas merchants are part of Milestone 7.

## Milestone 6 — Remaining holidays ✅ (acceptance criteria verified)

### Completed systems
* **Easter egg hunts**: while preparing, adults hide eggs around the village (`HIDE_EGGS`, up to `eggs_per_village`
  from the holiday JSON, default 12). An egg is a small block display plus a vanilla `Interaction` hitbox, both
  tracked by the decoration ledger, so hiding never edits a block and leftovers vanish in cleanup. 1 in 20 is golden.
  On Easter children go hunting (`EGG_HUNT`); players right-click an egg to collect a coloured Easter Egg item (golden
  → Golden Egg + chat notice) and earn festival-participation reputation. Every egg can be found exactly once.
* **Easter Basket** (bundle-style container for eggs, flowers and candy), four Easter Egg colours, Golden Egg,
  `#holidayseasons:easter_eggs` tag, recipes.
* **Holiday trades**: while a holiday is ACTIVE, working villagers (not mid-trade) gain one marked, low-use emerald
  offer — Halloween candy corn, Christmas ornaments, Easter basket, New Year's/Independence Day fireworks, Valentine's
  chocolate box, Thanksgiving harvest pie. Afterwards only offers carrying EverFest's marker are removed; vanilla and
  other mods' trades are never touched.
* **Firework shows** (`FireworkShowService`): New Year's midnight countdown — gold titles 10…1 with rising note-block
  ticks, then "Happy New Year!" at midnight and 30 seconds of fireworks over every loaded bell village and around players
  who are not near one; Independence Day dusk show (holiday disabled by default). Each show runs once per occurrence (persistent
  claim), survives restarts and time jumps.
* **Valentine's Day**: villagers exchange flowers with friends (`GIFT_EXCHANGE` carries a poppy); players give a
  Chocolate Box or a small flower to a villager for a reaction and reputation, once per villager per player per year.
* **Thanksgiving**: harvest decorations, feasts serve Harvest Pie; Valentine's feasts serve chocolate boxes.
* New items: Chocolate Box, Harvest Pie (food), textures generated by `tools/gen_textures.py`.

### Test results
* **Unit tests — 67/67** (no new pure-logic units; M6 logic is world-bound and covered by GameTests).
* **GameTests — 30/30** (new: hiding an egg places no block, each egg is found exactly once by a player or a child and its model is removed; holiday trades appear and are
  removed without touching vanilla offers; firework show runs and ends; Valentine's gifts only count on Valentine's).
* **Live (dedicated server + client, savanna village, 13 villagers)**:
  * Easter preparation: adults assigned `HIDE_EGGS`, ledger egg count rose. Easter day: tasks `{GATHER, SOCIALIZE,
    EGG_HUNT}`, hidden eggs dropped 5 → 3 → 0 as children found them.
  * A real leatherworker carried the marked Easter-basket trade (`holidayseasons_holiday_trade` custom data).
  * New Year's: server log "firework show starting with countdown"; client screenshot shows the bold gold countdown
    title ("7") over the village and fireworks bursting in the night sky afterwards.
  * Valentine's Day: festival ACTIVE, tasks `{GATHER=2, SOCIALIZE=1, GIFT_EXCHANGE=1}`.

### Bugs found and fixed during M6 verification
* The countdown started at 17750, so "Happy New Year!" landed at 23:57 → starts at 17800 now; the once-a-second check
  puts it at 00:00–00:01.
* Live-test lesson: a forward `time add` past dawn of a one-day holiday skips straight to CLEANUP (correct engine
  behaviour; the test script now stays on the active day).

### Known issues / limitations (M6)
* The "Happy New Year!" title frame itself was not captured in a screenshot (the countdown digit and fireworks were).
* Villager↔villager Valentine's flower hand-overs were confirmed by task assignment, not by a screenshot.
* Independence Day is disabled by default, per spec; enable it in the server config.

## Current milestone: 7 — World expansion
