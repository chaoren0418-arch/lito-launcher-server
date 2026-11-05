// DECOMPILED from base-0.6.71.jar, TODO review
// Decompiled with Vineflower 1.11.2; access mismatch requires review.
package local.lito381.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

class NativeDeDeletionState extends NativeDeCorpseState implements NativeDeDeletionPorts.ObjectRef {
   final NativeDeDeletionOwner deletionOwner;
   final ConcurrentHashMap<Integer, NativeDeDeletionPorts.Viewer> _registeredViewers = new ConcurrentHashMap<>();
   final NativeDeDeletionVisibility _deletionVisibility = new NativeDeDeletionVisibility();
   final AtomicBoolean _deletionStallReported = new AtomicBoolean();
   NativeDeDeletionPorts.Spawn _spawn;
   int _spawnNumber;
   NativeDeCharacterFields _master;
   NativeDeDeletionPorts.Hate _hateList;
   NativeDeDeletionPorts.Hate _dropHateList;
   final ReentrantLock _targetItemLock = new ReentrantLock();
   final ReentrantLock _digestItemLock = new ReentrantLock();
   final List<Object> _targetItemList = new ArrayList<>();
   final HashMap<Object, Object> _del_map = new HashMap<>();
   volatile boolean _combatDeathCause;
   boolean respawn;
   boolean timedExist;
   int spawnTime;
   String nameId = "";
   NativeDeInitializerKernel.Name deName;
   NativeDeDeletionPorts.Inventory inventory;
   NativeDeDeletionPorts.Template template;
   NativeDeDeletionPorts.MobGroup mobGroup;

   NativeDeDeletionState(int var1, NativeDeResurrectionEffects var2, NativeDeDeletionOwner var3) {
      super(2, var1, var2, var3.timer);
      this.deletionOwner = var3;
   }

   NativeDeDeletionState(int var1, NativeDeCharacterFields.Snapshot var2, NativeDeResurrectionEffects var3, NativeDeDeletionOwner var4) throws IOException {
      super(var1, var2, var3, var4.timer);
      this.deletionOwner = var4;
   }

   @Override
   public void deleteMe() {
      NativeDeDeletionScope.run(this.deletionOwner, this::sourceDeleteNpc);
   }

   public final boolean registerViewer(NativeDeDeletionPorts.Viewer var1) {
      return NativeDeDeletionScope.call(this.deletionOwner, () -> this.sourceRegisterViewer(var1));
   }

   public final void unregisterViewer(NativeDeDeletionPorts.Viewer var1) {
      NativeDeDeletionScope.run(this.deletionOwner, () -> this.sourceUnregisterViewer(var1));
   }

   public final boolean isViewerRegistered(NativeDeDeletionPorts.Viewer var1) {
      return NativeDeDeletionScope.call(this.deletionOwner, () -> this.sourceIsViewerRegistered(var1));
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
   // TODO #72: formal access is package-private; javac rejects narrowing the public superclass method.
   public int get_showId() {
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

   void setMobGroupInfo(NativeDeDeletionPorts.MobGroup var1) {
      this.mobGroup = var1;
   }

   boolean isReSpawn() {
      return this.respawn;
   }

   public final boolean sourceIsViewerRegistered(NativeDeDeletionPorts.Viewer var1) {
      NativeDeDeletionScope.require(this);
      return var1 != null && this._registeredViewers.get(var1.getId()) == var1;
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

   private void sourceTransitionDeletion(NativeDeDeletionVisibility.Phase var1) {
      NativeDeDeletionScope.require(this);
      this._deletionVisibility.transition(var1);
   }

   private void sourceResetDeletionObservation() {
      NativeDeDeletionScope.require(this);
      NativeDeDeletionScope.DELETIONS_IN_FLIGHT().remove(this.getId(), this);
      this._deletionStallReported.set(false);
      this._deletionVisibility.resetActive();
   }

   private void sourceFinishDeletionObservation(NativeDeDeletionVisibility.Phase var1) {
      NativeDeDeletionScope.require(this);
      this.sourceTransitionDeletion(var1);
      NativeDeDeletionScope.DELETIONS_IN_FLIGHT().remove(this.getId(), this);
   }

    public static void sourceAuditDeletionStalls() {
        if (NativeDeDeletionScope.DELETIONS_IN_FLIGHT().isEmpty()) {
            return;
        }
        for (NativeDeDeletionState nativeDeDeletionState : NativeDeDeletionScope.DELETIONS_IN_FLIGHT().values()) {
            try {
                if (nativeDeDeletionState == null) continue;
                NativeDeDeletionVisibility.Snapshot snapshot = nativeDeDeletionState.sourceDeletionVisibilitySnapshot();
                if (!snapshot.deletionInProgress()) {
                    NativeDeDeletionScope.DELETIONS_IN_FLIGHT().remove(nativeDeDeletionState.getId(), nativeDeDeletionState);
                    continue;
                }
                if (snapshot.deleteAgeMillis() < NativeDeDeletionScope.DELETION_STALL_MILLIS() || !nativeDeDeletionState._deletionStallReported.compareAndSet(false, true)) continue;
                long l = NativeDeDeletionScope.DELETION_STALL_COUNT().incrementAndGet();
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
                long l = NativeDeDeletionScope.DELETION_AUDIT_FAILURE_COUNT().incrementAndGet();
                NativeDeDeletionScope._log().error("[NpcLifecycle] deletion stall audit failed: objectId={} count={}", nativeDeDeletionState != null ? nativeDeDeletionState.getId() : -1, l, runtimeException);
            }
        }
    }

    public final boolean sourceRegisterViewer(NativeDeDeletionPorts.Viewer viewer) {
        NativeDeDeletionScope.require(this);
        if (viewer == null) {
            return false;
        }
        if (this._destroyed || ((NativeDeNpcVitalState)this).deletionClaimFlag().get()) {
            NativeDeDeletionScope.LATE_VIEWER_REJECTED_COUNT().incrementAndGet();
            return false;
        }
        this._registeredViewers.put(viewer.getId(), viewer);
        if (this._destroyed || ((NativeDeNpcVitalState)this).deletionClaimFlag().get()) {
            this._registeredViewers.remove(viewer.getId(), viewer);
            NativeDeDeletionScope.LATE_VIEWER_REJECTED_COUNT().incrementAndGet();
            return false;
        }
        return true;
    }

   public final void sourceUnregisterViewer(NativeDeDeletionPorts.Viewer var1) {
      NativeDeDeletionScope.require(this);
      if (var1 != null) {
         this._registeredViewers.remove(var1.getId(), var1);
      }
   }

   private List<NativeDeDeletionPorts.Viewer> sourceSnapshotRemovalRecipients() {
      NativeDeDeletionScope.require(this);
      LinkedHashMap var1 = new LinkedHashMap();

      for (NativeDeDeletionPorts.Viewer var3 : this._registeredViewers.values()) {
         if (var3 != null) {
            var1.put(var3.getId(), var3);
         }
      }

      try {
         for (NativeDeDeletionPorts.Viewer var6 : NativeDeDeletionPorts.World.get().getRecognizePlayer(this)) {
            if (var6 != null) {
               var1.put(var6.getId(), var6);
            }
         }
      } catch (RuntimeException var4) {
         NativeDeDeletionScope._log()
            .warn(
               "[NpcLifecycle] nearby viewer snapshot failed; using registered viewers: objectId={} npcId={} registered={}",
               this.getId(),
               this.getNpcId(),
               var1.size(),
               var4
            );
      }

      return new ArrayList<>(var1.values());
   }

   private boolean sourceIsCombatDeathCause() {
      NativeDeDeletionScope.require(this);
      return this._combatDeathCause;
   }

   protected void sourceDeleteNpc() {
      NativeDeDeletionScope.require(this);
      if (!((NativeDeNpcVitalState)this).deletionClaimFlag().compareAndSet(false, true)) {
         long var61 = NativeDeDeletionScope.DELETE_DUPLICATE_SUPPRESSED_COUNT().incrementAndGet();
         if (var61 <= 8L || (var61 & var61 - 1L) == 0L) {
            NativeDeDeletionScope._log()
               .warn(
                  "[NpcLifecycle] duplicate delete request suppressed: objectId={} npcId={} destroyed={} dead={} spawnNumber={} count={}",
                  this.getId(),
                  this.getNpcId(),
                  this._destroyed,
                  this.isDead(),
                  this._spawnNumber,
                  var61
               );
         }
      } else {
         this.sourceBeginDeletionObservation();

         try {
            this.setWeaponBreakStack(0);
         } catch (RuntimeException var56) {
            NativeDeDeletionScope._log()
               .warn("[NpcLifecycle] pre-delete weapon state cleanup failed: objectId={} npcId={}", this.getId(), this.getNpcId(), var56);
         }

         int var1 = 0;
         switch (this.getNpcId()) {
            case 92000:
               var1 = 92001;
               break;
            case 92001:
               var1 = 92000;
         }

         try {
            if (this instanceof NativeDeDeletionPorts.Illusory && this.getMaster() instanceof NativeDeDeletionPorts.Viewer var2) {
               var2.get_otherList().removeIllusoryList(this.getId());
            }
         } catch (RuntimeException var55) {
            NativeDeDeletionScope._log().warn("[NpcLifecycle] illusory registry cleanup failed: objectId={} npcId={}", this.getId(), this.getNpcId(), var55);
         }

         NativeDeDeletionState var62 = null;

         try {
            if (var1 != 0) {
               for (NativeDeDeletionPorts.ObjectRef var5 : NativeDeDeletionPorts.World.get().getVisibleObjects(this)) {
                  if (var5 instanceof NativeDeDeletionState var6 && var6.getNpcId() == var1) {
                     if (!var6.isDead()) {
                        this.releasePairedCorpseClaim();
                        this.resurrect(this.getMaxHp());
                        if (!this.isDead()) {
                           this.sourceResetDeletionObservation();
                           ((NativeDeNpcVitalState)this).deletionClaimFlag().set(false);
                           return;
                        }

                        NativeDeDeletionScope._log()
                           .error(
                              "[NpcLifecycle] paired resurrection failed; continuing corpse removal: objectId={} npcId={} pairedObjectId={}",
                              this.getId(),
                              this.getNpcId(),
                              var6.getId()
                           );
                     } else if (!var6._destroyed) {
                        var62 = var6;
                        break;
                     }
                  }
               }
            }
         } catch (RuntimeException var60) {
            NativeDeDeletionScope._log()
               .warn(
                  "[NpcLifecycle] paired NPC lookup failed; continuing deletion: objectId={} npcId={} targetNpcId={}",
                  this.getId(),
                  this.getNpcId(),
                  var1,
                  var60
               );
         }

         try {
            this.onDeleteClaimed();
         } catch (RuntimeException var54) {
            NativeDeDeletionScope._log()
               .warn(
                  "[NpcLifecycle] subtype cleanup failed; continuing central deletion: objectId={} npcId={} type={}",
                  this.getId(),
                  this.getNpcId(),
                  this.getClass().getSimpleName(),
                  var54
               );
         }

         this._destroyed = true;
         this.sourceTransitionDeletion(NativeDeDeletionVisibility.Phase.DESTROYED_MARKED);
         NativeDeDeletionPorts.DeadTimer.get().untrack(this);
         NativeDeDeletionPorts.DeleteTimer.get().untrack(this);
         NativeDeDeletionPorts.RestTimer.remove(this);
         NativeDeDeletionPorts.WorkTimer.remove(this);
         NativeDeDeletionPorts.DigestTimer.remove(this);
         if (this instanceof NativeDeDeletionPorts.Monster var64) {
            NativeDeDeletionPorts.HprTimer.remove(var64);
            NativeDeDeletionPorts.MprTimer.remove(var64);
         }

         this.cancelNpcChatTimer();
         List<NativeDeDeletionPorts.Viewer> var65 = this.sourceSnapshotRemovalRecipients();
         this.sourceTransitionDeletion(NativeDeDeletionVisibility.Phase.VIEWERS_CAPTURED);

         try {
            if (this.getInventory() != null) {
               this.getInventory().clearItems();
            }

            this.allTargetClear();
            this._master = null;
         } catch (RuntimeException var53) {
            NativeDeDeletionScope._log().warn("[NpcLifecycle] owned state cleanup failed: objectId={} npcId={}", this.getId(), this.getNpcId(), var53);
         }

         int var66 = this.get_showId();

         try {
            if (NativeDeDeletionPorts.Quest.get().isQuest(var66)) {
               NativeDeDeletionPorts.Quest.get().remove(var66, this);
            }
         } catch (RuntimeException var52) {
            NativeDeDeletionScope._log()
               .warn("[NpcLifecycle] quest registry cleanup failed: objectId={} npcId={} showId={}", this.getId(), this.getNpcId(), var66, var52);
         }

         try {
            if (!this.getDolls().isEmpty()) {
               for (Object var8 : this.getDolls().values().toArray()) {
                  NativeDeDeletionPorts.Doll var9 = (NativeDeDeletionPorts.Doll)var8;
                  if (var9 != null) {
                     var9.deleteDoll();
                  }
               }
            }
         } catch (RuntimeException var59) {
            NativeDeDeletionScope._log().warn("[NpcLifecycle] doll cleanup failed: objectId={} npcId={}", this.getId(), this.getNpcId(), var59);
         }

         try {
            if (!this.getPetList().isEmpty()) {
               for (Object var80 : this.getPetList().values().toArray()) {
                  NativeDeDeletionState var82 = (NativeDeDeletionState)var80;
                  if (var82 instanceof NativeDeDeletionPorts.Summon var10) {
                     var10.deleteMe();
                  }
               }
            }
         } catch (RuntimeException var58) {
            NativeDeDeletionScope._log().warn("[NpcLifecycle] summon cleanup failed: objectId={} npcId={}", this.getId(), this.getNpcId(), var58);
         }

         try {
            this._hateList.clear();
            this._dropHateList.clear();
            this._targetItemLock.lock();

            try {
               this._targetItemList.clear();
            } finally {
               this._targetItemLock.unlock();
            }

            this._digestItemLock.lock();

            try {
               this._del_map.clear();
            } finally {
               this._digestItemLock.unlock();
            }

            NativeDeDeletionPorts.DigestTimer.remove(this);
         } catch (RuntimeException var51) {
            NativeDeDeletionScope._log().warn("[NpcLifecycle] target state cleanup failed: objectId={} npcId={}", this.getId(), this.getNpcId(), var51);
         }

         NativeDeDeletionPorts.World var69 = NativeDeDeletionPorts.World.get();

         try {
            var69.removeObject(this);
            if (var69.isObjectRegistered(this)) {
               var69.removeVisibleObject(this);
               var69.removeObject(this);
            }

            if (!var69.isObjectRegistered(this)) {
               this.sourceTransitionDeletion(NativeDeDeletionVisibility.Phase.WORLD_REMOVED);
            }
         } catch (RuntimeException var48) {
            NativeDeDeletionScope._log()
               .error(
                  "[NpcLifecycle] world removal failed: objectId={} npcId={} map={} x={} y={}",
                  this.getId(),
                  this.getNpcId(),
                  Integer.valueOf(this.getMapId()),
                  this.getX(),
                  this.getY(),
                  var48
               );
         }

         for (NativeDeDeletionPorts.Viewer var77 : var65) {
            try {
               if (var77 != null) {
                  var77.removeKnownObject(this);
                  var77.sendPackets(new NativeDeDeletionPorts.RemoveObject(this));
                  if (var77.knownsObject(this)) {
                     long var81 = NativeDeDeletionScope.DELETE_VERIFY_FAILURE_COUNT().incrementAndGet();
                     NativeDeDeletionScope._log()
                        .warn(
                           "[NpcLifecycle] viewer removal verification failed: objectId={} npcId={} viewerId={} count={}",
                           this.getId(),
                           this.getNpcId(),
                           var77.getId(),
                           var81
                        );
                  }
               }
            } catch (RuntimeException var57) {
               long var83 = NativeDeDeletionScope.DELETE_VERIFY_FAILURE_COUNT().incrementAndGet();
               NativeDeDeletionScope._log()
                  .warn(
                     "[NpcLifecycle] remove packet publication failed: objectId={} npcId={} viewerId={} count={}",
                     this.getId(),
                     this.getNpcId(),
                     var77 != null ? var77.getId() : -1,
                     var83,
                     var57
                  );
            }
         }

         this.sourceTransitionDeletion(NativeDeDeletionVisibility.Phase.REMOVE_PUBLISHED);
         this._registeredViewers.clear();
         this.removeAllKnownObjects();
         if (var62 != null) {
            try {
               var62.deleteMe();
            } catch (RuntimeException var47) {
               NativeDeDeletionScope._log()
                  .warn("[NpcLifecycle] paired NPC deletion failed: objectId={} pairedObjectId={}", this.getId(), var62.getId(), var47);
            }
         }

         try {
            if (this.getNpcTemplate().isBoss()) {
               String var73 = this.sourceIsCombatDeathCause()
                  ? "COMBAT_CORPSE_CLEANUP"
                  : (this.isDead() ? "NON_COMBAT_FORCED_DEAD_DELETE" : "NON_COMBAT_LIVE_DELETE");
               if (NativeDeDeletionScope.BOSS_DEATH_AUDIT_ENABLE()) {
                  NativeDeDeletionScope._log()
                     .info(
                        "[BossDeathAudit] event=BOSS_REMOVE cause={} npcId={} name=\"{}\" nameId=\"{}\" objectId={} hp={}/{} dead={} mapId={} x={} y={} showId={} spawnTable={} spawnId={} spawnNumber={} timedExist={} remainingExistSeconds={}",
                        var73,
                        this.getNpcId(),
                        sourceAuditText(this.getName()),
                        sourceAuditText(this.getNameId()),
                        this.getId(),
                        this.getCurrentHp(),
                        this.getMaxHp(),
                        this.isDead(),
                        Integer.valueOf(this.getMapId()),
                        this.getX(),
                        this.getY(),
                        this.get_showId(),
                        this._spawn != null ? this._spawn.getSpawnConfigTable() : "none",
                        this._spawn != null ? this._spawn.getId() : -1,
                        this.getSpawnNumber(),
                        this.is_spawnTime(),
                        this.get_spawnTime()
                     );
               }

               if (this._spawn != null) {
                  NativeDeDeletionPorts.Boss.get().onBossDeath(this._spawn, this.getNameId());
               }
            }
         } catch (Exception var46) {
            NativeDeDeletionScope._log().error(var46.getLocalizedMessage(), var46);
         }

         try {
            NativeDeDeletionPorts.MobGroup var74 = this.getMobGroupInfo();
            if (var74 != null && var74.removeMember(this) == 0) {
               this.setMobGroupInfo(null);
            }
         } catch (RuntimeException var45) {
            NativeDeDeletionScope._log().warn("[NpcLifecycle] mob group cleanup failed: objectId={} npcId={}", this.getId(), this.getNpcId(), var45);
         }

         boolean var75 = !var69.isObjectRegistered(this);
         if (!var75) {
            long var79 = NativeDeDeletionScope.DELETE_VERIFY_FAILURE_COUNT().incrementAndGet();
            this.sourceFinishDeletionObservation(NativeDeDeletionVisibility.Phase.FAILED);
            NativeDeDeletionScope._log()
               .error(
                  "[NpcLifecycle] removal incomplete; respawn suppressed to prevent duplicate slot: objectId={} npcId={} spawnTable={} spawnId={} spawnNumber={} count={}",
                  this.getId(),
                  this.getNpcId(),
                  this._spawn != null ? this._spawn.getSpawnConfigTable() : "none",
                  this._spawn != null ? this._spawn.getId() : -1,
                  this._spawnNumber,
                  var79
               );
         } else {
            if (this._spawn != null) {
               this._spawn.releaseActiveNpc(this);
            }

            if (this.isReSpawn()) {
               if (this._spawn == null) {
                  long var78 = NativeDeDeletionScope.DELETE_VERIFY_FAILURE_COUNT().incrementAndGet();
                  NativeDeDeletionScope._log()
                     .error(
                        "[NpcLifecycle] respawn requested without spawn owner: objectId={} npcId={} spawnNumber={} count={}",
                        this.getId(),
                        this.getNpcId(),
                        this._spawnNumber,
                        var78
                     );
               } else {
                  this._spawn.executeSpawnTask(this._spawnNumber, 0);
               }
            }

            this.sourceFinishDeletionObservation(NativeDeDeletionVisibility.Phase.TERMINAL);
         }
      }
   }

   private static String sourceAuditText(String var0) {
      return var0 == null ? "" : var0.replace('\r', ' ').replace('\n', ' ');
   }

   private final void sourceAbortFailedWorldRegistration() {
      NativeDeDeletionScope.require(this);
      ((NativeDeNpcVitalState)this).deletionClaimFlag().set(true);
      this._destroyed = true;
      this._deletionVisibility.registrationAborted();
      NativeDeDeletionScope.DELETIONS_IN_FLIGHT().remove(this.getId(), this);
      NativeDeDeletionPorts.DeadTimer.get().untrack(this);
      NativeDeDeletionPorts.DeleteTimer.get().untrack(this);
      NativeDeDeletionPorts.RestTimer.remove(this);
      NativeDeDeletionPorts.WorkTimer.remove(this);
      NativeDeDeletionPorts.DigestTimer.remove(this);
      if (this instanceof NativeDeDeletionPorts.Monster var1) {
         NativeDeDeletionPorts.HprTimer.remove(var1);
         NativeDeDeletionPorts.MprTimer.remove(var1);
      }

      this.cancelNpcChatTimer();
   }

   public final void abortFailedWorldRegistration() {
      NativeDeDeletionScope.run(this.deletionOwner, this::sourceAbortFailedWorldRegistration);
   }
}
