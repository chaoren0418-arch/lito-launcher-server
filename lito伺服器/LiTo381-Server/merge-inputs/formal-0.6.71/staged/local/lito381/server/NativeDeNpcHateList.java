// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeDeletionState;
import local.lito381.server.NativeDeNpcConstructionGeometry;
import local.lito381.server.NativeDeNpcConstructionPorts;
import local.lito381.server.NativeDeWorldKinds;

public class NativeDeNpcHateList
implements NativeDeDeletionPorts.Hate {
    private final Map<NativeDeCharacterFields, Integer> _hateMap;

    private NativeDeNpcHateList(Map<NativeDeCharacterFields, Integer> map) {
        this._hateMap = map;
    }

    public NativeDeNpcHateList() {
        this._hateMap = new ConcurrentHashMap<NativeDeCharacterFields, Integer>();
    }

    public void add(NativeDeCharacterFields nativeDeCharacterFields, int n) {
        if (nativeDeCharacterFields == null) {
            return;
        }
        this._hateMap.merge(nativeDeCharacterFields, n, Integer::sum);
    }

    public boolean isHate(NativeDeCharacterFields nativeDeCharacterFields) {
        return this._hateMap.containsKey(nativeDeCharacterFields);
    }

    public int get(NativeDeCharacterFields nativeDeCharacterFields) {
        Integer n = this._hateMap.get(nativeDeCharacterFields);
        return n != null ? n : 0;
    }

    public boolean containsKey(NativeDeCharacterFields nativeDeCharacterFields) {
        return this._hateMap.containsKey(nativeDeCharacterFields);
    }

    public void remove(NativeDeCharacterFields nativeDeCharacterFields) {
        if (nativeDeCharacterFields == null) {
            return;
        }
        this._hateMap.remove(nativeDeCharacterFields);
    }

    @Override
    public void clear() {
        this._hateMap.clear();
    }

    public boolean isEmpty() {
        return this._hateMap.isEmpty();
    }

    public NativeDeCharacterFields getMaxHateCharacter() {
        NativeDeCharacterFields nativeDeCharacterFields = null;
        int n = Integer.MIN_VALUE;
        for (Map.Entry<NativeDeCharacterFields, Integer> entry : this._hateMap.entrySet()) {
            NativeDeCharacterFields nativeDeCharacterFields2 = entry.getKey();
            Integer n2 = entry.getValue();
            if (nativeDeCharacterFields2 == null || n2 == null || n >= n2) continue;
            nativeDeCharacterFields = nativeDeCharacterFields2;
            n = n2;
        }
        return nativeDeCharacterFields;
    }

    public void removeInvalidCharacter(NativeDeDeletionState nativeDeDeletionState) {
        for (NativeDeCharacterFields nativeDeCharacterFields : this._hateMap.keySet()) {
            NativeDeCharacterFields nativeDeCharacterFields2;
            if (nativeDeCharacterFields == null || nativeDeCharacterFields.isDead()) {
                this._hateMap.remove(nativeDeCharacterFields);
                continue;
            }
            if ((nativeDeDeletionState instanceof NativeDeWorldKinds.Pet || nativeDeDeletionState instanceof NativeDeDeletionPorts.Summon) && nativeDeCharacterFields instanceof NativeDeDeletionPorts.Monster) {
                NativeDeDeletionPorts.Monster monster = (NativeDeDeletionPorts.Monster)nativeDeCharacterFields;
                nativeDeCharacterFields2 = monster;
                if (!monster.destroyed() && NativeDeNpcConstructionGeometry.mapId(monster) == NativeDeNpcConstructionGeometry.mapId(nativeDeDeletionState) && NativeDeNpcConstructionGeometry.showId(monster) == NativeDeNpcConstructionGeometry.showId(nativeDeDeletionState) && NativeDeNpcConstructionGeometry.location(nativeDeDeletionState).getTileDistance(NativeDeNpcConstructionGeometry.location(monster)) <= 30) continue;
                this._hateMap.remove(nativeDeCharacterFields);
                continue;
            }
            if (nativeDeCharacterFields instanceof NativeDeDeletionPorts.Viewer) {
                nativeDeCharacterFields2 = (NativeDeDeletionPorts.Viewer)nativeDeCharacterFields;
                if (NativeDeNpcConstructionGeometry.mapId(nativeDeCharacterFields2) == NativeDeNpcConstructionGeometry.mapId(nativeDeDeletionState) && NativeDeNpcConstructionGeometry.showId(nativeDeCharacterFields2) == NativeDeNpcConstructionGeometry.showId(nativeDeDeletionState)) continue;
                this._hateMap.remove(nativeDeCharacterFields);
                continue;
            }
            if (nativeDeCharacterFields instanceof NativeDeWorldKinds.Pet) {
                nativeDeCharacterFields2 = (NativeDeWorldKinds.Pet)nativeDeCharacterFields;
                if (NativeDeNpcConstructionGeometry.mapId(nativeDeCharacterFields2) == NativeDeNpcConstructionGeometry.mapId(nativeDeDeletionState) && NativeDeNpcConstructionGeometry.showId(nativeDeCharacterFields2) == NativeDeNpcConstructionGeometry.showId(nativeDeDeletionState)) continue;
                this._hateMap.remove(nativeDeCharacterFields);
                continue;
            }
            if (nativeDeCharacterFields instanceof NativeDeDeletionPorts.Summon) {
                nativeDeCharacterFields2 = (NativeDeDeletionPorts.Summon)nativeDeCharacterFields;
                if (NativeDeNpcConstructionGeometry.mapId(nativeDeCharacterFields2) == NativeDeNpcConstructionGeometry.mapId(nativeDeDeletionState) && NativeDeNpcConstructionGeometry.showId(nativeDeCharacterFields2) == NativeDeNpcConstructionGeometry.showId(nativeDeDeletionState)) continue;
                this._hateMap.remove(nativeDeCharacterFields);
                continue;
            }
            if (nativeDeDeletionState.knownsObject(nativeDeCharacterFields)) continue;
            this._hateMap.remove(nativeDeCharacterFields);
        }
    }

    public int getTotalHate() {
        int n = 0;
        for (Integer n2 : this._hateMap.values()) {
            if (n2 == null) continue;
            n += n2.intValue();
        }
        return n;
    }

    public int getTotalLawfulHate() {
        int n = 0;
        for (Map.Entry<NativeDeCharacterFields, Integer> entry : this._hateMap.entrySet()) {
            if (!(entry.getKey() instanceof NativeDeDeletionPorts.Viewer) || entry.getValue() == null) continue;
            n += entry.getValue().intValue();
        }
        return n;
    }

    public int getPartyHate(NativeDeNpcConstructionPorts.Party party) {
        int n = 0;
        for (Map.Entry<NativeDeCharacterFields, Integer> entry : this._hateMap.entrySet()) {
            NativeDeCharacterFields nativeDeCharacterFields;
            NativeDeDeletionPorts.Viewer viewer = null;
            if (entry.getKey() instanceof NativeDeDeletionPorts.Viewer) {
                viewer = (NativeDeDeletionPorts.Viewer)entry.getKey();
            }
            if (entry.getKey() instanceof NativeDeDeletionState && (nativeDeCharacterFields = ((NativeDeDeletionState)entry.getKey()).getMaster()) instanceof NativeDeDeletionPorts.Viewer) {
                viewer = (NativeDeDeletionPorts.Viewer)nativeDeCharacterFields;
            }
            if (viewer == null || !party.isMember(viewer) || entry.getValue() == null) continue;
            n += entry.getValue().intValue();
        }
        return n;
    }

    public int getPartyLawfulHate(NativeDeNpcConstructionPorts.Party party) {
        int n = 0;
        for (Map.Entry<NativeDeCharacterFields, Integer> entry : this._hateMap.entrySet()) {
            NativeDeDeletionPorts.Viewer viewer = null;
            if (entry.getKey() instanceof NativeDeDeletionPorts.Viewer) {
                viewer = (NativeDeDeletionPorts.Viewer)entry.getKey();
            }
            if (viewer == null || !party.isMember(viewer) || entry.getValue() == null) continue;
            n += entry.getValue().intValue();
        }
        return n;
    }

    public NativeDeNpcHateList copy() {
        return new NativeDeNpcHateList(new ConcurrentHashMap<NativeDeCharacterFields, Integer>(this._hateMap));
    }

    public Set<Map.Entry<NativeDeCharacterFields, Integer>> entrySet() {
        return this._hateMap.entrySet();
    }

    public ArrayList<NativeDeCharacterFields> toTargetArrayList() {
        return new ArrayList<NativeDeCharacterFields>(this._hateMap.keySet());
    }

    public ArrayList<Integer> toHateArrayList() {
        return new ArrayList<Integer>(this._hateMap.values());
    }

    public void copyToPairedLists(ArrayList<NativeDeCharacterFields> arrayList, ArrayList<Integer> arrayList2) {
        arrayList.clear();
        arrayList2.clear();
        for (Map.Entry<NativeDeCharacterFields, Integer> entry : this._hateMap.entrySet()) {
            arrayList.add(entry.getKey());
            Integer n = entry.getValue();
            arrayList2.add(n != null ? n : 0);
        }
    }
}
