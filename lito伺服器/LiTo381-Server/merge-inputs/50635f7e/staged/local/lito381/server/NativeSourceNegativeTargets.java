package local.lito381.server;

import java.io.*;
import java.util.*;

/**
 * Actual controlled primary, original area player/NPC arithmetic, Source failure by controlled
 * actor class.
 */
final class NativeSourceNegativeTargets {
  record Output(
      ClientSession receiver, NativePlayerCommit.Binding binding, byte[] packet, String note) {}

  static final class Aim {
    transient Object nativeSourceNegativePairFirst;
    transient Object nativeSourceNegativeFinal;
    final int skill;
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
        ClientSession player,
        int skill) {
      this.skill = skill;
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
      NativeSourceNegativePairFirst.capture(this);
      NativeSourceNegativeFinalFrame.install(this);
    }

    void close() {
      NativeSourceNegativeFinalFrame.close(this);
      NativeSourceNegativePairFirst.close(this);
      closed = true;
      outbound = null;
      if (c.nativeSourceNegativeAim == this) c.nativeSourceNegativeAim = null;
    }

    boolean valid() {
      if (!NativeSourceNegativePairFirst.current(this)) return false;
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
      var row = NativeSkillTargetMetadata.row(skill);
      if (row.area() != 0 || row.targetTo() != 3 || row.type() != 1 || row.through())
        throw new IllegalStateException("unqualified Source167 target metadata");
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

  static void entangle(
      ClientSession pc, byte[] packet, BattleSpellCatalog.EntangleSpell spell, int fixture)
      throws IOException {
    run(pc, packet, spell, fixture);
  }

  static void erase(
      ClientSession pc, byte[] packet, BattleSpellCatalog.EraseMagicSpell spell, int fixture)
      throws IOException {
    run(pc, packet, spell, fixture);
  }

  static void original(ClientSession pc, byte[] packet, Object spell, int fixture)
      throws IOException {
    if (spell instanceof BattleSpellCatalog.EntangleSpell e)
      NativeNegativeMaterialAction.entangle(pc, packet, e, fixture);
    else
      NativeNegativeMaterialAction.erase(
          pc, packet, (BattleSpellCatalog.EraseMagicSpell) spell, fixture);
  }

  // ported from candidate 50635f7e, post-freeze ASM.
  static void run(ClientSession caster, byte[] packet, Object spell, int fixture)
      throws IOException {
    Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(packet);
    int skill = request.skillId();
    if (NativeSkillTargetMetadata.size() == 0 || skill != 29 && skill != 152 && skill != 153) {
      NativeSourceNegativeTargets.original(caster, packet, spell, fixture);
      return;
    }
    NativeAreaMaterialAction.Context context = new NativeAreaMaterialAction.Context(caster);
    request = NativeSourceNoneTargetRequest.request(context, caster, request, request.skillId());
    if (!context.phase.equals("WORLD") && !context.phase.equals("BATTLE")) {
      return;
    }
    NativeAreaMaterialAction.Npc npc = context.npcs.get(request.targetObjectId());
    ClientSession player = null;
    for (ClientSession candidate : context.players) {
      if (context.bindings.get(candidate).object() == request.targetObjectId()) {
        player = candidate;
        break;
      }
    }
    if (player == null && (npc == null || npc.kind() != 3)) {
      NativeSourceNegativeTargets.original(caster, packet, spell, fixture);
      return;
    }
    if (player != null) {
      npc = null;
    }
    Aim aim = new Aim(context, npc, player, skill);
    try (NativeCompanionTargets.Scope scope = new NativeCompanionTargets.Scope(context); ) {
      if (!scope.valid) {
        return;
      }
      CombatAccess.set(context, "nativeCompanionTargetScope", scope);
      context.nativeSourceNegativeAim = aim;
      CombatAccess.set(context, "nativeWeaponBreakTarget", 1);
      NativeAreaMaterialAction.Context previous = NativeAreaMaterialAction.current.get();
      NativeAreaMaterialAction.current.set(context);
      try {
        NativeAreaMaterialAction.nativeCompanionAreaCapturedRun(
            caster,
            skill,
            () ->
                NativeSourceNegativeFinalFrame.produce(
                    aim, context, scope, spell, caster, packet, fixture),
            context);
      } finally {
        if (previous == null) {
          NativeAreaMaterialAction.current.remove();
        } else {
          NativeAreaMaterialAction.current.set(previous);
        }
        if (!aim.registered) {
          aim.close();
        }
        CombatAccess.set(context, "nativeCompanionTargetScope", null);
      }
    }
  }

  static Aim aim() {
    var c = NativeAreaMaterialAction.current.get();
    return c != null && c.nativeSourceNegativeAim instanceof Aim a ? a : null;
  }

  static void register(Aim a) {
    if (NativeSourceNegativeFinalFrame.register(a)) return;
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
    if (NativeSourceNegativeFinalFrame.capture(receiver, packet, note)) return true;
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
    if (!NativeSourceMaterialFirstLife.negativeFrame()) return false;
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
      if (!NativeSourceMaterialFirstLife.negativeAttached(
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

  // ported from candidate 50635f7e, post-freeze ASM.
  static boolean admit(
      ClientSession caster,
      ClientSession player,
      ClientSession owner,
      int skill,
      int action,
      int reuse) {
    Aim aim = NativeSourceNegativeTargets.aim();
    if (aim == null) {
      return NativeNegativeMaterialAction.admit(caster, player, owner, skill, action, reuse);
    }
    if (aim.c.pc != caster
        || aim.skill != skill
        || !aim.valid()
        || (aim.player != null
            ? player != aim.player || owner != null
            : player != null || owner != aim.owner)) {
      return false;
    }
    return NativeLightAction.admitPair(caster, caster, skill, action, reuse);
  }

  private NativeSourceNegativeTargets() {}

  // ported from candidate 50635f7e, post-freeze ASM members.
  static void nativeSourceNegativeFinalOriginalProducer(
      Aim aim,
      NativeAreaMaterialAction.Context context,
      NativeCompanionTargets.Scope scope,
      Object spell,
      ClientSession caster,
      byte[] packet,
      int fixture)
      throws IOException {
    synchronized (aim.lifetime.admission) {
      register(aim);
      if (NativeAreaMaterialAction.current.get() != context || !aim.valid()) return;
      context
          .npcs
          .entrySet()
          .removeIf(
              entry ->
                  entry.getValue().kind() == 3
                      && scope.receipts.containsKey((ClientSession) entry.getValue().owner())
                      && !scope.owns(context, (ClientSession) entry.getValue().owner()));
      CombatAccess.set(context, NativeCompanionNegative.STAGE, 0);
      try {
        if (spell instanceof BattleSpellCatalog.EntangleSpell entangle) {
          caster.nativeMaterialEntangleBody(packet, entangle, fixture);
        } else {
          caster.nativeMaterialEraseBody(
              packet, (BattleSpellCatalog.EraseMagicSpell) spell, fixture);
        }
      } finally {
        CombatAccess.set(context, NativeCompanionNegative.STAGE, 1);
      }
    }
  }

  // ported from candidate 50635f7e, post-freeze ASM members.
  // Retained candidate lambda body after the ASM producer call-site redirection.
  private static void lambda$legacyProducer$0(
      Aim aim,
      NativeAreaMaterialAction.Context context,
      NativeCompanionTargets.Scope scope,
      Object spell,
      ClientSession caster,
      byte[] packet,
      int fixture)
      throws IOException {
    synchronized (aim.lifetime.admission) {
      register(aim);
      if (NativeAreaMaterialAction.current.get() != context || !aim.valid()) return;
      context
          .npcs
          .entrySet()
          .removeIf(
              entry ->
                  entry.getValue().kind() == 3
                      && scope.receipts.containsKey((ClientSession) entry.getValue().owner())
                      && !scope.owns(context, (ClientSession) entry.getValue().owner()));
      CombatAccess.set(context, NativeCompanionNegative.STAGE, 0);
      try {
        if (spell instanceof BattleSpellCatalog.EntangleSpell entangle) {
          caster.nativeMaterialEntangleBody(packet, entangle, fixture);
        } else {
          caster.nativeMaterialEraseBody(
              packet, (BattleSpellCatalog.EraseMagicSpell) spell, fixture);
        }
      } finally {
        CombatAccess.set(context, NativeCompanionNegative.STAGE, 1);
      }
    }
  }
}
