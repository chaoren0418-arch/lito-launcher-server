package local.lito381.server;

import java.util.Objects;

/** Explicit Modern VIP numeric policy, NOT a claim that the current classic
 * executor is correct: its DMG_REDUX dispatch calls the melee-damage adder and
 * its magic-only accumulator has no damage consumer. This policy uses the
 * configured field meanings: generic reduction joins the existing sampled
 * armor pool; signed magic-only reduction is a separate flat pre-MR term.
 * EXP/ADENA are additional percentages, never win/loss multipliers.
 * Bound to one complete immutable character frame; no visual/skill/equip grant. */
final class ModernVipCombatProfile {
    static final String RULESET="REBUILT_VIP_COMBAT_V1";
    private final ModernVipCharacterState state;
    private final ModernPhysicalCombat.Source source;
    private final ModernCombatProfile character;
    private ModernVipCombatProfile(ModernVipCharacterState state,long now,long runtimeRevision) {
        this.state=Objects.requireNonNull(state);source=state.profile().vip().source();character=state.profile().character().withVipRuntimeRevision(runtimeRevision);
        if(character.armorPolicy()!=ModernCombatProfile.ArmorPolicy.REBUILT_CHARACTER_EQUIPMENT_VIP
                ||character.evaluatedAtMillis()!=state.ledger().observedMillis()
                ||now<character.evaluatedAtMillis()||character.validUntilMillis()>=0&&now>=character.validUntilMillis())throw invalid();
    }
    static ModernVipCombatProfile qualify(ModernVipCharacterState state,long now){return qualify(state,now,state.ledger().equipmentRevision());}
    static ModernVipCombatProfile qualify(ModernVipCharacterState state,long now,long runtimeRevision){return new ModernVipCombatProfile(state,now,runtimeRevision);}
    void require(ModernPhysicalCombat.Source source,ModernCombatProfile character) {
        if(this.source!=source||this.character!=character)throw invalid();
    }
    boolean permits(ModernCharacterData.InventoryItem item,boolean equipped) {
        if(!ModernVipLoadout.isVip(source,item.objectId())||!source.input().equipment().items().get(item.objectId()).source().equals(item)
                ||!state.leases().containsKey(item.objectId()))return false;
        boolean applied=state.ledger().items().stream().anyMatch(i->i.objectId()==item.objectId());
        return applied==equipped;
    }
    long genericReduction(){return state.profile().genericDamageReduction();}
    long magicOnlyReduction(){return state.profile().magicOnlyReduction();}
    long experienceBonusPercent(){return state.profile().experienceRate();}
    long adenaBonusPercent(){return state.profile().adenaRate();}
    ModernVipCharacterState state(){return state;}
    ModernPhysicalCombat.Source source(){return source;}
    ModernCombatProfile character(){return character;}
    @Override public String toString(){return "CompleteVipCombatProfile[redacted]";}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("invalid complete VIP combat profile");}
}
