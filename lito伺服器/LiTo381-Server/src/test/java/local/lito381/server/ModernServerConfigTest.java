package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernServerConfigTest {
    @TempDir Path temporary;
    private static final Map<String, String> ENVIRONMENT = Map.of(
            "TEST_TLS", "keystore-only-secret", "TEST_USER", "database-user-only",
            "TEST_PASSWORD", "database-password-only", "TEST_TRUST", "truststore-only-secret");

    private String settlement() {
        return "settlement.enabled=true\nsettlement.outboxDirectory=" + portable(temporary) + "\n"
                + "settlement.mysql.host=127.0.0.1\nsettlement.mysql.port=3306\nsettlement.mysql.database=modern_results\n"
                + "settlement.mysql.username.env=WRITER_USER\nsettlement.mysql.password.env=WRITER_PASSWORD\nsettlement.mysql.tls=disabled\n";
    }
    private static String writerEnvironment(String name) {
        return switch (name) { case "WRITER_USER" -> "only-writer"; case "WRITER_PASSWORD" -> "only-writer-secret";
            default -> ENVIRONMENT.get(name); };
    }
    private ModernServerConfig parseWriter(String addition) throws Exception {
        return ModernServerConfig.parse(utf8(base() + addition), ModernServerConfigTest::writerEnvironment);
    }
    private void rejectWriterBeforeSecrets(String addition, ModernServerConfig.Failure reason) {
        var calls = new AtomicInteger();
        var failure = assertThrows(ModernServerConfig.ConfigException.class, () -> ModernServerConfig.parse(utf8(base() + addition), name -> {
            calls.incrementAndGet(); return writerEnvironment(name);
        }));
        assertEquals(reason, failure.failure()); assertEquals(0, calls.get()); assertNull(failure.getCause());
    }
    @Test void matchmakingRequiresExplicitStrictOptInAndAllAuthoritiesBeforeSecrets() throws Exception {
        try (var config = parseWriter("")) { assertFalse(config.matchmakingEnabled()); }
        try (var config = parseWriter("matchmaking.enabled=false\n")) { assertFalse(config.matchmakingEnabled()); }
        for (String value : List.of("", "TRUE", "1", "yes", "true "))
            rejectWriterBeforeSecrets("matchmaking.enabled=" + value + "\n", ModernServerConfig.Failure.INVALID_VALUE);
        rejectWriterBeforeSecrets("matchmaking.enabled=true\n", ModernServerConfig.Failure.INVALID_VALUE);
        rejectWriterBeforeSecrets(settlement() + "matchmaking.enabled=true\n", ModernServerConfig.Failure.INVALID_VALUE);
    }
    @Test void settlementDefaultsOffAndExplicitDisabledDoesNotRequestCredentials() throws Exception {
        try (var config = parseWriter("")) { assertNull(config.settlement()); }
        try (var config = parseWriter("settlement.enabled=false\n")) { assertNull(config.settlement()); }
        rejectWriterBeforeSecrets("settlement.mysql.database=modern_results\n", ModernServerConfig.Failure.INVALID_VALUE);
        rejectWriterBeforeSecrets(settlement().replace("enabled=true", "enabled=false"), ModernServerConfig.Failure.INVALID_VALUE);
        for (String value : List.of("", "TRUE", "1", "yes", "true "))
            rejectWriterBeforeSecrets("settlement.enabled=" + value + "\n", ModernServerConfig.Failure.INVALID_VALUE);
    }
    @Test void settlementIsIndependentOwnedConfigWithoutOpeningAnyFileOrDatabase() throws Exception {
        var config = parseWriter(settlement());
        var writer = config.settlement(); var database = writer.database();
        assertEquals(temporary, writer.outboxDirectory()); assertEquals("modern_results", database.database());
        assertEquals("only-writer", database.username()); assertArrayEquals("only-writer-secret".toCharArray(), database.password());
        assertNotSame(config.sources().get(0).database(), database);
        assertEquals("database-user-only", config.sources().get(0).database().username());
        assertFalse(Files.exists(temporary.resolve(".modern-match-outbox.lock")));
        assertFalse(Files.exists(temporary.resolve("outbox.mso")));
        assertEquals("ModernSettlementConfig[redacted]", writer.toString());
        config.close(); config.close();
        assertThrows(IllegalStateException.class, database::password);
        assertThrows(IllegalStateException.class, database::username);
        assertThrows(IllegalStateException.class, config.sources().get(0).database()::password);
    }
    @Test void settlementRequiresEveryFieldAndRejectsSourceOrCatalogSchemaEvenCaseFolded() {
        for (String field : List.of("outboxDirectory", "mysql.host", "mysql.port", "mysql.database", "mysql.username.env", "mysql.password.env", "mysql.tls")) {
            String missing = settlement().lines().filter(line -> !line.startsWith("settlement." + field + "=")).collect(Collectors.joining("\n")) + "\n";
            rejectWriterBeforeSecrets(missing, ModernServerConfig.Failure.INVALID_VALUE);
        }
        rejectWriterBeforeSecrets(settlement().replace("database=modern_results", "database=LITO_7"), ModernServerConfig.Failure.INVALID_VALUE);
        rejectWriterBeforeSecrets("source.7.catalog.database=CATALOG\n" + settlement().replace("database=modern_results", "database=catalog"), ModernServerConfig.Failure.INVALID_VALUE);
        rejectWriterBeforeSecrets(settlement().replace("database=modern_results", "database=a.b"), ModernServerConfig.Failure.INVALID_VALUE);
    }
    @Test void settlementHostTlsAndTrustRulesAreNotDowngraded() throws Exception {
        for (String host : List.of("localhost", "example.test", "0.0.0.0", "255.255.255.255", "224.0.0.1", "169.254.1.1", "192.0.2.7"))
            rejectWriterBeforeSecrets(settlement().replace("mysql.host=127.0.0.1", "mysql.host=" + host), ModernServerConfig.Failure.INVALID_VALUE);
        for (String mode : List.of("preferred", "required", "verify_ca", "DISABLED"))
            rejectWriterBeforeSecrets(settlement().replace("mysql.tls=disabled", "mysql.tls=" + mode), ModernServerConfig.Failure.INVALID_VALUE);
        rejectWriterBeforeSecrets(settlement() + "settlement.mysql.trustStore.pkcs12=" + portable(temporary.resolve("root.p12")) + "\n", ModernServerConfig.Failure.INVALID_VALUE);
        String remote = settlement().replace("mysql.host=127.0.0.1", "mysql.host=192.0.2.7").replace("mysql.tls=disabled", "mysql.tls=verify_identity")
                + "settlement.mysql.trustStore.pkcs12=" + portable(temporary.resolve("root.p12")) + "\nsettlement.mysql.trustStore.password.env=TEST_TRUST\n";
        try (var config = parseWriter(remote)) {
            assertEquals("VERIFY_IDENTITY", config.settlement().database().sslMode());
            assertEquals(temporary.resolve("root.p12"), config.settlement().database().trustStorePath());
            assertArrayEquals("truststore-only-secret".toCharArray(), config.settlement().database().trustStorePassword());
        }
    }
    @Test void settlementPathMustBeExistingLocalDirectoryAndNeverCreatesOne() {
        for (String path : List.of("relative", portable(temporary.resolve("absent")), portable(temporary.getRoot()), "//server/share/outbox"))
            rejectWriterBeforeSecrets(settlement().replace(portable(temporary), path), ModernServerConfig.Failure.INVALID_PATH);
        assertFalse(Files.exists(temporary.resolve("absent")));
    }
    @Test void settlementChecksActualLockAndAuthorityFileShapesBeforeReadingAnySecret() throws Exception {
        for (String name : List.of(".modern-match-outbox.lock", "outbox.mso")) {
            Path outbox = Files.createDirectory(temporary.resolve(name.equals("outbox.mso") ? "bad-authority" : "bad-lock"));
            Files.createDirectory(outbox.resolve(name));
            rejectWriterBeforeSecrets(settlement().replace(portable(temporary), portable(outbox)), ModernServerConfig.Failure.INVALID_PATH);
        }
    }
    @Test void settlementUnknownDuplicateAndLiteralSecretsAreRejectedBeforeAnyEnvironmentRead() {
        rejectWriterBeforeSecrets(settlement() + "settlement.mysql.password=literal\n", ModernServerConfig.Failure.UNKNOWN_PROPERTY);
        rejectWriterBeforeSecrets(settlement() + "settlement.mysql.passwordScheme=plain\n", ModernServerConfig.Failure.UNKNOWN_PROPERTY);
        rejectWriterBeforeSecrets(settlement() + "settlement.mysql.jdbcUrl=jdbc:mysql://unsafe\n", ModernServerConfig.Failure.UNKNOWN_PROPERTY);
        rejectWriterBeforeSecrets(settlement() + "settlement.enabled=true\n", ModernServerConfig.Failure.DUPLICATE_PROPERTY);
        rejectWriterBeforeSecrets(settlement().replace("WRITER_PASSWORD", "unsafe.env"), ModernServerConfig.Failure.INVALID_ENVIRONMENT);
    }
    @Test void settlementMissingSecretFailsRedactedAndDoesNotReturnPartialConfig() {
        var error = assertThrows(ModernServerConfig.ConfigException.class, () -> ModernServerConfig.parse(utf8(base() + settlement()), name ->
                name.equals("WRITER_PASSWORD") ? null : writerEnvironment(name)));
        assertEquals(ModernServerConfig.Failure.INVALID_ENVIRONMENT, error.failure());
        assertNull(error.getCause()); assertEquals(0, error.getSuppressed().length);
        assertFalse(error.toString().contains("WRITER_PASSWORD")); assertFalse(error.toString().contains("only-writer"));
    }

    @Test void equipmentIsExplicitExistingDirectoryAndRequiresEveryCatalogWithoutOpeningStore() throws Exception {
        try (var absent = parse(base())) { assertNull(absent.equipmentStoreDirectory()); }
        String enabled = base() + "source.7.catalog.database=game_catalog\nequipment.storeDirectory=" + portable(temporary) + "\n";
        try (var config = parse(enabled)) {
            assertEquals(temporary, config.equipmentStoreDirectory());
            assertFalse(Files.exists(temporary.resolve(".modern-equipment.lock")));
        }
        rejects(base() + "equipment.storeDirectory=" + portable(temporary) + "\n", ModernServerConfig.Failure.INVALID_VALUE);
        for (String path : List.of("relative", portable(temporary.resolve("absent")), portable(temporary.getRoot()))) {
            rejects(base() + "source.7.catalog.database=game_catalog\nequipment.storeDirectory=" + path + "\n", ModernServerConfig.Failure.INVALID_PATH);
        }
    }

    private String base() {
        return "schema=1\nbind.port=0\ntls.pkcs12=" + portable(temporary.resolve("server.p12"))
                + "\ntls.password.env=TEST_TLS\nsources=7\n" + source(7, "太陽神");
    }

    @Test void speechAdministratorsAreExplicitPerSourceAndDefaultToEmpty() throws Exception {
        try(var config=parse(base())) { assertTrue(config.sources().get(0).administrationAccounts().isEmpty()); }
        String both=base().replace("sources=7", "sources=7,42")+source(42,"月亮神")
                +"source.7.administration.accounts=SunGM, 管理員\nsource.42.administration.accounts=MoonGM\n";
        try(var config=parse(both)) {
            var bindings=ModernSpeechAuthority.sources(config);
            assertEquals(java.util.Set.of("sungm","管理員"),bindings.get(7).administrators());
            assertEquals(java.util.Set.of("moongm"),bindings.get(42).administrators());
            assertThrows(UnsupportedOperationException.class,()->bindings.clear());
            assertFalse(config.sources().toString().contains("SunGM"));
        }
    }
    @Test void invalidSpeechConfigurationIsRejectedBeforeReadingCredentials() {
        for(String value:List.of("a,A","a,","a,,b","界".repeat(22),"İ".repeat(22),"a".repeat(65)))
            rejectWriterBeforeSecrets("source.7.administration.accounts="+value+"\n",ModernServerConfig.Failure.INVALID_VALUE);
        rejectWriterBeforeSecrets("source.42.administration.accounts=SunGM\n",ModernServerConfig.Failure.UNKNOWN_PROPERTY);
        rejectWriterBeforeSecrets("administration.accounts=SunGM\n",ModernServerConfig.Failure.UNKNOWN_PROPERTY);
    }

    private static String source(int id, String name) {
        String prefix = "source." + id + ".";
        return prefix + "name=" + name + "\n" + prefix + "mysql.host=127.0.0.1\n"
                + prefix + "mysql.port=3306\n" + prefix + "mysql.database=lito_" + id + "\n"
                + prefix + "mysql.username.env=TEST_USER\n" + prefix + "mysql.password.env=TEST_PASSWORD\n"
                + prefix + "mysql.passwordScheme=plain\n" + prefix + "mysql.tls=disabled\n";
    }

    private static String portable(Path path) { return path.toAbsolutePath().toString().replace('\\', '/'); }
    private static byte[] utf8(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    private static ModernServerConfig parse(String value) throws ModernServerConfig.ConfigException {
        return ModernServerConfig.parse(utf8(value), ENVIRONMENT::get);
    }
    private void rejects(String value, ModernServerConfig.Failure failure) {
        var error = assertThrows(ModernServerConfig.ConfigException.class, () -> parse(value));
        assertEquals(failure, error.failure());
        for (String secret : ENVIRONMENT.values()) assertFalse(error.toString().contains(secret));
        assertNull(error.getCause());
    }

    @Test void minimalConfigIsLoopbackEphemeralAndOnlyLoadsInjectedEnvironment() throws Exception {
        AtomicInteger lookups = new AtomicInteger();
        try (var config = ModernServerConfig.parse(utf8(base()), name -> {
            lookups.incrementAndGet();
            return ENVIRONMENT.get(name);
        })) {
            assertEquals(3, lookups.get());
            assertTrue(config.bind().getAddress().isLoopbackAddress());
            assertFalse(config.bind().isUnresolved());
            assertEquals(0, config.bind().getPort());
            assertEquals(temporary.resolve("server.p12"), config.tls().keyStore());
            assertFalse(Files.exists(config.tls().keyStore()), "configuration must not create/open a key-store");
            assertEquals(1, config.sources().size());
            var source = config.sources().get(0);
            assertEquals(7, source.id());
            assertEquals("太陽神", source.name());
            assertEquals("lito_7", source.database().database());
            assertNull(source.catalogDatabase(), "an absent catalog binding never falls back to the account schema");
            assertEquals("127.0.0.1", source.database().host());
            assertEquals(3306, source.database().port());
            assertEquals("plain", source.database().passwordScheme());
            assertEquals("DISABLED", source.database().sslMode());
            assertNull(source.database().trustStorePath());
            assertNull(source.database().trustStorePassword());
            assertEquals("database-user-only", source.database().username());
            assertEquals(32, config.maxSessions());
            assertEquals(4, config.maxPerIp());
            assertEquals(4, config.backendWorkers());
            assertEquals(32, config.backendQueue());
            assertEquals(5000, config.backendDeadlineMillis());
            assertEquals(5000, config.handshakeMillis());
            assertEquals(5000, config.frameMillis());
            assertEquals(10000, config.applicationReadMillis());
            assertEquals(1000, config.dbConnectMillis());
            assertEquals(3000, config.dbNetworkMillis());
            assertEquals(60, config.acceptPerMinute());
            assertEquals(4, config.acceptBurst());
        }
    }

    @Test void explicitSourcesAreSortedAndImmutableWithoutPrimarySourceFallback() throws Exception {
        String value = base().replace("sources=7", "sources=7,3") + source(3, "月神");
        try (var config = parse(value)) {
            assertEquals(List.of(3, 7), config.sources().stream().map(ModernServerConfig.SourceConfig::id).toList());
            assertEquals("月神", config.sources().get(0).name());
            assertEquals("lito_3", config.sources().get(0).database().database());
            assertEquals("lito_7", config.sources().get(1).database().database());
            assertThrows(UnsupportedOperationException.class, () -> config.sources().clear());
        }
    }

    @Test void catalogBindingIsExplicitSourceSpecificAndOptionalForOlderRosterConfigurations() throws Exception {
        String value = base().replace("sources=7", "sources=7,3") + source(3, "月神")
                + "source.7.catalog.database=lito_static\n";
        try (var config = parse(value)) {
            assertEquals("lito_3", config.sources().get(0).database().database());
            assertNull(config.sources().get(0).catalogDatabase());
            assertEquals("lito_7", config.sources().get(1).database().database());
            assertEquals("lito_static", config.sources().get(1).catalogDatabase());
        }
        try (var config = parse(value + "source.3.catalog.database=catalog_3\n")) {
            assertEquals("catalog_3", config.sources().get(0).catalogDatabase());
            assertEquals("lito_static", config.sources().get(1).catalogDatabase());
        }
        for (String name : List.of("", "catalog.weapon", "`catalog`", "catalog?x=1", "a".repeat(65), "目錄", "catalog ")) {
            AtomicInteger lookups = new AtomicInteger();
            var error = assertThrows(ModernServerConfig.ConfigException.class,
                    () -> ModernServerConfig.parse(utf8(base() + "source.7.catalog.database=" + name + "\n"), key -> {
                        lookups.incrementAndGet(); return ENVIRONMENT.get(key);
                    }));
            assertEquals(ModernServerConfig.Failure.INVALID_VALUE, error.failure());
            assertEquals(0, lookups.get(), "unsafe catalog identifiers must fail before any secret is loaded");
        }
        rejects(base() + "source.8.catalog.database=lito_static\n", ModernServerConfig.Failure.UNKNOWN_PROPERTY);
        rejects(base() + "source.7.catalog.database=first\nsource.7.catalog.database=second\n",
                ModernServerConfig.Failure.DUPLICATE_PROPERTY);
    }

    @Test void decodedDuplicateUnknownKeysAndUndeclaredSourcesAreRejectedBeforeEnvironmentLookup() {
        for (String addition : List.of("schema=1\n", "\\u0073chema=1\n", "source.7.name=alias\n")) {
            rejects(base() + addition, ModernServerConfig.Failure.DUPLICATE_PROPERTY);
        }
        for (String addition : List.of("source.8.name=unknown\n", "native.config=secret\n", "tls.password=literal-secret\n",
                "source.7.mysql.jdbcUrl=jdbc:mysql://example/other\n", "limits.workres=2\n")) {
            AtomicInteger lookups = new AtomicInteger();
            var error = assertThrows(ModernServerConfig.ConfigException.class,
                    () -> ModernServerConfig.parse(utf8(base() + addition), name -> {
                        lookups.incrementAndGet(); return ENVIRONMENT.get(name);
                    }));
            assertEquals(ModernServerConfig.Failure.UNKNOWN_PROPERTY, error.failure());
            assertEquals(0, lookups.get());
        }
    }

    @Test void utf8PropertiesAndByteBudgetAreStrict() {
        var malformed = assertThrows(ModernServerConfig.ConfigException.class,
                () -> ModernServerConfig.parse(new byte[]{(byte) 0xc0, (byte) 0xaf}, ENVIRONMENT::get));
        assertEquals(ModernServerConfig.Failure.INVALID_UTF8, malformed.failure());
        rejects("\ufeff" + base(), ModernServerConfig.Failure.INVALID_PROPERTIES);
        rejects(base() + "#bad\0comment", ModernServerConfig.Failure.INVALID_PROPERTIES);
        rejects(base().replace("太陽神", "\\uD800"), ModernServerConfig.Failure.INVALID_PROPERTIES);
        rejects(base().replace("太陽神", "\\u0000"), ModernServerConfig.Failure.INVALID_PROPERTIES);
        rejects(base() + "invalid=\\uZZZZ\n", ModernServerConfig.Failure.INVALID_PROPERTIES);
        rejects(base() + "#" + "a".repeat(ModernServerConfig.MAX_CONFIG_BYTES), ModernServerConfig.Failure.FILE_TOO_LARGE);
        rejects(base().replace("太陽神", "a".repeat(1025)), ModernServerConfig.Failure.INVALID_PROPERTIES);
        byte[] exact = utf8(base() + "#" + "a".repeat(ModernServerConfig.MAX_CONFIG_BYTES - utf8(base()).length - 1));
        assertEquals(ModernServerConfig.MAX_CONFIG_BYTES, exact.length);
        assertDoesNotThrow(() -> { try (var ignored = ModernServerConfig.parse(exact, ENVIRONMENT::get)) {} });
    }

    @Test void sourceCountIdsNamesAndRequiredBindingAreBounded() throws Exception {
        for (String ids : List.of("", "0", "-1", "01", "7,7", "7,", "7, 3", "2147483648")) {
            rejects(base().replace("sources=7", "sources=" + ids), ModernServerConfig.Failure.INVALID_VALUE);
        }
        String tooMany = IntStream.rangeClosed(1, 65).mapToObj(Integer::toString).collect(Collectors.joining(","));
        rejects(base().replace("sources=7", "sources=" + tooMany), ModernServerConfig.Failure.INVALID_VALUE);
        for (String name : List.of("a".repeat(65), "太陽神 ", "   ")) {
            rejects(base().replace("name=太陽神", "name=" + name), ModernServerConfig.Failure.INVALID_VALUE);
        }
        rejects(base().replace("sources=7", "sources=7,3") + source(3, "太陽神"), ModernServerConfig.Failure.INVALID_VALUE);
        rejects(base().replace("name=太陽神", "name=I").replace("sources=7", "sources=7,3")
                + source(3, "ı"), ModernServerConfig.Failure.INVALID_VALUE);
        rejects(base().replace("source.7.mysql.database=lito_7\n", ""), ModernServerConfig.Failure.INVALID_VALUE);
        rejects(base().replace("mysql.database=lito_7", "mysql.database=lito_7?allowMultiQueries=true"), ModernServerConfig.Failure.INVALID_VALUE);
        rejects(base().replace("mysql.passwordScheme=plain", "mysql.passwordScheme=auto"), ModernServerConfig.Failure.INVALID_VALUE);
        rejects(base().replace("source.7.mysql.passwordScheme=plain\n", ""), ModernServerConfig.Failure.INVALID_VALUE);

        String sixtyFour = IntStream.rangeClosed(1, 64).mapToObj(Integer::toString).collect(Collectors.joining(","));
        String header = base().substring(0, base().indexOf("sources=")) + "sources=" + sixtyFour + "\n";
        String all = header + IntStream.rangeClosed(1, 64).mapToObj(id -> source(id, "Source" + id)).collect(Collectors.joining());
        assertTrue(utf8(all).length <= ModernServerConfig.MAX_CONFIG_BYTES);
        try (var config = parse(all)) { assertEquals(64, config.sources().size()); }
    }

    @Test void onlyNumericAddressesAndExplicitSafeDbTlsModesAreAccepted() throws Exception {
        for (String host : List.of("localhost", "127.1", "0177.0.0.1", "127.0.0.1:3306", "127.0.0.1?x=y", "[::1]",
                "::1%1", "256.0.0.1", "224.0.0.1", "255.255.255.255", "fe80::1")) {
            rejects(base() + "bind.host=" + host + "\n", ModernServerConfig.Failure.INVALID_VALUE);
        }
        for (String host : List.of("0.0.0.0", "192.0.2.1", "::", "2001:db8::1")) {
            rejects(base().replace("mysql.host=127.0.0.1", "mysql.host=" + host), ModernServerConfig.Failure.INVALID_VALUE);
        }
        for (String host : List.of("127.12.3.4", "::1", "::ffff:127.0.0.1")) {
            try (var config = parse(base().replace("mysql.host=127.0.0.1", "mysql.host=" + host))) {
                assertEquals("DISABLED", config.sources().get(0).database().sslMode());
            }
        }
        try (var config = parse(base() + "bind.host=::\n")) {
            assertTrue(config.bind().getAddress().isAnyLocalAddress());
        }
        rejects(base().replace("mysql.tls=disabled", "mysql.tls=preferred"), ModernServerConfig.Failure.INVALID_VALUE);
        rejects(base().replace("source.7.mysql.tls=disabled\n", ""), ModernServerConfig.Failure.INVALID_VALUE);
    }

    private String verifiedRemote() {
        return base().replace("mysql.host=127.0.0.1", "mysql.host=192.0.2.1")
                .replace("mysql.tls=disabled", "mysql.tls=verify_identity")
                + "source.7.mysql.trustStore.pkcs12=" + portable(temporary.resolve("trust.p12")) + "\n"
                + "source.7.mysql.trustStore.password.env=TEST_TRUST\n";
    }

    @Test void verifiedRemoteRequiresExplicitLocalTrustStoreAndOwnedSecret() throws Exception {
        try (var config = parse(verifiedRemote())) {
            var db = config.sources().get(0).database();
            assertEquals("192.0.2.1", db.host());
            assertEquals("VERIFY_IDENTITY", db.sslMode());
            assertEquals(temporary.resolve("trust.p12"), db.trustStorePath());
            char[] copy = db.trustStorePassword();
            assertArrayEquals(ENVIRONMENT.get("TEST_TRUST").toCharArray(), copy);
            Arrays.fill(copy, '\0');
            assertArrayEquals(ENVIRONMENT.get("TEST_TRUST").toCharArray(), db.trustStorePassword());
        }
        rejects(base().replace("mysql.tls=disabled", "mysql.tls=verify_identity"), ModernServerConfig.Failure.INVALID_VALUE);
        rejects(verifiedRemote().replace("source.7.mysql.trustStore.password.env=TEST_TRUST\n", ""), ModernServerConfig.Failure.INVALID_VALUE);
        rejects(base() + "source.7.mysql.trustStore.password.env=TEST_TRUST\n", ModernServerConfig.Failure.INVALID_VALUE);
        rejects(verifiedRemote().replace("mysql.host=192.0.2.1", "mysql.host=0.0.0.0"), ModernServerConfig.Failure.INVALID_VALUE);
        rejects(verifiedRemote().replace(portable(temporary.resolve("trust.p12")), "relative.p12"), ModernServerConfig.Failure.INVALID_PATH);
    }

    @Test void limitsAreFiniteAndCrossCheckedWithZeroQueueAllowed() throws Exception {
        Map<String, List<String>> bad = Map.ofEntries(
                Map.entry("maxSessions", List.of("0", "257")), Map.entry("maxPerIp", List.of("0", "33")),
                Map.entry("backendWorkers", List.of("0", "33")), Map.entry("backendQueue", List.of("-1", "257")),
                Map.entry("backendDeadlineMillis", List.of("99", "30001")), Map.entry("handshakeMillis", List.of("0", "30001")),
                Map.entry("frameMillis", List.of("0", "30001")), Map.entry("applicationReadMillis", List.of("0", "30001")),
                Map.entry("dbConnectMillis", List.of("99", "5001")), Map.entry("dbNetworkMillis", List.of("99", "5001")),
                Map.entry("acceptPerMinute", List.of("0", "3601")), Map.entry("acceptBurst", List.of("0", "33")));
        for (var item : bad.entrySet()) for (String value : item.getValue()) {
            rejects(base() + "limits." + item.getKey() + "=" + value + "\n", ModernServerConfig.Failure.INVALID_VALUE);
        }
        for (String port : List.of("-1", "65536", "+1", "00", "1.0", "999999999999", "1 ")) {
            rejects(base().replace("bind.port=0", "bind.port=" + port), ModernServerConfig.Failure.INVALID_VALUE);
        }
        try (var config = parse(base() + "limits.maxSessions=1\nlimits.backendQueue=0\nlimits.backendDeadlineMillis=100\n")) {
            assertEquals(1, config.maxPerIp());
            assertEquals(1, config.acceptBurst());
            assertEquals(0, config.backendQueue());
            assertEquals(100, config.dbConnectMillis());
            assertEquals(100, config.dbNetworkMillis());
        }
    }

    @Test void environmentNamesValuesAndExceptionsCannotLeakIntoConfigErrors() {
        for (String name : List.of("literal password", "1BAD", "$ENV", "TEST_TLS;echo", "a".repeat(129))) {
            rejects(base().replace("tls.password.env=TEST_TLS", "tls.password.env=" + name), ModernServerConfig.Failure.INVALID_ENVIRONMENT);
        }
        for (String value : List.of("", "a".repeat(1025), "password\0tail", "password\nline", "\ud800")) {
            var error = assertThrows(ModernServerConfig.ConfigException.class,
                    () -> ModernServerConfig.parse(utf8(base()), name -> name.equals("TEST_TLS") ? value : ENVIRONMENT.get(name)));
            assertEquals(ModernServerConfig.Failure.INVALID_ENVIRONMENT, error.failure());
            assertNull(error.getCause());
        }
        for (Function<String, String> lookup : List.<Function<String, String>>of(name -> null,
                name -> { throw new IllegalStateException("must-not-leak-an-environment-secret"); })) {
            var error = assertThrows(ModernServerConfig.ConfigException.class, () -> ModernServerConfig.parse(utf8(base()), lookup));
            assertEquals(ModernServerConfig.Failure.INVALID_ENVIRONMENT, error.failure());
            assertNull(error.getCause());
            assertFalse(error.toString().contains("must-not-leak"));
        }
        // All binding/path/limit validation precedes even TLS secret lookup.
        AtomicInteger lookups = new AtomicInteger();
        assertThrows(ModernServerConfig.ConfigException.class,
                () -> ModernServerConfig.parse(utf8(base() + "limits.maxSessions=0\n"), name -> {
                    lookups.incrementAndGet(); return ENVIRONMENT.get(name);
                }));
        assertEquals(0, lookups.get());
    }

    @Test void acceptedConfigLimitsReachTheListenerEvenWithFewerSessionsThanBackendWorkers() throws Exception {
        for (int maximum : new int[]{1, 2, 256}) {
            for (String overrides : List.of("", "limits.backendWorkers=32\nlimits.backendQueue=256\n")) {
                try (var config = parse(base() + "limits.maxSessions=" + maximum + "\n" + overrides)) {
                    var listener = assertDoesNotThrow(() -> new ModernServer.Limits(config.maxSessions(), config.maxPerIp(),
                            config.acceptPerMinute(), config.acceptBurst(), config.backendWorkers(), config.backendQueue(),
                            config.backendDeadlineMillis(), config.handshakeMillis(), config.frameMillis(),
                            config.applicationReadMillis()));
                    assertEquals(maximum, listener.maxSessions());
                    assertEquals(config.backendWorkers(), listener.backendWorkers());
                    assertEquals(config.backendQueue(), listener.backendQueue());
                }
            }
        }
    }

    @Test void secretAccessorsAreDefensiveRedactedAndRevokedAfterIdempotentClose() throws Exception {
        var config = parse(verifiedRemote());
        var db = config.sources().get(0).database();
        char[] ownedTls = ownedArray(config.tls(), "password");
        char[] ownedUsername = ownedArray(db, "username");
        char[] ownedPassword = ownedArray(db, "password");
        char[] ownedTrustPassword = ownedArray(db, "trustStorePassword");
        char[] tlsCopy = config.tls().password();
        char[] dbCopy = db.password();
        Arrays.fill(tlsCopy, 'x');
        Arrays.fill(dbCopy, 'x');
        assertArrayEquals(ENVIRONMENT.get("TEST_TLS").toCharArray(), config.tls().password());
        assertArrayEquals(ENVIRONMENT.get("TEST_PASSWORD").toCharArray(), db.password());
        for (Object printable : List.of(config, config.tls(), config.sources().get(0), db)) {
            for (String value : ENVIRONMENT.values()) assertFalse(printable.toString().contains(value));
            assertFalse(printable.toString().contains("TEST_"));
        }
        config.close();
        config.close();
        for (char[] owned : List.of(ownedTls, ownedUsername, ownedPassword, ownedTrustPassword)) {
            assertArrayEquals(new char[owned.length], owned, "close must erase the actual owned array, not just deny access");
        }
        assertThrows(IllegalStateException.class, () -> config.tls().password());
        assertThrows(IllegalStateException.class, db::password);
        assertThrows(IllegalStateException.class, db::username);
        assertThrows(IllegalStateException.class, db::trustStorePassword);
        assertEquals("database-password-only", ENVIRONMENT.get("TEST_PASSWORD"), "process env fixture was never changed");
    }

    private static char[] ownedArray(Object owner, String name) throws ReflectiveOperationException {
        var field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (char[]) field.get(owner);
    }

    @Test void pathsMustBeAbsoluteLocalRegularAndWithoutAdsTraversalOrDeviceAliases() throws Exception {
        String existing = portable(temporary.resolve("server.p12"));
        for (String path : List.of("relative.p12", "../server.p12", "//server/share/server.p12", "C:/server.p12:secret",
                portable(temporary) + "/../server.p12", portable(temporary) + "/server.p12.",
                portable(temporary) + "/server.p12 ", portable(temporary) + "/NUL.p12")) {
            rejects(base().replace(existing, path), ModernServerConfig.Failure.INVALID_PATH);
        }
        rejects(base().replace(existing, portable(temporary)), ModernServerConfig.Failure.INVALID_PATH);
        Path notDirectory = temporary.resolve("not-directory");
        Files.writeString(notDirectory, "fixture");
        rejects(base().replace(existing, portable(notDirectory.resolve("server.p12"))), ModernServerConfig.Failure.INVALID_PATH);
        Files.writeString(temporary.resolve("server.p12"), "not read or parsed by config");
        try (var config = parse(base())) { assertEquals(temporary.resolve("server.p12"), config.tls().keyStore()); }
    }

    @Test void loadReadsAtMost32KiBAndRequiresAnAbsoluteConfigPath() throws Exception {
        Path fixture = temporary.resolve("modern.properties");
        Files.write(fixture, utf8(base()));
        try (var config = ModernServerConfig.load(fixture, ENVIRONMENT::get)) { assertEquals(7, config.sources().get(0).id()); }
        assertThrows(ModernServerConfig.ConfigException.class, () -> ModernServerConfig.load(Path.of("modern.properties"), ENVIRONMENT::get));
        Files.write(fixture, new byte[ModernServerConfig.MAX_CONFIG_BYTES + 1]);
        var oversized = assertThrows(ModernServerConfig.ConfigException.class, () -> ModernServerConfig.load(fixture, ENVIRONMENT::get));
        assertEquals(ModernServerConfig.Failure.FILE_TOO_LARGE, oversized.failure());
    }

    @Test void loadCannotFollowFinalOrAncestorSymlinks() throws Exception {
        Path actual = temporary.resolve("actual.properties");
        Files.write(actual, utf8(base()));
        Path link = temporary.resolve("linked.properties");
        try { Files.createSymbolicLink(link, actual); }
        catch (IOException | UnsupportedOperationException | SecurityException unavailable) {
            assumeTrue(false, "symbolic-link privileges are unavailable for this test host");
        }
        var finalLink = assertThrows(ModernServerConfig.ConfigException.class, () -> ModernServerConfig.load(link, ENVIRONMENT::get));
        assertEquals(ModernServerConfig.Failure.INVALID_PATH, finalLink.failure());
        Path directoryLink = temporary.resolve("linked-directory");
        Files.createSymbolicLink(directoryLink, temporary);
        var ancestorLink = assertThrows(ModernServerConfig.ConfigException.class,
                () -> ModernServerConfig.load(directoryLink.resolve("actual.properties"), ENVIRONMENT::get));
        assertEquals(ModernServerConfig.Failure.INVALID_PATH, ancestorLink.failure());
        rejects(base().replace(portable(temporary.resolve("server.p12")), portable(directoryLink.resolve("server.p12"))),
                ModernServerConfig.Failure.INVALID_PATH);
    }
}
