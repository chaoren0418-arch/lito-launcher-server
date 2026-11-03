package local.lito381.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Transport-neutral authority for character reservations and the four Tournament
 * channels. An Owner is an unforgeable in-process session capability, never a
 * wire ID. Adapters must authenticate and qualify a character before select().
 * No database, packet encoder, fallback character or native socket lives here.
 */
final class TournamentPresence {
    static final int CHANNEL_CAPACITY = 100;
    record Tier(int id, String name, long minimumLevel, long maximumLevel) {
        boolean contains(long level) { return level >= minimumLevel && level <= maximumLevel; }
    }
    static final List<Tier> TIERS = List.of(
            new Tier(0, "DeathKnight", 52, Long.MAX_VALUE),
            new Tier(1, "Baphomet", 48, 51),
            new Tier(2, "Succubus", 40, 47),
            new Tier(3, "Ogre", 15, 39));
    static Tier recommendedChannel(long level) {
        return TIERS.stream().filter(tier -> tier.contains(level)).findFirst().orElse(null);
    }
    record CharacterKey(int sourceId, long objectId) {
        CharacterKey {
            if (sourceId <= 0 || objectId <= 0) throw new IllegalArgumentException("invalid character key");
        }
    }
    record Selection(CharacterKey key, String sourceName, String characterName,
                     long level, int characterClass, int sex) {
        Selection {
            Objects.requireNonNull(key);
            if (sourceName == null || sourceName.isBlank() || characterName == null || characterName.isBlank()
                    || level < 1 || characterClass < 0 || characterClass > 0xFFFF || sex < 0 || sex > 1) {
                throw new IllegalArgumentException("invalid qualified character");
            }
            // This alias exists ONLY for native name-addressed deletion/social
            // compatibility. It never substitutes for the source/object key.
            GameStateStore.sourceCharacter(sourceName, characterName);
        }
        String legacyAlias() { return GameStateStore.sourceCharacter(sourceName, characterName); }
    }
    record ChannelSnapshot(int id, String name, int users, int capacity,
                           long minimumLevel, long maximumLevel) {}
    enum SelectStatus { ACCEPTED, BUSY, CLOSED }
    static final class Owner { private Owner() {} }
    private static final class DeletionGeneration {}
    /** Opaque, owner-bound evidence captured before an adapter's fresh row query. */
    static final class SelectionTicket {
        private final Owner owner;
        private final DeletionGeneration generation;
        private SelectionTicket(Owner owner, DeletionGeneration generation) {
            this.owner = owner; this.generation = generation;
        }
    }
    /**
     * Opaque pin for one exact selected role, never a numeric wire credential.
     * Only retainSelection can create it. close is idempotent and does not call
     * external code or perform I/O. It removes this pin, not an unrelated later
     * selection; a pending clear/return/disconnect is completed at that point.
     */
    static final class SelectionRetention implements AutoCloseable {
        private final TournamentPresence authority;
        private final Owner owner;
        private final Presence presence;
        private final CharacterKey key;
        private boolean closed;
        private SelectionRetention(TournamentPresence authority, Owner owner,
                                   Presence presence, CharacterKey key) {
            this.authority = authority; this.owner = owner; this.presence = presence; this.key = key;
        }
        CharacterKey key() { return key; }
        /** An active selection match, not permission to ignore lease invalidation. */
        boolean matches(Owner expectedOwner, CharacterKey expectedKey) {
            synchronized (authority) {
                return !closed && owner == expectedOwner && key.equals(expectedKey)
                        && authority.owners.get(owner) == presence && presence.retention == this
                        && !presence.closing && !presence.selectionReleaseRequested;
            }
        }
        @Override public void close() { authority.releaseRetention(this); }
        @Override public String toString() { return "TournamentSelectionRetention[redacted]"; }
    }
    private static final class Presence {
        Selection selected;
        String alias;
        Integer channel;
        SelectionRetention retention;
        boolean selectionReleaseRequested;
        boolean closing;
    }
    private final Map<Owner, Presence> owners = new LinkedHashMap<>();
    private final Map<CharacterKey, Owner> characterLocks = new LinkedHashMap<>();
    private final Map<String, Owner> aliases = new LinkedHashMap<>();
    private final Set<String> deletingAliases = new LinkedHashSet<>();
    // An identity token avoids counter wrap and a permanent per-character
    // tombstone map. A concurrent deletion conservatively retries any pending
    // selection; only in-flight callers retain old generation objects.
    private DeletionGeneration deletionGeneration = new DeletionGeneration();
    private final Map<Integer, String> sourceNames = new LinkedHashMap<>();
    private final List<Set<Owner>> channels = TIERS.stream()
            .map(tier -> (Set<Owner>) new LinkedHashSet<Owner>()).toList();

    synchronized Owner connect() {
        Owner owner = new Owner(); owners.put(owner, new Presence()); return owner;
    }
    /** Both adapters must agree on stable source IDs; array positions are not IDs. */
    synchronized void registerSource(int sourceId, String sourceName) {
        if (sourceId <= 0 || sourceName == null || sourceName.isBlank()) {
            throw new IllegalArgumentException("invalid Tournament source binding");
        }
        String normalized = aliasKey(sourceName.trim());
        String prior = sourceNames.get(sourceId);
        if ((prior != null && !prior.equals(normalized)) || sourceNames.entrySet().stream()
                .anyMatch(entry -> entry.getKey() != sourceId && entry.getValue().equals(normalized))) {
            throw new IllegalArgumentException("conflicting Tournament source binding");
        }
        sourceNames.put(sourceId, normalized);
    }
    synchronized SelectStatus select(Owner owner, Selection selected) {
        Objects.requireNonNull(selected);
        Presence present = owners.get(owner);
        if (present == null || present.closing) return SelectStatus.CLOSED;
        if (present.retention != null || present.selectionReleaseRequested) return SelectStatus.BUSY;
        registerSource(selected.key().sourceId(), selected.sourceName());
        String alias = aliasKey(selected.legacyAlias());
        Owner locked = characterLocks.get(selected.key());
        Owner named = aliases.get(alias);
        if ((locked != null && locked != owner) || (named != null && named != owner)
                || deletingAliases.contains(alias)) return SelectStatus.BUSY;
        clearSelectionInternal(owner, present);
        present.selected = selected; present.alias = alias;
        characterLocks.put(selected.key(), owner); aliases.put(alias, owner);
        return SelectStatus.ACCEPTED;
    }
    synchronized SelectionTicket selectionTicket(Owner owner) {
        Presence present = owners.get(owner);
        return present != null && !present.closing && present.retention == null
                && !present.selectionReleaseRequested ? new SelectionTicket(owner, deletionGeneration) : null;
    }
    /** Validate the query's deletion generation and claim under the SAME monitor. */
    synchronized SelectStatus select(Owner owner, Selection selected, SelectionTicket ticket) {
        Presence present = owners.get(owner);
        if (present == null || present.closing) return SelectStatus.CLOSED;
        if (ticket == null || ticket.owner != owner || ticket.generation != deletionGeneration) {
            return SelectStatus.BUSY;
        }
        return select(owner, selected);
    }
    /**
     * Called only after the adapter has successfully claimed this exact key.
     * There is at most one pin per owner. It covers the entire selection lease,
     * not merely an individual operation: an admitted commit can keep it after
     * disconnect until the operation's real worker-finally close retires it.
     */
    synchronized SelectionRetention retainSelection(Owner owner, CharacterKey expectedKey) {
        Presence present = owners.get(owner);
        if (present == null || present.closing || present.selectionReleaseRequested
                || present.retention != null || present.selected == null
                || !present.selected.key().equals(expectedKey)
                || deletingAliases.contains(present.alias)) return null;
        present.retention = new SelectionRetention(this, owner, present, expectedKey);
        return present.retention;
    }
    private synchronized void releaseRetention(SelectionRetention retained) {
        if (retained.closed) return;
        retained.closed = true;
        Presence present = owners.get(retained.owner);
        if (present != retained.presence || present.retention != retained) return;
        present.retention = null;
        if (present.selectionReleaseRequested) clearSelectionInternal(retained.owner, present);
        if (present.closing) owners.remove(retained.owner, present);
    }
    /**
     * Explicit legacy-name reservation for old internal callers/deletion tests.
     * This does not create a CharacterKey or qualify a modern character. Live
     * native selection uses select() with its verified database row instead.
     */
    synchronized boolean reserveLegacyAlias(Owner owner, String legacyAlias) {
        Presence present = owners.get(owner);
        String alias = aliasKey(legacyAlias);
        if (present == null || present.closing || present.retention != null
                || present.selectionReleaseRequested || alias.isEmpty() || deletingAliases.contains(alias)) return false;
        Owner held = aliases.get(alias);
        if (held != null && held != owner) return false;
        clearSelectionInternal(owner, present);
        present.alias = alias; aliases.put(alias, owner); return true;
    }
    synchronized boolean beginDeletion(String legacyAlias, Owner requester) {
        String alias = aliasKey(legacyAlias);
        if (alias.isEmpty() || deletingAliases.contains(alias)) return false;
        Owner held = aliases.get(alias);
        if (held != null && held != requester) return false;
        Presence present = owners.get(held);
        // The legacy same-owner deletion allowance must not bypass an admitted
        // modern commit, even after that owner's TLS session has disconnected.
        if (present != null && (present.retention != null || present.closing)) return false;
        deletingAliases.add(alias);
        deletionGeneration = new DeletionGeneration();
        return true;
    }
    synchronized void endDeletion(String legacyAlias) {
        if (deletingAliases.remove(aliasKey(legacyAlias))) {
            // Also invalidate a query begun while deletion was in progress:
            // it may have observed the row before the database commit.
            deletionGeneration = new DeletionGeneration();
        }
    }
    synchronized Selection selected(Owner owner) {
        Presence present = owners.get(owner); return present == null ? null : present.selected;
    }
    synchronized void clearSelection(Owner owner) {
        Presence present = owners.get(owner);
        if (present != null) clearSelectionInternal(owner, present);
    }
    private void clearSelectionInternal(Owner owner, Presence present) {
        if (present.retention != null) {
            present.selectionReleaseRequested = true;
            return;
        }
        if (present.selected != null) characterLocks.remove(present.selected.key(), owner);
        if (present.alias != null) aliases.remove(present.alias, owner);
        present.selected = null; present.alias = null;
        present.selectionReleaseRequested = false;
    }
    /** Capacity-only internal primitive. Native phase/roster rules remain in its adapter. */
    synchronized ChannelSnapshot enterChannel(Owner owner, int channelId) {
        Presence present = owners.get(owner);
        if (present == null || present.closing || present.selectionReleaseRequested
                || channelId < 0 || channelId >= TIERS.size()) return null;
        Set<Owner> target = channels.get(channelId);
        if (!target.contains(owner) && target.size() >= CHANNEL_CAPACITY) return null;
        if (present.channel != null && present.channel != channelId) channels.get(present.channel).remove(owner);
        target.add(owner); present.channel = channelId;
        return snapshot(channelId);
    }
    synchronized Integer channelOf(Owner owner) {
        Presence present = owners.get(owner); return present == null ? null : present.channel;
    }
    synchronized void leaveChannel(Owner owner) {
        Presence present = owners.get(owner);
        if (present != null && present.channel != null) {
            channels.get(present.channel).remove(owner); present.channel = null;
        }
    }
    synchronized void disconnect(Owner owner) {
        Presence present = owners.get(owner);
        if (present == null) return;
        present.closing = true;
        leaveChannel(owner); clearSelectionInternal(owner, present);
        if (present.retention == null) owners.remove(owner, present);
    }
    synchronized void returnToCharacters(Owner owner) {
        Presence present = owners.get(owner);
        if (present == null) return;
        leaveChannel(owner); clearSelectionInternal(owner, present);
    }
    synchronized List<Owner> members(int channelId) {
        return channelId < 0 || channelId >= TIERS.size() ? List.of() : List.copyOf(channels.get(channelId));
    }
    synchronized List<Owner> channelMembers() {
        List<Owner> result = new ArrayList<>();
        for (Set<Owner> channel : channels) result.addAll(channel);
        return List.copyOf(result);
    }
    synchronized int userCount() { return channels.stream().mapToInt(Set::size).sum(); }
    // A disconnected owner pinned by an actual commit worker still counts;
    // channel/user counts above exclude it immediately. Do not fake quiescence.
    synchronized int ownerCount() { return owners.size(); }
    synchronized ChannelSnapshot snapshot(int channelId) {
        if (channelId < 0 || channelId >= TIERS.size()) return null;
        Tier tier = TIERS.get(channelId);
        return new ChannelSnapshot(channelId, tier.name(), channels.get(channelId).size(), CHANNEL_CAPACITY,
                tier.minimumLevel(), tier.maximumLevel());
    }
    synchronized List<ChannelSnapshot> snapshots() {
        return TIERS.stream().map(tier -> snapshot(tier.id())).toList();
    }
    private static String aliasKey(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
