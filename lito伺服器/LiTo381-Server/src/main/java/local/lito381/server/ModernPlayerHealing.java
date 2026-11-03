package local.lito381.server;

import java.util.Objects;

/** Source-qualified skill1 arithmetic for participant targets. This is NOT a
 * learned grant, target admission, active-status import, wire or engine hook.
 * Honor objectives retain their existing, separate target rules and receipts. */
final class ModernPlayerHealing {
    static final String RULESET = "MODERN_PLAYER_HEALING_1_V1";
    static final int SKILL_ID = 1, ACTION = 19, EFFECT = 11722, REUSE_MILLIS = 10;

    record Profile(TournamentPresence.CharacterKey key, ModernApplicationProtocol.CharacterRow character,
            long equipmentRevision, long maximumHp, long maximumMp, ModernHonorHealing.Profile calculation) {
        Profile {
            Objects.requireNonNull(key); Objects.requireNonNull(character); Objects.requireNonNull(calculation);
            if (key.objectId()!=character.objectId() || equipmentRevision<0 || maximumHp<1 || maximumMp<0
                    || !calculation.supported()) throw bad();
        }
        int mpCost() { return calculation.mpCost(); }
        boolean accepts(ModernSkills.Snapshot skills) {
            return skills!=null && skills.sourceId()==key.sourceId() && skills.character().equals(character)
                    && skills.skills().stream().anyMatch(s->s.skillId()==SKILL_ID && s.support()==ModernSkills.Support.BASIC_HEAL);
        }
        @Override public String toString() { return "PlayerHealingProfile[redacted]"; }
    }
    /** These are the TARGET's live states, never the caster's persisted buffs.
     * Water Life is consumed on an admitted cast, including full HP/erosion.
     * The eventual engine adapter must obtain and revalidate their authority. */
    record Effects(boolean waterLife, boolean polluteWater, boolean dirtyCurrent, boolean healingErosion) {
        static final Effects NONE = new Effects(false,false,false,false);
        Effects consume() { return new Effects(false,polluteWater,dirtyCurrent,healingErosion); }
    }
    record Outcome(long baseAmount, long adjustedAmount, long restored, long remainingHp, Effects after) {
        Outcome {
            Objects.requireNonNull(after);
            if (baseAmount<0 || restored<0 || restored>Math.max(0,adjustedAmount) || remainingHp<1) throw bad();
        }
    }
    static Profile qualify(ModernPhysicalCombat.Fighter fighter) {
        Objects.requireNonNull(fighter);
        var profile=fighter.profile(); var magic=fighter.magic(); var healing=fighter.healing();
        // Fighter was produced from the same source-owned, complete 127-row
        // INT growth table and live equipment evaluation used by Honor. Do
        // not invent a two-dice fallback when that source did not opt in.
        if (magic==null || !magic.supported() || magic.profile()!=profile || healing==null || !healing.supported()) throw bad();
        return new Profile(fighter.key(),profile.source().character(),profile.equipmentRevision(),
                profile.effective().maximumHp(),profile.effective().maximumMp(),healing);
    }
    static Outcome resolve(Profile profile, long lawful, long hp, long maximumHp, Effects effects,
            ModernPhysicalCombat.BoundedRandom random) {
        Objects.requireNonNull(profile); Objects.requireNonNull(effects);
        // Lawful can change after other casts. Use the captured LIVE runtime
        // value, not the character's original SQL row or equipment profile.
        var calculation=new ModernHonorHealing.Profile(true,profile.calculation().diceCount(),profile.mpCost(),lawful);
        // amount is the complete lawful-adjusted roll, not the capped Honor
        // restoration. Target modifiers must run BEFORE the player's HP cap.
        long base=ModernHonorHealing.resolve(calculation,hp,maximumHp,random).amount();
        long adjusted=adjustedAmount(base,effects);
        long restored=Math.min(Math.max(0,adjusted),Math.subtractExact(maximumHp,hp));
        return new Outcome(base,adjusted,restored,Math.addExact(hp,restored),effects.consume());
    }
    /** Native negative-healing arithmetic: doubling precedes arithmetic
     * right shifts. Odd positive recovery therefore rounds UP on each half;
     * erosion suppresses restoration, it does not damage a living player. */
    static long adjustedAmount(long base, Effects effects) {
        Objects.requireNonNull(effects);
        if (base<0) throw bad();
        long damage=Math.negateExact(effects.waterLife()?Math.multiplyExact(base,2):base);
        if (effects.polluteWater()) damage>>=1;
        if (effects.dirtyCurrent()) damage>>=1;
        if (effects.healingErosion()) damage=Math.negateExact(damage);
        return Math.negateExact(damage);
    }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid qualified player healing"); }
    private ModernPlayerHealing() { }
}
