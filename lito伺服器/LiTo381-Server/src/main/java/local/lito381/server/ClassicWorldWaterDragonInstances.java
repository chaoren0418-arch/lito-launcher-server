package local.lito381.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/** Runtime owner of quest117 show membership, doors and scripted boss timing. */
final class ClassicWorldWaterDragonInstances {
    enum AdmissionStatus {
        ADMITTED, INVALID_CREDENTIAL, LEVEL_TOO_LOW, COOLDOWN_ACTIVE,
        CAPACITY_REACHED, REENTRY_DENIED, INVALID_SHOW_ID, INSTANCE_ENDED
    }

    enum EventKind { MESSAGE, SPAWN_BOSS, EXIT }

    record Admission(AdmissionStatus status, int showId, boolean created,
                     long deadlineNanos,
                     List<ClassicWorldWaterDragonCatalog.SpawnRow> initialSpawns,
                     List<ClassicWorldWaterDragonCatalog.Door> initialDoors) {
        Admission {
            initialSpawns = List.copyOf(initialSpawns);
            initialDoors = List.copyOf(initialDoors);
        }

        static Admission rejected(AdmissionStatus status, int showId) {
            return new Admission(status, showId, false, -1, List.of(), List.of());
        }

        boolean admitted() { return status == AdmissionStatus.ADMITTED; }
    }

    record GateTalk(boolean accepted, boolean scheduled,
                    ClassicWorldWaterDragonCatalog.Location destination) {}

    record Event(int showId, EventKind kind, long dueNanos, int value,
                 Set<String> recipients, Set<String> bloodRecipients,
                 ClassicWorldWaterDragonCatalog.Location destination) {
        Event {
            recipients = Set.copyOf(recipients);
            bloodRecipients = Set.copyOf(bloodRecipients);
        }
    }

    record Ended(int showId, Set<String> joinedIdentities, String reason) {
        Ended {
            joinedIdentities = Set.copyOf(joinedIdentities);
            reason = reason == null ? "" : reason;
        }
    }

    record Snapshot(int showId, Set<String> joinedIdentities,
                    Set<String> seenIdentities, Map<Integer, Boolean> openDoors,
                    int activeBossNpcId, boolean bossSequenceStarted,
                    long deadlineNanos, boolean active) {
        Snapshot {
            joinedIdentities = Set.copyOf(joinedIdentities);
            seenIdentities = Set.copyOf(seenIdentities);
            openDoors = Map.copyOf(openDoors);
        }
    }

    private record Pending(long sequence, long dueNanos, EventKind kind,
                           int value, Set<String> bloodRecipients) {}

    private static final class Instance {
        private final int showId;
        private final long deadlineNanos;
        private final LinkedHashSet<String> joined = new LinkedHashSet<>();
        private final LinkedHashSet<String> seen = new LinkedHashSet<>();
        private final LinkedHashMap<Integer, Boolean> openDoors = new LinkedHashMap<>();
        private final PriorityQueue<Pending> pending = new PriorityQueue<>(
                Comparator.comparingLong(Pending::dueNanos)
                        .thenComparingLong(Pending::sequence));
        private long nextSequence;
        private int activeBossNpcId = -1;
        private boolean bossSequenceStarted;
        private boolean active = true;

        private Instance(int showId, long deadlineNanos) {
            this.showId = showId;
            this.deadlineNanos = deadlineNanos;
            for (ClassicWorldWaterDragonCatalog.Door door
                    : ClassicWorldWaterDragonCatalog.doors()) {
                openDoors.put(door.doorId(), false);
            }
        }

        private void schedule(long dueNanos, EventKind kind, int value,
                              Set<String> bloodRecipients) {
            pending.add(new Pending(nextSequence++, dueNanos, kind, value,
                    Set.copyOf(bloodRecipients)));
        }
    }

    private static final long INSTANCE_NANOS =
            ClassicWorldWaterDragonCatalog.INSTANCE_SECONDS * 1_000_000_000L;
    private final Map<Integer, Instance> byShowId = new LinkedHashMap<>();
    private final Map<String, Integer> byJoinedIdentity = new LinkedHashMap<>();

    synchronized Admission admit(String identity, int showId, int level,
                                 boolean hasCredential, boolean cooldownActive,
                                 long nowNanos) {
        String member = normalize(identity);
        if (showId <= 0 || member.isEmpty()) {
            return Admission.rejected(AdmissionStatus.INVALID_SHOW_ID, showId);
        }
        if (!hasCredential) {
            return Admission.rejected(AdmissionStatus.INVALID_CREDENTIAL, showId);
        }
        if (level < ClassicWorldWaterDragonCatalog.MINIMUM_ENTRY_LEVEL) {
            return Admission.rejected(AdmissionStatus.LEVEL_TOO_LOW, showId);
        }
        if (cooldownActive) {
            return Admission.rejected(AdmissionStatus.COOLDOWN_ACTIVE, showId);
        }
        Integer joinedElsewhere = byJoinedIdentity.get(member);
        if (joinedElsewhere != null && joinedElsewhere != showId) {
            return Admission.rejected(AdmissionStatus.INSTANCE_ENDED, showId);
        }

        Instance instance = byShowId.get(showId);
        boolean created = instance == null;
        if (created) {
            instance = new Instance(showId, saturatedAdd(nowNanos, INSTANCE_NANOS));
            byShowId.put(showId, instance);
        } else if (!instance.active || nowNanos >= instance.deadlineNanos) {
            if (instance.active) end(instance, "timeout");
            return Admission.rejected(AdmissionStatus.INSTANCE_ENDED, showId);
        }
        if (instance.seen.contains(member)) {
            return Admission.rejected(AdmissionStatus.REENTRY_DENIED, showId);
        }
        if (instance.joined.size() >= ClassicWorldWaterDragonCatalog.MAXIMUM_USERS) {
            if (created) byShowId.remove(showId);
            return Admission.rejected(AdmissionStatus.CAPACITY_REACHED, showId);
        }

        instance.seen.add(member);
        instance.joined.add(member);
        byJoinedIdentity.put(member, showId);
        return new Admission(AdmissionStatus.ADMITTED, showId, created,
                instance.deadlineNanos,
                created ? ClassicWorldWaterDragonCatalog.instanceSpawnRows() : List.of(),
                created ? ClassicWorldWaterDragonCatalog.doors() : List.of());
    }

    synchronized void rollbackAdmission(String identity, Admission admission) {
        if (admission == null || !admission.admitted()) return;
        String member = normalize(identity);
        Instance instance = byShowId.get(admission.showId());
        if (instance == null || !instance.active) return;
        instance.joined.remove(member);
        instance.seen.remove(member);
        byJoinedIdentity.remove(member, admission.showId());
        if (admission.created() && instance.joined.isEmpty()) {
            instance.active = false;
            byShowId.remove(instance.showId, instance);
        }
    }

    synchronized Ended leave(String identity, String reason) {
        String member = normalize(identity);
        Integer showId = byJoinedIdentity.remove(member);
        Instance instance = showId == null ? null : byShowId.get(showId);
        if (instance == null || !instance.active || !instance.joined.remove(member)) {
            return null;
        }
        // Native quest survives an individual map leave until its shared timer ends.
        return new Ended(instance.showId, Set.of(member),
                reason == null ? "map-leave" : reason);
    }

    synchronized ClassicWorldWaterDragonCatalog.Door guardianDied(
            int showId, int guardianNpcId) {
        Instance instance = active(showId);
        ClassicWorldWaterDragonCatalog.Door door =
                ClassicWorldWaterDragonCatalog.doorForGuardian(guardianNpcId);
        if (instance == null || door == null
                || Boolean.TRUE.equals(instance.openDoors.get(door.doorId()))) {
            return null;
        }
        instance.openDoors.put(door.doorId(), true);
        return door;
    }

    synchronized GateTalk talkBossGate(String identity, long nowNanos) {
        String member = normalize(identity);
        Integer showId = byJoinedIdentity.get(member);
        Instance instance = showId == null ? null : active(showId);
        if (instance == null || nowNanos >= instance.deadlineNanos) {
            return new GateTalk(false, false, null);
        }
        boolean scheduled = !instance.bossSequenceStarted;
        if (scheduled) {
            instance.bossSequenceStarted = true;
            for (ClassicWorldWaterDragonCatalog.TimedMessage message
                    : ClassicWorldWaterDragonCatalog.startMessages()) {
                instance.schedule(afterSeconds(nowNanos, message.offsetSeconds()),
                        EventKind.MESSAGE, message.messageId(), Set.of());
            }
            instance.schedule(afterSeconds(nowNanos, 36),
                    EventKind.SPAWN_BOSS, 71026, Set.of());
        }
        return new GateTalk(true, scheduled,
                ClassicWorldWaterDragonCatalog.BOSS_GATE_DESTINATION);
    }

    synchronized boolean bossDied(int showId, int npcId,
                                  Set<String> hateListIdentities,
                                  long nowNanos) {
        Instance instance = active(showId);
        if (instance == null || instance.activeBossNpcId != npcId
                || ClassicWorldWaterDragonCatalog.bossPhase(npcId) == null) {
            return false;
        }
        instance.activeBossNpcId = -1;
        for (ClassicWorldWaterDragonCatalog.TimedMessage message
                : ClassicWorldWaterDragonCatalog.deathMessages(npcId)) {
            instance.schedule(afterSeconds(nowNanos, message.offsetSeconds()),
                    EventKind.MESSAGE, message.messageId(), Set.of());
        }
        int nextNpcId = ClassicWorldWaterDragonCatalog.bossPhase(npcId).nextNpcId();
        if (nextNpcId > 0) {
            instance.schedule(afterSeconds(nowNanos,
                            ClassicWorldWaterDragonCatalog.deathResolutionSeconds(npcId)),
                    EventKind.SPAWN_BOSS, nextNpcId, Set.of());
        } else {
            LinkedHashSet<String> blood = new LinkedHashSet<>();
            if (hateListIdentities != null) {
                for (String identity : hateListIdentities) {
                    String normalized = normalize(identity);
                    if (instance.seen.contains(normalized)) blood.add(normalized);
                }
            }
            for (ClassicWorldWaterDragonCatalog.TimedMessage message
                    : ClassicWorldWaterDragonCatalog.completionMessages()) {
                instance.schedule(afterSeconds(nowNanos, message.offsetSeconds()),
                        EventKind.MESSAGE, message.messageId(), Set.of());
            }
            instance.schedule(afterSeconds(nowNanos, 60), EventKind.EXIT, 0, blood);
        }
        return true;
    }

    synchronized List<Event> poll(long nowNanos) {
        List<Event> result = new ArrayList<>();
        for (Instance instance : new ArrayList<>(byShowId.values())) {
            if (!instance.active) continue;
            while (!instance.pending.isEmpty()
                    && instance.pending.peek().dueNanos() <= nowNanos) {
                Pending pending = instance.pending.remove();
                if (pending.kind() == EventKind.SPAWN_BOSS) {
                    instance.activeBossNpcId = pending.value();
                }
                Set<String> recipients = Set.copyOf(instance.joined);
                ClassicWorldWaterDragonCatalog.Location destination =
                        pending.kind() == EventKind.EXIT
                                ? ClassicWorldWaterDragonCatalog.COMPLETION_EXIT : null;
                result.add(new Event(instance.showId, pending.kind(),
                        pending.dueNanos(), pending.value(), recipients,
                        pending.bloodRecipients(), destination));
                if (pending.kind() == EventKind.EXIT) {
                    end(instance, "complete");
                    break;
                }
            }
            if (instance.active && nowNanos >= instance.deadlineNanos) {
                end(instance, "timeout");
            }
        }
        return List.copyOf(result);
    }

    synchronized List<Ended> expire(long nowNanos) {
        List<Ended> result = new ArrayList<>();
        for (Instance instance : new ArrayList<>(byShowId.values())) {
            if (instance.active && nowNanos >= instance.deadlineNanos) {
                result.add(end(instance, "timeout"));
            }
        }
        return List.copyOf(result);
    }

    synchronized Ended end(int showId, String reason) {
        Instance instance = active(showId);
        return instance == null ? null : end(instance, reason);
    }

    synchronized Snapshot snapshot(int showId) {
        Instance instance = byShowId.get(showId);
        return instance == null ? null : snapshot(instance);
    }

    synchronized List<Snapshot> activeSnapshots() {
        return byShowId.values().stream().filter(instance -> instance.active)
                .map(ClassicWorldWaterDragonInstances::snapshot).toList();
    }

    private static Snapshot snapshot(Instance instance) {
        return new Snapshot(instance.showId, instance.joined, instance.seen,
                instance.openDoors, instance.activeBossNpcId,
                instance.bossSequenceStarted, instance.deadlineNanos,
                instance.active);
    }

    private Instance active(int showId) {
        Instance instance = byShowId.get(showId);
        return instance == null || !instance.active ? null : instance;
    }

    private Ended end(Instance instance, String reason) {
        instance.active = false;
        byShowId.remove(instance.showId, instance);
        for (String member : instance.joined) {
            byJoinedIdentity.remove(member, instance.showId);
        }
        return new Ended(instance.showId, instance.joined, reason);
    }

    private static long afterSeconds(long nowNanos, int seconds) {
        return saturatedAdd(nowNanos, seconds * 1_000_000_000L);
    }

    private static long saturatedAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }

    static String normalize(String identity) {
        return identity == null ? "" : identity.trim().toLowerCase(Locale.ROOT);
    }
}
