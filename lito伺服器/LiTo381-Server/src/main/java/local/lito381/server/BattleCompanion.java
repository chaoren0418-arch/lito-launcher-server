// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

import java.io.IOException;

import java.util.List;
import java.util.Locale;

/**
 * Small server-authoritative controlled actor used by the native anicom controller.
 *
 * <p>Every object has a unique world id and belongs to exactly one
 * {@link ClientSession}.  Arena-only companions remain battle-scoped, while
 * source-backed summons may enter classic WORLD maps whose retained
 * {@code mapids} row permits pets.  It is never a player or an ArenaBot alias,
 * so C_DIALOG/C_HACTION cannot be applied to a combatant merely because a
 * client supplied that object id.</p>
 */
final class BattleCompanion {
    private static final long BLESSED_GREATER_HASTE_POTION = 140018;
    private static final long GREATER_HASTE_POTION = 40018;
    private static final long BLESSED_HASTE_POTION = 140013;

    /** Retained skill44 result for one living pet/summon actor. */
    record MagicCancellation(Snapshot target, boolean haste, boolean slow,
                             boolean sleep, boolean freeze, boolean earthBind,
                             boolean thunderGrab,
                             boolean horrorOfDeath, boolean boneBreak,
                             boolean poison, boolean paralysis,
                             boolean eraseMagic, boolean panic) {
        MagicCancellation {
            if (target == null || !target.active() || target.hp() <= 0) {
                throw new IllegalArgumentException(
                        "invalid companion magic cancellation");
            }
        }
    }

    /** Exact retained L1Poison installation result for a non-player target. */
    record NpcPoisonInstallation(
            ClassicWorldMonsterPoisonAttack.Kind kind, Snapshot target,
            long attackerObjectId, boolean nativeDamageTarget,
            boolean existingPoison, boolean applied) {
        NpcPoisonInstallation {
            if (kind == null || target == null || attackerObjectId <= 0) {
                throw new IllegalArgumentException("invalid companion poison installation");
            }
        }
    }

    /** Timer transition emitted by damage/silence/paralysis poison leases. */
    record NpcPoisonPoll(
            ClassicWorldMonsterPoisonAttack.Kind kind, Snapshot target,
            long attackerObjectId, int damageTicks,
            boolean activated, boolean expired) {
        NpcPoisonPoll {
            if (kind == null || target == null || attackerObjectId < 0
                    || damageTicks < 0 || activated && expired) {
                throw new IllegalArgumentException("invalid companion poison poll");
            }
        }
    }

    /** Retained curePoison/cureParalaysis result for a controlled NPC. */
    record NpcPoisonCancellation(
            ClassicWorldMonsterPoisonAttack.Kind kind,
            boolean poison, boolean paralysis) {
        NpcPoisonCancellation {
            if (kind == null) {
                throw new IllegalArgumentException("invalid companion poison cancellation");
            }
        }
    }

    /** Retained L1CurseParalysis installation for an NPC-family target. */
    record NpcCurseParalysisInstallation(
            Snapshot target, boolean nativeMonsterTarget,
            boolean existingParalysis, boolean applied) {
        NpcCurseParalysisInstallation {
            if (target == null) {
                throw new IllegalArgumentException(
                        "invalid companion curse-paralysis installation");
            }
        }
    }

    /** Independent L1Paralysis timer transition; this is not an L1Poison. */
    record NpcCurseParalysisPoll(
            Snapshot target, boolean activated, boolean expired) {
        NpcCurseParalysisPoll {
            if (target == null || activated && expired) {
                throw new IllegalArgumentException(
                        "invalid companion curse-paralysis poll");
            }
        }
    }

    static final int NPC_ID = 45034;
    static final int GFX = 936; // 3.80/3.81 npc template 45034, shepherd dog
    static final int LEVEL = 5;
    static final int MAX_HP = 30;
    static final int MAX_MP = 5;
    static final int ARMOR_CLASS = 9;
    static final int STRENGTH = 15;
    static final int DEXTERITY = 15;
    static final int MOVE_INTERVAL_MILLIS = 560;
    static final int ATTACK_INTERVAL_MILLIS = 960;
    static final int PATH_FAIL_GIVE_UP = 20;
    static final int CROWD_RETRY_MILLIS = 600;
    static final int TRANSIENT_PATH_RETRY_MILLIS = 200;
    static final String DEFAULT_NAME = "Arena Companion";

    enum Kind {
        ARENA(false, false),
        TAMED(true, false),
        SUMMON(true, false),
        PET(true, true);

        private final boolean classicWorldEligible;
        private final boolean earnsExperience;

        Kind(boolean classicWorldEligible, boolean earnsExperience) {
            this.classicWorldEligible = classicWorldEligible;
            this.earnsExperience = earnsExperience;
        }

        boolean classicWorldEligible() {
            return classicWorldEligible;
        }

        boolean earnsExperience() {
            return earnsExperience;
        }
    }


    synchronized int nativeSourceControlledHealingHp(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("invalid companion healing amount");
        }
        if (!this.active || this.hp <= 0 || amount == 0) {
            return this.hp;
        }
        this.hp = Math.min(this.profile.maxHp(), NativeSourceControlledHealing.sum(this.hp, amount));
        return this.hp;
    }
    record Profile(int npcId, int gfx, int level, int maxHp, int maxMp,
                   int armorClass, int strength, int dexterity,
                   int moveIntervalMillis, int attackIntervalMillis,
                   int moveSpeed, int petCost, String initialName) {
        Profile {
            if (npcId <= 0 || gfx <= 0 || gfx > 0xFFFF
                    || level <= 0 || level > 0xFF || maxHp <= 0 || maxMp < 0
                    || strength <= 0 || dexterity <= 0
                    || moveIntervalMillis <= 0 || attackIntervalMillis <= 0
                    || moveSpeed < BattleTimedStatus.NORMAL
                    || moveSpeed > BattleTimedStatus.SLOW
                    || petCost <= 0 || initialName == null || initialName.isBlank()) {
                throw new IllegalArgumentException("invalid companion profile");
            }
        }

        static Profile arenaDefault() {
            return new Profile(NPC_ID, GFX, LEVEL, MAX_HP, MAX_MP,
                    ARMOR_CLASS, STRENGTH, DEXTERITY,
                    MOVE_INTERVAL_MILLIS, ATTACK_INTERVAL_MILLIS,
                    BattleTimedStatus.NORMAL, 6, DEFAULT_NAME);
        }
    }

    record Movement(int sourceX, int sourceY, int destinationX, int destinationY,
                    int heading) {}

    record Strike(int heading) {}

    record Damage(Snapshot before, Snapshot after, int appliedDamage,
                  boolean diedNow) {
        Damage {
            if (before == null || after == null || appliedDamage < 0
                    || before.objectId() != after.objectId()
                    || diedNow != (before.active() && before.hp() > 0
                        && (!after.active() || after.hp() == 0))) {
                throw new IllegalArgumentException("invalid companion damage transition");
            }
        }
    }

    enum GivenItemUseKind {
        NONE,
        HEAL,
        HASTE
    }

    /** Retained pet/summon C_GIVE -> onGetItem synchronous result. */
    record GivenItemReceipt(
            Snapshot beforeUse, Snapshot afterUse,
            long destinationObjectId, long receivedCatalogId, int quantity,
            GivenItemUseKind useKind, long consumedCatalogId,
            int effectGfx, int durationSeconds, int appliedHp,
            int lightRange) {
        GivenItemReceipt {
            if (beforeUse == null || afterUse == null
                    || beforeUse.objectId() != afterUse.objectId()
                    || destinationObjectId <= 0
                    || destinationObjectId > 0xFFFF_FFFFL
                    || receivedCatalogId <= 0 || quantity <= 0
                    || useKind == null || consumedCatalogId < 0
                    || effectGfx < 0 || effectGfx > 0xFFFF
                    || durationSeconds < 0
                    || lightRange < 0 || lightRange > 0xFF
                    || (useKind == GivenItemUseKind.NONE)
                        != (consumedCatalogId == 0 && effectGfx == 0
                            && durationSeconds == 0 && appliedHp == 0)
                    || useKind == GivenItemUseKind.HEAL && durationSeconds != 0
                    || useKind == GivenItemUseKind.HASTE && appliedHp != 0) {
                throw new IllegalArgumentException(
                        "invalid companion given-item receipt");
            }
        }
    }

    /** Atomic strict-greater MP debit used by retained skill207. */
    record ManaDebit(Snapshot before, Snapshot after,
                     int requested, boolean debited) {
        ManaDebit {
            if (before == null || after == null || requested <= 0
                    || before.objectId() != after.objectId()
                    || debited != (before.mp() > requested
                        && before.mp() - after.mp() == requested)
                    || !debited && before.mp() != after.mp()) {
                throw new IllegalArgumentException(
                        "invalid companion strict MP debit");
            }
        }
    }

    enum Posture {
        AGGRESSIVE("$469"),
        DEFENSIVE("$470"),
        STAY("$471"),
        EXTEND("$471"),
        ALERT("$472");

        private final String menuStatus;

        Posture(String menuStatus) {
            this.menuStatus = menuStatus;
        }

        String menuStatus() {
            return menuStatus;
        }
    }

    enum CommandResult {
        POSTURE_CHANGED,
        DISMISSED,
        TARGET_SELECTION_PENDING,
        COLLECTION_REQUESTED,
        RENAME_PENDING,
        REJECTED
    }

    @AsRecord(components = {"objectId", "npcId", "gfx", "name", "kind", "active", "posture", "targetObjectId", "x", "y", "heading", "nextActionNanos", "hp", "maxHp", "mp", "maxMp", "level", "armorClass", "strength", "dexterity", "moveSpeed", "petCost", "expiresAtNanos", "collarObjectId", "collarCatalogId", "experience", "experiencePercentage", "lawful"}, finalFields = {"nativeSourceNpcFactoryMemory", "nativeSourceCompanionFirstActor"})
    static final class Snapshot {
        private final long objectId;
        private final int npcId;
        private final int gfx;
        private final String name;
        private final Kind kind;
        private final boolean active;
        private final Posture posture;
        private final long targetObjectId;
        private final int x;
        private final int y;
        private final int heading;
        private final long nextActionNanos;
        private final int hp;
        private final int maxHp;
        private final int mp;
        private final int maxMp;
        private final int level;
        private final int armorClass;
        private final int strength;
        private final int dexterity;
        private final int moveSpeed;
        private final int petCost;
        private final long expiresAtNanos;
        private final long collarObjectId;
        private final int collarCatalogId;
        private final long experience;
        private final int experiencePercentage;
        private final int lawful;
        transient Object nativeSourceNpcFactoryMemory;
        transient Object nativeSourceCompanionFirstActor;

        Snapshot(long l, int n, int n2, String string, Kind kind, boolean bl, Posture posture, long l2, int n3, int n4, int n5, long l3, int n6, int n7, int n8, int n9, int n10, int n11, int n12, int n13, int n14, int n15, long l4, long l5, int n16, long l6, int n17, int n18) {
            this.objectId = l;
            this.npcId = n;
            this.gfx = n2;
            this.name = string;
            this.kind = kind;
            this.active = bl;
            this.posture = posture;
            this.targetObjectId = l2;
            this.x = n3;
            this.y = n4;
            this.heading = n5;
            this.nextActionNanos = l3;
            this.hp = n6;
            this.maxHp = n7;
            this.mp = n8;
            this.maxMp = n9;
            this.level = n10;
            this.armorClass = n11;
            this.strength = n12;
            this.dexterity = n13;
            this.moveSpeed = n14;
            this.petCost = n15;
            this.expiresAtNanos = l4;
            this.collarObjectId = l5;
            this.collarCatalogId = n16;
            this.experience = l6;
            this.experiencePercentage = n17;
            this.lawful = n18;
        }

        List<String> anicomArguments() {
            // Same-version classic S_PetMenuPacket contract: status, HP,
            // max HP, MP, max MP, level, name, carrying label, EXP%, lawful.
            return List.of(
                    posture.menuStatus(),
                    Integer.toString(hp),
                    Integer.toString(maxHp),
                    Integer.toString(mp),
                    Integer.toString(maxMp),
                    Integer.toString(level),
                    name,
                    "$611",
                    Integer.toString(experiencePercentage),
                    Integer.toString(lawful));
        }




        public long objectId() {
            return this.objectId;
        }

        public int npcId() {
            return this.npcId;
        }

        public int gfx() {
            return this.gfx;
        }

        public String name() {
            return this.name;
        }

        public Kind kind() {
            return this.kind;
        }

        public boolean active() {
            return this.active;
        }

        public Posture posture() {
            return this.posture;
        }

        public long targetObjectId() {
            return this.targetObjectId;
        }

        public int x() {
            return this.x;
        }

        public int y() {
            return this.y;
        }

        public int heading() {
            return this.heading;
        }

        public long nextActionNanos() {
            return this.nextActionNanos;
        }

        public int hp() {
            return this.hp;
        }

        public int maxHp() {
            return this.maxHp;
        }

        public int mp() {
            return this.mp;
        }

        public int maxMp() {
            return this.maxMp;
        }

        public int level() {
            return this.level;
        }

        public int armorClass() {
            return this.armorClass;
        }

        public int strength() {
            return this.strength;
        }

        public int dexterity() {
            return this.dexterity;
        }

        public int moveSpeed() {
            return this.moveSpeed;
        }

        public int petCost() {
            return this.petCost;
        }

        public long expiresAtNanos() {
            return this.expiresAtNanos;
        }

        public long collarObjectId() {
            return this.collarObjectId;
        }

        public int collarCatalogId() {
            return this.collarCatalogId;
        }

        public long experience() {
            return this.experience;
        }

        public int experiencePercentage() {
            return this.experiencePercentage;
        }

        public int lawful() {
            return this.lawful;
        }

        Snapshot(long l, int n, int n2, String string, Kind kind, boolean bl, Posture posture, long l2, int n3, int n4, int n5, long l3, int n6, int n7, int n8, int n9, int n10, int n11, int n12, int n13, int n14, int n15, long l4, long l5, int n16, long l6, int n17, int n18, Object object) {
            this.objectId = l;
            this.npcId = n;
            this.gfx = n2;
            this.name = string;
            this.kind = kind;
            this.active = bl;
            this.posture = posture;
            this.targetObjectId = l2;
            this.x = n3;
            this.y = n4;
            this.heading = n5;
            this.nextActionNanos = l3;
            this.hp = n6;
            this.maxHp = n7;
            this.mp = n8;
            this.maxMp = n9;
            this.level = n10;
            this.armorClass = n11;
            this.strength = n12;
            this.dexterity = n13;
            this.moveSpeed = n14;
            this.petCost = n15;
            this.expiresAtNanos = l4;
            this.collarObjectId = l5;
            this.collarCatalogId = n16;
            this.experience = l6;
            this.experiencePercentage = n17;
            this.lawful = n18;
            this.nativeSourceNpcFactoryMemory = object;
        }

        Snapshot(long l, int n, int n2, String string, Kind kind, boolean bl, Posture posture, long l2, int n3, int n4, int n5, long l3, int n6, int n7, int n8, int n9, int n10, int n11, int n12, int n13, int n14, int n15, long l4, long l5, int n16, long l6, int n17, int n18, Object object, Object object2) {
            this(l, n, n2, string, kind, bl, posture, l2, n3, n4, n5, l3, n6, n7, n8, n9, n10, n11, n12, n13, n14, n15, l4, l5, n16, l6, n17, n18, object);
            this.nativeSourceCompanionFirstActor = object2;
        }
    }

    private final long objectId;
    private final Kind kind;
    private volatile Profile profile;
    private String name;
    private volatile boolean active;
    private Posture posture = Posture.STAY;
    private long targetObjectId;
    private int pathFailCount;
    private volatile int x;
    private volatile int y;
    private int heading;
    private volatile int hp;
    private int mp;
    private long nextActionNanos;
    private long expiresAtNanos;
    /**
     * Runtime Haste is distinct from a source profile that is already born
     * hasted (summons and persistent pets).  Keeping the lease here lets the
     * native TARGET_TO_PC/TARGET_TO_NPC spell path treat L1Pet/L1Summon as the
     * retained L1Character family without scaling an already-hasted template
     * twice.
     */
    private int moveSpeed;
    private volatile long hasteExpiresAtNanos;
    private int hasteSkillId;
    private volatile long slowExpiresAtNanos;
    private int slowSkillId;
    private long collarObjectId;
    private int collarCatalogId;
    private String petSourceKey = "";
    private long experience;
    private int lawful;
    /** Source NPC combat fields retained by L1AttackNpc/L1NpcInstance. */
    private int magicResistance;
    private boolean boss;
    private int attackRange;
    private int bowActionId;
    private int poisonAttack;
    private int paralysisAttack;
    /** Immutable NPC-template level used by retained L1PetInstance damage dice. */
    private int sourceTemplateLevel;
    private int undead;
    /** Retained L1NpcInstance Weapon Break stack count (0..10). */
    private int weaponBreakStacks;
    private boolean npcDefenseQualified;
    private BattleQuestEncounter.Snapshot questTamedOrigin;
    private BattleTimeAttackEncounter.Snapshot timeAttackTamedOrigin;
    private ClassicWorldMonsterPopulation.Snapshot classicWorldTamedOrigin;
    private int classicWorldTamedInstanceId = -1;
    private volatile RuntimeInventory carriedItems = new RuntimeInventory();
    /** Retained L1Character status also applies to L1Pet/L1Summon targets. */
    private final BattleEraseMagicStatus eraseMagicStatus =
            new BattleEraseMagicStatus();
    /** Freeze-family action lock shared by retained L1Pet/L1Summon targets. */
    private final BattleTimedStatus timedStatus = new BattleTimedStatus();
    /** L1Character stores curse paralysis separately from its L1Poison slot. */
    private final BattleTimedStatus curseParalysisStatus =
            new BattleTimedStatus();
    /** L1Character owns exactly one installed poison object at a time. */
    private ClassicWorldMonsterPoisonAttack.Kind npcPoisonKind =
            ClassicWorldMonsterPoisonAttack.Kind.NONE;
    private long npcPoisonAttackerObjectId;
    private final BattleArmBreakerStatus armBreakerStatus =
            new BattleArmBreakerStatus();
    private final BattlePanicStatus panicStatus = new BattlePanicStatus();
    private final BattleJoyOfPainStatus joyOfPainStatus =
            new BattleJoyOfPainStatus();

    private Object nativeTamedCapturePopulation;
    private volatile NativeCompanionCallbacks.Stamp nativeSummonTerminalStamp;
    private volatile Object nativeSummonGroundFallback;
    private volatile Object nativeWorldCompanionPlacement;
    private BattleWindShackleStatus nativeCompanionWindStatus;
    volatile Object nativeSourceElementalLease;
    volatile Object nativeSourceDollSlowEpoch;
    final Object nativeSourceNpcFactoryType;
    /** T135 (#386): actor-owned Source mobskill use counts and reuse delays; see NativeCloseoutCompanionSkills. */
    volatile Object nativeCloseoutMobSkillState;

    BattleCompanion(long objectId) {
        this(objectId, Profile.arenaDefault(), Kind.ARENA);
    }

    private BattleCompanion(long objectId, Profile profile, Kind kind) {
        if (objectId <= 0 || objectId > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("invalid companion object id");
        }
        if (kind == null) throw new IllegalArgumentException("missing companion kind");
        this.objectId = objectId;
        this.kind = kind;
        this.profile = profile;
        name = profile.initialName();
        hp = profile.maxHp();
        mp = profile.maxMp();
        moveSpeed = profile.moveSpeed();
        sourceTemplateLevel = profile.level();
        this.nativeSourceNpcFactoryType = NativeSourceNpcFactoryType.birth(this, null);
        NativeCloseoutCompanionSkills.install(this);
    }

    static BattleCompanion tamed(long objectId, int npcId, int gfx, String name,
                                 int level, int currentHp, int maxHp, int maxMp,
                                 int armorClass, int strength, int dexterity,
                                 int spawnX, int spawnY) {
        BattleAnimationCatalog animations = BattleAnimationCatalog.get();
        int moveMillis = Math.toIntExact(Math.max(40L, animations.requireTimedAction(
                gfx, BattleActionCodes.WALK).durationMillis()));
        int attackMillis = Math.toIntExact(Math.max(40L, animations.requireTimedAction(
                gfx, BattleActionCodes.ATTACK).durationMillis()));
        Profile profile = new Profile(npcId, gfx, level, maxHp, maxMp,
                armorClass, strength, dexterity, moveMillis, attackMillis,
                BattleTimedStatus.NORMAL, 6, name);
        BattleCompanion result = new BattleCompanion(objectId, profile, Kind.TAMED);
        result.activate(spawnX, spawnY, currentHp, maxMp);
        return result;
    }

    static BattleCompanion tamedFromQuest(long objectId, BattleQuestEncounter.Snapshot captured, SourceNpcPhysicalStats stats) {
        if (captured == null || stats == null || captured.npcId() != stats.npcId()) {
            throw new IllegalArgumentException("invalid quest tamed source");
        }
        BattleCompanion battleCompanion = BattleCompanion.tamed(objectId, captured.npcId(), captured.gfx(), captured.name(), captured.level(), NativeBattleTamedLifecycle.initialHp(captured), captured.maxHp(), captured.maximumMp(), stats.armorClass(), Math.max(1, stats.strength()), Math.max(1, stats.dexterity()), captured.x(), captured.y());
        battleCompanion.qualifyNpcDefense(stats);
        battleCompanion.questTamedOrigin = captured;
        return NativeBattleTamedLifecycle.vitals(battleCompanion, captured);
    }

    static BattleCompanion tamedFromTimeAttack(long objectId, BattleTimeAttackEncounter.Snapshot captured, SourceNpcPhysicalStats stats) {
        if (captured == null || stats == null || captured.npcId() != stats.npcId()) {
            throw new IllegalArgumentException("invalid Time Attack tamed source");
        }
        BattleCompanion battleCompanion = BattleCompanion.tamed(objectId, captured.npcId(), captured.gfx(), captured.name(), captured.level(), NativeBattleTamedLifecycle.initialHp(captured), captured.maxHp(), captured.maximumMp(), stats.armorClass(), Math.max(1, stats.strength()), Math.max(1, stats.dexterity()), captured.x(), captured.y());
        battleCompanion.qualifyNpcDefense(stats);
        battleCompanion.timeAttackTamedOrigin = captured;
        return NativeBattleTamedLifecycle.vitals(battleCompanion, captured);
    }

    static BattleCompanion tamedFromClassicWorld(
            long objectId, ClassicWorldMonsterPopulation.Snapshot captured,
            int instanceId, SourceNpcPhysicalStats stats) {
        if (captured == null || stats == null || !captured.alive()
                || !captured.present() || captured.npcId() != stats.npcId()) {
            throw new IllegalArgumentException("invalid classic WORLD tamed source");
        }
        BattleCompanion result = tamed(
                objectId, captured.npcId(), captured.gfxId(),
                captured.nameId(), captured.template().level(),
                captured.currentHp(), captured.maximumHp(), captured.maximumMp(),
                stats.armorClass(), Math.max(1, stats.strength()),
                Math.max(1, stats.dexterity()), captured.x(), captured.y());
        result.qualifyNpcDefense(stats);
        result.classicWorldTamedOrigin = captured;
        result.classicWorldTamedInstanceId = instanceId;
        return result;
    }

    synchronized BattleQuestEncounter.Snapshot questTamedOrigin() {
        return questTamedOrigin;
    }

    synchronized BattleTimeAttackEncounter.Snapshot timeAttackTamedOrigin() {
        return timeAttackTamedOrigin;
    }

    synchronized ClassicWorldMonsterPopulation.Snapshot classicWorldTamedOrigin() {
        return classicWorldTamedOrigin;
    }

    synchronized int classicWorldTamedInstanceId() {
        return classicWorldTamedInstanceId;
    }

    static BattleCompanion zombie(long objectId, BattleSpellCatalog.ZombieTemplate template, SourceNpcPhysicalStats stats, int petCost, int spawnX, int spawnY) {
        if (template == null || stats == null || template.npcId() != stats.npcId() || petCost <= 0) {
            throw new IllegalArgumentException("invalid create-zombie companion source");
        }
        BattleAnimationCatalog battleAnimationCatalog = BattleAnimationCatalog.get();
        int n4 = Math.toIntExact(Math.max(40L, battleAnimationCatalog.requireTimedAction(template.gfx(), 0).durationMillis()));
        int n5 = Math.toIntExact(Math.max(40L, battleAnimationCatalog.requireTimedAction(template.gfx(), 1).durationMillis()));
        Profile profile = new Profile(template.npcId(), template.gfx(), template.level(), template.maxHp(), stats.maxMp(), stats.armorClass(), Math.max(1, stats.strength()), Math.max(1, stats.dexterity()), n4, n5, 0, petCost, template.name());
        BattleCompanion battleCompanion = new BattleCompanion(objectId, profile, Kind.TAMED);
        battleCompanion.qualifyNpcDefense(stats);
        battleCompanion.activate(spawnX, spawnY, template.maxHp(), stats.maxMp());
        return battleCompanion;
    }

    static BattleCompanion summoned(long objectId,
                                    BattleSpellCatalog.SummonChoice choice,
                                    SourceSummonTemplate template,
                                    int petCost, int lifetimeSeconds,
                                    int spawnX, int spawnY,
                                    long nowNanos) {
        if (choice == null || template == null || choice.npcId() != template.npcId()
                || petCost <= 0 || lifetimeSeconds <= 0 || nowNanos <= 0) {
            throw new IllegalArgumentException("invalid summoned companion source");
        }
        BattleAnimationCatalog animations = BattleAnimationCatalog.get();
        animations.requireBattleCapableSprite(template.gfx(), BattleActionCodes.WALK);
        int moveMillis = hasteInterval(template.moveIntervalMillis());
        int attackMillis = hasteInterval(template.attackIntervalMillis());
        Profile profile = new Profile(template.npcId(), template.gfx(), template.level(),
                template.maxHp(), template.maxMp(), template.armorClass(),
                template.strength(), template.dexterity(), moveMillis, attackMillis,
                BattleTimedStatus.HASTE, petCost, template.nameId());
        BattleCompanion result = new BattleCompanion(objectId, profile, Kind.SUMMON);
        result.expiresAtNanos = Math.addExact(nowNanos,
                Math.multiplyExact((long) lifetimeSeconds, 1_000_000_000L));
        result.activate(spawnX, spawnY, template.maxHp(), template.maxMp());
        result.posture = Posture.AGGRESSIVE;
        return result;
    }

    static BattleCompanion lesserElemental(
            long objectId, SourceSummonTemplate template,
            int petCost, int lifetimeSeconds,
            int spawnX, int spawnY, long nowNanos) {
        return elemental(objectId, template, petCost, lifetimeSeconds,
                spawnX, spawnY, nowNanos, 45303, 45306, "Lesser");
    }

    static BattleCompanion greaterElemental(
            long objectId, SourceSummonTemplate template,
            int petCost, int lifetimeSeconds,
            int spawnX, int spawnY, long nowNanos) {
        return elemental(objectId, template, petCost, lifetimeSeconds,
                spawnX, spawnY, nowNanos, 81050, 81053, "Greater");
    }

    private static BattleCompanion elemental(
            long objectId, SourceSummonTemplate template,
            int petCost, int lifetimeSeconds,
            int spawnX, int spawnY, long nowNanos,
            int firstNpcId, int lastNpcId, String tier) {
        if (template == null || !SourceSummonTemplate.supportedNpcId(template.npcId())
                || template.npcId() < firstNpcId || template.npcId() > lastNpcId
                || petCost <= 0 || lifetimeSeconds <= 0 || nowNanos <= 0) {
            throw new IllegalArgumentException(
                    "invalid " + tier + " Elemental companion source");
        }
        BattleAnimationCatalog animations = BattleAnimationCatalog.get();
        animations.requireBattleCapableSprite(
                template.gfx(), BattleActionCodes.WALK);
        Profile profile = new Profile(
                template.npcId(), template.gfx(), template.level(),
                template.maxHp(), template.maxMp(), template.armorClass(),
                template.strength(), template.dexterity(),
                hasteInterval(template.moveIntervalMillis()),
                hasteInterval(template.attackIntervalMillis()),
                BattleTimedStatus.HASTE, petCost, template.nameId());
        BattleCompanion result = new BattleCompanion(
                objectId, profile, Kind.SUMMON);
        result.expiresAtNanos = Math.addExact(nowNanos,
                Math.multiplyExact((long) lifetimeSeconds, 1_000_000_000L));
        result.activate(spawnX, spawnY, template.maxHp(), template.maxMp());
        result.posture = Posture.AGGRESSIVE;
        return result;
    }

    static BattleCompanion pet(GameStateStore.PetState state,
                               SourcePetTemplate template,
                               int spawnX, int spawnY) {
        if (state == null || template == null || state.npcId() != template.npcId()
                || state.collarCatalogId() != template.collarCatalogId()) {
            throw new IllegalArgumentException("invalid persistent pet source");
        }
        BattleAnimationCatalog animations = BattleAnimationCatalog.get();
        animations.requireBattleCapableSprite(template.gfx(), BattleActionCodes.WALK);
        Profile profile = new Profile(template.npcId(), template.gfx(), state.level(),
                state.maxHp(), state.maxMp(), template.armorClass(),
                template.strength(), template.dexterity(),
                hasteInterval(template.moveIntervalMillis()),
                hasteInterval(template.attackIntervalMillis()),
                BattleTimedStatus.HASTE, template.charismaCost(), template.name());
        BattleCompanion result = new BattleCompanion(state.objectId(), profile, Kind.PET);
        result.qualifyNpcDefense(
                template.npcId(), template.sourceLevel(), template.undead(),
                template.magicResistance(), template.boss(),
                template.ranged(), template.bowActionId(),
                template.poisonAttack(), template.paralysisAttack());
        result.name = state.name();
        result.collarObjectId = state.collarObjectId();
        result.collarCatalogId = state.collarCatalogId();
        result.petSourceKey = state.sourceKey();
        result.experience = state.experience();
        result.lawful = state.lawful();
        result.activate(spawnX, spawnY, state.maxHp(), state.maxMp());
        result.posture = Posture.STAY;
        return result;
    }

    synchronized void qualifyNpcDefense(SourceNpcPhysicalStats stats) {
        if (stats == null) {
            throw new IllegalArgumentException("missing companion NPC defense source");
        }
        qualifyNpcDefense(stats.npcId(), profile.level(), stats.undead(),
                stats.magicResistance(), stats.boss(),
                stats.ranged(), stats.bowActionId(),
                stats.poisonAttack(), stats.paralysisAttack());
    }

    private synchronized void qualifyNpcDefense(
            int npcId, int sourceLevel, int sourceUndead,
            int sourceMagicResistance, boolean sourceBoss,
            int sourceAttackRange, int sourceBowActionId,
            int sourcePoisonAttack, int sourceParalysisAttack) {
        if (profile.npcId() != npcId || sourceLevel <= 0 || sourceLevel > 0xFF
                || sourceUndead < 0 || sourceMagicResistance < 0
                || sourceAttackRange < 0
                || sourceBowActionId < 0 || sourceBowActionId > 0xFFFF
                || sourcePoisonAttack < 0 || sourceParalysisAttack < 0) {
            throw new IllegalArgumentException("invalid companion NPC defense source");
        }
        magicResistance = sourceMagicResistance;
        boss = sourceBoss;
        attackRange = Math.max(1, sourceAttackRange);
        bowActionId = sourceBowActionId;
        poisonAttack = sourcePoisonAttack;
        paralysisAttack = sourceParalysisAttack;
        sourceTemplateLevel = sourceLevel;
        undead = sourceUndead;
        npcDefenseQualified = true;
    }

    synchronized boolean npcDefenseQualified() {
        return npcDefenseQualified;
    }

    synchronized int magicResistance() {
        if (!npcDefenseQualified) {
            throw new IllegalStateException("companion NPC defense is not qualified");
        }
        return magicResistance;
    }

    synchronized boolean boss() {
        if (!npcDefenseQualified) {
            throw new IllegalStateException("companion NPC defense is not qualified");
        }
        return boss;
    }

    synchronized int attackRange() {
        if (!npcDefenseQualified) {
            throw new IllegalStateException("companion NPC combat is not qualified");
        }
        return attackRange;
    }

    synchronized int bowActionId() {
        if (!npcDefenseQualified) {
            throw new IllegalStateException("companion NPC combat is not qualified");
        }
        return bowActionId;
    }

    synchronized int poisonAttack() {
        if (!npcDefenseQualified) {
            throw new IllegalStateException("companion NPC combat is not qualified");
        }
        return poisonAttack;
    }

    synchronized int paralysisAttack() {
        if (!npcDefenseQualified) {
            throw new IllegalStateException("companion NPC combat is not qualified");
        }
        return paralysisAttack;
    }

    synchronized int sourceTemplateLevel() {
        if (!npcDefenseQualified) {
            throw new IllegalStateException("companion NPC combat is not qualified");
        }
        return sourceTemplateLevel;
    }

    synchronized int undead() {
        if (!npcDefenseQualified) {
            throw new IllegalStateException("companion NPC combat is not qualified");
        }
        return undead;
    }

    synchronized int weaponBreakStacks() {
        return weaponBreakStacks;
    }

    synchronized int addWeaponBreakStack() {
        if (!active || hp <= 0) return -1;
        weaponBreakStacks = BattleWeaponBreak.addStack(weaponBreakStacks);
        return weaponBreakStacks;
    }

    synchronized boolean clearWeaponBreakStacks() {
        boolean changed = weaponBreakStacks != 0;
        weaponBreakStacks = 0;
        return changed;
    }

    synchronized boolean projectileAtDistance(int chebyshevDistance) {
        return chebyshevDistance > 1 && bowActionId() > 0;
    }

    synchronized int projectileAttackAction() {
        return switch (profile.gfx()) {
            case 3860 -> 21;
            case 2716 -> 19;
            default -> BattleActionCodes.ATTACK;
        };
    }

    /** Retained tamePet keeps the living monster vitals and transfers its bag. */
    static BattleCompanion petFromTaming(
            GameStateStore.PetState state, SourcePetTemplate template,
            ClassicWorldMonsterPopulation.Snapshot captured,
            List<RuntimeInventory.Item> capturedItems) {
        if (captured == null || !captured.alive() || !captured.present()
                || captured.npcId() != template.npcId()
                || captured.currentHp() <= 0
                || captured.currentHp() > state.maxHp()
                || captured.currentMp() < 0
                || captured.currentMp() > state.maxMp()
                || capturedItems == null) {
            throw new IllegalArgumentException("invalid persistent tamePet source");
        }
        BattleCompanion result = pet(
                state, template, captured.x(), captured.y());
        result.activate(captured.x(), captured.y(),
                captured.currentHp(), captured.currentMp());
        for (RuntimeInventory.Item item : capturedItems) {
            if (!result.carriedItems.add(item, item.quantity())) {
                throw new IllegalStateException(
                        "captured monster bag did not fit persistent pet");
            }
        }
        return result;
    }

    private static int hasteInterval(int baseIntervalMillis) {
        return Math.max(1, (int) (baseIntervalMillis * 0.755D));
    }

    synchronized void resetForBattle(int spawnX, int spawnY) {
        activate(spawnX, spawnY, profile.maxHp(), profile.maxMp());
    }

    private synchronized void activate(int spawnX, int spawnY, int currentHp, int currentMp) {
        BattleCompanion.requireCoordinate(spawnX, "companion spawn x");
        BattleCompanion.requireCoordinate(spawnY, "companion spawn y");
        if (currentHp <= 0 || currentHp > this.profile.maxHp() || currentMp < 0 || currentMp > this.profile.maxMp()) {
            throw new IllegalArgumentException("invalid companion current vitals");
        }
        BattleCompanion recipient = this;
        boolean updatedValue = true;
        NativeCompanionCallbacks.advance(this);
        recipient.active = updatedValue;
        this.posture = Posture.STAY;
        this.targetObjectId = 0L;
        this.pathFailCount = 0;
        this.x = spawnX;
        this.y = spawnY;
        this.heading = 0;
        this.hp = currentHp;
        this.mp = currentMp;
        this.nextActionNanos = 0L;
        this.clearSpeedEffects();
        this.eraseMagicStatus.clear();
        this.timedStatus.clear();
        NativeCompanionWindState.clear(this);
        this.curseParalysisStatus.clear();
        this.clearNpcPoisonLease();
        this.panicStatus.clear();
        this.joyOfPainStatus.clear();
        this.weaponBreakStacks = 0;
    }

    synchronized void deactivate() {
        BattleCompanion recipient = this;
        boolean updatedValue = false;
        NativeCompanionCallbacks.advance(this);
        recipient.active = updatedValue;
        NativeCloseoutCompanionSkills.terminal(this);
        this.targetObjectId = 0L;
        this.pathFailCount = 0;
        this.nextActionNanos = 0L;
        this.clearSpeedEffects();
        this.eraseMagicStatus.clear();
        this.timedStatus.clear();
        NativeCompanionWindState.clear(this);
        this.curseParalysisStatus.clear();
        this.clearNpcPoisonLease();
        this.panicStatus.clear();
        this.joyOfPainStatus.clear();
        this.weaponBreakStacks = 0;
    }

    synchronized ManaDebit debitManaIfStrictlyAbove(int requested) {
        if (requested <= 0) {
            throw new IllegalArgumentException("invalid companion MP debit");
        }
        Snapshot before = snapshot();
        boolean debited = active && hp > 0 && mp > requested;
        if (debited) mp -= requested;
        return new ManaDebit(before, snapshot(), requested, debited);
    }

    synchronized boolean activateHaste(
            long nowNanos, int durationSeconds, int skillId) {
        if (!active || hp <= 0 || nowNanos < 0 || durationSeconds <= 0
                || skillId != BattleSpellCatalog.HASTE) return false;
        if (slowSkillId != 0 && nowNanos < slowExpiresAtNanos) {
            slowExpiresAtNanos = 0;
            slowSkillId = 0;
            moveSpeed = BattleTimedStatus.NORMAL;
            return true;
        }
        slowExpiresAtNanos = 0;
        slowSkillId = 0;
        moveSpeed = BattleTimedStatus.HASTE;
        hasteExpiresAtNanos = Math.addExact(
                nowNanos, durationSeconds * 1_000_000_000L);
        hasteSkillId = skillId;
        return true;
    }

    synchronized int cancelHaste() {
        if (hasteSkillId == 0) return 0;
        int cancelled = hasteSkillId;
        clearHaste();
        return cancelled;
    }

    synchronized int expireHaste(long nowNanos) {
        if (hasteSkillId == 0 || nowNanos < hasteExpiresAtNanos) return 0;
        int expired = hasteSkillId;
        clearHaste();
        return expired;
    }

    private void clearHaste() {
        hasteExpiresAtNanos = 0;
        hasteSkillId = 0;
        if (slowSkillId == 0) moveSpeed = profile.moveSpeed();
    }

    synchronized BattleTimedStatus.SpeedChange activateSlow(
            long nowNanos, int durationSeconds, int skillId) {
        if (!active || hp <= 0 || nowNanos < 0 || durationSeconds <= 0
                || durationSeconds > 0xFFFF
                || skillId != BattleSpellCatalog.SLOW
                    && skillId != BattleSpellCatalog.MASS_SLOW
                    && skillId != BattleSpellCatalog.ENTANGLE) {
            throw new IllegalArgumentException("invalid companion slow lease");
        }
        if (hasteSkillId != 0 && nowNanos < hasteExpiresAtNanos) {
            int stopped = hasteSkillId;
            hasteExpiresAtNanos = 0;
            hasteSkillId = 0;
            slowExpiresAtNanos = 0;
            slowSkillId = 0;
            moveSpeed = BattleTimedStatus.NORMAL;
            return new BattleTimedStatus.SpeedChange(
                    BattleTimedStatus.NORMAL, 0, stopped);
        }
        hasteExpiresAtNanos = 0;
        hasteSkillId = 0;
        slowExpiresAtNanos = Math.addExact(
                nowNanos, durationSeconds * 1_000_000_000L);
        slowSkillId = skillId;
        moveSpeed = BattleTimedStatus.SLOW;
        return new BattleTimedStatus.SpeedChange(
                BattleTimedStatus.SLOW, durationSeconds, 0);
    }

    synchronized int expireSlow(long nowNanos) {
        if (slowSkillId == 0 || nowNanos < slowExpiresAtNanos) return 0;
        int expired = slowSkillId;
        slowExpiresAtNanos = 0;
        slowSkillId = 0;
        moveSpeed = BattleTimedStatus.NORMAL;
        return expired;
    }

    synchronized int cancelSlow() {
        if (slowSkillId == 0) return 0;
        int cancelled = slowSkillId;
        slowExpiresAtNanos = 0;
        slowSkillId = 0;
        moveSpeed = BattleTimedStatus.NORMAL;
        return cancelled;
    }

    private void clearSpeedEffects() {
        hasteExpiresAtNanos = 0;
        hasteSkillId = 0;
        slowExpiresAtNanos = 0;
        slowSkillId = 0;
        moveSpeed = profile.moveSpeed();
    }

    synchronized boolean activateEraseMagic(long nowNanos, int durationSeconds) {
        if (!active || hp <= 0) return false;
        eraseMagicStatus.activate(nowNanos, durationSeconds);
        return true;
    }

    synchronized int effectiveMagicResistance(
            int baseMagicResistance, long nowNanos) {
        return eraseMagicStatus.effectiveMagicResistance(
                baseMagicResistance, nowNanos);
    }

    synchronized BattleEraseMagicStatus.Lease captureEraseMagic(long nowNanos) {
        return eraseMagicStatus.capture(nowNanos);
    }

    synchronized boolean consumeEraseMagic(BattleEraseMagicStatus.Lease lease) {
        return eraseMagicStatus.consume(lease);
    }

    synchronized boolean consumeEraseMagic(long nowNanos) {
        return eraseMagicStatus.consume(nowNanos);
    }

    synchronized boolean cancelEraseMagic() {
        return eraseMagicStatus.cancel();
    }

    synchronized MagicCancellation cancelMagicEffects() {
        NativeSourceCompanionElemental.cancel(this);
        if (!this.active || this.hp <= 0) {
            return null;
        }
        Snapshot snapshot = this.snapshot();
        NativeCompanionForestCancellation.cancel(this);
        NativeCompanionWindState.cancel(this);
        NativeBlindSourceState.cancel(this);
        boolean bl = this.cancelHaste() != 0;
        boolean bl2 = this.cancelSlow() != 0;
        this.moveSpeed = 0;
        boolean bl3 = this.timedStatus.cancelSleep();
        boolean bl4 = this.timedStatus.cancelFreeze();
        boolean bl5 = this.timedStatus.cancelEarthBind();
        boolean bl6 = this.timedStatus.cancelThunderGrab();
        boolean bl7 = this.timedStatus.cancelHorrorOfDeath();
        boolean bl8 = this.timedStatus.cancelBoneBreak();
        NpcPoisonCancellation npcPoisonCancellation = this.cancelNpcPoison();
        boolean bl9 = this.curseParalysisStatus.cancelParalysis().present();
        boolean bl10 = this.cancelEraseMagic();
        this.armBreakerStatus.cancel();
        boolean bl11 = this.panicStatus.cancel();
        this.joyOfPainStatus.cancel();
        this.clearWeaponBreakStacks();
        return new MagicCancellation(snapshot, bl, bl2, bl3, bl4, bl5, bl6, bl7, bl8, npcPoisonCancellation.poison(), npcPoisonCancellation.paralysis() || bl9, bl10, bl11);
    }

    /**
     * Installs the exact non-player poison branches retained by L1AttackNpc.
     * Damage poison is restricted to L1PcInstance/L1MonsterInstance, so only
     * a captured TAMED monster qualifies; PET and SUMMON are L1NpcInstance
     * siblings and reject kind 1. Silence and paralysis accept all three.
     */
    synchronized NpcPoisonInstallation activateNpcPoison(
            ClassicWorldMonsterPoisonAttack.Kind requested,
            long attackerObjectId, long nowNanos) {
        if (requested == null || attackerObjectId <= 0 || nowNanos < 0) {
            throw new IllegalArgumentException("invalid companion poison request");
        }
        Snapshot target = snapshot();
        boolean nativeDamageTarget = kind == Kind.TAMED;
        boolean existingPoison = timedStatus.poisonActive(nowNanos)
                || timedStatus.poisonSilenceActive()
                || timedStatus.paralysisVisualActive(nowNanos);
        if (!existingPoison && npcPoisonKind !=
                ClassicWorldMonsterPoisonAttack.Kind.NONE) {
            clearNpcPoisonLease();
        }
        boolean applied = false;
        if (active && hp > 0 && requested !=
                ClassicWorldMonsterPoisonAttack.Kind.NONE
                && (requested != ClassicWorldMonsterPoisonAttack.Kind.DAMAGE
                    || nativeDamageTarget)
                && !existingPoison) {
            applied = switch (requested) {
                case DAMAGE -> timedStatus.activatePoison(
                        nowNanos,
                        ClassicWorldMonsterPoisonAttack.DAMAGE_DURATION_SECONDS,
                        ClassicWorldMonsterPoisonAttack.DAMAGE_TICK_MILLIS);
                case SILENCE -> timedStatus.activatePoisonSilence(nowNanos);
                case PARALYSIS -> timedStatus.activateParalysis(
                        nowNanos,
                        ClassicWorldMonsterPoisonAttack.PARALYSIS_WARNING_MILLIS,
                        ClassicWorldMonsterPoisonAttack.PARALYSIS_DURATION_MILLIS);
                case NONE -> false;
            };
        }
        if (applied) {
            npcPoisonKind = requested;
            npcPoisonAttackerObjectId = attackerObjectId;
        }
        return new NpcPoisonInstallation(
                requested, target, attackerObjectId, nativeDamageTarget,
                existingPoison, applied);
    }

    synchronized NpcPoisonPoll pollNpcPoison(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid companion poison poll time");
        }
        ClassicWorldMonsterPoisonAttack.Kind current = npcPoisonKind;
        long attackerObjectId = npcPoisonAttackerObjectId;
        int damageTicks = 0;
        boolean activated = false;
        boolean expired = false;
        switch (current) {
            case DAMAGE -> {
                BattleTimedStatus.PoisonPoll poll = timedStatus.pollPoison(
                        nowNanos,
                        ClassicWorldMonsterPoisonAttack.DAMAGE_TICK_MILLIS);
                damageTicks = poll.damageTicks();
                expired = poll.expired();
            }
            case PARALYSIS -> {
                BattleTimedStatus.ParalysisPoll poll =
                        timedStatus.pollParalysis(nowNanos);
                activated = poll.activated();
                expired = poll.expired();
            }
            case SILENCE, NONE -> {
                // Silence poison intentionally has no timer in retained source.
            }
        }
        NpcPoisonPoll result = new NpcPoisonPoll(
                current, snapshot(), attackerObjectId, damageTicks,
                activated, expired);
        if (expired) clearNpcPoisonLease();
        return result;
    }

    /**
     * Retained skill33 NPC branch: only L1MonsterInstance is accepted, so a
     * captured TAMED actor locks immediately for the active duration. PET and
     * SUMMON are different L1NpcInstance siblings and reject installation.
     */
    synchronized NpcCurseParalysisInstallation activateNpcCurseParalysis(
            long nowNanos, int activeDurationMillis) {
        if (nowNanos < 0 || activeDurationMillis <= 0) {
            throw new IllegalArgumentException(
                    "invalid companion curse-paralysis request");
        }
        Snapshot target = snapshot();
        boolean nativeMonsterTarget = kind == Kind.TAMED;
        boolean existingParalysis = curseParalysisStatus
                .paralysisVisualActive(nowNanos);
        boolean applied = active && hp > 0 && nativeMonsterTarget
                && !timedStatus.freezeActive(nowNanos)
                && !timedStatus.earthBindActive(nowNanos)
                && !existingParalysis
                && curseParalysisStatus.activateParalysis(
                        nowNanos, 0, activeDurationMillis);
        if (applied) {
            BattleTimedStatus.ParalysisPoll immediate =
                    curseParalysisStatus.pollParalysis(nowNanos);
            if (!immediate.activated()) {
                throw new IllegalStateException(
                        "zero-delay companion curse paralysis did not activate");
            }
        }
        return new NpcCurseParalysisInstallation(
                target, nativeMonsterTarget, existingParalysis, applied);
    }

    synchronized NpcCurseParalysisPoll pollNpcCurseParalysis(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException(
                    "invalid companion curse-paralysis poll time");
        }
        BattleTimedStatus.ParalysisPoll poll =
                curseParalysisStatus.pollParalysis(nowNanos);
        return new NpcCurseParalysisPoll(
                snapshot(), poll.activated(), poll.expired());
    }

    synchronized boolean npcCurseParalysisActive(long nowNanos) {
        return curseParalysisStatus.paralysisActive(nowNanos);
    }

    synchronized NpcPoisonCancellation removeCurseNpcPoison(
            boolean cureParalysis) {
        ClassicWorldMonsterPoisonAttack.Kind current = npcPoisonKind;
        boolean poison = timedStatus.curePoison()
                | timedStatus.curePoisonSilence();
        // Poison kind 4 is an L1Poison and is cured by CURE_POISON. Skill33
        // owns the independent L1Paralysis slot and needs REMOVE_CURSE.
        boolean poisonParalysis = current ==
                ClassicWorldMonsterPoisonAttack.Kind.PARALYSIS
                && timedStatus.cancelParalysis().present();
        boolean curseParalysis = cureParalysis
                && curseParalysisStatus.cancelParalysis().present();
        boolean paralysis = poisonParalysis || curseParalysis;
        if (poison || poisonParalysis) clearNpcPoisonLease();
        return new NpcPoisonCancellation(current, poison, paralysis);
    }

    private NpcPoisonCancellation cancelNpcPoison() {
        ClassicWorldMonsterPoisonAttack.Kind current = npcPoisonKind;
        boolean poison = timedStatus.curePoison()
                | timedStatus.curePoisonSilence();
        boolean paralysis = timedStatus.cancelParalysis().present();
        if (poison || paralysis || current !=
                ClassicWorldMonsterPoisonAttack.Kind.NONE) {
            clearNpcPoisonLease();
        }
        return new NpcPoisonCancellation(current, poison, paralysis);
    }

    private void clearNpcPoisonLease() {
        npcPoisonKind = ClassicWorldMonsterPoisonAttack.Kind.NONE;
        npcPoisonAttackerObjectId = 0;
    }

    synchronized boolean expireEraseMagic(long nowNanos) {
        return eraseMagicStatus.expire(nowNanos);
    }

    synchronized boolean freezeActive(long nowNanos) {
        return timedStatus.freezeActive(nowNanos);
    }

    synchronized boolean activateFreeze(long nowNanos, int durationSeconds) {
        return active && hp > 0
                && timedStatus.activateFreeze(nowNanos, durationSeconds);
    }

    synchronized boolean expireFreeze(long nowNanos) {
        return timedStatus.expireFreeze(nowNanos);
    }

    synchronized boolean cancelFreeze() {
        return timedStatus.cancelFreeze();
    }

    synchronized boolean earthBindActive(long nowNanos) {
        return timedStatus.earthBindActive(nowNanos);
    }

    synchronized boolean activateEarthBind(long nowNanos, int durationSeconds) {
        return active && hp > 0
                && timedStatus.activateEarthBind(nowNanos, durationSeconds);
    }

    synchronized boolean expireEarthBind(long nowNanos) {
        return timedStatus.expireEarthBind(nowNanos);
    }

    synchronized boolean cancelEarthBind() {
        return timedStatus.cancelEarthBind();
    }

    synchronized boolean sleepActive(long nowNanos) {
        return timedStatus.sleepActive(nowNanos);
    }

    synchronized boolean activateSleep(long nowNanos, int durationSeconds) {
        return active && hp > 0
                && timedStatus.activateSleep(nowNanos, durationSeconds);
    }

    synchronized boolean expireSleep(long nowNanos) {
        return timedStatus.expireSleep(nowNanos);
    }

    synchronized boolean cancelSleep() {
        return timedStatus.cancelSleep();
    }

    synchronized boolean shockStunActive(long nowNanos) {
        return timedStatus.shockStunActive(nowNanos);
    }

    synchronized BattleTimedStatus.ShockStunActivation activateShockStun(
            long nowNanos, int rolledSeconds, int maximumSeconds) {
        if (!active || hp <= 0) return null;
        return timedStatus.activateShockStun(
                nowNanos, rolledSeconds, maximumSeconds);
    }

    synchronized boolean expireShockStun(long nowNanos) {
        return timedStatus.expireShockStun(nowNanos);
    }

    synchronized boolean boneBreakActive(long nowNanos) {
        return timedStatus.boneBreakActive(nowNanos);
    }

    synchronized BattleArmBreakerStatus.Activation activateArmBreaker(
            long nowNanos, int durationSeconds) {
        if (!active || hp <= 0) {
            return BattleArmBreakerStatus.Activation.rejected();
        }
        return armBreakerStatus.activateIfAbsent(nowNanos, durationSeconds);
    }

    synchronized int armBreakerMeleeDamageModifier(long nowNanos) {
        return active && hp > 0
                ? armBreakerStatus.meleeDamageModifier(nowNanos) : 0;
    }

    synchronized boolean cancelArmBreaker() {
        return armBreakerStatus.cancel();
    }

    synchronized boolean activatePanic(
            long nowNanos, int durationSeconds, int modifier) {
        return active && hp > 0
                && panicStatus.activateIfAbsent(
                        nowNanos, durationSeconds, modifier);
    }

    synchronized int panicAbilityScoreModifier(long nowNanos) {
        return active && hp > 0
                ? panicStatus.snapshot(nowNanos).abilityScoreModifier() : 0;
    }

    synchronized boolean expirePanic(long nowNanos) {
        return panicStatus.expire(nowNanos);
    }

    synchronized boolean cancelPanic() {
        return panicStatus.cancel();
    }

    synchronized BattleJoyOfPainStatus.Activation activateJoyOfPain(
            long nowNanos, int durationSeconds) {
        return !active || hp <= 0 ? null
                : joyOfPainStatus.activateOrRefresh(nowNanos, durationSeconds);
    }

    synchronized BattleJoyOfPainStatus.Lease captureJoyOfPain(long nowNanos) {
        return active && hp > 0 ? joyOfPainStatus.capture(nowNanos) : null;
    }

    synchronized boolean consumeJoyOfPain(BattleJoyOfPainStatus.Lease lease) {
        return joyOfPainStatus.consume(lease);
    }

    synchronized boolean expireJoyOfPain(long nowNanos) {
        return joyOfPainStatus.expire(nowNanos);
    }

    synchronized boolean cancelJoyOfPain() {
        return joyOfPainStatus.cancel();
    }

    synchronized boolean activateBoneBreak(
            long nowNanos, int durationSeconds) {
        return active && hp > 0
                && timedStatus.activateBoneBreak(nowNanos, durationSeconds);
    }

    synchronized boolean expireBoneBreak(long nowNanos) {
        return timedStatus.expireBoneBreak(nowNanos);
    }

    synchronized boolean thunderGrabActive(long nowNanos) {
        return timedStatus.thunderGrabActive(nowNanos);
    }

    synchronized boolean activateThunderGrab(
            long nowNanos, int durationSeconds) {
        return active && hp > 0
                && timedStatus.activateThunderGrab(nowNanos, durationSeconds);
    }

    synchronized boolean expireThunderGrab(long nowNanos) {
        return timedStatus.expireThunderGrab(nowNanos);
    }

    synchronized boolean horrorOfDeathActive(long nowNanos) {
        return timedStatus.horrorOfDeathActive(nowNanos);
    }

    synchronized boolean activateHorrorOfDeath(
            long nowNanos, int durationSeconds) {
        return active && hp > 0
                && timedStatus.activateHorrorOfDeath(nowNanos, durationSeconds);
    }

    synchronized boolean expireHorrorOfDeath(long nowNanos) {
        return timedStatus.expireHorrorOfDeath(nowNanos);
    }

    synchronized Snapshot snapshot() {
        Object object = NativeSourceCompanionFirstActor.capture(this);
        int n = this.panicStatus.snapshot(System.nanoTime()).abilityScoreModifier();
        return new Snapshot(this.objectId, this.profile.npcId(), this.profile.gfx(), this.name, this.kind, this.active, this.posture, this.targetObjectId, this.x, this.y, this.heading, this.nextActionNanos, this.hp, this.profile.maxHp(), this.mp, this.profile.maxMp(), this.profile.level(), this.profile.armorClass(), Math.max(1, this.profile.strength() + n), Math.max(1, this.profile.dexterity() + n), this.moveSpeed, this.profile.petCost(), this.expiresAtNanos, this.collarObjectId, this.collarCatalogId, this.experience, BattleCompanion.experiencePercentage(this.profile.level(), this.experience), this.lawful, NativeSourceNpcFactoryMemory.carry(this), object);
    }

    synchronized boolean expired(long nowNanos) {
        NativeSourceCompanionElemental.expire(this, nowNanos);
        return this.active && this.expiresAtNanos > 0L && nowNanos >= this.expiresAtNanos;
    }

    synchronized boolean selectTarget(long selectedObjectId) {
        if (!active || hp <= 0
                || selectedObjectId <= 0 || selectedObjectId > 0xFFFF_FFFFL) {
            return false;
        }
        targetObjectId = selectedObjectId;
        pathFailCount = 0;
        nextActionNanos = 0;
        return true;
    }

    /** Retained {@code L1PcInstance.setPetTarget}: aggressive summons follow the master's target. */
    synchronized boolean selectMasterTarget(long selectedObjectId) {
        if (!active || hp <= 0 || posture != Posture.AGGRESSIVE
                || selectedObjectId <= 0 || selectedObjectId > 0xFFFF_FFFFL) {
            return false;
        }
        targetObjectId = selectedObjectId;
        pathFailCount = 0;
        nextActionNanos = 0;
        return true;
    }

    synchronized void clearTarget() {
        targetObjectId = 0;
        pathFailCount = 0;
    }

    synchronized int pathFailureCount() {
        return pathFailCount;
    }

    /**
     * Retained pet/summon path retry policy.  A proven static-map failure is
     * the only failure that advances the 20-attempt give-up counter; live
     * crowding and other transient races keep the selected target.
     *
     * @return true only when this attempt cleared the target.
     */
    synchronized boolean retryPath(
            long nowNanos, ClassicWorldNpcPathfinder.Failure failure,
            boolean sameMasterCrowding) {
        if (!active || hp <= 0 || targetObjectId == 0
                || nowNanos < 0 || failure == null
                || failure == ClassicWorldNpcPathfinder.Failure.NONE) {
            return false;
        }
        if (sameMasterCrowding) {
            pathFailCount = 0;
            nextActionNanos = nowNanos
                    + CROWD_RETRY_MILLIS * 1_000_000L;
            return false;
        }
        if (failure == ClassicWorldNpcPathfinder.Failure.TRANSIENT) {
            pathFailCount = 0;
            nextActionNanos = nowNanos
                    + TRANSIENT_PATH_RETRY_MILLIS * 1_000_000L;
            return false;
        }
        pathFailCount++;
        nextActionNanos = nowNanos + CROWD_RETRY_MILLIS * 1_000_000L;
        if (pathFailCount < PATH_FAIL_GIVE_UP) return false;
        targetObjectId = 0;
        pathFailCount = 0;
        return true;
    }

    synchronized boolean ready(long nowNanos) {
        return active && hp > 0 && targetObjectId != 0
                && !timedStatus.sleepActive(nowNanos)
                && !timedStatus.freezeActive(nowNanos)
                && !timedStatus.earthBindActive(nowNanos)
                && !timedStatus.shockStunActive(nowNanos)
                && !timedStatus.boneBreakActive(nowNanos)
                && !timedStatus.thunderGrabActive(nowNanos)
                && !timedStatus.paralysisActive(nowNanos)
                && !curseParalysisStatus.paralysisActive(nowNanos)
                && nowNanos >= nextActionNanos;
    }

    synchronized Movement moveOneTile(int destinationX, int destinationY,
                                      int moveHeading, long nowNanos) {
        requireCoordinate(destinationX, "companion destination x");
        requireCoordinate(destinationY, "companion destination y");
        if (!ready(nowNanos) || moveHeading < 0 || moveHeading > 7
                || Math.max(Math.abs(destinationX - x), Math.abs(destinationY - y)) != 1) {
            return null;
        }
        Movement movement = new Movement(x, y, destinationX, destinationY, moveHeading);
        pathFailCount = 0;
        x = destinationX;
        y = destinationY;
        heading = moveHeading;
        nextActionNanos = nowNanos
                + effectiveMoveIntervalMillis() * 1_000_000L;
        return movement;
    }

    synchronized Strike beginStrike(int targetX, int targetY, long nowNanos) {
        return this.beginStrike(targetX, targetY, 1, nowNanos);
    }

    synchronized Strike beginStrike(int targetX, int targetY, int allowedRange, long nowNanos) {
        int n4 = Math.max(Math.abs(targetX - this.x), Math.abs(targetY - this.y));
        if (!this.ready(nowNanos) || allowedRange < 1 || n4 < 1 || n4 > allowedRange) {
            return null;
        }
        this.heading = BattleCompanion.heading(this.x, this.y, targetX, targetY);
        this.pathFailCount = 0;
        this.nextActionNanos = nowNanos + (long)NativeCompanionWindState.interval(this.effectiveAttackIntervalMillis(), this, nowNanos) * 1000000L;
        return new Strike(this.heading);
    }

    private int effectiveMoveIntervalMillis() {
        if (moveSpeed == BattleTimedStatus.SLOW) {
            return Math.max(1, (int) (profile.moveIntervalMillis()
                    * BattleActionInterval.SLOW_MULTIPLIER));
        }
        return moveSpeed == BattleTimedStatus.HASTE
                && profile.moveSpeed() != BattleTimedStatus.HASTE
                ? hasteInterval(profile.moveIntervalMillis())
                : profile.moveIntervalMillis();
    }

    private int effectiveAttackIntervalMillis() {
        if (moveSpeed == BattleTimedStatus.SLOW) {
            return Math.max(1, (int) (profile.attackIntervalMillis()
                    * BattleActionInterval.SLOW_MULTIPLIER));
        }
        return moveSpeed == BattleTimedStatus.HASTE
                && profile.moveSpeed() != BattleTimedStatus.HASTE
                ? hasteInterval(profile.attackIntervalMillis())
                : profile.attackIntervalMillis();
    }

    synchronized void defer(long nowNanos, int delayMillis) {
        if (active && targetObjectId != 0 && delayMillis > 0) {
            nextActionNanos = nowNanos + delayMillis * 1_000_000L;
        }
    }

    synchronized void relocate(int destinationX, int destinationY) {
        requireCoordinate(destinationX, "companion relocation x");
        requireCoordinate(destinationY, "companion relocation y");
        x = destinationX;
        y = destinationY;
        pathFailCount = 0;
        nextActionNanos = 0;
    }

    /** Living, active pets accept TYPE_HEAL recovery up to their template maximum. */
    synchronized int heal(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("invalid companion healing amount");
        }
        if (!active || hp <= 0 || amount == 0) return hp;
        hp = Math.min(profile.maxHp(), Math.addExact(hp, amount));
        return hp;
    }

    synchronized Damage applyDamage(int amount) {
        boolean bl;
        if (amount < 0) {
            throw new IllegalArgumentException("invalid companion damage amount");
        }
        Snapshot snapshot = this.snapshot();
        if (!this.active || this.hp <= 0 || amount == 0) {
            return new Damage(snapshot, snapshot, 0, false);
        }
        int n2 = Math.min(this.hp, amount);
        this.hp -= n2;
        NativeCloseoutCompanionSkills.terminal(this);
        this.timedStatus.cancelSleep();
        boolean bl2 = bl = this.hp == 0;
        if (bl) {
            this.clearSpeedEffects();
            this.eraseMagicStatus.clear();
            this.timedStatus.clear();
            NativeCompanionWindState.clear(this);
            this.curseParalysisStatus.clear();
            this.clearNpcPoisonLease();
            this.panicStatus.clear();
            this.joyOfPainStatus.clear();
            this.weaponBreakStacks = 0;
            if (this.kind != Kind.PET) {
                BattleCompanion recipient = this;
                boolean updatedValue = false;
                NativeCompanionCallbacks.advance(this);
                recipient.active = updatedValue;
                NativeCloseoutCompanionSkills.terminal(this);
                this.targetObjectId = 0L;
                this.nextActionNanos = 0L;
            }
        }
        return new Damage(snapshot, this.snapshot(), n2, bl);
    }

    synchronized CommandResult apply(String action) {
        if (!this.active || this.hp <= 0 || action == null || !action.equals(action.toLowerCase(Locale.ROOT))) {
            return CommandResult.REJECTED;
        }
        switch (action) {
            case "aggressive": {
                this.posture = Posture.AGGRESSIVE;
                break;
            }
            case "defensive": {
                this.posture = Posture.DEFENSIVE;
                break;
            }
            case "stay": {
                this.posture = Posture.STAY;
                this.targetObjectId = 0L;
                this.nextActionNanos = 0L;
                break;
            }
            case "extend": {
                this.posture = Posture.EXTEND;
                break;
            }
            case "alert": {
                this.posture = Posture.ALERT;
                break;
            }
            case "dismiss": {
                BattleCompanion recipient = this;
                boolean updatedValue = false;
                NativeCompanionCallbacks.advance(this);
                recipient.active = updatedValue;
                NativeCloseoutCompanionSkills.terminal(this);
                this.targetObjectId = 0L;
                this.nextActionNanos = 0L;
                this.clearSpeedEffects();
                this.timedStatus.clear();
                NativeCompanionWindState.clear(this);
                this.curseParalysisStatus.clear();
                this.clearNpcPoisonLease();
                this.panicStatus.clear();
                this.joyOfPainStatus.clear();
                this.weaponBreakStacks = 0;
                return CommandResult.DISMISSED;
            }
            case "attackchr": {
                return CommandResult.TARGET_SELECTION_PENDING;
            }
            case "getitem": {
                return CommandResult.COLLECTION_REQUESTED;
            }
            case "changename": {
                return CommandResult.RENAME_PENDING;
            }
            default: {
                return CommandResult.REJECTED;
            }
        }
        return CommandResult.POSTURE_CHANGED;
    }

    synchronized boolean receiveItem(InventoryCatalog.Definition definition, long quantity) {
        return active && hp > 0 && carriedItems.add(definition, quantity);
    }

    synchronized boolean receiveItem(RuntimeInventory.Item item, long quantity) {
        return active && hp > 0 && carriedItems.add(item, quantity);
    }

    synchronized boolean canReceiveItem(RuntimeInventory.Item item, long quantity) {
        RuntimeInventory staged = carriedItems.copy();
        return active && hp > 0
                && staged.addTransferredResult(item, quantity) != null;
    }

    /** Commits the owner sidecar before publishing an item in the companion bag. */
    synchronized boolean receiveItemPersisted(
            RuntimeInventory.Item item, long quantity, DurableCommit commit)
            throws java.io.IOException {
        return receiveItemAndOnGetItemPersisted(
                item, quantity, System.nanoTime(), commit) != null;
    }

    synchronized GivenItemReceipt receiveItemAndOnGetItemPersisted(RuntimeInventory.Item item, long quantity, long nowNanos, DurableCommit commit) throws IOException {
        RuntimeInventory.Item item2;
        RuntimeInventory runtimeInventory = this.carriedItems.copy();
        RuntimeInventory.Item item3 = item2 = item == null ? null : runtimeInventory.addTransferredResult(item, quantity);
        if (!this.active || this.hp <= 0 || nowNanos < 0L || commit == null || item2 == null) {
            return null;
        }
        commit.commit();
        this.carriedItems = runtimeInventory;
        Snapshot snapshot = this.snapshot();
        GivenItemUseKind givenItemUseKind = GivenItemUseKind.NONE;
        long l3 = 0L;
        int n = 0;
        int n2 = 0;
        int n3 = 0;
        long l4 = item.definition().catalogId();
        if (!this.timedStatus.decayPotionActive(nowNanos) && BattleCompanion.healingPotion(l4) && this.hp != this.profile.maxHp()) {
            l3 = this.firstCarriedConsumable(40012L, 40011L, 40010L);
            if (l3 != 0L) {
                int n4 = switch ((int)l3) {
                    case 40012 -> 75;
                    case 40011 -> 45;
                    default -> 15;
                };
                n = switch ((int)l3) {
                    case 40012 -> 197;
                    case 40011 -> 194;
                    default -> 189;
                };
                int n5 = BattlePotionRecovery.adjustedAmount(n4, 0, 0, this.timedStatus.polluteWaterActive(nowNanos), this.timedStatus.waterDragonDirtyCurrentActive(nowNanos), this.timedStatus.waterDragonPotionErosionActive(nowNanos));
                int n6 = this.hp;
                this.hp = Math.max(0, Math.min(this.profile.maxHp(), Math.addExact(this.hp, n5)));
                n3 = this.hp - n6;
                givenItemUseKind = GivenItemUseKind.HEAL;
                if (this.hp == 0) {
                    this.clearSpeedEffects();
                    this.eraseMagicStatus.clear();
                    this.timedStatus.clear();
                    NativeCompanionWindState.clear(this);
                    this.curseParalysisStatus.clear();
                    this.clearNpcPoisonLease();
                    this.panicStatus.clear();
                    this.joyOfPainStatus.clear();
                    if (this.kind != Kind.PET) {
                        BattleCompanion recipient = this;
                        boolean updatedValue = false;
                        NativeCompanionCallbacks.advance(this);
                        recipient.active = updatedValue;
                        this.targetObjectId = 0L;
                        this.nextActionNanos = 0L;
                    }
                }
            }
        } else if (!this.timedStatus.decayPotionActive(nowNanos) && BattleCompanion.hastePotion(l4)) {
            if (this.hasteSkillId != 0 && nowNanos >= this.hasteExpiresAtNanos) {
                this.clearHaste();
            }
            if (this.hasteSkillId == 0 && (l3 = this.firstCarriedConsumable(140018L, 40018L, 140013L, 40013L)) != 0L) {
                n2 = switch ((int)l3) {
                    case 140018 -> 2100;
                    case 40018 -> 1800;
                    case 140013 -> 350;
                    default -> 300;
                };
                this.slowExpiresAtNanos = 0L;
                this.slowSkillId = 0;
                this.moveSpeed = 1;
                this.hasteExpiresAtNanos = Math.addExact(nowNanos, (long)n2 * 1000000000L);
                this.hasteSkillId = 43;
                n = 191;
                givenItemUseKind = GivenItemUseKind.HASTE;
            }
        }
        return new GivenItemReceipt(snapshot, this.snapshot(), item2.objectId(), l4, Math.toIntExact(quantity), givenItemUseKind, l3, n, n2, n3, this.carriedItems.activeLightRange());
    }

    synchronized List<RuntimeInventory.Item> carriedItems() {
        return carriedItems.items();
    }

    synchronized int lightRange() {
        return carriedItems.activeLightRange();
    }

    private long firstCarriedConsumable(long... catalogIds) {
        for (long catalogId : catalogIds) {
            if (carriedItems.removeCatalog(catalogId, 1)) return catalogId;
        }
        return 0;
    }

    private static boolean healingPotion(long catalogId) {
        return catalogId == InventoryCatalog.GREATER_HEALING_POTION
                || catalogId == InventoryCatalog.EXTRA_HEALING_POTION
                || catalogId == InventoryCatalog.NATIVE_HEALING_POTION;
    }

    private static boolean hastePotion(long catalogId) {
        return catalogId == BLESSED_GREATER_HASTE_POTION
                || catalogId == GREATER_HASTE_POTION
                || catalogId == BLESSED_HASTE_POTION
                || catalogId == InventoryCatalog.SELF_HASTE_POTION;
    }

    synchronized Snapshot evolve(GameStateStore.PetState evolved, SourcePetTemplate targetTemplate) {
        if (this.kind != Kind.PET || !this.active || this.hp <= 0 || evolved == null || targetTemplate == null || evolved.objectId() != this.objectId || evolved.npcId() != targetTemplate.npcId() || evolved.collarCatalogId() != targetTemplate.collarCatalogId() || evolved.collarObjectId() == this.collarObjectId || !evolved.sourceKey().equals(this.petSourceKey) || evolved.level() != 1 || evolved.experience() != 0L) {
            throw new IllegalArgumentException("invalid persistent pet evolution");
        }
        BattleAnimationCatalog.get().requireBattleCapableSprite(targetTemplate.gfx(), 0);
        this.profile = new Profile(targetTemplate.npcId(), targetTemplate.gfx(), evolved.level(), evolved.maxHp(), evolved.maxMp(), targetTemplate.armorClass(), targetTemplate.strength(), targetTemplate.dexterity(), BattleCompanion.hasteInterval(targetTemplate.moveIntervalMillis()), BattleCompanion.hasteInterval(targetTemplate.attackIntervalMillis()), 1, targetTemplate.charismaCost(), targetTemplate.name());
        this.qualifyNpcDefense(targetTemplate.npcId(), targetTemplate.sourceLevel(), targetTemplate.undead(), targetTemplate.magicResistance(), targetTemplate.boss(), targetTemplate.ranged(), targetTemplate.bowActionId(), targetTemplate.poisonAttack(), targetTemplate.paralysisAttack());
        this.name = evolved.name();
        this.collarObjectId = evolved.collarObjectId();
        this.collarCatalogId = evolved.collarCatalogId();
        this.experience = evolved.experience();
        this.lawful = evolved.lawful();
        this.hp = evolved.maxHp();
        this.mp = evolved.maxMp();
        this.posture = Posture.STAY;
        this.targetObjectId = 0L;
        this.nextActionNanos = 0L;
        this.clearSpeedEffects();
        this.timedStatus.clear();
        NativeCompanionWindState.clear(this);
        this.curseParalysisStatus.clear();
        this.clearNpcPoisonLease();
        this.panicStatus.clear();
        this.joyOfPainStatus.clear();
        this.weaponBreakStacks = 0;
        this.carriedItems.clear();
        return this.snapshot();
    }

    synchronized void clearCarriedItems() {
        carriedItems.clear();
    }

    /**
     * Same-family C_UsePetItem rejects this exact classic shepherd-dog NPC
     * template before it reads or mutates the selected inventory row.
     */
    synchronized boolean nativeEquipmentEligible() {
        return nativeEquipmentEligibleNpc(profile.npcId());
    }

    static boolean nativeEquipmentEligibleNpc(int npcId) {
        return switch (npcId) {
            case 45034, 45039, 45040, 45042, 45043, 45044, 45046, 45047,
                    45048, 45049, 45053, 45054, 45313, 45711, 46042, 46044 -> false;
            default -> true;
        };
    }

    synchronized boolean removeCarriedItem(long objectId, long quantity) {
        return carriedItems.remove(objectId, quantity);
    }

    /** Classic pets can replace their NPC-template name only once. */
    synchronized boolean renameOnce(String newName) {
        if (!active || newName == null || !name.equals(profile.initialName())) return false;
        name = newName;
        return true;
    }

    synchronized boolean resurrect(int restoredHp) {
        if (!this.active || this.hp != 0 || restoredHp <= 0 || restoredHp > this.profile.maxHp()) {
            return false;
        }
        BattleCompanion recipient = this;
        int updatedValue = restoredHp;
        NativeCompanionCallbacks.advance(this);
        recipient.hp = updatedValue;
        this.targetObjectId = 0L;
        this.nextActionNanos = 0L;
        this.posture = Posture.STAY;
        this.timedStatus.clear();
        NativeCompanionWindState.clear(this);
        this.curseParalysisStatus.clear();
        this.clearNpcPoisonLease();
        return true;
    }

    synchronized void updatePetProgress(
            int level, int maxHp, int maxMp, long newExperience) {
        if (kind != Kind.PET || level < profile.level() || level > 0xFF
                || maxHp < profile.maxHp() || maxMp < profile.maxMp()
                || newExperience < experience || newExperience > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("invalid persistent pet progress");
        }
        profile = new Profile(profile.npcId(), profile.gfx(), level,
                maxHp, maxMp, profile.armorClass(), profile.strength(), profile.dexterity(),
                profile.moveIntervalMillis(), profile.attackIntervalMillis(),
                profile.moveSpeed(), profile.petCost(), profile.initialName());
        experience = newExperience;
    }

    synchronized GameStateStore.PetState durablePetState() {
        if (kind != Kind.PET || collarObjectId == 0) return null;
        return new GameStateStore.PetState(
                petSourceKey, collarObjectId, objectId, collarCatalogId,
                profile.npcId(), name, profile.level(), profile.maxHp(), profile.maxMp(),
                experience, lawful);
    }

    private static int experiencePercentage(int level, long experience) {
        if (level >= ClassicExperienceCatalog.MAXIMUM_LEVEL) return 0;
        long floor = ClassicExperienceCatalog.minimumExperienceForLevel(level);
        long ceiling = ClassicExperienceCatalog.minimumExperienceForLevel(level + 1);
        if (experience <= floor || ceiling <= floor) return 0;
        return (int) Math.max(0, Math.min(100,
                100L * (experience - floor) / (ceiling - floor)));
    }

    private static int heading(int sourceX, int sourceY, int targetX, int targetY) {
        int dx = Integer.compare(targetX, sourceX);
        int dy = Integer.compare(targetY, sourceY);
        if (dx > 0 && dy > 0) return 3;
        if (dx < 0 && dy < 0) return 7;
        if (dx > 0 && dy == 0) return 2;
        if (dx < 0 && dy == 0) return 6;
        if (dx == 0 && dy > 0) return 4;
        if (dx < 0) return 5;
        if (dx > 0) return 1;
        return 0;
    }

    private static void requireCoordinate(int value, String field) {
        if (value < 0 || value > 0xFFFF) {
            throw new IllegalArgumentException("invalid " + field);
        }
    }
}
