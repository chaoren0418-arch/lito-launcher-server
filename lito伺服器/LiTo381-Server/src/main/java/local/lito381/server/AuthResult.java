// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Set;

record AuthResult(boolean accepted, boolean banned, String account, Set<String> authorizedClassicSources) {
    AuthResult {
        authorizedClassicSources = authorizedClassicSources == null ? Set.of() : Set.copyOf(authorizedClassicSources);
    }

    AuthResult(boolean accepted, boolean banned, String account) {
        this(accepted, banned, account, Set.of());
    }

    static AuthResult rejected(String account) {
        return new AuthResult(false, false, account, Set.of());
    }

    boolean authorizesClassicSource(String classicServerName) {
        if (this.authorizedClassicSources.isEmpty()) {
            return true;
        }
        return this.authorizedClassicSources.stream().anyMatch(source -> source.equalsIgnoreCase(classicServerName));
    }
}
