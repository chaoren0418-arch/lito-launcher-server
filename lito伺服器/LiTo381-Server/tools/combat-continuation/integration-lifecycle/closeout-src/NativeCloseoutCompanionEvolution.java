package local.lito381.server;

/** Capture the replacement template proof before the accepted in-memory profile write. */
final class NativeCloseoutCompanionEvolution {
    static Object capture(BattleCompanion pet, BattleCompanion.Profile replacement) {
        if (!Thread.holdsLock(pet)) throw new IllegalStateException("pet evolution lost original monitor");
        if (!CombatRules.loaded()) return CombatAccess.field(pet, "nativeSourceNpcFactoryType");
        return NativeSourceNpcFactoryType.controlled(replacement.npcId(), (BattleCompanion.Kind) CombatAccess.field(pet, "kind"));
    }
    private NativeCloseoutCompanionEvolution() {}
}
