package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

final class BattleDragonSkinCatalogTest {
    @Test void skills177Through180RemainNativeSelectorOnlyReservedSlots() {
        for (int skillId = 177; skillId <= 180; skillId++) {
            BattleSpellCatalog.ReservedSkillSlot slot =
                    BattleSpellCatalog.reservedSkillSlot(skillId);
            assertEquals(skillId, slot.skillId());
            assertEquals(22, slot.row());
            assertEquals(skillId - 177, slot.column());
            assertEquals(0, slot.skillLevel());
            assertEquals(0, slot.durationSeconds());
            assertEquals(0, slot.actionId());
            assertEquals(0, slot.castGfx());
            assertTrue(slot.templatePresent());
            assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                    slot.requestShape());
        }
        assertNull(BattleSpellCatalog.reservedSkillSlot(181));
    }

    @Test void dragonSkinPinsPrimaryRetainedResourceAndDamageContract() {
        BattleSpellCatalog.DragonSkinSpell spell =
                BattleSpellCatalog.dragonSkinSpell(
                        BattleSpellCatalog.DRAGON_SKIN);
        assertEquals(181, spell.skillId());
        assertEquals(22, spell.row());
        assertEquals(4, spell.column());
        assertEquals(23, spell.level());
        assertEquals(4, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(6_531, spell.effectGfx());
        assertEquals(0, spell.mpCost());
        assertEquals(15, spell.hpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(1_200, spell.durationSeconds());
        assertEquals(10, spell.damageReduction());
        assertTrue(spell.counterMagicBypass());
        assertEquals("dragon-skin", spell.key());
        assertNull(BattleSpellCatalog.dragonSkinSpell(180));
    }

    @Test void loopbackChainswordCarrierLearnsWorldBridgeAndDragonKnightBuffs()
            throws Exception {
        AccountRepository accounts = new MemoryAccountRepository("test", "test");
        assertEquals(Set.of(BattleSpellCatalog.WORLD_TREE_CALL,
                        BattleSpellCatalog.DRAGON_SKIN,
                        BattleSpellCatalog.BURNING_SLASH,
                        BattleSpellCatalog.GUARD_BRAKE,
                        BattleSpellCatalog.MAGMA_BREATH,
                        BattleSpellCatalog.AWAKEN_ANTHARAS,
                        BattleSpellCatalog.BLOODLUST,
                        BattleSpellCatalog.FOE_SLAYER,
                        BattleSpellCatalog.RESIST_FEAR,
                         BattleSpellCatalog.SHOCK_SKIN,
                         BattleSpellCatalog.AWAKEN_FAFURION,
                         BattleSpellCatalog.MORTAL_BODY,
                         BattleSpellCatalog.THUNDER_GRAB,
                         BattleSpellCatalog.HORROR_OF_DEATH,
                         BattleSpellCatalog.FREEZING_BREATH,
                         BattleSpellCatalog.AWAKEN_VALAKAS),
                accounts.learnedSkills(18));
    }
}
