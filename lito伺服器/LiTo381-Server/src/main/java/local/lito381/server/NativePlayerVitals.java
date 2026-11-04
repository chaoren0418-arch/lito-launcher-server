// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.AccountRepository;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.SourceAdministration;

final class NativePlayerVitals {
    private static final ThreadLocal<Integer> minimum = new ThreadLocal();

    NativePlayerVitals() {
    }

    static int minimum() {
        Integer n = minimum.get();
        return n == null ? 0 : n;
    }

    static Result calculate(int n, int n2, int n3, int n4, boolean bl) {
        int n5;
        long l = Math.min((long)n - (long)n3, (long)n2);
        if (l <= (long)(n5 = Math.max(0, Math.min(n, Math.min(Math.max(0, n4), n2))))) {
            if (n5 > 0) {
                return new Result(n5, false);
            }
            if (bl) {
                return new Result(n2, false);
            }
            return new Result(0, true);
        }
        return new Result((int)Math.min(Integer.MAX_VALUE, l), false);
    }

    static boolean gm(ClientSession clientSession) {
        AccountRepository accountRepository = (AccountRepository)CombatAccess.call(clientSession, "selectedSourceAccounts", "()Llocal/lito381/server/AccountRepository;", new Object[0]);
        return SourceAdministration.accessLevel(accountRepository, clientSession.accountName(), clientSession) >= 200;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void nonlethal(ClientSession clientSession, int n, ClientSession clientSession2, boolean bl) {
        Integer n2 = minimum.get();
        minimum.set(1);
        try {
            if (bl) {
                CombatAccess.call(clientSession, "takeClassicWorldPlayerDamage", "(ILlocal/lito381/server/ClientSession;ZZ)V", n, clientSession2, false, false);
            } else {
                CombatAccess.call(clientSession, "takePlayerDamage", "(ILlocal/lito381/server/ClientSession;ZZJ)V", n, clientSession2, false, false, 0L);
            }
        }
        finally {
            if (n2 == null) {
                minimum.remove();
            } else {
                minimum.set(n2);
            }
        }
    }

    static boolean idle() {
        return minimum.get() == null;
    }

    record Result(int after, boolean fatal) {
    }
}
