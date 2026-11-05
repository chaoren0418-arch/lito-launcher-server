// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

import java.util.concurrent.TimeUnit;

final class BattleTimedStatus {
    static final int NORMAL = 0;
    static final int HASTE = 1;
    static final int SLOW = 2;
    static final int BRAVE_NORMAL = 0;
    static final int BRAVE_FIRST = 1;
    static final int BRAVE_ELF = 3;
    static final int BRAVE_HOLY_WALK = 4;
    static final int BRAVE_SECOND = 5;
    static final int BRAVE_BLOODLUST = 6;
    private volatile long hasteExpiresAtNanos;
    private int hasteSkillId;
    private volatile long slowExpiresAtNanos;
    private int slowSkillId;
    private long braveExpiresAtNanos;
    private int braveMode;
    private long blindExpiresAtNanos;
    private long weaknessExpiresAtNanos;
    private long diseaseExpiresAtNanos;
    private long silenceExpiresAtNanos;
    private long areaSilenceExpiresAtNanos;
    private long decayPotionExpiresAtNanos;
    private long polluteWaterExpiresAtNanos;
    private long strikerGaleExpiresAtNanos;
    private long waterDragonPotionErosionExpiresAtNanos;
    private long waterDragonDirtyCurrentExpiresAtNanos;
    private long waterDragonHealingErosionExpiresAtNanos;
    private long lightExpiresAtNanos;
    private long bluePotionExpiresAtNanos;
    private long wisdomPotionExpiresAtNanos;
    private long meditationExpiresAtNanos;
    private long concentrationExpiresAtNanos;
    private long counterMagicExpiresAtNanos;
    private long breathExpiresAtNanos;
    private long poisonExpiresAtNanos;
    private long poisonNextTickAtNanos;
    private boolean poisonSilence;
    private long sleepExpiresAtNanos;
    private volatile long freezeExpiresAtNanos;
    private long earthBindExpiresAtNanos;
    private long absoluteBarrierExpiresAtNanos;
    private long advanceSpiritExpiresAtNanos;
    private long burningSpiritExpiresAtNanos;
    private long elementalFireExpiresAtNanos;
    private long soulOfFlameExpiresAtNanos;
    private long venomResistExpiresAtNanos;
    private long doubleBrakeExpiresAtNanos;
    private long shockStunExpiresAtNanos;
    private long boneBreakExpiresAtNanos;
    private long thunderGrabExpiresAtNanos;
    private long horrorOfDeathExpiresAtNanos;
    private long paralysisActivatesAtNanos;
    private long paralysisExpiresAtNanos;
    private boolean paralysisLockEmitted;
    private boolean drunken;
    private BattleTimedStatus sourcePoisonParalysis;
    private volatile CombatPoisonLifecycle.State sourcePoisonLifecycle;
    private volatile Object sourceSleepLease;
    private Object sourceProcPlayerState;
    private Object sourceProcNpcState;
    private Object sourceDeActorState;
    private volatile Object sourceDeKnownViewer;
    volatile Object nativeSourceDollSlowEpoch;
    volatile transient int nativeSourceIcons1BraveSkill;
    volatile transient long nativeSourceIcons1Version;

    BattleTimedStatus() {
    }

    synchronized Speed speed(long nowNanos) {
        Speed speed = NativeSourceDollSlow.extendedSpeed(this, nowNanos);
        if (speed != null) {
            return speed;
        }
        long l2 = 0L;
        int n = 0;
        if (this.slowExpiresAtNanos != 0L && nowNanos < this.slowExpiresAtNanos) {
            l2 = this.slowExpiresAtNanos;
            n = 2;
        } else if (this.hasteExpiresAtNanos != 0L && nowNanos < this.hasteExpiresAtNanos) {
            l2 = this.hasteExpiresAtNanos;
            n = 1;
        }
        if (n == 0) {
            return new Speed(0, 0);
        }
        long l3 = l2 - nowNanos;
        long l4 = (l3 + TimeUnit.SECONDS.toNanos(1L) - 1L) / TimeUnit.SECONDS.toNanos(1L);
        return new Speed(n, (int)Math.min(65535L, Math.max(1L, l4)));
    }

    synchronized SpeedChange activateHaste(long nowNanos, int durationSeconds) {
        return activateHaste(nowNanos, durationSeconds, 0);
    }

    synchronized SpeedChange activateHaste(
            long nowNanos, int durationSeconds, int skillId) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native haste duration");
        }
        if (skillId != 0 && skillId != BattleSpellCatalog.HASTE
                && skillId != BattleSpellCatalog.GREATER_HASTE) {
            throw new IllegalArgumentException("invalid native haste source skill");
        }
        if (slowExpiresAtNanos != 0 && nowNanos < slowExpiresAtNanos) {
            int stopped = slowSkillId;
            slowExpiresAtNanos = 0;
            slowSkillId = 0;
            return new SpeedChange(NORMAL, 0, stopped);
        }
        slowExpiresAtNanos = 0;
        slowSkillId = 0;
        hasteExpiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        hasteSkillId = skillId;
        return new SpeedChange(HASTE, durationSeconds, 0);
    }

    synchronized SpeedChange activateSlow(
            long nowNanos, int durationSeconds, int skillId) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || skillId != BattleSpellCatalog.SLOW
                    && skillId != BattleSpellCatalog.MASS_SLOW
                    && skillId != BattleSpellCatalog.ENTANGLE) {
            throw new IllegalArgumentException("invalid native slow lease");
        }
        if (hasteExpiresAtNanos != 0 && nowNanos < hasteExpiresAtNanos) {
            int stopped = hasteSkillId;
            hasteExpiresAtNanos = 0;
            hasteSkillId = 0;
            return new SpeedChange(NORMAL, 0, stopped);
        }
        hasteExpiresAtNanos = 0;
        hasteSkillId = 0;
        slowExpiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        slowSkillId = skillId;
        return new SpeedChange(SLOW, durationSeconds, 0);
    }

    synchronized int slowSkillId(long nowNanos) {
        return slowExpiresAtNanos != 0 && nowNanos < slowExpiresAtNanos
                ? slowSkillId : 0;
    }

    synchronized SlowExpiration expireSlowLease(long nowNanos) {
        if (slowExpiresAtNanos == 0 || nowNanos < slowExpiresAtNanos) {
            return new SlowExpiration(false, 0);
        }
        int sourceSkillId = slowSkillId;
        slowExpiresAtNanos = 0;
        slowSkillId = 0;
        return new SlowExpiration(true, sourceSkillId);
    }

    synchronized int hasteSkillId(long nowNanos) {
        return hasteExpiresAtNanos != 0 && nowNanos < hasteExpiresAtNanos
                ? hasteSkillId : 0;
    }

    synchronized boolean expireHaste(long nowNanos) {
        return expireHasteLease(nowNanos).expired();
    }

    synchronized HasteExpiration expireHasteLease(long nowNanos) {
        if (hasteExpiresAtNanos == 0 || nowNanos < hasteExpiresAtNanos) {
            return new HasteExpiration(false, 0);
        }
        int sourceSkillId = hasteSkillId;
        hasteExpiresAtNanos = 0;
        hasteSkillId = 0;
        return new HasteExpiration(true, sourceSkillId);
    }

    synchronized Brave brave(long nowNanos) {
        if (braveExpiresAtNanos == 0 || nowNanos >= braveExpiresAtNanos) {
            return new Brave(BRAVE_NORMAL, 0);
        }
        long remainingNanos = braveExpiresAtNanos - nowNanos;
        long seconds = (remainingNanos + TimeUnit.SECONDS.toNanos(1) - 1)
                / TimeUnit.SECONDS.toNanos(1);
        return new Brave(braveMode,
                (int) Math.min(0xFFFFL, Math.max(1L, seconds)));
    }

    synchronized void activateBrave(long nowNanos, int durationSeconds) {
        this.activateBrave(nowNanos, durationSeconds, 1);
    }

    synchronized void activateBrave(long nowNanos, int durationSeconds, int mode) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535) {
            throw new IllegalArgumentException("invalid native brave duration");
        }
        if (mode != 1 && mode != 3 && mode != 4 && mode != 5 && mode != 6) {
            throw new IllegalArgumentException("invalid native brave mode");
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).braveExpiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        int n3 = 0;
        battleTimedStatus = this;
        this.nativeSourceIcons1BraveSkill = n3;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        n3 = mode;
        battleTimedStatus = this;
        this.braveMode = n3;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean expireBrave(long nowNanos) {
        if (this.braveExpiresAtNanos == 0L || nowNanos < this.braveExpiresAtNanos) {
            return false;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).braveExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        int n = 0;
        battleTimedStatus = this;
        this.nativeSourceIcons1BraveSkill = n;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        n = 0;
        battleTimedStatus = this;
        this.braveMode = n;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean blindActive(long nowNanos) {
        return blindExpiresAtNanos != 0 && nowNanos < blindExpiresAtNanos;
    }

    synchronized void activateBlind(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native blindness duration");
        }
        blindExpiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
    }

    synchronized boolean expireBlind(long nowNanos) {
        if (blindExpiresAtNanos == 0 || nowNanos < blindExpiresAtNanos) return false;
        blindExpiresAtNanos = 0;
        return true;
    }

    synchronized boolean weaknessActive(long nowNanos) {
        return weaknessExpiresAtNanos != 0 && nowNanos < weaknessExpiresAtNanos;
    }

    synchronized void activateWeakness(long nowNanos, int durationSeconds) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535) {
            throw new IllegalArgumentException("invalid native Weakness duration");
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).weaknessExpiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean expireWeakness(long nowNanos) {
        if (this.weaknessExpiresAtNanos == 0L || nowNanos < this.weaknessExpiresAtNanos) {
            return false;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).weaknessExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean diseaseActive(long nowNanos) {
        return diseaseExpiresAtNanos != 0 && nowNanos < diseaseExpiresAtNanos;
    }

    synchronized void activateDisease(long nowNanos, int durationSeconds) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535) {
            throw new IllegalArgumentException("invalid native Disease duration");
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).diseaseExpiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean expireDisease(long nowNanos) {
        if (this.diseaseExpiresAtNanos == 0L || nowNanos < this.diseaseExpiresAtNanos) {
            return false;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).diseaseExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean silenceActive(long nowNanos) {
        return poisonSilence
                || (silenceExpiresAtNanos != 0 && nowNanos < silenceExpiresAtNanos)
                || areaSilenceActive(nowNanos);
    }

    synchronized void activateSilence(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid Silence duration");
        }
        silenceExpiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
    }

    /** Skill202 CONFUSION installs skill64 only when that exact lease is absent. */
    synchronized boolean activateSilenceIfAbsent(
            long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid Confusion Silence duration");
        }
        if (silenceExpiresAtNanos != 0 && nowNanos < silenceExpiresAtNanos) {
            return false;
        }
        silenceExpiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
        return true;
    }

    synchronized boolean expireSilence(long nowNanos) {
        if (silenceExpiresAtNanos == 0 || nowNanos < silenceExpiresAtNanos) {
            return false;
        }
        silenceExpiresAtNanos = 0;
        return true;
    }

    /**
     * Retained NPC L1SkillUse consumes ordinary SILENCE on the next real
     * spell attempt. Area of Silence is deliberately not handled here: it
     * rejects every attempt until its independent lease expires.
     */
    synchronized boolean consumeSilenceForNpcSpell(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Silence attempt time");
        }
        if (silenceExpiresAtNanos == 0
                || nowNanos >= silenceExpiresAtNanos) {
            silenceExpiresAtNanos = 0;
            return false;
        }
        silenceExpiresAtNanos = 0;
        return true;
    }

    synchronized boolean areaSilenceActive(long nowNanos) {
        return areaSilenceExpiresAtNanos != 0
                && nowNanos < areaSilenceExpiresAtNanos;
    }

    synchronized boolean activateAreaSilence(
            long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException(
                    "invalid Area of Silence duration");
        }
        boolean refreshed = areaSilenceActive(nowNanos);
        areaSilenceExpiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
        return refreshed;
    }

    synchronized boolean expireAreaSilence(long nowNanos) {
        if (areaSilenceExpiresAtNanos == 0
                || nowNanos < areaSilenceExpiresAtNanos) return false;
        areaSilenceExpiresAtNanos = 0;
        return true;
    }

    synchronized boolean decayPotionActive(long nowNanos) {
        return decayPotionExpiresAtNanos != 0 && nowNanos < decayPotionExpiresAtNanos;
    }

    synchronized boolean activateDecayPotion(long nowNanos, int durationSeconds) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535) {
            throw new IllegalArgumentException("invalid Decay Potion duration");
        }
        if (this.decayPotionActive(nowNanos)) {
            return false;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).decayPotionExpiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean expireDecayPotion(long nowNanos) {
        if (this.decayPotionExpiresAtNanos == 0L || nowNanos < this.decayPotionExpiresAtNanos) {
            return false;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).decayPotionExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean polluteWaterActive(long nowNanos) {
        return polluteWaterExpiresAtNanos != 0
                && nowNanos < polluteWaterExpiresAtNanos;
    }

    synchronized void activatePolluteWater(long nowNanos, int durationSeconds) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535) {
            throw new IllegalArgumentException("invalid Pollute Water duration");
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).polluteWaterExpiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean expirePolluteWater(long nowNanos) {
        if (this.polluteWaterExpiresAtNanos == 0L || nowNanos < this.polluteWaterExpiresAtNanos) {
            return false;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).polluteWaterExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean strikerGaleActive(long nowNanos) {
        return strikerGaleExpiresAtNanos != 0
                && nowNanos < strikerGaleExpiresAtNanos;
    }

    synchronized void activateStrikerGale(long nowNanos, int durationSeconds) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535) {
            throw new IllegalArgumentException("invalid Striker Gale duration");
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).strikerGaleExpiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean expireStrikerGale(long nowNanos) {
        if (this.strikerGaleExpiresAtNanos == 0L || nowNanos < this.strikerGaleExpiresAtNanos) {
            return false;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).strikerGaleExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized BattleWaterDragonAffliction.Kind waterDragonAffliction(
            long nowNanos) {
        expireWaterDragonAffliction(nowNanos);
        if (waterDragonPotionErosionExpiresAtNanos != 0) {
            return BattleWaterDragonAffliction.Kind.POTION_EROSION;
        }
        if (waterDragonDirtyCurrentExpiresAtNanos != 0) {
            return BattleWaterDragonAffliction.Kind.DIRTY_CURRENT;
        }
        if (waterDragonHealingErosionExpiresAtNanos != 0) {
            return BattleWaterDragonAffliction.Kind.HEALING_EROSION;
        }
        return null;
    }

    synchronized boolean activateWaterDragonAffliction(
            BattleWaterDragonAffliction.Kind kind, long nowNanos,
            int durationSeconds) {
        if (kind == null || nowNanos < 0 || durationSeconds <= 0
                || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid Water Dragon affliction");
        }
        if (waterDragonAffliction(nowNanos) != null) return false;
        long expiresAt = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
        switch (kind) {
            case POTION_EROSION -> waterDragonPotionErosionExpiresAtNanos = expiresAt;
            case DIRTY_CURRENT -> waterDragonDirtyCurrentExpiresAtNanos = expiresAt;
            case HEALING_EROSION -> waterDragonHealingErosionExpiresAtNanos = expiresAt;
        }
        return true;
    }

    synchronized boolean waterDragonPotionErosionActive(long nowNanos) {
        return waterDragonAffliction(nowNanos)
                == BattleWaterDragonAffliction.Kind.POTION_EROSION;
    }

    synchronized boolean waterDragonDirtyCurrentActive(long nowNanos) {
        return waterDragonAffliction(nowNanos)
                == BattleWaterDragonAffliction.Kind.DIRTY_CURRENT;
    }

    synchronized boolean waterDragonHealingErosionActive(long nowNanos) {
        return waterDragonAffliction(nowNanos)
                == BattleWaterDragonAffliction.Kind.HEALING_EROSION;
    }

    synchronized boolean expireWaterDragonAffliction(long nowNanos) {
        boolean expired = false;
        if (waterDragonPotionErosionExpiresAtNanos != 0
                && nowNanos >= waterDragonPotionErosionExpiresAtNanos) {
            waterDragonPotionErosionExpiresAtNanos = 0;
            expired = true;
        }
        if (waterDragonDirtyCurrentExpiresAtNanos != 0
                && nowNanos >= waterDragonDirtyCurrentExpiresAtNanos) {
            waterDragonDirtyCurrentExpiresAtNanos = 0;
            expired = true;
        }
        if (waterDragonHealingErosionExpiresAtNanos != 0
                && nowNanos >= waterDragonHealingErosionExpiresAtNanos) {
            waterDragonHealingErosionExpiresAtNanos = 0;
            expired = true;
        }
        return expired;
    }

    synchronized BattleWaterDragonAffliction.Kind clearWaterDragonAffliction() {
        BattleWaterDragonAffliction.Kind previous = null;
        if (waterDragonPotionErosionExpiresAtNanos != 0) {
            previous = BattleWaterDragonAffliction.Kind.POTION_EROSION;
        } else if (waterDragonDirtyCurrentExpiresAtNanos != 0) {
            previous = BattleWaterDragonAffliction.Kind.DIRTY_CURRENT;
        } else if (waterDragonHealingErosionExpiresAtNanos != 0) {
            previous = BattleWaterDragonAffliction.Kind.HEALING_EROSION;
        }
        waterDragonPotionErosionExpiresAtNanos = 0;
        waterDragonDirtyCurrentExpiresAtNanos = 0;
        waterDragonHealingErosionExpiresAtNanos = 0;
        return previous;
    }

    synchronized void activateLight(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native light-spell duration");
        }
        lightExpiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
    }

    synchronized boolean lightActive(long nowNanos) {
        return lightExpiresAtNanos != 0 && nowNanos < lightExpiresAtNanos;
    }

    synchronized boolean expireLight(long nowNanos) {
        if (lightExpiresAtNanos == 0 || nowNanos < lightExpiresAtNanos) return false;
        lightExpiresAtNanos = 0;
        return true;
    }

    synchronized void activateBluePotion(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native blue-potion duration");
        }
        bluePotionExpiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
    }

    synchronized boolean bluePotionActive(long nowNanos) {
        return bluePotionExpiresAtNanos != 0 && nowNanos < bluePotionExpiresAtNanos;
    }

    synchronized boolean expireBluePotion(long nowNanos) {
        if (bluePotionExpiresAtNanos == 0 || nowNanos < bluePotionExpiresAtNanos) return false;
        bluePotionExpiresAtNanos = 0;
        return true;
    }

    synchronized void activateWisdomPotion(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native wisdom-potion duration");
        }
        wisdomPotionExpiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
    }

    synchronized boolean wisdomPotionActive(long nowNanos) {
        return wisdomPotionExpiresAtNanos != 0 && nowNanos < wisdomPotionExpiresAtNanos;
    }

    synchronized boolean expireWisdomPotion(long nowNanos) {
        if (wisdomPotionExpiresAtNanos == 0 || nowNanos < wisdomPotionExpiresAtNanos) {
            return false;
        }
        wisdomPotionExpiresAtNanos = 0;
        return true;
    }

    synchronized void activateMeditation(long nowNanos, int durationSeconds) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535) {
            throw new IllegalArgumentException("invalid native meditation duration");
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).meditationExpiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean meditationActive(long nowNanos) {
        return meditationExpiresAtNanos != 0 && nowNanos < meditationExpiresAtNanos;
    }

    synchronized boolean expireMeditation(long nowNanos) {
        if (this.meditationExpiresAtNanos == 0L || nowNanos < this.meditationExpiresAtNanos) {
            return false;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).meditationExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean cancelMeditation() {
        if (this.meditationExpiresAtNanos == 0L) {
            return false;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).meditationExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized void activateConcentration(long nowNanos, int durationSeconds) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535) {
            throw new IllegalArgumentException("invalid native concentration duration");
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).concentrationExpiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean concentrationActive(long nowNanos) {
        return concentrationExpiresAtNanos != 0 && nowNanos < concentrationExpiresAtNanos;
    }

    synchronized boolean expireConcentration(long nowNanos) {
        if (this.concentrationExpiresAtNanos == 0L || nowNanos < this.concentrationExpiresAtNanos) {
            return false;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).concentrationExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized void activateCounterMagic(long nowNanos, int durationSeconds) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535) {
            throw new IllegalArgumentException("invalid native Counter Magic duration");
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).counterMagicExpiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean counterMagicActive(long nowNanos) {
        return counterMagicExpiresAtNanos != 0 && nowNanos < counterMagicExpiresAtNanos;
    }

    synchronized boolean expireCounterMagic(long nowNanos) {
        if (this.counterMagicExpiresAtNanos == 0L || nowNanos < this.counterMagicExpiresAtNanos) {
            return false;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).counterMagicExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean consumeCounterMagic(long nowNanos) {
        if (!this.counterMagicActive(nowNanos)) {
            if (this.counterMagicExpiresAtNanos != 0L && nowNanos >= this.counterMagicExpiresAtNanos) {
                long l2 = 0L;
                BattleTimedStatus battleTimedStatus = this;
                this.counterMagicExpiresAtNanos = l2;
                NativeSourceIcons1Epoch.bump(battleTimedStatus);
            }
            return false;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).counterMagicExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized Breath breath(long nowNanos) {
        if (breathExpiresAtNanos == 0 || nowNanos >= breathExpiresAtNanos) {
            return new Breath(0);
        }
        long remainingNanos = breathExpiresAtNanos - nowNanos;
        long seconds = (remainingNanos + TimeUnit.SECONDS.toNanos(1) - 1)
                / TimeUnit.SECONDS.toNanos(1);
        return new Breath((int) Math.min(0xFFFFL, Math.max(1L, seconds)));
    }

    synchronized Breath addBreath(long nowNanos, int addedSeconds, int maximumSeconds) {
        if (nowNanos < 0 || addedSeconds <= 0 || maximumSeconds <= 0
                || maximumSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native underwater-breath duration");
        }
        int current = breath(nowNanos).remainingSeconds();
        int duration = Math.min(maximumSeconds, Math.addExact(current, addedSeconds));
        breathExpiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(duration));
        return new Breath(duration);
    }

    synchronized boolean expireBreath(long nowNanos) {
        if (breathExpiresAtNanos == 0 || nowNanos < breathExpiresAtNanos) return false;
        breathExpiresAtNanos = 0;
        return true;
    }

    synchronized boolean cancelBreath() {
        if (breathExpiresAtNanos == 0) return false;
        breathExpiresAtNanos = 0;
        return true;
    }

    synchronized boolean drunkenActive() {
        return drunken;
    }

    synchronized void activateDrunken() {
        drunken = true;
    }

    synchronized boolean clearDrunken() {
        boolean present = drunken;
        drunken = false;
        return present;
    }

    synchronized boolean poisonActive(long nowNanos) {
        return poisonExpiresAtNanos != 0 && nowNanos < poisonExpiresAtNanos;
    }

    synchronized boolean activatePoison(long nowNanos, int durationSeconds,
                                        int tickIntervalMillis) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || tickIntervalMillis <= 0) {
            throw new IllegalArgumentException("invalid native damage-poison timing");
        }
        if (!acceptsPoison(nowNanos, PoisonKind.DAMAGE)
                || poisonActive(nowNanos) || poisonSilence) return false;
        poisonExpiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
        poisonNextTickAtNanos = Math.addExact(nowNanos,
                TimeUnit.MILLISECONDS.toNanos(tickIntervalMillis));
        return true;
    }

    synchronized PoisonPoll pollPoison(long nowNanos, int tickIntervalMillis) {
        if (tickIntervalMillis <= 0) {
            throw new IllegalArgumentException("invalid native damage-poison tick interval");
        }
        if (poisonExpiresAtNanos == 0) return new PoisonPoll(0, false);
        if (nowNanos >= poisonExpiresAtNanos) {
            poisonExpiresAtNanos = 0;
            poisonNextTickAtNanos = 0;
            return new PoisonPoll(0, true);
        }
        long intervalNanos = TimeUnit.MILLISECONDS.toNanos(tickIntervalMillis);
        int ticks = 0;
        while (poisonNextTickAtNanos != 0 && poisonNextTickAtNanos <= nowNanos
                && poisonNextTickAtNanos < poisonExpiresAtNanos) {
            ticks++;
            poisonNextTickAtNanos = Math.addExact(poisonNextTickAtNanos, intervalNanos);
        }
        return new PoisonPoll(ticks, false);
    }

    synchronized boolean curePoison() {
        if (poisonExpiresAtNanos == 0) return false;
        poisonExpiresAtNanos = 0;
        poisonNextTickAtNanos = 0;
        return true;
    }

    synchronized boolean poisonSilenceActive() {
        return poisonSilence;
    }

    synchronized boolean activatePoisonSilence(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid native silence-poison timing");
        }
        if (!acceptsPoison(nowNanos, PoisonKind.SILENCE)
                || poisonActive(nowNanos) || poisonSilence) return false;
        poisonSilence = true;
        return true;
    }

    synchronized boolean curePoisonSilence() {
        boolean present = poisonSilence;
        poisonSilence = false;
        return present;
    }

    synchronized boolean sleepActive(long nowNanos) {
        return sleepExpiresAtNanos != 0 && nowNanos < sleepExpiresAtNanos;
    }

    synchronized boolean activateSleep(long nowNanos, int durationSeconds) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535) {
            throw new IllegalArgumentException("invalid native sleep duration");
        }
        BattleTimedStatus recipient = this;
        long expiration = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeAcceptedState.sleepChanged(this);
        recipient.sleepExpiresAtNanos = expiration;
        return true;
    }

    synchronized boolean expireSleep(long nowNanos) {
        if (this.sleepExpiresAtNanos == 0L || nowNanos < this.sleepExpiresAtNanos) {
            return false;
        }
        BattleTimedStatus sleepRecipient = this;
        long clearedExpiration = 0L;
        NativeAcceptedState.sleepChanged(this);
        sleepRecipient.sleepExpiresAtNanos = clearedExpiration;
        return true;
    }

    synchronized boolean cancelSleep() {
        if (this.sleepExpiresAtNanos == 0L) {
            return false;
        }
        BattleTimedStatus sleepRecipient = this;
        long clearedExpiration = 0L;
        NativeAcceptedState.sleepChanged(this);
        sleepRecipient.sleepExpiresAtNanos = clearedExpiration;
        return true;
    }

    synchronized boolean freezeActive(long nowNanos) {
        return freezeExpiresAtNanos != 0 && nowNanos < freezeExpiresAtNanos;
    }

    synchronized boolean activateFreeze(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native freeze duration");
        }
        if (freezeActive(nowNanos)) return false;
        freezeExpiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
        return true;
    }

    synchronized boolean expireFreeze(long nowNanos) {
        if (freezeExpiresAtNanos == 0 || nowNanos < freezeExpiresAtNanos) return false;
        freezeExpiresAtNanos = 0;
        return true;
    }

    synchronized boolean cancelFreeze() {
        if (freezeExpiresAtNanos == 0) return false;
        freezeExpiresAtNanos = 0;
        return true;
    }

    synchronized boolean earthBindActive(long nowNanos) {
        return earthBindExpiresAtNanos != 0 && nowNanos < earthBindExpiresAtNanos;
    }

    synchronized boolean activateEarthBind(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native Earth Bind duration");
        }
        if (earthBindActive(nowNanos)) return false;
        earthBindExpiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
        return true;
    }

    synchronized boolean expireEarthBind(long nowNanos) {
        if (earthBindExpiresAtNanos == 0 || nowNanos < earthBindExpiresAtNanos) {
            return false;
        }
        earthBindExpiresAtNanos = 0;
        return true;
    }

    synchronized boolean cancelEarthBind() {
        if (earthBindExpiresAtNanos == 0) return false;
        earthBindExpiresAtNanos = 0;
        return true;
    }

    synchronized void activateAbsoluteBarrier(long nowNanos, int durationSeconds) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535) {
            throw new IllegalArgumentException("invalid native Absolute Barrier duration");
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).absoluteBarrierExpiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean absoluteBarrierActive(long nowNanos) {
        return absoluteBarrierExpiresAtNanos != 0
                && nowNanos < absoluteBarrierExpiresAtNanos;
    }

    synchronized boolean expireAbsoluteBarrier(long nowNanos) {
        if (this.absoluteBarrierExpiresAtNanos == 0L || nowNanos < this.absoluteBarrierExpiresAtNanos) {
            return false;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).absoluteBarrierExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean cancelAbsoluteBarrier() {
        if (this.absoluteBarrierExpiresAtNanos == 0L) {
            return false;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).absoluteBarrierExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean activateAdvanceSpirit(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native Advance Spirit duration");
        }
        boolean newlyActivated = advanceSpiritExpiresAtNanos == 0
                || nowNanos >= advanceSpiritExpiresAtNanos;
        advanceSpiritExpiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
        return newlyActivated;
    }

    synchronized boolean advanceSpiritActive(long nowNanos) {
        return advanceSpiritExpiresAtNanos != 0
                && nowNanos < advanceSpiritExpiresAtNanos;
    }

    synchronized boolean expireAdvanceSpirit(long nowNanos) {
        if (advanceSpiritExpiresAtNanos == 0
                || nowNanos < advanceSpiritExpiresAtNanos) return false;
        advanceSpiritExpiresAtNanos = 0;
        return true;
    }

    synchronized void activateBurningSpirit(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native Burning Spirit duration");
        }
        burningSpiritExpiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
    }

    synchronized boolean burningSpiritActive(long nowNanos) {
        return burningSpiritExpiresAtNanos != 0
                && nowNanos < burningSpiritExpiresAtNanos;
    }

    synchronized boolean expireBurningSpirit(long nowNanos) {
        if (burningSpiritExpiresAtNanos == 0
                || nowNanos < burningSpiritExpiresAtNanos) return false;
        burningSpiritExpiresAtNanos = 0;
        return true;
    }

    synchronized boolean activateElementalFire(long nowNanos, int durationSeconds) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535) {
            throw new IllegalArgumentException("invalid native Elemental Fire duration");
        }
        boolean bl = this.elementalFireExpiresAtNanos != 0L && nowNanos < this.elementalFireExpiresAtNanos;
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).elementalFireExpiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return bl;
    }

    synchronized boolean elementalFireActive(long nowNanos) {
        return elementalFireExpiresAtNanos != 0
                && nowNanos < elementalFireExpiresAtNanos;
    }

    synchronized boolean expireElementalFire(long nowNanos) {
        if (this.elementalFireExpiresAtNanos == 0L || nowNanos < this.elementalFireExpiresAtNanos) {
            return false;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).elementalFireExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean activateSoulOfFlame(long nowNanos, int durationSeconds) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535) {
            throw new IllegalArgumentException("invalid native Soul of Flame duration");
        }
        boolean bl = this.soulOfFlameExpiresAtNanos != 0L && nowNanos < this.soulOfFlameExpiresAtNanos;
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).soulOfFlameExpiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return bl;
    }

    synchronized boolean soulOfFlameActive(long nowNanos) {
        return soulOfFlameExpiresAtNanos != 0
                && nowNanos < soulOfFlameExpiresAtNanos;
    }

    synchronized boolean expireSoulOfFlame(long nowNanos) {
        if (this.soulOfFlameExpiresAtNanos == 0L || nowNanos < this.soulOfFlameExpiresAtNanos) {
            return false;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).soulOfFlameExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized void activateVenomResist(long nowNanos, int durationSeconds) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535) {
            throw new IllegalArgumentException("invalid native Venom Resist duration");
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).venomResistExpiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean venomResistActive(long nowNanos) {
        return venomResistExpiresAtNanos != 0
                && nowNanos < venomResistExpiresAtNanos;
    }

    synchronized boolean acceptsPoison(long nowNanos, PoisonKind kind) {
        if (kind == null) throw new IllegalArgumentException("missing poison kind");
        // The retained L1Poison.isValidTarget gate is shared by ordinary
        // damage, silence and paralysis poison installers.
        return !venomResistActive(nowNanos);
    }

    synchronized boolean expireVenomResist(long nowNanos) {
        if (this.venomResistExpiresAtNanos == 0L || nowNanos < this.venomResistExpiresAtNanos) {
            return false;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).venomResistExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized void activateDoubleBrake(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native Double Brake duration");
        }
        doubleBrakeExpiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
    }

    synchronized boolean doubleBrakeActive(long nowNanos) {
        return doubleBrakeExpiresAtNanos != 0
                && nowNanos < doubleBrakeExpiresAtNanos;
    }

    synchronized boolean expireDoubleBrake(long nowNanos) {
        if (doubleBrakeExpiresAtNanos == 0
                || nowNanos < doubleBrakeExpiresAtNanos) return false;
        doubleBrakeExpiresAtNanos = 0;
        return true;
    }

    /** Both native effects own the same S_PARALYSE 12/13 client lock. */
    synchronized boolean freezeVisualLockActive(long nowNanos) {
        return freezeActive(nowNanos) || earthBindActive(nowNanos);
    }

    synchronized boolean actionLocked(long nowNanos) {
        return sleepActive(nowNanos) || freezeVisualLockActive(nowNanos)
                || paralysisActive(nowNanos) || shockStunActive(nowNanos)
                || boneBreakActive(nowNanos)
                || thunderGrabActive(nowNanos);
    }

    /**
     * Retained Shock Stun behavior: a player recast adds the rounded-up remaining
     * lease to the new 2-4 second roll, capped at four seconds total.
     */
    synchronized ShockStunActivation activateShockStun(
            long nowNanos, int rolledSeconds, int maximumSeconds) {
        if (nowNanos < 0 || rolledSeconds <= 0 || maximumSeconds <= 0
                || rolledSeconds > maximumSeconds || maximumSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native Shock Stun duration");
        }
        boolean refreshed = shockStunActive(nowNanos);
        int remainingSeconds = 0;
        if (refreshed) {
            long remainingNanos = shockStunExpiresAtNanos - nowNanos;
            long rounded = (remainingNanos + TimeUnit.SECONDS.toNanos(1) - 1)
                    / TimeUnit.SECONDS.toNanos(1);
            remainingSeconds = (int) Math.min(maximumSeconds, Math.max(1L, rounded));
        }
        int durationSeconds = Math.min(maximumSeconds,
                Math.addExact(remainingSeconds, rolledSeconds));
        shockStunExpiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
        return new ShockStunActivation(durationSeconds, refreshed);
    }

    synchronized boolean shockStunActive(long nowNanos) {
        return shockStunExpiresAtNanos != 0 && nowNanos < shockStunExpiresAtNanos;
    }

    synchronized boolean expireShockStun(long nowNanos) {
        if (shockStunExpiresAtNanos == 0 || nowNanos < shockStunExpiresAtNanos) {
            return false;
        }
        shockStunExpiresAtNanos = 0;
        return true;
    }

    synchronized boolean cancelShockStun() {
        if (shockStunExpiresAtNanos == 0) return false;
        shockStunExpiresAtNanos = 0;
        return true;
    }

    /** Retained skill208 does not roll or refresh while its own lease exists. */
    synchronized boolean activateBoneBreak(long nowNanos,
                                            int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0
                || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException(
                    "invalid native Bone Break duration");
        }
        if (boneBreakActive(nowNanos)) return false;
        boneBreakExpiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
        return true;
    }

    synchronized boolean boneBreakActive(long nowNanos) {
        return boneBreakExpiresAtNanos != 0
                && nowNanos < boneBreakExpiresAtNanos;
    }

    synchronized boolean expireBoneBreak(long nowNanos) {
        if (boneBreakExpiresAtNanos == 0
                || nowNanos < boneBreakExpiresAtNanos) return false;
        boneBreakExpiresAtNanos = 0;
        return true;
    }

    synchronized boolean cancelBoneBreak() {
        if (boneBreakExpiresAtNanos == 0) return false;
        boneBreakExpiresAtNanos = 0;
        return true;
    }

    /** Retained skill192 does not refresh an existing MOVE_STOP lease. */
    synchronized boolean activateThunderGrab(long nowNanos,
                                             int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0
                || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException(
                    "invalid native Thunder Grab duration");
        }
        if (thunderGrabActive(nowNanos)) return false;
        thunderGrabExpiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
        return true;
    }

    synchronized boolean thunderGrabActive(long nowNanos) {
        return thunderGrabExpiresAtNanos != 0
                && nowNanos < thunderGrabExpiresAtNanos;
    }

    synchronized boolean expireThunderGrab(long nowNanos) {
        if (thunderGrabExpiresAtNanos == 0
                || nowNanos < thunderGrabExpiresAtNanos) return false;
        thunderGrabExpiresAtNanos = 0;
        return true;
    }

    synchronized boolean cancelThunderGrab() {
        if (thunderGrabExpiresAtNanos == 0) return false;
        thunderGrabExpiresAtNanos = 0;
        return true;
    }

    synchronized boolean activateHorrorOfDeath(long nowNanos, int durationSeconds) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535) {
            throw new IllegalArgumentException("invalid native Horror of Death duration");
        }
        boolean bl = this.horrorOfDeathActive(nowNanos);
        if (bl) {
            return true;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).horrorOfDeathExpiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return false;
    }

    synchronized boolean horrorOfDeathActive(long nowNanos) {
        return horrorOfDeathExpiresAtNanos != 0
                && nowNanos < horrorOfDeathExpiresAtNanos;
    }

    synchronized boolean expireHorrorOfDeath(long nowNanos) {
        if (this.horrorOfDeathExpiresAtNanos == 0L || nowNanos < this.horrorOfDeathExpiresAtNanos) {
            return false;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).horrorOfDeathExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean cancelHorrorOfDeath() {
        if (this.horrorOfDeathExpiresAtNanos == 0L) {
            return false;
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).horrorOfDeathExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    /** Warning/gray-palette phase plus the later fully locked phase. */
    synchronized boolean activateParalysis(long nowNanos, int warningDelayMillis,
                                           int activeDurationMillis) {
        // Retained Curse Paralysis gives players a warning lease, but applies
        // the same status to L1MonsterInstance with an exact zero delay.
        if (nowNanos < 0 || warningDelayMillis < 0 || activeDurationMillis <= 0) {
            throw new IllegalArgumentException("invalid native curse-paralysis timing");
        }
        if (paralysisActivatesAtNanos != 0 && nowNanos < paralysisExpiresAtNanos) {
            return false;
        }
        long activation = Math.addExact(nowNanos,
                TimeUnit.MILLISECONDS.toNanos(warningDelayMillis));
        paralysisActivatesAtNanos = activation;
        paralysisExpiresAtNanos = Math.addExact(activation,
                TimeUnit.MILLISECONDS.toNanos(activeDurationMillis));
        paralysisLockEmitted = false;
        return true;
    }

    synchronized boolean paralysisVisualActive(long nowNanos) {
        return CombatPoisonParalysis.otherVisual(this.paralysisActivatesAtNanos != 0L && nowNanos < this.paralysisExpiresAtNanos, this, nowNanos);
    }

    synchronized boolean paralysisActive(long nowNanos) {
        return CombatPoisonParalysis.otherActive(this.paralysisActivatesAtNanos != 0L && nowNanos >= this.paralysisActivatesAtNanos && nowNanos < this.paralysisExpiresAtNanos, this, nowNanos);
    }

    synchronized ParalysisPoll pollParalysis(long nowNanos) {
        if (paralysisActivatesAtNanos == 0) {
            return new ParalysisPoll(false, false, false);
        }
        if (nowNanos >= paralysisExpiresAtNanos) {
            boolean unlockRequired = paralysisLockEmitted;
            clearParalysisFields();
            return new ParalysisPoll(false, true, unlockRequired);
        }
        if (nowNanos >= paralysisActivatesAtNanos && !paralysisLockEmitted) {
            paralysisLockEmitted = true;
            return new ParalysisPoll(true, false, false);
        }
        return new ParalysisPoll(false, false, false);
    }

    /** Marks a reconstructed local world as already sent mode 2. */
    synchronized boolean noteParalysisLockSent(long nowNanos) {
        if (!paralysisActive(nowNanos)) return false;
        paralysisLockEmitted = true;
        return true;
    }

    synchronized ParalysisCancellation cancelParalysis() {
        if (paralysisActivatesAtNanos == 0) {
            return new ParalysisCancellation(false, false);
        }
        boolean unlockRequired = paralysisLockEmitted;
        clearParalysisFields();
        return new ParalysisCancellation(true, unlockRequired);
    }

    /** Retained skill 37 calls curePoison, which includes silence poison. */
    synchronized CurseRemoval removeCurse() {
        boolean poisonCured = curePoison() | curePoisonSilence();
        return new CurseRemoval(poisonCured, cancelParalysis());
    }

    synchronized MagicCancellation cancelMagicEffects() {
        boolean bl = this.hasteExpiresAtNanos != 0L;
        boolean bl2 = this.slowExpiresAtNanos != 0L;
        boolean bl3 = this.braveExpiresAtNanos != 0L;
        boolean bl4 = NativeBlindSourceState.removed(this.blindExpiresAtNanos != 0L);
        boolean bl5 = this.weaknessExpiresAtNanos != 0L;
        boolean bl6 = this.diseaseExpiresAtNanos != 0L;
        boolean bl7 = this.silenceExpiresAtNanos != 0L || this.areaSilenceExpiresAtNanos != 0L || this.poisonSilence;
        boolean bl8 = this.decayPotionExpiresAtNanos != 0L;
        boolean bl9 = this.polluteWaterExpiresAtNanos != 0L;
        boolean bl10 = this.strikerGaleExpiresAtNanos != 0L;
        boolean bl11 = this.lightExpiresAtNanos != 0L;
        boolean bl12 = this.bluePotionExpiresAtNanos != 0L;
        boolean bl13 = this.wisdomPotionExpiresAtNanos != 0L;
        boolean bl14 = this.meditationExpiresAtNanos != 0L;
        boolean bl15 = this.concentrationExpiresAtNanos != 0L;
        boolean bl16 = this.counterMagicExpiresAtNanos != 0L;
        boolean bl17 = this.breathExpiresAtNanos != 0L;
        boolean bl18 = this.poisonExpiresAtNanos != 0L || this.poisonSilence;
        boolean bl19 = this.sleepExpiresAtNanos != 0L;
        boolean bl20 = this.freezeExpiresAtNanos != 0L;
        boolean bl21 = this.earthBindExpiresAtNanos != 0L;
        boolean bl22 = this.burningSpiritExpiresAtNanos != 0L;
        boolean bl23 = this.elementalFireExpiresAtNanos != 0L;
        boolean bl24 = this.soulOfFlameExpiresAtNanos != 0L;
        boolean bl25 = this.venomResistExpiresAtNanos != 0L;
        boolean bl26 = this.doubleBrakeExpiresAtNanos != 0L;
        boolean bl27 = this.horrorOfDeathExpiresAtNanos != 0L;
        boolean bl28 = this.thunderGrabExpiresAtNanos != 0L;
        boolean bl29 = this.boneBreakExpiresAtNanos != 0L;
        ParalysisCancellation paralysisCancellation = this.cancelParalysis();
        this.hasteExpiresAtNanos = 0L;
        this.hasteSkillId = 0;
        this.slowExpiresAtNanos = 0L;
        this.slowSkillId = 0;
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).braveExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        int n = 0;
        battleTimedStatus = this;
        this.nativeSourceIcons1BraveSkill = n;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        n = 0;
        battleTimedStatus = this;
        this.braveMode = n;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        this.blindExpiresAtNanos = NativeBlindSourceState.cleared(this.blindExpiresAtNanos);
        long l2 = 0L;
        battleTimedStatus = this;
        this.weaknessExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        l2 = 0L;
        battleTimedStatus = this;
        this.diseaseExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        this.silenceExpiresAtNanos = 0L;
        this.areaSilenceExpiresAtNanos = 0L;
        l2 = 0L;
        battleTimedStatus = this;
        this.decayPotionExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        l2 = 0L;
        battleTimedStatus = this;
        this.polluteWaterExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        l2 = 0L;
        battleTimedStatus = this;
        this.strikerGaleExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        this.lightExpiresAtNanos = 0L;
        this.bluePotionExpiresAtNanos = 0L;
        this.wisdomPotionExpiresAtNanos = 0L;
        l2 = 0L;
        battleTimedStatus = this;
        this.meditationExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        l2 = 0L;
        battleTimedStatus = this;
        this.concentrationExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        l2 = 0L;
        battleTimedStatus = this;
        this.counterMagicExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        this.breathExpiresAtNanos = 0L;
        this.poisonExpiresAtNanos = 0L;
        this.poisonNextTickAtNanos = 0L;
        this.poisonSilence = false;
        BattleTimedStatus sleepRecipient = this;
        long clearedExpiration = 0L;
        NativeAcceptedState.sleepChanged(this);
        sleepRecipient.sleepExpiresAtNanos = clearedExpiration;
        this.freezeExpiresAtNanos = 0L;
        this.earthBindExpiresAtNanos = 0L;
        this.burningSpiritExpiresAtNanos = 0L;
        l2 = 0L;
        battleTimedStatus = this;
        this.elementalFireExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        l2 = 0L;
        battleTimedStatus = this;
        this.soulOfFlameExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        l2 = 0L;
        battleTimedStatus = this;
        this.venomResistExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        this.doubleBrakeExpiresAtNanos = 0L;
        l2 = 0L;
        battleTimedStatus = this;
        this.horrorOfDeathExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        this.thunderGrabExpiresAtNanos = 0L;
        this.boneBreakExpiresAtNanos = 0L;
        return new MagicCancellation(bl, bl2, bl3, bl4, bl5, bl6, bl7, bl8, bl9, bl10, bl11, bl12, bl13, bl14, bl15, bl16, bl17, bl18, bl19, bl20, bl21, bl22, bl23, bl24, bl25, bl26, bl27, bl28, bl29, paralysisCancellation);
    }

    private void clearParalysisFields() {
        paralysisActivatesAtNanos = 0;
        paralysisExpiresAtNanos = 0;
        paralysisLockEmitted = false;
    }

    synchronized void clear() {
        NativeDeClientViewer.retire(this);
        NativeDeActorState.clear(this);
        NativeProcNpcState.clear(this);
        NativeProcPlayerState.clear(this);
        CombatPoisonParalysis.discard(this);
        this.hasteExpiresAtNanos = 0L;
        this.hasteSkillId = 0;
        this.slowExpiresAtNanos = 0L;
        this.slowSkillId = 0;
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).braveExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        int n = 0;
        battleTimedStatus = this;
        this.nativeSourceIcons1BraveSkill = n;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        n = 0;
        battleTimedStatus = this;
        this.braveMode = n;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        this.blindExpiresAtNanos = 0L;
        long l2 = 0L;
        battleTimedStatus = this;
        this.weaknessExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        l2 = 0L;
        battleTimedStatus = this;
        this.diseaseExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        this.silenceExpiresAtNanos = 0L;
        this.areaSilenceExpiresAtNanos = 0L;
        l2 = 0L;
        battleTimedStatus = this;
        this.decayPotionExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        l2 = 0L;
        battleTimedStatus = this;
        this.polluteWaterExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        l2 = 0L;
        battleTimedStatus = this;
        this.strikerGaleExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        this.waterDragonPotionErosionExpiresAtNanos = 0L;
        this.waterDragonDirtyCurrentExpiresAtNanos = 0L;
        this.waterDragonHealingErosionExpiresAtNanos = 0L;
        this.lightExpiresAtNanos = 0L;
        this.bluePotionExpiresAtNanos = 0L;
        this.wisdomPotionExpiresAtNanos = 0L;
        l2 = 0L;
        battleTimedStatus = this;
        this.meditationExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        l2 = 0L;
        battleTimedStatus = this;
        this.concentrationExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        l2 = 0L;
        battleTimedStatus = this;
        this.counterMagicExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        this.breathExpiresAtNanos = 0L;
        this.poisonExpiresAtNanos = 0L;
        this.poisonNextTickAtNanos = 0L;
        this.poisonSilence = false;
        BattleTimedStatus sleepRecipient = this;
        long clearedExpiration = 0L;
        NativeAcceptedState.sleepChanged(this);
        sleepRecipient.sleepExpiresAtNanos = clearedExpiration;
        this.freezeExpiresAtNanos = 0L;
        this.earthBindExpiresAtNanos = 0L;
        l2 = 0L;
        battleTimedStatus = this;
        this.absoluteBarrierExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        this.advanceSpiritExpiresAtNanos = 0L;
        this.burningSpiritExpiresAtNanos = 0L;
        l2 = 0L;
        battleTimedStatus = this;
        this.elementalFireExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        l2 = 0L;
        battleTimedStatus = this;
        this.soulOfFlameExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        l2 = 0L;
        battleTimedStatus = this;
        this.venomResistExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        this.doubleBrakeExpiresAtNanos = 0L;
        this.shockStunExpiresAtNanos = 0L;
        this.boneBreakExpiresAtNanos = 0L;
        this.thunderGrabExpiresAtNanos = 0L;
        l2 = 0L;
        battleTimedStatus = this;
        this.horrorOfDeathExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        this.drunken = false;
        this.clearParalysisFields();
        NativeShockPlayerAction.deathReset(this);
    }

    synchronized void nativeSourceProcActivateDecayPotion(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Source proc Decay Potion duration");
        }
        BattleTimedStatus battleTimedStatus;
        (battleTimedStatus = this).decayPotionExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    record Speed(int type, int remainingSeconds) {
    }

    record SpeedChange(int type, int durationSeconds, int stoppedSkillId) {
    }

    record SlowExpiration(boolean expired, int skillId) {
    }

    record HasteExpiration(boolean expired, int skillId) {
    }

    record Brave(int mode, int remainingSeconds) {
    }

    record Breath(int remainingSeconds) {
    }

    static enum PoisonKind {
        DAMAGE,
        SILENCE,
        PARALYSIS;

    }

    record PoisonPoll(int damageTicks, boolean expired) {
    }

    record ShockStunActivation(int durationSeconds, boolean refreshed) {
    }

    record ParalysisPoll(boolean activated, boolean expired, boolean unlockRequired) {
    }

    record ParalysisCancellation(boolean present, boolean unlockRequired) {
    }

    record CurseRemoval(boolean poisonCured, ParalysisCancellation paralysisCancellation) {
    }

    record MagicCancellation(boolean haste, boolean slow, boolean brave, boolean blind, boolean weakness, boolean disease, boolean silence, boolean decayPotion, boolean polluteWater, boolean strikerGale, boolean light, boolean bluePotion, boolean wisdomPotion, boolean meditation, boolean concentration, boolean counterMagic, boolean breath, boolean poison, boolean sleep, boolean freeze, boolean earthBind, boolean burningSpirit, boolean elementalFire, boolean soulOfFlame, boolean venomResist, boolean doubleBrake, boolean horrorOfDeath, boolean thunderGrab, boolean boneBreak, ParalysisCancellation paralysisCancellation) {
    }
}
