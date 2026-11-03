package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Server-authoritative leases for native ability and damage-protection buffs. */
final class BattleAbilityStatus {
    record Modifier(int amount, int remainingSeconds) {}
    record ArmorProtection(int skillId, int armorClass, int remainingSeconds) {}
    record Berserk(int armorClass, int meleeHit, int meleeDamage,
                   int remainingSeconds) {}
    record Protection(int divisor, int remainingSeconds) {
        int applyTo(int damage) {
            if (damage < 0) throw new IllegalArgumentException("negative combat damage");
            return divisor == 0 ? damage : damage / divisor;
        }
    }
    record ReductionArmor(int remainingSeconds) {}
    record BounceAttack(int armorClass, int meleeHit,
                        int weaponDamageChance, int remainingSeconds) {}
    record SolidCarriage(int evasionRate, int remainingSeconds) {}
    record DressEvasion(int evasionRate, int remainingSeconds) {}
    record UncannyDodge(int dodgeModifier, int remainingSeconds) {}
    record MirrorImage(int dodgeModifier, int remainingSeconds) {}
    record IllusionOgre(int meleeHit, int meleeDamage, int remainingSeconds) {}
    record IllusionLich(int spellPower, int remainingSeconds) {}
    record IllusionAvatar(int meleeDamage, int rangedDamage, int spellPower,
                          double incomingDamageRate, int remainingSeconds) {
        int applyIncomingDamage(int damage) {
            if (damage < 0) {
                throw new IllegalArgumentException("negative incoming damage");
            }
            // Retained L1PcInstance multiplies the double and then casts the
            // completed value to int, which truncates fractional halves.
            return (int) (damage * incomingDamageRate);
        }
    }
    record DiamondGolem(int armorClass, int remainingSeconds) {}
    record Insight(int abilityScoreModifier, int remainingSeconds) {}
    record Panic(int abilityScoreModifier, int remainingSeconds) {}
    record CounterBarrier(int remainingSeconds) {}
    record MortalBody(int remainingSeconds) {}
    record RoyalAura(int skillId, int armorClass, int meleeHit, int rangedHit,
                     int magicResistance, int remainingSeconds) {}
    record BraveAura(int meleeDamage, int remainingSeconds) {}
    record MagicResistance(int amount, int remainingSeconds) {}
    record Cancellation(int armorProtectionSkillId,
                        int strengthSkillId, int dexteritySkillId,
                        int royalAuraSkillId, int braveAuraSkillId,
                        boolean magicResistance,
                        boolean berserkers, boolean immuneToHarm,
                        boolean bounceAttack, boolean uncannyDodge,
                        boolean mirrorImage,
                        boolean illusionOgre,
                        boolean illusionLich,
                        boolean illusionAvatar,
                        boolean diamondGolem,
                        boolean insight,
                        boolean panic,
                        boolean dressEvasion, boolean mortalBody) {
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

    synchronized void activateShield(long nowNanos, int durationSeconds, int modifier) {
        activateArmorProtection(BattleSpellCatalog.SHIELD,
                nowNanos, durationSeconds, modifier);
    }

    synchronized void activateArmorProtection(
            int skillId, long nowNanos, int durationSeconds, int modifier) {
        if (skillId <= 0) {
            throw new IllegalArgumentException("invalid armor-protection skill id");
        }
        validateActivation(nowNanos, durationSeconds, modifier);
        shieldExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
        shieldSkillId = skillId;
        shieldModifier = modifier;
    }

    synchronized void activateStrength(long nowNanos, int durationSeconds, int modifier) {
        activateStrength(BattleSpellCatalog.PHYSICAL_ENCHANT_STR,
                nowNanos, durationSeconds, modifier);
    }

    synchronized int activateStrength(
            int skillId, long nowNanos, int durationSeconds, int modifier) {
        validateSkillId(skillId);
        validateActivation(nowNanos, durationSeconds, modifier);
        int replacedSkillId = activeSkillId(
                nowNanos, strengthExpiresAtNanos, strengthSkillId);
        strengthExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
        strengthSkillId = skillId;
        strengthModifier = modifier;
        return replacedSkillId == skillId ? 0 : replacedSkillId;
    }

    synchronized void activateDexterity(long nowNanos, int durationSeconds, int modifier) {
        activateDexterity(BattleSpellCatalog.PHYSICAL_ENCHANT_DEX,
                nowNanos, durationSeconds, modifier);
    }

    synchronized int activateDexterity(
            int skillId, long nowNanos, int durationSeconds, int modifier) {
        validateSkillId(skillId);
        validateActivation(nowNanos, durationSeconds, modifier);
        int replacedSkillId = activeSkillId(
                nowNanos, dexterityExpiresAtNanos, dexteritySkillId);
        dexterityExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
        dexteritySkillId = skillId;
        dexterityModifier = modifier;
        return replacedSkillId == skillId ? 0 : replacedSkillId;
    }

    synchronized void activateBerserk(long nowNanos, int durationSeconds,
                                      int armorClass, int meleeHit,
                                      int meleeDamage) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || armorClass <= 0 || armorClass > 127
                || meleeHit <= 0 || meleeHit > 127
                || meleeDamage <= 0 || meleeDamage > 127) {
            throw new IllegalArgumentException("invalid native Berserkers lease");
        }
        berserkExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
        berserkArmorClass = armorClass;
        berserkMeleeHit = meleeHit;
        berserkMeleeDamage = meleeDamage;
    }

    synchronized void activateImmuneToHarm(
            long nowNanos, int durationSeconds, int divisor) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || divisor <= 1 || divisor > 127) {
            throw new IllegalArgumentException("invalid native Immune to Harm lease");
        }
        immuneToHarmExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
        immuneToHarmDivisor = divisor;
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

    synchronized void activateDressEvasion(
            long nowNanos, int durationSeconds, int evasionRate) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || evasionRate <= 0 || evasionRate > 127) {
            throw new IllegalArgumentException("invalid native Dress Evasion lease");
        }
        dressEvasionExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
        dressEvasionRate = evasionRate;
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

    /**
     * Installs retained skill216 exactly: an active Insight lease neither stacks
     * nor refreshes. The return value is true only when a new lease was applied.
     */
    synchronized boolean activateInsight(
            long nowNanos, int durationSeconds, int abilityScoreModifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || abilityScoreModifier <= 0 || abilityScoreModifier > 127) {
            throw new IllegalArgumentException("invalid native Insight lease");
        }
        if (insightExpiresAtNanos != 0 && nowNanos < insightExpiresAtNanos) {
            return false;
        }
        insightExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
        insightAbilityScoreModifier = abilityScoreModifier;
        return true;
    }

    /**
     * Installs retained skill217 exactly: an active Panic lease neither stacks
     * nor refreshes. The return value is true only when a new lease was applied.
     */
    synchronized boolean activatePanic(
            long nowNanos, int durationSeconds, int abilityScoreModifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || abilityScoreModifier >= 0 || abilityScoreModifier < -127) {
            throw new IllegalArgumentException("invalid native Panic lease");
        }
        if (panicExpiresAtNanos != 0 && nowNanos < panicExpiresAtNanos) {
            return false;
        }
        panicExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
        panicAbilityScoreModifier = abilityScoreModifier;
        return true;
    }

    synchronized void activateCounterBarrier(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native Counter Barrier lease");
        }
        counterBarrierExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
    }

    synchronized void activateMortalBody(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native Mortal Body lease");
        }
        mortalBodyExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
    }

    synchronized int activateRoyalAura(
            int skillId, long nowNanos, int durationSeconds,
            int armorClass, int meleeHit, int rangedHit, int magicResistance) {
        validateSkillId(skillId);
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || armorClass < -127 || armorClass > 127
                || meleeHit < 0 || meleeHit > 127
                || rangedHit < 0 || rangedHit > 127
                || magicResistance < 0 || magicResistance > 127
                || armorClass + meleeHit + rangedHit + magicResistance == 0) {
            throw new IllegalArgumentException("invalid native royal-aura lease");
        }
        int replacedSkillId = activeSkillId(
                nowNanos, royalAuraExpiresAtNanos, royalAuraSkillId);
        royalAuraExpiresAtNanos = expiresAt(nowNanos, durationSeconds);
        royalAuraSkillId = skillId;
        royalAuraArmorClass = armorClass;
        royalAuraMeleeHit = meleeHit;
        royalAuraRangedHit = rangedHit;
        royalAuraMagicResistance = magicResistance;
        return replacedSkillId == skillId ? 0 : replacedSkillId;
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
        return modifier(nowNanos, shieldExpiresAtNanos, shieldModifier);
    }

    synchronized ArmorProtection armorProtection(long nowNanos) {
        Modifier active = modifier(nowNanos, shieldExpiresAtNanos, shieldModifier);
        if (active.remainingSeconds() == 0) {
            return new ArmorProtection(0, 0, 0);
        }
        return new ArmorProtection(
                shieldSkillId, active.amount(), active.remainingSeconds());
    }

    synchronized Modifier strength(long nowNanos) {
        return modifier(nowNanos, strengthExpiresAtNanos, strengthModifier);
    }

    synchronized int strengthSkillId(long nowNanos) {
        return activeSkillId(nowNanos, strengthExpiresAtNanos, strengthSkillId);
    }

    synchronized Modifier dexterity(long nowNanos) {
        return modifier(nowNanos, dexterityExpiresAtNanos, dexterityModifier);
    }

    synchronized int dexteritySkillId(long nowNanos) {
        return activeSkillId(nowNanos, dexterityExpiresAtNanos, dexteritySkillId);
    }

    synchronized Berserk berserk(long nowNanos) {
        if (berserkExpiresAtNanos == 0 || nowNanos >= berserkExpiresAtNanos) {
            return new Berserk(0, 0, 0, 0);
        }
        return new Berserk(berserkArmorClass, berserkMeleeHit,
                berserkMeleeDamage,
                modifier(nowNanos, berserkExpiresAtNanos, 1).remainingSeconds());
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
        if (bounceAttackExpiresAtNanos == 0
                || nowNanos >= bounceAttackExpiresAtNanos) {
            return new BounceAttack(0, 0, 0, 0);
        }
        return new BounceAttack(
                bounceAttackArmorClass, bounceAttackMeleeHit,
                bounceAttackWeaponDamageChance,
                modifier(nowNanos, bounceAttackExpiresAtNanos, 1)
                        .remainingSeconds());
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
        if (illusionOgreExpiresAtNanos == 0
                || nowNanos >= illusionOgreExpiresAtNanos) {
            return new IllusionOgre(0, 0, 0);
        }
        return new IllusionOgre(
                illusionOgreMeleeHit, illusionOgreMeleeDamage,
                modifier(nowNanos, illusionOgreExpiresAtNanos, 1)
                        .remainingSeconds());
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
        if (illusionAvatarExpiresAtNanos == 0
                || nowNanos >= illusionAvatarExpiresAtNanos) {
            return new IllusionAvatar(0, 0, 0, 1.0D, 0);
        }
        return new IllusionAvatar(
                illusionAvatarMeleeDamage, illusionAvatarRangedDamage,
                illusionAvatarSpellPower, illusionAvatarIncomingDamageRate,
                modifier(nowNanos, illusionAvatarExpiresAtNanos, 1)
                        .remainingSeconds());
    }

    synchronized DiamondGolem diamondGolem(long nowNanos) {
        if (diamondGolemExpiresAtNanos == 0
                || nowNanos >= diamondGolemExpiresAtNanos) {
            return new DiamondGolem(0, 0);
        }
        return new DiamondGolem(
                diamondGolemArmorClass,
                modifier(nowNanos, diamondGolemExpiresAtNanos, 1)
                        .remainingSeconds());
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
        if (royalAuraExpiresAtNanos == 0 || nowNanos >= royalAuraExpiresAtNanos) {
            return new RoyalAura(0, 0, 0, 0, 0, 0);
        }
        return new RoyalAura(
                royalAuraSkillId, royalAuraArmorClass,
                royalAuraMeleeHit, royalAuraRangedHit,
                royalAuraMagicResistance,
                modifier(nowNanos, royalAuraExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized BraveAura braveAura(long nowNanos) {
        if (braveAuraExpiresAtNanos == 0 || nowNanos >= braveAuraExpiresAtNanos) {
            return new BraveAura(0, 0);
        }
        return new BraveAura(braveAuraMeleeDamage,
                modifier(nowNanos, braveAuraExpiresAtNanos, 1).remainingSeconds());
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

    /** Removes only the named lease, matching L1PcInstance.removeSkillEffect. */
    synchronized boolean removeSkill(int skillId) {
        if (skillId == shieldSkillId && shieldExpiresAtNanos != 0) {
            shieldExpiresAtNanos = 0;
            shieldSkillId = 0;
            shieldModifier = 0;
            return true;
        }
        if (skillId == strengthSkillId && strengthExpiresAtNanos != 0) {
            strengthExpiresAtNanos = 0;
            strengthSkillId = 0;
            strengthModifier = 0;
            return true;
        }
        if (skillId == dexteritySkillId && dexterityExpiresAtNanos != 0) {
            dexterityExpiresAtNanos = 0;
            dexteritySkillId = 0;
            dexterityModifier = 0;
            return true;
        }
        if (skillId == BattleSpellCatalog.BERSERKERS
                && berserkExpiresAtNanos != 0) {
            clearBerserk();
            return true;
        }
        return false;
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
        if (!expired(nowNanos, mortalBodyExpiresAtNanos)) return false;
        mortalBodyExpiresAtNanos = 0;
        return true;
    }

    synchronized boolean removeMortalBody() {
        if (mortalBodyExpiresAtNanos == 0) return false;
        mortalBodyExpiresAtNanos = 0;
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
        clearCancelable();
        reductionArmorExpiresAtNanos = 0;
        clearSolidCarriage();
        clearUncannyDodge();
        counterBarrierExpiresAtNanos = 0;
        mortalBodyExpiresAtNanos = 0;
        clearRoyalAura();
        clearBraveAura();
        clearMagicResistance();
    }

    private void clearCancelable() {
        shieldExpiresAtNanos = 0;
        shieldSkillId = 0;
        shieldModifier = 0;
        strengthExpiresAtNanos = 0;
        strengthSkillId = 0;
        strengthModifier = 0;
        dexterityExpiresAtNanos = 0;
        dexteritySkillId = 0;
        dexterityModifier = 0;
        clearBerserk();
        clearImmuneToHarm();
        clearBounceAttack();
        clearUncannyDodge();
        clearMirrorImage();
        clearIllusionOgre();
        clearIllusionLich();
        clearIllusionAvatar();
        clearDiamondGolem();
        clearInsight();
        clearPanic();
        clearDressEvasion();
        mortalBodyExpiresAtNanos = 0;
        clearRoyalAura();
        clearBraveAura();
        clearMagicResistance();
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
        berserkExpiresAtNanos = 0;
        berserkArmorClass = 0;
        berserkMeleeHit = 0;
        berserkMeleeDamage = 0;
    }

    private void clearImmuneToHarm() {
        immuneToHarmExpiresAtNanos = 0;
        immuneToHarmDivisor = 0;
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
        insightExpiresAtNanos = 0;
        insightAbilityScoreModifier = 0;
    }

    private void clearPanic() {
        panicExpiresAtNanos = 0;
        panicAbilityScoreModifier = 0;
    }

    private void clearDressEvasion() {
        dressEvasionExpiresAtNanos = 0;
        dressEvasionRate = 0;
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
}
