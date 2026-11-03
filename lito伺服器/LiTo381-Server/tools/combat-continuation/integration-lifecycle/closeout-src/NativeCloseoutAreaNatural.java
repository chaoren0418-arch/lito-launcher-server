package local.lito381.server;

import java.io.IOException;

/** Reuse the original first Context for BATTLE natural primaries; original bodies and locks remain. */
final class NativeCloseoutAreaNatural {
    static boolean eligible(NativeAreaMaterialAction.Context c, NativeAreaMaterialAction.Npc n) {
        return NativeSourceTargetNegativeLife.enabled(c) && n != null && (n.kind() == 1 || n.kind() == 2);
    }
    static void run(NativeAreaMaterialAction.Context c, NativeAreaMaterialAction.Npc n, int skill, NativeSelfCastAction.Task body) throws IOException {
        NativeSourceAreaPairFirst.capture(c, n, null, skill);
        if (!NativeSourceAreaPairFirst.current(c)) return;
        NativeCloseoutAreaOutput.install(c);
        try {
            NativeAreaMaterialAction.nativeCompanionAreaCapturedRun(c.pc, skill, () -> {
                try { if (NativeSourceAreaPairFirst.current(c)) body.run(); }
                finally { close(c); } // before the existing Batch drains deferred IO
            }, c);
        } finally { close(c); } // rejected admission or producer exception
    }
    static void close(NativeAreaMaterialAction.Context c) {
        NativeCloseoutAreaOutput.close(c); c.nativeSourceAreaPairFirst = null;
    }
    static void slow(ClientSession pc, byte[] packet, BattleSpellCatalog.MassSlowSpell spell, boolean world,
                     NativeAreaMaterialAction.Context c, NativeAreaMaterialAction.Npc n) throws IOException {
        if (!eligible(c, n)) { NativeSourceMassSlowTargets.original(pc, packet, spell, world); return; }
        run(c, n, 76, () -> pc.nativeAreaBattleSlowBody(packet, spell));
    }
    static void wind(ClientSession pc, byte[] packet, BattleSpellCatalog.WindShackleSpell spell, int fixture,
                     NativeAreaMaterialAction.Context c, NativeAreaMaterialAction.Npc n) throws IOException {
        if (!eligible(c, n)) { NativeSourceWindTargets.original(pc, packet, spell, fixture); return; }
        run(c, n, 167, () -> pc.nativeMaterialAreaWindBody(packet, spell, fixture));
    }
    private NativeCloseoutAreaNatural() {}
}
