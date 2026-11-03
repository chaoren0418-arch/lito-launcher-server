package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ProtocolClassicCharacterTest {
    @Test void parsesNativeTitleAndRollRequests() {
        assertEquals(new Protocol.TitleRequest("TestMage", "Arena Hero"),
                Protocol.titleRequest(new Protocol.Writer().c(Protocol.C_TITLE)
                        .s("TestMage").s("Arena Hero").bytes()));
        assertEquals(4, Protocol.characterRollClass(
                new Protocol.Writer().c(Protocol.C_REQUEST_ROLL).c(4).bytes()));
        assertEquals("Arena Gate", Protocol.bookmarkName(
                new Protocol.Writer().c(Protocol.C_BOOKMARK).s("Arena Gate").bytes(),
                Protocol.C_BOOKMARK));
        assertEquals("Arena Gate", Protocol.bookmarkName(
                new Protocol.Writer().c(Protocol.C_DELETE_BOOKMARK).s("Arena Gate").bytes(),
                Protocol.C_DELETE_BOOKMARK));
        assertEquals(0x12, Protocol.weatherControl(
                new byte[] {(byte) Protocol.C_CONTROL_WEATHER, 0x12}));
    }

    @Test void writesNativeTitleAndSixAttributeRoll() {
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_TITLE)
                        .d(0x1122_3344L).s("Hero").bytes(),
                Protocol.title(0x1122_3344L, "Hero"));
        assertArrayEquals(new byte[] {0x48, 13, 10, 10, 11, 13, 10},
                Protocol.characterRollResult(new int[] {13, 10, 10, 11, 13, 10}));
        assertArrayEquals(new byte[] {0x05}, Protocol.newCharacterInfo());
        assertArrayEquals(new byte[] {0x06, 0x06, 0x00, 0x00, 0x00, 0x00},
                Protocol.createCharacterFailure(
                        Protocol.CreateCharacterFailure.ALREADY_EXISTS, 0));
        assertArrayEquals(new byte[] {0x07, 0x03, 0x00, 0x00, 0x00, 0x00},
                Protocol.enterWorldCheck(Protocol.EnterWorldCheckResult.ACCEPTED, 0));
        assertArrayEquals(new byte[] {0x09, 0x05, 0x00, 0x00, 0x00, 0x00},
                Protocol.deleteCharacterCheck(
                        Protocol.DeleteCharacterCheckResult.DELETE_NOW, 0));
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_ADD_BOOKMARK)
                        .s("Arena Gate").h(32730).h(32798).h(509).bytes(),
                Protocol.addBookmark("Arena Gate", 32730, 32798, 509));
        assertArrayEquals(new byte[] {0x33, 0x12}, Protocol.weather(0x12));
        assertArrayEquals(new byte[] {0x21, 0x00, 0x10, 0x5E, 0x5F},
                Protocol.worldTime(1_600_000_000L));
    }

    @Test void narrowsRetainedTitleWithoutSplittingMs950Characters() {
        assertEquals("1234567890123456",
                Protocol.nativeTitleValue("12345678901234567890"));
        assertEquals("測試稱號12345678",
                Protocol.nativeTitleValue("測試稱號123456789"));
        assertEquals("ABCD", Protocol.nativeTitleValue("AB\u0000😀CD"));
        assertEquals("", Protocol.nativeTitleValue(null));
    }

    @Test void parsesNativeCustomCharacterRequest() {
        Protocol.CustomCharacterRequest request = Protocol.customCharacter(
                new Protocol.Writer().c(Protocol.C_CREATE_CUSTOM_CHARACTER)
                        .s("NativeHero").c(0).c(4)
                        .c(12).c(15).c(8).c(10).c(9).c(11)
                        .c(0).c(0).bytes());
        assertEquals("NativeHero", request.name());
        assertEquals(0, request.sex());
        assertEquals(4, request.characterClass());
        assertEquals(List.of(12, 15, 8, 10, 9, 11), request.attributes());
    }

    @Test void parsesSameFamilyClassicCharacterNameRequests() {
        assertEquals("NativeHero", Protocol.classicCharacterRequest(
                new Protocol.Writer().c(Protocol.C_ENTER_WORLD).s("NativeHero").bytes(),
                Protocol.C_ENTER_WORLD).name());
        assertEquals("NativeHero", Protocol.classicCharacterRequest(
                new Protocol.Writer().c(Protocol.C_DELETE_CHARACTER).s("NativeHero").bytes(),
                Protocol.C_DELETE_CHARACTER).name());
    }

    @Test void rollsOnlyValuesAcceptedByTheNativeClassTable() {
        for (int characterClass = 0; characterClass < 5; characterClass++) {
            for (int attempt = 0; attempt < 100; attempt++) {
                assertTrue(CharacterRollCatalog.contains(
                        characterClass, CharacterRollCatalog.roll(characterClass)));
            }
        }
    }

    @Test void rejectsMalformedOrOutOfRangeClassicCharacterPackets() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.characterRollClass(new byte[] {0x43, 0x05}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.titleRequest(new Protocol.Writer().c(Protocol.C_TITLE)
                        .s("TestMage").s("").bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.characterRollResult(new int[] {1, 2, 3}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.customCharacter(new Protocol.Writer()
                        .c(Protocol.C_CREATE_CUSTOM_CHARACTER).s("BadStats").c(0).c(4)
                        .c(1).c(1).c(1).c(1).c(1).c(1).c(0).c(0).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.bookmarkName(new Protocol.Writer().c(Protocol.C_BOOKMARK)
                                .s("X".repeat(28)).bytes(), Protocol.C_BOOKMARK));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.weatherControl(new byte[] {0x3A, 0x34}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.worldTime(0x8000_0000L));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.classicCharacterRequest(
                        new Protocol.Writer().c(Protocol.C_ENTER_WORLD).s("").bytes(),
                        Protocol.C_ENTER_WORLD));
        assertThrows(NullPointerException.class,
                () -> Protocol.createCharacterFailure(null, 0));
        assertThrows(NullPointerException.class,
                () -> Protocol.enterWorldCheck(null, 0));
        assertThrows(NullPointerException.class,
                () -> Protocol.deleteCharacterCheck(null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.deleteCharacterCheck(
                        Protocol.DeleteCharacterCheckResult.DELETE_NOW, -1));
    }
}
