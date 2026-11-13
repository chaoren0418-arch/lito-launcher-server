package local.lito381.server;

import static local.lito381.server.NativeSourceAreaPairFirstTest.allocate;
import static local.lito381.server.NativeSourceAreaPairFirstTest.put;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;

/**
 * T135 (Issue #386, G15 group 1): natural HP / MP recovery of controlled NPCs (pets, summons) and
 * the owner-only HP meter, ported from candidate {@code 046c58c5…} and checked against {@code
 * 1-伺服器} ({@code timecontroller/pet/HprPet.java}, {@code MprPet.java}, {@code
 * StartTimer_Pet.java}, {@code L1PetInstance.java:1093}, {@code L1SummonInstance.java:1465}).
 *
 * <p>Reuses the memory-only BATTLE fixture of {@link NativeSourceAreaPairFirstTest.Scene} (Hub,
 * lifetime, owner PC) and adds a running room plus real {@link BattleCompanion} actors born through
 * the production factories. No socket, DB, thread pool or sleep: the one-second clock is driven by
 * calling {@code collect(clock, tick)} with explicit tick numbers.
 */
final class NativeCloseoutCompanionRecoveryTest {

  static final int PET_NPC = 45034;
  static final int SUMMON_NPC = 81222;
  static final int TAMED_NPC = 81217;

  // ---------------------------------------------------------------- Source template fixture

  static NativeDeNpcTemplate template(int npc, int hprInterval, int hpr, int mprInterval, int mpr) {
    var template = new NativeDeNpcTemplate();
    template.set_npcId(npc);
    template.set_hprinterval(hprInterval);
    template.set_hpr(hpr);
    template.set_mprinterval(mprInterval);
    template.set_mpr(mpr);
    template.set_int(12);
    template.set_wis(9);
    template.set_family(7);
    template.setAtkMagicSpeed(1200);
    template.setSubMagicSpeed(900);
    return template;
  }

  static NativeDeNpcTemplateResources.Snapshot snapshot(
      List<NativeDeNpcTemplate> npcs, List<NativeDeNpcMobSkill> mobSkills) {
    var npcMap = new HashMap<Integer, NativeDeNpcTemplate>();
    for (var npc : npcs) npcMap.put(npc.get_npcId(), npc);
    var mobMap = new HashMap<Integer, NativeDeNpcMobSkill>();
    for (var mob : mobSkills) mobMap.put(mob.get_mobid(), mob);
    return new NativeDeNpcTemplateResources.Snapshot(
        "fixture", npcMap, mobMap, Map.of(), -1, Set.of(), Set.of(), 0, 0L, 0L, 0L, 0L, false);
  }

  /** Publishes a Source template snapshot for the duration of an actor birth, then restores. */
  static AutoCloseable templates(NativeDeNpcTemplateResources.Snapshot snapshot) throws Exception {
    Field current = NativeDeNpcTemplateResources.class.getDeclaredField("current");
    current.setAccessible(true);
    Object previous = current.get(null);
    current.set(null, snapshot);
    return () -> current.set(null, previous);
  }

  static BattleCompanion pet() {
    var durable =
        new GameStateStore.PetState(
            "legacy\u001f10231",
            0x7000_3901L,
            0x9000_0303L,
            (int) InventoryCatalog.PET_COLLAR,
            PET_NPC,
            "牧羊犬",
            12,
            78,
            17,
            21_375,
            0);
    return BattleCompanion.pet(durable, SourcePetTemplate.memoryFixture(PET_NPC), 100, 120);
  }

  static BattleCompanion summon() {
    return BattleCompanion.summoned(
        0x6000_0302L,
        BattleSpellCatalog.summonChoice(11),
        SourceSummonTemplate.memoryFixture(SUMMON_NPC),
        6,
        1_800,
        100,
        120,
        1_000_000L);
  }

  static BattleCompanion tamed() {
    return BattleCompanion.tamed(
        0x6000_0301L, TAMED_NPC, 4205, "$3378", 20, 100, 100, 20, 0, 14, 14, 100, 120);
  }

  static int hp(BattleCompanion pet) {
    return pet.snapshot().hp();
  }

  static int mp(BattleCompanion pet) {
    return pet.snapshot().mp();
  }

  // ---------------------------------------------------------------- running BATTLE room fixture

  /** Scene of #354 plus a RUNNING room, an installed clock and the owner's controlled actors. */
  static final class Arena {
    final NativeSourceAreaPairFirstTest.Scene s = new NativeSourceAreaPairFirstTest.Scene(76);
    final Object room;
    final Object match = allocate(BattleRules.MatchState.class);
    final NativeCloseoutCompanionRecovery.Clock clock;
    final List<BattleCompanion> bag;

    @SuppressWarnings("unchecked")
    Arena(BattleCompanion... pets) {
      try {
        room = allocate(Class.forName("local.lito381.server.GameHub$Room"));
      } catch (ClassNotFoundException e) {
        throw new AssertionError(e);
      }
      put(match, "phase", BattleRules.Phase.RUNNING);
      put(room, "match", match);
      put(room, "battleStarted", true);
      var rooms = new LinkedHashMap<Long, Object>();
      rooms.put(7L, room);
      put(s.hub, "rooms", rooms);
      put(s.hub, "connectedSessions", new LinkedHashSet<>(List.of(s.caster, s.owner)));
      put(s.hub, "classicWorldMembership", new LinkedHashMap<>());
      put(s.life, "admission", new Object());
      for (var pc : List.of(s.caster, s.owner, s.bystander))
        put(pc, "sourceVitalCompletions", new NativePlayerVitalCompletions.State());
      clock = new NativeCloseoutCompanionRecovery.Clock(s.hub, s.life);
      put(s.hub, NativeCloseoutCompanionRecovery.CLOCK, clock);
      bag = (List<BattleCompanion>) CombatAccess.field(s.owner, "tamedCompanions");
      bag.addAll(List.of(pets));
    }

    List<NativeCloseoutCompanionRecovery.Value> tick(int tick) {
      return NativeCloseoutCompanionRecovery.collect(clock, tick);
    }
  }

  /** Pet 45034 (max 78 HP / 17 MP) born from a template with the given cadence. */
  static BattleCompanion petWith(int hprInterval, int hpr, int mprInterval, int mpr)
      throws Exception {
    try (var ignored =
        templates(
            snapshot(List.of(template(PET_NPC, hprInterval, hpr, mprInterval, mpr)), List.of()))) {
      return pet();
    }
  }

  // ---------------------------------------------------------------- template capture

  @Test
  void birthFreezesTheTemplateCadenceAndSourceDefaults() throws Exception {
    var explicit = petWith(3, 4, 5, 2);
    var stamp = NativeSourceNpcFactoryType.stamp(explicit);
    assertEquals(5, stamp.kind(), "L1Pet");
    assertEquals(
        new NativeCloseoutCompanionRecoveryData.Data(3, 4, 5, 2), stamp.nativeCloseoutNpcRecovery);

    // HprPet.java:45-52 / MprPet.java:49-56: non-positive interval -> 20 s, non-positive amount -> 1.
    for (int raw : new int[] {0, -1}) {
      var defaults = petWith(raw, raw, raw, raw);
      assertEquals(
          new NativeCloseoutCompanionRecoveryData.Data(20, 1, 20, 1),
          NativeSourceNpcFactoryType.stamp(defaults).nativeCloseoutNpcRecovery,
          "defaults for raw template value " + raw);
    }
  }

  @Test
  void summonAndTamedActorsAreStampedAsL1SummonWithTheirOwnTemplate() throws Exception {
    try (var ignored =
        templates(
            snapshot(
                List.of(template(SUMMON_NPC, 6, 9, 7, 3), template(TAMED_NPC, 2, 1, 0, 0)),
                List.of()))) {
      var summon = NativeSourceNpcFactoryType.stamp(summon());
      assertEquals(6, summon.kind());
      assertEquals(SUMMON_NPC, summon.npc());
      assertEquals(
          new NativeCloseoutCompanionRecoveryData.Data(6, 9, 7, 3),
          summon.nativeCloseoutNpcRecovery);
      var tamed = NativeSourceNpcFactoryType.stamp(tamed());
      assertEquals(6, tamed.kind());
      assertEquals(
          new NativeCloseoutCompanionRecoveryData.Data(2, 1, 20, 1), tamed.nativeCloseoutNpcRecovery);
    }
  }

  @Test
  void unknownOrFailedTemplateSnapshotLeavesTheActorWithoutRecovery() throws Exception {
    assertSame(
        NativeCloseoutControlledNpcData.UNKNOWN,
        NativeCloseoutControlledNpcData.capture(null, PET_NPC));
    var other = snapshot(List.of(template(SUMMON_NPC, 3, 4, 5, 2)), List.of());
    assertSame(
        NativeCloseoutControlledNpcData.UNKNOWN,
        NativeCloseoutControlledNpcData.capture(other, PET_NPC),
        "template row of another NPC is never borrowed");
    var failed =
        new NativeDeNpcTemplateResources.Snapshot(
            "fixture", Map.of(), Map.of(), Map.of(), -1, Set.of(), Set.of(), 0, 0L, 0L, 0L, 1L, true);
    assertSame(
        NativeCloseoutControlledNpcData.UNKNOWN,
        NativeCloseoutControlledNpcData.capture(failed, PET_NPC));

    BattleCompanion pet;
    try (var ignored = templates(other)) {
      pet = pet();
    }
    assertNull(NativeSourceNpcFactoryType.stamp(pet).nativeCloseoutNpcRecovery);
    var arena = new Arena(pet);
    pet.applyDamage(20);
    for (int tick = 1; tick <= 40; tick++) assertTrue(arena.tick(tick).isEmpty());
    assertEquals(58, hp(pet), "no template, no recovery");
  }

  // ---------------------------------------------------------------- cadence and amounts

  @Test
  void hpAndMpRecoverOnTheirOwnIntervalsByTheTemplateAmount() throws Exception {
    var pet = petWith(3, 4, 5, 2);
    var arena = new Arena(pet);
    pet.applyDamage(20); // 78 -> 58
    assertTrue(pet.debitManaIfStrictlyAbove(10).debited()); // 17 -> 7
    int hp = 58, mp = 7;
    for (int tick = 1; tick <= 15; tick++) {
      var values = arena.tick(tick);
      if (tick % 3 == 0) hp += 4;
      if (tick % 5 == 0) mp += 2;
      assertEquals(hp, hp(pet), "HP after tick " + tick);
      assertEquals(mp, mp(pet), "MP after tick " + tick);
      // L1PetInstance.java:1093: the meter follows an HP change only; MP has no owner packet.
      assertEquals(tick % 3 == 0 ? 1 : 0, values.size(), "meters at tick " + tick);
    }
    assertEquals(78, hp(pet));
    assertEquals(13, mp(pet));
  }

  @Test
  void recoveryNeverExceedsTheMaximumAndStopsWhenFull() throws Exception {
    var pet = petWith(3, 4, 5, 2);
    var arena = new Arena(pet);
    pet.applyDamage(1); // 77: +4 would overshoot
    assertTrue(pet.debitManaIfStrictlyAbove(1).debited()); // 16: +2 would overshoot
    var values = arena.tick(15);
    assertEquals(78, hp(pet));
    assertEquals(17, mp(pet));
    assertEquals(1, values.size());
    assertEquals(100, values.get(0).ratio());
    // HprPet.java:40 / MprPet.java:44: full actors are skipped, so no repeated meter either.
    assertTrue(arena.tick(30).isEmpty());
    assertEquals(78, hp(pet));
    assertEquals(17, mp(pet));
  }

  @Test
  void sourceDefaultCadenceIsOnePointEveryTwentySeconds() throws Exception {
    var pet = petWith(0, 0, 0, 0);
    var arena = new Arena(pet);
    pet.applyDamage(20);
    assertTrue(pet.debitManaIfStrictlyAbove(10).debited());
    for (int tick = 1; tick < 20; tick++) assertTrue(arena.tick(tick).isEmpty(), "tick " + tick);
    assertEquals(58, hp(pet));
    assertEquals(7, mp(pet));
    assertEquals(1, arena.tick(20).size());
    assertEquals(59, hp(pet));
    assertEquals(8, mp(pet));
    assertTrue(arena.tick(21).isEmpty());
    assertEquals(1, arena.tick(40).size());
    assertEquals(60, hp(pet));
    assertEquals(9, mp(pet));
  }

  @Test
  void summonsAndTamedActorsRecoverWithTheSameRule() throws Exception {
    BattleCompanion summon, tamed;
    try (var ignored =
        templates(
            snapshot(
                List.of(template(SUMMON_NPC, 2, 10, 4, 5), template(TAMED_NPC, 4, 3, 2, 1)),
                List.of()))) {
      summon = summon(); // 500 HP / 50 MP
      tamed = tamed(); // 100 HP / 20 MP
    }
    var arena = new Arena(summon, tamed);
    summon.applyDamage(100);
    tamed.applyDamage(50);
    assertTrue(tamed.debitManaIfStrictlyAbove(5).debited());
    assertEquals(1, arena.tick(2).size(), "summon HP interval only");
    assertEquals(410, hp(summon));
    assertEquals(50, hp(tamed));
    assertEquals(16, mp(tamed), "tamed MP interval");
    assertEquals(2, arena.tick(4).size());
    assertEquals(420, hp(summon));
    assertEquals(53, hp(tamed));
    assertEquals(17, mp(tamed));
  }

  // ---------------------------------------------------------------- owner HP meter

  @Test
  void meterIsTheSourceHpMeterPacketForTheOwnerOnly() throws Exception {
    var pet = petWith(3, 4, 5, 2);
    var arena = new Arena(pet);
    pet.applyDamage(40); // 38
    var values = arena.tick(3); // 42
    assertEquals(1, values.size());
    var value = values.get(0);
    assertSame(arena.s.owner, value.owner());
    assertSame(pet, value.pet());
    assertEquals(0x9000_0303L, value.id());
    assertEquals(100 * 42 / 78, value.ratio(), "S_HPMeter(id, 100 * hp / maxHp), truncated");
    assertTrue(NativeCloseoutCompanionRecovery.current(value));

    // serverpackets/S_HPMeter.java:39-43: writeC(opcode), writeD(objId), writeC(ratio).
    byte[] packet = NativeCloseoutCompanionRecovery.packet(value);
    assertArrayEquals(
        new byte[] {(byte) Protocol.S_HIT_RATIO, 0x03, 0x03, 0x00, (byte) 0x90, (byte) 53}, packet);

    var prior = NativeCloseoutCompanionRecovery.output.get();
    try {
      NativeCloseoutCompanionRecovery.output.remove();
      assertTrue(
          NativeCloseoutCompanionRecovery.allowFrame(true, arena.s.caster, packet),
          "outside a recovery output the original predicate decides");
      assertFalse(NativeCloseoutCompanionRecovery.allowFrame(false, arena.s.owner, packet));

      NativeCloseoutCompanionRecovery.output.set(value);
      assertTrue(NativeCloseoutCompanionRecovery.allowFrame(true, arena.s.owner, packet));
      assertFalse(
          NativeCloseoutCompanionRecovery.allowFrame(false, arena.s.owner, packet),
          "an original rejection is never overridden");
      assertFalse(
          NativeCloseoutCompanionRecovery.allowFrame(true, arena.s.caster, packet),
          "other PCs in the room never receive the pet's meter");
      assertFalse(
          NativeCloseoutCompanionRecovery.allowFrame(true, arena.s.bystander, packet));
      byte[] wrongRatio = packet.clone();
      wrongRatio[5] = 54;
      assertFalse(NativeCloseoutCompanionRecovery.allowFrame(true, arena.s.owner, wrongRatio));
      byte[] wrongObject = packet.clone();
      wrongObject[1] = 0x04;
      assertFalse(NativeCloseoutCompanionRecovery.allowFrame(true, arena.s.owner, wrongObject));
      byte[] wrongOpcode = packet.clone();
      wrongOpcode[0] = (byte) Protocol.S_HIT_POINT;
      assertFalse(NativeCloseoutCompanionRecovery.allowFrame(true, arena.s.owner, wrongOpcode));
      assertFalse(NativeCloseoutCompanionRecovery.allowFrame(true, arena.s.owner, null));

      // The existing final-frame predicate now carries the post-guard.
      assertTrue(NativeSourceNpcMagicEffects.allowFrame(arena.s.owner, packet));
      assertFalse(NativeSourceNpcMagicEffects.allowFrame(arena.s.caster, packet));
    } finally {
      if (prior == null) NativeCloseoutCompanionRecovery.output.remove();
      else NativeCloseoutCompanionRecovery.output.set(prior);
    }
  }

  @Test
  void ratioAndBoundFollowSourceIntegerArithmetic() {
    assertEquals(0, NativeCloseoutCompanionRecovery.ratio(0, 78));
    assertEquals(1, NativeCloseoutCompanionRecovery.ratio(1, 78));
    assertEquals(98, NativeCloseoutCompanionRecovery.ratio(77, 78));
    assertEquals(100, NativeCloseoutCompanionRecovery.ratio(78, 78));
    assertEquals(0, NativeCloseoutCompanionRecovery.ratio(5, 0));
    assertEquals(78, NativeCloseoutCompanionRecovery.bounded(77, 4, 78));
    assertEquals(62, NativeCloseoutCompanionRecovery.bounded(58, 4, 78));
    assertEquals(
        Integer.MAX_VALUE,
        NativeCloseoutCompanionRecovery.bounded(Integer.MAX_VALUE - 1, 9, Integer.MAX_VALUE));
  }

  // ---------------------------------------------------------------- death / dismissal / removal

  @Test
  void deadPetNeitherRecoversNorProducesAMeter() throws Exception {
    var pet = petWith(3, 4, 5, 2);
    var arena = new Arena(pet);
    assertTrue(pet.debitManaIfStrictlyAbove(10).debited());
    assertTrue(pet.applyDamage(78).diedNow());
    for (int tick = 1; tick <= 30; tick++) assertTrue(arena.tick(tick).isEmpty(), "tick " + tick);
    assertEquals(0, hp(pet), "HprPet.java:28-34");
    assertEquals(7, mp(pet), "MprPet.java:32-42");
  }

  @Test
  void dismissedOrRemovedActorsNeverRecover() throws Exception {
    BattleCompanion summon, tamed;
    try (var ignored =
        templates(
            snapshot(
                List.of(template(SUMMON_NPC, 1, 10, 1, 5), template(TAMED_NPC, 1, 3, 1, 1)),
                List.of()))) {
      summon = summon();
      tamed = tamed();
    }
    var arena = new Arena(summon, tamed);
    summon.applyDamage(100);
    tamed.applyDamage(50);
    assertEquals(BattleCompanion.CommandResult.DISMISSED, summon.apply("dismiss"));
    arena.bag.remove(tamed); // released / returned to nature: no longer the owner's actor
    for (int tick = 1; tick <= 5; tick++) assertTrue(arena.tick(tick).isEmpty());
    assertEquals(400, hp(summon));
    assertEquals(50, hp(tamed));
    assertFalse(summon.snapshot().active(), "dismissed summon stays inactive");
  }

  @Test
  void unstampedArenaCompanionIsNotASourcePetOrSummon() throws Exception {
    BattleCompanion arenaCompanion;
    try (var ignored =
        templates(snapshot(List.of(template(BattleCompanion.NPC_ID, 1, 5, 1, 5)), List.of()))) {
      arenaCompanion = new BattleCompanion(0x6000_0213L);
    }
    arenaCompanion.resetForBattle(100, 100);
    var arena = new Arena(arenaCompanion);
    int max = hp(arenaCompanion);
    arenaCompanion.applyDamage(10);
    assertTrue(arena.tick(1).isEmpty());
    assertEquals(max - 10, hp(arenaCompanion));
  }

  // ---------------------------------------------------------------- room / owner admission

  @Test
  void onlyARunningBattleRoomOrARegisteredWorldOwnerTicks() throws Exception {
    var pet = petWith(1, 4, 1, 2);
    var arena = new Arena(pet);
    pet.applyDamage(40);

    put(arena.match, "phase", BattleRules.Phase.FINISHED);
    assertTrue(arena.tick(1).isEmpty(), "settled match");
    put(arena.match, "phase", BattleRules.Phase.COUNTDOWN);
    assertTrue(arena.tick(2).isEmpty(), "countdown");
    put(arena.match, "phase", BattleRules.Phase.RUNNING);
    put(arena.room, "disconnectResolutionPending", true);
    assertTrue(arena.tick(3).isEmpty(), "disconnect resolution pending");
    put(arena.room, "disconnectResolutionPending", false);
    put(arena.room, "battleStarted", false);
    assertTrue(arena.tick(4).isEmpty(), "battle not started");
    put(arena.room, "battleStarted", true);

    put(arena.s.owner, "phase", NativeSourceAreaPairFirstTest.phase("WORLD"));
    assertTrue(arena.tick(5).isEmpty(), "WORLD owner that is not registered in the Hub");
    put(arena.s.owner, "phase", NativeSourceAreaPairFirstTest.phase("BATTLE"));

    ((Set<?>) CombatAccess.field(arena.s.hub, "connectedSessions")).remove(arena.s.owner);
    assertTrue(arena.tick(6).isEmpty(), "disconnected owner");
    assertEquals(38, hp(pet));

    @SuppressWarnings("unchecked")
    var connected = (Set<ClientSession>) CombatAccess.field(arena.s.hub, "connectedSessions");
    connected.add(arena.s.owner);
    assertEquals(1, arena.tick(7).size(), "back to a running room");
    assertEquals(42, hp(pet));
  }

  // ---------------------------------------------------------------- retirement during lock waits

  /** Owner action lock whose first acquisition performs a retirement exactly inside the wait. */
  static final class RetiringLock extends ReentrantLock {
    final Runnable retire;
    final AtomicBoolean done = new AtomicBoolean();

    RetiringLock(Runnable retire) {
      this.retire = retire;
    }

    @Override
    public void lock() {
      if (done.compareAndSet(false, true)) retire.run();
      super.lock();
    }
  }

  @Test
  void retirementWhileWaitingForTheOwnerLockCancelsTheTick() throws Exception {
    Map<String, java.util.function.BiConsumer<Arena, BattleCompanion>> retirements =
        new LinkedHashMap<>();
    retirements.put("pet killed", (a, p) -> p.applyDamage(1_000));
    retirements.put("pet dismissed", (a, p) -> p.apply("dismiss"));
    retirements.put("pet removed from the owner's list", (a, p) -> a.bag.remove(p));
    retirements.put(
        "owner's list replaced",
        (a, p) -> put(a.s.owner, "tamedCompanions", new ArrayList<>(List.of(p))));
    retirements.put("owner left the room", (a, p) -> put(a.s.owner, "currentRoomId", 8L));
    retirements.put(
        "owner relogged (status replaced)",
        (a, p) ->
            put(
                a.s.owner,
                "timedStatus",
                NativeSourceAreaPairFirstTest.status(new Object(), new Object())));
    retirements.put("owner disconnected", (a, p) -> put(a.s.owner, "sourceItemDisconnected", true));
    retirements.put(
        "room replaced",
        (a, p) -> {
          @SuppressWarnings("unchecked")
          var rooms = (Map<Long, Object>) CombatAccess.field(a.s.hub, "rooms");
          rooms.put(7L, new Arena().room);
        });
    retirements.put("match finished", (a, p) -> put(a.match, "phase", BattleRules.Phase.FINISHED));
    retirements.put(
        "server lifetime stopped", (a, p) -> put(a.s.life, "mode", NativePlayerLifetime.Mode.STOPPED));
    retirements.put("Hub motion closed", (a, p) -> put(a.s.hub, "nativeStaticMotionClosed", true));
    retirements.put(
        "clock uninstalled", (a, p) -> put(a.s.hub, NativeCloseoutCompanionRecovery.CLOCK, null));

    for (var entry : retirements.entrySet()) {
      var pet = petWith(1, 4, 1, 2);
      var arena = new Arena(pet);
      pet.applyDamage(40);
      assertTrue(pet.debitManaIfStrictlyAbove(10).debited());
      var gate = new RetiringLock(() -> entry.getValue().accept(arena, pet));
      put(arena.s.owner, "sourcePlayerActionLock", gate);
      var values = arena.tick(1);
      assertTrue(gate.done.get(), "retirement ran inside the lock wait: " + entry.getKey());
      assertTrue(values.isEmpty(), "no meter after " + entry.getKey());
      int expectedHp = entry.getKey().equals("pet killed") ? 0 : 38;
      assertEquals(expectedHp, hp(pet), "no HP recovery after " + entry.getKey());
      assertEquals(7, mp(pet), "no MP recovery after " + entry.getKey());
      assertFalse(gate.isLocked(), "owner lock released after " + entry.getKey());
    }
  }

  @Test
  void meterComputedBeforeARetirementIsDroppedAtTheFinalFrame() throws Exception {
    Map<String, java.util.function.BiConsumer<Arena, BattleCompanion>> retirements =
        new LinkedHashMap<>();
    retirements.put("pet killed", (a, p) -> p.applyDamage(1_000));
    retirements.put("pet dismissed", (a, p) -> p.apply("dismiss"));
    retirements.put("pet damaged again (ratio is stale)", (a, p) -> p.applyDamage(10));
    retirements.put("owner left the room", (a, p) -> put(a.s.owner, "currentRoomId", 8L));
    retirements.put("owner disconnected", (a, p) -> put(a.s.owner, "sourceItemDisconnected", true));
    retirements.put("match finished", (a, p) -> put(a.match, "phase", BattleRules.Phase.FINISHED));
    retirements.put(
        "server lifetime stopped", (a, p) -> put(a.s.life, "mode", NativePlayerLifetime.Mode.STOPPED));
    retirements.put(
        "clock uninstalled", (a, p) -> put(a.s.hub, NativeCloseoutCompanionRecovery.CLOCK, null));

    for (var entry : retirements.entrySet()) {
      var pet = petWith(1, 4, 1, 2);
      var arena = new Arena(pet);
      pet.applyDamage(40);
      var values = arena.tick(1);
      assertEquals(1, values.size());
      var value = values.get(0);
      byte[] packet = NativeCloseoutCompanionRecovery.packet(value);
      assertTrue(NativeCloseoutCompanionRecovery.current(value));
      entry.getValue().accept(arena, pet);
      assertFalse(NativeCloseoutCompanionRecovery.current(value), entry.getKey());
      var prior = NativeCloseoutCompanionRecovery.output.get();
      try {
        NativeCloseoutCompanionRecovery.output.set(value);
        assertFalse(
            NativeCloseoutCompanionRecovery.allowFrame(true, arena.s.owner, packet),
            "final frame rejects after " + entry.getKey());
      } finally {
        if (prior == null) NativeCloseoutCompanionRecovery.output.remove();
        else NativeCloseoutCompanionRecovery.output.set(prior);
      }
      // send() exits on the same predicate before it opens a frame or touches the socket.
      NativeCloseoutCompanionRecovery.send(value);
      assertNull(NativeCloseoutCompanionRecovery.output.get());
    }
  }

  // ---------------------------------------------------------------- the clock itself

  @Test
  void clockCountsFromOneAndIgnoresAStaleOrStoppedInstallation() throws Exception {
    var pet = petWith(3, 4, 5, 2);
    var arena = new Arena(pet);
    assertTrue(pet.debitManaIfStrictlyAbove(10).debited()); // full HP: MP-only ticks send nothing

    for (int run = 1; run <= 4; run++) arena.clock.run();
    assertEquals(4, arena.clock.seconds, "PetMprTimer.java:92 increments before use");
    assertEquals(7, mp(pet));
    arena.clock.run();
    assertEquals(5, arena.clock.seconds);
    assertEquals(9, mp(pet), "MP interval 5 reached on the fifth run");

    put(arena.s.life, "mode", NativePlayerLifetime.Mode.STOPPED);
    for (int run = 6; run <= 10; run++) arena.clock.run();
    assertEquals(10, arena.clock.seconds, "the clock keeps counting while admission is closed");
    assertEquals(9, mp(pet), "but nothing recovers");
    put(arena.s.life, "mode", NativePlayerLifetime.Mode.OPEN);

    var stale = arena.clock;
    NativeCloseoutCompanionRecovery.close(arena.s.hub);
    assertNull(CombatAccess.field(arena.s.hub, NativeCloseoutCompanionRecovery.CLOCK));
    for (int run = 0; run < 10; run++) stale.run();
    assertEquals(10, stale.seconds, "a closed clock never ticks again");
    assertEquals(9, mp(pet));
    NativeCloseoutCompanionRecovery.close(arena.s.hub); // idempotent
  }
}
