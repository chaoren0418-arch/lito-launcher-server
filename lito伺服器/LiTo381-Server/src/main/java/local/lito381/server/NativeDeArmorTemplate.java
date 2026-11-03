// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeItemTemplate;

public class NativeDeArmorTemplate
extends NativeDeItemTemplate {
    private static final long serialVersionUID = 1L;
    private int _ac = 0;
    private int _damageReduction = 0;
    private int _weightReduction = 0;
    private int _hitModifierByArmor = 0;
    private int _dmgModifierByArmor = 0;
    private int _bowHitModifierByArmor = 0;
    private int _bowDmgModifierByArmor = 0;
    private int _defense_water = 0;
    private int _defense_wind = 0;
    private int _defense_fire = 0;
    private int _defense_earth = 0;
    private int _regist_stun = 0;
    private int _regist_stone = 0;
    private int _regist_sleep = 0;
    private int _regist_freeze = 0;
    private int _regist_sustain = 0;
    private int _regist_blind = 0;
    private boolean _activity;
    private int _addPvpRedux = 0;
    private int _addIgnoreReduction = 0;
    private int _addPvpDmg = 0;
    private int _addBossDmg = 0;
    private int _addBossRedux = 0;
    private int _addDodge = 0;
    private int _addEr = 0;
    private int _addHitup = 0;
    private int _addDmgup = 0;
    private int _addBowHitup = 0;
    private int _addBowDmgup = 0;
    private int _addAdenaRate = 0;
    private int _addDropRate = 0;

    @Override
    public int get_ac() {
        return this._ac;
    }

    public void set_ac(int n) {
        this._ac = n;
    }

    @Override
    public int getDamageReduction() {
        return this._damageReduction;
    }

    public void setDamageReduction(int n) {
        this._damageReduction = n;
    }

    @Override
    public int getWeightReduction() {
        return this._weightReduction;
    }

    public void setWeightReduction(int n) {
        this._weightReduction = n;
    }

    @Override
    public int getHitModifierByArmor() {
        return this._hitModifierByArmor;
    }

    public void setHitModifierByArmor(int n) {
        this._hitModifierByArmor = n;
    }

    @Override
    public int getDmgModifierByArmor() {
        return this._dmgModifierByArmor;
    }

    public void setDmgModifierByArmor(int n) {
        this._dmgModifierByArmor = n;
    }

    @Override
    public int getBowHitModifierByArmor() {
        return this._bowHitModifierByArmor;
    }

    public void setBowHitModifierByArmor(int n) {
        this._bowHitModifierByArmor = n;
    }

    @Override
    public int getBowDmgModifierByArmor() {
        return this._bowDmgModifierByArmor;
    }

    public void setBowDmgModifierByArmor(int n) {
        this._bowDmgModifierByArmor = n;
    }

    public void set_defense_water(int n) {
        this._defense_water = n;
    }

    @Override
    public int get_defense_water() {
        return this._defense_water;
    }

    public void set_defense_wind(int n) {
        this._defense_wind = n;
    }

    @Override
    public int get_defense_wind() {
        return this._defense_wind;
    }

    public void set_defense_fire(int n) {
        this._defense_fire = n;
    }

    @Override
    public int get_defense_fire() {
        return this._defense_fire;
    }

    public void set_defense_earth(int n) {
        this._defense_earth = n;
    }

    @Override
    public int get_defense_earth() {
        return this._defense_earth;
    }

    public void set_regist_stun(int n) {
        this._regist_stun = n;
    }

    @Override
    public int get_regist_stun() {
        return this._regist_stun;
    }

    public void set_regist_stone(int n) {
        this._regist_stone = n;
    }

    @Override
    public int get_regist_stone() {
        return this._regist_stone;
    }

    public void set_regist_sleep(int n) {
        this._regist_sleep = n;
    }

    @Override
    public int get_regist_sleep() {
        return this._regist_sleep;
    }

    public void set_regist_freeze(int n) {
        this._regist_freeze = n;
    }

    @Override
    public int get_regist_freeze() {
        return this._regist_freeze;
    }

    public void set_regist_sustain(int n) {
        this._regist_sustain = n;
    }

    @Override
    public int get_regist_sustain() {
        return this._regist_sustain;
    }

    public void set_regist_blind(int n) {
        this._regist_blind = n;
    }

    @Override
    public int get_regist_blind() {
        return this._regist_blind;
    }

    public void setActivity(boolean bl) {
        this._activity = bl;
    }

    @Override
    public boolean isActivity() {
        return this._activity;
    }

    public void setAddPvpRedux(int n) {
        this._addPvpRedux = n;
    }

    @Override
    public int getAddPvpRedux() {
        return this._addPvpRedux;
    }

    public void setAddIgnoreReduction(int n) {
        this._addIgnoreReduction = n;
    }

    public int getAddIgnoreReduction() {
        return this._addIgnoreReduction;
    }

    public void setAddPvpDmg(int n) {
        this._addPvpDmg = n;
    }

    @Override
    public int getAddPvpDmg() {
        return this._addPvpDmg;
    }

    public void setAddBossDmg(int n) {
        this._addBossDmg = n;
    }

    @Override
    public int getAddBossDmg() {
        return this._addBossDmg;
    }

    public void setAddBossRedux(int n) {
        this._addBossRedux = n;
    }

    @Override
    public int getAddBossRedux() {
        return this._addBossRedux;
    }

    public void setAddDodge(int n) {
        this._addDodge = n;
    }

    public int getAddDodge() {
        return this._addDodge;
    }

    public void setAddEr(int n) {
        this._addEr = n;
    }

    public int getAddEr() {
        return this._addEr;
    }

    public void setAddHitup(int n) {
        this._addHitup = n;
    }

    public int getAddHitup() {
        return this._addHitup;
    }

    public void setAddDmgup(int n) {
        this._addDmgup = n;
    }

    public int getAddDmgup() {
        return this._addDmgup;
    }

    public void setAddBowHitup(int n) {
        this._addBowHitup = n;
    }

    public int getAddBowHitup() {
        return this._addBowHitup;
    }

    public void setAddBowDmgup(int n) {
        this._addBowDmgup = n;
    }

    public int getAddBowDmgup() {
        return this._addBowDmgup;
    }

    public void setAddAdenaRate(int n) {
        this._addAdenaRate = n;
    }

    public int getAddAdenaRate() {
        return this._addAdenaRate;
    }

    public void setAddDropRate(int n) {
        this._addDropRate = n;
    }

    public int getAddDropRate() {
        return this._addDropRate;
    }
}
