// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Arrays;
import local.lito381.server.NativeDeNpcTemplateLoadScope;
import local.lito381.server.NativeDeNpcTemplateScope;
import local.lito381.server.NativeDeWorldLog;

final class NativeDeNpcTemplateLog {
    NativeDeNpcTemplateLog() {
    }

    static NativeDeWorldLog getLogger(Class<?> clazz) {
        return (string, string2, objectArray) -> {
            if (NativeDeNpcTemplateLoadScope.present()) {
                NativeDeNpcTemplateLoadScope.context().log.emit(string, string2, objectArray);
                return;
            }
            if (NativeDeNpcTemplateScope.present()) {
                NativeDeNpcTemplateScope.actor().initialization.log().emit(string, string2, objectArray);
                return;
            }
            System.getLogger(clazz.getName()).log(string.equals("error") ? System.Logger.Level.ERROR : (string.equals("warn") ? System.Logger.Level.WARNING : System.Logger.Level.INFO), string2 + " " + Arrays.toString(objectArray));
        };
    }
}
