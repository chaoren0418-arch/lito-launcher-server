package local.lito381.server;

import java.io.*;
import java.util.*;

/**
 * Natural/controlled NPC original target and defense-summon provenance on existing ingress and raw
 * FIFO.
 */
final class NativeSourceNpcPlayer {
  record Origin(
      GameHub hub,
      long room,
      ClientSession primary,
      NativeSourceRawDamage.Member original,
      Map<ClientSession, NativeSourceRawDamage.Member> members) {}

  // ported from candidate 50635f7e, record with ASM instance fields.
  @AsRecord(components = {"origin", "attacker", "defense", "lifetime"})
  static final class Scope {
    private final Origin origin;
    private final NativeCombatAttacker.Ref attacker;
    private final Object defense;
    private final NativePlayerLifetime lifetime;
    Object nativeSourceNpcMagicDefinition;

    Scope(
        Origin origin,
        NativeCombatAttacker.Ref attacker,
        Object defense,
        NativePlayerLifetime lifetime) {
      this.origin = origin;
      this.attacker = attacker;
      this.defense = defense;
      this.lifetime = lifetime;
    }

    public Origin origin() {
      return this.origin;
    }

    public NativeCombatAttacker.Ref attacker() {
      return this.attacker;
    }

    public Object defense() {
      return this.defense;
    }

    public NativePlayerLifetime lifetime() {
      return this.lifetime;
    }
  }

  static Object capture(GameHub.MonsterAiTick tick) {
    var pc = tick.target();
    if (pc == null || !NativeSourceDollCommit.enabled(pc)) return null;
    var original = NativeSourceAcceptedPlayers.ingress(pc);
    var hub = (GameHub) CombatAccess.field(pc, "hub");
    return capture(pc, original, hub, tick.roomId());
  }

  static Origin nativeSourceNpcOriginOriginalCapture(
      ClientSession pc, NativeSourceRawDamage.Member original, GameHub hub, long room) {
    var members = new IdentityHashMap<ClientSession, NativeSourceRawDamage.Member>();
    for (var member : hub.roomMembers(room)) {
      var binding = NativePlayerCommit.Binding.capture(member);
      members.put(
          member, member == pc ? original : new NativeSourceRawDamage.Member(member, binding));
    }
    members.put(pc, original);
    return new Origin(hub, room, pc, original, Collections.unmodifiableMap(members));
  }

  static Origin origin(GameHub.MonsterAiTick tick) {
    var value = tick == null ? null : CombatAccess.field(tick, "nativeSourceNpcPlayerOrigin");
    return value instanceof Origin origin ? origin : null;
  }

  static boolean validTick(ClientSession pc, GameHub.MonsterAiTick tick) {
    if (!CombatRules.loaded()) return true;
    var original = origin(tick);
    return original == null || original.primary() == pc && original.original().current(false);
  }

  static boolean namespace(NativeCombatAttacker.Ref ref, Origin origin) {
    return ref != null
        && origin != null
        && ref.room() == origin.room()
        && (ref.kind() == 2 && ref.master() != null
            || ref.kind() == 3 && ref.master() == null
            || ref.kind() == 4 && ref.master() == null);
  }

  static Object attach(
      Object ingress, ClientSession pc, GameHub.MonsterAiTick tick, BattleCompanion pet) {
    if (ingress == null || !NativeSourceDollCommit.enabled(pc)) return ingress;
    CombatAccess.set(ingress, "nativeSourceNpcPlayerScope", Boolean.FALSE);
    try {
      var ref = (NativeCombatAttacker.Ref) CombatAccess.field(ingress, "attacker");
      Origin origin;
      if (tick != null) {
        origin = origin(tick);
        if (origin == null || origin.primary() != pc || !origin.original().current(false))
          return ingress;
        Object original = NativeCombatOrigin.tick(tick);
        if (original == null
            || !Objects.equals(CombatAccess.field(original, "target"), origin.original().binding)
            || !Objects.equals(CombatAccess.field(original, "attacker"), ref)) return ingress;
      } else {
        if (pet == null
            || ref == null
            || ref.kind() != 2
            || ref.id() != (long) CombatAccess.field(pet, "objectId")
            || ref.master() == null
            || !NativeAcceptedFreezeMutation.roleFrame(ref.master(), pc)) return ingress;
        var member = NativeSourceNpcOrigins.master(pc, ref.master(), ref);
        origin = capture(pc, member, (GameHub) CombatAccess.field(pc, "hub"), ref.room());
      }
      if (!namespace(ref, origin)) return ingress;
      var life = NativePlayerLifetime.of(origin.hub());
      if (life.mode != NativePlayerLifetime.Mode.OPEN || !origin.original().current(false))
        return ingress;
      var context = new NativeAreaMaterialAction.Context(pc);
      if (!context.caster.equals(origin.original().binding) || !origin.original().current(false))
        return ingress;
      CombatAccess.set(
          ingress,
          "nativeSourceNpcPlayerScope",
          new Scope(origin, ref, context.nativeSourceDollDefenseSnapshot, life));
      return ingress;
    } catch (RuntimeException failure) {
      NativeSourcePhysicalNpc.failed(
          NativePlayerLifetime.of((GameHub) CombatAccess.field(pc, "hub")));
      return ingress;
    }
  }

  static void clear(Object ingress) {
    if (ingress != null) CombatAccess.set(ingress, "nativeSourceNpcPlayerScope", null);
  }

  static boolean target(Scope scope, ClientSession pc) {
    var member = scope.origin().members().get(pc);
    return member != null
        && scope.lifetime().mode == NativePlayerLifetime.Mode.OPEN
        && !(boolean) CombatAccess.field(scope.origin().hub(), "nativeStaticMotionClosed")
        && member.current(false)
        && member.binding.phase().equals("BATTLE")
        && member.binding.room() == scope.origin().room()
        && CombatAccess.field(pc, "hub") == scope.origin().hub()
        && NativeSourceDamageDisplay.n(pc, "playerHp") > 0;
  }

  static void damage(ClientSession pc, int damage, ClientSession caster, boolean magic)
      throws IOException {
    var value = NativeCombatAttacker.nativeSourceNpcPlayerCurrent();
    if (!CombatRules.loaded() || value == null) {
      pc.nativeSourceNpcPlayerOriginalDamage(damage, caster);
      return;
    }
    if (!(value instanceof Scope scope)
        || caster != null
        || !Objects.equals(NativeCombatAttacker.current(), scope.attacker())
        || !target(scope, pc)) return;
    try {
      NativeSourcePlayerActionLock.runPairOrdered(
          pc,
          pc,
          () -> {
            synchronized (scope.lifetime().admission) {
              if (!target(scope, pc)) return;
              try {
                NativeLightAction.commitSelf(
                    pc,
                    () -> {
                      if (!target(scope, pc)) return;
                      var member = scope.origin().members().get(pc);
                      var context = new NativeAreaMaterialAction.Context(pc);
                      if (!context.caster.equals(member.binding) || !target(scope, pc)) return;
                      context.nativeSourceDollDefenseSnapshot = scope.defense();
                      var actor =
                          new NativeSourceDamageDisplay.Actor(
                              pc, member.binding, member.life, member.x, member.y);
                      var plan = new NativeSourceDamageDisplay.Plan(actor, actor, List.of());
                      plan.nativeRawGuard =
                          new NativeSourceRawDamage.Guard(member, null, false, scope.lifetime());
                      NativeSourceNpcHpOutput.bind(plan, scope, pc);
                      if (NativeSourceNpcMagicDirect.handled(plan, scope, pc, damage, magic))
                        return;
                      NativeSourceNpcMagicEffects.bind(plan, scope, pc, damage, magic);
                      NativeAreaMaterialAction.owned(
                          context,
                          () ->
                              NativeSourceDamageDisplay.damage(
                                  pc, damage, null, magic, false, 0, plan));
                      NativeSourceNpcMagicDirect.afterSigned(plan, damage);
                    });
              } catch (IOException error) {
                throw new UncheckedIOException(error);
              }
            }
          });
    } catch (UncheckedIOException error) {
      throw error.getCause();
    }
  }

  static boolean finalRawFrame(boolean allowed, ClientSession pc) {
    if (!allowed) return false;
    var frame = NativePlayerCommit.frame();
    var plan = frame == null ? null : frame.event.sourceDisplay();
    var guard = plan == null ? null : plan.nativeRawGuard;
    return guard == null
        || NativeSourceControlledDisplay.rawLife(pc) == guard.target().acceptedLife;
  }

  private NativeSourceNpcPlayer() {}

  // ported from candidate 50635f7e, post-freeze ASM members.
  static Origin nativeSourceNpcOriginFrozenCapture(
      ClientSession caster, NativeSourceRawDamage.Member original, GameHub hub, long room) {
    IdentityHashMap<ClientSession, NativeSourceRawDamage.Member> members =
        new IdentityHashMap<ClientSession, NativeSourceRawDamage.Member>();
    for (ClientSession receiver : hub.roomMembers(room)) {
      NativePlayerCommit.Binding binding =
          NativeSourceNpcOrigins.binding(receiver, members, caster, original, hub);
      members.put(
          receiver,
          receiver == caster
              ? original
              : NativeSourceNpcOrigins.member(receiver, binding, members));
    }
    members.put(caster, original);
    return new Origin(hub, room, caster, original, Collections.unmodifiableMap(members));
  }

  // ported from candidate 50635f7e, post-freeze ASM members.
  static Object nativeSourceCompanionClockAttach(
      Object ingress,
      ClientSession pc,
      GameHub.MonsterAiTick tick,
      BattleCompanion pet,
      Object clock) {
    if (ingress == null || !NativeSourceDollCommit.enabled(pc)) return ingress;
    CombatAccess.set(ingress, "nativeSourceNpcPlayerScope", Boolean.FALSE);
    try {
      var ref = (NativeCombatAttacker.Ref) CombatAccess.field(ingress, "attacker");
      Origin origin;
      if (tick != null) {
        origin = origin(tick);
        if (origin == null || origin.primary() != pc || !origin.original().current(false))
          return ingress;
        Object original = NativeCombatOrigin.tick(tick);
        if (original == null
            || !Objects.equals(CombatAccess.field(original, "target"), origin.original().binding)
            || !Objects.equals(CombatAccess.field(original, "attacker"), ref)) return ingress;
      } else {
        if (pet == null
            || ref == null
            || ref.kind() != 2
            || ref.id() != (long) CombatAccess.field(pet, "objectId")
            || ref.master() == null
            || !NativeAcceptedFreezeMutation.roleFrame(ref.master(), pc)) return ingress;
        var member = NativeSourceNpcOrigins.master(pc, ref.master(), ref);
        origin =
            NativeSourceCompanionClock.origin(
                pc, member, (GameHub) CombatAccess.field(pc, "hub"), ref.room(), clock);
      }
      if (!namespace(ref, origin)) return ingress;
      var life = NativePlayerLifetime.of(origin.hub());
      if (life.mode != NativePlayerLifetime.Mode.OPEN || !origin.original().current(false))
        return ingress;
      var context = new NativeAreaMaterialAction.Context(pc);
      if (!context.caster.equals(origin.original().binding) || !origin.original().current(false))
        return ingress;
      CombatAccess.set(
          ingress,
          "nativeSourceNpcPlayerScope",
          new Scope(origin, ref, context.nativeSourceDollDefenseSnapshot, life));
      return ingress;
    } catch (RuntimeException failure) {
      NativeSourcePhysicalNpc.failed(
          NativePlayerLifetime.of((GameHub) CombatAccess.field(pc, "hub")));
      return ingress;
    }
  }

  // ported from candidate 50635f7e, post-freeze ASM members.
  static Origin capture(
      ClientSession caster, NativeSourceRawDamage.Member original, GameHub hub, long room) {
    return NativeSourceNpcOrigins.origin(caster, original, hub, room);
  }
}
