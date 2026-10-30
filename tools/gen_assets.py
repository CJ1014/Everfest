#!/usr/bin/env python3
"""Generates EverFest's JSON resources: models, blockstates, loot tables, recipes, tags, sounds.json and language
entries for the Halloween content.  python3 tools/gen_assets.py"""
import json
import os

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
             "whispers", "gourdling_hop", "gourdling_hurt", "gourdling_death", "scarecrow_rustle", "scarecrow_creak", "trick_poof"]
    write(f"{A}/sounds.json", {n: {"sounds": [t(n)], "subtitle": f"subtitles.{NS}.{n}"} for n in names})
    return names


def lang(sound_names):
    p = f"{A}/lang/en_us.json"
    with open(p) as f:
        lang = json.load(f)
    nice = lambda s: s.replace("_", " ").title()
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
        f"{NS}.home.unregistered": "This hearth isn't linked to a home",
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
                 "trick_poof": "Trick!"}
    for n in sound_names:
        lang[f"subtitles.{NS}.{n}"] = subtitles[n]
    with open(p, "w") as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write("\n")


if __name__ == "__main__":
    items()
    blocks()
    loot()
    recipes()
    tags()
    lang(sounds())
    print("assets generated")
