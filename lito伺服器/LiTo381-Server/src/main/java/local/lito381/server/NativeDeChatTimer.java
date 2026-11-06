// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import local.lito381.server.NativeDeChatAccess;
import local.lito381.server.NativeDeChatChat;
import local.lito381.server.NativeDeChatChatGlobal;
import local.lito381.server.NativeDeChatChatShouting;
import local.lito381.server.NativeDeChatData;
import local.lito381.server.NativeDeChatScope;
import local.lito381.server.NativeDeChatWorld;
import local.lito381.server.NativeDeNpcConstructionState;
import local.lito381.server.NativeDeWorldLog;

public class NativeDeChatTimer
implements Runnable {
    private static final NativeDeWorldLog _log = NativeDeChatScope.Logs.getLogger(NativeDeChatTimer.class);
    private static final AtomicLong RUN_COUNT = new AtomicLong();
    private static final AtomicLong SCHEDULED_COUNT = new AtomicLong();
    private static final AtomicLong ACTIVE_COUNT = new AtomicLong();
    private static final AtomicLong COMPLETED_COUNT = new AtomicLong();
    private static final AtomicLong CHAT_COUNT = new AtomicLong();
    private static final AtomicLong SKIP_COUNT = new AtomicLong();
    private static final AtomicLong REJECTED_COUNT = new AtomicLong();
    private static final AtomicLong ERROR_COUNT = new AtomicLong();
    private static final AtomicLong DISPATCH_REJECTED_COUNT = new AtomicLong();
    private static final AtomicLong DROPPED_COUNT = new AtomicLong();
    private static final AtomicLong COALESCED_COUNT = new AtomicLong();
    private static final int MAX_DISPATCH_RETRIES = 8;
    private final NativeDeNpcConstructionState _npc;
    private final NativeDeChatData _npcChat;
    private final AtomicBoolean _runPending = new AtomicBoolean();

    public NativeDeChatTimer(NativeDeNpcConstructionState nativeDeNpcConstructionState, NativeDeChatData nativeDeChatData) {
        this._npc = nativeDeNpcConstructionState;
        this._npcChat = nativeDeChatData;
    }

    @Override
    public void run() {
        if (!this._runPending.compareAndSet(false, true)) {
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.COALESCED_COUNT").incrementAndGet();
            return;
        }
        this.dispatchRunSequence(0);
    }

    private void dispatchRunSequence(int n) {
        if (NativeDeChatScope.Pool.get().tryExecute(new RunSequenceTask())) {
            return;
        }
        NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.DISPATCH_REJECTED_COUNT").incrementAndGet();
        NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.REJECTED_COUNT").incrementAndGet();
        if (NativeDeChatScope.Pool.get().isShutdown() || n >= 8) {
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.DROPPED_COUNT").incrementAndGet();
            this._runPending.set(false);
            NativeDeChatScope.log().warn("L1NpcChatTimer NPC worker unavailable; dropping sequence attempt={}", (Object)n);
            return;
        }
        try {
            NativeDeChatScope.Clock.get().aiSchedule(() -> this.dispatchRunSequence(n + 1), Math.min(500L, 25L << Math.min(4, n)));
        }
        catch (RuntimeException runtimeException) {
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.REJECTED_COUNT").incrementAndGet();
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.DROPPED_COUNT").incrementAndGet();
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.ERROR_COUNT").incrementAndGet();
            this._runPending.set(false);
            NativeDeChatScope.log().warn("L1NpcChatTimer sequence retry clock rejected attempt={}", (Object)n, (Object)runtimeException);
        }
    }

    private void runChatSequence() {
        NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.RUN_COUNT").incrementAndGet();
        try {
            if (this._npc == null || this._npcChat == null) {
                NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.SKIP_COUNT").incrementAndGet();
                return;
            }
            if (NativeDeChatAccess.L1NpcInstance$getHiddenStatus(this._npc) != 0 || this._npc._destroyed) {
                NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.SKIP_COUNT").incrementAndGet();
                return;
            }
            int n = this._npcChat.getChatTiming();
            int n2 = this._npcChat.getChatInterval();
            boolean bl = this._npcChat.isShout();
            boolean bl2 = this._npcChat.isWorldChat();
            String string = this._npcChat.getChatId1();
            String string2 = this._npcChat.getChatId2();
            String string3 = this._npcChat.getChatId3();
            String string4 = this._npcChat.getChatId4();
            String string5 = this._npcChat.getChatId5();
            if (!string.equals("")) {
                this.chat(this._npc, n, string, bl, bl2);
            }
            if (!string2.equals("")) {
                this.scheduleChat("chat2", n, string2, bl, bl2, n2);
            }
            if (!string3.equals("")) {
                this.scheduleChat("chat3", n, string3, bl, bl2, 2L * (long)n2);
            }
            if (!string4.equals("")) {
                this.scheduleChat("chat4", n, string4, bl, bl2, 3L * (long)n2);
            }
            if (!string5.equals("")) {
                this.scheduleChat("chat5", n, string5, bl, bl2, 4L * (long)n2);
            }
        }
        catch (Throwable throwable) {
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.ERROR_COUNT").incrementAndGet();
            NativeDeChatScope.log().error(throwable.getLocalizedMessage(), throwable);
        }
    }

    private void scheduleChat(String string, int n, String string2, boolean bl, boolean bl2, long l) {
        NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.SCHEDULED_COUNT").incrementAndGet();
        Runnable runnable = () -> {
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.ACTIVE_COUNT").incrementAndGet();
            try {
                this.safeChat(n, string2, bl, bl2);
            }
            finally {
                NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.COMPLETED_COUNT").incrementAndGet();
                NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.ACTIVE_COUNT").decrementAndGet();
            }
        };
        this.scheduleChatClock(string, runnable, Math.max(0L, l), 0);
    }

    private void scheduleChatClock(String string, Runnable runnable, long l, int n) {
        try {
            ScheduledFuture<?> scheduledFuture = NativeDeChatScope.Clock.get().aiSchedule(() -> this.dispatchChat(string, runnable, n), l);
            if (scheduledFuture == null) {
                NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.REJECTED_COUNT").incrementAndGet();
                NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.ERROR_COUNT").incrementAndGet();
                NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.DROPPED_COUNT").incrementAndGet();
                NativeDeChatScope.log().warn("L1NpcChatTimer clock returned null: {} delay={} attempt={}", (Object)string, (Object)l, (Object)n);
            }
        }
        catch (RejectedExecutionException rejectedExecutionException) {
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.REJECTED_COUNT").incrementAndGet();
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.ERROR_COUNT").incrementAndGet();
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.DROPPED_COUNT").incrementAndGet();
            NativeDeChatScope.log().warn("L1NpcChatTimer clock rejected: {} attempt={}", (Object)string, (Object)n, (Object)rejectedExecutionException);
        }
        catch (RuntimeException runtimeException) {
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.REJECTED_COUNT").incrementAndGet();
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.ERROR_COUNT").incrementAndGet();
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.DROPPED_COUNT").incrementAndGet();
            NativeDeChatScope.log().warn("L1NpcChatTimer clock submit failed: {} delay={} attempt={}", (Object)string, (Object)l, (Object)n, (Object)runtimeException);
        }
    }

    private void dispatchChat(String string, Runnable runnable, int n) {
        if (NativeDeChatScope.Pool.get().tryExecute(runnable)) {
            return;
        }
        NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.DISPATCH_REJECTED_COUNT").incrementAndGet();
        NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.REJECTED_COUNT").incrementAndGet();
        if (NativeDeChatScope.Pool.get().isShutdown() || n >= 8) {
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.DROPPED_COUNT").incrementAndGet();
            NativeDeChatScope.log().warn("L1NpcChatTimer NPC worker unavailable; dropping {} attempt={}", (Object)string, (Object)n);
            return;
        }
        long l = Math.min(500L, 25L << Math.min(4, n));
        this.scheduleChatClock(string, runnable, l, n + 1);
    }

    private void safeChat(int n, String string, boolean bl, boolean bl2) {
        try {
            if (this._npc == null || this._npcChat == null || this._npc._destroyed) {
                NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.SKIP_COUNT").incrementAndGet();
                return;
            }
            if (NativeDeChatAccess.L1NpcInstance$getHiddenStatus(this._npc) != 0) {
                NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.SKIP_COUNT").incrementAndGet();
                return;
            }
            this.chat(this._npc, n, string, bl, bl2);
        }
        catch (Throwable throwable) {
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.ERROR_COUNT").incrementAndGet();
            NativeDeChatScope.log().error(throwable.getLocalizedMessage(), throwable);
        }
    }

    private void chat(NativeDeNpcConstructionState nativeDeNpcConstructionState, int n, String string, boolean bl, boolean bl2) {
        if (n == 0 && NativeDeChatAccess.L1NpcInstance$isDead(nativeDeNpcConstructionState)) {
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.SKIP_COUNT").incrementAndGet();
            return;
        }
        if (n == 1 && !NativeDeChatAccess.L1NpcInstance$isDead(nativeDeNpcConstructionState)) {
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.SKIP_COUNT").incrementAndGet();
            return;
        }
        if (n == 2 && NativeDeChatAccess.L1NpcInstance$isDead(nativeDeNpcConstructionState)) {
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.SKIP_COUNT").incrementAndGet();
            return;
        }
        NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.CHAT_COUNT").incrementAndGet();
        if (!bl) {
            NativeDeChatAccess.L1NpcInstance$broadcastPacketX8(nativeDeNpcConstructionState, new NativeDeChatChat(nativeDeNpcConstructionState, string));
        } else {
            NativeDeChatAccess.L1NpcInstance$wideBroadcastPacket(nativeDeNpcConstructionState, new NativeDeChatChatShouting(nativeDeNpcConstructionState, string));
        }
        if (bl2) {
            NativeDeChatWorld.get().broadcastPacketToAll(new NativeDeChatChatGlobal(nativeDeNpcConstructionState, string));
        }
    }

    public static String getMonitorLine() {
        return String.format("L1NpcChatTimer run=%d scheduled=%d active=%d done=%d chat=%d skip=%d rejected=%d dispatchRejected=%d coalesced=%d drop=%d error=%d", NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.RUN_COUNT").get(), NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.SCHEDULED_COUNT").get(), NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.ACTIVE_COUNT").get(), NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.COMPLETED_COUNT").get(), NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.CHAT_COUNT").get(), NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.SKIP_COUNT").get(), NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.REJECTED_COUNT").get(), NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.DISPATCH_REJECTED_COUNT").get(), NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.COALESCED_COUNT").get(), NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.DROPPED_COUNT").get(), NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.ERROR_COUNT").get());
    }

    private final class RunSequenceTask
    implements NativeDeChatScope.Pool.ShutdownDiscardable {
        private RunSequenceTask() {
        }

        @Override
        public void run() {
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.ACTIVE_COUNT").incrementAndGet();
            try {
                NativeDeChatTimer.this.runChatSequence();
            }
            finally {
                NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.COMPLETED_COUNT").incrementAndGet();
                NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.ACTIVE_COUNT").decrementAndGet();
                NativeDeChatTimer.this._runPending.set(false);
            }
        }

        @Override
        public void discardOnShutdown() {
            NativeDeChatScope.counter("com/lineage/server/model/L1NpcChatTimer.DROPPED_COUNT").incrementAndGet();
            NativeDeChatTimer.this._runPending.set(false);
        }
    }
}
