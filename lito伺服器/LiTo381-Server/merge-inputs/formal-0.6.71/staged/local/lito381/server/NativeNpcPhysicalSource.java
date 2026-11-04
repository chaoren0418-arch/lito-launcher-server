// ORIGINAL SOURCE aligned to base-0.6.71.jar (formal behavior restored from reference JAR)
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class NativeNpcPhysicalSource {
   private static volatile NativeNpcPhysicalSource.Snapshot current;

   static NativeNpcPhysicalSource.Snapshot read(Connection var0, ServerConfig var1) throws SQLException {
      String var2 = var0.getCatalog();

      for (String var4 : var1.legacySourceResourceDatabases()) {
         if (!Objects.equals(var2, var4)) {
            throw new SQLException("NPC snapshot requires the configured common static database: " + var4);
         }
      }

      return read(var0);
   }

   static NativeNpcPhysicalSource.Snapshot read(Connection var0) throws SQLException {
      String var1 = var0.getCatalog();
      if (var1 != null && var1.matches("[A-Za-z0-9_]+")) {
         HashMap var2 = new HashMap();
         HashSet var3 = new HashSet();
         String var4 = "SELECT npcid,ac,size,damage_reduction,hard,undead,weakAttr,mr,IsTU,IsErase,lawful,tamable,boss,mp,str,dex,ranged,bowActId,poison_atk,paralysis_atk FROM npc";

         try (
            PreparedStatement var5 = var0.prepareStatement(var4);
            ResultSet var6 = var5.executeQuery();
         ) {
            while (var6.next()) {
               int var7 = var6.getInt("npcid");
               if (var2.containsKey(var7) || var3.contains(var7)) {
                  throw new SQLException("duplicate source NPC physical row: " + var7);
               }

               try {
                  var2.put(
                     var7,
                     SourceNpcPhysicalStats.fromSourceRow(
                        var7,
                        var6.getInt("ac"),
                        physicalSize(var6.getString("size")),
                        var6.getInt("damage_reduction"),
                        var6.getBoolean("hard"),
                        var6.getInt("undead"),
                        var6.getInt("weakAttr"),
                        var6.getInt("mr"),
                        var6.getBoolean("IsTU"),
                        var6.getInt("lawful"),
                        var6.getBoolean("tamable"),
                        var6.getBoolean("boss"),
                        var6.getInt("mp"),
                        var6.getInt("str"),
                        var6.getInt("dex"),
                        var6.getBoolean("IsErase"),
                        var6.getInt("ranged"),
                        var6.getInt("bowActId"),
                        var6.getInt("poison_atk"),
                        var6.getInt("paralysis_atk")
                     )
                  );
               } catch (IllegalArgumentException var11) {
                  var3.add(var7);
               }
            }
         }

         return new NativeNpcPhysicalSource.Snapshot(var1 + ".npc", var2, var3);
      } else {
         throw new SQLException("NPC snapshot has no qualified static catalog");
      }
   }

   static void publish(NativeNpcPhysicalSource.Snapshot var0) {
      current = Objects.requireNonNull(var0);
   }

   static SourceNpcPhysicalStats lookup(String var0, int var1) throws SQLException {
      if (var1 <= 0) {
         throw new IllegalArgumentException("invalid source NPC id");
      } else {
         NativeNpcPhysicalSource.Snapshot var2 = current;
         if (var2 == null) {
            return null;
         } else if (!var2.table.equals(var0) && !("`" + var2.table.replace(".", "`.`") + "`").equals(var0)) {
            throw new SQLException("NPC snapshot static source mismatch: " + var0);
         } else if (var2.invalid.contains(var1)) {
            throw new SQLException("invalid source NPC physical row: " + var1, new IllegalArgumentException("invalid source NPC physical snapshot"));
         } else {
            SourceNpcPhysicalStats var3 = var2.rows.get(var1);
            if (var3 == null) {
               throw new SQLException("source NPC physical row was not found: " + var1);
            } else {
               return var3;
            }
         }
      }
   }

   static String physicalSize(String var0) {
      return "".equals(var0) ? "small" : var0;
   }

   private NativeNpcPhysicalSource() {
   }

   record Snapshot(String table, Map<Integer, SourceNpcPhysicalStats> rows, Set<Integer> invalid) {
      Snapshot(String table, Map<Integer, SourceNpcPhysicalStats> rows, Set<Integer> invalid) {
         rows = Map.copyOf(rows);
         invalid = Set.copyOf(invalid);
         this.table = table;
         this.rows = rows;
         this.invalid = invalid;
      }
   }
}
