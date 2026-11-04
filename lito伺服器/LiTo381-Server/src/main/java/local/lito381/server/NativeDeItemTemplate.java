// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.Serializable;

public abstract class NativeDeItemTemplate
implements Serializable {
    private static final long serialVersionUID = 1L;
    private int _type2;
    private int _itemId;
    private String _name;
    private String _classname;
    private String _nameId;
    private int _type;
    private int _type1;
    private int _material;
    private int _weight;
    private int _gfxId;
    private int _groundGfxId;
    private int _minLevel;
    private int _itemDescId;
    private int _maxLevel;
    private int _bless;
    public static final int BLESS_NORMAL = 0;
    public static final int BLESS_BLESSED = 1;
    public static final int BLESS_CURSED = 2;
    public static final int BLESS_ILLUSION = 3;
    private boolean _tradable;
    private boolean _cantDelete;
    private boolean _save_at_once;
    private int _maxUseTime = 0;
    private int _foodVolume;
    private int _useType;
    private int _dmgSmall = 0;
    private int _dmgLarge = 0;
    private int[] _mode = null;
    private int[] _setIds = new int[0];
    private int _safeEnchant = 0;
    private boolean _useRoyal = false;
    private boolean _useKnight = false;
    private boolean _useElf = false;
    private boolean _useMage = false;
    private boolean _useDarkelf = false;
    private boolean _useDragonknight = false;
    private boolean _useIllusionist = false;
    private byte _addstr = 0;
    private byte _adddex = 0;
    private byte _addcon = 0;
    private byte _addint = 0;
    private byte _addwis = 0;
    private byte _addcha = 0;
    private int _addhp = 0;
    private int _addmp = 0;
    private int _addhpr = 0;
    private int _addmpr = 0;
    private int _addsp = 0;
    private int _mdef = 0;
    private boolean _isHasteItem = false;
    private int _delay_effect;
    private int _magicDmgModifier = 0;
    private int _addMeleeRedux = 0;
    private int _addBowRedux = 0;
    private int _addStunSuccess = 0;
    private int _addPotionRecovery = 0;
    private int _addPoisonResist = 0;
    private int _addExpRate = 0;
    private int _addPierce = 0;
    private int _magicBow = 0;
    private int _arrowSpr = -1;
    private int _addMagicHit = 0;
    private int _greaterTier = 0;

    public int getType2() {
        return this._type2;
    }

    public void setType2(int n) {
        this._type2 = n;
    }

    public int getItemId() {
        return this._itemId;
    }

    public void setItemId(int n) {
        this._itemId = n;
    }

    public String getName() {
        return this._name;
    }

    public void setName(String string) {
        this._name = string;
    }

    public String getclassname() {
        return this._classname;
    }

    public void setClassname(String string) {
        this._classname = string;
    }

    public String getNameId() {
        return this._nameId;
    }

    public void setNameId(String string) {
        this._nameId = string;
    }

    public int getType() {
        return this._type;
    }

    public void setType(int n) {
        this._type = n;
    }

    public int getType1() {
        return this._type1;
    }

    public void setType1(int n) {
        this._type1 = n;
    }

    public int getMaterial() {
        return this._material;
    }

    public void setMaterial(int n) {
        this._material = n;
    }

    public int getWeight() {
        return this._weight;
    }

    public void setWeight(int n) {
        this._weight = n;
    }

    public int getGfxId() {
        return this._gfxId;
    }

    public void setGfxId(int n) {
        this._gfxId = n;
    }

    public int getGroundGfxId() {
        return this._groundGfxId;
    }

    public void setGroundGfxId(int n) {
        this._groundGfxId = n;
    }

    public int getItemDescId() {
        return this._itemDescId;
    }

    public void setItemDescId(int n) {
        this._itemDescId = n;
    }

    public int getMinLevel() {
        return this._minLevel;
    }

    public void setMinLevel(int n) {
        this._minLevel = n;
    }

    public int getMaxLevel() {
        return this._maxLevel;
    }

    public void setMaxLevel(int n) {
        this._maxLevel = n;
    }

    public static int toPacketBless(int n) {
        if (n == 0) {
            return 1;
        }
        if (n == 1) {
            return 0;
        }
        return n;
    }

    public int getBless() {
        return this._bless;
    }

    public void setBless(int n) {
        this._bless = n;
    }

    public boolean isTradable() {
        return this._tradable;
    }

    public void setTradable(boolean bl) {
        this._tradable = bl;
    }

    public boolean isCantDelete() {
        return this._cantDelete;
    }

    public void setCantDelete(boolean bl) {
        this._cantDelete = bl;
    }

    public boolean isToBeSavedAtOnce() {
        return this._save_at_once;
    }

    public void setToBeSavedAtOnce(boolean bl) {
        this._save_at_once = bl;
    }

    public int getMaxUseTime() {
        return this._maxUseTime;
    }

    public void setMaxUseTime(int n) {
        this._maxUseTime = n;
    }

    public int getFoodVolume() {
        return this._foodVolume;
    }

    public void setFoodVolume(int n) {
        this._foodVolume = n;
    }

    public int getLightRange() {
        int n = 0;
        switch (this._itemId) {
            case 40001: {
                n = 11;
                break;
            }
            case 40002: {
                n = 14;
                break;
            }
            case 40004: {
                n = 22;
                break;
            }
            case 40005: {
                n = 8;
            }
        }
        return n;
    }

    public int getLightFuel() {
        int n = 0;
        switch (this._itemId) {
            case 40001: {
                n = 6000;
                break;
            }
            case 40002: {
                n = 12000;
                break;
            }
            case 40003: {
                n = 12000;
                break;
            }
            case 40004: {
                n = 0;
                break;
            }
            case 40005: {
                n = 600;
            }
        }
        return n;
    }

    public int getUseType() {
        return this._useType;
    }

    public void setUseType(int n) {
        this._useType = n;
    }

    public int getDmgSmall() {
        return this._dmgSmall;
    }

    public void setDmgSmall(int n) {
        this._dmgSmall = n;
    }

    public int getDmgLarge() {
        return this._dmgLarge;
    }

    public void setDmgLarge(int n) {
        this._dmgLarge = n;
    }

    public int[] get_mode() {
        return this._mode;
    }

    public void set_mode(int[] nArray) {
        this._mode = nArray;
    }

    public int getSetId() {
        if (this._setIds != null && this._setIds.length > 0) {
            return this._setIds[0];
        }
        return 0;
    }

    public void setSetIds(String string) {
        if (string == null || string.trim().isEmpty() || string.equals("0")) {
            this._setIds = new int[0];
            return;
        }
        try {
            String[] stringArray = string.split(",");
            this._setIds = new int[stringArray.length];
            for (int i = 0; i < stringArray.length; ++i) {
                this._setIds[i] = Integer.parseInt(stringArray[i].trim());
            }
        }
        catch (Exception exception) {
            this._setIds = new int[0];
        }
    }

    public int[] getAllSetIds() {
        return this._setIds;
    }

    public int get_safeenchant() {
        return this._safeEnchant;
    }

    public void set_safeenchant(int n) {
        this._safeEnchant = n;
    }

    public boolean isUseRoyal() {
        return this._useRoyal;
    }

    public void setUseRoyal(boolean bl) {
        this._useRoyal = bl;
    }

    public boolean isUseKnight() {
        return this._useKnight;
    }

    public void setUseKnight(boolean bl) {
        this._useKnight = bl;
    }

    public boolean isUseElf() {
        return this._useElf;
    }

    public void setUseElf(boolean bl) {
        this._useElf = bl;
    }

    public boolean isUseMage() {
        return this._useMage;
    }

    public void setUseMage(boolean bl) {
        this._useMage = bl;
    }

    public boolean isUseDarkelf() {
        return this._useDarkelf;
    }

    public void setUseDarkelf(boolean bl) {
        this._useDarkelf = bl;
    }

    public boolean isUseDragonknight() {
        return this._useDragonknight;
    }

    public void setUseDragonknight(boolean bl) {
        this._useDragonknight = bl;
    }

    public boolean isUseIllusionist() {
        return this._useIllusionist;
    }

    public void setUseIllusionist(boolean bl) {
        this._useIllusionist = bl;
    }

    public byte get_addstr() {
        return this._addstr;
    }

    public void set_addstr(byte by) {
        this._addstr = by;
    }

    public byte get_adddex() {
        return this._adddex;
    }

    public void set_adddex(byte by) {
        this._adddex = by;
    }

    public byte get_addcon() {
        return this._addcon;
    }

    public void set_addcon(byte by) {
        this._addcon = by;
    }

    public byte get_addint() {
        return this._addint;
    }

    public void set_addint(byte by) {
        this._addint = by;
    }

    public byte get_addwis() {
        return this._addwis;
    }

    public void set_addwis(byte by) {
        this._addwis = by;
    }

    public byte get_addcha() {
        return this._addcha;
    }

    public void set_addcha(byte by) {
        this._addcha = by;
    }

    public int get_addhp() {
        return this._addhp;
    }

    public void set_addhp(int n) {
        this._addhp = n;
    }

    public int get_addmp() {
        return this._addmp;
    }

    public void set_addmp(int n) {
        this._addmp = n;
    }

    public int get_addhpr() {
        return this._addhpr;
    }

    public void set_addhpr(int n) {
        this._addhpr = n;
    }

    public int get_addmpr() {
        return this._addmpr;
    }

    public void set_addmpr(int n) {
        this._addmpr = n;
    }

    public int get_addsp() {
        return this._addsp;
    }

    public void set_addsp(int n) {
        this._addsp = n;
    }

    public int get_mdef() {
        return this._mdef;
    }

    public void set_mdef(int n) {
        this._mdef = n;
    }

    public boolean isHasteItem() {
        return this._isHasteItem;
    }

    public void setHasteItem(boolean bl) {
        this._isHasteItem = bl;
    }

    public boolean isStackable() {
        return false;
    }

    public int get_delayid() {
        return 0;
    }

    public int get_delaytime() {
        return 0;
    }

    public int getMaxChargeCount() {
        return 0;
    }

    public void set_delayEffect(int n) {
        this._delay_effect = n;
    }

    public int get_delayEffect() {
        return this._delay_effect;
    }

    public int get_add_dmg() {
        return 0;
    }

    public int getRange() {
        return 0;
    }

    public int getHitModifier() {
        return 0;
    }

    public int getDmgModifier() {
        return 0;
    }

    public int getDmgCriChance() {
        return 0;
    }

    public double getDmgCri() {
        return 0.0;
    }

    public int getDmgMaxChance() {
        return 0;
    }

    public boolean isDmgCriEffectEnabled() {
        return false;
    }

    public boolean isDmgMaxEffectEnabled() {
        return false;
    }

    public int getMagicDmgModifier() {
        return this._magicDmgModifier;
    }

    public void setMagicDmgModifier(int n) {
        this._magicDmgModifier = n;
    }

    public int get_canbedmg() {
        return 0;
    }

    public boolean isTwohandedWeapon() {
        return false;
    }

    public int get_ac() {
        return 0;
    }

    public int getDamageReduction() {
        return 0;
    }

    public int getWeightReduction() {
        return 0;
    }

    public int getHitModifierByArmor() {
        return 0;
    }

    public int getDmgModifierByArmor() {
        return 0;
    }

    public int getBowHitModifierByArmor() {
        return 0;
    }

    public int getBowDmgModifierByArmor() {
        return 0;
    }

    public int get_defense_water() {
        return 0;
    }

    public int get_defense_fire() {
        return 0;
    }

    public int get_defense_earth() {
        return 0;
    }

    public int get_defense_wind() {
        return 0;
    }

    public int get_regist_stun() {
        return 0;
    }

    public int get_regist_stone() {
        return 0;
    }

    public int get_regist_sleep() {
        return 0;
    }

    public int get_regist_freeze() {
        return 0;
    }

    public int get_regist_sustain() {
        return 0;
    }

    public int get_regist_blind() {
        return 0;
    }

    public boolean isActivity() {
        return false;
    }

    public int getPacketItemDescId() {
        switch (this.getItemId()) {
            case 80027: {
                return this.normalizeSmithDesc(this.getItemDescId(), 6258);
            }
            case 80028: {
                return this.normalizeSmithDesc(this.getItemDescId(), 5212);
            }
            case 80029: {
                return this.normalizeSmithDesc(this.getItemDescId(), 5210);
            }
            case 80030: {
                return this.normalizeSmithDesc(this.getItemDescId(), 6259);
            }
        }
        int n = this.getItemDescId();
        if (n != 0) {
            return n;
        }
        return this.getMagicCatalystType();
    }

    public int getPacketUseType() {
        int n = this.getUseType();
        if (n == 96 || n >= 98) {
            return 26;
        }
        if (n == 97) {
            return 27;
        }
        if (n < 0) {
            return n & 0xFF;
        }
        return n;
    }

    private int normalizeSmithDesc(int n, int n2) {
        if (n == 6528 || n == 6529 || n == 6825) {
            return n2;
        }
        return n != 0 ? n : n2;
    }

    public int getMagicCatalystType() {
        int n = 0;
        switch (this.getItemId()) {
            case 40318: {
                n = 166;
                break;
            }
            case 40319: {
                n = 569;
                break;
            }
            case 40321: {
                n = 837;
                break;
            }
            case 49158: {
                n = 3674;
                break;
            }
            case 49157: {
                n = 3605;
                break;
            }
            case 49156: {
                n = 3606;
            }
        }
        return n;
    }

    public int getAddPvpDmg() {
        return 0;
    }

    public int getAddPvpRedux() {
        return 0;
    }

    public int getAddBossDmg() {
        return 0;
    }

    public int getAddBossRedux() {
        return 0;
    }

    public int getAddMeleeRedux() {
        return this._addMeleeRedux;
    }

    public void setAddMeleeRedux(int n) {
        this._addMeleeRedux = n;
    }

    public int getAddBowRedux() {
        return this._addBowRedux;
    }

    public void setAddBowRedux(int n) {
        this._addBowRedux = n;
    }

    public int getAddStunSuccess() {
        return this._addStunSuccess;
    }

    public void setAddStunSuccess(int n) {
        this._addStunSuccess = n;
    }

    public int getAddPotionRecovery() {
        return this._addPotionRecovery;
    }

    public void setAddPotionRecovery(int n) {
        this._addPotionRecovery = n;
    }

    public int getAddPoisonResist() {
        return this._addPoisonResist;
    }

    public void setAddPoisonResist(int n) {
        this._addPoisonResist = n;
    }

    public int getAddExpRate() {
        return this._addExpRate;
    }

    public void setAddExpRate(int n) {
        this._addExpRate = n;
    }

    public int getAddPierce() {
        return this._addPierce;
    }

    public void setAddPierce(int n) {
        this._addPierce = n;
    }

    public int getMagicBow() {
        return this._magicBow;
    }

    public void setMagicBow(int n) {
        this._magicBow = n;
    }

    public int getArrowSpr() {
        return this._arrowSpr;
    }

    public void setArrowSpr(int n) {
        this._arrowSpr = n;
    }

    public int getAddMagicHit() {
        return this._addMagicHit;
    }

    public void setAddMagicHit(int n) {
        this._addMagicHit = n;
    }

    public int getGreaterTier() {
        return this._greaterTier;
    }

    public void setGreaterTier(int n) {
        this._greaterTier = n;
    }
}
