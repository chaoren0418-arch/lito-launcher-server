// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.List;
import local.lito381.server.SourceMagicDollPower;

record SourceMagicDollType(int itemId, int dollType, int iconId, List<Integer> powerIds, List<Integer> materialIds, List<Integer> materialCounts, int durationSeconds, int gfxId, String nameId, List<SourceMagicDollPower> powerDefinitions) {
    SourceMagicDollType(int itemId, int dollType, int iconId, List<Integer> powerIds, List<Integer> materialIds, List<Integer> materialCounts, int durationSeconds, int gfxId, String nameId) {
        this(itemId, dollType, iconId, powerIds, materialIds, materialCounts, durationSeconds, gfxId, nameId, List.of());
    }

    SourceMagicDollType {
        powerIds = powerIds == null ? List.of() : List.copyOf(powerIds);
        materialIds = materialIds == null ? List.of() : List.copyOf(materialIds);
        materialCounts = materialCounts == null ? List.of() : List.copyOf(materialCounts);
        powerDefinitions = powerDefinitions == null ? List.of() : List.copyOf(powerDefinitions);
        String string = nameId = nameId == null ? "" : nameId;
        if (itemId <= 0 || dollType < 0 || iconId < 0 || durationSeconds <= 0 || durationSeconds > 65535 || gfxId <= 0 || gfxId > 65535 || nameId.isBlank() || materialIds.size() != materialCounts.size() || powerIds.stream().anyMatch(id -> id == null || id <= 0) || materialIds.stream().anyMatch(id -> id == null || id <= 0) || materialCounts.stream().anyMatch(count -> count == null || count <= 0) || !powerDefinitions.isEmpty() && !powerDefinitions.stream().map(SourceMagicDollPower::id).toList().equals(powerIds)) {
            throw new IllegalArgumentException("invalid selected-source magic doll type");
        }
    }
}
