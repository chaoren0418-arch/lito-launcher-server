// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeAcceptedState;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativePlayerVitalCompletions;
import local.lito381.server.NativeProcHealingPlayer;
import local.lito381.server.Protocol;

final class NativeEffectCompletions {
    private static final ThreadLocal<NativePlayerCommit.Frame> defensePublication = new ThreadLocal();

    static boolean defensePublishing(NativePlayerCommit.Frame frame) {
        return defensePublication.get() == frame;
    }

    static void state(NativePlayerVitalCompletions.Receipt receipt, NativePlayerCommit.Frame frame, Object object, boolean bl, String string) {
        NativePlayerVitalCompletions.enqueueLocked(receipt, new State(frame, object, bl, string));
    }

    static void healing(NativePlayerVitalCompletions.Receipt receipt, ClientSession clientSession, NativePlayerCommit.Binding binding, ClientSession clientSession2, NativePlayerCommit.Binding binding2, int n, int n2, Object object) {
        NativePlayerVitalCompletions.enqueueLocked(receipt, new Healing(clientSession, binding, clientSession2, binding2, n, n2, object));
    }

    private NativeEffectCompletions() {
    }

    private record State(NativePlayerCommit.Frame frame, Object released, boolean sleep, String reason) implements Runnable
    {
        @Override
        public void run() {
            NativeAcceptedState.completeState(this.frame, this.released, this.sleep, new StateOutput(this.frame.pc, this.sleep, this.reason));
        }
    }

    private record Healing(ClientSession actor, NativePlayerCommit.Binding caster, ClientSession receiver, NativePlayerCommit.Binding target, int skill, int graphics, Object snapshot) implements Runnable
    {
        @Override
        public void run() {
            NativePlayerCommit.Frame frame = defensePublication.get();
            NativePlayerCommit.Frame frame2 = NativePlayerCommit.frame();
            if (frame2 == null) {
                defensePublication.remove();
            } else {
                defensePublication.set(frame2);
            }
            try {
                NativeProcHealingPlayer.completeHealing(this.actor, this.caster, this.receiver, this.target, this.skill, this.graphics, this.snapshot);
            }
            finally {
                if (frame == null) {
                    defensePublication.remove();
                } else {
                    defensePublication.set(frame);
                }
            }
        }
    }

    private record StateOutput(ClientSession pc, boolean sleep, String reason) implements Runnable
    {
        @Override
        public void run() {
            if (this.sleep) {
                NativePlayerCommit.safeAuxSend(this.pc, Protocol.paralysis(Protocol.ParalysisMode.SLEEP_RELEASE), "S_PARALYSE mode=11 reason=" + this.reason);
            } else {
                CombatAccess.call(this.pc, "publishInvisibilityReveal", "(Ljava/lang/String;)V", this.reason);
            }
        }
    }
}
