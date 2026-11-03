package com.lineage.server.model;

import static com.lineage.server.model.skill.L1SkillId.ARMOR_BREAK;
import static com.lineage.server.model.skill.L1SkillId.BURNING_SPIRIT;
import static com.lineage.server.model.skill.L1SkillId.DOUBLE_BRAKE;
import static com.lineage.server.model.skill.L1SkillId.ELEMENTAL_FIRE;
import static com.lineage.server.model.skill.L1SkillId.ENCHANT_VENOM;
import static com.lineage.server.model.skill.L1SkillId.IMMUNE_TO_HARM;
import static com.lineage.server.model.skill.L1SkillId.REDUCTION_ARMOR;
import static com.lineage.server.model.skill.L1SkillId.SOUL_OF_FLAME;
import static com.lineage.server.model.skill.L1SkillId.STRIKER_GALE;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lineage.config.ConfigAlt;
import com.lineage.config.ConfigOther;
import com.lineage.config.ConfigPcDamage;
import com.lineage.config.ConfigSkill;
import com.lineage.server.ActionCodes;
import com.lineage.server.model.Instance.L1DeInstance;
import com.lineage.server.model.Instance.L1ItemInstance;
import com.lineage.server.model.Instance.L1NpcInstance;
import com.lineage.server.model.Instance.L1PcInstance;
import com.lineage.server.model.Instance.L1PetInstance;
import com.lineage.server.model.Instance.L1SummonInstance;
import com.lineage.server.model.combat.DamageCommit;
import com.lineage.server.model.combat.DamageEvent;
import com.lineage.server.model.combat.PhysicalHitCommitPlan;
import com.lineage.server.model.poison.L1DamagePoison;
import com.lineage.server.model.proc.ProcAttackResult;
import com.lineage.server.model.proc.UnifiedProcExecutor;
import com.lineage.server.serverpackets.S_AttackPacketPc;
import com.lineage.server.serverpackets.S_ChangeHeading;
import com.lineage.server.serverpackets.S_DoActionGFX;
import com.lineage.server.serverpackets.S_PacketBoxDk;
import com.lineage.server.serverpackets.S_ServerMessage;
import com.lineage.server.serverpackets.S_SkillSound;
import com.lineage.server.serverpackets.S_UseArrowSkill;

/**
 * 物理攻擊判斷項(PC)
 * @author dexc
 *
 */
public class L1AttackPc extends L1AttackMode {

	private static final Logger _log = LogManager.getLogger(L1AttackPc.class);

	// 攻擊模式 0x00:none 0x02:暴擊 0x04:雙擊 0x08:鏡反射
	private byte _attackType = 0x00;
	/** 最大傷害是否已觸發；不可依賴 _attackType，因為資料庫可關閉視覺標記。 */
	private boolean _weaponDmgMaxTriggered;

	/** 倍率傷害：多種同時觸發時只取最高，不連乘 */
	private double _damageMultiplier = 1.0;

	/** D-2：攻擊 Proc Calc 產出（LogicPool calcAttackProcs → SlowPool apply） */
	private ProcAttackResult _attackProcResult = ProcAttackResult.empty();
	private boolean _projectileReservationFailed;
	/** 權威距離檢查失敗；僅供攻擊管線診斷，不改變既有封包與傷害語意。 */
	private boolean _isOutOfRange;
	
	public L1AttackPc(final L1PcInstance attacker, final L1Character target) {
		if (target == null) {
			return;
		}
		
		if (target.isDead()) {
			return;
		}
		
		_pc = attacker;

		if (target instanceof L1PcInstance) {
			_targetPc = (L1PcInstance) target;
			_calcType = PC_PC;

		} else if (target instanceof L1NpcInstance) {
			_targetNpc = (L1NpcInstance) target;
			_calcType = PC_NPC;
		}
		
		// 取得武器資訊
		_weapon = this._pc.getWeapon();
		if (_weapon != null) {
			// 確保 ItemAbility 數值是最新的
			_weapon.ensureAbilityFresh();
			
			_weaponId = _weapon.getItem().getItemId();
			_weaponType = _weapon.getItem().getType1();
			_weaponType2 = _weapon.getItem().getType();
			
			final int _weaponAddHit = _weapon.getItem().getHitModifier();
			final int _weaponAddDmg = _weapon.getItem().getDmgModifier();
			
			// 將參考版本的計算值設置到現有變數
			_weaponHit = _weaponAddHit;
			_weaponDmg = _weaponAddDmg;
			
			// [保留] ItemAbility 系統數值以備其他功能使用
			final com.lineage.server.model.item.L1ItemAbility ability = _weapon.getAbility();
			_weaponBowHit = ability.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.BOW_HIT);
			_weaponBowDmg = ability.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.BOW_DMG);
			
			_weaponSmall = _weapon.getItem().getDmgSmall();
			_weaponLarge = _weapon.getItem().getDmgLarge();
			_weaponRange = _weapon.getItem().getRange();
			_weaponBless = _weapon.getItem().getBless();
			
			// [修正] 強制修正遠距離武器的 Range 設定（Type1 或武器細類）
			if (_weaponType == 20 || _weaponType == 62
					|| _weaponType2 == 4 || _weaponType2 == 13 || _weaponType2 == 10) {
				_weaponRange = -1; // 強制設定為遠距離武器
			}
			
			// _weaponEnchant 不再需要，因為強化加成已包含在 ItemAbility 中
			// 保留此變數以備其他用途（如屬性強化計算），但不再用於命中/傷害計算
			if ((_weaponType != 20) && (_weaponType != 62)) {
				_weaponEnchant = _weapon.getEnchantLevel() - _weapon.get_durability(); // 扣除損傷

			} else {
				_weaponEnchant = _weapon.getEnchantLevel();
			}

			_weaponMaterial = _weapon.getItem().getMaterial();
			if (_weaponType == 20) {// 弓 武器類型:箭取回
				_arrow = _pc.getInventory().getArrow();
				if (_arrow != null) {
					_weaponBless = _arrow.getItem().getBless();
					_weaponMaterial = _arrow.getItem().getMaterial();
				}
			}
			
			if (_weaponType == 62) {// 鐵手甲 武器類型:飛刀取回
				_sting = _pc.getInventory().getSting();
				if (_sting != null) {
					_weaponBless = _sting.getItem().getBless();
					_weaponMaterial = _sting.getItem().getMaterial();
				}
			}
			
			_weaponDmgCriChance = _weapon.getItem().getDmgCriChance();
			_weaponDmgCri = _weapon.getItem().getDmgCri();
			_weaponDmgMaxChance = _weapon.getItem().getDmgMaxChance();
			_weaponDmgCriEffectEnabled = _weapon.getItem().isDmgCriEffectEnabled();
			_weaponDmgMaxEffectEnabled = _weapon.getItem().isDmgMaxEffectEnabled();
			_weaponAttrEnchantKind = _weapon.getAttrEnchantKind();
			_weaponAttrEnchantLevel = _weapon.getAttrEnchantLevel();
		}
		
		// [素質] 傷害補正（保留以備後續使用，但主要計算將在傷害公式中直接使用 CalcStat）
		if (_weaponType == 20) {// 弓 增加敏捷傷害
			_statusDamage = com.lineage.server.utils.CalcStat.calcDexDmg((int) _pc.getDex());
		} else { // 其他則參照 STR 值
			_statusDamage = com.lineage.server.utils.CalcStat.calcStrDmg((int) _pc.getStr());
		}

		_target = target;
		_targetId = target.getId();

		// 傷害顯示座標快照：
		// 若目標是正在移動動畫中的 NPC，使用 FROM 座標（客戶端視覺仍在此）
		// 否則使用當前座標
		if (target instanceof L1NpcInstance npc) {
			final long moveDelta = System.currentTimeMillis() - npc.getLastMoveTimeMs();
			if (moveDelta < L1NpcInstance.MOVE_ANIM_DURATION_MS && npc.getPrevX() != 0) {
				_targetX = npc.getPrevX();
				_targetY = npc.getPrevY();
			} else {
				_targetX = target.getX();
				_targetY = target.getY();
			}
		} else {
			_targetX = target.getX();
			_targetY = target.getY();
		}
	}

	/** C-3：建構是否有效（target 非 dead/null 且 calcType 已設定）。 */
	public boolean isValid() {
		return _pc != null && _target != null && _calcType != 0;
	}

	/** 最終命中結果；供同一伺服器攻擊順序線記錄診斷。 */
	public boolean isHitResult() {
		return _isHit;
	}

	/** 攻擊是否被權威 LOS／地形阻擋。 */
	public boolean isTerrainBlockedResult() {
		return _isTerrainBlocked;
	}

	/** 攻擊是否被權威距離檢查阻擋。 */
	public boolean isOutOfRangeResult() {
		return _isOutOfRange;
	}

	/** BIN/資料庫武器射程原值；-1 代表遠距離武器。 */
	public int getWeaponRangeForDiagnostics() {
		return _weaponRange;
	}

	/**
	 * 命中判定
	 */
	//@Override
	public boolean calcHit() {
		if (this._target == null) {// 物件遺失
			this._isHit = false;
			return this._isHit;
		}
		if (com.lineage.server.model.L1DeathMatch.isPeaceCombatBlocked(this._pc, this._target)) {
			this._isHit = false;
			return false;
		}
		
		if (this._weaponRange != -1) {
			
			// 近距離武器攻擊距離判斷
			final int location = 
				this._pc.getLocation().getTileLineDistance(this._target.getLocation());
			
			if (location > (this._weaponRange + 1)) {
				this._isHit = false; // 射程範囲外
				this._isOutOfRange = true;
				return this._isHit;
			}
			
		} else {
			// 遠距離武器攻擊距離判斷
			if (!this._pc.getLocation().isInScreen(this._target.getLocation())) {
				this._isHit = false; // 射程範囲外
				this._isOutOfRange = true;
				return this._isHit;
			}
		}
		
		// 魔法弓可在無箭時生成魔法箭；一般弓必須具有實體箭矢。
		if (this._weaponType == 20) {
			if ((this._weapon == null || !this._weapon.isMagicBow()) && (this._arrow == null)) {
				this._isHit = false; // 持弓 無箭
				return this._isHit;
			}
		} else if ((this._weaponType == 62) && (this._sting == null)) {
			this._isHit = false; // 持鐵手甲 無飛刀
			return this._isHit;
		}
		
		if (!this._pc.glanceCheck(this._target.getX(), this._target.getY())) {
			this._isHit = false;
			this._isTerrainBlocked = true; // 攻擊路徑被地形阻擋，區別於戰鬥 MISS
			return false;
		}

		if ((this._weaponId == 247) || (this._weaponId == 248) || (this._weaponId == 249)) {
			this._isHit = false; // 試煉武器
			return this._isHit;
		}

		if (this._calcType == PC_PC) {
			this._isHit = this.calcPcHit();// PC TO PC

		} else if (this._calcType == PC_NPC) {
			// =======================================================
			// [新增] 假人安全區保護邏輯 (在此攔截)
			// =======================================================
			if (this._targetNpc instanceof com.lineage.server.model.Instance.L1DeInstance) {
				// 如果 攻擊者 或 目標 任一方在安全區
				if (this._pc.getMap().isSafetyZone(this._pc.getLocation())
						|| this._targetNpc.getMap().isSafetyZone(this._targetNpc.getLocation())) {
					
					this._isHit = false; // 設定為「未命中」
					return this._isHit;  // 直接回傳失敗，不進入 calcNpcHit (不扣血)
				}
			}
			// =======================================================

			this._isHit = this.calcNpcHit();// PC TO NPC
		}
		
		return this._isHit;
	}
	
	/**
	 * PC對PC的命中
	 * @return
	 */
	private boolean calcPcHit() {
		if (_targetPc == null) {
			return false;
		}
		
		// 傷害為0
		if (dmg0(_targetPc)) {
			return false;
		}

		// 迴避攻擊
		if (calcEvasion()) {
			return false;
		}

		if (_weaponType2 == 17) {// 奇古獸
			return true;
		}
		
		_hitRate = _pc.getLevel();
		_hitRate += _pc.getClassFeature().getHitLevel(_pc.getLevel());// 職業物理命中補正
		
		if ((_weaponType != 20) && (_weaponType != 62)) {
			// [近戰命中公式]
			// [素質] 力量命中補正
			_hitRate += com.lineage.server.utils.CalcStat.calcStrHit((int) _pc.getStr());
			// [武器] 武器命中 + 強化加成
			_hitRate += (_weaponHit + _pc.getHitup() + _pc.getOriginalHitup() + (_weaponEnchant * 0.6));
			// [防具] 額外命中
			_hitRate += _pc.getHitModifierByArmor();
			
		} else {
			// [遠攻命中公式] - 修正：恢復強化值計算
			// [素質] 敏捷命中補正
			_hitRate += com.lineage.server.utils.CalcStat.calcDexHit((int) _pc.getDex());
			// [武器] 武器命中 + 強化加成（修正關鍵問題）
			_hitRate += (_weaponHit + _pc.getBowHitup() + _pc.getOriginalBowHitup() + (_weaponEnchant * 0.6));
			// [防具] 遠攻額外命中
			_hitRate += _pc.getBowHitModifierByArmor();
		}
		
		final int weight240 = _pc.getInventory().getWeight240();
		if (weight240 > 80) { // 重量命中修正
			if ((80 < weight240) && (120 >= weight240)) {
				_hitRate -= 1;

			} else if ((121 <= weight240) && (160 >= weight240)) {
				_hitRate -= 3;

			} else if ((161 <= weight240) && (200 >= weight240)) {
				_hitRate -= 5;
			}
		}

		_hitRate += hitUp();

		if (_pc.is_mazu()) {// 媽祖祝福
			_hitRate += 1;
		}
		
		int attackerDice = ThreadLocalRandom.current().nextInt(20) + 2 + _hitRate - 10;

		// 技能增加閃避
		attackerDice += attackerDice(_targetPc);
		
		int defenderDice = 0;

		final int defenderValue = (int) (_targetPc.getAc() * 1.5) * -1;

		if (_targetPc.getAc() >= 0) {
			defenderDice = 10 - _targetPc.getAc();

		} else if (_targetPc.getAc() < 0) {
			defenderDice = 10 + ThreadLocalRandom.current().nextInt(defenderValue) + 1;
		}

		final int fumble = _hitRate - 9;
		final int critical = _hitRate + 10;
		if (_pc.isDragonKnight()) {
			attackerDice *= 1.01;
		}
		if (_pc.isElf()) {
			if ((_pc.getElfAttr() & 2) != 0) { // 具火屬性加成（位元遮罩，雙屬性亦適用）
				attackerDice *= 1.02;
			}
		}

		if (attackerDice <= fumble) {
			_hitRate = 15;

		} else if (attackerDice >= critical) {
			_hitRate = 100;

		} else {
			if (attackerDice > defenderDice) {
				_hitRate = 100;
				
			} else if (attackerDice <= defenderDice) {
				_hitRate = 15;
			}
		}

		final int rnd = ThreadLocalRandom.current().nextInt(100) + 1;
		if (_weaponType == 20) {// 弓 附加ER計算
			if (_hitRate > rnd) {
				return calcErEvasion();
			}
		}
		return _hitRate >= rnd;
	}

	/**
	 * PC對NPC的命中
	 * @return
	 */
	private boolean calcNpcHit() {
		// 對不可見的怪物額外判斷
		final int gfxid = this._targetNpc.getNpcTemplate().get_gfxid();
		switch (gfxid) {
		case 2412:// 南瓜的影子
			if (!_pc.getInventory().checkEquipped(20046)) {// 南瓜帽
				return false;
			}
			break;
		}
		
		// 傷害為0
		if (dmg0(_targetNpc)) {
			return false;
		}
		
		// NPC商店模式
		if (this._targetNpc.isShop()) {
			// [修正] 如果是假人，直接放行 (不論 Type 0,1,2...)
			// 因為我們已經在上面 calcHit 做了安全區保護，這裡可以放心開放
			if (this._targetNpc instanceof L1DeInstance) {
				// Pass: 繼續往下執行命中率計算
			} else {
				return false; // 一般正規 NPC (潘朵拉等) 維持保護
			}
		}

		if (_weaponType2 == 17) {// 奇古獸 命中100%
			return true;
		}
		
		// 對 NPC 命中率
		// ＝（PC 等級＋職業補正＋STR 補正＋DEX 補正＋武器補正＋DAI 數量/2＋魔法補正）×5−{NPC AC×（-5）}
		_hitRate = _pc.getLevel();

		if ((_weaponType != 20) && (_weaponType != 62)) {
			// [近戰命中公式]
			// [素質] 力量命中補正
			_hitRate += com.lineage.server.utils.CalcStat.calcStrHit((int) _pc.getStr());
			// [武器] 武器命中 + 強化加成
			_hitRate += (_weaponHit + _pc.getHitup() + _pc.getOriginalHitup() + (_weaponEnchant * 0.6));
			// [防具] 額外命中
			_hitRate += _pc.getHitModifierByArmor();

		} else {
			// [遠攻命中公式] - 修正：恢復強化值計算
			// [素質] 敏捷命中補正
			_hitRate += com.lineage.server.utils.CalcStat.calcDexHit((int) _pc.getDex());
			// [武器] 武器命中 + 強化加成（修正關鍵問題）
			_hitRate += (_weaponHit + _pc.getBowHitup() + _pc.getOriginalBowHitup() + (_weaponEnchant * 0.6));
			// [防具] 遠攻額外命中
			_hitRate += _pc.getBowHitModifierByArmor();
		}

		final int weight240 = _pc.getInventory().getWeight240();
		if (weight240 > 80) { // 重量命中修正
			if ((80 < weight240) && (120 >= weight240)) {
				_hitRate -= 1;

			} else if ((121 <= weight240) && (160 >= weight240)) {
				_hitRate -= 3;

			} else if ((161 <= weight240) && (200 >= weight240)) {
				_hitRate -= 5;
			}
		}
		
		_hitRate += hitUp();

		if (_pc.is_mazu()) {// 媽祖祝福
			_hitRate += 1;
		}

		int attackerDice = ThreadLocalRandom.current().nextInt(20) + 2 + _hitRate - 10;

		// 技能增加閃避
		attackerDice += attackerDice(_targetNpc);
		
		final int defenderDice = 10 - _targetNpc.getAc();

		final int fumble = _hitRate - 9;
		final int critical = _hitRate + 10;
		if (_pc.isDragonKnight()) {
			attackerDice *= 1.01;
		}
		if (_pc.isElf()) {
			if ((_pc.getElfAttr() & 2) != 0) { // 具火屬性加成（位元遮罩，雙屬性亦適用）
				attackerDice *= 1.02;
			}
		}
		
		if (attackerDice <= fumble) {
			_hitRate = 15;

		} else if (attackerDice >= critical) {
			_hitRate = 100;

		} else {
			if (attackerDice > defenderDice) {
				_hitRate = 100;
				
			} else if (attackerDice <= defenderDice) {
				_hitRate = 15;
			}
		}

		final int npcId = _targetNpc.getNpcTemplate().get_npcId();
		
		final Integer tgskill = L1AttackList.SKNPC.get(npcId);
		if (tgskill != null) {
			if (!_pc.hasSkillEffect(tgskill)) {
				_hitRate = 0;
			}
		}

		final Integer tgpoly = L1AttackList.PLNPC.get(npcId);
		if (tgpoly != null) {
			if (tgpoly.equals(_pc.getTempCharGfx())) {
				_hitRate = 0;
			}
		}

		final int rnd = ThreadLocalRandom.current().nextInt(100) + 1;

		return _hitRate >= rnd;
	}

	/**
	 * 追加命中
	 * @return
	 */
	private int hitUp() {
		final boolean ranged = (_weaponType == 20) || (_weaponType == 62);
		return _pc.getCombatHitBonus(ranged);
	}

	/**
	 * 奇古獸傷害計算 (修正版)
	 * 公式: (INT + SP + 魔法命中) * 1.8 + 隨機(武器傷害) + 強化值
	 */
	private int calcKiringkuDamage() {
		int damage = 0;
		
		// 1. 基礎魔法傷害 (INT + SP)
		// [修正] 直接使用 getInt() 與 getSp()，不需透過 getAbility()
		int magicBonus = _pc.getInt() + _pc.getSp();
		
		// 2. 加入裝備的魔法命中 (之前新增的屬性)
		magicBonus += _pc.getMagicHit(); 
		
		// 3. 計算係數 (1.8倍)
		damage += (int) (magicBonus * 1.8);
		
		// 4. 武器傷害與強化
		if (_weapon != null) {
			damage += ThreadLocalRandom.current().nextInt(_weapon.getItem().getDmgSmall()) + 1;
			damage += _weapon.getEnchantLevel();
		}
		
		return damage;
	}
	
	/**
	 * 傷害計算
	 */
	//@Override
	public int calcDamage() {
		switch (_calcType) {
		case PC_PC:
			_damage = calcPcDamage();
			break;

		case PC_NPC:
			_damage = calcNpcDamage();
			break;
		}
		if (_damage > 0) {
			int outcount = 0;
			// 特殊魔法武器
			if (_weapon != null) {
			}
			if (outcount > 1) {
				_damage /= outcount;
			}
		}
		return _damage;
	}

	/**
	 * 傷害質初始化
	 * @param weaponMaxDamage 可發出的最大攻擊質
	 * @return
	 */
	/**
	 * 傷害質初始化 (完全外控最大值版本)
	 * @param weaponMaxDamage 可發出的最大攻擊質
	 * @return
	 */
	private int weaponDamage1(int weaponMaxDamage) {
		int weaponDamage = 0;
		
		switch (_weaponType2) {
		case 0:// 空手
			break;

		case 4:// 弓
		case 13:// 弓(單手)
		case 10:// 鐵手甲
			// 弓、鐵手甲的浮動傷害在 weaponDamage2 由箭矢/飛刀產生，此處僅做暴擊判定
			if (_weaponDmgMaxChance > 0 && (ThreadLocalRandom.current().nextInt(100) + 1) <= _weaponDmgMaxChance) {
				_weaponDmgMaxTriggered = true;
				if (_weaponDmgMaxEffectEnabled) {
					_attackType = 0x02;
				}
			}
			break;

		case 11:// 鋼爪
			// 鋼爪基礎依然為 0，全靠後面的暴擊判定給予傷害
			weaponDamage = 0;
			break;

		case 15:// 雙手斧
		case 1:// 劍
		case 2:// 匕首
		case 3:// 雙手劍
		case 5:// 矛(雙手)
		case 6:// 斧(單手)
		case 7:// 魔杖
		case 8:// 飛刀
		case 9:// 箭
		case 12:// 雙刀
		case 14:// 矛(單手)
		case 16:// 魔杖(雙手)
		case 17:// 奇古獸
		case 18:// 鎖鏈劍
			// 【核心修改】判斷是否有外控設定
			if (_weaponDmgMaxChance > 0) {
				// 有外控機率時，先判定是否觸發暴擊
				if ((ThreadLocalRandom.current().nextInt(100) + 1) <= _weaponDmgMaxChance) {
					weaponDamage = weaponMaxDamage;
					_weaponDmgMaxTriggered = true;
					if (_weaponDmgMaxEffectEnabled) {
						_attackType = 0x02;
					}
				} else {
					// 沒觸發暴擊時，嚴格限制傷害只能在 1 ~ (最大值-1) 之間
					if (weaponMaxDamage > 1) {
						weaponDamage = ThreadLocalRandom.current().nextInt(weaponMaxDamage - 1) + 1;
					} else {
						weaponDamage = 1; // 避免最大值只有 1 的武器報錯
					}
				}
			} else {
				// 【防呆機制】如果 DB 的 dmg_max_chance 填 0，代表不啟用外控
				// 退回傳統天堂的自然機率 (1 ~ max) 隨機骰，但不顯示暴擊特效
				weaponDamage = ThreadLocalRandom.current().nextInt(weaponMaxDamage) + 1;
			}
			break;
		}

		// 鋼爪專用的外控暴擊判定 (因為鋼爪在 switch 裡預設是 0)
		if (_weaponType2 == 11 && _weaponDmgMaxChance > 0) {
			if ((ThreadLocalRandom.current().nextInt(100) + 1) <= _weaponDmgMaxChance) {
				weaponDamage = weaponMaxDamage;
				_weaponDmgMaxTriggered = true;
				if (_weaponDmgMaxEffectEnabled) {
					_attackType = 0x02;
				}
			}
		}

		// 烈焰之魂 (175)：有技能時每下強制最大值傷害
		if (_pc.hasSkillEffect(SOUL_OF_FLAME) && weaponMaxDamage > 0) {
			weaponDamage = weaponMaxDamage;
		}
		
		switch (_pc.guardianEncounter()) {
		case 3:// 邪惡的守護 Lv.1
			weaponDamage += 1;
			break;
			
		case 4:// 邪惡的守護 Lv.2
			weaponDamage += 3;
			break;
			
		case 5:// 邪惡的守護 Lv.3
			weaponDamage += 5;
			break;
		}
		return weaponDamage;
	}

	/**
	 * 傷害質最終計算
	 * @param weaponTotalDamage
	 * @return
	 */
	private double weaponDamage2(double weaponTotalDamage) {
		double dmg = 0.0;
		boolean doubleBrake = false;// 技能(雙重破壞)
		switch (_weaponType2) {
		case 1:// 劍
		case 2:// 匕首
		case 3:// 雙手劍
		case 5:// 矛(雙手)
		case 6:// 斧(單手)
		case 7:// 魔杖
		case 8:// 飛刀
		case 9:// 箭
		case 14:// 矛(單手)
		case 15:// 雙手斧
		case 16:// 魔杖(雙手)
		case 18:// 鎖鏈劍
			// weaponTotalDamage 已包含武器傷害、防具傷害、素質傷害等，直接使用
			dmg = weaponTotalDamage;
			break;

		case 11:// 鋼爪
			// 技能(雙重破壞)
			if (_pc.hasSkillEffect(DOUBLE_BRAKE)) {
				doubleBrake = true;
			}
			dmg = weaponTotalDamage;
			dmg += 5.0;
			break;
			
		case 12:// 雙刀
			// 技能(雙重破壞)
			if (_pc.hasSkillEffect(DOUBLE_BRAKE)) {
				doubleBrake = true;
			}
			dmg = weaponTotalDamage;
			break;
			
		case 0:// 空手
			dmg = (ThreadLocalRandom.current().nextInt(5) + 4) >> 2;// / 4;
			break;
			
		case 4:// 弓
		case 13:// 弓(單手)
			// weaponTotalDamage 已包含武器傷害、防具傷害、素質傷害等
			// 但遠距離武器的素質傷害需要額外倍率計算
			final int dexDmgForBow = com.lineage.server.utils.CalcStat.calcDexDmg((int) _pc.getDex());
			double add = dexDmgForBow;
			switch (_calcType) {
			case PC_PC:
				add *= 2.5D;
				break;

			case PC_NPC:
				add *= 1.5D;
				break;
			}
			// 移除 weaponTotalDamage 中已包含的 dexDmg，加上倍率後的版本
			dmg = weaponTotalDamage - dexDmgForBow + add;
			if (_arrow != null) {
				final int add_dmg = resolveProjectileDamage(_arrow, isLargeNpcTarget());
				dmg += _weaponDmgMaxTriggered ? add_dmg : (ThreadLocalRandom.current().nextInt(add_dmg) + 1);
			} else {
				// 魔法弓即使沒有箭矢也能造成傷害（固定 1-15 傷害）
				if (_weapon != null && _weapon.isMagicBow()) {
					dmg += _weaponDmgMaxTriggered ? 15 : (ThreadLocalRandom.current().nextInt(15) + 1);
				}
			}
			break;

		case 10:// 鐵手甲
			// weaponTotalDamage 已包含武器傷害、防具傷害、素質傷害等，直接使用
			dmg = weaponTotalDamage;
			if (_sting != null) {
				final int add_dmg = resolveProjectileDamage(_sting, isLargeNpcTarget());
				dmg += _weaponDmgMaxTriggered ? add_dmg : (ThreadLocalRandom.current().nextInt(add_dmg) + 1);
			}
			break;

		case 17:// 奇古獸
			// 奇古獸傷害 = calcKiringkuDamage * 0.9
			dmg += (calcKiringkuDamage() * 0.9);
			// [MR魔防減傷] 僅對玩家目標套用 ConfigPcDamage.MR_REDUCTION_PER_10
			if (_targetPc != null) {
				final int targetMr = _targetPc.getMr();
				// 減傷百分比係數 = (目標MR / 10) * MR_REDUCTION_PER_10 / 100
				// 範例: MR 100, 係數 3.0 => (100/10)*3/100 = 0.30 => 30% 減傷
				final double reductionCoeff = Math.min(1.0,
						(targetMr / 10.0) * ConfigPcDamage.MR_REDUCTION_PER_10 / 100.0);
				final double rawDmg = dmg;
				dmg *= (1.0 - reductionCoeff);
				dmg = Math.max(0.0, dmg);
				if (_pc.isGm() && ConfigAlt.ALT_ATKMSG) {
					final StringBuilder debug = new StringBuilder();
					debug.append("【奇古獸MR減傷】原始:").append(String.format("%.0f", rawDmg));
					debug.append(" | 目標MR:").append(targetMr);
					debug.append(" | 減傷:").append(String.format("%.1f%%", reductionCoeff * 100));
					debug.append(" → 最終:").append(String.format("%.0f", dmg));
					_pc.sendPackets(new S_ServerMessage(166, debug.toString()));
				}
			}
			break;
		}

		// 技能(雙重破壞) (黑暗妖精_技能設定表) - 記錄倍率，多種觸發時取最高
		if (doubleBrake) {
			if ((ThreadLocalRandom.current().nextInt(100) + 1) <= ConfigSkill.DOUBLE_BREAK_CHANCE) {
				_damageMultiplier = Math.max(_damageMultiplier, ConfigSkill.DOUBLE_BREAK_DMG);
			}
		}

		// 統一雙擊檢查：倍率傷害一定生效，0x04 標記由 dmg_cri_effect 控制。
		if (_weaponDmgCriChance > 0 && _weaponDmgCri > 0 && (ThreadLocalRandom.current().nextInt(100) + 1) <= _weaponDmgCriChance) {
			_damageMultiplier = Math.max(_damageMultiplier, _weaponDmgCri);
			if (_weaponDmgCriEffectEnabled) {
				_attackType = 0x04;
			}
		}
		
		if (_weaponType2 != 0) {
			final int weapon_add_dmg = _weapon.getItem().get_add_dmg(); // 建議改名避免混淆，但修復 switch 後原本的 add_dmg 應該也能過
			if (weapon_add_dmg != 0) {
				dmg += weapon_add_dmg;
			}
		}

		return dmg;
	} // 方法結束

	private boolean isLargeNpcTarget() {
		return _targetNpc != null && _targetNpc.getNpcTemplate() != null
				&& _targetNpc.getNpcTemplate().isLarge();
	}

	static int resolveProjectileDamage(final L1ItemInstance projectile, final boolean largeTarget) {
		if (projectile == null || projectile.getItem() == null) {
			return 1;
		}
		final int configuredDamage = largeTarget
				? projectile.getItem().getDmgLarge()
				: projectile.getItem().getDmgSmall();
		return Math.max(configuredDamage, 1);
	}

	/**
	 * PC基礎傷害提昇計算
	 * @param dmg
	 * @param weaponTotalDamage
	 * @return
	 */
	private double pcDmgMode(double dmg, double weaponTotalDamage) {
		
		dmg = calcBuffDamage(dmg);

		dmg += _pc.getClassFeature().getAttackLevel(_pc.getLevel());// 職業傷害補正
		
		addPcPoisonAttack(_target);

		// [遷移] 屬性武器機率型 Proc 已移至 UnifiedProcExecutor（AttrWeaponProcAction），於 commit 時觸發
		
		// 注意：防具傷害已在 weaponTotalDamage 中計算，此處不再重複添加
		// if ((_weaponType != 20) && (_weaponType != 62)) { // 防具追加傷害
		// 	dmg += _pc.getDmgModifierByArmor();
		// } else {
		// 	dmg += _pc.getBowDmgModifierByArmor();
		// }

		dmg += dmgUp();
		dmg += dk_dmgUp();
		switch (_pc.get_weaknss()) {
		case 1:
		case 2:
			if (_pc.isFoeSlayer()) {
				_pc.set_weaknss(0, 0);
				_pc.sendPackets(new S_PacketBoxDk(0));
			}
			break;
		case 3:
			if (_pc.isFoeSlayer()) {
				_pc.set_weaknss(0, 0);
				_pc.sendPackets(new S_PacketBoxDk(0));
				_damageMultiplier = Math.max(_damageMultiplier, 1.3D); // Lv3 屠宰終結
			}
			break;
		}
		if (_pc.double_dmg() != 0) {
			if ((ThreadLocalRandom.current().nextInt(1000) + 1) <= _pc.double_dmg()) {
				_damageMultiplier = Math.max(_damageMultiplier, 1.8D);
			}
		}
		// 三重矢傷害倍率 (妖精_技能設定表) - 取最高倍率
		if (_pc.getTripleArrowHitsRemaining() > 0 && (_weaponType2 == 4 || _weaponType2 == 13)) {
			_damageMultiplier = Math.max(_damageMultiplier, ConfigSkill.TRIPLE_ARROW_DMG);
			_pc.setTripleArrowHitsRemaining(_pc.getTripleArrowHitsRemaining() - 1);
		}
		// 多種倍率同時觸發時，只套用最高倍率（不連乘）
		dmg *= _damageMultiplier;
		return dmg;
	}

	/**
	 * PC對PC傷害計算
	 */
	public int calcPcDamage() {
		if (_targetPc == null) {
			return 0;
		}

		_damageMultiplier = 1.0; // 每輪傷害計算前重置
		
		// 傷害為0
		if (dmg0(_targetPc)) {
			_isHit = false;
			_drainHp = 0;
			return 0;
		}

		if (!_isHit) {
			return 0;
		}
		
		final int weaponMaxDamage = _weaponSmall;

		// 傷害直初始化
		int weaponDamage = weaponDamage1(weaponMaxDamage);

		if (_pc.is_mazu()) {// 媽祖祝福
			weaponDamage += 1;
		}

		// 傷害計算：根據武器類型使用對應的 DMG 值
		double weaponTotalDamage;
		if ((_weaponType != 20) && (_weaponType != 62)) {
			// [近戰傷害公式]
			// [素質] 力量傷害補正
			final int strDmg = com.lineage.server.utils.CalcStat.calcStrDmg((int) _pc.getStr());
			// [武器] 浮動傷害值
			// [武器] 通用傷害（已包含基礎值、額外值、強化加成）
			weaponTotalDamage = weaponDamage + _weaponDmg + _weaponEnchant;
			// [防具/魔法] 傷害加成
			weaponTotalDamage += _pc.getDmgup() + _pc.getOriginalDmgup();
			// [防具] 額外傷害
			weaponTotalDamage += _pc.getDmgModifierByArmor();
			// [素質] 加上力量傷害補正
			weaponTotalDamage += strDmg;
		} else {
			// [遠攻傷害公式] - 修正：恢復強化值計算以匹配參考版本
			// [素質] 敏捷傷害補正
			final int dexDmg = com.lineage.server.utils.CalcStat.calcDexDmg((int) _pc.getDex());
			// [武器] 浮動傷害值 + 武器追加傷害 + 強化值（修正關鍵問題）
			weaponTotalDamage = weaponDamage + _weaponDmg + _weaponEnchant;
			// [防具/魔法] 遠攻傷害加成
			weaponTotalDamage += _pc.getBowDmgup() + _pc.getOriginalBowDmgup();
			// [防具] 遠攻額外傷害
			weaponTotalDamage += _pc.getBowDmgModifierByArmor();
			// [素質] 加上敏捷傷害補正
			weaponTotalDamage += dexDmg;
		}

		weaponTotalDamage += calcAttrEnchantDmg(); // 屬性強化

		// 傷害直最終計算
		double dmg = weaponDamage2(weaponTotalDamage);

		// PC基礎傷害提昇計算
		dmg = pcDmgMode(dmg, weaponTotalDamage);

		// [PVP] 應用攻擊者的 PVP 傷害加成
		dmg += _pc.getPvpDmg();

		dmg -= calcPcDefense();// 被攻擊者防禦力傷害減低

		// [AC 額外減傷] AC < 0 時依設定獲得額外減傷
		if (ConfigPcDamage.AC_REDUCTION_DIVISOR > 0 && _targetPc != null && _targetPc.getAc() < 0) {
			final int acReduction = (int) (Math.abs(_targetPc.getAc()) / ConfigPcDamage.AC_REDUCTION_DIVISOR);
			dmg -= acReduction;
			dmg = Math.max(0.0, dmg);
		}

		// [無視減免] 計算有效減免值
		// 取得目標的原始減免值
		int targetDamageReduction = _targetPc.getDamageReductionByArmor();
		// 取得攻擊者的無視減免值
		int attackerIgnoreReduction = _pc.getIgnoreReduction();
		// 計算有效減免值：原始減免 - 無視減免，最低為 0
		int effectiveRedux = Math.max(0, targetDamageReduction - attackerIgnoreReduction);
		dmg -= effectiveRedux;

		// [新增] 遠/近 傷害減免 (僅限 PC 目標)
		if (_targetPc != null) {
			int reduction = 0;
			// 判斷是否為遠距離武器 (弓=20, 鐵手甲=62)
			if (_weaponType == 20 || _weaponType == 62) {
				reduction = _targetPc.getBowRedux(); // 讀取遠距離減免
			} else {
				reduction = _targetPc.getMeleeRedux(); // 讀取近距離減免
			}
			
			// 確保減免值有效
			if (reduction > 0) {
				dmg -= reduction;
				
				// [建議檢查] 防止傷害變成負數 (雖然方法最後通常有檢查，但在這裡攔截更保險)
				if (dmg < 0) {
					dmg = 0;
				}
			}
		}
		
		// [PVP] 應用被攻擊者的 PVP 傷害減免
		dmg -= _targetPc.getPvpRedux();

		// [Phase 3] 機率傷害減免改由 UnifiedProcExecutor.triggerOnDefense 在 receiveDamage 時處理

		// 增幅防禦
		if (_targetPc.hasSkillEffect(REDUCTION_ARMOR)) {
			final int targetPcLvl = Math.max(_targetPc.getLevel(), 50);
			dmg -= (targetPcLvl - 50) / 5 + 10;
		}

		// 破壞盔甲：近距離武器對受術者傷害 +58%（參考 阿原專案）
		if ((_weaponType != 20) && (_weaponType != 62) && _targetPc.hasSkillEffect(ARMOR_BREAK)) {
			dmg = (int) (dmg * 1.58);
		}

		dmg += _targetPc.getCombatDmgBonus();
		if (_targetPc.isCombatDmgHalve()) {
			dmg /= 2;
		}

		// [Phase 3] 魔法娃娃技能改由 UnifiedProcExecutor.triggerOnAttack 在 commit 時觸發

		dmg *= coatArms();

		// [職業傷害倍數] 各職業對玩家物理傷害額外加乘
		final double pcClassMultiplier = ConfigPcDamage.getPhysicalDamageMultiplierToPc(_pc);
		dmg *= pcClassMultiplier;
		
		// 未命中傷害歸0
		if (!this._isHit) {
			dmg = 0.0;
		}

		if (dmg <= 0) {
			_isHit = false;
			_drainHp = 0;
		}
		
		// [天賦系統] 物理傷害天賦加成
		final int talentDamage = com.lineage.server.model.talent.TalentDamageCalculator.calcTalentDamage(_pc, 0, (int) dmg);
		dmg += talentDamage;
		
		// [GM 除錯] 顯示詳細的 PC 傷害計算過程
		if (_pc.isGm() && ConfigAlt.ALT_ATKMSG) {
			final int pcDefense = calcPcDefense();

			final StringBuilder debug = new StringBuilder();
			debug.append("【對PC傷害】目標: ").append(_targetPc.getName());
			debug.append(" | 初始: ").append(String.format("%.0f", weaponTotalDamage));
			debug.append(" | AC-").append(pcDefense);
			if (ConfigPcDamage.AC_REDUCTION_DIVISOR > 0 && _targetPc != null && _targetPc.getAc() < 0) {
				final int acRedux = (int) (Math.abs(_targetPc.getAc()) / ConfigPcDamage.AC_REDUCTION_DIVISOR);
				debug.append(" | AC額外減免-").append(acRedux);
			}
			debug.append(" | PVP+").append(_pc.getPvpDmg());
			debug.append(" | 倍數x").append(String.format("%.2f", pcClassMultiplier));
			if (talentDamage > 0) {
				debug.append(" | 天賦+").append(talentDamage);
			}
			debug.append(" → 最終: ").append((int)dmg);
			
			_pc.sendPackets(new S_ServerMessage(166, debug.toString()));
		}
		
		return (int) dmg;
	}


	/**
	 * PC對NPC傷害
	 * @return
	 */
	private int calcNpcDamage() {
		if (_targetNpc == null) {
			return 0;
		}

		_damageMultiplier = 1.0; // 每輪傷害計算前重置
		
		// 傷害為0
		if (dmg0(_targetNpc)) {
			_isHit = false;
			_drainHp = 0;
			return 0;
		}

		if (!_isHit) {
			return 0;
		}
	
		int weaponMaxDamage = 0;
		if (_targetNpc.getNpcTemplate().isSmall() && (_weaponSmall > 0)) {
			if (_weaponSmall > 0) {
				weaponMaxDamage = _weaponSmall;
			}
			
		} else if (_targetNpc.getNpcTemplate().isLarge() && (_weaponLarge > 0)) {
			if (_weaponLarge > 0) {
				weaponMaxDamage = _weaponLarge;
			}
			
		} else {
			if (_weaponSmall > 0) {
				weaponMaxDamage = _weaponSmall;
			}
		}
		// 傷害直初始化
		int weaponDamage = weaponDamage1(weaponMaxDamage);

		if (_pc.is_mazu()) {// 媽祖祝福
			weaponDamage += 1;
		}

		// 傷害計算：根據武器類型使用對應的 DMG 值
		double weaponTotalDamage;
		if ((_weaponType != 20) && (_weaponType != 62)) {
			// [近戰傷害公式]
			// [素質] 力量傷害補正
			final int strDmg = com.lineage.server.utils.CalcStat.calcStrDmg((int) _pc.getStr());
			// [武器] 浮動傷害值
			// [武器] 通用傷害（已包含基礎值、額外值、強化加成）
			weaponTotalDamage = weaponDamage + _weaponDmg + _weaponEnchant;
			// [防具/魔法] 傷害加成
			weaponTotalDamage += _pc.getDmgup() + _pc.getOriginalDmgup();
			// [防具] 額外傷害
			weaponTotalDamage += _pc.getDmgModifierByArmor();
			// [素質] 加上力量傷害補正
			weaponTotalDamage += strDmg;
		} else {
			// [遠攻傷害公式] - 修正：恢復強化值計算以匹配參考版本
			// [素質] 敏捷傷害補正
			final int dexDmg = com.lineage.server.utils.CalcStat.calcDexDmg((int) _pc.getDex());
			// [武器] 浮動傷害值 + 武器追加傷害 + 強化值（修正關鍵問題）
			weaponTotalDamage = weaponDamage + _weaponDmg + _weaponEnchant;
			// [防具/魔法] 遠攻傷害加成
			weaponTotalDamage += _pc.getBowDmgup() + _pc.getOriginalBowDmgup();
			// [防具] 遠攻額外傷害
			weaponTotalDamage += _pc.getBowDmgModifierByArmor();
			// [素質] 加上敏捷傷害補正
			weaponTotalDamage += dexDmg;
		}

		weaponTotalDamage += calcMaterialBlessDmg(); // 祝福武器 銀/米索莉/奧里哈魯根材質武器

		weaponTotalDamage += calcAttrEnchantDmg(); // 属性強化


		// 傷害直最終計算
		double dmg = weaponDamage2(weaponTotalDamage);

		// PC基礎傷害提昇計算
		dmg = pcDmgMode(dmg, weaponTotalDamage);


		dmg -= calcNpcDamageReduction();// NPC防禦力額外傷害減低

		// 玩家攻擊寵物、召喚獸（依 ConfigPcDamage.PC_TO_PET_DAMAGE_MULTIPLIER 倍率計算）
		if (_targetNpc instanceof L1PetInstance) {
			dmg *= ConfigPcDamage.PC_TO_PET_DAMAGE_MULTIPLIER;
		} else if (_targetNpc instanceof L1SummonInstance) {
			final L1SummonInstance summon = (L1SummonInstance) _targetNpc;
			if (summon.isExsistMaster()) {
				dmg *= ConfigPcDamage.PC_TO_PET_DAMAGE_MULTIPLIER;
			}
		}

		// 聖結界 (法師_技能設定表)
		if (_targetNpc.hasSkillEffect(IMMUNE_TO_HARM)) {
			dmg /= ConfigSkill.IMMUNE_TO_HARM;
		}
		
		// [硬皮怪機制] 遠距離攻擊減傷，近距離攻擊增加武器損壞機率
		if (_targetNpc.getNpcTemplate().is_hard()) {
			// [重構] 從 L1PcInstance 緩存讀取貫穿屬性（而非直接從武器讀取）
			// 這樣可以支援來自武器、防具、套裝等多種來源的貫穿屬性
			boolean isPierce = _pc.getPierce() > 0;
			
			// 判斷是否為遠距離攻擊（弓=20, 鐵手甲=62）
			boolean isRangedAttack = (_weaponType == 20) || (_weaponType == 62);
			
			// 遠距離攻擊：如果沒有貫穿屬性，傷害減半
			if (isRangedAttack && !isPierce) {
				dmg /= 2;
			}
			// 近距離攻擊：不減傷；武器損壞延後到 accepted-hit commit plan 處理。
		}

		// [Phase 3] 魔法娃娃技能改由 UnifiedProcExecutor.triggerOnAttack 在 commit 時觸發

		// [職業傷害倍數] 各職業對怪物物理傷害額外加乘
		final double classMultiplier = ConfigPcDamage.getPhysicalDamageMultiplierToNpc(_pc);
		dmg *= classMultiplier;

		// [對 BOSS 增傷] 目標為 npc.boss 時追加裝備/套裝的對 BOSS 傷害
		if (_targetNpc.getNpcTemplate().isBoss()) {
			dmg += _pc.getBossDmg();
		}

		// 未命中傷害歸0
		if (!_isHit) {
			dmg = 0D;
		}
		
		if (dmg <= 0D) {
			_isHit = false;
			_drainHp = 0; // 無傷害時不因吸收回復
		}
		
		// [GM 除錯] 顯示詳細的 NPC 傷害計算過程
		if (_pc.isGm() && ConfigAlt.ALT_ATKMSG) {
			final int npcReduction = calcNpcDamageReduction();
			final boolean isHard = _targetNpc.getNpcTemplate().is_hard();
			final boolean hasPierce = _pc.getPierce() > 0;
			final boolean isRanged = (_weaponType == 20) || (_weaponType == 62);
			
			final StringBuilder debug = new StringBuilder();
			debug.append("【對NPC傷害】目標: ").append(_targetNpc.getName());
			debug.append(" | 初始:").append(String.format("%.0f", weaponTotalDamage));
			debug.append(" | 防禦-").append(npcReduction);
			if (isHard && isRanged) {
				debug.append(" | 硬皮(遠):").append(hasPierce ? "有貫穿" : "減半⚠️");
			}
			debug.append(" → 最終: ").append((int)dmg);
			_pc.sendPackets(new S_ServerMessage(166, debug.toString()));
		}
		
		// [天賦系統] 物理傷害天賦加成
		final int talentDamage = com.lineage.server.model.talent.TalentDamageCalculator.calcTalentDamage(_pc, 0, (int) dmg);
		dmg += talentDamage;
		
		return (int) dmg;
	}
	
	/**
	 * 龍騎士 弱點曝光
	 * @return
	 */
	private int dk_dmgUp() {
		int dmg = 0;
		if (_pc.isDragonKnight() && _weaponType2 == 18) {// 鎖鍊劍 (龍騎士_技能設定表)
			long h_time = Calendar.getInstance().getTimeInMillis() / 1000;
			switch (_pc.get_weaknss()) {
			case 0:
				if (_pc.isFoeSlayer()) return 0;
				if ((ThreadLocalRandom.current().nextInt(100)) < ConfigSkill.VULNERABILITY_ROM) {
					_pc.set_weaknss(1, h_time);
					_pc.sendPackets(new S_PacketBoxDk(S_PacketBoxDk.LV1));
					dmg += ConfigSkill.VULNERABILITY_1;
				}
				break;
			case 1:
				if (_pc.isFoeSlayer()) return 0;
				if ((ThreadLocalRandom.current().nextInt(100)) < ConfigSkill.VULNERABILITY_ROM) {
					_pc.set_weaknss(1, h_time);
					_pc.sendPackets(new S_PacketBoxDk(S_PacketBoxDk.LV1));
					dmg += ConfigSkill.VULNERABILITY_1;
				} else {
					_pc.set_weaknss(2, h_time);
					_pc.sendPackets(new S_PacketBoxDk(S_PacketBoxDk.LV2));
					dmg += ConfigSkill.VULNERABILITY_2;
				}
				break;
			case 2:
				if (_pc.isFoeSlayer()) return 0;
				if ((ThreadLocalRandom.current().nextInt(100)) < ConfigSkill.VULNERABILITY_ROM) {
					_pc.set_weaknss(2, h_time);
					_pc.sendPackets(new S_PacketBoxDk(S_PacketBoxDk.LV2));
					dmg += ConfigSkill.VULNERABILITY_2;
				} else {
					_pc.set_weaknss(3, h_time);
					_pc.sendPackets(new S_PacketBoxDk(S_PacketBoxDk.LV3));
					dmg += ConfigSkill.VULNERABILITY_3;
				}
				break;
			case 3:
				if (_pc.isFoeSlayer()) return 0;
				if ((ThreadLocalRandom.current().nextInt(100)) < ConfigSkill.VULNERABILITY_ROM) {
					_pc.set_weaknss(3, h_time);
					_pc.sendPackets(new S_PacketBoxDk(S_PacketBoxDk.LV3));
					dmg += ConfigSkill.VULNERABILITY_3;
				}
				break; // Lv3 維持機率同 VULNERABILITY_ROM
			}
		}
		return dmg;
	}
	
	/**
	 * 技能對武器追加傷害
	 * @return
	 */
	private double dmgUp() {
		final boolean ranged = (_weaponType == 20) || (_weaponType == 62);
		return _pc.getCombatCookingDmgBonus(ranged);
	}

	/**
	 * 武器強化魔法
	 * @param dmg
	 * @return
	 */
	private double calcBuffDamage(double dmg) {
		// 烈焰之魂 (175)：每下必記錄 SOUL_OF_FLAME_DAMAGE 倍率，多種觸發時取最高
		if (_pc.hasSkillEffect(SOUL_OF_FLAME)) {
			_damageMultiplier = Math.max(_damageMultiplier, ConfigSkill.SOUL_OF_FLAME_DAMAGE);
		}

		if (_weaponType == 20) {// 弓
			return dmg;
		}
		if (_weaponType == 62) {// 鐵手甲
			return dmg;
		}
		if (_weaponType2 == 17) {// 奇古獸
			return dmg;
		}
		
		final int random = ThreadLocalRandom.current().nextInt(100) + 1;
		
		// 屬性之火 (妖精_技能設定表) - 取最高倍率
		if (_pc.hasSkillEffect(ELEMENTAL_FIRE)) {
			if (random <= ConfigSkill.ELEMENTAL_FIRE_RND) {
				_damageMultiplier = Math.max(_damageMultiplier, ConfigSkill.ELEMENTAL_FIRE);
			}
		}
		
		// 燃燒鬥志 (黑暗妖精_技能設定表) - 取最高倍率
		if (_pc.hasSkillEffect(BURNING_SPIRIT)) {
			if (random <= ConfigSkill.BURNING_CHANCE) {
				_damageMultiplier = Math.max(_damageMultiplier, ConfigSkill.BURNING_DMG);
			}
		}
		return dmg;
	}

	/**
	 * 祝福武器 銀/米索莉/奧里哈魯根材質武器<BR>
	 * 其他屬性定義
	 * @return
	 */
	private int calcMaterialBlessDmg() {
		int damage = 0;
		if (_pc.getWeapon() != null) {
			final int undead = _targetNpc.getNpcTemplate().get_undead();
			switch (undead) {
			case 1:// 不死系
				if ((_weaponMaterial == 14) || (_weaponMaterial == 17) || (_weaponMaterial == 22)) {// 銀/米索莉/奧里哈魯根
					damage += ThreadLocalRandom.current().nextInt(20) + 1;
				}
				if (_weaponBless == 1) { // 祝福武器（內部 1=祝福）
					damage += ThreadLocalRandom.current().nextInt(4) + 1;
				}
				break;
			case 2:// 惡魔系
				if ((_weaponMaterial == 17) || (_weaponMaterial == 22)) {// 米索莉/奧里哈魯根
					damage += ThreadLocalRandom.current().nextInt(3) + 1;
				}
				if (_weaponBless == 1) { // 祝福武器（內部 1=祝福）
					damage += ThreadLocalRandom.current().nextInt(4) + 1;
				}
				break;
			case 3:// 殭屍系
				if ((_weaponMaterial == 14) || (_weaponMaterial == 17) || (_weaponMaterial == 22)) {// 銀/米索莉/奧里哈魯根
					damage += ThreadLocalRandom.current().nextInt(20) + 1;
				}
				if (_weaponBless == 1) { // 祝福武器（內部 1=祝福）
					damage += ThreadLocalRandom.current().nextInt(4) + 1;
				}
				break;
			case 5:// 狼人系
				if ((_weaponMaterial == 14) || (_weaponMaterial == 17) || (_weaponMaterial == 22)) {// 銀/米索莉/奧里哈魯根
					damage += ThreadLocalRandom.current().nextInt(20) + 1;
				}
				break;
			}
		}
		return damage;
	}

	/**
	 * 武器屬性強化追加傷害計算
	 * [架構重構] 從 ItemAbility 讀取常駐傷害，而非查詢資料表
	 */
	private int calcAttrEnchantDmg() {
		int damage = 0;
		
		// 從 ItemAbility 讀取屬性武器常駐傷害
		if (_weapon != null) {
			final com.lineage.server.model.item.L1ItemAbility weaponAbility = _weapon.getAbility();
			final int attrKind = weaponAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.ATTR_ENCHANT_KIND);
			
			if (attrKind > 0) {
				// 從 ItemAbility 讀取常駐傷害
				damage = weaponAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.ATTR_PASSIVE_DMG);
				
				// 如果 ItemAbility 中沒有設定（為 0），回退到舊公式 (Lv*2 - 1)
				if (damage <= 0) {
					final int attrLevel = weaponAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.ATTR_ENCHANT_LEVEL);
					if (attrLevel > 0) {
						damage = (attrLevel * 2) - 1;
					}
				}
			}
		}
		if (damage <= 0) {
			return 0;
		}
		
		// [保留] 對地火火風光暗聖邪 抗性的處理
		int resist = 0;
		final int weaponAttrKind = (_weapon != null) ? 
			_weapon.getAbility().get(com.lineage.server.model.item.L1ItemAbility.AbilityType.ATTR_ENCHANT_KIND) : 0;
		
		switch (this._calcType) {
		case PC_PC:
			switch (weaponAttrKind) {
			case 1: // 地
				resist = this._targetPc.getEarth();
				break;

			case 2: // 火
				resist = this._targetPc.getFire();
				break;

			case 4: // 水
				resist = this._targetPc.getWater();
				break;

			case 8: // 風
				resist = this._targetPc.getWind();
				break;
				
			case 16: // 光
				resist = this._targetPc.getMr(); // 通常光屬性對應魔防或特定抗性，這裡暫用MR或自訂
				// 若你的版本光暗聖邪有獨立抗性欄位，請改為 getLight() 等
				break;
				
			case 32: // 暗
				resist = this._targetPc.getMr(); // 同上
				break;
				
			case 64: // 聖
				resist = this._targetPc.getMr(); // 同上
				break;
				
			case 128: // 邪
				resist = this._targetPc.getMr(); // 同上
				break;
			}
			break;

		case PC_NPC:
			switch (weaponAttrKind) {
			case 1: // 地
				resist = this._targetNpc.getEarth();
				break;

			case 2: // 火
				resist = this._targetNpc.getFire();
				break;

			case 4: // 水
				resist = this._targetNpc.getWater();
				break;

			case 8: // 風
				resist = this._targetNpc.getWind();
				break;
				
			case 16: // 光
				resist = this._targetNpc.getMr();
				break;
				
			case 32: // 暗
				resist = this._targetNpc.getMr();
				break;
				
			case 64: // 聖
				resist = this._targetNpc.getMr();
				break;
				
			case 128: // 邪
				resist = this._targetNpc.getMr();
				break;
			}
			break;
		}

		int resistFloor = (int) (0.32 * Math.abs(resist));

		if (resist < 0) {
			resistFloor *= -1;
		}
		
		final double attrDeffence = resistFloor / 32.0;
		final double attrCoefficient = 1 - attrDeffence;

		damage *= attrCoefficient;

		return damage;
	}
	
	/**
	 * PC附加毒性攻擊
	 * @param attacker
	 * @param target
	 */
	private void addPcPoisonAttack(final L1Character target) {
		if (_weaponId != 0) {// 非空手
			if (_pc.hasSkillEffect(ENCHANT_VENOM)) {
				final int chance = ThreadLocalRandom.current().nextInt(100) + 1;
				if (chance <= 10) {
					// 普通毒，每 3 秒，傷害 HP-5
					L1DamagePoison.doInfection(_pc, target, 3000, 5);
				}
			}
		}
	}

	/**
	 * 攻擊資訊送出
	 */
	//@Override
	public void action() {
		try {
			if (_pc == null) {
				return;
			}
			if (_target == null) {
				return;
			}
			// 改變面向
			_pc.setHeading(_pc.targetDirection(_targetX, _targetY));
			
			if (_weaponRange == -1) {// 遠距離武器
				actionX1();
				
			} else {// 近距離武器
				actionX2();
			}
			
		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * 近距離武器/空手
	 */
	private void actionX2() {
		try {
		if (_isHit) {// 命中
			_pc.sendPacketsAll(
						new S_AttackPacketPc(
								_pc, 
								_target, 
								_attackType, 
								_damage
								));
			// 奇古獸普攻特效（SideEffects 層送包，受 other.properties 外控）
			if (_weaponType2 == 17 && ConfigOther.KIRINGKU_EFFECT_ENABLE && ConfigOther.KIRINGKU_EFFECT_SPR > 0) {
				_pc.sendPacketsAll(new S_SkillSound(_pc.getId(), ConfigOther.KIRINGKU_EFFECT_SPR));
			}
			
			} else if (_isTerrainBlocked) {// 地形阻擋：只送攻擊者自身動作，不鎖定目標（目標看不到 MISS）
				_pc.sendPacketsAll(new S_AttackPacketPc(_pc));

			} else {// 戰鬥 MISS
				if (_targetId > 0) {
					_pc.sendPacketsAll(
							new S_AttackPacketPc(_pc, _target));

				} else {
					_pc.sendPacketsAll(new S_AttackPacketPc(_pc));
				}
			}
			
		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * 遠距離武器
	 */
	private void actionX1() {
		try {
			// 地形阻擋時：只播放攻擊者本身的攻擊動作，不射出箭矢/飛刀（目標感知不到）
			if (_isTerrainBlocked) {
				int aid = 1;
				if (_pc.getTempCharGfx() == 3860) {
					aid = 21;
				}
				_pc.sendPacketsAllBatch(List.of(
						new S_ChangeHeading(_pc),
						new S_DoActionGFX(_pc.getId(), aid)));
				return;
			}
			// 安全區/戰鬥未命中時，將目標ID設為0，避免出現受傷動作
			// 這樣箭矢會飛向目標座標，但不會鎖定在目標身上觸發受傷動畫
			int sendId = _isHit ? _targetId : 0;
			int sendDmg = _isHit ? _damage : 0;

			switch (_weaponType) {
			case 20:// 弓
				// 有選用實體箭矢時依 etcitem_arrow 決定是否消耗；無箭的魔法弓生成魔法箭。
				if (_arrow != null) { // 具有箭
					if (!_pc.getInventory().consumeProjectile(_arrow)) {
						_projectileReservationFailed = true;
						_pc.sendPacketsAll(new S_AttackPacketPc(_pc));
						break;
					}
					final int arrowGfxid = L1PolyMorph.resolveProjectileGfx(_pc, _weapon, _arrow, 66);
					_pc.sendPacketsAll(
							new S_UseArrowSkill(
									_pc, 
									sendId, // [修正] 使用 sendId
									arrowGfxid, 
									_targetX, 
									_targetY, 
									sendDmg // [修正] 使用 sendDmg
									));
					
				} else if (_weapon != null && _weapon.isMagicBow()) {
					final int arrowGfxid = L1PolyMorph.resolveProjectileGfx(_pc, _weapon, 2349);
					_pc.sendPacketsAll(
							new S_UseArrowSkill(
									_pc,
									sendId,
									arrowGfxid,
									_targetX,
									_targetY,
									sendDmg
									));
				} else {
					int aid = 1;
					// 外型編號改變動作
					if (_pc.getTempCharGfx() == 3860) {
						aid = 21;
					}
					_pc.sendPacketsAllBatch(List.of(
							new S_ChangeHeading(_pc),
							new S_DoActionGFX(_pc.getId(), aid)));
				}
				break;

			case 62: // 鐵手甲
				if (_sting != null) {// 具有飛刀
					if (!_pc.getInventory().consumeProjectile(_sting)) {
						_projectileReservationFailed = true;
						_pc.sendPacketsAll(new S_AttackPacketPc(_pc));
						break;
					}
					final int stingGfxid = L1PolyMorph.resolveProjectileGfx(_pc, _weapon, _sting, 2989);
					_pc.sendPacketsAll(
							new S_UseArrowSkill(
									_pc, 
									sendId, // [修正] 使用 sendId
									stingGfxid, 
									_targetX, 
									_targetY, 
									sendDmg // [修正] 使用 sendDmg
									));
				} else {// 沒有飛刀
					int aid = 1;
					// 外型編號改變動作
					if (_pc.getTempCharGfx() == 3860) {
						aid = 21;
					}

					_pc.sendPacketsAllBatch(List.of(
							new S_ChangeHeading(_pc),
							new S_DoActionGFX(_pc.getId(), aid)));
				}
				break;
			}
			
		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * D-2 Calc：攻擊 Proc（LogicPool 於 calcDamage 後呼叫）。
	 */
	public void calcAttackProcs() {
		if (!_isHit || _pc == null) {
			_attackProcResult = ProcAttackResult.empty();
			return;
		}
		final L1Character target = (_calcType == PC_PC) ? _targetPc : _targetNpc;
		if (target == null) {
			_attackProcResult = ProcAttackResult.empty();
			return;
		}
		try {
			_attackProcResult = UnifiedProcExecutor.calcOnAttack(_pc, target, _damage);
		} catch (final Exception e) {
			_log.error("統一 Proc 攻擊 Calc 失敗", e);
			_attackProcResult = ProcAttackResult.empty();
		}
	}

	/**
	 * C-3 SideEffects 入口（封包、Proc、{@code receiveDamage}→DamageCommit）。
	 * 應由 SlowPool 呼叫；Calc 完成後再執行。
	 */
	public void commitSideEffects() {
		commit();
	}

	/**
	 * 計算結果
	 */
	//@Override
	public void commit() {
		if (_projectileReservationFailed) {
			return;
		}
		if (_isHit) {
			if (_pc.dice_hp() != 0) {
				if ((ThreadLocalRandom.current().nextInt(1000) + 1) <= _pc.dice_hp()) {
					_drainHp = _pc.sucking_hp();
				}
			}
			if (_pc.dice_mp() != 0) {
				if ((ThreadLocalRandom.current().nextInt(1000) + 1) <= _pc.dice_mp()) {
					_drainMana = _pc.sucking_mp();
				}
			}
			switch (_calcType) {
			case PC_PC:
				commitPc();
				break;

			case PC_NPC:
				commitNpc();
				break;
			}
		} else {
			// 地形阻擋時不顯示 MISS 文字（目標完全感知不到此次攻擊）
			if (!_isTerrainBlocked && ConfigOther.DMG_DISPLAY_ENABLE) {
				final L1Character missTarget = (_calcType == PC_PC) ? _targetPc : _targetNpc;
				com.lineage.server.utils.DamageDisplayUtil.showPhysicalDamage(_pc, missTarget, 0, false, true);
			}
		}
		// Accepted hits publish GM/debug damage from their immutable commit plan.
		// Misses have no target-lane event and continue through the legacy message.
		if (_isHit) {
			return;
		}
		
		// gm攻擊資訊
		if (!ConfigAlt.ALT_ATKMSG) {
			return;
			
		} else {
			switch (_calcType) {
			case PC_PC:
				if (!_pc.isGm()) {
					if (!_targetPc.isGm()) {
						return;
					}
				}
				break;

			case PC_NPC:
				if (!_pc.isGm()) {
					return;
				}
				break;
			}
		}

		final String srcatk = _pc.getName();// 攻擊者
		String tgatk = "";// 被攻擊者
		String x = "";// 最終資訊

		switch (this._calcType) {
		case PC_PC:
			tgatk = _targetPc.getName();
			x = _isHit ? (srcatk + ">" + tgatk + "傷害:" + _damage) : (srcatk + ">" + tgatk + "失敗");
			if (_pc.isGm()) {
				_pc.sendPackets(new S_ServerMessage(166, x));
			}
			if (_targetPc.isGm()) {
				_targetPc.sendPackets(new S_ServerMessage(166, x));
			}
			break;

		case PC_NPC:
			tgatk = this._targetNpc.getName();
			x = this._isHit ? (srcatk + ">" + tgatk + "傷害:" + this._damage) : (srcatk + ">" + tgatk + "失敗");
			if (this._pc.isGm()) {
				this._pc.sendPackets(new S_ServerMessage(166, x));
			}
			break;
		}
	}

	/**
	 * 對PC攻擊傷害結果
	 */
	private void commitPc() {
		// 精準射擊：目標在持續時間內受到遠距離傷害增加（ConfigSkill.STRIKER_DMG2）
		if (_isHit && _targetPc.hasSkillEffect(STRIKER_GALE) && (_weaponType == 20 || _weaponType == 62)) {
			_damage = (int) (_damage * ConfigSkill.STRIKER_DMG2);
		}

		calcAttackProcs();
		final PhysicalHitCommitPlan plan = new PhysicalHitCommitPlan(
				_pc, _targetPc, _weapon, _weaponType, _weaponMaterial, _damage,
				_drainHp, _drainMana, _attackProcResult, isCriticalHit(), _pc.lift());
		_targetPc.receiveDamage(_pc, _damage, false, false, plan);
	}

	/**
	 * 對NPC攻擊傷害結果
	 */
	private void commitNpc() {
		// 精準射擊：目標在持續時間內受到遠距離傷害增加（ConfigSkill.STRIKER_DMG2）
		if (_isHit && _targetNpc.hasSkillEffect(STRIKER_GALE) && (_weaponType == 20 || _weaponType == 62)) {
			_damage = (int) (_damage * ConfigSkill.STRIKER_DMG2);
		}

		calcAttackProcs();
		final PhysicalHitCommitPlan plan = new PhysicalHitCommitPlan(
				_pc, _targetNpc, _weapon, _weaponType, _weaponMaterial, _damage,
				_drainHp, _drainMana, _attackProcResult, isCriticalHit(), _pc.lift());
		DamageCommit.enqueue(new DamageEvent(_pc, _targetNpc, _damage, 0,
				false, false, null, plan));
	}

	private boolean isCriticalHit() {
		return (_weaponType2 == 4 || _weaponType2 == 13 || _weaponType2 == 10)
				? _attackType == 0x02
				: _attackType == 0x02 || _attackType == 0x04;
	}

	/**
	 * 判斷反擊屏障對敵方攻擊是否有效
	 */
	//@Override
	public boolean isShortDistance() {
		boolean isShortDistance = true;
		if ((_weaponType == 20) || (_weaponType == 62)) { // 弓或臂甲
			isShortDistance = false;
		}
		return isShortDistance;
	}

	/**
	 * 反擊屏障的傷害反擊
	 */
	//@Override
	public void commitCounterBarrier() {
		int damage = calcCounterBarrierDamage();
		if (damage == 0) {
			return;
		}
		damage = (int) (damage * ConfigSkill.COUNTER_BARRIER_DMG);
		if (_targetPc != null) {
			_targetPc.sendPacketsAll(new S_SkillSound(
					_targetPc.getId(), COUNTER_BARRIER_TRIGGER_GFX));
		}
		// 受傷動作
		_pc.sendPacketsAll(
				new S_DoActionGFX(
						_pc.getId(), 
						ActionCodes.ACTION_Damage
						));
		_pc.receiveDamage(_target, damage, false, true);
	}

}
