package local.lito381.server;

import java.util.List;

/** Cross-field life proof in ENGINE revision space, without legacy frames.
 * Granted HP is an applied total across equipment eras, never current maxHP
 * multiplied by a count. This validates facts, not a client resurrection
 * request, elapsed wall time, map reachability or a complete mode envelope. */
final class ModernBattleLifeProof {
    private record Life(long entity,long deaths,long deathRevision,long respawns,long respawnRevision,long hpGranted) {}
    static void honor(long revision,ModernBattleEngine.Phase phase,ModernBattleObservation.Clock clock,
            ModernWorldMap.Manifest manifest,List<ModernBattleObservation.Actor> actors,List<ModernHonor.Life> lives) {
        if(lives.size()!=actors.size())throw bad();
        for(int i=0;i<actors.size();i++) {
            var a=actors.get(i);var l=lives.get(i);
            var proof=new Life(l.entityId(),l.deathCount(),l.lastDeathRevision(),l.respawnCount(),l.respawnRevision(),l.respawnHpGain());
            life(revision,phase,clock,a,proof);
            pending(phase,clock,proof,l.pendingDueMillis(),ModernHonor.RESPAWN_MILLIS);
            var death=new ModernWorldMap.Cell(l.deathX(),l.deathY());
            if(l.deathCount()>0&&(!ModernWorldProtocol.contains(manifest,death)
                    ||(a.vital().values().hp()==0||l.lastDeathRevision()==revision||l.respawnRevision()==revision)&&!a.pose().position().equals(death)))throw bad();
        }
    }
    static void quest(long revision,ModernBattleEngine.Phase phase,ModernBattleObservation.Clock clock,
            ModernWorldMap.Manifest manifest,List<ModernBattleObservation.Actor> actors,List<ModernQuestLife.State> lives) {
        if(lives.size()!=actors.size())throw bad();
        for(int i=0;i<actors.size();i++) {
            var a=actors.get(i);var l=lives.get(i);
            life(revision,phase,clock,a,new Life(l.entityId(),l.deaths(),l.deathRevision(),l.respawns(),l.respawnRevision(),l.hpGain()));
            if(l.lane()!=a.pose().member().team()||!ModernWorldProtocol.contains(manifest,l.spawn())
                    ||l.deaths()>0&&!ModernWorldProtocol.contains(manifest,l.deathCell())
                    ||l.dead()&&!a.pose().position().equals(l.deathCell())
                    ||l.deathRevision()==revision&&!a.pose().position().equals(l.deathCell())
                    ||l.respawnRevision()==revision&&!a.pose().position().equals(l.spawn())
                    ||initial(phase)&&!a.pose().position().equals(l.spawn()))throw bad();
        }
    }
    static void siege(long revision,ModernBattleEngine.Phase phase,ModernBattleObservation.Clock clock,
            ModernWorldMap.Manifest manifest,List<ModernBattleObservation.Actor> actors,ModernSiege.Snapshot siege) {
        var lives=siege.lives();var s=siege.state();if(s==null||lives.size()!=actors.size())throw bad();
        var config=s.config();
        for(var cell:config.attackCamp())if(!ModernWorldProtocol.contains(manifest,cell))throw bad();
        for(var cell:config.defenseCamp())if(!ModernWorldProtocol.contains(manifest,cell))throw bad();
        for(int i=0;i<actors.size();i++) {
            var a=actors.get(i);var l=lives.get(i);
            var proof=new Life(l.entityId(),l.deaths(),l.deathRevision(),l.respawns(),l.respawnRevision(),l.hpGain());
            life(revision,phase,clock,a,proof);pending(phase,clock,proof,l.dueMillis(),config.respawnMillis());
            if((initial(phase)||l.respawnRevision()==revision)&&!config.camp(a.pose().member().team(),s.attacker()).contains(a.pose().position()))throw bad();
        }
    }
    /** A mode without participant resurrection cannot introduce a new life
     * or mint a resource grant, even when its resource equation balances. */
    static void absent(List<ModernBattleObservation.Actor> actors) {
        for(var a:actors)if(a.resources()!=null&&(a.resources().life()!=0||a.resources().flows().hpGranted()!=0||a.resources().flows().mpGranted()!=0))throw bad();
    }
    private static void life(long revision,ModernBattleEngine.Phase phase,ModernBattleObservation.Clock clock,
            ModernBattleObservation.Actor actor,Life l) {
        var vital=actor.vital();var resources=actor.resources();
        if(vital==null||resources==null||l.entity()!=actor.pose().entityId()
                ||l.deathRevision()!=vital.deathRevision()||l.deathRevision()>revision||l.respawnRevision()>revision
                ||l.deaths()>l.deathRevision()||l.respawns()>l.respawnRevision()
                ||l.respawns()>revision-1||l.deaths()>revision-1-l.respawns()
                ||(vital.values().hp()==0)!=(l.deaths()>l.respawns())
                ||l.respawns()!=resources.life()||l.hpGranted()!=resources.flows().hpGranted()||resources.flows().mpGranted()!=0
                ||l.deathRevision()>resources.revision()||l.respawnRevision()>resources.revision()
                ||l.deaths()>0&&clock.activeElapsedNanos()<0||initial(phase)&&l.deaths()!=0
                ||l.respawnRevision()==revision&&(vital.values().hp()!=vital.values().maximumHp()||resources.revision()!=revision))throw bad();
    }
    private static void pending(ModernBattleEngine.Phase phase,ModernBattleObservation.Clock clock,
            Life l,long dueMillis,long delayMillis) {
        boolean pending=phase==ModernBattleEngine.Phase.ACTIVE&&l.deaths()>l.respawns();
        if((dueMillis>0)!=pending)throw bad();
        if(pending) {
            long latest=clock.activeElapsedNanos()/1_000_000+delayMillis;
            // An occupied siege camp may legitimately delay an already-due
            // resurrection. Never equate a due timer with an applied grant.
            // A read may advance the clock without changing the world revision:
            // deathRevision == worldRevision does NOT locate the death in time.
            if(clock.activeElapsedNanos()<0||dueMillis>latest)throw bad();
        }
    }
    private static boolean initial(ModernBattleEngine.Phase phase){return phase==ModernBattleEngine.Phase.LOADING||phase==ModernBattleEngine.Phase.COUNTDOWN;}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid canonical participant life proof");}
    private ModernBattleLifeProof(){}
}
