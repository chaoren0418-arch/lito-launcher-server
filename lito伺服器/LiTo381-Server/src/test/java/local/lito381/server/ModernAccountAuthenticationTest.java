package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ModernAccountAuthenticationTest {
    @Test void concreteBindingsAuthorizeOnlyTheMatchingRepository() throws Exception {
        var sunCalls = new AtomicInteger();
        var moonCalls = new AtomicInteger();
        var verifier = ModernAccountAuthentication.repositories(Map.of(
                1, new ModernAccountAuthentication.SourceBinding("Sun", repository(new AuthResult(true, false, "Alice"), sunCalls)),
                2, new ModernAccountAuthentication.SourceBinding("Moon", repository(AuthResult.rejected("alice"), moonCalls))), account -> false);
        char[] password = "fixture-password".toCharArray();
        var principal = ModernAccountAuthentication.verify(verifier, "alice", password);
        assertEquals("Alice", principal.account());
        assertEquals(Set.of(new ModernAccountAuthentication.Source(1, "Sun")), principal.sources());
        assertTrue(principal.authorizesSource(1));
        assertFalse(principal.authorizesSource(2));
        assertEquals("Alice", principal.accountForSource(1));
        assertThrows(IllegalArgumentException.class, () -> principal.accountForSource(2));
        assertEquals(1, sunCalls.get());
        assertEquals(1, moonCalls.get());
        assertCleared(password);
    }

    @Test void legacyFactoryAndBindingsRejectAmbiguousMultiDatabaseRouters() throws Exception {
        var repo = repository(new AuthResult(true, false, "alice"), new AtomicInteger());
        assertThrows(IllegalArgumentException.class, () -> ModernAccountAuthentication.repository(
                repo, Map.of(1, "Sun", 2, "Moon"), account -> false));
        var router = new SourceRoutedAccountRepository(Map.of("Sun", repo, "Moon", repo));
        assertThrows(IllegalArgumentException.class, () -> ModernAccountAuthentication.repository(
                router, Map.of(1, "Sun"), account -> false));
        assertThrows(IllegalArgumentException.class, () -> new ModernAccountAuthentication.SourceBinding("Sun", router));
        var single = ModernAccountAuthentication.repository(repo, Map.of(7, "Sun"), account -> false);
        assertEquals(Set.of(new ModernAccountAuthentication.Source(7, "Sun")),
                ModernAccountAuthentication.verify(single, "alice", "fixture-password".toCharArray()).sources());
    }

    @Test void sameObjectIdInDifferentSourcesNeverSharesACharacterKey() {
        var principal = new ModernAccountAuthentication.Principal("alice", Set.of(
                new ModernAccountAuthentication.Source(1, "Sun"), new ModernAccountAuthentication.Source(2, "Moon")),
                Map.of(1, "sun-owner", 2, "moon-owner"));
        var first = principal.characterKey(1, 3333);
        var second = principal.characterKey(2, 3333);
        assertNotEquals(first, second);
        assertEquals(2, Map.of(first, "Sun character", second, "Moon character").size());
        assertThrows(IllegalArgumentException.class, () -> principal.characterKey(3, 3333));
        assertThrows(IllegalArgumentException.class, () -> principal.characterKey(1, 0));
    }

    @Test void bansUnknownSourceAndBackendErrorsFailClosedWithoutSecrets() {
        var calls = new AtomicInteger();
        var banned = ModernAccountAuthentication.repository(
                repository(new AuthResult(true, false, "alice"), calls), Map.of(1, "Sun"), account -> true);
        assertThrows(ModernAccountAuthentication.AuthenticationException.class,
                () -> ModernAccountAuthentication.verify(banned, "alice", "fixture-password".toCharArray()));
        assertEquals(0, calls.get());
        var unknown = ModernAccountAuthentication.repository(
                repository(new AuthResult(true, false, "alice", Set.of("unknown")), calls),
                Map.of(1, "Sun"), account -> false);
        assertThrows(ModernAccountAuthentication.AuthenticationException.class,
                () -> ModernAccountAuthentication.verify(unknown, "alice", "fixture-password".toCharArray()));
        char[] secret = "do-not-log-this-fixture".toCharArray();
        var failure = assertThrows(ModernAccountAuthentication.AuthenticationException.class,
                () -> ModernAccountAuthentication.verify((account, password) -> {
                    throw new IllegalStateException("backend leaked " + new String(password));
                }, "alice", secret));
        assertEquals(ModernAccountAuthentication.Failure.BACKEND_UNAVAILABLE, failure.failure());
        assertFalse(failure.toString().contains("do-not-log"));
        assertNull(failure.getCause());
        assertCleared(secret);
    }

    @Test void malformedCredentialsAreBoundedClearedAndDoNotCallVerifier() {
        var calls = new AtomicInteger();
        ModernAccountAuthentication.CredentialVerifier verifier = (account, password) -> { calls.incrementAndGet(); return null; };
        for (char[] password : new char[][] {new char[0], new char[257], {'a', '\0', 'b'}}) {
            assertThrows(ModernAccountAuthentication.AuthenticationException.class,
                    () -> ModernAccountAuthentication.verify(verifier, "alice", password));
            assertCleared(password);
        }
        assertEquals(0, calls.get());
        assertThrows(IllegalArgumentException.class,
                () -> ModernAccountAuthentication.repositories(Map.of(
                        1, new ModernAccountAuthentication.SourceBinding("Sun", repository(AuthResult.rejected("a"), calls)),
                        2, new ModernAccountAuthentication.SourceBinding("sun", repository(AuthResult.rejected("a"), calls))), account -> false));
    }

    @Test void differentCanonicalAccountsAreRetainedForActualSourceSpecificRosterReads() throws Exception {
        var backend = new ModernApplicationTestBackend("auth");
        char[] password = ModernApplicationTestBackend.PASSWORD.toCharArray();
        var principal = ModernAccountAuthentication.verify(backend.verifier, ModernApplicationTestBackend.ACCOUNT, password);
        assertEquals(ModernApplicationTestBackend.SUN_ACCOUNT, principal.account());
        assertEquals(Map.of(7, ModernApplicationTestBackend.SUN_ACCOUNT, 42, ModernApplicationTestBackend.MOON_ACCOUNT),
                principal.sourceAccounts());
        assertEquals(2, backend.authenticationCalls.get(), "one authentication per concrete source, not aggregate then re-auth");
        assertCleared(password);
        assertCleared(backend.borrowedPassword.get());
        // Each real modern read adapter calls its fixture repository, which
        // asserts the exact canonical account it independently authenticated.
        var sun = backend.characters.characters(principal, 7);
        var moon = backend.characters.characters(principal, 42);
        assertEquals("Sun Knight", sun.get(0).name());
        assertEquals("Moon Mage", moon.get(0).name());
        assertEquals(3333L, sun.get(0).objectId());
        assertEquals(3333L, moon.get(0).objectId());
        assertNotEquals(principal.characterKey(7, sun.get(0).objectId()), principal.characterKey(42, moon.get(0).objectId()));
        assertEquals(2, backend.characterCalls.get());
        assertThrows(SecurityException.class, () -> backend.characters.characters(principal, 99));
        assertEquals(2, backend.characterCalls.get(), "unauthorized sources never call a repository");
    }

    @Test void principalGrantMapIsCompleteExactImmutableAndNeverFallsBackToDisplayAccount() {
        var sun = new ModernAccountAuthentication.Source(1, "Sun");
        var moon = new ModernAccountAuthentication.Source(2, "Moon");
        var sources = Set.of(sun, moon);
        assertThrows(IllegalArgumentException.class, () -> new ModernAccountAuthentication.Principal("display", sources));
        for (Map<Integer, String> grants : java.util.List.of(
                Map.of(1, "sun-owner"), Map.of(1, "sun-owner", 2, "moon-owner", 3, "extra"),
                Map.of(1, "sun-owner", 3, "wrong-key"), Map.of(1, "sun-owner", 2, " "))) {
            assertThrows(IllegalArgumentException.class, () -> new ModernAccountAuthentication.Principal("display", sources, grants));
        }
        var supplied = new HashMap<>(Map.of(1, "sun-owner", 2, "moon-owner"));
        var principal = new ModernAccountAuthentication.Principal("display-only", sources, supplied);
        supplied.put(2, "changed-after-construction");
        assertEquals("moon-owner", principal.accountForSource(2));
        assertThrows(UnsupportedOperationException.class, () -> principal.sourceAccounts().put(2, "tamper"));
        assertThrows(IllegalArgumentException.class, () -> principal.accountForSource(3));
        assertEquals("single-owner", new ModernAccountAuthentication.Principal("single-owner", Set.of(sun)).accountForSource(1));
    }

    @Test void canonicalBansAndPartialBackendFailureCannotGrantAnUnavailableSource() throws Exception {
        var first = new AtomicInteger();
        var second = new AtomicInteger();
        var configured = Map.of(
                1, new ModernAccountAuthentication.SourceBinding("Sun", repository(new AuthResult(true, false, "sun-owner"), first)),
                2, new ModernAccountAuthentication.SourceBinding("Moon", repository(null, second)));
        var accepted = ModernAccountAuthentication.verify(ModernAccountAuthentication.repositories(configured, name -> false),
                "alias", "fixture-password".toCharArray());
        assertEquals(Set.of(new ModernAccountAuthentication.Source(1, "Sun")), accepted.sources());
        assertEquals(1, first.get()); assertEquals(1, second.get());
        assertThrows(IllegalArgumentException.class, () -> accepted.accountForSource(2));
        char[] password = "fixture-password".toCharArray();
        var rejected = assertThrows(ModernAccountAuthentication.AuthenticationException.class,
                () -> ModernAccountAuthentication.verify(ModernAccountAuthentication.repositories(configured,
                        name -> name.equals("sun-owner")), "alias", password));
        assertEquals(ModernAccountAuthentication.Failure.INVALID_CREDENTIALS, rejected.failure());
        assertCleared(password);

        var safeCalls = new AtomicInteger();
        var bannedCalls = new AtomicInteger();
        var aliases = Map.of(
                1, new ModernAccountAuthentication.SourceBinding("Sun", repository(new AuthResult(true, false, "safe-owner"), safeCalls)),
                2, new ModernAccountAuthentication.SourceBinding("Moon", repository(new AuthResult(true, false, "banned-owner"), bannedCalls)));
        char[] aliasPassword = "fixture-password".toCharArray();
        var canonicalBan = assertThrows(ModernAccountAuthentication.AuthenticationException.class,
                () -> ModernAccountAuthentication.verify(ModernAccountAuthentication.repositories(aliases,
                        name -> name.equals("banned-owner")), "unbanned-input-alias", aliasPassword));
        assertEquals(ModernAccountAuthentication.Failure.INVALID_CREDENTIALS, canonicalBan.failure());
        assertEquals(1, safeCalls.get(), "a successful earlier alias cannot hide a later globally banned canonical owner");
        assertEquals(1, bannedCalls.get());
        assertCleared(aliasPassword);

        var noAccepted = Map.of(
                1, new ModernAccountAuthentication.SourceBinding("Sun", repository(AuthResult.rejected("alias"), new AtomicInteger())),
                2, new ModernAccountAuthentication.SourceBinding("Moon", repository(null, new AtomicInteger())));
        char[] failedPassword = "fixture-password".toCharArray();
        var outage = assertThrows(ModernAccountAuthentication.AuthenticationException.class,
                () -> ModernAccountAuthentication.verify(ModernAccountAuthentication.repositories(noAccepted, name -> false),
                        "alias", failedPassword));
        assertEquals(ModernAccountAuthentication.Failure.BACKEND_UNAVAILABLE, outage.failure());
        assertCleared(failedPassword);
    }

    @Test void invalidCanonicalUtf8WidthOrSurrogateBecomesGenericBackendFailure() throws Exception {
        for (String canonical : new String[] {"界".repeat(22), "\ud800"}) {
            char[] password = "fixture-password".toCharArray();
            var verifier = ModernAccountAuthentication.repository(
                    repository(new AuthResult(true, false, canonical), new AtomicInteger()), Map.of(1, "Sun"), name -> false);
            var failure = assertThrows(ModernAccountAuthentication.AuthenticationException.class,
                    () -> ModernAccountAuthentication.verify(verifier, "alias", password));
            assertEquals(ModernAccountAuthentication.Failure.BACKEND_UNAVAILABLE, failure.failure());
            assertNull(failure.getCause());
            assertCleared(password);
        }
        var verifier = ModernAccountAuthentication.repository(
                repository(new AuthResult(true, false, "界".repeat(21)), new AtomicInteger()), Map.of(1, "Sun"), name -> false);
        assertEquals("界".repeat(21), ModernAccountAuthentication.verify(verifier, "alias", "fixture-password".toCharArray()).accountForSource(1));
    }

    private static void assertCleared(char[] value) { assertArrayEquals(new char[value.length], value); }
    private static AccountRepository repository(AuthResult result, AtomicInteger calls) {
        return (AccountRepository) Proxy.newProxyInstance(AccountRepository.class.getClassLoader(),
                new Class<?>[] {AccountRepository.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("authenticate")) {
                        calls.incrementAndGet();
                        if (result == null) throw new IllegalStateException("synthetic backend failure must not escape");
                        return result;
                    }
                    if (method.getName().equals("close")) return null;
                    throw new UnsupportedOperationException("unexpected test repository operation");
                });
    }
}
