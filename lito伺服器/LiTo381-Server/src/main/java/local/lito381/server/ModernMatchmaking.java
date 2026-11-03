package local.lito381.server;

import java.math.BigInteger;
import java.util.Objects;

/** Modern automatic grouping, NOT a decoder for the native participant-band
 * selector. Its unrecovered cutoffs are replaced by an explicit player count.
 * Room monitor owns queue/publication; this class has no IO, timers or sessions.
 * The transport adapter must obtain source-qualified central records itself,
 * never accept win/loss counts or a character identity from a matchmaking form. */
final class ModernMatchmaking {
    static final int MAX_WAITERS = 4096;
    enum State { IDLE, WAITING, MATCHED }

    /** mapId=0 means server selection among currently qualified shared worlds.
     * Exact criteria group together. Team modes require equal, nonempty forces.
     * This modern contract does not claim native random-map probabilities. */
    record Criteria(int mode, int mapId, int players) {
        Criteria {
            if (mode < 0 || mode > 5 || mapId < 0) throw invalid();
            var selected = BattleRules.Mode.fromWire(mode);
            if (mapId != 0 && !selected.maps().contains(mapId)) throw invalid();
            var rules = TournamentRoomPolicy.rules(mode, mapId == 0 ? selected.maps().get(0) : mapId);
            if (players < rules.minimumPlayers() || players > TournamentRoomPolicy.combatantCapacity(100, selected)
                    || rules.teamBased() && players % 2 != 0) throw invalid();
        }
        boolean accepts(ModernRooms.Option option) {
            return option.worldAvailable() && option.mode() == mode && (mapId == 0 || mapId == option.mapId())
                    && players >= option.minimumPlayers() && Math.max(2, players) <= option.maximumPlayers();
        }
    }

    /** MATCHED always includes real room membership. A cancellation racing with
     * commit returns MATCHED, never a fictitious cancellation or implicit leave.
     * IDs are admission generations, not room IDs and never reused. */
    record Result(State state, long ticketId, Criteria criteria, ModernRooms.Result rooms) {
        Result {
            Objects.requireNonNull(state); Objects.requireNonNull(rooms);
            if (state == State.IDLE ? ticketId != 0 || criteria != null : ticketId <= 0 || criteria == null)
                throw invalid();
            if (state == State.WAITING && rooms.view().joined() != null
                    || state == State.MATCHED && rooms.view().joined() == null) throw invalid();
        }
        @Override public String toString() { return "ModernMatchmaking.Result[redacted]"; }
    }

    // Retain the native policy's evidence-backed goal (similar win rates), with
    // explicit modern zero-history/FIFO policy. Exact rational arithmetic keeps
    // distinct signed-64-bit records from collapsing into a double tie.
    static int compare(ModernCharacterRecord.Snapshot requester,
                       ModernCharacterRecord.Snapshot left, long leftSequence,
                       ModernCharacterRecord.Snapshot right, long rightSequence) {
        if (leftSequence <= 0 || rightSequence <= 0) throw invalid();
        var a = distance(requester, left); var b = distance(requester, right);
        int order = a.numerator.multiply(b.denominator).compareTo(b.numerator.multiply(a.denominator));
        return order == 0 ? Long.compare(leftSequence, rightSequence) : order;
    }
    private record Distance(BigInteger numerator, BigInteger denominator) {}
    private static Distance distance(ModernCharacterRecord.Snapshot a, ModernCharacterRecord.Snapshot b) {
        Objects.requireNonNull(a); Objects.requireNonNull(b);
        if (a.revision() == 0 || b.revision() == 0)
            return new Distance(a.revision() == b.revision() ? BigInteger.ZERO : BigInteger.ONE, BigInteger.ONE);
        var at = BigInteger.valueOf(a.revision()); var bt = BigInteger.valueOf(b.revision());
        return new Distance(BigInteger.valueOf(a.wins()).multiply(bt)
                .subtract(BigInteger.valueOf(b.wins()).multiply(at)).abs(), at.multiply(bt));
    }
    private ModernMatchmaking() {}
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid modern matchmaking value"); }
}
