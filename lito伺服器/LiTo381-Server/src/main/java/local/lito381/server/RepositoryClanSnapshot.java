// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import local.lito381.server.RepositoryClanMember;

record RepositoryClanSnapshot(boolean authoritative, long clanId, String name, long leaderObjectId, String leaderName, int castleId, int houseId, int maximumOnlineUsers, boolean showEmblem, String announcement, long birthEpochSeconds, boolean destroyPending, List<RepositoryClanMember> members) {
    RepositoryClanSnapshot {
        boolean empty;
        name = name == null ? "" : name.trim();
        leaderName = leaderName == null ? "" : leaderName.trim();
        announcement = announcement == null ? "" : announcement;
        members = RepositoryClanSnapshot.normalizeClanMembers(members);
        boolean bl = empty = clanId == 0L;
        if (clanId < 0L || clanId > 0xFFFFFFFFL || leaderObjectId < 0L || leaderObjectId > 0xFFFFFFFFL || castleId < 0 || houseId < 0 || maximumOnlineUsers < 0 || birthEpochSeconds < 0L || empty && (!name.isEmpty() || leaderObjectId != 0L || !leaderName.isEmpty() || !members.isEmpty()) || !empty && (name.isBlank() || name.length() > 45 || leaderObjectId == 0L || leaderName.isBlank() || leaderName.length() > 45)) {
            throw new IllegalArgumentException("invalid repository clan snapshot");
        }
        if (!empty) {
            long owners = members.stream().filter(member -> member.role() == 4).count();
            String canonicalLeaderName = leaderName;
            boolean exactLeader = members.stream().anyMatch(member -> member.objectId() == leaderObjectId && member.name().equalsIgnoreCase(canonicalLeaderName) && member.role() == 4);
            if (owners != 1L || !exactLeader) {
                throw new IllegalArgumentException("repository clan leader/member mismatch");
            }
        }
    }

    static RepositoryClanSnapshot unavailable() {
        return new RepositoryClanSnapshot(false, 0L, "", 0L, "", 0, 0, 0, false, "", 0L, false, List.of());
    }

    static RepositoryClanSnapshot empty() {
        return new RepositoryClanSnapshot(true, 0L, "", 0L, "", 0, 0, 0, false, "", 0L, false, List.of());
    }

    boolean present() {
        return this.authoritative && this.clanId != 0L;
    }

    private static List<RepositoryClanMember> normalizeClanMembers(List<RepositoryClanMember> source) {
        List<Object> rows;
        List<Object> list = rows = source == null ? List.of() : source;
        if (rows.size() > 1024) {
            throw new IllegalArgumentException("repository clan exceeds 1024 members");
        }
        TreeMap<Long, RepositoryClanMember> identities = new TreeMap<Long, RepositoryClanMember>();
        LinkedHashSet<String> names = new LinkedHashSet<String>();
        for (RepositoryClanMember repositoryClanMember : rows) {
            String key;
            String string = key = repositoryClanMember == null ? "" : repositoryClanMember.name().toLowerCase(Locale.ROOT);
            if (repositoryClanMember != null && identities.putIfAbsent(repositoryClanMember.objectId(), repositoryClanMember) == null && names.add(key)) continue;
            throw new IllegalArgumentException("duplicate/null repository clan member");
        }
        return List.copyOf(identities.values());
    }
}
