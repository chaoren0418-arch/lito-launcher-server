package local.lito381.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Retained skill70 battle-NPC arithmetic: NPC MR, zero player armor/elemental
 * resistance, 24+8d15, ignored circle-nine critical roll. This is not the
 * ordinary WORLD weakAttr branch. Admission deliberately retains the existing
 * modern grid LOS policy, not the old LiTo BATTLE radius-only policy. No
 * permission, mana or wave change here. */
final class ModernNpcFireStorm {
    static final String RULESET="MODERN_NPC_FIRE_STORM_V1";
    static final int MAX_TARGETS=48; // radius-three square minus the caster's cell
    record Hit(ModernNpcRoster.Actor before,ModernFireStorm.Outcome outcome) {
        Hit {
            Objects.requireNonNull(before);Objects.requireNonNull(outcome);
            if(!before.alive()||outcome.appliedDamage()>before.hp()
                    ||outcome.remainingHp()!=before.hp()-outcome.appliedDamage())throw bad();
        }
    }
    /** Frozen victim-side batch. Only its owning battle engine can authorize
     * and debit the caster. No caller-supplied damage or target expansion. */
    static final class Incoming {
        private final ModernNpcRoster.Scope scope;
        private final long revision,elapsedMillis,equipmentRevision,evaluatedAtMillis,validUntilMillis;
        private final TournamentPresence.CharacterKey attacker;
        private final ModernWorldMap.Cell origin;
        private final int lane,mpCost;
        private final List<Hit> hits;
        private Incoming(ModernNpcRoster.Snapshot frame,ModernPhysicalCombat.Fighter caster,
                ModernWorldMap.Cell origin,int lane,int mpCost,List<Hit> hits) {
            scope=frame.scope();revision=frame.revision();elapsedMillis=frame.elapsedMillis();
            var p=caster.profile();attacker=caster.key();equipmentRevision=p.equipmentRevision();
            evaluatedAtMillis=p.evaluatedAtMillis();validUntilMillis=p.validUntilMillis();
            this.origin=origin;this.lane=lane;this.mpCost=mpCost;this.hits=List.copyOf(hits);
        }
        private Incoming(Incoming prior,ModernNpcRoster.Snapshot now) {
            scope=prior.scope;revision=now.revision();elapsedMillis=now.elapsedMillis();
            attacker=prior.attacker;equipmentRevision=prior.equipmentRevision;evaluatedAtMillis=prior.evaluatedAtMillis;
            validUntilMillis=prior.validUntilMillis;origin=prior.origin;lane=prior.lane;mpCost=prior.mpCost;hits=prior.hits;
        }
        ModernNpcRoster.Scope scope(){return scope;}
        long revision(){return revision;}
        long equipmentRevision(){return equipmentRevision;}
        long evaluatedAtMillis(){return evaluatedAtMillis;}
        long validUntilMillis(){return validUntilMillis;}
        TournamentPresence.CharacterKey attacker(){return attacker;}
        ModernWorldMap.Cell origin(){return origin;}
        int lane(){return lane;}
        int mpCost(){return mpCost;}
        List<Hit> hits(){return hits;}
        /** Clock-only advances or unrelated spawns do not retarget a captured
         * cast. Any captured victim mutation invalidates the entire batch. */
        Incoming revalidated(ModernNpcRoster.Snapshot now) {
            if(now.closed()||!now.scope().equals(scope)||now.revision()<revision||now.elapsedMillis()<elapsedMillis)throw bad();
            for(var h:hits)if(now.actors().stream().noneMatch(a->a.equals(h.before())))throw bad();
            return new Incoming(this,now);
        }
        @Override public String toString(){return "NpcFireStormIncoming[redacted]";}
    }
    static Incoming prepareIncoming(ModernPhysicalCombat.Fighter caster,ModernNpcCatalog.Catalog catalog,
            ModernNpcRoster.Snapshot frame,ModernWorldMap.Cell origin,int lane,List<ModernNpcRoster.Actor> candidates,
            ModernPhysicalCombat.BoundedRandom random) {
        Objects.requireNonNull(caster);Objects.requireNonNull(catalog);Objects.requireNonNull(frame);
        Objects.requireNonNull(origin);Objects.requireNonNull(random);candidates=List.copyOf(candidates);
        if(frame.closed()||lane<0||lane>1||!ModernWorldProtocol.contains(frame.manifest(),origin)||candidates.size()>MAX_TARGETS)throw bad();
        var profile=ModernFireStorm.qualify(caster);var seen=new HashSet<ModernNpcRoster.Id>();
        var defenses=new ArrayList<ModernFireStorm.Values>();
        // Qualify every target before consuming any random draw.
        for(var a:candidates) {
            if(!a.id().scope().equals(frame.scope())||a.lane()!=lane||!seen.add(a.id())
                    ||a.cell().equals(origin)||distance(origin,a.cell())>ModernFireStorm.RADIUS
                    ||frame.actors().stream().noneMatch(a::equals))throw bad();
            defenses.add(defense(catalog,a));
        }
        var hits=new ArrayList<Hit>();
        for(int i=0;i<candidates.size();i++)hits.add(new Hit(candidates.get(i),
                ModernFireStorm.resolve(profile.values(),defenses.get(i),candidates.get(i).hp(),random,0)));
        hits.sort(java.util.Comparator.comparingLong(h->h.before().id().serial()));
        return new Incoming(frame,caster,origin,lane,profile.mpCost(),hits);
    }
    static ModernFireStorm.Outcome againstNpc(ModernFireStorm.Profile caster,ModernNpcCatalog.Catalog catalog,
            ModernNpcRoster.Actor target,ModernPhysicalCombat.BoundedRandom random) {
        return ModernFireStorm.resolve(caster.values(),defense(catalog,target),target.hp(),random,0);
    }
    /** Retained guardian/start-tower branch has zero armor, MR and elemental resistance. */
    static ModernFireStorm.Outcome againstObjective(ModernFireStorm.Profile caster,long hp,long maximum,ModernPhysicalCombat.BoundedRandom random) {
        var target=new ModernMagicCombat.Values(true,maximum,new ModernMagicCombat.Offense(0,0,1,0,0,0,0),new ModernMagicCombat.Defense(0,0,0,0,0,0,0));
        return ModernFireStorm.resolve(caster.values(),new ModernFireStorm.Values(target,0),hp,random,0);
    }
    private static ModernFireStorm.Values defense(ModernNpcCatalog.Catalog catalog,ModernNpcRoster.Actor actor) {
        Objects.requireNonNull(catalog);Objects.requireNonNull(actor);if(!actor.alive())throw bad();
        var t=actor.template();var n=catalog.require(new ModernNpcCatalog.Key(t.catalogSourceId(),t.catalogDatabase(),t.npcId()));
        n.validateActor(actor);
        if(!n.pendingCleanMelee().isEmpty()||n.get(ModernNpcCatalog.Field.BOSS)!=0)
            throw new ModernFireStorm.CombatException(ModernFireStorm.Failure.UNSUPPORTED);
        long mr=n.get(ModernNpcCatalog.Field.MR);if(mr<0)throw bad();
        return new ModernFireStorm.Values(new ModernMagicCombat.Values(true,t.maximumHp(),
                new ModernMagicCombat.Offense(0,0,1,0,0,0,0),new ModernMagicCombat.Defense(0,0,0,0,0,mr,0)),0);
    }
    static long distance(ModernWorldMap.Cell a,ModernWorldMap.Cell b){return Math.max(Math.abs((long)a.x()-b.x()),Math.abs((long)a.y()-b.y()));}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid NPC fire storm batch");}
    private ModernNpcFireStorm(){}
}
