package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real production startup/ownership with isolated files/TLS/TCP; no live MySQL or DDL. */
class ModernMainSettlementTest {
    @TempDir Path temporary;
    private static SSLContext clients;
    @BeforeAll static void certificates() throws Exception {
        var material = ModernTlsTestMaterial.defaultDirectory(); ModernTlsTestMaterial.prepare(material);
        clients = ModernTlsTestMaterial.clientContext(material);
    }
    private static String portable(Path path) { return path.toAbsolutePath().toString().replace('\\', '/'); }
    private Path directory(String name) throws IOException { return Files.createDirectories(temporary.resolve(name)); }
    private String base(int sourcePort) {
        return "schema=1\nbind.port=0\ntls.pkcs12=" + portable(ModernTlsTestMaterial.defaultDirectory().resolve("server.p12"))
                + "\ntls.password.env=TLS\nsources=7\nsource.7.name=Sun\nsource.7.mysql.host=127.0.0.1\nsource.7.mysql.port=" + sourcePort
                + "\nsource.7.mysql.database=source_only\nsource.7.mysql.username.env=SOURCE_USER\nsource.7.mysql.password.env=SOURCE_PASSWORD\n"
                + "source.7.mysql.passwordScheme=plain\nsource.7.mysql.tls=disabled\nlimits.backendDeadlineMillis=600\n"
                + "limits.dbConnectMillis=200\nlimits.dbNetworkMillis=600\n";
    }
    private String writer(int port, Path path) {
        return "settlement.enabled=true\nsettlement.outboxDirectory=" + portable(path) + "\nsettlement.mysql.host=127.0.0.1\nsettlement.mysql.port=" + port
                + "\nsettlement.mysql.database=modern_results\nsettlement.mysql.username.env=WRITER_USER\nsettlement.mysql.password.env=WRITER_PASSWORD\nsettlement.mysql.tls=disabled\n";
    }
    private String world(boolean combat, int mode, int logicalMap) throws Exception {
        Path root = directory("resources"), maps = Files.createDirectories(root.resolve("map/514"));
        Files.write(maps.resolve("7fff8000.s32"), ModernWorldMapTest.openSectorBytes());
        String prefix = "world." + mode + "." + logicalMap + ".";
        return "world.sharedRoot=" + portable(root) + "\nworld.bindings=" + mode + ":" + logicalMap + "\n"
                + prefix + "physicalMap=514\n" + prefix + "sectors=7fff8000\n" + prefix + "spawnX=32724\n"
                + prefix + "spawnY=32784\n" + prefix + "stepMillis=50\n"
                + (combat ? prefix + "combatRules=" + ModernPhysicalCombat.RULESET + "\n" + prefix + "attackMillis=1000\n" : "")
                + "source.7.catalog.database=source_catalog\nequipment.storeDirectory=" + portable(directory("equipment")) + "\n";
    }
    private static ModernServerConfig parse(String text) throws Exception {
        return ModernServerConfig.parse(text.getBytes(StandardCharsets.UTF_8), name -> switch (name) {
            case "TLS" -> "modern-lito-fixture-only"; case "SOURCE_USER" -> "source-readonly"; case "WRITER_USER" -> "settlement-writer";
            default -> "synthetic-secret";
        });
    }
    private static ModernWorldService world(ModernServer server) throws Exception {
        var field = ModernServer.class.getDeclaredField("worlds"); field.setAccessible(true); return (ModernWorldService) field.get(server);
    }
    private static ModernMatchSettlement.Store store(ModernServer server) throws Exception {
        var field = ModernWorldService.class.getDeclaredField("settlementStore"); field.setAccessible(true); return (ModernMatchSettlement.Store) field.get(world(server));
    }
    private static void stop(ModernServer server) throws Exception { server.close(); assertTrue(server.awaitTermination(5000)); }
    private static ServerSocket blackhole() throws IOException {
        var socket = new ServerSocket(0, 4, InetAddress.getByName("127.0.0.1")); socket.setSoTimeout(150); return socket;
    }
    private static ModernSessionProtocol.Ack hello(ModernServer server) throws Exception {
        return hello(server, ModernSessionProtocol.MAX_VERSION, ModernSessionProtocol.KNOWN_CAPABILITIES);
    }
    private static ModernSessionProtocol.Ack hello(ModernServer server, int version, int capabilities) throws Exception {
        try (var socket = (SSLSocket) clients.getSocketFactory().createSocket()) {
            var parameters = socket.getSSLParameters(); parameters.setEndpointIdentificationAlgorithm("HTTPS"); socket.setSSLParameters(parameters);
            socket.setSoTimeout(3000); socket.connect(server.address(), 1000); socket.startHandshake();
            socket.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1, version,
                    version, capabilities, 7, ModernSessionProtocol.NUMERIC_SCHEMA_I64_V1)));
            socket.getOutputStream().flush();
            return (ModernSessionProtocol.Ack) ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream()));
        }
    }
    private static void reopen(ModernServerConfig config) throws Exception {
        byte[] binding = ModernSettlementCoordinator.binding(config);
        try (var outbox = new ModernMatchOutbox(config.settlement().outboxDirectory(), binding)) { assertTrue(outbox.isHealthy()); }
        finally { Arrays.fill(binding, (byte) 0); }
        try (var equipment = new ModernEquipmentOverrideStore(config.equipmentStoreDirectory())) { /* Its original lifetime lock also retired. */ }
    }

    private String bindingSettings(Path path) {
        String second = base(3306).lines().filter(line -> line.startsWith("source.7.")).map(line -> line.replace("source.7.", "source.42.")
                .replace("name=Sun", "name=Moon").replace("database=source_only", "database=source_second")).collect(java.util.stream.Collectors.joining("\n"));
        return base(3306).replace("sources=7", "sources=7,42") + second + "\nsource.7.catalog.database=source_catalog\n" + writer(3307, path);
    }
    private static byte[] binding(String settings) throws Exception {
        try (var config = parse(settings)) { return ModernSettlementCoordinator.binding(config); }
    }
    @Test void outboxBindingIgnoresSourceOrderDisplayNamesCredentialValuesAndEnvironmentNames() throws Exception {
        String settings = bindingSettings(directory("binding-only")); byte[] expected = binding(settings);
        try {
            assertArrayEquals(expected, binding(settings.replace("sources=7,42", "sources=42,7")));
            assertArrayEquals(expected, binding(settings.replace("name=Sun", "name=Solar").replace("name=Moon", "name=Lunar")));
            String renamed = settings.replace("SOURCE_USER", "OTHER_SOURCE_USER").replace("SOURCE_PASSWORD", "OTHER_SOURCE_PASSWORD")
                    .replace("WRITER_USER", "OTHER_WRITER_USER").replace("WRITER_PASSWORD", "OTHER_WRITER_PASSWORD");
            assertArrayEquals(expected, binding(renamed));
            try (var config = ModernServerConfig.parse(settings.getBytes(StandardCharsets.UTF_8), name -> name.equals("TLS") ? "modern-lito-fixture-only" : "changed-secret-and-user")) {
                assertNotEquals("source-readonly", config.sources().get(0).database().username());
                assertNotEquals("settlement-writer", config.settlement().database().username());
                assertArrayEquals(expected, ModernSettlementCoordinator.binding(config));
            }
        } finally { Arrays.fill(expected, (byte) 0); }
    }
    @Test void outboxBindingChangesForEveryConfiguredWriterAndSourceRoutingIdentity() throws Exception {
        String settings = bindingSettings(directory("binding-only")); byte[] expected = binding(settings);
        List<String> changed = List.of(
                settings.replace("settlement.mysql.database=modern_results", "settlement.mysql.database=modern_results_two"),
                settings.replace("settlement.mysql.host=127.0.0.1", "settlement.mysql.host=127.0.0.2"),
                settings.replace("settlement.mysql.port=3307", "settlement.mysql.port=3308"),
                settings.replace("sources=7,42", "sources=8,42").replace("source.7.", "source.8."),
                settings.replace("source.7.mysql.database=source_only", "source.7.mysql.database=source_changed"),
                settings.replace("source.7.mysql.host=127.0.0.1", "source.7.mysql.host=127.0.0.2"),
                settings.replace("source.7.mysql.port=3306", "source.7.mysql.port=3308"),
                settings.replace("source.7.catalog.database=source_catalog", "source.7.catalog.database=catalog_changed"),
                settings.replace("source.7.catalog.database=source_catalog\n", ""),
                settings.replace("sources=7,42", "sources=7").lines().filter(line -> !line.startsWith("source.42.")).collect(java.util.stream.Collectors.joining("\n")) + "\n");
        try {
            for (String modified : changed) {
                byte[] actual = binding(modified);
                try { assertFalse(Arrays.equals(expected, actual), "a changed routing identity cannot reuse the original durable namespace"); }
                finally { Arrays.fill(actual, (byte) 0); }
            }
        } finally { Arrays.fill(expected, (byte) 0); }
    }
    @Test void optInMagicHasDistinctNamespaceWithoutChangingAnyLegacyTeleportDigest() throws Exception {
        String plain=base(3306)+writer(3307,directory("binding-magic"))+world(true,1,514);
        String teleport=plain+"world.1.514.spellRules="+ModernWorldConfiguration.SPELL_RULESET+"\nworld.1.514.castMillis=700\n";
        String magic=teleport.replace(ModernWorldConfiguration.SPELL_RULESET,ModernWorldConfiguration.DAMAGE_SPELL_RULESET);
        byte[] legacy=binding(plain),oldTeleport=binding(teleport),newMagic=binding(magic);
        try {
            assertArrayEquals(legacy,oldTeleport);
            assertFalse(Arrays.equals(legacy,newMagic));
            assertArrayEquals(newMagic,binding(magic.replace("name=Sun","name=Renamed")));
            try(var config=parse(magic)) {
                assertTrue(config.worlds().damageMagicEnabled());
                try(var outbox=new ModernMatchOutbox(config.settlement().outboxDirectory(),newMagic)) {assertTrue(outbox.isHealthy());}
                assertThrows(IOException.class,()->new ModernMatchOutbox(config.settlement().outboxDirectory(),legacy));
                try(var outbox=new ModernMatchOutbox(config.settlement().outboxDirectory(),newMagic)) {assertTrue(outbox.isHealthy());}
            }
        } finally {Arrays.fill(legacy,(byte)0);Arrays.fill(oldTeleport,(byte)0);Arrays.fill(newMagic,(byte)0);}
    }
    @Test void optInBotsBindTemplatePolicyAndLogicalMapsWithoutChangingBotDisabledNamespaces() throws Exception {
        String plain=base(3306)+writer(3307,directory("binding-bots"))+world(true,1,514);
        String enabled=plain+"world.1.514.botRules="+ModernWorldConfiguration.BOT_RULESET+"\n";
        byte[] legacy=binding(plain),bots=binding(enabled);
        try {
            // Independent established v1 route bytes: bot-disabled startup must
            // not append a false flag/count or invalidate an existing outbox.
            try(var config=parse(plain)) {
                var bytes=new java.io.ByteArrayOutputStream();
                try(var data=new java.io.DataOutputStream(bytes)) {
                    data.writeUTF("modern-match-outbox-routing-v1");
                    var writer=config.settlement().database();
                    data.writeUTF(writer.host());data.writeInt(writer.port());data.writeUTF(writer.database());data.writeUTF(writer.sslMode());
                    var sources=config.sources().stream().sorted(java.util.Comparator.comparingInt(ModernServerConfig.SourceConfig::id)).toList();
                    data.writeInt(sources.size());
                    for(var source:sources) {
                        data.writeInt(source.id());var db=source.database();
                        data.writeUTF(db.host());data.writeInt(db.port());data.writeUTF(db.database());data.writeUTF(db.sslMode());
                        data.writeUTF(db.passwordScheme());data.writeBoolean(source.catalogDatabase()!=null);
                        if(source.catalogDatabase()!=null)data.writeUTF(source.catalogDatabase());
                    }
                }
                assertArrayEquals(java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()),legacy);
            }
            assertFalse(Arrays.equals(legacy,bots));assertArrayEquals(bots,binding(enabled.replace("name=Sun","name=Renamed")));
            assertFalse(Arrays.equals(bots,binding(enabled.replace("world.bindings=1:514","world.bindings=1:509").replace("world.1.514.","world.1.509."))));
            try(var config=parse(enabled)) {
                assertTrue(config.worlds().botsEnabled());assertEquals(java.util.List.of(new ModernWorldBinding.Key(1,514)),config.worlds().botBindings());
                try(var outbox=new ModernMatchOutbox(config.settlement().outboxDirectory(),bots)){assertTrue(outbox.isHealthy());}
                assertThrows(IOException.class,()->new ModernMatchOutbox(config.settlement().outboxDirectory(),legacy));
                try(var outbox=new ModernMatchOutbox(config.settlement().outboxDirectory(),bots)){assertTrue(outbox.isHealthy());}
            }
        } finally {Arrays.fill(legacy,(byte)0);Arrays.fill(bots,(byte)0);}
    }
    @Test void evenAnEmptyDurableOutboxRejectsChangedRouteBindingAndRemainsReopenableWithOriginal() throws Exception {
        Path path = directory("binding-only"); String settings = bindingSettings(path);
        byte[] original = binding(settings), changed = binding(settings.replace("source.7.mysql.database=source_only", "source.7.mysql.database=source_changed"));
        try {
            try (var outbox = new ModernMatchOutbox(path, original)) { assertEquals(0, outbox.size()); }
            var error = assertThrows(ModernMatchOutbox.OutboxException.class, () -> new ModernMatchOutbox(path, changed));
            assertEquals(ModernMatchOutbox.Failure.BINDING_MISMATCH, error.failure());
            try (var outbox = new ModernMatchOutbox(path, original)) {
                assertTrue(outbox.isHealthy()); assertEquals(List.of(), outbox.pending(System.nanoTime() + TimeUnit.SECONDS.toNanos(2)));
            }
        } finally { Arrays.fill(original, (byte) 0); Arrays.fill(changed, (byte) 0); }
    }

    @Test void absentSettlementKeepsProductionCombatWithoutResultStoreOrOutbox() throws Exception {
        try (var source = blackhole(); var config = parse(base(source.getLocalPort()) + world(true, 1, 514))) {
            var server = ModernMain.start(config);
            try {
                assertNull(store(server)); assertTrue(world(server).combatEnabled());
                assertTrue((hello(server).capabilities() & ModernSessionProtocol.CAP_COMBAT) != 0);
                assertEquals(0, hello(server).capabilities() & ModernSessionProtocol.CAP_MATCHMAKING);
                assertThrows(SocketTimeoutException.class, source::accept); assertFalse(Files.exists(temporary.resolve("outbox")));
            } finally { stop(server); }
        }
    }
    @Test void enabledProductionOwnsCoordinatorAndStartsWithoutSourceOrWriterSql() throws Exception {
        Path path = directory("outbox");
        try (var source = blackhole(); var writer = blackhole(); var config = parse(base(source.getLocalPort()) + world(true, 1, 514) + writer(writer.getLocalPort(), path))) {
            var server = ModernMain.start(config); var coordinator = assertInstanceOf(ModernMatchSettlement.JournaledStore.class, store(server));
            try {
                assertTrue(coordinator.accepting()); assertTrue(Files.isRegularFile(path.resolve("outbox.mso")));
                assertTrue((hello(server).capabilities() & ModernSessionProtocol.CAP_COMBAT) != 0);
                assertEquals(0, hello(server).capabilities() & ModernSessionProtocol.CAP_MATCHMAKING);
                assertThrows(SocketTimeoutException.class, source::accept); assertThrows(SocketTimeoutException.class, writer::accept);
                assertThrows(IOException.class, () -> new ModernMatchOutbox(path, ModernSettlementCoordinator.binding(config)));
                assertEquals("source-readonly", config.sources().get(0).database().username());
            } finally { stop(server); }
            assertTrue(coordinator.isQuiescent()); assertTrue(coordinator.awaitTermination(0)); reopen(config);
            assertEquals("settlement-writer", config.settlement().database().username(), "caller owns config until server retirement");
        }
    }
    @Test void explicitWriterWithoutAnyWorldIsRejectedBeforeCreatingOutbox() throws Exception {
        Path path = directory("outbox");
        try (var source = blackhole(); var writer = blackhole(); var config = parse(base(source.getLocalPort()) + writer(writer.getLocalPort(), path))) {
            assertThrows(IOException.class, () -> ModernMain.start(config));
            assertFalse(Files.exists(path.resolve("outbox.mso"))); assertThrows(SocketTimeoutException.class, writer::accept); assertThrows(SocketTimeoutException.class, source::accept);
        }
    }
    @Test void matchmakingProductionOptInNegotiatesOnlyV34AndDoesNotRebindResults() throws Exception {
        Path path = directory("matching-outbox");
        try (var source = blackhole(); var writer = blackhole()) {
            String settings = base(source.getLocalPort()) + world(true, 1, 514) + writer(writer.getLocalPort(), path);
            try (var disabled = parse(settings); var enabled = parse(settings + "matchmaking.enabled=true\n")) {
                assertFalse(disabled.matchmakingEnabled()); assertTrue(enabled.matchmakingEnabled());
                byte[] before = ModernSettlementCoordinator.binding(disabled), after = ModernSettlementCoordinator.binding(enabled);
                try { assertArrayEquals(before, after, "queue availability cannot change the identity of saved results"); }
                finally { Arrays.fill(before, (byte) 0); Arrays.fill(after, (byte) 0); }
                var server = ModernMain.start(enabled);
                try {
                    assertNotEquals(0, hello(server).capabilities() & ModernSessionProtocol.CAP_MATCHMAKING);
                    var legacy = hello(server, 33, ModernSessionProtocol.KNOWN_CAPABILITIES & ~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_ACTION_LIFE|ModernSessionProtocol.CAP_MATCHMAKING | ModernSessionProtocol.CAP_LARGE_MAPS|ModernSessionProtocol.CAP_BATTLE_DATA));
                    assertEquals(33, legacy.version()); assertEquals(0, legacy.capabilities() & ModernSessionProtocol.CAP_MATCHMAKING);
                    assertEquals(0, hello(server, 34, ModernSessionProtocol.KNOWN_CAPABILITIES & ~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_ACTION_LIFE|ModernSessionProtocol.CAP_MATCHMAKING | ModernSessionProtocol.CAP_BATTLE_DATA))
                            .capabilities() & ModernSessionProtocol.CAP_MATCHMAKING);
                    assertThrows(SocketTimeoutException.class, source::accept); assertThrows(SocketTimeoutException.class, writer::accept);
                } finally { stop(server); }
                reopen(enabled);
            }
        }
    }
    @Test void greaterHasteProductionStartupIsOptInAndDowngradesWithoutConnectingSource()throws Exception {
        try(var source=blackhole();var writer=blackhole()) {
            String settings=base(source.getLocalPort())+world(true,1,514)+writer(writer.getLocalPort(),directory("haste-outbox"))
                    +"shop.allowance=20000\nsource.7.weight.rate=100\nworld.1.514.itemRules="+ModernWorldConfiguration.ITEM_RULESET
                    +"\nworld.1.514.weightRules="+ModernWorldConfiguration.WEIGHT_RULESET+"\nworld.1.514.buffRules="+ModernWorldConfiguration.TIMED_RULESET
                    +"\nworld.1.514.spellRules="+ModernWorldConfiguration.SPELL_RULESET+"\nworld.1.514.castMillis=700\n";
            try(var disabled=parse(settings);var enabled=parse(settings+"world.1.514.greaterHasteRules="+ModernGreaterHaste.RULESET+"\n")) {
                assertFalse(Arrays.equals(ModernSettlementCoordinator.binding(disabled),ModernSettlementCoordinator.binding(enabled)));
                var off=ModernMain.start(disabled);try{assertEquals(0,hello(off).capabilities()&ModernSessionProtocol.CAP_GREATER_HASTE);}finally{stop(off);}
                // Different rules intentionally have a different persistence namespace.
                settings=settings.replace(portable(temporary.resolve("haste-outbox")),portable(directory("haste-enabled-outbox")));
                try(var fresh=parse(settings+"world.1.514.greaterHasteRules="+ModernGreaterHaste.RULESET+"\n")) {
                    var server=ModernMain.start(fresh);try {
                        assertTrue(world(server).greaterHasteEnabled());assertNotEquals(0,hello(server).capabilities()&ModernSessionProtocol.CAP_GREATER_HASTE);
                        assertEquals(0,hello(server,41,ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE)).capabilities()&ModernSessionProtocol.CAP_GREATER_HASTE);
                        assertThrows(SocketTimeoutException.class,source::accept);assertThrows(SocketTimeoutException.class,writer::accept);
                    }finally{stop(server);}
                }
            }
        }
    }
    @Test void slowMainExplicitRulesHaveNewNamespaceAndNeverGrantCommandsTo46()throws Exception {
        try(var source=blackhole();var writer=blackhole()) {
            String settings=base(source.getLocalPort())+world(true,1,514)+writer(writer.getLocalPort(),directory("slow-outbox"))
                    +"shop.allowance=20000\nsource.7.weight.rate=100\nworld.1.514.itemRules="+ModernWorldConfiguration.ITEM_RULESET
                    +"\nworld.1.514.weightRules="+ModernWorldConfiguration.WEIGHT_RULESET+"\nworld.1.514.buffRules="+ModernWorldConfiguration.TIMED_RULESET
                    +"\nworld.1.514.spellRules="+ModernWorldConfiguration.SPELL_RULESET+"\nworld.1.514.castMillis=700\n";
            String enabled=settings+"world.1.514.slowRules="+ModernSlow.RULESET+"\n";
            try(var off=parse(settings);var on=parse(enabled)) {
                assertFalse(off.worlds().slowEnabled());assertTrue(on.worlds().slowEnabled());
                assertFalse(Arrays.equals(ModernSettlementCoordinator.binding(off),ModernSettlementCoordinator.binding(on)));
                var server=ModernMain.start(on);try {
                    assertTrue(world(server).slowEnabled());
                    assertNotEquals(0,hello(server).capabilities()&ModernSessionProtocol.CAP_SLOW_DATA);
                    assertEquals(0,hello(server,46,ModernSessionProtocol.KNOWN_CAPABILITIES).capabilities()&ModernSessionProtocol.CAP_SLOW_DATA);
                    assertThrows(SocketTimeoutException.class,source::accept);
                    assertThrows(SocketTimeoutException.class,writer::accept);
                }finally{stop(server);}
            }
            for(String missing:List.of("world.1.514.buffRules=","world.1.514.spellRules=","world.1.514.castMillis=")) {
                String bad=enabled.lines().filter(line->!line.startsWith(missing)).collect(java.util.stream.Collectors.joining("\n"))+"\n";
                assertThrows(ModernServerConfig.ConfigException.class,()->parse(bad),missing);
            }
            assertThrows(ModernServerConfig.ConfigException.class,()->parse(enabled.replace(ModernSlow.RULESET,"MODERN_SLOW_UNKNOWN")));
        }
    }
    @Test void explicitWriterWithMovementOnlyOrOtherCombatModeCannotSilentlyEnableResults() throws Exception {
        for (boolean combat : List.of(false, true)) {
            Path path = directory(combat ? "honor-outbox" : "movement-outbox");
            try (var source = blackhole(); var writer = blackhole(); var config = parse(base(source.getLocalPort()) + world(combat, combat ? 5 : 1, combat ? 510 : 514) + writer(writer.getLocalPort(), path))) {
                assertThrows(IOException.class, () -> ModernMain.start(config)); assertFalse(Files.exists(path.resolve("outbox.mso")));
                assertThrows(SocketTimeoutException.class, source::accept); assertThrows(SocketTimeoutException.class, writer::accept);
            }
        }
    }
    @Test void matchmakingMissingAuthorityIsRejectedBeforeReadingAnySecret() throws Exception {
        String settings = base(3306) + world(true, 1, 514) + writer(3307, directory("matching-rejected-outbox"))
                + "matchmaking.enabled=true\n";
        for (String prefix : List.of("world.", "world.1.514.combatRules=", "equipment.storeDirectory=", "settlement.", "source.7.catalog.database=")) {
            String incomplete = settings.lines().filter(line -> !line.startsWith(prefix)).collect(java.util.stream.Collectors.joining("\n")) + "\n";
            var secrets = new java.util.concurrent.atomic.AtomicInteger();
            var error = assertThrows(ModernServerConfig.ConfigException.class, () -> ModernServerConfig.parse(incomplete.getBytes(StandardCharsets.UTF_8), key -> {
                secrets.incrementAndGet(); return "must-not-be-read";
            }), prefix);
            assertEquals(ModernServerConfig.Failure.INVALID_VALUE, error.failure(), prefix);
            assertEquals(0, secrets.get(), prefix);
        }
        assertFalse(Files.exists(temporary.resolve("matching-rejected-outbox/outbox.mso")));
    }
    @Test void occupiedListenerFailureReleasesWorldCoordinatorOutboxAndEquipmentOwners() throws Exception {
        Path path = directory("outbox");
        try (var occupied = blackhole(); var source = blackhole(); var writer = blackhole();
             var config = parse(base(source.getLocalPort()).replace("bind.port=0", "bind.port=" + occupied.getLocalPort()) + world(true, 1, 514) + writer(writer.getLocalPort(), path))) {
            assertThrows(IOException.class, () -> ModernMain.start(config)); reopen(config);
            assertThrows(SocketTimeoutException.class, source::accept); assertThrows(SocketTimeoutException.class, writer::accept);
        }
    }
    @Test void occupiedEquipmentFailureStillRetiresAlreadyCreatedCoordinator() throws Exception {
        Path path = directory("outbox");
        try (var source = blackhole(); var writer = blackhole(); var config = parse(base(source.getLocalPort()) + world(true, 1, 514) + writer(writer.getLocalPort(), path))) {
            try (var occupied = new ModernEquipmentOverrideStore(config.equipmentStoreDirectory())) {
                assertThrows(IOException.class, () -> ModernMain.start(config));
                try (var outbox = new ModernMatchOutbox(path, ModernSettlementCoordinator.binding(config))) { assertTrue(outbox.isHealthy()); }
            }
            reopen(config); assertThrows(SocketTimeoutException.class, writer::accept);
        }
    }
    @Test void occupiedOutboxFailureDoesNotStealLockOrStartDatabaseWork() throws Exception {
        Path path = directory("outbox");
        try (var source = blackhole(); var writer = blackhole(); var config = parse(base(source.getLocalPort()) + world(true, 1, 514) + writer(writer.getLocalPort(), path))) {
            try (var occupied = new ModernMatchOutbox(path, ModernSettlementCoordinator.binding(config))) {
                assertThrows(IOException.class, () -> ModernMain.start(config)); assertTrue(occupied.isHealthy());
                assertThrows(SocketTimeoutException.class, writer::accept);
            }
            reopen(config);
        }
    }
    @Test void invalidWriterTrustFailsBeforeOpeningDurableOutbox() throws Exception {
        Path path = directory("outbox"), trust = temporary.resolve("invalid-trust.p12"); Files.write(trust, new byte[]{1,2,3});
        try (var source = blackhole(); var writer = blackhole(); var config = parse(base(source.getLocalPort()) + world(true, 1, 514)
                + writer(writer.getLocalPort(), path).replace("mysql.tls=disabled", "mysql.tls=verify_identity")
                + "settlement.mysql.trustStore.pkcs12=" + portable(trust) + "\nsettlement.mysql.trustStore.password.env=TRUST\n")) {
            assertThrows(Exception.class, () -> ModernMain.start(config)); assertFalse(Files.exists(path.resolve("outbox.mso")));
            assertThrows(SocketTimeoutException.class, writer::accept);
        }
    }
    @Test void durableStartupReplayUsesOnlyWriterAndShutdownRetainsUnconfirmedEvent() throws Exception {
        Path path = directory("outbox");
        try (var source = blackhole(); var writer = blackhole(); var config = parse(base(source.getLocalPort()) + world(true, 1, 514) + writer(writer.getLocalPort(), path))) {
            var event = new ModernMatchSettlement.Event(ModernMatchSettlementTest.ID, 7, 11, 19, 1, 514, 123456789,
                    List.of(ModernMatchSettlementTest.member(1, 7, 3333, 0, ModernMatchSettlement.Verdict.WIN),
                            ModernMatchSettlementTest.member(2, 7, 3334, 1, ModernMatchSettlement.Verdict.LOSS)));
            try (var outbox = new ModernMatchOutbox(path, ModernSettlementCoordinator.binding(config))) {
                outbox.record(event, System.nanoTime() + TimeUnit.SECONDS.toNanos(2));
            }
            writer.setSoTimeout(4000); var threads = Executors.newSingleThreadExecutor(); var entered = new CountDownLatch(1);
            ModernServer server = null;
            try {
                var peer = threads.submit(() -> {
                    try (var socket = writer.accept()) { socket.setSoTimeout(4000); entered.countDown(); return socket.getInputStream().read(); }
                });
                server = ModernMain.start(config);
                var coordinator = assertInstanceOf(ModernMatchSettlement.JournaledStore.class, store(server));
                assertTrue(entered.await(3, TimeUnit.SECONDS), "production coordinator must replay a recovered event through the real scoped writer");
                assertThrows(SocketTimeoutException.class, source::accept);
                stop(server); assertTrue(coordinator.isQuiescent()); assertEquals(-1, peer.get(3, TimeUnit.SECONDS));
                try (var outbox = new ModernMatchOutbox(path, ModernSettlementCoordinator.binding(config))) {
                    assertEquals(List.of(event), outbox.pending(System.nanoTime() + TimeUnit.SECONDS.toNanos(2)), "greeting failure cannot acknowledge/erase an event");
                }
                assertEquals("settlement-writer", config.settlement().database().username());
            } finally {
                if (server != null) stop(server); writer.close(); threads.shutdownNow(); assertTrue(threads.awaitTermination(4000, TimeUnit.MILLISECONDS));
            }
        }
    }
}
