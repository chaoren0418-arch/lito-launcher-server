package local.lito381.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

/** GM 測試授權留在測試邊界；正式 MemoryAccountRepository 沒有 grantAccessLevel。 */
final class AccountAccessFixture {
    private AccountAccessFixture() {}
    static AccountRepository grant(AccountRepository source, String account, int level) {
        return (AccountRepository) Proxy.newProxyInstance(AccountRepository.class.getClassLoader(),
                new Class<?>[]{AccountRepository.class}, (proxy, method, args) -> {
                    if (method.getName().equals("accessLevel") && account.equalsIgnoreCase((String) args[0])) return level;
                    try {
                        Object result=method.invoke(source,args);
                        return result instanceof AccountRepository child ? grant(child,account,level) : result;
                    } catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
    }
}
