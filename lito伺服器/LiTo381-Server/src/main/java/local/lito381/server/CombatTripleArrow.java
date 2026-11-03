// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.BattleWeaponCritical;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatRules;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.SourceWeaponPhysicalStats;

final class CombatTripleArrow {
    private static final ThreadLocal<Scope> current = new ThreadLocal();

    CombatTripleArrow() {
    }

    static Scope enter(ClientSession clientSession, int n) {
        Scope scope = new Scope(clientSession, n, current.get());
        current.set(scope);
        return scope;
    }

    static void leave(Scope scope) {
        if (current.get() != scope) {
            throw new IllegalStateException("triple-arrow scope mismatch");
        }
        if (scope.previous == null) {
            current.remove();
        } else {
            current.set(scope.previous);
        }
    }

    static boolean idle() {
        return current.get() == null;
    }

    static BattleWeaponCritical.DamageOutcome apply(SourceWeaponPhysicalStats sourceWeaponPhysicalStats, int n, int n2, double d, ClientSession clientSession) {
        Scope scope = current.get();
        if (scope != null && scope.caster == clientSession && !scope.consumed && scope.index >= 0 && scope.index < 3 && scope.binding.matches(clientSession) && sourceWeaponPhysicalStats.present() && (sourceWeaponPhysicalStats.weaponType() == 4 || sourceWeaponPhysicalStats.weaponType() == 13)) {
            scope.consumed = true;
            d = Math.max(d, CombatRules.number("Triple_Arrow_Dmg", 1.0));
        }
        return BattleWeaponCritical.apply(sourceWeaponPhysicalStats, n, n2, d);
    }

    static final class Scope {
        final ClientSession caster;
        final NativePlayerCommit.Binding binding;
        final int index;
        final Scope previous;
        boolean consumed;

        Scope(ClientSession clientSession, int n, Scope scope) {
            this.caster = clientSession;
            this.binding = NativePlayerCommit.Binding.capture(clientSession);
            this.index = n;
            this.previous = scope;
        }
    }
}
