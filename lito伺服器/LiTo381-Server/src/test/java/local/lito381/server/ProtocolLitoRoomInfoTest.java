package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ProtocolLitoRoomInfoTest {
    private static final long ROOM_ID = 0x0102_0304L;

    private static GameHub.RoomSettings settings() {
        return new GameHub.RoomSettings("Arena", "1234", 4,
                GameHub.RoomSettings.PASSWORD_PRESENT | GameHub.RoomSettings.ALLOW_OBSERVERS,
                2, 3, 2, 5, 6, List.of(7, 8));
    }

    @Test void subtypeZeroUsesNativeSummaryModeMapAndOwnerLayout() {
        assertArrayEquals(new byte[] {
                (byte) Protocol.S_LITO_GAME_INFO, 0, 4, 3, 2, 1,
                'A', 'r', 'e', 'n', 'a', 0,
                2, 0x34, 0x02, 0,
                'H', 'o', 's', 't', 0,
                0, 0, 0, 0
        }, Protocol.litoRoomSummary(ROOM_ID, settings(), 0x0234, "Host"));
    }

    @Test void subtypeOneUsesNativeEditableConfigurationLayout() {
        assertArrayEquals(new byte[] {
                (byte) Protocol.S_LITO_GAME_INFO, 1, 4, 3, 2, 1,
                'A', 'r', 'e', 'n', 'a', 0,
                '1', '2', '3', '4', 0,
                0x0C, 4, 3, 2, 5, 6, 2, 7, 8
        }, Protocol.litoRoomConfiguration(ROOM_ID, settings()));

        GameHub.RoomSettings capacity200 = new GameHub.RoomSettings(
                "Arena", "", 200, 0x08, 1, 0, 0, 0, 0, List.of());
        byte[] boundary = Protocol.litoRoomConfiguration(ROOM_ID, capacity200);
        assertArrayEquals(new byte[] {
                (byte) Protocol.S_LITO_GAME_INFO, 1, 4, 3, 2, 1,
                'A', 'r', 'e', 'n', 'a', 0, 0, 0x08, (byte) 200,
                0, 0, 0, 0, 0
        }, boundary);
    }

    @Test void subtypeFiveUsesNativeOwnerStringLayout() {
        assertArrayEquals(new byte[] {
                (byte) Protocol.S_LITO_GAME_INFO, 5, 4, 3, 2, 1,
                'H', 'o', 's', 't', 0
        }, Protocol.litoRoomOwner(ROOM_ID, "Host"));
    }

    @Test void subtypeTwoAndThreeUseNativeModeAndMapScalarLayouts() {
        assertArrayEquals(new byte[] {
                (byte) Protocol.S_LITO_GAME_INFO, 2, 4, 3, 2, 1, 6
        }, Protocol.litoRoomMode(ROOM_ID, BattleRules.Mode.TOWN));
        assertArrayEquals(new byte[] {
                (byte) Protocol.S_LITO_GAME_INFO, 3, 4, 3, 2, 1, (byte) 0x9C, (byte) 0xFF
        }, Protocol.litoRoomMap(ROOM_ID, 0xFF9C));
        assertArrayEquals(new byte[] {
                (byte) Protocol.S_LITO_GAME_INFO, 3, 4, 3, 2, 1, (byte) 0xFF, (byte) 0xFF
        }, Protocol.litoRoomMap(ROOM_ID, 0xFFFF));
    }

    @Test void subtypesFourAndSixThroughNineUseExactSummaryScalarLayouts() {
        int[] subtypes = {4, 6, 7, 8, 9};
        for (int index = 0; index < subtypes.length; index++) {
            assertArrayEquals(new byte[] {
                    (byte) Protocol.S_LITO_GAME_INFO, (byte) subtypes[index],
                    4, 3, 2, 1, (byte) (0xF0 + index)
            }, Protocol.litoRoomSummaryScalar(ROOM_ID, index, 0xF0 + index));
        }
    }

    @Test void configurationRejectsValuesThatOverflowNativeBuffers() {
        GameHub.RoomSettings longPassword = new GameHub.RoomSettings(
                "Arena", "12345", 4, 0, 2, 3, 4, 5, 6, List.of());
        GameHub.RoomSettings tooManySelections = new GameHub.RoomSettings(
                "Arena", "", 4, 0, 2, 3, 4, 5, 6,
                List.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
        GameHub.RoomSettings capacityOne = new GameHub.RoomSettings(
                "Arena", "", 1, 0, 1, 0, 0, 0, 0, List.of());
        GameHub.RoomSettings capacity201 = new GameHub.RoomSettings(
                "Arena", "", 201, 0, 1, 0, 0, 0, 0, List.of());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomConfiguration(ROOM_ID, longPassword));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomConfiguration(ROOM_ID, tooManySelections));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomConfiguration(ROOM_ID, capacityOne));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomConfiguration(ROOM_ID, capacity201));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomOwner(ROOM_ID, ""));
        assertThrows(IllegalArgumentException.class,
                () -> BattleRules.Mode.fromWire(0x100));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomMap(ROOM_ID, -1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomMap(ROOM_ID, 0x1_0000));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomSummaryScalar(ROOM_ID, -1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomSummaryScalar(ROOM_ID, 5, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomSummaryScalar(ROOM_ID, 0, 0x100));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomMode(0, BattleRules.Mode.PET_RACE));
    }
}
