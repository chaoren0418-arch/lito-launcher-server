package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BattleMirrorImageCatalogTest {
    @Test void selectedRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(25).c(0).bytes();
        assertArrayEquals(new byte[] {0x14, 0x19, 0x00}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.MIRROR_IMAGE, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(25).c(0).d(17).bytes()));
    }

    @Test void pinsSelectedDatabaseClientAndRetainedDodgeContract() {
        BattleSpellCatalog.DodgeBuffSpell spell =
                BattleSpellCatalog.dodgeBuffSpell(BattleSpellCatalog.MIRROR_IMAGE);
        assertEquals(201, spell.skillId());
        assertEquals(25, spell.row());
        assertEquals(0, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(6_813, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(1_800, spell.clientCastClockMillis());
        assertEquals(1_200, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(1, spell.nativeId());
        assertEquals(5, spell.dodgeModifier());
        assertEquals("mirror-image", spell.key());
        assertNull(BattleSpellCatalog.dodgeBuffSpell(200));
    }

    @Test void originalIllusionistInt13PaysExactTenMp() {
        BattleSpellCatalog.DodgeBuffSpell spell =
                BattleSpellCatalog.dodgeBuffSpell(BattleSpellCatalog.MIRROR_IMAGE);
        assertEquals(0, BattleSpellCatalog.originalMagicConsumeReduction(6, 13));
        assertEquals(10, spell.effectiveMpCost(13, 0));
        assertEquals(8, spell.effectiveMpCost(13, 2));
    }
}
