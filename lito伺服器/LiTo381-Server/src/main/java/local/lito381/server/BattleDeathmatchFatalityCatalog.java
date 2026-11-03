package local.lito381.server;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * Native Fatality floor symbols for Death Match map 508.
 *
 * <p>The thirty trigger cells are derived mechanically from Layer4 groups
 * 70..99 in the selected client's {@code LiTo/LTmap/508/80008000.s32}; they
 * are not copied from the unrelated classic-server map-id 4310 row.  The
 * contemporary 2005 LWC rules prove that entering a symbol chooses magic
 * independently of the symbol artwork.  They name the families Haste, Heal,
 * Cancellation, Fire Storm, Frozen Cloud and Disintegrate.  A 2003 Lineage
 * Compendium page explicitly marked as translated from lineage.co.kr says
 * {@code various Heals}; the retained 3.80 skill table contains the contiguous
 * single-target Heal family 1/19/35/57 with gfx 11722..11725.  Neither source
 * preserves category weights, Heal-subtype weights or the re-arm interval.
 * Runtime therefore keeps a reviewable two-stage compatibility policy: six
 * equal named families, then four equal retained Heal variants when the Heal
 * family is selected.  Those probabilities remain provisional.</p>
 */
final class BattleDeathmatchFatalityCatalog {
    static final int MAP_ID = 508;

    /** Selected 3.80 skill row 22 and TW13081901 sprite text agree on this. */
    static final int FROZEN_CLOUD_SKILL_ID = 22;
    static final int FROZEN_CLOUD_EFFECT_GFX = 1_804;
    static final int FROZEN_CLOUD_AREA_RADIUS = 3;
    static final int FROZEN_CLOUD_DAMAGE_VALUE = 0;
    static final int FROZEN_CLOUD_DAMAGE_DIE = 2;
    static final int FROZEN_CLOUD_DAMAGE_DICE = 2;

    enum Family {
        HASTE,
        HEAL,
        CANCELLATION,
        FIRE_STORM,
        FROZEN_CLOUD,
        DISINTEGRATE
    }

    enum Effect {
        HASTE(Family.HASTE, BattleSpellCatalog.HASTE, 755, "Haste"),
        BASIC_HEAL(Family.HEAL, BattleSpellCatalog.BASIC_HEAL, 11_722,
                "Basic Heal"),
        EXTRA_HEAL(Family.HEAL, BattleSpellCatalog.EXTRA_HEAL, 11_723,
                "Extra Heal"),
        GREATER_HEAL(Family.HEAL, BattleSpellCatalog.GREATER_HEAL, 11_724,
                "Greater Heal"),
        FULL_HEAL(Family.HEAL, BattleSpellCatalog.FULL_HEAL, 11_725,
                "Full Heal"),
        CANCELLATION(Family.CANCELLATION, BattleSpellCatalog.CANCELLATION, 870,
                "Cancellation"),
        FIRE_STORM(Family.FIRE_STORM, BattleSpellCatalog.FIRE_STORM, 1_819,
                "Fire Storm"),
        FROZEN_CLOUD(Family.FROZEN_CLOUD, FROZEN_CLOUD_SKILL_ID,
                FROZEN_CLOUD_EFFECT_GFX, "Frozen Cloud"),
        DISINTEGRATE(Family.DISINTEGRATE, BattleSpellCatalog.DISINTEGRATE,
                11_746, "Disintegrate");

        private final Family family;
        private final int skillId;
        private final int effectGfx;
        private final String label;

        Effect(Family family, int skillId, int effectGfx, String label) {
            this.family = family;
            this.skillId = skillId;
            this.effectGfx = effectGfx;
            this.label = label;
        }

        Family family() { return family; }
        int skillId() { return skillId; }
        int effectGfx() { return effectGfx; }
        String label() { return label; }
    }

    record Trigger(int x, int y, Effect effect) {}

    private static final List<ArenaMapCatalog.Point> SYMBOLS = List.of(
            point(32_803, 32_794), point(32_801, 32_794), point(32_799, 32_794),
            point(32_803, 32_796), point(32_803, 32_798), point(32_801, 32_796),
            point(32_790, 32_794), point(32_792, 32_794), point(32_794, 32_794),
            point(32_790, 32_796), point(32_790, 32_798), point(32_792, 32_796),
            point(32_803, 32_807), point(32_803, 32_805), point(32_801, 32_807),
            point(32_799, 32_807), point(32_803, 32_803), point(32_801, 32_805),
            point(32_790, 32_807), point(32_792, 32_807), point(32_790, 32_805),
            point(32_792, 32_805), point(32_794, 32_807), point(32_790, 32_803),
            point(32_795, 32_799), point(32_798, 32_802), point(32_795, 32_802),
            point(32_798, 32_799), point(32_796, 32_800), point(32_797, 32_801));

    private static final Set<ArenaMapCatalog.Point> SYMBOL_SET =
            Set.copyOf(new LinkedHashSet<>(SYMBOLS));
    private static final List<Family> PROVISIONAL_EQUAL_FAMILIES = List.of(
            Family.HASTE,
            Family.HEAL,
            Family.CANCELLATION,
            Family.FIRE_STORM,
            Family.FROZEN_CLOUD,
            Family.DISINTEGRATE);
    private static final List<Effect> PROVISIONAL_EQUAL_HEALS = List.of(
            Effect.BASIC_HEAL,
            Effect.EXTRA_HEAL,
            Effect.GREATER_HEAL,
            Effect.FULL_HEAL);
    /** Deterministic test surface; production does not weight these nine equally. */
    private static final List<Effect> FIXTURE_EFFECTS = List.of(
            Effect.HASTE,
            Effect.BASIC_HEAL,
            Effect.EXTRA_HEAL,
            Effect.GREATER_HEAL,
            Effect.FULL_HEAL,
            Effect.CANCELLATION,
            Effect.FIRE_STORM,
            Effect.FROZEN_CLOUD,
            Effect.DISINTEGRATE);

    private BattleDeathmatchFatalityCatalog() {}

    static List<ArenaMapCatalog.Point> symbols() {
        return SYMBOLS;
    }

    static boolean isSymbol(int x, int y) {
        return SYMBOL_SET.contains(point(x, y));
    }

    static List<Family> provisionalEqualFamilies() {
        return PROVISIONAL_EQUAL_FAMILIES;
    }

    static List<Effect> provisionalEqualHeals() {
        return PROVISIONAL_EQUAL_HEALS;
    }

    static List<Effect> fixtureEffects() {
        return FIXTURE_EFFECTS;
    }

    static Effect selectProvisionalEffect(int familySlot, int healSlot) {
        Family family = PROVISIONAL_EQUAL_FAMILIES.get(
                Math.floorMod(familySlot, PROVISIONAL_EQUAL_FAMILIES.size()));
        return switch (family) {
            case HASTE -> Effect.HASTE;
            case HEAL -> PROVISIONAL_EQUAL_HEALS.get(
                    Math.floorMod(healSlot, PROVISIONAL_EQUAL_HEALS.size()));
            case CANCELLATION -> Effect.CANCELLATION;
            case FIRE_STORM -> Effect.FIRE_STORM;
            case FROZEN_CLOUD -> Effect.FROZEN_CLOUD;
            case DISINTEGRATE -> Effect.DISINTEGRATE;
        };
    }

    static Effect selectFixtureEffect(int effectSlot) {
        return FIXTURE_EFFECTS.get(
                Math.floorMod(effectSlot, FIXTURE_EFFECTS.size()));
    }

    static Trigger entered(BattleRules.Mode mode, int mapId,
                           int fromX, int fromY, int toX, int toY,
                           Effect effect) {
        if (mode != BattleRules.Mode.DEATHMATCH || mapId != MAP_ID
                || fromX == toX && fromY == toY || !isSymbol(toX, toY)) {
            return null;
        }
        if (effect == null) throw new IllegalArgumentException("missing Fatality effect");
        return new Trigger(toX, toY, effect);
    }

    static int rollNeutralHeal(Effect effect, IntUnaryOperator dieRoller) {
        BattleSpellCatalog.HealingSpell spell =
                BattleSpellCatalog.healingSpell(effect.skillId());
        if (spell == null || dieRoller == null) {
            throw new IllegalArgumentException("Fatality effect is not a heal");
        }
        int result = 0;
        for (int index = 0; index < spell.diceCount(11); index++) {
            result = Math.addExact(result, checkedRoll(dieRoller, spell.dieSides()));
        }
        return result;
    }

    static int rollRawDamage(Effect effect, IntUnaryOperator dieRoller) {
        if (dieRoller == null) {
            throw new IllegalArgumentException("missing Fatality die roller");
        }
        int base;
        int dice;
        int die;
        if (effect == Effect.FROZEN_CLOUD) {
            base = FROZEN_CLOUD_DAMAGE_VALUE;
            dice = FROZEN_CLOUD_DAMAGE_DICE;
            die = FROZEN_CLOUD_DAMAGE_DIE;
        } else if (effect == Effect.FIRE_STORM) {
            BattleSpellCatalog.AreaAttackSpell spell =
                    BattleSpellCatalog.areaAttackSpell(effect.skillId());
            base = spell.baseDamage();
            dice = spell.diceCount();
            die = spell.dieSides();
        } else if (effect == Effect.DISINTEGRATE) {
            BattleSpellCatalog.AttackSpell spell =
                    BattleSpellCatalog.attackSpell(effect.skillId());
            base = spell.baseDamage();
            dice = spell.diceCount();
            die = spell.dieSides();
        } else {
            throw new IllegalArgumentException(
                    "Fatality effect has no damage metadata: " + effect);
        }
        int damage = base;
        for (int index = 0; index < dice; index++) {
            damage = Math.addExact(damage, checkedRoll(dieRoller, die));
        }
        return damage;
    }

    private static int checkedRoll(IntUnaryOperator dieRoller, int sides) {
        int roll = dieRoller.applyAsInt(sides);
        if (roll < 1 || roll > sides) {
            throw new IllegalArgumentException("invalid Fatality die roll");
        }
        return roll;
    }

    private static ArenaMapCatalog.Point point(int x, int y) {
        return new ArenaMapCatalog.Point(x, y);
    }
}
