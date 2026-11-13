package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;

/**
 * T123 (Issue #354, G14): controlled-NPC primary / secondary retirement while a Source 76 (Mass
 * Slow) or 167 (Wind Shackle) cast waits for the original roster locks. Ported from candidate
 * {@code 3ccbb6f5…}.
 *
 * <p>The fixture is a minimal memory-only BATTLE scene (caster PC, pet owner PC, owner's controlled
 * pet) built with {@code Unsafe.allocateInstance}: no socket, DB, thread pool or sleep. "Waiting"
 * is made deterministic by a lock whose {@code tryLock()} performs the retirement synchronously and
 * then reports contention, i.e. the retirement lands exactly inside the original lock-wait window.
 */
final class NativeSourceAreaPairFirstTest {

  // ---------------------------------------------------------------- reflection fixture helpers

  static <T> T allocate(Class<T> type) {
    try {
      Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
      field.setAccessible(true);
      return type.cast(((sun.misc.Unsafe) field.get(null)).allocateInstance(type));
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  static Field field(Class<?> type, String name) {
    for (Class<?> owner = type; owner != null; owner = owner.getSuperclass())
      try {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        return f;
      } catch (NoSuchFieldException absent) {
        // keep walking
      }
    throw new AssertionError("missing field " + type.getName() + "#" + name);
  }

  static void put(Object target, String name, Object value) {
    try {
      field(target.getClass(), name).set(target, value);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  static Object phase(String name) {
    try {
      Class type = Class.forName("local.lito381.server.ClientSession$Phase");
      return Enum.valueOf(type, name);
    } catch (ClassNotFoundException e) {
      throw new AssertionError(e);
    }
  }

  /** Timed status whose Source lifecycle carries the given owner / life tokens. */
  static BattleTimedStatus status(Object playerOwner, Object life) {
    var timed = new BattleTimedStatus();
    var state = allocate(CombatPoisonLifecycle.State.class);
    put(state, "sourcePlayerOwner", playerOwner);
    put(state, "sourceNpcOwner", life);
    put(timed, "sourcePoisonLifecycle", state);
    return timed;
  }

  static ClientSession player(GameHub hub, String name, int x, int y) {
    var pc = allocate(ClientSession.class);
    put(pc, "hub", hub);
    put(pc, "phase", phase("BATTLE"));
    put(pc, "selectedCharacter", name);
    put(pc, "selectedClassicServerName", "lito");
    put(pc, "account", "acct-" + name);
    put(pc, "characters", List.of());
    put(pc, "tamedCompanions", new ArrayList<BattleCompanion>());
    put(pc, "currentRoomId", 7L);
    put(pc, "classicWorldMapId", 0);
    put(pc, "classicWorldInstanceId", 0);
    put(pc, "positionX", x);
    put(pc, "positionY", y);
    put(pc, "playerHp", 100);
    put(pc, "sourcePlayerActionLock", new ReentrantLock());
    return pc;
  }

  static NativePlayerCommit.Binding binding(ClientSession pc, Object owner) {
    return new NativePlayerCommit.Binding(
        owner,
        pc.playerObjectId(),
        pc.sourceCharacterIdentity(),
        pc.sourceAccountIdentity(),
        pc.selectedCharacterName(),
        "BATTLE",
        7L,
        0,
        0);
  }

  /** Memory-only BATTLE scene: caster attacks the owner's controlled pet with 76 / 167. */
  static final class Scene {
    static final long PET_ID = 0x5000_0001L;
    final GameHub hub = allocate(GameHub.class);
    final NativePlayerLifetime life = allocate(NativePlayerLifetime.class);
    final ClientSession caster = player(hub, "Caster", 32700, 32800);
    final ClientSession owner = player(hub, "Owner", 32703, 32801);
    final ClientSession bystander = player(hub, "Bystander", 32702, 32802);
    final Object casterOwner = new Object(), ownerOwner = new Object(), bystanderOwner = new Object();
    final Object casterLife = new Object(), ownerLife = new Object(), bystanderLife = new Object();
    final Object petLife = new Object();
    final NativePlayerCommit.Binding casterBinding, ownerBinding, bystanderBinding;
    final BattleCompanion pet = allocate(BattleCompanion.class);
    final NativeAreaMaterialAction.Npc npc;
    final NativeAreaMaterialAction.Context c = allocate(NativeAreaMaterialAction.Context.class);
    final NativeSourceRawDamage.Member casterMember, ownerMember, bystanderMember;

    Scene(int skill) {
      put(life, "mode", NativePlayerLifetime.Mode.OPEN);
      put(hub, "sourcePlayerCommitLifetime", life);
      put(caster, "timedStatus", status(casterOwner, casterLife));
      put(owner, "timedStatus", status(ownerOwner, ownerLife));
      put(bystander, "timedStatus", status(bystanderOwner, bystanderLife));
      casterBinding = binding(caster, casterOwner);
      ownerBinding = binding(owner, ownerOwner);
      bystanderBinding = binding(bystander, bystanderOwner);
      put(pet, "timedStatus", status(null, petLife));
      put(pet, "hp", 50);
      put(owner, "battleCompanion", pet);
      var ref = new NativeCombatAttacker.Ref(2, PET_ID, petLife, 0L, ownerBinding, 7L, 0, 0);
      npc = new NativeAreaMaterialAction.Npc(3, PET_ID, owner, pet, ref);
      var bindings = new IdentityHashMap<ClientSession, NativePlayerCommit.Binding>();
      bindings.put(caster, casterBinding);
      bindings.put(owner, ownerBinding);
      bindings.put(bystander, bystanderBinding);
      var npcs = new HashMap<Long, NativeAreaMaterialAction.Npc>();
      npcs.put(PET_ID, npc);
      put(c, "pc", caster);
      put(c, "hub", hub);
      put(c, "caster", casterBinding);
      put(c, "phase", "BATTLE");
      put(c, "room", 7L);
      put(c, "players", List.of(caster, owner, bystander));
      put(c, "bindings", bindings);
      put(c, "npcs", npcs);
      casterMember = new NativeSourceRawDamage.Member(caster, casterBinding, casterLife, 32700, 32800);
      ownerMember = new NativeSourceRawDamage.Member(owner, ownerBinding, ownerLife, 32703, 32801);
      bystanderMember =
          new NativeSourceRawDamage.Member(bystander, bystanderBinding, bystanderLife, 32702, 32802);
      var members = new IdentityHashMap<ClientSession, NativeSourceRawDamage.Member>();
      members.put(caster, casterMember);
      members.put(owner, ownerMember);
      members.put(bystander, bystanderMember);
      c.nativeSourceFirstPlayers = new NativeSourceFirstPlayers.Snapshot(members);
      var source =
          new NativeSourceSelfFinalFrame.First(
              new NativeSourceSelfFinalFrame.Participant(
                  caster,
                  casterBinding,
                  CombatAccess.field(caster, "timedStatus"),
                  casterLife,
                  false,
                  false),
              life,
              hub,
              null,
              null);
      var proof =
          new NativeSourceTargetNegativeLife.Proof(
              life,
              casterMember,
              CombatAccess.field(caster, "timedStatus"),
              owner,
              ownerMember,
              CombatAccess.field(owner, "timedStatus"),
              npc,
              CombatAccess.field(pet, "timedStatus"),
              petLife,
              null);
      // Same First value the original capture() builds before any roster lock is taken.
      c.nativeSourceAreaPairFirst =
          new NativeSourceAreaPairFirst.First(
              skill, source, proof, NativeSourceAreaPairFirst.players(c));
    }
  }

  // ---------------------------------------------------------------- primary identity

  @Test
  void freshControlledPrimaryIsCurrentForBothSkills() {
    for (int skill : new int[] {76, 167}) {
      var s = new Scene(skill);
      assertTrue(NativeSourceAreaPairFirst.current(s.c), "fresh first primary for " + skill);
      var first = (NativeSourceAreaPairFirst.First) s.c.nativeSourceAreaPairFirst;
      assertEquals(
          Set.of(s.caster, s.owner, s.bystander),
          first.playerStatuses().keySet(),
          "every first-accepted PC status is captured");
    }
  }

  /** Each retirement of the controlled primary that can land inside the lock wait. */
  static final Map<String, java.util.function.Consumer<Scene>> RETIREMENTS = new LinkedHashMap<>();

  static {
    RETIREMENTS.put("pet killed", s -> put(s.pet, "hp", 0));
    RETIREMENTS.put("pet removed from first scene", s -> s.c.npcs.remove(Scene.PET_ID));
    RETIREMENTS.put(
        "pet replaced by owner (dismiss + new summon)",
        s -> put(s.owner, "battleCompanion", allocate(BattleCompanion.class)));
    RETIREMENTS.put(
        "pet life token replaced",
        s -> put(CombatAccess.field(s.pet, "timedStatus"), "sourcePoisonLifecycle",
            CombatAccess.field(status(null, new Object()), "sourcePoisonLifecycle")));
    RETIREMENTS.put(
        "pet timed status replaced",
        s -> put(s.pet, "timedStatus", status(null, s.petLife)));
    RETIREMENTS.put("owner died", s -> put(s.owner, "playerHp", 0));
    RETIREMENTS.put(
        "owner timed status replaced (relog / revive)",
        s -> put(s.owner, "timedStatus", status(s.ownerOwner, s.ownerLife)));
    RETIREMENTS.put("owner left the room", s -> put(s.owner, "currentRoomId", 8L));
    RETIREMENTS.put("caster died", s -> put(s.caster, "playerHp", 0));
    RETIREMENTS.put("caster moved", s -> put(s.caster, "positionX", 32701));
    RETIREMENTS.put(
        "caster timed status replaced",
        s -> put(s.caster, "timedStatus", status(s.casterOwner, s.casterLife)));
    RETIREMENTS.put(
        "server lifetime stopped", s -> put(s.life, "mode", NativePlayerLifetime.Mode.STOPPED));
    RETIREMENTS.put(
        "server lifetime replaced",
        s -> {
          var next = allocate(NativePlayerLifetime.class);
          put(next, "mode", NativePlayerLifetime.Mode.OPEN);
          put(s.hub, "sourcePlayerCommitLifetime", next);
        });
  }

  @Test
  void everyPrimaryRetirementInvalidatesTheFirstIdentity() {
    for (int skill : new int[] {76, 167})
      for (var entry : RETIREMENTS.entrySet()) {
        var s = new Scene(skill);
        assertTrue(NativeSourceAreaPairFirst.current(s.c), "baseline " + entry.getKey());
        entry.getValue().accept(s);
        assertFalse(NativeSourceAreaPairFirst.current(s.c), skill + ": " + entry.getKey());
      }
  }

  @Test
  void secondaryOnlyRetirementKeepsThePrimaryCurrent() {
    var s = new Scene(76);
    put(s.bystander, "timedStatus", status(s.bystanderOwner, s.bystanderLife));
    put(s.bystander, "playerHp", 0);
    assertTrue(NativeSourceAreaPairFirst.current(s.c), "a secondary PC never cancels the cast");
  }

  @Test
  void rejectedCaptureAndMissingCaptureAreDistinguished() {
    var s = new Scene(76);
    s.c.nativeSourceAreaPairFirst = null;
    assertTrue(NativeSourceAreaPairFirst.current(s.c), "no controlled primary: original path");
    assertTrue(NativeSourceAreaPairFirst.current((NativeAreaMaterialAction.Context) null));
    s.c.nativeSourceAreaPairFirst = Boolean.FALSE;
    assertFalse(NativeSourceAreaPairFirst.current(s.c), "first identity could not be proven");
  }

  // ---------------------------------------------------------------- controllable lock wait

  /** Original roster lock: first contended tryLock() runs the retirement, then reports busy. */
  static final class RetiringLock extends ReentrantLock {
    final Runnable retire;
    final AtomicInteger tries = new AtomicInteger();

    RetiringLock(Runnable retire) {
      this.retire = retire;
    }

    @Override
    public boolean tryLock() {
      if (tries.getAndIncrement() == 0) {
        retire.run();
        return false;
      }
      return super.tryLock();
    }
  }

  @Test
  void primaryRetiredWhileWaitingForOwnerLockDropsTheCastBeforeAnyEffect() throws Exception {
    for (int skill : new int[] {76, 167})
      for (var entry : RETIREMENTS.entrySet()) {
        var s = new Scene(skill);
        var casterLock = (ReentrantLock) CombatAccess.field(s.caster, "sourcePlayerActionLock");
        var gate = new RetiringLock(() -> entry.getValue().accept(s));
        put(s.owner, "sourcePlayerActionLock", gate);
        var ran = new AtomicBoolean();
        NativeAreaMaterialAction.nativeCompanionAreaCapturedRun(
            s.caster, skill, () -> ran.set(true), s.c);
        String why = skill + ": " + entry.getKey();
        assertFalse(ran.get(), "no cost / RNG / packet body after " + why);
        assertEquals(1, gate.tries.get(), "first retry check exits the wait loop after " + why);
        assertFalse(casterLock.isLocked(), "already-taken roster locks released after " + why);
        assertFalse(gate.isLocked(), "owner lock never taken after " + why);
      }
  }

  @Test
  void rejectedCaptureExitsTheRosterWaitImmediately() throws Exception {
    var s = new Scene(76);
    s.c.nativeSourceAreaPairFirst = Boolean.FALSE;
    var gate = new RetiringLock(() -> {});
    put(s.owner, "sourcePlayerActionLock", gate);
    var ran = new AtomicBoolean();
    NativeAreaMaterialAction.nativeCompanionAreaCapturedRun(s.caster, 76, () -> ran.set(true), s.c);
    assertFalse(ran.get());
    assertEquals(1, gate.tries.get());
    assertFalse(NativeSourceAreaPairFirst.retry(null, s.caster, s.c), "short-circuits before binding");
    assertFalse(NativeSourceAreaPairFirst.scene(s.c));
  }

  // ---------------------------------------------------------------- secondary PCs (76 only)

  @Test
  void secondaryWhoseStatusWasReplacedDuringTheWaitIsNotRolled() {
    var s = new Scene(76);
    assertTrue(NativeSourceAreaPairFirst.secondaryPlayer(s.c, s.bystander));
    put(s.bystander, "timedStatus", status(s.bystanderOwner, s.bystanderLife));
    assertFalse(NativeSourceAreaPairFirst.secondaryPlayer(s.c, s.bystander), "stale secondary");
    assertTrue(NativeSourceAreaPairFirst.secondaryPlayer(s.c, s.owner), "others unaffected");
    assertFalse(
        NativeSourceAreaPlayerProof.player(s.c, s.bystander),
        "original roster predicate rejects the stale secondary first");
  }

  @Test
  void secondaryJoiningAfterTheFirstSnapshotIsNotRolled() {
    var s = new Scene(76);
    var late = player(s.hub, "Late", 32701, 32801);
    put(late, "timedStatus", status(new Object(), new Object()));
    assertFalse(NativeSourceAreaPairFirst.secondaryPlayer(s.c, late));
  }

  @Test
  void secondaryFilterOnlyAppliesToSkill76WithACapturedPrimary() {
    var s = new Scene(167);
    put(s.bystander, "timedStatus", status(s.bystanderOwner, s.bystanderLife));
    assertTrue(NativeSourceAreaPairFirst.secondaryPlayer(s.c, s.bystander), "167 is single target");
    s.c.nativeSourceAreaPairFirst = null;
    assertTrue(NativeSourceAreaPairFirst.secondaryPlayer(s.c, s.bystander), "no capture: original");
    s.c.nativeSourceAreaPairFirst = Boolean.FALSE;
    assertTrue(NativeSourceAreaPairFirst.secondaryPlayer(s.c, s.bystander));
  }

  @Test
  void secondaryIteratorWrapsOnlyCapturedSkill76() {
    var s = new Scene(76);
    Iterator<Object> original = new ArrayList<Object>(List.of(s.bystander)).iterator();
    var prior = NativeAreaMaterialAction.current.get();
    try {
      NativeAreaMaterialAction.current.remove();
      assertSame(original, NativeSourceAreaPairFirst.secondaryIterator(original), "no context");
      NativeAreaMaterialAction.current.set(s.c);
      var cursor = NativeSourceAreaPairFirst.secondaryIterator(original);
      assertNotSame(original, cursor);
      put(s.bystander, "timedStatus", status(s.bystanderOwner, s.bystanderLife));
      assertFalse(cursor.hasNext(), "replaced secondary filtered before probability / RNG");
      assertThrows(NoSuchElementException.class, cursor::next);
      var bad = NativeSourceAreaPairFirst.secondaryIterator(List.<Object>of("not a PC").iterator());
      assertThrows(IllegalStateException.class, bad::hasNext);
      var wind = new Scene(167);
      NativeAreaMaterialAction.current.set(wind.c);
      assertSame(original, NativeSourceAreaPairFirst.secondaryIterator(original), "167 untouched");
      wind.c.nativeSourceAreaPairFirst = null;
      assertSame(original, NativeSourceAreaPairFirst.secondaryIterator(original));
    } finally {
      if (prior == null) NativeAreaMaterialAction.current.remove();
      else NativeAreaMaterialAction.current.set(prior);
    }
  }

  // ---------------------------------------------------------------- Aim wiring

  @Test
  void massSlowAimValidityAndCloseAreGatedByTheFirstIdentity() {
    var s = new Scene(76);
    var aim = allocate(NativeSourceMassSlowTargets.Aim.class);
    put(aim, "c", s.c);
    s.c.nativeSourceAreaPairFirst = Boolean.FALSE;
    assertFalse(aim.valid(), "valid() rejects before any original roster/position read");
    s.c.nativeSourceAreaPairFirst = new Scene(76).c.nativeSourceAreaPairFirst;
    aim.close();
    assertNull(s.c.nativeSourceAreaPairFirst, "close() releases the bounded first snapshot");
    assertTrue(aim.closed);
  }

  @Test
  void windAimValidityAndCloseAreGatedByTheFirstIdentity() {
    var s = new Scene(167);
    var aim = allocate(NativeSourceWindTargets.Aim.class);
    put(aim, "c", s.c);
    s.c.nativeSourceAreaPairFirst = Boolean.FALSE;
    assertFalse(aim.valid());
    s.c.nativeSourceAreaPairFirst = new Scene(167).c.nativeSourceAreaPairFirst;
    aim.close();
    assertNull(s.c.nativeSourceAreaPairFirst);
    assertTrue(aim.closed);
  }

  @Test
  void captureIsANoOpOutsideControlledBattlePrimaries() {
    var s = new Scene(76);
    s.c.nativeSourceAreaPairFirst = null;
    // Natural NPC primary (kind 1) and missing target never capture.
    NativeSourceAreaPairFirst.capture(
        s.c, new NativeAreaMaterialAction.Npc(1, 9L, null, null, null), 76);
    assertNull(s.c.nativeSourceAreaPairFirst);
    NativeSourceAreaPairFirst.capture(s.c, null, 76);
    assertNull(s.c.nativeSourceAreaPairFirst);
    // WORLD casts never capture (Source path is BATTLE-only).
    put(s.c, "phase", "WORLD");
    NativeSourceAreaPairFirst.capture(s.c, s.npc, 167);
    assertNull(s.c.nativeSourceAreaPairFirst);
  }
}
