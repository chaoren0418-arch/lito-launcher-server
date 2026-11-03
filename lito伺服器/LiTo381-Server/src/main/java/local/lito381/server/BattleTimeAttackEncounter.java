package local.lito381.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Authoritative cooperative Time Attack end-game encounter on native map 540.
 *
 * <p>Contemporary LiTo reports establish the server-owned sequence used here:
 * a participant destroys a guardian tower to start the encounter, all players
 * cooperate in one arena, ordinary monsters must be cleared under a time limit,
 * and only then does a final boss stage appear.  NC Japan's retained 2005
 * reports establish two distinct documented patterns: Fukuoka/Osaka begin with
 * Black Knight Chief and Fukuoka ends with Great Minotaur plus Ancient Giant;
 * Tokyo begins with Owlbear, later contains Sea Dancer, and ends with Death
 * Knight plus Zombie Lord.  Same-family 3.80 data pins their npc ids, gfx and
 * combat attributes, while the selected 3.81 sprite manifest corroborates their
 * required actions.  Separate 2005 Tokyo screenshots visibly establish an Ice
 * Golem-shaped ordinary stage at 06:13 and identify Sea Dancer at 02:46.
 * The selected client's decoded gfx 1525 frame matches that Ice Golem shape;
 * same-family data maps it to npc 45182/$1395/Lv18/220 HP and the packaged
 * animation table proves walk 0 plus melee 1.  The selected Sea Dancer row maps
 * to npc 45812/gfx 1603 and its selected mobskill casts skill 38 with native
 * actions 18/19, so the absence of melee action 1 does not make it passive.</p>
 *
 * <p>A retained contemporary Fukuoka report explicitly says that monsters
 * appear in a fixed order, while the selected pattern is unknown even to the
 * event organizer until the match starts.  This establishes server-owned
 * selection of one whole ordered pattern, but does not disclose the original
 * pattern count or selection weights.  The ordinary wave/pattern table has not
 * been recovered from the client or any retained server database.  The 2005 Tokyo sequence nevertheless
 * proves Owlbear first, then the relative Ice Golem-before-Sea-Dancer order and
 * that the boss remains gated behind an ordinary-monster count.  Production
 * randomly selects one of the two documented minimum patterns without joining
 * actors from different event patterns.  It does not claim either minimum list
 * is a complete pattern, their original counts, or the original selection
 * weights.  Gfx 1603 has no
 * ordinary action 1, so its AI is restricted to its exact selected ice-cone row.
 * Exact boss coordinates are likewise unavailable and use distinct S32-probed
 * safe anchors with an explicit provisional source.  A contemporary 2005
 * dragon-room guide additionally proves that a dragon is selected at random
 * after the boss-grade monsters, including a no-dragon outcome.  The guide
 * does not disclose the selection weights.  Production therefore exposes an
 * explicit provisional equal-weight none/Antharas/Fafurion/Valakas selector;
 * the three identities are corroborated by the selected sprite manifest and
 * immutable same-family NPC rows, but their event-specific tuning is not
 * claimed to be recovered.</p>
 */
final class BattleTimeAttackEncounter {
    /**
     * Later native dragon-room material permits at most sixty cooperative
     * Time Attack combatants.  This is independent of the selected Taiwan
     * client's common 2..100 room-capacity field.
     */
    static final int MAXIMUM_PLAYERS = 60;
    static final String BOSS_SET_SOURCE =
            "LiTo-2005-Fukuoka-report+same-family-380-npc";
    static final String ORDINARY_WAVE_SOURCE =
            "unit-mechanics-fixture-not-production-pattern";
    static final String PATTERN_SELECTION_MODEL_SOURCE =
            "4Gamer-Fukuoka-2005-fixed-order+organizer-unknown-until-start";
    static final String PATTERN_SELECTION_WEIGHTS_SOURCE = "unrecovered";
    static final String CLIENT_WAVE_TABLE_SOURCE =
            "selected-LiTo-sub_46F9C0-map-metadata-only+no-wave-table";
    static final String EXCLUDED_ADEN_ARENA_DM_SOURCE =
            "Lineage-Compendium-Aden-Arena-Deathmatch-not-LiTo-map540";
    static final String NCJAPAN_FUKUOKA_SOURCE =
            "NC-Japan-Battle-GP-2005-Fukuoka-retained-report";
    static final String NCJAPAN_TOKYO_SOURCE =
            "NC-Japan-Battle-GP-2005-Tokyo-retained-report";
    static final String FUKUOKA_PATTERN_SOURCE =
            "incomplete-Fukuoka-Black-Knight-Chief-to-Great-Minotaur+Ancient-Giant";
    static final String TOKYO_PATTERN_SOURCE =
            "incomplete-Tokyo-Owlbear-Ice-Golem-Sea-Dancer-to-Death-Knight+Zombie-Lord";
    static final String FUKUOKA_BLACK_KNIGHT_CHIEF_SOURCE =
            "NC-Japan-Fukuoka+Osaka-first-wave+same-family-380-npc";
    static final String TOKYO_OWLBEAR_SOURCE =
            "NC-Japan-Tokyo-first-wave+same-family-380-npc";
    static final String TOKYO_ICE_GOLEM_FRAGMENT_SOURCE =
            "GAME-Watch-2005-Tokyo-lin25+selected-client-gfx1525+same-family-380-npc";
    static final String TOKYO_ORDINARY_FRAGMENT_SOURCE =
            "GAME-Watch-2005-Tokyo-lin27+same-family-380-npc";
    static final String PLACEMENT_SOURCE =
            BattleTimeAttackSpawnCatalog.ENCOUNTER_PLACEMENT_SOURCE;
    static final String AI_SOURCE =
            "LTmap-Layer3-native-sub_48B640-pursuit+native-sprite-action-duration";
    static final String AI_DAMAGE_SOURCE = BattleTimeAttackPhysicalDamage.SOURCE;
    static final String DEATH_KNIGHT_SUMMON_SOURCE =
            "selected-380-mobskill45601-act2+npc45161+retained-L1MobSkillUse.summon";
    static final String DRAGON_STAGE_SOURCE =
            "GameMeca-LWC-2005-random-boss-then-random-dragon";
    static final String DRAGON_IDENTITY_SOURCE =
            "GameMeca-2005-main-server-dragon+selected-client-sprite+same-family-380-npc";
    static final String DRAGON_SELECTION_SOURCE =
            "provisional-equal-none+antharas+fafurion+valakas";
    static final String NON_LEADER_DRAGON_BOUNDARY_SOURCE =
            "GameMeca-LWC-2005-non-room-owner-break-often-no-dragon+"
                    + "unknown-original-probability+conservative-no-dragon";
    static final int CORPSE_LIFETIME_SECONDS = 3;

    private static final long FIRST_OBJECT_ID = 0x6D00_0000L;
    private static final AtomicLong NEXT_OBJECT_ID = new AtomicLong(FIRST_OBJECT_ID);

    enum Phase {
        INACTIVE, WAITING_FOR_START_TOWER, ORDINARY_DOCUMENTED_FRAGMENT,
        FINAL_BOSSES, COMPLETE
    }

    record Template(int npcId, int gfx, String name, int level, int maxHp,
                    int maximumMp, int intelligence) {
        Template {
            if (npcId <= 0 || gfx <= 0 || name == null || name.isBlank()
                    || level <= 0 || level > 0xFF || maxHp <= 0
                    || maximumMp < 0 || intelligence < 0) {
                throw new IllegalArgumentException("invalid Time Attack boss template");
            }
        }
    }

    record SummonedTemplate(int npcId, int gfx, String name, int level,
                            int maxHp, int maximumMp, int intelligence) {
        SummonedTemplate {
            if (npcId <= 0 || gfx <= 0 || name == null || name.isBlank()
                    || level <= 0 || level > 0xFF || maxHp <= 0
                    || maximumMp < 0 || intelligence < 0) {
                throw new IllegalArgumentException("invalid Time Attack summon template");
            }
            BattleAnimationCatalog animations = BattleAnimationCatalog.get();
            animations.requireBattleCapableSprite(gfx, BattleActionCodes.WALK);
            animations.requireTimedAction(gfx, BattleActionCodes.ATTACK);
        }
    }

    record DocumentedOrdinaryFragment(
            int npcId, int gfx, String name, int level, int maxHp,
            int maximumMp, int intelligence,
            int observedSecondsRemaining, List<Integer> nativeTimedActions,
            String source, boolean completePattern) {
        DocumentedOrdinaryFragment {
            if (npcId <= 0 || gfx <= 0 || name == null || name.isBlank()
                    || level <= 0 || level > 0xFF || maxHp <= 0
                    || maximumMp < 0 || intelligence < 0
                    || observedSecondsRemaining < 0
                    || nativeTimedActions == null || nativeTimedActions.isEmpty()
                    || source == null || source.isBlank() || completePattern) {
                throw new IllegalArgumentException(
                        "invalid incomplete Time Attack ordinary fragment");
            }
            nativeTimedActions = List.copyOf(nativeTimedActions);
            BattleAnimationCatalog animations = BattleAnimationCatalog.get();
            for (int action : nativeTimedActions) {
                animations.requireTimedAction(gfx, action);
            }
        }
    }

    record DocumentedPattern(
            String id, String reportSource,
            List<DocumentedOrdinaryFragment> ordinaryFragments,
            List<Template> finalBosses, boolean completePattern) {
        DocumentedPattern {
            if (id == null || id.isBlank() || reportSource == null
                    || reportSource.isBlank() || ordinaryFragments == null
                    || ordinaryFragments.isEmpty() || finalBosses == null
                    || finalBosses.size() != 2 || completePattern) {
                throw new IllegalArgumentException(
                        "invalid incomplete Time Attack documented pattern");
            }
            ordinaryFragments = List.copyOf(ordinaryFragments);
            finalBosses = List.copyOf(finalBosses);
        }
    }

    record Snapshot(long objectId, int npcId, int x, int y, int gfx,
                    String name, int level, int hp, int maxHp, boolean alive,
                    boolean finalBoss, boolean activeAi,
                    int currentMp, int maximumMp, int intelligence,
                    String identitySource, String ordinaryWaveSource,
                    String placementSource) {}

    record AiAttack(
            Snapshot actor, boolean magic,
            BattleTimeAttackMagicSkillCatalog.Definition magicSkill,
            BattleTimeAttackMagicSkillCatalog.VisualDefinition visualSkill,
            BattleTimeAttackMagicSkillCatalog.PhysicalDefinition physicalSkill,
            BattleTimeAttackMagicSkillCatalog.SummonDefinition summonSkill,
            BattleTimeAttackMagicSkillCatalog.GroundDefinition groundSkill,
            List<Snapshot> spawned,
            int triggerRoll, int selectedCandidate) {
        AiAttack(Snapshot actor, boolean magic,
                 BattleTimeAttackMagicSkillCatalog.Definition magicSkill,
                 BattleTimeAttackMagicSkillCatalog.PhysicalDefinition physicalSkill,
                 BattleTimeAttackMagicSkillCatalog.SummonDefinition summonSkill,
                 List<Snapshot> spawned, int triggerRoll, int selectedCandidate) {
            this(actor, magic, magicSkill, null, physicalSkill, summonSkill, null,
                    spawned, triggerRoll, selectedCandidate);
        }

        AiAttack(Snapshot actor, boolean magic,
                 BattleTimeAttackMagicSkillCatalog.Definition magicSkill,
                 BattleTimeAttackMagicSkillCatalog.PhysicalDefinition physicalSkill,
                 BattleTimeAttackMagicSkillCatalog.SummonDefinition summonSkill,
                 BattleTimeAttackMagicSkillCatalog.GroundDefinition groundSkill,
                 List<Snapshot> spawned, int triggerRoll, int selectedCandidate) {
            this(actor, magic, magicSkill, null, physicalSkill, summonSkill,
                    groundSkill, spawned, triggerRoll, selectedCandidate);
        }

        AiAttack {
            spawned = List.copyOf(spawned);
            if (actor == null || !actor.alive()
                    || magic != (magicSkill != null || visualSkill != null)
                    || magicSkill != null && (visualSkill != null
                        || physicalSkill != null
                        || summonSkill != null || groundSkill != null
                        || !spawned.isEmpty())
                    || visualSkill != null && (physicalSkill != null
                        || summonSkill != null || groundSkill != null
                        || !spawned.isEmpty())
                    || physicalSkill != null && (summonSkill != null
                        || groundSkill != null)
                    || summonSkill != null && groundSkill != null
                    || summonSkill == null && !spawned.isEmpty()
                    || triggerRoll < 0 || triggerRoll > 100
                    || selectedCandidate < 0) {
                throw new IllegalArgumentException("invalid Time Attack AI attack");
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
                             boolean weakness, boolean disease, boolean silence,
                             boolean decayPotion, boolean polluteWater,
                             boolean strikerGale,
                             boolean sleep,
                             boolean paralysis,
                             boolean freeze, boolean earthBind,
                             boolean thunderGrab, boolean horrorOfDeath,
                             boolean boneBreak,
                             boolean windShackle,
                             BattleElementalFallDownStatus.Cancellation
                                     elementalFallDown,
                             boolean eraseMagic,
                             boolean panic) {}

    record Hit(Damage damage, boolean finalBossesCleared, int actorsRemaining,
               List<Snapshot> spawned) {
        Hit {
            spawned = List.copyOf(spawned);
        }
    }

    record PoisonUpdate(long attackerObjectId, Snapshot target,
                        List<Hit> hits, boolean expired) {
        PoisonUpdate {
            if (attackerObjectId <= 0 || target == null) {
                throw new IllegalArgumentException("invalid Time Attack poison update");
            }
            hits = List.copyOf(hits);
        }
    }

    record Capture(Snapshot captured, int actorsRemaining, List<Snapshot> spawned) {
        Capture {
            if (captured == null) throw new IllegalArgumentException("captured actor is required");
            spawned = List.copyOf(spawned);
        }
    }

    private record Corpse(Snapshot snapshot, long expiresAtNanos) {}

    private static final Template GREAT_MINOTAUR =
            template(45584, 2402, "$2106", 53, 6_500, 200, 20),
            ANCIENT_GIANT =
            template(45610, 2421, "$2109", 56, 6_500, 350, 22),
            DEATH_KNIGHT =
            template(45601, 240, "$371", 55, 7_500, 250, 19),
            ZOMBIE_LORD =
            template(45650, 2803, "$2398", 62, 9_500, 100, 17),
            ANTHARAS =
            template(45682, 1245, "$1116", 90, 33_500, 800, 30),
            FAFURION =
            template(45683, 1590, "$1426", 93, 35_500, 5_000, 35),
            VALAKAS =
            template(45684, 2001, "$1605", 95, 38_800, 4_500, 35);
    private static final SummonedTemplate SPARTOI =
            new SummonedTemplate(45161, 145, "$318", 16, 120, 30, 14);
    private static final DocumentedOrdinaryFragment BLACK_KNIGHT_CHIEF =
            new DocumentedOrdinaryFragment(
                    81321, 3101, "$273", 48, 5_000, 250, 17, 420,
                    List.of(0, 1, 30, 19, 18, 4, 5, 11, 12, 20, 24,
                            40, 41, 46, 47, 50, 51, 54, 55, 58, 59, 62),
                    FUKUOKA_BLACK_KNIGHT_CHIEF_SOURCE, false),
            OWLBEAR = new DocumentedOrdinaryFragment(
                    45147, 1162, "$1047", 15, 250, 10, 8, 420,
                    List.of(0, 1, 17, 18, 19), TOKYO_OWLBEAR_SOURCE, false),
            ICE_GOLEM = new DocumentedOrdinaryFragment(
                    45182, 1525, "$1395", 18, 220, 0, 6, 373,
                    List.of(0, 1, 30), TOKYO_ICE_GOLEM_FRAGMENT_SOURCE, false),
            SEA_DANCER = new DocumentedOrdinaryFragment(
                    45812, 1603, "$1429", 27, 210, 500, 18, 166,
                    List.of(0, 18, 19), TOKYO_ORDINARY_FRAGMENT_SOURCE, false);
    private static final List<Template> FINAL_BOSSES =
            List.of(GREAT_MINOTAUR, ANCIENT_GIANT);
    private static final List<DocumentedOrdinaryFragment> ORDINARY_FRAGMENTS =
            List.of(ICE_GOLEM, SEA_DANCER);
    private static final List<DocumentedPattern> DOCUMENTED_PATTERNS = List.of(
            new DocumentedPattern("fukuoka-black-knight-chief",
                    NCJAPAN_FUKUOKA_SOURCE, List.of(BLACK_KNIGHT_CHIEF),
                    List.of(GREAT_MINOTAUR, ANCIENT_GIANT), false),
            new DocumentedPattern("tokyo-owlbear",
                    NCJAPAN_TOKYO_SOURCE, List.of(OWLBEAR, ICE_GOLEM, SEA_DANCER),
                    List.of(DEATH_KNIGHT, ZOMBIE_LORD), false));
    private static final List<Template> DRAGON_CANDIDATES =
            List.of(ANTHARAS, FAFURION, VALAKAS);

    private final List<Monster> actors = new ArrayList<>();
    private final List<Corpse> corpses = new ArrayList<>();
    private ArenaMapCatalog.Arena arena;
    private Phase phase = Phase.INACTIVE;
    private int ordinaryFragmentIndex = -1;
    private List<DocumentedOrdinaryFragment> selectedOrdinaryFragments =
            ORDINARY_FRAGMENTS;
    private List<Template> selectedFinalBosses = FINAL_BOSSES;
    private String selectedPatternSource = ORDINARY_WAVE_SOURCE;
    private String selectedBossSource = BOSS_SET_SOURCE;
    private String selectedPatternId = "unit-mechanics-fixture";
    private Template selectedDragon;
    private boolean dragonStageSpawned;

    synchronized void reset(BattleRules.RuleSet rules,
                            ArenaMapCatalog.Arena encounterArena) {
        resetSelection(rules, encounterArena, ORDINARY_FRAGMENTS, FINAL_BOSSES,
                ORDINARY_WAVE_SOURCE, BOSS_SET_SOURCE,
                "unit-mechanics-fixture", null);
    }

    synchronized void reset(BattleRules.RuleSet rules,
                            ArenaMapCatalog.Arena encounterArena,
                            int documentedPatternIndex) {
        reset(rules, encounterArena, documentedPatternIndex, 0);
    }

    synchronized void reset(BattleRules.RuleSet rules,
                            ArenaMapCatalog.Arena encounterArena,
                            int documentedPatternIndex,
                            int dragonOutcomeIndex) {
        if (documentedPatternIndex < 0
                || documentedPatternIndex >= DOCUMENTED_PATTERNS.size()) {
            throw new IllegalArgumentException("invalid Time Attack pattern index");
        }
        if (dragonOutcomeIndex < 0
                || dragonOutcomeIndex > DRAGON_CANDIDATES.size()) {
            throw new IllegalArgumentException("invalid Time Attack dragon outcome index");
        }
        DocumentedPattern pattern = DOCUMENTED_PATTERNS.get(documentedPatternIndex);
        resetSelection(rules, encounterArena, pattern.ordinaryFragments(),
                pattern.finalBosses(),
                documentedPatternIndex == 0
                        ? FUKUOKA_PATTERN_SOURCE : TOKYO_PATTERN_SOURCE,
                pattern.reportSource(), pattern.id(),
                dragonOutcomeIndex == 0
                        ? null : DRAGON_CANDIDATES.get(dragonOutcomeIndex - 1));
    }

    private void resetSelection(BattleRules.RuleSet rules,
                                ArenaMapCatalog.Arena encounterArena,
                                List<DocumentedOrdinaryFragment> ordinaryFragments,
                                List<Template> finalBosses,
                                String patternSource, String bossSource,
                                String patternId, Template dragon) {
        deactivate();
        if (rules == null || encounterArena == null
                || rules.mode() != BattleRules.Mode.TIME_ATTACK) return;
        arena = encounterArena;
        selectedOrdinaryFragments = ordinaryFragments;
        selectedFinalBosses = finalBosses;
        selectedPatternSource = patternSource;
        selectedBossSource = bossSource;
        selectedPatternId = patternId;
        selectedDragon = dragon;
        dragonStageSpawned = false;
        phase = Phase.WAITING_FOR_START_TOWER;
    }

    synchronized void deactivate() {
        actors.clear();
        corpses.clear();
        arena = null;
        phase = Phase.INACTIVE;
        ordinaryFragmentIndex = -1;
        selectedDragon = null;
        dragonStageSpawned = false;
    }

    synchronized List<Snapshot> startAfterTowerDestroyed() {
        return startAfterTowerDestroyed(true);
    }

    synchronized List<Snapshot> startAfterTowerDestroyed(boolean destroyedByLeader) {
        if (phase != Phase.WAITING_FOR_START_TOWER || arena == null) return List.of();
        // The contemporary dragon-room guide says another participant can
        // break the start tower, but that doing so often prevents a dragon.
        // It does not expose the altered probability.  Keep the encounter
        // playable and conservatively select the documented no-dragon branch
        // instead of inventing a replacement chance table.
        if (!destroyedByLeader) selectedDragon = null;
        actors.clear();
        return spawnOrdinaryFragment(0);
    }

    synchronized List<Snapshot> snapshots() {
        if (phase != Phase.ORDINARY_DOCUMENTED_FRAGMENT
                && phase != Phase.FINAL_BOSSES) return List.of();
        return actors.stream().filter(Monster::alive)
                .map(Monster::snapshot).toList();
    }

    synchronized Snapshot find(long objectId) {
        for (Snapshot snapshot : snapshots()) {
            if (snapshot.objectId() == objectId) return snapshot;
        }
        return null;
    }

    synchronized boolean canAttack(long objectId) {
        Snapshot target = find(objectId);
        return target != null && target.alive();
    }

    synchronized Hit damage(long objectId, int amount) {
        if (!canAttack(objectId) || amount <= 0) return null;
        Monster target = actors.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        if (target == null) return null;
        return applyDamage(target, amount, false);
    }

    private Hit applyDamage(Monster target, int amount, boolean poisonDamage) {
        Damage damage = poisonDamage
                ? target.poisonDamage(amount) : target.damage(amount);
        if (damage != null && damage.diedNow()) {
            rememberCorpse(damage.after(), System.nanoTime());
        }
        int remaining = (int) actors.stream().filter(Monster::alive).count();
        if (damage == null || !damage.diedNow() || remaining != 0) {
            return new Hit(damage, false, remaining, List.of());
        }
        if (phase == Phase.ORDINARY_DOCUMENTED_FRAGMENT) {
            int nextFragment = ordinaryFragmentIndex + 1;
            if (nextFragment < selectedOrdinaryFragments.size()) {
                List<Snapshot> spawned = spawnOrdinaryFragment(nextFragment);
                return new Hit(damage, false, spawned.size(), spawned);
            }
            List<Snapshot> spawned = spawnFinalBosses();
            return new Hit(damage, false, spawned.size(), spawned);
        }
        if (selectedDragon != null && !dragonStageSpawned) {
            List<Snapshot> spawned = spawnDragon();
            return new Hit(damage, false, spawned.size(), spawned);
        }
        phase = Phase.COMPLETE;
        return new Hit(damage, true, 0, List.of());
    }

    synchronized boolean poison(long objectId, long attackerObjectId,
                                long nowNanos, int durationSeconds,
                                int tickIntervalMillis) {
        if (!canAttack(objectId) || attackerObjectId <= 0 || nowNanos < 0
                || durationSeconds <= 0 || tickIntervalMillis <= 0) return false;
        Monster target = monster(objectId);
        return target != null && target.activatePoison(
                attackerObjectId, nowNanos, durationSeconds, tickIntervalMillis);
    }

    synchronized boolean poisonActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.poisonActive(nowNanos);
    }

    synchronized boolean curePoison(long objectId) {
        Monster target = monster(objectId);
        return target != null && target.curePoison();
    }

    synchronized List<PoisonUpdate> pollPoisons(
            long nowNanos, int tickIntervalMillis, int tickDamage) {
        if ((phase != Phase.ORDINARY_DOCUMENTED_FRAGMENT
                && phase != Phase.FINAL_BOSSES) || nowNanos < 0
                || tickIntervalMillis <= 0 || tickDamage <= 0) return List.of();
        List<PoisonUpdate> updates = new ArrayList<>();
        for (Monster target : List.copyOf(actors)) {
            long attackerObjectId = target.poisonAttackerObjectId;
            BattleTimedStatus.PoisonPoll poll = target.pollPoison(
                    nowNanos, tickIntervalMillis);
            if (attackerObjectId <= 0
                    || (poll.damageTicks() == 0 && !poll.expired())) continue;
            List<Hit> hits = new ArrayList<>();
            for (int tick = 0; tick < poll.damageTicks() && target.alive(); tick++) {
                Hit hit = applyDamage(target, tickDamage, true);
                if (hit != null && hit.damage() != null) hits.add(hit);
            }
            Snapshot finalTarget = hits.isEmpty()
                    ? target.snapshot() : hits.get(hits.size() - 1).damage().after();
            updates.add(new PoisonUpdate(attackerObjectId, finalTarget,
                    hits, poll.expired()));
        }
        return List.copyOf(updates);
    }

    synchronized Capture capture(long objectId) {
        if (!canAttack(objectId)) return null;
        Monster target = actors.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        if (target == null || target.finalBoss) return null;
        Snapshot captured = target.snapshot();
        actors.remove(target);
        int remaining = (int) actors.stream().filter(Monster::alive).count();
        if (remaining != 0) return new Capture(captured, remaining, List.of());
        if (phase != Phase.ORDINARY_DOCUMENTED_FRAGMENT) return null;
        int nextFragment = ordinaryFragmentIndex + 1;
        if (nextFragment < selectedOrdinaryFragments.size()) {
            List<Snapshot> spawned = spawnOrdinaryFragment(nextFragment);
            return new Capture(captured, spawned.size(), spawned);
        }
        List<Snapshot> spawned = spawnFinalBosses();
        return new Capture(captured, spawned.size(), spawned);
    }

    /** Retained tamed-summon liberation for RETURN_TO_NATURE. */
    synchronized Snapshot releaseTamed(
            Snapshot captured, BattleCompanion.Snapshot companion,
            List<RuntimeInventory.Item> carriedItems) {
        if (captured == null || companion == null || !companion.active()
                || companion.kind() != BattleCompanion.Kind.TAMED
                || captured.npcId() != companion.npcId()
                || captured.finalBoss() || carriedItems == null) return null;
        Monster released = new Monster(captured, companion, carriedItems);
        actors.add(released);
        return released.snapshot();
    }

    synchronized RuntimeInventory releasedInventory(long objectId) {
        Monster monster = actors.stream()
                .filter(candidate -> candidate.objectId == objectId)
                .findFirst().orElse(null);
        return monster == null ? null : monster.inventory.copy();
    }

    synchronized Snapshot findCorpse(long objectId, long nowNanos) {
        if ((phase != Phase.ORDINARY_DOCUMENTED_FRAGMENT && phase != Phase.FINAL_BOSSES)
                || nowNanos < 0) return null;
        pruneCorpses(nowNanos);
        return corpses.stream()
                .map(Corpse::snapshot)
                .filter(corpse -> corpse.objectId() == objectId && !corpse.alive())
                .findFirst().orElse(null);
    }

    synchronized Snapshot consumeCorpse(long objectId, long nowNanos) {
        Snapshot corpse = findCorpse(objectId, nowNanos);
        if (corpse == null) return null;
        corpses.removeIf(entry -> entry.snapshot().objectId() == objectId);
        actors.removeIf(actor -> actor.objectId == objectId && !actor.alive());
        return corpse;
    }

    synchronized Resurrection resurrectCorpse(long objectId, long nowNanos) {
        return resurrectCorpse(objectId, nowNanos, 4);
    }

    synchronized Resurrection resurrectCorpse(
            long objectId, long nowNanos, int hpDivisor) {
        Snapshot corpse = findCorpse(objectId, nowNanos);
        if ((hpDivisor != 1 && hpDivisor != 4) || corpse == null
                || !BattleSpellCatalog.npcAllowsResurrection(corpse.npcId())) {
            return null;
        }
        Monster target = actors.stream()
                .filter(actor -> actor.objectId == objectId && !actor.alive())
                .findFirst().orElse(null);
        if (target == null) {
            target = new Monster(corpse);
            actors.add(target);
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

    synchronized Healing heal(long objectId, int amount) {
        if (!canAttack(objectId) || amount < 0) return null;
        Monster target = actors.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target == null ? null : target.heal(amount);
    }

    synchronized ManaDrain drainMana(long objectId, int sourceMaximumMp, int requested) {
        if (!canAttack(objectId) || sourceMaximumMp < 0 || requested <= 0) return null;
        Monster target = actors.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target == null ? null : target.drainMana(sourceMaximumMp, requested);
    }

    synchronized int recoverMana(long objectId, int amount) {
        if (amount <= 0) return 0;
        Monster target = actors.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target == null ? 0 : target.recoverMana(amount);
    }

    synchronized ManaDrain drainManaIfStrictlyAbove(
            long objectId, int sourceMaximumMp, int requested) {
        if (!canAttack(objectId) || sourceMaximumMp < 0 || requested <= 0) {
            return null;
        }
        Monster target = actors.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target == null ? null
                : target.drainManaIfStrictlyAbove(sourceMaximumMp, requested);
    }

    synchronized boolean haste(long objectId, long nowNanos, int durationSeconds) {
        if (!canAttack(objectId) || nowNanos < 0 || durationSeconds <= 0) return false;
        Monster target = monster(objectId);
        return target != null && target.activateHaste(nowNanos, durationSeconds);
    }

    synchronized boolean hasteActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.hasteActive(nowNanos);
    }

    synchronized boolean slow(long objectId, long nowNanos, int durationSeconds) {
        if (!canAttack(objectId) || nowNanos < 0 || durationSeconds <= 0) return false;
        Monster target = monster(objectId);
        return target != null && target.activateSlow(nowNanos, durationSeconds);
    }

    synchronized boolean slowActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.slowActive(nowNanos);
    }

    synchronized List<Snapshot> expireHastes(long nowNanos) {
        if (nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Monster monster : actors) {
            if (monster.alive() && monster.expireHaste(nowNanos)) {
                expired.add(monster.snapshot());
            }
        }
        return List.copyOf(expired);
    }

    synchronized List<Snapshot> expireSlows(long nowNanos) {
        if (nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Monster monster : actors) {
            if (monster.alive() && monster.expireSlow(nowNanos)) {
                expired.add(monster.snapshot());
            }
        }
        return List.copyOf(expired);
    }

    synchronized MagicCancellation cancelMagicEffects(long objectId) {
        if (!canAttack(objectId)) return null;
        Monster target = monster(objectId);
        return target == null ? null : target.cancelMagicEffects();
    }

    synchronized boolean blind(long objectId, long nowNanos, int durationSeconds) {
        if (!canAttack(objectId) || nowNanos < 0 || durationSeconds <= 0) return false;
        Monster target = actors.stream()
                .filter(monster -> monster.objectId == objectId)
                .findFirst().orElse(null);
        return target != null && !target.finalBoss
                && target.activateBlind(nowNanos, durationSeconds);
    }

    synchronized boolean blindActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.blindActive(nowNanos);
    }

    synchronized int addWeaponBreakStack(long objectId) {
        if (!canAttack(objectId)) return -1;
        Monster target = monster(objectId);
        return target == null ? -1 : target.addWeaponBreakStack();
    }

    synchronized int weaponBreakStacks(long objectId) {
        Monster target = monster(objectId);
        return target == null ? 0 : target.weaponBreakStacks;
    }

    synchronized boolean weakness(long objectId, long nowNanos, int durationSeconds) {
        if (!canAttack(objectId) || nowNanos < 0 || durationSeconds <= 0) return false;
        Monster target = monster(objectId);
        return target != null && target.activateWeakness(nowNanos, durationSeconds);
    }

    synchronized boolean weaknessActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.weaknessActive(nowNanos);
    }

    synchronized boolean disease(long objectId, long nowNanos, int durationSeconds) {
        if (!canAttack(objectId) || nowNanos < 0 || durationSeconds <= 0) return false;
        Monster target = monster(objectId);
        return target != null && target.activateDisease(nowNanos, durationSeconds);
    }

    synchronized boolean diseaseActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.diseaseActive(nowNanos);
    }

    synchronized boolean silence(long objectId, long nowNanos, int durationSeconds) {
        if (!canAttack(objectId) || nowNanos < 0 || durationSeconds <= 0) return false;
        Monster target = monster(objectId);
        return target != null && !target.finalBoss
                && target.activateSilence(nowNanos, durationSeconds);
    }

    synchronized boolean confusionSilence(
            long objectId, long nowNanos, int durationSeconds) {
        if (!canAttack(objectId) || nowNanos < 0 || durationSeconds <= 0) {
            return false;
        }
        Monster target = monster(objectId);
        return target != null && !target.finalBoss
                && target.activateSilenceIfAbsent(nowNanos, durationSeconds);
    }

    synchronized boolean areaSilence(
            long objectId, long nowNanos, int durationSeconds) {
        if (!canAttack(objectId) || nowNanos < 0 || durationSeconds <= 0) {
            return false;
        }
        Monster target = monster(objectId);
        return target != null && !target.finalBoss
                && target.activateAreaSilence(nowNanos, durationSeconds);
    }

    synchronized boolean windShackle(
            long objectId, long nowNanos, int durationSeconds) {
        if (!canAttack(objectId) || nowNanos < 0 || durationSeconds <= 0) {
            return false;
        }
        Monster target = monster(objectId);
        return target != null && !target.finalBoss
                && target.activateWindShackle(nowNanos, durationSeconds);
    }

    synchronized boolean windShackleActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.windShackleActive(nowNanos);
    }

    synchronized boolean silenceActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.silenceActive(nowNanos);
    }

    synchronized boolean decayPotion(long objectId, long nowNanos,
                                     int durationSeconds) {
        if (!canAttack(objectId) || nowNanos < 0 || durationSeconds <= 0) {
            return false;
        }
        Monster target = monster(objectId);
        return target != null && !target.finalBoss
                && target.activateDecayPotion(nowNanos, durationSeconds);
    }

    synchronized boolean decayPotionActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.decayPotionActive(nowNanos);
    }

    synchronized boolean polluteWater(long objectId, long nowNanos,
                                      int durationSeconds) {
        if (!canAttack(objectId) || nowNanos < 0 || durationSeconds <= 0) {
            return false;
        }
        Monster target = monster(objectId);
        return target != null && !target.finalBoss
                && target.activatePolluteWater(nowNanos, durationSeconds);
    }

    synchronized boolean polluteWaterActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.polluteWaterActive(nowNanos);
    }

    synchronized boolean strikerGale(long objectId,
                                     long nowNanos, int durationSeconds) {
        if (!canAttack(objectId) || nowNanos < 0
                || durationSeconds <= 0) return false;
        Monster target = monster(objectId);
        return target != null && !target.finalBoss
                && target.activateStrikerGale(nowNanos, durationSeconds);
    }

    synchronized boolean strikerGaleActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.strikerGaleActive(nowNanos);
    }

    synchronized boolean elementalFallDown(
            long objectId, long nowNanos, int durationSeconds,
            int sourceElfAttribute) {
        if (!canAttack(objectId) || nowNanos < 0 || durationSeconds <= 0) {
            return false;
        }
        Monster target = monster(objectId);
        return target != null && !target.finalBoss
                && target.activateElementalFallDown(
                        nowNanos, durationSeconds, sourceElfAttribute);
    }

    synchronized int elementalFallDownModifier(
            long objectId, int attributeKind, long nowNanos) {
        Monster target = monster(objectId);
        return target == null ? 0 : target.elementalFallDownStatus.modifier(
                attributeKind, nowNanos);
    }

    synchronized boolean eraseMagic(long objectId, long nowNanos,
                                    int durationSeconds) {
        if (!canAttack(objectId) || nowNanos < 0 || durationSeconds <= 0) {
            return false;
        }
        Monster target = monster(objectId);
        return target != null && !target.finalBoss
                && target.activateEraseMagic(nowNanos, durationSeconds);
    }

    synchronized int effectiveMagicResistance(
            long objectId, int baseMagicResistance, long nowNanos) {
        Monster target = monster(objectId);
        return target == null ? baseMagicResistance
                : target.eraseMagicStatus.effectiveMagicResistance(
                        baseMagicResistance, nowNanos);
    }

    synchronized boolean consumeEraseMagic(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.eraseMagicStatus.consume(nowNanos);
    }

    synchronized BattleEraseMagicStatus.Lease captureEraseMagic(
            long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target == null ? BattleEraseMagicStatus.Lease.none()
                : target.eraseMagicStatus.capture(nowNanos);
    }

    synchronized boolean consumeEraseMagic(
            long objectId, BattleEraseMagicStatus.Lease lease) {
        Monster target = monster(objectId);
        return target != null && target.eraseMagicStatus.consume(lease);
    }

    /** Retained NPC casters consume their Silence lease on the rejected spell attempt. */
    synchronized boolean consumeSilenceForSpell(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.consumeSilenceForSpell(nowNanos);
    }

    synchronized boolean sleep(long objectId, long nowNanos, int durationSeconds) {
        if (!canAttack(objectId) || nowNanos < 0 || durationSeconds <= 0) return false;
        Monster target = monster(objectId);
        return target != null && !target.finalBoss
                && target.activateSleep(nowNanos, durationSeconds);
    }

    synchronized boolean sleepActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.sleepActive(nowNanos);
    }

    synchronized boolean cancelSleep(long objectId) {
        Monster target = monster(objectId);
        return target != null && target.cancelSleep();
    }

    synchronized boolean freeze(long objectId, long nowNanos, int durationSeconds) {
        if (!canAttack(objectId) || nowNanos < 0 || durationSeconds <= 0) return false;
        Monster target = monster(objectId);
        return target != null && !target.finalBoss
                && target.activateFreeze(nowNanos, durationSeconds);
    }

    synchronized boolean freezeActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.freezeActive(nowNanos);
    }

    synchronized boolean cancelFreeze(long objectId) {
        Monster target = monster(objectId);
        return target != null && target.cancelFreeze();
    }

    synchronized boolean paralysis(long objectId, long nowNanos, int durationMillis) {
        if (!canAttack(objectId) || nowNanos < 0 || durationMillis <= 0) return false;
        Monster target = monster(objectId);
        return target != null && !target.finalBoss
                && target.activateParalysis(nowNanos, durationMillis);
    }

    synchronized boolean paralysisActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.paralysisActive(nowNanos);
    }

    synchronized boolean shockStun(long objectId, long nowNanos,
                                   int durationSeconds) {
        Monster target = monster(objectId);
        return canAttack(objectId) && target != null
                && target.activateShockStun(nowNanos, durationSeconds);
    }

    synchronized boolean shockStunActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.shockStunActive(nowNanos);
    }

    synchronized boolean boneBreak(long objectId, long nowNanos,
                                   int durationSeconds) {
        Monster target = monster(objectId);
        return canAttack(objectId) && target != null
                && target.activateBoneBreak(nowNanos, durationSeconds);
    }

    synchronized boolean boneBreakActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.boneBreakActive(nowNanos);
    }

    synchronized BattleArmBreakerStatus.Activation armBreaker(
            long objectId, long nowNanos, int durationSeconds) {
        Monster target = monster(objectId);
        return !canAttack(objectId) || target == null
                ? BattleArmBreakerStatus.Activation.rejected()
                : target.armBreakerStatus.activateIfAbsent(
                        nowNanos, durationSeconds);
    }

    synchronized int armBreakerMeleeDamageModifier(
            long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target == null ? 0
                : target.armBreakerStatus.meleeDamageModifier(nowNanos);
    }

    synchronized boolean cancelArmBreaker(long objectId) {
        Monster target = monster(objectId);
        return target != null && target.armBreakerStatus.cancel();
    }

    synchronized boolean panic(long objectId, long nowNanos,
                               int durationSeconds, int abilityScoreModifier) {
        Monster target = monster(objectId);
        return !canAttack(objectId) || target == null || target.finalBoss
                ? false : target.panicStatus.activateIfAbsent(
                        nowNanos, durationSeconds, abilityScoreModifier);
    }

    synchronized int panicAbilityScoreModifier(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target == null ? 0
                : target.panicStatus.snapshot(nowNanos).abilityScoreModifier();
    }

    synchronized boolean cancelPanic(long objectId) {
        Monster target = monster(objectId);
        return target != null && target.panicStatus.cancel();
    }

    synchronized List<Snapshot> expirePanics(long nowNanos) {
        if (nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Monster monster : actors) {
            if (monster.alive() && monster.panicStatus.expire(nowNanos)) {
                expired.add(monster.snapshot());
            }
        }
        return List.copyOf(expired);
    }

    synchronized BattleJoyOfPainStatus.Activation joyOfPain(
            long objectId, long nowNanos, int durationSeconds) {
        Monster target = monster(objectId);
        return !canAttack(objectId) || target == null ? null
                : target.joyOfPainStatus.activateOrRefresh(
                        nowNanos, durationSeconds);
    }

    synchronized BattleJoyOfPainStatus.Lease captureJoyOfPain(
            long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target == null ? null : target.joyOfPainStatus.capture(nowNanos);
    }

    synchronized boolean consumeJoyOfPain(
            long objectId, BattleJoyOfPainStatus.Lease lease) {
        Monster target = monster(objectId);
        return target != null && target.joyOfPainStatus.consume(lease);
    }

    synchronized List<Snapshot> expireJoyOfPain(long nowNanos) {
        if (nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Monster monster : actors) {
            if (monster.alive()
                    && monster.joyOfPainStatus.expire(nowNanos)) {
                expired.add(monster.snapshot());
            }
        }
        return List.copyOf(expired);
    }

    synchronized boolean thunderGrab(long objectId, long nowNanos,
                                     int durationSeconds) {
        Monster target = monster(objectId);
        return canAttack(objectId) && target != null && !target.finalBoss
                && target.activateThunderGrab(nowNanos, durationSeconds);
    }

    synchronized boolean thunderGrabActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.thunderGrabActive(nowNanos);
    }

    synchronized boolean horrorOfDeath(long objectId, long nowNanos,
                                       int durationSeconds) {
        Monster target = monster(objectId);
        return canAttack(objectId) && target != null && !target.finalBoss
                && target.activateHorrorOfDeath(nowNanos, durationSeconds);
    }

    synchronized boolean horrorOfDeathActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.horrorOfDeathActive(nowNanos);
    }

    synchronized List<Snapshot> expireHorrorOfDeaths(long nowNanos) {
        if (nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Monster monster : actors) {
            if (monster.alive()
                    && monster.damagePoison.expireHorrorOfDeath(nowNanos)) {
                expired.add(monster.snapshot());
            }
        }
        return List.copyOf(expired);
    }

    synchronized boolean cancelParalysis(long objectId) {
        Monster target = monster(objectId);
        return target != null && target.cancelParalysis();
    }

    synchronized List<Snapshot> expireParalyses(long nowNanos) {
        if (nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Monster monster : actors) {
            if (monster.alive() && monster.expireParalysis(nowNanos)) {
                expired.add(monster.snapshot());
            }
        }
        return List.copyOf(expired);
    }

    synchronized List<Snapshot> expireFreezes(long nowNanos) {
        if (nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Monster monster : actors) {
            if (monster.alive() && monster.expireFreeze(nowNanos)) {
                expired.add(monster.snapshot());
            }
        }
        return List.copyOf(expired);
    }

    synchronized boolean earthBind(long objectId, long nowNanos, int durationSeconds) {
        if (!canAttack(objectId) || nowNanos < 0 || durationSeconds <= 0) return false;
        Monster target = monster(objectId);
        return target != null && !target.finalBoss
                && target.activateEarthBind(nowNanos, durationSeconds);
    }

    synchronized boolean earthBindActive(long objectId, long nowNanos) {
        Monster target = monster(objectId);
        return target != null && target.earthBindActive(nowNanos);
    }

    synchronized List<Snapshot> expireEarthBinds(long nowNanos) {
        if (nowNanos < 0) return List.of();
        List<Snapshot> expired = new ArrayList<>();
        for (Monster monster : actors) {
            if (monster.alive() && monster.expireEarthBind(nowNanos)) {
                expired.add(monster.snapshot());
            }
        }
        return List.copyOf(expired);
    }

    synchronized boolean blocks(int x, int y) {
        return (phase == Phase.ORDINARY_DOCUMENTED_FRAGMENT
                || phase == Phase.FINAL_BOSSES) && actors.stream()
                .anyMatch(monster -> monster.alive() && monster.x == x && monster.y == y);
    }

    synchronized Snapshot moveForAi(long objectId, int x, int y, long nowNanos) {
        Monster monster = monster(objectId);
        if ((phase != Phase.ORDINARY_DOCUMENTED_FRAGMENT
                && phase != Phase.FINAL_BOSSES) || arena == null || monster == null
                || !monster.activeAi
                || !monster.ready(nowNanos)
                || monster.blindActive(nowNanos)
                || monster.sleepActive(nowNanos)
                || monster.paralysisActive(nowNanos)
                || monster.shockStunActive(nowNanos)
                || monster.boneBreakActive(nowNanos)
                || monster.thunderGrabActive(nowNanos)
                || monster.freezeActive(nowNanos)
                || monster.earthBindActive(nowNanos)
                || Math.max(Math.abs(monster.x - x),
                    Math.abs(monster.y - y)) != 1
                || !arena.nativeCanStep(monster.x, monster.y, x, y)) return null;
        boolean occupied = actors.stream().anyMatch(candidate ->
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
        if ((phase != Phase.ORDINARY_DOCUMENTED_FRAGMENT
                && phase != Phase.FINAL_BOSSES) || monster == null
                || !monster.activeAi
                || !monster.ready(nowNanos)
                || monster.sleepActive(nowNanos)
                || monster.paralysisActive(nowNanos)
                || monster.shockStunActive(nowNanos)
                || monster.boneBreakActive(nowNanos)
                || monster.thunderGrabActive(nowNanos)
                || monster.freezeActive(nowNanos)
                || monster.earthBindActive(nowNanos)
                || !BattleAnimationCatalog.get().supportsTimedAction(
                        monster.gfx, BattleActionCodes.ATTACK)) return null;
        monster.schedule(nowNanos, BattleActionCodes.ATTACK);
        return monster.snapshot();
    }

    synchronized AiAttack aiAttackFor(long objectId, int targetX, int targetY,
                                      long nowNanos) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Monster monster = monster(objectId);
        if (monster == null) return null;
        List<BattleTimeAttackMagicSkillCatalog.MobskillRow> rows =
                BattleTimeAttackMagicSkillCatalog.rowsForNpc(monster.npcId);
        int[] triggerRolls = new int[rows.size()];
        for (int index = 0; index < triggerRolls.length; index++) {
            triggerRolls[index] = random.nextInt(1, 101);
        }
        BattleTimeAttackMagicSkillCatalog.SummonDefinition summon =
                BattleTimeAttackMagicSkillCatalog.summonForNpc(monster.npcId);
        int summonCountRoll = summon == null ? 0
                : random.nextInt(summon.summonRandomBound());
        return aiAttackFor(objectId, targetX, targetY, nowNanos,
                triggerRolls, random.nextInt(Integer.MAX_VALUE),
                summonCountRoll);
    }

    /** Deterministic package fixture for exact selected mobskill selection. */
    synchronized AiAttack aiAttackFor(
            long objectId, int targetX, int targetY, long nowNanos,
            int magicTriggerRoll, int physicalTriggerRoll,
            int candidateSelector) {
        if (magicTriggerRoll < 1 || magicTriggerRoll > 100
                || physicalTriggerRoll < 1 || physicalTriggerRoll > 100
                || candidateSelector < 0) {
            throw new IllegalArgumentException("invalid Time Attack AI fixture");
        }
        Monster monster = monster(objectId);
        if (monster == null) return null;
        List<BattleTimeAttackMagicSkillCatalog.MobskillRow> rows =
                BattleTimeAttackMagicSkillCatalog.rowsForNpc(monster.npcId);
        int[] triggerRolls = new int[rows.size()];
        for (int index = 0; index < rows.size(); index++) {
            triggerRolls[index] = rows.get(index)
                    instanceof BattleTimeAttackMagicSkillCatalog.PhysicalDefinition
                        ? physicalTriggerRoll : magicTriggerRoll;
        }
        BattleTimeAttackMagicSkillCatalog.SummonDefinition summon =
                BattleTimeAttackMagicSkillCatalog.summonForNpc(monster.npcId);
        int summonCountRoll = summon == null ? 0
                : candidateSelector % summon.summonRandomBound();
        return aiAttackFor(objectId, targetX, targetY, nowNanos,
                triggerRolls, candidateSelector, summonCountRoll);
    }

    /** Select one retained actNo while preserving the full independent roll vector. */
    synchronized AiAttack aiAttackForActNoFixture(
            long objectId, int targetX, int targetY, long nowNanos,
            int actNo, int candidateSelector) {
        if (actNo < 0 || candidateSelector < 0) {
            throw new IllegalArgumentException("invalid Time Attack actNo fixture");
        }
        Monster monster = monster(objectId);
        List<BattleTimeAttackMagicSkillCatalog.MobskillRow> rows = monster == null
                ? List.of()
                : BattleTimeAttackMagicSkillCatalog.rowsForNpc(monster.npcId);
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
                BattleTimeAttackMagicSkillCatalog.summonForNpc(monster.npcId);
        int summonCountRoll = summon == null ? 0
                : candidateSelector % summon.summonRandomBound();
        return aiAttackFor(objectId, targetX, targetY, nowNanos,
                triggerRolls, candidateSelector, summonCountRoll);
    }

    /** Exact per-actNo deterministic fixture matching retained independent rolls. */
    synchronized AiAttack aiAttackFor(
            long objectId, int targetX, int targetY, long nowNanos,
            int[] triggerRolls, int candidateSelector, int summonCountRoll) {
        if (triggerRolls == null || candidateSelector < 0 || summonCountRoll < 0) {
            throw new IllegalArgumentException("invalid Time Attack row fixture");
        }
        Monster monster = monster(objectId);
        List<BattleTimeAttackMagicSkillCatalog.MobskillRow> rows = monster == null
                ? List.of()
                : BattleTimeAttackMagicSkillCatalog.rowsForNpc(monster.npcId);
        if (triggerRolls.length != rows.size()) {
            throw new IllegalArgumentException("Time Attack row roll count mismatch");
        }
        for (int roll : triggerRolls) {
            if (roll < 1 || roll > 100) {
                throw new IllegalArgumentException("invalid Time Attack row roll");
            }
        }
        if ((phase != Phase.ORDINARY_DOCUMENTED_FRAGMENT
                && phase != Phase.FINAL_BOSSES) || monster == null
                || !monster.activeAi || !monster.ready(nowNanos)
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
                        && monster.magicReady(magic, nowNanos)) {
                    eligible.add(new Eligible(row, roll));
                }
            } else if (row instanceof BattleTimeAttackMagicSkillCatalog
                    .VisualDefinition visual) {
                if (visual.castDistance(distance)
                        && !monster.silenceActive(nowNanos)
                        && monster.visualReady(visual, nowNanos)
                        && BattleAnimationCatalog.get().supportsTimedAction(
                                monster.gfx, visual.actionId())) {
                    eligible.add(new Eligible(row, roll));
                }
            } else if (row instanceof BattleTimeAttackMagicSkillCatalog
                    .PhysicalDefinition physical) {
                if (physical.attackDistance(distance)
                        && BattleAnimationCatalog.get().supportsTimedAction(
                        monster.gfx, physical.effectiveActionId())) {
                    eligible.add(new Eligible(row, roll));
                }
            } else if (row instanceof BattleTimeAttackMagicSkillCatalog
                    .SummonDefinition summon
                    && monster.summonReady(summon)) {
                eligible.add(new Eligible(row, roll));
            } else if (row instanceof BattleTimeAttackMagicSkillCatalog
                    .GroundDefinition ground) {
                if (ground.castDistance(distance)
                        && !monster.silenceActive(nowNanos)
                        && monster.groundReady(ground, nowNanos)
                        && BattleAnimationCatalog.get().supportsTimedAction(
                                monster.gfx, ground.actionId())) {
                    eligible.add(new Eligible(row, roll));
                }
            }
            // InertDefinition rows retain their exact actNo/trigger position
            // but have either a proved-empty trigger/skill-range intersection
            // or a selected-source orphan skill that retained checkUseSkill()
            // rejects before any executable skill state exists.
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
                    .VisualDefinition visual) {
                monster.scheduleVisual(nowNanos, visual);
                return new AiAttack(monster.snapshot(), true, null, visual,
                        null, null, null, List.of(), chosen.triggerRoll(), selected);
            }
            if (chosen.row() instanceof BattleTimeAttackMagicSkillCatalog
                    .PhysicalDefinition physical) {
                monster.schedule(nowNanos, physical.effectiveActionId());
                return new AiAttack(monster.snapshot(), false, null,
                        physical, null, List.of(), chosen.triggerRoll(), selected);
            }
            if (chosen.row() instanceof BattleTimeAttackMagicSkillCatalog
                    .GroundDefinition ground) {
                monster.scheduleGround(nowNanos, ground);
                return new AiAttack(monster.snapshot(), false, null,
                        null, null, ground, List.of(),
                        chosen.triggerRoll(), selected);
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

        // Retained L1NpcInstance falls through to ordinary attack after a
        // rejected mobskill. Only sprites with exact action 1 may do so.
        if (distance <= 1 && BattleAnimationCatalog.get().supportsTimedAction(
                monster.gfx, BattleActionCodes.ATTACK)) {
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
        if (arena == null || caster == null || summon == null
                || summon.summonNpcId() != SPARTOI.npcId()
                || requestedCount <= 0 || placementSelector < 0) return List.of();
        List<ArenaMapCatalog.Point> candidates = new ArrayList<>();
        // Retained L1MobSkillUseSpawn uses randomLocation(6, false) for every
        // summon; do not narrow the source contract to adjacent tiles.
        for (int y = caster.y - 6; y <= caster.y + 6; y++) {
            for (int x = caster.x - 6; x <= caster.x + 6; x++) {
                int candidateX = x;
                int candidateY = y;
                if (candidateX == caster.x && candidateY == caster.y
                        || !arena.nativeStandingFloor(candidateX, candidateY)
                        || actors.stream().anyMatch(actor -> actor.alive()
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
            Monster actor = new Monster(SPARTOI, point.x(), point.y(),
                    DEATH_KNIGHT_SUMMON_SOURCE, selectedPatternSource);
            actors.add(actor);
            spawned.add(actor.snapshot());
        }
        return List.copyOf(spawned);
    }

    synchronized Phase phase() {
        return phase;
    }

    static List<Template> finalBosses() {
        return FINAL_BOSSES;
    }

    static List<DocumentedOrdinaryFragment> documentedOrdinaryFragments() {
        return ORDINARY_FRAGMENTS;
    }

    static List<DocumentedPattern> documentedPatterns() {
        return DOCUMENTED_PATTERNS;
    }

    static List<Template> dragonCandidates() {
        return DRAGON_CANDIDATES;
    }

    static boolean isDragonNpc(int npcId) {
        return DRAGON_CANDIDATES.stream().anyMatch(dragon -> dragon.npcId() == npcId);
    }

    static boolean isDragonStageActor(Snapshot snapshot) {
        return snapshot != null
                && DRAGON_IDENTITY_SOURCE.equals(snapshot.identitySource());
    }

    synchronized String selectedPatternId() {
        return selectedPatternId;
    }

    static boolean ordinaryWavePatternRecovered() {
        return false;
    }

    static boolean patternSelectionWeightsRecovered() {
        return false;
    }

    static boolean documentedOrdinaryFragmentEnabled() {
        return true;
    }

    private Monster monster(long objectId) {
        return actors.stream()
                .filter(candidate -> candidate.objectId == objectId && candidate.alive())
                .findFirst().orElse(null);
    }

    private List<Snapshot> spawnFinalBosses() {
        actors.clear();
        for (int index = 0; index < selectedFinalBosses.size(); index++) {
            ArenaMapCatalog.Point point =
                    BattleTimeAttackSpawnCatalog.encounterSpawn(arena, index);
            actors.add(new Monster(selectedFinalBosses.get(index), point.x(), point.y(),
                    selectedBossSource, selectedPatternSource));
        }
        phase = Phase.FINAL_BOSSES;
        return snapshots();
    }

    private List<Snapshot> spawnDragon() {
        if (selectedDragon == null || dragonStageSpawned) return List.of();
        actors.clear();
        ArenaMapCatalog.Point point =
                BattleTimeAttackSpawnCatalog.encounterSpawn(arena, 3);
        actors.add(new Monster(selectedDragon, point.x(), point.y(),
                DRAGON_IDENTITY_SOURCE,
                DRAGON_STAGE_SOURCE + "+" + DRAGON_SELECTION_SOURCE));
        dragonStageSpawned = true;
        phase = Phase.FINAL_BOSSES;
        return snapshots();
    }

    private List<Snapshot> spawnOrdinaryFragment(int index) {
        actors.clear();
        ordinaryFragmentIndex = index;
        DocumentedOrdinaryFragment fragment = selectedOrdinaryFragments.get(index);
        ArenaMapCatalog.Point point =
                BattleTimeAttackSpawnCatalog.encounterSpawn(arena, index);
        actors.add(new Monster(fragment, point.x(), point.y(),
                selectedPatternSource));
        phase = Phase.ORDINARY_DOCUMENTED_FRAGMENT;
        return snapshots();
    }

    private static Template template(int npcId, int gfx, String name,
                                     int level, int maxHp,
                                     int maximumMp, int intelligence) {
        BattleAnimationCatalog.get().requireBattleCapableSprite(
                gfx, BattleActionCodes.WALK);
        return new Template(npcId, gfx, name, level, maxHp,
                maximumMp, intelligence);
    }

    private static final class Monster {
        final long objectId;
        final int npcId;
        final int gfx;
        final String name;
        final int level;
        final int maxHp;
        final int intelligence;
        final boolean finalBoss;
        final boolean activeAi;
        final String identitySource;
        final String waveSource;
        int x;
        int y;
        int hp;
        int mp;
        int maxMp;
        final java.util.Map<Integer, Long> skillReadyNanos =
                new java.util.HashMap<>();
        final java.util.Map<Integer, Integer> skillUseCounts =
                new java.util.HashMap<>();
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

        Monster(Template template, int x, int y,
                String identitySource, String waveSource) {
            this.objectId = NEXT_OBJECT_ID.getAndUpdate(current ->
                    current >= 0x6DFF_FFFFL ? FIRST_OBJECT_ID : current + 1);
            this.npcId = template.npcId();
            this.gfx = template.gfx();
            this.name = template.name();
            this.level = template.level();
            this.maxHp = template.maxHp();
            this.maxMp = template.maximumMp();
            this.mp = maxMp;
            this.intelligence = template.intelligence();
            this.finalBoss = true;
            this.activeAi = true;
            this.identitySource = identitySource;
            this.waveSource = waveSource;
            this.x = x;
            this.y = y;
            hp = maxHp;
        }

        Monster(DocumentedOrdinaryFragment fragment, int x, int y,
                String waveSource) {
            this.objectId = NEXT_OBJECT_ID.getAndUpdate(current ->
                    current >= 0x6DFF_FFFFL ? FIRST_OBJECT_ID : current + 1);
            this.npcId = fragment.npcId();
            this.gfx = fragment.gfx();
            this.name = fragment.name();
            this.level = fragment.level();
            this.maxHp = fragment.maxHp();
            this.maxMp = fragment.maximumMp();
            this.mp = maxMp;
            this.intelligence = fragment.intelligence();
            this.finalBoss = false;
            this.activeAi = BattleAnimationCatalog.get().supportsTimedAction(
                    gfx, BattleActionCodes.WALK)
                    && (BattleAnimationCatalog.get().supportsTimedAction(
                            gfx, BattleActionCodes.ATTACK)
                        || BattleTimeAttackMagicSkillCatalog.magicForNpc(npcId) != null);
            this.identitySource = fragment.source();
            this.waveSource = waveSource;
            this.x = x;
            this.y = y;
            hp = maxHp;
        }

        Monster(SummonedTemplate template, int x, int y,
                String identitySource, String waveSource) {
            this.objectId = NEXT_OBJECT_ID.getAndUpdate(current ->
                    current >= 0x6DFF_FFFFL ? FIRST_OBJECT_ID : current + 1);
            this.npcId = template.npcId();
            this.gfx = template.gfx();
            this.name = template.name();
            this.level = template.level();
            this.maxHp = template.maxHp();
            this.maxMp = template.maximumMp();
            this.mp = maxMp;
            this.intelligence = template.intelligence();
            this.finalBoss = false;
            this.activeAi = true;
            this.identitySource = identitySource;
            this.waveSource = waveSource;
            this.x = x;
            this.y = y;
            hp = maxHp;
        }

        Monster(Snapshot corpse) {
            this.objectId = corpse.objectId();
            this.npcId = corpse.npcId();
            this.gfx = corpse.gfx();
            this.name = corpse.name();
            this.level = corpse.level();
            this.maxHp = corpse.maxHp();
            this.maxMp = corpse.maximumMp();
            this.mp = corpse.currentMp();
            this.intelligence = corpse.intelligence();
            this.finalBoss = corpse.finalBoss();
            this.activeAi = corpse.activeAi();
            this.identitySource = corpse.identitySource();
            this.waveSource = corpse.ordinaryWaveSource();
            this.x = corpse.x();
            this.y = corpse.y();
            this.hp = 0;
        }

        Monster(Snapshot captured, BattleCompanion.Snapshot companion,
                List<RuntimeInventory.Item> carriedItems) {
            this.objectId = NEXT_OBJECT_ID.getAndUpdate(current ->
                    current >= 0x6DFF_FFFFL ? FIRST_OBJECT_ID : current + 1);
            this.npcId = captured.npcId();
            this.gfx = captured.gfx();
            this.name = captured.name();
            this.level = captured.level();
            this.maxHp = captured.maxHp();
            this.maxMp = captured.maximumMp();
            this.mp = Math.max(0, Math.min(companion.mp(), maxMp));
            this.intelligence = captured.intelligence();
            this.finalBoss = false;
            this.activeAi = captured.activeAi();
            this.identitySource = captured.identitySource();
            this.waveSource = captured.ordinaryWaveSource();
            this.x = companion.x();
            this.y = companion.y();
            this.hp = Math.max(1, Math.min(companion.hp(), maxHp));
            for (RuntimeInventory.Item item : carriedItems) {
                if (!inventory.add(item, item.quantity())) {
                    throw new IllegalStateException(
                            "released Time Attack monster inventory exceeds native capacity");
                }
            }
        }

        boolean alive() {
            return hp > 0;
        }

        Snapshot snapshot() {
            return new Snapshot(objectId, npcId, x, y,
                    gfx, name, level, hp,
                    maxHp, hp > 0, finalBoss, activeAi,
                    mp, maxMp, intelligence,
                    identitySource,
                    waveSource, PLACEMENT_SOURCE);
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
                paralysisExpiresAtNanos = 0;
                shockStunExpiresAtNanos = 0;
                boneBreakExpiresAtNanos = 0;
                thunderGrabExpiresAtNanos = 0;
                freezeExpiresAtNanos = 0;
                earthBindExpiresAtNanos = 0;
                hasteExpiresAtNanos = 0;
                slowExpiresAtNanos = 0;
                weaponBreakStacks = 0;
                curePoison();
                joyOfPainStatus.clear();
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
            if (hp > 0 || restoredHp <= 0) {
                throw new IllegalStateException("invalid Time Attack resurrection");
            }
            hp = Math.min(maxHp, restoredHp);
            nextActionNanos = 0;
            skillReadyNanos.clear();
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
            hp = Math.min(maxHp, hp + amount);
            Snapshot after = snapshot();
            return new Healing(before, after, after.hp() - before.hp());
        }

        ManaDrain drainMana(int sourceMaximumMp, int requested) {
            if (!alive() || sourceMaximumMp < 0 || requested <= 0) return null;
            if (maxMp != sourceMaximumMp) {
                throw new IllegalStateException("Time Attack actor source maximum MP changed");
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
                        "Time Attack actor source maximum MP changed");
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
            long expiresAt = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            weaknessExpiresAtNanos = Math.max(weaknessExpiresAtNanos, expiresAt);
            return true;
        }

        boolean weaknessActive(long nowNanos) {
            return nowNanos >= 0 && nowNanos < weaknessExpiresAtNanos;
        }

        boolean activateDisease(long nowNanos, int durationSeconds) {
            if (!alive() || nowNanos < 0 || durationSeconds <= 0) return false;
            long expiresAt = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            diseaseExpiresAtNanos = Math.max(diseaseExpiresAtNanos, expiresAt);
            return true;
        }

        boolean diseaseActive(long nowNanos) {
            return nowNanos >= 0 && nowNanos < diseaseExpiresAtNanos;
        }

        boolean activateSilence(long nowNanos, int durationSeconds) {
            if (!alive() || finalBoss || nowNanos < 0 || durationSeconds <= 0) {
                return false;
            }
            long expiresAt = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            silenceExpiresAtNanos = Math.max(silenceExpiresAtNanos, expiresAt);
            return true;
        }

        boolean activateSilenceIfAbsent(long nowNanos, int durationSeconds) {
            if (!alive() || finalBoss || nowNanos < 0 || durationSeconds <= 0) {
                return false;
            }
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
            if (!alive() || finalBoss || nowNanos < 0 || durationSeconds <= 0) {
                return false;
            }
            long expiresAt = Math.addExact(nowNanos,
                    TimeUnit.SECONDS.toNanos(durationSeconds));
            areaSilenceExpiresAtNanos = Math.max(
                    areaSilenceExpiresAtNanos, expiresAt);
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
            if (!alive() || finalBoss || nowNanos < 0 || durationSeconds <= 0) {
                return false;
            }
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
            if (!alive() || finalBoss || nowNanos < 0 || durationSeconds <= 0) {
                return false;
            }
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
            if (!alive() || finalBoss || nowNanos < 0 || durationSeconds <= 0) {
                return false;
            }
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
            if (!alive() || finalBoss || nowNanos < 0 || durationSeconds <= 0) {
                return false;
            }
            elementalFallDownStatus.activate(
                    nowNanos, durationSeconds, sourceElfAttribute);
            return true;
        }

        boolean activateEraseMagic(long nowNanos, int durationSeconds) {
            if (!alive() || finalBoss || nowNanos < 0 || durationSeconds <= 0) {
                return false;
            }
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
            if (!alive() || finalBoss || nowNanos < 0 || durationSeconds <= 0) {
                return false;
            }
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
            if (!alive() || finalBoss || nowNanos < 0 || durationMillis <= 0
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
                    || finalBoss || thunderGrabActive(nowNanos)) return false;
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
            if (!alive() || finalBoss || nowNanos < 0 || durationSeconds <= 0
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
            if (!alive() || finalBoss || nowNanos < 0 || durationSeconds <= 0
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
                            gfx, action, hasteActive(nowNanos),
                            slowActive(nowNanos), windShackleActive(nowNanos)));
            nextActionNanos = Math.addExact(nowNanos, delay);
        }

        boolean magicReady(BattleTimeAttackMagicSkillCatalog.Definition skill,
                           long nowNanos) {
            return skill != null && skill.npcId() == npcId
                    && mp >= skill.mpCost()
                    && nowNanos >= skillReadyNanos.getOrDefault(skill.actNo(), 0L);
        }

        boolean visualReady(BattleTimeAttackMagicSkillCatalog.VisualDefinition skill,
                            long nowNanos) {
            return skill != null && skill.npcId() == npcId
                    && mp >= skill.mpCost()
                    && nowNanos >= skillReadyNanos.getOrDefault(skill.actNo(), 0L);
        }

        void scheduleVisual(long nowNanos,
                            BattleTimeAttackMagicSkillCatalog.VisualDefinition skill) {
            if (!visualReady(skill, nowNanos)) {
                throw new IllegalStateException("Time Attack visual skill was not ready");
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

        boolean groundReady(BattleTimeAttackMagicSkillCatalog.GroundDefinition skill,
                            long nowNanos) {
            return skill != null && skill.npcId() == npcId
                    && mp >= skill.mpCost()
                    && nowNanos >= skillReadyNanos.getOrDefault(skill.actNo(), 0L);
        }

        void scheduleGround(long nowNanos,
                            BattleTimeAttackMagicSkillCatalog.GroundDefinition skill) {
            if (!groundReady(skill, nowNanos)) {
                throw new IllegalStateException("Time Attack ground skill was not ready");
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
            skillReadyNanos.put(skill.actNo(), nextActionNanos);
        }

        void scheduleMagic(long nowNanos,
                           BattleTimeAttackMagicSkillCatalog.Definition skill) {
            if (!magicReady(skill, nowNanos)) {
                throw new IllegalStateException("Time Attack magic was not ready");
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
            return summon != null && summon.npcId() == npcId
                    && skillUseCounts.getOrDefault(summon.actNo(), 0)
                        < summon.triggerCount();
        }

        void scheduleSummon(long nowNanos,
                            BattleTimeAttackMagicSkillCatalog.SummonDefinition summon) {
            if (!summonReady(summon)) {
                throw new IllegalStateException("Time Attack summon was not ready");
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
