#!/usr/bin/env python3
"""Generates EverFest's JSON resources: models, blockstates, loot tables, recipes, tags, sounds.json and language
entries for the Halloween content.  python3 tools/gen_assets.py"""
import json
import os
import re

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
NS = "holidayseasons"
A = os.path.join(ROOT, "assets", NS)
D = os.path.join(ROOT, "data", NS)
FACES = ["grin", "scary", "silly", "cat", "ghost"]
COSTUMES = ["pumpkin", "ghost", "skeleton", "witch", "vampire", "scarecrow", "bat", "black_cat"]
CANDY = ["candy_corn", "caramel_apple", "chocolate_bar", "lollipop", "gummy_bat"]


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def t(name):
    return f"{NS}:{name}"


def cube(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), overrides=None, rotation=None):
    """An element using full-texture UVs on each face (simple, readable art)."""
    overrides = overrides or {}
    e = {"from": frm, "to": to, "faces": {f: {"uv": [0, 0, 16, 16], "texture": overrides.get(f, tex)} for f in faces}}
    if rotation:
        e["rotation"] = rotation
    return e


def facing_variants(model, extra=""):
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    return {"variants": {f"facing={f}{extra}": ({"model": model, "y": r} if r else {"model": model}) for f, r in rot.items()}}


# ----------------------------------------------------------------------------------------------- items

def simple_item(name, texture=None):
    write(f"{A}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": t("item/" + (texture or name))}})


def costume_models():
    gui = {"rotation": [30, 225, 0], "translation": [0, -1.5, 0], "scale": [0.5, 0.5, 0.5]}
    common = {"parent": "minecraft:block/block", "display": {"gui": gui, "fixed": {"rotation": [0, 180, 0], "scale": [0.5, 0.5, 0.5]}}}
    C = lambda n: t("item/costume/" + n)
    models = {
        "pumpkin": {"textures": {"side": C("pumpkin_side"), "top": C("pumpkin_top"), "face": C("pumpkin_face"), "particle": C("pumpkin_side")},
                    "elements": [cube([0.5, 0.5, 0.5], [15.5, 15.5, 15.5], "#side", overrides={"north": "#face", "up": "#top"})]},
        "ghost": {"textures": {"cloth": C("ghost_cloth"), "face": C("ghost_face"), "particle": C("ghost_cloth")},
                  "elements": [cube([0.5, 0.5, 0.5], [15.5, 16.5, 15.5], "#cloth", overrides={"north": "#face"}),
                               cube([-1.5, -6, -1.5], [17.5, 1, 17.5], "#cloth", faces=("north", "south", "east", "west", "down"))]},
        "skeleton": {"textures": {"bone": C("bone"), "face": C("skull_face"), "particle": C("bone")},
                     "elements": [cube([0.5, 0.5, 0.5], [15.5, 15.5, 15.5], "#bone", overrides={"north": "#face"})]},
        "witch": {"textures": {"hat": C("witch_purple"), "band": C("witch_band"), "particle": C("witch_purple")},
                  "elements": [cube([-2.5, 14.5, -2.5], [18.5, 15.5, 18.5], "#hat"),
                               cube([2.5, 15.5, 2.5], [13.5, 19.5, 13.5], "#hat"),
                               cube([2.4, 15.5, 2.4], [13.6, 17, 13.6], "#band", faces=("north", "south", "east", "west")),
                               cube([4.5, 19.5, 4.5], [11.5, 23.5, 11.5], "#hat"),
                               cube([6.5, 23, 6.5], [9.5, 27.5, 9.5], "#hat", rotation={"angle": 22.5, "axis": "z", "origin": [8, 23, 8]})]},
        "vampire": {"textures": {"black": C("vampire_black"), "red": C("vampire_red"), "particle": C("vampire_black")},
                    "elements": [cube([0.3, 13, 0.3], [15.7, 16.3, 15.7], "#black"),
                                 cube([5, 11, 0.1], [11, 13, 1], "#black", faces=("north", "south", "east", "west", "down")),
                                 cube([-2, -5, 15.5], [18, 9, 17.5], "#black", overrides={"north": "#red"}),
                                 cube([-2.5, -5, 2], [-0.5, 7, 17.5], "#black", overrides={"east": "#red"}),
                                 cube([16.5, -5, 2], [18.5, 7, 17.5], "#black", overrides={"west": "#red"})]},
        "scarecrow": {"textures": {"straw": C("straw"), "band": C("straw_band"), "particle": C("straw")},
                      "elements": [cube([-3, 14.5, -3], [19, 15.5, 19], "#straw"),
                                   cube([2.5, 15.5, 2.5], [13.5, 20.5, 13.5], "#straw"),
                                   cube([2.4, 15.5, 2.4], [13.6, 17, 13.6], "#band", faces=("north", "south", "east", "west")),
                                   cube([-1.5, 4, 5], [0.5, 11, 11], "#straw"),
                                   cube([15.5, 4, 5], [17.5, 11, 11], "#straw")]},
        "bat": {"textures": {"bat": C("bat"), "particle": C("bat")},
                "elements": [cube([1, 14.5, 7], [15, 15.5, 9], "#bat"),
                             cube([1.5, 15.5, 7.5], [5.5, 18, 8.5], "#bat"), cube([2.5, 18, 7.5], [5, 21, 8.5], "#bat"),
                             cube([10.5, 15.5, 7.5], [14.5, 18, 8.5], "#bat"), cube([11, 18, 7.5], [13.5, 21, 8.5], "#bat")]},
        "black_cat": {"textures": {"fur": C("cat"), "inner": C("cat_inner"), "particle": C("cat")},
                      "elements": [cube([1, 14.5, 7], [15, 15.5, 9], "#fur"),
                                   cube([2, 15.5, 7], [6, 17.5, 9], "#fur", overrides={"north": "#inner"}),
                                   cube([3, 17.5, 7.5], [5, 19.5, 8.5], "#fur"),
                                   cube([10, 15.5, 7], [14, 17.5, 9], "#fur", overrides={"north": "#inner"}),
                                   cube([11, 17.5, 7.5], [13, 19.5, 8.5], "#fur")]},
    }
    for name, m in models.items():
        model = dict(common)
        model.update(m)
        write(f"{A}/models/item/costume_{name}.json", model)


# ----------------------------------------------------------------------------------------------- blocks

def blocks():
    # Candy bowl with four fill levels.
    for fill in range(4):
        el = [cube([4, 0, 4], [12, 1, 12], "#side", overrides={"up": "#inside"}),
              cube([3, 1, 3], [13, 5, 4], "#side", overrides={"up": "#inside", "south": "#inside"}),
              cube([3, 1, 12], [13, 5, 13], "#side", overrides={"up": "#inside", "north": "#inside"}),
              cube([3, 1, 4], [4, 5, 12], "#side", overrides={"up": "#inside", "east": "#inside"}),
              cube([12, 1, 4], [13, 5, 12], "#side", overrides={"up": "#inside", "west": "#inside"})]
        if fill > 0:
            el.append(cube([4, 1, 4], [12, 1 + fill * 1.4, 12], "#candy", faces=("up", "north", "south", "east", "west")))
        write(f"{A}/models/block/candy_bowl_{fill}.json", {"parent": "minecraft:block/block", "ambientocclusion": False,
              "textures": {"side": t("block/candy_bowl_side"), "inside": t("block/candy_bowl_inside"), "candy": t("block/candy_pile"),
                           "particle": t("block/candy_bowl_side")}, "elements": el})
    write(f"{A}/blockstates/candy_bowl.json", {"variants": {f"fill={i}": {"model": t(f"block/candy_bowl_{i}")} for i in range(4)}})
    write(f"{A}/models/item/candy_bowl.json", {"parent": t("block/candy_bowl_2")})

    # Doorbell (button-like).
    for pressed, depth in ((False, 2), (True, 1)):
        name = "doorbell_pressed" if pressed else "doorbell"
        write(f"{A}/models/block/{name}.json", {"textures": {"plate": t("block/doorbell"), "particle": t("block/doorbell")},
              "elements": [{"from": [5, 0, 5], "to": [11, depth, 11], "faces": {
                  "down": {"uv": [5, 5, 11, 11], "texture": "#plate", "cullface": "down"},
                  "up": {"uv": [5, 5, 11, 11], "texture": "#plate"},
                  "north": {"uv": [5, 14, 11, 16], "texture": "#plate"}, "south": {"uv": [5, 14, 11, 16], "texture": "#plate"},
                  "west": {"uv": [5, 14, 11, 16], "texture": "#plate"}, "east": {"uv": [5, 14, 11, 16], "texture": "#plate"}}}]})
    variants = {}
    for face, x in (("floor", 0), ("wall", 90), ("ceiling", 180)):
        for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            yy = y if face != "ceiling" else (y + 180) % 360
            for powered in ("false", "true"):
                v = {"model": t("block/doorbell_pressed" if powered == "true" else "block/doorbell")}
                if x:
                    v["x"] = x
                if yy:
                    v["y"] = yy
                if face == "wall":
                    v["uvlock"] = True
                variants[f"face={face},facing={facing},powered={powered}"] = v
    write(f"{A}/blockstates/doorbell.json", {"variants": variants})
    write(f"{A}/models/item/doorbell.json", {"parent": "minecraft:item/generated", "textures": {"layer0": t("block/doorbell")}})

    # Holiday hearth.
    write(f"{A}/models/block/holiday_hearth.json", {"parent": "minecraft:block/block",
          "textures": {"stone": t("block/hearth_stone"), "front": t("block/hearth_front"), "top": t("block/hearth_top"), "particle": t("block/hearth_stone")},
          "elements": [cube([0, 0, 0], [16, 4, 16], "#stone"),
                       cube([2, 4, 2], [14, 12, 14], "#stone", overrides={"north": "#front"}),
                       cube([1, 12, 1], [15, 14, 15], "#stone", overrides={"up": "#top"})]})
    write(f"{A}/blockstates/holiday_hearth.json", facing_variants(t("block/holiday_hearth")))
    write(f"{A}/models/item/holiday_hearth.json", {"parent": t("block/holiday_hearth")})

    # Carved pumpkins and jack-o'-lanterns.
    for face in FACES:
        for kind in ("carved_pumpkin", "jack_o_lantern"):
            name = f"{kind}_{face}"
            write(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/orientable", "textures": {
                "top": "minecraft:block/pumpkin_top", "side": "minecraft:block/pumpkin_side", "front": t("block/" + name)}})
            write(f"{A}/blockstates/{name}.json", facing_variants(t("block/" + name)))
            write(f"{A}/models/item/{name}.json", {"parent": t("block/" + name)})

    # Tombstone.
    write(f"{A}/models/block/tombstone.json", {"parent": "minecraft:block/block", "textures": {"stone": t("block/tombstone"), "particle": t("block/tombstone")},
          "elements": [cube([2, 0, 5], [14, 1, 11], "#stone"), cube([3, 1, 6], [13, 11, 10], "#stone"), cube([4, 11, 6], [12, 13, 10], "#stone")]})
    write(f"{A}/blockstates/tombstone.json", facing_variants(t("block/tombstone")))
    write(f"{A}/models/item/tombstone.json", {"parent": t("block/tombstone")})

    # Hanging ghost.
    write(f"{A}/models/block/hanging_ghost.json", {"parent": "minecraft:block/block", "ambientocclusion": False,
          "textures": {"cloth": t("block/hanging_ghost"), "string": "minecraft:block/white_wool", "particle": t("block/hanging_ghost")},
          "elements": [cube([7.5, 13, 7.5], [8.5, 16, 8.5], "#string"), cube([4, 4, 4], [12, 13, 12], "#cloth"),
                       cube([4, 2, 4], [6.5, 4, 6.5], "#cloth"), cube([9.5, 2, 9.5], [12, 4, 12], "#cloth"), cube([6.75, 1, 4], [9.25, 4, 6.5], "#cloth")]})
    write(f"{A}/blockstates/hanging_ghost.json", facing_variants(t("block/hanging_ghost")))
    write(f"{A}/models/item/hanging_ghost.json", {"parent": t("block/hanging_ghost")})


def items():
    for c in CANDY + ["candy_bag", "carving_knife", "ectoplasm"]:
        simple_item(c)
    write(f"{A}/models/item/carving_knife.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": t("item/carving_knife")}})
    for egg in ("ghost", "gourdling", "scarecrow"):
        write(f"{A}/models/item/{egg}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    costume_models()


# ----------------------------------------------------------------------------------------------- data

def loot():
    names = ["candy_bowl", "doorbell", "holiday_hearth", "tombstone", "hanging_ghost"] + \
            [f"{k}_{f}" for f in FACES for k in ("carved_pumpkin", "jack_o_lantern")]
    for n in names:
        write(f"{D}/loot_table/blocks/{n}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
              "entries": [{"type": "minecraft:item", "name": t(n)}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
              "random_sequence": t("blocks/" + n)})
    for mob, drops in (("ghost", [("holidayseasons:ectoplasm", 0, 1)]),):
        write(f"{D}/loot_table/entities/{mob}.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [{
            "type": "minecraft:item", "name": drops[0][0], "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": drops[0][1], "max": drops[0][2]}},
                {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting", "count": {"min": 0, "max": 1}}]}]}],
            "random_sequence": t("entities/" + mob)})


def shapeless(name, ingredients, count=1, result=None):
    write(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
          "ingredients": [{"item": i} if not i.startswith("#") else {"tag": i[1:]} for i in ingredients],
          "result": {"id": result or t(name), "count": count}})


def shaped(name, pattern, key, count=1):
    write(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
          "key": {k: ({"item": v} if not v.startswith("#") else {"tag": v[1:]}) for k, v in key.items()},
          "result": {"id": t(name), "count": count}})


def recipes():
    shapeless("candy_corn", ["minecraft:sugar", "minecraft:wheat", "minecraft:orange_dye", "minecraft:yellow_dye"], 4)
    shapeless("caramel_apple", ["minecraft:apple", "minecraft:sugar", "minecraft:stick"])
    shapeless("chocolate_bar", ["minecraft:cocoa_beans", "minecraft:cocoa_beans", "minecraft:sugar", "minecraft:milk_bucket"], 3)
    shapeless("lollipop", ["minecraft:sugar", "minecraft:sugar", "minecraft:stick", "minecraft:magenta_dye"], 2)
    shapeless("gummy_bat", ["minecraft:slime_ball", "minecraft:sugar", "minecraft:purple_dye"], 3)
    shaped("candy_bag", [" S ", "LDL", " L "], {"S": "minecraft:string", "L": "minecraft:leather", "D": "minecraft:orange_dye"})
    costume = {
        "pumpkin": ["minecraft:carved_pumpkin", "minecraft:string"],
        "ghost": ["minecraft:white_wool", "minecraft:white_wool", "minecraft:string", "minecraft:black_dye"],
        "skeleton": ["minecraft:bone", "minecraft:bone", "minecraft:bone_meal", "minecraft:string"],
        "witch": ["minecraft:purple_wool", "minecraft:purple_wool", "minecraft:black_wool", "minecraft:orange_dye"],
        "vampire": ["minecraft:black_wool", "minecraft:black_wool", "minecraft:red_wool", "minecraft:string"],
        "scarecrow": ["minecraft:wheat", "minecraft:wheat", "minecraft:wheat", "minecraft:string"],
        "bat": ["minecraft:black_wool", "minecraft:leather", "minecraft:string"],
        "black_cat": ["minecraft:black_wool", "minecraft:pink_dye", "minecraft:string"],
    }
    for name, ing in costume.items():
        shapeless("costume_" + name, ing)
    shaped("carving_knife", ["  I", " I ", "S  "], {"I": "minecraft:iron_ingot", "S": "minecraft:stick"})
    shapeless("candy_bowl", ["minecraft:bowl", "minecraft:orange_terracotta", "minecraft:sugar"])
    shapeless("doorbell", ["minecraft:gold_ingot", "minecraft:stone_button", "minecraft:redstone"])
    shaped("holiday_hearth", ["BBB", "BCB", "SSS"], {"B": "minecraft:bricks", "C": "minecraft:campfire", "S": "minecraft:stone_bricks"})
    shaped("tombstone", [" S ", "SSS", "SSS"], {"S": "minecraft:smooth_stone"}, 2)
    shapeless("hanging_ghost", ["minecraft:white_wool", "minecraft:string", t("ectoplasm")], 2)
    for f in FACES:
        shapeless(f"jack_o_lantern_{f}", [t(f"carved_pumpkin_{f}"), "minecraft:torch"])


def tags():
    write(f"{D}/tags/item/candy.json", {"values": [t(c) for c in CANDY]})
    write(f"{D}/tags/item/costumes.json", {"values": [t("costume_" + c) for c in COSTUMES]})
    base = os.path.join(ROOT, "data", "minecraft", "tags", "block", "mineable")
    write(os.path.join(base, "pickaxe.json"), {"replace": False, "values": [t("holiday_hearth"), t("tombstone")]})
    write(os.path.join(base, "axe.json"), {"replace": False, "values": [t(f"{k}_{f}") for f in FACES for k in ("carved_pumpkin", "jack_o_lantern")]})


def sounds():
    names = ["doorbell", "knock", "candy", "candy_bowl_empty", "ghost_moan", "ghost_hurt", "ghost_death", "distant_bell", "eerie_wind",
             "whispers", "gourdling_hop", "gourdling_hurt", "gourdling_death", "scarecrow_rustle", "scarecrow_creak", "trick_poof",
             "turkey_gobble"]
    write(f"{A}/sounds.json", {n: {"sounds": [t(n)], "subtitle": f"subtitles.{NS}.{n}"} for n in names})
    return names


def lang(sound_names):
    p = f"{A}/lang/en_us.json"
    with open(p) as f:
        lang = json.load(f)
    nice = lambda s: " ".join(w[:1].upper() + w[1:] for w in re.sub(r"(?<=[a-z])(?=[A-Z])", " ", s.replace("_", " ")).split())
    for c in CANDY:
        lang[f"item.{NS}.{c}"] = {"candy_corn": "Candy Corn", "caramel_apple": "Caramel Apple", "chocolate_bar": "Chocolate Bar",
                                   "lollipop": "Lollipop", "gummy_bat": "Gummy Bat"}[c]
    costume_names = {"pumpkin": "Pumpkin Mask", "ghost": "Ghost Sheet", "skeleton": "Skeleton Mask", "witch": "Witch Hat",
                     "vampire": "Vampire Collar", "scarecrow": "Scarecrow Hat", "bat": "Bat Ears", "black_cat": "Black Cat Ears"}
    for c, n in costume_names.items():
        lang[f"item.{NS}.costume_{c}"] = n
    face_names = {"grin": "Grinning", "scary": "Scary", "silly": "Silly", "cat": "Cat", "ghost": "Ghostly"}
    for f, n in face_names.items():
        lang[f"block.{NS}.carved_pumpkin_{f}"] = f"{n} Carved Pumpkin"
        lang[f"block.{NS}.jack_o_lantern_{f}"] = f"{n} Jack o'Lantern"
    lang.update({
        f"item.{NS}.candy_bag": "Trick-or-Treat Bag",
        f"item.{NS}.carving_knife": "Carving Knife",
        f"item.{NS}.ectoplasm": "Ectoplasm",
        f"item.{NS}.ghost_spawn_egg": "Ghost Spawn Egg",
        f"item.{NS}.gourdling_spawn_egg": "Gourdling Spawn Egg",
        f"item.{NS}.scarecrow_spawn_egg": "Scarecrow Spawn Egg",
        f"item.{NS}.costume.tooltip": "Costume - wear it on your head",
        f"block.{NS}.candy_bowl": "Candy Bowl",
        f"block.{NS}.doorbell": "Doorbell",
        f"block.{NS}.holiday_hearth": "Holiday Hearth",
        f"block.{NS}.tombstone": "Tombstone",
        f"block.{NS}.hanging_ghost": "Hanging Ghost",
        f"entity.{NS}.ghost": "Ghost",
        f"entity.{NS}.gourdling": "Gourdling",
        f"entity.{NS}.scarecrow": "Scarecrow",
        f"{NS}.candy_bowl.full": "The candy bowl is full",
        f"{NS}.candy_bowl.contents": "%s candies in the bowl (%s handed out)",
        f"{NS}.candy_bowl.empty": "The candy bowl is empty!",
        f"{NS}.candy_bowl.limit": "You've already had your candy from this bowl today",
        f"{NS}.candy_bowl.owner_only": "This candy is for %s's trick-or-treaters",
        f"{NS}.doorbell.someone": "Someone",
        f"{NS}.doorbell.rang": "%s rang the doorbell at %s",
        f"{NS}.trick_or_treaters": "Trick-or-treaters",
        f"{NS}.trick_or_treat.at_door": "%s trick-or-treaters are knocking at %s! Hand them candy before they play a trick.",
        f"{NS}.ghost.stole_candy": "A ghost swiped a piece of your candy!",
        f"{NS}.home.too_close": "Another player's home is too close (minimum %2$s blocks)",
        f"{NS}.home.too_many": "You already have the maximum of %1$s registered homes",
        f"{NS}.home.registered": "Home registered: %s. Visitors will come to the door at %s.",
        f"{NS}.home.registered_no_door": "Home registered: %s. No door found nearby - visitors will come to the hearth.",
        f"{NS}.home.unregistered": "Nobody lives here. Sneak and use the hearth to make this house your home",
        f"{NS}.home.belongs_to": "%s - the home of %s",
        f"{NS}.home.not_owner": "Only the owner can change this home",
        f"{NS}.home.saved": "Saved settings for %s",
        f"{NS}.home.protected": "This hearth belongs to %s",
        f"{NS}.home.none_nearby": "Stand within 16 blocks of one of your Holiday Hearths",
        f"{NS}.screen.hearth": "Holiday Hearth",
        f"{NS}.screen.hearth.name": "Home name",
        f"{NS}.screen.hearth.visitors": "Welcome visitors",
        f"{NS}.screen.hearth.decorations": "Villagers may decorate here",
        f"{NS}.screen.hearth.candy": "Candy bowls",
        f"{NS}.screen.hearth.entrance": "Visitors knock at %s",
        f"{NS}.screen.hearth.no_door": "No door found - visitors come to the hearth",
        f"{NS}.screen.hearth.visits": "Visits this year: %s",
        f"{NS}.screen.save": "Save",
        f"{NS}.candy_policy.everyone": "Everyone may take one",
        f"{NS}.candy_policy.owner_only": "Only for trick-or-treaters",
        f"{NS}.configuration.effects": "Effects",
        f"{NS}.configuration.effectsIntensity": "Effects Intensity",
        f"{NS}.configuration.halloweenFog": "Halloween Fog",
        f"{NS}.configuration.seasonalParticles": "Seasonal Particles",
        f"{NS}.configuration.ambientSounds": "Ambient Sounds",
        f"{NS}.configuration.villages": "Villages",
        f"{NS}.configuration.homes": "Homes",
        f"{NS}.configuration.halloween": "Halloween",
    })
    for key in ["enabled", "festivalSchedules", "villagerBudgetPerTick", "taskCooldownTicks", "decorations", "maxDecorationsPerVillage",
                "decorationRadius", "decorationLedgerBudget", "reconcileUnloadedVillages"]:
        lang[f"{NS}.configuration.villages.{key}"] = nice(key.replace("Ticks", "_ticks"))
    for key in ["maxHomesPerPlayer", "minHomeDistance", "homeProtectionRadius", "protectHearths"]:
        lang[f"{NS}.configuration.homes.{key}"] = nice(key)
    for key in ["trickOrTreating", "maxTrickOrTreatGroups", "trickOrTreatRadius", "maxVisitsPerHouse", "candyPerVisitorPerDay",
                "villagerCostumes", "harmlessTricks", "creatures", "creaturesPerPlayer", "creatureSpawnChance"]:
        lang[f"{NS}.configuration.halloween.{key}"] = nice(key)
    subtitles = {"doorbell": "Doorbell rings", "knock": "Knocking", "candy": "Candy crinkles", "candy_bowl_empty": "Empty bowl clinks",
                 "ghost_moan": "Ghost moans", "ghost_hurt": "Ghost wails", "ghost_death": "Ghost fades away", "distant_bell": "Distant bell tolls",
                 "eerie_wind": "Eerie wind", "whispers": "Whispers", "gourdling_hop": "Gourdling hops", "gourdling_hurt": "Gourdling squeaks",
                 "gourdling_death": "Gourdling bursts", "scarecrow_rustle": "Straw rustles", "scarecrow_creak": "Scarecrow creaks",
                 "trick_poof": "Trick!", "turkey_gobble": "Turkey gobbles"}
    for n in sound_names:
        lang[f"subtitles.{NS}.{n}"] = subtitles[n]
    with open(p, "w") as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write("\n")


# ----------------------------------------------------------------------------------------------- christmas

def tinted(frm, to, tex, tint=0, faces=("north", "south", "east", "west", "up", "down")):
    return {"from": frm, "to": to, "faces": {f: {"uv": [0, 0, 16, 16], "texture": tex, "tintindex": tint} for f in faces}}


def christmas():
    # Present: wrapped (tinted paper + ribbons + bow) and open box.
    wrapped = [tinted([3, 0, 3], [13, 9, 13], "#paper"),
               cube([7.25, 0, 2.9], [8.75, 9.1, 13.1], "#ribbon", faces=("north", "south", "up")),
               cube([2.9, 0, 7.25], [13.1, 9.1, 8.75], "#ribbon", faces=("east", "west", "up")),
               cube([6, 9, 7], [8, 11, 9], "#ribbon"), cube([8, 9, 7], [10, 11, 9], "#ribbon")]
    write(f"{A}/models/block/present_wrapped.json", {"parent": "minecraft:block/block", "ambientocclusion": False,
          "textures": {"paper": t("block/present_paper"), "ribbon": t("block/present_ribbon"), "particle": t("block/present_paper")},
          "elements": wrapped})
    write(f"{A}/models/block/present_open.json", {"parent": "minecraft:block/block", "ambientocclusion": False,
          "textures": {"box": t("block/present_open"), "particle": t("block/present_open")},
          "elements": [cube([3, 0, 3], [13, 1, 13], "#box"), cube([3, 1, 3], [13, 8, 4], "#box"), cube([3, 1, 12], [13, 8, 13], "#box"),
                       cube([3, 1, 4], [4, 8, 12], "#box"), cube([12, 1, 4], [13, 8, 12], "#box")]})
    write(f"{A}/blockstates/present.json", {"variants": {"wrapped=true": {"model": t("block/present_wrapped")},
                                                         "wrapped=false": {"model": t("block/present_open")}}})
    write(f"{A}/models/item/present.json", {"parent": t("block/present_wrapped")})

    # Christmas tree halves.
    variants = {}
    for deco in range(4):
        for lit in (False, True):
            ltex = {"needles": t("block/tree_needles"), "log": "minecraft:block/spruce_log", "particle": t("block/tree_needles")}
            lower = [cube([6.5, 0, 6.5], [9.5, 3, 9.5], "#log"), cube([1, 3, 1], [15, 9, 15], "#needles"), cube([2.5, 9, 2.5], [13.5, 16, 13.5], "#needles")]
            upper = [cube([4, 0, 4], [12, 7, 12], "#needles"), cube([5.5, 7, 5.5], [10.5, 12, 10.5], "#needles"),
                     cube([7, 12, 7], [9, 15, 9], "#star", rotation={"angle": 45, "axis": "y", "origin": [8, 13, 8]})]
            if deco > 0:
                ltex["orn"] = t(f"block/tree_ornaments_{deco}")
                lower += [cube([0.9, 3, 0.9], [15.1, 9, 15.1], "#orn", faces=("north", "south", "east", "west")),
                          cube([2.4, 9, 2.4], [13.6, 16, 13.6], "#orn", faces=("north", "south", "east", "west"))]
                upper += [cube([3.9, 0, 3.9], [12.1, 7, 12.1], "#orn", faces=("north", "south", "east", "west"))]
            if lit:
                ltex["lights"] = t("block/tree_lights")
                lower += [cube([0.8, 3, 0.8], [15.2, 9, 15.2], "#lights", faces=("north", "south", "east", "west"))]
                upper += [cube([5.4, 7, 5.4], [10.6, 12, 10.6], "#lights", faces=("north", "south", "east", "west"))]
            utex = dict(ltex)
            utex["star"] = t("block/tree_star_lit" if lit else "block/tree_star")
            suffix = f"{deco}{'_lit' if lit else ''}"
            write(f"{A}/models/block/christmas_tree_lower_{suffix}.json", {"parent": "minecraft:block/block", "textures": ltex, "elements": lower})
            write(f"{A}/models/block/christmas_tree_upper_{suffix}.json", {"parent": "minecraft:block/block", "textures": utex, "elements": upper})
            for half in ("lower", "upper"):
                variants[f"decoration={deco},half={half},lit={str(lit).lower()}"] = {"model": t(f"block/christmas_tree_{half}_{suffix}")}
    write(f"{A}/blockstates/christmas_tree.json", {"variants": variants})
    write(f"{A}/models/item/christmas_tree.json", {"parent": "minecraft:item/generated", "textures": {"layer0": t("item/christmas_tree")}})

    for name in ("wreath", "string_lights"):
        write(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/block", "ambientocclusion": False,
              "textures": {"tex": t("block/" + name), "particle": t("block/" + name)},
              "elements": [{"from": [0, 0, 15.5], "to": [16, 16, 15.6], "faces": {
                  "north": {"uv": [0, 0, 16, 16], "texture": "#tex"}, "south": {"uv": [16, 0, 0, 16], "texture": "#tex"}}}]})
        write(f"{A}/blockstates/{name}.json", facing_variants(t("block/" + name)))
        write(f"{A}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": t("block/" + name)}})
    simple_item("ornament")

    # Festive hats (3D, head-centred like the costumes).
    gui = {"rotation": [30, 225, 0], "translation": [0, -1.5, 0], "scale": [0.5, 0.5, 0.5]}
    common = {"parent": "minecraft:block/block", "display": {"gui": gui, "fixed": {"rotation": [0, 180, 0], "scale": [0.5, 0.5, 0.5]}}}
    C = lambda n: t("item/costume/" + n)
    hats = {
        "santa_hat": {"textures": {"red": C("santa_red"), "fur": C("fur"), "particle": C("santa_red")},
                      "elements": [cube([0, 14, 0], [16, 16.5, 16], "#fur"), cube([1.5, 16.5, 1.5], [14.5, 20, 14.5], "#red"),
                                   cube([3.5, 20, 3.5], [12.5, 23, 12.5], "#red", rotation={"angle": -22.5, "axis": "x", "origin": [8, 20, 8]}),
                                   cube([5.5, 22, 9], [10.5, 25, 14], "#red", rotation={"angle": -45, "axis": "x", "origin": [8, 22, 11]}),
                                   cube([6, 21, 14], [10, 25, 18], "#fur")]},
        "elf_hat": {"textures": {"green": C("elf_green"), "trim": C("elf_trim"), "bell": C("bell"), "particle": C("elf_green")},
                    "elements": [cube([0, 14, 0], [16, 16, 16], "#trim"), cube([2, 16, 2], [14, 20, 14], "#green"),
                                 cube([4, 20, 4], [12, 24, 12], "#green", rotation={"angle": 22.5, "axis": "z", "origin": [8, 20, 8]}),
                                 cube([6, 23, 6], [10, 27, 10], "#green", rotation={"angle": 45, "axis": "z", "origin": [8, 24, 8]}),
                                 cube([13, 23, 7], [16, 26, 10], "#bell")]},
        "antlers": {"textures": {"antler": C("antler"), "band": C("headband"), "particle": C("antler")},
                    "elements": [cube([1, 14.5, 7], [15, 15.5, 9], "#band"),
                                 cube([2, 15.5, 7.5], [3.5, 22, 8.5], "#antler"), cube([-1.5, 19, 7.5], [2, 20.5, 8.5], "#antler"),
                                 cube([3.5, 20, 7.5], [5.5, 21.5, 8.5], "#antler"),
                                 cube([12.5, 15.5, 7.5], [14, 22, 8.5], "#antler"), cube([14, 19, 7.5], [17.5, 20.5, 8.5], "#antler"),
                                 cube([10.5, 20, 7.5], [12.5, 21.5, 8.5], "#antler")]},
        "winter_beanie": {"textures": {"knit": C("beanie"), "fur": C("fur"), "particle": C("beanie")},
                          "elements": [cube([0.3, 11, 0.3], [15.7, 16.5, 15.7], "#knit"), cube([2, 16.5, 2], [14, 18, 14], "#knit"),
                                       cube([6, 18, 6], [10, 21, 10], "#fur")]},
    }
    for name, m in hats.items():
        model = dict(common)
        model.update(m)
        write(f"{A}/models/item/{name}.json", model)

    # Loot: presents keep contents and wrapping like shulker boxes; the tree drops from its lower half only.
    write(f"{D}/loot_table/blocks/present.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": t("present"), "functions": [{"function": "minecraft:copy_components", "source": "block_entity",
                                                                       "include": ["minecraft:container", t("present")]}]}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}], "random_sequence": t("blocks/present")})
    write(f"{D}/loot_table/blocks/christmas_tree.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
        "entries": [{"type": "minecraft:item", "name": t("christmas_tree")}],
        "conditions": [{"condition": "minecraft:block_state_property", "block": t("christmas_tree"), "properties": {"half": "lower"}},
                       {"condition": "minecraft:survives_explosion"}]}], "random_sequence": t("blocks/christmas_tree")})
    for n in ("wreath", "string_lights"):
        write(f"{D}/loot_table/blocks/{n}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
              "entries": [{"type": "minecraft:item", "name": t(n)}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
              "random_sequence": t("blocks/" + n)})

    shaped("present", ["PSP", "P P", "PPP"], {"P": "minecraft:paper", "S": "minecraft:string"})
    shapeless("ornament", ["minecraft:glass_pane", "minecraft:red_dye", "minecraft:gold_nugget"], 4)
    shaped("christmas_tree", [" S ", "LLL", " W "], {"S": "minecraft:gold_nugget", "L": "minecraft:spruce_leaves", "W": "minecraft:spruce_log"})
    shaped("wreath", ["LLL", "L L", "LRL"], {"L": "minecraft:spruce_leaves", "R": "minecraft:red_dye"})
    shapeless("string_lights", ["minecraft:string", "minecraft:glowstone_dust", "minecraft:red_dye", "minecraft:blue_dye"], 3)
    shapeless("santa_hat", ["minecraft:red_wool", "minecraft:red_wool", "minecraft:white_wool"])
    shapeless("elf_hat", ["minecraft:green_wool", "minecraft:green_wool", "minecraft:red_wool", "minecraft:gold_nugget"])
    shapeless("antlers", ["minecraft:stick", "minecraft:stick", "minecraft:brown_wool", "minecraft:leather"])
    shapeless("winter_beanie", ["minecraft:blue_wool", "minecraft:white_wool", "minecraft:string"])

    p = f"{A}/lang/en_us.json"
    with open(p) as f:
        lang = json.load(f)
    lang.update({
        f"block.{NS}.present": "Present", f"item.{NS}.present": "Present",
        f"block.{NS}.christmas_tree": "Christmas Tree", f"block.{NS}.wreath": "Wreath", f"block.{NS}.string_lights": "String Lights",
        f"item.{NS}.ornament": "Ornament", f"item.{NS}.santa_hat": "Santa Hat", f"item.{NS}.elf_hat": "Elf Hat",
        f"item.{NS}.antlers": "Reindeer Antlers", f"item.{NS}.winter_beanie": "Winter Beanie",
        f"container.{NS}.present": "Present",
        f"{NS}.present.not_yours": "Someone else is wrapping this present",
        f"{NS}.present.empty": "Put something in the present first",
        f"{NS}.present.cannot_wrap": "This present can't be wrapped",
        f"{NS}.present.wrapped": "Wrapped! Break it to pick it up, or leave it for its recipient",
        f"{NS}.present.anyone": "Anyone",
        f"{NS}.present.tag": "To: %s - From: %s",
        f"{NS}.present.unwrapped_hint": "Place it, fill it, then sneak + right-click to wrap",
        f"{NS}.screen.wrap": "Wrap a Present", f"{NS}.screen.wrap.to": "To (player name, empty = anyone)",
        f"{NS}.screen.wrap.to_hint": "anyone", f"{NS}.screen.wrap.message": "Message", f"{NS}.screen.wrap.paper": "Wrapping paper",
        f"{NS}.screen.wrap.button": "Wrap it!",
        f"{NS}.gift.wishes": "%s wishes for: %s",
        f"{NS}.gift.wrap_first": "Wrap something nice in a present first",
        f"{NS}.gift.reaction.0": "%s thanks you for the thoughtful present",
        f"{NS}.gift.reaction.1": "%s loves it - just their kind of thing!",
        f"{NS}.gift.reaction.2": "%s is overjoyed - it was on their wish list!",
        f"{NS}.gift.from_villager": "%s has a present for you!",
        f"{NS}.gift.mysterious": "Jingling bells in the night... someone left a present by your hearth.",
        f"{NS}.screen.preparing_in": "Preparing (%s d)",
        f"{NS}.screen.in_days": "In %s days",
        f"{NS}.reputation.no_village": "There is no village EverFest knows within 96 blocks",
        f"{NS}.reputation.show": "Your holiday reputation here: %s (%s)",
        f"{NS}.reputation.tier.stranger": "Stranger", f"{NS}.reputation.tier.neighbour": "Neighbour",
        f"{NS}.reputation.tier.friend": "Friend", f"{NS}.reputation.tier.cherished": "Cherished", f"{NS}.reputation.tier.legend": "Legend",
    })
    with open(p, "w") as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write("\n")


def spring_and_feasts():
    eggs = ["easter_egg_pink", "easter_egg_blue", "easter_egg_yellow", "easter_egg_green", "golden_egg"]
    for n in eggs + ["easter_basket", "chocolate_box", "harvest_pie"]:
        simple_item(n)
    write(f"{D}/tags/item/easter_eggs.json", {"values": [t(e) for e in eggs]})
    shaped("easter_basket", ["S S", "WSW", "WWW"], {"S": "minecraft:stick", "W": "minecraft:wheat"})
    shapeless("chocolate_box", ["minecraft:cocoa_beans", "minecraft:cocoa_beans", "minecraft:sugar", "minecraft:paper", "minecraft:red_dye"], 2)
    shapeless("harvest_pie", ["minecraft:pumpkin", "minecraft:apple", "minecraft:wheat", "minecraft:egg", "minecraft:sugar"], 2)
    p = f"{A}/lang/en_us.json"
    with open(p) as f:
        lang = json.load(f)
    lang.update({
        f"item.{NS}.easter_egg_pink": "Pink Easter Egg", f"item.{NS}.easter_egg_blue": "Blue Easter Egg",
        f"item.{NS}.easter_egg_yellow": "Yellow Easter Egg", f"item.{NS}.easter_egg_green": "Green Easter Egg",
        f"item.{NS}.golden_egg": "Golden Egg", f"item.{NS}.easter_basket": "Easter Basket",
        f"item.{NS}.chocolate_box": "Box of Chocolates", f"item.{NS}.harvest_pie": "Harvest Pie",
        f"{NS}.easter.golden": "You found a Golden Egg! What luck!",
        f"{NS}.new_year.happy": "Happy New Year!",
    })
    with open(p, "w") as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write("\n")


def update_lang(entries):
    p = f"{A}/lang/en_us.json"
    with open(p) as f:
        lang = json.load(f)
    lang.update(entries)
    with open(p, "w") as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write("\n")


def entity_loot(mob, pools):
    write(f"{D}/loot_table/entities/{mob}.json", {"type": "minecraft:entity", "pools": pools, "random_sequence": t("entities/" + mob)})


def drop(item, lo, hi, smelt=False):
    functions = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    if smelt:
        functions.append({"function": "minecraft:furnace_smelt", "conditions": [{"condition": "minecraft:entity_properties",
                          "entity": "this", "predicate": {"flags": {"is_on_fire": True}}}]})
    functions.append({"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting", "count": {"min": 0, "max": 1}})
    return {"rolls": 1, "entries": [{"type": "minecraft:item", "name": item, "functions": functions}]}


def wildlife():
    for n in ("raw_turkey", "roast_turkey"):
        simple_item(n)
    for egg in ("reindeer", "spring_bunny", "wild_turkey"):
        write(f"{A}/models/item/{egg}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    entity_loot("reindeer", [drop("minecraft:leather", 0, 2)])
    entity_loot("spring_bunny", [drop("minecraft:rabbit_hide", 0, 1)])
    entity_loot("wild_turkey", [drop("minecraft:feather", 0, 2), drop(t("raw_turkey"), 1, 1, smelt=True)])
    for kind, time in (("smelting", 200), ("smoking", 100), ("campfire_cooking", 600)):
        write(f"{D}/recipe/roast_turkey_from_{kind}.json", {"type": f"minecraft:{kind}", "category": "food",
              "ingredient": {"item": t("raw_turkey")}, "result": {"id": t("roast_turkey")}, "experience": 0.35, "cookingtime": time})
    write(f"{D}/tags/item/reindeer_food.json", {"values": ["minecraft:wheat", "minecraft:sweet_berries"]})
    write(f"{D}/tags/item/turkey_food.json", {"values": ["minecraft:wheat_seeds", "minecraft:pumpkin_seeds", "minecraft:melon_seeds",
                                                          "minecraft:beetroot_seeds"]})
    write(os.path.join(ROOT, "data", "minecraft", "tags", "item", "meat.json"), {"replace": False, "values": [t("raw_turkey"), t("roast_turkey")]})
    write(f"{D}/tags/block/reindeer_spawnable_on.json", {"values": ["minecraft:grass_block", "minecraft:snow", "minecraft:snow_block",
                                                                     "minecraft:podzol", "minecraft:coarse_dirt"]})
    write(f"{D}/tags/worldgen/biome/spawns_reindeer.json", {"values": ["minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:grove",
                                                                        "minecraft:snowy_slopes", "minecraft:ice_spikes"]})
    write(f"{D}/tags/worldgen/biome/spawns_spring_bunnies.json", {"values": ["minecraft:plains", "minecraft:sunflower_plains",
          "minecraft:meadow", "minecraft:flower_forest", "minecraft:cherry_grove", "minecraft:forest", "minecraft:birch_forest"]})
    write(f"{D}/tags/worldgen/biome/spawns_wild_turkeys.json", {"values": ["minecraft:forest", "minecraft:birch_forest",
          "minecraft:old_growth_birch_forest", "minecraft:dark_forest", "minecraft:taiga", "minecraft:old_growth_pine_taiga",
          "minecraft:old_growth_spruce_taiga", "minecraft:plains", "minecraft:meadow", "minecraft:windswept_forest"]})
    write(f"{D}/neoforge/biome_modifier/reindeer.json", {"type": "neoforge:add_spawns", "biomes": "#holidayseasons:spawns_reindeer",
          "spawners": {"type": t("reindeer"), "weight": 5, "minCount": 2, "maxCount": 4}})
    wl = f"{NS}.configuration.wildlife"
    update_lang({
        f"entity.{NS}.reindeer": "Reindeer", f"entity.{NS}.spring_bunny": "Spring Bunny", f"entity.{NS}.wild_turkey": "Wild Turkey",
        f"item.{NS}.reindeer_spawn_egg": "Reindeer Spawn Egg", f"item.{NS}.spring_bunny_spawn_egg": "Spring Bunny Spawn Egg",
        f"item.{NS}.wild_turkey_spawn_egg": "Wild Turkey Spawn Egg",
        f"item.{NS}.raw_turkey": "Raw Turkey", f"item.{NS}.roast_turkey": "Roast Turkey",
        wl: "Wildlife", f"{wl}.reindeer": "Reindeer", f"{wl}.springBunnies": "Spring Bunnies", f"{wl}.wildTurkeys": "Wild Turkeys",
        f"{wl}.seasonalSpawnIntervalTicks": "Seasonal Visit Interval (ticks)", f"{wl}.seasonalSpawnChance": "Seasonal Visit Chance",
        f"{wl}.maxSeasonalAnimalsNearPlayer": "Max Seasonal Animals Near A Player",
        f"{NS}.configuration.festiveAnimalOutfits": "Festive Animal Outfits",
    })


def chest_loot(name, rolls, entries):
    """entries: (item, weight, min, max) or (item, weight, min, max, 'enchant')."""
    out = []
    for e in entries:
        item, weight, lo, hi = e[:4]
        functions = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}] if hi > 1 else []
        if len(e) > 4 and e[4] == "enchant":
            functions = [{"function": "minecraft:enchant_randomly"}]
        entry = {"type": "minecraft:item", "name": item, "weight": weight}
        if functions:
            entry["functions"] = functions
        out.append(entry)
    write(f"{D}/loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": rolls[0],
          "max": rolls[1]}, "entries": out}], "random_sequence": t("chests/" + name)})


def worldgen():
    landmarks = {
        "haunted_cottage": (["minecraft:dark_forest", "minecraft:swamp", "minecraft:taiga", "minecraft:old_growth_pine_taiga",
                             "minecraft:old_growth_spruce_taiga"], 38, 14, 31031, 3),
        "forgotten_graveyard": (["minecraft:plains", "minecraft:forest", "minecraft:birch_forest", "minecraft:dark_forest",
                                 "minecraft:taiga", "minecraft:meadow"], 34, 12, 11022, 2),
        "seasonal_cabin": (["minecraft:snowy_taiga", "minecraft:snowy_plains", "minecraft:grove", "minecraft:taiga"], 34, 12, 12251, 4),
    }
    for name, (biomes, spacing, separation, salt, slope) in landmarks.items():
        write(f"{D}/tags/worldgen/biome/has_structure/{name}.json", {"values": biomes})
        write(f"{D}/worldgen/structure/{name}.json", {"type": t("holiday_landmark"), "biomes": f"#{NS}:has_structure/{name}",
              "step": "surface_structures", "terrain_adaptation": "beard_thin", "spawn_overrides": {},
              "template": t("landmark/" + name), "config_key": name, "max_slope": slope})
        write(f"{D}/worldgen/structure_set/{name}.json", {"structures": [{"structure": t(name), "weight": 1}],
              "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": separation, "salt": salt}})
    candy = [(t(c), 6, 1, 4) for c in CANDY]
    chest_loot("haunted_cottage", (3, 6), candy + [(t("costume_" + c), 2, 1, 1) for c in COSTUMES] + [
        (t("ectoplasm"), 6, 1, 3), ("minecraft:pumpkin_seeds", 8, 2, 6), (t("carving_knife"), 2, 1, 1), ("minecraft:string", 6, 1, 4),
        ("minecraft:bone", 6, 1, 4), ("minecraft:emerald", 3, 1, 3), ("minecraft:golden_apple", 1, 1, 1)])
    chest_loot("forgotten_graveyard", (3, 6), [
        ("minecraft:bone", 10, 2, 6), ("minecraft:rotten_flesh", 8, 1, 4), (t("ectoplasm"), 6, 1, 3), ("minecraft:candle", 5, 1, 3),
        ("minecraft:iron_ingot", 4, 1, 3), ("minecraft:gold_nugget", 4, 2, 6), ("minecraft:emerald", 2, 1, 2),
        (t("tombstone"), 2, 1, 1), (t("costume_skeleton"), 2, 1, 1), ("minecraft:soul_lantern", 2, 1, 1),
        ("minecraft:book", 1, 1, 1, "enchant")])
    chest_loot("seasonal_cabin", (3, 6), [
        ("minecraft:cookie", 10, 3, 8), ("minecraft:sweet_berries", 8, 2, 6), (t("ornament"), 8, 1, 4), (t("santa_hat"), 2, 1, 1),
        (t("elf_hat"), 2, 1, 1), (t("antlers"), 2, 1, 1), (t("winter_beanie"), 2, 1, 1), ("minecraft:spruce_sapling", 5, 1, 3),
        ("minecraft:snowball", 6, 2, 8), (t("string_lights"), 3, 1, 3), (t("wreath"), 2, 1, 1), ("minecraft:emerald", 3, 1, 3),
        ("minecraft:campfire", 2, 1, 1)])
    wg = f"{NS}.configuration.worldgen"
    update_lang({wg: "World Generation", f"{wg}.hauntedCottages": "Haunted Cottages", f"{wg}.forgottenGraveyards": "Forgotten Graveyards",
                 f"{wg}.seasonalCabins": "Seasonal Cabins"})


def economy():
    simple_item("holiday_token")
    write(f"{A}/models/item/holiday_merchant_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    wood, snow, tree, glass = "minecraft:block/dark_oak_planks", "minecraft:block/snow", t("block/snow_globe_tree"), "minecraft:block/glass"
    write(f"{A}/models/block/snow_globe.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
          "textures": {"wood": wood, "snow": snow, "tree": tree, "glass": glass, "particle": glass},
          "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 2, 0], "scale": [0.9, 0.9, 0.9]},
                      "ground": {"translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
                      "fixed": {"scale": [0.8, 0.8, 0.8]},
                      "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.5, 0.5, 0.5]},
                      "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.5, 0.5, 0.5]}},
          "elements": [cube([4, 0, 4], [12, 2, 12], "#wood"), cube([5, 2, 5], [11, 3, 11], "#snow"),
                       cube([6, 3, 6], [10, 5.5, 10], "#tree"), cube([7, 5.5, 7], [9, 8, 9], "#tree"),
                       cube([4.5, 2, 4.5], [11.5, 10, 11.5], "#glass", faces=("north", "south", "east", "west", "up"))]})
    write(f"{A}/blockstates/snow_globe.json", facing_variants(t("block/snow_globe")))
    write(f"{A}/models/item/snow_globe.json", {"parent": t("block/snow_globe")})
    write(f"{D}/loot_table/blocks/snow_globe.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
          "entries": [{"type": "minecraft:item", "name": t("snow_globe")}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
          "random_sequence": t("blocks/snow_globe")})
    ec, ev = f"{NS}.configuration.economy", f"{NS}.configuration.events"
    update_lang({
        f"item.{NS}.holiday_token": "Festival Token", f"block.{NS}.snow_globe": "Snow Globe",
        f"item.{NS}.holiday_merchant_spawn_egg": "Holiday Merchant Spawn Egg", f"entity.{NS}.holiday_merchant": "Holiday Merchant",
        f"item.{NS}.peddlers_lantern": "Peddler's Lantern", f"item.{NS}.peddlers_lantern.lore": "It still smells of fog and candle smoke",
        f"{NS}.peddler.arrives": "A cloaked peddler steps out of the dark, lantern swinging...",
        f"{NS}.tokens.earned": "+%s Festival Token (%s)", f"{NS}.tokens.reason.egg": "found an egg",
        f"{NS}.tokens.reason.present": "a gift well chosen", f"{NS}.tokens.reason.objective": "village objective",
        f"{NS}.tokens.reason.party": "hosted a party",
        ec: "Economy", f"{ec}.holidayTokens": "Festival Tokens", f"{ec}.holidayMerchants": "Holiday Merchants",
        f"{ec}.merchantVisitChance": "Merchant Visit Chance", f"{ec}.merchantStayTicks": "Merchant Stay (ticks)",
        ev: "Rare Events", f"{ev}.midnightPeddler": "Midnight Peddler", f"{ev}.midnightPeddlerChance": "Midnight Peddler Chance",
    })


def events_and_parties():
    ev, pa = f"{NS}.configuration.events", f"{NS}.configuration.parties"
    update_lang({
        f"{NS}.objective.bar": "Village objective: bring %s %s to the bell (%s/%s)",
        f"{NS}.objective.done": "Village objective complete: %s. Thank you!",
        f"{NS}.objective.thanks": "You brought %s %s to the village (%s/%s)",
        f"{NS}.objective.complete": "The village finished its holiday objective (%s) - they will remember your help!",
        f"{NS}.objective.tag.candy": "candy", f"{NS}.objective.tag.easter_eggs": "Easter eggs", f"{NS}.objective.tag.small_flowers": "flowers",
        f"{NS}.graveyard.stirs": "The graves stir... something is rising.",
        f"{NS}.screen.hearth.party": "Host a holiday party",
        f"{NS}.party.disabled": "Parties are turned off on this server",
        f"{NS}.party.no_holiday": "Parties can only be hosted while a holiday is being celebrated",
        f"{NS}.party.already": "A party is already going on here",
        f"{NS}.party.nobody": "Nobody nearby accepted your invitation. Get to know the neighbours first!",
        f"{NS}.party.started": "You invited the neighbours: %s villagers are on their way",
        f"{NS}.party.arrived": "%s arrived at your party (%s guests)",
        f"{NS}.party.over": "Your party is over: %s of %s invited villagers came",
        f"{ev}.villageObjectives": "Village Objectives", f"{ev}.graveyardHaunting": "Graveyard Haunting", f"{ev}.festivalMarkets": "Festival Markets",
        pa: "Parties", f"{pa}.hearthParties": "Hearth Parties", f"{pa}.maxPartyGuests": "Max Party Guests",
        f"{pa}.partyInviteRadius": "Party Invite Radius", f"{pa}.partyDurationTicks": "Party Duration (ticks)",
    })


MUSIC = ["lantern_hollow", "hearthlight", "meadow_morning", "harvest_table", "midnight_fireworks"]


def music_and_particles():
    with open(f"{A}/sounds.json") as f:
        sounds = json.load(f)
    for m in MUSIC:
        sounds[f"music.{m}"] = {"sounds": [{"name": t("music/" + m), "stream": True}]}
    write(f"{A}/sounds.json", sounds)
    write(f"{A}/particles/autumn_leaf.json", {"textures": [t(f"autumn_leaf_{i}") for i in range(4)]})
    update_lang({f"{NS}.configuration.holidayMusic": "Holiday Music"})


ADVANCEMENTS = [
    # id, parent, icon, frame, trigger (None = granted by code)
    ("home_sweet_home", "root", "holidayseasons:holiday_hearth", "task", None),
    ("life_of_the_party", "home_sweet_home", "minecraft:cake", "goal", None),
    ("trick_or_treat", "root", "holidayseasons:candy_corn", "task", None),
    ("the_peddler", "trick_or_treat", "minecraft:soul_lantern", "goal", None),
    ("restless_graves", "the_peddler", "holidayseasons:tombstone", "goal", None),
    ("well_chosen", "root", "holidayseasons:present", "task", None),
    ("egg_hunter", "root", "holidayseasons:easter_egg_pink", "task", None),
    ("golden_find", "egg_hunter", "holidayseasons:golden_egg", "goal", "holidayseasons:golden_egg"),
    ("happy_new_year", "root", "minecraft:firework_rocket", "task", None),
    ("pillar_of_the_community", "root", "minecraft:bell", "task", None),
    ("local_legend", "pillar_of_the_community", "minecraft:emerald", "challenge", None),
    ("red_nosed", "root", "holidayseasons:reindeer_spawn_egg", "goal", None),
    ("a_new_chapter", "root", "holidayseasons:collection_book", "task", "holidayseasons:collection_book"),
    ("collector", "a_new_chapter", "holidayseasons:snow_globe", "challenge", None),
    ("a_year_of_festivals", "a_new_chapter", "holidayseasons:wall_calendar", "challenge", None),
]
ADV_TEXT = {
    "root": ("EverFest", "Celebrate the holidays with the villagers"),
    "home_sweet_home": ("Home Sweet Home", "Make a house your home with a Holiday Hearth"),
    "life_of_the_party": ("Life of the Party", "Host a holiday party that at least three villagers come to"),
    "trick_or_treat": ("Trick or Treat!", "Give candy to a child out trick-or-treating"),
    "the_peddler": ("Strange Wares", "Meet the Midnight Peddler on Halloween night"),
    "restless_graves": ("Restless Graves", "Wake the ghosts of a forgotten graveyard on Halloween night"),
    "well_chosen": ("Just What I Wished For", "Give a villager the present they wished for"),
    "egg_hunter": ("Egg Hunter", "Find 10 hidden Easter eggs"),
    "golden_find": ("Golden Find", "Find a Golden Egg"),
    "happy_new_year": ("Happy New Year!", "Be there when the countdown reaches midnight"),
    "pillar_of_the_community": ("Pillar of the Community", "Help a village finish its holiday objective"),
    "local_legend": ("Local Legend", "Become a Legend in a village"),
    "red_nosed": ("A Very Shiny Nose", "Spot a festive red-nosed reindeer"),
    "a_new_chapter": ("A New Chapter", "Get a Holiday Collection Book"),
    "collector": ("Collector", "Collect every collectible in the Holiday Collection Book"),
    "a_year_of_festivals": ("A Year of Festivals", "Celebrate every holiday on the calendar"),
}


def progression():
    simple_item("collection_book")
    shapeless("collection_book", ["minecraft:book", "minecraft:gold_nugget", "minecraft:red_dye"])
    base = f"{D}/advancement/everfest"
    root_text = ADV_TEXT["root"]
    write(f"{base}/root.json", {"display": {"icon": {"id": t("wall_calendar")}, "title": {"translate": f"advancements.{NS}.root.title"},
          "description": {"translate": f"advancements.{NS}.root.description"}, "background": "minecraft:textures/block/spruce_planks.png",
          "show_toast": False, "announce_to_chat": False}, "criteria": {"tick": {"trigger": "minecraft:tick"}}})
    lang = {f"advancements.{NS}.root.title": root_text[0], f"advancements.{NS}.root.description": root_text[1]}
    for adv_id, parent, icon, frame, item in ADVANCEMENTS:
        if item is None:
            criteria = {"done": {"trigger": "minecraft:impossible"}}
        else:
            criteria = {"done": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": item}]}}}
        write(f"{base}/{adv_id}.json", {"parent": f"{NS}:everfest/{parent}", "display": {"icon": {"id": icon},
              "title": {"translate": f"advancements.{NS}.{adv_id}.title"}, "description": {"translate": f"advancements.{NS}.{adv_id}.description"},
              "frame": frame, "show_toast": True, "announce_to_chat": True, "hidden": False},
              "criteria": criteria, "requirements": [["done"]]})
        lang[f"advancements.{NS}.{adv_id}.title"] = ADV_TEXT[adv_id][0]
        lang[f"advancements.{NS}.{adv_id}.description"] = ADV_TEXT[adv_id][1]
    c = f"{NS}.collection"
    lang.update({
        f"item.{NS}.collection_book": "Holiday Collection Book",
        f"{c}.title": "Holiday Collection Book", f"{c}.complete": "%s%% complete",
        f"{c}.tab.holidays": "Holidays", f"{c}.tab.collectibles": "Collection", f"{c}.tab.wildlife": "Wildlife",
        f"{c}.tab.achievements": "Feats", f"{c}.tab.encounters": "Encounters",
        f"{c}.celebrated": "%s: celebrated %s time(s) (year %s)", f"{c}.not_yet": "%s: not celebrated yet",
        f"{c}.section.decorations": "Decorations", f"{c}.section.costumes": "Costumes and hats",
        f"{c}.section.treats": "Treats", f"{c}.section.rare": "Rare finds",
        f"{c}.unknown_item": "???", f"{c}.not_found": "Not found yet", f"{c}.undiscovered": "Not discovered yet",
        f"{c}.new_entry": "New in your Collection Book: %s",
        f"{c}.wildlife.reindeer.brown": "Reindeer (brown)", f"{c}.wildlife.reindeer.frosty": "Reindeer (frosty)",
        f"{c}.wildlife.reindeer.festive": "Red-nosed reindeer", f"{c}.wildlife.spring_bunny.cream": "Spring bunny (cream)",
        f"{c}.wildlife.spring_bunny.blush": "Spring bunny (blush)", f"{c}.wildlife.spring_bunny.sky": "Spring bunny (sky)",
        f"{c}.wildlife.spring_bunny.lilac": "Spring bunny (lilac)", f"{c}.wildlife.wild_turkey": "Wild turkey",
        f"{c}.encounter.holiday_merchant": "Traded with a holiday merchant", f"{c}.encounter.midnight_peddler": "Met the Midnight Peddler",
        f"{c}.encounter.graveyard_haunting": "Woke a haunted graveyard", f"{c}.encounter.mysterious_gift": "Received a mysterious gift",
        f"{c}.encounter.firework_show": "Watched a holiday firework show", f"{c}.encounter.trick_or_treaters": "Trick-or-treaters at your door",
        f"{c}.encounter.hearth_party": "Hosted a hearth party",
        f"{c}.achievement.eggs_found": "Hidden eggs found: %s/%s", f"{c}.achievement.candy_given": "Candy given to children: %s/%s",
        f"{c}.achievement.presents_given": "Presents given to villagers: %s/%s",
        f"{c}.achievement.objectives_completed": "Village objectives helped: %s/%s",
        f"{c}.achievement.parties_hosted": "Parties hosted: %s/%s", f"{c}.achievement.guests_welcomed": "Party guests welcomed: %s/%s",
        f"{c}.totals": "All time: %s eggs, %s candy, %s presents, %s guests",
    })
    update_lang(lang)


if __name__ == "__main__":
    items()
    blocks()
    loot()
    recipes()
    tags()
    lang(sounds())
    christmas()
    spring_and_feasts()
    wildlife()
    worldgen()
    economy()
    events_and_parties()
    music_and_particles()
    progression()
    print("assets generated")
