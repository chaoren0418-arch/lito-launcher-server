// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.util.Objects;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeResurrectionEffects;
import local.lito381.server.NativeDeVitalState;

class NativeDeResurrectableState
extends NativeDeVitalState {
    private final NativeDeResurrectionEffects effects;

    NativeDeResurrectableState(int n, NativeDeCharacterFields.Snapshot snapshot) throws IOException {
        this(n, snapshot, NativeDeResurrectionEffects.UNBOUND);
    }

    NativeDeResurrectableState(int n, NativeDeCharacterFields.Snapshot snapshot, NativeDeResurrectionEffects nativeDeResurrectionEffects) throws IOException {
        super(n, snapshot);
        this.effects = Objects.requireNonNull(nativeDeResurrectionEffects);
    }

    NativeDeResurrectableState(int n, int n2, NativeDeResurrectionEffects nativeDeResurrectionEffects) {
        super(n, n2);
        this.effects = Objects.requireNonNull(nativeDeResurrectionEffects);
    }

    @Override
    protected NativeDeResurrectionEffects resurrectionEffects() {
        return this.effects;
    }
}
