package local.lito381.server;

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

    record Snapshot(long objectId, int npcId, int gfx, String name, Kind kind,
                    boolean active, Posture posture,
                    long targetObjectId,
                    int x, int y, int heading, long nextActionNanos,
                    int hp, int maxHp, int mp, int maxMp, int level,
                    int armorClass, int strength, int dexterity,
                    int moveSpeed, int petCost, long expiresAtNanos,
                    long collarObjectId, int collarCatalogId,
                    long experience, int experiencePercentage, int lawful) {
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
    }

    private final long objectId;
    private final Kind kind;
    private Profile profile;
    private String name;
    private boolean active;
    private Posture posture = Posture.STAY;
    private long targetObjectId;
    private int pathFailCount;
    private int x;
    private int y;
    private int heading;
    private int hp;
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
    private long hasteExpiresAtNanos;
    private int hasteSkillId;
    private long slowExpiresAtNanos;
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
    private RuntimeInventory carriedItems = new RuntimeInventory();
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

    static BattleCompanion tamedFromQuest(
            long objectId, BattleQuestEncounter.Snapshot captured,
            SourceNpcPhysicalStats stats) {
        if (captured == null || stats == null || captured.npcId() != stats.npcId()) {
            throw new IllegalArgumentException("invalid quest tamed source");
        }
        BattleCompanion result = tamed(
                objectId, captured.npcId(), captured.gfx(), captured.name(),
                captured.level(), captured.hp(), captured.maxHp(), stats.maxMp(),
                stats.armorClass(), Math.max(1, stats.strength()),
                Math.max(1, stats.dexterity()), captured.x(), captured.y());
        result.qualifyNpcDefense(stats);
        result.questTamedOrigin = captured;
        return result;
    }

    static BattleCompanion tamedFromTimeAttack(
            long objectId, BattleTimeAttackEncounter.Snapshot captured,
            SourceNpcPhysicalStats stats) {
        if (captured == null || stats == null || captured.npcId() != stats.npcId()) {
            throw new IllegalArgumentException("invalid Time Attack tamed source");
        }
        BattleCompanion result = tamed(
                objectId, captured.npcId(), captured.gfx(), captured.name(),
                captured.level(), captured.hp(), captured.maxHp(), stats.maxMp(),
                stats.armorClass(), Math.max(1, stats.strength()),
                Math.max(1, stats.dexterity()), captured.x(), captured.y());
        result.qualifyNpcDefense(stats);
        result.timeAttackTamedOrigin = captured;
        return result;
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

    static BattleCompanion zombie(long objectId,
                                  BattleSpellCatalog.ZombieTemplate template,
                                  SourceNpcPhysicalStats stats,
                                  int petCost, int spawnX, int spawnY) {
        if (template == null || stats == null || template.npcId() != stats.npcId()
                || petCost <= 0) {
            throw new IllegalArgumentException("invalid create-zombie companion source");
        }
        BattleAnimationCatalog animations = BattleAnimationCatalog.get();
        int moveMillis = Math.toIntExact(Math.max(40L, animations.requireTimedAction(
                template.gfx(), BattleActionCodes.WALK).durationMillis()));
        int attackMillis = Math.toIntExact(Math.max(40L, animations.requireTimedAction(
                template.gfx(), BattleActionCodes.ATTACK).durationMillis()));
        Profile profile = new Profile(template.npcId(), template.gfx(), template.level(),
                template.maxHp(), stats.maxMp(), stats.armorClass(),
                Math.max(1, stats.strength()), Math.max(1, stats.dexterity()),
                moveMillis, attackMillis, BattleTimedStatus.NORMAL,
                petCost, template.name());
        BattleCompanion result = new BattleCompanion(objectId, profile, Kind.SUMMON);
        result.qualifyNpcDefense(stats);
        result.activate(spawnX, spawnY, template.maxHp(), stats.maxMp());
        return result;
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

    private synchronized void activate(int spawnX, int spawnY,
                                       int currentHp, int currentMp) {
        requireCoordinate(spawnX, "companion spawn x");
        requireCoordinate(spawnY, "companion spawn y");
        if (currentHp <= 0 || currentHp > profile.maxHp()
                || currentMp < 0 || currentMp > profile.maxMp()) {
            throw new IllegalArgumentException("invalid companion current vitals");
        }
        active = true;
        posture = Posture.STAY;
        targetObjectId = 0;
        pathFailCount = 0;
        x = spawnX;
        y = spawnY;
        heading = 0;
        hp = currentHp;
        mp = currentMp;
        nextActionNanos = 0;
        clearSpeedEffects();
        eraseMagicStatus.clear();
        timedStatus.clear();
        curseParalysisStatus.clear();
        clearNpcPoisonLease();
        panicStatus.clear();
        joyOfPainStatus.clear();
        weaponBreakStacks = 0;
    }

    synchronized void deactivate() {
        active = false;
        targetObjectId = 0;
        pathFailCount = 0;
        nextActionNanos = 0;
        clearSpeedEffects();
        eraseMagicStatus.clear();
        timedStatus.clear();
        curseParalysisStatus.clear();
        clearNpcPoisonLease();
        panicStatus.clear();
        joyOfPainStatus.clear();
        weaponBreakStacks = 0;
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

    /**
     * Atomically applies the subset of retained CANCELLATION state currently
     * implemented by a controlled actor.  The pre-clear snapshot preserves
     * the authoritative target identity and coordinates for packet delivery.
     */
    synchronized MagicCancellation cancelMagicEffects() {
        if (!active || hp <= 0) return null;
        Snapshot target = snapshot();
        boolean haste = cancelHaste() != 0;
        boolean slow = cancelSlow() != 0;
        // Retained CANCELLATION always invokes L1NpcInstance.setMoveSpeed(0),
        // even for templates whose source row is born hasted.  Natural lease
        // expiry still restores profile.moveSpeed() through clearHaste().
        moveSpeed = BattleTimedStatus.NORMAL;
        boolean sleep = timedStatus.cancelSleep();
        boolean freeze = timedStatus.cancelFreeze();
        boolean earthBind = timedStatus.cancelEarthBind();
        boolean thunderGrab = timedStatus.cancelThunderGrab();
        boolean horrorOfDeath = timedStatus.cancelHorrorOfDeath();
        boolean boneBreak = timedStatus.cancelBoneBreak();
        NpcPoisonCancellation poisonCancellation = cancelNpcPoison();
        boolean curseParalysis = curseParalysisStatus
                .cancelParalysis().present();
        boolean eraseMagic = cancelEraseMagic();
        armBreakerStatus.cancel();
        boolean panic = panicStatus.cancel();
        joyOfPainStatus.cancel();
        clearWeaponBreakStacks();
        return new MagicCancellation(
                target, haste, slow, sleep, freeze, earthBind, thunderGrab,
                horrorOfDeath, boneBreak, poisonCancellation.poison(),
                poisonCancellation.paralysis() || curseParalysis,
                eraseMagic, panic);
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
        int panicModifier = panicStatus.snapshot(System.nanoTime())
                .abilityScoreModifier();
        return new Snapshot(objectId, profile.npcId(), profile.gfx(), name, kind,
                active, posture, targetObjectId,
                x, y, heading, nextActionNanos,
                hp, profile.maxHp(), mp, profile.maxMp(), profile.level(),
                profile.armorClass(), Math.max(1, profile.strength() + panicModifier),
                Math.max(1, profile.dexterity() + panicModifier),
                moveSpeed, profile.petCost(), expiresAtNanos,
                collarObjectId, collarCatalogId, experience,
                experiencePercentage(profile.level(), experience), lawful);
    }

    synchronized boolean expired(long nowNanos) {
        return active && expiresAtNanos > 0 && nowNanos >= expiresAtNanos;
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
        return beginStrike(targetX, targetY, 1, nowNanos);
    }

    synchronized Strike beginStrike(
            int targetX, int targetY, int allowedRange, long nowNanos) {
        int distance = Math.max(Math.abs(targetX - x), Math.abs(targetY - y));
        if (!ready(nowNanos) || allowedRange < 1
                || distance < 1 || distance > allowedRange) {
            return null;
        }
        heading = heading(x, y, targetX, targetY);
        pathFailCount = 0;
        nextActionNanos = nowNanos
                + effectiveAttackIntervalMillis() * 1_000_000L;
        return new Strike(heading);
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
        if (amount < 0) throw new IllegalArgumentException("invalid companion damage amount");
        Snapshot before = snapshot();
        if (!active || hp <= 0 || amount == 0) {
            return new Damage(before, before, 0, false);
        }
        int applied = Math.min(hp, amount);
        hp -= applied;
        timedStatus.cancelSleep();
        boolean diedNow = hp == 0;
        if (diedNow) {
            clearSpeedEffects();
            eraseMagicStatus.clear();
            timedStatus.clear();
            curseParalysisStatus.clear();
            clearNpcPoisonLease();
            panicStatus.clear();
            joyOfPainStatus.clear();
            weaponBreakStacks = 0;
            if (kind != Kind.PET) {
                active = false;
                targetObjectId = 0;
                nextActionNanos = 0;
            }
        }
        return new Damage(before, snapshot(), applied, diedNow);
    }

    synchronized CommandResult apply(String action) {
        if (!active || hp <= 0 || action == null
                || !action.equals(action.toLowerCase(Locale.ROOT))) {
            return CommandResult.REJECTED;
        }
        switch (action) {
            case "aggressive" -> posture = Posture.AGGRESSIVE;
            case "defensive" -> posture = Posture.DEFENSIVE;
            case "stay" -> {
                posture = Posture.STAY;
                // Classic pet status 3 calls allTargetClear(). This must also
                // stop an explicit setMasterSelectTarget chase already in flight.
                targetObjectId = 0;
                nextActionNanos = 0;
            }
            case "extend" -> posture = Posture.EXTEND;
            case "alert" -> posture = Posture.ALERT;
            case "dismiss" -> {
                active = false;
                targetObjectId = 0;
                nextActionNanos = 0;
                clearSpeedEffects();
                timedStatus.clear();
                curseParalysisStatus.clear();
                clearNpcPoisonLease();
                panicStatus.clear();
                joyOfPainStatus.clear();
                weaponBreakStacks = 0;
                return CommandResult.DISMISSED;
            }
            case "attackchr" -> {
                return CommandResult.TARGET_SELECTION_PENDING;
            }
            case "getitem" -> {
                return CommandResult.COLLECTION_REQUESTED;
            }
            case "changename" -> {
                return CommandResult.RENAME_PENDING;
            }
            default -> {
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

    /**
     * Exact retained L1Pet/L1Summon onGetItem specialization: no ordinary
     * refine/shuffle call; a received healing or haste potion triggers one
     * immediate highest-priority bag consumption at 100 percent chance.
     */
    synchronized GivenItemReceipt receiveItemAndOnGetItemPersisted(
            RuntimeInventory.Item item, long quantity, long nowNanos,
            DurableCommit commit) throws java.io.IOException {
        RuntimeInventory staged = carriedItems.copy();
        RuntimeInventory.Item destination = item == null ? null
                : staged.addTransferredResult(item, quantity);
        if (!active || hp <= 0 || nowNanos < 0 || commit == null
                || destination == null) {
            return null;
        }
        commit.commit();
        carriedItems = staged;
        Snapshot before = snapshot();
        GivenItemUseKind useKind = GivenItemUseKind.NONE;
        long consumedCatalogId = 0;
        int effectGfx = 0;
        int durationSeconds = 0;
        int appliedHp = 0;
        long receivedCatalogId = item.definition().catalogId();

        if (!timedStatus.decayPotionActive(nowNanos)
                && healingPotion(receivedCatalogId)
                && hp != profile.maxHp()) {
            consumedCatalogId = firstCarriedConsumable(
                    InventoryCatalog.GREATER_HEALING_POTION,
                    InventoryCatalog.EXTRA_HEALING_POTION,
                    InventoryCatalog.NATIVE_HEALING_POTION);
            if (consumedCatalogId != 0) {
                int baseHealing = switch ((int) consumedCatalogId) {
                    case (int) InventoryCatalog.GREATER_HEALING_POTION -> 75;
                    case (int) InventoryCatalog.EXTRA_HEALING_POTION -> 45;
                    default -> 15;
                };
                effectGfx = switch ((int) consumedCatalogId) {
                    case (int) InventoryCatalog.GREATER_HEALING_POTION -> 197;
                    case (int) InventoryCatalog.EXTRA_HEALING_POTION -> 194;
                    default -> 189;
                };
                int requested = BattlePotionRecovery.adjustedAmount(
                        baseHealing, 0, 0,
                        timedStatus.polluteWaterActive(nowNanos),
                        timedStatus.waterDragonDirtyCurrentActive(nowNanos),
                        timedStatus.waterDragonPotionErosionActive(nowNanos));
                int previousHp = hp;
                hp = Math.max(0, Math.min(profile.maxHp(),
                        Math.addExact(hp, requested)));
                appliedHp = hp - previousHp;
                useKind = GivenItemUseKind.HEAL;
                if (hp == 0) {
                    clearSpeedEffects();
                    eraseMagicStatus.clear();
                    timedStatus.clear();
                    curseParalysisStatus.clear();
                    clearNpcPoisonLease();
                    panicStatus.clear();
                    joyOfPainStatus.clear();
                    if (kind != Kind.PET) {
                        active = false;
                        targetObjectId = 0;
                        nextActionNanos = 0;
                    }
                }
            }
        } else if (!timedStatus.decayPotionActive(nowNanos)
                && hastePotion(receivedCatalogId)) {
            if (hasteSkillId != 0 && nowNanos >= hasteExpiresAtNanos) {
                clearHaste();
            }
            if (hasteSkillId == 0) {
                consumedCatalogId = firstCarriedConsumable(
                        BLESSED_GREATER_HASTE_POTION,
                        GREATER_HASTE_POTION,
                        BLESSED_HASTE_POTION,
                        InventoryCatalog.SELF_HASTE_POTION);
                if (consumedCatalogId != 0) {
                    durationSeconds = switch ((int) consumedCatalogId) {
                        case (int) BLESSED_GREATER_HASTE_POTION -> 2100;
                        case (int) GREATER_HASTE_POTION -> 1800;
                        case (int) BLESSED_HASTE_POTION -> 350;
                        default -> 300;
                    };
                    slowExpiresAtNanos = 0;
                    slowSkillId = 0;
                    moveSpeed = BattleTimedStatus.HASTE;
                    hasteExpiresAtNanos = Math.addExact(
                            nowNanos, durationSeconds * 1_000_000_000L);
                    hasteSkillId = BattleSpellCatalog.HASTE;
                    effectGfx = 191;
                    useKind = GivenItemUseKind.HASTE;
                }
            }
        }
        return new GivenItemReceipt(
                before, snapshot(), destination.objectId(), receivedCatalogId,
                Math.toIntExact(quantity), useKind, consumedCatalogId,
                effectGfx, durationSeconds, appliedHp,
                carriedItems.activeLightRange());
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

    /** Retained evolvePet: same world object, new collar/template, level 1 and empty bag. */
    synchronized Snapshot evolve(
            GameStateStore.PetState evolved, SourcePetTemplate targetTemplate) {
        if (kind != Kind.PET || !active || hp <= 0 || evolved == null
                || targetTemplate == null || evolved.objectId() != objectId
                || evolved.npcId() != targetTemplate.npcId()
                || evolved.collarCatalogId() != targetTemplate.collarCatalogId()
                || evolved.collarObjectId() == collarObjectId
                || !evolved.sourceKey().equals(petSourceKey)
                || evolved.level() != 1 || evolved.experience() != 0) {
            throw new IllegalArgumentException("invalid persistent pet evolution");
        }
        BattleAnimationCatalog.get().requireBattleCapableSprite(
                targetTemplate.gfx(), BattleActionCodes.WALK);
        profile = new Profile(targetTemplate.npcId(), targetTemplate.gfx(),
                evolved.level(), evolved.maxHp(), evolved.maxMp(),
                targetTemplate.armorClass(), targetTemplate.strength(),
                targetTemplate.dexterity(),
                hasteInterval(targetTemplate.moveIntervalMillis()),
                hasteInterval(targetTemplate.attackIntervalMillis()),
                BattleTimedStatus.HASTE, targetTemplate.charismaCost(),
                targetTemplate.name());
        qualifyNpcDefense(targetTemplate.npcId(),
                targetTemplate.sourceLevel(), targetTemplate.undead(),
                targetTemplate.magicResistance(), targetTemplate.boss(),
                targetTemplate.ranged(), targetTemplate.bowActionId(),
                targetTemplate.poisonAttack(), targetTemplate.paralysisAttack());
        name = evolved.name();
        collarObjectId = evolved.collarObjectId();
        collarCatalogId = evolved.collarCatalogId();
        experience = evolved.experience();
        lawful = evolved.lawful();
        hp = evolved.maxHp();
        mp = evolved.maxMp();
        posture = Posture.STAY;
        targetObjectId = 0;
        nextActionNanos = 0;
        clearSpeedEffects();
        timedStatus.clear();
        curseParalysisStatus.clear();
        clearNpcPoisonLease();
        panicStatus.clear();
        joyOfPainStatus.clear();
        weaponBreakStacks = 0;
        carriedItems.clear();
        return snapshot();
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
        if (!active || hp != 0
                || restoredHp <= 0 || restoredHp > profile.maxHp()) return false;
        hp = restoredHp;
        targetObjectId = 0;
        nextActionNanos = 0;
        posture = Posture.STAY;
        timedStatus.clear();
        curseParalysisStatus.clear();
        clearNpcPoisonLease();
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
