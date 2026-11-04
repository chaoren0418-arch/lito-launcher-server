// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import local.lito381.server.AccountRepository;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.LegacyMySqlRepository;
import local.lito381.server.MemoryAccountRepository;
import local.lito381.server.NativeDeItemApplication;
import local.lito381.server.NativeReuseActor;
import local.lito381.server.NativeReuseGate;
import local.lito381.server.NativeWorldMaterialPolicy;

final class NativeCastMaterialPolicy {
    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean nativeOriginalGm(ClientSession clientSession) throws IOException {
        int n;
        long l;
        String string;
        String string2;
        AccountRepository accountRepository;
        NativeReuseActor nativeReuseActor;
        block32: {
            if (!clientSession.nativePlayerActionLock().isHeldByCurrentThread()) {
                throw new IllegalStateException("material policy requires actual action lock");
            }
            nativeReuseActor = NativeReuseGate.actor(clientSession);
            if (nativeReuseActor == null) {
                throw new IOException("material role unavailable");
            }
            State state = nativeReuseActor.materialPolicy;
            if (state != null && state.owner() == nativeReuseActor.owner) {
                return state.gm();
            }
            accountRepository = (AccountRepository)CombatAccess.call(clientSession, "selectedSourceAccounts", "()Llocal/lito381/server/AccountRepository;", new Object[0]);
            string2 = (String)CombatAccess.field(clientSession, "account");
            string = clientSession.selectedCharacterName();
            l = clientSession.playerObjectId();
            try {
                if (accountRepository instanceof Source) {
                    Source source = (Source)((Object)accountRepository);
                    n = source.characterAccessLevel(string2, l, string);
                    break block32;
                }
                if (accountRepository instanceof MemoryAccountRepository) {
                    n = 0;
                    break block32;
                }
                if (accountRepository instanceof LegacyMySqlRepository) {
                    LegacyMySqlRepository legacyMySqlRepository = (LegacyMySqlRepository)accountRepository;
                    try (Connection connection = (Connection)CombatAccess.call(legacyMySqlRepository, "nativeMaterialCharacterConnection", "()Ljava/sql/Connection;");){
                        connection.setReadOnly(true);
                        connection.setTransactionIsolation(4);
                        connection.setAutoCommit(false);
                        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT AccessLevel FROM characters WHERE objid = ? AND account_name = ? AND char_name = ? AND Banned = 0 LIMIT 1");){
                            preparedStatement.setLong(1, l);
                            preparedStatement.setString(2, string2);
                            preparedStatement.setString(3, string);
                            try (ResultSet resultSet = preparedStatement.executeQuery();){
                                if (!resultSet.next()) {
                                    throw new IOException("material character policy missing");
                                }
                                n = resultSet.getInt("AccessLevel");
                                break block32;
                            }
                        }
                        finally {
                            connection.rollback();
                        }
                    }
                }
                throw new IOException("material character policy provider unavailable");
            }
            catch (IOException iOException) {
                throw iOException;
            }
            catch (Exception exception) {
                throw new IOException("material character policy read failed", exception);
            }
        }
        if (nativeReuseActor.owner != NativeDeItemApplication.publicationOwner(clientSession) || !nativeReuseActor.owner.matches(clientSession) || l != clientSession.playerObjectId() || !string.equals(clientSession.selectedCharacterName())) {
            throw new IOException("material character policy role changed");
        }
        nativeReuseActor.materialPolicy = new State(nativeReuseActor.owner, accountRepository, string2, l, string, n);
        return nativeReuseActor.materialPolicy.gm();
    }

    private NativeCastMaterialPolicy() {
    }

    static boolean gm(ClientSession clientSession) throws IOException {
        return NativeWorldMaterialPolicy.gm(clientSession);
    }

    record State(NativeDeItemApplication.Owner owner, AccountRepository repository, String account, long id, String name, int access) {
        boolean gm() {
            return this.access >= 200;
        }
    }

    static interface Source {
        public int characterAccessLevel(String var1, long var2, String var4) throws Exception;
    }
}
