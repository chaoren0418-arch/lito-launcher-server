// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Map.Entry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import local.lito381.server.NativeDeShopData;
import local.lito381.server.NativeDeShopResources;
import local.lito381.server.NativeDeTerrainScope;
import local.lito381.server.NativeDeTerrainTimer;
import local.lito381.server.NativeDeWorldLog;

public class NativeDeShopTable {
    private static final NativeDeWorldLog _log = NativeDeTerrainScope.Logs.getLogger(NativeDeShopTable.class);
    private volatile Map<Integer, List<NativeDeShopData>> _shopData = Map.of();

    private NativeDeShopTable() {
        this.load();
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public void load() {
      NativeDeTerrainTimer var1 = new NativeDeTerrainTimer();
      Map<Integer, List<NativeDeShopData>> var2 = new HashMap();
      int var3 = 0;

      try (
         Connection var4 = NativeDeTerrainScope.Database.get().getConnection();
         PreparedStatement var5 = var4.prepareStatement("SELECT mapid, x_min, x_max, y_min, y_max, currency FROM mapids_shop");
         ResultSet var6 = var5.executeQuery();
      ) {
         while (var6.next()) {
            int var7 = -1;

            try {
               var7 = var6.getInt("mapid");
               int var8 = var6.getInt("x_min");
               int var9 = var6.getInt("x_max");
               int var10 = var6.getInt("y_min");
               int var11 = var6.getInt("y_max");
               int var12 = var6.getInt("currency");
               NativeDeShopData var13 = new NativeDeShopData(var7, var8, var9, var10, var11, var12);
               var2.computeIfAbsent(var7, var0 -> new ArrayList<>()).add(var13);
               var3++;
            } catch (Exception var17) {
               _log.error("載入資料失敗，已略過該筆 (npc_id: " + var7 + ")", (Throwable)var17);
            }
         }

         Map<Integer, List<NativeDeShopData>> var22 = new HashMap();

         for (Entry var24 : var2.entrySet()) {
            var22.put((Integer)var24.getKey(), List.copyOf((Collection)var24.getValue()));
         }

         this._shopData = Map.copyOf(var22);
         _log.info("載入 mapids_shop ok: {} maps, {} ranges ({}ms)", this._shopData.size(), var3, var1.get());
      } catch (SQLException var21) {
         _log.error("載入 mapids_shop 失敗! 請檢查資料表!", (Throwable)var21);
      }
   }

    public boolean checkShop(int var1, int var2, int var3) {
      List<NativeDeShopData> var4 = this._shopData.get(var1);
      if (var4 == null) {
         return false;
      } else {
         for (NativeDeShopData var6 : var4) {
            if (var6.isInRange(var2, var3)) {
               return true;
            }
         }

         return false;
      }
   }

    public int getCurrency(int var1, int var2, int var3) {
      List<NativeDeShopData> var4 = this._shopData.get(var1);
      if (var4 == null) {
         return 0;
      } else {
         for (NativeDeShopData var6 : var4) {
            if (var6.isInRange(var2, var3)) {
               return var6.getCurrency();
            }
         }

         return 0;
      }
   }

    public static NativeDeShopTable get() {
        return NativeDeShopResources.table();
    }

    public static NativeDeShopTable create() {
        return new NativeDeShopTable();
    }
}
