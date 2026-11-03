package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BattleSmashCatalogTest {
    @Test void selectedRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(25).c(2).bytes();
        assertArrayEquals(new byte[] {0x14, 0x19, 0x02}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.SMASH, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(25).c(2).d(17).bytes()));
    }

    @Test void pinsPrimaryDatabaseClientAndRetainedAttackContract() {
        BattleSpellCatalog.SmashSpell spell =
                BattleSpellCatalog.smashSpell(BattleSpellCatalog.SMASH);
        BattleSpellCatalog.AttackSpell attack = spell.attackSpell();
        assertEquals(203, spell.skillId());
        assertEquals(25, spell.row());
        assertEquals(2, spell.column());
        assertEquals(18, attack.actionId());
        assertEquals(6_526, attack.effectGfx());
        assertEquals(7, attack.mpCost());
        assertEquals(600, attack.reuseDelayMillis());
        assertEquals(1, spell.maximumRange());
        assertEquals(50, attack.baseDamage());
        assertEquals(2, attack.diceCount());
        assertEquals(10, attack.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED,
                attack.attribute());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(3, spell.targetTo());
        assertEquals(64, spell.type());
        assertEquals(4, spell.nativeSkillId());
        assertEquals(49_119L, spell.skillBookItemId());
        assertEquals("$5683", spell.skillBookNameId());
        assertEquals("$5937", spell.skillRowNameId());
        assertEquals("暴擊", spell.skillsItemLabel());
        assertEquals("smash-energy", spell.key());
        assertEquals(attack, BattleSpellCatalog.attackSpell(203));
        assertEquals(5, attack.effectiveMpCost(18, 2));
        assertNull(BattleSpellCatalog.smashSpell(202));
    }

    @Test void sourceDamageBoundsAreFiftyPlusTwoD10() {
        BattleSpellCatalog.AttackSpell attack =
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.SMASH);
        int minimum = attack.baseDamage() + attack.diceCount();
        int maximum = attack.baseDamage()
                + attack.diceCount() * attack.dieSides();
        assertEquals(52, minimum);
        assertEquals(70, maximum);
    }
}
