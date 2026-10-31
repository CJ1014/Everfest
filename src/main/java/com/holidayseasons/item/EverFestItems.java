package com.holidayseasons.item;

import com.holidayseasons.EverFest;
import com.holidayseasons.block.CarvedPumpkinVariantBlock;
import com.holidayseasons.block.EverFestBlocks;
import com.holidayseasons.entity.EverFestEntities;
import com.holidayseasons.villager.VillagerPersonality;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.BundleContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class EverFestItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EverFest.MOD_ID);
    public static final TagKey<Item> CANDY_TAG = TagKey.create(Registries.ITEM, EverFest.id("candy"));
    public static final TagKey<Item> COSTUME_TAG = TagKey.create(Registries.ITEM, EverFest.id("costumes"));

    public static final DeferredItem<WallCalendarItem> WALL_CALENDAR = ITEMS.register("wall_calendar",
            () -> new WallCalendarItem(EverFestBlocks.WALL_CALENDAR.get(), new Item.Properties().stacksTo(16)));

    // ---- candy
    public static final DeferredItem<Item> CANDY_CORN = candy("candy_corn", 2, null);
    public static final DeferredItem<Item> CARAMEL_APPLE = candy("caramel_apple", 5, null);
    public static final DeferredItem<Item> CHOCOLATE_BAR = candy("chocolate_bar", 3, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
    public static final DeferredItem<Item> LOLLIPOP = candy("lollipop", 2, null);
    public static final DeferredItem<Item> GUMMY_BAT = candy("gummy_bat", 2, new MobEffectInstance(MobEffects.JUMP, 200, 0));

    public static final DeferredItem<CandyBagItem> CANDY_BAG = ITEMS.register("candy_bag",
            () -> new CandyBagItem(new Item.Properties().stacksTo(1).component(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY)));

    // ---- costumes
    public static final Map<VillagerPersonality.Costume, DeferredItem<CostumeItem>> COSTUMES = new EnumMap<>(VillagerPersonality.Costume.class);

    static {
        for (VillagerPersonality.Costume c : VillagerPersonality.Costume.values()) {
            COSTUMES.put(c, ITEMS.register("costume_" + c.name().toLowerCase(Locale.ROOT),
                    () -> new CostumeItem(c, new Item.Properties().stacksTo(1))));
        }
    }

    public static final DeferredItem<CarvingKnifeItem> CARVING_KNIFE = ITEMS.register("carving_knife",
            () -> new CarvingKnifeItem(new Item.Properties().durability(128)));
    public static final DeferredItem<Item> ECTOPLASM = ITEMS.registerSimpleItem("ectoplasm", new Item.Properties().rarity(Rarity.UNCOMMON));

    // ---- block items
    public static final DeferredItem<BlockItem> CANDY_BOWL = ITEMS.registerSimpleBlockItem(EverFestBlocks.CANDY_BOWL);
    public static final DeferredItem<BlockItem> DOORBELL = ITEMS.registerSimpleBlockItem(EverFestBlocks.DOORBELL);
    public static final DeferredItem<BlockItem> HOLIDAY_HEARTH = ITEMS.registerSimpleBlockItem(EverFestBlocks.HOLIDAY_HEARTH);
    public static final DeferredItem<BlockItem> TOMBSTONE = ITEMS.registerSimpleBlockItem(EverFestBlocks.TOMBSTONE);
    public static final DeferredItem<BlockItem> HANGING_GHOST = ITEMS.registerSimpleBlockItem(EverFestBlocks.HANGING_GHOST);
    public static final Map<CarvedPumpkinVariantBlock.Face, DeferredItem<BlockItem>> CARVED = new EnumMap<>(CarvedPumpkinVariantBlock.Face.class);
    public static final Map<CarvedPumpkinVariantBlock.Face, DeferredItem<BlockItem>> JACK = new EnumMap<>(CarvedPumpkinVariantBlock.Face.class);

    static {
        for (CarvedPumpkinVariantBlock.Face f : CarvedPumpkinVariantBlock.Face.values()) {
            CARVED.put(f, ITEMS.registerSimpleBlockItem(EverFestBlocks.CARVED.get(f)));
            JACK.put(f, ITEMS.registerSimpleBlockItem(EverFestBlocks.JACK.get(f)));
        }
    }

    // ---- christmas
    public static final DeferredItem<PresentItem> PRESENT = ITEMS.register("present",
            () -> new PresentItem(EverFestBlocks.PRESENT.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> ORNAMENT = ITEMS.registerSimpleItem("ornament", new Item.Properties());
    public static final DeferredItem<FestiveHatItem> SANTA_HAT = ITEMS.register("santa_hat", () -> new FestiveHatItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<FestiveHatItem> ELF_HAT = ITEMS.register("elf_hat", () -> new FestiveHatItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<FestiveHatItem> ANTLERS = ITEMS.register("antlers", () -> new FestiveHatItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<FestiveHatItem> WINTER_BEANIE = ITEMS.register("winter_beanie", () -> new FestiveHatItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<net.minecraft.world.item.DoubleHighBlockItem> CHRISTMAS_TREE = ITEMS.register("christmas_tree",
            () -> new net.minecraft.world.item.DoubleHighBlockItem(EverFestBlocks.CHRISTMAS_TREE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WREATH = ITEMS.registerSimpleBlockItem(EverFestBlocks.WREATH);
    public static final DeferredItem<BlockItem> STRING_LIGHTS = ITEMS.registerSimpleBlockItem(EverFestBlocks.STRING_LIGHTS);

    // ---- spring, valentines, harvest
    public static final DeferredItem<Item> EASTER_EGG_PINK = ITEMS.registerSimpleItem("easter_egg_pink", new Item.Properties());
    public static final DeferredItem<Item> EASTER_EGG_BLUE = ITEMS.registerSimpleItem("easter_egg_blue", new Item.Properties());
    public static final DeferredItem<Item> EASTER_EGG_YELLOW = ITEMS.registerSimpleItem("easter_egg_yellow", new Item.Properties());
    public static final DeferredItem<Item> EASTER_EGG_GREEN = ITEMS.registerSimpleItem("easter_egg_green", new Item.Properties());
    public static final DeferredItem<Item> GOLDEN_EGG = ITEMS.registerSimpleItem("golden_egg", new Item.Properties().rarity(Rarity.RARE).stacksTo(16));
    public static final TagKey<Item> EGG_TAG = TagKey.create(Registries.ITEM, EverFest.id("easter_eggs"));
    public static final DeferredItem<EasterBasketItem> EASTER_BASKET = ITEMS.register("easter_basket",
            () -> new EasterBasketItem(new Item.Properties().stacksTo(1).component(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY)));
    public static final DeferredItem<Item> CHOCOLATE_BOX = ITEMS.register("chocolate_box", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.4f).alwaysEdible()
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 1f).build())));
    public static final DeferredItem<Item> HARVEST_PIE = ITEMS.register("harvest_pie", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(10).saturationModifier(0.7f).build())));

    public static final DeferredItem<CollectionBookItem> COLLECTION_BOOK = ITEMS.register("collection_book",
            () -> new CollectionBookItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> HOLIDAY_TOKEN = ITEMS.registerSimpleItem("holiday_token", new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> SNOW_GLOBE = ITEMS.registerSimpleBlockItem("snow_globe", EverFestBlocks.SNOW_GLOBE,
            new Item.Properties().rarity(Rarity.RARE).stacksTo(16));
    public static final DeferredItem<Item> RAW_TURKEY = ITEMS.register("raw_turkey", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build())));
    public static final DeferredItem<Item> ROAST_TURKEY = ITEMS.register("roast_turkey", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8f).build())));

    // ---- spawn eggs
    public static final DeferredItem<DeferredSpawnEggItem> GHOST_SPAWN_EGG = ITEMS.register("ghost_spawn_egg",
            () -> new DeferredSpawnEggItem(EverFestEntities.GHOST, 0xE8F0FF, 0x7A8CB8, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GOURDLING_SPAWN_EGG = ITEMS.register("gourdling_spawn_egg",
            () -> new DeferredSpawnEggItem(EverFestEntities.GOURDLING, 0xE07B1A, 0x3A6B1F, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> SCARECROW_SPAWN_EGG = ITEMS.register("scarecrow_spawn_egg",
            () -> new DeferredSpawnEggItem(EverFestEntities.SCARECROW, 0xD9B44A, 0x6B4F2A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> HOLIDAY_MERCHANT_SPAWN_EGG = ITEMS.register("holiday_merchant_spawn_egg",
            () -> new DeferredSpawnEggItem(EverFestEntities.HOLIDAY_MERCHANT, 0xB8322A, 0xF4EBD0, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> REINDEER_SPAWN_EGG = ITEMS.register("reindeer_spawn_egg",
            () -> new DeferredSpawnEggItem(EverFestEntities.REINDEER, 0x7A5634, 0xE9E1D2, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> SPRING_BUNNY_SPAWN_EGG = ITEMS.register("spring_bunny_spawn_egg",
            () -> new DeferredSpawnEggItem(EverFestEntities.SPRING_BUNNY, 0xF6D6E4, 0xB9D8F2, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> WILD_TURKEY_SPAWN_EGG = ITEMS.register("wild_turkey_spawn_egg",
            () -> new DeferredSpawnEggItem(EverFestEntities.WILD_TURKEY, 0x4A3526, 0xB8432F, new Item.Properties()));

    private EverFestItems() {
    }

    private static DeferredItem<Item> candy(String name, int nutrition, MobEffectInstance effect) {
        return ITEMS.register(name, () -> {
            FoodProperties.Builder food = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(0.15f).alwaysEdible().fast();
            if (effect != null) food.effect(() -> new MobEffectInstance(effect), 1f);
            return new Item(new Item.Properties().food(food.build()));
        });
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
