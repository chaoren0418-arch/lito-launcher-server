package local.lito381.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Output-only <=34 projection. No caller may use these fixed-price DTOs to
 * decide MP, sequence, cooldown, lawful, random damage or victim publication.
 * Repriced successful histories are deliberately not representable: this
 * adapter fails rather than resetting casts or fabricating shadow resources. */
final class ModernLegacySpellProjection {
    record Projection(ModernSpellProtocol.CastState teleport,ModernDamageSpellProtocol.State arrow,
            ModernHonor.Heal healing,ModernFireStormProtocol.State fireStorm,ModernDisintegrateState.State disintegrate,
            ModernNpcMagic.CastState npcArrow,ModernNpcFireStormState.State npcFireStorm,ModernNpcDisintegrateState.State npcDisintegrate) {}
    static Projection from(long entity,int team,int castIntervalMillis,ModernSpellRuntime.State runtime,
            Map<ModernUseHistory.Lane,ModernUseHistory.Spell> histories) {
        if(runtime!=null&&runtime.playerHealingEnabled()||histories.containsKey(ModernUseHistory.Lane.SLOW)||histories.containsKey(ModernUseHistory.Lane.GREATER_HASTE)||histories.containsKey(ModernUseHistory.Lane.HASTE))throw bad();
        if(entity<1||runtime!=null&&(runtime.identity().actor().entityId()!=entity||!runtime.lanes().equals(histories.keySet())))throw bad();
        if(runtime==null&&histories.values().stream().anyMatch(s->s.last()!=null))throw bad();
        return new Projection(teleport(entity,castIntervalMillis,runtime,histories),arrow(entity,runtime,histories),healing(entity,runtime,histories),
                fire(entity,runtime,histories),disintegrate(entity,runtime,histories),npcArrow(entity,team,runtime,histories),
                npcFire(entity,team,runtime,histories),npcDisintegrate(entity,team,runtime,histories));
    }
    private static ModernUseHistory.Spell spell(Map<ModernUseHistory.Lane,ModernUseHistory.Spell> histories,ModernUseHistory.Lane lane) {
        var spell=histories.get(lane);if(spell==null)return null;
        var l=spell.current();
        if(l.priorCasts()!=0||l.priorSpent()!=0||l.spent()!=Math.multiplyExact(l.casts(),l.cost())
                ||spell.last()!=null&&spell.last().cost()!=l.cost())throw bad();
        return spell;
    }
    private static ModernSpellRuntime.Receipt receipt(ModernSpellRuntime.State runtime,ModernUseHistory.Lane lane,ModernUseHistory.Spell spell) {
        var r=runtime==null?null:runtime.receipts().get(lane);
        if(spell.last()==null) {if(r!=null)throw bad();return null;}
        if(r==null||!r.batch().use().equals(spell.last())||r.casts()!=spell.current().casts()||r.spent()!=spell.current().spent())throw bad();return r;
    }
    private static ModernSpellProtocol.CastState teleport(long entity,int interval,ModernSpellRuntime.State runtime,Map<ModernUseHistory.Lane,ModernUseHistory.Spell> histories) {
        var lane=ModernUseHistory.Lane.TELEPORT;var spell=spell(histories,lane);if(spell==null)return null;
        if(spell.current().cost()!=10)throw bad();var r=receipt(runtime,lane,spell);long sequence=runtime==null?0:runtime.sequence();
        if(r==null)return new ModernSpellProtocol.CastState(entity,sequence,0,0,0,0,0,0,0,0,interval);
        var use=r.batch().use();var from=use.before();var to=((ModernSpellRuntime.Teleport)r.detail()).destination();
        return new ModernSpellProtocol.CastState(entity,sequence,r.casts(),use.revision(),r.spent(),5,from.x(),from.y(),to.x(),to.y(),interval);
    }
    private static ModernDamageSpellProtocol.State arrow(long entity,ModernSpellRuntime.State runtime,Map<ModernUseHistory.Lane,ModernUseHistory.Spell> histories) {
        var lane=ModernUseHistory.Lane.LIGHT_ARROW;var spell=spell(histories,lane);if(spell==null)return null;
        var r=receipt(runtime,lane,spell);int cost=spell.current().cost();if(r==null)return ModernDamageSpellProtocol.State.initial(entity,cost);
        var use=r.batch().use();var target=r.batch().targets().get(0).before();
        return new ModernDamageSpellProtocol.State(entity,r.casts(),use.revision(),r.spent(),4,entity(target.target()),
                use.before().x(),use.before().y(),target.cell().x(),target.cell().y(),cost);
    }
    private static ModernHonor.Heal healing(long entity,ModernSpellRuntime.State runtime,Map<ModernUseHistory.Lane,ModernUseHistory.Spell> histories) {
        var lane=ModernUseHistory.Lane.HEALING;var spell=spell(histories,lane);if(spell==null)return null;
        var r=receipt(runtime,lane,spell);int cost=spell.current().cost();if(r==null)return ModernHonor.Heal.initial(entity,cost);
        var use=r.batch().use();var hit=r.batch().targets().get(0);var target=hit.before();
        return new ModernHonor.Heal(entity,r.casts(),use.revision(),r.spent(),entity(target.target()),target.hp(),hit.hpAfter(),hit.applied(),
                use.before().x(),use.before().y(),target.cell().x(),target.cell().y(),1,cost);
    }
    private static ModernFireStormProtocol.State fire(long entity,ModernSpellRuntime.State runtime,Map<ModernUseHistory.Lane,ModernUseHistory.Spell> histories) {
        var lane=ModernUseHistory.Lane.FIRE_STORM;var spell=spell(histories,lane);if(spell==null)return null;
        var r=receipt(runtime,lane,spell);int cost=spell.current().cost();if(r==null)return ModernFireStormProtocol.State.initial(entity,cost);
        var use=r.batch().use();var detail=(ModernSpellRuntime.FireStorm)r.detail();var targets=new ArrayList<ModernFireStormProtocol.Target>();
        for(int i=0;i<r.batch().targets().size();i++) {
            var hit=r.batch().targets().get(i);var h=hit.before();
            targets.add(new ModernFireStormProtocol.Target(entity(h.target()),h.hp(),hit.hpAfter(),hit.applied(),detail.damageLossAfter().get(i),
                    h.life()-1,h.cell().x(),h.cell().y()));
        }
        return new ModernFireStormProtocol.State(entity,r.casts(),use.revision(),r.spent(),use.sequence(),use.before().x(),use.before().y(),cost,ModernFireStorm.REUSE_MILLIS,targets);
    }
    private static ModernDisintegrateState.State disintegrate(long entity,ModernSpellRuntime.State runtime,Map<ModernUseHistory.Lane,ModernUseHistory.Spell> histories) {
        var lane=ModernUseHistory.Lane.DISINTEGRATE;var spell=spell(histories,lane);if(spell==null)return null;
        var r=receipt(runtime,lane,spell);int cost=spell.current().cost();long initial=runtime==null?0:runtime.initialLawful();
        if(r==null)return ModernDisintegrateState.State.initial(entity,cost,initial);
        var use=r.batch().use();var hit=r.batch().targets().get(0);var h=hit.before();var detail=(ModernSpellRuntime.Disintegrate)r.detail();
        return new ModernDisintegrateState.State(entity,r.casts(),use.revision(),r.spent(),use.sequence(),initial,detail.lawfulAfter(),cost,ModernDisintegrate.REUSE_MILLIS,
                new ModernDisintegrateState.Hit(entity(h.target()),use.before().life(),h.life(),use.before().x(),use.before().y(),h.cell().x(),h.cell().y(),
                        h.hp(),hit.hpAfter(),hit.raw(),hit.applied(),detail.damageLossAfter(),detail.critical()));
    }
    private static ModernNpcMagic.CastState npcArrow(long entity,int team,ModernSpellRuntime.State runtime,Map<ModernUseHistory.Lane,ModernUseHistory.Spell> histories) {
        var lane=ModernUseHistory.Lane.NPC_LIGHT_ARROW;var spell=spell(histories,lane);if(spell==null)return null;
        var r=receipt(runtime,lane,spell);int cost=spell.current().cost();if(r==null)return ModernNpcMagic.CastState.initial(entity,cost);
        var use=r.batch().use();var hit=r.batch().targets().get(0);var h=hit.before();
        return new ModernNpcMagic.CastState(entity,use.sequence(),r.casts(),use.revision(),r.spent(),cost,use.before().life(),((ModernTargetHistory.Npc)h.target()).id(),
                h.life(),team,origin(use),h.cell(),new ModernMagicCombat.Outcome(hit.raw(),hit.applied(),hit.hpAfter(),hit.hpAfter()==0,((ModernSpellRuntime.Arrow)r.detail()).critical()));
    }
    private static ModernNpcDisintegrateState.State npcDisintegrate(long entity,int team,ModernSpellRuntime.State runtime,Map<ModernUseHistory.Lane,ModernUseHistory.Spell> histories) {
        var lane=ModernUseHistory.Lane.NPC_DISINTEGRATE;var spell=spell(histories,lane);if(spell==null)return null;
        var r=receipt(runtime,lane,spell);int cost=spell.current().cost();long initial=runtime==null?0:runtime.initialLawful();
        if(r==null)return ModernNpcDisintegrateState.State.initial(entity,cost,initial);
        var use=r.batch().use();var hit=r.batch().targets().get(0);var h=hit.before();var detail=(ModernSpellRuntime.Disintegrate)r.detail();
        return new ModernNpcDisintegrateState.State(entity,r.casts(),use.revision(),use.sequence(),r.spent(),cost,initial,detail.lawfulAfter(),
                new ModernNpcDisintegrateState.Hit(((ModernTargetHistory.Npc)h.target()).id(),h.life(),team,use.before().life(),origin(use),h.cell(),h.hp(),
                        new ModernDisintegrate.Outcome(hit.raw(),hit.applied(),hit.hpAfter(),hit.hpAfter()==0,detail.critical())));
    }
    private static ModernNpcFireStormState.State npcFire(long entity,int team,ModernSpellRuntime.State runtime,Map<ModernUseHistory.Lane,ModernUseHistory.Spell> histories) {
        var lane=ModernUseHistory.Lane.NPC_FIRE_STORM;var spell=spell(histories,lane);if(spell==null)return null;
        var r=receipt(runtime,lane,spell);int cost=spell.current().cost();if(r==null)return ModernNpcFireStormState.State.initial(entity,cost);
        var use=r.batch().use();var w=r.batch().world();var scope=new ModernNpcRoster.Scope(w.roomId(),w.matchId(),w.generation());
        var targets=new ArrayList<ModernNpcFireStormState.Target>();
        for(var hit:r.batch().targets())if(hit.before().target() instanceof ModernTargetHistory.Npc npc) {
            var h=hit.before();targets.add(new ModernNpcFireStormState.Target(npc.id(),h.life(),team,h.cell(),h.hp(),fireOutcome(hit)));
        }
        ModernNpcFireStormStructures.Receipt retained=null;var structure=runtime.structure();
        if(structure!=null) {
            var batch=structure.batch();var effect=batch.targets().get(batch.targets().size()-1);var h=effect.before();
            ModernNpcFireStormStructures.Hit hit=h.target() instanceof ModernTargetHistory.Quest q
                    ?new ModernNpcFireStormStructures.QuestHit(q.id(),structure.questKind(),h.cell(),h.maximumHp(),h.hp(),q.bornRevision(),fireOutcome(effect))
                    :new ModernNpcFireStormStructures.TowerHit(((ModernTargetHistory.TimeTower)h.target()).id(),h.cell(),h.maximumHp(),h.hp(),fireOutcome(effect));
            var used=batch.use();retained=new ModernNpcFireStormStructures.Receipt(structure.casts(),used.revision(),used.sequence(),used.before().life(),origin(used),hit);
        }
        return new ModernNpcFireStormState.State(entity,r.casts(),use.revision(),use.sequence(),r.spent(),cost,use.before().life(),scope,origin(use),team,targets,retained);
    }
    private static ModernFireStorm.Outcome fireOutcome(ModernTargetHistory.Effect hit){return new ModernFireStorm.Outcome(hit.raw(),hit.applied(),hit.hpAfter(),hit.hpAfter()==0);}
    private static ModernWorldMap.Cell origin(ModernUseHistory.Cast use){return new ModernWorldMap.Cell(use.before().x(),use.before().y());}
    private static long entity(ModernTargetHistory.Target target) {
        if(target instanceof ModernTargetHistory.Participant p)return p.entityId();
        if(target instanceof ModernTargetHistory.Honor h)return h.entityId();throw bad();
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("history cannot be projected to legacy spell packets");}
    private ModernLegacySpellProjection(){}
}
