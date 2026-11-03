package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ProtocolSpellShopTest {
    @Test
    void parsesExactNpcListRequestAndModeSevenSelection() {
        Protocol.SpellShopRequest list = Protocol.spellShopRequest(new Protocol.Writer()
                .c(Protocol.C_BUYABLE_SPELL).d(0x6400_0001L).bytes());
        assertEquals(0x6400_0001L, list.npcObjectId());

        Protocol.SpellPurchaseRequest purchase = Protocol.spellPurchaseRequest(
                new Protocol.Writer().c(Protocol.C_BUY_SPELL).h(2).d(1).d(15).bytes());
        assertEquals(List.of(1, 15), purchase.wireSkillIds());
        assertThrows(IllegalArgumentException.class, () -> Protocol.spellPurchaseRequest(
                new Protocol.Writer().c(Protocol.C_BUY_SPELL).h(0).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.spellPurchaseRequest(
                new Protocol.Writer().c(Protocol.C_BUY_SPELL).h(1).d(220).bytes()));
    }

    @Test
    void writesExactDhRowsAndNativeEmptyNpcTail() {
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_BUYABLE_SPELL_LIST)
                        .d(6_000).h(3).d(1).d(2).d(15).bytes(),
                Protocol.buyableSpellList(6_000, List.of(1, 2, 15), 0x6400_0001L));
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_BUYABLE_SPELL_LIST)
                        .d(6_000).h(0).d(0x6400_0001L).bytes(),
                Protocol.buyableSpellList(6_000, List.of(), 0x6400_0001L));
    }
}
