// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import local.lito381.server.NativeDeItemAbility.AbilityType;
import local.lito381.server.NativeDeItemDefinitionsScope.Data;
import local.lito381.server.NativeDeItemDefinitionsScope.Database;
import local.lito381.server.NativeDeItemDefinitionsScope.Logs;

public class NativeDeItemIdStatsTable {
   private static final NativeDeWorldLog _log = Logs.getLogger(NativeDeItemIdStatsTable.class);
   private final Map<Integer, Map<Integer, NativeDeItemAbility>> _itemIdStats;
   final Data nativeDefinitions = NativeDeItemDefinitionsScope.loadingData();

   private NativeDeItemIdStatsTable() {
      this._itemIdStats = new HashMap<>();
      this.load();
   }

   public void reload() {
      NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
      this._itemIdStats.clear();
      this.load();
   }

   private void load() {
      NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
      NativeDeTerrainTimer var1 = new NativeDeTerrainTimer();
      this.loadWeaponStats();
      this.loadArmorStats();
      this.nativeDefinitions.log.info("載入 裝備_bless_指定 總計 ok: " + this._itemIdStats.size() + " (" + var1.get() + "ms)");
   }

   private void loadWeaponStats() {
      NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
      NativeDeTerrainTimer var1 = new NativeDeTerrainTimer();
      int var2 = 0;

      try (
         Connection var3 = Database.get().getConnection();
         PreparedStatement var4 = var3.prepareStatement("SELECT * FROM `裝備_weapon_bless_指定`");
         ResultSet var5 = var4.executeQuery();
      ) {
         while (var5.next()) {
            int var6 = -1;

            try {
               var6 = var5.getInt("item_id");
               int var7 = var5.getInt("bless_id");
               NativeDeItemAbility var8 = new NativeDeItemAbility();
               this.fillCommonAbility(var8, var5);
               this.safeAdd(var8, AbilityType.BASE_DMG_SMALL, var5, "add_dmg_small");
               this.safeAdd(var8, AbilityType.BASE_DMG_LARGE, var5, "add_dmg_large");
               this.safeAdd(var8, AbilityType.PIERCE, var5, "add_pierce");
               this.safeAdd(var8, AbilityType.STUN_SUCCESS, var5, "add_stun_success");
               this.safeAdd(var8, AbilityType.IGNORE_REDUCTION, var5, "add_ignore_reduction");
               this._itemIdStats.computeIfAbsent(var6, var0 -> new HashMap<>()).put(var7, var8);
               var2++;
            } catch (Exception var12) {
               this.nativeDefinitions.log.error("載入資料失敗，已略過該筆 (item_id: " + var6 + ")", (Throwable)var12);
               continue;
            }
         }
      } catch (SQLException var16) {
         this.nativeDefinitions.log.error("載入 裝備_weapon_bless_指定 失敗! 請檢查資料表!", (Throwable)var16);
      }

      this.nativeDefinitions.log.info("載入 裝備_weapon_bless_指定 ok: " + var2 + " (" + var1.get() + "ms)");
   }

   private void loadArmorStats() {
      NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
      NativeDeTerrainTimer var1 = new NativeDeTerrainTimer();
      int var2 = 0;

      try (
         Connection var3 = Database.get().getConnection();
         PreparedStatement var4 = var3.prepareStatement("SELECT * FROM `裝備_armor_bless_指定`");
         ResultSet var5 = var4.executeQuery();
      ) {
         while (var5.next()) {
            int var6 = -1;

            try {
               var6 = var5.getInt("item_id");
               int var7 = var5.getInt("bless_id");
               NativeDeItemAbility var8 = new NativeDeItemAbility();
               this.fillCommonAbility(var8, var5);
               if (this.hasColumn(var5, "add_ac")) {
                  int var9 = var5.getInt("add_ac");
                  if (var9 != 0) {
                     var8.set(AbilityType.AC, -var9);
                  }
               }

               this.safeAdd(var8, AbilityType.MELEE_REDUX, var5, "add_melee_redux");
               this.safeAdd(var8, AbilityType.BOW_REDUX, var5, "add_bow_redux");
               this.safeAdd(var8, AbilityType.DMG_REDUX, var5, "add_reduction");
               this.safeAdd(var8, AbilityType.REGIST_FREEZE, var5, "regist_freeze");
               this.safeAdd(var8, AbilityType.REGIST_STONE, var5, "regist_stone");
               this.safeAdd(var8, AbilityType.REGIST_SLEEP, var5, "regist_sleep");
               this.safeAdd(var8, AbilityType.REGIST_BLIND, var5, "regist_blind");
               this.safeAdd(var8, AbilityType.REGIST_SUSTAIN, var5, "regist_sustain");
               this.safeAdd(var8, AbilityType.REGIST_STUN, var5, "regist_stun");
               this._itemIdStats.computeIfAbsent(var6, var0 -> new HashMap<>()).put(var7, var8);
               var2++;
            } catch (Exception var13) {
               this.nativeDefinitions.log.error("載入資料失敗，已略過該筆 (item_id: " + var6 + ")", (Throwable)var13);
               continue;
            }
         }
      } catch (SQLException var17) {
         this.nativeDefinitions.log.error("載入 裝備_armor_bless_指定 失敗! 請檢查資料表!", (Throwable)var17);
      }

      this.nativeDefinitions.log.info("載入 裝備_armor_bless_指定 ok: " + var2 + " (" + var1.get() + "ms)");
   }

   private void fillCommonAbility(NativeDeItemAbility var1, ResultSet var2) throws SQLException {
      this.safeAdd(var1, AbilityType.STR, var2, "add_str");
      this.safeAdd(var1, AbilityType.DEX, var2, "add_dex");
      this.safeAdd(var1, AbilityType.CON, var2, "add_con");
      this.safeAdd(var1, AbilityType.INT, var2, "add_int");
      this.safeAdd(var1, AbilityType.WIS, var2, "add_wis");
      this.safeAdd(var1, AbilityType.CHA, var2, "add_cha");
      this.safeAdd(var1, AbilityType.HP, var2, "add_hp");
      this.safeAdd(var1, AbilityType.MP, var2, "add_mp");
      this.safeAdd(var1, AbilityType.HP_REGEN, var2, "add_hpr");
      this.safeAdd(var1, AbilityType.MP_REGEN, var2, "add_mpr");
      this.safeAdd(var1, AbilityType.MR, var2, "add_mr");
      this.safeAdd(var1, AbilityType.SP, var2, "add_sp");
      this.safeAdd(var1, AbilityType.HIT, var2, "add_hit");
      this.safeAdd(var1, AbilityType.DMG, var2, "add_dmg");
      this.safeAdd(var1, AbilityType.BOW_HIT, var2, "add_bow_hit");
      this.safeAdd(var1, AbilityType.BOW_DMG, var2, "add_bow_dmg");
      this.safeAdd(var1, AbilityType.MAGIC_HIT, var2, "add_magic_hit");
      this.safeAdd(var1, AbilityType.MAGIC_DMG_MODIFIER, var2, "add_magic_dmg");
      this.safeAdd(var1, AbilityType.DMG_REDUX, var2, "add_reduction");
      this.safeAdd(var1, AbilityType.DODGE, var2, "add_dodge");
      this.safeAdd(var1, AbilityType.ER, var2, "add_er");
      this.safeAdd(var1, AbilityType.DEF_FIRE, var2, "add_fire");
      this.safeAdd(var1, AbilityType.DEF_WATER, var2, "add_water");
      this.safeAdd(var1, AbilityType.DEF_WIND, var2, "add_wind");
      this.safeAdd(var1, AbilityType.DEF_EARTH, var2, "add_earth");
      this.safeAdd(var1, AbilityType.PVP_DMG, var2, "add_pvp_dmg");
      this.safeAdd(var1, AbilityType.PVP_REDUX, var2, "add_pvp_redux");
      this.safeAdd(var1, AbilityType.DMG_BOSS, var2, "add_boss_dmg");
      this.safeAdd(var1, AbilityType.BOSS_REDUX, var2, "add_boss_redux");
      this.safeAdd(var1, AbilityType.PIERCE, var2, "add_pierce");
      this.safeAdd(var1, AbilityType.STUN_SUCCESS, var2, "add_stun_success");
      this.safeAdd(var1, AbilityType.IGNORE_REDUCTION, var2, "add_ignore_reduction");
      this.safeAdd(var1, AbilityType.POTION_RECOVERY, var2, "add_potion_recovery");
      this.safeAdd(var1, AbilityType.POISON_RESIST, var2, "add_poison_resist");
      this.safeAdd(var1, AbilityType.EXP_RATE, var2, "add_exp_rate");
      this.safeAdd(var1, AbilityType.DROP_RATE, var2, "add_drop_rate");
      this.safeAdd(var1, AbilityType.ADENA_RATE, var2, "add_adena_rate");
   }

   private void safeAdd(NativeDeItemAbility var1, AbilityType var2, ResultSet var3, String var4) {
      try {
         if (this.hasColumn(var3, var4)) {
            int var5 = var3.getInt(var4);
            if (var5 != 0) {
               var1.set(var2, var5);
            }
         }
      } catch (SQLException var6) {
      }
   }

   private boolean hasColumn(ResultSet var1, String var2) throws SQLException {
      try {
         var1.findColumn(var2);
         return true;
      } catch (SQLException var4) {
         return false;
      }
   }

   public NativeDeItemAbility getSpecificItemStats(int var1, int var2) {
      Map var3 = this._itemIdStats.get(var1);
      return var3 != null ? (NativeDeItemAbility)var3.get(var2) : null;
   }

   public static NativeDeItemIdStatsTable getInstance() {
      return (NativeDeItemIdStatsTable)NativeDeItemDefinitionsScope.table(NativeDeItemIdStatsTable.class);
   }
}
