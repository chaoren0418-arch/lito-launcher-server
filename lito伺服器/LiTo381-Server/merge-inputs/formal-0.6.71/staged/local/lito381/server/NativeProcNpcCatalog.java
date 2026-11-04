// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

final class NativeProcNpcCatalog {
   static final String RESOURCE = "/combat/proc-npc-kinds.tsv";
   static final String QUERY = "SELECT npcid,impl,undead FROM npc ORDER BY npcid";
   private static volatile NativeProcNpcCatalog.Catalog current;

   private NativeProcNpcCatalog() {
   }

   static Map<String, NativeProcHealingKernel.Kind> readKinds(InputStream var0) throws IOException {
      if (var0 == null) {
         throw new FileNotFoundException("missing frozen Proc NPC ancestry /combat/proc-npc-kinds.tsv");
      } else {
         LinkedHashMap var1 = new LinkedHashMap();
         BufferedReader var2 = new BufferedReader(new InputStreamReader(var0, StandardCharsets.UTF_8));

         try {
            if (!"# source-npc-kinds-v1".equals(var2.readLine())) {
               throw new IOException("invalid Proc NPC ancestry version");
            }

            String var3;
            while ((var3 = var2.readLine()) != null) {
               String[] var4 = var3.split("\t", -1);
               if (var4.length != 2 || var4[0].isEmpty()) {
                  throw new IOException("invalid Proc NPC ancestry row");
               }

               NativeProcHealingKernel.Kind var5;
               try {
                  var5 = NativeProcHealingKernel.Kind.valueOf(var4[1]);
               } catch (IllegalArgumentException var8) {
                  throw new IOException("invalid Proc NPC ancestry kind", var8);
               }

               if (var5 == NativeProcHealingKernel.Kind.PLAYER || var1.putIfAbsent(var4[0], var5) != null) {
                  throw new IOException("invalid or duplicate Proc NPC ancestry=" + var4[0]);
               }
            }
         } catch (Throwable var9) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var9.addSuppressed(var7);
            }

            throw var9;
         }

         var2.close();
         if (var1.isEmpty()) {
            throw new IOException("empty Proc NPC ancestry");
         } else {
            return Map.copyOf(var1);
         }
      }
   }

   static NativeProcNpcCatalog.Catalog read(Connection var0, String var1, Map<String, NativeProcHealingKernel.Kind> var2) throws SQLException {
      LinkedHashMap var3 = new LinkedHashMap();

      try (
         Statement var4 = var0.createStatement();
         ResultSet var5 = var4.executeQuery("SELECT npcid,impl,undead FROM npc ORDER BY npcid");
      ) {
         while (var5.next()) {
            NativeProcNpcCatalog.Npc var6 = new NativeProcNpcCatalog.Npc(var5.getInt(1), var5.getString(2), var5.getInt(3));
            if (var3.putIfAbsent(var6.id(), var6) != null) {
               throw new SQLException("duplicate Proc npcid=" + var6.id());
            }
         }
      }

      if (var3.isEmpty()) {
         throw new SQLException("empty Proc NPC catalog");
      } else {
         return new NativeProcNpcCatalog.Catalog(var1, var3, var2);
      }
   }

   static synchronized void load(ServerConfig var0) throws Exception {
      if (current == null) {
         Map var1 = readKinds(NativeProcNpcCatalog.class.getResourceAsStream("/combat/proc-npc-kinds.tsv"));
         Properties var2 = UnifiedConfiguration.read(var0.source());
         String var3 = SharedDatabase.identifier(var2.getProperty("database.static.database"));

         NativeProcNpcCatalog.Catalog var4;
         try (Connection var5 = DriverManager.getConnection(
               var2.getProperty("database.jdbc.base.url") + var3 + var2.getProperty("database.jdbc.options"),
               var2.getProperty("database.user"),
               var2.getProperty("database.password")
            )) {
            var5.setReadOnly(true);
            var5.setAutoCommit(false);
            var5.setTransactionIsolation(4);

            try {
               var4 = read(var5, var3, var1);
            } finally {
               var5.rollback();
            }
         }

         current = var4;
      }
   }

   static NativeProcNpcCatalog.Catalog catalog() {
      NativeProcNpcCatalog.Catalog var0 = current;
      if (var0 == null) {
         throw new IllegalStateException("Proc NPC catalog not loaded at startup");
      } else {
         return var0;
      }
   }

   static NativeProcHealingKernel.Kind companionKind(BattleCompanion.Kind var0) {
      return switch ((BattleCompanion.Kind)Objects.requireNonNull(var0)) {
         case PET -> NativeProcHealingKernel.Kind.PET;
         case SUMMON, TAMED -> NativeProcHealingKernel.Kind.SUMMON;
         case ARENA -> NativeProcHealingKernel.Kind.NPC;
      };
   }

   record Catalog(String database, Map<Integer, NativeProcNpcCatalog.Npc> npcs, Map<String, NativeProcHealingKernel.Kind> implementations) {
      Catalog(String database, Map<Integer, NativeProcNpcCatalog.Npc> npcs, Map<String, NativeProcHealingKernel.Kind> implementations) {
         npcs = Map.copyOf(npcs);
         implementations = Map.copyOf(implementations);
         this.database = database;
         this.npcs = npcs;
         this.implementations = implementations;
      }

      NativeProcNpcCatalog.Npc find(int var1) {
         return this.npcs.get(var1);
      }

      NativeProcHealingKernel.Kind kind(int var1) {
         NativeProcNpcCatalog.Npc var2 = this.find(var1);
         if (var2 == null) {
            throw new IllegalArgumentException("missing Proc NPC template=" + var1);
         } else {
            NativeProcHealingKernel.Kind var3 = var2.implementation() == null ? null : this.implementations.get(var2.implementation());
            if (var3 == null) {
               throw new IllegalArgumentException("unqualified Proc NPC implementation id=" + var1 + " impl=" + var2.implementation());
            } else {
               return var3;
            }
         }
      }
   }

   record Npc(int id, String implementation, int undead) {
   }
}
