package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

final class BattleMortalBodyTest {
    private static final BattleSpellCatalog.MortalBodySpell SPELL =
            BattleSpellCatalog.mortalBodySpell(BattleSpellCatalog.MORTAL_BODY);

    @Test void selectedCastMetadataStaysExact() {
        assertEquals(191, SPELL.skillId());
        assertEquals(23, SPELL.row());
        assertEquals(6, SPELL.column());
        assertEquals(24, SPELL.level());
        assertEquals(6, SPELL.number());
        assertEquals(19, SPELL.actionId());
        assertEquals(6519, SPELL.effectGfx());
        assertEquals(0, SPELL.mpCost());
        assertEquals(50, SPELL.hpCost());
        assertEquals(1_000, SPELL.reuseDelayMillis());
        assertEquals(30, SPELL.clientCastClockMillis());
        assertEquals(300, SPELL.durationSeconds());
        assertEquals(64, SPELL.nativeId());
        assertEquals(20, SPELL.baseReflectionDamage());
        assertEquals(0, SPELL.bossModifier());
        assertEquals(true, SPELL.counterMagicBypass());
        assertEquals(false, SPELL.cancellationImmune());
        assertEquals("mortal-body", SPELL.key());
    }

    @Test void retainedProbabilityUsesOnlyTheThreeLevelBands() {
        assertEquals(35, SPELL.triggerProbability(52, 51));
        assertEquals(25, SPELL.triggerProbability(52, 52));
        assertEquals(15, SPELL.triggerProbability(51, 52));
    }

    @Test void leaseRefreshesExpiresAndCancellationClearsIt() {
        BattleAbilityStatus status = new BattleAbilityStatus();
        long start = TimeUnit.SECONDS.toNanos(10);
        status.activateMortalBody(start, 300);
        assertEquals(new BattleAbilityStatus.MortalBody(300),
                status.mortalBody(start));
        status.activateMortalBody(start + TimeUnit.SECONDS.toNanos(20), 300);
        assertEquals(new BattleAbilityStatus.MortalBody(300),
                status.mortalBody(start + TimeUnit.SECONDS.toNanos(20)));
        assertTrue(status.cancelMagicEffects().mortalBody());
        assertEquals(new BattleAbilityStatus.MortalBody(0),
                status.mortalBody(start));
        status.activateMortalBody(start, 300);
        assertTrue(status.expireMortalBody(
                start + TimeUnit.SECONDS.toNanos(300)));
    }

    @Test void playerReflectionAddsEnchantThenAppliesRetainedReductions() {
        BattleMortalBody.Decision decision = BattleMortalBody.decide(
                SPELL, true, 52, 51, false, 35,
                true, 7, 3, 4, 2);
        assertEquals(new BattleMortalBody.Decision(
                true, 35, 35, true, 27, 9, 18), decision);

        BattleMortalBody.Decision minimum = BattleMortalBody.decide(
                SPELL, true, 52, 51, false, 1,
                true, -3, 30, 20, 10);
        assertEquals(new BattleMortalBody.Decision(
                true, 35, 1, true, 20, 60, 1), minimum);
    }

    @Test void npcReflectionUsesConfiguredBaseDamageWithoutPlayerReductions() {
        assertEquals(new BattleMortalBody.Decision(
                        true, 25, 25, true, 20, 0, 20),
                BattleMortalBody.decide(
                        SPELL, true, 52, 52, false, 25,
                        false, 0, 0, 0, 0));
    }

    @Test void inactiveBossAndFailedRollDoNotReflect() {
        assertEquals(BattleMortalBody.Decision.inactive(),
                BattleMortalBody.decide(
                        SPELL, false, 52, 51, false, 1,
                        true, 7, 0, 0, 0));
        assertEquals(BattleMortalBody.Decision.inactive(),
                BattleMortalBody.decide(
                        SPELL, true, 52, 51, true, 1,
                        false, 0, 0, 0, 0));
        assertEquals(new BattleMortalBody.Decision(
                        true, 15, 16, false, 0, 0, 0),
                BattleMortalBody.decide(
                        SPELL, true, 51, 52, false, 16,
                        false, 0, 0, 0, 0));
    }

    @Test void invalidDiceAndMissingMetadataFailClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> BattleMortalBody.decide(
                        SPELL, true, 52, 51, false, 0,
                        true, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BattleMortalBody.decide(
                        null, true, 52, 51, false, 1,
                        true, 0, 0, 0, 0));
    }
}
