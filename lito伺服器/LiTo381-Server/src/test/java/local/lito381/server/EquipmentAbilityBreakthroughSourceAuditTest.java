package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks the database-to-Schema-2 path for wide equipment ability deltas. */
final class EquipmentAbilityBreakthroughSourceAuditTest {
    @Test
    void templateLoaderNeverRequestsJdbcByteForSixAbilityColumns() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/local/lito381/server/LegacyEquipmentMagicLoader.java"),
                StandardCharsets.UTF_8);
        for (String column : new String[] {
                "add_str", "add_dex", "add_con", "add_cha", "add_int", "add_wis"
        }) {
            assertFalse(source.contains("getByte(\"" + column + "\")"), column);
            assertTrue(source.contains("getLong(\"" + column + "\")")
                    || source.contains("getInt(\"" + column + "\")"), column);
        }
        assertTrue(source.contains(
                "intelligence = addAppliedAbilityModifier(intelligence, other.intelligence)"));
        assertTrue(source.contains(
                "charisma = addAppliedAbilityModifier(charisma, other.charisma)"));
    }

    @Test
    void nativeItemDetailUsesSaturatedShadowInsteadOfModuloWrap() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/local/lito381/server/InventoryCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains(
                "out.c(tag).c(NativeCharacterStats.signedByte(value))"));
        assertFalse(source.contains("if (value != 0) out.c(tag).c(value);"));
    }
}
