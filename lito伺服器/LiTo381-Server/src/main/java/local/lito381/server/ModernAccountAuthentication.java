package local.lito381.server;

import java.util.Arrays;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Injectable, source-scoped authentication boundary. Does not open repositories,
 * migrate tables or mutate GameStateStore. A verifier must bound its own backend
 * operations and must not retain/log the borrowed password array.
 */
final class ModernAccountAuthentication {
    private ModernAccountAuthentication() {}

    enum Failure { INVALID_CREDENTIALS, BACKEND_UNAVAILABLE, INVALID_STATE }

    static final class AuthenticationException extends Exception {
        private final Failure failure;
        AuthenticationException(Failure failure) {
            // Never include usernames, passwords, SQL URLs or backend exception messages.
            super("modern authentication failed: " + failure);
            this.failure = failure;
        }
        Failure failure() { return failure; }
    }

    record Source(int id, String name) {
        Source {
            if (id <= 0 || !validText(name, 64)) throw new IllegalArgumentException("invalid source identity");
        }
    }

    /** Identity only: later character selection must ALSO verify account ownership. */
    record CharacterKey(int sourceId, long objectId) {
        CharacterKey {
            if (sourceId <= 0 || objectId <= 0) throw new IllegalArgumentException("invalid source-scoped character key");
        }
    }

    /** Display account is not a database lookup key. Each grant owns its canonical key. */
    record Principal(String account, Set<Source> sources, Map<Integer, String> sourceAccounts) {
        Principal {
            if (!validAccount(account) || sources == null || sources.isEmpty() || sources.size() > 64
                    || sourceAccounts == null) {
                throw new IllegalArgumentException("invalid authenticated principal");
            }
            sources = Set.copyOf(sources);
            if (sources.stream().map(Source::id).distinct().count() != sources.size()) {
                throw new IllegalArgumentException("duplicate authorized source id");
            }
            sourceAccounts = Map.copyOf(sourceAccounts);
            if (sourceAccounts.size() != sources.size()
                    || sourceAccounts.values().stream().anyMatch(value -> !validAccount(value))) {
                throw new IllegalArgumentException("incomplete source account grants");
            }
            for (Source source : sources) {
                if (!sourceAccounts.containsKey(source.id())) {
                    throw new IllegalArgumentException("source account grant key mismatch");
                }
            }
        }
        /** Only a single-source principal can have an unambiguous convenience constructor. */
        Principal(String account, Set<Source> sources) {
            this(account, sources, singleSourceAccount(account, sources));
        }
        boolean authorizesSource(int sourceId) { return sources.stream().anyMatch(source -> source.id() == sourceId); }
        String accountForSource(int sourceId) {
            String canonical = sourceAccounts.get(sourceId);
            if (canonical == null) throw new IllegalArgumentException("unauthorized character source");
            return canonical;
        }
        CharacterKey characterKey(int sourceId, long objectId) {
            if (!authorizesSource(sourceId)) throw new IllegalArgumentException("unauthorized character source");
            return new CharacterKey(sourceId, objectId);
        }
    }

    private static Map<Integer, String> singleSourceAccount(String account, Set<Source> sources) {
        if (sources == null || sources.size() != 1) {
            throw new IllegalArgumentException("multiple sources require explicit canonical account grants");
        }
        return Map.of(sources.iterator().next().id(), account);
    }

    /** Explicit modern binding; aggregate/native primary routers are never lookup authority. */
    @FunctionalInterface
    interface AccountAuthenticator {
        AuthResult authenticate(String account, String password) throws Exception;
    }

    record SourceBinding(String name, AccountAuthenticator authenticator) {
        SourceBinding {
            if (!validText(name, 64) || authenticator == null) {
                throw new IllegalArgumentException("explicit concrete source binding required");
            }
        }
        SourceBinding(String name, AccountRepository repository) {
            this(name, concrete(repository));
        }
        static SourceBinding direct(String name, AccountAuthenticator authenticator) {
            return new SourceBinding(name, authenticator);
        }
        private static AccountAuthenticator concrete(AccountRepository repository) {
            if (repository == null || repository instanceof SourceRoutedAccountRepository) {
                throw new IllegalArgumentException("explicit concrete source binding required");
            }
            return repository::authenticate;
        }
    }

    @FunctionalInterface
    interface CredentialVerifier {
        /** Null is generic rejection (including banned accounts); exceptions mean backend unavailable. */
        Principal verify(String account, char[] borrowedPassword) throws Exception;
    }

    /** Consumes/clears the supplied password on success, rejection and backend failure. */
    static Principal verify(CredentialVerifier verifier, String account, char[] password)
            throws AuthenticationException {
        try {
            if (!validAccount(account) || password == null || password.length == 0 || password.length > 256) {
                throw new AuthenticationException(Failure.INVALID_CREDENTIALS);
            }
            for (char value : password) {
                if (value == 0 || Character.isISOControl(value)) {
                    throw new AuthenticationException(Failure.INVALID_CREDENTIALS);
                }
            }
            Principal principal;
            try {
                principal = Objects.requireNonNull(verifier).verify(account, password);
            } catch (Exception ignored) {
                // The backend may mention credentials in an exception: do not chain it outward.
                throw new AuthenticationException(Failure.BACKEND_UNAVAILABLE);
            }
            if (principal == null) throw new AuthenticationException(Failure.INVALID_CREDENTIALS);
            return principal;
        } finally {
            clear(password);
        }
    }

    /**
     * Single-source compatibility factory only. Multi-source authentication must
     * use repositories(): an aggregate AuthResult has already discarded all but
     * one source's canonical account and cannot safely recover the other owners.
     * The ban predicate can be GameStateStore::isAccountBanned (read-only).
     *
     * Legacy AccountRepository requires a String password, so this adapter has
     * one unavoidable immutable copy; removing that limitation requires a
     * separate repository API migration. It never stores or logs that copy.
     */
    static CredentialVerifier repository(AccountRepository repository, Map<Integer, String> configuredSources,
                                         Predicate<String> locallyBanned) {
        Objects.requireNonNull(repository);
        if (configuredSources == null || configuredSources.size() != 1
                || repository instanceof SourceRoutedAccountRepository) {
            throw new IllegalArgumentException("use explicit concrete modern bindings for multiple sources");
        }
        var source = configuredSources.entrySet().iterator().next();
        return repositories(Map.of(source.getKey(), new SourceBinding(source.getValue(), repository)), locallyBanned);
    }

    /**
     * Each concrete source authenticates exactly once with the original supplied
     * identity/password. Its successful canonical identity is retained privately
     * for all later reads from that source. Wire account is only deterministic
     * display identity (lowest successful source ID), never an owner fallback.
     *
     * As with the native router, a partial backend outage cannot grant the failed
     * source; successful other sources remain independently authorized. If none
     * succeed, an outage is a generic backend failure, not a password rejection.
     */
    static CredentialVerifier repositories(Map<Integer, SourceBinding> configured,
                                           Predicate<String> locallyBanned) {
        Objects.requireNonNull(locallyBanned);
        if (configured == null || configured.isEmpty() || configured.size() > 64) {
            throw new IllegalArgumentException("explicit modern source bindings required");
        }
        Map<Integer, SourceBinding> bindings = new TreeMap<>();
        Map<Integer, Source> sources = new LinkedHashMap<>();
        for (var entry : configured.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new IllegalArgumentException("invalid modern source binding");
            }
            Source source = new Source(entry.getKey(), entry.getValue().name());
            if (sources.values().stream().anyMatch(present -> present.name().equalsIgnoreCase(source.name()))) {
                throw new IllegalArgumentException("duplicate source name");
            }
            sources.put(source.id(), source);
            bindings.put(source.id(), entry.getValue());
        }
        Map<Integer, Source> immutableSources = Map.copyOf(sources);
        var orderedIds = bindings.keySet().stream().toList();
        Map<Integer, SourceBinding> immutableBindings = Map.copyOf(bindings);
        return (account, password) -> {
            if (locallyBanned.test(account)) return null;
            // AccountRepository's legacy String API requires this single
            // immutable copy. It is never logged/retained by this adapter.
            String suppliedPassword = new String(password);
            Set<Source> authorized = new LinkedHashSet<>();
            Map<Integer, String> sourceAccounts = new LinkedHashMap<>();
            boolean backendFailed = false;
            for (int id : orderedIds) {
                Source source = immutableSources.get(id);
                AuthResult authentication;
                try {
                    authentication = immutableBindings.get(id).authenticator().authenticate(account, suppliedPassword);
                    if (authentication == null) throw new IllegalStateException("missing source authentication result");
                    if (!authentication.accepted() || authentication.banned()) continue;
                    if (!validAccount(authentication.account())
                            || authentication.authorizedClassicSources().stream()
                            .anyMatch(name -> !source.name().equalsIgnoreCase(name))) {
                        throw new IllegalStateException("invalid concrete source authentication result");
                    }
                } catch (Exception ignored) {
                    backendFailed = true;
                    continue;
                }
                // Local account bans are global here. Never evade a canonical
                // ban by displaying a different successful source's alias.
                if (locallyBanned.test(authentication.account())) return null;
                authorized.add(source);
                sourceAccounts.put(id, authentication.account());
            }
            if (authorized.isEmpty()) {
                if (backendFailed) throw new IllegalStateException("source authentication backend unavailable");
                return null;
            }
            String displayAccount = sourceAccounts.get(authorized.iterator().next().id());
            return new Principal(displayAccount, authorized, sourceAccounts);
        };
    }

    static void clear(char[] password) { if (password != null) Arrays.fill(password, '\0'); }

    /** Modern identity width is UTF-8 bytes, not Java UTF-16 code units. */
    private static boolean validAccount(String value) {
        return validText(value, 64) && StandardCharsets.UTF_8.newEncoder().canEncode(value)
                && value.getBytes(StandardCharsets.UTF_8).length <= 64;
    }

    private static boolean validText(String value, int maximum) {
        return value != null && !value.isBlank() && value.length() <= maximum
                && value.chars().noneMatch(character -> character == 0 || Character.isISOControl(character));
    }
}
