package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ProtocolLitoCharacterSourceTest {
    private static byte[] request(String sourceName) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(Protocol.C_LITO_LIN_CHARACTER_INFO);
        out.writeBytes(sourceName.getBytes(Protocol.LEGACY_TEXT));
        out.write(0);
        return out.toByteArray();
    }

    @Test void parsesBothExactCsProducersAndThe63ByteRecordBoundary() {
        assertEquals(new Protocol.LitoCharacterSourceRequest("Classic Source"),
                Protocol.litoCharacterSourceRequest(request("Classic Source")));
        assertEquals(new Protocol.LitoCharacterSourceRequest(""),
                Protocol.litoCharacterSourceRequest(request("")));
        assertEquals(63, Protocol.litoCharacterSourceRequest(
                request("A".repeat(63))).sourceName().length());
    }

    @Test void rejectsOpcodeOnlyOverflowUnterminatedAndTrailingLayouts() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoCharacterSourceRequest(new byte[] {(byte) 0xC8}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoCharacterSourceRequest(request("A".repeat(64))));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoCharacterSourceRequest(new byte[] {(byte) 0xC8, 'A'}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoCharacterSourceRequest(
                        new byte[] {(byte) 0xC8, 'A', 0, 0}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoCharacterSourceRequest(new byte[] {(byte) 0xC9, 0}));
    }

    @Test void rejectsBytesThatCannotRoundTripThroughMs950() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoCharacterSourceRequest(
                        new byte[] {(byte) 0xC8, (byte) 0x81, 0}));
    }

    @Test void configUsesTheSameNativeSourceNameBoundary(@TempDir Path directory)
            throws Exception {
        Path valid = directory.resolve("valid.properties");
        Files.writeString(valid, "database.mode=memory\nlogin.classic.server.names="
                + "A".repeat(63));
        assertEquals("A".repeat(63), ServerConfig.load(valid).classicServerNames().get(0));

        Path overflow = directory.resolve("overflow.properties");
        Files.writeString(overflow, "database.mode=memory\nlogin.classic.server.names="
                + "A".repeat(64));
        assertThrows(IllegalArgumentException.class, () -> ServerConfig.load(overflow));

        Path control = directory.resolve("control.properties");
        Files.writeString(control,
                "database.mode=memory\nlogin.classic.server.names=bad\\tname");
        assertThrows(IllegalArgumentException.class, () -> ServerConfig.load(control));
    }

    @Test void configRequiresOneReadOnlyDatabaseRoutePerAdvertisedLegacySource(
            @TempDir Path directory) throws Exception {
        Path valid = directory.resolve("two-sources.properties");
        Files.writeString(valid, """
                database.mode=legacy_mysql
                login.classic.server.names=Sun,Moon
                database.legacy.source.databases=legacy_sun,legacy_moon
                database.legacy.source.resource.databases=resource_sun,resource_moon
                database.legacy.source.weight.rates=100.0,127.0
                """);
        ServerConfig config = ServerConfig.load(valid);
        assertEquals(java.util.List.of("Sun", "Moon"), config.classicServerNames());
        assertEquals(java.util.List.of("legacy_sun", "legacy_moon"),
                config.legacySourceDatabases());
        assertEquals(java.util.List.of("resource_sun", "resource_moon"),
                config.legacySourceResourceDatabases());
        assertEquals(java.util.List.of(100.0, 127.0),
                config.legacySourceWeightRates());

        Path missing = directory.resolve("missing-route.properties");
        Files.writeString(missing, """
                database.mode=legacy_mysql
                login.classic.server.names=Sun,Moon
                database.legacy.login.database=legacy_sun
                """);
        assertThrows(IllegalArgumentException.class, () -> ServerConfig.load(missing));

        Path missingResource = directory.resolve("missing-resource-route.properties");
        Files.writeString(missingResource, """
                database.mode=legacy_mysql
                login.classic.server.names=Sun,Moon
                database.legacy.source.databases=legacy_sun,legacy_moon
                """);
        assertThrows(IllegalArgumentException.class, () -> ServerConfig.load(missingResource));

        Path mismatch = directory.resolve("mismatched-route.properties");
        Files.writeString(mismatch, """
                database.mode=legacy_mysql
                login.classic.server.names=Sun,Moon
                database.legacy.source.databases=legacy_sun
                """);
        assertThrows(IllegalArgumentException.class, () -> ServerConfig.load(mismatch));

        Path unsafe = directory.resolve("unsafe-route.properties");
        Files.writeString(unsafe, """
                database.mode=legacy_mysql
                login.classic.server.names=Sun,Moon
                database.legacy.source.databases=legacy_sun,legacy-moon
                """);
        assertThrows(IllegalArgumentException.class, () -> ServerConfig.load(unsafe));
    }

    @Test void litoMysqlUsesOneStaticDatabaseAcrossMultipleDynamicRoutes(
            @TempDir Path directory) throws Exception {
        Path valid = directory.resolve("lito-mysql.properties");
        Files.writeString(valid, """
                database.mode=lito_mysql
                database.connection.properties=sql.properties
                login.classic.server.names=Shard 1,Shard 2
                database.static.database=lito_static
                database.dynamic.databases=380_ac_1,380_ac_2
                database.source.weight.rates=100.0,100.0
                """);
        ServerConfig config = ServerConfig.load(valid);
        assertEquals("lito_mysql", config.databaseMode());
        assertEquals(directory.resolve("sql.properties").toAbsolutePath().normalize(),
                config.legacySqlProperties());
        assertEquals("380_ac_1", config.legacyLoginDatabase());
        assertEquals(java.util.List.of("380_ac_1", "380_ac_2"),
                config.legacySourceDatabases());
        assertEquals(java.util.List.of("lito_static", "lito_static"),
                config.legacySourceResourceDatabases());
        assertEquals(java.util.List.of(100.0, 100.0),
                config.legacySourceWeightRates());

        Path missingStatic = directory.resolve("missing-static.properties");
        Files.writeString(missingStatic, """
                database.mode=lito_mysql
                login.classic.server.names=Shard 1,Shard 2
                database.dynamic.databases=380_ac_1,380_ac_2
                database.source.weight.rates=100.0,100.0
                """);
        assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.load(missingStatic));

        Path mismatchedDynamic = directory.resolve("mismatched-dynamic.properties");
        Files.writeString(mismatchedDynamic, """
                database.mode=lito_mysql
                login.classic.server.names=Shard 1,Shard 2
                database.static.database=lito_static
                database.dynamic.databases=380_ac_1
                database.source.weight.rates=100.0,100.0
                """);
        assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.load(mismatchedDynamic));
    }
}
