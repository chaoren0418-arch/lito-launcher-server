package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OriginSourceConfigTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @Test
    void defaultProfileRoutesTheDedicatedReadOnlyCharacterStores() throws Exception {
        ServerConfig first = ServerConfig.load(Path.of("config/server.properties"));
        assertEquals("lito-shared", first.originSourceId());
        assertEquals("LiTo 共用資料架構", first.originSourceName());
        assertEquals("380_ac_1", first.legacyLoginDatabase());
        assertEquals(java.util.List.of("太陽神", "月亮神"),
                first.classicServerNames());
        assertEquals(java.util.List.of(1, 2), first.classicServerIds());
        assertEquals(java.util.List.of("380_ac_1", "380_ac_2"),
                first.legacySourceDatabases());
        assertEquals(java.util.List.of("lito_static", "lito_static"),
                first.legacySourceResourceDatabases());
        assertEquals(java.util.List.of(100.0, 100.0), first.legacySourceWeightRates());
        assertTrue(first.databaseReadOnly());

        ServerConfig second = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.perfect2.properties"));
        assertEquals("perfect-2", second.originSourceId());
        assertEquals("\u5b8c\u7f8e\u5929\u58022", second.originSourceName());
        assertEquals("380yiwei_ac", second.legacyLoginDatabase());
        assertEquals(java.util.List.of("\u5b8c\u7f8e\u5929\u58022"),
                second.classicServerNames());
        assertEquals(java.util.List.of(2), second.classicServerIds());
        assertEquals(java.util.List.of("380yiwei_ac"), second.legacySourceDatabases());
        assertEquals(java.util.List.of("380yiwei_db"),
                second.legacySourceResourceDatabases());
        assertEquals(java.util.List.of(127.0), second.legacySourceWeightRates());
        assertTrue(second.databaseReadOnly());

        assertTrue(first.administrationAccounts().isEmpty());
        assertTrue(second.administrationAccounts().isEmpty());
        assertEquals("", first.repairNpcAppearanceSpeech());
        ServerConfig selftest = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        assertEquals(java.util.Set.of("test"), selftest.administrationAccounts());
        assertEquals("NPC speech fixture ready.", selftest.repairNpcAppearanceSpeech());
    }
}
