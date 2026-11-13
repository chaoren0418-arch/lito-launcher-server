package local.lito381.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * T134 (#385): one character's shared item cooldowns ({@code etcitem.delay_id} groups),
 * mirroring 1-伺服器 {@code L1ItemDelay} / {@code L1Character._itemdelay}.
 *
 * <ul>
 * <li>A use installs a lease of {@code delay_time} ms on its group; a second lease on the
 * same group keeps the later deadline ({@code L1ItemDelay.ItemDelayTimer.merge}, :261-271).</li>
 * <li>The group is busy while {@code deadline - now > 0}; at exactly {@code delay_time}
 * it is free again ({@code isExpiredForCurrentAction}, :242-251, and
 * {@code L1Character.hasItemDelay}, :1938-1955, which also drops the expired lease).</li>
 * <li>Zero group or non-positive time installs nothing ({@code L1ItemDelay.java:87-90,99}).</li>
 * </ul>
 *
 * <p>1-伺服器 measures from the packet's ingress timestamp; LiTo handles C_USE_ITEM on
 * the session thread and uses {@link System#nanoTime()} when the item is used.</p>
 */
final class ItemUseDelays {
    private final Map<Integer, Long> deadlines = new HashMap<>();

    /** True while {@code delayId}'s cooldown is running at {@code nowNanos}. */
    synchronized boolean blocked(int delayId, long nowNanos) {
        if (delayId <= 0) return false;
        Long deadline = deadlines.get(delayId);
        if (deadline == null) return false;
        if (deadline - nowNanos > 0L) return true;
        deadlines.remove(delayId);
        return false;
    }

    /** Starts (or extends) the cooldown for an item that was actually used. */
    synchronized void start(ConsumableItemParameters.Delay delay, long nowNanos) {
        if (delay == null || !delay.active()) return;
        long deadline = nowNanos + TimeUnit.MILLISECONDS.toNanos(delay.millis());
        deadlines.merge(delay.id(), deadline, (current, candidate) ->
                candidate - current > 0L ? candidate : current);
    }

    synchronized int activeGroups() {
        return deadlines.size();
    }
}
