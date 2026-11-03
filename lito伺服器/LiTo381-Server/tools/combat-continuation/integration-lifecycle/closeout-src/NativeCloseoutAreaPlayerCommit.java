package local.lito381.server;

/** First status identity, checked inside each original status monitor without new locks. */
final class NativeCloseoutAreaPlayerCommit {
    static boolean allow(Object status) {
        var c = NativeAreaMaterialAction.current.get(); var scope = NativeCloseoutAreaOutput.scope(c);
        if (scope == null) return true;
        if (!Thread.holdsLock(status)) throw new IllegalStateException("area mutation requires original status monitor");
        if (!NativeSourceSelfFinalFrame.current(scope.first)
                || !NativeSourceWeaponFinalFrame.member(scope.source, c.hub)) return false;
        var player = scope.mutationPlayers.get(status);
        if (player != null) return CombatAccess.field(player.pc(), scope.mutationFields.get(status)) == status
                && NativeSourceSelfFinalFrame.member(player, c.hub)
                && NativeSourceWeaponFinalFrame.member(scope.addressed.get(player.binding().object()), c.hub);
        // Erase-magic status is shared with natural NPCs. Preserve that original
        // path only for its separately frozen NPC, never an unknown current status.
        var npc = scope.mutationNpcs.get(status);
        return npc != null && CombatAccess.field(npc.identity().actor(), scope.mutationFields.get(status)) == status
                && NativeCloseoutAreaOutput.current(npc, c.hub);
    }
    static BattleTimedStatus.SpeedChange unchangedSlow() { return new BattleTimedStatus.SpeedChange(0, 0, 0); }
    private NativeCloseoutAreaPlayerCommit() {}
}
