package local.lito381.server;
import java.util.*;

/** Last-resort release after the normal ordered disconnect path has run. */
public final class SelectionRecovery {
    public static void finish(ClientSession session) {
        GameHub hub=(GameHub)CombatAccess.field(session,"hub");
        synchronized(hub) {
            Collection<?> connected=(Collection<?>)CombatAccess.field(hub,"connectedSessions");
            if(!connected.contains(session))return;
            System.out.println("[LiTo] recovering incomplete session disconnect");
            try {
                hub.disconnect(session);
            } catch(RuntimeException failure) {
                failure.printStackTrace(System.out);
                // A failed optional teardown must never reserve a dead account.
                connected.remove(session);
                ((Map<?,?>)CombatAccess.field(hub,"authenticatedGameAccounts"))
                    .values().removeIf(value->value==session);
            }
        }
    }
}
