// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Locale;

record GmCommandDefinition(String name, int requiredAccessLevel, String executor, String note, boolean systemCommand) {
    GmCommandDefinition {
        name = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        executor = executor == null ? "" : executor.trim();
        String string = note = note == null ? "" : note.trim();
        if (name.isEmpty() || requiredAccessLevel < 0 || requiredAccessLevel > 200 || executor.isEmpty()) {
            throw new IllegalArgumentException("invalid GM command definition");
        }
    }
}
