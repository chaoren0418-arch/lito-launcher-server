package local.lito381.server;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Source HPBAR for arena encounter monsters; cache is attached to the actual monster, never a
 * global actor registry.
 */
final class NativeSourceMonsterHp {
  static final String P = "local/lito381/server/", CACHE = "nativeSourceHpViewers";

  record Viewer(WeakReference<ClientSession> pc, NativePlayerCommit.Binding binding, int ratio) {}

  static final class Cache {
    final Map<Long, Viewer> viewers = new HashMap<>();
    Object life;
  }

  record Aim(
      NativeAreaMaterialAction.Context c,
      NativeAreaMaterialAction.Npc npc,
      Cache cache,
      NativeSourceDamageDisplay.Actor caster,
      Object generation,
      int x,
      int y) {}

  static Aim capture(ClientSession pc, long id, int kind) {
    if (!CombatRules.loaded()) return null;
    var c = new NativeAreaMaterialAction.Context(pc);
    var npc = c.npcs.get(id);
    if (!c.phase.equals("BATTLE") || npc == null || npc.kind() != kind) return null;
    var cache = (Cache) CombatAccess.field(npc.actor(), CACHE);
    return new Aim(
        c,
        npc,
        cache,
        NativeSourcePlayerFactories.capture(pc, c),
        NativeProcHealingEncounter.generation(npc.owner()),
        (int) CombatAccess.field(npc.actor(), "x"),
        (int) CombatAccess.field(npc.actor(), "y"));
  }

  static Object life(Object actor) {
    return NativeCombatAttacker.token(
        (BattleTimedStatus) CombatAccess.field(actor, "damagePoison"), true);
  }

  static boolean valid(
      Aim aim,
      ClientSession viewer,
      NativePlayerCommit.Binding binding,
      Object life,
      boolean hide) {
    var actor = aim.npc.actor();
    var state =
        CombatAccess.field(CombatAccess.field(actor, "damagePoison"), "sourcePoisonLifecycle");
    return NativePlayerLifetime.of(aim.c.hub).mode != NativePlayerLifetime.Mode.STOPPED
        && !(boolean) CombatAccess.field(aim.c.hub, "nativeStaticMotionClosed")
        && CombatAccess.field(aim.npc.owner(), "sourceProcEncounterGeneration") == aim.generation
        && NativeAcceptedFreezeMutation.roleFrame(binding, viewer)
        && !(boolean) CombatAccess.field(viewer, "sourceItemDisconnected")
        && state != null
        && CombatAccess.field(state, "sourceNpcOwner") == life
        && (hide
            ? (int) CombatAccess.field(actor, "hp") <= 0
            : aim.caster.valid(aim.caster.life())
                && NativeSourceDamageDisplay.n(aim.c.pc, "playerHp") > 0
                && (int) CombatAccess.field(actor, "hp") > 0
                && (int) CombatAccess.field(actor, "x") == aim.x
                && (int) CombatAccess.field(actor, "y") == aim.y
                && BattleSpellCatalog.isInNativeScreen(
                    NativeSourceDamageDisplay.n(viewer, "positionX"),
                    NativeSourceDamageDisplay.n(viewer, "positionY"),
                    aim.x,
                    aim.y));
  }

  static void publish(Aim aim) {
    if (aim == null) return;
    int hp = (int) CombatAccess.field(aim.npc.actor(), "hp");
    if (hp <= 0) {
      hide(aim);
      return;
    }
    if (!NativeSourceOtherControls.settings().hpbar() || !aim.c.scene() || !aim.c.npc(aim.npc.id()))
      return;
    var snapshot =
        CombatAccess.call(
            aim.npc.actor(),
            "snapshot",
            "()L"
                + P
                + (aim.npc.kind() == 1 ? "BattleQuestEncounter" : "BattleTimeAttackEncounter")
                + "$Snapshot;");
    int max = (int) CombatAccess.call(snapshot, "maxHp", "()I");
    int ratio = max <= 0 ? 100 : 100 * hp / max;
    var viewer = aim.c.pc;
    Object token = aim.npc.epoch();
    try {
      synchronized (viewer) {
        if (!valid(aim, viewer, aim.c.caster, token, false)) return;
        var cache = aim.cache;
        synchronized (cache) {
          if (cache.life != token) {
            cache.viewers.clear();
            cache.life = token;
          }
          long id = aim.c.caster.object();
          var prior = cache.viewers.get(id);
          if (prior != null
              && prior.pc.get() == viewer
              && prior.binding.equals(aim.c.caster)
              && prior.ratio == ratio) return;
          if (cache.viewers.size() >= 1024 && !cache.viewers.containsKey(id)) cache.viewers.clear();
          cache.viewers.put(id, new Viewer(new WeakReference<>(viewer), aim.c.caster, ratio));
        }
        viewer.nativeSelfOriginalSend(
            new Protocol.Writer().c(Protocol.S_HIT_RATIO).d(aim.npc.id()).c(ratio).bytes(),
            "S_HIT_RATIO Source monster HPBAR");
      }
    } catch (IOException ignored) {
      /* Visual-only failure cannot undo accepted HP or prevent reward/lifecycle completion. */
    }
  }

  static void hide(Aim aim) {
    List<Viewer> viewers;
    synchronized (aim.cache) {
      viewers = List.copyOf(aim.cache.viewers.values());
      if (aim.cache.life != aim.npc.epoch()) return;
      aim.cache.viewers.clear();
      aim.cache.life = null;
    }
    Object token = life(aim.npc.actor());
    for (var entry : viewers) {
      var viewer = entry.pc.get();
      if (viewer == null) continue;
      try {
        synchronized (viewer) {
          if (valid(aim, viewer, entry.binding, token, true))
            viewer.nativeSelfOriginalSend(
                new Protocol.Writer().c(Protocol.S_HIT_RATIO).d(aim.npc.id()).c(255).bytes(),
                "S_HIT_RATIO Source monster HPBAR hide");
        }
      } catch (IOException ignored) {
      }
    }
  }

  // ported from candidate 50635f7e, post-freeze ASM.
  static int quest(ClientSession caster, long id, int damage, String source) throws IOException {
    return NativeSourceNpcDamageDisplay.quest(caster, id, damage, source);
  }

  // ported from candidate 50635f7e, post-freeze ASM.
  static int time(ClientSession caster, long id, int damage, String source) throws IOException {
    return NativeSourceNpcDamageDisplay.time(caster, id, damage, source);
  }

  /**
   * Only the original encounter-monster meter fan-out is replaced; death actions, poison, waves and
   * rewards remain original.
   */
  static void oldRatio(ClientSession receiver, byte[] packet, String note) throws IOException {
    if (!CombatRules.loaded())
      CombatAccess.call(receiver, "sendEncrypted", "([BLjava/lang/String;)V", packet, note);
  }

  private NativeSourceMonsterHp() {}
}
