// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeCorpseState;
import local.lito381.server.NativeDeDeletionOwner;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeDeletionScope;
import local.lito381.server.NativeDeDeletionVisibility;
import local.lito381.server.NativeDeInitializerKernel;
import local.lito381.server.NativeDeResurrectionEffects;

class NativeDeDeletionState
extends NativeDeCorpseState
implements NativeDeDeletionPorts.ObjectRef {
    final NativeDeDeletionOwner deletionOwner;
    final ConcurrentHashMap<Integer, NativeDeDeletionPorts.Viewer> _registeredViewers = new ConcurrentHashMap();
    final NativeDeDeletionVisibility _deletionVisibility = new NativeDeDeletionVisibility();
    final AtomicBoolean _deletionStallReported = new AtomicBoolean();
    NativeDeDeletionPorts.Spawn _spawn;
    int _spawnNumber;
    NativeDeCharacterFields _master;
    NativeDeDeletionPorts.Hate _hateList;
    NativeDeDeletionPorts.Hate _dropHateList;
    final ReentrantLock _targetItemLock = new ReentrantLock();
    final ReentrantLock _digestItemLock = new ReentrantLock();
    final List<Object> _targetItemList = new ArrayList<Object>();
    final HashMap<Object, Object> _del_map = new HashMap();
    volatile boolean _combatDeathCause;
    boolean respawn;
    boolean timedExist;
    int spawnTime;
    String nameId = "";
    NativeDeInitializerKernel.Name deName;
    NativeDeDeletionPorts.Inventory inventory;
    NativeDeDeletionPorts.Template template;
    NativeDeDeletionPorts.MobGroup mobGroup;

    NativeDeDeletionState(int n, NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeDeletionOwner nativeDeDeletionOwner) {
        super(2, n, nativeDeResurrectionEffects, nativeDeDeletionOwner.timer);
        this.deletionOwner = nativeDeDeletionOwner;
    }

    NativeDeDeletionState(int n, NativeDeCharacterFields.Snapshot snapshot, NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeDeletionOwner nativeDeDeletionOwner) throws IOException {
        super(n, snapshot, nativeDeResurrectionEffects, nativeDeDeletionOwner.timer);
        this.deletionOwner = nativeDeDeletionOwner;
    }

    @Override
    public void deleteMe() {
        NativeDeDeletionScope.run(this.deletionOwner, this::sourceDeleteNpc);
    }

    public final boolean registerViewer(NativeDeDeletionPorts.Viewer viewer) {
        return NativeDeDeletionScope.call(this.deletionOwner, () -> this.sourceRegisterViewer(viewer));
    }

    public final void unregisterViewer(NativeDeDeletionPorts.Viewer viewer) {
        NativeDeDeletionScope.run(this.deletionOwner, () -> this.sourceUnregisterViewer(viewer));
    }

    public final boolean isViewerRegistered(NativeDeDeletionPorts.Viewer viewer) {
        return NativeDeDeletionScope.call(this.deletionOwner, () -> this.sourceIsViewerRegistered(viewer));
    }

    public final NativeDeDeletionVisibility.Snapshot deletionVisibilitySnapshot() {
        return NativeDeDeletionScope.call(this.deletionOwner, this::sourceDeletionVisibilitySnapshot);
    }

    protected void onDeleteClaimed() {
        this.deletionOwner.services.onDeleteClaimed(this);
    }

    void cancelNpcChatTimer() {
        this.deletionOwner.services.cancelChat(this);
    }

    public void allTargetClear() {
        this.deletionOwner.services.allTargetClear(this);
    }

    NativeDeCharacterFields getMaster() {
        return this._master;
    }

    NativeDeDeletionPorts.Inventory getInventory() {
        return this.inventory;
    }

    @Override
    int get_showId() {
        return this.showId;
    }

    NativeDeDeletionPorts.Template getNpcTemplate() {
        return this.template;
    }

    String getNameId() {
        return this.nameId;
    }

    int getSpawnNumber() {
        return this._spawnNumber;
    }

    boolean is_spawnTime() {
        return this.timedExist;
    }

    int get_spawnTime() {
        return this.spawnTime;
    }

    NativeDeDeletionPorts.MobGroup getMobGroupInfo() {
        return this.mobGroup;
    }

    void setMobGroupInfo(NativeDeDeletionPorts.MobGroup mobGroup) {
        this.mobGroup = mobGroup;
    }

    boolean isReSpawn() {
        return this.respawn;
    }

    public final boolean sourceIsViewerRegistered(NativeDeDeletionPorts.Viewer viewer) {
        NativeDeDeletionScope.require(this);
        return viewer != null && this._registeredViewers.get(viewer.getId()) == viewer;
    }

    public final NativeDeDeletionVisibility.Snapshot sourceDeletionVisibilitySnapshot() {
        NativeDeDeletionScope.require(this);
        return this._deletionVisibility.snapshot();
    }

    private void sourceBeginDeletionObservation() {
        NativeDeDeletionScope.require(this);
        this._deletionStallReported.set(false);
        this._deletionVisibility.beginDeletion();
        NativeDeDeletionScope.DELETIONS_IN_FLIGHT().put(this.getId(), this);
    }

    private void sourceTransitionDeletion(NativeDeDeletionVisibility.Phase phase) {
        NativeDeDeletionScope.require(this);
        this._deletionVisibility.transition(phase);
    }

    private void sourceResetDeletionObservation() {
        NativeDeDeletionScope.require(this);
        NativeDeDeletionScope.DELETIONS_IN_FLIGHT().remove(this.getId(), this);
        this._deletionStallReported.set(false);
        this._deletionVisibility.resetActive();
    }

    private void sourceFinishDeletionObservation(NativeDeDeletionVisibility.Phase phase) {
        NativeDeDeletionScope.require(this);
        this.sourceTransitionDeletion(phase);
        NativeDeDeletionScope.DELETIONS_IN_FLIGHT().remove(this.getId(), this);
    }

    public static void sourceAuditDeletionStalls() {
        if (NativeDeDeletionScope.DELETIONS_IN_FLIGHT().isEmpty()) {
            return;
        }
        for (NativeDeDeletionState nativeDeDeletionState : NativeDeDeletionScope.DELETIONS_IN_FLIGHT().values()) {
            long l;
            try {
                if (nativeDeDeletionState == null) continue;
                NativeDeDeletionVisibility.Snapshot snapshot = nativeDeDeletionState.sourceDeletionVisibilitySnapshot();
                if (!snapshot.deletionInProgress()) {
                    NativeDeDeletionScope.DELETIONS_IN_FLIGHT().remove(nativeDeDeletionState.getId(), nativeDeDeletionState);
                    continue;
                }
                if (snapshot.deleteAgeMillis() < NativeDeDeletionScope.DELETION_STALL_MILLIS() || !nativeDeDeletionState._deletionStallReported.compareAndSet(false, true)) continue;
                l = NativeDeDeletionScope.DELETION_STALL_COUNT().incrementAndGet();
                boolean bl = false;
                try {
                    bl = NativeDeDeletionPorts.World.get().isObjectRegistered(nativeDeDeletionState);
                }
                catch (RuntimeException runtimeException) {
                    NativeDeDeletionScope._log().warn("[NpcLifecycle] deletion stall world-state read failed: objectId={} npcId={} phase={}", (Object)nativeDeDeletionState.getId(), (Object)nativeDeDeletionState.getNpcId(), (Object)snapshot.phase(), (Object)runtimeException);
                }
                NativeDeDeletionScope._log().warn("[NpcLifecycle] deletion stalled: objectId={} npcId={} phase={} deleteAgeMs={} phaseAgeMs={} worldRegistered={} destroyed={} viewers={} map={} x={} y={} count={}", new Object[]{nativeDeDeletionState.getId(), nativeDeDeletionState.getNpcId(), snapshot.phase(), snapshot.deleteAgeMillis(), snapshot.phaseAgeMillis(), bl, nativeDeDeletionState.destroyed(), nativeDeDeletionState._registeredViewers.size(), (int)nativeDeDeletionState.getMapId(), nativeDeDeletionState.getX(), nativeDeDeletionState.getY(), l});
            }
            catch (RuntimeException runtimeException) {
                l = NativeDeDeletionScope.DELETION_AUDIT_FAILURE_COUNT().incrementAndGet();
                NativeDeDeletionScope._log().error("[NpcLifecycle] deletion stall audit failed: objectId={} count={}", nativeDeDeletionState != null ? nativeDeDeletionState.getId() : -1, l, runtimeException);
            }
        }
    }

    public final boolean sourceRegisterViewer(NativeDeDeletionPorts.Viewer viewer) {
        NativeDeDeletionScope.require(this);
        if (viewer == null) {
            return false;
        }
        if (this._destroyed || this.deletionClaimFlag().get()) {
            NativeDeDeletionScope.LATE_VIEWER_REJECTED_COUNT().incrementAndGet();
            return false;
        }
        this._registeredViewers.put(viewer.getId(), viewer);
        if (this._destroyed || this.deletionClaimFlag().get()) {
            this._registeredViewers.remove(viewer.getId(), viewer);
            NativeDeDeletionScope.LATE_VIEWER_REJECTED_COUNT().incrementAndGet();
            return false;
        }
        return true;
    }

    public final void sourceUnregisterViewer(NativeDeDeletionPorts.Viewer viewer) {
        NativeDeDeletionScope.require(this);
        if (viewer != null) {
            this._registeredViewers.remove(viewer.getId(), viewer);
        }
    }

    private List<NativeDeDeletionPorts.Viewer> sourceSnapshotRemovalRecipients() {
        NativeDeDeletionScope.require(this);
        LinkedHashMap<Integer, NativeDeDeletionPorts.Viewer> linkedHashMap = new LinkedHashMap<Integer, NativeDeDeletionPorts.Viewer>();
        for (NativeDeDeletionPorts.Viewer viewer : this._registeredViewers.values()) {
            if (viewer == null) continue;
            linkedHashMap.put(viewer.getId(), viewer);
        }
        try {
            for (NativeDeDeletionPorts.Viewer viewer : NativeDeDeletionPorts.World.get().getRecognizePlayer(this)) {
                if (viewer == null) continue;
                linkedHashMap.put(viewer.getId(), viewer);
            }
        }
        catch (RuntimeException runtimeException) {
            NativeDeDeletionScope._log().warn("[NpcLifecycle] nearby viewer snapshot failed; using registered viewers: objectId={} npcId={} registered={}", (Object)this.getId(), (Object)this.getNpcId(), (Object)linkedHashMap.size(), (Object)runtimeException);
        }
        return new ArrayList<NativeDeDeletionPorts.Viewer>(linkedHashMap.values());
    }

    private boolean sourceIsCombatDeathCause() {
        NativeDeDeletionScope.require(this);
        return this._combatDeathCause;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void sourceDeleteNpc() {
        boolean bl;
        Object object;
        Object object2;
        Object object3;
        NativeDeCharacterFields nativeDeCharacterFields;
        Object object4;
        block64: {
            NativeDeDeletionScope.require(this);
            if (!this.deletionClaimFlag().compareAndSet(false, true)) {
                long l = NativeDeDeletionScope.DELETE_DUPLICATE_SUPPRESSED_COUNT().incrementAndGet();
                if (l <= 8L || (l & l - 1L) == 0L) {
                    NativeDeDeletionScope._log().warn("[NpcLifecycle] duplicate delete request suppressed: objectId={} npcId={} destroyed={} dead={} spawnNumber={} count={}", (Object)this.getId(), (Object)this.getNpcId(), (Object)this._destroyed, (Object)this.isDead(), (Object)this._spawnNumber, (Object)l);
                }
                return;
            }
            this.sourceBeginDeletionObservation();
            try {
                this.setWeaponBreakStack(0);
            }
            catch (RuntimeException runtimeException) {
                NativeDeDeletionScope._log().warn("[NpcLifecycle] pre-delete weapon state cleanup failed: objectId={} npcId={}", (Object)this.getId(), (Object)this.getNpcId(), (Object)runtimeException);
            }
            int n = 0;
            switch (this.getNpcId()) {
                case 92000: {
                    n = 92001;
                    break;
                }
                case 92001: {
                    n = 92000;
                }
            }
            try {
                if (this instanceof NativeDeDeletionPorts.Illusory && (object4 = this.getMaster()) instanceof NativeDeDeletionPorts.Viewer) {
                    nativeDeCharacterFields = (NativeDeDeletionPorts.Viewer)object4;
                    ((NativeDeDeletionPorts.Viewer)nativeDeCharacterFields).get_otherList().removeIllusoryList(this.getId());
                }
            }
            catch (RuntimeException runtimeException) {
                NativeDeDeletionScope._log().warn("[NpcLifecycle] illusory registry cleanup failed: objectId={} npcId={}", (Object)this.getId(), (Object)this.getNpcId(), (Object)runtimeException);
            }
            nativeDeCharacterFields = null;
            try {
                if (n == 0) break block64;
                object4 = NativeDeDeletionPorts.World.get().getVisibleObjects(this);
                object3 = ((ArrayList)object4).iterator();
                while (object3.hasNext()) {
                    NativeDeDeletionState nativeDeDeletionState;
                    object2 = (Object[])object3.next();
                    if (!(object2 instanceof NativeDeDeletionState) || (nativeDeDeletionState = (NativeDeDeletionState)object2).getNpcId() != n) continue;
                    if (!nativeDeDeletionState.isDead()) {
                        this.releasePairedCorpseClaim();
                        this.resurrect(this.getMaxHp());
                        if (!this.isDead()) {
                            this.sourceResetDeletionObservation();
                            this.deletionClaimFlag().set(false);
                            return;
                        }
                        NativeDeDeletionScope._log().error("[NpcLifecycle] paired resurrection failed; continuing corpse removal: objectId={} npcId={} pairedObjectId={}", this.getId(), this.getNpcId(), nativeDeDeletionState.getId());
                        continue;
                    }
                    if (nativeDeDeletionState._destroyed) continue;
                    nativeDeCharacterFields = nativeDeDeletionState;
                    break;
                }
            }
            catch (RuntimeException runtimeException) {
                NativeDeDeletionScope._log().warn("[NpcLifecycle] paired NPC lookup failed; continuing deletion: objectId={} npcId={} targetNpcId={}", (Object)this.getId(), (Object)this.getNpcId(), (Object)n, (Object)runtimeException);
            }
        }
        try {
            this.onDeleteClaimed();
        }
        catch (RuntimeException runtimeException) {
            NativeDeDeletionScope._log().warn("[NpcLifecycle] subtype cleanup failed; continuing central deletion: objectId={} npcId={} type={}", (Object)this.getId(), (Object)this.getNpcId(), (Object)this.getClass().getSimpleName(), (Object)runtimeException);
        }
        this._destroyed = true;
        this.sourceTransitionDeletion(NativeDeDeletionVisibility.Phase.DESTROYED_MARKED);
        NativeDeDeletionPorts.DeadTimer.get().untrack(this);
        NativeDeDeletionPorts.DeleteTimer.get().untrack(this);
        NativeDeDeletionPorts.RestTimer.remove(this);
        NativeDeDeletionPorts.WorkTimer.remove(this);
        NativeDeDeletionPorts.DigestTimer.remove(this);
        object3 = this;
        if (object3 instanceof NativeDeDeletionPorts.Monster) {
            object4 = (NativeDeDeletionPorts.Monster)object3;
            NativeDeDeletionPorts.HprTimer.remove((NativeDeDeletionPorts.Monster)object4);
            NativeDeDeletionPorts.MprTimer.remove((NativeDeDeletionPorts.Monster)object4);
        }
        this.cancelNpcChatTimer();
        object4 = this.sourceSnapshotRemovalRecipients();
        this.sourceTransitionDeletion(NativeDeDeletionVisibility.Phase.VIEWERS_CAPTURED);
        try {
            if (this.getInventory() != null) {
                this.getInventory().clearItems();
            }
            this.allTargetClear();
            this._master = null;
        }
        catch (RuntimeException runtimeException) {
            NativeDeDeletionScope._log().warn("[NpcLifecycle] owned state cleanup failed: objectId={} npcId={}", (Object)this.getId(), (Object)this.getNpcId(), (Object)runtimeException);
        }
        int n = this.get_showId();
        try {
            if (NativeDeDeletionPorts.Quest.get().isQuest(n)) {
                NativeDeDeletionPorts.Quest.get().remove(n, this);
            }
        }
        catch (RuntimeException runtimeException) {
            NativeDeDeletionScope._log().warn("[NpcLifecycle] quest registry cleanup failed: objectId={} npcId={} showId={}", (Object)this.getId(), (Object)this.getNpcId(), (Object)n, (Object)runtimeException);
        }
        try {
            if (!this.getDolls().isEmpty()) {
                for (Object object5 : this.getDolls().values().toArray()) {
                    object = (NativeDeDeletionPorts.Doll)object5;
                    if (object == null) continue;
                    ((NativeDeDeletionPorts.Doll)object).deleteDoll();
                }
            }
        }
        catch (RuntimeException runtimeException) {
            NativeDeDeletionScope._log().warn("[NpcLifecycle] doll cleanup failed: objectId={} npcId={}", (Object)this.getId(), (Object)this.getNpcId(), (Object)runtimeException);
        }
        try {
            if (!this.getPetList().isEmpty()) {
                for (Object object5 : this.getPetList().values().toArray()) {
                    object = (NativeDeDeletionState)object5;
                    if (!(object instanceof NativeDeDeletionPorts.Summon)) continue;
                    NativeDeDeletionPorts.Summon summon = (NativeDeDeletionPorts.Summon)object;
                    summon.deleteMe();
                }
            }
        }
        catch (RuntimeException runtimeException) {
            NativeDeDeletionScope._log().warn("[NpcLifecycle] summon cleanup failed: objectId={} npcId={}", (Object)this.getId(), (Object)this.getNpcId(), (Object)runtimeException);
        }
        try {
            this._hateList.clear();
            this._dropHateList.clear();
            this._targetItemLock.lock();
            try {
                this._targetItemList.clear();
            }
            finally {
                this._targetItemLock.unlock();
            }
            this._digestItemLock.lock();
            try {
                this._del_map.clear();
            }
            finally {
                this._digestItemLock.unlock();
            }
            NativeDeDeletionPorts.DigestTimer.remove(this);
        }
        catch (RuntimeException runtimeException) {
            NativeDeDeletionScope._log().warn("[NpcLifecycle] target state cleanup failed: objectId={} npcId={}", (Object)this.getId(), (Object)this.getNpcId(), (Object)runtimeException);
        }
        object2 = NativeDeDeletionPorts.World.get();
        try {
            ((NativeDeDeletionPorts.World)object2).removeObject(this);
            if (((NativeDeDeletionPorts.World)object2).isObjectRegistered(this)) {
                ((NativeDeDeletionPorts.World)object2).removeVisibleObject(this);
                ((NativeDeDeletionPorts.World)object2).removeObject(this);
            }
            if (!((NativeDeDeletionPorts.World)object2).isObjectRegistered(this)) {
                this.sourceTransitionDeletion(NativeDeDeletionVisibility.Phase.WORLD_REMOVED);
            }
        }
        catch (RuntimeException runtimeException) {
            NativeDeDeletionScope._log().error("[NpcLifecycle] world removal failed: objectId={} npcId={} map={} x={} y={}", this.getId(), this.getNpcId(), this.getMapId(), this.getX(), this.getY(), runtimeException);
        }
        Object object6 = object4.iterator();
        while (object6.hasNext()) {
            NativeDeDeletionPorts.Viewer viewer = (NativeDeDeletionPorts.Viewer)object6.next();
            try {
                if (viewer == null) continue;
                viewer.removeKnownObject(this);
                viewer.sendPackets(new NativeDeDeletionPorts.RemoveObject(this));
                if (!viewer.knownsObject(this)) continue;
                long l = NativeDeDeletionScope.DELETE_VERIFY_FAILURE_COUNT().incrementAndGet();
                NativeDeDeletionScope._log().warn("[NpcLifecycle] viewer removal verification failed: objectId={} npcId={} viewerId={} count={}", (Object)this.getId(), (Object)this.getNpcId(), (Object)viewer.getId(), (Object)l);
            }
            catch (RuntimeException runtimeException) {
                long l = NativeDeDeletionScope.DELETE_VERIFY_FAILURE_COUNT().incrementAndGet();
                NativeDeDeletionScope._log().warn("[NpcLifecycle] remove packet publication failed: objectId={} npcId={} viewerId={} count={}", (Object)this.getId(), (Object)this.getNpcId(), (Object)(viewer != null ? viewer.getId() : -1), (Object)l, (Object)runtimeException);
            }
        }
        this.sourceTransitionDeletion(NativeDeDeletionVisibility.Phase.REMOVE_PUBLISHED);
        this._registeredViewers.clear();
        this.removeAllKnownObjects();
        if (nativeDeCharacterFields != null) {
            try {
                ((NativeDeDeletionState)nativeDeCharacterFields).deleteMe();
            }
            catch (RuntimeException runtimeException) {
                NativeDeDeletionScope._log().warn("[NpcLifecycle] paired NPC deletion failed: objectId={} pairedObjectId={}", (Object)this.getId(), (Object)((NativeDeCorpseState)nativeDeCharacterFields).getId(), (Object)runtimeException);
            }
        }
        try {
            if (this.getNpcTemplate().isBoss()) {
                Object object7 = this.sourceIsCombatDeathCause() ? "COMBAT_CORPSE_CLEANUP" : (object6 = this.isDead() ? "NON_COMBAT_FORCED_DEAD_DELETE" : "NON_COMBAT_LIVE_DELETE");
                if (NativeDeDeletionScope.BOSS_DEATH_AUDIT_ENABLE()) {
                    NativeDeDeletionScope._log().info("[BossDeathAudit] event=BOSS_REMOVE cause={} npcId={} name=\"{}\" nameId=\"{}\" objectId={} hp={}/{} dead={} mapId={} x={} y={} showId={} spawnTable={} spawnId={} spawnNumber={} timedExist={} remainingExistSeconds={}", object6, this.getNpcId(), NativeDeDeletionState.sourceAuditText(this.getName()), NativeDeDeletionState.sourceAuditText(this.getNameId()), this.getId(), this.getCurrentHp(), this.getMaxHp(), this.isDead(), this.getMapId(), this.getX(), this.getY(), this.get_showId(), this._spawn != null ? this._spawn.getSpawnConfigTable() : "none", this._spawn != null ? this._spawn.getId() : -1, this.getSpawnNumber(), this.is_spawnTime(), this.get_spawnTime());
                }
                if (this._spawn != null) {
                    NativeDeDeletionPorts.Boss.get().onBossDeath(this._spawn, this.getNameId());
                }
            }
        }
        catch (Exception exception) {
            NativeDeDeletionScope._log().error(exception.getLocalizedMessage(), exception);
        }
        try {
            object6 = this.getMobGroupInfo();
            if (object6 != null && object6.removeMember(this) == 0) {
                this.setMobGroupInfo(null);
            }
        }
        catch (RuntimeException runtimeException) {
            NativeDeDeletionScope._log().warn("[NpcLifecycle] mob group cleanup failed: objectId={} npcId={}", (Object)this.getId(), (Object)this.getNpcId(), (Object)runtimeException);
        }
        boolean bl2 = bl = !((NativeDeDeletionPorts.World)object2).isObjectRegistered(this);
        if (!bl) {
            long l = NativeDeDeletionScope.DELETE_VERIFY_FAILURE_COUNT().incrementAndGet();
            this.sourceFinishDeletionObservation(NativeDeDeletionVisibility.Phase.FAILED);
            NativeDeDeletionScope._log().error("[NpcLifecycle] removal incomplete; respawn suppressed to prevent duplicate slot: objectId={} npcId={} spawnTable={} spawnId={} spawnNumber={} count={}", this.getId(), this.getNpcId(), this._spawn != null ? this._spawn.getSpawnConfigTable() : "none", this._spawn != null ? this._spawn.getId() : -1, this._spawnNumber, l);
            return;
        }
        if (this._spawn != null) {
            this._spawn.releaseActiveNpc(this);
        }
        if (this.isReSpawn()) {
            if (this._spawn == null) {
                long l = NativeDeDeletionScope.DELETE_VERIFY_FAILURE_COUNT().incrementAndGet();
                NativeDeDeletionScope._log().error("[NpcLifecycle] respawn requested without spawn owner: objectId={} npcId={} spawnNumber={} count={}", this.getId(), this.getNpcId(), this._spawnNumber, l);
            } else {
                this._spawn.executeSpawnTask(this._spawnNumber, 0);
            }
        }
        this.sourceFinishDeletionObservation(NativeDeDeletionVisibility.Phase.TERMINAL);
    }

    private static String sourceAuditText(String string) {
        return string == null ? "" : string.replace('\r', ' ').replace('\n', ' ');
    }

    private final void sourceAbortFailedWorldRegistration() {
        NativeDeDeletionScope.require(this);
        this.deletionClaimFlag().set(true);
        this._destroyed = true;
        this._deletionVisibility.registrationAborted();
        NativeDeDeletionScope.DELETIONS_IN_FLIGHT().remove(this.getId(), this);
        NativeDeDeletionPorts.DeadTimer.get().untrack(this);
        NativeDeDeletionPorts.DeleteTimer.get().untrack(this);
        NativeDeDeletionPorts.RestTimer.remove(this);
        NativeDeDeletionPorts.WorkTimer.remove(this);
        NativeDeDeletionPorts.DigestTimer.remove(this);
        NativeDeDeletionState nativeDeDeletionState = this;
        if (nativeDeDeletionState instanceof NativeDeDeletionPorts.Monster) {
            NativeDeDeletionPorts.Monster monster = (NativeDeDeletionPorts.Monster)nativeDeDeletionState;
            NativeDeDeletionPorts.HprTimer.remove(monster);
            NativeDeDeletionPorts.MprTimer.remove(monster);
        }
        this.cancelNpcChatTimer();
    }

    public final void abortFailedWorldRegistration() {
        NativeDeDeletionScope.run(this.deletionOwner, this::sourceAbortFailedWorldRegistration);
    }
}
