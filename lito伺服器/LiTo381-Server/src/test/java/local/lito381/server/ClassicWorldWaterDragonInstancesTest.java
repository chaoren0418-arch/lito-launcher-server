package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClassicWorldWaterDragonInstancesTest {
    private static final long START = 9_000_000_000L;

    @Test
    void admissionUsesCredentialShowIdCapacityAndOneSharedDeadline() {
        ClassicWorldWaterDragonInstances instances =
                new ClassicWorldWaterDragonInstances();
        assertEquals(ClassicWorldWaterDragonInstances.AdmissionStatus.INVALID_CREDENTIAL,
                instances.admit("S:A", 3, 80, false, false, START).status());
        assertEquals(ClassicWorldWaterDragonInstances.AdmissionStatus.LEVEL_TOO_LOW,
                instances.admit("S:A", 3, 59, true, false, START).status());
        assertEquals(ClassicWorldWaterDragonInstances.AdmissionStatus.COOLDOWN_ACTIVE,
                instances.admit("S:A", 3, 80, true, true, START).status());

        ClassicWorldWaterDragonInstances.Admission first =
                instances.admit("S:P0", 3, 60, true, false, START);
        assertTrue(first.admitted());
        assertTrue(first.created());
        assertEquals(START + 1_800_000_000_000L, first.deadlineNanos());
        assertEquals(6, first.initialSpawns().size());
        assertEquals(3, first.initialDoors().size());
        for (int index = 1; index < 8; index++) {
            ClassicWorldWaterDragonInstances.Admission admission = instances.admit(
                    "S:P" + index, 3, 80, true, false, START + index);
            assertTrue(admission.admitted());
            assertFalse(admission.created());
            assertTrue(admission.initialSpawns().isEmpty());
        }
        assertEquals(ClassicWorldWaterDragonInstances.AdmissionStatus.CAPACITY_REACHED,
                instances.admit("S:P8", 3, 80, true, false, START + 9).status());
        assertEquals(8, instances.snapshot(3).joinedIdentities().size());
    }

    @Test
    void leavingDoesNotPermitNativeReentryAndGuardiansOpenOnlyTheirShowDoor() {
        ClassicWorldWaterDragonInstances instances =
                new ClassicWorldWaterDragonInstances();
        instances.admit("S:A", 4, 80, true, false, START);
        assertEquals(10008, instances.guardianDied(4, 70976).doorId());
        assertNull(instances.guardianDied(4, 70976));
        assertTrue(instances.snapshot(4).openDoors().get(10008));
        assertFalse(instances.snapshot(4).openDoors().get(10009));
        assertNull(instances.guardianDied(5, 70977));

        assertEquals(Set.of("s:a"),
                instances.leave("S:A", "map-leave").joinedIdentities());
        assertEquals(ClassicWorldWaterDragonInstances.AdmissionStatus.REENTRY_DENIED,
                instances.admit("s:a", 4, 80, true, false, START + 1).status());
    }

    @Test
    void bossGateSchedulesExactThirtySecondPrologueOnce() {
        ClassicWorldWaterDragonInstances instances = joined(6, "S:A");
        ClassicWorldWaterDragonInstances.GateTalk first =
                instances.talkBossGate("s:a", START);
        assertTrue(first.accepted());
        assertTrue(first.scheduled());
        assertEquals(ClassicWorldWaterDragonCatalog.BOSS_GATE_DESTINATION,
                first.destination());
        assertFalse(instances.talkBossGate("S:A", START + 1).scheduled());
        assertTrue(instances.poll(START + 29_999_999_999L).isEmpty());

        assertEquals(List.of(1657), values(instances.poll(
                START + 30_000_000_000L),
                ClassicWorldWaterDragonInstances.EventKind.MESSAGE));
        assertEquals(List.of(1658), values(instances.poll(
                START + 32_000_000_000L),
                ClassicWorldWaterDragonInstances.EventKind.MESSAGE));
        assertEquals(List.of(1659), values(instances.poll(
                START + 34_000_000_000L),
                ClassicWorldWaterDragonInstances.EventKind.MESSAGE));
        List<ClassicWorldWaterDragonInstances.Event> at36 =
                instances.poll(START + 36_000_000_000L);
        assertEquals(List.of(1660), values(at36,
                ClassicWorldWaterDragonInstances.EventKind.MESSAGE));
        assertEquals(List.of(71026), values(at36,
                ClassicWorldWaterDragonInstances.EventKind.SPAWN_BOSS));
        assertEquals(71026, instances.snapshot(6).activeBossNpcId());
    }

    @Test
    void threeBossDeathsPreserveMessagesBloodTargetsAndSixtySecondExit() {
        ClassicWorldWaterDragonInstances instances = joined(7, "S:A", "S:B");
        instances.talkBossGate("S:A", START);
        instances.poll(START + 36_000_000_000L);

        long phase1Death = START + 40_000_000_000L;
        assertTrue(instances.bossDied(7, 71026, Set.of(), phase1Death));
        assertFalse(instances.bossDied(7, 71026, Set.of(), phase1Death));
        assertEquals(List.of(1661, 1662, 1663, 1664), values(
                instances.poll(phase1Death + 39_999_999_999L),
                ClassicWorldWaterDragonInstances.EventKind.MESSAGE));
        assertEquals(List.of(71027), values(instances.poll(
                phase1Death + 40_000_000_000L),
                ClassicWorldWaterDragonInstances.EventKind.SPAWN_BOSS));

        long phase2Death = phase1Death + 41_000_000_000L;
        assertTrue(instances.bossDied(7, 71027, Set.of(), phase2Death));
        assertEquals(List.of(1665, 1666, 1667), values(instances.poll(
                phase2Death + 29_999_999_999L),
                ClassicWorldWaterDragonInstances.EventKind.MESSAGE));
        assertEquals(List.of(71028), values(instances.poll(
                phase2Death + 30_000_000_000L),
                ClassicWorldWaterDragonInstances.EventKind.SPAWN_BOSS));

        long phase3Death = phase2Death + 31_000_000_000L;
        assertTrue(instances.bossDied(7, 71028,
                Set.of("S:A", "outside"), phase3Death));
        List<ClassicWorldWaterDragonInstances.Event> beforeExit =
                instances.poll(phase3Death + 59_000_000_000L);
        assertEquals(List.of(1668, 1669, 1476, 1477, 1478,
                        1480, 1481, 1482, 1483, 1484),
                values(beforeExit,
                        ClassicWorldWaterDragonInstances.EventKind.MESSAGE));
        List<ClassicWorldWaterDragonInstances.Event> exit =
                instances.poll(phase3Death + 60_000_000_000L);
        assertEquals(1, exit.size());
        assertEquals(ClassicWorldWaterDragonInstances.EventKind.EXIT,
                exit.get(0).kind());
        assertEquals(Set.of("s:a", "s:b"), exit.get(0).recipients());
        assertEquals(Set.of("s:a"), exit.get(0).bloodRecipients());
        assertEquals(ClassicWorldWaterDragonCatalog.COMPLETION_EXIT,
                exit.get(0).destination());
        assertNull(instances.snapshot(7));
    }

    @Test
    void timeoutEndsSharedInstanceAndReleasesIdentityMapping() {
        ClassicWorldWaterDragonInstances instances = joined(8, "S:A", "S:B");
        long deadline = instances.snapshot(8).deadlineNanos();
        assertTrue(instances.expire(deadline - 1).isEmpty());
        List<ClassicWorldWaterDragonInstances.Ended> ended =
                instances.expire(deadline);
        assertEquals(1, ended.size());
        assertEquals("timeout", ended.get(0).reason());
        assertEquals(Set.of("s:a", "s:b"), ended.get(0).joinedIdentities());
        assertNull(instances.snapshot(8));
        assertTrue(instances.admit("S:A", 9, 80, true, false,
                deadline + 1).admitted());
    }

    private static ClassicWorldWaterDragonInstances joined(
            int showId, String... identities) {
        ClassicWorldWaterDragonInstances instances =
                new ClassicWorldWaterDragonInstances();
        for (String identity : identities) {
            assertTrue(instances.admit(identity, showId, 80, true, false,
                    START).admitted());
        }
        return instances;
    }

    private static List<Integer> values(
            List<ClassicWorldWaterDragonInstances.Event> events,
            ClassicWorldWaterDragonInstances.EventKind kind) {
        return events.stream().filter(event -> event.kind() == kind)
                .map(ClassicWorldWaterDragonInstances.Event::value).toList();
    }
}
