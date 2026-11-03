package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-owned metadata for the small Tournament inventory catalog.
 *
 * <p>Catalog ids identify durable item kinds. They are deliberately not the
 * runtime object ids used by the native inventory packets.</p>
 */
final class InventoryCatalog {
    static final String HOME_SCROLL_SOURCE_SHA256 =
            "A754539F710E6B6C6237705F749DC0D49B5A3245E3FAB1DCFC2EF88089FC512B";
    static final String ANCIENT_SWORD_SOURCE_SHA256 =
            "C8A12A282EF5430040C5D9CD9AB67255B0C8F9AA142D59242B90F23D162070BE";
    static final String SOURCE_ARMOR_STATUS_SHA256 =
            "9AEDCD8E4E47BDE4EE4C14A6FF7A8DFCA51537DDABA0E1B8DAD67200B2F05557";
    static final long SHORT_SWORD = 24;
    static final long RED_KNIGHT_SWORD = 30;
    static final long ANCIENT_SWORD = 68;
    static final long PAN_HORN = 88;
    static final long DEXTERITY_MAGIC_HELM = 20013;
    static final long STRENGTH_MAGIC_HELM = 20015;
    static final long TRAINING_SWORD = 1001;
    static final long HEALING_POTION = 1002;
    static final long ARENA_TOKEN = 2001;
    static final long MAGIC_LANTERN = 40004;
    static final long LIGHTNING_WAND = 40007;
    static final long NATIVE_HEALING_POTION = 40010;
    static final long EXTRA_HEALING_POTION = 40011;
    static final long GREATER_HEALING_POTION = 40012;
    static final long SELF_HASTE_POTION = 40013;
    static final long BRAVE_POTION = 40014;
    static final long BLUE_POTION = 40015;
    static final long WISDOM_POTION = 40016;
    static final long CURE_POISON_POTION = 40017;
    static final long BLINDNESS_POTION = 40025;
    static final long BLESS_OF_EVA = 40032;
    static final long DRAGON_DIAMOND = BattleExperienceGemCatalog.DRAGON_DIAMOND;
    static final long DRAGON_SAPPHIRE = BattleExperienceGemCatalog.DRAGON_SAPPHIRE;
    static final long DRAGON_RUBY = BattleExperienceGemCatalog.DRAGON_RUBY;
    static final long EVOLUTION_FRUIT = 40070;
    static final long ELVEN_WAFER = 40068;
    static final long EARTH_DRAGON_EYE = BattleDragonEyeCatalog.EARTH_DRAGON_EYE;
    static final long WATER_DRAGON_EYE = BattleDragonEyeCatalog.WATER_DRAGON_EYE;
    static final long WIND_DRAGON_EYE = BattleDragonEyeCatalog.WIND_DRAGON_EYE;
    static final long FIRE_DRAGON_EYE = BattleDragonEyeCatalog.FIRE_DRAGON_EYE;
    static final long BIRTH_DRAGON_EYE = BattleDragonEyeCatalog.BIRTH_DRAGON_EYE;
    static final long IMAGE_DRAGON_EYE = BattleDragonEyeCatalog.IMAGE_DRAGON_EYE;
    static final long LIFE_DRAGON_EYE = BattleDragonEyeCatalog.LIFE_DRAGON_EYE;
    static final long PET_COLLAR = 40314;
    static final long WHISTLE = 40315;
    static final long HIGH_PET_COLLAR = 40316;
    static final long WHETSTONE = 40317;
    static final long SKELETON_HOLY_GRAIL =
            BattlePetRaceRewardCatalog.SKELETON_HOLY_GRAIL;
    static final long HERO_PUMPKIN_BAG =
            BattlePetRaceRewardCatalog.HERO_PUMPKIN_BAG;
    static final long VICTORY_FRUIT = 41310;
    static final long PET_CALLING_FLUTE = 41160;
    static final long WISDOM_COIN = 40736;
    static final long ARROW = 40743;
    static final long SILVER_ARROW = 40744;
    static final long MITHRIL_ARROW = 40746;
    static final long CASTLE_TREBUCHET_SHELL =
            ClassicWorldTrebuchetCatalog.NORMAL_SHELL_ITEM_ID;
    static final long COOKING_BOOK_LEVEL_ONE = BattleCookingCraftingCatalog.LEVEL_ONE_BOOK;
    static final long COOKING_BOOK_LEVEL_TWO = BattleCookingCraftingCatalog.LEVEL_TWO_BOOK;
    static final long COOKING_BOOK_LEVEL_THREE = BattleCookingCraftingCatalog.LEVEL_THREE_BOOK;
    static final long COOKING_FUEL = 41260;
    static final int COOKING_FUEL_MIN_LEVEL = 60;
    static final long FLOATING_EYE_STEAK = BattleCookingCatalog.FLOATING_EYE_STEAK;
    static final long ROAST_BEAR_MEAT = BattleCookingCatalog.ROAST_BEAR_MEAT;
    static final long NUT_PANCAKE = BattleCookingCatalog.NUT_PANCAKE;
    static final long ANT_LEG_CHEESE = BattleCookingCatalog.ANT_LEG_CHEESE;
    static final long FRUIT_SALAD = BattleCookingCatalog.FRUIT_SALAD;
    static final long SWEET_SOUR_MEAT = BattleCookingCatalog.SWEET_SOUR_MEAT;
    static final long BOAR_SKEWER = BattleCookingCatalog.BOAR_SKEWER;
    static final long MUSHROOM_SOUP = BattleCookingCatalog.MUSHROOM_SOUP;
    static final long SPECIAL_FLOATING_EYE_STEAK =
            BattleCookingCatalog.SPECIAL_FLOATING_EYE_STEAK;
    static final long SPECIAL_ROAST_BEAR_MEAT =
            BattleCookingCatalog.SPECIAL_ROAST_BEAR_MEAT;
    static final long SPECIAL_NUT_PANCAKE = BattleCookingCatalog.SPECIAL_NUT_PANCAKE;
    static final long SPECIAL_ANT_LEG_CHEESE =
            BattleCookingCatalog.SPECIAL_ANT_LEG_CHEESE;
    static final long SPECIAL_FRUIT_SALAD = BattleCookingCatalog.SPECIAL_FRUIT_SALAD;
    static final long SPECIAL_SWEET_SOUR_MEAT =
            BattleCookingCatalog.SPECIAL_SWEET_SOUR_MEAT;
    static final long SPECIAL_BOAR_SKEWER = BattleCookingCatalog.SPECIAL_BOAR_SKEWER;
    static final long SPECIAL_MUSHROOM_SOUP = BattleCookingCatalog.SPECIAL_MUSHROOM_SOUP;
    static final long SCORPION_SKEWER = BattleCookingCatalog.SCORPION_SKEWER;
    static final long SPECIAL_SCORPION_SKEWER = BattleCookingCatalog.SPECIAL_SCORPION_SKEWER;
    static final long GRILLED_WYVERN = BattleCookingCatalog.GRILLED_WYVERN;
    static final long SPECIAL_GRILLED_WYVERN = BattleCookingCatalog.SPECIAL_GRILLED_WYVERN;
    static final long SHIP_TICKET_TO_TALKING_ISLAND = 40298;
    static final long SHIP_TICKET_TO_GLUDIO = 40299;
    static final long SHIP_TICKET_FORGOTTEN_ISLAND = 40300;
    static final long SHIP_TICKET_HEINE = 40301;
    static final long SHIP_TICKET_PIRATE_ISLAND = 40302;
    static final long SHIP_TICKET_HIDDEN_DOCK = 40303;
    static final long WORLD_MAP = 40373;
    static final long POLYMORPH_SCROLL = 40088;
    static final long APPRAISAL_SCROLL = 40126;
    static final long CURSE_REMOVAL_SCROLL = 40119;
    static final long HOME_SCROLL = 40079;
    static final long LETTER_PAPER = 40310;
    static final long LETTER_CLOSED = 49016;
    static final long LETTER_OPEN = 49017;
    static final long RING_OF_FIRE_RESISTANCE = 20304;
    static final long RING_OF_POLYMORPH_CONTROL = 20281;
    static final long RING_OF_SUMMON_CONTROL = 20284;
    static final long RING_OF_TELEPORT_CONTROL = 20288;
    static final long ADENA = 40308;
    static final long DEATH_SAFE_EXPERIENCE = 350351;
    static final long DEATH_SAFE_ITEM = 350352;
    static final long DEATH_SAFE_SKILL = 350353;
    static final long DEATH_SAFE_ALL = 350354;
    static final long HOTEL_KEY = ClassicWorldHotelCatalog.KEY_ITEM_ID;
    static final long WATER_DRAGON_PASS = ClassicWorldWaterDragonCatalog.PASS_ITEM_ID;
    static final long MAGIC_GEM = 40318;
    static final long ROI_POUCH = 41003;
    static final long CKEW_SECOND_FLOOR_KEY = ClassicWorldCkewCatalog.SECOND_FLOOR_KEY;
    static final long CKEW_THIRD_FLOOR_KEY = ClassicWorldCkewCatalog.THIRD_FLOOR_KEY;
    static final long CKEW_WILL_OF_DESTRUCTION =
            ClassicWorldCkewCatalog.WILL_OF_DESTRUCTION;
    static final long CKEW_MAGIC_FLUTE = ClassicWorldCkewCatalog.MAGIC_FLUTE;
    static final long CKEW_DESTRUCTION_MEDICINE =
            ClassicWorldCkewCatalog.DESTRUCTION_POTION;
    static final long CKEW_ALTAR_PROOF = ClassicWorldCkewCatalog.ALTAR_PROOF;
    static final long CKEW_CROWN_REWARD =
            ClassicWorldCkewCompletionCatalog.CROWN_REWARD;
    static final long CKEW_KNIGHT_REWARD =
            ClassicWorldCkewCompletionCatalog.KNIGHT_REWARD;
    static final long CKEW_ELF_SWORD_REWARD =
            ClassicWorldCkewCompletionCatalog.ELF_SWORD_REWARD;
    static final long CKEW_ELF_BOW_REWARD =
            ClassicWorldCkewCompletionCatalog.ELF_BOW_REWARD;
    static final long CKEW_WIZARD_REWARD =
            ClassicWorldCkewCompletionCatalog.WIZARD_REWARD;
    static final long PRECISE_TARGET_SPELLBOOK = 40226;
    static final long LIFE_SCROLL = 40564;
    /** Native beginner row 17 / ActionHtml entry used by the pc_action helper UI. */
    static final long PC_ACTION_HELPER = 300026;
    static final long SEARCH_DOCUMENT = 40565;
    static final long YANGGUO_CLAW = 40590;
    static final long LETTER_OF_GRATITUDE = 40529;
    static final long SECRET_ROOM_KEY = 40555;
    static final long ENDIA_POUCH = 40582;
    static final long ENDIA_LETTER = 40583;
    static final long SEARCHER_MISSING_PAGE = 40593;
    static final long SEALED_INTELLIGENCE_BOOK =
            ClassicWorldDspyCatalog.REWARD_ITEM_ID;
    static final long POTION_OF_REVIVAL = 40607;
    static final long RED_CLOAK = 20065;
    static final long SPIRIT_CRYSTAL = 40319;
    static final long ATTRIBUTE_STONE = 49156;
    static final long AWAKENING_BONE_FRAGMENT = 49157;
    static final long FIRST_LEVEL_DARK_STONE = 40320;
    static final long SECOND_LEVEL_DARK_STONE = 40321;
    static final long THIRD_LEVEL_DARK_STONE = 40322;
    static final long FOURTH_LEVEL_DARK_STONE = 40323;
    static final long FIFTH_LEVEL_DARK_STONE = 40324;
    static final long LIQUOR = 40858;
    static final long BLESSED_BLUE_POTION = 140015;
    static final long PURE_MITHRIL = 40494;
    static final long MITHRIL_THREAD = 40495;
    static final long MUSHROOM_JUICE = 40499;
    static final long RYUKEN_WEB = 40503;
    static final long RYUKEN_SKIN = 40504;
    static final long ENT_BARK = 40505;
    static final long ENT_FRUIT = 40506;
    static final long ENT_BRANCH = 40507;
    static final long ORIHARUKON = 40508;
    static final long PAN_MANE = 40519;
    static final long ELVEN_DUST = 40520;
    static final long ELVEN_WING = 40521;
    static final int SELF_HASTE_DURATION_SECONDS = 300;
    static final int SELF_HASTE_EFFECT_GFX = 191;
    static final int BRAVE_DURATION_SECONDS = 300;
    static final int BRAVE_EFFECT_GFX = 751;
    static final int WISDOM_POTION_DURATION_SECONDS = 300;
    static final int WISDOM_POTION_EFFECT_GFX = 750;
    static final int WISDOM_POTION_SPELL_POWER = 2;
    static final int ELVEN_WAFER_DURATION_SECONDS = 480;
    static final int ELVEN_WAFER_EFFECT_GFX = 751;
    static final int BLUE_POTION_DURATION_SECONDS = 600;
    static final int BLESSED_BLUE_POTION_DURATION_SECONDS = 700;
    static final int BLUE_POTION_EFFECT_GFX = 190;
    static final int CURE_POISON_EFFECT_GFX = 192;
    static final int BLINDNESS_DURATION_SECONDS = 16;
    static final int BLESS_OF_EVA_DURATION_SECONDS = 1_800;
    static final int UNDERWATER_BREATH_MAX_SECONDS = 3_600;
    static final int BLESS_OF_EVA_EFFECT_GFX = 190;
    static final int WHISTLE_SOUND_ID = 437;
    static final int LETTER_DELIVERY_EFFECT_GFX = 1091;
    static final int LETTER_CLOSED_INVENTORY_GFX = 465;
    static final int LETTER_OPEN_INVENTORY_GFX = 466;

    enum Effect {
        NONE, HEAL, HASTE, BRAVE, WISDOM, ELVEN_BRAVE, BLUE_MPR, ANTIDOTE, BLIND,
        BREATH, POLYMORPH, WHISTLE, WHETSTONE, LIGHTNING_WAND,
        LIQUOR, DRAGON_EYE, EXPERIENCE_GEM, COOKING, APPRAISAL, CURSE_REMOVAL,
        MAP, SHIP_TICKET, HOME_TELEPORT, CUSTOM_AREA_TELEPORT,
        PET_COLLAR, MAGIC_DOLL,
        LETTER_PAPER, LETTER_CLOSED, LETTER_OPEN, COOKING_BOOK, COOKING_FUEL,
        ROI_POUCH, ENDIA_POUCH, HERO_PUMPKIN_BAG,
        CKEW_STAGE_TELEPORT, CKEW_MAGIC_FLUTE,
        CKEW_DESTRUCTION_MEDICINE
    }

    /**
     * Native status-byte metadata for the small subset of exact classic
     * weapons whose source rows are pinned in this project.  This is kept
     * separate from the human-readable catalog detail: LiTo renders these
     * tagged bytes in the item-detail pane.
     */
    record NativeWeaponStatus(int damageSmall, int damageLarge, int material,
                              int unitWeight, int classMask, int safeEnchant,
                              int hitModifier, int damageModifier) {
        NativeWeaponStatus(int damageSmall, int damageLarge, int material,
                           int unitWeight, int classMask, int safeEnchant) {
            this(damageSmall, damageLarge, material, unitWeight, classMask,
                    safeEnchant, 0, 0);
        }

        NativeWeaponStatus {
            if (damageSmall < 0 || damageSmall > 0xFF
                    || damageLarge < 0 || damageLarge > 0xFF
                    || material < 0 || material > 0xFF
                    || unitWeight < 0 || classMask < 0 || classMask > 0x7F
                    || safeEnchant < -1 || safeEnchant > 0x7F
                    || hitModifier < -128 || hitModifier > 127
                    || damageModifier < -128 || damageModifier > 127) {
                throw new IllegalArgumentException("invalid native weapon status metadata");
            }
        }

        long packetWeight(long count) {
            if (count <= 0) throw new IllegalArgumentException("native item count must be positive");
            if (unitWeight == 0) return 0;
            return Math.max(1L, Math.min(0xFFFF_FFFFL,
                    Math.multiplyExact(count, (long) unitWeight) / 1_000L));
        }
    }

    /** Exact retained etcitem_arrow row used by a physical bow/gauntlet. */
    record NativeProjectileStatus(
            int damageSmall, int damageLarge, int material,
            int arrowGfx, boolean consumeOnAttack) {
        NativeProjectileStatus {
            if (damageSmall < 0 || damageLarge < 0
                    || material < 0 || material > 22
                    || arrowGfx < -1 || arrowGfx > 0xFFFF) {
                throw new IllegalArgumentException(
                        "invalid native projectile status metadata");
            }
        }
    }

    /** Direct retained armor-template fields rendered by LiTo's item pane. */
    record NativeArmorStatus(
            int displayArmorClass, int material, int unitWeight, int classMask,
            int safeEnchant, int strength, int dexterity, int constitution,
            int wisdom, int intelligence, int charisma, int hitPoints,
            int manaPoints, int magicResistance, int spellPower, boolean haste,
            int fireResistance, int waterResistance, int windResistance,
            int earthResistance, int blindResistance, int sleepResistance,
            int freezeResistance, int stoneResistance, int sustainResistance,
            int stunResistance, int armorType) {
        // sub_4438F0 owns only twenty line-end slots at item+0x1c.  Keep four
        // slots available for the retained client-side itemdesc/material rows;
        // ddraw's verified LiTo hook is the final guard for unusually long
        // resource text.  This also prevents a stock client from receiving a
        // status block which cannot fit its native item pane.
        private static final int MAX_NATIVE_STATUS_ROWS = 16;

        NativeArmorStatus {
            if (displayArmorClass < 0 || displayArmorClass > 0xFF
                    || material < 0 || material > 0xFF || unitWeight < 0
                    || classMask < 0 || classMask > 0x7F
                    || safeEnchant < -1 || safeEnchant > 0x7F
                    || armorType < 0 || armorType > 25) {
                throw new IllegalArgumentException("invalid native armor status metadata");
            }
        }

        byte[] detail(int useType, int quantity, int enchantLevel, int durability) {
            if (quantity <= 0 || enchantLevel < -128 || enchantLevel > 127
                    || durability < 0 || durability > 0x7F) {
                throw new IllegalArgumentException("invalid native armor instance status");
            }
            long weight = unitWeight == 0 ? 0 : Math.max(1L,
                    Math.min(0xFFFF_FFFFL,
                            Math.multiplyExact(quantity, (long) unitWeight) / 1_000L));
            Protocol.Writer out = new Protocol.Writer()
                    .c(0x13).c(displayArmorClass).c(material).d(weight);
            // Tag 0x13 renders one armor row and its material renders one row
            // after decoding has completed.
            int rows = 2;
            // Retained L1ItemStatus.armor emits instance enchant/durability and
            // the safe-enchant line. accessories/accessories2 deliberately do
            // not, even though getViewName still prefixes their enchant level.
            boolean bodyArmor = switch (useType) {
                case 2, 18, 19, 20, 21, 22, 25 -> true;
                default -> false;
            };
            if (bodyArmor && enchantLevel != 0 && rows < MAX_NATIVE_STATUS_ROWS) {
                out.c(0x02).c(enchantLevel);
                rows++;
            }
            if (bodyArmor && durability != 0 && rows < MAX_NATIVE_STATUS_ROWS) {
                out.c(0x03).c(durability);
                rows++;
            }
            if (rows < MAX_NATIVE_STATUS_ROWS) {
                out.c(0x07).c(classMask);
                rows++;
            }
            // This LiTo image's sub_4A2090 accepts only status tags 1..33.
            // Later-server custom-string tag 0x27 makes it return before it
            // commits any decoded text, which is why sufficiently enhanced
            // source items produced an entirely blank description.  Keep the
            // safe-enchant value authoritative in metadata, but never put the
            // unsupported tag on this client's wire.
            rows = taggedByte(out, rows, 0x08, strength);
            rows = taggedByte(out, rows, 0x09, dexterity);
            rows = taggedByte(out, rows, 0x0A, constitution);
            rows = taggedByte(out, rows, 0x0B, wisdom);
            rows = taggedByte(out, rows, 0x0C, intelligence);
            rows = taggedByte(out, rows, 0x0D, charisma);
            if (hitPoints != 0 && rows < MAX_NATIVE_STATUS_ROWS) {
                out.c(0x0E).h(Math.max(Short.MIN_VALUE,
                        Math.min(Short.MAX_VALUE, hitPoints)));
                rows++;
            }
            if (manaPoints != 0 && rows < MAX_NATIVE_STATUS_ROWS) {
                out.c(0x20).c(NativeCharacterStats.signedByte(manaPoints));
                rows++;
            }
            if (magicResistance != 0 && rows < MAX_NATIVE_STATUS_ROWS) {
                out.c(0x0F).h(Math.max(Short.MIN_VALUE,
                        Math.min(Short.MAX_VALUE, magicResistance)));
                rows++;
            }
            rows = taggedByte(out, rows, 0x11, spellPower);
            if (haste && rows < MAX_NATIVE_STATUS_ROWS) {
                out.c(0x12);
                rows++;
            }
            rows = taggedByte(out, rows, 0x1B, fireResistance);
            rows = taggedByte(out, rows, 0x1C, waterResistance);
            rows = taggedByte(out, rows, 0x1D, windResistance);
            rows = taggedByte(out, rows, 0x1E, earthResistance);
            boolean separatedResistance = false;
            int[] resistance = {
                    freezeResistance, stoneResistance, sleepResistance,
                    blindResistance, stunResistance, sustainResistance};
            for (int index = 0; index < resistance.length; index++) {
                int value = resistance[index];
                if (value == 0 || rows >= MAX_NATIVE_STATUS_ROWS) continue;
                if (magicResistance != 0 && !separatedResistance) {
                    out.c(0x21).c(0xD6);
                    separatedResistance = true;
                }
                out.c(0x0F).h(Math.max(Short.MIN_VALUE,
                        Math.min(Short.MAX_VALUE, value))).c(0x21).c(index + 1);
                rows++;
            }
            return out.bytes();
        }

        private static int taggedByte(
                Protocol.Writer out, int rows, int tag, int value) {
            // Item-detail tags are still native byte fields.  The character
            // total is published through the negotiated dword status packets,
            // while this tooltip keeps a safe signed-byte shadow.  Never let a
            // wide database value wrap modulo 256 and display the opposite sign.
            if (value != 0 && rows < MAX_NATIVE_STATUS_ROWS) {
                out.c(tag).c(NativeCharacterStats.signedByte(value));
                return rows + 1;
            }
            return rows;
        }
    }

    record Definition(long catalogId, int useType, int icon, int groundGfx, int blessing,
                      boolean identified, String name, String detail,
                      long buyPrice, long sellPrice, boolean stackable, boolean tradable,
                      int weaponWalkAction, int lightRange, Effect effect,
                      int itemDescriptionId, int unitWeight, int fireResistance,
                      NativeWeaponStatus sourceWeaponStatus,
                      NativeArmorStatus sourceArmorStatus,
                      boolean cantDelete) {
        Definition(long catalogId, int useType, int icon, int groundGfx, int blessing,
                   boolean identified, String name, String detail,
                   long buyPrice, long sellPrice, boolean stackable, boolean tradable,
                   int weaponWalkAction, int lightRange, Effect effect,
                   int itemDescriptionId, int unitWeight, int fireResistance,
                   NativeWeaponStatus sourceWeaponStatus,
                   NativeArmorStatus sourceArmorStatus) {
            this(catalogId, useType, icon, groundGfx, blessing, identified, name, detail,
                    buyPrice, sellPrice, stackable, tradable, weaponWalkAction, lightRange,
                    effect, itemDescriptionId, unitWeight, fireResistance,
                    sourceWeaponStatus, sourceArmorStatus, false);
        }

        Definition(long catalogId, int useType, int icon, int groundGfx, int blessing,
                   boolean identified, String name, String detail,
                   long buyPrice, long sellPrice, boolean stackable, boolean tradable,
                   int weaponWalkAction, int lightRange, Effect effect,
                   int itemDescriptionId, int unitWeight, int fireResistance,
                   NativeWeaponStatus sourceWeaponStatus) {
            this(catalogId, useType, icon, groundGfx, blessing, identified, name, detail,
                    buyPrice, sellPrice, stackable, tradable, weaponWalkAction, lightRange,
                    effect, itemDescriptionId, unitWeight, fireResistance,
                    sourceWeaponStatus, null);
        }

        Definition(long catalogId, int useType, int icon, int groundGfx, int blessing,
                   boolean identified, String name, String detail,
                   long buyPrice, long sellPrice, boolean stackable, boolean tradable,
                   int weaponWalkAction, int lightRange, Effect effect,
                   int itemDescriptionId, int unitWeight, int fireResistance) {
            this(catalogId, useType, icon, groundGfx, blessing, identified, name, detail,
                    buyPrice, sellPrice, stackable, tradable, weaponWalkAction, lightRange,
                    effect, itemDescriptionId, unitWeight, fireResistance, null, null);
        }

        Definition(long catalogId, int useType, int icon, int groundGfx, int blessing,
                   boolean identified, String name, String detail,
                   long buyPrice, long sellPrice, boolean stackable, boolean tradable,
                   int weaponWalkAction, int lightRange, Effect effect) {
            this(catalogId, useType, icon, groundGfx, blessing, identified, name, detail,
                    buyPrice, sellPrice, stackable, tradable, weaponWalkAction, lightRange,
                    effect, 0, 0, 0);
        }

        Definition(long catalogId, int useType, int icon, int groundGfx, int blessing,
                   boolean identified, String name, String detail,
                   long buyPrice, long sellPrice, boolean stackable, boolean tradable,
                   int weaponWalkAction, int lightRange, Effect effect,
                   int itemDescriptionId, int unitWeight) {
            this(catalogId, useType, icon, groundGfx, blessing, identified, name, detail,
                    buyPrice, sellPrice, stackable, tradable, weaponWalkAction, lightRange,
                    effect, itemDescriptionId, unitWeight, 0);
        }

        Definition {
            if (catalogId <= 0 || catalogId > 0xFFFF_FFFFL) {
                throw new IllegalArgumentException("catalog id is outside native dword range");
            }
            if (useType < 0 || useType > 0xFF || icon < 0 || icon > 0xFFFF
                    || groundGfx < 0 || groundGfx > 0xFFFF
                    || blessing < 0 || blessing >= 4
                    || lightRange < 0 || lightRange > 0xFF
                    || itemDescriptionId < 0 || itemDescriptionId > 0xFFFF
                    || unitWeight < 0 || fireResistance < -128 || fireResistance > 127) {
                throw new IllegalArgumentException("invalid native inventory metadata");
            }
            name = name == null ? "" : name;
            detail = detail == null ? "" : detail;
            effect = effect == null ? Effect.NONE : effect;
            if (sourceWeaponStatus != null && weaponWalkAction == BattleActionCodes.WALK) {
                throw new IllegalArgumentException(
                        "ordinary inventory item carries source weapon status");
            }
            // A record compact constructor runs before the component fields
            // are assigned, so calling armor() here would observe useType=0.
            if (sourceArmorStatus != null && !armorUseType(useType)) {
                throw new IllegalArgumentException(
                        "ordinary inventory item carries source armor status");
            }
            if (sourceArmorStatus != null
                    && !ClassicWorldDropCatalog.armorTypeSupportsUseType(
                            sourceArmorStatus.armorType(), useType)) {
                throw new IllegalArgumentException(
                        "source armor type and native use type differ");
            }
            if (sourceWeaponStatus != null && sourceArmorStatus != null) {
                throw new IllegalArgumentException("item cannot be both source weapon and armor");
            }
        }

        /** Preserve the exact source template while attaching its retained executor. */
        Definition withEffect(Effect sourceEffect) {
            return new Definition(catalogId, useType, icon, groundGfx, blessing,
                    identified, name, detail, buyPrice, sellPrice, stackable, tradable,
                    weaponWalkAction, lightRange, sourceEffect, itemDescriptionId,
                    unitWeight, fireResistance, sourceWeaponStatus, sourceArmorStatus,
                    cantDelete);
        }

        boolean weapon() {
            return weaponWalkAction != BattleActionCodes.WALK;
        }

        NativeWeaponStatus effectiveWeaponStatus() {
            if (sourceWeaponStatus != null) return sourceWeaponStatus;
            return InventoryCatalog.nativeWeaponStatus(catalogId);
        }

        boolean light() {
            return lightRange > 0;
        }

        /** Exact same-family S_Ability selector, or -1 for ordinary items. */
        int controlAbilityType() {
            if (catalogId == RING_OF_TELEPORT_CONTROL) return 1;
            if (catalogId == RING_OF_POLYMORPH_CONTROL) return 2;
            if (catalogId == RING_OF_SUMMON_CONTROL) return 5;
            return -1;
        }

        boolean controlRing() {
            return controlAbilityType() >= 0;
        }

        boolean ring() {
            return useType == 23;
        }

        boolean armor() {
            return armorUseType(useType);
        }

        private static boolean armorUseType(int useType) {
            return switch (useType) {
                case 2, 18, 19, 20, 21, 22, 23, 24, 25, 37, 40,
                        43, 44, 45, 47, 48, 49, 50, 51, 52, 53, 54, 70 -> true;
                default -> false;
            };
        }

        boolean sourceArmor() {
            return sourceArmorStatus != null;
        }

        /** Retained signed use types -2/-3 serialized as native bytes FE/FD. */
        boolean projectile() {
            return useType == 0xFE || useType == 0xFD;
        }

        /** Exact retained {@code etcitem.max_charge_count}; only 40007 is retained here. */
        int maximumChargeCount() {
            return catalogId == LIGHTNING_WAND ? 6 : 0;
        }

        int initialChargeCount() {
            return maximumChargeCount();
        }

        long packetWeight(long count) {
            if (count <= 0) throw new IllegalArgumentException("native item count must be positive");
            if (unitWeight == 0) return 0;
            return Math.max(1L, Math.min(0x7FFF_FFFFL,
                    Math.multiplyExact(count, (long) unitWeight) / 1_000L));
        }
    }

    private static final Map<Long, Definition> ITEMS;
    private static final List<Definition> BUY_ITEMS;

    static {
        Map<Long, Definition> items = new LinkedHashMap<>();
        // Exact 380_db.sql weapon 24 row and Pandora shop price.  The client
        // receives the native $100 name id, inventory gfx 106 and ground gfx
        // 26; order-list ids remain separate from this catalog id.
        register(items, new Definition(
                SHORT_SWORD, 1, 106, 26, 0, true,
                "$100", "$100",
                0, 0, false, true, BattleActionCodes.SWORD_WALK, 0, Effect.NONE,
                0, 30_000));
        // Exact 380_db etcitem 40743 + etcitem_arrow row: ordinary arrows
        // are FE-use stacks and contribute a 6/5 wood projectile die.
        register(items, new Definition(
                ARROW, 0xFE, 7, 66, 0, true,
                "$61", "$61",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.NONE,
                0, 30));
        register(items, new Definition(
                SILVER_ARROW, 0xFE, 96, 66, 0, true,
                "$93", "$93",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.NONE,
                68, 47));
        register(items, new Definition(
                MITHRIL_ARROW, 0xFE, 96, 66, 0, true,
                "$773", "$773",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.NONE,
                0, 47));
        // Exact selected 380_db.sql etcitem 46281 template.  The retained
        // database supplies no shop/drop acquisition row, so registration
        // only makes a source-owned stack representable; it grants nothing.
        // The simplified source name is preserved by the trebuchet catalog,
        // while this wire-facing label is lossless in the LiTo CP950 client.
        register(items, new Definition(
                CASTLE_TREBUCHET_SHELL, 0, 592, 3965, 0, true,
                ClassicWorldTrebuchetCatalog.CLIENT_NORMAL_SHELL_NAME,
                ClassicWorldTrebuchetCatalog.CLIENT_NORMAL_SHELL_NAME,
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.NONE,
                0, 1_000));
        // Exact selected weapon 30, the knight-30 Gunter reward: $313,
        // sword walk carrier, 8/12 iron damage, native gfx 441/3.
        register(items, new Definition(
                RED_KNIGHT_SWORD, 1, 441, 3, 0, true,
                "$313", "$313",
                0, 0, false, true, BattleActionCodes.SWORD_WALK, 0, Effect.NONE,
                123, 40_000));
        // Exact identical 380_db/380yiwei_db weapon 68 row.  Map244's
        // retained AC -50 population needs a source-scale weapon for the
        // isolated physical combat/death proof; this is also a generally
        // usable native inventory definition rather than a test-only item.
        register(items, new Definition(
                ANCIENT_SWORD, 1, 902, 26, 0, true,
                "$1950", "$1950",
                0, 0, false, true, BattleActionCodes.SWORD_WALK, 0, Effect.NONE,
                672, 30_000));
        // Exact classic weapon 88 row. Pan's horn is also a circle-two/three
        // spell-learning reagent, so it must retain its non-stackable spear
        // inventory identity while material transactions count instances.
        register(items, new Definition(
                PAN_HORN, 1, 317, 887, 0, true,
                "潘的角", "$783",
                0, 0, false, true, BattleActionCodes.SPEAR_WALK, 0, Effect.NONE,
                0, 12_000));
        register(items, new Definition(
                TRAINING_SWORD, 1, 24, 68, 1, true,
                "Training Sword", "A development-arena weapon",
                1_000, 500, false, true, BattleActionCodes.SWORD_WALK, 0, Effect.NONE));
        register(items, new Definition(
                HEALING_POTION, 0, 27, 45, 1, true,
                "Healing Potion", "Restores battle hit points",
                500, 250, true, true, BattleActionCodes.WALK, 0, Effect.HEAL));
        // Existing 0.7.x state files contain catalog 2001. Keep it readable and
        // sellable while assigning it a real native inventory record.
        register(items, new Definition(
                ARENA_TOKEN, 0, 27, 7, 1, true,
                "Arena Token", "Legacy Tournament reward token",
                0, 250, true, true, BattleActionCodes.WALK, 0, Effect.NONE));
        // Retained quest117 Dragon Gate creates a 32-count key-family stack.
        // Its private database row is absent from the selected public dump;
        // the same-family 40312 key wire assets are 59/151 and the retained
        // executor supplies the dynamic instance number and expiry details.
        register(items, new Definition(
                WATER_DRAGON_PASS, 0, 59, 151, 0, true,
                "龍門憑證", "法利昂棲息地副本憑證",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.NONE,
                0, 1_000));
        // Exact 380_db.sql etcitem 40004 row: use type light, invgfx 140,
        // ordinary blessing. L1Item assigns light range 0x16 and zero fuel,
        // making the magic lantern an infinite on/off light source.
        register(items, new Definition(
                MAGIC_LANTERN, 10, 140, 181, 0, true,
                "魔法燈籠", "無限燃料照明 ($3426)",
                0, 0, false, true, BattleActionCodes.WALK, 0x16, Effect.NONE,
                0, 30_000));
        // Exact selected 380_db row: spell_long maps to native use type 5 and
        // max_charge_count=6. The protected client gates its cddhh callback on
        // the second inventory byte before sending the target/location.
        register(items, new Definition(
                LIGHTNING_WAND, 5, 118, 28, 0, true,
                "$262", "$262",
                0, 0, false, true, BattleActionCodes.WALK, 0, Effect.LIGHTNING_WAND,
                21, 7_000));
        // Exact 380_db.sql etcitem 40010 row and Pandora price: normal
        // inventory type, invgfx 80, ground gfx 45 and native $26 name id.
        register(items, new Definition(
                NATIVE_HEALING_POTION, 0, 80, 45, 0, true,
                "$26", "$26",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.HEAL,
                1342, 3_900));
        register(items, new Definition(
                EXTRA_HEALING_POTION, 0, 78, 43, 0, true,
                "$235", "$235",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.HEAL,
                15, 4_900));
        register(items, new Definition(
                GREATER_HEALING_POTION, 0, 81, 46, 0, true,
                "$238", "$238",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.HEAL,
                130, 5_850));
        // Exact 380_db.sql etcitem 40013 row: use type power, invgfx 77,
        // ordinary blessing, 300-second StatusHaste and object gfx 191.
        register(items, new Definition(
                SELF_HASTE_POTION, 0, 77, 42, 0, true,
                "自我加速藥水", "300 秒加速效果 ($234)",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.HASTE,
                17, 3_900));
        // Exact 380_db.sql etcitem 40014 row: power maps to native use type 0,
        // invgfx 79, ordinary blessing, 300-second brave mode 1 and gfx 751.
        register(items, new Definition(
                BRAVE_POTION, 0, 79, 44, 0, true,
                "勇敢藥水", "300 秒勇敢速度 ($943)",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.BRAVE,
                296, 4_500));
        // Exact 380_db.sql etcitem 40015 row. Source use type "mpr" is
        // signed -6 and therefore travels in LiTo's inventory byte as 0xFA.
        // sub_445C10 sign-extends it and uses the default sub_445BD0 path,
        // which produces C_USE_ITEM cds(objectId, "").
        register(items, new Definition(
                BLUE_POTION, 0xFA, 75, 20, 0, true,
                "$232", "$232",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.BLUE_MPR,
                0, 7_800));
        // Exact retained StatusWisdomPotion: wizard-only limit_class 8,
        // 300-second SP+2 lease and object effect 750.
        register(items, new Definition(
                WISDOM_POTION, 0, 1529, 3836, 0, true,
                "$944", "$944",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.WISDOM,
                297, 4_500));
        // Blessed source variant: identical resource with a 700-second lease.
        register(items, new Definition(
                BLESSED_BLUE_POTION, 0xFA, 75, 20, 1, true,
                "$232", "$232",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.BLUE_MPR,
                0, 7_800));
        // Exact 380_db.sql etcitem 40017 row: normal potion, invgfx 76,
        // ordinary blessing. Disintoxicat_Potion emits gfx 192, consumes one
        // item and invokes the character poison cure path.
        register(items, new Definition(
                CURE_POISON_POTION, 0, 76, 41, 0, true,
                "解毒藥水", "解除中毒效果 ($233)",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.ANTIDOTE,
                126, 3_900));
        // Exact retained StatusElfBrave: elf-only limit_class 4, 480-second
        // brave mode 3 and object effect 751.
        register(items, new Definition(
                ELVEN_WAFER, 0, 130, 81, 0, true,
                "$110", "$110",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.ELVEN_BRAVE,
                86, 5_000));
        // Exact 380_db.sql etcitem 40025 row: normal potion, invgfx 82,
        // ordinary blessing. Blindness_Potion applies CURSE_BLIND for 16 s.
        register(items, new Definition(
                BLINDNESS_POTION, 0, 82, 47, 0, true,
                "失明藥水", "16 秒失明效果 ($239)",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.BLIND,
                16, 15_600));
        // Exact 380_db.sql etcitem 40032 row: normal-use potion, invgfx 557,
        // ground gfx 1733, item description 411 and unit weight 2000.
        // Water_Essence adds 1800 seconds of underwater breath, capped at
        // 3600, emits effect 190 and consumes one item.
        register(items, new Definition(
                BLESS_OF_EVA, 0, 557, 1733, 0, true,
                "$1507", "$1507",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.BREATH,
                411, 2000));
        // Exact retained 380_db.sql rows 49549..49551. Their use_type is
        // "normal" (native zero); the adjacent numeric 49 is the minimum
        // character level, not a client item-use selector. LiTo therefore
        // follows sub_445C10's default C_USE_ITEM cds(objectId, "") path.
        for (BattleExperienceGemCatalog.Gem gem : BattleExperienceGemCatalog.all()) {
            register(items, new Definition(
                    gem.itemId(), 0, gem.inventoryGfx(), gem.groundGfx(), 0, true,
                    gem.nameId(), gem.nameId(),
                    0, 0, true, true, BattleActionCodes.WALK, 0,
                    Effect.EXPERIENCE_GEM, 0, 0));
        }
        // Exact retained etcitem 42518..42524 plus both etcitem_buff tables:
        // seven reusable/non-tradable Dragon Eyes. GenericBuffItem applies one
        // 600-second, non-overwriting DRAGON_EYE lease without consuming it.
        for (BattleDragonEyeCatalog.Eye eye : BattleDragonEyeCatalog.all()) {
            register(items, new Definition(
                    eye.itemId(), eye.useType(), eye.inventoryGfx(),
                    eye.groundGfx(), 0, true,
                    eye.nameId(), eye.nameId(),
                    0, 0, true, false, BattleActionCodes.WALK, 0,
                    Effect.DRAGON_EYE, 0, 0));
        }
        // Exact 380_db.sql etcitem 40088 row: use type sosc (native 16),
        // invgfx 472, ordinary blessing and item description 1341. LiTo's
        // callback sends the selected native polymorph name in the cds tail.
        register(items, new Definition(
                POLYMORPH_SCROLL, 16, 472, 22, 0, true,
                "變形卷軸", "1800 秒變形效果 ($971)",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.POLYMORPH,
                1341, 630));
        // Exact 380_db.sql etcitem 40126 row: identify/use type 7,
        // invgfx 475, ground gfx 22, ItemDesc id 1339 and unit weight 630.
        // sub_407710 installs an inventory-row callback; sub_407760 then sends
        // C_USE_ITEM cdd(scroll object id, selected object id).
        register(items, new Definition(
                APPRAISAL_SCROLL, 7, 475, 22, 0, true,
                "$55", "$55",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.APPRAISAL,
                1339, 630));
        // Exact 380_db.sql etcitem 40119 row: normal/use type 0, inventory
        // gfx 470, ground gfx 22, ItemDesc 1340 and unit weight 630. The
        // Scroll_CurseRemoval executor changes every cursed inventory instance
        // to the ordinary client display state and consumes one scroll.
        register(items, new Definition(
                CURSE_REMOVAL_SCROLL, 0, 470, 22, 0, true,
                "$243", "$243",
                0, 0, true, true, BattleActionCodes.WALK, 0,
                Effect.CURSE_REMOVAL, 1340, 630));
        // Exact 380_db.sql armor 20304 row in both retained server trees:
        // ring/use type 23, $1663, invgfx 628, ground gfx 21, gold weight
        // 3000 and defense_fire +30. It is catalogued here without inventing
        // a merchant price; acquisition remains an independent gameplay path.
        register(items, new Definition(
                RING_OF_FIRE_RESISTANCE, 23, 628, 21, 0, true,
                "$1663", "$1663",
                0, 0, false, true, BattleActionCodes.WALK, 0,
                Effect.NONE, 0, 3000, 30));
        // Exact 380_db.sql armor rows. These are use-type 23 ring records;
        // L1EquipmentSlot maps them to native S_Ability selectors 2, 5 and 1.
        register(items, new Definition(
                RING_OF_POLYMORPH_CONTROL, 23, 1195, 3166, 0, true,
                "$261", "$261",
                0, 0, false, true, BattleActionCodes.WALK, 0,
                Effect.NONE, 65, 3000));
        register(items, new Definition(
                RING_OF_SUMMON_CONTROL, 23, 905, 21, 0, true,
                "$1937", "$1937",
                0, 0, false, true, BattleActionCodes.WALK, 0,
                Effect.NONE, 658, 3000));
        register(items, new Definition(
                RING_OF_TELEPORT_CONTROL, 23, 1194, 3165, 0, true,
                "$241", "$241",
                0, 0, false, true, BattleActionCodes.WALK, 0,
                Effect.NONE, 64, 3000));
        // Exact 380_db.sql etcitem 40315 row: instrument/use type 15,
        // invgfx 477, ordinary blessing and non-stackable. Its Whistle
        // executor broadcasts native sound 437 and does not consume the item.
        register(items, new Definition(
                WHISTLE, 15, 477, 1218, 0, true,
                "哨子", "呼喚寵物的哨音 ($1086)",
                0, 0, false, true, BattleActionCodes.WALK, 0, Effect.WHISTLE,
                343, 8_000));
        // Exact selected 380_db rows.  LiTo.bin sign-extends petcollar's
        // native use byte -4, so the unsigned wire metadata is 0xFC and its
        // click still takes the default 0x1C+d+empty-string producer.
        register(items, new Definition(
                PET_COLLAR, 0xFC, 487, 1317, 0, true,
                "$1173", "$1173",
                0, 0, false, true, BattleActionCodes.WALK, 0,
                Effect.PET_COLLAR, 361, 5_000));
        register(items, new Definition(
                HIGH_PET_COLLAR, 0xFC, 1617, 1317, 0, true,
                "$1173", "$1173",
                0, 0, false, true, BattleActionCodes.WALK, 0,
                Effect.PET_COLLAR, 0, 5_000));
        // choice maps to native use type 14. sub_407890 sends cdd(item,target);
        // retained Hone consumes one stone and recovers exactly one damage.
        register(items, new Definition(
                WHETSTONE, 14, 478, 1252, 0, true,
                "$1100", "$1100",
                0, 0, true, true, BattleActionCodes.WALK, 0,
                Effect.WHETSTONE, 348, 4_000));
        // Exact selected 380_db.sql Evolution_Fruit rows.  They are not
        // clicked to evolve a pet: the retained C_GIVE path transfers the
        // stack to the owned pet and pettypes selects the matching stage.
        register(items, new Definition(
                EVOLUTION_FRUIT, 0, 1316, 3350, 0, true,
                "$2810", "$2810",
                0, 0, true, true, BattleActionCodes.WALK, 0,
                Effect.NONE, 969, 10_000));
        register(items, new Definition(
                VICTORY_FRUIT, 0, 2639, 6376, 0, true,
                "$5307", "$5307",
                0, 0, true, true, BattleActionCodes.WALK, 0,
                Effect.NONE, 0, 1_000));
        // Exact selected 380_db etcitem rows. L1PolyRace.giftWinner creates
        // 41308 x1; its weighted contents are pinned separately in
        // BattlePetRaceRewardCatalog and are not guessed from newer regions.
        register(items, new Definition(
                SKELETON_HOLY_GRAIL, 0, 2599, 6266, 0, true,
                "$5235", "$5235",
                0, 0, true, true, BattleActionCodes.WALK, 0,
                Effect.NONE, 2777, 1_000));
        register(items, new Definition(
                HERO_PUMPKIN_BAG, 0, 943, 3963, 0, true,
                "$5287", "$5287",
                0, 0, true, false, BattleActionCodes.WALK, 0,
                Effect.HERO_PUMPKIN_BAG, 0, 14_000));
        register(items, new Definition(
                PET_CALLING_FLUTE, 0, 477, 1218, 0, true,
                "$4288", "$4288",
                0, 0, true, true, BattleActionCodes.WALK, 0,
                Effect.NONE, 2045, 0));
        // Exact 40736 source row also executes UserMprBlue 600/190.
        register(items, new Definition(
                WISDOM_COIN, 0xFA, 1482, 3683, 1, true,
                "$3058", "$3058",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.BLUE_MPR,
                0, 0));
        // Exact Fuel/41260 row. The global item-use gate requires level 60
        // before Fuel.execute creates NPC 81170 for 600 seconds.
        register(items, new Definition(
                COOKING_FUEL, 0, 2489, 5986, 0, true,
                "$4922", "$4922",
                0, 0, true, true, BattleActionCodes.WALK, 0,
                Effect.COOKING_FUEL, 2551, 780));
        // The selected LiTo sign-extends use type F8 to -8 but its item-use
        // jump table still falls through to C_USE_ITEM cds(objectId, empty).
        // These exact book resources are catalogued and preserved on use;
        // recipe selection remains closed because this target has neither the
        // old cdcc producer nor the PacketBox-52 cooking-window receiver.
        registerCookingBook(items, COOKING_BOOK_LEVEL_ONE,
                2498, 5994, "料理書：1階段", "$4923", 2552);
        registerCookingBook(items, COOKING_BOOK_LEVEL_TWO,
                2499, 5995, "料理書：2階段", "$4924", 2553);
        registerCookingBook(items, COOKING_BOOK_LEVEL_THREE,
                2500, 5996, "料理書：3階段", "$4925", 2554);

        // Exact ingredient resources used by Cooking_Book cookNo 0..23.
        // Food1 maps to native use type 38; source rows marked normal/none
        // retain the project's existing default-use byte 0.
        registerCookingIngredient(items, 40056, 38, 124, 36, "肉", "$23", 10, 10_000);
        registerCookingIngredient(items, 40057, 38, 1205, 3176,
                "漂浮之眼肉", "$6 $23", 9, 10_000);
        registerCookingIngredient(items, 40060, 38, 25, 67, "胡蘿蔔", "$68", 0, 2_000);
        registerCookingIngredient(items, 40061, 38, 63, 72, "檸檬", "$82", 0, 2_000);
        registerCookingIngredient(items, 40062, 38, 127, 80, "香蕉", "$107", 0, 2_000);
        registerCookingIngredient(items, 40064, 38, 995, 2723, "蘋果", "$106", 0, 2_000);
        registerCookingIngredient(items, 40069, 38, 71, 73, "橘子", "$85", 0, 2_000);
        registerCookingIngredient(items, 41263, 0, 949, 2475,
                "太陽花籽", "$4969", 2564, 1_000);
        registerCookingIngredient(items, 41265, 0, 2488, 5985,
                "蜂蜜", "$4918", 2564, 5_860);
        registerCookingIngredient(items, 41267, 38, 2504, 6000,
                "起士", "$4920", 2564, 0);
        registerCookingIngredient(items, 41274, 38, 2569, 6164,
                "螞蟻腿", "$5241", 2664, 1_000);
        registerCookingIngredient(items, 41275, 38, 2571, 6166,
                "熊肉", "$5242", 2664, 10_000);
        registerCookingIngredient(items, 41276, 38, 2574, 6168,
                "山豬肉", "$5243", 2664, 10_000);

        registerCookingIngredient(items, 49040, 38, 2729, 36,
                "鯊魚卵", "$5438", 2778, 10_000);
        registerCookingIngredient(items, 49041, 38, 2732, 36,
                "鱷魚肉", "$5439", 2778, 10_000);
        registerCookingIngredient(items, 49042, 38, 2833, 36,
                "龍龜蛋", "$5446", 2778, 10_000);
        registerCookingIngredient(items, 49043, 38, 2739, 36,
                "奇異鸚鵡肉", "$5440", 2778, 10_000);
        registerCookingIngredient(items, 49044, 38, 2731, 36,
                "毒蠍肉", "$5441", 2778, 10_000);
        registerCookingIngredient(items, 49045, 38, 2734, 36,
                "伊萊克頓肉", "$5442", 2778, 10_000);
        registerCookingIngredient(items, 49046, 38, 2727, 36,
                "蜘蛛腿肉", "$5443", 2778, 10_000);
        registerCookingIngredient(items, 49047, 38, 2737, 36,
                "蟹肉", "$5444", 2778, 10_000);
        registerCookingIngredient(items, 49048, 0, 2492, 5988,
                "綜合料理醬", "$4916", 2778, 10_000);

        registerCookingIngredient(items, 49243, 0, 513, 1490,
                "香菜", "$1191", 3618, 1_000);
        registerCookingIngredient(items, 49260, 0, 2941, 6550,
                "奎斯坦修的螯", "$6107", 3632, 10_000);
        registerCookingIngredient(items, 49261, 0, 2926, 6536,
                "格利芬肉", "$6108", 3634, 10_000);
        registerCookingIngredient(items, 49262, 0, 2938, 6547,
                "亞力安的尾巴", "$6109", 3634, 10_000);
        registerCookingIngredient(items, 49263, 0, 2937, 6546,
                "巨王龜肉", "$6110", 3634, 10_000);
        registerCookingIngredient(items, 49264, 0, 2931, 6541,
                "幼龍翅膀", "$6111", 3634, 10_000);
        registerCookingIngredient(items, 49265, 0, 2929, 6539,
                "飛龍肉", "$6112", 3634, 10_000);
        registerCookingIngredient(items, 49266, 0, 2935, 6544,
                "深海魚肉", "$6113", 3634, 10_000);
        registerCookingIngredient(items, 49267, 0, 2932, 6542,
                "邪惡蜥蜴蛋", "$6114", 3634, 10_000);
        // Exact 380_db.sql level-one Food3 rows. Food maps to native use type
        // 38; LiTo then follows sub_445C10's F8/default sub_445BD0 producer and
        // emits C_USE_ITEM cds(objectId, empty). The three source rows whose
        // weight is 3000 retain that difference instead of sharing a default.
        registerCooking(items, FLOATING_EYE_STEAK, 2486, 5983, "$4932", 2562, 2_000);
        registerCooking(items, ROAST_BEAR_MEAT, 2572, 6167, "$5218", 2563, 2_000);
        registerCooking(items, NUT_PANCAKE, 2495, 5991, "$4929", 2564, 2_000);
        registerCooking(items, ANT_LEG_CHEESE, 2570, 6165, "$5219", 2565, 2_000);
        registerCooking(items, FRUIT_SALAD, 2484, 5981, "$4930", 2566, 2_000);
        registerCooking(items, SWEET_SOUR_MEAT, 2485, 5982, "$4931", 2567, 2_000);
        registerCooking(items, BOAR_SKEWER, 2573, 6169, "$5220", 2568, 2_000);
        registerCooking(items, MUSHROOM_SOUP, 2575, 6170, "$5221", 2569, 2_000);
        registerCooking(items, SPECIAL_FLOATING_EYE_STEAK, 2486, 5983,
                "$4928$4932", 2781, 2_000);
        registerCooking(items, SPECIAL_ROAST_BEAR_MEAT, 2572, 6167,
                "$4928$5218", 2782, 2_000);
        registerCooking(items, SPECIAL_NUT_PANCAKE, 2495, 5991,
                "$4928$4929", 2783, 2_000);
        registerCooking(items, SPECIAL_ANT_LEG_CHEESE, 2570, 6165,
                "$4928$5219", 2784, 3_000);
        registerCooking(items, SPECIAL_FRUIT_SALAD, 2484, 5981,
                "$4928$4930", 2785, 3_000);
        registerCooking(items, SPECIAL_SWEET_SOUR_MEAT, 2485, 5982,
                "$4928$4931", 2786, 3_000);
        registerCooking(items, SPECIAL_BOAR_SKEWER, 2573, 6169,
                "$4928$5220", 2787, 2_000);
        registerCooking(items, SPECIAL_MUSHROOM_SOUP, 2575, 6170,
                "$4928$5221", 2788, 2_000);
        // Exact 380_db.sql level-two Food3 rows. All share ground gfx 6165
        // and unit weight 2000; their inventory gfx/name/description ids are
        // deliberately retained per row.
        registerCooking(items, BattleCookingCatalog.CAVIAR,
                2736, 6165, "$5430", 2953, 2_000);
        registerCooking(items, BattleCookingCatalog.ALLIGATOR_STEAK,
                2733, 6165, "$5431", 2954, 2_000);
        registerCooking(items, BattleCookingCatalog.TURTLE_DRAGON_COOKIE,
                2741, 6165, "$5432", 2955, 2_000);
        registerCooking(items, BattleCookingCatalog.KIWI_PARROT_ROAST,
                2740, 6165, "$5433", 2956, 2_000);
        registerCooking(items, SCORPION_SKEWER,
                2730, 6165, "$5434", 2957, 2_000);
        registerCooking(items, BattleCookingCatalog.ELECTON_STEW,
                2735, 6165, "$5435", 2958, 2_000);
        registerCooking(items, BattleCookingCatalog.SPIDER_LEG_SKEWER,
                2728, 6165, "$5436", 2959, 2_000);
        registerCooking(items, BattleCookingCatalog.CRAB_SOUP,
                2738, 6165, "$5437", 2960, 2_000);
        registerCooking(items, BattleCookingCatalog.SPECIAL_CAVIAR,
                2736, 6165, "$4928$5430", 2961, 2_000);
        registerCooking(items, BattleCookingCatalog.SPECIAL_ALLIGATOR_STEAK,
                2733, 6165, "$4928$5431", 2962, 2_000);
        registerCooking(items, BattleCookingCatalog.SPECIAL_TURTLE_DRAGON_COOKIE,
                2741, 6165, "$4928$5432", 2963, 2_000);
        registerCooking(items, BattleCookingCatalog.SPECIAL_KIWI_PARROT_ROAST,
                2740, 6165, "$4928$5433", 2964, 2_000);
        registerCooking(items, SPECIAL_SCORPION_SKEWER,
                2730, 6165, "$4928$5434", 2965, 2_000);
        registerCooking(items, BattleCookingCatalog.SPECIAL_ELECTON_STEW,
                2735, 6165, "$4928$5435", 2966, 2_000);
        registerCooking(items, BattleCookingCatalog.SPECIAL_SPIDER_LEG_SKEWER,
                2728, 6165, "$4928$5436", 2967, 2_000);
        registerCooking(items, BattleCookingCatalog.SPECIAL_CRAB_SOUP,
                2738, 6165, "$4928$5437", 2968, 2_000);

        // Exact level-three rows. Ground gfx differs by recipe and is shared
        // only by the corresponding normal/special pair.
        registerCooking(items, BattleCookingCatalog.QUESTANUX_CLAW,
                2940, 6549, "$6099", 3618, 2_000);
        registerCooking(items, BattleCookingCatalog.GRIFFON_ROAST,
                2925, 6535, "$6100", 3619, 2_000);
        registerCooking(items, BattleCookingCatalog.ARIAN_TAIL_STEAK,
                2939, 6548, "$6101", 3620, 2_000);
        registerCooking(items, BattleCookingCatalog.GIANT_TURTLE_ROAST,
                2927, 6537, "$6102", 3621, 2_000);
        registerCooking(items, BattleCookingCatalog.BABY_DRAGON_WING,
                2930, 6540, "$6103", 3622, 2_000);
        registerCooking(items, GRILLED_WYVERN,
                2928, 6538, "$6104", 3623, 2_000);
        registerCooking(items, BattleCookingCatalog.DEEP_SEA_FISH_STEW,
                2936, 6545, "$6105", 3624, 2_000);
        registerCooking(items, BattleCookingCatalog.EVIL_LIZARD_EGG_SOUP,
                2933, 6543, "$6106", 3625, 2_000);
        registerCooking(items, BattleCookingCatalog.SPECIAL_QUESTANUX_CLAW,
                2940, 6549, "$4928$6099", 3626, 2_000);
        registerCooking(items, BattleCookingCatalog.SPECIAL_GRIFFON_ROAST,
                2925, 6535, "$4928$6100", 3627, 2_000);
        registerCooking(items, BattleCookingCatalog.SPECIAL_ARIAN_TAIL_STEAK,
                2939, 6548, "$4928$6101", 3628, 2_000);
        registerCooking(items, BattleCookingCatalog.SPECIAL_GIANT_TURTLE_ROAST,
                2927, 6537, "$4928$6102", 3629, 2_000);
        registerCooking(items, BattleCookingCatalog.SPECIAL_BABY_DRAGON_WING,
                2930, 6540, "$4928$6103", 3630, 2_000);
        registerCooking(items, SPECIAL_GRILLED_WYVERN,
                2928, 6538, "$4928$6104", 3631, 2_000);
        registerCooking(items, BattleCookingCatalog.SPECIAL_DEEP_SEA_FISH_STEW,
                2936, 6545, "$4928$6105", 3632, 2_000);
        registerCooking(items, BattleCookingCatalog.SPECIAL_EVIL_LIZARD_EGG_SOUP,
                2933, 6543, "$4928$6106", 3633, 2_000);

        // Same-family Food3 level-four rows use literal names in the retained
        // database. Their item-description id is zero in that source.
        registerCooking(items, BattleCookingCatalog.STRENGTH_STEAK,
                4955, 4337, "強壯的牛排", 0, 1_000);
        registerCooking(items, BattleCookingCatalog.DEXTERITY_SALMON,
                4956, 4337, "敏捷的煎鮭魚", 0, 2_000);
        registerCooking(items, BattleCookingCatalog.CHARCOAL_TURKEY,
                4944, 4337, "炭烤的火雞", 0, 2_000);
        registerCooking(items, BattleCookingCatalog.HEALTH_CHICKEN_SOUP,
                4978, 4337, "養生的雞湯", 0, 2_000);
        // Exact 380_db.sql normal letter family. sub_479680 emits cdhsSS for
        // use type 12; the two card forms use type 13 and preserve one
        // content-bearing instance each. The source rows are tradeable, but
        // content transfer is kept closed here until every server transfer
        // path can retain the instance binding instead of allocating a new id.
        register(items, new Definition(
                LETTER_PAPER, 12, 464, 1215, 0, true,
                "$1075", "$1075",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.LETTER_PAPER));
        register(items, new Definition(
                LETTER_CLOSED, 13, LETTER_CLOSED_INVENTORY_GFX, 1215, 0, true,
                "$1075", "$1075",
                0, 0, false, false, BattleActionCodes.WALK, 0, Effect.LETTER_CLOSED));
        register(items, new Definition(
                LETTER_OPEN, 13, LETTER_OPEN_INVENTORY_GFX, 1215, 0, true,
                "$1075", "$1075",
                0, 0, false, false, BattleActionCodes.WALK, 0, Effect.LETTER_OPEN));
        // The exact binary's four ship animations first use the matching
        // non-usable ticket with cdc(marker=1), then send C_GOTO_MAP chhh.
        registerShipTicket(items, SHIP_TICKET_TO_TALKING_ISLAND, "$315");
        registerShipTicket(items, SHIP_TICKET_TO_GLUDIO, "$314");
        registerShipTicket(items, SHIP_TICKET_FORGOTTEN_ISLAND, "$1930");
        registerShipTicket(items, SHIP_TICKET_HEINE, "$1945");
        // Exact selected 380_db row sold by Duran: non-usable, ordinary,
        // non-stackable paper ticket with invgfx138/ground151 and no itemdesc.
        register(items, new Definition(
                SHIP_TICKET_PIRATE_ISLAND, 0xFF, 138, 151, 0, true,
                "$3439", "", 0, 0, false, true,
                BattleActionCodes.WALK, 0, Effect.SHIP_TICKET, 0, 1_000));
        // Exact retained 380_db/L1JGO row sold by Roxanne on map440.
        register(items, new Definition(
                SHIP_TICKET_HIDDEN_DOCK, 0xFF, 138, 151, 0, true,
                "$3440", "", 0, 0, false, true,
                BattleActionCodes.WALK, 0, Effect.SHIP_TICKET, 0, 1_000));
        // MapR items are non-consumed normal/use-type-0 paper items. Their
        // catalog id selects one exact 1..18 S_SHOW_MAP page.
        for (NativeMapItemCatalog.Item map : NativeMapItemCatalog.all()) {
            register(items, new Definition(
                    map.catalogId(), 0, map.icon(), map.groundGfx(), 0, true,
                    map.nameId(), map.nameId(),
                    0, 0, false, true, BattleActionCodes.WALK, 0, Effect.MAP,
                    0, 1_000));
        }
        // Exact classic etcitem 40308 row: inventory gfx 5, ground gfx 7,
        // ordinary blessing and a stackable/tradable currency item. Standard
        // C_CREATE_PLEDGE consumes 30,000 of this item; Tournament points are
        // intentionally a separate economy and must not stand in for Adena.
        register(items, new Definition(
                ADENA, 0, 5, 7, 0, true,
                "Adena", "$4",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.NONE));
        // Exact selected 380_db etcitem rows executed by add.DeathSafe.  They
        // are ordinary, tradable, stackable quest carriers; death processing,
        // rather than C_USE_ITEM, reads their category mask and consume flag.
        register(items, new Definition(
                DEATH_SAFE_EXPERIENCE, 0xFF, 5576, 22, 0, true,
                "經驗保護符", "經驗保護符",
                0, 0, true, true, BattleActionCodes.WALK, 0,
                Effect.NONE, 0, 630));
        register(items, new Definition(
                DEATH_SAFE_ITEM, 0xFF, 5563, 22, 0, true,
                "道具保護符", "道具保護符",
                0, 0, true, true, BattleActionCodes.WALK, 0,
                Effect.NONE, 0, 630));
        register(items, new Definition(
                DEATH_SAFE_SKILL, 0xFF, 5567, 22, 0, true,
                "技能保護符", "技能保護符",
                0, 0, true, true, BattleActionCodes.WALK, 0,
                Effect.NONE, 0, 630));
        register(items, new Definition(
                DEATH_SAFE_ALL, 0xFF, 5565, 22, 0, true,
                "全能保護符(7日)", "全能保護符(7日)",
                0, 0, true, true, BattleActionCodes.WALK, 0,
                Effect.NONE, 0, 630));
        // Exact selected 380_db etcitem 40312 row.  The retained inn system
        // attaches lease/showId metadata to each runtime object separately;
        // the client-facing catalog row remains an ordinary, tradable,
        // non-stackable iron key with native name id $954.
        register(items, new Definition(
                HOTEL_KEY, 0xFF, 59, 151, 0, true,
                "$954", "$954",
                0, 0, false, true, BattleActionCodes.WALK, 0,
                Effect.NONE, 0, 1_000));
        // Exact 380_db/380yiwei_db etcitem 40079 row. LiTo use type 9
        // emits cdhhh(object,current-map,0,0), while retained GoHome_Reel
        // consumes one only after the map escapability check succeeds.
        register(items, new Definition(
                HOME_SCROLL, 9, 474, 22, 0, true,
                "$505", "$505",
                0, 0, true, true, BattleActionCodes.WALK, 0,
                Effect.HOME_TELEPORT, 1338, 630));
        // Exact 380_db.sql etcitem 40858 row: normal potion/use type 0,
        // invgfx 82, ordinary blessing and stackable. Liquor consumes one,
        // sets the persistent drink flag and sends local S_DRUNKEN mode 1.
        register(items, new Definition(
                LIQUOR, 0, 82, 47, 0, true,
                "酒", "醉酒效果 ($10099)",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.LIQUOR,
                0, 8_000));
        // Exact 380_db.sql etcitem row used by Life Stream's itemConsumeId.
        register(items, new Definition(
                MAGIC_GEM, 0, 235, 773, 0, true,
                "Magic Gem", "Spell reagent ($508)",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.NONE,
                0, 2_000));
        // Exact selected Gunter quest resources. "book" is native signed -9
        // (0xF7); "none" is signed -1 (0xFF). Quest carriers are retained as
        // non-tradable objects while the crown spellbook remains stackable.
        register(items, new Definition(
                PRECISE_TARGET_SPELLBOOK, 0xF7, 252, 776, 0, true,
                "$1959", "$1959",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.NONE,
                0, 7_000));
        register(items, new Definition(
                LIFE_SCROLL, 0xFF, 912, 22, 0, true,
                "$1961", "$1961",
                0, 0, false, false, BattleActionCodes.WALK, 0, Effect.NONE,
                0, 1_000));
        register(items, new Definition(
                SEARCH_DOCUMENT, 0xFF, 914, 22, 0, true,
                "$1908", "$1908",
                0, 0, false, false, BattleActionCodes.WALK, 0, Effect.NONE,
                0, 1_000));
        // Exact selected map-4 quest rewards. Endia's 40582 treasure-box row
        // is the sole etcitem_boxs producer of 40583; Searcher's 40593 remains
        // a non-usable paper quest carrier for the later Masha exchange.
        register(items, new Definition(
                ENDIA_POUCH, 0, 1321, 3374, 0, true,
                "$2653", "$2653",
                0, 0, false, false, BattleActionCodes.WALK, 0,
                Effect.ENDIA_POUCH, 1534, 1_000));
        register(items, new Definition(
                ENDIA_LETTER, 0xFF, 1322, 3375, 0, true,
                "$2654", "$2654",
                0, 0, false, false, BattleActionCodes.WALK, 0, Effect.NONE,
                0, 1_000));
        register(items, new Definition(
                SEARCHER_MISSING_PAGE, 0xFF, 1800, 3963, 0, true,
                "$2248", "$2248",
                0, 0, false, false, BattleActionCodes.WALK, 0, Effect.NONE,
                0, 1_000));
        // Exact selected ElfLv50_1 Dspy arrival reward.  The etcitem row is a
        // weightless, non-usable paper quest carrier with itemdesc_id 127.
        register(items, new Definition(
                SEALED_INTELLIGENCE_BOOK, 0xFF, 912, 22, 0, true,
                "$5583", "$5583",
                0, 0, false, false, BattleActionCodes.WALK, 0, Effect.NONE,
                127, 0));
        register(items, new Definition(
                YANGGUO_CLAW, 0xFF, 139, 3963, 0, true,
                "$2023", "$2023",
                0, 0, false, false, BattleActionCodes.WALK, 0, Effect.NONE,
                0, 1_000));
        // Exact selected knight-30 Jim quest carriers. The live selected
        // CustomAreaTeleport row intentionally owns 40555 rather than the
        // older class-bound SecretRoom_Key executor.
        register(items, new Definition(
                LETTER_OF_GRATITUDE, 0xFF, 917, 3963, 0, true,
                "$2026", "$2026",
                0, 0, false, false, BattleActionCodes.WALK, 0, Effect.NONE,
                0, 1_000));
        register(items, new Definition(
                SECRET_ROOM_KEY, 0, 59, 137, 0, true,
                "$2022", "$2022",
                0, 0, false, false, BattleActionCodes.WALK, 0,
                Effect.CUSTOM_AREA_TELEPORT, 0, 3_000));
        register(items, new Definition(
                POTION_OF_REVIVAL, 0xFF, 947, 5925, 0, true,
                "$2025", "$2025",
                0, 0, false, false, BattleActionCodes.WALK, 0, Effect.NONE,
                0, 1_000));
        // Exact retained etcitem 41003 row. Roy_Pouch is a normal/use-type-0
        // stackable cloth quest reward with native $3422, invgfx 1645,
        // ground gfx 3963 and raw weight 4000. Its randomized opening
        // executor is a separate gameplay slice; acquisition remains exact.
        register(items, new Definition(
                ROI_POUCH, 0, 1645, 3963, 0, true,
                "$3422", "$3422",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.ROI_POUCH,
                0, 4_000));
        // Retained quest122 executors, not the later selected-DB single-class
        // CustomAreaTeleport rows: each item validates the complete same-show
        // four-class party, removes the whole matching stack from every member
        // that owns one, then moves all four to their class-specific landing.
        register(items, new Definition(
                CKEW_SECOND_FLOOR_KEY, 0, 59, 151, 0, true,
                "$5585", "$5585",
                0, 0, true, false, BattleActionCodes.WALK, 0,
                Effect.CKEW_STAGE_TELEPORT, 0, 0));
        register(items, new Definition(
                CKEW_THIRD_FLOOR_KEY, 0, 59, 151, 0, true,
                "$5586", "$5586",
                0, 0, true, false, BattleActionCodes.WALK, 0,
                Effect.CKEW_STAGE_TELEPORT, 0, 0));
        register(items, new Definition(
                CKEW_WILL_OF_DESTRUCTION, 0, 990, 3963, 0, true,
                "$5735", "$5735",
                0, 0, true, false, BattleActionCodes.WALK, 0,
                Effect.CKEW_STAGE_TELEPORT, 0, 1_000));
        // Exact retained etcitem 49167 row and cooperative show executor.
        register(items, new Definition(
                CKEW_MAGIC_FLUTE, 0, 2992, 906, 0, true,
                "$5587", "$5587",
                0, 0, true, false, BattleActionCodes.WALK, 0,
                Effect.CKEW_MAGIC_FLUTE, 0, 8_000));
        register(items, new Definition(
                CKEW_DESTRUCTION_MEDICINE, 0, 2996, 3714, 0, true,
                "$5588", "$5588",
                0, 0, true, false, BattleActionCodes.WALK, 0,
                Effect.CKEW_DESTRUCTION_MEDICINE, 0, 0));
        // Exact retained etcitem 49241 row awarded to every same-show member
        // when the regeneration altar dies.
        register(items, new Definition(
                CKEW_ALTAR_PROOF, 0xFF, 1997, 5048, 0, true,
                "$5799", "$5799",
                0, 0, true, false, BattleActionCodes.WALK, 0,
                Effect.NONE, 0, 0));
        // Exact retained Npc_Dicarding quest122 rewards and matching
        // 380_db rows. They are identified, non-stackable and tradable; the
        // native class masks, safe enchant, damage/AC and MP bonus remain in
        // their status metadata rather than being approximated by fixtures.
        register(items, new Definition(
                CKEW_CROWN_REWARD, 1, 1020, 781, 0, true,
                "$2354", "$2354",
                0, 0, false, true, BattleActionCodes.SWORD_WALK, 0,
                Effect.NONE, 791, 110_000, 0,
                new NativeWeaponStatus(14, 14, 15, 110_000, 1, 6, 1, 0)));
        register(items, new Definition(
                CKEW_KNIGHT_REWARD, 1, 1016, 3, 0, true,
                "$2350", "$2350",
                0, 0, false, true, BattleActionCodes.SWORD_WALK, 0,
                Effect.NONE, 787, 100_000, 0,
                new NativeWeaponStatus(16, 8, 11, 100_000, 2, 6, 2, 0)));
        register(items, new Definition(
                CKEW_ELF_SWORD_REWARD, 1, 1019, 3, 0, true,
                "$2353", "$2353",
                0, 0, false, true, BattleActionCodes.SWORD_WALK, 0,
                Effect.NONE, 790, 80_000, 0,
                new NativeWeaponStatus(14, 6, 14, 80_000, 4, 6, 0, 0)));
        register(items, new Definition(
                CKEW_ELF_BOW_REWARD, 1, 1018, 12, 0, true,
                "$2352", "$2352",
                0, 0, false, true, BattleActionCodes.BOW_WALK, 0,
                Effect.NONE, 789, 30_000, 0,
                new NativeWeaponStatus(3, 3, 14, 30_000, 4, 6, 2, 4)));
        register(items, new Definition(
                CKEW_WIZARD_REWARD, 25, 1017, 2958, 0, true,
                "$2351", "$2351",
                0, 0, false, true, BattleActionCodes.WALK, 0,
                Effect.NONE, 788, 50_000, 0, null,
                new NativeArmorStatus(
                        2, 19, 50_000, 8, 4,
                        0, 0, 0, 0, 0, 0, 0, 100, 0, 0, false,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7)));
        // Exact selected armor 20065 row, the crown-15 Zero reward.  Retain
        // the cloak slot, AC 2, royal-only class mask, safe enchant 4 and
        // charisma +1 so the awarded object remains fully equippable.
        register(items, new Definition(
                RED_CLOAK, 19, 907, 13, 0, true,
                "$1918", "$1918",
                0, 0, false, true, BattleActionCodes.WALK, 0, Effect.NONE,
                0, 10_000, 0, null,
                new NativeArmorStatus(
                        2, 6, 10_000, 1, 4,
                        0, 0, 0, 0, 0, 1, 0, 0, 0, 0, false,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4)));
        // Exact 380_db.sql etcitem 40319 row: material/use type 0,
        // inventory gfx 706, ground gfx 773, ordinary, stackable/tradable,
        // localized $1822 and unit weight 1000. Skills 169/176 consume one.
        register(items, new Definition(
                SPIRIT_CRYSTAL, 0, 706, 773, 0, true,
                "精靈玉", "$1822",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.NONE,
                0, 1_000));
        // Exact shared 380_db/380yiwei_db etcitem49156 row consumed by
        // Illusionist skill205. It is an ordinary stackable/tradable
        // gemstone with inventory GFX3279, ground GFX7090 and weight1000.
        registerMaterial(items, ATTRIBUTE_STONE, 3_279, 7_090,
                "屬性石", "$6346", 1_000);
        // Exact selected 380_db.sql etcitem 49157 row: material/use type 0,
        // inventory gfx3025, ground gfx6927, ordinary blessing, stackable,
        // tradable/droppable, localized $6072 and unit weight1000. Dragon
        // Knight awakening rows 185/186/190/195 consume this material.
        register(items, new Definition(
                AWAKENING_BONE_FRAGMENT, 0, 3_025, 6_927, 0, true,
                "刻印的骨頭碎片", "$6072",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.NONE,
                0, 1_000));
        // Exact retained 40320..40324 dark-stone ladder. All five are
        // material/use type 0, ordinary, stackable and tradable. Skill 100
        // consumes one selected source stack and may store the next tier;
        // skill 98 separately consumes one second-level stone.
        register(items, new Definition(
                FIRST_LEVEL_DARK_STONE, 0, 1061, 2897, 0, true,
                "一級黑魔石", "$2474",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.NONE,
                836, 300));
        register(items, new Definition(
                SECOND_LEVEL_DARK_STONE, 0, 1054, 2898, 0, true,
                "二級黑魔石", "$2475",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.NONE,
                837, 600));
        register(items, new Definition(
                THIRD_LEVEL_DARK_STONE, 0, 1055, 2899, 0, true,
                "三級黑魔石", "$2476",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.NONE,
                838, 900));
        register(items, new Definition(
                FOURTH_LEVEL_DARK_STONE, 0, 1056, 2900, 0, true,
                "四級黑魔石", "$2477",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.NONE,
                839, 1_200));
        register(items, new Definition(
                FIFTH_LEVEL_DARK_STONE, 0, 1057, 2901, 0, true,
                "五級黑魔石", "$2478",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.NONE,
                840, 1_500));
        // Exact 380_db.sql material rows used by skills_item 1..23. Material
        // use type maps to native 0; all ten etcitems are ordinary, stackable
        // and tradable. The localized name-id is preserved in the detail.
        registerMaterial(items, PURE_MITHRIL, 318, 888, "純粹的米索莉塊", "$767", 500);
        registerMaterial(items, MITHRIL_THREAD, 307, 3963, "米索莉線", "$772", 1_000);
        registerMaterial(items, MUSHROOM_JUICE, 82, 47, "蘑菇汁", "$764", 14_000);
        registerMaterial(items, RYUKEN_WEB, 308, 3963, "芮克妮的網", "$769", 1_000);
        registerMaterial(items, RYUKEN_SKIN, 306, 3963, "芮克妮的蛻皮", "$774", 8_000);
        registerMaterial(items, ENT_BARK, 311, 883, "安特之樹皮", "$770", 4_000);
        // Exact selected 380_db rows harvested by L1GuardianInstance.  The
        // retained ItemTable maps hpr to signed use type -7 (wire byte 249).
        register(items, new Definition(
                ENT_FRUIT, 0xF9, 126, 79, 0, true,
                "安特的水果", "$794",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.HEAL,
                127, 8_000));
        register(items, new Definition(
                ENT_BRANCH, 0, 309, 881, 0, true,
                "安特之樹枝", "$763",
                0, 0, true, true, BattleActionCodes.WALK, 0, Effect.ANTIDOTE,
                127, 1_000));
        registerMaterial(items, ORIHARUKON, 319, 3963, "奧裡哈魯根", "$771", 2_000);
        registerMaterial(items, PAN_MANE, 314, 3963, "潘的鬃毛", "$760", 2_000);
        registerMaterial(items, ELVEN_DUST, 312, 884, "精靈粉末", "$768", 1_000);
        registerMaterial(items, ELVEN_WING, 313, 885, "精靈羽翼", "$776", 8_000);
        register(items, new Definition(
                PC_ACTION_HELPER, 0, 1760, 4337,
                RuntimeInventory.BLESSING_BLESSED, false,
                "天堂小幫手", "天堂小幫手", 0, 0, false, true,
                BattleActionCodes.WALK, 0, Effect.NONE, 0, 0));
        ITEMS = Map.copyOf(items);
        BUY_ITEMS = List.of(ITEMS.get(TRAINING_SWORD), ITEMS.get(HEALING_POTION));
    }

    private InventoryCatalog() {}

    static Definition find(long catalogId) {
        Definition builtIn = ITEMS.get(catalogId);
        if (builtIn != null) return builtIn;
        if (catalogId > Integer.MAX_VALUE) return null;
        Definition rental = RentalItemDefinitionCatalog.find((int) catalogId);
        if (rental != null) return rental;
        // Kamyla's five final rewards carry exact selected 380_db hit/damage
        // modifiers and shield status.  Some of the same catalog ids also
        // occur in generic drop tables whose compact ItemWire form cannot
        // retain those two weapon modifiers, so the quest-owned exact rows
        // must win before the generic drop lookup.
        Definition kamyla = ClassicWorldKamylaQuestCatalog.inventoryDefinition(
                (int) catalogId);
        if (kamyla != null) return kamyla;
        Definition desireCave =
                ClassicWorldDesireCaveItemCatalog.inventoryDefinition((int) catalogId);
        if (desireCave != null) return desireCave;
        Definition drop = ClassicWorldDropCatalog.inventoryDefinition((int) catalogId);
        if (drop != null) return drop;
        Definition craft = ClassicWorldCraftCatalog.inventoryDefinition((int) catalogId);
        if (craft != null) return craft;
        Definition shipGrave = ClassicWorldPirateShipGraveCatalog.inventoryDefinition(
                (int) catalogId);
        if (shipGrave != null) return shipGrave;
        Definition ivoryTower = ClassicWorldIvoryTowerCatalog.inventoryDefinition(
                (int) catalogId);
        return ivoryTower != null ? ivoryTower
                : ClassicWorldShopItemCatalog.inventoryDefinition((int) catalogId);
    }

    static Definition require(long catalogId) {
        Definition definition = find(catalogId);
        if (definition == null) throw new IllegalArgumentException("unknown inventory catalog id");
        return definition;
    }

    static List<Definition> buyItems() {
        return BUY_ITEMS;
    }

    /** Exact selected 380_db potion HP ranges; the development item stays fixed. */
    static int[] healingRange(long catalogId) {
        if (catalogId == HEALING_POTION) return new int[] {25, 25};
        if (catalogId == NATIVE_HEALING_POTION) return new int[] {10, 20};
        if (catalogId == EXTRA_HEALING_POTION) return new int[] {30, 50};
        if (catalogId == GREATER_HEALING_POTION) return new int[] {60, 80};
        if (catalogId == ENT_FRUIT) return new int[] {30, 50};
        throw new IllegalArgumentException("catalog id is not a healing potion");
    }

    /** Exact retained {@code hp.UserAddHp} drink-effect gfx for native potions. */
    static int healingEffectGfx(long catalogId) {
        if (catalogId == HEALING_POTION) return 0;
        if (catalogId == NATIVE_HEALING_POTION) return 189;
        if (catalogId == EXTRA_HEALING_POTION) return 194;
        if (catalogId == GREATER_HEALING_POTION) return 197;
        if (catalogId == ENT_FRUIT) return 197;
        throw new IllegalArgumentException("catalog id is not a healing potion");
    }

    static int bluePotionDurationSeconds(long catalogId) {
        if (catalogId == BLUE_POTION || catalogId == WISDOM_COIN) {
            return BLUE_POTION_DURATION_SECONDS;
        }
        if (catalogId == BLESSED_BLUE_POTION) {
            return BLESSED_BLUE_POTION_DURATION_SECONDS;
        }
        throw new IllegalArgumentException("catalog id is not a blue-potion source");
    }

    /**
     * Exact retained {@code L1BuffUtil.stopPotion} scope represented by the
     * item executors currently implemented here: HP/MP and speed potions plus
     * other drinkable potion executors, liquor, and native use-type 38 food.
     */
    static boolean blockedByDecayPotion(Definition definition) {
        if (definition == null) return false;
        return switch (definition.effect()) {
            case HEAL, HASTE, BRAVE, WISDOM, ELVEN_BRAVE, BLUE_MPR,
                    ANTIDOTE, BLIND, BREATH,
                    LIQUOR, COOKING -> true;
            default -> false;
        };
    }

    static NativeWeaponStatus nativeWeaponStatus(long catalogId) {
        // Exact 380_db.sql weapon 24: 6/8, iron=11, weight=30000 and all
        // classes except illusionist.  Exact weapon 88: 2/2, bone=9,
        // weight=12000 and the four original classes.
        if (catalogId == SHORT_SWORD) {
            return new NativeWeaponStatus(6, 8, 11, 30_000, 0x3F, 6);
        }
        if (catalogId == RED_KNIGHT_SWORD) {
            return new NativeWeaponStatus(8, 12, 11, 40_000, 0x3F, 6);
        }
        if (catalogId == ANCIENT_SWORD) {
            return new NativeWeaponStatus(35, 20, 11, 30_000, 0x3B, -1);
        }
        if (catalogId == PAN_HORN) {
            return new NativeWeaponStatus(2, 2, 9, 12_000, 0x0F, 6);
        }
        NativeWeaponStatus drop = ClassicWorldDropCatalog.nativeWeaponStatus((int) catalogId);
        if (drop != null) return drop;
        NativeWeaponStatus craft =
                ClassicWorldCraftCatalog.nativeWeaponStatus((int) catalogId);
        if (craft != null) return craft;
        NativeWeaponStatus ivoryTower =
                ClassicWorldIvoryTowerCatalog.nativeWeaponStatus((int) catalogId);
        return ivoryTower != null ? ivoryTower
                : ClassicWorldShopItemCatalog.nativeWeaponStatus((int) catalogId);
    }

    /**
     * Exact 380_db etcitem_arrow combat rows.  The inventory wire row carries
     * the native use type, blessing and count; this table supplies only the
     * physical fields that the retained arrow executor reads at attack time.
     */
    static NativeProjectileStatus nativeProjectileStatus(long catalogId) {
        return switch ((int) catalogId) {
            case 40738 -> new NativeProjectileStatus(8, 8, 14, -1, true);
            case 40739 -> new NativeProjectileStatus(10, 10, 11, -1, true);
            case 40740 -> new NativeProjectileStatus(14, 14, 11, -1, true);
            case 40741 -> new NativeProjectileStatus(4, 3, 22, -1, true);
            case 40742 -> new NativeProjectileStatus(5, 4, 14, -1, true);
            case 40743 -> new NativeProjectileStatus(6, 5, 8, -1, true);
            case 40744 -> new NativeProjectileStatus(7, 6, 14, -1, true);
            case 40745 -> new NativeProjectileStatus(9, 9, 15, -1, true);
            case 40746 -> new NativeProjectileStatus(10, 9, 17, -1, true);
            case 40747 -> new NativeProjectileStatus(11, 15, 18, -1, true);
            case 40748 -> new NativeProjectileStatus(12, 12, 22, -1, true);
            default -> null;
        };
    }

    static Definition builtIn(long catalogId) {
        return ITEMS.get(catalogId);
    }

    static Definition retainedUnknown(long catalogId) {
        // Preserve an older/local state entry without claiming that it is a
        // usable or sellable native item.
        return new Definition(catalogId, 0, 0, 0, 1, true,
                "Unknown Item " + Long.toUnsignedString(catalogId), "",
                0, 0, true, false, BattleActionCodes.WALK, 0, Effect.NONE);
    }

    private static void register(Map<Long, Definition> items, Definition definition) {
        if (items.put(definition.catalogId(), definition) != null) {
            throw new IllegalStateException("duplicate inventory catalog id " + definition.catalogId());
        }
    }

    private static void registerMaterial(Map<Long, Definition> items, long catalogId,
                                         int icon, int groundGfx, String name, String detail,
                                         int unitWeight) {
        register(items, new Definition(catalogId, 0, icon, groundGfx, 0, true,
                name, detail, 0, 0, true, true,
                BattleActionCodes.WALK, 0, Effect.NONE, 0, unitWeight));
    }

    private static void registerCooking(Map<Long, Definition> items, long catalogId,
                                        int icon, int groundGfx, String nameId,
                                        int itemDescriptionId, int unitWeight) {
        register(items, new Definition(catalogId, 38, icon, groundGfx, 0, true,
                nameId, nameId, 0, 0, true, true,
                BattleActionCodes.WALK, 0, Effect.COOKING,
                itemDescriptionId, unitWeight));
    }

    private static void registerCookingBook(Map<Long, Definition> items, long catalogId,
                                            int icon, int groundGfx, String name,
                                            String detail, int itemDescriptionId) {
        register(items, new Definition(catalogId, 0xF8, icon, groundGfx, 0, true,
                name, detail, 0, 0, false, true,
                BattleActionCodes.WALK, 0, Effect.COOKING_BOOK,
                itemDescriptionId, 1_000));
    }

    private static void registerCookingIngredient(Map<Long, Definition> items, long catalogId,
                                                  int useType, int icon, int groundGfx,
                                                  String name, String detail,
                                                  int itemDescriptionId, int unitWeight) {
        register(items, new Definition(catalogId, useType, icon, groundGfx, 0, true,
                name, detail, 0, 0, true, true,
                BattleActionCodes.WALK, 0, Effect.NONE,
                itemDescriptionId, unitWeight));
    }

    private static void registerShipTicket(Map<Long, Definition> items,
                                           long catalogId, String nameId) {
        register(items, new Definition(catalogId, 0xFF, 138, 151, 0, true,
                nameId, nameId, 0, 0, false, true,
                BattleActionCodes.WALK, 0, Effect.SHIP_TICKET, 0, 1_000));
    }
}
