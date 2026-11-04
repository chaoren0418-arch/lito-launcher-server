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

    synchronized void activateShield(long l, int n, int n2) {
        this.activateArmorProtection(3, l, n, n2);
    }

    synchronized void activateArmorProtection(int n, long l, int n2, int n3) {
        if (n <= 0) {
            throw new IllegalArgumentException("invalid armor-protection skill id");
        }
        BattleAbilityStatus.validateActivation(l, n2, n3);
        this.shieldExpiresAtNanos = BattleAbilityStatus.expiresAt(l, n2);
        this.shieldSkillId = n;
        this.shieldModifier = n3;
        this.sourceBuffLeases.activate(n, l, n2);
    }

    synchronized void activateStrength(long l, int n, int n2) {
        this.activateStrength(42, l, n, n2);
    }

    synchronized int activateStrength(int n, long l, int n2, int n3) {
        BattleAbilityStatus.validateSkillId(n);
        BattleAbilityStatus.validateActivation(l, n2, n3);
        int n4 = BattleAbilityStatus.activeSkillId(l, this.strengthExpiresAtNanos, this.strengthSkillId);
        this.strengthExpiresAtNanos = BattleAbilityStatus.expiresAt(l, n2);
        this.strengthSkillId = n;
        this.strengthModifier = n3;
        return this.sourceBuffLeases.replace(n4 == n ? 0 : n4, n, l, n2);
    }

    synchronized void activateDexterity(long l, int n, int n2) {
        this.activateDexterity(26, l, n, n2);
    }

    synchronized int activateDexterity(int n, long l, int n2, int n3) {
        BattleAbilityStatus.validateSkillId(n);
        BattleAbilityStatus.validateActivation(l, n2, n3);
        int n4 = BattleAbilityStatus.activeSkillId(l, this.dexterityExpiresAtNanos, this.dexteritySkillId);
        this.dexterityExpiresAtNanos = BattleAbilityStatus.expiresAt(l, n2);
        this.dexteritySkillId = n;
        this.dexterityModifier = n3;
        return this.sourceBuffLeases.replace(n4 == n ? 0 : n4, n, l, n2);
    }

    synchronized void activateBerserk(long l, int n, int n2, int n3, int n4) {
        if (l < 0L || n <= 0 || n > 65535 || n2 <= 0 || n2 > 127 || n3 <= 0 || n3 > 127 || n4 <= 0 || n4 > 127) {
            throw new IllegalArgumentException("invalid native Berserkers lease");
        }
        long l2 = BattleAbilityStatus.expiresAt(l, n);
        BattleAbilityStatus battleAbilityStatus = this;
        this.berserkExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        this.berserkArmorClass = n2;
        this.berserkMeleeHit = n3;
        this.berserkMeleeDamage = n4;
    }

    synchronized void activateImmuneToHarm(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n > 65535 || n2 <= 1 || n2 > 127) {
            throw new IllegalArgumentException("invalid native Immune to Harm lease");
        }
        this.immuneToHarmExpiresAtNanos = BattleAbilityStatus.expiresAt(l, n);
        this.immuneToHarmDivisor = n2;
        this.nativeSourceProc68IconLease = new Object();
    }

    synchronized void activateReductionArmor(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Reduction Armor lease");
        }
        this.reductionArmorExpiresAtNanos = BattleAbilityStatus.expiresAt(l, n);
    }

    synchronized void activateBounceAttack(long l, int n, int n2, int n3, int n4) {
        if (l < 0L || n <= 0 || n > 65535 || n2 >= 0 || n2 < -127 || n3 <= 0 || n3 > 127 || n4 <= 0 || n4 > 100) {
            throw new IllegalArgumentException("invalid native Bounce Attack lease");
        }
        this.bounceAttackExpiresAtNanos = BattleAbilityStatus.expiresAt(l, n);
        this.bounceAttackArmorClass = n2;
        this.bounceAttackMeleeHit = n3;
        this.bounceAttackWeaponDamageChance = n4;
    }

    synchronized void activateSolidCarriage(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n > 65535 || n2 <= 0 || n2 > 127) {
            throw new IllegalArgumentException("invalid native Solid Carriage lease");
        }
        this.solidCarriageExpiresAtNanos = BattleAbilityStatus.expiresAt(l, n);
        this.solidCarriageEvasionRate = n2;
    }

    synchronized void activateDressEvasion(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n > 65535 || n2 <= 0 || n2 > 127) {
            throw new IllegalArgumentException("invalid native Dress Evasion lease");
        }
        long l2 = BattleAbilityStatus.expiresAt(l, n);
        BattleAbilityStatus battleAbilityStatus = this;
        this.dressEvasionExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        this.dressEvasionRate = n2;
    }

    synchronized boolean activateUncannyDodge(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n > 65535 || n2 <= 0 || n2 > 10) {
            throw new IllegalArgumentException("invalid native Uncanny Dodge lease");
        }
        if (this.uncannyDodgeExpiresAtNanos != 0L && l < this.uncannyDodgeExpiresAtNanos) {
            return false;
        }
        this.uncannyDodgeExpiresAtNanos = BattleAbilityStatus.expiresAt(l, n);
        this.uncannyDodgeModifier = n2;
        return true;
    }

    synchronized boolean activateMirrorImage(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n > 65535 || n2 <= 0 || n2 > 10) {
            throw new IllegalArgumentException("invalid native Mirror Image lease");
        }
        if (this.mirrorImageExpiresAtNanos != 0L && l < this.mirrorImageExpiresAtNanos) {
            return false;
        }
        this.mirrorImageExpiresAtNanos = BattleAbilityStatus.expiresAt(l, n);
        this.mirrorImageDodgeModifier = n2;
        return true;
    }

    synchronized boolean activateIllusionOgre(long l, int n, int n2, int n3) {
        if (l < 0L || n <= 0 || n > 65535 || n2 <= 0 || n2 > 127 || n3 <= 0 || n3 > 127) {
            throw new IllegalArgumentException("invalid native Illusion: Ogre lease");
        }
        boolean bl = this.illusionOgreExpiresAtNanos != 0L && l < this.illusionOgreExpiresAtNanos;
        this.illusionOgreExpiresAtNanos = BattleAbilityStatus.expiresAt(l, n);
        this.illusionOgreMeleeHit = n2;
        this.illusionOgreMeleeDamage = n3;
        return bl;
    }

    synchronized boolean activateIllusionLich(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n > 65535 || n2 <= 0 || n2 > 127) {
            throw new IllegalArgumentException("invalid native Illusion: Lich lease");
        }
        boolean bl = this.illusionLichExpiresAtNanos != 0L && l < this.illusionLichExpiresAtNanos;
        this.illusionLichExpiresAtNanos = BattleAbilityStatus.expiresAt(l, n);
        this.illusionLichSpellPower = n2;
        return bl;
    }

    synchronized boolean activateIllusionAvatar(long l, int n, int n2, int n3, int n4, double d) {
        if (l < 0L || n <= 0 || n > 65535 || n2 <= 0 || n2 > 127 || n3 <= 0 || n3 > 127 || n4 <= 0 || n4 > 127 || Double.compare(d, 1.5) != 0) {
            throw new IllegalArgumentException("invalid native Illusion: Avatar lease");
        }
        boolean bl = this.illusionAvatarExpiresAtNanos != 0L && l < this.illusionAvatarExpiresAtNanos;
        this.illusionAvatarExpiresAtNanos = BattleAbilityStatus.expiresAt(l, n);
        this.illusionAvatarMeleeDamage = n2;
        this.illusionAvatarRangedDamage = n3;
        this.illusionAvatarSpellPower = n4;
        this.illusionAvatarIncomingDamageRate = d;
        return bl;
    }

    synchronized boolean activateDiamondGolem(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n > 65535 || n2 >= 0 || n2 < -127) {
            throw new IllegalArgumentException("invalid native Illusion: Diamond Golem lease");
        }
        boolean bl = this.diamondGolemExpiresAtNanos != 0L && l < this.diamondGolemExpiresAtNanos;
        this.diamondGolemExpiresAtNanos = BattleAbilityStatus.expiresAt(l, n);
        this.diamondGolemArmorClass = n2;
        return bl;
    }

    synchronized boolean activateInsight(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n > 65535 || n2 <= 0 || n2 > 127) {
            throw new IllegalArgumentException("invalid native Insight lease");
        }
        if (this.insightExpiresAtNanos != 0L && l < this.insightExpiresAtNanos) {
            return false;
        }
        long l2 = BattleAbilityStatus.expiresAt(l, n);
        BattleAbilityStatus battleAbilityStatus = this;
        this.insightExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        this.insightAbilityScoreModifier = n2;
        return true;
    }

    synchronized boolean activatePanic(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n > 65535 || n2 >= 0 || n2 < -127) {
            throw new IllegalArgumentException("invalid native Panic lease");
        }
        if (this.panicExpiresAtNanos != 0L && l < this.panicExpiresAtNanos) {
            return false;
        }
        long l2 = BattleAbilityStatus.expiresAt(l, n);
        BattleAbilityStatus battleAbilityStatus = this;
        this.panicExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        this.panicAbilityScoreModifier = n2;
        return true;
    }

    synchronized void activateCounterBarrier(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Counter Barrier lease");
        }
        this.counterBarrierExpiresAtNanos = BattleAbilityStatus.expiresAt(l, n);
    }

    synchronized void activateMortalBody(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Mortal Body lease");
        }
        long l2 = BattleAbilityStatus.expiresAt(l, n);
        BattleAbilityStatus battleAbilityStatus = this;
        this.mortalBodyExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
    }

    synchronized int activateRoyalAura(int n, long l, int n2, int n3, int n4, int n5, int n6) {
        BattleAbilityStatus.validateSkillId(n);
        if (l < 0L || n2 <= 0 || n2 > 65535 || n3 < -127 || n3 > 127 || n4 < 0 || n4 > 127 || n5 < 0 || n5 > 127 || n6 < 0 || n6 > 127 || n3 + n4 + n5 + n6 == 0) {
            throw new IllegalArgumentException("invalid native royal-aura lease");
        }
        int n7 = BattleAbilityStatus.activeSkillId(l, this.royalAuraExpiresAtNanos, this.royalAuraSkillId);
        this.royalAuraExpiresAtNanos = BattleAbilityStatus.expiresAt(l, n2);
        this.royalAuraSkillId = n;
        this.royalAuraArmorClass = n3;
        this.royalAuraMeleeHit = n4;
        this.royalAuraRangedHit = n5;
        this.royalAuraMagicResistance = n6;
        return this.sourceBuffLeases.replace(n7 == n ? 0 : n7, n, l, n2);
    }

    synchronized void activateBraveAura(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n > 65535 || n2 <= 0 || n2 > 127) {
            throw new IllegalArgumentException("invalid native Brave Aura lease");
        }
        this.braveAuraExpiresAtNanos = BattleAbilityStatus.expiresAt(l, n);
        this.braveAuraMeleeDamage = n2;
    }

    synchronized void activateMagicResistance(long l, int n, int n2) {
        BattleAbilityStatus.validateActivation(l, n, n2);
        if (n2 <= 0) {
            throw new IllegalArgumentException("invalid native Resist Magic lease");
        }
        this.magicResistanceExpiresAtNanos = BattleAbilityStatus.expiresAt(l, n);
        this.magicResistanceModifier = n2;
    }

    synchronized Modifier shield(long l) {
        if (CombatBuffLeases.loaded()) {
            return this.sourceBuffLeases.shield(l);
        }
        return BattleAbilityStatus.modifier(l, this.shieldExpiresAtNanos, this.shieldModifier);
    }

    synchronized ArmorProtection armorProtection(long l) {
        if (CombatBuffLeases.loaded()) {
            return this.sourceBuffLeases.armorProtection(l);
        }
        Modifier modifier = BattleAbilityStatus.modifier(l, this.shieldExpiresAtNanos, this.shieldModifier);
        if (modifier.remainingSeconds() == 0) {
            return new ArmorProtection(0, 0, 0);
        }
        return new ArmorProtection(this.shieldSkillId, modifier.amount(), modifier.remainingSeconds());
    }

    synchronized Modifier strength(long l) {
        if (CombatBuffLeases.loaded()) {
            return this.sourceBuffLeases.strength(l);
        }
        return BattleAbilityStatus.modifier(l, this.strengthExpiresAtNanos, this.strengthModifier);
    }

    synchronized int strengthSkillId(long l) {
        if (CombatBuffLeases.loaded()) {
            return this.sourceBuffLeases.strengthSkillId(l);
        }
        return BattleAbilityStatus.activeSkillId(l, this.strengthExpiresAtNanos, this.strengthSkillId);
    }

    synchronized Modifier dexterity(long l) {
        if (CombatBuffLeases.loaded()) {
            return this.sourceBuffLeases.dexterity(l);
        }
        return BattleAbilityStatus.modifier(l, this.dexterityExpiresAtNanos, this.dexterityModifier);
    }

    synchronized int dexteritySkillId(long l) {
        if (CombatBuffLeases.loaded()) {
            return this.sourceBuffLeases.dexteritySkillId(l);
        }
        return BattleAbilityStatus.activeSkillId(l, this.dexterityExpiresAtNanos, this.dexteritySkillId);
    }

    synchronized Berserk berserk(long l) {
        if (this.berserkExpiresAtNanos == 0L || l >= this.berserkExpiresAtNanos) {
            return new Berserk(0, 0, 0, 0);
        }
        return new Berserk(CombatBuffLeases.value(this.berserkArmorClass, 55, "AC"), CombatBuffLeases.value(this.berserkMeleeHit, 55, "HIT"), CombatBuffLeases.value(this.berserkMeleeDamage, 55, "DMG"), BattleAbilityStatus.modifier(l, this.berserkExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized Protection immuneToHarm(long l) {
        if (this.immuneToHarmExpiresAtNanos == 0L || l >= this.immuneToHarmExpiresAtNanos) {
            return new Protection(0, 0);
        }
        return new Protection(this.immuneToHarmDivisor, BattleAbilityStatus.modifier(l, this.immuneToHarmExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized ReductionArmor reductionArmor(long l) {
        return new ReductionArmor(BattleAbilityStatus.modifier(l, this.reductionArmorExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized BounceAttack bounceAttack(long l) {
        if (this.bounceAttackExpiresAtNanos == 0L || l >= this.bounceAttackExpiresAtNanos) {
            return new BounceAttack(0, 0, 0, 0);
        }
        return new BounceAttack(CombatBuffLeases.value(this.bounceAttackArmorClass, 89, "AC"), CombatBuffLeases.value(this.bounceAttackMeleeHit, 89, "HIT"), this.bounceAttackWeaponDamageChance, BattleAbilityStatus.modifier(l, this.bounceAttackExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized SolidCarriage solidCarriage(long l) {
        if (this.solidCarriageExpiresAtNanos == 0L || l >= this.solidCarriageExpiresAtNanos) {
            return new SolidCarriage(0, 0);
        }
        return new SolidCarriage(this.solidCarriageEvasionRate, BattleAbilityStatus.modifier(l, this.solidCarriageExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized DressEvasion dressEvasion(long l) {
        if (this.dressEvasionExpiresAtNanos == 0L || l >= this.dressEvasionExpiresAtNanos) {
            return new DressEvasion(0, 0);
        }
        return new DressEvasion(this.dressEvasionRate, BattleAbilityStatus.modifier(l, this.dressEvasionExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized UncannyDodge uncannyDodge(long l) {
        if (this.uncannyDodgeExpiresAtNanos == 0L || l >= this.uncannyDodgeExpiresAtNanos) {
            return new UncannyDodge(0, 0);
        }
        return new UncannyDodge(this.uncannyDodgeModifier, BattleAbilityStatus.modifier(l, this.uncannyDodgeExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized MirrorImage mirrorImage(long l) {
        if (this.mirrorImageExpiresAtNanos == 0L || l >= this.mirrorImageExpiresAtNanos) {
            return new MirrorImage(0, 0);
        }
        return new MirrorImage(this.mirrorImageDodgeModifier, BattleAbilityStatus.modifier(l, this.mirrorImageExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized IllusionOgre illusionOgre(long l) {
        if (this.illusionOgreExpiresAtNanos == 0L || l >= this.illusionOgreExpiresAtNanos) {
            return new IllusionOgre(0, 0, 0);
        }
        return new IllusionOgre(CombatBuffLeases.value(this.illusionOgreMeleeHit, 204, "HIT"), CombatBuffLeases.value(this.illusionOgreMeleeDamage, 204, "DMG"), BattleAbilityStatus.modifier(l, this.illusionOgreExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized IllusionLich illusionLich(long l) {
        if (this.illusionLichExpiresAtNanos == 0L || l >= this.illusionLichExpiresAtNanos) {
            return new IllusionLich(0, 0);
        }
        return new IllusionLich(this.illusionLichSpellPower, BattleAbilityStatus.modifier(l, this.illusionLichExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized IllusionAvatar illusionAvatar(long l) {
        if (this.illusionAvatarExpiresAtNanos == 0L || l >= this.illusionAvatarExpiresAtNanos) {
            return new IllusionAvatar(0, 0, 0, 1.0, 0);
        }
        return new IllusionAvatar(CombatBuffLeases.value(this.illusionAvatarMeleeDamage, 219, "DMG"), CombatBuffLeases.value(this.illusionAvatarRangedDamage, 219, "BOW_DMG"), this.illusionAvatarSpellPower, this.illusionAvatarIncomingDamageRate, BattleAbilityStatus.modifier(l, this.illusionAvatarExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized DiamondGolem diamondGolem(long l) {
        if (this.diamondGolemExpiresAtNanos == 0L || l >= this.diamondGolemExpiresAtNanos) {
            return new DiamondGolem(0, 0);
        }
        return new DiamondGolem(CombatBuffLeases.value(this.diamondGolemArmorClass, 214, "AC"), BattleAbilityStatus.modifier(l, this.diamondGolemExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized Insight insight(long l) {
        if (this.insightExpiresAtNanos == 0L || l >= this.insightExpiresAtNanos) {
            return new Insight(0, 0);
        }
        return new Insight(this.insightAbilityScoreModifier, BattleAbilityStatus.modifier(l, this.insightExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized Panic panic(long l) {
        if (this.panicExpiresAtNanos == 0L || l >= this.panicExpiresAtNanos) {
            return new Panic(0, 0);
        }
        return new Panic(this.panicAbilityScoreModifier, BattleAbilityStatus.modifier(l, this.panicExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized CounterBarrier counterBarrier(long l) {
        return new CounterBarrier(BattleAbilityStatus.modifier(l, this.counterBarrierExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized MortalBody mortalBody(long l) {
        return new MortalBody(BattleAbilityStatus.modifier(l, this.mortalBodyExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized RoyalAura royalAura(long l) {
        if (CombatBuffLeases.loaded()) {
            return this.sourceBuffLeases.royalAura(l);
        }
        if (this.royalAuraExpiresAtNanos == 0L || l >= this.royalAuraExpiresAtNanos) {
            return new RoyalAura(0, 0, 0, 0, 0, 0);
        }
        return new RoyalAura(this.royalAuraSkillId, this.royalAuraArmorClass, this.royalAuraMeleeHit, this.royalAuraRangedHit, this.royalAuraMagicResistance, BattleAbilityStatus.modifier(l, this.royalAuraExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized BraveAura braveAura(long l) {
        if (this.braveAuraExpiresAtNanos == 0L || l >= this.braveAuraExpiresAtNanos) {
            return new BraveAura(0, 0);
        }
        return new BraveAura(CombatBuffLeases.value(this.braveAuraMeleeDamage, 117, "DMG"), BattleAbilityStatus.modifier(l, this.braveAuraExpiresAtNanos, 1).remainingSeconds());
    }

    synchronized MagicResistance magicResistance(long l) {
        Modifier modifier = BattleAbilityStatus.modifier(l, this.magicResistanceExpiresAtNanos, this.magicResistanceModifier);
        return new MagicResistance(modifier.amount(), modifier.remainingSeconds());
    }

    synchronized boolean expireShield(long l) {
        return this.expireArmorProtection(l) != 0;
    }

    synchronized int expireArmorProtection(long l) {
        if (!BattleAbilityStatus.expired(l, this.shieldExpiresAtNanos)) {
            return 0;
        }
        int n = this.shieldSkillId;
        this.shieldExpiresAtNanos = 0L;
        this.shieldSkillId = 0;
        this.shieldModifier = 0;
        return n;
    }

    synchronized boolean expireStrength(long l) {
        return this.expireStrengthSkillId(l) != 0;
    }

    synchronized int expireStrengthSkillId(long l) {
        if (!BattleAbilityStatus.expired(l, this.strengthExpiresAtNanos)) {
            return 0;
        }
        int n = this.strengthSkillId;
        this.strengthExpiresAtNanos = 0L;
        this.strengthSkillId = 0;
        this.strengthModifier = 0;
        return n;
    }

    synchronized boolean expireDexterity(long l) {
        return this.expireDexteritySkillId(l) != 0;
    }

    synchronized int expireDexteritySkillId(long l) {
        if (!BattleAbilityStatus.expired(l, this.dexterityExpiresAtNanos)) {
            return 0;
        }
        int n = this.dexteritySkillId;
        this.dexterityExpiresAtNanos = 0L;
        this.dexteritySkillId = 0;
        this.dexterityModifier = 0;
        return n;
    }

    synchronized boolean removeSkill(int n) {
        if (n == this.shieldSkillId && this.shieldExpiresAtNanos != 0L) {
            this.shieldExpiresAtNanos = 0L;
            this.shieldSkillId = 0;
            this.shieldModifier = 0;
            return this.sourceBuffLeases.remove(true, n);
        }
        if (n == this.strengthSkillId && this.strengthExpiresAtNanos != 0L) {
            this.strengthExpiresAtNanos = 0L;
            this.strengthSkillId = 0;
            this.strengthModifier = 0;
            return this.sourceBuffLeases.remove(true, n);
        }
        if (n == this.dexteritySkillId && this.dexterityExpiresAtNanos != 0L) {
            this.dexterityExpiresAtNanos = 0L;
            this.dexteritySkillId = 0;
            this.dexterityModifier = 0;
            return this.sourceBuffLeases.remove(true, n);
        }
        if (n == 55 && this.berserkExpiresAtNanos != 0L) {
            this.clearBerserk();
            return this.sourceBuffLeases.remove(true, n);
        }
        return this.sourceBuffLeases.remove(false, n);
    }

    synchronized boolean expireBerserk(long l) {
        if (!BattleAbilityStatus.expired(l, this.berserkExpiresAtNanos)) {
            return false;
        }
        this.clearBerserk();
        return true;
    }

    synchronized boolean expireImmuneToHarm(long l) {
        if (!BattleAbilityStatus.expired(l, this.immuneToHarmExpiresAtNanos)) {
            return false;
        }
        this.clearImmuneToHarm();
        return true;
    }

    synchronized boolean expireReductionArmor(long l) {
        if (!BattleAbilityStatus.expired(l, this.reductionArmorExpiresAtNanos)) {
            return false;
        }
        this.reductionArmorExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean expireBounceAttack(long l) {
        if (!BattleAbilityStatus.expired(l, this.bounceAttackExpiresAtNanos)) {
            return false;
        }
        this.clearBounceAttack();
        return true;
    }

    synchronized boolean expireSolidCarriage(long l) {
        if (!BattleAbilityStatus.expired(l, this.solidCarriageExpiresAtNanos)) {
            return false;
        }
        this.clearSolidCarriage();
        return true;
    }

    synchronized boolean expireDressEvasion(long l) {
        if (!BattleAbilityStatus.expired(l, this.dressEvasionExpiresAtNanos)) {
            return false;
        }
        this.clearDressEvasion();
        return true;
    }

    synchronized boolean expireUncannyDodge(long l) {
        if (!BattleAbilityStatus.expired(l, this.uncannyDodgeExpiresAtNanos)) {
            return false;
        }
        this.clearUncannyDodge();
        return true;
    }

    synchronized boolean expireMirrorImage(long l) {
        if (!BattleAbilityStatus.expired(l, this.mirrorImageExpiresAtNanos)) {
            return false;
        }
        this.clearMirrorImage();
        return true;
    }

    synchronized boolean expireIllusionOgre(long l) {
        if (!BattleAbilityStatus.expired(l, this.illusionOgreExpiresAtNanos)) {
            return false;
        }
        this.clearIllusionOgre();
        return true;
    }

    synchronized boolean expireIllusionLich(long l) {
        if (!BattleAbilityStatus.expired(l, this.illusionLichExpiresAtNanos)) {
            return false;
        }
        this.clearIllusionLich();
        return true;
    }

    synchronized boolean expireIllusionAvatar(long l) {
        if (!BattleAbilityStatus.expired(l, this.illusionAvatarExpiresAtNanos)) {
            return false;
        }
        this.clearIllusionAvatar();
        return true;
    }

    synchronized boolean expireDiamondGolem(long l) {
        if (!BattleAbilityStatus.expired(l, this.diamondGolemExpiresAtNanos)) {
            return false;
        }
        this.clearDiamondGolem();
        return true;
    }

    synchronized boolean expireInsight(long l) {
        if (!BattleAbilityStatus.expired(l, this.insightExpiresAtNanos)) {
            return false;
        }
        this.clearInsight();
        return true;
    }

    synchronized boolean expirePanic(long l) {
        if (!BattleAbilityStatus.expired(l, this.panicExpiresAtNanos)) {
            return false;
        }
        this.clearPanic();
        return true;
    }

    synchronized boolean removeSolidCarriage() {
        if (this.solidCarriageExpiresAtNanos == 0L) {
            return false;
        }
        this.clearSolidCarriage();
        return true;
    }

    synchronized boolean expireCounterBarrier(long l) {
        if (!BattleAbilityStatus.expired(l, this.counterBarrierExpiresAtNanos)) {
            return false;
        }
        this.counterBarrierExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean removeCounterBarrier() {
        if (this.counterBarrierExpiresAtNanos == 0L) {
            return false;
        }
        this.counterBarrierExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean expireMortalBody(long l) {
        if (!BattleAbilityStatus.expired(l, this.mortalBodyExpiresAtNanos)) {
            return false;
        }
        long l2 = 0L;
        BattleAbilityStatus battleAbilityStatus = this;
        this.mortalBodyExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        return true;
    }

    synchronized boolean removeMortalBody() {
        if (this.mortalBodyExpiresAtNanos == 0L) {
            return false;
        }
        long l = 0L;
        BattleAbilityStatus battleAbilityStatus = this;
        this.mortalBodyExpiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        return true;
    }

    synchronized int expireRoyalAura(long l) {
        if (!BattleAbilityStatus.expired(l, this.royalAuraExpiresAtNanos)) {
            return 0;
        }
        int n = this.royalAuraSkillId;
        this.clearRoyalAura();
        return n;
    }

    synchronized boolean expireBraveAura(long l) {
        if (!BattleAbilityStatus.expired(l, this.braveAuraExpiresAtNanos)) {
            return false;
        }
        this.clearBraveAura();
        return true;
    }

    synchronized boolean expireMagicResistance(long l) {
        if (!BattleAbilityStatus.expired(l, this.magicResistanceExpiresAtNanos)) {
            return false;
        }
        this.clearMagicResistance();
        return true;
    }

    synchronized void clear() {
        this.clearCancelable();
        this.reductionArmorExpiresAtNanos = 0L;
        this.clearSolidCarriage();
        this.clearUncannyDodge();
        this.counterBarrierExpiresAtNanos = 0L;
        long l = 0L;
        BattleAbilityStatus battleAbilityStatus = this;
        this.mortalBodyExpiresAtNanos = l;
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
        long l = 0L;
        BattleAbilityStatus battleAbilityStatus = this;
        this.mortalBodyExpiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        this.clearRoyalAura();
        this.clearBraveAura();
        this.clearMagicResistance();
        this.sourceBuffLeases.cancel();
    }

    synchronized Cancellation cancelMagicEffects() {
        Cancellation cancellation = new Cancellation(this.shieldExpiresAtNanos == 0L ? 0 : this.shieldSkillId, this.strengthExpiresAtNanos == 0L ? 0 : this.strengthSkillId, this.dexterityExpiresAtNanos == 0L ? 0 : this.dexteritySkillId, this.royalAuraExpiresAtNanos == 0L ? 0 : this.royalAuraSkillId, this.braveAuraExpiresAtNanos == 0L ? 0 : 117, this.magicResistanceExpiresAtNanos != 0L, this.berserkExpiresAtNanos != 0L, this.immuneToHarmExpiresAtNanos != 0L, this.bounceAttackExpiresAtNanos != 0L, this.uncannyDodgeExpiresAtNanos != 0L, this.mirrorImageExpiresAtNanos != 0L, this.illusionOgreExpiresAtNanos != 0L, this.illusionLichExpiresAtNanos != 0L, this.illusionAvatarExpiresAtNanos != 0L, this.diamondGolemExpiresAtNanos != 0L, this.insightExpiresAtNanos != 0L, this.panicExpiresAtNanos != 0L, this.dressEvasionExpiresAtNanos != 0L, this.mortalBodyExpiresAtNanos != 0L);
        this.clearCancelable();
        return cancellation;
    }

    private void clearBerserk() {
        long l = 0L;
        BattleAbilityStatus battleAbilityStatus = this;
        this.berserkExpiresAtNanos = l;
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
        this.bounceAttackExpiresAtNanos = 0L;
        this.bounceAttackArmorClass = 0;
        this.bounceAttackMeleeHit = 0;
        this.bounceAttackWeaponDamageChance = 0;
    }

    private void clearSolidCarriage() {
        this.solidCarriageExpiresAtNanos = 0L;
        this.solidCarriageEvasionRate = 0;
    }

    private void clearUncannyDodge() {
        this.uncannyDodgeExpiresAtNanos = 0L;
        this.uncannyDodgeModifier = 0;
    }

    private void clearMirrorImage() {
        this.mirrorImageExpiresAtNanos = 0L;
        this.mirrorImageDodgeModifier = 0;
    }

    private void clearIllusionOgre() {
        this.illusionOgreExpiresAtNanos = 0L;
        this.illusionOgreMeleeHit = 0;
        this.illusionOgreMeleeDamage = 0;
    }

    private void clearIllusionLich() {
        this.illusionLichExpiresAtNanos = 0L;
        this.illusionLichSpellPower = 0;
    }

    private void clearIllusionAvatar() {
        this.illusionAvatarExpiresAtNanos = 0L;
        this.illusionAvatarMeleeDamage = 0;
        this.illusionAvatarRangedDamage = 0;
        this.illusionAvatarSpellPower = 0;
        this.illusionAvatarIncomingDamageRate = 0.0;
    }

    private void clearDiamondGolem() {
        this.diamondGolemExpiresAtNanos = 0L;
        this.diamondGolemArmorClass = 0;
    }

    private void clearInsight() {
        long l = 0L;
        BattleAbilityStatus battleAbilityStatus = this;
        this.insightExpiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        this.insightAbilityScoreModifier = 0;
    }

    private void clearPanic() {
        long l = 0L;
        BattleAbilityStatus battleAbilityStatus = this;
        this.panicExpiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        this.panicAbilityScoreModifier = 0;
    }

    private void clearDressEvasion() {
        long l = 0L;
        BattleAbilityStatus battleAbilityStatus = this;
        this.dressEvasionExpiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleAbilityStatus);
        this.dressEvasionRate = 0;
    }

    private void clearRoyalAura() {
        this.royalAuraExpiresAtNanos = 0L;
        this.royalAuraSkillId = 0;
        this.royalAuraArmorClass = 0;
        this.royalAuraMeleeHit = 0;
        this.royalAuraRangedHit = 0;
        this.royalAuraMagicResistance = 0;
    }

    private void clearBraveAura() {
        this.braveAuraExpiresAtNanos = 0L;
        this.braveAuraMeleeDamage = 0;
    }

    private void clearMagicResistance() {
        this.magicResistanceExpiresAtNanos = 0L;
        this.magicResistanceModifier = 0;
    }

    private static void validateActivation(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n > 65535 || n2 == 0 || n2 < -127 || n2 > 127) {
            throw new IllegalArgumentException("invalid native ability-buff lease");
        }
    }

    private static void validateSkillId(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("invalid native ability-buff skill id");
        }
    }

    private static long expiresAt(long l, int n) {
        return Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
    }

    private static boolean expired(long l, long l2) {
        return l2 != 0L && l >= l2;
    }

    private static int activeSkillId(long l, long l2, int n) {
        return l2 != 0L && l < l2 ? n : 0;
    }

    private static Modifier modifier(long l, long l2, int n) {
        if (l2 == 0L || l >= l2) {
            return new Modifier(0, 0);
        }
        long l3 = l2 - l;
        long l4 = (l3 + TimeUnit.SECONDS.toNanos(1L) - 1L) / TimeUnit.SECONDS.toNanos(1L);
        return new Modifier(n, (int)Math.min(65535L, Math.max(1L, l4)));
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
            return this.armorProtectionSkillId != 0;
        }

        boolean strength() {
            return this.strengthSkillId != 0;
        }

        boolean dexterity() {
            return this.dexteritySkillId != 0;
        }
    }
}
