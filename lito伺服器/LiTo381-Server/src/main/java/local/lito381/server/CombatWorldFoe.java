package local.lito381.server;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Foe cast bindings end on the caller stack; accepted extra damage reuses the value-only magic plan. */
final class CombatWorldFoe {
    private static final String P="local/lito381/server/";
    record Pending(CombatWorldMagic.Attempt binding, BattleEraseMagicStatus.Lease erase) {}

    static Pending capture(ClientSession pc, Object target) {
        if(!CombatRules.loaded() || target==null || !String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD")
                || !(CombatAccess.get(target,"world") instanceof ClassicWorldMonsterPopulation.Snapshot world))return null;
        var binding=CombatWorldMagic.prepare(pc,true,world,null,null,0);
        if(binding==null)return new Pending(null,BattleEraseMagicStatus.Lease.none());
        var a=binding.actor;
        synchronized(a.hub()){synchronized(a.population()){synchronized(a.timed()){
            var erase=a.valid(binding.epoch)?a.hub().captureClassicWorldMonsterEraseMagic(a.map(),a.instance(),a.id(),System.nanoTime()):BattleEraseMagicStatus.Lease.none();
            return new Pending(binding,erase);
        }}}
    }

    static void finish(ClientSession pc,Object target,boolean accepted,Pending pending) throws java.io.IOException {
        if(pending==null){CombatDragon.finishFoe(pc,target,accepted);return;}
        if(!CombatRules.loaded() || !accepted || pending.binding==null)return;
        // The source mode reads its bound and draws only after the five actions and both effects.
        int damage=CombatDragon.extraDamage(maximum->ThreadLocalRandom.current().nextInt(maximum)+1);
        var binding=pending.binding;var a=binding.actor;
        if(CombatPoisonLifecycle.ownerToken(pc,false)!=binding.ownerToken)return;
        synchronized(a.hub()){synchronized(a.population()){synchronized(a.timed()){
            if(!a.valid(binding.epoch)||!binding.owner.valid(pc,a.map(),a.instance()))return;
            if(damage>0)CombatMonsterCurseLifecycle.enqueue(a,binding.owner,binding.epoch,damage,
                    new CombatWorldMultiMagic.Multi(187,pending.erase,damage));
            else a.hub().consumeClassicWorldMonsterEraseMagic(a.map(),a.instance(),a.id(),pending.erase);
        }}}
    }

}
