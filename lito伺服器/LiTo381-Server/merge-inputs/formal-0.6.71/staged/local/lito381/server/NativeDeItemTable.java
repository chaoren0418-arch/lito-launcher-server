// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@AsmFinal({"_armorTypes", "_weaponTypes", "_weaponId", "_materialTypes", "_etcItemTypes", "_useTypes"})
public class NativeDeItemTable {
   private static final NativeDeWorldLog _log = NativeDeItemTemplatesScope.Logs.getLogger(NativeDeItemTable.class);
   private static Map<String, Integer> _armorTypes = new HashMap<>();
   private static Map<String, Integer> _weaponTypes = new HashMap<>();
   private static Map<String, Integer> _weaponId = new HashMap<>();
   private static Map<String, Integer> _materialTypes = new HashMap<>();
   private static Map<String, Integer> _etcItemTypes = new HashMap<>();
   private static Map<String, Integer> _useTypes = new HashMap<>();
   private NativeDeItemTemplate[] _allTemplates;
   private Map<Integer, NativeDeEtcItemTemplate> _etcitems;
   private Map<Integer, NativeDeArmorTemplate> _armors;
   private Map<Integer, NativeDeWeaponTemplate> _weapons;
   final NativeDeItemTemplatesScope.Data nativeTemplates = NativeDeItemTemplatesScope.loadingData();

   public void load() {
      NativeDeItemTemplatesScope.requireLoading(this.nativeTemplates);
      NativeDeTerrainTimer var1 = new NativeDeTerrainTimer();
      this._etcitems = this.allEtcItem();
      this._weapons = this.allWeapon();
      this._armors = this.allArmor();
      this.buildFastLookupTable();
      this.nativeTemplates.log.info("載入 etcitem ok: " + this._etcitems.size() + " (" + var1.get() + "ms)");
      this.nativeTemplates.log.info("載入 weapon ok: " + this._weapons.size() + " (" + var1.get() + "ms)");
      this.nativeTemplates.log.info("載入 armor ok: " + this._armors.size() + " (" + var1.get() + "ms)");
   }

   public void reloadArmor() {
      NativeDeItemTemplatesScope.requireLoading(this.nativeTemplates);
      this._armors = this.allArmor();
      this.buildFastLookupTable();
      this.nativeTemplates.log.info("熱重載 armor: " + this._armors.size());
   }

   public void reloadWeapon() {
      NativeDeItemTemplatesScope.requireLoading(this.nativeTemplates);
      this._weapons = this.allWeapon();
      this.buildFastLookupTable();
      this.nativeTemplates.log.info("熱重載 weapon: " + this._weapons.size());
   }

   public void reloadEtcitem() {
      NativeDeItemTemplatesScope.requireLoading(this.nativeTemplates);
      this._etcitems = this.allEtcItem();
      this.buildFastLookupTable();
      this.nativeTemplates.log.info("熱重載 etcitem: " + this._etcitems.size());
   }

   private Map<Integer, NativeDeEtcItemTemplate> allEtcItem() {
      NativeDeItemTemplatesScope.requireLoading(this.nativeTemplates);
      Map var1 = new HashMap();

      try (
         Connection var2 = NativeDeItemTemplatesScope.Database.get().getConnection();
         PreparedStatement var3 = var2.prepareStatement(
            "SELECT e.*, a.item_id AS arrow_config_id, a.consume_on_attack AS arrow_consume_on_attack, a.dmg_small AS arrow_dmg_small, a.dmg_large AS arrow_dmg_large, a.arrow_spr AS arrow_config_spr FROM `etcitem` e LEFT JOIN `etcitem_arrow` a ON a.item_id = e.item_id"
         );
         ResultSet var4 = var3.executeQuery();
      ) {
         while (var4.next()) {
            int var5 = -1;

            try {
               var5 = var4.getInt("item_id");
               NativeDeEtcItemTemplate var6 = new NativeDeEtcItemTemplate();
               var6.setItemId(var5);
               var6.setName(var4.getString("name"));
               String var7 = var4.getString("classname");
               var6.setClassname(var7);
               var6.setNameId(var4.getString("name_id"));
               var6.setType(_etcItemTypes.get(var4.getString("item_type")));
               var6.setUseType(_useTypes.get(var4.getString("use_type")));
               var6.setType2(0);
               var6.setMaterial(_materialTypes.get(var4.getString("material")));
               var6.setWeight(var4.getInt("weight"));
               var6.setGfxId(var4.getInt("invgfx"));
               var6.setGroundGfxId(var4.getInt("grdgfx"));
               var6.setItemDescId(var4.getInt("itemdesc_id"));
               var6.setMinLevel(var4.getInt("min_lvl"));
               var6.setMaxLevel(var4.getInt("max_lvl"));
               var6.setBless(var4.getInt("bless"));
               var6.setTradable(var4.getInt("trade") == 0);
               var6.setCantDelete(var4.getInt("cant_delete") == 1);
               var6.set_stackable(var4.getInt("stackable") == 1);
               var6.setMaxChargeCount(var4.getInt("max_charge_count"));
               var6.set_delayid(var4.getInt("delay_id"));
               var6.set_delaytime(var4.getInt("delay_time"));
               var6.set_delayEffect(var4.getInt("delay_effect"));
               var6.setFoodVolume(var4.getInt("food_volume"));
               var6.setToBeSavedAtOnce(var4.getInt("save_at_once") == 1);
               var6.setLimitClass(var4.getInt("limit_class"));
               if (!this.applyArrowConfig(var4, var6)) { continue; }
                  NativeDeItemTemplatesScope.Classes.get().addList(var5, var7, 0);
                  var1.put(var6.getItemId(), var6);

            } catch (Exception var11) {
               this.nativeTemplates.log.error("載入物品失敗，已跳過該物品 (item_id: " + var5 + ")", (Throwable)var11);
               continue;
            }
         }
      } catch (SQLException var15) {
         this.nativeTemplates.log.error("載入 etcitem 失敗! 請檢查資料表!", (Throwable)var15);
      }

      return var1;
   }

   private boolean applyArrowConfig(ResultSet var1, NativeDeEtcItemTemplate var2) throws SQLException {
      int var3 = var1.getInt("arrow_config_id");
      boolean var4 = !var1.wasNull();
      int var5 = var2.getUseType();
      boolean var6 = var5 == -2 || var5 == -3;
      if (!var6) {
         if (var4) {
            this.nativeTemplates.log.warn("忽略非箭矢道具的 etcitem_arrow 設定 itemId={} useType={}", var2.getItemId(), var5);
         }

         return true;
      } else if (var4 && var3 == var2.getItemId()) {
         int var7 = var1.getInt("arrow_consume_on_attack");
         int var8 = var1.getInt("arrow_dmg_small");
         int var9 = var1.getInt("arrow_dmg_large");
         int var10 = var1.getInt("arrow_config_spr");
         if ((var7 == 0 || var7 == 1) && var8 >= 0 && var9 >= 0 && var10 >= -1 && var10 <= 65535) {
            var2.setConsumeOnAttack(var7 == 1);
            var2.setDmgSmall(var8);
            var2.setDmgLarge(var9);
            var2.setArrowSpr(var10);
            return true;
         } else {
            this.nativeTemplates.log.error("etcitem_arrow 設定不合法，已略過 itemId={} consume={} small={} large={} spr={}", var2.getItemId(), var7, var8, var9, var10);
            return false;
         }
      } else {
         this.nativeTemplates.log.error("箭矢／飛刀缺少 etcitem_arrow 設定，已略過 itemId={} name={}", var2.getItemId(), var2.getName());
         return false;
      }
   }

   private Map<Integer, NativeDeWeaponTemplate> allWeapon() {
      NativeDeItemTemplatesScope.requireLoading(this.nativeTemplates);
      Map var1 = new HashMap();

      try (
         Connection var2 = NativeDeItemTemplatesScope.Database.get().getConnection();
         PreparedStatement var3 = var2.prepareStatement("SELECT * FROM `weapon`");
         ResultSet var4 = var3.executeQuery();
      ) {
         while (var4.next()) {
            int var5 = -1;

            try {
               var5 = var4.getInt("item_id");
               NativeDeWeaponTemplate var6 = new NativeDeWeaponTemplate();
               var6.setItemId(var5);
               var6.setName(var4.getString("name"));
               String var7 = var4.getString("classname");
               var6.setClassname(var7);
               var6.setNameId(var4.getString("name_id"));
               var6.setType(_weaponTypes.get(var4.getString("type")));
               var6.setType1(_weaponId.get(var4.getString("type")));
               var6.setType2(1);
               var6.setUseType(1);
               var6.setMaterial(_materialTypes.get(var4.getString("material")));
               var6.setWeight(var4.getInt("weight"));
               var6.setGfxId(var4.getInt("invgfx"));
               var6.setGroundGfxId(var4.getInt("grdgfx"));
               var6.setItemDescId(var4.getInt("itemdesc_id"));
               var6.setDmgSmall(var4.getInt("dmg_small"));
               var6.setDmgLarge(var4.getInt("dmg_large"));
               var6.setRange(var4.getInt("range"));
               var6.set_safeenchant(var4.getInt("safenchant"));
               var6.setUseRoyal(var4.getInt("use_royal") == 0 ? false : true);
               var6.setUseKnight(var4.getInt("use_knight") == 0 ? false : true);
               var6.setUseElf(var4.getInt("use_elf") == 0 ? false : true);
               var6.setUseMage(var4.getInt("use_mage") == 0 ? false : true);
               var6.setUseDarkelf(var4.getInt("use_darkelf") == 0 ? false : true);
               var6.setUseDragonknight(var4.getInt("use_dragonknight") == 0 ? false : true);
               var6.setUseIllusionist(var4.getInt("use_illusionist") == 0 ? false : true);
               var6.setHitModifier(var4.getInt("hitmodifier"));
               var6.setDmgModifier(var4.getInt("dmgmodifier"));
               var6.set_addstr(var4.getByte("add_str"));
               var6.set_adddex(var4.getByte("add_dex"));
               var6.set_addcon(var4.getByte("add_con"));
               var6.set_addint(var4.getByte("add_int"));
               var6.set_addwis(var4.getByte("add_wis"));
               var6.set_addcha(var4.getByte("add_cha"));
               var6.set_addhp(var4.getInt("add_hp"));
               var6.set_addmp(var4.getInt("add_mp"));
               var6.set_addhpr(var4.getInt("add_hpr"));
               var6.set_addmpr(var4.getInt("add_mpr"));
               var6.set_addsp(var4.getInt("add_sp"));
               var6.set_mdef(var4.getInt("m_def"));
               var6.setDmgCriChance(var4.getInt("dmg_cri_chance"));
               var6.setDmgCri(parseDmgCri(var4, "dmg_cri"));
               var6.setDmgMaxChance(var4.getInt("dmg_max_chance"));
               var6.setDmgCriEffectEnabled(var4.getInt("dmg_cri_effect") == 1);
               var6.setDmgMaxEffectEnabled(var4.getInt("dmg_max_effect") == 1);
               var6.setMagicDmgModifier(var4.getInt("magicdmgmodifier"));
               var6.set_canbedmg(var4.getInt("canbedmg"));
               var6.setMinLevel(var4.getInt("min_lvl"));
               var6.setMaxLevel(var4.getInt("max_lvl"));
               var6.setBless(var4.getInt("bless"));
               var6.setTradable(var4.getInt("trade") == 0);
               var6.setCantDelete(var4.getInt("cant_delete") == 1);
               var6.setHasteItem(var4.getInt("haste_item") == 0 ? false : true);
               var6.setMaxUseTime(var4.getInt("max_use_time"));
               var6.setAddPvpDmg(var4.getInt("add_pvp_dmg"));
               var6.setAddIgnoreReduction(var4.getInt("add_ignore_reduction"));
               var6.setAddPvpRedux(var4.getInt("add_pvp_redux"));
               var6.setAddBossDmg(var4.getInt("add_boss_dmg"));
               var6.setAddBossRedux(var4.getInt("add_boss_redux"));
               var6.setAddDodge(var4.getInt("add_dodge"));
               var6.setAddEr(var4.getInt("add_er"));
               var6.setAddHitup(var4.getInt("add_hitup"));
               var6.setAddDmgup(var4.getInt("add_dmgup"));
               var6.setAddBowHitup(var4.getInt("add_bow_hitup"));
               var6.setAddBowDmgup(var4.getInt("add_bow_dmgup"));
               var6.setAddAdenaRate(var4.getInt("add_adena_rate"));
               var6.setAddDropRate(var4.getInt("add_drop_rate"));
               var6.setAddMeleeRedux(var4.getInt("add_melee_redux"));
               var6.setAddBowRedux(var4.getInt("add_bow_redux"));
               var6.setAddStunSuccess(var4.getInt("add_stun_success"));
               var6.setAddPotionRecovery(var4.getInt("add_potion_recovery"));
               var6.setAddPoisonResist(var4.getInt("add_poison_resist"));
               var6.setAddExpRate(var4.getInt("add_exp_rate"));
               var6.setAddPierce(var4.getInt("add_pierce"));
               var6.setMagicBow(var4.getInt("magic_bow"));
               var6.setArrowSpr(var4.getInt("arrow_spr"));
               var6.setAddMagicHit(var4.getInt("add_magic_hit"));

               try {
                  String var8 = var4.getString("set_id");
                  var6.setSetIds(var8);
               } catch (SQLException var13) {
                  var6.setSetIds(null);
               }

               try {
                  var6.setGreaterTier(var4.getInt("greater_tier"));
               } catch (SQLException var12) {
                  var6.setGreaterTier(0);
               }

               NativeDeItemTemplatesScope.Classes.get().addList(var5, var7, 1);
               var1.put(var6.getItemId(), var6);
            } catch (Exception var14) {
               this.nativeTemplates.log.error("載入物品失敗，已跳過該物品 (item_id: " + var5 + ")", (Throwable)var14);
               continue;
            }
         }
      } catch (SQLException var18) {
         this.nativeTemplates.log.error("載入 weapon 失敗! 請檢查資料表!", (Throwable)var18);
      }

      return var1;
   }

   private static double parseDmgCri(ResultSet var0, String var1) throws SQLException {
      double var2 = var0.getDouble(var1);
      return var2 >= 10.0 ? var2 / 100.0 : var2;
   }

   private Map<Integer, NativeDeArmorTemplate> allArmor() {
      NativeDeItemTemplatesScope.requireLoading(this.nativeTemplates);
      Map var1 = new HashMap();

      try (
         Connection var2 = NativeDeItemTemplatesScope.Database.get().getConnection();
         PreparedStatement var3 = var2.prepareStatement("SELECT * FROM `armor`");
         ResultSet var4 = var3.executeQuery();
      ) {
         while (var4.next()) {
            int var5 = -1;

            try {
               var5 = var4.getInt("item_id");
               NativeDeArmorTemplate var6 = new NativeDeArmorTemplate();
               var6.setItemId(var5);
               var6.setName(var4.getString("name"));
               String var7 = var4.getString("classname");
               var6.setClassname(var7);
               var6.setNameId(var4.getString("name_id"));
               var6.setType(_armorTypes.get(var4.getString("type")));
               var6.setType2(2);
               var6.setUseType(_useTypes.get(var4.getString("type")));
               var6.setMaterial(_materialTypes.get(var4.getString("material")));
               var6.setWeight(var4.getInt("weight"));
               var6.setGfxId(var4.getInt("invgfx"));
               var6.setGroundGfxId(var4.getInt("grdgfx"));
               var6.setItemDescId(var4.getInt("itemdesc_id"));
               var6.set_ac(var4.getInt("ac"));
               var6.set_safeenchant(var4.getInt("safenchant"));
               var6.setUseRoyal(var4.getInt("use_royal") == 0 ? false : true);
               var6.setUseKnight(var4.getInt("use_knight") == 0 ? false : true);
               var6.setUseElf(var4.getInt("use_elf") == 0 ? false : true);
               var6.setUseMage(var4.getInt("use_mage") == 0 ? false : true);
               var6.setUseDarkelf(var4.getInt("use_darkelf") == 0 ? false : true);
               var6.setUseDragonknight(var4.getInt("use_dragonknight") == 0 ? false : true);
               var6.setUseIllusionist(var4.getInt("use_illusionist") == 0 ? false : true);
               var6.set_addstr(var4.getByte("add_str"));
               var6.set_addcon(var4.getByte("add_con"));
               var6.set_adddex(var4.getByte("add_dex"));
               var6.set_addint(var4.getByte("add_int"));
               var6.set_addwis(var4.getByte("add_wis"));
               var6.set_addcha(var4.getByte("add_cha"));
               var6.set_addhp(var4.getInt("add_hp"));
               var6.set_addmp(var4.getInt("add_mp"));
               var6.set_addhpr(var4.getInt("add_hpr"));
               var6.set_addmpr(var4.getInt("add_mpr"));
               var6.set_addsp(var4.getInt("add_sp"));
               var6.setMinLevel(var4.getInt("min_lvl"));
               var6.setMaxLevel(var4.getInt("max_lvl"));
               var6.set_mdef(var4.getInt("m_def"));
               var6.setDamageReduction(var4.getInt("damage_reduction"));
               var6.setWeightReduction(var4.getInt("weight_reduction"));
               var6.setHitModifierByArmor(var4.getInt("hit_modifier"));
               var6.setDmgModifierByArmor(var4.getInt("dmg_modifier"));
               var6.setBowHitModifierByArmor(var4.getInt("bow_hit_modifier"));
               var6.setBowDmgModifierByArmor(var4.getInt("bow_dmg_modifier"));
               var6.setHasteItem(var4.getInt("haste_item") == 0 ? false : true);
               var6.setBless(var4.getInt("bless"));
               var6.setTradable(var4.getInt("trade") == 0);
               var6.setCantDelete(var4.getInt("cant_delete") == 1);
               var6.set_defense_earth(var4.getInt("defense_earth"));
               var6.set_defense_water(var4.getInt("defense_water"));
               var6.set_defense_wind(var4.getInt("defense_wind"));
               var6.set_defense_fire(var4.getInt("defense_fire"));
               var6.set_regist_stun(var4.getInt("regist_stun"));
               var6.set_regist_stone(var4.getInt("regist_stone"));
               var6.set_regist_sleep(var4.getInt("regist_sleep"));
               var6.set_regist_freeze(var4.getInt("regist_freeze"));
               var6.set_regist_sustain(var4.getInt("regist_sustain"));
               var6.set_regist_blind(var4.getInt("regist_blind"));
               var6.setMaxUseTime(var4.getInt("max_use_time"));
               var6.setActivity(var4.getBoolean("is_activity"));
               var6.setAddPvpRedux(var4.getInt("add_pvp_redux"));
               var6.setAddIgnoreReduction(var4.getInt("add_ignore_reduction"));
               var6.setAddPvpDmg(var4.getInt("add_pvp_dmg"));
               var6.setAddBossDmg(var4.getInt("add_boss_dmg"));
               var6.setAddBossRedux(var4.getInt("add_boss_redux"));
               var6.setAddDodge(var4.getInt("add_dodge"));
               var6.setAddEr(var4.getInt("add_er"));
               var6.setAddHitup(var4.getInt("add_hitup"));
               var6.setAddDmgup(var4.getInt("add_dmgup"));
               var6.setAddBowHitup(var4.getInt("add_bow_hitup"));
               var6.setAddBowDmgup(var4.getInt("add_bow_dmgup"));
               var6.setAddAdenaRate(var4.getInt("add_adena_rate"));
               var6.setAddDropRate(var4.getInt("add_drop_rate"));
               var6.setAddMeleeRedux(var4.getInt("add_melee_redux"));
               var6.setAddBowRedux(var4.getInt("add_bow_redux"));
               var6.setAddStunSuccess(var4.getInt("add_stun_success"));
               var6.setMagicDmgModifier(var4.getInt("magicdmgmodifier"));
               var6.setAddPotionRecovery(var4.getInt("add_potion_recovery"));
               var6.setAddPoisonResist(var4.getInt("add_poison_resist"));
               var6.setAddExpRate(var4.getInt("add_exp_rate"));
               var6.setAddMagicHit(var4.getInt("add_magic_hit"));

               try {
                  String var8 = var4.getString("set_id");
                  var6.setSetIds(var8);
               } catch (SQLException var13) {
                  var6.setSetIds(null);
               }

               try {
                  var6.setGreaterTier(var4.getInt("greater_tier"));
               } catch (SQLException var12) {
                  var6.setGreaterTier(0);
               }

               NativeDeItemTemplatesScope.Classes.get().addList(var5, var7, 2);
               var1.put(var6.getItemId(), var6);
            } catch (Exception var14) {
               this.nativeTemplates.log.error("載入物品失敗，已跳過該物品 (item_id: " + var5 + ")", (Throwable)var14);
               continue;
            }
         }
      } catch (SQLException var18) {
         this.nativeTemplates.log.error("載入 armor 失敗! 請檢查資料表!", (Throwable)var18);
      }

      return var1;
   }

   private void buildFastLookupTable() {
      NativeDeItemTemplatesScope.requireLoading(this.nativeTemplates);
      int var1 = 0;

      for (NativeDeEtcItemTemplate var4 : this._etcitems.values()) {
         if (var4.getItemId() > var1) {
            var1 = var4.getItemId();
         }
      }

      for (NativeDeWeaponTemplate var5 : this._weapons.values()) {
         if (var5.getItemId() > var1) {
            var1 = var5.getItemId();
         }
      }

      for (NativeDeArmorTemplate var6 : this._armors.values()) {
         if (var6.getItemId() > var1) {
            var1 = var6.getItemId();
         }
      }

      this._allTemplates = new NativeDeItemTemplate[var1 + 1];

      for (Integer var15 : this._etcitems.keySet()) {
         NativeDeEtcItemTemplate var7 = this._etcitems.get(var15);
         this._allTemplates[var15] = var7;
      }

      for (Integer var16 : this._weapons.keySet()) {
         NativeDeWeaponTemplate var18 = this._weapons.get(var16);
         this._allTemplates[var16] = var18;
      }

      for (Integer var17 : this._armors.keySet()) {
         NativeDeArmorTemplate var19 = this._armors.get(var17);
         this._allTemplates[var17] = var19;
      }
   }

   @Deprecated
   public void se_mode() {
      NativeDeItemTemplatesScope.debug(this.nativeTemplates.log, "se_mode() 已廢棄，新系統使用 SetBonusTable");
   }

   public NativeDeItemTemplate getTemplate(int var1) {
      try {
         return this._allTemplates[var1];
      } catch (Exception var3) {
         return null;
      }
   }

   public NativeDeItemTemplate getTemplate(String var1) {
      for (NativeDeItemTemplate var5 : this._allTemplates) {
         if (var5 != null && var5.getNameId().equals(var1)) {
            return var5;
         }
      }

      return null;
   }

   public int findItemIdByName(String var1) {
      int var2 = 0;

      for (NativeDeItemTemplate var6 : this._allTemplates) {
         if (var6 != null && var6.getNameId().equals(var1)) {
            var2 = var6.getItemId();
            break;
         }
      }

      return var2;
   }

   public int findItemIdByNameWithoutSpace(String var1) {
      int var2 = 0;

      for (NativeDeItemTemplate var6 : this._allTemplates) {
         if (var6 != null && var6.getNameId().replace(" ", "").equals(var1)) {
            var2 = var6.getItemId();
            break;
         }
      }

      return var2;
   }

   public List<NativeDeItemTemplate> findTemplatesByName(int var1, String var2) {
      List<NativeDeItemTemplate> var3 = new ArrayList<>();
      if (this._allTemplates == null || var2 == null) { return var3; }
         String var4 = var2.toLowerCase(Locale.ROOT);

         for (NativeDeItemTemplate var8 : this._allTemplates) {
            if (var8 == null || var8.getType2() != var1) continue;
            if (containsName(var8.getName(), var4)) var3.add(var8);
         }

         return var3;

   }

   public List<Integer> findItemIdsBySetId(int var1) {
      ArrayList var2 = new ArrayList();
      if (this._allTemplates == null) {
         return var2;
      } else {
         this.collectItemIdsBySetId(var2, var1, 1);
         this.collectItemIdsBySetId(var2, var1, 2);
         return var2;
      }
   }

   private void collectItemIdsBySetId(List<Integer> var1, int var2, int var3) {
      for (NativeDeItemTemplate var7 : this._allTemplates) {
         if (var7 == null || var7.getType2() != var3) continue;
         if (hasSetId(var7, var2)) var1.add(var7.getItemId());
      }
   }

   private static boolean hasSetId(NativeDeItemTemplate var0, int var1) {
      for (int var5 : var0.getAllSetIds()) {
         if (var5 == var1) {
            return true;
         }
      }

      return false;
   }

   private static boolean containsName(String var0, String var1) {
      return var0 != null && var0.toLowerCase(Locale.ROOT).contains(var1);
   }

   static {
      _etcItemTypes.put("arrow", 0);
      _etcItemTypes.put("wand", 1);
      _etcItemTypes.put("light", 2);
      _etcItemTypes.put("gem", 3);
      _etcItemTypes.put("totem", 4);
      _etcItemTypes.put("firecracker", 5);
      _etcItemTypes.put("potion", 6);
      _etcItemTypes.put("food", 7);
      _etcItemTypes.put("scroll", 8);
      _etcItemTypes.put("questitem", 9);
      _etcItemTypes.put("spellbook", 10);
      _etcItemTypes.put("petitem", 11);
      _etcItemTypes.put("other", 12);
      _etcItemTypes.put("material", 13);
      _etcItemTypes.put("event", 14);
      _etcItemTypes.put("sting", 15);
      _etcItemTypes.put("treasure_box", 16);
      _useTypes.put("petitem", -12);
      _useTypes.put("other", -11);
      _useTypes.put("power", -10);
      _useTypes.put("book", -9);
      _useTypes.put("makecooking", -8);
      _useTypes.put("hpr", -7);
      _useTypes.put("mpr", -6);
      _useTypes.put("ticket", -5);
      _useTypes.put("petcollar", -4);
      _useTypes.put("sting", -3);
      _useTypes.put("arrow", -2);
      _useTypes.put("none", -1);
      _useTypes.put("normal", 0);
      _useTypes.put("weapon", 1);
      _useTypes.put("armor", 2);
      _useTypes.put("spell_1", 3);
      _useTypes.put("4", 4);
      _useTypes.put("spell_long", 5);
      _useTypes.put("ntele", 6);
      _useTypes.put("identify", 7);
      _useTypes.put("res", 8);
      _useTypes.put("home", 9);
      _useTypes.put("light", 10);
      _useTypes.put("11", 11);
      _useTypes.put("letter", 12);
      _useTypes.put("letter_card", 13);
      _useTypes.put("choice", 14);
      _useTypes.put("instrument", 15);
      _useTypes.put("sosc", 16);
      _useTypes.put("spell_short", 17);
      _useTypes.put("T", 18);
      _useTypes.put("cloak", 19);
      _useTypes.put("glove", 20);
      _useTypes.put("boots", 21);
      _useTypes.put("helm", 22);
      _useTypes.put("ring", 23);
      _useTypes.put("amulet", 24);
      _useTypes.put("shield", 25);
      _useTypes.put("guarder", 25);
      _useTypes.put("dai", 26);
      _useTypes.put("zel", 27);
      _useTypes.put("blank", 28);
      _useTypes.put("btele", 29);
      _useTypes.put("spell_buff", 30);
      _useTypes.put("ccard", 31);
      _useTypes.put("ccard_w", 32);
      _useTypes.put("vcard", 33);
      _useTypes.put("vcard_w", 34);
      _useTypes.put("wcard", 35);
      _useTypes.put("wcard_w", 36);
      _useTypes.put("belt", 37);
      _useTypes.put("food", 38);
      _useTypes.put("spell_long2", 39);
      _useTypes.put("earring", 40);
      _useTypes.put("fishing_rod", 42);
      _useTypes.put("enc", 46);
      _useTypes.put("aid_left", 43);
      _useTypes.put("aid_right", 44);
      _useTypes.put("pants", 70);
      _useTypes.put("talisman", 49);
      _useTypes.put("talisman2", 49);
      _useTypes.put("talisman3", 49);
      _useTypes.put("talisman4", 49);
      _useTypes.put("talisman5", 49);
      _useTypes.put("shoulder_left", 49);
      _useTypes.put("shoulder_right", 49);
      _useTypes.put("choice_doll", 55);
      _armorTypes.put("none", 0);
      _armorTypes.put("helm", 1);
      _armorTypes.put("armor", 2);
      _armorTypes.put("T", 3);
      _armorTypes.put("cloak", 4);
      _armorTypes.put("glove", 5);
      _armorTypes.put("boots", 6);
      _armorTypes.put("shield", 7);
      _armorTypes.put("amulet", 8);
      _armorTypes.put("ring", 9);
      _armorTypes.put("belt", 10);
      _armorTypes.put("ring2", 11);
      _armorTypes.put("earring", 12);
      _armorTypes.put("guarder", 13);
      _armorTypes.put("aid_left", 14);
      _armorTypes.put("aid_right", 15);
      _armorTypes.put("pants", 17);
      _armorTypes.put("talisman", 19);
      _armorTypes.put("talisman2", 20);
      _armorTypes.put("talisman3", 21);
      _armorTypes.put("talisman4", 22);
      _armorTypes.put("shoulder_left", 23);
      _armorTypes.put("shoulder_right", 24);
      _armorTypes.put("talisman5", 25);
      _weaponTypes.put("none", 0);
      _weaponTypes.put("sword", 1);
      _weaponTypes.put("dagger", 2);
      _weaponTypes.put("tohandsword", 3);
      _weaponTypes.put("bow", 4);
      _weaponTypes.put("spear", 5);
      _weaponTypes.put("blunt", 6);
      _weaponTypes.put("staff", 7);
      _weaponTypes.put("throwingknife", 8);
      _weaponTypes.put("arrow", 9);
      _weaponTypes.put("gauntlet", 10);
      _weaponTypes.put("claw", 11);
      _weaponTypes.put("edoryu", 12);
      _weaponTypes.put("singlebow", 13);
      _weaponTypes.put("singlespear", 14);
      _weaponTypes.put("tohandblunt", 15);
      _weaponTypes.put("tohandstaff", 16);
      _weaponTypes.put("kiringku", 17);
      _weaponTypes.put("chainsword", 18);
      _weaponId.put("sword", 4);
      _weaponId.put("dagger", 46);
      _weaponId.put("tohandsword", 50);
      _weaponId.put("bow", 20);
      _weaponId.put("blunt", 11);
      _weaponId.put("spear", 24);
      _weaponId.put("staff", 40);
      _weaponId.put("throwingknife", 2922);
      _weaponId.put("arrow", 66);
      _weaponId.put("gauntlet", 62);
      _weaponId.put("claw", 58);
      _weaponId.put("edoryu", 54);
      _weaponId.put("singlebow", 20);
      _weaponId.put("singlespear", 24);
      _weaponId.put("tohandblunt", 11);
      _weaponId.put("tohandstaff", 40);
      _weaponId.put("kiringku", 58);
      _weaponId.put("chainsword", 24);
      _weaponId.put("chainsword", 83);
      _materialTypes.put("none", 0);
      _materialTypes.put("liquid", 1);
      _materialTypes.put("web", 2);
      _materialTypes.put("vegetation", 3);
      _materialTypes.put("animalmatter", 4);
      _materialTypes.put("paper", 5);
      _materialTypes.put("cloth", 6);
      _materialTypes.put("leather", 7);
      _materialTypes.put("wood", 8);
      _materialTypes.put("bone", 9);
      _materialTypes.put("dragonscale", 10);
      _materialTypes.put("iron", 11);
      _materialTypes.put("steel", 12);
      _materialTypes.put("copper", 13);
      _materialTypes.put("silver", 14);
      _materialTypes.put("gold", 15);
      _materialTypes.put("platinum", 16);
      _materialTypes.put("mithril", 17);
      _materialTypes.put("blackmithril", 18);
      _materialTypes.put("glass", 19);
      _materialTypes.put("gemstone", 20);
      _materialTypes.put("mineral", 21);
      _materialTypes.put("oriharukon", 22);
      _armorTypes = Collections.unmodifiableMap(_armorTypes);
      _etcItemTypes = Collections.unmodifiableMap(_etcItemTypes);
      _materialTypes = Collections.unmodifiableMap(_materialTypes);
      _useTypes = Collections.unmodifiableMap(_useTypes);
      _weaponId = Collections.unmodifiableMap(_weaponId);
      _weaponTypes = Collections.unmodifiableMap(_weaponTypes);
   }

   public static NativeDeItemTable get() {
      return NativeDeItemTemplatesScope.table();
   }
}
