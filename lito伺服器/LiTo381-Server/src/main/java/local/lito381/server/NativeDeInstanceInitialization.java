// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import local.lito381.server.NativeDeInitializerKernel;
import local.lito381.server.NativeDeInstanceNames;
import local.lito381.server.NativeDeInstanceScope;
import local.lito381.server.NativeDeInstanceState;

final class NativeDeInstanceInitialization {
    static void init(NativeDeInstanceState nativeDeInstanceState, NativeDeInitializerKernel.Name name, boolean bl, String string) {
        NativeDeInstanceScope.run(nativeDeInstanceState, () -> NativeDeInitializerKernel.init(NativeDeInstanceNames.services(nativeDeInstanceState), nativeDeInstanceState, NativeDeInstanceNames.name(nativeDeInstanceState.construction, name), bl, string));
    }

    static void initDoll(NativeDeInstanceState nativeDeInstanceState) {
        NativeDeInstanceScope.run(nativeDeInstanceState, () -> NativeDeInitializerKernel.initDoll(NativeDeInstanceNames.services(nativeDeInstanceState), nativeDeInstanceState));
    }

    static void initPet(NativeDeInstanceState nativeDeInstanceState) {
        NativeDeInstanceScope.run(nativeDeInstanceState, () -> NativeDeInitializerKernel.initPet(NativeDeInstanceNames.services(nativeDeInstanceState), nativeDeInstanceState));
    }

    static ArrayList<NativeDeInitializerKernel.Name> names(NativeDeInstanceState nativeDeInstanceState) {
        return nativeDeInstanceState.deletionOwner.services.names();
    }

    static ReentrantLock namesLock(NativeDeInstanceState nativeDeInstanceState) {
        return nativeDeInstanceState.deletionOwner.services.namesLock();
    }

    private NativeDeInstanceInitialization() {
    }
}
