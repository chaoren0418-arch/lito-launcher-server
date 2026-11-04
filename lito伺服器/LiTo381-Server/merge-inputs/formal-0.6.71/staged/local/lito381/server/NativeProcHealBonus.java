// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

final class NativeProcHealBonus {
   private static volatile NativeProcHealBonus.Catalog current;

   static NativeProcHealBonus.Catalog read(Connection var0) throws SQLException {
      LinkedHashMap var1 = new LinkedHashMap();
      LinkedHashMap var2 = new LinkedHashMap();
      boolean var3 = false;

      try (
         Statement var4 = var0.createStatement();
         ResultSet var5 = var4.executeQuery("SELECT * FROM `角色_智力成長`");
      ) {
         ResultSetMetaData var6 = var5.getMetaData();

         for (int var7 = 1; var7 <= var6.getColumnCount(); var7++) {
            if (var6.getColumnLabel(var7).equals("治癒骰加成")) {
               var3 = true;
            }
         }

         while (var5.next()) {
            int var13 = var5.getInt("id");
            int var8 = var5.getInt("魔法加成");
            var1.put(var13, var8);
            var2.put(var13, var3 ? var5.getInt("治癒骰加成") : Math.min(var8, 10));
         }
      }

      return new NativeProcHealBonus.Catalog(var1, var2, var3);
   }

   static synchronized void load(ServerConfig var0) throws Exception {
      if (current == null) {
         Properties var1 = UnifiedConfiguration.read(var0.source());
         String var2 = SharedDatabase.identifier(var1.getProperty("database.static.database"));

         NativeProcHealBonus.Catalog var3;
         try (Connection var4 = DriverManager.getConnection(
               var1.getProperty("database.jdbc.base.url") + var2 + var1.getProperty("database.jdbc.options"),
               var1.getProperty("database.user"),
               var1.getProperty("database.password")
            )) {
            var4.setReadOnly(true);
            var4.setAutoCommit(false);
            var4.setTransactionIsolation(4);

            try {
               var3 = read(var4);
            } finally {
               var4.rollback();
            }
         }

         current = var3;
      }
   }

   static NativeProcHealBonus.Catalog catalog() {
      NativeProcHealBonus.Catalog var0 = current;
      if (var0 == null) {
         throw new IllegalStateException("Proc healing INT catalog not loaded at startup");
      } else {
         return var0;
      }
   }

   record Catalog(Map<Integer, Integer> magic, Map<Integer, Integer> healing, boolean explicitColumn) {
      Catalog(Map<Integer, Integer> magic, Map<Integer, Integer> healing, boolean explicitColumn) {
         magic = Map.copyOf(magic);
         healing = Map.copyOf(healing);
         this.magic = magic;
         this.healing = healing;
         this.explicitColumn = explicitColumn;
      }

      int bonus(int var1) {
         if (var1 < 1) {
            return -2;
         } else {
            int var2 = Math.min(var1, 127);
            return this.healing.getOrDefault(var2, Math.min(this.magic.getOrDefault(var2, 0), 10));
         }
      }
   }
}
