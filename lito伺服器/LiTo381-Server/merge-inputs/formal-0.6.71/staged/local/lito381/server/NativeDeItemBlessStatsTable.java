// DECOMPILED from base-0.6.71.jar, TODO review
// Decompiled with Vineflower 1.11.2; reviewed against formal bytecode.
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class NativeDeItemBlessStatsTable {
   private static final NativeDeWorldLog _log = NativeDeItemDefinitionsScope.Logs.getLogger(NativeDeItemBlessStatsTable.class);
   private final Map<Integer, Map<Integer, NativeDeItemAbility>> _weaponBlessStats;
   private final Map<Integer, Map<Integer, NativeDeItemAbility>> _armorBlessStats;
   final NativeDeItemDefinitionsScope.Data nativeDefinitions = NativeDeItemDefinitionsScope.loadingData();

   private NativeDeItemBlessStatsTable() {
      this._weaponBlessStats = new HashMap<>();
      this._armorBlessStats = new HashMap<>();
      this.load();
   }

   public void reload() {
      NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
      this._weaponBlessStats.clear();
      this._armorBlessStats.clear();
      this.load();
   }

   private void load() {
      NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
      NativeDeTerrainTimer var1 = new NativeDeTerrainTimer();
      this.loadWeaponBlessStats();
      this.loadArmorBlessStats();
      this.nativeDefinitions.log.info("載入 裝備_weapon_bless_類型 ok: " + this._weaponBlessStats.size() + " (" + var1.get() + "ms)");
      this.nativeDefinitions.log.info("載入 裝備_armor_bless_類型 ok: " + this._armorBlessStats.size() + " (" + var1.get() + "ms)");
   }

   private void loadWeaponBlessStats() {
      NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);

      try (
         Connection var1 = NativeDeItemDefinitionsScope.Database.get().getConnection();
         PreparedStatement var2 = var1.prepareStatement("SELECT * FROM `裝備_weapon_bless_類型`");
         ResultSet var3 = var2.executeQuery();
      ) {
         while (var3.next()) {
            int var4 = -1;

            try {
               var4 = var3.getInt("bless_id");
               int var5 = var3.getInt("weapon_type");
               NativeDeItemAbility var6 = new NativeDeItemAbility();
               this.fillCommonAbility(var6, var3);
               this.safeAdd(var6, NativeDeItemAbility.AbilityType.BASE_DMG_SMALL, var3, "add_dmg_small");
               this.safeAdd(var6, NativeDeItemAbility.AbilityType.BASE_DMG_LARGE, var3, "add_dmg_large");
               this._weaponBlessStats.computeIfAbsent(var4, var0 -> new HashMap<>()).put(var5, var6);
            } catch (Exception var10) {
               this.nativeDefinitions.log.error("載入資料失敗，已略過該筆 (bless_id: " + var4 + ")", (Throwable)var10);
            }
         }
      } catch (SQLException var14) {
         this.nativeDefinitions.log.error("載入 裝備_weapon_bless_類型 失敗! 請檢查資料表!", (Throwable)var14);
      }
   }

   private void loadArmorBlessStats() {
      NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);

      try (
         Connection var1 = NativeDeItemDefinitionsScope.Database.get().getConnection();
         PreparedStatement var2 = var1.prepareStatement("SELECT * FROM `裝備_armor_bless_類型`");
         ResultSet var3 = var2.executeQuery();
      ) {
         while (var3.next()) {
            int var4 = -1;

            try {
               var4 = var3.getInt("bless_id");
               int var5 = var3.getInt("armor_type");
               NativeDeItemAbility var6 = new NativeDeItemAbility();
               this.fillCommonAbility(var6, var3);
               if (this.hasColumn(var3, "add_ac")) {
                  int var7 = var3.getInt("add_ac");
                  if (var7 != 0) {
                     var6.set(NativeDeItemAbility.AbilityType.AC, -var7);
                  }
               }

               this.safeAdd(var6, NativeDeItemAbility.AbilityType.MELEE_REDUX, var3, "add_melee_redux");
               this.safeAdd(var6, NativeDeItemAbility.AbilityType.BOW_REDUX, var3, "add_bow_redux");
               this.safeAdd(var6, NativeDeItemAbility.AbilityType.REGIST_FREEZE, var3, "regist_freeze");
               this.safeAdd(var6, NativeDeItemAbility.AbilityType.REGIST_STONE, var3, "regist_stone");
               this.safeAdd(var6, NativeDeItemAbility.AbilityType.REGIST_SLEEP, var3, "regist_sleep");
               this.safeAdd(var6, NativeDeItemAbility.AbilityType.REGIST_BLIND, var3, "regist_blind");
               this.safeAdd(var6, NativeDeItemAbility.AbilityType.REGIST_SUSTAIN, var3, "regist_sustain");
               this._armorBlessStats.computeIfAbsent(var4, var0 -> new HashMap<>()).put(var5, var6);
            } catch (Exception var11) {
               this.nativeDefinitions.log.error("載入資料失敗，已略過該筆 (bless_id: " + var4 + ")", (Throwable)var11);
            }
         }
      } catch (SQLException var15) {
         this.nativeDefinitions.log.error("載入 裝備_armor_bless_類型 失敗! 請檢查資料表!", (Throwable)var15);
      }
   }

   private void fillCommonAbility(NativeDeItemAbility var1, ResultSet var2) throws SQLException {
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.STR, var2, "add_str");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.DEX, var2, "add_dex");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.CON, var2, "add_con");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.INT, var2, "add_int");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.WIS, var2, "add_wis");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.CHA, var2, "add_cha");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.HP, var2, "add_hp");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.MP, var2, "add_mp");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.HP_REGEN, var2, "add_hpr");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.MP_REGEN, var2, "add_mpr");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.MR, var2, "add_mr");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.SP, var2, "add_sp");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.HIT, var2, "add_hit");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.DMG, var2, "add_dmg");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.BOW_HIT, var2, "add_bow_hit");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.BOW_DMG, var2, "add_bow_dmg");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.MAGIC_HIT, var2, "add_magic_hit");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.MAGIC_DMG_MODIFIER, var2, "add_magic_dmg");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.DMG_REDUX, var2, "add_reduction");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.IGNORE_REDUCTION, var2, "add_ignore_reduction");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.DODGE, var2, "add_dodge");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.ER, var2, "add_er");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.DEF_FIRE, var2, "add_fire");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.DEF_WATER, var2, "add_water");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.DEF_WIND, var2, "add_wind");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.DEF_EARTH, var2, "add_earth");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.PVP_DMG, var2, "add_pvp_dmg");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.PVP_REDUX, var2, "add_pvp_redux");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.DMG_BOSS, var2, "add_boss_dmg");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.BOSS_REDUX, var2, "add_boss_redux");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.PIERCE, var2, "add_pierce");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.STUN_SUCCESS, var2, "add_stun_success");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.REGIST_STUN, var2, "regist_stun");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.POTION_RECOVERY, var2, "add_potion_recovery");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.POISON_RESIST, var2, "add_poison_resist");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.EXP_RATE, var2, "add_exp_rate");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.DROP_RATE, var2, "add_drop_rate");
      this.safeAdd(var1, NativeDeItemAbility.AbilityType.ADENA_RATE, var2, "add_adena_rate");
   }

   private void safeAdd(NativeDeItemAbility var1, NativeDeItemAbility.AbilityType var2, ResultSet var3, String var4) {
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

   public NativeDeItemAbility getWeaponBlessStats(int var1, int var2) {
      Map var3 = this._weaponBlessStats.get(var1);
      return var3 != null ? (NativeDeItemAbility)var3.get(var2) : null;
   }

   public NativeDeItemAbility getArmorBlessStats(int var1, int var2) {
      Map var3 = this._armorBlessStats.get(var1);
      return var3 != null ? (NativeDeItemAbility)var3.get(var2) : null;
   }

   public static NativeDeItemBlessStatsTable getInstance() {
      return (NativeDeItemBlessStatsTable)NativeDeItemDefinitionsScope.table(NativeDeItemBlessStatsTable.class);
   }
}
