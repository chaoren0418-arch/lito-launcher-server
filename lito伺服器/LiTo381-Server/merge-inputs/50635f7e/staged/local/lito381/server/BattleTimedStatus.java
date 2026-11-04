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

    synchronized Speed speed(long l) {
        Speed speed = NativeSourceDollSlow.extendedSpeed(this, l);
        if (speed != null) {
            return speed;
        }
        long l2 = 0L;
        int n = 0;
        if (this.slowExpiresAtNanos != 0L && l < this.slowExpiresAtNanos) {
            l2 = this.slowExpiresAtNanos;
            n = 2;
        } else if (this.hasteExpiresAtNanos != 0L && l < this.hasteExpiresAtNanos) {
            l2 = this.hasteExpiresAtNanos;
            n = 1;
        }
        if (n == 0) {
            return new Speed(0, 0);
        }
        long l3 = l2 - l;
        long l4 = (l3 + TimeUnit.SECONDS.toNanos(1L) - 1L) / TimeUnit.SECONDS.toNanos(1L);
        return new Speed(n, (int)Math.min(65535L, Math.max(1L, l4)));
    }

    synchronized SpeedChange activateHaste(long l, int n) {
        return this.activateHaste(l, n, 0);
    }

    synchronized SpeedChange activateHaste(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native haste duration");
        }
        if (n2 != 0 && n2 != 43 && n2 != 54) {
            throw new IllegalArgumentException("invalid native haste source skill");
        }
        if (this.slowExpiresAtNanos != 0L && l < this.slowExpiresAtNanos) {
            int n3 = this.slowSkillId;
            this.slowExpiresAtNanos = 0L;
            this.slowSkillId = 0;
            return new SpeedChange(0, 0, n3);
        }
        this.slowExpiresAtNanos = 0L;
        this.slowSkillId = 0;
        this.hasteExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        this.hasteSkillId = n2;
        return new SpeedChange(1, n, 0);
    }

    synchronized SpeedChange activateSlow(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n > 65535 || n2 != 29 && n2 != 76 && n2 != 152) {
            throw new IllegalArgumentException("invalid native slow lease");
        }
        if (this.hasteExpiresAtNanos != 0L && l < this.hasteExpiresAtNanos) {
            int n3 = this.hasteSkillId;
            this.hasteExpiresAtNanos = 0L;
            this.hasteSkillId = 0;
            return new SpeedChange(0, 0, n3);
        }
        this.hasteExpiresAtNanos = 0L;
        this.hasteSkillId = 0;
        this.slowExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        this.slowSkillId = n2;
        return new SpeedChange(2, n, 0);
    }

    synchronized int slowSkillId(long l) {
        return this.slowExpiresAtNanos != 0L && l < this.slowExpiresAtNanos ? this.slowSkillId : 0;
    }

    synchronized SlowExpiration expireSlowLease(long l) {
        if (this.slowExpiresAtNanos == 0L || l < this.slowExpiresAtNanos) {
            return new SlowExpiration(false, 0);
        }
        int n = this.slowSkillId;
        this.slowExpiresAtNanos = 0L;
        this.slowSkillId = 0;
        return new SlowExpiration(true, n);
    }

    synchronized int hasteSkillId(long l) {
        return this.hasteExpiresAtNanos != 0L && l < this.hasteExpiresAtNanos ? this.hasteSkillId : 0;
    }

    synchronized boolean expireHaste(long l) {
        return this.expireHasteLease(l).expired();
    }

    synchronized HasteExpiration expireHasteLease(long l) {
        if (this.hasteExpiresAtNanos == 0L || l < this.hasteExpiresAtNanos) {
            return new HasteExpiration(false, 0);
        }
        int n = this.hasteSkillId;
        this.hasteExpiresAtNanos = 0L;
        this.hasteSkillId = 0;
        return new HasteExpiration(true, n);
    }

    synchronized Brave brave(long l) {
        if (this.braveExpiresAtNanos == 0L || l >= this.braveExpiresAtNanos) {
            return new Brave(0, 0);
        }
        long l2 = this.braveExpiresAtNanos - l;
        long l3 = (l2 + TimeUnit.SECONDS.toNanos(1L) - 1L) / TimeUnit.SECONDS.toNanos(1L);
        return new Brave(this.braveMode, (int)Math.min(65535L, Math.max(1L, l3)));
    }

    synchronized void activateBrave(long l, int n) {
        this.activateBrave(l, n, 1);
    }

    synchronized void activateBrave(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native brave duration");
        }
        if (n2 != 1 && n2 != 3 && n2 != 4 && n2 != 5 && n2 != 6) {
            throw new IllegalArgumentException("invalid native brave mode");
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleTimedStatus battleTimedStatus = this;
        this.braveExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        int n3 = 0;
        battleTimedStatus = this;
        this.nativeSourceIcons1BraveSkill = n3;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        n3 = n2;
        battleTimedStatus = this;
        this.braveMode = n3;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean expireBrave(long l) {
        if (this.braveExpiresAtNanos == 0L || l < this.braveExpiresAtNanos) {
            return false;
        }
        long l2 = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.braveExpiresAtNanos = l2;
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

    synchronized boolean blindActive(long l) {
        return this.blindExpiresAtNanos != 0L && l < this.blindExpiresAtNanos;
    }

    synchronized void activateBlind(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native blindness duration");
        }
        this.blindExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
    }

    synchronized boolean expireBlind(long l) {
        if (this.blindExpiresAtNanos == 0L || l < this.blindExpiresAtNanos) {
            return false;
        }
        this.blindExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean weaknessActive(long l) {
        return this.weaknessExpiresAtNanos != 0L && l < this.weaknessExpiresAtNanos;
    }

    synchronized void activateWeakness(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Weakness duration");
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleTimedStatus battleTimedStatus = this;
        this.weaknessExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean expireWeakness(long l) {
        if (this.weaknessExpiresAtNanos == 0L || l < this.weaknessExpiresAtNanos) {
            return false;
        }
        long l2 = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.weaknessExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean diseaseActive(long l) {
        return this.diseaseExpiresAtNanos != 0L && l < this.diseaseExpiresAtNanos;
    }

    synchronized void activateDisease(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Disease duration");
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleTimedStatus battleTimedStatus = this;
        this.diseaseExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean expireDisease(long l) {
        if (this.diseaseExpiresAtNanos == 0L || l < this.diseaseExpiresAtNanos) {
            return false;
        }
        long l2 = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.diseaseExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean silenceActive(long l) {
        return this.poisonSilence || this.silenceExpiresAtNanos != 0L && l < this.silenceExpiresAtNanos || this.areaSilenceActive(l);
    }

    synchronized void activateSilence(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid Silence duration");
        }
        this.silenceExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
    }

    synchronized boolean activateSilenceIfAbsent(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid Confusion Silence duration");
        }
        if (this.silenceExpiresAtNanos != 0L && l < this.silenceExpiresAtNanos) {
            return false;
        }
        this.silenceExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        return true;
    }

    synchronized boolean expireSilence(long l) {
        if (this.silenceExpiresAtNanos == 0L || l < this.silenceExpiresAtNanos) {
            return false;
        }
        this.silenceExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean consumeSilenceForNpcSpell(long l) {
        if (l < 0L) {
            throw new IllegalArgumentException("invalid Silence attempt time");
        }
        if (this.silenceExpiresAtNanos == 0L || l >= this.silenceExpiresAtNanos) {
            this.silenceExpiresAtNanos = 0L;
            return false;
        }
        this.silenceExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean areaSilenceActive(long l) {
        return this.areaSilenceExpiresAtNanos != 0L && l < this.areaSilenceExpiresAtNanos;
    }

    synchronized boolean activateAreaSilence(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid Area of Silence duration");
        }
        boolean bl = this.areaSilenceActive(l);
        this.areaSilenceExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        return bl;
    }

    synchronized boolean expireAreaSilence(long l) {
        if (this.areaSilenceExpiresAtNanos == 0L || l < this.areaSilenceExpiresAtNanos) {
            return false;
        }
        this.areaSilenceExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean decayPotionActive(long l) {
        return this.decayPotionExpiresAtNanos != 0L && l < this.decayPotionExpiresAtNanos;
    }

    synchronized boolean activateDecayPotion(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid Decay Potion duration");
        }
        if (this.decayPotionActive(l)) {
            return false;
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleTimedStatus battleTimedStatus = this;
        this.decayPotionExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean expireDecayPotion(long l) {
        if (this.decayPotionExpiresAtNanos == 0L || l < this.decayPotionExpiresAtNanos) {
            return false;
        }
        long l2 = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.decayPotionExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean polluteWaterActive(long l) {
        return this.polluteWaterExpiresAtNanos != 0L && l < this.polluteWaterExpiresAtNanos;
    }

    synchronized void activatePolluteWater(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid Pollute Water duration");
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleTimedStatus battleTimedStatus = this;
        this.polluteWaterExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean expirePolluteWater(long l) {
        if (this.polluteWaterExpiresAtNanos == 0L || l < this.polluteWaterExpiresAtNanos) {
            return false;
        }
        long l2 = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.polluteWaterExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean strikerGaleActive(long l) {
        return this.strikerGaleExpiresAtNanos != 0L && l < this.strikerGaleExpiresAtNanos;
    }

    synchronized void activateStrikerGale(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid Striker Gale duration");
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleTimedStatus battleTimedStatus = this;
        this.strikerGaleExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean expireStrikerGale(long l) {
        if (this.strikerGaleExpiresAtNanos == 0L || l < this.strikerGaleExpiresAtNanos) {
            return false;
        }
        long l2 = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.strikerGaleExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized BattleWaterDragonAffliction.Kind waterDragonAffliction(long l) {
        this.expireWaterDragonAffliction(l);
        if (this.waterDragonPotionErosionExpiresAtNanos != 0L) {
            return BattleWaterDragonAffliction.Kind.POTION_EROSION;
        }
        if (this.waterDragonDirtyCurrentExpiresAtNanos != 0L) {
            return BattleWaterDragonAffliction.Kind.DIRTY_CURRENT;
        }
        if (this.waterDragonHealingErosionExpiresAtNanos != 0L) {
            return BattleWaterDragonAffliction.Kind.HEALING_EROSION;
        }
        return null;
    }

    synchronized boolean activateWaterDragonAffliction(BattleWaterDragonAffliction.Kind kind, long l, int n) {
        if (kind == null || l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid Water Dragon affliction");
        }
        if (this.waterDragonAffliction(l) != null) {
            return false;
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        switch (kind) {
            case POTION_EROSION: {
                this.waterDragonPotionErosionExpiresAtNanos = l2;
                break;
            }
            case DIRTY_CURRENT: {
                this.waterDragonDirtyCurrentExpiresAtNanos = l2;
                break;
            }
            case HEALING_EROSION: {
                this.waterDragonHealingErosionExpiresAtNanos = l2;
            }
        }
        return true;
    }

    synchronized boolean waterDragonPotionErosionActive(long l) {
        return this.waterDragonAffliction(l) == BattleWaterDragonAffliction.Kind.POTION_EROSION;
    }

    synchronized boolean waterDragonDirtyCurrentActive(long l) {
        return this.waterDragonAffliction(l) == BattleWaterDragonAffliction.Kind.DIRTY_CURRENT;
    }

    synchronized boolean waterDragonHealingErosionActive(long l) {
        return this.waterDragonAffliction(l) == BattleWaterDragonAffliction.Kind.HEALING_EROSION;
    }

    synchronized boolean expireWaterDragonAffliction(long l) {
        boolean bl = false;
        if (this.waterDragonPotionErosionExpiresAtNanos != 0L && l >= this.waterDragonPotionErosionExpiresAtNanos) {
            this.waterDragonPotionErosionExpiresAtNanos = 0L;
            bl = true;
        }
        if (this.waterDragonDirtyCurrentExpiresAtNanos != 0L && l >= this.waterDragonDirtyCurrentExpiresAtNanos) {
            this.waterDragonDirtyCurrentExpiresAtNanos = 0L;
            bl = true;
        }
        if (this.waterDragonHealingErosionExpiresAtNanos != 0L && l >= this.waterDragonHealingErosionExpiresAtNanos) {
            this.waterDragonHealingErosionExpiresAtNanos = 0L;
            bl = true;
        }
        return bl;
    }

    synchronized BattleWaterDragonAffliction.Kind clearWaterDragonAffliction() {
        BattleWaterDragonAffliction.Kind kind = null;
        if (this.waterDragonPotionErosionExpiresAtNanos != 0L) {
            kind = BattleWaterDragonAffliction.Kind.POTION_EROSION;
        } else if (this.waterDragonDirtyCurrentExpiresAtNanos != 0L) {
            kind = BattleWaterDragonAffliction.Kind.DIRTY_CURRENT;
        } else if (this.waterDragonHealingErosionExpiresAtNanos != 0L) {
            kind = BattleWaterDragonAffliction.Kind.HEALING_EROSION;
        }
        this.waterDragonPotionErosionExpiresAtNanos = 0L;
        this.waterDragonDirtyCurrentExpiresAtNanos = 0L;
        this.waterDragonHealingErosionExpiresAtNanos = 0L;
        return kind;
    }

    synchronized void activateLight(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native light-spell duration");
        }
        this.lightExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
    }

    synchronized boolean lightActive(long l) {
        return this.lightExpiresAtNanos != 0L && l < this.lightExpiresAtNanos;
    }

    synchronized boolean expireLight(long l) {
        if (this.lightExpiresAtNanos == 0L || l < this.lightExpiresAtNanos) {
            return false;
        }
        this.lightExpiresAtNanos = 0L;
        return true;
    }

    synchronized void activateBluePotion(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native blue-potion duration");
        }
        this.bluePotionExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
    }

    synchronized boolean bluePotionActive(long l) {
        return this.bluePotionExpiresAtNanos != 0L && l < this.bluePotionExpiresAtNanos;
    }

    synchronized boolean expireBluePotion(long l) {
        if (this.bluePotionExpiresAtNanos == 0L || l < this.bluePotionExpiresAtNanos) {
            return false;
        }
        this.bluePotionExpiresAtNanos = 0L;
        return true;
    }

    synchronized void activateWisdomPotion(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native wisdom-potion duration");
        }
        this.wisdomPotionExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
    }

    synchronized boolean wisdomPotionActive(long l) {
        return this.wisdomPotionExpiresAtNanos != 0L && l < this.wisdomPotionExpiresAtNanos;
    }

    synchronized boolean expireWisdomPotion(long l) {
        if (this.wisdomPotionExpiresAtNanos == 0L || l < this.wisdomPotionExpiresAtNanos) {
            return false;
        }
        this.wisdomPotionExpiresAtNanos = 0L;
        return true;
    }

    synchronized void activateMeditation(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native meditation duration");
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleTimedStatus battleTimedStatus = this;
        this.meditationExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean meditationActive(long l) {
        return this.meditationExpiresAtNanos != 0L && l < this.meditationExpiresAtNanos;
    }

    synchronized boolean expireMeditation(long l) {
        if (this.meditationExpiresAtNanos == 0L || l < this.meditationExpiresAtNanos) {
            return false;
        }
        long l2 = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.meditationExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean cancelMeditation() {
        if (this.meditationExpiresAtNanos == 0L) {
            return false;
        }
        long l = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.meditationExpiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized void activateConcentration(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native concentration duration");
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleTimedStatus battleTimedStatus = this;
        this.concentrationExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean concentrationActive(long l) {
        return this.concentrationExpiresAtNanos != 0L && l < this.concentrationExpiresAtNanos;
    }

    synchronized boolean expireConcentration(long l) {
        if (this.concentrationExpiresAtNanos == 0L || l < this.concentrationExpiresAtNanos) {
            return false;
        }
        long l2 = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.concentrationExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized void activateCounterMagic(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Counter Magic duration");
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleTimedStatus battleTimedStatus = this;
        this.counterMagicExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean counterMagicActive(long l) {
        return this.counterMagicExpiresAtNanos != 0L && l < this.counterMagicExpiresAtNanos;
    }

    synchronized boolean expireCounterMagic(long l) {
        if (this.counterMagicExpiresAtNanos == 0L || l < this.counterMagicExpiresAtNanos) {
            return false;
        }
        long l2 = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.counterMagicExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean consumeCounterMagic(long l) {
        if (!this.counterMagicActive(l)) {
            if (this.counterMagicExpiresAtNanos != 0L && l >= this.counterMagicExpiresAtNanos) {
                long l2 = 0L;
                BattleTimedStatus battleTimedStatus = this;
                this.counterMagicExpiresAtNanos = l2;
                NativeSourceIcons1Epoch.bump(battleTimedStatus);
            }
            return false;
        }
        long l3 = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.counterMagicExpiresAtNanos = l3;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized Breath breath(long l) {
        if (this.breathExpiresAtNanos == 0L || l >= this.breathExpiresAtNanos) {
            return new Breath(0);
        }
        long l2 = this.breathExpiresAtNanos - l;
        long l3 = (l2 + TimeUnit.SECONDS.toNanos(1L) - 1L) / TimeUnit.SECONDS.toNanos(1L);
        return new Breath((int)Math.min(65535L, Math.max(1L, l3)));
    }

    synchronized Breath addBreath(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n2 <= 0 || n2 > 65535) {
            throw new IllegalArgumentException("invalid native underwater-breath duration");
        }
        int n3 = this.breath(l).remainingSeconds();
        int n4 = Math.min(n2, Math.addExact(n3, n));
        this.breathExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n4));
        return new Breath(n4);
    }

    synchronized boolean expireBreath(long l) {
        if (this.breathExpiresAtNanos == 0L || l < this.breathExpiresAtNanos) {
            return false;
        }
        this.breathExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean cancelBreath() {
        if (this.breathExpiresAtNanos == 0L) {
            return false;
        }
        this.breathExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean drunkenActive() {
        return this.drunken;
    }

    synchronized void activateDrunken() {
        this.drunken = true;
    }

    synchronized boolean clearDrunken() {
        boolean bl = this.drunken;
        this.drunken = false;
        return bl;
    }

    synchronized boolean poisonActive(long l) {
        return this.poisonExpiresAtNanos != 0L && l < this.poisonExpiresAtNanos;
    }

    synchronized boolean activatePoison(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n > 65535 || n2 <= 0) {
            throw new IllegalArgumentException("invalid native damage-poison timing");
        }
        if (!this.acceptsPoison(l, PoisonKind.DAMAGE) || this.poisonActive(l) || this.poisonSilence) {
            return false;
        }
        this.poisonExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        this.poisonNextTickAtNanos = Math.addExact(l, TimeUnit.MILLISECONDS.toNanos(n2));
        return true;
    }

    synchronized PoisonPoll pollPoison(long l, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("invalid native damage-poison tick interval");
        }
        if (this.poisonExpiresAtNanos == 0L) {
            return new PoisonPoll(0, false);
        }
        if (l >= this.poisonExpiresAtNanos) {
            this.poisonExpiresAtNanos = 0L;
            this.poisonNextTickAtNanos = 0L;
            return new PoisonPoll(0, true);
        }
        long l2 = TimeUnit.MILLISECONDS.toNanos(n);
        int n2 = 0;
        while (this.poisonNextTickAtNanos != 0L && this.poisonNextTickAtNanos <= l && this.poisonNextTickAtNanos < this.poisonExpiresAtNanos) {
            ++n2;
            this.poisonNextTickAtNanos = Math.addExact(this.poisonNextTickAtNanos, l2);
        }
        return new PoisonPoll(n2, false);
    }

    synchronized boolean curePoison() {
        if (this.poisonExpiresAtNanos == 0L) {
            return false;
        }
        this.poisonExpiresAtNanos = 0L;
        this.poisonNextTickAtNanos = 0L;
        return true;
    }

    synchronized boolean poisonSilenceActive() {
        return this.poisonSilence;
    }

    synchronized boolean activatePoisonSilence(long l) {
        if (l < 0L) {
            throw new IllegalArgumentException("invalid native silence-poison timing");
        }
        if (!this.acceptsPoison(l, PoisonKind.SILENCE) || this.poisonActive(l) || this.poisonSilence) {
            return false;
        }
        this.poisonSilence = true;
        return true;
    }

    synchronized boolean curePoisonSilence() {
        boolean bl = this.poisonSilence;
        this.poisonSilence = false;
        return bl;
    }

    synchronized boolean sleepActive(long l) {
        return this.sleepExpiresAtNanos != 0L && l < this.sleepExpiresAtNanos;
    }

    synchronized boolean activateSleep(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native sleep duration");
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        NativeAcceptedState.sleepChanged(this);
        this.sleepExpiresAtNanos = l2;
        return true;
    }

    synchronized boolean expireSleep(long l) {
        if (this.sleepExpiresAtNanos == 0L || l < this.sleepExpiresAtNanos) {
            return false;
        }
        NativeAcceptedState.sleepChanged(this);
        this.sleepExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean cancelSleep() {
        if (this.sleepExpiresAtNanos == 0L) {
            return false;
        }
        NativeAcceptedState.sleepChanged(this);
        this.sleepExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean freezeActive(long l) {
        return this.freezeExpiresAtNanos != 0L && l < this.freezeExpiresAtNanos;
    }

    synchronized boolean activateFreeze(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native freeze duration");
        }
        if (this.freezeActive(l)) {
            return false;
        }
        this.freezeExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        return true;
    }

    synchronized boolean expireFreeze(long l) {
        if (this.freezeExpiresAtNanos == 0L || l < this.freezeExpiresAtNanos) {
            return false;
        }
        this.freezeExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean cancelFreeze() {
        if (this.freezeExpiresAtNanos == 0L) {
            return false;
        }
        this.freezeExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean earthBindActive(long l) {
        return this.earthBindExpiresAtNanos != 0L && l < this.earthBindExpiresAtNanos;
    }

    synchronized boolean activateEarthBind(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Earth Bind duration");
        }
        if (this.earthBindActive(l)) {
            return false;
        }
        this.earthBindExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        return true;
    }

    synchronized boolean expireEarthBind(long l) {
        if (this.earthBindExpiresAtNanos == 0L || l < this.earthBindExpiresAtNanos) {
            return false;
        }
        this.earthBindExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean cancelEarthBind() {
        if (this.earthBindExpiresAtNanos == 0L) {
            return false;
        }
        this.earthBindExpiresAtNanos = 0L;
        return true;
    }

    synchronized void activateAbsoluteBarrier(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Absolute Barrier duration");
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleTimedStatus battleTimedStatus = this;
        this.absoluteBarrierExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean absoluteBarrierActive(long l) {
        return this.absoluteBarrierExpiresAtNanos != 0L && l < this.absoluteBarrierExpiresAtNanos;
    }

    synchronized boolean expireAbsoluteBarrier(long l) {
        if (this.absoluteBarrierExpiresAtNanos == 0L || l < this.absoluteBarrierExpiresAtNanos) {
            return false;
        }
        long l2 = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.absoluteBarrierExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean cancelAbsoluteBarrier() {
        if (this.absoluteBarrierExpiresAtNanos == 0L) {
            return false;
        }
        long l = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.absoluteBarrierExpiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean activateAdvanceSpirit(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Advance Spirit duration");
        }
        boolean bl = this.advanceSpiritExpiresAtNanos == 0L || l >= this.advanceSpiritExpiresAtNanos;
        this.advanceSpiritExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        return bl;
    }

    synchronized boolean advanceSpiritActive(long l) {
        return this.advanceSpiritExpiresAtNanos != 0L && l < this.advanceSpiritExpiresAtNanos;
    }

    synchronized boolean expireAdvanceSpirit(long l) {
        if (this.advanceSpiritExpiresAtNanos == 0L || l < this.advanceSpiritExpiresAtNanos) {
            return false;
        }
        this.advanceSpiritExpiresAtNanos = 0L;
        return true;
    }

    synchronized void activateBurningSpirit(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Burning Spirit duration");
        }
        this.burningSpiritExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
    }

    synchronized boolean burningSpiritActive(long l) {
        return this.burningSpiritExpiresAtNanos != 0L && l < this.burningSpiritExpiresAtNanos;
    }

    synchronized boolean expireBurningSpirit(long l) {
        if (this.burningSpiritExpiresAtNanos == 0L || l < this.burningSpiritExpiresAtNanos) {
            return false;
        }
        this.burningSpiritExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean activateElementalFire(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Elemental Fire duration");
        }
        boolean bl = this.elementalFireExpiresAtNanos != 0L && l < this.elementalFireExpiresAtNanos;
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleTimedStatus battleTimedStatus = this;
        this.elementalFireExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return bl;
    }

    synchronized boolean elementalFireActive(long l) {
        return this.elementalFireExpiresAtNanos != 0L && l < this.elementalFireExpiresAtNanos;
    }

    synchronized boolean expireElementalFire(long l) {
        if (this.elementalFireExpiresAtNanos == 0L || l < this.elementalFireExpiresAtNanos) {
            return false;
        }
        long l2 = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.elementalFireExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean activateSoulOfFlame(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Soul of Flame duration");
        }
        boolean bl = this.soulOfFlameExpiresAtNanos != 0L && l < this.soulOfFlameExpiresAtNanos;
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleTimedStatus battleTimedStatus = this;
        this.soulOfFlameExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return bl;
    }

    synchronized boolean soulOfFlameActive(long l) {
        return this.soulOfFlameExpiresAtNanos != 0L && l < this.soulOfFlameExpiresAtNanos;
    }

    synchronized boolean expireSoulOfFlame(long l) {
        if (this.soulOfFlameExpiresAtNanos == 0L || l < this.soulOfFlameExpiresAtNanos) {
            return false;
        }
        long l2 = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.soulOfFlameExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized void activateVenomResist(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Venom Resist duration");
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleTimedStatus battleTimedStatus = this;
        this.venomResistExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
    }

    synchronized boolean venomResistActive(long l) {
        return this.venomResistExpiresAtNanos != 0L && l < this.venomResistExpiresAtNanos;
    }

    synchronized boolean acceptsPoison(long l, PoisonKind poisonKind) {
        if (poisonKind == null) {
            throw new IllegalArgumentException("missing poison kind");
        }
        return !this.venomResistActive(l);
    }

    synchronized boolean expireVenomResist(long l) {
        if (this.venomResistExpiresAtNanos == 0L || l < this.venomResistExpiresAtNanos) {
            return false;
        }
        long l2 = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.venomResistExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized void activateDoubleBrake(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Double Brake duration");
        }
        this.doubleBrakeExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
    }

    synchronized boolean doubleBrakeActive(long l) {
        return this.doubleBrakeExpiresAtNanos != 0L && l < this.doubleBrakeExpiresAtNanos;
    }

    synchronized boolean expireDoubleBrake(long l) {
        if (this.doubleBrakeExpiresAtNanos == 0L || l < this.doubleBrakeExpiresAtNanos) {
            return false;
        }
        this.doubleBrakeExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean freezeVisualLockActive(long l) {
        return this.freezeActive(l) || this.earthBindActive(l);
    }

    synchronized boolean actionLocked(long l) {
        return this.sleepActive(l) || this.freezeVisualLockActive(l) || this.paralysisActive(l) || this.shockStunActive(l) || this.boneBreakActive(l) || this.thunderGrabActive(l);
    }

    synchronized ShockStunActivation activateShockStun(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n2 <= 0 || n > n2 || n2 > 65535) {
            throw new IllegalArgumentException("invalid native Shock Stun duration");
        }
        boolean bl = this.shockStunActive(l);
        int n3 = 0;
        if (bl) {
            long l2 = this.shockStunExpiresAtNanos - l;
            long l3 = (l2 + TimeUnit.SECONDS.toNanos(1L) - 1L) / TimeUnit.SECONDS.toNanos(1L);
            n3 = (int)Math.min((long)n2, Math.max(1L, l3));
        }
        int n4 = Math.min(n2, Math.addExact(n3, n));
        this.shockStunExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n4));
        return new ShockStunActivation(n4, bl);
    }

    synchronized boolean shockStunActive(long l) {
        return this.shockStunExpiresAtNanos != 0L && l < this.shockStunExpiresAtNanos;
    }

    synchronized boolean expireShockStun(long l) {
        if (this.shockStunExpiresAtNanos == 0L || l < this.shockStunExpiresAtNanos) {
            return false;
        }
        this.shockStunExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean cancelShockStun() {
        if (this.shockStunExpiresAtNanos == 0L) {
            return false;
        }
        this.shockStunExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean activateBoneBreak(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Bone Break duration");
        }
        if (this.boneBreakActive(l)) {
            return false;
        }
        this.boneBreakExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        return true;
    }

    synchronized boolean boneBreakActive(long l) {
        return this.boneBreakExpiresAtNanos != 0L && l < this.boneBreakExpiresAtNanos;
    }

    synchronized boolean expireBoneBreak(long l) {
        if (this.boneBreakExpiresAtNanos == 0L || l < this.boneBreakExpiresAtNanos) {
            return false;
        }
        this.boneBreakExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean cancelBoneBreak() {
        if (this.boneBreakExpiresAtNanos == 0L) {
            return false;
        }
        this.boneBreakExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean activateThunderGrab(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Thunder Grab duration");
        }
        if (this.thunderGrabActive(l)) {
            return false;
        }
        this.thunderGrabExpiresAtNanos = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        return true;
    }

    synchronized boolean thunderGrabActive(long l) {
        return this.thunderGrabExpiresAtNanos != 0L && l < this.thunderGrabExpiresAtNanos;
    }

    synchronized boolean expireThunderGrab(long l) {
        if (this.thunderGrabExpiresAtNanos == 0L || l < this.thunderGrabExpiresAtNanos) {
            return false;
        }
        this.thunderGrabExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean cancelThunderGrab() {
        if (this.thunderGrabExpiresAtNanos == 0L) {
            return false;
        }
        this.thunderGrabExpiresAtNanos = 0L;
        return true;
    }

    synchronized boolean activateHorrorOfDeath(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Horror of Death duration");
        }
        boolean bl = this.horrorOfDeathActive(l);
        if (bl) {
            return true;
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleTimedStatus battleTimedStatus = this;
        this.horrorOfDeathExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return false;
    }

    synchronized boolean horrorOfDeathActive(long l) {
        return this.horrorOfDeathExpiresAtNanos != 0L && l < this.horrorOfDeathExpiresAtNanos;
    }

    synchronized boolean expireHorrorOfDeath(long l) {
        if (this.horrorOfDeathExpiresAtNanos == 0L || l < this.horrorOfDeathExpiresAtNanos) {
            return false;
        }
        long l2 = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.horrorOfDeathExpiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean cancelHorrorOfDeath() {
        if (this.horrorOfDeathExpiresAtNanos == 0L) {
            return false;
        }
        long l = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.horrorOfDeathExpiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleTimedStatus);
        return true;
    }

    synchronized boolean activateParalysis(long l, int n, int n2) {
        long l2;
        if (l < 0L || n < 0 || n2 <= 0) {
            throw new IllegalArgumentException("invalid native curse-paralysis timing");
        }
        if (this.paralysisActivatesAtNanos != 0L && l < this.paralysisExpiresAtNanos) {
            return false;
        }
        this.paralysisActivatesAtNanos = l2 = Math.addExact(l, TimeUnit.MILLISECONDS.toNanos(n));
        this.paralysisExpiresAtNanos = Math.addExact(l2, TimeUnit.MILLISECONDS.toNanos(n2));
        this.paralysisLockEmitted = false;
        return true;
    }

    synchronized boolean paralysisVisualActive(long l) {
        return CombatPoisonParalysis.otherVisual(this.paralysisActivatesAtNanos != 0L && l < this.paralysisExpiresAtNanos, this, l);
    }

    synchronized boolean paralysisActive(long l) {
        return CombatPoisonParalysis.otherActive(this.paralysisActivatesAtNanos != 0L && l >= this.paralysisActivatesAtNanos && l < this.paralysisExpiresAtNanos, this, l);
    }

    synchronized ParalysisPoll pollParalysis(long l) {
        if (this.paralysisActivatesAtNanos == 0L) {
            return new ParalysisPoll(false, false, false);
        }
        if (l >= this.paralysisExpiresAtNanos) {
            boolean bl = this.paralysisLockEmitted;
            this.clearParalysisFields();
            return new ParalysisPoll(false, true, bl);
        }
        if (l >= this.paralysisActivatesAtNanos && !this.paralysisLockEmitted) {
            this.paralysisLockEmitted = true;
            return new ParalysisPoll(true, false, false);
        }
        return new ParalysisPoll(false, false, false);
    }

    synchronized boolean noteParalysisLockSent(long l) {
        if (!this.paralysisActive(l)) {
            return false;
        }
        this.paralysisLockEmitted = true;
        return true;
    }

    synchronized ParalysisCancellation cancelParalysis() {
        if (this.paralysisActivatesAtNanos == 0L) {
            return new ParalysisCancellation(false, false);
        }
        boolean bl = this.paralysisLockEmitted;
        this.clearParalysisFields();
        return new ParalysisCancellation(true, bl);
    }

    synchronized CurseRemoval removeCurse() {
        boolean bl = this.curePoison() | this.curePoisonSilence();
        return new CurseRemoval(bl, this.cancelParalysis());
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
        long l = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.braveExpiresAtNanos = l;
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
        NativeAcceptedState.sleepChanged(this);
        this.sleepExpiresAtNanos = 0L;
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
        this.paralysisActivatesAtNanos = 0L;
        this.paralysisExpiresAtNanos = 0L;
        this.paralysisLockEmitted = false;
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
        long l = 0L;
        BattleTimedStatus battleTimedStatus = this;
        this.braveExpiresAtNanos = l;
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
        NativeAcceptedState.sleepChanged(this);
        this.sleepExpiresAtNanos = 0L;
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
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleTimedStatus battleTimedStatus = this;
        this.decayPotionExpiresAtNanos = l2;
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
