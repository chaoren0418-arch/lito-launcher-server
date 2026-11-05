// DECOMPILED from base-0.6.71.jar, TODO review (Vineflower 1.11.2)
package local.lito381.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import local.lito381.server.NativeDeItemApplication.Owner;

final class NativeCastMaterialPolicy {
   static boolean nativeOriginalGm(ClientSession var0) throws IOException {
      if (!var0.nativePlayerActionLock().isHeldByCurrentThread()) {
         throw new IllegalStateException("material policy requires actual action lock");
      } else {
         NativeReuseActor var1 = NativeReuseGate.actor(var0);
         if (var1 == null) {
            throw new IOException("material role unavailable");
         } else {
            NativeCastMaterialPolicy.State var2 = var1.materialPolicy;
            if (var2 != null && var2.owner() == var1.owner) {
               return var2.gm();
            } else {
               AccountRepository var3 = (AccountRepository)CombatAccess.call(var0, "selectedSourceAccounts", "()Llocal/lito381/server/AccountRepository;");
               String var4 = (String)CombatAccess.field(var0, "account");
               String var5 = var0.selectedCharacterName();
               long var6 = var0.playerObjectId();

               int var8;
               try {
                  LegacyMySqlRepository var9;
                  if (var3 instanceof NativeCastMaterialPolicy.Source var10) {
                     var8 = var10.characterAccessLevel(var4, var6, var5);
                  } else if (var3 instanceof MemoryAccountRepository) {
                     var8 = 0;
                  } else if (var3 instanceof LegacyMySqlRepository) {
                     var9 = (LegacyMySqlRepository)var3;

                     try (Connection var11 = var9.nativeMaterialCharacterConnection()) {
                        var11.setReadOnly(true);
                        var11.setTransactionIsolation(4);
                        var11.setAutoCommit(false);

                        try (PreparedStatement var12 = var11.prepareStatement(
                              "SELECT AccessLevel FROM characters WHERE objid = ? AND account_name = ? AND char_name = ? AND Banned = 0 LIMIT 1"
                           )) {
                           var12.setLong(1, var6);
                           var12.setString(2, var4);
                           var12.setString(3, var5);

                           try (ResultSet var13 = var12.executeQuery()) {
                              if (!var13.next()) {
                                 throw new IOException("material character policy missing");
                              }

                              var8 = var13.getInt("AccessLevel");
                           }
                        } finally {
                           var11.rollback();
                        }
                     }
                  } else {
                     throw new IOException("material character policy provider unavailable");
                  }
               } catch (IOException var33) {
                  throw var33;
               } catch (Exception var34) {
                  throw new IOException("material character policy read failed", var34);
               }

               if (var1.owner != NativeDeItemApplication.publicationOwner(var0)
                  || !var1.owner.matches(var0)
                  || var6 != var0.playerObjectId()
                  || !var5.equals(var0.selectedCharacterName())) {
                  throw new IOException("material character policy role changed");
               }
               var1.materialPolicy = new NativeCastMaterialPolicy.State(var1.owner, var3, var4, var6, var5, var8);
               return var1.materialPolicy.gm();
            }
         }
      }
   }

   private NativeCastMaterialPolicy() {
   }

   static boolean gm(ClientSession var0) throws IOException {
      return NativeWorldMaterialPolicy.gm(var0);
   }

   interface Source {
      int characterAccessLevel(String var1, long var2, String var4) throws Exception;
   }

   record State(Owner owner, AccountRepository repository, String account, long id, String name, int access) {
      boolean gm() {
         return this.access >= 200;
      }
   }
}
