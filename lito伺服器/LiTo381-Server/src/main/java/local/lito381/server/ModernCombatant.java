package local.lito381.server;

import java.util.Objects;

/**
 * Immutable typed identity plus calculator values. A HUMAN can only be adapted
 * from the existing qualified Fighter. A SERVER_BOT never owns a CharacterData
 * snapshot, SQL character key, equipment binding or synthetic learned-skill row.
 * This is not authorization to attack/cast: the engine still checks scope,
 * learned skills, team, position, cooldown, lifecycle and commits HP/MP atomically.
 */
final class ModernCombatant {
    private final ModernParticipantId identity;
    private final long initialHp, maxHp, initialMp, maxMp, armorClass;
    private final int weaponType;
    private final long evaluatedAtMillis, validUntilMillis, attackRange, lightArrowMpCost;
    private final boolean passive;
    private final ModernPhysicalCombat.Values physical;
    private final ModernMagicCombat.Values magic;
    private final ModernRangedCombat.Values ranged;
    private final ModernFireStorm.Profile fire;

    private ModernCombatant(ModernParticipantId identity, long initialHp, long maxHp,
            long initialMp, long maxMp, long armorClass, int weaponType,
            long evaluatedAtMillis, long validUntilMillis, long attackRange,
            long lightArrowMpCost, boolean passive, ModernPhysicalCombat.Values physical,
            ModernMagicCombat.Values magic,ModernRangedCombat.Values ranged,ModernFireStorm.Profile fire) {
        this.identity = Objects.requireNonNull(identity);
        this.physical = Objects.requireNonNull(physical);
        this.magic = magic;
        this.ranged=ranged;
        this.fire=fire;
        this.initialHp = initialHp; this.maxHp = maxHp;
        this.initialMp = initialMp; this.maxMp = maxMp;
        this.armorClass = armorClass; this.weaponType = weaponType;
        this.evaluatedAtMillis = evaluatedAtMillis; this.validUntilMillis = validUntilMillis;
        this.attackRange = attackRange; this.lightArrowMpCost = lightArrowMpCost;
        this.passive = passive;
    }

    static ModernCombatant human(ModernPhysicalCombat.Fighter fighter) {
        return human(fighter,false);
    }
    static ModernCombatant human(ModernPhysicalCombat.Fighter fighter,boolean bowEnabled) {
        return human(fighter,bowEnabled,false);
    }
    static ModernCombatant human(ModernPhysicalCombat.Fighter fighter,boolean bowEnabled,boolean fireStormEnabled) {
        Objects.requireNonNull(fighter);
        var profile = fighter.profile(); var effective = profile.effective();
        var weapon = profile.weapon(); var magic = fighter.magic();
        return new ModernCombatant(ModernParticipantId.human(fighter.key()),
                effective.initialHp(), effective.maximumHp(), effective.initialMp(), effective.maximumMp(),
                effective.armorClass(), weapon.facts() == null ? 0 : Math.toIntExact(weapon.facts().retainedType()),
                profile.evaluatedAtMillis(), profile.validUntilMillis(), fighter.attackRange(),
                magic == null ? 0 : magic.mpCost(), false, fighter.values(), magic == null ? null : magic.values(),
                bowEnabled?Objects.requireNonNull(fighter.ranged()).values():null,fireStormEnabled?ModernFireStorm.qualify(fighter):null);
    }

    static ModernCombatant bot(ModernParticipantId identity, ModernBotTemplate template) {
        Objects.requireNonNull(identity); Objects.requireNonNull(template);
        if (identity.isHuman()) throw new IllegalArgumentException("bot requires server-bot identity");
        return new ModernCombatant(identity, template.initialHp(), template.maxHp(), template.initialMp(),
                template.maxMp(), template.armorClass(), template.weaponType(), 0, -1,
                template.attackRange(), template.lightArrowMpCost(), template.passive(),
                template.physicalValues(), template.magicValues(),null,null);
    }

    ModernParticipantId identity() { return identity; }
    long initialHp() { return initialHp; }
    long maxHp() { return maxHp; }
    long initialMp() { return initialMp; }
    long maxMp() { return maxMp; }
    long armorClass() { return armorClass; }
    int weaponType() { return weaponType; }
    long evaluatedAtMillis() { return evaluatedAtMillis; }
    long validUntilMillis() { return validUntilMillis; }
    long attackRange() { return attackRange; }
    boolean passive() { return passive; }
    /** Numeric attack/defense qualification, NOT permission for a passive BOT to initiate combat. */
    boolean bow(){return weaponType==4||weaponType==13;}
    boolean physicalSupported() { return bow()&&ranged!=null?ranged.supported():physical.supported(); }
    boolean physicalDefenseSupported(){return ranged==null?physical.supported():ranged.defenseSupported();}
    /** Full magic defense is required even for a BOT whose learned-skill set is empty. */
    boolean magicSupported() { return magic != null && magic.supported(); }
    ModernMagicCombat.Values disintegrateDefense() {
        if(!magicSupported())throw new ModernDisintegrate.CombatException(ModernDisintegrate.Failure.UNSUPPORTED);
        return magic;
    }
    boolean fireStormSupported(){return fire!=null&&fire.values().supported()||!identity.isHuman()&&magicSupported();}
    int fireStormMpCost(){if(fire==null)throw new IllegalStateException("fire storm profile unavailable");return fire.mpCost();}
    ModernFireStorm.Values fireStormValues(){
        if(fire!=null)return fire.values();
        // ROOM_TEST_DUMMY_V1 is an explicitly source-less target, never a human
        // missing catalog/equipment read. Its elemental resistance is zero.
        if(!identity.isHuman()&&magicSupported())return new ModernFireStorm.Values(magic,0);
        throw new ModernFireStorm.CombatException(ModernFireStorm.Failure.UNSUPPORTED);
    }
    ModernFireStorm.Outcome fireStormObjective(long hp,long max,ModernPhysicalCombat.BoundedRandom random,int timedSp) {
        if(passive)throw new ModernFireStorm.CombatException(ModernFireStorm.Failure.UNSUPPORTED);
        var target=new ModernMagicCombat.Values(true,max,new ModernMagicCombat.Offense(0,0,1,0,0,0,0),new ModernMagicCombat.Defense(0,0,0,0,0,0,0));
        return ModernFireStorm.resolve(fireStormValues(),new ModernFireStorm.Values(target,0),hp,random,timedSp);
    }
    long slowLevel(){return physical.offense().level();}
    long slowMr(){if(!magicSupported())throw new IllegalArgumentException("unqualified slow target");return Math.max(0,magic.defense().magicResistance());}
    long lightArrowMpCost() {
        if (magic == null) throw new IllegalStateException("light arrow profile is not qualified");
        return lightArrowMpCost;
    }
    /** Retained LiTo guardian branch: AC10, no NPC/PC reduction, no player ER. */
    ModernPhysicalCombat.Outcome attackObjective(long hp,long max,ModernPhysicalCombat.BoundedRandom random,int penalty,ModernAmmo.Facts arrow) {
        if(passive||!physicalSupported())throw new ModernPhysicalCombat.CombatException(ModernPhysicalCombat.Failure.UNSUPPORTED);
        var defense=new ModernPhysicalCombat.Defense(10,0,0,0,0,0);
        if(bow()) {
            if(ranged==null||arrow==null)throw new ModernPhysicalCombat.CombatException(ModernPhysicalCombat.Failure.UNSUPPORTED);
            var target=new ModernRangedCombat.Values(false,true,max,1,0,new ModernRangedCombat.Offense(0,0,0),defense,defense,null,new ModernPhysicalCombat.Rational(1,1));
            return ModernRangedCombat.resolve(ranged,target,arrow,hp,random,penalty);
        }
        var o=physical.offense();
        long hit=Math.addExact(Math.addExact(Math.addExact(o.level(),o.classHit()),o.growthHit()),Math.addExact(o.equipmentHit(),o.enchantHit()));
        var offense=new ModernPhysicalCombat.Offense(o.level(),o.classHit(),o.growthHit(),0,o.equipmentHit(),o.enchantHit(),hit,
                0,o.growthDamage(),0,o.itemDamage(),o.templateDamage(),o.enchantDamage(),0);
        var outgoing=new ModernPhysicalCombat.Values(true,maxHp,offense,physical.defense(),physical.unarmed(),physical.weapon(),physical.multiplier());
        var target=new ModernPhysicalCombat.Values(true,max,offense,defense,true,null,new ModernPhysicalCombat.Rational(1,1));
        return ModernPhysicalCombat.resolveValues(outgoing,target,hp,random,penalty);
    }
    ModernMagicCombat.Outcome lightArrowObjective(long hp,long max,ModernPhysicalCombat.BoundedRandom random,int timedSp) {
        if(passive||!magicSupported())throw new ModernMagicCombat.CombatException(ModernMagicCombat.Failure.UNSUPPORTED);
        var target=new ModernMagicCombat.Values(true,max,new ModernMagicCombat.Offense(0,0,1,0,0,0,0),new ModernMagicCombat.Defense(0,0,0,0,0,0,0));
        return ModernMagicCombat.resolveValues(magic,target,hp,random,timedSp);
    }

    static ModernPhysicalCombat.Outcome resolvePhysical(ModernCombatant attacker, ModernCombatant defender,
            long currentHp, ModernPhysicalCombat.BoundedRandom random) {
        return resolvePhysical(attacker,defender,currentHp,random,0);
    }
    static ModernPhysicalCombat.Outcome resolvePhysical(ModernCombatant attacker,ModernCombatant defender,
            long currentHp,ModernPhysicalCombat.BoundedRandom random,int weightHitPenalty) {
        return resolvePhysical(attacker,defender,currentHp,random,weightHitPenalty,null);
    }
    static ModernPhysicalCombat.Outcome resolvePhysical(ModernCombatant attacker,ModernCombatant defender,
            long currentHp,ModernPhysicalCombat.BoundedRandom random,int weightHitPenalty,ModernAmmo.Facts arrow) {
        return resolveProtectedPhysical(attacker,defender,currentHp,random,weightHitPenalty,arrow,ModernIncomingDamage.NONE).outcome();
    }
    static ModernIncomingDamage.Resolved<ModernPhysicalCombat.Outcome> resolveProtectedPhysical(ModernCombatant attacker,
            ModernCombatant defender,long currentHp,ModernPhysicalCombat.BoundedRandom random,int weightHitPenalty,
            ModernAmmo.Facts arrow,ModernIncomingDamage.Protection protection) {
        Objects.requireNonNull(attacker); Objects.requireNonNull(defender); Objects.requireNonNull(random);
        Objects.requireNonNull(protection);protection.requireTarget(defender.identity);
        if (attacker.passive || !attacker.physicalSupported() || !defender.physicalDefenseSupported())
            throw new ModernPhysicalCombat.CombatException(ModernPhysicalCombat.Failure.UNSUPPORTED);
        if (attacker.identity.equals(defender.identity))
            throw new ModernPhysicalCombat.CombatException(ModernPhysicalCombat.Failure.INVALID_STATE);
        if(attacker.bow()) {
            if(attacker.ranged==null||arrow==null)throw new ModernPhysicalCombat.CombatException(ModernPhysicalCombat.Failure.UNSUPPORTED);
            return ModernRangedCombat.resolveProtected(attacker.ranged,defender.rangedDefense(),arrow,currentHp,random,weightHitPenalty,protection);
        }
        var values=defender.physical;
        if(defender.ranged!=null)values=new ModernPhysicalCombat.Values(defender.ranged.defenseSupported(),values.maximumHp(),values.offense(),values.defense(),values.unarmed(),values.weapon(),values.multiplier());
        return ModernPhysicalCombat.resolveProtected(attacker.physical, values, currentHp, random,weightHitPenalty,protection);
    }
    private ModernRangedCombat.Values rangedDefense() {
        if(ranged!=null)return ranged;
        // ROOM_TEST_DUMMY_V1 explicitly has no ranged-evasion bonus; no human
        // missing source can use this policy. It never grants a BOT bow attack.
        if(identity.isHuman())throw new ModernPhysicalCombat.CombatException(ModernPhysicalCombat.Failure.UNSUPPORTED);
        var d=physical.defense();return new ModernRangedCombat.Values(false,physical.supported(),maxHp,1,0,
                new ModernRangedCombat.Offense(0,0,0),d,d,null,new ModernPhysicalCombat.Rational(1,1));
    }

    static ModernMagicCombat.Outcome resolveLightArrow(ModernCombatant attacker, ModernCombatant defender,
            long currentHp, ModernPhysicalCombat.BoundedRandom random) {
        return resolveLightArrow(attacker,defender,currentHp,random,0);
    }
    static ModernMagicCombat.Outcome resolveLightArrow(ModernCombatant attacker, ModernCombatant defender,
            long currentHp, ModernPhysicalCombat.BoundedRandom random,int timedSpellPower) {
        return resolveProtectedLightArrow(attacker,defender,currentHp,random,timedSpellPower,ModernIncomingDamage.NONE).outcome();
    }
    static ModernIncomingDamage.Resolved<ModernMagicCombat.Outcome> resolveProtectedLightArrow(ModernCombatant attacker,
            ModernCombatant defender,long currentHp,ModernPhysicalCombat.BoundedRandom random,int timedSpellPower,
            ModernIncomingDamage.Protection protection) {
        Objects.requireNonNull(attacker); Objects.requireNonNull(defender); Objects.requireNonNull(random);
        Objects.requireNonNull(protection);protection.requireTarget(defender.identity);
        if (attacker.passive || !attacker.magicSupported() || !defender.magicSupported())
            throw new ModernMagicCombat.CombatException(ModernMagicCombat.Failure.UNSUPPORTED);
        if (attacker.identity.equals(defender.identity))
            throw new ModernMagicCombat.CombatException(ModernMagicCombat.Failure.INVALID_STATE);
        return ModernMagicCombat.resolveProtected(attacker.magic, defender.magic, currentHp, random,timedSpellPower,protection);
    }

    @Override public String toString() { return "ModernCombatant[redacted]"; }
}
