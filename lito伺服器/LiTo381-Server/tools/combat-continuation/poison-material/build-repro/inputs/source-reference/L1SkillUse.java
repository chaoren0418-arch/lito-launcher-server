package com.lineage.server.model.skill;

import static com.lineage.server.model.skill.L1SkillId.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lineage.config.ConfigCharSetting;
import com.lineage.config.ConfigSkill;
import com.lineage.server.ActionCodes;
import com.lineage.server.clientpackets.AcceleratorChecker;
import com.lineage.server.datatables.SkillCounterMagicExceptionTable;
import com.lineage.server.datatables.SkillCoexistEventTable;
import com.lineage.server.datatables.SkillsTable;
import com.lineage.server.datatables.WandPolyTable;
import com.lineage.server.model.L1CastleLocation;
import com.lineage.server.model.L1Character;
import com.lineage.server.model.ClientActionTimeline.Reservation;
import com.lineage.server.model.L1Magic;
import com.lineage.server.model.L1Object;
import com.lineage.server.model.L1PcInventory;
import com.lineage.server.model.L1PolyMorph;
import com.lineage.server.model.L1Teleport;
import com.lineage.server.model.L1War;
import com.lineage.server.model.PlayerActionLock;
import com.lineage.server.model.Instance.L1CrownInstance;
import com.lineage.server.model.Instance.L1DeInstance;
import com.lineage.server.model.Instance.L1DollInstance;
import com.lineage.server.model.Instance.L1DoorInstance;
import com.lineage.server.model.Instance.L1DwarfInstance;
import com.lineage.server.model.Instance.L1EffectInstance;
import com.lineage.server.model.Instance.L1FieldObjectInstance;
import com.lineage.server.model.Instance.L1FurnitureInstance;
import com.lineage.server.model.Instance.L1GuardInstance;
import com.lineage.server.model.Instance.L1HousekeeperInstance;
import com.lineage.server.model.Instance.L1ItemInstance;
import com.lineage.server.model.Instance.L1MerchantInstance;
import com.lineage.server.model.Instance.L1MonsterInstance;
import com.lineage.server.model.Instance.L1NpcInstance;
import com.lineage.server.model.Instance.L1PcInstance;
import com.lineage.server.model.Instance.L1PetInstance;
import com.lineage.server.model.Instance.L1ScarecrowInstance;
import com.lineage.server.model.Instance.L1SummonInstance;
import com.lineage.server.model.Instance.L1TeleporterInstance;
import com.lineage.server.model.Instance.L1TowerInstance;
import com.lineage.server.model.combat.DamageCommitOutcome;
import com.lineage.server.model.combat.DamageCommitPlan;
import com.lineage.server.model.poison.L1DamagePoison;
import com.lineage.server.model.skill.skillmode.SkillMode;
import com.lineage.server.utils.TeleportTrace;
import com.lineage.server.serverpackets.S_AttackPacketPc;
import com.lineage.server.serverpackets.S_ChangeHeading;
import com.lineage.server.serverpackets.S_Chat;
import com.lineage.server.serverpackets.S_Dexup;
import com.lineage.server.serverpackets.S_DoActionGFX;
import com.lineage.server.serverpackets.S_Invis;
import com.lineage.server.serverpackets.S_MagicEquipment;
import com.lineage.server.serverpackets.S_NpcChat;
import com.lineage.server.serverpackets.S_OwnCharAttrDef;
import com.lineage.server.serverpackets.S_OwnCharStatus;
import com.lineage.server.serverpackets.S_PacketBox;
import com.lineage.server.serverpackets.S_PacketBoxIconAura;
import com.lineage.server.serverpackets.S_Paralysis;
import com.lineage.server.serverpackets.S_Poison;
import com.lineage.server.serverpackets.S_RangeSkill;
import com.lineage.server.serverpackets.S_RemoveObject;
import com.lineage.server.serverpackets.S_SPMR;
import com.lineage.server.serverpackets.S_ServerMessage;
import com.lineage.server.serverpackets.S_ShowPolyList;
import com.lineage.server.serverpackets.S_SkillBrave;
import com.lineage.server.serverpackets.S_SkillHaste;
import com.lineage.server.serverpackets.S_SkillIconShield;
import com.lineage.server.serverpackets.S_SkillSound;
import com.lineage.server.serverpackets.S_Sound;
import com.lineage.server.serverpackets.S_Strup;
import com.lineage.server.serverpackets.S_UseAttackSkill;
import com.lineage.server.templates.L1Npc;
import com.lineage.server.templates.L1Skills;
import com.lineage.server.thread.GeneralThreadPool;
import com.lineage.server.utils.CheckUtil;
import com.lineage.server.utils.L1SpawnUtil;
import com.lineage.server.world.World;
import com.lineage.server.world.WorldTrap;
import com.lineage.server.world.WorldWar;

/**
 * 技能施放判斷
 * @author dexc
 *
 */
public class L1SkillUse {

	private static final Logger _log = LogManager.getLogger(L1SkillUse.class);

	private static final AtomicLong CHECK_COUNT = new AtomicLong();
	private static final AtomicLong HANDLE_COUNT = new AtomicLong();
	private static final AtomicLong AUTO_HUNT_HANDLE_COUNT = new AtomicLong();
	private static final AtomicLong RUN_SKILL_COUNT = new AtomicLong();
	private static final AtomicLong SIDE_EFFECT_FLUSH_COUNT = new AtomicLong();
	private static final AtomicLong ASYNC_SUBMIT_COUNT = new AtomicLong();
	private static final AtomicLong ASYNC_ACTIVE_COUNT = new AtomicLong();
	private static final AtomicLong ASYNC_COMPLETED_COUNT = new AtomicLong();
	private static final AtomicLong CREATE_ZOMBIE_ASYNC_COUNT = new AtomicLong();
	private static final AtomicLong ASYNC_RETRY_COUNT = new AtomicLong();
	private static final AtomicLong ASYNC_TERMINAL_FAILURE_COUNT = new AtomicLong();
	private static final AtomicLong SKIP_COUNT = new AtomicLong();
	private static final AtomicLong FAIL_COUNT = new AtomicLong();
	private static final AtomicLong REJECTED_COUNT = new AtomicLong();
	private static final AtomicLong ERROR_COUNT = new AtomicLong();
	private static final AtomicLong MATERIAL_COMMIT_COUNT = new AtomicLong();
	private static final AtomicLong MATERIAL_COMMIT_FAILURE_COUNT = new AtomicLong();
	private static final int ASYNC_MAX_SUBMIT_ATTEMPTS = 3;
	private static final long ASYNC_RETRY_BASE_MILLIS = 50L;
	public static final int TYPE_NORMAL = 0;
	public static final int TYPE_LOGIN = 1;
	public static final int TYPE_SPELLSC = 2;
	public static final int TYPE_NPCBUFF = 3;
	public static final int TYPE_GMBUFF = 4;
	public static final int TYPE_PROCS = 5;

	private L1Skills _skill;
	private int _skillId;
	private int _getBuffDuration;// 技能時間
	private int _shockStunDuration;
	private int _getBuffIconDuration;// 技能圖示時間
	private int _targetID;
	private int _mpConsume = 0;
	private int _hpConsume = 0;
	private int _targetX = 0;
	private int _targetY = 0;
	private int _dmg = 0;// 傷害
	private int _skillTime = 0;
	private int _type = 0;
	private boolean _isPK = false;
	private int _bookmarkId = 0;
	private int _itemobjid = 0;
	private boolean _checkedUseSkill = false; // 是否已事前檢查
	private int _leverage = 10; // 1/10倍率，10表示1倍
	private boolean _isFreeze = false;
	private boolean _isCounterMagic = true;
	private boolean _isGlanceCheckFail = false;
	/** 光裂術爆擊（送封包時使用 ConfigSkill.CRITICAL_SPR 特效） */
	private boolean _disintegrateCrit = false;
	/** 武器／防具 Proc 施放：略過視線障礙判定，目標以 Proc 命中對象為準 */
	private boolean _procCast = false;
	private boolean _castMotionSent = false;
	/** Single-target damaging impact is released by the accepted target commit. */
	private boolean _deferSingleTargetImpactGrfx = false;
	/** Set only on the detached snapshot executed by DamageCommitPlan. */
	private boolean _releaseDeferredImpactGrfx = false;
	/** True only after this cast's required material passed DB-first commit. */
	private boolean _materialConsumedForCast = false;
	/** Distinguishes no-material/GM casts from a forgotten pre-effect debit. */
	private boolean _materialDebitResolvedForCast = false;
	/** One-shot packet state that becomes visible only after normal-cast admission. */
	private Runnable _acceptedCastStatePublisher;

	public void setAcceptedCastStatePublisher(final Runnable publisher) {
		_acceptedCastStatePublisher = publisher;
	}

	private void publishAcceptedCastState() {
		final Runnable publisher = _acceptedCastStatePublisher;
		_acceptedCastStatePublisher = null;
		if (publisher != null) {
			try {
				publisher.run();
			} catch (final RuntimeException e) {
				// Admission has already committed material/action/reuse state.  An
				// auxiliary packet-state publication must not turn that irreversible
				// success into a cast that silently never executes.
				ERROR_COUNT.incrementAndGet();
				_log.error("Accepted cast state publication failed skillId={} player={}",
						_skillId, _player != null ? _player.getName() : "unknown", e);
			}
		}
	}

	/**執行者*/
	private L1Character _user = null;

	/**執行者為pc*/
	private L1PcInstance _player = null;
	
	/**執行者為npc*/
	private L1NpcInstance _npc = null;
	
	/**目標*/
	private L1Character _target = null;
	
	/**目標為NPC*/
	private L1NpcInstance _targetNpc = null;

	private int _calcType;
	private static final int PC_PC = 1;
	private static final int PC_NPC = 2;
	private static final int NPC_PC = 3;
	private static final int NPC_NPC = 4;

	private ArrayList<TargetStatus> _targetList;

	/** D-2e：runSkill 目標 SideEffects 佇列（Calc 後由同一動作順序線 flush）。 */
	private final ArrayList<Runnable> _targetSideEffectQueue = new ArrayList<>();

	private interface MonitoredAsyncTask extends Runnable {
		void onTerminalFailure();
	}

	private static void executeMonitored(final String taskName, final MonitoredAsyncTask task) {
		if (task == null) {
			SKIP_COUNT.incrementAndGet();
			return;
		}
		ASYNC_SUBMIT_COUNT.incrementAndGet();
		final Runnable monitored = () -> {
			ASYNC_ACTIVE_COUNT.incrementAndGet();
			try {
				task.run();
			} catch (final RuntimeException e) {
				ERROR_COUNT.incrementAndGet();
				_log.error("L1SkillUse async task failed: {}", taskName, e);
			} finally {
				ASYNC_COMPLETED_COUNT.incrementAndGet();
				ASYNC_ACTIVE_COUNT.decrementAndGet();
			}
		};
		submitMonitored(taskName, task, monitored, 1);
	}

	/**
	 * Heavy work is admitted only by SlowPool.  A rejection schedules a short,
	 * bounded re-submit callback; the callback never executes the heavy task and
	 * terminal failure is explicit.  Using the raw executor deliberately bypasses
	 * GeneralThreadPool's packet-lane compatibility inline path.
	 */
	private static void submitMonitored(final String taskName,
			final MonitoredAsyncTask task, final Runnable monitored, final int attempt) {
		try {
			GeneralThreadPool.getExecutor().execute(monitored);
			return;
		} catch (final RejectedExecutionException failure) {
			REJECTED_COUNT.incrementAndGet();
			_log.warn("L1SkillUse SlowPool rejected task={} attempt={}/{}",
					taskName, attempt, ASYNC_MAX_SUBMIT_ATTEMPTS, failure);
		} catch (final RuntimeException failure) {
			REJECTED_COUNT.incrementAndGet();
			_log.warn("L1SkillUse SlowPool submit failed task={} attempt={}/{}",
					taskName, attempt, ASYNC_MAX_SUBMIT_ATTEMPTS, failure);
		}

		if (attempt >= ASYNC_MAX_SUBMIT_ATTEMPTS) {
			terminalAsyncFailure(taskName, task, attempt, null);
			return;
		}
		final long retryDelay = ASYNC_RETRY_BASE_MILLIS << Math.max(0, attempt - 1);
		try {
			GeneralThreadPool.get().pcSchedule(
					() -> submitMonitored(taskName, task, monitored, attempt + 1), retryDelay);
			ASYNC_RETRY_COUNT.incrementAndGet();
		} catch (final RuntimeException scheduleFailure) {
			REJECTED_COUNT.incrementAndGet();
			terminalAsyncFailure(taskName, task, attempt, scheduleFailure);
		}
	}

	private static void terminalAsyncFailure(final String taskName,
			final MonitoredAsyncTask task, final int attempts, final RuntimeException cause) {
		ASYNC_TERMINAL_FAILURE_COUNT.incrementAndGet();
		ERROR_COUNT.incrementAndGet();
		_log.error("L1SkillUse async task terminal failure task={} attempts={}",
				taskName, attempts, cause);
		try {
			task.onTerminalFailure();
		} catch (final RuntimeException callbackFailure) {
			ERROR_COUNT.incrementAndGet();
			_log.error("L1SkillUse async terminal callback failed task={}",
					taskName, callbackFailure);
		}
	}

	/** Immutable async request; never retains the reusable L1SkillUse calculator. */
	private record CreateZombieRequest(
			L1NpcInstance targetNpc,
			L1PcInstance player) implements MonitoredAsyncTask {

		@Override
		public void run() {
			if (!isCurrentSession() || targetNpc == null || !targetNpc.isDead()
					|| World.get().findObject(targetNpc.getId()) != targetNpc) {
				SKIP_COUNT.incrementAndGet();
				return;
			}
			if (!player.canTakePet(6)) {
				player.sendPackets(new S_ServerMessage(319));
				return;
			}
			try {
				final L1SummonInstance summon = new L1SummonInstance(
						targetNpc, player, true);
				summon.initialize();
			} catch (final Exception failure) {
				ERROR_COUNT.incrementAndGet();
				_log.error("Create zombie async execution failed playerId={} targetId={}",
						player.getId(), targetNpc.getId(), failure);
			}
		}

		@Override
		public void onTerminalFailure() {
			if (isCurrentSession()) {
				player.sendPackets(new S_ServerMessage(280));
			}
		}

		private boolean isCurrentSession() {
			return player != null && !player.isDead()
					&& World.get().findObject(player.getId()) == player
					&& World.get().getPlayer(player.getName()) == player;
		}
	}

	private static final int[] EXCEPT_COUNTER_MAGIC = { 1, 2, 3, 5, 8, 9, 12,
		13, 14, 19, 21, 26, 31, 32, 35, 37, 42, 43, 44, 48, 49, 52, 54, 55,
		57, 60, 61, 63, 67, 68, 69, 72, 73, 75, 78, 79, SHOCK_STUN,
		REDUCTION_ARMOR, BOUNCE_ATTACK, SOLID_CARRIAGE, COUNTER_BARRIER,
		97, 98, 99, 100, 101, 102, 104, 105, 106, 107, 109, 110, 111, ARMOR_BREAK, 113,
		114, 115, 116, 117, 118, 129, 130, 131, 132, 134, 137, 138, 146,
		147, 148, 149, 150, 151, 155, 156, 158, 159, 161, 163, 164, 165,
		166, 168, 169, 170, 171, SOUL_OF_FLAME, ADDITIONAL_FIRE,
		DRAGON_SKIN, AWAKEN_ANTHARAS, AWAKEN_FAFURION, AWAKEN_VALAKAS,
		MIRROR_IMAGE, ILLUSION_OGRE, ILLUSION_LICH, PATIENCE, 10026, 10027,
		ILLUSION_DIA_GOLEM, INSIGHT, ILLUSION_AVATAR, 10028, 10029 };

	/**
	 * 攻擊倍率(1/10)
	 */
	public void setLeverage(final int i) {
		this._leverage = i;
	}

	/**
	 * 攻擊倍率(1/10)
	 * @return
	 */
	public int getLeverage() {
		return this._leverage;
	}

	private boolean isCheckedUseSkill() {
		return this._checkedUseSkill;
	}

	private void setCheckedUseSkill(final boolean flg) {
		this._checkedUseSkill = flg;
	}

	/** Proc 專用：略過 {@link #isTarget}／{@link #makeTargetList} 視線檢查。 */
	public void setProcCast(final boolean procCast) {
		this._procCast = procCast;
	}

	/**
	 * 
	 * @param player 攻擊者為PC
	 * @param skillid 技能編號
	 * @param target_id 目標OBJID
	 * @param x X座標
	 * @param y Y座標
	 * @param time 時間
	 * @param type 類型
	 * @param attacker 攻擊者為NPC
	 * @return
	 */
	public boolean checkUseSkill(final L1PcInstance player, final int skillid,
			final int target_id, final int x, final int y, final int time, final int type,
			final L1Character attacker) {
		CHECK_COUNT.incrementAndGet();
		// 初期設定開始
		setCheckedUseSkill(true);
		_targetList = new ArrayList<TargetStatus>(); // 目標清單初始化

		_target = null;
		_targetNpc = null;
		_isPK = false;
		_skill = SkillsTable.get().getTemplate(skillid);
		if (_skill == null) {
			SKIP_COUNT.incrementAndGet();
			return false;
		}
		_skillId = skillid;
		_targetX = x;
		_targetY = y;
		_skillTime = time;
		_type = type;
		boolean checkedResult = true;

		if (attacker == null) {
			// pc
			if (player == null) {
				SKIP_COUNT.incrementAndGet();
				return false;
			}
			_player = player;
			_user = _player;
			
		} else {
			// npc
			if (!(attacker instanceof L1NpcInstance)) {
				SKIP_COUNT.incrementAndGet();
				return false;
			}
			_npc = (L1NpcInstance) attacker;
			_user = _npc;
		}

		if (_skill.getTarget().equals("none")) {
			_targetID = _user.getId();
			_targetX = _user.getX();
			_targetY = _user.getY();
			
		} else {
			_targetID = target_id;
		}
		switch (type) {
		case TYPE_NORMAL: // 一般魔法使用時
			checkedResult = this.isNormalSkillUsable();
			break;
			
		case TYPE_SPELLSC: // 魔法卷軸使用時
			/*checkedResult = this.isSpellScrollUsable();
			break;*/
			
		case TYPE_NPCBUFF:
			checkedResult = true;
			break;
		}
		
		if (!checkedResult) {
			return false;
		}

		// 火牆、生命之泉的詠唱對象為座標
		// 立方體在詠唱者座標配置，因此為例外
		// id58火牢 id63治愈能量风暴
		if ((_skillId == FIRE_WALL) || (_skillId == LIFE_STREAM)) {
			return true;
		}

		final L1Object object = World.get().findObject(_targetID);
		if (object instanceof L1ItemInstance) {
			return false;
		}
		if (!(object instanceof L1Character)) {
			return false;
		}
		final L1Character targetCharacter = (L1Character) object;
		if (_user instanceof L1PcInstance) {
			if (object instanceof L1PcInstance) {
				_calcType = PC_PC;
				
			} else if (object instanceof L1NpcInstance) {
				_calcType = PC_NPC;
				_targetNpc = (L1NpcInstance) object;
			} else {
				return false;
			}
			
		} else if (_user instanceof L1NpcInstance) {
			if (object instanceof L1PcInstance) {
				_calcType = NPC_PC;
				
			} else if (_skill.getTarget().equals("none")) {
				_calcType = NPC_PC;
				
			} else if (object instanceof L1NpcInstance) {
				_calcType = NPC_NPC;
				_targetNpc = (L1NpcInstance) object;
			} else {
				return false;
			}
		}

		switch (_skillId) {
		// 可使用傳送戒指技能
		case TELEPORT:
		case MASS_TELEPORT:
			_bookmarkId = target_id;
			break;
		
		// 技能對象為道具
		case CREATE_MAGICAL_WEAPON:
		case BRING_STONE:
		case BLESSED_ARMOR:
		case ENCHANT_WEAPON:
		case SHADOW_FANG:
			_itemobjid = target_id;
			break;
		}
		
		_target = targetCharacter;

		if (!(_target instanceof L1MonsterInstance)
				&& _skill.getTarget().equals("attack")
				&& (_user.getId() != target_id)) {
			_isPK = true; // 目標非怪物且為攻擊技能、且非自身時設為PK模式
		}

		// 初期設定結束

		// 事前檢查
		if (!(object instanceof L1Character)) { // 目標非角色時不處理
			checkedResult = false;
		}
		
		// 技能發動 目標清單判定
		makeTargetList();

		if ((_user instanceof L1PcInstance)
				&& com.lineage.server.model.L1DeathMatch.isPeaceHarmfulMagicBlocked(_player, _skill, _targetList)) {
			_player.sendPackets(new S_ServerMessage(280)); // \f1魔法失敗了。
			return false;
		}
		
		if ((_targetList.size() == 0) 
				&& (_user instanceof L1NpcInstance)) {
			checkedResult = false;
		}
		// 事前檢查結束
		return checkedResult;
	}

	/**
	 * 一般技能使用時，根據使用者狀態判斷技能是否可使用
	 *
	 * @return false 技能無法使用的狀態時 在以下情況下不可使用技能
	 */
	private boolean isNormalSkillUsable() {
		// 技能使用者為PC時的檢查
		if (this._user instanceof L1PcInstance) {
			final L1PcInstance pc = (L1PcInstance) this._user;

			if (!this.isAttrAgrees()) { // 精靈魔法時，屬性不符則不處理
				return false;
			}

			// 屬性防護：只要已選擇至少一種屬性即可施放
			if ((this._skillId == ELEMENTAL_PROTECTION) && (pc.getElfAttr() == 0)) {
				pc.sendPackets(new S_ServerMessage(280)); // \f1魔法失敗了。
				return false;
			}

			// 崩壞術僅在性向值達標時可用
			if ((this._skillId == DISINTEGRATE) && (pc.getLawful() < 500)) {
				// 此訊息是否正確待確認
				pc.sendPackets(new S_ServerMessage(352, "$967")); // 使用此魔法需要性向值達%0。
				return false;
			}

			// 相同立方體在效果範圍外則可配置
			if ((this._skillId == CUBE_IGNITION) || 
					(this._skillId == CUBE_QUAKE) || 
					(this._skillId == CUBE_SHOCK) || 
					(this._skillId == CUBE_BALANCE)) {
				boolean isNearSameCube = false;
				int gfxId = 0;
				for (final L1Object obj : World.get().getVisibleObjects(pc, 3)) {
					if (obj instanceof L1EffectInstance) {
						final L1EffectInstance effect = (L1EffectInstance) obj;
						gfxId = effect.getGfxId();
						if (((this._skillId == CUBE_IGNITION) && (gfxId == 6706))
								|| ((this._skillId == CUBE_QUAKE) && (gfxId == 6712))
								|| ((this._skillId == CUBE_SHOCK) && (gfxId == 6718))
								|| ((this._skillId == CUBE_BALANCE) && (gfxId == 6724))) {
							isNearSameCube = true;
							break;
						}
					}
				}
				if (isNearSameCube) {
					pc.sendPackets(new S_ServerMessage(1412)); // 已在地板上召喚了魔法立方塊。
					return false;
				}
			}

			// 覺醒狀態下僅能使用覺醒技能（外控：DragonKnightAwakeClassicMode=true 仿正）
			if (ConfigCharSetting.isDragonKnightAwakeClassicMode()) {
				if (((pc.getAwakeSkillId() == AWAKEN_ANTHARAS)
						&& (this._skillId != AWAKEN_ANTHARAS) && (this._skillId != MAGMA_BREATH)
						&& (this._skillId != SHOCK_SKIN) && (this._skillId != FREEZING_BREATH)
						&& (this._skillId != THUNDER_GRAB))
						|| ((pc.getAwakeSkillId() == AWAKEN_FAFURION)
								&& (this._skillId != AWAKEN_FAFURION) && (this._skillId != MAGMA_BREATH)
								&& (this._skillId != SHOCK_SKIN) && (this._skillId != FREEZING_BREATH)
								&& (this._skillId != THUNDER_GRAB))
								|| ((pc.getAwakeSkillId() == AWAKEN_VALAKAS)
										&& (this._skillId != AWAKEN_VALAKAS) && (this._skillId != MAGMA_BREATH)
										&& (this._skillId != SHOCK_SKIN) && (this._skillId != FREEZING_BREATH)
										&& (this._skillId != THUNDER_GRAB))) {
					pc.sendPackets(new S_ServerMessage(1385)); // 目前狀態中無法使用覺醒魔法。
					return false;
				}
			}

			if ((this.isItemConsume() == false) && !this._player.isGm()) { // 消耗道具是否足夠
				this._player.sendPackets(new S_ServerMessage(299)); // \f1施放魔法所需材料不足。
				return false;
			}
		}
		// 技能使用者為NPC時的檢查
		else if (this._user instanceof L1NpcInstance) {

			// 沉默狀態下無法使用
			if (this._user.hasSkillEffect(SILENCE)) {
				// NPC處於沉默時僅取消當次使用
				this._user.removeSkillEffect(SILENCE);
				return false;
			}
		}

		// PC、NPC共通檢查
		if (!this.isHPMPConsume()) { // HP、MP消耗是否足夠
			return false;
		}
		return true;
	}

	/**
	 * pc 用技能施放判斷
	 * @param player
	 * @param skillId
	 * @param targetId
	 * @param x
	 * @param y
	 * @param timeSecs 秒
	 * @param type
	 */
	public boolean handleCommands(final L1PcInstance player, final int skillId, final int targetId,
			final int x, final int y, final int timeSecs, final int type) {
		return this.handleCommands(player, skillId, targetId, x, y, timeSecs, type, null);
	}

	/**
	 * D-2e：清空目標 SideEffects 佇列（每次 handleCommands 開始前）。
	 */
	private void clearTargetSideEffects() {
		_targetSideEffectQueue.clear();
	}

	/**
	 * D-2e：執行已佇列的目標 SideEffects。
	 */
	private void flushTargetSideEffects() {
		if (_targetSideEffectQueue.isEmpty()) {
			SKIP_COUNT.incrementAndGet();
			return;
		}
		SIDE_EFFECT_FLUSH_COUNT.incrementAndGet();
		try {
			for (final Runnable task : _targetSideEffectQueue) {
				if (task == null) {
					SKIP_COUNT.incrementAndGet();
					continue;
				}
				try {
					task.run();
				} catch (final RuntimeException e) {
					ERROR_COUNT.incrementAndGet();
					_log.error("L1SkillUse side-effect task failed skillId={}", _skillId, e);
				}
			}
		} finally {
			_targetSideEffectQueue.clear();
		}
	}

	/**
	 * D-2e：enqueue 單一目標 commit 後 SideEffects。
	 */
	private void queueTargetSideEffects(final L1Character cha, final int heal,
			final SkillTargetMutationPlan mutationPlan) {
		if (cha == null) {
			return;
		}
		final L1Character target = cha;
		final L1SkillUse castSnapshot = detachedTargetSideEffectSnapshot();
		final SkillTargetMutationPlan acceptedMutations = mutationPlan == null
				? SkillTargetMutationPlan.none() : mutationPlan;
		_targetSideEffectQueue.add(() -> {
			acceptedMutations.apply(castSnapshot, target);
			castSnapshot.applyTargetSideEffects(target, heal);
		});
	}

	/** Snapshot a damaging skill's cross-character effects for target-lane release. */
	private DamageCommitPlan acceptedTargetSideEffectPlan(final L1Character target,
			final int heal, final SkillTargetMutationPlan mutationPlan) {
		final L1PcInstance talentAttacker = _user instanceof L1PcInstance pc ? pc : null;
		final boolean releaseImpactGrfx = shouldDeferSingleTargetImpactGrfx(target);
		if (releaseImpactGrfx) {
			_deferSingleTargetImpactGrfx = true;
		}
		return new SkillTargetCommitPlan(detachedTargetSideEffectSnapshot(target, releaseImpactGrfx), target,
				heal, talentAttacker, _skillId,
				mutationPlan == null ? SkillTargetMutationPlan.none() : mutationPlan,
				releaseImpactGrfx);
	}

	private boolean shouldDeferSingleTargetImpactGrfx(final L1Character target) {
		return _type == TYPE_NORMAL && target != null && target == _target
				&& _skill != null && _skill.getCastGfx() != 0
				&& _skill.getArea() == 0 && _skill.getTarget().equals("attack")
				&& _targetList != null && _targetList.size() == 1;
	}

	/**
	 * L1SkillUse instances are reused by some bulk/NPC callers.  The target lane
	 * must therefore retain a detached cast snapshot rather than this mutable
	 * calculator.
	 */
	private L1SkillUse detachedTargetSideEffectSnapshot() {
		return detachedTargetSideEffectSnapshot(null, false);
	}

	private L1SkillUse detachedTargetSideEffectSnapshot(
			final L1Character impactTarget, final boolean releaseImpactGrfx) {
		final L1SkillUse snapshot = new L1SkillUse();
		snapshot._skill = _skill;
		snapshot._skillId = _skillId;
		snapshot._getBuffDuration = _getBuffDuration;
		snapshot._shockStunDuration = _shockStunDuration;
		snapshot._getBuffIconDuration = _getBuffIconDuration;
		snapshot._skillTime = _skillTime;
		snapshot._type = _type;
		snapshot._isFreeze = _isFreeze;
		snapshot._procCast = _procCast;
		snapshot._user = _user;
		snapshot._player = _player;
		snapshot._npc = _npc;
		if (releaseImpactGrfx) {
			snapshot._target = impactTarget;
			snapshot._targetX = _targetX;
			snapshot._targetY = _targetY;
			snapshot._dmg = _dmg;
			snapshot._disintegrateCrit = _disintegrateCrit;
			snapshot._castMotionSent = _castMotionSent;
			snapshot._releaseDeferredImpactGrfx = true;
			snapshot._targetList = new ArrayList<>();
			final TargetStatus impactStatus = new TargetStatus(impactTarget);
			impactStatus.setDamage(_dmg);
			snapshot._targetList.add(impactStatus);
		}
		return snapshot;
	}

	private record SkillTargetCommitPlan(
			L1SkillUse castSnapshot,
			L1Character target,
			int heal,
			L1PcInstance talentAttacker,
			int skillId,
			SkillTargetMutationPlan mutationPlan,
			boolean releaseImpactGrfx) implements DamageCommitPlan {

		@Override
		public void afterAccepted(final DamageCommitOutcome outcome) {
			if (outcome == null || !outcome.accepted() || castSnapshot == null || target == null) {
				return;
			}
			if (heal > 0 && castSnapshot._user != null) {
				castSnapshot._user.adjustCurrentHpIfAlive(heal, 0);
			}
			if (!outcome.terminal()) {
				try {
					mutationPlan.apply(castSnapshot, target);
				} catch (final RuntimeException failure) {
					ERROR_COUNT.incrementAndGet();
					_log.error("Accepted skill target mutation failed skillId={}", skillId, failure);
				}
			}
			if (!outcome.terminal() && talentAttacker != null) {
				try {
					com.lineage.server.model.talent.TalentDamageCalculator.tryTalentStun(
							talentAttacker, target, skillId);
				} catch (final RuntimeException failure) {
					ERROR_COUNT.incrementAndGet();
					_log.error("Accepted skill talent stun failed skillId={}", skillId, failure);
				}
			}
			if (!outcome.terminal()) {
				try {
					castSnapshot.applyTargetSideEffects(target, 0);
				} catch (final RuntimeException failure) {
					ERROR_COUNT.incrementAndGet();
					_log.error("Accepted skill target effects failed skillId={}", skillId, failure);
				}
			}
			if (releaseImpactGrfx) {
				try {
					castSnapshot.sendGrfx(true);
				} catch (final RuntimeException failure) {
					ERROR_COUNT.incrementAndGet();
					_log.error("Accepted skill impact visual failed skillId={}", skillId, failure);
				}
			}
		}
	}

	/**
	 * Cross-target mutations calculated before admission and released only by the
	 * target lane.  The ERASE_MAGIC timer identity prevents an accepted but delayed
	 * hit from deleting a newer recast (ABA).
	 */
	private record SkillTargetMutationPlan(
			L1SkillTimer eraseMagicLease,
			boolean freeze,
			int thunderGrabMillis,
			boolean shockStun) {

		private SkillTargetMutationPlan {
			thunderGrabMillis = Math.max(0, thunderGrabMillis);
		}

		private static SkillTargetMutationPlan none() {
			return new SkillTargetMutationPlan(null, false, 0, false);
		}

		private void apply(final L1SkillUse castSnapshot, final L1Character target) {
			if (castSnapshot == null || target == null) {
				return;
			}
			if (eraseMagicLease != null) {
				target.killSkillEffectTimer(ERASE_MAGIC, eraseMagicLease);
			}
			if (freeze) {
				castSnapshot.applyFreezeTargetEffect(target);
			}
			if (thunderGrabMillis > 0) {
				castSnapshot.applyThunderGrabTargetEffect(target, thunderGrabMillis);
			}
			if (shockStun) {
				com.lineage.server.model.skill.skillmode.SHOCK_STUN.applyStunToTarget(target);
			}
		}
	}

	private void applyFreezeTargetEffect(final L1Character cha) {
		if (cha == null || _skill == null || _user == null) {
			return;
		}
		L1SpawnUtil.spawnEffect(81168, _skill.getBuffDuration(), cha.getX(), cha.getY(),
				cha.getMapId(), _user, 0);
		if (cha instanceof L1PcInstance pc) {
			pc.sendPacketsAll(new S_Poison(pc.getId(), 2));
			pc.sendPackets(new S_Paralysis(S_Paralysis.TYPE_FREEZE, true));
		} else if ((cha instanceof L1MonsterInstance)
				|| (cha instanceof L1SummonInstance)
				|| (cha instanceof L1PetInstance)) {
			final L1NpcInstance npc = (L1NpcInstance) cha;
			npc.broadcastPacketAll(new S_Poison(npc.getId(), 2));
			npc.setParalyzed(true);
		}
	}

	private void applyThunderGrabTargetEffect(final L1Character cha, final int millis) {
		if (cha == null || millis <= 0 || cha.hasSkillEffect(L1SkillId.MOVE_STOP)) {
			return;
		}
		cha.setSkillEffect(L1SkillId.MOVE_STOP, millis);
		if (cha instanceof L1PcInstance pc) {
			pc.sendPackets(new S_Paralysis(S_Paralysis.TYPE_BIND, true));
		} else if ((cha instanceof L1MonsterInstance)
				|| (cha instanceof L1SummonInstance)
				|| (cha instanceof L1PetInstance)) {
			((L1NpcInstance) cha).setParalyzed(true);
		}
	}

	/**
	 * D-2e SideEffects：magic.commit 之後的封包 / buff / 施法者回復。
	 */
	private void applyTargetSideEffects(final L1Character cha, final int heal) {
		if (_user == null || cha == null) {
			SKIP_COUNT.incrementAndGet();
			return;
		}
		if (heal > 0) {
			this._user.adjustCurrentHpIfAlive(heal, 0);
		}

		if (cha instanceof L1PcInstance pc) {
			// Proc 走 addMagicList→updateBuffStatus 即可；略過前置狀態封包與 happen 訊息，避免對話欄洗頻
			if (!_procCast) {
				pc.turnOnOffLight();
				pc.sendPackets(new S_OwnCharAttrDef(pc));
				pc.sendPackets(new S_OwnCharStatus(pc));
				pc.sendPackets(new S_PacketBox(S_PacketBox.EVASION_UPDATE, pc.getEr()));
				sendHappenMessage(pc);
			}
		}

		// 隱身類技能必須在送出 S_Invis / S_RemoveObject 前同步建立狀態。
		// runSkill() 已先行套用，避免延後副作用造成視野更新將角色重新顯示。
		if (!isInvisibilitySkill()) {
			addMagicList(cha, false);
		}

		if (cha instanceof L1PcInstance pc) {
			pc.turnOnOffLight();
		}
	}

	/**
	 * 通用技能施放判斷
	 * @param player
	 * @param skillId
	 * @param targetId
	 * @param x
	 * @param y
	 * @param timeSecs
	 * @param type
	 * @param attacker
	 */
	public boolean handleCommands(final L1PcInstance player, final int skillId, final int targetId,
			final int x, final int y, final int timeSecs, final int type,
			final L1Character attacker) {
		HANDLE_COUNT.incrementAndGet();
		clearTargetSideEffects();
		_castMotionSent = false;
		_deferSingleTargetImpactGrfx = false;
		_releaseDeferredImpactGrfx = false;
		_materialConsumedForCast = false;
		_materialDebitResolvedForCast = false;
		try {
			// 是否已做事前檢查？
			if (!isCheckedUseSkill()) {
				final boolean isUseSkill = 
					checkUseSkill(
							player, 
							skillId, 
							targetId, 
							x, 
							y, 
							timeSecs, 
							type, 
							attacker
						);
				
				if (!isUseSkill) {
					failSkill();
					return false;
				}
			}
			switch (type) {
				case TYPE_NORMAL -> { // 魔法詠唱時
					if (!_isGlanceCheckFail || 
							(_skill.getArea() > 0) || 
							_skill.getTarget().equals("none")) {
						// D-2：runSkill = Calc + magic.commit→DamageCommit
						if (!commitMaterialAndAcquireCastLocks()) {
							failSkill();
							return false;
						}
						breakInterruptedCastEffects();
						final boolean invisibilityToggledOff = toggleOffInvisibilityRecast();
						if (!invisibilityToggledOff) {
							breakInvisibilityOnCast();
						}
						sendCastMotion();
						if (!invisibilityToggledOff) {
							runSkill();
						}
						applySideEffectsAfterNormalCast();
					}
				}
				
				case TYPE_LOGIN -> // 登入時（無HPMP材料消耗、無圖示）
					runSkill();

				case TYPE_SPELLSC -> { // 魔法卷軸使用時（無HPMP材料消耗）
					sendCastMotion();
					runSkill();
					flushTargetSideEffects();
					sendGrfx(true);
				}

				case TYPE_GMBUFF -> { // GMBUFF使用時（無HPMP材料消耗、無魔法動作）
					runSkill();
					flushTargetSideEffects();
					sendGrfx(false);
				}

				case TYPE_NPCBUFF -> { // NPCBUFF使用時（無HPMP材料消耗）
					sendCastMotion();
					runSkill();
					flushTargetSideEffects();
					sendGrfx(true);
				}
				
				case TYPE_PROCS -> {
					runSkill();
					flushTargetSideEffects();
				sendGrfx(false);
				}
			}
			return true;
		} catch (final Exception e) {
			ERROR_COUNT.incrementAndGet();
			_log.error(e.getLocalizedMessage(), e);
			return false;
		} finally {
			// checkUseSkill() marks this instance as pre-validated. Clear that
			// marker on every exit, including action-window/reuse-delay rejection,
			// so a reused L1SkillUse can never skip the next cast's validation.
			setCheckedUseSkill(false);
			_acceptedCastStatePublisher = null;
		}
	}

	/**
	 * 一般施法成功進入執行階段時，依外控解除施法者的隱身狀態。
	 */
	private void breakInvisibilityOnCast() {
		if (ConfigSkill.INVIS_CAST_BREAK && (_player != null) && _player.isInvisble()) {
			_player.delInvis();
		}
	}

	/**
	 * Recasting the same invisibility skill is a toggle-off action.  Equipment
	 * invisibility remains owned by the equipped cloak and must be removed by
	 * unequipping it.
	 */
	private boolean toggleOffInvisibilityRecast() {
		if (_player == null || !ConfigSkill.INVIS_CAST_BREAK) {
			return false;
		}
		final boolean invisibilityEquipmentEquipped =
				_player.getInventory().checkEquipped(20077)
				|| _player.getInventory().checkEquipped(120077)
				|| _player.getInventory().checkEquipped(20062);
		final boolean toggleOff = shouldToggleOffInvisibilityRecast(
				_skillId,
				_player.hasSkillEffect(INVISIBILITY),
				_player.hasSkillEffect(BLIND_HIDING),
				invisibilityEquipmentEquipped);
		if (toggleOff) {
			_player.delInvis();
		}
		return toggleOff;
	}

	static boolean shouldToggleOffInvisibilityRecast(final int skillId,
			final boolean hasInvisibility, final boolean hasBlindHiding,
			final boolean invisibilityEquipmentEquipped) {
		if (invisibilityEquipmentEquipped) {
			return false;
		}
		return (skillId == INVISIBILITY && hasInvisibility)
				|| (skillId == BLIND_HIDING && hasBlindHiding);
	}

	/** Mutate cast-breaking buffs only after every admission gate has committed. */
	private void breakInterruptedCastEffects() {
		if (_player == null) {
			return;
		}
		if (_player.hasSkillEffect(ABSOLUTE_BARRIER)) {
			_player.killSkillEffectTimer(ABSOLUTE_BARRIER);
			_player.startHpRegeneration();
			_player.startMpRegeneration();
		}
		_player.killSkillEffectTimer(MEDITATION);
	}

	public boolean handleCommandsForAutoHunt(final L1PcInstance player, final int skillId, final int targetId,
			final int x, final int y, final int timeSecs, final int type) {
		return handleCommandsForAutoHunt(player, skillId, targetId, x, y, timeSecs, type, null);
	}

	public boolean handleCommandsForAutoHunt(final L1PcInstance player, final int skillId, final int targetId,
			final int x, final int y, final int timeSecs, final int type,
			final L1Character attacker) {
		AUTO_HUNT_HANDLE_COUNT.incrementAndGet();
		clearTargetSideEffects();
		_castMotionSent = false;
		_deferSingleTargetImpactGrfx = false;
		_releaseDeferredImpactGrfx = false;
		_materialConsumedForCast = false;
		_materialDebitResolvedForCast = false;
		try {
			if (!isCheckedUseSkill()) {
				final boolean isUseSkill =
					checkUseSkill(
							player,
							skillId,
							targetId,
							x,
							y,
							timeSecs,
							type,
							attacker
							);

				if (!isUseSkill) {
					failSkill();
					return false;
				}
			}
			boolean casted = false;
			switch (type) {
				case TYPE_NORMAL -> {
					if (!_isGlanceCheckFail ||
							(_skill.getArea() > 0) ||
							_skill.getTarget().equals("none")) {
						if (!commitMaterialAndAcquireCastLocks()) {
							failSkill();
							return false;
						}
						breakInterruptedCastEffects();
						final boolean invisibilityToggledOff = toggleOffInvisibilityRecast();
						if (!invisibilityToggledOff) {
							breakInvisibilityOnCast();
						}
						sendCastMotion();
						if (!invisibilityToggledOff) {
							runSkill();
						}
						applySideEffectsAfterNormalCast();
						casted = true;
					}
				}

				case TYPE_LOGIN -> {
					runSkill();
					casted = true;
				}

				case TYPE_SPELLSC -> {
					sendCastMotion();
					runSkill();
					flushTargetSideEffects();
					sendGrfx(true);
					casted = true;
				}

				case TYPE_GMBUFF -> {
					runSkill();
					flushTargetSideEffects();
					sendGrfx(false);
					casted = true;
				}

				case TYPE_NPCBUFF -> {
					sendCastMotion();
					runSkill();
					flushTargetSideEffects();
					sendGrfx(true);
					casted = true;
				}

				case TYPE_PROCS -> {
					runSkill();
					flushTargetSideEffects();
					sendGrfx(false);
					casted = true;
				}
			}
			return casted;

		} catch (final Exception e) {
			ERROR_COUNT.incrementAndGet();
			_log.error(e.getLocalizedMessage(), e);
			return false;

		} finally {
			setCheckedUseSkill(false);
			_acceptedCastStatePublisher = null;
		}
	}

	/**
	 * D-2：TYPE_NORMAL 施法副作用（同一動作順序線）：消耗、圖示、延遲。
	 */
	private void applySideEffectsAfterNormalCast() {
		flushTargetSideEffects();
		useConsume();
		sendGrfx(true);
		sendFailMessageHandle();
	}

	/**
	 * 法術施展失敗的處理
	 */
	private void failSkill() {
		FAIL_COUNT.incrementAndGet();
		setCheckedUseSkill(false);
		if (_player == null) {
			return;
		}
		switch (_skillId) {
			case TELEPORT, MASS_TELEPORT, TELEPORT_TO_MATHER -> // 解除傳送鎖定
				_player.sendPackets(
						new S_Paralysis(S_Paralysis.TYPE_TELEPORT_UNLOCK, false));
		}
	}

	private static boolean isTeleportSkill(final int skillId) {
		return skillId == TELEPORT || skillId == MASS_TELEPORT || skillId == TELEPORT_TO_MATHER;
	}

	private static String explainCastLockReject(final SkillCastAdmission.Result result) {
		return switch (result) {
		case PREFLIGHT_REJECTED ->
			"瞬移施法預檢失敗（技能 CD 中或動作時間軸仍被占用；走路中施放應已豁免，若仍出現請回報）";
		case RESERVATION_REJECTED -> "瞬移施法預約失敗（動作時間軸或技能 reuse 衝突）";
		case MATERIAL_REJECTED -> "瞬移施法失敗：材料不足";
		case ACCEPTED -> "unknown";
		};
	}

	/**
	 * 可加入設置目標的判斷
	 * @param cha 加入判斷的目標物件
	 * @return true:可加入目標 false:不可加入目標
	 * @throws Exception
	 */
	private boolean isTarget(final L1Character cha) throws Exception {
		if (cha == null) {
			return false;
		}
		
		// 副本ID不相等
		if (_user.get_showId() != cha.get_showId()) {
			return false;
		}
		
		if (_npc != null) {
			// 在目標清單中
			if (_npc.isHate(cha)) {
				return true;
			}
			// 施展者是寵物 XXX
			if (_npc instanceof L1PetInstance) {
				if (cha instanceof L1MonsterInstance) {
					return true;
				}
			}
			// 施展者是召喚獸
			if (_npc instanceof L1SummonInstance) {
				if (cha instanceof L1MonsterInstance) {
					return true;
				}
			}
		}
		
		// 該物件是否允許攻擊
		if (!CheckUtil.checkAttackSkill(cha)) {
			return false;
		}

		boolean flg = false;

		// 目標是門
		if (cha instanceof L1DoorInstance) {
			// 目標不可破壞設置
			if ((cha.getMaxHp() == 0) || (cha.getMaxHp() == 1)) {
				return false;
			}
		}

		// 目標是魔法娃娃 拒絕所有技能
		if (cha instanceof L1DollInstance) {
			return false;
		}
		
		// 目標是人物
		if (cha instanceof L1PcInstance) {
			final L1PcInstance pc = (L1PcInstance) cha;
			// 鬼魂模式 以及 GM隱身
			if (pc.isGhost() || pc.isGmInvis()) {
				return false;
			}
		}
		
		// NPC 對 PC
		if (_calcType == NPC_PC) {
			if ((cha instanceof L1PcInstance) || 
					(cha instanceof L1PetInstance) || 
					(cha instanceof L1DeInstance) || 
					(cha instanceof L1SummonInstance)) {
				flg = true;
			}
			if (cha instanceof L1DeInstance) {
				// 位於安全區域中
				if (cha.isSafetyZone()) {
					return false;
				}
			}
		}

		// PC 對 NPC
		if (_calcType == PC_NPC) {
			// 判斷目標為人物
			if (cha instanceof L1PcInstance) {
				// 位於安全區域中
				if (cha.isSafetyZone()) {
					return false;
				}
			}
		}
		
		// 原目標為寵物、召喚獸以外的NPC時，PC、寵物、召喚獸不在範圍內
		if ((_calcType == PC_NPC)
				// 目標是NPC
				&& (_target instanceof L1NpcInstance)
				// 不能是寵物
				&& !(_target instanceof L1PetInstance)
				// 不能是召喚獸
				&& !(_target instanceof L1SummonInstance)
				&& ((cha instanceof L1PetInstance)
						|| (cha instanceof L1SummonInstance)
						|| (cha instanceof L1PcInstance))) {
			return false;
		}

		// 原目標為守衛以外的NPC時，守衛不在範圍內
		if ((_calcType == PC_NPC) && 
				(_target instanceof L1NpcInstance) && 
				!(_target instanceof L1GuardInstance) && 
				(cha instanceof L1GuardInstance)) {
			return false;
		}

		// NPC對PC時，若目標為怪物則非目標。
		if ((_skill.getTarget().equals("attack") || (_skill.getType() == L1Skills.TYPE_ATTACK))
				&& (_calcType == NPC_PC)
				&& !(cha instanceof L1PetInstance)
				&& !(cha instanceof L1SummonInstance)
				&& !(cha instanceof L1PcInstance)) {
			return false;
		}

		// NPC對NPC時，使用者與目標皆為MOB則非目標。
		if ((_skill.getTarget().equals("attack") || (_skill.getType() == L1Skills.TYPE_ATTACK))
				&& (_calcType == NPC_NPC)
				&& (_user instanceof L1MonsterInstance)
				&& (cha instanceof L1MonsterInstance)) {
			return false;
		}

		// 無方向範圍攻擊魔法無法攻擊的NPC不在範圍內
		if (_skill.getTarget().equals("none")
				&& (_skill.getType() == L1Skills.TYPE_ATTACK)
				&& ((cha instanceof L1CrownInstance)
						|| (cha instanceof L1DwarfInstance)
						|| (cha instanceof L1EffectInstance)
						|| (cha instanceof L1FieldObjectInstance)
						|| (cha instanceof L1FurnitureInstance)
						|| (cha instanceof L1HousekeeperInstance)
						|| (cha instanceof L1MerchantInstance) 
						|| (cha instanceof L1TeleporterInstance))) {
			return false;
		}

		// 攻擊系技能且目標為自己時不在範圍內
		if ((_skill.getType() == L1Skills.TYPE_ATTACK)
				&& (cha.getId() == _user.getId())) {
			return false;
		}

		// 目標為自己且為全體治癒時無效
		if ((cha.getId() == _user.getId()) 
				&& (_skillId == HEAL_ALL)) {
			return false;
		}

		if ((((_skill.getTargetTo() & L1Skills.TARGET_TO_PC) == L1Skills.TARGET_TO_PC)
				|| ((_skill.getTargetTo() & L1Skills.TARGET_TO_CLAN) == L1Skills.TARGET_TO_CLAN) 
				|| ((_skill.getTargetTo() & L1Skills.TARGET_TO_PARTY) == L1Skills.TARGET_TO_PARTY))
				&& (cha.getId() == _user.getId()) && (_skillId != HEAL_ALL)) {
			return true; // 目標為隊伍或血盟成員時對自己有效（全體治癒除外）
		}

		// 攻擊者是PC
		if ((_user instanceof L1PcInstance) && (_skill.getTarget().equals("attack") 
						|| (_skill.getType() == L1Skills.TYPE_ATTACK)) && (_isPK == false)) {
			
			// 目標是寵物
			if (cha instanceof L1SummonInstance) {
				final L1SummonInstance summon = (L1SummonInstance) cha;
				// 自己的寵物
				if (_player.getId() == summon.getMaster().getId()) {
					return false;
				}
				
			} else if (cha instanceof L1PetInstance) {
				final L1PetInstance pet = (L1PetInstance) cha;
				// 自己的寵物
				if (_player.getId() == pet.getMaster().getId()) {
					return false;
				}
			}
		}

		if ((_skill.getTarget().equals("attack") 
				|| (_skill.getType() == L1Skills.TYPE_ATTACK))
				// 目標不是怪物
				&& !(cha instanceof L1MonsterInstance)
				// 不是PK狀態
				&& (_isPK == false)
				// 目標是人物
				&& (_target instanceof L1PcInstance)) {

			L1PcInstance enemy = null;

			try {
				enemy = (L1PcInstance) cha;
				
			} catch (final Exception e) {
				return false;
			}

			// 反制無所遁形術
			if ((_skillId == COUNTER_DETECTION)
					&& (enemy.getZoneType() != 1)
					&& (cha.hasSkillEffect(INVISIBILITY) 
							|| cha.hasSkillEffect(BLIND_HIDING))) {
				return true; // 隱身或暗影之眼狀態中
			}
			if ((_player.getClanid() != 0) && (enemy.getClanid() != 0)) { // 血盟所屬中
				// 取回全部戰爭清單
				for (final L1War war : WorldWar.get().getWarList()) {
					if (war.checkClanInWar(_player.getClanname())) { // 己方血盟參加戰爭中
						if (war.checkClanInSameWar( // 參加同一戰爭
								_player.getClanname(), enemy.getClanname())) {
							if (L1CastleLocation.checkInAllWarArea(
									enemy.getX(), 
									enemy.getY(), 
									enemy.getMapId())
									) {
								return true;
							}
						}
					}
				}
			}
			return false; // 攻擊技能且非PK模式時
		}

		final boolean attackSkill =
				(_skill.getType() & L1Skills.TYPE_ATTACK) == L1Skills.TYPE_ATTACK;
		// 所有會造成攻擊效果的技能都必須服從權威 LOS；through 與 Proc 不得成為穿牆旁路。
		// 非攻擊技能仍保留原本的 through、附魔與復活例外語意。
		if ((!_procCast || attackSkill)
				&& (_user.glanceCheck(cha.getX(), cha.getY()) == false)
				&& (attackSkill || (_skill.isThrough() == false))) {
			// 附魔、復活技能不判斷障礙物
			if (attackSkill
					|| !((_skill.getType() == L1Skills.TYPE_CHANGE)
							|| (_skill.getType() == L1Skills.TYPE_RESTORE))) {
				_isGlanceCheckFail = true;
				return false; // 直線上存在障礙物
			}
		}

		if (cha.hasSkillEffect(ICE_LANCE)
				&& ((_skillId == ICE_LANCE) || (_skillId == FREEZING_BLIZZARD) || (_skillId == FREEZING_BREATH))) {
			return false; // 冰矛術中無法施放冰矛術、冰暴術、冰霜吐息
		}

		if (cha.hasSkillEffect(FREEZING_BLIZZARD)
				&& ((_skillId == ICE_LANCE) || (_skillId == FREEZING_BLIZZARD) || (_skillId == FREEZING_BREATH))) {
			return false; // 冰暴術中無法施放冰矛術、冰暴術、冰霜吐息
		}

		if (cha.hasSkillEffect(FREEZING_BREATH)
				&& ((_skillId == ICE_LANCE) || (_skillId == FREEZING_BLIZZARD) || (_skillId == FREEZING_BREATH))) {
			return false; // 冰霜吐息中無法施放冰矛術、冰暴術、冰霜吐息
		}

		if (cha.hasSkillEffect(EARTH_BIND) && (_skillId == EARTH_BIND)) {
			return false; // 大地束縛中無法再次施放大地束縛
		}

		if (!(cha instanceof L1MonsterInstance)
				&& ((_skillId == TAMING_MONSTER) || (_skillId == CREATE_ZOMBIE))) {
			return false; // 目標非怪物（馴服怪物用）
		}
		if (cha.isDead()
				&& ((_skillId != CREATE_ZOMBIE) && (_skillId != RESURRECTION)
						&& (_skillId != GREATER_RESURRECTION) && (_skillId != CALL_OF_NATURE))) {
			return false; // 目標已死亡
		}

		if ((cha.isDead() == false)
				&& ((_skillId == CREATE_ZOMBIE) || (_skillId == RESURRECTION)
						|| (_skillId == GREATER_RESURRECTION) || (_skillId == CALL_OF_NATURE))) {
			return false; // 目標未死亡
		}

		if (((cha instanceof L1TowerInstance) || (cha instanceof L1DoorInstance))
				&& ((_skillId == CREATE_ZOMBIE) || (_skillId == RESURRECTION)
						|| (_skillId == GREATER_RESURRECTION) || (_skillId == CALL_OF_NATURE))) {
			return false; // 目標為守護塔、門
		}

		if (cha instanceof L1PcInstance) {
			final L1PcInstance pc = (L1PcInstance) cha;
			// 
			if (pc.hasSkillEffect(ABSOLUTE_BARRIER)) {
				switch (_skillId) {
				case CURSE_BLIND:
				case WEAPON_BREAK:
				case DARKNESS:
				case WEAKNESS:
				case DISEASE:
				case FOG_OF_SLEEPING:
				case MASS_SLOW: 
				case SLOW:
				case CANCELLATION: 
				case SILENCE:
				case DECAY_POTION:
				case MASS_TELEPORT: 
				case DETECTION:
				case COUNTER_DETECTION:
				case ERASE_MAGIC: 
				case ENTANGLE:
				case PHYSICAL_ENCHANT_DEX:
				case PHYSICAL_ENCHANT_STR:
				case BLESS_WEAPON: 
				case EARTH_SKIN:
				case IMMUNE_TO_HARM:
				case REMOVE_CURSE:
					return true;
					
				default:
					return false;
				}
			}
		}

		// 目標在隱身狀態(地下)
		if (cha instanceof L1NpcInstance) {
			final int hiddenStatus = ((L1NpcInstance) cha).getHiddenStatus();
			switch (hiddenStatus) {
			case L1NpcInstance.HIDDEN_STATUS_SINK:
				switch (_skillId) {
				case DETECTION:
				case COUNTER_DETECTION:
					return true;
				}
				return false;
				
			case L1NpcInstance.HIDDEN_STATUS_FLY:
				return false;
			}
		}

		if (((_skill.getTargetTo() & L1Skills.TARGET_TO_PC) == L1Skills.TARGET_TO_PC // 目標為PC
		)
		&& (cha instanceof L1PcInstance)) {
			flg = true;
			
		} else if (((_skill.getTargetTo() & L1Skills.TARGET_TO_NPC) == L1Skills.TARGET_TO_NPC // 目標為NPC
		)
		
		&& ((cha instanceof L1MonsterInstance)
				|| (cha instanceof L1NpcInstance)
				|| (cha instanceof L1SummonInstance) 
				|| (cha instanceof L1PetInstance))) {
			flg = true;
			
		} else if (((_skill.getTargetTo() & L1Skills.TARGET_TO_PET) == L1Skills.TARGET_TO_PET)
				&& (_user instanceof L1PcInstance)) { // 目標為召喚獸、寵物
			if (cha instanceof L1SummonInstance) {
				final L1SummonInstance summon = (L1SummonInstance) cha;
				if (summon.getMaster() != null) {
					if (_player.getId() == summon.getMaster().getId()) {
						flg = true;
					}
				}
			}
			
			if (cha instanceof L1PetInstance) {
				final L1PetInstance pet = (L1PetInstance) cha;
				if (pet.getMaster() != null) {
					if (_player.getId() == pet.getMaster().getId()) {
						flg = true;
					}
				}
			}
		}
		
		if ((_calcType == PC_PC) && (cha instanceof L1PcInstance)) {
			
			final L1PcInstance xpc = (L1PcInstance) cha;
			if (((_skill.getTargetTo() & L1Skills.TARGET_TO_CLAN) == L1Skills.TARGET_TO_CLAN)
					&& (((_player.getClanid() != 0 // 目標為血盟成員
					)
					&& (_player.getClanid() == xpc.getClanid())) || _player.isGm())) {
				return true;
			}
			
			if (((_skill.getTargetTo() & L1Skills.TARGET_TO_PARTY) == L1Skills.TARGET_TO_PARTY)
					&& (_player.getParty().isMember(xpc) || _player.isGm())) {
				return true;
			}
		}

		return flg;
	}

	/**
	 * 是否為同組
	 * @param npc 
	 * @param cha 
	 * @return
	 */
	private boolean isParty(final L1NpcInstance npc, final L1Character cha) {
		if (npc.getMaster() == null) {
			return false;
		}
		// 在目標清單中
		if (npc.isHate(cha)) {
			return false;
		}
		
		final int masterId = npc.getMaster().getId();
		
		// 目標是人物
		if (cha instanceof L1PcInstance) {
			if (cha.getId() == masterId) {
				return true;
			}
			return false;
		}
		
		// 目標是寵物
		if (cha instanceof L1PetInstance) {
			final L1PetInstance tgPet = (L1PetInstance) cha;
			if (tgPet.getMaster() != null && 
					tgPet.getMaster().getId() == masterId) {
				return true;
			}
			return false;
		}
		
		// 目標是召喚獸
		if (cha instanceof L1SummonInstance) {
			final L1SummonInstance tgSu = (L1SummonInstance) cha;
			if (tgSu.getMaster() != null && 
					tgSu.getMaster().getId() == masterId) {
				return true;
			}
			return false;
		}
		return false;
	}

	private boolean containsTarget(final L1Character cha) {
		if (cha == null) {
			return false;
		}
		for (final TargetStatus ts : this._targetList) {
			if (ts.getTarget() != null && ts.getTarget().getId() == cha.getId()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 技能發動 目標清單判定
	 */
	private void makeTargetList() {
		try {
			if (this._type == TYPE_LOGIN) { // 登入時（含死亡、幽靈之家取消）僅限使用者
				this._targetList.add(new TargetStatus(this._user));
				return;
			}
			
			// 聖結界(68)需走一般目標驗證（距離、視線、地形與障礙物），不適用 TARGET_TO_ME 捷徑
			if ((this._skill.getTargetTo() == L1Skills.TARGET_TO_ME) && 
					((this._skill.getType() & L1Skills.TYPE_ATTACK) != L1Skills.TYPE_ATTACK)
					&& (this._skillId != IMMUNE_TO_HARM)) {
				this._targetList.add(new TargetStatus(this._user)); // 目標僅限使用者
				return;
			}

			// 具有攻擊範圍設置（Proc 以本次命中目標為準，不套用技能表射程）
			if (!_procCast) {
				if (this._skill.getRanged() != -1) {
					if (this._user.getLocation().getTileLineDistance(
							this._target.getLocation()) > this._skill.getRanged()) {
						return; // 射程範囲外
					}

				} else {
					// 距離不可見
					if (!this._user.getLocation().isInScreen(this._target.getLocation())) {
						return; // 射程範囲外
					}
				}
			}

			if ((this.isTarget(this._target) == false)
					&& !(this._skill.getTarget().equals("none"))) {
				if (_procCast && this._target != null
						&& (this._skill.getType() & L1Skills.TYPE_ATTACK) != L1Skills.TYPE_ATTACK) {
					if (!containsTarget(this._target)) {
						this._targetList.add(new TargetStatus(this._target));
					}
					return;
				}
				// 目標不符，技能不發動
				return;
			}

			// 直線上目標列舉
			switch (this._calcType) {
			case LIGHTNING:
			case FREEZING_BREATH:
				final ArrayList<L1Object> al1object = 
					World.get().getVisibleLineObjects(this._user, this._target);
				for (final L1Object tgobj : al1object) {
					if (tgobj == null) {
						continue;
					}
					
					if (!(tgobj instanceof L1Character)) { // 目標非角色時不處理
						continue;
					}
					
					final L1Character cha = (L1Character) tgobj;
					if (this.isTarget(cha) == false) {
						continue;
					}
					// 技能發動 目標清單判定:直線上目標列舉
					this._targetList.add(new TargetStatus(cha));
				}
				al1object.clear();
				return;
			}

			// 單一目標攻擊
			if (this._skill.getArea() == 0) {
				if (!this._user.glanceCheck(this._target.getX(), this._target.getY())) { // 直線上是否存在障礙物
					if ((this._skill.getType() & L1Skills.TYPE_ATTACK) == L1Skills.TYPE_ATTACK) {
						// 不產生傷害與傷害動作，但技能會發動
						this._targetList.add(new TargetStatus(this._target, false));
						return;
					}
				}
				if (!containsTarget(this._target)) {
					this._targetList.add(new TargetStatus(this._target));
				}
				
			// 範圍攻擊
			} else {
				if (!this._skill.getTarget().equals("none")) {
					this._targetList.add(new TargetStatus(this._target));
				}

				if ((this._skillId != HEAL_ALL) && 
						!(this._skill.getTarget().equals("attack") ||
								(this._skill.getType() == L1Skills.TYPE_ATTACK))) {
					// 攻擊系以外技能及全體治癒以外，含目標自身
					this._targetList.add(new TargetStatus(this._user));
				}

				List<L1Object> objects;
				// 全畫面物件
				if (this._skill.getArea() == -1) {
					objects = World.get().getVisibleObjects(this._user);
					
				// 指定範圍物件
				} else {
					objects = World.get().getVisibleObjects(this._target, this._skill.getArea());
				}
				//System.out.println("攻擊範圍物件數量:"+objects.size());
				for (final L1Object tgobj : objects) {
					if (tgobj == null) {
						continue;
					}
					
					if (!(tgobj instanceof L1Character)) {
						continue;
					}

					final L1Character cha = (L1Character) tgobj;

					if (!this.isTarget(cha)) {
						continue;
					}

					// 技能發動 目標清單判定:加入目標清單 - 迴圈
					this._targetList.add(new TargetStatus(cha));
				}
				return;
			}

		} catch (final Exception e) {
			//_log.error("SkillId:" + this._skillId + " UserName:" + this._player.getName());
		}
	}

	/**
	 * 訊息發送
	 * @param pc
	 */
	private void sendHappenMessage(final L1PcInstance pc) {
		final int msgID = this._skill.getSysmsgIdHappen();
		if (msgID > 0) {
			pc.sendPackets(new S_ServerMessage(msgID));
		}
	}

	// 失敗訊息顯示處理
	private void sendFailMessageHandle() {
		// 非攻擊技能且指定目標的技能失敗時，向客戶端發送失敗訊息
		// ※攻擊技能即使有障礙物也應與成功時相同動作
		if ((_skill.getType() != L1Skills.TYPE_ATTACK)
				&& !_skill.getTarget().equals("none")
				&& (_targetList.size() == 0)) {
			sendFailMessage();
		}
	}

	// 訊息顯示（失敗時）
	private void sendFailMessage() {
		final int msgID = _skill.getSysmsgIdFail();
		if ((msgID > 0) && (_user instanceof L1PcInstance)) {
			_player.sendPackets(new S_ServerMessage(msgID));
		}
	}

	// 精靈魔法屬性與使用者屬性是否包含（位元遮罩：允許雙屬性，只要玩家擁有該技能屬性之一即可）
	private boolean isAttrAgrees() {
		final int magicattr = _skill.getAttr();
		if (_user instanceof L1NpcInstance) { // NPC使用時一律OK
			return true;
		}

		if ((_skill.getSkillLevel() >= 17) && (_skill.getSkillLevel() <= 22)
				&& (magicattr != 0) // 精靈魔法且非無屬性魔法
				&& ((_player.getElfAttr() & magicattr) == 0) // 玩家不包含此技能屬性則不可施放
				&& !_player.isGm()) { // GM為例外
			return false;
		}
		return true;
	}

	/**
	 * 判断技能的使用是否需要消耗HP/MP
	 * @return
	 */
	private boolean isHPMPConsume() {
		this._mpConsume = _skill.getMpConsume();
		this._hpConsume = _skill.getHpConsume();
		int currentMp = 0;
		int currentHp = 0;

		if (this._user instanceof L1NpcInstance) {
			currentMp = _npc.getCurrentMp();
			currentHp = _npc.getCurrentHp();
			
		} else {
			currentMp = _player.getCurrentMp();
			currentHp = _player.getCurrentHp();

			// MP的INT減免
			if ((this._player.getInt() > 12) && (this._skillId > HOLY_WEAPON)
					&& (this._skillId <= FREEZING_BLIZZARD)) { // LV2以上
				this._mpConsume--;
			}
			if ((this._player.getInt() > 13) && (this._skillId > STALAC)
					&& (this._skillId <= FREEZING_BLIZZARD)) { // LV3以上
				this._mpConsume--;
			}
			if ((this._player.getInt() > 14) && (this._skillId > WEAK_ELEMENTAL)
					&& (this._skillId <= FREEZING_BLIZZARD)) { // LV4以上
				this._mpConsume--;
			}
			if ((this._player.getInt() > 15) && (this._skillId > MEDITATION)
					&& (this._skillId <= FREEZING_BLIZZARD)) { // LV5以上
				this._mpConsume--;
			}
			if ((this._player.getInt() > 16) && (this._skillId > DARKNESS)
					&& (this._skillId <= FREEZING_BLIZZARD)) { // LV6以上
				this._mpConsume--;
			}
			if ((this._player.getInt() > 17) && (this._skillId > BLESS_WEAPON)
					&& (this._skillId <= FREEZING_BLIZZARD)) { // LV7以上
				this._mpConsume--;
			}
			if ((this._player.getInt() > 18) && (this._skillId > DISEASE)
					&& (this._skillId <= FREEZING_BLIZZARD)) { // LV8以上
				this._mpConsume--;
			}

			if ((this._player.getInt() > 12) && (this._skillId >= SHOCK_STUN)
					&& (this._skillId <= COUNTER_BARRIER)) {
				this._mpConsume -= (this._player.getInt() - 12);
			}

			// 裝備減低MP消耗
			switch (_skillId) {
			case PHYSICAL_ENCHANT_DEX:// 通暢氣脈術
				if (this._player.getInventory().checkEquipped(20013)) {// 敏捷魔法頭盔
					this._mpConsume = this._mpConsume >> 1;
				}
				break;
			case HASTE:// 加速術
				if (this._player.getInventory().checkEquipped(20013)) { // 敏捷魔法頭盔
					this._mpConsume = this._mpConsume >> 1;
				} else if (this._player.getInventory().checkEquipped(20008)) { // 小型風之頭盔
					this._mpConsume = this._mpConsume >> 1;
				}
				break;
			case HEAL:// 初級治癒術
				if (this._player.getInventory().checkEquipped(20014)) { // 治癒魔法頭盔
					this._mpConsume = this._mpConsume >> 1;
				}
				break;
			case EXTRA_HEAL:// 中級治癒術
				if (this._player.getInventory().checkEquipped(20014)) { // 治癒魔法頭盔
					this._mpConsume = this._mpConsume >> 1;
				}
				break;
			case ENCHANT_WEAPON:// 擬似魔法武器
				if (this._player.getInventory().checkEquipped(20015)) { // 力量魔法頭盔
					this._mpConsume = this._mpConsume >> 1;
				}
				break;
			case DETECTION:// 無所遁形術
				if (this._player.getInventory().checkEquipped(20015)) { // 力量魔法頭盔
					this._mpConsume = this._mpConsume >> 1;
				}
				break;
			case PHYSICAL_ENCHANT_STR:// 體魄強健術
				if (this._player.getInventory().checkEquipped(20015)) { // 力量魔法頭盔
					this._mpConsume = this._mpConsume >> 1;
				}
				break;
			case GREATER_HASTE:// 強力加速術
				if (this._player.getInventory().checkEquipped(20023)) { // 風之頭盔
					this._mpConsume = this._mpConsume >> 1;
				}
				break;
			}

			if (0 < this._skill.getMpConsume()) { // 需要MP大於0
				this._mpConsume = Math.max(this._mpConsume, 1); // 最低消耗1點
			}

			// MP的原始INT減免
			if (this._player.getOriginalMagicConsumeReduction() > 0) {
				this._mpConsume -= this._player.getOriginalMagicConsumeReduction();
			}
		}

		if (currentHp < this._hpConsume + 1) {
			if (this._user instanceof L1PcInstance) {
				// 279 \f1因體力不足而無法使用魔法。
				this._player.sendPackets(new S_ServerMessage(279));
			}
			return false;
			
		} else if (currentMp < this._mpConsume) {
			if (this._user instanceof L1PcInstance) {
				// 278 \f1因魔力不足而無法使用魔法。
				this._player.sendPackets(new S_ServerMessage(278));
				if (this._player.isGm()) {
					this._player.setCurrentMp(this._player.getMaxMp());
				}
			}
			return false;
		}

		return true;
	}

	// 必要材料是否足夠？
	// 判断技能的使用是否需要其他物品的辅助
	private boolean isItemConsume() {
		// [修正] 假人不需要檢查材料，直接返回 true，避免 NullPointerException
		if (this._user instanceof L1DeInstance) {
			return true;
		}

		final int itemConsume = this._skill.getItemConsumeId();
		final int itemConsumeCount = this._skill.getItemConsumeCount();

		if (itemConsume == 0) {
			return true; // 不需材料的魔法
		}

		if (!this._player.getInventory().checkItem(itemConsume, itemConsumeCount)) {
			return false; // 必要材料不足
		}

		return true;
	}

	/**
	 * 使用技能后，相应的HP和MP、Lawful、材料的减少
	 */
	private void useConsume() {
		
		//如果是假人
		if (this._user instanceof L1DeInstance) {
	        return;
	    }
		
		if (this._user instanceof L1NpcInstance) {
			// NPC時僅扣除HP、MP
			this._npc.adjustCurrentHp(-((long) this._hpConsume));
			this._npc.adjustCurrentMp(-((long) this._mpConsume));
			return;
		}

		// 扣除HP、MP
		if (this.isHPMPConsume()) {
			if (this._skillId == FINAL_BURN) { // 終極烈焰
				this._player.setCurrentHp(1);
				this._player.setCurrentMp(0);
				
			} else {
				this._player.adjustCurrentHp(-((long) this._hpConsume));
				this._player.adjustCurrentMp(-((long) this._mpConsume));
			}
		}

		// 扣除性向值
		this._player.addLawful(this._skill.getLawful());

		// Required material is DB-first committed before action reservation and
		// runSkill().  Never perform a second, post-effect debit here.
		if (this._skill.getItemConsumeId() != 0 && !_materialDebitResolvedForCast) {
			ERROR_COUNT.incrementAndGet();
			_log.error("Skill material precommit invariant missing skillId={} player={}",
					_skillId, _player.getName());
		}
	}

	// 加入魔法清單
	// 使用相应技能要对玩家人物或者使用的相应道具追加必要动作
	private void addMagicList(final L1Character cha, final boolean repetition) {
		if (_skillTime == 0) {
			_getBuffDuration = _skill.getBuffDuration() * 1000; // 効果時間
			if (_skill.getBuffDuration() == 0) {
				if (_skillId == INVISIBILITY) { // 隱身術
					cha.setSkillEffect(INVISIBILITY, 0);
				}
				return;
			}
		} else {
			_getBuffDuration = _skillTime * 1000; // time參數非0時設為效果時間
		}
		switch (_skillId) {
		case IMMUNE_TO_HARM:
			break;

		case SHOCK_STUN:// 衝擊之暈
			_getBuffDuration = _shockStunDuration;
			break;
			
		case CURSE_POISON:// 毒咒
			// L1Poison處理
			return;
			
		case CURSE_PARALYZE:// 木乃伊的詛咒
		case CURSE_PARALYZE2:// 魔法效果:麻痺
			// L1CurseParalysis處理
			return;
			
		case SHAPE_CHANGE:// 變形術
			// L1PolyMorph處理
			return;
			
		case BLESSED_ARMOR:// 鎧甲護持
		case HOLY_WEAPON: // 神聖武器
		case ENCHANT_WEAPON:// 擬似魔法武器
		case BLESS_WEAPON:// 祝福魔法武器
		case SHADOW_FANG:// 暗影之牙
			// 武器防具效果移轉 L1ItemInstance處理
			return;
			
		case ICE_LANCE:
		case FREEZING_BLIZZARD:
		case FREEZING_BREATH:
			if (!_isFreeze) {
				return;
			}
			break;
		}

		/*if (_skillId == SHOCK_STUN) {
			_getBuffDuration = _shockStunDuration;
		}

		if (_skillId == CURSE_POISON) { // 詛咒毒效果交由L1Poison處理
			return;
		}
		if ((_skillId == CURSE_PARALYZE) || (_skillId == CURSE_PARALYZE2)) { // 詛咒麻痹效果交由L1CurseParalysis處理
			return;
		}
		if (_skillId == SHAPE_CHANGE) { // 變形術效果交由L1PolyMorph處理
			return;
		}
		if ((_skillId == BLESSED_ARMOR)
				|| (_skillId == HOLY_WEAPON) // 對武器防具生效的處理交由L1ItemInstance
				|| (_skillId == ENCHANT_WEAPON) 
				|| (_skillId == BLESS_WEAPON)
				|| (_skillId == SHADOW_FANG)) {
			return;
		}*/
		/*if (((_skillId == ICE_LANCE) || (_skillId == FREEZING_BLIZZARD) || (_skillId == FREEZING_BREATH))
				&& !_isFreeze) { // 凍結失敗
			return;
		}*/
		/*if ((this._skillId == AWAKEN_ANTHARAS) 
				|| (this._skillId == AWAKEN_FAFURION)
				|| (this._skillId == AWAKEN_VALAKAS)) { // 覺醒效果交由L1Awake處理
			return;
		}*/
		final SkillMode mode = getSkillMode();
		if (mode == null) {
			cha.setSkillEffect(getEffectSkillId(), _getBuffDuration);
		} else if (_skillId == AWAKEN_ANTHARAS || _skillId == AWAKEN_FAFURION || _skillId == AWAKEN_VALAKAS) {
			if (cha instanceof L1PcInstance) {
				final L1PcInstance apc = (L1PcInstance) cha;
				if (apc.getAwakeSkillId() == _skillId) {
					cha.setSkillEffect(_skillId, _getBuffDuration);
				}
			}
		}
		// XXX
		if (cha instanceof L1PcInstance) {
			final L1PcInstance pc = (L1PcInstance) cha;
			// 不論首次或重疊施放，皆重算 skills_buff_ability 並套用
			pc.updateBuffStatus();
			// 首次施放時 sendIcon 僅在 repetition；自訂秒數、聖界、加速術需補送圖示
			if (repetition || (_skillTime > 0 && _skillId == IMMUNE_TO_HARM)
					|| _skillId == HASTE || _skillId == GREATER_HASTE) {
				sendIcon(pc);
			}
		}
	}

	/**
	 * 發送技能圖示
	 * @param pc
	 */
	private void sendIcon(final L1PcInstance pc) {
		if (this._skillTime == 0) {
			this._getBuffIconDuration = this._skill.getBuffDuration(); // 効果時間
			
		} else {
			this._getBuffIconDuration = this._skillTime; // time參數非0時設為圖示效果時間
		}

		SkillBuffIconHelper.sendBuffIcon(pc, this._skillId, this._getBuffIconDuration);
	}

	// 圖示發送
	// 图解发送，技能使用完毕后发送结束提示图标
	private void sendCastMotion() {
		if (_castMotionSent || _skill == null || _user == null
				|| ((_skill.getCastGfx() == 0) && (_skillId != INVISIBILITY))) {
			return;
		}
		if (_user instanceof L1PcInstance) {
			sendPcCastMotion();
		} else if (_user instanceof L1NpcInstance) {
			sendNpcCastMotion();
		}
	}

	private void sendPcCastMotion() {
		if (_player == null) {
			return;
		}
		if ((_skillId == FIRE_WALL) || (_skillId == LIFE_STREAM)) {
			if (_skillId == FIRE_WALL) {
				_player.setHeading(_player.targetDirection(_targetX, _targetY));
				_player.sendPacketsAll(new S_ChangeHeading(_player));
			}
			_player.sendPacketsAll(new S_DoActionGFX(_player.getId(), _skill.getActionId()));
			_castMotionSent = true;
			return;
		}
		if (usesCompositeSkillPacket()) {
			return;
		}
		if (hasNoTargetForTargetedSkill()) {
			int actionId = _skill.getActionId();
			switch (_player.getTempCharGfx()) {
			case 5727: case 5730:
				actionId = ActionCodes.ACTION_SkillBuff;
				break;
			case 5733: case 5736:
				actionId = ActionCodes.ACTION_Attack;
				break;
			}
			_player.sendPacketsX10(new S_DoActionGFX(_player.getId(), actionId));
			_castMotionSent = true;
			return;
		}
		if ((_skillId != 5) && (_skillId != 69) && (_skillId != 131)) {
			_player.sendPacketsX10(new S_DoActionGFX(_player.getId(), _skill.getActionId()));
			_castMotionSent = true;
		}
	}

	private void sendNpcCastMotion() {
		if (_npc == null || _user instanceof L1MerchantInstance || usesCompositeSkillPacket()) {
			return;
		}
		if (hasNoTargetForTargetedSkill() || ((_skillId != 5) && (_skillId != 69) && (_skillId != 131))) {
			_user.broadcastPacketX10(new S_DoActionGFX(_user.getId(), _skill.getActionId()));
			_castMotionSent = true;
		}
	}

	private boolean hasNoTargetForTargetedSkill() {
		return (_targetList == null || _targetList.size() == 0) && !(_skill.getTarget().equals("none"));
	}

	private boolean usesCompositeSkillPacket() {
		return (_skillId == SHOCK_STUN)
				|| (_skill.getTarget().equals("attack") && (_skillId != 18))
				|| (_skill.getTarget().equals("none") && (_skill.getType() == L1Skills.TYPE_ATTACK));
	}

	private int getEffectSkillId() {
		if ((_skill != null) && (_skill.getEffectSkillId() > 0)) {
			return _skill.getEffectSkillId();
		}
		return _skillId;
	}

	private SkillMode getSkillMode() {
		SkillMode mode = L1SkillMode.get().getSkill(this._skillId);
		if ((mode == null) && (_skill != null)
				&& (_skill.getEffectSkillId() > 0)
				&& (_skill.getEffectSkillId() != this._skillId)) {
			mode = L1SkillMode.get().getSkill(_skill.getEffectSkillId());
		}
		return mode;
	}

	private void sendGrfx(final boolean isSkillAction) {
		if (_deferSingleTargetImpactGrfx && !_releaseDeferredImpactGrfx) {
			return;
		}
		int actionId = _skill.getActionId();
		final int castgfx = _skill.getCastGfx();
		if (castgfx == 0) {
			return; // 無需顯示的圖示
		}

		// 施展者為PC
		if (_user instanceof L1PcInstance) {
			if ((_skillId == FIRE_WALL) || (_skillId == LIFE_STREAM)) {
				final L1PcInstance pc = (L1PcInstance) _user;
				if (_skillId == FIRE_WALL) {
					pc.setHeading(pc.targetDirection(_targetX, _targetY));
					pc.sendPacketsAll(new S_ChangeHeading(pc));
				}
				if (isSkillAction && !_castMotionSent) {
					pc.sendPacketsAll(new S_DoActionGFX(pc.getId(), actionId));
				}
				return;
			}

			final int targetid = this._target.getId();

			// 衝擊之暈：以「近戰攻擊封包」呈現揮擊（ACTION_Attack）。
			if (_skillId == SHOCK_STUN) {
				if (_targetList.size() == 0) {
					return;
				}
				if (isSkillAction && !_castMotionSent) {
					// 面向說明：若與目標格不一致，伺服器會先送 S_ChangeHeading 再送揮刀，畫面上就像「先轉身再砍」。
					// 改以目標當下座標計算（避免封包 x,y 與實際位置不同步）；且僅在面向真的改變時才廣播，減少多餘轉身感。
					int faceX = _targetX;
					int faceY = _targetY;
					if (_target != null) {
						faceX = _target.getX();
						faceY = _target.getY();
					}
					final int newHeading = _player.targetDirection(faceX, faceY);
					if (_player.getHeading() != newHeading) {
						_player.setHeading(newHeading);
						_player.sendPacketsX10(new S_ChangeHeading(_player));
					}
					if (isPcSummonPet(_target)) {
						if (_player.isSafetyZone() || _target.isSafetyZone() || _player.checkNonPvP(_player, _target)) {
							// 與近戰安全區一致：不鎖定目標 OBJ，僅自身攻擊動作
							_player.sendPacketsX10(new S_AttackPacketPc(_player));
						} else if (_skill.getArea() == 0) {
							_player.sendPacketsX10(new S_AttackPacketPc(_player, _target, 0, _dmg));
						}
					} else if (_skill.getArea() == 0) {
						_player.sendPacketsX10(new S_AttackPacketPc(_player, _target, 0, _dmg));
					}
				}
				if (_target instanceof L1PcInstance) {
					final L1PcInstance pc = (L1PcInstance) _target;
					pc.sendPacketsAll(new S_SkillSound(pc.getId(), 4434));
				} else if (_target instanceof L1NpcInstance) {
					_target.broadcastPacketX10(new S_SkillSound(_target.getId(), 4434));
				}
				return;
			}

			if (_skillId == LIGHT) {
				final L1PcInstance pc = (L1PcInstance) _target;
				pc.sendPackets(new S_Sound(145));
			}

			// 如果目標數量為0，且不是無目標技能
			if ((_targetList.size() == 0) && !(_skill.getTarget().equals("none"))) {
				final int tempchargfx = _player.getTempCharGfx();
				switch (tempchargfx) {
				case 5727: case 5730: // 暗影系變身動作對應
					actionId = ActionCodes.ACTION_SkillBuff;
					break;
				case 5733: case 5736:
					actionId = ActionCodes.ACTION_Attack;
					break;
				}
				// [修正] 只有在允許動作時才發送動作
				if (isSkillAction && !_castMotionSent) {
					_player.sendPacketsX10(new S_DoActionGFX(_player.getId(), actionId));
				}
				return;
			}

			if (isSkillAction && _skill.getTarget().equals("attack") && (_skillId != 18)) {
				final int atkGfx = (_skillId == DISINTEGRATE && _disintegrateCrit) ? ConfigSkill.CRITICAL_SPR : castgfx;
				if (isPcSummonPet(_target)) {
					if (_player.isSafetyZone() || _target.isSafetyZone() || _player.checkNonPvP(_player, _target)) {
						_player.sendPacketsX10(new S_UseAttackSkill(_player, 0, atkGfx, _targetX, _targetY, actionId, _dmg));
						return;
					}
				}
				// 單體攻擊 (含動作+特效)
				if (_skill.getArea() == 0) {
					_player.sendPacketsX10(new S_UseAttackSkill(_player, targetid, atkGfx, _targetX, _targetY, actionId, _dmg));
				} 
				// 有方向範圍魔法 (含動作+特效)
				else {
					// 傷害擴充只發給施法玩家本人；旁觀者僅收原版動畫封包。
					_player.sendPackets(new S_RangeSkill(_player, _targetList, castgfx, actionId,
							S_RangeSkill.TYPE_DIR, true));
					_player.broadcastPacketX10(new S_RangeSkill(_player, _targetList, castgfx, actionId,
							S_RangeSkill.TYPE_DIR, false));
				}
				
			// 判斷 2: 是否為無方向攻擊技能 (且允許動作)
			} else if (isSkillAction && _skill.getTarget().equals("none") && (_skill.getType() == L1Skills.TYPE_ATTACK)) { 
				_player.sendPackets(new S_RangeSkill(_player, _targetList, castgfx, actionId,
						S_RangeSkill.TYPE_NODIR, true));
				_player.broadcastPacketX10(new S_RangeSkill(_player, _targetList, castgfx, actionId,
						S_RangeSkill.TYPE_NODIR, false));
				
			// 判斷 3: 輔助魔法 OR 被降級的攻擊魔法 (靜默模式走這裡)
			} else { 
				// 排除傳送類技能
				if ((_skillId != 5) && (_skillId != 69) && (_skillId != 131)) {
					
					// A. 發送動作 (只在允許時)
					if (isSkillAction && !_castMotionSent) {
						_player.sendPacketsX10(new S_DoActionGFX(_player.getId(), _skill.getActionId()));
					}

					// B. 發送特效 (S_SkillSound)
					// 這裡處理所有「純特效」的顯示
					if ((_skillId == COUNTER_MAGIC) || (_skillId == COUNTER_BARRIER) || (_skillId == COUNTER_MIRROR)) {
						_player.sendPackets(new S_SkillSound(targetid, castgfx));
						
					} else if (_skillId == TRUE_TARGET) {
						return;
						
					} else if ((_skillId == AWAKEN_ANTHARAS) || (_skillId == AWAKEN_FAFURION) || (_skillId == AWAKEN_VALAKAS)) {
						if (_skillId == _player.getAwakeSkillId()) {
							_player.sendPacketsX10(new S_SkillSound(targetid, castgfx));
						} else {
							return;
						}
						
					} else {
						// [關鍵] 無論是輔助還是被降級的攻擊魔法，都在這裡發送 S_SkillSound
						// 這保證了特效一定會出現
						_player.sendPacketsX10(new S_SkillSound(targetid, castgfx));
					}
				}

				// 狀態更新
				for (final TargetStatus ts : _targetList) {
					final L1Character cha = ts.getTarget();
					if (cha instanceof L1PcInstance) {
						final L1PcInstance pc = (L1PcInstance) cha;
						pc.sendPackets(new S_OwnCharStatus(pc));
						// 神聖/祝福武器的原生 icon 可能由施法特效重新建立，特效送完後再清一次。
						IndependentSkillIcon.clearNativeWeaponIcon(pc, this._skillId);
					}
				}
			}
			
		// TODO 施展者是NPC
		} else if (this._user instanceof L1NpcInstance) { // NPC使用技能時
			final int targetid = this._target.getId();

			if (this._user instanceof L1MerchantInstance) {
				this._user.broadcastPacketX10(new S_SkillSound(targetid, castgfx));
				return;
			}

			// 假人施法
			int npcActionId = this._skill.getActionId();

			if ((this._targetList.size() == 0) && !(this._skill.getTarget().equals("none"))) {
				// 目標數為0且為指定目標的技能時，僅顯示魔法使用效果後結束
				// [修正] 讓系統根據 Skill 設定自動發送對應動作（假人也需要動作封包）
				if (isSkillAction && !_castMotionSent) {
					this._user.broadcastPacketX10(new S_DoActionGFX(this._user.getId(), npcActionId));
				}
				return;
			}

			if (this._skill.getTarget().equals("attack") && (this._skillId != 18)) {
				if (this._skill.getArea() == 0) { // 単体攻撃魔法
					// [修正] 假人使用攻擊技能時，使用轉換後的動作 ID
					int attackActionId = actionId;
					if (this._user instanceof L1DeInstance) {
						attackActionId = npcActionId;
					}
					this._user.broadcastPacketX10(
							new S_UseAttackSkill(
									this._user, 
									targetid, 
									castgfx, 
									this._targetX, 
									this._targetY, 
									attackActionId,
									this._dmg
									));

				} else { // 有方向範圍攻撃魔法
					// [修正] 假人使用範圍攻擊技能時，使用轉換後的動作 ID
					int rangeActionId = actionId;
					if (this._user instanceof L1DeInstance) {
						rangeActionId = npcActionId;
					}
					this._user.broadcastPacketX10(
							new S_RangeSkill(
									this._user, 
									this._targetList, 
									castgfx, 
									rangeActionId, 
									S_RangeSkill.TYPE_DIR
									));
				}
				
			} else if (this._skill.getTarget().equals("none")
					&& (this._skill.getType() == L1Skills.TYPE_ATTACK)) { // 無方向範圍魔法
				//System.out.println("無方向範圍魔法");
				// [修正] 假人使用無方向範圍魔法時，使用轉換後的動作 ID
				int noDirActionId = actionId;
				if (this._user instanceof L1DeInstance) {
					noDirActionId = npcActionId;
				}
				this._user.broadcastPacketX10(
						new S_RangeSkill(
								this._user, 
								this._targetList, 
								castgfx, 
								noDirActionId, 
								S_RangeSkill.TYPE_NODIR
								));
				
			} else { // 補助魔法
				// 傳送、集體傳送、回卷以外
				if ((this._skillId != 5) && (this._skillId != 69) && (this._skillId != 131)) {
					// 使用魔法的動作效果僅限使用者
					// [修正] 讓系統根據 Skill 設定自動發送對應動作（假人也需要動作封包）
					if (isSkillAction && !_castMotionSent) {
						this._user.broadcastPacketX10(new S_DoActionGFX(this._user.getId(), npcActionId));
					}
					this._user.broadcastPacketX10(new S_SkillSound(targetid, castgfx));
				}
			}
		}
	}
	
	// 兩種模式都維持覆蓋的加速技能組。
	private static final int[][] ALWAYS_REPEATED_SKILLS = {
			{ HOLY_WALK, MOVING_ACCELERATION, WIND_WALK, STATUS_BRAVE,
				STATUS_ELFBRAVE, STATUS_RIBRAVE, BLOODLUST },
				
			{ HASTE, GREATER_HASTE, STATUS_HASTE }
			};

	// server_event SkillCoexistSet=0 時恢復的原廠同群組覆蓋。
	private static final int[][] ORIGINAL_REPEATED_SKILLS = {
			{ HOLY_WEAPON, BLESS_WEAPON, ENCHANT_WEAPON },
			{ FIRE_WEAPON, WIND_SHOT, FIRE_BLESS, STORM_EYE, BURNING_WEAPON, STORM_SHOT },
			{ SHIELD, SHADOW_ARMOR, EARTH_SKIN, EARTH_BLESS, IRON_SKIN },
			{ PHYSICAL_ENCHANT_DEX, DRESS_DEXTERITY },
			{ PHYSICAL_ENCHANT_STR, DRESS_MIGHTY },
			{ GLOWING_AURA, SHINING_AURA }
			};
	
	/**
	 * 删除不能重复/同时使用的技能，图标更改为刚使用时的图标
	 * @param cha
	 */
	private void deleteRepeatedSkills(final L1Character cha) {
		deleteRepeatedSkills(cha, ALWAYS_REPEATED_SKILLS);
		if (!SkillCoexistEventTable.get().isCoexistEnabled()) {
			deleteRepeatedSkills(cha, ORIGINAL_REPEATED_SKILLS);
		}
	}

	private void deleteRepeatedSkills(final L1Character cha, final int[][] groups) {
		for (final int[] skills : groups) {
			for (final int id : skills) {
				if (id == _skillId) {
					stopSkillList(cha, skills);
					break;
				}
			}
		}
	}

	/**
	 * 删除全部重复的正在使用的技能
	 * @param cha
	 * @param repeat_skill
	 */
	private void stopSkillList(final L1Character cha, final int[] repeat_skill) {
		for (final int skillId : repeat_skill) {
			if (skillId != _skillId) {
				cha.removeSkillEffect(skillId);
			}
		}
	}

	// 技能使用延迟的设定
	/**
	 * 動作窗與技能 reuseDelay 使用獨立狀態，且從同一次施法同時開始。
	 * 下一次施法必須兩者都到期，因此有效等待時間是 max(actionDelay, reuseDelay)，
	 * 不是 actionDelay + reuseDelay。
	 */
	private boolean commitMaterialAndAcquireCastLocks() {
		if (_user == null || _skill == null) {
			return false;
		}
		if (_user instanceof L1PcInstance pc) {
			// The read-only gate check, DB-first material commit and final action/CD
			// reservation are one player critical section.  The DB call executes on
			// PacketBlocking for client skills, never on LogicPool.
			return PlayerActionLock.call(pc, this::commitMaterialAndAcquireCastLocksHeld);
		}
		_materialDebitResolvedForCast = true;
		final SkillCastAdmission.Reservation reservation = reserveCastLocksHeld();
		if (reservation == null) {
			return false;
		}
		reservation.commit();
		publishAcceptedCastState();
		return true;
	}

	private boolean commitMaterialAndAcquireCastLocksHeld() {
		// Do not debit a speed-burst/cooldown-rejected packet.  Holding the player
		// action lock makes this preflight stable until the final reservation; time
		// advancing during JDBC can only make an existing runtime window expire.
		final SkillCastAdmission.Result result = SkillCastAdmission.admit(
				this::canAcquireCastLocksHeld,
				this::reserveCastLocksHeld,
				this::commitRequiredMaterialHeld);
		if (result != SkillCastAdmission.Result.ACCEPTED) {
			if (_player != null && isTeleportSkill(_skillId)) {
				TeleportTrace.warn(_player, explainCastLockReject(result),
						"skillId", _skillId, "admit", result);
			}
			return false;
		}
		if (_player != null) {
			// A newly accepted skill supersedes the single buffered physical
			// packet. The client may submit a fresh attack after this exact skill
			// timeline ends; never let an older target action fire through it.
			_player.getClientAttackDeferral().cancel();
		}
		publishAcceptedCastState();
		return true;
	}

	private boolean commitRequiredMaterialHeld() {
		if (_materialDebitResolvedForCast) {
			return _skill.getItemConsumeId() == 0 || (_player != null && _player.isGm())
					|| _materialConsumedForCast;
		}
		if (_player == null || _user instanceof L1DeInstance) {
			_materialDebitResolvedForCast = true;
			return true;
		}
		final int itemId = _skill.getItemConsumeId();
		final int itemCount = _skill.getItemConsumeCount();
		if (itemId == 0 || _player.isGm()) {
			_materialDebitResolvedForCast = true;
			return true;
		}
		if (itemCount <= 0 || !_player.getInventory().consumeItem(itemId, itemCount)) {
			MATERIAL_COMMIT_FAILURE_COUNT.incrementAndGet();
			_player.sendPackets(new S_ServerMessage(299));
			return false;
		}
		_materialConsumedForCast = true;
		_materialDebitResolvedForCast = true;
		MATERIAL_COMMIT_COUNT.incrementAndGet();
		return true;
	}

	private boolean canAcquireCastLocksHeld() {
		if (_user.isSkillDelay()) {
			return false;
		}
		if (_user instanceof L1PcInstance pc && resolveCastActionType() != null) {
			/*
			 * Native clients submit a spell as the next visible action even while
			 * moving or attacking. The timeline admits that one-way transition
			 * without clearing any unrelated ingress/reuse protection.
			 */
			return pc.getClientActionTimeline().canReserveSkillAction();
		}
		return true;
	}

	/** PC callers own the action lock; non-PC delay admission is atomic itself. */
	private SkillCastAdmission.Reservation reserveCastLocksHeld() {
		if (_user.isSkillDelay()) {
			return null;
		}
		Reservation actionReservation = null;
		if (_user instanceof L1PcInstance pc) {
			final AcceleratorChecker.ACT_TYPE actionType = resolveCastActionType();
			if (actionType != null) {
				final int actionDelay = pc.speed_Attack().getActionInterval(actionType);
				if (actionDelay <= 0) {
					return null;
				}
				actionReservation = pc.getClientActionTimeline().tryReserveSkillAction(actionDelay);
				if (actionReservation == null) {
					return null;
				}
			}
		}
		final L1SkillDelay.Lease reuseLease = L1SkillDelay.tryReserveSkillUse(
				_user, _skill.getReuseDelay());
		if (reuseLease == null) {
			if (actionReservation != null) {
				actionReservation.cancel();
			}
			return null;
		}
		final Reservation reservedAction = actionReservation;
		return new SkillCastAdmission.Reservation() {
			@Override
			public void commit() {
				if (reservedAction != null && !reservedAction.commitAtRuntime()) {
					// This can only happen if a foreign lifecycle mutation replaced the
					// exact action token while PlayerActionLock was held. Keep the reuse
					// gate fail-closed and make the invariant violation observable.
					_log.error("Skill action runtime rebase lost exact token skillId={} player={}",
							_skillId, _player != null ? _player.getName() : "unknown");
				}
				reuseLease.commit();
			}

			@Override
			public void cancel() {
				if (reservedAction != null) {
					reservedAction.cancel();
				}
				reuseLease.cancel();
			}
		};
	}

	private AcceleratorChecker.ACT_TYPE resolveCastActionType() {
		if (_skill == null || _skill.getCastGfx() == 0) {
			return null;
		}
		// 指定傳送類無詠唱動作，不應與走路共用 visible action 窗口（否則走路中施放會被靜默拒絕）
		if (isTeleportSkill(_skillId)) {
			return null;
		}
		return switch (_skill.getActionId()) {
		case ActionCodes.ACTION_SkillAttack, ActionCodes.ACTION_AltAttack ->
			AcceleratorChecker.ACT_TYPE.SPELL_DIR;
		case ActionCodes.ACTION_SkillBuff -> AcceleratorChecker.ACT_TYPE.SPELL_NODIR;
		default -> null;
		};
	}

	/**
	 * 發動技能效果
	 */
	private void runSkill() {
		RUN_SKILL_COUNT.incrementAndGet();
		switch (_skillId) {
		case LIFE_STREAM:// 法師技能(治癒能量風暴)
			L1SpawnUtil.spawnEffect(81169,
					_skill.getBuffDuration(), _targetX, _targetY,
					_user.getMapId(), _user, 0);
			return;
			
		case CUBE_IGNITION:// 幻術師技能(立方：燃燒)
			L1SpawnUtil.spawnEffect(80149,
					_skill.getBuffDuration(), _targetX, _targetY,
					_user.getMapId(), _user, _skillId);
			return;
			
		case CUBE_QUAKE:// 幻術師技能(立方：地裂)
			L1SpawnUtil.spawnEffect(80150,
					_skill.getBuffDuration(), _targetX, _targetY,
					_user.getMapId(), _user, _skillId);
			return;
			
		case CUBE_SHOCK:// 幻術師技能(立方：衝擊)
			L1SpawnUtil.spawnEffect(80151,
					_skill.getBuffDuration(), _targetX, _targetY,
					_user.getMapId(), _user, _skillId);
			return;
			
		case CUBE_BALANCE:// 幻術師技能(立方：和諧)
			L1SpawnUtil.spawnEffect(80152,
					_skill.getBuffDuration(), _targetX, _targetY,
					_user.getMapId(), _user, _skillId);
			return;
			
		case FIRE_WALL:// 法師技能(火牢)
			//System.out.println("法師技能(火牢):"+_targetX+"/"+_targetY);
			L1SpawnUtil.doSpawnFireWall(_user, _targetX, _targetY);
			return;
		}

		// 有使用次数限制的技能使用成功与否的判断
		// 魔法屏障無法抵銷：硬編碼 + DB 外控
		for (final int skillId : EXCEPT_COUNTER_MAGIC) {
			if (_skillId == skillId) {
				_isCounterMagic = false; // 反制魔法無效
				break;
			}
		}
		if (_isCounterMagic && SkillCounterMagicExceptionTable.get().contains(_skillId)) {
			_isCounterMagic = false;
		}

		// 衝擊之暈（方案 A）：已改走攻擊系魔法傷害＋機率暈眩，不再對目標 onAction（避免額外普攻與 skillId=0 天賦路徑）
		
		if (!this.isTargetCalc(_target)) {
			return;
		}

		try {
			TargetStatus ts = null;
			L1Character cha = null;
			//int dmg = 0;
			int drainMana = 0;
			int heal = 0;
			boolean isSuccess = false;
			int undeadType = 0;
			L1SkillTimer eraseMagicLease = null;
			boolean freezeAfterAccepted = false;
			int thunderGrabMillisAfterAccepted = 0;
			boolean shockStunAfterAccepted = false;

			for (final Iterator<TargetStatus> iter = _targetList.iterator(); iter.hasNext();) {
				ts = null;
				cha = null;
				//dmg = 0;
				heal = 0;
				isSuccess = false;
				undeadType = 0;
				eraseMagicLease = null;
				freezeAfterAccepted = false;
				thunderGrabMillisAfterAccepted = 0;
				shockStunAfterAccepted = false;

				ts = iter.next();
				cha = ts.getTarget();
				ts.setDamage(0);

				//System.out.println("發動技能效果");
				if (_npc != null) {
					// 施展者是寵物 XXX
					if (_npc instanceof L1PetInstance) {
						if (isParty(_npc, cha)) {
							ts.isCalc(false);
							_dmg = 0;
							continue;
						}
					}
					// 施展者是召喚獸
					if (_npc instanceof L1SummonInstance) {
						if (isParty(_npc, cha)) {
							ts.isCalc(false);
							_dmg = 0;
							continue;
						}
					}
				}
				
				if (!ts.isCalc() || !this.isTargetCalc(cha)) {
					ts.isCalc(false);
					continue; // 不需計算
				}

				final L1Magic magic = new L1Magic(_user, cha);
				magic.setLeverage(getLeverage());
				
				if (cha instanceof L1MonsterInstance monster) { // 不死系判定
					undeadType = monster.getNpcTemplate().get_undead();
				}

				// 機率系技能確定失敗時
				// 概率系技能失败的确定
				if (((_skill.getType() == L1Skills.TYPE_CURSE) || (
						_skill.getType() == L1Skills.TYPE_PROBABILITY)) 
						&& isTargetFailure(cha)) {
					iter.remove();
					continue;
				}

				if (cha instanceof L1PcInstance) { // 僅目標為PC時發送圖示
					// 只有在目标为pc的情况下发送图标，代表使用成功
					if (_skillTime == 0) {
						_getBuffIconDuration = _skill.getBuffDuration(); // 効果時間
						
					} else {
						_getBuffIconDuration = _skillTime; // time參數非0時設為圖示效果時間
					}
				}
				
				deleteRepeatedSkills(cha); // 删除重复的技能

				//System.out.println("NPC對PC傷害計算 XXX:"+this._skill.getType());
				switch (_skill.getType()) {
				case L1Skills.TYPE_ATTACK:// 攻擊系技能且目標非使用者
					if (_user.getId() != cha.getId()) {
						// 攻击系技能和使用者除外
						if (isUseCounterMagic(cha)) { // 反制魔法發動時從清單移除
							iter.remove();
							continue;
						}
						_dmg = magic.calcMagicDamage(_skillId);
						_disintegrateCrit = (_skillId == DISINTEGRATE && magic.isDisintegrateCrit());
						eraseMagicLease = cha.captureSkillEffectLease(ERASE_MAGIC);
						
						// Talent stun is part of the immutable accepted-target plan below;
						// calculating damage must not mutate another character.
					}
					break;
					
				case L1Skills.TYPE_CURSE:
				case L1Skills.TYPE_PROBABILITY: // 機率系技能
					isSuccess = magic.calcProbabilityMagic(this._skillId);
					if (_type == TYPE_GMBUFF || _type == TYPE_PROCS) {
						isSuccess = true;
					}
					if (this._skillId != ERASE_MAGIC) {
						cha.removeSkillEffect(ERASE_MAGIC); // 魔法消除
					}
					
					if (this._skillId != FOG_OF_SLEEPING) {
						cha.removeSkillEffect(FOG_OF_SLEEPING); // 沉睡之霧
					}
					
					if (isSuccess) { // 成功但反制魔法發動時從清單移除
						if (this.isUseCounterMagic(cha)) { // 是否發動反制魔法
							iter.remove();
							continue;
						}
						
					} else { // 失敗時從清單移除
						if ((this._skillId == FOG_OF_SLEEPING) && (cha instanceof L1PcInstance)) {
							final L1PcInstance pc = (L1PcInstance) cha;
							// 297 你感覺些微地暈眩。
							pc.sendPackets(new S_ServerMessage(297));
						}
						iter.remove();
						continue;
					}
					break;
					
				case L1Skills.TYPE_HEAL: // 回復系技能
					// 回復量以負傷害表示
					this._dmg = -1 * magic.calcHealing(this._skillId);
					if (cha.hasSkillEffect(WATER_LIFE)) { // 水之元氣(回復量2倍)
						//this._dmg *= 2;
						// (>> 1: 除)  (<< 1: 乘)
						this._dmg = (this._dmg << 1);
					}
					
					if (cha.hasSkillEffect(POLLUTE_WATER)) { // 污濁之水(回復量1/2倍)
						//this._dmg /= 2;
						// (>> 1: 除)  (<< 1: 乘)
						this._dmg = (this._dmg >> 1);
					}
					
					if (cha.hasSkillEffect(ADLV80_2_2)) {// 污濁的水流(水龍副本 回復量1/2倍)
						this._dmg = (this._dmg >> 1);
					}
					
					if (cha.hasSkillEffect(ADLV80_2_3)) {
						this._dmg *= -1;
					}
					// 生命的祝福：原治癒量 + 外控額外治癒量（_dmg 為負值，減去正數=治癒量增加）
					if (this._skillId == NATURES_BLESSING && ConfigSkill.NATURES_BLESSING_HEAL != 0) {
						this._dmg -= ConfigSkill.NATURES_BLESSING_HEAL;
					}
					break;
				}

				// TODO SKILL移轉
				final SkillMode mode = getSkillMode();
				if (mode != null) {
					// 衝擊之暈：勿在此 mode.start（會把 _dmg 歸零）；暈眩改由下方與奪命之雷同級之機率區塊處理
					if (this._skillId != SHOCK_STUN) {
						// 施展者是PC
						if (this._user instanceof L1PcInstance) {
							switch (this._skillId) {
							case TELEPORT:// 指定傳送5
							case MASS_TELEPORT:// 集體傳送術69
								this._dmg = mode.start(this._player, cha, magic, this._bookmarkId);
								break;
								
							case CALL_CLAN:// 呼喚盟友
							case RUN_CLAN:// 援護盟友118
								this._dmg = mode.start(this._player, cha, magic, this._targetID);
								break;
							default:
								this._dmg = mode.start(this._player, cha, magic, this._getBuffIconDuration);
								break;
							}
						}
						// 施展者是NPC
						if (this._user instanceof L1NpcInstance) {
							this._dmg = mode.start(this._npc, cha, magic, this._getBuffIconDuration);
						}
					}
				} else {
					// ■■■■ 僅撰寫有個別處理的技能 ■■■■
					// 需要個別處理的技能（無法簡單以技能屬系判斷）
					// 已使用技能時不處理（重複使用無效的技能）
					// 衝擊之暈可疊加為例外
					if ((_skill.getType() == L1Skills.TYPE_OTHER)
							&& "buff".equals(_skill.getTarget())
							&& (_skill.getBuffDuration() > 0)) {
						final int duration = _getBuffIconDuration > 0
								? _getBuffIconDuration : _skill.getBuffDuration();
						cha.setSkillEffect(getEffectSkillId(), duration);
					}
					// 鎧甲護持重複施放仍需走個別處理，才能再次送出裝備發光訊息。
					if (cha.hasSkillEffect(this._skillId) && (this._skillId != BLESSED_ARMOR)) {
						this.addMagicList(cha, true); // 覆寫目標的魔法效果時間
						if (this._skillId != SHAPE_CHANGE) { // 變形術
							continue;
						}
					}
				}

				if (this._skillId == DETECTION) { // 無所遁形術
					if (cha instanceof L1NpcInstance) {
						final L1NpcInstance npc = (L1NpcInstance) cha;
						final int hiddenStatus = npc.getHiddenStatus();
						if (hiddenStatus == L1NpcInstance.HIDDEN_STATUS_SINK) {
							npc.appearOnGround(this._player);
						}
					}
					
				} else if (this._skillId == COUNTER_DETECTION) { // 反制無所遁形術
					if (cha instanceof L1PcInstance) {
						this._dmg = magic.calcMagicDamage(this._skillId);
						
					} else if (cha instanceof L1NpcInstance) {
						final L1NpcInstance npc = (L1NpcInstance) cha;
						final int hiddenStatus = npc.getHiddenStatus();
						if (hiddenStatus == L1NpcInstance.HIDDEN_STATUS_SINK) {
							npc.appearOnGround(this._player);
						} else {
							this._dmg = 0;
						}
						
					} else {
						this._dmg = 0;
					}

				// ★★★ 回復系技能 ★★★
				} else if (
						(this._skillId == HEAL) || 
						(this._skillId == EXTRA_HEAL) || 
						(this._skillId == GREATER_HEAL) || 
						(this._skillId == FULL_HEAL) || 
						(this._skillId == HEAL_ALL) || 
						(this._skillId == NATURES_TOUCH) || 
						(this._skillId == NATURES_BLESSING)
						) {
					if (this._user instanceof L1PcInstance) {
						cha.removeSkillEffect(WATER_LIFE);
					}
					
				// ★★★ 攻擊系技能 ★★★
				// 寒冰之觸、吸血鬼之觸
				} else if ((this._skillId == CHILL_TOUCH) || (this._skillId == VAMPIRIC_TOUCH)) {
					heal = this._dmg;

				} else if ((this._skillId == 10026) || (this._skillId == 10027)
						|| (this._skillId == 10028) || (this._skillId == 10029)) { // 安息攻撃
					if (this._user instanceof L1NpcInstance) {
						this._user.broadcastPacketX8(new S_NpcChat(this._npc, "$3717")); // 賜予你安息吧。

					} else {
						this._player.broadcastPacketX8(new S_Chat(this._player, "$3717")); // 賜予你安息吧。
					}
					
				} else if (this._skillId == 10057) { // 牽引術
					L1Teleport.teleportToTargetFront(cha, this._user, 1);
					
				// ★★★ 機率系技能 ★★★
				} else if ((this._skillId == SLOW) || (this._skillId == MASS_SLOW)
						|| (this._skillId == ENTANGLE)) { // 緩速術、集體緩速
					// 緩速術、藤蔓術
					if (cha instanceof L1PcInstance) {
						final L1PcInstance pc = (L1PcInstance) cha;
						if (pc.getHasteItemEquipped() > 0) { 
							continue;
						}
					}
					if (cha.getBraveSpeed() == 5) {// 具有強化勇水狀態
						continue;
					}
					switch (cha.getMoveSpeed()) {
					case 0:
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_SkillHaste(pc.getId(), 2, this._getBuffIconDuration));
						}
						cha.broadcastPacketAll(new S_SkillHaste(cha.getId(), 2, this._getBuffIconDuration));
						cha.setMoveSpeed(2);
						break;
						
					case 1:
						int skillNum = 0;
						if (cha.hasSkillEffect(HASTE)) {
							skillNum = HASTE;
							
						} else if (cha.hasSkillEffect(GREATER_HASTE)) {
							skillNum = GREATER_HASTE;
							
						} else if (cha.hasSkillEffect(STATUS_HASTE)) {
							skillNum = STATUS_HASTE;
						}
						
						if (skillNum != 0) {
							cha.removeSkillEffect(skillNum);
							cha.removeSkillEffect(this._skillId);
							cha.setMoveSpeed(0);
							continue;
						}
						break;
					}
					
				/*} else if ((this._skillId == CURSE_BLIND) || (this._skillId == DARKNESS)) {
					if (cha instanceof L1PcInstance) {
						final L1PcInstance pc = (L1PcInstance) cha;
						if (pc.hasSkillEffect(STATUS_FLOATING_EYE)) {
							pc.sendPackets(new S_CurseBlind(2));
							
						} else {
							pc.sendPackets(new S_CurseBlind(1));
						}
					}*/
					
				} else if (this._skillId == CURSE_POISON) {
					L1DamagePoison.doInfection(this._user, cha, 3000, 5);

				/*} else if ((this._skillId == CURSE_PARALYZE)
						|| (this._skillId == CURSE_PARALYZE2)) {
					if (!cha.hasSkillEffect(EARTH_BIND)
							&& !cha.hasSkillEffect(ICE_LANCE)
							&& !cha.hasSkillEffect(FREEZING_BLIZZARD)
							&& !cha.hasSkillEffect(FREEZING_BREATH)) {
						if (cha instanceof L1PcInstance) {
							L1CurseParalysis.curse(cha, 8000, 16000);
							
						} else if (cha instanceof L1MonsterInstance) {
							L1CurseParalysis.curse(cha, 0, 16000);
						}
					}*/
					
				} else if (this._skillId == WEAKNESS) { // 弱化術
					if (!_procCast && cha instanceof L1PcInstance pc) {
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						pc.updateBuffStatus();
					}
					
				} else if (this._skillId == DISEASE) { // 疾病術
					if (!_procCast && cha instanceof L1PcInstance pc) {
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						pc.updateBuffStatus();
					}
					
				} else if ((this._skillId == ICE_LANCE) // 冰矛術
				|| (this._skillId == FREEZING_BLIZZARD) // 冰暴術
				|| (this._skillId == FREEZING_BREATH)) { // 冰霜吐息
					// 只做機率計算；凍結狀態與封包必須等主傷害 accepted。
					this._isFreeze = magic.calcProbabilityMagic(this._skillId);
					freezeAfterAccepted = this._isFreeze;
					
				} else if (this._skillId == EARTH_BIND) { // 大地束縛
					if (cha instanceof L1PcInstance) {
						final L1PcInstance pc = (L1PcInstance) cha;
						pc.sendPacketsAll(new S_Poison(pc.getId(), 2));
						pc.sendPackets(new S_Paralysis(S_Paralysis.TYPE_FREEZE, true));
						
					} else if ((cha instanceof L1MonsterInstance)
							|| (cha instanceof L1SummonInstance)
							|| (cha instanceof L1PetInstance)) {
						final L1NpcInstance npc = (L1NpcInstance) cha;
						
						npc.broadcastPacketAll(new S_Poison(npc.getId(), 2));
						npc.setParalyzed(true);
					}
					
				/*} else if (this._skillId == WIND_SHACKLE) { // 風之枷鎖
					if (cha instanceof L1PcInstance) {
						final L1PcInstance pc = (L1PcInstance) cha;
						pc.sendPackets(new S_PacketBoxWindShackle(pc.getId(),
								this._getBuffIconDuration));
					}*/
					
				} else if (this._skillId == TURN_UNDEAD) {
					if ((undeadType == 1) || (undeadType == 3)) {
						// 以傷害為目標HP
						this._dmg = cha.getCurrentHp();
					}
					
				} else if (this._skillId == MANA_DRAIN) { // 魔力吸取
					final Random random = new Random();
					final int chance = random.nextInt(10) + 5;
					drainMana = chance + (this._user.getInt() / 2);
					if (cha.getCurrentMp() < drainMana) {
						drainMana = cha.getCurrentMp();
					}
					
				} else if (this._skillId == WEAPON_BREAK) { // 武器破壞
					/*
					 * 對 PC：武器耐久 1~(施法者 INT/3)。
					 * 對 NPC：疊加壞物層數（每層 -5% 物理輸出，最多 10 層），見 L1AttackNpc.npcDmgMode。
					 */
					if ((this._calcType == PC_PC) || (this._calcType == NPC_PC)) {
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							final L1ItemInstance weapon = pc.getWeapon();
							if (weapon != null) {
								final Random random = new Random();
								final int weaponDamage = random.nextInt(this._user.getInt() / 3) + 1;
								// \f1你的%0損壞了。
								pc.sendPackets(new S_ServerMessage(268, weapon.getLogName()));
								pc.getInventory().receiveDamage(weapon, weaponDamage);
							}
						}
					} else {
						((L1NpcInstance) cha).addWeaponBreakStack();
					}
					
				} else if (this._skillId == FOG_OF_SLEEPING) {
					if (cha instanceof L1PcInstance) {
						final L1PcInstance pc = (L1PcInstance) cha;
						pc.sendPackets(new S_Paralysis(S_Paralysis.TYPE_SLEEP, true));
					}
					cha.setSleeped(true);

				} else if (this._skillId == GUARD_BRAKE) { // 防禦破壞
					if (cha instanceof L1PcInstance) {
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addAc(15); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
					}
					
				} else if (this._skillId == HORROR_OF_DEATH) { // 死亡恐懼
					if (cha instanceof L1PcInstance) {
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 4] 移除直接屬性修改，改用 Buff 系統
						// pc.addStr(-5); // 已遷移至 L1PcBuffAbilityCalc
						// pc.addInt(-5); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
					}
				}

				// ●●●● 僅對PC生效的技能 ●●●●
				if ((this._calcType == PC_PC) || (this._calcType == NPC_PC)) {
					// ★★★ 特殊系技能 ★★★
					if (this._skillId == CREATE_MAGICAL_WEAPON) { // 創造魔法武器
						// 魔法武器
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						final L1ItemInstance item = pc.getInventory().getItem(
								this._itemobjid);
						if ((item != null) && (item.getItem().getType2() == 1)) {
							final int item_type = item.getItem().getType2();
							final int safe_enchant = item.getItem().get_safeenchant();
							final int enchant_level = item.getEnchantLevel();
							String item_name = item.getName();
							if (safe_enchant < 0) { // 強化不可
								pc.sendPackets( // \f1什麼都沒發生。
										new S_ServerMessage(79));
							} else if (safe_enchant == 0) { // 安全圏+0
								pc.sendPackets( // \f1什麼都沒發生。
										new S_ServerMessage(79));
							} else if ((item_type == 1) && (enchant_level == 0)) {
								if (!item.isIdentified()) {// 未鑑定
									pc.sendPackets( // \f1%0散發%2%1光芒。
											new S_ServerMessage(161, item_name,
													"$245", "$247"));
								} else {
									item_name = "+0 " + item_name;
									pc.sendPackets( // \f1%0散發%2%1光芒。
											new S_ServerMessage(161, "+0 "
													+ item_name, "$245", "$247"));
								}
								item.setEnchantLevel(1);
								pc.getInventory().updateItem(item,
										L1PcInventory.COL_ENCHANTLVL);
							} else {
								pc.sendPackets( // \f1什麼都沒發生。
										new S_ServerMessage(79));
							}
						} else {
							pc.sendPackets( // \f1什麼都沒發生。
									new S_ServerMessage(79));
						}
						
					} else if (this._skillId == BRING_STONE) { // 召石術
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						final Random random = new Random();
						final L1ItemInstance item = pc.getInventory().getItem(
								this._itemobjid);
						if (item != null) {
							final int dark = (int) (10 + (pc.getLevel() * 0.8) + (pc
									.getWis() - 6) * 1.2);
							final int brave = (int) (dark / 2.1);
							final int wise = (int) (brave / 2.0);
							final int kayser = (int) (wise / 1.9);
							final int chance = random.nextInt(100) + 1;
							switch (item.getItem().getItemId()) {
							case 40320:
								bringStone(pc, item, dark, chance, 40321, "$2475");
								break;
							case 40321:
								bringStone(pc, item, brave, chance, 40322, "$2476");
								break;
							case 40322:
								bringStone(pc, item, wise, chance, 40323, "$2477");
								break;
							case 40323:
								bringStone(pc, item, kayser, chance, 40324, "$2478");
								break;
							default:
								break;
							}
						}

					/*} else if ((this._skillId == LESSER_ELEMENTAL)
							|| (this._skillId == GREATER_ELEMENTAL)) { // 次級精靈、高級精靈
						final L1PcInstance pc = (L1PcInstance) cha;
						final int attr = pc.getElfAttr();
						if (attr != 0) { // 非無屬性時執行
							if (!pc.getMap().isRecallPets()) {
								// 353：在這附近無法召喚怪物。  
								pc.sendPackets(new S_ServerMessage(353));
								return;
							}

							int petcost = 0;
							final Map<Integer, L1NpcInstance> pcPetList = pc.getPetList();
							final Object[] petlist = pcPetList != null ? pcPetList.values()
							.toArray() : new Object[0];
							for (final Object pet : petlist) {
								if (!(pet instanceof L1NpcInstance)) {
									continue;
								}
								// 當前寵物花費
								petcost += ((L1NpcInstance) pet)
								.getPetcost();
							}

							if (petcost == 0) { // 無所屬NPC時執行
								int summonid = 0;
								int summons[];
								if (this._skillId == LESSER_ELEMENTAL) { // 次級精靈[地火水風]
									summons = new int[] { 45306, 45303,
											45304, 45305 };
								} else {
									// 高級精靈[地火水風]
									summons = new int[] { 81053, 81050,
											81051, 81052 };
								}
								int npcattr = 1;
								for (int i = 0; i < summons.length; i++) {
									if (npcattr == attr) {
										summonid = summons[i];
										i = summons.length;
									}
									npcattr *= 2;
								}
								// 特殊設定時隨機出現
								if (summonid == 0) {
									final Random random = new Random();
									final int k3 = random.nextInt(4);
									summonid = summons[k3];
								}

								final L1Npc npcTemp = NpcTable.get()
								.getTemplate(summonid);
								final L1SummonInstance summon = new L1SummonInstance(
										npcTemp, pc);
								summon.setPetcost(pc.getCha() + 7); // 精靈以外無法所屬其他NPC
							}
						}*/
						
					} else if (this._skillId == ABSOLUTE_BARRIER) { // 絕對屏障
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						pc.stopHpRegeneration();
						pc.stopMpRegeneration();
					}

					// ★★★ 變化系技能（附魔） ★★★
					if (this._skillId == LIGHT) { // 光術
						// addMagicList()後，以turnOnOffLight()發送封包
						
					} else if (this._skillId == GLOWING_AURA) { // 光輝術
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addHitup(5); // 已遷移至 L1PcBuffAbilityCalc
						// pc.addBowHitup(5); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						pc.addMr(20);
						pc.sendPackets(new S_SPMR(pc));
						IndependentSkillIcon.sendStart(pc, this._skillId, this._getBuffIconDuration);
						
					} else if (this._skillId == SHINING_AURA) { // 鋼鐵士氣
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addAc(-8); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						IndependentSkillIcon.sendStart(pc, this._skillId, this._getBuffIconDuration);
						
					} else if (this._skillId == BRAVE_AURA) { // 衝擊士氣
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addDmgup(5); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						pc.sendPackets(new S_PacketBoxIconAura(116,
								this._getBuffIconDuration));
						
					} else if (this._skillId == SHIELD) { // 保護盾
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addAc(-2); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						IndependentSkillIcon.sendStart(pc, this._skillId, this._getBuffIconDuration);
						
					} else if (this._skillId == SHADOW_ARMOR) { // 暗影盔甲
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addAc(-3); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						IndependentSkillIcon.sendStart(pc, this._skillId, this._getBuffIconDuration);

					} else if (this._skillId == DRESS_DEXTERITY) { // 敏捷術
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 4] 移除直接屬性修改，改用 Buff 系統
						// pc.addDex((byte) 2); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						IndependentSkillIcon.sendStart(pc, this._skillId, this._getBuffIconDuration);

					} else if (this._skillId == DRESS_MIGHTY) { // 強力術
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 4] 移除直接屬性修改，改用 Buff 系統
						// pc.addStr((byte) 2); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						IndependentSkillIcon.sendStart(pc, this._skillId, this._getBuffIconDuration);

					} else if (this._skillId == SHADOW_FANG) { // 暗影之牙（改為玩家 Buff，捨棄裝備紀錄）
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						if (pc.getWeapon() == null) {
							pc.sendPackets(new S_ServerMessage(79));
							return;
						}
						pc.sendPackets(new S_ServerMessage(161, pc.getWeapon().getLogName(), "$245", "$247"));
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addDmgup(5); // 已遷移至 L1PcBuffAbilityCalc
						final int duration = (this._skillTime == 0 ? this._skill.getBuffDuration() : this._skillTime) * 1000;
						cha.setSkillEffect(this._skillId, duration);
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						
					} else if (this._skillId == ENCHANT_WEAPON) { // 擬似魔法武器（改為玩家 Buff）
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						if (pc.getWeapon() == null) {
							pc.sendPackets(new S_ServerMessage(79));
							return;
						}
						pc.sendPackets(new S_ServerMessage(161, pc.getWeapon().getLogName(), "$245", "$247"));
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addDmgup(2); // 已遷移至 L1PcBuffAbilityCalc
						final int duration = (this._skillTime == 0 ? this._skill.getBuffDuration() : this._skillTime) * 1000;
						cha.setSkillEffect(this._skillId, duration);
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						IndependentSkillIcon.sendStart(pc, this._skillId, this._getBuffIconDuration);
						
					} else if (this._skillId == HOLY_WEAPON) { // 神聖武器（改為玩家 Buff）
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						if (pc.getWeapon() == null) {
							pc.sendPackets(new S_ServerMessage(79));
							return;
						}
						pc.sendPackets(new S_ServerMessage(161, pc.getWeapon().getLogName(), "$245", "$247"));
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addDmgup(1); // 已遷移至 L1PcBuffAbilityCalc
						// pc.addHitup(1); // 已遷移至 L1PcBuffAbilityCalc
						final int duration = (this._skillTime == 0 ? this._skill.getBuffDuration() : this._skillTime) * 1000;
						cha.setSkillEffect(this._skillId, duration);
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						IndependentSkillIcon.sendStart(pc, this._skillId, this._getBuffIconDuration);
						
					} else if (this._skillId == BLESS_WEAPON) { // 祝福魔法武器（改為玩家 Buff）
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						if (pc.getWeapon() == null) {
							pc.sendPackets(new S_ServerMessage(79));
							return;
						}
						pc.sendPackets(new S_ServerMessage(161, pc.getWeapon().getLogName(), "$245", "$247"));
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addDmgup(2); // 已遷移至 L1PcBuffAbilityCalc
						// pc.addHitup(2); // 已遷移至 L1PcBuffAbilityCalc
						final int duration = (this._skillTime == 0 ? this._skill.getBuffDuration() : this._skillTime) * 1000;
						cha.setSkillEffect(this._skillId, duration);
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						IndependentSkillIcon.sendStart(pc, this._skillId, this._getBuffIconDuration);
						
					} else if (this._skillId == BLESSED_ARMOR) { // 鎧甲護持（改為玩家 Buff）
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						final L1ItemInstance armor = pc.getInventory().getItemEquipped(2, 2);
						if ((armor == null) || (armor.getItem().getType2() != 2) || (armor.getItem().getType() != 2)) {
							pc.sendPackets(new S_ServerMessage(79));
							return;
						}
						pc.sendPackets(new S_ServerMessage(161, armor.getLogName(), "$245", "$247"));
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addAc(-3); // 已遷移至 L1PcBuffAbilityCalc
						final int duration = (this._skillTime == 0 ? this._skill.getBuffDuration() : this._skillTime) * 1000;
						cha.setSkillEffect(this._skillId, duration);
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						
					} else if (this._skillId == EARTH_BLESS) { // 大地祝福
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addAc(-7); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						IndependentSkillIcon.sendStart(pc, this._skillId, this._getBuffIconDuration);
						
					} else if (this._skillId == RESIST_MAGIC) { // 魔法抵抗
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						pc.addMr(10);
						pc.sendPackets(new S_SPMR(pc));
						
					} else if (this._skillId == CLEAR_MIND) { // 淨化心靈
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 4] 移除直接屬性修改，改用 Buff 系統
						// pc.addWis((byte) 3); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						pc.resetBaseMr();
						
					} else if (this._skillId == RESIST_ELEMENTAL) { // 屬性抵抗
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						pc.addWind(10);
						pc.addWater(10);
						pc.addFire(10);
						pc.addEarth(10);
						pc.sendPackets(new S_OwnCharAttrDef(pc));

					} else if (this._skillId == ELEMENTAL_PROTECTION) { // 屬性防護
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// 精靈屬性為位元遮罩（1地2火4水8風）；雙屬性時兩系各 +50
						final int attr = pc.getElfAttr();
						if ((attr & 1) != 0) {
							pc.addEarth(50);
						}
						if ((attr & 2) != 0) {
							pc.addFire(50);
						}
						if ((attr & 4) != 0) {
							pc.addWater(50);
						}
						if ((attr & 8) != 0) {
							pc.addWind(50);
						}
					} else if (this._skillId == IRON_SKIN) { // 鋼鐵皮膚
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addAc(-10); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						IndependentSkillIcon.sendStart(pc, this._skillId, this._getBuffIconDuration);
						
					} else if (this._skillId == EARTH_SKIN) { // 大地皮膚
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addAc(-6); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						IndependentSkillIcon.sendStart(pc, this._skillId, this._getBuffIconDuration);
						
					} else if (this._skillId == PHYSICAL_ENCHANT_STR) { // 體能強化：STR
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 2] 已遷移至 L1PcBuffAbilityCalc (addMagicList 中統一 updateBuffStatus)
						// pc.addStr((byte) 5); // 移除硬編碼，改由 skills_buff_ability 控制
						IndependentSkillIcon.sendStart(pc, this._skillId, this._getBuffIconDuration);
						
					} else if (this._skillId == PHYSICAL_ENCHANT_DEX) { // 體能強化：DEX
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 2] 已遷移至 L1PcBuffAbilityCalc (addMagicList 中統一 updateBuffStatus)
						// pc.addDex((byte) 5); // 移除硬編碼，改由 skills_buff_ability 控制
						IndependentSkillIcon.sendStart(pc, this._skillId, this._getBuffIconDuration);
						
					} else if (this._skillId == FIRE_WEAPON) { // 烈焰武器
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addDmgup(4); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						IndependentSkillIcon.sendStart(pc, this._skillId,
								this._getBuffIconDuration);
						
					} else if (this._skillId == FIRE_BLESS) { // 烈焰祝福
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addDmgup(4); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						IndependentSkillIcon.sendStart(pc, this._skillId,
								this._getBuffIconDuration);
						
					} else if (this._skillId == BURNING_WEAPON) { // 燃燒武器
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addDmgup(6); // 已遷移至 L1PcBuffAbilityCalc
						// pc.addHitup(3); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						IndependentSkillIcon.sendStart(pc, this._skillId,
								this._getBuffIconDuration);
						
					} else if (this._skillId == WIND_SHOT) { // 風之箭
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addBowHitup(6); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						IndependentSkillIcon.sendStart(pc, this._skillId,
								this._getBuffIconDuration);
						
					} else if (this._skillId == STORM_EYE) { // 暴風之眼
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addBowHitup(2); // 已遷移至 L1PcBuffAbilityCalc
						// pc.addBowDmgup(3); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						IndependentSkillIcon.sendStart(pc, this._skillId,
								this._getBuffIconDuration);
						
					} else if (this._skillId == STORM_SHOT) { // 暴風箭
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addBowDmgup(5); // 已遷移至 L1PcBuffAbilityCalc
						// pc.addBowHitup(-1); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						IndependentSkillIcon.sendStart(pc, this._skillId,
								this._getBuffIconDuration);
						
					} else if (this._skillId == BERSERKERS) { // 狂暴術
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addAc(10); // 已遷移至 L1PcBuffAbilityCalc
						// pc.addDmgup(5); // 已遷移至 L1PcBuffAbilityCalc
						// pc.addHitup(2); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						
					} else if (this._skillId == SHAPE_CHANGE) { // 變形術
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						if (pc.getInventory().checkEquipped(20281)) { // 變形控制戒指
							pc.sendPackets(new S_ShowPolyList(pc.getId()));
							pc.setSummonMonster(false);
							pc.setShapeChange(true);

						} else {
							// 與變形魔杖共用「道具_變形控制魔杖」的機率與變身來源。
							pc.setShapeChange(false);
							pc.setSummonMonster(false);
							final int polyId = WandPolyTable.get().getRandomPolyId();
							L1PolyMorph.doPoly(pc, polyId, this._getBuffIconDuration,
									L1PolyMorph.MORPH_BY_ITEMMAGIC);
						}

					} else if (this._skillId == GREATER_HASTE) { // 高級加速術
						if (cha.getMoveSpeed() != 2) { // 非緩速狀態時
							cha.setMoveSpeed(1);
							if (cha instanceof L1PcInstance pc) {
								pc.setDrink(false);
								pc.sendPackets(new S_SkillHaste(pc.getId(), 1, this._getBuffIconDuration));
							}
							cha.broadcastPacketAll(new S_SkillHaste(cha.getId(), 1, 0));
							
						} else { // 緩速狀態中
							int skillNum = 0;
							if (cha.hasSkillEffect(SLOW)) {
								skillNum = SLOW;
							} else if (cha.hasSkillEffect(MASS_SLOW)) {
								skillNum = MASS_SLOW;
							} else if (cha.hasSkillEffect(ENTANGLE)) {
								skillNum = ENTANGLE;
							}
							if (skillNum != 0) {
								cha.removeSkillEffect(skillNum);
								cha.removeSkillEffect(GREATER_HASTE);
								cha.setMoveSpeed(0);
								continue;
							}
						}
						
					} else if ((this._skillId == HOLY_WALK)
							|| (this._skillId == MOVING_ACCELERATION)
							|| (this._skillId == WIND_WALK)) { // 神聖疾走、疾風術、風之疾走
						cha.setBraveSpeed(4);
						if (cha instanceof L1PcInstance pc) {
							pc.sendPackets(new S_SkillBrave(pc.getId(), 4, this._getBuffIconDuration));
						}
						cha.broadcastPacketAll(new S_SkillBrave(cha.getId(), 4, 0));
						
					} else if (this._skillId == ILLUSION_OGRE) { // 幻覺：食人魔
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addDmgup(4); // 已遷移至 L1PcBuffAbilityCalc
						// pc.addHitup(4); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統
						
					/*} else if (this._skillId == ILLUSION_LICH) { // 幻術：巫妖
						final L1PcInstance pc = (L1PcInstance) cha;
						pc.addSp(2);
						pc.sendPackets(new S_SPMR(pc));*/
						
					} else if (this._skillId == ILLUSION_DIA_GOLEM) { // 幻覺：鑽石高崙
						// [修正] 假人不需要此技能效果，跳過
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						final L1PcInstance pc = (L1PcInstance) cha;
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// pc.addAc(-20); // 已遷移至 L1PcBuffAbilityCalc
						pc.updateBuffStatus(); // 使用新的 Buff 系統

					/*} else if (this._skillId == INSIGHT) { // 洞察
						final L1PcInstance pc = (L1PcInstance) cha;
						pc.addStr((byte) 1);
						pc.addCon((byte) 1);
						pc.addDex((byte) 1);
						pc.addWis((byte) 1);
						pc.addInt((byte) 1);*/
						
					/*} else if (this._skillId == PANIC) {// 恐惧217
						final L1PcInstance pc = (L1PcInstance) cha;
						pc.addStr((byte) -1);
						pc.addCon((byte) -1);
						pc.addDex((byte) -1);
						pc.addWis((byte) -1);
						pc.addInt((byte) -1);*/

					} else if (_skillId == THUNDER_GRAB) {// 奪命之雷192（束縛機率：skills_負面技能控制 或 L1MagicPc 三檔後備）
						if (magic.calcProbabilityMagic(THUNDER_GRAB)) {
							if (!cha.hasSkillEffect(L1SkillId.MOVE_STOP)) {
								final int[] timer = ConfigSkill.SLAY_BREAK_TIMER;
								final int baseSec = (timer != null && timer.length >= 2) ? timer[0] : 4;
								final int randomSec = (timer != null && timer.length >= 2) ? timer[1] : 0;
								final int sec = baseSec + (randomSec > 0
										? ThreadLocalRandom.current().nextInt(randomSec + 1) : 0);
								thunderGrabMillisAfterAccepted = Math.max(1, sec) * 1000;
							}
						}
					} else if (_skillId == SHOCK_STUN) {// 衝擊之暈87（暈眩機率：skills_負面技能控制 或 L1MagicPc 舊式；與魔法傷害／天賦分離）
						if (magic.calcProbabilityMagic(SHOCK_STUN)) {
							shockStunAfterAccepted = true;
						}
					}
				}

				// ●●●● 僅對NPC生效的技能 ●●●●
				if ((_calcType == PC_NPC) || (_calcType == NPC_NPC)) {
					// ★★★ 寵物系技能 ★★★
					if ((_skillId == TAMING_MONSTER)
							&& ((L1MonsterInstance) cha).getNpcTemplate().isTamable()) { // 可馴服怪物
						if (_player.canTakePet(6)) {
							final L1SummonInstance summon = new L1SummonInstance(
									this._targetNpc, this._user, false);
							this._target = summon; // 目標替換
						} else {
							this._player.sendPackets(new S_ServerMessage(319)); // \f1無法操控更多怪物。
						}
						
					} else if (this._skillId == CREATE_ZOMBIE) { // 召喚殭屍
						if (this._player.canTakePet(6)) {
							final CreateZombieRequest request = new CreateZombieRequest(
									this._targetNpc, this._player);
							CREATE_ZOMBIE_ASYNC_COUNT.incrementAndGet();
							executeMonitored("create-zombie", request);
						} else {
							this._player.sendPackets(new S_ServerMessage(319)); // \f1無法操控更多怪物。
						}
						
					} else if (this._skillId == WEAK_ELEMENTAL) { // 弱化精靈
						if (cha instanceof L1MonsterInstance) {
							final L1Npc npcTemp = ((L1MonsterInstance) cha).getNpcTemplate();
							final int weakAttr = npcTemp.get_weakAttr();
							if ((weakAttr & 1) == 1) { // 地
								cha.broadcastPacketX8(new S_SkillSound(cha.getId(), 2169));
							}
							if ((weakAttr & 2) == 2) { // 火
								cha.broadcastPacketX8(new S_SkillSound(cha.getId(), 2167));
							}
							if ((weakAttr & 4) == 4) { // 水
								cha.broadcastPacketX8(new S_SkillSound(cha.getId(), 2166));
							}
							if ((weakAttr & 8) == 8) { // 風
								cha.broadcastPacketX8(new S_SkillSound(cha.getId(), 2168));
							}
						}
						
					} else if (this._skillId == RETURN_TO_NATURE) { // 回歸自然
						if (cha instanceof L1SummonInstance) {
							final L1SummonInstance summon = (L1SummonInstance) cha;
							summon.broadcastPacketX10(new S_SkillSound(summon.getId(), 2245));
							summon.returnToNature();
							
						} else {
							if (this._user instanceof L1PcInstance) {
								this._player.sendPackets(new S_ServerMessage(79));
							}
						}
					}
				}

				// ■■■■ 個別處理結束 ■■■■

				if ((this._skill.getType() == L1Skills.TYPE_HEAL)
						&& (this._calcType == PC_NPC) && (undeadType == 1)) {
					this._dmg *= -1; // 不死系受回復技能時轉為傷害
				}

				if ((this._skill.getType() == L1Skills.TYPE_HEAL)
						&& (this._calcType == PC_NPC) && (undeadType == 3)) {
					this._dmg = 0; // 不死系Boss受回復技能時無效
				}

				if (((cha instanceof L1TowerInstance) || (cha instanceof L1DoorInstance)) && (this._dmg < 0)) { // 對守護塔、門使用治癒術
					this._dmg = 0;
				}

				//System.out.println("dmg1:"+dmg); XXX
				ts.setDamage(this._dmg);
				final boolean gateOnDamageAcceptance = this._dmg > 0;
				final SkillTargetMutationPlan targetMutations = new SkillTargetMutationPlan(
						eraseMagicLease, freezeAfterAccepted,
						thunderGrabMillisAfterAccepted, shockStunAfterAccepted);
				final DamageCommitPlan acceptedPlan = gateOnDamageAcceptance
						? acceptedTargetSideEffectPlan(cha, heal, targetMutations)
						: DamageCommitPlan.none();
				if ((this._dmg != 0) || (drainMana != 0)) {
					//System.out.println("結果質2:(HP) " + this._dmg);
					if (gateOnDamageAcceptance) {
						magic.commit(this._dmg, drainMana, acceptedPlan);
					} else {
						magic.commit(this._dmg, drainMana);
					}
				}

				// 隱身類技能不可延後到圖示階段；先建立狀態，再更新本人及周圍玩家的可見性。
				if (isInvisibilitySkill()) {
					applyInvisibilityEffect(cha);
				}
				if (!gateOnDamageAcceptance) {
					queueTargetSideEffects(cha, heal, targetMutations);
				}
			}

			if ((_skillId == DETECTION) || (_skillId == COUNTER_DETECTION)) {
				final L1PcInstance detectionPc = _player;
				_targetSideEffectQueue.add(() -> detection(detectionPc));
			}

			if (!_targetSideEffectQueue.isEmpty() && _type == TYPE_LOGIN) {
				flushTargetSideEffects();
			}

		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	private boolean isInvisibilitySkill() {
		return (this._skillId == INVISIBILITY) || (this._skillId == BLIND_HIDING);
	}

	/**
	 * 先建立隱身技能狀態，再通知本人及周圍玩家更新可見狀態。
	 */
	private void applyInvisibilityEffect(final L1Character cha) {
		if (!(cha instanceof L1PcInstance pc)) {
			return;
		}
		this.addMagicList(cha, false);
		pc.sendPackets(new S_Invis(pc.getId(), 1));
		pc.broadcastPacketAll(new S_RemoveObject(pc));
	}

	/**
	 * 回傳取消術無法解除的技能。 被技能攻擊而暫時無法解除
	 */
	/*private boolean isNotCancelable(final int skillNum) {
		return (skillNum == ENCHANT_WEAPON) ||
		(skillNum == BLESSED_ARMOR) ||
		(skillNum == ABSOLUTE_BARRIER) ||
		(skillNum == ADVANCE_SPIRIT) ||
		(skillNum == SHOCK_STUN) ||
		(skillNum == SHADOW_FANG) ||
		(skillNum == REDUCTION_ARMOR) ||
		(skillNum == SOLID_CARRIAGE) ||
		(skillNum == COUNTER_BARRIER) ||
		(skillNum == AWAKEN_ANTHARAS) ||
		(skillNum == AWAKEN_FAFURION) ||
		(skillNum == AWAKEN_VALAKAS);
	}*/

	private void detection(final L1PcInstance pc) {
		final Collection<L1PcInstance> visiblePlayers = World.get().getVisiblePlayer(pc);
		if (visiblePlayers == null) {
			return;
		}
		for (final L1PcInstance tgt : visiblePlayers) {
			if (tgt == null) {
				continue;
			}
			if (!tgt.isGmInvis() && tgt.isInvisble()) {
				tgt.delInvis();
			}
		}
		
		// 偵測陷阱的處理
		WorldTrap.get().onDetection(pc);
	}

	/**
	 * 目標判定
	 * @param cha
	 * @param cha 
	 * @return
	 */
	private boolean isTargetCalc(final L1Character cha) {
		// 攻擊魔法的 Non-PvP / 安全區判定
		if (this._skill.getTarget().equals("attack") && (this._skillId != TURN_UNDEAD)) { // 攻撃魔法

			// [安全區統一攔截] 攻擊者在安全區時，無法對任何目標使用攻擊魔法
			// 目標在安全區時亦同，與物理攻擊行為一致
			// 例外：怪物與訓練稻草人不受安全區限制，允許玩家正常攻擊
			if (this._player != null && !(cha instanceof L1MonsterInstance)
					&& !(cha instanceof L1ScarecrowInstance)
					&& (this._player.isSafetyZone() || cha.isSafetyZone())) {
				return false;
			}

			if (this.isPcSummonPet(cha)) { // 目標為PC、召喚獸、寵物、假人
				
				// 假人不需要 PvP 判定（已通過上方安全區檢查）
				if (cha instanceof L1DeInstance) {
					return true;
				}

				// 確保攻擊者是玩家才進行 Non-PvP 設定檢查
				if (this._player != null) {
					if (this._player.checkNonPvP(this._player, cha)) { // Non-PvP設定
						return false;
					}
				}
			}
		}
		switch (this._skillId) {
		// 沉睡之霧
		case FOG_OF_SLEEPING:
			if (this._user.getId() == cha.getId()) {
				return false;
			}
			break;
			
		// 集體緩速術
		case MASS_SLOW:
			if (this._user.getId() == cha.getId()) {
				return false;
			}
			
			if (cha instanceof L1SummonInstance) {
				final L1SummonInstance summon = (L1SummonInstance) cha;
				if (this._user.getId() == summon.getMaster().getId()) {
					return false;
				}
				
			} else if (cha instanceof L1PetInstance) {
				final L1PetInstance pet = (L1PetInstance) cha;
				if (this._user.getId() == pet.getMaster().getId()) {
					return false;
				}
			}
			break;

		// 集體傳送術
		case MASS_TELEPORT:
			if (this._user.getId() != cha.getId()) {
				return false;
			}
			break;
		}
		return true;
	}

	/**
	 * 目標對象 是否為寵物 召喚獸 虛擬人物
	 * @param cha
	 * @return
	 */
	private boolean isPcSummonPet(final L1Character cha) {
		// PC 對 PC
		switch (this._calcType) {
		case PC_PC:
			return true;

		// PC 對 NPC
		case PC_NPC:
			// 目標對象為召喚獸
			if (cha instanceof L1SummonInstance) {
				final L1SummonInstance summon = (L1SummonInstance) cha;
				// 目標對象具有主人
				if (summon.isExsistMaster()) {
					return true;
				}
			}
			// 目標對象為寵物
			if (cha instanceof L1PetInstance) {
				return true;
			}
			if (cha instanceof L1DeInstance) {
				return true;
			}
			return false;
			
		default:
			return false;
		}
	}

	// 回傳對目標是否必定失敗
	private boolean isTargetFailure(final L1Character cha) {
		boolean isTU = false;
		boolean isErase = false;
		boolean isManaDrain = false;
		int undeadType = 0;
		if ((cha instanceof L1TowerInstance) || (cha instanceof L1DoorInstance)) { // 守護塔、門對機率系技能無效
			return true;
		}

		if (cha instanceof L1PcInstance) { // 對PC時
			if ((this._calcType == PC_PC) && this._player.checkNonPvP(this._player, cha)) { // Non-PvP設定
				final L1PcInstance pc = (L1PcInstance) cha;
				if ((this._player.getId() == pc.getId())
						|| ((pc.getClanid() != 0) && (this._player.getClanid() == pc
								.getClanid()))) {
					return false;
				}
				return true;
			}
			return false;
		}

		if (cha instanceof L1MonsterInstance) { // 聖光術判定
			isTU = ((L1MonsterInstance) cha).getNpcTemplate().get_IsTU();
		}

		if (cha instanceof L1MonsterInstance) { // 解除魔法判定
			isErase = ((L1MonsterInstance) cha).getNpcTemplate().get_IsErase();
		}

		if (cha instanceof L1MonsterInstance) { // 不死系判定
			undeadType = ((L1MonsterInstance) cha).getNpcTemplate().get_undead();
		}

		// 魔力吸取是否可行？
		if (cha instanceof L1MonsterInstance) {
			isManaDrain = true;
		}
		/*
		 * 成功除外條件1：聖光術成功但目標非不死系。 成功除外條件2：聖光術成功但目標對聖光術無效。
		 * 成功除外條件3：緩速、集體緩速、魔力吸取、藤蔓術、解除魔法、風之枷鎖對目標無效
		 * 成功除外條件4：魔力吸取成功但目標非怪物
		 */
		if (((this._skillId == TURN_UNDEAD) && 
				((undeadType == 0) || (undeadType == 2))) || ((this._skillId == TURN_UNDEAD) && 
						(isTU == false)) || (((this._skillId == ERASE_MAGIC) 
						|| (this._skillId == SLOW)
						|| (this._skillId == MANA_DRAIN) 
						|| (this._skillId == MASS_SLOW)
						|| (this._skillId == ENTANGLE) 
						|| (this._skillId == WIND_SHACKLE)) && (isErase == false))
						|| ((this._skillId == MANA_DRAIN) && (isManaDrain == false))) {
			return true;
		}
		return false;
	}

	private static void bringStone(final L1PcInstance pc, final L1ItemInstance source,
			final int successThreshold, final int chance, final int targetItemId,
			final String targetNameId) {
		PlayerActionLock.run(pc, () -> {
			final L1PcInventory.InventoryMutationSnapshot snapshot =
					pc.getInventory().snapshotForCompensation();
			if (pc.getInventory().removeItem(source, 1) != 1L) {
				return;
			}
			if (successThreshold < chance) {
				pc.sendPackets(new S_ServerMessage(280));
				return;
			}
			if (pc.getInventory().storeItem(targetItemId, 1) == null) {
				pc.getInventory().restoreCompensationSnapshot(snapshot,
						"bring-stone result delivery failed");
				pc.sendPackets(new S_ServerMessage(79));
				return;
			}
			pc.sendPackets(new S_ServerMessage(403, targetNameId));
		});
	}

	// 回傳是否發動反制魔法
	public static String getMonitorLine() {
		return String.format(
				"L1SkillUse check=%d handle=%d autoHandle=%d runSkill=%d sideFlush=%d materialCommit=%d materialFail=%d asyncSubmit=%d asyncActive=%d asyncDone=%d asyncRetry=%d asyncTerminal=%d createZombieAsync=%d skip=%d fail=%d rejected=%d error=%d",
				CHECK_COUNT.get(), HANDLE_COUNT.get(), AUTO_HUNT_HANDLE_COUNT.get(), RUN_SKILL_COUNT.get(),
				SIDE_EFFECT_FLUSH_COUNT.get(), MATERIAL_COMMIT_COUNT.get(),
				MATERIAL_COMMIT_FAILURE_COUNT.get(), ASYNC_SUBMIT_COUNT.get(), ASYNC_ACTIVE_COUNT.get(),
				ASYNC_COMPLETED_COUNT.get(), ASYNC_RETRY_COUNT.get(), ASYNC_TERMINAL_FAILURE_COUNT.get(),
				CREATE_ZOMBIE_ASYNC_COUNT.get(), SKIP_COUNT.get(),
				FAIL_COUNT.get(), REJECTED_COUNT.get(), ERROR_COUNT.get());
	}

	private boolean isUseCounterMagic(final L1Character cha) {
		// 反制魔法有效的技能且處於反制魔法狀態
		if (this._isCounterMagic && cha.hasSkillEffect(COUNTER_MAGIC)) {
			cha.removeSkillEffect(COUNTER_MAGIC);
			final int castgfx = SkillsTable.get().getTemplate(COUNTER_MAGIC).getCastGfx();
			cha.broadcastPacketX10(new S_SkillSound(cha.getId(), castgfx));
			if (cha instanceof L1PcInstance) {
				final L1PcInstance pc = (L1PcInstance) cha;
				pc.sendPackets(new S_SkillSound(pc.getId(), castgfx));
			}
			return true;
		}
		return false;
	}
}	
