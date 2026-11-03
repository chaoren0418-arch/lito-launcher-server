package local.lito381.server;

import java.util.Objects;

/**
 * Selected retained skill70, not a generic spell factory. NEW_MATCH_FIRE_STORM_V1
 * deliberately uses the modern qualified grid LOS policy (ordinary WORLD and
 * retained L1SkillUse have LOS); it does not claim the older LiTo BATTLE radius-
 * only admission is identical. No active source buff/doll/talent is imported.
 */
final class ModernFireStorm {
    static final String RULESET="NEW_MATCH_FIRE_STORM_V1";
    static final int SKILL_ID=70, RADIUS=3, ACTION=18, EFFECT=1819, REUSE_MILLIS=2000;
    enum Failure { UNSUPPORTED, INVALID_STATE, INVALID_RANDOM, ARITHMETIC_OVERFLOW }
    static final class CombatException extends IllegalArgumentException {
        private final Failure failure;
        CombatException(Failure failure){super("modern fire storm: "+failure);this.failure=failure;}
        Failure failure(){return failure;}
    }
    record Values(ModernMagicCombat.Values magic,long fireResistance) {
        Values { Objects.requireNonNull(magic); }
        boolean supported(){return magic.supported();}
    }
    record Profile(Values values,int mpCost) {
        Profile {Objects.requireNonNull(values);if(mpCost<39||mpCost>48)throw bad();}
    }
    record Outcome(long rawDamage,long appliedDamage,long remainingHp,boolean death) {
        Outcome {
            if(rawDamage<0||appliedDamage<0||appliedDamage>rawDamage||remainingHp<0
                    ||death!=(remainingHp==0)||death&&appliedDamage==0)throw bad();
        }
    }
    private ModernFireStorm(){}
    static Profile qualify(ModernPhysicalCombat.Fighter fighter) {
        Objects.requireNonNull(fighter);var magic=fighter.magic();
        if(magic==null||!magic.supported()||magic.profile()!=fighter.profile())throw new CombatException(Failure.UNSUPPORTED);
        var profile=fighter.profile();var row=profile.source().character();
        long intelligence=profile.effective().intelligence();
        if(intelligence<1)throw new CombatException(Failure.UNSUPPORTED);
        int reduction=0;for(int threshold=12;threshold<=18;threshold++)if(intelligence>threshold)reduction++;
        long original=ModernMagicCombat.originalMpReduction(row.characterClass(),profile.source().persisted().originalIntelligence());
        int cost=Math.toIntExact(Math.subtractExact(48-reduction,original));
        return new Profile(new Values(magic.values(),profile.equipmentEvaluation().total(ModernEquipmentFacts.Stat.FIRE)),cost);
    }
    /** Caller owns all identity/team/LOS/TTL/life gates and atomic batch commit. */
    static Outcome resolve(Values attacker,Values defender,long currentHp,
            ModernPhysicalCombat.BoundedRandom random,int timedSpellPower) {
        return resolveProtected(attacker,defender,currentHp,random,timedSpellPower,ModernIncomingDamage.NONE).outcome();
    }
    static ModernIncomingDamage.Resolved<Outcome> resolveProtected(Values attacker,Values defender,long currentHp,
            ModernPhysicalCombat.BoundedRandom random,int timedSpellPower,ModernIncomingDamage.Protection protection) {
        Objects.requireNonNull(attacker);Objects.requireNonNull(defender);Objects.requireNonNull(random);
        Objects.requireNonNull(protection);
        if(!attacker.supported()||!defender.supported())throw new CombatException(Failure.UNSUPPORTED);
        if(currentHp<1||currentHp>defender.magic().maximumHp()||timedSpellPower!=0&&timedSpellPower!=2)throw bad();
        try {
            long source=24;for(int i=0;i<8;i++)source=Math.addExact(source,Math.addExact(draw(random,15),1));
            var offense=attacker.magic().offense();
            long term=Math.max(1,Math.subtractExact(Math.addExact(Math.addExact(
                    offense.effectiveIntelligence(),offense.itemSpellPower()),timedSpellPower),12));
            long resistance=defender.fireResistance();
            // Preserve the retained binary64 0.32 quantization; never narrow
            // source resistance to byte/int or silently saturate a signed i64.
            long absolute=resistance<0?Math.negateExact(resistance):resistance;
            double scaled=0.32*(double)absolute;
            if(!Double.isFinite(scaled)||scaled>=0x1.0p63)throw new ArithmeticException();
            long floor=(long)scaled;if(resistance<0)floor=Math.negateExact(floor);
            long coefficient=Math.max(0,Math.subtractExact(Math.addExact(32,Math.multiplyExact(term,3)),floor));
            long elemental=Math.multiplyExact(source,coefficient)/32;
            // Both ClientSession.ordinaryMagicDamage and L1MagicPc draw this
            // value before testing skill_level<=6. Circle nine never criticals.
            draw(random,100);
            long augmented=Math.addExact(Math.addExact(elemental,offense.originalDamage()),offense.equipmentDamage());
            long armor=defender.magic().defense().armorReduction();
            if(armor>10)armor=Math.addExact(11,draw(random,Math.subtractExact(armor,10)));
            long beforeMr=ModernMagicCombat.beforeResistance(augmented,armor,defender.magic().defense().magicOnlyReduction());
            // Keep the already-selected modern checked-integer MR policy:
            // unlike native binary64 truncation, 1840 at MR200 is 828, not
            // 827. Do not silently change existing Light Arrow arithmetic.
            long raw=ModernMagicCombat.afterResistance(beforeMr,defender.magic().defense().magicResistance(),offense.originalHit());
            var impact=protection.apply(raw,currentHp);
            return new ModernIncomingDamage.Resolved<>(new Outcome(impact.damage(),impact.applied(),impact.remaining(),impact.death()),impact);
        }catch(ArithmeticException overflow){throw new CombatException(Failure.ARITHMETIC_OVERFLOW);}
    }
    private static long draw(ModernPhysicalCombat.BoundedRandom random,long bound){
        if(bound<1)throw new CombatException(Failure.INVALID_RANDOM);long value=random.nextLong(bound);
        if(value<0||value>=bound)throw new CombatException(Failure.INVALID_RANDOM);return value;
    }
    private static CombatException bad(){return new CombatException(Failure.INVALID_STATE);}
}
