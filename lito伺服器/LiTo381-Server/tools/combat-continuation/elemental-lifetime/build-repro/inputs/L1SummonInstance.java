package com.lineage.server.model.Instance;

import static com.lineage.server.model.skill.L1SkillId.FOG_OF_SLEEPING;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lineage.config.ConfigAlt;
import com.lineage.server.ActionCodes;
import com.lineage.server.IdFactoryNpc;
import com.lineage.server.datatables.NpcTable;
import com.lineage.server.model.L1AttackMode;
import com.lineage.server.model.L1AttackPc;
import com.lineage.server.model.L1Character;
import com.lineage.server.model.L1Inventory;
import com.lineage.server.model.L1Location;
import com.lineage.server.model.CompanionCommandGuard;
import com.lineage.server.model.PlayerActionLock;
import com.lineage.server.model.drop.SetDrop;
import com.lineage.server.model.drop.SetDropExecutor;
import com.lineage.server.serverpackets.S_DoActionGFX;
import com.lineage.server.serverpackets.S_HPMeter;
import com.lineage.server.serverpackets.S_NPCPack_Summon;
import com.lineage.server.serverpackets.S_PetMenuPacket;
import com.lineage.server.serverpackets.S_ServerMessage;
import com.lineage.server.serverpackets.S_SkillSound;
import com.lineage.server.templates.L1Npc;
import com.lineage.server.utils.CheckUtil;
import com.lineage.server.world.World;

/**
 * 召喚獸控制項
 * @author daien
 *
 */
public class L1SummonInstance extends L1NpcInstance {

	private static final long serialVersionUID = 1L;

	private static final Logger _log = LogManager.getLogger(L1SummonInstance.class);
	private final AtomicLong _deathGeneration = new AtomicLong();
	private final LifecycleGate _lifecycle = new LifecycleGate();
	private static final AtomicLong RESURRECTION_REJECTED_COUNT = new AtomicLong();
	private static final ThreadLocal<ResurrectionPermit> RESURRECTION_PERMIT =
			new ThreadLocal<>();

	enum LifecyclePhase {
		ACTIVE,
		DEAD_PENDING,
		RESURRECTING,
		FINALIZING,
		DEAD_SETTLED,
		CLEANUP_FAILED,
		TERMINATED
	}

	enum CleanupKind {
		DEATH,
		RETURN_TO_NATURE,
		DELETE,
		LIBERATE
	}

	record LifecycleState(LifecyclePhase phase, long generation,
			CleanupKind cleanupKind) {
	}

	record LifecycleClaim(LifecycleState state) {
		CleanupKind kind() {
			return state.cleanupKind();
		}

		long generation() {
			return state.generation();
		}
	}

	record ResurrectionClaim(LifecycleState state) {
		long generation() {
			return state.generation();
		}
	}

	private record ResurrectionPermit(L1SummonInstance owner, long generation) {
	}

	/**
	 * One atomic owner for every terminal path.  Production transitions also run
	 * inside the target vital lock so HP/dead and lifecycle publication cannot be
	 * torn; CAS remains the final exactly-one guard and is independently testable.
	 */
	static final class LifecycleGate {
		private final AtomicReference<LifecycleState> _state =
				new AtomicReference<>(new LifecycleState(
						LifecyclePhase.ACTIVE, 0L, null));

		LifecycleState snapshot() {
			return _state.get();
		}

		boolean isActive() {
			return snapshot().phase() == LifecyclePhase.ACTIVE;
		}

		boolean isCurrentDeath(final long generation) {
			final LifecycleState current = snapshot();
			return current.phase() == LifecyclePhase.DEAD_PENDING
					&& current.generation() == generation;
		}

		boolean markDeadPending(final long generation) {
			final LifecycleState current = snapshot();
			return current.phase() == LifecyclePhase.ACTIVE
					&& _state.compareAndSet(current, new LifecycleState(
							LifecyclePhase.DEAD_PENDING, generation, null));
		}

		boolean markDeadSettled(final long generation) {
			final LifecycleState current = snapshot();
			return current.phase() == LifecyclePhase.ACTIVE
					&& _state.compareAndSet(current, new LifecycleState(
							LifecyclePhase.DEAD_SETTLED, generation, null));
		}

		LifecycleClaim tryClaimDeath(final long generation) {
			final LifecycleState current = snapshot();
			if (current.phase() != LifecyclePhase.DEAD_PENDING
					|| current.generation() != generation) {
				return null;
			}
			final LifecycleState claimed = new LifecycleState(
					LifecyclePhase.FINALIZING, generation, CleanupKind.DEATH);
			return _state.compareAndSet(current, claimed)
					? new LifecycleClaim(claimed) : null;
		}

		LifecycleClaim tryClaimCleanup(final CleanupKind kind, final long generation) {
			if (kind == null || kind == CleanupKind.DEATH) {
				throw new IllegalArgumentException("non-death cleanup kind required");
			}
			final LifecycleState current = snapshot();
			if (current.phase() != LifecyclePhase.ACTIVE
					&& current.phase() != LifecyclePhase.DEAD_PENDING
					&& current.phase() != LifecyclePhase.DEAD_SETTLED
					&& current.phase() != LifecyclePhase.CLEANUP_FAILED) {
				return null;
			}
			final LifecycleState claimed = new LifecycleState(
					LifecyclePhase.FINALIZING, generation, kind);
			return _state.compareAndSet(current, claimed)
					? new LifecycleClaim(claimed) : null;
		}

		ResurrectionClaim tryReserveResurrection(final long generation) {
			final LifecycleState current = snapshot();
			if (current.phase() != LifecyclePhase.DEAD_PENDING
					&& current.phase() != LifecyclePhase.DEAD_SETTLED) {
				return null;
			}
			final LifecycleState reserved = new LifecycleState(
					LifecyclePhase.RESURRECTING, generation, null);
			return _state.compareAndSet(current, reserved)
					? new ResurrectionClaim(reserved) : null;
		}

		boolean completeResurrection(final ResurrectionClaim claim) {
			return claim != null && _state.compareAndSet(claim.state(),
					new LifecycleState(LifecyclePhase.ACTIVE,
							claim.generation(), null));
		}

		boolean abortResurrection(final ResurrectionClaim claim) {
			return claim != null && _state.compareAndSet(claim.state(),
					new LifecycleState(LifecyclePhase.DEAD_SETTLED,
							claim.generation(), null));
		}

		boolean reviveDirect(final LifecycleState expected, final long generation) {
			if (expected == null || (expected.phase() != LifecyclePhase.DEAD_PENDING
					&& expected.phase() != LifecyclePhase.DEAD_SETTLED)) {
				return false;
			}
			return _state.compareAndSet(expected, new LifecycleState(
					LifecyclePhase.ACTIVE, generation, null));
		}

		boolean completeCleanup(final LifecycleClaim claim, final boolean retainedCorpse,
				final boolean externalCleanupComplete) {
			if (claim == null) {
				return false;
			}
			final LifecyclePhase phase = !externalCleanupComplete
					? LifecyclePhase.CLEANUP_FAILED
					: retainedCorpse ? LifecyclePhase.DEAD_SETTLED
							: LifecyclePhase.TERMINATED;
			return _state.compareAndSet(claim.state(), new LifecycleState(
					phase, claim.generation(), null));
		}
	}

	private record DamageTransition(int currentHp, boolean hpChanged,
			long deathGeneration, boolean invalidZeroHp) {
		private static final DamageTransition IGNORED =
				new DamageTransition(0, false, 0L, false);
	}

	private static final int _summonTime = 3600;
	
	private int _currentPetStatus;
	
	/**
	 * 假人攜帶寵／召喚物行為模式（來源：{@code 假人_設置狀態.De_pet} 每組第一個數字）。<br>
	 * 實體仍為 {@link L1SummonInstance}，僅死亡／跟隨行為不同，與真實 {@code L1PetInstance} 項圈寵物並非同類別。<br>
	 * <ul>
	 * <li><b>Type 0</b>：一般假人跟班，死亡走一般召喚分支（通常會 {@code deleteMe()}）。</li>
	 * <li><b>Type 1</b>：擬真「項圈寵物」體感 — 死亡後為屍體狀態留在世界；仍留在主人 {@code getPetList()}。
	 * 假人瞬移時與玩家 {@code Teleportation} 一致，<b>含已死亡</b>仍會 {@code teleport} 到新座標附近（見 {@link L1DeInstance#teleport}）。</li>
	 * <li><b>Type 2</b>：擬真「召喚物」體感 — 死亡時立即回收（{@code deleteMe()}），不保留屍體。</li>
	 * </ul>
	 */
	private int _dePetType = 0;
	
	private int _checkMove= 0;
	
	private boolean _tamed;
	
	// 初始化參數（用於延遲資料庫操作）
	private int _initNpcId = -1;
	private boolean _initIsCreateZombie = false;
	private boolean _needsInit = false;
	private boolean _initialized = false;
	
	private static Random _random = new Random();

	public boolean tamed() {
		return this._tamed;
	}
	
	// 無目標時的處理
	//@Override
	public boolean noTarget() {
		switch (this._currentPetStatus) {
		case 3:// 休息
			return true;
			
		case 4:// 散開
			if ((_master != null)
					&& (_master.getMapId() == getMapId())
					&& (getLocation().getTileLineDistance(_master.getLocation()) < 5)) {
				if (_npcMove != null) {
					int dir = _npcMove.targetReverseDirection(_master.getX(), _master.getY());
					dir = _npcMove.checkObject(dir);
					
					_npcMove.setDirectionMove(dir);
					setSleepTime(calcSleepTime(getPassispeed(), MOVE_SPEED));
				}
				
			} else {
				_currentPetStatus = 3;
				return true;
			}
			break;
			
		case 5:// 警戒
			if ((Math.abs(getHomeX() - getX()) > 1)
					|| (Math.abs(getHomeY() - getY()) > 1)) {
				if (_npcMove != null) {
					final int dir = _npcMove.moveDirection(getHomeX(), getHomeY());
					if (dir == -1) {
						// 若離出生點太遠則以目前位置為出生點
						setHomeX(getX());
						setHomeY(getY());
						
					} else {
						_npcMove.setDirectionMove(dir);
						setSleepTime(calcSleepTime(getPassispeed(), MOVE_SPEED));
					}
				}
			}
			break;
			
		default:
			// ======================================================
			// [Fix #4] 距離同步：假人寵物離主人太遠時強制瞬移（>12）
			// - 解決寵物跟不上、卡牆後被丟包問題
			// - 若正在戰鬥仍離主人過遠，直接清目標避免幽靈攻擊
			// ======================================================
			if (_master != null && (_master instanceof L1DeInstance)) {
				int distToMaster = getLocation().getTileLineDistance(_master.getLocation());
				if (_master.getMapId() != getMapId() || distToMaster > 12) {
					// 清除戰鬥目標，避免瞬移後仍想打舊位置目標
					allTargetClear();
					
					// 計算落點（最多 10 次找可通行點）
					int newX = _master.getX();
					int newY = _master.getY();
					for (int tryCount = 0; tryCount < 10; tryCount++) {
						int rndX = _master.getX() + _random.nextInt(5) - 2;
						int rndY = _master.getY() + _random.nextInt(5) - 2;
						
						L1Location checkLoc = new L1Location(rndX, rndY, _master.getMapId());
						
						// 第二個參數傳 null (只檢查地形，不檢查是否被生物阻擋)
						if (_master.getMap().isPassable(checkLoc, null)) {
							newX = rndX;
							newY = rndY;
							break;
						}
						// [修正結束]
					}
					
					teleport(newX, newY, _master.getHeading());
					_checkMove = 0;
					return true;
				}
			}
			
			// [修正] 攻擊模式且有目標時，禁止執行跟隨邏輯
			// 避免「戰鬥中仍執行跟隨移動」導致的同步錯誤（幽靈攻擊）
			if (this._currentPetStatus == 1 && this.is_now_target() != null) {
				return false;
			}
			
			// [新增] 強制傳送機制 (Hard Pull)
			// 條件：主人存在 且 (地圖不同 或 距離超過 30 格)
			// 作用：直接飛到主人身邊，無視地形與障礙
			if (_master != null) {
				if (_master.getMapId() != getMapId() || 
					getLocation().getTileLineDistance(_master.getLocation()) > 30) {
					
					// 1. 計算落點 (隨機在主人周圍 1 格)
					int newX = _master.getX() + _random.nextInt(3) - 1;
					int newY = _master.getY() + _random.nextInt(3) - 1;
					
					// 2. 執行傳送 (使用 L1NpcInstance 原生 teleport，會自動處理封包與視野)
					teleport(newX, newY, _master.getHeading());
					
					// 3. 清除跟隨計數器，避免連續觸發錯誤
					_checkMove = 0;
					return true;
				}
			}
			
			if ((_master != null) && (_master.getMapId() == getMapId())) {
				// 主人跟隨
				final int location = getLocation().getTileLineDistance(_master.getLocation());
				// debug: 主人跟隨距離
				if (location > 2) {
					if (_npcMove != null) {
						final int dir = _npcMove.moveDirection(_master.getX(), _master.getY());
						// debug: 控制者遺失次數
						if (dir == -1) {
							_checkMove++;
							if (_checkMove >= 10) {
								// 控制者遺失
								// debug: 控制者遺失次數
								_checkMove = 0;
								_currentPetStatus = 3;
								return true;
							}
							
						} else {
							_checkMove = 0;
							_npcMove.setDirectionMove(dir);
							setSleepTime(calcSleepTime(getPassispeed(), MOVE_SPEED));
						}
					}
				}

			} else {
				// 控制者遺失
				// debug: 控制者遺失else
				_currentPetStatus = 3;
				return true;
			}
			break;
		}
		return false;
	}

	/**
	 * 召喚獸
	 * @param template
	 * @param master
	 */
	public L1SummonInstance(final L1Npc template, final L1Character master) {
		super(template);
		this.setId(IdFactoryNpc.get().nextId());
		// 副本編號
		this.set_showId(master.get_showId());

		this.setMaster(master);

		this.setMap(master.getMapId());
		CheckUtil.applySummonBirthLandingNearMaster(master, this);
		this.setHeading(5);
		this.setLightSize(template.getLightSize());

		// 初始狀態設置：玩家主人為攻擊模式，NPC主人也為攻擊模式
		_currentPetStatus = 1; // 攻擊模式
		_tamed = false;

		// // 假人寵物：時間限制設為 0 (無限)
		if (master instanceof L1DeInstance) {
			this.set_time(0);
			// [Fix #3] 假人寵物不允許死亡後再召喚(避免像野怪重生)
			this.setreSpawn(false);
			
		} else {
			// 玩家召喚獸：1800 秒
			this.set_time(1800);
		}

		// --- [修改開始] 先建立寵物關係，再讓物件出生，這樣封包才會帶有主人名 ---
		master.addPet(this);
		if (master instanceof L1PcInstance pcMaster) {
			// 增加物件組人
			addMaster(pcMaster);
		}
		// --- [修改結束] ---

		World.get().storeObject(this);
		if (!World.get().addVisibleObject(this)) {
			master.removePet(this);
			throw new IllegalStateException("Summon visible registration failed: objectId=" + getId());
		}
		this.setMoveSpeed(1); // 設定移動速度狀態為加速 (1)
		
		// =======================================================
		for (final L1PcInstance pc : World.get().getRecognizePlayer(this)) {
			this.onPerceive(pc);
		}
		
		if (master instanceof L1PcInstance) {
			// 動畫還是要最後播
			this.broadcastPacketAll(new S_SkillSound(getId(), 755));
		} else if (master instanceof L1NpcInstance) {
			// NPC主人也使用攻擊模式（已在上面設置為1）
		}
	}

	/**
	 * 造屍術
	 * @param target
	 * @param master
	 * @param isCreateZombie
	 */
	public L1SummonInstance(final L1NpcInstance target, final L1Character master, final boolean isCreateZombie) {
		super(null);
		this.setId(IdFactoryNpc.get().nextId());
		// 副本編號
		this.set_showId(master.get_showId());

		if (isCreateZombie) { // 造屍術
			int npcId = 45065;
			final L1PcInstance pc = (L1PcInstance) master;
			final int level = pc.getLevel();
			if (pc.isWizard()) {
				if ((level >= 24) && (level <= 31)) {
					npcId = 81183;
				} else if ((level >= 32) && (level <= 39)) {
					npcId = 81184;
				} else if ((level >= 40) && (level <= 43)) {
					npcId = 81185;
				} else if ((level >= 44) && (level <= 47)) {
					npcId = 81186;
				} else if ((level >= 48) && (level <= 51)) {
					npcId = 81187;
				} else if (level >= 52) {
					npcId = 81188;
				}
			} else if (pc.isElf()) {
				if (level >= 48) {
					npcId = 81183;
				}
			}
			// 將 NpcTable.get().getTemplate(npcId) 移至 initialize() 方法，避免阻塞建構子
			// 儲存初始化參數
			this._initNpcId = npcId;
			this._initIsCreateZombie = true;
			this._needsInit = true;
			// 暫時使用目標的模板作為預設值，initialize() 會從資料庫讀取正確的模板並重新設置
			if (target != null) {
				this.setting_template(target.getNpcTemplate());
			}

		} else { // 馴服怪物
			this.setting_template(target.getNpcTemplate());
			this.setCurrentHpDirect(target.getCurrentHp());
			this.setCurrentMpDirect(target.getCurrentMp());
		}
		
		this.set_time(_summonTime);

		this.setMaster(master);
		this.setX(target.getX());
		this.setY(target.getY());
		this.setMap(target.getMapId());
		this.setHeading(target.getHeading());
		this.setLightSize(target.getLightSize());
		this.setPetcost(6);

		if (target instanceof L1MonsterInstance mob && !mob.is_storeDroped()) {
			// XXX
			final SetDropExecutor setDropExecutor = new SetDrop();
			setDropExecutor.setDrop(target, target.getInventory());
		}
		
		this.setInventory(target.getInventory());
		target.setInventory(null);

		this._currentPetStatus = 3;
		this._tamed = true;

		// 若寵物正在攻擊則停止
		for (final L1NpcInstance each : master.getPetList().values()) {
			each.targetRemove(target);
		}

		target.deleteMe();
		World.get().storeObject(this);
		if (!World.get().addVisibleObject(this)) {
			throw new IllegalStateException("Tamed summon visible registration failed: objectId=" + getId());
		}
		for (final L1PcInstance pc : World.get().getRecognizePlayer(this)) {
			this.onPerceive(pc);
		}
		master.addPet(this);
		if (master instanceof L1PcInstance pcMaster) {
			// 增加物件組人
			addMaster(pcMaster);
		}
	}

	/**
	 * 初始化方法：處理資料庫操作
	 * 此方法應在 GeneralThreadPool.execute() 中調用，避免阻塞建構子
	 */
	public void initialize() {
		if (this._initialized) {
			return; // 已經初始化過
		}
		
		try {
			if (this._needsInit && this._initIsCreateZombie && this._initNpcId > 0) {
				// 造屍術：從資料庫讀取 NPC 模板並設置
				final L1Npc template = NpcTable.get().getTemplate(this._initNpcId).clone();
				this.setting_template(template);
				
				this._initNpcId = -1; // 清理
				this._initIsCreateZombie = false;
				this._needsInit = false;
			}
			
			this._initialized = true;
			
		} catch (final Exception e) {
			_log.error("L1SummonInstance.initialize() 發生異常", e);
		}
	}

	//@Override
	public void receiveDamage(final L1Character attacker, final int damage) { // 攻擊減少HP時在此處理
		ISASCAPE = false;
		if (!isLifecycleActive()) {
			return;
		}
		if (this.getCurrentHp() > 0 && !this.isDead()) {
			if (damage > 0) {
				this.setHate(attacker, 0); // 召喚獸無仇恨
				this.removeSkillEffect(FOG_OF_SLEEPING);
				// [修正] 無論主人是否存在，被攻擊時都應該進入攻擊模式並設定目標
				// 避免「主人存在時被攻擊，但沒有設定目標」導致的同步錯誤
				if (attacker != getMaster()) {
					if (this._currentPetStatus != 1) {
						this._currentPetStatus = 1; // 切換為攻擊模式
					}
					this.setTarget(attacker); // 設定攻擊目標
				}
			}

			if (attacker instanceof L1PcInstance player && damage > 0
					&& attacker != getMaster()) {
				player.setPetTarget(this);
			}
		}

		final DamageTransition transition = commitDamageState(damage);
		if (transition.invalidZeroHp()) {
			_log.error("NPC hp減少處理失敗 可能原因: 初始hp為0(" + this.getNpcId() + ")");
		}
		if (transition.deathGeneration() != 0L) {
			submitClaimedDeath(attacker, transition.deathGeneration());
		} else if (transition.hpChanged()) {
			sendSummonHpMeter(transition.currentHp());
		}
	}

	/**
	 * 死亡
	 * @param lastAttacker
	 */
	public void Death(final L1Character lastAttacker) {
		final long generation = claimDeathState();
		if (generation != 0L) {
			submitClaimedDeath(lastAttacker, generation);
		}
	}

	private DamageTransition commitDamageState(final int damage) {
		return withVitalMutation(() -> {
			if (!_lifecycle.isActive() || isDead()) {
				return DamageTransition.IGNORED;
			}
			final int oldHp = getCurrentHp();
			final long candidate = (long) oldHp - (long) damage;
			if (oldHp <= 0 || candidate <= 0L) {
				final long generation = _deathGeneration.incrementAndGet();
				if (!_lifecycle.markDeadPending(generation)) {
					return DamageTransition.IGNORED;
				}
				setCurrentHpDirect(0);
				super.setDead(true);
				setStatus(ActionCodes.ACTION_Die);
				return new DamageTransition(0, oldHp != 0, generation, oldHp <= 0);
			}
			final int newHp = (int) Math.min((long) getMaxHp(), candidate);
			if (newHp <= 0) {
				final long generation = _deathGeneration.incrementAndGet();
				if (!_lifecycle.markDeadPending(generation)) {
					return DamageTransition.IGNORED;
				}
				setCurrentHpDirect(0);
				super.setDead(true);
				setStatus(ActionCodes.ACTION_Die);
				return new DamageTransition(0, oldHp != 0, generation, false);
			}
			if (newHp == oldHp) {
				return DamageTransition.IGNORED;
			}
			setCurrentHpDirect(newHp);
			return new DamageTransition(newHp, true, 0L, false);
		});
	}

	private long claimDeathState() {
		return withVitalMutation(() -> {
			if (!_lifecycle.isActive() || isDead()) {
				return 0L;
			}
			final long generation = _deathGeneration.incrementAndGet();
			if (!_lifecycle.markDeadPending(generation)) {
				return 0L;
			}
			setCurrentHpDirect(0);
			super.setDead(true);
			setStatus(ActionCodes.ACTION_Die);
			return generation;
		});
	}

	private boolean isCurrentDeathGeneration(final long generation) {
		return generation != 0L && _lifecycle.isCurrentDeath(generation)
				&& _deathGeneration.get() == generation
				&& isDead() && !destroyed();
	}

	private boolean isLifecycleActive() {
		final LifecycleGate lifecycle = _lifecycle;
		return lifecycle == null || lifecycle.isActive();
	}

	@Override
	public void setDead(final boolean flag) {
		// L1NpcInstance's constructor invokes this virtual method before subclass
		// fields have been initialized.
		final LifecycleGate lifecycle = _lifecycle;
		if (lifecycle == null) {
			super.setDead(flag);
			return;
		}
		withVitalMutation(() -> {
			final LifecycleState current = lifecycle.snapshot();
			if (flag) {
				if (current.phase() == LifecyclePhase.RESURRECTING
						|| current.phase() == LifecyclePhase.FINALIZING
						|| current.phase() == LifecyclePhase.TERMINATED) {
					return;
				}
				if (current.phase() == LifecyclePhase.ACTIVE) {
					final long generation = _deathGeneration.incrementAndGet();
					if (!lifecycle.markDeadSettled(generation)) {
						return;
					}
				}
				super.setDead(true);
				return;
			}

			if (current.phase() == LifecyclePhase.FINALIZING
					|| current.phase() == LifecyclePhase.CLEANUP_FAILED
					|| current.phase() == LifecyclePhase.TERMINATED) {
				RESURRECTION_REJECTED_COUNT.incrementAndGet();
				return;
			}
			if (current.phase() == LifecyclePhase.RESURRECTING) {
				final ResurrectionPermit permit = RESURRECTION_PERMIT.get();
				if (permit == null || permit.owner() != this
						|| permit.generation() != current.generation()) {
					RESURRECTION_REJECTED_COUNT.incrementAndGet();
					return;
				}
				super.setDead(false);
				// Keep RESURRECTING until L1Character.resurrect finishes all unlocked
				// packet/world side effects. Cleanup claims remain closed meanwhile.
				return;
			}
			if (current.phase() == LifecyclePhase.DEAD_PENDING
					|| current.phase() == LifecyclePhase.DEAD_SETTLED) {
				final long generation = _deathGeneration.incrementAndGet();
				super.setDead(false);
				if (!lifecycle.reviveDirect(current, generation)) {
					super.setDead(true);
				}
				return;
			}
			super.setDead(false);
		});
	}

	@Override
	public void resurrect(final int hp) {
		final LifecycleGate lifecycle = _lifecycle;
		if (lifecycle == null) {
			super.resurrect(hp);
			return;
		}
		final ResurrectionClaim claim = withVitalMutation(() -> {
			if (!isDead() || destroyed()) {
				return null;
			}
			final LifecycleState current = lifecycle.snapshot();
			if (current.phase() != LifecyclePhase.DEAD_PENDING
					&& current.phase() != LifecyclePhase.DEAD_SETTLED) {
				return null;
			}
			final long generation = _deathGeneration.incrementAndGet();
			return lifecycle.tryReserveResurrection(generation);
		});
		if (claim == null) {
			if (isDead()) {
				RESURRECTION_REJECTED_COUNT.incrementAndGet();
			}
			return;
		}

		final ResurrectionPermit previousPermit = RESURRECTION_PERMIT.get();
		RESURRECTION_PERMIT.set(new ResurrectionPermit(this, claim.generation()));
		try {
			super.resurrect(hp);
		} finally {
			if (previousPermit == null) {
				RESURRECTION_PERMIT.remove();
			} else {
				RESURRECTION_PERMIT.set(previousPermit);
			}
			withVitalMutation(() -> {
				if (!lifecycle.snapshot().equals(claim.state())) {
					return;
				}
				if (!isDead() && getCurrentHp() > 0) {
					lifecycle.completeResurrection(claim);
				} else {
					if (!isDead()) {
						super.setDead(true);
					}
					lifecycle.abortResurrection(claim);
				}
			});
		}
	}

	private LifecycleClaim claimDeathFinalizer(final long generation) {
		return withVitalMutation(() -> {
			if (!isCurrentDeathGeneration(generation)) {
				return null;
			}
			return _lifecycle.tryClaimDeath(generation);
		});
	}

	private LifecycleClaim claimCleanup(final CleanupKind kind) {
		return withVitalMutation(() -> {
			if (destroyed()) {
				return null;
			}
			final LifecycleState current = _lifecycle.snapshot();
			if (current.phase() != LifecyclePhase.ACTIVE
					&& current.phase() != LifecyclePhase.DEAD_PENDING
					&& current.phase() != LifecyclePhase.DEAD_SETTLED
					&& current.phase() != LifecyclePhase.CLEANUP_FAILED) {
				return null;
			}
			final long generation = _deathGeneration.incrementAndGet();
			return _lifecycle.tryClaimCleanup(kind, generation);
		});
	}

	private void completeCleanup(final LifecycleClaim claim,
			final boolean retainedCorpse, final boolean externalCleanupComplete) {
		final boolean completed = withVitalMutation(
				() -> _lifecycle.completeCleanup(
						claim, retainedCorpse, externalCleanupComplete));
		if (!completed) {
			_log.error("Summon lifecycle completion lost ownership: name={} kind={} generation={}",
					getName(), claim != null ? claim.kind() : null,
					claim != null ? claim.generation() : 0L);
		}
	}

	private void submitClaimedDeath(final L1Character lastAttacker, final long generation) {
		NpcDeathHandoff.submit(new NpcDeathHandoff.DeathWork() {
			@Override
			public Object owner() {
				return L1SummonInstance.this;
			}

			@Override
			public long generation() {
				return generation;
			}

			@Override
			public boolean isCurrent() {
				return isCurrentDeathGeneration(generation);
			}

			@Override
			public void runFull() {
				finishClaimedDeath(generation);
			}

			@Override
			public void runMinimal(final String reason, final Throwable fullFailure) {
				applyMinimalDeathTerminal(generation, reason, fullFailure);
			}
		});
	}

	/** Full inventory/world cleanup. NpcDeathHandoff guarantees SlowPool ownership. */
	private void finishClaimedDeath(final long generation) {
		runWithMasterActionAndInstanceLock(() -> finishClaimedDeathLocked(generation));
	}

	private void finishClaimedDeathLocked(final long generation) {
		final LifecycleClaim claim = claimDeathFinalizer(generation);
		if (claim == null) {
			return;
		}
		final L1Character master = _master;
		final boolean tamed = _tamed;
		final int dePetType = _dePetType;
		final boolean retainedCorpse = !tamed && dePetType == 1;
		try {
			stopAiForCleanup();
			if (!transferInventoryToMasterOrGround(master)) {
				_log.error("Summon terminal cleanup retained non-empty inventory: name={} items={}",
						getName(), getInventory() != null ? getInventory().getSize() : -1);
				return;
			}
			if (!tamed && dePetType == 2) {
				removeSelfFromNearbyNpcHate(master);
				deleteWorldObjectClaimed(master, false);
				return;
			}

			if (dePetType == 1) {
				broadcastDeathSafely();
			}
			if (tamed) {
				broadcastDeathSafely();
				removeSelfFromNearbyNpcHate(master);
				startDeleteTimerSafely(ConfigAlt.NPC_DELETION_TIME * 2);
				deleteWorldObjectClaimed(master, false);
			} else if (retainedCorpse) {
				// Type-1 fake-player companion stays as a settled corpse and remains
				// in the master's list. Resurrection is allowed only after this owner
				// releases FINALIZING in the finally block below.
				removeSelfFromNearbyNpcHate(master);
			} else {
				removeSelfFromNearbyNpcHate(master);
				deleteWorldObjectClaimed(master, false);
			}
		} finally {
			final boolean deleted = destroyed();
			completeCleanup(claim, retainedCorpse && !deleted,
					retainedCorpse || deleted);
		}
	}

	/** Cheap no-JDBC terminal used only when the bounded SlowPool handoff fails. */
	private void applyMinimalDeathTerminal(final long generation, final String reason,
			final Throwable fullFailure) {
		if (!isCurrentDeathGeneration(generation)) {
			return;
		}
		// This callback can run on the rejecting damage/shutdown caller. HP/dead/
		// status are already terminal; do not broadcast, scan world hate, trade
		// inventory, or delete inline. A short timer owns eventual cleanup.
		if (_dePetType != 1) {
			try {
				setreSpawn(false);
				startDeleteTimer(1);
			} catch (final RuntimeException e) {
				_log.error("Summon minimal death timer failed: summon={}", getName(), e);
			}
		}
		if (fullFailure != null) {
			_log.error("Summon full death failed; minimal terminal applied: summon={} reason={}",
					getName(), reason, fullFailure);
		} else {
			_log.warn("Summon death pressure fallback applied: summon={} reason={}", getName(), reason);
		}
	}

	private void sendSummonHpMeter(final int currentHp) {
		if (_master instanceof L1PcInstance master && getMaxHp() > 0) {
			master.sendPackets(new S_HPMeter(getId(), 100 * currentHp / getMaxHp()));
		}
	}

	public void returnToNature() {
		runWithMasterActionAndInstanceLock(this::returnToNatureLocked);
	}

	private void returnToNatureLocked() {
		final LifecycleClaim claim = claimCleanup(CleanupKind.RETURN_TO_NATURE);
		if (claim == null) {
			return;
		}
		final L1Character master = _master;
		try {
			stopAiForCleanup();
			if (_tamed) {
				liberateClaimed(master);
			} else {
				if (!transferInventoryToMasterOrGround(master)) {
					_log.error("Summon return-to-nature retained non-empty inventory: name={} items={}",
							getName(), getInventory() != null ? getInventory().getSize() : -1);
					return;
				}
				deleteWorldObjectClaimed(master, true);
			}
		} finally {
			completeCleanup(claim, false, destroyed());
		}
	}

	// 物件刪除處理
	//@Override
	public void deleteMe() {
		synchronized (this) {
			deleteMeLocked();
		}
	}

	private void deleteMeLocked() {
		final LifecycleClaim claim = claimCleanup(CleanupKind.DELETE);
		if (claim == null) {
			return;
		}
		final L1Character master = _master;
		try {
			stopAiForCleanup();
			// deleteMe() 會被 NPC worker 與定時器呼叫，此路徑禁止進入玩家 DB。
			// 使用純記憶體的地面作為保底；登出等 SlowPool 路徑改用
			// deleteAfterInventoryReturn() 優先歸還主人。
			if (!transferInventoryToGround()) {
				_log.error("Summon deletion blocked because inventory was not drained: name={} items={}",
						getName(), getInventory() != null ? getInventory().getSize() : -1);
				return;
			}
			deleteWorldObjectClaimed(master, false);
		} finally {
			completeCleanup(claim, false, destroyed());
		}
	}

	/** SlowPool / logout callers: return items to the owner first, then delete. */
	public void deleteAfterInventoryReturn() {
		runWithMasterActionAndInstanceLock(this::deleteAfterInventoryReturnLocked);
	}

	private void deleteAfterInventoryReturnLocked() {
		final LifecycleClaim claim = claimCleanup(CleanupKind.DELETE);
		if (claim == null) {
			return;
		}
		final L1Character master = _master;
		try {
			stopAiForCleanup();
			if (!transferInventoryToMasterOrGround(master)) {
				_log.error("Summon deletion blocked because owner/ground return failed: name={} items={}",
						getName(), getInventory() != null ? getInventory().getSize() : -1);
				return;
			}
			deleteWorldObjectClaimed(master, false);
		} finally {
			completeCleanup(claim, false, destroyed());
		}
	}

	// 馴服怪物、造屍術時的解放處理
	public void liberate() {
		synchronized (this) {
			liberateLocked();
		}
	}

	private void liberateLocked() {
		final LifecycleClaim claim = claimCleanup(CleanupKind.LIBERATE);
		if (claim == null) {
			return;
		}
		final L1Character master = _master;
		try {
			stopAiForCleanup();
			liberateClaimed(master);
		} finally {
			completeCleanup(claim, false, destroyed());
		}
	}

	private void runWithMasterActionAndInstanceLock(final Runnable action) {
		final L1Character master = _master;
		if (master instanceof L1PcInstance pcMaster) {
			PlayerActionLock.run(pcMaster, () -> {
				synchronized (this) {
					action.run();
				}
			});
			return;
		}
		synchronized (this) {
			action.run();
		}
	}

	private boolean transferInventoryToMasterOrGround(final L1Character master) {
		final L1Inventory source = getInventory();
		if (source == null) {
			return true;
		}
		final L1Inventory masterInventory = master != null ? master.getInventory() : null;
		final List<L1ItemInstance> items;
		try {
			items = new ArrayList<>(source.getItems());
		} catch (final RuntimeException e) {
			_log.error("Summon inventory snapshot failed: name={}", getName(), e);
			return false;
		}
		boolean allTransferred = true;
		for (final L1ItemInstance item : items) {
			if (item == null) {
				continue;
			}
			final long transferCount = item.getCount();
			try {
				L1ItemInstance received = null;
				if (masterInventory != null
						&& masterInventory.checkAddItem(item, transferCount) == L1Inventory.OK) {
					received = source.tradeItem(
							item, transferCount, masterInventory);
					if (received != null && master instanceof L1PcInstance pcMaster) {
						pcMaster.sendPackets(new S_ServerMessage(
								143, getName(), L1ItemStatus.getLogName(received, transferCount)));
					}
				}
				if (received == null) {
					if (masterInventory != null) {
						_log.warn("Summon return-to-master rejected; falling back to ground: name={} item={}",
								getName(), item.getLogName());
					}
					received = transferSummonItemToGround(source, item);
					if (received == null) {
						allTransferred = false;
						_log.error("Summon return-to-ground rejected: name={} item={}",
								getName(), item.getLogName());
					}
				}
			} catch (final RuntimeException e) {
				allTransferred = false;
				_log.error("Summon inventory transfer failed: name={} item={}",
						getName(), item.getLogName(), e);
			}
		}
		return allTransferred && source.getSize() == 0;
	}

	private boolean transferInventoryToGround() {
		final L1Inventory source = getInventory();
		if (source == null) {
			return true;
		}
		final List<L1ItemInstance> items;
		try {
			items = new ArrayList<>(source.getItems());
		} catch (final RuntimeException failure) {
			_log.error("Summon ground-return snapshot failed: name={}", getName(), failure);
			return false;
		}
		boolean allTransferred = true;
		for (final L1ItemInstance item : items) {
			if (item == null) {
				continue;
			}
			try {
				if (transferSummonItemToGround(source, item) == null) {
					allTransferred = false;
					_log.error("Summon delete-to-ground rejected: name={} item={}",
							getName(), item.getLogName());
				}
			} catch (final RuntimeException failure) {
				allTransferred = false;
				_log.error("Summon delete-to-ground failed: name={} item={}",
						getName(), item.getLogName(), failure);
			}
		}
		return allTransferred && source.getSize() == 0;
	}

	private L1ItemInstance transferSummonItemToGround(final L1Inventory source,
			final L1ItemInstance item) {
		final int originalShowId = item.get_showId();
		item.set_showId(get_showId());
		L1ItemInstance received = null;
		try {
			final L1Inventory ground = World.get().getInventory(
					getX(), getY(), getMapId());
			received = source.tradeItem(item, item.getCount(), ground);
			return received;
		} finally {
			if (received == null && source.getItems().contains(item)) {
				item.set_showId(originalShowId);
			}
		}
	}

	private void liberateClaimed(final L1Character master) {
		final L1Inventory transferredInventory = getInventory();
		try {
			final L1MonsterInstance monster = new L1MonsterInstance(getNpcTemplate());
			monster.setId(IdFactoryNpc.get().nextId());
			monster.setX(getX());
			monster.setY(getY());
			monster.setMap(getMapId());
			monster.setHeading(getHeading());
			monster.set_storeDroped(true);
			monster.setInventory(transferredInventory);
			monster.setCurrentHpDirect(getCurrentHp());
			monster.setCurrentMpDirect(getCurrentMp());
			monster.setExp(0);

			setInventory(null);
			if (!deleteWorldObjectClaimed(master, true)) {
				// Do not publish both the original summon and its liberated replacement
				// when the inherited world deletion silently returns without destroying.
				setInventory(transferredInventory);
				return;
			}
			World.get().storeObject(monster);
			if (!World.get().addVisibleObject(monster)) {
				_log.error("[WorldLifecycle] liberated summon replacement registration failed: "
						+ "sourceObjectId={} replacementObjectId={} npcId={}",
						getId(), monster.getId(), monster.getNpcId());
			}
		} catch (final RuntimeException e) {
			_log.error("Summon liberation failed; original remains retryable when present: name={}",
					getName(), e);
			if (!destroyed() && getInventory() == null) {
				setInventory(transferredInventory);
			}
			// If replacement creation failed before deletion, leave the original in
			// CLEANUP_FAILED so a later lifecycle owner can retry without item loss.
		}
	}

	private boolean deleteWorldObjectClaimed(final L1Character master,
			final boolean suppressDismissEffect) {
		if (destroyed()) {
			return true;
		}
		if (getInventory() != null && getInventory().getSize() != 0) {
			_log.error("Refusing to delete summon with non-empty inventory: name={} items={}",
					getName(), getInventory().getSize());
			return false;
		}
		if (master != null) {
			try {
				master.removePet(this);
			} catch (final RuntimeException e) {
				_log.error("Summon master detach failed: name={}", getName(), e);
			}
		}
		if (!_tamed && !suppressDismissEffect) {
			try {
				broadcastPacketX8(new S_SkillSound(getId(), 169));
			} catch (final RuntimeException e) {
				_log.error("Summon dismiss effect failed: name={}", getName(), e);
			}
		}
		try {
			super.deleteMe();
		} catch (final RuntimeException e) {
			_log.error("Summon world deletion failed after lifecycle claim: name={}",
					getName(), e);
		}
		final boolean deleted = destroyed();
		if (!deleted) {
			_log.error("Summon world deletion returned without terminal state: name={} npcId={}",
					getName(), getNpcId());
		}
		return deleted;
	}

	/** 召喚獸將移除前：周邊怪將對本召喚的仇恨轉給主人。 */
	private void removeSelfFromNearbyNpcHate(final L1Character master) {
		try {
			notifyNearbyNpcsTransferPetHateToMaster(master);
		} catch (final RuntimeException e) {
			_log.error("Summon hate transfer failed: name={}", getName(), e);
		}
	}

	private void broadcastDeathSafely() {
		try {
			broadcastPacketAll(new S_DoActionGFX(getId(), ActionCodes.ACTION_Die));
		} catch (final RuntimeException e) {
			_log.error("Summon death broadcast failed: name={}", getName(), e);
		}
	}

	private void startDeleteTimerSafely(final int seconds) {
		try {
			startDeleteTimer(seconds);
		} catch (final RuntimeException e) {
			_log.error("Summon delete timer failed: name={}", getName(), e);
		}
	}

	private void stopAiForCleanup() {
		try {
			releaseAiOwner(currentAiOwnerToken());
		} catch (final RuntimeException e) {
			_log.error("Summon AI stop failed during lifecycle cleanup: name={}",
					getName(), e);
		}
	}

	public void setTarget(final L1Character target) {
		if ((target != null) && (
				(this._currentPetStatus == 1) || 
				(this._currentPetStatus == 2) || 
				(this._currentPetStatus == 5)
				)) {
			this.setHate(target, 0);
			if (!this.isAiRunning()) {
				this.startAI();
			}
		}
	}

	/**
	 * 設置主人目標
	 * @param target
	 */
	public void setMasterTarget(final L1Character target) {
		// debug: 設置主人目標
		if ((target != null) && (
				(_currentPetStatus == 1) || // 攻擊
				(_currentPetStatus == 5)// 警戒
				)) {
			setHate(target, 10);
			if (!isAiRunning()) {
				startAI();
			}
		}
	}

	/**
	 * 對該物件攻擊的調用
	 */
	//@Override
	public void onAction(final L1PcInstance player) {
		if (player == null) {
			return;
		}
		final L1Character cha = this.getMaster();
		if (cha == null) {
			return;
		}

		// ==========================================================
		// [修正] 增加類型檢查，避免將假人(L1DeInstance)強制轉為玩家
		// ==========================================================
		final boolean ownerAttack;
		if (cha instanceof L1PcInstance master) {
			if (master.isTeleport()) { // 主人傳送中
				return;
			}
			ownerAttack = master.equals(player);
		} else {
			ownerAttack = false;
		}
		// 若主人是假人，跳過上述檢查，直接執行後續邏輯
		// ==========================================================

		// 安全區域判斷 (依賴修正後的 isExsistMaster)
		if ((this.isSafetyZone() || player.isSafetyZone()) && this.isExsistMaster()) {
			final L1AttackMode attack_mortion = new L1AttackPc(player, this); // 攻擊判斷
			attack_mortion.action();
			return;
		}

		// ==========================================================
		// [修正] 只有當主人是玩家時，才執行 checkNonPvP 檢查
		// 假人的寵物預設允許被攻擊 (視為怪物或一般物件)
		// ==========================================================
		if (cha instanceof L1PcInstance) {
			if (!ownerAttack && player.checkNonPvP(player, this)) {
				return;
			}
		}
		// ==========================================================
		
		final L1AttackMode attack = new L1AttackPc(player, this);
		if (attack.calcHit()) {
			attack.calcDamage();
		}
		attack.action();
		attack.commit();
	}

	//@Override
	public void onTalkAction(final L1PcInstance player) {
		if (this.isDead()) {
			return;
		}
		if (this._master.equals(player)) {
			player.sendPackets(new S_PetMenuPacket(this, 0));
		}
	}

	//@Override
	public void onFinalAction(final L1PcInstance player, final String action) {
		if (!CompanionCommandGuard.isOwner(player, this)) {
			return;
		}
		final int status = NumberUtils.toInt(action != null ? action.trim() : "", 0);
		//int status = ActionType(action);
		switch (status) {
		case 0:
			return;

		case 6:// 解散
			if (this._tamed) {
				// 馴服怪物、造屍術的解放
				this.liberate();
			} else {
				// 召喚獸的解散
				this.Death(null);
			}
			break;

		default:
			// 更新同一主人的所有寵物狀態
			final Object[] petList = this._master.getPetList().values().toArray();
			for (final Object petObject : petList) {
				if (petObject instanceof L1SummonInstance summon) {
					if (!CompanionCommandGuard.isOwner(player, summon)) {
						continue;
					}
					// 召喚怪物
					summon.set_currentPetStatus(status);
					
				} else {
					// 寵物
				}
			}
			break;
		}
	}

	/**
	 * TODO 接觸資訊
	 */
	//@Override
	public void onPerceive(final L1PcInstance perceivedFrom) {
		try {
			// 副本ID不相等 不相護顯示
			if (perceivedFrom.get_showId() != get_showId()) {
				return;
			}
			if (!perceivedFrom.tryAddKnownObject(this)) {
				return;
			}
			perceivedFrom.sendPackets(new S_NPCPack_Summon(this, perceivedFrom));
			if (getMaster() != null) {
				if (perceivedFrom.getId() == getMaster().getId()) {
					perceivedFrom.sendPackets(new S_HPMeter(getId(), 100 * getCurrentHp() / getMaxHp()));
				}
			}
			
		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	//@Override
	public void onItemUse() {
		if (!this.isActived()) {
			// 100%機率使用加速藥水
			this.useItem(USEITEM_HASTE, 100);
		}
		if (this.getCurrentHp() * 100 / this.getMaxHp() < 40) {
			// HP降至40%以下時
			// 100%機率使用回復藥水
			this.useItem(USEITEM_HEAL, 100);
		}
	}

	//@Override
	public void onGetItem(final L1ItemInstance item) {
		if (this.getNpcTemplate().get_digestitem() > 0) {
			this.setDigestItem(item);
		}
		Arrays.sort(healPotions);
		Arrays.sort(haestPotions);
		if (Arrays.binarySearch(healPotions, item.getItem().getItemId()) >= 0) {
			if (this.getCurrentHp() != this.getMaxHp()) {
				this.useItem(USEITEM_HEAL, 100);
			}
		} else if (Arrays
				.binarySearch(haestPotions, item.getItem().getItemId()) >= 0) {
			this.useItem(USEITEM_HASTE, 100);
		}
	}

	@Override
	protected void afterCurrentHpMutation(final CurrentHpMutation mutation) {
		if (!mutation.changed()) {
			return;
		}
		// Cross-character packet delivery intentionally runs after this summon's vital lock.
		if (_master instanceof L1PcInstance master) {
			final int hpRatio = 100 * getCurrentHp() / Math.max(1, getMaxHp());
			master.sendPackets(new S_HPMeter(getId(), hpRatio));
		}
	}

	public void set_currentPetStatus(final int i) {
		this._currentPetStatus = i;
		set_tempModel();
		switch (this._currentPetStatus) {
		case 5:
			this.setHomeX(this.getX());
			this.setHomeY(this.getY());
			break;

		case 3:
			this.allTargetClear();
			break;

		default:
			if (!this.isAiRunning()) {
				this.startAI();
			}
			break;
		}
	}

	public int get_currentPetStatus() {
		return this._currentPetStatus;
	}

	/**
	 * 是否具有主人
	 * @return
	 */
	public boolean isExsistMaster() {
		boolean isExsistMaster = true;
		if (this.getMaster() != null) {
			// [新增] 支援假人主人 (L1DeInstance)
			if (this.getMaster() instanceof com.lineage.server.model.Instance.L1DeInstance) {
				// 如果假人死了或血量<=0 視為不存在
				if (this.getMaster().isDead() || this.getMaster().getCurrentHp() <= 0) {
					isExsistMaster = false;
				}
			} else {
				// 原有的玩家判斷邏輯
				final String masterName = this.getMaster().getName();
				if (World.get().getPlayer(masterName) == null) {
					isExsistMaster = false;
				}
			}
		}
		return isExsistMaster;
	}
	
	private int _time = 0;

	/**
	 * 設置剩餘使用時間
	 * @return
	 */
	public int get_time() {
		return _time;
	}

	/**
	 * 剩餘使用時間
	 * @param time
	 */
	public void set_time(final int time) {
		this._time = time;
	}

	private int _tempModel = 3;
	
	public void set_tempModel() {
		_tempModel = _currentPetStatus;
	}
	
	public void get_tempModel() {
		_currentPetStatus = _tempModel;
	}
	/**
	 * 假人 {@code De_pet} 設定的 type（0/1/2 意義見 {@link #_dePetType} 欄位註解）。
	 */
	public void setDePetType(int type) {
		_dePetType = type;
	}

	public int getDePetType() {
		return _dePetType;
	}
}
