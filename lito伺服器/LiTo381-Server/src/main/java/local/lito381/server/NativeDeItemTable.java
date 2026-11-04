// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import local.lito381.server.NativeDeArmorTemplate;
import local.lito381.server.NativeDeEtcItemTemplate;
import local.lito381.server.NativeDeItemTemplate;
import local.lito381.server.NativeDeItemTemplatesScope;
import local.lito381.server.NativeDeTerrainTimer;
import local.lito381.server.NativeDeWeaponTemplate;
import local.lito381.server.NativeDeWorldLog;

public class NativeDeItemTable {
    private static final NativeDeWorldLog _log = NativeDeItemTemplatesScope.Logs.getLogger(NativeDeItemTable.class);
    private static final Map<String, Integer> _armorTypes = new HashMap<String, Integer>();
    private static final Map<String, Integer> _weaponTypes = new HashMap<String, Integer>();
    private static final Map<String, Integer> _weaponId = new HashMap<String, Integer>();
    private static final Map<String, Integer> _materialTypes = new HashMap<String, Integer>();
    private static final Map<String, Integer> _etcItemTypes = new HashMap<String, Integer>();
    private static final Map<String, Integer> _useTypes = new HashMap<String, Integer>();
    private NativeDeItemTemplate[] _allTemplates;
    private Map<Integer, NativeDeEtcItemTemplate> _etcitems;
    private Map<Integer, NativeDeArmorTemplate> _armors;
    private Map<Integer, NativeDeWeaponTemplate> _weapons;
    final NativeDeItemTemplatesScope.Data nativeTemplates = NativeDeItemTemplatesScope.loadingData();

    public void load() {
        NativeDeItemTemplatesScope.requireLoading(this.nativeTemplates);
        NativeDeTerrainTimer nativeDeTerrainTimer = new NativeDeTerrainTimer();
        this._etcitems = this.allEtcItem();
        this._weapons = this.allWeapon();
        this._armors = this.allArmor();
        this.buildFastLookupTable();
        this.nativeTemplates.log.info("\u8f09\u5165 etcitem ok: " + this._etcitems.size() + " (" + nativeDeTerrainTimer.get() + "ms)");
        this.nativeTemplates.log.info("\u8f09\u5165 weapon ok: " + this._weapons.size() + " (" + nativeDeTerrainTimer.get() + "ms)");
        this.nativeTemplates.log.info("\u8f09\u5165 armor ok: " + this._armors.size() + " (" + nativeDeTerrainTimer.get() + "ms)");
    }

    public void reloadArmor() {
        NativeDeItemTemplatesScope.requireLoading(this.nativeTemplates);
        this._armors = this.allArmor();
        this.buildFastLookupTable();
        this.nativeTemplates.log.info("\u71b1\u91cd\u8f09 armor: " + this._armors.size());
    }

    public void reloadWeapon() {
        NativeDeItemTemplatesScope.requireLoading(this.nativeTemplates);
        this._weapons = this.allWeapon();
        this.buildFastLookupTable();
        this.nativeTemplates.log.info("\u71b1\u91cd\u8f09 weapon: " + this._weapons.size());
    }

    public void reloadEtcitem() {
        NativeDeItemTemplatesScope.requireLoading(this.nativeTemplates);
        this._etcitems = this.allEtcItem();
        this.buildFastLookupTable();
        this.nativeTemplates.log.info("\u71b1\u91cd\u8f09 etcitem: " + this._etcitems.size());
    }

    private Map<Integer, NativeDeEtcItemTemplate> allEtcItem() {
        HashMap<Integer, NativeDeEtcItemTemplate> hashMap;
        block24: {
            NativeDeItemTemplatesScope.requireLoading(this.nativeTemplates);
            hashMap = new HashMap<Integer, NativeDeEtcItemTemplate>();
            try (Connection connection = NativeDeItemTemplatesScope.Database.get().getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement("SELECT e.*, a.item_id AS arrow_config_id, a.consume_on_attack AS arrow_consume_on_attack, a.dmg_small AS arrow_dmg_small, a.dmg_large AS arrow_dmg_large, a.arrow_spr AS arrow_config_spr FROM `etcitem` e LEFT JOIN `etcitem_arrow` a ON a.item_id = e.item_id");){
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    while (resultSet.next()) {
                        int n = -1;
                        try {
                            n = resultSet.getInt("item_id");
                            NativeDeEtcItemTemplate nativeDeEtcItemTemplate = new NativeDeEtcItemTemplate();
                            nativeDeEtcItemTemplate.setItemId(n);
                            nativeDeEtcItemTemplate.setName(resultSet.getString("name"));
                            String string = resultSet.getString("classname");
                            nativeDeEtcItemTemplate.setClassname(string);
                            nativeDeEtcItemTemplate.setNameId(resultSet.getString("name_id"));
                            nativeDeEtcItemTemplate.setType(_etcItemTypes.get(resultSet.getString("item_type")));
                            nativeDeEtcItemTemplate.setUseType(_useTypes.get(resultSet.getString("use_type")));
                            nativeDeEtcItemTemplate.setType2(0);
                            nativeDeEtcItemTemplate.setMaterial(_materialTypes.get(resultSet.getString("material")));
                            nativeDeEtcItemTemplate.setWeight(resultSet.getInt("weight"));
                            nativeDeEtcItemTemplate.setGfxId(resultSet.getInt("invgfx"));
                            nativeDeEtcItemTemplate.setGroundGfxId(resultSet.getInt("grdgfx"));
                            nativeDeEtcItemTemplate.setItemDescId(resultSet.getInt("itemdesc_id"));
                            nativeDeEtcItemTemplate.setMinLevel(resultSet.getInt("min_lvl"));
                            nativeDeEtcItemTemplate.setMaxLevel(resultSet.getInt("max_lvl"));
                            nativeDeEtcItemTemplate.setBless(resultSet.getInt("bless"));
                            nativeDeEtcItemTemplate.setTradable(resultSet.getInt("trade") == 0);
                            nativeDeEtcItemTemplate.setCantDelete(resultSet.getInt("cant_delete") == 1);
                            nativeDeEtcItemTemplate.set_stackable(resultSet.getInt("stackable") == 1);
                            nativeDeEtcItemTemplate.setMaxChargeCount(resultSet.getInt("max_charge_count"));
                            nativeDeEtcItemTemplate.set_delayid(resultSet.getInt("delay_id"));
                            nativeDeEtcItemTemplate.set_delaytime(resultSet.getInt("delay_time"));
                            nativeDeEtcItemTemplate.set_delayEffect(resultSet.getInt("delay_effect"));
                            nativeDeEtcItemTemplate.setFoodVolume(resultSet.getInt("food_volume"));
                            nativeDeEtcItemTemplate.setToBeSavedAtOnce(resultSet.getInt("save_at_once") == 1);
                            nativeDeEtcItemTemplate.setLimitClass(resultSet.getInt("limit_class"));
                            if (!this.applyArrowConfig(resultSet, nativeDeEtcItemTemplate)) continue;
                            NativeDeItemTemplatesScope.Classes.get().addList(n, string, 0);
                            hashMap.put(nativeDeEtcItemTemplate.getItemId(), nativeDeEtcItemTemplate);
                            continue;
                        }
                        catch (Exception exception) {
                            this.nativeTemplates.log.error("\u8f09\u5165\u7269\u54c1\u5931\u6557\uff0c\u5df2\u8df3\u904e\u8a72\u7269\u54c1 (item_id: " + n + ")", exception);
                        }
                    }
                }
                finally {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
            }
            catch (SQLException sQLException) {
                this.nativeTemplates.log.error("\u8f09\u5165 etcitem \u5931\u6557! \u8acb\u6aa2\u67e5\u8cc7\u6599\u8868!", sQLException);
            }
        }
        return hashMap;
    }

    private boolean applyArrowConfig(ResultSet resultSet, NativeDeEtcItemTemplate nativeDeEtcItemTemplate) throws SQLException {
        boolean bl;
        int n = resultSet.getInt("arrow_config_id");
        boolean bl2 = !resultSet.wasNull();
        int n2 = nativeDeEtcItemTemplate.getUseType();
        boolean bl3 = bl = n2 == -2 || n2 == -3;
        if (!bl) {
            if (bl2) {
                this.nativeTemplates.log.warn("\u5ffd\u7565\u975e\u7bad\u77e2\u9053\u5177\u7684 etcitem_arrow \u8a2d\u5b9a itemId={} useType={}", (Object)nativeDeEtcItemTemplate.getItemId(), (Object)n2);
            }
            return true;
        }
        if (!bl2 || n != nativeDeEtcItemTemplate.getItemId()) {
            this.nativeTemplates.log.error("\u7bad\u77e2\uff0f\u98db\u5200\u7f3a\u5c11 etcitem_arrow \u8a2d\u5b9a\uff0c\u5df2\u7565\u904e itemId={} name={}", (Object)nativeDeEtcItemTemplate.getItemId(), (Object)nativeDeEtcItemTemplate.getName());
            return false;
        }
        int n3 = resultSet.getInt("arrow_consume_on_attack");
        int n4 = resultSet.getInt("arrow_dmg_small");
        int n5 = resultSet.getInt("arrow_dmg_large");
        int n6 = resultSet.getInt("arrow_config_spr");
        if (n3 != 0 && n3 != 1 || n4 < 0 || n5 < 0 || n6 < -1 || n6 > 65535) {
            this.nativeTemplates.log.error("etcitem_arrow \u8a2d\u5b9a\u4e0d\u5408\u6cd5\uff0c\u5df2\u7565\u904e itemId={} consume={} small={} large={} spr={}", (Object)nativeDeEtcItemTemplate.getItemId(), (Object)n3, (Object)n4, (Object)n5, (Object)n6);
            return false;
        }
        nativeDeEtcItemTemplate.setConsumeOnAttack(n3 == 1);
        nativeDeEtcItemTemplate.setDmgSmall(n4);
        nativeDeEtcItemTemplate.setDmgLarge(n5);
        nativeDeEtcItemTemplate.setArrowSpr(n6);
        return true;
    }

    private Map<Integer, NativeDeWeaponTemplate> allWeapon() {
        HashMap<Integer, NativeDeWeaponTemplate> hashMap;
        block28: {
            NativeDeItemTemplatesScope.requireLoading(this.nativeTemplates);
            hashMap = new HashMap<Integer, NativeDeWeaponTemplate>();
            try (Connection connection = NativeDeItemTemplatesScope.Database.get().getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `weapon`");){
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    while (resultSet.next()) {
                        int n = -1;
                        try {
                            n = resultSet.getInt("item_id");
                            NativeDeWeaponTemplate nativeDeWeaponTemplate = new NativeDeWeaponTemplate();
                            nativeDeWeaponTemplate.setItemId(n);
                            nativeDeWeaponTemplate.setName(resultSet.getString("name"));
                            String string = resultSet.getString("classname");
                            nativeDeWeaponTemplate.setClassname(string);
                            nativeDeWeaponTemplate.setNameId(resultSet.getString("name_id"));
                            nativeDeWeaponTemplate.setType(_weaponTypes.get(resultSet.getString("type")));
                            nativeDeWeaponTemplate.setType1(_weaponId.get(resultSet.getString("type")));
                            nativeDeWeaponTemplate.setType2(1);
                            nativeDeWeaponTemplate.setUseType(1);
                            nativeDeWeaponTemplate.setMaterial(_materialTypes.get(resultSet.getString("material")));
                            nativeDeWeaponTemplate.setWeight(resultSet.getInt("weight"));
                            nativeDeWeaponTemplate.setGfxId(resultSet.getInt("invgfx"));
                            nativeDeWeaponTemplate.setGroundGfxId(resultSet.getInt("grdgfx"));
                            nativeDeWeaponTemplate.setItemDescId(resultSet.getInt("itemdesc_id"));
                            nativeDeWeaponTemplate.setDmgSmall(resultSet.getInt("dmg_small"));
                            nativeDeWeaponTemplate.setDmgLarge(resultSet.getInt("dmg_large"));
                            nativeDeWeaponTemplate.setRange(resultSet.getInt("range"));
                            nativeDeWeaponTemplate.set_safeenchant(resultSet.getInt("safenchant"));
                            nativeDeWeaponTemplate.setUseRoyal(resultSet.getInt("use_royal") != 0);
                            nativeDeWeaponTemplate.setUseKnight(resultSet.getInt("use_knight") != 0);
                            nativeDeWeaponTemplate.setUseElf(resultSet.getInt("use_elf") != 0);
                            nativeDeWeaponTemplate.setUseMage(resultSet.getInt("use_mage") != 0);
                            nativeDeWeaponTemplate.setUseDarkelf(resultSet.getInt("use_darkelf") != 0);
                            nativeDeWeaponTemplate.setUseDragonknight(resultSet.getInt("use_dragonknight") != 0);
                            nativeDeWeaponTemplate.setUseIllusionist(resultSet.getInt("use_illusionist") != 0);
                            nativeDeWeaponTemplate.setHitModifier(resultSet.getInt("hitmodifier"));
                            nativeDeWeaponTemplate.setDmgModifier(resultSet.getInt("dmgmodifier"));
                            nativeDeWeaponTemplate.set_addstr(resultSet.getByte("add_str"));
                            nativeDeWeaponTemplate.set_adddex(resultSet.getByte("add_dex"));
                            nativeDeWeaponTemplate.set_addcon(resultSet.getByte("add_con"));
                            nativeDeWeaponTemplate.set_addint(resultSet.getByte("add_int"));
                            nativeDeWeaponTemplate.set_addwis(resultSet.getByte("add_wis"));
                            nativeDeWeaponTemplate.set_addcha(resultSet.getByte("add_cha"));
                            nativeDeWeaponTemplate.set_addhp(resultSet.getInt("add_hp"));
                            nativeDeWeaponTemplate.set_addmp(resultSet.getInt("add_mp"));
                            nativeDeWeaponTemplate.set_addhpr(resultSet.getInt("add_hpr"));
                            nativeDeWeaponTemplate.set_addmpr(resultSet.getInt("add_mpr"));
                            nativeDeWeaponTemplate.set_addsp(resultSet.getInt("add_sp"));
                            nativeDeWeaponTemplate.set_mdef(resultSet.getInt("m_def"));
                            nativeDeWeaponTemplate.setDmgCriChance(resultSet.getInt("dmg_cri_chance"));
                            nativeDeWeaponTemplate.setDmgCri(NativeDeItemTable.parseDmgCri(resultSet, "dmg_cri"));
                            nativeDeWeaponTemplate.setDmgMaxChance(resultSet.getInt("dmg_max_chance"));
                            nativeDeWeaponTemplate.setDmgCriEffectEnabled(resultSet.getInt("dmg_cri_effect") == 1);
                            nativeDeWeaponTemplate.setDmgMaxEffectEnabled(resultSet.getInt("dmg_max_effect") == 1);
                            nativeDeWeaponTemplate.setMagicDmgModifier(resultSet.getInt("magicdmgmodifier"));
                            nativeDeWeaponTemplate.set_canbedmg(resultSet.getInt("canbedmg"));
                            nativeDeWeaponTemplate.setMinLevel(resultSet.getInt("min_lvl"));
                            nativeDeWeaponTemplate.setMaxLevel(resultSet.getInt("max_lvl"));
                            nativeDeWeaponTemplate.setBless(resultSet.getInt("bless"));
                            nativeDeWeaponTemplate.setTradable(resultSet.getInt("trade") == 0);
                            nativeDeWeaponTemplate.setCantDelete(resultSet.getInt("cant_delete") == 1);
                            nativeDeWeaponTemplate.setHasteItem(resultSet.getInt("haste_item") != 0);
                            nativeDeWeaponTemplate.setMaxUseTime(resultSet.getInt("max_use_time"));
                            nativeDeWeaponTemplate.setAddPvpDmg(resultSet.getInt("add_pvp_dmg"));
                            nativeDeWeaponTemplate.setAddIgnoreReduction(resultSet.getInt("add_ignore_reduction"));
                            nativeDeWeaponTemplate.setAddPvpRedux(resultSet.getInt("add_pvp_redux"));
                            nativeDeWeaponTemplate.setAddBossDmg(resultSet.getInt("add_boss_dmg"));
                            nativeDeWeaponTemplate.setAddBossRedux(resultSet.getInt("add_boss_redux"));
                            nativeDeWeaponTemplate.setAddDodge(resultSet.getInt("add_dodge"));
                            nativeDeWeaponTemplate.setAddEr(resultSet.getInt("add_er"));
                            nativeDeWeaponTemplate.setAddHitup(resultSet.getInt("add_hitup"));
                            nativeDeWeaponTemplate.setAddDmgup(resultSet.getInt("add_dmgup"));
                            nativeDeWeaponTemplate.setAddBowHitup(resultSet.getInt("add_bow_hitup"));
                            nativeDeWeaponTemplate.setAddBowDmgup(resultSet.getInt("add_bow_dmgup"));
                            nativeDeWeaponTemplate.setAddAdenaRate(resultSet.getInt("add_adena_rate"));
                            nativeDeWeaponTemplate.setAddDropRate(resultSet.getInt("add_drop_rate"));
                            nativeDeWeaponTemplate.setAddMeleeRedux(resultSet.getInt("add_melee_redux"));
                            nativeDeWeaponTemplate.setAddBowRedux(resultSet.getInt("add_bow_redux"));
                            nativeDeWeaponTemplate.setAddStunSuccess(resultSet.getInt("add_stun_success"));
                            nativeDeWeaponTemplate.setAddPotionRecovery(resultSet.getInt("add_potion_recovery"));
                            nativeDeWeaponTemplate.setAddPoisonResist(resultSet.getInt("add_poison_resist"));
                            nativeDeWeaponTemplate.setAddExpRate(resultSet.getInt("add_exp_rate"));
                            nativeDeWeaponTemplate.setAddPierce(resultSet.getInt("add_pierce"));
                            nativeDeWeaponTemplate.setMagicBow(resultSet.getInt("magic_bow"));
                            nativeDeWeaponTemplate.setArrowSpr(resultSet.getInt("arrow_spr"));
                            nativeDeWeaponTemplate.setAddMagicHit(resultSet.getInt("add_magic_hit"));
                            try {
                                String string2 = resultSet.getString("set_id");
                                nativeDeWeaponTemplate.setSetIds(string2);
                            }
                            catch (SQLException sQLException) {
                                nativeDeWeaponTemplate.setSetIds(null);
                            }
                            try {
                                nativeDeWeaponTemplate.setGreaterTier(resultSet.getInt("greater_tier"));
                            }
                            catch (SQLException sQLException) {
                                nativeDeWeaponTemplate.setGreaterTier(0);
                            }
                            NativeDeItemTemplatesScope.Classes.get().addList(n, string, 1);
                            hashMap.put(nativeDeWeaponTemplate.getItemId(), nativeDeWeaponTemplate);
                            continue;
                        }
                        catch (Exception exception) {
                            this.nativeTemplates.log.error("\u8f09\u5165\u7269\u54c1\u5931\u6557\uff0c\u5df2\u8df3\u904e\u8a72\u7269\u54c1 (item_id: " + n + ")", exception);
                        }
                    }
                }
                finally {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
            }
            catch (SQLException sQLException) {
                this.nativeTemplates.log.error("\u8f09\u5165 weapon \u5931\u6557! \u8acb\u6aa2\u67e5\u8cc7\u6599\u8868!", sQLException);
            }
        }
        return hashMap;
    }

    private static double parseDmgCri(ResultSet resultSet, String string) throws SQLException {
        double d = resultSet.getDouble(string);
        return d >= 10.0 ? d / 100.0 : d;
    }

    private Map<Integer, NativeDeArmorTemplate> allArmor() {
        HashMap<Integer, NativeDeArmorTemplate> hashMap;
        block28: {
            NativeDeItemTemplatesScope.requireLoading(this.nativeTemplates);
            hashMap = new HashMap<Integer, NativeDeArmorTemplate>();
            try (Connection connection = NativeDeItemTemplatesScope.Database.get().getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `armor`");){
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    while (resultSet.next()) {
                        int n = -1;
                        try {
                            n = resultSet.getInt("item_id");
                            NativeDeArmorTemplate nativeDeArmorTemplate = new NativeDeArmorTemplate();
                            nativeDeArmorTemplate.setItemId(n);
                            nativeDeArmorTemplate.setName(resultSet.getString("name"));
                            String string = resultSet.getString("classname");
                            nativeDeArmorTemplate.setClassname(string);
                            nativeDeArmorTemplate.setNameId(resultSet.getString("name_id"));
                            nativeDeArmorTemplate.setType(_armorTypes.get(resultSet.getString("type")));
                            nativeDeArmorTemplate.setType2(2);
                            nativeDeArmorTemplate.setUseType(_useTypes.get(resultSet.getString("type")));
                            nativeDeArmorTemplate.setMaterial(_materialTypes.get(resultSet.getString("material")));
                            nativeDeArmorTemplate.setWeight(resultSet.getInt("weight"));
                            nativeDeArmorTemplate.setGfxId(resultSet.getInt("invgfx"));
                            nativeDeArmorTemplate.setGroundGfxId(resultSet.getInt("grdgfx"));
                            nativeDeArmorTemplate.setItemDescId(resultSet.getInt("itemdesc_id"));
                            nativeDeArmorTemplate.set_ac(resultSet.getInt("ac"));
                            nativeDeArmorTemplate.set_safeenchant(resultSet.getInt("safenchant"));
                            nativeDeArmorTemplate.setUseRoyal(resultSet.getInt("use_royal") != 0);
                            nativeDeArmorTemplate.setUseKnight(resultSet.getInt("use_knight") != 0);
                            nativeDeArmorTemplate.setUseElf(resultSet.getInt("use_elf") != 0);
                            nativeDeArmorTemplate.setUseMage(resultSet.getInt("use_mage") != 0);
                            nativeDeArmorTemplate.setUseDarkelf(resultSet.getInt("use_darkelf") != 0);
                            nativeDeArmorTemplate.setUseDragonknight(resultSet.getInt("use_dragonknight") != 0);
                            nativeDeArmorTemplate.setUseIllusionist(resultSet.getInt("use_illusionist") != 0);
                            nativeDeArmorTemplate.set_addstr(resultSet.getByte("add_str"));
                            nativeDeArmorTemplate.set_addcon(resultSet.getByte("add_con"));
                            nativeDeArmorTemplate.set_adddex(resultSet.getByte("add_dex"));
                            nativeDeArmorTemplate.set_addint(resultSet.getByte("add_int"));
                            nativeDeArmorTemplate.set_addwis(resultSet.getByte("add_wis"));
                            nativeDeArmorTemplate.set_addcha(resultSet.getByte("add_cha"));
                            nativeDeArmorTemplate.set_addhp(resultSet.getInt("add_hp"));
                            nativeDeArmorTemplate.set_addmp(resultSet.getInt("add_mp"));
                            nativeDeArmorTemplate.set_addhpr(resultSet.getInt("add_hpr"));
                            nativeDeArmorTemplate.set_addmpr(resultSet.getInt("add_mpr"));
                            nativeDeArmorTemplate.set_addsp(resultSet.getInt("add_sp"));
                            nativeDeArmorTemplate.setMinLevel(resultSet.getInt("min_lvl"));
                            nativeDeArmorTemplate.setMaxLevel(resultSet.getInt("max_lvl"));
                            nativeDeArmorTemplate.set_mdef(resultSet.getInt("m_def"));
                            nativeDeArmorTemplate.setDamageReduction(resultSet.getInt("damage_reduction"));
                            nativeDeArmorTemplate.setWeightReduction(resultSet.getInt("weight_reduction"));
                            nativeDeArmorTemplate.setHitModifierByArmor(resultSet.getInt("hit_modifier"));
                            nativeDeArmorTemplate.setDmgModifierByArmor(resultSet.getInt("dmg_modifier"));
                            nativeDeArmorTemplate.setBowHitModifierByArmor(resultSet.getInt("bow_hit_modifier"));
                            nativeDeArmorTemplate.setBowDmgModifierByArmor(resultSet.getInt("bow_dmg_modifier"));
                            nativeDeArmorTemplate.setHasteItem(resultSet.getInt("haste_item") != 0);
                            nativeDeArmorTemplate.setBless(resultSet.getInt("bless"));
                            nativeDeArmorTemplate.setTradable(resultSet.getInt("trade") == 0);
                            nativeDeArmorTemplate.setCantDelete(resultSet.getInt("cant_delete") == 1);
                            nativeDeArmorTemplate.set_defense_earth(resultSet.getInt("defense_earth"));
                            nativeDeArmorTemplate.set_defense_water(resultSet.getInt("defense_water"));
                            nativeDeArmorTemplate.set_defense_wind(resultSet.getInt("defense_wind"));
                            nativeDeArmorTemplate.set_defense_fire(resultSet.getInt("defense_fire"));
                            nativeDeArmorTemplate.set_regist_stun(resultSet.getInt("regist_stun"));
                            nativeDeArmorTemplate.set_regist_stone(resultSet.getInt("regist_stone"));
                            nativeDeArmorTemplate.set_regist_sleep(resultSet.getInt("regist_sleep"));
                            nativeDeArmorTemplate.set_regist_freeze(resultSet.getInt("regist_freeze"));
                            nativeDeArmorTemplate.set_regist_sustain(resultSet.getInt("regist_sustain"));
                            nativeDeArmorTemplate.set_regist_blind(resultSet.getInt("regist_blind"));
                            nativeDeArmorTemplate.setMaxUseTime(resultSet.getInt("max_use_time"));
                            nativeDeArmorTemplate.setActivity(resultSet.getBoolean("is_activity"));
                            nativeDeArmorTemplate.setAddPvpRedux(resultSet.getInt("add_pvp_redux"));
                            nativeDeArmorTemplate.setAddIgnoreReduction(resultSet.getInt("add_ignore_reduction"));
                            nativeDeArmorTemplate.setAddPvpDmg(resultSet.getInt("add_pvp_dmg"));
                            nativeDeArmorTemplate.setAddBossDmg(resultSet.getInt("add_boss_dmg"));
                            nativeDeArmorTemplate.setAddBossRedux(resultSet.getInt("add_boss_redux"));
                            nativeDeArmorTemplate.setAddDodge(resultSet.getInt("add_dodge"));
                            nativeDeArmorTemplate.setAddEr(resultSet.getInt("add_er"));
                            nativeDeArmorTemplate.setAddHitup(resultSet.getInt("add_hitup"));
                            nativeDeArmorTemplate.setAddDmgup(resultSet.getInt("add_dmgup"));
                            nativeDeArmorTemplate.setAddBowHitup(resultSet.getInt("add_bow_hitup"));
                            nativeDeArmorTemplate.setAddBowDmgup(resultSet.getInt("add_bow_dmgup"));
                            nativeDeArmorTemplate.setAddAdenaRate(resultSet.getInt("add_adena_rate"));
                            nativeDeArmorTemplate.setAddDropRate(resultSet.getInt("add_drop_rate"));
                            nativeDeArmorTemplate.setAddMeleeRedux(resultSet.getInt("add_melee_redux"));
                            nativeDeArmorTemplate.setAddBowRedux(resultSet.getInt("add_bow_redux"));
                            nativeDeArmorTemplate.setAddStunSuccess(resultSet.getInt("add_stun_success"));
                            nativeDeArmorTemplate.setMagicDmgModifier(resultSet.getInt("magicdmgmodifier"));
                            nativeDeArmorTemplate.setAddPotionRecovery(resultSet.getInt("add_potion_recovery"));
                            nativeDeArmorTemplate.setAddPoisonResist(resultSet.getInt("add_poison_resist"));
                            nativeDeArmorTemplate.setAddExpRate(resultSet.getInt("add_exp_rate"));
                            nativeDeArmorTemplate.setAddMagicHit(resultSet.getInt("add_magic_hit"));
                            try {
                                String string2 = resultSet.getString("set_id");
                                nativeDeArmorTemplate.setSetIds(string2);
                            }
                            catch (SQLException sQLException) {
                                nativeDeArmorTemplate.setSetIds(null);
                            }
                            try {
                                nativeDeArmorTemplate.setGreaterTier(resultSet.getInt("greater_tier"));
                            }
                            catch (SQLException sQLException) {
                                nativeDeArmorTemplate.setGreaterTier(0);
                            }
                            NativeDeItemTemplatesScope.Classes.get().addList(n, string, 2);
                            hashMap.put(nativeDeArmorTemplate.getItemId(), nativeDeArmorTemplate);
                            continue;
                        }
                        catch (Exception exception) {
                            this.nativeTemplates.log.error("\u8f09\u5165\u7269\u54c1\u5931\u6557\uff0c\u5df2\u8df3\u904e\u8a72\u7269\u54c1 (item_id: " + n + ")", exception);
                        }
                    }
                }
                finally {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
            }
            catch (SQLException sQLException) {
                this.nativeTemplates.log.error("\u8f09\u5165 armor \u5931\u6557! \u8acb\u6aa2\u67e5\u8cc7\u6599\u8868!", sQLException);
            }
        }
        return hashMap;
    }

    private void buildFastLookupTable() {
        NativeDeItemTemplate nativeDeItemTemplate;
        NativeDeItemTemplatesScope.requireLoading(this.nativeTemplates);
        int n = 0;
        Collection<NativeDeEtcItemTemplate> collection = this._etcitems.values();
        for (NativeDeEtcItemTemplate object22 : collection) {
            if (object22.getItemId() <= n) continue;
            n = object22.getItemId();
        }
        Collection<NativeDeWeaponTemplate> collection2 = this._weapons.values();
        Iterator iterator = collection2.iterator();
        while (iterator.hasNext()) {
            NativeDeWeaponTemplate nativeDeWeaponTemplate = (NativeDeWeaponTemplate)iterator.next();
            if (nativeDeWeaponTemplate.getItemId() <= n) continue;
            n = nativeDeWeaponTemplate.getItemId();
        }
        Collection<NativeDeArmorTemplate> collection3 = this._armors.values();
        for (NativeDeArmorTemplate nativeDeArmorTemplate : collection3) {
            if (nativeDeArmorTemplate.getItemId() <= n) continue;
            n = nativeDeArmorTemplate.getItemId();
        }
        this._allTemplates = new NativeDeItemTemplate[n + 1];
        for (Integer n2 : this._etcitems.keySet()) {
            nativeDeItemTemplate = this._etcitems.get(n2);
            this._allTemplates[n2.intValue()] = nativeDeItemTemplate;
        }
        for (Integer n3 : this._weapons.keySet()) {
            nativeDeItemTemplate = this._weapons.get(n3);
            this._allTemplates[n3.intValue()] = nativeDeItemTemplate;
        }
        for (Integer n4 : this._armors.keySet()) {
            nativeDeItemTemplate = this._armors.get(n4);
            this._allTemplates[n4.intValue()] = nativeDeItemTemplate;
        }
    }

    @Deprecated
    public void se_mode() {
        NativeDeItemTemplatesScope.debug(this.nativeTemplates.log, "se_mode() \u5df2\u5ee2\u68c4\uff0c\u65b0\u7cfb\u7d71\u4f7f\u7528 SetBonusTable");
    }

    public NativeDeItemTemplate getTemplate(int n) {
        try {
            return this._allTemplates[n];
        }
        catch (Exception exception) {
            return null;
        }
    }

    public NativeDeItemTemplate getTemplate(String string) {
        for (NativeDeItemTemplate nativeDeItemTemplate : this._allTemplates) {
            if (nativeDeItemTemplate == null || !nativeDeItemTemplate.getNameId().equals(string)) continue;
            return nativeDeItemTemplate;
        }
        return null;
    }

    public int findItemIdByName(String string) {
        int n = 0;
        for (NativeDeItemTemplate nativeDeItemTemplate : this._allTemplates) {
            if (nativeDeItemTemplate == null || !nativeDeItemTemplate.getNameId().equals(string)) continue;
            n = nativeDeItemTemplate.getItemId();
            break;
        }
        return n;
    }

    public int findItemIdByNameWithoutSpace(String string) {
        int n = 0;
        for (NativeDeItemTemplate nativeDeItemTemplate : this._allTemplates) {
            if (nativeDeItemTemplate == null || !nativeDeItemTemplate.getNameId().replace(" ", "").equals(string)) continue;
            n = nativeDeItemTemplate.getItemId();
            break;
        }
        return n;
    }

    public List<NativeDeItemTemplate> findTemplatesByName(int n, String string) {
        ArrayList<NativeDeItemTemplate> arrayList = new ArrayList<NativeDeItemTemplate>();
        if (this._allTemplates == null || string == null) {
            return arrayList;
        }
        String string2 = string.toLowerCase(Locale.ROOT);
        for (NativeDeItemTemplate nativeDeItemTemplate : this._allTemplates) {
            if (nativeDeItemTemplate == null || nativeDeItemTemplate.getType2() != n || !NativeDeItemTable.containsName(nativeDeItemTemplate.getName(), string2)) continue;
            arrayList.add(nativeDeItemTemplate);
        }
        return arrayList;
    }

    public List<Integer> findItemIdsBySetId(int n) {
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        if (this._allTemplates == null) {
            return arrayList;
        }
        this.collectItemIdsBySetId(arrayList, n, 1);
        this.collectItemIdsBySetId(arrayList, n, 2);
        return arrayList;
    }

    private void collectItemIdsBySetId(List<Integer> list, int n, int n2) {
        for (NativeDeItemTemplate nativeDeItemTemplate : this._allTemplates) {
            if (nativeDeItemTemplate == null || nativeDeItemTemplate.getType2() != n2 || !NativeDeItemTable.hasSetId(nativeDeItemTemplate, n)) continue;
            list.add(nativeDeItemTemplate.getItemId());
        }
    }

    private static boolean hasSetId(NativeDeItemTemplate nativeDeItemTemplate, int n) {
        for (int n2 : nativeDeItemTemplate.getAllSetIds()) {
            if (n2 != n) continue;
            return true;
        }
        return false;
    }

    private static boolean containsName(String string, String string2) {
        return string != null && string.toLowerCase(Locale.ROOT).contains(string2);
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
