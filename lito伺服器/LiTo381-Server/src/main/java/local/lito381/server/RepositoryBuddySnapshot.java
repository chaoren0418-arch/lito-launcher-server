// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import local.lito381.server.RepositoryBuddy;
import local.lito381.server.RepositoryBuddyGroup;

record RepositoryBuddySnapshot(boolean authoritative, List<RepositoryBuddyGroup> groups, List<RepositoryBuddy> buddies) {
    RepositoryBuddySnapshot {
        groups = RepositoryBuddySnapshot.normalizeGroups(groups);
        buddies = RepositoryBuddySnapshot.normalizeBuddies(buddies, groups);
    }

    static RepositoryBuddySnapshot unavailable() {
        return new RepositoryBuddySnapshot(false, List.of(), List.of());
    }

    private static List<RepositoryBuddyGroup> normalizeGroups(List<RepositoryBuddyGroup> source) {
        List<Object> rows = source == null ? List.of() : source;
        TreeMap<Integer, RepositoryBuddyGroup> ids = new TreeMap<Integer, RepositoryBuddyGroup>();
        LinkedHashSet<String> names = new LinkedHashSet<String>();
        for (RepositoryBuddyGroup repositoryBuddyGroup : rows) {
            String name;
            String string = name = repositoryBuddyGroup == null ? "" : repositoryBuddyGroup.name().toLowerCase(Locale.ROOT);
            if (repositoryBuddyGroup != null && ids.putIfAbsent(repositoryBuddyGroup.id(), repositoryBuddyGroup) == null && names.add(name)) continue;
            throw new IllegalArgumentException("duplicate or null buddy group");
        }
        if (!ids.isEmpty() && !ids.containsKey(1)) {
            throw new IllegalArgumentException("buddy group 1 is required");
        }
        return List.copyOf(ids.values());
    }

    private static List<RepositoryBuddy> normalizeBuddies(List<RepositoryBuddy> source, List<RepositoryBuddyGroup> groups) {
        List<Object> rows;
        List<Object> list = rows = source == null ? List.of() : source;
        if (rows.size() > 255) {
            throw new IllegalArgumentException("repository buddy snapshot exceeds 255 rows");
        }
        Set groupIds = groups.stream().map(RepositoryBuddyGroup::id).collect(Collectors.toSet());
        TreeMap<String, RepositoryBuddy> names = new TreeMap<String, RepositoryBuddy>();
        for (RepositoryBuddy repositoryBuddy : rows) {
            String name;
            String string = name = repositoryBuddy == null ? "" : repositoryBuddy.name().toLowerCase(Locale.ROOT);
            if (repositoryBuddy != null && groupIds.contains(repositoryBuddy.groupId()) && names.putIfAbsent(name, repositoryBuddy) == null) continue;
            throw new IllegalArgumentException("invalid or duplicate repository buddy");
        }
        return List.copyOf(names.values());
    }
}
