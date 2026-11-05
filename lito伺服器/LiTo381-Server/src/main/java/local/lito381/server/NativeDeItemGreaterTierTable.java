// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import local.lito381.server.NativeDeItemAbility.AbilityType;
import local.lito381.server.NativeDeItemDefinitionsScope.Data;
import local.lito381.server.NativeDeItemDefinitionsScope.Database;
import local.lito381.server.NativeDeItemDefinitionsScope.Logs;

public class NativeDeItemGreaterTierTable {
   private static final NativeDeWorldLog _log = Logs.getLogger(NativeDeItemGreaterTierTable.class);
   private final Map<Integer, Map<Integer, NativeDeItemAbility>> _weaponTiers;
   private final Map<Integer, Map<Integer, NativeDeItemAbility>> _armorTiers;
   private final Map<Integer, Map<Integer, String>> _weaponTierNotes;
   private final Map<Integer, Map<Integer, String>> _armorTierNotes;
   private final Map<Integer, String> _tierNames;
   private final Map<Integer, List<NativeDeItemGreaterTierTable.TierChance>> _weaponChances;
   private final Map<Integer, List<NativeDeItemGreaterTierTable.TierChance>> _armorChances;
   private final Random _random;
   final Data nativeDefinitions = NativeDeItemDefinitionsScope.loadingData();

   private NativeDeItemGreaterTierTable() {
      this._weaponTiers = new HashMap<>();
      this._armorTiers = new HashMap<>();
      this._weaponTierNotes = new HashMap<>();
      this._armorTierNotes = new HashMap<>();
      this._tierNames = new HashMap<>();
      this._weaponChances = new HashMap<>();
      this._armorChances = new HashMap<>();
      this._random = new Random();
      this.load();
   }

   public void reload() {
      NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
      this._weaponTiers.clear();
      this._armorTiers.clear();
      this._weaponChances.clear();
      this._armorChances.clear();
      this.load();
   }

   private void load() {
      NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
      NativeDeTerrainTimer var1 = new NativeDeTerrainTimer();
      this.loadWeaponTiers();
      this.loadArmorTiers();
      this.nativeDefinitions.log.info("載入 裝備_weapon_greater_類型 ok: " + this._weaponTiers.size() + " (" + var1.get() + "ms)");
      this.nativeDefinitions.log.info("載入 裝備_armor_greater_類型 ok: " + this._armorTiers.size() + " (" + var1.get() + "ms)");
   }

   private void loadWeaponTiers() {
      NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);

      try (
         Connection var1 = Database.get().getConnection();
         PreparedStatement var2 = var1.prepareStatement("SELECT * FROM `裝備_weapon_greater_類型`");
         ResultSet var3 = var2.executeQuery();
      ) {
         while (var3.next()) {
            int var4 = -1;

            try {
               var4 = var3.getInt("tier_id");
               int var5 = var3.getInt("weapon_type");
               NativeDeItemAbility var6 = new NativeDeItemAbility();
               this.fillCommonAbility(var6, var3);
               this.safeAdd(var6, AbilityType.BASE_DMG_SMALL, var3, "add_dmg_small");
               this.safeAdd(var6, AbilityType.BASE_DMG_LARGE, var3, "add_dmg_large");
               this._weaponTiers.computeIfAbsent(var4, var0 -> new HashMap<>()).put(var5, var6);
               String var7 = this.readOptionalString(var3, "note");
               if (var7 != null && !var7.isEmpty()) {
                  this._weaponTierNotes.computeIfAbsent(var4, var0 -> new HashMap<>()).put(var5, var7);
               }

               int var8 = this.readOptionalInt(var3, "chance");
               if (var8 > 0) {
                  this._weaponChances.computeIfAbsent(var5, var0 -> new ArrayList<>()).add(new NativeDeItemGreaterTierTable.TierChance(var4, var8));
               }
            } catch (Exception var12) {
               this.nativeDefinitions.log.error("載入資料失敗，已略過該筆 (tier_id: " + var4 + ")", (Throwable)var12);
               continue;
            }
         }
      } catch (SQLException var16) {
         this.nativeDefinitions.log.error("載入 裝備_weapon_greater_類型 失敗! 請檢查資料表!", (Throwable)var16);
      }
   }

   private void loadArmorTiers() {
      NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);

      try (
         Connection var1 = Database.get().getConnection();
         PreparedStatement var2 = var1.prepareStatement("SELECT * FROM `裝備_armor_greater_類型`");
         ResultSet var3 = var2.executeQuery();
      ) {
         while (var3.next()) {
            int var4 = -1;

            try {
               var4 = var3.getInt("tier_id");
               int var5 = var3.getInt("armor_type");
               NativeDeItemAbility var6 = new NativeDeItemAbility();
               this.fillCommonAbility(var6, var3);
               if (this.hasColumn(var3, "add_ac")) {
                  int var7 = var3.getInt("add_ac");
                  if (var7 != 0) {
                     var6.set(AbilityType.AC, -var7);
                  }
               }

               this.safeAdd(var6, AbilityType.MELEE_REDUX, var3, "add_melee_redux");
               this.safeAdd(var6, AbilityType.BOW_REDUX, var3, "add_bow_redux");
               this.safeAdd(var6, AbilityType.REGIST_FREEZE, var3, "regist_freeze");
               this.safeAdd(var6, AbilityType.REGIST_STONE, var3, "regist_stone");
               this.safeAdd(var6, AbilityType.REGIST_SLEEP, var3, "regist_sleep");
               this.safeAdd(var6, AbilityType.REGIST_BLIND, var3, "regist_blind");
               this.safeAdd(var6, AbilityType.REGIST_SUSTAIN, var3, "regist_sustain");
               this._armorTiers.computeIfAbsent(var4, var0 -> new HashMap<>()).put(var5, var6);
               String var17 = this.readOptionalString(var3, "note");
               if (var17 != null && !var17.isEmpty()) {
                  this._armorTierNotes.computeIfAbsent(var4, var0 -> new HashMap<>()).put(var5, var17);
               }

               int var8 = this.readOptionalInt(var3, "chance");
               if (var8 > 0) {
                  this._armorChances.computeIfAbsent(var5, var0 -> new ArrayList<>()).add(new NativeDeItemGreaterTierTable.TierChance(var4, var8));
               }
            } catch (Exception var12) {
               this.nativeDefinitions.log.error("載入資料失敗，已略過該筆 (tier_id: " + var4 + ")", (Throwable)var12);
               continue;
            }
         }
      } catch (SQLException var16) {
         this.nativeDefinitions.log.error("載入 裝備_armor_greater_類型 失敗! 請檢查資料表!", (Throwable)var16);
      }
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
      this.safeAdd(var1, AbilityType.IGNORE_REDUCTION, var2, "add_ignore_reduction");
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
      this.safeAdd(var1, AbilityType.REGIST_STUN, var2, "regist_stun");
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

   private String readOptionalString(ResultSet var1, String var2) {
      try {
         return this.hasColumn(var1, var2) ? var1.getString(var2) : null;
      } catch (SQLException var4) {
         this.nativeDefinitions.log.debug("Optional item greater tier column read failed: {}", var2, var4);
         return null;
      }
   }

   private int readOptionalInt(ResultSet var1, String var2) {
      try {
         return this.hasColumn(var1, var2) ? var1.getInt(var2) : 0;
      } catch (SQLException var4) {
         this.nativeDefinitions.log.debug("Optional item greater tier column read failed: {}", var2, var4);
         return 0;
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

   public NativeDeItemAbility getWeaponTier(int var1, int var2) {
      Map var3 = this._weaponTiers.get(var1);
      return var3 != null ? (NativeDeItemAbility)var3.get(var2) : null;
   }

   public NativeDeItemAbility getArmorTier(int var1, int var2) {
      Map var3 = this._armorTiers.get(var1);
      return var3 != null ? (NativeDeItemAbility)var3.get(var2) : null;
   }

   public String getTierName(int var1) {
      return this._tierNames.getOrDefault(var1, this.getDefaultTierName(var1));
   }

   private String getDefaultTierName(int var1) {
      switch (var1) {
         case 0:
            return "一般";
         case 1:
            return "普通";
         case 2:
            return "精良";
         case 3:
            return "史詩";
         case 4:
            return "傳說";
         case 5:
            return "神話";
         default:
            return var1 >= 5 ? "神話" : "階級 " + var1;
      }
   }

   public int getRandomWeaponTier(int var1) {
      List var2 = this._weaponChances.get(var1);
      return this.pickRandomTier(var2);
   }

   public int getRandomArmorTier(int var1) {
      List var2 = this._armorChances.get(var1);
      return this.pickRandomTier(var2);
   }

   private int pickRandomTier(List<NativeDeItemGreaterTierTable.TierChance> var1) {
      if (var1 == null || var1.isEmpty()) { return 0; }
         int var2 = 0;

         for (NativeDeItemGreaterTierTable.TierChance var4 : var1) {
            var2 += var4.chance;
         }

         if (var2 <= 0) {
            return 0;
         } else {
            int var7 = 100;
            if (var2 > 100) {
               var7 = var2;
            }

            int var8 = this._random.nextInt(var7);

            for (NativeDeItemGreaterTierTable.TierChance var6 : var1) {
               var8 -= var6.chance;
               if (var8 < 0) {
                  return var6.tierId;
               }
            }

            return 0;
         }

   }

   public String getTierNote(int var1, int var2, boolean var3) {
      String var4 = null;
      if (var3) {
         Map var5 = this._weaponTierNotes.get(var1);
         if (var5 != null) {
            var4 = (String)var5.get(var2);
         }
      } else {
         Map var6 = this._armorTierNotes.get(var1);
         if (var6 != null) {
            var4 = (String)var6.get(var2);
         }
      }

      return var4 == null ? this.getDefaultTierName(var1) : var4;
   }

   public static NativeDeItemGreaterTierTable getInstance() {
      return (NativeDeItemGreaterTierTable)NativeDeItemDefinitionsScope.table(NativeDeItemGreaterTierTable.class);
   }

   private static class TierChance {
      final int tierId;
      final int chance;

      TierChance(int var1, int var2) {
         this.tierId = var1;
         this.chance = var2;
      }
   }
}
