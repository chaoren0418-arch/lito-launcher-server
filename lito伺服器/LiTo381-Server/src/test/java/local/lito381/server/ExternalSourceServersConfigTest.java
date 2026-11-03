package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ExternalSourceServersConfigTest {
    @TempDir Path temporary;

    @Test
    void externalTableBindsDisplayedNamesIdsDatabasesAndRatesByRow() throws Exception {
        Path server = temporary.resolve("server.properties");
        Files.writeString(server, """
                bind=127.0.0.1
                game.bind=127.0.0.2
                database.mode=lito_mysql
                database.connection.properties=database.properties
                database.static.database=lito_static
                source.servers.file=source-servers.properties
                login.classic.server.names=fallback
                login.classic.server.ids=9
                database.dynamic.databases=fallback_db
                """);
        Files.writeString(temporary.resolve("source-servers.properties"), """
                source.count=2
                source.1.name=太陽神
                source.1.id=1
                source.1.database=380_ac_1
                source.1.weight.rate=125.5
                source.2.name=月亮神
                source.2.id=2
                source.2.database=380_ac_2
                source.2.weight.rate=80
                """);

        ServerConfig config = ServerConfig.load(server);
        assertEquals(List.of("太陽神", "月亮神"), config.classicServerNames());
        assertEquals(List.of(1, 2), config.classicServerIds());
        assertEquals(List.of("380_ac_1", "380_ac_2"), config.legacySourceDatabases());
        assertEquals(List.of(125.5, 80.0), config.legacySourceWeightRates());
    }

    @Test
    void incompleteExternalRowIsRejectedBeforeRoutesCanShift() throws Exception {
        Path server = temporary.resolve("server.properties");
        Files.writeString(server, """
                source.servers.file=source-servers.properties
                database.mode=memory
                """);
        Files.writeString(temporary.resolve("source-servers.properties"), """
                source.count=2
                source.1.name=太陽神
                source.1.id=1
                source.1.database=380_ac_1
                source.2.name=月亮神
                source.2.id=2
                """);

        assertThrows(IllegalArgumentException.class, () -> ServerConfig.load(server));
    }
}
