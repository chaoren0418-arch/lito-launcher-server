package com.lineage.server.model;

import static com.lineage.server.model.skill.L1SkillId.ABSOLUTE_BARRIER;
import static com.lineage.server.model.skill.L1SkillId.BLIND_HIDING;
import static com.lineage.server.model.skill.L1SkillId.BONE_BREAK;
import static com.lineage.server.model.skill.L1SkillId.CURSE_PARALYZE;
import static com.lineage.server.model.skill.L1SkillId.EARTH_BIND;
import static com.lineage.server.model.skill.L1SkillId.ERASE_MAGIC;
import static com.lineage.server.model.skill.L1SkillId.FREEZING_BLIZZARD;
import static com.lineage.server.model.skill.L1SkillId.FREEZING_BREATH;
import static com.lineage.server.model.skill.L1SkillId.ICE_LANCE;
import static com.lineage.server.model.skill.L1SkillId.INVISIBILITY;
import static com.lineage.server.model.skill.L1SkillId.LIGHT;
import static com.lineage.server.model.skill.L1SkillId.SHOCK_STUN;
import static com.lineage.server.model.skill.L1SkillId.STATUS_FREEZE;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lineage.server.model.Instance.L1DollInstance;
import com.lineage.server.model.Instance.L1FollowerInstance;
import com.lineage.server.model.Instance.L1ItemInstance;
import com.lineage.server.model.Instance.L1NpcInstance;
import com.lineage.server.model.Instance.L1PcInstance;
import com.lineage.server.model.Instance.L1PetInstance;
import com.lineage.server.model.Instance.L1SummonInstance;
import com.lineage.server.model.combat.DamageCommit;
import com.lineage.server.model.combat.DamageCommitQueue;
import com.lineage.server.model.combat.DamageEvent;
import com.lineage.server.model.map.L1Map;
import com.lineage.server.model.poison.L1Poison;
import com.lineage.server.model.skill.L1SkillTimer;
import com.lineage.server.model.skill.L1SkillTimerCreator;
import com.lineage.server.serverpackets.S_HPMeter;
import com.lineage.server.serverpackets.S_Light;
import com.lineage.server.serverpackets.S_PetCtrlMenu;
import com.lineage.server.serverpackets.S_Poison;
import com.lineage.server.serverpackets.S_RemoveObject;
import com.lineage.server.serverpackets.ServerBasePacket;
import com.lineage.server.timecontroller.server.ServerWarExecutor;
import com.lineage.server.types.Point;
import com.lineage.server.utils.RangeInt;
import com.lineage.server.world.PlayerFilter;
import com.lineage.server.world.World;
import com.lineage.server.world.WorldBroadcast;

/**
 * L1Character
 * @author daien
 *
 */
public class L1Character extends L1Object {

	private static final Logger _log = LogManager.getLogger(L1Character.class);

	private static final long serialVersionUID = 1L;

	/**
	 * 角色 MAX HP／MAX MP 伺服器端硬上限（RO 版登入器 32-bit 封包上限）。
	 * 公版上限維持 32767 (16-bit)。
	 */
	public static final int MAX_HP_MP_RO = 999_999;
	public static final int MAX_HP_MP_STANDARD = 32767;

	/** 永久技能效果哨兵，取代 null 以相容 ConcurrentHashMap 不允許 null value 的限制 */
	private static final L1SkillTimer PERMANENT_TIMER = new L1SkillTimer() {
		@Override public int getRemainingTime() { return -1; }
		@Override public void begin() { }
		@Override public void end() { }
		@Override public void kill() { }
	};

	/** Opaque per-removal claim; a unique token prevents transition ABA. */
	private static final class PoisonTransition extends L1Poison {
		@SuppressWarnings("unused")
		private final L1Poison owner;

		private PoisonTransition(final L1Poison owner) {
			this.owner = owner;
		}

		@Override public int getEffectId() { return 0; }
		@Override public void cure() { }
	}
	private static final AtomicReferenceFieldUpdater<L1Character, L1Poison> POISON_UPDATER =
			AtomicReferenceFieldUpdater.newUpdater(
					L1Character.class, L1Poison.class, "_poison");
	private volatile L1Poison _poison = null;
	
	private volatile boolean _sleeped;

	private final Map<Integer, L1NpcInstance> _petlist = new ConcurrentHashMap<>();
	
	private final ConcurrentHashMap<Integer, L1SkillTimer> _skillEffect = new ConcurrentHashMap<>();
	private final ReentrantLock _skillEffectLock = new ReentrantLock();

	/**
	 * One publication point for the active-skill set and every combat value derived
	 * from it.  Readers never observe a mixture of two cache generations.
	 */
	private static final class SkillEffectSnapshot {
		private static final L1AttackList.CombatSkillDerived EMPTY_DERIVED =
				new L1AttackList.CombatSkillDerived(0, 0, 0, 0, 0, false);
		private static final SkillEffectSnapshot EMPTY =
				new SkillEffectSnapshot(Set.of(), EMPTY_DERIVED);

		private final Set<Integer> activeSkillIds;
		private final L1AttackList.CombatSkillDerived combat;

		private SkillEffectSnapshot(final Set<Integer> activeSkillIds,
				final L1AttackList.CombatSkillDerived combat) {
			this.activeSkillIds = activeSkillIds;
			this.combat = combat;
		}
	}

	private volatile SkillEffectSnapshot _skillEffectSnapshot = SkillEffectSnapshot.EMPTY;

	/** 戰鬥派生快取：近戰命中加總（{@link L1AttackList#SKU1}），於技能增刪時重算 */
	@Deprecated
	protected volatile int _combatMeleeHitBonus = 0;

	/** 戰鬥派生快取：遠程命中加總（{@link L1AttackList#SKU2}） */
	@Deprecated
	protected volatile int _combatRangedHitBonus = 0;

	/** 戰鬥派生快取：近戰武器料理追加傷害加總（{@link L1AttackList#SKD1}） */
	@Deprecated
	protected volatile int _combatMeleeCookingDmgBonus = 0;

	/** 戰鬥派生快取：遠程武器料理追加傷害加總（{@link L1AttackList#SKD2}） */
	@Deprecated
	protected volatile int _combatRangedCookingDmgBonus = 0;

	/** 戰鬥派生快取：受物理傷害時 SKD3 加總（多為負值，語意同舊版 {@code dmg += integer}） */
	@Deprecated
	protected volatile int _combatDmgBonus = 0;

	/** 戰鬥派生快取：受物理傷害是否需除 2（如聖結界 IMMUNE_TO_HARM） */
	@Deprecated
	protected volatile boolean _combatDmgHalve = false;
	
	private final Map<Integer, L1ItemDelay.ItemDelayTimer> _itemdelay = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, L1Cube.CubeLease> _cubeLeases =
			new ConcurrentHashMap<>();
	
	private final Map<Integer, L1FollowerInstance> _followerlist = new ConcurrentHashMap<>();

	public L1Character() {
		this._level = 1;
	}

	/**
	 * 物件復活
	 *
	 * @param hp
	 *            復活後的HP
	 */
	public void resurrect(int hp) {
		try {
			final int restoredHp = Math.max(1, hp);
			final CurrentHpMutation hpMutation = withVitalMutation(() -> {
				if (!isDead()) {
					return null;
				}
				setDead(false);
				final CurrentHpMutation mutation = commitCurrentHpLocked(restoredHp);
				setStatus(0);
				return enqueueCurrentHpCompletionLocked(mutation);
			});
			if (hpMutation == null) {
				return;
			}
			// 解除變身
			L1PolyMorph.undoPoly(this);
			
			// 重新認識物件
			for (final L1PcInstance pc : World.get().getRecognizePlayer(this)) {
				pc.sendPackets(new S_RemoveObject(this));
				pc.removeKnownObject(this);
				pc.updateObject();
			}
			
		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	/** Last HP ratio (0–100) sent to each viewer; one target-wide scalar incorrectly suppressed fan-out. */
	private static final int HP_VIEWER_CACHE_LIMIT = 1024;
	private static final class ViewerHpState {
		private final WeakReference<L1PcInstance> viewer;
		private final AtomicInteger lastHpRatio = new AtomicInteger(-1);

		private ViewerHpState(final L1PcInstance viewer) {
			this.viewer = new WeakReference<>(viewer);
		}

		private boolean belongsTo(final L1PcInstance candidate) {
			return viewer.get() == candidate;
		}
	}

	private final ConcurrentHashMap<Integer, ViewerHpState> _secHpByViewer = new ConcurrentHashMap<>();

	/**
	 * 可見 HP 條百分比（與 {@link S_HPMeter} 一致：0–100）。
	 */
	private int hpMeterRatio() {
		final int maxHp = getMaxHp();
		if (maxHp <= 0) {
			return 100;
		}
		return 100 * getCurrentHp() / maxHp;
	}

	/**
	 * 發送該物件可見 HP 條至指定攻擊者（單播；DamageCommit SideEffects 路徑）。
	 * 僅在百分比相對該觀察者上次發送值變更時送出，避免同比例下的絕對 HP 抖動重送。
	 * @param pc 觀看 HP 條的玩家（通常為攻擊者）
	 */
	public void broadcastPacketHP(L1PcInstance pc) {
		try {
			// 副本ID相等 必須在這方法之前先判斷
			// 各觀察者獨立去重，避免第一位觀察者壓掉其餘 fan-out。
			final int hpRatio = hpMeterRatio();
			final int viewerId = pc.getId();
			// Long-lived targets must not retain an unbounded history of departed
			// viewers.  A concurrent clear can only cause a harmless duplicate send.
			if (_secHpByViewer.size() >= HP_VIEWER_CACHE_LIMIT
					&& !_secHpByViewer.containsKey(viewerId)) {
				_secHpByViewer.clear();
			}
			final ViewerHpState state = _secHpByViewer.compute(viewerId,
					(ignored, current) -> current != null && current.belongsTo(pc)
							? current : new ViewerHpState(pc));
			if (state.lastHpRatio.getAndSet(hpRatio) != hpRatio) {
				pc.sendPackets(new S_HPMeter(getId(), hpRatio));
			}
			
		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Hides this character's HP meter from viewers that previously received it,
	 * then releases the per-viewer delivery cache. Callers decide which lifecycle
	 * should hide the meter; player and companion deaths intentionally do not use
	 * this hook.
	 */
	protected final void hideTrackedHpBars() {
		try {
			for (final ViewerHpState state : _secHpByViewer.values()) {
				final L1PcInstance viewer = state.viewer.get();
				try {
					if (viewer != null) {
						viewer.sendPackets(new S_HPMeter(getId(), 0xFF));
					}
				} catch (final Exception e) {
					_log.error("HP meter hide failed: objectId={} viewerId={}",
							getId(), viewer != null ? viewer.getId() : -1, e);
				}
			}
		} finally {
			_secHpByViewer.clear();
		}
	}
	
	/**
	 * HP/MP are read by scheduler, combat and regeneration threads.  Volatile
	 * provides visibility; compound mutations are serialized by
	 * {@link #withVitalMutation(Supplier)}.
	 */
	private volatile int _currentHp;
	private volatile int _currentMp;
	private final ReentrantLock _npcVitalMutationLock = new ReentrantLock();
	private final ConcurrentLinkedQueue<Runnable> _vitalCompletions = new ConcurrentLinkedQueue<>();
	private final AtomicBoolean _vitalCompletionDraining = new AtomicBoolean();
	private static final ThreadLocal<ArrayDeque<DamageMutationReceipt>> DAMAGE_MUTATION_RECEIPTS =
			new ThreadLocal<>();

	/** per-target 傷害 Commit 佇列（Phase C-1；序列化 HP 寫入） */
	private volatile DamageCommitQueue _damageCommitQueue;
	private final ReentrantLock _damageCommitQueueLock = new ReentrantLock();

	/**
	 * 取得此角色專屬 {@link DamageCommitQueue}（懶初始化）。
	 */
	public DamageCommitQueue getDamageCommitQueue() {
		DamageCommitQueue queue = _damageCommitQueue;
		if (queue == null) {
			_damageCommitQueueLock.lock();
			try {
				queue = _damageCommitQueue;
				if (queue == null) {
					queue = new DamageCommitQueue(this);
					_damageCommitQueue = queue;
				}
			} finally {
				_damageCommitQueueLock.unlock();
			}
		}
		return queue;
	}

	/**
	 * 將傷害事件提交至此角色專屬 Commit 佇列。
	 */
	public void enqueueDamageCommit(final DamageEvent event) {
		DamageCommit.enqueue(event);
	}

	/**
	 * 現在的HP
	 *
	 * @return 現在HP
	 */
	public int getCurrentHp() {
		return _currentHp;
	}

	private ReentrantLock vitalMutationLock() {
		if (this instanceof L1PcInstance pc) {
			// PC action/death/damage state already has one authoritative lock.
			return pc.getPlayerActionLock();
		}
		return _npcVitalMutationLock;
	}

	/**
	 * Executes a target-local HP/MP transition.  Callers must not acquire a
	 * second character's vital lock from inside the callback.
	 */
	protected final <T> T withVitalMutation(final Supplier<T> mutation) {
		if (mutation == null) {
			throw new NullPointerException("mutation");
		}
		final ReentrantLock lock = vitalMutationLock();
		lock.lock();
		try {
			return mutation.get();
		} finally {
			lock.unlock();
			if (!lock.isHeldByCurrentThread()) {
				drainVitalCompletionsAfterActionUnlock();
			}
		}
	}

	protected final void withVitalMutation(final Runnable mutation) {
		withVitalMutation(() -> {
			mutation.run();
			return null;
		});
	}

	/**
	 * Records only HP/death transitions performed by the current thread while a
	 * legacy NPC receiveDamage call runs. External lifecycle mutations use another
	 * thread and therefore cannot make that call look accepted.
	 */
	protected static final class DamageMutationReceipt {
		private final L1Character _target;
		private final int _startHp;
		private int _hpBefore;
		private int _hpAfter;
		private boolean _hpSeen;
		private boolean _hpReduced;
		private boolean _deadAfter;
		private boolean _deathSeen;

		private DamageMutationReceipt(final L1Character target) {
			_target = target;
			_startHp = target.getCurrentHp();
			_hpBefore = _startHp;
			_hpAfter = _startHp;
			_deadAfter = target.isDead();
		}

		private void recordHp(final int before, final int after) {
			if (before == after) {
				return;
			}
			if (!_hpSeen) {
				_hpBefore = before;
				_hpSeen = true;
			}
			_hpAfter = after;
			_hpReduced |= after < before;
		}

		private void recordDead(final boolean before, final boolean after,
				final int currentHp) {
			if (before == after) {
				return;
			}
			_deathSeen = true;
			_deadAfter = after;
			if (!_hpSeen) {
				_hpBefore = currentHp;
				_hpAfter = currentHp;
			}
		}

		public boolean damageCommitted() {
			return _hpReduced || (_deathSeen && _deadAfter);
		}

		public int hpBefore() {
			return _hpBefore;
		}

		public int hpAfter() {
			return _hpAfter;
		}

		public boolean terminal() {
			return _deadAfter || (damageCommitted() && _hpAfter <= 0);
		}
	}

	protected final DamageMutationReceipt traceDamageMutations(final Runnable mutation) {
		if (mutation == null) {
			throw new NullPointerException("mutation");
		}
		ArrayDeque<DamageMutationReceipt> receipts = DAMAGE_MUTATION_RECEIPTS.get();
		if (receipts == null) {
			receipts = new ArrayDeque<>();
			DAMAGE_MUTATION_RECEIPTS.set(receipts);
		}
		final DamageMutationReceipt receipt = new DamageMutationReceipt(this);
		receipts.push(receipt);
		try {
			mutation.run();
		} finally {
			if (receipts.peek() == receipt) {
				receipts.pop();
			} else {
				receipts.remove(receipt);
			}
			if (receipts.isEmpty()) {
				DAMAGE_MUTATION_RECEIPTS.remove();
			}
		}
		return receipt;
	}

	private void recordDamageHpMutation(final int before, final int after) {
		if (before == after) {
			return;
		}
		final ArrayDeque<DamageMutationReceipt> receipts = DAMAGE_MUTATION_RECEIPTS.get();
		if (receipts == null) {
			return;
		}
		for (final DamageMutationReceipt receipt : receipts) {
			if (receipt._target == this) {
				receipt.recordHp(before, after);
				return;
			}
		}
	}

	private void recordDamageDeathMutation(final boolean before, final boolean after) {
		if (before == after) {
			return;
		}
		final ArrayDeque<DamageMutationReceipt> receipts = DAMAGE_MUTATION_RECEIPTS.get();
		if (receipts == null) {
			return;
		}
		for (final DamageMutationReceipt receipt : receipts) {
			if (receipt._target == this) {
				receipt.recordDead(before, after, _currentHp);
				return;
			}
		}
	}

	private static int boundedVitalValue(final int current, final long delta,
			final int minimum, final int maximum) {
		final int upper = Math.max(0, maximum);
		final int lower = Math.max(0, Math.min(minimum, upper));
		final long sum;
		try {
			sum = Math.addExact((long) current, delta);
		} catch (final ArithmeticException overflow) {
			return delta >= 0L ? upper : lower;
		}
		return (int) Math.max(lower, Math.min((long) upper, sum));
	}

	/**
	 * Immutable result of one target-local HP transition.  Subclasses may extend
	 * this value with a terminal-state token, but must keep heavyweight work in
	 * {@link #afterCurrentHpMutation(CurrentHpMutation)}.
	 */
	protected static class CurrentHpMutation {
		private final int _previousHp;
		private final int _currentHp;

		protected CurrentHpMutation(final int previousHp, final int currentHp) {
			_previousHp = previousHp;
			_currentHp = currentHp;
		}

		public final int previousHp() {
			return _previousHp;
		}

		public final int currentHp() {
			return _currentHp;
		}

		public final boolean changed() {
			return _previousHp != _currentHp;
		}

		protected boolean completionRequired() {
			return changed();
		}
	}

	/** Immutable result of one target-local MP transition. */
	protected static class CurrentMpMutation {
		private final int _previousMp;
		private final int _currentMp;

		protected CurrentMpMutation(final int previousMp, final int currentMp) {
			_previousMp = previousMp;
			_currentMp = currentMp;
		}

		public final int previousMp() {
			return _previousMp;
		}

		public final int currentMp() {
			return _currentMp;
		}

		public final boolean changed() {
			return _previousMp != _currentMp;
		}

		protected boolean completionRequired() {
			return changed();
		}
	}

	private static final class MpDebitMutation {
		private final int _debited;
		private final CurrentMpMutation _mutation;

		private MpDebitMutation(final int debited, final CurrentMpMutation mutation) {
			_debited = debited;
			_mutation = mutation;
		}
	}

	/**
	 * Locked phase for an absolute HP assignment.  This method must contain only
	 * target-local state; packet delivery, death handoff and cross-character work
	 * belong in {@link #afterCurrentHpMutation(CurrentHpMutation)}.
	 */
	protected CurrentHpMutation commitCurrentHpLocked(final int requestedHp) {
		final int previousHp = _currentHp;
		if (!isCurrentHpMutationAllowed()) {
			return new CurrentHpMutation(previousHp, previousHp);
		}
		final int currentHp = Math.max(0,
				Math.min(requestedHp, Math.max(0, getMaxHp())));
		// Resurrection is a distinct transition which clears dead under this lock
		// before assigning positive HP. Ordinary heals/setters must not revive.
		if (isDead() && currentHp > 0) {
			return new CurrentHpMutation(previousHp, previousHp);
		}
		_currentHp = currentHp;
		recordDamageHpMutation(previousHp, currentHp);
		return new CurrentHpMutation(previousHp, currentHp);
	}

	/** Locked phase for an absolute MP assignment. */
	protected CurrentMpMutation commitCurrentMpLocked(final int requestedMp) {
		final int previousMp = _currentMp;
		if (!isCurrentMpMutationAllowed()) {
			return new CurrentMpMutation(previousMp, previousMp);
		}
		final int currentMp = Math.max(0,
				Math.min(requestedMp, Math.max(0, getMaxMp())));
		_currentMp = currentMp;
		return new CurrentMpMutation(previousMp, currentMp);
	}

	/** Override only for deliberately immutable/invulnerable character types. */
	protected boolean isCurrentHpMutationAllowed() {
		return true;
	}

	/** Override only for deliberately immutable/invulnerable character types. */
	protected boolean isCurrentMpMutationAllowed() {
		return true;
	}

	/** Runs after the target-local vital lock has been released. */
	protected void afterCurrentHpMutation(final CurrentHpMutation mutation) {
	}

	/** Runs after the target-local vital lock has been released. */
	protected void afterCurrentMpMutation(final CurrentMpMutation mutation) {
	}

	private CurrentHpMutation enqueueCurrentHpCompletionLocked(final CurrentHpMutation mutation) {
		if (mutation.completionRequired()) {
			_vitalCompletions.add(() -> afterCurrentHpMutation(mutation));
		}
		return mutation;
	}

	private CurrentMpMutation enqueueCurrentMpCompletionLocked(final CurrentMpMutation mutation) {
		if (mutation.completionRequired()) {
			_vitalCompletions.add(() -> afterCurrentMpMutation(mutation));
		}
		return mutation;
	}

	/**
	 * Drains side effects only when the caller no longer owns this character's
	 * vital/action lock.  {@link PlayerActionLock} invokes this again after an
	 * outer player action critical section releases its final hold.
	 */
	final void drainVitalCompletionsAfterActionUnlock() {
		final ReentrantLock vitalLock = vitalMutationLock();
		for (;;) {
			if (vitalLock.isHeldByCurrentThread()
					|| !_vitalCompletionDraining.compareAndSet(false, true)) {
				return;
			}
			try {
				for (;;) {
					final ArrayDeque<Runnable> batch = new ArrayDeque<>();
					// isHeldByCurrentThread() says nothing about a different owner.  Taking
					// the target lock here is the publication barrier which proves every
					// completion copied into this batch belongs to a finished mutation.
					vitalLock.lock();
					try {
						Runnable completion;
						while ((completion = _vitalCompletions.poll()) != null) {
							batch.addLast(completion);
						}
					} finally {
						vitalLock.unlock();
					}
					if (batch.isEmpty()) {
						break;
					}
					Runnable completion;
					while ((completion = batch.pollFirst()) != null) {
						try {
							// Cross-character effects execute with no source vital lock held.
							completion.run();
						} catch (final Throwable failure) {
							// One packet/death hook must not strand later committed state.
							_log.error("Vital completion failed: objectId={} type={}",
									getId(), getClass().getName(), failure);
						}
					}
				}
			} finally {
				_vitalCompletionDraining.set(false);
			}
			// Iterative retry closes enqueue-after-last-poll without recursive stack
			// growth under a sustained HP/MP producer.
			if (_vitalCompletions.isEmpty()) {
				return;
			}
		}
	}

	/**
	 * Atomically adjusts HP and clamps the result to {@code 0..maxHp}.  State is
	 * committed under the target-local lock; subclass side effects run after it.
	 *
	 * @return the value visible after the virtual setter completes
	 */
	public final int adjustCurrentHp(final long delta) {
		return adjustCurrentHp(delta, 0);
	}

	/**
	 * HP adjustment variant for legacy non-lethal recovery zones whose floor is
	 * one. The requested floor is itself clamped to {@code 0..maxHp}.
	 */
	public final int adjustCurrentHp(final long delta, final int minimum) {
		final CurrentHpMutation mutation = withVitalMutation(() -> {
			final int next = boundedVitalValue(_currentHp, delta, minimum, getMaxHp());
			return enqueueCurrentHpCompletionLocked(commitCurrentHpLocked(next));
		});
		return mutation.currentHp();
	}

	/**
	 * Alive-only HP adjustment for effects that are explicitly non-lethal.  The
	 * alive check and floor are one target-local transition, so a delayed tick
	 * cannot resurrect a target that another commit has already killed.
	 */
	public final int adjustCurrentHpIfAlive(final long delta, final int minimum) {
		final CurrentHpMutation mutation = withVitalMutation(() -> {
			if (_currentHp <= 0 || isDead()) {
				return enqueueCurrentHpCompletionLocked(
						new CurrentHpMutation(_currentHp, _currentHp));
			}
			final int next = boundedVitalValue(_currentHp, delta,
					Math.min(_currentHp, minimum), getMaxHp());
			return enqueueCurrentHpCompletionLocked(commitCurrentHpLocked(next));
		});
		return mutation.currentHp();
	}

	/**
	 * Atomically adjusts MP and clamps the result to {@code 0..maxMp}.
	 *
	 * @return the value visible after the virtual setter completes
	 */
	public final int adjustCurrentMp(final long delta) {
		final CurrentMpMutation mutation = withVitalMutation(() -> {
			final int next = boundedVitalValue(_currentMp, delta, 0, getMaxMp());
			return enqueueCurrentMpCompletionLocked(commitCurrentMpLocked(next));
		});
		return mutation.currentMp();
	}

	/**
	 * Atomically removes at most the currently available MP.
	 *
	 * @return the amount actually removed
	 */
	public final int debitCurrentMpUpTo(final long requested) {
		if (requested <= 0L) {
			return 0;
		}
		final MpDebitMutation debit = withVitalMutation(() -> {
			final MpDebitMutation committed = debitCurrentMpLocked(requested);
			enqueueCurrentMpCompletionLocked(committed._mutation);
			return committed;
		});
		return debit._debited;
	}

	/** Atomically debits MP only while this character is alive. */
	public final int debitCurrentMpUpToIfAlive(final long requested) {
		if (requested <= 0L) {
			return 0;
		}
		final MpDebitMutation debit = withVitalMutation(() -> {
			final MpDebitMutation committed = isDead()
					? new MpDebitMutation(0, new CurrentMpMutation(_currentMp, _currentMp))
					: debitCurrentMpLocked(requested);
			enqueueCurrentMpCompletionLocked(committed._mutation);
			return committed;
		});
		return debit._debited;
	}

	private MpDebitMutation debitCurrentMpLocked(final long requested) {
		final int available = Math.max(0, _currentMp);
		final int debit = (int) Math.min((long) available, requested);
		final CurrentMpMutation mutation = debit > 0
				? commitCurrentMpLocked(available - debit)
				: new CurrentMpMutation(_currentMp, _currentMp);
		return new MpDebitMutation(debit, mutation);
	}

	/**
	 * 設置新HP
	 *
	 * @param i
	 */
	// Specialized behavior belongs in the locked commit / unlocked after hooks.
	public final void setCurrentHp(final int i) {
		withVitalMutation(() -> enqueueCurrentHpCompletionLocked(commitCurrentHpLocked(i)));
	}

	/**
	 * 設置登場物件HP
	 *
	 * @param i
	 */
	public final void setCurrentHpDirect(final int i) {
		withVitalMutation(() -> {
			final int previousHp = _currentHp;
			_currentHp = i;
			recordDamageHpMutation(previousHp, i);
		});
	}

	/**
	 * 現在的MP
	 *
	 * @return 現在MP
	 */
	public int getCurrentMp() {
		return _currentMp;
	}

	/**
	 * 設置新MP
	 *
	 * @param i
	 */
	public final void setCurrentMp(final int i) {
		withVitalMutation(() -> enqueueCurrentMpCompletionLocked(commitCurrentMpLocked(i)));
	}

	/**
	 * 設置登場物件MP
	 *
	 * @param i
	 */
	public final void setCurrentMpDirect(final int i) {
		withVitalMutation(() -> _currentMp = i);
	}

	/**
	 * 是否為睡眠狀態
	 *
	 * @return true:麻痺狀態 false:無
	 */
	public boolean isSleeped() {
		return _sleeped;
	}

	/**
	 * 是否為睡眠狀態
	 *
	 * @param sleeped true:睡眠狀態 false:無
	 */
	public void setSleeped(final boolean sleeped) {
		this._sleeped = sleeped;
	}

	/**
	 * 無法攻擊/使用道具/技能/回城的狀態
	 *
	 * @return true:狀態中 false:無
	 */
	public boolean isParalyzedX() {
		// 冰矛圍籬
		if (hasSkillEffect(ICE_LANCE)) {
			return true;
		}
		// 冰雪颶風
		if (hasSkillEffect(FREEZING_BLIZZARD)) {
			return true;
		}
		// 寒冰噴吐
		if (hasSkillEffect(FREEZING_BREATH)) {
			return true;
		}
		// 大地屏障
		if (hasSkillEffect(EARTH_BIND)) {
			return true;
		}
		// 衝擊之暈
		if (hasSkillEffect(SHOCK_STUN)) {
			return true;
		}
		// 骷髏毀壞
		if (hasSkillEffect(BONE_BREAK)) {
			return true;
		}
		// 木乃伊的詛咒
		if (hasSkillEffect(CURSE_PARALYZE)) {
			return true;
		}
		return false;
	}
	
	private volatile boolean _paralyzed;// 麻痺狀態

	/**
	 * 是否為麻痺狀態
	 *
	 * @return true:麻痺狀態 false:無
	 */
	public boolean isParalyzed() {
		return this._paralyzed;
	}

	/**
	 * 設定麻痺狀態
	 *
	 * @param paralyzed true:麻痺狀態 false:無
	 */
	public void setParalyzed(final boolean paralyzed) {
		this._paralyzed = paralyzed;
	}

	private static final class ParalysisTransition extends L1Paralysis {
		@SuppressWarnings("unused")
		private final L1Paralysis owner;

		private ParalysisTransition(final L1Paralysis owner) {
			this.owner = owner;
		}

		@Override public int getEffectId() { return 0; }
		@Override public void cure() { }
	}
	private static final AtomicReferenceFieldUpdater<L1Character, L1Paralysis> PARALYSIS_UPDATER =
			AtomicReferenceFieldUpdater.newUpdater(
					L1Character.class, L1Paralysis.class, "_paralysis");
	private volatile L1Paralysis _paralysis;

	public L1Paralysis getParalysis() {
		final L1Paralysis paralysis = this._paralysis;
		return paralysis instanceof ParalysisTransition ? null : paralysis;
	}

	public void setParalaysis(final L1Paralysis p) {
		if (p == null) {
			cureParalaysis();
			return;
		}
		tryInstallParalysis(p);
	}

	public boolean tryInstallParalysis(final L1Paralysis paralysis) {
		return (paralysis != null) && !(paralysis instanceof ParalysisTransition)
				&& PARALYSIS_UPDATER.compareAndSet(this, null, paralysis);
	}

	public boolean isCurrentParalysis(final L1Paralysis expected) {
		return (expected != null) && (this._paralysis == expected);
	}

	public L1Paralysis claimParalysisRemoval(final L1Paralysis expected) {
		if (expected == null) {
			return null;
		}
		final L1Paralysis transition = new ParalysisTransition(expected);
		return PARALYSIS_UPDATER.compareAndSet(this, expected, transition)
				? transition : null;
	}

	public boolean completeParalysisRemoval(final L1Paralysis transition) {
		return (transition instanceof ParalysisTransition)
				&& PARALYSIS_UPDATER.compareAndSet(this, transition, null);
	}

	public void cureParalaysis() {
		final L1Paralysis paralysis = getParalysis();
		if (paralysis != null) {
			paralysis.cure();
		}
	}

	/**
	 * 該物件全部可見範圍封包發送(不包含自己)
	 * @param packet 封包
	 */
	public void broadcastPacketAll(final ServerBasePacket packet) {
		WorldBroadcast.toVisiblePlayers(this, -1, packet);
	}

	/**
	 * 該物件指定範圍封包發送(範圍10)
	 * @param packet 封包
	 */
	public void broadcastPacketX10(final ServerBasePacket packet) {
		WorldBroadcast.toVisiblePlayers(this, 10, packet);
	}

	/**
	 * Proc 特效專用：僅發送給「特效顯示」開啟的玩家（proc_spr）
	 * 範圍 10 格，用於 Proc 系統的 S_SkillSound / S_DoActionGFX
	 */
	public void broadcastPacketX10ForProcViewers(final ServerBasePacket packet) {
		try {
			WorldBroadcast.forEachVisiblePlayer(this, 10, PlayerFilter.BROADCAST, pc -> {
				if (!pc.isShowProcSpr()) {
					return;
				}
				pc.sendPackets(packet);
			});
		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Proc 特效專用：僅發送給「特效顯示」開啟的玩家（proc_spr）
	 * 全可見範圍，用於娃娃 Proc 的 S_DoActionGFX
	 */
	public void broadcastPacketAllForProcViewers(final ServerBasePacket packet) {
		try {
			WorldBroadcast.forEachVisiblePlayer(this, -1, PlayerFilter.BROADCAST, pc -> {
				if (!pc.isShowProcSpr()) {
					return;
				}
				pc.sendPackets(packet);
			});
		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * 該物件指定範圍封包發送(範圍8)
	 * @param packet 封包
	 */
	public void broadcastPacketX8(final ServerBasePacket packet) {
		WorldBroadcast.toVisiblePlayers(this, 8, packet);
	}

	/**
	 * 該物件指定範圍封包發送(指定範圍)
	 * @param packet 封包
	 * @param r 指定範圍
	 */
	public void broadcastPacketXR(final ServerBasePacket packet, final int r) {
		WorldBroadcast.toVisiblePlayers(this, r, packet);
	}

	/**
	 * 該物件50格範圍封包發送
	 * @param packet 封包
	 */
	public void wideBroadcastPacket(final ServerBasePacket packet) {
		WorldBroadcast.toVisiblePlayers(this, 50, packet);
	}

	/**
	 * 該物件可見範圍封包發送, (指定物件)
	 * @param packet 封包
	 * @param target 指定物件
	 */
	public void broadcastPacketExceptTargetSight(final ServerBasePacket packet, final L1Character target) {
		boolean isX8 = false;
		// 檢查城堡戰爭狀態
		if (ServerWarExecutor.get().checkCastleWar() > 0) {
			isX8 = true;;
		}
		if (isX8) {
			for (final L1PcInstance tgpc : World.get().getVisiblePlayerExceptTargetSight(this, target, 8)) {
				tgpc.sendPackets(packet);
			}
			
		} else {
			for (final L1PcInstance tgpc : World.get().getVisiblePlayerExceptTargetSight(this, target)) {
				tgpc.sendPackets(packet);
			}
		}
	}

	// 正向
	protected static final byte HEADING_TABLE_X[] = { 0, 1, 1, 1, 0, -1, -1, -1 };
	protected static final byte HEADING_TABLE_Y[] = { -1, -1, 0, 1, 1, 1, 0, -1 };

	/**
	 * 回傳角色正面座標。
	 *
	 * @return 正面座標
	 */
	public int[] getFrontLoc() {
		final int[] loc = new int[2];
		int x = this.getX();
		int y = this.getY();
		final int heading = this.getHeading();

		x += HEADING_TABLE_X[heading];
		y += HEADING_TABLE_Y[heading];

		loc[0] = x;
		loc[1] = y;
		return loc;
	}

	/**
	 * 指定座標對硬的面向
	 *
	 * @param tx 座標 X 值
	 * @param ty 座標 Y 值
	 * @return 指定座標對硬的面向
	 */
	public int targetDirection(final int tx, final int ty) {

		final float dis_x = Math.abs(this.getX() - tx); // X點方向距離
		final float dis_y = Math.abs(this.getY() - ty); // Y點方向距離
		final float dis = Math.max(dis_x, dis_y); // 取得兩者最大值
		if (dis == 0) {
			return this.getHeading(); // 距離為0表示不須改變面向
		}
		final int avg_x = (int) Math.floor((dis_x / dis) + 0.59f); // 上下左右略優先的捨入
		final int avg_y = (int) Math.floor((dis_y / dis) + 0.59f); // 上下左右略優先的捨入

		int dir_x = 0;
		int dir_y = 0;
		if (this.getX() < tx) {
			dir_x = 1;
		}
		if (this.getX() > tx) {
			dir_x = -1;
		}
		if (this.getY() < ty) {
			dir_y = 1;
		}
		if (this.getY() > ty) {
			dir_y = -1;
		}

		if (avg_x == 0) {
			dir_x = 0;
		}
		if (avg_y == 0) {
			dir_y = 0;
		}

		switch (dir_x) {
		case -1:
			switch (dir_y) {
			case -1:
				return 7; // 左
			case 0:
				return 6; // 左下
			case 1:
				return 5; // 下
			}
			break;
		case 0:
			switch (dir_y) {
			case -1:
				return 0; // 左上
			case 1:
				return 4; // 右下
			}
			break;
		case 1:
			switch (dir_y) {
			case -1:
				return 1; // 上
			case 0:
				return 2; // 右上
			case 1:
				return 3; // 右
			}
			break;
		}
		return this.getHeading(); // 理論上不會到達此處
	}

	/**
	 * 由相鄰格 (from)→(to) 推算單步 heading（0–7），與 {@link #targetDirection(int, int)} 之方位表一致。
	 */
	public static int headingTowardAdjacentCell(final int fromX, final int fromY, final int toX, final int toY) {
		final int ddx = Integer.signum(toX - fromX);
		final int ddy = Integer.signum(toY - fromY);
		switch (ddx) {
		case -1:
			switch (ddy) {
			case -1:
				return 7;
			case 0:
				return 6;
			case 1:
				return 5;
			default:
				break;
			}
			break;
		case 0:
			switch (ddy) {
			case -1:
				return 0;
			case 1:
				return 4;
			default:
				break;
			}
			break;
		case 1:
			switch (ddy) {
			case -1:
				return 1;
			case 0:
				return 2;
			case 1:
				return 3;
			default:
				break;
			}
			break;
		default:
			break;
		}
		return 0;
	}

	/**
	 * 回傳指定座標直線上是否無障礙物。
	 *
	 * @param tx
	 *            座標 X 值
	 * @param ty
	 *            座標 Y 值
	 * @return 無障礙物則 true，有則 false
	 */
	public boolean glanceCheck(final int tx, final int ty) {
		return hasLineOfSight(getMap(), getX(), getY(), tx, ty);
	}

	/**
	 * 由任意起點檢查箭矢視線。玩家攻擊與伺服器內掛的射擊格必須共用此實作。
	 */
	public static boolean hasLineOfSight(final L1Map map, final int fromX, final int fromY,
			final int tx, final int ty) {
		int chx = fromX;
		int chy = fromY;
		
		if (chx == tx && chy == ty) {
			return true;
		}

		final int dx = Math.abs(tx - chx);
		final int dy = Math.abs(ty - chy);
		final boolean exactDiagonalRay = dx == dy;
		final int sx = Integer.compare(tx, chx);
		final int sy = Integer.compare(ty, chy);
		int crossedX = 0;
		int crossedY = 0;

		/*
		 * 由格子中心向目標格中心做網格邊界巡訪。Bresenham 只挑一條「看起來最接近」
		 * 的階梯路徑，斜率不是 1:1 時會漏掉射線實際進入的側邊格。
		 *
		 * 這裡比較下一條 X/Y 邊界的精確整數比例；同時撞到兩條邊界才是真正的
		 * 45 度對角步，否則逐一踏入射線實際穿越的格子。如此不限制射擊角度，
		 * 但玩家、怪物、近戰、遠程與魔法都不能再用階梯取樣切過障礙。
		 */
		final long maximumSteps = (long) dx + dy;
		for (long i = 0; i < maximumSteps && (chx != tx || chy != ty); i++) {
			int nextX = chx;
			int nextY = chy;
			final long nextXBoundary = crossedX < dx
					? (2L * crossedX + 1L) * dy : Long.MAX_VALUE;
			final long nextYBoundary = crossedY < dy
					? (2L * crossedY + 1L) * dx : Long.MAX_VALUE;
			final boolean moveX = nextXBoundary <= nextYBoundary;
			final boolean moveY = nextYBoundary <= nextXBoundary;

			if (moveX) {
				nextX += sx;
				crossedX++;
			}
			if (moveY) {
				nextY += sy;
				crossedY++;
			}

			/*
			 * X/Y 邊界不同時到達時，這是直線依序穿越兩條正交邊，
			 * 不是射線在格子內轉彎。因此只驗證實際穿越的邊與中間格，
			 * 不得再探測未被射線穿越的第四個外側格。
			 */

			// 對角射線只有在兩條正交繞角路徑都封閉時才算切牆角。
			// 單側牆仍可能是地圖作者允許的斜向通道；舊邏輯從兩個側邊格沿總方向
			// 多探一格，會把射線外的牆誤判成射線障礙。
			// 靜態 tile=3 可承載目標，不可因此擋住攻擊；真正關閉的動態門仍須阻擋。
			if (map.isClosedDoorAt(nextX, nextY)) {
				return false;
			}
			boolean diagonalStepPassable = false;
			if (moveX && moveY) {
				final int horizontalCornerX = nextX;
				final int horizontalCornerY = chy;
				final int verticalCornerX = chx;
				final int verticalCornerY = nextY;
				final int horizontalHeading = headingTowardAdjacentCell(chx, chy, nextX, chy);
				final int verticalHeading = headingTowardAdjacentCell(chx, chy, chx, nextY);
				final int horizontalThenVertical = headingTowardAdjacentCell(nextX, chy, nextX, nextY);
				final int verticalThenHorizontal = headingTowardAdjacentCell(chx, nextY, nextX, nextY);
				final boolean viaHorizontal = map.isArrowPassable(horizontalCornerX, horizontalCornerY)
						&& isProjectileCardinalBoundaryOpen(map,
						chx, chy, nextX, chy, horizontalHeading)
						&& isProjectileCardinalBoundaryOpen(map,
								nextX, chy, nextX, nextY, horizontalThenVertical);
				final boolean viaVertical = map.isArrowPassable(verticalCornerX, verticalCornerY)
						&& isProjectileCardinalBoundaryOpen(map,
						chx, chy, chx, nextY, verticalHeading)
						&& isProjectileCardinalBoundaryOpen(map,
								chx, nextY, nextX, nextY, verticalThenHorizontal);
				/*
				 * 只有整段射線是 45 度（|dx| == |dy|）時，才能沿格點的單側開放通路通過。
				 * 非 45 度射線也可能在某一步剛好穿過格點；此時若仍使用 OR，會讓 1:3、1:5
				 * 等斜率擦過另一側的牆角。非 45 度須兩側都完整開放。
				 */
				diagonalStepPassable = exactDiagonalRay
						? (viaHorizontal || viaVertical)
						: (viaHorizontal && viaVertical);
				if (!diagonalStepPassable) {
					return false;
				}
			}

			// 踏出前：驗證「本步」由目前格沿相鄰方向進入 next 格（isArrowPassable 自目前格看鄰格）。
			// 原流程只在落點後用「終點總方向 th」檢更外一圈，會漏掉第一步踏上的中間格，造成直線隔牆仍通過。
			if (moveX && moveY) {
				if (!diagonalStepPassable) {
					return false;
				}
			} else {
				final int stepTh = headingTowardAdjacentCell(chx, chy, nextX, nextY);
				if (!isProjectileCardinalBoundaryOpen(map,
						chx, chy, nextX, nextY, stepTh)) {
					return false;
				}
			}

			/*
			 * 非正 45 度的中間格仍必須具有 0x04/0x08 箭線位元。
			 * 正 45 度每步同時跨越 X/Y 邊界，則以上方已完整驗證的
			 * diagonalStepPassable 為準，避免中間格位元重複否決實際開放的直線。
			 */
			final boolean intermediateTile = nextX != tx || nextY != ty;
			// 正 45 度每步同時跨越 X/Y 邊界；上方已完整驗證實際通路。
			// 不再依賴地圖編號或特定 tile 值放行。
			final boolean exactDiagonalPassage = exactDiagonalRay
					&& moveX && moveY && diagonalStepPassable;
			if (intermediateTile && !map.isArrowPassable(nextX, nextY)
					&& !exactDiagonalPassage) {
				return false;
			}

			chx = nextX;
			chy = nextY;

			// 實際跨越的最後一條邊也通過後才放行；目標本身不作為額外肉體障礙。
			if (chx == tx && chy == ty) {
				return true;
			}
			/*
			 * 每一條實際跨越邊都已在上方驗證；不可再以總方向向前探一格。
			 * 目標格由 isProjectileCardinalBoundaryOpen 驗證進入邊；
			 * 正 45 度中間格由 diagonalStepPassable 驗證實際跨越邊界。
			 */
		}
		return true;
	}

	/**
	 * 檢查射線實際跨越的單一正交邊。
	 *
	 * 邊的開放資料沿用地圖移動方向位元；這只證明射線能跨過邊界，
	 * 不代表能穿過整個中間格。整格箭線位元由主迴圈另行驗證。
	 */
	private static boolean isProjectileCardinalBoundaryOpen(final L1Map map,
			final int fromX, final int fromY, final int toX, final int toY,
			final int heading) {
		if (heading < 0 || (heading & 1) != 0 || map.isClosedDoorAt(toX, toY)) {
			return false;
		}
		if (map.isArrowPassableOverride(toX, toY)) {
			return true;
		}
		return map.isPassable(fromX, fromY, heading, null);
	}

	/**
	 * 回傳是否可攻擊指定座標。
	 *
	 * @param x
	 *            座標 X 值
	 * @param y
	 *            座標 Y 值
	 * @param range
	 *            攻擊範圍（格數）
	 * @return 可攻擊則 true，不可則 false
	 */
	public boolean isAttackPosition(final int x, final int y, final int range) {
		if (range >= 7) {// 遠程武器（7 以上時考量斜向會超出畫面）
			if (getLocation().getTileDistance(new Point(x, y)) > range) {
				return false;
			}

		} else {// 近戰武器
			if (getLocation().getTileLineDistance(new Point(x, y)) > range) {
				return false;
			}
		}
		return this.glanceCheck(x, y);
	}

	/**
	 * 傳回背包資料
	 *
	 * @return L1Inventory
	 */
	public L1Inventory getInventory() {
		return null;
	}

	/**
	 * 為角色新增技能效果。
	 *
	 * @param skillId
	 *            欲新增效果的技能 ID
	 * @param timeMillis
	 *            效果持續時間。無限為 0
	 */
	private L1SkillTimer createSkillTimerLocked(final int skillId, final int timeMillis) {
		final L1SkillTimer timer;
		if (timeMillis > 0) {
			timer = L1SkillTimerCreator.create(this, skillId, timeMillis);
			try {
				timer.begin();
			} catch (final RejectedExecutionException e) {
				_log.warn("Skill effect timer rejected: objId={} name={} skillId={} timeMillis={}",
						getId(), getName(), skillId, timeMillis, e);
				return null;
			}
		} else {
			timer = PERMANENT_TIMER;
		}
		return timer;
	}

	/**
	 * 為角色設定技能效果。<br>
	 * 若無重複技能，則新增技能效果。<br>
	 * 若有重複，則以剩餘效果時間與參數效果時間較長者為準。
	 *
	 * @param skillId
	 *            要設定的效果技能 ID。
	 * @param timeMillis 效果時間(單位:毫秒)
	 */
	public void setSkillEffect(final int skillId, final int timeMillis) {
		L1SkillTimer replacedTimer = null;
		_skillEffectLock.lock();
		try {
			final L1SkillTimer currentTimer = _skillEffect.get(skillId);
			if (currentTimer == null) {
				final L1SkillTimer newTimer = createSkillTimerLocked(skillId, timeMillis);
				if (newTimer != null) {
					_skillEffect.put(skillId, newTimer);
					publishSkillEffectSnapshotLocked();
				}
			} else {
				final int remainingSeconds = currentTimer.getRemainingTime();
				final long remainingMillis = (long) remainingSeconds * 1000L;
				// 剩餘時間有限且參數效果時間較長或無限時，則覆寫。
				if ((remainingSeconds >= 0)
						&& ((remainingMillis < timeMillis) || (timeMillis == 0))) {
					final L1SkillTimer newTimer = createSkillTimerLocked(skillId, timeMillis);
					if (newTimer != null) {
						_skillEffect.put(skillId, newTimer);
						publishSkillEffectSnapshotLocked();
						replacedTimer = currentTimer;
					}
				}
			}
		} finally {
			_skillEffectLock.unlock();
		}
		// Timer cancellation and talent/stat side effects may re-enter character
		// code.  They deliberately run after the map+snapshot linearization lock.
		if (replacedTimer != null) {
			replacedTimer.kill();
		}
		
		// [天賦系統] 當 Buff 變化時，若對象是玩家則重算狀態天賦能力
		if (this instanceof com.lineage.server.model.Instance.L1PcInstance pc) {
			pc.applyTalentBuffAbility();
		}
	}

	/**
	 * 技能效果結束
	 *
	 * @param skillId 技能編號
	 */
	public void removeSkillEffect(final int skillId) {
		final L1SkillTimer timer;
		_skillEffectLock.lock();
		try {
			timer = _skillEffect.remove(skillId);
			if (timer != null) {
				publishSkillEffectSnapshotLocked();
			}
		} finally {
			_skillEffectLock.unlock();
		}
		if (timer != null) {
			timer.end();
		}
		
		// [天賦系統] 當 Buff 移除時，若對象是玩家則重算狀態天賦能力
		if (this instanceof com.lineage.server.model.Instance.L1PcInstance pc) {
			pc.applyTalentBuffAbility();
		}
	}
	/**
	 * Expires a skill effect only when the exact timer instance is still the
	 * current lease for {@code skillId}. Periodic callbacks use this path;
	 * administrative and gameplay removals keep using
	 * {@link #removeSkillEffect(int)}.
	 *
	 * @return {@code true} when the expected timer was current and removed
	 */
	public boolean removeSkillEffect(final int skillId,
			final L1SkillTimer expectedTimer) {
		if (expectedTimer == null) {
			return false;
		}
		final boolean removed;
		_skillEffectLock.lock();
		try {
			final L1SkillTimer currentTimer = _skillEffect.get(skillId);
			removed = currentTimer == expectedTimer;
			if (removed) {
				_skillEffect.remove(skillId);
				publishSkillEffectSnapshotLocked();
			}
		} finally {
			_skillEffectLock.unlock();
		}
		if (!removed) {
			return false;
		}
		expectedTimer.end();
		if (this instanceof L1PcInstance pc) {
			pc.applyTalentBuffAbility();
		}
		return true;
	}

	/**
	 * 刪除全部技能效果
	 */
	public void clearAllSkill() {
		final List<L1SkillTimer> timers;
		_skillEffectLock.lock();
		try {
			timers = List.copyOf(_skillEffect.values());
			_skillEffect.clear();
			publishSkillEffectSnapshotLocked();
		} finally {
			_skillEffectLock.unlock();
		}
		for (final L1SkillTimer timer : timers) {
			if (timer != null) {
				timer.end();
			}
		}
		if (this instanceof L1PcInstance pc) {
			pc.applyTalentBuffAbility();
		}
	}
	
	/**
	 * 指定技能效果消除
	 *
	 * @param skillId 技能編號
	 */
	public void killSkillEffectTimer(final int skillId) {
		final L1SkillTimer timer;
		_skillEffectLock.lock();
		try {
			timer = _skillEffect.remove(skillId);
			if (timer != null) {
				publishSkillEffectSnapshotLocked();
			}
		} finally {
			_skillEffectLock.unlock();
		}
		if (timer != null) {
			timer.kill();
		}
		
		// [天賦系統] 當強制移除 Buff 時，若對象是玩家則重算狀態天賦能力
		if (this instanceof com.lineage.server.model.Instance.L1PcInstance pc) {
			pc.applyTalentBuffAbility();
		}
	}

	/** Returns the exact current timer lease for deferred, identity-safe use. */
	public L1SkillTimer captureSkillEffectLease(final int skillId) {
		_skillEffectLock.lock();
		try {
			return _skillEffect.get(skillId);
		} finally {
			_skillEffectLock.unlock();
		}
	}

	/**
	 * Kills a skill only when the captured timer is still current.  Deferred
	 * combat effects use this to avoid consuming a rapid reapplication (ABA).
	 */
	public boolean killSkillEffectTimer(final int skillId,
			final L1SkillTimer expectedTimer) {
		if (expectedTimer == null) {
			return false;
		}
		final boolean removed;
		_skillEffectLock.lock();
		try {
			removed = _skillEffect.remove(skillId, expectedTimer);
			if (removed) {
				publishSkillEffectSnapshotLocked();
			}
		} finally {
			_skillEffectLock.unlock();
		}
		if (!removed) {
			return false;
		}
		expectedTimer.kill();
		if (this instanceof L1PcInstance pc) {
			pc.applyTalentBuffAbility();
		}
		return true;
	}

	/**
	 * 刪除全部技能效果
	 */
	public void clearSkillEffectTimer() {
		final List<L1SkillTimer> timers;
		_skillEffectLock.lock();
		try {
			timers = List.copyOf(_skillEffect.values());
			_skillEffect.clear();
			publishSkillEffectSnapshotLocked();
		} finally {
			_skillEffectLock.unlock();
		}
		for (final L1SkillTimer timer : timers) {
			if (timer != null) {
				timer.kill();
			}
		}
		if (this instanceof L1PcInstance pc) {
			pc.applyTalentBuffAbility();
		}
	}

	/**
	 * 是否有該技能效果
	 *
	 * @param skillId
	 * @return 有true 無false。
	 */
	public boolean hasSkillEffect(final int skillId) {
		return _skillEffectSnapshot.activeSkillIds.contains(skillId);
	}
	
	/**
	 * 該物件目前具有的技能編號
	 * @return
	 */
	public Set<Integer> getSkillEffect() {
		return new HashSet<>(_skillEffectSnapshot.activeSkillIds);
	}
	
	/**
	 * 該物件目前具有技能狀態
	 * @return true:沒有 false:有
	 */
	public boolean getSkillisEmpty() {
		return _skillEffectSnapshot.activeSkillIds.isEmpty();
	}

	/**
	 * 技能效果剩餘時間
	 *
	 * @param skillId
	 * @return 剩餘秒數 無時間限制傳回-1
	 */
	public int getSkillEffectTimeSec(final int skillId) {
		_skillEffectLock.lock();
		try {
			final L1SkillTimer timer = _skillEffect.get(skillId);
			if (timer == null) {
				return -1;
			}
			return timer.getRemainingTime();
		} finally {
			_skillEffectLock.unlock();
		}
	}

	/**
	 * 依目前 {@link #_skillEffect} 重算戰鬥派生快取（命中加總、受物理傷害 SKD3 修正）。<br>
	 * 於技能增刪時由對應的狀態轉換自動呼叫。
	 */
	public void updateCombatSkillCache() {
		_skillEffectLock.lock();
		try {
			publishSkillEffectSnapshotLocked();
		} finally {
			_skillEffectLock.unlock();
		}
	}

	private void publishSkillEffectSnapshotLocked() {
		final Set<Integer> activeSkillIds = Set.copyOf(_skillEffect.keySet());
		L1AttackList.CombatSkillDerived derived = _skillEffectSnapshot.combat;
		try {
			derived = L1AttackList.CombatSkillDerived.compute(activeSkillIds);
		} catch (final RuntimeException e) {
			_log.error(
					"重算戰鬥技能快取失敗 objId={} name={} type={} skillEffectSize={}",
					getId(), getName(), getClass().getSimpleName(), activeSkillIds.size(), e);
		}

		// Keep the protected legacy fields visible for source/binary-compatible
		// subclasses.  In-tree combat readers use the single snapshot below.
		this._combatMeleeHitBonus = derived.meleeHitBonus();
		this._combatRangedHitBonus = derived.rangedHitBonus();
		this._combatMeleeCookingDmgBonus = derived.meleeCookingDmgBonus();
		this._combatRangedCookingDmgBonus = derived.rangedCookingDmgBonus();
		this._combatDmgBonus = derived.skd3FlatModifier();
		this._combatDmgHalve = derived.skd3HalveDamage();
		this._skillEffectSnapshot = new SkillEffectSnapshot(activeSkillIds, derived);
	}

	/**
	 * 戰鬥命中加總（料理 SKU1／SKU2），遠程含弓(20)、鐵手甲(62)時傳 true。
	 */
	public int getCombatHitBonus(final boolean rangedWeapon) {
		final L1AttackList.CombatSkillDerived combat = _skillEffectSnapshot.combat;
		return rangedWeapon ? combat.rangedHitBonus() : combat.meleeHitBonus();
	}

	/**
	 * 技能對武器追加傷害加總（料理 SKD1／SKD2），遠程武器時傳 true。
	 */
	public int getCombatCookingDmgBonus(final boolean rangedWeapon) {
		final L1AttackList.CombatSkillDerived combat = _skillEffectSnapshot.combat;
		return rangedWeapon ? combat.rangedCookingDmgBonus() : combat.meleeCookingDmgBonus();
	}

	/**
	 * 受物理傷害時的 SKD3 加總修正（語意同舊版迴圈 {@code dmg += integer}）。
	 */
	public int getCombatDmgBonus() {
		return _skillEffectSnapshot.combat.skd3FlatModifier();
	}

	/**
	 * 受物理傷害是否需除 2（聖結界等，對應舊版 SKD3 {@code integer.equals(key)}）。
	 */
	public boolean isCombatDmgHalve() {
		return _skillEffectSnapshot.combat.skd3HalveDamage();
	}

	/**
	 * 是否處於 {@link L1AttackList#SKM0} 所定義之「物理傷害視為 0」狀態（O(1) 查詢）。
	 */
	public boolean isSkillEffectPhysicalDamageZero() {
		final Set<Integer> activeSkillIds = _skillEffectSnapshot.activeSkillIds;
		if (activeSkillIds.isEmpty()) {
			return false;
		}
		return activeSkillIds.contains(STATUS_FREEZE)
				|| activeSkillIds.contains(ABSOLUTE_BARRIER)
				|| activeSkillIds.contains(ICE_LANCE)
				|| activeSkillIds.contains(FREEZING_BLIZZARD)
				|| activeSkillIds.contains(FREEZING_BREATH)
				|| activeSkillIds.contains(EARTH_BIND);
	}

	/**
	 * 技能共用延遲鎖。C_UseSkill 會將施法丟到工作執行緒，延遲計時器又在
	 * scheduler 執行，因此不能使用普通 boolean 做跨執行緒的「檢查後設定」。
	 */
	private static final Object LEGACY_SKILL_DELAY_TOKEN = new Object();
	private final AtomicReference<Object> _skillDelayToken = new AtomicReference<>();

	/**
	 * 設定技能施放延遲中
	 * @param flag true:是 false:否
	 */
	public void setSkillDelay(final boolean flag) {
		if (flag) {
			_skillDelayToken.compareAndSet(null, LEGACY_SKILL_DELAY_TOKEN);
		} else {
			// Compatibility callers may release only the compatibility lease; they
			// must never erase an exact-token cast owned by L1SkillDelay.
			_skillDelayToken.compareAndSet(LEGACY_SKILL_DELAY_TOKEN, null);
		}
	}

	/**
	 * 原子取得技能延遲鎖。
	 *
	 * @return true 表示本次施法取得鎖；false 表示已有技能正在 CD
	 */
	public boolean trySetSkillDelay() {
		return _skillDelayToken.compareAndSet(null, LEGACY_SKILL_DELAY_TOKEN);
	}

	/** Exact-owner lease acquisition used by L1SkillDelay. */
	public boolean trySetSkillDelay(final Object token) {
		return token != null && _skillDelayToken.compareAndSet(null, token);
	}

	/** A stale timer/cancellation can clear only the lease it originally owned. */
	public boolean clearSkillDelay(final Object token) {
		return token != null && _skillDelayToken.compareAndSet(token, null);
	}

	/**
	 * 是否在技能施放延遲中
	 * @return true:是 false:否
	 */
	public boolean isSkillDelay() {
		return _skillDelayToken.get() != null;
	}

	/** 角色施法動作可再開始的 monotonic nano time，與技能 reuseDelay 無關。 */
	private final AtomicLong _skillActionReadyAt = new AtomicLong(0L);

	/**
	 * 取得一次施法動作時間窗。只用來防止客戶端動作重疊，不代表技能 CD。
	 */
	public boolean trySetSkillActionDelay(final int delayMillis) {
		if (delayMillis <= 0) {
			return true;
		}
		// A player has one action lane for attacks and spells. Keep the legacy
		// character-local lane only for NPC callers.
		if (this instanceof L1PcInstance pc) {
			return pc.getClientActionTimeline().tryReserveSkillAction(delayMillis) != null;
		}
		final long now = System.nanoTime();
		final long delayNanos = java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(delayMillis);
		while (true) {
			final long readyAt = _skillActionReadyAt.get();
			if (readyAt > now) {
				return false;
			}
			final long nextReadyAt = now > Long.MAX_VALUE - delayNanos
					? Long.MAX_VALUE : now + delayNanos;
			if (_skillActionReadyAt.compareAndSet(readyAt, nextReadyAt)) {
				return true;
			}
		}
	}

	/**
	 * 物件使用延遲編號設置
	 *
	 * @param delayId 延遲編號
	 * @param timer 時間(毫秒)
	 */
	public void addItemDelay(final int delayId, final L1ItemDelay.ItemDelayTimer timer) {
		_itemdelay.put(delayId, timer);
	}

	/** Atomically combines packet/server cooldown sources instead of last-put-wins. */
	public L1ItemDelay.ItemDelayTimer installOrMergeItemDelay(final int delayId,
			final L1ItemDelay.ItemDelayTimer candidate) {
		if (candidate == null) {
			throw new NullPointerException("candidate");
		}
		return _itemdelay.compute(delayId,
				(ignored, current) -> current == null ? candidate : current.merge(candidate));
	}

	/**
	 * 物件使用延遲編號移除
	 *
	 * @param delayId 延遲編號
	 */
	public void removeItemDelay(final int delayId) {
		_itemdelay.remove(delayId);
	}

	/**
	 * Removes an item-delay lease only when the exact timer is still current.
	 * Timer expiry and scheduling-rejection cleanup must use this overload so an
	 * old callback cannot remove a replacement that reuses the same delay ID.
	 *
	 * @return {@code true} only when {@code expectedTimer} was current and removed
	 */
	public boolean removeItemDelay(final int delayId,
			final L1ItemDelay.ItemDelayTimer expectedTimer) {
		if (expectedTimer == null) {
			return false;
		}
		return _itemdelay.remove(delayId, expectedTimer);
	}

	/**
	 * 是否為延遲使用的物件
	 *
	 * @param delayId 延遲編號
	 * @return  true:是 false:否
	 */
	public boolean hasItemDelay(final int delayId) {
		final L1ItemDelay.ItemDelayTimer timer = _itemdelay.get(delayId);
		if (timer == null) {
			return false;
		}
		if (!timer.isExpiredForCurrentAction()) {
			return true;
		}
		if (timer.retainIngressLeaseForQueuedPackets()) {
			// Server/AI consumers are open once runtime time passes, but they may not
			// erase the ingress half before an older queued packet checks it.
			return false;
		}
		if (_itemdelay.remove(delayId, timer)) {
			L1ItemDelay.recordLazyExpiry();
		}
		return false;
	}

	/**
	 * 是否為延遲使用的物件
	 *
	 * @param delayId 延遲編號
	 * @return 物件延遲設置
	 */
	public L1ItemDelay.ItemDelayTimer getItemDelayTimer(final int delayId) {
		return _itemdelay.get(delayId);
	}

	/** Lifecycle cleanup for lazy packet-origin cooldown leases. */
	public void clearItemDelays() {
		_itemdelay.clear();
	}

	L1Cube.CubeLease installCubeLease(final int skillId,
			final L1Cube.CubeLease lease) {
		if (lease == null) {
			throw new NullPointerException("lease");
		}
		return _cubeLeases.put(skillId, lease);
	}

	L1Cube.CubeLease getCubeLease(final int skillId) {
		return _cubeLeases.get(skillId);
	}

	boolean isCurrentCubeLease(final int skillId,
			final L1Cube.CubeLease expected) {
		return (expected != null) && (_cubeLeases.get(skillId) == expected);
	}

	boolean removeCubeLease(final int skillId,
			final L1Cube.CubeLease expected) {
		return (expected != null) && _cubeLeases.remove(skillId, expected);
	}

	/**
	 * 加入寵物清單
	 *
	 * @param npc 
	 */
	public void addPet(final L1NpcInstance npc) {
		_petlist.put(npc.getId(), npc);
		// 加入寵物控制介面
		sendPetCtrlMenu(npc, true);
	}

	/**
	 * 移除寵物清單
	 *
	 * @param npc 
	 */
	public void removePet(final L1NpcInstance npc) {
		_petlist.remove(npc.getId());
		// 移除寵物控制介面
		sendPetCtrlMenu(npc, false);
	}

	/**
	 * 傳回寵物控制清單
	 *
	 * @return 
	 */
	public Map<Integer, L1NpcInstance> getPetList() {
		return _petlist;
	}

	/**
	 * 寵物選單控制
	 * 
	 * @param npc
	 * @param type true:顯示 false:關閉
	 */
	private final void sendPetCtrlMenu(L1NpcInstance npc, boolean type) {
		if (npc instanceof L1PetInstance) {
			L1PetInstance pet = (L1PetInstance) npc;
			L1Character cha = pet.getMaster();
			
			if (cha instanceof L1PcInstance) {
				L1PcInstance pc = (L1PcInstance) cha;
				pc.sendPackets(new S_PetCtrlMenu(pc, pet, type));
				
				if (type) {
					pc.sendPackets(new S_HPMeter(pet));
				}
			}
			
		} else if (npc instanceof L1SummonInstance) {
			L1SummonInstance summon = (L1SummonInstance) npc;
			L1Character cha = summon.getMaster();
			
			if (cha instanceof L1PcInstance) {
				L1PcInstance pc = (L1PcInstance) cha;
				pc.sendPackets(new S_PetCtrlMenu(pc, summon, type));
				
				if (type) {
					pc.sendPackets(new S_HPMeter(summon));
				}
			}
		}
	}

	private final Map<Integer, L1DollInstance> _dolls = new ConcurrentHashMap<>();
	
	/**
	 * 設置娃娃
	 * @param doll
	 */
	public void addDoll(final L1DollInstance doll) {
		_dolls.put(doll.getItemObjId(), doll);
	}

	/**
	 * 移除娃娃
	 */
	public void removeDoll(final L1DollInstance doll) {
		if (doll != null) {
			// A delayed expiry from an old instance must never remove a replacement
			// that reused the same item-object slot (card album uses slot zero).
			_dolls.remove(doll.getItemObjId(), doll);
		}
	}

	/**
	 * 移除娃娃
	 * @return 
	 */
	public L1DollInstance getDoll(final int key) {
		return _dolls.get(key);
	}

	/**
	 * 目前攜帶的娃娃
	 * @return 目前攜帶的娃娃
	 */
	public Map<Integer, L1DollInstance> getDolls() {
		return _dolls;
	}

	/**
	 * 加入跟隨者
	 * @param follower 跟隨者
	 */
	public void addFollower(final L1FollowerInstance follower) {
		_followerlist.put(follower.getId(), follower);
	}

	/**
	 * 移除跟隨者
	 * @param follower 跟隨者
	 */
	public void removeFollower(final L1FollowerInstance follower) {
		_followerlist.remove(follower.getId());
	}

	/**
	 * 傳回跟隨者
	 * @return 跟隨者清單
	 */
	public Map<Integer, L1FollowerInstance> getFollowerList() {
		return _followerlist;
	}

	/**
	 * 為角色附加毒效果。
	 *
	 * @param poison
	 *            代表毒的 L1Poison 物件
	 */
	public void setPoison(final L1Poison poison) {
		if (poison == null) {
			curePoison();
			return;
		}
		tryInstallPoison(poison);
	}

	/**
	 * Atomically installs one poison lease. Constructors must not start effects
	 * or timers until this claim succeeds.
	 */
	public boolean tryInstallPoison(final L1Poison poison) {
		return (poison != null) && !(poison instanceof PoisonTransition)
				&& POISON_UPDATER.compareAndSet(this, null, poison);
	}

	/** Returns whether {@code expected} is the exact active poison lease. */
	public boolean isCurrentPoison(final L1Poison expected) {
		return (expected != null) && (this._poison == expected);
	}

	/**
	 * Claims the exact current poison, runs its cleanup without holding a
	 * character lock, then opens the slot for a new infection. The transition
	 * sentinel prevents a new poison from being installed while old effects are
	 * still being rolled back.
	 */
	public L1Poison claimPoisonRemoval(final L1Poison expected) {
		if (expected == null) {
			return null;
		}
		final L1Poison transition = new PoisonTransition(expected);
		return POISON_UPDATER.compareAndSet(this, expected, transition)
				? transition : null;
	}

	/** Completes the exact opaque transition returned by {@link #claimPoisonRemoval}. */
	public boolean completePoisonRemoval(final L1Poison transition) {
		return (transition instanceof PoisonTransition)
				&& POISON_UPDATER.compareAndSet(this, transition, null);
	}

	public boolean removePoisonIfCurrent(final L1Poison expected,
			final Runnable cleanup) {
		if (cleanup == null) {
			return false;
		}
		final L1Poison transition = claimPoisonRemoval(expected);
		if (transition == null) {
			return false;
		}
		try {
			cleanup.run();
		} finally {
			if (!completePoisonRemoval(transition)) {
				_log.error("Poison transition ownership lost: objId={} name={}",
						getId(), getName());
			}
		}
		return true;
	}

	/**
	 * 治療角色身上的毒。
	 */
	public void curePoison() {
		final L1Poison poison = this._poison;
		if (poison == null) {
			return;
		}
		poison.cure();
	}

	/**
	 * 回傳角色中毒狀態。
	 *
	 * @return 代表角色毒的 L1Poison 物件
	 */
	public L1Poison getPoison() {
		final L1Poison poison = this._poison;
		return poison instanceof PoisonTransition ? null : poison;
	}

	/**
	 * 為角色附加毒效果
	 *
	 * @param effectId
	 * @see S_Poison#S_Poison(int, int)
	 */
	public void setPoisonEffect(final int effectId) {
		this.broadcastPacketX8(new S_Poison(this.getId(), effectId));
	}

	/**
	 * 所在位置區域屬性返回
	 * @return 0一般區域 1安全區域 -1戰鬥區域
	 */
	public int getZoneType() {
		if (this.getMap().isSafetyZone(this.getLocation())) {
			return 1;
		} else if (this.getMap().isCombatZone(this.getLocation())) {
			return -1;
		} else { // 一般區域
			return 0;
		}
	}
	
	/**
	 * 位於安全區域中
	 * @return
	 */
	public boolean isSafetyZone() {
		if (this.getMap().isSafetyZone(this.getLocation())) {
			return true;
		}
		return false;
	}
	
	/**
	 * 位於戰鬥區域中
	 * @return
	 */
	public boolean isCombatZone() {
		if (this.getMap().isCombatZone(this.getLocation())) {
			return true;
		}
		return false;
	}
	
	/**
	 * 位於一般區域中
	 * @return
	 */
	public boolean isNoneZone() {
		return getZoneType() == 0;
	}

	private long _exp; // 經驗值

	/**
	 * 回傳角色持有的經驗值。
	 *
	 * @return 經驗值。
	 */
	public long getExp() {
		return this._exp;
	}

	/**
	 * 設定角色擁有的經驗值。
	 *
	 * @param exp
	 *            經驗值
	 */
	public void setExp(final long exp) {
		this._exp = exp;
	}

	// ■■■■■■■■■■ 將移至 L1PcInstance 的屬性 ■■■■■■■■■■
	private final CopyOnWriteArrayList<L1Object> _knownObjects = new CopyOnWriteArrayList<L1Object>();
	private final CopyOnWriteArrayList<L1PcInstance> _knownPlayer = new CopyOnWriteArrayList<L1PcInstance>();
	/** known-list 與 NPC viewer registry 的複合更新必須共用同一把玩家端鎖。 */
	private final ReentrantLock _knownObjectLock = new ReentrantLock();

	/**
	 * 是否為已認識物件
	 *
	 * @param obj 判斷對象
	 * @return true:是 false:不是
	 */
	public boolean knownsObject(final L1Object obj) {
		return this._knownObjects.contains(obj);
	}

	/**
	 * 全部認識物件(L1Object)清單
	 *
	 * @return 全部認識物件(L1Object)清單
	 */
	public List<L1Object> getKnownObjects() {
		return this._knownObjects;
	}

	/**
	 * 全部認識物件(PC)清單
	 *
	 * @return 全部認識物件(PC)清單
	 */
	public List<L1PcInstance> getKnownPlayers() {
		return this._knownPlayer;
	}

	/**
	 * 加入已認識物件
	 *
	 * @param obj 加入對象
	 */
	public void addKnownObject(final L1Object obj) {
		tryAddKnownObject(obj);
	}

	/**
	 * Binary-compatible admission-aware variant used before appearance packets.
	 *
	 * @return false when a deleting/unpublished NPC rejects the viewer
	 */
	public boolean tryAddKnownObject(final L1Object obj) {
		if (obj == null) {
			return false;
		}
		_knownObjectLock.lock();
		try {
			if (this instanceof L1PcInstance pc && obj instanceof L1NpcInstance npc
					&& !npc.registerViewer(pc)) {
				return false;
			}
			if (this._knownObjects.addIfAbsent(obj)) {
				if (obj instanceof L1PcInstance) {
					this._knownPlayer.addIfAbsent((L1PcInstance) obj);
				}
			}
			return true;
		} finally {
			_knownObjectLock.unlock();
		}
	}

	/**
	 * 移出已認識物件
	 *
	 * @param obj 移出對象
	 */
	public void removeKnownObject(final L1Object obj) {
		removeKnownObjectIfPresent(obj);
	}

	/**
	 * 原子移除 known object，回傳這次是否真的持有該物件。
	 * 視野 snapshot 可能已過期；呼叫端只有在 true 時才可補送封包或記錄修復。
	 */
	public boolean removeKnownObjectIfPresent(final L1Object obj) {
		if (obj == null) {
			return false;
		}
		_knownObjectLock.lock();
		try {
			final boolean removed = this._knownObjects.remove(obj);
			if (obj instanceof L1PcInstance) {
				this._knownPlayer.remove(obj);
			}
			if (this instanceof L1PcInstance pc && obj instanceof L1NpcInstance npc) {
				npc.unregisterViewer(pc);
			}
			return removed;
		} finally {
			_knownObjectLock.unlock();
		}
	}

	public boolean isKnownNpcViewerConsistent(final L1NpcInstance npc) {
		if (!(this instanceof L1PcInstance pc) || npc == null) {
			return true;
		}
		_knownObjectLock.lock();
		try {
			return this._knownObjects.contains(npc)
					== npc.isViewerRegistered(pc);
		} finally {
			_knownObjectLock.unlock();
		}
	}

	/**
	 * 全部認識對象移除
	 */
	public void removeAllKnownObjects() {
		_knownObjectLock.lock();
		try {
			if (this instanceof L1PcInstance pc) {
				for (final L1Object known : this._knownObjects) {
					if (known instanceof L1NpcInstance npc) {
						npc.unregisterViewer(pc);
					}
				}
			}
			this._knownObjects.clear();
			this._knownPlayer.clear();
		} finally {
			_knownObjectLock.unlock();
		}
	}

	// ■■■■■■■■■■ 屬性 ■■■■■■■■■■

	private String _name; // ● 名稱

	public String getName() {
		return this._name;
	}

	public void setName(final String s) {
		this._name = s;
	}

	private int _level; // ● 等級
/** 返回人物等級 */
	public synchronized int getLevel() {
		return this._level;
	}

	public synchronized void setLevel(final int level) {
		this._level = level;
	}

	private volatile int _maxHp = 0; // 最大HP量(MOB 1~100000, 其他 1~32767)
	private volatile int _trueMaxHp = 0; // ● 真實 MAX HP

	/**
	 * 最大HP量
	 * @return
	 */
	public int getMaxHp() {
		return (this._maxHp);
	}

	/**
	 * 最大HP
	 * @param hp
	 */
	public void setMaxHp(final int hp) {
		withVitalMutation(() -> {
			final int previousHp = this._currentHp;
			this._trueMaxHp = hp;
			// RO版: 上限 999999, 公版: 上限 32767
			final int maxLimit = com.lineage.config.ConfigClient.isROClient()
					? MAX_HP_MP_RO : MAX_HP_MP_STANDARD;
			this._maxHp = com.lineage.server.utils.RangeInt.ensure(this._trueMaxHp, 1, maxLimit);
			this._currentHp = Math.min(this._currentHp, this._maxHp);
			enqueueCurrentHpCompletionLocked(
					new CurrentHpMutation(previousHp, this._currentHp));
		});
	}

	/**
	 * 增加(減少)HP上限
	 * @param i
	 */
	public void addMaxHp(final int i) {
		withVitalMutation(() -> this.setMaxHp(this._trueMaxHp + i));
	}

	private volatile int _maxMp = 0; // ● ＭＡＸＭＰ（RO版: 0〜999999, 公版: 0〜32767）
	private volatile int _trueMaxMp = 0; // ● 真實 MAX MP

	/**
	 * 最大MP量
	 * @return
	 */
	public int getMaxMp() {
		return (this._maxMp);
	}

	/**
	 * 最大MP
	 * @param mp
	 */
	public void setMaxMp(final int mp) {
		withVitalMutation(() -> {
			final int previousMp = this._currentMp;
			this._trueMaxMp = mp;
			// RO版: 上限 999999, 公版: 上限 32767
			final int maxLimit = com.lineage.config.ConfigClient.isROClient()
					? MAX_HP_MP_RO : MAX_HP_MP_STANDARD;
			this._maxMp = com.lineage.server.utils.RangeInt.ensure(this._trueMaxMp, 0, maxLimit);
			this._currentMp = Math.min(this._currentMp, this._maxMp);
			enqueueCurrentMpCompletionLocked(
					new CurrentMpMutation(previousMp, this._currentMp));
		});
	}

	/**
	 * 增加(減少)MP上限
	 * @param i
	 */
	public void addMaxMp(final int i) {
		withVitalMutation(() -> this.setMaxMp(this._trueMaxMp + i));
	}

	private int _ac = 10; // ● ＡＣ（公版 -211〜44，RO 版放寬）
	private int _trueAc = 0; // ● 真實 AC

	public int getAc() {
		int ac = _ac;
		if (this instanceof L1PcInstance) {
			final L1PcInstance pc = (L1PcInstance) this;
			switch (pc.guardianEncounter()) {
			case 0:// 正義的守護 Lv1
				ac -= 2;
				break;
				
			case 1:// 正義的守護 Lv2
				ac -= 4;
				break;
				
			case 2:// 正義的守護 Lv3
				ac -= 6;
				break;
			}
		}
		return RangeInt.ensure(ac, getMinAcLimit(), 44);
	}

	public void setAc(final int i) {
		this._trueAc = i;
		this._ac = RangeInt.ensure(i, getMinAcLimit(), 44);
	}

	private int getMinAcLimit() {
		return com.lineage.config.ConfigClient.isROClient() ? Integer.MIN_VALUE : -211;
	}

	/**
	 * 增加(減少)防禦力
	 * @param i
	 */
	public void addAc(final int i) {
		this.setAc(this._trueAc + i);
	}

	private short _str = 0; // ● ＳＴＲ（1〜127）
	private short _trueStr = 0; // ● 真實 STR

	/**
	 * 力量
	 * @return
	 */
	public short getStr() {
		return (this._str);
	}

	public void setStr(final int i) {
		this._trueStr = (short) i;
		this._str = (short) RangeInt.ensure(i, 1, 254);
	}

	/**
	 * 增加(減少)力量
	 * @param i
	 */
	public void addStr(final int i) {
		this.setStr(this._trueStr + i);
	}

	private short _con = 0; // ● ＣＯＮ（1〜127）
	private short _trueCon = 0; // ● 真實 CON

	/**
	 * 體質
	 * @return
	 */
	public short getCon() {
		return (this._con);
	}

	public void setCon(final int i) {
		this._trueCon = (short) i;
		this._con = (short) RangeInt.ensure(i, 1, 254);
	}

	/**
	 * 增加(減少)體質
	 * @param i
	 */
	public void addCon(final int i) {
		this.setCon(this._trueCon + i);
	}

	private short _dex = 0; // ● ＤＥＸ（1〜127）
	private short _trueDex = 0; // ● 真實 DEX

	/**
	 * 敏捷
	 * @return
	 */
	public short getDex() {
		return (this._dex);
	}

	public void setDex(final int i) {
		this._trueDex = (short) i;
		this._dex = (short) RangeInt.ensure(i, 1, 254);
	}

	/**
	 * 增加(減少)敏捷
	 * @param i
	 */
	public void addDex(final int i) {
		this.setDex(this._trueDex + i);
	}

	private short _cha = 0; // ● ＣＨＡ（1〜127）
	private short _trueCha = 0; // ● 真實 CHA

	/**
	 * 魅力
	 * @return
	 */
	public short getCha() {
		return (this._cha);
	}

	public void setCha(final int i) {
		this._trueCha = (short) i;
		this._cha = (short) RangeInt.ensure(i, 1, 254);
	}

	/**
	 * 增加(減少)魅力
	 * @param i
	 */
	public void addCha(final int i) {
		this.setCha(this._trueCha + i);
	}

	private short _int = 0; // ● ＩＮＴ（1〜127）
	private short _trueInt = 0; // ● 真實 INT

	/**
	 * 智力
	 * @return
	 */
	public short getInt() {
		return (this._int);
	}

	public void setInt(final int i) {
		this._trueInt = (short) i;
		this._int = (short) RangeInt.ensure(i, 1, 254);
	}

	/**
	 * 增加(減少)智力
	 * @param i
	 */
	public void addInt(final int i) {
		this.setInt(this._trueInt + i);
	}

	private short _wis = 0; // ● ＷＩＳ（1〜127）
	private short _trueWis = 0; // ● 真實 WIS

	/**
	 * 精神
	 * @return
	 */
	public short getWis() {
		return (this._wis);
	}

	public void setWis(final int i) {
		this._trueWis = (short) i;
		this._wis = (short) RangeInt.ensure(i, 1, 254);
	}

	/**
	 * 增加(減少)精神
	 * @param i
	 */
	public void addWis(final int i) {
		this.setWis(this._trueWis + i);
	}

	private int _wind = 0; // ● 風防御（-128〜127）
	private int _trueWind = 0; // ● 真實風抗

	/**
	 * 風屬性
	 * @return
	 */
	public int getWind() {
		return this._wind;
	} // 使用時

	/**
	 * 增加(減少)風屬性
	 * @param i
	 */
	public void addWind(final int i) {
		this._trueWind += i;
		if (this._trueWind >= 127) {
			this._wind = 127;
		} else if (this._trueWind <= -128) {
			this._wind = -128;
		} else {
			this._wind = this._trueWind;
		}
	}

	private int _water = 0; // ● 水防御（-128〜127）
	private int _trueWater = 0; // ● 真實水抗

	/**
	 * 水屬性
	 * @return
	 */
	public int getWater() {
		return this._water;
	} // 使用時

	/**
	 * 增加(減少)水屬性
	 * @param i
	 */
	public void addWater(final int i) {
		this._trueWater += i;
		if (this._trueWater >= 127) {
			this._water = 127;
		} else if (this._trueWater <= -128) {
			this._water = -128;
		} else {
			this._water = this._trueWater;
		}
	}

	private int _fire = 0; // ● 火防御（-128〜127）
	private int _trueFire = 0; // ● 真實火抗

	/**
	 * 火屬性
	 * @return
	 */
	public int getFire() {
		return this._fire;
	} // 使用時

	/**
	 * 增加(減少)火屬性
	 * @param i
	 */
	public void addFire(final int i) {
		this._trueFire += i;
		if (this._trueFire >= 127) {
			this._fire = 127;
		} else if (this._trueFire <= -128) {
			this._fire = -128;
		} else {
			this._fire = this._trueFire;
		}
	}

	private int _earth = 0; // ● 地防御（-128〜127）
	private int _trueEarth = 0; // ● 真實地抗

	/**
	 * 地屬性
	 * @return
	 */
	public int getEarth() {
		return this._earth;
	} // 使用時

	/**
	 * 增加(減少)地屬性
	 * @param i
	 */
	public void addEarth(final int i) {
		this._trueEarth += i;
		if (this._trueEarth >= 127) {
			this._earth = 127;
			
		} else if (this._trueEarth <= -128) {
			this._earth = -128;
			
		} else {
			this._earth = this._trueEarth;
		}
	}

	private int _addAttrKind; // 元素衰弱所減少的屬性種類

	public int getAddAttrKind() {
		return this._addAttrKind;
	}

	public void setAddAttrKind(final int i) {
		this._addAttrKind = i;
	}

	// 暈眩耐性
	private int _registStun = 0;
	private int _trueRegistStun = 0;

	/**
	 * 暈眩耐性
	 * @return
	 */
	public int getRegistStun() {
		return this._registStun;
	} // 使用時

	/**
	 * 暈眩耐性
	 * @param i
	 */
	public void addRegistStun(final int i) {
		this._trueRegistStun += i;
		if (this._trueRegistStun > 127) {
			this._registStun = 127;
		} else if (this._trueRegistStun < -128) {
			this._registStun = -128;
		} else {
			this._registStun = this._trueRegistStun;
		}
	}

	// 石化耐性
	private int _registStone = 0;
	private int _trueRegistStone = 0;

	/**
	 * 石化耐性
	 * @return
	 */
	public int getRegistStone() {
		return this._registStone;
	}

	/**
	 * 石化耐性
	 * @param i
	 */
	public void addRegistStone(final int i) {
		this._trueRegistStone += i;
		if (this._trueRegistStone > 127) {
			this._registStone = 127;
		} else if (this._trueRegistStone < -128) {
			this._registStone = -128;
		} else {
			this._registStone = this._trueRegistStone;
		}
	}

	// 睡眠耐性
	private int _registSleep = 0;
	private int _trueRegistSleep = 0;

	/**
	 * 睡眠耐性
	 * @return
	 */
	public int getRegistSleep() {
		return this._registSleep;
	}

	/**
	 * 睡眠耐性
	 * @param i
	 */
	public void addRegistSleep(final int i) {
		this._trueRegistSleep += i;
		if (this._trueRegistSleep > 127) {
			this._registSleep = 127;
		} else if (this._trueRegistSleep < -128) {
			this._registSleep = -128;
		} else {
			this._registSleep = this._trueRegistSleep;
		}
	}

	// 凍結耐性
	private int _registFreeze = 0;
	private int _trueRegistFreeze = 0;

	/**
	 * 寒冰耐性
	 * @return
	 */
	public int getRegistFreeze() {
		return this._registFreeze;
	}

	/**
	 * 寒冰耐性
	 * @param i
	 */
	public void add_regist_freeze(final int i) {
		this._trueRegistFreeze += i;
		if (this._trueRegistFreeze > 127) {
			this._registFreeze = 127;
		} else if (this._trueRegistFreeze < -128) {
			this._registFreeze = -128;
		} else {
			this._registFreeze = this._trueRegistFreeze;
		}
	}

	// 支撐耐性
	private int _registSustain = 0;
	private int _trueRegistSustain = 0;

	/**
	 * 支撐耐性
	 * @return
	 */
	public int getRegistSustain() {
		return this._registSustain;
	}

	/**
	 * 支撐耐性
	 * @param i
	 */
	public void addRegistSustain(final int i) {
		this._trueRegistSustain += i;
		if (this._trueRegistSustain > 127) {
			this._registSustain = 127;
		} else if (this._trueRegistSustain < -128) {
			this._registSustain = -128;
		} else {
			this._registSustain = this._trueRegistSustain;
		}
	}

	// 暗黑耐性
	private int _registBlind = 0;
	private int _trueRegistBlind = 0;

	/**
	 * 暗黑耐性
	 * @return
	 */
	public int getRegistBlind() {
		return this._registBlind;
	}
	
	/**
	 * 暗黑耐性
	 * @param i
	 */
	public void addRegistBlind(final int i) {
		this._trueRegistBlind += i;
		if (this._trueRegistBlind > 127) {
			this._registBlind = 127;
		} else if (this._trueRegistBlind < -128) {
			this._registBlind = -128;
		} else {
			this._registBlind = this._trueRegistBlind;
		}
	}

	private int _dmgup = 0; // 傷害修正（-128～127）
	private int _trueDmgup = 0; // 實際傷害修正

	/**
	 * 傷害增加
	 * @return
	 */
	public int getDmgup() {
		return this._dmgup;
	} // 使用時

	/**
	 * 傷害增加
	 * @param i
	 */
	public void addDmgup(final int i) {
		this._trueDmgup += i;
		if (this._trueDmgup >= 127) {
			this._dmgup = 127;
		} else if (this._trueDmgup <= -128) {
			this._dmgup = -128;
		} else {
			this._dmgup = this._trueDmgup;
		}
	}

	private int _bowDmgup = 0; // 弓傷害修正（-128～127）
	private int _trueBowDmgup = 0; // 實際弓傷害修正

	/**
	 * 遠距離傷害增加
	 * @return
	 */
	public int getBowDmgup() {
		return this._bowDmgup;
	} // 使用時

	/**
	 * 遠距離傷害增加
	 * @param i
	 */
	public void addBowDmgup(final int i) {
		this._trueBowDmgup += i;
		if (this._trueBowDmgup >= 127) {
			this._bowDmgup = 127;
		} else if (this._trueBowDmgup <= -128) {
			this._bowDmgup = -128;
		} else {
			this._bowDmgup = this._trueBowDmgup;
		}
	}

	private int _hitup = 0; // 命中修正（-128～127）
	private int _trueHitup = 0; // 實際命中修正

	/**
	 * 命中增加
	 * @return
	 */
	public int getHitup() {
		return this._hitup;
	} // 使用時

	/**
	 * 命中增加
	 * @param i
	 */
	public void addHitup(final int i) {
		this._trueHitup += i;
		if (this._trueHitup >= 127) {
			this._hitup = 127;
		} else if (this._trueHitup <= -128) {
			this._hitup = -128;
		} else {
			this._hitup = this._trueHitup;
		}
	}

	private int _bowHitup = 0; // 弓命中修正（-128～127）
	private int _trueBowHitup = 0; // 實際弓命中修正

	/**
	 * 遠距離命中增加
	 * @return
	 */
	public int getBowHitup() {
		return this._bowHitup;
	} // 使用時

	/**
	 * 遠距離命中增加
	 * @param i
	 */
	public void addBowHitup(final int i) {
		this._trueBowHitup += i;
		if (this._trueBowHitup >= 127) {
			this._bowHitup = 127;
		} else if (this._trueBowHitup <= -128) {
			this._bowHitup = -128;
		} else {
			this._bowHitup = this._trueBowHitup;
		}
	}
	
	private int _stunSuccess = 0; // 暈眩成功率
	
	/**
	 * 取得暈眩成功率
	 * @return 暈眩成功率數值
	 */
	public int getStunSuccess() {
		return this._stunSuccess;
	}
	
	/**
	 * 增加暈眩成功率
	 * @param i 暈眩成功率數值
	 */
	public void addStunSuccess(final int i) {
		this._stunSuccess += i;
	}

	private int _mr = 0; // 魔防（0～）
	private int _trueMr = 0; // 實際魔防

	/**
	 * 魔防
	 * @return
	 */
	public int getMr() {
		if (this.hasSkillEffect(ERASE_MAGIC) == true) {// 魔法消除
			return this._mr >> 2;// / 4;
		} else {
			return this._mr;
		}
	} // 使用時

	public int getTrueMr() {
		return this._trueMr;
	} // 設定時

	/**
	 * 魔防
	 * @param i
	 */
	public void addMr(final int i) {
		this._trueMr += i;
		if (this._trueMr <= 0) {
			this._mr = 0;
		} else {
			this._mr = this._trueMr;
		}
	}

	private int _sp = 0; // 增加的 SP

	/**
	 * 魔功
	 * @return
	 */
	public int getSp() {
		return this.getTrueSp() + this._sp;
	}

	/**
	 * 魔功
	 * @return
	 */
	public int getTrueSp() {
		return this.getMagicLevel() + this.getMagicBonus();
	}

	/**
	 * 增加魔功
	 * @param i
	 */
	public void addSp(final int i) {
		this._sp += i;
	}

	private volatile boolean _isDead; // 死亡狀態

	/**
	 * 死亡狀態
	 * @return
	 */
	public boolean isDead() {
		return this._isDead;
	}

	/**
	 * 死亡状態
	 * @param flag
	 */
	public void setDead(final boolean flag) {
		withVitalMutation(() -> {
			final boolean previousDead = this._isDead;
			this._isDead = flag;
			recordDamageDeathMutation(previousDead, flag);
		});
	}

	private volatile int _status; // 初始化狀態

	/**
	 * 初始化狀態
	 * @return
	 */
	public int getStatus() {
		return this._status;
	}

	/**
	 * 初始化狀態
	 * @param i
	 */
	public void setStatus(final int i) {
		withVitalMutation(() -> {
			this._status = i;
		});
	}

	private String _title; // 封號

	/**
	 * 封號
	 * @return
	 */
	public String getTitle() {
		return this._title;
	}

	/**
	 * 封號
	 * @param s
	 */
	public void setTitle(final String s) {
		this._title = s;
	}

	private int _lawful; // 陣營值

	/**
	 * 傳回正義質
	 * @return
	 */
	public int getLawful() {
		return this._lawful;
	}

	/**
	 * 設定正義質
	 * @param i
	 */
	public void setLawful(final int i) {
		this._lawful = i;
	}

	/*public synchronized void addLawful(final int i) {
		this._lawful += i;
		if (this._lawful > 32767) {
			this._lawful = 32767;
			
		} else if (this._lawful < -32768) {
			this._lawful = -32768;
		}
	}*/

	private volatile int _heading; // 面向： 0.左上 1.上 2.右上 3.右 4.右下 5.下 6.左下 7.左

	/**
	 * 面向
	 * @return 0:左上 1:上 2:右上 3:右 4:右下 5:下 6:左下 7:左
	 * 41517276
	 */
	public int getHeading() {
		return this._heading;
	}

	/**
	 * 面向
	 * @param i 0:左上 1:上 2:右上 3:右 4:右下 5:下 6:左下 7:左
	 */
	public void setHeading(final int i) {
		this._heading = i;
	}

	private static final int[] MOVE_HEADING_X = { 0, 1, 1, 1, 0, -1, -1, -1 };

	private static final int[] MOVE_HEADING_Y = { -1, -1, 0, 1, 1, 1, 0, -1 };

	/**
	 * 一次已提交移動的不可變快照。locationRevision 會在任何後續座標／地圖
	 * 變更時失效，讓每條連線在真正寫入 socket 前淘汰舊步行封包。
	 */
	public record MovementSnapshot(int objectId, int sourceX, int sourceY,
			int targetX, int targetY, short mapId, int heading,
			long locationRevision, boolean coherent) {
	}

	/**
	 * 必須在該步座標已提交後、仍持有物件狀態鎖時擷取。
	 */
	public synchronized MovementSnapshot captureCommittedMovement(
			final int sourceX, final int sourceY, final int heading) {
		final int targetX = getX();
		final int targetY = getY();
		final boolean validHeading = heading >= 0 && heading < MOVE_HEADING_X.length;
		final boolean coherent = validHeading
				&& targetX == sourceX + MOVE_HEADING_X[heading]
				&& targetY == sourceY + MOVE_HEADING_Y[heading];
		return new MovementSnapshot(getId(), sourceX, sourceY, targetX, targetY,
				getMapId(), heading, getLocationRevision(), coherent);
	}

	private int _moveSpeed; // 移動加速狀態(綠水)

	/**
	 * 移動加速狀態(綠水)
	 * @return 0:無 1:加速 2:緩速
	 */
	public int getMoveSpeed() {
		return this._moveSpeed;
	}

	/**
	 * 移動加速狀態(綠水)
	 * @param i 0:無 1:加速 2:緩速
	 */
	public void setMoveSpeed(final int i) {
		this._moveSpeed = i;
	}

	private int _braveSpeed; // 攻擊加速狀態(勇水)

	/**
	 * 攻擊加速狀態(勇水)
	 * @return 0:無 1:勇水 5:強化勇水
	 */
	public int getBraveSpeed() {
		return this._braveSpeed;
	}

	/**
	 * 攻擊加速狀態(勇水)
	 * @param i 0:無 1:勇水 5:強化勇水
	 */
	public void setBraveSpeed(final int i) {
		this._braveSpeed = i;
	}

	private int _tempCharGfx; // 顯示外型編號

	/**
	 * 傳回顯示外型編號
	 * @return
	 */
	public int getTempCharGfx() {
		return this._tempCharGfx;
	}

	/**
	 * 設置顯示外型編號
	 * @param i
	 */
	public void setTempCharGfx(final int i) {
		this._tempCharGfx = i;
	}

	private int _gfxid; // 原始外型編號

	/**
	 * 傳回原始外型編號
	 * @return
	 */
	public int getGfxId() {
		return this._gfxid;
	}

	/**
	 * 設置原始外型編號
	 * @param i
	 */
	public void setGfxId(final int i) {
		this._gfxid = i;
	}

	/**
	 * 魔法等級
	 * @return
	 */
	public int getMagicLevel() {
		return this.getLevel() >> 2;// / 4;
	}

	/**
	 * 智力命中魔法追加
	 * @return
	 */
	public int getMagicBonus() {
		switch (this.getInt()) {
		case 0:
		case 1:
		case 2:
		case 3:
		case 4:
		case 5:
			return -2;

		case 6:
		case 7:
		case 8:
			return -1;

		case 9:
		case 10:
		case 11:
			return 0;

		case 12:
		case 13:
		case 14:
			return 1;

		case 15:
		case 16:
		case 17:
			return 2;

		case 18:
			return 3;
		case 19:
			return 4;
		case 20:
			return 5;
		case 21:
			return 6;
		case 22:
			return 7;
		case 23:
			return 8;
		case 24:
			return 9;
		case 25:
		case 26:
		case 27:
			return 10;
		case 28:
		case 29:
		case 30:
			return 11;
		case 31:
		case 32:
		case 33:
			return 12;
		case 34:
		case 35:
		case 36:
			return 13;
		case 37:
		case 38:
		case 39:
			return 14;
		case 40:
		case 41:
		case 42:
		case 43:
			return 15;
		/*case 44:
			return 17;
		case 45:
			return 18;
		case 46:
			return 19;
		case 47:
			return 20;
		case 48:
			return 21;
		case 49:
			return 22;
		case 50:
			return 23;
		case 51:
			return 24;
		case 52:
			return 25;
		case 53:
			return 26;
		case 54:
			return 27;
		case 55:
			return 28;
		case 56:
			return 29;
		case 57:
			return 30;
		case 58:
			return 31;
		case 59:
			return 32;
		case 60:
			return 33;
		case 61:
			return 34;
		case 62:
			return 35;
		case 63:
			return 36;
		case 64:
			return 37;
		case 65:
			return 38;
		case 66:
			return 39;
		case 67:
			return 40;
		case 68:
			return 41;
		case 69:
			return 42;
		case 70:
			return 43;
		case 71:
			return 44;
		case 72:
			return 45;
		case 73:
			return 46;
		case 74:
			return 47;
		case 75:
			return 48;
		case 76:
			return 49;
		case 77:
			return 50;
		case 78:
			return 51;
		case 79:
			return 52;
		case 80:
			return 53;
		case 81:
			return 54;
		case 82:
			return 55;
		case 83:
			return 56;
		case 84:
			return 57;
		case 85:
			return 58;
		case 86:
			return 59;
		case 87:
			return 60;
		case 88:
			return 61;
		case 89:
			return 62;
		case 90:
			return 63;*/
		default:
			return this.getInt() - 28;
		}
	}

	/**
	 * 是否在隱身狀態
	 * @return
	 */
	public boolean isInvisble() {
		return (this.hasSkillEffect(INVISIBILITY) || this.hasSkillEffect(BLIND_HIDING));
	}

	/**
	 * 治療
	 * @param pt 治療值
	 */
	public void healHp(final int pt) {
		adjustCurrentHp(pt);
	}

	private int _karma;

	/**
	 * 回傳角色持有的陣營值。
	 *
	 * @return karma 值。
	 */
	public int getKarma() {
		return this._karma;
	}

	/**
	 * 設定角色擁有的 karma 值。
	 *
	 * @param karma
	 *            陣營值
	 */
	public void setKarma(final int karma) {
		this._karma = karma;
	}

	public void setMr(final int i) {
		this._trueMr = i;
		if (this._trueMr <= 0) {
			this._mr = 0;
		} else {
			this._mr = this._trueMr;
		}
	}

	/**
	 * 光
	 */
	public void turnOnOffLight() {
		int lightSize = 0x00;
		if (this instanceof L1NpcInstance) {
			final L1NpcInstance npc = (L1NpcInstance) this;
			lightSize = npc.getLightSize(); // npc.sql 的光照範圍
		}

		for (final L1ItemInstance item : this.getInventory().getItems()) {
			if ((item.getItem().getType2() == 0) && (item.getItem().getType() == 2)) { // 照明類道具
				final int itemlightSize = item.getItem().getLightRange();
				if ((itemlightSize != 0) && item.isNowLighting()) {
					if (itemlightSize > lightSize) {
						lightSize = itemlightSize;
					}
				}
			}
		}
		// 照明法術
		if (this.hasSkillEffect(LIGHT)) {
			lightSize = 0x0e;
		}

		// 人物
		if (this instanceof L1PcInstance) {
			final L1PcInstance pc = (L1PcInstance) this;
			pc.sendPackets(new S_Light(pc.getId(), lightSize));
		}
		
		if (!this.isInvisble()) {
			// 可見性檢查：如果沒有玩家在範圍內，跳過廣播
			if (!World.get().getVisiblePlayer(this).isEmpty()) {
				this.broadcastPacketAll(new S_Light(this.getId(), lightSize));
			}
		}

		this.setOwnLightSize(lightSize); // S_OwnCharPack 的光照範圍
		this.setChaLightSize(lightSize); // S_OtherCharPack、S_NPCPack 等的光照範圍
	}

	private int _chaLightSize; // 照明範圍

	/**
	 * 物件原始亮度
	 * @return
	 */
	public int getChaLightSize() {
		if (this.isInvisble()) {
			return 0;
		}
		return this._chaLightSize;
	}

	/**
	 * 設置原始亮度
	 * @param i
	 */
	public void setChaLightSize(final int i) {
		this._chaLightSize = i;
	}

	private int _ownLightSize; // 照明範圍（S_OwnCharPack 用）

	/**
	 * 傳回附加亮度
	 * @return
	 */
	public int getOwnLightSize() {
		if (this.isInvisble()) {
			return 0;
		}
		return this._ownLightSize;
	}

	/**
	 * 設置附加亮度
	 * @param i
	 */
	public void setOwnLightSize(final int i) {
		this._ownLightSize = i;
	}

	private int _tmp; // 緩存數據

	/**
	 * 傳出 緩存數據
	 * @return the _tmp
	 */
	public int get_tmp() {
		return _tmp;
	}

	/**
	 * 設置 緩存數據
	 * @param tmp 對 _tmp 進行設置
	 */
	public void set_tmp(int tmp) {
		this._tmp = tmp;
	}

	private int _tmp_mr; // 暫存數據(MR)

	/**
	 * 傳出 暫存數據(MR)
	 * @return the _tmp_mr
	 */
	public int get_tmp_mr() {
		return _tmp_mr;
	}

	/**
	 * 設置 暫存數據(MR)
	 * @param tmp 對 _tmp_mr 進行設置
	 */
	public void set_tmp_mr(int tmp) {
		this._tmp_mr = tmp;
	}

	// 閃避率 +
	private int _dodge_up = 0;

	/**
	 * 閃避增加
	 * @return
	 */
	public int get_dodge() {
		return _dodge_up;
	}

	/**
	 * 閃避增加
	 * @param i
	 */
	public void add_dodge(int i) {
		_dodge_up += i;
		if (_dodge_up > 10) {
			_dodge_up = 10;
			
		} else if (_dodge_up < 0){
			_dodge_up = 0;
		}
	}

	// 閃避率 -
	private int _dodge_down = 0;

	/**
	 * 閃避減少
	 * @return
	 */
	public int get_dodge_down() {
		return _dodge_down;
	}

	/**
	 * 閃避減少
	 * @param i
	 */
	public void add_dodge_down(int i) {
		_dodge_down += i;
		if (_dodge_down > 10) {
			_dodge_down = 10;
			
		} else if (_dodge_down < 0){
			_dodge_down = 0;
		}
	}
	
	// 受到藥水霜化術
	private boolean _decay_potion = false;

	/**
	 * 受到藥水霜化術
	 * @param b
	 */
	public void set_decay_potion(final boolean b) {
		_decay_potion = b;
	}
	
	/**
	 * 受到藥水霜化術
	 * @return
	 */
	public boolean is_decay_potion() {
		return _decay_potion;
	}
}
