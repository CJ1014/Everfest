# EverFest

**A living holiday calendar for Minecraft 1.21.1 (NeoForge).** Villages prepare for and celebrate the holidays all
year round. Halloween, Christmas, Easter, New Year's, Valentine's Day, Thanksgiving and (optionally) Independence Day
each arrive on the calendar, and villagers decorate, gather, feast, trick-or-treat, exchange gifts, hunt eggs and
set off fireworks, then tidy up afterwards. EverFest synchronizes with **Serene Seasons** when it is installed.

Mod id `holidayseasons` · version 1.0.0 · Minecraft 1.21.1 · NeoForge 21.1.200 or newer · Java 21

## Installation

1. Install NeoForge 21.1.200+ for Minecraft 1.21.1 (client and/or server).
2. Put `everfest-1.21.1-1.0.0.jar` in `mods/` on the server **and** on every client.
3. Optional, recommended: Serene Seasons 10.1.x for NeoForge with its required GlitchCore 2.1.x. Without it, EverFest
   runs its own calendar and derives the seasons from it.

## What happens in the world

* **Calendar**: a 12-month calendar driven by Serene Seasons (each sub-season is a month), a custom calendar of your own
  month lengths, or the real-world date in a chosen timezone. Open it with a Wall Calendar or `/everfest calendar`.
  Holidays move through *preparing → celebrating → cleaning up*, survive restarts and time jumps, and are never
  celebrated twice.
* **Villages** decorate in their own biome style, gather at the bell, feast and dance on holiday days, and take
  everything down afterwards. Decorations are display entities and light blocks only, so nothing in the world is
  broken or replaced. Unloaded villages catch up when they load.
* **Halloween**: costumes, candy, candy bowls, doorbells, carved pumpkin faces, trick-or-treating children who knock
  on villagers' doors and on registered player homes, harmless tricks, ghosts, gourdlings and scarecrows at night, and
  fog with ambient sounds.
* **Christmas**: wrappable presents, villager wish lists, gift exchanges, presents from villagers who like you,
  decorated trees, wreaths, string lights, festive hats and a mysterious overnight delivery.
* **Easter** egg hunts, a **New Year's** midnight countdown with fireworks, **Valentine's** gifts, **Thanksgiving**
  feasts, and holiday trades on working villagers.
* **Player homes**: place a Holiday Hearth to make a house your home (trick-or-treaters and party guests know
  where to come), then host a **holiday party**: neighbours who like you walk over and celebrate.
* **Seasonal wildlife**: reindeer herds in snowy biomes (with a rare red-nosed festive coat), pastel spring bunnies
  that leave painted eggs, and strutting autumn wild turkeys; some vanilla animals dress up for the holidays.
* **Landmarks**: haunted cottages, forgotten graveyards (haunted on Halloween night) and snowy seasonal cabins whose
  hearth you can claim.
* **Merchants and events**: holiday merchants with a market stall, the Midnight Peddler on Halloween night, village
  objectives handed in at the bell, and optional festival tokens for collectibles such as the Snow Globe.
* **Progress**: per-player, per-village holiday reputation (Stranger to Legend), the Holiday Collection Book, and an
  EverFest advancement tab.
* **Atmosphere**: falling autumn leaves, spring petals, winter flurries, and five original holiday music tracks.

## Commands

Everyone: `/everfest calendar`, `/everfest date`, `/everfest date info`, `/everfest holiday list`,
`/everfest holiday info <holiday>`, `/everfest reputation`, `/everfest collection`, `/everfest home list|edit|party`.

Operators (permission level 2):

| Command | What it does |
|---|---|
| `/everfest admin config` | Edit every server setting in-game, also on remote servers |
| `/everfest date set <yyyy-mm-dd>` / `add <days>` / `clearoverride` | Move the calendar |
| `/everfest holiday goto <holiday> [prepare\|start]` / `reset` / `finish` | Jump to or reset a holiday |
| `/everfest villages`, `/everfest halloween` | Diagnostics for celebrating villages and trick-or-treat groups |
| `/everfest reputation set <player> <points>` | Set a player's reputation in the nearest village |
| `/everfest home register <pos> <owner>` | Register a home for a player |
| `/everfest wildlife [spawn <species>]` | Seasonal wildlife status, or bring a group now |
| `/everfest merchant visit\|peddler` | Bring a holiday merchant or the Midnight Peddler now |
| `/everfest perf [reset]` | How long each EverFest system takes per server tick |

## Configuration

* **Server** (`serverconfig/holidayseasons-server.toml`, or `/everfest admin config` in game): calendar mode and
  months, Serene Seasons sync policy, every built-in holiday (on/off, dates, durations, intensity), village behaviour
  budgets, homes, Halloween, wildlife, world generation, economy (tokens are **off** by default), rare events and
  parties.
* **Client** (Mods → EverFest → Config): date HUD, 24-hour clock, holiday toasts, effects intensity, Halloween fog,
  seasonal particles, ambient sounds, festive animal outfits, holiday music.
* **Data packs** can add or replace holidays (`data/<namespace>/everfest/holiday/*.json`) and retune where landmarks and
  wildlife appear (`worldgen`, `tags/worldgen/biome`, `neoforge/biome_modifier`).

## Performance

All server work runs on budgets and timers: villagers are processed a few per tick, players are checked every couple
of seconds, and nothing loads chunks. On the test server, during a celebrated Christmas with a merchant, a party and
two players online, EverFest used about 0.1-0.3 ms per tick, measured with `/everfest perf`.

## Credits

Code, textures, models, sound effects and music: original work made for EverFest. Every texture, model, sound effect
and music track is generated by the scripts in `tools/` (no samples, recordings or existing melodies). Serene Seasons
and GlitchCore are separate mods by the Glitchfiend team.

License: All Rights Reserved.
