// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.GameHub;

final class NativeWorldPotionPhase {
    static boolean allowed(ClientSession clientSession) {
        String string = String.valueOf(CombatAccess.field(clientSession, "phase"));
        if (string.equals("BATTLE")) {
            return true;
        }
        if (!string.equals("WORLD")) {
            return false;
        }
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        GameHub.ClassicWorldLocation classicWorldLocation = gameHub.classicWorldLocation(clientSession);
        return classicWorldLocation != null && classicWorldLocation.mapId() == ((Integer)CombatAccess.field(clientSession, "classicWorldMapId")).intValue() && classicWorldLocation.instanceId() == ((Integer)CombatAccess.field(clientSession, "classicWorldInstanceId")).intValue();
    }

    private NativeWorldPotionPhase() {
    }
}
