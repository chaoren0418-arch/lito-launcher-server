package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.List;
import org.junit.jupiter.api.Test;

final class NativeMagicDollEffectsTest {
    @Test
    void attackAndDefenseUseIndependentPerMilleRollsAndNativeModes() {
        NativeMagicDollPowers powers = powers(
                power(120, "ACTIVE_SKILL", 0, 300, 0, 0, 0, 0, 0, 74, 0, 0),
                power(121, "ACTIVE_DMG", 0, 500, 0, 1, 2, 5528, 50, 0, 0, 0),
                power(122, "ACTIVE_REDUX", 0, 100, 0, 0, 0, 0, 10, 0, 0, 0));
        ArrayDeque<Integer> attackRolls = new ArrayDeque<>(List.of(299, 500));
        NativeMagicDollEffects.AttackProc attack =
                NativeMagicDollEffects.attack(powers, attackRolls::removeFirst);
        assertTrue(attack.triggered());
        assertEquals(0, attack.additionalDamage());
        assertEquals(List.of(120), attack.skillTriggers().stream()
                .map(SourceMagicDollPower::id).toList());

        NativeMagicDollEffects.DefenseProc defense =
                NativeMagicDollEffects.defense(powers, () -> 99);
        assertEquals(10, defense.damageReduction());
        assertEquals(List.of(122), defense.triggers().stream()
                .map(SourceMagicDollPower::id).toList());
        assertThrows(IllegalArgumentException.class,
                () -> NativeMagicDollEffects.attack(powers, () -> 1000));
    }

    @Test
    void periodicCadencePausesFullVitalsAndResetsAfterAward() {
        NativeMagicDollPowers powers = powers(
                power(133, "TIMED_HP_REGEN", 20, 0, 2, 0, 0, 0, 0, 0, 0, 0),
                power(135, "TIMED_MP_REGEN", 10, 0, 3, 0, 0, 0, 0, 0, 0, 0),
                power(132, "TIMED_ITEM", 0, 0, 2, 0, 0, 0, 0, 0, 40308, 100));
        NativeMagicDollEffects.PeriodicState state =
                NativeMagicDollEffects.PeriodicState.from(powers);
        assertTrue(state.advance(false, true).isEmpty());
        List<NativeMagicDollEffects.PeriodicAward> second =
                state.advance(true, true);
        assertEquals(List.of(NativeMagicDollEffects.AwardType.ITEM),
                second.stream().map(NativeMagicDollEffects.PeriodicAward::type).toList());
        List<NativeMagicDollEffects.PeriodicAward> third =
                state.advance(true, true);
        assertEquals(List.of(NativeMagicDollEffects.AwardType.HP,
                        NativeMagicDollEffects.AwardType.MP),
                third.stream().map(NativeMagicDollEffects.PeriodicAward::type).toList());
        assertFalse(state.advance(true, true).isEmpty());
    }

    private static NativeMagicDollPowers powers(SourceMagicDollPower... rows) {
        List<SourceMagicDollPower> definitions = List.of(rows);
        SourceMagicDollType type = new SourceMagicDollType(55000, 0, 0,
                definitions.stream().map(SourceMagicDollPower::id).toList(),
                List.of(), List.of(), 1800, 6100, "doll", definitions);
        return NativeMagicDollPowers.from(type);
    }

    private static SourceMagicDollPower power(
            int id, String name, int main, int probability, int interval,
            int mode, int attr, int gfx, int sub, int passiveSkill,
            int itemId, int itemCount) {
        return new SourceMagicDollPower(id, name, name, main, probability,
                interval, passiveSkill, mode, attr, gfx, gfx > 0 ? 1 : 0,
                sub, name.equals("ACTIVE_SKILL") ? 74 : 0,
                itemId, itemCount, 0);
    }
}
