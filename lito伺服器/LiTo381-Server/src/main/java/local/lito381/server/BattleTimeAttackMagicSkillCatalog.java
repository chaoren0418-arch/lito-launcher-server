package local.lito381.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Selected-source {@code mobskill}/{@code skills} rows used by Tournament actors. */
final class BattleTimeAttackMagicSkillCatalog {
    static final String SOURCE = "selected-380_db-mobskill+skills+npc";

    /** Common retained {@code mobskill} trigger columns, ordered by actNo. */
    interface MobskillRow {
        int npcId();
        int actNo();
        int triggerChance();
        int triggerRange();

        default boolean triggerDistance(int distance) {
            if (distance < 0) throw new IllegalArgumentException("negative target distance");
            return triggerRange() < 0
                    ? distance <= Math.abs(triggerRange())
                    : distance >= triggerRange();
        }

        default boolean triggerRoll(int oneBasedRoll) {
            if (oneBasedRoll < 1 || oneBasedRoll > 100) {
                throw new IllegalArgumentException("trigger roll outside percentile range");
            }
            // Retained L1MobSkillUse.isSkillUseble uses chance < TriRnd.
            return oneBasedRoll < triggerChance();
        }
    }

    enum Targeting {
        DIRECTIONAL,
        TARGET_RADIUS,
        CASTER_RADIUS,
        CASTER_SCREEN
    }

    enum GroundPattern {
        TARGET_SQUARE
    }

    /**
     * Same-family executable ground-effect contract for a selected mobskill row.
     * The selected action remains separately auditable when its sprite lacks it.
     */
    record GroundDefinition(
            int npcId, int actNo, int triggerChance, int triggerRange,
            int leverage, int skillId, int actionDelayMillis,
            int mpCost, int actionId, int sourceActionId, int effectNpcId, int effectGfx,
            int maximumRange, GroundPattern pattern, int halfWidth,
            int durationMillis, int pulseIntervalMillis, int pulseCount,
            int fixedDamage, int attribute, String behaviorSource)
            implements MobskillRow {
        GroundDefinition {
            if (npcId <= 0 || actNo < 0 || triggerChance <= 0
                    || triggerChance > 100 || triggerRange == 0 || leverage <= 0
                    || skillId <= 0 || actionDelayMillis <= 0 || mpCost < 0
                    || actionId < 0 || actionId > 0xFF
                    || sourceActionId < 0 || sourceActionId > 0xFF
                    || effectNpcId <= 0 || effectGfx <= 0 || effectGfx > 0xFFFF
                    || maximumRange <= 0 || pattern == null || halfWidth < 0
                    || durationMillis <= 0 || pulseIntervalMillis <= 0
                    || pulseCount <= 0 || fixedDamage <= 0
                    || attribute != BattleSpellCatalog.ATTRIBUTE_FIRE
                    || behaviorSource == null || behaviorSource.isBlank()) {
                throw new IllegalArgumentException("invalid Time Attack ground row");
            }
            if ((long) pulseIntervalMillis * pulseCount > durationMillis) {
                throw new IllegalArgumentException("ground pulses exceed effect duration");
            }
        }

        boolean castDistance(int distance) {
            if (distance < 0) throw new IllegalArgumentException("negative cast distance");
            return distance <= maximumRange;
        }

        List<ArenaMapCatalog.Point> points(
                int targetX, int targetY, ArenaMapCatalog.Arena arena) {
            if (arena == null) return List.of();
            List<ArenaMapCatalog.Point> result = new ArrayList<>();
            for (int y = targetY - halfWidth; y <= targetY + halfWidth; y++) {
                for (int x = targetX - halfWidth; x <= targetX + halfWidth; x++) {
                    if (arena.contains(x, y)) result.add(new ArenaMapCatalog.Point(x, y));
                }
            }
            return List.copyOf(result);
        }
    }

    record Definition(
            int npcId, int actNo, int triggerChance, int triggerRange,
            int leverage, int skillId, int actionDelayMillis,
            Targeting targeting, int mpCost, int reuseDelayMillis,
            int actionId, int effectGfx, int maximumRange, int areaRadius,
            int baseDamage, int diceCount, int dieSides, int attribute,
            int sourceActionId)
            implements MobskillRow {
        Definition(
                int npcId, int actNo, int triggerChance, int triggerRange,
                int leverage, int skillId, int actionDelayMillis,
                Targeting targeting, int mpCost, int reuseDelayMillis,
                int actionId, int effectGfx, int maximumRange, int areaRadius,
                int baseDamage, int diceCount, int dieSides, int attribute) {
            this(npcId, actNo, triggerChance, triggerRange, leverage, skillId,
                    actionDelayMillis, targeting, mpCost, reuseDelayMillis,
                    actionId, effectGfx, maximumRange, areaRadius, baseDamage,
                    diceCount, dieSides, attribute, actionId);
        }

        Definition {
            if (npcId <= 0 || actNo < 0 || triggerChance <= 0
                    || triggerChance > 100 || triggerRange == 0
                    || leverage <= 0 || skillId <= 0 || actionDelayMillis <= 0
                    || targeting == null || mpCost < 0 || reuseDelayMillis < 0
                    || actionId < 0 || actionId > 0xFF
                    || sourceActionId < 0 || sourceActionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF
                    || maximumRange < 0 || areaRadius < 0 || baseDamage < 0
                    || diceCount <= 0 || dieSides <= 0
                    || !switch (attribute) {
                        case BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED,
                             BattleSpellCatalog.ATTRIBUTE_EARTH,
                             BattleSpellCatalog.ATTRIBUTE_FIRE,
                             BattleSpellCatalog.ATTRIBUTE_WATER,
                             BattleSpellCatalog.ATTRIBUTE_WIND,
                             BattleSpellCatalog.ATTRIBUTE_NONE -> true;
                        default -> false;
                    }) {
                throw new IllegalArgumentException("invalid Time Attack magic row");
            }
            boolean validTargeting = switch (targeting) {
                case DIRECTIONAL -> maximumRange > 0 && areaRadius == 0;
                case TARGET_RADIUS -> maximumRange > 0 && areaRadius > 0;
                case CASTER_RADIUS -> maximumRange == 0 && areaRadius > 0;
                case CASTER_SCREEN -> maximumRange == 0 && areaRadius == 0;
            };
            if (!validTargeting) {
                throw new IllegalArgumentException(
                        "Time Attack magic targeting disagrees with skill " + skillId);
            }
        }

        boolean drainsLife() {
            return skillId == 28;
        }

        boolean castDistance(int distance) {
            if (distance < 0) throw new IllegalArgumentException("negative cast distance");
            return switch (targeting) {
                case DIRECTIONAL, TARGET_RADIUS -> distance <= maximumRange;
                case CASTER_RADIUS, CASTER_SCREEN -> true;
            };
        }
    }

    /**
     * Executable Type2 row whose selected skill has an action/effect contract
     * but no damage, duration or retained special-status implementation.
     */
    record VisualDefinition(
            int npcId, int actNo, int triggerChance, int triggerRange,
            int leverage, int skillId, int actionDelayMillis,
            int mpCost, int reuseDelayMillis, int actionId, int effectGfx,
            int maximumRange, int durationMillis, String behaviorSource)
            implements MobskillRow {
        VisualDefinition {
            if (npcId <= 0 || actNo < 0 || triggerChance <= 0
                    || triggerChance > 100 || triggerRange == 0
                    || leverage <= 0 || skillId <= 0 || actionDelayMillis <= 0
                    || mpCost < 0 || reuseDelayMillis < 0
                    || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF
                    || maximumRange <= 0 || durationMillis != 0
                    || behaviorSource == null || behaviorSource.isBlank()) {
                throw new IllegalArgumentException("invalid Time Attack visual row");
            }
        }

        boolean castDistance(int distance) {
            if (distance < 0) throw new IllegalArgumentException("negative cast distance");
            return distance <= maximumRange;
        }
    }

    /**
     * A selected row whose complete retained precondition chain proves that it
     * can never execute. It remains in the independent actNo trigger sequence,
     * but is closed evidence rather than an unknown implementation gap.
     * {@code maximumRange == 0} means that the selected skills table has no
     * template at all and the retained {@code L1SkillUse.checkUseSkill}
     * immediately returns false.
     */
    record InertDefinition(
            int npcId, int actNo, int triggerChance, int triggerRange,
            int skillId, int maximumRange, String reason) implements MobskillRow {
        InertDefinition {
            if (npcId <= 0 || actNo < 0 || triggerChance <= 0
                    || triggerChance > 100 || triggerRange <= 0
                    || skillId <= 0 || maximumRange < 0
                    || reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("invalid inert Time Attack row");
            }
            boolean rangeIntersectionEmpty = maximumRange > 0
                    && triggerRange > maximumRange;
            boolean missingTemplate = maximumRange == 0
                    && reason.equals("selected-skill-template-absent"
                            + "+retained-L1SkillUse-checkUseSkill-false");
            if (!rangeIntersectionEmpty && !missingTemplate) {
                throw new IllegalArgumentException("unproved inert Time Attack row");
            }
        }

        boolean executableDistance(int distance) {
            if (distance < 0) throw new IllegalArgumentException("negative target distance");
            return maximumRange > 0
                    && triggerDistance(distance) && distance <= maximumRange;
        }
    }

    record PhysicalDefinition(
            int npcId, int actNo, int triggerChance, int triggerRange,
            int attackRange, int leverage, int actionOverride,
            int areaWidth, int areaHeight)
            implements MobskillRow {
        PhysicalDefinition(
                int npcId, int actNo, int triggerChance, int triggerRange,
                int attackRange, int leverage, int actionOverride) {
            this(npcId, actNo, triggerChance, triggerRange,
                    attackRange, leverage, actionOverride, 0, 0);
        }

        PhysicalDefinition {
            if (npcId <= 0 || actNo < 0 || triggerChance <= 0
                    || triggerChance > 100 || triggerRange == 0
                    || attackRange <= 0 || leverage <= 0
                    || actionOverride < 0 || actionOverride > 0xFF
                    || areaWidth < 0 || areaHeight < 0
                    || areaHeight == 0 && areaWidth != 0) {
                throw new IllegalArgumentException("invalid Time Attack physical row");
            }
        }

        int effectiveActionId() {
            // Retained L1MobSkillUse only calls setActId when ActId > 0.
            return actionOverride > 0
                    ? actionOverride : BattleActionCodes.ATTACK;
        }

        boolean attackDistance(int distance) {
            if (distance < 0) throw new IllegalArgumentException("negative attack distance");
            return distance <= attackRange;
        }

        boolean areaAttack() {
            return areaHeight > 0;
        }

        /** Exact selected World.getVisibleBoxObjects rotated-box geometry. */
        boolean areaContains(
                int actorX, int actorY, int heading, int targetX, int targetY) {
            if (!areaAttack() || heading < 0 || heading > 7) return false;
            int x = targetX - actorX;
            int y = targetY - actorY;
            if (x == 0 && y == 0) return true;
            int distance = Math.max(Math.abs(x), Math.abs(y));
            if (distance > areaHeight && distance > areaWidth) return false;
            int[] headingRotate = {6, 7, 0, 1, 2, 3, 4, 5};
            double angle = headingRotate[heading] * Math.PI / 4.0;
            int rotatedX = (int) Math.round(x * Math.cos(angle) + y * Math.sin(angle));
            int rotatedY = (int) Math.round(-x * Math.sin(angle) + y * Math.cos(angle));
            return rotatedX > 0 && distance <= areaHeight
                    && rotatedY >= -areaWidth && rotatedY <= areaWidth;
        }
    }

    record SummonDefinition(
            int npcId, int actNo, int triggerChance, int triggerRange,
            int triggerCount, int summonNpcId, int summonMinimum,
            int summonRandomBound, int actionDelayMillis,
            int actionId, int effectGfx) implements MobskillRow {
        SummonDefinition {
            if (npcId <= 0 || actNo < 0 || triggerChance <= 0
                    || triggerChance > 100 || triggerRange == 0
                    || triggerCount <= 0 || summonNpcId <= 0
                    || summonMinimum <= 0 || summonRandomBound <= 0
                    || actionDelayMillis <= 0 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF) {
                throw new IllegalArgumentException("invalid Time Attack summon row");
            }
        }

        int summonCount(int zeroBasedRoll) {
            if (zeroBasedRoll < 0 || zeroBasedRoll >= summonRandomBound) {
                throw new IllegalArgumentException("summon roll outside source bound");
            }
            // Retained L1MobSkillUse.summon: nextInt(SummonMax) + SummonMin.
            return zeroBasedRoll + summonMinimum;
        }
    }

    private static final List<Definition> MAGIC = List.of(
            // mobskill(45812,0): Type2, TriRnd100, TriRange-8, leverage default10,
            // selected skill38 Cone of Cold and npc atk_magic_speed 1440 ms.
            standard(45812, 0, 100, -8, 10,
                    BattleSpellCatalog.CONE_OF_COLD, 1_440,
                    Targeting.DIRECTIONAL),
            // mobskill(45584,0): Type2, TriRnd20, TriRange-3, leverage40,
            // selected skill62 Earthquake and npc sub_magic_speed 1520 ms.
            standard(45584, 0, 20, -3, 40,
                    BattleSpellCatalog.EARTHQUAKE, 1_520,
                    Targeting.CASTER_RADIUS),
            // mobskill(45610,0) + selected custom skill10019. area=-1 means
            // every target visible from the caster; target=none emits TYPE_NODIR.
            new Definition(45610, 0, 30, -15, 30, 10019, 1_880,
                    Targeting.CASTER_SCREEN, 5, 0, 18, 2483,
                    0, 0, 100, 10, 10,
                    BattleSpellCatalog.ATTRIBUTE_EARTH),
            // mobskill(45601,0) + skill28 Vampiric Touch. Because target is
            // "attack", retained L1MobSkillUse uses npc atk_magic_speed 900.
            new Definition(45601, 0, 20, -3, 50, 28, 900,
                    Targeting.DIRECTIONAL, 12, 50, 18, 236,
                    4, 0, 12, 3, 10,
                    BattleSpellCatalog.ATTRIBUTE_WATER),
            // mobskill(45601,1) + custom skill10056; target=none selects the
            // npc sub_magic_speed 960 ms and the skill's native action 19.
            new Definition(45601, 1, 60, -6, 30, 10056, 960,
                    Targeting.CASTER_RADIUS, 5, 3_000, 19, 245,
                    0, 4, 100, 10, 10,
                    BattleSpellCatalog.ATTRIBUTE_FIRE),
            // mobskill(45650,0) + custom skill10000; attr0 is the retained
            // unspecified element, not ATTRIBUTE_NONE(16). The skills row's
            // action18 conflicts with the same source spr_action rows for
            // gfx2803 (0/1/19 only), so preserve 18 as sourceActionId while
            // emitting its sprite-valid native magic action19.
            new Definition(45650, 0, 30, -6, 20, 10000, 1_800,
                    Targeting.CASTER_RADIUS, 5, 0, 19, 2806,
                    0, 6, 100, 10, 10,
                    BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, 18),

            // Same-family dragon rows from the selected 380 database. The
            // documented LiTo event proves the optional random-dragon stage;
            // these rows supply combat-compatible identity/skill data, not a
            // claim of recovered event-only tuning.
            // Antharas act0: attack target, range4, area6, action30/gfx4156.
            new Definition(45682, 0, 60, -5, 40, 31001, 2_240,
                    Targeting.TARGET_RADIUS, 5, 0, 30, 4156,
                    4, 6, 200, 8, 10,
                    BattleSpellCatalog.ATTRIBUTE_WATER),
            // Antharas act1: attack target, range20, no area.
            new Definition(45682, 1, 10, -3, 40, 10027, 2_240,
                    Targeting.DIRECTIONAL, 5, 0, 30, 4156,
                    20, 0, 1_500, 50, 10,
                    BattleSpellCatalog.ATTRIBUTE_EARTH),
            // Antharas act2: attack target, range6, no area.
            new Definition(45682, 2, 20, -3, 40, 31004, 2_240,
                    Targeting.DIRECTIONAL, 5, 0, 25, 129,
                    6, 0, 75, 3, 5,
                    BattleSpellCatalog.ATTRIBUTE_EARTH),
            // Antharas act3: target=none, caster-centered area17.
            new Definition(45682, 3, 60, -5, 40, 31002, 2_240,
                    Targeting.CASTER_RADIUS, 5, 0, 19, 2510,
                    0, 17, 150, 3, 5,
                    BattleSpellCatalog.ATTRIBUTE_EARTH),
            // Fafurion act0: attack target, range4, area6.
            new Definition(45683, 0, 60, -5, 29, 33001, 1_600,
                    Targeting.TARGET_RADIUS, 5, 0, 30, 4163,
                    4, 6, 200, 8, 10,
                    BattleSpellCatalog.ATTRIBUTE_WATER),
            // Fafurion act1: attack target, range20, no area.
            new Definition(45683, 1, 10, -3, 29, 10029, 1_600,
                    Targeting.DIRECTIONAL, 5, 0, 30, 4163,
                    20, 0, 1_500, 50, 10,
                    BattleSpellCatalog.ATTRIBUTE_WATER),
            // Fafurion act2: target=none, caster-centered area17.
            new Definition(45683, 2, 60, -5, 29, 33002, 1_600,
                    Targeting.CASTER_RADIUS, 5, 0, 19, 2510,
                    0, 17, 200, 3, 5,
                    BattleSpellCatalog.ATTRIBUTE_FIRE),
            // Fafurion act3: attack target, range4, area3.
            new Definition(45683, 3, 45, -4, 29, 33003, 1_600,
                    Targeting.TARGET_RADIUS, 5, 0, 18, 2510,
                    4, 3, 75, 3, 5,
                    BattleSpellCatalog.ATTRIBUTE_WIND),

            // Valakas act0 deliberately references selected skill31001 even
            // though the row label incorrectly names Antharas.
            new Definition(45684, 0, 60, -5, 40, 31001, 1_840,
                    Targeting.TARGET_RADIUS, 5, 0, 30, 4156,
                    4, 6, 200, 8, 10,
                    BattleSpellCatalog.ATTRIBUTE_WATER),
            // Valakas act1: attack target, range20, no area.
            new Definition(45684, 1, 10, -3, 40, 10028, 1_840,
                    Targeting.DIRECTIONAL, 5, 0, 30, 4160,
                    20, 0, 1_500, 50, 10,
                    BattleSpellCatalog.ATTRIBUTE_FIRE),
            // Valakas act3: target=none, caster-centered area17.
            new Definition(45684, 3, 60, -5, 40, 30002, 1_840,
                    Targeting.CASTER_RADIUS, 5, 0, 19, 2510,
                    0, 17, 150, 3, 5,
                    BattleSpellCatalog.ATTRIBUTE_FIRE),
            // Valakas act4: attack target, range4, area3.
            new Definition(45684, 4, 45, -4, 40, 30003, 1_840,
                    Targeting.TARGET_RADIUS, 5, 0, 18, 2510,
                    4, 3, 75, 3, 5,
                    BattleSpellCatalog.ATTRIBUTE_FIRE));

    private static final List<PhysicalDefinition> PHYSICAL = List.of(
            // Quest Battle Ogre mobskill(45278,0): Type1/TriRnd30,
            // TriRange-2, Range2, AreaWidth1, AreaHeight2, leverage20 and
            // ActId30 (1-伺服器 db/380_db.sql:34296).  L1MobSkillUse:660-661
            // calls attack.setActId(30) because ActId > 0; gfx1020 carries
            // spr_action 30, matching ClassicWorldMonsterPhysicalSkillCatalog.
            new PhysicalDefinition(45278, 0, 30, -2, 2, 20, 30, 1, 2),
            // Exact selected mobskill act1 rows, both Type1/TriRnd100,
            // leverage30 and ActId0. Their Range fields are 1/2; retained
            // L1MobSkillUse leaves the ordinary attack action unchanged when
            // ActId is zero and passes leverage into L1AttackNpc.
            new PhysicalDefinition(45584, 1, 100, -1, 1, 30, 0),
            new PhysicalDefinition(45610, 1, 100, -2, 2, 30, 0),
            new PhysicalDefinition(45601, 3, 100, -1, 1, 20, 0),
            new PhysicalDefinition(45650, 1, 40, -2, 2, 30, 0),
            // Dragon Type1 rows preserve Range, AreaWidth/AreaHeight,
            // leverage and ActId exactly. AreaHeight>0 selects retained
            // World.getVisibleBoxObjects directional-box targeting.
            new PhysicalDefinition(45682, 5, 50, -3, 3, 15, 5, 3, 3),
            new PhysicalDefinition(45682, 6, 80, -3, 3, 15, 1, 0, 3),
            new PhysicalDefinition(45683, 4, 50, -3, 3, 15, 5, 3, 3),
            new PhysicalDefinition(45684, 5, 50, -3, 3, 15, 5, 3, 3),
            new PhysicalDefinition(45684, 6, 80, -3, 3, 15, 1, 0, 3));

    private static final List<SummonDefinition> SUMMON = List.of(
            // mobskill(45601,2): Type3, TriRnd5, TriRange-3, TriCount2,
            // summon 45161 with nextInt(3)+1; retained summon() emits
            // object effect 761, action19 and waits npc sub_magic_speed 960.
            new SummonDefinition(45601, 2, 5, -3, 2,
                    45161, 1, 3, 960, 19, 761));

    private static final List<GroundDefinition> GROUND = List.of(
            // selected mobskill(45684,2) + skill30004 identifies Valakas Fire
            // Prison.  The selected npc row 81157 pins gfx168. Same-family
            // executable code places a target-centred 3x3 field for ten
            // seconds and applies fixed 25 damage eight times at 1.2 seconds.
            // The selected skill action25 is absent from gfx2001; action18 is
            // both present in the selected sprite and used by the compatible
            // executable Fire Prison row, so preserve 25 as sourceActionId.
            new GroundDefinition(45684, 2, 20, -3, 40, 30004, 1_840,
                    5, 18, 25, 81157, 168, 10,
                    GroundPattern.TARGET_SQUARE, 1,
                    10_000, 1_200, 8, 25,
                    BattleSpellCatalog.ATTRIBUTE_FIRE,
                    "selected-380-db+npc81157+same-family-BLKS_FIRE_WALL"));

    private static final List<VisualDefinition> VISUAL = List.of(
            // mobskill(45682,4): Type2/TriRnd45/TriRange-4/leverage40.
            // Selected skill31003 is target=buff/target_to=3/type=128,
            // mp5/range10/action18/gfx2510 with damage, dice and duration all
            // zero. Retained L1SkillUse has no 31003 special branch, so its
            // exact executable result is target effect + caster action only.
            new VisualDefinition(45682, 4, 45, -4, 40, 31003, 2_240,
                    5, 0, 18, 2510, 10, 0,
                    "selected-380-db+retained-L1SkillUse-no-special-case"));

    private static final List<InertDefinition> INERT = List.of(
            // TriRange=+8 requires a target at least eight tiles away while
            // selected skill31001 has maximum range four. Retained
            // checkUseSkill therefore rejects the cast for every legal target.
            new InertDefinition(45682, 7, 100, 8, 31001, 4,
                    "retained-trigger-and-skill-range-intersection-empty"),
            // The selected mobskill row points to skill31000, but no selected
            // skills row exists.  Both the retained active source and the
            // independent old-source snapshot route Type2 through
            // checkUseSkill(), which returns false before any MP, motion,
            // effect, damage or reuse state when the template is null.
            new InertDefinition(45684, 7, 100, 8, 31000, 0,
                    "selected-skill-template-absent"
                            + "+retained-L1SkillUse-checkUseSkill-false"));

    /**
     * Single caster-wide reuse key. Retained L1SkillUse reserves
     * {@code skills.reuseDelay} through {@code L1SkillDelay.tryReserveSkillUse(_user, ...)}
     * (L1SkillUse.java:2675-2676) and rejects every later cast while
     * {@code _user.isSkillDelay()} (:2642, :2658). The delay is therefore owned
     * by the NPC, not by the mobskill actNo: Death Knight skill10056
     * (reuseDelay 3000) also blocks its skill28 row for three seconds.
     */
    static final int CASTER_SKILL_DELAY_KEY = -1;

    /**
     * Retained L1NpcInstance.attack (L1NpcInstance.java:894-907): at an
     * attack position a coin {@code nextInt(2) >= 1} chooses the ordinary
     * attack before any mobskill TriRnd roll. The arena AI only owns an
     * ordinary-attack branch for adjacent targets, so the coin applies there.
     */
    static boolean ordinaryAttackBranch(int distance, int coin) {
        if (distance < 0) throw new IllegalArgumentException("negative target distance");
        if (coin < 0 || coin > 1) {
            throw new IllegalArgumentException("ordinary attack coin outside nextInt(2)");
        }
        return distance <= 1 && coin >= 1;
    }

    private BattleTimeAttackMagicSkillCatalog() {}

    static Definition magicForNpc(int npcId) {
        return MAGIC.stream().filter(row -> row.npcId() == npcId)
                .findFirst().orElse(null);
    }

    static List<Definition> magicForNpcDefinitions(int npcId) {
        return MAGIC.stream().filter(row -> row.npcId() == npcId).toList();
    }

    static PhysicalDefinition physicalForNpc(int npcId) {
        return PHYSICAL.stream().filter(row -> row.npcId() == npcId)
                .findFirst().orElse(null);
    }

    static List<PhysicalDefinition> physicalForNpcDefinitions(int npcId) {
        return PHYSICAL.stream().filter(row -> row.npcId() == npcId).toList();
    }

    static SummonDefinition summonForNpc(int npcId) {
        return SUMMON.stream().filter(row -> row.npcId() == npcId)
                .findFirst().orElse(null);
    }

    static List<SummonDefinition> summonForNpcDefinitions(int npcId) {
        return SUMMON.stream().filter(row -> row.npcId() == npcId).toList();
    }

    static List<MobskillRow> rowsForNpc(int npcId) {
        List<MobskillRow> rows = new ArrayList<>();
        rows.addAll(magicForNpcDefinitions(npcId));
        rows.addAll(physicalForNpcDefinitions(npcId));
        rows.addAll(summonForNpcDefinitions(npcId));
        rows.addAll(groundForNpcDefinitions(npcId));
        rows.addAll(visualForNpcDefinitions(npcId));
        rows.addAll(inertForNpcDefinitions(npcId));
        rows.sort(Comparator.comparingInt(MobskillRow::actNo));
        return List.copyOf(rows);
    }

    static List<Definition> magicDefinitions() {
        return MAGIC;
    }

    static List<PhysicalDefinition> physicalDefinitions() {
        return PHYSICAL;
    }

    static List<SummonDefinition> summonDefinitions() {
        return SUMMON;
    }

    static GroundDefinition groundForNpc(int npcId) {
        return GROUND.stream().filter(row -> row.npcId() == npcId)
                .findFirst().orElse(null);
    }

    static List<GroundDefinition> groundForNpcDefinitions(int npcId) {
        return GROUND.stream().filter(row -> row.npcId() == npcId).toList();
    }

    static List<GroundDefinition> groundDefinitions() {
        return GROUND;
    }

    static VisualDefinition visualForNpc(int npcId) {
        return VISUAL.stream().filter(row -> row.npcId() == npcId)
                .findFirst().orElse(null);
    }

    static List<VisualDefinition> visualForNpcDefinitions(int npcId) {
        return VISUAL.stream().filter(row -> row.npcId() == npcId).toList();
    }

    static List<VisualDefinition> visualDefinitions() {
        return VISUAL;
    }

    static List<InertDefinition> inertForNpcDefinitions(int npcId) {
        return INERT.stream().filter(row -> row.npcId() == npcId).toList();
    }

    static List<InertDefinition> inertDefinitions() {
        return INERT;
    }

    private static Definition standard(
            int npcId, int actNo, int triggerChance, int triggerRange,
            int leverage, int skillId, int actionDelayMillis,
            Targeting expectedTargeting) {
        BattleSpellCatalog.AttackSpell attack =
                BattleSpellCatalog.attackSpell(skillId);
        if (attack != null) {
            if (expectedTargeting != Targeting.DIRECTIONAL) {
                throw new IllegalArgumentException("attack targeting mismatch " + skillId);
            }
            return new Definition(npcId, actNo, triggerChance, triggerRange,
                    leverage, skillId, actionDelayMillis, expectedTargeting,
                    attack.mpCost(), attack.reuseDelayMillis(), attack.actionId(),
                    attack.effectGfx(), attack.maximumRange(), 0,
                    attack.baseDamage(), attack.diceCount(), attack.dieSides(),
                    attack.attribute());
        }
        BattleSpellCatalog.AreaAttackSpell area =
                BattleSpellCatalog.areaAttackSpell(skillId);
        if (area == null || expectedTargeting != Targeting.CASTER_RADIUS
                || area.maximumRange() != 0 || area.areaRadius() <= 0) {
            throw new IllegalArgumentException("area targeting mismatch " + skillId);
        }
        return new Definition(npcId, actNo, triggerChance, triggerRange,
                leverage, skillId, actionDelayMillis, expectedTargeting,
                area.mpCost(), area.reuseDelayMillis(), area.actionId(),
                area.effectGfx(), 0, area.areaRadius(),
                area.baseDamage(), area.diceCount(), area.dieSides(),
                area.attribute());
    }
}
