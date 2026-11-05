// ported from candidate 50635f7e; three-way baseline: base-0.6.71.jar and src/main
// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/resurrection-material/build-repro/inputs/original/NativeLightAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.GameHub;
import local.lito381.server.NativeAreaMaterialAction;
import local.lito381.server.NativeCancellationNpcMaterialAction;
import local.lito381.server.NativeCastMaterial;
import local.lito381.server.NativeCastTimeline;
import local.lito381.server.NativeCompanionTargets;
import local.lito381.server.NativeCubeCastAction;
import local.lito381.server.NativeDeItemApplication;
import local.lito381.server.NativeItemWeaponCastAction;
import local.lito381.server.NativePairedCastSafety;
import local.lito381.server.NativePetWorldLifetime;
import local.lito381.server.NativePlayerActionScope;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativePlayerLifetime;
import local.lito381.server.NativePlayerVitalCompletions;
import local.lito381.server.NativeResurrectionMaterialAction;
import local.lito381.server.NativeReturnNatureMaterialAction;
import local.lito381.server.NativeReturnNatureTransfer;
import local.lito381.server.NativeRolePublication;
import local.lito381.server.NativeSelfCastAction;
import local.lito381.server.NativeSourceControlledRoleFrame;
import local.lito381.server.NativeSourceManualScope;
import local.lito381.server.NativeSourceMassTeleportExit;
import local.lito381.server.NativeSourceMaterialMain;
import local.lito381.server.NativeSourceMaterialPairGuard;
import local.lito381.server.NativeSourceMaterialPairIngress;
import local.lito381.server.NativeSourcePlayerActionLock;
import local.lito381.server.NativeSourceSelfFinalFrame;
import local.lito381.server.NativeSourceTargetIconBatch;
import local.lito381.server.NativeWeaponBreakTargetLifecycle;
import local.lito381.server.Protocol;
import java.io.*;import java.util.*;
/** Actual Light commit uses Source action ordering; original reveal and original output tail retain FIFO. */
final class NativeLightAction {
 interface Output {void run()throws IOException;}
 interface Completion {void finish(boolean accepted,Throwable failure);}
    private static final class Batch
    implements Runnable {
        boolean captureSelf;
        ClientSession target;
        NativePlayerCommit.Binding targetBinding;
        NativeCastMaterial.Cast material;
        final ClientSession pc;
        final NativeCastTimeline.Arrival arrival;
        final List<Output> outputs = new ArrayList<Output>();
        final List<Completion> completions = new ArrayList<Completion>();
        final Ingress accepted;
        NativePlayerVitalCompletions.Receipt targetReceipt;
        transient Object nativeSourceTargetIconOutputs;
        transient Object nativeSourceSelfFinalFirst;

        Batch(ClientSession clientSession, NativeCastTimeline.Arrival arrival, Ingress ingress) {
            this.pc = clientSession;
            this.arrival = arrival;
            this.accepted = ingress;
        }

        @Override
        public void run() {
            if (this.targetReceipt == null) {
                this.emit();
            } else {
                this.targetReceipt.run(this::emit);
            }
        }

        /*
         * WARNING - Removed back jump from a try to a catch block - possible behaviour change.
         * Unable to fully structure code
         * Enabled aggressive block sorting
         * Enabled unnecessary exception pruning
         * Enabled aggressive exception aggregation
         */
        private void nativeSourceBatchOriginalEmit() throws IOException {
            boolean accepted = false;
            Throwable failure = null;
            try {
                for (Output output : this.outputs) {
                    if (!(NativeSourceTargetIconBatch.independent(output, this))) {
                    if (!NativeSourceControlledRoleFrame.current(this.accepted.binding, this.pc)
                            || !NativeRolePublication.allowPublication(this.pc)
                            || this.target != null && !NativeWeaponBreakTargetLifecycle.targetPublication(this.targetBinding, this.target, this.material)) return;
                    output.run();

                    }
}
                accepted = true;
            } catch (IOException error) {
                failure = error;
                throw new UncheckedIOException(error);
            } catch (RuntimeException | Error error) {
                failure = error;
                throw error;
            } finally {
                Throwable completionFailure = null;
                NativeSourceTargetIconBatch.rescue(this);
                for (Completion completion : this.completions) {
                    try {
                        completion.finish(accepted && NativeSourceControlledRoleFrame.current(this.accepted.binding, this.pc)
                                && (this.target == null || NativeSourceControlledRoleFrame.current(this.targetBinding, this.target)), failure);
                    } catch (Throwable error) {
                        if (completionFailure == null) completionFailure = error;
                        else if (completionFailure != error) completionFailure.addSuppressed(error);
                    }
                }
                if (completionFailure != null) {
                    if (failure != null) {
                        if (completionFailure != failure) failure.addSuppressed(completionFailure);
                    } else if (completionFailure instanceof RuntimeException error) {
                        throw error;
                    } else if (completionFailure instanceof Error error) {
                        throw error;
                    } else throw new IllegalStateException("cast completion failed", completionFailure);
                }
            }
        }

        private void emit() {
            try {
            try {
                this.nativeSourceBatchOriginalEmit();
            }
            catch (Throwable throwable) {
                NativeSourceTargetIconBatch.finish(this, throwable);
                throw throwable;
            }
            NativeSourceTargetIconBatch.finish(this, null);
            } catch (Throwable failure) {
                NativeLightAction.<RuntimeException>rethrowCandidateFailure(failure);
            }
        }
    }
 private static final ThreadLocal<Batch> current=new ThreadLocal<>();
@AsRecord(components={"pc","binding","hub","application","receipt","arrival"})
    private static final class Ingress
    {
        private final ClientSession pc;
        private final NativePlayerCommit.Binding binding;
        private final GameHub hub;
        private final NativeDeItemApplication application;
        private final NativePlayerVitalCompletions.Receipt receipt;
        private final NativeCastTimeline.Arrival arrival;
        transient Object nativeSourceSelfIngressFirst;

        private Ingress(ClientSession clientSession, NativePlayerCommit.Binding binding, GameHub gameHub, NativeDeItemApplication nativeDeItemApplication, NativePlayerVitalCompletions.Receipt receipt, NativeCastTimeline.Arrival arrival) {
            this.pc = clientSession;
            this.binding = binding;
            this.hub = gameHub;
            this.application = nativeDeItemApplication;
            this.receipt = receipt;
            this.arrival = arrival;
            NativeSourceSelfFinalFrame.initialize(this);
        }

        public ClientSession pc() {
            return this.pc;
        }

        public NativePlayerCommit.Binding binding() {
            return this.binding;
        }

        public GameHub hub() {
            return this.hub;
        }

        public NativeDeItemApplication application() {
            return this.application;
        }

        public NativePlayerVitalCompletions.Receipt receipt() {
            return this.receipt;
        }

        public NativeCastTimeline.Arrival arrival() {
            return this.arrival;
        }
    }
 private static final ThreadLocal<Ingress> ingress=new ThreadLocal<>();
    private static Ingress reserve(ClientSession clientSession) {
        if (!NativeSourceMaterialMain.ready(clientSession)) {
            return null;
        }
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        NativeDeItemApplication nativeDeItemApplication = (NativeDeItemApplication)CombatAccess.field(gameHub, "sourceItemApplication");
        NativePlayerVitalCompletions.Receipt receipt = NativePlayerVitalCompletions.reserve(clientSession);
        return receipt == null ? null : new Ingress(clientSession, binding, gameHub, nativeDeItemApplication, receipt, NativeCastTimeline.capture(clientSession));
    }
    static void dispatch(ClientSession clientSession, byte[] byArray) throws IOException {
        if (!NativeSourceMaterialMain.ready(clientSession)) {
            return;
        }
        NativeWeaponBreakTargetLifecycle.dispatch(clientSession, byArray, () -> NativeReturnNatureMaterialAction.dispatch(clientSession, byArray, () -> NativeResurrectionMaterialAction.dispatch(clientSession, byArray, () -> NativeLightAction.dispatchResurrectionOriginal(clientSession, byArray))));
    }
    static void commit(ClientSession clientSession, BattleSpellCatalog.LightSpell lightSpell, Object object, boolean bl, long l) throws IOException {
        Ingress ingress;
        Ingress ingress2 = NativeLightAction.ingress.get();
        boolean bl2 = ingress2 == null || ingress2.pc != clientSession;
        ingress = bl2 ? NativeLightAction.reserve(clientSession) : ingress2;
        if (ingress == null) {
            return;
        }
        NativeLightAction.ingress.remove();
        NativeCastTimeline.Arrival arrival = NativeCastTimeline.detach();
        try {
            NativeSourcePlayerActionLock.run(clientSession, () -> {
                if (!NativeSourceMaterialMain.locked(ingress, clientSession)) {
                    return;
                }
                if (!ingress.binding.matches(clientSession) || ((Boolean)CombatAccess.field(clientSession, "sourceItemDisconnected")).booleanValue() || CombatAccess.field(ingress.hub, "sourceItemApplication") != ingress.application || ingress.application != null && ingress.application.closed) {
                    return;
                }
                Batch batch = new Batch(clientSession, ingress.arrival, ingress);
                Batch batch2 = current.get();
                current.set(batch);
                try {
                    Integer n = clientSession.nativeLightCommit(lightSpell, object, System.nanoTime());
                    if (n != null) {
                        batch.outputs.add(() -> clientSession.nativeLightTail(lightSpell, bl, n));
                    }
                }
                finally {
                    if (batch2 == null) {
                        current.remove();
                    } else {
                        current.set(batch2);
                    }
                    if (!batch.outputs.isEmpty() || !batch.completions.isEmpty()) {
                        NativePlayerVitalCompletions.enqueueLocked(ingress.receipt, batch);
                    }
                }
            });
        }
        finally {
            NativeCastTimeline.restore(arrival);
            if (ingress2 == null) {
                NativeLightAction.ingress.remove();
            } else {
                NativeLightAction.ingress.set(ingress2);
            }
            if (bl2) {
                NativePlayerVitalCompletions.Receipt receipt = ingress.receipt;
                NativeSourceSelfFinalFrame.retire(ingress);
                receipt.close();
            }
        }
    }
 static boolean timelineIngress(ClientSession pc){var accepted=ingress.get();return accepted!=null&&accepted.pc==pc;}
 static boolean admit(ClientSession pc,BattleSpellCatalog.LightSpell spell){var batch=current.get();if(batch==null||batch.pc!=pc)throw new IllegalStateException("missing actual Light batch");return NativeCastTimeline.admit(pc,spell,batch.arrival);}
 static void enterState(ClientSession pc){pc.nativePlayerActionLock().lock();}
 static void exitState(ClientSession pc){pc.nativePlayerActionLock().unlock();}
    static void activate(ClientSession clientSession, int n, long l, int n2) {
        NativePlayerActionScope.Ticket ticket = NativeSourceManualScope.enterState(clientSession);
        try {
            clientSession.nativeLightActivate(n, l, n2);
        }
        catch (Throwable throwable) {
            NativeSourceManualScope.exitState(clientSession, ticket, throwable);
            throw throwable;
        }
        NativeSourceManualScope.exitState(clientSession, ticket, null);
    }
    static boolean cancel(ClientSession clientSession) {
        boolean bl;
        NativePlayerActionScope.Ticket ticket = NativeSourceManualScope.enterState(clientSession);
        try {
            bl = clientSession.nativeLightCancel();
        }
        catch (Throwable throwable) {
            NativeSourceManualScope.exitState(clientSession, ticket, throwable);
            throw throwable;
        }
        NativeSourceManualScope.exitState(clientSession, ticket, null);
        return bl;
    }
    static void clear(ClientSession clientSession) {
        NativePlayerActionScope.Ticket ticket = NativeSourceManualScope.enterState(clientSession);
        try {
            clientSession.nativeLightClear();
        }
        catch (Throwable throwable) {
            NativeSourceManualScope.exitState(clientSession, ticket, throwable);
            throw throwable;
        }
        NativeSourceManualScope.exitState(clientSession, ticket, null);
    }
    static int expire(ClientSession clientSession, long l) {
        int n;
        NativePlayerActionScope.Ticket ticket = NativeSourceManualScope.enterState(clientSession);
        try {
            n = clientSession.nativeLightExpire(l);
        }
        catch (Throwable throwable) {
            NativeSourceManualScope.exitState(clientSession, ticket, throwable);
            throw throwable;
        }
        NativeSourceManualScope.exitState(clientSession, ticket, null);
        return n;
    }
    static boolean honorPending(ClientSession clientSession, long l, long l2) {
        boolean bl;
        NativePlayerActionScope.Ticket ticket = NativeSourceManualScope.enterState(clientSession);
        try {
            bl = clientSession.nativeLightHonorPending(l, l2);
        }
        catch (Throwable throwable) {
            NativeSourceManualScope.exitState(clientSession, ticket, throwable);
            throw throwable;
        }
        NativeSourceManualScope.exitState(clientSession, ticket, null);
        return bl;
    }
    static void storeReady(ClientSession clientSession, int n, long l) {
        NativePlayerActionScope.Ticket ticket = NativeSourceManualScope.enterState(clientSession);
        try {
            clientSession.nativeLightStoreReady(n, l);
        }
        catch (Throwable throwable) {
            NativeSourceManualScope.exitState(clientSession, ticket, throwable);
            throw throwable;
        }
        NativeSourceManualScope.exitState(clientSession, ticket, null);
    }
 static void publish(ClientSession pc,String reason)throws IOException{
  var batch=current.get();if(batch!=null&&batch.pc==pc){batch.outputs.add(()->pc.nativeLightPublishReveal(reason));return;}if(batch==null)current.remove();pc.nativeLightPublishReveal(reason);
 }
 // Match the Source stable-before-status order before the original PC monitor is entered.
 static void enterRespawn(ClientSession pc){pc.nativePlayerActionLock().lock();}
 static void exitRespawn(ClientSession pc){pc.nativePlayerActionLock().unlock();NativePlayerVitalCompletions.drain(pc);}
 private NativeLightAction(){}

    static void commitSelf(ClientSession clientSession, NativeSelfCastAction.Task originalTask) throws IOException {
        Ingress ingress = NativeLightAction.ingress.get();
        boolean bl = ingress == null || ingress.pc != clientSession;
        Ingress ingress2 = bl ? NativeLightAction.reserve(clientSession) : ingress;
        NativeSelfCastAction.Task task = NativeSourceSelfFinalFrame.task(clientSession, originalTask, ingress2);
        if (ingress2 == null) {
            return;
        }
        NativeLightAction.ingress.remove();
        NativeCastTimeline.Arrival arrival = NativeCastTimeline.detach();
        try {
            try {
                NativeSourcePlayerActionLock.run(clientSession, () -> {
                    if (!NativeSourceMaterialMain.locked(ingress2, clientSession)) {
                        return;
                    }
                    if (!NativeSourceSelfFinalFrame.matches(ingress.binding, clientSession, task) || ((Boolean)CombatAccess.field(clientSession, "sourceItemDisconnected")).booleanValue() || CombatAccess.field(ingress.hub, "sourceItemApplication") != ingress.application || ingress.application != null && ingress.application.closed) {
                        return;
                    }
                    Batch batch = new Batch(clientSession, ingress.arrival, ingress2);
                    NativeSourceSelfFinalFrame.install(batch, task);
                    batch.captureSelf = true;
                    Batch batch2 = current.get();
                    current.set(batch);
                    try {
                        task.run();
                    }
                    catch (IOException iOException) {
                        throw new UncheckedIOException(iOException);
                    }
                    finally {
                        if (batch2 == null) {
                            NativeSourceSelfFinalFrame.produced(batch);
                            current.remove();
                        } else {
                            NativeSourceSelfFinalFrame.produced(batch);
                            current.set(batch2);
                        }
                        if (!batch.outputs.isEmpty() || !batch.completions.isEmpty()) {
                            NativePlayerVitalCompletions.enqueueLocked(ingress.receipt, batch);
                        }
                    }
                });
            }
            catch (UncheckedIOException uncheckedIOException) {
                throw uncheckedIOException.getCause();
            }
        }
        finally {
            NativeCastTimeline.restore(arrival);
            if (ingress == null) {
                NativeLightAction.ingress.remove();
            } else {
                NativeLightAction.ingress.set(ingress);
            }
            if (bl) {
                NativePlayerVitalCompletions.Receipt receipt = ingress2.receipt;
                NativeSourceSelfFinalFrame.retire(ingress2);
                receipt.close();
            }
        }
    }
    static boolean deferSelfSend(ClientSession clientSession, byte[] byArray, String string) {
        if (NativeAreaMaterialAction.captureOutput(clientSession, byArray, string)) {
            return true;
        }
        if (NativeCancellationNpcMaterialAction.captureOutput(clientSession, byArray, string)) {
            return true;
        }
        Batch batch = current.get();
        if (NativeSourceSelfFinalFrame.capture(batch, clientSession, byArray, string)) {
            return true;
        }
        if (batch == null) {
            current.remove();
            return false;
        }
        if (!batch.captureSelf) {
            return false;
        }
        if (!batch.pc.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("self cast output capture requires actual action lock");
        }
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
        if (NativePetWorldLifetime.captureOutput(clientSession, byArray, string)) {
            return true;
        }
        if (NativeReturnNatureMaterialAction.captureOutput(clientSession, byArray, string)) {
            return true;
        }
        byte[] byArray2 = (byte[])byArray.clone();
        batch.outputs.add(() -> {
            if (!binding.matches(clientSession) || ((Boolean)CombatAccess.field(clientSession, "sourceItemDisconnected")).booleanValue()) {
                return;
            }
            if (batch.target != null && (clientSession == batch.target || string.startsWith("S_EFFECT Water-Life target=") || string.startsWith("S_EFFECT immune-to-harm target=") || string.startsWith("S_EFFECT Armor Break target=") || string.contains("Weapon Break target=") || string.contains("Cancellation target=") || string.contains("Entangle target=") || string.contains("Erase Magic target=") || string.contains("Weakness target=") || string.contains("Disease target=") || string.contains("Silence target=") || string.contains("Decay Potion target=") || string.contains("Pollute Water target=") || string.contains("Striker Gale target=") || string.contains("shape-change target=") || string.contains("Earth Bind target=") || string.contains("resurrection target=") || string.contains("poison target=") || string.contains("Curse-Poison target=") || string.contains("paralysis target=") || string.contains("Curse-Paralysis target=") || string.contains("Blind target=") || string.contains("blind target=")) && !batch.targetBinding.matches(batch.target)) {
                return;
            }
            clientSession.nativeSelfOriginalSend(byArray2, string);
        });
        return true;
    }
 static boolean admitSelf(ClientSession pc,int skill,int action,int reuse){
  var batch=current.get();if(batch==null||batch.pc!=pc||!batch.captureSelf)throw new IllegalStateException("missing actual self cast batch");
  return NativeCastTimeline.admitSelf(pc,skill,action,reuse,batch.arrival);
 }

 static NativeCastMaterial.Cast material(ClientSession pc,int skill){var batch=current.get();if(batch==null||batch.pc!=pc||!pc.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("material requires actual owned cast batch");if(batch.material==null)batch.material=new NativeCastMaterial.Cast(skill);if(batch.material.skill!=skill)throw new IllegalStateException("material cast identity changed");return batch.material;}
 static void materialOutput(ClientSession pc,byte[] plain,String note){var batch=current.get();if(batch==null||batch.pc!=pc||!pc.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("material output requires actual owned cast batch");byte[] snapshot=plain.clone();batch.outputs.add(()->pc.nativeSelfOriginalSend(snapshot,note));}
    static NativeCastMaterial.Cast currentMaterial(ClientSession clientSession) {
        NativeCastMaterial.Cast cast = NativeReturnNatureTransfer.material(clientSession);
        if (cast != null) {
            return cast;
        }
        Batch batch = current.get();
        if (batch == null) {
            current.remove();
            return null;
        }
        return batch.pc == clientSession ? batch.material : null;
    }
    static boolean retainedMaterial(ClientSession clientSession) {
        return NativeCompanionTargets.retained(clientSession);
    }
    static void commitPair(ClientSession clientSession, ClientSession clientSession2, NativeSelfCastAction.Task originalTask) throws IOException {
        NativePlayerVitalCompletions.Receipt receipt;
        NativeSelfCastAction.Task task = NativeSourceMaterialPairIngress.prepare(clientSession, clientSession2, originalTask);
        if (!NativeSourceMaterialPairIngress.enter(task)) {
            return;
        }
        NativePlayerCommit.Binding binding = NativeSourceMaterialPairIngress.binding(clientSession2, task);
        receipt = clientSession2 == clientSession ? null : NativeSourceMaterialPairIngress.target(clientSession2, task);
        if (clientSession2 != clientSession && receipt == null) {
            return;
        }
        try {
            Ingress ingress;
            Ingress ingress2 = NativeLightAction.ingress.get();
            boolean bl = ingress2 == null || ingress2.pc != clientSession;
            ingress = bl ? (Ingress)NativeSourceMaterialPairIngress.ingress(clientSession, task) : ingress2;
            if (ingress == null) {
                return;
            }
            NativeLightAction.ingress.remove();
            NativeCastTimeline.Arrival arrival = NativeCastTimeline.detach();
            try {
                try {
                    NativePairedCastSafety.run(clientSession, clientSession2, () -> {
                        if (!NativeSourceMaterialMain.locked(ingress, clientSession)) {
                            return;
                        }
                        if (!NativeSourceMaterialPairGuard.matches(ingress.binding, clientSession, task) || !NativeSourceMaterialPairGuard.matches(binding, clientSession2, task) || CombatAccess.field(clientSession2, "hub") != ingress.hub || ((Boolean)CombatAccess.field(clientSession, "sourceItemDisconnected")).booleanValue() || ((Boolean)CombatAccess.field(clientSession2, "sourceItemDisconnected")).booleanValue() || CombatAccess.field(ingress.hub, "sourceItemApplication") != ingress.application || ingress.application != null && ingress.application.closed) {
                            return;
                        }
                        Batch batch = new Batch(clientSession, ingress.arrival, ingress);
                        batch.captureSelf = true;
                        batch.target = clientSession2;
                        batch.targetBinding = binding;
                        batch.targetReceipt = receipt;
                        Batch batch2 = current.get();
                        current.set(batch);
                        try {
                            task.run();
                        }
                        catch (IOException iOException) {
                            throw new UncheckedIOException(iOException);
                        }
                        finally {
                            if (batch2 == null) {
                                current.remove();
                            } else {
                                current.set(batch2);
                            }
                            if (!batch.outputs.isEmpty() || !batch.completions.isEmpty()) {
                                NativePairedCastSafety.transfer(clientSession, clientSession2, receipt);
                                try {
                                    NativePlayerVitalCompletions.enqueueLocked(ingress.receipt, batch);
                                }
                                catch (Error | RuntimeException throwable) {
                                    NativePairedCastSafety.abandon(receipt);
                                    throw throwable;
                                }
                            }
                        }
                    });
                }
                catch (UncheckedIOException uncheckedIOException) {
                    throw uncheckedIOException.getCause();
                }
            }
            finally {
                NativeCastTimeline.restore(arrival);
                if (ingress2 == null) {
                    NativeLightAction.ingress.remove();
                } else {
                    NativeLightAction.ingress.set(ingress2);
                }
                if (bl) {
                    NativePlayerVitalCompletions.Receipt receipt3 = ingress.receipt;
                    NativeSourceSelfFinalFrame.retire(ingress);
                    receipt3.close();
                }
            }
        }
        finally {
            if (receipt != null) {
                receipt.close();
            }
        }
    }
 static boolean admitPair(ClientSession pc,ClientSession target,int skill,int action,int reuse){
  var batch=current.get();if(batch==null||batch.pc!=pc||batch.target!=target||!batch.targetBinding.matches(target))return false;
  if(!pc.nativePlayerActionLock().isHeldByCurrentThread()||!target.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("target cast requires both ordered action locks");
  return admitSelf(pc,skill,action,reuse);
 }
 static void finishScoped(Completion completion){
  var batch=current.get();
  if(batch==null||!batch.pc.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("scope completion requires owned cast");
  batch.completions.add(completion);
 }
    static void deferScoped(NativeSelfCastAction.Task task) {
        Batch batch = current.get();
        if (batch == null || !batch.pc.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("scope output requires owned cast");
        }
        batch.outputs.add(() -> {
            if (batch.accepted.binding.matches(batch.pc)) {
                task.run();
            }
        });
    }

    static ClientSession currentMaterialProducer() {
        Batch batch = current.get();
        if (batch == null) {
            return null;
        }
        return batch.pc;
    }
    static void deferWeaponTargetOutput(NativeSelfCastAction.Task task) {
        Batch batch = current.get();
        if (batch == null || !batch.pc.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("scope output requires owned cast");
        }
        batch.outputs.add(() -> {
            if (batch.accepted.binding.matches(batch.pc)) {
                task.run();
            }
        });
    }
    private static void dispatchResurrectionOriginal(ClientSession clientSession, byte[] byArray) throws IOException {
        int n = Protocol.spellSelector(byArray).skillId();
        String string = String.valueOf(CombatAccess.field(clientSession, "phase"));
        if (n != 165 && n != 69 && n != 66 && n != 76 && n != 161 && n != 167 && n != 2 && !NativeSelfCastAction.supported(n) && !NativeCubeCastAction.supported(n) && !NativeItemWeaponCastAction.supported(n) && n != 73 && n != 170 && n != 68 && n != 112 && n != 29 && n != 152 && n != 153 && n != 47 && n != 56 && n != 58 && n != 63 && n != 64 && n != 71 && n != 173 && n != 174 && n != 44 && n != 27 && n != 36 && n != 41 && n != 145 && n != 67 && n != 157 && n != 40 && n != 103 && n != 61 && n != 75 && n != 11 && n != 33 && n != 51 && n != 154 && n != 162 || !string.equals("BATTLE") && !string.equals("WORLD") || clientSession.selectedCharacterName().isBlank()) {
            clientSession.nativeLightSpellDispatch(byArray);
            return;
        }
        Ingress ingress = NativeLightAction.reserve(clientSession);
        if (ingress == null) {
            return;
        }
        Ingress ingress2 = NativeLightAction.ingress.get();
        NativeLightAction.ingress.set(ingress);
        try {
            clientSession.nativeLightSpellDispatch(byArray);
        }
        finally {
            if (ingress2 == null) {
                NativeLightAction.ingress.remove();
            } else {
                NativeLightAction.ingress.set(ingress2);
            }
            NativePlayerVitalCompletions.Receipt receipt = ingress.receipt;
            NativeSourceSelfFinalFrame.retire(ingress);
            receipt.close();
        }
    }
    static boolean nativeCompanionOriginalRetainedMaterial(ClientSession clientSession) {
        Batch batch = current.get();
        if (batch == null) {
            current.remove();
            return false;
        }
        if (batch.pc != clientSession) {
            return batch.target == clientSession && batch.targetBinding.matches(clientSession) && NativeReturnNatureTransfer.retained(clientSession);
        }
        if (!clientSession.nativePlayerActionLock().isHeldByCurrentThread()) {
            return false;
        }
        Ingress ingress = batch.accepted;
        return ingress.pc == clientSession && ingress.binding.matches(clientSession) && ingress.receipt.pc == clientSession && !ingress.receipt.released.get() && ingress.receipt.life == NativePlayerLifetime.of(clientSession) && CombatAccess.field(ingress.hub, "sourceItemApplication") == ingress.application && (ingress.application == null || !ingress.application.closed) && (Boolean)CombatAccess.field(clientSession, "sourceItemDisconnected") == false;
    }
    static Object nativeSourceGroundPlacementBatch() {
        return current.get();
    }
    static Object nativeSourceMassTeleportIngress(ClientSession clientSession) {
        return NativeSourceMassTeleportExit.ingress(clientSession, ingress.get());
    }
    static Ingress nativeSourceMaterialPairIngress(ClientSession clientSession, NativeSelfCastAction.Task task) {
        if (!NativeSourceMaterialMain.ready(clientSession)) {
            return null;
        }
        NativePlayerCommit.Binding binding = NativeSourceMaterialPairIngress.binding(clientSession, task);
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        NativeDeItemApplication nativeDeItemApplication = (NativeDeItemApplication)CombatAccess.field(gameHub, "sourceItemApplication");
        NativePlayerVitalCompletions.Receipt receipt = NativeSourceMaterialPairIngress.reserve(clientSession, task);
        return receipt == null ? null : new Ingress(clientSession, binding, gameHub, nativeDeItemApplication, receipt, NativeCastTimeline.capture(clientSession));
    }
    static Ingress nativeSourceMaterialPairOriginalReserve(ClientSession clientSession) {
        if (!NativeSourceMaterialMain.ready(clientSession)) {
            return null;
        }
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        NativeDeItemApplication nativeDeItemApplication = (NativeDeItemApplication)CombatAccess.field(gameHub, "sourceItemApplication");
        NativePlayerVitalCompletions.Receipt receipt = NativePlayerVitalCompletions.reserve(clientSession);
        return receipt == null ? null : new Ingress(clientSession, binding, gameHub, nativeDeItemApplication, receipt, NativeCastTimeline.capture(clientSession));
    }
    static boolean nativeSourceNpcHpDefer(NativeSelfCastAction.Task task) {
        Batch batch = current.get();
        if (batch != null && batch.captureSelf) {
            NativeLightAction.deferScoped(task);
            return true;
        }
        return false;
    }
    static boolean nativeSourceSelfIngressValid(ClientSession clientSession) {
        return NativeSourceSelfFinalFrame.entry(clientSession, ingress.get());
    }
    static boolean validTargets(ClientSession clientSession) {
        Batch batch = current.get();
        if (batch == null || batch.pc != clientSession || batch.target == null) {
            return true;
        }
        return batch.accepted.binding.matches(clientSession) && batch.targetBinding.matches(batch.target) && clientSession.nativePlayerActionLock().isHeldByCurrentThread() && batch.target.nativePlayerActionLock().isHeldByCurrentThread() && CombatAccess.field(batch.target, "hub") == batch.accepted.hub && CombatAccess.field(batch.accepted.hub, "sourceItemApplication") == batch.accepted.application && (batch.accepted.application == null || !batch.accepted.application.closed) && (Boolean)CombatAccess.field(clientSession, "sourceItemDisconnected") == false && (Boolean)CombatAccess.field(batch.target, "sourceItemDisconnected") == false;
    }
    // Candidate ASM omits checked exception declarations; pending controller approval.
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void rethrowCandidateFailure(Throwable failure) throws E {
        throw (E) failure;
    }
}
