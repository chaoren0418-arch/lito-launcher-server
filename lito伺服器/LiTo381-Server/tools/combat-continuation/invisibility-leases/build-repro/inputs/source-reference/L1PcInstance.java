package com.lineage.server.model.Instance;

import static com.lineage.server.model.skill.L1SkillId.ADDITIONAL_FIRE;
import static com.lineage.server.model.skill.L1SkillId.ADLV80_1;
import static com.lineage.server.model.skill.L1SkillId.ADLV80_2;
import static com.lineage.server.model.skill.L1SkillId.BLIND_HIDING;
import static com.lineage.server.model.skill.L1SkillId.BLOODLUST;
import static com.lineage.server.model.skill.L1SkillId.BONE_BREAK;
import static com.lineage.server.model.skill.L1SkillId.CANCELLATION;
import static com.lineage.server.model.skill.L1SkillId.DECREASE_WEIGHT;
import static com.lineage.server.model.skill.L1SkillId.DRESS_EVASION;
import static com.lineage.server.model.skill.L1SkillId.EARTH_BIND;
import static com.lineage.server.model.skill.L1SkillId.ENTANGLE;
import static com.lineage.server.model.skill.L1SkillId.EXOTIC_VITALIZE;
import static com.lineage.server.model.skill.L1SkillId.FOG_OF_SLEEPING;
import static com.lineage.server.model.skill.L1SkillId.FREEZING_BLIZZARD;
import static com.lineage.server.model.skill.L1SkillId.GREATER_HASTE;
import static com.lineage.server.model.skill.L1SkillId.HASTE;
import static com.lineage.server.model.skill.L1SkillId.HOLY_WALK;
import static com.lineage.server.model.skill.L1SkillId.ICE_LANCE;
import static com.lineage.server.model.skill.L1SkillId.ILLUSION_AVATAR;
import static com.lineage.server.model.skill.L1SkillId.INVISIBILITY;
import static com.lineage.server.model.skill.L1SkillId.JOY_OF_PAIN;
import static com.lineage.server.model.skill.L1SkillId.MASS_SLOW;
import static com.lineage.server.model.skill.L1SkillId.MORTAL_BODY;
import static com.lineage.server.model.skill.L1SkillId.MOVING_ACCELERATION;
import static com.lineage.server.model.skill.L1SkillId.SHAPE_CHANGE;
import static com.lineage.server.model.skill.L1SkillId.SHOCK_STUN;
import static com.lineage.server.model.skill.L1SkillId.SLOW;
import static com.lineage.server.model.skill.L1SkillId.SOLID_CARRIAGE;
import static com.lineage.server.model.skill.L1SkillId.STATUS_BRAVE;
import static com.lineage.server.model.skill.L1SkillId.STATUS_BRAVE3;
import static com.lineage.server.model.skill.L1SkillId.STATUS_CHAT_PROHIBITED;
import static com.lineage.server.model.skill.L1SkillId.STATUS_ELFBRAVE;
import static com.lineage.server.model.skill.L1SkillId.STATUS_HASTE;
import static com.lineage.server.model.skill.L1SkillId.STATUS_POISON_PARALYZED;
import static com.lineage.server.model.skill.L1SkillId.STATUS_RIBRAVE;
import static com.lineage.server.model.skill.L1SkillId.STRIKER_GALE;
import static com.lineage.server.model.skill.L1SkillId.WIND_WALK;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lineage.config.Config;
import com.lineage.config.ConfigAlt;
import com.lineage.config.ConfigCharSetting;
import com.lineage.config.ConfigMsg;
import com.lineage.config.ConfigOther;
import com.lineage.config.ConfigRate;
import com.lineage.config.ConfigSkill;
import com.lineage.data.event.OnlineGiftSet;
import com.lineage.data.quest.Chapter01R;
import com.lineage.echo.ClientExecutor;
import com.lineage.echo.EncryptExecutor;
import com.lineage.server.ActionCodes;
import com.lineage.server.clientpackets.AcceleratorChecker;
import com.lineage.server.datatables.DeathPenaltyItemControlTable;
import com.lineage.server.datatables.ExpTable;
import com.lineage.server.datatables.MapLevelTable;
import com.lineage.server.datatables.SkillsTable;
import com.lineage.server.datatables.SprTable;
import com.lineage.server.datatables.user.AutoHuntTable;
import com.lineage.server.datatables.user.CharBuffTable;
import com.lineage.server.datatables.user.CharMapsTimeTable;
import com.lineage.server.datatables.user.CharSkillTable;
import com.lineage.server.datatables.user.CharacterTable;
import com.lineage.server.model.L1ActionPc;
import com.lineage.server.model.L1ActionPet;
import com.lineage.server.model.L1ActionSummon;
import com.lineage.server.model.L1CastleLocation;
import com.lineage.server.model.L1Character;
import com.lineage.server.model.L1ChatParty;
import com.lineage.server.model.L1Clan;
import com.lineage.server.model.ClientActionTimeline;
import com.lineage.server.model.ClientAttackDeferral;
import com.lineage.server.model.DollPeriodicState;
import com.lineage.server.model.L1DwarfForChaInventory;
import com.lineage.server.model.L1DwarfForElfInventory;
import com.lineage.server.model.L1DwarfInventory;
import com.lineage.server.model.L1EquipmentSlot;
import com.lineage.server.model.L1ExcludingList;
import com.lineage.server.model.L1Karma;
import com.lineage.server.model.L1Magic;
import com.lineage.server.model.L1Object;
import com.lineage.server.model.L1Party;
import com.lineage.server.model.L1PcInventory;
import com.lineage.server.model.L1PcQuest;
import com.lineage.server.model.L1PinkName;
import com.lineage.server.model.L1PolyMorph;
import com.lineage.server.model.PlayerActionLock;
import com.lineage.server.model.PlayerAutoActionGate;
import com.lineage.server.model.PlayerPositionSyncGate;
import com.lineage.server.model.L1Teleport;
import com.lineage.server.model.L1TownLocation;
import com.lineage.server.model.L1War;
import com.lineage.server.model.Instance.pcai.AutoHuntAI;
import com.lineage.server.model.Instance.pcai.L1PcAutoHuntInfo;
import com.lineage.server.model.Instance.pcai.L1PcAutoHuntInfo.AiMode;
import com.lineage.server.model.Instance.pcai.L1PcAutoHuntInfo.AiOwnerToken;
import com.lineage.server.model.classes.L1ClassFeature;
import com.lineage.server.model.combat.CombatSideEffects;
import com.lineage.server.model.combat.CompositeDamageCommitPlan;
import com.lineage.server.model.combat.DamageCommit;
import com.lineage.server.model.combat.DamageCommitOutcome;
import com.lineage.server.model.combat.DamageCommitPlan;
import com.lineage.server.model.combat.DamageEvent;
import com.lineage.server.model.combat.LegacyDefenseFeedbackPlan;
import com.lineage.server.model.combat.PcIncomingHitCommitPlan;
import com.lineage.server.model.combat.PcPhysicalAttackFlow;
import com.lineage.server.model.skill.L1SkillTimer;
import com.lineage.server.model.monitor.L1PcInvisDelay;
import com.lineage.server.model.skill.L1SkillUse;
import com.lineage.server.monitor.HitReactionSyncDiagnostics;
import com.lineage.server.serverpackets.S_BlueMessage;
import com.lineage.server.serverpackets.S_Bonusstats;
import com.lineage.server.serverpackets.S_ChangeShape;
import com.lineage.server.serverpackets.S_CharVisualUpdate;
import com.lineage.server.serverpackets.S_DelSkill;
import com.lineage.server.serverpackets.S_DoActionGFX;
import com.lineage.server.serverpackets.S_DoActionShop;
import com.lineage.server.serverpackets.S_Exp;
import com.lineage.server.serverpackets.S_Fishing;
import com.lineage.server.serverpackets.S_HPMeter;
import com.lineage.server.serverpackets.S_HPUpdate;
import com.lineage.server.serverpackets.S_Invis;
import com.lineage.server.serverpackets.S_Karma;
import com.lineage.server.serverpackets.S_Lawful;
import com.lineage.server.serverpackets.S_Liquor;
import com.lineage.server.serverpackets.S_MPUpdate;
import com.lineage.server.serverpackets.S_MapID;
import com.lineage.server.serverpackets.S_NPCTalkReturn;
import com.lineage.server.serverpackets.S_OtherCharPacks;
import com.lineage.server.serverpackets.S_OwnCharAttrDef;
import com.lineage.server.serverpackets.S_OwnCharPack;
import com.lineage.server.serverpackets.S_OwnCharStatus;
import com.lineage.server.serverpackets.S_PacketBox;
import com.lineage.server.serverpackets.S_PacketBoxIcon1;
import com.lineage.server.serverpackets.S_PacketBoxParty;
import com.lineage.server.serverpackets.S_PacketBoxProtection;
import com.lineage.server.serverpackets.S_Poison;
import com.lineage.server.serverpackets.S_Paralysis;
import com.lineage.server.serverpackets.S_RemoveObject;
import com.lineage.server.serverpackets.S_SPMR;
import com.lineage.server.serverpackets.S_ServerMessage;
import com.lineage.server.serverpackets.S_Weather;
import com.lineage.server.serverpackets.ServerBasePacket;
import com.lineage.server.templates.L1PcOther;
import com.lineage.server.templates.L1PcOtherList;
import com.lineage.server.templates.L1PrivateShopBuyList;
import com.lineage.server.templates.L1PrivateShopSellList;
import com.lineage.server.templates.L1Skills;
import com.lineage.server.templates.L1TradeItem;
import com.lineage.server.templates.L1WearingArrowConfig;
import com.lineage.server.thread.GeneralThreadPool;
import com.lineage.server.thread.LogicThreadPool;
import com.lineage.server.timecontroller.server.ServerWarExecutor;
import com.lineage.server.utils.CalcInitHpMp;
import com.lineage.server.utils.CalcStat;
import com.lineage.server.utils.ListMapUtil;
import com.lineage.server.world.World;
import com.lineage.server.world.WorldBroadcast;
import com.lineage.server.world.WorldClan;
import com.lineage.server.world.WorldQuest;
import com.lineage.server.world.WorldWar;

/**
 * 對象:PC 控制項
 * 
 * @author dexc
 * 
 */
public class L1PcInstance extends L1Character {

	private static final Logger _log = LogManager.getLogger(L1PcInstance.class);

	private static final AtomicLong ASYNC_SUBMIT_REJECTED_COUNT = new AtomicLong();

	private static final AtomicLong ASYNC_SUBMIT_ERROR_COUNT = new AtomicLong();

	private static final AtomicLong SCHEDULE_REJECTED_COUNT = new AtomicLong();

	private static final AtomicLong SCHEDULE_ERROR_COUNT = new AtomicLong();
	private static final AtomicLong DELAYED_SLOW_CLOCK_REJECTED_COUNT = new AtomicLong();
	private static final AtomicLong DELAYED_SLOW_WORKER_REJECTED_COUNT = new AtomicLong();
	private static final AtomicLong DELAYED_SLOW_STALE_COUNT = new AtomicLong();
	private static final AtomicLong DELAYED_SLOW_TERMINAL_COUNT = new AtomicLong();
	private static final AtomicLong DELAYED_SLOW_WORK_ERROR_COUNT = new AtomicLong();
	private static final AtomicLong DELAYED_DEATH_MINIMAL_REJECTED_COUNT = new AtomicLong();
	private static final AtomicLong NPC_VISIBILITY_RACE_REPAIR_COUNT = new AtomicLong();
	private static final AtomicLong NPC_VISIBILITY_LEAK_REPAIR_COUNT = new AtomicLong();
	private static final AtomicLong NPC_VISIBILITY_STALL_REPAIR_COUNT = new AtomicLong();
	private static final AtomicLong ONLINE_PACKET_NO_CONNECTION_COUNT = new AtomicLong();
	private static final AtomicLong EXPECTED_DETACHED_PACKET_COUNT = new AtomicLong();
	private static final int DELAYED_SLOW_MAX_ATTEMPTS = 40;
	private static final int DELAYED_TERMINAL_MAX_ATTEMPTS = 8;

	private static final int PC_DEATH_HANDOFF_CAPACITY = Math.max(16, Math.min(16_384,
			Integer.getInteger("lineage.pcDeath.handoffCapacity", 1_024)));
	private static final int PC_DEATH_HANDOFF_MAX_ATTEMPTS = Math.max(2, Math.min(1_000,
			Integer.getInteger("lineage.pcDeath.handoffMaxAttempts", 40)));
	private static final long PC_DEATH_HANDOFF_MAX_AGE_MILLIS = Math.max(100L, Math.min(300_000L,
			Long.getLong("lineage.pcDeath.handoffMaxAgeMillis", 30_000L)));
	private static final PcDeathHandoffQueue PC_DEATH_HANDOFFS = new PcDeathHandoffQueue(
			new PcDeathHandoffQueue.Dispatcher() {
				@Override
				public void executeSlow(final Runnable task) {
					// Bypass GeneralThreadPool.execute()'s packet-lane compatibility inline
					// path: full player death must never caller-run on Netty/DamageCommit.
					GeneralThreadPool.getExecutor().execute(task);
				}

				@Override
				public void schedulePc(final Runnable task, final long delayMillis) {
					if (pcScheduleTask("PcDeathHandoff", task, delayMillis) == null) {
						throw new RejectedExecutionException("PcScheduler did not accept player death continuation");
					}
				}

				@Override
				public void executeTerminal(final Runnable task) {
					if (!LogicThreadPool.get().tryExecute(task)) {
						throw new RejectedExecutionException(
								"Logic workers did not accept player death terminalization");
					}
				}
			},
			PC_DEATH_HANDOFF_CAPACITY, 32, PC_DEATH_HANDOFF_MAX_ATTEMPTS,
			PC_DEATH_HANDOFF_MAX_AGE_MILLIS, 300L);

	public static String getMonitorLine() {
		final PcDeathHandoffQueue.Metrics death = PC_DEATH_HANDOFFS.metrics();
		return "L1PcInstance"
				+ " asyncSubmitRejected=" + ASYNC_SUBMIT_REJECTED_COUNT.get()
				+ " asyncSubmitError=" + ASYNC_SUBMIT_ERROR_COUNT.get()
				+ " scheduleRejected=" + SCHEDULE_REJECTED_COUNT.get()
				+ " scheduleError=" + SCHEDULE_ERROR_COUNT.get()
				+ " delayedClockRejected=" + DELAYED_SLOW_CLOCK_REJECTED_COUNT.get()
				+ " delayedWorkerRejected=" + DELAYED_SLOW_WORKER_REJECTED_COUNT.get()
				+ " delayedStale=" + DELAYED_SLOW_STALE_COUNT.get()
				+ " delayedTerminal=" + DELAYED_SLOW_TERMINAL_COUNT.get()
				+ " delayedWorkError=" + DELAYED_SLOW_WORK_ERROR_COUNT.get()
				+ " delayedMinimalRejected=" + DELAYED_DEATH_MINIMAL_REJECTED_COUNT.get()
				+ " onlinePacketNoConnection=" + ONLINE_PACKET_NO_CONNECTION_COUNT.get()
				+ " expectedDetachedPacket=" + EXPECTED_DETACHED_PACKET_COUNT.get()
				+ " visibilityRaceRepair=" + NPC_VISIBILITY_RACE_REPAIR_COUNT.get()
				+ " visibilityLeakRepair=" + NPC_VISIBILITY_LEAK_REPAIR_COUNT.get()
				+ " visibilityStallRepair=" + NPC_VISIBILITY_STALL_REPAIR_COUNT.get()
				+ " deathPending=" + death.pending
				+ " deathOpen=" + death.open
				+ " deathTerminal=" + death.terminalOpen
				+ " deathFallback=" + death.fallback
				+ " deathTerminalDrop=" + death.terminalDropped;
	}

	/** Invoke before PcScheduler/SlowPool shutdown. */
	public static void shutdownDeathHandoffs() {
		PC_DEATH_HANDOFFS.shutdownAndFallback();
	}

	public static int getDeathHandoffPendingCount() {
		return PC_DEATH_HANDOFFS.metrics().pending;
	}

	public static long getDeathHandoffFallbackCount() {
		return PC_DEATH_HANDOFFS.metrics().fallback;
	}

	public static long getDeathHandoffOverflowCount() {
		return PC_DEATH_HANDOFFS.metrics().overflow;
	}

	public static long getDeathHandoffTimerFailureCount() {
		return PC_DEATH_HANDOFFS.metrics().timerFailure;
	}

	public static long getDeathHandoffTerminalDropCount() {
		return PC_DEATH_HANDOFFS.metrics().terminalDropped;
	}

	public static long getNpcVisibilityRaceRepairCount() {
		return NPC_VISIBILITY_RACE_REPAIR_COUNT.get();
	}

	public static long getNpcVisibilityLeakRepairCount() {
		return NPC_VISIBILITY_LEAK_REPAIR_COUNT.get();
	}

	public static long getNpcVisibilityStallRepairCount() {
		return NPC_VISIBILITY_STALL_REPAIR_COUNT.get();
	}

	public static long getNpcVisibilityFailureCount() {
		return NPC_VISIBILITY_LEAK_REPAIR_COUNT.get()
				+ NPC_VISIBILITY_STALL_REPAIR_COUNT.get();
	}

	private static boolean executePcTask(final String name, final Runnable task) {
		try {
			GeneralThreadPool.get().execute(task);
			return true;
		} catch (final RejectedExecutionException e) {
			ASYNC_SUBMIT_REJECTED_COUNT.incrementAndGet();
			_log.warn("[L1PcInstance] async task rejected: name={}", name, e);
		} catch (final RuntimeException e) {
			ASYNC_SUBMIT_ERROR_COUNT.incrementAndGet();
			_log.warn("[L1PcInstance] async task submit failed: name={}", name, e);
		}
		return false;
	}

	private static ScheduledFuture<?> pcScheduleTask(final String name, final Runnable task, final long delayMs) {
		try {
			final ScheduledFuture<?> future = GeneralThreadPool.get().pcSchedule(task, delayMs);
			if (future != null) {
				return future;
			}
			SCHEDULE_ERROR_COUNT.incrementAndGet();
			_log.warn("[L1PcInstance] pcSchedule returned null: name={} delayMs={}", name, delayMs);
		} catch (final RejectedExecutionException e) {
			SCHEDULE_REJECTED_COUNT.incrementAndGet();
			_log.warn("[L1PcInstance] pcSchedule rejected: name={} delayMs={}", name, delayMs, e);
		} catch (final RuntimeException e) {
			SCHEDULE_ERROR_COUNT.incrementAndGet();
			_log.warn("[L1PcInstance] pcSchedule failed: name={} delayMs={}", name, delayMs, e);
		}
		return null;
	}

	@FunctionalInterface
	private interface DelayedTerminalHandler {
		void onTerminal(String reason, Throwable failure);
	}

	/** Builds a one-shot clock-to-SlowPool handoff with no compatibility caller-run path. */
	private static DelayedSlowHandoff delayedSlowHandoff(final String name,
			final BooleanSupplier current, final Runnable work, final Runnable cleanup,
			final DelayedTerminalHandler terminalHandler) {
		return new DelayedSlowHandoff(name, current, work, cleanup,
				(callback, delayMillis) -> pcScheduleTask(
						name + "Clock", callback, delayMillis) != null,
				GeneralThreadPool.getExecutor()::execute,
				new DelayedSlowHandoff.Listener() {
					@Override
					public void onClockRejected(final String taskName, final Throwable failure) {
						DELAYED_SLOW_CLOCK_REJECTED_COUNT.incrementAndGet();
						_log.warn("[L1PcInstance] delayed clock unavailable: task={}", taskName, failure);
					}

					@Override
					public void onWorkerRejected(final String taskName, final int attempt,
							final Throwable failure) {
						final long rejected = DELAYED_SLOW_WORKER_REJECTED_COUNT.incrementAndGet();
						if (rejected == 1L || (rejected & (rejected - 1L)) == 0L) {
							_log.warn("[L1PcInstance] delayed SlowPool pressure: task={} attempt={} rejected={}",
									taskName, attempt, rejected, failure);
						}
					}

					@Override
					public void onStale(final String taskName) {
						DELAYED_SLOW_STALE_COUNT.incrementAndGet();
					}

					@Override
					public void onTerminalFailure(final String taskName, final String reason,
							final Throwable failure) {
						DELAYED_SLOW_TERMINAL_COUNT.incrementAndGet();
						_log.error("[L1PcInstance] delayed work terminal failure: task={} reason={}",
								taskName, reason, failure);
						if (terminalHandler != null) {
							terminalHandler.onTerminal(reason, failure);
						}
					}

					@Override
					public void onWorkFailure(final String taskName, final Throwable failure) {
						DELAYED_SLOW_WORK_ERROR_COUNT.incrementAndGet();
						_log.error("[L1PcInstance] delayed accepted work failed: task={}",
								taskName, failure);
					}
				}, DELAYED_SLOW_MAX_ATTEMPTS, 25L, 1_000L);
	}

	private static final long serialVersionUID = 1L;

	private final ClientActionTimeline _clientActionTimeline = new ClientActionTimeline();
	private final ClientAttackDeferral _clientAttackDeferral = new ClientAttackDeferral();
	private final PlayerPositionSyncGate _positionSyncGate = new PlayerPositionSyncGate();
	private final ReentrantLock _playerActionLock = new ReentrantLock();
	private final ReentrantLock _expMutationLock = new ReentrantLock();
	private final ReentrantLock _lawfulMutationLock = new ReentrantLock();
	private final ReentrantLock _contributionMutationLock = new ReentrantLock();
	private final AtomicLong _deathPipelineGeneration = new AtomicLong();
	private final DeathLifecycleGate _deathLifecycle = new DeathLifecycleGate();

	public ClientActionTimeline getClientActionTimeline() {
		return _clientActionTimeline;
	}

	public ClientAttackDeferral getClientAttackDeferral() {
		return _clientAttackDeferral;
	}

	public PlayerPositionSyncGate getPositionSyncGate() {
		return _positionSyncGate;
	}

	public ReentrantLock getPlayerActionLock() {
		return _playerActionLock;
	}

	private String clanNote = "";// 血盟成员备注

	/** 返回成员血盟备注 */
	public String getClanNote() {
		return clanNote;
	}

	public void setClanNote(String clanNote) {
		this.clanNote = clanNote;
	}

	// 5種星盤，每種最多 32 個節點 (Type 1 有 32 個)
	private int[][] _astrolabeNodes = new int[5][32];

	public int[][] getAstrolabeNodes() {
		return _astrolabeNodes;
	}

	public void setAstrolabeNodes(int[][] astrolabeNodes) {
		if (astrolabeNodes != null && astrolabeNodes.length == 5) {
			this._astrolabeNodes = astrolabeNodes;
		}
	}

	/** 騎士(男) */
	public static final int CLASSID_KNIGHT_MALE = 61;
	/** 騎士(女) */
	public static final int CLASSID_KNIGHT_FEMALE = 48;

	/** 精靈(男) */
	public static final int CLASSID_ELF_MALE = 138;
	/** 精靈(女) */
	public static final int CLASSID_ELF_FEMALE = 37;

	/** 法師(男) */
	public static final int CLASSID_WIZARD_MALE = 734;
	/** 法師(女) */
	public static final int CLASSID_WIZARD_FEMALE = 1186;

	/** 黑妖(男) */
	public static final int CLASSID_DARK_ELF_MALE = 2786;
	/** 黑妖(女) */
	public static final int CLASSID_DARK_ELF_FEMALE = 2796;

	/** 王族(男) */
	public static final int CLASSID_PRINCE = 0;
	/** 王族(女) */
	public static final int CLASSID_PRINCESS = 1;

	/** 龍騎(男) */
	public static final int CLASSID_DRAGON_KNIGHT_MALE = 6658;
	/** 龍騎(女) */
	public static final int CLASSID_DRAGON_KNIGHT_FEMALE = 6661;

	/** 幻術(男) */
	public static final int CLASSID_ILLUSIONIST_MALE = 6671;
	/** 幻術(女) */
	public static final int CLASSID_ILLUSIONIST_FEMALE = 6650;

	/**
	 * 設定原始質資訊
	 */
	public void load_src() {
		_old_exp = getExp();
		_old_lawful = getLawful();
		_old_karma = getKarma();
	}

	private boolean _isKill = false;

	public boolean is_isKill() {
		return _isKill;
	}

	public void set_isKill(boolean _isKill) {
		this._isKill = _isKill;
	}

	private short _hpr = 0;

	private short _trueHpr = 0;

	public short getHpr() {
		return (short) Math.max(0,
				this._hpr + _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.HP_REGEN));
	}

	/**
	 * 增加(減少)HP回復量
	 * 
	 * @param i
	 */
	public void addHpr(final int i) {
		this._trueHpr += i;
		this._hpr = (short) Math.max(0, this._trueHpr);
	}

	private short _mpr = 0;
	private short _trueMpr = 0;

	public short getMpr() {
		return (short) Math.max(0,
				this._mpr + _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.MP_REGEN));
	}

	/**
	 * 增加(減少)MP回復量
	 * 
	 * @param i
	 */
	public void addMpr(final int i) {
		this._trueMpr += i;
		this._mpr = (short) Math.max(0, this._trueMpr);
	}

	private int _adenaRate = 0; // ● 金幣獲取率

	/**
	 * 取得金幣獲取率
	 * 
	 * @return 金幣獲取率百分比
	 */
	public int getAdenaRate() {
		return this._adenaRate + _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.ADENA_RATE);
	}

	/**
	 * 增加(減少)金幣獲取率
	 * 
	 * @param i 百分比數值（例如：10 表示 +10%）
	 */
	public void addAdenaRate(final int i) {
		this._adenaRate += i;
	}

	private int _dropRate = 0; // ● 掉寶獲取率

	/**
	 * 取得掉寶獲取率
	 * 
	 * @return 掉寶獲取率百分比
	 */
	public int getDropRate() {
		return this._dropRate + _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.DROP_RATE);
	}

	/**
	 * 增加(減少)掉寶獲取率
	 * 
	 * @param i 百分比數值（例如：10 表示 +10%）
	 */
	public void addDropRate(final int i) {
		this._dropRate += i;
	}

	public short _originalHpr = 0; // ● 原始CON HPR

	public short getOriginalHpr() {

		return this._originalHpr;
	}

	public short _originalMpr = 0; // ● 原始WIS MPR

	public short getOriginalMpr() {
		return this._originalMpr;
	}

	private boolean _mpRegenActive;
	private boolean _mpReductionActiveByAwake;
	private boolean _hpRegenActive;

	private int _hpRegenType = 0;
	private int _hpRegenState = 4;

	/** 地圖恢復：獨立計時 tick（每秒 +1），達 interval 時觸發 */
	private int _mapRegenTick = 0;
	/** 地圖恢復：上次套用的 mapId，換圖時重置 tick */
	private int _lastMapRecoveryMapId = -1;

	public int getMapRegenTick() {
		return _mapRegenTick;
	}

	public void addMapRegenTick(final int i) {
		_mapRegenTick += i;
	}

	public void resetMapRegenTick() {
		_mapRegenTick = 0;
	}

	public int getLastMapRecoveryMapId() {
		return _lastMapRecoveryMapId;
	}

	public void setLastMapRecoveryMapId(final int mapId) {
		_lastMapRecoveryMapId = mapId;
	}

	public int getHpRegenState() {
		return this._hpRegenState;
	}

	public void set_hpRegenType(final int hpRegenType) {
		this._hpRegenType = hpRegenType;
	}

	public int hpRegenType() {
		return this._hpRegenType;
	}

	/**
	 * HP 恢復門檻（固定 64，與 MP 相同）
	 * <p>
	 * 累加速度：靜止 +4、行走 +2、戰鬥 +1 → 觸發間隔約 16秒 / 32秒 / 64秒
	 */
	private int regenMax() {
		return 64;
	}

	// 遠近傷害減免
	private int _meleeRedux = 0;
	private int _bowRedux = 0;

	// Getter
	public int getMeleeRedux() {
		return _meleeRedux;
	}

	public int getBowRedux() {
		return _bowRedux;
	}

	// Adder (用於裝備穿脫時呼叫)
	public void addMeleeRedux(int i) {
		_meleeRedux += i;
	}

	public void addBowRedux(int i) {
		_bowRedux += i;
	}

	/**
	 * HP回復成立
	 * 
	 * @return
	 */
	public boolean isRegenHp() {
		if (!_hpRegenActive) {
			return false;
		}
		if (hasSkillEffect(EXOTIC_VITALIZE) || hasSkillEffect(ADDITIONAL_FIRE)) {
			return _hpRegenType >= regenMax();
		}
		if (120 <= _inventory.getWeight240()) {
			return false;
		}
		if ((_food < 3)) {
			return false;
		}
		return _hpRegenType >= regenMax();
	}

	private int _mpRegenType = 0;
	private int _mpRegenState = 4;

	public int getMpRegenState() {
		return this._mpRegenState;
	}

	public void set_mpRegenType(final int hpmpRegenType) {
		this._mpRegenType = hpmpRegenType;
	}

	public int mpRegenType() {
		return this._mpRegenType;
	}

	/**
	 * MP回復成立
	 * 
	 * @return
	 */
	public boolean isRegenMp() {
		if (!this._mpRegenActive) {
			return false;
		}
		if (this.hasSkillEffect(EXOTIC_VITALIZE) || this.hasSkillEffect(ADDITIONAL_FIRE)) {
			return this._mpRegenType >= 64;
		}
		if (120 <= this._inventory.getWeight240()) {
			return false;
		}
		if ((this._food < 3)) {
			return false;
		}
		/*
		 * // 法師加速 if (this.isWizard()) { return this._mpRegenType >= 40; }
		 */
		return this._mpRegenType >= 64;
	}

	// HP自然回復 MP自然回復

	/** 無動作 */
	public static final int REGENSTATE_NONE = 4;

	/** 移動中 */
	public static final int REGENSTATE_MOVE = 2;

	/** 攻擊中 */
	public static final int REGENSTATE_ATTACK = 1;

	public void setRegenState(final int state) {
		this._mpRegenState = state;
		this._hpRegenState = state;
	}

	/**
	 * HP自然回復啟用
	 */
	public void startHpRegeneration() {
		if (!this._hpRegenActive) {
			this._hpRegenActive = true;
		}
	}

	/**
	 * HP自然回復停止
	 */
	public void stopHpRegeneration() {
		if (this._hpRegenActive) {
			this._hpRegenActive = false;
		}
	}

	/**
	 * HP自然回復狀態
	 * 
	 * @return
	 */
	public boolean getHpRegeneration() {
		return _hpRegenActive;
	}

	/**
	 * MP自然回復啟用
	 */
	public void startMpRegeneration() {
		if (!this._mpRegenActive) {
			this._mpRegenActive = true;
		}
	}

	/**
	 * MP自然回復停止
	 */
	public void stopMpRegeneration() {
		if (this._mpRegenActive) {
			this._mpRegenActive = false;
		}
	}

	/**
	 * MP自然回復狀態
	 * 
	 * @return
	 */
	public boolean getMpRegeneration() {
		return _mpRegenActive;
	}

	// 龍騎士 覺醒技能 MP自然減少時間
	public static final int INTERVAL_BY_AWAKE = 4;// 秒

	// 龍騎士覺醒技能 MP自然減少計算時間
	private int _awakeMprTime = 0;// 秒

	/**
	 * 龍騎士覺醒技能 MP自然減少處理時間
	 * 
	 * @return
	 */
	public int get_awakeMprTime() {
		return _awakeMprTime;
	}

	/**
	 * 設置龍騎士覺醒技能 MP自然減少處理時間
	 * 
	 * @param awakeMprTime
	 */
	public void set_awakeMprTime(final int awakeMprTime) {
		this._awakeMprTime = awakeMprTime;
	}

	/**
	 * 龍騎士 覺醒技能 MP自然減少啟用
	 */
	public void startMpReductionByAwake() {
		if (!this._mpReductionActiveByAwake) {
			this.set_awakeMprTime(INTERVAL_BY_AWAKE);
			this._mpReductionActiveByAwake = true;
		}
	}

	/**
	 * 龍騎士 覺醒技能 MP自然減少停止
	 */
	public void stopMpReductionByAwake() {
		if (this._mpReductionActiveByAwake) {
			this.set_awakeMprTime(0);
			this._mpReductionActiveByAwake = false;
		}
	}

	/**
	 * 是否在龍騎士 覺醒技能 MP自然減少中
	 * 
	 * @return
	 */
	public boolean isMpReductionActiveByAwake() {
		return _mpReductionActiveByAwake;
	}

	private int _awakeSkillId = 0;// 龍騎士 覺醒技能編號

	/**
	 * 龍騎士 覺醒技能編號
	 * 
	 * @return
	 */
	public int getAwakeSkillId() {
		return this._awakeSkillId;
	}

	/**
	 * 龍騎士 覺醒技能編號
	 * 
	 * @param i
	 */
	public void setAwakeSkillId(final int i) {
		this._awakeSkillId = i;
	}

	/**
	 * 加入PC 可見物更新處理清單
	 */
	public void startObjectAutoUpdate() {
		this.removeAllKnownObjects();
	}

	/**
	 * 移出各種處理清單
	 */
	public void stopEtcMonitor() {
		// 移出PC 鬼魂模式處理清單
		this.set_ghostTime(-1);
		this.setGhost(false);
		this.setGhostCanTalk(true);
		this.setReserveGhost(false);

		// 移出龍騎士覺醒技能MP自然減少處理
		stopMpReductionByAwake();

		// 移出在線獎勵清單
		OnlineGiftSet.remove(this);

		if (!this.isPrivateShop()) {
			ListMapUtil.clear(_skillList);
			clearPrivateShopLists();
		}
		ListMapUtil.clear(_trade_items);
	}

	private int _old_lawful;

	/**
	 * 原始Lawful
	 * 
	 * @return
	 */
	public int getLawfulo() {
		return _old_lawful;
	}

	/**
	 * 更新Lawful
	 */
	public void onChangeLawful() {
		if (_old_lawful != getLawful()) {
			_old_lawful = getLawful();
			sendPacketsAll(new S_Lawful(this));
			// 戰鬥特化效果
			lawfulUpdate();
		}
	}

	private int _old_karma;

	/**
	 * 原始karma
	 * 
	 * @return
	 */
	public int getKarmalo() {
		return _old_karma;
	}

	/**
	 * 更新karma
	 */
	public void onChangeKarma() {
		if (_old_karma != getKarma()) {
			_old_karma = getKarma();
			sendPackets(new S_Karma(this));
		}
	}

	private boolean _jl1 = false;// 正義的守護 Lv.1
	private boolean _jl2 = false;// 正義的守護 Lv.2
	private boolean _jl3 = false;// 正義的守護 Lv.3
	private boolean _el1 = false;// 邪惡的守護 Lv.1
	private boolean _el2 = false;// 邪惡的守護 Lv.2
	private boolean _el3 = false;// 邪惡的守護 Lv.3

	/**
	 * TODO 戰鬥特化<BR>
	 */
	public void lawfulUpdate() {
		int l = getLawful();

		if (l >= 10000 && l <= 19999) {
			if (!_jl1) {
				overUpdate();
				_jl1 = true;
				sendPackets(new S_PacketBoxProtection(S_PacketBoxProtection.JUSTICE_L1, 1));
				sendPackets(new S_OwnCharAttrDef(this));
				sendPackets(new S_SPMR(this));
			}

		} else if (l >= 20000 && l <= 29999) {
			if (!_jl2) {
				overUpdate();
				_jl2 = true;
				sendPackets(new S_PacketBoxProtection(S_PacketBoxProtection.JUSTICE_L2, 1));
				sendPackets(new S_OwnCharAttrDef(this));
				sendPackets(new S_SPMR(this));
			}

		} else if (l >= 30000 && l <= 39999) {
			if (!_jl3) {
				overUpdate();
				_jl3 = true;
				sendPackets(new S_PacketBoxProtection(S_PacketBoxProtection.JUSTICE_L3, 1));
				sendPackets(new S_OwnCharAttrDef(this));
				sendPackets(new S_SPMR(this));
			}

		} else if (l >= -19999 && l <= -10000) {
			if (!_el1) {
				overUpdate();
				_el1 = true;
				sendPackets(new S_PacketBoxProtection(S_PacketBoxProtection.EVIL_L1, 1));
				sendPackets(new S_SPMR(this));
			}

		} else if (l >= -29999 && l <= -20000) {
			if (!_el2) {
				overUpdate();
				_el2 = true;
				sendPackets(new S_PacketBoxProtection(S_PacketBoxProtection.EVIL_L2, 1));
				sendPackets(new S_SPMR(this));
			}

		} else if (l >= -39999 && l <= -30000) {
			if (!_el3) {
				overUpdate();
				_el3 = true;
				sendPackets(new S_PacketBoxProtection(S_PacketBoxProtection.EVIL_L3, 1));
				sendPackets(new S_SPMR(this));
			}

		} else {
			if (overUpdate()) {
				sendPackets(new S_OwnCharAttrDef(this));
				sendPackets(new S_SPMR(this));
			}
		}
	}

	/**
	 * TODO 戰鬥特化<BR>
	 * 
	 * @return
	 */
	private boolean overUpdate() {
		if (_jl1) {
			_jl1 = false;
			sendPackets(new S_PacketBoxProtection(S_PacketBoxProtection.JUSTICE_L1, 0));
			return true;

		} else if (_jl2) {
			_jl2 = false;
			sendPackets(new S_PacketBoxProtection(S_PacketBoxProtection.JUSTICE_L2, 0));
			return true;

		} else if (_jl3) {
			_jl3 = false;
			sendPackets(new S_PacketBoxProtection(S_PacketBoxProtection.JUSTICE_L3, 0));
			return true;

		} else if (_el1) {
			_el1 = false;
			sendPackets(new S_PacketBoxProtection(S_PacketBoxProtection.EVIL_L1, 0));
			return true;

		} else if (_el2) {
			_el2 = false;
			sendPackets(new S_PacketBoxProtection(S_PacketBoxProtection.EVIL_L2, 0));
			return true;

		} else if (_el3) {
			_el3 = false;
			sendPackets(new S_PacketBoxProtection(S_PacketBoxProtection.EVIL_L3, 0));
			return true;
		}
		return false;
	}

	/**
	 * TODO 戰鬥特化<BR>
	 * <FONT COLOR="#0000ff">遭遇的守護 </FONT>20級以下角色
	 * 被超過10級以上的玩家攻擊而死亡時，不會失去經驗值，也不會掉落物品<BR>
	 * 
	 * @return
	 */
	private boolean isEncounter() {
		if (getLevel() <= ConfigOther.ENCOUNTER_LV) {
			return true;
		}
		return false;
	}

	/**
	 * TODO 戰鬥特化<BR>
	 * <FONT COLOR="#0000ff">正義的守護 Lv.1 </FONT>善惡值 10,000 ~ 19,999 (防御：-2 /
	 * 魔防+3)<BR>
	 * <FONT COLOR="#0000ff">正義的守護 Lv.2 </FONT>善惡值 20,000 ~ 29,999 (防御：-4 /
	 * 魔防+6)<BR>
	 * <FONT COLOR="#0000ff">正義的守護 Lv.3 </FONT>善惡值 30,000 ~ 32,767 (防御：-6 /
	 * 魔防+9)<BR>
	 * <FONT COLOR="#0000ff">邪惡的守護 Lv.1 </FONT>善惡值 -10,000 ~ -19,999 (近/遠距離攻擊力+1 /
	 * 魔攻+1)<BR>
	 * <FONT COLOR="#0000ff">邪惡的守護 Lv.2 </FONT>善惡值 -20,000 ~ -29,999 (近/遠距離攻擊力+3 /
	 * 魔攻+2)<BR>
	 * <FONT COLOR="#0000ff">邪惡的守護 Lv.3 </FONT>善惡值 -30,000 ~ -32,767 (近/遠距離攻擊力+5 /
	 * 魔攻+3)<BR>
	 * <FONT COLOR="#0000ff">遭遇的守護 </FONT>20級以下角色
	 * 被超過10級以上的玩家攻擊而死亡時，不會失去經驗值，也不會掉落物品<BR>
	 */
	public int guardianEncounter() {
		final int l = getLawful();
		if (l >= 10000 && l <= 19999) {
			return 0;

		} else if (l >= 20000 && l <= 29999) {
			return 1;

		} else if (l >= 30000 && l <= 39999) {
			return 2;

		} else if (l >= -19999 && l <= -10000) {
			return 3;

		} else if (l >= -29999 && l <= -20000) {
			return 4;

		} else if (l >= -39999 && l <= -30000) {
			return 5;
		}
		return -1;
	}

	private long _old_exp;

	/**
	 * 原始EXP
	 * 
	 * @return
	 */
	public long getExpo() {
		return _old_exp;
	}

	/**
	 * 更新EXP
	 */
	public void onChangeExp() {
		PlayerActionLock.run(this, this::onChangeExpLocked);
	}

	private void onChangeExpLocked() {
		if (_old_exp != getExp()) {
			_old_exp = getExp();

			final int level = ExpTable.getLevelByExp(getExp());
			final int char_level = getLevel();
			final int gap = level - char_level;

			if (gap == 0) {
				if (level <= 127) {
					sendPackets(new S_Exp(this));
				} else {
					sendPackets(new S_OwnCharStatus(this));
				}
				return;
			}

			if (gap > 0) {
				levelUp(gap);

			} else if (gap < 0) {
				levelDown(gap);
			}

			if (getLevel() > 20) {// LOLI 戰鬥特化
				sendPackets(new S_PacketBoxProtection(S_PacketBoxProtection.ENCOUNTER, 0));

			} else {
				sendPackets(new S_PacketBoxProtection(S_PacketBoxProtection.ENCOUNTER, 1));
			}
		}
	}

	/**
	 * TODO 接觸資訊
	 */
	// @Override
	public void onPerceive(final L1PcInstance perceivedFrom) {
		try {
			if (perceivedFrom.getId() == this.getId()) {
				return;
			}

			/*
			 * if (perceivedFrom.knownsObject(this)) {
			 * return;
			 * }
			 */

			if (this.isGmInvis() || this.isGhost() || this.isInvisble()) {
				return;
			}

			// 副本ID不相等 不相護顯示
			if (perceivedFrom.get_showId() != this.get_showId()) {
				return;
			}

			perceivedFrom.addKnownObject(this);
			// 發送自身資訊給予接觸人物
			perceivedFrom.sendPackets(new S_OtherCharPacks(this, perceivedFrom));

			// 隊伍成員HP狀態發送
			final L1Party perceiveParty = getParty();
			if (perceiveParty != null) {
				if (perceiveParty.isMember(perceivedFrom)) {// 對象是隊員
					perceivedFrom.sendPackets(new S_HPMeter(this));
				}
			}
			if (_isFishing) {
				perceivedFrom.sendPackets(new S_Fishing(getId(), ActionCodes.ACTION_Fishing, get_fishX(), get_fishY()));
			}

			if (this.isPrivateShop()) {
				// [改用] 呼叫地圖的 isUsableShop (查 MapShopTable)
				if (!this.getMap().isUsableShop(this.getX(), this.getY())) {
					this.clearPrivateShopLists();

					this.setPrivateShop(false);
					this.sendPacketsAll(new S_DoActionGFX(this.getId(), ActionCodes.ACTION_Idle));

				} else {
					// 如果合法，就發送商店封包給對方看
					perceivedFrom.sendPackets(new S_DoActionShop(this.getId(), this.getShopChat()));
				}
			}

		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * 清除離開可視範圍物件。先做 MapId/ShowId 隔離，再做距離判斷。
	 * D-3：由 {@link com.lineage.server.world.VisibilityCoordinator} 呼叫。
	 */
	public void removeOutOfRangeKnownObjects() {
		try {
			final List<L1Object> knownList = new ArrayList<L1Object>(getKnownObjects());
			final int myMapId = getMapId();
			final int myShowId = get_showId();
			for (final L1Object known : knownList) {
				if (known == null)
					continue;
				if (known instanceof L1NpcInstance npc) {
					final NpcDeletionVisibilityState.Snapshot state =
							npc.deletionVisibilitySnapshot();
					final boolean worldRegistered =
							World.get().isObjectRegistered(npc);
					if (npc.destroyed() || !worldRegistered) {
					final boolean viewerTracked = npc.isViewerRegistered(this);
					if (!removeKnownObjectIfPresent(known)) {
						continue;
					}
					final NpcDeletionVisibilityState.RepairKind repairKind =
							NpcDeletionVisibilityState.classifyRepair(state,
									npc.deletionStarted(),
									L1NpcInstance.getDeletionStallMillis());
					sendPackets(new S_RemoveObject(known));
					switch (repairKind) {
					case NORMAL_RACE:
						NPC_VISIBILITY_RACE_REPAIR_COUNT.incrementAndGet();
						break;
					case STALL: {
						final long repairs =
								NPC_VISIBILITY_STALL_REPAIR_COUNT.incrementAndGet();
						_log.warn("[NpcLifecycle] stalled deletion visibility repaired: "
								+ "playerId={} player={} objectId={} npcId={} phase={} "
								+ "deleteAgeMs={} phaseAgeMs={} worldRegistered={} "
								+ "destroyed={} viewerTracked={} playerMap={} npcMap={} count={}",
								getId(), getName(), npc.getId(), npc.getNpcId(),
								state.phase(), state.deleteAgeMillis(),
								state.phaseAgeMillis(), worldRegistered,
								npc.destroyed(), viewerTracked, (int) getMapId(),
								(int) npc.getMapId(), repairs);
						break;
					}
					case LEAK: {
						final long repairs =
								NPC_VISIBILITY_LEAK_REPAIR_COUNT.incrementAndGet();
						_log.warn("[NpcLifecycle] stale NPC leak repaired: "
								+ "playerId={} player={} objectId={} npcId={} phase={} "
								+ "deleteAgeMs={} phaseAgeMs={} worldRegistered={} "
								+ "destroyed={} viewerTracked={} playerMap={} npcMap={} count={}",
								getId(), getName(), npc.getId(), npc.getNpcId(),
								state.phase(), state.deleteAgeMillis(),
								state.phaseAgeMillis(), worldRegistered,
								npc.destroyed(), viewerTracked, (int) getMapId(),
								(int) npc.getMapId(), repairs);
						break;
					}
					}
					continue;
					}
				}
				if (known.getMapId() != myMapId || known.get_showId() != myShowId) {
					removeKnownObject(known);
					sendPackets(new S_RemoveObject(known));
					continue;
				}
				if (Config.PC_RECOGNIZE_RANGE == -1) {
					if (!getLocation().isInScreen(known.getLocation())) {
						removeKnownObject(known);
						sendPackets(new S_RemoveObject(known));
					}
				} else if (getLocation().getTileLineDistance(known.getLocation()) > Config.PC_RECOGNIZE_RANGE) {
					removeKnownObject(known);
					sendPackets(new S_RemoveObject(known));
				}
			}
		} catch (Exception e) {
			_log.error("視野移除處理異常: " + getName(), e);
		}
	}

	/**
	 * 可見物更新處理（委派 {@link com.lineage.server.world.VisibilityCoordinator}）。
	 */
	public void updateObject() {
		com.lineage.server.world.VisibilityCoordinator.updateForPc(this);
	}

	/**
	 * 可以觀看HP的對象(特別定義)
	 * 
	 * @param obj
	 * @return
	 */
	public boolean isHpBarTarget(final L1Object obj) {
		// 所在地圖位置
		switch (this.getMapId()) {
			case 400:// 大洞穴/大洞穴抵抗軍/隱遁者地區
				if (obj instanceof L1FollowerInstance follower) {
					if (follower.getMaster().equals(this)) {
						return true;
					}
				}
				break;
		}
		return false;
	}

	/**
	 * GM HPBAR 可以觀看HP的對象
	 * 
	 * @param obj
	 * @return
	 */
	public boolean isGmHpBarTarget(final L1Object obj) {
		if (obj instanceof L1MonsterInstance) {
			return true;
		}
		if (obj instanceof L1PcInstance) {
			return true;
		}
		if (obj instanceof L1SummonInstance) {
			return true;
		}
		if (obj instanceof L1PetInstance) {
			return true;
		}
		if (obj instanceof L1DeInstance) {
			return true;
		}
		if (obj instanceof L1FollowerInstance) {
			return true;
		}
		return false;
	}

	private void sendVisualEffect() {
		int poisonId = 0;
		if (this.getPoison() != null) { // 毒状態
			poisonId = this.getPoison().getEffectId();
		}
		if (this.getParalysis() != null) { // 麻痺状態
			// 優先發送麻痺效果，故覆寫poisonId
			poisonId = this.getParalysis().getEffectId();
		}
		if (poisonId != 0) { // 此 if 可能可省略
			this.sendPacketsAll(new S_Poison(this.getId(), poisonId));
		}
	}

	/** 中毒，麻痹視覺效果顯示 */
	public void sendVisualEffectAtLogin() {
		this.sendVisualEffect();
		this.updateSetBonus();
	}

	private boolean _isCHAOTIC = false;

	public boolean isCHAOTIC() {
		return this._isCHAOTIC;
	}

	public void setCHAOTIC(final boolean flag) {
		this._isCHAOTIC = flag;
	}

	public void sendVisualEffectAtTeleport() {
		if (this.isDrink()) { // 醉酒效果
			this.sendPackets(new S_Liquor(this.getId()));
		}
		if (this.isCHAOTIC()) { // 混亂效果
			this.sendPackets(new S_Liquor(this.getId(), 2));
		}
		this.sendVisualEffect();
	}

	// 可用技能編號列表
	private java.util.concurrent.CopyOnWriteArrayList<Integer> _skillList = new java.util.concurrent.CopyOnWriteArrayList<Integer>();

	/**
	 * 加入技能編號列表
	 * 
	 * @param skillid
	 */
	public void setSkillMastery(final int skillid) {
		if (!this._skillList.contains(skillid)) {
			this._skillList.add(skillid);
		}
	}

	/**
	 * 移出技能編號列表
	 * 
	 * @param skillid
	 */
	public void removeSkillMastery(final int skillid) {
		this._skillList.remove(Integer.valueOf(skillid));
	}

	/**
	 * 傳回是否具有該技能使用權
	 * 
	 * @param skillid
	 * @return
	 */
	public boolean isSkillMastery(final int skillid) {
		return this._skillList.contains(skillid);
	}

	/**
	 * 清空
	 */
	public void clearSkillMastery() {
		this._skillList.clear();
	}

	/**
	 * TODO 起始設置
	 */
	public L1PcInstance() {
		_accessLevel = 0;
		_currentWeapon = 0;
		_inventory = new L1PcInventory(this);
		_dwarf = new L1DwarfInventory(this);
		_dwarfForCha = new L1DwarfForChaInventory(this);
		_dwarfForElf = new L1DwarfForElfInventory(this);
		_quest = new L1PcQuest(this);
		_action = new L1ActionPc(this);
		_actionPet = new L1ActionPet(this);
		_actionSummon = new L1ActionSummon(this);
		_equipSlot = new L1EquipmentSlot(this);
	}

	/**
	 * 娃娃跟隨主人變更移動/速度狀態
	 */
	public void setNpcSpeed() {
		try {
			// 取回娃娃
			if (!getDolls().isEmpty()) {
				for (final Object obj : getDolls().values().toArray()) {
					final L1DollInstance doll = (L1DollInstance) obj;
					if (doll != null) {
						doll.setNpcMoveSpeed();
					}
				}
			}

		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	private static final class PlayerHpMutation extends CurrentHpMutation {
		private final long _deathGeneration;

		private PlayerHpMutation(final int previousHp, final int currentHp,
				final long deathGeneration) {
			super(previousHp, currentHp);
			_deathGeneration = deathGeneration;
		}

		@Override
		protected boolean completionRequired() {
			return changed() || _deathGeneration != 0L;
		}
	}

	/**
	 * The HP value and terminal death claim are one player-local transition.
	 * Network/party work and the death handoff are completed by the unlocked hook.
	 */
	@Override
	protected CurrentHpMutation commitCurrentHpLocked(final int requestedHp) {
		final int maxHp = Math.max(0, getMaxHp());
		int currentHp = Math.max(0, Math.min(requestedHp, maxHp));
		// A freshly constructed/login-hydration object has not received max HP yet.
		// Clamping its first assignment to zero must not start a death pipeline.
		if (maxHp == 0) {
			final CurrentHpMutation committed = super.commitCurrentHpLocked(0);
			return new PlayerHpMutation(committed.previousHp(), committed.currentHp(), 0L);
		}
		if (currentHp <= 0 && isGm()) {
			currentHp = maxHp;
		}
		// Ordinary healing/setter paths cannot create the invalid dead + positive HP state.
		// L1Character.resurrect explicitly clears dead under this same lock first.
		if (isDead() && currentHp > 0) {
			return new PlayerHpMutation(getCurrentHp(), getCurrentHp(), 0L);
		}

		final CurrentHpMutation committed = super.commitCurrentHpLocked(currentHp);
		long deathGeneration = 0L;
		if (currentHp <= 0 && !isGm() && !isDead()) {
			deathGeneration = beginDeathStateLocked();
		}
		return new PlayerHpMutation(committed.previousHp(), getCurrentHp(), deathGeneration);
	}

	@Override
	protected void afterCurrentHpMutation(final CurrentHpMutation mutation) {
		final long deathGeneration = mutation instanceof PlayerHpMutation playerMutation
				? playerMutation._deathGeneration : 0L;
		if (deathGeneration != 0L) {
			setMining(false);
			finishDeathStart(null, deathGeneration);
		}
		if (!mutation.changed()) {
			return;
		}
		final int publishedHp = getCurrentHp();
		try {
			sendPackets(new S_HPUpdate(publishedHp, getMaxHp()));
		} catch (final RuntimeException packetFailure) {
			_log.error("Player HP update failed after vital commit: pc={}", getName(), packetFailure);
		}
		try {
			final L1Party party = getParty();
			if (party != null) {
				party.updateMiniHP(this);
			}
		} catch (final RuntimeException partyFailure) {
			_log.error("Party HP update failed after vital commit: pc={}", getName(), partyFailure);
		}
	}

	@Override
	protected void afterCurrentMpMutation(final CurrentMpMutation mutation) {
		if (!mutation.changed()) {
			return;
		}
		try {
			sendPackets(new S_MPUpdate(getCurrentMp(), getMaxMp()));
		} catch (final RuntimeException packetFailure) {
			_log.error("Player MP update failed after vital commit: pc={}", getName(), packetFailure);
		}
	}

	// @Override
	public L1PcInventory getInventory() {
		return this._inventory;
	}

	public L1DwarfInventory getDwarfInventory() {
		this._dwarf.refreshAfterMigration();
		return this._dwarf;
	}

	public L1DwarfForChaInventory getDwarfForChaInventory() {
		return this._dwarfForCha;
	}

	public L1DwarfForElfInventory getDwarfForElfInventory() {
		this._dwarfForElf.refreshAfterMigration();
		return this._dwarfForElf;
	}

	public boolean isGmInvis() {
		return this._gmInvis;
	}

	public void setGmInvis(final boolean flag) {
		this._gmInvis = flag;
	}

	public boolean isGmMapTileProbeEnabled() {
		return this._gmMapTileProbeEnabled;
	}

	public void setGmMapTileProbeEnabled(final boolean enabled) {
		this._gmMapTileProbeEnabled = enabled;
	}

	public int getCurrentWeapon() {
		return this._currentWeapon;
	}

	public void setCurrentWeapon(final int i) {
		this._currentWeapon = i;
	}

	/**
	 * 0:王族 1:騎士 2:精靈 3:法師 4:黑妖 5:龍騎 6:幻術
	 * 
	 * @return
	 */
	public int getType() {
		return this._type;
	}

	/**
	 * 0:王族 1:騎士 2:精靈 3:法師 4:黑妖 5:龍騎 6:幻術
	 * 
	 * @param i
	 */
	public void setType(final int i) {
		this._type = i;
	}

	public short getAccessLevel() {
		return this._accessLevel;
	}

	public void setAccessLevel(final short i) {
		this._accessLevel = i;
	}

	public int getClassId() {
		return this._classId;
	}

	public void setClassId(final int i) {
		this._classId = i;
		this._classFeature = L1ClassFeature.newClassFeature(i);
	}

	private L1ClassFeature _classFeature = null;

	public L1ClassFeature getClassFeature() {
		return _classFeature;
	}

	// @Override
	public long getExp() {
		_expMutationLock.lock();
		try {
			return _exp;
		} finally {
			_expMutationLock.unlock();
		}
	}

	// @Override
	public void setExp(final long i) {
		_expMutationLock.lock();
		try {
			_exp = i;
		} finally {
			_expMutationLock.unlock();
		}
	}

	private int _PKcount; // ● PK 計數

	public int get_PKcount() {
		return this._PKcount;
	}

	public void set_PKcount(final int i) {
		this._PKcount = i;
	}

	private int _PkCountForElf; // ● PK 計數（妖精用）

	public int getPkCountForElf() {
		return this._PkCountForElf;
	}

	public void setPkCountForElf(final int i) {
		this._PkCountForElf = i;
	}

	private volatile int _clanid = -1; // 血盟ID

	public int getClanid() {
		return this._clanid;
	}

	public void setClanid(final int i) {
		this._clanid = i;
	}

	private volatile String clanname; // 血盟名稱

	public String getClanname() {
		return this.clanname;
	}

	public void setClanname(final String s) {
		this.clanname = s;
	}

	/**
	 * 血盟資料
	 * 
	 * @return
	 */
	public L1Clan getClan() {
		return WorldClan.get().getClan(this.getClanname());
	}

	private int _clanRank; // ● 血盟內排名（血盟君主、守護者、一般、見習）

	/**
	 * 血盟階級
	 * 
	 * @return 2:一般 3:副君主 4:聯盟君主 5:修習騎士 6:守護騎士 7:一般 8:修習騎士 9:守護騎士 10:聯盟君主
	 */
	public int getClanRank() {
		return this._clanRank;
	}

	/**
	 * 血盟階級<BR>
	 * 2:一般 3:副君主 4:聯盟君主 5:修習騎士 6:守護騎士 7:一般 8:修習騎士 9:守護騎士 10:聯盟君主
	 * 
	 * @param i
	 */
	public void setClanRank(final int i) {
		this._clanRank = i;
	}

	private byte _sex; // ● 性別

	/**
	 * 性別
	 * 
	 * @return
	 */
	public byte get_sex() {
		return this._sex;
	}

	/**
	 * 性別
	 * 
	 * @param i
	 */
	public void set_sex(final int i) {
		this._sex = (byte) i;
	}

	public boolean isGm() {
		return this._gm;
	}

	public void setGm(final boolean flag) {
		this._gm = flag;
	}

	public boolean isMonitor() {
		return this._monitor;
	}

	public void setMonitor(final boolean flag) {
		this._monitor = flag;
	}

	// [已移除] getStat() 方法總是返回 null，導致 NullPointerException
	// 此方法已被移除，因為沒有實際用途且會導致錯誤

	// [已修復] reduceCurrentHp 方法：直接調用父類或使用 receiveDamage 方法
	// 注意：此方法目前未被使用，如果未來需要，應該實作正確的邏輯
	@Deprecated
	public void reduceCurrentHp(final double d, final L1Character l1character) {
		// Environmental damage also joins the target FIFO. The former null-attacker
		// branch was an unlocked read-modify-write and could lose a concurrent hit.
		this.receiveDamage(l1character, d, false, false);
	}

	/**
	 * 通知指定玩家群已登出
	 * 
	 * @param playersList
	 *                    要通知的玩家陣列
	 */
	private void notifyPlayersLogout(final List<L1PcInstance> playersArray) {
		for (final L1PcInstance player : playersArray) {
			if (player.knownsObject(this)) {
				player.removeKnownObject(this);
				player.sendPackets(new S_RemoveObject(this));
			}
		}
	}

    private volatile com.lineage.server.model.MigrationDeparture migrationDeparture;
    public com.lineage.server.model.MigrationDeparture getMigrationDeparture() { return migrationDeparture; }
    public void setMigrationDeparture(com.lineage.server.model.MigrationDeparture departure) { migrationDeparture = departure; }

    /** Only a published source actor can enter this no-save teardown. */
    public void logoutAfterMigration(final com.lineage.server.model.MigrationDeparture departure) {
        if (departure == null || departure != migrationDeparture
                || !com.lineage.server.model.MigrationDeparture.isDeparted(getId()))
            throw new IllegalStateException("Wrong migration teardown owner");
        setOnlineStatus(0);
        stopEtcMonitor();
        stopAI();
        if (isFollowRunning() || !getFollowers().isEmpty()) stopFollow();
        clearSkillEffectTimer();
        if (departure.ownsWorldId()) {
            com.lineage.server.datatables.user.CharItemsTable.get().removeDepartedUserItemsFromMemoryOnly(this);
            CharBuffTable.get().removeCharacterBuffFromMemoryOnly(getId());
            CharMapsTimeTable.get().removeDepartedCharacterFromMemory(getId());
            logoutInternal(false, true);
        } else {
            // World removal already compares exact instances. No ID/name cache or remove packets here.
            World.get().removeVisibleObject(this);
            World.get().removeObject(this);
            removeAllKnownObjects();
            stopHpRegeneration(); stopMpRegeneration(); setDead(true);
            if (departure.ownsConnection()) detachOutboundConnection();
        }
    }

	public void logout() {
		logoutInternal(false);
	}

	public void logoutWithConfirmedBuffs() {
		logoutInternal(true);
	}

	private void logoutInternal(final boolean confirmBuffSave) {
        if (com.lineage.server.model.MigrationDeparture.deferLogout(this)) return;
        if (com.lineage.server.model.MigrationDeparture.isDeparted(getId())) {
            com.lineage.server.model.MigrationDeparture.complete(this); return;
        }
        logoutInternal(confirmBuffSave, false);
    }

    private void logoutInternal(final boolean confirmBuffSave, final boolean migrated) {
		com.lineage.server.model.L1DeathMatch.getInstance().onPlayerLogout(this);
		com.lineage.server.model.shop.ShopDailyLimitTracker.clear(this.getId());
		setTeleport(false);
		setMining(false);
		// 強制停止內掛與排程器，防止玩家物件殘留於記憶體中
		if (isAiRunning()) {
			stopAI();
		}
		// 強制停止高寵跟隨 (高寵或主人皆需解綁)
		if (isFollowRunning() || !getFollowers().isEmpty()) {
			stopFollow();
		}
		if (!migrated) {
		if (confirmBuffSave) {
			try {
				CharBuffTable.get().saveBuffConfirmed(this);
			} catch (final java.sql.SQLException failure) {
				throw new IllegalStateException("Logout buff save unconfirmed: charId=" + getId(), failure);
			}
		} else {
			CharBuffTable.get().saveBuff(this);
		}
		CharMapsTimeTable.get().updateTime(this);
        }
		
		// 清除天賦快取（釋放記憶體）
		com.lineage.server.datatables.user.CharacterTalentTable.get().removeCacheForChar(this.getId());

		clearOptionalGameSessionsOnLogout();

		if (this.getClanid() != 0) {
			final L1Clan clan = WorldClan.get().getClan(this.getClanname());
			if (clan != null) {
				if (clan.getWarehouseUsingChar() == this.getId()) {
					clan.releaseWarehouse(this.getId()); // 僅釋放自己的倉庫 owner
				}
			}
		}
		this.notifyPlayersLogout(this.getKnownPlayers());

		// 正在參加副本
		if (this.get_showId() != -1) {
			// 副本編號 是執行中副本
			if (WorldQuest.get().isQuest(this.get_showId())) {
				// 移出副本
				WorldQuest.get().remove(this.get_showId(), this);
			}
		}
		// 重置副本編號
		this.set_showId(-1);

		World.get().removeVisibleObject(this);
		World.get().removeObject(this);
		this.notifyPlayersLogout(World.get().getRecognizePlayer(this));

		// this._inventory.clearItems();
		// this._dwarf.clearItems();

		this.removeAllKnownObjects();
		this.stopHpRegeneration();
		this.stopMpRegeneration();

		this.setDead(true); // 用法可能不當，但用於讓NPC得知已消失
		if (!migrated || migrationDeparture.ownsConnection()) this.detachOutboundConnection();
	}

	private void clearOptionalGameSessionsOnLogout() {
		try {
			com.lineage.server.model.slot.SlotMachineSession.get().clearOnLogout(this);
		} catch (final NoClassDefFoundError e) {
			_log.warn("SlotMachineSession unavailable during pc logout: {}", getName(), e);
		} catch (final LinkageError e) {
			_log.warn("SlotMachineSession linkage failure during pc logout: {}", getName(), e);
		} catch (final Exception e) {
			_log.warn("SlotMachineSession cleanup failed during pc logout: {}", getName(), e);
		}

		try {
			com.lineage.server.model.bingo.BingoLotterySession.get().clearOnLogout(this);
		} catch (final NoClassDefFoundError e) {
			_log.warn("BingoLotterySession unavailable during pc logout: {}", getName(), e);
		} catch (final LinkageError e) {
			_log.warn("BingoLotterySession linkage failure during pc logout: {}", getName(), e);
		} catch (final Exception e) {
			_log.warn("BingoLotterySession cleanup failed during pc logout: {}", getName(), e);
		}
	}

	public ClientExecutor getNetConnection() {
		return this._netConnection;
	}

	public void setNetConnection(final ClientExecutor clientthread) {
		if (clientthread != null) {
            com.lineage.server.model.MigrationDeparture.requireWritable(getId());
			_outboundDetachExpected = false;
			invalidatePendingOfflineLogout();
		}
		this._netConnection = clientthread;
		_connectionGeneration.updateAndGet(L1PcInstance::nextLifecycleGeneration);
		setTeleport(false);
	}

	/**
	 * Marks an intentional logout/character-switch detach before clearing the two
	 * outbound references. PacketSafety must keep warning for an online character
	 * that loses these references without passing through this lifecycle boundary.
	 *
	 * @return the client that owned this character before detachment
	 */
	public ClientExecutor detachOutboundConnection() {
		com.lineage.server.model.npc.UiOperationContext.clear(this);
		_outboundDetachExpected = true;
		final ClientExecutor client = _netConnection;
		setNetConnection(null);
		setPacketOutput(null);
		return client;
	}

	/**
	 * 是否再隊伍中
	 * 
	 * @return
	 */
	public boolean isInParty() {
		return this.getParty() != null;
	}

	/**
	 * 傳回隊伍
	 * 
	 * @return
	 */
	public L1Party getParty() {
		return this._party;
	}

	/**
	 * 設置隊伍
	 * 
	 * @param p
	 */
	public void setParty(final L1Party p) {
		this._party = p;
	}

	public boolean isInChatParty() {
		return this.getChatParty() != null;
	}

	public L1ChatParty getChatParty() {
		return this._chatParty;
	}

	public void setChatParty(final L1ChatParty cp) {
		this._chatParty = cp;
	}

	public int getPartyID() {
		return this._partyID;
	}

	public void setPartyID(final int partyID) {
		this._partyID = partyID;
	}

	public int getTradeID() {
		return this._tradeID;
	}

	public void setTradeID(final int tradeID) {
		this._tradeID = tradeID;
	}

	public void setTradeOk(final boolean tradeOk) {
		this._tradeOk = tradeOk;
	}

	public boolean getTradeOk() {
		return this._tradeOk;
	}

	/**
	 * 傳回暫時紀錄的objid
	 * 
	 * @return
	 */
	public int getTempID() {
		return this._tempID;
	}

	/**
	 * 設置暫時紀錄的objid
	 * 
	 * @param tempID
	 */
	public void setTempID(final int tempID) {
		this._tempID = tempID;
	}

	public int getTempDailyQuestId() {
		return _tempDailyQuestId;
	}

	public void setTempDailyQuestId(final int id) {
		this._tempDailyQuestId = id;
	}

	public int getDailyQuestListPage() {
		return _dailyQuestListPage;
	}

	public void setDailyQuestListPage(final int page) {
		this._dailyQuestListPage = page;
	}

	public java.util.List<com.lineage.server.templates.L1QuestTemplate> getTempDailyQuestList() {
		return _tempDailyQuestList;
	}

	public void setTempDailyQuestList(final java.util.List<com.lineage.server.templates.L1QuestTemplate> list) {
		this._tempDailyQuestList = list;
	}

	/**
	 * 取得穿雲招集設定
	 */
	public L1WearingArrowConfig getWearingArrowConfig() {
		return _wearingArrowConfig;
	}

	/**
	 * 設置穿雲招集設定 (使用穿雲箭道具時由 WearingArrows 設定)
	 */
	public void setWearingArrowConfig(L1WearingArrowConfig config) {
		this._wearingArrowConfig = config;
	}

	/**
	 * 是否顯示穿雲招集 Y/N 邀請視窗
	 * 
	 * @return true=顯示彈窗邀請, false=僅收通知不彈窗 (可避免部分客戶端異常)
	 */
	public boolean getShowWearingArrows() {
		return _showWearingArrows;
	}

	/**
	 * 設置是否顯示穿雲招集 Y/N 邀請視窗
	 */
	public void setShowWearingArrows(boolean value) {
		this._showWearingArrows = value;
	}

	/** 隊伍掉落：關閉時不顯示隊友獲取戰利品 (813) */
	public boolean isShowPartyDrop() {
		return _showPartyDrop;
	}

	public void setShowPartyDrop(boolean value) {
		this._showPartyDrop = value;
	}

	/** 個人掉落：關閉時不顯示個人獲取戰利品 (143) */
	public boolean isShowPcDrop() {
		return _showPcDrop;
	}

	public void setShowPcDrop(boolean value) {
		this._showPcDrop = value;
	}

	/** 喝水顯示：關閉時不顯示喝水的 spr 跟文字 */
	public boolean isShowPotionEffect() {
		return _showPotionEffect;
	}

	public void setShowPotionEffect(boolean value) {
		this._showPotionEffect = value;
	}

	/** 傷害顯示：關閉時不顯示傷害 Spr */
	public boolean isShowDamage() {
		return _showDamage;
	}

	public void setShowDamage(boolean value) {
		this._showDamage = value;
	}

	/** 娃娃顯示：關閉時不顯示召喚娃娃的 Spr */
	public boolean isShowDollSpr() {
		return _showDollSpr;
	}

	public void setShowDollSpr(boolean value) {
		this._showDollSpr = value;
	}

	/** Proc 特效顯示：關閉時不顯示武器/裝備/魔法石/娃娃等 Proc 的視覺效果 */
	public boolean isShowProcSpr() {
		return _showProcSpr;
	}

	public void setShowProcSpr(boolean value) {
		this._showProcSpr = value;
	}

	/**
	 * 是否為傳送狀態中
	 * 
	 * @return
	 */
	public boolean isTeleport() {
		return this._isTeleport;
	}

	/**
	 * A rejected item/skill may unlock its own unaccepted client request, but
	 * must not clear an accepted teleport or its position-sync evidence.
	 * Fence both publication and outbound admission against a newer
	 * teleport/connection. No new timer or queued retry is introduced.
	 */
	public boolean sendTeleportUnlockIfIdle() {
		synchronized (_teleportLifecycleLock) {
			if (_isTeleport) {
				return false;
			}
			final long generation = _teleportGeneration;
			final long connection = _connectionGeneration.get();
			final S_Paralysis unlock = new S_Paralysis(S_Paralysis.TYPE_TELEPORT_UNLOCK, false) {
				@Override
				public boolean isValidForWrite() {
					return !_isTeleport && _teleportGeneration == generation
							&& _connectionGeneration.get() == connection;
				}
			};
			sendPackets(com.lineage.server.utils.TeleportTrace.packet(this, "IDLE_REQUEST_UNLOCK", unlock));
			return true;
		}
	}

	// Immutable diagnostic mirror: readers must not take the lifecycle lock from
	// socket/timer/PlayerActionLock lanes. It never participates in admission.
	private volatile String _teleportTraceState = "lifecycle[uninitialized]";

	public String getTeleportTraceState() {
		return _teleportTraceState;
	}

	/** Caller already owns _teleportLifecycleLock; only opted-in characters allocate. */
	private void refreshTeleportTraceStateLocked() {
		if (com.lineage.server.utils.TeleportTrace.enabled(this)) {
			_teleportTraceState = formatTeleportLifecycleState();
		}
	}

	/** {@link com.lineage.server.utils.TeleportTrace} 調查用快照。 */
	public String formatTeleportLifecycleState() {
		synchronized (_teleportLifecycleLock) {
			return String.format(
					"lifecycle[isTeleport=%s gen=%d connGen=%d curConnGen=%d ack=%s dest=(%d,%d,%d) h=%d]",
					_isTeleport, _teleportGeneration, _teleportConnectionGeneration,
					_connectionGeneration.get(), _teleportClientAckAllowed,
					_teleportX, _teleportY, _teleportMapId, _teleportHeading);
		}
	}

	/** Starts a new teleport generation using the currently staged destination. */
	public long beginTeleportTransition() {
		return prepareTeleportTransition(getTeleportX(), getTeleportY(), getTeleportMapId(),
				getTeleportHeading(), false);
	}

	/** Atomically installs one complete teleport request. */
	public long prepareTeleportTransition(final int x, final int y, final short mapId,
			final int heading, final boolean clientAckAllowed) {
		final long generation;
		synchronized (_teleportLifecycleLock) {
			if (_isTeleport) {
				com.lineage.server.utils.TeleportTrace.event(this, "STATE_SUPERSEDED");
			}
			_teleportGeneration = nextLifecycleGeneration(_teleportGeneration);
			_teleportConnectionGeneration = _connectionGeneration.get();
			_teleportX = x;
			_teleportY = y;
			_teleportMapId = mapId;
			_teleportHeading = heading;
			_teleportClientAckAllowed = clientAckAllowed;
			_teleportClientAckAction = null;
			_isTeleport = true;
			com.lineage.server.model.npc.UiOperationContext.clear(this);
			generation = _teleportGeneration;
			refreshTeleportTraceStateLocked();
			com.lineage.server.utils.TeleportTrace.event(this, "STATE_PREPARED");
		}
		setNowTarget(null);
		return generation;
	}

	/** Runs a teleport commit only while both its request and connection are current. */
	public boolean runIfTeleportTransition(final long teleportGeneration,
			final long connectionGeneration, final Runnable action) {
		if (action == null) {
			return false;
		}
		synchronized (_teleportLifecycleLock) {
			if (!_isTeleport || _teleportGeneration != teleportGeneration
					|| _teleportConnectionGeneration != connectionGeneration
					|| _connectionGeneration.get() != connectionGeneration) {
				return false;
			}
			action.run();
			return true;
		}
	}

	/** Installs the native-ready continuation before allowing its acknowledgement. */
	public boolean installTeleportClientAckAction(final long generation, final long connection,
			final Runnable action) {
		synchronized (_teleportLifecycleLock) {
			if (action == null || !isTeleportTransitionCurrent(generation, connection, false)
					|| _teleportClientAckAllowed) return false;
			_teleportClientAckAction = action;
			_teleportClientAckAllowed = true;
			refreshTeleportTraceStateLocked();
			return true;
		}
	}

	/** Accepts exactly one native-ready acknowledgement; it is not an animation-complete ACK. */
	public boolean runIfTeleportClientAck(final Runnable action) {
		if (action == null) {
			return false;
		}
		synchronized (_teleportLifecycleLock) {
			if (!_isTeleport || !_teleportClientAckAllowed
					|| _teleportConnectionGeneration != _connectionGeneration.get()) {
				return false;
			}
			_teleportClientAckAllowed = false;
			final Runnable continuation = _teleportClientAckAction != null ? _teleportClientAckAction : action;
			_teleportClientAckAction = null;
			refreshTeleportTraceStateLocked();
			final long generation = _teleportGeneration;
			final long connection = _teleportConnectionGeneration;
			try {
				continuation.run();
			} catch (final Throwable failure) {
				L1Teleport.recoverFailedTeleport(this, generation, connection, "client_ack_error", failure);
			}
			return true;
		}
	}

	public boolean cancelTeleportTransition(final long teleportGeneration) {
		synchronized (_teleportLifecycleLock) {
			if (!_isTeleport || _teleportGeneration != teleportGeneration) {
				return false;
			}
			com.lineage.server.utils.TeleportTrace.event(this, "STATE_CANCEL", "reqGen", teleportGeneration);
			_isTeleport = false;
			_teleportClientAckAllowed = false;
			_teleportClientAckAction = null;
			_teleportGeneration = nextLifecycleGeneration(_teleportGeneration);
			refreshTeleportTraceStateLocked();
			return true;
		}
	}

	/** Clock admission must succeed before publishing native teleport initialization. */
	public boolean armTeleportClientAckTimeout(final long teleportGeneration, final long timeoutMillis) {
		return L1Teleport.armTeleportClientAckTimeout(this, teleportGeneration, timeoutMillis);
	}

	/** Lock-free outbound fence; never acquire lifecycle locks from a socket writer. */
	public boolean isTeleportTransitionCurrent(final long generation, final long connection,
			final boolean requireClientAck) {
		return _teleportGeneration == generation && _isTeleport
				&& _teleportConnectionGeneration == connection
				&& _connectionGeneration.get() == connection
				&& (!requireClientAck || _teleportClientAckAllowed)
				&& _teleportGeneration == generation;
	}

	/** One current owner may recover; an ACK already claimed wins against its timeout. */
	public boolean runIfTeleportRecovery(final long generation, final long connection,
			final boolean requireClientAck, final Runnable recovery) {
		synchronized (_teleportLifecycleLock) {
			if (recovery == null || !isTeleportTransitionCurrent(generation, connection, requireClientAck)) {
				return false;
			}
			_teleportClientAckAllowed = false;
			_teleportClientAckAction = null;
			refreshTeleportTraceStateLocked();
			recovery.run();
			return true;
		}
	}

	/** Capture under the existing owner lock; the returned socket predicate is lock-free. */
	public java.util.function.BooleanSupplier captureTeleportRecoveryGuard() {
		synchronized (_teleportLifecycleLock) {
			final long generation = _teleportGeneration;
			final long connection = _connectionGeneration.get();
			final long location = getLocationRevision();
			return () -> isTeleportTransitionCurrent(generation, connection, false)
					&& getLocationRevision() == location;
		}
	}

	public long getConnectionGeneration() {
		return _connectionGeneration.get();
	}

	private static long nextLifecycleGeneration(final long current) {
		return current == Long.MAX_VALUE ? 1L : Math.max(1L, current + 1L);
	}

	/**
	 * 設置傳送狀態中
	 * 
	 * @param flag
	 */
	public void setTeleport(final boolean flag) {
		if (flag) {
			beginTeleportTransition();
			return;
		}
		synchronized (_teleportLifecycleLock) {
			if (_isTeleport) {
				com.lineage.server.utils.TeleportTrace.event(this, "STATE_CLEAR");
			}
			_isTeleport = false;
			_teleportClientAckAllowed = false;
			_teleportClientAckAction = null;
			_teleportGeneration = nextLifecycleGeneration(_teleportGeneration);
			refreshTeleportTraceStateLocked();
		}
		// Teleport/relogin uses the native full positioning lifecycle.  Any MOVE
		// correction evidence from the previous client/location generation must not
		// keep the newly synchronized character fenced from targeted actions.
		_positionSyncGate.resetPositionSync();
	}

	/**
	 * 醉酒狀態
	 * 
	 * @return
	 */
	public boolean isDrink() {
		return this._isDrink;
	}

	/**
	 * 醉酒狀態
	 * 
	 * @param flag
	 */
	public void setDrink(final boolean flag) {
		this._isDrink = flag;
	}

	public boolean isGres() {
		return this._isGres;
	}

	public void setGres(final boolean flag) {
		this._isGres = flag;
	}

	/**
	 * 紅名狀態
	 * 
	 * @return
	 */
	public boolean isPinkName() {
		return _activePinkNameLease.get() != null;
	}

	/**
	 * Legacy state setter. Clearing invalidates the current timer lease; setting
	 * creates a new standalone generation only when no generation is active.
	 * 
	 * @param flag
	 */
	public void setPinkName(final boolean flag) {
		_pinkNameStateLock.lock();
		try {
			if (!flag) {
				_activePinkNameLease.set(null);
			} else if (_activePinkNameLease.get() == null) {
				final PinkNameLease lease = new PinkNameLease(
						_pinkNameGeneration.incrementAndGet());
				_activePinkNameLease.compareAndSet(null, lease);
			}
		} finally {
			_pinkNameStateLock.unlock();
		}
	}

	/** Identity token owned by exactly one active pink-name timer generation. */
	public static final class PinkNameLease {
		private final long _generation;

		private PinkNameLease(final long generation) {
			_generation = generation;
		}

		public long generation() {
			return _generation;
		}
	}

	/**
	 * Publishes the active packet in the same per-player state transaction and
	 * claims a timer lease only if none is active. Every valid hit still invokes
	 * {@code publishActive}, preserving the legacy client refresh behaviour.
	 *
	 * @return the new lease when this call must start the timer, otherwise null
	 */
	public PinkNameLease publishPinkNameActive(final Runnable publishActive) {
		if (publishActive == null) {
			throw new NullPointerException("publishActive");
		}
		_pinkNameStateLock.lock();
		try {
			// Legacy order sent the active packet before changing the state. Keeping
			// that order also avoids leaving an unscheduled lease if packet creation fails.
			publishActive.run();
			if (_activePinkNameLease.get() != null) {
				return null;
			}
			final PinkNameLease lease = new PinkNameLease(
					_pinkNameGeneration.incrementAndGet());
			return _activePinkNameLease.compareAndSet(null, lease) ? lease : null;
		} finally {
			_pinkNameStateLock.unlock();
		}
	}

	/** Returns true only while this exact timer generation is active. */
	public boolean isPinkNameLeaseCurrent(final PinkNameLease lease) {
		return lease != null && _activePinkNameLease.get() == lease;
	}

	/**
	 * Clears and publishes inactive only when the caller still owns the exact
	 * active generation. The state lock orders inactive/active packets across a
	 * stop-versus-new-hit race, while CAS supplies the ABA identity fence.
	 */
	public boolean finishPinkNameLease(final PinkNameLease lease,
			final Runnable publishInactive) {
		if (lease == null || publishInactive == null) {
			return false;
		}
		_pinkNameStateLock.lock();
		try {
			if (!_activePinkNameLease.compareAndSet(lease, null)) {
				return false;
			}
			publishInactive.run();
			return true;
		} finally {
			_pinkNameStateLock.unlock();
		}
	}

	// 賣出物品清單
	private final ArrayList<L1PrivateShopSellList> _sellList = new ArrayList<L1PrivateShopSellList>();

	/**
	 * 傳回賣出物品清單
	 * 
	 * @return
	 */
	public ArrayList<L1PrivateShopSellList> getSellList() {
		return this._sellList;
	}

	// 回收物品清單
	private final ArrayList<L1PrivateShopBuyList> _buyList = new ArrayList<L1PrivateShopBuyList>();
	private final ReentrantLock _privateShopListLock = new ReentrantLock();

	/**
	 * 傳回回收物品清單
	 * 
	 * @return
	 */
	public ArrayList<L1PrivateShopBuyList> getBuyList() {
		return this._buyList;
	}

	public ReentrantLock getPrivateShopListLock() {
		return this._privateShopListLock;
	}

	public void clearPrivateShopLists() {
		_privateShopListLock.lock();
		try {
			_sellList.clear();
			_buyList.clear();
		} finally {
			_privateShopListLock.unlock();
		}
	}

	private volatile byte[] _shopChat;

	/** 设置个人商店喊话内容 */
	public void setShopChat(final byte[] chat) {
		this._shopChat = chat == null ? null : chat.clone();
	}

	public byte[] getShopChat() {
		final byte[] chat = this._shopChat;
		return chat == null ? null : chat.clone();
	}

	private volatile boolean _isPrivateShop = false;
	private volatile int _privateShopPolyIndex = 0;

	/**
	 * 傳回商店模式
	 * 
	 * @return
	 */
	public boolean isPrivateShop() {
		return this._isPrivateShop;
	}

	public int getPrivateShopPolyIndex() {
		return this._privateShopPolyIndex;
	}

	/**
	 * 設置商店模式
	 * 
	 * @param flag
	 */
	public void setPrivateShop(final boolean flag) {
		// this._isPrivateShop = flag;
		setPrivateShop(flag, 0);
	}

	public void setPrivateShop(boolean flag, int polyIndex) {
		if (flag) {
			this._privateShopPolyIndex = (polyIndex >= 1 && polyIndex <= 8) ? polyIndex : 0;
			L1PolyMorph.doPolyPraivateShop(this, polyIndex);
		}
		if (!flag && this._isPrivateShop) {
			this._privateShopPolyIndex = 0;
			L1PolyMorph.undoPoly(this);
		}
		this._isPrivateShop = flag;
	}

	// 正在執行個人商店交易
	private volatile boolean _isTradingInPrivateShop = false;

	/**
	 * 正在執行個人商店交易
	 * 
	 * @return
	 */
	public boolean isTradingInPrivateShop() {
		return this._isTradingInPrivateShop;
	}

	/**
	 * 正在執行個人商店交易
	 * 
	 * @param flag
	 */
	public void setTradingInPrivateShop(final boolean flag) {
		this._isTradingInPrivateShop = flag;
	}

	private int _partnersPrivateShopItemCount = 0; // 出售物品種類數量

	/**
	 * 傳回出售物品種類數量
	 * 
	 * @return
	 */
	public int getPartnersPrivateShopItemCount() {
		return this._partnersPrivateShopItemCount;
	}

	/**
	 * 設置出售物品種類數量
	 * 
	 * @param i
	 */
	public void setPartnersPrivateShopItemCount(final int i) {
		this._partnersPrivateShopItemCount = i;
	}

	private volatile EncryptExecutor _out;// 封包加密管理；由連線執行緒安裝、各工作池讀取

	/**
	 * 設置封包加密管理
	 * 
	 * @param out
	 */
	public void setPacketOutput(final EncryptExecutor out) {
		this._out = out;
	}

	/**
	 * 發送單體封包
	 * 
	 * @param packet
	 *               封包
	 */
	public void sendPackets(final ServerBasePacket packet) {
		final EncryptExecutor out = _out;
		final ClientExecutor client = _netConnection;
		if (out == null || client == null) {
			com.lineage.server.utils.TeleportTrace.wire(packet, null, "DROP_NO_CONNECTION", null);
			if (_outboundDetachExpected) {
				EXPECTED_DETACHED_PACKET_COUNT.incrementAndGet();
			} else if (getOnlineStatus() == 1) {
				final long drops = ONLINE_PACKET_NO_CONNECTION_COUNT.incrementAndGet();
				if (drops <= 8L || (drops & (drops - 1L)) == 0L) {
					_log.warn("[PacketSafety] online player has no outbound connection: "
							+ "charId={} name={} packet={} count={}",
							getId(), getName(),
							packet != null ? packet.getDiagnosticContext() : "null", drops);
				}
			}
			return;
		}
		try {
			if (out.isActive()) com.lineage.server.model.npc.UiOperationContext.onSend(this, packet);
			out.encrypt(packet);
		} catch (final Exception e) {
			this.logout();
			this.close();
		}
	}

	/**
	 * 批次發送封包（同步送出，確保同一幀送達客戶端）
	 * 用於傷害數字多位數對齊顯示
	 *
	 * @param packets 封包集合
	 */
	public void sendPacketsBatch(final Collection<ServerBasePacket> packets) {
		final EncryptExecutor out = _out;
		final ClientExecutor client = _netConnection;
		if (out == null || client == null) {
			if (_outboundDetachExpected) {
				EXPECTED_DETACHED_PACKET_COUNT.incrementAndGet();
			} else if (getOnlineStatus() == 1) {
				final long drops = ONLINE_PACKET_NO_CONNECTION_COUNT.incrementAndGet();
				if (drops <= 8L || (drops & (drops - 1L)) == 0L) {
					_log.warn("[PacketSafety] online player has no outbound connection for batch: "
							+ "charId={} name={} packetCount={} count={}",
							getId(), getName(), packets != null ? packets.size() : 0, drops);
				}
			}
			return;
		}
		if (packets == null || packets.isEmpty()) return;
		try {
			if (out.isActive()) for (ServerBasePacket packet : packets) com.lineage.server.model.npc.UiOperationContext.onSend(this, packet);
			out.encryptBatch(packets);
		} catch (final Exception e) {
			this.logout();
			this.close();
		}
	}

	/**
	 * 批次發送給自己並廣播可見範圍（每位接收者一次 flush）。
	 * 封包實例在廣播路徑共用，payload 定稿後不可再變更。
	 */
	public void sendPacketsAllBatch(final Collection<ServerBasePacket> packets) {
		if (packets == null || packets.isEmpty()) {
			return;
		}
		try {
			final EncryptExecutor out = _out;
			if (out != null) {
				out.encryptBatch(packets);
			}
			if (!this.isGmInvis() && !this.isInvisble()) {
				WorldBroadcast.toVisiblePlayers(this, -1, packets);
			}
		} catch (final Exception e) {
			this.logout();
			this.close();
		}
	}

	/**
	 * 發送單體封包 與可見範圍發送封包
	 * 
	 * @param packet
	 *               封包
	 */
	public void sendPacketsAll(final ServerBasePacket packet) {
		try {
			final EncryptExecutor out = _out;
			if (out != null) {
				out.encrypt(packet);
			}
			if (!this.isGmInvis() && !this.isInvisble()) {
				this.broadcastPacketAll(packet);
			}

		} catch (final Exception e) {
			this.logout();
			this.close();
		}
	}

	/**
	 * 發送單體封包 與指定範圍發送封包(範圍8)
	 * 
	 * @param packet
	 *               封包
	 */
	public void sendPacketsX8(final ServerBasePacket packet) {
		try {
			final EncryptExecutor out = _out;
			if (out != null) {
				out.encrypt(packet);
			} else {
				com.lineage.server.utils.TeleportTrace.wire(packet, null, "DROP_NO_CONNECTION", null);
			}
			if (!this.isGmInvis() && !this.isInvisble()) {
				this.broadcastPacketX8(packet);
			}

		} catch (final Exception e) {
			this.logout();
			this.close();
		}
	}

	/**
	 * 發送單體封包 與指定範圍發送封包(範圍10)
	 * 
	 * @param packet
	 *               封包
	 */
	public void sendPacketsX10(final ServerBasePacket packet) {
		try {
			final EncryptExecutor out = _out;
			if (out != null) {
				out.encrypt(packet);
			}
			if (!this.isGmInvis() && !this.isInvisble()) {
				this.broadcastPacketX10(packet);
			}

		} catch (final Exception e) {
			this.logout();
			this.close();
		}
	}

	/**
	 * 發送單體封包 與可見指定範圍發送封包
	 * 
	 * @param packet
	 *               封包
	 * @param r
	 *               範圍
	 */
	/**
	 * 發送喝水特效封包（僅發給 potion_spr 開啟的玩家）
	 * 用於 UserHpr、UserAddHp 等藥水使用時的 S_SkillSound 廣播
	 */
	public void broadcastPotionEffect(final ServerBasePacket packet) {
		try {
			if (this.isGmInvis() || this.isInvisble()) {
				return;
			}
			// 自己（喝水者）
			final EncryptExecutor out = _out;
			if (this.isShowPotionEffect() && out != null) {
				out.encrypt(packet);
			}
			// 可見範圍內其他玩家（僅 potion_spr 開啟者）
			for (final L1PcInstance viewer : World.get().getVisiblePlayer(this, 8)) {
				if (viewer.get_showId() == this.get_showId() && viewer.isShowPotionEffect()) {
					viewer.sendPackets(packet);
				}
			}
		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	public void sendPacketsXR(final ServerBasePacket packet, final int r) {
		try {
			final EncryptExecutor out = _out;
			if (out != null) {
				out.encrypt(packet);
			}
			if (!this.isGmInvis() && !this.isInvisble()) {
				this.broadcastPacketXR(packet, r);
			}

		} catch (final Exception e) {
			this.logout();
			this.close();
		}
	}

	/**
	 * 關閉連線線程
	 */
	public void close() {
		final ClientExecutor client = _netConnection;
		if (client == null) {
			return; // 已經是離線狀態，不重複執行關閉
		}
		client.close();
	}

	/**
	 * 對該物件攻擊的調用
	 * 
	 * @param attacker
	 *                 攻擊方
	 */
	// @Override
	public void onAction(final L1PcInstance attacker) {
		// 避免 NullPointerException。onAction 參數型別是否應為 L1Character？
		if (attacker == null) {
			return;
		}
		// 傳送處理中
		if (this.isTeleport()) {
			return;
		}

		// 雙方之一 位於安全區域 僅送出動作資訊
		if (this.isSafetyZone() || attacker.isSafetyZone()) {
			PcPhysicalAttackFlow.executeMotionOnly(attacker, this);
			return;
		}

		// 禁止PK服務器 僅送出動作資訊
		if (this.checkNonPvP(this, attacker) == true) {
			PcPhysicalAttackFlow.executeMotionOnly(attacker, this);
			return;
		}

		if ((this.getCurrentHp() > 0) && !this.isDead()) {
			// 攻擊行為產生解除隱身
			attacker.delInvis();
			PcPhysicalAttackFlow.execute(attacker, this, true);
		}
	}

	/**
	 * 檢查是否可以攻擊
	 * 
	 * @param pc
	 * @param target
	 * @return
	 */
	public boolean checkNonPvP(final L1PcInstance pc, final L1Character target) {
		L1PcInstance targetpc = null;
		if (target instanceof L1PcInstance targetPc) {
			targetpc = targetPc;

		} else if (target instanceof L1PetInstance pet) {
			targetpc = (L1PcInstance) pet.getMaster();

		} else if (target instanceof L1SummonInstance summon) {
			targetpc = (L1PcInstance) summon.getMaster();
		}
		if (targetpc == null) {
			return false; // 對方非PC、召喚獸、寵物
		}
		// Targeting one's own companion through C_Attack or an offensive skill is
		// an explicit forced attack. Safety-zone checks remain at the caller.
		if (targetpc == pc
				&& (target instanceof L1PetInstance || target instanceof L1SummonInstance)) {
			return false;
		}

		if (!ConfigAlt.ALT_NONPVP) { // Non-PvP設定
			if (this.getMap().isCombatZone(this.getLocation())) {
				return false;
			}

			// 取回全部戰爭清單
			for (final L1War war : WorldWar.get().getWarList()) {
				if ((pc.getClanid() != 0) && (targetpc.getClanid() != 0)) { // 皆為血盟所屬
					final boolean same_war = war.checkClanInSameWar(pc.getClanname(), targetpc.getClanname());
					if (same_war == true) { // 參加同一戰爭
						return false;
					}
				}
			}
			// 非 PvP 設定下，戰爭期間內無須布告即可攻擊
			if (target instanceof L1PcInstance targetPc) {
				if (this.isInWarAreaAndWarTime(pc, targetPc)) {
					return false;
				}
			}
			return true;

		} else {
			return false;
		}
	}

	/**
	 * 戰爭旗幟座標內
	 * 
	 * @param pc
	 * @param target
	 * @return
	 */
	private boolean isInWarAreaAndWarTime(final L1PcInstance pc, final L1PcInstance target) {
		// pc 與 target 是否於戰爭中在戰爭區域
		final int castleId = L1CastleLocation.getCastleIdByArea(pc);
		final int targetCastleId = L1CastleLocation.getCastleIdByArea(target);
		if ((castleId != 0) && (targetCastleId != 0) && (castleId == targetCastleId)) {
			if (ServerWarExecutor.get().isNowWar(castleId)) {
				return true;
			}
		}
		return false;
	}

	private static boolean _debug = Config.DEBUG;

	/**
	 * 設置 寵物/召換獸/分身/護衛 攻擊目標
	 * 
	 * @param target
	 */
	public void setPetTarget(final L1Character target) {
		if (target == null) {
			return;
		}
		if (target.isDead()) {
			return;
		}
		final Map<Integer, L1NpcInstance> petList = this.getPetList();

		// 有寵物元素
		try {
			if (!petList.isEmpty()) {// 有寵物元素
				for (final Iterator<L1NpcInstance> iter = petList.values().iterator(); iter.hasNext();) {
					final L1NpcInstance pet = iter.next();
					if (pet != null) {
						if (pet instanceof L1PetInstance pets) {// 寵物
							pets.setMasterTarget(target);

						} else if (pet instanceof L1SummonInstance summon) {// 召換獸
							summon.setMasterTarget(target);
						}
					}
				}
			}

		} catch (final Exception e) {
			if (_debug) {
				_log.error(e.getLocalizedMessage(), e);
			}
		}

		final Map<Integer, L1IllusoryInstance> illList = this.get_otherList().get_illusoryList();

		// 有分身元素
		try {
			if (!illList.isEmpty()) {// 有分身元素
				// 控制分身攻擊
				if (this.getId() != target.getId()) {
					for (final Iterator<L1IllusoryInstance> iter = illList.values().iterator(); iter.hasNext();) {
						final L1IllusoryInstance ill = iter.next();
						if (ill != null) {
							ill.setLink(target);
						}
					}
				}
			}

		} catch (final Exception e) {
			if (_debug) {
				_log.error(e.getLocalizedMessage(), e);
			}
		}
	}

	/**
	 * 解除隱身術/暗隱術
	 */
	public void delInvis() {
		if (this.hasSkillEffect(INVISIBILITY)) { // 隱身術
			this.killSkillEffectTimer(INVISIBILITY);
			this.sendPackets(new S_Invis(this.getId(), 0));
			this.broadcastPacketAll(new S_OtherCharPacks(this));
		}
		if (this.hasSkillEffect(BLIND_HIDING)) { // 暗隱術
			this.killSkillEffectTimer(BLIND_HIDING);
			this.sendPackets(new S_Invis(this.getId(), 0));
			this.broadcastPacketAll(new S_OtherCharPacks(this));
		}
	}

	/**
	 * Removes only the invisibility leases captured by a deferred accepted hit.
	 * A recast that replaced either lease remains untouched.
	 */
	public void delInvisIfCurrent(final L1SkillTimer invisibilityLease,
			final L1SkillTimer blindHidingLease) {
		final boolean invisibilityRemoved =
				this.killSkillEffectTimer(INVISIBILITY, invisibilityLease);
		final boolean blindHidingRemoved =
				this.killSkillEffectTimer(BLIND_HIDING, blindHidingLease);
		if (invisibilityRemoved || blindHidingRemoved) {
			this.sendPackets(new S_Invis(this.getId(), 0));
			this.broadcastPacketAll(new S_OtherCharPacks(this));
		}
	}

	/**
	 * 解除暗隱術
	 */
	public void delBlindHiding() {
		this.killSkillEffectTimer(BLIND_HIDING);
		this.sendPackets(new S_Invis(this.getId(), 0));
		this.broadcastPacketAll(new S_OtherCharPacks(this));
	}

	/**
	 * 魔法具有属性傷害使用 (魔法抗性處理) attr:0.無属性魔法,1.地魔法,2.火魔法,4.水魔法,8.風魔法 (武器技能使用)
	 * 
	 * @param attacker
	 * @param damage
	 * @param attr
	 */
	public void receiveDamage(final L1Character attacker, double damage, final int attr) {
		final int player_mr = this.getMr();
		final int rnd = ThreadLocalRandom.current().nextInt(300) + 1;
		if (player_mr >= rnd) {
			damage /= 2.0;
		}

		int resist = switch (attr) {
			case L1Skills.ATTR_EARTH -> this.getEarth();
			case L1Skills.ATTR_FIRE -> this.getFire();
			case L1Skills.ATTR_WATER -> this.getWater();
			case L1Skills.ATTR_WIND -> this.getWind();
			default -> 0;
		};

		int resistFloor = (int) (0.32 * Math.abs(resist));
		if (resist >= 0) {
			resistFloor *= 1;

		} else {
			resistFloor *= -1;
		}

		final double attrDeffence = resistFloor / 32.0;

		double coefficient = (1.0 - attrDeffence + 3.0 / 32.0);// 0.09375

		if (coefficient > 0) {
			damage *= coefficient;
		}
		this.receiveDamage(attacker, damage, false, false);
	}

	/**
	 * 受攻擊mp減少計算
	 * 
	 * @param attacker
	 * @param mpDamage
	 */
	public void receiveManaDamage(final L1Character attacker, final int mpDamage) {
		commitManaDamage(attacker, mpDamage);
	}

	/** Atomically debits MP, then performs visibility/PvP side effects unlocked. */
	public int commitManaDamage(final L1Character attacker, final int mpDamage) {
		return commitManaDamageInternal(attacker, mpDamage, true);
	}

	/**
	 * MP drain paired with an HP hit already accepted by this target's FIFO lane.
	 * A fatal accepted hit may still drain the MP that was present at commit time;
	 * the actor credit happens later and never shares this character's lock.
	 */
	public int commitManaDamageAfterAcceptedHit(final L1Character attacker,
			final int mpDamage) {
		return commitManaDamageInternal(attacker, mpDamage, false);
	}

	private int commitManaDamageInternal(final L1Character attacker, final int mpDamage,
			final boolean requireAlive) {
		if (mpDamage <= 0) {
			return 0;
		}
		final int actual = requireAlive ? debitCurrentMpUpToIfAlive(mpDamage)
				: debitCurrentMpUpTo(mpDamage);
		if (actual <= 0) {
			return 0;
		}

		this.delInvis();
		if (attacker instanceof L1PcInstance attackerPc) {
			// [新增] 紀錄最後被玩家魔法攻擊的時間
			this.setLastPvpTime(System.currentTimeMillis());
			L1PinkName.onAction(this, attacker);
			if (attackerPc.isPinkName()) {
				CombatSideEffects.aggravateGuardsForPinkAttacker(attacker, attackerPc);
			}
		}
		return actual;
	}

	private final AtomicLong _lastMagicDamageNanos = new AtomicLong();

	private static final Map<Long, Double> _magicDamagerList = new HashMap<Long, Double>();

	/**
	 * 連續魔法減低損傷質預先載入 特殊定義道具 預先載入
	 */
	public static void load() {
		double newdmg = 100.00;
		for (long i = 2000; i > 0; i--) {
			if (i % 100 == 0) {
				newdmg -= 3.33;
			}
			_magicDamagerList.put(i, newdmg);
		}
	}

	/**
	 * 連續魔法減低損傷
	 * 
	 * @param damage
	 * @return
	 */
	public double isMagicDamager(final double damage) {
		if (damage < 0 || Double.isNaN(damage)) {
			return damage;
		}
		final long nowNanos = System.nanoTime();
		final long previousNanos = _lastMagicDamageNanos.getAndSet(nowNanos);
		final long elapsedNanos = previousNanos == 0L ? Long.MAX_VALUE : nowNanos - previousNanos;
		final long intervalMillis = elapsedNanos < 0L
				? Long.MAX_VALUE : elapsedNanos / 1_000_000L;
		final Double damagePercent = _magicDamagerList.get(intervalMillis);
		final double adjusted = damagePercent == null
				? damage : (damage * damagePercent) / 100.0D;
		return Math.max(adjusted, 0.0D);
	}

	/**
	 * 受攻擊hp減少計算
	 * 
	 * @param attacker
	 *                         攻擊者
	 * @param damage
	 *                         傷害
	 * @param isMagicDamage
	 *                         連續魔法傷害減低
	 * @param isCounterBarrier
	 *                         這個傷害是否不執行反饋 true:不執行反饋 false:執行反饋
	 */
	public void receiveDamage(final L1Character attacker, double damage, final boolean isMagicDamage,
			final boolean isCounterBarrier) {
		receiveDamageInternal(attacker, damage, isMagicDamage, isCounterBarrier, 0,
				DamageCommitPlan.none());
	}

	/** Carries immutable hit effects into this target's damage-commit lane. */
	public void receiveDamage(final L1Character attacker, final double damage,
			final boolean isMagicDamage, final boolean isCounterBarrier,
			final DamageCommitPlan commitPlan) {
		receiveDamageInternal(attacker, damage, isMagicDamage, isCounterBarrier, 0,
				commitPlan);
	}

	/** Chaser-style damage retains normal Calc side effects but cannot reduce HP below one. */
	public void receiveNonlethalDamage(final L1Character attacker, final double damage) {
		receiveDamageInternal(attacker, damage, false, false, 1,
				DamageCommitPlan.none());
	}

	private void receiveDamageInternal(final L1Character attacker, double damage,
			final boolean isMagicDamage, final boolean isCounterBarrier, final int minimumHp,
			final DamageCommitPlan commitPlan) {
		// System.out.println("投石器计算的伤害：" + isMagicDamage);
		if ((this.getCurrentHp() > 0) && !this.isDead()) {

			com.lineage.server.model.proc.ProcDefenseResult defenseSideEffects = null;
			DamageCommitPlan legacyDefenseFeedback = DamageCommitPlan.none();
			DamageCommitPlan incomingHitEffects = DamageCommitPlan.none();
			final boolean suppressCombatFeedback = isCounterBarrier
					|| com.lineage.server.model.proc.UnifiedProcExecutor.isApplyingProcSideEffects();

			if (attacker != null) {
				final boolean perceiveAttacker = attacker != this
						&& !(attacker instanceof L1EffectInstance)
						&& !this.knownsObject(attacker)
						&& attacker.getMapId() == this.getMapId();

				// 連續魔法傷害減低
				if (isMagicDamage == true) {
					damage = this.isMagicDamager(damage);
				}

				// 攻擊者定義
				L1PcInstance attackPc = null;
				L1NpcInstance attackNpc = null;

				if (attacker instanceof L1PcInstance pc) {
					attackPc = pc;// 攻擊者為PC

				} else if (attacker instanceof L1NpcInstance npc) {
					attackNpc = npc;// 攻擊者為NPC
				}
				final boolean positiveIncomingHit = damage > 0;
				final L1SkillTimer invisibilityLease = positiveIncomingHit
						? captureSkillEffectLease(INVISIBILITY) : null;
				final L1SkillTimer blindHidingLease = positiveIncomingHit
						? captureSkillEffectLease(BLIND_HIDING) : null;
				final L1SkillTimer fogOfSleepingLease = positiveIncomingHit
						? captureSkillEffectLease(FOG_OF_SLEEPING) : null;

				// 傷害大於等於0(小於0回復HP)
				if (positiveIncomingHit) {
					// Counter Barrier 與非反應型 Proc 傷害仍可被減傷，但不可再觸發
					// REFLECT／回復／legacy feedback，否則雙方反傷會無限產生 DamageEvent。
					try {
						defenseSideEffects = com.lineage.server.model.proc.UnifiedProcExecutor.calcOnDefense(
								this, attacker, (int) damage);
						if (suppressCombatFeedback) {
							defenseSideEffects = defenseSideEffects.reductionOnly();
						}
						damage -= defenseSideEffects.damageReduced();
						if (damage < 0) {
							damage = 0;
						}
					} catch (final Exception e) {
						_log.error("統一 Proc 受傷 Calc 失敗", e);
					}
				}

				incomingHitEffects = new PcIncomingHitCommitPlan(
						this, attacker, attackPc, perceiveAttacker, positiveIncomingHit,
						invisibilityLease, blindHidingLease, fogOfSleepingLease);

				if (!suppressCombatFeedback && this.getId() != attacker.getId()
						&& damage > 0) {
					int mortalBodyDamage = 0;
					if (this.hasSkillEffect(MORTAL_BODY)) {
						final L1Magic mbMagic = new L1Magic(this, attacker);
						if (mbMagic.calcProbabilityMagic(MORTAL_BODY)) {
							final int base = ConfigSkill.MORTAL_BODY_DMG
									+ sumMortalBodyReflectWeaponEnchant(attacker);
							if (attackPc != null) {
								mortalBodyDamage = attackPc.calcMortalBodyReflectDamageTaken(base);
							} else if (attackNpc != null) {
								mortalBodyDamage = Math.max(1, base);
							}
						}
					}

					final L1SkillTimer joyLease = attacker.captureSkillEffectLease(JOY_OF_PAIN);
					int joyDamage = 0;
					if (joyLease != null) {
						final int hpup = this.get_other().get_addhp();
						joyDamage = (getMaxHp() - getCurrentHp() - hpup)
								/ ConfigSkill.JOY_OF_PAIN_COUNTDMG;
						joyDamage = Math.min(joyDamage, ConfigSkill.JOY_OF_PAIN_DMG);
					}
					final LegacyDefenseFeedbackPlan feedback = new LegacyDefenseFeedbackPlan(
							this, attacker, mortalBodyDamage, joyDamage, joyLease);
					if (!feedback.isEmpty()) {
						legacyDefenseFeedback = feedback;
					}
				}
			}

			// 裝備使自己傷害加深的裝備
			if (this.getInventory().checkEquipped(145) // 狂戰士斧
					|| this.getInventory().checkEquipped(149)) { // 牛人斧頭
				damage *= 1.5; // 傷害提高1.5倍
			}

			// 幻覺：化身219 (由幻術師_技能設定表外控)
			if (this.hasSkillEffect(ILLUSION_AVATAR)) {
				damage *= ConfigSkill.ILLUSION_AVATAR_DAMAGE_RATE;
			}

			// C-2 Calc 完成 → Commit 佇列（HP / 死亡 / 封包副作用見 processDamageCommit）
			final DamageCommitPlan acceptedPlan = CompositeDamageCommitPlan.of(
					incomingHitEffects,
					CompositeDamageCommitPlan.of(legacyDefenseFeedback, commitPlan));
			final DamageEvent damageEvent = minimumHp > 0
					? DamageEvent.nonLethal(attacker, this, (int) damage, 0, isMagicDamage,
							isCounterBarrier, defenseSideEffects, acceptedPlan, minimumHp)
					: new DamageEvent(attacker, this, (int) damage, 0, isMagicDamage,
							isCounterBarrier, defenseSideEffects, acceptedPlan);
			DamageCommit.enqueue(damageEvent);

		} else if (!this.isDead()) {
			_log.error("人物hp減少處理失敗 可能原因: 初始hp為0");
			DamageCommit.enqueue(new DamageEvent(attacker, this, this.getCurrentHp(), 0, isMagicDamage, isCounterBarrier));
		}
	}

	/**
	 * C-2：DamageCommit FIFO 消化入口（Commit → SideEffects）。
	 */
	public DamageCommitOutcome processDamageCommit(final DamageEvent event) {
		if (event == null) {
			return DamageCommitOutcome.rejected(0, getCurrentHp());
		}
		final L1Character attacker = event.attacker();
		final int damage = event.damage();

		// Resolve immutable Proc plans before taking the target lock. Applying HP/MP
		// recovery, reflect, skills, GFX, or attacker damage remains below, outside
		// the target critical section, so X→Y and Y→X cannot invert two locks.
		final com.lineage.server.model.proc.ProcDefenseResult defenseSideEffects =
				(this.getCurrentHp() > 0 && !this.isDead())
						? com.lineage.server.model.proc.UnifiedProcExecutor.resolveDeferredDefenseProcs(
								this, attacker, event.defenseProc())
						: event.defenseProc();
		final boolean[] committed = { false };
		final boolean[] hpChanged = { false };
		final boolean[] invalidZeroHp = { false };
		final int[] hpBefore = { getCurrentHp() };
		final int[] committedHp = { 0 };
		final long[] deathGeneration = { 0L };

		PlayerActionLock.run(this, () -> {
			if (getCurrentHp() > 0 && !isDead()) {
				committed[0] = true;
				final int oldHp = getCurrentHp();
				hpBefore[0] = oldHp;
				long candidate = (long) oldHp - (long) damage;
				candidate = Math.min(candidate, (long) getMaxHp());
				final int hpFloor = Math.max(0,
						Math.min(oldHp, Math.min(event.minimumHp(), getMaxHp())));
				int newHp;
				boolean triggerDeath = false;
				if (candidate <= hpFloor) {
					if (hpFloor > 0) {
						newHp = hpFloor;
					} else if (isGm()) {
						newHp = getMaxHp();
					} else {
						newHp = 0;
						triggerDeath = true;
					}
				} else {
					newHp = (int) Math.min((long) Integer.MAX_VALUE, candidate);
				}

				committedHp[0] = newHp;
				if (oldHp != newHp) {
					setCurrentHpDirect(newHp);
					hpChanged[0] = true;
				}
				if (triggerDeath) {
					deathGeneration[0] = beginDeathStateLocked();
				}
			} else if (!isDead()) {
				invalidZeroHp[0] = true;
				deathGeneration[0] = beginDeathStateLocked();
			}
		});

		// Network, party, AI/follow, and all cross-character Proc work stays outside
		// the target-local atomic HP/dead transition.
		// Packet/party callbacks must not suppress the only terminal death handoff.
		if (committed[0] && damage > 0) {
			HitReactionSyncDiagnostics.recordDamage(this, attacker, damage,
					hpBefore[0], committedHp[0], event.magicDamage(),
					deathGeneration[0] != 0L);
		}
		if (deathGeneration[0] != 0L) {
			finishDeathStart(attacker, deathGeneration[0]);
		}
		if (hpChanged[0]) {
			try {
				sendPackets(new S_HPUpdate(committedHp[0], getMaxHp()));
			} catch (final RuntimeException packetFailure) {
				_log.error("Player HP update failed after damage commit: pc={}", getName(), packetFailure);
			}
			try {
				final L1Party party = getParty();
				if (party != null) {
					party.updateMiniHP(this);
				}
			} catch (final RuntimeException partyFailure) {
				_log.error("Party HP update failed after damage commit: pc={}", getName(), partyFailure);
			}
		}
		if (invalidZeroHp[0]) {
			_log.error("人物hp減少處理失敗 可能原因: 初始hp為0");
		}
		if (committed[0]) {
			// HP acceptance is already final.  A packet/proc callback must never
			// prevent DamageCommitApplier from receiving the accepted outcome and
			// releasing the remaining immutable hit plan.
			try {
				com.lineage.server.model.proc.UnifiedProcExecutor.applyDefenseSideEffects(
						this, attacker, defenseSideEffects);
			} catch (final RuntimeException procFailure) {
				_log.error("Accepted player defense proc failed: pc={}", getName(), procFailure);
			}
			if (damage > 0 && this.isMining()) {
				try {
					this.setMining(false);
					this.sendPackets(new S_ServerMessage("遭受攻擊，停止挖礦。"));
				} catch (final RuntimeException miningFailure) {
					_log.error("Accepted hit mining-stop effect failed: pc={}",
							getName(), miningFailure);
				}
			}
		}
		return committed[0]
				? DamageCommitOutcome.accepted(damage, hpBefore[0], committedHp[0],
						deathGeneration[0] != 0L)
				: DamageCommitOutcome.rejected(damage, getCurrentHp());
	}

	/**
	 * 死亡的處理
	 * 
	 * @param lastAttacker
	 *                     攻擊致死的攻擊者
	 */
	public void death(final L1Character lastAttacker) {
		setMining(false);
		final long generation = PlayerActionLock.call(this, this::beginDeathStateLocked);
		if (generation == 0L) {
			return;
		}
		finishDeathStart(lastAttacker, generation);
	}

	/** Caller owns this PC's action lock. No cross-character work is allowed here. */
	private long beginDeathStateLocked() {
		if (isDead()) {
			return 0L;
		}
		final long generation = _deathPipelineGeneration.incrementAndGet();
		if (!_deathLifecycle.markDeadPending(generation)) {
			return 0L;
		}
		if (!isGm() && getCurrentHp() != 0) {
			setCurrentHpDirect(0);
		}
		setNowTarget(null);
		setDead(true);
		setStatus(ActionCodes.ACTION_Die);
		return generation;
	}

	/** Runs only after the target-local HP/dead critical section has been released. */
	private void finishDeathStart(final L1Character lastAttacker, final long generation) {
		if (!isCurrentDeathGeneration(generation)) {
			return;
		}
		final DeathLifecycleGate.Ownership lifecycle =
				_deathLifecycle.ownership(generation);
		if (!lifecycle.tryClaim()) {
			return;
		}
		try {
			if (isAiRunning()) {
				stopAIForDeath();
			}
		} catch (final RuntimeException e) {
			_log.error("Player death AI stop failed: pc={}", getName(), e);
		}
		try {
			if (isFollowRunning() || getFollowMaster() != null || !getFollowers().isEmpty()) {
				stopFollow();
			}
		} catch (final RuntimeException e) {
			_log.error("Player death follow stop failed: pc={}", getName(), e);
		}
		try {
			com.lineage.server.model.L1DeathMatch.getInstance().onPlayerDeath(this);
		} catch (final RuntimeException e) {
			// Ancillary event bookkeeping must not suppress the only death handoff.
			_log.error("Player death-match callback failed: pc={}", getName(), e);
		}

		// ========================================================================
		// [修正] 擴充假人判定 (支援假人的寵物/召喚獸殺人)
		// ========================================================================
		try {
			L1Character killer = lastAttacker;

			// 如果是寵物或召喚獸，嘗試取得主人
			if (lastAttacker instanceof L1PetInstance) {
				L1PetInstance pet = (L1PetInstance) lastAttacker;
				if (pet.getMaster() != null)
					killer = pet.getMaster();
			} else if (lastAttacker instanceof L1SummonInstance) {
				L1SummonInstance summon = (L1SummonInstance) lastAttacker;
				if (summon.getMaster() != null)
					killer = summon.getMaster();
			}

			// 如果最終兇手是假人，執行廣播與計數
			if (killer instanceof L1DeInstance) {
				// 假人殺人廣播
				ConfigMsg.announceKill(killer.getName(), this.getName());
				this.get_other().add_deathCount(1);
			}
		} catch (final RuntimeException e) {
			// Kill-feed bookkeeping is not allowed to strand the dead-state marker.
			_log.error("Player death kill-feed callback failed: pc={}", getName(), e);
		}
		// ========================================================================

		final Death fullDeath = new Death(lastAttacker, generation, lifecycle);
		final AtomicBoolean deferredPrepared = new AtomicBoolean();
		PC_DEATH_HANDOFFS.submit(new PcDeathHandoffQueue.DeathWork() {
			@Override
			public boolean isCurrent() {
				return isCurrentDeathGeneration(generation);
			}

			@Override
			public boolean shouldDefer() {
				return L1PcInstance.this.isTeleport();
			}

			@Override
			public void onDeferred(final String reason) {
				if (deferredPrepared.compareAndSet(false, true)) {
					prepareDeferredDeathState(generation);
				}
			}

			@Override
			public void runFull() {
				fullDeath.run();
			}

			@Override
			public void runMinimal(final String reason, final Throwable fullFailure) {
				applyMinimalDeathTerminal(generation, reason, fullFailure);
			}

			@Override
			public DeathLifecycleGate.Ownership lifecycleOwnership() {
				return lifecycle;
			}

			@Override
			public boolean autoCompleteLifecycle() {
				return false;
			}
		});
	}

	private boolean isCurrentDeathGeneration(final long generation) {
		return generation != 0L && _deathPipelineGeneration.get() == generation && isDead();
	}

	/** Cheap, idempotent state used while the full SlowPool workflow is retained. */
	private void prepareDeferredDeathState(final long generation) {
		if (!isCurrentDeathGeneration(generation)) {
			return;
		}
		setCurrentHpDirect(0);
		setGresValid(false);
		stopHpRegeneration();
		stopMpRegeneration();
		set_delete_time(300);
	}

	/**
	 * Last-resort terminal state. This path deliberately performs no JDBC, item
	 * drop, doll deletion, skill cancellation, or death-penalty calculation.
	 */
	private void applyMinimalDeathTerminal(final long generation, final String reason,
			final Throwable fullFailure) {
		if (!isCurrentDeathGeneration(generation)) {
			return;
		}
		prepareDeferredDeathState(generation);
		setNowTarget(null);
		setStatus(ActionCodes.ACTION_Die);
		sendPackets(new S_HPUpdate(getCurrentHp(), getMaxHp()));
		sendPacketsAll(new S_DoActionGFX(getId(), ActionCodes.ACTION_Die));
		if (fullFailure != null) {
			_log.error("Player death full workflow failed; minimal terminal state applied: pc={} reason={}",
					getName(), reason, fullFailure);
		} else {
			_log.error("Player death pressure fallback applied: pc={} reason={}", getName(), reason);
		}
	}

	/** Terminal death-state fallback is also dispatched; a timer never applies it inline. */
	private void dispatchDelayedDeathMinimal(final long generation, final String reason,
			final Throwable failure, final DeathLifecycleGate.Ownership lifecycle,
			final int attempt) {
		if (!isCurrentDeathGeneration(generation)) {
			return;
		}
		if (LogicThreadPool.get().tryExecute(
				() -> {
					if (!lifecycle.tryClaim()) {
						return;
					}
					try {
						applyMinimalDeathTerminal(generation, reason, failure);
					} finally {
						lifecycle.complete(true);
					}
				})) {
			return;
		}
		DELAYED_DEATH_MINIMAL_REJECTED_COUNT.incrementAndGet();
		if (attempt >= DELAYED_TERMINAL_MAX_ATTEMPTS || LogicThreadPool.get().isShutdown()) {
			_log.error("Player delayed death terminal dispatch exhausted: pc={} generation={} reason={} attempt={}",
					getName(), generation, reason, attempt, failure);
			return;
		}
		final long retryDelay = Math.min(500L, 25L << Math.min(attempt, 4));
		final ScheduledFuture<?> retry = pcScheduleTask("DelayedDeathMinimalClock",
				() -> dispatchDelayedDeathMinimal(generation, reason, failure,
						lifecycle, attempt + 1),
				retryDelay);
		if (retry == null) {
			_log.error("Player delayed death terminal retry clock unavailable: pc={} generation={} reason={}",
					getName(), generation, reason, failure);
		}
	}

	/**
	 * 人物死亡的處理
	 * 
	 * @author dexc
	 * 
	 */
	private class Death implements Runnable {
		private L1Character _lastAttacker;
		private final long _generation;
		private final DeathLifecycleGate.Ownership _lifecycle;

		private Death(final L1Character cha, final long generation,
				final DeathLifecycleGate.Ownership lifecycle) {
			this._lastAttacker = cha;
			this._generation = generation;
			this._lifecycle = lifecycle;
		}

		// @Override
		public void run() {
			if (!_lifecycle.isOwned() || !isCurrentDeathGeneration(_generation)) {
				_lifecycle.complete(false);
				return;
			}
			final L1Character lastAttacker = this._lastAttacker;
			this._lastAttacker = null;
			final boolean deferredHpWasAlreadyZero = L1PcInstance.this.getCurrentHp() == 0;
			L1PcInstance.this.setCurrentHp(0);
			if (deferredHpWasAlreadyZero) {
				L1PcInstance.this.sendPackets(new S_HPUpdate(0, L1PcInstance.this.getMaxHp()));
			}
			L1PcInstance.this.setGresValid(false); // 經驗 loss 期間 G-RES 無效

			// [優化] 已由開頭檢查與重新排程承載節拍，迴圈內無阻塞延遲
			final L1Party deathParty = L1PcInstance.this.getParty();
			if (deathParty != null) {// 隊伍中
				for (final L1PcInstance member : deathParty.partyUsers().values()) {
					member.sendPackets(new S_PacketBoxParty(deathParty, L1PcInstance.this));
				}
			}
			// 加入死亡清單
			set_delete_time(300);

			// 娃娃刪除
			if (!getDolls().isEmpty()) {
				for (Object obj : getDolls().values().toArray()) {
					final L1DollInstance doll = (L1DollInstance) obj;
					doll.deleteDoll();
				}
			}

			L1PcInstance.this.stopHpRegeneration();
			L1PcInstance.this.stopMpRegeneration();

			final int targetobjid = L1PcInstance.this.getId();

			// 死亡不寫回地圖 byte；佔位由 World 移除與 isDead 決定
			// 死亡時具有變身狀態
			int tempchargfx = 0;
			if (L1PcInstance.this.hasSkillEffect(SHAPE_CHANGE)) {
				tempchargfx = L1PcInstance.this.getTempCharGfx();
				L1PcInstance.this.setTempCharGfxAtDead(tempchargfx);

			} else {
				L1PcInstance.this.setTempCharGfxAtDead(L1PcInstance.this.getClassId());
			}

			// 死亡時 現有技能消除
			final L1SkillUse l1skilluse = new L1SkillUse();
			l1skilluse.handleCommands(L1PcInstance.this, CANCELLATION, L1PcInstance.this.getId(),
					L1PcInstance.this.getX(), L1PcInstance.this.getY(), 0, L1SkillUse.TYPE_LOGIN);

			// 暗影系變身中死亡會使客戶端崩潰，暫定處理
			if ((tempchargfx == 5727) || (tempchargfx == 5730) || (tempchargfx == 5733) || (tempchargfx == 5736)) {
				tempchargfx = 0;
			}

			if (tempchargfx == 7351) {
				tempchargfx = L1PcInstance.this.getClassId();
				L1PcInstance.this.setTempCharGfx(tempchargfx);
			}

			if (tempchargfx != 0) {
				// System.out.println("tempchargfx: " + tempchargfx);
				L1PcInstance.this.sendPacketsAll(new S_ChangeShape(L1PcInstance.this, tempchargfx));
				// 繼續執行後續邏輯
				boolean completed = false;
				try {
					continueDeathProcess(lastAttacker, targetobjid);
					completed = true;
				} finally {
					_lifecycle.complete(completed);
				}
			} else {
				// [優化] 暗影系變身中攻擊時死亡會使客戶端崩潰，加入延遲
				// PcScheduler owns the one-second animation clock only. The remaining
				// death workflow can perform durable/world work and must re-enter SlowPool.
				delayedSlowHandoff("ContinueDeathProcess",
						() -> _lifecycle.isOwned()
								&& isCurrentDeathGeneration(_generation),
						() -> {
							boolean completed = false;
							try {
								continueDeathProcess(lastAttacker, targetobjid);
								completed = true;
							} catch (final RuntimeException | Error failure) {
								_lifecycle.complete(false);
								dispatchDelayedDeathMinimal(_generation,
										"continuation-work-failed", failure,
										_lifecycle, 0);
								throw failure;
							} finally {
								if (completed) {
									_lifecycle.complete(true);
								}
							}
						},
						() -> { },
						(reason, failure) -> {
							_lifecycle.complete(false);
							dispatchDelayedDeathMinimal(_generation,
									"continuation-" + reason, failure, _lifecycle, 0);
						})
						.start(1_000L);
			}
		}

		/**
		 * 繼續執行死亡處理的後續邏輯
		 */
		private void continueDeathProcess(final L1Character lastAttacker, final int targetobjid) {
			boolean isSafetyZone = false;// 是否為安全區中

			boolean isCombatZone = false;// 是否為戰鬥區中

			boolean isWar = false;// 是否參戰

			if (L1PcInstance.this.isSafetyZone()) {
				isSafetyZone = true;
			}
			if (L1PcInstance.this.isCombatZone()) {
				isCombatZone = true;
			}

			// 殺人次數的減少
			if (lastAttacker instanceof L1GuardInstance) {
				if (L1PcInstance.this.get_PKcount() > 0) {
					L1PcInstance.this.set_PKcount(L1PcInstance.this.get_PKcount() - 1);
				}
				L1PcInstance.this.setLastPk(null);
			}

			if (lastAttacker instanceof L1GuardianInstance) {
				if (L1PcInstance.this.getPkCountForElf() > 0) {
					L1PcInstance.this.setPkCountForElf(L1PcInstance.this.getPkCountForElf() - 1);
				}
				L1PcInstance.this.setLastPkForElf(null);
			}

			// 檢查攻擊者是否為PC(寵物 定義為主人)
			L1PcInstance fightPc = null;

			if (lastAttacker instanceof L1PcInstance pc) {// 攻擊者是玩家
				fightPc = pc;

			} else if (lastAttacker instanceof L1PetInstance npc) {// 攻擊者是寵物
				// [修正] 增加 instanceof 檢查，避免假人主人導致崩潰
				if (npc.getMaster() != null && npc.getMaster() instanceof L1PcInstance) {
					fightPc = (L1PcInstance) npc.getMaster();
				}

			} else if (lastAttacker instanceof L1SummonInstance npc) {// 攻擊者是 召換獸
				// [修正] 增加 instanceof 檢查
				if (npc.getMaster() != null && npc.getMaster() instanceof L1PcInstance) {
					fightPc = (L1PcInstance) npc.getMaster();
				}

			} else if (lastAttacker instanceof L1IllusoryInstance npc) {// 攻擊者是 分身
				// [修正] 增加 instanceof 檢查
				if (npc.getMaster() != null && npc.getMaster() instanceof L1PcInstance) {
					fightPc = (L1PcInstance) npc.getMaster();
				}

			} else if (lastAttacker instanceof L1EffectInstance npc) {// 攻擊者是 技能物件
				// [修正] 增加 instanceof 檢查
				if (npc.getMaster() != null && npc.getMaster() instanceof L1PcInstance) {
					fightPc = (L1PcInstance) npc.getMaster();
				}
			}

			L1PcInstance.this.sendPacketsAll(new S_DoActionGFX(targetobjid, ActionCodes.ACTION_Die));

			if (fightPc != null) {
				// 決鬥中
				if ((L1PcInstance.this.getFightId() == fightPc.getId())
						&& (fightPc.getFightId() == L1PcInstance.this.getId())) {
					L1PcInstance.this.setFightId(0);
					L1PcInstance.this.sendPackets(new S_PacketBox(S_PacketBox.MSG_DUEL, 0, 0));
					fightPc.setFightId(0);
					fightPc.sendPackets(new S_PacketBox(S_PacketBox.MSG_DUEL, 0, 0));
					return;
				}

				// 效果: 被超過10級以上的玩家攻擊而死亡時，不會失去經驗值，也不會掉落物品
				if (isEncounter()) {// 遭遇的守護
					if (fightPc.getLevel() > getLevel()) {
						if ((fightPc.getLevel() - getLevel()) >= 10) {
							return;
						}
					}
				}

				// 攻城戰爭進行狀態
				if (L1PcInstance.this.castleWarResult()) {
					isWar = true;
				}

				// 血盟戰爭進行狀態
				if (L1PcInstance.this.simWarResult(lastAttacker)) {
					isWar = true;
				}

				// 攻城戰進行狀態
				if (L1PcInstance.this.isInWarAreaAndWarTime(L1PcInstance.this, fightPc)) {
					isWar = true;
				}

				// 死亡公告
				if (L1PcInstance.this.getLevel() >= 10) {
					if (!fightPc.isGm()) {
						boolean isShow = false;// 是否公告
						if (isWar) {// 戰爭中
							isShow = true;

						} else {// 非戰爭中
							// 非戰鬥區
							if (!isCombatZone) {
								isShow = true;
							}
						}
						if (isShow) {
							// [修改] 呼叫新的廣播系統 (支援 訊息欄/畫面中央/雙重)
							// 參數: 勝利者(兇手), 失敗者(死者)
							// 注意: 必須使用 L1PcInstance.this 來指涉外部的玩家物件
							ConfigMsg.announceKill(fightPc.getName(), L1PcInstance.this.getName());

							// [保留] 更新殺/死次數
							fightPc.get_other().add_killCount(1);
							L1PcInstance.this.get_other().add_deathCount(1);
						}
					}
				}
			}

			// 安全區中
			if (isSafetyZone) {
				_log.info("[死亡懲罰] 略過 name={} 原因=安全區 mapId={}", L1PcInstance.this.getName(),
						L1PcInstance.this.getMapId());
				return;
			}
			// 戰鬥區中
			if (isCombatZone) {
				_log.info("[死亡懲罰] 略過 name={} 原因=戰鬥區 mapId={}", L1PcInstance.this.getName(),
						L1PcInstance.this.getMapId());
				return;
			}
			// 死亡逞罰
			if (!L1PcInstance.this.getMap().isEnabledDeathPenalty()) {
				_log.info("[死亡懲罰] 略過 name={} 原因=地圖未啟用死亡懲罰 mapId={}", L1PcInstance.this.getName(),
						L1PcInstance.this.getMapId());
				return;
			}

			// 死亡保護 (1=經驗, 2=道具, 4=技能)：不可消耗型整類擋下；可消耗型依「每次掉落／每個技能」各扣 1
			final int deathSafeNonConsumeMask = L1PcInstance.this.computeDeathSafeNonConsumeMask();
			final boolean[] deathSafeNonConsumeNotified = { false };

			_log.info(
					"[死亡懲罰] 進入 name={} mapId={} lawful={} 死亡保護(不可消耗)mask={}(_skillList.size={})",
					L1PcInstance.this.getName(), L1PcInstance.this.getMapId(), L1PcInstance.this.getLawful(),
					deathSafeNonConsumeMask, _skillList.size());

			// 正義質未滿
			if (L1PcInstance.this.getLawful() < 32767) {
				if ((deathSafeNonConsumeMask & 2) != 0) {
					_log.info("[死亡懲罰] 道具整類擋下(不可消耗保護) name={}", L1PcInstance.this.getName());
					L1PcInstance.this.sendDeathSafeNonConsumeOnce(deathSafeNonConsumeNotified);
				} else {
					for (final L1ItemInstance item : L1PcInstance.this.getInventory().getAlwaysDropItemsForDeath()) {
						if (L1PcInstance.this.tryConsumeOneDeathSafeConsumable(2)) {
							continue;
						}
						L1PcInstance.this.dropDeathPenaltyItemToGround(item);
					}
					for (final L1ItemInstance item : L1PcInstance.this.getInventory().getAlwaysVanishItemsForDeath()) {
						if (L1PcInstance.this.tryConsumeOneDeathSafeConsumable(2)) {
							continue;
						}
						L1PcInstance.this.vanishDeathPenaltyItem(item);
					}
					this.lostRate();
				}
				if ((deathSafeNonConsumeMask & 4) != 0) {
					_log.info("[死亡懲罰] 技能整類擋下(不可消耗保護) name={}", L1PcInstance.this.getName());
					L1PcInstance.this.sendDeathSafeNonConsumeOnce(deathSafeNonConsumeNotified);
				} else {
					this.lostSkillRate();
				}
			} else {
				_log.info("[死亡懲罰] 道具/技能懲罰略過 name={} 原因=lawful>={}", L1PcInstance.this.getName(), 32767);
			}

			if ((deathSafeNonConsumeMask & 1) != 0) {
				L1PcInstance.this.sendDeathSafeNonConsumeOnce(deathSafeNonConsumeNotified);
				L1PcInstance.this.setGresValid(true);
				if (L1PcInstance.this.getExpRes() == 0) {
					L1PcInstance.this.setExpRes(1);
				}
			} else if (L1PcInstance.this.tryConsumeOneDeathSafeConsumable(1)) {
				L1PcInstance.this.setGresValid(true);
				if (L1PcInstance.this.getExpRes() == 0) {
					L1PcInstance.this.setExpRes(1);
				}
			} else {
				this.expRate();
			}

			// 參戰中
			if (isWar) {
				return;
			}

			final boolean castle_area = L1CastleLocation.checkInAllWarArea(getX(), getY(), getMapId());
			if (castle_area) {// 戰爭旗中
				return;
			}

			if (fightPc != null) {
				if (fightPc.getClan() != null && getClan() != null) {
					if (WorldWar.get().isWar(fightPc.getClan().getClanName(), getClan().getClanName())) {
						return;
					}
				}
				if (fightPc.isSafetyZone()) {
					return;
				}
				if (fightPc.isCombatZone()) {
					return;
				}
				if ((L1PcInstance.this.getLawful() >= 0) && (L1PcInstance.this.isPinkName() == false)) {
					boolean isChangePkCount = false;
					// boolean isChangePkCountForElf = false;
					// 性向值未達30000時增加PK計數
					if (fightPc.getLawful() < 30000) {
						fightPc.set_PKcount(fightPc.get_PKcount() + 1);
						isChangePkCount = true;
						if (fightPc.isElf() && L1PcInstance.this.isElf()) {
							fightPc.setPkCountForElf(fightPc.getPkCountForElf() + 1);
							// isChangePkCountForElf = true;
						}
					}
					fightPc.setLastPk();
					if (fightPc.isElf() && L1PcInstance.this.isElf()) {
						fightPc.setLastPkForElf();
					}

					// 性向值處理
					// 依官方公告及各等級PK調整
					// （依PK方等級，越高風險越大）
					// 約48級-8k，DK時約10k
					// 60 級約 20k 強，65 級約 30k 弱
					int lawful;

					if (fightPc.getLevel() < 50) {
						// lawful = -1 * (int) ((Math.pow(fightPc.getLevel(), 2) * 4));
						lawful = -1 * (((int) Math.pow(fightPc.getLevel(), 2)) << 2);

					} else {
						lawful = -1 * (int) ((Math.pow(fightPc.getLevel(), 3) * 0.08));
					}
					// 若（原始正義值-1000）低於計算結果
					// 則以原始正義值-1000 作為正義值
					// （根據連續 PK 時數值幾乎不變的記憶推測）
					// 此部分比上方公式的信心度低，僅為模糊記憶
					// 若確認不會有此情況，請協助修正
					if ((fightPc.getLawful() - 1000) < lawful) {
						lawful = fightPc.getLawful() - 1000;
					}

					if (lawful <= -32768) {
						lawful = -32768;
					}
					fightPc.setLawful(lawful);

					fightPc.sendPacketsAll(new S_Lawful(fightPc));

					if (ConfigAlt.ALT_PUNISHMENT) {
						if (isChangePkCount && (fightPc.get_PKcount() >= 5) && (fightPc.get_PKcount() < 100)) {
							// 你的PK次數已達%0。達%1次將下地獄。
							fightPc.sendPackets(new S_BlueMessage(551, String.valueOf(fightPc.get_PKcount()), "100"));

						} else if (isChangePkCount && (fightPc.get_PKcount() >= 100)) {
							fightPc.beginHell(true);
						}
					}

				} else {
					setPinkName(false);
				}
			}
			// [新增] 離線掛機角色死亡後，延遲徹底登出 (釋放伺服器資源)
			if (L1PcInstance.this.getOnlineStatus() == 3) {
				_log.info("離線掛機角色死亡，執行登出釋放: " + L1PcInstance.this.getName());
				// 死亡 finalizer 立即取得唯一 owner；登入會使此 token 失效，舊 callback
				// 因而不能重設/存檔/移除後來建立的新 session。
				final AiOwnerToken logoutToken = L1PcInstance.this.tryClaimPlayerAi(this, AiMode.OFFLINE_LOGOUT);
				if (logoutToken != null) {
					// The clock must not perform saveSync/saveInventory/JDBC/logout.
					delayedSlowHandoff("OfflineDeathLogout",
							() -> L1PcInstance.this.isOfflineDeathLogoutCurrent(logoutToken),
							() -> L1PcInstance.this.runOfflineDeathLogout(logoutToken),
							() -> L1PcInstance.this.releasePlayerAi(logoutToken),
							(reason, failure) -> _log.error(
									"Offline death logout handoff exhausted: pc={} reason={}",
									L1PcInstance.this.getName(), reason, failure))
							.start(5_000L);
				}
			}
			// =====================================================
		}

		/**
		 * <FONT COLOR="#0000ff">經驗值掉落判斷</FONT>
		 */
		private void expRate() {
			deathPenalty(); // 經驗質逞罰
			setGresValid(true); // 經驗 loss 後 G-RES 有效

			if (getExpRes() == 0) {
				setExpRes(1);
			}

			// onChangeExp();
		}

		/**
		 * <FONT COLOR="#0000ff">物品掉落判斷</FONT>
		 * 機率與最大掉落數由 charsettings.properties 死亡懲罰 6 組外控
		 */
		private void lostRate() {
			final int lawful = L1PcInstance.this.getLawful();
			final int[] cfg = ConfigCharSetting.getDeathPenaltyItem(lawful);
			final int ratePercent = cfg[0];
			final int maxCount = cfg[1];
			if (ratePercent <= 0 || maxCount <= 0) {
				_log.info("[死亡懲罰] 道具骰略過 name={} lawful={} cfg=[{}%, max={}]", L1PcInstance.this.getName(),
						lawful, ratePercent, maxCount);
				return;
			}
			final int rnd = ThreadLocalRandom.current().nextInt(100) + 1;
			if (rnd <= ratePercent) {
				final int count = ThreadLocalRandom.current().nextInt(maxCount) + 1;
				_log.info("[死亡懲罰] 道具骰 name={} lawful={} rnd={}/100<={}% → 掉落格數={}", L1PcInstance.this.getName(),
						lawful, rnd, ratePercent, count);
				if (count > 0) {
					L1PcInstance.this.caoPenaltyResult(count);
				}
			} else {
				_log.info("[死亡懲罰] 道具骰未中 name={} lawful={} rnd={}/100 門檻<={}%", L1PcInstance.this.getName(),
						lawful, rnd, ratePercent);
			}
		}

		/**
		 * <FONT COLOR="#0000ff">死亡技能遺失判斷</FONT>
		 * 機率與最大掉落數由 charsettings.properties 死亡懲罰 6 組外控
		 */
		private void lostSkillRate() {
			final int skillCount = _skillList.size();
			if (skillCount <= 0) {
				_log.info("[死亡懲罰] 技能骰略過 name={} 原因=_skillList 為空(無可遺失的已學技能清單)",
						L1PcInstance.this.getName());
				return;
			}
			final int lawful = getLawful();
			final int[] cfg = ConfigCharSetting.getDeathPenaltySkill(lawful);
			final int ratePercent = cfg[0];
			final int maxCount = cfg[1];
			if (ratePercent <= 0 || maxCount <= 0) {
				_log.info("[死亡懲罰] 技能骰略過 name={} lawful={} cfg=[{}%, max={}] (無區間或機率/數量為0)",
						L1PcInstance.this.getName(), lawful, ratePercent, maxCount);
				return;
			}
			final int rnd = ThreadLocalRandom.current().nextInt(100) + 1;
			if (rnd <= ratePercent) {
				final int count = Math.min(ThreadLocalRandom.current().nextInt(maxCount) + 1, skillCount);
				_log.info("[死亡懲罰] 技能骰 name={} lawful={} rnd={}/100<={}% → 預計嘗試遺失{}個(_skillList={})",
						L1PcInstance.this.getName(), lawful, rnd, ratePercent, count, skillCount);
				if (count > 0) {
					delSkill(count);
				}
			} else {
				_log.info("[死亡懲罰] 技能骰未中 name={} lawful={} rnd={}/100 門檻<={}% (_skillList={})",
						L1PcInstance.this.getName(), lawful, rnd, ratePercent, skillCount);
			}
		}
	}

	/**
	 * 外控表「死亡必噴」：將指定物品噴至地面（先解除裝備再噴出）
	 */
	private void dropDeathPenaltyItemToGround(final L1ItemInstance item) {
		getInventory().setEquipped(item, false);
		_log.warn("死亡噴出物品:" + item.getId() + "/" + item.getItem().getName());
		item.set_showId(get_showId());
		item.setDropSourceName(this.getName());
		final int x = getX();
		final int y = getY();
		final short m = getMapId();
		final long dropCount = item.isStackable() ? item.getCount() : 1L;
		final L1ItemInstance dropped = getInventory().tradeItem(
				item, dropCount, World.get().getInventory(x, y, m));
		if (dropped != null) {
			ConfigMsg.announceItemDrop(this.getName(),
					L1ItemStatus.getPlainNumberedName(dropped, dropCount));
			sendPackets(new S_ServerMessage(638,
					L1ItemStatus.getLogName(dropped, dropCount)));
		} else {
			_log.error("Death penalty ground transfer rejected pc={} itemObjId={} count={}",
					getName(), item.getId(), dropCount);
		}
	}

	/**
	 * 外控表「噴出後系統回收」：自背包移除，不落地（drop_setting=3 必噴、4 隨機噴出時）
	 */
	private void vanishDeathPenaltyItem(final L1ItemInstance item) {
		getInventory().setEquipped(item, false);
		_log.warn("死亡噴出消失(系統回收):" + item.getId() + "/" + item.getItem().getName());
		getInventory().removeItem(item, item.getCount());
		sendPackets(new S_ServerMessage(638, item.getLogName()));
	}

	/**
	 * <FONT COLOR="#0000ff">死亡掉落物品</FONT>
	 *
	 * @param count 掉落數量
	 */
	private void caoPenaltyResult(final int count) {
		for (int i = 0; i < count; i++) {
			if (tryConsumeOneDeathSafeConsumable(2)) {
				continue;
			}
			final L1ItemInstance item = getInventory().caoPenalty();
			if (item != null) {
				if (DeathPenaltyItemControlTable.get().isVanishIfDropped(item.getItem().getItemId())) {
					vanishDeathPenaltyItem(item);
				} else {
					dropDeathPenaltyItemToGround(item);
				}
			}
		}
	}

	/**
	 * <FONT COLOR="#0000ff">死亡技能遺失</FONT>
	 * 
	 * @param count
	 *              掉落數量
	 */
	private void delSkill(final int count) {
		for (int i = 0; i < count; i++) {
			if (tryConsumeOneDeathSafeConsumable(4)) {
				_log.info("[死亡懲罰] 技能遺失擋下 name={} 第{}格(消耗死亡保護)", this.getName(), i + 1);
				continue;
			}
			if (this._skillList.isEmpty()) {
				_log.info("[死亡懲罰] 技能遺失中止 name={} 第{}格 原因=_skillList已空", this.getName(), i + 1);
				return;
			}
			final int index = ThreadLocalRandom.current().nextInt(this._skillList.size());
			final Integer skillid = _skillList.get(index);
			final L1Skills sk = SkillsTable.get().getTemplate(skillid);
			final String skillDisp = (sk != null && sk.getName() != null && !sk.getName().isEmpty())
					? sk.getName()
					: ("#" + skillid);

			if (sk != null && sk.getSkillLevel() > 0) {
				this.sendPackets(new S_DelSkill(this, skillid));
			} else {
				this.removeSkillMastery(skillid);
			}
			CharSkillTable.get().spellLost(this.getId(), skillid);

			_log.info("[死亡懲罰] 技能已遺失 name={} skillId={} 剩餘_skillList={}", this.getName(), skillid,
					this._skillList.size());

			ConfigMsg.announceSkillDrop(this.getName(), skillDisp);
			this.sendPackets(new S_ServerMessage(166, "死亡懲罰：遺失了技能 「" + skillDisp + "」"));
		}
	}

	/**
	 * <FONT COLOR="#0000ff">復活移出死亡清單</FONT>
	 */
	@Override
	public void setDead(final boolean flag) {
		final DeathLifecycleGate lifecycle = _deathLifecycle;
		if (lifecycle == null || !lifecycle.isManaged()) {
			super.setDead(flag);
			if (flag) com.lineage.server.model.npc.UiOperationContext.clear(this);
			return;
		}
		withVitalMutation(() -> {
			final DeathLifecycleGate.State current = lifecycle.snapshot();
			if (flag) {
				if (current.phase() != DeathLifecycleGate.Phase.RESURRECTING
						&& current.phase() != DeathLifecycleGate.Phase.TERMINATED) {
					super.setDead(true);
					com.lineage.server.model.npc.UiOperationContext.clear(this);
				}
				return;
			}
			switch (current.phase()) {
			case ACTIVE, RESURRECTING -> super.setDead(false);
			case DEAD_PENDING, DEAD_SETTLED, DEATH_DROPPED -> {
				if (lifecycle.reviveDirect(current)) {
					super.setDead(false);
				}
			}
			case FINALIZING, TERMINATED -> {
				// Death finalizer ownership wins; resurrection/restart may retry
				// only after the corpse reaches DEAD_SETTLED.
			}
			}
		});
	}

	@Override
	public void resurrect(final int hp) {
		final DeathLifecycleGate lifecycle = _deathLifecycle;
		if (lifecycle == null || !lifecycle.isManaged()) {
			super.resurrect(hp);
			return;
		}
		final DeathLifecycleGate.ResurrectionClaim claim =
				withVitalMutation(lifecycle::tryReserveResurrection);
		if (claim == null) {
			return;
		}
		try {
			super.resurrect(hp);
			if (isDead()) {
				lifecycle.abortResurrection(claim);
				return;
			}
			if (!lifecycle.completeResurrection(claim)) {
				super.setDead(true);
			}
		} catch (final RuntimeException | Error failure) {
			lifecycle.abortResurrection(claim);
			throw failure;
		}
	}

	public void stopPcDeleteTimer() {
		this.setDead(false);
		if (isDead()) {
			return;
		}
		_deathPipelineGeneration.incrementAndGet();
		set_delete_time(0);
	}

	/**
	 * 等同客戶端「死亡後重新開始」（{@link com.lineage.server.clientpackets.C_Restart}）的核心流程。<br>
	 * 用於活動清場等：{@link com.lineage.server.utils.Teleportation#teleportation} 會拒絕 {@code isDead()} 之瞬移。
	 */
	public void restartLikeC_Restart(final int locX, final int locY, final short mapId) {
		try {
			if (getNetConnection() == null) {
				return;
			}
			// 解除「傳送中」狀態，避免卡在不可操作（Teleportation 會在完成時 setTeleport(false)，但此流程不走 teleportation）
			setTeleport(false);

			stopPcDeleteTimer();
			if (isDead()) {
				return;
			}
			final List<L1PcInstance> previousViewers =
					World.get().getRecognizePlayer(this);
			setCurrentHp(Math.max(1, getLevel()));
			set_food(40);
			setStatus(0);
			L1PolyMorph.undoPoly(this);
			if (!World.get().relocateVisibleObject(this, locX, locY, mapId)) {
				_log.warn("restartLikeC_Restart: relocateVisibleObject 失敗 pc={} map={}", getName(), mapId);
				return;
			}
			removeAllKnownObjects();
			for (final L1PcInstance viewer : previousViewers) {
				viewer.removeKnownObject(this);
				viewer.sendPackets(new S_RemoveObject(this));
			}
			set_showId(-1);
			sendPackets(new S_MapID(this, getMapId(), getMap().isUnderwater()));
			broadcastPacketAll(new S_OtherCharPacks(this));
			sendPackets(new S_OwnCharPack(this));
			sendPackets(new S_CharVisualUpdate(this));
			startHpRegeneration();
			startMpRegeneration();
			sendPackets(new S_Weather(World.get().getWeather()));
			sendPackets(new S_PacketBoxIcon1(true, get_dodge()));
			updateObject();
			if (getHellTime() > 0) {
				beginHell(false);
			}
		} catch (final Exception e) {
			_log.error("restartLikeC_Restart: " + getName(), e);
		}
	}

	/**
	 * <FONT COLOR="#0000ff">是否在參加攻城戰中</FONT>
	 * 
	 * @return true:是 false:不是
	 */
	public boolean castleWarResult() {
		if ((this.getClanid() != 0) && this.isCrown()) { // 具有血盟的王族
			final L1Clan clan = WorldClan.get().getClan(this.getClanname());
			if (clan.getCastleId() == 0) {
				// 取回全部戰爭清單
				for (final L1War war : WorldWar.get().getWarList()) {
					final int warType = war.getWarType();
					final boolean isInWar = war.checkClanInWar(this.getClanname());
					final boolean isAttackClan = war.checkAttackClan(this.getClanname());
					if ((this.getId() == clan.getLeaderId()) && // 攻城戰中 攻擊方盟主死亡 退出戰爭
							(warType == 1) && isInWar && isAttackClan) {
						final String enemyClanName = war.getEnemyClanName(this.getClanname());
						if (enemyClanName != null) {
							war.ceaseWar(this.getClanname(), enemyClanName); // 結束
						}
						break;
					}
				}
			}
		}

		int castleId = 0;
		boolean isNowWar = false;
		castleId = L1CastleLocation.getCastleIdByArea(this);
		if (castleId != 0) { // 戰爭範圍旗幟內城堡ID
			isNowWar = ServerWarExecutor.get().isNowWar(castleId);
		}
		return isNowWar;
	}

	/**
	 * <FONT COLOR="#0000ff">是否在參加血盟戰爭中</FONT>
	 * 
	 * @param lastAttacker
	 * @return true:是 false:不是
	 */
	public boolean simWarResult(final L1Character lastAttacker) {
		if (this.getClanid() == 0) { // 具有血盟
			return false;
		}

		L1PcInstance attacker = null;
		String enemyClanName = null;
		boolean sameWar = false;

		// 判斷主要攻擊者
		if (lastAttacker instanceof L1PcInstance pc) {// 攻擊者是玩家
			attacker = pc;

		} else if (lastAttacker instanceof L1PetInstance pet) {// 攻擊者是寵物
			attacker = (L1PcInstance) pet.getMaster();

		} else if (lastAttacker instanceof L1SummonInstance summon) {// 攻擊者是 召換獸
			attacker = (L1PcInstance) summon.getMaster();

		} else if (lastAttacker instanceof L1IllusoryInstance illusory) {// 攻擊者是 分身
			attacker = (L1PcInstance) illusory.getMaster();

		} else if (lastAttacker instanceof L1EffectInstance effect) {// 攻擊者是 技能物件(火牢)
			attacker = (L1PcInstance) effect.getMaster();

		} else {
			return false;
		}

		final L1Clan clan = WorldClan.get().getClan(this.getClanname());
		// 取回全部戰爭清單
		for (final L1War war : WorldWar.get().getWarList()) {
			final int warType = war.getWarType();
			if (warType == 1) {// 攻城戰
				continue;
			}
			final boolean isInWar = war.checkClanInWar(this.getClanname());
			if (isInWar) {
				if ((attacker != null) && (attacker.getClanid() != 0)) { // 兇手不為空 並且具有血盟
					sameWar = war.checkClanInSameWar(this.getClanname(), attacker.getClanname());
				}

				if (this.getId() == clan.getLeaderId()) {// 戰爭中盟主
					enemyClanName = war.getEnemyClanName(this.getClanname());
					if (enemyClanName != null) {
						war.ceaseWar(this.getClanname(), enemyClanName); // 盟主死亡結束
					}
				}

				if ((warType == 2) && sameWar) {// 戰爭中模擬戰
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 經驗質恢復（比例由 exp 資料表 restore_rate 外控，0.05=5%）
	 */
	public void resExp() {
		final int oldLevel = this.getLevel();
		final long needExp = ExpTable.getNeedExpNextLevel(oldLevel);
		final double rate = ExpTable.getRestoreRate(oldLevel);
		if (rate <= 0D) {
			return;
		}
		final long exp = (long) (needExp * rate);
		if (exp == 0) {
			return;
		}
		this.addExp(exp);
	}

	private static record DeathSafeParams(int type, boolean consume) {
	}

	/**
	 * 解析死亡保護道具 classname：{@code add.DeathSafe [type] [consume]}
	 */
	private DeathSafeParams parseDeathSafeParams(final L1ItemInstance item) {
		final String className = item.getItem().getclassname();
		if (className == null || !className.startsWith("add.DeathSafe")) {
			return null;
		}
		try {
			final String[] params = className.trim().split("\\s+");
			if (params.length >= 3) {
				return new DeathSafeParams(Integer.parseInt(params[1]), Boolean.parseBoolean(params[2]));
			}
		} catch (final Exception e) {
			// 略過
		}
		return null;
	}

	/**
	 * 合併所有「不消耗」型死亡保護的 type 位元（該次死亡內整類懲罰不執行）。
	 */
	private int computeDeathSafeNonConsumeMask() {
		int mask = 0;
		for (final L1ItemInstance item : new ArrayList<>(this.getInventory().getItems())) {
			final DeathSafeParams p = parseDeathSafeParams(item);
			if (p != null && !p.consume) {
				mask |= p.type;
			}
		}
		return mask;
	}

	private void sendDeathSafeNonConsumeOnce(final boolean[] notifiedFlag) {
		if (notifiedFlag[0]) {
			return;
		}
		this.sendPackets(new S_ServerMessage(166, "死亡保護道具發揮了作用"));
		notifiedFlag[0] = true;
	}

	/**
	 * 以背包順序找到第一個可消耗且 type 含 {@code categoryBit} 的死亡保護，扣 1 並通知。
	 *
	 * @param categoryBit 1=經驗, 2=道具, 4=技能
	 * @return 是否成功消耗
	 */
	private boolean tryConsumeOneDeathSafeConsumable(final int categoryBit) {
		for (final L1ItemInstance item : new ArrayList<>(this.getInventory().getItems())) {
			final DeathSafeParams p = parseDeathSafeParams(item);
			if (p != null && p.consume && (p.type & categoryBit) != 0) {
				this.getInventory().removeItem(item, 1L);
				this.sendPackets(new S_ServerMessage(166, "已消耗一個死亡保護道具"));
				return true;
			}
		}
		return false;
	}

	/**
	 * 經驗質逞罰（比例由 exp 資料表 death_penalty_rate 外控，0.05=5%）
	 *
	 * @return 實際扣除的經驗值
	 */
	private long deathPenalty() {
		final int oldLevel = this.getLevel();
		final long needExp = ExpTable.getNeedExpNextLevel(oldLevel);
		final double rate = ExpTable.getDeathPenaltyRate(oldLevel);
		if (rate <= 0D) {
			return 0;
		}
		final long exp = (long) (needExp * rate);
		if (exp == 0) {
			return 0;
		}
		this.addExp(-exp);
		return exp;
	}

	private int _originalEr = 0; // ● 原始DEX ER補正

	public int getOriginalEr() {

		return this._originalEr;
	}

	// [ER] 裝備提供的 ER 加成
	private int _erBonus = 0;

	/**
	 * 增加（減少）ER 加成
	 * 
	 * @param value 數值（可為負數）
	 */
	public void addEr(final int value) {
		_erBonus += value;
	}

	/**
	 * 取得 ER 加成
	 * 
	 * @return ER 加成數值
	 */
	public int getErBonus() {
		return _erBonus;
	}

	public int getEr() {
		if (this.hasSkillEffect(STRIKER_GALE)) {
			return 0;
		}

		int er = 0;
		if (this.isKnight()) {
			er = this.getLevel() >> 2;// /4 // 騎士

		} else if (this.isCrown() || this.isElf()) {
			er = this.getLevel() >> 3;// / 8; // 君主、妖精

		} else if (this.isDarkelf()) {
			er = this.getLevel() / 6; // 黑暗妖精

		} else if (this.isWizard()) {
			er = this.getLevel() / 10; // 法師

		} else if (this.isDragonKnight()) {
			er = this.getLevel() / 7; // 龍騎士

		} else if (this.isIllusionist()) {
			er = this.getLevel() / 9; // 幻術師
		}

		er += (this.getDex() - 8) >> 1;// / 2;

		er += this.getOriginalEr();

		if (this.hasSkillEffect(DRESS_EVASION)) {// 閃避提升
			er += 12;
		}

		if (this.hasSkillEffect(SOLID_CARRIAGE)) {// 堅固防護
			er += 15;
		}

		if (this.hasSkillEffect(ADLV80_1)) {// 卡瑞的祝福(地龍副本)
			er += 30;
		}

		if (this.hasSkillEffect(ADLV80_2)) {// 莎爾的祝福(水龍副本)
			er += 15;
		}

		// [ER] 加上裝備提供的 ER 加成
		er += _erBonus;

		// 加上套裝提供的 ER 加成
		er += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.ER);

		return er;
	}

	/**
	 * 使用的武器
	 * 
	 * @return
	 */
	public L1ItemInstance getWeapon() {
		return this._weapon;
	}

	/**
	 * 使用的武器
	 * 
	 * @param weapon
	 */
	public void setWeapon(final L1ItemInstance weapon) {
		this._weapon = weapon;
	}

	/**
	 * 傳回任務狀態類
	 * 
	 * @return
	 */
	public L1PcQuest getQuest() {
		return this._quest;
	}

	/**
	 * 傳回選單命令執行類
	 * 
	 * @return
	 */
	public L1ActionPc getAction() {
		return this._action;
	}

	/**
	 * 傳回寵物選單命令執行類
	 * 
	 * @return
	 */
	public L1ActionPet getActionPet() {
		return this._actionPet;
	}

	/**
	 * 傳回召喚獸選單命令執行類
	 * 
	 * @return
	 */
	public L1ActionSummon getActionSummon() {
		return this._actionSummon;
	}

	/**
	 * 王族
	 * 
	 * @return
	 */
	public boolean isCrown() {
		return ((this.getClassId() == CLASSID_PRINCE) || (this.getClassId() == CLASSID_PRINCESS));
	}

	/**
	 * 騎士
	 * 
	 * @return
	 */
	public boolean isKnight() {
		return ((this.getClassId() == CLASSID_KNIGHT_MALE) || (this.getClassId() == CLASSID_KNIGHT_FEMALE));
	}

	/**
	 * 精靈
	 * 
	 * @return
	 */
	public boolean isElf() {
		return ((this.getClassId() == CLASSID_ELF_MALE) || (this.getClassId() == CLASSID_ELF_FEMALE));
	}

	/**
	 * 法師
	 * 
	 * @return
	 */
	public boolean isWizard() {
		return ((this.getClassId() == CLASSID_WIZARD_MALE) || (this.getClassId() == CLASSID_WIZARD_FEMALE));
	}

	/**
	 * 黑暗精靈
	 * 
	 * @return
	 */
	public boolean isDarkelf() {
		return ((this.getClassId() == CLASSID_DARK_ELF_MALE) || (this.getClassId() == CLASSID_DARK_ELF_FEMALE));
	}

	/**
	 * 龍騎士
	 * 
	 * @return
	 */
	public boolean isDragonKnight() {
		return ((this.getClassId() == CLASSID_DRAGON_KNIGHT_MALE)
				|| (this.getClassId() == CLASSID_DRAGON_KNIGHT_FEMALE));
	}

	/**
	 * 幻術師
	 * 
	 * @return
	 */
	public boolean isIllusionist() {
		return ((this.getClassId() == CLASSID_ILLUSIONIST_MALE) || (this.getClassId() == CLASSID_ILLUSIONIST_FEMALE));
	}

	private volatile ClientExecutor _netConnection = null;
	private volatile boolean _outboundDetachExpected;
	private final AtomicLong _connectionGeneration = new AtomicLong(1L);
	private int _classId;
	private int _type;
	private long _exp;
	private final L1Karma _karma = new L1Karma();
	private final ReentrantLock _karmaLock = new ReentrantLock();
	private boolean _gm;
	private boolean _monitor;
	private boolean _gmInvis;
	/** GM 採點開通（走向障礙自動寫入 mapids_tile）：預設關閉，需以 commands 指令啟用 */
	private boolean _gmMapTileProbeEnabled;
	private volatile short _accessLevel;
	private int _currentWeapon;
	private final L1PcInventory _inventory;
	private final L1DwarfInventory _dwarf;
	private final L1DwarfForChaInventory _dwarfForCha;
	private final L1DwarfForElfInventory _dwarfForElf;
	private volatile L1ItemInstance _weapon;
	private volatile L1Party _party;
	private L1ChatParty _chatParty;
	private int _partyID;
	private int _tradeID;
	private boolean _tradeOk;
	/** Prompt correlation is published by one session lane and consumed by another. */
	private volatile int _tempID;

	// 每日任務面板專用暫存變數
	private int _tempDailyQuestId = 0;
	private int _dailyQuestListPage = 0;
	private java.util.List<com.lineage.server.templates.L1QuestTemplate> _tempDailyQuestList = null;

	/** 穿雲招集設定 (使用穿雲箭時暫存，供 C_Attr 4774 傳送邏輯使用) */
	private L1WearingArrowConfig _wearingArrowConfig;
	/** 是否顯示穿雲招集 Y/N 邀請視窗 (關閉時僅收通知，可避免部分客戶端異常) */
	private boolean _showWearingArrows = true;
	/** 隊伍掉落：關閉時不顯示隊友獲取戰利品資訊 (813) */
	private boolean _showPartyDrop = true;
	/** 個人掉落：關閉時不顯示個人獲取戰利品資訊 (143) */
	private boolean _showPcDrop = true;
	/** 喝水顯示：關閉時不顯示喝水的 spr 跟文字資訊 */
	private boolean _showPotionEffect = true;
	/** 傷害顯示：關閉時不顯示傷害的 Spr */
	private boolean _showDamage = true;
	/** 娃娃顯示：關閉時不顯示娃娃的 Spr */
	private boolean _showDollSpr = true;
	/** Proc 特效顯示：關閉時不顯示 Proc 的 S_SkillSound / S_DoActionGFX */
	private boolean _showProcSpr = true;
	private volatile boolean _isTeleport = false;
	private final Object _teleportLifecycleLock = new Object();
	// Read by the non-blocking outbound validity fence; writes retain the lifecycle lock.
	private volatile long _teleportGeneration;
	private volatile long _teleportConnectionGeneration;
	private volatile boolean _teleportClientAckAllowed;
	/** Guarded by _teleportLifecycleLock; released on ACK, replacement, recovery or detach. */
	private Runnable _teleportClientAckAction;
	private boolean _isDrink = false;
	private boolean _isGres = false;
	private final ReentrantLock _pinkNameStateLock = new ReentrantLock();
	private final AtomicLong _pinkNameGeneration = new AtomicLong();
	private final AtomicReference<PinkNameLease> _activePinkNameLease = new AtomicReference<>();
	private L1PcQuest _quest;
	private L1ActionPc _action;
	private L1ActionPet _actionPet;
	private L1ActionSummon _actionSummon;

	private L1EquipmentSlot _equipSlot;

	// 套裝效果能力值（快取）
	private com.lineage.server.model.item.L1ItemAbility _setAbility = new com.lineage.server.model.item.L1ItemAbility();

	// Buff/技能效果能力值（快取）
	private com.lineage.server.model.item.L1ItemAbility _cachedBuffAbility = new com.lineage.server.model.item.L1ItemAbility();

	// 永久能力快取（供 L1PcPermanentAbilityCalc 先回滾舊值再套用新值）
	private com.lineage.server.model.item.L1ItemAbility _cachedPermanentAbility = null;
	// 收藏／卡冊／任務能力快取（避免重複重算時累加）
	private com.lineage.server.model.item.L1ItemAbility _cachedQuestAbility = null;

	public com.lineage.server.model.item.L1ItemAbility getCachedPermanentAbility() {
		return _cachedPermanentAbility;
	}

	public void setCachedPermanentAbility(final com.lineage.server.model.item.L1ItemAbility ability) {
		this._cachedPermanentAbility = ability;
	}

	public com.lineage.server.model.item.L1ItemAbility getCachedQuestAbility() {
		return _cachedQuestAbility;
	}

	public void setCachedQuestAbility(final com.lineage.server.model.item.L1ItemAbility ability) {
		this._cachedQuestAbility = ability;
	}

	// ========== 技能天賦系統 ==========
	
	/**
	 * 玩家天賦等級 Map（鍵＝{@code talent_id} 模板，值＝{@code talent_level}；與 {@code character_天賦紀錄.talent_id} 一致）
	 */
	private Map<Integer, Integer> _talentLevels = new java.util.concurrent.ConcurrentHashMap<>();

	/** {@code characters.talent}：額外天賦點（活動／GM 等，非等級公式） */
	private int _talentBonusPoints = 0;
	/** 各 {@code talent_id} 於 {@code character_天賦紀錄.talent_point} 之累計已花費 */
	private final Map<Integer, Integer> _talentPointSpentByTalentId = new java.util.concurrent.ConcurrentHashMap<>();

	/**
	 * 天賦 Buff 能力快取
	 * 用於狀態天賦（當玩家擁有特定 Buff 時給予額外能力）
	 * 供 L1PcTalentBuffCalc 先回滾舊值再套用新值
	 */
	private com.lineage.server.model.item.L1ItemAbility _cachedTalentBuffAbility = null;

	/** 天賦 HTML 詳情頁：當前頁面職業位元（1、2、4…64 或 127），供 talent_{talent_id} 點擊後刷新同一頁 */
	private int _talentPanelPageBit = 0;

	/**
	 * 取得玩家天賦等級 Map（鍵＝{@code talent_id}）
	 */
	public Map<Integer, Integer> getTalentLevels() {
		return _talentLevels;
	}

	/**
	 * 設定玩家天賦等級 Map（鍵＝{@code talent_id}；通常由 {@link com.lineage.server.datatables.user.CharacterTalentTable#loadTalents} 呼叫）
	 */
	public void setTalentLevels(Map<Integer, Integer> talentLevels) {
		this._talentLevels = talentLevels;
	}

	/**
	 * 取得天賦 Buff 能力快取
	 */
	public com.lineage.server.model.item.L1ItemAbility getCachedTalentBuffAbility() {
		_playerActionLock.lock();
		try {
			return _cachedTalentBuffAbility;
		} finally {
			_playerActionLock.unlock();
		}
	}

	/**
	 * 設定天賦 Buff 能力快取
	 */
	public void setCachedTalentBuffAbility(final com.lineage.server.model.item.L1ItemAbility ability) {
		_playerActionLock.lock();
		try {
			this._cachedTalentBuffAbility = ability;
		} finally {
			_playerActionLock.unlock();
		}
	}

	public int getTalentPanelPageBit() {
		return _talentPanelPageBit;
	}

	public void setTalentPanelPageBit(final int bit) {
		this._talentPanelPageBit = bit;
	}

	public int getTalentBonusPoints() {
		return _talentBonusPoints;
	}

	public void setTalentBonusPoints(final int points) {
		this._talentBonusPoints = Math.max(0, points);
	}

	public void addTalentBonusPoints(final int delta) {
		if (delta == 0) {
			return;
		}
		this._talentBonusPoints = Math.max(0, this._talentBonusPoints + delta);
	}

	public int getTalentPointSpent(final int talentId) {
		return _talentPointSpentByTalentId.getOrDefault(talentId, 0);
	}

	public void setTalentPointSpent(final int talentId, final int spent) {
		if (spent <= 0) {
			_talentPointSpentByTalentId.remove(talentId);
		} else {
			_talentPointSpentByTalentId.put(talentId, spent);
		}
	}

	public void addTalentPointSpent(final int talentId, final int delta) {
		if (delta <= 0) {
			return;
		}
		final int next = getTalentPointSpent(talentId) + delta;
		setTalentPointSpent(talentId, next);
	}

	public int getTalentPointSpentSum() {
		int s = 0;
		for (final int v : _talentPointSpentByTalentId.values()) {
			s += Math.max(0, v);
		}
		return s;
	}

	public void clearTalentPointSpentMap() {
		_talentPointSpentByTalentId.clear();
	}
	
	// ========== 天賦系統結束 ==========

	// 套裝造成的變身ID（0 表示無套裝變身）
	private int _setPolyId = 0;

	// 鑲嵌卡片套裝：當前生效的組合名稱（用於比對綠字訊息）
	private List<String> _currentCardComboNames = new ArrayList<>();

	/** 屬性武器機率加成（RatePlus，來源：防具 classname，供 AttrWeaponProcAction 使用） */
	private int _attrRatePlus = 0;

	/**
	 * [Phase 1] 攻擊時觸發的 GenericProcInfo 列表（取代散落的 Weapon/Armor/Doll Proc）
	 * 由 updateProcCache() 建置，Phase 2 & 3 填入 Weapon、Armor、Doll 掃描邏輯。
	 */
	private final List<com.lineage.server.model.proc.GenericProcInfo> _attackProcs = new ArrayList<>();

	/**
	 * [Phase 1] 受傷時觸發的 GenericProcInfo 列表（裝備受傷、娃娃減傷、反擊）
	 * 由 updateProcCache() 建置，Phase 2 & 3 填入具體邏輯。
	 */
	private final List<com.lineage.server.model.proc.GenericProcInfo> _defenseProcs = new ArrayList<>();
	/** Proc rebuild/snapshot lock; deliberately independent of the PC object monitor. */
	private final ReentrantLock _procCacheLock = new ReentrantLock();

	/** [ItemBuffExecutor] 裝備自動 Buff 技能計數 (skillId -> 來源數)，供 Timer 定期檢查並重補 */
	private final ConcurrentHashMap<Integer, Integer> _autoSkillCounts = new ConcurrentHashMap<>();

	private String _accountName; // ● 帳號名稱

	public String getAccountName() {
		return this._accountName;
	}

	public void setAccountName(final String s) {
		this._accountName = s;
	}

	private int _baseMaxHp = 0; // ● 基礎 MAX HP（RO版: 1~999999, 公版: 1~32767）

	/**
	 * 基礎HP
	 * 
	 * @return
	 */
	public int getBaseMaxHp() {
		return this._baseMaxHp;
	}

	/**
	 * 基礎HP
	 * 
	 * @param i
	 */
	public void addBaseMaxHp(int i) {
		i += this._baseMaxHp;
		// RO版: 上限 999999, 公版: 上限 32767
		int maxLimit = com.lineage.config.ConfigClient.isROClient() 
			? com.lineage.server.model.L1Character.MAX_HP_MP_RO 
			: com.lineage.server.model.L1Character.MAX_HP_MP_STANDARD;
		if (i >= maxLimit) {
			i = maxLimit;
		} else if (i < 1) {
			i = 1;
		}
		this.addMaxHp(i - this._baseMaxHp);
		this._baseMaxHp = i;
	}

	private int _baseMaxMp = 0; // ● 基礎 MAX MP（RO版: 0~999999, 公版: 0~32767）
	public int _baseMaxMpc;

	/**
	 * 基礎MP
	 * 
	 * @return
	 */
	public int getBaseMaxMp() {
		return this._baseMaxMp;
	}

	/**
	 * 基礎MP
	 * 
	 * @param i
	 */
	public void addBaseMaxMp(int i) {
		i += this._baseMaxMp;
		// RO版: 上限 999999, 公版: 上限 32767
		int maxLimit = com.lineage.config.ConfigClient.isROClient() 
			? com.lineage.server.model.L1Character.MAX_HP_MP_RO 
			: com.lineage.server.model.L1Character.MAX_HP_MP_STANDARD;
		if (i >= maxLimit) {
			i = maxLimit;
		} else if (i < 1) {
			i = 1;
		}
		this.addMaxMp(i - this._baseMaxMp);
		this._baseMaxMp = i;
	}

	private int _baseAc = 0; // ● 基礎 AC（-128~127）

	public int getBaseAc() {
		return this._baseAc;
	}

	private int _originalAc = 0; // ● 原始 DEX AC 補正

	public int getOriginalAc() {
		return this._originalAc;
	}

	private int _baseStr = 0; // ● 基礎 STR（1~127）

	/**
	 * 原始力量(內含素質提升/萬能藥)
	 * 
	 * @return
	 */
	public int getBaseStr() {
		return this._baseStr;
	}

	/**
	 * 原始力量(內含素質提升/萬能藥)
	 * 
	 * @param i
	 */
	public void addBaseStr(int i) {
		i += this._baseStr;
		if (i >= 254) {
			i = 254;

		} else if (i < 1) {
			i = 1;
		}
		this.addStr((i - this._baseStr));
		this._baseStr = i;
	}

	private int _baseCon = 0; // ● 基礎 CON（1~127）

	/**
	 * 原始體質(內含素質提升/萬能藥)
	 * 
	 * @return
	 */
	public int getBaseCon() {
		return this._baseCon;
	}

	/**
	 * 原始體質(內含素質提升/萬能藥)
	 * 
	 * @param i
	 */
	public void addBaseCon(int i) {
		i += this._baseCon;
		if (i >= 254) {
			i = 254;

		} else if (i < 1) {
			i = 1;
		}
		this.addCon((i - this._baseCon));
		this._baseCon = i;
	}

	private int _baseDex = 0; // ● 基礎 DEX（1~127）

	/**
	 * 原始敏捷(內含素質提升/萬能藥)
	 * 
	 * @return
	 */
	public int getBaseDex() {
		return this._baseDex;
	}

	/**
	 * 原始敏捷(內含素質提升/萬能藥)
	 * 
	 * @param i
	 */
	public void addBaseDex(int i) {
		i += this._baseDex;
		if (i >= 254) {
			i = 254;

		} else if (i < 1) {
			i = 1;
		}
		this.addDex((i - this._baseDex));
		this._baseDex = i;
	}

	private int _baseCha = 0; // ● CHA 基礎值（1〜127）

	/**
	 * 原始魅力(內含素質提升/萬能藥)
	 * 
	 * @return
	 */
	public int getBaseCha() {
		return this._baseCha;
	}

	/**
	 * 原始魅力(內含素質提升/萬能藥)
	 * 
	 * @param i
	 */
	public void addBaseCha(int i) {
		i += this._baseCha;
		if (i >= 254) {
			i = 254;

		} else if (i < 1) {
			i = 1;
		}
		this.addCha((i - this._baseCha));
		this._baseCha = i;
	}

	private int _baseInt = 0; // ● INT 基礎值（1〜127）

	/**
	 * 原始智力(內含素質提升/萬能藥)
	 * 
	 * @return
	 */
	public int getBaseInt() {
		return this._baseInt;
	}

	/**
	 * 原始智力(內含素質提升/萬能藥)
	 * 
	 * @param i
	 */
	public void addBaseInt(int i) {
		i += this._baseInt;
		if (i >= 254) {
			i = 254;

		} else if (i < 1) {
			i = 1;
		}
		this.addInt((i - this._baseInt));
		this._baseInt = i;
	}

	private int _baseWis = 0; // ● WIS 基礎值（1〜127）

	/**
	 * 原始精神(內含素質提升/萬能藥)
	 * 
	 * @return
	 */
	public int getBaseWis() {
		return this._baseWis;
	}

	/**
	 * 原始精神(內含素質提升/萬能藥)
	 * 
	 * @param i
	 */
	public void addBaseWis(int i) {
		i += this._baseWis;
		if (i >= 254) {
			i = 254;

		} else if (i < 1) {
			i = 1;
		}
		this.addWis((i - this._baseWis));
		this._baseWis = i;
	}

	// //////////////////////////////////////////////////////////////////////////////////////

	private int _originalStr = 0; // ● 原始 STR

	/**
	 * 原始力量(人物出生)
	 * 
	 * @return
	 */
	public int getOriginalStr() {
		return this._originalStr;
	}

	/**
	 * 原始力量(人物出生)
	 * 
	 * @param i
	 */
	public void setOriginalStr(final int i) {
		this._originalStr = i;
	}

	private int _originalCon = 0; // ● 原始 CON

	/**
	 * 原始體質(人物出生)
	 * 
	 * @return
	 */
	public int getOriginalCon() {
		return this._originalCon;
	}

	/**
	 * 原始體質(人物出生)
	 * 
	 * @param i
	 */
	public void setOriginalCon(final int i) {
		this._originalCon = i;
	}

	private int _originalDex = 0; // ● 原始 DEX

	/**
	 * 原始敏捷(人物出生)
	 * 
	 * @return
	 */
	public int getOriginalDex() {
		return this._originalDex;
	}

	/**
	 * 原始敏捷(人物出生)
	 * 
	 * @param i
	 */
	public void setOriginalDex(final int i) {
		this._originalDex = i;
	}

	private int _originalCha = 0; // ● 原始 CHA

	/**
	 * 原始魅力(人物出生)
	 * 
	 * @return
	 */
	public int getOriginalCha() {
		return this._originalCha;
	}

	/**
	 * 原始魅力(人物出生)
	 * 
	 * @param i
	 */
	public void setOriginalCha(final int i) {
		this._originalCha = i;
	}

	private int _originalInt = 0; // ● 原始 INT

	/**
	 * 原始智力(人物出生)
	 * 
	 * @return
	 */
	public int getOriginalInt() {
		return this._originalInt;
	}

	/**
	 * 原始智力(人物出生)
	 * 
	 * @param i
	 */
	public void setOriginalInt(final int i) {
		this._originalInt = i;
	}

	private int _originalWis = 0; // ● 原始 WIS

	/**
	 * 原始精神(人物出生)
	 * 
	 * @return
	 */
	public int getOriginalWis() {
		return this._originalWis;
	}

	/**
	 * 原始精神(人物出生)
	 * 
	 * @param i
	 */
	public void setOriginalWis(final int i) {
		this._originalWis = i;
	}

	private int _originalDmgup = 0; // ● 原始 STR 傷害補正

	public int getOriginalDmgup() {
		return this._originalDmgup;
	}

	private int _originalBowDmgup = 0; // ● 原始 DEX 弓傷害補正

	public int getOriginalBowDmgup() {
		return this._originalBowDmgup;
	}

	private int _originalHitup = 0; // ● 原始 STR 命中補正

	public int getOriginalHitup() {
		return this._originalHitup;
	}

	private int _originalBowHitup = 0; // ● 原始 DEX 命中補正

	public int getOriginalBowHitup() {
		return this._originalHitup + this._originalBowHitup;
	}

	private int _originalMr = 0; // ● 原始 WIS 魔法防禦

	public int getOriginalMr() {
		return this._originalMr;
	}

	private int _originalMagicHit = 0; // ● 原始 INT 魔法命中

	/**
	 * 智力(依職業)附加魔法命中
	 * 
	 * @return
	 */
	public int getOriginalMagicHit() {
		return this._originalMagicHit;
	}

	private int _originalMagicCritical = 0; // ● 原始 INT 魔法爆擊

	public int getOriginalMagicCritical() {
		return this._originalMagicCritical;
	}

	private int _originalMagicConsumeReduction = 0; // ● 原始 INT 消耗 MP 減免

	public int getOriginalMagicConsumeReduction() {
		return this._originalMagicConsumeReduction;
	}

	private int _originalMagicDamage = 0; // ● 原始 INT 魔法傷害

	/**
	 * 魔攻
	 * 
	 * @return
	 */
	public int getOriginalMagicDamage() {
		return this._originalMagicDamage;
	}

	private int _originalHpup = 0; // ● 原始 CON HP 上升值補正

	/**
	 * 體質 HP上昇値補正
	 * 
	 * @return
	 */
	public int getOriginalHpup() {
		return this._originalHpup;
	}

	private int _originalMpup = 0; // ● 原始 WIS MP 上升值補正

	/**
	 * 精神 MP上昇値補正
	 * 
	 * @return
	 */
	public int getOriginalMpup() {
		return this._originalMpup;
	}

	private int _baseDmgup = 0; // ● 傷害補正基礎（-128〜127）

	public int getBaseDmgup() {
		return this._baseDmgup;
	}

	private int _baseBowDmgup = 0; // ● 弓傷害補正基礎（-128〜127）

	public int getBaseBowDmgup() {
		return this._baseBowDmgup;
	}

	private int _baseHitup = 0; // ● 命中補正基礎（-128〜127）

	/**
	 * 命中補正
	 * 
	 * @return
	 */
	public int getBaseHitup() {
		return this._baseHitup;
	}

	private int _baseBowHitup = 0; // ● 弓命中補正基礎（-128〜127）

	/**
	 * 弓命中補正
	 * 
	 * @return
	 */
	public int getBaseBowHitup() {
		return this._baseBowHitup;
	}

	private int _baseMr = 0; // ● 魔法防禦基礎（0〜）

	/**
	 * 魔法防御
	 * 
	 * @return
	 */
	public int getBaseMr() {
		return this._baseMr;
	}

	private int _advenHp; // 暫時增加的HP

	/**
	 * 暫時增加的HP
	 * 
	 * @return
	 */
	public int getAdvenHp() {
		return this._advenHp;
	}

	/**
	 * 暫時增加的HP
	 * 
	 * @param i
	 */
	public void setAdvenHp(final int i) {
		this._advenHp = i;
	}

	private int _advenMp; // 暫時增加的MP

	/**
	 * 暫時增加的MP
	 * 
	 * @return
	 */
	public int getAdvenMp() {
		return this._advenMp;
	}

	/**
	 * 暫時增加的MP
	 * 
	 * @param i
	 */
	public void setAdvenMp(final int i) {
		this._advenMp = i;
	}

	private int _highLevel; // ● 歷史最高等級

	public int getHighLevel() {
		return this._highLevel;
	}

	public void setHighLevel(final int i) {
		this._highLevel = i;
	}

	private int _bonusStats; // 升級點數使用次數

	/**
	 * 升級點數使用次數
	 * 
	 * @return
	 */
	public int getBonusStats() {
		return this._bonusStats;
	}

	/**
	 * 設置升級點數使用次數
	 * 
	 * @param i
	 */
	public void setBonusStats(final int i) {
		this._bonusStats = i;
	}

	private int _elixirStats; // 萬能藥使用次數

	/**
	 * 萬能藥使用次數
	 * 
	 * @return
	 */
	public int getElixirStats() {
		return this._elixirStats;
	}

	/**
	 * 設置萬能藥使用次數
	 * 
	 * @param i
	 */
	public void setElixirStats(final int i) {
		this._elixirStats = i;
	}

	private int _elfAttr; // ● 妖精屬性

	/**
	 * 精靈屬性
	 * 
	 * @return
	 */
	public int getElfAttr() {
		return this._elfAttr;
	}

	public void setElfAttr(final int i) {
		this._elfAttr = i;
	}

	private int _expRes; // ● EXP復旧

	public int getExpRes() {
		return this._expRes;
	}

	public void setExpRes(final int i) {
		this._expRes = i;
	}

	private int _partnerId; // ● 結婚相手

	public int getPartnerId() {
		return this._partnerId;
	}

	public void setPartnerId(final int i) {
		_partnerId = i;
	}

	private volatile int _onlineStatus; // 人物連線狀態

	/**
	 * 人物連線狀態
	 * 
	 * @return
	 */
	public int getOnlineStatus() {
		return _onlineStatus;
	}

	/**
	 * 設置人物連線狀態
	 * 
	 * @param i
	 */
	public void setOnlineStatus(final int i) {
		if (i == 1) {
			invalidatePendingOfflineLogout();
		}
		_onlineStatus = i;
		// 登入時同步計算所有任務/收藏類能力（不可延遲，否則首次狀態封包會缺少加成）
		if (i == 1 && getNetConnection() != null) {
			L1PcQuestAbilityCalc.applyTo(this);
			L1PcPermanentAbilityCalc.applyTo(this);
		}
	}

	private int _homeTownId; // ● 所屬村莊

	public int getHomeTownId() {
		return _homeTownId;
	}

	public void setHomeTownId(final int i) {
		_homeTownId = i;
	}

	private int _contribution; // 貢獻度

	/**
	 * 貢獻度
	 * 
	 * @return
	 */
	public int getContribution() {
		_contributionMutationLock.lock();
		try {
			return _contribution;
		} finally {
			_contributionMutationLock.unlock();
		}
	}

	/**
	 * 貢獻度
	 * 
	 * @param i
	 */
	public void setContribution(final int i) {
		_contributionMutationLock.lock();
		try {
			_contribution = i;
		} finally {
			_contributionMutationLock.unlock();
		}
	}

	private int _hellTime;// 地獄滯留時間

	/**
	 * 地獄滯留時間
	 * 
	 * @return
	 */
	public int getHellTime() {
		return this._hellTime;
	}

	/**
	 * 地獄滯留時間
	 * 
	 * @param i
	 */
	public void setHellTime(final int i) {
		this._hellTime = i;
	}

	private boolean _banned; // ● 凍結

	public boolean isBanned() {
		return this._banned;
	}

	public void setBanned(final boolean flag) {
		this._banned = flag;
	}

	private int _food; // ● 満腹度

	public int get_food() {
		return _food;
	}

	public void set_food(int i) {
		if (i > 225) {
			i = 225;
		}
		_food = i;
		if (_food == 225) {// LOLI 生存吶喊
			final Calendar cal = Calendar.getInstance();
			long h_time = cal.getTimeInMillis() / 1000;// 換算為秒
			set_h_time(h_time);// 紀錄吃飽時間

		} else {
			set_h_time(-1);// 紀錄吃飽時間
		}
	}

	public L1EquipmentSlot getEquipSlot() {
		return this._equipSlot;
	}

	/**
	 * 取得套裝效果能力值
	 * 
	 * @return 套裝效果能力值
	 */
	public com.lineage.server.model.item.L1ItemAbility getSetAbility() {
		return this._setAbility;
	}

	/**
	 * 取得屬性武器機率加成（RatePlus）
	 */
	public int getAttrRatePlus() {
		_procCacheLock.lock();
		try {
			return _attrRatePlus;
		} finally {
			_procCacheLock.unlock();
		}
	}

	/**
	 * 設定屬性武器機率加成
	 */
	public void setAttrRatePlus(final int val) {
		_procCacheLock.lock();
		try {
			_attrRatePlus = val;
		} finally {
			_procCacheLock.unlock();
		}
	}

	/**
	 * 累加屬性武器機率加成
	 */
	public void addAttrRatePlus(final int val) {
		_procCacheLock.lock();
		try {
			_attrRatePlus += val;
		} finally {
			_procCacheLock.unlock();
		}
	}

	/**
	 * [Phase 1] 取得攻擊觸發 Proc 列表（唯讀）
	 */
	public List<com.lineage.server.model.proc.GenericProcInfo> getAttackProcs() {
		_procCacheLock.lock();
		try {
			return List.copyOf(_attackProcs);
		} finally {
			_procCacheLock.unlock();
		}
	}

	/**
	 * [Phase 1] 取得防禦觸發 Proc 列表（唯讀）
	 */
	public List<com.lineage.server.model.proc.GenericProcInfo> getDefenseProcs() {
		_procCacheLock.lock();
		try {
			return List.copyOf(_defenseProcs);
		} finally {
			_procCacheLock.unlock();
		}
	}

	/**
	 * [Phase 1] 新增 Proc 至對應列表（依 triggerType 加入 _attackProcs 或 _defenseProcs）
	 *
	 * @param info 要加入的 GenericProcInfo
	 */
	public void addProc(final com.lineage.server.model.proc.GenericProcInfo info) {
		if (info == null) {
			return;
		}
		_procCacheLock.lock();
		try {
			if (info.triggerType() == com.lineage.server.model.proc.ProcTriggerType.ON_ATTACK) {
				_attackProcs.add(info);
			} else {
				_defenseProcs.add(info);
			}
		} finally {
			_procCacheLock.unlock();
		}
	}

	/**
	 * 新增裝備自動 Buff 技能來源（ItemAbility.AUTO_SKILL_ID）
	 * 
	 * @param skillId 技能 ID
	 */
	public void addAutoSkill(final int skillId) {
		if (skillId <= 0)
			return;
		_autoSkillCounts.compute(skillId, (ignored, count) ->
				count == null ? 1 : count == Integer.MAX_VALUE ? count : count + 1);
	}

	/**
	 * 移除裝備自動 Buff 技能來源
	 * 
	 * @param skillId 技能 ID
	 */
	public void removeAutoSkill(final int skillId) {
		if (skillId <= 0)
			return;
		_autoSkillCounts.computeIfPresent(skillId,
				(ignored, count) -> count <= 1 ? null : count - 1);
	}

	/**
	 * 取得當前應維持的自動 Buff 技能 ID 集合（供 ItemBuffTimer 使用）
	 * 
	 * @return 技能 ID 集合
	 */
	public Set<Integer> getAutoSkills() {
		return Set.copyOf(_autoSkillCounts.keySet());
	}

	/**
	 * 更新 Proc 效果快取
	 * <p>
	 * 掃描 {@link #getEquipSlot() getEquipSlot()} 中的武器與防具，以及 {@link #getDolls()
	 * getDolls()}，
	 * 查詢 WeaponProcTable、EquipProcTable、MagicStoneTable 等，
	 * 建置 {@link #getAttackProcs()} 與 {@link #getDefenseProcs()} 以及
	 * {@link #getAttrRatePlus()}。
	 * 應於 equip/unequip 時呼叫（例如 updateSetBonus 之後）。
	 * </p>
	 */
	public void updateProcCache() {
		_procCacheLock.lock();
		try {
		// [Phase 1] 清空 GenericProcInfo 列表與屬性武器機率加成
		_attackProcs.clear();
		_defenseProcs.clear();
		_attrRatePlus = 0;

		// [Phase 3] 掃描武器：魔法石 → _attackProcs
		final com.lineage.server.model.Instance.L1ItemInstance weaponForProc = getWeapon();
		if (weaponForProc != null) {
			weaponForProc.ensureAbilityFresh();
			final com.lineage.server.model.item.L1ItemAbility weaponAbility = weaponForProc.getAbility();
			final boolean hasMagicStone = weaponAbility
					.has(com.lineage.server.model.item.L1ItemAbility.AbilityType.HAS_MAGIC_STONE);
			// [Debug] 魔法石 Proc 註冊追蹤：被動屬性與 Proc 共用 HAS_MAGIC_STONE，若被動生效但 Proc 未觸發，檢查此日誌
			if (_log.isDebugEnabled()) {
				_log.debug("updateProcCache: weaponId={} HAS_MAGIC_STONE={} rawStoneId={} rawStoneLevel={}",
						weaponForProc.getItemId(), hasMagicStone,
						weaponForProc.getMagicStoneItemId(), weaponForProc.getMagicStoneLevel());
			}
			if (hasMagicStone) {
				final int stoneId = weaponAbility
						.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.MAGIC_STONE_ID);
				final int stoneLevel = weaponAbility
						.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.MAGIC_STONE_LEVEL);
				final com.lineage.server.templates.L1MagicStone stone = com.lineage.server.datatables.MagicStoneTable
						.getInstance().get(stoneId, stoneLevel);
				if (stone != null) {
					// 編碼 stoneId*1000+tier（避免 stoneId>214748 時 int 溢位；tier 需 <1000）
					addProc(new com.lineage.server.model.proc.GenericProcInfo(
							weaponForProc.getItemId(),
							com.lineage.server.model.proc.ProcTriggerType.ON_ATTACK,
							com.lineage.server.model.proc.GenericProcInfo.ProcType.LEGACY_MAGIC_STONE,
							stoneId * 1000 + stoneLevel,
							stone.procChance(),
							0,
							stone.gfxId()));
				} else if (_log.isDebugEnabled()) {
					_log.debug("updateProcCache: 魔法石查無 stoneId={} stoneLevel={}", stoneId, stoneLevel);
				}
			}
		}

		// [Phase 4] 掃描武器：WeaponProcTable、AttrWeapon → _attackProcs
		final com.lineage.server.model.item.L1ItemAbility.AbilityType HAS_WEAPON_PROC_SCAN = com.lineage.server.model.item.L1ItemAbility.AbilityType.HAS_WEAPON_PROC;
		if (weaponForProc != null) {
			final com.lineage.server.model.item.L1ItemAbility weaponAbilityScan = weaponForProc.getAbility();
			final com.lineage.server.datatables.WeaponProcTable weaponProcTable = com.lineage.server.datatables.WeaponProcTable
					.getInstance();
			final int weaponItemId = weaponForProc.getItemId();
			// 舊 DB 道具可能缺 HAS_WEAPON_PROC 標記，改以資料表為準避免 Proc 漏註冊
			if (weaponAbilityScan.get(HAS_WEAPON_PROC_SCAN) == 1 || weaponProcTable.hasProcs(weaponItemId)) {
				final java.util.List<com.lineage.server.model.proc.WeaponProc> weaponProcs = weaponProcTable
						.getProcs(weaponItemId);
				for (final com.lineage.server.model.proc.WeaponProc wp : weaponProcs) {
					addProc(new com.lineage.server.model.proc.GenericProcInfo(
							weaponItemId,
							com.lineage.server.model.proc.ProcTriggerType.ON_ATTACK,
							com.lineage.server.model.proc.GenericProcInfo.ProcType.LEGACY_WEAPON_PROC,
							wp.id(),
							0,
							0,
							wp.gfxId()));
				}
			}
			if (weaponAbilityScan.has(com.lineage.server.model.item.L1ItemAbility.AbilityType.ATTR_ENCHANT_KIND)
					&& weaponAbilityScan
							.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.ATTR_PROBABILITY) > 0) {
				addProc(new com.lineage.server.model.proc.GenericProcInfo(
						weaponForProc.getItemId(),
						com.lineage.server.model.proc.ProcTriggerType.ON_ATTACK,
						com.lineage.server.model.proc.GenericProcInfo.ProcType.LEGACY_ATTR_WEAPON,
						0,
						0,
						0,
						0));
			}
		}

		// [Phase 4] 掃描防具：魔法石、EquipProcTable，onAttack → _attackProcs，onHurt →
		// _defenseProcs；同時累加 ratePlus
		if (_equipSlot != null) {
			for (final com.lineage.server.model.Instance.L1ItemInstance item : _equipSlot.getArmors()) {
				if (item == null)
					continue;
				item.ensureAbilityFresh();
				addAttrRatePlus(parseRatePlus(item));
				// 防具魔法石 Proc（攻擊時觸發，與武器魔法石相同邏輯）
				final com.lineage.server.model.item.L1ItemAbility itemAbility = item.getAbility();
				if (itemAbility.has(com.lineage.server.model.item.L1ItemAbility.AbilityType.HAS_MAGIC_STONE)) {
					final int stoneId = itemAbility
							.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.MAGIC_STONE_ID);
					final int stoneLevel = itemAbility
							.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.MAGIC_STONE_LEVEL);
					final com.lineage.server.templates.L1MagicStone armorStone = com.lineage.server.datatables.MagicStoneTable
							.getInstance().get(stoneId, stoneLevel);
					if (armorStone != null) {
						addProc(new com.lineage.server.model.proc.GenericProcInfo(
								item.getItemId(),
								com.lineage.server.model.proc.ProcTriggerType.ON_ATTACK,
								com.lineage.server.model.proc.GenericProcInfo.ProcType.LEGACY_MAGIC_STONE,
								stoneId * 1000 + stoneLevel,
								armorStone.procChance(),
								0,
								armorStone.gfxId()));
					}
				}
				if (item.getAbility().get(HAS_WEAPON_PROC_SCAN) != 1)
					continue;

				final java.util.List<com.lineage.server.model.proc.EquipProc> equipProcs = com.lineage.server.datatables.EquipProcTable
						.getInstance().getProcs(
								item.getItemId(), item.getEnchantLevel());
				if (equipProcs.isEmpty()) {
					continue;
				}

				for (final com.lineage.server.model.proc.EquipProc proc : equipProcs) {
					if (proc.hasOnAttack()) {
						addProc(new com.lineage.server.model.proc.GenericProcInfo(
								item.getItemId(),
								com.lineage.server.model.proc.ProcTriggerType.ON_ATTACK,
								com.lineage.server.model.proc.GenericProcInfo.ProcType.LEGACY_EQUIP_ATTACK,
								proc.getId(),
								0,
								0,
								proc.getGfxId()));
					}
					if (proc.hasOnHurt()) {
						addProc(new com.lineage.server.model.proc.GenericProcInfo(
								item.getItemId(),
								com.lineage.server.model.proc.ProcTriggerType.ON_DEFENSE,
								com.lineage.server.model.proc.GenericProcInfo.ProcType.LEGACY_EQUIP_HURT,
								proc.getId(),
								0,
								0,
								proc.getGfxId()));
					}
				}
			}
		}

		// [Phase 2] 掃描魔法娃娃 DAMAGE_REDUCTION → _defenseProcs（Doll_DmgDownR 遷移）
		if (getDolls() != null && !getDolls().isEmpty()) {
			for (final com.lineage.server.model.Instance.L1DollInstance doll : getDolls().values()) {
				final com.lineage.server.templates.L1Doll type = doll.get_type();
				if (type == null)
					continue;
				for (final com.lineage.server.model.proc.DollActiveProc proc : type.getActiveProcs()) {
					if (proc.type() == com.lineage.server.model.proc.DollActiveProc.ProcType.DAMAGE_REDUCTION) {
						final com.lineage.server.model.proc.GenericProcInfo info = new com.lineage.server.model.proc.GenericProcInfo(
								type.get_itemid(),
								com.lineage.server.model.proc.ProcTriggerType.ON_DEFENSE,
								com.lineage.server.model.proc.GenericProcInfo.ProcType.DAMAGE_REDUCTION,
								proc.param1(),
								proc.param2(),
								0,
								proc.param3());
						addProc(info);
					}
				}
				// [娃娃覺醒系統] 娃娃道具的覺醒主動能力（減傷，支援多重覺醒）
				final com.lineage.server.model.Instance.L1ItemInstance dollItem = getInventory()
						.getItem(doll.getItemObjId());
				if (dollItem != null && !dollItem.getAwakenPowerIds().isEmpty()) {
					for (final int powerId : dollItem.getAwakenPowerIds()) {
						final com.lineage.server.datatables.DollPowerTable.DollPowerRow awRow = com.lineage.server.datatables.DollPowerTable
								.getActivePowerRow(powerId);
						if (awRow != null) {
							final com.lineage.server.model.proc.DollActiveProc awProc = com.lineage.server.datatables.DollPowerTable
									.buildActiveProcFromRow(awRow);
							if (awProc != null && awProc
									.type() == com.lineage.server.model.proc.DollActiveProc.ProcType.DAMAGE_REDUCTION) {
								addProc(new com.lineage.server.model.proc.GenericProcInfo(
										type.get_itemid(),
										com.lineage.server.model.proc.ProcTriggerType.ON_DEFENSE,
										com.lineage.server.model.proc.GenericProcInfo.ProcType.DAMAGE_REDUCTION,
										awProc.param1(),
										awProc.param2(),
										0,
										awProc.param3()));
							}
						}
					}
				}
			}
		}

		// [Phase 3] 掃描魔法娃娃 SKILL / DAMAGE_BONUS → _attackProcs（Doll_Skill、Doll_DmgR 遷移）
		if (getDolls() != null && !getDolls().isEmpty()) {
			for (final com.lineage.server.model.Instance.L1DollInstance doll : getDolls().values()) {
				final com.lineage.server.templates.L1Doll type = doll.get_type();
				if (type == null)
					continue;
				for (final com.lineage.server.model.proc.DollActiveProc proc : type.getActiveProcs()) {
					if (proc.type() == com.lineage.server.model.proc.DollActiveProc.ProcType.SKILL) {
						addProc(new com.lineage.server.model.proc.GenericProcInfo(
								type.get_itemid(),
								com.lineage.server.model.proc.ProcTriggerType.ON_ATTACK,
								com.lineage.server.model.proc.GenericProcInfo.ProcType.SKILL,
								proc.param1(),
								proc.param2(),
								0,
								proc.param3()));
					} else if (proc.type() == com.lineage.server.model.proc.DollActiveProc.ProcType.DAMAGE_BONUS) {
						addProc(new com.lineage.server.model.proc.GenericProcInfo(
								type.get_itemid(),
								com.lineage.server.model.proc.ProcTriggerType.ON_ATTACK,
								com.lineage.server.model.proc.GenericProcInfo.ProcType.DAMAGE,
								proc.param1(),
								proc.param2(),
								proc.attrType(),
								proc.param3()));
					}
				}
				// [娃娃覺醒系統] 娃娃道具的覺醒主動能力（施技、加傷，支援多重覺醒）
				final com.lineage.server.model.Instance.L1ItemInstance dollItem = getInventory()
						.getItem(doll.getItemObjId());
				if (dollItem != null && !dollItem.getAwakenPowerIds().isEmpty()) {
					for (final int powerId : dollItem.getAwakenPowerIds()) {
						final com.lineage.server.datatables.DollPowerTable.DollPowerRow awRow = com.lineage.server.datatables.DollPowerTable
								.getActivePowerRow(powerId);
						if (awRow != null) {
							final com.lineage.server.model.proc.DollActiveProc awProc = com.lineage.server.datatables.DollPowerTable
									.buildActiveProcFromRow(awRow);
							if (awProc != null) {
								if (awProc.type() == com.lineage.server.model.proc.DollActiveProc.ProcType.SKILL) {
									addProc(new com.lineage.server.model.proc.GenericProcInfo(
											type.get_itemid(),
											com.lineage.server.model.proc.ProcTriggerType.ON_ATTACK,
											com.lineage.server.model.proc.GenericProcInfo.ProcType.SKILL,
											awProc.param1(),
											awProc.param2(),
											0,
											awProc.param3()));
								} else if (awProc
										.type() == com.lineage.server.model.proc.DollActiveProc.ProcType.DAMAGE_BONUS) {
									addProc(new com.lineage.server.model.proc.GenericProcInfo(
											type.get_itemid(),
											com.lineage.server.model.proc.ProcTriggerType.ON_ATTACK,
											com.lineage.server.model.proc.GenericProcInfo.ProcType.DAMAGE,
											awProc.param1(),
											awProc.param2(),
											awProc.attrType(),
											awProc.param3()));
								}
							}
						}
					}
				}
			}
		}

		// [守護星盤] 掃描已解鎖節點主動能力 → _attackProcs / _defenseProcs
		com.lineage.server.model.AstrolabeSystem.getInstance().registerActiveProcs(this);

		// [Phase 4] 武器與防具 Proc 已遷移至 _attackProcs/_defenseProcs（LEGACY_*）
		// _attrRatePlus 已於防具掃描時累加完成
		} finally {
			_procCacheLock.unlock();
		}
	}

	/**
	 * 從裝備的 classname 解析 RatePlus 數值（對應 L1AttackPower 屬性武器機率加成）
	 * 僅對 type2==2 且 type 為 1 或 8~12 的防具計入
	 */
	private int parseRatePlus(final com.lineage.server.model.Instance.L1ItemInstance item) {
		if (item == null || item.getItem() == null)
			return 0;
		final int type2 = item.getItem().getType2();
		final int type = item.getItem().getType();
		if (type2 != 2)
			return 0;
		if (type != 1 && (type < 8 || type > 12))
			return 0;
		try {
			final String classname = item.getItem().getclassname();
			if (classname == null)
				return 0;
			final String[] parts = classname.split(" ");
			if (parts.length > 2 && "RatePlus".equals(parts[0])) {
				return Math.max(0, Integer.parseInt(parts[2]));
			}
		} catch (final Exception e) {
			_log.debug("getRatePlusByItem 解析失敗", e);
		}
		return 0;
	}

	// 宣告套裝訊息變數 (記憶體)
	private String _lastSetMsg = "";

	/**
	 * 更新套裝效果
	 * 計算玩家身上的裝備並更新套裝效果能力值
	 * 同時處理套裝變身邏輯
	 */
	public void updateSetBonus() {
		this.updateSetBonus(true);
	}

	/**
	 * 重建套裝、卡片、星盤與 Proc 狀態。
	 *
	 * @param notifyClient true 發送面板/訊息封包；false 用於登入背包快照前的靜默還原
	 */
	public void updateSetBonus(final boolean notifyClient) {
		// 1. 裝備套裝效果
		com.lineage.server.model.L1SetBonusManager.SetBonusResult result = com.lineage.server.model.L1SetBonusManager
				.getInstance().calculateSetBonus(this);

		this._setAbility = result.getAbility();

		// 2. 鑲嵌卡片套裝效果（跨裝備）
		recalcCardSetBonuses();

		// 2.5 星盤能力疊加
		com.lineage.server.model.AstrolabeSystem.getInstance().applyAstrolabePowers(this);

		// 3. 處理裝備套裝變身
		handleSetBonusPoly(result.getPolyId());

		// 4. 處理裝備套裝訊息
		String newMsg = result.getMessage();
		if (newMsg != null && !newMsg.equals(_lastSetMsg)) {
			if (notifyClient && !newMsg.isEmpty()) {
				this.sendPackets(new S_ServerMessage(166, "\\f>套裝效果:"));
				for (final String line : newMsg.split(", ")) {
					this.sendPackets(new S_ServerMessage(166, line));
				}
			}
			_lastSetMsg = newMsg;
		}

		// 5. 更新 Proc 快取（統一戰鬥觸發系統）
		updateProcCache();

		// 6. 更新面板；登入還原階段必須保持靜默，讓 S_InvList 成為第一個物品快照。
		if (notifyClient) {
			this.sendPackets(new S_OwnCharStatus(this));
			this.sendPackets(new S_PacketBox(S_PacketBox.EVASION_UPDATE, this.getEr()));
		}
	}

	/**
	 * 更新 Buff/技能 效果能力值
	 * 計算玩家當前所有生效的 Buff 並更新相關屬性
	 * <p>
	 * 此方法採用「先回滾再重算」的安全策略，避免重複疊加問題
	 * </p>
	 */
	public void updateBuffStatus() {
		updateBuffStatus(true);
	}

	/**
	 * Recalculates Buff-derived abilities.
	 *
	 * @param notifyClient true while the character owns a live game connection;
	 *                     false during login restore or logout cleanup
	 */
	public void updateBuffStatus(final boolean notifyClient) {
		try {
			// 1. 回滾舊的 Buff 屬性（如果有快取）
			if (_cachedBuffAbility != null) {
				L1PcBuffAbilityCalc.applyTo(this, _cachedBuffAbility, -1);
			}

			// 2. 計算新的 Buff 屬性總和
			final com.lineage.server.model.item.L1ItemAbility newBuffAbility = L1PcBuffAbilityCalc.calculate(this);

			// 3. 套用新的 Buff 屬性
			if (newBuffAbility != null) {
				L1PcBuffAbilityCalc.applyTo(this, newBuffAbility, 1);
			}

			// 4. 更新快取
			_cachedBuffAbility = (newBuffAbility != null) ? newBuffAbility
					: new com.lineage.server.model.item.L1ItemAbility();

			// 5. 更新面板（登出／登入還原期間只重算伺服器狀態）
			if (notifyClient) {
				this.sendPackets(new S_OwnCharStatus(this));
				this.sendPackets(new S_SPMR(this));
				this.sendPackets(new com.lineage.server.serverpackets.S_HPUpdate(this.getCurrentHp(), this.getMaxHp()));
				this.sendPackets(new com.lineage.server.serverpackets.S_MPUpdate(this.getCurrentMp(), this.getMaxMp()));
				this.sendPackets(new com.lineage.server.serverpackets.S_OwnCharStatus2(this));
			}

		} catch (Exception e) {
			// 記錄錯誤但不中斷遊戲流程
			org.apache.logging.log4j.LogManager.getLogger(L1PcInstance.class)
					.error("更新 Buff 狀態失敗: " + this.getName(), e);
		}
	}

	/**
	 * 重新計算鑲嵌卡片套裝效果（跨裝備）
	 * 掃描全身已裝備的卡片，將符合的組合能力合併至 _setAbility，並發送綠字訊息
	 */
	public void recalcCardSetBonuses() {
		final com.lineage.server.model.L1CardSetBonusManager.CardSetResult result = com.lineage.server.model.L1CardSetBonusManager
				.getInstance().calculateCardSets(this);

		// 合併能力至 _setAbility
		final com.lineage.server.model.item.L1ItemAbility cardAbility = result.getAbility();
		if (cardAbility != null) {
			for (final com.lineage.server.model.item.L1ItemAbility.AbilityType type : com.lineage.server.model.item.L1ItemAbility.AbilityType
					.values()) {
				final int value = cardAbility.get(type);
				if (value != 0) {
					this._setAbility.add(type, value);
				}
			}
		}

		// 處理綠字訊息：僅發送新增的組合
		final List<String> newNames = result.getComboNames();
		if (newNames != null) {
			for (final String name : newNames) {
				if (name != null && !name.isEmpty() && !_currentCardComboNames.contains(name)) {
					this.sendPackets(new S_ServerMessage(166, "[鑲嵌組合] " + name + " 生效"));
				}
			}
			_currentCardComboNames = new ArrayList<>(newNames);
		} else {
			_currentCardComboNames.clear();
		}
	}

	/**
	 * 處理套裝變身邏輯
	 * 
	 * @param newPolyId 新的套裝變身ID（0 表示無變身）
	 */
	private void handleSetBonusPoly(int newPolyId) {
		// 只有在變身ID改變時才處理
		if (newPolyId != _setPolyId) {

			// 狀況 1: 套裝效果消失 (例如脫裝)
			if (_setPolyId > 0 && newPolyId == 0) {
				// 只有當目前變身是「舊套裝變身」時，才解除
				if (this.getTempCharGfx() == _setPolyId) {
					L1PolyMorph.undoPoly(this);
				}
				_setPolyId = 0;
			}

			// 狀況 2: 套裝效果改變/新增 (例如穿裝、或者從2件套升級4件套)
			else if (newPolyId > 0) {
				// 判斷是否需要執行變身：
				// 1. 玩家目前沒變身 (原始外型)
				// 2. [新增] 玩家目前變身就是「舊的套裝變身」 (代表是套裝升級/降級，應該覆蓋)
				boolean needPoly = (this.getTempCharGfx() == this.getClassId()) ||
						(this.getTempCharGfx() == _setPolyId);

				if (needPoly) {
					// 執行變身
					L1PolyMorph.doPoly(
							this,
							newPolyId,
							-1,
							L1PolyMorph.MORPH_BY_ITEMMAGIC);
					_setPolyId = newPolyId;
				} else {
					// 玩家處於其他變身狀態 (如變身卷軸、變戒)，不強制覆蓋
					// 但必須更新 _setPolyId，這樣當玩家解除卷軸變身時，下次穿脫裝備才能正確判定
					_setPolyId = newPolyId;
				}
			}
		}
	}

	// ========================================================================
	// 套裝效果掛鉤：覆寫所有屬性的 getter 方法，加入套裝加成
	// ========================================================================

	/**
	 * 力量（含套裝加成）
	 */
	@Override
	public short getStr() {
		return (short) com.lineage.server.utils.RangeInt.ensure(
				super.getStr() + _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.STR),
				1, 254);
	}

	/**
	 * 敏捷（含套裝加成）
	 */
	@Override
	public short getDex() {
		return (short) com.lineage.server.utils.RangeInt.ensure(
				super.getDex() + _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.DEX),
				1, 254);
	}

	/**
	 * 體質（含套裝加成）
	 */
	@Override
	public short getCon() {
		return (short) com.lineage.server.utils.RangeInt.ensure(
				super.getCon() + _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.CON),
				1, 254);
	}

	/**
	 * 智力（含套裝加成）
	 */
	@Override
	public short getInt() {
		return (short) com.lineage.server.utils.RangeInt.ensure(
				super.getInt() + _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.INT),
				1, 254);
	}

	/**
	 * 精神（含套裝加成）
	 */
	@Override
	public short getWis() {
		return (short) com.lineage.server.utils.RangeInt.ensure(
				super.getWis() + _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.WIS),
				1, 254);
	}

	/**
	 * 魅力（含套裝加成）
	 */
	@Override
	public short getCha() {
		return (short) com.lineage.server.utils.RangeInt.ensure(
				super.getCha() + _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.CHA),
				1, 254);
	}

	/**
	 * 取得寵物控制用有效魅力（含職業加成，依 charsettings.properties 設定）
	 * @return 有效魅力值
	 */
	public int getChaForPet() {
		int bonus = 0;
		if (isCrown()) bonus = ConfigCharSetting.PRINCE_CHA_FOR_PET;
		else if (isKnight()) bonus = ConfigCharSetting.KNIGHT_CHA_FOR_PET;
		else if (isElf()) bonus = ConfigCharSetting.ELF_CHA_FOR_PET;
		else if (isWizard()) bonus = ConfigCharSetting.WIZARD_CHA_FOR_PET;
		else if (isDarkelf()) bonus = ConfigCharSetting.DARKELF_CHA_FOR_PET;
		else if (isDragonKnight()) bonus = ConfigCharSetting.DRAGONKNIGHT_CHA_FOR_PET;
		else if (isIllusionist()) bonus = ConfigCharSetting.ILLUSIONIST_CHA_FOR_PET;
		return getCha() + bonus;
	}

	private static final int MAX_CONTROLLED_PET_COUNT = 8;

	/** 寵物與召喚物共用的剩餘控制數量。 */
	public int getRemainingControlledPetSlots() {
		return Math.max(0, MAX_CONTROLLED_PET_COUNT - getPetList().size());
	}

	/**
	 * 檢查玩家是否可再攜帶指定魅力消耗的寵物（項圈型）
	 * 含數量上限(8)與魅力檢查，每次呼叫即時讀取當前 petList
	 * @param chaCost 欲領取寵物的魅力消耗（pettypes.ChaCost）
	 * @return true 若可攜帶
	 */
	public boolean canTakePet(final int chaCost) {
		final Object[] petList = getPetList().values().toArray();
		if (getRemainingControlledPetSlots() <= 0) {
			return false;
		}
		int petCost = 0;
		for (final Object pet : petList) {
			petCost += ((com.lineage.server.model.Instance.L1NpcInstance) pet).getPetcost();
		}
		final int remaining = getChaForPet() - petCost;
		return remaining >= chaCost;
	}

	/**
	 * 防禦力（含套裝加成）
	 * 注意：AC 是負數為佳，套裝加成會減少 AC（增加防禦）
	 */
	@Override
	public int getAc() {
		int ac = super.getAc();
		// 套裝的 AC 加成（負數表示增加防禦）
		ac -= _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.AC);
		final int minAc = com.lineage.config.ConfigClient.isROClient() ? Integer.MIN_VALUE : -211;
		return com.lineage.server.utils.RangeInt.ensure(ac, minAc, 44);
	}

	/**
	 * 魔防（含套裝加成）
	 */
	@Override
	public int getMr() {
		int mr = super.getMr();
		mr += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.MR);
		return Math.max(0, mr);
	}

	/**
	 * 火屬性防禦（含套裝加成）
	 */
	@Override
	public int getFire() {
		int fire = super.getFire();
		fire += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.DEF_FIRE);
		return com.lineage.server.utils.RangeInt.ensure(fire, -128, 127);
	}

	/**
	 * 水屬性防禦（含套裝加成）
	 */
	@Override
	public int getWater() {
		int water = super.getWater();
		water += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.DEF_WATER);
		return com.lineage.server.utils.RangeInt.ensure(water, -128, 127);
	}

	/**
	 * 風屬性防禦（含套裝加成）
	 */
	@Override
	public int getWind() {
		int wind = super.getWind();
		wind += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.DEF_WIND);
		return com.lineage.server.utils.RangeInt.ensure(wind, -128, 127);
	}

	/**
	 * 地屬性防禦（含套裝加成）
	 */
	@Override
	public int getEarth() {
		int earth = super.getEarth();
		earth += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.DEF_EARTH);
		return com.lineage.server.utils.RangeInt.ensure(earth, -128, 127);
	}

	/**
	 * 昏迷耐性（含套裝加成）
	 */
	@Override
	public int getRegistStun() {
		int registStun = super.getRegistStun();
		registStun += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.REGIST_STUN);
		return com.lineage.server.utils.RangeInt.ensure(registStun, -128, 127);
	}

	/**
	 * 石化耐性（含套裝加成）
	 */
	@Override
	public int getRegistStone() {
		int registStone = super.getRegistStone();
		registStone += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.REGIST_STONE);
		return com.lineage.server.utils.RangeInt.ensure(registStone, -128, 127);
	}

	/**
	 * 睡眠耐性（含套裝加成）
	 */
	@Override
	public int getRegistSleep() {
		int registSleep = super.getRegistSleep();
		registSleep += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.REGIST_SLEEP);
		return com.lineage.server.utils.RangeInt.ensure(registSleep, -128, 127);
	}

	/**
	 * 寒冰耐性（含套裝加成）
	 */
	@Override
	public int getRegistFreeze() {
		int registFreeze = super.getRegistFreeze();
		registFreeze += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.REGIST_FREEZE);
		return com.lineage.server.utils.RangeInt.ensure(registFreeze, -128, 127);
	}

	/**
	 * 暗黑耐性（含套裝加成）
	 */
	@Override
	public int getRegistBlind() {
		int registBlind = super.getRegistBlind();
		registBlind += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.REGIST_BLIND);
		return com.lineage.server.utils.RangeInt.ensure(registBlind, -128, 127);
	}

	/**
	 * 支撐耐性（含套裝加成）
	 */
	@Override
	public int getRegistSustain() {
		int registSustain = super.getRegistSustain();
		registSustain += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.REGIST_SUSTAIN);
		return com.lineage.server.utils.RangeInt.ensure(registSustain, -128, 127);
	}

	/**
	 * 近距離傷害（含套裝加成）
	 */
	@Override
	public int getDmgup() {
		int dmgup = super.getDmgup();
		dmgup += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.DMG);
		return com.lineage.server.utils.RangeInt.ensure(dmgup, -128, 127);
	}

	/**
	 * 近距離命中（含套裝加成）
	 */
	@Override
	public int getHitup() {
		int hitup = super.getHitup();
		hitup += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.HIT);
		return com.lineage.server.utils.RangeInt.ensure(hitup, -128, 127);
	}

	/**
	 * 遠距離傷害（含套裝加成）
	 */
	@Override
	public int getBowDmgup() {
		int bowDmgup = super.getBowDmgup();
		bowDmgup += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.BOW_DMG);
		return com.lineage.server.utils.RangeInt.ensure(bowDmgup, -128, 127);
	}

	/**
	 * 遠距離命中（含套裝加成）
	 */
	@Override
	public int getBowHitup() {
		int bowHitup = super.getBowHitup();
		bowHitup += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.BOW_HIT);
		return com.lineage.server.utils.RangeInt.ensure(bowHitup, -128, 127);
	}

	/**
	 * 最大HP（含套裝加成）
	 */
	@Override
	public int getMaxHp() {
		int maxHp = super.getMaxHp();
		maxHp += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.HP);
		// RO版: 上限 999999, 公版: 上限 32767
		int maxLimit = com.lineage.config.ConfigClient.isROClient() 
			? com.lineage.server.model.L1Character.MAX_HP_MP_RO 
			: com.lineage.server.model.L1Character.MAX_HP_MP_STANDARD;
		return com.lineage.server.utils.RangeInt.ensure(maxHp, 1, maxLimit);
	}

	/**
	 * 最大MP（含套裝加成）
	 */
	@Override
	public int getMaxMp() {
		int maxMp = super.getMaxMp();
		maxMp += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.MP);
		// RO版: 上限 999999, 公版: 上限 32767
		int maxLimit = com.lineage.config.ConfigClient.isROClient() 
			? com.lineage.server.model.L1Character.MAX_HP_MP_RO 
			: com.lineage.server.model.L1Character.MAX_HP_MP_STANDARD;
		return com.lineage.server.utils.RangeInt.ensure(maxMp, 0, maxLimit);
	}

	/**
	 * 加載指定PC資料
	 * 
	 * @param charName
	 *                 PC名稱
	 * @return
	 */
	public static L1PcInstance load(final String charName) {
		L1PcInstance result = null;
		try {
			result = CharacterTable.get().loadCharacter(charName);

		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
		return result;
	}

	/**
	 * 人物資料存檔
	 * 
	 * @throws Exception
	 */
	public void save() throws Exception {
        if (com.lineage.server.model.MigrationDeparture.isDeparted(getId())) return;
		if (isGhost() || isInCharReset()) {
			return;
		}
		// Capture the immutable bundle on this thread; the keyed persistence lane
		// coalesces later saves without ever retaining this mutable player object.
		CharacterTable.get().storeCharacter(this);
	}

	/**
	 * 【強制同步存檔】
	 * 繞過 ThreadPool，確保當下 Thread 直接將數據寫入資料庫，避免關機或登出時遺失。
	 */
	public void saveSync() throws Exception {
        if (com.lineage.server.model.MigrationDeparture.isDeparted(getId())) return;
		if (isGhost() || isInCharReset()) {
			return;
		}
		try {
			// The same keyed lane waits behind any in-flight older write and makes
			// this exact snapshot the final version for logout/shutdown.
			CharacterTable.get().storeCharacterSync(this);
		} catch (Exception e) {
			_log.error("人物同步存檔失敗: " + getName(), e);
			throw e;
		}
	}

	/**
	 * 背包資料存檔
	 */
	public void saveInventory() {
        if (com.lineage.server.model.MigrationDeparture.isDeparted(getId())) return;
		getInventory().saveAllItems();
	}

	public L1PcInventory.InventorySaveResult saveInventoryWithResult() {
		return getInventory().saveAllItemsWithResult();
	}

	public double getMaxWeight() {
		final int str = getStr();
		final int con = getCon();
		double maxWeight = (150 * (Math.floor(0.6 * str + 0.4 * con + 1))) * get_weightUP();

		double weightReductionByArmor = getWeightReduction(); // 減重設置
		weightReductionByArmor /= 100;

		int weightReductionByMagic = 0;
		if (hasSkillEffect(DECREASE_WEIGHT)) { // 減輕負重
			weightReductionByMagic = 180;
		}

		double originalWeightReduction = 0; // 原始屬性造成的負重減免
		originalWeightReduction += 0.04 * (getOriginalStrWeightReduction() + getOriginalConWeightReduction());

		final double weightReduction = 1 + weightReductionByArmor + originalWeightReduction;

		maxWeight *= weightReduction;

		maxWeight += weightReductionByMagic;

		maxWeight *= ConfigRate.RATE_WEIGHT_LIMIT; // 服務器提高設置

		return maxWeight;
	}

	/**
	 * 神聖疾走效果 行走加速效果 風之疾走效果 生命之樹果實效果
	 * 
	 * @return
	 */
	public boolean isFastMovable() {
		return (this.hasSkillEffect(HOLY_WALK) || this.hasSkillEffect(MOVING_ACCELERATION)
				|| this.hasSkillEffect(WIND_WALK) || this.hasSkillEffect(STATUS_RIBRAVE));
	}

	/**
	 * 血之渴望效果
	 * 
	 * @return
	 */
	public boolean isFastAttackable() {
		return this.hasSkillEffect(BLOODLUST);
	}

	/**
	 * 勇敢藥水效果
	 * 
	 * @return
	 */
	public boolean isBrave() {
		return this.hasSkillEffect(STATUS_BRAVE);
	}

	/**
	 * 精靈餅乾效果
	 * 
	 * @return
	 */
	public boolean isElfBrave() {
		return this.hasSkillEffect(STATUS_ELFBRAVE);
	}

	/**
	 * 巧克力蛋糕效果
	 * 
	 * @return
	 */
	public boolean isBraveX() {
		return this.hasSkillEffect(STATUS_BRAVE3);
	}

	/**
	 * 加速效果
	 * 
	 * @return
	 */
	public boolean isHaste() {
		return (this.hasSkillEffect(STATUS_HASTE) || this.hasSkillEffect(HASTE) || this.hasSkillEffect(GREATER_HASTE)
				|| (this.getMoveSpeed() == 1));
	}

	private static final AtomicLong INVIS_DELAY_OWNER_SEQUENCE = new AtomicLong();
	private final long _invisDelayOwnerEpoch = INVIS_DELAY_OWNER_SEQUENCE.incrementAndGet();
	private final Set<Long> _invisDelayTokens = new HashSet<>();
	private long _nextInvisDelayToken;
	private volatile int invisDelayCounter = 0;

	public boolean isInvisDelay() {
		return (this.invisDelayCounter > 0);
	}

	private final ReentrantLock _invisTimerLock = new ReentrantLock();

	/**
	 * Legacy compatibility entry. Negative adjustments saturate at the number of
	 * active generation tokens, so a stale callback can never underflow or clear a
	 * newer timer.
	 */
	@Deprecated
	public void addInvisDelayCounter(final int counter) {
		_invisTimerLock.lock();
		try {
			final long adjusted = (long) this.invisDelayCounter + counter;
			final long floor = _invisDelayTokens.size();
			this.invisDelayCounter = (int) Math.min(Integer.MAX_VALUE,
					Math.max(floor, adjusted));
		} finally {
			_invisTimerLock.unlock();
		}
	}

	long registerInvisDelayToken() {
		_invisTimerLock.lock();
		try {
			final long token = ++_nextInvisDelayToken;
			_invisDelayTokens.add(token);
			invisDelayCounter = (int) Math.min(Integer.MAX_VALUE,
					Math.max((long) invisDelayCounter + 1L, _invisDelayTokens.size()));
			return token;
		} finally {
			_invisTimerLock.unlock();
		}
	}

	long invisDelayOwnerEpoch() {
		return _invisDelayOwnerEpoch;
	}

	/** Completes or rolls back exactly one timer owned by this PC incarnation. */
	public void completeInvisDelayToken(final long ownerEpoch, final long token) {
		_invisTimerLock.lock();
		try {
			if (ownerEpoch != _invisDelayOwnerEpoch || !_invisDelayTokens.remove(token)) {
				return;
			}
			if (invisDelayCounter > 0) {
				invisDelayCounter--;
			}
			if (invisDelayCounter < _invisDelayTokens.size()) {
				invisDelayCounter = _invisDelayTokens.size();
			}
		} finally {
			_invisTimerLock.unlock();
		}
	}

	private static final long DELAY_INVIS = 3000L;

	/**
	 * 啟用隱身時間軸設置
	 */
	public void beginInvisTimer() {
		final long token = registerInvisDelayToken();
		final ScheduledFuture<?> future = pcScheduleTask("L1PcInvisDelay",
				new L1PcInvisDelay(this.getId(), _invisDelayOwnerEpoch, token), DELAY_INVIS);
		if (future == null) {
			// Roll back this exact generation only. A concurrent newer timer remains.
			completeInvisDelayToken(_invisDelayOwnerEpoch, token);
		}
	}

	public void addLawful(final int i) {
		_lawfulMutationLock.lock();
		try {
			int lawful = this.getLawful() + i;
			if (lawful > 32767) {
				lawful = 32767;

			} else if (lawful < -32768) {
				lawful = -32768;
			}
			this.setLawful(lawful);
			onChangeLawful();
		} finally {
			_lawfulMutationLock.unlock();
		}
	}

	/**
	 * 更新經驗質
	 * 
	 * @param exp
	 */
	public void addExp(final long exp) {
		_expMutationLock.lock();
		try {
			final long newexp = _exp + exp;
			setExp(newexp);
		} finally {
			_expMutationLock.unlock();
		}
		onChangeExp();
	}

	/**
	 * 增加貢獻度
	 * 
	 * @param contribution
	 */
	public void addContribution(final int contribution) {
		_contributionMutationLock.lock();
		try {
			// Re-check inside the same lock used by the monthly reset. A CalcExp
			// thread that observed the old global flag cannot add after that reset.
			if (!World.get().isProcessingContributionTotal()) {
				_contribution += contribution;
			}
		} finally {
			_contributionMutationLock.unlock();
		}
	}

	/**
	 * 等級提升的判斷
	 * 
	 * @param gap
	 */
	private void levelUp(final int gap) {
		final int highBefore = getHighLevel();
		final int levelBefore = getLevel();
		resetLevel();
		for (int i = 0; i < gap; i++) {
			final short randomHp = CalcStat.calcStatHp(getType(), getBaseMaxHp(), getBaseCon(), getOriginalHpup());
			final short randomMp = CalcStat.calcStatMp(getType(), getBaseMaxMp(), getBaseWis(), getOriginalMpup());
			addBaseMaxHp(randomHp);
			addBaseMaxMp(randomMp);
		}

		resetBaseHitup();
		resetBaseDmgup();
		resetBaseAc();
		resetBaseMr();
		if (getLevel() > highBefore) {
			final int newLv = getLevel();
			setHighLevel(newLv);
			final int tpGain = ExpTable.sumTalentPointsInRange(highBefore + 1, newLv);
			if (tpGain > 0) {
				sendPackets(new S_ServerMessage(166, "獲得天賦點數" + tpGain + "點"));
			}
		}

		setCurrentHp(getMaxHp());
		setCurrentMp(getMaxMp());

		try {
			// 人物資料存檔
			save();

		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);

		} finally {
			// 更新人物資訊
			sendPackets(new S_OwnCharStatus(this));

			// 地圖等級限制判斷
			MapLevelTable.get().get_level(getMapId(), this);
			showWindows();
			// 升級獎勵系統檢查
			com.lineage.server.datatables.LevelRewardTable.get().checkAndGive(this, levelBefore + 1, getLevel());
		}
	}

	/**
	 * 判斷是否展示視窗<BR>
	 * 能力質/任務
	 */
	public void showWindows() {
		if (power()) {
			this.sendPackets(new S_Bonusstats(this.getId()));
		}
	}

	/**
	 * 展示任務室窗
	 */
	public void isWindows() {
		// 判斷是否出現能力選取視窗
		if (power()) {// 是
			this.sendPackets(new S_NPCTalkReturn(this.getId(), "y_qs_10"));

		} else {// 不是
			this.sendPackets(new S_NPCTalkReturn(this.getId(), "y_qs_00"));
		}
	}

	/**
	 * 判斷是否出現能力選取視窗
	 * 
	 * @return
	 */
	public boolean power() {
		if (this.getLevel() >= 51) {
			if (this.getLevel() - 50 > this.getBonusStats()) {
				int power = getBaseStr() + getBaseDex() + getBaseCon() + getBaseInt() + getBaseWis() + getBaseCha();
				if (power < ConfigAlt.POWER * 6) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 等級下降
	 * 
	 * @param gap
	 */
	private void levelDown(final int gap) {
		this.resetLevel();

		for (int i = 0; i > gap; i--) {
			// 降級時直接扣除隨機值，故 base 值設為 0
			final short randomHp = CalcStat.calcStatHp(this.getType(), 0, this.getBaseCon(), this.getOriginalHpup());
			final short randomMp = CalcStat.calcStatMp(this.getType(), 0, this.getBaseWis(), this.getOriginalMpup());
			this.addBaseMaxHp((short) -randomHp);
			this.addBaseMaxMp((short) -randomMp);
		}

		if (this.getLevel() == 1) {
			final int initHp = CalcInitHpMp.calcInitHp(this);
			final int initMp = CalcInitHpMp.calcInitMp(this);
			this.addBaseMaxHp((short) -this.getBaseMaxHp());
			this.addBaseMaxHp((short) initHp);
			this.setCurrentHp((short) initHp);
			this.addBaseMaxMp((short) -this.getBaseMaxMp());
			this.addBaseMaxMp((short) initMp);
			this.setCurrentMp((short) initMp);
		}

		this.resetBaseHitup();
		this.resetBaseDmgup();
		this.resetBaseAc();
		this.resetBaseMr();

		try {
			// 存入資料
			this.save();

		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);

		} finally {
			// 更新人物資訊
			sendPackets(new S_OwnCharStatus(this));

			// 地圖等級限制判斷
			MapLevelTable.get().get_level(getMapId(), this);
		}
	}

	private boolean _ghost = false; // 鬼魂狀態

	/**
	 * 鬼魂狀態
	 * 
	 * @return
	 */
	public boolean isGhost() {
		return this._ghost;
	}

	/**
	 * 設置鬼魂狀態
	 * 
	 * @param flag
	 */
	private void setGhost(final boolean flag) {
		this._ghost = flag;
	}

	private int _ghostTime = -1; // 鬼魂狀態時間

	/**
	 * 鬼魂狀態時間
	 * 
	 * @return
	 */
	public int get_ghostTime() {
		return this._ghostTime;
	}

	/**
	 * 設置鬼魂狀態時間
	 * 
	 * @param ghostTime
	 */
	public void set_ghostTime(final int ghostTime) {
		this._ghostTime = ghostTime;
	}

	private boolean _ghostCanTalk = true; // 鬼魂狀態NPC對話允許

	/**
	 * 鬼魂狀態NPC對話允許
	 * 
	 * @return
	 */
	public boolean isGhostCanTalk() {
		return this._ghostCanTalk;
	}

	/**
	 * 設置鬼魂狀態NPC對話允許
	 * 
	 * @param flag
	 */
	private void setGhostCanTalk(final boolean flag) {
		this._ghostCanTalk = flag;
	}

	private boolean _isReserveGhost = false; // 準備鬼魂狀態解除

	/**
	 * 準備鬼魂狀態解除
	 * 
	 * @return
	 */
	public boolean isReserveGhost() {
		return this._isReserveGhost;
	}

	/**
	 * 準備鬼魂狀態解除
	 * 
	 * @param flag
	 */
	private void setReserveGhost(final boolean flag) {
		this._isReserveGhost = flag;
	}

	/**
	 * 鬼魂模式傳送
	 * 
	 * @param locx
	 * @param locy
	 * @param mapid
	 * @param canTalk
	 */
	public void beginGhost(final int locx, final int locy, final short mapid, final boolean canTalk) {
		this.beginGhost(locx, locy, mapid, canTalk, 0);
	}

	/**
	 * 鬼魂模式傳送
	 * 
	 * @param locx
	 * @param locy
	 * @param mapid
	 * @param canTalk
	 * @param sec
	 */
	public void beginGhost(final int locx, final int locy, final short mapid, final boolean canTalk, final int sec) {
		if (this.isGhost()) {
			return;
		}
		this.setGhost(true);
		this._ghostSaveLocX = this.getX();
		this._ghostSaveLocY = this.getY();
		this._ghostSaveMapId = this.getMapId();
		this._ghostSaveHeading = this.getHeading();
		this.setGhostCanTalk(canTalk);
		L1Teleport.teleport(this, locx, locy, mapid, 5, true);
		if (sec > 0) {
			this.set_ghostTime(sec);
		}
	}

	/**
	 * 離開鬼魂模式(傳送回出發點)
	 */
	public void makeReadyEndGhost() {
		this.setReserveGhost(true);
		L1Teleport.teleport(this, this._ghostSaveLocX, this._ghostSaveLocY, this._ghostSaveMapId,
				this._ghostSaveHeading, true);
	}

	/**
	 * 結束鬼魂模式
	 */
	public void endGhost() {
		this.set_ghostTime(-1);
		this.setGhost(false);
		this.setGhostCanTalk(true);
		this.setReserveGhost(false);
	}

	private int _ghostSaveLocX = 0;
	private int _ghostSaveLocY = 0;
	private short _ghostSaveMapId = 0;
	private int _ghostSaveHeading = 0;

	/**
	 * 若不在地獄時則強制移動至地獄
	 * 
	 * @param isFirst
	 */
	public void beginHell(final boolean isFirst) {
		// 若不在地獄時則強制移動至地獄
		if (this.getMapId() != 666) {
			final int locx = 32701;
			final int locy = 32777;
			final short mapid = 666;
			L1Teleport.teleport(this, locx, locy, mapid, 5, false);
		}

		if (isFirst) {
			if (this.get_PKcount() <= 10) {
				this.setHellTime(300);

			} else {
				this.setHellTime(300 * (this.get_PKcount() - 10) + 300);
			}
			// 552 因為你已經殺了 %0 人所以被打入地獄。 你將在這裡停留 %1 分鐘。
			this.sendPackets(new S_BlueMessage(552, String.valueOf(this.get_PKcount()),
					String.valueOf(this.getHellTime() / 60)));

		} else {
			// 637 你必須在此地停留 %0 秒。
			this.sendPackets(new S_BlueMessage(637, String.valueOf(this.getHellTime())));
		}
	}

	/**
	 * 地獄時間終止
	 */
	public void endHell() {
		// 地獄時間終止 返回然柳村
		final int[] loc = L1TownLocation.getGetBackLoc(L1TownLocation.TOWNID_ORCISH_FOREST);
		L1Teleport.teleport(this, loc[0], loc[1], (short) loc[2], 5, true);

		try {
			this.save();

		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	// @Override
	public void setPoisonEffect(final int effectId) {
		this.sendPackets(new S_Poison(this.getId(), effectId));

		if (!this.isGmInvis() && !this.isGhost() && !this.isInvisble()) {
			this.broadcastPacketAll(new S_Poison(this.getId(), effectId));
		}
	}

	// @Override
	public void healHp(final int pt) {
		super.healHp(pt);

		this.sendPackets(new S_HPUpdate(this));
	}

	// @Override
	public int getKarma() {
		return this._karma.get();
	}

	// @Override
	public void setKarma(final int i) {
		this._karma.set(i);
	}

	public void addKarma(final int i) {
		_karmaLock.lock();
		try {
			this._karma.add(i);
			onChangeKarma();
		} finally {
			_karmaLock.unlock();
		}
	}

	public int getKarmaLevel() {
		return this._karma.getLevel();
	}

	public int getKarmaPercent() {
		return this._karma.getPercent();
	}

	private Timestamp _lastPk;

	/**
	 * 回傳玩家最後 PK 時間。
	 * 
	 * @return _lastPk
	 * 
	 */
	public Timestamp getLastPk() {
		return this._lastPk;
	}

	/**
	 * 設定玩家最後 PK 時間。
	 * 
	 * @param time
	 *             最後 PK 時間（Timestamp 型），解除時傳入 null
	 */
	public void setLastPk(final Timestamp time) {
		this._lastPk = time;
	}

	/**
	 * 將玩家最後 PK 時間設為目前時間。
	 */
	public void setLastPk() {
		this._lastPk = new Timestamp(System.currentTimeMillis());
	}

	/**
	 * 回傳玩家是否為通緝中。
	 * 
	 * @return 若為通緝中則回傳 true
	 */
	public boolean isWanted() {
		if (this._lastPk == null) {
			return false;

			// 距離PK時間超過1小時
		} else if (System.currentTimeMillis() - this._lastPk.getTime() > 1 * 3600 * 1000) {
			this.setLastPk(null);
			return false;
		}
		return true;
	}

	private Timestamp _lastPkForElf;

	public Timestamp getLastPkForElf() {
		return this._lastPkForElf;
	}

	public void setLastPkForElf(final Timestamp time) {
		this._lastPkForElf = time;
	}

	public void setLastPkForElf() {
		this._lastPkForElf = new Timestamp(System.currentTimeMillis());
	}

	public boolean isWantedForElf() {
		if (this._lastPkForElf == null) {
			return false;

		} else if (System.currentTimeMillis() - this._lastPkForElf.getTime() > 24 * 3600 * 1000) {
			this.setLastPkForElf(null);
			return false;
		}
		return true;
	}

	private Timestamp _deleteTime; // 角色刪除前的等待時間

	public Timestamp getDeleteTime() {
		return this._deleteTime;
	}

	public void setDeleteTime(final Timestamp time) {
		this._deleteTime = time;
	}

	/**
	 * 職業魔法等級
	 */
	// @Override
	public int getMagicLevel() {
		return this.getClassFeature().getMagicLevel(this.getLevel());
	}

	/**
	 * Magic Bonus（額外魔法點數）依 系統_智力INT成長 表抓取
	 * <p>
	 * 外控僅針對玩家現階段智力（含裝備），不修改出生智力獎勵。
	 * </p>
	 */
	@Override
	public int getMagicBonus() {
		return com.lineage.server.datatables.BaseStatTable.get().getIntMagicBonus(this.getInt());
	}

	/**
	 * 治癒術額外骰次，依 {@code 角色_智力成長.治癒骰加成} 外控（無程式上限 10）。
	 */
	public int getHealDiceBonus() {
		return com.lineage.server.datatables.BaseStatTable.get().getIntHealDiceBonus(this.getInt());
	}

	private double _weightUP = 1.0D;// 負重提高%

	/**
	 * 負重提高%
	 * 
	 * @return
	 */
	public double get_weightUP() {
		return _weightUP;
	}

	/**
	 * 負重提高%
	 * 
	 * @param i
	 */
	public void add_weightUP(final int i) {
		_weightUP += (i / 100D);
	}

	private int _weightReduction = 0;// 減重

	/**
	 * 減重
	 * 
	 * @return
	 */
	public int getWeightReduction() {
		return this._weightReduction
				+ _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.WEIGHT_REDUCTION);
	}

	/**
	 * 減重
	 * 
	 * @param i
	 */
	public void addWeightReduction(final int i) {
		this._weightReduction += i;
	}

	private int _originalStrWeightReduction = 0; // ● 原始 STR 負重減免

	public int getOriginalStrWeightReduction() {
		return this._originalStrWeightReduction;
	}

	private int _originalConWeightReduction = 0; // ● 原始 CON 負重減免

	public int getOriginalConWeightReduction() {
		return this._originalConWeightReduction;
	}

	private int _hasteItemEquipped = 0;// 裝備有加速能力裝備(裝備數量)

	/**
	 * 裝備有加速能力裝備(裝備數量)
	 * 
	 * @return
	 */
	public int getHasteItemEquipped() {
		return this._hasteItemEquipped;
	}

	/**
	 * 裝備有加速能力裝備(裝備數量)
	 * 
	 * @param i
	 */
	public void addHasteItemEquipped(final int i) {
		this._hasteItemEquipped += i;
	}

	public void removeHasteSkillEffect() {
		if (this.hasSkillEffect(SLOW)) {
			this.removeSkillEffect(SLOW);
		}

		if (this.hasSkillEffect(MASS_SLOW)) {
			this.removeSkillEffect(MASS_SLOW);
		}

		if (this.hasSkillEffect(ENTANGLE)) {
			this.removeSkillEffect(ENTANGLE);
		}

		if (this.hasSkillEffect(HASTE)) {
			this.removeSkillEffect(HASTE);
		}

		if (this.hasSkillEffect(GREATER_HASTE)) {
			this.removeSkillEffect(GREATER_HASTE);
		}

		if (this.hasSkillEffect(STATUS_HASTE)) {
			this.removeSkillEffect(STATUS_HASTE);
		}
	}

	private int _damageReductionByArmor = 0; // 防具增加傷害減免

	// [PVP] PVP 傷害加成（來自裝備）
	private int _pvpDmg = 0;

	/**
	 * 取得 PVP 傷害加成
	 * 
	 * @return PVP 傷害加成數值
	 */
	public int getPvpDmg() {
		return _pvpDmg + _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.PVP_DMG);
	}

	/**
	 * 增加（減少）PVP 傷害加成
	 * 
	 * @param value 數值（可為負數）
	 */
	public void addPvpDmg(final int value) {
		_pvpDmg += value;
	}

	// [PVP] PVP 傷害減免（來自裝備）
	private int _pvpRedux = 0;

	/**
	 * 取得 PVP 傷害減免
	 * 
	 * @return PVP 傷害減免數值
	 */
	public int getPvpRedux() {
		return _pvpRedux + _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.PVP_REDUX);
	}

	/**
	 * 增加（減少）PVP 傷害減免
	 * 
	 * @param value 數值（可為負數）
	 */
	public void addPvpRedux(final int value) {
		_pvpRedux += value;
	}

	// [對 BOSS 增傷] 對 npc.boss 怪物的傷害加成（來自裝備/套裝）
	private int _bossDmg = 0;

	/**
	 * 取得對 BOSS 傷害加成（裝備 + 套裝合併值）
	 *
	 * @return 對 BOSS 傷害加成數值
	 */
	public int getBossDmg() {
		return _bossDmg + _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.DMG_BOSS);
	}

	/**
	 * 增加（減少）對 BOSS 傷害加成（由裝備穿脫時呼叫）
	 *
	 * @param value 數值（可為負數）
	 */
	public void addBossDmg(final int value) {
		_bossDmg += value;
	}

	// [受到 BOSS 減傷] 被 BOSS 攻擊時傷害減免（百分比 0~100，來自裝備/套裝）
	private int _bossRedux = 0;

	/**
	 * 取得受到 BOSS 傷害減免（%，裝備 + 套裝合併值）
	 * 用於計算：實際傷害 = 原傷害 * (100 - getBossRedux()) / 100
	 *
	 * @return 減免百分比 0~100
	 */
	public int getBossRedux() {
		return _bossRedux + _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.BOSS_REDUX);
	}

	/**
	 * 增加（減少）受到 BOSS 傷害減免（由裝備穿脫時呼叫）
	 *
	 * @param value 百分比數值（可為負數）
	 */
	public void addBossRedux(final int value) {
		_bossRedux += value;
	}

	// [無視減免] 無視傷害減免（來自裝備）
	private int _ignoreReduction = 0;

	/**
	 * 取得無視傷害減免
	 * 
	 * @return 無視傷害減免數值
	 */
	public int getIgnoreReduction() {
		return _ignoreReduction
				+ _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.IGNORE_REDUCTION);
	}

	/**
	 * 增加（減少）無視傷害減免
	 * 
	 * @param value 數值（可為負數）
	 */
	public void addIgnoreReduction(final int value) {
		_ignoreReduction += value;
	}

	public int getDamageReductionByArmor() {
		int baseReduction = _damageReductionByArmor;
		baseReduction += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.DMG_REDUX);

		int damageReduction = 0;
		if (baseReduction > 10) {
			damageReduction = 10 + (ThreadLocalRandom.current().nextInt((baseReduction - 10)) + 1);
		} else {
			damageReduction = baseReduction;
		}
		return damageReduction;
	}

	public void addDamageReductionByArmor(final int i) {
		this._damageReductionByArmor += i;
	}

	/**
	 * 致命身軀反射：攻擊者主手武器強化值加總（目前僅 {@link #getWeapon}；非 PC 或空手為 0）。
	 */
	public static int sumMortalBodyReflectWeaponEnchant(final L1Character attacker) {
		if (attacker instanceof L1PcInstance pc) {
			final L1ItemInstance w = pc.getWeapon();
			if (w != null) {
				return Math.max(0, w.getEnchantLevel());
			}
		}
		return 0;
	}

	/**
	 * 致命身軀反射：以 base 為基礎值（已含 MORTAL_BODY_DMG + 攻擊者武器強化），扣減防具傷害減免、PVP 減免、近距離減免，最低為 1。
	 * 不套用任何傷害放大（不經 receiveDamage 內裝備／狀態加乘）。
	 */
	public int calcMortalBodyReflectDamageTaken(final int baseDmg) {
		int d = baseDmg;
		d -= getDamageReductionByArmor();
		d -= getPvpRedux();
		d -= getMeleeRedux();
		return Math.max(1, d);
	}

	/**
	 * 承受致命身軀反射傷害：僅扣 HP／死亡／中斷採集，不觸發 receiveDamage 內放大與其他反饋鏈。
	 */
	public void receiveMortalBodyReflectDamage(final L1Character attacker, final int damage) {
		if (damage < 1) {
			return;
		}
		// The value is already reduced and counterBarrier suppresses a second
		// feedback chain. It still belongs to the per-target FIFO; direct mutation
		// here used to race ordinary DamageCommit work and duplicate death.
		DamageCommit.enqueue(new DamageEvent(attacker, this, damage, 0, false, true));
	}

	private int _hitModifierByArmor = 0; // 防具增加物理命中

	public int getHitModifierByArmor() {
		return this._hitModifierByArmor;
	}

	public void addHitModifierByArmor(final int i) {
		this._hitModifierByArmor += i;
	}

	private int _dmgModifierByArmor = 0; // 防具增加物理傷害

	public int getDmgModifierByArmor() {
		return this._dmgModifierByArmor;
	}

	public void addDmgModifierByArmor(final int i) {
		this._dmgModifierByArmor += i;
	}

	private int _bowHitModifierByArmor = 0; // 防具增加遠距離物理命中

	public int getBowHitModifierByArmor() {
		return this._bowHitModifierByArmor;
	}

	public void addBowHitModifierByArmor(final int i) {
		this._bowHitModifierByArmor += i;
	}

	private int _bowDmgModifierByArmor = 0; // 防具增加遠距離物理傷害

	public int getBowDmgModifierByArmor() {
		return this._bowDmgModifierByArmor;
	}

	public void addBowDmgModifierByArmor(final int i) {
		this._bowDmgModifierByArmor += i;
	}

	private boolean _gresValid; // G-RES 是否有效

	private void setGresValid(final boolean valid) {
		this._gresValid = valid;
	}

	public boolean isGresValid() {
		return this._gresValid;
	}

	private boolean _isFishing = false;// 釣魚狀態

	/**
	 * 釣魚狀態
	 * 
	 * @return
	 */
	public boolean isFishing() {
		return this._isFishing;
	}

	private int _fishX = -1;

	public int get_fishX() {
		return _fishX;
	}

	private int _fishY = -1;

	public int get_fishY() {
		return _fishY;
	}

	/** 當前使用的釣竿道具 ID，用於依釣竿抽漁獲 */
	private int _fishingpoleId = 0;

	public int getFishingPoleId() {
		return _fishingpoleId;
	}

	public void setFishingPoleId(final int itemId) {
		this._fishingpoleId = itemId;
	}

	/**
	 * 釣魚狀態
	 * 
	 * @param flag
	 * @param fishY
	 * @param fishX
	 */
	public void setFishing(final boolean flag, int fishX, int fishY) {
		this._isFishing = flag;
		_fishX = fishX;
		_fishY = fishY;
	}

	/** 挖礦 session；generation 防止 stop→restart 後舊計時器復活。 */
	private volatile boolean _isMining = false;
	private final Object _miningLifecycleLock = new Object();
	private long _miningGeneration;
	private ScheduledFuture<?> _miningFuture;

	public boolean isMining() {
		return _isMining;
	}

	public long beginMiningSession() {
		synchronized (_miningLifecycleLock) {
			cancelMiningFutureLocked();
			_miningGeneration = nextLifecycleGeneration(_miningGeneration);
			_isMining = true;
			return _miningGeneration;
		}
	}

	public boolean installMiningFuture(final long generation, final ScheduledFuture<?> future) {
		if (future == null) {
			return false;
		}
		synchronized (_miningLifecycleLock) {
			if (!_isMining || _miningGeneration != generation) {
				future.cancel(false);
				return false;
			}
			_miningFuture = future;
			return true;
		}
	}

	public boolean runIfMiningSession(final long generation, final Runnable action) {
		if (action == null) {
			return false;
		}
		synchronized (_miningLifecycleLock) {
			if (!_isMining || _miningGeneration != generation) {
				return false;
			}
			action.run();
			return true;
		}
	}

	public boolean stopMiningSession(final long generation) {
		synchronized (_miningLifecycleLock) {
			if (!_isMining || _miningGeneration != generation) {
				return false;
			}
			stopMiningLocked();
			return true;
		}
	}

	public void setMining(final boolean flag) {
		if (flag) {
			beginMiningSession();
			return;
		}
		synchronized (_miningLifecycleLock) {
			stopMiningLocked();
		}
	}

	private void stopMiningLocked() {
		cancelMiningFutureLocked();
		_isMining = false;
		_miningGeneration = nextLifecycleGeneration(_miningGeneration);
	}

	private void cancelMiningFutureLocked() {
		final ScheduledFuture<?> future = _miningFuture;
		_miningFuture = null;
		if (future != null) {
			future.cancel(false);
		}
	}

	private int _cookingId = 0;

	public int getCookingId() {
		return this._cookingId;
	}

	public void setCookingId(final int i) {
		this._cookingId = i;
	}

	private int _dessertId = 0;

	public int getDessertId() {
		return this._dessertId;
	}

	public void setDessertId(final int i) {
		this._dessertId = i;
	}

	/**
	 * 依等級設定命中加成，等級變動時呼叫可重新計算
	 * 
	 * @return
	 */
	public void resetBaseDmgup() {
		int newBaseDmgup = 0;
		int newBaseBowDmgup = 0;
		if (this.isKnight() || this.isDarkelf() || this.isDragonKnight()) { // 騎士、黑暗妖精、龍騎士
			newBaseDmgup = this.getLevel() / 10;
			newBaseBowDmgup = 0;

		} else if (this.isElf()) { // 妖精
			newBaseDmgup = 0;
			newBaseBowDmgup = this.getLevel() / 10;
		}
		this.addDmgup(newBaseDmgup - this._baseDmgup);
		this.addBowDmgup(newBaseBowDmgup - this._baseBowDmgup);
		this._baseDmgup = newBaseDmgup;
		this._baseBowDmgup = newBaseBowDmgup;
	}

	/**
	 * 依等級設定命中加成，等級變動時呼叫可重新計算
	 * 
	 * @return
	 */
	public void resetBaseHitup() {
		int newBaseHitup = 0;
		int newBaseBowHitup = 0;
		if (this.isCrown()) { // 君主
			newBaseHitup = this.getLevel() / 5;
			newBaseBowHitup = this.getLevel() / 5;

		} else if (this.isKnight()) { // 騎士
			newBaseHitup = this.getLevel() / 3;
			newBaseBowHitup = this.getLevel() / 3;

		} else if (this.isElf()) { // 妖精
			newBaseHitup = this.getLevel() / 5;
			newBaseBowHitup = this.getLevel() / 5;

		} else if (this.isDarkelf()) { // 黑暗妖精
			newBaseHitup = this.getLevel() / 3;
			newBaseBowHitup = this.getLevel() / 3;

		} else if (this.isDragonKnight()) { // 龍騎士
			newBaseHitup = this.getLevel() / 3;
			newBaseBowHitup = this.getLevel() / 3;

		} else if (this.isIllusionist()) { // 幻術師
			newBaseHitup = this.getLevel() / 5;
			newBaseBowHitup = this.getLevel() / 5;
		}

		this.addHitup(newBaseHitup - this._baseHitup);
		this.addBowHitup(newBaseBowHitup - this._baseBowHitup);
		this._baseHitup = newBaseHitup;
		this._baseBowHitup = newBaseBowHitup;
	}

	/**
	 * 從角色屬性重新計算並設定 AC，於初始化、升級、降級時呼叫
	 */
	public void resetBaseAc() {
		final int newAc = CalcStat.calcAc(this.getLevel(), this.getOriginalDex());
		this.addAc(newAc - this._baseAc);
		this._baseAc = newAc;
	}

	/**
	 * 從角色屬性重新計算並設定基礎 MR，於初始化、使用技能、升級、降級時呼叫
	 */
	public void resetBaseMr() {
		int newMr = 0;
		if (this.isCrown()) { // 君主
			newMr = 10;

		} else if (this.isElf()) { // 妖精
			newMr = 25;

		} else if (this.isWizard()) { // 法師
			newMr = 15;

		} else if (this.isDarkelf()) { // 黑暗妖精
			newMr = 10;

		} else if (this.isDragonKnight()) { // 龍騎士
			newMr = 18;

		} else if (this.isIllusionist()) { // 幻術師
			newMr = 20;
		}
		newMr += CalcStat.calcStatMr(this.getWis()); // WIS 的 MR 加成
		newMr += this.getLevel() / 2; // 僅加等級的一半
		this.addMr(newMr - this._baseMr);
		this._baseMr = newMr;
	}

	/**
	 * 重新設置等級為目前經驗質所屬
	 */
	public void resetLevel() {
		this.setLevel(ExpTable.getLevelByExp(this._exp));
	}

	/**
	 * 從初始屬性重新計算並設定目前加成，於初始化、屬性重配時呼叫
	 */
	public void resetOriginalHpup() {
		this._originalHpup = L1PcOriginal.resetOriginalHpup(this);
	}

	public void resetOriginalMpup() {
		this._originalMpup = L1PcOriginal.resetOriginalMpup(this);
	}

	public void resetOriginalStrWeightReduction() {
		this._originalStrWeightReduction = L1PcOriginal.resetOriginalStrWeightReduction(this);
	}

	public void resetOriginalDmgup() {
		this._originalDmgup = L1PcOriginal.resetOriginalDmgup(this);
	}

	public void resetOriginalConWeightReduction() {
		this._originalConWeightReduction = L1PcOriginal.resetOriginalConWeightReduction(this);
	}

	public void resetOriginalBowDmgup() {
		this._originalBowDmgup = L1PcOriginal.resetOriginalBowDmgup(this);
	}

	public void resetOriginalHitup() {
		this._originalHitup = L1PcOriginal.resetOriginalHitup(this);
	}

	public void resetOriginalBowHitup() {
		this._originalBowHitup = L1PcOriginal.resetOriginalBowHitup(this);
	}

	public void resetOriginalMr() {
		this._originalMr = L1PcOriginal.resetOriginalMr(this);
		this.addMr(this._originalMr);
	}

	public void resetOriginalMagicHit() {
		this._originalMagicHit = L1PcOriginal.resetOriginalMagicHit(this);
	}

	public void resetOriginalMagicCritical() {
		this._originalMagicCritical = L1PcOriginal.resetOriginalMagicCritical(this);
	}

	public void resetOriginalMagicConsumeReduction() {
		this._originalMagicConsumeReduction = L1PcOriginal.resetOriginalMagicConsumeReduction(this);
	}

	public void resetOriginalMagicDamage() {
		this._originalMagicDamage = L1PcOriginal.resetOriginalMagicDamage(this);
	}

	public void resetOriginalAc() {
		this._originalAc = L1PcOriginal.resetOriginalAc(this);
		// System.out.println("_originalAc:"+_originalAc);
		this.addAc(0 - this._originalAc);
	}

	public void resetOriginalEr() {
		this._originalEr = L1PcOriginal.resetOriginalEr(this);
	}

	public void resetOriginalHpr() {
		this._originalHpr = L1PcOriginal.resetOriginalHpr(this);
	}

	public void resetOriginalMpr() {
		this._originalMpr = L1PcOriginal.resetOriginalMpr(this);
	}

	/**
	 * 全屬性重置
	 */
	public void refresh() {
		_playerActionLock.lock();
		try {
			this.resetLevel();
			this.resetBaseHitup();
			this.resetBaseDmgup();
			this.resetBaseMr();
			this.resetBaseAc();
			this.resetOriginalHpup();
			this.resetOriginalMpup();
			this.resetOriginalDmgup();
			this.resetOriginalBowDmgup();
			this.resetOriginalHitup();
			this.resetOriginalBowHitup();
			this.resetOriginalMr();
			this.resetOriginalMagicHit();
			this.resetOriginalMagicCritical();
			this.resetOriginalMagicConsumeReduction();
			this.resetOriginalMagicDamage();
			this.resetOriginalAc();
			this.resetOriginalEr();
			this.resetOriginalHpr();
			this.resetOriginalMpr();
			this.resetOriginalStrWeightReduction();
			this.resetOriginalConWeightReduction();
			if (getOnlineStatus() == 1) {
				// 統一計算所有任務/收藏類能力（卡冊 + 任務獎勵 + 未來擴充）
				L1PcQuestAbilityCalc.applyTo(this);
				// 永久能力（藥水/天賦/等）
				L1PcPermanentAbilityCalc.applyTo(this);
				// [天賦系統] 狀態天賦能力（當擁有特定 Buff 時給予額外能力）
				applyTalentBuffAbility();
				// 紋樣系統
				com.lineage.server.model.WenyangSystem.applyWenyang(this);
			}
		} finally {
			_playerActionLock.unlock();
		}
	}

	/**
	 * [天賦系統] 套用狀態天賦能力
	 * 
	 * <p>採用「先回滾再重算」的安全策略，避免 Ghost Stats（無限疊加）Bug。</p>
	 * 
	 * <p>呼叫時機：</p>
	 * <ul>
	 *   <li>refresh() - 全屬性重算時</li>
	 *   <li>Buff 變化時（setSkillEffect / removeSkillEffect）</li>
	 * </ul>
	 */
	public void applyTalentBuffAbility() {
		// Ability deltas include max HP/MP, whose authoritative mutation lock is
		// PlayerActionLock. Reuse that reentrant lock instead of introducing a
		// talent->action lock edge that could ABBA with combat/buff paths.
		_playerActionLock.lock();
		try {
			// 1. 回滾舊值（sign = -1）
			if (_cachedTalentBuffAbility != null) {
				L1PcTalentBuffCalc.applyTo(this, _cachedTalentBuffAbility, -1);
			}

			// 2. 計算新值（檢查玩家當前 Buff 與已學天賦的匹配情況）
			final com.lineage.server.model.item.L1ItemAbility newAbility = L1PcTalentBuffCalc.calculate(this);

			// 3. 套用新值（sign = 1）
			if (newAbility != null) {
				L1PcTalentBuffCalc.applyTo(this, newAbility, 1);
			}

			// 4. 快取新值供下次回滾使用
			_cachedTalentBuffAbility = (newAbility != null) ? newAbility
					: new com.lineage.server.model.item.L1ItemAbility();

		} catch (Exception e) {
			org.apache.logging.log4j.LogManager.getLogger(L1PcInstance.class)
					.error("【天賦系統】狀態天賦能力套用失敗: " + this.getName(), e);
		} finally {
			_playerActionLock.unlock();
		}
	}

	// 人物訊息拒絕清單
	private final L1ExcludingList _excludingList = new L1ExcludingList();

	/**
	 * 人物訊息拒絕清單
	 * 
	 * @return
	 */
	public L1ExcludingList getExcludingList() {
		return this._excludingList;
	}

	private volatile int _teleportX = 0;// 傳送目的座標X

	/**
	 * 傳送目的座標X
	 * 
	 * @return
	 */
	public int getTeleportX() {
		return this._teleportX;
	}

	/**
	 * 傳送目的座標X
	 * 
	 * @param i
	 */
	public void setTeleportX(final int i) {
		this._teleportX = i;
	}

	private volatile int _teleportY = 0;// 傳送目的座標Y

	/**
	 * 傳送目的座標Y
	 * 
	 * @return
	 */
	public int getTeleportY() {
		return this._teleportY;
	}

	/**
	 * 傳送目的座標Y
	 * 
	 * @param i
	 */
	public void setTeleportY(final int i) {
		this._teleportY = i;
	}

	private volatile short _teleportMapId = 0;// 傳送目的座標MAP

	/**
	 * 傳送目的座標MAP
	 * 
	 * @return
	 */
	public short getTeleportMapId() {
		return this._teleportMapId;
	}

	/**
	 * 傳送目的座標MAP
	 * 
	 * @param i
	 */
	public void setTeleportMapId(final short i) {
		this._teleportMapId = i;
	}

	private volatile int _teleportHeading = 0;// 傳送後面向

	/**
	 * 傳送後面向
	 * 
	 * @return
	 */
	public int getTeleportHeading() {
		return this._teleportHeading;
	}

	/**
	 * 傳送後面向
	 * 
	 * @param i
	 */
	public void setTeleportHeading(final int i) {
		this._teleportHeading = i;
	}

	private int _tempCharGfxAtDead;// 死亡時外型代號

	/**
	 * 死亡時外型代號
	 * 
	 * @return
	 */
	public int getTempCharGfxAtDead() {
		return this._tempCharGfxAtDead;
	}

	/**
	 * 死亡時外型代號
	 * 
	 * @param i
	 */
	private void setTempCharGfxAtDead(final int i) {
		this._tempCharGfxAtDead = i;
	}

	private boolean _isCanWhisper = true;// 全秘密語(收聽)

	/**
	 * 全秘密語(收聽)
	 * 
	 * @return flag true:接收 false:拒絕
	 */
	public boolean isCanWhisper() {
		return this._isCanWhisper;
	}

	/**
	 * 全秘密語(收聽)
	 * 
	 * @param flag
	 *             flag true:接收 false:拒絕
	 */
	public void setCanWhisper(final boolean flag) {
		this._isCanWhisper = flag;
	}

	private boolean _isShowTradeChat = true;// 買賣頻道(收聽)

	/**
	 * 買賣頻道(收聽)
	 * 
	 * @return flag true:接收 false:拒絕
	 */
	public boolean isShowTradeChat() {
		return this._isShowTradeChat;
	}

	/**
	 * 買賣頻道(收聽)
	 * 
	 * @param flag
	 *             true:接收 false:拒絕
	 */
	public void setShowTradeChat(final boolean flag) {
		this._isShowTradeChat = flag;
	}

	private boolean _isShowWorldChat = true;// 全體聊天(收聽)

	/**
	 * 全體聊天(收聽)
	 * 
	 * @return flag true:接收 false:拒絕
	 */
	public boolean isShowWorldChat() {
		return this._isShowWorldChat;
	}

	/**
	 * 全體聊天(收聽)
	 * 
	 * @param flag
	 *             flag true:接收 false:拒絕
	 */
	public void setShowWorldChat(final boolean flag) {
		this._isShowWorldChat = flag;
	}

	private int _fightId;// 決鬥對象OBJID

	/**
	 * 決鬥對象OBJID
	 * 
	 * @return
	 */
	public int getFightId() {
		return this._fightId;
	}

	/**
	 * 決鬥對象OBJID
	 * 
	 * @param i
	 */
	public void setFightId(final int i) {
		this._fightId = i;
	}

	private byte _chatCount = 0;// 對話檢查次數

	private long _oldChatTimeInMillis = 0L;// 對話檢查毫秒差

	/**
	 * 對話檢查(洗畫面)
	 */
	public void checkChatInterval() {
		final long nowChatTimeInMillis = System.currentTimeMillis();
		if (this._chatCount == 0) {
			this._chatCount++;
			this._oldChatTimeInMillis = nowChatTimeInMillis;
			return;
		}

		final long chatInterval = nowChatTimeInMillis - this._oldChatTimeInMillis;
		// 時間差異2秒以上
		if (chatInterval > 2000) {
			this._chatCount = 0;
			this._oldChatTimeInMillis = 0;

		} else {
			if (this._chatCount >= 3) {
				this.setSkillEffect(STATUS_CHAT_PROHIBITED, 120 * 1000);
				this.sendPackets(new S_PacketBox(S_PacketBox.ICON_CHATBAN, 120));
				// \f3因洗畫面的關係，2分鐘之內無法聊天。
				this.sendPackets(new S_ServerMessage(153));
				this._chatCount = 0;
				this._oldChatTimeInMillis = 0;
			}
			this._chatCount++;
		}
	}

	/** One immutable invitation snapshot shared by two different client lanes. */
	private final AtomicReference<CallClanDestination> _callClanDestination =
			new AtomicReference<>();
	private final AtomicReference<CallClanInvitation> _incomingCallClanInvitation =
			new AtomicReference<>();

	public record CallClanDestination(int targetId, int heading,
			int x, int y, short mapId, int showId) {
	}

	public record CallClanInvitation(int callerId, CallClanDestination destination) {
	}

	public boolean tryPublishIncomingCallClanInvitation(final int callerId,
			final CallClanDestination destination) {
		if (callerId <= 0 || destination == null) {
			return false;
		}
		final CallClanInvitation replacement =
				new CallClanInvitation(callerId, destination);
		while (true) {
			final CallClanInvitation current = _incomingCallClanInvitation.get();
			if (current != null) {
				// The wire reply contains only Y/N, not an invitation token.  Keep
				// first-prompt ownership so an answer to an older dialog can never
				// accept a different caller's newer invitation.  Re-sending the same
				// prompt is harmless: only the first reply can consume it.
				return current.callerId() == callerId
						&& current.destination() == destination;
			}
			if (_incomingCallClanInvitation.compareAndSet(null, replacement)) {
				return true;
			}
		}
	}

	/** Compatibility entry; production callers should observe the boolean result. */
	public void publishIncomingCallClanInvitation(final int callerId,
			final CallClanDestination destination) {
		tryPublishIncomingCallClanInvitation(callerId, destination);
	}

	public CallClanInvitation consumeIncomingCallClanInvitation(final int callerId) {
		final CallClanInvitation invitation = _incomingCallClanInvitation.getAndSet(null);
		return invitation != null && invitation.callerId() == callerId
				? invitation : null;
	}

	public void clearIncomingCallClanInvitation() {
		_incomingCallClanInvitation.set(null);
	}

	/**
	 * Publishes target, direction and destination as one release operation. The
	 * same still-pending target deliberately retains the original destination and
	 * object token because the client Y/N reply does not carry a generation id.
	 */
	public void publishCallClanDestination(final int targetId) {
		if (targetId <= 0) {
			clearCallClanDestination();
			return;
		}
		while (true) {
			final CallClanDestination current = _callClanDestination.get();
			if (current != null && current.targetId() == targetId) {
				return;
			}
			final CallClanDestination replacement =
					new CallClanDestination(targetId, Math.floorMod(getHeading(), 8),
							getX(), getY(), getMapId(), get_showId());
			if (_callClanDestination.compareAndSet(current, replacement)) {
				return;
			}
		}
	}

	public void clearCallClanDestination() {
		_callClanDestination.set(null);
	}

	public CallClanDestination getCallClanDestination(final int targetId) {
		final CallClanDestination destination = _callClanDestination.get();
		return destination != null && destination.targetId() == targetId
				? destination : null;
	}

	/** Consumes only the exact snapshot that was validated by the invitee. */
	public boolean claimCallClanDestination(final CallClanDestination destination) {
		return destination != null && _callClanDestination.compareAndSet(destination, null);
	}

	/**
	 * 傳回呼喚盟友(對象OBJID)
	 * 
	 * @return
	 */
	public int getCallClanId() {
		final CallClanDestination destination = _callClanDestination.get();
		return destination != null ? destination.targetId() : 0;
	}

	/**
	 * 設置呼喚盟友(對象OBJID)
	 * 
	 * @param i
	 */
	public void setCallClanId(final int i) {
		if (i == 0) {
			clearCallClanDestination();
		} else {
			publishCallClanDestination(i);
		}
	}

	/**
	 * 設置呼喚盟友(自己的面向)
	 * 
	 * @return
	 */
	public int getCallClanHeading() {
		final CallClanDestination destination = _callClanDestination.get();
		return destination != null ? destination.heading() : 0;
	}

	/**
	 * 傳回呼喚盟友(自己的面向)
	 * 
	 * @return
	 */
	public void setCallClanHeading(final int i) {
		final CallClanDestination current = _callClanDestination.get();
		if (current != null) {
			_callClanDestination.compareAndSet(current,
					new CallClanDestination(current.targetId(), Math.floorMod(i, 8),
							current.x(), current.y(), current.mapId(), current.showId()));
		}
	}

	private boolean _isInCharReset = false;// 執行人物重設狀態

	/**
	 * 傳回執行人物重設狀態
	 * 
	 * @return
	 */
	public boolean isInCharReset() {
		return this._isInCharReset;
	}

	/**
	 * 設置執行人物重設狀態
	 * 
	 * @param flag
	 */
	public void setInCharReset(final boolean flag) {
		this._isInCharReset = flag;
	}

	private int _tempLevel = 1;// 人物重置等級暫存(最低)

	/**
	 * 人物重置等級暫存(最低)
	 * 
	 * @return
	 */
	public int getTempLevel() {
		return this._tempLevel;
	}

	/**
	 * 人物重置等級暫存(最低)
	 * 
	 * @param i
	 */
	public void setTempLevel(final int i) {
		this._tempLevel = i;
	}

	private int _tempMaxLevel = 1;// 人物重置等級暫存(最高)

	/**
	 * 人物重置等級暫存(最高)
	 * 
	 * @return
	 */
	public int getTempMaxLevel() {
		return this._tempMaxLevel;
	}

	/**
	 * 人物重置等級暫存(最高)
	 * 
	 * @param i
	 */
	public void setTempMaxLevel(final int i) {
		this._tempMaxLevel = i;
	}

	private boolean _isSummonMonster = false;// 是否展開召喚控制選單

	/**
	 * 設置是否展開召喚控制選單
	 * 
	 * @param SummonMonster
	 */
	public void setSummonMonster(final boolean SummonMonster) {
		this._isSummonMonster = SummonMonster;
	}

	/**
	 * 是否展開召喚控制選單
	 * 
	 * @return
	 */
	public boolean isSummonMonster() {
		return this._isSummonMonster;
	}

	private boolean _isShapeChange = false;// 是否展開變身控制選單

	/**
	 * 設置是否展開變身控制選單
	 * 
	 * @param isShapeChange
	 */
	public void setShapeChange(final boolean isShapeChange) {
		this._isShapeChange = isShapeChange;
	}

	/**
	 * 是否展開變身控制選單
	 * 
	 * @return
	 */
	public boolean isShapeChange() {
		return this._isShapeChange;
	}

	private String _text;// 暫存文字串
	private final AtomicReference<String> _trueTargetText = new AtomicReference<>();

	public void publishTrueTargetText(final String text) {
		_trueTargetText.set(text == null ? "" : text);
	}

	public String consumeTrueTargetText() {
		final String text = _trueTargetText.getAndSet(null);
		return text == null ? "" : text;
	}

	public void clearTrueTargetText() {
		_trueTargetText.set(null);
	}

	/**
	 * 設置暫存文字串(收件者)
	 * 
	 * @param text
	 */
	public void setText(final String text) {
		this._text = text;
	}

	/**
	 * 傳回暫存文字串(收件者)
	 * 
	 * @return
	 */
	public String getText() {
		return this._text;
	}

	private byte[] _textByte = null;// 暫存byte[]陣列

	/**
	 * 設定暫存byte[]陣列
	 * 
	 * @param textByte
	 */
	public void setTextByte(final byte[] textByte) {
		this._textByte = textByte;
	}

	/**
	 * 傳回暫存byte[]陣列
	 * 
	 * @return
	 */
	public byte[] getTextByte() {
		return this._textByte;
	}

	private L1PcOther _other;// 額外紀錄資料

	/**
	 * 額外紀錄資料
	 * 
	 * @param other
	 */
	public void set_other(final L1PcOther other) {
		this._other = other;
	}

	/**
	 * 額外紀錄資料
	 * 
	 * @return
	 */
	public L1PcOther get_other() {
		return this._other;
	}

	private L1PcOtherList _otherList;// 額外清單紀錄資料

	/**
	 * 額外清單紀錄資料
	 * 
	 * @param other
	 */
	public void set_otherList(final L1PcOtherList other) {
		_otherList = other;
	}

	/**
	 * 額外清單紀錄資料
	 * 
	 * @return
	 */
	public L1PcOtherList get_otherList() {
		return _otherList;
	}

	private int _oleLocX;// 移動前座標暫存X

	/**
	 * 移動前座標暫存X
	 * 
	 * @param oleLocx
	 */
	public void setOleLocX(final int oleLocx) {
		this._oleLocX = oleLocx;
	}

	/**
	 * 移動前座標暫存X
	 * 
	 * @return
	 */
	public int getOleLocX() {
		return this._oleLocX;
	}

	private int _oleLocY;// 移動前座標暫存Y

	/**
	 * 移動前座標暫存Y
	 * 
	 * @param oleLocy
	 */
	public void setOleLocY(final int oleLocy) {
		this._oleLocY = oleLocy;
	}

	/**
	 * 移動前座標暫存Y
	 * 
	 * @return
	 */
	public int getOleLocY() {
		return this._oleLocY;
	}

	private volatile L1Character _target = null;

	/**
	 * 設置目前攻擊對象
	 * 
	 * @param target
	 */
	public void setNowTarget(final L1Character target) {
		this._target = target;
	}

	/**
	 * 傳回目前攻擊對象
	 */
	public L1Character getNowTarget() {
		return this._target;
	}

	private int _dmgDown = 0;

	/**
	 * 副助道具傷害減免
	 * 
	 * @param dmgDown
	 */
	public void set_dmgDown(int dmgDown) {
		_dmgDown = dmgDown;
	}

	/**
	 * 副助道具傷害減免
	 * 
	 * @return
	 */
	public int get_dmgDown() {
		return _dmgDown;
	}

	/**
	 * 保存寵物目前模式
	 * 
	 * @param pc
	 */
	public void setPetModel() {
		try {
			// 寵物的跟隨移動
			for (final L1NpcInstance petNpc : getPetList().values()) {
				if (petNpc != null) {
					if (petNpc instanceof L1SummonInstance summon) { // 召喚獸的跟隨移動
						summon.set_tempModel();

					} else if (petNpc instanceof L1PetInstance pet) { // 寵物的跟隨移動
						pet.set_tempModel();
					}
				}
			}

		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * 恢復寵物目前模式
	 * 
	 * @param pc
	 */
	public void getPetModel() {
		try {
			// 寵物的跟隨移動
			for (final L1NpcInstance petNpc : getPetList().values()) {
				if (petNpc != null) {
					if (petNpc instanceof L1SummonInstance summon) { // 召喚獸的跟隨移動
						summon.get_tempModel();

					} else if (petNpc instanceof L1PetInstance pet) { // 寵物的跟隨移動
						pet.get_tempModel();
					}
				}
			}

		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	private L1DeInstance _outChat = null;

	/**
	 * 設置對話輸出
	 * 
	 * @param b
	 */
	public void set_outChat(L1DeInstance b) {
		_outChat = b;
	}

	/**
	 * 對話輸出模式
	 * 
	 * @return
	 */
	public L1DeInstance get_outChat() {
		return _outChat;
	}

	private long _h_time;// 生存吶喊時間

	/**
	 * 生存吶喊時間
	 * 
	 * @return
	 */
	public long get_h_time() {
		return _h_time;
	}

	/**
	 * 生存吶喊時間
	 * 
	 * @param h_time
	 */
	public void set_h_time(long time) {
		_h_time = time;
	}

	private boolean _mazu = false;// 媽祖祝福

	/**
	 * 媽祖祝福
	 * 
	 * @param b
	 */
	public void set_mazu(boolean b) {
		_mazu = b;
	}

	/**
	 * 媽祖祝福
	 * 
	 * @return
	 */
	public boolean is_mazu() {
		return _mazu;
	}

	private long _mazu_time = 0;// 媽祖祝福時間

	/**
	 * 媽祖祝福時間
	 * 
	 * @return
	 */
	public long get_mazu_time() {
		return _mazu_time;
	}

	/**
	 * 媽祖祝福時間
	 * 
	 * @param time
	 */
	public void set_mazu_time(long time) {
		_mazu_time = time;
	}

	private int _int1;// 機率增加攻擊力
	private int _int2;// 機率(1/100)

	/**
	 * 機率增加攻擊力
	 * 
	 * @param int1
	 * @param int2
	 */
	public void set_dmgAdd(int int1, int int2) {
		_int1 += int1;
		_int2 += int2;
	}

	/**
	 * 傳回機率增加的攻擊力
	 * 
	 * @return
	 */
	public int dmgAdd() {
		if (_int2 == 0) {
			return 0;
		}
		if ((ThreadLocalRandom.current().nextInt(100) + 1) <= _int2) {
			if (!getDolls().isEmpty()) {
				for (L1DollInstance doll : getDolls().values()) {
					doll.show_action(1);
				}
			}
			return _int1;
		}
		return 0;
	}

	private int _evasion;// 迴避機率(1/1000)

	/**
	 * 迴避機率
	 * 
	 * @param int1
	 */
	public void set_evasion(int int1) {
		_evasion += int1;
	}

	/**
	 * 傳回迴避機率
	 * 
	 * @return
	 */
	public int get_evasion() {
		return _evasion;
	}

	private double _expadd = 0.0D;// 經驗值增加

	/**
	 * 經驗值增加
	 * 
	 * @param int1
	 */
	public void set_expadd(int int1) {
		_expadd += (int1 / 100D);
	}

	/**
	 * 經驗值增加
	 * 
	 * @return
	 */
	public double getExpAdd() {
		return _expadd;
	}

	private boolean _isFoeSlayer = false;

	/**
	 * 是否使用屠宰者
	 * 
	 * @return
	 */
	public boolean isFoeSlayer() {
		return _isFoeSlayer;
	}

	/**
	 * 是否使用屠宰者
	 */
	public void isFoeSlayer(boolean isFoeSlayer) {
		_isFoeSlayer = isFoeSlayer;
	}

	private int _weaknss = 0;
	private long _weaknss_t = 0;// 時間

	/**
	 * 弱點曝光時間
	 * 
	 * @return
	 */
	public long get_weaknss_t() {
		return _weaknss_t;
	}

	/**
	 * 弱點曝光階段
	 * 
	 * @return
	 */
	public int get_weaknss() {
		return _weaknss;
	}

	/**
	 * 弱點曝光階段
	 * 
	 * @param lv
	 */
	public void set_weaknss(int lv, long t) {
		_weaknss = lv;
		_weaknss_t = t;
	}

	/** 三重矢剩餘命中數（用於套用 TRIPLE_ARROW_DMG，0=非三重矢攻擊中） */
	private int _tripleArrowHitsRemaining = 0;

	public int getTripleArrowHitsRemaining() {
		return _tripleArrowHitsRemaining;
	}

	public void setTripleArrowHitsRemaining(final int n) {
		_tripleArrowHitsRemaining = Math.max(0, n);
	}

	private int _actionId = -1;// 角色表情動作代號

	/**
	 * 角色表情動作代號
	 * 
	 * @param actionId
	 */
	public void set_actionId(int actionId) {
		_actionId = actionId;
	}

	/**
	 * 角色表情動作代號
	 * 
	 * @return
	 */
	public int get_actionId() {
		return _actionId;
	}

	private Chapter01R _hardin = null;// 哈汀副本線程

	/**
	 * 哈汀副本線程
	 * 
	 * @param hardin
	 */
	public void set_hardinR(Chapter01R hardin) {
		_hardin = hardin;
	}

	/**
	 * 哈汀副本線程
	 * 
	 * @return
	 */
	public Chapter01R get_hardinR() {
		return _hardin;
	}

	private final AtomicInteger _unfreezingTime = new AtomicInteger();// 解除人物卡點

	/** 角色錯位 (locerr2) 倒數秒數，0 表示未倒數（流程同角色卡點） */
	private final AtomicInteger _locerr2Countdown = new AtomicInteger();

	public void set_unfreezingTime(int i) {
		_unfreezingTime.set(Math.max(0, i));
	}

	public int get_unfreezingTime() {
		return _unfreezingTime.get();
	}

	public int decrementUnfreezingTime() {
		return _unfreezingTime.updateAndGet(value -> value > 0 ? value - 1 : 0);
	}

	/** 角色錯位是否倒數中 */
	public boolean isLocerr2CountingDown() {
		return _locerr2Countdown.get() > 0;
	}

	/** 角色錯位倒數秒數（Locerr2Timer 每秒遞減） */
	public int getLocerr2Countdown() {
		return _locerr2Countdown.get();
	}

	public void setLocerr2Countdown(int sec) {
		_locerr2Countdown.set(Math.max(0, sec));
	}

	public int decrementLocerr2Countdown() {
		return _locerr2Countdown.updateAndGet(value -> value > 0 ? value - 1 : 0);
	}

	private int _dice_hp;
	private int _sucking_hp;

	/**
	 * 增加機率吸血
	 * 
	 * @param dice_hp
	 * @param sucking_hp
	 */
	public void add_dice_hp(int dice_hp, int sucking_hp) {
		_dice_hp += dice_hp;
		_sucking_hp += sucking_hp;
	}

	public int dice_hp() {
		return _dice_hp;
	}

	public int sucking_hp() {
		return _sucking_hp;
	}

	private int _dice_mp;
	private int _sucking_mp;

	/**
	 * 增加機率吸魔
	 * 
	 * @param dice_mp
	 * @param sucking_mp
	 */
	public void add_dice_mp(int dice_mp, int sucking_mp) {
		_dice_mp += dice_mp;
		_sucking_mp += sucking_mp;
	}

	public int dice_mp() {
		return _dice_mp;
	}

	public int sucking_mp() {
		return _sucking_mp;
	}

	private int _double_dmg;

	/**
	 * 增加機率雙倍打擊
	 * 
	 * @param double_dmg
	 */
	public void add_double_dmg(int double_dmg) {
		_double_dmg += double_dmg;
	}

	public int double_dmg() {
		return _double_dmg;
	}

	private int _lift;

	/**
	 * 增加機率防禦解除
	 * 
	 * @param lift
	 */
	public void add_lift(int lift) {
		_lift += lift;
	}

	public int lift() {
		return _lift;
	}

	private int _magic_modifier_dmg = 0;// 套裝增加魔法傷害

	public void add_magic_modifier_dmg(int add) {
		_magic_modifier_dmg += add;
	}

	public int get_magic_modifier_dmg() {
		return _magic_modifier_dmg
				+ _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.MAGIC_DMG_MODIFIER);
	}

	private int _magic_reduction_dmg = 0;// 套裝減免魔法傷害

	public void add_magic_reduction_dmg(int add) {
		_magic_reduction_dmg += add;
	}

	// 藥水恢復量增加
	private int _potionRecovery = 0;

	/**
	 * 取得藥水恢復量增加
	 * 
	 * @return 藥水恢復量增加數值
	 */
	public int getPotionRecovery() {
		return _potionRecovery
				+ _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.POTION_RECOVERY);
	}

	/**
	 * 增加藥水恢復量
	 * 
	 * @param i 藥水恢復量增加數值
	 */
	public void addPotionRecovery(int i) {
		_potionRecovery += i;
	}

	// 毒性抵抗
	private int _poisonResist = 0;

	/**
	 * 取得毒性抵抗
	 * 
	 * @return 毒性抵抗數值 (大於0即有效)
	 */
	public int getPoisonResist() {
		return _poisonResist + _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.POISON_RESIST);
	}

	/**
	 * 增加毒性抵抗
	 * 
	 * @param i 毒性抵抗數值
	 */
	public void addPoisonResist(int i) {
		_poisonResist += i;
	}

	// 經驗獲取率
	private int _expRate = 0; // 裝備提供的經驗加成(%)

	/**
	 * 取得經驗獲取率
	 * 
	 * @return 經驗獲取率數值
	 */
	public int getExpRate() {
		return _expRate + _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.EXP_RATE);
	}

	/**
	 * 增加經驗獲取率
	 * 
	 * @param i 經驗獲取率數值
	 */
	public void addExpRate(int i) {
		_expRate += i;
	}

	// 魔法命中率
	private int _magicHit = 0; // 裝備提供的魔法命中

	/**
	 * 取得魔法命中率
	 * 
	 * @return 魔法命中率數值
	 */
	public int getMagicHit() {
		return _magicHit + _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.MAGIC_HIT);
	}

	/**
	 * 增加魔法命中率
	 * 
	 * @param i 魔法命中率數值
	 */
	public void addMagicHit(int i) {
		_magicHit += i;
	}

	// 貫穿屬性
	private int _pierce = 0; // 裝備提供的貫穿屬性

	/**
	 * 取得貫穿屬性（含套裝加成）
	 * 
	 * @return 貫穿屬性數值（大於 0 表示可以無視硬皮怪減傷）
	 */
	public int getPierce() {
		return _pierce + _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.PIERCE);
	}

	/**
	 * 增加貫穿屬性
	 * 
	 * @param i 貫穿屬性數值
	 */
	public void addPierce(int i) {
		_pierce += i;
	}

	/**
	 * 取得暈眩成功率（含套裝加成）
	 * 
	 * @return 暈眩成功率數值
	 */
	@Override
	public int getStunSuccess() {
		int stunSuccess = super.getStunSuccess();
		stunSuccess += _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.STUN_SUCCESS);
		return stunSuccess;
	}

	public int get_magic_reduction_dmg() {
		return _magic_reduction_dmg;
	}

	private boolean _rname = false;// 重設名稱

	/**
	 * 重設名稱
	 * 
	 * @param b
	 */
	public void rename(boolean b) {
		_rname = b;
	}

	/**
	 * 重設名稱
	 * 
	 * @return
	 */
	public boolean is_rname() {
		return _rname;
	}

	private boolean _retitle = false;// 重設封號

	/**
	 * 重設封號
	 * 
	 * @return
	 */
	public boolean is_retitle() {
		return _retitle;
	}

	/**
	 * 重設封號
	 * 
	 * @param b
	 */
	public void retitle(boolean b) {
		_retitle = b;
	}

	private static final int PASSWORD_CHANGE_IDLE = 0;
	private static final int PASSWORD_CHANGE_CURRENT = 1;
	private static final int PASSWORD_CHANGE_NEW = 2;
	private static final int PASSWORD_CHANGE_CONFIRM = 3;
	private static final int PASSWORD_CHANGE_PERSISTING = 4;

	/**
	 * Password prompts used to split their phase ({@code _repass}) and candidate
	 * ({@code _text}) across unrelated mutable fields. An item/letter packet or a
	 * delayed DB completion could therefore overwrite/clear another prompt. Keep
	 * the whole workflow in one immutable CAS-published state instead.
	 */
	private static final class PasswordChangeState {
		private final long generation;
		private final int stage;
		private final String candidate;

		private PasswordChangeState(final long generation, final int stage,
				final String candidate) {
			this.generation = generation;
			this.stage = stage;
			this.candidate = candidate;
		}
	}

	/** Exact claim captured by the one persistence owner. */
	public static final class PasswordChangeClaim {
		private final PasswordChangeState claimedState;

		private PasswordChangeClaim(final PasswordChangeState claimedState) {
			this.claimedState = claimedState;
		}

		public String password() {
			return claimedState.candidate;
		}
	}

	private final AtomicLong _passwordChangeGeneration = new AtomicLong();
	private final AtomicReference<PasswordChangeState> _passwordChangeState =
			new AtomicReference<>(new PasswordChangeState(0L, PASSWORD_CHANGE_IDLE, null));

	/**
	 * 重設密碼
	 * 
	 * @return
	 */
	public int is_repass() {
		return _passwordChangeState.get().stage;
	}

	/**
	 * 重設密碼
	 * 
	 * @param b
	 */
	public void repass(int b) {
		if (b == PASSWORD_CHANGE_IDLE) {
			for (;;) {
				final PasswordChangeState current = _passwordChangeState.get();
				if (current.stage == PASSWORD_CHANGE_IDLE
						|| _passwordChangeState.compareAndSet(current,
								new PasswordChangeState(current.generation,
										PASSWORD_CHANGE_IDLE, null))) {
					return;
				}
			}
		}
		if (b == PASSWORD_CHANGE_CURRENT) {
			tryBeginPasswordChange();
		}
	}

	/**
	 * Starts one workflow only while idle.
	 *
	 * @return non-zero generation on success; zero if another workflow owns it
	 */
	public long tryBeginPasswordChange() {
		for (;;) {
			final PasswordChangeState current = _passwordChangeState.get();
			if (current.stage != PASSWORD_CHANGE_IDLE) {
				return 0L;
			}
			long generation = _passwordChangeGeneration.incrementAndGet();
			if (generation == 0L) {
				generation = _passwordChangeGeneration.incrementAndGet();
			}
			final PasswordChangeState next = new PasswordChangeState(generation,
					PASSWORD_CHANGE_CURRENT, null);
			if (_passwordChangeState.compareAndSet(current, next)) {
				return generation;
			}
		}
	}

	/** Cancels only the generation whose item admission failed. */
	public boolean cancelPasswordChange(final long generation) {
		for (;;) {
			final PasswordChangeState current = _passwordChangeState.get();
			if (current.generation != generation || current.stage == PASSWORD_CHANGE_IDLE) {
				return false;
			}
			if (_passwordChangeState.compareAndSet(current,
					new PasswordChangeState(generation, PASSWORD_CHANGE_IDLE, null))) {
				return true;
			}
		}
	}

	public boolean acceptCurrentPasswordForChange() {
		for (;;) {
			final PasswordChangeState current = _passwordChangeState.get();
			if (current.stage != PASSWORD_CHANGE_CURRENT) {
				return false;
			}
			if (_passwordChangeState.compareAndSet(current,
					new PasswordChangeState(current.generation, PASSWORD_CHANGE_NEW, null))) {
				return true;
			}
		}
	}

	public boolean stagePasswordChangeCandidate(final String candidate) {
		if (candidate == null) {
			return false;
		}
		for (;;) {
			final PasswordChangeState current = _passwordChangeState.get();
			if (current.stage != PASSWORD_CHANGE_NEW) {
				return false;
			}
			if (_passwordChangeState.compareAndSet(current,
					new PasswordChangeState(current.generation,
							PASSWORD_CHANGE_CONFIRM, candidate))) {
				return true;
			}
		}
	}

	/** Claims a matching confirmation exactly once. */
	public PasswordChangeClaim claimPasswordChange(final String confirmation) {
		for (;;) {
			final PasswordChangeState current = _passwordChangeState.get();
			if (current.stage != PASSWORD_CHANGE_CONFIRM || current.candidate == null
					|| !current.candidate.equals(confirmation)) {
				return null;
			}
			final PasswordChangeState claimed = new PasswordChangeState(current.generation,
					PASSWORD_CHANGE_PERSISTING, current.candidate);
			if (_passwordChangeState.compareAndSet(current, claimed)) {
				return new PasswordChangeClaim(claimed);
			}
		}
	}

	/** Clears only the exact state whose database update actually committed. */
	public boolean completePasswordChange(final PasswordChangeClaim claim) {
		if (claim == null) {
			return false;
		}
		final PasswordChangeState claimed = claim.claimedState;
		return _passwordChangeState.compareAndSet(claimed,
				new PasswordChangeState(claimed.generation, PASSWORD_CHANGE_IDLE, null));
	}

	/** Returns an uncommitted exact claim to confirmation so the player can retry. */
	public boolean retryPasswordChange(final PasswordChangeClaim claim) {
		if (claim == null) {
			return false;
		}
		final PasswordChangeState claimed = claim.claimedState;
		return _passwordChangeState.compareAndSet(claimed,
				new PasswordChangeState(claimed.generation,
						PASSWORD_CHANGE_CONFIRM, claimed.candidate));
	}

	// 交易物品暫存
	private ArrayList<L1TradeItem> _trade_items = new ArrayList<L1TradeItem>();

	/**
	 * 加入交易物品暫存
	 * 
	 * @param info
	 */
	public void add_trade_item(L1TradeItem info) {
		if (_trade_items.size() == 16) {
			return;
		}
		_trade_items.add(info);
	}

	/**
	 * 交易物品暫存
	 * 
	 * @return
	 */
	public ArrayList<L1TradeItem> get_trade_items() {
		return _trade_items;
	}

	/**
	 * 清空交易物品暫存
	 */
	public void get_trade_clear() {
		_tradeID = 0;
		_trade_items.clear();
	}

	private int _mode_id = 0;// 記錄選取位置

	/**
	 * 記錄選取位置
	 * 
	 * @param mode
	 */
	public void set_mode_id(int mode) {
		_mode_id = mode;
	}

	/**
	 * 記錄選取位置
	 * 
	 * @return
	 */
	public int get_mode_id() {
		return _mode_id;
	}

	private boolean _check_item = false;

	public void set_check_item(boolean b) {
		_check_item = b;
	}

	public boolean get_check_item() {
		return _check_item;
	}

	private long _global_time = 0;

	public long get_global_time() {
		return _global_time;
	}

	public void set_global_time(final long global_time) {
		_global_time = global_time;
	}

	// DOLL 指定時間恢復／給予物品：單一不可變快照，避免跨欄位設定撕裂。
	private final DollPeriodicState _dollPeriodicState = new DollPeriodicState();

	public final void updateDollPeriodicEffects(
			final int hprAmount,
			final int hprInterval,
			final int mprAmount,
			final int mprInterval,
			final int itemId,
			final int itemCount,
			final int itemInterval,
			final boolean installing) {
		PlayerActionLock.run(this, () -> _dollPeriodicState.updateFromAbility(
				hprAmount, hprInterval,
				mprAmount, mprInterval,
				itemId, itemCount, itemInterval,
				installing));
	}

	public final DollPeriodicState.Snapshot getDollPeriodicSnapshot() {
		return _dollPeriodicState.snapshot();
	}

	public final DollPeriodicState.Award advanceDollHprTick() {
		return _dollPeriodicState.advanceHpr();
	}

	public final DollPeriodicState.Award advanceDollMprTick() {
		return _dollPeriodicState.advanceMpr();
	}

	public final DollPeriodicState.Award advanceDollItemTick() {
		return _dollPeriodicState.advanceItem();
	}

	public final boolean commitDollPeriodicAward(
			final DollPeriodicState.Award award, final Runnable action) {
		/* Global order: player action -> (inventory, when needed) -> doll commit. */
		return PlayerActionLock.call(this, () -> _dollPeriodicState.commit(award, action));
	}

	public final boolean commitDollPeriodicItemAward(
			final DollPeriodicState.Award award, final BooleanSupplier delivery) {
		/* Global order: player action -> inventory -> doll commit. */
		return PlayerActionLock.call(this,
				() -> _dollPeriodicState.commitItem(award, delivery));
	}

	/* Legacy accessors remain source-compatible, but now operate on snapshots. */
	public int get_doll_hpr() {
		return _dollPeriodicState.snapshot().hpr().amount();
	}

	public void set_doll_hpr(final int hpr) {
		PlayerActionLock.run(this, () -> {
			final DollPeriodicState.Recovery current = _dollPeriodicState.snapshot().hpr();
			_dollPeriodicState.replaceHpr(hpr, current.intervalSeconds());
		});
	}

	public int get_doll_hpr_time() {
		return _dollPeriodicState.snapshot().hpr().remainingSeconds();
	}

	public void set_doll_hpr_time(final int time) {
		PlayerActionLock.run(this,
				() -> _dollPeriodicState.setRemaining(DollPeriodicState.AwardType.HPR, time));
	}

	public int get_doll_hpr_time_src() {
		return _dollPeriodicState.snapshot().hpr().intervalSeconds();
	}

	public void set_doll_hpr_time_src(final int time) {
		PlayerActionLock.run(this, () -> {
			final DollPeriodicState.Recovery current = _dollPeriodicState.snapshot().hpr();
			_dollPeriodicState.replaceHpr(current.amount(), time);
		});
	}

	public int get_doll_mpr() {
		return _dollPeriodicState.snapshot().mpr().amount();
	}

	public void set_doll_mpr(final int mpr) {
		PlayerActionLock.run(this, () -> {
			final DollPeriodicState.Recovery current = _dollPeriodicState.snapshot().mpr();
			_dollPeriodicState.replaceMpr(mpr, current.intervalSeconds());
		});
	}

	public int get_doll_mpr_time() {
		return _dollPeriodicState.snapshot().mpr().remainingSeconds();
	}

	public void set_doll_mpr_time(final int time) {
		PlayerActionLock.run(this,
				() -> _dollPeriodicState.setRemaining(DollPeriodicState.AwardType.MPR, time));
	}

	public int get_doll_mpr_time_src() {
		return _dollPeriodicState.snapshot().mpr().intervalSeconds();
	}

	public void set_doll_mpr_time_src(final int time) {
		PlayerActionLock.run(this, () -> {
			final DollPeriodicState.Recovery current = _dollPeriodicState.snapshot().mpr();
			_dollPeriodicState.replaceMpr(current.amount(), time);
		});
	}

	public int[] get_doll_get() {
		final DollPeriodicState.ItemGrant current = _dollPeriodicState.snapshot().item();
		return new int[] { current.itemId(), current.count() };
	}

	public void set_doll_get(final int itemid, final int count) {
		PlayerActionLock.run(this, () -> {
			final DollPeriodicState.ItemGrant current = _dollPeriodicState.snapshot().item();
			_dollPeriodicState.replaceItem(itemid, count, current.intervalSeconds());
		});
	}

	public int get_doll_get_time() {
		return _dollPeriodicState.snapshot().item().remainingSeconds();
	}

	public void set_doll_get_time(final int time) {
		PlayerActionLock.run(this,
				() -> _dollPeriodicState.setRemaining(DollPeriodicState.AwardType.ITEM, time));
	}

	public int get_doll_get_time_src() {
		return _dollPeriodicState.snapshot().item().intervalSeconds();
	}

	public void set_doll_get_time_src(final int time) {
		PlayerActionLock.run(this, () -> {
			final DollPeriodicState.ItemGrant current = _dollPeriodicState.snapshot().item();
			_dollPeriodicState.replaceItem(current.itemId(), current.count(), time);
		});
	}

	// 留言版使用
	private String _board_title;// 暫存文字串

	public void set_board_title(final String text) {
		this._board_title = text;
	}

	public String get_board_title() {
		return this._board_title;
	}

	private String _board_content;// 暫存文字串

	public void set_board_content(final String text) {
		this._board_content = text;
	}

	public String get_board_content() {
		return this._board_content;
	}

	// 封包接收速度紀錄
	private long _spr_move_time = 0;// 移動

	public void set_spr_move_time(final long spr_time) {
		_spr_move_time = spr_time;
	}

	public long get_spr_move_time() {
		return this._spr_move_time;
	}

	private long _spr_attack_time = 0;// 攻擊

	public void set_spr_attack_time(final long spr_time) {
		_spr_attack_time = spr_time;
	}

	public long get_spr_attack_time() {
		return this._spr_attack_time;
	}

	private long _spr_skill_time = 0;// 技能

	public void set_spr_skill_time(final long spr_time) {
		_spr_skill_time = spr_time;
	}

	public long get_spr_skill_time() {
		return this._spr_skill_time;
	}

	// 死亡相關

	private final AtomicInteger _delete_time = new AtomicInteger();// 死亡時間

	public void set_delete_time(final int time) {
		_delete_time.set(Math.max(0, time));
	}

	public int get_delete_time() {
		return _delete_time.get();
	}

	public long getDeathPipelineGeneration() {
		return _deathPipelineGeneration.get();
	}

	public boolean isDeathPipelineGeneration(final long generation) {
		return generation > 0L && _deathPipelineGeneration.get() == generation;
	}

	public int decrementDeleteTime(final long expectedGeneration) {
		while (isDeathPipelineGeneration(expectedGeneration)) {
			final int current = _delete_time.get();
			if (current <= 0) {
				return 0;
			}
			if (_delete_time.compareAndSet(current, current - 1)) {
				return isDeathPipelineGeneration(expectedGeneration) ? current - 1 : -1;
			}
		}
		return -1;
	}

	// 藥水使用HP恢復增加

	private int _up_hp_potion = 0;

	/**
	 * 藥水使用HP恢復增加(1/100)
	 * 
	 * @param up_hp_potion
	 */
	public void set_up_hp_potion(final int up_hp_potion) {
		_up_hp_potion = up_hp_potion;
	}

	/**
	 * 藥水使用HP恢復增加(1/100)
	 * 
	 * @return
	 */
	public int get_up_hp_potion() {
		return _up_hp_potion;
	}

	// 毒性抵抗效果
	int _venom_resist = 0;

	/**
	 * 毒性抵抗效果(裝備)
	 * 
	 * @param i
	 *          裝備數量
	 */
	public void set_venom_resist(int i) {
		_venom_resist += i;
	}

	/**
	 * 毒性抵抗效果(裝備)
	 * 
	 * @return
	 */
	public int get_venom_resist() {
		return _venom_resist;
	}

	// 加速檢測器
	private volatile AcceleratorChecker _speed = null;
	private final ReentrantLock _speedInitLock = new ReentrantLock();

	/**
	 * 加速檢測器
	 * 
	 * @return
	 */
	public AcceleratorChecker speed_Attack() {
		AcceleratorChecker checker = _speed;
		if (checker == null) {
			_speedInitLock.lock();
			try {
				checker = _speed;
				if (checker == null) {
					checker = new AcceleratorChecker(this);
					_speed = checker;
				}
			} finally {
				_speedInitLock.unlock();
			}
		}
		return checker;
	}

	public void resetSpeedCheckerGrace() {
		speed_Attack().resetWithGrace();
	}

	private int _arena = 0;// 競技場得分

	/**
	 * 競技場得分
	 * 
	 * @param i
	 */
	public void set_arena(int i) {
		_arena = i;
	}

	/**
	 * 競技場得分
	 * 
	 * @return
	 */
	public int get_arena() {
		return _arena;
	}

	private int _temp_adena = 0;// 本次使用貨幣類型

	/**
	 * shop_cn 是否為「nodetail」模式（NPC classname 含 nodetail，列表不送道具詳情）。<br>
	 * 購買時此為 true 則道具維持未鑑定，否則與一般商店相同為已鑑定。
	 */
	private boolean _shopCnNoDetail = false;

	/**
	 * 本次使用貨幣類型
	 * 
	 * @param itemid
	 */
	public void set_temp_adena(int itemid) {
		_temp_adena = itemid;
	}

	/**
	 * 本次使用貨幣類型
	 * 
	 * @return
	 */
	public int get_temp_adena() {
		return _temp_adena;
	}

	public void setShopCnNoDetail(final boolean shopCnNoDetail) {
		_shopCnNoDetail = shopCnNoDetail;
	}

	public boolean isShopCnNoDetail() {
		return _shopCnNoDetail;
	}

	public final void setMapsList(final HashMap<Integer, Integer> list) {
		CharMapsTimeTable.get().replaceInMemory(getId(), list);
	}

	/**
	 * 取回限時地圖時間
	 * 
	 * @param key
	 * @return
	 */
	public final int getMapsTime(final int key) {
		return CharMapsTimeTable.get().getUsedTime(getId(), key);
	}

	/**
	 * 
	 * @param key
	 * @param value
	 */
	public void putMapsTime(final int key, final int value) {
		CharMapsTimeTable.get().putTime(getId(), key, value);
	}

	/**
	 * {@code characters.RingsExpansion}：bit0=左戒額外格、bit1=右戒額外格、bit2=耳環第2格
	 */
	public static final byte RINGS_EXPANSION_RING_LEFT = 1;
	public static final byte RINGS_EXPANSION_RING_RIGHT = 2;
	public static final byte RINGS_EXPANSION_EARRING_SECOND = 4;

	private byte _ringsExpansion;

	public final byte getRingsExpansion() {
		return this._ringsExpansion;
	}

	/**
	 * 飾品欄擴充（與 DB RingsExpansion 同步）：bit0 左戒、bit1 右戒、bit2 耳環第二格
	 *
	 * @param i
	 *            3.63
	 */
	public final void setRingsExpansion(final byte i) {
		this._ringsExpansion = i;
	}

	private int killCount;

	/**
	 * @return the killCount
	 */
	public final int getKillCount() {
		return killCount;
	}

	/**
	 * 怪物擊殺數量
	 * 
	 * @param killCount
	 *                  3.63 the killCount to set
	 */
	public final void setKillCount(int killCount) {
		this.killCount = killCount;
	}

	/**
	 * 死騎拿鎖鍊劍
	 * 
	 * @param Type1
	 * @return
	 */
	public final int getWeaponType(final int Type1) {
		if (Type1 == ActionCodes.ACTION_ChainswordWalk) {
			if (SprTable.get().containsChainswordSpr(getTempCharGfx())) {
				return ActionCodes.ACTION_ChainswordWalk;

			} else {
				return ActionCodes.ACTION_SpearWalk;
			}
		}
		return Type1;
	}

	// -------------3.8新增-----------
	/** 血盟注视 */
	int attentionClanId;

	/** 设置血盟注视 */
	public void setAttentionClanId(int attentionClanId) {
		this.attentionClanId = attentionClanId;
	}

	/** 返回血盟注视 */
	public int getAttentionClanId() {
		return attentionClanId;
	}

	/** 血盟注視清單目前選取列（3.8 客戶端刪除封包 47 53 用） */
	private int _attentionListSelect = -1;

	public int getAttentionListSelect() {
		return this._attentionListSelect;
	}

	public void setAttentionListSelect(final int index) {
		this._attentionListSelect = index;
	}

	// 投石机相关
	private long _quiztime5 = 0;
	private long _quiztime6 = 0;

	public long getQuizTime5() {
		return _quiztime5;
	}

	public void setQuizTime5(long l) {
		_quiztime5 = l;
	}

	public long getQuizTime6() {
		return _quiztime6;
	}

	public void setQuizTime6(long l) {
		_quiztime6 = l;
	}

	int _SummonId = 0;

	public void setSummonId(int SummonId) {
		this._SummonId = SummonId;
	}

	public int getSummonId() {
		return this._SummonId;
	}

	// -----------end-------------

	// ========================================================================
	// Yiwei380 內掛系統擴充 (L1PcAutoHuntInfo 代理)
	// ========================================================================
	// 懶加載模式：只有當玩家真正使用內掛時才建立物件，節省記憶體
	private volatile L1PcAutoHuntInfo _autoHuntInfo = null;
	private final ReentrantLock _autoHuntInfoLock = new ReentrantLock();

	public L1PcAutoHuntInfo getAutoHuntInfo() {
		if (_autoHuntInfo == null) {
			_autoHuntInfoLock.lock();
			try {
				if (_autoHuntInfo == null) {
					_autoHuntInfo = new L1PcAutoHuntInfo();
				}
			} finally {
				_autoHuntInfoLock.unlock();
			}
		}
		return _autoHuntInfo;
	}

	/** 普通登入不額外建立 AutoHuntInfo；只取消已存在的離線掛機登出 callback。 */
	private void invalidatePendingOfflineLogout() {
		final L1PcAutoHuntInfo info = _autoHuntInfo;
		if (info != null) {
			info.invalidateOfflineLogoutForOnlineSession();
		}
	}

	// --- 代理方法 (兼容舊版 AutoHuntControl 呼叫) ---

	public boolean isAiRunning() {
		return getAutoHuntInfo().isAiRunning();
	}

	/**
	 * 舊 boolean API 相容入口。實際 AutoHunt/AutoFollow 生命週期一律使用
	 * {@link AiOwnerToken}，避免 check-then-set 與舊 callback 清掉新 owner。
	 */
	@Deprecated
	public void setAiRunning(final boolean running) {
		if (running) {
			getAutoHuntInfo().setAiRunning(true);
		} else {
			stopAI();
		}
	}

	public AiOwnerToken tryClaimPlayerAi(final Object owner, final AiMode mode) {
		return getAutoHuntInfo().tryClaimAiOwner(owner, mode);
	}

	public AiOwnerToken tryClaimPlayerAiIfGeneration(final Object owner, final AiMode mode,
			final long expectedGeneration) {
		return getAutoHuntInfo().tryClaimAiOwnerIfGeneration(owner, mode, expectedGeneration);
	}

	public boolean isPlayerAiOwner(final AiOwnerToken token) {
		return getAutoHuntInfo().isAiOwner(token);
	}

	public boolean releasePlayerAi(final AiOwnerToken token) {
		return getAutoHuntInfo().releaseAiOwner(token);
	}

	public boolean runIfPlayerAiOwner(final AiOwnerToken token, final Runnable action) {
		return getAutoHuntInfo().runIfAiOwner(token, action);
	}

	public boolean isPlayerAiGenerationCurrent(final long generation) {
		return getAutoHuntInfo().isGenerationCurrent(generation);
	}

	public long getPlayerAiGeneration() {
		return getAutoHuntInfo().currentGeneration();
	}

	public boolean runIfPlayerAiIdleGeneration(final long generation, final Runnable action) {
		return getAutoHuntInfo().runIfAiIdleGeneration(generation, action);
	}

	public AiOwnerToken getPlayerAiOwnerToken() {
		return getAutoHuntInfo().currentAiOwnerToken();
	}

	public Object getPlayerAiOwner() {
		return getAutoHuntInfo().currentAiOwner();
	}

	/** @deprecated 改由 {@link AutoHuntAI#start()} claim owner。 */
	@Deprecated
	public void setSmartAI(final Runnable ai) {
		if (ai == null) {
			stopAI();
			return;
		}
		if (ai instanceof AutoHuntAI) {
			// 相容舊式 setAiRunning(true) → setSmartAI(ai) 順序：先釋放僅有標記的 legacy owner。
			getAutoHuntInfo().setAiRunning(false);
			((AutoHuntAI) ai).start();
		}
	}

	public Runnable getSmartAI() {
		final AiOwnerToken token = getPlayerAiOwnerToken();
		if (token != null && token.mode() == AiMode.AUTO_HUNT && token.owner() instanceof Runnable) {
			return (Runnable) token.owner();
		}
		return null;
	}

	public void stopAI() {
		final AiOwnerToken token = getPlayerAiOwnerToken();
		if (token == null || token.mode() != AiMode.AUTO_HUNT) {
			return;
		}
		if (token.owner() instanceof AutoHuntAI) {
			((AutoHuntAI) token.owner()).stop();
		} else {
			// legacy boolean owner 沒有可取消的 Future，仍只能釋放當下 token。
			releasePlayerAi(token);
		}
	}

	/** 死亡流程另有 token 化 finalizer；不可由一般 stop 再排 1.5 秒離線登出。 */
	private void stopAIForDeath() {
		final AiOwnerToken token = getPlayerAiOwnerToken();
		if (token == null || token.mode() != AiMode.AUTO_HUNT) {
			return;
		}
		if (token.owner() instanceof AutoHuntAI) {
			((AutoHuntAI) token.owner()).stopForDeath();
		} else {
			releasePlayerAi(token);
		}
	}

	/**
	 * 離線死亡唯一 finalizer。角色狀態重設、同步存檔、離線表移除與 world logout
	 * 全部位於同一 player-AI transition fence；登入端會先取得同一 fence 並使 token 失效。
	 */
	private boolean isOfflineDeathLogoutCurrent(final AiOwnerToken logoutToken) {
		return isPlayerAiOwner(logoutToken) && getOnlineStatus() == 3
				&& getNetConnection() == null
				&& World.get().findObject(getId()) == this
				&& World.get().getPlayer(getName()) == this;
	}

	private void runOfflineDeathLogout(final AiOwnerToken logoutToken) {
		try {
			runIfPlayerAiOwner(logoutToken, () -> {
				if (getOnlineStatus() != 3 || getNetConnection() != null) {
					return;
				}
				// 同 objId/name 的新實例可能已經進入 World；舊 callback 不得碰它。
				if (World.get().findObject(getId()) != this || World.get().getPlayer(getName()) != this) {
					return;
				}
				try {
					setDead(false);
					if (isDead()) {
						return;
					}
					_deathPipelineGeneration.incrementAndGet();
					setStatus(0);
					setCurrentHp(getLevel());

					final int[] loc = com.lineage.server.datatables.GetbackTable.GetBack_Location(this, true);
					if (loc != null && loc.length >= 3) {
						setX(loc[0]);
						setY(loc[1]);
						setMap((short) loc[2]);
					} else {
						setX(33432);
						setY(32807);
						setMap((short) 4);
					}

					// save() 只排入通用池；離線 finalizer 必須同步落庫才可釋放 fence。
					saveSync();
					saveInventory();
					com.lineage.server.datatables.user.CharOfflLineTable.get().remove(getId());
					logout();
				} catch (final Exception e) {
					throw new OfflineDeathLogoutException(e);
				}
			});
		} catch (final OfflineDeathLogoutException e) {
			_log.error("離線死亡登出異常", e.getCause());
		} catch (final RuntimeException e) {
			_log.error("離線死亡登出異常", e);
		} finally {
			// 登入若已 invalidate/換代，此 CAS 必然失敗，不能清掉新 owner。
			releasePlayerAi(logoutToken);
		}
	}

	private static final class OfflineDeathLogoutException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private OfflineDeathLogoutException(final Exception cause) {
			super(cause);
		}
	}

	// 狀態相關
	public boolean isActived() {
		return isAiRunning();
	}

	public void setActived(boolean b) {
		setAiRunning(b);
	}

	// 檢查是否處於麻痺等不可掛機狀態
	public boolean isParalyzed_guaji() {
		return isParalyzed() || isSleeped() || isStunned();
	}

	public boolean isStunned() {
		// 請根據您的 L1SkillId 實際變數名稱調整
		return hasSkillEffect(SHOCK_STUN) ||
				hasSkillEffect(BONE_BREAK) ||
				hasSkillEffect(EARTH_BIND) ||
				hasSkillEffect(ICE_LANCE) ||
				hasSkillEffect(FREEZING_BLIZZARD) ||
				hasSkillEffect(FOG_OF_SLEEPING) ||
				// 如果有石化也算
				hasSkillEffect(STATUS_POISON_PARALYZED) ||
				isParalyzed();
	}

	// 存檔 (委派給 Table)
	public void saveAutoConfig() {
		if (com.lineage.server.model.MigrationDeparture.isDeparted(getId())) return;
		AutoHuntTable.getInstance().store(this);
	}

	/** 儲存高寵設定至資料庫 */
	public void saveAutoFollowConfig() {
		if (com.lineage.server.model.MigrationDeparture.isDeparted(getId())) return;
		com.lineage.server.datatables.user.AutoFollowTable.getInstance().store(this);
	}

	// --- Auto Set ---
	public int getAu_AutoSet(int i) {
		return getAutoHuntInfo().getAutoSet(i);
	}

	public void setAu_AutoSet(int i, int v) {
		getAutoHuntInfo().setAutoSet(i, v);
	}

	public int getAutoX() {
		return getAutoHuntInfo().getAutoX();
	}

	public void setAutoX(int x) {
		getAutoHuntInfo().setAutoX(x);
	}

	public int getAutoY() {
		return getAutoHuntInfo().getAutoY();
	}

	public void setAutoY(int y) {
		getAutoHuntInfo().setAutoY(y);
	}

	public int getAutoMap() {
		return getAutoHuntInfo().getAutoMap();
	}

	public void setAutoMap(int map) {
		getAutoHuntInfo().setAutoMap(map);
	}

	public void setAutoLocation(final int x, final int y, final int map) {
		getAutoHuntInfo().setAutoLocation(x, y, map);
	}

	// --- Auto Skill ---
	public int getAu_AutoSkill(int i) {
		return getAutoHuntInfo().getAutoSkill(i);
	}

	public void setAu_AutoSkill(int i, int v) {
		getAutoHuntInfo().setAutoSkill(i, v);
	}

	// --- Summon ---
	public String getSummon_npcid() {
		return getAutoHuntInfo().getSummonNpcId();
	}

	public void setSummon_npcid(String id) {
		getAutoHuntInfo().setSummonNpcId(id);
	}

	public int getsummon_skillid() {
		return getAutoHuntInfo().getSummonHealSkillId();
	}

	public void setsummon_skillid(int id) {
		getAutoHuntInfo().setSummonHealSkillId(id);
	}

	public int getsummon_skillidmp_1() {
		return getAutoHuntInfo().getSummonHealMp();
	}

	public void setsummon_skillidmp_1(int mp) {
		getAutoHuntInfo().setSummonHealMp(mp);
	}

	// --- Shop ---
	public boolean IsAu_Shop() {
		return getAutoHuntInfo().isAuShop();
	}

	public void setAu_Shop(boolean b) {
		getAutoHuntInfo().setAuShop(b);
	}

	public boolean IsAu_SetShop() {
		return getAutoHuntInfo().isAuSetShop();
	}

	public void setAu_SetShop(boolean b) {
		getAutoHuntInfo().setAuSetShop(b);
	}

	public int getAu_SetShopType() {
		return getAutoHuntInfo().getAuSetShopType();
	}

	public void SetAu_SetShopType(int i) {
		getAutoHuntInfo().setAuSetShopType(i);
	} // 注意大小寫相容

	public int getAu_BuyItemSwitch(int i) {
		return getAutoHuntInfo().getBuyItemSwitch(i);
	}

	public void setAu_BuyItemSwitch(int i, int v) {
		getAutoHuntInfo().setBuyItemSwitch(i, v);
	}

	public int getAu_BuyItemCount(int i) {
		return getAutoHuntInfo().getBuyItemCount(i);
	}

	public void setAu_BuyItemCount(int i, int v) {
		getAutoHuntInfo().setBuyItemCount(i, v);
	}

	// --- Other ---
	public int getAu_OtherSet(int i) {
		return getAutoHuntInfo().getOtherSet(i);
	}

	public void setAu_OtherSet(int i, int v) {
		getAutoHuntInfo().setOtherSet(i, v);
	}

	public java.util.ArrayList<String> InEnemyList() {
		return getAutoHuntInfo().getEnemyList();
	}

	public boolean isInEnemyList(String name) {
		return getAutoHuntInfo().containsEnemy(name);
	}

	public void setInEnemyList(String name) {
		getAutoHuntInfo().addEnemy(name);
	}

	public void clearEnemyList() {
		getAutoHuntInfo().clearEnemy();
	}

	public void setsummon_skillidmp(int mp) {
		getAutoHuntInfo().setSummonHealMp(mp);
	}
	// ========================================================================
	// ==========================================================
	// Yiwei380 內掛系統補丁 (Fix AutoHuntAI Errors)
	// ==========================================================

	/** 跟隨目標 (用於高寵/跟隨模式) - 我是高寵時的主人 */
	private final AtomicReference<L1PcInstance> _followMaster = new AtomicReference<>();

	/** 跟隨我的高寵集合 (我是主人時)，key=pc.getId()，使用 ConcurrentHashMap 確保執行緒安全 */
	private final java.util.concurrent.ConcurrentHashMap<Integer, L1PcInstance> _followers = new java.util.concurrent.ConcurrentHashMap<>();

	/** 高寵功能總開關 (是否允許被邀請) */
	private volatile boolean _isFollowEnabled = false;

	/** 跟隨狀態標記 (L1AutoFollowAI 使用) */
	private volatile boolean _isFollow = false;

	/** 是否幫打 */
	private volatile boolean _isFollowAttack = false;

	/** 是否施放攻擊魔法 */
	private volatile boolean _isFollowMagic = false;

	/** 跟隨距離 (預設2格) */
	private volatile int _followDistance = 2;

	/** 攻擊魔法 ID */
	private volatile int _followAttackSkillId = 0;

	/** 攻擊魔力限制% */
	private volatile int _followAttackMp = 30;

	/** 是否幫補血 */
	private volatile boolean _isFollowHeal = false;

	/** 幫補血 HP% */
	private volatile int _followHealHp = 80;

	/** 治癒技能 ID */
	private volatile int _followHealSkillId = 0;

	/** 輔助狀態 (index 1~6) */
	private final AtomicIntegerArray _followBuffs = new AtomicIntegerArray(7);

	public L1PcInstance getFollowMaster() {
		return _followMaster.get();
	}

	public void setFollowMaster(L1PcInstance master) {
		_followMaster.set(master);
	}

	/** 僅能清除自己當時綁定的主人，舊 Follow callback 不可清掉新關係。 */
	public boolean clearFollowMaster(final L1PcInstance expectedMaster) {
		return expectedMaster != null && _followMaster.compareAndSet(expectedMaster, null);
	}

	public java.util.Map<Integer, L1PcInstance> getFollowers() {
		return _followers;
	}

	public void addFollower(L1PcInstance pc) {
		if (pc != null) _followers.put(pc.getId(), pc);
	}

	public void removeFollower(L1PcInstance pc) {
		if (pc != null) _followers.remove(pc.getId());
	}

	public boolean isFollowEnabled() {
		return _isFollowEnabled;
	}

	public void setFollowEnabled(boolean b) {
		_isFollowEnabled = b;
	}

	public void setfollow(boolean d) {
		this._isFollow = d;
	}

	public boolean isfollow() {
		return this._isFollow;
	}

	public void set_followmaster(L1PcInstance master) {
		setFollowMaster(master);
	}

	public L1PcInstance get_followmaster() {
		return getFollowMaster();
	}

	public void startFollow(final L1PcInstance master) {
		tryStartFollow(master);
	}

	/** 供邀請流程確認 owner 與 Future 皆成功安裝後，才宣告連線成功。 */
	public boolean tryStartFollow(final L1PcInstance master) {
		if (master == null) {
			return false;
		}
		final AiOwnerToken current = getPlayerAiOwnerToken();
		if (current != null && current.mode() == AiMode.AUTO_FOLLOW
				&& current.owner() instanceof L1AutoFollowAI) {
			((L1AutoFollowAI) current.owner()).stop();
		}
		stopAI();
		com.lineage.server.datatables.user.AutoFollowTable.getInstance().load(this); // 確保運行時依資料庫設定
		return new L1AutoFollowAI(this, master).tryStart();
	}

	public void stopFollow() {
		// 1. 關閉自己的 AI。真正的 Follow owner 必須自行清理雙向關係；它在
		// release token 前完成清理，因此舊 stop 返回後不得再 getAndSet(null)。
		final AiOwnerToken current = getPlayerAiOwnerToken();
		if (current != null && current.mode() == AiMode.AUTO_FOLLOW) {
			if (current.owner() instanceof L1AutoFollowAI) {
				((L1AutoFollowAI) current.owner()).stop();
			} else {
				final L1PcInstance expectedMaster = getFollowMaster();
				if (expectedMaster != null) {
					runIfPlayerAiOwner(current, () -> clearLegacyFollowRelation(expectedMaster));
				}
				releasePlayerAi(current);
			}
		} else if (current == null) {
			// 僅修復沒有 owner 的 legacy/orphan 關係；generation fence 可防止
			// stop 與新 Follow claim 交錯後清掉新主人。
			final long generation = getPlayerAiGeneration();
			final L1PcInstance expectedMaster = getFollowMaster();
			if (expectedMaster != null) {
				runIfPlayerAiIdleGeneration(generation, () -> clearLegacyFollowRelation(expectedMaster));
			}
		}

		// 2. 如果自己是主人，只停止快照中仍以「這個主人」為 owner 的 AI。
		// 不可直接清 follower.followMaster 或 _followers.clear()，否則舊主人
		// callback 可能斷開 follower 後來建立的新 session。
		if (!_followers.isEmpty()) {
			for (L1PcInstance f : new java.util.ArrayList<>(_followers.values())) {
				if (f != null) {
					if (f.stopFollowingMaster(this)) {
						com.lineage.commons.system.AutoFollowControl.getInstance().showMenu(f);
					}
				}
			}
		}
		// 3. [Yiwei380] 高寵狀態解除時刷新畫面封包 (解決變身/動作卡住)
		sendPackets(new S_MapID(this, getMapId(), getMap().isUnderwater()));
		sendPackets(new S_OwnCharPack(this));
		sendPackets(new S_OwnCharStatus(this));
		removeAllKnownObjects();
		updateObject();
	}

	/**
	 * 只供主人端以身分條件停止跟隨；舊 master 快照不得停止新的 Follow owner。
	 */
	public boolean stopFollowingMaster(final L1PcInstance expectedMaster) {
		if (expectedMaster == null) {
			return false;
		}
		final AiOwnerToken current = getPlayerAiOwnerToken();
		if (current != null && current.mode() == AiMode.AUTO_FOLLOW
				&& current.owner() instanceof L1AutoFollowAI) {
			return ((L1AutoFollowAI) current.owner()).stopIfFollowing(expectedMaster);
		}

		// legacy/orphan 路徑也必須在相同 token/generation fence 內清理。
		final java.util.concurrent.atomic.AtomicBoolean cleared =
				new java.util.concurrent.atomic.AtomicBoolean(false);
		if (current != null) {
			if (current.mode() != AiMode.AUTO_FOLLOW) {
				return false;
			}
			runIfPlayerAiOwner(current,
					() -> cleared.set(clearLegacyFollowRelation(expectedMaster)));
			if (cleared.get()) {
				releasePlayerAi(current);
			}
		} else {
			final long generation = getPlayerAiGeneration();
			runIfPlayerAiIdleGeneration(generation,
					() -> cleared.set(clearLegacyFollowRelation(expectedMaster)));
		}
		return cleared.get();
	}

	/** 呼叫者必須持有 player-AI token 或 idle-generation transition fence。 */
	private boolean clearLegacyFollowRelation(final L1PcInstance expectedMaster) {
		if (expectedMaster == null || !clearFollowMaster(expectedMaster)) {
			return false;
		}
		setfollow(false);
		setNowTarget(null);
		expectedMaster.removeFollower(this);
		return true;
	}

	public boolean isFollowRunning() {
		return getAutoHuntInfo().isFollowRunning();
	}

	public boolean isFollowAttack() {
		return _isFollowAttack;
	}

	public void setFollowAttack(boolean b) {
		_isFollowAttack = b;
	}

	public boolean isFollowMagic() {
		return _isFollowMagic;
	}

	public void setFollowMagic(boolean b) {
		_isFollowMagic = b;
	}

	public int getFollowDistance() {
		return _followDistance;
	}

	public void setFollowDistance(int i) {
		_followDistance = i;
	}

	public int getFollowAttackSkillId() {
		return _followAttackSkillId;
	}

	public void setFollowAttackSkillId(int i) {
		_followAttackSkillId = i;
	}

	public int getFollowAttackMp() {
		return _followAttackMp;
	}

	public void setFollowAttackMp(int i) {
		_followAttackMp = i;
	}

	public boolean isFollowHeal() {
		return _isFollowHeal;
	}

	public void setFollowHeal(boolean b) {
		_isFollowHeal = b;
	}

	public int getFollowHealHp() {
		return _followHealHp;
	}

	public void setFollowHealHp(int i) {
		_followHealHp = i;
	}

	public int getFollowHealSkillId() {
		return _followHealSkillId;
	}

	public void setFollowHealSkillId(int i) {
		_followHealSkillId = i;
	}

	public int getFollowBuffSkillId(int index) {
		if (index >= 1 && index <= 6)
			return _followBuffs.get(index);
		return 0;
	}

	public void setFollowBuffSkillId(int index, int skillId) {
		if (index >= 1 && index <= 6)
			_followBuffs.set(index, skillId);
	}

	// ----------------------------------------------------------

	/** 最後被玩家攻擊的時間 (用於 PVP 逃跑判定) */
	private final AtomicLong _lastPvpTime = new AtomicLong();

	/**
	 * * 設定最後被玩家攻擊的時間
	 * (需在 L1AttackPc.java 中呼叫此方法: pc.setLastPvpTime(System.currentTimeMillis());)
	 */
	public void setLastPvpTime(long time) {
		_lastPvpTime.accumulateAndGet(time, Math::max);
	}

	/** Current monotonic PVP-hit timestamp, exposed for diagnostics/tests. */
	public long getLastPvpTime() {
		return _lastPvpTime.get();
	}

	/**
	 * 檢查是否處於 PVP 攻擊狀態 (判定閾值: 3秒內)
	 * 
	 * @return true=正在被玩家攻擊, false=安全
	 */
	public boolean isUnderPvpAttack() {
		return System.currentTimeMillis() - _lastPvpTime.get() < 3000;
	}

	// ==================== 卡冊收藏系統 ====================

	private final PlayerAutoActionGate _autoPolyGate = new PlayerAutoActionGate();
	private final PlayerAutoActionGate _autoDollGate = new PlayerAutoActionGate();

	public String getAutoPolyAction() {
		return _autoPolyGate.getAction();
	}

	public void setAutoPolyAction(final String action) {
		_autoPolyGate.setAction(action);
	}

	public PlayerAutoActionGate.Token tryClaimAutoPolyAction() {
		return _autoPolyGate.tryClaim();
	}

	public boolean isCurrentAutoPolyAction(final PlayerAutoActionGate.Token token) {
		return _autoPolyGate.isCurrent(token);
	}

	public void releaseAutoPolyAction(final PlayerAutoActionGate.Token token) {
		_autoPolyGate.release(token);
	}

	public PlayerAutoActionGate.Token tryReclaimAutoPolyAction(
			final PlayerAutoActionGate.Token token) {
		return _autoPolyGate.tryReclaim(token);
	}

	public boolean clearAutoPolyActionIfCurrent(final PlayerAutoActionGate.Token token) {
		return _autoPolyGate.clearIfCurrent(token);
	}

	public String getAutoDollAction() {
		return _autoDollGate.getAction();
	}

	public void setAutoDollAction(final String action) {
		_autoDollGate.setAction(action);
	}

	public PlayerAutoActionGate.Token tryClaimAutoDollAction() {
		return _autoDollGate.tryClaim();
	}

	public boolean isCurrentAutoDollAction(final PlayerAutoActionGate.Token token) {
		return _autoDollGate.isCurrent(token);
	}

	public void releaseAutoDollAction(final PlayerAutoActionGate.Token token) {
		_autoDollGate.release(token);
	}

	public PlayerAutoActionGate.Token tryReclaimAutoDollAction(
			final PlayerAutoActionGate.Token token) {
		return _autoDollGate.tryReclaim(token);
	}

	public boolean clearAutoDollActionIfCurrent(final PlayerAutoActionGate.Token token) {
		return _autoDollGate.clearIfCurrent(token);
	}

	/** 變身單卡能力 */
	public void addCardStats() {
		if (getOnlineStatus() != 1 || getQuest() == null)
			return;
		try {
			for (com.lineage.server.templates.CardInfo card : com.lineage.server.datatables.CardSystemLoader.get()
					.getAllCards()) {
				if (getQuest().get_step(card.getQuestId()) > 0) {
					addStr((short) card.getStr());
					addDex((short) card.getDex());
					addCon((short) card.getCon());
					addInt((short) card.getInt());
					addWis((short) card.getWis());
					addCha((short) card.getCha());
					addMaxHp(card.getHp());
					addMaxMp(card.getMp());
				}
			}
		} catch (Exception e) {
			_log.error("addCardStats 異常: " + getName(), e);
		}
	}

	/** 變身套卡能力 */
	public void addCardSetStats() {
		if (getOnlineStatus() != 1 || getQuest() == null)
			return;
		try {
			for (com.lineage.server.templates.CardSetInfo set : com.lineage.server.datatables.CardSetLoader.get()
					.getAllSets()) {
				boolean hasAll = true;
				for (int qId : set.getQuestIds()) {
					if (getQuest().get_step(qId) == 0) {
						hasAll = false;
						break;
					}
				}
				if (hasAll) {
					addStr((short) set.getStr());
					addDex((short) set.getDex());
					addCon((short) set.getCon());
					addInt((short) set.getInt());
					addWis((short) set.getWis());
					addCha((short) set.getCha());
					addMaxHp(set.getHp());
					addMaxMp(set.getMp());
					addHpr(set.getHpr());
					addMpr(set.getMpr());
					addDmgup(set.getDmgShort());
					addHitup(set.getHitShort());
					addBowDmgup(set.getDmgLong());
					addBowHitup(set.getHitLong());
					addDamageReductionByArmor(set.getDmgReduc());
					addSp(set.getSp());
					addMr((short) set.getMr());
					addFire(set.getFire());
					addWater(set.getWater());
					addWind(set.getWind());
					addEarth(set.getEarth());
				}
			}
		} catch (Exception e) {
			_log.error("addCardSetStats 異常: " + getName(), e);
		}
	}

	/** 娃娃單卡能力 */
	public void addDollStats() {
		if (getOnlineStatus() != 1 || getQuest() == null)
			return;
		try {
			for (com.lineage.server.templates.DollCardInfo card : com.lineage.server.datatables.DollCardLoader.get()
					.getAllCards()) {
				if (getQuest().get_step(card.getQuestId()) > 0) {
					addStr((short) card.getStr());
					addDex((short) card.getDex());
					addCon((short) card.getCon());
					addInt((short) card.getInt());
					addWis((short) card.getWis());
					addCha((short) card.getCha());
					addAc((byte) card.getAc());
					addMaxHp(card.getHp());
					addMaxMp(card.getMp());
					addHpr(card.getHpr());
					addMpr(card.getMpr());
					addDmgup(card.getDmgShort());
					addBowDmgup(card.getDmgLong());
					addHitup(card.getHitShort());
					addBowHitup(card.getHitLong());
					addDamageReductionByArmor(card.getDmgReduc());
					addSp(card.getSp());
					addMr((short) card.getMr());
					addFire(card.getFire());
					addWater(card.getWater());
					addWind(card.getWind());
					addEarth(card.getEarth());
				}
			}
		} catch (Exception e) {
			_log.error("addDollStats 異常: " + getName(), e);
		}
	}

	/** 娃娃套卡能力 */
	public void addDollSetStats() {
		if (getOnlineStatus() != 1 || getQuest() == null)
			return;
		try {
			for (com.lineage.server.templates.DollCardSetInfo set : com.lineage.server.datatables.DollCardLoader.get()
					.getAllSets()) {
				boolean hasAll = true;
				for (int qId : set.getQuestIds()) {
					if (getQuest().get_step(qId) == 0) {
						hasAll = false;
						break;
					}
				}
				if (hasAll) {
					addStr((short) set.getStr());
					addDex((short) set.getDex());
					addCon((short) set.getCon());
					addInt((short) set.getInt());
					addWis((short) set.getWis());
					addCha((short) set.getCha());
					addMaxHp(set.getHp());
					addMaxMp(set.getMp());
					addHpr(set.getHpr());
					addMpr(set.getMpr());
					addDmgup(set.getDmgShort());
					addHitup(set.getHitShort());
					addBowDmgup(set.getDmgLong());
					addBowHitup(set.getHitLong());
					addDamageReductionByArmor(set.getDmgReduc());
					addSp(set.getSp());
					addMr((short) set.getMr());
					addAc((byte) set.getAc());
					addFire(set.getFire());
					addWater(set.getWater());
					addWind(set.getWind());
					addEarth(set.getEarth());
				}
			}
		} catch (Exception e) {
			_log.error("addDollSetStats 異常: " + getName(), e);
		}
	}

	/** 變身結束時自動重新變身；僅供 PlayerAiWorker 以排程 token 呼叫。 */
	public void handleAutoPoly(final PlayerAutoActionGate.Token token) {
		try {
			PlayerActionLock.run(this, () -> _autoPolyGate.runIfCurrent(token, () -> {
				if (getOnlineStatus() != 1 || getNetConnection() == null || getQuest() == null) {
					return;
				}
				final com.lineage.server.templates.CardInfo card =
						com.lineage.server.datatables.CardSystemLoader.get()
								.getCardByAction(token.action());
				if (card == null || getQuest().get_step(card.getQuestId()) <= 0) {
					setAutoPolyAction(null);
					return;
				}
				if (!L1PolyMorph.canDoPoly(this, card.getPolyId(),
						L1PolyMorph.MORPH_BY_ITEMMAGIC)) {
					setAutoPolyAction(null);
					sendPackets(new S_ServerMessage(166,
							"目前狀態無法自動變身，未扣除道具並已關閉。"));
					return;
				}

				final int itemId = card.getConsumeItemId();
				final int count = card.getConsumeCount();
				if (itemId > 0 && count > 0 && !getInventory().consumeItem(itemId, count)) {
					setAutoPolyAction(null);
					final com.lineage.server.templates.L1Item item =
							com.lineage.server.datatables.ItemTable.get().getTemplate(itemId);
					if (item != null) {
						sendPackets(new S_ServerMessage(337, item.getName()));
					}
					sendPackets(new S_ServerMessage(166, "關閉自動變身"));
					return;
				}

				if (L1PolyMorph.doPoly(this, card.getPolyId(), card.getTime(),
						L1PolyMorph.MORPH_BY_ITEMMAGIC)) {
					sendPackets(new S_ServerMessage(166, "變身" + card.getCardLabel()));
				} else {
					setAutoPolyAction(null);
					_log.error("Auto poly material committed but morph effect failed: char={} action={}",
							getName(), token.action());
					sendPackets(new S_ServerMessage(166,
							"自動變身效果執行失敗，已關閉。"));
				}
			}));
		} catch (final RuntimeException e) {
			_log.error("handleAutoPoly 異常: " + getName(), e);
			_autoPolyGate.clearIfCurrent(token);
		} finally {
			_autoPolyGate.release(token);
		}
	}

	/** 娃娃消失時自動重新召喚；僅供 PlayerAiWorker 以排程 token 呼叫。 */
	public void handleAutoDoll(final PlayerAutoActionGate.Token token) {
		try {
			PlayerActionLock.run(this, () -> _autoDollGate.runIfCurrent(token, () -> {
				if (getOnlineStatus() != 1 || getNetConnection() == null || getQuest() == null
						|| !getDolls().isEmpty()) {
					return;
				}
				com.lineage.server.model.quest.CardActionHandler
						.executeDollActionLocked(this, token.action());
			}));
		} catch (final RuntimeException e) {
			_log.error("handleAutoDoll 異常: " + getName(), e);
			_autoDollGate.clearIfCurrent(token);
		} finally {
			_autoDollGate.release(token);
		}
	}

	/** 紋樣系統等級 (索引 0 不用，索引 1~6 對應 6 種紋樣) */
	private int[] _wenyangLevels = new int[] { 0, 0, 0, 0, 0, 0, 0 };
	private List<com.lineage.server.templates.L1WenyangConfig> _cachedWenyangConfigs = List.of();

	/**
	 * 紋樣系統各部位的隨機機率加成值 (索引 0 不用，索引 1~6 對應 6 種紋樣，每種有 3 個階層: +1, +2, +3 的機率) -
	 * 僅存活於登入期間
	 */
	private int[][] _wenyangRefreshBonus = new int[][] {
			{ 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 }
	};

	public int getWenyangLevel(int type) {
		if (type < 1 || type > 6)
			return 0;
		return _wenyangLevels[type];
	}

	public void setWenyangLevel(int type, int level) {
		if (type < 1 || type > 6)
			return;
		this._wenyangLevels[type] = level;
	}

	public int[] getWenyangLevels() {
		return _wenyangLevels;
	}

	public List<com.lineage.server.templates.L1WenyangConfig> getCachedWenyangConfigs() {
		return _cachedWenyangConfigs;
	}

	public void setCachedWenyangConfigs(
			final List<com.lineage.server.templates.L1WenyangConfig> configs) {
		_cachedWenyangConfigs = configs == null || configs.isEmpty()
				? List.of() : List.copyOf(configs);
	}

	public int[] getWenyangRefreshBonus(int type) {
		if (type < 1 || type > 6)
			return new int[] { 0, 0, 0 };
		return _wenyangRefreshBonus[type];
	}

	public void setWenyangRefreshBonus(int type, int[] val) {
		if (type < 1 || type > 6 || val == null || val.length != 3)
			return;
		this._wenyangRefreshBonus[type] = val;
	}

	public void setWenyangLevels(int[] levels) {
		if (levels != null && levels.length == 7) {
			this._wenyangLevels = levels;
		}
	}

	// =======================================================
	// NPC Action 防連點與防掛系統
	// =======================================================
	private long _lastNpcActionTime = 0;
	private int _npcActionSpamCount = 0;
	private String _lastNpcActionString = "";

	/**
	 * 檢查 NPC Action 是否過度頻繁 (0.3秒安全門)
	 * 
	 * @param action 客戶端請求的 action 字串
	 * @return true 代表允許執行, false 代表頻率過高需阻擋
	 */
	public boolean checkNpcActionSpam(String action) {
		long now = System.currentTimeMillis();
		// 0.3 秒 = 300 毫秒
		if (now - this._lastNpcActionTime < 300) {
			if (action != null && action.equals(this._lastNpcActionString)) {
				this._npcActionSpamCount++;
			} else {
				// 即使動作不同，只要短時間內狂點一樣算異常
				this._npcActionSpamCount++;
				this._lastNpcActionString = action;
			}
			// 【關鍵機制】刷新最後點擊時間，讓持續高頻發送的外掛永遠卡在冷卻中
			this._lastNpcActionTime = now;
			return false;
		}

		// 通過 0.3 秒冷卻，重置計數
		this._npcActionSpamCount = 0;
		this._lastNpcActionString = action;
		this._lastNpcActionTime = now;
		return true;
	}

	public int getNpcActionSpamCount() {
		return this._npcActionSpamCount;
	}

	// ==========================================================
}
