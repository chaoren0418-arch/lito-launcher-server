// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

import java.util.Iterator;

import java.util.HashMap;

import java.util.Map;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Authoritative, force-isolated Quest Battle encounter.
 *
 * <p>The four-room species order is explicitly listed in the 2005-08-29
 * 4Gamer Tokyo Battle GP report: Turtle Dragon; Iceman/Muryan;
 * Scorpion/Ogre/Living Armor; Minotaur/Medusa/Death Knight.  The concrete npc
 * id, gfx, display string, level and HP are taken from the same-family 3.80 npc
 * table.  Its paired {@code spr_action} table independently confirms timed
 * walk and attack actions for every selected gfx.  Those sources establish
 * usable same-family actors; they do not recover the original event server's
 * spawn count, coordinates or tuned event-server stats.</p>
 *
 * <p>The contemporaneous 4Gamer Fukuoka room-one photograph visibly contains
 * at least two spatially separated Turtle Dragon actors whose shape matches
 * selected-client gfx 1649.  The sealed GAME Watch {@code lin22/lin23/lin24}
 * photographs similarly contradict the former one-per-species placeholder:
 * fixed upper-lane regions contain seven, nine and seven continuous red
 * actor-label/bar candidates in rooms two through four.  All four values are
 * visible lower bounds, not complete or per-species counts.  The roster below
 * therefore uses two Turtle Dragons in room one and reaches the later totals
 * with a balanced provisional species distribution.  Actors are spread over a
 * deterministic connected native-floor envelope around an S32-probed safe
 * anchor.  Every snapshot exposes these boundaries rather than presenting
 * them as exact original-server spawn data.</p>
 */
final class BattleQuestEncounter {
    static final String SPECIES_SOURCE =
            "4Gamer-Tokyo-2005-08-29-rooms+same-family-380-npc+same-family-380-spr-action";
    static final String ROOM_ONE_PHOTO_SOURCE_URL =
            "https://www.4gamer.net/games/001/G000189/20050819182443/";
    static final int ROOM_ONE_PHOTO_LOWER_BOUND = 2;
    static final String COUNT_SOURCE =
            "4Gamer-Fukuoka-2005-room1-two-visible-native-gfx1649-lower-bound"
                    + "+GAME-Watch-lin22-24-visible-total-lower-bound"
                    + "+balanced-later-species-provisional";
    static final String PLACEMENT_SOURCE =
            "LTmap-Quest-family-S32-connected-local-envelope-provisional";
    static final String DATABASE_SPAWN_SOURCE =
            "lito_static.lito_battle_spawn";
    static final String AI_SOURCE =
            "LTmap-Layer3-native-sub_48B640-pursuit+native-sprite-action-duration";
    static final String AI_DAMAGE_SOURCE =
            BattleTournamentNpcPhysicalDamage.SOURCE;
    static final String MOBSKILL_SOURCE =
            BattleTimeAttackMagicSkillCatalog.SOURCE;
    static final String DEATH_KNIGHT_SUMMON_SOURCE =
            "selected-380_db-mobskill-45601-act2+npc45161+retained-L1MobSkillUse";
    static final String SUMMON_PLACEMENT_SOURCE =
            "retained-L1Location-randomLocation-max6+native-standing-floor+Quest-boundary";
    static final String ARMY_ANNOUNCEMENT_SOURCE =
            "GAME-Watch-Tokyo-lin22-lin23-lin24+native-S_MESSAGE+same-family-S_SystemMessage";
    static final String ROOM_ADVANCE_RULE_SOURCE =
            "4Gamer-Tokyo-2005-08-29-kill-room-monsters-to-enter-next-room";
    static final String PER_ROOM_SYMBOL_RULE_SOURCE =
            "Lineage-Compendium-2003-Hellgate+Terrible-Quest-"
                    + "kill-all-monsters+destroy-each-room-symbol";
    static final String MAP_SPECIFIC_FLOW_SOURCE =
            "native-map-513-Hellgate+527-Terrible-Quest-vs-"
                    + "2005-Battle-GP-direct-wave-flow";
    static final String FINAL_TOWER_PRESENTATION_SOURCE =
            "2005-JP-tournament-final-Guardian-Tower+selected-client-common-tower-sprite-family+event-hp-coordinate-provisional";
    static final int CORPSE_LIFETIME_SECONDS = 3;

    private static final long FIRST_OBJECT_ID = 0x6C00_0000L;
    private static final AtomicLong NEXT_OBJECT_ID = new AtomicLong(FIRST_OBJECT_ID);

    record Template(int npcId, int gfx, String name, int level, int maxHp,
                    int maximumMp, int intelligence) {
        Template {
            if (npcId <= 0 || gfx <= 0 || name == null || name.isBlank()
                    || level <= 0 || level > 0xFF || maxHp <= 0
                    || maximumMp < 0 || intelligence < 0) {
                throw new IllegalArgumentException("invalid Quest monster template");
            }
        }
    }

    @AsRecord(components = {"objectId", "force", "roomNumber", "npcId", "x", "y", "gfx", "name", "level", "hp", "maxHp", "alive", "currentMp", "maximumMp", "intelligence", "speciesSource", "countSource", "placementSource"}, finalFields = {"nativeSourceNpcFactoryMemory", "nativeSourceNpcFirstView"})
    static final class Snapshot {
        private final long objectId;
        private final int force;
        private final int roomNumber;
        private final int npcId;
        private final int x;
        private final int y;
        private final int gfx;
        private final String name;
        private final int level;
        private final int hp;
        private final int maxHp;
        private final boolean alive;
        private final int currentMp;
        private final int maximumMp;
        private final int intelligence;
        private final String speciesSource;
        private final String countSource;
        private final String placementSource;
        private volatile Object nativeBattleTamedCompanion;
        private Object nativeZombieCorpseActor;
        private RuntimeInventory nativeZombieCorpseBag;
        transient Object nativeSourceNpcFactoryMemory;
        transient Object nativeSourceNpcFirstView;

        Snapshot(long l, int n, int n2, int n3, int n4, int n5, int n6, String string, int n7, int n8, int n9, boolean bl, int n10, int n11, int n12, String string2, String string3, String string4) {
            this.objectId = l;
            this.force = n;
            this.roomNumber = n2;
            this.npcId = n3;
            this.x = n4;
            this.y = n5;
            this.gfx = n6;
            this.name = string;
            this.level = n7;
            this.hp = n8;
            this.maxHp = n9;
            this.alive = bl;
            this.currentMp = n10;
            this.maximumMp = n11;
            this.intelligence = n12;
            this.speciesSource = string2;
            this.countSource = string3;
            this.placementSource = string4;
        }




        public long objectId() {
            return this.objectId;
        }

        public int force() {
            return this.force;
        }

        public int roomNumber() {
            return this.roomNumber;
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

        public int gfx() {
            return this.gfx;
        }

        public String name() {
            return this.name;
        }

        public int level() {
            return this.level;
        }

        public int hp() {
            return this.hp;
        }

        public int maxHp() {
            return this.maxHp;
        }

        public boolean alive() {
            return this.alive;
        }

        public int currentMp() {
            return this.currentMp;
        }

        public int maximumMp() {
            return this.maximumMp;
        }

        public int intelligence() {
            return this.intelligence;
        }

        public String speciesSource() {
            return this.speciesSource;
        }

        public String countSource() {
            return this.countSource;
        }

        public String placementSource() {
            return this.placementSource;
        }

        Snapshot(long l, int n, int n2, int n3, int n4, int n5, int n6, String string, int n7, int n8, int n9, boolean bl, int n10, int n11, int n12, String string2, String string3, String string4, Object object) {
            this.objectId = l;
            this.force = n;
            this.roomNumber = n2;
            this.npcId = n3;
            this.x = n4;
            this.y = n5;
            this.gfx = n6;
            this.name = string;
            this.level = n7;
            this.hp = n8;
            this.maxHp = n9;
            this.alive = bl;
            this.currentMp = n10;
            this.maximumMp = n11;
            this.intelligence = n12;
            this.speciesSource = string2;
            this.countSource = string3;
            this.placementSource = string4;
            this.nativeSourceNpcFactoryMemory = object;
        }

        Snapshot(long l, int n, int n2, int n3, int n4, int n5, int n6, String string, int n7, int n8, int n9, boolean bl, int n10, int n11, int n12, String string2, String string3, String string4, Object object, Object object2) {
            this(l, n, n2, n3, n4, n5, n6, string, n7, n8, n9, bl, n10, n11, n12, string2, string3, string4, object);
            this.nativeSourceNpcFirstView = object2;
        }
    }

    record AiAttack(
            Snapshot actor, boolean magic,
            BattleTimeAttackMagicSkillCatalog.Definition magicSkill,
            BattleTimeAttackMagicSkillCatalog.PhysicalDefinition physicalSkill,
            BattleTimeAttackMagicSkillCatalog.SummonDefinition summonSkill,
            List<Snapshot> spawned, int triggerRoll, int selectedCandidate) {
        AiAttack {
            spawned = List.copyOf(spawned);
            if (actor == null || !actor.alive()
                    || magic != (magicSkill != null)
                    || magicSkill != null && (physicalSkill != null
                        || summonSkill != null || !spawned.isEmpty())
                    || physicalSkill != null && summonSkill != null
                    || summonSkill == null && !spawned.isEmpty()
                    || triggerRoll < 0 || triggerRoll > 100
                    || selectedCandidate < 0) {
                throw new IllegalArgumentException("invalid Quest AI attack");
            }
        }

        boolean summon() {
            return summonSkill != null;
        }
    }

    record Damage(Snapshot before, Snapshot after, boolean diedNow) {}

    record Healing(Snapshot before, Snapshot after, int restoredHp) {}

    record Resurrection(Snapshot before, Snapshot after) {}

    /** Authoritative mutable NPC MP result, initialized from the selected source row. */
    record ManaDrain(long objectId, int maximumMp, int beforeMp,
                     int afterMp, int requested, int drained) {}

    record MagicCancellation(Snapshot target, boolean haste, boolean slow, boolean blind,
                             boolean weakness, boolean disease,
                             boolean silence, boolean decayPotion, boolean polluteWater,
                             boolean strikerGale,
                             boolean sleep, boolean paralysis,
                             boolean freeze, boolean earthBind,
                             boolean thunderGrab, boolean horrorOfDeath,
                             boolean boneBreak,
                             boolean windShackle,
                             BattleElementalFallDownStatus.Cancellation
                                     elementalFallDown,
                             boolean eraseMagic,
                             boolean panic) {}

    record Hit(Damage damage, boolean waveCleared, int clearedRoom,
               List<Snapshot> spawned, boolean objectiveUnlocked) {
        Hit {
            spawned = List.copyOf(spawned);
        }

        boolean finalTowerUnlocked() {
            return objectiveUnlocked && clearedRoom == WAVES.size();
        }

    }

    @AsRecord(components = {"attackerObjectId", "target", "hits", "expired"})
    static final class PoisonUpdate {
        private final long attackerObjectId;
        private final Snapshot target;
        private final List<Hit> hits;
        private final boolean expired;
        volatile Object nativeSourceArenaPoisonProof;

        PoisonUpdate(long l, Snapshot snapshot, List<Hit> list, boolean bl) {
            if (l <= 0L || snapshot == null) {
                throw new IllegalArgumentException("invalid Quest poison update");
            }
            list = List.copyOf(list);
            this.attackerObjectId = l;
            this.target = snapshot;
            this.hits = list;
            this.expired = bl;
        }




        public long attackerObjectId() {
            return this.attackerObjectId;
        }

        public Snapshot target() {
            return this.target;
        }

        public List<Hit> hits() {
            return this.hits;
        }

        public boolean expired() {
            return this.expired;
        }
    }

    record Capture(Snapshot captured, boolean waveCleared, int clearedRoom,
                   List<Snapshot> spawned, boolean objectiveUnlocked) {
        Capture {
            if (captured == null) throw new IllegalArgumentException("captured actor is required");
            spawned = List.copyOf(spawned);
        }

        boolean finalTowerUnlocked() {
            return objectiveUnlocked && clearedRoom == WAVES.size();
        }

    }

    record ObjectiveCompletion(int force, int completedRoom,
                               List<Snapshot> spawned, boolean matchCompleted) {
        ObjectiveCompletion {
            spawned = List.copyOf(spawned);
            if (force < 0 || force > 1 || completedRoom < 1
                    || completedRoom > WAVES.size()
                    || matchCompleted != (completedRoom == WAVES.size())
                    || matchCompleted && !spawned.isEmpty()) {
                throw new IllegalArgumentException("invalid Quest objective completion");
            }
        }
    }

    /** Compatibility view retained for callers that only handle the final objective. */
    record FinalTowerCompletion(int force, int completedRoom) {}

    private record Corpse(Snapshot snapshot, long expiresAtNanos) {}
    private record SpawnPlan(Template template, Integer x, Integer y) {}

    private static final Template TURTLE_DRAGON =
            template(45262, 1649, "$1498", 26, 280, 10, 12);
    private static final Template ICEMAN =
            template(45207, 2158, "$1791", 26, 200, 20, 12);
    private static final Template MURYAN =
            template(45289, 1082, "$991", 30, 350, 50, 16);
    private static final Template SCORPION =
            template(45259, 1047, "$966", 26, 200, 30, 8);
    private static final Template OGRE =
            template(45278, 1020, "$953", 28, 500, 20, 8);
    private static final Template LIVING_ARMOR =
            template(45322, 1578, "$1408", 31, 300, 50, 10);
    private static final Template MINOTAUR =
            template(45390, 1517, "$1393", 37, 300, 20, 14);
    private static final Template MEDUSA =
            template(45380, 2535, "$2113", 36, 420, 120, 15);
    private static final Template DEATH_KNIGHT =
            template(45601, 240, "$371", 55, 7_500, 250, 19);

    // Species order is historically corroborated.  Room totals 2/7/9/7 are
    // photo-visible lower bounds; the later rooms' balanced per-species split
    // and every event-specific stat remain provisional as documented above.
    private static final List<List<Template>> WAVES = List.of(
            List.of(TURTLE_DRAGON, TURTLE_DRAGON),
            List.of(ICEMAN, MURYAN, ICEMAN, MURYAN,
                    ICEMAN, MURYAN, ICEMAN),
            List.of(SCORPION, OGRE, LIVING_ARMOR,
                    SCORPION, OGRE, LIVING_ARMOR,
                    SCORPION, OGRE, LIVING_ARMOR),
            List.of(MINOTAUR, MEDUSA, DEATH_KNIGHT,
                    MINOTAUR, MEDUSA, DEATH_KNIGHT, MINOTAUR));

    private static final Template SPARTOI =
            template(45161, 145, "$318", 16, 120, 30, 14);

    private final Lane[] lanes = {new Lane(0), new Lane(1)};
    private final ArenaMapCatalog.Arena[] arenas = new ArenaMapCatalog.Arena[2];
    private final List<Corpse> corpses = new ArrayList<>();
    private boolean active;
    private boolean perRoomSymbols;
    private List<List<SpawnPlan>> configuredWaves = List.of();
    private volatile Object sourceProcEncounterGeneration;

    synchronized void reset(BattleRules.RuleSet rules,
                            ArenaMapCatalog.Arena encounterArena) {
        reset(rules, encounterArena, BattleControlCatalog.defaults());
    }

    synchronized void reset(BattleRules.RuleSet rules,
                            ArenaMapCatalog.Arena encounterArena,
                            BattleControlCatalog controls) {
        deactivate();
        if (rules == null || encounterArena == null
                || rules.mode() != BattleRules.Mode.QUEST_BATTLE) return;
        arenas[0] = ArenaMapCatalog.forMap(BattleRules.arenaMapForForce(rules, 0));
        arenas[1] = ArenaMapCatalog.forMap(BattleRules.arenaMapForForce(rules, 1));
        perRoomSymbols = usesPerRoomSymbols(encounterArena.mapId());
        configuredWaves = configuredWaves(
                controls == null ? List.of()
                        : controls.spawns(rules.mode(), encounterArena.mapId()));
        active = true;
        for (Lane lane : lanes) {
            lane.roomIndex = 0;
            lane.awaitingObjective = false;
            lane.completed = false;
            lane.monsters.addAll(spawnWave(lane.force, lane.roomIndex));
        }
    }

    synchronized void deactivate() {
        NativeProcHealingEncounter.retire(this);
        this.active = false;
        this.perRoomSymbols = false;
        this.configuredWaves = List.of();
        this.arenas[0] = null;
        this.arenas[1] = null;
        for (Lane lane : this.lanes) {
            lane.roomIndex = 0;
            lane.awaitingObjective = false;
            lane.completed = false;
            lane.monsters.clear();
        }
        this.corpses.clear();
    }

    synchronized List<Snapshot> snapshots() {
        if (!active) return (List<Snapshot>) (List<?>) NativeSourceNpcFirstView.owner(List.of(), this);
        List<Snapshot> result = new ArrayList<>();
        for (Lane lane : lanes) {
            for (Monster monster : lane.monsters) {
                Snapshot snapshot = monster.snapshot();
                if (snapshot != null && snapshot.alive()) result.add(snapshot);
            }
        }
        return (List<Snapshot>) (List<?>) NativeSourceNpcFirstView.owner(List.copyOf(result), this);
    }

    synchronized List<Snapshot> snapshots(int force) {
        if (!active || force < 0 || force > 1) return List.of();
        List<Snapshot> result = new ArrayList<>();
        for (Monster monster : lanes[force].monsters) {
            Snapshot snapshot = monster.snapshot();
            if (snapshot != null && snapshot.alive()) result.add(snapshot);
        }
        return List.copyOf(result);
    }

    synchronized Snapshot find(long objectId) {
        for (Snapshot snapshot : snapshots()) {
            if (snapshot.objectId() == objectId) return snapshot;
        }
        return null;
    }

    synchronized boolean canAttack(long objectId, int attackerForce) {
        Snapshot target = find(objectId);
        return target != null && target.alive() && target.force() == attackerForce;
    }

    synchronized Hit damage(long objectId, int attackerForce, int amount) {
        if (!canAttack(objectId, attackerForce) || amount <= 0) return null;
        Lane lane = lanes[attackerForce];
        Monster target = lane.monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        if (target == null) return null;
        return applyDamage(lane, target, amount, false);
    }

    private Hit applyDamage(Lane lane, Monster target, int amount,
                            boolean poisonDamage) {
        return NativeSourcePoisonNpc.questDamage(this, lane, target, amount, poisonDamage);
    }

    Hit nativeSourcePoisonOriginalApplyDamage(Lane lane, Monster target, int amount,
                            boolean poisonDamage) {
        Damage damage = poisonDamage
                ? target.poisonDamage(amount) : target.damage(amount);
        if (damage != null && damage.diedNow()) {
            rememberCorpse(damage.after(), System.nanoTime());
        }
        if (damage == null || !damage.diedNow()) {
            return new Hit(damage, false, 0, List.of(), false);
        }
        boolean cleared = lane.monsters.stream().noneMatch(Monster::alive);
        if (!cleared) return new Hit(damage, false, 0, List.of(), false);

        int clearedRoom = lane.roomIndex + 1;
        lane.monsters.clear();
        if (perRoomSymbols) {
            lane.awaitingObjective = true;
            return new Hit(damage, true, clearedRoom, List.of(), true);
        }
        if (clearedRoom == WAVES.size()) {
            lane.awaitingObjective = true;
            return new Hit(damage, true, clearedRoom, List.of(), true);
        }
        lane.roomIndex++;
        lane.monsters.addAll(spawnWave(lane.force, lane.roomIndex));
        return new Hit(damage, true, clearedRoom, snapshots(lane.force), false);
    }

    synchronized boolean poison(long objectId, int attackerForce,
                                long attackerObjectId, long nowNanos,
                                int durationSeconds, int tickIntervalMillis) {
        if (!canAttack(objectId, attackerForce) || attackerObjectId <= 0
                || nowNanos < 0 || durationSeconds <= 0
                || tickIntervalMillis <= 0) return false;
        Monster target = lanes[attackerForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null && target.activatePoison(
                attackerObjectId, nowNanos, durationSeconds, tickIntervalMillis);
    }

    synchronized boolean poisonActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null && target.poisonActive(nowNanos);
    }

    synchronized boolean curePoison(long objectId) {
        Monster target = monster(objectId);
        return active && target != null && target.curePoison();
    }

    synchronized List<PoisonUpdate> pollPoisons(
            long nowNanos, int tickIntervalMillis, int tickDamage) {
        if (!active || nowNanos < 0 || tickIntervalMillis <= 0 || tickDamage <= 0) {
            return List.of();
        }
        List<PoisonUpdate> updates = new ArrayList<>();
        for (Lane lane : lanes) {
            for (Monster target : List.copyOf(lane.monsters)) {
                long attackerObjectId = target.poisonAttackerObjectId;
                BattleTimedStatus.PoisonPoll poll = target.pollPoison(
                        nowNanos, tickIntervalMillis);
                if (attackerObjectId <= 0
                        || (poll.damageTicks() == 0 && !poll.expired())) continue;
                List<Hit> hits = new ArrayList<>();
                for (int tick = 0; tick < poll.damageTicks() && target.alive(); tick++) {
                    Hit hit = applyDamage(lane, target, tickDamage, true);
                    if (hit != null && hit.damage() != null) hits.add(hit);
                }
                Snapshot finalTarget = hits.isEmpty()
                        ? target.snapshot() : hits.get(hits.size() - 1).damage().after();
                PoisonUpdate update = new PoisonUpdate(attackerObjectId, finalTarget,
                        hits, poll.expired());
                NativeSourceArenaPoison.stamp(update, target);
                updates.add(update);
            }
        }
        return List.copyOf(updates);
    }

    synchronized Capture capture(long objectId, int attackerForce) {
        if (!canAttack(objectId, attackerForce)) return null;
        Lane lane = lanes[attackerForce];
        Monster target = lane.monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        if (target == null) return null;
        Snapshot captured = target.snapshot();
        lane.monsters.remove(target);
        boolean cleared = lane.monsters.stream().noneMatch(Monster::alive);
        if (!cleared) return new Capture(captured, false, 0, List.of(), false);

        int clearedRoom = lane.roomIndex + 1;
        if (perRoomSymbols) {
            lane.awaitingObjective = true;
            return new Capture(captured, true, clearedRoom, List.of(), true);
        }
        if (clearedRoom == WAVES.size()) {
            lane.awaitingObjective = true;
            return new Capture(captured, true, clearedRoom, List.of(), true);
        }
        lane.roomIndex++;
        lane.monsters.addAll(spawnWave(lane.force, lane.roomIndex));
        return new Capture(captured, true, clearedRoom, snapshots(lane.force), false);
    }

    /**
     * Retained RETURN_TO_NATURE liberation: the controlled summon object is
     * replaced by a fresh wild monster object at its current tile and HP.
     * Capturing may already have advanced the lane; liberation deliberately
     * does not roll that progression back.
     */
    synchronized Snapshot releaseTamed(
            Snapshot captured, BattleCompanion.Snapshot companion,
            List<RuntimeInventory.Item> carriedItems) {
        if (captured == null || companion == null || !companion.active()
                || companion.kind() != BattleCompanion.Kind.TAMED
                || captured.npcId() != companion.npcId()
                || captured.force() < 0 || captured.force() >= lanes.length
                || carriedItems == null) {
            return null;
        }
        Lane lane = lanes[captured.force()];
        if (lane == null) return null;
        Monster released = new Monster(captured, companion, carriedItems);
        lane.monsters.add(released);
        return released.snapshot();
    }

    synchronized RuntimeInventory releasedInventory(long objectId) {
        for (Lane lane : lanes) {
            if (lane == null) continue;
            Monster monster = lane.monsters.stream()
                    .filter(candidate -> candidate.objectId == objectId)
                    .findFirst().orElse(null);
            if (monster != null) return monster.inventory.copy();
        }
        return null;
    }

    synchronized Snapshot findCorpse(long objectId, int force, long nowNanos) {
        if (!active || force < 0 || force > 1 || nowNanos < 0) return null;
        pruneCorpses(nowNanos);
        return corpses.stream()
                .map(Corpse::snapshot)
                .filter(corpse -> corpse.objectId() == objectId
                        && corpse.force() == force && !corpse.alive())
                .findFirst().orElse(null);
    }

    synchronized Snapshot consumeCorpse(long objectId, int force, long nowNanos) {
        Snapshot corpse = findCorpse(objectId, force, nowNanos);
        if (corpse == null) return null;
        corpses.removeIf(entry -> entry.snapshot().objectId() == objectId);
        for (Lane lane : lanes) {
            lane.monsters.removeIf(monster -> monster.objectId == objectId && !monster.alive());
        }
        return corpse;
    }

    synchronized Resurrection resurrectCorpse(long objectId, int force, long nowNanos) {
        return this.resurrectCorpse(objectId, force, nowNanos, 4);
    }

    synchronized Resurrection resurrectCorpse(long objectId, int force, long nowNanos, int hpDivisor) {
        Snapshot snapshot = this.findCorpse(objectId, force, nowNanos);
        if (hpDivisor != 1 && hpDivisor != 4 || snapshot == null || !BattleSpellCatalog.npcAllowsResurrection(snapshot.npcId())) {
            return (Resurrection)NativeCreateZombieLifecycle.resurrected(null, this, snapshot, 1);
        }
        Monster monster2 = null;
        for (Lane lane : this.lanes) {
            monster2 = lane.monsters.stream().filter(monster -> monster.objectId == objectId && !monster.alive()).findFirst().orElse(null);
            if (monster2 != null) break;
        }
        if (monster2 == null) {
            monster2 = new Monster(snapshot);
            this.lanes[force].monsters.add(monster2);
        }
        Snapshot snapshot2 = monster2.snapshot();
        monster2.resurrect(Math.max(1, snapshot.maxHp() / hpDivisor));
        this.corpses.removeIf(corpse -> corpse.snapshot().objectId() == objectId);
        return (Resurrection)NativeCreateZombieLifecycle.resurrected(new Resurrection(snapshot2, monster2.snapshot()), this, snapshot, 1);
    }

    private void rememberCorpse(Snapshot snapshot, long nowNanos) {
        NativeCreateZombieLifecycle.retainCorpse(this, snapshot, 1);
        if (snapshot == null || snapshot.alive() || nowNanos < 0L) {
            return;
        }
        this.corpses.removeIf(corpse -> corpse.snapshot().objectId() == snapshot.objectId());
        this.corpses.add(new Corpse(snapshot, Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(3L))));
    }

    private void pruneCorpses(long nowNanos) {
        corpses.removeIf(entry -> nowNanos >= entry.expiresAtNanos());
    }

    synchronized Healing heal(long objectId, int healerForce, int amount) {
        Snapshot visible = find(objectId);
        if (visible == null || !visible.alive() || visible.force() != healerForce
                || amount < 0) return null;
        Monster target = lanes[healerForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target == null ? null : target.heal(amount);
    }

    synchronized ManaDrain drainMana(long objectId, int attackerForce,
                                     int sourceMaximumMp, int requested) {
        if (!canAttack(objectId, attackerForce) || sourceMaximumMp < 0
                || requested <= 0) return null;
        Monster target = lanes[attackerForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target == null ? null : target.drainMana(sourceMaximumMp, requested);
    }

    synchronized int recoverMana(long objectId, int amount) {
        if (amount <= 0) return 0;
        Monster target = lanes[0].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        if (target == null) {
            target = lanes[1].monsters.stream()
                    .filter(monster -> monster.objectId == objectId)
                    .findFirst().orElse(null);
        }
        return target == null ? 0 : target.recoverMana(amount);
    }

    synchronized ManaDrain drainManaIfStrictlyAbove(
            long objectId, int attackerForce,
            int sourceMaximumMp, int requested) {
        if (!canAttack(objectId, attackerForce) || sourceMaximumMp < 0
                || requested <= 0) return null;
        Monster target = lanes[attackerForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target == null ? null
                : target.drainManaIfStrictlyAbove(sourceMaximumMp, requested);
    }

    synchronized boolean haste(long objectId, int casterForce, long nowNanos,
                               int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null && target.activateHaste(nowNanos, durationSeconds);
    }

    synchronized boolean hasteActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.hasteActive(nowNanos);
    }

    synchronized boolean slow(long objectId, int casterForce, long nowNanos,
                              int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null && target.activateSlow(nowNanos, durationSeconds);
    }

    synchronized boolean slowActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.slowActive(nowNanos);
    }

    synchronized List<Snapshot> expireHastes(long nowNanos) {
        if (nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Lane lane : lanes) {
            for (Monster monster : lane.monsters) {
                if (monster.alive() && monster.expireHaste(nowNanos)) {
                    expired.add(monster.snapshot());
                }
            }
        }
        return List.copyOf(expired);
    }

    synchronized List<Snapshot> expireSlows(long nowNanos) {
        if (nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Lane lane : lanes) {
            for (Monster monster : lane.monsters) {
                if (monster.alive() && monster.expireSlow(nowNanos)) {
                    expired.add(monster.snapshot());
                }
            }
        }
        return List.copyOf(expired);
    }

    synchronized MagicCancellation cancelMagicEffects(long objectId,
                                                       int casterForce) {
        if (!canAttack(objectId, casterForce)) return null;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target == null ? null : target.cancelMagicEffects();
    }

    synchronized boolean blind(long objectId, int casterForce, long nowNanos,
                               int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null && target.activateBlind(nowNanos, durationSeconds);
    }

    synchronized boolean blindActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null && target.blindActive(nowNanos);
    }

    synchronized int addWeaponBreakStack(long objectId, int casterForce) {
        if (!canAttack(objectId, casterForce)) return -1;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target == null ? -1 : target.addWeaponBreakStack();
    }

    synchronized int weaponBreakStacks(long objectId) {
        Monster target = monster(objectId);
        return active && target != null ? target.weaponBreakStacks : 0;
    }

    synchronized boolean weakness(long objectId, int casterForce, long nowNanos,
                                  int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null && target.activateWeakness(nowNanos, durationSeconds);
    }

    synchronized boolean weaknessActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null && target.weaknessActive(nowNanos);
    }

    synchronized boolean disease(long objectId, int casterForce, long nowNanos,
                                 int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null && target.activateDisease(nowNanos, durationSeconds);
    }

    synchronized boolean diseaseActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null && target.diseaseActive(nowNanos);
    }

    synchronized boolean silence(long objectId, int casterForce, long nowNanos,
                                 int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null && target.activateSilence(nowNanos, durationSeconds);
    }

    synchronized boolean confusionSilence(
            long objectId, int casterForce, long nowNanos,
            int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null
                && target.activateSilenceIfAbsent(nowNanos, durationSeconds);
    }

    synchronized boolean areaSilence(long objectId, int casterForce,
                                     long nowNanos, int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null
                && target.activateAreaSilence(nowNanos, durationSeconds);
    }

    synchronized boolean windShackle(long objectId, int casterForce,
                                     long nowNanos, int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null
                && target.activateWindShackle(nowNanos, durationSeconds);
    }

    synchronized boolean windShackleActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null && target.windShackleActive(nowNanos);
    }

    synchronized boolean silenceActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null && target.silenceActive(nowNanos);
    }

    synchronized boolean decayPotion(long objectId, int casterForce,
                                     long nowNanos, int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null
                && target.activateDecayPotion(nowNanos, durationSeconds);
    }

    synchronized boolean decayPotionActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null && target.decayPotionActive(nowNanos);
    }

    synchronized boolean polluteWater(long objectId, int casterForce,
                                      long nowNanos, int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null
                && target.activatePolluteWater(nowNanos, durationSeconds);
    }

    synchronized boolean polluteWaterActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null && target.polluteWaterActive(nowNanos);
    }

    synchronized boolean strikerGale(long objectId, int casterForce,
                                     long nowNanos, int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null
                && target.activateStrikerGale(nowNanos, durationSeconds);
    }

    synchronized boolean strikerGaleActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null && target.strikerGaleActive(nowNanos);
    }

    synchronized boolean elementalFallDown(
            long objectId, int casterForce, long nowNanos,
            int durationSeconds, int sourceElfAttribute) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null && target.activateElementalFallDown(
                nowNanos, durationSeconds, sourceElfAttribute);
    }

    synchronized int elementalFallDownModifier(
            long objectId, int attributeKind, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null
                ? target.elementalFallDownStatus.modifier(
                        attributeKind, nowNanos) : 0;
    }

    synchronized boolean eraseMagic(long objectId, int casterForce,
                                    long nowNanos, int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null
                && target.activateEraseMagic(nowNanos, durationSeconds);
    }

    synchronized int effectiveMagicResistance(
            long objectId, int baseMagicResistance, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null
                ? target.eraseMagicStatus.effectiveMagicResistance(
                        baseMagicResistance, nowNanos)
                : baseMagicResistance;
    }

    synchronized boolean consumeEraseMagic(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null
                && target.eraseMagicStatus.consume(nowNanos);
    }

    synchronized BattleEraseMagicStatus.Lease captureEraseMagic(
            long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null
                ? target.eraseMagicStatus.capture(nowNanos)
                : BattleEraseMagicStatus.Lease.none();
    }

    synchronized boolean consumeEraseMagic(
            long objectId, BattleEraseMagicStatus.Lease lease) {
        Monster target = monster(objectId);
        return active && target != null && target.eraseMagicStatus.consume(lease);
    }

    /** Retained NPC casters consume their Silence lease on the rejected spell attempt. */
    synchronized boolean consumeSilenceForSpell(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null && target.consumeSilenceForSpell(nowNanos);
    }

    synchronized boolean sleep(long objectId, int casterForce, long nowNanos,
                               int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null && target.activateSleep(nowNanos, durationSeconds);
    }

    synchronized boolean sleepActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null && target.sleepActive(nowNanos);
    }

    synchronized boolean cancelSleep(long objectId) {
        Monster target = monster(objectId);
        return active && target != null && target.cancelSleep();
    }

    synchronized boolean freeze(long objectId, int casterForce, long nowNanos,
                                int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null && target.activateFreeze(nowNanos, durationSeconds);
    }

    synchronized boolean freezeActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null && target.freezeActive(nowNanos);
    }

    synchronized boolean cancelFreeze(long objectId) {
        Monster target = monster(objectId);
        return active && target != null && target.cancelFreeze();
    }

    synchronized boolean paralysis(long objectId, int casterForce, long nowNanos,
                                   int durationMillis) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationMillis <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null && target.activateParalysis(nowNanos, durationMillis);
    }

    synchronized boolean paralysisActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null && target.paralysisActive(nowNanos);
    }

    synchronized boolean shockStun(long objectId, int casterForce, long nowNanos,
                                   int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null && target.activateShockStun(nowNanos, durationSeconds);
    }

    synchronized boolean shockStunActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null && target.shockStunActive(nowNanos);
    }

    synchronized boolean boneBreak(long objectId, int casterForce,
                                   long nowNanos, int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null
                && target.activateBoneBreak(nowNanos, durationSeconds);
    }

    synchronized boolean boneBreakActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null && target.boneBreakActive(nowNanos);
    }

    synchronized BattleArmBreakerStatus.Activation armBreaker(
            long objectId, int casterForce, long nowNanos,
            int durationSeconds) {
        if (!canAttack(objectId, casterForce)) {
            return BattleArmBreakerStatus.Activation.rejected();
        }
        Monster target = monster(objectId);
        return target == null
                ? BattleArmBreakerStatus.Activation.rejected()
                : target.armBreakerStatus.activateIfAbsent(
                        nowNanos, durationSeconds);
    }

    synchronized int armBreakerMeleeDamageModifier(
            long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null
                ? target.armBreakerStatus.meleeDamageModifier(nowNanos) : 0;
    }

    synchronized boolean cancelArmBreaker(long objectId) {
        Monster target = monster(objectId);
        return active && target != null && target.armBreakerStatus.cancel();
    }

    synchronized boolean panic(long objectId, int casterForce,
                               long nowNanos, int durationSeconds,
                               int abilityScoreModifier) {
        if (!canAttack(objectId, casterForce)) return false;
        Monster target = monster(objectId);
        return target != null && target.panicStatus.activateIfAbsent(
                nowNanos, durationSeconds, abilityScoreModifier);
    }

    synchronized int panicAbilityScoreModifier(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null
                ? target.panicStatus.snapshot(nowNanos).abilityScoreModifier() : 0;
    }

    synchronized boolean cancelPanic(long objectId) {
        Monster target = monster(objectId);
        return active && target != null && target.panicStatus.cancel();
    }

    synchronized List<Snapshot> expirePanics(long nowNanos) {
        if (nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Lane lane : lanes) {
            for (Monster monster : lane.monsters) {
                if (monster.alive() && monster.panicStatus.expire(nowNanos)) {
                    expired.add(monster.snapshot());
                }
            }
        }
        return List.copyOf(expired);
    }

    synchronized BattleJoyOfPainStatus.Activation joyOfPain(
            long objectId, int casterForce, long nowNanos,
            int durationSeconds) {
        if (!canAttack(objectId, casterForce)) return null;
        Monster target = monster(objectId);
        return target == null ? null : target.joyOfPainStatus.activateOrRefresh(
                nowNanos, durationSeconds);
    }

    synchronized BattleJoyOfPainStatus.Lease captureJoyOfPain(
            long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null
                ? target.joyOfPainStatus.capture(nowNanos) : null;
    }

    synchronized boolean consumeJoyOfPain(
            long objectId, BattleJoyOfPainStatus.Lease lease) {
        Monster target = monster(objectId);
        return active && target != null
                && target.joyOfPainStatus.consume(lease);
    }

    synchronized List<Snapshot> expireJoyOfPain(long nowNanos) {
        if (nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Lane lane : lanes) {
            for (Monster monster : lane.monsters) {
                if (monster.alive()
                        && monster.joyOfPainStatus.expire(nowNanos)) {
                    expired.add(monster.snapshot());
                }
            }
        }
        return List.copyOf(expired);
    }

    synchronized boolean thunderGrab(long objectId, int casterForce,
                                     long nowNanos, int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null
                && target.activateThunderGrab(nowNanos, durationSeconds);
    }

    synchronized boolean thunderGrabActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null && target.thunderGrabActive(nowNanos);
    }

    synchronized boolean horrorOfDeath(long objectId, int casterForce,
                                       long nowNanos, int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null
                && target.activateHorrorOfDeath(nowNanos, durationSeconds);
    }

    synchronized boolean horrorOfDeathActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null
                && target.horrorOfDeathActive(nowNanos);
    }

    synchronized List<Snapshot> expireHorrorOfDeaths(long nowNanos) {
        if (!active || nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Lane lane : lanes) {
            for (Monster monster : lane.monsters) {
                if (monster.alive()
                        && monster.damagePoison.expireHorrorOfDeath(nowNanos)) {
                    expired.add(monster.snapshot());
                }
            }
        }
        return List.copyOf(expired);
    }

    synchronized boolean cancelParalysis(long objectId) {
        Monster target = monster(objectId);
        return active && target != null && target.cancelParalysis();
    }

    synchronized List<Snapshot> expireParalyses(long nowNanos) {
        if (!active || nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Lane lane : lanes) {
            for (Monster monster : lane.monsters) {
                if (monster.alive() && monster.expireParalysis(nowNanos)) {
                    expired.add(monster.snapshot());
                }
            }
        }
        return List.copyOf(expired);
    }

    synchronized List<Snapshot> expireFreezes(long nowNanos) {
        if (!active || nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Lane lane : lanes) {
            for (Monster monster : lane.monsters) {
                if (monster.alive() && monster.expireFreeze(nowNanos)) {
                    expired.add(monster.snapshot());
                }
            }
        }
        return List.copyOf(expired);
    }

    synchronized boolean earthBind(long objectId, int casterForce, long nowNanos,
                                   int durationSeconds) {
        if (!canAttack(objectId, casterForce) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = lanes[casterForce].monsters.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null && target.activateEarthBind(nowNanos, durationSeconds);
    }

    synchronized boolean earthBindActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return active && target != null && target.earthBindActive(nowNanos);
    }

    synchronized List<Snapshot> expireEarthBinds(long nowNanos) {
        if (!active || nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Lane lane : lanes) {
            for (Monster monster : lane.monsters) {
                if (monster.alive() && monster.expireEarthBind(nowNanos)) {
                    expired.add(monster.snapshot());
                }
            }
        }
        return List.copyOf(expired);
    }

    static boolean usesPerRoomSymbols(int mapId) {
        return mapId == 513 || mapId == 527;
    }

    synchronized boolean usesPerRoomSymbols() {
        return active && perRoomSymbols;
    }

    synchronized boolean objectiveUnlocked(int force) {
        return active && force >= 0 && force <= 1
                && lanes[force].awaitingObjective;
    }

    synchronized boolean finalTowerUnlocked(int force) {
        return objectiveUnlocked(force)
                && lanes[force].roomIndex + 1 == WAVES.size();
    }

    synchronized BattleGuardianTower.ObjectiveKind pendingObjectiveKind(int force) {
        if (!objectiveUnlocked(force)) return null;
        if (!perRoomSymbols) {
            return BattleGuardianTower.ObjectiveKind.QUEST_FINAL_GUARDIAN_TOWER;
        }
        return lanes[force].roomIndex + 1 == WAVES.size()
                ? BattleGuardianTower.ObjectiveKind.QUEST_FINAL_SYMBOL
                : BattleGuardianTower.ObjectiveKind.QUEST_ROOM_SYMBOL;
    }

    synchronized String pendingObjectivePlacementSource(int force) {
        BattleGuardianTower.ObjectiveKind kind = pendingObjectiveKind(force);
        return kind == BattleGuardianTower.ObjectiveKind.QUEST_ROOM_SYMBOL
                        || kind == BattleGuardianTower.ObjectiveKind.QUEST_FINAL_SYMBOL
                ? PER_ROOM_SYMBOL_RULE_SOURCE
                        + "+selected-client-common-tower-sprite-family+"
                        + "event-gfx-hp-coordinate-provisional"
                : FINAL_TOWER_PRESENTATION_SOURCE;
    }

    synchronized ArenaMapCatalog.Point pendingObjectivePoint(int force) {
        if (!objectiveUnlocked(force)) return null;
        ArenaMapCatalog.Arena laneArena = arenas[force];
        int roomNumber = lanes[force].roomIndex + 1;
        ArenaMapCatalog.Point anchor = BattleQuestBoundary.roomAnchor(
                laneArena.mapId(), roomNumber);
        if (anchor == null) anchor = laneArena.spawn(3 + roomNumber);
        ArenaMapCatalog.Point candidate = new ArenaMapCatalog.Point(
                anchor.x(), anchor.y() + 1);
        return laneArena.nativeCanStep(
                anchor.x(), anchor.y(), candidate.x(), candidate.y())
                ? candidate : anchor;
    }

    synchronized ArenaMapCatalog.Point finalTowerPoint(int force) {
        return finalTowerUnlocked(force) ? pendingObjectivePoint(force) : null;
    }

    synchronized ObjectiveCompletion destroyObjective(int force) {
        if (!active || force < 0 || force > 1) return null;
        Lane lane = lanes[force];
        if (lane.completed || !lane.awaitingObjective) return null;
        int completedRoom = lane.roomIndex + 1;
        lane.awaitingObjective = false;
        if (completedRoom == WAVES.size()) {
            lane.completed = true;
            return new ObjectiveCompletion(
                    force, completedRoom, List.of(), true);
        }
        if (!perRoomSymbols) {
            throw new IllegalStateException(
                    "non-symbol Quest flow exposed an intermediate objective");
        }
        lane.roomIndex++;
        lane.monsters.addAll(spawnWave(lane.force, lane.roomIndex));
        return new ObjectiveCompletion(
                force, completedRoom, snapshots(force), false);
    }

    synchronized FinalTowerCompletion destroyFinalTower(int force) {
        if (!finalTowerUnlocked(force)) return null;
        ObjectiveCompletion completion = destroyObjective(force);
        return completion == null || !completion.matchCompleted()
                ? null : new FinalTowerCompletion(force, completion.completedRoom());
    }

    synchronized int roomNumber(int force) {
        return active && force >= 0 && force <= 1
                ? lanes[force].roomIndex + 1 : 0;
    }

    synchronized boolean blocks(int force, int x, int y) {
        if (!active || force < 0 || force > 1) return false;
        return lanes[force].monsters.stream()
                .anyMatch(monster -> monster.alive() && monster.x == x && monster.y == y);
    }

    synchronized Snapshot moveForAi(long objectId, int x, int y, long nowNanos) {
        Monster monster = this.monster(objectId);
        if (!this.active || monster == null || !NativeSourceNpcMovement.allowed(monster.ready(nowNanos), monster) || monster.blindActive(nowNanos) || monster.sleepActive(nowNanos) || monster.paralysisActive(nowNanos) || monster.shockStunActive(nowNanos) || monster.boneBreakActive(nowNanos) || monster.thunderGrabActive(nowNanos) || monster.freezeActive(nowNanos) || monster.earthBindActive(nowNanos)) {
            return null;
        }
        ArenaMapCatalog.Arena arena = this.arenas[monster.force];
        if (arena == null || !arena.nativeCanStep(monster.x, monster.y, x, y) || BattleQuestBoundary.blocksMap(arena.mapId(), monster.roomNumber, monster.x, monster.y, x, y)) {
            return null;
        }
        Lane lane = this.lanes[monster.force];
        boolean bl = lane.monsters.stream().anyMatch(monster2 -> monster2 != monster && monster2.alive() && monster2.x == x && monster2.y == y);
        if (bl) {
            return null;
        }
        monster.x = NativeSourceNpcMotion.moving(x, monster, y);
        monster.y = y;
        monster.schedule(nowNanos, 0);
        return monster.snapshot();
    }

    synchronized Snapshot attackForAi(long objectId, long nowNanos) {
        Monster monster = monster(objectId);
        if (!active || monster == null || !monster.ready(nowNanos)
                || monster.sleepActive(nowNanos)
                || monster.paralysisActive(nowNanos)
                || monster.shockStunActive(nowNanos)
                || monster.boneBreakActive(nowNanos)
                || monster.thunderGrabActive(nowNanos)
                || monster.freezeActive(nowNanos)
                || monster.earthBindActive(nowNanos)
                || !BattleAnimationCatalog.get().supportsTimedAction(
                        monster.template.gfx(), BattleActionCodes.ATTACK)) return null;
        monster.schedule(nowNanos, BattleActionCodes.ATTACK);
        return monster.snapshot();
    }

    /**
     * Retained L1NpcInstance.attack:894-897 ordinary-attack coin. Roll 100
     * never passes the strict {@code roll < TriRnd} test, so the row-fixture
     * path falls through to its ordinary adjacent attack.
     */
    private static void applyOrdinaryAttackCoin(Monster monster, int targetX, int targetY,
                                                int[] triggerRolls,
                                                ThreadLocalRandom random) {
        int distance = NativeSourceNpcMotion.onTarget(
                Math.max(Math.abs(monster.x - targetX), Math.abs(monster.y - targetY)), monster);
        if (BattleTimeAttackMagicSkillCatalog.ordinaryAttackBranch(
                distance, random.nextInt(2))) {
            java.util.Arrays.fill(triggerRolls, 100);
        }
    }

    synchronized AiAttack aiAttackFor(long objectId, int targetX, int targetY, long nowNanos) {
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        Monster monster = this.monster(objectId);
        if (monster == null) {
            return null;
        }
        List<BattleTimeAttackMagicSkillCatalog.MobskillRow> list = BattleTimeAttackMagicSkillCatalog.rowsForNpc(monster.template.npcId());
        int[] nArray = new int[list.size()];
        for (int i = 0; i < nArray.length; ++i) {
            nArray[i] = threadLocalRandom.nextInt(1, 101);
        }
        applyOrdinaryAttackCoin(monster, targetX, targetY, nArray, threadLocalRandom);
        BattleTimeAttackMagicSkillCatalog.SummonDefinition summonDefinition = BattleTimeAttackMagicSkillCatalog.summonForNpc(monster.template.npcId());
        int n3 = summonDefinition == null ? 0 : threadLocalRandom.nextInt(summonDefinition.summonRandomBound());
        return this.aiAttackFor(objectId, targetX, targetY, nowNanos, nArray, threadLocalRandom.nextInt(Integer.MAX_VALUE), n3);
    }

    /** Deterministic fixture preserving every selected mobskill actNo. */
    synchronized AiAttack aiAttackForActNoFixture(
            long objectId, int targetX, int targetY, long nowNanos,
            int actNo, int candidateSelector) {
        if (actNo < 0 || candidateSelector < 0) {
            throw new IllegalArgumentException("invalid Quest actNo fixture");
        }
        Monster monster = monster(objectId);
        List<BattleTimeAttackMagicSkillCatalog.MobskillRow> rows = monster == null
                ? List.of() : BattleTimeAttackMagicSkillCatalog.rowsForNpc(
                        monster.template.npcId());
        int[] triggerRolls = new int[rows.size()];
        java.util.Arrays.fill(triggerRolls, 100);
        int selectedIndex = -1;
        for (int index = 0; index < rows.size(); index++) {
            if (rows.get(index).actNo() == actNo) {
                selectedIndex = index;
                triggerRolls[index] = 1;
                break;
            }
        }
        if (selectedIndex < 0) return null;
        BattleTimeAttackMagicSkillCatalog.SummonDefinition summon =
                BattleTimeAttackMagicSkillCatalog.summonForNpc(
                        monster.template.npcId());
        int summonCountRoll = summon == null ? 0
                : candidateSelector % summon.summonRandomBound();
        return aiAttackFor(objectId, targetX, targetY, nowNanos,
                triggerRolls, candidateSelector, summonCountRoll);
    }

    synchronized AiAttack aiAttackFor(
            long objectId, int targetX, int targetY, long nowNanos,
            int[] triggerRolls, int candidateSelector, int summonCountRoll) {
        if (triggerRolls == null || candidateSelector < 0 || summonCountRoll < 0) {
            throw new IllegalArgumentException("invalid Quest mobskill fixture");
        }
        Monster monster = monster(objectId);
        List<BattleTimeAttackMagicSkillCatalog.MobskillRow> rows = monster == null
                ? List.of() : BattleTimeAttackMagicSkillCatalog.rowsForNpc(
                        monster.template.npcId());
        if (triggerRolls.length != rows.size()) {
            throw new IllegalArgumentException("Quest mobskill roll count mismatch");
        }
        for (int roll : triggerRolls) {
            if (roll < 1 || roll > 100) {
                throw new IllegalArgumentException("invalid Quest mobskill roll");
            }
        }
        if (!active || monster == null || !monster.ready(nowNanos)
                || monster.sleepActive(nowNanos)
                || monster.paralysisActive(nowNanos)
                || monster.shockStunActive(nowNanos)
                || monster.boneBreakActive(nowNanos)
                || monster.thunderGrabActive(nowNanos)
                || monster.freezeActive(nowNanos)
                || monster.earthBindActive(nowNanos)) return null;

        int distance = NativeSourceNpcMotion.onTarget(
                Math.max(Math.abs(monster.x - targetX), Math.abs(monster.y - targetY)), monster);
        record Eligible(BattleTimeAttackMagicSkillCatalog.MobskillRow row,
                        int triggerRoll) {}
        List<Eligible> eligible = new ArrayList<>(rows.size());
        for (int index = 0; index < rows.size(); index++) {
            BattleTimeAttackMagicSkillCatalog.MobskillRow row = rows.get(index);
            int roll = triggerRolls[index];
            if (!row.triggerDistance(distance) || !row.triggerRoll(roll)) continue;
            if (row instanceof BattleTimeAttackMagicSkillCatalog.Definition magic) {
                if (magic.castDistance(distance)
                        && !monster.silenceActive(nowNanos)
                        && monster.magicReady(magic, nowNanos)
                        && BattleAnimationCatalog.get().supportsTimedAction(
                                monster.template.gfx(), magic.actionId())) {
                    eligible.add(new Eligible(row, roll));
                }
            } else if (row instanceof BattleTimeAttackMagicSkillCatalog
                    .PhysicalDefinition physical) {
                if (physical.attackDistance(distance)
                        && BattleAnimationCatalog.get().supportsTimedAction(
                                monster.template.gfx(),
                                physical.effectiveActionId())) {
                    eligible.add(new Eligible(row, roll));
                }
            } else if (row instanceof BattleTimeAttackMagicSkillCatalog
                    .SummonDefinition summon && monster.summonReady(summon)
                    && BattleAnimationCatalog.get().supportsTimedAction(
                            monster.template.gfx(), summon.actionId())) {
                eligible.add(new Eligible(row, roll));
            }
        }

        if (!eligible.isEmpty()) {
            int selected = candidateSelector % eligible.size();
            Eligible chosen = eligible.get(selected);
            if (chosen.row() instanceof BattleTimeAttackMagicSkillCatalog
                    .Definition magic) {
                monster.scheduleMagic(nowNanos, magic);
                return new AiAttack(monster.snapshot(), true, magic,
                        null, null, List.of(), chosen.triggerRoll(), selected);
            }
            if (chosen.row() instanceof BattleTimeAttackMagicSkillCatalog
                    .PhysicalDefinition physical) {
                Monster scheduledMonster = monster;
                long scheduledAt = nowNanos;
                int actionId = physical.effectiveActionId();
                NativeSourceNpcMotion.facing(monster, targetX, targetY);
                scheduledMonster.schedule(scheduledAt, actionId);
                return new AiAttack(monster.snapshot(), false, null,
                        physical, null, List.of(), chosen.triggerRoll(), selected);
            }
            BattleTimeAttackMagicSkillCatalog.SummonDefinition summon =
                    (BattleTimeAttackMagicSkillCatalog.SummonDefinition) chosen.row();
            int count = summon.summonCount(
                    summonCountRoll % summon.summonRandomBound());
            List<Snapshot> spawned = spawnSummons(
                    monster, summon, count, candidateSelector);
            monster.scheduleSummon(nowNanos, summon);
            return new AiAttack(monster.snapshot(), false, null,
                    null, summon, spawned, chosen.triggerRoll(), selected);
        }

        if (distance <= 1 && BattleAnimationCatalog.get().supportsTimedAction(
                monster.template.gfx(), BattleActionCodes.ATTACK)) {
            Monster scheduledMonster = monster;
            long scheduledAt = nowNanos;
            int actionId = BattleActionCodes.ATTACK;
            NativeSourceNpcMotion.facing(monster, targetX, targetY);
            scheduledMonster.schedule(scheduledAt, actionId);
            return new AiAttack(monster.snapshot(), false, null,
                    null, null, List.of(), 0, 0);
        }
        return null;
    }

    private List<Snapshot> spawnSummons(
            Monster caster,
            BattleTimeAttackMagicSkillCatalog.SummonDefinition summon,
            int requestedCount, int placementSelector) {
        if (caster == null || summon == null
                || summon.summonNpcId() != SPARTOI.npcId()
                || requestedCount <= 0 || placementSelector < 0) return List.of();
        ArenaMapCatalog.Arena laneArena = arenas[caster.force];
        if (laneArena == null) return List.of();
        Lane lane = lanes[caster.force];
        List<ArenaMapCatalog.Point> candidates = new ArrayList<>();
        // Retained L1MobSkillUseSpawn calls randomLocation(6, false) once for
        // every summon.  Preserve that full Chebyshev square, while retaining
        // this standalone server's native-floor and force-room isolation.
        for (int y = caster.y - 6; y <= caster.y + 6; y++) {
            for (int x = caster.x - 6; x <= caster.x + 6; x++) {
                int candidateX = x;
                int candidateY = y;
                if (candidateX == caster.x && candidateY == caster.y
                        || !laneArena.nativeStandingFloor(candidateX, candidateY)
                        || BattleQuestBoundary.blocksMap(laneArena.mapId(),
                            caster.roomNumber, caster.x, caster.y,
                            candidateX, candidateY)
                        || lane.monsters.stream().anyMatch(actor -> actor.alive()
                            && actor.x == candidateX
                            && actor.y == candidateY)) continue;
                candidates.add(new ArenaMapCatalog.Point(candidateX, candidateY));
            }
        }
        List<Snapshot> spawned = new ArrayList<>();
        while (spawned.size() < requestedCount && !candidates.isEmpty()) {
            int index = Math.floorMod(placementSelector
                    + spawned.size() * 31, candidates.size());
            ArenaMapCatalog.Point point = candidates.remove(index);
            Monster actor = new Monster(caster.force, caster.roomNumber,
                    SPARTOI, point.x(), point.y(), true);
            lane.monsters.add(actor);
            spawned.add(actor.snapshot());
        }
        return List.copyOf(spawned);
    }

    static List<List<Template>> waves() {
        return WAVES;
    }

    /**
     * Contemporary Tokyo photographs visibly show both force progress lines
     * when armies two through four appear.  The text is representable in the
     * selected Traditional-Chinese legacy charset and is transported through
     * the already recovered native S_MESSAGE envelope.
     */
    static String armyAnnouncement(int force, int roomNumber) {
        if (force < 0 || force > 1 || roomNumber < 2 || roomNumber > 4) {
            throw new IllegalArgumentException("invalid Quest army announcement");
        }
        return "Force " + (force == 0 ? "Red" : "Blue")
                + ": 第" + roomNumber + "軍登場!";
    }

    private List<Monster> spawnWave(int force, int roomIndex) {
        List<SpawnPlan> plans = configuredWaves.isEmpty()
                ? WAVES.get(roomIndex).stream()
                        .map(template -> new SpawnPlan(template, null, null)).toList()
                : configuredWaves.get(roomIndex);
        ArenaMapCatalog.Arena laneArena = arenas[force];
        ArenaMapCatalog.Point anchor = BattleQuestBoundary.roomAnchor(
                laneArena.mapId(), roomIndex + 1);
        if (anchor == null) anchor = laneArena.spawn(4 + roomIndex);
        List<ArenaMapCatalog.Point> points = placementPoints(
                laneArena, anchor, plans.size(), roomIndex + 1);
        List<Monster> result = new ArrayList<>(plans.size());
        Set<ArenaMapCatalog.Point> used = new HashSet<>();
        for (int index = 0; index < plans.size(); index++) {
            SpawnPlan plan = plans.get(index);
            ArenaMapCatalog.Point point = plan.x() == null ? points.get(index)
                    : new ArenaMapCatalog.Point(plan.x(), plan.y());
            if (!laneArena.nativeStandingFloor(point.x(), point.y())
                    || !used.add(point)) {
                throw new IllegalStateException(
                        "database Quest spawn is blocked or duplicated map="
                                + laneArena.mapId() + " wave=" + (roomIndex + 1)
                                + " point=" + point);
            }
            result.add(new Monster(force, roomIndex + 1,
                    plan.template(), point.x(), point.y(), false,
                    configuredWaves.isEmpty() ? PLACEMENT_SOURCE
                            : DATABASE_SPAWN_SOURCE));
        }
        return result;
    }

    private static List<List<SpawnPlan>> configuredWaves(
            List<BattleControlCatalog.Spawn> rows) {
        if (rows == null || rows.isEmpty()) return List.of();
        List<List<SpawnPlan>> result = new ArrayList<>();
        for (int wave = 1; wave <= WAVES.size(); wave++) {
            List<SpawnPlan> plans = new ArrayList<>();
            for (BattleControlCatalog.Spawn row : rows) {
                if (row.wave() != wave) continue;
                Template template = templateByNpc(row.npcId());
                for (int index = 0; index < row.quantity(); index++) {
                    plans.add(new SpawnPlan(template, row.x(), row.y()));
                }
            }
            if (plans.isEmpty() || plans.size() > 100) {
                throw new IllegalArgumentException(
                        "database Quest configuration requires waves 1..4");
            }
            result.add(List.copyOf(plans));
        }
        if (rows.stream().anyMatch(row -> row.wave() > WAVES.size())) {
            throw new IllegalArgumentException("database Quest wave exceeds native flow");
        }
        return List.copyOf(result);
    }

    private static Template templateByNpc(int npcId) {
        return WAVES.stream().flatMap(List::stream)
                .filter(template -> template.npcId() == npcId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "database Quest spawn uses unsupported NPC " + npcId));
    }

    /**
     * Selects a compact, connected and reproducible local floor envelope.
     * This prevents the photographed multi-actor lower bounds from being
     * collapsed onto one horizontal line while preserving the explicit fact
     * that original event-server coordinates remain unavailable.
     */
    private static List<ArenaMapCatalog.Point> placementPoints(
            ArenaMapCatalog.Arena arena, ArenaMapCatalog.Point anchor,
            int count, int roomNumber) {
        if (!arena.nativeStandingFloor(anchor.x(), anchor.y())) {
            throw new IllegalStateException("Quest anchor is not native standing floor: "
                    + arena.mapId() + " room=" + roomNumber);
        }
        int radius = 6;
        ArrayDeque<ArenaMapCatalog.Point> queue = new ArrayDeque<>();
        Set<ArenaMapCatalog.Point> visited = new HashSet<>();
        List<ArenaMapCatalog.Point> candidates = new ArrayList<>();
        queue.add(anchor);
        visited.add(anchor);
        while (!queue.isEmpty()) {
            ArenaMapCatalog.Point current = queue.removeFirst();
            candidates.add(current);
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue;
                    ArenaMapCatalog.Point next = new ArenaMapCatalog.Point(
                            current.x() + dx, current.y() + dy);
                    if (Math.max(Math.abs(next.x() - anchor.x()),
                            Math.abs(next.y() - anchor.y())) > 6
                            || visited.contains(next)
                            || !arena.nativeCanStep(current.x(), current.y(),
                                    next.x(), next.y())) continue;
                    visited.add(next);
                    queue.addLast(next);
                }
            }
        }
        if (candidates.size() < count) {
            throw new IllegalStateException("Quest local floor envelope is too small: "
                    + arena.mapId() + " room=" + roomNumber
                    + " candidates=" + candidates.size() + " required=" + count);
        }

        // Farthest-point sampling spreads actors without inventing a random
        // original-server placement.  Coordinate ordering seals every tie.
        List<ArenaMapCatalog.Point> selected = new ArrayList<>(count);
        selected.add(anchor);
        while (selected.size() < count) {
            ArenaMapCatalog.Point best = null;
            int bestDistance = -1;
            for (ArenaMapCatalog.Point candidate : candidates) {
                if (selected.contains(candidate)) continue;
                int minimum = Integer.MAX_VALUE;
                for (ArenaMapCatalog.Point used : selected) {
                    int dx = candidate.x() - used.x();
                    int dy = candidate.y() - used.y();
                    minimum = Math.min(minimum, dx * dx + dy * dy);
                }
                if (minimum > bestDistance
                        || minimum == bestDistance && (best == null
                        || candidate.y() < best.y()
                        || candidate.y() == best.y()
                        && candidate.x() < best.x())) {
                    best = candidate;
                    bestDistance = minimum;
                }
            }
            if (best == null) {
                throw new IllegalStateException("Quest placement selection exhausted");
            }
            selected.add(best);
        }
        return List.copyOf(selected);
    }

    private Monster monster(long objectId) {
        for (Lane lane : lanes) {
            for (Monster monster : lane.monsters) {
                if (monster.objectId == objectId && monster.alive()) return monster;
            }
        }
        return null;
    }

    private static Template template(int npcId, int gfx, String name,
                                     int level, int maxHp,
                                     int maximumMp, int intelligence) {
        BattleAnimationCatalog.get().requireBattleCapableSprite(
                gfx, BattleActionCodes.WALK);
        return new Template(npcId, gfx, name, level, maxHp,
                maximumMp, intelligence);
    }

    synchronized Snapshot nativeSourceEscapeMoveForAi(long l, int n, int n2, long l2) {
        Monster monster = this.monster(l);
        if (!this.active || monster == null || !NativeSourceNpcMovement.allowed(monster.ready(l2), monster) || NativeSourceNpcEscape.blind(monster.blindActive(l2), monster) || monster.sleepActive(l2) || monster.paralysisActive(l2) || monster.shockStunActive(l2) || monster.boneBreakActive(l2) || monster.thunderGrabActive(l2) || monster.freezeActive(l2) || monster.earthBindActive(l2)) {
            return null;
        }
        ArenaMapCatalog.Arena arena = this.arenas[monster.force];
        if (arena == null || !arena.nativeCanStep(monster.x, monster.y, n, n2) || BattleQuestBoundary.blocksMap(arena.mapId(), monster.roomNumber, monster.x, monster.y, n, n2)) {
            return null;
        }
        Lane lane = this.lanes[monster.force];
        boolean bl = lane.monsters.stream().anyMatch(monster2 -> monster2 != monster && monster2.alive() && monster2.x == n && monster2.y == n2);
        if (bl) {
            return null;
        }
        monster.x = NativeSourceNpcMotion.moving(n, monster, n2);
        monster.y = n2;
        monster.schedule(l2, 0);
        return monster.snapshot();
    }

    synchronized Snapshot nativeSourceViewActionMoveForAi(long l, int n, int n2, long l2, Object object) {
        if (!NativeSourceNpcViewAction.allowed(this, l, object)) {
            return null;
        }
        Monster monster = this.monster(l);
        if (!this.active || monster == null || !NativeSourceNpcMovement.allowed(monster.ready(l2), monster) || monster.blindActive(l2) || monster.sleepActive(l2) || monster.paralysisActive(l2) || monster.shockStunActive(l2) || monster.boneBreakActive(l2) || monster.thunderGrabActive(l2) || monster.freezeActive(l2) || monster.earthBindActive(l2)) {
            return null;
        }
        ArenaMapCatalog.Arena arena = this.arenas[monster.force];
        if (arena == null || !arena.nativeCanStep(monster.x, monster.y, n, n2) || BattleQuestBoundary.blocksMap(arena.mapId(), monster.roomNumber, monster.x, monster.y, n, n2)) {
            return null;
        }
        Lane lane = this.lanes[monster.force];
        boolean bl = lane.monsters.stream().anyMatch(monster2 -> monster2 != monster && monster2.alive() && monster2.x == n && monster2.y == n2);
        if (bl) {
            return null;
        }
        monster.x = NativeSourceNpcMotion.moving(n, monster, n2);
        monster.y = n2;
        monster.schedule(l2, 0);
        return monster.snapshot();
    }

    synchronized AiAttack nativeSourceViewActionAiAttackFor(long l, int n, int n2, long l2, Object object) {
        if (!NativeSourceNpcViewAction.allowed(this, l, object)) {
            return null;
        }
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        Monster monster = this.monster(l);
        if (monster == null) {
            return null;
        }
        List<BattleTimeAttackMagicSkillCatalog.MobskillRow> list = BattleTimeAttackMagicSkillCatalog.rowsForNpc(monster.template.npcId());
        int[] nArray = new int[list.size()];
        for (int i = 0; i < nArray.length; ++i) {
            nArray[i] = threadLocalRandom.nextInt(1, 101);
        }
        applyOrdinaryAttackCoin(monster, n, n2, nArray, threadLocalRandom);
        BattleTimeAttackMagicSkillCatalog.SummonDefinition summonDefinition = BattleTimeAttackMagicSkillCatalog.summonForNpc(monster.template.npcId());
        int n3 = summonDefinition == null ? 0 : threadLocalRandom.nextInt(summonDefinition.summonRandomBound());
        return this.aiAttackFor(l, n, n2, l2, nArray, threadLocalRandom.nextInt(Integer.MAX_VALUE), n3);
    }

    synchronized AiAttack nativeSourceViewActionAiAttackForActNoFixture(long l, int n, int n2, long l2, int n3, int n4, Object object) {
        if (!NativeSourceNpcViewAction.allowed(this, l, object)) {
            return null;
        }
        if (n3 < 0 || n4 < 0) {
            throw new IllegalArgumentException("invalid Quest actNo fixture");
        }
        Monster monster = this.monster(l);
        List list = monster == null ? List.of() : BattleTimeAttackMagicSkillCatalog.rowsForNpc(monster.template.npcId());
        int[] nArray = new int[list.size()];
        Arrays.fill(nArray, 100);
        int n5 = -1;
        for (int i = 0; i < list.size(); ++i) {
            if (((BattleTimeAttackMagicSkillCatalog.MobskillRow)list.get(i)).actNo() == n3) {
                n5 = i;
                nArray[i] = 1;
                break;
            }
        }
        if (n5 < 0) {
            return null;
        }
        BattleTimeAttackMagicSkillCatalog.SummonDefinition summonDefinition = BattleTimeAttackMagicSkillCatalog.summonForNpc(monster.template.npcId());
        int n6 = summonDefinition == null ? 0 : n4 % summonDefinition.summonRandomBound();
        return this.aiAttackFor(l, n, n2, l2, nArray, n4, n6);
    }

    synchronized Snapshot nativeSourceNpcPlayerFirstMoveForAi(long l, int n, int n2, long l2, Object object, Object object2) {
        if (!NativeSourceNpcPlayerView.current(object2)) {
            return null;
        }
        if (!NativeSourceNpcViewAction.allowed(this, l, object)) {
            return null;
        }
        Monster monster = this.monster(l);
        if (!this.active || monster == null || !NativeSourceNpcMovement.allowed(monster.ready(l2), monster) || monster.blindActive(l2) || monster.sleepActive(l2) || monster.paralysisActive(l2) || monster.shockStunActive(l2) || monster.boneBreakActive(l2) || monster.thunderGrabActive(l2) || monster.freezeActive(l2) || monster.earthBindActive(l2)) {
            return null;
        }
        ArenaMapCatalog.Arena arena = this.arenas[monster.force];
        if (arena == null || !arena.nativeCanStep(monster.x, monster.y, n, n2) || BattleQuestBoundary.blocksMap(arena.mapId(), monster.roomNumber, monster.x, monster.y, n, n2)) {
            return null;
        }
        Lane lane = this.lanes[monster.force];
        boolean bl = lane.monsters.stream().anyMatch(monster2 -> monster2 != monster && monster2.alive() && monster2.x == n && monster2.y == n2);
        if (bl) {
            return null;
        }
        monster.x = NativeSourceNpcMotion.moving(n, monster, n2);
        monster.y = n2;
        monster.schedule(l2, 0);
        return monster.snapshot();
    }

    synchronized AiAttack nativeSourceNpcPlayerFirstAiAttackFor(long l, int n, int n2, long l2, Object object, Object object2) {
        if (!NativeSourceNpcPlayerView.current(object2)) {
            return null;
        }
        if (!NativeSourceNpcViewAction.allowed(this, l, object)) {
            return null;
        }
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        Monster monster = this.monster(l);
        if (monster == null) {
            return null;
        }
        List<BattleTimeAttackMagicSkillCatalog.MobskillRow> list = BattleTimeAttackMagicSkillCatalog.rowsForNpc(monster.template.npcId());
        int[] nArray = new int[list.size()];
        for (int i = 0; i < nArray.length; ++i) {
            nArray[i] = threadLocalRandom.nextInt(1, 101);
        }
        applyOrdinaryAttackCoin(monster, n, n2, nArray, threadLocalRandom);
        BattleTimeAttackMagicSkillCatalog.SummonDefinition summonDefinition = BattleTimeAttackMagicSkillCatalog.summonForNpc(monster.template.npcId());
        int n3 = summonDefinition == null ? 0 : threadLocalRandom.nextInt(summonDefinition.summonRandomBound());
        return this.aiAttackFor(l, n, n2, l2, nArray, threadLocalRandom.nextInt(Integer.MAX_VALUE), n3);
    }

    synchronized AiAttack nativeSourceNpcPlayerFirstAiAttackForActNoFixture(long l, int n, int n2, long l2, int n3, int n4, Object object, Object object2) {
        if (!NativeSourceNpcPlayerView.current(object2)) {
            return null;
        }
        if (!NativeSourceNpcViewAction.allowed(this, l, object)) {
            return null;
        }
        if (n3 < 0 || n4 < 0) {
            throw new IllegalArgumentException("invalid Quest actNo fixture");
        }
        Monster monster = this.monster(l);
        List list = monster == null ? List.of() : BattleTimeAttackMagicSkillCatalog.rowsForNpc(monster.template.npcId());
        int[] nArray = new int[list.size()];
        Arrays.fill(nArray, 100);
        int n5 = -1;
        for (int i = 0; i < list.size(); ++i) {
            if (((BattleTimeAttackMagicSkillCatalog.MobskillRow)list.get(i)).actNo() == n3) {
                n5 = i;
                nArray[i] = 1;
                break;
            }
        }
        if (n5 < 0) {
            return null;
        }
        BattleTimeAttackMagicSkillCatalog.SummonDefinition summonDefinition = BattleTimeAttackMagicSkillCatalog.summonForNpc(monster.template.npcId());
        int n6 = summonDefinition == null ? 0 : n4 % summonDefinition.summonRandomBound();
        return this.aiAttackFor(l, n, n2, l2, nArray, n4, n6);
    }

    private static final class Lane {
        final int force;
        final List<Monster> monsters = new ArrayList<>();
        int roomIndex;
        boolean awaitingObjective;
        boolean completed;

        Lane(int force) {
            this.force = force;
        }
    }

    @AsmFinal({"nativeSourceHpViewers", "nativeSourceNpcFactoryType"})
    private static final class Monster {
        final long objectId;
        final int force;
        final int roomNumber;
        final Template template;
        volatile int x;
        volatile int y;
        volatile int hp;
        int mp;
        int maxMp;
        final Map<Integer, Long> skillReadyNanos = new HashMap<Integer, Long>();
        final Map<Integer, Integer> skillUseCounts = new HashMap<Integer, Integer>();
        final boolean summoned;
        final String placementSource;
        long nextActionNanos;
        long blindExpiresAtNanos;
        long weaknessExpiresAtNanos;
        long diseaseExpiresAtNanos;
        long silenceExpiresAtNanos;
        long areaSilenceExpiresAtNanos;
        long windShackleExpiresAtNanos;
        long decayPotionExpiresAtNanos;
        long polluteWaterExpiresAtNanos;
        long strikerGaleExpiresAtNanos;
        long sleepExpiresAtNanos;
        long paralysisExpiresAtNanos;
        volatile long shockStunExpiresAtNanos;
        volatile long boneBreakExpiresAtNanos;
        long thunderGrabExpiresAtNanos;
        volatile long freezeExpiresAtNanos;
        long earthBindExpiresAtNanos;
        volatile long hasteExpiresAtNanos;
        volatile long slowExpiresAtNanos;
        int weaponBreakStacks;
        final BattleElementalFallDownStatus elementalFallDownStatus = new BattleElementalFallDownStatus();
        final BattleEraseMagicStatus eraseMagicStatus = new BattleEraseMagicStatus();
        final BattleArmBreakerStatus armBreakerStatus = new BattleArmBreakerStatus();
        final BattlePanicStatus panicStatus = new BattlePanicStatus();
        final BattleJoyOfPainStatus joyOfPainStatus = new BattleJoyOfPainStatus();
        final BattleTimedStatus damagePoison = new BattleTimedStatus();
        RuntimeInventory inventory = new RuntimeInventory();
        long poisonAttackerObjectId;
        NativeSourceMonsterHp.Cache nativeSourceHpViewers;
        volatile Object nativeSourceArenaPoisonLease;
        volatile Object nativeSourceDollSlowEpoch;
        volatile Object nativeSourceTargetFreezeLease;
        volatile Object nativeSourceTargetShockLease;
        volatile Object nativeSourceSpecialBoneLease;
        Object nativeSourceNpcFactoryType;
        private volatile transient Object nativeSourceNpcMotion;

        Monster(int n, int n2, Template template, int n3, int n4) {
            this(n, n2, template, n3, n4, false);
        }

        Monster(int n, int n2, Template template, int n3, int n4, boolean bl) {
            this(n, n2, template, n3, n4, bl, bl ? BattleQuestEncounter.SUMMON_PLACEMENT_SOURCE : BattleQuestEncounter.PLACEMENT_SOURCE);
        }

        Monster(int n, int n2, Template template, int n3, int n4, boolean bl, String string) {
            this.objectId = NEXT_OBJECT_ID.getAndUpdate(l -> l >= 0x6CFFFFFFL ? 0x6C000000L : l + 1L);
            this.force = n;
            this.roomNumber = n2;
            this.template = template;
            this.x = n3;
            this.y = n4;
            this.maxMp = template.maximumMp();
            this.mp = this.maxMp;
            this.summoned = bl;
            this.placementSource = string;
            this.hp = template.maxHp();
            this.nativeSourceHpViewers = new NativeSourceMonsterHp.Cache();
            this.nativeSourceNpcFactoryType = NativeSourceNpcFactoryType.birth(this, null);
        }

        Monster(Snapshot snapshot) {
            this.objectId = snapshot.objectId();
            this.force = snapshot.force();
            this.roomNumber = snapshot.roomNumber();
            this.template = new Template(snapshot.npcId(), snapshot.gfx(), snapshot.name(), snapshot.level(), snapshot.maxHp(), snapshot.maximumMp(), snapshot.intelligence());
            this.x = snapshot.x();
            this.y = snapshot.y();
            this.hp = 0;
            this.maxMp = snapshot.maximumMp();
            this.mp = snapshot.currentMp();
            this.summoned = snapshot.npcId() == SPARTOI.npcId();
            this.placementSource = snapshot.placementSource();
            this.nativeSourceHpViewers = new NativeSourceMonsterHp.Cache();
            this.nativeSourceNpcFactoryType = NativeSourceNpcFactoryMemory.restore(this, null, snapshot);
        }

        Monster(Snapshot snapshot, BattleCompanion.Snapshot snapshot2, List<RuntimeInventory.Item> list) {
            this.objectId = NEXT_OBJECT_ID.getAndUpdate(l -> l >= 0x6CFFFFFFL ? 0x6C000000L : l + 1L);
            this.force = snapshot.force();
            this.roomNumber = snapshot.roomNumber();
            this.template = new Template(snapshot.npcId(), snapshot.gfx(), snapshot.name(), snapshot.level(), snapshot.maxHp(), snapshot.maximumMp(), snapshot.intelligence());
            this.x = snapshot2.x();
            this.y = snapshot2.y();
            this.hp = Math.max(1, Math.min(snapshot2.hp(), snapshot.maxHp()));
            this.maxMp = snapshot.maximumMp();
            this.mp = Math.max(0, Math.min(snapshot2.mp(), this.maxMp));
            this.summoned = snapshot.npcId() == SPARTOI.npcId();
            this.placementSource = snapshot.placementSource();
            for (RuntimeInventory.Item item : list) {
                if (!this.inventory.add(item, (long)item.quantity())) {
                    throw new IllegalStateException("released quest monster inventory exceeds native capacity");
                }
            }
            this.nativeSourceHpViewers = new NativeSourceMonsterHp.Cache();
            this.nativeSourceNpcFactoryType = NativeSourceNpcFactoryMemory.restore(this, snapshot2, snapshot);
        }

        boolean alive() {
            return hp > 0;
        }

        Snapshot snapshot() {
            Object object = NativeSourceNpcFirstView.capture(this);
            return new Snapshot(this.objectId, this.force, this.roomNumber, this.template.npcId(), this.x, this.y, this.template.gfx(), this.template.name(), this.template.level(), this.hp, this.template.maxHp(), this.hp > 0, this.mp, this.maxMp, this.template.intelligence(), this.summoned ? BattleQuestEncounter.DEATH_KNIGHT_SUMMON_SOURCE : BattleQuestEncounter.SPECIES_SOURCE, this.summoned ? "selected-mobskill-summon-count" : BattleQuestEncounter.COUNT_SOURCE, this.placementSource, NativeSourceNpcFactoryMemory.carry(this), object);
        }

        Damage damage(int amount) {
            return this.damage(amount, true);
        }

        Damage poisonDamage(int amount) {
            return damage(amount, false);
        }

        /**
         * 1-伺服器 SKM0（L1Character.isSkillEffectPhysicalDamageZero）：大地屏障與冰凍
         * （冰矛／冰雪颶風／冰凍氣息共用同一凍結租約）期間，物理與魔法傷害為 0。
         * 毒傷（L1DamagePoison.poisonTickCurrent 直接 receiveDamage）不經過這個判斷。
         */
        boolean damageZeroActive(long nowNanos) {
            return this.earthBindActive(nowNanos) || this.freezeActive(nowNanos);
        }

        private Damage damage(int amount, boolean earthBindBlocks) {
            if (this.hp <= 0 || amount <= 0 || NativeCombatAttacker.earth(earthBindBlocks, this) && this.damageZeroActive(System.nanoTime())) {
                return null;
            }
            Snapshot snapshot = this.snapshot();
            this.hp = Math.max(0, this.hp - amount);
            this.sleepExpiresAtNanos = 0L;
            if (this.hp == 0) {
                this.skillReadyNanos.clear();
                this.skillUseCounts.clear();
                this.paralysisExpiresAtNanos = 0L;
                NativeSourceTargetNpc.writeShock(this, 0L);
                NativeSourceSpecialNpc.writeBone(this, 0L);
                this.thunderGrabExpiresAtNanos = 0L;
                NativeSourceTargetNpc.writeFreeze(this, 0L);
                this.earthBindExpiresAtNanos = 0L;
                this.hasteExpiresAtNanos = 0L;
                this.slowExpiresAtNanos = 0L;
                this.weaponBreakStacks = 0;
                this.weaknessExpiresAtNanos = 0L;
                this.diseaseExpiresAtNanos = 0L;
                this.silenceExpiresAtNanos = 0L;
                this.areaSilenceExpiresAtNanos = 0L;
                this.windShackleExpiresAtNanos = 0L;
                this.decayPotionExpiresAtNanos = 0L;
                this.polluteWaterExpiresAtNanos = 0L;
                this.strikerGaleExpiresAtNanos = 0L;
                this.elementalFallDownStatus.clear();
                this.eraseMagicStatus.clear();
                this.armBreakerStatus.clear();
                this.panicStatus.clear();
                this.joyOfPainStatus.clear();
                this.curePoison();
            }
            Snapshot snapshot2 = this.snapshot();
            return new Damage(snapshot, snapshot2, snapshot.alive() && !snapshot2.alive());
        }

        boolean activatePoison(long attackerObjectId, long nowNanos,
                               int durationSeconds, int tickIntervalMillis) {
            if (!alive() || attackerObjectId <= 0) return false;
            boolean activated = damagePoison.activatePoison(
                    nowNanos, durationSeconds, tickIntervalMillis);
            if (activated) poisonAttackerObjectId = attackerObjectId;
            return activated;
        }

        boolean poisonActive(long nowNanos) {
            return alive() && damagePoison.poisonActive(nowNanos);
        }

        BattleTimedStatus.PoisonPoll pollPoison(
                long nowNanos, int tickIntervalMillis) {
            BattleTimedStatus.PoisonPoll poll = damagePoison.pollPoison(
                    nowNanos, tickIntervalMillis);
            if (poll.expired()) poisonAttackerObjectId = 0;
            return poll;
        }

        boolean curePoison() {
            boolean cured = damagePoison.curePoison();
            if (cured || poisonAttackerObjectId != 0) poisonAttackerObjectId = 0;
            return cured;
        }

        void resurrect(int restoredHp) {
            if (this.hp > 0 || restoredHp <= 0) {
                throw new IllegalStateException("invalid Quest resurrection");
            }
            Monster recipient = this;
            int n2 = Math.min(this.template.maxHp(), restoredHp);
            NativeCombatAttacker.advance(this);
            recipient.hp = n2;
            this.nextActionNanos = 0L;
            this.skillReadyNanos.clear();
            this.skillUseCounts.clear();
            this.blindExpiresAtNanos = 0L;
            this.weaknessExpiresAtNanos = 0L;
            this.diseaseExpiresAtNanos = 0L;
            this.silenceExpiresAtNanos = 0L;
            this.areaSilenceExpiresAtNanos = 0L;
            this.windShackleExpiresAtNanos = 0L;
            this.decayPotionExpiresAtNanos = 0L;
            this.polluteWaterExpiresAtNanos = 0L;
            this.strikerGaleExpiresAtNanos = 0L;
            this.elementalFallDownStatus.clear();
            this.eraseMagicStatus.clear();
            this.armBreakerStatus.clear();
            this.panicStatus.clear();
            this.joyOfPainStatus.clear();
            this.sleepExpiresAtNanos = 0L;
            this.paralysisExpiresAtNanos = 0L;
            NativeSourceTargetNpc.writeShock(this, 0L);
            NativeSourceSpecialNpc.writeBone(this, 0L);
            this.thunderGrabExpiresAtNanos = 0L;
            NativeSourceTargetNpc.writeFreeze(this, 0L);
            this.earthBindExpiresAtNanos = 0L;
            this.hasteExpiresAtNanos = 0L;
            this.slowExpiresAtNanos = 0L;
            this.weaponBreakStacks = 0;
        }

        int addWeaponBreakStack() {
            if (!alive()) return -1;
            weaponBreakStacks = BattleWeaponBreak.addStack(weaponBreakStacks);
            return weaponBreakStacks;
        }

        Healing heal(int amount) {
            if (hp <= 0 || amount < 0) return null;
            Snapshot before = snapshot();
            hp = Math.min(template.maxHp(), hp + amount);
            Snapshot after = snapshot();
            return new Healing(before, after, after.hp() - before.hp());
        }

        ManaDrain drainMana(int sourceMaximumMp, int requested) {
            if (!alive() || sourceMaximumMp < 0 || requested <= 0) return null;
            if (maxMp != sourceMaximumMp) {
                throw new IllegalStateException("Quest monster source maximum MP changed");
            }
            int before = mp;
            int drained = Math.min(before, requested);
            mp -= drained;
            return new ManaDrain(objectId, maxMp, before, mp, requested, drained);
        }

        int recoverMana(int amount) {
            if (!alive() || amount <= 0) return 0;
            int before = mp;
            mp = Math.min(maxMp, Math.addExact(mp, amount));
            return mp - before;
        }

        ManaDrain drainManaIfStrictlyAbove(
                int sourceMaximumMp, int requested) {
            if (!alive() || sourceMaximumMp < 0 || requested <= 0) return null;
            if (maxMp != sourceMaximumMp) {
                throw new IllegalStateException(
                        "Quest monster source maximum MP changed");
            }
            int before = mp;
            int drained = before > requested ? requested : 0;
            mp -= drained;
            return new ManaDrain(objectId, maxMp, before, mp, requested, drained);
        }

        boolean activateHaste(long nowNanos, int durationSeconds) {
            if (!alive() || nowNanos < 0 || durationSeconds <= 0) return false;
            if (slowActive(nowNanos)) {
                slowExpiresAtNanos = 0;
                return true;
            }
            slowExpiresAtNanos = 0;
            hasteExpiresAtNanos = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            return true;
        }

        boolean hasteActive(long nowNanos) {
            return nowNanos >= 0 && nowNanos < hasteExpiresAtNanos;
        }

        boolean expireHaste(long nowNanos) {
            if (hasteExpiresAtNanos == 0 || nowNanos < hasteExpiresAtNanos) {
                return false;
            }
            hasteExpiresAtNanos = 0;
            return true;
        }

        boolean activateSlow(long nowNanos, int durationSeconds) {
            if (!alive() || nowNanos < 0 || durationSeconds <= 0) return false;
            if (hasteActive(nowNanos)) {
                hasteExpiresAtNanos = 0;
                return true;
            }
            hasteExpiresAtNanos = 0;
            slowExpiresAtNanos = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            return true;
        }

        boolean slowActive(long nowNanos) {
            return nowNanos >= 0 && nowNanos < slowExpiresAtNanos;
        }

        boolean expireSlow(long nowNanos) {
            if (slowExpiresAtNanos == 0 || nowNanos < slowExpiresAtNanos) {
                return false;
            }
            slowExpiresAtNanos = 0;
            return true;
        }

        MagicCancellation cancelMagicEffects() {
            if (!this.alive()) {
                return null;
            }
            Snapshot snapshot = this.snapshot();
            boolean bl = this.hasteExpiresAtNanos != 0L;
            boolean bl2 = this.slowExpiresAtNanos != 0L;
            boolean bl3 = this.blindExpiresAtNanos != 0L;
            boolean bl4 = this.weaknessExpiresAtNanos != 0L;
            boolean bl5 = this.diseaseExpiresAtNanos != 0L;
            boolean bl6 = this.silenceExpiresAtNanos != 0L || this.areaSilenceExpiresAtNanos != 0L;
            boolean bl7 = this.decayPotionExpiresAtNanos != 0L;
            boolean bl8 = this.polluteWaterExpiresAtNanos != 0L;
            boolean bl9 = this.strikerGaleExpiresAtNanos != 0L;
            boolean bl10 = this.sleepExpiresAtNanos != 0L;
            boolean bl11 = this.paralysisExpiresAtNanos != 0L;
            boolean bl12 = this.freezeExpiresAtNanos != 0L;
            boolean bl13 = this.earthBindExpiresAtNanos != 0L;
            boolean bl14 = this.thunderGrabExpiresAtNanos != 0L;
            boolean bl15 = this.damagePoison.cancelHorrorOfDeath();
            boolean bl16 = this.boneBreakExpiresAtNanos != 0L;
            boolean bl17 = this.windShackleExpiresAtNanos != 0L;
            BattleElementalFallDownStatus.Cancellation cancellation = this.elementalFallDownStatus.cancel();
            boolean bl18 = this.eraseMagicStatus.cancel();
            this.armBreakerStatus.cancel();
            boolean bl19 = this.panicStatus.cancel();
            this.joyOfPainStatus.cancel();
            this.hasteExpiresAtNanos = 0L;
            this.slowExpiresAtNanos = 0L;
            this.blindExpiresAtNanos = 0L;
            this.weaknessExpiresAtNanos = 0L;
            this.diseaseExpiresAtNanos = 0L;
            this.silenceExpiresAtNanos = 0L;
            this.areaSilenceExpiresAtNanos = 0L;
            this.decayPotionExpiresAtNanos = 0L;
            this.polluteWaterExpiresAtNanos = 0L;
            this.strikerGaleExpiresAtNanos = 0L;
            this.sleepExpiresAtNanos = 0L;
            this.paralysisExpiresAtNanos = 0L;
            NativeSourceTargetNpc.writeFreeze(this, 0L);
            this.earthBindExpiresAtNanos = 0L;
            this.thunderGrabExpiresAtNanos = 0L;
            NativeSourceSpecialNpc.writeBone(this, 0L);
            this.windShackleExpiresAtNanos = 0L;
            return new MagicCancellation(snapshot, bl, bl2, bl3, bl4, bl5, bl6, bl7, bl8, bl9, bl10, bl11, bl12, bl13, bl14, bl15, bl16, bl17, cancellation, bl18, bl19);
        }

        boolean activateBlind(long nowNanos, int durationSeconds) {
            if (!alive() || nowNanos < 0 || durationSeconds <= 0) return false;
            long expiresAt = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            blindExpiresAtNanos = Math.max(blindExpiresAtNanos, expiresAt);
            return true;
        }

        boolean blindActive(long nowNanos) {
            return nowNanos >= 0 && nowNanos < blindExpiresAtNanos;
        }

        boolean activateWeakness(long nowNanos, int durationSeconds) {
            if (!alive() || nowNanos < 0 || durationSeconds <= 0) return false;
            weaknessExpiresAtNanos = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            return true;
        }

        boolean weaknessActive(long nowNanos) {
            return nowNanos >= 0 && nowNanos < weaknessExpiresAtNanos;
        }

        boolean activateDisease(long nowNanos, int durationSeconds) {
            if (!alive() || nowNanos < 0 || durationSeconds <= 0) return false;
            diseaseExpiresAtNanos = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            return true;
        }

        boolean diseaseActive(long nowNanos) {
            return nowNanos >= 0 && nowNanos < diseaseExpiresAtNanos;
        }

        boolean activateSilence(long nowNanos, int durationSeconds) {
            if (!alive() || nowNanos < 0 || durationSeconds <= 0) return false;
            silenceExpiresAtNanos = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            return true;
        }

        boolean activateSilenceIfAbsent(long nowNanos, int durationSeconds) {
            if (!alive() || nowNanos < 0 || durationSeconds <= 0) return false;
            if (nowNanos < silenceExpiresAtNanos) return false;
            silenceExpiresAtNanos = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            return true;
        }

        boolean silenceActive(long nowNanos) {
            return nowNanos >= 0 && (nowNanos < silenceExpiresAtNanos
                    || nowNanos < areaSilenceExpiresAtNanos);
        }

        boolean activateAreaSilence(long nowNanos, int durationSeconds) {
            if (!alive() || nowNanos < 0 || durationSeconds <= 0) return false;
            areaSilenceExpiresAtNanos = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            return true;
        }

        boolean activateWindShackle(long nowNanos, int durationSeconds) {
            if (!alive() || nowNanos < 0 || durationSeconds <= 0) return false;
            if (windShackleActive(nowNanos)) return false;
            windShackleExpiresAtNanos = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            return true;
        }

        boolean windShackleActive(long nowNanos) {
            if (windShackleExpiresAtNanos != 0
                    && nowNanos >= windShackleExpiresAtNanos) {
                windShackleExpiresAtNanos = 0;
            }
            return nowNanos >= 0 && nowNanos < windShackleExpiresAtNanos;
        }

        boolean activateDecayPotion(long nowNanos, int durationSeconds) {
            if (!alive() || nowNanos < 0 || durationSeconds <= 0) return false;
            if (decayPotionActive(nowNanos)) return true;
            decayPotionExpiresAtNanos = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            return true;
        }

        boolean decayPotionActive(long nowNanos) {
            if (decayPotionExpiresAtNanos != 0
                    && nowNanos >= decayPotionExpiresAtNanos) {
                decayPotionExpiresAtNanos = 0;
            }
            return nowNanos >= 0 && nowNanos < decayPotionExpiresAtNanos;
        }

        boolean activatePolluteWater(long nowNanos, int durationSeconds) {
            if (!alive() || nowNanos < 0 || durationSeconds <= 0) return false;
            polluteWaterExpiresAtNanos = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            return true;
        }

        boolean polluteWaterActive(long nowNanos) {
            if (polluteWaterExpiresAtNanos != 0
                    && nowNanos >= polluteWaterExpiresAtNanos) {
                polluteWaterExpiresAtNanos = 0;
            }
            return nowNanos >= 0 && nowNanos < polluteWaterExpiresAtNanos;
        }

        boolean activateStrikerGale(long nowNanos, int durationSeconds) {
            if (!alive() || nowNanos < 0 || durationSeconds <= 0) return false;
            strikerGaleExpiresAtNanos = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            return true;
        }

        boolean strikerGaleActive(long nowNanos) {
            if (strikerGaleExpiresAtNanos != 0
                    && nowNanos >= strikerGaleExpiresAtNanos) {
                strikerGaleExpiresAtNanos = 0;
            }
            return nowNanos >= 0 && nowNanos < strikerGaleExpiresAtNanos;
        }

        boolean activateElementalFallDown(
                long nowNanos, int durationSeconds, int sourceElfAttribute) {
            if (!alive() || nowNanos < 0 || durationSeconds <= 0) return false;
            elementalFallDownStatus.activate(
                    nowNanos, durationSeconds, sourceElfAttribute);
            return true;
        }

        boolean activateEraseMagic(long nowNanos, int durationSeconds) {
            if (!alive() || nowNanos < 0 || durationSeconds <= 0) return false;
            eraseMagicStatus.activate(nowNanos, durationSeconds);
            return true;
        }

        boolean consumeSilenceForSpell(long nowNanos) {
            if (areaSilenceExpiresAtNanos != 0) {
                if (nowNanos < areaSilenceExpiresAtNanos) {
                    return true;
                }
                areaSilenceExpiresAtNanos = 0;
            }
            if (silenceExpiresAtNanos == 0
                    || nowNanos >= silenceExpiresAtNanos) {
                silenceExpiresAtNanos = 0;
                return false;
            }
            silenceExpiresAtNanos = 0;
            return true;
        }

        boolean activateSleep(long nowNanos, int durationSeconds) {
            if (!alive() || nowNanos < 0 || durationSeconds <= 0) return false;
            sleepExpiresAtNanos = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            return true;
        }

        boolean sleepActive(long nowNanos) {
            return nowNanos >= 0 && nowNanos < sleepExpiresAtNanos;
        }

        boolean cancelSleep() {
            if (sleepExpiresAtNanos == 0) return false;
            sleepExpiresAtNanos = 0;
            return true;
        }

        boolean activateParalysis(long nowNanos, int durationMillis) {
            if (!alive() || nowNanos < 0 || durationMillis <= 0
                    || paralysisActive(nowNanos)) return false;
            paralysisExpiresAtNanos = Math.addExact(nowNanos,
                    TimeUnit.MILLISECONDS.toNanos(durationMillis));
            return true;
        }

        boolean paralysisActive(long nowNanos) {
            return nowNanos >= 0 && nowNanos < paralysisExpiresAtNanos;
        }

        boolean activateShockStun(long nowNanos, int durationSeconds) {
            if (!this.alive() || nowNanos < 0L || durationSeconds <= 0) {
                return false;
            }
            NativeSourceTargetNpc.writeShock(this, Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds)));
            return true;
        }

        boolean shockStunActive(long nowNanos) {
            return nowNanos >= 0 && nowNanos < shockStunExpiresAtNanos;
        }

        boolean activateBoneBreak(long nowNanos, int durationSeconds) {
            if (!this.alive() || nowNanos < 0L || durationSeconds <= 0 || this.boneBreakActive(nowNanos)) {
                return false;
            }
            NativeSourceSpecialNpc.writeBone(this, Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds)));
            return true;
        }

        boolean boneBreakActive(long nowNanos) {
            return nowNanos >= 0 && nowNanos < boneBreakExpiresAtNanos;
        }

        boolean activateThunderGrab(long nowNanos, int durationSeconds) {
            if (!alive() || nowNanos < 0 || durationSeconds <= 0
                    || thunderGrabActive(nowNanos)) return false;
            thunderGrabExpiresAtNanos = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            return true;
        }

        boolean thunderGrabActive(long nowNanos) {
            return nowNanos >= 0 && nowNanos < thunderGrabExpiresAtNanos;
        }

        boolean activateHorrorOfDeath(long nowNanos, int durationSeconds) {
            return alive() && damagePoison.activateHorrorOfDeath(
                    nowNanos, durationSeconds);
        }

        boolean horrorOfDeathActive(long nowNanos) {
            return alive() && damagePoison.horrorOfDeathActive(nowNanos);
        }

        boolean expireParalysis(long nowNanos) {
            if (paralysisExpiresAtNanos == 0 || nowNanos < paralysisExpiresAtNanos) {
                return false;
            }
            paralysisExpiresAtNanos = 0;
            return true;
        }

        boolean cancelParalysis() {
            if (paralysisExpiresAtNanos == 0) return false;
            paralysisExpiresAtNanos = 0;
            return true;
        }

        boolean activateFreeze(long nowNanos, int durationSeconds) {
            if (!this.alive() || nowNanos < 0L || durationSeconds <= 0 || this.freezeActive(nowNanos)) {
                return false;
            }
            NativeSourceTargetNpc.writeFreeze(this, Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds)));
            return true;
        }

        boolean freezeActive(long nowNanos) {
            return nowNanos >= 0 && nowNanos < freezeExpiresAtNanos;
        }

        boolean expireFreeze(long nowNanos) {
            if (this.freezeExpiresAtNanos == 0L || nowNanos < this.freezeExpiresAtNanos) {
                return false;
            }
            NativeSourceTargetNpc.writeFreeze(this, 0L);
            return true;
        }

        boolean cancelFreeze() {
            if (this.freezeExpiresAtNanos == 0L) {
                return false;
            }
            NativeSourceTargetNpc.writeFreeze(this, 0L);
            return true;
        }

        boolean activateEarthBind(long nowNanos, int durationSeconds) {
            if (!alive() || nowNanos < 0 || durationSeconds <= 0
                    || earthBindActive(nowNanos)) return false;
            earthBindExpiresAtNanos = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            return true;
        }

        boolean earthBindActive(long nowNanos) {
            return nowNanos >= 0 && nowNanos < earthBindExpiresAtNanos;
        }

        boolean expireEarthBind(long nowNanos) {
            if (earthBindExpiresAtNanos == 0 || nowNanos < earthBindExpiresAtNanos) {
                return false;
            }
            earthBindExpiresAtNanos = 0;
            return true;
        }

        boolean ready(long nowNanos) {
            return nowNanos >= 0 && nowNanos >= nextActionNanos;
        }

        void schedule(long nowNanos, int action) {
            long l2 = TimeUnit.MILLISECONDS.toNanos(NativeSourceNpcBaseSpeeds.npcMillis(this.template.gfx(), action, this.hasteActive(nowNanos), this.slowActive(nowNanos), this.windShackleActive(nowNanos), this));
            this.nextActionNanos = Math.addExact(nowNanos, l2);
        }

        boolean magicReady(BattleTimeAttackMagicSkillCatalog.Definition skill,
                           long nowNanos) {
            return skill != null && skill.npcId() == template.npcId()
                    && mp >= skill.mpCost()
                    && nowNanos >= skillReadyNanos.getOrDefault(BattleTimeAttackMagicSkillCatalog.CASTER_SKILL_DELAY_KEY, 0L);
        }

        void scheduleMagic(long nowNanos, BattleTimeAttackMagicSkillCatalog.Definition skill) {
            if (!this.magicReady(skill, nowNanos)) {
                throw new IllegalStateException("Quest magic was not ready");
            }
            this.mp -= skill.mpCost();
            int n = NativeSourceNpcSpellTiming.base(skill.actionDelayMillis(), this, skill);
            if (this.slowActive(nowNanos)) {
                n = (int)((double)n * 2.0);
            } else if (this.hasteActive(nowNanos)) {
                n = (int)((double)n * 0.755);
            }
            boolean windShackle = this.windShackleActive(nowNanos);
            n = NativeSourceNpcActionIntervals.brave(n, this);
            if (windShackle) {
                n = (int)((double)n * 1.25);
            }
            this.nextActionNanos = Math.addExact(nowNanos, TimeUnit.MILLISECONDS.toNanos(Math.max(1, n)));
            this.skillReadyNanos.put(BattleTimeAttackMagicSkillCatalog.CASTER_SKILL_DELAY_KEY, Math.addExact(nowNanos, TimeUnit.MILLISECONDS.toNanos(skill.reuseDelayMillis())));
        }

        boolean summonReady(
                BattleTimeAttackMagicSkillCatalog.SummonDefinition summon) {
            return summon != null && summon.npcId() == template.npcId()
                    && skillUseCounts.getOrDefault(summon.actNo(), 0)
                        < summon.triggerCount();
        }

        void scheduleSummon(long nowNanos, BattleTimeAttackMagicSkillCatalog.SummonDefinition summon) {
            if (!this.summonReady(summon)) {
                throw new IllegalStateException("Quest summon was not ready");
            }
            int n = NativeSourceNpcSpellTiming.base(summon.actionDelayMillis(), this, summon);
            if (this.slowActive(nowNanos)) {
                n = (int)((double)n * 2.0);
            } else if (this.hasteActive(nowNanos)) {
                n = (int)((double)n * 0.755);
            }
            boolean windShackle = this.windShackleActive(nowNanos);
            n = NativeSourceNpcActionIntervals.brave(n, this);
            if (windShackle) {
                n = (int)((double)n * 1.25);
            }
            this.nextActionNanos = Math.addExact(nowNanos, TimeUnit.MILLISECONDS.toNanos(Math.max(1, n)));
            this.skillUseCounts.merge(summon.actNo(), 1, Integer::sum);
        }

        int nativeSourceCubeRemainderRecoverMana(int n) {
            if (!this.alive() || n <= 0) {
                return 0;
            }
            int n2 = this.mp;
            this.mp = Math.min(this.maxMp, NativeSourceCubeRemainder.cappedAdd(this.mp, n));
            return this.mp - n2;
        }

        Healing nativeSourceNpcMagicHeal(int n) {
            if (this.hp <= 0 || n < 0) {
                return null;
            }
            Snapshot snapshot = this.snapshot();
            this.hp = NativeSourceNpcMagicEffects.capped(this.template.maxHp(), this.hp, n);
            Snapshot snapshot2 = this.snapshot();
            return new Healing(snapshot, snapshot2, snapshot2.hp() - snapshot.hp());
        }
    }
}
