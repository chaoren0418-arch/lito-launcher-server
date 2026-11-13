package local.lito381.server;

import static local.lito381.server.NativeDeNpcMobSkill.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * T135 (Issue #386, G15 group 2): Source {@code model/L1MobSkillUse.java} row selection and the
 * actor-owned use-count / reuse-delay state of pets and summons, ported from candidate {@code
 * 046c58c5…}.
 *
 * <p>Selection is pure: the scene, the random source and the effects are scripted through {@link
 * NativeCloseoutMobSkillSelection.Ports}. Every random draw is supplied explicitly and an
 * unexpected draw fails the test, so the number and the bound of the draws are part of each
 * assertion.
 */
final class NativeCloseoutMobSkillSelectionTest {

  // ---------------------------------------------------------------- scripted scene

  static final class Actor {
    final String name;
    boolean npc, player, pet, dead, ghost;
    boolean hated = true, sight = true;
    int family, hp = 100, maxHp = 100, distance = 1;

    Actor(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  static Actor monster(String name, int family) {
    var actor = new Actor(name);
    actor.npc = true;
    actor.family = family;
    return actor;
  }

  static Actor player(String name, int distance) {
    var actor = new Actor(name);
    actor.player = true;
    actor.distance = distance;
    return actor;
  }

  static class Fake implements NativeCloseoutMobSkillSelection.Ports<Actor> {
    final Actor self = monster("self", 7);
    final List<Actor> visible = new ArrayList<>();
    final ArrayDeque<Integer> draws = new ArrayDeque<>();
    final List<Integer> bounds = new ArrayList<>();
    final Map<Integer, Integer> counts = new HashMap<>();
    final Map<Integer, Integer> delays = new HashMap<>();
    final Set<Integer> cooling = new HashSet<>();
    final List<String> effects = new ArrayList<>();
    boolean accept = true;

    Fake draw(int... values) {
      for (int value : values) draws.add(value);
      return this;
    }

    @Override
    public Actor attacker() {
      return self;
    }

    @Override
    public Iterable<Actor> visible() {
      return visible;
    }

    @Override
    public boolean npc(Actor target) {
      return target.npc;
    }

    @Override
    public boolean player(Actor target) {
      return target.player;
    }

    @Override
    public boolean petOrSummon(Actor target) {
      return target.pet;
    }

    @Override
    public boolean dead(Actor target) {
      return target.dead;
    }

    @Override
    public boolean ghost(Actor target) {
      return target.ghost;
    }

    @Override
    public boolean hates(Actor target) {
      return target.hated;
    }

    @Override
    public boolean lineOfSight(Actor target) {
      return target.sight;
    }

    @Override
    public int family(Actor target) {
      return target.family;
    }

    @Override
    public int hp(Actor target) {
      return target.hp;
    }

    @Override
    public int maxHp(Actor target) {
      return target.maxHp;
    }

    @Override
    public int distance(Actor target) {
      return target.distance;
    }

    @Override
    public boolean delayed(int row) {
      return cooling.contains(row);
    }

    @Override
    public int useCount(int row) {
      return counts.getOrDefault(row, 0);
    }

    @Override
    public int nextInt(int bound) {
      bounds.add(bound);
      Integer value = draws.poll();
      if (value == null) throw new AssertionError("unexpected random draw, bound " + bound);
      if (value < 0 || value >= bound)
        throw new AssertionError("scripted draw " + value + " outside bound " + bound);
      return value;
    }

    private boolean effect(String kind, NativeCloseoutControlledNpcData.Row row, Actor target) {
      effects.add(kind + ":" + row.actNo() + ":" + target);
      return accept;
    }

    @Override
    public boolean physical(NativeCloseoutControlledNpcData.Row row, Actor target) {
      return effect("physical", row, target);
    }

    @Override
    public boolean magic(NativeCloseoutControlledNpcData.Row row, Actor target) {
      return effect("magic", row, target);
    }

    @Override
    public boolean summon(NativeCloseoutControlledNpcData.Row row, Actor target) {
      return effect("summon", row, target);
    }

    @Override
    public boolean poly(NativeCloseoutControlledNpcData.Row row, Actor target) {
      return effect("poly", row, target);
    }

    @Override
    public boolean shockStun(NativeCloseoutControlledNpcData.Row row, Actor target) {
      return effect("shockStun", row, target);
    }

    @Override
    public boolean cancellation(NativeCloseoutControlledNpcData.Row row, Actor target) {
      return effect("cancellation", row, target);
    }

    @Override
    public boolean weaponBreak(NativeCloseoutControlledNpcData.Row row, Actor target) {
      return effect("weaponBreak", row, target);
    }

    @Override
    public void incrementUseCount(int row) {
      counts.merge(row, 1, Integer::sum);
    }

    @Override
    public void startDelay(int millis, int row) {
      delays.put(row, millis);
      if (millis > 0) cooling.add(row);
    }
  }

  /** Builder for one mobskill row; unset trigger columns are 0 as in {@code mobskill}. */
  static final class RowSpec {
    int type = TYPE_MAGIC_ATTACK;
    int triggerRandom, triggerHp, triggerCompanionHp, triggerRange, triggerCount, changeTarget;
    int reuse;

    RowSpec type(int value) {
      type = value;
      return this;
    }

    RowSpec random(int value) {
      triggerRandom = value;
      return this;
    }

    RowSpec hp(int value) {
      triggerHp = value;
      return this;
    }

    RowSpec companionHp(int value) {
      triggerCompanionHp = value;
      return this;
    }

    RowSpec range(int value) {
      triggerRange = value;
      return this;
    }

    RowSpec count(int value) {
      triggerCount = value;
      return this;
    }

    RowSpec change(int value) {
      changeTarget = value;
      return this;
    }

    RowSpec reuse(int value) {
      reuse = value;
      return this;
    }
  }

  static RowSpec row() {
    return new RowSpec();
  }

  static NativeCloseoutControlledNpcData.Data data(RowSpec... specs) {
    var rows = new ArrayList<NativeCloseoutControlledNpcData.Row>();
    for (int i = 0; i < specs.length; i++) {
      var s = specs[i];
      rows.add(
          new NativeCloseoutControlledNpcData.Row(
              i,
              s.type,
              s.triggerRandom,
              s.triggerHp,
              s.triggerCompanionHp,
              s.triggerRange,
              s.triggerCount,
              s.changeTarget,
              3,
              0,
              0,
              0,
              100 + i,
              0,
              0,
              0,
              0,
              0,
              0,
              s.reuse));
    }
    return new NativeCloseoutControlledNpcData.Data("fixture", 45034, 12, 9, 7, 1200, 900, rows);
  }

  // ---------------------------------------------------------------- probability (TriRnd)

  @Test
  void triggerRandomIsAStrictOneBasedPercentRoll() {
    // L1MobSkillUse.java:691-697: chance = nextInt(100) + 1; usable only when chance < TriRnd.
    var data = data(row().random(30));
    var target = player("target", 1);
    int usable = 0;
    for (int draw = 0; draw < 100; draw++) {
      // Second scripted value is the uniform pick, drawn only when the row turned out usable.
      var ports = new Fake().draw(draw, 0);
      var choice = NativeCloseoutMobSkillSelection.select(data, ports, target, true);
      assertEquals(
          choice == null ? List.of(100) : List.of(100, 1), ports.bounds, "one percent roll per row");
      if (choice != null) {
        usable++;
        assertTrue(draw + 1 < 30, "draw " + draw);
      } else assertTrue(draw + 1 >= 30, "draw " + draw);
    }
    assertEquals(29, usable, "TriRnd 30 fires on 29 of 100 rolls (Source strict comparison)");

    for (int draw = 0; draw < 100; draw++)
      assertNull(
          NativeCloseoutMobSkillSelection.select(
              data(row().random(1)), new Fake().draw(draw), target, true),
          "TriRnd 1 can never fire");
  }

  @Test
  void ordinaryRowsSkipTheRollWithoutTriRndButSummonAndPolyAlwaysRoll() {
    var target = player("target", 1);
    // L1MobSkillUse.java:688-690: isTriRnd || TYPE_SUMMON || TYPE_POLY.
    var ordinary = data(row().random(30).range(-3));
    var quiet = new Fake();
    assertTrue(NativeCloseoutMobSkillSelection.trigger(ordinary, quiet, target));
    assertTrue(quiet.bounds.isEmpty(), "isSkillTrigger draws nothing for an ordinary row");
    quiet.draw(0);
    assertNotNull(NativeCloseoutMobSkillSelection.select(ordinary, quiet, target, false));
    assertEquals(List.of(1), quiet.bounds, "only the uniform pick among one usable row");

    for (int type : new int[] {TYPE_SUMMON, TYPE_POLY}) {
      var forced = data(row().type(type).random(30).range(-3));
      assertFalse(
          NativeCloseoutMobSkillSelection.trigger(forced, new Fake().draw(29), target),
          "type " + type + " still rolls in isSkillTrigger");
      var hit = new Fake().draw(28);
      assertTrue(NativeCloseoutMobSkillSelection.trigger(forced, hit, target));
      assertEquals(List.of(100), hit.bounds);
    }
  }

  @Test
  void aRowWithoutAnySatisfiedTriggerColumnIsNeverUsable() {
    var target = player("target", 1);
    // L1MobSkillUse.java:681: useble starts false and only a satisfied column sets it.
    assertFalse(NativeCloseoutMobSkillSelection.trigger(data(row()), new Fake(), target));
    assertNull(NativeCloseoutMobSkillSelection.select(data(row()), new Fake(), target, true));
    // TriRnd alone is ignored by isSkillTrigger (isTriRnd == false), so the row stays unusable.
    assertFalse(
        NativeCloseoutMobSkillSelection.trigger(data(row().random(99)), new Fake(), target));
    assertFalse(NativeCloseoutMobSkillSelection.trigger(null, new Fake(), target));
    assertFalse(NativeCloseoutMobSkillSelection.trigger(data(), new Fake(), target));
    assertFalse(
        NativeCloseoutMobSkillSelection.use(data(row()), new Fake(), target, true),
        "nothing usable: no effect, no count");
  }

  // ---------------------------------------------------------------- HP / range / count columns

  @Test
  void triggerHpComparesTheAttackersTruncatedPercentInclusive() {
    var data = data(row().hp(50));
    var target = player("target", 1);
    var ports = new Fake();
    ports.self.hp = 50;
    assertTrue(NativeCloseoutMobSkillSelection.trigger(data, ports, target), "50% <= 50");
    ports.self.hp = 51;
    assertFalse(NativeCloseoutMobSkillSelection.trigger(data, ports, target), "51% > 50");
    ports.self.maxHp = 199;
    ports.self.hp = 101; // 101 * 100 / 199 = 50 (truncated)
    assertTrue(NativeCloseoutMobSkillSelection.trigger(data, ports, target));
  }

  @Test
  void triggerRangeNegativeMeansWithinAndPositiveMeansAtLeast() {
    // templates/L1MobSkill.java:265-273.
    assertTrue(NativeCloseoutMobSkillSelection.distance(-3, 3));
    assertTrue(NativeCloseoutMobSkillSelection.distance(-3, 0));
    assertFalse(NativeCloseoutMobSkillSelection.distance(-3, 4));
    assertTrue(NativeCloseoutMobSkillSelection.distance(3, 3));
    assertTrue(NativeCloseoutMobSkillSelection.distance(3, 9));
    assertFalse(NativeCloseoutMobSkillSelection.distance(3, 2));
    assertFalse(NativeCloseoutMobSkillSelection.distance(0, 0), "0 is 'no range trigger'");

    var near = player("near", 2);
    var far = player("far", 6);
    assertTrue(NativeCloseoutMobSkillSelection.trigger(data(row().range(-3)), new Fake(), near));
    assertFalse(NativeCloseoutMobSkillSelection.trigger(data(row().range(-3)), new Fake(), far));
    assertFalse(NativeCloseoutMobSkillSelection.trigger(data(row().range(4)), new Fake(), near));
    assertTrue(NativeCloseoutMobSkillSelection.trigger(data(row().range(4)), new Fake(), far));
  }

  @Test
  void triggerCountLimitsSuccessfulUsesUntilTheAiFinishes() {
    var data = data(row().count(2));
    var target = player("target", 1);
    var ports = new Fake().draw(0, 0);
    assertTrue(NativeCloseoutMobSkillSelection.use(data, ports, target, true));
    assertTrue(NativeCloseoutMobSkillSelection.use(data, ports, target, true));
    assertEquals(2, ports.useCount(0));
    assertFalse(
        NativeCloseoutMobSkillSelection.use(data, ports, target, true), "third use is refused");
    assertFalse(NativeCloseoutMobSkillSelection.trigger(data, ports, target));
    assertEquals(2, ports.effects.size());
    ports.counts.clear(); // NpcAI.finishAI -> resetAllSkillUseCount
    assertTrue(NativeCloseoutMobSkillSelection.trigger(data, ports, target));
  }

  @Test
  void aFailedColumnRejectsTheRowEvenWhenAnEarlierColumnPassed() {
    var target = player("target", 6);
    var ports = new Fake();
    ports.self.hp = 10;
    // HP column passes, range column fails -> return false (L1MobSkillUse.java:726-736).
    assertFalse(
        NativeCloseoutMobSkillSelection.trigger(data(row().hp(50).range(-3)), ports, target));
    target.distance = 2;
    assertTrue(
        NativeCloseoutMobSkillSelection.trigger(data(row().hp(50).range(-3)), ports, target));
  }

  // ---------------------------------------------------------------- reuse delay (cooldown)

  @Test
  void aDelayedRowIsSkippedBeforeAnyRollAndOnlyASuccessStartsTheDelay() {
    var data = data(row().range(-3).reuse(8_000), row().range(-3));
    var target = player("target", 1);
    var ports = new Fake().draw(0);
    assertTrue(NativeCloseoutMobSkillSelection.use(data, ports, target, true));
    assertEquals(List.of("magic:0:target"), ports.effects);
    assertEquals(Map.of(0, 8_000), ports.delays, "L1SkillDelayforMob.onSkillUse(reuseDelay, idx)");
    assertEquals(1, ports.useCount(0));

    // Row 0 is cooling: only row 1 is eligible, so the uniform pick has bound 1.
    ports.bounds.clear();
    ports.draw(0);
    assertTrue(NativeCloseoutMobSkillSelection.use(data, ports, target, true));
    assertEquals(List.of(1), ports.bounds);
    assertEquals("magic:1:target", ports.effects.get(1));
    assertEquals(0, (int) ports.delays.get(1), "reuse 0 is passed through; the state ignores it");

    // A refused effect neither counts nor cools (L1MobSkillUse.java:290-292).
    var refused = new Fake().draw(0);
    refused.accept = false;
    assertFalse(NativeCloseoutMobSkillSelection.use(data, refused, target, true));
    assertEquals(1, refused.effects.size());
    assertTrue(refused.counts.isEmpty());
    assertTrue(refused.delays.isEmpty());
  }

  // ---------------------------------------------------------------- choice among usable rows

  @Test
  void theUsedRowIsDrawnUniformlyFromTheUsableRowsOnly() {
    var data = data(row().range(-3), row().range(9), row().range(-3), row().range(-3));
    var target = player("target", 2);
    var picks = new ArrayList<Integer>();
    for (int draw = 0; draw < 3; draw++) {
      var ports = new Fake().draw(draw);
      var choice = NativeCloseoutMobSkillSelection.select(data, ports, target, true);
      assertEquals(List.of(3), ports.bounds, "_rnd.nextInt(skillSizeCounter)");
      picks.add(choice.row().actNo());
      assertSame(target, choice.target());
    }
    assertEquals(List.of(0, 2, 3), picks, "row 1 (range >= 9) is not usable at distance 2");
  }

  @Test
  void typeNoneTerminatesTheTableAndAnEmptyFirstRowDisablesIt() {
    var target = player("target", 1);
    var terminated = data(row().range(9), row().type(TYPE_NONE).range(-3), row().range(-3));
    assertFalse(
        NativeCloseoutMobSkillSelection.trigger(terminated, new Fake(), target),
        "rows after TYPE_NONE are never read");
    assertNull(NativeCloseoutMobSkillSelection.select(terminated, new Fake(), target, true));
    var disabled = data(row().type(TYPE_NONE).range(-3), row().range(-3));
    assertFalse(NativeCloseoutMobSkillSelection.trigger(disabled, new Fake(), target));
    assertFalse(NativeCloseoutMobSkillSelection.use(disabled, new Fake(), target, true));
  }

  @Test
  void eachTypeDispatchesToItsOwnEffect() {
    var target = player("target", 1);
    Map<Integer, String> expected = new LinkedHashMap<>();
    expected.put(TYPE_PHYSICAL_ATTACK, "physical");
    expected.put(TYPE_MAGIC_ATTACK, "magic");
    expected.put(AHTHARAS_1, "shockStun");
    expected.put(AHTHARAS_2, "cancellation");
    expected.put(AHTHARAS_3, "weaponBreak");
    for (var entry : expected.entrySet()) {
      var ports = new Fake().draw(0);
      assertTrue(
          NativeCloseoutMobSkillSelection.use(
              data(row().type(entry.getKey()).range(-3)), ports, target, true));
      assertEquals(List.of(entry.getValue() + ":0:target"), ports.effects);
      assertEquals(1, ports.useCount(0));
    }
    expected.clear();
    expected.put(TYPE_SUMMON, "summon");
    expected.put(TYPE_POLY, "poly");
    for (var entry : expected.entrySet()) {
      var ports = new Fake().draw(0);
      assertTrue(
          NativeCloseoutMobSkillSelection.use(
              data(row().type(entry.getKey()).range(-3)), ports, target, false));
      assertEquals(List.of(entry.getValue() + ":0:target"), ports.effects);
    }
    var unknown = new Fake().draw(0);
    assertFalse(
        NativeCloseoutMobSkillSelection.use(data(row().type(99).range(-3)), unknown, target, true));
    assertTrue(unknown.effects.isEmpty());
    assertTrue(unknown.counts.isEmpty(), "unknown type: no count, no delay");
  }

  // ---------------------------------------------------------------- target changes

  @Test
  void companionHpTriggerRetargetsToTheWeakestSameFamilyNpc() {
    var target = player("target", 1);
    var ports = new Fake();
    var healthy = monster("healthy", 7);
    var hurt = monster("hurt", 7);
    hurt.hp = 30;
    var weaker = monster("weakerOtherFamily", 8);
    weaker.hp = 5;
    var hurtPlayer = player("hurtPlayer", 1);
    hurtPlayer.hp = 1;
    ports.visible.addAll(List.of(healthy, weaker, hurtPlayer, hurt));
    var data = data(row().companionHp(40));

    // searchMinCompanionHp (749-769): same family NPCs only, strictly below 100 %.
    var choice = NativeCloseoutMobSkillSelection.select(data, ports.draw(0), target, true);
    assertSame(hurt, choice.target(), "_target = companionNpc");

    hurt.hp = 41;
    assertNull(NativeCloseoutMobSkillSelection.select(data, new FakeWith(ports), target, true));
    hurt.hp = 100;
    assertNull(
        NativeCloseoutMobSkillSelection.select(data, new FakeWith(ports), target, true),
        "nobody below 100 %: companion is null and the row is rejected");
  }

  /** Same scene as another fake, fresh random script. */
  static final class FakeWith extends Fake {
    FakeWith(Fake scene) {
      visible.addAll(scene.visible);
    }
  }

  @Test
  void changeTargetMeAndRandomFollowTheSourceFilters() {
    var original = player("original", 1);
    var ports = new Fake();
    var inRange = player("inRange", 2);
    var blocked = player("blocked", 2);
    blocked.sight = false;
    var unhated = player("unhated", 2);
    unhated.hated = false;
    var dead = player("dead", 2);
    dead.dead = true;
    var ghost = player("ghost", 2);
    ghost.ghost = true;
    var far = player("far", 8);
    var summon = new Actor("summon");
    summon.pet = true;
    summon.distance = 3;
    var bystanderNpc = monster("npc", 9);
    bystanderNpc.distance = 1;
    ports.visible.addAll(List.of(blocked, unhated, inRange, dead, ghost, far, bystanderNpc, summon));

    // CHANGE_TARGET_ME (781-783).
    var me = NativeCloseoutMobSkillSelection.select(
        data(row().change(CHANGE_TARGET_ME).range(-3)), new FakeWith(ports).draw(0), original, true);
    assertEquals("self", me.target().name);

    // CHANGE_TARGET_RANDOM (785-835): two candidates (inRange, summon); index = nextInt(n*100)/100.
    var random = data(row().change(CHANGE_TARGET_RANDOM).range(-3));
    var first = new FakeWith(ports).draw(99, 0);
    assertSame(inRange, NativeCloseoutMobSkillSelection.select(random, first, original, true).target());
    assertEquals(List.of(200, 1), first.bounds);
    var second = new FakeWith(ports).draw(100, 0);
    assertSame(summon, NativeCloseoutMobSkillSelection.select(random, second, original, true).target());

    // No candidate: the previous target is kept and no draw is made for the target.
    var empty = new Fake().draw(0);
    assertSame(
        original, NativeCloseoutMobSkillSelection.select(random, empty, original, true).target());
    assertEquals(List.of(1), empty.bounds);

    // Unsupported change type (1 = companion) keeps the previous scan target.
    var kept = NativeCloseoutMobSkillSelection.select(
        data(row().change(CHANGE_TARGET_COMPANION).range(-3)),
        new FakeWith(ports).draw(0),
        original,
        true);
    assertSame(original, kept.target());
  }

  @Test
  void theFinalScanTargetIsUsedForWhicheverRowIsDrawn() {
    // Source quirk (182-232): _target is a field; the row is drawn after the scan finished, so
    // the effect uses the target left by the last scanned row.
    var original = player("original", 1);
    var data = data(row().range(-3), row().change(CHANGE_TARGET_ME).hp(100));
    var ports = new Fake().draw(0);
    var choice = NativeCloseoutMobSkillSelection.select(data, ports, original, true);
    assertEquals(0, choice.row().actNo());
    assertEquals("self", choice.target().name);

    // A later row without changeTarget resets the scan target to the original.
    var reset = data(row().change(CHANGE_TARGET_ME).hp(100), row().range(-3));
    assertSame(
        original,
        NativeCloseoutMobSkillSelection.select(reset, new Fake().draw(0), original, true).target());
  }
}
