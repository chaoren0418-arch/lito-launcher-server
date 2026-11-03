package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

final class BattleArmBreakerCatalogTest {
    @Test void selectedRowsPinDamageStatusAndSelectorMetadata() {
        BattleSpellCatalog.ArmBreakerSpell spell =
                BattleSpellCatalog.armBreakerSpell(
                        BattleSpellCatalog.ARM_BREAKER);
        BattleSpellCatalog.AttackSpell attack = spell.attackSpell();

        assertEquals(213, spell.skillId());
        assertEquals(26, spell.row());
        assertEquals(4, spell.column());
        assertEquals(18, attack.actionId());
        assertEquals(6_551, attack.effectGfx());
        assertEquals(25, attack.mpCost());
        assertEquals(1_200, attack.reuseDelayMillis());
        assertEquals(3, spell.maximumRange());
        assertEquals(15, attack.baseDamage());
        assertEquals(0, attack.diceCount());
        assertEquals(0, attack.dieSides());
        assertEquals(32, spell.clientCastClockMillis());
        assertEquals(12, spell.durationSeconds());
        assertEquals(3, spell.targetTo());
        assertEquals(64, spell.type());
        assertEquals(16, spell.nativeSkillId());
        assertEquals(-2, spell.meleeDamageModifier());
        assertEquals(1_026, spell.retainedStatusId());
        assertEquals(49_129L, spell.skillBookItemId());
        assertEquals("$5693", spell.skillBookNameId());
        assertEquals("$5947", spell.skillRowNameId());
        assertEquals(0, spell.dormantBossModifier());
        assertFalse(spell.counterMagicBypass());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());
        assertEquals("arm-breaker", spell.key());
        assertEquals(attack,
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.ARM_BREAKER));
        assertNull(BattleSpellCatalog.armBreakerSpell(212));
    }
}
