package local.lito381.server;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Recipient-local public conversation authority. Every call is guarded by the
 * owning ModernRooms monitor. No wire registration, socket, callback or SQL.
 * This is NOT the v32 shared-log protocol: its cursor counts only messages
 * actually delivered to one admitted recipient, and SEND has a separate receipt
 * because a sender may disable receiving their own message category.
 */
final class ModernFilteredChat {
    static final int MAX_MESSAGES = 64;
    enum Kind { NORMAL, WORLD, TRADE }
    enum Action { READ, SEND }
    // UNAVAILABLE denies this operation (including a saturated legacy NORMAL
    // sink). Snapshot.available separately describes this recipient's counters;
    // do not copy v32's status-to-availability implication into a new codec.
    enum Status { OK, RATE_LIMITED, COMMAND_REQUIRED, SILENCED, UNAVAILABLE }
    record Token(ModernRooms.Scope scope, long roomId, long membership) {
        Token {
            Objects.requireNonNull(scope);
            if (roomId < 0 || membership < 0) throw invalid();
        }
    }
    record Request(Token token, Action action, Kind kind, long sequence, long after, String text) {
        Request {
            Objects.requireNonNull(token); Objects.requireNonNull(action); Objects.requireNonNull(kind);
            Objects.requireNonNull(text);
            if (after < 0) throw invalid();
            if (action == Action.READ) {
                if (sequence != 0 || kind != Kind.NORMAL || !text.isEmpty()
                        || token.membership() == 0 && after != 0) throw invalid();
            } else {
                if (sequence <= 0 || token.membership() <= 0) throw invalid();
                ModernChat.validateText(text);
            }
        }
        @Override public String toString() { return "FilteredChatRequest[redacted]"; }
    }
    /** Trusted authority input, NOT player command fields. ModernSpeechAuthority
     * supplies the new Rooms seam; future wire adapters must use that seam and
     * must not construct this from client flags. No default grant:
     * missing or stale authority cannot consume a SEND sequence or rate token.
     * Native say bypasses timed/room silence for administrators; named silence
     * affects everyone. Native tell only checks named silence.
     */
    record SpeechPolicy(Token token, boolean namedSilence, boolean timedSilence,
                        boolean roomSilence, boolean administrator) {
        SpeechPolicy { Objects.requireNonNull(token); if (token.membership() <= 0) throw invalid(); }
        boolean blocksPublic() { return namedSilence || !administrator && (timedSilence || roomSilence); }
        boolean blocksPrivate() { return namedSilence; }
    }
    record Message(long id, ModernDirectChat.Peer sender, Kind kind, String text) {
        Message {
            if (id <= 0) throw invalid();
            Objects.requireNonNull(sender); Objects.requireNonNull(kind); ModernChat.validateText(text);
        }
        @Override public String toString() { return "FilteredChatMessage[redacted]"; }
    }
    record Receipt(long sequence, ModernDirectChat.Peer sender, Kind kind, String text) {
        Receipt {
            if (sequence <= 0) throw invalid();
            Objects.requireNonNull(sender); Objects.requireNonNull(kind); ModernChat.validateText(text);
        }
        @Override public String toString() { return "FilteredChatReceipt[redacted]"; }
    }
    record Snapshot(Token token, ModernDirectChat.Peer owner, long sequence, long cursor,
                    boolean available, boolean truncated, List<Message> messages) {
        Snapshot {
            Objects.requireNonNull(token); Objects.requireNonNull(owner); messages = List.copyOf(messages);
            if (token.membership() <= 0 || sequence < 0 || cursor < 0 || messages.size() > MAX_MESSAGES
                    || owner.key().sourceId() != token.scope().sourceId()
                    || owner.key().objectId() != token.scope().objectId()
                    || available && (sequence == Long.MAX_VALUE || cursor == Long.MAX_VALUE)) throw invalid();
            long previous = 0;
            for (var message : messages) {
                if (message.id() <= previous || message.id() > cursor
                        || previous != 0 && message.id() != previous + 1) throw invalid();
                previous = message.id();
            }
            if (!messages.isEmpty() && previous != cursor) throw invalid();
        }
        @Override public String toString() { return "FilteredChatSnapshot[redacted]"; }
    }
    record Result(Status status, Snapshot snapshot, Receipt receipt) {
        Result {
            Objects.requireNonNull(status); Objects.requireNonNull(snapshot);
            if (receipt != null && (status != Status.OK || !receipt.sender().equals(snapshot.owner())
                    || receipt.sequence() != snapshot.sequence())) throw invalid();
        }
        @Override public String toString() { return "FilteredChatResult[redacted]"; }
    }
    private static final class Ledger {
        Token token;
        ModernDirectChat.Peer peer;
        long sequence, cursor, droppedThrough;
        final ArrayDeque<Message> messages = new ArrayDeque<>();
        boolean available() { return sequence < Long.MAX_VALUE && cursor < Long.MAX_VALUE; }
        void clear() { messages.clear(); cursor = 0; droppedThrough = 0; }
    }
    private final Map<TournamentPresence.Owner, Ledger> ledgers = new IdentityHashMap<>();

    void enable(TournamentPresence.Owner owner, ModernDirectChat.Peer peer, Token token) {
        Objects.requireNonNull(owner); Objects.requireNonNull(peer); Objects.requireNonNull(token);
        if (token.membership() <= 0 || peer.key().sourceId() != token.scope().sourceId()
                || peer.key().objectId() != token.scope().objectId()) throw invalid();
        var state = ledgers.computeIfAbsent(owner, ignored -> new Ledger());
        if (state.token != null) {
            if (!state.token.equals(token) || !state.peer.equals(peer)) throw stale();
            return; // Repeated negotiation/bootstrap cannot erase a receipt/history.
        }
        state.token = token; state.peer = peer; state.clear();
    }
    void move(TournamentPresence.Owner owner, Token token) {
        var state = ledgers.get(owner);
        if (state == null || state.token == null) return;
        if (token.membership() <= state.token.membership() || !token.scope().equals(state.token.scope())) throw stale();
        state.token = token; state.clear(); // Public history never crosses room admissions.
    }
    void leave(TournamentPresence.Owner owner) {
        var state = ledgers.get(owner);
        if (state != null) { state.token = null; state.peer = null; state.clear(); }
    }
    void disconnect(TournamentPresence.Owner owner) { ledgers.remove(owner); }
    int ownerCount() { return ledgers.size(); }

    Result execute(TournamentPresence.Owner owner, Request request, SpeechPolicy policy,
                   ModernChat chat, ModernChat.Ledger rate, ModernChat.Log legacy,
                   TournamentPresence.Selection selected, ModernDirectChat preferences) {
        var state = require(owner, request);
        if (request.action() == Action.READ)
            return result(state, state.available() ? Status.OK : Status.UNAVAILABLE, request.after(), null);
        if (policy == null || !policy.token().equals(state.token)) throw stale();
        if (state.sequence == Long.MAX_VALUE || request.sequence() != state.sequence + 1)
            throw new SecurityException("filtered chat sequence mismatch");
        // Validate all trusted publication data before any mutation.
        if (!state.peer.equals(ModernDirectChat.Peer.of(selected))) throw stale();
        state.sequence = request.sequence();
        Status status;
        Receipt receipt = null;
        if (state.cursor == Long.MAX_VALUE || request.kind() == Kind.NORMAL && !chat.canPublish(legacy)) status = Status.UNAVAILABLE;
        else if (ModernChat.reservedCommand(request.text())) status = Status.COMMAND_REQUIRED;
        else if (policy.blocksPublic()) status = Status.SILENCED;
        else if (!chat.takeMessageToken(rate)) status = Status.RATE_LIMITED;
        else {
            // Legacy observers only understand NORMAL. Never strip a category
            // from world/trade traffic and present it as a legacy normal message.
            if (request.kind() == Kind.NORMAL) chat.publish(legacy, selected, request.text());
            publish(state.token, state.peer, request.kind(), request.text(), preferences);
            receipt = new Receipt(request.sequence(), state.peer, request.kind(), request.text());
            status = Status.OK;
        }
        return result(state, status, request.after(), receipt);
    }
    private Ledger require(TournamentPresence.Owner owner, Request request) {
        Objects.requireNonNull(request);
        var state = ledgers.get(owner);
        if (state == null || state.token == null) throw stale();
        var token = request.token();
        if (!token.scope().equals(state.token.scope()) || token.roomId() != state.token.roomId()
                || token.membership() != state.token.membership()
                    && !(request.action() == Action.READ && token.membership() == 0 && request.after() == 0)) throw stale();
        if (request.after() > state.cursor) throw invalid();
        return state;
    }
    // Called after a legacy NORMAL append succeeded, or once for a filtered SEND.
    // No callbacks/I/O occur under Rooms' monitor. Preferences are evaluated at
    // delivery time; READ cannot retroactively erase or resurrect a message.
    void publish(Token origin, ModernDirectChat.Peer sender, Kind kind, String text, ModernDirectChat preferences) {
        Objects.requireNonNull(origin); Objects.requireNonNull(sender); Objects.requireNonNull(kind);
        ModernChat.validateText(text);
        for (var entry : ledgers.entrySet()) {
            var target = entry.getValue();
            if (target.token == null || target.token.scope().channelId() != origin.scope().channelId()
                    || target.token.roomId() != origin.roomId() || target.cursor == Long.MAX_VALUE
                    || !preferences.acceptsPublic(entry.getKey(), sender, kind)) continue;
            var message = new Message(target.cursor + 1, sender, kind, text);
            if (target.messages.size() == MAX_MESSAGES) target.droppedThrough = target.messages.removeFirst().id();
            target.cursor++; target.messages.addLast(message);
        }
    }
    private static Result result(Ledger state, Status status, long after, Receipt receipt) {
        return new Result(status, new Snapshot(state.token, state.peer, state.sequence, state.cursor,
                state.available(), after < state.droppedThrough,
                state.messages.stream().filter(m -> m.id() > after).toList()), receipt);
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid filtered chat value"); }
    private static SecurityException stale() { return new SecurityException("filtered chat admission or authority changed"); }
}
