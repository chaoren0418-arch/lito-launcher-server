package local.lito381.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** Cross-section authority checks. A payload receipt alone never proves MP,
 * caster life, world ownership, target HP or mode completion. */
final class ModernNpcFireStormFrame {
    static long eventRevision(ModernWorldProtocol.Frame w) {
        return w.timeAttack()!=null&&w.timeAttack().receipt()!=null?Math.incrementExact(w.timeAttack().receipt().revision()):
                w.completion()!=null?Math.incrementExact(w.completion().event().engineRevision()):w.revision();
    }
    static void validateCombat(ModernWorldProtocol.Frame w,List<ModernCombatProtocol.Vital> vitals,List<ModernItemProtocol.State> items,
            List<ModernRegenProtocol.State> regen,List<ModernNpcMagic.CastState> arrows,List<ModernNpcDisintegrateState.State> disintegrations,
            List<ModernNpcFireStormState.State> states) {
        if(states.isEmpty())return;int n=w.actors().size();
        if(w.descriptor()==null||!w.descriptor().has(ModernMatchDescriptor.NPC_FIRE_STORM|ModernMatchDescriptor.NPC|ModernMatchDescriptor.SPELLS|ModernMatchDescriptor.REGEN)
                ||states.size()!=n||vitals.size()!=n||items.size()!=n||regen.size()!=n||w.casts().size()!=n
                ||!arrows.isEmpty()&&arrows.size()!=n||!disintegrations.isEmpty()&&disintegrations.size()!=n
                ||w.room().summary().config().mode()!=(w.timeAttack()==null?2:4))throw bad();
        var occupied=new HashSet<Long>();var owners=new HashMap<Long,Long>();
        for(var c:w.casts())occupied.add(c.castRevision());for(var a:arrows)occupied.add(a.revision());for(var d:disintegrations)occupied.add(d.revision());
        for(var i:items)occupied.add(i.useRevision());for(var v:vitals){occupied.add(v.attackRevision());occupied.add(v.hurtRevision());occupied.add(v.deathRevision());}
        for(var r:regen){occupied.add(r.hpRevision());occupied.add(r.mpRevision());}
        for(var l:w.quest().lives()){occupied.add(l.deathRevision());occupied.add(l.respawnRevision());}
        for(var e:w.quest().exchanges())occupied.add(e.revision());if(w.timeAttack()!=null)for(var e:w.timeAttack().exchanges())occupied.add(Math.incrementExact(e.revision()));
        long end=eventRevision(w);var scope=new ModernNpcRoster.Scope(w.room().summary().id(),w.matchId(),w.generation());
        for(int i=0;i<n;i++) {
            var s=states.get(i);var a=w.actors().get(i);var c=w.casts().get(i);var v=vitals.get(i);
            long total=Math.addExact(Math.addExact(c.castCount(),s.castCount()),Math.addExact(arrows.isEmpty()?0:arrows.get(i).castCount(),disintegrations.isEmpty()?0:disintegrations.get(i).castCount()));
            if(s.entityId()!=a.entityId()||s.revision()>end||s.lastSequence()>c.requestSequence()||total>c.requestSequence()
                    ||(a.member().identity().isHuman()?s.mpCost()==0:!s.equals(ModernNpcFireStormState.State.initial(s.entityId(),0)))
                    ||(w.phase()==ModernWorldProtocol.Phase.LOADING||w.phase()==ModernWorldProtocol.Phase.COUNTDOWN)&&s.castCount()!=0)throw bad();
            if(s.castCount()==0)continue;
            if(!s.scope().equals(scope)||s.lane()!=a.member().team()||occupied.contains(s.revision())||owners.putIfAbsent(s.revision(),s.entityId())!=null
                    ||!ModernWorldProtocol.contains(w.manifest(),s.origin())||s.targets().stream().anyMatch(t->!ModernWorldProtocol.contains(w.manifest(),t.cell())))throw bad();
            caster(w,a,v,c,s.revision(),s.lastSequence(),s.casterLife(),s.origin(),end);
        }
        for(int i=0;i<n;i++) {
            var s=states.get(i);var e=s.structure();if(e==null)continue;var h=e.hit();var other=owners.putIfAbsent(e.revision(),s.entityId());
            if(other!=null&&other!=s.entityId()||occupied.contains(e.revision())||!h.scope().equals(scope)
                    ||!ModernWorldProtocol.contains(w.manifest(),e.origin())||!ModernWorldProtocol.contains(w.manifest(),h.cell()))throw bad();
            caster(w,w.actors().get(i),vitals.get(i),w.casts().get(i),e.revision(),e.sequence(),e.casterLife(),e.origin(),end);
            if(h instanceof ModernNpcFireStormStructures.QuestHit q)quest(w,e,q);else tower(w,e,(ModernNpcFireStormStructures.TowerHit)h);
        }
        // Both the latest cast and retained structure receipt own an authority
        // revision. History must not borrow another objective's HP event.
        for(var a:w.quest().actors())for(long revision:new long[]{a.hurtRevision(),a.deathRevision()}) {
            if(revision==0||!owners.containsKey(revision))continue;
            var s=states.stream().filter(v->v.entityId()==owners.get(revision)).findFirst().orElseThrow(ModernNpcFireStormFrame::bad);
            if(s.structure()==null||s.structure().revision()!=revision||!(s.structure().hit() instanceof ModernNpcFireStormStructures.QuestHit q)||q.id().slot()!=a.slot())throw bad();
        }
        var t=w.timeAttack();if(t!=null)for(var s:states)if(s.castCount()>0&&(s.revision()==wire(t.towerHurtRevision())||s.revision()==wire(t.towerDeathRevision()))
                &&(s.structure()==null||s.structure().revision()!=s.revision()||!(s.structure().hit() instanceof ModernNpcFireStormStructures.TowerHit)))throw bad();
        if(w.quest().winningLane()>=0&&(w.audienceLane()<0||w.audienceLane()==w.quest().winningLane())) {
            int slot=w.quest().winningLane()*4+4;long death=w.quest().deathRevision();
            if(w.quest().exchanges().stream().noneMatch(e->e.slot()==slot&&e.revision()==death&&e.outcome().death())
                    &&states.stream().map(ModernNpcFireStormState.State::structure).filter(java.util.Objects::nonNull).noneMatch(e->e.revision()==death
                        &&e.hit() instanceof ModernNpcFireStormStructures.QuestHit q&&q.id().slot()==slot&&q.outcome().death()))throw bad();
        }
        if(t!=null&&t.towerDeathRevision()>0&&t.exchanges().stream().noneMatch(e->e.revision()==t.towerDeathRevision()&&e.outcome().death())
                &&states.stream().map(ModernNpcFireStormState.State::structure).filter(java.util.Objects::nonNull).noneMatch(e->e.revision()==wire(t.towerDeathRevision())
                    &&e.hit() instanceof ModernNpcFireStormStructures.TowerHit h&&h.outcome().death()))throw bad();
    }
    private static void caster(ModernWorldProtocol.Frame w,ModernBattleEngine.Actor a,ModernCombatProtocol.Vital v,ModernSpellProtocol.CastState c,
            long revision,long sequence,long life,ModernWorldMap.Cell origin,long end) {
        var state=w.quest().life(a.entityId());long current=state==null?1:state.incarnation();
        if(!a.member().identity().isHuman()||life>current||state!=null&&(life==current?revision<=state.respawnRevision():revision>state.respawnRevision())
                ||life==current&&v.hp()==0&&revision>=v.deathRevision()
                ||revision==end&&(v.hp()==0||sequence!=c.requestSequence()||life!=current||!a.position().equals(origin)
                    ||w.timeAttack()!=null&&w.timeAttack().departed(a.entityId())))throw bad();
    }
    private static void quest(ModernWorldProtocol.Frame w,ModernNpcFireStormStructures.Receipt e,ModernNpcFireStormStructures.QuestHit h) {
        var q=w.quest();if(!q.enabled()||w.timeAttack()!=null)throw bad();var target=q.target(h.id().slot());var actor=q.actor(h.id().slot());
        var current=q.actors().stream().filter(a->a.lane()==h.lane()).findFirst().orElse(null);
        var kind=q.flow()==ModernQuestProgress.Flow.FINAL_GUARDIAN?ModernQuestObjectives.Kind.FINAL_GUARDIAN:
                h.id().stage()==4?ModernQuestObjectives.Kind.FINAL_SYMBOL:ModernQuestObjectives.Kind.ROOM_SYMBOL;
        if(h.kind()!=kind||!h.cell().equals(target.cell())||h.maximumHp()!=target.maximumHp()||h.id().stage()>q.lane(h.lane()).stage()
                ||actor==null&&h.id().stage()==q.lane(h.lane()).stage()
                ||current!=null&&h.id().stage()<current.stage()&&e.revision()>=current.bornRevision()
                ||actor!=null&&(h.bornRevision()!=actor.bornRevision()||e.revision()>actor.revision()||h.outcome().remainingHp()<actor.hp()
                    ||h.outcome().death()&&actor.deathRevision()!=e.revision()
                    ||e.revision()==actor.revision()&&(actor.hp()!=h.outcome().remainingHp()||h.outcome().appliedDamage()>0&&actor.hurtRevision()!=e.revision())))throw bad();
    }
    private static void tower(ModernWorldProtocol.Frame w,ModernNpcFireStormStructures.Receipt e,ModernNpcFireStormStructures.TowerHit h) {
        var t=w.timeAttack();if(t==null||w.quest().enabled())throw bad();
        if(!h.cell().equals(t.tower().cell())||h.maximumHp()!=t.tower().maximumHp()||e.revision()>wire(t.revision())
                ||h.outcome().remainingHp()<t.towerHp()||h.outcome().death()&&wire(t.towerDeathRevision())!=e.revision()
                ||e.revision()==wire(t.towerHurtRevision())&&h.outcome().remainingHp()!=t.towerHp()
                ||e.revision()==wire(t.revision())&&(h.outcome().remainingHp()!=t.towerHp()||h.outcome().appliedDamage()>0&&wire(t.towerHurtRevision())!=e.revision()))throw bad();
    }
    static void validateNpcs(ModernCombatProtocol.Frame combat,ModernNpcRoster.Scope scope,long spawned,List<ModernNpcProtocol.Actor> actors,List<ModernNpcBattle.Exchange> exchanges) {
        long end=eventRevision(combat.world());
        for(var s:combat.npcFireStorms()) {
            if(exchanges.stream().anyMatch(e->e.revision()==s.revision()||s.structure()!=null&&e.revision()==s.structure().revision()))throw bad();
            for(var h:s.targets()) {
                if(!h.id().scope().equals(scope)||h.id().serial()>spawned)throw bad();
                var target=actors.stream().filter(n->n.serial()==h.id().serial()).findFirst().orElse(null);
                if(target!=null&&(target.lane()!=h.lane()||target.life()<h.life()||h.hpBefore()>target.maxHp()
                        ||target.life()==h.life()&&h.outcome().remainingHp()<target.hp()))throw bad();
                if(s.revision()==end&&(target==null||target.life()!=h.life()||!target.cell().equals(h.cell())||target.hp()!=h.outcome().remainingHp()
                        ||h.outcome().appliedDamage()>0&&target.action()!=(h.outcome().death()?ModernNpcRoster.Action.DEATH:ModernNpcRoster.Action.HURT)))throw bad();
            }
        }
    }
    private static long wire(long revision){return revision==0?0:Math.incrementExact(revision);}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid NPC fire storm atomic frame");}
    private ModernNpcFireStormFrame(){}
}
