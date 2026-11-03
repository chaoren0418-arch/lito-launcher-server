// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Collection;
import java.util.Map;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.GameHub;

public final class SelectionRecovery {
    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void finish(ClientSession clientSession) {
        GameHub gameHub;
        GameHub gameHub2 = gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        synchronized (gameHub2) {
            Collection collection = (Collection)CombatAccess.field(gameHub, "connectedSessions");
            if (!collection.contains(clientSession)) {
                return;
            }
            System.out.println("[LiTo] recovering incomplete session disconnect");
            try {
                gameHub.disconnect(clientSession);
            }
            catch (RuntimeException runtimeException) {
                runtimeException.printStackTrace(System.out);
                collection.remove(clientSession);
                ((Map)CombatAccess.field(gameHub, "authenticatedGameAccounts")).values().removeIf(object -> object == clientSession);
            }
        }
    }
}
