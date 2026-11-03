package local.lito381.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OriginSourceProfileBindingTest {
    @Test
    void perfectOneIsASingleReadOnlyNativeSource() throws IOException {
        assertProfile("server.perfect1.properties", "perfect-1", "完美天堂",
                "380_ac", "380_db", "100.0");
    }

    @Test
    void perfectTwoIsASingleReadOnlyNativeSource() throws IOException {
        assertProfile("server.perfect2.properties", "perfect-2", "完美天堂2",
                "380yiwei_ac", "380yiwei_db", "127.0");
    }

    private static void assertProfile(String fileName, String sourceId, String sourceName,
                                      String accountDatabase, String resourceDatabase,
                                      String weightRate) throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(
                Path.of("src/test/resources/server-config/config", fileName),
                StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        assertEquals(sourceId, properties.getProperty("origin.source.id"));
        assertEquals(sourceName, properties.getProperty("origin.source.name"));
        assertEquals(sourceName, properties.getProperty("login.classic.server.names"));
        assertEquals("legacy_mysql", properties.getProperty("database.mode"));
        assertEquals(accountDatabase,
                properties.getProperty("database.legacy.login.database"));
        assertEquals(resourceDatabase,
                properties.getProperty("database.legacy.source.resource.databases"));
        assertEquals(weightRate,
                properties.getProperty("database.legacy.source.weight.rates"));
        assertEquals("true", properties.getProperty("database.read.only"));
        assertEquals("false",
                properties.getProperty("classic.world.compatibility.enabled"));
    }
}
