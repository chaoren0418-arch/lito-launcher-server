package local.lito381.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ProtocolSummonTest {
    @Test
    void parsesAllFourNativeSummonWireFamiliesAndRejectsCrossModeShapes() {
        Protocol.SummonRequest summon = parse("Dire Wolf", 0);
        assertEquals("Dire Wolf", summon.argument());
        assertEquals(0, summon.mode());
        assertNull(summon.firstValue());
        assertNull(summon.secondValue());

        assertEquals(1, parse("X".repeat(0x7F), 1).mode());

        Protocol.SummonRequest summonBot = Protocol.summonRequest(new Protocol.Writer()
                .c(Protocol.C_SUMMON).s("").c(2).d(0xFFFF_FFFFL).bytes());
        assertEquals(0xFFFF_FFFFL, summonBot.firstValue());
        assertNull(summonBot.secondValue());

        assertEquals(3, parse("", 3).mode());
        assertEquals(4, parse("X".repeat(0xFF), 4).mode());
        assertEquals(5, parse("", 5).mode());
        assertEquals(6, parse("target name", 6).mode());
        assertEquals(7, parse("", 7).mode());

        Protocol.SummonRequest askBot = Protocol.summonRequest(new Protocol.Writer()
                .c(Protocol.C_SUMMON).s("X".repeat(0x0F)).c(8)
                .d(0).d(0xFFFF_FFFFL).bytes());
        assertEquals(8, askBot.mode());
        assertEquals(0L, askBot.firstValue());
        assertEquals(0xFFFF_FFFFL, askBot.secondValue());

        assertInvalid(new Protocol.Writer().c(Protocol.C_TELEPORT).s("wolf").c(0).bytes());
        assertInvalid(new byte[] {(byte) Protocol.C_SUMMON, 'x'});
        assertInvalid(new Protocol.Writer().c(Protocol.C_SUMMON).s("wolf").c(9).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SUMMON).s("").c(0).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SUMMON).s("X".repeat(0x80)).c(1).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SUMMON).s("not-empty").c(2).d(1).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SUMMON).s("").c(2).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SUMMON).s("not-empty").c(3).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SUMMON).s("X".repeat(0x100)).c(4).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SUMMON).s("not-empty").c(5).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SUMMON).s("X".repeat(0x10)).c(8).d(1).d(2).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SUMMON).s("ask").c(8).d(1).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_SUMMON).s("wolf").c(0).c(0).bytes());
    }

    private static Protocol.SummonRequest parse(String argument, int mode) {
        return Protocol.summonRequest(new Protocol.Writer()
                .c(Protocol.C_SUMMON).s(argument).c(mode).bytes());
    }

    private static void assertInvalid(byte[] payload) {
        assertThrows(IllegalArgumentException.class, () -> Protocol.summonRequest(payload));
    }
}
