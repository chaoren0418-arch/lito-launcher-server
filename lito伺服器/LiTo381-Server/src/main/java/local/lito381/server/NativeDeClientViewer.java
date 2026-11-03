// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Set;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatNpcDamageQueue;
import local.lito381.server.CombatPoisonAtomic;
import local.lito381.server.GameHub;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeDeletionState;
import local.lito381.server.NativeDeKnownState;
import local.lito381.server.NativeDePerceptionOutput;
import local.lito381.server.NativeDePerceptionPacket;
import local.lito381.server.NativeDeVisibilityOutput;
import local.lito381.server.NativeDeVisibilityRuntime;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativePlayerLifetime;
import local.lito381.server.Protocol;

final class NativeDeClientViewer
extends NativeDeKnownState {
    static final String FIELD = "sourceDeKnownViewer";
    final GameHub hub;
    final NativePlayerLifetime life;
    final NativePlayerCommit.Binding binding;
    volatile boolean retired;

    private NativeDeClientViewer(GameHub gameHub, NativePlayerCommit.Binding binding) {
        super(NativeDeClientViewer.objectId(binding.object()));
        this.hub = gameHub;
        this.life = NativePlayerLifetime.of(gameHub);
        this.binding = binding;
    }

    private static int objectId(long l) {
        if (l <= 0L || l > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("source viewer object ID outside unsigned32");
        }
        return (int)l;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static NativeDeClientViewer of(ClientSession clientSession) {
        BattleTimedStatus battleTimedStatus;
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus = CombatPoisonAtomic.status(clientSession);
        synchronized (battleTimedStatus2) {
            if (!NativePlayerLifetime.of(gameHub).admits()) {
                throw new IllegalStateException("viewer application closed");
            }
            NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
            if (!Set.of("BATTLE", "WORLD").contains(binding.phase())) {
                throw new IllegalStateException("viewer is outside a native scene");
            }
            NativeDeClientViewer nativeDeClientViewer = (NativeDeClientViewer)CombatAccess.field(battleTimedStatus, FIELD);
            if (nativeDeClientViewer != null && !nativeDeClientViewer.retired && nativeDeClientViewer.binding.equals(binding) && nativeDeClientViewer.hub == gameHub) {
                return nativeDeClientViewer;
            }
            if (nativeDeClientViewer != null) {
                nativeDeClientViewer.retire();
            }
            NativeDeClientViewer nativeDeClientViewer2 = new NativeDeClientViewer(gameHub, binding);
            CombatAccess.set(battleTimedStatus, FIELD, nativeDeClientViewer2);
            return nativeDeClientViewer2;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void retire(BattleTimedStatus battleTimedStatus) {
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            NativeDeClientViewer nativeDeClientViewer = (NativeDeClientViewer)CombatAccess.field(battleTimedStatus, FIELD);
            if (nativeDeClientViewer != null) {
                CombatAccess.set(battleTimedStatus, FIELD, null);
                try {
                    nativeDeClientViewer.retire();
                }
                catch (Throwable throwable) {
                    CombatNpcDamageQueue.failed(throwable);
                }
            }
        }
    }

    static void retireClient(ClientSession clientSession) {
        if (clientSession == null) {
            return;
        }
        BattleTimedStatus battleTimedStatus = CombatPoisonAtomic.status(clientSession);
        if (battleTimedStatus != null) {
            NativeDeClientViewer.retire(battleTimedStatus);
        }
    }

    static void closeHub(GameHub gameHub) {
        for (ClientSession clientSession : gameHub.connectedSessionsSnapshot()) {
            NativeDeClientViewer.retireClient(clientSession);
        }
    }

    private void retire() {
        this.withKnownLock(() -> {
            this.retired = true;
            try {
                NativeDeWorldContext.detach(this);
            }
            finally {
                this.removeAllKnownObjects();
            }
            return null;
        });
    }

    private ClientSession current() {
        if (this.retired || this.life.mode == NativePlayerLifetime.Mode.STOPPED) {
            return null;
        }
        for (ClientSession clientSession : this.hub.connectedSessionsSnapshot()) {
            if (!this.binding.matches(clientSession)) continue;
            return clientSession;
        }
        return null;
    }

    @Override
    public boolean tryAddKnownObject(NativeDeDeletionPorts.ObjectRef objectRef) {
        ClientSession clientSession = this.current();
        if (clientSession == null) {
            return false;
        }
        if (objectRef instanceof NativeDeDeletionState) {
            NativeDeDeletionState nativeDeDeletionState = (NativeDeDeletionState)objectRef;
            if (nativeDeDeletionState.deletionOwner.timer.life != this.life) {
                return false;
            }
        }
        return this.withKnownLock(() -> !this.retired && this.life.admits() && this.binding.matchesFast(clientSession) && super.tryAddKnownObject(objectRef));
    }

    @Override
    NativeDeDeletionPorts.OtherList get_otherList() {
        throw new UnsupportedOperationException("native illusory ownership registry is not connected");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    void sendPackets(NativeDeDeletionPorts.Packet packet) {
        Object object;
        block12: {
            block11: {
                if (NativeDeVisibilityOutput.accepts(packet)) {
                    NativeDeVisibilityOutput.send(this, packet);
                    return;
                }
                if (packet instanceof NativeDePerceptionPacket) {
                    NativeDePerceptionOutput.send(this, (NativeDePerceptionPacket)packet);
                    return;
                }
                if (!(packet instanceof NativeDeDeletionPorts.RemoveObject)) break block11;
                NativeDeDeletionPorts.RemoveObject removeObject = (NativeDeDeletionPorts.RemoveObject)packet;
                object = removeObject.actor;
                if (object instanceof NativeDeDeletionState) break block12;
            }
            throw new IllegalArgumentException("viewer accepts source NPC remove packets only");
        }
        NativeDeDeletionState nativeDeDeletionState = (NativeDeDeletionState)object;
        if (nativeDeDeletionState.deletionOwner.timer.life != this.life) {
            throw new IllegalArgumentException("foreign application remove packet");
        }
        object = this.current();
        if (object == null) {
            throw new IllegalStateException("stale native viewer");
        }
        byte[] byArray = Protocol.removeObject(Integer.toUnsignedLong(nativeDeDeletionState.getId()));
        Object object2 = object;
        synchronized (object2) {
            if (this.retired || this.life.mode == NativePlayerLifetime.Mode.STOPPED || !this.binding.matchesFast((ClientSession)object) || CombatAccess.field(CombatPoisonAtomic.status((ClientSession)object), FIELD) != this) {
                throw new IllegalStateException("stale native viewer publication");
            }
            Object[] objectArray = new Object[]{byArray, "S_REMOVE_OBJECT source-De deletion id=" + Integer.toUnsignedLong(nativeDeDeletionState.getId())};
            NativeDeVisibilityRuntime.requirePublication(this);
            CombatAccess.call(object, "sendEncryptedFrame", "([BLjava/lang/String;)V", objectArray);
        }
    }

    public void updateObject() {
        NativeDeVisibilityRuntime.update(this);
    }
}
