// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.List;
import local.lito381.server.RepositoryBattleObject;
import local.lito381.server.RepositoryBattlePlayerSpawn;
import local.lito381.server.RepositoryBattleRule;
import local.lito381.server.RepositoryBattleSpawn;

record RepositoryBattleConfiguration(List<RepositoryBattleRule> rules, List<RepositoryBattleObject> objects, List<RepositoryBattleSpawn> spawns, List<RepositoryBattlePlayerSpawn> playerSpawns) {
    RepositoryBattleConfiguration(List<RepositoryBattleRule> rules, List<RepositoryBattleObject> objects, List<RepositoryBattleSpawn> spawns) {
        this(rules, objects, spawns, List.of());
    }

    RepositoryBattleConfiguration {
        rules = rules == null ? List.of() : List.copyOf(rules);
        objects = objects == null ? List.of() : List.copyOf(objects);
        spawns = spawns == null ? List.of() : List.copyOf(spawns);
        playerSpawns = playerSpawns == null ? List.of() : List.copyOf(playerSpawns);
    }

    static RepositoryBattleConfiguration empty() {
        return new RepositoryBattleConfiguration(List.of(), List.of(), List.of(), List.of());
    }
}
