// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

import java.util.concurrent.TimeUnit;

final class BattleAbilityStatus {
    private long shieldExpiresAtNanos;
    private int shieldSkillId;
    private int shieldModifier;
    private long strengthExpiresAtNanos;
    private int strengthSkillId;
    private int strengthModifier;
    private long dexterityExpiresAtNanos;
    private int dexteritySkillId;
    private int dexterityModifier;
    private long berserkExpiresAtNanos;
    private int berserkArmorClass;
    private int berserkMeleeHit;
    private int berserkMeleeDamage;
    private long immuneToHarmExpiresAtNanos;
    private int immuneToHarmDivisor;
    private long reductionArmorExpiresAtNanos;
    private long bounceAttackExpiresAtNanos;
    private int bounceAttackArmorClass;
    private int bounceAttackMeleeHit;
    private int bounceAttackWeaponDamageChance;
    private long solidCarriageExpiresAtNanos;
    private int solidCarriageEvasionRate;
    private long dressEvasionExpiresAtNanos;
    private int dressEvasionRate;
    private long uncannyDodgeExpiresAtNanos;
    private int uncannyDodgeModifier;
    private long mirrorImageExpiresAtNanos;
    private int mirrorImageDodgeModifier;
    private long illusionOgreExpiresAtNanos;
    private int illusionOgreMeleeHit;
    private int illusionOgreMeleeDamage;
    private long illusionLichExpiresAtNanos;
    private int illusionLichSpellPower;
    private long illusionAvatarExpiresAtNanos;
    private int illusionAvatarMeleeDamage;
    private int illusionAvatarRangedDamage;
    private int illusionAvatarSpellPower;
    private double illusionAvatarIncomingDamageRate;
    private long diamondGolemExpiresAtNanos;
    private int diamondGolemArmorClass;
    private long insightExpiresAtNanos;
    private int insightAbilityScoreModifier;
    private long panicExpiresAtNanos;
    private int panicAbilityScoreModifier;
    private long counterBarrierExpiresAtNanos;
    private long mortalBodyExpiresAtNanos;
    private long royalAuraExpiresAtNanos;
    private int royalAuraSkillId;
    private int royalAuraArmorClass;
    private int royalAuraMeleeHit;
    private int royalAuraRangedHit;
    private int royalAuraMagicResistance;
    private long braveAuraExpiresAtNanos;
    private int braveAuraMeleeDamage;
    private long magicResistanceExpiresAtNanos;
    private int magicResistanceModifier;
    private final CombatBuffLeases sourceBuffLeases = new CombatBuffLeases();
    volatile transient Object nativeSourceProc68IconLease;
    volatile transient long nativeSourceIcons1Version;

    BattleAbilityStatus() {
    }

    synchronized void activateShield(long nowNanos, int durationSeconds, int modifier) {
        activateArmorProtection(BattleSpellCatalog.SHIELD,
                nowNanos, durationSeconds, modifier);
    }

    synchronized void activateArmorProtection(int skillId, long nowNanos, int durationSeconds, int modifier) {
        if (skillId <= 0) {
            throw new IllegalArgumentException("invalid armor-protection skill id");
        }
        BattleAbilityStatus.validateActivation(nowNanos, durationSeconds, modifier);
        this.shieldExpiresAtNanos = BattleAbilityStatus.expiresAt(nowNanos, durationSeconds);
        this.shieldSkillId = skillId;
        this.shieldModifier = modifier;
        this.sourceBuffLeases.activate(skillId, nowNanos, durationSeconds);
    }

    synchronized void activateStrength(long nowNanos, int durationSeconds, int modifier) {
        this.activateStrength(42, nowNanos, durationSeconds, modifier);
    }

    synchronized int activateStrength(int skillId, long nowNanos, int durationSeconds, int modifier) {
        BattleAbilityStatus.validateSkillId(skillId);
        BattleAbilityStatus.validateActivation(nowNanos, durationSeconds, modifier);
        int n4 = BattleAbilityStatus.activeSkillId(nowNanos, this.strengthExpiresAtNanos, this.strengthSkillId);
        this.strengthExpiresAtNanos = BattleAbilityStatus.expiresAt(nowNanos, durationSeconds);
        this.strengthSkillId = skillId;
        this.strengthModifier = modifier;
        return this.sourceBuffLeases.replace(n4 == skillId ? 0 : n4, skillId, nowNanos, durationSeconds);
    }

    synchronized void activateDexterity(long nowNanos, int durationSeconds, int modifier) {
        this.activateDexterity(26, nowNanos, durationSeconds, modifier);
    }

    synchronized int activateDexterity(int skillId, long nowNanos, int durationSeconds, int modifier) {
        BattleAbilityStatus.validateSkillId(skillId);
        BattleAbilityStatus.validateActivation(nowNanos, durationSeconds, modifier);
        int n4 = BattleAbilityStatus.activeSkillId(nowNanos, this.dexterityExpiresAtNanos, this.dexteritySkillId);
        this.dexterityExpiresAtNanos = BattleAbilityStatus.expiresAt(nowNanos, durationSeconds);
        this.dexteritySkillId = skillId;
        this.dexterityModifier = modifier;
        return this.sourceBuffLeases.replace(n4 == skillId ? 0 : n4, skillId, nowNanos, durationSeconds);
    }

    synchronized void activateBerserk(long nowNanos, int durationSeconds, int armorClass, int meleeHit, int meleeDamage) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535 || armorClass <= 0 || armorClass > 127 || meleeHit <= 0 || meleeHit > 127 || meleeDamage <= 0 || meleeDamage > 127) {
            throw new IllegalArgumentException("invalid native Berserkers lease");
        }
        BattleAbilityStatus battleAbilityStatus;
        (battleAbilityStatus = this).berserkExpiresAtNanos = BattleAbilityStatus.expiresAt(nowNanos, durationSeconds);
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        this.berserkArmorClass = armorClass;
        this.berserkMeleeHit = meleeHit;
        this.berserkMeleeDamage = meleeDamage;
    }

    synchronized void activateImmuneToHarm(long nowNanos, int durationSeconds, int divisor) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535 || divisor <= 1 || divisor > 127) {
            throw new IllegalArgumentException("invalid native Immune to Harm lease");
        }
        this.immuneToHarmExpiresAtNanos = BattleAbilityStatus.expiresAt(nowNanos, durationSeconds);
        this.immuneToHarmDivisor = divisor;
        this.nativeSourceProc68IconLease = new Object();
    }

    synchronized void activateReductionArmor(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native Reduction Armor lease");
        }
        reductionArmorExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
    }

    synchronized void activateBounceAttack(
            long nowNanos, int durationSeconds, int armorClass,
            int meleeHit, int weaponDamageChance) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || armorClass >= 0 || armorClass < -127
                || meleeHit <= 0 || meleeHit > 127
                || weaponDamageChance <= 0 || weaponDamageChance > 100) {
            throw new IllegalArgumentException("invalid native Bounce Attack lease");
        }
        bounceAttackExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
        bounceAttackArmorClass = armorClass;
        bounceAttackMeleeHit = meleeHit;
        bounceAttackWeaponDamageChance = weaponDamageChance;
    }

    synchronized void activateSolidCarriage(
            long nowNanos, int durationSeconds, int evasionRate) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || evasionRate <= 0 || evasionRate > 127) {
            throw new IllegalArgumentException("invalid native Solid Carriage lease");
        }
        solidCarriageExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
        solidCarriageEvasionRate = evasionRate;
    }

    synchronized void activateDressEvasion(long nowNanos, int durationSeconds, int evasionRate) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535 || evasionRate <= 0 || evasionRate > 127) {
            throw new IllegalArgumentException("invalid native Dress Evasion lease");
        }
        BattleAbilityStatus battleAbilityStatus;
        (battleAbilityStatus = this).dressEvasionExpiresAtNanos = BattleAbilityStatus.expiresAt(nowNanos, durationSeconds);
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        this.dressEvasionRate = evasionRate;
    }

    synchronized boolean activateUncannyDodge(
            long nowNanos, int durationSeconds, int dodgeModifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || dodgeModifier <= 0 || dodgeModifier > 10) {
            throw new IllegalArgumentException("invalid native Uncanny Dodge lease");
        }
        if (uncannyDodgeExpiresAtNanos != 0
                && nowNanos < uncannyDodgeExpiresAtNanos) {
            return false;
        }
        uncannyDodgeExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
        uncannyDodgeModifier = dodgeModifier;
        return true;
    }

    synchronized boolean activateMirrorImage(
            long nowNanos, int durationSeconds, int dodgeModifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || dodgeModifier <= 0 || dodgeModifier > 10) {
            throw new IllegalArgumentException("invalid native Mirror Image lease");
        }
        if (mirrorImageExpiresAtNanos != 0
                && nowNanos < mirrorImageExpiresAtNanos) {
            return false;
        }
        mirrorImageExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
        mirrorImageDodgeModifier = dodgeModifier;
        return true;
    }

    synchronized boolean activateIllusionOgre(
            long nowNanos, int durationSeconds,
            int meleeHit, int meleeDamage) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || meleeHit <= 0 || meleeHit > 127
                || meleeDamage <= 0 || meleeDamage > 127) {
            throw new IllegalArgumentException(
                    "invalid native Illusion: Ogre lease");
        }
        boolean refreshed = illusionOgreExpiresAtNanos != 0
                && nowNanos < illusionOgreExpiresAtNanos;
        illusionOgreExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
        illusionOgreMeleeHit = meleeHit;
        illusionOgreMeleeDamage = meleeDamage;
        return refreshed;
    }

    synchronized boolean activateIllusionLich(
            long nowNanos, int durationSeconds, int spellPower) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || spellPower <= 0 || spellPower > 127) {
            throw new IllegalArgumentException(
                    "invalid native Illusion: Lich lease");
        }
        boolean refreshed = illusionLichExpiresAtNanos != 0
                && nowNanos < illusionLichExpiresAtNanos;
        illusionLichExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
        illusionLichSpellPower = spellPower;
        return refreshed;
    }

    synchronized boolean activateIllusionAvatar(
            long nowNanos, int durationSeconds,
            int meleeDamage, int rangedDamage, int spellPower,
            double incomingDamageRate) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || meleeDamage <= 0 || meleeDamage > 127
                || rangedDamage <= 0 || rangedDamage > 127
                || spellPower <= 0 || spellPower > 127
                || Double.compare(incomingDamageRate, 1.5D) != 0) {
            throw new IllegalArgumentException(
                    "invalid native Illusion: Avatar lease");
        }
        boolean refreshed = illusionAvatarExpiresAtNanos != 0
                && nowNanos < illusionAvatarExpiresAtNanos;
        illusionAvatarExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
        illusionAvatarMeleeDamage = meleeDamage;
        illusionAvatarRangedDamage = rangedDamage;
        illusionAvatarSpellPower = spellPower;
        illusionAvatarIncomingDamageRate = incomingDamageRate;
        return refreshed;
    }

    synchronized boolean activateDiamondGolem(
            long nowNanos, int durationSeconds, int armorClass) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || armorClass >= 0 || armorClass < -127) {
            throw new IllegalArgumentException(
                    "invalid native Illusion: Diamond Golem lease");
        }
        boolean refreshed = diamondGolemExpiresAtNanos != 0
                && nowNanos < diamondGolemExpiresAtNanos;
        diamondGolemExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
        diamondGolemArmorClass = armorClass;
        return refreshed;
    }

    synchronized boolean activateInsight(long nowNanos, int durationSeconds, int abilityScoreModifier) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535 || abilityScoreModifier <= 0 || abilityScoreModifier > 127) {
            throw new IllegalArgumentException("invalid native Insight lease");
        }
        if (this.insightExpiresAtNanos != 0L && nowNanos < this.insightExpiresAtNanos) {
            return false;
        }
        BattleAbilityStatus battleAbilityStatus;
        (battleAbilityStatus = this).insightExpiresAtNanos = BattleAbilityStatus.expiresAt(nowNanos, durationSeconds);
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        this.insightAbilityScoreModifier = abilityScoreModifier;
        return true;
    }

    synchronized boolean activatePanic(long nowNanos, int durationSeconds, int abilityScoreModifier) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535 || abilityScoreModifier >= 0 || abilityScoreModifier < -127) {
            throw new IllegalArgumentException("invalid native Panic lease");
        }
        if (this.panicExpiresAtNanos != 0L && nowNanos < this.panicExpiresAtNanos) {
            return false;
        }
        BattleAbilityStatus battleAbilityStatus;
        (battleAbilityStatus = this).panicExpiresAtNanos = BattleAbilityStatus.expiresAt(nowNanos, durationSeconds);
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        this.panicAbilityScoreModifier = abilityScoreModifier;
        return true;
    }

    synchronized void activateCounterBarrier(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native Counter Barrier lease");
        }
        counterBarrierExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
    }

    synchronized void activateMortalBody(long nowNanos, int durationSeconds) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535) {
            throw new IllegalArgumentException("invalid native Mortal Body lease");
        }
        BattleAbilityStatus battleAbilityStatus;
        (battleAbilityStatus = this).mortalBodyExpiresAtNanos = BattleAbilityStatus.expiresAt(nowNanos, durationSeconds);
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
    }

    synchronized int activateRoyalAura(int skillId, long nowNanos, int durationSeconds, int armorClass, int meleeHit, int rangedHit, int magicResistance) {
        BattleAbilityStatus.validateSkillId(skillId);
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535 || armorClass < -127 || armorClass > 127 || meleeHit < 0 || meleeHit > 127 || rangedHit < 0 || rangedHit > 127 || magicResistance < 0 || magicResistance > 127 || armorClass + meleeHit + rangedHit + magicResistance == 0) {
            throw new IllegalArgumentException("invalid native royal-aura lease");
        }
        int n7 = BattleAbilityStatus.activeSkillId(nowNanos, this.royalAuraExpiresAtNanos, this.royalAuraSkillId);
        this.royalAuraExpiresAtNanos = BattleAbilityStatus.expiresAt(nowNanos, durationSeconds);
        this.royalAuraSkillId = skillId;
        this.royalAuraArmorClass = armorClass;
        this.royalAuraMeleeHit = meleeHit;
        this.royalAuraRangedHit = rangedHit;
        this.royalAuraMagicResistance = magicResistance;
        return this.sourceBuffLeases.replace(n7 == skillId ? 0 : n7, skillId, nowNanos, durationSeconds);
    }

    synchronized void activateBraveAura(
            long nowNanos, int durationSeconds, int meleeDamage) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || meleeDamage <= 0 || meleeDamage > 127) {
            throw new IllegalArgumentException("invalid native Brave Aura lease");
        }
        braveAuraExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
        braveAuraMeleeDamage = meleeDamage;
    }

    synchronized void activateMagicResistance(
            long nowNanos, int durationSeconds, int modifier) {
        validateActivation(nowNanos, durationSeconds, modifier);
        if (modifier <= 0) {
            throw new IllegalArgumentException(
                    "invalid native Resist Magic lease");
        }
        magicResistanceExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
        magicResistanceModifier = modifier;
    }

    synchronized Modifier shield(long nowNanos) {
        if (CombatBuffLeases.loaded()) {
            return this.sourceBuffLeases.shield(nowNanos);
        }
        return BattleAbilityStatus.modifier(nowNanos, this.shieldExpiresAtNanos, this.shieldModifier);
    }

    synchronized ArmorProtection armorProtection(long nowNanos) {
        if (CombatBuffLeases.loaded()) {
            return this.sourceBuffLeases.armorProtection(nowNanos);
        }
        Modifier modifier = BattleAbilityStatus.modifier(nowNanos, this.shieldExpiresAtNanos, this.shieldModifier);
        if (modifier.remainingSeconds() == 0) {
            return new ArmorProtection(0, 0, 0);
        }
        return new ArmorProtection(this.shieldSkillId, modifier.amount(), modifier.remainingSeconds());
    }

    synchronized Modifier strength(long nowNanos) {
        if (CombatBuffLeases.loaded()) {
            return this.sourceBuffLeases.strength(nowNanos);
        }
        return BattleAbilityStatus.modifier(nowNanos, this.strengthExpiresAtNanos, this.strengthModifier);
    }

    synchronized int strengthSkillId(long nowNanos) {
        if (CombatBuffLeases.loaded()) {
            return this.sourceBuffLeases.strengthSkillId(nowNanos);
        }
        return BattleAbilityStatus.activeSkillId(nowNanos, this.strengthExpiresAtNanos, this.strengthSkillId);
    }

    synchronized Modifier dexterity(long nowNanos) {
        if (CombatBuffLeases.loaded()) {
            return this.sourceBuffLeases.dexterity(nowNanos);
        }
        return BattleAbilityStatus.modifier(nowNanos, this.dexterityExpiresAtNanos, this.dexterityModifier);
    }

    synchronized int dexteritySkillId(long nowNanos) {
        if (CombatBuffLeases.loaded()) {
            return this.sourceBuffLeases.dexteritySkillId(nowNanos);
        }
        return BattleAbilityStatus.activeSkillId(nowNanos, this.dexterityExpiresAtNanos, this.dexteritySkillId);
    }

    synchronized Berserk berserk(long nowNanos) {
        if (this.berserkExpiresAtNanos == 0L || nowNanos >= this.berserkExpiresAtNanos) {
            return new Berserk(0, 0, 0, 0);
        }
        return new Berserk(CombatBuffLeases.value(this.berserkArmorClass, 55, "AC"), CombatBuffLeases.value(this.berserkMeleeHit, 55, "HIT"), CombatBuffLeases.value(this.berserkMeleeDamage, 55, "DMG"), BattleAbilityStatus.modifier(nowNanos, this.berserkExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized Protection immuneToHarm(long nowNanos) {
        if (immuneToHarmExpiresAtNanos == 0
                || nowNanos >= immuneToHarmExpiresAtNanos) {
            return new Protection(0, 0);
        }
        return new Protection(immuneToHarmDivisor,
                modifier(nowNanos, immuneToHarmExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized ReductionArmor reductionArmor(long nowNanos) {
        return new ReductionArmor(
                modifier(nowNanos, reductionArmorExpiresAtNanos, 1)
                        .remainingSeconds());
    }

    synchronized BounceAttack bounceAttack(long nowNanos) {
        if (this.bounceAttackExpiresAtNanos == 0L || nowNanos >= this.bounceAttackExpiresAtNanos) {
            return new BounceAttack(0, 0, 0, 0);
        }
        return new BounceAttack(CombatBuffLeases.value(this.bounceAttackArmorClass, 89, "AC"), CombatBuffLeases.value(this.bounceAttackMeleeHit, 89, "HIT"), this.bounceAttackWeaponDamageChance, BattleAbilityStatus.modifier(nowNanos, this.bounceAttackExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized SolidCarriage solidCarriage(long nowNanos) {
        if (solidCarriageExpiresAtNanos == 0
                || nowNanos >= solidCarriageExpiresAtNanos) {
            return new SolidCarriage(0, 0);
        }
        return new SolidCarriage(solidCarriageEvasionRate,
                modifier(nowNanos, solidCarriageExpiresAtNanos, 1)
                        .remainingSeconds());
    }

    synchronized DressEvasion dressEvasion(long nowNanos) {
        if (dressEvasionExpiresAtNanos == 0
                || nowNanos >= dressEvasionExpiresAtNanos) {
            return new DressEvasion(0, 0);
        }
        return new DressEvasion(dressEvasionRate,
                modifier(nowNanos, dressEvasionExpiresAtNanos, 1)
                        .remainingSeconds());
    }

    synchronized UncannyDodge uncannyDodge(long nowNanos) {
        if (uncannyDodgeExpiresAtNanos == 0
                || nowNanos >= uncannyDodgeExpiresAtNanos) {
            return new UncannyDodge(0, 0);
        }
        return new UncannyDodge(uncannyDodgeModifier,
                modifier(nowNanos, uncannyDodgeExpiresAtNanos, 1)
                        .remainingSeconds());
    }

    synchronized MirrorImage mirrorImage(long nowNanos) {
        if (mirrorImageExpiresAtNanos == 0
                || nowNanos >= mirrorImageExpiresAtNanos) {
            return new MirrorImage(0, 0);
        }
        return new MirrorImage(mirrorImageDodgeModifier,
                modifier(nowNanos, mirrorImageExpiresAtNanos, 1)
                        .remainingSeconds());
    }

    synchronized IllusionOgre illusionOgre(long nowNanos) {
        if (this.illusionOgreExpiresAtNanos == 0L || nowNanos >= this.illusionOgreExpiresAtNanos) {
            return new IllusionOgre(0, 0, 0);
        }
        return new IllusionOgre(CombatBuffLeases.value(this.illusionOgreMeleeHit, 204, "HIT"), CombatBuffLeases.value(this.illusionOgreMeleeDamage, 204, "DMG"), BattleAbilityStatus.modifier(nowNanos, this.illusionOgreExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized IllusionLich illusionLich(long nowNanos) {
        if (illusionLichExpiresAtNanos == 0
                || nowNanos >= illusionLichExpiresAtNanos) {
            return new IllusionLich(0, 0);
        }
        return new IllusionLich(
                illusionLichSpellPower,
                modifier(nowNanos, illusionLichExpiresAtNanos, 1)
                        .remainingSeconds());
    }

    synchronized IllusionAvatar illusionAvatar(long nowNanos) {
        if (this.illusionAvatarExpiresAtNanos == 0L || nowNanos >= this.illusionAvatarExpiresAtNanos) {
            return new IllusionAvatar(0, 0, 0, 1.0, 0);
        }
        return new IllusionAvatar(CombatBuffLeases.value(this.illusionAvatarMeleeDamage, 219, "DMG"), CombatBuffLeases.value(this.illusionAvatarRangedDamage, 219, "BOW_DMG"), this.illusionAvatarSpellPower, this.illusionAvatarIncomingDamageRate, BattleAbilityStatus.modifier(nowNanos, this.illusionAvatarExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized DiamondGolem diamondGolem(long nowNanos) {
        if (this.diamondGolemExpiresAtNanos == 0L || nowNanos >= this.diamondGolemExpiresAtNanos) {
            return new DiamondGolem(0, 0);
        }
        return new DiamondGolem(CombatBuffLeases.value(this.diamondGolemArmorClass, 214, "AC"), BattleAbilityStatus.modifier(nowNanos, this.diamondGolemExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized Insight insight(long nowNanos) {
        if (insightExpiresAtNanos == 0 || nowNanos >= insightExpiresAtNanos) {
            return new Insight(0, 0);
        }
        return new Insight(
                insightAbilityScoreModifier,
                modifier(nowNanos, insightExpiresAtNanos, 1)
                        .remainingSeconds());
    }

    synchronized Panic panic(long nowNanos) {
        if (panicExpiresAtNanos == 0 || nowNanos >= panicExpiresAtNanos) {
            return new Panic(0, 0);
        }
        return new Panic(
                panicAbilityScoreModifier,
                modifier(nowNanos, panicExpiresAtNanos, 1)
                        .remainingSeconds());
    }

    synchronized CounterBarrier counterBarrier(long nowNanos) {
        return new CounterBarrier(
                modifier(nowNanos, counterBarrierExpiresAtNanos, 1)
                        .remainingSeconds());
    }

    synchronized MortalBody mortalBody(long nowNanos) {
        return new MortalBody(
                modifier(nowNanos, mortalBodyExpiresAtNanos, 1)
                        .remainingSeconds());
    }

    synchronized RoyalAura royalAura(long nowNanos) {
        if (CombatBuffLeases.loaded()) {
            return this.sourceBuffLeases.royalAura(nowNanos);
        }
        if (this.royalAuraExpiresAtNanos == 0L || nowNanos >= this.royalAuraExpiresAtNanos) {
            return new RoyalAura(0, 0, 0, 0, 0, 0);
        }
        return new RoyalAura(this.royalAuraSkillId, this.royalAuraArmorClass, this.royalAuraMeleeHit, this.royalAuraRangedHit, this.royalAuraMagicResistance, BattleAbilityStatus.modifier(nowNanos, this.royalAuraExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized BraveAura braveAura(long nowNanos) {
        if (this.braveAuraExpiresAtNanos == 0L || nowNanos >= this.braveAuraExpiresAtNanos) {
            return new BraveAura(0, 0);
        }
        return new BraveAura(CombatBuffLeases.value(this.braveAuraMeleeDamage, 117, "DMG"), BattleAbilityStatus.modifier(nowNanos, this.braveAuraExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized MagicResistance magicResistance(long nowNanos) {
        Modifier active = modifier(nowNanos, magicResistanceExpiresAtNanos,
                magicResistanceModifier);
        return new MagicResistance(active.amount(), active.remainingSeconds());
    }

    synchronized boolean expireShield(long nowNanos) {
        return expireArmorProtection(nowNanos) != 0;
    }

    synchronized int expireArmorProtection(long nowNanos) {
        if (!expired(nowNanos, shieldExpiresAtNanos)) return 0;
        int expiredSkillId = shieldSkillId;
        shieldExpiresAtNanos = 0;
        shieldSkillId = 0;
        shieldModifier = 0;
        return expiredSkillId;
    }

    synchronized boolean expireStrength(long nowNanos) {
        return expireStrengthSkillId(nowNanos) != 0;
    }

    synchronized int expireStrengthSkillId(long nowNanos) {
        if (!expired(nowNanos, strengthExpiresAtNanos)) return 0;
        int expiredSkillId = strengthSkillId;
        strengthExpiresAtNanos = 0;
        strengthSkillId = 0;
        strengthModifier = 0;
        return expiredSkillId;
    }

    synchronized boolean expireDexterity(long nowNanos) {
        return expireDexteritySkillId(nowNanos) != 0;
    }

    synchronized int expireDexteritySkillId(long nowNanos) {
        if (!expired(nowNanos, dexterityExpiresAtNanos)) return 0;
        int expiredSkillId = dexteritySkillId;
        dexterityExpiresAtNanos = 0;
        dexteritySkillId = 0;
        dexterityModifier = 0;
        return expiredSkillId;
    }

    synchronized boolean removeSkill(int skillId) {
        if (skillId == this.shieldSkillId && this.shieldExpiresAtNanos != 0L) {
            this.shieldExpiresAtNanos = 0L;
            this.shieldSkillId = 0;
            this.shieldModifier = 0;
            return this.sourceBuffLeases.remove(true, skillId);
        }
        if (skillId == this.strengthSkillId && this.strengthExpiresAtNanos != 0L) {
            this.strengthExpiresAtNanos = 0L;
            this.strengthSkillId = 0;
            this.strengthModifier = 0;
            return this.sourceBuffLeases.remove(true, skillId);
        }
        if (skillId == this.dexteritySkillId && this.dexterityExpiresAtNanos != 0L) {
            this.dexterityExpiresAtNanos = 0L;
            this.dexteritySkillId = 0;
            this.dexterityModifier = 0;
            return this.sourceBuffLeases.remove(true, skillId);
        }
        if (skillId == 55 && this.berserkExpiresAtNanos != 0L) {
            this.clearBerserk();
            return this.sourceBuffLeases.remove(true, skillId);
        }
        return this.sourceBuffLeases.remove(false, skillId);
    }

    synchronized boolean expireBerserk(long nowNanos) {
        if (!expired(nowNanos, berserkExpiresAtNanos)) return false;
        clearBerserk();
        return true;
    }

    synchronized boolean expireImmuneToHarm(long nowNanos) {
        if (!expired(nowNanos, immuneToHarmExpiresAtNanos)) return false;
        clearImmuneToHarm();
        return true;
    }

    synchronized boolean expireReductionArmor(long nowNanos) {
        if (!expired(nowNanos, reductionArmorExpiresAtNanos)) return false;
        reductionArmorExpiresAtNanos = 0;
        return true;
    }

    synchronized boolean expireBounceAttack(long nowNanos) {
        if (!expired(nowNanos, bounceAttackExpiresAtNanos)) return false;
        clearBounceAttack();
        return true;
    }

    synchronized boolean expireSolidCarriage(long nowNanos) {
        if (!expired(nowNanos, solidCarriageExpiresAtNanos)) return false;
        clearSolidCarriage();
        return true;
    }

    synchronized boolean expireDressEvasion(long nowNanos) {
        if (!expired(nowNanos, dressEvasionExpiresAtNanos)) return false;
        clearDressEvasion();
        return true;
    }

    synchronized boolean expireUncannyDodge(long nowNanos) {
        if (!expired(nowNanos, uncannyDodgeExpiresAtNanos)) return false;
        clearUncannyDodge();
        return true;
    }

    synchronized boolean expireMirrorImage(long nowNanos) {
        if (!expired(nowNanos, mirrorImageExpiresAtNanos)) return false;
        clearMirrorImage();
        return true;
    }

    synchronized boolean expireIllusionOgre(long nowNanos) {
        if (!expired(nowNanos, illusionOgreExpiresAtNanos)) return false;
        clearIllusionOgre();
        return true;
    }

    synchronized boolean expireIllusionLich(long nowNanos) {
        if (!expired(nowNanos, illusionLichExpiresAtNanos)) return false;
        clearIllusionLich();
        return true;
    }

    synchronized boolean expireIllusionAvatar(long nowNanos) {
        if (!expired(nowNanos, illusionAvatarExpiresAtNanos)) return false;
        clearIllusionAvatar();
        return true;
    }

    synchronized boolean expireDiamondGolem(long nowNanos) {
        if (!expired(nowNanos, diamondGolemExpiresAtNanos)) return false;
        clearDiamondGolem();
        return true;
    }

    synchronized boolean expireInsight(long nowNanos) {
        if (!expired(nowNanos, insightExpiresAtNanos)) return false;
        clearInsight();
        return true;
    }

    synchronized boolean expirePanic(long nowNanos) {
        if (!expired(nowNanos, panicExpiresAtNanos)) return false;
        clearPanic();
        return true;
    }

    synchronized boolean removeSolidCarriage() {
        if (solidCarriageExpiresAtNanos == 0) return false;
        clearSolidCarriage();
        return true;
    }

    synchronized boolean expireCounterBarrier(long nowNanos) {
        if (!expired(nowNanos, counterBarrierExpiresAtNanos)) return false;
        counterBarrierExpiresAtNanos = 0;
        return true;
    }

    synchronized boolean removeCounterBarrier() {
        if (counterBarrierExpiresAtNanos == 0) return false;
        counterBarrierExpiresAtNanos = 0;
        return true;
    }

    synchronized boolean expireMortalBody(long nowNanos) {
        if (!BattleAbilityStatus.expired(nowNanos, this.mortalBodyExpiresAtNanos)) {
            return false;
        }
        BattleAbilityStatus battleAbilityStatus;
        (battleAbilityStatus = this).mortalBodyExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        return true;
    }

    synchronized boolean removeMortalBody() {
        if (this.mortalBodyExpiresAtNanos == 0L) {
            return false;
        }
        BattleAbilityStatus battleAbilityStatus;
        (battleAbilityStatus = this).mortalBodyExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        return true;
    }

    synchronized int expireRoyalAura(long nowNanos) {
        if (!expired(nowNanos, royalAuraExpiresAtNanos)) return 0;
        int expiredSkillId = royalAuraSkillId;
        clearRoyalAura();
        return expiredSkillId;
    }

    synchronized boolean expireBraveAura(long nowNanos) {
        if (!expired(nowNanos, braveAuraExpiresAtNanos)) return false;
        clearBraveAura();
        return true;
    }

    synchronized boolean expireMagicResistance(long nowNanos) {
        if (!expired(nowNanos, magicResistanceExpiresAtNanos)) return false;
        clearMagicResistance();
        return true;
    }

    synchronized void clear() {
        this.clearCancelable();
        this.reductionArmorExpiresAtNanos = 0L;
        this.clearSolidCarriage();
        this.clearUncannyDodge();
        this.counterBarrierExpiresAtNanos = 0L;
        BattleAbilityStatus battleAbilityStatus;
        (battleAbilityStatus = this).mortalBodyExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        this.clearRoyalAura();
        this.clearBraveAura();
        this.clearMagicResistance();
        this.sourceBuffLeases.clear();
    }

    private void clearCancelable() {
        this.shieldExpiresAtNanos = 0L;
        this.shieldSkillId = 0;
        this.shieldModifier = 0;
        this.strengthExpiresAtNanos = 0L;
        this.strengthSkillId = 0;
        this.strengthModifier = 0;
        this.dexterityExpiresAtNanos = 0L;
        this.dexteritySkillId = 0;
        this.dexterityModifier = 0;
        this.clearBerserk();
        this.clearImmuneToHarm();
        this.clearBounceAttack();
        this.clearUncannyDodge();
        this.clearMirrorImage();
        this.clearIllusionOgre();
        this.clearIllusionLich();
        this.clearIllusionAvatar();
        this.clearDiamondGolem();
        this.clearInsight();
        this.clearPanic();
        this.clearDressEvasion();
        BattleAbilityStatus battleAbilityStatus;
        (battleAbilityStatus = this).mortalBodyExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        this.clearRoyalAura();
        this.clearBraveAura();
        this.clearMagicResistance();
        this.sourceBuffLeases.cancel();
    }

    synchronized Cancellation cancelMagicEffects() {
        Cancellation result = new Cancellation(
                shieldExpiresAtNanos == 0 ? 0 : shieldSkillId,
                strengthExpiresAtNanos == 0 ? 0 : strengthSkillId,
                dexterityExpiresAtNanos == 0 ? 0 : dexteritySkillId,
                royalAuraExpiresAtNanos == 0 ? 0 : royalAuraSkillId,
                braveAuraExpiresAtNanos == 0 ? 0 : BattleSpellCatalog.BRAVE_AURA,
                magicResistanceExpiresAtNanos != 0,
                berserkExpiresAtNanos != 0,
                immuneToHarmExpiresAtNanos != 0,
                bounceAttackExpiresAtNanos != 0,
                uncannyDodgeExpiresAtNanos != 0,
                mirrorImageExpiresAtNanos != 0,
                illusionOgreExpiresAtNanos != 0,
                illusionLichExpiresAtNanos != 0,
                illusionAvatarExpiresAtNanos != 0,
                diamondGolemExpiresAtNanos != 0,
                insightExpiresAtNanos != 0,
                panicExpiresAtNanos != 0,
                dressEvasionExpiresAtNanos != 0,
                mortalBodyExpiresAtNanos != 0);
        clearCancelable();
        return result;
    }

    private void clearBerserk() {
        BattleAbilityStatus battleAbilityStatus;
        (battleAbilityStatus = this).berserkExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        this.berserkArmorClass = 0;
        this.berserkMeleeHit = 0;
        this.berserkMeleeDamage = 0;
    }

    private void clearImmuneToHarm() {
        this.immuneToHarmExpiresAtNanos = 0L;
        this.immuneToHarmDivisor = 0;
        this.nativeSourceProc68IconLease = null;
    }

    private void clearBounceAttack() {
        bounceAttackExpiresAtNanos = 0;
        bounceAttackArmorClass = 0;
        bounceAttackMeleeHit = 0;
        bounceAttackWeaponDamageChance = 0;
    }

    private void clearSolidCarriage() {
        solidCarriageExpiresAtNanos = 0;
        solidCarriageEvasionRate = 0;
    }

    private void clearUncannyDodge() {
        uncannyDodgeExpiresAtNanos = 0;
        uncannyDodgeModifier = 0;
    }

    private void clearMirrorImage() {
        mirrorImageExpiresAtNanos = 0;
        mirrorImageDodgeModifier = 0;
    }

    private void clearIllusionOgre() {
        illusionOgreExpiresAtNanos = 0;
        illusionOgreMeleeHit = 0;
        illusionOgreMeleeDamage = 0;
    }

    private void clearIllusionLich() {
        illusionLichExpiresAtNanos = 0;
        illusionLichSpellPower = 0;
    }

    private void clearIllusionAvatar() {
        illusionAvatarExpiresAtNanos = 0;
        illusionAvatarMeleeDamage = 0;
        illusionAvatarRangedDamage = 0;
        illusionAvatarSpellPower = 0;
        illusionAvatarIncomingDamageRate = 0.0D;
    }

    private void clearDiamondGolem() {
        diamondGolemExpiresAtNanos = 0;
        diamondGolemArmorClass = 0;
    }

    private void clearInsight() {
        BattleAbilityStatus battleAbilityStatus;
        (battleAbilityStatus = this).insightExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        this.insightAbilityScoreModifier = 0;
    }

    private void clearPanic() {
        BattleAbilityStatus battleAbilityStatus;
        (battleAbilityStatus = this).panicExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        this.panicAbilityScoreModifier = 0;
    }

    private void clearDressEvasion() {
        BattleAbilityStatus battleAbilityStatus;
        (battleAbilityStatus = this).dressEvasionExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        this.dressEvasionRate = 0;
    }

    private void clearRoyalAura() {
        royalAuraExpiresAtNanos = 0;
        royalAuraSkillId = 0;
        royalAuraArmorClass = 0;
        royalAuraMeleeHit = 0;
        royalAuraRangedHit = 0;
        royalAuraMagicResistance = 0;
    }

    private void clearBraveAura() {
        braveAuraExpiresAtNanos = 0;
        braveAuraMeleeDamage = 0;
    }

    private void clearMagicResistance() {
        magicResistanceExpiresAtNanos = 0;
        magicResistanceModifier = 0;
    }

    private static void validateActivation(long nowNanos, int durationSeconds, int modifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || modifier == 0 || modifier < -127 || modifier > 127) {
            throw new IllegalArgumentException("invalid native ability-buff lease");
        }
    }

    private static void validateSkillId(int skillId) {
        if (skillId <= 0) {
            throw new IllegalArgumentException("invalid native ability-buff skill id");
        }
    }

    private static long expiresAt(long nowNanos, int durationSeconds) {
        return Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
    }

    private static boolean expired(long nowNanos, long expiresAtNanos) {
        return expiresAtNanos != 0 && nowNanos >= expiresAtNanos;
    }

    private static int activeSkillId(
            long nowNanos, long expiresAtNanos, int skillId) {
        return expiresAtNanos != 0 && nowNanos < expiresAtNanos ? skillId : 0;
    }

    private static Modifier modifier(long nowNanos, long expiresAtNanos, int amount) {
        if (expiresAtNanos == 0 || nowNanos >= expiresAtNanos) return new Modifier(0, 0);
        long remainingNanos = expiresAtNanos - nowNanos;
        long remainingSeconds = (remainingNanos + TimeUnit.SECONDS.toNanos(1) - 1)
                / TimeUnit.SECONDS.toNanos(1);
        return new Modifier(amount,
                (int) Math.min(0xFFFFL, Math.max(1L, remainingSeconds)));
    }

    record Modifier(int amount, int remainingSeconds) {
    }

    record ArmorProtection(int skillId, int armorClass, int remainingSeconds) {
    }

    record Berserk(int armorClass, int meleeHit, int meleeDamage, int remainingSeconds) {
    }

    record Protection(int divisor, int remainingSeconds) {
        int applyTo(int damage) {
            if (damage < 0) {
                throw new IllegalArgumentException("negative combat damage");
            }
            return this.divisor == 0 ? damage : CombatRules.protectedDamage(damage);
        }
    }

    record ReductionArmor(int remainingSeconds) {
    }

    record BounceAttack(int armorClass, int meleeHit, int weaponDamageChance, int remainingSeconds) {
    }

    record SolidCarriage(int evasionRate, int remainingSeconds) {
    }

    record DressEvasion(int evasionRate, int remainingSeconds) {
    }

    record UncannyDodge(int dodgeModifier, int remainingSeconds) {
    }

    record MirrorImage(int dodgeModifier, int remainingSeconds) {
    }

    record IllusionOgre(int meleeHit, int meleeDamage, int remainingSeconds) {
    }

    record IllusionLich(int spellPower, int remainingSeconds) {
    }

    record IllusionAvatar(int meleeDamage, int rangedDamage, int spellPower, double incomingDamageRate, int remainingSeconds) {
        int applyIncomingDamage(int damage) {
            if (damage < 0) {
                throw new IllegalArgumentException("negative incoming damage");
            }
            return (int)((double)damage * CombatRules.avatarIncomingRate(this.incomingDamageRate, this.remainingSeconds()));
        }

        public double incomingDamageRate() {
            return CombatRules.avatarIncomingRate(this.incomingDamageRate, this.remainingSeconds());
        }
    }

    record DiamondGolem(int armorClass, int remainingSeconds) {
    }

    record Insight(int abilityScoreModifier, int remainingSeconds) {
    }

    record Panic(int abilityScoreModifier, int remainingSeconds) {
    }

    record CounterBarrier(int remainingSeconds) {
    }

    record MortalBody(int remainingSeconds) {
    }

    record RoyalAura(int skillId, int armorClass, int meleeHit, int rangedHit, int magicResistance, int remainingSeconds) {
    }

    record BraveAura(int meleeDamage, int remainingSeconds) {
    }

    record MagicResistance(int amount, int remainingSeconds) {
    }

    record Cancellation(int armorProtectionSkillId, int strengthSkillId, int dexteritySkillId, int royalAuraSkillId, int braveAuraSkillId, boolean magicResistance, boolean berserkers, boolean immuneToHarm, boolean bounceAttack, boolean uncannyDodge, boolean mirrorImage, boolean illusionOgre, boolean illusionLich, boolean illusionAvatar, boolean diamondGolem, boolean insight, boolean panic, boolean dressEvasion, boolean mortalBody) {
        boolean shield() {
            return armorProtectionSkillId != 0;
        }

        boolean strength() {
            return strengthSkillId != 0;
        }

        boolean dexterity() {
            return dexteritySkillId != 0;
        }
    }
}
