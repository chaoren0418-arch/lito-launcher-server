package local.lito381.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RentalBattleHandoffStoreTest {
    @TempDir Path temporary;

    @Test
    void differentHubsShareExactOneShotInventoryAndRejectMismatch() throws Exception {
        Path journal = temporary.resolve("shared-handoff.properties");
        ServerConfig sourceConfig = config(journal, "source.properties");
        ServerConfig destinationConfig = config(journal, "destination.properties");
        RentalLoadoutPlan.Staged staged = RentalLoadoutPlan.require(1, 1).stage(
                RentalLoadoutPlan.StatePolicy.IDENTIFIED_TABLE_SLOTS,
                new AtomicLong(0x7410_0000L)::getAndIncrement);
        try (GameHub source = new GameHub(sourceConfig);
             GameHub destination = new GameHub(destinationConfig)) {
            assertTrue(source.stageRentalBattleHandoff(
                    "SharedAccount", "source::RentalKnight", staged.inventory()));
            assertFalse(destination.hasRentalBattleHandoff(
                    "sharedaccount", "source::WrongKnight"));
            assertNull(destination.consumeRentalBattleHandoff(
                    "sharedaccount", "source::WrongKnight"));
            assertTrue(destination.hasRentalBattleHandoff(
                    "sharedaccount", "SOURCE::RENTALKNIGHT"));

            GameHub.RentalBattleHandoff consumed =
                    destination.consumeRentalBattleHandoff(
                            "SHAREDACCOUNT", "source::RentalKnight");
            assertNotNull(consumed);
            assertEquals(staged.inventory().items(), consumed.inventory().items());
            assertEquals(staged.inventory().catalogCounts(),
                    consumed.inventory().catalogCounts());
            assertEquals(staged.inventory().equippedWalkAction(),
                    consumed.inventory().equippedWalkAction());
            assertEquals(staged.inventory().equippedPhysicalWeapon(),
                    consumed.inventory().equippedPhysicalWeapon());
            assertNull(source.consumeRentalBattleHandoff(
                    "sharedaccount", "source::RentalKnight"));
        }
    }

    @Test
    void expiryAndCorruptionFailClosedWithoutReplayingInventory() throws Exception {
        Path journal = temporary.resolve("expiry-handoff.properties");
        AtomicLong clock = new AtomicLong(10_000);
        RentalBattleHandoffStore source =
                new RentalBattleHandoffStore(journal, clock::get);
        RentalBattleHandoffStore destination =
                new RentalBattleHandoffStore(journal, clock::get);
        RuntimeInventory inventory = RentalLoadoutPlan.require(1, 1).stage(
                RentalLoadoutPlan.StatePolicy.IDENTIFIED_TABLE_SLOTS,
                new AtomicLong(0x7420_0000L)::getAndIncrement).inventory();
        assertTrue(source.stage("account", "character", inventory, 30_000));
        clock.set(40_000);
        assertFalse(destination.has("account", "character"));
        assertNull(source.consume("account", "character"));

        Files.writeString(journal, "format=1\nentry.count=1\n",
                StandardCharsets.UTF_8);
        assertThrows(java.io.IOException.class,
                () -> destination.has("account", "character"));
        assertThrows(java.io.IOException.class,
                () -> source.stage("account", "character", inventory, 30_000));
    }

    @Test
    void sourceBoundEquipmentWithoutPortablePhysicalMetadataIsRejected() {
        RuntimeInventory sourceBound = new RuntimeInventory();
        sourceBound.addDistinctState(
                InventoryCatalog.require(InventoryCatalog.SHORT_SWORD),
                new RuntimeInventory.PersistentItem(
                        0x7430_0000L, 1, 0, 0, true, 1, true));
        RentalBattleHandoffStore store = new RentalBattleHandoffStore(
                temporary.resolve("source-bound.properties"));
        assertThrows(IllegalArgumentException.class,
                () -> store.stage("account", "character", sourceBound, 30_000));
    }

    @Test
    void separateJvmConsumersCanClaimSharedCapsuleOnlyOnce() {
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            Path journal = temporary.resolve("process-handoff.properties");
            Path gate = temporary.resolve("consume.gate");
            Process staged = process("stage", journal.toString());
            String stagedOutput = output(staged);
            assertEquals(0, staged.exitValue(), stagedOutput);
            assertTrue(stagedOutput.startsWith("STAGED|66|10|3|"), stagedOutput);

            Process first = process("consume", journal.toString(), gate.toString());
            Process second = process("consume", journal.toString(), gate.toString());
            Files.writeString(gate, "go", StandardCharsets.US_ASCII);
            String firstOutput = output(first);
            String secondOutput = output(second);
            assertEquals(0, first.exitValue(), firstOutput);
            assertEquals(0, second.exitValue(), secondOutput);
            List<String> results = List.of(firstOutput.trim(), secondOutput.trim());
            assertEquals(1, results.stream().filter(
                    value -> value.startsWith("CONSUMED|66|10|3|")).count(), results::toString);
            assertEquals(1, results.stream().filter("EMPTY"::equals).count(), results::toString);
        });
    }

    private ServerConfig config(Path journal, String name) throws Exception {
        Path file = temporary.resolve(name);
        String path = journal.toAbsolutePath().normalize().toString()
                .replace('\\', '/');
        Files.writeString(file, """
                bind=127.0.0.1
                game.bind=127.0.0.2
                database.mode=memory
                database.read.only=true
                state.persistence.enabled=false
                lito.rental.handoff.file=%s
                """.formatted(path), StandardCharsets.UTF_8);
        return ServerConfig.load(file);
    }

    private static Process process(String... arguments) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java.exe")
                .toString();
        String classPath = System.getProperty("surefire.test.class.path",
                System.getProperty("java.class.path"));
        String[] command = new String[4 + arguments.length];
        command[0] = java;
        command[1] = "-cp";
        command[2] = classPath;
        command[3] = RentalBattleHandoffProcessProbe.class.getName();
        System.arraycopy(arguments, 0, command, 4, arguments.length);
        return new ProcessBuilder(command).redirectErrorStream(true).start();
    }

    private static String output(Process process) throws Exception {
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "probe process timeout");
        return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
