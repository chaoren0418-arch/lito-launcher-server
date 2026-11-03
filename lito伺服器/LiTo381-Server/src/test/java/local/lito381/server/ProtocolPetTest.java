package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

final class ProtocolPetTest {
    @Test void emitsRetainedPetPackWithExpLawfulMasterAndOwnerOnlyHpTail() {
        GameStateStore.PetState durable = new GameStateStore.PetState(
                "legacy\u001f10231", 0x7000_3810L, 0x9000_0038L,
                (int) InventoryCatalog.PET_COLLAR, 45034,
                "牧羊犬", 12, 78, 17, 21_375, -7);
        BattleCompanion.Snapshot pet = BattleCompanion.pet(
                durable, SourcePetTemplate.memoryFixture(45034), 100, 120).snapshot();

        byte[] owner = Protocol.classicPet(pet, "TestMage", true);
        byte[] peer = Protocol.classicPet(pet, "TestMage", false);

        assertEquals(Protocol.S_PUT_OBJECT, Byte.toUnsignedInt(owner[0]));
        ByteBuffer fields = ByteBuffer.wrap(owner).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(100, Short.toUnsignedInt(fields.getShort(1)));
        assertEquals(120, Short.toUnsignedInt(fields.getShort(3)));
        assertEquals(0x9000_0038L, Integer.toUnsignedLong(fields.getInt(5)));
        assertEquals(21_375L, Integer.toUnsignedLong(fields.getInt(15)));
        assertEquals(-7, fields.getShort(19));
        assertEquals(100, Byte.toUnsignedInt(owner[owner.length - 6]));
        assertEquals(0xFF, Byte.toUnsignedInt(peer[peer.length - 6]));
        assertEquals(owner.length, peer.length);
        assertNotEquals(Byte.toUnsignedInt(owner[owner.length - 6]),
                Byte.toUnsignedInt(peer[peer.length - 6]));
        assertEquals(0xFF, Byte.toUnsignedInt(owner[owner.length - 1]));
        assertEquals(0xFF, Byte.toUnsignedInt(owner[owner.length - 2]));
    }
}
