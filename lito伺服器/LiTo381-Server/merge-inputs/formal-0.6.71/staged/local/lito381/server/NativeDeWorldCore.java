// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Map.Entry;
import local.lito381.server.NativeDeDeletionPorts.Monster;
import local.lito381.server.NativeDeDeletionPorts.ObjectRef;
import local.lito381.server.NativeDeDeletionPorts.Summon;
import local.lito381.server.NativeDeDeletionPorts.Viewer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import local.lito381.server.NativeDeDeletionDeState;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeDeletionState;
import local.lito381.server.NativeDeWorldAccess;
import local.lito381.server.NativeDeWorldKinds;
import local.lito381.server.NativeDeWorldLists;
import local.lito381.server.NativeDeWorldLocation;
import local.lito381.server.NativeDeWorldMap;
import local.lito381.server.NativeDeWorldMaps;
import local.lito381.server.NativeDeWorldPlayerFilter;
import local.lito381.server.NativeDeWorldPoint;
import local.lito381.server.NativeDeWorldRegistryBow;
import local.lito381.server.NativeDeWorldRegistryCrown;
import local.lito381.server.NativeDeWorldRegistryDarkelf;
import local.lito381.server.NativeDeWorldRegistryDe;
import local.lito381.server.NativeDeWorldRegistryDoll;
import local.lito381.server.NativeDeWorldRegistryDragonKnight;
import local.lito381.server.NativeDeWorldRegistryEffect;
import local.lito381.server.NativeDeWorldRegistryElf;
import local.lito381.server.NativeDeWorldRegistryIllusionist;
import local.lito381.server.NativeDeWorldRegistryItem;
import local.lito381.server.NativeDeWorldRegistryKnight;
import local.lito381.server.NativeDeWorldRegistryMob;
import local.lito381.server.NativeDeWorldRegistryNpc;
import local.lito381.server.NativeDeWorldRegistryPet;
import local.lito381.server.NativeDeWorldRegistryQuest;
import local.lito381.server.NativeDeWorldRegistrySummons;
import local.lito381.server.NativeDeWorldRegistryTrap;
import local.lito381.server.NativeDeWorldRegistryWizard;
import local.lito381.server.NativeDeWorldScope;

public class NativeDeWorldCore {
    private int _weather = 4;
    private boolean _worldChatEnabled = true;
    private volatile boolean _processingContributionTotal = false;
    private final ConcurrentHashMap<String, NativeDeDeletionPorts.Viewer> _allPlayers = new ConcurrentHashMap();
    private final ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef> _allObjects = new ConcurrentHashMap();
    private final List<Object> _allWars;
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef>> _visibleObjects = new ConcurrentHashMap();
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef>>> _spatialIndex = new ConcurrentHashMap();
    private final ConcurrentHashMap<Integer, VisibleBinding> _visibleBindings = new ConcurrentHashMap();
    public static final int SPATIAL_SECTOR_SHIFT = 5;
    private static final int MAX_SECTOR_RING = 4;
    private Collection<NativeDeDeletionPorts.ObjectRef> _allValues;
    private Collection<NativeDeDeletionPorts.Viewer> _allPlayerValues;
    private List<Object> _allWarList;

    public NativeDeWorldCore() {
        this._allWars = NativeDeWorldLists.newConcurrentList();
        for (Integer mapid : NativeDeWorldMaps.get().getMaps().keySet()) {
            ConcurrentHashMap map = new ConcurrentHashMap();
            this._visibleObjects.put(mapid, map);
        }
        NativeDeWorldScope._log().info("\u904a\u6232\u4e16\u754c\u5132\u5b58\u4e2d\u5fc3\u5efa\u7acb\u5b8c\u6210!!!");
    }

    public static int getSectorId(int x, int y) {
        return x >> 5 << 16 | y >> 5;
    }

    public static int sectorRingForRadius(int radius) {
        if (radius < 0) {
            return 1;
        }
        if (radius == 0) {
            return 0;
        }
        return (radius >>> 5) + 1;
    }

    private int resolveSectorRing(int radius) {
        int ring = NativeDeWorldCore.sectorRingForRadius(radius);
        if (ring > 4) {
            NativeDeWorldScope._log().warn("[World] radius={} \u63a8\u7b97 sectorRing={} \u8d85\u904e MAX_SECTOR_RING={}\uff0c\u5df2 clamp", (Object)radius, (Object)ring, (Object)4);
            ring = 4;
        }
        return ring;
    }

    private ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef> sliceForStructuralAccess(int mapId) {
        ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef> existing = this._visibleObjects.get(mapId);
        if (existing != null) {
            return existing;
        }
        if (!NativeDeWorldMaps.get().hasMap(mapId)) {
            return null;
        }
        return this._visibleObjects.computeIfAbsent(mapId, k -> new ConcurrentHashMap());
    }

    public void storeObject(NativeDeDeletionPorts.ObjectRef object) {
        try {
            if (object == null) {
                throw new NullPointerException();
            }
            this._allObjects.put(NativeDeWorldAccess.L1Object$getId(object), object);
            if (object instanceof NativeDeWorldKinds.Item) {
                NativeDeWorldKinds.Item item = (NativeDeWorldKinds.Item)object;
                NativeDeWorldRegistryItem.get().put(NativeDeWorldAccess.L1ItemInstance$getId(item), item);
            }
            if (object instanceof NativeDeDeletionPorts.Viewer) {
                NativeDeDeletionPorts.Viewer pc = (NativeDeDeletionPorts.Viewer)object;
                if (NativeDeWorldAccess.L1PcInstance$isCrown(pc)) {
                    NativeDeWorldRegistryCrown.get().put(NativeDeWorldAccess.L1PcInstance$getId(pc), pc);
                } else if (NativeDeWorldAccess.L1PcInstance$isKnight(pc)) {
                    NativeDeWorldRegistryKnight.get().put(NativeDeWorldAccess.L1PcInstance$getId(pc), pc);
                } else if (NativeDeWorldAccess.L1PcInstance$isElf(pc)) {
                    NativeDeWorldRegistryElf.get().put(NativeDeWorldAccess.L1PcInstance$getId(pc), pc);
                } else if (NativeDeWorldAccess.L1PcInstance$isWizard(pc)) {
                    NativeDeWorldRegistryWizard.get().put(NativeDeWorldAccess.L1PcInstance$getId(pc), pc);
                } else if (NativeDeWorldAccess.L1PcInstance$isDarkelf(pc)) {
                    NativeDeWorldRegistryDarkelf.get().put(NativeDeWorldAccess.L1PcInstance$getId(pc), pc);
                } else if (NativeDeWorldAccess.L1PcInstance$isDragonKnight(pc)) {
                    NativeDeWorldRegistryDragonKnight.get().put(NativeDeWorldAccess.L1PcInstance$getId(pc), pc);
                } else if (NativeDeWorldAccess.L1PcInstance$isIllusionist(pc)) {
                    NativeDeWorldRegistryIllusionist.get().put(NativeDeWorldAccess.L1PcInstance$getId(pc), pc);
                }
                this._allPlayers.put(NativeDeWorldAccess.L1PcInstance$getName(pc), pc);
            }
            if (object instanceof NativeDeDeletionDeState) {
                NativeDeDeletionDeState de = (NativeDeDeletionDeState)object;
                NativeDeWorldRegistryDe.get().put(NativeDeWorldAccess.L1DeInstance$getNameId(de), de);
            }
            if (object instanceof NativeDeWorldKinds.Trap) {
                NativeDeWorldKinds.Trap trap = (NativeDeWorldKinds.Trap)object;
                NativeDeWorldRegistryTrap.get().put(NativeDeWorldAccess.L1TrapInstance$getId(trap), trap);
            }
            if (object instanceof NativeDeWorldKinds.Pet) {
                NativeDeWorldKinds.Pet pet = (NativeDeWorldKinds.Pet)object;
                NativeDeWorldRegistryPet.get().put(NativeDeWorldAccess.L1PetInstance$getId(pet), pet);
            }
            if (object instanceof NativeDeDeletionPorts.Summon) {
                NativeDeDeletionPorts.Summon summon = (NativeDeDeletionPorts.Summon)object;
                NativeDeWorldRegistrySummons.get().put(NativeDeWorldAccess.L1SummonInstance$getId(summon), summon);
            }
            if (object instanceof NativeDeWorldKinds.Doll) {
                NativeDeWorldKinds.Doll doll = (NativeDeWorldKinds.Doll)object;
                NativeDeWorldRegistryDoll.get().put(NativeDeWorldAccess.L1DollInstance$getId(doll), doll);
            }
            if (object instanceof NativeDeWorldKinds.Effect) {
                NativeDeWorldKinds.Effect effect = (NativeDeWorldKinds.Effect)object;
                NativeDeWorldRegistryEffect.get().put(NativeDeWorldAccess.L1EffectInstance$getId(effect), effect);
            }
            if (object instanceof NativeDeDeletionPorts.Monster) {
                NativeDeDeletionPorts.Monster mob = (NativeDeDeletionPorts.Monster)object;
                NativeDeWorldRegistryMob.get().put(NativeDeWorldAccess.L1MonsterInstance$getId(mob), mob);
            }
            if (object instanceof NativeDeWorldKinds.Bow) {
                NativeDeWorldKinds.Bow bow = (NativeDeWorldKinds.Bow)object;
                NativeDeWorldRegistryBow.get().put(NativeDeWorldAccess.L1BowInstance$getId(bow), bow);
            }
            if (object instanceof NativeDeDeletionState) {
                NativeDeDeletionState npc = (NativeDeDeletionState)object;
                NativeDeWorldRegistryNpc.get().put(NativeDeWorldAccess.L1NpcInstance$getId(npc), npc);
            }
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public void removeObject(NativeDeDeletionPorts.ObjectRef object) {
        try {
            if (object == null) {
                throw new NullPointerException();
            }
            this._allObjects.remove(NativeDeWorldAccess.L1Object$getId(object), object);
            this.removeVisibleObject(object);
            if (object instanceof NativeDeWorldKinds.Item) {
                NativeDeWorldKinds.Item item = (NativeDeWorldKinds.Item)object;
                NativeDeWorldRegistryItem.get().map().remove(NativeDeWorldAccess.L1ItemInstance$getId(item), item);
            }
            if (object instanceof NativeDeDeletionPorts.Viewer) {
                NativeDeDeletionPorts.Viewer pc = (NativeDeDeletionPorts.Viewer)object;
                if (NativeDeWorldAccess.L1PcInstance$isCrown(pc)) {
                    NativeDeWorldRegistryCrown.get().map().remove(NativeDeWorldAccess.L1PcInstance$getId(pc), pc);
                } else if (NativeDeWorldAccess.L1PcInstance$isKnight(pc)) {
                    NativeDeWorldRegistryKnight.get().map().remove(NativeDeWorldAccess.L1PcInstance$getId(pc), pc);
                } else if (NativeDeWorldAccess.L1PcInstance$isElf(pc)) {
                    NativeDeWorldRegistryElf.get().map().remove(NativeDeWorldAccess.L1PcInstance$getId(pc), pc);
                } else if (NativeDeWorldAccess.L1PcInstance$isWizard(pc)) {
                    NativeDeWorldRegistryWizard.get().map().remove(NativeDeWorldAccess.L1PcInstance$getId(pc), pc);
                } else if (NativeDeWorldAccess.L1PcInstance$isDarkelf(pc)) {
                    NativeDeWorldRegistryDarkelf.get().map().remove(NativeDeWorldAccess.L1PcInstance$getId(pc), pc);
                } else if (NativeDeWorldAccess.L1PcInstance$isDragonKnight(pc)) {
                    NativeDeWorldRegistryDragonKnight.get().map().remove(NativeDeWorldAccess.L1PcInstance$getId(pc), pc);
                } else if (NativeDeWorldAccess.L1PcInstance$isIllusionist(pc)) {
                    NativeDeWorldRegistryIllusionist.get().map().remove(NativeDeWorldAccess.L1PcInstance$getId(pc), pc);
                }
                this._allPlayers.remove(NativeDeWorldAccess.L1PcInstance$getName(pc), pc);
            }
            if (object instanceof NativeDeDeletionDeState) {
                NativeDeDeletionDeState de = (NativeDeDeletionDeState)object;
                NativeDeWorldRegistryDe.get().map().remove(NativeDeWorldAccess.L1DeInstance$getNameId(de), de);
            }
            if (object instanceof NativeDeWorldKinds.Trap) {
                NativeDeWorldKinds.Trap trap = (NativeDeWorldKinds.Trap)object;
                NativeDeWorldRegistryTrap.get().remove(NativeDeWorldAccess.L1TrapInstance$getId(trap), trap);
            }
            if (object instanceof NativeDeWorldKinds.Pet) {
                NativeDeWorldKinds.Pet pet = (NativeDeWorldKinds.Pet)object;
                NativeDeWorldRegistryPet.get().map().remove(NativeDeWorldAccess.L1PetInstance$getId(pet), pet);
            }
            if (object instanceof NativeDeDeletionPorts.Summon) {
                NativeDeDeletionPorts.Summon summon = (NativeDeDeletionPorts.Summon)object;
                NativeDeWorldRegistrySummons.get().map().remove(NativeDeWorldAccess.L1SummonInstance$getId(summon), summon);
            }
            if (object instanceof NativeDeWorldKinds.Doll) {
                NativeDeWorldKinds.Doll doll = (NativeDeWorldKinds.Doll)object;
                NativeDeWorldRegistryDoll.get().map().remove(NativeDeWorldAccess.L1DollInstance$getId(doll), doll);
            }
            if (object instanceof NativeDeWorldKinds.Effect) {
                NativeDeWorldKinds.Effect effect = (NativeDeWorldKinds.Effect)object;
                NativeDeWorldRegistryEffect.get().map().remove(NativeDeWorldAccess.L1EffectInstance$getId(effect), effect);
            }
            if (object instanceof NativeDeDeletionPorts.Monster) {
                NativeDeDeletionPorts.Monster mob = (NativeDeDeletionPorts.Monster)object;
                NativeDeWorldRegistryMob.get().map().remove(NativeDeWorldAccess.L1MonsterInstance$getId(mob), mob);
            }
            if (object instanceof NativeDeWorldKinds.Bow) {
                NativeDeWorldKinds.Bow bow = (NativeDeWorldKinds.Bow)object;
                NativeDeWorldRegistryBow.get().map().remove(NativeDeWorldAccess.L1BowInstance$getId(bow), bow);
            }
            if (object instanceof NativeDeDeletionState) {
                NativeDeDeletionState npc = (NativeDeDeletionState)object;
                NativeDeWorldRegistryNpc.get().map().remove(NativeDeWorldAccess.L1NpcInstance$getId(npc), npc);
            }
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public NativeDeDeletionPorts.ObjectRef findObject(int oID) {
        if (oID == 0) {
            return null;
        }
        return this._allObjects.get(oID);
    }

    public Collection<NativeDeDeletionPorts.ObjectRef> getObject() {
        try {
            Collection<NativeDeDeletionPorts.ObjectRef> vs = this._allValues;
            return vs != null ? vs : (this._allValues = Collections.unmodifiableCollection(this._allObjects.values()));
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
            return null;
        }
    }

    public boolean addVisibleObject(NativeDeDeletionPorts.ObjectRef object) {
        try {
            if (object == null) {
                return false;
            }
            short mapId = NativeDeWorldAccess.L1Object$getMapId(object);
            if (this.bindVisibleObject(object, mapId, NativeDeWorldAccess.L1Object$getX(object), NativeDeWorldAccess.L1Object$getY(object))) {
                return true;
            }
            this.rollbackFailedVisibleRegistration(object, mapId, null);
            return false;
        }
        catch (Exception e) {
            this.rollbackFailedVisibleRegistration(object, object != null ? (int)NativeDeWorldAccess.L1Object$getMapId(object) : -1, e);
            return false;
        }
    }

    private void rollbackFailedVisibleRegistration(NativeDeDeletionPorts.ObjectRef object, int mapId, Throwable failure) {
        long failures = NativeDeWorldScope.VISIBLE_REGISTRATION_FAILURE_COUNT().incrementAndGet();
        if (failure == null) {
            NativeDeWorldScope._log().error("[WorldLifecycle] visible registration failed: mapId={} objId={} type={} count={}", (Object)mapId, (Object)(object != null ? NativeDeWorldAccess.L1Object$getId(object) : -1), (Object)(object != null ? object.getClass().getSimpleName() : "null"), (Object)failures);
        } else {
            NativeDeWorldScope._log().error("[WorldLifecycle] visible registration threw: mapId={} objId={} type={} count={}", (Object)mapId, (Object)(object != null ? NativeDeWorldAccess.L1Object$getId(object) : -1), (Object)(object != null ? object.getClass().getSimpleName() : "null"), (Object)failures, (Object)failure);
        }
        if (object == null) {
            return;
        }
        this.removeObject(object);
        if (object instanceof NativeDeDeletionState) {
            NativeDeDeletionState npc = (NativeDeDeletionState)object;
            NativeDeWorldRegistryQuest.get().rollbackNpcRegistration(NativeDeWorldAccess.L1NpcInstance$get_showId(npc), npc);
            NativeDeWorldAccess.L1NpcInstance$abortFailedWorldRegistration(npc);
        }
        if (this.isObjectRegistered(object)) {
            long rollbackFailures = NativeDeWorldScope.VISIBLE_REGISTRATION_ROLLBACK_FAILURE_COUNT().incrementAndGet();
            NativeDeWorldScope._log().error("[WorldLifecycle] registration rollback incomplete: mapId={} objId={} type={} count={}", (Object)mapId, (Object)NativeDeWorldAccess.L1Object$getId(object), (Object)object.getClass().getSimpleName(), (Object)rollbackFailures);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void removeVisibleObject(ObjectRef object) {
      try {
         if (object == null) {
            return;
         }

         synchronized (object) {
            boolean[] removed = new boolean[]{false};
            this._visibleBindings.compute(NativeDeWorldAccess.L1Object$getId(object), (objectId, binding) -> {
               if (binding == null || binding.object() != object) {
                  return binding;
               }
               this.removeVisibleMapEntry(binding.mapId(), object);
               this.spatialIndexRemoveObjectAtSector(binding.mapId(), binding.sectorId(), object);
               removed[0] = true;
               return null;
            });
            if (!removed[0]) {
               int mapId = NativeDeWorldAccess.L1Object$getMapId(object);
               ConcurrentHashMap<Integer, ObjectRef> mapSlice = this._visibleObjects.get(mapId);
               if (mapSlice != null && mapSlice.remove(NativeDeWorldAccess.L1Object$getId(object), object)) {
                  this.spatialIndexRemoveObjectAt(mapId, NativeDeWorldAccess.L1Object$getX(object), NativeDeWorldAccess.L1Object$getY(object), object);
                  this.logBindingRepair(
                     "remove-unbound", object, mapId, getSectorId(NativeDeWorldAccess.L1Object$getX(object), NativeDeWorldAccess.L1Object$getY(object))
                  );
               }
            }
         }
      } catch (Exception var8) {
         NativeDeWorldScope._log().error(var8.getLocalizedMessage(), (Throwable)var8);
      }
   }

    public boolean isObjectRegistered(NativeDeDeletionPorts.ObjectRef object) {
        if (object == null) {
            return false;
        }
        if (this._allObjects.get(NativeDeWorldAccess.L1Object$getId(object)) == object) {
            return true;
        }
        VisibleBinding binding = this._visibleBindings.get(NativeDeWorldAccess.L1Object$getId(object));
        return binding != null && binding.object() == object;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean moveVisibleObject(ObjectRef object, int newMapId) {
      if (object == null) {
         return false;
      } else {
         synchronized (object) {
            return this.relocateVisibleObject(object, NativeDeWorldAccess.L1Object$getX(object), NativeDeWorldAccess.L1Object$getY(object), newMapId);
         }
      }
   }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean relocateVisibleObject(ObjectRef object, int newX, int newY, int newMapId) {
      try {
         if (object == null) {
            return false;
         } else if (this.sliceForStructuralAccess(newMapId) == null) {
            NativeDeWorldScope._log().error("relocateVisibleObject 中止：目標地圖無分槽 dstMapId={} objId={}", newMapId, NativeDeWorldAccess.L1Object$getId(object));
            return false;
         } else {
            synchronized (object) {
               NativeDeWorldAccess.L1Object$setLocation(object, newX, newY, newMapId);
               if (this.isVisibleBindingOwner(object)) {
                  this.bindVisibleObject(object, newMapId, newX, newY);
               }
            }

            return true;
         }
      } catch (Exception var8) {
         NativeDeWorldScope._log().error(var8.getLocalizedMessage(), (Throwable)var8);
         return false;
      }
   }

    public void updateObjectSector(NativeDeDeletionPorts.ObjectRef obj, int oldX, int oldY, int oldMapId, int newX, int newY, int newMapId) {
        if (obj == null || NativeDeWorldAccess.L1Object$getId(obj) == 0) {
            return;
        }
        try {
            VisibleBinding binding = this._visibleBindings.get(NativeDeWorldAccess.L1Object$getId(obj));
            if (!(binding != null && binding.object() == obj || this.isObjectInVisibleMap(obj, oldMapId) || this.isObjectInVisibleMap(obj, newMapId))) {
                return;
            }
            int oldSectorId = NativeDeWorldCore.getSectorId(oldX, oldY);
            int newSectorId = NativeDeWorldCore.getSectorId(newX, newY);
            if (binding != null && binding.object() == obj && oldMapId == newMapId && oldSectorId == newSectorId && binding.mapId() == newMapId && binding.sectorId() == newSectorId) {
                return;
            }
            if (!this.bindVisibleObject(obj, newMapId, newX, newY)) {
                NativeDeWorldScope._log().error("[SpatialIndex] relocation failed: objectId={} type={} old={},{},{} new={},{},{}", (Object)NativeDeWorldAccess.L1Object$getId(obj), (Object)obj.getClass().getSimpleName(), (Object)oldMapId, (Object)oldX, (Object)oldY, (Object)newMapId, (Object)newX, (Object)newY);
            }
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    private boolean isVisibleBindingOwner(NativeDeDeletionPorts.ObjectRef object) {
        VisibleBinding binding = this._visibleBindings.get(NativeDeWorldAccess.L1Object$getId(object));
        return binding != null && binding.object() == object;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean bindVisibleObject(NativeDeDeletionPorts.ObjectRef object, int mapId, int x, int y) {
        if (object == null || NativeDeWorldAccess.L1Object$getId(object) == 0) {
            return false;
        }
        synchronized (object) {
            ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef> destination = this.sliceForStructuralAccess(mapId);
            if (destination == null) {
                return false;
            }
            int sectorId = NativeDeWorldCore.getSectorId(x, y);
            VisibleBinding observed = this._visibleBindings.get(NativeDeWorldAccess.L1Object$getId(object));
            if (observed != null && observed.object() == object && observed.mapId() == mapId && observed.sectorId() == sectorId && destination.get(NativeDeWorldAccess.L1Object$getId(object)) == object && this.isSpatialEntryAt(object, mapId, sectorId)) {
                return true;
            }
            boolean[] success = new boolean[]{false};
            this._visibleBindings.compute(NativeDeWorldAccess.L1Object$getId(object), (objectId, previous) -> {
                if (previous != null && previous.object() != object) {
                    if (this._allObjects.get(objectId) != object) {
                        long conflicts = NativeDeWorldScope.SPATIAL_BINDING_CONFLICT_COUNT().incrementAndGet();
                        NativeDeWorldScope._log().error("[SpatialIndex] binding conflict rejected: objectId={} existingType={} requestedType={} count={}", objectId, (Object)previous.object().getClass().getSimpleName(), (Object)object.getClass().getSimpleName(), (Object)conflicts);
                        return previous;
                    }
                    this.removeVisibleMapEntry(previous.mapId(), previous.object());
                    this.spatialIndexRemoveObjectAtSector(previous.mapId(), previous.sectorId(), previous.object());
                    this.logBindingRepair("replace-instance", object, mapId, sectorId);
                } else if (previous != null && (previous.mapId() != mapId || previous.sectorId() != sectorId)) {
                    this.removeVisibleMapEntry(previous.mapId(), object);
                    this.spatialIndexRemoveObjectAtSector(previous.mapId(), previous.sectorId(), object);
                } else if (previous != null && destination.get(objectId) == object && this.isSpatialEntryAt(object, mapId, sectorId)) {
                    success[0] = true;
                    return previous;
                }
                destination.put((Integer)objectId, object);
                this.spatialIndexPutObjectAtSector(object, mapId, sectorId);
                success[0] = true;
                return new VisibleBinding(object, mapId, sectorId);
            });
            return success[0];
        }
    }

    private void removeVisibleMapEntry(int mapId, NativeDeDeletionPorts.ObjectRef object) {
        ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef> map = this._visibleObjects.get(mapId);
        if (map != null) {
            map.remove(NativeDeWorldAccess.L1Object$getId(object), object);
        }
    }

    private boolean isSpatialEntryAt(NativeDeDeletionPorts.ObjectRef object, int mapId, int sectorId) {
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef>> sectors = this._spatialIndex.get(mapId);
        if (sectors == null) {
            return false;
        }
        ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef> bucket = sectors.get(sectorId);
        return bucket != null && bucket.get(NativeDeWorldAccess.L1Object$getId(object)) == object;
    }

    private boolean isObjectInVisibleMap(NativeDeDeletionPorts.ObjectRef obj, int mapId) {
        ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef> m = this._visibleObjects.get(mapId);
        return m != null && m.get(NativeDeWorldAccess.L1Object$getId(obj)) == obj;
    }

    private void spatialIndexPutObjectAtSector(NativeDeDeletionPorts.ObjectRef object, int mapId, int sectorId) {
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef>> mapSectors = this._spatialIndex.computeIfAbsent(mapId, k -> new ConcurrentHashMap());
        mapSectors.compute(sectorId, (key, bucket) -> {
            ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef> target = bucket != null ? bucket : new ConcurrentHashMap();
            target.put(NativeDeWorldAccess.L1Object$getId(object), object);
            return target;
        });
    }

    private void spatialIndexRemoveObjectAt(int mapId, int x, int y, NativeDeDeletionPorts.ObjectRef expectedObject) {
        if (expectedObject == null || NativeDeWorldAccess.L1Object$getId(expectedObject) == 0) {
            return;
        }
        int sectorId = NativeDeWorldCore.getSectorId(x, y);
        this.spatialIndexRemoveObjectAtSector(mapId, sectorId, expectedObject);
    }

    private void spatialIndexRemoveObjectAtSector(int mapId, int sectorId, NativeDeDeletionPorts.ObjectRef expectedObject) {
        if (expectedObject == null || NativeDeWorldAccess.L1Object$getId(expectedObject) == 0) {
            return;
        }
        int objectId = NativeDeWorldAccess.L1Object$getId(expectedObject);
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef>> mapSectors = this._spatialIndex.get(mapId);
        if (mapSectors == null) {
            return;
        }
        mapSectors.computeIfPresent(sectorId, (key, bucket) -> {
            bucket.remove(objectId, expectedObject);
            return bucket.isEmpty() ? null : bucket;
        });
    }

    private void collectSpatialCandidates(int mapId, int centerX, int centerY, int sectorRing, ArrayList<NativeDeDeletionPorts.ObjectRef> out) {
        if (out == null) {
            return;
        }
        this.forEachSpatialObject(mapId, centerX, centerY, sectorRing, out::add);
    }

    private void forEachSpatialObject(int mapId, int centerX, int centerY, int sectorRing, Consumer<NativeDeDeletionPorts.ObjectRef> action) {
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef>> mapSectors;
        if (action == null) {
            return;
        }
        if (sectorRing > 4) {
            NativeDeWorldScope._log().warn("[World] sectorRing={} \u8d85\u904e MAX_SECTOR_RING={}\uff0c\u5df2 clamp", (Object)sectorRing, (Object)4);
            sectorRing = 4;
        }
        mapSectors = this._spatialIndex.get(mapId);
        if (mapSectors == null || mapSectors.isEmpty()) {
            return;
        }
        int sx = centerX >> 5;
        int sy = centerY >> 5;
        if (sectorRing == 1) {
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dy = -1; dy <= 1; ++dy) {
                    this.emitSpatialSectorBucket(mapId, mapSectors, sx + dx, sy + dy, action);
                }
            }
            return;
        }
        for (int dx = -sectorRing; dx <= sectorRing; ++dx) {
            for (int dy = -sectorRing; dy <= sectorRing; ++dy) {
                this.emitSpatialSectorBucket(mapId, mapSectors, sx + dx, sy + dy, action);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void emitSpatialSectorBucket(int indexedMapId, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef>> mapSectors, int sectorX, int sectorY, Consumer<NativeDeDeletionPorts.ObjectRef> action) {
        int sectorId = sectorX << 16 | sectorY;
        ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef> bucket = mapSectors.get(sectorId);
        if (bucket == null || bucket.isEmpty()) {
            return;
        }
        for (NativeDeDeletionPorts.ObjectRef element : bucket.values()) {
            if (element == null) continue;
            VisibleBinding observed = this._visibleBindings.get(NativeDeWorldAccess.L1Object$getId(element));
            if (observed != null && observed.object() == element && observed.mapId() == indexedMapId && observed.sectorId() == sectorId) {
                action.accept(element);
                continue;
            }
            boolean staleEntry = false;
            synchronized (element) {
                VisibleBinding binding = this._visibleBindings.get(NativeDeWorldAccess.L1Object$getId(element));
                if (binding == null || binding.object() != element || binding.mapId() != indexedMapId || binding.sectorId() != sectorId) {
                    int actualMapId = NativeDeWorldAccess.L1Object$getMapId(element);
                    int actualSectorId = NativeDeWorldCore.getSectorId(NativeDeWorldAccess.L1Object$getX(element), NativeDeWorldAccess.L1Object$getY(element));
                    boolean removed = bucket.remove(NativeDeWorldAccess.L1Object$getId(element), element);
                    if (removed) {
                        long stale = NativeDeWorldScope.SPATIAL_STALE_ENTRY_COUNT().incrementAndGet();
                        if (element instanceof NativeDeDeletionPorts.Viewer) {
                            NativeDeWorldScope.SPATIAL_STALE_PLAYER_ENTRY_COUNT().incrementAndGet();
                        }
                        if ((stale & stale - 1L) == 0L) {
                            NativeDeWorldScope._log().warn("[SpatialIndex] removed stale entry after locked recheck: objectId={} type={} indexedMap={} indexedSector={},{} actualMap={} actualSector={},{} count={}", (Object)NativeDeWorldAccess.L1Object$getId(element), (Object)element.getClass().getSimpleName(), (Object)indexedMapId, (Object)sectorX, (Object)sectorY, (Object)actualMapId, (Object)(actualSectorId >> 16), (Object)((short)(actualSectorId & 0xFFFF)), (Object)stale);
                        }
                    }
                    SpatialReconcileResult reconcile = this.reconcileVisibleObject(element, indexedMapId);
                    switch (reconcile) {
                        case REPAIRED: {
                            this.logBindingRepair("traversal-reconcile", element, actualMapId, actualSectorId);
                            break;
                        }
                        case ALREADY_CONSISTENT:
                        case NOT_REGISTERED: {
                            if (!removed) {
                                NativeDeWorldScope.SPATIAL_TRAVERSAL_STALE_READ_COUNT().incrementAndGet();
                            }
                            break;
                        }
                        case FAILED: {
                            this.logReconcileFailure(element, indexedMapId, sectorId, actualMapId, actualSectorId);
                            break;
                        }
                        default: {
                            throw new IllegalStateException("Unhandled spatial reconcile result: " + String.valueOf((Object)reconcile));
                        }
                    }
                    staleEntry = true;
                }
            }
            if (staleEntry) continue;
            action.accept(element);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private NativeDeWorldCore.SpatialReconcileResult reconcileVisibleObject(ObjectRef object, int indexedMapId) {
      synchronized (object) {
         NativeDeWorldCore.VisibleBinding binding = this._visibleBindings.get(NativeDeWorldAccess.L1Object$getId(object));
         int actualMapId = NativeDeWorldAccess.L1Object$getMapId(object);
         int actualSectorId = getSectorId(NativeDeWorldAccess.L1Object$getX(object), NativeDeWorldAccess.L1Object$getY(object));
         if (binding != null
            && binding.object() == object
            && binding.mapId() == actualMapId
            && binding.sectorId() == actualSectorId
            && this.isObjectInVisibleMap(object, actualMapId)
            && this.isSpatialEntryAt(object, actualMapId, actualSectorId)) {
            return NativeDeWorldCore.SpatialReconcileResult.ALREADY_CONSISTENT;
         } else if ((binding == null || binding.object() != object)
            && !this.isObjectInVisibleMap(object, indexedMapId)
            && !this.isObjectInVisibleMap(object, actualMapId)) {
            return NativeDeWorldCore.SpatialReconcileResult.NOT_REGISTERED;
         } else {
            return this.bindVisibleObject(object, actualMapId, NativeDeWorldAccess.L1Object$getX(object), NativeDeWorldAccess.L1Object$getY(object))
               ? NativeDeWorldCore.SpatialReconcileResult.REPAIRED
               : NativeDeWorldCore.SpatialReconcileResult.FAILED;
         }
      }
   }

    private void logReconcileFailure(NativeDeDeletionPorts.ObjectRef object, int indexedMapId, int indexedSectorId, int actualMapId, int actualSectorId) {
        long failures = NativeDeWorldScope.SPATIAL_RECONCILE_FAILURE_COUNT().incrementAndGet();
        if ((failures & failures - 1L) == 0L) {
            NativeDeWorldScope._log().error("[SpatialIndex] reconcile failed: objectId={} type={} indexedMap={} indexedSector={},{} actualMap={} actualSector={},{} count={}", (Object)NativeDeWorldAccess.L1Object$getId(object), (Object)object.getClass().getSimpleName(), (Object)indexedMapId, (Object)(indexedSectorId >> 16), (Object)((short)(indexedSectorId & 0xFFFF)), (Object)actualMapId, (Object)(actualSectorId >> 16), (Object)((short)(actualSectorId & 0xFFFF)), (Object)failures);
        }
    }

    private void logBindingRepair(String reason, NativeDeDeletionPorts.ObjectRef object, int mapId, int sectorId) {
        long repairs = NativeDeWorldScope.SPATIAL_BINDING_REPAIR_COUNT().incrementAndGet();
        if ((repairs & repairs - 1L) == 0L) {
            NativeDeWorldScope._log().warn("[SpatialIndex] binding repaired: reason={} objectId={} type={} map={} sector={},{} count={}", (Object)reason, (Object)NativeDeWorldAccess.L1Object$getId(object), (Object)object.getClass().getSimpleName(), (Object)mapId, (Object)(sectorId >> 16), (Object)((short)(sectorId & 0xFFFF)), (Object)repairs);
        }
    }

    public String getSpatialIndexMonitorLine() {
        return "SpatialIndex staleRemoved=" + NativeDeWorldScope.SPATIAL_STALE_ENTRY_COUNT().get() + " stalePlayers=" + NativeDeWorldScope.SPATIAL_STALE_PLAYER_ENTRY_COUNT().get() + " traversalStaleReads=" + NativeDeWorldScope.SPATIAL_TRAVERSAL_STALE_READ_COUNT().get() + " bindingRepairs=" + NativeDeWorldScope.SPATIAL_BINDING_REPAIR_COUNT().get() + " bindingConflicts=" + NativeDeWorldScope.SPATIAL_BINDING_CONFLICT_COUNT().get() + " reconcileFailures=" + NativeDeWorldScope.SPATIAL_RECONCILE_FAILURE_COUNT().get() + " audits=" + NativeDeWorldScope.SPATIAL_AUDIT_COUNT().get() + " auditRepairs=" + NativeDeWorldScope.SPATIAL_AUDIT_REPAIR_COUNT().get() + " registrationFailures=" + NativeDeWorldScope.VISIBLE_REGISTRATION_FAILURE_COUNT().get() + " registrationRollbackFailures=" + NativeDeWorldScope.VISIBLE_REGISTRATION_ROLLBACK_FAILURE_COUNT().get();
    }

    public static long getVisibleRegistrationFailureCount() {
        return NativeDeWorldScope.VISIBLE_REGISTRATION_FAILURE_COUNT().get();
    }

    public static long getSpatialReconcileFailureCount() {
        return NativeDeWorldScope.SPATIAL_RECONCILE_FAILURE_COUNT().get();
    }

    public static long getVisibleRegistrationRollbackFailureCount() {
        return NativeDeWorldScope.VISIBLE_REGISTRATION_ROLLBACK_FAILURE_COUNT().get();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public SpatialIndexAuditSnapshot auditAndRepairSpatialIndex() {
        long sequence = NativeDeWorldScope.SPATIAL_AUDIT_COUNT().incrementAndGet();
        int checked = 0;
        int repaired = 0;
        for (VisibleBinding visibleBinding : this._visibleBindings.values()) {
            if (visibleBinding == null || visibleBinding.object() == null) continue;
            NativeDeDeletionPorts.ObjectRef object = visibleBinding.object();
            synchronized (object) {
                ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef> bucket2;
                VisibleBinding current = this._visibleBindings.get(NativeDeWorldAccess.L1Object$getId(object));
                if (current == null || current.object() != object) {
                    continue;
                }
                ++checked;
                int actualMapId = NativeDeWorldAccess.L1Object$getMapId(object);
                int actualSectorId = NativeDeWorldCore.getSectorId(NativeDeWorldAccess.L1Object$getX(object), NativeDeWorldAccess.L1Object$getY(object));
                ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef> visible = this._visibleObjects.get(actualMapId);
                ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef>> sectors = this._spatialIndex.get(actualMapId);
                bucket2 = sectors != null ? sectors.get(actualSectorId) : null;
                if (current.mapId() != actualMapId || current.sectorId() != actualSectorId || visible == null || visible.get(NativeDeWorldAccess.L1Object$getId(object)) != object || bucket2 == null || bucket2.get(NativeDeWorldAccess.L1Object$getId(object)) != object) {
                    SpatialReconcileResult reconcile = this.reconcileVisibleObject(object, current.mapId());
                    if (reconcile == SpatialReconcileResult.REPAIRED) {
                        ++repaired;
                        this.logBindingRepair("full-audit-binding", object, actualMapId, actualSectorId);
                    } else if (reconcile == SpatialReconcileResult.FAILED) {
                        this.logReconcileFailure(object, current.mapId(), current.sectorId(), actualMapId, actualSectorId);
                    }
                }
            }
        }
        for (Map.Entry entry : this._visibleObjects.entrySet()) {
            int indexedMapId = (Integer)entry.getKey();
            for (NativeDeDeletionPorts.ObjectRef object : ((ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef>)entry.getValue()).values()) {
                if (object == null) continue;
                synchronized (object) {
                    VisibleBinding binding = this._visibleBindings.get(NativeDeWorldAccess.L1Object$getId(object));
                    short actualMapId2 = NativeDeWorldAccess.L1Object$getMapId(object);
                    if (binding == null || binding.object() != object || binding.mapId() != indexedMapId || actualMapId2 != indexedMapId) {
                        SpatialReconcileResult reconcile;
                        int actualSectorId = NativeDeWorldCore.getSectorId(NativeDeWorldAccess.L1Object$getX(object), NativeDeWorldAccess.L1Object$getY(object));
                        if (actualMapId2 == indexedMapId) {
                            reconcile = this.reconcileVisibleObject(object, indexedMapId);
                            if (reconcile == SpatialReconcileResult.REPAIRED) {
                                ++repaired;
                                this.logBindingRepair("full-audit-visible", object, actualMapId2, actualSectorId);
                            } else if (reconcile == SpatialReconcileResult.FAILED) {
                                this.logReconcileFailure(object, indexedMapId, actualSectorId, actualMapId2, actualSectorId);
                            }
                            continue;
                        }
                        if (((ConcurrentHashMap)entry.getValue()).remove(NativeDeWorldAccess.L1Object$getId(object), object)) {
                            ++repaired;
                        }
                        reconcile = this.reconcileVisibleObject(object, indexedMapId);
                        if (reconcile == SpatialReconcileResult.REPAIRED) {
                            ++repaired;
                            this.logBindingRepair("full-audit-visible", object, actualMapId2, actualSectorId);
                        } else if (reconcile == SpatialReconcileResult.FAILED) {
                            this.logReconcileFailure(object, indexedMapId, actualSectorId, actualMapId2, actualSectorId);
                        }
                    }
                }
            }
        }
        for (Map.Entry entry : this._spatialIndex.entrySet()) {
            int indexedMapId = (Integer)entry.getKey();
            for (Map.Entry sectorEntry : ((ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef>>)entry.getValue()).entrySet()) {
                int indexedSectorId = (Integer)sectorEntry.getKey();
                for (NativeDeDeletionPorts.ObjectRef object : ((ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef>)sectorEntry.getValue()).values()) {
                    if (object == null) continue;
                    synchronized (object) {
                        VisibleBinding binding = this._visibleBindings.get(NativeDeWorldAccess.L1Object$getId(object));
                        int actualMapId = NativeDeWorldAccess.L1Object$getMapId(object);
                        int actualSectorId = NativeDeWorldCore.getSectorId(NativeDeWorldAccess.L1Object$getX(object), NativeDeWorldAccess.L1Object$getY(object));
                        if (binding == null || binding.object() != object || binding.mapId() != indexedMapId || binding.sectorId() != indexedSectorId || actualMapId != indexedMapId || actualSectorId != indexedSectorId) {
                            SpatialReconcileResult reconcile;
                            if (actualMapId == indexedMapId && actualSectorId == indexedSectorId) {
                                reconcile = this.reconcileVisibleObject(object, indexedMapId);
                                if (reconcile == SpatialReconcileResult.REPAIRED) {
                                    ++repaired;
                                    this.logBindingRepair("full-audit-spatial", object, actualMapId, actualSectorId);
                                } else if (reconcile == SpatialReconcileResult.FAILED) {
                                    this.logReconcileFailure(object, indexedMapId, indexedSectorId, actualMapId, actualSectorId);
                                }
                                continue;
                            }
                            if (((ConcurrentHashMap)sectorEntry.getValue()).remove(NativeDeWorldAccess.L1Object$getId(object), object)) {
                                ++repaired;
                            }
                            reconcile = this.reconcileVisibleObject(object, indexedMapId);
                            if (reconcile == SpatialReconcileResult.REPAIRED) {
                                ++repaired;
                                this.logBindingRepair("full-audit-spatial", object, actualMapId, actualSectorId);
                            } else if (reconcile == SpatialReconcileResult.FAILED) {
                                this.logReconcileFailure(object, indexedMapId, indexedSectorId, actualMapId, actualSectorId);
                            }
                        }
                    }
                }
                ((ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef>>)entry.getValue()).computeIfPresent(indexedSectorId, (key, bucket) -> bucket.isEmpty() ? null : bucket);
            }
        }
        if (repaired > 0) {
            NativeDeWorldScope.SPATIAL_AUDIT_REPAIR_COUNT().addAndGet(repaired);
            NativeDeWorldScope._log().warn("[SpatialIndex] full audit repaired entries: sequence={} checkedBindings={} repaired={}", (Object)sequence, (Object)checked, (Object)repaired);
        }
        return new SpatialIndexAuditSnapshot(sequence, checked, repaired);
    }

    private static boolean matchesNearbyRadius(NativeDeWorldPoint pt, NativeDeWorldPoint target, int radius) {
        return switch (radius) {
            case -1 -> pt.isInScreen(target);
            case 0 -> pt.isSamePoint(target);
            default -> pt.getTileLineDistance(target) <= radius;
        };
    }

    private static boolean passesShowIdForNearbyQuery(NativeDeDeletionPorts.ObjectRef src, NativeDeDeletionPorts.ObjectRef element) {
        if (NativeDeWorldAccess.L1Object$get_showId(src) == NativeDeWorldAccess.L1Object$get_showId(element)) {
            return true;
        }
        return element instanceof NativeDeWorldKinds.Merchant || element instanceof NativeDeWorldKinds.Dwarf;
    }

    private boolean hasVisibleObjectSlice(int mapId) {
        if (this._visibleObjects.get(mapId) != null) {
            return true;
        }
        if (mapId != 0) {
            NativeDeWorldScope._log().error("[World] \u627e\u4e0d\u5230\u5730\u5716\u53ef\u898b\u5206\u69fd\uff1aMapId={}\uff08\u9644\u8fd1\u67e5\u8a62\u4e2d\u6b62\uff09", (Object)mapId);
        }
        return false;
    }

    public ArrayList<NativeDeDeletionPorts.ObjectRef> getVisibleObjects(NativeDeDeletionPorts.ObjectRef object) {
        return this.getVisibleObjects(object, -1);
    }

    public boolean getVisibleObjects(NativeDeDeletionPorts.ObjectRef src, NativeDeDeletionPorts.ObjectRef tg) {
        if (NativeDeWorldAccess.L1Object$get_showId(src) != NativeDeWorldAccess.L1Object$get_showId(tg)) {
            return false;
        }
        if (NativeDeWorldAccess.L1Object$getMapId(src) != NativeDeWorldAccess.L1Object$getMapId(tg)) {
            return false;
        }
        if (NativeDeWorldAccess.L1Object$getLocation(src).isInScreen(NativeDeWorldAccess.L1Object$getLocation(tg))) {
            return true;
        }
        return false;
    }

    public ArrayList<NativeDeDeletionPorts.ObjectRef> getVisibleObjects(NativeDeDeletionPorts.ObjectRef object, int radius) {
        NativeDeWorldMap map = NativeDeWorldAccess.L1Object$getMap(object);
        NativeDeWorldPoint pt = NativeDeWorldAccess.L1Object$getLocation(object);
        ArrayList<NativeDeDeletionPorts.ObjectRef> result = new ArrayList<NativeDeDeletionPorts.ObjectRef>();
        int mapId = map.getId();
        ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef> mapSrc = this._visibleObjects.get(mapId);
        if (mapSrc == null) {
            if (mapId != 0) {
                NativeDeWorldScope._log().error("[World] \u627e\u4e0d\u5230\u5730\u5716\u53ef\u898b\u5206\u69fd\uff1aMapId={} \u7269\u4ef6 objId={}\uff08\u53ef\u898b\u5217\u8209\u4e2d\u6b62\uff09", (Object)mapId, (Object)NativeDeWorldAccess.L1Object$getId(object));
            }
            return result;
        }
        ArrayList<NativeDeDeletionPorts.ObjectRef> candidates = new ArrayList<NativeDeDeletionPorts.ObjectRef>();
        this.collectSpatialCandidates(mapId, pt.getX(), pt.getY(), this.resolveSectorRing(radius), candidates);
        try {
            for (NativeDeDeletionPorts.ObjectRef element : candidates) {
                if (element.equals(object)) continue;
                if (NativeDeWorldAccess.L1Object$get_showId(object) != NativeDeWorldAccess.L1Object$get_showId(element)) {
                    boolean isShow = false;
                    if (element instanceof NativeDeWorldKinds.Merchant || element instanceof NativeDeWorldKinds.Dwarf) {
                        isShow = true;
                    }
                    if (!isShow) continue;
                }
                if (mapId != NativeDeWorldAccess.L1Object$getMapId(element)) continue;
                switch (radius) {
                    case -1:
                        if (pt.isInScreen(NativeDeWorldAccess.L1Object$getLocation(element))) {
                            result.add(element);
                        }
                        break;
                    case 0:
                        if (pt.isSamePoint(NativeDeWorldAccess.L1Object$getLocation(element))) {
                            result.add(element);
                        }
                        break;
                    default:
                        if (pt.getTileLineDistance(NativeDeWorldAccess.L1Object$getLocation(element)) <= radius) {
                            result.add(element);
                        }
                        break;
                }
            }
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error("[World] getVisibleObjects \u8ff4\u5708\u7570\u5e38", e);
        }
        return result;
    }

    public ArrayList<ObjectRef> getVisiblePoint(NativeDeWorldLocation loc, int radius, int showid) {
        ArrayList<ObjectRef> result = new ArrayList<>();
        int mapId = loc.getMapId();
        if (this.sliceForStructuralAccess(mapId) == null) {
            NativeDeWorldScope._log().error("遊戲世界儲存中心並未建立該地圖編號資料檔案: " + mapId);
            return result;
        } else {
            ArrayList<ObjectRef> candidates = new ArrayList<>();
            this.collectSpatialCandidates(mapId, loc.getX(), loc.getY(), this.resolveSectorRing(radius), candidates);

            for (ObjectRef element : candidates) {
                if (mapId != NativeDeWorldAccess.L1Object$getMapId(element)) {
                    continue;
                }
                if (showid != NativeDeWorldAccess.L1Object$get_showId(element)) {
                    continue;
                }
                if (loc.getTileLineDistance(NativeDeWorldAccess.L1Object$getLocation(element)) <= radius) {
                    result.add(element);
                }
            }

            return result;
        }
    }

    public ArrayList<ObjectRef> queryNearbyObjects(ObjectRef src, int radius, Predicate<ObjectRef> filter) {
      ArrayList<ObjectRef> result = new ArrayList<>();
      if (src == null) {
         return result;
      } else {
         NativeDeWorldMap map = NativeDeWorldAccess.L1Object$getMap(src);
         if (map == null) {
            return result;
         } else {
            int mapId = map.getId();
            if (!this.hasVisibleObjectSlice(mapId)) {
               return result;
            } else {
               NativeDeWorldPoint pt = NativeDeWorldAccess.L1Object$getLocation(src);
               this.forEachSpatialObject(mapId, pt.getX(), pt.getY(), this.resolveSectorRing(radius), element -> {
                  if (element.equals(src)) {
                     return;
                  }
                  if (mapId != NativeDeWorldAccess.L1Object$getMapId(element)) {
                     return;
                  }
                  if (!passesShowIdForNearbyQuery(src, element)) {
                     return;
                  }
                  if (!matchesNearbyRadius(pt, NativeDeWorldAccess.L1Object$getLocation(element), radius)) {
                     return;
                  }
                  if (filter != null && !filter.test(element)) {
                     return;
                  }
                  result.add(element);
               });
               return result;
            }
         }
      }
   }

    public void forEachNearbyObject(NativeDeDeletionPorts.ObjectRef src, int radius, Predicate<NativeDeDeletionPorts.ObjectRef> filter, Consumer<NativeDeDeletionPorts.ObjectRef> action) {
        if (src == null || action == null) {
            return;
        }
        NativeDeWorldMap map = NativeDeWorldAccess.L1Object$getMap(src);
        if (map == null) {
            return;
        }
        int mapId = map.getId();
        if (!this.hasVisibleObjectSlice(mapId)) {
            return;
        }
        NativeDeWorldPoint pt = NativeDeWorldAccess.L1Object$getLocation(src);
        this.forEachSpatialObject(mapId, pt.getX(), pt.getY(), this.resolveSectorRing(radius), element -> {
            if (element.equals(src)) {
                return;
            }
            if (mapId != NativeDeWorldAccess.L1Object$getMapId(element)) {
                return;
            }
            if (!NativeDeWorldCore.passesShowIdForNearbyQuery(src, element)) {
                return;
            }
            if (!NativeDeWorldCore.matchesNearbyRadius(pt, NativeDeWorldAccess.L1Object$getLocation(element), radius)) {
                return;
            }
            if (filter != null && !filter.test((NativeDeDeletionPorts.ObjectRef)element)) {
                return;
            }
            action.accept((NativeDeDeletionPorts.ObjectRef)element);
        });
    }

    public ArrayList<NativeDeDeletionPorts.Viewer> queryNearbyPlayers(NativeDeDeletionPorts.ObjectRef src, int radius, NativeDeWorldPlayerFilter playerFilter) {
        ArrayList<NativeDeDeletionPorts.Viewer> result = new ArrayList<NativeDeDeletionPorts.Viewer>();
        this.forEachNearbyPlayer(src, radius, playerFilter, result::add);
        return result;
    }

    public void forEachNearbyPlayer(NativeDeDeletionPorts.ObjectRef src, int radius, NativeDeWorldPlayerFilter filter, Consumer<NativeDeDeletionPorts.Viewer> action) {
        if (src == null || action == null) {
            return;
        }
        NativeDeWorldMap map = NativeDeWorldAccess.L1Object$getMap(src);
        if (map == null) {
            return;
        }
        int mapId = map.getId();
        if (!this.hasVisibleObjectSlice(mapId)) {
            return;
        }
        NativeDeWorldPlayerFilter effectiveFilter = filter != null ? filter : NativeDeWorldPlayerFilter.NONE;
        NativeDeWorldPoint pt = NativeDeWorldAccess.L1Object$getLocation(src);
        this.forEachSpatialObject(mapId, pt.getX(), pt.getY(), this.resolveSectorRing(radius), element -> {
            if (!(element instanceof NativeDeDeletionPorts.Viewer pc)) {
                return;
            }
            if (element.equals(src)) {
                return;
            }
            if (mapId != NativeDeWorldAccess.L1Object$getMapId(element)) {
                return;
            }
            if (NativeDeWorldAccess.L1Object$get_showId(src) != NativeDeWorldAccess.L1Object$get_showId(element)) {
                return;
            }
            if (!NativeDeWorldCore.matchesNearbyRadius(pt, NativeDeWorldAccess.L1Object$getLocation(element), radius)) {
                return;
            }
            if (!effectiveFilter.accepts(pc)) {
                return;
            }
            action.accept(pc);
        });
    }

    public ArrayList<NativeDeDeletionPorts.Viewer> getVisiblePc(NativeDeWorldLocation loc) {
        ArrayList<NativeDeDeletionPorts.Viewer> result = new ArrayList<NativeDeDeletionPorts.Viewer>();
        if (loc == null) {
            return result;
        }
        int mapId = loc.getMapId();
        if (!this.hasVisibleObjectSlice(mapId)) {
            NativeDeWorldScope._log().error("\u904a\u6232\u4e16\u754c\u5132\u5b58\u4e2d\u5fc3\u4e26\u672a\u5efa\u7acb\u8a72\u5730\u5716\u7de8\u865f\u8cc7\u6599\u6a94\u6848: " + mapId);
            return result;
        }
        this.forEachSpatialObject(mapId, loc.getX(), loc.getY(), 1, candidate -> {
            if (!(candidate instanceof NativeDeDeletionPorts.Viewer element)) {
                return;
            }
            if (NativeDeWorldAccess.L1PcInstance$getOnlineStatus(element) == 2 || NativeDeWorldAccess.L1PcInstance$getOnlineStatus(element) == 3) {
                return;
            }
            if (mapId != NativeDeWorldAccess.L1PcInstance$getMapId(element)) {
                return;
            }
            if (loc.isInScreen(NativeDeWorldAccess.L1PcInstance$getLocation(element))) {
                result.add(element);
            }
        });
        return result;
    }

    public ArrayList<NativeDeDeletionPorts.Viewer> getVisiblePlayer(NativeDeDeletionPorts.ObjectRef object) {
        return this.getVisiblePlayer(object, -1);
    }

    public ArrayList<NativeDeDeletionPorts.Viewer> getVisiblePlayer(NativeDeDeletionPorts.ObjectRef object, int radius) {
        return this.queryNearbyPlayers(object, radius, NativeDeWorldPlayerFilter.BROADCAST);
    }

    public ArrayList<NativeDeDeletionPorts.Viewer> getRecognizePlayer(NativeDeDeletionPorts.ObjectRef object) {
        return this.getVisiblePlayer(object, NativeDeWorldScope.PC_RECOGNIZE_RANGE());
    }

    public Collection<NativeDeDeletionPorts.Viewer> getAllPlayers() {
        try {
            Collection<NativeDeDeletionPorts.Viewer> vs = this._allPlayerValues;
            return vs != null ? vs : (this._allPlayerValues = Collections.unmodifiableCollection(this._allPlayers.values()));
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
            return null;
        }
    }

    public Viewer getPlayer(String name) {
      if (name == null) {
         return null;
      } else {
         Viewer exact = this._allPlayers.get(name);
         if (exact != null) {
            return exact;
         } else {
            for (Viewer each : this.getAllPlayers()) {
               if (NativeDeWorldAccess.L1PcInstance$getName(each).equalsIgnoreCase(name)) {
                  return each;
               }
            }

            return null;
         }
      }
   }

    public final Map<Integer, NativeDeDeletionPorts.ObjectRef> getAllVisibleObjects() {
        return this._allObjects;
    }

    public final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef>> getVisibleObjects() {
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef>> snapshot = new ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef>>();
        for (Map.Entry<Integer, ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef>> entry : this._visibleObjects.entrySet()) {
            snapshot.put(entry.getKey(), new ConcurrentHashMap(entry.getValue()));
        }
        return snapshot;
    }

    public final ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef> getVisibleObjects(int mapId) {
        ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef> map = this._visibleObjects.get(mapId);
        return map != null ? new ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef>(map) : null;
    }

    public int countVisibleObjectsOnMap(int mapId, Predicate<NativeDeDeletionPorts.ObjectRef> filter) {
        ConcurrentHashMap<Integer, NativeDeDeletionPorts.ObjectRef> map = this._visibleObjects.get(mapId);
        if (map == null || map.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (NativeDeDeletionPorts.ObjectRef object : map.values()) {
            if (object == null || NativeDeWorldAccess.L1Object$getMapId(object) != mapId) {
                continue;
            }
            VisibleBinding binding = this._visibleBindings.get(NativeDeWorldAccess.L1Object$getId(object));
            if (binding == null || binding.object() != object || binding.mapId() != mapId) {
                continue;
            }
            if (filter == null || filter.test(object)) {
                ++count;
            }
        }
        return count;
    }

    public int countNearbyObjects(int mapId, int centerX, int centerY, int radius, Predicate<NativeDeDeletionPorts.ObjectRef> filter) {
        if (radius < 0) {
            return this.countVisibleObjectsOnMap(mapId, filter);
        }
        int[] count = new int[]{0};
        this.forEachSpatialObject(mapId, centerX, centerY, this.resolveSectorRing(radius), object -> {
            if (NativeDeWorldAccess.L1Object$getMapId(object) != mapId || Math.max(Math.abs(NativeDeWorldAccess.L1Object$getX(object) - centerX), Math.abs(NativeDeWorldAccess.L1Object$getY(object) - centerY)) > radius) {
                return;
            }
            if (filter == null || filter.test((NativeDeDeletionPorts.ObjectRef)object)) {
                count[0]++;
            }
        });
        return count[0];
    }

    public boolean hasVisibleObjectAt(int mapId, int x, int y, Predicate<NativeDeDeletionPorts.ObjectRef> filter) {
        boolean[] found = new boolean[]{false};
        this.forEachSpatialObject(mapId, x, y, 0, object -> {
            if (!found[0] && NativeDeWorldAccess.L1Object$getMapId(object) == mapId && NativeDeWorldAccess.L1Object$getX(object) == x && NativeDeWorldAccess.L1Object$getY(object) == y && (filter == null || filter.test((NativeDeDeletionPorts.ObjectRef)object))) {
                found[0] = true;
            }
        });
        return found[0];
    }

    public boolean hasPlacementBlockerAt(int mapId, int x, int y, Predicate<ObjectRef> filter) {
      ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, ObjectRef>> sectors = this._spatialIndex.get(mapId);
      ConcurrentHashMap<Integer, ObjectRef> bucket = sectors != null ? sectors.get(getSectorId(x, y)) : null;
      if (bucket == null) {
         return false;
      } else {
         for (ObjectRef object : bucket.values()) {
            if (NativeDeWorldAccess.L1Object$getMapId(object) == mapId
               && NativeDeWorldAccess.L1Object$getX(object) == x
               && NativeDeWorldAccess.L1Object$getY(object) == y
               && (filter == null || filter.test(object))) {
               return true;
            }
         }

         return false;
      }
   }

    private record VisibleBinding(NativeDeDeletionPorts.ObjectRef object, int mapId, int sectorId) {
    }

    private static enum SpatialReconcileResult {
        ALREADY_CONSISTENT,
        REPAIRED,
        NOT_REGISTERED,
        FAILED;

    }

    public record SpatialIndexAuditSnapshot(long sequence, int checkedBindings, int repairedEntries) {
    }
}
