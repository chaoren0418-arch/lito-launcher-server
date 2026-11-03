package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleTimedStatusTest {
    @Test void hasteUsesCeilingSecondsAndExpiresAtTheExactBoundary() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(10);
        status.activateHaste(start, 300);

        assertEquals(new BattleTimedStatus.Speed(BattleTimedStatus.HASTE, 300),
                status.speed(start));
        assertEquals(new BattleTimedStatus.Speed(BattleTimedStatus.HASTE, 1),
                status.speed(start + TimeUnit.SECONDS.toNanos(299) + 1));
        assertFalse(status.expireHaste(start + TimeUnit.SECONDS.toNanos(300) - 1));
        assertEquals(new BattleTimedStatus.Speed(BattleTimedStatus.NORMAL, 0),
                status.speed(start + TimeUnit.SECONDS.toNanos(300)));
        assertTrue(status.expireHaste(start + TimeUnit.SECONDS.toNanos(300)));
        assertFalse(status.expireHaste(start + TimeUnit.SECONDS.toNanos(301)));
    }

    @Test void aSecondPotionRefreshesInsteadOfStackingTheDuration() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(10);
        status.activateHaste(start, 300);
        status.activateHaste(start + TimeUnit.SECONDS.toNanos(100), 300);

        assertEquals(300, status.speed(start + TimeUnit.SECONDS.toNanos(100)).remainingSeconds());
        assertEquals(200, status.speed(start + TimeUnit.SECONDS.toNanos(200)).remainingSeconds());
        status.clear();
        assertEquals(BattleTimedStatus.NORMAL, status.speed(start).type());
    }

    @Test void greaterHasteRetainsItsSpellIdentityUntilExpiryOrCancellation() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(10);
        status.activateHaste(start, 2_400, BattleSpellCatalog.GREATER_HASTE);

        assertEquals(BattleSpellCatalog.GREATER_HASTE, status.hasteSkillId(start));
        assertEquals(new BattleTimedStatus.HasteExpiration(false, 0),
                status.expireHasteLease(
                        start + TimeUnit.SECONDS.toNanos(2_400) - 1));
        assertEquals(new BattleTimedStatus.HasteExpiration(
                        true, BattleSpellCatalog.GREATER_HASTE),
                status.expireHasteLease(start + TimeUnit.SECONDS.toNanos(2_400)));
        assertEquals(0, status.hasteSkillId(start + TimeUnit.SECONDS.toNanos(2_400)));

        status.activateHaste(start, 1_200, BattleSpellCatalog.HASTE);
        assertTrue(status.cancelMagicEffects().haste());
        assertEquals(0, status.hasteSkillId(start));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateHaste(start, 300, 52));
    }

    @Test void massSlowUsesExactLeaseAndMutuallyNeutralizesHaste() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(10);
        BattleTimedStatus.SpeedChange slowed = status.activateSlow(
                start, 64, BattleSpellCatalog.MASS_SLOW);
        assertEquals(new BattleTimedStatus.SpeedChange(
                BattleTimedStatus.SLOW, 64, 0), slowed);
        assertEquals(new BattleTimedStatus.Speed(BattleTimedStatus.SLOW, 64),
                status.speed(start));
        assertEquals(BattleSpellCatalog.MASS_SLOW, status.slowSkillId(start));
        assertFalse(status.expireSlowLease(
                start + TimeUnit.SECONDS.toNanos(64) - 1).expired());
        assertEquals(new BattleTimedStatus.SlowExpiration(
                        true, BattleSpellCatalog.MASS_SLOW),
                status.expireSlowLease(start + TimeUnit.SECONDS.toNanos(64)));

        status.activateHaste(start, 1_200, BattleSpellCatalog.HASTE);
        assertEquals(new BattleTimedStatus.SpeedChange(
                        BattleTimedStatus.NORMAL, 0, BattleSpellCatalog.HASTE),
                status.activateSlow(start, 64, BattleSpellCatalog.MASS_SLOW));
        assertEquals(BattleTimedStatus.NORMAL, status.speed(start).type());

        status.activateSlow(start, 64, BattleSpellCatalog.MASS_SLOW);
        assertEquals(new BattleTimedStatus.SpeedChange(
                        BattleTimedStatus.NORMAL, 0, BattleSpellCatalog.MASS_SLOW),
                status.activateHaste(start, 1_200, BattleSpellCatalog.HASTE));
        assertEquals(BattleTimedStatus.NORMAL, status.speed(start).type());

        status.activateSlow(start, 64, BattleSpellCatalog.MASS_SLOW);
        BattleTimedStatus.MagicCancellation cancellation = status.cancelMagicEffects();
        assertTrue(cancellation.slow());
        assertFalse(cancellation.haste());
        assertEquals(BattleTimedStatus.NORMAL, status.speed(start).type());
        assertThrows(IllegalArgumentException.class,
                () -> status.activateSlow(start, 64, BattleSpellCatalog.HASTE));
    }

    @Test void entangleRetainsSkillIdentityAndMutuallyNeutralizesHaste() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(20);

        BattleTimedStatus.SpeedChange slowed = status.activateSlow(
                start, 64, BattleSpellCatalog.ENTANGLE);
        assertEquals(new BattleTimedStatus.SpeedChange(
                BattleTimedStatus.SLOW, 64, 0), slowed);
        assertEquals(BattleSpellCatalog.ENTANGLE, status.slowSkillId(start));
        assertEquals(new BattleTimedStatus.SlowExpiration(
                        true, BattleSpellCatalog.ENTANGLE),
                status.expireSlowLease(start + TimeUnit.SECONDS.toNanos(64)));

        status.activateHaste(start, 1_200, BattleSpellCatalog.HASTE);
        BattleTimedStatus.SpeedChange neutralized = status.activateSlow(
                start + 1, 64, BattleSpellCatalog.ENTANGLE);
        assertEquals(new BattleTimedStatus.SpeedChange(
                BattleTimedStatus.NORMAL, 0, BattleSpellCatalog.HASTE),
                neutralized);
        assertEquals(0, status.hasteSkillId(start + 1));
        assertEquals(0, status.slowSkillId(start + 1));
    }

    @Test void bravePotionUsesCeilingSecondsRefreshesAndExpiresExactly() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(15);
        status.activateBrave(start, 300);
        assertEquals(new BattleTimedStatus.Brave(BattleTimedStatus.BRAVE_FIRST, 300),
                status.brave(start));
        assertEquals(new BattleTimedStatus.Brave(BattleTimedStatus.BRAVE_FIRST, 1),
                status.brave(start + TimeUnit.SECONDS.toNanos(299) + 1));
        status.activateBrave(start + TimeUnit.SECONDS.toNanos(100), 300);
        assertEquals(300,
                status.brave(start + TimeUnit.SECONDS.toNanos(100)).remainingSeconds());
        assertFalse(status.expireBrave(start + TimeUnit.SECONDS.toNanos(400) - 1));
        assertTrue(status.expireBrave(start + TimeUnit.SECONDS.toNanos(400)));
        assertEquals(BattleTimedStatus.BRAVE_NORMAL,
                status.brave(start + TimeUnit.SECONDS.toNanos(400)).mode());
        status.activateBrave(start, 300);
        status.clear();
        assertEquals(BattleTimedStatus.BRAVE_NORMAL, status.brave(start).mode());
    }

    @Test void holyWalkKeepsNativeModeFourAndReplacesTheBraveFamilyLease() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(20);
        status.activateBrave(start, 300);
        status.activateBrave(start + TimeUnit.SECONDS.toNanos(1), 32,
                BattleTimedStatus.BRAVE_HOLY_WALK);

        assertEquals(new BattleTimedStatus.Brave(
                        BattleTimedStatus.BRAVE_HOLY_WALK, 32),
                status.brave(start + TimeUnit.SECONDS.toNanos(1)));
        assertFalse(status.expireBrave(start + TimeUnit.SECONDS.toNanos(33) - 1));
        assertTrue(status.expireBrave(start + TimeUnit.SECONDS.toNanos(33)));
        assertEquals(BattleTimedStatus.BRAVE_NORMAL,
                status.brave(start + TimeUnit.SECONDS.toNanos(33)).mode());
    }

    @Test void bloodlustKeepsNativeModeSixAndCancellationClearsIt() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(21);
        status.activateBrave(start, 300, BattleTimedStatus.BRAVE_HOLY_WALK);
        status.activateBrave(start + 1, 60, BattleTimedStatus.BRAVE_BLOODLUST);

        assertEquals(new BattleTimedStatus.Brave(
                        BattleTimedStatus.BRAVE_BLOODLUST, 60),
                status.brave(start + 1));
        BattleTimedStatus.MagicCancellation cancellation =
                status.cancelMagicEffects();
        assertTrue(cancellation.brave());
        assertEquals(BattleTimedStatus.BRAVE_NORMAL,
                status.brave(start + 1).mode());
    }

    @Test void elvenWaferKeepsNativeModeThreeAndRefreshesTheBraveFamily() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(22);
        status.activateBrave(start, 300);
        status.activateBrave(start + TimeUnit.SECONDS.toNanos(1), 480,
                BattleTimedStatus.BRAVE_ELF);
        assertEquals(new BattleTimedStatus.Brave(BattleTimedStatus.BRAVE_ELF, 480),
                status.brave(start + TimeUnit.SECONDS.toNanos(1)));
        assertFalse(status.expireBrave(start + TimeUnit.SECONDS.toNanos(481) - 1));
        assertTrue(status.expireBrave(start + TimeUnit.SECONDS.toNanos(481)));
    }

    @Test void braveLeaseRejectsReceiverOnlyModeTwoButAcceptsProducerModes() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(23);
        for (int mode : new int[] {
                BattleTimedStatus.BRAVE_FIRST,
                BattleTimedStatus.BRAVE_ELF,
                BattleTimedStatus.BRAVE_HOLY_WALK,
                BattleTimedStatus.BRAVE_SECOND,
                BattleTimedStatus.BRAVE_BLOODLUST
        }) {
            status.activateBrave(start, 32, mode);
            assertEquals(mode, status.brave(start).mode());
        }
        assertThrows(IllegalArgumentException.class,
                () -> status.activateBrave(start, 32, 2));
    }

    @Test void blindnessRefreshesAndExpiresAtTheExactServerBoundary() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(20);
        status.activateBlind(start, 16);

        assertTrue(status.blindActive(start));
        assertTrue(status.blindActive(start + TimeUnit.SECONDS.toNanos(16) - 1));
        assertFalse(status.expireBlind(start + TimeUnit.SECONDS.toNanos(16) - 1));
        assertFalse(status.blindActive(start + TimeUnit.SECONDS.toNanos(16)));
        assertTrue(status.expireBlind(start + TimeUnit.SECONDS.toNanos(16)));
        assertFalse(status.expireBlind(start + TimeUnit.SECONDS.toNanos(17)));

        status.activateBlind(start, 16);
        status.activateBlind(start + TimeUnit.SECONDS.toNanos(10), 16);
        assertTrue(status.blindActive(start + TimeUnit.SECONDS.toNanos(25)));
        status.clear();
        assertFalse(status.blindActive(start));
    }

    @Test void lightSpellRefreshesAndExpiresAtTheExactServerBoundary() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(25);
        status.activateLight(start, 3_600);

        assertTrue(status.lightActive(start));
        assertTrue(status.lightActive(start + TimeUnit.SECONDS.toNanos(3_600) - 1));
        assertFalse(status.expireLight(start + TimeUnit.SECONDS.toNanos(3_600) - 1));
        assertFalse(status.lightActive(start + TimeUnit.SECONDS.toNanos(3_600)));
        assertTrue(status.expireLight(start + TimeUnit.SECONDS.toNanos(3_600)));
        assertFalse(status.expireLight(start + TimeUnit.SECONDS.toNanos(3_601)));

        status.activateLight(start, 3_600);
        status.clear();
        assertFalse(status.lightActive(start));
    }

    @Test void bluePotionRefreshesRatherThanStacksAndExpiresSilently() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(27);
        status.activateBluePotion(start, 600);
        assertTrue(status.bluePotionActive(start));
        status.activateBluePotion(start + TimeUnit.SECONDS.toNanos(100), 700);
        assertTrue(status.bluePotionActive(start + TimeUnit.SECONDS.toNanos(799)));
        assertFalse(status.expireBluePotion(start + TimeUnit.SECONDS.toNanos(800) - 1));
        assertFalse(status.bluePotionActive(start + TimeUnit.SECONDS.toNanos(800)));
        assertTrue(status.expireBluePotion(start + TimeUnit.SECONDS.toNanos(800)));
        assertFalse(status.expireBluePotion(start + TimeUnit.SECONDS.toNanos(801)));
        status.activateBluePotion(start, 600);
        status.clear();
        assertFalse(status.bluePotionActive(start));
    }

    @Test void wisdomPotionRefreshesExpiresAndCancellationClearsIt() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(27);
        status.activateWisdomPotion(start, 300);
        assertTrue(status.wisdomPotionActive(start));
        status.activateWisdomPotion(start + TimeUnit.SECONDS.toNanos(100), 300);
        assertTrue(status.wisdomPotionActive(start + TimeUnit.SECONDS.toNanos(399)));
        assertFalse(status.expireWisdomPotion(
                start + TimeUnit.SECONDS.toNanos(400) - 1));
        assertTrue(status.expireWisdomPotion(start + TimeUnit.SECONDS.toNanos(400)));
        assertFalse(status.wisdomPotionActive(start + TimeUnit.SECONDS.toNanos(400)));
        status.activateWisdomPotion(start, 300);
        assertTrue(status.cancelMagicEffects().wisdomPotion());
        assertFalse(status.wisdomPotionActive(start));
        status.activateWisdomPotion(start, 300);
        status.clear();
        assertFalse(status.wisdomPotionActive(start));
    }

    @Test void meditationRefreshesExpiresAndCanBeCancelledByActivity() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(28);
        status.activateMeditation(start, 640);
        assertTrue(status.meditationActive(start));
        status.activateMeditation(start + TimeUnit.SECONDS.toNanos(100), 640);
        assertTrue(status.meditationActive(start + TimeUnit.SECONDS.toNanos(739)));
        assertFalse(status.expireMeditation(start + TimeUnit.SECONDS.toNanos(740) - 1));
        assertTrue(status.cancelMeditation());
        assertFalse(status.cancelMeditation());
        assertFalse(status.meditationActive(start));
        status.activateMeditation(start, 640);
        assertTrue(status.expireMeditation(start + TimeUnit.SECONDS.toNanos(640)));
    }

    @Test void concentrationRefreshesAndOnlyEndsAtItsLeaseBoundary() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(29);
        status.activateConcentration(start, 600);
        assertTrue(status.concentrationActive(start));
        status.activateConcentration(start + TimeUnit.SECONDS.toNanos(100), 600);
        assertTrue(status.concentrationActive(start + TimeUnit.SECONDS.toNanos(699)));
        assertFalse(status.expireConcentration(start + TimeUnit.SECONDS.toNanos(700) - 1));
        assertFalse(status.concentrationActive(start + TimeUnit.SECONDS.toNanos(700)));
        assertTrue(status.expireConcentration(start + TimeUnit.SECONDS.toNanos(700)));
        status.activateConcentration(start, 600);
        status.clear();
        assertFalse(status.concentrationActive(start));
    }

    @Test void counterMagicRefreshesExpiresAndIsConsumedExactlyOnce() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(31);
        status.activateCounterMagic(start, 16);
        assertTrue(status.counterMagicActive(start));
        assertTrue(status.counterMagicActive(start + TimeUnit.SECONDS.toNanos(16) - 1));
        status.activateCounterMagic(start + TimeUnit.SECONDS.toNanos(10), 16);
        assertTrue(status.counterMagicActive(start + TimeUnit.SECONDS.toNanos(25)));
        assertFalse(status.expireCounterMagic(
                start + TimeUnit.SECONDS.toNanos(26) - 1));
        assertTrue(status.consumeCounterMagic(
                start + TimeUnit.SECONDS.toNanos(26) - 1));
        assertFalse(status.consumeCounterMagic(
                start + TimeUnit.SECONDS.toNanos(26) - 1));

        status.activateCounterMagic(start, 16);
        assertTrue(status.expireCounterMagic(start + TimeUnit.SECONDS.toNanos(16)));
        assertFalse(status.counterMagicActive(start + TimeUnit.SECONDS.toNanos(16)));
        status.activateCounterMagic(start, 16);
        status.clear();
        assertFalse(status.counterMagicActive(start));
    }

    @Test void underwaterBreathAccumulatesCapsAndExpiresExactly() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(30);
        assertEquals(new BattleTimedStatus.Breath(1_800),
                status.addBreath(start, 1_800, 3_600));
        assertEquals(new BattleTimedStatus.Breath(3_500),
                status.addBreath(start + TimeUnit.SECONDS.toNanos(100), 1_800, 3_600));
        assertEquals(new BattleTimedStatus.Breath(3_600),
                status.addBreath(start + TimeUnit.SECONDS.toNanos(200), 1_800, 3_600));
        long expiry = start + TimeUnit.SECONDS.toNanos(3_800);
        assertEquals(1, status.breath(expiry - 1).remainingSeconds());
        assertFalse(status.expireBreath(expiry - 1));
        assertEquals(0, status.breath(expiry).remainingSeconds());
        assertTrue(status.expireBreath(expiry));
        status.addBreath(start, 1_800, 3_600);
        assertTrue(status.cancelBreath());
        assertFalse(status.cancelBreath());
        assertEquals(0, status.breath(start).remainingSeconds());
        status.addBreath(start, 1_800, 3_600);
        status.clear();
        assertEquals(0, status.breath(start).remainingSeconds());
    }

    @Test void drunkenStatusPersistsUntilAnAuthoritativeClear() {
        BattleTimedStatus status = new BattleTimedStatus();
        assertFalse(status.drunkenActive());
        status.activateDrunken();
        assertTrue(status.drunkenActive());
        assertTrue(status.clearDrunken());
        assertFalse(status.clearDrunken());
        status.activateDrunken();
        status.clear();
        assertFalse(status.drunkenActive());
    }

    @Test void damagePoisonTicksBeforeButNotAtItsThirtySecondExpiry() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(40);
        assertTrue(status.activatePoison(start, 30, 3_000));
        assertFalse(status.activatePoison(start + 1, 30, 3_000));
        assertTrue(status.poisonActive(start));
        assertEquals(new BattleTimedStatus.PoisonPoll(0, false),
                status.pollPoison(start + TimeUnit.SECONDS.toNanos(3) - 1, 3_000));
        assertEquals(new BattleTimedStatus.PoisonPoll(1, false),
                status.pollPoison(start + TimeUnit.SECONDS.toNanos(3), 3_000));
        assertEquals(new BattleTimedStatus.PoisonPoll(2, false),
                status.pollPoison(start + TimeUnit.SECONDS.toNanos(9), 3_000));
        assertEquals(new BattleTimedStatus.PoisonPoll(0, true),
                status.pollPoison(start + TimeUnit.SECONDS.toNanos(30), 3_000));
        assertFalse(status.poisonActive(start + TimeUnit.SECONDS.toNanos(30)));

        assertTrue(status.activatePoison(start, 30, 3_000));
        assertTrue(status.curePoison());
        assertFalse(status.curePoison());
    }

    @Test void retainedSilencePoisonIsIndefiniteAndSharesPoisonCures() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(40);

        assertTrue(status.activatePoisonSilence(start));
        assertFalse(status.activatePoisonSilence(start + TimeUnit.DAYS.toNanos(1)));
        assertTrue(status.poisonSilenceActive());
        assertTrue(status.silenceActive(start + TimeUnit.DAYS.toNanos(1)));
        assertFalse(status.activatePoison(start + 1, 30, 3_000));
        assertTrue(status.curePoisonSilence());
        assertFalse(status.curePoisonSilence());
        assertFalse(status.silenceActive(start + TimeUnit.DAYS.toNanos(1)));

        assertTrue(status.activatePoisonSilence(start));
        BattleTimedStatus.CurseRemoval removal = status.removeCurse();
        assertTrue(removal.poisonCured());
        assertFalse(status.poisonSilenceActive());

        assertTrue(status.activatePoisonSilence(start));
        BattleTimedStatus.MagicCancellation cancellation =
                status.cancelMagicEffects();
        assertTrue(cancellation.silence());
        assertTrue(cancellation.poison());
        assertFalse(status.poisonSilenceActive());

        assertTrue(status.activatePoisonSilence(start));
        status.clear();
        assertFalse(status.poisonSilenceActive());
    }

    @Test void areaSilenceIsIndependentButFeedsCombinedSilenceGate() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(20);
        status.activateSilence(start, 5);
        assertFalse(status.activateAreaSilence(start, 16));
        assertTrue(status.silenceActive(start + TimeUnit.SECONDS.toNanos(6)));
        assertTrue(status.expireSilence(start + TimeUnit.SECONDS.toNanos(6)));
        assertTrue(status.expireAreaSilence(
                start + TimeUnit.SECONDS.toNanos(16)));
        assertFalse(status.silenceActive(
                start + TimeUnit.SECONDS.toNanos(16)));

        assertFalse(status.activateAreaSilence(start, 16));
        assertTrue(status.activateAreaSilence(start + 1, 16));
        BattleTimedStatus.MagicCancellation cancellation =
                status.cancelMagicEffects();
        assertTrue(cancellation.silence());
        assertFalse(status.areaSilenceActive(start + 2));
    }

    @Test void ordinaryNpcSilenceIsConsumedByOneSpellAttemptButAreaSilenceIsNot() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(20);
        status.activateSilence(start, 16);
        status.activateAreaSilence(start, 16);

        assertTrue(status.consumeSilenceForNpcSpell(start + 1));
        assertFalse(status.consumeSilenceForNpcSpell(start + 2));
        assertTrue(status.areaSilenceActive(start + 2));
        assertTrue(status.silenceActive(start + 2));
        assertFalse(status.areaSilenceActive(
                start + TimeUnit.SECONDS.toNanos(16)));
    }

    @Test void venomResistRejectsEveryRetainedPoisonInstaller() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(10);
        status.activateVenomResist(start, 320);

        assertFalse(status.acceptsPoison(start, BattleTimedStatus.PoisonKind.DAMAGE));
        assertFalse(status.acceptsPoison(start, BattleTimedStatus.PoisonKind.SILENCE));
        assertFalse(status.acceptsPoison(start, BattleTimedStatus.PoisonKind.PARALYSIS));
        assertFalse(status.activatePoison(start, 30, 3_000));
        assertFalse(status.activatePoisonSilence(start));
    }

    @Test void curseParalysisTransitionsFromWarningToLockAtExactBoundaries() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(10);
        long activation = start + TimeUnit.SECONDS.toNanos(8);
        long expiry = activation + TimeUnit.SECONDS.toNanos(16);

        assertTrue(status.activateParalysis(start, 8_000, 16_000));
        assertFalse(status.activateParalysis(start + 1, 8_000, 16_000));
        assertTrue(status.paralysisVisualActive(start));
        assertFalse(status.paralysisActive(activation - 1));
        assertEquals(new BattleTimedStatus.ParalysisPoll(false, false, false),
                status.pollParalysis(activation - 1));
        assertTrue(status.paralysisActive(activation));
        assertEquals(new BattleTimedStatus.ParalysisPoll(true, false, false),
                status.pollParalysis(activation));
        assertEquals(new BattleTimedStatus.ParalysisPoll(false, false, false),
                status.pollParalysis(activation + 1));
        assertTrue(status.paralysisActive(expiry - 1));
        assertFalse(status.paralysisActive(expiry));
        assertEquals(new BattleTimedStatus.ParalysisPoll(false, true, true),
                status.pollParalysis(expiry));
        assertFalse(status.paralysisVisualActive(expiry));
    }

    @Test void sleepLocksAllActionsAndSupportsExpiryDamageStyleCancellation() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(10);
        long expiry = start + TimeUnit.SECONDS.toNanos(32);

        assertTrue(status.activateSleep(start, 32));
        assertTrue(status.sleepActive(start));
        assertTrue(status.actionLocked(start));
        assertTrue(status.sleepActive(expiry - 1));
        assertFalse(status.sleepActive(expiry));
        assertTrue(status.expireSleep(expiry));
        assertFalse(status.actionLocked(expiry));
        assertFalse(status.expireSleep(expiry));

        assertTrue(status.activateSleep(start, 32));
        assertTrue(status.cancelSleep());
        assertFalse(status.cancelSleep());
        assertFalse(status.actionLocked(start));
    }

    @Test void freezeLocksUntilExactExpiryIsNotRefreshedAndCanBeCancelled() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(10);
        long expiry = start + TimeUnit.SECONDS.toNanos(16);

        assertTrue(status.activateFreeze(start, 16));
        assertFalse(status.activateFreeze(start + 1, 16));
        assertTrue(status.freezeActive(start));
        assertTrue(status.actionLocked(start));
        assertTrue(status.freezeActive(expiry - 1));
        assertFalse(status.freezeActive(expiry));
        assertTrue(status.expireFreeze(expiry));
        assertFalse(status.actionLocked(expiry));
        assertFalse(status.expireFreeze(expiry));

        assertTrue(status.activateFreeze(start, 16));
        assertTrue(status.cancelFreeze());
        assertFalse(status.cancelFreeze());
        assertFalse(status.actionLocked(start));
        status.activateFreeze(start, 16);
        status.clear();
        assertFalse(status.freezeActive(start));
    }

    @Test void curseParalysisCancellationOnlyUnlocksAfterModeTwoWasSent() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(50);
        long activation = start + TimeUnit.SECONDS.toNanos(8);

        assertTrue(status.activateParalysis(start, 8_000, 16_000));
        assertEquals(new BattleTimedStatus.ParalysisCancellation(true, false),
                status.cancelParalysis());
        assertEquals(new BattleTimedStatus.ParalysisCancellation(false, false),
                status.cancelParalysis());

        assertTrue(status.activateParalysis(start, 8_000, 16_000));
        assertTrue(status.noteParalysisLockSent(activation));
        assertEquals(new BattleTimedStatus.ParalysisCancellation(true, true),
                status.cancelParalysis());
        status.clear();
        assertFalse(status.paralysisVisualActive(start));
    }

    @Test void removeCurseClearsPoisonFamilyAndCurseParalysis() {
        BattleTimedStatus status = new BattleTimedStatus();
        long now = TimeUnit.SECONDS.toNanos(2);
        assertTrue(status.activatePoison(now, 30, 3_000));
        status.activateBlind(now, 16);
        assertTrue(status.activateSleep(now, 32));
        assertTrue(status.activateFreeze(now, 16));
        assertTrue(status.activateEarthBind(now, 16));
        assertTrue(status.activateParalysis(now, 8_000, 16_000));

        BattleTimedStatus.CurseRemoval removal = status.removeCurse();
        assertTrue(removal.poisonCured());
        assertEquals(new BattleTimedStatus.ParalysisCancellation(true, false),
                removal.paralysisCancellation());
        assertFalse(status.poisonActive(now + 1));
        assertFalse(status.paralysisVisualActive(now + 1));
        assertTrue(status.blindActive(now + 1));
        assertTrue(status.sleepActive(now + 1));
        assertTrue(status.freezeActive(now + 1));
        assertTrue(status.earthBindActive(now + 1));
    }

    @Test void curePoisonFamilyDoesNotClearIndependentCurseParalysis() {
        BattleTimedStatus status = new BattleTimedStatus();
        long now = TimeUnit.SECONDS.toNanos(2);
        assertTrue(status.activatePoison(now, 30, 3_000));
        assertTrue(status.activateParalysis(now, 8_000, 16_000));

        assertTrue(status.curePoison());
        assertFalse(status.curePoisonSilence());
        assertFalse(status.poisonActive(now + 1));
        assertTrue(status.paralysisVisualActive(now + 1));
        assertEquals(new BattleTimedStatus.ParalysisCancellation(true, false),
                status.cancelParalysis());
    }

    @Test void earthBindOwnsActionLockAndSharesFreezeVisualLeaseSafely() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(10);
        long bindExpiry = start + TimeUnit.SECONDS.toNanos(16);
        long freezeExpiry = start + TimeUnit.SECONDS.toNanos(32);

        assertTrue(status.activateEarthBind(start, 16));
        assertFalse(status.activateEarthBind(start + 1, 16));
        assertTrue(status.earthBindActive(start));
        assertTrue(status.freezeVisualLockActive(start));
        assertTrue(status.actionLocked(start));
        assertTrue(status.activateFreeze(start, 32));
        assertTrue(status.expireEarthBind(bindExpiry));
        assertFalse(status.earthBindActive(bindExpiry));
        assertTrue(status.freezeVisualLockActive(bindExpiry));
        assertTrue(status.actionLocked(bindExpiry));
        assertTrue(status.expireFreeze(freezeExpiry));
        assertFalse(status.freezeVisualLockActive(freezeExpiry));
        assertFalse(status.actionLocked(freezeExpiry));

        assertTrue(status.activateEarthBind(start, 16));
        assertTrue(status.cancelEarthBind());
        assertFalse(status.cancelEarthBind());
        status.activateEarthBind(start, 16);
        status.clear();
        assertFalse(status.earthBindActive(start));
    }

    @Test void absoluteBarrierRefreshesExpiresCancelsAndSurvivesCancellation() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(12);
        status.activateAbsoluteBarrier(start, 12);
        assertTrue(status.absoluteBarrierActive(start));
        assertTrue(status.absoluteBarrierActive(
                start + TimeUnit.SECONDS.toNanos(12) - 1));
        assertFalse(status.absoluteBarrierActive(
                start + TimeUnit.SECONDS.toNanos(12)));
        assertTrue(status.expireAbsoluteBarrier(
                start + TimeUnit.SECONDS.toNanos(12)));
        assertFalse(status.expireAbsoluteBarrier(
                start + TimeUnit.SECONDS.toNanos(12)));

        status.activateAbsoluteBarrier(start, 12);
        status.cancelMagicEffects();
        assertTrue(status.absoluteBarrierActive(start));
        assertTrue(status.cancelAbsoluteBarrier());
        assertFalse(status.cancelAbsoluteBarrier());
        status.activateAbsoluteBarrier(start, 12);
        status.clear();
        assertFalse(status.absoluteBarrierActive(start));
    }

    @Test void advanceSpiritRefreshesExpiresAndSurvivesCancellation() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(12);
        assertTrue(status.activateAdvanceSpirit(start, 1_200));
        assertTrue(status.advanceSpiritActive(start));
        assertFalse(status.activateAdvanceSpirit(
                start + TimeUnit.SECONDS.toNanos(600), 1_200));
        assertTrue(status.advanceSpiritActive(
                start + TimeUnit.SECONDS.toNanos(1_799)));
        assertFalse(status.advanceSpiritActive(
                start + TimeUnit.SECONDS.toNanos(1_800)));
        assertTrue(status.expireAdvanceSpirit(
                start + TimeUnit.SECONDS.toNanos(1_800)));
        assertFalse(status.expireAdvanceSpirit(
                start + TimeUnit.SECONDS.toNanos(1_800)));

        assertTrue(status.activateAdvanceSpirit(start, 1_200));
        status.cancelMagicEffects();
        assertTrue(status.advanceSpiritActive(start));
        status.clear();
        assertFalse(status.advanceSpiritActive(start));
    }

    @Test void magicCancellationAtomicallyClearsEveryImplementedCancelableFamily() {
        BattleTimedStatus status = new BattleTimedStatus();
        long now = TimeUnit.SECONDS.toNanos(10);
        status.activateHaste(now, 1_200);
        status.activateBrave(now, 1_200);
        status.activateBlind(now, 32);
        status.activateWeakness(now, 64);
        status.activateDisease(now, 64);
        status.activateSilence(now, 16);
        assertTrue(status.activateDecayPotion(now, 16));
        status.activateLight(now, 1_200);
        status.activateBluePotion(now, 600);
        status.activateWisdomPotion(now, 300);
        status.activateMeditation(now, 640);
        status.activateConcentration(now, 600);
        status.activateCounterMagic(now, 16);
        status.addBreath(now, 600, 1_800);
        assertTrue(status.activatePoison(now, 30, 3_000));
        assertTrue(status.activateSleep(now, 32));
        assertTrue(status.activateFreeze(now, 16));
        assertTrue(status.activateEarthBind(now, 16));
        assertTrue(status.activateParalysis(now, 8_000, 16_000));

        BattleTimedStatus.MagicCancellation cancelled =
                status.cancelMagicEffects();
        assertTrue(cancelled.haste());
        assertFalse(cancelled.slow());
        assertTrue(cancelled.brave());
        assertTrue(cancelled.blind());
        assertTrue(cancelled.weakness());
        assertTrue(cancelled.disease());
        assertTrue(cancelled.silence());
        assertTrue(cancelled.decayPotion());
        assertTrue(cancelled.light());
        assertTrue(cancelled.bluePotion());
        assertTrue(cancelled.wisdomPotion());
        assertTrue(cancelled.meditation());
        assertTrue(cancelled.concentration());
        assertTrue(cancelled.counterMagic());
        assertTrue(cancelled.breath());
        assertTrue(cancelled.poison());
        assertTrue(cancelled.sleep());
        assertTrue(cancelled.freeze());
        assertTrue(cancelled.earthBind());
        assertEquals(new BattleTimedStatus.ParalysisCancellation(true, false),
                cancelled.paralysisCancellation());
        assertEquals(BattleTimedStatus.NORMAL, status.speed(now).type());
        assertEquals(BattleTimedStatus.BRAVE_NORMAL, status.brave(now).mode());
        assertFalse(status.blindActive(now));
        assertFalse(status.weaknessActive(now));
        assertFalse(status.diseaseActive(now));
        assertFalse(status.silenceActive(now));
        assertFalse(status.decayPotionActive(now));
        assertFalse(status.lightActive(now));
        assertFalse(status.bluePotionActive(now));
        assertFalse(status.wisdomPotionActive(now));
        assertFalse(status.meditationActive(now));
        assertFalse(status.concentrationActive(now));
        assertFalse(status.counterMagicActive(now));
        assertEquals(0, status.breath(now).remainingSeconds());
        assertFalse(status.poisonActive(now));
        assertFalse(status.actionLocked(now));

        BattleTimedStatus.MagicCancellation empty =
                status.cancelMagicEffects();
        assertFalse(empty.haste());
        assertFalse(empty.slow());
        assertFalse(empty.disease());
        assertFalse(empty.silence());
        assertFalse(empty.decayPotion());
        assertFalse(empty.paralysisCancellation().present());
    }

    @Test void diseaseLeaseActivatesExpiresAndClears() {
        BattleTimedStatus status = new BattleTimedStatus();
        long now = TimeUnit.SECONDS.toNanos(7);
        status.activateDisease(now, 64);
        assertTrue(status.diseaseActive(now));
        assertTrue(status.diseaseActive(now + TimeUnit.SECONDS.toNanos(63)));
        assertFalse(status.expireDisease(now + TimeUnit.SECONDS.toNanos(63)));
        assertTrue(status.expireDisease(now + TimeUnit.SECONDS.toNanos(64)));
        assertFalse(status.diseaseActive(now + TimeUnit.SECONDS.toNanos(64)));
        status.activateDisease(now, 64);
        status.clear();
        assertFalse(status.diseaseActive(now));
    }

    @Test void silenceLeaseActivatesExpiresAndClears() {
        BattleTimedStatus status = new BattleTimedStatus();
        long now = TimeUnit.SECONDS.toNanos(7);
        status.activateSilence(now, 16);
        assertTrue(status.silenceActive(now));
        assertTrue(status.silenceActive(now + TimeUnit.SECONDS.toNanos(15)));
        assertFalse(status.expireSilence(now + TimeUnit.SECONDS.toNanos(15)));
        assertTrue(status.expireSilence(now + TimeUnit.SECONDS.toNanos(16)));
        assertFalse(status.silenceActive(now + TimeUnit.SECONDS.toNanos(16)));
        status.activateSilence(now, 16);
        status.clear();
        assertFalse(status.silenceActive(now));
    }

    @Test void confusionSilenceUsesTheExistingLeaseWithoutRefreshingIt() {
        BattleTimedStatus status = new BattleTimedStatus();
        long now = TimeUnit.SECONDS.toNanos(7);
        assertTrue(status.activateSilenceIfAbsent(now, 8));
        assertFalse(status.activateSilenceIfAbsent(
                now + TimeUnit.SECONDS.toNanos(3), 8));
        assertTrue(status.silenceActive(
                now + TimeUnit.SECONDS.toNanos(7)));
        assertTrue(status.expireSilence(
                now + TimeUnit.SECONDS.toNanos(8)));
        assertFalse(status.silenceActive(
                now + TimeUnit.SECONDS.toNanos(8)));
        assertTrue(status.activateSilenceIfAbsent(
                now + TimeUnit.SECONDS.toNanos(8), 8));
    }

    @Test void decayPotionLeaseDoesNotRefreshAndCancellationClearsIt() {
        BattleTimedStatus status = new BattleTimedStatus();
        long now = TimeUnit.SECONDS.toNanos(7);
        assertTrue(status.activateDecayPotion(now, 16));
        assertFalse(status.activateDecayPotion(
                now + TimeUnit.SECONDS.toNanos(5), 16));
        assertTrue(status.decayPotionActive(
                now + TimeUnit.SECONDS.toNanos(15)));
        assertFalse(status.expireDecayPotion(
                now + TimeUnit.SECONDS.toNanos(15)));
        assertTrue(status.expireDecayPotion(
                now + TimeUnit.SECONDS.toNanos(16)));
        assertFalse(status.decayPotionActive(
                now + TimeUnit.SECONDS.toNanos(16)));
        assertTrue(status.activateDecayPotion(now, 16));
        assertTrue(status.cancelMagicEffects().decayPotion());
        assertFalse(status.decayPotionActive(now));
        assertTrue(status.activateDecayPotion(now, 16));
        status.clear();
        assertFalse(status.decayPotionActive(now));
    }

    @Test void polluteWaterLeaseRefreshesExpiresCancelsAndClears() {
        BattleTimedStatus status = new BattleTimedStatus();
        long now = TimeUnit.SECONDS.toNanos(7);
        status.activatePolluteWater(now, 192);
        assertTrue(status.polluteWaterActive(
                now + TimeUnit.SECONDS.toNanos(191)));
        status.activatePolluteWater(
                now + TimeUnit.SECONDS.toNanos(100), 192);
        assertFalse(status.expirePolluteWater(
                now + TimeUnit.SECONDS.toNanos(291)));
        assertTrue(status.expirePolluteWater(
                now + TimeUnit.SECONDS.toNanos(292)));
        assertFalse(status.polluteWaterActive(
                now + TimeUnit.SECONDS.toNanos(292)));
        status.activatePolluteWater(now, 192);
        assertTrue(status.cancelMagicEffects().polluteWater());
        assertFalse(status.polluteWaterActive(now));
        status.activatePolluteWater(now, 192);
        status.clear();
        assertFalse(status.polluteWaterActive(now));
    }

    @Test void strikerGaleLeaseRefreshesExpiresCancelsAndClears() {
        BattleTimedStatus status = new BattleTimedStatus();
        long now = TimeUnit.SECONDS.toNanos(7);
        status.activateStrikerGale(now, 192);
        assertTrue(status.strikerGaleActive(
                now + TimeUnit.SECONDS.toNanos(191)));
        status.activateStrikerGale(
                now + TimeUnit.SECONDS.toNanos(100), 192);
        assertFalse(status.expireStrikerGale(
                now + TimeUnit.SECONDS.toNanos(291)));
        assertTrue(status.expireStrikerGale(
                now + TimeUnit.SECONDS.toNanos(292)));
        assertFalse(status.strikerGaleActive(
                now + TimeUnit.SECONDS.toNanos(292)));
        status.activateStrikerGale(now, 192);
        assertTrue(status.cancelMagicEffects().strikerGale());
        assertFalse(status.strikerGaleActive(now));
        status.activateStrikerGale(now, 192);
        status.clear();
        assertFalse(status.strikerGaleActive(now));
    }

    @Test void shockStunStacksPlayerRemainingSecondsCapsAtFourAndSurvivesCancellation() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(100);
        assertEquals(new BattleTimedStatus.ShockStunActivation(2, false),
                status.activateShockStun(start, 2, 4));
        assertTrue(status.shockStunActive(start));
        assertTrue(status.actionLocked(start));

        long refresh = start + TimeUnit.MILLISECONDS.toNanos(1_500);
        assertEquals(new BattleTimedStatus.ShockStunActivation(4, true),
                status.activateShockStun(refresh, 3, 4));
        status.cancelMagicEffects();
        assertTrue(status.shockStunActive(refresh));
        assertTrue(status.actionLocked(refresh));
        assertFalse(status.expireShockStun(
                refresh + TimeUnit.SECONDS.toNanos(4) - 1));
        assertTrue(status.expireShockStun(
                refresh + TimeUnit.SECONDS.toNanos(4)));
        assertFalse(status.actionLocked(
                refresh + TimeUnit.SECONDS.toNanos(4)));

        status.activateShockStun(start, 4, 4);
        assertTrue(status.cancelShockStun());
        assertFalse(status.cancelShockStun());
        assertThrows(IllegalArgumentException.class,
                () -> status.activateShockStun(start, 5, 4));
        status.activateShockStun(start, 2, 4);
        status.clear();
        assertFalse(status.shockStunActive(start));
    }

    @Test void boneBreakIsIndependentNonRefreshingAndCancellationUnlocksIt() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(120);
        assertTrue(status.activateBoneBreak(start, 2));
        assertTrue(status.boneBreakActive(start));
        assertTrue(status.actionLocked(start));
        assertFalse(status.activateBoneBreak(
                start + TimeUnit.SECONDS.toNanos(1), 2));
        assertFalse(status.expireBoneBreak(
                start + TimeUnit.SECONDS.toNanos(2) - 1));
        assertTrue(status.expireBoneBreak(
                start + TimeUnit.SECONDS.toNanos(2)));
        assertFalse(status.actionLocked(
                start + TimeUnit.SECONDS.toNanos(2)));

        assertTrue(status.activateBoneBreak(start, 2));
        assertTrue(status.cancelMagicEffects().boneBreak());
        assertFalse(status.boneBreakActive(start));
        assertFalse(status.cancelMagicEffects().boneBreak());
        assertTrue(status.activateBoneBreak(start, 2));
        assertTrue(status.cancelBoneBreak());
        assertFalse(status.cancelBoneBreak());
        assertThrows(IllegalArgumentException.class,
                () -> status.activateBoneBreak(start, 0));
        status.activateBoneBreak(start, 2);
        status.clear();
        assertFalse(status.boneBreakActive(start));
    }

    @Test void burningSpiritRefreshesExpiresAndIsRemovedByCancellation() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(25);
        status.activateBurningSpirit(start, 320);
        assertTrue(status.burningSpiritActive(start));
        assertFalse(status.expireBurningSpirit(
                start + TimeUnit.SECONDS.toNanos(320) - 1));
        assertTrue(status.expireBurningSpirit(
                start + TimeUnit.SECONDS.toNanos(320)));
        assertFalse(status.burningSpiritActive(
                start + TimeUnit.SECONDS.toNanos(320)));

        status.activateBurningSpirit(start, 320);
        assertTrue(status.cancelMagicEffects().burningSpirit());
        assertFalse(status.burningSpiritActive(start));
        assertFalse(status.cancelMagicEffects().burningSpirit());
        assertThrows(IllegalArgumentException.class,
                () -> status.activateBurningSpirit(start, 0));
        status.activateBurningSpirit(start, 320);
        status.clear();
        assertFalse(status.burningSpiritActive(start));
    }

    @Test void elementalFireRefreshesExpiresAndIsRemovedByCancellation() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = 1_000_000_000L;
        assertFalse(status.activateElementalFire(start, 192));
        assertTrue(status.elementalFireActive(start));
        assertTrue(status.activateElementalFire(
                start + java.util.concurrent.TimeUnit.SECONDS.toNanos(10), 192));
        long refreshedExpiry = start
                + java.util.concurrent.TimeUnit.SECONDS.toNanos(202);
        assertTrue(status.elementalFireActive(refreshedExpiry - 1));
        assertTrue(status.expireElementalFire(refreshedExpiry));
        assertFalse(status.elementalFireActive(refreshedExpiry));

        assertFalse(status.activateElementalFire(start, 192));
        assertTrue(status.cancelMagicEffects().elementalFire());
        assertFalse(status.elementalFireActive(start));
        assertFalse(status.cancelMagicEffects().elementalFire());
        assertFalse(status.activateElementalFire(start, 192));
        status.clear();
        assertFalse(status.elementalFireActive(start));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateElementalFire(-1, 192));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateElementalFire(start, 0));
    }

    @Test void soulOfFlameRefreshesExpiresAndIsRemovedByCancellation() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(40);
        assertFalse(status.activateSoulOfFlame(start, 64));
        assertTrue(status.soulOfFlameActive(start));
        assertTrue(status.activateSoulOfFlame(
                start + TimeUnit.SECONDS.toNanos(10), 64));
        long refreshedExpiry = start + TimeUnit.SECONDS.toNanos(74);
        assertTrue(status.soulOfFlameActive(refreshedExpiry - 1));
        assertTrue(status.expireSoulOfFlame(refreshedExpiry));
        assertFalse(status.soulOfFlameActive(refreshedExpiry));

        assertFalse(status.activateSoulOfFlame(start, 64));
        assertTrue(status.cancelMagicEffects().soulOfFlame());
        assertFalse(status.soulOfFlameActive(start));
        assertFalse(status.cancelMagicEffects().soulOfFlame());
        assertFalse(status.activateSoulOfFlame(start, 64));
        status.clear();
        assertFalse(status.soulOfFlameActive(start));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateSoulOfFlame(-1, 64));
        assertThrows(IllegalArgumentException.class,
                () -> status.activateSoulOfFlame(start, 0));
    }

    @Test void venomResistBlocksEveryRetainedPoisonFamilyWithoutCuringExistingPoison() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(30);

        assertTrue(status.activatePoison(start, 30, 3_000));
        status.activateVenomResist(start, 320);
        assertTrue(status.poisonActive(start));
        assertTrue(status.venomResistActive(start));
        for (BattleTimedStatus.PoisonKind kind : BattleTimedStatus.PoisonKind.values()) {
            assertFalse(status.acceptsPoison(start, kind));
        }
        assertThrows(IllegalArgumentException.class,
                () -> status.acceptsPoison(start, null));

        assertFalse(status.expireVenomResist(
                start + TimeUnit.SECONDS.toNanos(320) - 1));
        assertTrue(status.expireVenomResist(
                start + TimeUnit.SECONDS.toNanos(320)));
        assertTrue(status.acceptsPoison(
                start + TimeUnit.SECONDS.toNanos(320),
                BattleTimedStatus.PoisonKind.DAMAGE));

        status.curePoison();
        status.activateVenomResist(start, 320);
        assertFalse(status.activatePoison(start, 30, 3_000));
        assertTrue(status.cancelMagicEffects().venomResist());
        assertFalse(status.venomResistActive(start));
        assertFalse(status.cancelMagicEffects().venomResist());
        assertThrows(IllegalArgumentException.class,
                () -> status.activateVenomResist(start, 0));
        status.activateVenomResist(start, 320);
        status.clear();
        assertFalse(status.venomResistActive(start));
    }

    @Test void doubleBrakeRefreshesExpiresClearsAndIsCancelled() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(35);
        status.activateDoubleBrake(start, 192);
        assertTrue(status.doubleBrakeActive(start));
        assertFalse(status.expireDoubleBrake(
                start + TimeUnit.SECONDS.toNanos(192) - 1));
        assertTrue(status.expireDoubleBrake(
                start + TimeUnit.SECONDS.toNanos(192)));
        assertFalse(status.doubleBrakeActive(
                start + TimeUnit.SECONDS.toNanos(192)));

        status.activateDoubleBrake(start, 192);
        assertTrue(status.cancelMagicEffects().doubleBrake());
        assertFalse(status.doubleBrakeActive(start));
        assertFalse(status.cancelMagicEffects().doubleBrake());
        assertThrows(IllegalArgumentException.class,
                () -> status.activateDoubleBrake(start, 0));
        status.activateDoubleBrake(start, 192);
        status.clear();
        assertFalse(status.doubleBrakeActive(start));
    }

    @Test void thunderGrabLocksWithoutRefreshAndUnlocksOnExpiryOrCancellation() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(50);
        assertTrue(status.activateThunderGrab(start, 2));
        assertTrue(status.thunderGrabActive(start));
        assertTrue(status.actionLocked(start));
        assertFalse(status.activateThunderGrab(
                start + TimeUnit.SECONDS.toNanos(1), 4));
        assertFalse(status.expireThunderGrab(
                start + TimeUnit.SECONDS.toNanos(2) - 1));
        assertTrue(status.expireThunderGrab(
                start + TimeUnit.SECONDS.toNanos(2)));
        assertFalse(status.actionLocked(
                start + TimeUnit.SECONDS.toNanos(2)));

        assertTrue(status.activateThunderGrab(start, 4));
        assertTrue(status.cancelMagicEffects().thunderGrab());
        assertFalse(status.thunderGrabActive(start));
        assertFalse(status.cancelMagicEffects().thunderGrab());
        assertThrows(IllegalArgumentException.class,
                () -> status.activateThunderGrab(start, 0));
        status.activateThunderGrab(start, 4);
        status.clear();
        assertFalse(status.thunderGrabActive(start));
    }

    @Test void horrorOfDeathDoesNotRefreshAndClearsOnExpiryCancellationOrLifecycle() {
        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(60);
        assertFalse(status.activateHorrorOfDeath(start, 60));
        assertTrue(status.horrorOfDeathActive(start));
        assertTrue(status.activateHorrorOfDeath(
                start + TimeUnit.SECONDS.toNanos(20), 60));
        assertFalse(status.expireHorrorOfDeath(
                start + TimeUnit.SECONDS.toNanos(59)));
        assertTrue(status.expireHorrorOfDeath(
                start + TimeUnit.SECONDS.toNanos(60)));
        assertFalse(status.horrorOfDeathActive(
                start + TimeUnit.SECONDS.toNanos(60)));

        assertFalse(status.activateHorrorOfDeath(start, 60));
        assertTrue(status.cancelMagicEffects().horrorOfDeath());
        assertFalse(status.horrorOfDeathActive(start));
        assertFalse(status.cancelMagicEffects().horrorOfDeath());
        assertThrows(IllegalArgumentException.class,
                () -> status.activateHorrorOfDeath(start, 0));
        status.activateHorrorOfDeath(start, 60);
        status.clear();
        assertFalse(status.horrorOfDeathActive(start));
    }
}
