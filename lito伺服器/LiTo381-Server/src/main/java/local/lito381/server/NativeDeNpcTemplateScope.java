// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Random;
import local.lito381.server.NativeDeNpcTemplateState;

final class NativeDeNpcTemplateScope {
    private static final ThreadLocal<NativeDeNpcTemplateState> current = new ThreadLocal();

    NativeDeNpcTemplateScope() {
    }

    static NativeDeNpcTemplateState actor() {
        NativeDeNpcTemplateState nativeDeNpcTemplateState = current.get();
        if (nativeDeNpcTemplateState == null) {
            throw new IllegalStateException("NPC template scope absent");
        }
        return nativeDeNpcTemplateState;
    }

    static Random random() {
        return NativeDeNpcTemplateScope.actor().initialization.random();
    }

    static void run(NativeDeNpcTemplateState nativeDeNpcTemplateState, Runnable runnable) {
        NativeDeNpcTemplateState nativeDeNpcTemplateState2 = current.get();
        current.set(nativeDeNpcTemplateState);
        try {
            runnable.run();
        }
        finally {
            if (nativeDeNpcTemplateState2 == null) {
                current.remove();
            } else {
                current.set(nativeDeNpcTemplateState2);
            }
        }
    }

    static boolean present() {
        return current.get() != null;
    }
}
