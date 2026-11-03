// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatDatabase;
import local.lito381.server.GameStateStore;
import local.lito381.server.NativeCastMaterialPolicy;
import local.lito381.server.NativeCubeCastAction;
import local.lito381.server.NativeDeItemApplication;
import local.lito381.server.NativeDeItemInventory;
import local.lito381.server.NativeDeItemTransaction;
import local.lito381.server.NativeDeWorldKinds;
import local.lito381.server.NativeLightAction;
import local.lito381.server.NativeMaterialItem;
import local.lito381.server.NativePlayerLifetime;
import local.lito381.server.NativeReuseLog;
import local.lito381.server.NativeSourceMaterialPlan;
import local.lito381.server.Protocol;
import local.lito381.server.RuntimeInventory;

final class NativeCastMaterial {
    static boolean retained(ClientSession clientSession, NativePlayerLifetime nativePlayerLifetime) {
        return Thread.holdsLock(nativePlayerLifetime.admission) && clientSession.nativePlayerActionLock().isHeldByCurrentThread() && NativeLightAction.retainedMaterial(clientSession);
    }

    static boolean active(ClientSession clientSession, Cast cast) {
        return cast != null && cast.transaction && cast.operation != null && NativeCastMaterial.retained(clientSession, NativePlayerLifetime.of(clientSession)) && cast.owner != null && cast.owner.active && cast.owner.matches(clientSession) && CombatAccess.field(clientSession, "sourceItemOwner") == cast.owner && (clientSession.nativeDeInventory(0) == cast.live || clientSession.nativeDeInventory(0) == cast.staged);
    }

    static boolean ownerLifetime(NativePlayerLifetime nativePlayerLifetime, NativeDeItemApplication nativeDeItemApplication, ClientSession clientSession) {
        if (nativePlayerLifetime.admits()) {
            return true;
        }
        Cast cast = NativeLightAction.currentMaterial(clientSession);
        return NativeCastMaterial.active(clientSession, cast) && cast.owner.application.get() == nativeDeItemApplication && nativeDeItemApplication.life == nativePlayerLifetime && !nativeDeItemApplication.closed;
    }

    static boolean adoptLifetime(NativePlayerLifetime nativePlayerLifetime, NativeDeItemApplication.Owner owner, RuntimeInventory runtimeInventory) {
        if (nativePlayerLifetime.admits()) {
            return true;
        }
        ClientSession clientSession = (ClientSession)owner.session.get();
        if (clientSession == null) {
            return false;
        }
        Cast cast = NativeLightAction.currentMaterial(clientSession);
        return NativeCastMaterial.active(clientSession, cast) && cast.owner == owner && cast.staged == runtimeInventory && NativePlayerLifetime.of(clientSession) == nativePlayerLifetime;
    }

    static void enterTransaction(ClientSession clientSession, NativeDeItemTransaction.Operation<?> operation) throws IOException {
        Cast cast = NativeLightAction.currentMaterial(clientSession);
        if (cast == null) {
            return;
        }
        if (cast.transaction) {
            throw new IOException("nested item transaction cannot inherit material cast authority");
        }
        if (cast.operation == operation) {
            if (!NativeCastMaterial.retained(clientSession, NativePlayerLifetime.of(clientSession))) {
                throw new IOException("material cast receipt retired");
            }
            cast.transaction = true;
        }
    }

    static void exitTransaction(ClientSession clientSession, NativeDeItemTransaction.Operation<?> operation) {
        Cast cast = NativeLightAction.currentMaterial(clientSession);
        if (cast != null && cast.operation == operation) {
            cast.transaction = false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean commit(ClientSession clientSession, int n) {
        Cast cast = NativeLightAction.material(clientSession, n);
        int n2 = CombatDatabase.value(0, n, "NativeCastMaterial", "itemConsumeId");
        int n3 = CombatDatabase.value(0, n, "NativeCastMaterial", "itemConsumeCount");
        if (cast.resolved) {
            return n2 == 0 || cast.bypass || cast.consumed;
        }
        try {
            if (n2 == 0 || NativeCastMaterialPolicy.gm(clientSession)) {
                cast.bypass = true;
                if (cast.effect == null) {
                    RuntimeInventory runtimeInventory;
                    cast.resolved = true;
                    cast.before = runtimeInventory = NativeDeItemTransaction.wireCopy(clientSession.nativeDeInventory(0));
                    cast.after = runtimeInventory;
                    return true;
                }
            }
            if (!cast.bypass && n3 <= 0) {
                return NativeCastMaterial.reject(clientSession);
            }
            if (cast.placement) {
                if (!NativeCastMaterial.active(clientSession, cast) || !Thread.holdsLock(CombatAccess.field(clientSession, "state"))) {
                    throw new IOException("cube material lost its exact original operation");
                }
                return NativeCastMaterial.consumeLocked(clientSession, cast, n2, n3) || NativeCastMaterial.reject(clientSession);
            }
            NativePlayerLifetime nativePlayerLifetime = NativePlayerLifetime.of(clientSession);
            Object object = nativePlayerLifetime.admission;
            synchronized (object) {
                boolean bl;
                if (!nativePlayerLifetime.admits() && !NativeCastMaterial.retained(clientSession, nativePlayerLifetime)) {
                    return NativeCastMaterial.reject(clientSession);
                }
                cast.live = clientSession.nativeDeInventory(0);
                cast.owner = NativeDeItemApplication.publicationOwner(clientSession);
                NativeDeItemTransaction.Operation<Boolean> operation = () -> NativeCastMaterial.consumeLocked(clientSession, cast, n2, n3);
                cast.operation = operation;
                try {
                    bl = NativeDeItemTransaction.call(clientSession, operation);
                }
                finally {
                    cast.operation = null;
                    cast.live = null;
                    cast.staged = null;
                    cast.owner = null;
                    cast.transaction = false;
                }
                return bl || NativeCastMaterial.reject(clientSession);
            }
        }
        catch (IOException iOException) {
            NativeReuseLog.getLogger(NativeCastMaterial.class).error("material memory transaction failed", iOException);
            return NativeCastMaterial.reject(clientSession);
        }
    }

    static boolean reject(ClientSession clientSession) {
        NativeLightAction.materialOutput(clientSession, Protocol.messageCode(299, new String[0]), "S_MESSAGE_CODE Source-required-material-rejected=299");
        return false;
    }

    static RuntimeInventory previous(ClientSession clientSession) {
        Cast cast = NativeLightAction.currentMaterial(clientSession);
        if (cast == null || !cast.resolved || !clientSession.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("material not committed");
        }
        return cast.before;
    }

    static RuntimeInventory next(ClientSession clientSession) {
        Cast cast = NativeLightAction.currentMaterial(clientSession);
        if (cast == null || !cast.resolved || !clientSession.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("material not committed");
        }
        return cast.after;
    }

    static boolean rejectPlacement(ClientSession clientSession, Cast cast) {
        cast.placementBlocked = true;
        return NativeCastMaterial.reject(clientSession);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    static <T> T withPlacement(ClientSession clientSession, int n, NativeDeItemTransaction.Operation<T> operation, T t) throws IOException {
        if (!NativeCubeCastAction.supported(n) || !clientSession.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("unreviewed cube placement scope");
        }
        Cast cast = NativeLightAction.material(clientSession, n);
        if (cast.transaction || cast.resolved) {
            throw new IOException("cube placement cannot inherit another material transaction");
        }
        try {
            NativeCastMaterialPolicy.gm(clientSession);
            NativePlayerLifetime nativePlayerLifetime = NativePlayerLifetime.of(clientSession);
            Object object = nativePlayerLifetime.admission;
            synchronized (object) {
                if (!nativePlayerLifetime.admits() && !NativeCastMaterial.retained(clientSession, nativePlayerLifetime)) {
                    NativeCastMaterial.rejectPlacement(clientSession, cast);
                    return t;
                }
                cast.live = clientSession.nativeDeInventory(0);
                cast.owner = NativeDeItemApplication.publicationOwner(clientSession);
                NativeDeItemTransaction.Operation<Object> operation2 = () -> {
                    GameStateStore gameStateStore;
                    if (!NativeCastMaterial.active(clientSession, cast)) {
                        throw new IOException("cube canonical owner retired before original placement");
                    }
                    GameStateStore gameStateStore2 = gameStateStore = (GameStateStore)CombatAccess.field(clientSession, "state");
                    synchronized (gameStateStore2) {
                        Object t;
                        cast.placement = true;
                        try {
                            t = operation.call();
                            cast.placement = false;
                        }
                        catch (Throwable throwable) {
                            cast.placement = false;
                            throw throwable;
                        }
                        return t;
                    }
                };
                cast.operation = operation2;
                Object object2 = NativeDeItemTransaction.call(clientSession, operation2);
                return (T)object2;
                finally {
                    cast.operation = null;
                    cast.live = null;
                    cast.staged = null;
                    cast.owner = null;
                    cast.transaction = false;
                    cast.placement = false;
                }
            }
        }
        catch (IOException iOException) {
            NativeReuseLog.getLogger(NativeCastMaterial.class).error("cube placement memory transaction failed", iOException);
            NativeCastMaterial.rejectPlacement(clientSession, cast);
            return t;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean consumeLocked(ClientSession clientSession, Cast cast, int n, int n2) throws IOException {
        Object object;
        Iterator<NativeSourceMaterialPlan.Debit> iterator;
        Object object2;
        Object object3;
        if (!NativeCastMaterial.active(clientSession, cast)) {
            throw new IOException("material canonical owner retired before transaction");
        }
        RuntimeInventory runtimeInventory = clientSession.nativeDeInventory(0);
        ArrayList<NativeMaterialItem> arrayList = new ArrayList<NativeMaterialItem>();
        Map map = runtimeInventory.nativeDeItems();
        Object object4 = runtimeInventory.items().iterator();
        while (object4.hasNext()) {
            NativeDeWorldKinds.Item item;
            object3 = object4.next();
            if (map instanceof NativeDeItemInventory) {
                object2 = (NativeDeItemInventory)map;
                item = ((NativeDeItemInventory)object2).actor(((RuntimeInventory.Item)object3).objectId());
            } else {
                item = null;
            }
            iterator = item;
            arrayList.add(new ItemView((RuntimeInventory.Item)object3, (NativeDeWorldKinds.Item)((Object)iterator), (int)clientSession.playerObjectId()));
        }
        cast.staged = object4 = runtimeInventory.copy();
        if (!cast.bypass) {
            object3 = NativeSourceMaterialPlan.plan(arrayList, (int)clientSession.playerObjectId(), n, n2);
            if (!((NativeSourceMaterialPlan.Result)object3).allowed()) {
                return false;
            }
            iterator = ((NativeSourceMaterialPlan.Result)object3).debits().iterator();
            while (iterator.hasNext()) {
                object2 = iterator.next();
                object = ((RuntimeInventory)object4).item(((NativeSourceMaterialPlan.Debit)object2).objectId());
                if (object != null && (long)((RuntimeInventory.Item)object).quantity() == ((NativeSourceMaterialPlan.Debit)object2).oldCount() && ((RuntimeInventory)object4).remove(((NativeSourceMaterialPlan.Debit)object2).objectId(), ((NativeSourceMaterialPlan.Debit)object2).count())) continue;
                throw new IOException("material canonical plan changed");
            }
        }
        if (cast.effect != null) {
            cast.effectResult = cast.effect.apply((RuntimeInventory)object4);
        }
        if ((iterator = ((RuntimeInventory)object4).nativeDeItems()) instanceof NativeDeItemInventory) {
            object3 = (NativeDeItemInventory)((Object)iterator);
            try {
                ((NativeDeItemInventory)object3).validateReplacement(runtimeInventory);
            }
            catch (IllegalStateException illegalStateException) {
                throw new IOException("material inventory copy is stale", illegalStateException);
            }
        }
        object3 = NativeDeItemTransaction.wireCopy(runtimeInventory);
        iterator = NativeDeItemTransaction.wireCopy((RuntimeInventory)object4);
        object2 = (GameStateStore)CombatAccess.field(clientSession, "state");
        object = (String)CombatAccess.call(clientSession, "stateCharacter", "()Ljava/lang/String;", new Object[0]);
        long l = ((Number)CombatAccess.field(clientSession, "shopPoints")).longValue();
        Object object5 = object2;
        synchronized (object5) {
            GameStateStore.Economy economy = ((GameStateStore)object2).economy((String)object);
            ((GameStateStore)object2).saveEconomyState((String)object, l, ((RuntimeInventory)object4).catalogCounts(), ((RuntimeInventory)object4).catalogItemStates());
            try {
                NativeDeItemApplication.replace(clientSession, (RuntimeInventory)object4, 0);
            }
            catch (Error | RuntimeException throwable) {
                try {
                    ((GameStateStore)object2).saveEconomyState((String)object, economy.points(), economy.inventory(), economy.itemStates());
                }
                catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
        }
        cast.before = object3;
        cast.after = iterator;
        cast.consumed = true;
        cast.resolved = true;
        return true;
    }

    private NativeCastMaterial() {
    }

    static final class Cast {
        Effect effect;
        boolean effectResult;
        final int skill;
        boolean resolved;
        boolean consumed;
        boolean bypass;
        boolean transaction;
        boolean placement;
        boolean placementBlocked;
        RuntimeInventory before;
        RuntimeInventory after;
        RuntimeInventory live;
        RuntimeInventory staged;
        NativeDeItemApplication.Owner owner;
        NativeDeItemTransaction.Operation<?> operation;

        Cast(int n) {
            this.skill = n;
        }
    }

    static interface Effect {
        public boolean apply(RuntimeInventory var1) throws IOException;
    }

    private record ItemView(RuntimeInventory.Item view, NativeDeWorldKinds.Item source, int owner) implements NativeMaterialItem
    {
        @Override
        public Object getItem() {
            return this.source == null ? this.view.definition() : this.source.getItem();
        }

        @Override
        public int getItemId() {
            return this.source == null ? (int)this.view.definition().catalogId() : this.source.getItemId();
        }

        @Override
        public int get_char_objid() {
            return this.source == null ? this.owner : this.source.get_char_objid();
        }

        @Override
        public boolean isEquipped() {
            return this.source == null ? this.view.equipped() : this.source.isEquipped();
        }

        @Override
        public boolean isSealed() {
            return this.source == null ? this.view.sealed() : this.source.isSealed();
        }

        @Override
        public long getCount() {
            return this.source == null ? (long)this.view.quantity() : this.source.getCount();
        }

        @Override
        public boolean isStackable() {
            return this.source == null ? this.view.definition().stackable() : this.source.isStackable();
        }

        @Override
        public int getEnchantLevel() {
            return this.source == null ? this.view.enchantLevel() : this.source.getEnchantLevel();
        }

        @Override
        public int getId() {
            return this.source == null ? (int)this.view.objectId() : this.source.getId();
        }
    }
}
