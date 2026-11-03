package com.lineage.server.templates;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lineage.data.executor.NpcExecutor;
import com.lineage.server.model.L1Object;

public class L1Npc extends L1Object implements Cloneable {

	private static final Logger _log = LogManager.getLogger(L1Npc.class);
	
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	//@Override
	public L1Npc clone() {
		try {
			return (L1Npc) (super.clone());
			
		} catch (final CloneNotSupportedException e) {
			throw (new InternalError(e.getMessage()));
		}
	}

	public L1Npc() {
	}

	private int _npcid;

	public int get_npcId() {
		return this._npcid;
	}

	public void set_npcId(final int i) {
		this._npcid = i;
	}

	private String _name;

	public String get_name() {
		return this._name;
	}

	public void set_name(final String s) {
		this._name = s;
	}

	private String _impl;

	public String getImpl() {
		return this._impl;
	}

	public void setImpl(final String s) {
		this._impl = s;
	}

	private int _level;

	public int get_level() {
		return this._level;
	}

	public void set_level(final int i) {
		this._level = i;
	}

	private int _hp;

	public int get_hp() {
		return this._hp;
	}

	public void set_hp(final int i) {
		this._hp = i;
	}

	private int _mp;

	public int get_mp() {
		return this._mp;
	}

	public void set_mp(final int i) {
		this._mp = i;
	}

	private int _ac;

	public int get_ac() {
		return this._ac;
	}

	public void set_ac(final int i) {
		this._ac = i;
	}

	private int _str;

	public int get_str() {
		return this._str;
	}

	public void set_str(final int i) {
		this._str = i;
	}

	private int _con;

	public int get_con() {
		return this._con;
	}

	public void set_con(final int i) {
		this._con = i;
	}

	private int _dex;

	public int get_dex() {
		return this._dex;
	}

	public void set_dex(final int i) {
		this._dex = i;
	}

	private int _wis;

	public int get_wis() {
		return this._wis;
	}

	public void set_wis(final int i) {
		this._wis = i;
	}

	private int _int;

	public int get_int() {
		return this._int;
	}

	public void set_int(final int i) {
		this._int = i;
	}

	private int _mr;

	public int get_mr() {
		return this._mr;
	}

	public void set_mr(final int i) {
		this._mr = i;
	}

	private int _exp;

	public int get_exp() {
		return this._exp;
	}

	public void set_exp(final int i) {
		this._exp = i;
	}

	private int _lawful;

	public int get_lawful() {
		return this._lawful;
	}

	public void set_lawful(final int i) {
		this._lawful = i;
	}

	private String _size;

	public String get_size() {
		return this._size;
	}

	/**
	 * 是小怪
	 * @return
	 */
	public boolean isSmall() {
		return _size.equalsIgnoreCase("small");
	}

	/**
	 * 是大怪
	 * @return
	 */
	public boolean isLarge() {
		return _size.equalsIgnoreCase("large");
	}

	public void set_size(final String s) {
		this._size = s;
	}

	private int _weakAttr;

	/**
	 * NPC害怕屬性
	 * @return 0.無属性魔法,1.地魔法,2.火魔法,4.水魔法,8.風魔法
	 */
	public int get_weakAttr() {
		return this._weakAttr;
	}

	/**
	 * NPC害怕屬性
	 * @param i 0.無属性魔法,1.地魔法,2.火魔法,4.水魔法,8.風魔法
	 */
	public void set_weakAttr(final int i) {
		this._weakAttr = i;
	}

	private int _ranged;// 攻擊距離

	/**
	 * 攻擊距離
	 * @return
	 */
	public int get_ranged() {
		return this._ranged;
	}

	/**
	 * 攻擊距離
	 * @param i
	 */
	public void set_ranged(final int i) {
		this._ranged = i;
	}

	private boolean _agrososc;

	public boolean is_agrososc() {
		return this._agrososc;
	}

	public void set_agrososc(final boolean flag) {
		this._agrososc = flag;
	}

	private boolean _agrocoi;

	public boolean is_agrocoi() {
		return this._agrocoi;
	}

	public void set_agrocoi(final boolean flag) {
		this._agrocoi = flag;
	}

	private boolean _tameable;

	/**
	 * 可以迷魅
	 * @return
	 */
	public boolean isTamable() {
		return this._tameable;
	}

	public void setTamable(final boolean flag) {
		this._tameable = flag;
	}

	private int _passispeed;

	/**
	 * 移動速度
	 * @return
	 */
	public int get_passispeed() {
		return this._passispeed;
	}

	/**
	 * 移動速度
	 * @param i
	 */
	public void set_passispeed(final int i) {
		this._passispeed = i;
	}

	private int _atkspeed;

	/**
	 * 攻擊速度
	 * @return
	 */
	public int get_atkspeed() {
		return this._atkspeed;
	}

	/**
	 * 攻擊速度
	 * @param i
	 */
	public void set_atkspeed(final int i) {
		this._atkspeed = i;
	}

	private boolean _agro;

	/**
	 * 主動攻擊
	 * @return
	 */
	public boolean is_agro() {
		return this._agro;
	}

	public void set_agro(final boolean flag) {
		this._agro = flag;
	}

	private int _gfxid;

	public int get_gfxid() {
		return this._gfxid;
	}

	public void set_gfxid(final int i) {
		this._gfxid = i;
	}

	private String _nameid;

	public String get_nameid() {
		return this._nameid;
	}

	public void set_nameid(final String s) {
		this._nameid = s;
	}

	private int _undead;// NPC屬性系

	/**
	 * NPC屬性系
	 * @return <BR>
	 * 0:無
	 * 1:不死系
	 * 2:惡魔系
	 * 3:殭屍系
	 * 4:不死系(治療系無傷害/無法使用起死回生)
	 * 5:狼人系
	 * 6:龍系
	 */
	public int get_undead() {
		return this._undead;
	}

	public void set_undead(final int i) {
		this._undead = i;
	}

	private int _poisonatk;

	public int get_poisonatk() {
		return this._poisonatk;
	}

	public void set_poisonatk(final int i) {
		this._poisonatk = i;
	}

	private int _paralysisatk;

	public int get_paralysisatk() {
		return this._paralysisatk;
	}

	public void set_paralysisatk(final int i) {
		this._paralysisatk = i;
	}

	private int _family;

	public int get_family() {
		return this._family;
	}

	public void set_family(final int i) {
		this._family = i;
	}

	private int _agrofamily;// 同族幫忙

	/**
	 * 同族幫忙
	 * @return 0:無 1:幫助同族 1以上:全部NPC幫助
	 */
	public int get_agrofamily() {
		return this._agrofamily;
	}

	/**
	 * 同族幫忙
	 * @param i 0:無 1:幫助同族 1以上:全部NPC幫助
	 */
	public void set_agrofamily(final int i) {
		this._agrofamily = i;
	}

	private int _agrogfxid1;

	public int is_agrogfxid1() {
		return this._agrogfxid1;
	}

	public void set_agrogfxid1(final int i) {
		this._agrogfxid1 = i;
	}

	private int _agrogfxid2;

	public int is_agrogfxid2() {
		return this._agrogfxid2;
	}

	public void set_agrogfxid2(final int i) {
		this._agrogfxid2 = i;
	}

	private boolean _picupitem;

	public boolean is_picupitem() {
		return this._picupitem;
	}

	public void set_picupitem(final boolean flag) {
		this._picupitem = flag;
	}

	private int _digestitem;

	public int get_digestitem() {
		return this._digestitem;
	}

	public void set_digestitem(final int i) {
		this._digestitem = i;
	}

	private boolean _bravespeed;

	public boolean is_bravespeed() {
		return this._bravespeed;
	}

	public void set_bravespeed(final boolean flag) {
		this._bravespeed = flag;
	}

	private int _hprinterval;

	public int get_hprinterval() {
		return this._hprinterval;
	}

	public void set_hprinterval(final int i) {
		this._hprinterval = (i / 1000);
	}

	private int _hpr;

	public int get_hpr() {
		return this._hpr;
	}

	public void set_hpr(final int i) {
		this._hpr = i;
	}

	private int _mprinterval;

	public int get_mprinterval() {
		return this._mprinterval;
	}

	public void set_mprinterval(final int i) {
		this._mprinterval = (i / 1000);
	}

	private int _mpr;

	public int get_mpr() {
		return this._mpr;
	}

	public void set_mpr(final int i) {
		this._mpr = i;
	}

	private boolean _teleport;

	public boolean is_teleport() {
		return this._teleport;
	}

	public void set_teleport(final boolean flag) {
		this._teleport = flag;
	}

	private int _randomlevel;

	public int get_randomlevel() {
		return this._randomlevel;
	}

	public void set_randomlevel(final int i) {
		this._randomlevel = i;
	}

	private int _randomhp;

	public int get_randomhp() {
		return this._randomhp;
	}

	public void set_randomhp(final int i) {
		this._randomhp = i;
	}

	private int _randommp;

	public int get_randommp() {
		return this._randommp;
	}

	public void set_randommp(final int i) {
		this._randommp = i;
	}

	private int _randomac;

	public int get_randomac() {
		return this._randomac;
	}

	public void set_randomac(final int i) {
		this._randomac = i;
	}

	private int _randomexp;

	public int get_randomexp() {
		return this._randomexp;
	}

	public void set_randomexp(final int i) {
		this._randomexp = i;
	}

	private int _randomlawful;

	public int get_randomlawful() {
		return this._randomlawful;
	}

	public void set_randomlawful(final int i) {
		this._randomlawful = i;
	}

	private int _damagereduction;

	public int get_damagereduction() {
		return this._damagereduction;
	}

	public void set_damagereduction(final int i) {
		this._damagereduction = i;
	}

	private boolean _hard;

	public boolean is_hard() {
		return this._hard;
	}

	public void set_hard(final boolean flag) {
		this._hard = flag;
	}

	private boolean _doppel;

	public boolean is_doppel() {
		return this._doppel;
	}

	public void set_doppel(final boolean flag) {
		this._doppel = flag;
	}

	private boolean _tu;

	public void set_IsTU(final boolean i) {
		this._tu = i;
	}

	public boolean get_IsTU() {
		return this._tu;
	}

	private boolean _erase;

	public void set_IsErase(final boolean i) {
		this._erase = i;
	}

	public boolean get_IsErase() {
		return this._erase;
	}

	private int bowActId = 0;

	public int getBowActId() {
		return this.bowActId;
	}

	public void setBowActId(final int i) {
		this.bowActId = i;
	}

	private int _karma;

	public int getKarma() {
		return this._karma;
	}

	public void setKarma(final int i) {
		this._karma = i;
	}

	private int _transformId;// 死亡變身的目標NPCID

	/**
	 * 死亡變身的目標NPCID
	 * @return
	 */
	public int getTransformId() {
		return this._transformId;
	}

	/**
	 * 死亡變身的目標NPCID
	 * @param transformId
	 */
	public void setTransformId(final int transformId) {
		this._transformId = transformId;
	}

	private int _transformGfxId;// 死亡變身的動畫代號

	/**
	 * 死亡變身的動畫代號
	 * @return
	 */
	public int getTransformGfxId() {
		return this._transformGfxId;
	}

	/**
	 * 死亡變身的動畫代號
	 * @param i
	 */
	public void setTransformGfxId(final int i) {
		this._transformGfxId = i;
	}

	private int _atkMagicSpeed;// 有方向魔法速度延遲

	/**
	 * 有方向魔法速度延遲
	 * @return
	 */
	public int getAtkMagicSpeed() {
		return this._atkMagicSpeed;
	}

	/**
	 * 有方向魔法速度延遲
	 * @param atkMagicSpeed
	 */
	public void setAtkMagicSpeed(final int atkMagicSpeed) {
		this._atkMagicSpeed = atkMagicSpeed;
	}

	private int _subMagicSpeed;// 無方向魔法速度延遲

	/**
	 * 無方向魔法速度延遲
	 * @return
	 */
	public int getSubMagicSpeed() {
		return this._subMagicSpeed;
	}

	/**
	 * 無方向魔法速度延遲
	 * @param subMagicSpeed
	 */
	public void setSubMagicSpeed(final int subMagicSpeed) {
		this._subMagicSpeed = subMagicSpeed;
	}

	private int _lightSize;

	public int getLightSize() {
		return this._lightSize;
	}

	public void setLightSize(final int lightSize) {
		this._lightSize = lightSize;
	}

	private boolean _amountFixed;

	/**
	 * 傳回是否受 mapids 資料表中設定的怪物量倍率影響。
	 *
	 * @return 若設定為不受影響則傳回 true
	 */
	public boolean isAmountFixed() {
		return this._amountFixed;
	}

	public void setAmountFixed(final boolean fixed) {
		this._amountFixed = fixed;
	}

	private boolean _changeHead;

	public boolean getChangeHead() {
		return this._changeHead;
	}

	public void setChangeHead(final boolean changeHead) {
		this._changeHead = changeHead;
	}

	private boolean _isCantResurrect;

	/**
	 * 不可以復活
	 * @return true:不允許 false:允許
	 */
	public boolean isCantResurrect() {
		return this._isCantResurrect;
	}

	/**
	 * 設置為不可以復活
	 * @param isCantResurrect
	 */
	public void setCantResurrect(final boolean isCantResurrect) {
		this._isCantResurrect = isCantResurrect;
	}

	private String _classname;// 獨立判斷項名稱

	/**
	 * 獨立判斷項名稱
	 * @param classname
	 */
	public void set_classname(final String classname) {
		this._classname = classname;
	}

	/**
	 * 獨立判斷項名稱
	 * @return
	 */
	public String get_classname() {
		return this._classname;
	}

	private NpcExecutor _class;// 獨立判斷項
	
	/**
	 * 獨立判斷項
	 * @return
	 */
	public NpcExecutor getNpcExecutor() {
		return _class;
	}
	
	/**
	 * 獨立判斷項
	 * @param _class
	 */
	public void setNpcExecutor(NpcExecutor _class) {
		try {
			if (_class == null) {
				return;
			}
			this._class = _class;

			int type = _class.type();

			if (type >= 32) {
				_spawn = true;// NPC召喚
				type -= 32;
			}
			if (type >= 16) {
				_work = true;// NPC工作時間
				type -= 16;
			}
			if (type >= 8) {
				_death = true;// NPC死亡
				type -= 8;
			}
			if (type >= 4) {
				_attack = true;// NPC受到攻擊
				type -= 4;
			}
			if (type >= 2) {
				_action = true;// NPC對話執行
				type -= 2;
			}
			if (type >= 1) {
				_talk = true;// NPC對話判斷
				type -= 1;
			}
			if (type > 0) {
				_log.error("獨立判斷項數組設定錯誤:餘數大於0 NpcId: " + _npcid);
			}
			
		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	private boolean _talk = false;// NPC對話判斷
	
	/**
	 * NPC對話判斷
	 */
	public boolean talk() {
		return _talk;
	}

	private boolean _action = false;// NPC對話執行

	/**
	 * NPC對話執行
	 */
	public boolean action() {
		return _action;
	}

	private boolean _attack = false;// NPC受到攻擊

	/**
	 * NPC受到攻擊
	 */
	public boolean attack() {
		return _attack;
	}

	private boolean _death = false;// NPC死亡

	/**
	 * NPC死亡
	 */
	public boolean death() {
		return _death;
	}

	private boolean _work = false;// NPC工作時間

	/**
	 * NPC工作時間
	 */
	public boolean work() {
		return _work;
	}

	private boolean _spawn = false;// NPC召喚

	/**
	 * NPC召喚
	 */
	public boolean spawn() {
		return _spawn;
	}
	
	private boolean _boss;

	public boolean isBoss() {
		return _boss;
	}

	public void setBoss(final boolean boss) {
		this._boss = boss;
	}
}
