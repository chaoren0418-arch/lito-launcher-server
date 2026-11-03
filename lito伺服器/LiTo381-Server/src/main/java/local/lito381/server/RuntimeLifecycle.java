// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import local.lito381.server.CharacterSummary;
import local.lito381.server.ClientSession;
import local.lito381.server.LiveBattleStats;
import local.lito381.server.ServerConfig;

final class RuntimeLifecycle {
    RuntimeLifecycle() {
    }

    static boolean enabled(ClientSession clientSession) {
        return ((ServerConfig)LiveBattleStats.get(clientSession, "config")).ephemeralSourceProfiles();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void reset(ClientSession clientSession, String string) {
        Object object;
        if (!RuntimeLifecycle.enabled(clientSession)) {
            return;
        }
        Object object2 = List.of("spellReadyNanos", "itemReadyNanos", "storySkillEffectDeadlines").iterator();
        while (object2.hasNext()) {
            object = object2.next();
            ((Map)LiveBattleStats.get(clientSession, (String)object)).clear();
        }
        object = object2 = LiveBattleStats.get(clientSession, "tamedCompanions");
        synchronized (object) {
            ((Collection)object2).clear();
        }
        RuntimeLifecycle.set(clientSession, "lastAttacker", null);
        RuntimeLifecycle.set(clientSession, "lastCombatTargetObjectId", 0L);
        RuntimeLifecycle.set(clientSession, "pendingTournamentShop", null);
        if ("disconnect".equals(string)) {
            ((Map)LiveBattleStats.get(clientSession, "sqlTournamentProfiles")).clear();
        }
    }

    static void loaded(ClientSession clientSession) {
        if (!RuntimeLifecycle.enabled(clientSession)) {
            return;
        }
        HashSet<Long> hashSet = new HashSet<Long>();
        for (CharacterSummary characterSummary : (List)LiveBattleStats.get(clientSession, "characters")) {
            hashSet.add(characterSummary.objectId());
        }
        ((Map)LiveBattleStats.get(clientSession, "sqlTournamentProfiles")).keySet().retainAll(hashSet);
    }

    static void set(Object object, String string, Object object2) {
        try {
            LiveBattleStats.field(object.getClass(), string).set(object, object2);
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException("Runtime lifecycle field " + string, reflectiveOperationException);
        }
    }
}
