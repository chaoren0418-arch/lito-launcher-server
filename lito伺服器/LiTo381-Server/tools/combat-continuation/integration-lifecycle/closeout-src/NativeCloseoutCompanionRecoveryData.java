package local.lito381.server;

/** Freeze the actual pet/summon NPC template at the existing controlled factory boundary. */
final class NativeCloseoutCompanionRecoveryData {
    static Object capture(int npcId) {
        final NativeDeNpcTemplateResources.Snapshot snapshot;
        try { snapshot = NativeDeNpcTemplateResources.snapshot(); }
        catch (IllegalStateException unloaded) { return null; }
        if (snapshot == null || snapshot.lifecycleFailure() || snapshot.npcs() == null) return null;
        var npc = snapshot.npcs().get(npcId);
        if (npc == null || npc.get_npcId() != npcId) return null;
        return new NativeCloseoutNpcRecovery.Data(npc.get_hprinterval() > 0 ? npc.get_hprinterval() : 20,
                npc.get_hpr() > 0 ? npc.get_hpr() : 1,
                npc.get_mprinterval() > 0 ? npc.get_mprinterval() : 20,
                npc.get_mpr() > 0 ? npc.get_mpr() : 1);
    }
    private NativeCloseoutCompanionRecoveryData() {}
}
