// ported from candidate 50635f7e; three-way baseline: base-0.6.71.jar and src/main
package local.lito381.server;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import local.lito381.server.BattleActionInterval;
import local.lito381.server.BattleArmBreakerStatus;
import local.lito381.server.BattleCompanion;
import local.lito381.server.BattleElementalFallDownStatus;
import local.lito381.server.BattleEraseMagicStatus;
import local.lito381.server.BattleInventoryWeight;
import local.lito381.server.BattleJoyOfPainStatus;
import local.lito381.server.BattlePanicStatus;
import local.lito381.server.BattlePotionRecovery;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.BattleWeaponBreak;
import local.lito381.server.ClassicWorldCkewMonsterCatalog;
import local.lito381.server.ClassicWorldDesireCaveCatalog;
import local.lito381.server.ClassicWorldGroundItems;
import local.lito381.server.ClassicWorldGuardCatalog;
import local.lito381.server.ClassicWorldGuardianCatalog;
import local.lito381.server.ClassicWorldMapCatalog;
import local.lito381.server.ClassicWorldMobGroupCatalog;
import local.lito381.server.ClassicWorldMonsterAggressionCatalog;
import local.lito381.server.ClassicWorldMonsterCatalog;
import local.lito381.server.ClassicWorldMonsterItemLifecycleCatalog;
import local.lito381.server.ClassicWorldMonsterMagicSkillCatalog;
import local.lito381.server.ClassicWorldMonsterPhysicalSkillCatalog;
import local.lito381.server.ClassicWorldMonsterPickupCatalog;
import local.lito381.server.ClassicWorldMonsterPoisonAttack;
import local.lito381.server.ClassicWorldMonsterStatusSkillCatalog;
import local.lito381.server.ClassicWorldMonsterTeleportCatalog;
import local.lito381.server.ClassicWorldNpcCatalog;
import local.lito381.server.ClassicWorldNpcPathfinder;
import local.lito381.server.ClassicWorldRoyalTrialCatalog;
import local.lito381.server.CombatMonsterCurseLifecycle;
import local.lito381.server.CombatMonsterParalysis;
import local.lito381.server.CombatWorldCommit;
import local.lito381.server.CombatWorldFreeze;
import local.lito381.server.DurableCommit;
import local.lito381.server.InventoryCatalog;
import local.lito381.server.NativeCombatOrigin;
import local.lito381.server.NativeEtcItemTypeCatalog;
import local.lito381.server.NativeStaticNpcMotion;
import local.lito381.server.NativeTamedWorldLifecycle;
import local.lito381.server.RuntimeInventory;
import local.lito381.server.SourcePetTemplate;


/** Runtime slots produced from the selected-source classic WORLD spawn groups. */
final class ClassicWorldMonsterPopulation {
    volatile java.lang.ref.WeakReference nativeStaticMotionHub;
    volatile int nativeStaticMotionInstance;
    static final int HIDDEN_STATUS_NONE = 0;
    static final int HIDDEN_STATUS_SINK = 1;
    static final int HIDDEN_STATUS_FLY = 2;
    static final int HIDDEN_STATUS_ICE = 3;
    /** Reproducible development seed; every candidate still follows retained area rules. */
    static final long DEFAULT_SEED = 21L;
    static final long OBJECT_BASE = 0x6500_0000L;
    static final long MOB_GROUP_DYNAMIC_OBJECT_BASE = 0x65D0_0000L;
    static final long MOB_GROUP_DYNAMIC_OBJECT_END = 0x65DF_FFFFL;
    /** Global one-shot quest actors, below the retained released-pet range. */
    static final long QUEST_DYNAMIC_OBJECT_BASE = 0x65E0_0000L;
    static final long QUEST_DYNAMIC_OBJECT_END = 0x65EF_FFFFL;
    static final long RELEASED_PET_OBJECT_BASE = 0x65F0_0000L;
    private static final int AREA_FULL_SCAN_MAX_CELLS = 262_144;
    private static final int AREA_LARGE_RANDOM_PROBES = 300;
    private static final int SOURCE_RANDOM_ATTEMPTS = 51;
    /** Retained L1NpcInstance ordinary-monster proven-unreachable limit. */
    static final int PATH_FAIL_GIVE_UP = 3;
    /** Retained NpcMove transient path recalculation delay. */
    static final int TRANSIENT_PATH_RETRY_MILLIS = 200;
    /** Retained L1NpcInstance pursuit teleport uses distance strictly below 20. */
    static final int NEAR_TELEPORT_DISTANCE_LIMIT = 20;
    static final int NEAR_TELEPORT_MP_COST = 10;
    static final int NEAR_TELEPORT_RANDOM_PERCENT = 20;
    private static final long BLESSED_GREATER_HASTE_POTION = 140018;
    private static final long GREATER_HASTE_POTION = 40018;
    private static final long BLESSED_HASTE_POTION = 140013;
@AsRecord(components={"mapId","spawnId","spawnNumber","objectId","npcId","x","y","heading","currentHp","maximumHp","currentMp","maximumMp","alive","present","hiddenStatus","actorStatus","definition","gfxId","nameId","title","lawful","passispeed","atkspeed"})

    static final class Snapshot
    {
        private final int mapId;
        private final int spawnId;
        private final int spawnNumber;
        private final long objectId;
        private final int npcId;
        private final int x;
        private final int y;
        private final int heading;
        private final int currentHp;
        private final int maximumHp;
        private final int currentMp;
        private final int maximumMp;
        private final boolean alive;
        private final boolean present;
        private final int hiddenStatus;
        private final int actorStatus;
        private final ClassicWorldMonsterCatalog.SpawnDefinition definition;
        private final int gfxId;
        private final String nameId;
        private final String title;
        private final int lawful;
        private final int passispeed;
        private final int atkspeed;
        private volatile Object sourceCombatOrigin;
        private volatile Object nativeTamedCapturePreparation;

        Snapshot(int n, int n2, int n3, long l, int n4, int n5, int n6, int n7, int n8, int n9, int n10, int n11, boolean bl, boolean bl2, int n12, int n13, ClassicWorldMonsterCatalog.SpawnDefinition spawnDefinition, int n14, String string, String string2, int n15, int n16, int n17) {
            block3: {
                block2: {
                    if (n < 0 || n2 <= 0 || n3 <= 0 || l <= 0L || n4 <= 0 || n5 <= 0 || n6 <= 0 || n7 < 0 || n7 > 7 || n8 < 0 || n9 <= 0) break block2;
                    if (n10 < 0 || n11 < 0) break block2;
                    if (bl == n8 > 0 && n12 >= 0 && n12 <= 3 && n13 >= 0 && n13 <= 255 && (n12 != 0 || n13 == 0) && spawnDefinition != null && n14 >= 0 && n14 <= 65535 && string != null && string2 != null && n15 >= Short.MIN_VALUE && n15 <= Short.MAX_VALUE && n16 >= 0 && n17 >= 0) break block3;
                }
                throw new IllegalArgumentException("invalid classic monster snapshot");
            }
            this.mapId = n;
            this.spawnId = n2;
            this.spawnNumber = n3;
            this.objectId = l;
            this.npcId = n4;
            this.x = n5;
            this.y = n6;
            this.heading = n7;
            this.currentHp = n8;
            this.maximumHp = n9;
            this.currentMp = n10;
            this.maximumMp = n11;
            this.alive = bl;
            this.present = bl2;
            this.hiddenStatus = n12;
            this.actorStatus = n13;
            this.definition = spawnDefinition;
            this.gfxId = n14;
            this.nameId = string;
            this.title = string2;
            this.lawful = n15;
            this.passispeed = n16;
            this.atkspeed = n17;
        }

        Snapshot(int n, int n2, int n3, long l, int n4, int n5, int n6, int n7, int n8, int n9, int n10, int n11, boolean bl, boolean bl2, int n12, int n13, ClassicWorldMonsterCatalog.SpawnDefinition spawnDefinition) {
            this(n, n2, n3, l, n4, n5, n6, n7, n8, n9, n10, n11, bl, bl2, n12, n13, spawnDefinition, spawnDefinition.template().gfxId(), spawnDefinition.template().nameId(), "", spawnDefinition.template().lawful(), spawnDefinition.template().passispeed(), spawnDefinition.template().atkspeed());
        }

        Snapshot(int n, int n2, int n3, long l, int n4, int n5, int n6, int n7, int n8, int n9, int n10, int n11, boolean bl, boolean bl2, ClassicWorldMonsterCatalog.SpawnDefinition spawnDefinition) {
            this(n, n2, n3, l, n4, n5, n6, n7, n8, n9, n10, n11, bl, bl2, 0, 0, spawnDefinition);
        }

        ClassicWorldMonsterCatalog.MonsterTemplate template() {
            return this.definition.template();
        }

        public int mapId() {
            return this.mapId;
        }

        public int spawnId() {
            return this.spawnId;
        }

        public int spawnNumber() {
            return this.spawnNumber;
        }

        public long objectId() {
            return this.objectId;
        }

        public int npcId() {
            return this.npcId;
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

        public int currentHp() {
            return this.currentHp;
        }

        public int maximumHp() {
            return this.maximumHp;
        }

        public int currentMp() {
            return this.currentMp;
        }

        public int maximumMp() {
            return this.maximumMp;
        }

        public boolean alive() {
            return this.alive;
        }

        public boolean present() {
            return this.present;
        }

        public int hiddenStatus() {
            return this.hiddenStatus;
        }

        public int actorStatus() {
            return this.actorStatus;
        }

        public ClassicWorldMonsterCatalog.SpawnDefinition definition() {
            return this.definition;
        }

        public int gfxId() {
            return this.gfxId;
        }

        public String nameId() {
            return this.nameId;
        }

        public String title() {
            return this.title;
        }

        public int lawful() {
            return this.lawful;
        }

        public int passispeed() {
            return this.passispeed;
        }

        public int atkspeed() {
            return this.atkspeed;
        }
    }

    /** Retained skill44 result for one ordinary classic-world NPC actor. */
    record MagicCancellation(
            Snapshot target, boolean haste, boolean slow, boolean blind,
            boolean weakness, boolean disease, boolean silence,
            boolean decayPotion,
            boolean poison, boolean paralysis, boolean sleep, boolean freeze,
            boolean earthBind,
            boolean thunderGrab, boolean horrorOfDeath, boolean windShackle,
            boolean boneBreak,
            boolean polluteWater, boolean strikerGale,
            BattleElementalFallDownStatus.Cancellation elementalFallDown,
            boolean eraseMagic, boolean armorBreak, boolean weaponBreak,
            boolean panic) {
        MagicCancellation {
            if (target == null || !target.present() || !target.alive()
                    || elementalFallDown == null) {
                throw new IllegalArgumentException(
                        "invalid classic monster magic cancellation");
            }
        }
    }

    enum ContributorKind {
        PLAYER,
        PET,
        SUMMON
    }

    /**
     * Retained CalcExp identity: the attacking actor stays distinct from its
     * player owner.  DropShare later resolves pet/summon entries to the owner,
     * while EXP keeps the actor kind for the owner-local split.
     */
    record Contribution(ContributorKind kind, long sourceObjectId,
                        long playerObjectId, long hate) {
        Contribution {
            if (kind == null || sourceObjectId <= 0 || sourceObjectId > 0xFFFF_FFFFL
                    || playerObjectId <= 0 || playerObjectId > 0xFFFF_FFFFL
                    || (kind == ContributorKind.PLAYER
                        && sourceObjectId != playerObjectId)
                    || hate <= 0) {
                throw new IllegalArgumentException("invalid classic monster contribution");
            }
        }

        Contribution(long playerObjectId, long hate) {
            this(ContributorKind.PLAYER, playerObjectId, playerObjectId, hate);
        }
    }

    record Hit(Snapshot before, Snapshot after, int appliedDamage, boolean diedNow,
               List<Contribution> contributions) {
        Hit {
            if (before == null || after == null || appliedDamage < 0
                    || before.objectId() != after.objectId()
                    || diedNow != (before.alive() && !after.alive())
                    || contributions == null || (!diedNow && !contributions.isEmpty())) {
                throw new IllegalArgumentException("invalid classic monster damage transition");
            }
            contributions = List.copyOf(contributions);
        }
    }

    record Healing(Snapshot before, Snapshot after, int appliedHealing) {
        Healing {
            if (before == null || after == null || appliedHealing < 0
                    || before.objectId() != after.objectId()
                    || !before.alive() || !before.present()
                    || !after.alive() || !after.present()
                    || after.currentHp() < before.currentHp()
                    || after.currentHp() - before.currentHp() != appliedHealing) {
                throw new IllegalArgumentException(
                        "invalid classic monster healing transition");
            }
        }
    }

    /** Retained accepted-hit MP debit for one ordinary classic-world NPC actor. */
    record ManaDrain(Snapshot before, Snapshot after,
                     int requestedDrain, int actualDrain) {
        ManaDrain {
            if (before == null || after == null || requestedDrain <= 0
                    || actualDrain < 0 || actualDrain > requestedDrain
                    || before.objectId() != after.objectId()
                    || !before.alive() || !before.present()
                    || !after.alive() || !after.present()
                    || before.currentHp() != after.currentHp()
                    || before.currentMp() - after.currentMp() != actualDrain) {
                throw new IllegalArgumentException(
                        "invalid classic monster mana-drain transition");
            }
        }
    }

    /** Damage already committed by the retained {@code L1DamagePoison} clock. */
    record PoisonUpdate(ContributorKind contributorKind,
                        long attackerObjectId, long playerObjectId,
                        Snapshot target, List<Hit> hits, boolean expired) {
        PoisonUpdate {
            if (contributorKind == null || attackerObjectId <= 0
                    || attackerObjectId > 0xFFFF_FFFFL || playerObjectId <= 0
                    || playerObjectId > 0xFFFF_FFFFL
                    || contributorKind == ContributorKind.PLAYER
                        && attackerObjectId != playerObjectId
                    || target == null) {
                throw new IllegalArgumentException(
                        "invalid classic world monster poison update");
            }
            hits = List.copyOf(hits);
        }
    }

    /** One retained paralysis clock transition for an ordinary WORLD monster. */
    record ParalysisUpdate(Snapshot target, boolean activated,
                           boolean expired, boolean poisonParalysis) {
        ParalysisUpdate {
            if (target == null || activated == expired) {
                throw new IllegalArgumentException(
                        "invalid classic world monster paralysis update");
            }
        }
    }

    /** Result of installing one retained NPC poison_atk lease on an NPC target. */
    record NpcPoison(ClassicWorldMonsterPoisonAttack.Kind kind,
                     Snapshot target, boolean existingPoison, boolean applied) {
        NpcPoison {
            if (kind == null || target == null || applied && existingPoison
                    || applied && kind == ClassicWorldMonsterPoisonAttack.Kind.NONE) {
                throw new IllegalArgumentException(
                        "invalid classic world NPC poison transition");
            }
        }
    }

    record Respawn(Snapshot before, Snapshot after, List<Snapshot> groupMembers) {
        Respawn {
            if (before == null || after == null || before.objectId() != after.objectId()
                    || before.present() || before.alive()
                    || !after.present() || !after.alive() || groupMembers == null) {
                throw new IllegalArgumentException("invalid classic monster respawn");
            }
            groupMembers = List.copyOf(groupMembers);
        }

        Respawn(Snapshot before, Snapshot after) {
            this(before, after, List.of());
        }
    }

    /** Exact retained hidden -> ground transition and provocation. */
    record Reveal(Snapshot before, Snapshot after, long provokedObjectId) {
        Reveal {
            if (before == null || after == null
                    || before.objectId() != after.objectId()
                    || before.hiddenStatus() != HIDDEN_STATUS_SINK
                        && before.hiddenStatus() != HIDDEN_STATUS_FLY
                        && before.hiddenStatus() != HIDDEN_STATUS_ICE
                    || after.hiddenStatus() != HIDDEN_STATUS_NONE
                    || after.actorStatus() != 0
                    || provokedObjectId <= 0) {
                throw new IllegalArgumentException("invalid hidden monster reveal");
            }
        }

        int actionId() {
            return before.hiddenStatus() == HIDDEN_STATUS_FLY
                    ? BattleActionCodes.MOVE_DOWN : BattleActionCodes.APPEAR;
        }
    }

    /** Retained damaged-fly item discovery: land without provoking the observer. */
    record AirItemLanding(
            Snapshot before, Snapshot after, long targetItemObjectId) {
        AirItemLanding {
            if (before == null || after == null
                    || before.objectId() != after.objectId()
                    || before.hiddenStatus() != HIDDEN_STATUS_FLY
                    || before.currentHp() >= before.maximumHp()
                    || after.hiddenStatus() != HIDDEN_STATUS_NONE
                    || after.actorStatus() != 0
                    || targetItemObjectId <= 0
                    || targetItemObjectId > 0xFFFF_FFFFL) {
                throw new IllegalArgumentException(
                        "invalid airborne item landing");
            }
        }

        int actionId() {
            return BattleActionCodes.MOVE_DOWN;
        }
    }

    /** Living spawn detached while a retained Taming Monster summon owns it. */
    record Capture(Snapshot captured, Snapshot hidden) {
        Capture {
            if (captured == null || hidden == null
                    || captured.objectId() != hidden.objectId()
                    || !captured.alive() || !captured.present()
                    || !hidden.alive() || hidden.present()
                    || captured.currentHp() != hidden.currentHp()
                    || captured.currentMp() != hidden.currentMp()) {
                throw new IllegalArgumentException("invalid classic monster capture");
            }
        }
    }

    /** A tamed actor returned to its original population slot. */
    record TamedRelease(Snapshot hidden, Snapshot released) {
        TamedRelease {
            if (hidden == null || released == null
                    || hidden.objectId() == released.objectId()
                    || !hidden.alive() || hidden.present()
                    || !released.alive() || !released.present()) {
                throw new IllegalArgumentException("invalid classic monster release");
            }
        }
    }

    /** Restore an already visible corpse in place without creating a new spawn. */
    record Resurrection(Snapshot before, Snapshot after) {
        Resurrection {
            if (before == null || after == null
                    || before.objectId() != after.objectId()
                    || !before.present() || before.alive()
                    || !after.present() || !after.alive()
                    || before.x() != after.x() || before.y() != after.y()) {
                throw new IllegalArgumentException(
                        "invalid classic monster resurrection");
            }
        }
    }

    record GridPoint(int x, int y) {}

    record Movement(Snapshot before, Snapshot after, Snapshot settled) {
        Movement {
            if (before == null || after == null || settled == null
                    || before.objectId() != after.objectId()
                    || before.objectId() != settled.objectId()
                    || before.mapId() != after.mapId()
                    || before.mapId() != settled.mapId()
                    || Math.max(Math.abs(before.x() - after.x()),
                        Math.abs(before.y() - after.y())) != 1) {
                throw new IllegalArgumentException("invalid classic monster movement");
            }
        }

        Movement(Snapshot before, Snapshot after) {
            this(before, after, after);
        }

        boolean teleportedHome() {
            return after.x() != settled.x() || after.y() != settled.y();
        }
    }

    enum NearTeleportKind {
        FIRST_FOUND,
        RANDOM_PURSUIT
    }

    /** One successful retained {@code L1NpcInstance.nearTeleport} pursuit. */
    record NearTeleport(
            NearTeleportKind kind, Snapshot before, Snapshot after,
            long targetObjectId, int targetX, int targetY,
            int startingDirection, int randomRoll, int mpDebit) {
        NearTeleport {
            if (kind == null || before == null || after == null
                    || before.objectId() != after.objectId()
                    || before.mapId() != after.mapId()
                    || targetObjectId <= 0 || targetObjectId > 0xFFFF_FFFFL
                    || targetX <= 0 || targetY <= 0
                    || startingDirection < 0 || startingDirection > 7
                    || kind == NearTeleportKind.FIRST_FOUND && randomRoll != -1
                    || kind == NearTeleportKind.RANDOM_PURSUIT
                        && (randomRoll < 0
                            || randomRoll >= NEAR_TELEPORT_RANDOM_PERCENT)
                    || Math.max(Math.abs(after.x() - targetX),
                        Math.abs(after.y() - targetY)) != 1
                    || after.heading() != (heading(
                            targetX, targetY, after.x(), after.y()) + 4) % 8
                    || mpDebit != Math.min(before.currentMp(),
                            NEAR_TELEPORT_MP_COST)
                    || before.currentMp() - after.currentMp() != mpDebit) {
                throw new IllegalArgumentException(
                        "invalid classic monster near teleport");
            }
        }
    }

    enum ItemActionKind {
        MOVED,
        PICKUP_READY,
        ABANDONED
    }

    /** One retained checkTargetItem/searchTargetItem/onTargetItem decision. */
    record ItemAction(
            ItemActionKind kind, long targetItemObjectId, Movement movement) {
        ItemAction {
            if (kind == null || targetItemObjectId <= 0
                    || targetItemObjectId > 0xFFFF_FFFFL
                    || (kind == ItemActionKind.MOVED) != (movement != null)) {
                throw new IllegalArgumentException(
                        "invalid classic monster item action");
            }
        }
    }

    /** Whole ground stack plus the synchronous native onGetItem result. */
    record ItemPickup(
            Snapshot actor, long itemObjectId, long destinationObjectId,
            long catalogId, int quantity, List<Refinement> refinements,
            int digestSeconds) {
        ItemPickup {
            if (actor == null || !actor.present() || !actor.alive()
                    || itemObjectId <= 0 || itemObjectId > 0xFFFF_FFFFL
                    || destinationObjectId <= 0
                    || destinationObjectId > 0xFFFF_FFFFL
                    || catalogId <= 0 || quantity <= 0
                    || refinements == null || digestSeconds < 0) {
                throw new IllegalArgumentException(
                        "invalid classic monster item pickup");
            }
            refinements = List.copyOf(refinements);
        }
    }

    enum GiveAdmission {
        ACCEPTED,
        UNKNOWN_TARGET,
        STALE_POSITION,
        CAPACITY
    }

    record Refinement(long outputCatalogId, int quantity) {
        Refinement {
            if (outputCatalogId <= 0 || quantity <= 0) {
                throw new IllegalArgumentException("invalid monster refinement result");
            }
        }
    }

    /** Exact C_GIVE destination plus the synchronous native onGetItem result. */
    record MonsterItemReceipt(
            Snapshot actor, long destinationObjectId, long catalogId,
            int quantity, List<Refinement> refinements, int digestSeconds,
            int lightRange) {
        MonsterItemReceipt {
            if (actor == null || !actor.present() || !actor.alive()
                    || destinationObjectId <= 0
                    || destinationObjectId > 0xFFFF_FFFFL
                    || catalogId <= 0 || quantity <= 0
                    || refinements == null || digestSeconds < 0
                    || lightRange < 0 || lightRange > 0xFF) {
                throw new IllegalArgumentException("invalid monster item receipt");
            }
            refinements = List.copyOf(refinements);
        }
    }

    @FunctionalInterface
    interface MonsterTameCommit {
        void commit(Snapshot actor, List<RuntimeInventory.Item> carriedItems)
                throws IOException;
    }

    /** Atomic C_GIVE/onGetItem result whose living source became a pet. */
    record TamedItemReceipt(
            MonsterItemReceipt item, Capture capture,
            List<RuntimeInventory.Item> carriedItems) {
        TamedItemReceipt {
            if (item == null || capture == null || carriedItems == null
                    || item.actor().objectId() != capture.captured().objectId()) {
                throw new IllegalArgumentException("invalid tamed item receipt");
            }
            carriedItems = List.copyOf(carriedItems);
        }
    }

    /** One whole stack removed by the retained five-second digest clock. */
    record DigestRemoval(
            Snapshot actor, RuntimeInventory.Item item, int elapsedSeconds) {
        DigestRemoval {
            if (actor == null || item == null || elapsedSeconds <= 0) {
                throw new IllegalArgumentException("invalid monster digest removal");
            }
        }
    }

    enum ItemUseKind {
        HEAL,
        HASTE
    }

    /** One retained {@code L1MonsterInstance.onItemUse/useItem} consumption. */
    record ItemUse(
            ItemUseKind kind, Snapshot before, Snapshot after, long catalogId,
            int effectGfx, int durationSeconds, int appliedHpDelta) {
        ItemUse {
            if (kind == null || before == null || after == null
                    || before.objectId() != after.objectId()
                    || catalogId <= 0 || effectGfx <= 0
                    || durationSeconds < 0
                    || kind == ItemUseKind.HASTE
                        && (durationSeconds <= 0 || appliedHpDelta != 0
                            || before.currentHp() != after.currentHp())
                    || kind == ItemUseKind.HEAL
                        && (durationSeconds != 0
                            || after.currentHp() - before.currentHp()
                                != appliedHpDelta)) {
                throw new IllegalArgumentException(
                        "invalid classic monster item use");
            }
        }
    }

    /** Exact first-target doppel appearance swap before retained onTarget. */
    record DoppelTransformation(
            Snapshot before, Snapshot after, long playerObjectId) {
        DoppelTransformation {
            if (before == null || after == null || playerObjectId <= 0
                    || before.objectId() != after.objectId()
                    || !before.present() || !before.alive()
                    || !after.present() || !after.alive()
                    || !after.template().doppel()
                    || before.x() != after.x() || before.y() != after.y()
                    || after.passispeed() != 640 || after.atkspeed() != 900) {
                throw new IllegalArgumentException(
                        "invalid classic monster doppel transformation");
            }
        }
    }

    /**
     * One retained ownerless {@code L1NpcInstance.noTarget} movement tick.
     * {@code moved} is the one-tile state broadcast by {@code setDirectionMove};
     * {@code after} differs only when movement_distance immediately leashes the
     * actor back to its actual spawn landing.
     */
    record IdleMovement(Snapshot before, Snapshot moved, Snapshot after) {
        IdleMovement {
            if (before == null || moved == null || after == null
                    || before.objectId() != moved.objectId()
                    || before.objectId() != after.objectId()
                    || before.mapId() != moved.mapId()
                    || before.mapId() != after.mapId()
                    || Math.max(Math.abs(before.x() - moved.x()),
                        Math.abs(before.y() - moved.y())) != 1) {
                throw new IllegalArgumentException(
                        "invalid classic monster idle movement");
            }
        }

        boolean teleportedHome() {
            return moved.x() != after.x() || moved.y() != after.y();
        }
    }

    /** Native noTarget off-screen return to the actor's actual spawn landing. */
    record HomeReset(Snapshot before, Snapshot after) {
        HomeReset {
            if (before == null || after == null
                    || before.objectId() != after.objectId()
                    || before.mapId() != after.mapId()
                    || before.x() == after.x() && before.y() == after.y()) {
                throw new IllegalArgumentException(
                        "invalid classic monster home reset");
            }
        }
    }

    /** One retained L1Guard noTarget step or its path-failure home teleport. */
    record GuardHomecoming(Snapshot before, Snapshot after, boolean teleported) {
        GuardHomecoming {
            if (before == null || after == null
                    || before.objectId() != after.objectId()
                    || before.mapId() != after.mapId()
                    || !ClassicWorldGuardCatalog.isGuard(before)
                    || !ClassicWorldGuardCatalog.isGuard(after)
                    || before.x() == after.x() && before.y() == after.y()
                    || !teleported && Math.max(
                            Math.abs(before.x() - after.x()),
                            Math.abs(before.y() - after.y())) != 1) {
                throw new IllegalArgumentException("invalid L1Guard homecoming");
            }
        }
    }

    record Attack(Snapshot attacker, long targetObjectId,
                  int targetX, int targetY, boolean projectile) {
        Attack {
            if (attacker == null || !attacker.present() || !attacker.alive()
                    || targetObjectId <= 0 || targetX <= 0 || targetY <= 0
                    || projectile != attacker.template().projectileAtDistance(
                            Math.max(Math.abs(attacker.x() - targetX),
                                Math.abs(attacker.y() - targetY)))) {
                throw new IllegalArgumentException("invalid classic monster attack");
            }
        }
    }

    record MagicAttack(
            Snapshot attacker, long targetObjectId, int targetX, int targetY,
            ClassicWorldMonsterMagicSkillCatalog.Definition skill,
            int triggerRoll, boolean triggerChanceApplied) {
        MagicAttack {
            if (attacker == null || !attacker.present() || !attacker.alive()
                    || targetObjectId <= 0 || targetX <= 0 || targetY <= 0
                    || skill == null || skill.npcId() != attacker.npcId()
                    || (triggerChanceApplied
                        ? triggerRoll < 1 || triggerRoll > 100
                        : triggerRoll != 0)) {
                throw new IllegalArgumentException("invalid classic monster magic attack");
            }
        }
    }

    record PhysicalSkillAttack(
            Snapshot attacker, long targetObjectId, int targetX, int targetY,
            ClassicWorldMonsterPhysicalSkillCatalog.Definition skill,
            int triggerRoll, boolean triggerChanceApplied) {
        PhysicalSkillAttack {
            if (attacker == null || !attacker.present() || !attacker.alive()
                    || targetObjectId <= 0 || targetX <= 0 || targetY <= 0
                    || skill == null || skill.npcId() != attacker.npcId()
                    || (triggerChanceApplied
                        ? triggerRoll < 1 || triggerRoll > 100
                        : triggerRoll != 0)) {
                throw new IllegalArgumentException(
                        "invalid classic monster physical skill attack");
            }
        }
    }

    record StatusSkillAttack(
            Snapshot attacker, long targetObjectId, int targetX, int targetY,
            ClassicWorldMonsterStatusSkillCatalog.Definition skill,
            int triggerRoll, boolean triggerChanceApplied) {
        StatusSkillAttack {
            if (attacker == null || !attacker.present() || !attacker.alive()
                    || targetObjectId <= 0 || targetX <= 0 || targetY <= 0
                    || skill == null || skill.npcId() != attacker.npcId()
                    || (triggerChanceApplied
                        ? triggerRoll < 1 || triggerRoll > 100
                        : triggerRoll != 0)) {
                throw new IllegalArgumentException(
                        "invalid classic monster status skill attack");
            }
        }
    }

    /** One retained CKEW mobskill choice; exactly one Type1/Type2 branch wins. */
    record CkewSkillAttack(
            PhysicalSkillAttack physical, MagicAttack magic,
            StatusSkillAttack status) {
        CkewSkillAttack {
            int branches = (physical == null ? 0 : 1)
                    + (magic == null ? 0 : 1)
                    + (status == null ? 0 : 1);
            if (branches != 1) {
                throw new IllegalArgumentException(
                        "classic CKEW skill choice must contain exactly one attack");
            }
        }
    }

    private record Cell(int mapId, int x, int y) {}
    private record ContributorKey(ContributorKind kind, long sourceObjectId,
                                  long playerObjectId) {}

    private static final class Actor {
        boolean nativeTamedDropInitialized;
        long nativeTamedRespawnGeneration;
        final ClassicWorldMonsterCatalog.SpawnDefinition definition;
        final int spawnNumber;
        final long objectId;
        final Random random;
        final boolean oneShot;
        RuntimeInventory releasedItems = new RuntimeInventory();
        final Map<Long, Integer> digestSeconds = new LinkedHashMap<>();
        final List<Long> targetItemObjectIds = new ArrayList<>();
        int x;
        int y;
        int homeX;
        int homeY;
        int heading;
        int currentHp;
        int currentMp;
        boolean alive;
        boolean present;
        boolean actived;
        int hiddenStatus;
        int actorStatus;
        long targetObjectId;
        int pathFailCount;
        long targetItemObjectId;
        long pendingFamilyTargetObjectId;
        long mobGroupLeaderObjectId;
        boolean mobGroupLeader;
        boolean mobGroupMinion;
        boolean removeMobGroupIfLeaderDies;
        long nextActionNanos;
        final Map<Integer, Long> skillReadyNanos = new LinkedHashMap<>();
        final Map<Integer, Integer> physicalSkillUseCount = new LinkedHashMap<>();
        /** Retained L1NpcInstance _hateList, keyed by the actual target object. */
        final Map<Long, Long> combatHate = new LinkedHashMap<>();
        /** Retained _dropHateList projection used by reward distribution. */
        final Map<ContributorKey, Long> contributionHate = new LinkedHashMap<>();
        int weaponBreakStacks;
        long hasteExpiresAtNanos;
        long slowExpiresAtNanos;
        long areaSilenceExpiresAtNanos;
        long windShackleExpiresAtNanos;
        long polluteWaterExpiresAtNanos;
        long strikerGaleExpiresAtNanos;
        long armorBreakExpiresAtNanos;
        final BattleTimedStatus damagePoisonStatus = new BattleTimedStatus();
        long poisonAttackerObjectId;
        long poisonPlayerObjectId;
        ContributorKind poisonContributorKind;
        ClassicWorldMonsterPoisonAttack.Kind poisonKind =
                ClassicWorldMonsterPoisonAttack.Kind.NONE;
        final BattleElementalFallDownStatus elementalFallDownStatus =
                new BattleElementalFallDownStatus();
        final BattleEraseMagicStatus eraseMagicStatus =
                new BattleEraseMagicStatus();
        final BattleArmBreakerStatus armBreakerStatus =
                new BattleArmBreakerStatus();
        final BattlePanicStatus panicStatus = new BattlePanicStatus();
        final BattleJoyOfPainStatus joyOfPainStatus =
                new BattleJoyOfPainStatus();
        boolean firstAttack;
        /** Object-lifetime retained flag; the source does not reset it on respawn. */
        boolean firstFound = true;
        int randomMoveDistance;
        int randomMoveDirection;
        int runtimeGfxId;
        String runtimeNameId;
        String runtimeTitle;
        int runtimeLawful;
        int runtimePassispeed;
        int runtimeAtkspeed;

        Actor(ClassicWorldMonsterCatalog.SpawnDefinition definition,
              int spawnNumber, long objectId, Random random) {
            this(definition, spawnNumber, objectId, random, false, List.of());
        }

        Actor(ClassicWorldMonsterCatalog.SpawnDefinition definition,
              int spawnNumber, long objectId, Random random,
              boolean oneShot, List<RuntimeInventory.Item> carriedItems) {
            this.definition = definition;
            this.spawnNumber = spawnNumber;
            this.objectId = objectId;
            this.random = random;
            this.oneShot = oneShot;
            heading = definition.heading();
            currentHp = definition.template().hp();
            currentMp = definition.template().mp();
            alive = true;
            present = true;
            resetRuntimeAppearance();
            resetInitialHiddenState();
            if (carriedItems != null) {
                for (RuntimeInventory.Item item : carriedItems) {
                    if (item == null || !releasedItems.add(item, item.quantity())) {
                        throw new IllegalArgumentException(
                                "released pet inventory exceeds native capacity");
                    }
                }
            }
        }

        Snapshot snapshot() {
            return NativeCombatOrigin.worldSnapshot(new Snapshot(this.definition.mapId(), this.definition.spawnId(), this.spawnNumber, this.objectId, this.definition.template().npcId(), this.x, this.y, this.heading, this.currentHp, this.definition.template().hp(), this.currentMp, this.definition.template().mp(), this.alive, this.present, this.hiddenStatus, this.actorStatus, this.definition, this.runtimeGfxId, this.runtimeNameId, this.runtimeTitle, this.runtimeLawful, this.runtimePassispeed, this.runtimeAtkspeed), this);
        }

        void resetRuntimeAppearance() {
            runtimeGfxId = definition.template().gfxId();
            runtimeNameId = definition.template().nameId();
            runtimeTitle = "";
            runtimeLawful = definition.template().lawful();
            runtimePassispeed = definition.template().passispeed();
            runtimeAtkspeed = definition.template().atkspeed();
        }

        void resetInitialHiddenState() {
            hiddenStatus = HIDDEN_STATUS_NONE;
            actorStatus = 0;
            if (!oneShot && initialSinkStatus(
                    definition.template().npcId(), spawnNumber)) {
                hiddenStatus = HIDDEN_STATUS_SINK;
                actorStatus = sinkActorStatus(definition.template().npcId());
            } else if (!oneShot && initialFlyStatus(
                    definition.template().npcId())) {
                hiddenStatus = HIDDEN_STATUS_FLY;
                actorStatus = 4;
            } else if (!oneShot && initialIceStatus(
                    definition.template().npcId())) {
                hiddenStatus = HIDDEN_STATUS_ICE;
                actorStatus = 4;
            }
        }
    }

    private final Map<Long, Actor> actors = new LinkedHashMap<>();
    private final Map<Cell, Integer> occupied = new LinkedHashMap<>();
    /** Sparse native noTarget off-screen return candidates; avoids a world scan. */
    private final Set<Long> wanderedActors = new LinkedHashSet<>();
    /** Sparse actors whose retained NpcAI lifetime has entered onTarget. */
    private final Set<Long> activatedActors = new LinkedHashSet<>();
    /** Retained NpcDigestItemTimer ACTIVE projection; no full population scan. */
    private final Set<Long> digestActors = new LinkedHashSet<>();
    private long releasedPetObjectBase = RELEASED_PET_OBJECT_BASE;
    private long releasedPetObjectEnd = 0x65FF_FFFFL;
    private long nextReleasedPetObjectId = RELEASED_PET_OBJECT_BASE;
    private long nextQuestDynamicObjectId = QUEST_DYNAMIC_OBJECT_BASE;
    private long questDynamicObjectEnd = QUEST_DYNAMIC_OBJECT_END;
    private long nextMobGroupDynamicObjectId = MOB_GROUP_DYNAMIC_OBJECT_BASE;
    private boolean staticGuardianAugmented;
    private boolean staticGuardAugmented;
    private int mobGroupMinionCount;

    ClassicWorldMonsterPopulation() {
        this(DEFAULT_SEED);
    }

    ClassicWorldMonsterPopulation(long seed) {
        int ordinal = 0;
        for (int mapId : ClassicWorldMapCatalog.supportedMapIds()) {
            // 0.7.332 maps are deliberately appended below the already-shipped
            // trial actors so every 0.7.331 object id remains unchanged.
            if (mapId == ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_FIRST_MAP_ID
                    || mapId == ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_SECOND_MAP_ID
                    || mapId == ClassicWorldMapCatalog.UNDERSEA_TUNNEL_MAP_ID
                    || mapId == ClassicWorldMapCatalog.UNDERSEA_TUNNEL_SEVENTH_FLOOR_MAP_ID
                    || ClassicWorldMapCatalog.GLUDIO_DUNGEON_MAP_IDS.contains(mapId)
                    || ClassicWorldMapCatalog.GIRAN_DUNGEON_MAP_IDS.contains(mapId)
                    || ClassicWorldMapCatalog.WINDAWOOD_CASTLE_MAP_IDS.contains(mapId)
                    || ClassicWorldMapCatalog.KENT_COMPONENT_MAP_IDS.contains(mapId)
                    || ClassicWorldMapCatalog.IVORY_TOWER_MAP_IDS.contains(mapId)
                    || ClassicWorldMapCatalog.ADEN_TOI_MAP_IDS.contains(mapId)
                    || ClassicWorldMapCatalog.HEINE_EVA_MAP_IDS.contains(mapId)
                    || ClassicWorldMapCatalog.SILENT_CAVERN_MAP_IDS.contains(mapId)
                    || ClassicWorldMapCatalog.RASTABAD_OUM_MAP_IDS.contains(mapId)
                    || ClassicWorldMapCatalog.DIADRAD_SERVICE_CORE_MAP_IDS.contains(mapId)
                    || mapId == ClassicWorldMapCatalog.DEMON_TEMPLE_MAP_ID
                    || ClassicWorldMapCatalog.ELEMENTAL_GRAVE_MAP_IDS.contains(mapId)
                    || ClassicWorldMapCatalog.PIRATE_DUNGEON_MAP_IDS.contains(mapId)
                    || ClassicWorldMapCatalog.PIRATE_BACK_HALF_MAP_IDS.contains(mapId)
                    || ClassicWorldDesireCaveCatalog.supports(mapId)
                    || mapId == ClassicWorldHell.MAP_ID) {
                continue;
            }
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                long definitionSeed = mix64(seed
                        ^ (mapId == ClassicWorldMapCatalog.WORLD_TREE_MAP_ID
                            ? 0L : Integer.toUnsignedLong(mapId)
                                * 0xD6E8FEB86659FD93L)
                        ^ Integer.toUnsignedLong(definition.spawnId())
                            * 0x9E3779B97F4A7C15L);
                for (int spawnNumber = 1;
                     spawnNumber <= definition.count(); spawnNumber++) {
                    long objectId = OBJECT_BASE | ++ordinal;
                    Actor actor = new Actor(definition, spawnNumber, objectId,
                            new Random(mix64(definitionSeed ^ spawnNumber)));
                    if (!place(actor)) {
                        throw new ExceptionInInitializerError(
                                "cannot place classic monster map=" + mapId
                                        + " spawn=" + definition.spawnId()
                                        + " slot=" + spawnNumber);
                    }
                    actors.put(objectId, actor);
                }
            }
        }
        // Append the restored trial actors only after every selected-source
        // actor so all previously proven native object IDs remain stable.
        for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                : ClassicWorldRoyalTrialCatalog.definitions(
                        ClassicWorldMapCatalog.WORLD_TREE_MAP_ID)) {
            long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(definition.mapId())
                        * 0xD6E8FEB86659FD93L
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
            for (int spawnNumber = 1;
                 spawnNumber <= definition.count(); spawnNumber++) {
                long objectId = OBJECT_BASE | ++ordinal;
                Actor actor = new Actor(definition, spawnNumber, objectId,
                        new Random(mix64(definitionSeed ^ spawnNumber)));
                if (!place(actor)) {
                    throw new ExceptionInInitializerError(
                            "cannot place restored royal-trial monster map="
                                    + definition.mapId() + " spawn="
                                    + definition.spawnId() + " slot=" + spawnNumber);
                }
                actors.put(objectId, actor);
            }
        }
        // Append the selected map1/map2/map14 populations only after the 0.7.331
        // black-knight slice, preserving both older selected and restored IDs.
        for (int mapId : List.of(
                ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_FIRST_MAP_ID,
                ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_SECOND_MAP_ID,
                ClassicWorldMapCatalog.UNDERSEA_TUNNEL_MAP_ID)) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                long definitionSeed = mix64(seed
                        ^ Integer.toUnsignedLong(mapId) * 0xD6E8FEB86659FD93L
                        ^ Integer.toUnsignedLong(definition.spawnId())
                            * 0x9E3779B97F4A7C15L);
                for (int spawnNumber = 1;
                     spawnNumber <= definition.count(); spawnNumber++) {
                    long objectId = OBJECT_BASE | ++ordinal;
                    Actor actor = new Actor(definition, spawnNumber, objectId,
                            new Random(mix64(definitionSeed ^ spawnNumber)));
                    if (!place(actor)) {
                        throw new ExceptionInInitializerError(
                                "cannot place selected royal-trial cave monster map="
                                        + mapId + " spawn=" + definition.spawnId()
                                        + " slot=" + spawnNumber);
                    }
                    actors.put(objectId, actor);
                }
            }
        }
        // Public classic spawn 200009 restores the ten quest providers only
        // after every selected-source actor, so selected ordinals stay stable.
        for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                : ClassicWorldRoyalTrialCatalog.definitions(
                        ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_SECOND_MAP_ID)) {
            long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(definition.mapId())
                        * 0xD6E8FEB86659FD93L
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
            for (int spawnNumber = 1;
                 spawnNumber <= definition.count(); spawnNumber++) {
                long objectId = OBJECT_BASE | ++ordinal;
                Actor actor = new Actor(definition, spawnNumber, objectId,
                        new Random(mix64(definitionSeed ^ spawnNumber)));
                if (!place(actor)) {
                    throw new ExceptionInInitializerError(
                            "cannot place restored stone golem map="
                                    + definition.mapId() + " spawn="
                                    + definition.spawnId() + " slot=" + spawnNumber);
                }
                actors.put(objectId, actor);
            }
        }
        // Append selected map13 only after all 0.7.332 selected/restored actors.
        // This preserves the exact object IDs proven by the 0.7.332 TCP capture.
        for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                : ClassicWorldMonsterCatalog.definitions(
                        ClassicWorldMapCatalog.UNDERSEA_TUNNEL_SEVENTH_FLOOR_MAP_ID)) {
            long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(definition.mapId())
                        * 0xD6E8FEB86659FD93L
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
            for (int spawnNumber = 1;
                 spawnNumber <= definition.count(); spawnNumber++) {
                long objectId = OBJECT_BASE | ++ordinal;
                Actor actor = new Actor(definition, spawnNumber, objectId,
                        new Random(mix64(definitionSeed ^ spawnNumber)));
                if (!place(actor)) {
                    throw new ExceptionInInitializerError(
                            "cannot place selected map13 monster spawn="
                                    + definition.spawnId() + " slot=" + spawnNumber);
                }
                actors.put(objectId, actor);
            }
        }
        // Append the complete selected Gludio Dungeon component only after
        // every 0.7.334 actor so all previously captured object IDs remain stable.
        for (int mapId : ClassicWorldMapCatalog.GLUDIO_DUNGEON_MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                long definitionSeed = mix64(seed
                        ^ Integer.toUnsignedLong(mapId) * 0xD6E8FEB86659FD93L
                        ^ Integer.toUnsignedLong(definition.spawnId())
                            * 0x9E3779B97F4A7C15L);
                for (int spawnNumber = 1;
                     spawnNumber <= definition.count(); spawnNumber++) {
                    long objectId = OBJECT_BASE | ++ordinal;
                    Actor actor = new Actor(definition, spawnNumber, objectId,
                            new Random(mix64(definitionSeed ^ spawnNumber)));
                    if (!place(actor)) {
                        throw new ExceptionInInitializerError(
                                "cannot place selected Gludio Dungeon monster map="
                                        + mapId + " spawn=" + definition.spawnId()
                                        + " slot=" + spawnNumber);
                    }
                    actors.put(objectId, actor);
                }
            }
        }
        // Append the selected Giran Dungeon component only after all 0.7.335
        // actors so every previously captured object ID remains stable.
        for (int mapId : ClassicWorldMapCatalog.GIRAN_DUNGEON_MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                long definitionSeed = mix64(seed
                        ^ Integer.toUnsignedLong(mapId) * 0xD6E8FEB86659FD93L
                        ^ Integer.toUnsignedLong(definition.spawnId())
                            * 0x9E3779B97F4A7C15L);
                for (int spawnNumber = 1;
                     spawnNumber <= definition.count(); spawnNumber++) {
                    long objectId = OBJECT_BASE | ++ordinal;
                    Actor actor = new Actor(definition, spawnNumber, objectId,
                            new Random(mix64(definitionSeed ^ spawnNumber)));
                    if (!place(actor)) {
                        throw new ExceptionInInitializerError(
                                "cannot place selected Giran Dungeon monster map="
                                        + mapId + " spawn=" + definition.spawnId()
                                        + " slot=" + spawnNumber);
                    }
                    actors.put(objectId, actor);
                }
            }
        }
        // Append Windawood maps23/24/29 only after every 0.7.337 actor.
        for (int mapId : ClassicWorldMapCatalog.WINDAWOOD_CASTLE_MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(mapId) * 0xD6E8FEB86659FD93L
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
                for (int spawnNumber = 1;
                     spawnNumber <= definition.count(); spawnNumber++) {
                    long objectId = OBJECT_BASE | ++ordinal;
                    Actor actor = new Actor(definition, spawnNumber, objectId,
                            new Random(mix64(definitionSeed ^ spawnNumber)));
                    if (!place(actor)) {
                        throw new ExceptionInInitializerError(
                                "cannot place selected Windawood monster map="
                                        + mapId + " spawn=" + definition.spawnId()
                                        + " slot=" + spawnNumber);
                    }
                    actors.put(objectId, actor);
                }
            }
        }
        // Append Kent maps15/240-243 only after every 0.7.338 actor.
        for (int mapId : ClassicWorldMapCatalog.KENT_COMPONENT_MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(mapId) * 0xD6E8FEB86659FD93L
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
                for (int spawnNumber = 1;
                     spawnNumber <= definition.count(); spawnNumber++) {
                    long objectId = OBJECT_BASE | ++ordinal;
                    Actor actor = new Actor(definition, spawnNumber, objectId,
                            new Random(mix64(definitionSeed ^ spawnNumber)));
                    if (!place(actor)) {
                        throw new ExceptionInInitializerError(
                                "cannot place selected Kent monster map="
                                        + mapId + " spawn=" + definition.spawnId()
                                        + " slot=" + spawnNumber);
                    }
                    actors.put(objectId, actor);
                }
            }
        }
        // Append Ivory Tower maps75-82 only after every 0.7.339 actor.
        for (int mapId : ClassicWorldMapCatalog.IVORY_TOWER_MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(mapId) * 0xD6E8FEB86659FD93L
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
                for (int spawnNumber = 1;
                     spawnNumber <= definition.count(); spawnNumber++) {
                    long objectId = OBJECT_BASE | ++ordinal;
                    Actor actor = new Actor(definition, spawnNumber, objectId,
                            new Random(mix64(definitionSeed ^ spawnNumber)));
                    if (!place(actor)) {
                        throw new ExceptionInInitializerError(
                                "cannot place selected Ivory-Tower monster map="
                                        + mapId + " spawn=" + definition.spawnId()
                                        + " slot=" + spawnNumber);
                    }
                    actors.put(objectId, actor);
                }
            }
        }
        // Append Aden Castle, TOI 1-10F and waterway only after every .341
        // actor, preserving all object IDs already proved by prior captures.
        for (int mapId : ClassicWorldMapCatalog.ADEN_TOI_MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(mapId) * 0xD6E8FEB86659FD93L
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
                for (int spawnNumber = 1;
                     spawnNumber <= definition.count(); spawnNumber++) {
                    long objectId = OBJECT_BASE | ++ordinal;
                    Actor actor = new Actor(definition, spawnNumber, objectId,
                            new Random(mix64(definitionSeed ^ spawnNumber)));
                    if (!place(actor)) {
                        throw new ExceptionInInitializerError(
                                "cannot place selected Aden/TOI monster map="
                                        + mapId + " spawn=" + definition.spawnId()
                                        + " slot=" + spawnNumber);
                    }
                    actors.put(objectId, actor);
                }
            }
        }
        // Append Heine/Eva only after every .342 actor; this keeps all
        // previously captured S_PUT_OBJECT ids stable.
        for (int mapId : ClassicWorldMapCatalog.HEINE_EVA_MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(mapId) * 0xD6E8FEB86659FD93L
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
                for (int spawnNumber = 1;
                     spawnNumber <= definition.count(); spawnNumber++) {
                    long objectId = OBJECT_BASE | ++ordinal;
                    Actor actor = new Actor(definition, spawnNumber, objectId,
                            new Random(mix64(definitionSeed ^ spawnNumber)));
                    if (!place(actor)) {
                        throw new ExceptionInInitializerError(
                                "cannot place selected Heine/Eva monster map="
                                        + mapId + " spawn=" + definition.spawnId()
                                        + " slot=" + spawnNumber);
                    }
                    actors.put(objectId, actor);
                }
            }
        }
        // Append Silent Cavern / Bludika Cave only after every .343 actor;
        // previously validated native S_PUT_OBJECT ids therefore stay stable.
        for (int mapId : ClassicWorldMapCatalog.SILENT_CAVERN_MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(mapId) * 0xD6E8FEB86659FD93L
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
                for (int spawnNumber = 1;
                     spawnNumber <= definition.count(); spawnNumber++) {
                    long objectId = OBJECT_BASE | ++ordinal;
                    Actor actor = new Actor(definition, spawnNumber, objectId,
                            new Random(mix64(definitionSeed ^ spawnNumber)));
                    if (!place(actor)) {
                        throw new ExceptionInInitializerError(
                                "cannot place selected Silent-Cavern monster map="
                                        + mapId + " spawn=" + definition.spawnId()
                                        + " slot=" + spawnNumber);
                    }
                    actors.put(objectId, actor);
                }
            }
        }
        // Append Rastabad/Oum only after every .344 actor, preserving every
        // previously validated native S_PUT_OBJECT object id.
        for (int mapId : ClassicWorldMapCatalog.RASTABAD_OUM_MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(mapId) * 0xD6E8FEB86659FD93L
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
                for (int spawnNumber = 1;
                     spawnNumber <= definition.count(); spawnNumber++) {
                    long objectId = OBJECT_BASE | ++ordinal;
                    Actor actor = new Actor(definition, spawnNumber, objectId,
                            new Random(mix64(definitionSeed ^ spawnNumber)));
                    if (!place(actor)) {
                        throw new ExceptionInInitializerError(
                                "cannot place selected Rastabad/Oum monster map="
                                        + mapId + " spawn=" + definition.spawnId()
                                        + " slot=" + spawnNumber);
                    }
                    actors.put(objectId, actor);
                }
            }
        }
        // Append Diad service-core actors only after every .345 actor so the
        // immutable release keeps all previously proved native object ids.
        for (int mapId : ClassicWorldMapCatalog.DIADRAD_SERVICE_CORE_MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(mapId) * 0xD6E8FEB86659FD93L
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
                for (int spawnNumber = 1;
                     spawnNumber <= definition.count(); spawnNumber++) {
                    long objectId = OBJECT_BASE | ++ordinal;
                    Actor actor = new Actor(definition, spawnNumber, objectId,
                            new Random(mix64(definitionSeed ^ spawnNumber)));
                    if (!place(actor)) {
                        throw new ExceptionInInitializerError(
                                "cannot place selected Diad service-core monster map="
                                        + mapId + " spawn=" + definition.spawnId()
                                        + " slot=" + spawnNumber);
                    }
                    actors.put(objectId, actor);
                }
            }
        }
        // Append Demon Temple only after every .346 actor so all immutable
        // release object ids remain stable.
        for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                : ClassicWorldMonsterCatalog.definitions(
                        ClassicWorldDemonTempleCatalog.MAP_ID)) {
            long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(ClassicWorldDemonTempleCatalog.MAP_ID)
                        * 0xD6E8FEB86659FD93L
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
            for (int spawnNumber = 1;
                 spawnNumber <= definition.count(); spawnNumber++) {
                long objectId = OBJECT_BASE | ++ordinal;
                Actor actor = new Actor(definition, spawnNumber, objectId,
                        new Random(mix64(definitionSeed ^ spawnNumber)));
                if (!place(actor)) {
                    throw new ExceptionInInitializerError(
                            "cannot place selected Demon Temple monster map="
                                    + ClassicWorldDemonTempleCatalog.MAP_ID
                                    + " spawn=" + definition.spawnId()
                                    + " slot=" + spawnNumber);
                }
                actors.put(objectId, actor);
            }
        }
        // Append Abyss Lake, Elemental Grave, and the Pirate Island landing only
        // after every .375 actor.  Keeping this component out of the generic loop
        // preserves every previously captured S_PUT_OBJECT object id.
        for (int mapId : ClassicWorldMapCatalog.ELEMENTAL_GRAVE_MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(mapId) * 0xD6E8FEB86659FD93L
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
                for (int spawnNumber = 1;
                     spawnNumber <= definition.count(); spawnNumber++) {
                    long objectId = OBJECT_BASE | ++ordinal;
                    Actor actor = new Actor(definition, spawnNumber, objectId,
                            new Random(mix64(definitionSeed ^ spawnNumber)));
                    if (!place(actor)) {
                        throw new ExceptionInInitializerError(
                                "cannot place selected Elemental-Grave slice monster map="
                                        + mapId + " spawn=" + definition.spawnId()
                                        + " slot=" + spawnNumber);
                    }
                    actors.put(objectId, actor);
                }
            }
        }
        // Append Pirate Island dungeon 1F-4F after every .376 actor so all
        // previously captured S_PUT_OBJECT object ids remain stable.
        for (int mapId : ClassicWorldMapCatalog.PIRATE_DUNGEON_MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(mapId) * 0xD6E8FEB86659FD93L
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
                for (int spawnNumber = 1;
                     spawnNumber <= definition.count(); spawnNumber++) {
                    long objectId = OBJECT_BASE | ++ordinal;
                    Actor actor = new Actor(definition, spawnNumber, objectId,
                            new Random(mix64(definitionSeed ^ spawnNumber)));
                    if (!place(actor)) {
                        throw new ExceptionInInitializerError(
                                "cannot place selected Pirate-dungeon monster map="
                                        + mapId + " spawn=" + definition.spawnId()
                                        + " slot=" + spawnNumber);
                    }
                    actors.put(objectId, actor);
                }
            }
        }
        // Append the 0.7.378 Pirate back-half and Ship Grave actors last so
        // every object id already sealed by 0.7.377 remains byte-for-byte stable.
        for (int mapId : ClassicWorldMapCatalog.PIRATE_BACK_HALF_MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(mapId) * 0xD6E8FEB86659FD93L
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
                for (int spawnNumber = 1;
                     spawnNumber <= definition.count(); spawnNumber++) {
                    long objectId = OBJECT_BASE | ++ordinal;
                    Actor actor = new Actor(definition, spawnNumber, objectId,
                            new Random(mix64(definitionSeed ^ spawnNumber)));
                    if (!place(actor)) {
                        throw new ExceptionInInitializerError(
                                "cannot place selected Pirate-back-half monster map="
                                        + mapId + " spawn=" + definition.spawnId()
                                        + " slot=" + spawnNumber);
                    }
                    actors.put(objectId, actor);
                }
            }
        }
        // Append Desire Cave / Abandoned Land after every actor sealed by
        // 0.7.381.  This is intentionally outside the generic loop: inserting
        // these 583 actors earlier would renumber every previously captured
        // S_PUT_OBJECT identity.
        for (int mapId : ClassicWorldDesireCaveCatalog.MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(mapId) * 0xD6E8FEB86659FD93L
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
                for (int spawnNumber = 1;
                     spawnNumber <= definition.count(); spawnNumber++) {
                    long objectId = OBJECT_BASE | ++ordinal;
                    Actor actor = new Actor(definition, spawnNumber, objectId,
                            new Random(mix64(definitionSeed ^ spawnNumber)));
                    if (!place(actor)) {
                        throw new ExceptionInInitializerError(
                                "cannot place selected Desire-Cave monster map="
                                        + mapId + " spawn=" + definition.spawnId()
                                        + " slot=" + spawnNumber);
                    }
                    actors.put(objectId, actor);
                }
            }
        }
        if (actors.size() != ClassicWorldMonsterCatalog.TOTAL_POPULATION
                - ClassicWorldMonsterCatalog.MAP666_POPULATION) {
            throw new ExceptionInInitializerError(
                    "pre-Hell classic monster population changed actual=" + actors.size()
                            + " expected="
                            + (ClassicWorldMonsterCatalog.TOTAL_POPULATION
                                - ClassicWorldMonsterCatalog.MAP666_POPULATION));
        }
        int sourcePopulation = actors.size();
        ordinal = appendSelectedMobGroups(seed, ordinal);
        mobGroupMinionCount = actors.size() - sourcePopulation;
        if (mobGroupMinionCount
                != ClassicWorldMobGroupCatalog.PROJECTED_MINION_POPULATION) {
            throw new ExceptionInInitializerError(
                    "classic mob-group population changed actual="
                            + mobGroupMinionCount + " expected="
                            + ClassicWorldMobGroupCatalog.PROJECTED_MINION_POPULATION
                            + " leaders=" + selectedMobGroupLeaderCounts());
        }
        // Append map666 after every previously shipped ordinary actor and
        // mob-group minion.  This preserves all sealed 0x6500.... identities.
        for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                : ClassicWorldMonsterCatalog.definitions(ClassicWorldHell.MAP_ID)) {
            long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(ClassicWorldHell.MAP_ID)
                        * 0xD6E8FEB86659FD93L
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
            for (int spawnNumber = 1;
                 spawnNumber <= definition.count(); spawnNumber++) {
                long objectId = OBJECT_BASE | ++ordinal;
                Actor actor = new Actor(definition, spawnNumber, objectId,
                        new Random(mix64(definitionSeed ^ spawnNumber)));
                if (!place(actor)) {
                    throw new ExceptionInInitializerError(
                            "cannot place selected Hell monster map="
                                    + ClassicWorldHell.MAP_ID + " spawn="
                                    + definition.spawnId() + " slot=" + spawnNumber);
                }
                actors.put(objectId, actor);
            }
        }
        if (actors.size() != ClassicWorldMonsterCatalog.TOTAL_POPULATION
                + ClassicWorldMobGroupCatalog.PROJECTED_MINION_POPULATION) {
            throw new ExceptionInInitializerError(
                    "post-Hell classic runtime population changed actual="
                            + actors.size());
        }
        // Keep every previously captured 0x6500.... monster identity stable.
        // Selected spawnlist_npc L1Guardian rows already own stable 0x6400....
        // object ids, so they can be appended without renumbering either set.
        for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                : ClassicWorldGuardianCatalog.definitions()) {
            long objectId = ClassicWorldNpcCatalog.objectId(definition.spawnId());
            if (actors.containsKey(objectId)) {
                throw new ExceptionInInitializerError(
                        "guardian object id collides " + objectId);
            }
            long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
            Actor actor = new Actor(definition, 1, objectId,
                    new Random(definitionSeed));
            if (!place(actor)) {
                throw new ExceptionInInitializerError(
                        "cannot place selected guardian spawn="
                                + definition.spawnId());
            }
            actors.put(objectId, actor);
        }
        staticGuardianAugmented = true;
        if (actors.size() != ClassicWorldMonsterCatalog.TOTAL_POPULATION
                + ClassicWorldMobGroupCatalog.PROJECTED_MINION_POPULATION
                + ClassicWorldGuardianCatalog.POPULATION) {
            throw new ExceptionInInitializerError(
                    "classic runtime population changed actual=" + actors.size());
        }
        // Append every live L1Guard row only after the sealed Guardian range.
        // Both special families retain their selected spawnlist_npc 0x6400....
        // identities and therefore never renumber ordinary 0x6500.... actors.
        for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                : ClassicWorldGuardCatalog.definitions()) {
            long objectId = ClassicWorldNpcCatalog.objectId(definition.spawnId());
            if (actors.containsKey(objectId)) {
                throw new ExceptionInInitializerError(
                        "guard object id collides " + objectId);
            }
            long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
            Actor actor = new Actor(definition, 1, objectId,
                    new Random(definitionSeed));
            if (!place(actor)) {
                throw new ExceptionInInitializerError(
                        "cannot place selected guard spawn="
                                + definition.spawnId());
            }
            actors.put(objectId, actor);
        }
        staticGuardAugmented = true;
        if (actors.size() != ClassicWorldMonsterCatalog.TOTAL_POPULATION
                + ClassicWorldMobGroupCatalog.PROJECTED_MINION_POPULATION
                + ClassicWorldGuardianCatalog.POPULATION
                + ClassicWorldGuardCatalog.POPULATION) {
            throw new ExceptionInInitializerError(
                    "classic live-guard runtime population changed actual="
                            + actors.size());
        }
    }

    /**
     * Retained {@code L1MobGroupSpawn}: each selected spawn slot is the leader
     * of an independent runtime group; its minions are appended after all
     * previously sealed ordinary object ids and never receive a respawn slot.
     */
    private int appendSelectedMobGroups(long seed, int ordinal) {
        List<Actor> leaders = actors.values().stream()
                .filter(actor -> actor.definition.groupId() > 0)
                .toList();
        for (Actor leader : leaders) {
            ClassicWorldMobGroupCatalog.Group group =
                    ClassicWorldMobGroupCatalog.forId(leader.definition.groupId());
            if (group == null
                    || group.leaderNpcId() != leader.definition.template().npcId()) {
                throw new ExceptionInInitializerError(
                        "missing selected mob group " + leader.definition.groupId()
                                + " for NPC " + leader.definition.template().npcId());
            }
            leader.mobGroupLeaderObjectId = leader.objectId;
            leader.mobGroupLeader = true;
            leader.removeMobGroupIfLeaderDies = group.removeIfLeaderDies();
            int memberNumber = 0;
            for (ClassicWorldMobGroupCatalog.Minion minion : group.minions()) {
                for (int index = 0; index < minion.count(); index++) {
                    long objectId = OBJECT_BASE | ++ordinal;
                    if (actors.containsKey(objectId)) {
                        throw new ExceptionInInitializerError(
                                "mob-group object id collides " + objectId);
                    }
                    ClassicWorldMonsterCatalog.SpawnDefinition definition =
                            mobGroupMinionDefinition(leader, minion.template());
                    long memberSeed = mix64(seed ^ leader.objectId
                            ^ Integer.toUnsignedLong(minion.npcId())
                                * 0x9E3779B97F4A7C15L
                            ^ ++memberNumber);
                    Actor actor = new Actor(definition, memberNumber, objectId,
                            new Random(memberSeed), true, List.of());
                    actor.mobGroupLeaderObjectId = leader.objectId;
                    actor.mobGroupMinion = true;
                    actor.removeMobGroupIfLeaderDies = group.removeIfLeaderDies();
                    actor.hiddenStatus = leader.hiddenStatus;
                    actor.actorStatus = leader.actorStatus;
                    placeMobGroupMinion(actor, leader);
                    actors.put(objectId, actor);
                }
            }
        }
        return ordinal;
    }

    private Map<Integer, Long> selectedMobGroupLeaderCounts() {
        return actors.values().stream()
                .filter(actor -> actor.definition.groupId() > 0)
                .collect(java.util.stream.Collectors.groupingBy(
                        actor -> actor.definition.groupId(),
                        java.util.TreeMap::new,
                        java.util.stream.Collectors.counting()));
    }

    private static ClassicWorldMonsterCatalog.SpawnDefinition
            mobGroupMinionDefinition(
                    Actor leader,
                    ClassicWorldMonsterCatalog.MonsterTemplate template) {
        ClassicWorldMonsterCatalog.SpawnDefinition source = leader.definition;
        return new ClassicWorldMonsterCatalog.SpawnDefinition(
                source.mapId(), source.spawnId(),
                "mobgroup:" + source.groupId(), 1, 0,
                leader.x, leader.y, 0, 0, 0, 0, 0, 0, leader.heading,
                0, 0, true, source.movementDistance(), source.rest(), 0,
                template);
    }

    private void placeMobGroupMinion(Actor actor, Actor leader) {
        int x = leader.x + actor.random.nextInt(5) - 2;
        int y = leader.y + actor.random.nextInt(5) - 2;
        if (commitIfAvailable(actor, x, y)) return;
        if (!commitSharedIfWalkable(actor, leader.x, leader.y)) {
            throw new ExceptionInInitializerError(
                    "cannot place mob-group minion for leader=" + leader.objectId);
        }
    }

    private List<Snapshot> spawnRespawnMobGroup(Actor leader) {
        if (leader.definition.groupId() <= 0) return List.of();
        ClassicWorldMobGroupCatalog.Group group =
                ClassicWorldMobGroupCatalog.forId(leader.definition.groupId());
        if (group == null
                || group.leaderNpcId() != leader.definition.template().npcId()) {
            throw new IllegalStateException(
                    "missing respawn mob group " + leader.definition.groupId());
        }
        leader.mobGroupLeaderObjectId = leader.objectId;
        leader.mobGroupLeader = true;
        leader.removeMobGroupIfLeaderDies = group.removeIfLeaderDies();
        List<Snapshot> result = new ArrayList<>();
        int memberNumber = 0;
        for (ClassicWorldMobGroupCatalog.Minion minion : group.minions()) {
            for (int index = 0; index < minion.count(); index++) {
                long objectId = allocateMobGroupDynamicObjectId();
                ClassicWorldMonsterCatalog.SpawnDefinition definition =
                        mobGroupMinionDefinition(leader, minion.template());
                long memberSeed = mix64(leader.objectId ^ objectId
                        ^ Integer.toUnsignedLong(minion.npcId())
                            * 0x9E3779B97F4A7C15L
                        ^ ++memberNumber);
                Actor actor = new Actor(definition, memberNumber, objectId,
                        new Random(memberSeed), true, List.of());
                actor.mobGroupLeaderObjectId = leader.objectId;
                actor.mobGroupMinion = true;
                actor.removeMobGroupIfLeaderDies = group.removeIfLeaderDies();
                actor.hiddenStatus = leader.hiddenStatus;
                actor.actorStatus = leader.actorStatus;
                placeMobGroupMinion(actor, leader);
                actors.put(objectId, actor);
                mobGroupMinionCount++;
                result.add(actor.snapshot());
            }
        }
        return List.copyOf(result);
    }

    private long allocateMobGroupDynamicObjectId() {
        long capacity = MOB_GROUP_DYNAMIC_OBJECT_END
                - MOB_GROUP_DYNAMIC_OBJECT_BASE + 1;
        for (long attempt = 0; attempt < capacity; attempt++) {
            long candidate = nextMobGroupDynamicObjectId++;
            if (nextMobGroupDynamicObjectId > MOB_GROUP_DYNAMIC_OBJECT_END) {
                nextMobGroupDynamicObjectId = MOB_GROUP_DYNAMIC_OBJECT_BASE;
            }
            if (!actors.containsKey(candidate)) return candidate;
        }
        throw new IllegalStateException("mob-group dynamic object range exhausted");
    }

    /** Builds the one-shot monster population owned by one retained WorldQuest show. */
    static ClassicWorldMonsterPopulation questInstance(
            long seed, long objectBase, long releasedPetObjectBase,
            List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions) {
        if (objectBase <= 0 || objectBase > 0xFFFF_FFFFL
                || releasedPetObjectBase <= objectBase
                || releasedPetObjectBase > 0xFFFF_EFFFL
                || definitions == null || definitions.isEmpty()) {
            throw new IllegalArgumentException("invalid quest monster population range");
        }
        return new ClassicWorldMonsterPopulation(
                seed, objectBase, releasedPetObjectBase, definitions, true);
    }

    /**
     * Builds an isolated retained-source slice without making its maps part of
     * the production scheduler.  This keeps source-profile-gated populations
     * testable without merging maps or routes from another retained database.
     */
    static ClassicWorldMonsterPopulation retainedSourceSlice(
            long seed, long objectBase, long releasedPetObjectBase,
            List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions) {
        if (objectBase <= 0 || objectBase > 0xFFFF_FFFFL
                || releasedPetObjectBase <= objectBase
                || releasedPetObjectBase > 0xFFFF_EFFFL
                || definitions == null || definitions.isEmpty()) {
            throw new IllegalArgumentException(
                    "invalid retained source monster population range");
        }
        return new ClassicWorldMonsterPopulation(
                seed, objectBase, releasedPetObjectBase, definitions, false);
    }

    private ClassicWorldMonsterPopulation(
            long seed, long objectBase, long releasedPetBase,
            List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions,
            boolean oneShot) {
        this.releasedPetObjectBase = releasedPetBase;
        this.releasedPetObjectEnd = releasedPetBase + 0x0FFFL;
        this.nextReleasedPetObjectId = releasedPetBase;
        this.nextQuestDynamicObjectId = objectBase + 0x0800L;
        this.questDynamicObjectEnd = releasedPetBase - 1L;
        int ordinal = 0;
        for (ClassicWorldMonsterCatalog.SpawnDefinition definition : definitions) {
            if (definition == null) {
                throw new IllegalArgumentException("null quest monster definition");
            }
            long definitionSeed = mix64(seed
                    ^ Integer.toUnsignedLong(definition.mapId())
                        * 0xD6E8FEB86659FD93L
                    ^ Integer.toUnsignedLong(definition.spawnId())
                        * 0x9E3779B97F4A7C15L);
            for (int spawnNumber = 1;
                 spawnNumber <= definition.count(); spawnNumber++) {
                long objectId = objectBase + ++ordinal;
                if (objectId >= releasedPetBase || objectId > 0xFFFF_FFFFL) {
                    throw new IllegalArgumentException(
                            "quest monster object range exhausted");
                }
                Actor actor = new Actor(definition, spawnNumber, objectId,
                        new Random(mix64(definitionSeed ^ spawnNumber)),
                        oneShot, List.of());
                if (!place(actor)) {
                    throw new ExceptionInInitializerError(
                            "cannot place quest monster map=" + definition.mapId()
                                    + " spawn=" + definition.spawnId()
                                    + " slot=" + spawnNumber);
                }
                actors.put(objectId, actor);
            }
        }
    }

    synchronized int size() {
        // Preserve the long-standing selected spawnlist population contract;
        // dynamic L1Guardian/L1Guard rows have independently audited counts.
        int result = actors.size();
        result -= mobGroupMinionCount;
        if (staticGuardianAugmented) {
            result -= ClassicWorldGuardianCatalog.POPULATION;
        }
        if (staticGuardAugmented) {
            result -= ClassicWorldGuardCatalog.POPULATION;
        }
        return result;
    }

    synchronized int runtimeActorCount() {
        return actors.size();
    }

    synchronized List<Snapshot> inScreen(int mapId, int x, int y) {
        return actors.values().stream()
                .filter(actor -> actor.present && actor.definition.mapId() == mapId)
                .map(Actor::snapshot)
                .filter(actor -> BattleSpellCatalog.isInNativeScreen(
                        x, y, actor.x(), actor.y()))
                .toList();
    }

    /** Retained World.getVisiblePlayer(this, DISTANCE=20) AI activation range. */
    synchronized List<Snapshot> withinTileDistance(
            int mapId, int x, int y, int maximumDistance) {
        if (maximumDistance < 0) return List.of();
        return actors.values().stream()
                .filter(actor -> actor.present && actor.definition.mapId() == mapId)
                .filter(actor -> Math.max(Math.abs(x - actor.x),
                        Math.abs(y - actor.y)) <= maximumDistance)
                .map(Actor::snapshot)
                .toList();
    }

    synchronized List<Reveal> revealSinkInScreen(
            int mapId, int x, int y, long playerObjectId) {
        return revealHidden(mapId, x, y, playerObjectId,
                HIDDEN_STATUS_SINK, Integer.MAX_VALUE);
    }

    synchronized List<Reveal> revealSinkNear(
            int mapId, int x, int y, long playerObjectId) {
        return revealHidden(mapId, x, y, playerObjectId,
                HIDDEN_STATUS_SINK, 2);
    }

    synchronized List<Reveal> revealFlyNear(
            int mapId, int x, int y, long playerObjectId) {
        if (playerObjectId <= 0) return List.of();
        List<Reveal> result = new ArrayList<>();
        for (Actor actor : actors.values()) {
            if (!actor.present || !actor.alive
                    || actor.definition.mapId() != mapId
                    || actor.hiddenStatus != HIDDEN_STATUS_FLY
                    || actor.currentHp != actor.definition.template().hp()
                    || !BattleSpellCatalog.isInNativeScreen(
                            x, y, actor.x, actor.y)
                    || Math.max(Math.abs(x - actor.x), Math.abs(y - actor.y)) > 1) {
                continue;
            }
            result.add(revealActor(actor, playerObjectId));
        }
        return List.copyOf(result);
    }

    /** Damaged airborne actors whose retained approachPlayer branch can run. */
    synchronized List<Snapshot> damagedFlyingInScreen(int mapId, int x, int y) {
        return actors.values().stream()
                .filter(actor -> actor.present && actor.alive
                        && actor.definition.mapId() == mapId
                        && actor.hiddenStatus == HIDDEN_STATUS_FLY
                        && actor.currentHp < actor.definition.template().hp())
                .map(Actor::snapshot)
                .filter(actor -> BattleSpellCatalog.isInNativeScreen(
                        x, y, actor.x(), actor.y()))
                .toList();
    }

    synchronized AirItemLanding searchItemFromAir(
            int mapId, long objectId,
            List<ClassicWorldGroundItems.Item> visibleGroundItems) {
        return searchItemFromAir(
                mapId, objectId, visibleGroundItems, -1);
    }

    /**
     * Exact retained {@code searchItemFromAir}: choose one ground inventory,
     * accept only etcitem type 6/7, land, rebuild and queue the first item.
     */
    synchronized AirItemLanding searchItemFromAir(
            int mapId, long objectId,
            List<ClassicWorldGroundItems.Item> visibleGroundItems,
            int groundInventoryFixture) {
        if (visibleGroundItems == null || groundInventoryFixture < -1) {
            throw new IllegalArgumentException(
                    "invalid airborne item-search input");
        }
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive
                || actor.hiddenStatus != HIDDEN_STATUS_FLY
                || actor.currentHp >= actor.definition.template().hp()) {
            return null;
        }

        Map<GridPoint, List<ClassicWorldGroundItems.Item>> inventories =
                new LinkedHashMap<>();
        for (ClassicWorldGroundItems.Item item : visibleGroundItems) {
            if (item != null && item.mapId() == mapId) {
                inventories.computeIfAbsent(
                        new GridPoint(item.x(), item.y()),
                        ignored -> new ArrayList<>()).add(item);
            }
        }
        if (inventories.isEmpty()) return null;
        List<List<ClassicWorldGroundItems.Item>> candidates =
                new ArrayList<>(inventories.values());
        if (groundInventoryFixture >= candidates.size()) return null;
        int selected = groundInventoryFixture >= 0
                ? groundInventoryFixture : actor.random.nextInt(candidates.size());
        for (ClassicWorldGroundItems.Item item : candidates.get(selected)) {
            if (!NativeEtcItemTypeCatalog.airbornePickupEligible(
                        item.definition())
                    || !canCarry(actor, item)) {
                continue;
            }
            Snapshot before = actor.snapshot();
            actor.hiddenStatus = HIDDEN_STATUS_NONE;
            actor.actorStatus = 0;
            actor.actived = false;
            activatedActors.remove(objectId);
            actor.targetItemObjectId = item.objectId();
            actor.targetItemObjectIds.add(item.objectId());
            return new AirItemLanding(
                    before, actor.snapshot(), item.objectId());
        }
        return null;
    }

    /**
     * Retained ICE actors stay targetable while frozen and surface only after
     * their HP has actually fallen below maximum.  Existing hate wins over the
     * scheduler candidate so the damaging actor remains the provoked target.
     */
    synchronized List<Reveal> revealDamagedIceInScreen(
            int mapId, int x, int y, long playerObjectId) {
        if (playerObjectId <= 0) return List.of();
        List<Reveal> result = new ArrayList<>();
        for (Actor actor : actors.values()) {
            if (!actor.present || !actor.alive
                    || actor.definition.mapId() != mapId
                    || actor.hiddenStatus != HIDDEN_STATUS_ICE
                    || actor.currentHp >= actor.definition.template().hp()
                    || !BattleSpellCatalog.isInNativeScreen(
                            x, y, actor.x, actor.y)) {
                continue;
            }
            long provokedObjectId = actor.targetObjectId > 0
                    ? actor.targetObjectId : playerObjectId;
            result.add(revealActor(actor, provokedObjectId));
        }
        return List.copyOf(result);
    }

    private List<Reveal> revealHidden(
            int mapId, int x, int y, long playerObjectId,
            int expectedHiddenStatus, int maximumDistance) {
        if (playerObjectId <= 0) return List.of();
        List<Reveal> result = new ArrayList<>();
        for (Actor actor : actors.values()) {
            if (!actor.present || !actor.alive
                    || actor.definition.mapId() != mapId
                    || actor.hiddenStatus != expectedHiddenStatus
                    || !BattleSpellCatalog.isInNativeScreen(
                            x, y, actor.x, actor.y)
                    || Math.max(Math.abs(x - actor.x), Math.abs(y - actor.y))
                        > maximumDistance) {
                continue;
            }
            result.add(revealActor(actor, playerObjectId));
        }
        return List.copyOf(result);
    }

    private static Reveal revealActor(Actor actor, long provokedObjectId) {
        Snapshot before = actor.snapshot();
        actor.hiddenStatus = HIDDEN_STATUS_NONE;
        actor.actorStatus = 0;
        actor.targetObjectId = provokedObjectId;
        return new Reveal(before, actor.snapshot(), provokedObjectId);
    }

    /** Guards keep their AI timeline until they have returned to source home. */
    synchronized List<Snapshot> guardsNeedingHome(int mapId) {
        return actors.values().stream()
                .filter(actor -> actor.definition.mapId() == mapId)
                .filter(actor -> actor.present && actor.alive)
                .filter(actor -> actor.definition.template().guard())
                .filter(actor -> actor.targetObjectId == 0)
                .filter(actor -> actor.x != actor.homeX
                        || actor.y != actor.homeY)
                .map(Actor::snapshot)
                .toList();
    }

    synchronized Snapshot actor(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        return actor == null || actor.definition.mapId() != mapId
                ? null : actor.snapshot();
    }

    synchronized boolean hasPresentNpc(int mapId, Set<Integer> npcIds) {
        return npcIds != null && !npcIds.isEmpty() && actors.values().stream()
                .anyMatch(actor -> actor.present
                        && actor.definition.mapId() == mapId
                        && npcIds.contains(actor.definition.template().npcId()));
    }

    /** Adds one one-shot runtime actor to the population's dynamic quest range. */
    synchronized Snapshot spawnQuestMonsterAt(
            int mapId, int spawnId, int x, int y, int heading,
            ClassicWorldMonsterCatalog.MonsterTemplate template) {
        if (nextQuestDynamicObjectId <= 0
                || nextQuestDynamicObjectId > questDynamicObjectEnd
                || mapId < 0 || spawnId <= 0 || x <= 0 || y <= 0
                || heading < 0 || heading > 7 || template == null) return null;
        long objectId = nextQuestDynamicObjectId++;
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        mapId, spawnId, "quest-runtime-spawn", 1, 0,
                        x, y, 0, 0, 0, 0, 0, 0, heading,
                        0, 0, false, 30, false, 0, template);
        Actor actor = new Actor(definition, 1, objectId,
                new Random(mix64(objectId)), true, List.of());
        if (!commitIfAvailable(actor, x, y)) return null;
        actors.put(objectId, actor);
        return actor.snapshot();
    }

    /** Reserves an invisible one-shot wild monster before the durable collar delete commits. */
    synchronized Snapshot reserveReleasedPet(
            int mapId, BattleCompanion.Snapshot pet, SourcePetTemplate source,
            List<RuntimeInventory.Item> carriedItems) {
        if (mapId < 0 || pet == null || source == null || !pet.active()
                || pet.kind() != BattleCompanion.Kind.PET || pet.hp() <= 0
                || pet.npcId() != source.npcId()
                || isOccupied(new Cell(mapId, pet.x(), pet.y()))) {
            return null;
        }
        long objectId = allocateReleasedPetObjectId();
        int spawnId = (int) (objectId & 0x7FFF_FFFFL);
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        mapId, spawnId, "released-pet", 1, 0,
                        pet.x(), pet.y(), 0, 0, 0, 0, 0, 0, pet.heading(),
                        0, 0, false, 30, false, 0,
                        source.releasedMonsterTemplate(pet));
        Actor actor = new Actor(definition, 1, objectId,
                new Random(mix64(objectId)), true, carriedItems);
        actor.x = pet.x();
        actor.y = pet.y();
        actor.homeX = pet.x();
        actor.homeY = pet.y();
        actor.heading = pet.heading();
        actor.currentHp = pet.hp();
        actor.currentMp = pet.mp();
        actor.present = false;
        actors.put(objectId, actor);
        occupy(new Cell(mapId, pet.x(), pet.y()));
        return actor.snapshot();
    }

    synchronized Snapshot commitReleasedPet(long objectId) {
        Actor actor = actors.get(objectId);
        if (actor == null || !actor.oneShot || actor.present || !actor.alive
                || !isOccupied(new Cell(
                        actor.definition.mapId(), actor.x, actor.y))) {
            return null;
        }
        actor.present = true;
        return actor.snapshot();
    }

    synchronized boolean cancelReleasedPet(long objectId) {
        Actor actor = actors.get(objectId);
        if (actor == null || !actor.oneShot || actor.present) return false;
        actors.remove(objectId);
        vacate(new Cell(actor.definition.mapId(), actor.x, actor.y));
        return true;
    }

    synchronized List<RuntimeInventory.Item> takeReleasedPetItems(
            int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId || !actor.oneShot
                || actor.alive || !actor.present) {
            return List.of();
        }
        List<RuntimeInventory.Item> result = actor.releasedItems.items();
        actor.releasedItems.clear();
        actor.digestSeconds.clear();
        digestActors.remove(objectId);
        return result;
    }

    /**
     * Retained DropShare consumes the NPC inventory after death. This includes
     * both a released pet's original bag and ordinary items picked up while
     * the monster was alive.
     */
    synchronized List<RuntimeInventory.Item> takeMonsterCarriedItems(
            int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || actor.alive || !actor.present) {
            return List.of();
        }
        List<RuntimeInventory.Item> result = actor.releasedItems.items();
        actor.releasedItems.clear();
        actor.digestSeconds.clear();
        digestActors.remove(objectId);
        return result;
    }

    synchronized boolean occupies(int mapId, int x, int y) {
        return isOccupied(new Cell(mapId, x, y));
    }

    synchronized Hit damage(int n, long l, int n2) {
        return this.damage(n, l, 0L, n2);
    }

    synchronized Healing heal(int mapId, long objectId, int amount) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || amount <= 0) {
            return null;
        }
        Snapshot before = actor.snapshot();
        actor.currentHp = Math.min(
                actor.definition.template().hp(),
                Math.addExact(actor.currentHp, amount));
        return new Healing(before, actor.snapshot(), actor.currentHp - before.currentHp());
    }

    synchronized ManaDrain drainMana(int mapId, long objectId, int requestedDrain) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || requestedDrain <= 0) {
            return null;
        }
        Snapshot before = actor.snapshot();
        int actualDrain = Math.min(actor.currentMp, requestedDrain);
        actor.currentMp -= actualDrain;
        return new ManaDrain(before, actor.snapshot(), requestedDrain, actualDrain);
    }

    synchronized int recoverMana(int mapId, long objectId, int amount) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || amount <= 0) return 0;
        int before = actor.currentMp;
        actor.currentMp = Math.min(actor.definition.template().mp(),
                Math.addExact(actor.currentMp, amount));
        return actor.currentMp - before;
    }

    synchronized ManaDrain drainManaIfStrictlyAbove(
            int mapId, long objectId, int requestedDrain) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || requestedDrain <= 0) {
            return null;
        }
        Snapshot before = actor.snapshot();
        int actualDrain = actor.currentMp > requestedDrain ? requestedDrain : 0;
        actor.currentMp -= actualDrain;
        return new ManaDrain(before, actor.snapshot(), requestedDrain, actualDrain);
    }

    synchronized Capture captureTamed(int n, long l) {
        Actor actor = this.actors.get(l);
        if (actor == null || actor.definition.mapId() != n || !actor.alive || !actor.present) {
            return null;
        }
        Snapshot snapshot = actor.snapshot();
        actor.present = false;
        actor.actived = false;
        this.activatedActors.remove(l);
        actor.targetObjectId = 0L;
        ClassicWorldMonsterPopulation.clearItemTargets(actor);
        actor.nextActionNanos = 0L;
        actor.skillReadyNanos.clear();
        actor.physicalSkillUseCount.clear();
        actor.weaponBreakStacks = 0;
        actor.hasteExpiresAtNanos = 0L;
        actor.slowExpiresAtNanos = 0L;
        actor.areaSilenceExpiresAtNanos = 0L;
        actor.windShackleExpiresAtNanos = 0L;
        actor.polluteWaterExpiresAtNanos = 0L;
        actor.strikerGaleExpiresAtNanos = 0L;
        actor.armorBreakExpiresAtNanos = 0L;
        CombatMonsterParalysis.resetMagic(actor.damagePoisonStatus);
        CombatMonsterCurseLifecycle.clearSource(actor, this);
        actor.elementalFallDownStatus.clear();
        actor.eraseMagicStatus.clear();
        actor.panicStatus.clear();
        actor.joyOfPainStatus.clear();
        actor.combatHate.clear();
        actor.contributionHate.clear();
        actor.pathFailCount = 0;
        actor.firstAttack = false;
        this.vacate(new Cell(n, actor.x, actor.y));
        return new Capture(snapshot, actor.snapshot());
    }

    synchronized TamedRelease releaseTamed(
            int mapId, long objectId, BattleCompanion.Snapshot companion) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.alive || actor.present || companion == null
                || !companion.active() || companion.hp() <= 0
                || companion.kind() != BattleCompanion.Kind.TAMED
                || companion.npcId() != actor.definition.template().npcId()) {
            return null;
        }
        Snapshot hidden = actor.snapshot();
        actor.x = companion.x();
        actor.y = companion.y();
        actor.heading = companion.heading();
        actor.currentHp = Math.min(actor.definition.template().hp(), companion.hp());
        actor.currentMp = Math.min(actor.definition.template().mp(), companion.mp());
        actor.present = true;
        actor.actived = false;
        activatedActors.remove(objectId);
        occupy(new Cell(mapId, actor.x, actor.y));
        updateWandered(actor);
        return new TamedRelease(hidden, actor.snapshot());
    }

    /** Converts a hidden captured slot into an ordinary dead respawn candidate. */
    synchronized Snapshot abandonTamed(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.alive || actor.present) return null;
        actor.currentHp = 0;
        actor.alive = false;
        actor.actived = false;
        activatedActors.remove(objectId);
        actor.targetObjectId = 0;
        clearItemTargets(actor);
        actor.nextActionNanos = 0;
        actor.combatHate.clear();
        actor.contributionHate.clear();
        actor.pathFailCount = 0;
        actor.firstAttack = false;
        return actor.snapshot();
    }

    synchronized Hit damage(int n, long l, long l2, int n2) {
        Contribution contribution = l2 > 0L ? new Contribution(l2, 1L) : null;
        return this.damage(n, l, contribution, n2);
    }

    synchronized Hit damage(int n, long l, ContributorKind contributorKind, long l2, long l3, int n2) {
        return this.damage(n, l, new Contribution(contributorKind, l2, l3, 1L), n2);
    }

    private synchronized Hit damage(int n, long l, Contribution contribution, int n2) {
        Actor actor;
        block8: {
            block7: {
                actor = this.actors.get(l);
                if (actor == null || actor.definition.mapId() != n || !actor.present || !actor.alive || CombatWorldCommit.input(n2, actor) <= 0) break block7;
                Actor actor2 = actor;
                if (CombatMonsterCurseLifecycle.damageHidden(actor2, actor2.hiddenStatus) == 1) break block7;
                Actor actor3 = actor;
                if (CombatMonsterCurseLifecycle.damageHidden(actor3, actor3.hiddenStatus) != 2 && !CombatMonsterCurseLifecycle.damageEarthBound(actor.damagePoisonStatus, System.nanoTime())) break block8;
            }
            return null;
        }
        Snapshot snapshot = actor.snapshot();
        int n3 = CombatWorldCommit.delta(actor.currentHp, n2, actor);
        actor.currentHp -= n3;
        CombatWorldCommit.sleep(actor.damagePoisonStatus, n2);
        if (CombatWorldCommit.hate(contribution, n2) != null) {
            long l2 = n2;
            if (!actor.firstAttack) {
                l2 = Math.addExact(l2, (long)actor.definition.template().hp() / 10L);
                actor.firstAttack = true;
            }
            ContributorKey contributorKey = new ContributorKey(contribution.kind(), contribution.sourceObjectId(), contribution.playerObjectId());
            actor.contributionHate.merge(contributorKey, l2, Math::addExact);
            actor.combatHate.merge(contribution.sourceObjectId(), l2, Math::addExact);
            actor.targetObjectId = ClassicWorldMonsterPopulation.maxCombatHateTarget(actor);
            actor.pathFailCount = 0;
            if (contribution.kind() == ContributorKind.PLAYER) {
                actor.pendingFamilyTargetObjectId = contribution.playerObjectId();
            }
        }
        boolean bl = actor.currentHp == 0;
        List<Contribution> list = List.of();
        if (bl) {
            list = actor.contributionHate.entrySet().stream().map(entry -> new Contribution(((ContributorKey)entry.getKey()).kind(), ((ContributorKey)entry.getKey()).sourceObjectId(), ((ContributorKey)entry.getKey()).playerObjectId(), (Long)entry.getValue())).toList();
            actor.contributionHate.clear();
            actor.alive = false;
            actor.actived = false;
            this.activatedActors.remove(l);
            actor.targetObjectId = 0L;
            ClassicWorldMonsterPopulation.clearItemTargets(actor);
            actor.pendingFamilyTargetObjectId = 0L;
            actor.nextActionNanos = 0L;
            actor.skillReadyNanos.clear();
            actor.physicalSkillUseCount.clear();
            actor.combatHate.clear();
            actor.pathFailCount = 0;
            actor.weaponBreakStacks = 0;
            actor.hasteExpiresAtNanos = 0L;
            actor.slowExpiresAtNanos = 0L;
            actor.areaSilenceExpiresAtNanos = 0L;
            actor.windShackleExpiresAtNanos = 0L;
            actor.polluteWaterExpiresAtNanos = 0L;
            actor.strikerGaleExpiresAtNanos = 0L;
            actor.armorBreakExpiresAtNanos = 0L;
            CombatMonsterParalysis.resetMagic(actor.damagePoisonStatus);
            CombatMonsterCurseLifecycle.clearSource(actor, this);
            actor.elementalFallDownStatus.clear();
            actor.eraseMagicStatus.clear();
            actor.panicStatus.clear();
            actor.joyOfPainStatus.clear();
            this.vacate(new Cell(n, actor.x, actor.y));
        }
        return new Hit(snapshot, actor.snapshot(), CombatWorldCommit.applied(n3), bl, list);
    }

    synchronized Snapshot removeCorpse(int n, long l) {
        Actor actor = this.actors.get(l);
        if (actor == null || actor.definition.mapId() != n || actor.alive || !actor.present) {
            return null;
        }
        this.detachMobGroup(actor);
        actor.present = false;
        actor.actived = false;
        this.activatedActors.remove(l);
        this.wanderedActors.remove(l);
        actor.targetObjectId = 0L;
        ClassicWorldMonsterPopulation.clearItemTargets(actor);
        actor.pendingFamilyTargetObjectId = 0L;
        actor.nextActionNanos = 0L;
        actor.randomMoveDistance = 0;
        actor.randomMoveDirection = 0;
        actor.skillReadyNanos.clear();
        actor.physicalSkillUseCount.clear();
        actor.weaponBreakStacks = 0;
        actor.hasteExpiresAtNanos = 0L;
        actor.slowExpiresAtNanos = 0L;
        actor.areaSilenceExpiresAtNanos = 0L;
        actor.windShackleExpiresAtNanos = 0L;
        actor.polluteWaterExpiresAtNanos = 0L;
        actor.strikerGaleExpiresAtNanos = 0L;
        actor.armorBreakExpiresAtNanos = 0L;
        CombatMonsterParalysis.resetMagic(actor.damagePoisonStatus);
        CombatMonsterCurseLifecycle.clearSource(actor, this);
        actor.elementalFallDownStatus.clear();
        actor.eraseMagicStatus.clear();
        actor.panicStatus.clear();
        actor.joyOfPainStatus.clear();
        actor.combatHate.clear();
        actor.contributionHate.clear();
        actor.pathFailCount = 0;
        Snapshot snapshot = actor.snapshot();
        if (actor.oneShot) {
            this.actors.remove(l);
            if (actor.mobGroupMinion) {
                --this.mobGroupMinionCount;
            }
        }
        return snapshot;
    }

    synchronized Snapshot nativeCancellationDelete(int n, long l) {
        Actor actor = this.actors.get(l);
        if (actor == null || !actor.present || actor.definition.mapId() != n) {
            return null;
        }
        if (actor.alive) {
            actor.alive = false;
            actor.currentHp = 0;
            this.vacate(new Cell(n, actor.x, actor.y));
        }
        actor.releasedItems.clear();
        actor.digestSeconds.clear();
        this.digestActors.remove(l);
        return this.removeCorpse(n, l);
    }

    synchronized Snapshot nativeCancellationSpawn(int n, long l, ClassicWorldMonsterCatalog.MonsterTemplate monsterTemplate) {
        Actor actor = this.actors.get(l);
        if (actor == null || !actor.alive || !actor.present || actor.definition.mapId() != n || monsterTemplate == null || this.nextQuestDynamicObjectId <= 0L || this.nextQuestDynamicObjectId > this.questDynamicObjectEnd || this.actors.containsKey(this.nextQuestDynamicObjectId)) {
            return null;
        }
        long l2 = this.nextQuestDynamicObjectId;
        ClassicWorldMonsterCatalog.SpawnDefinition spawnDefinition = new ClassicWorldMonsterCatalog.SpawnDefinition(n, actor.definition.spawnId(), "cancellation-runtime-spawn", 1, 0, actor.x, actor.y, 0, 0, 0, 0, 0, 0, actor.heading, 0, 0, false, 30, false, 0, monsterTemplate);
        Actor actor2 = new Actor(spawnDefinition, 1, l2, new Random(ClassicWorldMonsterPopulation.mix64(l2)), true, List.of());
        actor2.x = actor.x;
        actor2.y = actor.y;
        actor2.homeX = actor.x;
        actor2.homeY = actor.y;
        actor2.heading = actor.heading;
        Snapshot snapshot = actor2.snapshot();
        this.occupy(new Cell(n, actor2.x, actor2.y));
        this.actors.put(l2, actor2);
        ++this.nextQuestDynamicObjectId;
        return snapshot;
    }

    synchronized Resurrection resurrect(int n, long l, int n2) {
        Actor actor = this.actors.get(l);
        if (actor == null || actor.definition.mapId() != n || actor.alive || !actor.present || n2 <= 0 || n2 > actor.definition.template().hp()) {
            return null;
        }
        Snapshot snapshot = actor.snapshot();
        actor.currentHp = n2;
        actor.alive = true;
        actor.actived = false;
        this.activatedActors.remove(l);
        actor.targetObjectId = 0L;
        ClassicWorldMonsterPopulation.clearItemTargets(actor);
        actor.pendingFamilyTargetObjectId = 0L;
        actor.nextActionNanos = 0L;
        actor.randomMoveDistance = 0;
        actor.randomMoveDirection = 0;
        actor.skillReadyNanos.clear();
        actor.physicalSkillUseCount.clear();
        actor.weaponBreakStacks = 0;
        actor.hasteExpiresAtNanos = 0L;
        actor.slowExpiresAtNanos = 0L;
        actor.areaSilenceExpiresAtNanos = 0L;
        actor.windShackleExpiresAtNanos = 0L;
        actor.polluteWaterExpiresAtNanos = 0L;
        actor.strikerGaleExpiresAtNanos = 0L;
        actor.armorBreakExpiresAtNanos = 0L;
        CombatMonsterParalysis.resetMagic(actor.damagePoisonStatus);
        CombatMonsterCurseLifecycle.clearSource(actor, this);
        actor.elementalFallDownStatus.clear();
        actor.eraseMagicStatus.clear();
        actor.panicStatus.clear();
        actor.joyOfPainStatus.clear();
        actor.combatHate.clear();
        actor.contributionHate.clear();
        actor.pathFailCount = 0;
        actor.firstAttack = false;
        this.occupy(new Cell(n, actor.x, actor.y));
        return new Resurrection(snapshot, actor.snapshot());
    }

    synchronized Respawn respawn(int n, long l) {
        Actor actor = this.actors.get(l);
        if (actor == null || actor.definition.mapId() != n || actor.alive || actor.present) {
            return null;
        }
        Snapshot snapshot = actor.snapshot();
        actor.currentHp = actor.definition.template().hp();
        actor.currentMp = actor.definition.template().mp();
        actor.alive = true;
        actor.present = true;
        actor.actived = false;
        this.activatedActors.remove(l);
        actor.heading = actor.definition.heading();
        actor.targetObjectId = 0L;
        ClassicWorldMonsterPopulation.clearItemTargets(actor);
        actor.releasedItems.clear();
        actor.digestSeconds.clear();
        this.digestActors.remove(l);
        actor.pendingFamilyTargetObjectId = 0L;
        actor.nextActionNanos = 0L;
        actor.randomMoveDistance = 0;
        actor.randomMoveDirection = 0;
        actor.skillReadyNanos.clear();
        actor.physicalSkillUseCount.clear();
        actor.weaponBreakStacks = 0;
        actor.hasteExpiresAtNanos = 0L;
        actor.slowExpiresAtNanos = 0L;
        actor.areaSilenceExpiresAtNanos = 0L;
        actor.windShackleExpiresAtNanos = 0L;
        actor.polluteWaterExpiresAtNanos = 0L;
        actor.strikerGaleExpiresAtNanos = 0L;
        actor.armorBreakExpiresAtNanos = 0L;
        CombatMonsterParalysis.resetMagic(actor.damagePoisonStatus);
        CombatMonsterCurseLifecycle.clearSource(actor, this);
        actor.elementalFallDownStatus.clear();
        actor.eraseMagicStatus.clear();
        actor.panicStatus.clear();
        actor.joyOfPainStatus.clear();
        actor.combatHate.clear();
        actor.contributionHate.clear();
        actor.pathFailCount = 0;
        actor.firstAttack = false;
        actor.resetRuntimeAppearance();
        actor.nativeTamedDropInitialized = false;
        NativeTamedWorldLifecycle.retireRespawn(actor);
        if (!this.place(actor)) {
            actor.currentHp = 0;
            actor.alive = false;
            actor.present = false;
            return null;
        }
        actor.resetInitialHiddenState();
        List<Snapshot> list = this.spawnRespawnMobGroup(actor);
        return new Respawn(snapshot, actor.snapshot(), list);
    }

    synchronized int nextRespawnDelaySeconds(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId) return -1;
        if (actor.oneShot) return -1;
        int minimum = actor.definition.minRespawnSeconds();
        int maximum = actor.definition.maxRespawnSeconds();
        // Retained L1Spawn treats a non-positive (max-min) interval as the
        // exact minimum delay; selected map0 row 400580 intentionally has
        // min=7200 and max=3600.
        if (maximum <= minimum) return minimum;
        return minimum + actor.random.nextInt(maximum - minimum + 1);
    }

    private long allocateReleasedPetObjectId() {
        long capacity = releasedPetObjectEnd - releasedPetObjectBase + 1;
        for (long attempt = 0; attempt < capacity; attempt++) {
            long candidate = nextReleasedPetObjectId++;
            if (nextReleasedPetObjectId > releasedPetObjectEnd) {
                nextReleasedPetObjectId = releasedPetObjectBase;
            }
            if (!actors.containsKey(candidate)) return candidate;
        }
        throw new IllegalStateException("released pet object id range exhausted");
    }

    synchronized Snapshot nearest(int mapId, int x, int y, int npcId) {
        Snapshot best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Actor actor : actors.values()) {
            if (!actor.present || actor.definition.mapId() != mapId
                    || actor.definition.template().npcId() != npcId) continue;
            int distance = Math.abs(x - actor.x) + Math.abs(y - actor.y);
            if (distance < bestDistance) {
                best = actor.snapshot();
                bestDistance = distance;
            }
        }
        return best;
    }

    synchronized boolean provoke(
            int mapId, long objectId, long playerObjectId) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || playerObjectId <= 0) {
            return false;
        }
        actor.combatHate.putIfAbsent(playerObjectId, 0L);
        actor.targetObjectId = playerObjectId;
        actor.pathFailCount = 0;
        clearItemTargets(actor);
        return true;
    }

    /**
     * Selected {@code serchLink}: a directly attacked monster defers support
     * until it proves that it can attack, cast through line of sight, or take a
     * path step.  Only player-visible, idle allies with their own clear line of
     * sight receive zero-hate links.
     */
    private void flushDeferredFamilyLink(
            Actor attacked, long playerObjectId, int playerX, int playerY) {
        if (attacked.pendingFamilyTargetObjectId != playerObjectId) return;
        attacked.pendingFamilyTargetObjectId = 0;
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(attacked.definition.mapId());
        int attackedDistance = Math.max(
                Math.abs(attacked.x - playerX), Math.abs(attacked.y - playerY));
        if (attackedDistance
                    > ClassicWorldMonsterAggressionCatalog.LOS_REQUIRED_BEYOND_TILES
                && !map.hasLineOfSight(attacked.x, attacked.y, playerX, playerY)) {
            return;
        }
        ClassicWorldMonsterAggressionCatalog.Profile attackedProfile =
                ClassicWorldMonsterAggressionCatalog.forTemplate(
                        attacked.definition.template());
        for (Actor ally : actors.values()) {
            if (ally == attacked || !ally.present || !ally.alive
                    || ally.definition.mapId() != attacked.definition.mapId()
                    || ally.hiddenStatus != HIDDEN_STATUS_NONE
                    || ally.targetObjectId != 0
                    || !BattleSpellCatalog.isInNativeScreen(
                            playerX, playerY, ally.x, ally.y)
                    || !map.hasLineOfSight(ally.x, ally.y, playerX, playerY)) {
                continue;
            }
            ClassicWorldMonsterAggressionCatalog.Profile allyProfile =
                    ClassicWorldMonsterAggressionCatalog.forTemplate(
                            ally.definition.template());
            boolean familySupport = allyProfile.agroFamily() > 0
                    && (allyProfile.agroFamily() > 1
                        || allyProfile.family().equals(attackedProfile.family()));
            boolean groupSupport = attacked.mobGroupLeaderObjectId != 0
                    && attacked.mobGroupLeaderObjectId
                        == ally.mobGroupLeaderObjectId;
            if (familySupport || groupSupport) {
                ally.combatHate.putIfAbsent(playerObjectId, 0L);
                ally.targetObjectId = playerObjectId;
                ally.pathFailCount = 0;
                clearItemTargets(ally);
            }
        }
    }

    private void detachMobGroup(Actor actor) {
        long groupId = actor.mobGroupLeaderObjectId;
        if (groupId == 0) return;
        if (actor.mobGroupLeader && actor.removeMobGroupIfLeaderDies) {
            for (Actor member : actors.values()) {
                if (member.mobGroupLeaderObjectId == groupId) {
                    member.mobGroupLeaderObjectId = 0;
                    member.mobGroupLeader = false;
                    member.removeMobGroupIfLeaderDies = false;
                }
            }
        } else {
            actor.mobGroupLeaderObjectId = 0;
            actor.mobGroupLeader = false;
            actor.removeMobGroupIfLeaderDies = false;
        }
    }

    synchronized Snapshot faceAndRest(
            int mapId, long objectId, int targetX, int targetY,
            long nowNanos, int restSeconds) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive
                || !actor.definition.template().guardian()
                || targetX <= 0 || targetY <= 0 || nowNanos < 0
                || restSeconds < 0) return null;
        actor.heading = heading(actor.x, actor.y, targetX, targetY);
        actor.nextActionNanos = Math.max(actor.nextActionNanos,
                nowNanos + TimeUnit.SECONDS.toNanos(restSeconds));
        return actor.snapshot();
    }

    synchronized int addWeaponBreakStack(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive) {
            return -1;
        }
        actor.weaponBreakStacks = BattleWeaponBreak.addStack(
                actor.weaponBreakStacks);
        return actor.weaponBreakStacks;
    }

    synchronized int weaponBreakStacks(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        return actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive
                ? 0 : actor.weaponBreakStacks;
    }

    synchronized boolean slow(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || nowNanos < 0
                || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            return false;
        }
        actor.hasteExpiresAtNanos = 0;
        actor.slowExpiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        return true;
    }

    synchronized boolean haste(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || nowNanos < 0
                || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            return false;
        }
        actor.slowExpiresAtNanos = 0;
        actor.hasteExpiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        return true;
    }

    synchronized boolean hasteActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive && nowNanos >= 0
                && nowNanos < actor.hasteExpiresAtNanos;
    }

    synchronized boolean cancelHaste(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive
                || actor.hasteExpiresAtNanos == 0) return false;
        actor.hasteExpiresAtNanos = 0;
        return true;
    }

    /**
     * Selected CANCELLATION removes every cancelable status represented by an
     * ordinary WORLD actor and resets the retained NPC weapon-break counter in
     * the same population lock.
     */
    synchronized MagicCancellation cancelMagicEffects(int n, long l) {
        Actor actor = this.actors.get(l);
        if (actor == null || actor.definition.mapId() != n || !actor.present || !actor.alive) {
            return null;
        }
        Snapshot snapshot = actor.snapshot();
        boolean bl = actor.hasteExpiresAtNanos != 0L;
        boolean bl2 = actor.slowExpiresAtNanos != 0L;
        boolean bl3 = actor.areaSilenceExpiresAtNanos != 0L;
        boolean bl4 = actor.windShackleExpiresAtNanos != 0L;
        boolean bl5 = actor.polluteWaterExpiresAtNanos != 0L;
        boolean bl6 = actor.strikerGaleExpiresAtNanos != 0L;
        boolean bl7 = actor.armorBreakExpiresAtNanos != 0L;
        boolean bl8 = actor.weaponBreakStacks != 0;
        BattleTimedStatus.MagicCancellation magicCancellation = CombatMonsterParalysis.cancelMagic(actor.damagePoisonStatus);
        BattleElementalFallDownStatus.Cancellation cancellation = actor.elementalFallDownStatus.cancel();
        boolean bl9 = actor.eraseMagicStatus.cancel();
        actor.armBreakerStatus.cancel();
        boolean bl10 = actor.panicStatus.cancel();
        actor.joyOfPainStatus.cancel();
        actor.weaponBreakStacks = 0;
        actor.hasteExpiresAtNanos = 0L;
        actor.slowExpiresAtNanos = 0L;
        actor.areaSilenceExpiresAtNanos = 0L;
        actor.windShackleExpiresAtNanos = 0L;
        actor.polluteWaterExpiresAtNanos = 0L;
        actor.strikerGaleExpiresAtNanos = 0L;
        actor.armorBreakExpiresAtNanos = 0L;
        CombatMonsterCurseLifecycle.clearSource(actor, this);
        return new MagicCancellation(snapshot, bl, bl2, magicCancellation.blind(), magicCancellation.weakness(), magicCancellation.disease(), bl3 || magicCancellation.silence(), magicCancellation.decayPotion(), magicCancellation.poison(), magicCancellation.paralysisCancellation().present(), magicCancellation.sleep(), magicCancellation.freeze(), magicCancellation.earthBind(), magicCancellation.thunderGrab(), magicCancellation.horrorOfDeath(), bl4, magicCancellation.boneBreak(), bl5, bl6, cancellation, bl9, bl7, bl8, bl10);
    }

    synchronized List<Snapshot> expireHastes(int mapId, long nowNanos) {
        if (mapId < 0 || nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Actor actor : actors.values()) {
            if (actor.definition.mapId() != mapId || !actor.present || !actor.alive
                    || actor.hasteExpiresAtNanos == 0
                    || nowNanos < actor.hasteExpiresAtNanos) continue;
            actor.hasteExpiresAtNanos = 0;
            expired.add(actor.snapshot());
        }
        return List.copyOf(expired);
    }

    private static boolean poisonLeaseActive(Actor actor, long l) {
        return CombatMonsterCurseLifecycle.poisonExisting(switch (actor.poisonKind) {
            default -> throw new IncompatibleClassChangeError();
            case DAMAGE -> CombatMonsterCurseLifecycle.damageActive(actor.damagePoisonStatus, l);
            case SILENCE -> actor.damagePoisonStatus.poisonSilenceActive();
            case PARALYSIS -> CombatMonsterParalysis.poisonVisual(actor.damagePoisonStatus, l);
            case NONE -> false;
        }, actor.damagePoisonStatus);
    }

    private static void setPoisonSource(
            Actor actor, ContributorKind contributorKind,
            long attackerObjectId, long playerObjectId,
            ClassicWorldMonsterPoisonAttack.Kind kind) {
        actor.poisonContributorKind = contributorKind;
        actor.poisonAttackerObjectId = attackerObjectId;
        actor.poisonPlayerObjectId = playerObjectId;
        actor.poisonKind = kind;
    }

    private static void clearPoisonSource(Actor actor) {
        actor.poisonContributorKind = null;
        actor.poisonAttackerObjectId = 0;
        actor.poisonPlayerObjectId = 0;
        actor.poisonKind = ClassicWorldMonsterPoisonAttack.Kind.NONE;
    }

    synchronized boolean poison(int n, long l, long l2, long l3, int n2, int n3) {
        Actor actor = this.actors.get(l);
        if (actor == null || actor.definition.mapId() != n || !actor.present || !actor.alive || l2 <= 0L || l2 > 0xFFFFFFFFL || l3 < 0L || n2 <= 0 || n3 <= 0) {
            return false;
        }
        boolean bl = CombatMonsterCurseLifecycle.damage(actor.damagePoisonStatus, l3, n2, n3);
        if (bl) {
            ClassicWorldMonsterPopulation.setPoisonSource(actor, ContributorKind.PLAYER, l2, l2, ClassicWorldMonsterPoisonAttack.Kind.DAMAGE);
        }
        return bl;
    }

    /**
     * Retained {@code L1AttackNpc.addNpcPoisonAttack} against an NPC target.
     * NPC targets have no venom-resistance gate in {@code L1Poison}; the one
     * installed poison object is nevertheless exclusive across damage,
     * silence and paralysis poison kinds.
     */
    synchronized NpcPoison npcPoison(int n, long l, ContributorKind contributorKind, long l2, long l3, ClassicWorldMonsterPoisonAttack.Kind kind, long l4) {
        Actor actor = this.actors.get(l);
        if (actor == null || actor.definition.mapId() != n || !actor.present || !actor.alive || contributorKind == null || l2 <= 0L || l2 > 0xFFFFFFFFL || l3 <= 0L || l3 > 0xFFFFFFFFL || contributorKind == ContributorKind.PLAYER && l2 != l3 || kind == null || l4 < 0L) {
            return null;
        }
        boolean bl = ClassicWorldMonsterPopulation.poisonLeaseActive(actor, l4);
        boolean bl2 = false;
        if (kind != ClassicWorldMonsterPoisonAttack.Kind.NONE && !bl) {
            bl2 = switch (kind) {
                case DAMAGE -> CombatMonsterCurseLifecycle.damage(actor.damagePoisonStatus, l4, 30, 3000);
                case SILENCE -> actor.damagePoisonStatus.activatePoisonSilence(l4);
                case PARALYSIS -> actor.damagePoisonStatus.activateParalysis(l4, 20000, 45000);
                case NONE -> false;
            };
        }
        if (bl2) {
            ClassicWorldMonsterPopulation.setPoisonSource(actor, contributorKind, l2, l3, kind);
        }
        return new NpcPoison(kind, actor.snapshot(), bl, bl2);
    }

    /** Retained DARKNESS status shared by skills 20, 40 and 103. */
    synchronized boolean blind(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || nowNanos < 0
                || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            return false;
        }
        actor.damagePoisonStatus.activateBlind(nowNanos, durationSeconds);
        return true;
    }

    synchronized boolean blindActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive && nowNanos >= 0
                && actor.damagePoisonStatus.blindActive(nowNanos);
    }

    synchronized boolean weakness(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || nowNanos < 0
                || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            return false;
        }
        actor.damagePoisonStatus.activateWeakness(nowNanos, durationSeconds);
        return true;
    }

    synchronized boolean disease(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || nowNanos < 0
                || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            return false;
        }
        actor.damagePoisonStatus.activateDisease(nowNanos, durationSeconds);
        return true;
    }

    /** Selected skill64 SILENCE lease for one ordinary WORLD NPC. */
    synchronized boolean silence(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive
                || actor.definition.template().boss() || nowNanos < 0
                || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            return false;
        }
        actor.damagePoisonStatus.activateSilence(nowNanos, durationSeconds);
        return true;
    }

    /** Skill202 installs the ordinary Silence lease without refreshing it. */
    synchronized boolean confusionSilence(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive
                || actor.definition.template().boss() || nowNanos < 0
                || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            return false;
        }
        return actor.damagePoisonStatus.activateSilenceIfAbsent(
                nowNanos, durationSeconds);
    }

    /** Selected skill71 non-refreshing Decay Potion lease. */
    synchronized boolean decayPotion(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive
                || actor.definition.template().boss() || nowNanos < 0
                || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            return false;
        }
        if (!actor.damagePoisonStatus.decayPotionActive(nowNanos)) {
            actor.damagePoisonStatus.activateDecayPotion(
                    nowNanos, durationSeconds);
        }
        return true;
    }

    synchronized boolean weaknessActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive && nowNanos >= 0
                && actor.damagePoisonStatus.weaknessActive(nowNanos);
    }

    synchronized boolean diseaseActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive && nowNanos >= 0
                && actor.damagePoisonStatus.diseaseActive(nowNanos);
    }

    synchronized boolean silenceActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive && nowNanos >= 0
                && actor.damagePoisonStatus.silenceActive(nowNanos);
    }

    synchronized boolean decayPotionActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive && nowNanos >= 0
                && actor.damagePoisonStatus.decayPotionActive(nowNanos);
    }

    /**
     * Area of Silence blocks every NPC magic attempt for its full lease;
     * ordinary skill64 Silence consumes itself on the next eligible attempt.
     */
    synchronized boolean consumeSilenceForSpell(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || nowNanos < 0) {
            return false;
        }
        if (nowNanos < actor.areaSilenceExpiresAtNanos) return true;
        if (actor.areaSilenceExpiresAtNanos != 0) {
            actor.areaSilenceExpiresAtNanos = 0;
        }
        return actor.damagePoisonStatus.consumeSilenceForNpcSpell(nowNanos);
    }

    synchronized boolean poisonActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive && nowNanos >= 0
                && poisonLeaseActive(actor, nowNanos);
    }

    synchronized boolean curePoison(int n, long l) {
        Actor actor = this.actors.get(l);
        if (actor == null || actor.definition.mapId() != n || !actor.present || !actor.alive) {
            return false;
        }
        boolean bl = CombatMonsterCurseLifecycle.cureDamage(actor.damagePoisonStatus) | CombatMonsterCurseLifecycle.cureSilence(actor.damagePoisonStatus);
        if (actor.poisonKind == ClassicWorldMonsterPoisonAttack.Kind.PARALYSIS) {
            bl |= CombatMonsterCurseLifecycle.curePoisonParalysis(actor.damagePoisonStatus).present();
        }
        if (bl || actor.poisonKind != ClassicWorldMonsterPoisonAttack.Kind.NONE) {
            CombatMonsterCurseLifecycle.clearSource(actor, this);
        }
        return bl;
    }

    synchronized int poisonPalette(int n, long l, long l2) {
        Actor actor = this.actors.get(l);
        if (actor == null || actor.definition.mapId() != n || !actor.present || !actor.alive || l2 < 0L) {
            return 0;
        }
        if (CombatMonsterParalysis.paletteFirst(CombatWorldFreeze.rawFrozen(actor.damagePoisonStatus.freezeVisualLockActive(l2), actor.damagePoisonStatus), actor.damagePoisonStatus, l2)) {
            return 2;
        }
        if (actor.damagePoisonStatus.paralysisVisualActive(l2)) {
            if (actor.poisonKind == ClassicWorldMonsterPoisonAttack.Kind.PARALYSIS) {
                return CombatMonsterCurseLifecycle.poisonParalysisColor(actor.damagePoisonStatus, l2) ? 2 : 1;
            }
            return 2;
        }
        return CombatMonsterCurseLifecycle.damageActive(actor.damagePoisonStatus, l2) || actor.damagePoisonStatus.poisonSilenceActive() ? 1 : 0;
    }

    synchronized boolean freeze(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.damagePoisonStatus.activateFreeze(
                        nowNanos, durationSeconds);
    }

    synchronized boolean freezeActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.damagePoisonStatus.freezeActive(nowNanos);
    }

    synchronized boolean cancelFreeze(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.damagePoisonStatus.cancelFreeze();
    }

    synchronized boolean earthBind(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.damagePoisonStatus.activateEarthBind(
                        nowNanos, durationSeconds);
    }

    synchronized boolean earthBindActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.damagePoisonStatus.earthBindActive(nowNanos);
    }

    synchronized boolean cancelEarthBind(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.damagePoisonStatus.cancelEarthBind();
    }

    synchronized List<Snapshot> expireEarthBinds(
            int mapId, long nowNanos) {
        if (mapId < 0 || nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Actor actor : actors.values()) {
            if (actor.definition.mapId() != mapId || !actor.present || !actor.alive
                    || !actor.damagePoisonStatus.expireEarthBind(nowNanos)) continue;
            expired.add(actor.snapshot());
        }
        return List.copyOf(expired);
    }

    synchronized boolean sleep(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.damagePoisonStatus.activateSleep(
                        nowNanos, durationSeconds);
    }

    synchronized boolean sleepActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.damagePoisonStatus.sleepActive(nowNanos);
    }

    synchronized boolean cancelSleep(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.damagePoisonStatus.cancelSleep();
    }

    synchronized List<Snapshot> expireFreezes(int mapId, long nowNanos) {
        if (mapId < 0 || nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Actor actor : actors.values()) {
            if (actor.definition.mapId() != mapId || !actor.present || !actor.alive
                    || !actor.damagePoisonStatus.expireFreeze(nowNanos)) continue;
            expired.add(actor.snapshot());
        }
        return List.copyOf(expired);
    }

    synchronized BattleTimedStatus.ShockStunActivation shockStun(
            int mapId, long objectId, long nowNanos,
            int rolledSeconds, int maximumSeconds) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive) return null;
        return actor.damagePoisonStatus.activateShockStun(
                nowNanos, rolledSeconds, maximumSeconds);
    }

    synchronized boolean shockStunActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.damagePoisonStatus.shockStunActive(nowNanos);
    }

    synchronized boolean boneBreak(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.damagePoisonStatus.activateBoneBreak(
                        nowNanos, durationSeconds);
    }

    synchronized boolean boneBreakActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.damagePoisonStatus.boneBreakActive(nowNanos);
    }

    synchronized BattleArmBreakerStatus.Activation armBreaker(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        return actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive
                ? BattleArmBreakerStatus.Activation.rejected()
                : actor.armBreakerStatus.activateIfAbsent(
                        nowNanos, durationSeconds);
    }

    synchronized int armBreakerMeleeDamageModifier(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive ? 0
                : actor.armBreakerStatus.meleeDamageModifier(nowNanos);
    }

    synchronized boolean cancelArmBreaker(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.armBreakerStatus.cancel();
    }

    synchronized boolean panic(
            int mapId, long objectId, long nowNanos,
            int durationSeconds, int modifier) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.panicStatus.activateIfAbsent(
                        nowNanos, durationSeconds, modifier);
    }

    synchronized int panicAbilityScoreModifier(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive ? 0
                : actor.panicStatus.snapshot(nowNanos).abilityScoreModifier();
    }

    synchronized boolean cancelPanic(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive && actor.panicStatus.cancel();
    }

    synchronized List<Snapshot> expirePanics(int mapId, long nowNanos) {
        if (mapId < 0 || nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Actor actor : actors.values()) {
            if (actor.definition.mapId() != mapId || !actor.present
                    || !actor.alive || !actor.panicStatus.expire(nowNanos)) {
                continue;
            }
            expired.add(actor.snapshot());
        }
        return List.copyOf(expired);
    }

    synchronized BattleJoyOfPainStatus.Activation joyOfPain(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        return actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive ? null
                : actor.joyOfPainStatus.activateOrRefresh(
                        nowNanos, durationSeconds);
    }

    synchronized BattleJoyOfPainStatus.Lease captureJoyOfPain(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive ? null
                : actor.joyOfPainStatus.capture(nowNanos);
    }

    synchronized boolean consumeJoyOfPain(
            int mapId, long objectId, BattleJoyOfPainStatus.Lease lease) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.joyOfPainStatus.consume(lease);
    }

    synchronized List<Snapshot> expireJoyOfPain(int mapId, long nowNanos) {
        if (mapId < 0 || nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Actor actor : actors.values()) {
            if (actor.definition.mapId() != mapId || !actor.present
                    || !actor.alive
                    || !actor.joyOfPainStatus.expire(nowNanos)) continue;
            expired.add(actor.snapshot());
        }
        return List.copyOf(expired);
    }

    synchronized List<Snapshot> expireBoneBreaks(
            int mapId, long nowNanos) {
        if (mapId < 0 || nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Actor actor : actors.values()) {
            if (actor.definition.mapId() != mapId || !actor.present
                    || !actor.alive
                    || !actor.damagePoisonStatus
                            .expireBoneBreak(nowNanos)) continue;
            expired.add(actor.snapshot());
        }
        return List.copyOf(expired);
    }

    synchronized List<Snapshot> expireShockStuns(int mapId, long nowNanos) {
        if (mapId < 0 || nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Actor actor : actors.values()) {
            if (actor.definition.mapId() != mapId || !actor.present || !actor.alive
                    || !actor.damagePoisonStatus.expireShockStun(nowNanos)) continue;
            expired.add(actor.snapshot());
        }
        return List.copyOf(expired);
    }

    synchronized boolean thunderGrab(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive
                || actor.definition.template().boss()) return false;
        return actor.damagePoisonStatus.activateThunderGrab(
                nowNanos, durationSeconds);
    }

    synchronized boolean thunderGrabActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.damagePoisonStatus.thunderGrabActive(nowNanos);
    }

    synchronized List<Snapshot> expireThunderGrabs(
            int mapId, long nowNanos) {
        if (mapId < 0 || nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Actor actor : actors.values()) {
            if (actor.definition.mapId() != mapId || !actor.present || !actor.alive
                    || !actor.damagePoisonStatus
                            .expireThunderGrab(nowNanos)) continue;
            expired.add(actor.snapshot());
        }
        return List.copyOf(expired);
    }

    /** Retained skill193 gives NPCs a cancelable lease but no STR/INT penalty. */
    synchronized boolean horrorOfDeath(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive
                || actor.definition.template().boss()) return false;
        return actor.damagePoisonStatus.activateHorrorOfDeath(
                nowNanos, durationSeconds);
    }

    synchronized boolean horrorOfDeathActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.damagePoisonStatus.horrorOfDeathActive(nowNanos);
    }

    synchronized List<Snapshot> expireHorrorOfDeaths(
            int mapId, long nowNanos) {
        if (mapId < 0 || nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Actor actor : actors.values()) {
            if (actor.definition.mapId() != mapId || !actor.present || !actor.alive
                    || !actor.damagePoisonStatus
                            .expireHorrorOfDeath(nowNanos)) continue;
            expired.add(actor.snapshot());
        }
        return List.copyOf(expired);
    }

    /** Retained player-cast Curse Paralysis is immediate for monster targets. */
    synchronized boolean paralysis(int n, long l, long l2, int n2) {
        Actor actor = this.actors.get(l);
        if (actor == null || actor.definition.mapId() != n || !actor.present || !actor.alive || l2 < 0L || n2 <= 0) {
            return false;
        }
        return CombatMonsterParalysis.activate(actor.damagePoisonStatus, l2, 0, n2);
    }

    synchronized boolean paralysisActive(int n, long l, long l2) {
        Actor actor = this.actors.get(l);
        return actor != null && actor.definition.mapId() == n && actor.present && actor.alive && l2 >= 0L && CombatMonsterParalysis.curseVisual(actor.damagePoisonStatus, l2);
    }

    synchronized BattleTimedStatus.ParalysisCancellation cureParalysis(int n, long l) {
        Actor actor = this.actors.get(l);
        if (actor == null || actor.definition.mapId() != n || !actor.present || !actor.alive) {
            return new BattleTimedStatus.ParalysisCancellation(false, false);
        }
        BattleTimedStatus.ParalysisCancellation paralysisCancellation = CombatMonsterParalysis.cancelCurse(actor.damagePoisonStatus);
        if (CombatMonsterParalysis.legacyPoisonCancellation(paralysisCancellation) && actor.poisonKind == ClassicWorldMonsterPoisonAttack.Kind.PARALYSIS) {
            CombatMonsterCurseLifecycle.clearSource(actor, this);
        }
        return paralysisCancellation;
    }

    synchronized List<Snapshot> expireParalyses(int mapId, long nowNanos) {
        return pollParalyses(mapId, nowNanos).stream()
                .filter(ParalysisUpdate::expired)
                .map(ParalysisUpdate::target)
                .toList();
    }

    synchronized List<ParalysisUpdate> pollParalyses(int n, long l) {
        if (n < 0 || l < 0L) {
            return List.of();
        }
        List<ParalysisUpdate> arrayList = new ArrayList<ParalysisUpdate>();
        for (Actor actor : this.actors.values()) {
            if (!(actor.definition.mapId() != n || !actor.present || !actor.alive)) {
            boolean bl = actor.poisonKind == ClassicWorldMonsterPoisonAttack.Kind.PARALYSIS;
            CombatMonsterParalysis.Poll poll = CombatMonsterParalysis.poll(actor.damagePoisonStatus, l);
            if (poll.poisonExpired() && bl) {
                CombatMonsterCurseLifecycle.clearSource(actor, this);
            }
            if (!poll.expired() && (!poll.activated() || !bl)) continue;
            arrayList.add(new ParalysisUpdate(actor.snapshot(), poll.activated(), poll.expired(), CombatMonsterParalysis.poisonEvent(bl, poll)));

            }
}
        return List.copyOf(arrayList);
    }

    synchronized List<PoisonUpdate> pollPoisons(int n, long l, int n2, int n3) {
        if (n < 0 || l < 0L || n2 <= 0 || n3 <= 0) {
            return List.of();
        }
        List<PoisonUpdate> arrayList = new ArrayList<PoisonUpdate>();
        for (Actor actor : List.copyOf(this.actors.values())) {
            if (!(actor.definition.mapId() != n || !actor.present || !actor.alive)) {
            long l2 = actor.poisonAttackerObjectId;
            long l3 = actor.poisonPlayerObjectId;
            ContributorKind contributorKind = actor.poisonContributorKind;
            BattleTimedStatus.PoisonPoll poisonPoll = CombatMonsterCurseLifecycle.damagePoll(actor.damagePoisonStatus, l, n2);
            if (l2 <= 0L || l3 <= 0L || contributorKind == null || poisonPoll.damageTicks() == 0 && !poisonPoll.expired()) continue;
            List<Hit> arrayList2 = new ArrayList<Hit>();
            for (int i = 0; i < poisonPoll.damageTicks() && actor.alive; ++i) {
                Hit hit = this.damage(n, actor.objectId, contributorKind, l2, l3, n3);
                if (!(hit == null)) {
                arrayList2.add(hit);

                }
}
            Snapshot snapshot = arrayList2.isEmpty() ? actor.snapshot() : ((Hit)arrayList2.get(arrayList2.size() - 1)).after();
            arrayList.add(new PoisonUpdate(contributorKind, l2, l3, snapshot, arrayList2, poisonPoll.expired()));
            if (!poisonPoll.expired()) continue;
            CombatMonsterCurseLifecycle.clearSource(actor, this);

            }
}
        return List.copyOf(arrayList);
    }

    synchronized boolean slowActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive && nowNanos >= 0
                && nowNanos < actor.slowExpiresAtNanos;
    }

    synchronized List<Snapshot> expireSlows(int mapId, long nowNanos) {
        if (mapId < 0 || nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Actor actor : actors.values()) {
            if (actor.definition.mapId() != mapId || !actor.present || !actor.alive
                    || actor.slowExpiresAtNanos == 0
                    || nowNanos < actor.slowExpiresAtNanos) {
                continue;
            }
            actor.slowExpiresAtNanos = 0;
            expired.add(actor.snapshot());
        }
        return List.copyOf(expired);
    }

    synchronized boolean areaSilence(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || nowNanos < 0
                || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || actor.definition.template().boss()) return false;
        actor.areaSilenceExpiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        return true;
    }

    synchronized boolean areaSilenceActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive && nowNanos >= 0
                && nowNanos < actor.areaSilenceExpiresAtNanos;
    }

    synchronized boolean windShackle(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || nowNanos < 0
                || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || actor.definition.template().boss()
                || nowNanos < actor.windShackleExpiresAtNanos) return false;
        actor.windShackleExpiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        return true;
    }

    synchronized boolean windShackleActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || nowNanos < 0) return false;
        if (actor.windShackleExpiresAtNanos != 0
                && nowNanos >= actor.windShackleExpiresAtNanos) {
            actor.windShackleExpiresAtNanos = 0;
        }
        return nowNanos < actor.windShackleExpiresAtNanos;
    }

    synchronized boolean polluteWater(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || nowNanos < 0
                || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || actor.definition.template().boss()) return false;
        actor.polluteWaterExpiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        return true;
    }

    synchronized boolean polluteWaterActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || nowNanos < 0) return false;
        if (actor.polluteWaterExpiresAtNanos != 0
                && nowNanos >= actor.polluteWaterExpiresAtNanos) {
            actor.polluteWaterExpiresAtNanos = 0;
        }
        return nowNanos < actor.polluteWaterExpiresAtNanos;
    }

    synchronized boolean strikerGale(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || nowNanos < 0
                || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || actor.definition.template().boss()) return false;
        actor.strikerGaleExpiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        return true;
    }

    synchronized boolean strikerGaleActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || nowNanos < 0) return false;
        if (actor.strikerGaleExpiresAtNanos != 0
                && nowNanos >= actor.strikerGaleExpiresAtNanos) {
            actor.strikerGaleExpiresAtNanos = 0;
        }
        return nowNanos < actor.strikerGaleExpiresAtNanos;
    }

    synchronized boolean armorBreak(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || nowNanos < 0
                || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || actor.definition.template().boss()) return false;
        actor.armorBreakExpiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        return true;
    }

    synchronized boolean armorBreakActive(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || nowNanos < 0) return false;
        if (actor.armorBreakExpiresAtNanos != 0
                && nowNanos >= actor.armorBreakExpiresAtNanos) {
            actor.armorBreakExpiresAtNanos = 0;
        }
        return nowNanos < actor.armorBreakExpiresAtNanos;
    }

    synchronized boolean elementalFallDown(
            int mapId, long objectId, long nowNanos, int durationSeconds,
            int sourceElfAttribute) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || nowNanos < 0
                || durationSeconds <= 0 || actor.definition.template().boss()) {
            return false;
        }
        actor.elementalFallDownStatus.activate(
                nowNanos, durationSeconds, sourceElfAttribute);
        return true;
    }

    synchronized int elementalFallDownModifier(
            int mapId, long objectId, int attributeKind, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive ? 0
                : actor.elementalFallDownStatus.modifier(
                        attributeKind, nowNanos);
    }

    synchronized boolean eraseMagic(
            int mapId, long objectId, long nowNanos, int durationSeconds) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || nowNanos < 0
                || durationSeconds <= 0 || actor.definition.template().boss()) {
            return false;
        }
        actor.eraseMagicStatus.activate(nowNanos, durationSeconds);
        return true;
    }

    synchronized int effectiveMagicResistance(
            int mapId, long objectId, int baseMagicResistance, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive ? baseMagicResistance
                : actor.eraseMagicStatus.effectiveMagicResistance(
                        baseMagicResistance, nowNanos);
    }

    synchronized boolean consumeEraseMagic(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.eraseMagicStatus.consume(nowNanos);
    }

    synchronized BattleEraseMagicStatus.Lease captureEraseMagic(
            int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                ? actor.eraseMagicStatus.capture(nowNanos)
                : BattleEraseMagicStatus.Lease.none();
    }

    synchronized boolean consumeEraseMagic(
            int mapId, long objectId, BattleEraseMagicStatus.Lease lease) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.eraseMagicStatus.consume(lease);
    }

    synchronized long targetObjectId(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        return actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive ? 0 : actor.targetObjectId;
    }

    synchronized int pathFailureCount(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        return actor == null || actor.definition.mapId() != mapId
                ? 0 : actor.pathFailCount;
    }

    synchronized boolean firstFound(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.firstFound;
    }

    synchronized long pendingFamilyTargetObjectId(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        return actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive
                ? 0 : actor.pendingFamilyTargetObjectId;
    }

    synchronized long mobGroupLeaderObjectId(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        return actor == null || actor.definition.mapId() != mapId
                ? 0 : actor.mobGroupLeaderObjectId;
    }

    synchronized List<Snapshot> mobGroupMembers(int mapId, long leaderObjectId) {
        if (leaderObjectId <= 0) return List.of();
        return actors.values().stream()
                .filter(actor -> actor.definition.mapId() == mapId
                        && actor.mobGroupLeaderObjectId == leaderObjectId)
                .map(Actor::snapshot)
                .toList();
    }

    synchronized List<Snapshot> mobGroupLeaders(int mapId, int sourceGroupId) {
        if (sourceGroupId <= 0) return List.of();
        return actors.values().stream()
                .filter(actor -> actor.definition.mapId() == mapId
                        && actor.definition.groupId() == sourceGroupId
                        && actor.mobGroupLeader)
                .map(Actor::snapshot)
                .toList();
    }

    synchronized void clearTarget(int mapId, long objectId, long playerObjectId) {
        Actor actor = actors.get(objectId);
        if (actor != null && actor.definition.mapId() == mapId
                && (playerObjectId == 0 || actor.targetObjectId == playerObjectId)) {
            long clearedObjectId = actor.targetObjectId;
            actor.combatHate.remove(clearedObjectId);
            actor.targetObjectId = 0;
            actor.pathFailCount = 0;
            if (playerObjectId == 0
                    || actor.pendingFamilyTargetObjectId == playerObjectId) {
                actor.pendingFamilyTargetObjectId = 0;
            }
            actor.targetObjectId = maxCombatHateTarget(actor);
        }
    }

    synchronized void clearTargetsForPlayer(long playerObjectId) {
        if (playerObjectId <= 0) return;
        for (Actor actor : actors.values()) {
            actor.combatHate.remove(playerObjectId);
            actor.contributionHate.entrySet().removeIf(entry ->
                    entry.getKey().sourceObjectId() == playerObjectId);
            if (actor.targetObjectId == playerObjectId) {
                actor.targetObjectId = maxCombatHateTarget(actor);
                actor.pathFailCount = 0;
            }
            if (actor.pendingFamilyTargetObjectId == playerObjectId) {
                actor.pendingFamilyTargetObjectId = 0;
            }
        }
    }

    synchronized void transferTargets(long sourceObjectId, long destinationObjectId) {
        if (sourceObjectId <= 0 || destinationObjectId <= 0
                || sourceObjectId == destinationObjectId) return;
        for (Actor actor : actors.values()) {
            Long combat = actor.combatHate.remove(sourceObjectId);
            if (combat != null) {
                actor.combatHate.merge(
                        destinationObjectId, combat, Math::addExact);
            }
            long transferredContribution = 0;
            var iterator = actor.contributionHate.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (entry.getKey().sourceObjectId() != sourceObjectId) continue;
                transferredContribution = Math.addExact(
                        transferredContribution, entry.getValue());
                iterator.remove();
            }
            if (transferredContribution != 0) {
                actor.contributionHate.merge(
                        new ContributorKey(
                                ContributorKind.PLAYER,
                                destinationObjectId, destinationObjectId),
                        transferredContribution, Math::addExact);
            }
            if (actor.targetObjectId == sourceObjectId || combat != null) {
                actor.targetObjectId = maxCombatHateTarget(actor);
                actor.pathFailCount = 0;
            }
            if (actor.pendingFamilyTargetObjectId == sourceObjectId) {
                actor.pendingFamilyTargetObjectId = destinationObjectId;
            }
        }
    }

    private static long maxCombatHateTarget(Actor actor) {
        long targetObjectId = 0;
        long maximumHate = Long.MIN_VALUE;
        for (Map.Entry<Long, Long> entry : actor.combatHate.entrySet()) {
            if (entry.getKey() > 0 && entry.getValue() != null
                    && maximumHate < entry.getValue()) {
                targetObjectId = entry.getKey();
                maximumHate = entry.getValue();
            }
        }
        return targetObjectId;
    }

    synchronized boolean actionReady(int mapId, long objectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && !actionLocked(actor, nowNanos)
                && nowNanos >= actor.nextActionNanos;
    }

    synchronized void defer(int mapId, long objectId, long nowNanos, int delayMillis) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || delayMillis < 0) return;
        actor.nextActionNanos = nowNanos + delayMillis * 1_000_000L;
    }

    synchronized Attack attack(
            int mapId, long objectId, long playerObjectId,
            int playerX, int playerY, long nowNanos) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive
                || actor.targetObjectId != playerObjectId
                || nowNanos < actor.nextActionNanos
                || actionLocked(actor, nowNanos)
                || actor.runtimeAtkspeed <= 0
                || !ClassicWorldMapCatalog.forMap(mapId).attackPosition(
                        actor.x, actor.y, playerX, playerY,
                        actor.definition.template().attackRange())) {
            return null;
        }
        actor.heading = heading(actor.x, actor.y, playerX, playerY);
        actor.pathFailCount = 0;
        flushDeferredFamilyLink(actor, playerObjectId, playerX, playerY);
        actor.nextActionNanos = nowNanos + TimeUnit.MILLISECONDS.toNanos(
                BattleActionInterval.npcSourceMillis(
                        Math.max(100, actor.runtimeAtkspeed),
                        hasteActive(mapId, objectId, nowNanos),
                        slowActive(mapId, objectId, nowNanos),
                        windShackleActive(mapId, objectId, nowNanos)));
        int distance = Math.max(Math.abs(actor.x - playerX),
                Math.abs(actor.y - playerY));
        return new Attack(actor.snapshot(), playerObjectId, playerX, playerY,
                actor.definition.template().projectileAtDistance(distance));
    }

    synchronized NearTeleport nearTeleport(
            int mapId, long objectId, long targetObjectId,
            int targetX, int targetY, Set<GridPoint> occupiedPlayers,
            long nowNanos) {
        return nearTeleport(mapId, objectId, targetObjectId,
                targetX, targetY, occupiedPlayers, nowNanos, 0, 0);
    }

    /**
     * Reproduces the retained pursuit order after mobskill and ordinary attack
     * have both failed.  Trigger fixture 0 uses native random; otherwise 1..100
     * maps to the source's exact {@code nextInt(100)} result.  Landing fixture
     * 0 uses native random; otherwise 1..8 selects the first tested direction.
     */
    synchronized NearTeleport nearTeleport(
            int mapId, long objectId, long targetObjectId,
            int targetX, int targetY, Set<GridPoint> occupiedPlayers,
            long nowNanos, int triggerRollFixture, int landingDirectionFixture) {
        if (occupiedPlayers == null || triggerRollFixture < 0
                || triggerRollFixture > 100 || landingDirectionFixture < 0
                || landingDirectionFixture > 8) {
            throw new IllegalArgumentException("invalid classic near-teleport fixture");
        }
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive
                || actor.targetObjectId != targetObjectId
                || nowNanos < actor.nextActionNanos
                || actionLocked(actor, nowNanos)
                || actor.runtimePassispeed <= 0
                || !ClassicWorldMonsterTeleportCatalog.canTeleport(
                        actor.definition.template())) {
            return null;
        }
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(mapId);
        if (map.attackPosition(actor.x, actor.y, targetX, targetY,
                actor.definition.template().attackRange())) {
            return null;
        }
        int distance = Math.max(Math.abs(actor.x - targetX),
                Math.abs(actor.y - targetY));
        if (actor.firstFound && distance > 3
                && distance < NEAR_TELEPORT_DISTANCE_LIMIT) {
            NearTeleport result = attemptNearTeleport(
                    actor, targetObjectId, targetX, targetY, occupiedPlayers,
                    NearTeleportKind.FIRST_FOUND, -1,
                    landingDirectionFixture);
            if (result != null) {
                actor.firstFound = false;
                return result;
            }
        }

        int randomRoll = triggerRollFixture == 0
                ? actor.random.nextInt(100) : triggerRollFixture - 1;
        if (randomRoll >= NEAR_TELEPORT_RANDOM_PERCENT
                || actor.currentMp < NEAR_TELEPORT_MP_COST
                || distance <= 6 || distance >= NEAR_TELEPORT_DISTANCE_LIMIT) {
            return null;
        }
        return attemptNearTeleport(
                actor, targetObjectId, targetX, targetY, occupiedPlayers,
                NearTeleportKind.RANDOM_PURSUIT, randomRoll,
                landingDirectionFixture);
    }

    private NearTeleport attemptNearTeleport(
            Actor actor, long targetObjectId, int targetX, int targetY,
            Set<GridPoint> occupiedPlayers, NearTeleportKind kind,
            int randomRoll, int landingDirectionFixture) {
        int mapId = actor.definition.mapId();
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(mapId);
        int startingDirection = landingDirectionFixture == 0
                ? actor.random.nextInt(8) : landingDirectionFixture - 1;
        for (int offset = 0; offset < 8; offset++) {
            int direction = (startingDirection + offset) & 7;
            int landingX = targetX + HEADING_X[direction];
            int landingY = targetY + HEADING_Y[direction];
            GridPoint landing = new GridPoint(landingX, landingY);
            if (!map.standingPassable(landingX, landingY)
                    || isOccupied(new Cell(mapId, landingX, landingY))
                    || occupiedPlayers.contains(landing)
                    || NativeStaticNpcMotion.occupies(
                            mapId, landingX, landingY, this)) {
                continue;
            }
            Snapshot before = actor.snapshot();
            vacate(new Cell(mapId, actor.x, actor.y));
            actor.x = landingX;
            actor.y = landingY;
            actor.heading = (direction + 4) & 7;
            actor.currentMp -= Math.min(actor.currentMp,
                    NEAR_TELEPORT_MP_COST);
            actor.pathFailCount = 0;
            occupy(new Cell(mapId, actor.x, actor.y));
            updateWandered(actor);
            Snapshot after = actor.snapshot();
            return new NearTeleport(kind, before, after,
                    targetObjectId, targetX, targetY, startingDirection,
                    randomRoll, before.currentMp() - after.currentMp());
        }
        return null;
    }

    /**
     * Attempts the retained Type=1 mobskill branch before Type=2 magic and
     * ordinary attack. Branch fixture: 0=native random, 1=force skill,
     * 2=force ordinary. Trigger fixture: 0=native random, otherwise the exact
     * one-based TriRnd roll.
     */
    synchronized PhysicalSkillAttack physicalSkillAttack(
            int mapId, long objectId, long targetObjectId,
            int targetX, int targetY, long nowNanos,
            int branchFixture, int triggerRollFixture) {
        if (branchFixture < 0 || branchFixture > 2
                || triggerRollFixture < 0 || triggerRollFixture > 100) {
            throw new IllegalArgumentException("invalid classic mob physical fixture");
        }
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive
                || actor.targetObjectId != targetObjectId
                || nowNanos < actor.nextActionNanos
                || actionLocked(actor, nowNanos)) {
            return null;
        }
        List<ClassicWorldMonsterPhysicalSkillCatalog.Definition> definitions =
                mapId == ClassicWorldCkewCatalog.MAP_ID
                        ? ClassicWorldCkewMonsterCatalog.physicalSkills(
                                actor.definition.template().npcId())
                        : ClassicWorldMonsterPhysicalSkillCatalog.forNpc(
                                actor.definition.template().npcId());
        if (definitions.isEmpty()) return null;

        int distance = Math.max(Math.abs(actor.x - targetX),
                Math.abs(actor.y - targetY));
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(mapId);
        List<ClassicWorldMonsterPhysicalSkillCatalog.Definition> eligible =
                definitions.stream()
                        .filter(skill -> skill.triggerDistance(distance))
                        .filter(skill -> skill.triggerCount() == 0
                                || actor.physicalSkillUseCount.getOrDefault(
                                        skill.actNo(), 0) < skill.triggerCount())
                        .filter(skill -> map.attackPosition(
                                actor.x, actor.y, targetX, targetY, skill.range()))
                        .toList();
        if (eligible.isEmpty()) return null;

        boolean ordinaryAttackPosition = map.attackPosition(
                actor.x, actor.y, targetX, targetY,
                actor.definition.template().attackRange());
        if (ordinaryAttackPosition) {
            boolean ordinaryBranch = branchFixture == 2
                    || (branchFixture == 0 && actor.random.nextInt(2) >= 1);
            if (ordinaryBranch) return null;
        }

        int triggerRoll = triggerRollFixture > 0
                ? triggerRollFixture : actor.random.nextInt(100) + 1;
        List<ClassicWorldMonsterPhysicalSkillCatalog.Definition> triggered =
                eligible.stream().filter(skill -> skill.triggerRoll(triggerRoll)).toList();
        if (triggered.isEmpty()) return null;
        ClassicWorldMonsterPhysicalSkillCatalog.Definition skill = triggered.size() == 1
                ? triggered.get(0) : triggered.get(actor.random.nextInt(triggered.size()));

        flushDeferredFamilyLink(actor, targetObjectId, targetX, targetY);
        actor.pathFailCount = 0;
        actor.heading = heading(actor.x, actor.y, targetX, targetY);
        actor.nextActionNanos = nowNanos + TimeUnit.MILLISECONDS.toNanos(
                BattleActionInterval.npcSourceMillis(
                        Math.max(100, actor.runtimeAtkspeed),
                        hasteActive(mapId, objectId, nowNanos),
                        slowActive(mapId, objectId, nowNanos),
                        windShackleActive(mapId, objectId, nowNanos)));
        actor.physicalSkillUseCount.merge(skill.actNo(), 1, Integer::sum);
        return new PhysicalSkillAttack(actor.snapshot(), targetObjectId,
                targetX, targetY, skill, triggerRoll, true);
    }

    /**
     * Reproduces retained {@code L1MobSkillUse.skillUse}: map-2000 Type1 and
     * Type2 rows share one act-number-ordered candidate pool. Inside ordinary
     * attack range the NPC first takes the native 50/50 ordinary-vs-skill
     * branch and independently rolls every candidate's TriRnd. During
     * pursuit, TriRnd is deliberately ignored. Validation that belongs to
     * {@code checkUseSkill} is performed only after one row is selected, so a
     * failed selected row never falls through to another candidate.
     */
    synchronized CkewSkillAttack ckewSkillAttack(
            int mapId, long objectId, long targetObjectId,
            int targetX, int targetY, long nowNanos,
            Set<GridPoint> hostileTargetCells,
            int branchFixture, int triggerRollFixture,
            int selectionFixture) {
        if (mapId != ClassicWorldCkewCatalog.MAP_ID
                || branchFixture < 0 || branchFixture > 2
                || triggerRollFixture < 0 || triggerRollFixture > 100
                || selectionFixture < 0 || hostileTargetCells == null) {
            throw new IllegalArgumentException("invalid classic CKEW skill fixture");
        }
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive
                || actor.targetObjectId != targetObjectId
                || nowNanos < actor.nextActionNanos
                || actionLocked(actor, nowNanos)) {
            return null;
        }

        record Candidate(
                int actNo,
                ClassicWorldMonsterPhysicalSkillCatalog.Definition physical,
                ClassicWorldMonsterMagicSkillCatalog.Definition magic,
                ClassicWorldMonsterStatusSkillCatalog.Definition status) {
            Candidate {
                int branches = (physical == null ? 0 : 1)
                        + (magic == null ? 0 : 1)
                        + (status == null ? 0 : 1);
                if (branches != 1) {
                    throw new IllegalArgumentException("invalid CKEW skill candidate");
                }
            }

            int triggerRange() {
                return physical != null
                        ? physical.triggerRange()
                        : magic != null ? magic.triggerRange()
                        : status.triggerRange();
            }

            boolean triggerDistance(int distance) {
                return physical != null
                        ? physical.triggerDistance(distance)
                        : magic != null ? magic.triggerDistance(distance)
                        : status.triggerDistance(distance);
            }

            boolean triggerRoll(int roll) {
                return physical != null
                        ? physical.triggerRoll(roll)
                        : magic != null ? magic.triggerRoll(roll)
                        : status.triggerRoll(roll);
            }
        }
        int npcId = actor.definition.template().npcId();
        List<Candidate> candidates = new ArrayList<>();
        for (ClassicWorldMonsterPhysicalSkillCatalog.Definition physical
                : ClassicWorldCkewMonsterCatalog.physicalSkills(npcId)) {
            candidates.add(new Candidate(
                    physical.actNo(), physical, null, null));
        }
        for (ClassicWorldMonsterMagicSkillCatalog.Definition magic
                : ClassicWorldMonsterMagicSkillCatalog.forNpcAll(npcId)) {
            candidates.add(new Candidate(magic.actNo(), null, magic, null));
        }
        for (ClassicWorldMonsterStatusSkillCatalog.Definition status
                : ClassicWorldMonsterStatusSkillCatalog.forNpc(npcId)) {
            candidates.add(new Candidate(status.actNo(), null, null, status));
        }
        if (candidates.isEmpty()) return null;
        candidates.sort(java.util.Comparator.comparingInt(Candidate::actNo));

        int distance = Math.max(Math.abs(actor.x - targetX),
                Math.abs(actor.y - targetY));
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(mapId);
        List<Candidate> eligible = candidates.stream()
                .filter(candidate -> candidate.triggerDistance(distance))
                .filter(candidate -> candidate.physical() == null
                        || candidate.physical().triggerCount() == 0
                        || actor.physicalSkillUseCount.getOrDefault(
                                candidate.actNo(), 0)
                                < candidate.physical().triggerCount())
                .filter(candidate -> map.attackPosition(
                        actor.x, actor.y, targetX, targetY,
                        Math.abs(candidate.triggerRange())))
                .toList();
        if (eligible.isEmpty()) return null;

        boolean ordinaryAttackPosition = map.attackPosition(
                actor.x, actor.y, targetX, targetY,
                actor.definition.template().attackRange());
        if (ordinaryAttackPosition) {
            boolean ordinaryBranch = branchFixture == 2
                    || (branchFixture == 0 && actor.random.nextInt(2) >= 1);
            if (ordinaryBranch) return null;
        }

        record Triggered(Candidate candidate, int triggerRoll) {}
        List<Triggered> triggered = new ArrayList<>();
        for (Candidate candidate : eligible) {
            int roll = 0;
            if (ordinaryAttackPosition) {
                roll = triggerRollFixture > 0
                        ? triggerRollFixture : actor.random.nextInt(100) + 1;
                if (!candidate.triggerRoll(roll)) continue;
            }
            triggered.add(new Triggered(candidate, roll));
        }
        if (triggered.isEmpty() || selectionFixture > triggered.size()) return null;
        Triggered selected = selectionFixture > 0
                ? triggered.get(selectionFixture - 1)
                : triggered.get(actor.random.nextInt(triggered.size()));
        Candidate candidate = selected.candidate();

        if (candidate.physical() != null) {
            ClassicWorldMonsterPhysicalSkillCatalog.Definition skill =
                    candidate.physical();
            if (!map.attackPosition(actor.x, actor.y,
                    targetX, targetY, skill.range())
                    || skill.triggerCount() > 0
                    && actor.physicalSkillUseCount.getOrDefault(
                            skill.actNo(), 0) >= skill.triggerCount()) {
                return null;
            }
            actor.heading = heading(actor.x, actor.y, targetX, targetY);
            actor.nextActionNanos = nowNanos + TimeUnit.MILLISECONDS.toNanos(
                    BattleActionInterval.npcSourceMillis(
                            Math.max(100, actor.runtimeAtkspeed),
                            hasteActive(mapId, objectId, nowNanos),
                            slowActive(mapId, objectId, nowNanos),
                            windShackleActive(mapId, objectId, nowNanos)));
            actor.physicalSkillUseCount.merge(skill.actNo(), 1, Integer::sum);
            actor.pathFailCount = 0;
            return new CkewSkillAttack(new PhysicalSkillAttack(
                    actor.snapshot(), targetObjectId, targetX, targetY, skill,
                    selected.triggerRoll(), ordinaryAttackPosition), null, null);
        }

        if (consumeSilenceForSpell(mapId, objectId, nowNanos)) return null;

        if (candidate.status() != null) {
            ClassicWorldMonsterStatusSkillCatalog.Definition skill =
                    candidate.status();
            if (nowNanos < actor.skillReadyNanos.getOrDefault(
                    skill.actNo(), 0L)
                    || !map.attackPosition(actor.x, actor.y,
                            targetX, targetY, skill.maximumRange())
                    || actor.currentMp < skill.mpCost()) {
                return null;
            }
            actor.currentMp -= skill.mpCost();
            actor.heading = heading(actor.x, actor.y, targetX, targetY);
            actor.nextActionNanos = nowNanos + TimeUnit.MILLISECONDS.toNanos(
                    BattleActionInterval.npcSourceMillis(
                            Math.max(100, skill.actionDelayMillis()),
                            hasteActive(mapId, objectId, nowNanos),
                            slowActive(mapId, objectId, nowNanos),
                            windShackleActive(mapId, objectId, nowNanos)));
            actor.skillReadyNanos.put(skill.actNo(), nowNanos
                    + skill.reuseDelayMillis() * 1_000_000L);
            actor.pathFailCount = 0;
            return new CkewSkillAttack(null, null, new StatusSkillAttack(
                    actor.snapshot(), targetObjectId, targetX, targetY, skill,
                    selected.triggerRoll(), ordinaryAttackPosition));
        }

        ClassicWorldMonsterMagicSkillCatalog.Definition skill = candidate.magic();
        if (nowNanos < actor.skillReadyNanos.getOrDefault(skill.actNo(), 0L)) {
            return null;
        }
        if (skill.targeting()
                == ClassicWorldMonsterMagicSkillCatalog.Targeting.CASTER_CENTERED) {
            boolean hasAreaTarget = hostileTargetCells.stream().anyMatch(cell ->
                    map.attackPosition(actor.x, actor.y,
                            cell.x(), cell.y(), skill.areaRadius()));
            if (!hasAreaTarget) return null;
        } else if (!map.attackPosition(actor.x, actor.y,
                targetX, targetY, skill.maximumRange())) {
            return null;
        }
        if (actor.currentMp < skill.mpCost()) return null;

        flushDeferredFamilyLink(actor, targetObjectId, targetX, targetY);
        actor.currentMp -= skill.mpCost();
        if (skill.targeting()
                != ClassicWorldMonsterMagicSkillCatalog.Targeting.CASTER_CENTERED) {
            actor.heading = heading(actor.x, actor.y, targetX, targetY);
        }
        actor.nextActionNanos = nowNanos + TimeUnit.MILLISECONDS.toNanos(
                BattleActionInterval.npcSourceMillis(
                        Math.max(100, skill.actionDelayMillis()),
                        hasteActive(mapId, objectId, nowNanos),
                        slowActive(mapId, objectId, nowNanos),
                        windShackleActive(mapId, objectId, nowNanos)));
        actor.skillReadyNanos.put(skill.actNo(), nowNanos
                + skill.reuseDelayMillis() * 1_000_000L);
        actor.pathFailCount = 0;
        return new CkewSkillAttack(null, new MagicAttack(
                actor.snapshot(), targetObjectId, targetX, targetY, skill,
                selected.triggerRoll(), ordinaryAttackPosition), null);
    }

    /**
     * Attempts the retained Type=2 mobskill branch before ordinary attack or
     * pursuit. Branch fixture: 0=native random, 1=force skill, 2=force physical.
     * Trigger fixture: 0=native random, otherwise the exact one-based TriRnd roll.
     */
    synchronized MagicAttack magicAttack(
            int mapId, long objectId, long targetObjectId,
            int targetX, int targetY, long nowNanos,
            int branchFixture, int triggerRollFixture) {
        return magicAttack(mapId, objectId, targetObjectId,
                targetX, targetY, nowNanos,
                Set.of(new GridPoint(targetX, targetY)),
                branchFixture, triggerRollFixture, 0);
    }

    /** Test-only selection fixture is one-based after native eligibility filtering. */
    synchronized MagicAttack magicAttack(
            int mapId, long objectId, long targetObjectId,
            int targetX, int targetY, long nowNanos,
            int branchFixture, int triggerRollFixture,
            int selectionFixture) {
        return magicAttack(mapId, objectId, targetObjectId,
                targetX, targetY, nowNanos,
                Set.of(new GridPoint(targetX, targetY)),
                branchFixture, triggerRollFixture, selectionFixture);
    }

    synchronized MagicAttack magicAttack(
            int mapId, long objectId, long targetObjectId,
            int targetX, int targetY, long nowNanos,
            Set<GridPoint> hostileTargetCells,
            int branchFixture, int triggerRollFixture,
            int selectionFixture) {
        if (branchFixture < 0 || branchFixture > 2
                || triggerRollFixture < 0 || triggerRollFixture > 100
                || selectionFixture < 0 || hostileTargetCells == null) {
            throw new IllegalArgumentException("invalid classic mob magic fixture");
        }
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive
                || actor.targetObjectId != targetObjectId
                || nowNanos < actor.nextActionNanos
                || actionLocked(actor, nowNanos)
                || areaSilenceActive(mapId, objectId, nowNanos)) {
            return null;
        }
        List<ClassicWorldMonsterMagicSkillCatalog.Definition> definitions =
                ClassicWorldMonsterMagicSkillCatalog.forNpcAll(
                        actor.definition.template().npcId());
        if (definitions.isEmpty()) return null;
        int distance = Math.max(Math.abs(actor.x - targetX),
                Math.abs(actor.y - targetY));
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(mapId);
        List<ClassicWorldMonsterMagicSkillCatalog.Definition> eligible =
                definitions.stream()
                        .filter(skill -> nowNanos >= actor.skillReadyNanos
                                .getOrDefault(skill.actNo(), 0L))
                        .filter(skill -> skill.triggerDistance(distance))
                        .filter(skill -> map.attackPosition(
                                actor.x, actor.y, targetX, targetY,
                                Math.abs(skill.triggerRange())))
                        .toList();
        if (eligible.isEmpty()) return null;

        boolean ordinaryAttackPosition =
                map.attackPosition(
                        actor.x, actor.y, targetX, targetY,
                        actor.definition.template().attackRange());
        if (ordinaryAttackPosition) {
            boolean physicalBranch = branchFixture == 2
                    || (branchFixture == 0 && actor.random.nextInt(2) >= 1);
            if (physicalBranch) return null;
        }

        // Retained L1NpcInstance only applies each row's independent TriRnd
        // while the target is in the ordinary attack position. During pursuit,
        // skillUse(target,false) deliberately ignores Type1/Type2 TriRnd.
        record TriggeredMagic(
                ClassicWorldMonsterMagicSkillCatalog.Definition skill,
                int triggerRoll) {}
        List<TriggeredMagic> triggered = new ArrayList<>();
        for (ClassicWorldMonsterMagicSkillCatalog.Definition candidate : eligible) {
            int roll = 0;
            if (ordinaryAttackPosition) {
                roll = triggerRollFixture > 0
                        ? triggerRollFixture : actor.random.nextInt(100) + 1;
                if (!candidate.triggerRoll(roll)) continue;
            }
            triggered.add(new TriggeredMagic(candidate, roll));
        }
        if (triggered.isEmpty() || selectionFixture > triggered.size()) return null;
        TriggeredMagic selected = selectionFixture > 0
                ? triggered.get(selectionFixture - 1)
                : triggered.get(actor.random.nextInt(triggered.size()));
        ClassicWorldMonsterMagicSkillCatalog.Definition skill = selected.skill();

        // checkUseSkill resets target="none" to the caster. Such a spell only
        // succeeds when its resulting caster-centred target list is non-empty.
        if (skill.targeting()
                == ClassicWorldMonsterMagicSkillCatalog.Targeting.CASTER_CENTERED) {
            boolean hasAreaTarget = hostileTargetCells.stream().anyMatch(cell ->
                    map.attackPosition(actor.x, actor.y,
                            cell.x(), cell.y(), skill.areaRadius()));
            if (!hasAreaTarget) return null;
        } else if (!map.attackPosition(actor.x, actor.y,
                targetX, targetY, skill.maximumRange())) {
            return null;
        }
        if (actor.damagePoisonStatus.consumeSilenceForNpcSpell(nowNanos)) {
            return null;
        }
        if (actor.currentMp < skill.mpCost()) return null;

        actor.currentMp -= skill.mpCost();
        actor.pathFailCount = 0;
        if (skill.targeting()
                != ClassicWorldMonsterMagicSkillCatalog.Targeting.CASTER_CENTERED) {
            actor.heading = heading(actor.x, actor.y, targetX, targetY);
        }
        actor.nextActionNanos = nowNanos + TimeUnit.MILLISECONDS.toNanos(
                BattleActionInterval.npcSourceMillis(
                        Math.max(100, skill.actionDelayMillis()),
                        hasteActive(mapId, objectId, nowNanos),
                        slowActive(mapId, objectId, nowNanos),
                        windShackleActive(mapId, objectId, nowNanos)));
        actor.skillReadyNanos.put(skill.actNo(), nowNanos
                + skill.reuseDelayMillis() * 1_000_000L);
        return new MagicAttack(actor.snapshot(), targetObjectId,
                targetX, targetY, skill, selected.triggerRoll(),
                ordinaryAttackPosition);
    }

    synchronized Movement moveToward(int n, long l, long l2, int n4, int n5, Set<GridPoint> set, long l3) {
        Actor actor = this.actors.get(l);
        if (actor == null || actor.definition.mapId() != n || !actor.present || !actor.alive || actor.targetObjectId != l2 || l3 < actor.nextActionNanos || ClassicWorldMonsterPopulation.actionLocked(actor, l3) || actor.runtimePassispeed <= 0) {
            return null;
        }
        ClassicWorldMapCatalog.ClassicMap classicMap = ClassicWorldMapCatalog.forMap(n);
        if (actor.damagePoisonStatus.blindActive(l3) && Math.max(Math.abs(actor.x - n4), Math.abs(actor.y - n5)) >= 2) {
            actor.pathFailCount = 0;
            actor.nextActionNanos = l3 + TimeUnit.MILLISECONDS.toNanos(200L);
            return null;
        }
        ClassicWorldNpcPathfinder.Result result = ClassicWorldNpcPathfinder.nextStep(classicMap, actor.x, actor.y, n4, n5, actor.definition.template().attackRange(), (n2, n3) -> this.isOccupied(new Cell(n, n2, n3)) || set.contains(new GridPoint(n2, n3)) || NativeStaticNpcMotion.occupies(n, n2, n3, this));
        if (!result.moved()) {
            boolean bl = actor.definition.template().boss();
            if (result.failure() != ClassicWorldNpcPathfinder.Failure.UNREACHABLE || bl) {
                actor.pathFailCount = 0;
            } else {
                ++actor.pathFailCount;
                if (actor.pathFailCount >= 3) {
                    actor.combatHate.remove(l2);
                    actor.targetObjectId = ClassicWorldMonsterPopulation.maxCombatHateTarget(actor);
                    if (actor.pendingFamilyTargetObjectId == l2) {
                        actor.pendingFamilyTargetObjectId = 0L;
                    }
                    actor.pathFailCount = 0;
                }
            }
            actor.nextActionNanos = l3 + TimeUnit.MILLISECONDS.toNanos(200L);
            return null;
        }
        actor.pathFailCount = 0;
        Snapshot snapshot = actor.snapshot();
        this.flushDeferredFamilyLink(actor, l2, n4, n5);
        this.vacate(new Cell(n, actor.x, actor.y));
        actor.x = result.destinationX();
        actor.y = result.destinationY();
        actor.heading = result.heading();
        this.occupy(new Cell(n, actor.x, actor.y));
        Snapshot snapshot2 = actor.snapshot();
        this.applyMovementDistance(actor);
        this.updateWandered(actor);
        actor.nextActionNanos = l3 + TimeUnit.MILLISECONDS.toNanos(BattleActionInterval.npcSourceMillis(Math.max(100, actor.runtimePassispeed), this.hasteActive(n, l, l3), this.slowActive(n, l, l3)));
        return new Movement(snapshot, snapshot2, actor.snapshot());
    }

    /**
     * Retained {@code L1NpcInstance.ascape}: an actor without an attack
     * animation but with a passive movement speed runs in the exact opposite
     * direction from its target.  The source keeps the target through tile
     * distance 17 (only 1 while DARKNESS is active) and uses NpcMove's full
     * eight-direction side-step order when the reverse tile is blocked.
     */
    synchronized Movement moveAway(
            int mapId, long objectId, long playerObjectId,
            int playerX, int playerY, Set<GridPoint> occupiedPlayers,
            long nowNanos) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive
                || actor.targetObjectId != playerObjectId
                || nowNanos < actor.nextActionNanos
                || actionLocked(actor, nowNanos)
                || actor.runtimeAtkspeed != 0
                || actor.runtimePassispeed <= 0) {
            return null;
        }
        int distance = Math.max(
                Math.abs(actor.x - playerX), Math.abs(actor.y - playerY));
        int escapeDistance = actor.damagePoisonStatus.blindActive(nowNanos)
                ? 1 : 17;
        if (distance > escapeDistance) {
            actor.combatHate.remove(playerObjectId);
            actor.targetObjectId = maxCombatHateTarget(actor);
            if (actor.pendingFamilyTargetObjectId == playerObjectId) {
                actor.pendingFamilyTargetObjectId = 0;
            }
            return null;
        }

        int toward = heading(actor.x, actor.y, playerX, playerY);
        if (toward < 0) return null;
        int reverse = (toward + 4) & 7;
        // NpcMove.checkObject: h, -1, +1, +2, -2, -3, +3, opposite.
        int[] headings = {
                reverse, (reverse + 7) & 7, (reverse + 1) & 7,
                (reverse + 2) & 7, (reverse + 6) & 7,
                (reverse + 5) & 7, (reverse + 3) & 7,
                (reverse + 4) & 7
        };
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(mapId);
        for (int candidate : headings) {
            int x = actor.x + HEADING_X[candidate];
            int y = actor.y + HEADING_Y[candidate];
            Cell destination = new Cell(mapId, x, y);
            if (!map.canStep(actor.x, actor.y, x, y)
                    || isOccupied(destination)
                    || occupiedPlayers.contains(new GridPoint(x, y))
                    || NativeStaticNpcMotion.occupies(mapId, x, y, this)) {
                continue;
            }
            Snapshot before = actor.snapshot();
            vacate(new Cell(mapId, actor.x, actor.y));
            actor.x = x;
            actor.y = y;
            actor.heading = candidate;
            occupy(destination);
            Snapshot moved = actor.snapshot();
            applyMovementDistance(actor);
            updateWandered(actor);
            actor.nextActionNanos = nowNanos + TimeUnit.MILLISECONDS.toNanos(
                    BattleActionInterval.npcSourceMillis(
                            Math.max(100,
                                    actor.runtimePassispeed),
                            hasteActive(mapId, objectId, nowNanos),
                            slowActive(mapId, objectId, nowNanos)));
            return new Movement(before, moved, actor.snapshot());
        }
        // ascape still sleeps for passispeed after setDirectionMove(-1).
        actor.nextActionNanos = nowNanos + TimeUnit.MILLISECONDS.toNanos(
                BattleActionInterval.npcSourceMillis(
                        Math.max(100, actor.runtimePassispeed),
                        hasteActive(mapId, objectId, nowNanos),
                        slowActive(mapId, objectId, nowNanos)));
        return null;
    }

    synchronized void abandonItemTargetsForCombat(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || actor.targetObjectId == 0) return;
        clearItemTargets(actor);
    }

    /** Retained {@code onTarget}: mark this AI lifetime active and clear item targets. */
    synchronized void beginTargetAction(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || actor.targetObjectId == 0) {
            return;
        }
        actor.actived = true;
        activatedActors.add(objectId);
        clearItemTargets(actor);
    }

    /**
     * Retained {@code L1MonsterInstance.onItemUse} doppel branch. The source
     * copies the player's persistent class id (not a temporary polymorph),
     * name, title and lawful value, then rebuilds the same object id before
     * {@code onTarget} marks this AI lifetime active.
     */
    synchronized DoppelTransformation doppelTransform(
            int mapId, long objectId, long playerObjectId,
            String playerName, String playerTitle,
            int playerLawful, int playerClassId) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || actor.actived
                || actor.hiddenStatus != HIDDEN_STATUS_NONE
                || actor.targetObjectId != playerObjectId
                || !actor.definition.template().implementation()
                        .equals("L1Monster")
                || !actor.definition.template().doppel()) {
            return null;
        }
        if (playerObjectId <= 0 || playerName == null || playerTitle == null
                || playerLawful < Short.MIN_VALUE || playerLawful > Short.MAX_VALUE
                || playerClassId < 0 || playerClassId > 0xFFFF) {
            throw new IllegalArgumentException("invalid doppel player appearance");
        }
        Snapshot before = actor.snapshot();
        actor.runtimeNameId = playerName;
        actor.runtimeTitle = playerTitle;
        actor.runtimeLawful = playerLawful;
        actor.runtimeGfxId = playerClassId;
        actor.runtimePassispeed = 640;
        actor.runtimeAtkspeed = 900;
        return new DoppelTransformation(before, actor.snapshot(), playerObjectId);
    }

    synchronized List<ItemUse> itemUse(
            int mapId, long objectId, long nowNanos) {
        return itemUse(mapId, objectId, nowNanos, -1, -1);
    }

    /**
     * Retained monster item order: one initial target-side haste attempt,
     * followed by a healing attempt whenever HP is below forty percent.
     * Source probabilities use {@code nextInt(100) > chance}, hence inclusive
     * success domains 0..40 and 0..50.
     */
    synchronized List<ItemUse> itemUse(
            int mapId, long objectId, long nowNanos,
            int hasteRollFixture, int healRollFixture) {
        if (nowNanos < 0 || hasteRollFixture < -1 || hasteRollFixture > 99
                || healRollFixture < -1 || healRollFixture > 99) {
            throw new IllegalArgumentException("invalid monster item-use input");
        }
        List<ItemUse> result = new ArrayList<>(2);
        ItemUse haste = itemUseHaste(
                mapId, objectId, nowNanos, hasteRollFixture);
        if (haste != null) result.add(haste);
        ItemUse healing = itemUseHealing(
                mapId, objectId, nowNanos, healRollFixture);
        if (healing != null) result.add(healing);
        return List.copyOf(result);
    }

    synchronized ItemUse itemUseHaste(
            int mapId, long objectId, long nowNanos) {
        return itemUseHaste(mapId, objectId, nowNanos, -1);
    }

    synchronized ItemUse itemUseHaste(
            int mapId, long objectId, long nowNanos, int rollFixture) {
        if (nowNanos < 0 || rollFixture < -1 || rollFixture > 99) {
            throw new IllegalArgumentException("invalid monster haste-use input");
        }
        Actor actor = actors.get(objectId);
        if (!itemUseEligible(actor, mapId, nowNanos)
                || actor.actived || actor.targetObjectId == 0
                || actor.damagePoisonStatus.decayPotionActive(nowNanos)) {
            return null;
        }
        int roll = rollFixture >= 0 ? rollFixture : actor.random.nextInt(100);
        if (roll > 40 || hasteActive(mapId, objectId, nowNanos)) return null;
        long catalogId = firstConsumableCatalog(actor,
                BLESSED_GREATER_HASTE_POTION,
                GREATER_HASTE_POTION,
                BLESSED_HASTE_POTION,
                InventoryCatalog.SELF_HASTE_POTION);
        if (catalogId == 0) return null;
        int durationSeconds = switch ((int) catalogId) {
            case (int) BLESSED_GREATER_HASTE_POTION -> 2100;
            case (int) GREATER_HASTE_POTION -> 1800;
            case (int) BLESSED_HASTE_POTION -> 350;
            default -> 300;
        };
        Snapshot before = actor.snapshot();
        actor.slowExpiresAtNanos = 0;
        actor.hasteExpiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        return new ItemUse(ItemUseKind.HASTE,
                before, actor.snapshot(), catalogId, 191, durationSeconds, 0);
    }

    synchronized ItemUse itemUseHealing(
            int mapId, long objectId, long nowNanos) {
        return itemUseHealing(mapId, objectId, nowNanos, -1);
    }

    synchronized ItemUse itemUseHealing(
            int mapId, long objectId, long nowNanos, int rollFixture) {
        if (nowNanos < 0 || rollFixture < -1 || rollFixture > 99) {
            throw new IllegalArgumentException("invalid monster healing-use input");
        }
        Actor actor = actors.get(objectId);
        if (!itemUseEligible(actor, mapId, nowNanos)
                || actor.damagePoisonStatus.decayPotionActive(nowNanos)
                || (long) actor.currentHp * 100
                        / actor.definition.template().hp() >= 40) {
            return null;
        }
        int roll = rollFixture >= 0 ? rollFixture : actor.random.nextInt(100);
        if (roll > 50) return null;
        long catalogId = firstConsumableCatalog(actor,
                InventoryCatalog.GREATER_HEALING_POTION,
                InventoryCatalog.EXTRA_HEALING_POTION,
                InventoryCatalog.NATIVE_HEALING_POTION);
        if (catalogId == 0) return null;
        int baseHealing = switch ((int) catalogId) {
            case (int) InventoryCatalog.GREATER_HEALING_POTION -> 75;
            case (int) InventoryCatalog.EXTRA_HEALING_POTION -> 45;
            default -> 15;
        };
        int effectGfx = switch ((int) catalogId) {
            case (int) InventoryCatalog.GREATER_HEALING_POTION -> 197;
            case (int) InventoryCatalog.EXTRA_HEALING_POTION -> 194;
            default -> 189;
        };
        int requestedHealing = BattlePotionRecovery.adjustedAmount(
                baseHealing, 0, 0,
                nowNanos < actor.polluteWaterExpiresAtNanos,
                actor.damagePoisonStatus
                        .waterDragonDirtyCurrentActive(nowNanos),
                actor.damagePoisonStatus
                        .waterDragonPotionErosionActive(nowNanos));
        Snapshot before = actor.snapshot();
        actor.currentHp = Math.max(0, Math.min(
                actor.definition.template().hp(),
                Math.addExact(actor.currentHp, requestedHealing)));
        actor.alive = actor.currentHp > 0;
        return new ItemUse(ItemUseKind.HEAL,
                before, actor.snapshot(), catalogId, effectGfx, 0,
                actor.currentHp - before.currentHp());
    }

    private static boolean itemUseEligible(
            Actor actor, int mapId, long nowNanos) {
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && actor.definition.template().implementation()
                        .equals("L1Monster")
                && actor.hiddenStatus == HIDDEN_STATUS_NONE
                && nowNanos >= actor.nextActionNanos;
    }

    private static long firstConsumableCatalog(Actor actor, long... catalogIds) {
        for (long catalogId : catalogIds) {
            if (actor.releasedItems.removeCatalog(catalogId, 1)) {
                return catalogId;
            }
        }
        return 0;
    }

    synchronized ItemAction itemAction(int n, long l, List<ClassicWorldGroundItems.Item> list, Set<GridPoint> set, long l2) {
        return this.itemAction(n, l, list, set, l2, -1);
    }

    /**
     * Retained {@code NpcAI} item branch: validate the current queue, choose a
     * random visible ground inventory, then either pick from an adjacent tile
     * or take one {@code NpcMove.checkObject} pursuit step.
     */
    synchronized ItemAction itemAction(int n, long l, List<ClassicWorldGroundItems.Item> list, Set<GridPoint> set, long l2, int n2) {
        if (list == null || set == null || n2 < -1) {
            throw new IllegalArgumentException("invalid monster item action input");
        }
        Actor actor = this.actors.get(l);
        if (actor == null || actor.definition.mapId() != n || !actor.present || !actor.alive || actor.targetObjectId != 0L || actor.hiddenStatus != 0 || l2 < actor.nextActionNanos || ClassicWorldMonsterPopulation.actionLocked(actor, l2)) {
            return null;
        }
        ClassicWorldGroundItems.Item item = ClassicWorldMonsterPopulation.checkTargetItem(actor, list);
        if (item == null) {
            if (!ClassicWorldMonsterPickupCatalog.enabled(actor.definition.template())) {
                return null;
            }
            item = ClassicWorldMonsterPopulation.searchTargetItem(actor, list, n2);
        }
        if (item == null) {
            return null;
        }
        int n3 = Math.max(Math.abs(actor.x - item.x()), Math.abs(actor.y - item.y()));
        if (n3 == 1) {
            return new ItemAction(ItemActionKind.PICKUP_READY, item.objectId(), null);
        }
        int n4 = ClassicWorldMonsterPopulation.heading(actor.x, actor.y, item.x(), item.y());
        if (n4 < 0) {
            ClassicWorldMonsterPopulation.abandonCurrentItemTarget(actor, item.objectId());
            ClassicWorldMonsterPopulation.deferIdle(actor, l2);
            return new ItemAction(ItemActionKind.ABANDONED, item.objectId(), null);
        }
        int[] nArray = new int[]{n4, n4 + 7 & 7, n4 + 1 & 7, n4 + 2 & 7, n4 + 6 & 7, n4 + 5 & 7, n4 + 3 & 7, n4 + 4 & 7};
        ClassicWorldMapCatalog.ClassicMap classicMap = ClassicWorldMapCatalog.forMap(n);
        for (int n5 : nArray) {
            int n6 = actor.x + HEADING_X[n5];
            int n7 = actor.y + HEADING_Y[n5];
            Cell cell = new Cell(n, n6, n7);
            if (!(!classicMap.canStep(actor.x, actor.y, n6, n7) || this.isOccupied(cell) || set.contains(new GridPoint(n6, n7)) || NativeStaticNpcMotion.occupies(n, n6, n7, this))) {
            Snapshot snapshot = actor.snapshot();
            this.vacate(new Cell(n, actor.x, actor.y));
            actor.x = n6;
            actor.y = n7;
            actor.heading = n5;
            this.occupy(cell);
            Snapshot snapshot2 = actor.snapshot();
            this.applyMovementDistance(actor);
            this.updateWandered(actor);
            actor.nextActionNanos = l2 + TimeUnit.MILLISECONDS.toNanos(BattleActionInterval.npcSourceMillis(Math.max(100, actor.runtimePassispeed), this.hasteActive(n, l, l2), this.slowActive(n, l, l2), this.windShackleActive(n, l, l2)));
            return new ItemAction(ItemActionKind.MOVED, item.objectId(), new Movement(snapshot, snapshot2, actor.snapshot()));

            }
}
        ClassicWorldMonsterPopulation.abandonCurrentItemTarget(actor, item.objectId());
        ClassicWorldMonsterPopulation.deferIdle(actor, l2);
        return new ItemAction(ItemActionKind.ABANDONED, item.objectId(), null);
    }

    synchronized ItemPickup acceptGroundItem(
            int mapId, long objectId, ClassicWorldGroundItems.Item item,
            long nowNanos) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || !actor.alive || actor.targetObjectId != 0
                || actor.targetItemObjectId == 0 || item == null
                || item.mapId() != mapId
                || item.objectId() != actor.targetItemObjectId
                || Math.max(Math.abs(actor.x - item.x()),
                        Math.abs(actor.y - item.y())) != 1
                || !canCarry(actor, item)) {
            return null;
        }
        RuntimeInventory staged = actor.releasedItems.copy();
        Map<Long, Integer> stagedDigest =
                new LinkedHashMap<>(actor.digestSeconds);
        RuntimeInventory.Item destination = staged.addGroundTransferredResult(
                item.inventoryItem(), item.quantity());
        if (destination == null) {
            throw new IllegalStateException(
                    "validated monster ground transfer changed");
        }
        // Retained L1NpcInstance.onGetItem order: refine, shuffle, digest input.
        List<Refinement> refinements = refineGivenItems(
                actor.definition.template(), staged, stagedDigest);
        staged.shuffle(actor.random);
        int digest = ClassicWorldMonsterItemLifecycleCatalog.digestSeconds(
                actor.definition.template());
        if (digest > 0) stagedDigest.put(destination.objectId(), digest);

        actor.releasedItems = staged;
        actor.digestSeconds.clear();
        actor.digestSeconds.putAll(stagedDigest);
        if (actor.digestSeconds.isEmpty()) digestActors.remove(objectId);
        else digestActors.add(objectId);
        actor.targetItemObjectIds.remove(Long.valueOf(item.objectId()));
        actor.targetItemObjectId = 0;
        actor.nextActionNanos = nowNanos + TimeUnit.SECONDS.toNanos(1);
        return new ItemPickup(actor.snapshot(), item.objectId(),
                destination.objectId(), item.definition().catalogId(),
                item.quantity(), refinements, digest);
    }

    synchronized void groundItemUnavailable(
            int mapId, long objectId, long itemObjectId, long nowNanos) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || actor.targetItemObjectId != itemObjectId) return;
        abandonCurrentItemTarget(actor, itemObjectId);
        actor.nextActionNanos = nowNanos + TimeUnit.SECONDS.toNanos(1);
    }

    synchronized List<RuntimeInventory.Item> carriedItems(
            int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId) return List.of();
        return actor.releasedItems.items();
    }

    synchronized GiveAdmission canReceiveGivenItem(
            int mapId, long objectId, int targetX, int targetY,
            RuntimeInventory.Item item, long quantity) {
        Actor actor = actors.get(objectId);
        if (!receivableNpcActor(actor, mapId) || item == null || quantity <= 0
                || quantity > item.quantity() || !item.transferable()) {
            return GiveAdmission.UNKNOWN_TARGET;
        }
        if (actor.x != targetX || actor.y != targetY) {
            return GiveAdmission.STALE_POSITION;
        }
        return canCarry(actor.releasedItems, item, quantity)
                ? GiveAdmission.ACCEPTED : GiveAdmission.CAPACITY;
    }

    synchronized MonsterItemReceipt receiveGivenItemPersisted(
            int mapId, long objectId, int targetX, int targetY,
            RuntimeInventory.Item item, long quantity, DurableCommit commit)
            throws IOException {
        if (commit == null || canReceiveGivenItem(
                mapId, objectId, targetX, targetY, item, quantity)
                != GiveAdmission.ACCEPTED) {
            return null;
        }
        Actor actor = actors.get(objectId);
        RuntimeInventory staged = actor.releasedItems.copy();
        Map<Long, Integer> stagedDigest = new LinkedHashMap<>(actor.digestSeconds);
        RuntimeInventory.Item destination = staged.addTransferredResult(item, quantity);
        if (destination == null) {
            throw new IllegalStateException("validated monster C_GIVE transfer changed");
        }
        List<Refinement> refinements = refineGivenItems(
                actor.definition.template(), staged, stagedDigest);
        staged.shuffle(actor.random);
        int digest = ClassicWorldMonsterItemLifecycleCatalog.digestSeconds(
                actor.definition.template());
        if (digest > 0) stagedDigest.put(destination.objectId(), digest);
        commit.commit();
        actor.releasedItems = staged;
        actor.digestSeconds.clear();
        actor.digestSeconds.putAll(stagedDigest);
        if (actor.digestSeconds.isEmpty()) digestActors.remove(objectId);
        else digestActors.add(objectId);
        return new MonsterItemReceipt(actor.snapshot(), destination.objectId(),
                item.definition().catalogId(), Math.toIntExact(quantity),
                refinements, digest, Math.max(
                        actor.definition.template().lightSize(),
                staged.activeLightRange()));
    }

    synchronized int lightRange(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        if (actor == null || actor.definition.mapId() != mapId
                || !actor.present || actor.currentHp <= 0) return 0;
        return Math.max(actor.definition.template().lightSize(),
                actor.releasedItems.activeLightRange());
    }

    /**
     * Retained C_GIVE -> onGetItem -> tamePet transition.  The owner-side
     * durable commit runs before the staged NPC bag is published; capture and
     * bag transfer then happen under the same population lock.
     */
    synchronized TamedItemReceipt receiveGivenItemAndCapturePersisted(
            int mapId, long objectId, int targetX, int targetY,
            RuntimeInventory.Item item, long quantity, MonsterTameCommit commit)
            throws IOException {
        if (commit == null || canReceiveGivenItem(
                mapId, objectId, targetX, targetY, item, quantity)
                != GiveAdmission.ACCEPTED) {
            return null;
        }
        Actor actor = actors.get(objectId);
        RuntimeInventory staged = actor.releasedItems.copy();
        Map<Long, Integer> stagedDigest = new LinkedHashMap<>(actor.digestSeconds);
        RuntimeInventory.Item destination = staged.addTransferredResult(item, quantity);
        if (destination == null) {
            throw new IllegalStateException("validated monster C_GIVE transfer changed");
        }
        List<Refinement> refinements = refineGivenItems(
                actor.definition.template(), staged, stagedDigest);
        staged.shuffle(actor.random);
        int digest = ClassicWorldMonsterItemLifecycleCatalog.digestSeconds(
                actor.definition.template());
        if (digest > 0) stagedDigest.put(destination.objectId(), digest);

        Snapshot captured = actor.snapshot();
        List<RuntimeInventory.Item> carried = staged.items();
        commit.commit(captured, carried);
        actor.releasedItems = staged;
        actor.digestSeconds.clear();
        actor.digestSeconds.putAll(stagedDigest);
        if (actor.digestSeconds.isEmpty()) digestActors.remove(objectId);
        else digestActors.add(objectId);
        MonsterItemReceipt itemReceipt = new MonsterItemReceipt(
                captured, destination.objectId(), item.definition().catalogId(),
                Math.toIntExact(quantity), refinements, digest, Math.max(
                        actor.definition.template().lightSize(),
                        staged.activeLightRange()));
        Capture capture = captureTamed(mapId, objectId);
        if (capture == null) {
            throw new IllegalStateException("durably committed tame target disappeared");
        }
        actor.releasedItems.clear();
        actor.digestSeconds.clear();
        digestActors.remove(objectId);
        return new TamedItemReceipt(itemReceipt, capture, carried);
    }

    synchronized List<DigestRemoval> advanceDigestItems(int elapsedSeconds) {
        if (elapsedSeconds <= 0) {
            throw new IllegalArgumentException("invalid monster digest interval");
        }
        List<DigestRemoval> removals = new ArrayList<>();
        for (long objectId : List.copyOf(digestActors)) {
            Actor actor = actors.get(objectId);
            if (actor == null || !actor.present) {
                digestActors.remove(objectId);
                continue;
            }
            if (!actor.alive) continue;
            if (actor.digestSeconds.isEmpty()
                    || ClassicWorldMonsterItemLifecycleCatalog.digestSeconds(
                            actor.definition.template()) <= 0) {
                digestActors.remove(objectId);
                continue;
            }
            for (Map.Entry<Long, Integer> entry
                    : List.copyOf(actor.digestSeconds.entrySet())) {
                RuntimeInventory.Item tracked = actor.releasedItems.item(entry.getKey());
                if (tracked == null) {
                    actor.digestSeconds.remove(entry.getKey());
                    continue;
                }
                int remaining = entry.getValue() - elapsedSeconds;
                if (remaining > 0) {
                    actor.digestSeconds.put(entry.getKey(), remaining);
                    continue;
                }
                RuntimeInventory.Item removed = actor.releasedItems.removeEntire(entry.getKey());
                actor.digestSeconds.remove(entry.getKey());
                if (removed != null) {
                    removals.add(new DigestRemoval(
                            actor.snapshot(), removed, elapsedSeconds));
                }
            }
            if (actor.digestSeconds.isEmpty()) digestActors.remove(objectId);
        }
        return List.copyOf(removals);
    }

    synchronized int digestRemainingSeconds(int mapId, long objectId, long itemObjectId) {
        Actor actor = actors.get(objectId);
        return actor == null || actor.definition.mapId() != mapId
                ? 0 : actor.digestSeconds.getOrDefault(itemObjectId, 0);
    }

    private static boolean receivableNpcActor(Actor actor, int mapId) {
        return actor != null && actor.definition.mapId() == mapId
                && actor.present && actor.alive
                && switch (actor.definition.template().implementation()) {
                    case "L1Monster", "L1Guardian", "L1Guard" -> true;
                    default -> false;
                };
    }

    private static boolean canCarry(
            RuntimeInventory inventory, RuntimeInventory.Item source, long quantity) {
        boolean stackTarget = source.definition().stackable()
                && inventory.items().stream().anyMatch(existing ->
                        existing.definition().catalogId()
                                == source.definition().catalogId()
                        && existing.definition().equals(source.definition())
                        && existing.tournamentReward() == source.tournamentReward());
        if (inventory.size() > ClassicWorldMonsterPickupCatalog.MAX_NPC_ITEMS
                || inventory.size() == ClassicWorldMonsterPickupCatalog.MAX_NPC_ITEMS
                    && !stackTarget) {
            return false;
        }
        int addedWeight = BattleInventoryWeight.itemWeight(
                quantity, source.definition().unitWeight());
        if (BattleInventoryWeight.addWeight(inventory.totalWeight(), addedWeight)
                > ClassicWorldMonsterPickupCatalog.MAX_NPC_WEIGHT) {
            return false;
        }
        return inventory.copy().addTransferred(source, quantity);
    }

    private static List<Refinement> refineGivenItems(
            ClassicWorldMonsterCatalog.MonsterTemplate template,
            RuntimeInventory inventory,
            Map<Long, Integer> digestSeconds) {
        List<Refinement> results = new ArrayList<>();
        int npcId = template.npcId();
        if (npcId == 45032 && template.experience() != 0) {
            refine(inventory, digestSeconds, results,
                    new long[] {40508, 40521, 40045}, new int[] {150, 3, 3}, 20, 1,
                    false);
            refine(inventory, digestSeconds, results,
                    new long[] {40494, 40521}, new int[] {150, 3}, 19, 1, false);
            refine(inventory, digestSeconds, results,
                    new long[] {40494, 40521}, new int[] {50, 1}, 3, 1, false);
            refine(inventory, digestSeconds, results,
                    new long[] {88, 40508, 40045}, new int[] {4, 80, 3}, 100, 1,
                    false);
            refine(inventory, digestSeconds, results,
                    new long[] {88, 40494}, new int[] {2, 80}, 89, 1, true);
        } else if ((npcId == 45166 || npcId == 45167)
                && template.experience() != 0
                && inventory.countCatalog(40726) == 0) {
            refine(inventory, digestSeconds, results,
                    new long[] {40725}, new int[] {1}, 40726, 1, false);
        }
        return List.copyOf(results);
    }

    private static void refine(
            RuntimeInventory inventory, Map<Long, Integer> digestSeconds,
            List<Refinement> results, long[] ingredients, int[] required,
            long outputCatalogId, int outputPerBatch, boolean digestOutput) {
        if (ingredients.length != required.length || ingredients.length == 0) {
            throw new IllegalArgumentException("invalid retained refinement recipe");
        }
        long batches = Long.MAX_VALUE;
        for (int index = 0; index < ingredients.length; index++) {
            batches = Math.min(batches,
                    inventory.countCatalog(ingredients[index]) / required[index]);
        }
        if (batches <= 0) return;
        long outputQuantity = Math.multiplyExact(batches, outputPerBatch);
        InventoryCatalog.Definition output = InventoryCatalog.find(outputCatalogId);
        if (output == null) {
            throw new IllegalStateException(
                    "missing retained refinement item " + outputCatalogId);
        }
        for (int index = 0; index < ingredients.length; index++) {
            if (!inventory.removeCatalog(
                    ingredients[index], Math.multiplyExact(batches, required[index]))) {
                throw new IllegalStateException("validated refinement debit changed");
            }
        }
        Set<Long> before = new LinkedHashSet<>();
        for (RuntimeInventory.Item item : inventory.items()) before.add(item.objectId());
        if (!inventory.add(output, outputQuantity)) {
            throw new IllegalStateException("retained refinement output exceeds NPC inventory");
        }
        if (digestOutput) {
            int seconds = ClassicWorldMonsterItemLifecycleCatalog.digestSecondsForNpc(45032);
            if (seconds > 0) {
                for (RuntimeInventory.Item produced : inventory.items()) {
                    if (produced.definition().catalogId() == outputCatalogId
                            && (!before.contains(produced.objectId())
                                || produced.definition().stackable())) {
                        digestSeconds.put(produced.objectId(), seconds);
                    }
                }
            }
        }
        results.add(new Refinement(outputCatalogId, Math.toIntExact(outputQuantity)));
    }

    private static ClassicWorldGroundItems.Item checkTargetItem(
            Actor actor, List<ClassicWorldGroundItems.Item> visible) {
        ClassicWorldGroundItems.Item target = findGroundItem(
                visible, actor.targetItemObjectId);
        while (target == null || target.mapId() != actor.definition.mapId()
                || Math.max(Math.abs(actor.x - target.x()),
                        Math.abs(actor.y - target.y())) > 15) {
            if (actor.targetItemObjectIds.isEmpty()) {
                actor.targetItemObjectId = 0;
                return null;
            }
            actor.targetItemObjectId = actor.targetItemObjectIds.remove(0);
            target = findGroundItem(visible, actor.targetItemObjectId);
        }
        return target;
    }

    private static ClassicWorldGroundItems.Item searchTargetItem(
            Actor actor, List<ClassicWorldGroundItems.Item> visible,
            int groundInventoryFixture) {
        Map<GridPoint, List<ClassicWorldGroundItems.Item>> inventories =
                new LinkedHashMap<>();
        for (ClassicWorldGroundItems.Item item : visible) {
            if (item != null && item.mapId() == actor.definition.mapId()) {
                inventories.computeIfAbsent(
                        new GridPoint(item.x(), item.y()),
                        ignored -> new ArrayList<>()).add(item);
            }
        }
        if (inventories.isEmpty()) return null;
        List<List<ClassicWorldGroundItems.Item>> candidates =
                new ArrayList<>(inventories.values());
        if (groundInventoryFixture >= candidates.size()) return null;
        int selected = groundInventoryFixture >= 0
                ? groundInventoryFixture : actor.random.nextInt(candidates.size());
        ClassicWorldGroundItems.Item target = null;
        for (ClassicWorldGroundItems.Item item : candidates.get(selected)) {
            if (!canCarry(actor, item)) continue;
            actor.targetItemObjectId = item.objectId();
            actor.targetItemObjectIds.add(item.objectId());
            target = item;
        }
        return target;
    }

    private static ClassicWorldGroundItems.Item findGroundItem(
            List<ClassicWorldGroundItems.Item> items, long objectId) {
        if (objectId == 0) return null;
        for (ClassicWorldGroundItems.Item item : items) {
            if (item != null && item.objectId() == objectId) return item;
        }
        return null;
    }

    private static boolean canCarry(
            Actor actor, ClassicWorldGroundItems.Item item) {
        RuntimeInventory.Item source = item.inventoryItem();
        boolean stackTarget = source.definition().stackable()
                && actor.releasedItems.items().stream().anyMatch(existing ->
                        existing.definition().catalogId()
                                == source.definition().catalogId());
        if (actor.releasedItems.size()
                    > ClassicWorldMonsterPickupCatalog.MAX_NPC_ITEMS
                || actor.releasedItems.size()
                    == ClassicWorldMonsterPickupCatalog.MAX_NPC_ITEMS
                    && !stackTarget) {
            return false;
        }
        int addedWeight = BattleInventoryWeight.itemWeight(
                item.quantity(), item.definition().unitWeight());
        if (BattleInventoryWeight.addWeight(
                actor.releasedItems.totalWeight(), addedWeight)
                > ClassicWorldMonsterPickupCatalog.MAX_NPC_WEIGHT) {
            return false;
        }
        RuntimeInventory candidate = actor.releasedItems.copy();
        return candidate.addGroundTransferred(source, item.quantity());
    }

    private static void abandonCurrentItemTarget(
            Actor actor, long itemObjectId) {
        actor.targetItemObjectIds.remove(Long.valueOf(itemObjectId));
        actor.targetItemObjectId = 0;
    }

    private static void clearItemTargets(Actor actor) {
        actor.targetItemObjectIds.clear();
        actor.targetItemObjectId = 0;
    }

    synchronized IdleMovement idleMove(int n, long l, Set<GridPoint> set, long l2) {
        return this.idleMove(n, l, set, l2, 0, -1);
    }

    /**
     * Exact ownerless {@code L1NpcInstance.noTarget} idle state machine.
     * A new cycle retains a source run of 1..3 plus its selection tick (2..4
     * ticks total); rolls 8..39 intentionally consume the 300 ms default AI
     * delay without moving.  Fixtures are package-private test controls:
     * zero run length and negative direction select the actor's native RNG.
     */
    synchronized IdleMovement idleMove(int n, long l, Set<GridPoint> set, long l2, int n2, int n3) {
        int n4;
        if (set == null || n2 < 0 || n2 > 3 || n3 < -1 || n3 > 39) {
            throw new IllegalArgumentException("invalid idle movement input");
        }
        Actor actor2 = this.actors.get(l);
        if (actor2 == null || actor2.definition.mapId() != n || !actor2.present || !actor2.alive || actor2.targetObjectId != 0L || actor2.definition.template().guard() || actor2.runtimePassispeed <= 0 || actor2.definition.rest() || l2 < actor2.nextActionNanos || ClassicWorldMonsterPopulation.actionLocked(actor2, l2)) {
            return null;
        }
        Actor actor = actor2.mobGroupMinion ? this.actors.get(actor2.mobGroupLeaderObjectId) : null;
        if (actor != null && actor != actor2 && actor.present && actor.alive && actor.definition.mapId() == n) {
            int n5 = Math.max(Math.abs(actor2.x - actor.x), Math.abs(actor2.y - actor.y));
            if (n5 <= 2 || actor2.damagePoisonStatus.blindActive(l2)) {
                ClassicWorldMonsterPopulation.deferIdle(actor2, l2);
                return null;
            }
            n4 = ClassicWorldMonsterPopulation.heading(actor2.x, actor2.y, actor.x, actor.y);
        } else {
            if (actor2.randomMoveDistance == 0) {
                actor2.randomMoveDistance = n2 > 0 ? n2 : actor2.random.nextInt(3) + 1;
                actor2.randomMoveDirection = n3 >= 0 ? n3 : actor2.random.nextInt(40);
            } else {
                --actor2.randomMoveDistance;
            }
            if (actor2.randomMoveDirection >= 8) {
                ClassicWorldMonsterPopulation.deferIdle(actor2, l2);
                return null;
            }
            if (actor2.x == actor2.homeX && actor2.y == actor2.homeY) {
                n4 = actor2.heading;
            } else {
                double d = Math.hypot(actor2.x - actor2.homeX, actor2.y - actor2.homeY);
                if (actor2.damagePoisonStatus.blindActive(l2) && d >= 2.0) {
                    ClassicWorldMonsterPopulation.deferIdle(actor2, l2);
                    return null;
                }
                n4 = ClassicWorldMonsterPopulation.heading(actor2.x, actor2.y, actor2.homeX, actor2.homeY);
            }
        }
        if (n4 < 0) {
            ClassicWorldMonsterPopulation.deferIdle(actor2, l2);
            return null;
        }
        int[] nArray = new int[]{n4, n4 + 7 & 7, n4 + 1 & 7, n4 + 2 & 7, n4 + 6 & 7, n4 + 5 & 7, n4 + 3 & 7, n4 + 4 & 7};
        ClassicWorldMapCatalog.ClassicMap classicMap = ClassicWorldMapCatalog.forMap(n);
        for (int n6 : nArray) {
            int n7 = actor2.x + HEADING_X[n6];
            int n8 = actor2.y + HEADING_Y[n6];
            Cell cell = new Cell(n, n7, n8);
            if (!(!classicMap.canStep(actor2.x, actor2.y, n7, n8) || this.isOccupied(cell) || set.contains(new GridPoint(n7, n8)) || NativeStaticNpcMotion.occupies(n, n7, n8, this))) {
            Snapshot snapshot = actor2.snapshot();
            this.vacate(new Cell(n, actor2.x, actor2.y));
            actor2.x = n7;
            actor2.y = n8;
            actor2.heading = n6;
            this.occupy(cell);
            Snapshot snapshot2 = actor2.snapshot();
            this.applyMovementDistance(actor2);
            this.updateWandered(actor2);
            actor2.nextActionNanos = l2 + TimeUnit.MILLISECONDS.toNanos(BattleActionInterval.npcSourceMillis(Math.max(100, actor2.runtimePassispeed), this.hasteActive(n, l, l2), this.slowActive(n, l, l2)));
            return new IdleMovement(snapshot, snapshot2, actor2.snapshot());

            }
}
        ClassicWorldMonsterPopulation.deferIdle(actor2, l2);
        return null;
    }

    /**
     * Native noTarget ends AI and teleports a wandered ownerless actor home
     * when no PC remains within DISTANCE=20. Exact-home occupancy is reference
     * counted because retained teleport does not perform a landing search.
     */
    synchronized List<HomeReset> resetUnobservedHome(
            int mapId, Set<GridPoint> playerCells, long nowNanos) {
        if (playerCells == null) {
            throw new IllegalArgumentException("null idle observer cells");
        }
        List<HomeReset> result = new ArrayList<>();
        Set<Long> candidates = new LinkedHashSet<>(wanderedActors);
        candidates.addAll(activatedActors);
        for (long objectId : candidates) {
            Actor actor = actors.get(objectId);
            if (actor == null) {
                wanderedActors.remove(objectId);
                activatedActors.remove(objectId);
                continue;
            }
            if (actor.definition.mapId() != mapId) continue;
            if (!actor.present || !actor.alive
                    || actor.targetObjectId != 0
                    || actor.definition.template().guard()
                    || actor.hiddenStatus != HIDDEN_STATUS_NONE) {
                continue;
            }
            boolean observed = playerCells.stream().anyMatch(point ->
                    Math.max(Math.abs(point.x() - actor.x),
                            Math.abs(point.y() - actor.y)) <= 20);
            if (observed) continue;
            // Retained noTarget ends this NpcAI lifetime. A later onPerceive
            // starts a fresh lifetime and resets L1Monster._actived.
            actor.actived = false;
            activatedActors.remove(objectId);
            if (actor.x == actor.homeX && actor.y == actor.homeY) {
                wanderedActors.remove(objectId);
                continue;
            }
            Snapshot before = actor.snapshot();
            vacate(new Cell(mapId, actor.x, actor.y));
            actor.x = actor.homeX;
            actor.y = actor.homeY;
            occupy(new Cell(mapId, actor.x, actor.y));
            wanderedActors.remove(objectId);
            actor.nextActionNanos = nowNanos + 300_000_000L;
            result.add(new HomeReset(before, actor.snapshot()));
        }
        return List.copyOf(result);
    }

    private static void deferIdle(Actor actor, long nowNanos) {
        actor.nextActionNanos = nowNanos + 300_000_000L;
    }

    private void applyMovementDistance(Actor actor) {
        if (actor.definition.movementDistance() <= 0
                || Math.hypot(actor.x - actor.homeX,
                    actor.y - actor.homeY)
                    <= actor.definition.movementDistance()) {
            return;
        }
        int mapId = actor.definition.mapId();
        vacate(new Cell(mapId, actor.x, actor.y));
        actor.x = actor.homeX;
        actor.y = actor.homeY;
        occupy(new Cell(mapId, actor.x, actor.y));
    }

    private void updateWandered(Actor actor) {
        if (actor.x == actor.homeX && actor.y == actor.homeY) {
            wanderedActors.remove(actor.objectId);
        } else {
            wanderedActors.add(actor.objectId);
        }
    }

    /**
     * Retained {@code L1GuardInstance.noTarget}: walk one tile toward home;
     * when path selection fails, teleport to the nearest safe home landing
     * within the retained direct-teleport radius and face south-east (1).
     */
    synchronized GuardHomecoming returnGuardHome(int n, long l, Set<GridPoint> set, long l2) {
        Actor actor = this.actors.get(l);
        if (actor == null || actor.definition.mapId() != n || !actor.present || !actor.alive || !actor.definition.template().guard() || actor.targetObjectId != 0L || l2 < actor.nextActionNanos || ClassicWorldMonsterPopulation.actionLocked(actor, l2) || actor.damagePoisonStatus.blindActive(l2) || actor.runtimePassispeed <= 0) {
            return null;
        }
        int n2 = actor.homeX;
        int n3 = actor.homeY;
        if (actor.x == n2 && actor.y == n3) {
            return null;
        }
        int n4 = ClassicWorldMonsterPopulation.heading(actor.x, actor.y, n2, n3);
        if (n4 >= 0) {
            int[] headings = new int[]{n4, n4 + 7 & 7, n4 + 1 & 7, n4 + 6 & 7, n4 + 2 & 7};
            ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(n);
            for (int object3 : headings) {
                int n5 = actor.x + HEADING_X[object3];
                int n6 = actor.y + HEADING_Y[object3];
                Cell cell = new Cell(n, n5, n6);
                if (!(!map.canStep(actor.x, actor.y, n5, n6) || this.isOccupied(cell) || set.contains(new GridPoint(n5, n6)) || NativeStaticNpcMotion.occupies(n, n5, n6, this))) {
                Snapshot snapshot = actor.snapshot();
                this.vacate(new Cell(n, actor.x, actor.y));
                actor.x = n5;
                actor.y = n6;
                actor.heading = (int)object3;
                this.occupy(cell);
                actor.nextActionNanos = l2 + TimeUnit.MILLISECONDS.toNanos(BattleActionInterval.npcSourceMillis(Math.max(100, actor.runtimePassispeed), this.hasteActive(n, l, l2), this.slowActive(n, l, l2)));
                return new GuardHomecoming(snapshot, actor.snapshot(), false);

                }
}
        }
        GridPoint landing = this.nearestGuardHomeLanding(n, n2, n3, set);
        if (landing == null) {
            actor.nextActionNanos = l2 + 250000000L;
            return null;
        }
        Snapshot before = actor.snapshot();
        this.vacate(new Cell(n, actor.x, actor.y));
        actor.x = landing.x();
        actor.y = landing.y();
        actor.heading = 1;
        this.occupy(new Cell(n, actor.x, actor.y));
        actor.nextActionNanos = l2 + TimeUnit.MILLISECONDS.toNanos(BattleActionInterval.npcSourceMillis(Math.max(100, actor.runtimePassispeed), this.hasteActive(n, l, l2), this.slowActive(n, l, l2)));
        return new GuardHomecoming(before, actor.snapshot(), true);
    }

    private GridPoint nearestGuardHomeLanding(
            int mapId, int homeX, int homeY, Set<GridPoint> occupiedPlayers) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(mapId);
        // CheckUtil.DIRECT_TELEPORT_LANDING_SEARCH_RADIUS in the retained
        // server is 12. Rings keep the landing deterministic and nearest.
        for (int radius = 0; radius <= 12; radius++) {
            for (int y = homeY - radius; y <= homeY + radius; y++) {
                for (int x = homeX - radius; x <= homeX + radius; x++) {
                    if (Math.max(Math.abs(x - homeX),
                            Math.abs(y - homeY)) != radius) continue;
                    GridPoint point = new GridPoint(x, y);
                    if (!map.standingPassable(x, y)
                            || isOccupied(new Cell(mapId, x, y))
                            || occupiedPlayers.contains(point)
                            || NativeStaticNpcMotion.occupies(mapId, x, y, this)) {
                        continue;
                    }
                    return point;
                }
            }
        }
        return null;
    }

    private static boolean actionLocked(Actor actor, long l) {
        return actor.hiddenStatus != 0 || actor.damagePoisonStatus.paralysisActive(l) || actor.damagePoisonStatus.sleepActive(l) || CombatWorldFreeze.rawFrozen(actor.damagePoisonStatus.freezeActive(l), actor.damagePoisonStatus) || actor.damagePoisonStatus.earthBindActive(l) || actor.damagePoisonStatus.shockStunActive(l) || actor.damagePoisonStatus.boneBreakActive(l) || actor.damagePoisonStatus.thunderGrabActive(l);
    }

    private static boolean initialSinkStatus(int npcId, int spawnNumber) {
        return switch (npcId) {
            case 45061, 45161, 45181, 45455,
                    45045, 45126, 45134, 45281,
                    46107, 46108 -> Math.floorMod(spawnNumber, 3) == 0;
            default -> false;
        };
    }

    private static int sinkActorStatus(int npcId) {
        return switch (npcId) {
            case 45045, 45126, 45134, 45281 -> 4;
            case 45061, 45161, 45181, 45455, 46107, 46108 -> 13;
            default -> 0;
        };
    }

    private static boolean initialFlyStatus(int npcId) {
        return switch (npcId) {
            case 45067, 45264, 45452, 45090, 45321, 45445, 45681 -> true;
            default -> false;
        };
    }

    private static boolean initialIceStatus(int npcId) {
        return switch (npcId) {
            case 46125, 46126, 46127, 46128 -> true;
            default -> false;
        };
    }

    private boolean place(Actor actor) {
        ClassicWorldMonsterCatalog.SpawnDefinition definition = actor.definition;
        for (int attempt = 0; attempt < SOURCE_RANDOM_ATTEMPTS; attempt++) {
            int[] candidate = sourceCandidate(definition, actor.random);
            if (commitIfAvailable(actor, candidate[0], candidate[1])) return true;
        }
        if (!definition.areaSpawn()) {
            // Retained L1Spawn does not reserve cells between independent
            // spawn rows.  A broad area row may therefore land on a later
            // exact fixed row; the fixed actor must still exist at its source
            // coordinate instead of making the whole quest instance fail.
            if (!definition.randomSpawn()) {
                return commitSharedIfWalkable(
                        actor, definition.locX(), definition.locY());
            }
            return false;
        }
        int minX = definition.areaMinX();
        int maxX = definition.areaMaxX();
        int minY = definition.areaMinY();
        int maxY = definition.areaMaxY();
        int spanX = Math.addExact(Math.subtractExact(maxX, minX), 1);
        int spanY = Math.addExact(Math.subtractExact(maxY, minY), 1);
        long cells = (long) spanX * spanY;
        if (cells > AREA_FULL_SCAN_MAX_CELLS) {
            for (int probe = 0; probe < AREA_LARGE_RANDOM_PROBES; probe++) {
                int x = minX + actor.random.nextInt(spanX);
                int y = minY + actor.random.nextInt(spanY);
                if (commitIfAvailable(actor, x, y)) return true;
            }
            return false;
        }
        int offsetX = actor.random.nextInt(spanX);
        int offsetY = actor.random.nextInt(spanY);
        for (int xi = 0; xi < spanX; xi++) {
            int x = minX + (offsetX + xi) % spanX;
            for (int yi = 0; yi < spanY; yi++) {
                int y = minY + (offsetY + yi) % spanY;
                if (commitIfAvailable(actor, x, y)) return true;
            }
        }
        // Retained L1Spawn does not enforce one-monster-per-cell.  A few exact
        // source groups contain more actors than distinct cells (for example
        // map309 spawn2121 count5 in a 2x2 area).  Preserve that source shape
        // only after every unique standing cell has been attempted.
        for (int xi = 0; xi < spanX; xi++) {
            int x = minX + (offsetX + xi) % spanX;
            for (int yi = 0; yi < spanY; yi++) {
                int y = minY + (offsetY + yi) % spanY;
                if (commitSharedIfWalkable(actor, x, y)) return true;
            }
        }
        return false;
    }

    private int[] sourceCandidate(
            ClassicWorldMonsterCatalog.SpawnDefinition definition, Random random) {
        if (definition.areaSpawn()) {
            int x = definition.areaMinX()
                    + random.nextInt(definition.areaMaxX() - definition.areaMinX() + 1);
            int y = definition.areaMinY()
                    + random.nextInt(definition.areaMaxY() - definition.areaMinY() + 1);
            return new int[] {x, y};
        }
        if (definition.randomSpawn()) {
            int x = definition.locX()
                    + randomOffset(random, definition.randomX());
            int y = definition.locY()
                    + randomOffset(random, definition.randomY());
            return new int[] {x, y};
        }
        return new int[] {definition.locX(), definition.locY()};
    }

    private static int randomOffset(Random random, int range) {
        if (range == 0) return 0;
        return (int) (random.nextDouble() * range)
                - (int) (random.nextDouble() * range);
    }

    private boolean commitIfAvailable(Actor actor, int n, int n2) {
        int n3 = actor.definition.mapId();
        Cell cell = new Cell(n3, n, n2);
        ClassicWorldMapCatalog.ClassicMap classicMap = ClassicWorldMapCatalog.forMap(n3);
        if (!classicMap.standingPassable(n, n2) || this.isOccupied(cell) || NativeStaticNpcMotion.occupies(n3, n, n2, this)) {
            return false;
        }
        actor.x = n;
        actor.y = n2;
        actor.homeX = n;
        actor.homeY = n2;
        this.wanderedActors.remove(actor.objectId);
        this.occupy(cell);
        return true;
    }

    private boolean commitSharedIfWalkable(Actor actor, int n, int n2) {
        int n3 = actor.definition.mapId();
        ClassicWorldMapCatalog.ClassicMap classicMap = ClassicWorldMapCatalog.forMap(n3);
        if (!classicMap.standingPassable(n, n2) || NativeStaticNpcMotion.occupies(n3, n, n2, this)) {
            return false;
        }
        actor.x = n;
        actor.y = n2;
        actor.homeX = n;
        actor.homeY = n2;
        this.wanderedActors.remove(actor.objectId);
        this.occupy(new Cell(n3, n, n2));
        return true;
    }

    private boolean isOccupied(Cell cell) {
        return occupied.getOrDefault(cell, 0) > 0;
    }

    private void occupy(Cell cell) {
        occupied.merge(cell, 1, Math::addExact);
    }

    private void vacate(Cell cell) {
        Integer count = occupied.get(cell);
        if (count == null) return;
        if (count <= 1) occupied.remove(cell);
        else occupied.put(cell, count - 1);
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static final int[] HEADING_X = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] HEADING_Y = {-1, -1, 0, 1, 1, 1, 0, -1};

    private static int heading(int fromX, int fromY, int toX, int toY) {
        int dx = Integer.compare(toX, fromX);
        int dy = Integer.compare(toY, fromY);
        for (int heading = 0; heading < HEADING_X.length; heading++) {
            if (dx == HEADING_X[heading] && dy == HEADING_Y[heading]) return heading;
        }
        return -1;
    }
    synchronized Snapshot nativeStaticCancellationSpawn(int n, int n2, int n3, int n4, int n5, ClassicWorldMonsterCatalog.MonsterTemplate monsterTemplate) {
        if (monsterTemplate == null || this.nextQuestDynamicObjectId <= 0L || this.nextQuestDynamicObjectId > this.questDynamicObjectEnd || this.actors.containsKey(this.nextQuestDynamicObjectId)) {
            return null;
        }
        long l = this.nextQuestDynamicObjectId;
        ClassicWorldMonsterCatalog.SpawnDefinition spawnDefinition = new ClassicWorldMonsterCatalog.SpawnDefinition(n, n2, "cancellation-runtime-spawn", 1, 0, n3, n4, 0, 0, 0, 0, 0, 0, n5, 0, 0, false, 30, false, 0, monsterTemplate);
        Actor actor = new Actor(spawnDefinition, 1, l, new Random(ClassicWorldMonsterPopulation.mix64(l)), true, List.of());
        actor.x = n3;
        actor.y = n4;
        actor.homeX = n3;
        actor.homeY = n4;
        actor.heading = n5;
        Snapshot snapshot = actor.snapshot();
        this.occupy(new Cell(n, actor.x, actor.y));
        this.actors.put(l, actor);
        ++this.nextQuestDynamicObjectId;
        return snapshot;
    }
}
