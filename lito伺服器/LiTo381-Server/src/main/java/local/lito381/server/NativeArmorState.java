// ported from candidate 50635f7e; three-way baseline: base-0.6.71.jar and src/main
// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/armor-state/NativeArmorState.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import local.lito381.server.ClientSession;
import local.lito381.server.NativePlayerActionScope;
import local.lito381.server.NativeSourceManualScope;
/** Armor Break's canonical WORLD state uses the same action lock as its paired producer. */
final class NativeArmorState {
    static boolean activate(ClientSession clientSession, long l, int n) {
        boolean bl;
        NativePlayerActionScope.Ticket ticket = NativeSourceManualScope.enterState(clientSession);
        try {
            bl = clientSession.nativeArmorStateActivate(l, n);
        }
        catch (Throwable throwable) {
            NativeSourceManualScope.exitState(clientSession, ticket, throwable);
            throw throwable;
        }
        NativeSourceManualScope.exitState(clientSession, ticket, null);
        return bl;
    }
    static boolean active(ClientSession clientSession, long l) {
        boolean bl;
        NativePlayerActionScope.Ticket ticket = NativeSourceManualScope.enterState(clientSession);
        try {
            bl = clientSession.nativeArmorStateActive(l);
        }
        catch (Throwable throwable) {
            NativeSourceManualScope.exitState(clientSession, ticket, throwable);
            throw throwable;
        }
        NativeSourceManualScope.exitState(clientSession, ticket, null);
        return bl;
    }
    static boolean cancel(ClientSession clientSession) {
        boolean bl;
        NativePlayerActionScope.Ticket ticket = NativeSourceManualScope.enterState(clientSession);
        try {
            bl = clientSession.nativeArmorStateCancel();
        }
        catch (Throwable throwable) {
            NativeSourceManualScope.exitState(clientSession, ticket, throwable);
            throw throwable;
        }
        NativeSourceManualScope.exitState(clientSession, ticket, null);
        return bl;
    }
 private NativeArmorState(){}
}
