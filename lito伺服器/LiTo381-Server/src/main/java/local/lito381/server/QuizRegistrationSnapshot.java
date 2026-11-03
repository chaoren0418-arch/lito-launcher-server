// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.List;

record QuizRegistrationSnapshot(List<String> fields) {
    QuizRegistrationSnapshot {
        fields = List.copyOf(fields);
    }
}
