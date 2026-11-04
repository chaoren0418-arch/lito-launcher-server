// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Map;
import local.lito381.server.NativeDeItemAbility;

final class NativeDeItemFrozenAbility
extends NativeDeItemAbility {
    NativeDeItemFrozenAbility(NativeDeItemAbility nativeDeItemAbility) {
        for (Map.Entry<NativeDeItemAbility.AbilityType, Integer> entry : nativeDeItemAbility.getAll().entrySet()) {
            super.set(entry.getKey(), entry.getValue());
        }
        if (!super.auditIntegrity("definition-freeze")) {
            throw new IllegalStateException("invalid source definition ability");
        }
    }

    @Override
    public synchronized void add(NativeDeItemAbility.AbilityType abilityType, int n) {
        throw NativeDeItemFrozenAbility.immutable();
    }

    @Override
    public synchronized void set(NativeDeItemAbility.AbilityType abilityType, int n) {
        throw NativeDeItemFrozenAbility.immutable();
    }

    @Override
    public synchronized void clear() {
        throw NativeDeItemFrozenAbility.immutable();
    }

    @Override
    public synchronized void setDebugContext(String string) {
        throw NativeDeItemFrozenAbility.immutable();
    }

    private static UnsupportedOperationException immutable() {
        return new UnsupportedOperationException("published item definition ability is immutable");
    }
}
