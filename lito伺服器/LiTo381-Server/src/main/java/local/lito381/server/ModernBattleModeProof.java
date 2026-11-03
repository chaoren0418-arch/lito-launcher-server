package local.lito381.server;

import java.util.HashSet;
import java.util.List;

/** Cross-field mode proofs in ENGINE revision space. No legacy wire offset,
 * mutable publication receipt, guessed map geometry or new result identity. */
final class ModernBattleModeProof {
    static void siege(long revision,ModernBattleEngine.Phase phase,ModernBattleObservation.Clock clock,
            ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,List<ModernBattleObservation.Actor> actors,ModernSiege.Snapshot snapshot) {
        var s=snapshot.state();var c=s.config();var config=room.summary().config();
        boolean waiting=phase==ModernBattleEngine.Phase.LOADING||phase==ModernBattleEngine.Phase.COUNTDOWN;
        if(!c.key().equals(new ModernWorldBinding.Key(config.mode(),config.mapId()))
                ||(phase==ModernBattleEngine.Phase.FINISHED)!=s.finished()
                ||(phase==ModernBattleEngine.Phase.ABORTED)!=(s.phase()==ModernSiege.Phase.ABORTED)
                ||waiting!=(s.phase()==ModernSiege.Phase.WAITING)
                ||s.finished()&&(s.finishRevision()!=revision||s.finishedAtMillis()>clock.activeElapsedNanos()/1_000_000))throw bad();
        var geometry=new HashSet<>(c.gate());geometry.addAll(c.towerCells());geometry.addAll(c.attackCamp());geometry.addAll(c.defenseCamp());
        geometry.add(c.portal().outside());geometry.add(c.portal().inside());
        for(var cell:geometry)if(!ModernWorldProtocol.contains(manifest,cell))throw bad();
        int[] teams={0,0};
        for(var a:actors) {
            teams[a.pose().member().team()]++;
            if(c.towerCells().contains(a.pose().position())||!s.open()&&c.gate().contains(a.pose().position()))throw bad();
        }
        if(teams[0]<15||teams[1]<15||teams[0]>50||teams[1]>50)throw bad();
        for(var t:s.objectives())if(t.lastActor()>actors.size()||t.hitCount()>0&&actors.get((int)t.lastActor()-1).pose().member().team()!=s.attacker()
                ||t.hitCount()>t.revision()||t.hp()==0&&t.deathRevision()!=t.revision()
                ||t.tower().slot()==ModernSiege.Slot.INNER&&t.hitCount()>0&&(s.gateRevision()==0||t.revision()<=s.gateRevision()))throw bad();
    }
    static void elimination(long revision,List<ModernBattleObservation.Actor> actors,ModernMatchSettlement.Event result) {
        if(result==null||result.mode()!=1)return;
        var teams=actors.stream().filter(a->a.vital()!=null&&a.vital().values().hp()>0).map(a->a.pose().member().team()).distinct().toList();
        if(teams.size()!=1||actors.stream().anyMatch(a->a.vital()==null)
                ||actors.stream().noneMatch(a->a.vital().values().hp()==0&&a.vital().deathRevision()==revision))throw bad();
        int winner=teams.get(0);
        for(var p:result.participants())if((p.verdict()==ModernMatchSettlement.Verdict.WIN)!=(p.team()==winner))throw bad();
    }
    /** A terminal capture may briefly omit its event while the engine creates
     * it once. A published audience may never contain that bootstrap state. */
    static void publication(ModernBattleObservation.World w,ModernMatchDescriptor descriptor) {
        var m=w.modes();var time=m.timeAttackResult();var siege=m.siegeResult();
        if(m.timeAttack().finished()!=(time!=null)||m.siege().finished()!=(siege!=null)
                ||m.siege().enabled()&&m.siege().state().config().durationMillis()!=descriptor.activeTimeoutMillis()
                ||time!=null&&!ModernTimeAttackSettlement.event(w,time.resultId(),time.finishedAtMillis()).equals(time)
                ||siege!=null&&!ModernSiegeSettlement.event(w,siege.resultId(),siege.finishedAtMillis()).equals(siege))throw bad();
        if(descriptor.ruleset()==ModernMatchDescriptor.Ruleset.DEATHMATCH_ELIMINATION&&w.phase()==ModernBattleEngine.Phase.ACTIVE
                &&w.actors().stream().filter(a->a.vital().values().hp()>0).map(a->a.pose().member().team()).distinct().count()<2)throw bad();
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid canonical battle mode proof");}
    private ModernBattleModeProof(){}
}
