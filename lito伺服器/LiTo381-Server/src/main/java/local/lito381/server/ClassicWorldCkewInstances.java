package local.lito381.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Runtime owner of retained quest122 showId allocation and membership. */
final class ClassicWorldCkewInstances {
    @FunctionalInterface
    interface DurableAdmissionCommit {
        void commit() throws IOException;
    }

    @FunctionalInterface
    interface AdmissionPublication {
        void publish() throws IOException;
    }

    enum AdmissionStatus {
        ADMITTED, INVALID_PARTY, PARTY_CHANGED, INSTANCE_ENDED
    }

    record Member(String identity, int characterType) {
        Member {
            identity = normalize(identity);
        }
    }

    record Admission(AdmissionStatus status, int showId, boolean created,
                     boolean alreadyJoined, long deadlineNanos) {
        static Admission rejected(AdmissionStatus status) {
            return new Admission(status, -1, false, false, -1);
        }

        boolean admitted() { return status == AdmissionStatus.ADMITTED; }
    }

    record Ended(int showId, List<String> joinedIdentities, String reason) {
        Ended {
            joinedIdentities = List.copyOf(joinedIdentities);
            reason = reason == null ? "" : reason;
        }
    }

    record Countdown(int showId, Set<String> joinedIdentities,
                     int remainingSeconds) {
        Countdown {
            joinedIdentities = Set.copyOf(joinedIdentities);
        }
    }

    record Snapshot(int showId, Set<String> partyIdentities,
                    Set<String> joinedIdentities, long deadlineNanos,
                    boolean active) {
        Snapshot {
            partyIdentities = Set.copyOf(partyIdentities);
            joinedIdentities = Set.copyOf(joinedIdentities);
        }
    }

    private static final long DURATION_NANOS =
            ClassicWorldCkewCatalog.INSTANCE_SECONDS * 1_000_000_000L;
    private static final int[] COUNTDOWN_THRESHOLDS = {
            1800, 900, 600, 300, 240, 180, 120, 60,
            30, 15, 10, 5, 4, 3, 2, 1
    };

    private static final class Instance {
        private final int showId;
        private final LinkedHashSet<String> partyIdentities;
        private final LinkedHashSet<String> admittingIdentities = new LinkedHashSet<>();
        private final LinkedHashSet<String> joinedIdentities = new LinkedHashSet<>();
        private final long deadlineNanos;
        private int lastRemainingSeconds =
                ClassicWorldCkewCatalog.INSTANCE_SECONDS + 1;
        private boolean active = true;

        private Instance(int showId, Set<String> partyIdentities,
                         long deadlineNanos) {
            this.showId = showId;
            this.partyIdentities = new LinkedHashSet<>(partyIdentities);
            this.deadlineNanos = deadlineNanos;
        }
    }

    private final AtomicInteger nextShowId =
            new AtomicInteger(ClassicWorldCkewCatalog.FIRST_SHOW_ID);
    private final Map<Integer, Instance> byShowId = new LinkedHashMap<>();
    private final Map<String, Integer> byExpectedMember = new LinkedHashMap<>();

    synchronized Admission admit(String callerIdentity, List<Member> party,
                                 long nowNanos) {
        String caller = normalize(callerIdentity);
        LinkedHashSet<String> identities = new LinkedHashSet<>();
        List<Integer> types = new ArrayList<>();
        if (party != null) {
            for (Member member : party) {
                if (member == null || member.identity().isEmpty()) continue;
                identities.add(member.identity());
                types.add(member.characterType());
            }
        }
        if (caller.isEmpty() || !identities.contains(caller)
                || identities.size() != 4 || types.size() != 4
                || !ClassicWorldCkewCatalog.exactNativeParty(types)) {
            return Admission.rejected(AdmissionStatus.INVALID_PARTY);
        }

        LinkedHashSet<Integer> existingIds = new LinkedHashSet<>();
        for (String identity : identities) {
            Integer existing = byExpectedMember.get(identity);
            if (existing != null) existingIds.add(existing);
        }
        if (existingIds.size() > 1) {
            return Admission.rejected(AdmissionStatus.PARTY_CHANGED);
        }

        boolean created = existingIds.isEmpty();
        Instance instance;
        if (created) {
            int showId = nextShowId.getAndIncrement();
            instance = new Instance(showId, identities,
                    saturatedDeadline(nowNanos));
            byShowId.put(showId, instance);
            for (String identity : identities) {
                byExpectedMember.put(identity, showId);
            }
        } else {
            int showId = existingIds.iterator().next();
            instance = byShowId.get(showId);
            if (instance == null || !instance.active) {
                return Admission.rejected(AdmissionStatus.INSTANCE_ENDED);
            }
            if (!instance.partyIdentities.equals(identities)) {
                return Admission.rejected(AdmissionStatus.PARTY_CHANGED);
            }
        }
        boolean alreadyJoined = instance.joinedIdentities.contains(caller)
                || !instance.admittingIdentities.add(caller);
        return new Admission(AdmissionStatus.ADMITTED, instance.showId,
                created, alreadyJoined, instance.deadlineNanos);
    }

    synchronized boolean commitAdmission(String callerIdentity,
                                         Admission admission) {
        try {
            return commitAdmission(callerIdentity, admission, () -> {});
        } catch (IOException impossible) {
            throw new AssertionError("no-op CKEW admission commit failed", impossible);
        }
    }

    /**
     * Serializes the durable quest mutation with the reservation-to-membership
     * transition.  While the callback runs, timeout/out-stop cannot invalidate
     * the instance between a successful quest save and membership commit.
     */
    synchronized boolean commitAdmission(
            String callerIdentity, Admission admission,
            DurableAdmissionCommit durableCommit) throws IOException {
        if (durableCommit == null) {
            throw new IllegalArgumentException("durable CKEW admission commit is required");
        }
        if (admission == null || !admission.admitted()) return false;
        String caller = normalize(callerIdentity);
        Instance instance = byShowId.get(admission.showId());
        if (instance == null || !instance.active
                || !instance.partyIdentities.contains(caller)) return false;
        if (instance.joinedIdentities.contains(caller)) {
            durableCommit.commit();
            return true;
        }
        if (!instance.admittingIdentities.contains(caller)) return false;
        durableCommit.commit();
        if (!instance.admittingIdentities.remove(caller)) {
            throw new IllegalStateException(
                    "CKEW admission reservation changed during durable commit");
        }
        return instance.joinedIdentities.add(caller);
    }

    /**
     * Publishes LiTo's entry effect and S_PORTAL only while the committed show
     * is still live.  Timeout/out-stop uses this same monitor, so it can run
     * either before the callback (which is then suppressed) or after the whole
     * wire publication, but never between its live check and S_PORTAL.
     */
    synchronized boolean publishAdmission(
            String callerIdentity, Admission admission,
            AdmissionPublication publication) throws IOException {
        if (publication == null) {
            throw new IllegalArgumentException(
                    "CKEW admission publication is required");
        }
        if (admission == null || !admission.admitted()) return false;
        String caller = normalize(callerIdentity);
        Instance instance = byShowId.get(admission.showId());
        if (instance == null || !instance.active
                || !instance.partyIdentities.contains(caller)
                || !instance.joinedIdentities.contains(caller)) return false;
        publication.publish();
        return true;
    }

    synchronized void rollbackAdmission(String callerIdentity,
                                         Admission admission) {
        if (admission == null || !admission.admitted()
                || admission.alreadyJoined()) return;
        String caller = normalize(callerIdentity);
        Instance instance = byShowId.get(admission.showId());
        if (instance == null || !instance.active) return;
        instance.admittingIdentities.remove(caller);
        instance.joinedIdentities.remove(caller);
        if (admission.created() && instance.joinedIdentities.isEmpty()
                && instance.admittingIdentities.isEmpty()) {
            remove(instance);
        }
    }

    synchronized Ended leave(String identity, String reason) {
        String key = normalize(identity);
        Integer showId = byExpectedMember.get(key);
        Instance instance = showId == null ? null : byShowId.get(showId);
        if (instance == null || !instance.active
                || !instance.joinedIdentities.contains(key)) return null;
        return end(instance, reason == null ? "out-stop" : reason);
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

    synchronized List<Countdown> countdowns(long nowNanos) {
        List<Countdown> result = new ArrayList<>();
        for (Instance instance : byShowId.values()) {
            if (!instance.active) continue;
            int remaining = remainingSeconds(instance.deadlineNanos, nowNanos);
            for (int threshold : COUNTDOWN_THRESHOLDS) {
                if (instance.lastRemainingSeconds > threshold
                        && remaining <= threshold) {
                    result.add(new Countdown(instance.showId,
                            instance.joinedIdentities, threshold));
                }
            }
            instance.lastRemainingSeconds = remaining;
        }
        return List.copyOf(result);
    }

    synchronized Ended complete(int showId) {
        return end(showId, "complete");
    }

    synchronized Ended end(int showId, String reason) {
        Instance instance = byShowId.get(showId);
        return instance == null || !instance.active
                ? null : end(instance, reason == null ? "" : reason);
    }

    synchronized int showId(String identity) {
        return byExpectedMember.getOrDefault(normalize(identity), -1);
    }

    synchronized Snapshot snapshot(int showId) {
        Instance instance = byShowId.get(showId);
        if (instance == null) return null;
        return new Snapshot(instance.showId, instance.partyIdentities,
                instance.joinedIdentities, instance.deadlineNanos,
                instance.active);
    }

    synchronized List<Snapshot> activeSnapshots() {
        return byShowId.values().stream()
                .filter(instance -> instance.active)
                .map(instance -> new Snapshot(instance.showId,
                        instance.partyIdentities, instance.joinedIdentities,
                        instance.deadlineNanos, true))
                .toList();
    }

    private Ended end(Instance instance, String reason) {
        List<String> joined = List.copyOf(instance.joinedIdentities);
        instance.active = false;
        remove(instance);
        return new Ended(instance.showId, joined, reason);
    }

    private void remove(Instance instance) {
        byShowId.remove(instance.showId, instance);
        for (String identity : instance.partyIdentities) {
            byExpectedMember.remove(identity, instance.showId);
        }
    }

    private static long saturatedDeadline(long nowNanos) {
        try {
            return Math.addExact(nowNanos, DURATION_NANOS);
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }

    private static int remainingSeconds(long deadlineNanos, long nowNanos) {
        if (nowNanos >= deadlineNanos) return 0;
        long remaining = deadlineNanos - nowNanos;
        long seconds = remaining / 1_000_000_000L;
        if (remaining % 1_000_000_000L != 0) seconds++;
        return (int) Math.min(seconds, Integer.MAX_VALUE);
    }

    static String normalize(String identity) {
        return identity == null ? "" : identity.trim().toLowerCase(Locale.ROOT);
    }
}
