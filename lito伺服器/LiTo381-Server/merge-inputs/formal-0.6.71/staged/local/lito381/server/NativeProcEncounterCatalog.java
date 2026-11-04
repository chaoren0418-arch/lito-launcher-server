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

final class NativeProcEncounterCatalog {
   static final String RESOURCE = "/combat/proc-npc-receivers.tsv";
   static final String QUERY = "SELECT npcid,impl,undead,lawful FROM npc ORDER BY npcid";
   private static volatile NativeProcEncounterCatalog.Catalog current;

   private NativeProcEncounterCatalog() {
   }

   static Map<String, NativeProcEncounterCatalog.Type> readTypes(InputStream var0) throws IOException {
      if (var0 == null) {
         throw new FileNotFoundException("/combat/proc-npc-receivers.tsv");
      } else {
         LinkedHashMap var1 = new LinkedHashMap();
         BufferedReader var2 = new BufferedReader(new InputStreamReader(var0, StandardCharsets.UTF_8));

         try {
            if (!"# source-npc-receivers-v1".equals(var2.readLine())) {
               throw new IOException("invalid NPC receiver resource version");
            }

            String var3;
            while ((var3 = var2.readLine()) != null) {
               String[] var4 = var3.split("\t", -1);
               if (var4.length != 4 || var4[0].isEmpty() || !var4[2].startsWith("com/lineage/server/model/Instance/")) {
                  throw new IOException("invalid NPC receiver resource row");
               }

               NativeProcEncounterCatalog.Type var5;
               try {
                  var5 = new NativeProcEncounterCatalog.Type(
                     NativeProcHealingKernel.Kind.valueOf(var4[1]), var4[2], NativeProcEncounterCatalog.Receiver.valueOf(var4[3])
                  );
               } catch (IllegalArgumentException var8) {
                  throw new IOException("invalid NPC receiver kind", var8);
               }

               if (var5.kind() == NativeProcHealingKernel.Kind.PLAYER || var1.putIfAbsent(var4[0], var5) != null) {
                  throw new IOException("duplicate/invalid NPC receiver implementation");
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
            throw new IOException("empty NPC receiver resource");
         } else {
            return Map.copyOf(var1);
         }
      }
   }

   static NativeProcEncounterCatalog.Catalog read(Connection var0, String var1, Map<String, NativeProcEncounterCatalog.Type> var2) throws SQLException {
      LinkedHashMap var3 = new LinkedHashMap();

      try (
         Statement var4 = var0.createStatement();
         ResultSet var5 = var4.executeQuery("SELECT npcid,impl,undead,lawful FROM npc ORDER BY npcid");
      ) {
         while (var5.next()) {
            String var6 = var5.getString(2);
            NativeProcEncounterCatalog.Npc var7 = new NativeProcEncounterCatalog.Npc(
               var5.getInt(1), var6, var5.getInt(3), var5.getInt(4), var6 == null ? null : (NativeProcEncounterCatalog.Type)var2.get(var6)
            );
            if (var3.putIfAbsent(var7.id(), var7) != null) {
               throw new SQLException("duplicate encounter NPC=" + var7.id());
            }
         }
      }

      if (var3.isEmpty()) {
         throw new SQLException("empty encounter NPC catalog");
      } else {
         return new NativeProcEncounterCatalog.Catalog(var1, var3, var2);
      }
   }

   static synchronized void load(ServerConfig var0) throws Exception {
      if (current == null) {
         Map var1 = readTypes(NativeProcEncounterCatalog.class.getResourceAsStream("/combat/proc-npc-receivers.tsv"));
         Properties var2 = UnifiedConfiguration.read(var0.source());
         String var3 = SharedDatabase.identifier(var2.getProperty("database.static.database"));

         NativeProcEncounterCatalog.Catalog var4;
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

         NativeProcNpcCatalog.Catalog var15 = NativeProcNpcCatalog.catalog();
         if (var15.npcs().size() != var4.npcs().size()) {
            throw new SQLException("NPC static snapshots changed during startup");
         } else {
            for (NativeProcEncounterCatalog.Npc var7 : var4.npcs().values()) {
               NativeProcNpcCatalog.Npc var8 = var15.find(var7.id());
               if (var8 == null || var8.undead() != var7.undead() || !Objects.equals(var8.implementation(), var7.implementation())) {
                  throw new SQLException("NPC static metadata changed during startup id=" + var7.id());
               }

               if (var7.type() != null && var15.implementations().get(var7.implementation()) != var7.kind()) {
                  throw new SQLException("source NPC hierarchy changed without kind qualification");
               }
            }

            current = var4;
         }
      }
   }

   static NativeProcEncounterCatalog.Catalog catalog() {
      NativeProcEncounterCatalog.Catalog var0 = current;
      if (var0 == null) {
         throw new IllegalStateException("encounter Proc NPC catalog not loaded");
      } else {
         return var0;
      }
   }

   record Catalog(String database, Map<Integer, NativeProcEncounterCatalog.Npc> npcs, Map<String, NativeProcEncounterCatalog.Type> types) {
      Catalog(String database, Map<Integer, NativeProcEncounterCatalog.Npc> npcs, Map<String, NativeProcEncounterCatalog.Type> types) {
         npcs = Map.copyOf(npcs);
         types = Map.copyOf(types);
         this.database = database;
         this.npcs = npcs;
         this.types = types;
      }

      NativeProcEncounterCatalog.Npc find(int var1) {
         return this.npcs.get(var1);
      }

      NativeProcEncounterCatalog.Npc require(int var1) {
         NativeProcEncounterCatalog.Npc var2 = this.find(var1);
         if (var2 != null && var2.type() != null) {
            if (var2.receiver() == NativeProcEncounterCatalog.Receiver.UNSUPPORTED
               && var2.kind() != NativeProcHealingKernel.Kind.TOWER
               && var2.kind() != NativeProcHealingKernel.Kind.DOOR) {
               throw new IllegalArgumentException("unported encounter NPC receiver=" + var2.implementation() + " owner=" + var2.type().owner());
            } else {
               return var2;
            }
         } else {
            throw new IllegalArgumentException("unqualified encounter NPC=" + var1);
         }
      }
   }

   record Npc(int id, String implementation, int undead, int lawful, NativeProcEncounterCatalog.Type type) {
      NativeProcHealingKernel.Kind kind() {
         return this.type.kind();
      }

      NativeProcEncounterCatalog.Receiver receiver() {
         return this.type.receiver();
      }
   }

   static enum Receiver {
      MONSTER,
      PET,
      SUMMON,
      GUARD,
      POSITIVE_ONLY,
      NONE,
      UNSUPPORTED,
      FOLLOWER;
   }

   record Type(NativeProcHealingKernel.Kind kind, String owner, NativeProcEncounterCatalog.Receiver receiver) {
   }
}
