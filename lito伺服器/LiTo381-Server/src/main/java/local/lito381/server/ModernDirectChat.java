package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Private conversation authority, guarded exclusively by ModernRooms' monitor.
 * No socket, SQL, callback, native session or name-only cross-source routing.
 * Enrollment is explicit: legacy/non-negotiated peers are never recipients. */
final class ModernDirectChat {
    static final int MAX_MESSAGES = 64, MAX_BLOCKED = 16;
    enum Action { READ, SEND, PREFERENCES, BLOCK, UNBLOCK, CLEAR_BLOCKS }
    enum Status { OK, RATE_LIMITED, UNREACHABLE, BLOCK_LIST_FULL, UNAVAILABLE, SILENCED }

    record Address(int sourceId, String name) {
        Address {
            if (sourceId <= 0 || name == null) throw invalid();
            name = name.strip().toLowerCase(Locale.ROOT);
            ModernChat.validateText(name);
            if (name.getBytes(StandardCharsets.UTF_8).length > 256) throw invalid();
        }
        boolean matches(Peer peer) {
            return sourceId == peer.key().sourceId()
                    && name.equals(peer.characterName().strip().toLowerCase(Locale.ROOT));
        }
        @Override public String toString() { return "DirectAddress[redacted]"; }
    }
    record Peer(TournamentPresence.CharacterKey key, String characterName, String sourceName) {
        Peer {
            Objects.requireNonNull(key);
            // Same trusted-row bounds as room membership; enrollment must not
            // reinterpret source/object identity as a display-name credential.
            ModernRooms.text(characterName, 256, false);
            ModernRooms.text(sourceName, 256, false);
        }
        static Peer of(TournamentPresence.Selection s) { return new Peer(s.key(), s.characterName(), s.sourceName()); }
        @Override public String toString() { return "DirectPeer[redacted]"; }
    }
    record Preferences(boolean whispers, boolean world, boolean trade) {
        static final Preferences DEFAULT = new Preferences(true, true, true);
    }
    record Request(Action action, long sequence, long after, Address target, Preferences preferences, String text) {
        Request {
            Objects.requireNonNull(action); Objects.requireNonNull(text);
            if (after < 0 || (action == Action.READ ? sequence != 0 : sequence <= 0)
                    || (action == Action.SEND || action == Action.BLOCK || action == Action.UNBLOCK) != (target != null)
                    || (action == Action.PREFERENCES) != (preferences != null)) throw invalid();
            if (action == Action.SEND) ModernChat.validateText(text);
            else if (!text.isEmpty()) throw invalid();
        }
        @Override public String toString() { return "DirectRequest[redacted]"; }
    }
    record Message(long id, long senderSequence, Peer sender, Peer recipient, String text) {
        Message {
            if (id <= 0 || senderSequence <= 0) throw invalid();
            Objects.requireNonNull(sender); Objects.requireNonNull(recipient); ModernChat.validateText(text);
        }
        @Override public String toString() { return "DirectMessage[redacted]"; }
    }
    record Snapshot(Peer owner, long requestSequence, long cursor, boolean available, boolean truncated,
                    Preferences preferences, List<Address> blocked, List<Message> messages) {
        Snapshot {
            Objects.requireNonNull(owner); Objects.requireNonNull(preferences);
            blocked = List.copyOf(blocked); messages = List.copyOf(messages);
            if (requestSequence < 0 || cursor < 0 || blocked.size() > MAX_BLOCKED || messages.size() > MAX_MESSAGES
                    || new LinkedHashSet<>(blocked).size() != blocked.size()
                    || available && (requestSequence == Long.MAX_VALUE || cursor == Long.MAX_VALUE)) throw invalid();
            long previous = 0;
            for (var m : messages) {
                if (m.id() <= previous || m.id() > cursor
                        || !m.sender().key().equals(owner.key()) && !m.recipient().key().equals(owner.key())
                        || m.sender().key().equals(owner.key()) && (!m.sender().equals(owner) || m.senderSequence() > requestSequence)
                        || m.recipient().key().equals(owner.key()) && !m.recipient().equals(owner)) throw invalid();
                previous = m.id();
            }
            if (!messages.isEmpty() && previous != cursor) throw invalid();
        }
        @Override public String toString() { return "DirectSnapshot[redacted]"; }
    }
    record Result(Status status, Snapshot snapshot) {
        Result { Objects.requireNonNull(status); Objects.requireNonNull(snapshot); }
    }
    private static final class Ledger {
        Peer peer;
        long epoch, sequence, cursor, droppedThrough;
        boolean active;
        Preferences preferences = Preferences.DEFAULT;
        final LinkedHashSet<Address> blocked = new LinkedHashSet<>();
        final ArrayDeque<Message> messages = new ArrayDeque<>();
        boolean available() { return sequence < Long.MAX_VALUE && cursor < Long.MAX_VALUE; }
        void clearHistory() { messages.clear(); cursor = 0; droppedThrough = 0; }
    }
    private final Map<TournamentPresence.Owner, Ledger> ledgers = new IdentityHashMap<>();

    void enable(TournamentPresence.Owner owner, TournamentPresence.Selection selected, long epoch) {
        Objects.requireNonNull(owner); if (epoch == 0) throw invalid();
        var peer = Peer.of(selected);
        var state = ledgers.computeIfAbsent(owner, ignored -> new Ledger());
        if (state.active) {
            if (!state.peer.equals(peer) || state.epoch != epoch) throw new SecurityException("direct selection still active");
            return; // Repeated bootstrap cannot erase history, preferences or the command sequence.
        }
        if (state.peer == null || !state.peer.key().equals(peer.key()) || state.epoch != epoch) {
            state.preferences = Preferences.DEFAULT;
            state.blocked.clear();
        }
        state.peer = peer; state.epoch = epoch; state.active = true; state.clearHistory();
    }
    void leave(TournamentPresence.Owner owner) {
        var state = ledgers.get(owner);
        if (state != null) { state.active = false; state.clearHistory(); }
    }
    void disconnect(TournamentPresence.Owner owner) { ledgers.remove(owner); }
    int ownerCount() { return ledgers.size(); }

    /** Same source-qualified canonical reject list for a future filtered public
     * feed. This is an authority decision, not a client-side display-name filter.
     * Legacy/non-enrolled sessions never become filtered recipients implicitly.
     */
    boolean acceptsPublic(TournamentPresence.Owner owner, Peer sender, ModernFilteredChat.Kind kind) {
        Objects.requireNonNull(sender); Objects.requireNonNull(kind);
        var state = ledgers.get(owner);
        return state != null && state.active
                && state.blocked.stream().noneMatch(a -> a.matches(sender))
                && switch (kind) {
                    case NORMAL -> true;
                    case WORLD -> state.preferences.world();
                    case TRADE -> state.preferences.trade();
                };
    }

    Result execute(TournamentPresence.Owner owner, Request request, ModernChat chat, ModernChat.Ledger rate) {
        return execute(owner, request, chat, rate, false);
    }
    /** Moderation is supplied only by Rooms' qualified authority under its monitor. */
    Result execute(TournamentPresence.Owner owner, Request request, ModernChat chat, ModernChat.Ledger rate, boolean namedSilence) {
        Objects.requireNonNull(request);
        var state = ledgers.get(owner);
        if (state == null || !state.active) throw new SecurityException("direct chat not enrolled");
        if (request.after() > state.cursor) throw invalid();
        if (request.action() == Action.READ)
            return result(state, state.available() ? Status.OK : Status.UNAVAILABLE, request.after());
        if (state.sequence == Long.MAX_VALUE || request.sequence() != state.sequence + 1)
            throw new SecurityException("direct request sequence mismatch");
        state.sequence = request.sequence(); // Semantic denial consumes only this session's direct command sequence.
        if (state.cursor == Long.MAX_VALUE) return result(state, Status.UNAVAILABLE, request.after());
        if (request.action() == Action.SEND && namedSilence)
            return result(state, Status.SILENCED, request.after());
        Status status = Status.OK;
        switch (request.action()) {
            case SEND -> {
                if (!chat.takeMessageToken(rate)) status = Status.RATE_LIMITED;
                else {
                    Ledger target = null;
                    for (var candidate : ledgers.values()) {
                        if (candidate.active && request.target().matches(candidate.peer)) {
                            if (target != null) { target = null; break; } // Ambiguous trusted rows never pick an arbitrary recipient.
                            target = candidate;
                        }
                    }
                    if (target == null || !target.preferences.whispers()
                            || target.blocked.stream().anyMatch(a -> a.matches(state.peer))
                            || target.cursor == Long.MAX_VALUE || target != state && !target.available()) status = Status.UNREACHABLE;
                    else {
                        // Both capacities were checked before either append. A self-whisper is one record, not two.
                        var sender = state.peer; var recipient = target.peer;
                        append(state, request.sequence(), sender, recipient, request.text());
                        if (target != state) append(target, request.sequence(), sender, recipient, request.text());
                    }
                }
            }
            case PREFERENCES -> state.preferences = request.preferences();
            case BLOCK -> {
                if (state.blocked.size() == MAX_BLOCKED && !state.blocked.contains(request.target())) status = Status.BLOCK_LIST_FULL;
                else state.blocked.add(request.target());
            }
            case UNBLOCK -> state.blocked.remove(request.target());
            case CLEAR_BLOCKS -> state.blocked.clear();
            case READ -> throw new AssertionError("read handled before mutation");
        }
        return result(state, status, request.after());
    }
    private static void append(Ledger state, long sequence, Peer sender, Peer recipient, String text) {
        var message = new Message(state.cursor + 1, sequence, sender, recipient, text);
        if (state.messages.size() == MAX_MESSAGES) state.droppedThrough = state.messages.removeFirst().id();
        state.cursor++; state.messages.addLast(message);
    }
    private static Result result(Ledger state, Status status, long after) {
        return new Result(status, new Snapshot(state.peer, state.sequence, state.cursor, state.available(),
                after < state.droppedThrough, state.preferences, List.copyOf(state.blocked),
                state.messages.stream().filter(m -> m.id() > after).toList()));
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid direct chat value"); }
}
