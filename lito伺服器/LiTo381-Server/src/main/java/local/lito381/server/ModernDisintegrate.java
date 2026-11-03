package local.lito381.server;

import java.math.BigInteger;
import java.util.Objects;

/** Selected skill77 arithmetic. Not a learned-skill grant or a mutable engine.
 * NEW_MATCH_DISINTEGRATE_V1 keeps the modern integer MR policy, not classic
 * binary64 rounding, active source buffs/dolls or configured PvP multipliers.
 * The independent critical doubles POST-MR damage, not the elemental dice.
 */
final class ModernDisintegrate {
    static final String RULESET="NEW_MATCH_DISINTEGRATE_V1";
    static final int SKILL_ID=77, ACTION=18, EFFECT=11746, CRITICAL_EFFECT=11748;
    static final int RANGE=10, REUSE_MILLIS=5000, MINIMUM_LAWFUL=500;
    enum Failure { UNSUPPORTED, INVALID_STATE, INVALID_RANDOM, ARITHMETIC_OVERFLOW }
    static final class CombatException extends IllegalArgumentException {
        private final Failure failure;
        CombatException(Failure failure){super("modern disintegrate: "+failure);this.failure=failure;}
        Failure failure(){return failure;}
    }
    record Profile(TournamentPresence.CharacterKey key,ModernMagicCombat.Values values,int mpCost) {
        Profile {Objects.requireNonNull(key);Objects.requireNonNull(values);if(mpCost<59||mpCost>70)throw bad();}
    }
    record Outcome(long rawDamage,long appliedDamage,long remainingHp,boolean death,boolean critical) {
        Outcome {
            if(rawDamage<0||appliedDamage<0||appliedDamage>rawDamage||remainingHp<0
                    ||death!=(remainingHp==0)||death&&appliedDamage==0)throw bad();
        }
        int effect(){return critical?CRITICAL_EFFECT:EFFECT;}
    }
    private ModernDisintegrate(){}
    static Profile qualify(ModernPhysicalCombat.Fighter fighter) {
        Objects.requireNonNull(fighter);var magic=fighter.magic();
        if(magic==null||!magic.supported()||magic.profile()!=fighter.profile())throw new CombatException(Failure.UNSUPPORTED);
        var profile=fighter.profile();var row=profile.source().character();
        long intelligence=profile.effective().intelligence();
        if(intelligence<1)throw new CombatException(Failure.UNSUPPORTED);
        // Skill77 is database circle10, native selector row9. Unlike skill70,
        // INT>19 and INT>20 also reduce MP; never narrow the source i64 INT.
        int reduction=0;for(int threshold=12;threshold<=20;threshold++)if(intelligence>threshold)reduction++;
        int cost=Math.toIntExact(70-reduction-ModernMagicCombat.originalMpReduction(
                row.characterClass(),profile.source().persisted().originalIntelligence()));
        return new Profile(fighter.key(),magic.values(),cost);
    }
    static boolean lawfulAllows(long lawful){return lawful>=MINIMUM_LAWFUL;}
    /** Source row lawful=+1 is NOT the lawful>=500 gate. Checked modern i64
     * match-local delta; the eventual engine must commit it with HP/MP/history.
     * This function never updates the imported character SQL.
     */
    static long lawfulAfterCast(long lawful) {
        if(!lawfulAllows(lawful))throw bad();
        try{return Math.incrementExact(lawful);}catch(ArithmeticException e){throw new CombatException(Failure.ARITHMETIC_OVERFLOW);}
    }
    /** Caller owns learned tuple/source, lawful, scope/life/LOS/cooldown/MP
     * checks, stale-state revalidation and one atomic commit. No side effects.
     */
    static Outcome resolve(ModernMagicCombat.Values attacker,ModernMagicCombat.Values defender,
            long currentHp,ModernPhysicalCombat.BoundedRandom random,int timedSpellPower) {
        return resolveProtected(attacker,defender,currentHp,random,timedSpellPower,ModernIncomingDamage.NONE).outcome();
    }
    static ModernIncomingDamage.Resolved<Outcome> resolveProtected(ModernMagicCombat.Values attacker,ModernMagicCombat.Values defender,
            long currentHp,ModernPhysicalCombat.BoundedRandom random,int timedSpellPower,ModernIncomingDamage.Protection protection) {
        Objects.requireNonNull(attacker);Objects.requireNonNull(defender);Objects.requireNonNull(random);
        Objects.requireNonNull(protection);
        if(!attacker.supported()||!defender.supported())throw new CombatException(Failure.UNSUPPORTED);
        var offense=attacker.offense();var defense=defender.defense();
        if(currentHp<1||currentHp>defender.maximumHp()||timedSpellPower!=0&&timedSpellPower!=2
                ||offense.effectiveIntelligence()<1||offense.originalHit()<0||offense.originalHit()>4
                ||offense.originalDamage()<0||defense.magicResistance()<0)throw bad();
        try {
            long source=190;for(int i=0;i<8;i++)source+=draw(random,15)+1;
            // Preserve ClientSession's selected battle draw order: eight dice,
            // independent critical, ignored circle10 ordinary critical, armor.
            boolean critical=draw(random,100)<40;
            var term=bi(offense.effectiveIntelligence()).add(bi(offense.itemSpellPower()))
                    .add(bi(timedSpellPower)).subtract(bi(12)).max(BigInteger.ONE);
            var elemental=bi(source).multiply(bi(32).add(term.multiply(bi(3)))).divide(bi(32));
            draw(random,100); // Circle10 never takes ordinary 1.5x critical.
            var augmented=elemental.add(bi(offense.originalDamage())).add(bi(offense.equipmentDamage()));
            long armor=defense.armorReduction();if(armor>10)armor=11+draw(random,armor-10);
            var beforeMr=augmented.subtract(bi(armor)).subtract(bi(defense.magicOnlyReduction())).max(BigInteger.ZERO);
            // Reuse only the established MR percentage selection, not skill4
            // damage/dice/critical or MP. Input100 yields the exact multiplier.
            long multiplier=ModernMagicCombat.afterResistance(100,defense.magicResistance(),offense.originalHit());
            var finalDamage=beforeMr.multiply(bi(multiplier)).divide(bi(100));
            if(critical)finalDamage=finalDamage.multiply(bi(2));
            // Work stays bounded (<140 bits for i64 inputs). Check the FINAL
            // i64 result: representable damage must not fail at an intermediate
            // multiply, nor silently clamp to native int/short.
            // Incoming protection is after independent critical, before final
            // i64 validation and HP clipping. A representable protected result
            // must not fail because the pre-protection value exceeds i64.
            var impact=protection.apply(finalDamage,currentHp);
            return new ModernIncomingDamage.Resolved<>(new Outcome(impact.damage(),impact.applied(),impact.remaining(),impact.death(),critical),impact);
        }catch(ArithmeticException e){throw new CombatException(Failure.ARITHMETIC_OVERFLOW);}
    }
    private static BigInteger bi(long n){return BigInteger.valueOf(n);}
    private static long draw(ModernPhysicalCombat.BoundedRandom random,long bound) {
        long n=random.nextLong(bound);if(n<0||n>=bound)throw new CombatException(Failure.INVALID_RANDOM);return n;
    }
    private static CombatException bad(){return new CombatException(Failure.INVALID_STATE);}
}
