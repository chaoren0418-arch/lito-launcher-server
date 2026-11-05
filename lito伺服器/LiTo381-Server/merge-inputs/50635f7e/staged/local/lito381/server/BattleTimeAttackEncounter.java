// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

import java.util.Iterator;

import java.util.HashMap;

import java.util.Map;

import java.util.ArrayList;
import java.util.Arrays;
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

    @AsRecord(components = {"objectId", "npcId", "x", "y", "gfx", "name", "level", "hp", "maxHp", "alive", "finalBoss", "activeAi", "currentMp", "maximumMp", "intelligence", "identitySource", "ordinaryWaveSource", "placementSource"}, finalFields = {"nativeSourceNpcFactoryMemory", "nativeSourceNpcFirstView"})
    static final class Snapshot {
        private final long objectId;
        private final int npcId;
        private final int x;
        private final int y;
        private final int gfx;
        private final String name;
        private final int level;
        private final int hp;
        private final int maxHp;
        private final boolean alive;
        private final boolean finalBoss;
        private final boolean activeAi;
        private final int currentMp;
        private final int maximumMp;
        private final int intelligence;
        private final String identitySource;
        private final String ordinaryWaveSource;
        private final String placementSource;
        private volatile Object nativeBattleTamedCompanion;
        private Object nativeZombieCorpseActor;
        private RuntimeInventory nativeZombieCorpseBag;
        transient Object nativeSourceNpcFactoryMemory;
        transient Object nativeSourceNpcFirstView;

        Snapshot(long l, int n, int n2, int n3, int n4, String string, int n5, int n6, int n7, boolean bl, boolean bl2, boolean bl3, int n8, int n9, int n10, String string2, String string3, String string4) {
            this.objectId = l;
            this.npcId = n;
            this.x = n2;
            this.y = n3;
            this.gfx = n4;
            this.name = string;
            this.level = n5;
            this.hp = n6;
            this.maxHp = n7;
            this.alive = bl;
            this.finalBoss = bl2;
            this.activeAi = bl3;
            this.currentMp = n8;
            this.maximumMp = n9;
            this.intelligence = n10;
            this.identitySource = string2;
            this.ordinaryWaveSource = string3;
            this.placementSource = string4;
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

        public boolean finalBoss() {
            return this.finalBoss;
        }

        public boolean activeAi() {
            return this.activeAi;
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

        public String identitySource() {
            return this.identitySource;
        }

        public String ordinaryWaveSource() {
            return this.ordinaryWaveSource;
        }

        public String placementSource() {
            return this.placementSource;
        }

        Snapshot(long l, int n, int n2, int n3, int n4, String string, int n5, int n6, int n7, boolean bl, boolean bl2, boolean bl3, int n8, int n9, int n10, String string2, String string3, String string4, Object object) {
            this.objectId = l;
            this.npcId = n;
            this.x = n2;
            this.y = n3;
            this.gfx = n4;
            this.name = string;
            this.level = n5;
            this.hp = n6;
            this.maxHp = n7;
            this.alive = bl;
            this.finalBoss = bl2;
            this.activeAi = bl3;
            this.currentMp = n8;
            this.maximumMp = n9;
            this.intelligence = n10;
            this.identitySource = string2;
            this.ordinaryWaveSource = string3;
            this.placementSource = string4;
            this.nativeSourceNpcFactoryMemory = object;
        }

        Snapshot(long l, int n, int n2, int n3, int n4, String string, int n5, int n6, int n7, boolean bl, boolean bl2, boolean bl3, int n8, int n9, int n10, String string2, String string3, String string4, Object object, Object object2) {
            this(l, n, n2, n3, n4, string, n5, n6, n7, bl, bl2, bl3, n8, n9, n10, string2, string3, string4, object);
            this.nativeSourceNpcFirstView = object2;
        }
    }

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

    @AsRecord(components = {"attackerObjectId", "target", "hits", "expired"})
    static final class PoisonUpdate {
        private final long attackerObjectId;
        private final Snapshot target;
        private final List<Hit> hits;
        private final boolean expired;
        volatile Object nativeSourceArenaPoisonProof;

        PoisonUpdate(long l, Snapshot snapshot, List<Hit> list, boolean bl) {
            if (l <= 0L || snapshot == null) {
                throw new IllegalArgumentException("invalid Time Attack poison update");
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
    private volatile Object sourceProcEncounterGeneration;

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
        NativeProcHealingEncounter.retire(this);
        LiToGmNpcs.clear(this);
        this.actors.clear();
        this.corpses.clear();
        this.arena = null;
        this.phase = Phase.INACTIVE;
        this.ordinaryFragmentIndex = -1;
        this.selectedDragon = null;
        this.dragonStageSpawned = false;
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
        if (this.phase != Phase.ORDINARY_DOCUMENTED_FRAGMENT && this.phase != Phase.FINAL_BOSSES) {
            return (List<Snapshot>) (List<?>) NativeSourceNpcFirstView.owner(List.of(), this);
        }
        return (List<Snapshot>) (List<?>) NativeSourceNpcFirstView.owner(this.actors.stream().filter(Monster::alive).map(Monster::snapshot).toList(), this);
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
        return NativeSourcePoisonNpc.timeDamage(this, target, amount, poisonDamage);
    }

    Hit nativeSourcePoisonOriginalApplyDamage(Monster target, int amount, boolean poisonDamage) {
        if (LiToGmNpcs.active(this)) {
            return LiToGmNpcs.damage(this, target, amount, poisonDamage);
        }
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
            PoisonUpdate update = new PoisonUpdate(attackerObjectId, finalTarget,
                    hits, poll.expired());
                NativeSourceArenaPoison.stamp(update, target);
                updates.add(update);
        }
        return List.copyOf(updates);
    }

    synchronized Capture capture(long objectId) {
        if (LiToGmNpcs.active(this)) {
            return LiToGmNpcs.capture(this, objectId);
        }
        if (!this.canAttack(objectId)) {
            return null;
        }
        Monster monster2 = this.actors.stream().filter(monster -> monster.objectId == objectId).findFirst().orElse(null);
        if (monster2 == null || monster2.finalBoss) {
            return null;
        }
        Snapshot snapshot = monster2.snapshot();
        this.actors.remove(monster2);
        int n = (int)this.actors.stream().filter(Monster::alive).count();
        if (n != 0) {
            return new Capture(snapshot, n, List.of());
        }
        if (this.phase != Phase.ORDINARY_DOCUMENTED_FRAGMENT) {
            return null;
        }
        int n2 = this.ordinaryFragmentIndex + 1;
        if (n2 < this.selectedOrdinaryFragments.size()) {
            List<Snapshot> list = this.spawnOrdinaryFragment(n2);
            return new Capture(snapshot, list.size(), list);
        }
        List<Snapshot> list = this.spawnFinalBosses();
        return new Capture(snapshot, list.size(), list);
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
        return this.resurrectCorpse(objectId, nowNanos, 4);
    }

    synchronized Resurrection resurrectCorpse(long objectId, long nowNanos, int hpDivisor) {
        Snapshot snapshot = this.findCorpse(objectId, nowNanos);
        if (hpDivisor != 1 && hpDivisor != 4 || snapshot == null || !BattleSpellCatalog.npcAllowsResurrection(snapshot.npcId())) {
            return (Resurrection)NativeCreateZombieLifecycle.resurrected(null, this, snapshot, 2);
        }
        Monster monster2 = this.actors.stream().filter(monster -> monster.objectId == objectId && !monster.alive()).findFirst().orElse(null);
        if (monster2 == null) {
            monster2 = new Monster(snapshot);
            this.actors.add(monster2);
        }
        Snapshot snapshot2 = monster2.snapshot();
        monster2.resurrect(Math.max(1, snapshot.maxHp() / hpDivisor));
        this.corpses.removeIf(corpse -> corpse.snapshot().objectId() == objectId);
        return (Resurrection)NativeCreateZombieLifecycle.resurrected(new Resurrection(snapshot2, monster2.snapshot()), this, snapshot, 2);
    }

    private void rememberCorpse(Snapshot snapshot, long nowNanos) {
        NativeCreateZombieLifecycle.retainCorpse(this, snapshot, 2);
        if (snapshot == null || snapshot.alive() || nowNanos < 0L) {
            return;
        }
        this.corpses.removeIf(corpse -> corpse.snapshot().objectId() == snapshot.objectId());
        this.corpses.add(new Corpse(snapshot, Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(3L))));
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
        Monster monster = this.monster(objectId);
        if (this.phase != Phase.ORDINARY_DOCUMENTED_FRAGMENT && this.phase != Phase.FINAL_BOSSES || this.arena == null || monster == null || !monster.activeAi || !NativeSourceNpcMovement.allowed(monster.ready(nowNanos), monster) || monster.blindActive(nowNanos) || monster.sleepActive(nowNanos) || monster.paralysisActive(nowNanos) || monster.shockStunActive(nowNanos) || monster.boneBreakActive(nowNanos) || monster.thunderGrabActive(nowNanos) || monster.freezeActive(nowNanos) || monster.earthBindActive(nowNanos) || Math.max(Math.abs(monster.x - x), Math.abs(monster.y - y)) != 1 || !this.arena.nativeCanStep(monster.x, monster.y, x, y)) {
            return null;
        }
        boolean bl = this.actors.stream().anyMatch(monster2 -> monster2 != monster && monster2.alive() && monster2.x == x && monster2.y == y);
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

    synchronized AiAttack aiAttackFor(long objectId, int targetX, int targetY, long nowNanos) {
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        Monster monster = this.monster(objectId);
        if (monster == null) {
            return null;
        }
        List<BattleTimeAttackMagicSkillCatalog.MobskillRow> list = BattleTimeAttackMagicSkillCatalog.rowsForNpc(monster.npcId);
        int[] nArray = new int[list.size()];
        for (int i = 0; i < nArray.length; ++i) {
            nArray[i] = threadLocalRandom.nextInt(1, 101);
        }
        BattleTimeAttackMagicSkillCatalog.SummonDefinition summonDefinition = BattleTimeAttackMagicSkillCatalog.summonForNpc(monster.npcId);
        int n3 = summonDefinition == null ? 0 : threadLocalRandom.nextInt(summonDefinition.summonRandomBound());
        return this.aiAttackFor(objectId, targetX, targetY, nowNanos, nArray, threadLocalRandom.nextInt(Integer.MAX_VALUE), n3);
    }

    synchronized AiAttack aiAttackFor(long objectId, int targetX, int targetY, long nowNanos, int magicTriggerRoll, int physicalTriggerRoll, int candidateSelector) {
        if (magicTriggerRoll < 1 || magicTriggerRoll > 100 || physicalTriggerRoll < 1 || physicalTriggerRoll > 100 || candidateSelector < 0) {
            throw new IllegalArgumentException("invalid Time Attack AI fixture");
        }
        Monster monster = this.monster(objectId);
        if (monster == null) {
            return null;
        }
        List<BattleTimeAttackMagicSkillCatalog.MobskillRow> list = BattleTimeAttackMagicSkillCatalog.rowsForNpc(monster.npcId);
        int[] nArray = new int[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            nArray[i] = list.get(i) instanceof BattleTimeAttackMagicSkillCatalog.PhysicalDefinition ? physicalTriggerRoll : magicTriggerRoll;
        }
        BattleTimeAttackMagicSkillCatalog.SummonDefinition summonDefinition = BattleTimeAttackMagicSkillCatalog.summonForNpc(monster.npcId);
        int n6 = summonDefinition == null ? 0 : candidateSelector % summonDefinition.summonRandomBound();
        return this.aiAttackFor(objectId, targetX, targetY, nowNanos, nArray, candidateSelector, n6);
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
                Monster scheduledMonster = monster;
                long scheduledAt = nowNanos;
                int actionId = physical.effectiveActionId();
                NativeSourceNpcMotion.facing(monster, targetX, targetY);
                scheduledMonster.schedule(scheduledAt, actionId);
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

    synchronized Snapshot nativeSourceEscapeMoveForAi(long l, int n, int n2, long l2) {
        Monster monster = this.monster(l);
        if (this.phase != Phase.ORDINARY_DOCUMENTED_FRAGMENT && this.phase != Phase.FINAL_BOSSES || this.arena == null || monster == null || !monster.activeAi || !NativeSourceNpcMovement.allowed(monster.ready(l2), monster) || NativeSourceNpcEscape.blind(monster.blindActive(l2), monster) || monster.sleepActive(l2) || monster.paralysisActive(l2) || monster.shockStunActive(l2) || monster.boneBreakActive(l2) || monster.thunderGrabActive(l2) || monster.freezeActive(l2) || monster.earthBindActive(l2) || Math.max(Math.abs(monster.x - n), Math.abs(monster.y - n2)) != 1 || !this.arena.nativeCanStep(monster.x, monster.y, n, n2)) {
            return null;
        }
        boolean bl = this.actors.stream().anyMatch(monster2 -> monster2 != monster && monster2.alive() && monster2.x == n && monster2.y == n2);
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
        if (this.phase != Phase.ORDINARY_DOCUMENTED_FRAGMENT && this.phase != Phase.FINAL_BOSSES || this.arena == null || monster == null || !monster.activeAi || !NativeSourceNpcMovement.allowed(monster.ready(l2), monster) || monster.blindActive(l2) || monster.sleepActive(l2) || monster.paralysisActive(l2) || monster.shockStunActive(l2) || monster.boneBreakActive(l2) || monster.thunderGrabActive(l2) || monster.freezeActive(l2) || monster.earthBindActive(l2) || Math.max(Math.abs(monster.x - n), Math.abs(monster.y - n2)) != 1 || !this.arena.nativeCanStep(monster.x, monster.y, n, n2)) {
            return null;
        }
        boolean bl = this.actors.stream().anyMatch(monster2 -> monster2 != monster && monster2.alive() && monster2.x == n && monster2.y == n2);
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
        List<BattleTimeAttackMagicSkillCatalog.MobskillRow> list = BattleTimeAttackMagicSkillCatalog.rowsForNpc(monster.npcId);
        int[] nArray = new int[list.size()];
        for (int i = 0; i < nArray.length; ++i) {
            nArray[i] = threadLocalRandom.nextInt(1, 101);
        }
        BattleTimeAttackMagicSkillCatalog.SummonDefinition summonDefinition = BattleTimeAttackMagicSkillCatalog.summonForNpc(monster.npcId);
        int n3 = summonDefinition == null ? 0 : threadLocalRandom.nextInt(summonDefinition.summonRandomBound());
        return this.aiAttackFor(l, n, n2, l2, nArray, threadLocalRandom.nextInt(Integer.MAX_VALUE), n3);
    }

    synchronized AiAttack nativeSourceViewActionAiAttackFor(long l, int n, int n2, long l2, int n3, int n4, int n5, Object object) {
        if (!NativeSourceNpcViewAction.allowed(this, l, object)) {
            return null;
        }
        if (n3 < 1 || n3 > 100 || n4 < 1 || n4 > 100 || n5 < 0) {
            throw new IllegalArgumentException("invalid Time Attack AI fixture");
        }
        Monster monster = this.monster(l);
        if (monster == null) {
            return null;
        }
        List<BattleTimeAttackMagicSkillCatalog.MobskillRow> list = BattleTimeAttackMagicSkillCatalog.rowsForNpc(monster.npcId);
        int[] nArray = new int[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            nArray[i] = list.get(i) instanceof BattleTimeAttackMagicSkillCatalog.PhysicalDefinition ? n4 : n3;
        }
        BattleTimeAttackMagicSkillCatalog.SummonDefinition summonDefinition = BattleTimeAttackMagicSkillCatalog.summonForNpc(monster.npcId);
        int n6 = summonDefinition == null ? 0 : n5 % summonDefinition.summonRandomBound();
        return this.aiAttackFor(l, n, n2, l2, nArray, n5, n6);
    }

    synchronized AiAttack nativeSourceViewActionAiAttackForActNoFixture(long l, int n, int n2, long l2, int n3, int n4, Object object) {
        if (!NativeSourceNpcViewAction.allowed(this, l, object)) {
            return null;
        }
        if (n3 < 0 || n4 < 0) {
            throw new IllegalArgumentException("invalid Time Attack actNo fixture");
        }
        Monster monster = this.monster(l);
        List list = monster == null ? List.of() : BattleTimeAttackMagicSkillCatalog.rowsForNpc(monster.npcId);
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
        BattleTimeAttackMagicSkillCatalog.SummonDefinition summonDefinition = BattleTimeAttackMagicSkillCatalog.summonForNpc(monster.npcId);
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
        if (this.phase != Phase.ORDINARY_DOCUMENTED_FRAGMENT && this.phase != Phase.FINAL_BOSSES || this.arena == null || monster == null || !monster.activeAi || !NativeSourceNpcMovement.allowed(monster.ready(l2), monster) || monster.blindActive(l2) || monster.sleepActive(l2) || monster.paralysisActive(l2) || monster.shockStunActive(l2) || monster.boneBreakActive(l2) || monster.thunderGrabActive(l2) || monster.freezeActive(l2) || monster.earthBindActive(l2) || Math.max(Math.abs(monster.x - n), Math.abs(monster.y - n2)) != 1 || !this.arena.nativeCanStep(monster.x, monster.y, n, n2)) {
            return null;
        }
        boolean bl = this.actors.stream().anyMatch(monster2 -> monster2 != monster && monster2.alive() && monster2.x == n && monster2.y == n2);
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
        List<BattleTimeAttackMagicSkillCatalog.MobskillRow> list = BattleTimeAttackMagicSkillCatalog.rowsForNpc(monster.npcId);
        int[] nArray = new int[list.size()];
        for (int i = 0; i < nArray.length; ++i) {
            nArray[i] = threadLocalRandom.nextInt(1, 101);
        }
        BattleTimeAttackMagicSkillCatalog.SummonDefinition summonDefinition = BattleTimeAttackMagicSkillCatalog.summonForNpc(monster.npcId);
        int n3 = summonDefinition == null ? 0 : threadLocalRandom.nextInt(summonDefinition.summonRandomBound());
        return this.aiAttackFor(l, n, n2, l2, nArray, threadLocalRandom.nextInt(Integer.MAX_VALUE), n3);
    }

    synchronized AiAttack nativeSourceNpcPlayerFirstAiAttackFor(long l, int n, int n2, long l2, int n3, int n4, int n5, Object object, Object object2) {
        if (!NativeSourceNpcPlayerView.current(object2)) {
            return null;
        }
        if (!NativeSourceNpcViewAction.allowed(this, l, object)) {
            return null;
        }
        if (n3 < 1 || n3 > 100 || n4 < 1 || n4 > 100 || n5 < 0) {
            throw new IllegalArgumentException("invalid Time Attack AI fixture");
        }
        Monster monster = this.monster(l);
        if (monster == null) {
            return null;
        }
        List<BattleTimeAttackMagicSkillCatalog.MobskillRow> list = BattleTimeAttackMagicSkillCatalog.rowsForNpc(monster.npcId);
        int[] nArray = new int[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            nArray[i] = list.get(i) instanceof BattleTimeAttackMagicSkillCatalog.PhysicalDefinition ? n4 : n3;
        }
        BattleTimeAttackMagicSkillCatalog.SummonDefinition summonDefinition = BattleTimeAttackMagicSkillCatalog.summonForNpc(monster.npcId);
        int n6 = summonDefinition == null ? 0 : n5 % summonDefinition.summonRandomBound();
        return this.aiAttackFor(l, n, n2, l2, nArray, n5, n6);
    }

    synchronized AiAttack nativeSourceNpcPlayerFirstAiAttackForActNoFixture(long l, int n, int n2, long l2, int n3, int n4, Object object, Object object2) {
        if (!NativeSourceNpcPlayerView.current(object2)) {
            return null;
        }
        if (!NativeSourceNpcViewAction.allowed(this, l, object)) {
            return null;
        }
        if (n3 < 0 || n4 < 0) {
            throw new IllegalArgumentException("invalid Time Attack actNo fixture");
        }
        Monster monster = this.monster(l);
        List list = monster == null ? List.of() : BattleTimeAttackMagicSkillCatalog.rowsForNpc(monster.npcId);
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
        BattleTimeAttackMagicSkillCatalog.SummonDefinition summonDefinition = BattleTimeAttackMagicSkillCatalog.summonForNpc(monster.npcId);
        int n6 = summonDefinition == null ? 0 : n4 % summonDefinition.summonRandomBound();
        return this.aiAttackFor(l, n, n2, l2, nArray, n4, n6);
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

    @AsmFinal({"nativeSourceHpViewers", "nativeSourceNpcFactoryType"})
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
        volatile int x;
        volatile int y;
        volatile int hp;
        int mp;
        int maxMp;
        final Map<Integer, Long> skillReadyNanos = new HashMap<Integer, Long>();
        final Map<Integer, Integer> skillUseCounts = new HashMap<Integer, Integer>();
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

        Monster(Template template, int n, int n2, String string, String string2) {
            this.objectId = NEXT_OBJECT_ID.getAndUpdate(l -> l >= 0x6DFFFFFFL ? 0x6D000000L : l + 1L);
            this.npcId = template.npcId();
            this.gfx = template.gfx();
            this.name = template.name();
            this.level = template.level();
            this.maxHp = template.maxHp();
            this.maxMp = template.maximumMp();
            this.mp = this.maxMp;
            this.intelligence = template.intelligence();
            this.finalBoss = true;
            this.activeAi = true;
            this.identitySource = string;
            this.waveSource = string2;
            this.x = n;
            this.y = n2;
            this.hp = this.maxHp;
            this.nativeSourceHpViewers = new NativeSourceMonsterHp.Cache();
            this.nativeSourceNpcFactoryType = NativeSourceNpcFactoryType.birth(this, null);
        }

        Monster(DocumentedOrdinaryFragment documentedOrdinaryFragment, int n, int n2, String string) {
            this.objectId = NEXT_OBJECT_ID.getAndUpdate(l -> l >= 0x6DFFFFFFL ? 0x6D000000L : l + 1L);
            this.npcId = documentedOrdinaryFragment.npcId();
            this.gfx = documentedOrdinaryFragment.gfx();
            this.name = documentedOrdinaryFragment.name();
            this.level = documentedOrdinaryFragment.level();
            this.maxHp = documentedOrdinaryFragment.maxHp();
            this.maxMp = documentedOrdinaryFragment.maximumMp();
            this.mp = this.maxMp;
            this.intelligence = documentedOrdinaryFragment.intelligence();
            this.finalBoss = false;
            this.activeAi = BattleAnimationCatalog.get().supportsTimedAction(this.gfx, 0) && (BattleAnimationCatalog.get().supportsTimedAction(this.gfx, 1) || BattleTimeAttackMagicSkillCatalog.magicForNpc(this.npcId) != null);
            this.identitySource = documentedOrdinaryFragment.source();
            this.waveSource = string;
            this.x = n;
            this.y = n2;
            this.hp = this.maxHp;
            this.nativeSourceHpViewers = new NativeSourceMonsterHp.Cache();
            this.nativeSourceNpcFactoryType = NativeSourceNpcFactoryType.birth(this, null);
        }

        Monster(SummonedTemplate summonedTemplate, int n, int n2, String string, String string2) {
            this.objectId = NEXT_OBJECT_ID.getAndUpdate(l -> l >= 0x6DFFFFFFL ? 0x6D000000L : l + 1L);
            this.npcId = summonedTemplate.npcId();
            this.gfx = summonedTemplate.gfx();
            this.name = summonedTemplate.name();
            this.level = summonedTemplate.level();
            this.maxHp = summonedTemplate.maxHp();
            this.maxMp = summonedTemplate.maximumMp();
            this.mp = this.maxMp;
            this.intelligence = summonedTemplate.intelligence();
            this.finalBoss = false;
            this.activeAi = true;
            this.identitySource = string;
            this.waveSource = string2;
            this.x = n;
            this.y = n2;
            this.hp = this.maxHp;
            this.nativeSourceHpViewers = new NativeSourceMonsterHp.Cache();
            this.nativeSourceNpcFactoryType = NativeSourceNpcFactoryType.birth(this, null);
        }

        Monster(Snapshot snapshot) {
            this.objectId = snapshot.objectId();
            this.npcId = snapshot.npcId();
            this.gfx = snapshot.gfx();
            this.name = snapshot.name();
            this.level = snapshot.level();
            this.maxHp = snapshot.maxHp();
            this.maxMp = snapshot.maximumMp();
            this.mp = snapshot.currentMp();
            this.intelligence = snapshot.intelligence();
            this.finalBoss = snapshot.finalBoss();
            this.activeAi = snapshot.activeAi();
            this.identitySource = snapshot.identitySource();
            this.waveSource = snapshot.ordinaryWaveSource();
            this.x = snapshot.x();
            this.y = snapshot.y();
            this.hp = 0;
            this.nativeSourceHpViewers = new NativeSourceMonsterHp.Cache();
            this.nativeSourceNpcFactoryType = NativeSourceNpcFactoryMemory.restore(this, null, snapshot);
        }

        Monster(Snapshot snapshot, BattleCompanion.Snapshot snapshot2, List<RuntimeInventory.Item> list) {
            this.objectId = NEXT_OBJECT_ID.getAndUpdate(l -> l >= 0x6DFFFFFFL ? 0x6D000000L : l + 1L);
            this.npcId = snapshot.npcId();
            this.gfx = snapshot.gfx();
            this.name = snapshot.name();
            this.level = snapshot.level();
            this.maxHp = snapshot.maxHp();
            this.maxMp = snapshot.maximumMp();
            this.mp = Math.max(0, Math.min(snapshot2.mp(), this.maxMp));
            this.intelligence = snapshot.intelligence();
            this.finalBoss = false;
            this.activeAi = snapshot.activeAi();
            this.identitySource = snapshot.identitySource();
            this.waveSource = snapshot.ordinaryWaveSource();
            this.x = snapshot2.x();
            this.y = snapshot2.y();
            this.hp = Math.max(1, Math.min(snapshot2.hp(), this.maxHp));
            for (RuntimeInventory.Item item : list) {
                if (!this.inventory.add(item, (long)item.quantity())) {
                    throw new IllegalStateException("released Time Attack monster inventory exceeds native capacity");
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
            return new Snapshot(this.objectId, this.npcId, this.x, this.y, this.gfx, this.name, this.level, this.hp, this.maxHp, this.hp > 0, this.finalBoss, this.activeAi, this.mp, this.maxMp, this.intelligence, this.identitySource, this.waveSource, BattleTimeAttackEncounter.PLACEMENT_SOURCE, NativeSourceNpcFactoryMemory.carry(this), object);
        }

        Damage damage(int amount) {
            return this.damage(amount, true);
        }

        Damage poisonDamage(int amount) {
            return damage(amount, false);
        }

        private Damage damage(int amount, boolean earthBindBlocks) {
            if (this.hp <= 0 || amount <= 0 || NativeCombatAttacker.earth(earthBindBlocks, this) && this.earthBindActive(System.nanoTime())) {
                return null;
            }
            Snapshot snapshot = this.snapshot();
            this.hp = Math.max(0, this.hp - amount);
            this.sleepExpiresAtNanos = 0L;
            if (this.hp == 0) {
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
                this.paralysisExpiresAtNanos = 0L;
                NativeSourceTargetNpc.writeShock(this, 0L);
                NativeSourceSpecialNpc.writeBone(this, 0L);
                this.thunderGrabExpiresAtNanos = 0L;
                NativeSourceTargetNpc.writeFreeze(this, 0L);
                this.earthBindExpiresAtNanos = 0L;
                this.hasteExpiresAtNanos = 0L;
                this.slowExpiresAtNanos = 0L;
                this.weaponBreakStacks = 0;
                this.curePoison();
                this.joyOfPainStatus.clear();
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
                throw new IllegalStateException("invalid Time Attack resurrection");
            }
            Monster recipient = this;
            int n2 = Math.min(this.maxHp, restoredHp);
            NativeCombatAttacker.advance(this);
            recipient.hp = n2;
            this.nextActionNanos = 0L;
            this.skillReadyNanos.clear();
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
            if (!this.alive() || this.finalBoss || nowNanos < 0L || durationSeconds <= 0 || this.freezeActive(nowNanos)) {
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
            long l2 = TimeUnit.MILLISECONDS.toNanos(NativeSourceNpcBaseSpeeds.npcMillis(this.gfx, action, this.hasteActive(nowNanos), this.slowActive(nowNanos), this.windShackleActive(nowNanos), this));
            this.nextActionNanos = Math.addExact(nowNanos, l2);
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

        void scheduleVisual(long nowNanos, BattleTimeAttackMagicSkillCatalog.VisualDefinition skill) {
            if (!this.visualReady(skill, nowNanos)) {
                throw new IllegalStateException("Time Attack visual skill was not ready");
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
            this.skillReadyNanos.put(skill.actNo(), Math.addExact(nowNanos, TimeUnit.MILLISECONDS.toNanos(skill.reuseDelayMillis())));
        }

        boolean groundReady(BattleTimeAttackMagicSkillCatalog.GroundDefinition skill,
                            long nowNanos) {
            return skill != null && skill.npcId() == npcId
                    && mp >= skill.mpCost()
                    && nowNanos >= skillReadyNanos.getOrDefault(skill.actNo(), 0L);
        }

        void scheduleGround(long nowNanos, BattleTimeAttackMagicSkillCatalog.GroundDefinition skill) {
            if (!this.groundReady(skill, nowNanos)) {
                throw new IllegalStateException("Time Attack ground skill was not ready");
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
            this.skillReadyNanos.put(skill.actNo(), this.nextActionNanos);
        }

        void scheduleMagic(long nowNanos, BattleTimeAttackMagicSkillCatalog.Definition skill) {
            if (!this.magicReady(skill, nowNanos)) {
                throw new IllegalStateException("Time Attack magic was not ready");
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
            this.skillReadyNanos.put(skill.actNo(), Math.addExact(nowNanos, TimeUnit.MILLISECONDS.toNanos(skill.reuseDelayMillis())));
        }

        boolean summonReady(
                BattleTimeAttackMagicSkillCatalog.SummonDefinition summon) {
            return summon != null && summon.npcId() == npcId
                    && skillUseCounts.getOrDefault(summon.actNo(), 0)
                        < summon.triggerCount();
        }

        void scheduleSummon(long nowNanos, BattleTimeAttackMagicSkillCatalog.SummonDefinition summon) {
            if (!this.summonReady(summon)) {
                throw new IllegalStateException("Time Attack summon was not ready");
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
            this.hp = NativeSourceNpcMagicEffects.capped(this.maxHp, this.hp, n);
            Snapshot snapshot2 = this.snapshot();
            return new Healing(snapshot, snapshot2, snapshot2.hp() - snapshot.hp());
        }
    }
}
