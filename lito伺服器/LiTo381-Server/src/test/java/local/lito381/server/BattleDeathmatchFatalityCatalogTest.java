package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

final class BattleDeathmatchFatalityCatalogTest {
    @Test
    void layer4AuditCoordinatesAreExactUniqueNativeStandingCells() {
        List<ArenaMapCatalog.Point> symbols =
                BattleDeathmatchFatalityCatalog.symbols();
        assertEquals(30, symbols.size());
        assertEquals(30, new LinkedHashSet<>(symbols).size());
        assertEquals(Set.of(
                point(32_803, 32_794), point(32_801, 32_794), point(32_799, 32_794),
                point(32_803, 32_796), point(32_803, 32_798), point(32_801, 32_796),
                point(32_790, 32_794), point(32_792, 32_794), point(32_794, 32_794),
                point(32_790, 32_796), point(32_790, 32_798), point(32_792, 32_796),
                point(32_803, 32_807), point(32_803, 32_805), point(32_801, 32_807),
                point(32_799, 32_807), point(32_803, 32_803), point(32_801, 32_805),
                point(32_790, 32_807), point(32_792, 32_807), point(32_790, 32_805),
                point(32_792, 32_805), point(32_794, 32_807), point(32_790, 32_803),
                point(32_795, 32_799), point(32_798, 32_802), point(32_795, 32_802),
                point(32_798, 32_799), point(32_796, 32_800), point(32_797, 32_801)),
                Set.copyOf(symbols));
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(
                BattleDeathmatchFatalityCatalog.MAP_ID);
        for (ArenaMapCatalog.Point symbol : symbols) {
            assertTrue(arena.nativeStandingFloor(symbol.x(), symbol.y()),
                    () -> "non-standing Fatality symbol " + symbol);
        }
        assertFalse(BattleDeathmatchFatalityCatalog.isSymbol(32_797, 32_800));
    }

    @Test
    void onlyAnAcceptedDeathmatchMap508EntryTriggers() {
        BattleDeathmatchFatalityCatalog.Trigger trigger =
                BattleDeathmatchFatalityCatalog.entered(
                        BattleRules.Mode.DEATHMATCH, 508,
                        32_802, 32_794, 32_803, 32_794,
                        BattleDeathmatchFatalityCatalog.Effect.FROZEN_CLOUD);
        assertNotNull(trigger);
        assertEquals(32_803, trigger.x());
        assertEquals(32_794, trigger.y());
        assertEquals(BattleDeathmatchFatalityCatalog.Effect.FROZEN_CLOUD,
                trigger.effect());

        assertNull(BattleDeathmatchFatalityCatalog.entered(
                BattleRules.Mode.HONOR_MATCH, 508,
                32_802, 32_794, 32_803, 32_794,
                BattleDeathmatchFatalityCatalog.Effect.HASTE));
        assertNull(BattleDeathmatchFatalityCatalog.entered(
                BattleRules.Mode.DEATHMATCH, 510,
                32_802, 32_794, 32_803, 32_794,
                BattleDeathmatchFatalityCatalog.Effect.HASTE));
        assertNull(BattleDeathmatchFatalityCatalog.entered(
                BattleRules.Mode.DEATHMATCH, 508,
                32_803, 32_794, 32_803, 32_794,
                BattleDeathmatchFatalityCatalog.Effect.HASTE));
        assertNull(BattleDeathmatchFatalityCatalog.entered(
                BattleRules.Mode.DEATHMATCH, 508,
                32_802, 32_794, 32_802, 32_795,
                BattleDeathmatchFatalityCatalog.Effect.HASTE));
    }

    @Test
    void historicalEffectPoolAndFrozenCloudResourceStayExplicit() {
        assertEquals(List.of(
                BattleDeathmatchFatalityCatalog.Family.HASTE,
                BattleDeathmatchFatalityCatalog.Family.HEAL,
                BattleDeathmatchFatalityCatalog.Family.CANCELLATION,
                BattleDeathmatchFatalityCatalog.Family.FIRE_STORM,
                BattleDeathmatchFatalityCatalog.Family.FROZEN_CLOUD,
                BattleDeathmatchFatalityCatalog.Family.DISINTEGRATE),
                BattleDeathmatchFatalityCatalog.provisionalEqualFamilies());
        assertEquals(List.of(
                BattleDeathmatchFatalityCatalog.Effect.BASIC_HEAL,
                BattleDeathmatchFatalityCatalog.Effect.EXTRA_HEAL,
                BattleDeathmatchFatalityCatalog.Effect.GREATER_HEAL,
                BattleDeathmatchFatalityCatalog.Effect.FULL_HEAL),
                BattleDeathmatchFatalityCatalog.provisionalEqualHeals());
        assertEquals(9, BattleDeathmatchFatalityCatalog.fixtureEffects().size());
        assertEquals(22,
                BattleDeathmatchFatalityCatalog.FROZEN_CLOUD_SKILL_ID);
        assertEquals(1_804,
                BattleDeathmatchFatalityCatalog.FROZEN_CLOUD_EFFECT_GFX);
        assertEquals(3,
                BattleDeathmatchFatalityCatalog.FROZEN_CLOUD_AREA_RADIUS);
        assertEquals(2,
                BattleDeathmatchFatalityCatalog.FROZEN_CLOUD_DAMAGE_DIE);
        assertEquals(2,
                BattleDeathmatchFatalityCatalog.FROZEN_CLOUD_DAMAGE_DICE);
        assertEquals(BattleDeathmatchFatalityCatalog.Effect.HASTE,
                BattleDeathmatchFatalityCatalog.selectProvisionalEffect(0, 0));
        for (int index = 0; index < 4; index++) {
            assertEquals(BattleDeathmatchFatalityCatalog
                            .provisionalEqualHeals().get(index),
                    BattleDeathmatchFatalityCatalog
                            .selectProvisionalEffect(1, index));
        }
        assertEquals(BattleDeathmatchFatalityCatalog.Effect.DISINTEGRATE,
                BattleDeathmatchFatalityCatalog.selectProvisionalEffect(-1, 0));
        for (int index = 0; index < 9; index++) {
            assertEquals(BattleDeathmatchFatalityCatalog.fixtureEffects().get(index),
                    BattleDeathmatchFatalityCatalog.selectFixtureEffect(index));
        }
    }

    @Test
    void provisionalAmountsUseTheExactRetainedSpellDice() {
        assertEquals(2, BattleDeathmatchFatalityCatalog.rollNeutralHeal(
                BattleDeathmatchFatalityCatalog.Effect.BASIC_HEAL, ignored -> 1));
        assertEquals(8, BattleDeathmatchFatalityCatalog.rollNeutralHeal(
                BattleDeathmatchFatalityCatalog.Effect.BASIC_HEAL, sides -> sides));
        assertEquals(3, BattleDeathmatchFatalityCatalog.rollNeutralHeal(
                BattleDeathmatchFatalityCatalog.Effect.EXTRA_HEAL, ignored -> 1));
        assertEquals(24, BattleDeathmatchFatalityCatalog.rollNeutralHeal(
                BattleDeathmatchFatalityCatalog.Effect.EXTRA_HEAL, sides -> sides));
        assertEquals(8, BattleDeathmatchFatalityCatalog.rollNeutralHeal(
                BattleDeathmatchFatalityCatalog.Effect.GREATER_HEAL, ignored -> 1));
        assertEquals(64, BattleDeathmatchFatalityCatalog.rollNeutralHeal(
                BattleDeathmatchFatalityCatalog.Effect.GREATER_HEAL, sides -> sides));
        assertEquals(15, BattleDeathmatchFatalityCatalog.rollNeutralHeal(
                BattleDeathmatchFatalityCatalog.Effect.FULL_HEAL, ignored -> 1));
        assertEquals(255, BattleDeathmatchFatalityCatalog.rollNeutralHeal(
                BattleDeathmatchFatalityCatalog.Effect.FULL_HEAL, sides -> sides));
        assertEquals(32, BattleDeathmatchFatalityCatalog.rollRawDamage(
                BattleDeathmatchFatalityCatalog.Effect.FIRE_STORM, ignored -> 1));
        assertEquals(144, BattleDeathmatchFatalityCatalog.rollRawDamage(
                BattleDeathmatchFatalityCatalog.Effect.FIRE_STORM, sides -> sides));
        assertEquals(2, BattleDeathmatchFatalityCatalog.rollRawDamage(
                BattleDeathmatchFatalityCatalog.Effect.FROZEN_CLOUD, ignored -> 1));
        assertEquals(4, BattleDeathmatchFatalityCatalog.rollRawDamage(
                BattleDeathmatchFatalityCatalog.Effect.FROZEN_CLOUD, sides -> sides));
        assertEquals(198, BattleDeathmatchFatalityCatalog.rollRawDamage(
                BattleDeathmatchFatalityCatalog.Effect.DISINTEGRATE, ignored -> 1));
        assertEquals(310, BattleDeathmatchFatalityCatalog.rollRawDamage(
                BattleDeathmatchFatalityCatalog.Effect.DISINTEGRATE, sides -> sides));
        assertThrows(IllegalArgumentException.class,
                () -> BattleDeathmatchFatalityCatalog.rollRawDamage(
                        BattleDeathmatchFatalityCatalog.Effect.HASTE, ignored -> 1));
        assertThrows(IllegalArgumentException.class,
                () -> BattleDeathmatchFatalityCatalog.rollNeutralHeal(
                        BattleDeathmatchFatalityCatalog.Effect.HASTE, ignored -> 1));
        assertThrows(IllegalArgumentException.class,
                () -> BattleDeathmatchFatalityCatalog.rollRawDamage(
                        BattleDeathmatchFatalityCatalog.Effect.FROZEN_CLOUD,
                        ignored -> 0));
    }

    private static ArenaMapCatalog.Point point(int x, int y) {
        return new ArenaMapCatalog.Point(x, y);
    }
}
