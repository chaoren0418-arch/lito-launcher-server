// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
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
import local.lito381.server.NativeCubeCastAction;
import local.lito381.server.NativeDeItemApplication;
import local.lito381.server.NativeItemWeaponCastAction;
import local.lito381.server.NativePetWorldLifetime;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativePlayerLifetime;
import local.lito381.server.NativePlayerVitalCompletions;
import local.lito381.server.NativeResurrectionMaterialAction;
import local.lito381.server.NativeReturnNatureMaterialAction;
import local.lito381.server.NativeReturnNatureTransfer;
import local.lito381.server.NativeRolePublication;
import local.lito381.server.NativeSelfCastAction;
import local.lito381.server.NativeSourcePlayerActionLock;
import local.lito381.server.Protocol;

final class NativeLightAction {
    private static final ThreadLocal<Batch> current = new ThreadLocal();
    private static final ThreadLocal<Ingress> ingress = new ThreadLocal();

    private static Ingress reserve(ClientSession clientSession) {
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        NativeDeItemApplication nativeDeItemApplication = (NativeDeItemApplication)CombatAccess.field(gameHub, "sourceItemApplication");
        NativePlayerVitalCompletions.Receipt receipt = NativePlayerVitalCompletions.reserve(clientSession);
        return receipt == null ? null : new Ingress(clientSession, binding, gameHub, nativeDeItemApplication, receipt, NativeCastTimeline.capture(clientSession));
    }

    static void dispatch(ClientSession clientSession, byte[] byArray) throws IOException {
        NativeCancellationNpcMaterialAction.dispatch(clientSession, byArray, () -> NativeReturnNatureMaterialAction.dispatch(clientSession, byArray, () -> NativeResurrectionMaterialAction.dispatch(clientSession, byArray, () -> NativeLightAction.dispatchResurrectionOriginal(clientSession, byArray))));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
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
            ingress.receipt.close();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void commit(ClientSession clientSession, BattleSpellCatalog.LightSpell lightSpell, Object object, boolean bl, long l) throws IOException {
        Ingress ingress;
        Ingress ingress2 = NativeLightAction.ingress.get();
        boolean bl2 = ingress2 == null || ingress2.pc != clientSession;
        Ingress ingress3 = ingress = bl2 ? NativeLightAction.reserve(clientSession) : ingress2;
        if (ingress == null) {
            return;
        }
        NativeLightAction.ingress.remove();
        NativeCastTimeline.Arrival arrival = NativeCastTimeline.detach();
        try {
            NativeSourcePlayerActionLock.run(clientSession, () -> {
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
                ingress.receipt.close();
            }
        }
    }

    static boolean timelineIngress(ClientSession clientSession) {
        Ingress ingress = NativeLightAction.ingress.get();
        return ingress != null && ingress.pc == clientSession;
    }

    static boolean admit(ClientSession clientSession, BattleSpellCatalog.LightSpell lightSpell) {
        Batch batch = current.get();
        if (batch == null || batch.pc != clientSession) {
            throw new IllegalStateException("missing actual Light batch");
        }
        return NativeCastTimeline.admit(clientSession, lightSpell, batch.arrival);
    }

    static void enterState(ClientSession clientSession) {
        clientSession.nativePlayerActionLock().lock();
    }

    static void exitState(ClientSession clientSession) {
        clientSession.nativePlayerActionLock().unlock();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void activate(ClientSession clientSession, int n, long l, int n2) {
        NativeLightAction.enterState(clientSession);
        try {
            clientSession.nativeLightActivate(n, l, n2);
        }
        finally {
            NativeLightAction.exitState(clientSession);
        }
    }

    static boolean cancel(ClientSession clientSession) {
        NativeLightAction.enterState(clientSession);
        try {
            boolean bl = clientSession.nativeLightCancel();
            return bl;
        }
        finally {
            NativeLightAction.exitState(clientSession);
        }
    }

    static void clear(ClientSession clientSession) {
        NativeLightAction.enterState(clientSession);
        try {
            clientSession.nativeLightClear();
        }
        finally {
            NativeLightAction.exitState(clientSession);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static int expire(ClientSession clientSession, long l) {
        NativeLightAction.enterState(clientSession);
        try {
            int n = clientSession.nativeLightExpire(l);
            return n;
        }
        finally {
            NativeLightAction.exitState(clientSession);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean honorPending(ClientSession clientSession, long l, long l2) {
        NativeLightAction.enterState(clientSession);
        try {
            boolean bl = clientSession.nativeLightHonorPending(l, l2);
            return bl;
        }
        finally {
            NativeLightAction.exitState(clientSession);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void storeReady(ClientSession clientSession, int n, long l) {
        NativeLightAction.enterState(clientSession);
        try {
            clientSession.nativeLightStoreReady(n, l);
        }
        finally {
            NativeLightAction.exitState(clientSession);
        }
    }

    static void publish(ClientSession clientSession, String string) throws IOException {
        Batch batch = current.get();
        if (batch != null && batch.pc == clientSession) {
            batch.outputs.add(() -> clientSession.nativeLightPublishReveal(string));
            return;
        }
        if (batch == null) {
            current.remove();
        }
        clientSession.nativeLightPublishReveal(string);
    }

    static void enterRespawn(ClientSession clientSession) {
        clientSession.nativePlayerActionLock().lock();
    }

    static void exitRespawn(ClientSession clientSession) {
        clientSession.nativePlayerActionLock().unlock();
        NativePlayerVitalCompletions.drain(clientSession);
    }

    private NativeLightAction() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void commitSelf(ClientSession clientSession, NativeSelfCastAction.Task task) throws IOException {
        Ingress ingress;
        Ingress ingress2 = NativeLightAction.ingress.get();
        boolean bl = ingress2 == null || ingress2.pc != clientSession;
        Ingress ingress3 = ingress = bl ? NativeLightAction.reserve(clientSession) : ingress2;
        if (ingress == null) {
            return;
        }
        NativeLightAction.ingress.remove();
        NativeCastTimeline.Arrival arrival = NativeCastTimeline.detach();
        try {
            try {
                NativeSourcePlayerActionLock.run(clientSession, () -> {
                    if (!ingress.binding.matches(clientSession) || ((Boolean)CombatAccess.field(clientSession, "sourceItemDisconnected")).booleanValue() || CombatAccess.field(ingress.hub, "sourceItemApplication") != ingress.application || ingress.application != null && ingress.application.closed) {
                        return;
                    }
                    Batch batch = new Batch(clientSession, ingress.arrival, ingress);
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
                ingress.receipt.close();
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
        if (NativePetWorldLifetime.captureOutput(clientSession, byArray, string)) {
            return true;
        }
        if (NativeReturnNatureMaterialAction.captureOutput(clientSession, byArray, string)) {
            return true;
        }
        byte[] byArray2 = (byte[])byArray.clone();
        batch.outputs.add(() -> {
            if (batch.target != null && (clientSession == batch.target || string.startsWith("S_EFFECT Water-Life target=") || string.startsWith("S_EFFECT immune-to-harm target=") || string.startsWith("S_EFFECT Armor Break target=") || string.contains("Weapon Break target=") || string.contains("Cancellation target=") || string.contains("Entangle target=") || string.contains("Erase Magic target=") || string.contains("Weakness target=") || string.contains("Disease target=") || string.contains("Silence target=") || string.contains("Decay Potion target=") || string.contains("Pollute Water target=") || string.contains("Striker Gale target=") || string.contains("shape-change target=") || string.contains("Earth Bind target=") || string.contains("resurrection target=") || string.contains("poison target=") || string.contains("Curse-Poison target=") || string.contains("paralysis target=") || string.contains("Curse-Paralysis target=") || string.contains("Blind target=") || string.contains("blind target=")) && !batch.targetBinding.matches(batch.target)) {
                return;
            }
            clientSession.nativeSelfOriginalSend(byArray2, string);
        });
        return true;
    }

    static boolean admitSelf(ClientSession clientSession, int n, int n2, int n3) {
        Batch batch = current.get();
        if (batch == null || batch.pc != clientSession || !batch.captureSelf) {
            throw new IllegalStateException("missing actual self cast batch");
        }
        return NativeCastTimeline.admitSelf(clientSession, n, n2, n3, batch.arrival);
    }

    static NativeCastMaterial.Cast material(ClientSession clientSession, int n) {
        Batch batch = current.get();
        if (batch == null || batch.pc != clientSession || !clientSession.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("material requires actual owned cast batch");
        }
        if (batch.material == null) {
            batch.material = new NativeCastMaterial.Cast(n);
        }
        if (batch.material.skill != n) {
            throw new IllegalStateException("material cast identity changed");
        }
        return batch.material;
    }

    static void materialOutput(ClientSession clientSession, byte[] byArray, String string) {
        Batch batch = current.get();
        if (batch == null || batch.pc != clientSession || !clientSession.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("material output requires actual owned cast batch");
        }
        byte[] byArray2 = (byte[])byArray.clone();
        batch.outputs.add(() -> clientSession.nativeSelfOriginalSend(byArray2, string));
    }

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

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void commitPair(ClientSession clientSession, ClientSession clientSession2, NativeSelfCastAction.Task task) throws IOException {
        Ingress ingress;
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession2);
        Ingress ingress2 = NativeLightAction.ingress.get();
        boolean bl = ingress2 == null || ingress2.pc != clientSession;
        Ingress ingress3 = ingress = bl ? NativeLightAction.reserve(clientSession) : ingress2;
        if (ingress == null) {
            return;
        }
        NativeLightAction.ingress.remove();
        NativeCastTimeline.Arrival arrival = NativeCastTimeline.detach();
        try {
            try {
                NativeSourcePlayerActionLock.runPairOrdered(clientSession, clientSession2, () -> {
                    if (!ingress.binding.matches(clientSession) || !binding.matches(clientSession2) || ((Boolean)CombatAccess.field(clientSession, "sourceItemDisconnected")).booleanValue() || ((Boolean)CombatAccess.field(clientSession2, "sourceItemDisconnected")).booleanValue() || CombatAccess.field(ingress.hub, "sourceItemApplication") != ingress.application || ingress.application != null && ingress.application.closed) {
                        return;
                    }
                    Batch batch = new Batch(clientSession, ingress.arrival, ingress);
                    batch.captureSelf = true;
                    batch.target = clientSession2;
                    batch.targetBinding = binding;
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
            if (ingress2 == null) {
                NativeLightAction.ingress.remove();
            } else {
                NativeLightAction.ingress.set(ingress2);
            }
            if (bl) {
                ingress.receipt.close();
            }
        }
    }

    static void deferScoped(NativeSelfCastAction.Task task) {
        Batch batch = current.get();
        if (batch == null || !batch.pc.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("scope output requires owned cast");
        }
        batch.outputs.add(() -> {
            if (batch.accepted.binding.matches(batch.pc) && (batch.target == null || batch.targetBinding.matches(batch.target))) {
                task.run();
            }
        });
    }

    static void finishScoped(Completion completion) {
        Batch batch = current.get();
        if (batch == null || !batch.pc.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("scope completion requires owned cast");
        }
        batch.completions.add(completion);
    }

    static boolean admitPair(ClientSession clientSession, ClientSession clientSession2, int n, int n2, int n3) {
        Batch batch = current.get();
        if (batch == null || batch.pc != clientSession || batch.target != clientSession2 || !batch.targetBinding.matches(clientSession2)) {
            return false;
        }
        if (!clientSession.nativePlayerActionLock().isHeldByCurrentThread() || !clientSession2.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("target cast requires both ordered action locks");
        }
        return NativeLightAction.admitSelf(clientSession, n, n2, n3);
    }

    private record Ingress(ClientSession pc, NativePlayerCommit.Binding binding, GameHub hub, NativeDeItemApplication application, NativePlayerVitalCompletions.Receipt receipt, NativeCastTimeline.Arrival arrival) {
    }

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

        Batch(ClientSession clientSession, NativeCastTimeline.Arrival arrival, Ingress ingress) {
            this.pc = clientSession;
            this.arrival = arrival;
            this.accepted = ingress;
        }

        /*
         * WARNING - Removed back jump from a try to a catch block - possible behaviour change.
         * Unable to fully structure code
         * Enabled aggressive block sorting
         * Enabled unnecessary exception pruning
         * Enabled aggressive exception aggregation
         */
        @Override
        public void run() {
            block26: {
                var1_1 = false;
                var2_2 = null;
                try lbl-1000:
                // 2 sources

                {
                    for (Output var4_6 : this.outputs) {
                        if (!NativeRolePublication.allowPublication(this.pc)) {
                            var5_9 = null;
                            break block26;
                        }
                        ** GOTO lbl-1000
                    }
                    ** GOTO lbl66
                }
                catch (IOException var3_4) {
                    try {
                        var2_2 = var3_4;
                        throw new UncheckedIOException(var3_4);
                        catch (Error | RuntimeException var3_5) {
                            var2_2 = var3_5;
                            throw var3_5;
                        }
                    }
                    catch (Throwable var9_19) {
                        var10_20 = null;
                        for (Completion var12_22 : this.completions) {
                            try {
                                var12_22.finish(var1_1 != false && this.accepted.binding.matches(this.pc) != false && (this.target == null || this.targetBinding.matches(this.target) != false), var2_2);
                            }
                            catch (Throwable var13_24) {
                                if (var10_20 == null) {
                                    var10_20 = var13_24;
                                    continue;
                                }
                                if (var10_20 == var13_24) continue;
                                var10_20.addSuppressed(var13_24);
                            }
                        }
                        if (var10_20 == null) throw var9_19;
                        if (var2_2 != null) {
                            if (var10_20 == var2_2) throw var9_19;
                            var2_2.addSuppressed(var10_20);
                            throw var9_19;
                        }
                        if (var10_20 instanceof RuntimeException) {
                            var11_21 = (RuntimeException)var10_20;
                            throw var11_21;
                        }
                        if (var10_20 instanceof Error == false) throw new IllegalStateException("cast completion failed", var10_20);
                        var12_23 = (Error)var10_20;
                        throw var12_23;
                    }
                }
            }
            for (Completion var7_16 : this.completions) {
                try {
                    var7_16.finish(var1_1 != false && this.accepted.binding.matches(this.pc) != false && (this.target == null || this.targetBinding.matches(this.target) != false), var2_2);
                }
                catch (Throwable var8_18) {
                    if (var5_10 == null) {
                        var5_11 = var8_18;
                        continue;
                    }
                    if (var5_10 == var8_18) continue;
                    var5_10.addSuppressed(var8_18);
                }
            }
            if (var5_10 == null) return;
            if (var2_2 != null) {
                if (var5_10 == var2_2) return;
                var2_2.addSuppressed((Throwable)var5_10);
                return;
            }
            if (var5_10 instanceof RuntimeException) {
                var6_14 = (RuntimeException)var5_10;
                throw var6_14;
            }
            if (var5_10 instanceof Error == false) throw new IllegalStateException("cast completion failed", (Throwable)var5_10);
            var7_17 = (Error)var5_10;
            throw var7_17;
lbl-1000:
            // 1 sources

            {
                var4_6.run();
                ** GOTO lbl-1000
lbl66:
                // 1 sources

                var1_1 = true;
                var3_3 = null;
            }
            for (Completion var5_12 : this.completions) {
                try {
                    var5_12.finish(var1_1 != false && this.accepted.binding.matches(this.pc) != false && (this.target == null || this.targetBinding.matches(this.target) != false), var2_2);
                }
                catch (Throwable var6_15) {
                    if (var3_3 == null) {
                        var3_3 = var6_15;
                        continue;
                    }
                    if (var3_3 == var6_15) continue;
                    var3_3.addSuppressed(var6_15);
                }
            }
            if (var3_3 == null) return;
            if (var2_2 != null) {
                if (var3_3 == var2_2) return;
                var2_2.addSuppressed((Throwable)var3_3);
                return;
            }
            if (var3_3 instanceof RuntimeException) {
                var4_8 = (RuntimeException)var3_3;
                throw var4_8;
            }
            if (var3_3 instanceof Error == false) throw new IllegalStateException("cast completion failed", (Throwable)var3_3);
            var5_13 = (Error)var3_3;
            throw var5_13;
        }
    }

    private static interface Output {
        public void run() throws IOException;
    }

    static interface Completion {
        public void finish(boolean var1, Throwable var2);
    }
}
