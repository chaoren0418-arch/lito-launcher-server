// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import local.lito381.server.BattleCubeHarmonyEffects;
import local.lito381.server.BattleCubeIgnitionEffects;
import local.lito381.server.BattleCubeQuakeEffects;
import local.lito381.server.BattleCubeShockEffects;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.GameHub;
import local.lito381.server.GameStateStore;
import local.lito381.server.NativeCastMaterial;
import local.lito381.server.NativeLightAction;

final class NativeCubeCastAction {
    static void cubeIgnition(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.CubeIgnitionSpell cubeIgnitionSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeCubeIgnitionBody(byArray, cubeIgnitionSpell));
    }

    static void cubeQuake(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.CubeQuakeSpell cubeQuakeSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeCubeQuakeBody(byArray, cubeQuakeSpell));
    }

    static void cubeShock(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.CubeShockSpell cubeShockSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeCubeShockBody(byArray, cubeShockSpell));
    }

    static void cubeHarmony(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.CubeHarmonySpell cubeHarmonySpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeCubeHarmonyBody(byArray, cubeHarmonySpell));
    }

    static boolean supported(int n) {
        return n == 205 || n == 210 || n == 215 || n == 220;
    }

    static boolean suppressFailure(ClientSession clientSession) {
        NativeCastMaterial.Cast cast = NativeLightAction.currentMaterial(clientSession);
        return cast != null && cast.placementBlocked;
    }

    static boolean admit(ClientSession clientSession, int n) {
        NativeCastMaterial.Cast cast = NativeLightAction.currentMaterial(clientSession);
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        GameStateStore gameStateStore = (GameStateStore)CombatAccess.field(clientSession, "state");
        if (!(NativeCubeCastAction.supported(n) && cast != null && cast.skill == n && cast.placement && NativeCastMaterial.active(clientSession, cast) && Thread.holdsLock(gameHub) && Thread.holdsLock(gameStateStore) && Thread.holdsLock(clientSession.nativeDeInventory(0)))) {
            throw new IllegalStateException("cube admission requires exact original placement and canonical material lock scope");
        }
        Record record = switch (n) {
            case 205 -> BattleSpellCatalog.cubeIgnitionSpell(n);
            case 210 -> BattleSpellCatalog.cubeQuakeSpell(n);
            case 215 -> BattleSpellCatalog.cubeShockSpell(n);
            case 220 -> BattleSpellCatalog.cubeHarmonySpell(n);
            default -> throw new AssertionError();
        };
        boolean bl = NativeLightAction.admitSelf(clientSession, n, (Integer)CombatAccess.call(record, "actionId", "()I", new Object[0]), (Integer)CombatAccess.call(record, "reuseDelayMillis", "()I", new Object[0]));
        if (!bl) {
            cast.placementBlocked = true;
        }
        return bl;
    }

    static BattleCubeIgnitionEffects.Placement placeCubeIgnition(GameHub gameHub, long l, ClientSession clientSession, int n, int n2, long l2) throws IOException {
        return NativeCastMaterial.withPlacement(clientSession, 205, () -> gameHub.nativeMaterialPlaceCubeIgnition(l, clientSession, n, n2, l2), BattleCubeIgnitionEffects.Placement.rejected());
    }

    static BattleCubeIgnitionEffects.Placement placeClassicWorldCubeIgnition(GameHub gameHub, ClientSession clientSession, int n, int n2, int n3, int n4, long l) throws IOException {
        return NativeCastMaterial.withPlacement(clientSession, 205, () -> gameHub.nativeMaterialPlaceClassicWorldCubeIgnition(clientSession, n, n2, n3, n4, l), BattleCubeIgnitionEffects.Placement.rejected());
    }

    static BattleCubeQuakeEffects.Placement placeCubeQuake(GameHub gameHub, long l, ClientSession clientSession, int n, int n2, long l2) throws IOException {
        return NativeCastMaterial.withPlacement(clientSession, 210, () -> gameHub.nativeMaterialPlaceCubeQuake(l, clientSession, n, n2, l2), BattleCubeQuakeEffects.Placement.rejected());
    }

    static BattleCubeQuakeEffects.Placement placeClassicWorldCubeQuake(GameHub gameHub, ClientSession clientSession, int n, int n2, int n3, int n4, long l) throws IOException {
        return NativeCastMaterial.withPlacement(clientSession, 210, () -> gameHub.nativeMaterialPlaceClassicWorldCubeQuake(clientSession, n, n2, n3, n4, l), BattleCubeQuakeEffects.Placement.rejected());
    }

    static BattleCubeShockEffects.Placement placeCubeShock(GameHub gameHub, long l, ClientSession clientSession, int n, int n2, long l2) throws IOException {
        return NativeCastMaterial.withPlacement(clientSession, 215, () -> gameHub.nativeMaterialPlaceCubeShock(l, clientSession, n, n2, l2), BattleCubeShockEffects.Placement.rejected());
    }

    static BattleCubeShockEffects.Placement placeClassicWorldCubeShock(GameHub gameHub, ClientSession clientSession, int n, int n2, int n3, int n4, long l) throws IOException {
        return NativeCastMaterial.withPlacement(clientSession, 215, () -> gameHub.nativeMaterialPlaceClassicWorldCubeShock(clientSession, n, n2, n3, n4, l), BattleCubeShockEffects.Placement.rejected());
    }

    static BattleCubeHarmonyEffects.Placement placeCubeHarmony(GameHub gameHub, long l, ClientSession clientSession, int n, int n2, long l2) throws IOException {
        return NativeCastMaterial.withPlacement(clientSession, 220, () -> gameHub.nativeMaterialPlaceCubeHarmony(l, clientSession, n, n2, l2), BattleCubeHarmonyEffects.Placement.rejected());
    }

    static BattleCubeHarmonyEffects.Placement placeClassicWorldCubeHarmony(GameHub gameHub, ClientSession clientSession, int n, int n2, int n3, int n4, long l) throws IOException {
        return NativeCastMaterial.withPlacement(clientSession, 220, () -> gameHub.nativeMaterialPlaceClassicWorldCubeHarmony(clientSession, n, n2, n3, n4, l), BattleCubeHarmonyEffects.Placement.rejected());
    }

    private NativeCubeCastAction() {
    }
}
