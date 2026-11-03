package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.Stat.*;

import java.util.List;
import java.util.Objects;

/**
 * NEW_MATCH_LIGHT_ARROW_V1: qualified PC-versus-PC skill 4, not a generic magic
 * executor. Its source tuple is admitted separately by ModernSkillsJdbc. This
 * pure calculator never grants a learned skill, consumes MP, changes HP, reads
 * SQL or imports classic active buffs/dolls/talents/PvP configuration.
 *
 * It selects the traced LiTo ordinaryMagicDamage layers: effective INT + item
 * SP (not INT-derived true SP again), original critical/damage/hit, direct magic
 * damage, armor-template/set reduction, then the retained MR bands. It does not
 * reinterpret direct REDUCTION or probability MAGIC_HIT as ordinary magic.
 */
final class ModernMagicCombat {
    static final String RULESET = "NEW_MATCH_LIGHT_ARROW_V1";
    static final int SKILL_ID = 4, RANGE = 10;
    enum UnsupportedReason { NONE, WEAPON_UNQUALIFIED, CUSTOM_EXECUTOR, EQUIPMENT_PROC, LIFETIME_UNQUALIFIED }
    enum Failure { UNSUPPORTED, INVALID_STATE, INVALID_RANDOM, ARITHMETIC_OVERFLOW }
    static final class CombatException extends IllegalArgumentException {
        private final Failure failure;
        CombatException(Failure failure) { super("modern magic combat: " + failure); this.failure = failure; }
        Failure failure() { return failure; }
    }
    record WisdomRow(long id, long magicResistance) {
        WisdomRow { if (id < 1 || id > 50 || magicResistance < 0) throw invalid(); }
    }
    record Growth(int sourceId, String database, List<WisdomRow> wisdom) {
        Growth {
            if (sourceId < 1) throw invalid();
            ModernPhysicalCombat.identifier(database);
            if (wisdom == null || wisdom.size() != 50) throw invalid();
            wisdom = List.copyOf(wisdom);
            for (int index = 0; index < 50; index++) if (wisdom.get(index).id() != index + 1) throw invalid();
        }
        long bonus(long effectiveWisdom) {
            if (effectiveWisdom < 0) throw invalid();
            // The actual table has 1..50, not a fabricated row 0 or 51..127.
            // BattleMagicResistance:28-30 explicitly defines zero WIS as zero;
            // only the retained lookup index, never the i64 ability, saturates.
            return effectiveWisdom == 0 ? 0 : wisdom.get((int)Math.min(50, effectiveWisdom) - 1).magicResistance();
        }
        @Override public String toString() { return "ModernMagicGrowth[redacted]"; }
    }
    record Offense(long effectiveIntelligence, long itemSpellPower, long intelligenceTerm,
            long originalCritical, long originalDamage, long originalHit, long equipmentDamage) {}
    record Defense(long classBase, long levelBonus, long wisdomBonus, long originalBonus,
            long equipmentMagicResistance, long magicResistance, long armorReduction,long magicOnlyReduction) {
        Defense(long classBase,long levelBonus,long wisdomBonus,long originalBonus,long equipmentMagicResistance,long magicResistance,long armorReduction) {
            this(classBase,levelBonus,wisdomBonus,originalBonus,equipmentMagicResistance,magicResistance,armorReduction,0);
        }
    }
    /** Complete numeric defense is independent of human identity and learned-skill permission. */
    record Values(boolean supported,long maximumHp,Offense offense,Defense defense) {
        Values {
            Objects.requireNonNull(offense);Objects.requireNonNull(defense);
            if(maximumHp<1)throw invalid();
        }
    }
    static final class Fighter {
        private final ModernCombatProfile profile;
        private final Offense offense;
        private final Defense defense;
        private final UnsupportedReason unsupported;
        private final long mpCost;
        private Fighter(ModernCombatProfile profile, Offense offense, Defense defense,
                UnsupportedReason unsupported, long mpCost) {
            this.profile=profile; this.offense=offense; this.defense=defense; this.unsupported=unsupported; this.mpCost=mpCost;
        }
        ModernCombatProfile profile() { return profile; }
        TournamentPresence.CharacterKey key() { return new TournamentPresence.CharacterKey(profile.source().sourceId(), profile.source().character().objectId()); }
        Offense offense() { return offense; }
        Defense defense() { return defense; }
        long mpCost() { return mpCost; }
        long range() { return RANGE; }
        boolean supported() { return unsupported == UnsupportedReason.NONE; }
        UnsupportedReason unsupportedReason() { return unsupported; }
        Values values() { return new Values(supported(),profile.effective().maximumHp(),offense,defense); }
        @Override public String toString() { return "ModernMagicFighter[redacted]"; }
    }
    /** rawDamage is the complete post-MR damage before remaining-HP saturation. */
    record Outcome(long rawDamage, long appliedDamage, long remainingHp, boolean death, boolean critical) {
        Outcome {
            if (rawDamage < 0 || appliedDamage < 0 || appliedDamage > rawDamage || remainingHp < 0
                    || death && (remainingHp != 0 || appliedDamage == 0)) throw invalid();
        }
    }
    private ModernMagicCombat() {}

    static Fighter qualify(ModernPhysicalCombat.Source source, ModernCombatProfile profile) {
        return qualify(source,profile,null);
    }
    static Fighter qualify(ModernPhysicalCombat.Source source, ModernCombatProfile profile,ModernArmorProtectionLoadout protection) {
        return qualify(source,profile,protection,null);
    }
    static Fighter qualify(ModernPhysicalCombat.Source source, ModernCombatProfile profile,ModernArmorProtectionLoadout protection,ModernVipCombatProfile vip) {
        if(vip!=null)vip.require(source,profile);
        if((profile.armorPolicy()==ModernCombatProfile.ArmorPolicy.REBUILT_CHARACTER_EQUIPMENT_VIP)!=(vip!=null))throw invalid();
        if(protection!=null)protection.require(source,profile);
        Objects.requireNonNull(source); Objects.requireNonNull(profile);
        if (source.magicGrowth() == null || !source.input().equipment().source().equals(profile.source())) throw invalid();
        try {
            var row=profile.source().character(); var saved=profile.source().persisted(); var equipment=profile.equipmentEvaluation();
            int type=row.characterClass(); long originalInt=saved.originalIntelligence();
            long intelligence=profile.effective().intelligence(), sp=equipment.total(SP);
            long term=Math.max(1, Math.subtractExact(Math.addExact(intelligence,sp),12));
            // Validate the exact positive coefficient before entering an engine.
            Math.addExact(32, Math.multiplyExact(term,3));
            var offense=new Offense(intelligence,sp,term,originalCritical(type,originalInt),
                    originalDamage(type,originalInt),originalHit(type,originalInt),equipment.total(MAGIC_DAMAGE));
            long classBase=classBase(type), level=row.level()/2;
            long wisdom=source.magicGrowth().bonus(profile.effective().wisdom());
            long originalMr=originalMr(type,saved.originalWisdom()), itemMr=equipment.total(MR);
            long mr=Math.max(0, add(classBase,level,wisdom,originalMr,itemMr));
            var defense=new Defense(classBase,level,wisdom,originalMr,itemMr,mr,
                    Math.addExact(equipment.magicReductionBase(),vip==null?0:vip.genericReduction()),vip==null?0:vip.magicOnlyReduction());
            UnsupportedReason reason=UnsupportedReason.NONE;
            for (var flag:profile.equipped()) {
                var item=source.input().equipment().items().get(flag.objectId()).source();
                if (!flag.equipped() && !item.equipped()) continue;
                var life=source.input().equipment().lifetimes().view(item.objectId(),profile.evaluatedAtMillis());
                if (!life.usable() && life.state()!=ModernItemLifetime.State.EXPIRED) reason=UnsupportedReason.LIFETIME_UNQUALIFIED;
                // Expiring an unknown custom source does not qualify its saved
                // baseline (e.g. a persisted VIP polymorph effect).
                if ((vip==null||!vip.permits(item,flag.equipped()))&&ModernVipEquipment.unresolvedCustom(source,item,flag.equipped())) reason=UnsupportedReason.CUSTOM_EXECUTOR;
                if (source.procCatalogIds().contains(item.template().catalogId())&&(protection==null||!protection.permits(item.objectId()))) reason=UnsupportedReason.EQUIPMENT_PROC;
            }
            if (profile.weapon().state()==ModernCombatProfile.WeaponState.UNQUALIFIED) reason=UnsupportedReason.WEAPON_UNQUALIFIED;
            // Skill 4 is circle one: no current INT or helmet reduction. The
            // original INT class reduction is still required, so MP is 1..3.
            return new Fighter(profile,offense,defense,reason,3-originalMpReduction(type,originalInt));
        } catch (ArithmeticException failure) { throw new CombatException(Failure.ARITHMETIC_OVERFLOW); }
    }

    /** Engine must first verify learned tuple, scope/team/range/LOS/pace/MP/TTL. */
    static Outcome resolve(Fighter attacker, Fighter defender, long currentHp, ModernPhysicalCombat.BoundedRandom random) {
        Objects.requireNonNull(attacker); Objects.requireNonNull(defender); Objects.requireNonNull(random);
        if (!attacker.supported() || !defender.supported()) throw new CombatException(Failure.UNSUPPORTED);
        if (attacker.key().equals(defender.key()) || currentHp < 1 || currentHp > defender.profile().effective().maximumHp())
            throw new CombatException(Failure.INVALID_STATE);
        return resolveValues(attacker.values(),defender.values(),currentHp,random);
    }
    /** Identity and learned-skill gates belong to the typed adapter/engine, never this arithmetic. */
    static Outcome resolveValues(Values attacker,Values defender,long currentHp,ModernPhysicalCombat.BoundedRandom random) {
        return resolveValues(attacker,defender,currentHp,random,0);
    }
    static Outcome resolveValues(Values attacker,Values defender,long currentHp,ModernPhysicalCombat.BoundedRandom random,int timedSpellPower) {
        return resolveProtected(attacker,defender,currentHp,random,timedSpellPower,ModernIncomingDamage.NONE).outcome();
    }
    static ModernIncomingDamage.Resolved<Outcome> resolveProtected(Values attacker,Values defender,long currentHp,
            ModernPhysicalCombat.BoundedRandom random,int timedSpellPower,ModernIncomingDamage.Protection protection) {
        Objects.requireNonNull(attacker);Objects.requireNonNull(defender);Objects.requireNonNull(random);
        Objects.requireNonNull(protection);
        if(timedSpellPower!=0 && timedSpellPower!=2)throw invalid();
        if(!attacker.supported()||!defender.supported())throw new CombatException(Failure.UNSUPPORTED);
        if(currentHp<1||currentHp>defender.maximumHp())throw new CombatException(Failure.INVALID_STATE);
        try {
            long dice=Math.addExact(7,draw(random,10)); // Actual qualified skill4: base6 + 1d10.
            // Accounts supplies INT+itemSP to ClientSession, which adds Wisdom
            // before BattleElementalResistance subtracts 12 and clamps. The
            // prepared offense term has ALREADY subtracted/clamped: adding to
            // it would incorrectly grant damage at low INT or negative SP.
            // Disabled timed policy keeps the existing qualified result exact.
            long term=timedSpellPower==0?attacker.offense.intelligenceTerm():Math.max(1,Math.subtractExact(
                    Math.addExact(Math.addExact(attacker.offense.effectiveIntelligence(),attacker.offense.itemSpellPower()),timedSpellPower),12));
            long coefficient=Math.addExact(32,Math.multiplyExact(term,3));
            long elemental=Math.multiplyExact(dice,coefficient)/32; // attr16 selects no elemental defense.
            boolean critical=Math.addExact(draw(random,100),1)<=10+attacker.offense.originalCritical();
            long scaled=critical?Math.multiplyExact(elemental,3)/2:elemental;
            long augmented=add(scaled,attacker.offense.originalDamage(),attacker.offense.equipmentDamage());
            long armor=defender.defense.armorReduction();
            if (armor>10) armor=Math.addExact(11,draw(random,Math.subtractExact(armor,10)));
            long beforeMr=beforeResistance(augmented,armor,defender.defense.magicOnlyReduction());
            long damage=afterResistance(beforeMr,defender.defense.magicResistance(),attacker.offense.originalHit());
            var impact=protection.apply(damage,currentHp);
            return new ModernIncomingDamage.Resolved<>(new Outcome(impact.damage(),impact.applied(),impact.remaining(),impact.death(),critical),impact);
        } catch (ArithmeticException failure) { throw new CombatException(Failure.ARITHMETIC_OVERFLOW); }
    }
    static long afterResistance(long damage, long mr, long originalHit) {
        if (damage<0 || mr<0 || originalHit<0 || originalHit>4) throw invalid();
        long divisor,percent;
        if(mr==0){divisor=1;percent=100;} else if(mr<=50){divisor=2;percent=100;}
        else if(mr<=100){divisor=3;percent=90;} else if(mr<=120){divisor=4;percent=90;}
        else if(mr<=140){divisor=5;percent=80;} else if(mr<=160){divisor=6;percent=80;}
        else if(mr<=180){divisor=7;percent=70;} else if(mr<=200){divisor=8;percent=70;}
        else if(mr<=220){divisor=9;percent=60;} else if(mr<=240){divisor=10;percent=60;}
        else{divisor=11;percent=50;}
        long multiplier=Math.subtractExact(percent,Math.floorDiv(Math.subtractExact(mr,originalHit),divisor));
        // Native receiveDamage floors nonpositive damage; no negative HP credit.
        if(multiplier<=0)return 0;
        try{return Math.multiplyExact(damage,multiplier)/100;}
        catch(ArithmeticException intermediateOverflow) {
            // A representable final damage must not be rejected just because
            // multiplying by percent precedes division. Inputs are bounded
            // i64 and positive multiplier is at most 104: < 71-bit work.
            // Genuine out-of-i64 final results still fail via longValueExact.
            return java.math.BigInteger.valueOf(damage).multiply(java.math.BigInteger.valueOf(multiplier))
                    .divide(java.math.BigInteger.valueOf(100)).longValueExact();
        }
    }
    /** Flat magic-only term is neither sampled with generic armor nor affected
     * by physical ignore-reduction. Subtract both before flooring, then MR.
     * Preserve the exact old zero-term arithmetic; widened VIP intermediates
     * must still produce a checked i64 result. */
    static long beforeResistance(long augmented,long sampledArmor,long magicOnly) {
        if(magicOnly==0)return Math.max(0,Math.subtractExact(augmented,sampledArmor));
        return java.math.BigInteger.valueOf(augmented).subtract(java.math.BigInteger.valueOf(sampledArmor))
                .subtract(java.math.BigInteger.valueOf(magicOnly)).max(java.math.BigInteger.ZERO).longValueExact();
    }
    private static long draw(ModernPhysicalCombat.BoundedRandom random,long bound) {
        if(bound<1)throw new CombatException(Failure.INVALID_RANDOM);
        long value=random.nextLong(bound);
        if(value<0||value>=bound)throw new CombatException(Failure.INVALID_RANDOM);return value;
    }
    private static void original(int type,long value) { if(type<0||type>6||value<0)throw invalid(); }
    static long classBase(int type) {
        original(type,0);return switch(type){case 0,4->10;case 1->0;case 2->25;case 3->15;case 5->18;default->20;};
    }
    static long originalMr(int type,long value) {
        original(type,value);return switch(type){
            case 0->value>=14?2:value>=12?1:0;case 1->value>=12?2:value>=10?1:0;
            case 2->value>=16?2:value>=13?1:0;case 3->value>=15?1:0;
            case 4->value>=16?4:value==15?3:value==14?2:value>=11?1:0;
            case 5->value>=14?2:0;default->value==18?4:value>=15&&value<=17?2:0;};
    }
    static long originalHit(int type,long value) {
        original(type,value);return switch(type){
            case 0,4->value>=14?2:value>=12?1:0;case 1->value==12?2:value>=10&&value<=11?1:0;
            case 2->value>=15?2:value>=13?1:0;case 3->value>80?2:value>14?1:0;
            case 5->value>=16?4:value>=14?3:value>=12?2:0;default->value>=13?1:0;};
    }
    static long originalCritical(int type,long value) {
        original(type,value);
        if(type==2)return value>=16?4:value>=14?2:0;
        if(type==3)return value==15?2:value==16?4:value==17?6:value==18?8:0;
        return 0;
    }
    static long originalDamage(int type,long value) {
        original(type,value);return switch(type){
            case 3->value>=13?1:0;case 5->value==17?3:value>=15&&value<=16?2:value>=13&&value<=14?1:0;
            case 6->value==17?2:value==16?1:0;default->0;};
    }
    static long originalMpReduction(int type,long value) {
        original(type,value);return switch(type){
            case 0->value>=13?2:value>=11?1:0;case 1->value>=11?2:value>=9?1:0;
            case 4->value>=15?2:value>=13?1:0;case 6->value>=15?2:value==14?1:0;default->0;};
    }
    private static long add(long... values) { long total=0;for(long value:values)total=Math.addExact(total,value);return total; }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid qualified modern magic"); }
}
