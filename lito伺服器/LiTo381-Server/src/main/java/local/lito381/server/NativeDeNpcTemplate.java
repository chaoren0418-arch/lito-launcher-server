// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeNpcExecutor;
import local.lito381.server.NativeDeNpcTemplateLog;
import local.lito381.server.NativeDeWorldLog;

public class NativeDeNpcTemplate
implements Cloneable,
NativeDeDeletionPorts.Template {
    private static final NativeDeWorldLog _log = NativeDeNpcTemplateLog.getLogger(NativeDeNpcTemplate.class);
    private static final long serialVersionUID = 1L;
    private int _npcid;
    private String _name;
    private String _impl;
    private int _level;
    private int _hp;
    private int _mp;
    private int _ac;
    private int _str;
    private int _con;
    private int _dex;
    private int _wis;
    private int _int;
    private int _mr;
    private int _exp;
    private int _lawful;
    private String _size;
    private int _weakAttr;
    private int _ranged;
    private boolean _agrososc;
    private boolean _agrocoi;
    private boolean _tameable;
    private int _passispeed;
    private int _atkspeed;
    private boolean _agro;
    private int _gfxid;
    private String _nameid;
    private int _undead;
    private int _poisonatk;
    private int _paralysisatk;
    private int _family;
    private int _agrofamily;
    private int _agrogfxid1;
    private int _agrogfxid2;
    private boolean _picupitem;
    private int _digestitem;
    private boolean _bravespeed;
    private int _hprinterval;
    private int _hpr;
    private int _mprinterval;
    private int _mpr;
    private boolean _teleport;
    private int _randomlevel;
    private int _randomhp;
    private int _randommp;
    private int _randomac;
    private int _randomexp;
    private int _randomlawful;
    private int _damagereduction;
    private boolean _hard;
    private boolean _doppel;
    private boolean _tu;
    private boolean _erase;
    private int bowActId = 0;
    private int _karma;
    private int _transformId;
    private int _transformGfxId;
    private int _atkMagicSpeed;
    private int _subMagicSpeed;
    private int _lightSize;
    private boolean _amountFixed;
    private boolean _changeHead;
    private boolean _isCantResurrect;
    private String _classname;
    private NativeDeNpcExecutor _class;
    private boolean _talk = false;
    private boolean _action = false;
    private boolean _attack = false;
    private boolean _death = false;
    private boolean _work = false;
    private boolean _spawn = false;
    private boolean _boss;

    public NativeDeNpcTemplate clone() {
        try {
            return (NativeDeNpcTemplate)super.clone();
        }
        catch (CloneNotSupportedException cloneNotSupportedException) {
            throw new InternalError(cloneNotSupportedException.getMessage());
        }
    }

    public int get_npcId() {
        return this._npcid;
    }

    public void set_npcId(int n) {
        this._npcid = n;
    }

    public String get_name() {
        return this._name;
    }

    public void set_name(String string) {
        this._name = string;
    }

    public String getImpl() {
        return this._impl;
    }

    public void setImpl(String string) {
        this._impl = string;
    }

    public int get_level() {
        return this._level;
    }

    public void set_level(int n) {
        this._level = n;
    }

    public int get_hp() {
        return this._hp;
    }

    public void set_hp(int n) {
        this._hp = n;
    }

    public int get_mp() {
        return this._mp;
    }

    public void set_mp(int n) {
        this._mp = n;
    }

    public int get_ac() {
        return this._ac;
    }

    public void set_ac(int n) {
        this._ac = n;
    }

    public int get_str() {
        return this._str;
    }

    public void set_str(int n) {
        this._str = n;
    }

    public int get_con() {
        return this._con;
    }

    public void set_con(int n) {
        this._con = n;
    }

    public int get_dex() {
        return this._dex;
    }

    public void set_dex(int n) {
        this._dex = n;
    }

    public int get_wis() {
        return this._wis;
    }

    public void set_wis(int n) {
        this._wis = n;
    }

    public int get_int() {
        return this._int;
    }

    public void set_int(int n) {
        this._int = n;
    }

    public int get_mr() {
        return this._mr;
    }

    public void set_mr(int n) {
        this._mr = n;
    }

    public int get_exp() {
        return this._exp;
    }

    public void set_exp(int n) {
        this._exp = n;
    }

    public int get_lawful() {
        return this._lawful;
    }

    public void set_lawful(int n) {
        this._lawful = n;
    }

    public String get_size() {
        return this._size;
    }

    public boolean isSmall() {
        return this._size.equalsIgnoreCase("small");
    }

    public boolean isLarge() {
        return this._size.equalsIgnoreCase("large");
    }

    public void set_size(String string) {
        this._size = string;
    }

    public int get_weakAttr() {
        return this._weakAttr;
    }

    public void set_weakAttr(int n) {
        this._weakAttr = n;
    }

    public int get_ranged() {
        return this._ranged;
    }

    public void set_ranged(int n) {
        this._ranged = n;
    }

    public boolean is_agrososc() {
        return this._agrososc;
    }

    public void set_agrososc(boolean bl) {
        this._agrososc = bl;
    }

    public boolean is_agrocoi() {
        return this._agrocoi;
    }

    public void set_agrocoi(boolean bl) {
        this._agrocoi = bl;
    }

    public boolean isTamable() {
        return this._tameable;
    }

    public void setTamable(boolean bl) {
        this._tameable = bl;
    }

    public int get_passispeed() {
        return this._passispeed;
    }

    public void set_passispeed(int n) {
        this._passispeed = n;
    }

    public int get_atkspeed() {
        return this._atkspeed;
    }

    public void set_atkspeed(int n) {
        this._atkspeed = n;
    }

    public boolean is_agro() {
        return this._agro;
    }

    public void set_agro(boolean bl) {
        this._agro = bl;
    }

    public int get_gfxid() {
        return this._gfxid;
    }

    public void set_gfxid(int n) {
        this._gfxid = n;
    }

    public String get_nameid() {
        return this._nameid;
    }

    public void set_nameid(String string) {
        this._nameid = string;
    }

    public int get_undead() {
        return this._undead;
    }

    public void set_undead(int n) {
        this._undead = n;
    }

    public int get_poisonatk() {
        return this._poisonatk;
    }

    public void set_poisonatk(int n) {
        this._poisonatk = n;
    }

    public int get_paralysisatk() {
        return this._paralysisatk;
    }

    public void set_paralysisatk(int n) {
        this._paralysisatk = n;
    }

    public int get_family() {
        return this._family;
    }

    public void set_family(int n) {
        this._family = n;
    }

    public int get_agrofamily() {
        return this._agrofamily;
    }

    public void set_agrofamily(int n) {
        this._agrofamily = n;
    }

    public int is_agrogfxid1() {
        return this._agrogfxid1;
    }

    public void set_agrogfxid1(int n) {
        this._agrogfxid1 = n;
    }

    public int is_agrogfxid2() {
        return this._agrogfxid2;
    }

    public void set_agrogfxid2(int n) {
        this._agrogfxid2 = n;
    }

    public boolean is_picupitem() {
        return this._picupitem;
    }

    public void set_picupitem(boolean bl) {
        this._picupitem = bl;
    }

    public int get_digestitem() {
        return this._digestitem;
    }

    public void set_digestitem(int n) {
        this._digestitem = n;
    }

    public boolean is_bravespeed() {
        return this._bravespeed;
    }

    public void set_bravespeed(boolean bl) {
        this._bravespeed = bl;
    }

    public int get_hprinterval() {
        return this._hprinterval;
    }

    public void set_hprinterval(int n) {
        this._hprinterval = n / 1000;
    }

    public int get_hpr() {
        return this._hpr;
    }

    public void set_hpr(int n) {
        this._hpr = n;
    }

    public int get_mprinterval() {
        return this._mprinterval;
    }

    public void set_mprinterval(int n) {
        this._mprinterval = n / 1000;
    }

    public int get_mpr() {
        return this._mpr;
    }

    public void set_mpr(int n) {
        this._mpr = n;
    }

    public boolean is_teleport() {
        return this._teleport;
    }

    public void set_teleport(boolean bl) {
        this._teleport = bl;
    }

    public int get_randomlevel() {
        return this._randomlevel;
    }

    public void set_randomlevel(int n) {
        this._randomlevel = n;
    }

    public int get_randomhp() {
        return this._randomhp;
    }

    public void set_randomhp(int n) {
        this._randomhp = n;
    }

    public int get_randommp() {
        return this._randommp;
    }

    public void set_randommp(int n) {
        this._randommp = n;
    }

    public int get_randomac() {
        return this._randomac;
    }

    public void set_randomac(int n) {
        this._randomac = n;
    }

    public int get_randomexp() {
        return this._randomexp;
    }

    public void set_randomexp(int n) {
        this._randomexp = n;
    }

    public int get_randomlawful() {
        return this._randomlawful;
    }

    public void set_randomlawful(int n) {
        this._randomlawful = n;
    }

    public int get_damagereduction() {
        return this._damagereduction;
    }

    public void set_damagereduction(int n) {
        this._damagereduction = n;
    }

    public boolean is_hard() {
        return this._hard;
    }

    public void set_hard(boolean bl) {
        this._hard = bl;
    }

    public boolean is_doppel() {
        return this._doppel;
    }

    public void set_doppel(boolean bl) {
        this._doppel = bl;
    }

    public void set_IsTU(boolean bl) {
        this._tu = bl;
    }

    public boolean get_IsTU() {
        return this._tu;
    }

    public void set_IsErase(boolean bl) {
        this._erase = bl;
    }

    public boolean get_IsErase() {
        return this._erase;
    }

    public int getBowActId() {
        return this.bowActId;
    }

    public void setBowActId(int n) {
        this.bowActId = n;
    }

    public int getKarma() {
        return this._karma;
    }

    public void setKarma(int n) {
        this._karma = n;
    }

    public int getTransformId() {
        return this._transformId;
    }

    public void setTransformId(int n) {
        this._transformId = n;
    }

    public int getTransformGfxId() {
        return this._transformGfxId;
    }

    public void setTransformGfxId(int n) {
        this._transformGfxId = n;
    }

    public int getAtkMagicSpeed() {
        return this._atkMagicSpeed;
    }

    public void setAtkMagicSpeed(int n) {
        this._atkMagicSpeed = n;
    }

    public int getSubMagicSpeed() {
        return this._subMagicSpeed;
    }

    public void setSubMagicSpeed(int n) {
        this._subMagicSpeed = n;
    }

    public int getLightSize() {
        return this._lightSize;
    }

    public void setLightSize(int n) {
        this._lightSize = n;
    }

    public boolean isAmountFixed() {
        return this._amountFixed;
    }

    public void setAmountFixed(boolean bl) {
        this._amountFixed = bl;
    }

    public boolean getChangeHead() {
        return this._changeHead;
    }

    public void setChangeHead(boolean bl) {
        this._changeHead = bl;
    }

    public boolean isCantResurrect() {
        return this._isCantResurrect;
    }

    public void setCantResurrect(boolean bl) {
        this._isCantResurrect = bl;
    }

    public void set_classname(String string) {
        this._classname = string;
    }

    public String get_classname() {
        return this._classname;
    }

    public NativeDeNpcExecutor getNpcExecutor() {
        return this._class;
    }

    public void setNpcExecutor(NativeDeNpcExecutor nativeDeNpcExecutor) {
        try {
            if (nativeDeNpcExecutor == null) {
                return;
            }
            this._class = nativeDeNpcExecutor;
            int n = nativeDeNpcExecutor.type();
            if (n >= 32) {
                this._spawn = true;
                n -= 32;
            }
            if (n >= 16) {
                this._work = true;
                n -= 16;
            }
            if (n >= 8) {
                this._death = true;
                n -= 8;
            }
            if (n >= 4) {
                this._attack = true;
                n -= 4;
            }
            if (n >= 2) {
                this._action = true;
                n -= 2;
            }
            if (n >= 1) {
                this._talk = true;
                --n;
            }
            if (n > 0) {
                _log.error("\u7368\u7acb\u5224\u65b7\u9805\u6578\u7d44\u8a2d\u5b9a\u932f\u8aa4:\u9918\u6578\u5927\u65bc0 NpcId: " + this._npcid);
            }
        }
        catch (Exception exception) {
            _log.error(exception.getLocalizedMessage(), exception);
        }
    }

    public boolean talk() {
        return this._talk;
    }

    public boolean action() {
        return this._action;
    }

    public boolean attack() {
        return this._attack;
    }

    public boolean death() {
        return this._death;
    }

    public boolean work() {
        return this._work;
    }

    public boolean spawn() {
        return this._spawn;
    }

    @Override
    public boolean isBoss() {
        return this._boss;
    }

    public void setBoss(boolean bl) {
        this._boss = bl;
    }
}
