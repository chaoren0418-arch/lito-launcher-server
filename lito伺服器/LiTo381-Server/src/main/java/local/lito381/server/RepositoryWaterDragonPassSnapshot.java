// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import local.lito381.server.GameStateStore;

record RepositoryWaterDragonPassSnapshot(boolean authoritative, List<GameStateStore.WaterDragonPassState> passes) {
    RepositoryWaterDragonPassSnapshot {
        passes = passes == null ? List.of() : List.copyOf(passes);
        LinkedHashSet<Long> objects = new LinkedHashSet<Long>();
        LinkedHashSet<Integer> rooms = new LinkedHashSet<Integer>();
        LinkedHashSet<String> renters = new LinkedHashSet<String>();
        for (GameStateStore.WaterDragonPassState pass : passes) {
            if (pass != null && objects.add(pass.itemObjectId()) && rooms.add(pass.roomNumber()) && renters.add(pass.renterIdentity().toLowerCase(Locale.ROOT))) continue;
            throw new IllegalArgumentException("duplicate/null repository Water Dragon pass");
        }
        if (!authoritative && !passes.isEmpty()) {
            throw new IllegalArgumentException("unavailable Water Dragon pass snapshot has rows");
        }
    }

    static RepositoryWaterDragonPassSnapshot unavailable() {
        return new RepositoryWaterDragonPassSnapshot(false, List.of());
    }
}
