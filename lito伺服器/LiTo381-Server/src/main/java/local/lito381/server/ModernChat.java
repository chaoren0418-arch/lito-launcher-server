package local.lito381.server;

import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Bounded modern conversation state, exclusively guarded by its ModernRooms
 * monitor. No sockets, callbacks, SQL, native broadcasts or GM execution.
 * Routing and sender identity come from Rooms' current qualified membership,
 * never from a claimed display name or destination supplied by a player.
 * Exposed only through the separately negotiated ModernChatProtocol. */
final class ModernChat {
    static final int MAX_MESSAGES = 64, MAX_TEXT_BYTES = 512, MAX_CODE_POINTS = 256;
    static final int BURST = 4;
    static final long REFILL_NANOS = 500_000_000L;
    enum Status { OK, RATE_LIMITED, COMMAND_REQUIRED, UNAVAILABLE }
    record Audience(int channelId, long roomId) {
        Audience { if (channelId < 0 || channelId > 3 || roomId < 0) throw invalid(); }
    }
    record Message(long id, TournamentPresence.CharacterKey sender, String characterName,
                   String sourceName, String text) {
        Message {
            if (id < 1) throw invalid();
            Objects.requireNonNull(sender); Objects.requireNonNull(characterName); Objects.requireNonNull(sourceName);
            validateText(text);
        }
        String displayName() { return characterName + "(" + sourceName + ")"; }
        @Override public String toString() { return "ModernChatMessage[redacted]"; }
    }
    record Snapshot(Audience audience, long membership, long requestSequence, long cursor,
                    boolean available, boolean truncated, List<Message> messages) {
        Snapshot {
            Objects.requireNonNull(audience); messages = List.copyOf(messages);
            if (membership < 0 || requestSequence < 0 || cursor < 0 || messages.size() > MAX_MESSAGES
                    || available && membership == 0) throw invalid();
            long previous = 0;
            for (var message : messages) {
                if (message.id() <= previous || message.id() > cursor) throw invalid();
                previous = message.id();
            }
        }
        @Override public String toString() { return "ModernChatSnapshot[redacted]"; }
    }
    record Result(Status status, Snapshot snapshot) {
        Result { Objects.requireNonNull(status); Objects.requireNonNull(snapshot); }
    }
    /** Immutable token/floor for one admission, including leave/rejoin to the
     * same room. An old request cannot be redirected to a successor audience. */
    record Membership(long generation, long floor, Log log) {}
    static final class Log {
        private long last, droppedThrough;
        private final ArrayDeque<Message> messages = new ArrayDeque<>();
    }
    static final class Ledger {
        private long generation, requestSequence, refillAt;
        private int tokens = BURST;
        private boolean exhausted;
        private Ledger(long now) { refillAt = now; }
    }
    private final Map<TournamentPresence.Owner, Ledger> ledgers = new IdentityHashMap<>();
    private final LongSupplier clock;
    ModernChat() { this(System::nanoTime); }
    ModernChat(LongSupplier clock) { this.clock = Objects.requireNonNull(clock); }
    Ledger register(TournamentPresence.Owner owner) {
        Objects.requireNonNull(owner);
        return ledgers.computeIfAbsent(owner, ignored -> new Ledger(clock.getAsLong()));
    }
    void disconnect(TournamentPresence.Owner owner) { ledgers.remove(owner); }
    int ownerCount() { return ledgers.size(); }
    Membership join(Ledger ledger, Log log) {
        if (ledger.generation == Long.MAX_VALUE) ledger.exhausted = true;
        if (!ledger.exhausted) ledger.generation++;
        return new Membership(ledger.exhausted ? 0 : ledger.generation, log.last, log);
    }
    Snapshot read(Audience audience, Ledger ledger, Membership member, long generation, long after) {
        if (generation != member.generation() && !(generation == 0 && after == 0)) throw stale();
        return snapshot(audience, ledger, member, after);
    }
    Result send(Audience audience, Ledger ledger, Membership member, long generation,
                long sequence, long after, TournamentPresence.Selection sender, String text) {
        validateText(text);
        if (generation < 1 || generation != member.generation()) throw stale();
        if (sequence < 1 || ledger.requestSequence == Long.MAX_VALUE || sequence != ledger.requestSequence + 1)
            throw new SecurityException("chat request sequence mismatch");
        snapshot(audience, ledger, member, after); // Validate cursor before any mutation.
        ledger.requestSequence = sequence;
        Status status;
        if (ledger.exhausted || member.log().last == Long.MAX_VALUE) status = Status.UNAVAILABLE;
        else if (reservedCommand(text)) status = Status.COMMAND_REQUIRED;
        else if (!takeMessageToken(ledger)) status = Status.RATE_LIMITED;
        else {
            publish(member.log(), sender, text);
            status = Status.OK;
        }
        return new Result(status, snapshot(audience, ledger, member, after));
    }
    boolean canPublish(Log log) { return log.last < Long.MAX_VALUE; }
    void publish(Log log, TournamentPresence.Selection sender, String text) {
        if (!canPublish(log)) throw new IllegalStateException("chat log exhausted");
        var message = new Message(log.last + 1, sender.key(), sender.characterName(), sender.sourceName(), text);
        log.last++;
        if (log.messages.size() == MAX_MESSAGES) log.droppedThrough = log.messages.removeFirst().id();
        log.messages.addLast(message);
    }
    // Called only while Rooms holds its monitor. Public and direct messages
    // consume the same owner budget, including across role/channel reentry.
    boolean takeMessageToken(Ledger ledger) {
        long now = clock.getAsLong();
        long elapsed = now - ledger.refillAt; // System.nanoTime's wrap-safe bounded difference.
        if (elapsed >= REFILL_NANOS) {
            long refill = elapsed / REFILL_NANOS;
            ledger.tokens += (int) Math.min(BURST - ledger.tokens, refill);
            ledger.refillAt = now - elapsed % REFILL_NANOS;
        }
        if (ledger.tokens == 0) return false;
        ledger.tokens--; return true;
    }
    private Snapshot snapshot(Audience audience, Ledger ledger, Membership member, long after) {
        var log = member.log();
        if (after < 0 || after > log.last) throw invalid();
        long floor = Math.max(after, member.floor());
        return new Snapshot(audience, member.generation(), ledger.requestSequence, log.last,
                !ledger.exhausted && log.last < Long.MAX_VALUE && ledger.requestSequence < Long.MAX_VALUE,
                floor < log.droppedThrough, log.messages.stream().filter(m -> m.id() > floor).toList());
    }
    static boolean reservedCommand(String text) {
        var value = text.stripLeading();
        return value.regionMatches(true, 0, ".de", 0, 3)
                && (value.length() == 3 || value.length() > 3 && Character.isWhitespace(value.codePointAt(3)));
    }
    static void validateText(String text) {
        if (text == null || text.length() > MAX_CODE_POINTS * 2
                || text.codePointCount(0, text.length()) > MAX_CODE_POINTS
                || text.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c) || c == 0x200c || c == 0x200d)
                || text.codePoints().anyMatch(c -> Character.isISOControl(c)
                    || Character.getType(c) == Character.LINE_SEPARATOR || Character.getType(c) == Character.PARAGRAPH_SEPARATOR
                    || Character.getType(c) == Character.FORMAT && c != 0x200c && c != 0x200d)) throw invalid();
        try {
            if (StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(text)).remaining() > MAX_TEXT_BYTES)
                throw invalid();
        } catch (CharacterCodingException invalid) { throw invalid(); }
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid modern chat value"); }
    private static SecurityException stale() { return new SecurityException("chat membership changed"); }
}
