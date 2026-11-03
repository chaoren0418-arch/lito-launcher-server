package local.lito381.server;

import java.util.HashSet;
import java.util.List;

/** Honor objective/terminal cross-field proof, independent of fixed-price
 * legacy spell/heal frames. The caller binds world identity, roster, clocks,
 * features and owner histories; this is not a complete wire decoder or RNG,
 * source permission, line-of-sight or persistence authority. */
final class ModernBattleHonorProof {
    static void validate(long revision,ModernBattleEngine.Phase phase,ModernWorldMap.Manifest manifest,
            List<ModernBattleObservation.Actor> actors,ModernBattleObservation.Honor honor,ModernMatchSettlement.Event result) {
        var objectives=honor.objectives();if(objectives.size()!=2||actors.size()<2)throw bad();
        var occupied=new HashSet<ModernWorldMap.Cell>();for(var a:actors)occupied.add(a.pose().position());
        boolean initial=phase==ModernBattleEngine.Phase.LOADING||phase==ModernBattleEngine.Phase.COUNTDOWN;
        for(int i=0;i<2;i++) {
            var o=objectives.get(i);
            if(o.entityId()!=actors.size()+i+1L||o.team()!=i||o.lastActorEntityId()>actors.size()
                    ||o.revision()>revision||initial&&o.revision()!=0||!ModernWorldProtocol.contains(manifest,o.cell())||!occupied.add(o.cell()))throw bad();
            if(o.lastActorEntityId()==0)continue;
            var actor=actors.get(Math.toIntExact(o.lastActorEntityId()-1));
            if((o.lastKind()==3)!=(actor.pose().member().team()==o.team()))throw bad();
            if(o.lastKind()==1) {
                var v=actor.vital();
                if(v==null||v.attackRevision()<o.revision()||v.attackRevision()==o.revision()&&v.target()!=o.entityId())throw bad();
            } else {
                var lane=lane(o.lastKind());var receipt=actor.spells().receipts().get(lane);
                if(receipt==null||receipt.impact().use().revision()<o.revision()
                        ||receipt.impact().use().revision()==o.revision()&&receipt.impact().targets().stream().noneMatch(e->is(e,o)))throw bad();
            }
        }
        for(var actor:actors)for(var lane:List.of(ModernUseHistory.Lane.HEALING,ModernUseHistory.Lane.LIGHT_ARROW,ModernUseHistory.Lane.FIRE_STORM)) {
            var r=actor.spells().receipts().get(lane);if(r==null)continue;
            var impact=r.impact();var use=impact.use();var targets=impact.targets();boolean healing=lane==ModernUseHistory.Lane.HEALING;
            if(!actor.pose().member().identity().isHuman()||impact.lane()!=lane
                    ||lane!=ModernUseHistory.Lane.FIRE_STORM&&targets.size()!=1
                    ||healing&&r.detail()!=ModernSpellRuntime.Ordinary.INSTANCE
                    ||lane==ModernUseHistory.Lane.LIGHT_ARROW&&!(r.detail() instanceof ModernSpellRuntime.Arrow)
                    ||lane==ModernUseHistory.Lane.FIRE_STORM&&(!(r.detail() instanceof ModernSpellRuntime.FireStorm f)||f.damageLossAfter().size()!=targets.size()))throw bad();
            var seen=new HashSet<Long>();
            for(int index=0;index<targets.size();index++) {
                var effect=targets.get(index);var health=effect.before();
                if(!(health.target() instanceof ModernTargetHistory.Honor h)) {if(healing)throw bad();continue;}
                var o=objectives.get(h.team());
                if(!seen.add(h.entityId())||!is(effect,o)||!health.cell().equals(o.cell())||health.maximumHp()!=o.maximumHp()
                        ||healing!=(actor.pose().member().team()==o.team())||effect.kind()!=(healing?ModernTargetHistory.Kind.HEAL:ModernTargetHistory.Kind.DAMAGE)
                        ||use.revision()>o.revision()||effect.applied()>(healing?o.healGain():o.damageLoss())
                        ||!ModernWorldProtocol.contains(manifest,new ModernWorldMap.Cell(use.before().x(),use.before().y()))
                        ||!inRange(lane,use.before(),o))throw bad();
                if(use.revision()==o.revision()&&(o.lastActorEntityId()!=actor.pose().entityId()||o.lastKind()!=kind(lane)||o.hp()!=effect.hpAfter()
                        ||!healing&&effect.applied()>0&&o.hurtRevision()!=use.revision()
                        ||effect.death()&&o.deathRevision()!=use.revision()))throw bad();
                if(use.revision()==revision&&(actor.pose().position().x()!=use.before().x()||actor.pose().position().y()!=use.before().y()))throw bad();
                if(r.detail() instanceof ModernSpellRuntime.FireStorm f) {
                    long loss=f.damageLossAfter().get(index);
                    if(loss<effect.applied()||loss>o.damageLoss()||use.revision()==o.revision()&&loss!=o.damageLoss())throw bad();
                }
            }
        }
        var dead=objectives.stream().filter(o->o.hp()==0).toList();
        if(result==null) {if(!dead.isEmpty()||phase==ModernBattleEngine.Phase.FINISHED)throw bad();}
        else {
            if(phase!=ModernBattleEngine.Phase.FINISHED||result.mode()!=5||dead.size()!=1||dead.get(0).deathRevision()!=revision)throw bad();
            int losing=dead.get(0).team();
            for(var p:result.participants())if((p.verdict()==ModernMatchSettlement.Verdict.LOSS)!=(p.team()==losing))throw bad();
        }
    }
    private static boolean is(ModernTargetHistory.Effect effect,ModernHonor.Objective o) {
        return effect.before().target() instanceof ModernTargetHistory.Honor h&&h.entityId()==o.entityId()&&h.team()==o.team();
    }
    private static boolean inRange(ModernUseHistory.Lane lane,ModernUseHistory.Anchor from,ModernHonor.Objective target) {
        if(lane==ModernUseHistory.Lane.HEALING)return ModernHonor.inScreen(from.x(),from.y(),target.x(),target.y());
        long distance=Math.max(Math.abs((long)from.x()-target.x()),Math.abs((long)from.y()-target.y()));
        return distance<=(lane==ModernUseHistory.Lane.FIRE_STORM?ModernFireStorm.RADIUS:10);
    }
    private static ModernUseHistory.Lane lane(int kind) {
        return switch(kind){case 2->ModernUseHistory.Lane.LIGHT_ARROW;case 3->ModernUseHistory.Lane.HEALING;case 4->ModernUseHistory.Lane.FIRE_STORM;default->throw bad();};
    }
    private static int kind(ModernUseHistory.Lane lane){return switch(lane){case LIGHT_ARROW->2;case HEALING->3;case FIRE_STORM->4;default->throw bad();};}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid canonical Honor objective proof");}
    private ModernBattleHonorProof(){}
}
