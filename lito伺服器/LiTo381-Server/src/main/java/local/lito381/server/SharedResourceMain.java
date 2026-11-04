// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.Main;
import local.lito381.server.SharedClientResources;

public final class SharedResourceMain {
    public static void main(String[] stringArray) throws Exception {
        SharedClientResources.initialize();
        Main.main(stringArray);
    }
}
