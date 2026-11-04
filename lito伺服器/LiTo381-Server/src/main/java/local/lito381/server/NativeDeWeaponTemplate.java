// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Random;
import local.lito381.server.NativeDeItemTemplate;

public class NativeDeWeaponTemplate
extends NativeDeItemTemplate {
    private static final long serialVersionUID = 1L;
    private static Random _random = new Random();
    private int _add_dmg_min = 0;
    private int _add_dmg_max = 0;
    private int _range = 0;
    private int _hitModifier = 0;
    private int _dmgModifier = 0;
    private int _dmgCriChance = 0;
    private double _dmgCri = 0.0;
    private int _dmgMaxChance = 0;
    private boolean _dmgCriEffectEnabled = false;
    private boolean _dmgMaxEffectEnabled = false;
    private int _canbedmg = 0;
    private int _addPvpDmg = 0;
    private int _addIgnoreReduction = 0;
    private int _addPvpRedux = 0;
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

    public void set_add_dmg(int n, int n2) {
        this._add_dmg_min = n;
        this._add_dmg_max = n2;
    }

    @Override
    public int get_add_dmg() {
        int n = 0;
        if (this._add_dmg_min != 0 && this._add_dmg_max != 0) {
            n = this._add_dmg_min + _random.nextInt(this._add_dmg_max - this._add_dmg_min);
        }
        return n;
    }

    @Override
    public int getRange() {
        return this._range;
    }

    public void setRange(int n) {
        this._range = n;
    }

    @Override
    public int getHitModifier() {
        return this._hitModifier;
    }

    public void setHitModifier(int n) {
        this._hitModifier = n;
    }

    @Override
    public int getDmgModifier() {
        return this._dmgModifier;
    }

    public void setDmgModifier(int n) {
        this._dmgModifier = n;
    }

    @Override
    public int getDmgCriChance() {
        return this._dmgCriChance;
    }

    public void setDmgCriChance(int n) {
        this._dmgCriChance = n;
    }

    @Override
    public double getDmgCri() {
        return this._dmgCri;
    }

    public void setDmgCri(double d) {
        this._dmgCri = d;
    }

    @Override
    public int getDmgMaxChance() {
        return this._dmgMaxChance;
    }

    public void setDmgMaxChance(int n) {
        this._dmgMaxChance = n;
    }

    @Override
    public boolean isDmgCriEffectEnabled() {
        return this._dmgCriEffectEnabled;
    }

    public void setDmgCriEffectEnabled(boolean bl) {
        this._dmgCriEffectEnabled = bl;
    }

    @Override
    public boolean isDmgMaxEffectEnabled() {
        return this._dmgMaxEffectEnabled;
    }

    public void setDmgMaxEffectEnabled(boolean bl) {
        this._dmgMaxEffectEnabled = bl;
    }

    @Override
    public int get_canbedmg() {
        return this._canbedmg;
    }

    public void set_canbedmg(int n) {
        this._canbedmg = n;
    }

    public void setAddPvpDmg(int n) {
        this._addPvpDmg = n;
    }

    @Override
    public int getAddPvpDmg() {
        return this._addPvpDmg;
    }

    public void setAddIgnoreReduction(int n) {
        this._addIgnoreReduction = n;
    }

    public int getAddIgnoreReduction() {
        return this._addIgnoreReduction;
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

    public void setAddPvpRedux(int n) {
        this._addPvpRedux = n;
    }

    @Override
    public int getAddPvpRedux() {
        return this._addPvpRedux;
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

    @Override
    public boolean isTwohandedWeapon() {
        int n = this.getType();
        boolean bl = n == 3 || n == 4 || n == 5 || n == 11 || n == 12 || n == 15 || n == 16 || n == 18;
        return bl;
    }
}
