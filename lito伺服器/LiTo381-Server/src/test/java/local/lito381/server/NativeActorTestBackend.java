package local.lito381.server;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Synthetic repositories using concrete-source auth/read-only adapters; never opens a DB. */
final class NativeActorTestBackend {
    static final String ACCOUNT = "fixture-user";
    static final String PASSWORD = "fixture-password";
    static final String SUN_ACCOUNT = "sun-owner";
    static final String MOON_ACCOUNT = "moon-owner";
    final AtomicInteger authenticationCalls = new AtomicInteger();
    final AtomicInteger characterCalls = new AtomicInteger();
    final AtomicReference<char[]> borrowedPassword = new AtomicReference<>();
    final ModernAccountAuthentication.CredentialVerifier verifier;
    final ModernCharacterRepository characters;

    NativeActorTestBackend(String mode) {
        this(mode,120_000);
    }
    /** Explicit fixture level, never an authority override or production damage cap. */
    NativeActorTestBackend(String mode,int moonLevel) {
        if(moonLevel<1)throw new IllegalArgumentException("invalid synthetic level");
        var sun = repository(SUN_ACCOUNT, mode.equals("timed") ? "Sun Mage" : "Sun Knight", mode.equals("timed") ? 3 : 1, 0, 80, mode);
        boolean moonElf=mode.equals("timed")||mode.equals("inventory-elf");
        var moon = repository(MOON_ACCOUNT, moonElf ? "Moon Elf" : "Moon Mage", moonElf ? 2 : 3, 1, moonLevel, mode);
        var actual = ModernAccountAuthentication.repositories(Map.of(
                7, new ModernAccountAuthentication.SourceBinding("太陽神", sun),
                42, new ModernAccountAuthentication.SourceBinding("月亮", moon)), name -> false);
        verifier = (account, password) -> {
            borrowedPassword.set(password);
            return actual.verify(account, password);
        };
        characters = ModernCharacterRepository.repositories(Map.of(7, sun, 42, moon));
    }

    private AccountRepository repository(String canonicalAccount, String characterName, int type, int sex, int level, String mode) {
        return (AccountRepository) Proxy.newProxyInstance(AccountRepository.class.getClassLoader(),
                new Class<?>[] {AccountRepository.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("authenticate")) {
                        authenticationCalls.incrementAndGet();
                        if (mode.equals("auth-backend")) throw new IllegalStateException("synthetic private backend detail");
                        return ACCOUNT.equals(arguments[0]) && PASSWORD.equals(arguments[1])
                                ? new AuthResult(true, false, canonicalAccount) : AuthResult.rejected((String) arguments[0]);
                    }
                    if (method.getName().equals("characters")) {
                        characterCalls.incrementAndGet();
                        if (!canonicalAccount.equals(arguments[0])) throw new AssertionError("character lookup not scoped to source canonical account");
                        if (mode.equals("characters-backend")) throw new IllegalStateException("synthetic private backend detail");
                        if (mode.equals("characters-slow")) Thread.sleep(1_000);
                        // Deliberately the SAME objectId in two distinct source repositories.
                        return List.of(new CharacterSummary(characterName, "", 0, 0, level, 500, 300,
                                -20, 18, 18, 18, 12, 18, 18, 18, 18,
                                3333L, 0L, 0L, 0, 9999L, 32768L, 32768L, 4L));
                    }
                    if (method.getName().equals("close")) return null;
                    throw new UnsupportedOperationException("unexpected fixture repository operation");
                });
    }
}

