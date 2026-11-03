package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class NativeMagicDollPowersTest {
    @Test
    void preservesAllNativeColumnsAndGroupsOnlyValueMain() {
        SourceMagicDollPower strength = power(1, "PASSIVE_STR", 2,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        SourceMagicDollPower active = power(120, "ACTIVE_SKILL", 0,
                300, 0, 0, 0, 2, 5935, 1, 0, 17, 0, 0);
        SourceMagicDollPower timed = power(132, "TIMED_ITEM", 0,
                0, 60, 0, 0, 0, 0, 0, 0, 0, 40308, 2);
        SourceMagicDollPower auto = power(130, "AUTO_SKILL", 0,
                0, 0, 42, 0, 0, 0, 0, 0, 0, 0, 0);
        SourceMagicDollType type = new SourceMagicDollType(
                430000, 1, 0, List.of(1, 120, 132, 130), List.of(), List.of(),
                1800, 1204, "$1234", List.of(strength, active, timed, auto));

        NativeMagicDollPowers powers = NativeMagicDollPowers.from(type);
        assertEquals(2, powers.value("PASSIVE_STR"));
        assertEquals(List.of(active), powers.activeDefinitions());
        assertEquals(List.of(timed), powers.timedDefinitions());
        assertEquals(300, powers.activeDefinitions().get(0).probability());
        assertEquals(17, powers.activeDefinitions().get(0).triggerSkillId());
        assertEquals(40308, powers.timedDefinitions().get(0).itemId());
        assertEquals(2, powers.timedDefinitions().get(0).itemCount());
        assertEquals(List.of(42), powers.autoSkillIds());
        assertEquals(1.02D, powers.percentMultiplier("PASSIVE_STR"), 0.000001D);
    }

    @Test
    void rejectsDefinitionsThatDoNotExactlyMatchTypePowerOrder() {
        SourceMagicDollPower strength = power(1, "PASSIVE_STR", 1,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> new SourceMagicDollType(
                430000, 1, 0, List.of(2), List.of(), List.of(),
                1800, 1204, "$1234", List.of(strength)));
    }

    @Test
    void emptyProjectionHasNoSyntheticAbility() {
        NativeMagicDollPowers powers = NativeMagicDollPowers.empty();
        assertEquals(0, powers.value("PASSIVE_STR"));
        assertTrue(powers.definitions().isEmpty());
        assertTrue(powers.activeDefinitions().isEmpty());
        assertTrue(powers.timedDefinitions().isEmpty());
        assertEquals(1.0D,
                powers.percentMultiplier("PASSIVE_EXP_RATE"), 0.000001D);
    }

    @Test
    void rejectsRowsThatTheRetainedDollPowerTableRejects() {
        assertThrows(IllegalArgumentException.class,
                () -> power(200, "UNKNOWN", 1,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> power(201, "PASSIVE_STR", 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> power(202, "ACTIVE_SKILL", 0,
                        300, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> power(203, "TIMED_ITEM", 0,
                        0, 60, 0, 0, 0, 0, 0, 0, 0, 0, 1));

        SourceMagicDollPower haste = power(204, "STATUS_HASTE", 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        SourceMagicDollPower autoSkill = power(205, "AUTO_SKILL", 0,
                0, 0, 42, 0, 0, 0, 0, 0, 0, 0, 0);
        assertEquals("STATUS_HASTE", haste.className());
        assertEquals(42, autoSkill.passiveSkillId());
    }

    private static SourceMagicDollPower power(
            int id, String className, int valueMain, int probability,
            int interval, int passiveSkill, int mode, int attr, int gfx,
            int gfxTarget, int valueSub, int triggerSkill, int itemId,
            int itemCount) {
        return new SourceMagicDollPower(id, "native-" + id, className,
                valueMain, probability, interval, passiveSkill, mode, attr,
                gfx, gfxTarget, valueSub, triggerSkill, itemId, itemCount, 0);
    }
}
