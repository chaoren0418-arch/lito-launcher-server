package com.lineage.server.model.skill;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lineage.server.model.L1Character;
import com.lineage.server.model.Instance.L1PcInstance;
import com.lineage.server.thread.GeneralThreadPool;

/**
 * 技能效果時間軸
 * @author daien
 *
 */
public class L1SkillTimerTimerImpl implements L1SkillTimer, Runnable {

	private static final Logger _log = LogManager.getLogger(L1SkillTimerTimerImpl.class);

	private static final AtomicLong BEGIN_COUNT = new AtomicLong();
	private static final AtomicLong SCHEDULED_COUNT = new AtomicLong();
	private static final AtomicLong ACTIVE_COUNT = new AtomicLong();
	private static final AtomicLong COMPLETED_COUNT = new AtomicLong();
	private static final AtomicLong TICK_COUNT = new AtomicLong();
	private static final AtomicLong END_COUNT = new AtomicLong();
	private static final AtomicLong KILL_COUNT = new AtomicLong();
	private static final AtomicLong SKIP_COUNT = new AtomicLong();
	private static final AtomicLong REJECTED_COUNT = new AtomicLong();
	private static final AtomicLong ERROR_COUNT = new AtomicLong();

	private volatile ScheduledFuture<?> _future = null;

	private final L1Character _cha;
	
	private final int _timeMillis;
	
	private final int _skillId;
	
	private volatile int _remainingTime;

	/**
	 * 技能效果時間軸
	 * @param cha 執行者
	 * @param skillId 技能編號
	 * @param timeMillis 技能時間(毫秒)
	 */
	public L1SkillTimerTimerImpl(final L1Character cha, final int skillId, final int timeMillis) {
		_cha = cha;
		_skillId = skillId;
		_timeMillis = timeMillis;

		_remainingTime = _timeMillis / 1000;
	}

	private void runMonitoredTick() {
		ACTIVE_COUNT.incrementAndGet();
		try {
			run();
		} catch (final RuntimeException e) {
			ERROR_COUNT.incrementAndGet();
			_log.error("L1SkillTimer tick failed: chaId={} skillId={}",
					_cha.getId(), _skillId, e);
			throw e;
		} finally {
			COMPLETED_COUNT.incrementAndGet();
			ACTIVE_COUNT.decrementAndGet();
		}
	}

	//@Override
	public void run() {
		TICK_COUNT.incrementAndGet();
		_remainingTime--;
		/*if (_skillId == 68||_skillId == 89) {
			_log.debug("_remainingTime:"+_remainingTime);
		}*/
		if (_remainingTime <= 0) {
			/*if (_skillId == 68||_skillId == 89) {
				_log.debug("_remainingTime:"+0);
			}*/
			_cha.removeSkillEffect(_skillId, this);
		}
	}

	//@Override
	public void begin() {
		//System.out.println("skillId:"+_skillId + " " + _remainingTime + "/" + _timeMillis);
		BEGIN_COUNT.incrementAndGet();
		try {
			SCHEDULED_COUNT.incrementAndGet();
			final GeneralThreadPool pool = GeneralThreadPool.get();
			_future = (_cha instanceof L1PcInstance)
					? pool.pcScheduleAtFixedRate(this::runMonitoredTick, 1000, 1000)
					: pool.aiScheduleAtFixedRate(this::runMonitoredTick, 1000, 1000);
			if (_future == null) {
				throw new RejectedExecutionException("L1SkillTimer schedule returned null");
			}
		} catch (final RejectedExecutionException e) {
			REJECTED_COUNT.incrementAndGet();
			ERROR_COUNT.incrementAndGet();
			_log.warn("L1SkillTimer begin rejected: chaId={} skillId={} timeMillis={}",
					_cha.getId(), _skillId, _timeMillis, e);
			throw e;
		} catch (final RuntimeException e) {
			REJECTED_COUNT.incrementAndGet();
			ERROR_COUNT.incrementAndGet();
			_log.warn("L1SkillTimer begin failed: chaId={} skillId={} timeMillis={}",
					_cha.getId(), _skillId, _timeMillis, e);
			throw e;
		}
	}

	//@Override
	public void end() {
		END_COUNT.incrementAndGet();
		kill();
		try {
			L1SkillStop.stopSkill(_cha, _skillId);

		} catch (final Throwable e) {
			ERROR_COUNT.incrementAndGet();
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	//@Override
	public void kill() {
		KILL_COUNT.incrementAndGet();
		try {
			if (_future != null) {
				// 试图取消对此任务的执行。
				//如果任务已完成、或已取消，或者由于某些其他原因而无法取消，则此尝试将失败。
				//当调用 cancel 时，如果调用成功，而此任务尚未启动，则此任务将永不运行。
				//如果任务已经启动，则 mayInterruptIfRunning 参数确定是否应该以试图停止任务的方式来中断执行此任务的线程。 
				
				//此方法返回后，对 isDone() 的后续调用将始终返回 true。
				//如果此方法返回 true，则对 isCancelled() 的后续调用将始终返回 true。 
				//参数：
				//mayInterruptIfRunning - 如果应该中断执行此任务的线程，则为 true；否则允许正在运行的任务运行完成 
				//返回：
				//如果无法取消任务，则返回 false，这通常是由于它已经正常完成；否则返回 true
				_future.cancel(false);
			} else {
				SKIP_COUNT.incrementAndGet();
			}

		} catch (final Throwable e) {
			ERROR_COUNT.incrementAndGet();
			_log.error(e.getLocalizedMessage(), e);
		}
	}

	//@Override
	public int getRemainingTime() {
		return _remainingTime;
	}

	public static String getMonitorLine() {
		return String.format(
				"L1SkillTimer begin=%d scheduled=%d active=%d done=%d tick=%d end=%d kill=%d skip=%d rejected=%d error=%d",
				BEGIN_COUNT.get(), SCHEDULED_COUNT.get(), ACTIVE_COUNT.get(), COMPLETED_COUNT.get(),
				TICK_COUNT.get(), END_COUNT.get(), KILL_COUNT.get(), SKIP_COUNT.get(),
				REJECTED_COUNT.get(), ERROR_COUNT.get());
	}
}
