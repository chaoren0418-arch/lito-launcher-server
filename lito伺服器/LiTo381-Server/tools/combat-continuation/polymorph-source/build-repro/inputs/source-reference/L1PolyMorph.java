package com.lineage.server.model;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lineage.config.ConfigCharSetting;
import com.lineage.server.datatables.PolyAbilityTable;
import com.lineage.server.datatables.PolyTable;
import com.lineage.server.model.Instance.L1DeInstance;
import com.lineage.server.model.Instance.L1ItemInstance;
import com.lineage.server.model.Instance.L1MonsterInstance;
import com.lineage.server.model.Instance.L1PcInstance;
import com.lineage.server.model.PlayerAutoActionGate.Token;
import com.lineage.server.model.item.L1ItemAbility;
import com.lineage.server.model.skill.L1SkillId;
import com.lineage.server.serverpackets.S_ChangeShape;
import com.lineage.server.serverpackets.S_CharVisualUpdate;
import com.lineage.server.serverpackets.S_CloseList;
import com.lineage.server.serverpackets.S_PacketBox;
import com.lineage.server.serverpackets.S_ServerMessage;
import com.lineage.server.serverpackets.S_WeaponRange;
import com.lineage.server.thread.GeneralThreadPool;
import com.lineage.server.thread.PlayerAiWorkerPool;

/**
 * 變形控制項
 * 
 * @author daien
 * 
 */
public class L1PolyMorph {

	private static final Logger _log = LogManager.getLogger(L1PolyMorph.class);
	private static final int[] PRIVATE_SHOP_POLY_LIST =
			{ 11479, 11427, 10047, 9688, 11322, 10069, 10034, 10032 };

	private static final AtomicLong AUTO_POLY_SCHEDULE_REJECTED_COUNT = new AtomicLong();
	private static final AtomicLong AUTO_POLY_WORKER_REJECTED_COUNT = new AtomicLong();
	private static final AtomicLong AUTO_POLY_SCHEDULE_ERROR_COUNT = new AtomicLong();

	// weapon equip bit(2047)
	private static final int DAGGER_EQUIP = 1;

	private static final int SWORD_EQUIP = 2;

	private static final int TWOHANDSWORD_EQUIP = 4;

	private static final int AXE_EQUIP = 8;

	private static final int SPEAR_EQUIP = 16;

	private static final int STAFF_EQUIP = 32;

	private static final int EDORYU_EQUIP = 64;

	private static final int CLAW_EQUIP = 128;

	private static final int BOW_EQUIP = 256; // 含護手

	private static final int KIRINGKU_EQUIP = 512;

	private static final int CHAINSWORD_EQUIP = 1024;

	// armor equip bit
	private static final int HELM_EQUIP = 1;

	private static final int AMULET_EQUIP = 2;

	private static final int EARRING_EQUIP = 4;

	private static final int TSHIRT_EQUIP = 8;

	private static final int ARMOR_EQUIP = 16;

	private static final int CLOAK_EQUIP = 32;

	private static final int BELT_EQUIP = 64;

	private static final int SHIELD_EQUIP = 128;

	private static final int GLOVE_EQUIP = 256;

	private static final int RING_EQUIP = 512;

	private static final int BOOTS_EQUIP = 1024;

	private static final int GUARDER_EQUIP = 2048;

	// 顯示變身原因的 bit
	public static final int MORPH_BY_ITEMMAGIC = 1;

	public static final int MORPH_BY_GM = 2;

	public static final int MORPH_BY_NPC = 4; // 占星術師凱普利夏以外的 NPC

	public static final int MORPH_BY_KEPLISHA = 8;

	public static final int MORPH_BY_LOGIN = 0;

	private static final Map<Integer, Integer> weaponFlgMap = new HashMap<Integer, Integer>();
	static {
		weaponFlgMap.put(1, SWORD_EQUIP);// 劍
		weaponFlgMap.put(2, DAGGER_EQUIP);// 匕首
		weaponFlgMap.put(3, TWOHANDSWORD_EQUIP);// 雙手劍
		weaponFlgMap.put(4, BOW_EQUIP);// 弓
		weaponFlgMap.put(5, SPEAR_EQUIP);// 矛(雙手)
		weaponFlgMap.put(6, AXE_EQUIP);// 斧(單手)
		weaponFlgMap.put(7, STAFF_EQUIP);// 魔杖
		weaponFlgMap.put(8, BOW_EQUIP);// 飛刀
		weaponFlgMap.put(9, BOW_EQUIP);// 箭
		weaponFlgMap.put(10, BOW_EQUIP);// 鐵手甲
		weaponFlgMap.put(11, CLAW_EQUIP);// 鋼爪
		weaponFlgMap.put(12, EDORYU_EQUIP);// 雙刀
		weaponFlgMap.put(13, BOW_EQUIP);// 弓(單手)
		weaponFlgMap.put(14, SPEAR_EQUIP);// 矛(單手)
		weaponFlgMap.put(15, AXE_EQUIP);// 雙手斧
		weaponFlgMap.put(16, STAFF_EQUIP);// 魔杖(雙手)
		weaponFlgMap.put(17, KIRINGKU_EQUIP);// 奇古獸
		weaponFlgMap.put(18, CHAINSWORD_EQUIP);// 鎖鏈劍
	}
	private static final Map<Integer, Integer> armorFlgMap = new HashMap<Integer, Integer>();
	static {
		armorFlgMap.put(1, HELM_EQUIP);
		armorFlgMap.put(2, ARMOR_EQUIP);
		armorFlgMap.put(3, TSHIRT_EQUIP);
		armorFlgMap.put(4, CLOAK_EQUIP);
		armorFlgMap.put(5, GLOVE_EQUIP);
		armorFlgMap.put(6, BOOTS_EQUIP);
		armorFlgMap.put(7, SHIELD_EQUIP);
		armorFlgMap.put(8, AMULET_EQUIP);
		armorFlgMap.put(9, RING_EQUIP);
		armorFlgMap.put(10, BELT_EQUIP);
		armorFlgMap.put(12, EARRING_EQUIP);
		armorFlgMap.put(13, GUARDER_EQUIP);
	}

	private int _id;
	private String _name;
	private int _polyId;
	private int _minLevel;
	private int _weaponEquipFlg;
	private int _armorEquipFlg;
	private boolean _canUseSkill;
	private int _causeFlg;
	/** 變身遠程投射物 GFX；-1 表示不控制（沿用武器或預設） */
	private int _arrowSpr = -1;

	public static String getMonitorLine() {
		return "L1PolyMorph{autoPolyScheduleRejected=" + AUTO_POLY_SCHEDULE_REJECTED_COUNT.get()
				+ ", autoPolyWorkerRejected=" + AUTO_POLY_WORKER_REJECTED_COUNT.get()
				+ ", autoPolyScheduleError=" + AUTO_POLY_SCHEDULE_ERROR_COUNT.get() + "}";
	}

	private static void scheduleAutoPoly(final L1PcInstance pc) {
		if (pc == null) {
			return;
		}
		final Token token = pc.tryClaimAutoPolyAction();
		if (token == null) {
			return;
		}
		boolean timerAccepted = false;
		try {
			// PcScheduler is a clock only; durable debit/effect runs on PlayerAiWorker.
			final ScheduledFuture<?> future = GeneralThreadPool.get().pcSchedule(
					() -> dispatchAutoPoly(pc, token), 500L);
			if (future != null) {
				timerAccepted = true;
				return;
			}
			AUTO_POLY_SCHEDULE_ERROR_COUNT.incrementAndGet();
			_log.warn("[L1PolyMorph] auto poly schedule returned null: pc={}", pc.getName());
		} catch (final RejectedExecutionException e) {
			AUTO_POLY_SCHEDULE_REJECTED_COUNT.incrementAndGet();
			_log.warn("[L1PolyMorph] auto poly schedule rejected: pc={}", pc.getName(), e);
		} catch (final RuntimeException e) {
			AUTO_POLY_SCHEDULE_ERROR_COUNT.incrementAndGet();
			_log.warn("[L1PolyMorph] auto poly schedule failed: pc={}", pc.getName(), e);
		} finally {
			// A failed timer admission has no future expiry callback that could
			// recover this loop. Clear only this exact generation, never a replacement.
			if (!timerAccepted) {
				pc.clearAutoPolyActionIfCurrent(token);
			}
		}
	}

	private static void dispatchAutoPoly(final L1PcInstance pc, final Token token) {
		dispatchAutoPoly(pc, token, true);
	}

	private static void dispatchAutoPoly(final L1PcInstance pc, final Token token,
			final boolean allowRetry) {
		if (!pc.isCurrentAutoPolyAction(token)) {
			pc.releaseAutoPolyAction(token);
			return;
		}
		if (!PlayerAiWorkerPool.get().tryExecute(new AutoPolyWorkerTask(pc, token))) {
			final long rejected = AUTO_POLY_WORKER_REJECTED_COUNT.incrementAndGet();
			if ((rejected & (rejected - 1L)) == 0L) {
				_log.warn("[L1PolyMorph] Player AI worker rejected auto poly: pc={} count={}",
						pc.getName(), rejected);
			}
			if (allowRetry && scheduleAutoPolyRetry(pc, token)) {
				pc.releaseAutoPolyAction(token);
			} else {
				pc.clearAutoPolyActionIfCurrent(token);
			}
		}
	}

	private static final class AutoPolyWorkerTask
			implements PlayerAiWorkerPool.ShutdownDiscardable {
		private final L1PcInstance _pc;
		private final Token _token;

		private AutoPolyWorkerTask(final L1PcInstance pc, final Token token) {
			_pc = pc;
			_token = token;
		}

		@Override
		public void run() {
			_pc.handleAutoPoly(_token);
		}

		@Override
		public void discardOnShutdown() {
			_pc.clearAutoPolyActionIfCurrent(_token);
		}
	}

	private static boolean scheduleAutoPolyRetry(final L1PcInstance pc,
			final Token rejectedToken) {
		try {
			final ScheduledFuture<?> future = GeneralThreadPool.get().pcSchedule(() -> {
				final Token retry = pc.tryReclaimAutoPolyAction(rejectedToken);
				if (retry != null) {
					dispatchAutoPoly(pc, retry, false);
				}
			}, 250L);
			return future != null;
		} catch (final RejectedExecutionException e) {
			AUTO_POLY_SCHEDULE_REJECTED_COUNT.incrementAndGet();
			return false;
		} catch (final RuntimeException e) {
			AUTO_POLY_SCHEDULE_ERROR_COUNT.incrementAndGet();
			_log.warn("[L1PolyMorph] auto poly retry scheduling failed: pc={}",
					pc.getName(), e);
			return false;
		}
	}

	public L1PolyMorph(final int id, final String name, final int polyId, final int minLevel, final int weaponEquipFlg, final int armorEquipFlg, final boolean canUseSkill, final int causeFlg, final int arrowSpr) {
		this._id = id;
		this._name = name;
		this._polyId = polyId;
		this._minLevel = minLevel;
		this._weaponEquipFlg = weaponEquipFlg;
		this._armorEquipFlg = armorEquipFlg;
		this._canUseSkill = canUseSkill;
		this._causeFlg = causeFlg;
		this._arrowSpr = arrowSpr;
	}

	public int getId() {
		return this._id;
	}

	public String getName() {
		return this._name;
	}

	public int getPolyId() {
		return this._polyId;
	}

	public int getMinLevel() {
		return this._minLevel;
	}

	public int getWeaponEquipFlg() {
		return this._weaponEquipFlg;
	}

	public int getArmorEquipFlg() {
		return this._armorEquipFlg;
	}

	public boolean canUseSkill() {
		return this._canUseSkill;
	}

	public int getCauseFlg() {
		return this._causeFlg;
	}

	/**
	 * 變身遠程投射物 GFX（弓箭／鐵手甲飛刀共用）。
	 * @return GFX ID；-1 表示不控制
	 */
	public int getArrowSpr() {
		return this._arrowSpr;
	}

	/**
	 * 解析遠程投射物 GFX：選定箭矢／飛刀優先，其次武器、變身，最後預設值。
	 * <p>戰鬥時動態讀取，不累加至角色能力（與 {@link L1ItemAbility.AbilityType#ARROW_SPR} 一致）。
	 *
	 * @param pc 攻擊者（可為 null，僅用於變身查表）
	 * @param weapon 武器（可為 null）
	 * @param projectile 已選定的箭矢／飛刀（可為 null）
	 * @param defaultGfx 預設 GFX（弓 66、鐵手甲 2989）
	 */
	public static int resolveProjectileGfx(final L1PcInstance pc, final L1ItemInstance weapon,
			final L1ItemInstance projectile, final int defaultGfx) {
		if (projectile != null && projectile.getItem() != null) {
			final int projectileSpr = projectile.getItem().getArrowSpr();
			if (projectileSpr != -1) {
				return projectileSpr;
			}
		}
		if (weapon != null) {
			final int weaponSpr = weapon.getAbility().get(L1ItemAbility.AbilityType.ARROW_SPR);
			if (weaponSpr != -1) {
				return weaponSpr;
			}
		}
		if (pc != null && pc.getTempCharGfx() != pc.getClassId()) {
			final L1PolyMorph poly = PolyTable.get().getTemplate(pc.getTempCharGfx());
			if (poly != null && poly.getArrowSpr() != -1) {
				return poly.getArrowSpr();
			}
		}
		return defaultGfx;
	}

	/** 相容沒有實體投射物的呼叫（例如魔法箭）。 */
	public static int resolveProjectileGfx(final L1PcInstance pc, final L1ItemInstance weapon,
			final int defaultGfx) {
		return resolveProjectileGfx(pc, weapon, null, defaultGfx);
	}

	/**
	 * 龍騎覺醒三型態外觀（施放覺醒後 {@link com.lineage.server.model.skill.L1BuffUtil#doPoly} 寫入之結果）。<br>
	 * <strong>不可單靠圖形 ID 當唯一「是否在覺醒」依據</strong>，應以 {@link #resolveDragonKnightAwakeSkillId(L1PcInstance)} 為準。
	 */
	public static boolean isDragonKnightAwakenCharGfx(final int gfxId) {
		return gfxId == 9362 || gfxId == 9364 || gfxId == 9363;
	}

	private static int guessAwakeSkillIdFromGfx(final int gfxId) {
		if (gfxId == 9362) {
			return L1SkillId.AWAKEN_ANTHARAS;
		}
		if (gfxId == 9364) {
			return L1SkillId.AWAKEN_FAFURION;
		}
		if (gfxId == 9363) {
			return L1SkillId.AWAKEN_VALAKAS;
		}
		return -1;
	}

	/**
	 * 龍騎是否處於「覺醒」語意，並回傳對應技能編號（185／190／195）。<br>
	 * 判定優先序：<strong>① {@link L1PcInstance#getAwakeSkillId()} ② 覺醒技能 Buff 計時 ③ 外觀為 9362／9364／9363 之推斷</strong>。<br>
	 * ①② 為正常來源（{@code AWAKEN_*} 的 {@code start} 會設 ID 並由 {@code addMagicList} 註冊計時）；③ 僅作狀態脫鉤時之補強。
	 *
	 * @return 覺醒技能 ID，非覺醒則 0
	 */
	public static int resolveDragonKnightAwakeSkillId(final L1PcInstance pc) {
		if (pc == null) {
			return 0;
		}
		final int sid = pc.getAwakeSkillId();
		if (sid == L1SkillId.AWAKEN_ANTHARAS || sid == L1SkillId.AWAKEN_FAFURION || sid == L1SkillId.AWAKEN_VALAKAS) {
			return sid;
		}
		if (pc.hasSkillEffect(L1SkillId.AWAKEN_ANTHARAS)) {
			return L1SkillId.AWAKEN_ANTHARAS;
		}
		if (pc.hasSkillEffect(L1SkillId.AWAKEN_FAFURION)) {
			return L1SkillId.AWAKEN_FAFURION;
		}
		if (pc.hasSkillEffect(L1SkillId.AWAKEN_VALAKAS)) {
			return L1SkillId.AWAKEN_VALAKAS;
		}
		final int fromGfx = guessAwakeSkillIdFromGfx(pc.getTempCharGfx());
		return fromGfx < 0 ? 0 : fromGfx;
	}

	/** Poly 表「不可施法」時是否仍應放行技能封包（覺醒語意下不由此擋 285） */
	public static boolean skipPolyTableSkillBlock(final L1PcInstance pc) {
		return pc != null && resolveDragonKnightAwakeSkillId(pc) != 0;
	}

	/** 覺醒技能 ID（185／190／195）對應之龍外觀 polyId，非覺醒則 -1 */
	public static int awakenDragonGfxForSkill(final int awakeSkillId) {
		switch (awakeSkillId) {
		case L1SkillId.AWAKEN_ANTHARAS:
			return 9362;
		case L1SkillId.AWAKEN_FAFURION:
			return 9364;
		case L1SkillId.AWAKEN_VALAKAS:
			return 9363;
		default:
			return -1;
		}
	}

	private static int awakenPolyGfx(final int awakeSkillId) {
		return awakenDragonGfxForSkill(awakeSkillId);
	}

	/**
	 * 仿正覺醒（{@link com.lineage.config.ConfigCharSetting#isDragonKnightAwakeClassicMode()}）且為龍外觀時，阻擋會改變外觀的變身（NPC／非道具來源）。<br>
	 * 變形卷軸等 {@link #MORPH_BY_ITEMMAGIC} 一律不由此擋。
	 */
	private static boolean isBlockedPolyWhileDragonAwake(final L1PcInstance pc, final int polyId, final int cause) {
		if (cause == MORPH_BY_ITEMMAGIC || cause == MORPH_BY_GM) {
			return false;
		}
		if (!ConfigCharSetting.isDragonKnightAwakeClassicMode()) {
			return false;
		}
		final int awakeSid = resolveDragonKnightAwakeSkillId(pc);
		if (awakeSid == 0) {
			return false;
		}
		return polyId != awakenPolyGfx(awakeSid);
	}

	public static void handleCommands(final L1PcInstance pc, final String s) {
		if ((pc == null) || pc.isDead()) {
			return;
		}
		final L1PolyMorph poly = PolyTable.get().getTemplate(s);
		if ((poly != null) || s.equals("none")) {
			if (s.equals("none")) {
				if ((pc.getTempCharGfx() == 6034) || (pc.getTempCharGfx() == 6035)) {
				} else {
					pc.removeSkillEffect(L1SkillId.SHAPE_CHANGE);
					pc.sendPackets(new S_CloseList(pc.getId()));
				}

			} else if ((pc.getLevel() >= poly.getMinLevel()) || pc.isGm()) {
				if ((pc.getTempCharGfx() == 6034) || (pc.getTempCharGfx() == 6035)) {
					// 181:\f1無法變成你指定的怪物。
					pc.sendPackets(new S_ServerMessage(181));

				} else {
					doPoly(pc, poly.getPolyId(), 7200, MORPH_BY_ITEMMAGIC);
					pc.sendPackets(new S_CloseList(pc.getId())); // 無論是否成功套用，關閉變身視窗
				}

			} else {
				// 181:\f1無法變成你指定的怪物。
				pc.sendPackets(new S_ServerMessage(181));
			}
		}
	}

	/**
	 * 執行變身
	 * 
	 * @param cha
	 * @param polyId
	 * @param timeSecs
	 * @param cause
	 * @return PC：成功套用或已為同外觀仍更新變身計時／圖示為 true；遭地圖／覺醒限制／原因不符等為 false。怪物／假人成功為 true。
	 */
	/**
	 * Admission-only validation for DB-first callers.  It deliberately performs
	 * no timer, appearance, equipment or inventory mutation.
	 */
	public static boolean canDoPoly(final L1Character cha, final int polyId,
			final int cause) {
		// DB-first card callers require a real polymorph template; otherwise a
		// stale card row could charge material for an unconfigured sprite.
		return PolyTable.get().getTemplate(polyId) != null
				&& canDoPoly(cha, polyId, cause, false);
	}

	private static boolean canDoPoly(final L1Character cha, final int polyId,
			final int cause, final boolean notify) {
		if (cha == null || cha.isDead() || polyId <= 0) {
			return false;
		}
		if (!(cha instanceof L1PcInstance)) {
			return cha instanceof L1MonsterInstance || cha instanceof L1DeInstance;
		}

		final L1PcInstance pc = (L1PcInstance) cha;
		if (pc.getMapId() == 5300 || pc.getMapId() == 9000
				|| pc.getMapId() == 9100 || pc.getMapId() == 9101
				|| pc.getMapId() == 9102 || pc.getMapId() == 9202) {
			if (notify) {
				pc.sendPackets(new S_ServerMessage(1170));
			}
			return false;
		}
		if (pc.getTempCharGfx() == 6034 || pc.getTempCharGfx() == 6035) {
			if (notify) {
				pc.sendPackets(new S_ServerMessage(181));
			}
			return false;
		}
		if (!isMatchCause(polyId, cause)) {
			if (notify) {
				pc.sendPackets(new S_ServerMessage(181));
			}
			return false;
		}
		if (isBlockedPolyWhileDragonAwake(pc, polyId, cause)) {
			if (notify) {
				pc.sendPackets(new S_ServerMessage(1384));
			}
			return false;
		}
		return true;
	}

	public static boolean doPoly(final L1Character cha, final int polyId,
			final int timeSecs, final int cause) {
		if (cha instanceof L1PcInstance) {
			final L1PcInstance pc = (L1PcInstance) cha;
			if (!pc.getPlayerActionLock().isHeldByCurrentThread()) {
				return PlayerActionLock.call(pc,
						() -> doPolyLocked(cha, polyId, timeSecs, cause));
			}
		}
		return doPolyLocked(cha, polyId, timeSecs, cause);
	}

	private static boolean doPolyLocked(final L1Character cha, final int polyId,
			final int timeSecs, final int cause) {
		try {
			if (!canDoPoly(cha, polyId, cause, true)) {
				return false;
			}
			// 變身禁止使用
			/*
			 * if (cha.hasItemDelay(L1ItemDelay.POLY) == true) { return; }
			 */
			if (cha instanceof L1PcInstance) {
				final L1PcInstance pc = (L1PcInstance) cha;
				// added
				if (pc.getMapId() == 5300 // 釣魚池
						|| pc.getMapId() == 9000 // 從前的說話之島
						|| pc.getMapId() == 9100 // 象牙塔的秘密研究室（哈汀）
						|| pc.getMapId() == 9101 // 歐林副本地圖
						|| pc.getMapId() == 9102 // 歐林副本地圖
						|| pc.getMapId() == 9202) { // 象牙塔的秘密研究室（歐林）
					// 這裡不可以變身。
					pc.sendPackets(new S_ServerMessage(1170));
					return false;
				}
				if ((pc.getTempCharGfx() == 6034) || (pc.getTempCharGfx() == 6035)) {
					// 181:\f1無法變成你指定的怪物。
					pc.sendPackets(new S_ServerMessage(181));
					return false;
				}
				if (!isMatchCause(polyId, cause)) {
					// 181:\f1無法變成你指定的怪物。
					pc.sendPackets(new S_ServerMessage(181));
					return false;
				}
				if (isBlockedPolyWhileDragonAwake(pc, polyId, cause)) {
					pc.sendPackets(new S_ServerMessage(1384)); // 目前狀態中無法變身。
					return false;
				}

				pc.killSkillEffectTimer(L1SkillId.SHAPE_CHANGE);
				final int shapeMillis = timeSecs * 1000;
				pc.setSkillEffect(L1SkillId.SHAPE_CHANGE, shapeMillis);

				if (pc.getTempCharGfx() != polyId) { // 相同變身時只需發送圖示
					// 移除舊變身的能力附加
					final int oldGfxId = pc.getTempCharGfx();
					final L1PolyAbility oldPolyAbility = PolyAbilityTable.get().getTemplate(oldGfxId);
					if (oldPolyAbility != null) {
						oldPolyAbility.removeFrom(pc);
					}

					pc.setTempCharGfx(polyId); // // 死騎拿鎖鍊劍 移動位置

					// 套用新變身的能力附加（採用 ItemAbility 共同來源）
					final L1PolyAbility newPolyAbility = PolyAbilityTable.get().getTemplate(polyId);
					if (newPolyAbility != null) {
						newPolyAbility.applyTo(pc);
					}

					L1ItemInstance weapon = pc.getWeapon();
					// 變身後武器是否卸下
					final boolean weaponTakeoff = ((weapon != null) && !isEquipableWeapon(polyId, weapon.getItem().getType()));
					// repaired // 死騎拿鎖鍊劍
					if (weaponTakeoff) { // 武器強制脫除
						pc.setCurrentWeapon(0);
					}

					// 死騎拿鎖鍊劍
					else if (weapon != null) {
						pc.setCurrentWeapon(pc.getWeaponType(weapon.getItem().getType1()));
					}

					pc.sendPackets(new S_ChangeShape(pc, polyId, weaponTakeoff));
					if (!pc.isGmInvis() && !pc.isInvisble()) {
						pc.broadcastPacketAll(new S_ChangeShape(pc, polyId));
					}

					pc.getInventory().takeoffEquip(polyId);
					weapon = pc.getWeapon();

					if (weapon != null) {
						syncWeaponRangeToClient(pc);
						pc.sendPacketsAll(new S_CharVisualUpdate(pc));
					}
				}
				pc.sendPackets(new S_PacketBox(S_PacketBox.ICON_POLYMORPH, timeSecs));
				return true;

			} else if (cha instanceof L1MonsterInstance) {
				final L1MonsterInstance mob = (L1MonsterInstance) cha;
				mob.killSkillEffectTimer(L1SkillId.SHAPE_CHANGE);
				mob.setSkillEffect(L1SkillId.SHAPE_CHANGE, timeSecs * 1000);
				if (mob.getTempCharGfx() != polyId) { // 相同變身時只需發送圖示
					mob.setTempCharGfx(polyId);
					mob.broadcastPacketAll(new S_ChangeShape(mob, polyId));
				}
				return true;

			} else if (cha instanceof L1DeInstance) {
				final L1DeInstance de = (L1DeInstance) cha;
				de.killSkillEffectTimer(L1SkillId.SHAPE_CHANGE);
				de.setSkillEffect(L1SkillId.SHAPE_CHANGE, timeSecs * 1000);
				if (de.getTempCharGfx() != polyId) { // 相同變身時只需發送圖示
					de.setTempCharGfx(polyId);
					de.broadcastPacketAll(new S_ChangeShape(de, polyId));
				}
				return true;
			}
			return false;

		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
			return false;
		}
	}

	/**
	 * 解除變身
	 * 
	 * @param cha
	 */
	public static void undoPoly(final L1Character cha) {
		if (cha instanceof L1PcInstance) {
			final L1PcInstance pc = (L1PcInstance) cha;
			if (!pc.getPlayerActionLock().isHeldByCurrentThread()) {
				PlayerActionLock.run(pc, () -> undoPolyLocked(cha));
				return;
			}
		}
		undoPolyLocked(cha);
	}

	private static void undoPolyLocked(final L1Character cha) {
		try {
			if (cha instanceof L1PcInstance) {
				final L1PcInstance pc = (L1PcInstance) cha;
				// 移除變身的能力附加（採用 ItemAbility 共同來源）
				final int oldGfxId = pc.getTempCharGfx();
				final L1PolyAbility oldPolyAbility = PolyAbilityTable.get().getTemplate(oldGfxId);
				if (oldPolyAbility != null) {
					oldPolyAbility.removeFrom(pc);
				}

				final int classId = pc.getClassId();
				pc.setTempCharGfx(classId);
				// 死騎拿鎖鍊劍
				pc.setCurrentWeapon(pc.getWeaponType(pc.getCurrentWeapon()));

				pc.sendPacketsAll(new S_ChangeShape(pc, classId));
				final L1ItemInstance weapon = pc.getWeapon();
				if (weapon != null) {
					syncWeaponRangeToClient(pc);
					pc.sendPacketsAll(new S_CharVisualUpdate(pc));
				}

				// 卡冊：自動變身循環
				if (pc.getAutoPolyAction() != null && !pc.getAutoPolyAction().isEmpty()) {
					scheduleAutoPoly(pc);
				}
			} else if (cha instanceof L1MonsterInstance) {
				final L1MonsterInstance mob = (L1MonsterInstance) cha;
				mob.setTempCharGfx(0);
				mob.broadcastPacketAll(new S_ChangeShape(mob, mob.getGfxId()));
			}

		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	// 指定的 polyId 能否裝備 weapontType 武器？
	public static boolean isEquipableWeapon(final int polyId, final int weaponType) {
		try {
			final L1PolyMorph poly = PolyTable.get().getTemplate(polyId);
			if (poly == null) {
				return true;
			}

			final Integer flg = weaponFlgMap.get(weaponType);
			if (flg != null) {
				return 0 != (poly.getWeaponEquipFlg() & flg);
			}
			return true;

		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
		return true;
	}

	// 指定的 polyId 能否裝備 armorType 防具？
	public static boolean isEquipableArmor(final int polyId, final int armorType) {
		try {
			final L1PolyMorph poly = PolyTable.get().getTemplate(polyId);
			if (poly == null) {
				return true;
			}

			final Integer flg = armorFlgMap.get(armorType);
			if (flg != null) {
				return 0 != (poly.getArmorEquipFlg() & flg);
			}
			return true;

		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
		return true;
	}

	// 指定的 polyId 為何種變身，且是否可被變身？
	public static boolean isMatchCause(final int polyId, final int cause) {
		try {
			final L1PolyMorph poly = PolyTable.get().getTemplate(polyId);
			if (poly == null) {
				return true;
			}
			if (cause == MORPH_BY_LOGIN) {
				return true;
			}
			return 0 != (poly.getCauseFlg() & cause);

		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
		return false;
	}

	/**
	 * 同步客戶端攻擊距離（{@link S_WeaponRange}）。
	 * <p>
	 * 弓／鐵手甲 DB {@code range=-1} 時，客戶端須收到遠距設定，否則會以近戰（1 格）自動走位後才攻擊。
	 * </p>
	 */
	public static void syncWeaponRangeToClient(final L1PcInstance pc) {
		if (pc == null) {
			return;
		}
		final L1ItemInstance weapon = pc.getWeapon();
		if (weapon == null) {
			pc.sendPackets(new S_WeaponRange(1));
			return;
		}
		if (checkWeaponType(pc.getTempCharGfx(), weapon.getItem().getType())) {
			pc.sendPackets(new S_WeaponRange(1));
			return;
		}
		int range = weapon.getItem().getRange();
		final int type1 = weapon.getItem().getType1();
		if ((type1 == 20 || type1 == 62) && range <= 0) {
			range = -1;
		}
		pc.sendPackets(new S_WeaponRange(range));
	}

	/**
	 * 死騎拿鎖鍊劍
	 * 
	 * @param polyId
	 * @param weaponType
	 * @return
	 */
	public static boolean checkWeaponType(final int polyId, final int weaponType) {
		if (weaponType == 18) {
			if (polyId == 6142 || polyId == 6137 || polyId == 6147 || polyId == 6152 || polyId == 6157 || polyId == 9205 || polyId == 9206 || polyId == 11443 || polyId == 11614 || polyId == 11616 || polyId == 11631 || polyId == 11653) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 個人商店外觀處理
	 * polyIndex 1~8: 採用 PolyList 對應變身外觀
	 * polyIndex 0 或無效: 採用原始角色外觀，僅執行 ACTION_Shop(70)
	 *
	 * @param cha 角色
	 * @param polyIndex 客戶端選擇的外觀索引 (0=使用原始外觀, 1~8=PolyList)
	 */
	public static void doPolyPraivateShop(L1Character cha, int polyIndex) {
		if ((cha == null) || (cha.isDead())) {
			return;
		}
		if ((cha instanceof L1PcInstance)) {
			L1PcInstance pc = (L1PcInstance) cha;
			final int actionShop = 70; // ActionCodes.ACTION_Shop

			if (polyIndex >= 1 && polyIndex <= 8) {
				// 客戶端有選擇 PolyList ID，執行變身
				int shoppolayid = PRIVATE_SHOP_POLY_LIST[polyIndex - 1];
				if (pc.getWeapon() != null) {
					pc.getInventory().setEquipped(pc.getWeapon(), false, false, false);
				}
				doPoly(pc, shoppolayid, 0, 1);
			}
			// 無論變身與否，廣播商店動作
			pc.sendPacketsAll(new S_CharVisualUpdate(pc.getId(), actionShop));
		}
	}

	/**
	 * Applies only the offline-shop appearance. Unlike a gameplay polymorph this
	 * does not create SHAPE_CHANGE, a timer, an icon, or PolyAbility stats.
	 */
	public static void applyOfflineShopAppearance(final L1PcInstance pc, final int polyIndex) {
		if (pc == null || pc.isDead()) {
			return;
		}
		if (polyIndex >= 1 && polyIndex <= PRIVATE_SHOP_POLY_LIST.length) {
			if (pc.getWeapon() != null) {
				pc.getInventory().setEquipped(pc.getWeapon(), false, false, false);
			}
			final int shopPolyId = PRIVATE_SHOP_POLY_LIST[polyIndex - 1];
			pc.setTempCharGfx(shopPolyId);
			pc.setCurrentWeapon(0);
			if (!pc.isGmInvis() && !pc.isInvisble()) {
				pc.broadcastPacketAll(new S_ChangeShape(pc, shopPolyId));
			}
		} else {
			final int classId = pc.getClassId();
			pc.setTempCharGfx(classId);
			final L1ItemInstance weapon = pc.getWeapon();
			pc.setCurrentWeapon(weapon == null ? 0 : pc.getWeaponType(weapon.getItem().getType1()));
			if (!pc.isGmInvis() && !pc.isInvisble()) {
				pc.broadcastPacketAll(new S_ChangeShape(pc, classId));
			}
		}
		if (!pc.isGmInvis() && !pc.isInvisble()) {
			pc.broadcastPacketAll(new S_CharVisualUpdate(pc.getId(), 70));
		}
	}
}
