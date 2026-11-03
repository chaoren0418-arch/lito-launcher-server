package local.lito381.server;

import java.util.Objects;

/**
 * Qualified ordinary Light Arrow against a catalog-bound NPC, not PC defense.
 * Retained ClientSession's ordinary battle-magic branch supplies NPC MR and
 * zero player armor reduction; skill 4's attr16 matches no NPC weakAttr.
 * Reuses the modern exact-i64 attacker/MR arithmetic, not native int/double
 * saturation. No learned-skill grant, MP debit, geometry/pace permission,
 * status effect, boss behavior or network admission is implied by this layer.
 */
final class ModernNpcMagic {
    static final String RULESET = "MODERN_NPC_LIGHT_ARROW_V1";

    enum Reason { NONE, NOT_ACTIVE, DEAD, UNLEARNED, UNSUPPORTED, TOO_FAST, NO_MP,
        TARGET_DEAD, WRONG_LANE, OUT_OF_RANGE, BLOCKED, TOO_HEAVY, NUMERIC, STALE_STATE, STALE_REGEN }
    /** Engine ledger; v24 translates its revision without aliasing PvP targets. */
    record CastState(long entityId,long requestSequence,long castCount,long revision,long mpSpent,long mpCost,
                     long casterLife,ModernNpcRoster.Id target,long targetLife,int targetLane,
                     ModernWorldMap.Cell from,ModernWorldMap.Cell to,ModernMagicCombat.Outcome outcome) {
        CastState {
            if(entityId<1||requestSequence<castCount||castCount<0||revision<0||mpSpent<0||mpCost<0||mpCost>3)
                throw new IllegalArgumentException("invalid NPC magic ledger");
            if(castCount==0) {
                if(requestSequence!=0||revision!=0||mpSpent!=0||casterLife!=0||target!=null||targetLife!=0
                        ||targetLane!=-1||from!=null||to!=null||outcome!=null)throw new IllegalArgumentException("invalid inert NPC magic ledger");
            } else if(mpCost<1||revision<1||casterLife<1||target==null||targetLife<1||targetLane<0||targetLane>1
                    ||from==null||to==null||from.equals(to)||outcome==null||mpSpent!=Math.multiplyExact(castCount,mpCost))
                throw new IllegalArgumentException("invalid NPC magic receipt");
        }
        static CastState initial(long entityId,long mpCost){return new CastState(entityId,0,0,0,0,mpCost,0,null,0,-1,null,null,null);}
        CastState wireRevision(){return new CastState(entityId,requestSequence,castCount,revision==0?0:Math.incrementExact(revision),mpSpent,mpCost,casterLife,target,targetLife,targetLane,from,to,outcome);}
        CastState committed(long sequence,long revision,long life,ModernNpcRoster.Actor target,
                            ModernWorldMap.Cell from,ModernMagicCombat.Outcome outcome) {
            var spending=ModernSkillSpending.advance(castCount,mpSpent,Math.toIntExact(mpCost));
            return new CastState(entityId,sequence,spending.casts(),revision,spending.spent(),mpCost,
                    life,target.id(),target.life(),target.lane(),from,target.cell(),outcome);
        }
    }
    record Result(Reason reason,ModernBattleObservation.Capture observation) implements ModernBattleCommand {
        Result {Objects.requireNonNull(reason);Objects.requireNonNull(observation);}
        ModernNpcBattle.Frame frame(){return ModernLegacyBattleProjection.npcs(observation);}
    }

    /** Immutable victim-side plan. Only the match owner may commit it together
     * with its own validated caster life, scope, equipment, MP and cooldown. */
    static final class Incoming {
        private final ModernNpcRoster.Scope scope;
        private final long revision;
        private final ModernNpcRoster.Actor before;
        private final ModernNpcCatalog.Key catalogKey;
        private final TournamentPresence.CharacterKey attacker;
        private final long equipmentRevision, evaluatedAtMillis, validUntilMillis, mpCost;
        private final ModernMagicCombat.Outcome outcome;
        private Incoming(ModernNpcRoster.Snapshot frame, ModernNpcRoster.Actor before,
                         ModernMagicCombat.Fighter caster, ModernMagicCombat.Outcome outcome) {
            this.scope=frame.scope();this.revision=frame.revision();this.before=before;
            var t=before.template();catalogKey=new ModernNpcCatalog.Key(t.catalogSourceId(),t.catalogDatabase(),t.npcId());
            attacker=caster.key();equipmentRevision=caster.profile().equipmentRevision();
            evaluatedAtMillis=caster.profile().evaluatedAtMillis();validUntilMillis=caster.profile().validUntilMillis();
            mpCost=caster.mpCost();this.outcome=outcome;
        }
        ModernNpcRoster.Scope scope(){return scope;}
        long revision(){return revision;}
        ModernNpcRoster.Actor before(){return before;}
        ModernNpcCatalog.Key catalogKey(){return catalogKey;}
        TournamentPresence.CharacterKey attacker(){return attacker;}
        long equipmentRevision(){return equipmentRevision;}
        long evaluatedAtMillis(){return evaluatedAtMillis;}
        long validUntilMillis(){return validUntilMillis;}
        long mpCost(){return mpCost;}
        ModernMagicCombat.Outcome outcome(){return outcome;}
        @Override public String toString(){return "NpcMagicIncoming[redacted]";}
    }

    static Incoming prepareIncoming(ModernMagicCombat.Fighter caster, ModernNpcCatalog.Catalog catalog,
                                    ModernNpcRoster.Snapshot frame, ModernNpcRoster.Id id, long life,
                                    ModernPhysicalCombat.BoundedRandom random) {
        Objects.requireNonNull(frame);Objects.requireNonNull(id);
        if(frame.closed()||!frame.scope().equals(id.scope())||life<1)throw invalid();
        var before=frame.actors().stream().filter(a->a.id().equals(id)).findFirst().orElseThrow(ModernNpcMagic::invalid);
        if(before.life()!=life)throw invalid();
        return new Incoming(frame,before,caster,againstNpc(caster,catalog,before,random));
    }

    static ModernMagicCombat.Outcome againstNpc(ModernMagicCombat.Fighter caster, ModernNpcCatalog.Catalog catalog,
                                               ModernNpcRoster.Actor defender, ModernPhysicalCombat.BoundedRandom random) {
        Objects.requireNonNull(caster);Objects.requireNonNull(catalog);Objects.requireNonNull(defender);Objects.requireNonNull(random);
        if(!caster.supported())throw new ModernMagicCombat.CombatException(ModernMagicCombat.Failure.UNSUPPORTED);
        if(!defender.alive())throw invalid();
        var t=defender.template();ModernNpcCatalog.Npc npc;
        try {
            npc=catalog.require(new ModernNpcCatalog.Key(t.catalogSourceId(),t.catalogDatabase(),t.npcId()));
            npc.validateActor(defender);
        }catch(IllegalArgumentException failure){throw invalid();}
        if(!npc.pendingCleanMelee().isEmpty()||npc.get(ModernNpcCatalog.Field.BOSS)!=0)
            throw new ModernMagicCombat.CombatException(ModernMagicCombat.Failure.UNSUPPORTED);
        long mr=npc.get(ModernNpcCatalog.Field.MR);
        // Retained ordinaryMagicDamage and Erase Magic both require nonnegative
        // MR. Do not turn an unsupported negative catalog value into zero.
        if(mr<0)throw invalid();
        var defense=new ModernMagicCombat.Values(true,t.maximumHp(),
                new ModernMagicCombat.Offense(0,0,1,0,0,0,0),
                new ModernMagicCombat.Defense(0,0,0,0,0,mr,0));
        // Numeric defense only: this is not a fabricated player or BOT. NPC AC,
        // hard skin, damage_reduction, size and undead material bonuses belong
        // to the physical branch, not this retained attr16 magic branch.
        return ModernMagicCombat.resolveValues(caster.values(),defense,defender.hp(),random);
    }
    private static ModernMagicCombat.CombatException invalid(){
        return new ModernMagicCombat.CombatException(ModernMagicCombat.Failure.INVALID_STATE);
    }
    private ModernNpcMagic(){}
}
