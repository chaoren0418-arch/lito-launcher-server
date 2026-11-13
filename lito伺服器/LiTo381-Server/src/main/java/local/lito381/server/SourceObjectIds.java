package local.lito381.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Runtime player object IDs that stay unique when several Lineage source
 * databases share one LiTo process (T105 / Issue #306).
 *
 * <p>Each source database allocates {@code characters.objid} independently, so
 * two different characters from two sources can carry the same objid. Battle
 * code keys rosters, targets, winners/losers and packets on
 * {@link ClientSession#playerObjectId()}, so that value must be unique per
 * (source, objid) inside one {@link GameHub}.
 *
 * <p>Rules:
 * <ul>
 * <li>The first claimant of a raw objid keeps it unchanged, so single-source
 *     deployments and every non-colliding player see exactly the old IDs.</li>
 * <li>The same (source, objid) — e.g. a reconnect of the same character —
 *     reuses the existing runtime ID.</li>
 * <li>A different source claiming an objid that is already in use (or a raw
 *     objid that falls inside the alias range) gets an alias from
 *     {@link #ALIAS_FIRST}..{@link #ALIAS_LAST}, a range no other LiTo runtime
 *     allocator uses and that fits a signed 32-bit packet field.</li>
 * <li>A claim lives as long as its owner (weak key) or until the owner selects
 *     a different (source, objid). It is never re-assigned while the owner is
 *     still reachable, so IDs captured by settlement/disconnect code stay
 *     valid after the socket closes.</li>
 * </ul>
 * Database writes never use the runtime ID: settlement keeps
 * (source database, raw objid) — see {@link DurableBattleSettlement}.
 */
final class SourceObjectIds {
    /** 0x7800_0000..0x78FF_FFFF: unused by every other runtime allocator (see docs/design/database.md §10). */
    static final long ALIAS_FIRST = 0x7800_0000L;
    static final long ALIAS_LAST = 0x78FF_FFFFL;

    record Key(String source, long objectId) {
        Key {
            source = normalize(source);
            if (objectId <= 0 || objectId > 0x7FFF_FFFFL)
                throw new IllegalArgumentException("source objid must be a positive 32-bit value: " + objectId);
        }
    }

    record Claim(Key key, long runtimeId) {}

    private static final Map<GameHub, SourceObjectIds> BY_HUB =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final SourceObjectIds DETACHED = new SourceObjectIds();

    private final Map<Object, Claim> claims = new WeakHashMap<>();
    private long nextAlias = ALIAS_FIRST;

    static SourceObjectIds of(GameHub hub) {
        if (hub == null) return DETACHED;
        return BY_HUB.computeIfAbsent(hub, ignored -> new SourceObjectIds());
    }

    static String normalize(String source) {
        return source == null ? "" : source.trim().toLowerCase(Locale.ROOT);
    }

    static boolean alias(long runtimeId) {
        return runtimeId >= ALIAS_FIRST && runtimeId <= ALIAS_LAST;
    }

    /** Returns the runtime ID for {@code owner}'s selected (source, objid). */
    synchronized long claim(Object owner, String source, long objectId) {
        Objects.requireNonNull(owner, "owner");
        Key key = new Key(source, objectId);
        Claim current = claims.get(owner);
        if (current != null && current.key().equals(key)) return current.runtimeId();
        if (current != null) claims.remove(owner);
        Set<Long> used = new HashSet<>();
        for (Claim claim : claims.values()) {
            if (claim.key().equals(key)) {
                claims.put(owner, claim);
                return claim.runtimeId();
            }
            used.add(claim.runtimeId());
        }
        long runtime = !used.contains(objectId) && !alias(objectId) ? objectId : nextFreeAlias(used);
        claims.put(owner, new Claim(key, runtime));
        return runtime;
    }

    synchronized void release(Object owner) {
        claims.remove(owner);
    }

    /** Source identity behind a runtime ID, or {@code null} when nobody holds it. */
    synchronized Key resolve(long runtimeId) {
        for (Claim claim : claims.values())
            if (claim.runtimeId() == runtimeId) return claim.key();
        return null;
    }

    synchronized List<Claim> snapshot() {
        return new ArrayList<>(claims.values());
    }

    private long nextFreeAlias(Set<Long> used) {
        long span = ALIAS_LAST - ALIAS_FIRST + 1;
        for (long attempt = 0; attempt < span; attempt++) {
            long candidate = nextAlias;
            nextAlias = candidate >= ALIAS_LAST ? ALIAS_FIRST : candidate + 1;
            if (!used.contains(candidate)) return candidate;
        }
        throw new IllegalStateException("source object alias range exhausted");
    }
}
