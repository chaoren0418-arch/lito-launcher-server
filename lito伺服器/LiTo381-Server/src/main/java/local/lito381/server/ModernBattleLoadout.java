package local.lito381.server;

import java.util.List;
import java.util.Objects;

/** One immutable, qualified runtime ability projection. Mutable resources,
 * inventory quantities, clocks, skill receipts and authorization do NOT live
 * here and must not be reset when replacing this object. No I/O or global state. */
final class ModernBattleLoadout {
    record Features(boolean bow, boolean fireStorm, boolean npcFireStorm, boolean disintegrate,
            boolean items, boolean regen, boolean greaterHaste, boolean haste, boolean slow) {
        Features(boolean bow,boolean fireStorm,boolean npcFireStorm,boolean disintegrate,boolean items,boolean regen,boolean greaterHaste,boolean haste) {
            this(bow,fireStorm,npcFireStorm,disintegrate,items,regen,greaterHaste,haste,false);
        }
        Features(boolean bow,boolean fireStorm,boolean npcFireStorm,boolean disintegrate,boolean items,boolean regen,boolean greaterHaste) {
            this(bow,fireStorm,npcFireStorm,disintegrate,items,regen,greaterHaste,false);
        }
        Features(boolean bow,boolean fireStorm,boolean npcFireStorm,boolean disintegrate,boolean items,boolean regen) {
            this(bow,fireStorm,npcFireStorm,disintegrate,items,regen,false);
        }
    }
    private final ModernPhysicalCombat.Fighter fighter;
    private final ModernCombatant combatant;
    private final Features features;
    private final ModernInventoryWeight.Basis weightBasis;
    private final ModernFireStorm.Profile fireStorm;
    private final ModernDisintegrate.Profile disintegrate;
    private final ModernGreaterHaste.Profile greaterHaste;
    private final ModernHaste.Profile haste;
    private final ModernSlow.Profile slow;
    private final long potionRecovery;
    private final ModernNpcMelee.BlessingBinding npcBinding;
    private final ModernNpcMelee.Player npcPlayer;
    private final ModernNpcMelee.Victim npcVictim;

    private ModernBattleLoadout(ModernPhysicalCombat.Fighter fighter, Features features,
            ModernInventoryWeight.Basis weightBasis) {
        this(fighter,features,weightBasis,null);
    }
    private ModernBattleLoadout(ModernPhysicalCombat.Fighter fighter, Features features,
            ModernInventoryWeight.Basis weightBasis,ModernNpcMelee.BlessingBinding npcBinding) {
        this.fighter=Objects.requireNonNull(fighter);this.features=Objects.requireNonNull(features);
        var profile=fighter.profile();
        if(features.regen()&&(fighter.regen()==null||!fighter.regen().supported()))throw invalid();
        if(weightBasis!=null) {
            var proven=ModernInventoryWeight.qualify(profile,weightBasis.sourceRate());
            if(!features.items()||!weightBasis.sameFacts(proven)||proven.validUntilMillis()>=0
                    &&(weightBasis.validUntilMillis()<0||weightBasis.validUntilMillis()>proven.validUntilMillis()))throw invalid();
        }
        this.weightBasis=weightBasis;
        combatant=ModernCombatant.human(fighter,features.bow(),features.fireStorm());
        fireStorm=features.fireStorm()||features.npcFireStorm()?ModernFireStorm.qualify(fighter):null;
        disintegrate=features.disintegrate()?ModernDisintegrate.qualify(fighter):null;
        greaterHaste=features.greaterHaste()?ModernGreaterHaste.qualify(fighter):null;
        haste=features.haste()?ModernHaste.qualify(fighter):null;
        slow=features.slow()?ModernSlow.qualify(fighter):null;
        if(features.slow())ModernSlow.equipmentImmune(fighter);
        potionRecovery=features.items()?Math.max(0,profile.equipmentEvaluation().total(ModernEquipmentFacts.Stat.POTION_RECOVERY)):0;
        this.npcBinding=npcBinding;
        npcPlayer=npcBinding==null?null:ModernNpcMelee.player(fighter,npcBinding);
        npcVictim=npcPlayer==null?null:ModernNpcMelee.victim(npcPlayer);
    }
    private ModernBattleLoadout(ModernParticipantId identity,ModernBotTemplate template) {
        fighter=null;features=null;weightBasis=null;fireStorm=null;disintegrate=null;greaterHaste=null;haste=null;slow=null;potionRecovery=0;
        combatant=ModernCombatant.bot(identity,template);
        npcBinding=null;npcPlayer=null;npcVictim=ModernNpcMelee.victim(identity,template);
    }
    static ModernBattleLoadout initial(ModernPhysicalCombat.Fighter fighter,Features features,
            ModernInventoryWeight.Basis basis) {
        Objects.requireNonNull(fighter);
        if(fighter.profile().vitalPolicy()!=ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH)throw invalid();
        return new ModernBattleLoadout(fighter,features,basis);
    }
    static ModernBattleLoadout bot(ModernParticipantId identity,ModernBotTemplate template) {
        return new ModernBattleLoadout(identity,template);
    }
    /** Qualification must already retain live HP/MP and prove the exact next
     * equipment revision. Rebuild every derived ability from that SAME fighter.
     * This is a candidate only, never permission to publish it in an old world. */
    ModernBattleLoadout replacement(ModernPhysicalCombat.Fighter next) {
        Objects.requireNonNull(next);
        if(fighter==null)throw invalid();
        var before=fighter.profile();var after=next.profile();
        if(after.vitalPolicy()!=ModernCombatProfile.VitalPolicy.RETAINED_MATCH
                ||!before.source().equals(after.source())||!fighter.catalogDatabase().equals(next.catalogDatabase())
                ||after.equipmentRevision()!=Math.incrementExact(before.equipmentRevision())
                ||after.evaluatedAtMillis()<before.evaluatedAtMillis()
                ||(fighter.magic()==null)!=(next.magic()==null)||(fighter.ranged()==null)!=(next.ranged()==null)
                ||(fighter.regen()==null)!=(next.regen()==null)||(fighter.healing()==null)!=(next.healing()==null)
                ||(fighter.vip()==null)!=(next.vip()==null)
                ||(fighter.armorProtection()==null)!=(next.armorProtection()==null))throw invalid();
        ModernInventoryWeight.Basis basis=null;
        if(weightBasis!=null) {
            var fresh=weightBasis.requalify(after);
            // Captured stock/catalog lifetime evidence may be tighter than the
            // fighter. A replacement must not silently extend that observation.
            // Legacy worlds freeze every deadline. Full VIP state instead
            // proves the original item leases and their dynamic expiry.
            basis=fighter.vip()==null?fresh.retaining(weightBasis.validUntilMillis()):fresh;
            if(basis.validUntilMillis()>=0&&after.evaluatedAtMillis()>=basis.validUntilMillis())throw invalid();
        }
        return new ModernBattleLoadout(next,features,basis,npcBinding);
    }
    /** No ability/version change: only bind the private VIP ledger to the
     * engine-owned match/entity/life. Pending reservations keep the old object. */
    ModernBattleLoadout rebindVip(ModernVipLifecycle.Actor actor) {
        if(fighter==null||fighter.vip()==null)throw invalid();
        var state=fighter.vip().state().rebind(actor);
        var next=ModernPhysicalCombat.qualifyVipRuntime(state,fighter.profile().evaluatedAtMillis(),fighter.profile().equipmentRevision(),fighter.armorProtection());
        return new ModernBattleLoadout(next,features,weightBasis,npcBinding);
    }
    /** Initial server-owned NPC convention, never inferred from weapon bits. */
    ModernBattleLoadout withNpc(ModernNpcMelee.BlessingBinding binding) {
        Objects.requireNonNull(binding);
        if(fighter==null||npcBinding!=null||fighter.profile().vitalPolicy()!=ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH)throw invalid();
        return new ModernBattleLoadout(fighter,features,weightBasis,binding);
    }
    ModernPhysicalCombat.Fighter fighter(){return fighter;}
    ModernCombatant combatant(){return combatant;}
    ModernMagicCombat.Fighter magic(){return fighter==null?null:fighter.magic();}
    ModernNaturalRegen.Profile regen(){return fighter==null?null:fighter.regen();}
    ModernHonorHealing.Profile healing(){return fighter==null?null:fighter.healing();}
    ModernFireStorm.Profile fireStorm(){return fireStorm;}
    ModernDisintegrate.Profile disintegrate(){return disintegrate;}
    ModernGreaterHaste.Profile greaterHaste(){return greaterHaste;}
    ModernHaste.Profile haste(){return haste;}
    ModernSlow.Profile slow(){return slow;}
    ModernInventoryWeight.Basis weightBasis(){return weightBasis;}
    long potionRecovery(){return potionRecovery;}
    /** Only already-enabled lanes are repriced; this never enables a skill or
     * supplies feature merely because a replacement has corresponding facts. */
    ModernEquipmentHistory.Profile historyProfile(java.util.Set<ModernUseHistory.Lane> lanes,boolean items) {
        if(fighter==null||items!=features.items())throw invalid();
        var costs=new java.util.EnumMap<ModernUseHistory.Lane,Integer>(ModernUseHistory.Lane.class);
        for(var lane:lanes)costs.put(lane,switch(lane) {
            case TELEPORT->10;
            case LIGHT_ARROW,NPC_LIGHT_ARROW->{if(magic()==null)throw invalid();yield Math.toIntExact(magic().mpCost());}
            case HEALING->{if(healing()==null)throw invalid();yield healing().mpCost();}
            case FIRE_STORM,NPC_FIRE_STORM->{if(fireStorm==null)throw invalid();yield fireStorm.mpCost();}
            case DISINTEGRATE,NPC_DISINTEGRATE->{if(disintegrate==null)throw invalid();yield disintegrate.mpCost();}
            case GREATER_HASTE->{if(greaterHaste==null)throw invalid();yield greaterHaste.mpCost();}
            case HASTE->{if(haste==null)throw invalid();yield haste.mpCost();}
            case SLOW->{if(slow==null)throw invalid();yield slow.mpCost();}
        });
        return new ModernEquipmentHistory.Profile(fighter.profile().equipmentRevision(),combatant.maxHp(),combatant.maxMp(),costs,items?potionRecovery:null);
    }
    ModernNpcMelee.Player npcPlayer(){return npcPlayer;}
    ModernNpcMelee.Victim npcVictim(){return npcVictim;}
    boolean currentAt(long epochMillis) {
        return epochMillis>=combatant.evaluatedAtMillis()
                &&(combatant.validUntilMillis()<0||epochMillis<combatant.validUntilMillis())
                &&(weightBasis==null||weightBasis.validUntilMillis()<0||epochMillis<weightBasis.validUntilMillis());
    }
    /** Pin freshly requalified CURRENT gear before evaluating a SET intent.
     * A matching SQL row/physical table alone does not pin magic/healing/regen
     * growth. Ignore profile object identity and retained-vs-full initial HP. */
    boolean matchesDerivedFacts(ModernPhysicalCombat.Fighter fresh) {
        if(fighter==null||fresh==null)return false;
        var protection=fighter.armorProtection();var nextProtection=fresh.armorProtection();
        if(protection==null?nextProtection!=null:!protection.sameFacts(nextProtection))return false;
        var a=magic();var b=fresh.magic();
        if(a==null?b!=null:b==null||!a.values().equals(b.values())||a.mpCost()!=b.mpCost()
                ||a.range()!=b.range()||a.unsupportedReason()!=b.unsupportedReason())return false;
        var r=fighter.ranged();var s=fresh.ranged();
        if(r==null?s!=null:s==null||!r.values().equals(s.values())||r.unsupported()!=s.unsupported())return false;
        var g=regen();var h=fresh.regen();
        if(g==null?h!=null:h==null||!g.values().equals(h.values())||g.unsupportedReason()!=h.unsupportedReason())return false;
        return Objects.equals(healing(),fresh.healing())&&(!features.slow()||fighter.hasteItemCount().equals(fresh.hasteItemCount()));
    }
    /** Preserve actual remaining stocks and original unit facts, not shop's
     * initial quantities or SQL inventory. A failed candidate mutates nothing. */
    ModernInventoryWeight.Snapshot reweight(List<ModernItemProtocol.Stock> items,List<ModernAmmo.Stock> ammo,
            List<ModernInventoryWeight.Unit> units) {
        if(weightBasis==null)throw invalid();
        return reweight(items,ammo,units,List.of());
    }
    ModernInventoryWeight.Snapshot reweight(List<ModernItemProtocol.Stock> items,List<ModernAmmo.Stock> ammo,
            List<ModernInventoryWeight.Unit> units,List<ModernSpellMaterials.Stock> materials) {
        if(weightBasis==null)throw invalid();
        return ModernInventoryWeight.world(weightBasis,items,ammo,materials,units);
    }
    @Override public String toString(){return "ModernBattleLoadout[redacted]";}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("invalid runtime battle loadout");}
}
