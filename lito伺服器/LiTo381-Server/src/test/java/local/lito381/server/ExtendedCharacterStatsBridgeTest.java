package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class ExtendedCharacterStatsBridgeTest {
    @AfterEach void clearNumericSchema() {
        System.clearProperty("lito.numeric.breakthrough");
    }

    @Test void publishesFullDatabaseAndSafeDirectoryShadowForLauncher() {
        CharacterSummary source = new CharacterSummary(
                "Over\\\"flow", "", 1, 0, 200, 100_000, 80_000, -500,
                300, 301, 302, 303, 304, 305,
                10, 10, 1, 0, 0,
                0, 0, 0, 0,
                0, 33000, 33000, 4,
                10, 40, 10, 10, 10, 0);
        String line = ExtendedCharacterStatsBridge.event(
                "DIRECTORY", "source\\\"one", source,
                NativeCharacterStats.directoryProjection(source));

        assertTrue(line.startsWith("[LiTo-Extended-Stats] {\"schemaVersion\":2"));
        assertTrue(line.contains("\"numericBreakthroughSchema\":"));
        assertTrue(line.contains("\"source\":\"source\\\\\\\"one\""));
        assertTrue(line.contains("\"character\":\"Over\\\\\\\"flow\""));
        assertTrue(line.contains("\"database\":{\"maxHp\":100000,\"maxMp\":80000"));
        assertTrue(line.contains("\"directoryShadow\":{\"maxHp\":32767,\"maxMp\":32767"));
        assertTrue(line.contains("\"runtime\":null"));
        assertTrue(line.endsWith("\"adjustedDirectory\":true}"));
    }

    @Test void publishesActualRuntimeWireValuesAndAllSchemaFourStats() {
        System.setProperty("lito.numeric.breakthrough", "4");
        CharacterSummary source = new CharacterSummary(
                "Overflow", "", 1, 0, 200, 100_000, 80_000, -500,
                300, 301, 302, 303, 304, 305,
                10, 10, 1, 0, 0,
                0, 0, 0, 0,
                0, 33000, 33000, 4,
                10, 40, 10, 10, 10, 0);
        String line = ExtendedCharacterStatsBridge.runtimeEvent(
                "source-one", "battle-status", source,
                new ExtendedCharacterStatsBridge.RuntimeProjection(
                        120_000, 90_000, -650, 200,
                        320, 321, 322, 323, 324, 325,
                        400, 401, 402, 403, 404, 405));

        assertTrue(line.contains("\"event\":\"RUNTIME\""));
        assertTrue(line.contains("\"numericBreakthroughSchema\":4"));
        assertTrue(line.contains("\"reason\":\"battle-status\""));
        assertTrue(line.contains("\"runtime\":{\"maxHp\":120000,\"maxMp\":90000"));
        assertTrue(line.contains("\"spellPowerModifier\":400"));
        assertTrue(line.contains("\"magicResistanceModifier\":401"));
        assertTrue(line.contains("\"fireResistance\":402"));
        assertTrue(line.contains("\"earthResistance\":405"));
    }
}
