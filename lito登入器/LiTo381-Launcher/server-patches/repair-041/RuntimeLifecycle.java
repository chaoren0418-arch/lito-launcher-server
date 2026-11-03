package local.lito381.server;
import java.util.*;

/** Shared-source runtime ownership boundaries. No database writes or global session cache. */
final class RuntimeLifecycle {
    static boolean enabled(ClientSession s) {
        return ((ServerConfig) LiveBattleStats.get(s,"config")).ephemeralSourceProfiles();
    }
    static void reset(ClientSession s, String reason) {
        if (!enabled(s)) return;
        // Called after the native economy reset, including its uninitialized path.
        // The result page has already received its immutable score rows.
        for (String field : List.of("spellReadyNanos", "itemReadyNanos", "storySkillEffectDeadlines"))
            ((Map<?,?>) LiveBattleStats.get(s,field)).clear();
        Object companions = LiveBattleStats.get(s,"tamedCompanions");
        synchronized (companions) { ((Collection<?>) companions).clear(); }
        set(s,"lastAttacker",null);
        set(s,"lastCombatTargetObjectId",0L);
        set(s,"pendingTournamentShop",null);
        if ("disconnect".equals(reason))
            ((Map<?,?>) LiveBattleStats.get(s,"sqlTournamentProfiles")).clear();
    }
    static void loaded(ClientSession s) {
        if (!enabled(s)) return;
        Set<Long> owned = new HashSet<>();
        for (CharacterSummary c : (List<CharacterSummary>) LiveBattleStats.get(s,"characters")) owned.add(c.objectId());
        ((Map<?,?>) LiveBattleStats.get(s,"sqlTournamentProfiles")).keySet().retainAll(owned);
    }
    static void set(Object object, String name, Object value) {
        try { LiveBattleStats.field(object.getClass(),name).set(object,value); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Runtime lifecycle field " + name,e); }
    }
}
