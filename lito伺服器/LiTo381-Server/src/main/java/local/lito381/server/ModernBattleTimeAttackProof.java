package local.lito381.server;

import java.util.HashSet;
import java.util.List;

/** Cooperative mode proof without projecting into fixed-price legacy frames.
 * Full plan/catalog remains private; only its digest is exported to audiences. */
final class ModernBattleTimeAttackProof {
    static void validate(long revision,ModernBattleEngine.Phase phase,ModernBattleObservation.Clock clock,
            ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,List<ModernBattleObservation.Actor> actors,
            ModernTimeAttack.Snapshot time,ModernBattleObservation.Npcs npcs) {
        var s=time.state();var plan=s.plan();var tower=plan.tower();var config=room.summary().config();
        boolean waiting=phase==ModernBattleEngine.Phase.LOADING||phase==ModernBattleEngine.Phase.COUNTDOWN;
        if(!plan.key().equals(new ModernWorldBinding.Key(config.mode(),config.mapId()))||actors.size()>60||npcs==null
                ||s.leader()>actors.size()||s.departed().stream().anyMatch(id->id>actors.size())
                ||(phase==ModernBattleEngine.Phase.FINISHED)!=time.finished()
                ||(phase==ModernBattleEngine.Phase.ABORTED)!=(s.phase()==ModernTimeAttack.Phase.ABORTED)
                ||waiting&&(s.phase()!=ModernTimeAttack.Phase.WAITING_FOR_TOWER||s.observedMillis()!=0||!time.exchanges().isEmpty()||!s.departed().isEmpty())
                ||s.observedMillis()>Math.max(0,clock.activeElapsedNanos()/1_000_000)
                ||!ModernWorldProtocol.contains(manifest,tower.cell())
                ||time.finished()&&s.revision()!=revision)throw bad();
        var eligible=new HashSet<Long>();
        for(var a:actors) {
            long id=a.pose().entityId();
            if(!a.pose().member().identity().isHuman()||a.pose().member().team()!=0||a.vital()==null
                    ||s.towerHp()>0&&!s.departed().contains(id)&&tower.cell().equals(a.pose().position()))throw bad();
            if(a.vital().values().hp()>0&&!s.departed().contains(id))eligible.add(id);
        }
        if(!s.terminal()&&!eligible.contains(s.leader())
                ||s.end()==ModernTimeAttack.End.LEADER_DEFEATED&&(actors.get((int)s.leader()-1).vital().values().hp()!=0||s.departed().contains(s.leader()))
                ||s.end()==ModernTimeAttack.End.PARTY_DEFEATED&&(!eligible.isEmpty()||!s.departed().contains(s.leader()))
                ||(s.end()==ModernTimeAttack.End.CLEARED||s.end()==ModernTimeAttack.End.TIMEOUT)&&!eligible.contains(s.leader())
                ||s.startedMillis()>=0&&s.phase()!=ModernTimeAttack.Phase.ABORTED
                    &&(s.end()==ModernTimeAttack.End.TIMEOUT?s.observedMillis()-s.startedMillis()!=plan.durationMillis():s.observedMillis()-s.startedMillis()>=plan.durationMillis()))throw bad();
        var roster=npcs.roster();var living=new HashSet<ModernNpcRoster.Id>();
        for(var n:roster.actors()) {
            if(n.lane()!=0)throw bad();
            if(n.alive()) {
                if(n.stage()!=plan.waves().get(s.wave()).kind().ordinal()+1)throw bad();living.add(n.id());
            }
        }
        if(!roster.closed()&&!living.equals(s.living())||s.living().stream().anyMatch(id->id.serial()>roster.spawned()))throw bad();
        long previous=0;boolean fatal=false;var seen=new HashSet<Long>();
        for(var e:time.exchanges()) {
            if(e.entityId()>actors.size()||!e.target().scope().equals(s.scope())||!seen.add(e.entityId())||e.revision()<=previous||e.revision()>s.revision()
                    ||e.sequence()>actors.get((int)e.entityId()-1).vital().attackSequence()
                    ||e.outcome().remainingHp()<s.towerHp()||e.outcome().remainingHp()>tower.maximumHp()
                    ||e.outcome().appliedDamage()>tower.maximumHp()-e.outcome().remainingHp()
                    ||e.outcome().death()&&e.revision()!=s.towerDeathRevision())throw bad();
            if(e.outcome().death())fatal=true;previous=e.revision();
        }
        for(var a:actors) {
            var structure=a.spells().structure();if(structure==null)continue;
            var impact=structure.impact();var use=impact.use();var history=a.spells().histories().get(ModernUseHistory.Lane.NPC_FIRE_STORM);
            if(structure.questKind()!=null||history==null||history.current().casts()<structure.casts()
                    ||use.revision()>s.revision()||use.sequence()>a.spells().sequence())throw bad();
            int count=0;
            for(var effect:impact.targets())if(effect.before().target() instanceof ModernTargetHistory.TimeTower t) {
                count++;var h=effect.before();var origin=new ModernWorldMap.Cell(use.before().x(),use.before().y());
                if(!t.id().scope().equals(s.scope())||!h.cell().equals(tower.cell())||h.maximumHp()!=tower.maximumHp()
                        ||effect.kind()!=ModernTargetHistory.Kind.DAMAGE||effect.hpAfter()<s.towerHp()
                        ||!ModernWorldProtocol.contains(manifest,origin)
                        ||Math.max(Math.abs((long)origin.x()-tower.cell().x()),Math.abs((long)origin.y()-tower.cell().y()))>ModernFireStorm.RADIUS
                        ||effect.death()&&use.revision()!=s.towerDeathRevision())throw bad();
                if(effect.death())fatal=true;
            }
            if(count!=1)throw bad();
        }
        if((s.towerDeathRevision()>0)!=fatal)throw bad();
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid canonical Time Attack proof");}
    private ModernBattleTimeAttackProof(){}
}
