// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Map;
import java.util.Set;

record LitoCharacterPersistence(String account, String characterName, long objectId, int currentHp, int currentMp, int level, long experience, int lawful, int pkCount, long lastPkEpochSeconds, int elfPkCount, long lastElfPkEpochSeconds, int hellTime, int killCount, int deathCount, int x, int y, int mapId, int heading, int elfAttribute, Set<Integer> skills, Map<Integer, Integer> quests) {
    LitoCharacterPersistence {
        if (account == null || account.isBlank() || characterName == null || characterName.isBlank() || objectId <= 0L) {
            throw new IllegalArgumentException("invalid LiTo character persistence identity");
        }
        currentHp = Math.max(0, currentHp);
        currentMp = Math.max(0, currentMp);
        level = Math.max(1, level);
        experience = Math.max(0L, experience);
        hellTime = Math.max(0, hellTime);
        killCount = Math.max(0, killCount);
        deathCount = Math.max(0, deathCount);
        skills = skills == null ? Set.of() : Set.copyOf(skills);
        quests = quests == null ? Map.of() : Map.copyOf(quests);
    }
}
