package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Replays the retained classic server's persisted equipment ability pipeline.
 *
 * <p>The order and even the source server's column-name mismatches are
 * intentional.  In particular, generic {@code add_reduction} is applied by
 * {@code L1EquipmentSlot} to the physical armor damage modifier and is not a
 * magic pre-MR reduction.  Several armor extension tables expose
 * {@code def_fire}, while their retained loaders ask for {@code add_fire}; that
 * absent value therefore contributes zero here as it does in the source.</p>
 */
final class LegacyEquipmentMagicLoader {
    // Schema observations live only for one evaluation, never across connections,
    // databases or later equipment changes. Actual item/ability rows stay live.
    private static final ThreadLocal<Map<String, Set<String>>> EVALUATION_COLUMNS =
            new ThreadLocal<>();
    private static final Set<String> STANDARD_ABILITY_TABLES = Set.of(
            "裝備_weapon_greater_類型", "裝備_armor_greater_類型",
            "裝備_weapon_bless_類型", "裝備_armor_bless_類型",
            "裝備_weapon_bless_指定", "裝備_armor_bless_指定",
            "裝備_weapon_enchant_指定", "裝備_armor_enchant_指定",
            "裝備_weapon_enchant_類型", "裝備_armor_enchant_類型",
            "裝備_特殊_卡片_能力");
    private static final Set<String> REQUIRED_RESOURCE_TABLES = Set.of(
            "armor", "weapon", "etcitem", "npc", "裝備_weapon_屬性",
            "裝備_weapon_greater_類型", "裝備_armor_greater_類型",
            "裝備_weapon_bless_類型", "裝備_armor_bless_類型",
            "裝備_weapon_bless_指定", "裝備_armor_bless_指定",
            "裝備_weapon_enchant_指定", "裝備_armor_enchant_指定",
            "裝備_weapon_enchant_類型", "裝備_armor_enchant_類型",
            "裝備_特殊_卡片_能力", "裝備_特殊_卡片_組合",
            "裝備_特殊_套裝_能力", "裝備_特殊_魔石_魔法");
    private static final Set<String> RESOURCE_TABLES;

    static {
        Set<String> tables = new HashSet<>(REQUIRED_RESOURCE_TABLES);
        // ItemVIPTable treats this extension as optional: when the table is
        // absent its retained loader keeps an empty VIP map. 380yiwei_db
        // intentionally has no such table.
        tables.add("道具_自訂穿脫能力");
        // Current retained ranged combat requires this table, but an older
        // source schema can legitimately omit it when no selected character
        // carries a ranged weapon.
        tables.add("etcitem_arrow");
        RESOURCE_TABLES = Set.copyOf(tables);
    }

    private static final Map<String, Integer> ARMOR_TYPES = Map.ofEntries(
            Map.entry("none", 0), Map.entry("helm", 1), Map.entry("armor", 2),
            Map.entry("T", 3), Map.entry("cloak", 4), Map.entry("glove", 5),
            Map.entry("boots", 6), Map.entry("shield", 7), Map.entry("amulet", 8),
            Map.entry("ring", 9), Map.entry("belt", 10), Map.entry("ring2", 11),
            Map.entry("earring", 12), Map.entry("guarder", 13),
            Map.entry("aid_left", 14), Map.entry("aid_right", 15),
            Map.entry("pants", 17), Map.entry("talisman", 19),
            Map.entry("talisman2", 20), Map.entry("talisman3", 21),
            Map.entry("talisman4", 22), Map.entry("shoulder_left", 23),
            Map.entry("shoulder_right", 24), Map.entry("talisman5", 25));

    private static final Map<String, Integer> WEAPON_TYPES = Map.ofEntries(
            Map.entry("none", 0), Map.entry("sword", 1), Map.entry("dagger", 2),
            Map.entry("tohandsword", 3), Map.entry("bow", 4), Map.entry("spear", 5),
            Map.entry("blunt", 6), Map.entry("staff", 7),
            Map.entry("throwingknife", 8), Map.entry("arrow", 9),
            Map.entry("gauntlet", 10), Map.entry("claw", 11),
            Map.entry("edoryu", 12), Map.entry("singlebow", 13),
            Map.entry("singlespear", 14), Map.entry("tohandblunt", 15),
            Map.entry("tohandstaff", 16), Map.entry("kiringku", 17),
            Map.entry("chainsword", 18));

    /** Exact ItemTable material byte mapping used by the retained server. */
    private static final Map<String, Integer> MATERIAL_TYPES = Map.ofEntries(
            Map.entry("none", 0), Map.entry("liquid", 1), Map.entry("web", 2),
            Map.entry("vegetation", 3), Map.entry("animalmatter", 4),
            Map.entry("paper", 5), Map.entry("cloth", 6), Map.entry("leather", 7),
            Map.entry("wood", 8), Map.entry("bone", 9), Map.entry("dragonscale", 10),
            Map.entry("iron", 11), Map.entry("steel", 12), Map.entry("copper", 13),
            Map.entry("silver", 14), Map.entry("gold", 15), Map.entry("platinum", 16),
            Map.entry("mithril", 17), Map.entry("blackmithril", 18),
            Map.entry("glass", 19), Map.entry("gemstone", 20), Map.entry("mineral", 21),
            Map.entry("oriharukon", 22));

    private LegacyEquipmentMagicLoader() {}

    /**
     * Fails during startup when the selected immutable source is missing a
     * retained STR/CON/HPR/MPR/weight column. LIMIT 0 keeps this a metadata-only read while
     * still making MySQL validate every identifier used by the battle loader.
     */
    static void healthCheck(Connection connection, String resourceDatabase)
            throws SQLException {
        if (connection == null || resourceDatabase == null
                || !resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid legacy equipment resource route");
        }
        checkQuery(connection,
                "SELECT add_str,add_dex,add_con,add_cha,add_hp,add_mp,add_hp_regen,add_mp_regen,add_potion_recovery,add_weight_reduction,add_magic_hit,add_regist_blind,add_regist_sleep,add_regist_freeze,add_regist_stone,add_regist_sustain,add_regist_stun,add_stun_success,"
                        + "add_hit,add_dmg,add_bow_hit,add_bow_dmg,add_ac,add_dmg_redux,"
                        + "add_melee_redux,add_bow_redux,add_ignore_reduction,"
                        + "add_def_fire,add_def_water,add_def_wind,add_def_earth "
                        + "FROM character_items_dark LIMIT 0");
        checkQuery(connection,
                "SELECT attr_enchant_kind,attr_enchant_level FROM character_items LIMIT 0");
        checkQuery(connection, "SELECT invgfx,add_str,add_dex,add_con,add_cha,add_hp,add_mp,add_hpr,add_mpr,add_potion_recovery,weight_reduction,add_magic_hit,regist_blind,regist_sleep,regist_freeze,regist_stone,regist_sustain,regist_stun,add_stun_success,"
                        + "hit_modifier,dmg_modifier,bow_hit_modifier,bow_dmg_modifier,"
                        + "add_hitup,add_dmgup,add_bow_hitup,add_bow_dmgup,ac,"
                        + "add_ignore_reduction,add_melee_redux,add_bow_redux,"
                        + "defense_fire,defense_water,defense_wind,defense_earth FROM "
                + qualified(resourceDatabase, "armor") + " LIMIT 0");
        checkQuery(connection, "SELECT add_str,add_dex,add_con,add_cha,add_hp,add_mp,add_hpr,add_mpr,add_potion_recovery,add_magic_hit,add_stun_success,dmg_small,dmg_large,invgfx,"
                + "`range`,hitmodifier,dmgmodifier,material,bless,add_pierce,"
                + "add_hitup,add_dmgup,add_bow_hitup,add_bow_dmgup,"
                + "add_ignore_reduction,add_melee_redux,add_bow_redux,"
                + "dmg_cri_chance,dmg_cri,dmg_max_chance FROM "
                + qualified(resourceDatabase, "weapon") + " LIMIT 0");
        checkQuery(connection, "SELECT material,bless FROM "
                + qualified(resourceDatabase, "etcitem") + " LIMIT 0");
        checkQuery(connection, "SELECT id,stage,type_dmg_passive FROM "
                + qualified(resourceDatabase, "裝備_weapon_屬性") + " LIMIT 0");
        for (String table : STANDARD_ABILITY_TABLES) {
            // The retained extension readers use safeAdd for optional
            // physical columns. Absence is therefore a qualified zero, not a
            // startup failure; standardAbility mirrors that per table below.
            checkQuery(connection, "SELECT add_str,add_dex,add_con,add_hpr,add_mpr FROM "
                    + qualified(resourceDatabase, table) + " LIMIT 0");
        }
        checkQuery(connection, "SELECT add_str,add_dex,add_con,add_hp,add_mp FROM "
                + qualified(resourceDatabase, "裝備_特殊_魔石_魔法") + " LIMIT 0");
        checkQuery(connection, "SELECT add_str,add_dex,add_wis,add_con,add_cha,add_hp,add_mp,add_hpr,add_mpr,add_potion_recovery,weight_reduction,add_magic_hit,regist_blind,regist_sleep,regist_freeze,regist_stone,regist_sustain,regist_stun,add_stun_success,"
                + "add_hitup,add_dmgup,add_bow_hitup,add_bow_dmgup,add_pierce,"
                + "ac,add_ignore_reduction,defense_fire,defense_water,"
                + "defense_wind,defense_earth FROM "
                + qualified(resourceDatabase, "裝備_特殊_套裝_能力") + " LIMIT 0");
        checkQuery(connection, "SELECT add_str,add_dex,add_wis,add_con,add_cha,add_hp,add_mp,add_hpr,add_mpr,add_potion_recovery,add_weight_reduction,add_magic_hit,regist_blind,regist_sleep,regist_freeze,regist_stone,regist_sustain,regist_stun,add_stun_success,"
                + "add_hit,add_dmg,add_bow_hit,add_bow_dmg,add_pierce,"
                + "add_reduction,add_melee_redux,add_bow_redux,add_ignore_reduction,"
                + "add_fire,add_water,add_wind,add_earth FROM "
                + qualified(resourceDatabase, "裝備_特殊_卡片_組合") + " LIMIT 0");
        if (resourceTableExists(connection, resourceDatabase, "道具_自訂穿脫能力")) {
            checkQuery(connection, "SELECT `力量`,`敏捷`,`體質`,`回血`,`回魔`,`暗黑耐性`,`睡眠耐性`,`寒冰耐性`,`石化耐性`,`支撐耐性`,`昏迷耐性`,"
                    + "`近戰命中`,`近戰傷害`,`遠攻命中`,`遠攻傷害`,"
                    + "`火屬性防禦`,`水屬性防禦`,`風屬性防禦`,`地屬性防禦` FROM "
                    + qualified(resourceDatabase, "道具_自訂穿脫能力") + " LIMIT 0");
        }
        if (resourceTableExists(connection, resourceDatabase, "etcitem_arrow")) {
            checkQuery(connection,
                    "SELECT item_id,consume_on_attack,dmg_small,dmg_large,arrow_spr FROM "
                            + qualified(resourceDatabase, "etcitem_arrow") + " LIMIT 0");
        }
    }

    private static void checkQuery(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet ignored = statement.executeQuery()) {
            // Metadata validation only.
        }
    }

    static SourceEquipmentMagicStats load(
            Connection connection, String resourceDatabase,
            long characterObjectId, long nowMillis) throws SQLException {
        return loadEvaluation(connection, resourceDatabase, characterObjectId,
                nowMillis, Map.of()).magic();
    }

    static SourceEquipmentEvaluation loadEvaluation(
            Connection connection, String resourceDatabase,
            long characterObjectId, long nowMillis,
            Map<Long, Boolean> equippedOverrides) throws SQLException {
        if (connection == null || resourceDatabase == null
                || !resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid legacy equipment resource route");
        }
        if (characterObjectId <= 0 || characterObjectId > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("legacy character object id is outside dword range");
        }
        if (equippedOverrides == null) {
            throw new IllegalArgumentException("source equipment overrides are required");
        }
        for (Map.Entry<Long, Boolean> override : equippedOverrides.entrySet()) {
            if (override.getKey() == null || override.getKey() <= 0
                    || override.getKey() > 0xFFFF_FFFFL || override.getValue() == null) {
                throw new IllegalArgumentException("invalid source equipment override");
            }
        }

        Map<String, Set<String>> previousColumns = EVALUATION_COLUMNS.get();
        EVALUATION_COLUMNS.set(new HashMap<>());
        try {
        int characterType = characterType(connection, characterObjectId);
        List<ItemRow> items = equipmentItems(
                connection, characterObjectId, equippedOverrides);
        boolean healingHelmEquipped = items.stream()
                .anyMatch(item -> item.itemId == 20014);
        boolean hasteCostHalvingHelmEquipped = items.stream()
                .anyMatch(item -> halvesHasteCost(item.itemId));
        boolean greaterHasteCostHalvingHelmEquipped = items.stream()
                .anyMatch(item -> halvesGreaterHasteCost(item.itemId));
        Ability itemAbility = new Ability();
        int armorTemplateReduction = 0;
        int armorTemplateWeightReduction = 0;
        int armorMeleeHit = 0;
        int armorMeleeDamage = 0;
        int armorRangedHit = 0;
        int armorRangedDamage = 0;
        boolean shieldEquipped = false;
        boolean guarderEquipped = false;
        SourceDefensiveEquipment defensiveEquipment =
                SourceDefensiveEquipment.none();
        SourceWeaponPhysicalStats equippedWeapon = SourceWeaponPhysicalStats.none();
        int matchedArmorRows = 0;
        int matchedVipRows = 0;
        int singleCardRows = 0;
        Map<Integer, Integer> setCounts = new HashMap<>();
        Set<Integer> uniqueCards = new HashSet<>();
        boolean vipTableAvailable = resourceTableExists(
                connection, resourceDatabase, "道具_自訂穿脫能力");

        for (ItemRow item : items) {
            Template armor = template(connection, resourceDatabase, "armor", item.itemId, true);
            Template weapon = template(connection, resourceDatabase, "weapon", item.itemId, false);
            if (armor != null && weapon != null) {
                throw new SQLException("equipped item has ambiguous resource template: "
                        + item.itemId);
            }
            if (armor == null && weapon == null) {
                if (!etcItemExists(connection, resourceDatabase, item.itemId)) {
                    throw new SQLException("equipped item has no resource template: "
                            + item.itemId);
                }

                // Retained login applies VIP attributes before temporarily
                // clearing equipped flags. Ordinary etcitems later pass
                // through EquipSlot, whose type2==0 branch is an exact no-op.
                // Neither path applies per-item dark/card/stone attributes.
                if (vipTableAvailable) {
                    Ability vip = vipAbility(connection, resourceDatabase, item.itemId);
                    if (vip != null) {
                        itemAbility.addApplied(vip);
                        matchedVipRows++;
                    }
                }
                // updateSetBonus() still scans every equipped item, including
                // etcitems, so their card IDs can activate cross-item combos.
                addUniqueCards(item.cards, uniqueCards);
                continue;
            }
            Template template = armor != null ? armor : weapon;
            Ability directAbility = new Ability();
            if (armor != null) {
                matchedArmorRows++;
                if (template.type == 7 || template.type == 13) {
                    if (defensiveEquipment.present()) {
                        throw new SQLException(
                                "character has more than one equipped shield/guarder");
                    }
                    shieldEquipped = template.type == 7;
                    guarderEquipped = template.type == 13;
                    defensiveEquipment = new SourceDefensiveEquipment(
                            item.itemId, template.type, template.inventoryGfx,
                            item.bless, template.nameId);
                }
                armorTemplateReduction += template.damageReduction;
                armorTemplateWeightReduction += template.weightReduction;
                armorMeleeHit += template.meleeHitModifier;
                armorMeleeDamage += template.meleeDamageModifier;
                armorRangedHit += template.rangedHitModifier;
                armorRangedDamage += template.rangedDamageModifier;
            } else {
                if (equippedWeapon.present()) {
                    throw new SQLException("character has more than one equipped weapon");
                }
                equippedWeapon = new SourceWeaponPhysicalStats(
                        item.itemId, template.type,
                        template.damageSmall, template.damageLarge, template.range,
                        template.meleeHitModifier, template.meleeDamageModifier,
                        item.enchantLevel, item.durability)
                        .withNameId(template.nameId)
                        .withCombatTraits(sourceWeaponCombatTraits(
                                connection, resourceDatabase, template, item));
            }

            directAbility.add(template.baseAbility);
            if (armor != null) {
                // L1ItemInstance adds -enchantLevel for every retained armor,
                // including cursed negative enchant values.
                directAbility.armorClass -= item.enchantLevel;
            }
            for (int setId : parseSetIds(template.setIds)) {
                if (setId > 0) setCounts.merge(setId, 1, Integer::sum);
            }

            String kind = template.armor ? "armor" : "weapon";
            String typeColumn = template.armor ? "armor_type" : "weapon_type";
            directAbility.add(standardAbility(connection, resourceDatabase,
                    "裝備_" + kind + "_greater_類型",
                    "tier_id=? AND " + typeColumn + "=?", true,
                    item.greater, template.type));
            // Exact retained loader mismatch: the armor table has def_fire,
            // while fillCommonAbility asks for add_fire. Weapon has neither.
            directAbility.add(standardAbility(connection, resourceDatabase,
                    "裝備_" + kind + "_bless_類型",
                    "bless_id=? AND " + typeColumn + "=?", false,
                    item.bless, template.type));
            directAbility.add(standardAbility(connection, resourceDatabase,
                    "裝備_" + kind + "_bless_指定",
                    "item_id=? AND bless_id=?", true, item.itemId, item.bless));

            if (item.enchantLevel > 0) {
                // Exact retained loader mismatch for fire mirrors bless/type.
                directAbility.add(standardAbility(connection, resourceDatabase,
                        "裝備_" + kind + "_enchant_指定",
                        "item_id=? AND enchant_level=?", false,
                        item.itemId, item.enchantLevel));
                int overEnchant = item.enchantLevel - template.safeEnchant;
                if (overEnchant > 0) {
                    directAbility.add(standardAbility(connection, resourceDatabase,
                            "裝備_" + kind + "_enchant_類型",
                            "over_enchant=? AND " + typeColumn + "=?", false,
                            overEnchant, template.type));
                }
            }

            directAbility.add(item.darkAbility);
            for (int cardId : item.cards) {
                if (cardId <= 1) continue;
                uniqueCards.add(cardId);
                Ability card = standardAbility(connection, resourceDatabase,
                        "裝備_特殊_卡片_能力", "item_id=?", true, true, cardId);
                if (card != null) {
                    directAbility.add(card);
                    singleCardRows++;
                }
            }

            boolean stoneActive = item.magicStoneItemId > 0
                    && (item.magicStoneTime == null
                    || nowMillis < item.magicStoneTime.getTime());
            if (stoneActive) {
                directAbility.add(stoneAbility(connection, resourceDatabase,
                        item.magicStoneItemId, item.magicStoneLevel));
            }

            if (characterType == 2 && "$187".equals(template.nameId)) {
                directAbility.mr += 5;
            }
            if (weapon != null) {
                equippedWeapon = equippedWeapon.withAbility(
                        directAbility.meleeHit, directAbility.meleeDamage,
                        directAbility.rangedHit, directAbility.rangedDamage);
            }
            // The retained applyAllAttributes cast the final per-item ability
            // aggregate to byte once. Schema 2 replaces those consumers with
            // dword sidecars, so preserve the signed aggregate there; older
            // clients receive a saturated byte shadow instead of a wrap.
            itemAbility.addApplied(directAbility);
        }

        Ability setAbility = new Ability();
        int activeSetBonuses = 0;
        for (Map.Entry<Integer, Integer> entry : setCounts.entrySet()) {
            Ability matched = setAbility(connection, resourceDatabase,
                    entry.getKey(), entry.getValue());
            if (matched != null) {
                setAbility.add(matched);
                activeSetBonuses++;
            }
        }

        int activeCardCombos = addCardCombos(
                connection, resourceDatabase, uniqueCards, setAbility);
        if (equippedWeapon.ranged()) {
            SourceProjectilePhysicalStats projectile = sourceProjectile(
                    connection, resourceDatabase, characterObjectId, equippedWeapon);
            if (projectile.present()) equippedWeapon = equippedWeapon.withProjectile(projectile);
        }
        SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(
                equippedWeapon,
                itemAbility.meleeHit - equippedWeapon.abilityMeleeHit(),
                itemAbility.meleeDamage - equippedWeapon.abilityMeleeDamage(),
                itemAbility.rangedHit - equippedWeapon.abilityRangedHit(),
                itemAbility.rangedDamage - equippedWeapon.abilityRangedDamage(),
                armorMeleeHit, armorMeleeDamage,
                armorRangedHit, armorRangedDamage,
                setAbility.meleeHit, setAbility.meleeDamage,
                setAbility.rangedHit, setAbility.rangedDamage,
                itemAbility.dexterity, setAbility.dexterity,
                itemAbility.pierce, setAbility.pierce,
                itemAbility.ignoreReduction, setAbility.ignoreReduction,
                itemAbility.meleeReduction, itemAbility.rangedReduction,
                itemAbility.armorClass, setAbility.armorClass,
                itemAbility.evasionRate, setAbility.evasionRate,
                new SourceEquipmentElementalStats(
                        itemAbility.earth, itemAbility.fire,
                        itemAbility.water, itemAbility.wind,
                        setAbility.earth, setAbility.fire,
                        setAbility.water, setAbility.wind),
                shieldEquipped, guarderEquipped, defensiveEquipment,
                Math.addExact(itemAbility.pvpReduction,
                        setAbility.pvpReduction));
        SourceEquipmentMagicStats magic = new SourceEquipmentMagicStats(
                itemAbility.intelligence, itemAbility.wisdom, itemAbility.constitution,
                itemAbility.sp,
                itemAbility.mr, itemAbility.magicDamage, itemAbility.fire,
                armorTemplateReduction,
                setAbility.intelligence, setAbility.wisdom, setAbility.constitution,
                setAbility.mr, setAbility.magicDamage,
                setAbility.fire, setAbility.reduction,
                itemAbility.hpRegen, setAbility.hpRegen,
                itemAbility.mpRegen, setAbility.mpRegen,
                items.size(), matchedArmorRows, matchedVipRows, singleCardRows,
                activeSetBonuses, activeCardCombos,
                itemAbility.strength, itemAbility.weightReduction,
                armorTemplateWeightReduction,
                setAbility.strength, setAbility.weightReduction,
                itemAbility.magicHit, setAbility.magicHit,
                physical, healingHelmEquipped,
                itemAbility.blindResistance, setAbility.blindResistance,
                itemAbility.sleepResistance, setAbility.sleepResistance,
                itemAbility.freezeResistance, setAbility.freezeResistance,
                itemAbility.stoneResistance, setAbility.stoneResistance,
                itemAbility.sustainResistance, setAbility.sustainResistance,
                hasteCostHalvingHelmEquipped,
                greaterHasteCostHalvingHelmEquipped,
                itemAbility.stunResistance, setAbility.stunResistance,
                itemAbility.stunSuccess, setAbility.stunSuccess);
        SourceEquipmentVitalStats vital = new SourceEquipmentVitalStats(
                itemAbility.maximumHp, setAbility.maximumHp,
                itemAbility.maximumMp, setAbility.maximumMp,
                itemAbility.charisma, setAbility.charisma,
                itemAbility.potionRecovery, setAbility.potionRecovery);
        return new SourceEquipmentEvaluation(magic, vital);
        } finally {
            if (previousColumns == null) EVALUATION_COLUMNS.remove();
            else EVALUATION_COLUMNS.set(previousColumns);
        }
    }

    static boolean halvesHasteCost(long itemId) {
        return itemId == 20008 || itemId == 20013;
    }

    static boolean halvesGreaterHasteCost(long itemId) {
        return itemId == 20023;
    }

    private static int characterType(Connection connection, long characterObjectId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT Type FROM characters WHERE objid=? LIMIT 1")) {
            statement.setLong(1, characterObjectId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new SQLException("equipment source character was not found");
                int type = row.getInt(1);
                if (type < 0 || type > 6) {
                    throw new SQLException("equipment source character has unsupported type");
                }
                return type;
            }
        }
    }

    private static List<ItemRow> equipmentItems(
            Connection connection, long characterObjectId,
            Map<Long, Boolean> equippedOverrides) throws SQLException {
        boolean override = !equippedOverrides.isEmpty();
        String sql = """
                SELECT i.id, i.item_id, i.is_equipped,
                       i.enchantlvl, i.durability, i.bless, i.greater,
                       i.attr_enchant_kind, i.attr_enchant_level,
                       i.magic_stone_item_id, i.magic_stone_level, i.magic_stone_time,
                       COALESCE(c.card_1,0) card_1, COALESCE(c.card_2,0) card_2,
                       COALESCE(c.card_3,0) card_3, COALESCE(c.card_4,0) card_4,
                       COALESCE(c.card_5,0) card_5,
                       COALESCE(d.add_str,0) dark_str,
                       COALESCE(d.add_dex,0) dark_dex,
                       COALESCE(d.add_int,0) dark_int,
                       COALESCE(d.add_wis,0) dark_wis,
                       COALESCE(d.add_con,0) dark_con,
                       COALESCE(d.add_cha,0) dark_cha,
                       COALESCE(d.add_hp,0) dark_hp,
                       COALESCE(d.add_mp,0) dark_mp,
                       COALESCE(d.add_sp,0) dark_sp,
                       COALESCE(d.add_mr,0) dark_mr,
                       COALESCE(d.add_magic_dmg_modifier,0) dark_magic_dmg,
                       COALESCE(d.add_magic_hit,0) dark_magic_hit,
                       COALESCE(d.add_regist_blind,0) dark_blind_resistance,
                       COALESCE(d.add_regist_sleep,0) dark_sleep_resistance,
                       COALESCE(d.add_regist_freeze,0) dark_freeze_resistance,
                       COALESCE(d.add_regist_stone,0) dark_stone_resistance,
                       COALESCE(d.add_regist_sustain,0) dark_sustain_resistance,
                       COALESCE(d.add_regist_stun,0) dark_stun_resistance,
                       COALESCE(d.add_stun_success,0) dark_stun_success,
                       COALESCE(d.add_def_fire,0) dark_fire,
                       COALESCE(d.add_def_water,0) dark_water,
                       COALESCE(d.add_def_wind,0) dark_wind,
                       COALESCE(d.add_def_earth,0) dark_earth,
                       COALESCE(d.add_hp_regen,0) dark_hpr,
                       COALESCE(d.add_mp_regen,0) dark_mpr,
                       COALESCE(d.add_potion_recovery,0) dark_potion_recovery,
                       COALESCE(d.add_weight_reduction,0) dark_weight_reduction,
                       COALESCE(d.add_hit,0) dark_hit,
                       COALESCE(d.add_dmg,0) dark_dmg,
                       COALESCE(d.add_bow_hit,0) dark_bow_hit,
                       COALESCE(d.add_bow_dmg,0) dark_bow_dmg,
                       COALESCE(d.add_ac,0) dark_ac,
                       COALESCE(d.add_er,0) dark_er,
                       COALESCE(d.add_dmg_redux,0) dark_reduction,
                       COALESCE(d.add_melee_redux,0) dark_melee_reduction,
                       COALESCE(d.add_bow_redux,0) dark_ranged_reduction,
                       COALESCE(d.add_ignore_reduction,0) dark_ignore_reduction
                  FROM character_items i
                  LEFT JOIN character_items_card c ON c.item_objid=i.id
                  LEFT JOIN character_items_dark d ON d.item_objid=i.id
                 WHERE i.char_id=?
                ORDER BY i.id
                """;
        List<ItemRow> result = new ArrayList<>();
        Set<Long> observedOverrides = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, characterObjectId);
            try (ResultSet row = statement.executeQuery()) {
                while (row.next()) {
                    long objectId = row.getLong("id");
                    Boolean overrideState = equippedOverrides.get(objectId);
                    if (overrideState != null) observedOverrides.add(objectId);
                    boolean equipped = overrideState == null
                            ? row.getBoolean("is_equipped") : overrideState;
                    if (!equipped) continue;
                    int[] cards = new int[5];
                    for (int index = 0; index < cards.length; index++) {
                        cards[index] = row.getInt("card_" + (index + 1));
                    }
                    Ability dark = new Ability(
                            row.getInt("dark_int"), row.getInt("dark_wis"),
                            row.getInt("dark_con"),
                             row.getInt("dark_sp"), row.getInt("dark_mr"),
                             row.getInt("dark_magic_dmg"), row.getInt("dark_fire"), 0,
                             row.getInt("dark_hpr"), row.getInt("dark_str"),
                             row.getInt("dark_weight_reduction"));
                    dark.mpRegen = row.getInt("dark_mpr");
                    dark.magicHit = row.getInt("dark_magic_hit");
                    dark.blindResistance = row.getInt("dark_blind_resistance");
                    dark.sleepResistance = row.getInt("dark_sleep_resistance");
                    dark.freezeResistance = row.getInt("dark_freeze_resistance");
                    dark.stoneResistance = row.getInt("dark_stone_resistance");
                    dark.sustainResistance = row.getInt("dark_sustain_resistance");
                    dark.stunResistance = row.getInt("dark_stun_resistance");
                    dark.stunSuccess = row.getInt("dark_stun_success");
                    dark.dexterity = row.getInt("dark_dex");
                    dark.meleeHit = row.getInt("dark_hit");
                    dark.meleeDamage = row.getInt("dark_dmg");
                    dark.rangedHit = row.getInt("dark_bow_hit");
                    dark.rangedDamage = row.getInt("dark_bow_dmg");
                    dark.armorClass = row.getInt("dark_ac");
                    dark.evasionRate = row.getInt("dark_er");
                    dark.reduction = row.getInt("dark_reduction");
                    dark.meleeReduction = row.getInt("dark_melee_reduction");
                    dark.rangedReduction = row.getInt("dark_ranged_reduction");
                    dark.ignoreReduction = row.getInt("dark_ignore_reduction");
                    dark.water = row.getInt("dark_water");
                    dark.wind = row.getInt("dark_wind");
                    dark.earth = row.getInt("dark_earth");
                    dark.charisma = row.getInt("dark_cha");
                    dark.maximumHp = row.getInt("dark_hp");
                    dark.maximumMp = row.getInt("dark_mp");
                    dark.potionRecovery = row.getInt("dark_potion_recovery");
                    result.add(new ItemRow(
                            objectId, row.getInt("item_id"),
                            row.getInt("enchantlvl"), row.getInt("durability"),
                            row.getInt("bless"),
                            row.getInt("greater"),
                            row.getInt("attr_enchant_kind"),
                            row.getInt("attr_enchant_level"),
                            row.getInt("magic_stone_item_id"),
                            row.getInt("magic_stone_level"),
                            row.getTimestamp("magic_stone_time"), cards, dark));
                }
            }
        }
        if (override && !observedOverrides.equals(equippedOverrides.keySet())) {
            Set<Long> missing = new HashSet<>(equippedOverrides.keySet());
            missing.removeAll(observedOverrides);
            throw new SQLException("source equipment override object was not found: " + missing);
        }
        return result;
    }

    private static Template template(Connection connection, String database,
                                     String table, int itemId, boolean armor)
            throws SQLException {
        String sql = armor
                ? "SELECT type,set_id,name_id,safenchant,invgfx,add_str,add_dex,add_int,add_wis,add_con,add_cha,add_hp,add_mp,add_sp,add_hpr,add_mpr,add_potion_recovery,add_magic_hit,regist_blind,regist_sleep,regist_freeze,regist_stone,regist_sustain,regist_stun,add_stun_success,m_def,"
                        + "magicdmgmodifier,defense_fire,defense_water,defense_wind,defense_earth,"
                        + "damage_reduction,weight_reduction,"
                        + "hit_modifier,dmg_modifier,bow_hit_modifier,bow_dmg_modifier,"
                        + "add_hitup,add_dmgup,add_bow_hitup,add_bow_dmgup,"
                        + "ac,add_er,add_ignore_reduction,add_melee_redux,add_bow_redux,"
                        + "add_pvp_redux,"
                        + "0 AS dmg_cri_chance,0.0 AS dmg_cri,0 AS dmg_max_chance,"
                        + "0 AS dmg_cri_effect,0 AS dmg_max_effect,"
                        + "0 AS material,1 AS bless,0 AS add_pierce FROM "
                        + qualified(database, table) + " WHERE item_id=? LIMIT 1"
                : "SELECT type,set_id,name_id,safenchant,invgfx,add_str,add_dex,add_int,add_wis,add_con,add_cha,add_hp,add_mp,add_sp,add_hpr,add_mpr,add_potion_recovery,add_magic_hit,"
                        + optionalPhysicalColumn(connection, database, table, "regist_blind") + ","
                        + optionalPhysicalColumn(connection, database, table, "regist_sleep") + ","
                        + optionalPhysicalColumn(connection, database, table, "regist_freeze") + ","
                        + optionalPhysicalColumn(connection, database, table, "regist_stone") + ","
                        + optionalPhysicalColumn(connection, database, table, "regist_sustain") + ","
                        + optionalPhysicalColumn(connection, database, table, "regist_stun") + ","
                        + optionalPhysicalColumn(connection, database, table, "add_stun_success") + ","
                        + "magicdmgmodifier,0 AS defense_fire,0 AS defense_water,"
                        + "0 AS defense_wind,0 AS defense_earth,"
                        + "dmg_small,dmg_large,`range`,hitmodifier,dmgmodifier,"
                        + "add_hitup,add_dmgup,add_bow_hitup,add_bow_dmgup,"
                        + optionalPhysicalColumn(connection, database, table, "add_er") + ","
                        + "add_ignore_reduction,add_melee_redux,add_bow_redux,"
                        + "add_pvp_redux,"
                        + "dmg_cri_chance,dmg_cri,dmg_max_chance,"
                        + optionalPhysicalColumn(
                                connection, database, table, "dmg_cri_effect") + ","
                        + optionalPhysicalColumn(
                                connection, database, table, "dmg_max_effect") + ","
                        + "0 AS ac,material,bless,add_pierce FROM "
                        + qualified(database, table)
                        + " WHERE item_id=? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, itemId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) return null;
                String typeName = row.getString("type");
                Integer type = (armor ? ARMOR_TYPES : WEAPON_TYPES).get(typeName);
                if (type == null) {
                    throw new SQLException("unsupported " + table + " type: " + typeName);
                }
                Ability ability = new Ability(
                        appliedAbilityModifier(row.getLong("add_int")),
                        appliedAbilityModifier(row.getLong("add_wis")),
                        appliedAbilityModifier(row.getLong("add_con")),
                        row.getInt("add_sp"), armor ? row.getInt("m_def") : 0,
                         row.getInt("magicdmgmodifier"),
                         armor ? row.getInt("defense_fire") : 0, 0,
                         row.getInt("add_hpr"),
                         appliedAbilityModifier(row.getLong("add_str")), 0);
                ability.mpRegen = row.getInt("add_mpr");
                ability.charisma = appliedAbilityModifier(row.getLong("add_cha"));
                ability.maximumHp = row.getInt("add_hp");
                ability.maximumMp = row.getInt("add_mp");
                ability.potionRecovery = row.getInt("add_potion_recovery");
                ability.magicHit = row.getInt("add_magic_hit");
                ability.blindResistance = row.getInt("regist_blind");
                ability.sleepResistance = row.getInt("regist_sleep");
                ability.freezeResistance = row.getInt("regist_freeze");
                ability.stoneResistance = row.getInt("regist_stone");
                ability.sustainResistance = row.getInt("regist_sustain");
                ability.stunResistance = row.getInt("regist_stun");
                ability.stunSuccess = row.getInt("add_stun_success");
                ability.dexterity = appliedAbilityModifier(row.getLong("add_dex"));
                ability.meleeHit = row.getInt("add_hitup");
                ability.meleeDamage = row.getInt("add_dmgup");
                ability.rangedHit = row.getInt("add_bow_hitup");
                ability.rangedDamage = row.getInt("add_bow_dmgup");
                ability.pierce = row.getInt("add_pierce");
                ability.armorClass = row.getInt("ac");
                ability.evasionRate = row.getInt("add_er");
                ability.ignoreReduction = row.getInt("add_ignore_reduction");
                ability.meleeReduction = row.getInt("add_melee_redux");
                ability.rangedReduction = row.getInt("add_bow_redux");
                ability.pvpReduction = row.getInt("add_pvp_redux");
                ability.water = row.getInt("defense_water");
                ability.wind = row.getInt("defense_wind");
                ability.earth = row.getInt("defense_earth");
                Integer material = armor ? 0 : MATERIAL_TYPES.get(row.getString("material"));
                if (material == null) {
                    throw new SQLException("unsupported weapon material: "
                            + row.getString("material"));
                }
                return new Template(armor, type, row.getString("set_id"),
                        row.getString("name_id"), row.getInt("safenchant"),
                        material, row.getInt("bless"),
                        armor ? row.getInt("damage_reduction") : 0,
                        armor ? row.getInt("weight_reduction") : 0,
                        armor ? 0 : row.getInt("dmg_small"),
                        armor ? 0 : row.getInt("dmg_large"),
                        armor ? 0 : row.getInt("range"),
                        row.getInt("invgfx"),
                        armor ? row.getInt("hit_modifier") : row.getInt("hitmodifier"),
                        armor ? row.getInt("dmg_modifier") : row.getInt("dmgmodifier"),
                        armor ? row.getInt("bow_hit_modifier") : 0,
                        armor ? row.getInt("bow_dmg_modifier") : 0,
                        row.getInt("dmg_cri_chance"),
                        row.getDouble("dmg_cri"),
                        row.getInt("dmg_max_chance"),
                        row.getInt("dmg_cri_effect") == 1,
                        row.getInt("dmg_max_effect") == 1,
                        ability);
            }
        }
    }

    private static boolean resourceTableExists(
            Connection connection, String database, String table) throws SQLException {
        // Validate through the same fail-closed identifier whitelist even
        // though INFORMATION_SCHEMA uses bound values rather than identifiers.
        qualified(database, table);
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE TABLE_SCHEMA=? AND TABLE_NAME=? LIMIT 1")) {
            statement.setString(1, database);
            statement.setString(2, table);
            try (ResultSet row = statement.executeQuery()) {
                return row.next();
            }
        }
    }

    private static boolean resourceColumnExists(
            Connection connection, String database, String table, String column)
            throws SQLException {
        qualified(database, table);
        if (column == null || !column.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid equipment resource column");
        }
        Map<String, Set<String>> cache = EVALUATION_COLUMNS.get();
        String key = database + "." + table;
        Set<String> columns = cache == null ? null : cache.get(key);
        if (columns != null) return columns.contains(column);
        columns = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_SCHEMA=? AND TABLE_NAME=?")) {
            statement.setString(1, database);
            statement.setString(2, table);
            try (ResultSet row = statement.executeQuery()) {
                while (row.next()) columns.add(row.getString(1));
            }
        }
        if (cache != null) cache.put(key, Set.copyOf(columns));
        return columns.contains(column);
    }

    private static String optionalPhysicalColumn(
            Connection connection, String database, String table, String column)
            throws SQLException {
        return resourceColumnExists(connection, database, table, column)
                ? column : "0 AS " + column;
    }

    private static boolean etcItemExists(
            Connection connection, String database, int itemId) throws SQLException {
        String sql = "SELECT 1 FROM " + qualified(database, "etcitem")
                + " WHERE item_id=? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, itemId);
            try (ResultSet row = statement.executeQuery()) {
                return row.next();
            }
        }
    }

    private static SourceWeaponCombatTraits sourceWeaponCombatTraits(
            Connection connection, String database, Template template, ItemRow item)
            throws SQLException {
        int attributeDamage = 0;
        if (item.attributeKind != 0 || item.attributeLevel != 0) {
            if (item.attributeKind <= 0 || item.attributeLevel <= 0) {
                throw new SQLException("incomplete source weapon attribute enchant: "
                        + item.itemId);
            }
            String sql = "SELECT type_dmg_passive FROM "
                    + qualified(database, "裝備_weapon_屬性")
                    + " WHERE id=? AND stage=? LIMIT 1";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, item.attributeKind);
                statement.setInt(2, item.attributeLevel);
                try (ResultSet row = statement.executeQuery()) {
                    if (!row.next()) {
                        throw new SQLException("source weapon attribute row was not found: "
                                + item.attributeKind + "/" + item.attributeLevel);
                    }
                    attributeDamage = row.getInt("type_dmg_passive");
                }
            }
        }
        try {
            return new SourceWeaponCombatTraits(
                    template.material, template.blessing,
                    item.attributeKind, item.attributeLevel, attributeDamage,
                    template.doubleDamageChance, template.doubleDamageMultiplier,
                    template.maximumDamageChance,
                    template.doubleDamageEffect, template.maximumDamageEffect,
                    template.inventoryGfx);
        } catch (IllegalArgumentException invalid) {
            throw new SQLException("invalid source weapon combat traits: "
                    + item.itemId, invalid);
        }
    }

    private static SourceProjectilePhysicalStats sourceProjectile(
            Connection connection, String database, long characterObjectId,
            SourceWeaponPhysicalStats weapon) throws SQLException {
        if (!weapon.ranged()) return SourceProjectilePhysicalStats.none();
        if (!resourceTableExists(connection, database, "etcitem_arrow")) {
            throw new SQLException("ranged source weapon requires etcitem_arrow resource");
        }
        String useType = weapon.weaponType() == 10 ? "sting" : "arrow";
        String sql = "SELECT i.id AS object_id,i.item_id,i.count,a.consume_on_attack,"
                + "a.dmg_small,a.dmg_large,a.arrow_spr,e.material,e.bless "
                + "FROM character_items i JOIN " + qualified(database, "etcitem")
                + " e ON e.item_id=i.item_id JOIN "
                + qualified(database, "etcitem_arrow")
                + " a ON a.item_id=i.item_id "
                + "WHERE i.char_id=? AND i.count>0 AND i.is_equipped=0 "
                + "AND e.use_type=? ORDER BY i.id LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, characterObjectId);
            statement.setString(2, useType);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) return SourceProjectilePhysicalStats.none();
                Integer material = MATERIAL_TYPES.get(row.getString("material"));
                if (material == null) {
                    throw new SQLException("unsupported projectile material: "
                            + row.getString("material"));
                }
                return new SourceProjectilePhysicalStats(
                        row.getLong("object_id"), row.getInt("item_id"),
                        weapon.weaponType() == 10 ? -3 : -2,
                        row.getInt("dmg_small"), row.getInt("dmg_large"),
                        material, row.getInt("bless"),
                        row.getInt("arrow_spr"), row.getInt("consume_on_attack") == 1,
                        row.getLong("count"));
            }
        }
    }

    private static Ability vipAbility(
            Connection connection, String database, int itemId) throws SQLException {
        String sql = "SELECT `力量` AS add_str,`敏捷` AS add_dex,"
                + "`智力` AS add_int,`精神` AS add_wis,"
                + "`體質` AS add_con,"
                + "`抗魔` AS add_mr,`魔攻` AS add_sp,"
                + "`回血` AS add_hpr,"
                + "`回魔` AS add_mpr,`暗黑耐性` AS regist_blind,`睡眠耐性` AS regist_sleep,"
                + "`寒冰耐性` AS regist_freeze,`石化耐性` AS regist_stone,`支撐耐性` AS regist_sustain,"
                + "`火屬性防禦` AS add_fire,"
                + "`水屬性防禦` AS add_water,"
                + "`風屬性防禦` AS add_wind,"
                + "`地屬性防禦` AS add_earth,"
                + "`近戰命中` AS add_hit,`近戰傷害` AS add_dmg,"
                + "`遠攻命中` AS add_bow_hit,`遠攻傷害` AS add_bow_dmg,"
                + "`昏迷耐性` AS regist_stun,0 AS add_stun_success FROM "
                + qualified(database, "道具_自訂穿脫能力")
                + " WHERE `道具編號`=? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, itemId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) return null;
                // ItemVIPTable has no magic-damage modifier. Its 傷害減免 is
                // routed to addDmgModifierByArmor (physical), and the retained
                // L1MagicPc path never reads 魔法傷害減免.
                Ability ability = new Ability(row.getInt("add_int"), row.getInt("add_wis"),
                        row.getInt("add_con"),
                        row.getInt("add_sp"), row.getInt("add_mr"),
                        0, row.getInt("add_fire"), 0, row.getInt("add_hpr"),
                        row.getInt("add_str"), 0);
                ability.mpRegen = row.getInt("add_mpr");
                ability.blindResistance = row.getInt("regist_blind");
                ability.sleepResistance = row.getInt("regist_sleep");
                ability.freezeResistance = row.getInt("regist_freeze");
                ability.stoneResistance = row.getInt("regist_stone");
                ability.sustainResistance = row.getInt("regist_sustain");
                ability.stunResistance = row.getInt("regist_stun");
                ability.stunSuccess = row.getInt("add_stun_success");
                ability.dexterity = row.getInt("add_dex");
                ability.meleeHit = row.getInt("add_hit");
                ability.meleeDamage = row.getInt("add_dmg");
                ability.rangedHit = row.getInt("add_bow_hit");
                ability.rangedDamage = row.getInt("add_bow_dmg");
                ability.water = row.getInt("add_water");
                ability.wind = row.getInt("add_wind");
                ability.earth = row.getInt("add_earth");
                return ability;
            }
        }
    }

    private static void addUniqueCards(int[] cards, Set<Integer> destination) {
        if (cards == null) return;
        for (int cardId : cards) {
            if (cardId > 1) destination.add(cardId);
        }
    }

    private static Ability standardAbility(
            Connection connection, String database, String table,
            String where, boolean readFire, int... parameters) throws SQLException {
        return standardAbility(connection, database, table, where,
                readFire, false, parameters);
    }

    private static Ability standardAbility(
            Connection connection, String database, String table,
            String where, boolean readFire, boolean readWeightReduction,
            int... parameters) throws SQLException {
        String fire = readFire ? "add_fire" : "0";
        String weight = readWeightReduction ? "add_weight_reduction" : "0";
        String sql = "SELECT add_str,add_dex,add_int,add_wis,add_con,"
                + optionalPhysicalColumn(connection, database, table, "add_cha") + ","
                + optionalPhysicalColumn(connection, database, table, "add_hp") + ","
                + optionalPhysicalColumn(connection, database, table, "add_mp") + ","
                + optionalPhysicalColumn(connection, database, table, "add_potion_recovery") + ","
                + "add_sp,add_mr,add_hpr,add_mpr,add_magic_dmg,"
                + optionalPhysicalColumn(connection, database, table, "add_magic_hit") + ","
                + optionalPhysicalColumn(connection, database, table, "regist_blind") + ","
                + optionalPhysicalColumn(connection, database, table, "regist_sleep") + ","
                + optionalPhysicalColumn(connection, database, table, "regist_freeze") + ","
                + optionalPhysicalColumn(connection, database, table, "regist_stone") + ","
                + optionalPhysicalColumn(connection, database, table, "regist_sustain") + ","
                + optionalPhysicalColumn(connection, database, table, "regist_stun") + ","
                + optionalPhysicalColumn(connection, database, table, "add_stun_success") + ","
                + optionalPhysicalColumn(connection, database, table, "add_hit") + ","
                + optionalPhysicalColumn(connection, database, table, "add_dmg") + ","
                + optionalPhysicalColumn(connection, database, table, "add_bow_hit") + ","
                + optionalPhysicalColumn(connection, database, table, "add_bow_dmg") + ","
                + optionalPhysicalColumn(connection, database, table, "add_pierce") + ","
                + optionalPhysicalColumn(connection, database, table, "add_ac") + ","
                + optionalPhysicalColumn(connection, database, table, "add_er") + ","
                + optionalPhysicalColumn(connection, database, table, "add_reduction") + ","
                + optionalPhysicalColumn(connection, database, table, "add_melee_redux") + ","
                + optionalPhysicalColumn(connection, database, table, "add_bow_redux") + ","
                + optionalPhysicalColumn(connection, database, table, "add_ignore_reduction") + ","
                + optionalPhysicalColumn(connection, database, table, "add_pvp_redux") + ","
                + optionalPhysicalColumn(connection, database, table, "add_water") + ","
                + optionalPhysicalColumn(connection, database, table, "add_wind") + ","
                + optionalPhysicalColumn(connection, database, table, "add_earth") + ","
                + fire + " AS add_fire," + weight + " AS add_weight_reduction FROM "
                + qualified(database, table)
                + " WHERE " + where + " LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) {
                statement.setInt(index + 1, parameters[index]);
            }
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) return null;
                Ability ability = ability(row, "add_magic_dmg", "add_fire", null,
                        "add_weight_reduction");
                ability.charisma = row.getInt("add_cha");
                ability.maximumHp = row.getInt("add_hp");
                ability.maximumMp = row.getInt("add_mp");
                ability.potionRecovery = row.getInt("add_potion_recovery");
                ability.magicHit = row.getInt("add_magic_hit");
                ability.blindResistance = row.getInt("regist_blind");
                ability.sleepResistance = row.getInt("regist_sleep");
                ability.freezeResistance = row.getInt("regist_freeze");
                ability.stoneResistance = row.getInt("regist_stone");
                ability.sustainResistance = row.getInt("regist_sustain");
                ability.stunResistance = row.getInt("regist_stun");
                ability.stunSuccess = row.getInt("add_stun_success");
                ability.dexterity = row.getInt("add_dex");
                readPhysicalAbility(row, ability, "add_hit", "add_dmg",
                        "add_bow_hit", "add_bow_dmg");
                ability.pierce = row.getInt("add_pierce");
                // Retained item extension loaders convert positive add_ac to
                // negative AC for armor rows and single inlay-card rows only.
                if (table.startsWith("裝備_armor_")
                        || "裝備_特殊_卡片_能力".equals(table)) {
                    ability.armorClass = -row.getInt("add_ac");
                }
                ability.reduction = row.getInt("add_reduction");
                ability.evasionRate = row.getInt("add_er");
                ability.meleeReduction = row.getInt("add_melee_redux");
                ability.rangedReduction = row.getInt("add_bow_redux");
                ability.ignoreReduction = row.getInt("add_ignore_reduction");
                ability.pvpReduction = row.getInt("add_pvp_redux");
                ability.water = row.getInt("add_water");
                ability.wind = row.getInt("add_wind");
                ability.earth = row.getInt("add_earth");
                return ability;
            }
        }
    }

    private static Ability stoneAbility(Connection connection, String database,
                                        int itemId, int tier) throws SQLException {
        String sql = "SELECT add_str,add_dex,add_int,add_wis,add_con,add_hp,add_mp,add_sp,add_mr FROM "
                + qualified(database, "裝備_特殊_魔石_魔法")
                + " WHERE item_id=? AND tier=? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, itemId);
            statement.setInt(2, tier);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) return null;
                Ability ability = new Ability(row.getInt("add_int"), row.getInt("add_wis"),
                        row.getInt("add_con"),
                        row.getInt("add_sp"), row.getInt("add_mr"), 0, 0, 0, 0,
                        row.getInt("add_str"), 0);
                ability.dexterity = row.getInt("add_dex");
                ability.maximumHp = row.getInt("add_hp");
                ability.maximumMp = row.getInt("add_mp");
                return ability;
            }
        }
    }

    private static Ability setAbility(Connection connection, String database,
                                      int setId, int pieceCount) throws SQLException {
        String sql = "SELECT add_str,add_dex,add_int,add_wis,add_con,add_cha,add_hp,add_mp,m_def,add_hpr,add_mpr,add_potion_recovery,magic_dmg_modifier,add_magic_hit,regist_blind,regist_sleep,regist_freeze,regist_stone,regist_sustain,regist_stun,add_stun_success,"
                + "defense_fire,defense_water,defense_wind,defense_earth,"
                + "add_hitup,add_dmgup,add_bow_hitup,add_bow_dmgup,add_pierce,"
                + "damage_reduction,weight_reduction,ac,add_er,add_ignore_reduction,add_pvp_redux FROM "
                + qualified(database, "裝備_特殊_套裝_能力")
                + " WHERE set_id=? AND piece_count<=? ORDER BY piece_count DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, setId);
            statement.setInt(2, pieceCount);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) return null;
                // Set WIS and SP remain in _setAbility, but updateSetBonus does
                // not reset base MR and getTargetSp does not read set SP.
                Ability ability = new Ability(row.getInt("add_int"), row.getInt("add_wis"), row.getInt("add_con"), 0,
                        row.getInt("m_def"), row.getInt("magic_dmg_modifier"),
                        row.getInt("defense_fire"), row.getInt("damage_reduction"),
                        row.getInt("add_hpr"), row.getInt("add_str"),
                        row.getInt("weight_reduction"));
                ability.mpRegen = row.getInt("add_mpr");
                ability.charisma = row.getInt("add_cha");
                ability.maximumHp = row.getInt("add_hp");
                ability.maximumMp = row.getInt("add_mp");
                ability.potionRecovery = row.getInt("add_potion_recovery");
                ability.magicHit = row.getInt("add_magic_hit");
                ability.blindResistance = row.getInt("regist_blind");
                ability.sleepResistance = row.getInt("regist_sleep");
                ability.freezeResistance = row.getInt("regist_freeze");
                ability.stoneResistance = row.getInt("regist_stone");
                ability.sustainResistance = row.getInt("regist_sustain");
                ability.stunResistance = row.getInt("regist_stun");
                ability.stunSuccess = row.getInt("add_stun_success");
                ability.dexterity = row.getInt("add_dex");
                readPhysicalAbility(row, ability, "add_hitup", "add_dmgup",
                        "add_bow_hitup", "add_bow_dmgup");
                ability.pierce = row.getInt("add_pierce");
                ability.armorClass = row.getInt("ac");
                ability.evasionRate = row.getInt("add_er");
                ability.ignoreReduction = row.getInt("add_ignore_reduction");
                ability.pvpReduction = row.getInt("add_pvp_redux");
                ability.water = row.getInt("defense_water");
                ability.wind = row.getInt("defense_wind");
                ability.earth = row.getInt("defense_earth");
                return ability;
            }
        }
    }

    private static int addCardCombos(Connection connection, String database,
                                     Set<Integer> cards, Ability destination)
            throws SQLException {
        if (cards.isEmpty()) return 0;
        String sql = "SELECT require_cards,add_str,add_dex,add_int,add_wis,add_con,add_cha,add_hp,add_mp,add_mr,add_hpr,add_mpr,add_potion_recovery,add_magic_dmg,add_magic_hit,regist_blind,regist_sleep,regist_freeze,regist_stone,regist_sustain,regist_stun,add_stun_success,"
                + "add_fire,add_water,add_wind,add_earth,"
                + "add_hit,add_dmg,add_bow_hit,add_bow_dmg,add_pierce,"
                + "add_ac,add_er,add_melee_redux,add_bow_redux,add_ignore_reduction,"
                + "add_reduction,add_weight_reduction,add_pvp_redux FROM "
                + qualified(database, "裝備_特殊_卡片_組合")
                + " ORDER BY combo_id";
        int count = 0;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            while (row.next()) {
                List<Integer> required = parseRequiredCards(row.getString("require_cards"));
                if (required.isEmpty() || !cards.containsAll(required)) continue;
                Ability ability = new Ability(row.getInt("add_int"), row.getInt("add_wis"),
                        row.getInt("add_con"), 0,
                        row.getInt("add_mr"), row.getInt("add_magic_dmg"),
                        row.getInt("add_fire"), row.getInt("add_reduction"),
                        row.getInt("add_hpr"), row.getInt("add_str"),
                        row.getInt("add_weight_reduction"));
                ability.mpRegen = row.getInt("add_mpr");
                ability.charisma = row.getInt("add_cha");
                ability.maximumHp = row.getInt("add_hp");
                ability.maximumMp = row.getInt("add_mp");
                ability.potionRecovery = row.getInt("add_potion_recovery");
                ability.magicHit = row.getInt("add_magic_hit");
                ability.blindResistance = row.getInt("regist_blind");
                ability.sleepResistance = row.getInt("regist_sleep");
                ability.freezeResistance = row.getInt("regist_freeze");
                ability.stoneResistance = row.getInt("regist_stone");
                ability.sustainResistance = row.getInt("regist_sustain");
                ability.stunResistance = row.getInt("regist_stun");
                ability.stunSuccess = row.getInt("add_stun_success");
                ability.dexterity = row.getInt("add_dex");
                readPhysicalAbility(row, ability, "add_hit", "add_dmg",
                        "add_bow_hit", "add_bow_dmg");
                ability.pierce = row.getInt("add_pierce");
                // L1InlayCombo stores -add_ac into the set aggregate. The
                // retained L1PcInstance later subtracts that aggregate again.
                ability.armorClass = -row.getInt("add_ac");
                ability.evasionRate = row.getInt("add_er");
                ability.meleeReduction = row.getInt("add_melee_redux");
                ability.rangedReduction = row.getInt("add_bow_redux");
                ability.ignoreReduction = row.getInt("add_ignore_reduction");
                ability.pvpReduction = row.getInt("add_pvp_redux");
                ability.water = row.getInt("add_water");
                ability.wind = row.getInt("add_wind");
                ability.earth = row.getInt("add_earth");
                destination.add(ability);
                count++;
            }
        }
        return count;
    }

    private static Ability ability(ResultSet row, String magicColumn,
                                   String fireColumn, String reductionColumn,
                                   String weightReductionColumn)
            throws SQLException {
        Ability ability = new Ability(row.getInt("add_int"), row.getInt("add_wis"),
                row.getInt("add_con"),
                row.getInt("add_sp"), row.getInt("add_mr"),
                magicColumn == null ? 0 : row.getInt(magicColumn),
                fireColumn == null ? 0 : row.getInt(fireColumn),
                reductionColumn == null ? 0 : row.getInt(reductionColumn),
                row.getInt("add_hpr"), row.getInt("add_str"),
                weightReductionColumn == null ? 0 : row.getInt(weightReductionColumn));
        ability.mpRegen = row.getInt("add_mpr");
        return ability;
    }

    private static void readPhysicalAbility(
            ResultSet row, Ability ability,
            String meleeHitColumn, String meleeDamageColumn,
            String rangedHitColumn, String rangedDamageColumn) throws SQLException {
        ability.meleeHit = row.getInt(meleeHitColumn);
        ability.meleeDamage = row.getInt(meleeDamageColumn);
        ability.rangedHit = row.getInt(rangedHitColumn);
        ability.rangedDamage = row.getInt(rangedDamageColumn);
    }

    private static int[] parseSetIds(String value) {
        if (value == null || value.trim().isEmpty() || "0".equals(value)) return new int[0];
        try {
            String[] parts = value.split(",");
            int[] result = new int[parts.length];
            for (int index = 0; index < parts.length; index++) {
                result[index] = Integer.parseInt(parts[index].trim());
            }
            return result;
        } catch (RuntimeException ignored) {
            return new int[0];
        }
    }

    private static List<Integer> parseRequiredCards(String value) {
        if (value == null || value.trim().isEmpty()) return List.of();
        List<Integer> result = new ArrayList<>();
        for (String part : value.split(",")) {
            try {
                result.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
                // Exact L1InlayCombo behavior: malformed entries are skipped.
            }
        }
        return result;
    }

    static String qualified(String database, String table) {
        if (database == null || !database.matches("[A-Za-z0-9_]+")
                || !RESOURCE_TABLES.contains(table)) {
            throw new IllegalArgumentException("unsupported equipment resource identifier");
        }
        return database + ".`" + table + "`";
    }

    static Set<String> requiredResourceTables() {
        return REQUIRED_RESOURCE_TABLES;
    }

    static int signedByte(int value) {
        return (byte) value;
    }

    static int appliedAbilityModifier(long value) {
        return NumericBreakthrough.clampAbilityModifier(value);
    }

    static int addAppliedAbilityModifier(int current, int delta) {
        return appliedAbilityModifier((long) current + delta);
    }

    static int addExtendedValue(int current, int delta) {
        return NumericBreakthrough.clampExtendedValue((long) current + delta);
    }

    private record ItemRow(
            long objectId, int itemId, int enchantLevel, int durability,
            int bless, int greater,
            int attributeKind, int attributeLevel,
            int magicStoneItemId, int magicStoneLevel, Timestamp magicStoneTime,
            int[] cards, Ability darkAbility) {}

    private record Template(
            boolean armor, int type, String setIds, String nameId, int safeEnchant,
            int material, int blessing,
            int damageReduction, int weightReduction,
            int damageSmall, int damageLarge, int range, int inventoryGfx,
            int meleeHitModifier, int meleeDamageModifier,
            int rangedHitModifier, int rangedDamageModifier,
            int doubleDamageChance, double doubleDamageMultiplier,
            int maximumDamageChance,
            boolean doubleDamageEffect, boolean maximumDamageEffect,
            Ability baseAbility) {}

    private static final class Ability {
        private int intelligence;
        private int wisdom;
        private int constitution;
        private int strength;
        private int dexterity;
        private int charisma;
        private int maximumHp;
        private int maximumMp;
        private int potionRecovery;
        private int sp;
        private int mr;
        private int magicDamage;
        private int magicHit;
        private int blindResistance;
        private int sleepResistance;
        private int freezeResistance;
        private int stoneResistance;
        private int sustainResistance;
        private int stunResistance;
        private int stunSuccess;
        private int fire;
        private int water;
        private int wind;
        private int earth;
        private int reduction;
        private int hpRegen;
        private int mpRegen;
        private int weightReduction;
        private int meleeHit;
        private int meleeDamage;
        private int rangedHit;
        private int rangedDamage;
        private int pierce;
        private int armorClass;
        private int evasionRate;
        private int meleeReduction;
        private int rangedReduction;
        private int ignoreReduction;
        private int pvpReduction;

        private Ability() {}

        private Ability(int intelligence, int wisdom, int sp, int mr,
                        int magicDamage, int fire, int reduction) {
            this(intelligence, wisdom, 0, sp, mr, magicDamage, fire, reduction, 0);
        }

        private Ability(int intelligence, int wisdom, int sp, int mr,
                        int magicDamage, int fire, int reduction, int hpRegen) {
            this(intelligence, wisdom, 0, sp, mr, magicDamage, fire, reduction, hpRegen);
        }

        private Ability(int intelligence, int wisdom, int constitution, int sp, int mr,
                        int magicDamage, int fire, int reduction, int hpRegen) {
            this(intelligence, wisdom, constitution, sp, mr, magicDamage, fire,
                    reduction, hpRegen, 0, 0);
        }

        private Ability(int intelligence, int wisdom, int constitution, int sp, int mr,
                        int magicDamage, int fire, int reduction, int hpRegen,
                        int strength, int weightReduction) {
            this.intelligence = intelligence;
            this.wisdom = wisdom;
            this.constitution = constitution;
            this.strength = strength;
            this.sp = sp;
            this.mr = mr;
            this.magicDamage = magicDamage;
            this.fire = fire;
            this.reduction = reduction;
            this.hpRegen = hpRegen;
            this.weightReduction = weightReduction;
        }

        private void add(Ability other) {
            if (other == null) return;
            intelligence += other.intelligence;
            wisdom += other.wisdom;
            constitution += other.constitution;
            strength += other.strength;
            dexterity += other.dexterity;
            charisma += other.charisma;
            maximumHp = addExtendedValue(maximumHp, other.maximumHp);
            maximumMp = addExtendedValue(maximumMp, other.maximumMp);
            potionRecovery += other.potionRecovery;
            sp = addExtendedValue(sp, other.sp);
            mr = addExtendedValue(mr, other.mr);
            magicDamage += other.magicDamage;
            magicHit += other.magicHit;
            blindResistance += other.blindResistance;
            sleepResistance += other.sleepResistance;
            freezeResistance += other.freezeResistance;
            stoneResistance += other.stoneResistance;
            sustainResistance += other.sustainResistance;
            stunResistance += other.stunResistance;
            stunSuccess += other.stunSuccess;
            fire = addExtendedValue(fire, other.fire);
            water = addExtendedValue(water, other.water);
            wind = addExtendedValue(wind, other.wind);
            earth = addExtendedValue(earth, other.earth);
            reduction += other.reduction;
            hpRegen += other.hpRegen;
            mpRegen += other.mpRegen;
            weightReduction += other.weightReduction;
            meleeHit += other.meleeHit;
            meleeDamage += other.meleeDamage;
            rangedHit += other.rangedHit;
            rangedDamage += other.rangedDamage;
            pierce += other.pierce;
            armorClass = addExtendedValue(armorClass, other.armorClass);
            evasionRate += other.evasionRate;
            meleeReduction += other.meleeReduction;
            rangedReduction += other.rangedReduction;
            ignoreReduction += other.ignoreReduction;
            pvpReduction += other.pvpReduction;
        }

        private void addApplied(Ability other) {
            if (other == null) return;
            intelligence = addAppliedAbilityModifier(intelligence, other.intelligence);
            wisdom = addAppliedAbilityModifier(wisdom, other.wisdom);
            constitution = addAppliedAbilityModifier(constitution, other.constitution);
            strength = addAppliedAbilityModifier(strength, other.strength);
            dexterity = addAppliedAbilityModifier(dexterity, other.dexterity);
            charisma = addAppliedAbilityModifier(charisma, other.charisma);
            maximumHp = addExtendedValue(maximumHp, other.maximumHp);
            maximumMp = addExtendedValue(maximumMp, other.maximumMp);
            potionRecovery += other.potionRecovery;
            sp = addExtendedValue(sp, other.sp);
            mr = addExtendedValue(mr, other.mr);
            magicDamage += other.magicDamage;
            magicHit += other.magicHit;
            blindResistance += other.blindResistance;
            sleepResistance += other.sleepResistance;
            freezeResistance += other.freezeResistance;
            stoneResistance += other.stoneResistance;
            sustainResistance += other.sustainResistance;
            stunResistance += other.stunResistance;
            stunSuccess += other.stunSuccess;
            fire = addExtendedValue(fire, other.fire);
            water = addExtendedValue(water, other.water);
            wind = addExtendedValue(wind, other.wind);
            earth = addExtendedValue(earth, other.earth);
            reduction += other.reduction;
            hpRegen += other.hpRegen;
            mpRegen += other.mpRegen;
            weightReduction += other.weightReduction;
            meleeHit += other.meleeHit;
            meleeDamage += other.meleeDamage;
            rangedHit += other.rangedHit;
            rangedDamage += other.rangedDamage;
            pierce += other.pierce;
            armorClass = addExtendedValue(armorClass, other.armorClass);
            evasionRate += other.evasionRate;
            meleeReduction += other.meleeReduction;
            rangedReduction += other.rangedReduction;
            ignoreReduction += other.ignoreReduction;
            pvpReduction += other.pvpReduction;
        }
    }
}
