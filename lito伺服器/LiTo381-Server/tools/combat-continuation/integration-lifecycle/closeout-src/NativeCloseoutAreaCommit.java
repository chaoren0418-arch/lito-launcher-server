package local.lito381.server;

/** Validate the actor resolved by the original encounter while its monitor is held. */
final class NativeCloseoutAreaCommit {
    static boolean allow(Object encounter, long objectId, Object actor) {
        var c = NativeAreaMaterialAction.current.get();
        var scope = NativeCloseoutAreaOutput.scope(c);
        if (scope == null) return true;
        if (!Thread.holdsLock(encounter)) throw new IllegalStateException("area commit requires original encounter monitor");
        var target = scope.npcs.get(objectId);
        return target != null && target.identity().owner() == encounter && target.identity().actor() == actor
                && NativeSourceSelfFinalFrame.current(scope.first)
                && NativeSourceWeaponFinalFrame.member(scope.source, c.hub)
                && NativeCloseoutAreaOutput.current(target, c.hub);
    }
    private NativeCloseoutAreaCommit() {}
}
