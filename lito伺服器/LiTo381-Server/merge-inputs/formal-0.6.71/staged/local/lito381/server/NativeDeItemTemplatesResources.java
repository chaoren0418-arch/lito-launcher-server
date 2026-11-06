// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.lang.System.Logger.Level;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.function.Supplier;
import local.lito381.server.NativeDeItemDefinitionsSql.Counts;
import local.lito381.server.NativeDeTerrainScope.Sql;

final class NativeDeItemTemplatesResources {
   static Snapshot read(String database, Sql sql, NativeDeWorldLog log) throws SQLException {
        NativeDeItemTemplatesScope.Data data = new NativeDeItemTemplatesScope.Data(log);
        NativeDeItemDefinitionsSql definitions = new NativeDeItemDefinitionsSql(sql.open(), NativeDeItemDefinitionsSql.TEMPLATE_QUERIES);
        Throwable failure = null;
        try {
            definitions.configure();
            NativeDeItemTemplatesScope.loading(data, definitions, () -> {
                data.table = new NativeDeItemTable();
                data.table.load();
                return null;
            });
            if (data.sourceErrors != 0L) {
                throw new SQLException("source item template errors; publication refused: " + data.sourceErrors);
            }
            definitions.validate();
        } catch (Throwable caught) {
            failure = caught;
        } finally {
            try {
                definitions.finish();
            } catch (Throwable finishFailure) {
                if (failure == null) {
                    failure = finishFailure;
                } else {
                    failure.addSuppressed(finishFailure);
                }
            }
            data.loading = false;
        }
        if (failure != null) {
            if (failure instanceof Error error) {
                throw error;
            }
            if (failure instanceof SQLException sqlFailure) {
                throw sqlFailure;
            }
            throw new SQLException("source item template publication refused", failure);
        }
        Counts counts = definitions.counts();
        if (counts.rolledBack() != 1 || counts.physicalClosed() != 1 || counts.lifecycleFailures() != 0) {
            throw new SQLException("item template SQL did not finish before publication");
        }
        int templates = freeze(data.table);
        return new Snapshot(database, data.table, data, Collections.unmodifiableMap(new LinkedHashMap<Integer, NativeDeItemTemplatesScope.Binding>(data.bindings)), counts, templates);
    }

   static int[] copyArray(int[] var0) {
      return var0 == null ? null : (int[])var0.clone();
   }

   private static NativeDeItemTemplate frozen(NativeDeItemTemplate var0, IdentityHashMap<NativeDeItemTemplate, NativeDeItemTemplate> var1) throws ReflectiveOperationException {
      if (var0 == null) {
         return null;
      } else {
         NativeDeItemTemplate var2 = (NativeDeItemTemplate)var1.get(var0);
         if (var2 != null) {
            return var2;
         } else {
            NativeDeItemTemplate var3 = var0 instanceof NativeDeWeaponTemplate
               ? new NativeDeFrozenWeaponTemplate()
               : (
                  var0 instanceof NativeDeArmorTemplate
                     ? new NativeDeFrozenArmorTemplate()
                     : (var0 instanceof NativeDeEtcItemTemplate ? new NativeDeFrozenEtcTemplate() : null)
               );
            if (var3 == null) {
               throw new IllegalArgumentException("unknown source item template role");
            } else {
               for (Class var4 = var0.getClass(); var4 != Object.class; var4 = var4.getSuperclass()) {
                  for (Field var8 : var4.getDeclaredFields()) {
                     if (!Modifier.isStatic(var8.getModifiers())) {
                        var8.setAccessible(true);
                        Object var9 = var8.get(var0);
                        if (var9 instanceof int[] var10) {
                           var9 = var10.clone();
                        } else if (var9 != null && !var8.getType().isPrimitive() && !(var9 instanceof String)) {
                           throw new IllegalArgumentException("unexpected mutable template field " + var8.getName());
                        }

                        var8.set(var3, var9);
                     }
                  }
               }

               var1.put(var0, var3);
               return (NativeDeItemTemplate)var3;
            }
         }
      }
   }

   private static int freeze(NativeDeItemTable var0) throws SQLException {
      try {
         IdentityHashMap var1 = new IdentityHashMap();

         for (String var3 : List.of("_etcitems", "_weapons", "_armors")) {
            Field var4 = var0.getClass().getDeclaredField(var3);
            var4.setAccessible(true);
            Map<?, ?> var5 = (Map<?, ?>)var4.get(var0);
            LinkedHashMap var6 = new LinkedHashMap();

            for (Entry var8 : var5.entrySet()) {
               var6.put(var8.getKey(), frozen((NativeDeItemTemplate)var8.getValue(), var1));
            }

            var4.set(var0, Collections.unmodifiableMap(var6));
         }

         Field var10 = var0.getClass().getDeclaredField("_allTemplates");
         var10.setAccessible(true);
         NativeDeItemTemplate[] var11 = (NativeDeItemTemplate[])var10.get(var0);
         NativeDeItemTemplate[] var12 = new NativeDeItemTemplate[var11.length];

         for (int var13 = 0; var13 < var11.length; var13++) {
            var12[var13] = frozen(var11[var13], var1);
         }

         var10.set(var0, var12);
         return var1.size();
      } catch (ReflectiveOperationException | RuntimeException var9) {
         throw new SQLException("source item template freeze failed; publication refused", var9);
      }
   }

   private NativeDeItemTemplatesResources() {
   }

   static final class Owner {
      private volatile NativeDeItemTemplatesResources.Snapshot current;

      NativeDeItemTemplatesResources.Snapshot snapshot() {
         return Objects.requireNonNull(this.current, "source item templates not loaded");
      }

      synchronized NativeDeItemTemplatesResources.Snapshot reload(String var1, Sql var2, NativeDeWorldLog var3) throws SQLException {
         NativeDeItemTemplatesResources.Snapshot var4 = NativeDeItemTemplatesResources.read(var1, var2, var3);
         this.current = var4;
         return var4;
      }

      synchronized NativeDeItemTemplatesResources.Snapshot reload(ServerConfig var1) throws Exception {
         Properties var2 = UnifiedConfiguration.read(var1.source());
         String var3 = SharedDatabase.identifier(var2.getProperty("database.static.database"));
         return this.reload(
            var3,
            () -> DriverManager.getConnection(
               var2.getProperty("database.jdbc.base.url") + var3 + var2.getProperty("database.jdbc.options"),
               var2.getProperty("database.user"),
               var2.getProperty("database.password")
            ),
            (var0, var1x, var2x) -> System.getLogger(NativeDeItemTemplatesResources.class.getName())
               .log(
                  var0.equals("error") ? Level.ERROR : (var0.equals("warn") ? Level.WARNING : (var0.equals("debug") ? Level.DEBUG : Level.INFO)),
                  var1x + " " + Arrays.toString(var2x)
               )
         );
      }
   }

   record Snapshot(
      String database,
      NativeDeItemTable table,
      NativeDeItemTemplatesScope.Data data,
      Map<Integer, NativeDeItemTemplatesScope.Binding> executorBindings,
      Counts counts,
      int templates
   ) {
      <T> T call(Supplier<T> var1) {
         return NativeDeItemTemplatesScope.call(this.data, var1);
      }
   }
}
