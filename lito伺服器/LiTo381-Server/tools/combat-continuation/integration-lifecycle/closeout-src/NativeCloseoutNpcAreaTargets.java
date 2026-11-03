package local.lito381.server;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Area target admission only; TYPE_GMBUFF execution and success scheduling are separate work. */
final class NativeCloseoutNpcAreaTargets {
    static final class Selection extends AbstractList<ClientSession> {
        final NativeSourceNpcPlayer.Scope scope;
        final NativeSourceNpcFirstView.View view;
        final List<ClientSession> members;
        Selection(NativeSourceNpcPlayer.Scope scope, NativeSourceNpcFirstView.View view, List<ClientSession> members) {
            this.scope = scope; this.view = view; this.members = List.copyOf(members);
        }
        public ClientSession get(int index) { return members.get(index); }
        public int size() { return members.size(); }
    }
    static List<ClientSession> select(ClientSession primary, GameHub.MonsterAiTick tick, Object snapshot) {
        if (!CombatRules.loaded()) return null;
        var definition = tick.magicSkill();
        var stamp = NativeSourceNpcFactoryMemory.proof(snapshot);
        if (definition == null || stamp == null || stamp.npc() != definition.npcId()
                || !(stamp.nativeSourceNpcSpellTiming instanceof NativeSourceNpcSpellTiming.Data data)) return null;
        var route = data.routes().get(definition.actNo());
        if (route == null || route.type() != NativeDeNpcMobSkill.TYPE_MAGIC_ATTACK
                || route.skill() != definition.skillId() || (route.width() <= 0 && route.height() <= 0)) return null;
        // A known Source area route never falls back to a new actor or the old radius selection.
        if (!(tick.nativeSourceNpcTickView instanceof NativeSourceNpcFirstView.View view)
                || view.stamp() != stamp || tick.objectId() != view.id() || tick.force() != view.force()
                || tick.sourceX() != view.x() || tick.sourceY() != view.y()
                || !(NativeCombatAttacker.nativeSourceNpcPlayerCurrent() instanceof NativeSourceNpcPlayer.Scope scope)
                || scope.origin().primary() != primary || scope.origin() != NativeSourceNpcPlayer.origin(tick)
                || scope.origin().room() != tick.roomId() || scope.attacker().nativeSourceNpcTickView != view
                || !Objects.equals(scope.attacker(), NativeCombatAttacker.current())) return List.of();
        var proof = new Selection(scope, view, List.of());
        var selected = new ArrayList<ClientSession>();
        // Retain the room's existing order; never adopt a player absent from the original ingress.
        for (var pc : scope.origin().hub().roomMembers(scope.origin().room()))
            if (eligible(proof, pc)) selected.add(pc);
        return new Selection(scope, view, selected);
    }
    static boolean allowed(List<ClientSession> selected, ClientSession pc) {
        return !(selected instanceof Selection proof) || eligible(proof, pc);
    }
    static boolean eligible(Selection proof, ClientSession pc) {
        var scope = proof.scope; var view = proof.view;
        var member = scope.origin().members().get(pc);
        if (member == null || !member.current(true) || !NativeSourceNpcPlayer.target(scope, pc)
                || CombatAccess.field(pc, "pendingPortal") != null
                || !Objects.equals(scope.attacker(), NativeCombatAttacker.current())
                || NativeCombatAttacker.nativeSourceNpcPlayerCurrent() != scope) return false;
        var hub = scope.origin().hub();
        ArenaMapCatalog.Arena arena;
        synchronized (hub) {
            // Membership is independent of the resolver's original NPC life/view check.
            if (!hub.roomMembers(scope.origin().room()).contains(pc)
                    || !NativeCombatAttacker.alive(pc, scope.attacker())) return false;
            Object owner = view.owner().get(), actor = view.actor().get();
            if (owner == null || actor == null) return false;
            synchronized (owner) {
                if (!NativeSourceNpcTickView.same(view, owner, actor)
                        || NativeSourceRawDamage.number(actor, "hp") <= 0
                        || NativeSourceRawDamage.number(actor, "x") != view.x()
                        || NativeSourceRawDamage.number(actor, "y") != view.y()) return false;
                arena = NativeCloseoutNpcAreaScene.arena(owner, actor);
            }
            if (arena == null || arena.mapId() == 93
                    || hub.battleMapId(scope.origin().room(), pc) != arena.mapId()) return false;
        }
        // Ordinary invisibility is permitted by Source areaMagicAttack. No modeled GM-invisibility flag exists here.
        return BattleSpellCatalog.isInNativeScreen(view.x(), view.y(), member.x, member.y)
                && BattleSpellCatalog.hasNativeLineOfSight(arena, view.x(), view.y(), member.x, member.y)
                && member.current(true) && NativeSourceNpcPlayer.target(scope, pc);
    }
    private NativeCloseoutNpcAreaTargets() {}
}
