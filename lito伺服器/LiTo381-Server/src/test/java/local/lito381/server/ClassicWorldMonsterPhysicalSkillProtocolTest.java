package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HexFormat;
import org.junit.jupiter.api.Test;

final class ClassicWorldMonsterPhysicalSkillProtocolTest {
    @Test
    void ogreUsesOneExactPrimaryAttackWithoutAttackAllOrSecondaryActions() {
        byte[] packet = Protocol.physicalDamageAttack(
                30, 0x6500_1012L, 16, 42, 6);
        assertEquals(16, packet.length);
        assertEquals("231E12100065100000002A0600000000",
                HexFormat.of().withUpperCase().formatHex(packet));
    }

    @Test
    void fireBeastGfxIsAnIndependentObjectBoundEffectBeforeItsAttack() {
        byte[] effect = Protocol.effectObject(0x6500_11D9L, 954);
        assertEquals("37D9110065BA03",
                HexFormat.of().withUpperCase().formatHex(effect));
        byte[] attack = Protocol.physicalDamageAttack(
                3, 0x6500_11D9L, 16, 21, 6);
        assertEquals("2303D911006510000000150600000000",
                HexFormat.of().withUpperCase().formatHex(attack));
    }
}
