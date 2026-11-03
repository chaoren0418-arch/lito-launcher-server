package local.lito381.server;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Server-owned match result, not a client claim or a native ranking mutation. */
final class ModernMatchSettlement {
    static final int MAX_PARTICIPANTS = 100;
    enum Verdict { WIN, LOSS }

    record Participant(long entityId, ModernParticipantId identity, int team, Verdict verdict) {
        Participant {
            Objects.requireNonNull(identity); Objects.requireNonNull(verdict);
            if (entityId < 1 || entityId > MAX_PARTICIPANTS || team < 0 || team > 1) throw invalid();
        }
        Participant(long entityId, TournamentPresence.CharacterKey key, int team, Verdict verdict) {
            this(entityId, ModernParticipantId.human(key), team, verdict);
        }
        TournamentPresence.CharacterKey key() { return identity.humanKey(); }
        @Override public String toString() { return "SettlementParticipant[redacted]"; }
    }

    /**
     * Every frozen match member appears once. The established two-team modes
     * include dead members of the winning team; cooperative mode 4 instead
     * requires its own terminal proof and individual eligibility validation.
     * Entity handles are 1..N in the same source/object ordering as the prepared world.
     * The caller proves actual elimination or objective victory; this value does
     * not invent a combat outcome. Siege mode 3 additionally requires its own
     * role/tower/deadline proof and 15..50 members on each side. Neither special
     * persistence variant enables the older player wire or weakens old modes.
     */
    record Event(UUID resultId, long matchId, long generation, long engineRevision,
                 int mode, int mapId, long finishedAtMillis, List<Participant> participants,ModernTimeAttackSettlement.Proof timeAttack,ModernSiegeSettlement.Proof siege) {
        Event(UUID resultId,long matchId,long generation,long engineRevision,int mode,int mapId,long finishedAtMillis,List<Participant> participants,ModernTimeAttackSettlement.Proof timeAttack) {
            this(resultId,matchId,generation,engineRevision,mode,mapId,finishedAtMillis,participants,timeAttack,null);
        }
        Event(UUID resultId,long matchId,long generation,long engineRevision,int mode,int mapId,long finishedAtMillis,List<Participant> participants) {
            this(resultId,matchId,generation,engineRevision,mode,mapId,finishedAtMillis,participants,null);
        }
        Event {
            Objects.requireNonNull(resultId); Objects.requireNonNull(participants);
            if ((resultId.getMostSignificantBits() | resultId.getLeastSignificantBits()) == 0
                    || matchId < 1 || generation < 1 || engineRevision < 1 || mode < 1 || mode > 5
                    || (mode==4)!=(timeAttack!=null) || (mode==3)!=(siege!=null)
                    || finishedAtMillis < 0 || participants.size() < (timeAttack==null?2:1) || participants.size() > MAX_PARTICIPANTS) throw invalid();
            TournamentRoomPolicy.rules(mode, mapId);
            participants = List.copyOf(participants);
            ModernParticipantId previous = null;
            Verdict[] teams = new Verdict[2];
            for (int index = 0; index < participants.size(); index++) {
                Participant member = participants.get(index);
                if (member.entityId() != index + 1L || previous != null
                        && member.identity().compareTo(previous) <= 0) throw invalid();
                if (timeAttack==null&&teams[member.team()] != null && teams[member.team()] != member.verdict()) throw invalid();
                teams[member.team()] = member.verdict(); previous = member.identity();
            }
            if(timeAttack!=null)timeAttack.validate(mode,mapId,engineRevision,participants);
            else if (teams[0] == null || teams[1] == null || teams[0] == teams[1]
                    || participants.stream().noneMatch(p -> p.identity().isHuman())) throw invalid();
            if(siege!=null)siege.validate(mode,mapId,engineRevision,participants);
        }
        boolean hasBots() { return participants.stream().anyMatch(p -> !p.identity().isHuman()); }
        @Override public String toString() { return "MatchSettlementEvent[redacted]"; }
    }

    /**
     * Same resultId + exact full event is success without re-incrementing totals.
     * Different payload for an existing resultId must fail closed. An uncertain
     * commit retains this event/key for retry; failure is never a rollback claim.
     */
    @FunctionalInterface interface Store {
        void persist(Event event, long absoluteDeadlineNanos) throws Exception;
    }

    enum ReceiptState { PENDING, SAVED, FAILED }
    /** A reserved durable slot belongs to a captured match, not a client-provided outcome. */
    interface Reservation extends AutoCloseable {
        void record(Event event, long absoluteDeadlineNanos) throws Exception;
        ReceiptState state();
        @Override void close();
    }
    /** Production persistence must journal before any FINISHED/last-hit acknowledgement. */
    interface JournaledStore extends Store, AutoCloseable {
        Reservation reserve(long matchId, long generation) throws Exception;
        boolean accepting();
        @Override void close();
        boolean awaitTermination(long timeoutMillis) throws InterruptedException;
        boolean isQuiescent();
    }

    enum Failure { CONFLICT, INVALID_STORAGE, OVERFLOW, DEADLINE, UNAVAILABLE, COMMIT_UNKNOWN }
    static final class SettlementException extends Exception {
        private final Failure failure;
        SettlementException(Failure failure) {
            super("modern match settlement: " + Objects.requireNonNull(failure)); this.failure = failure;
        }
        Failure failure() { return failure; }
    }
    private ModernMatchSettlement() {}
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid modern match result"); }
}
