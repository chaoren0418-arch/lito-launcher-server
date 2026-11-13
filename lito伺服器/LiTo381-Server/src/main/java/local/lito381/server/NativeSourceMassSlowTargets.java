package local.lito381.server;

import java.io.*;
import java.util.*;

/**
 * Actual controlled primary, original area player/NPC arithmetic, Source failure by controlled
 * actor class.
 */
final class NativeSourceMassSlowTargets {
  record Output(
      ClientSession receiver, NativePlayerCommit.Binding binding, byte[] packet, String note) {}

  static final class Aim {
    final NativeAreaMaterialAction.Context c;
    final NativeAreaMaterialAction.Npc target;
    final ClientSession player, owner;
    final BattleCompanion pet;
    final NativePlayerLifetime lifetime;
    final int cx, cy, x, y;
    final Object life;
    final NativeCompanionPlacement.State placement;
    final Object pose, scene, master;
    boolean registered;
    volatile boolean closed;
    volatile Output outbound;

    Aim(
        NativeAreaMaterialAction.Context c,
        NativeAreaMaterialAction.Npc target,
        ClientSession player) {
      this.c = c;
      this.target = target;
      this.player = player;
      owner = player != null ? player : (ClientSession) target.owner();
      pet = target == null ? null : (BattleCompanion) target.actor();
      lifetime = NativePlayerLifetime.of(c.hub);
      cx = NativePlayerCommit.integer(c.pc, "positionX");
      cy = NativePlayerCommit.integer(c.pc, "positionY");
      var snapshot = pet == null ? null : pet.snapshot();
      x = pet == null ? NativePlayerCommit.integer(player, "positionX") : snapshot.x();
      y = pet == null ? NativePlayerCommit.integer(player, "positionY") : snapshot.y();
      life =
          NativeSourceMaterialFirstLife.initial(
              NativeCombatAttacker.token(
                  (BattleTimedStatus) CombatAccess.field(pet == null ? player : pet, "timedStatus"),
                  true),
              c,
              player,
              target);
      placement = pet == null ? null : NativeCompanionPlacement.state(pet);
      pose = placement == null ? null : placement.pose;
      scene = placement == null ? null : placement.scene;
      master = placement == null ? null : placement.master;
      NativeSourceAreaPairFirst.capture(this); // T123 (Issue #354, G14)
    }

    void close() {
      NativeSourceAreaPairFirst.close(this); // T123 (Issue #354, G14)
      closed = true;
      outbound = null;
      if (c.nativeSourceMassSlowAim == this) c.nativeSourceMassSlowAim = null;
    }

    boolean valid() {
      if (!NativeSourceAreaPairFirst.current(this)) return false; // T123 (Issue #354, G14)
      if (!NativeSourceMaterialFirstLife.valid(this)) return false;
      if (lifetime.mode == NativePlayerLifetime.Mode.STOPPED
          || (boolean) CombatAccess.field(c.hub, "nativeStaticMotionClosed")
          || !c.scene()
          || !(pet == null ? c.player(player) : c.npc(target.id()))
          || NativePlayerCommit.integer(c.pc, "positionX") != cx
          || NativePlayerCommit.integer(c.pc, "positionY") != cy) return false;
      if (pet == null) {
        if (NativePlayerCommit.integer(player, "positionX") != x
            || NativePlayerCommit.integer(player, "positionY") != y) return false;
      } else {
        var snapshot = pet.snapshot();
        if (snapshot.x() != x || snapshot.y() != y) return false;
      }
      var row = NativeSkillTargetMetadata.row(76);
      if (row.area() != 3 || row.targetTo() != 3 || row.type() != 1 || row.through())
        throw new IllegalStateException("unqualified Source76 target metadata");
      if (!BattleSpellCatalog.isInNativeScreen(cx, cy, x, y)
          || row.ranged() >= 0 && Math.max(Math.abs(cx - x), Math.abs(cy - y)) > row.ranged())
        return false;
      var scope = NativeCompanionTargets.scope(c);
      if (pet != null && (scope == null || owner != c.pc && !scope.owns(c, owner))) return false;
      long id = pet == null ? c.bindings.get(player).object() : target.id();
      return !c.phase.equals("BATTLE")
          || owner == c.pc
          || ForcedCombat.canAttackTarget(c.hub, c.room, c.pc, owner, id);
    }
  }

  static void battle(ClientSession pc, byte[] packet, BattleSpellCatalog.MassSlowSpell spell)
      throws IOException {
    run(pc, packet, spell, false);
  }

  static void world(ClientSession pc, byte[] packet, BattleSpellCatalog.MassSlowSpell spell)
      throws IOException {
    run(pc, packet, spell, true);
  }

  static void original(
      ClientSession pc, byte[] packet, BattleSpellCatalog.MassSlowSpell spell, boolean world)
      throws IOException {
    if (world) NativeAreaMaterialAction.worldSlow(pc, packet, spell);
    else NativeAreaMaterialAction.battleSlow(pc, packet, spell);
  }

  static void run(
      ClientSession pc, byte[] packet, BattleSpellCatalog.MassSlowSpell spell, boolean world)
      throws IOException {
    if (spell.skillId() != 76 || NativeSkillTargetMetadata.size() == 0) {
      original(pc, packet, spell, world);
      return;
    }
    var rawRequest = Protocol.attackSpellRequest(packet);
    if (rawRequest.skillId() != 76) return;
    var c = new NativeAreaMaterialAction.Context(pc);
    var request = NativeSourceNoneTargetRequest.request(c, pc, rawRequest, rawRequest.skillId());
    if (!c.phase.equals(world ? "WORLD" : "BATTLE")) return;
    var n = c.npcs.get(request.targetObjectId());
    var player =
        c.players.stream()
            .filter(p -> c.bindings.get(p).object() == request.targetObjectId())
            .findFirst()
            .orElse(null);
    if (player == null && (n == null || n.kind() != 3)) {
      original(pc, packet, spell, world);
      return;
    }
    if (player != null) n = null;
    var aim = new Aim(c, n, player);
    try (var scope = new NativeCompanionTargets.Scope(c)) {
      if (!scope.valid) return;
      CombatAccess.set(c, "nativeCompanionTargetScope", scope);
      c.nativeSourceMassSlowAim = aim;
      CombatAccess.set(c, NativeCompanionNegative.STAGE, 1);
      var prior = NativeAreaMaterialAction.current.get();
      NativeAreaMaterialAction.current.set(c);
      try {
        NativeAreaMaterialAction.nativeCompanionAreaCapturedRun(
            pc,
            76,
            () -> {
              synchronized (aim.lifetime.admission) {
                register(aim);
                if (NativeAreaMaterialAction.current.get() != c || !aim.valid()) return;
                c.npcs
                    .entrySet()
                    .removeIf(
                        e ->
                            e.getValue().kind() == 3
                                && scope.receipts.containsKey((ClientSession) e.getValue().owner())
                                && !scope.owns(c, (ClientSession) e.getValue().owner()));
                CombatAccess.set(c, NativeCompanionNegative.STAGE, 0);
                try {
                  if (world) pc.nativeAreaWorldSlowBody(packet, spell);
                  else pc.nativeAreaBattleSlowBody(packet, spell);
                } finally {
                  CombatAccess.set(c, NativeCompanionNegative.STAGE, 1);
                }
              }
            },
            c);
      } finally {
        if (prior == null) NativeAreaMaterialAction.current.remove();
        else NativeAreaMaterialAction.current.set(prior);
        if (!aim.registered) aim.close();
        CombatAccess.set(c, "nativeCompanionTargetScope", null);
      }
    }
  }

  static Aim aim() {
    var c = NativeAreaMaterialAction.current.get();
    return c != null && c.nativeSourceMassSlowAim instanceof Aim a ? a : null;
  }

  static void register(Aim a) {
    if (!a.registered) {
      NativeLightAction.finishScoped((published, failure) -> a.close());
      a.registered = true;
    }
  }

  static boolean active() {
    return aim() != null;
  }

  static boolean impact(Aim a, Output o) {
    int op = o.packet.length == 0 ? -1 : o.packet[0] & 255;
    return o.receiver == a.c.pc
        && (op == Protocol.S_MANA_POINT
            || op == Protocol.S_HIT_POINT
            || op == Protocol.S_CHANGE_DIRECTION);
  }

  static boolean position(Aim a) {
    if (!NativeSourceMaterialFirstLife.valid(a)) return false;
    if (a.closed
        || a.lifetime.mode == NativePlayerLifetime.Mode.STOPPED
        || (boolean) CombatAccess.field(a.c.hub, "nativeStaticMotionClosed")
        || !a.c.scene()
        || !(a.pet == null ? a.c.player(a.player) : a.c.npc(a.target.id()))
        || NativePlayerCommit.integer(a.c.pc, "positionX") != a.cx
        || NativePlayerCommit.integer(a.c.pc, "positionY") != a.cy) return false;
    if (a.pet == null)
      return NativePlayerCommit.integer(a.player, "positionX") == a.x
          && NativePlayerCommit.integer(a.player, "positionY") == a.y;
    var snapshot = a.pet.snapshot();
    return snapshot.x() == a.x && snapshot.y() == a.y;
  }

  static boolean captureOutput(ClientSession receiver, byte[] packet, String note) {
    var a = aim();
    if (a == null || a.outbound != null) return false;
    register(a);
    var binding = a.c.bindings.get(receiver);
    if (binding == null) return true;
    var out = new Output(receiver, binding, packet.clone(), note);
    NativeLightAction.deferScoped(
        () -> {
          if (a.closed
              || a.lifetime.mode == NativePlayerLifetime.Mode.STOPPED
              || !a.c.caster.matches(a.c.pc)
              || !out.binding.matches(receiver)
              || !impact(a, out) && !position(a)) return;
          a.outbound = out;
          try {
            NativeAreaMaterialAction.owned(
                a.c, () -> receiver.nativeSelfOriginalSend(out.packet, out.note));
          } finally {
            a.outbound = null;
          }
        });
    return true;
  }

  /**
   * Existing native output monitor only: direct role/life/pose reads, no action/Hub/status locks.
   */
  static boolean allowFrame(ClientSession receiver, byte[] packet) {
    if (!NativeSourceMaterialFirstLife.slowFrame()) return false;
    var a = aim();
    var out = a == null ? null : a.outbound;
    if (out == null) return true;
    if (out.receiver != receiver
        || a.closed
        || a.lifetime.mode == NativePlayerLifetime.Mode.STOPPED
        || (boolean) CombatAccess.field(a.c.hub, "nativeStaticMotionClosed")
        || !NativeAcceptedFreezeMutation.roleFrame(a.c.caster, a.c.pc)
        || !NativeAcceptedFreezeMutation.roleFrame(out.binding, receiver)) return false;
    if (impact(a, out)) return true;
    var binding = a.c.bindings.get(a.owner);
    if (binding == null
        || !NativeAcceptedFreezeMutation.roleFrame(binding, a.owner)
        || (boolean) CombatAccess.field(a.owner, "sourceItemDisconnected")
        || NativePlayerCommit.integer(a.c.pc, "positionX") != a.cx
        || NativePlayerCommit.integer(a.c.pc, "positionY") != a.cy) return false;
    Object actor = a.pet == null ? a.player : a.pet;
    if ((int) CombatAccess.field(actor, a.pet == null ? "positionX" : "x") != a.x
        || (int) CombatAccess.field(actor, a.pet == null ? "positionY" : "y") != a.y) return false;
    var life =
        CombatAccess.field(CombatAccess.field(actor, "timedStatus"), "sourcePoisonLifecycle");
    if (life == null || CombatAccess.field(life, "sourceNpcOwner") != a.life) return false;
    if (a.pet != null) {
      if (!NativeSourceMaterialFirstLife.slowAttached(
          (List<?>) CombatAccess.field(a.owner, "tamedCompanions"), a.pet)) return false;
      var placement = NativeCompanionPlacement.state(a.pet);
      if (placement != a.placement
          || placement != null
              && (placement.owner.get() != a.owner
                  || placement.pose != a.pose
                  || placement.scene != a.scene
                  || placement.master != a.master)) return false;
    }
    if (packet.length >= 5) {
      int op = packet[0] & 255;
      if (op == Protocol.S_EFFECT
          || op == Protocol.S_SPEED
          || op == Protocol.S_ACTION
          || op == Protocol.S_POISON) {
        long id = NativeStaticNpcMotion.id(packet, 1);
        for (var entry : a.c.bindings.entrySet())
          if (entry.getValue().object() == id
              && !NativeAcceptedFreezeMutation.roleFrame(entry.getValue(), entry.getKey()))
            return false;
      }
    }
    return true;
  }

  static boolean controlled() {
    var a = aim();
    return a != null && a.pet != null;
  }

  static int x() {
    var a = aim();
    if (a == null) throw new IllegalStateException("captured Source76 primary required");
    return a.x;
  }

  static int y() {
    var a = aim();
    if (a == null) throw new IllegalStateException("captured Source76 primary required");
    return a.y;
  }

  /**
   * Source isTargetFailure rejects PET/SUMMON (including tamed constructor) before
   * probability/effect mutation.
   */
  static boolean playerAllowed(GameHub hub, long room, ClientSession caster, ClientSession target) {
    if (NativeSourceMassSelfTarget.self(caster, target)) return true;
    return ForcedCombat.canAttackTarget(hub, room, caster, target, target.playerObjectId());
  }

  static boolean skipPrimaryOwner(ClientSession owner, ClientSession caster) {
    var a = aim();
    return owner == caster && (a == null || a.c.pc != caster || a.pet == null || a.owner != owner);
  }

  static Iterator<?> failedControlledTargets(List<?> targets) {
    if (NativeSkillTargetMetadata.size() == 0) return targets.iterator();
    var row = NativeSkillTargetMetadata.row(76);
    if (row.type() != 1 && row.type() != 4) return targets.iterator();
    return Collections.emptyIterator();
  }

  private NativeSourceMassSlowTargets() {}

  // ported from candidate 50635f7e, post-freeze ASM members.
  static Iterator<?> calculationIterator(List<?> list) {
    Iterator<?> iterator = NativeSourceMassSelfTarget.iterator(list);
    if (iterator != null) {
      return iterator;
    }
    Aim aim = NativeSourceMassSlowTargets.aim();
    return NativeSourceNpcTargetFailure.iterator(
        aim != null && aim.owner == aim.c.pc ? Collections.emptyIterator() : list.iterator(), 76);
  }

  // ported from candidate 50635f7e, post-freeze ASM members.
  static int visualCount(int n) {
    if (NativeSkillTargetMetadata.size() == 0) {
      return n;
    }
    NativeSkillTargetMetadata.Row row = NativeSkillTargetMetadata.row(76);
    return row.area() != 0
            && !row.target().equals("none")
            && !row.target().equals("attack")
            && row.type() != 64
        ? 1
        : n;
  }

  // ported from candidate 50635f7e, post-freeze ASM members.
  static boolean primaryHeader() {
    Aim aim = NativeSourceMassSlowTargets.aim();
    return NativeSourceMassSlowTargets.controlled() || aim != null && aim.player == aim.c.pc;
  }

  // ported from candidate 50635f7e, post-freeze ASM members.
  static boolean skipPrimaryPlayer(ClientSession clientSession, ClientSession clientSession2) {
    Aim aim = NativeSourceMassSlowTargets.aim();
    return clientSession == clientSession2 && (aim == null || aim.player != clientSession2);
  }
}
