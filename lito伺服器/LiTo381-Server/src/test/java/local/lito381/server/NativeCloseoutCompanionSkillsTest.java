package local.lito381.server;

import static local.lito381.server.NativeCloseoutCompanionRecoveryTest.PET_NPC;
import static local.lito381.server.NativeCloseoutCompanionRecoveryTest.SUMMON_NPC;
import static local.lito381.server.NativeCloseoutCompanionRecoveryTest.snapshot;
import static local.lito381.server.NativeCloseoutCompanionRecoveryTest.template;
import static local.lito381.server.NativeCloseoutCompanionRecoveryTest.templates;
import static local.lito381.server.NativeDeNpcMobSkill.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * T135 (Issue #386, G15 group 2): actor-owned Source mobskill state of pets and summons ({@code
 * L1MobSkillUse._skillUseCount / _skillDelay}), its lease, and the existing actor seams (birth,
 * HP / active terminal writes, BATTLE / WORLD ticks). Ported from candidate {@code 046c58c5…}.
 *
 * <p>Real {@link BattleCompanion} actors born through the production factories; time is an
 * explicit nanosecond argument, so there is no sleep and no scheduler.
 */
final class NativeCloseoutCompanionSkillsTest {

  static final long SECOND = 1_000_000_000L;

  /** Row 0: magic, within 3 tiles, 8 s reuse. Row 1: physical, within 1 tile, at most 2 uses. */
  static NativeDeNpcMobSkill mobSkills(int npc) {
    var mob = new NativeDeNpcMobSkill(3);
    mob.set_mobid(npc);
    mob.setType(0, TYPE_MAGIC_ATTACK);
    mob.setTriggerRandom(0, 30);
    mob.setTriggerRange(0, -3);
    mob.setSkillId(0, 10);
    mob.setLeverage(0, 15);
    mob.setReuseDelay(0, 8_000);
    mob.setType(1, TYPE_PHYSICAL_ATTACK);
    mob.setTriggerRange(1, -1);
    mob.setTriggerCount(1, 2);
    mob.setRange(1, 1);
    mob.setActid(1, 30);
    // Row 2 stays TYPE_NONE: the Source table terminator.
    return mob;
  }

  static BattleCompanion petWithSkills() throws Exception {
    try (var ignored =
        templates(
            snapshot(List.of(template(PET_NPC, 3, 4, 5, 2)), List.of(mobSkills(PET_NPC))))) {
      return NativeCloseoutCompanionRecoveryTest.pet();
    }
  }

  static BattleCompanion summonWithSkills() throws Exception {
    try (var ignored =
        templates(
            snapshot(
                List.of(template(SUMMON_NPC, 3, 4, 5, 2)), List.of(mobSkills(SUMMON_NPC))))) {
      return NativeCloseoutCompanionRecoveryTest.summon();
    }
  }

  /** The monitors the lease contract requires: actor, then its timed status. */
  static <T> T locked(BattleCompanion pet, Supplier<T> body) {
    synchronized (pet) {
      synchronized (CombatAccess.field(pet, "timedStatus")) {
        return body.get();
      }
    }
  }

  static void lockedRun(BattleCompanion pet, Runnable body) {
    locked(
        pet,
        () -> {
          body.run();
          return null;
        });
  }

  static NativeCloseoutCompanionSkills.Lease lease(BattleCompanion pet) {
    return locked(pet, () -> NativeCloseoutCompanionSkills.capture(pet));
  }

  static NativeCloseoutCompanionSkills.State state(BattleCompanion pet) {
    return (NativeCloseoutCompanionSkills.State) pet.nativeCloseoutMobSkillState;
  }

  // ---------------------------------------------------------------- birth

  @Test
  void birthInstallsOneStatePerActorFromTheCompleteTemplateRows() throws Exception {
    var pet = petWithSkills();
    var state = state(pet);
    assertNotNull(state);
    assertSame(NativeSourceNpcFactoryType.stamp(pet), state.factory);
    assertSame(NativeSourceNpcFactoryType.stamp(pet).nativeCloseoutControlledNpcData, state.data);
    assertEquals(PET_NPC, state.data.npc());
    assertEquals(12, state.data.intelligence());
    assertEquals(9, state.data.wisdom());
    assertEquals(7, state.data.family());
    assertEquals(1200, state.data.attackMagicMillis());
    assertEquals(900, state.data.supportMagicMillis());
    assertEquals(3, state.data.rows().size(), "one Row per mobskill slot, including the terminator");
    var magic = state.data.rows().get(0);
    assertEquals(0, magic.actNo());
    assertEquals(TYPE_MAGIC_ATTACK, magic.type());
    assertEquals(30, magic.triggerRandom());
    assertEquals(-3, magic.triggerRange());
    assertEquals(10, magic.skill());
    assertEquals(15, magic.leverage());
    assertEquals(8_000, magic.reuse());
    var physical = state.data.rows().get(1);
    assertEquals(TYPE_PHYSICAL_ATTACK, physical.type());
    assertEquals(2, physical.triggerCount());
    assertEquals(1, physical.range());
    assertEquals(30, physical.action());
    assertEquals(TYPE_NONE, state.data.rows().get(2).type());
    assertEquals(3, state.counts.length);
    assertEquals(3, state.cooling.length);

    var other = petWithSkills();
    assertNotSame(state, state(other), "no shared registry: each actor owns its own counters");
    assertNotSame(state.counts, state(other).counts);
  }

  @Test
  void actorsWithoutASourceTemplateOrNotPetOrSummonHaveNoState() throws Exception {
    BattleCompanion unknown;
    try (var ignored =
        templates(snapshot(List.of(template(SUMMON_NPC, 3, 4, 5, 2)), List.of()))) {
      unknown = NativeCloseoutCompanionRecoveryTest.pet();
    }
    assertNull(unknown.nativeCloseoutMobSkillState);
    assertNull(lease(unknown), "no state, no lease, nothing to select");

    var arena = new BattleCompanion(0x6000_0214L);
    arena.resetForBattle(100, 100);
    assertNull(arena.nativeCloseoutMobSkillState);
    NativeCloseoutCompanionSkills.tick(arena); // existing ticks stay a no-op for such actors
    assertFalse(arena.applyDamage(1).diedNow());

    BattleCompanion bare;
    try (var ignored =
        templates(snapshot(List.of(template(PET_NPC, 3, 4, 5, 2)), List.of()))) {
      bare = NativeCloseoutCompanionRecoveryTest.pet();
    }
    assertNotNull(state(bare), "template without mobskill rows: an empty table");
    assertEquals(0, state(bare).counts.length);
    assertFalse(
        NativeCloseoutMobSkillSelection.trigger(
            state(bare).data,
            new NativeCloseoutMobSkillSelectionTest.Fake(),
            NativeCloseoutMobSkillSelectionTest.player("target", 1)));
  }

  // ---------------------------------------------------------------- lease contract

  @Test
  void leaseRequiresTheActorAndStatusMonitors() throws Exception {
    var pet = petWithSkills();
    assertEquals(
        "companion mobskill actor monitor required",
        assertThrows(
                IllegalStateException.class, () -> NativeCloseoutCompanionSkills.capture(pet))
            .getMessage());
    synchronized (pet) {
      assertEquals(
          "companion mobskill status monitor required",
          assertThrows(
                  IllegalStateException.class, () -> NativeCloseoutCompanionSkills.capture(pet))
              .getMessage());
    }
    var lease = lease(pet);
    assertNotNull(lease);
    assertThrows(IllegalStateException.class, () -> lease.useCount(0), "outside the monitors");
    lockedRun(
        pet,
        () -> {
          assertTrue(lease.current());
          assertEquals(0, lease.useCount(0));
          assertThrows(IndexOutOfBoundsException.class, () -> lease.useCount(3));
          assertThrows(IndexOutOfBoundsException.class, () -> lease.incrementUseCount(-1));
        });
  }

  // ---------------------------------------------------------------- cooldown (reuse delay)

  @Test
  void reuseDelayRunsOnItsDeadlineIsNeverExtendedAndIgnoresNonPositiveValues() throws Exception {
    var pet = petWithSkills();
    var lease = lease(pet);
    long start = 5 * SECOND;
    lockedRun(
        pet,
        () -> {
          assertFalse(lease.delayed(0, start));
          // L1SkillDelayforMob.onSkillUse: time <= 0 is a no-op.
          lease.startDelay(0, 0, start);
          lease.startDelay(-5, 0, start);
          assertFalse(lease.delayed(0, start));

          lease.startDelay(8_000, 0, start);
          assertTrue(lease.delayed(0, start));
          assertTrue(lease.delayed(0, start + 8 * SECOND - 1));
          assertFalse(lease.delayed(1, start), "delays are per row");

          // trySetSkillDelay: an active delay is not replaced by a later, longer one.
          lease.startDelay(60_000, 0, start + SECOND);
          assertTrue(lease.delayed(0, start + 8 * SECOND - 1));
          assertFalse(lease.delayed(0, start + 8 * SECOND), "expires exactly after 8000 ms");
          assertFalse(lease.delayed(0, start + 9 * SECOND));

          // After expiry a new delay starts from the new use.
          lease.startDelay(1_000, 0, start + 9 * SECOND);
          assertTrue(lease.delayed(0, start + 9 * SECOND + 999_999_999L));
          assertFalse(lease.delayed(0, start + 10 * SECOND));
        });
  }

  @Test
  void delayDeadlineSurvivesLongMillisecondValuesWithoutIntOverflow() throws Exception {
    var pet = petWithSkills();
    var lease = lease(pet);
    lockedRun(
        pet,
        () -> {
          lease.startDelay(3_600_000, 0, 0L); // one hour: 3.6e15 ns does not fit an int product
          assertTrue(lease.delayed(0, 3_599L * SECOND));
          assertFalse(lease.delayed(0, 3_600L * SECOND));
        });
  }

  // ---------------------------------------------------------------- AI finish (rest / no target)

  @Test
  void restingWithoutATargetResetsUseCountsButKeepsReuseDelays() throws Exception {
    var pet = petWithSkills();
    assertEquals(BattleCompanion.CommandResult.POSTURE_CHANGED, pet.apply("aggressive"));
    var lease = lease(pet);
    lockedRun(
        pet,
        () -> {
          lease.incrementUseCount(1);
          lease.incrementUseCount(1);
          lease.startDelay(8_000, 0, 0L);
        });

    // Aggressive actor that merely lost its target: the Source AI keeps running.
    NativeCloseoutCompanionSkills.tick(pet);
    lockedRun(pet, () -> assertEquals(2, lease.useCount(1)));

    // Target selected while resting: still not a finish.
    assertEquals(BattleCompanion.CommandResult.POSTURE_CHANGED, pet.apply("stay"));
    assertTrue(pet.selectTarget(0x1234L));
    NativeCloseoutCompanionSkills.tick(pet);
    lockedRun(pet, () -> assertEquals(2, lease.useCount(1)));

    // Rest (status 3) with no target: noTarget() -> NpcAI.finishAI -> resetAllSkillUseCount.
    pet.clearTarget();
    NativeCloseoutCompanionSkills.tick(pet);
    assertArrayEquals(new int[] {0, 0, 0}, state(pet).counts);
    lockedRun(
        pet,
        () -> {
          assertFalse(lease.current(), "a lease taken before the finish is stale");
          assertThrows(IllegalStateException.class, () -> lease.useCount(1));
          assertThrows(IllegalStateException.class, () -> lease.incrementUseCount(1));
          assertThrows(IllegalStateException.class, () -> lease.startDelay(1, 1, 0L));
        });

    var next = lease(pet);
    assertNotNull(next, "the next AI run takes a fresh lease");
    lockedRun(
        pet,
        () -> {
          assertEquals(0, next.useCount(1));
          assertTrue(next.delayed(0, 8 * SECOND - 1), "resetAllSkillUseCount leaves _skillDelay");
          assertFalse(next.delayed(0, 8 * SECOND));
        });
  }

  // ---------------------------------------------------------------- death / dismissal

  @Test
  void deadPetHasNoLeaseAndItsOldLeaseRejectsEveryAccounting() throws Exception {
    var pet = petWithSkills();
    assertEquals(BattleCompanion.CommandResult.POSTURE_CHANGED, pet.apply("aggressive"));
    var lease = lease(pet);
    lockedRun(pet, () -> lease.incrementUseCount(1));
    assertFalse(pet.applyDamage(10).diedNow());
    lockedRun(pet, () -> assertEquals(1, lease.useCount(1), "non-lethal damage keeps the AI run"));

    assertTrue(pet.applyDamage(1_000).diedNow());
    assertArrayEquals(new int[] {0, 0, 0}, state(pet).counts, "terminal HP write finishes the AI");
    lockedRun(
        pet,
        () -> {
          assertFalse(lease.current());
          assertThrows(IllegalStateException.class, () -> lease.delayed(0, 0L));
          assertThrows(IllegalStateException.class, () -> lease.incrementUseCount(0));
        });
    assertNull(lease(pet), "a dead pet cannot select or cast");

    // Selection wired to the stale lease fails before any effect is reached.
    var ports = new LeasePorts(pet, lease, new long[] {0L});
    ports.draw(0, 0);
    lockedRun(
        pet,
        () ->
            assertThrows(
                IllegalStateException.class,
                () ->
                    NativeCloseoutMobSkillSelection.use(
                        state(pet).data,
                        ports,
                        NativeCloseoutMobSkillSelectionTest.player("target", 1),
                        true)));
    assertTrue(ports.effects.isEmpty(), "no cast after death");
  }

  @Test
  void dismissedOrKilledSummonHasNoLease() throws Exception {
    var dismissed = summonWithSkills();
    var first = lease(dismissed);
    assertNotNull(first);
    lockedRun(dismissed, () -> first.incrementUseCount(0));
    assertEquals(BattleCompanion.CommandResult.DISMISSED, dismissed.apply("dismiss"));
    assertArrayEquals(new int[] {0, 0, 0}, state(dismissed).counts);
    lockedRun(dismissed, () -> assertFalse(first.current()));
    assertNull(lease(dismissed));

    var killed = summonWithSkills();
    var second = lease(killed);
    lockedRun(killed, () -> second.incrementUseCount(0));
    assertTrue(killed.applyDamage(10_000).diedNow());
    assertFalse(killed.snapshot().active());
    assertArrayEquals(new int[] {0, 0, 0}, state(killed).counts);
    assertNull(lease(killed));

    var expired = summonWithSkills();
    var third = lease(expired);
    lockedRun(expired, () -> third.incrementUseCount(0));
    expired.deactivate(); // lifetime expiry / owner removal path
    assertArrayEquals(new int[] {0, 0, 0}, state(expired).counts);
    lockedRun(expired, () -> assertFalse(third.current()));
    assertNull(lease(expired));
  }

  // ---------------------------------------------------------------- selection on the real state

  /** Selection ports whose use counts and delays are the actor's own state at a scripted time. */
  static final class LeasePorts extends NativeCloseoutMobSkillSelectionTest.Fake {
    final BattleCompanion pet;
    final NativeCloseoutCompanionSkills.Lease lease;
    final long[] now;

    LeasePorts(BattleCompanion pet, NativeCloseoutCompanionSkills.Lease lease, long[] now) {
      this.pet = pet;
      this.lease = lease;
      this.now = now;
    }

    @Override
    public boolean delayed(int row) {
      return lease.delayed(row, now[0]);
    }

    @Override
    public int useCount(int row) {
      return lease.useCount(row);
    }

    @Override
    public void incrementUseCount(int row) {
      lease.incrementUseCount(row);
    }

    @Override
    public void startDelay(int millis, int row) {
      lease.startDelay(millis, row, now[0]);
    }
  }

  @Test
  void selectionOnTheActorStateHonoursCooldownAndUseLimit() throws Exception {
    var pet = petWithSkills();
    assertEquals(BattleCompanion.CommandResult.POSTURE_CHANGED, pet.apply("aggressive"));
    var data = state(pet).data;
    var target = NativeCloseoutMobSkillSelectionTest.player("target", 1);
    long[] now = {0L};
    var ports = new LeasePorts(pet, lease(pet), now);

    lockedRun(
        pet,
        () -> {
          // Roll 0 -> chance 1 < TriRnd 30: both rows usable, uniform pick 0 -> the magic row.
          ports.draw(0, 0);
          assertTrue(NativeCloseoutMobSkillSelection.use(data, ports, target, true));
          assertEquals(List.of("magic:0:target"), ports.effects);
          assertEquals(List.of(100, 2), ports.bounds);

          // 8 s reuse: the magic row is skipped before its percent roll; only the physical row.
          now[0] = 7 * SECOND;
          ports.bounds.clear();
          ports.draw(0);
          assertTrue(NativeCloseoutMobSkillSelection.use(data, ports, target, true));
          assertEquals("physical:1:target", ports.effects.get(1));
          assertEquals(List.of(1), ports.bounds, "no percent roll for a cooling row");

          // Still cooling; physical row reaches its limit of 2 uses.
          ports.bounds.clear();
          ports.draw(0);
          assertTrue(NativeCloseoutMobSkillSelection.use(data, ports, target, true));
          assertEquals("physical:1:target", ports.effects.get(2));
          assertFalse(
              NativeCloseoutMobSkillSelection.use(data, ports, target, true),
              "magic cooling, physical exhausted: nothing to cast and no random draw");
          assertFalse(NativeCloseoutMobSkillSelection.trigger(data, ports, target));
          assertEquals(3, ports.effects.size());

          // Cooldown over: the magic row is back, the physical row is still exhausted.
          now[0] = 8 * SECOND;
          assertTrue(NativeCloseoutMobSkillSelection.trigger(data, ports, target));
          ports.bounds.clear();
          ports.draw(29); // chance 30 is not < 30
          assertFalse(NativeCloseoutMobSkillSelection.use(data, ports, target, true));
          ports.draw(28, 0);
          assertTrue(NativeCloseoutMobSkillSelection.use(data, ports, target, true));
          assertEquals("magic:0:target", ports.effects.get(3));
          assertEquals(List.of(100, 100, 1), ports.bounds);
        });
    assertArrayEquals(new int[] {2, 2, 0}, state(pet).counts);
  }
}
