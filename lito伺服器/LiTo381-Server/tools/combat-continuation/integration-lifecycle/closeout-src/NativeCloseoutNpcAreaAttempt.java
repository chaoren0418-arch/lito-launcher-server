package local.lito381.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Complete only the proven no-effect self cast while the original AI invocation still owns its locks. */
final class NativeCloseoutNpcAreaAttempt {
    static final String FIELD = "nativeCloseoutNpcAreaReceipt";
    static final ThreadLocal<Context> current = new ThreadLocal<>();
    static final class Context {
        final Context previous;
        final Object snapshot, choice;
        final int start;
        Receipt receipt;
        Context(Context previous, Object snapshot, Object choice, int start) {
            this.previous = previous; this.snapshot = snapshot; this.choice = choice; this.start = start;
        }
    }
    static final class Receipt {
        final BattleTimeAttackMagicSkillCatalog.Definition row;
        final NativeSourceNpcFirstView.View view;
        final NativeSourceNpcPlayer.Origin origin;
        final NativePlayerLifetime lifetime;
        List<NativeCloseoutNpcAreaCast.Attempt> attempts;
        Object snapshot;
        final AtomicBoolean claimed = new AtomicBoolean();
        Receipt(BattleTimeAttackMagicSkillCatalog.Definition row, NativeSourceNpcFirstView.View view,
                NativeSourceNpcPlayer.Origin origin, NativePlayerLifetime lifetime,
                Object snapshot, List<NativeCloseoutNpcAreaCast.Attempt> attempts) {
            this.row = row; this.view = view; this.origin = origin; this.lifetime = lifetime;
            this.snapshot = snapshot; this.attempts = List.copyOf(attempts);
        }
    }
    static Object enter(Object snapshot, Object choice, List<?> ticks) {
        var context = new Context(current.get(), snapshot, choice, ticks.size());
        current.set(context); return context;
    }
    static void leave(Object token, List<GameHub.MonsterAiTick> ticks, boolean completed) {
        var context = (Context) token;
        try {
            var receipt = context.receipt;
            if (completed && receipt != null) for (int i = context.start; i < ticks.size(); i++) {
                var tick = ticks.get(i);
                if (tick.magicSkill() == receipt.row && tick.nativeSourceNpcTickView == receipt.view
                        && NativeSourceNpcPlayer.origin(tick) == receipt.origin)
                    CombatAccess.set(tick, FIELD, receipt);
            }
        } finally {
            context.receipt = null;
            if (context.previous == null) current.remove(); else current.set(context.previous);
        }
    }
    static boolean live(GameHub hub, NativePlayerLifetime lifetime) {
        return lifetime.mode == NativePlayerLifetime.Mode.OPEN
                && CombatAccess.field(hub, "sourcePlayerCommitLifetime") == lifetime
                && !(boolean) CombatAccess.field(hub, "nativeStaticMotionClosed");
    }
    static boolean permit(Object actor, BattleTimeAttackMagicSkillCatalog.MobskillRow row, Object encounter) {
        if (!(row instanceof BattleTimeAttackMagicSkillCatalog.Definition definition)
                || !NativeCloseoutNpcAreaCost.matches(actor, row)) return true;
        final NativeSkillTargetMetadata.Row metadata;
        try { metadata = NativeSkillTargetMetadata.row(definition.skillId()); }
        catch (IllegalStateException absent) { return false; }
        if (!NativeCloseoutNpcAreaCast.emptyTarget(definition.skillId(), metadata)) return true;
        var context = current.get();
        if (context == null || !(context.choice instanceof NativeSourceNpcPlayer.Origin origin)
                || !(NativeSourceNpcFirstView.raw(context.snapshot) instanceof NativeSourceNpcFirstView.View view)
                || !Thread.holdsLock(origin.hub()) || !Thread.holdsLock(encounter)
                || !NativeSourceNpcPlayerView.current(origin) || view.owner().get() != encounter
                || !NativeSourceNpcTickView.same(view, encounter, actor)
                || NativeSourceRawDamage.number(actor, "hp") <= 0
                || NativeSourceRawDamage.number(actor, "x") != view.x()
                || NativeSourceRawDamage.number(actor, "y") != view.y()) return false;
        context.receipt = null;
        var hub = origin.hub(); var room = ((Map<?, ?>) CombatAccess.field(hub, "rooms")).get(origin.room());
        var lifetime = NativePlayerLifetime.of(hub);
        if (room == null || !hub.battleStarted(origin.room())
                || CombatAccess.field(room, view.family() == 1 ? "questEncounter" : "timeAttackEncounter") != encounter
                || !live(hub, lifetime)) return false;
        var arena = NativeCloseoutNpcAreaScene.arena(encounter, actor);
        if (arena == null || arena.mapId() == 93) return false;
        var attempts = new ArrayList<NativeCloseoutNpcAreaCast.Attempt>();
        for (var pc : hub.roomMembers(origin.room())) {
            if (!live(hub, lifetime)) return false;
            var member = origin.members().get(pc);
            if (member == null || !member.current(true) || !member.binding.phase().equals("BATTLE")
                    || member.binding.room() != origin.room() || CombatAccess.field(pc, "hub") != hub
                    || NativeSourceRawDamage.number(pc, "playerHp") <= 0
                    || CombatAccess.field(pc, "pendingPortal") != null
                    || hub.battleMapId(origin.room(), pc) != arena.mapId()
                    || !BattleSpellCatalog.isInNativeScreen(view.x(), view.y(), member.x, member.y)
                    || !NativeCloseoutNpcAreaDoors.nativeCloseoutOriginLineOfSight(arena, view.x(), view.y(), member.x, member.y, origin)
                    || !member.current(true) || NativeSourceRawDamage.number(pc, "playerHp") <= 0) continue;
            if (!live(hub, lifetime)) return false;
            // Source handleCommands(TYPE_GMBUFF) returns true here: its target list, effects and graphics are empty.
            attempts.add(new NativeCloseoutNpcAreaCast.Attempt(definition.skillId(), member, true));
        }
        if (attempts.isEmpty() || !live(hub, lifetime)) return false;
        context.receipt = new Receipt(definition, view, origin, lifetime, context.snapshot, attempts);
        return true;
    }
    /** Completed casts are independent of the original primary player. Only their original NPC/output proof remains authoritative. */
    static boolean deliver(ClientSession primary, GameHub.MonsterAiTick tick) {
        if (tick == null) return false;
        Object raw = CombatAccess.field(tick, FIELD);
        if (!(raw instanceof Receipt receipt)) return false;
        if (!receipt.claimed.compareAndSet(false, true)) return true;
        var attempts = receipt.attempts; receipt.attempts = null;
        var snapshot = receipt.snapshot; receipt.snapshot = null;
        if (receipt.row != tick.magicSkill() || receipt.view != tick.nativeSourceNpcTickView
                || receipt.origin != NativeSourceNpcPlayer.origin(tick) || primary != receipt.origin.primary()
                || primary != tick.target() || tick.roomId() != receipt.origin.room()
                || tick.objectId() != receipt.view.id() || tick.force() != receipt.view.force()
                || tick.sourceX() != receipt.view.x() || tick.sourceY() != receipt.view.y()
                || NativeSourceNpcFactoryMemory.proof(snapshot) != receipt.view.stamp()
                || receipt.lifetime.mode == NativePlayerLifetime.Mode.STOPPED
                || CombatAccess.field(receipt.origin.hub(), "sourcePlayerCommitLifetime") != receipt.lifetime) return true;
        var original = NativeCombatOrigin.tick(tick);
        if (original == null || !(CombatAccess.field(original, "attacker") instanceof NativeCombatAttacker.Ref ref)
                || ref.nativeSourceNpcTickView != receipt.view
                || !NativeSourceNpcPlayer.namespace(ref, receipt.origin)) return true;
        // This scope carries completed output provenance only; it is never installed as a damage/action context.
        var scope = new NativeSourceNpcPlayer.Scope(receipt.origin, ref, null, receipt.lifetime);
        var selection = new NativeCloseoutNpcAreaTargets.Selection(scope, receipt.view, List.of());
        CombatAccess.set(selection, NativeCloseoutNpcAreaCast.FIELD, attempts);
        try { NativeCloseoutNpcAreaOutput.finish(tick, snapshot, selection, List.of()); }
        finally { CombatAccess.set(selection, NativeCloseoutNpcAreaCast.FIELD, null); }
        return true;
    }
    private NativeCloseoutNpcAreaAttempt() {}
}
