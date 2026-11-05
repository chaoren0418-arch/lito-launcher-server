// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import local.lito381.server.NativeDeDeletionPorts.Hate;
import local.lito381.server.NativeDeDeletionPorts.Monster;
import local.lito381.server.NativeDeDeletionPorts.Summon;
import local.lito381.server.NativeDeDeletionPorts.Viewer;
import local.lito381.server.NativeDeNpcConstructionPorts.Party;
import local.lito381.server.NativeDeWorldKinds.Pet;

public class NativeDeNpcHateList implements Hate {
   private final Map<NativeDeCharacterFields, Integer> _hateMap;

   private NativeDeNpcHateList(Map<NativeDeCharacterFields, Integer> var1) {
      this._hateMap = var1;
   }

   public NativeDeNpcHateList() {
      this._hateMap = new ConcurrentHashMap<>();
   }

   public void add(NativeDeCharacterFields var1, int var2) {
      if (var1 != null) {
         this._hateMap.merge(var1, var2, Integer::sum);
      }
   }

   public boolean isHate(NativeDeCharacterFields var1) {
      return this._hateMap.containsKey(var1);
   }

   public int get(NativeDeCharacterFields var1) {
      Integer var2 = this._hateMap.get(var1);
      return var2 != null ? var2 : 0;
   }

   public boolean containsKey(NativeDeCharacterFields var1) {
      return this._hateMap.containsKey(var1);
   }

   public void remove(NativeDeCharacterFields var1) {
      if (var1 != null) {
         this._hateMap.remove(var1);
      }
   }

   @Override
   public void clear() {
      this._hateMap.clear();
   }

   public boolean isEmpty() {
      return this._hateMap.isEmpty();
   }

   public NativeDeCharacterFields getMaxHateCharacter() {
      NativeDeCharacterFields var1 = null;
      int var2 = Integer.MIN_VALUE;

      for (Entry var4 : this._hateMap.entrySet()) {
         NativeDeCharacterFields var5 = (NativeDeCharacterFields)var4.getKey();
         Integer var6 = (Integer)var4.getValue();
         if (var5 != null && var6 != null && var2 < var6) {
            var1 = var5;
            var2 = var6;
         }
      }

      return var1;
   }

   public void removeInvalidCharacter(NativeDeDeletionState var1) {
      for (NativeDeCharacterFields var3 : this._hateMap.keySet()) {
         if (var3 == null || var3.isDead()) { this._hateMap.remove(var3); continue; }
            if ((var1 instanceof Pet || var1 instanceof Summon) && var3 instanceof Monster var7) {
               if (var7.destroyed()
                  || NativeDeNpcConstructionGeometry.mapId(var7) != NativeDeNpcConstructionGeometry.mapId(var1)
                  || NativeDeNpcConstructionGeometry.showId(var7) != NativeDeNpcConstructionGeometry.showId(var1)
                  || NativeDeNpcConstructionGeometry.location(var1).getTileDistance(NativeDeNpcConstructionGeometry.location(var7)) > 30) {
                  this._hateMap.remove(var3);
               }
               continue;
            } else if (var3 instanceof Viewer var6) {
               if (NativeDeNpcConstructionGeometry.mapId(var6) != NativeDeNpcConstructionGeometry.mapId(var1)
                  || NativeDeNpcConstructionGeometry.showId(var6) != NativeDeNpcConstructionGeometry.showId(var1)) {
                  this._hateMap.remove(var3);
               }
               continue;
            } else if (var3 instanceof Pet var5) {
               if (NativeDeNpcConstructionGeometry.mapId(var5) != NativeDeNpcConstructionGeometry.mapId(var1)
                  || NativeDeNpcConstructionGeometry.showId(var5) != NativeDeNpcConstructionGeometry.showId(var1)) {
                  this._hateMap.remove(var3);
               }
               continue;
            } else if (var3 instanceof Summon var4) {
               if (NativeDeNpcConstructionGeometry.mapId(var4) != NativeDeNpcConstructionGeometry.mapId(var1)
                  || NativeDeNpcConstructionGeometry.showId(var4) != NativeDeNpcConstructionGeometry.showId(var1)) {
                  this._hateMap.remove(var3);
               }
               continue;
            } else if (!var1.knownsObject(var3)) {
               this._hateMap.remove(var3);
            }

      }
   }

   public int getTotalHate() {
      int var1 = 0;

      for (Integer var3 : this._hateMap.values()) {
         if (var3 != null) {
            var1 += var3;
         }
      }

      return var1;
   }

   public int getTotalLawfulHate() {
      int var1 = 0;

      for (Entry var3 : this._hateMap.entrySet()) {
         if (var3.getKey() instanceof Viewer && var3.getValue() != null) {
            var1 += (Integer)var3.getValue();
         }
      }

      return var1;
   }

   public int getPartyHate(Party var1) {
      int var2 = 0;

      for (Entry var4 : this._hateMap.entrySet()) {
         Viewer var5 = null;
         if (var4.getKey() instanceof Viewer) {
            var5 = (Viewer)var4.getKey();
         }

         if (var4.getKey() instanceof NativeDeDeletionState) {
            NativeDeCharacterFields var6 = ((NativeDeDeletionState)var4.getKey()).getMaster();
            if (var6 instanceof Viewer) {
               var5 = (Viewer)var6;
            }
         }

         if (var5 != null && var1.isMember(var5) && var4.getValue() != null) {
            var2 += (Integer)var4.getValue();
         }
      }

      return var2;
   }

   public int getPartyLawfulHate(Party var1) {
      int var2 = 0;

      for (Entry var4 : this._hateMap.entrySet()) {
         Viewer var5 = null;
         if (var4.getKey() instanceof Viewer) {
            var5 = (Viewer)var4.getKey();
         }

         if (var5 != null && var1.isMember(var5) && var4.getValue() != null) {
            var2 += (Integer)var4.getValue();
         }
      }

      return var2;
   }

   public NativeDeNpcHateList copy() {
      return new NativeDeNpcHateList(new ConcurrentHashMap<>(this._hateMap));
   }

   public Set<Entry<NativeDeCharacterFields, Integer>> entrySet() {
      return this._hateMap.entrySet();
   }

   public ArrayList<NativeDeCharacterFields> toTargetArrayList() {
      return new ArrayList<>(this._hateMap.keySet());
   }

   public ArrayList<Integer> toHateArrayList() {
      return new ArrayList<>(this._hateMap.values());
   }

   public void copyToPairedLists(ArrayList<NativeDeCharacterFields> var1, ArrayList<Integer> var2) {
      var1.clear();
      var2.clear();

      for (Entry var4 : this._hateMap.entrySet()) {
         var1.add((NativeDeCharacterFields)var4.getKey());
         Integer var5 = (Integer)var4.getValue();
         var2.add(var5 != null ? var5 : 0);
      }
   }
}
