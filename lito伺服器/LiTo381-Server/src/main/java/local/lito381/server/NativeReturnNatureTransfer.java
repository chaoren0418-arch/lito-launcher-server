// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import local.lito381.server.BattleCompanion;
import local.lito381.server.BattleGroundItems;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.DurableCommit;
import local.lito381.server.GameHub;
import local.lito381.server.GameStateStore;
import local.lito381.server.NativeCastMaterial;
import local.lito381.server.NativeDeItemApplication;
import local.lito381.server.NativeDeItemInventory;
import local.lito381.server.NativeDeItemTransaction;
import local.lito381.server.NativeLightAction;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativePlayerLifetime;
import local.lito381.server.NativeReturnNatureMaterialAction;
import local.lito381.server.NativeReuseLog;
import local.lito381.server.RuntimeInventory;

final class NativeReturnNatureTransfer {
    private static final ThreadLocal<Scope> current = new ThreadLocal();

    static NativeCastMaterial.Cast material(ClientSession clientSession) {
        Scope scope = current.get();
        if (scope == null) {
            current.remove();
            return null;
        }
        return scope.owner == clientSession ? scope.cast : null;
    }

    static boolean retained(ClientSession clientSession) {
        Scope scope = current.get();
        if (scope == null) {
            current.remove();
            return false;
        }
        return scope.owner == clientSession && scope.caster != clientSession && scope.binding.matches(clientSession) && clientSession.nativePlayerActionLock().isHeldByCurrentThread() && scope.caster.nativePlayerActionLock().isHeldByCurrentThread() && NativeReturnNatureMaterialAction.caster(clientSession, scope.pet) == scope.caster && scope.cast.operation != null && NativeLightAction.retainedMaterial(scope.caster);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Loose catch block
     * Enabled aggressive exception aggregation
     */
    static boolean release(ClientSession clientSession, BattleCompanion battleCompanion, BattleCompanion.Snapshot snapshot, int n) throws IOException {
        if (battleCompanion == null || snapshot == null) {
            return false;
        }
        ClientSession clientSession2 = NativeReturnNatureMaterialAction.caster(clientSession, battleCompanion);
        if (clientSession2 == null) {
            return false;
        }
        if (snapshot.kind() != BattleCompanion.Kind.SUMMON || battleCompanion.carriedItems().isEmpty()) {
            return NativeReturnNatureTransfer.original(clientSession, battleCompanion, snapshot, n);
        }
        NativePlayerLifetime nativePlayerLifetime = NativePlayerLifetime.of(clientSession);
        Object object = nativePlayerLifetime.admission;
        synchronized (object) {
            if (!NativeLightAction.retainedMaterial(clientSession2)) {
                return false;
            }
            NativeCastMaterial.Cast cast = new NativeCastMaterial.Cast(145);
            Scope scope = new Scope(clientSession2, clientSession, battleCompanion, NativePlayerCommit.Binding.capture(clientSession), cast);
            Scope scope2 = current.get();
            current.set(scope);
            try {
                boolean bl;
                cast.live = clientSession.nativeDeInventory(0);
                cast.owner = NativeDeItemApplication.publicationOwner(clientSession);
                NativeDeItemTransaction.Operation<Boolean> operation = () -> {
                    if (!NativeCastMaterial.active(clientSession, cast) || !scope.binding.matches(clientSession)) {
                        throw new IOException("companion return owner retired");
                    }
                    BattleCompanion battleCompanion2 = battleCompanion;
                    synchronized (battleCompanion2) {
                        if (NativeReturnNatureMaterialAction.caster(clientSession, battleCompanion) != clientSession2 || !battleCompanion.snapshot().active()) {
                            return false;
                        }
                        NativeReturnNatureTransfer.transfer(clientSession, battleCompanion, cast);
                        return NativeReturnNatureTransfer.original(clientSession, battleCompanion, snapshot, n);
                    }
                };
                cast.operation = operation;
                try {
                    bl = NativeDeItemTransaction.call(clientSession, operation);
                    cast.operation = null;
                    cast.transaction = false;
                    cast.live = null;
                    cast.staged = null;
                    cast.owner = null;
                }
                catch (IOException iOException) {
                    boolean bl2;
                    block19: {
                        NativeReuseLog.getLogger(NativeReturnNatureTransfer.class).error("Return to Nature owner transfer failed; summon retained", iOException);
                        bl2 = false;
                        cast.operation = null;
                        cast.transaction = false;
                        cast.live = null;
                        cast.staged = null;
                        cast.owner = null;
                        if (scope2 == null) {
                            current.remove();
                            break block19;
                        }
                        current.set(scope2);
                    }
                    return bl2;
                    {
                        catch (Throwable throwable) {
                            cast.operation = null;
                            cast.transaction = false;
                            cast.live = null;
                            cast.staged = null;
                            cast.owner = null;
                            throw throwable;
                        }
                    }
                }
                return bl;
            }
            finally {
                if (scope2 == null) {
                    current.remove();
                } else {
                    current.set(scope2);
                }
            }
        }
    }

    private static boolean original(ClientSession clientSession, BattleCompanion battleCompanion, BattleCompanion.Snapshot snapshot, int n) throws IOException {
        try {
            return (Boolean)CombatAccess.call(clientSession, "releaseCompanionToNature", "(Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;I)Z", battleCompanion, snapshot, n);
        }
        catch (RuntimeException runtimeException) {
            Throwable throwable = runtimeException;
            while (throwable.getCause() != null) {
                throwable = throwable.getCause();
            }
            if (throwable instanceof IOException) {
                IOException iOException = (IOException)throwable;
                throw iOException;
            }
            throw runtimeException;
        }
    }

    private static void transfer(ClientSession clientSession, BattleCompanion battleCompanion, NativeCastMaterial.Cast cast) throws IOException {
        Object object;
        Object object2;
        RuntimeInventory runtimeInventory = clientSession.nativeDeInventory(0);
        Object object4 = runtimeInventory.copy();
        cast.staged = object4;
        List<RuntimeInventory.Item> list = battleCompanion.carriedItems();
        ArrayList<Overflow> arrayList = new ArrayList<Overflow>();
        boolean bl = false;
        for (RuntimeInventory.Item object32 : list) {
            boolean bl2;
            object2 = ((RuntimeInventory)object4).copy();
            cast.staged = object2;
            boolean bl3 = ((RuntimeInventory)object2).add(object32, (long)object32.quantity());
            boolean bl4 = bl2 = bl3 && (Boolean)CombatAccess.call(clientSession, "permitsInventoryAddition", "(Llocal/lito381/server/RuntimeInventory;Llocal/lito381/server/InventoryCatalog$Definition;J)Z", object4, object32.definition(), object32.quantity()) != false;
            if (bl3 && bl2) {
                object4 = object2;
                bl = true;
            } else {
                arrayList.add(new Overflow(object32, bl3));
            }
            cast.staged = object4;
        }
        Map map = ((RuntimeInventory)object4).nativeDeItems();
        if (map instanceof NativeDeItemInventory) {
            object = (NativeDeItemInventory)map;
            ((NativeDeItemInventory)object).validateReplacement(runtimeInventory);
        }
        object = NativeDeItemTransaction.wireCopy(runtimeInventory);
        RuntimeInventory runtimeInventory2 = NativeDeItemTransaction.wireCopy((RuntimeInventory)object4);
        object2 = (GameStateStore)CombatAccess.field(clientSession, "state");
        String string = (String)CombatAccess.call(clientSession, "stateCharacter", "()Ljava/lang/String;", new Object[0]);
        long l = ((Number)CombatAccess.field(clientSession, "shopPoints")).longValue();
        Object object3 = object4;
        boolean bl5 = bl;
        Object[] objectArray = new Object[arrayList.size()];
        DurableCommit durableCommit = () -> NativeReturnNatureTransfer.lambda$transfer$1((GameStateStore)object2, string, bl5, l, (RuntimeInventory)object3, clientSession, list, battleCompanion);
        NativeReturnNatureTransfer.reserveGround(clientSession, battleCompanion.snapshot(), arrayList, objectArray, 0, durableCommit);
        CombatAccess.call(clientSession, "sendInventoryDelta", "(Llocal/lito381/server/RuntimeInventory;Llocal/lito381/server/RuntimeInventory;)V", object, runtimeInventory2);
        boolean bl6 = String.valueOf(CombatAccess.field(clientSession, "phase")).equals("WORLD");
        for (int i = 0; i < arrayList.size(); ++i) {
            String string2 = bl6 ? "classic-world-pet-collect" : "companion-collect";
            CombatAccess.call(clientSession, arrayList.get((int)i).capacity ? "sendInventoryWeightOver" : "sendInventoryCapacityOver", "(Ljava/lang/String;)V", string2);
            CombatAccess.call(clientSession, bl6 ? "broadcastClassicWorldGroundItem" : "broadcastGroundItem", bl6 ? "(Llocal/lito381/server/ClassicWorldGroundItems$Item;Ljava/lang/String;)V" : "(Llocal/lito381/server/BattleGroundItems$Item;Ljava/lang/String;)V", objectArray[i], bl6 ? "classic-world-pet-collect-overflow" : "companion-getitem-overflow");
        }
    }

    private static void reserveGround(ClientSession clientSession, BattleCompanion.Snapshot snapshot, List<Overflow> list, Object[] objectArray, int n, DurableCommit durableCommit) throws IOException {
        BattleGroundItems.Item item;
        if (n == list.size()) {
            durableCommit.commit();
            return;
        }
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        RuntimeInventory.Item item2 = list.get((int)n).item;
        DurableCommit durableCommit2 = () -> NativeReturnNatureTransfer.reserveGround(clientSession, snapshot, list, objectArray, n + 1, durableCommit);
        Record record = item = String.valueOf(CombatAccess.field(clientSession, "phase")).equals("WORLD") ? gameHub.putClassicWorldGroundItemPersisted((Integer)CombatAccess.field(clientSession, "classicWorldMapId"), (Integer)CombatAccess.field(clientSession, "classicWorldInstanceId"), item2, item2.quantity(), snapshot.x(), snapshot.y(), snapshot.npcId(), durableCommit2) : gameHub.putGroundItemPersisted((Long)CombatAccess.field(clientSession, "currentRoomId"), clientSession, item2, item2.quantity(), snapshot.x(), snapshot.y(), durableCommit2);
        if (item == null) {
            throw new IOException("companion return ground reservation rejected");
        }
        objectArray[n] = item;
    }

    private NativeReturnNatureTransfer() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static /* synthetic */ void lambda$transfer$1(GameStateStore gameStateStore, String string, boolean bl, long l, RuntimeInventory runtimeInventory, ClientSession clientSession, List list, BattleCompanion battleCompanion) throws IOException {
        GameStateStore gameStateStore2 = gameStateStore;
        synchronized (gameStateStore2) {
            GameStateStore.Economy economy = gameStateStore.economy(string);
            if (bl) {
                gameStateStore.saveEconomyState(string, l, runtimeInventory.catalogCounts(), runtimeInventory.catalogItemStates());
            }
            try {
                NativeDeItemApplication.replace(clientSession, runtimeInventory, 0);
            }
            catch (Error | RuntimeException throwable) {
                if (bl) {
                    try {
                        gameStateStore.saveEconomyState(string, economy.points(), economy.inventory(), economy.itemStates());
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
            for (RuntimeInventory.Item item : list) {
                if (battleCompanion.removeCarriedItem(item.objectId(), item.quantity())) continue;
                throw new IllegalStateException("validated companion bag changed under its monitor");
            }
        }
    }

    private record Scope(ClientSession caster, ClientSession owner, BattleCompanion pet, NativePlayerCommit.Binding binding, NativeCastMaterial.Cast cast) {
    }

    private record Overflow(RuntimeInventory.Item item, boolean capacity) {
    }
}
