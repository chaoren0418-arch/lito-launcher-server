package local.lito381.server;

import java.util.Objects;

/** Skill77 against a catalog-bound NPC, never a participant entity alias.
 * The retained battle branch uses the NPC's MR, neutral resistance and zero
 * player armor reduction. Source dice, critical and exact-i64 arithmetic are
 * shared with ModernDisintegrate, not approximated with the skill4 calculator.
 * This is preparation/victim commit only. A match transaction must still bind
 * learned source, caster life, lawful, MP, geometry, cooldown and publication.
 */
final class ModernNpcDisintegrate {
    static final String RULESET = "MODERN_NPC_DISINTEGRATE_V1";

    /** Immutable victim-side plan. It cannot debit MP or advance lawful and is
     * not a client request or authorization token. All identity/proof fields
     * come from the qualified source and generation-frozen NPC catalog.
     */
    static final class Incoming {
        private final ModernNpcRoster.Scope scope;
        private final long revision;
        private final ModernNpcRoster.Actor before;
        private final ModernNpcCatalog.Key catalogKey;
        private final TournamentPresence.CharacterKey attacker;
        private final long equipmentRevision, evaluatedAtMillis, validUntilMillis;
        private final int mpCost;
        private final ModernDisintegrate.Outcome outcome;

        private Incoming(ModernNpcRoster.Snapshot frame, ModernNpcRoster.Actor before,
                         ModernPhysicalCombat.Fighter caster, ModernDisintegrate.Profile profile,
                         ModernDisintegrate.Outcome outcome) {
            scope=frame.scope(); revision=frame.revision(); this.before=before;
            var t=before.template();
            catalogKey=new ModernNpcCatalog.Key(t.catalogSourceId(),t.catalogDatabase(),t.npcId());
            attacker=profile.key(); equipmentRevision=caster.profile().equipmentRevision();
            evaluatedAtMillis=caster.profile().evaluatedAtMillis(); validUntilMillis=caster.profile().validUntilMillis();
            mpCost=profile.mpCost(); this.outcome=outcome;
        }
        ModernNpcRoster.Scope scope(){return scope;}
        long revision(){return revision;}
        ModernNpcRoster.Actor before(){return before;}
        ModernNpcCatalog.Key catalogKey(){return catalogKey;}
        TournamentPresence.CharacterKey attacker(){return attacker;}
        long equipmentRevision(){return equipmentRevision;}
        long evaluatedAtMillis(){return evaluatedAtMillis;}
        long validUntilMillis(){return validUntilMillis;}
        int mpCost(){return mpCost;}
        ModernDisintegrate.Outcome outcome(){return outcome;}
        @Override public String toString(){return "NpcDisintegrateIncoming[redacted]";}
    }

    static Incoming prepareIncoming(ModernPhysicalCombat.Fighter caster, ModernNpcCatalog.Catalog catalog,
                                    ModernNpcRoster.Snapshot frame, ModernNpcRoster.Id id, long life,
                                    ModernPhysicalCombat.BoundedRandom random, int timedSpellPower) {
        Objects.requireNonNull(caster); Objects.requireNonNull(frame); Objects.requireNonNull(id);
        if(frame.closed()||!frame.scope().equals(id.scope())||life<1)throw invalid();
        var before=frame.actors().stream().filter(a->a.id().equals(id)).findFirst().orElseThrow(ModernNpcDisintegrate::invalid);
        if(before.life()!=life)throw invalid();
        var profile=ModernDisintegrate.qualify(caster);
        return new Incoming(frame,before,caster,profile,againstNpc(profile,catalog,before,random,timedSpellPower));
    }

    static ModernDisintegrate.Outcome againstNpc(ModernDisintegrate.Profile caster, ModernNpcCatalog.Catalog catalog,
                                                ModernNpcRoster.Actor defender, ModernPhysicalCombat.BoundedRandom random,
                                                int timedSpellPower) {
        Objects.requireNonNull(caster); Objects.requireNonNull(catalog); Objects.requireNonNull(defender); Objects.requireNonNull(random);
        if(!caster.values().supported())throw new ModernDisintegrate.CombatException(ModernDisintegrate.Failure.UNSUPPORTED);
        if(!defender.alive())throw invalid();
        var t=defender.template(); ModernNpcCatalog.Npc npc;
        try {
            npc=catalog.require(new ModernNpcCatalog.Key(t.catalogSourceId(),t.catalogDatabase(),t.npcId()));
            npc.validateActor(defender);
        }catch(IllegalArgumentException failure){throw invalid();}
        if(!npc.pendingCleanMelee().isEmpty()||npc.get(ModernNpcCatalog.Field.BOSS)!=0)
            throw new ModernDisintegrate.CombatException(ModernDisintegrate.Failure.UNSUPPORTED);
        long mr=npc.get(ModernNpcCatalog.Field.MR);
        if(mr<0)throw invalid();
        // NPC AC, hard skin, size, undead material and physical reduction do
        // not become player armor/class/WIS terms in the neutral magic branch.
        var defense=new ModernMagicCombat.Values(true,t.maximumHp(),
                new ModernMagicCombat.Offense(0,0,1,0,0,0,0),
                new ModernMagicCombat.Defense(0,0,0,0,0,mr,0));
        return ModernDisintegrate.resolve(caster.values(),defense,defender.hp(),random,timedSpellPower);
    }
    private static ModernDisintegrate.CombatException invalid(){
        return new ModernDisintegrate.CombatException(ModernDisintegrate.Failure.INVALID_STATE);
    }
    private ModernNpcDisintegrate(){}
}
