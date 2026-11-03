package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HexFormat;
import org.junit.jupiter.api.Test;

final class ClassicWorldMonsterStatusProtocolTest {
    @Test
    void watcherCurseUsesExactSelectedClientWarningActionEffectAndLockBytes() {
        HexFormat hex = HexFormat.of().withUpperCase();
        assertEquals("10D40000",
                hex.formatHex(Protocol.messageCode(212)));
        assertEquals("32650000000001",
                hex.formatHex(Protocol.poison(101, 2)));
        assertEquals("200000006613",
                hex.formatHex(Protocol.objectAction(0x6600_0000L, 19)));
        assertEquals("3765000000EA02",
                hex.formatHex(Protocol.effectObject(101, 746)));
        assertEquals("2502", hex.formatHex(Protocol.paralysis(
                Protocol.ParalysisMode.PARALYSIS_APPLY)));
        assertEquals("2503", hex.formatHex(Protocol.paralysis(
                Protocol.ParalysisMode.PARALYSIS_RELEASE)));
        assertEquals("32650000000000",
                hex.formatHex(Protocol.poison(101, 0)));
    }
}
