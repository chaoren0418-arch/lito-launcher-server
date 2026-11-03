package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ProtocolSpellMaterialTest {
    @Test
    void parsesExactNpcRequestAndModeSevenMaterialSelection() {
        Protocol.SpellMaterialShopRequest list = Protocol.spellMaterialShopRequest(
                new Protocol.Writer().c(Protocol.C_EXCHANGEABLE_SPELL)
                        .d(0x6500_0001L).bytes());
        assertEquals(0x6500_0001L, list.npcObjectId());

        Protocol.SpellMaterialPurchaseRequest purchase =
                Protocol.spellMaterialPurchaseRequest(new Protocol.Writer()
                        .c(Protocol.C_EXCHANGE_SPELL).h(2).d(1).d(15).bytes());
        assertEquals(List.of(1, 15), purchase.wireSkillIds());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.spellMaterialShopRequest(new Protocol.Writer()
                        .c(Protocol.C_EXCHANGEABLE_SPELL).d(1).c(0).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.spellMaterialPurchaseRequest(new Protocol.Writer()
                        .c(Protocol.C_EXCHANGE_SPELL).h(0).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.spellMaterialPurchaseRequest(new Protocol.Writer()
                        .c(Protocol.C_EXCHANGE_SPELL).h(1).d(220).bytes()));
    }

    @Test
    void writesExactChRowsEmptyHeaderAndCdShortage() {
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_EXCHANGEABLE_SPELL_LIST)
                        .h(3).d(1).d(2).d(15).bytes(),
                Protocol.exchangeableSpellList(List.of(1, 2, 15)));
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_EXCHANGEABLE_SPELL_LIST)
                        .h(0).bytes(),
                Protocol.exchangeableSpellList(List.of()));
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_NOT_ENOUGH_FOR_SPELL)
                        .d(15).bytes(),
                Protocol.notEnoughForSpell(15));
    }
}
