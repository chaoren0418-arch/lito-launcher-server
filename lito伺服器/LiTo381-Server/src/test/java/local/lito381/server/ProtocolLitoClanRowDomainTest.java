package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

final class ProtocolLitoClanRowDomainTest {
    @Test
    void exactNativeRoleAndActivityDomainsAreTyped() {
        assertArrayEquals(new int[] {1, 2, 3, 4},
                Arrays.stream(Protocol.LitoClanRole.values())
                        .mapToInt(Protocol.LitoClanRole::wireValue).toArray());
        assertEquals(Protocol.LitoClanRole.ROYAL,
                Protocol.LitoClanRole.fromWire(1));
        assertEquals(Protocol.LitoClanRole.OWNER,
                Protocol.LitoClanRole.fromWire(4));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.LitoClanRole.fromWire(0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.LitoClanRole.fromWire(5));

        assertArrayEquals(new int[] {0, 1, 2},
                Arrays.stream(Protocol.LitoClanActivity.values())
                        .mapToInt(Protocol.LitoClanActivity::wireValue).toArray());
        assertEquals(Protocol.LitoClanActivity.WAITING,
                Protocol.LitoClanActivity.fromWire(0));
        assertEquals(Protocol.LitoClanActivity.FIGHTING,
                Protocol.LitoClanActivity.fromWire(1));
        assertEquals(Protocol.LitoClanActivity.OFFLINE,
                Protocol.LitoClanActivity.fromWire(2));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.LitoClanActivity.fromWire(-1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.LitoClanActivity.fromWire(3));
    }

    @Test
    void rowBoundaryRejectsUnrenderableStyleAndPaletteValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameHub.ClanMemberSnapshot("Mage",
                        Protocol.LitoClanRole.MEMBER, 5, 0, "",
                        Protocol.LitoClanActivity.WAITING));
        assertThrows(IllegalArgumentException.class,
                () -> new GameHub.ClanMemberSnapshot("Mage",
                        Protocol.LitoClanRole.MEMBER, 0, 0xC3, "",
                        Protocol.LitoClanActivity.WAITING));
        assertThrows(NullPointerException.class,
                () -> new GameHub.ClanMemberSnapshot("Mage",
                        Protocol.LitoClanRole.MEMBER, 0, 0, "", null));
    }
}
