package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class ModernCombatantTest {
    private static final long OBJECT = 0x1_0000_0001L;

    @Test void allFourteenAppearancesUseTheExplicitPassiveTemplateNotDatabaseStats() {
        for (int type = 0; type <= 6; type++) for (int sex = 0; sex <= 1; sex++) {
            var template = ModernBotTemplate.roomTestDummy(type, sex);
            var bot = ModernCombatant.bot(ModernParticipantId.bot(7, OBJECT), template);
            assertEquals("ROOM_TEST_DUMMY_V1", template.templateId());
            assertEquals(1, template.templateVersion()); assertEquals(80, template.level());
            assertEquals(type, template.characterClass()); assertEquals(sex, template.sex());
            assertEquals(40, bot.initialHp()); assertEquals(40, bot.maxHp());
            assertEquals(0, bot.initialMp()); assertEquals(0, bot.maxMp());
            assertEquals(10, bot.armorClass()); assertEquals(40, template.magicResistance());
            assertEquals(0, bot.weaponType()); assertEquals(1, bot.attackRange());
            assertEquals(0, bot.evaluatedAtMillis()); assertEquals(-1, bot.validUntilMillis());
            assertTrue(bot.physicalSupported()); assertTrue(bot.magicSupported());
            assertTrue(bot.passive()); assertEquals(3, bot.lightArrowMpCost());
            assertTrue(template.learnedSkills().isEmpty());
            assertThrows(UnsupportedOperationException.class, () -> template.learnedSkills().add(4));
            assertFalse(bot.identity().isHuman());
            assertThrows(IllegalStateException.class, () -> bot.identity().humanKey());
        }
    }

    @Test void templateAppearanceAndTypedFactoryRejectInvalidOrHumanIdentity() {
        for (int type : new int[]{-1, 7, Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class, () -> ModernBotTemplate.roomTestDummy(type, 0));
        for (int sex : new int[]{-1, 2, Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class, () -> ModernBotTemplate.roomTestDummy(0, sex));
        assertThrows(IllegalArgumentException.class, () -> ModernCombatant.bot(
                ModernParticipantId.human(7, OBJECT), ModernBotTemplate.roomTestDummy(0, 0)));
        assertThrows(NullPointerException.class, () -> ModernCombatant.human(null));
        assertThrows(NullPointerException.class, () -> ModernCombatant.bot(null, ModernBotTemplate.roomTestDummy(0, 0)));
        assertThrows(NullPointerException.class, () -> ModernCombatant.bot(ModernParticipantId.bot(7, OBJECT), null));
    }

    @Test void humanAdapterRetainsActualQualifiedI64VitalsAndSourceIdentity() {
        var fighter = ModernMagicCombatTest.createFighter(42, row(6), 5_000_000_007L, 5_000_000_009L, 12, 0);
        var human = ModernCombatant.human(fighter); var profile = fighter.profile();
        assertEquals(ModernParticipantId.human(42, OBJECT), human.identity());
        assertEquals(profile.effective().initialHp(), human.initialHp());
        assertEquals(profile.effective().maximumHp(), human.maxHp());
        assertEquals(profile.effective().initialMp(), human.initialMp());
        assertEquals(profile.effective().maximumMp(), human.maxMp());
        assertEquals(profile.effective().armorClass(), human.armorClass());
        assertEquals(profile.evaluatedAtMillis(), human.evaluatedAtMillis());
        assertEquals(profile.validUntilMillis(), human.validUntilMillis());
        assertEquals(fighter.attackRange(), human.attackRange());
        assertEquals(fighter.magic().mpCost(), human.lightArrowMpCost());
        assertTrue(human.physicalSupported()); assertTrue(human.magicSupported()); assertFalse(human.passive());
        assertEquals(0, human.weaponType());
        assertFalse(human.toString().contains(row(6).name()));
        assertFalse(human.toString().contains(Long.toString(OBJECT)));
    }

    @Test void legacyHumanPhysicalOutcomesAndEveryRngBoundAreUnchangedForAllSupportedStances() {
        for (int type = 0; type <= 6; type++) for (long weapon : new long[]{0, 1, 2, 3, 5, 6, 14, 15}) {
            var attacker = ModernPhysicalCombatTest.createFighter(7, row(type), 100, 10, weapon);
            var defender = ModernPhysicalCombatTest.createFighter(42, row(1), 100, -30, 0);
            for (int seed = 0; seed < 16; seed++) {
                var legacyRandom = new RecordingRandom(seed); var typedRandom = new RecordingRandom(seed);
                var legacy = ModernPhysicalCombat.resolve(attacker, defender, 37, legacyRandom);
                var typed = ModernCombatant.resolvePhysical(ModernCombatant.human(attacker),
                        ModernCombatant.human(defender), 37, typedRandom);
                assertEquals(legacy, typed); assertEquals(legacyRandom.draws, typedRandom.draws);
            }
        }
    }

    @Test void legacyHumanMagicOutcomesAndEveryRngBoundAreUnchangedAcrossClasses() {
        for (int type = 0; type <= 6; type++) {
            var attacker = ModernMagicCombatTest.createFighter(7, row(type), 100, 50, 20, 12);
            var defender = ModernMagicCombatTest.createFighter(42, row(1), 100, 50, 12, 12);
            for (int seed = 0; seed < 24; seed++) {
                var legacyRandom = new RecordingRandom(seed); var typedRandom = new RecordingRandom(seed);
                var legacy = ModernMagicCombat.resolve(attacker.magic(), defender.magic(), 37, legacyRandom);
                var typed = ModernCombatant.resolveLightArrow(ModernCombatant.human(attacker),
                        ModernCombatant.human(defender), 37, typedRandom);
                assertEquals(legacy, typed); assertEquals(legacyRandom.draws, typedRandom.draws);
            }
        }
    }

    @Test void sameNumericHumanAndBotIdsAreDifferentAndMeleeUsesTheRealUnarmedFormula() {
        var human = ModernCombatant.human(ModernPhysicalCombatTest.createFighter(7, row(1), 100, 10, 0));
        var bot = dummy(7, OBJECT);
        assertNotEquals(human.identity(), bot.identity());
        var outcome = ModernCombatant.resolvePhysical(human, bot, 40, rolls(19, 0, 4));
        // Native unarmed die ((4+4)>>2) plus class1 level80/6, no invented fixed damage.
        assertEquals(new ModernPhysicalCombat.Outcome(true, 15, 15, 25, false, false, false), outcome);
        var terminal = ModernCombatant.resolvePhysical(human, bot, 1, rolls(19, 0, 4));
        assertEquals(15, terminal.rawDamage()); assertEquals(1, terminal.appliedDamage());
        assertEquals(0, terminal.remainingHp()); assertTrue(terminal.death());
        assertEquals(40, bot.initialHp()); // Pure result, no HP mutation or fake SQL update.
    }

    @Test void lightArrowReallyUsesDummyMrFortyAndDoesNotGrantOrSpendItsZeroMp() {
        var human = ModernCombatant.human(ModernMagicCombatTest.createFighter(7, row(1), 100, 50, 12, 0));
        var bot = dummy(7, OBJECT);
        // (base6+1)*35/32 = 7; MR40 => 80%, truncating to 5.
        var outcome = ModernCombatant.resolveLightArrow(human, bot, 40, rolls(0, 99));
        assertEquals(new ModernMagicCombat.Outcome(5, 5, 35, false, false), outcome);
        // Critical high die: floor(16*35/32)*3/2=25, then 80%=20.
        var critical = ModernCombatant.resolveLightArrow(human, bot, 7, rolls(9, 0));
        assertEquals(new ModernMagicCombat.Outcome(20, 7, 0, true, true), critical);
        assertEquals(0, bot.initialMp()); assertEquals(50, human.initialMp());
    }

    @Test void passiveBotCannotInitiatePhysicalOrMagicEvenWithAnExplicitNumericProfile() {
        var human = ModernCombatant.human(ModernMagicCombatTest.createFighter(7, row(1), 100, 50, 12, 0));
        var bot = dummy(42, OBJECT);
        assertPhysical(ModernPhysicalCombat.Failure.UNSUPPORTED,
                () -> ModernCombatant.resolvePhysical(bot, human, 100, noRandom()));
        assertMagic(ModernMagicCombat.Failure.UNSUPPORTED,
                () -> ModernCombatant.resolveLightArrow(bot, human, 100, noRandom()));
    }

    @Test void trueTypedSelfAndInvalidCurrentHpFailBeforeRandomWithoutChangingSnapshots() {
        var human = ModernCombatant.human(ModernMagicCombatTest.createFighter(7, row(1), 100, 50, 12, 0));
        var same = ModernCombatant.human(ModernMagicCombatTest.createFighter(7, row(1), 100, 50, 12, 0));
        assertPhysical(ModernPhysicalCombat.Failure.INVALID_STATE,
                () -> ModernCombatant.resolvePhysical(human, same, 40, noRandom()));
        assertMagic(ModernMagicCombat.Failure.INVALID_STATE,
                () -> ModernCombatant.resolveLightArrow(human, same, 40, noRandom()));
        var bot = dummy(42, OBJECT);
        for (long hp : new long[]{-1, 0, 41, Long.MAX_VALUE}) {
            assertPhysical(ModernPhysicalCombat.Failure.INVALID_STATE,
                    () -> ModernCombatant.resolvePhysical(human, bot, hp, noRandom()));
            assertMagic(ModernMagicCombat.Failure.INVALID_STATE,
                    () -> ModernCombatant.resolveLightArrow(human, bot, hp, noRandom()));
        }
        assertEquals(40, bot.initialHp());
    }

    @Test void missingMagicQualificationAndUnsupportedHumanWeaponAreNotBotFallbacks() {
        var plain = ModernCombatant.human(ModernPhysicalCombatTest.createFighter(7, row(1), 100, 10, 0));
        var ranged = ModernCombatant.human(ModernPhysicalCombatTest.createFighter(7, row(1), 100, 10, 4));
        var bot = dummy(42, OBJECT);
        assertFalse(plain.magicSupported());
        assertThrows(IllegalStateException.class, plain::lightArrowMpCost);
        assertMagic(ModernMagicCombat.Failure.UNSUPPORTED,
                () -> ModernCombatant.resolveLightArrow(plain, bot, 40, noRandom()));
        assertFalse(ranged.physicalSupported()); assertEquals(4, ranged.weaponType());
        assertPhysical(ModernPhysicalCombat.Failure.UNSUPPORTED,
                () -> ModernCombatant.resolvePhysical(ranged, bot, 40, noRandom()));
    }

    @Test void invalidRngAndOverflowRemainTypedFailuresAndNeverMutateBotVitals() {
        var human = ModernCombatant.human(ModernMagicCombatTest.createFighter(7, row(1), 100, 50, 12, 0));
        var bot = dummy(42, OBJECT);
        for (long invalid : new long[]{-1, Long.MAX_VALUE}) {
            assertPhysical(ModernPhysicalCombat.Failure.INVALID_RANDOM,
                    () -> ModernCombatant.resolvePhysical(human, bot, 40, bound -> invalid));
            assertMagic(ModernMagicCombat.Failure.INVALID_RANDOM,
                    () -> ModernCombatant.resolveLightArrow(human, bot, 40, bound -> invalid));
        }
        var huge = ModernCombatant.human(ModernMagicCombatTest.createFighter(
                7, row(1), 100, 50, Long.MAX_VALUE / 4, 0));
        assertMagic(ModernMagicCombat.Failure.ARITHMETIC_OVERFLOW,
                () -> ModernCombatant.resolveLightArrow(huge, bot, 40, rolls(9)));
        assertEquals(40, bot.initialHp()); assertEquals(0, bot.initialMp());
    }

    private static ModernApplicationProtocol.CharacterRow row(int type) {
        return new ModernApplicationProtocol.CharacterRow(OBJECT, "private-human", type, 0, 80);
    }
    private static ModernCombatant dummy(int source, long id) {
        return ModernCombatant.bot(ModernParticipantId.bot(source, id), ModernBotTemplate.roomTestDummy(1, 0));
    }
    private static ModernPhysicalCombat.BoundedRandom noRandom() {
        return bound -> { throw new AssertionError("invalid action must not consume RNG"); };
    }
    private static ModernPhysicalCombat.BoundedRandom rolls(long... values) {
        int[] index = {0};
        return bound -> {
            assertTrue(index[0] < values.length, "unexpected RNG draw");
            long value = values[index[0]++]; assertTrue(value >= 0 && value < bound); return value;
        };
    }
    private static void assertPhysical(ModernPhysicalCombat.Failure expected,
            org.junit.jupiter.api.function.Executable action) {
        assertEquals(expected, assertThrows(ModernPhysicalCombat.CombatException.class, action).failure());
    }
    private static void assertMagic(ModernMagicCombat.Failure expected,
            org.junit.jupiter.api.function.Executable action) {
        assertEquals(expected, assertThrows(ModernMagicCombat.CombatException.class, action).failure());
    }
    private static final class RecordingRandom implements ModernPhysicalCombat.BoundedRandom {
        final SplittableRandom random; final List<String> draws = new ArrayList<>();
        RecordingRandom(long seed) { random = new SplittableRandom(seed); }
        @Override public long nextLong(long bound) {
            long value = random.nextLong(bound); draws.add(bound + ":" + value); return value;
        }
    }
}
