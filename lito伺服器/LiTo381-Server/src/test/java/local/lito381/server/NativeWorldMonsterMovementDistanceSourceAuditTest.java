package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Source-order proof that the leash belongs after every committed NPC step. */
final class NativeWorldMonsterMovementDistanceSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    @Test
    void retainedMoveBroadcastsStepBeforeMonsterHomeTeleport() throws Exception {
        Path path = Path.of("..", "1-伺服器", "src", "com", "lineage",
                "server", "model", "Instance", "NpcMove.java")
                .toAbsolutePath().normalize();
        String source = Files.readString(path, StandardCharsets.UTF_8);
        int method = source.indexOf("public void setDirectionMove(final int dir)");
        int broadcast = source.indexOf(
                "broadcastPacketAll(new S_MoveCharPacket", method);
        int gate = source.indexOf("getMovementDistance() > 0", broadcast);
        int monster = source.indexOf("instanceof L1MonsterInstance", gate);
        int distance = source.indexOf("getLineDistance", monster);
        int teleport = source.indexOf("teleport(_npc.getHomeX()", distance);
        assertTrue(method >= 0 && method < broadcast && broadcast < gate
                && gate < monster && monster < distance && distance < teleport);
    }

    @Test
    void runtimeAppliesSameSettlementToPursuitEscapeAndIdle() throws Exception {
        String population = Files.readString(Path.of("src", "main", "java",
                "local", "lito381", "server",
                "ClassicWorldMonsterPopulation.java"));
        int toward = population.indexOf("synchronized Movement moveToward(");
        int towardSettle = population.indexOf(
                "applyMovementDistance(actor)", toward);
        int away = population.indexOf("synchronized Movement moveAway(");
        int awaySettle = population.indexOf(
                "applyMovementDistance(actor)", away);
        int idle = population.indexOf("synchronized IdleMovement idleMove(");
        int idleSettle = population.indexOf(
                "applyMovementDistance(actor)", idle);
        assertTrue(toward >= 0 && towardSettle > toward
                && away > towardSettle && awaySettle > away
                && idle > awaySettle && idleSettle > idle);
    }
}
