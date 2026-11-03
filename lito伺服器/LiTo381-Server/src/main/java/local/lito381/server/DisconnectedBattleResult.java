package local.lito381.server;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;

/** The delayed disconnect result must not depend on an already released world profile. */
final class DisconnectedBattleResult {
    private static final Field PROFILES;
    static {
        try { PROFILES=ClientSession.class.getDeclaredField("sqlTournamentProfiles");PROFILES.setAccessible(true); }
        catch(ReflectiveOperationException e){throw new ExceptionInInitializerError(e);}
    }
    static GameStateStore.Progress record(GameStateStore live,String character,boolean won,
            boolean disconnected,int reward,int experience,
            GameStateStore.TournamentProfilePersistenceHook commit,ClientSession session) throws IOException {
        if(DurableBattleSettlement.route(session)!=null)
            return DurableBattleSettlement.record(live,character,reward,experience,session);
        if(!disconnected || commit==null)
            return live.recordResult(character,won,disconnected,reward,experience,commit);
        RepositoryTournamentProfile baseline=null;
        try {
            for(Object value:((Map<?,?>)PROFILES.get(session)).values()) {
                var row=(RepositoryTournamentProfile)value;
                if(row.characterName().equals(session.selectedCharacterName())) {baseline=row;break;}
            }
        } catch(IllegalAccessException e){throw new IOException(e);}
        if(baseline==null) throw new IOException("Disconnected settlement has no source SQL baseline");
        // This temporary store contains only the compact result snapshot. No SQL/player
        // inventory is reloaded, and a newly logged-in session's working set is untouched.
        var resultState=new GameStateStore(Path.of("unused-disconnect-result.state"),false,true);
        resultState.reconcileTournamentProfileStates(Map.of(character,baseline.state()));
        try {
            return resultState.recordResult(character,won,true,reward,experience,commit);
        } finally { resultState.releaseSourceCharacter(character); }
    }
}
