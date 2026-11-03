package local.lito381.server;

import java.util.ArrayList;
import java.util.ArrayDeque;
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

    record Snapshot(long objectId, int force, int roomNumber, int npcId,
                    int x, int y, int gfx, String name, int level,
                    int hp, int maxHp, boolean alive, int currentMp,
                    int maximumMp, int intelligence,
                    String speciesSource, String countSource,
                    String placementSource) {}

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

    record PoisonUpdate(long attackerObjectId, Snapshot target,
                        List<Hit> hits, boolean expired) {
        PoisonUpdate {
            if (attackerObjectId <= 0 || target == null) {
                throw new IllegalArgumentException("invalid Quest poison update");
            }
            hits = List.copyOf(hits);
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
        active = false;
        perRoomSymbols = false;
        configuredWaves = List.of();
        arenas[0] = null;
        arenas[1] = null;
        for (Lane lane : lanes) {
            lane.roomIndex = 0;
            lane.awaitingObjective = false;
            lane.completed = false;
            lane.monsters.clear();
        }
        corpses.clear();
    }

    synchronized List<Snapshot> snapshots() {
        if (!active) return List.of();
        List<Snapshot> result = new ArrayList<>();
        for (Lane lane : lanes) {
            for (Monster monster : lane.monsters) {
                Snapshot snapshot = monster.snapshot();
                if (snapshot != null && snapshot.alive()) result.add(snapshot);
            }
        }
        return List.copyOf(result);
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
                updates.add(new PoisonUpdate(attackerObjectId, finalTarget,
                        hits, poll.expired()));
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
        return resurrectCorpse(objectId, force, nowNanos, 4);
    }

    synchronized Resurrection resurrectCorpse(
            long objectId, int force, long nowNanos, int hpDivisor) {
        Snapshot corpse = findCorpse(objectId, force, nowNanos);
        if ((hpDivisor != 1 && hpDivisor != 4) || corpse == null
                || !BattleSpellCatalog.npcAllowsResurrection(corpse.npcId())) {
            return null;
        }
        Monster target = null;
        for (Lane lane : lanes) {
            target = lane.monsters.stream()
                    .filter(monster -> monster.objectId == objectId && !monster.alive())
                    .findFirst().orElse(null);
            if (target != null) break;
        }
        if (target == null) {
            target = new Monster(corpse);
            lanes[force].monsters.add(target);
        }
        Snapshot before = target.snapshot();
        target.resurrect(Math.max(1, corpse.maxHp() / hpDivisor));
        corpses.removeIf(entry -> entry.snapshot().objectId() == objectId);
        return new Resurrection(before, target.snapshot());
    }

    private void rememberCorpse(Snapshot corpse, long nowNanos) {
        if (corpse == null || corpse.alive() || nowNanos < 0) return;
        corpses.removeIf(entry -> entry.snapshot().objectId() == corpse.objectId());
        corpses.add(new Corpse(corpse, Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(CORPSE_LIFETIME_SECONDS))));
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
        Monster monster = monster(objectId);
        if (!active || monster == null || !monster.ready(nowNanos)
                || monster.blindActive(nowNanos)
                || monster.sleepActive(nowNanos)
                || monster.paralysisActive(nowNanos)
                || monster.shockStunActive(nowNanos)
                || monster.boneBreakActive(nowNanos)
                || monster.thunderGrabActive(nowNanos)
                || monster.freezeActive(nowNanos)
                || monster.earthBindActive(nowNanos)) return null;
        ArenaMapCatalog.Arena laneArena = arenas[monster.force];
        if (laneArena == null || !laneArena.nativeCanStep(monster.x, monster.y, x, y)
                || BattleQuestBoundary.blocksMap(laneArena.mapId(), monster.roomNumber,
                        monster.x, monster.y, x, y)) {
            return null;
        }
        Lane lane = lanes[monster.force];
        boolean occupied = lane.monsters.stream().anyMatch(candidate ->
                candidate != monster && candidate.alive()
                        && candidate.x == x && candidate.y == y);
        if (occupied) return null;
        monster.x = x;
        monster.y = y;
        monster.schedule(nowNanos, BattleActionCodes.WALK);
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

    synchronized AiAttack aiAttackFor(long objectId, int targetX, int targetY,
                                      long nowNanos) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Monster monster = monster(objectId);
        if (monster == null) return null;
        List<BattleTimeAttackMagicSkillCatalog.MobskillRow> rows =
                BattleTimeAttackMagicSkillCatalog.rowsForNpc(
                        monster.template.npcId());
        int[] triggerRolls = new int[rows.size()];
        for (int index = 0; index < triggerRolls.length; index++) {
            triggerRolls[index] = random.nextInt(1, 101);
        }
        BattleTimeAttackMagicSkillCatalog.SummonDefinition summon =
                BattleTimeAttackMagicSkillCatalog.summonForNpc(
                        monster.template.npcId());
        int summonCountRoll = summon == null ? 0
                : random.nextInt(summon.summonRandomBound());
        return aiAttackFor(objectId, targetX, targetY, nowNanos,
                triggerRolls, random.nextInt(Integer.MAX_VALUE),
                summonCountRoll);
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

        int distance = Math.max(Math.abs(monster.x - targetX),
                Math.abs(monster.y - targetY));
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
                monster.schedule(nowNanos, physical.effectiveActionId());
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
            monster.schedule(nowNanos, BattleActionCodes.ATTACK);
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
        final int radius = 6;
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
                            Math.abs(next.y() - anchor.y())) > radius
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

    private static final class Monster {
        final long objectId;
        final int force;
        final int roomNumber;
        final Template template;
        int x;
        int y;
        int hp;
        int mp;
        int maxMp;
        final java.util.Map<Integer, Long> skillReadyNanos =
                new java.util.HashMap<>();
        final java.util.Map<Integer, Integer> skillUseCounts =
                new java.util.HashMap<>();
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
        long shockStunExpiresAtNanos;
        long boneBreakExpiresAtNanos;
        long thunderGrabExpiresAtNanos;
        long freezeExpiresAtNanos;
        long earthBindExpiresAtNanos;
        long hasteExpiresAtNanos;
        long slowExpiresAtNanos;
        int weaponBreakStacks;
        final BattleElementalFallDownStatus elementalFallDownStatus =
                new BattleElementalFallDownStatus();
        final BattleEraseMagicStatus eraseMagicStatus =
                new BattleEraseMagicStatus();
        final BattleArmBreakerStatus armBreakerStatus =
                new BattleArmBreakerStatus();
        final BattlePanicStatus panicStatus = new BattlePanicStatus();
        final BattleJoyOfPainStatus joyOfPainStatus =
                new BattleJoyOfPainStatus();
        final BattleTimedStatus damagePoison = new BattleTimedStatus();
        final RuntimeInventory inventory = new RuntimeInventory();
        long poisonAttackerObjectId;

        Monster(int force, int roomNumber, Template template, int x, int y) {
            this(force, roomNumber, template, x, y, false);
        }

        Monster(int force, int roomNumber, Template template, int x, int y,
                boolean summoned) {
            this(force, roomNumber, template, x, y, summoned,
                    summoned ? SUMMON_PLACEMENT_SOURCE : PLACEMENT_SOURCE);
        }

        Monster(int force, int roomNumber, Template template, int x, int y,
                boolean summoned, String placementSource) {
            this.objectId = NEXT_OBJECT_ID.getAndUpdate(current ->
                    current >= 0x6CFF_FFFFL ? FIRST_OBJECT_ID : current + 1);
            this.force = force;
            this.roomNumber = roomNumber;
            this.template = template;
            this.x = x;
            this.y = y;
            this.maxMp = template.maximumMp();
            this.mp = maxMp;
            this.summoned = summoned;
            this.placementSource = placementSource;
            hp = template.maxHp();
        }

        Monster(Snapshot corpse) {
            this.objectId = corpse.objectId();
            this.force = corpse.force();
            this.roomNumber = corpse.roomNumber();
            this.template = new Template(corpse.npcId(), corpse.gfx(), corpse.name(),
                    corpse.level(), corpse.maxHp(), corpse.maximumMp(),
                    corpse.intelligence());
            this.x = corpse.x();
            this.y = corpse.y();
            this.hp = 0;
            this.maxMp = corpse.maximumMp();
            this.mp = corpse.currentMp();
            this.summoned = corpse.npcId() == SPARTOI.npcId();
            this.placementSource = corpse.placementSource();
        }

        Monster(Snapshot captured, BattleCompanion.Snapshot companion,
                List<RuntimeInventory.Item> carriedItems) {
            this.objectId = NEXT_OBJECT_ID.getAndUpdate(current ->
                    current >= 0x6CFF_FFFFL ? FIRST_OBJECT_ID : current + 1);
            this.force = captured.force();
            this.roomNumber = captured.roomNumber();
            this.template = new Template(captured.npcId(), captured.gfx(),
                    captured.name(), captured.level(), captured.maxHp(),
                    captured.maximumMp(), captured.intelligence());
            this.x = companion.x();
            this.y = companion.y();
            this.hp = Math.max(1, Math.min(companion.hp(), captured.maxHp()));
            this.maxMp = captured.maximumMp();
            this.mp = Math.max(0, Math.min(companion.mp(), maxMp));
            this.summoned = captured.npcId() == SPARTOI.npcId();
            this.placementSource = captured.placementSource();
            for (RuntimeInventory.Item item : carriedItems) {
                if (!inventory.add(item, item.quantity())) {
                    throw new IllegalStateException(
                            "released quest monster inventory exceeds native capacity");
                }
            }
        }

        boolean alive() {
            return hp > 0;
        }

        Snapshot snapshot() {
            return new Snapshot(objectId, force, roomNumber, template.npcId(),
                    x, y, template.gfx(), template.name(), template.level(),
                    hp, template.maxHp(), hp > 0, mp, maxMp,
                    template.intelligence(),
                    summoned ? DEATH_KNIGHT_SUMMON_SOURCE : SPECIES_SOURCE,
                    summoned ? "selected-mobskill-summon-count"
                            : COUNT_SOURCE,
                    placementSource);
        }

        Damage damage(int amount) {
            return damage(amount, true);
        }

        Damage poisonDamage(int amount) {
            return damage(amount, false);
        }

        private Damage damage(int amount, boolean earthBindBlocks) {
            if (hp <= 0 || amount <= 0
                    || (earthBindBlocks && earthBindActive(System.nanoTime()))) return null;
            Snapshot before = snapshot();
            hp = Math.max(0, hp - amount);
            sleepExpiresAtNanos = 0;
            if (hp == 0) {
                skillReadyNanos.clear();
                skillUseCounts.clear();
                paralysisExpiresAtNanos = 0;
                shockStunExpiresAtNanos = 0;
                boneBreakExpiresAtNanos = 0;
                thunderGrabExpiresAtNanos = 0;
                freezeExpiresAtNanos = 0;
                earthBindExpiresAtNanos = 0;
                hasteExpiresAtNanos = 0;
                slowExpiresAtNanos = 0;
                weaponBreakStacks = 0;
                weaknessExpiresAtNanos = 0;
                diseaseExpiresAtNanos = 0;
                silenceExpiresAtNanos = 0;
                areaSilenceExpiresAtNanos = 0;
                windShackleExpiresAtNanos = 0;
                decayPotionExpiresAtNanos = 0;
                polluteWaterExpiresAtNanos = 0;
                strikerGaleExpiresAtNanos = 0;
                elementalFallDownStatus.clear();
                eraseMagicStatus.clear();
                armBreakerStatus.clear();
                panicStatus.clear();
                joyOfPainStatus.clear();
                curePoison();
            }
            Snapshot after = snapshot();
            return new Damage(before, after, before.alive() && !after.alive());
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
            if (hp > 0 || restoredHp <= 0) throw new IllegalStateException("invalid Quest resurrection");
            hp = Math.min(template.maxHp(), restoredHp);
            nextActionNanos = 0;
            skillReadyNanos.clear();
            skillUseCounts.clear();
            blindExpiresAtNanos = 0;
            weaknessExpiresAtNanos = 0;
            diseaseExpiresAtNanos = 0;
            silenceExpiresAtNanos = 0;
            areaSilenceExpiresAtNanos = 0;
            windShackleExpiresAtNanos = 0;
            decayPotionExpiresAtNanos = 0;
            polluteWaterExpiresAtNanos = 0;
            strikerGaleExpiresAtNanos = 0;
            elementalFallDownStatus.clear();
            eraseMagicStatus.clear();
            armBreakerStatus.clear();
            panicStatus.clear();
            joyOfPainStatus.clear();
            sleepExpiresAtNanos = 0;
            paralysisExpiresAtNanos = 0;
            shockStunExpiresAtNanos = 0;
            boneBreakExpiresAtNanos = 0;
            thunderGrabExpiresAtNanos = 0;
            freezeExpiresAtNanos = 0;
            earthBindExpiresAtNanos = 0;
            hasteExpiresAtNanos = 0;
            slowExpiresAtNanos = 0;
            weaponBreakStacks = 0;
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
            if (!alive()) return null;
            Snapshot target = snapshot();
            boolean haste = hasteExpiresAtNanos != 0;
            boolean slow = slowExpiresAtNanos != 0;
            boolean blind = blindExpiresAtNanos != 0;
            boolean weakness = weaknessExpiresAtNanos != 0;
            boolean disease = diseaseExpiresAtNanos != 0;
            boolean silence = silenceExpiresAtNanos != 0
                    || areaSilenceExpiresAtNanos != 0;
            boolean decayPotion = decayPotionExpiresAtNanos != 0;
            boolean polluteWater = polluteWaterExpiresAtNanos != 0;
            boolean strikerGale = strikerGaleExpiresAtNanos != 0;
            boolean sleep = sleepExpiresAtNanos != 0;
            boolean paralysis = paralysisExpiresAtNanos != 0;
            boolean freeze = freezeExpiresAtNanos != 0;
            boolean earthBind = earthBindExpiresAtNanos != 0;
            boolean thunderGrab = thunderGrabExpiresAtNanos != 0;
            boolean horrorOfDeath = damagePoison.cancelHorrorOfDeath();
            boolean boneBreak = boneBreakExpiresAtNanos != 0;
            boolean windShackle = windShackleExpiresAtNanos != 0;
            BattleElementalFallDownStatus.Cancellation elementalFallDown =
                    elementalFallDownStatus.cancel();
            boolean eraseMagic = eraseMagicStatus.cancel();
            armBreakerStatus.cancel();
            boolean panic = panicStatus.cancel();
            joyOfPainStatus.cancel();
            hasteExpiresAtNanos = 0;
            slowExpiresAtNanos = 0;
            blindExpiresAtNanos = 0;
            weaknessExpiresAtNanos = 0;
            diseaseExpiresAtNanos = 0;
            silenceExpiresAtNanos = 0;
            areaSilenceExpiresAtNanos = 0;
            decayPotionExpiresAtNanos = 0;
            polluteWaterExpiresAtNanos = 0;
            strikerGaleExpiresAtNanos = 0;
            sleepExpiresAtNanos = 0;
            paralysisExpiresAtNanos = 0;
            freezeExpiresAtNanos = 0;
            earthBindExpiresAtNanos = 0;
            thunderGrabExpiresAtNanos = 0;
            boneBreakExpiresAtNanos = 0;
            windShackleExpiresAtNanos = 0;
            return new MagicCancellation(target, haste, slow, blind, weakness, disease,
                    silence, decayPotion, polluteWater, strikerGale,
                    sleep, paralysis, freeze,
                    earthBind, thunderGrab, horrorOfDeath, boneBreak, windShackle,
                    elementalFallDown, eraseMagic, panic);
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
            if (!alive() || nowNanos < 0 || durationSeconds <= 0) return false;
            shockStunExpiresAtNanos = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            return true;
        }

        boolean shockStunActive(long nowNanos) {
            return nowNanos >= 0 && nowNanos < shockStunExpiresAtNanos;
        }

        boolean activateBoneBreak(long nowNanos, int durationSeconds) {
            if (!alive() || nowNanos < 0 || durationSeconds <= 0
                    || boneBreakActive(nowNanos)) return false;
            boneBreakExpiresAtNanos = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
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
            if (!alive() || nowNanos < 0 || durationSeconds <= 0
                    || freezeActive(nowNanos)) return false;
            freezeExpiresAtNanos = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            return true;
        }

        boolean freezeActive(long nowNanos) {
            return nowNanos >= 0 && nowNanos < freezeExpiresAtNanos;
        }

        boolean expireFreeze(long nowNanos) {
            if (freezeExpiresAtNanos == 0 || nowNanos < freezeExpiresAtNanos) {
                return false;
            }
            freezeExpiresAtNanos = 0;
            return true;
        }

        boolean cancelFreeze() {
            if (freezeExpiresAtNanos == 0) return false;
            freezeExpiresAtNanos = 0;
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
            long delay = TimeUnit.MILLISECONDS.toNanos(
                    BattleActionInterval.npcMillis(
                            template.gfx(), action, hasteActive(nowNanos),
                            slowActive(nowNanos), windShackleActive(nowNanos)));
            nextActionNanos = Math.addExact(nowNanos, delay);
        }

        boolean magicReady(BattleTimeAttackMagicSkillCatalog.Definition skill,
                           long nowNanos) {
            return skill != null && skill.npcId() == template.npcId()
                    && mp >= skill.mpCost()
                    && nowNanos >= skillReadyNanos.getOrDefault(skill.actNo(), 0L);
        }

        void scheduleMagic(long nowNanos,
                           BattleTimeAttackMagicSkillCatalog.Definition skill) {
            if (!magicReady(skill, nowNanos)) {
                throw new IllegalStateException("Quest magic was not ready");
            }
            mp -= skill.mpCost();
            int delay = skill.actionDelayMillis();
            if (slowActive(nowNanos)) {
                delay = (int) (delay * BattleActionInterval.SLOW_MULTIPLIER);
            } else if (hasteActive(nowNanos)) {
                delay = (int) (delay * BattleActionInterval.HASTE_MULTIPLIER);
            }
            if (windShackleActive(nowNanos)) {
                delay = (int) (delay
                        * BattleActionInterval.WIND_SHACKLE_MULTIPLIER);
            }
            nextActionNanos = Math.addExact(nowNanos,
                    TimeUnit.MILLISECONDS.toNanos(Math.max(1, delay)));
            skillReadyNanos.put(skill.actNo(), Math.addExact(nowNanos,
                    TimeUnit.MILLISECONDS.toNanos(skill.reuseDelayMillis())));
        }

        boolean summonReady(
                BattleTimeAttackMagicSkillCatalog.SummonDefinition summon) {
            return summon != null && summon.npcId() == template.npcId()
                    && skillUseCounts.getOrDefault(summon.actNo(), 0)
                        < summon.triggerCount();
        }

        void scheduleSummon(long nowNanos,
                            BattleTimeAttackMagicSkillCatalog.SummonDefinition summon) {
            if (!summonReady(summon)) {
                throw new IllegalStateException("Quest summon was not ready");
            }
            int delay = summon.actionDelayMillis();
            if (slowActive(nowNanos)) {
                delay = (int) (delay * BattleActionInterval.SLOW_MULTIPLIER);
            } else if (hasteActive(nowNanos)) {
                delay = (int) (delay * BattleActionInterval.HASTE_MULTIPLIER);
            }
            if (windShackleActive(nowNanos)) {
                delay = (int) (delay
                        * BattleActionInterval.WIND_SHACKLE_MULTIPLIER);
            }
            nextActionNanos = Math.addExact(nowNanos,
                    TimeUnit.MILLISECONDS.toNanos(Math.max(1, delay)));
            skillUseCounts.merge(summon.actNo(), 1, Integer::sum);
        }
    }
}
