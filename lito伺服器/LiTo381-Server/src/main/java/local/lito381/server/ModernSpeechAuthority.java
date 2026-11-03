package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** Trusted, connection-owned moderation. All operations run under ModernRooms'
 * monitor. No JDBC, native GameHub import, wire fields or GM-command privilege
 * inference. Account grants were authenticated; the navigation adapter must have
 * qualified character ownership before Rooms admission. This is not a fresh
 * account-ban query or an implementation of the Silence combat skill.
 */
final class ModernSpeechAuthority {
    static final int MAX_SECONDS = 0x7D00;
    static final int MAX_MUTED_CHARACTERS = 64;
    record Source(String name, Set<String> administrators) {
        Source {
            new ModernAccountAuthentication.Source(1, name);
            Objects.requireNonNull(administrators);
            if (administrators.size() > 64) throw invalid();
            administrators = administrators.stream().map(ModernSpeechAuthority::account)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
        @Override public String toString() { return "SpeechSource[redacted]"; }
    }
    private static final class Timers {
        Long named, timed;
        void expire(long now) {
            if (named != null && now - named >= 0) named = null;
            if (timed != null && now - timed >= 0) timed = null;
        }
        boolean empty() { return named == null && timed == null; }
    }
    private final Map<Integer, Source> sources;
    private final LongSupplier clock;
    private final Map<TournamentPresence.Owner, Map<TournamentPresence.CharacterKey, Timers>> silences = new IdentityHashMap<>();

    ModernSpeechAuthority(Map<Integer, Source> sources) { this(sources, System::nanoTime); }
    ModernSpeechAuthority(Map<Integer, Source> sources, LongSupplier clock) {
        this.sources = Map.copyOf(sources); this.clock = Objects.requireNonNull(clock);
        if (sources.size() > 64 || sources.keySet().stream().anyMatch(id -> id <= 0)) throw invalid();
        // An empty map is explicitly unavailable, not an all-player grant.
    }
    static Map<Integer, Source> sources(ModernServerConfig config) {
        return config.sources().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                ModernServerConfig.SourceConfig::id, s -> new Source(s.name(), s.administrationAccounts())));
    }
    boolean configured() { return !sources.isEmpty(); }
    static Set<String> parseAdministrators(String value) {
        if (value == null || value.isEmpty()) return Set.of();
        String[] fields = value.split(",", -1);
        if (fields.length > 64) throw invalid();
        var result = new java.util.HashSet<String>();
        for (String field : fields) if (!result.add(account(field.strip()))) throw invalid();
        return Set.copyOf(result);
    }
    private static String account(String value) {
        if (value == null || value.isBlank() || !value.equals(value.strip()) || value.contains(",")
                || value.chars().anyMatch(Character::isISOControl)
                || !StandardCharsets.UTF_8.newEncoder().canEncode(value)
                || value.getBytes(StandardCharsets.UTF_8).length > 64) throw invalid();
        String canonical=value.toLowerCase(Locale.ROOT);
        if(canonical.getBytes(StandardCharsets.UTF_8).length>64)throw invalid();
        return canonical;
    }
    ModernFilteredChat.SpeechPolicy policy(TournamentPresence.Owner owner,
            ModernAccountAuthentication.Principal principal, TournamentPresence.Selection selected,
            ModernFilteredChat.Token token, boolean roomSilenced) {
        Objects.requireNonNull(owner); Objects.requireNonNull(selected); Objects.requireNonNull(token);
        int id = selected.key().sourceId();
        Source configured = sources.get(id);
        if (principal == null || configured == null || !principal.authorizesSource(id)
                || !configured.name().equals(selected.sourceName())
                || principal.sources().stream().noneMatch(s -> s.id() == id && s.name().equals(configured.name()))
                || token.scope().sourceId() != id || token.scope().objectId() != selected.key().objectId()
                || token.membership() <= 0 || token.roomId() == 0 && roomSilenced) throw stale();
        // Source-specific canonical account, never the display account or another
        // source's grant. Mirrors native Locale.ROOT allowlist matching.
        boolean administrator = configured.administrators().contains(principal.accountForSource(id).toLowerCase(Locale.ROOT));
        var active = prune(owner, clock.getAsLong());
        Timers timers = active == null ? null : active.get(selected.key());
        return new ModernFilteredChat.SpeechPolicy(token, timers != null && timers.named != null,
                timers != null && timers.timed != null, roomSilenced, administrator);
    }
    /** Server-owned command/effect adapter only; Rooms validates exact admission
     * first. Zero clears just this kind. No wire moderation command is exposed. */
    void silence(TournamentPresence.Owner owner, TournamentPresence.CharacterKey key, int seconds, boolean timed) {
        Objects.requireNonNull(owner); Objects.requireNonNull(key);
        if (seconds < 0 || seconds > MAX_SECONDS) throw invalid();
        long now = clock.getAsLong();
        var active = prune(owner, now);
        if (seconds == 0 && (active == null || !active.containsKey(key))) return;
        if (active == null) { active = new HashMap<>(); silences.put(owner, active); }
        Timers timers = active.get(key);
        if (timers == null) {
            if (active.size() == MAX_MUTED_CHARACTERS) throw new IllegalStateException("speech moderation capacity exhausted");
            timers = new Timers(); active.put(key, timers);
        }
        // nanoTime's signed wrap is intentional; maximum duration is far below
        // half its range. Nullable deadline permits a real deadline of zero.
        Long deadline = seconds == 0 ? null : now + TimeUnit.SECONDS.toNanos(seconds);
        if (timed) timers.timed = deadline; else timers.named = deadline;
        if (timers.empty()) active.remove(key);
        if (active.isEmpty()) silences.remove(owner);
    }
    private Map<TournamentPresence.CharacterKey, Timers> prune(TournamentPresence.Owner owner, long now) {
        var active = silences.get(owner);
        if (active != null) {
            active.values().forEach(t -> t.expire(now));
            active.values().removeIf(Timers::empty);
            if (active.isEmpty()) { silences.remove(owner); return null; }
        }
        return active;
    }
    void disconnect(TournamentPresence.Owner owner) { silences.remove(owner); }
    int ownerCount() { return silences.size(); }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid speech authority configuration"); }
    private static SecurityException stale() { return new SecurityException("speech source or admission unavailable"); }
}
