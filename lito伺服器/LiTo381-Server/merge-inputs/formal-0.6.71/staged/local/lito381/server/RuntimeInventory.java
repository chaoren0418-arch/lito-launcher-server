// ORIGINAL SOURCE aligned to base-0.6.71.jar (formal behavior restored from reference JAR)
package local.lito381.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

final class RuntimeInventory {
   static final int MAX_SLOTS = 256;
   static final int BLESSING_BLESSED = 0;
   static final int BLESSING_NORMAL = 1;
   static final int BLESSING_CURSED = 2;
   static final int BLESSING_UNIDENTIFIED = 3;
   static final int NATIVE_RING_SLOT_LIMIT = 2;
   private static final long FIRST_OBJECT_ID = 1879048192L;
   private static final AtomicLong NEXT_OBJECT_ID = new AtomicLong(1879048192L);
   private Map<Long, RuntimeInventory.Item> items;
   private final Map<Long, SourceWeaponPhysicalStats> sourcePhysicalWeapons;

   RuntimeInventory() {
      this.items = new LinkedHashMap<>();
      this.sourcePhysicalWeapons = new LinkedHashMap<>();
   }

   private RuntimeInventory(Map<Long, RuntimeInventory.Item> var1, Map<Long, SourceWeaponPhysicalStats> var2) {
      this.items = var1;
      this.sourcePhysicalWeapons = var2 == null ? new LinkedHashMap<>() : new LinkedHashMap<>(var2);
   }

   static RuntimeInventory fromCatalogCounts(Map<Long, Integer> var0) {
      return fromCatalogCounts(var0, Map.of(), RuntimeInventory::nextObjectId);
   }

   static RuntimeInventory fromCatalogCounts(Map<Long, Integer> var0, LongSupplier var1) {
      return fromCatalogCounts(var0, Map.of(), var1);
   }

   static RuntimeInventory fromCatalogCounts(Map<Long, Integer> var0, Map<Long, List<Integer>> var1) {
      return fromCatalogCounts(var0, var1, RuntimeInventory::nextObjectId);
   }

   static RuntimeInventory fromCatalogCounts(Map<Long, Integer> var0, Map<Long, List<Integer>> var1, LongSupplier var2) {
      RuntimeInventory var3 = new RuntimeInventory();
      if (var0 == null) {
         return var3;
      } else {
         for (Entry var5 : var0.entrySet()) {
            long var6 = var5.getKey() == null ? 0L : (Long)var5.getKey();
            int var8 = var5.getValue() == null ? 0 : (Integer)var5.getValue();
            if (var6 > 0L && var6 <= 4294967295L && var8 > 0) {
               InventoryCatalog.Definition var9 = InventoryCatalog.find(var6);
               if (var9 == null) {
                  var9 = InventoryCatalog.retainedUnknown(var6);
               }

               if (!var3.add(var9, (long)var8, var2)) {
                  throw new IllegalStateException("durable inventory exceeds native 256-slot capacity");
               }
            }
         }

         var3.applyCatalogEnchantments(var1);
         return var3;
      }
   }

   static RuntimeInventory fromDurableState(Map<Long, Integer> var0, Map<Long, List<RuntimeInventory.PersistentItem>> var1) {
      return fromDurableState(var0, var1, Map.of(), RuntimeInventory::nextObjectId);
   }

   static RuntimeInventory fromDurableState(
      Map<Long, Integer> var0, Map<Long, List<RuntimeInventory.PersistentItem>> var1, Map<Long, InventoryCatalog.Definition> var2
   ) {
      return fromDurableState(var0, var1, var2, Map.of(), RuntimeInventory::nextObjectId);
   }

   static RuntimeInventory fromDurableState(
      Map<Long, Integer> var0,
      Map<Long, List<RuntimeInventory.PersistentItem>> var1,
      Map<Long, InventoryCatalog.Definition> var2,
      Map<Long, SourceWeaponPhysicalStats> var3
   ) {
      return fromDurableState(var0, var1, var2, var3, RuntimeInventory::nextObjectId);
   }

   static RuntimeInventory fromDurableState(Map<Long, Integer> var0, Map<Long, List<RuntimeInventory.PersistentItem>> var1, LongSupplier var2) {
      return fromDurableState(var0, var1, Map.of(), var2);
   }

   static RuntimeInventory fromDurableState(
      Map<Long, Integer> var0, Map<Long, List<RuntimeInventory.PersistentItem>> var1, Map<Long, InventoryCatalog.Definition> var2, LongSupplier var3
   ) {
      return fromDurableState(var0, var1, var2, Map.of(), var3);
   }

   static RuntimeInventory fromDurableState(
      Map<Long, Integer> var0,
      Map<Long, List<RuntimeInventory.PersistentItem>> var1,
      Map<Long, InventoryCatalog.Definition> var2,
      Map<Long, SourceWeaponPhysicalStats> var3,
      LongSupplier var4
   ) {
      RuntimeInventory var5 = new RuntimeInventory(new LinkedHashMap<>(), var3);
      if (var0 == null) {
         var5.validateSourcePhysicalWeapons();
         return var5;
      } else {
         for (Entry var7 : var0.entrySet()) {
            long var8 = var7.getKey() == null ? 0L : (Long)var7.getKey();
            int var10 = var7.getValue() == null ? 0 : (Integer)var7.getValue();
            if (var8 > 0L && var8 <= 4294967295L && var10 > 0) {
               InventoryCatalog.Definition var11 = var2 == null ? null : (InventoryCatalog.Definition)var2.get(var8);
               if (var11 != null && var11.catalogId() != var8) {
                  throw new IllegalArgumentException("source inventory definition/catalog id mismatch");
               }

               if (var11 == null) {
                  var11 = InventoryCatalog.find(var8);
               }

               if (var11 == null) {
                  var11 = InventoryCatalog.retainedUnknown(var8);
               }

               long var12 = 0L;
               List var14 = var1 == null ? null : (List)var1.get(var8);
               if (var14 != null) {
                  Iterator var15 = var14.iterator();

                  label117:
                  while (true) {
                     RuntimeInventory.PersistentItem var16;
                     InventoryCatalog.Definition var17;
                     while (true) {
                        if (!var15.hasNext()) {
                           break label117;
                        }

                        var16 = (RuntimeInventory.PersistentItem)var15.next();

                        try {
                           var17 = var16 == null ? null : var16.effectiveDefinition(var11, var8);
                           break;
                        } catch (IllegalArgumentException var19) {
                        }
                     }

                     if (var16 != null && var12 + var16.quantity() <= var10 && (var17.stackable() || var16.quantity() == 1)) {
                        boolean var18 = var16.objectId() == 0L ? var5.addState(var17, var16, var4) : var5.addDistinctState(var17, var16, var4) != 0L;
                        if (!var18) {
                           throw new IllegalStateException("durable inventory state exceeds native capacity");
                        }

                        var12 += var16.quantity();
                     }
                  }
               }

               if (var12 < var10 && !var5.add(var11, var10 - var12, var4)) {
                  throw new IllegalStateException("durable inventory exceeds native 256-slot capacity");
               }
            }
         }

         var5.validateSourcePhysicalWeapons();
         return var5;
      }
   }

   synchronized RuntimeInventory copy() {
      return NativeDeItemInventory.preserveCopy(this, new RuntimeInventory(new LinkedHashMap<>(this.items), this.sourcePhysicalWeapons));
   }

   synchronized RuntimeInventory sourceEquipmentOnly() {
      LinkedHashMap var1 = new LinkedHashMap();

      for (Entry var3 : this.items.entrySet()) {
         RuntimeInventory.Item var4 = (RuntimeInventory.Item)var3.getValue();
         if (var4.sourceBound() && (var4.definition().weapon() || var4.definition().armor())
            || var4.tournamentReward()
            || BattlePetRaceRewardCatalog.isDurablePrize(var4.definition().catalogId())) {
            var1.put((Long)var3.getKey(), var4);
         }
      }

      LinkedHashMap var6 = new LinkedHashMap();

      for (Entry var9 : this.sourcePhysicalWeapons.entrySet()) {
         if (var1.containsKey(var9.getKey())) {
            SourceWeaponPhysicalStats var5 = (SourceWeaponPhysicalStats)var9.getValue();
            var6.put((Long)var9.getKey(), var5.ranged() ? var5.withoutProjectile() : var5);
         }
      }

      RuntimeInventory var8 = new RuntimeInventory(var1, var6);
      var8.validateSourcePhysicalWeapons();
      return NativeDeItemInventory.preserveCopy(this, var8);
   }

   synchronized RuntimeInventory withoutObjects(Set<Long> var1) {
      if (var1 != null && !var1.isEmpty()) {
         LinkedHashMap var2 = new LinkedHashMap<>(this.items);

         for (Long var4 : var1) {
            if (var4 != null) {
               var2.remove(var4);
            }
         }

         LinkedHashMap var6 = new LinkedHashMap<>(this.sourcePhysicalWeapons);

         for (Long var5 : var1) {
            if (var5 != null) {
               var6.remove(var5);
            }
         }

         return NativeDeItemInventory.preserveCopy(this, new RuntimeInventory(var2, var6));
      } else {
         return NativeDeItemInventory.preserveCopy(this, this.copy());
      }
   }

   private synchronized void validateSourcePhysicalWeapons() {
      for (Entry var2 : this.sourcePhysicalWeapons.entrySet()) {
         RuntimeInventory.Item var3 = this.items.get(var2.getKey());
         SourceWeaponPhysicalStats var4 = (SourceWeaponPhysicalStats)var2.getValue();
         if (var3 == null
            || var4 == null
            || !var4.present()
            || !var3.sourceBound()
            || !var3.definition().weapon()
            || var3.definition().catalogId() != var4.itemId()
            || var3.enchantLevel() != (byte)var4.enchantLevel()) {
            throw new IllegalArgumentException("invalid per-instance selected-source weapon provenance");
         }
      }
   }

   synchronized List<RuntimeInventory.Item> items() {
      return List.copyOf(this.items.values());
   }

   synchronized long countCatalog(long var1) {
      return var1 <= 0L
         ? 0L
         : this.items.values().stream().filter(var2 -> var2.definition().catalogId() == var1).mapToLong(RuntimeInventory.Item::quantity).sum();
   }

   synchronized RuntimeInventory.Item item(long var1) {
      return this.items.get(var1);
   }

   synchronized int size() {
      return this.items.size();
   }

   synchronized int totalWeight() {
      int var1 = 0;

      for (RuntimeInventory.Item var3 : this.items.values()) {
         var1 = BattleInventoryWeight.addWeight(var1, BattleInventoryWeight.itemWeight(var3.quantity(), var3.definition().unitWeight()));
      }

      return var1;
   }

   synchronized void clear() {
      this.items.clear();
   }

   synchronized void shuffle(Random var1) {
      if (!NativeDeItemInventory.shuffle(this, var1)) {
         if (var1 == null) {
            throw new IllegalArgumentException("missing inventory random");
         } else {
            ArrayList<Entry<Long, RuntimeInventory.Item>> var2 = new ArrayList<>(this.items.entrySet());
            Collections.shuffle(var2, var1);
            this.items.clear();

            for (Entry<Long, RuntimeInventory.Item> var4 : var2) {
               this.items.put((Long)var4.getKey(), (RuntimeInventory.Item)var4.getValue());
            }
         }
      }
   }

   synchronized boolean add(InventoryCatalog.Definition var1, long var2) {
      return this.add(var1, var2, RuntimeInventory::nextObjectId);
   }

   synchronized boolean add(InventoryCatalog.Definition var1, long var2, LongSupplier var4) {
      return var1 != null && var2 > 0L && var2 <= 2147483647L && var4 != null
         ? this.addState(var1, RuntimeInventory.PersistentItem.defaults(var1, (int)var2), var4)
         : false;
   }

   synchronized boolean addTournamentReward(InventoryCatalog.Definition var1, long var2) {
      return this.addTournamentReward(var1, var2, RuntimeInventory::nextObjectId);
   }

   synchronized boolean addTournamentReward(InventoryCatalog.Definition var1, long var2, LongSupplier var4) {
      if (var1 != null && var2 > 0L && var2 <= 2147483647L && var4 != null) {
         RuntimeInventory.PersistentItem var5 = new RuntimeInventory.PersistentItem(
            0L, (int)var2, 0, 0, var1.identified(), var1.blessing(), false, true, false, null, var1.initialChargeCount()
         );
         return this.addState(var1, var5, var4);
      } else {
         return false;
      }
   }

   synchronized boolean addSystemReward(InventoryCatalog.Definition var1, long var2, int var4) {
      if (var1 != null && var2 > 0L && var2 <= 2147483647L && var4 >= -128 && var4 <= 127 && (var4 == 0 || var1.weapon() || var1.armor())) {
         RuntimeInventory.PersistentItem var5 = new RuntimeInventory.PersistentItem(
            0L, (int)var2, var4, 0, true, var1.blessing(), false, false, false, null, var1.initialChargeCount(), 0L, false, false
         );
         return this.addState(var1, var5, RuntimeInventory::nextObjectId);
      } else {
         return false;
      }
   }

   synchronized long addDistinctState(InventoryCatalog.Definition var1, RuntimeInventory.PersistentItem var2) {
      return this.addDistinctState(var1, var2, RuntimeInventory::nextObjectId);
   }

   synchronized long addDistinctState(InventoryCatalog.Definition var1, RuntimeInventory.PersistentItem var2, LongSupplier var3) {
      if (var1 != null
         && var2 != null
         && var3 != null
         && var2.quantity() > 0
         && (var1.stackable() || var2.quantity() == 1)
         && (var2.durability() <= 0 && var2.enchantLevel() == 0 || var1.weapon() || var1.armor())
         && (!var2.equipped() || equipStateSupported(var1))
         && this.items.size() < 256) {
         long var4 = var2.objectId() == 0L ? this.uniqueObjectId(var3) : this.admittedStableObjectId(var2.objectId());
         if (var4 == 0L) {
            return 0L;
         } else {
            this.items
               .put(
                  var4,
                  new RuntimeInventory.Item(
                     var4,
                     var1,
                     var2.quantity(),
                     var2.equipped(),
                     var2.durability(),
                     var2.enchantLevel(),
                     var2.identified(),
                     var2.blessing(),
                     var2.sourceBound(),
                     var2.tournamentReward(),
                     var2.chargeCount(),
                     var2.expiresAtEpochMillis(),
                     var2.sealed(),
                     var2.litoOwned()
                  )
               );
            return var4;
         }
      } else {
         return 0L;
      }
   }

   synchronized boolean add(RuntimeInventory.Item var1, long var2) {
      return this.add(var1, var2, RuntimeInventory::nextObjectId);
   }

   synchronized boolean add(RuntimeInventory.Item var1, long var2, LongSupplier var4) {
      return var1 != null && var2 > 0L && var2 <= var1.quantity() && var2 <= 2147483647L && var4 != null
         ? this.addState(
            var1.definition(),
            new RuntimeInventory.PersistentItem(
               var2 == var1.quantity() ? var1.objectId() : 0L,
               (int)var2,
               var1.enchantLevel(),
               var1.durability(),
               var1.identified(),
               var1.blessing(),
               var1.sourceBound(),
               var1.tournamentReward(),
               var1.equipped(),
               durableDefinitionOverride(var1.definition()),
               var1.chargeCount(),
               var1.expiresAtEpochMillis(),
               var1.sealed(),
               var1.litoOwned() || var2 < var1.quantity() && var1.sourceBound()
            ),
            var4
         )
         : false;
   }

   synchronized boolean addTransferred(RuntimeInventory.Item var1, long var2) {
      return this.addTransferred(var1, var2, RuntimeInventory::nextObjectId);
   }

   synchronized RuntimeInventory.Item addTransferredResult(RuntimeInventory.Item var1, long var2) {
      return this.addTransferredResult(var1, var2, RuntimeInventory::nextObjectId);
   }

   synchronized RuntimeInventory.Item addTransferredResult(RuntimeInventory.Item var1, long var2, LongSupplier var4) {
      return this.addTransferResult(var1, var2, var4, true);
   }

   synchronized RuntimeInventory.Item addGroundTransferredResult(RuntimeInventory.Item var1, long var2) {
      return this.addTransferResult(var1, var2, RuntimeInventory::nextObjectId, false);
   }

   synchronized boolean addGroundTransferred(RuntimeInventory.Item var1, long var2) {
      return this.addGroundTransferredResult(var1, var2) != null;
   }

   private synchronized RuntimeInventory.Item addTransferResult(RuntimeInventory.Item var1, long var2, LongSupplier var4, boolean var5) {
      if (var1 != null && var2 > 0L && var2 <= var1.quantity() && var2 <= 2147483647L && var4 != null && (!var5 || var1.transferable())) {
         RuntimeInventory.Item var6 = var1.definition().stackable()
            ? this.items
               .values()
               .stream()
               .filter(var1x -> var1x.definition().catalogId() == var1.definition().catalogId())
               .filter(var1x -> var1x.definition().equals(var1.definition()))
               .filter(var1x -> var1x.tournamentReward() == var1.tournamentReward())
               .findFirst()
               .orElse(null)
            : null;
         if (var6 != null) {
            if (var6.objectId() == var1.objectId()) {
               return null;
            } else {
               long var10 = var6.quantity() + var2;
               if (var10 > 2147483647L) {
                  return null;
               } else {
                  RuntimeInventory.Item var12 = var6.withQuantity((int)var10);
                  this.items.put(var6.objectId(), var12);
                  return var12;
               }
            }
         } else {
            Set var7 = Set.copyOf(this.items.keySet());
            if (!this.add(var1, var2, var4)) {
               return null;
            } else {
               if (var1.quantity() == var2 && !var7.contains(var1.objectId())) {
                  RuntimeInventory.Item var8 = this.items.get(var1.objectId());
                  if (var8 != null) {
                     return var8;
                  }
               }

               for (RuntimeInventory.Item var9 : this.items.values()) {
                  if (!var7.contains(var9.objectId())) {
                     return var9;
                  }
               }

               throw new IllegalStateException("transferred item admitted without destination object");
            }
         }
      } else {
         return null;
      }
   }

   synchronized boolean addTransferred(RuntimeInventory.Item var1, long var2, LongSupplier var4) {
      return this.addTransferredResult(var1, var2, var4) != null;
   }

   private synchronized boolean addState(InventoryCatalog.Definition var1, RuntimeInventory.PersistentItem var2, LongSupplier var3) {
      if (var1 != null
         && var2 != null
         && var3 != null
         && (var2.durability() <= 0 && var2.enchantLevel() == 0 || var1.weapon() || var1.armor())
         && (!var2.equipped() || equipStateSupported(var1))) {
         if (var1.stackable()) {
            RuntimeInventory.Item var4 = this.items
               .values()
               .stream()
               .filter(var1x -> var1x.definition().catalogId() == var1.catalogId())
               .filter(var1x -> var1x.definition().equals(var1))
               .filter(
                  var1x -> var1x.enchantLevel() == var2.enchantLevel()
                     && var1x.durability() == var2.durability()
                     && var1x.identified() == var2.identified()
                     && var1x.blessing() == var2.blessing()
                     && var1x.sourceBound() == var2.sourceBound()
                     && var1x.tournamentReward() == var2.tournamentReward()
                     && var1x.equipped() == var2.equipped()
                     && var1x.chargeCount() == var2.chargeCount()
                     && var1x.expiresAtEpochMillis() == var2.expiresAtEpochMillis()
                     && var1x.sealed() == var2.sealed()
               )
               .findFirst()
               .orElse(null);
            if (var4 != null) {
               long var9 = (long)var4.quantity() + var2.quantity();
               if (var9 > 2147483647L) {
                  return false;
               }

               this.items.put(var4.objectId(), var4.withQuantity((int)var9));
               return true;
            }
         }

         if (var1.stackable()) {
            if (this.items.size() >= 256) {
               return false;
            } else {
               long var8 = var2.objectId() == 0L ? this.uniqueObjectId(var3) : this.admittedStableObjectId(var2.objectId());
               if (var8 == 0L) {
                  return false;
               } else {
                  this.items
                     .put(
                        var8,
                        new RuntimeInventory.Item(
                           var8,
                           var1,
                           var2.quantity(),
                           var2.equipped(),
                           var2.durability(),
                           var2.enchantLevel(),
                           var2.identified(),
                           var2.blessing(),
                           var2.sourceBound(),
                           var2.tournamentReward(),
                           var2.chargeCount(),
                           var2.expiresAtEpochMillis(),
                           var2.sealed(),
                           var2.litoOwned()
                        )
                     );
                  return true;
               }
            }
         } else if (var2.quantity() <= 256 - this.items.size() && (var2.objectId() == 0L || var2.quantity() == 1)) {
            for (int var7 = 0; var7 < var2.quantity(); var7++) {
               long var5 = var2.objectId() == 0L ? this.uniqueObjectId(var3) : this.admittedStableObjectId(var2.objectId());
               if (var5 == 0L) {
                  return false;
               }

               this.items
                  .put(
                     var5,
                     new RuntimeInventory.Item(
                        var5,
                        var1,
                        1,
                        var2.equipped(),
                        var2.durability(),
                        var2.enchantLevel(),
                        var2.identified(),
                        var2.blessing(),
                        var2.sourceBound(),
                        var2.tournamentReward(),
                        var2.chargeCount(),
                        var2.expiresAtEpochMillis(),
                        var2.sealed(),
                        var2.litoOwned()
                     )
                  );
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   synchronized boolean decrementCharge(long var1) {
      RuntimeInventory.Item var3 = this.items.get(var1);
      if (var3 != null && var3.chargeCount() > 0) {
         this.items.put(var1, var3.withChargeCount(var3.chargeCount() - 1));
         return true;
      } else {
         return false;
      }
   }

   private static boolean equipStateSupported(InventoryCatalog.Definition var0) {
      return var0.weapon() || var0.ring() || var0.light() || var0.sourceArmor();
   }

   synchronized boolean remove(long var1, long var3) {
      RuntimeInventory.Item var5 = this.items.get(var1);
      if (var5 != null
         && !this.sourcePhysicalWeapons.containsKey(var1)
         && (!var5.sourceBound() || var5.litoOwned() || !var5.definition().weapon() && !var5.definition().sourceArmor() && !var5.definition().projectile())
         && var3 > 0L
         && var3 <= var5.quantity()) {
         int var6 = var5.quantity() - (int)var3;
         if (var6 == 0) {
            this.items.remove(var1);
         } else {
            this.items.put(var1, var5.withQuantity(var6));
         }

         return true;
      } else {
         return false;
      }
   }

   synchronized boolean removeCatalog(long var1, long var3) {
      if (var1 > 0L && var3 > 0L) {
         long var5 = this.items
            .values()
            .stream()
            .filter(var2 -> var2.definition().catalogId() == var1)
            .filter(
               var0 -> !var0.sourceBound()
                  || var0.litoOwned()
                  || !var0.definition().weapon() && !var0.definition().sourceArmor() && !var0.definition().projectile()
            )
            .mapToLong(RuntimeInventory.Item::quantity)
            .sum();
         if (var5 < var3) {
            return false;
         } else {
            long var7 = var3;

            for (RuntimeInventory.Item var10 : List.copyOf(this.items.values())) {
               if (var7 == 0L) {
                  break;
               }

               if (var10.definition().catalogId() == var1
                  && (
                     !var10.sourceBound()
                        || var10.litoOwned()
                        || !var10.definition().weapon() && !var10.definition().sourceArmor() && !var10.definition().projectile()
                  )) {
                  long var11 = Math.min(var7, (long)var10.quantity());
                  if (!this.remove(var10.objectId(), var11)) {
                     throw new IllegalStateException("validated catalog removal failed");
                  }

                  var7 -= var11;
               }
            }

            return var7 == 0L;
         }
      } else {
         return false;
      }
   }

   synchronized RuntimeInventory.Item removeEntire(long var1) {
      RuntimeInventory.Item var3 = this.items.get(var1);
      return var3 == null
            || !var3.sourceBound()
            || var3.litoOwned()
            || !var3.definition().weapon() && !var3.definition().sourceArmor() && !var3.definition().projectile()
         ? this.items.remove(var1)
         : null;
   }

   synchronized RuntimeInventory.Item removeEntireForDeath(long var1) {
      RuntimeInventory.Item var3 = this.items.remove(var1);
      if (var3 != null) {
         this.sourcePhysicalWeapons.remove(var1);
      }

      return var3;
   }

   synchronized List<RuntimeInventory.Item> expireTimedItems(long var1) {
      if (var1 < 0L) {
         throw new IllegalArgumentException("negative timed-item scan timestamp");
      } else {
         ArrayList var3 = new ArrayList();

         for (RuntimeInventory.Item var5 : List.copyOf(this.items.values())) {
            if (var5.expiresAtEpochMillis() != 0L && var5.expiresAtEpochMillis() < var1) {
               RuntimeInventory.Item var6 = this.items.remove(var5.objectId());
               if (var6 != null) {
                  this.sourcePhysicalWeapons.remove(var6.objectId());
                  var3.add(var6);
               }
            }
         }

         return List.copyOf(var3);
      }
   }

   synchronized RuntimeInventory.Item consumeSourceProjectile(long var1) {
      RuntimeInventory.Item var3 = this.items.get(var1);
      if (var3 != null && var3.sourceBound() && var3.definition().projectile() && !var3.equipped()) {
         int var4 = var3.quantity() - 1;
         if (var4 == 0) {
            this.items.remove(var1);
         } else {
            this.items.put(var1, var3.withQuantity(var4));
         }

         return var3;
      } else {
         return null;
      }
   }

   synchronized boolean replaceDefinition(long var1, InventoryCatalog.Definition var3) {
      RuntimeInventory.Item var4 = this.items.get(var1);
      if (var4 != null && var3 != null && var4.quantity() == 1 && !var3.stackable()) {
         this.items.put(var1, var4.withDefinition(var3));
         return true;
      } else {
         return false;
      }
   }

   synchronized List<RuntimeInventory.Item> repairableWeapons() {
      return this.items.values().stream().filter(var0 -> var0.definition().weapon() && var0.durability() > 0).toList();
   }

   synchronized boolean damageWeapon(long var1, int var3) {
      RuntimeInventory.Item var4 = this.items.get(var1);
      if (var4 != null && var4.definition().weapon() && var3 > 0 && var3 <= 127) {
         this.items.put(var1, var4.withDurability(var3));
         return true;
      } else {
         return false;
      }
   }

   synchronized boolean damageFirstWeapon(int var1) {
      RuntimeInventory.Item var2 = this.items.values().stream().filter(var0 -> var0.definition().weapon()).findFirst().orElse(null);
      return var2 != null && this.damageWeapon(var2.objectId(), var1);
   }

   synchronized boolean repairWeapon(long var1) {
      RuntimeInventory.Item var3 = this.items.get(var1);
      if (var3 != null && var3.definition().weapon() && var3.durability() > 0) {
         this.items.put(var1, var3.withDurability(0));
         return true;
      } else {
         return false;
      }
   }

   synchronized boolean recoverDamage(long var1) {
      RuntimeInventory.Item var3 = this.items.get(var1);
      if (var3 != null && (var3.definition().weapon() || var3.definition().armor()) && var3.durability() > 0) {
         this.items.put(var1, var3.withDurability(var3.durability() - 1));
         return true;
      } else {
         return false;
      }
   }

   synchronized boolean createMagicalWeapon(long var1) {
      RuntimeInventory.Item var3 = this.items.get(var1);
      InventoryCatalog.NativeWeaponStatus var4 = var3 == null ? null : InventoryCatalog.nativeWeaponStatus(var3.definition().catalogId());
      if (var3 != null && var3.definition().weapon() && var4 != null && var4.safeEnchant() > 0 && var3.enchantLevel() == 0) {
         this.items.put(var1, var3.withEnchantLevel(1));
         return true;
      } else {
         return false;
      }
   }

   synchronized boolean identify(long var1) {
      RuntimeInventory.Item var3 = this.items.get(var1);
      if (var3 == null) {
         return false;
      } else {
         if (!var3.identified()) {
            this.items.put(var1, var3.withIdentified(true));
         }

         return true;
      }
   }

   synchronized boolean setBlessing(long var1, int var3) {
      RuntimeInventory.Item var4 = this.items.get(var1);
      if (var4 != null && var3 >= 0 && var3 <= 3) {
         if (var4.blessing() != var3) {
            this.items.put(var1, var4.withBlessing(var3));
         }

         return true;
      } else {
         return false;
      }
   }

   synchronized List<RuntimeInventory.Item> removeCurses() {
      ArrayList var1 = new ArrayList();

      for (Entry var3 : List.copyOf(this.items.entrySet())) {
         RuntimeInventory.Item var4 = (RuntimeInventory.Item)var3.getValue();
         if (var4.blessing() == 2) {
            RuntimeInventory.Item var5 = var4.withBlessing(1);
            this.items.put((Long)var3.getKey(), var5);
            var1.add(var5);
         }
      }

      return List.copyOf(var1);
   }

   synchronized List<RuntimeInventory.Item> toggleWeapon(long var1) {
      return this.toggleWeaponInternal(var1, false);
   }

   synchronized List<RuntimeInventory.Item> toggleSourceWeaponForEvaluation(long var1) {
      RuntimeInventory.Item var3 = this.items.get(var1);
      return var3 != null && var3.sourceBound() && !var3.litoOwned() && var3.definition().weapon() ? this.toggleWeaponInternal(var1, true) : List.of();
   }

   private synchronized List<RuntimeInventory.Item> toggleWeaponInternal(long var1, boolean var3) {
      RuntimeInventory.Item var4 = this.items.get(var1);
      if (var4 != null && var4.definition().weapon() && (!var4.sourceBound() || var4.litoOwned() || var3 || this.sourcePhysicalWeapons.containsKey(var1))) {
         boolean var5 = !var4.equipped();
         ArrayList var6 = new ArrayList();

         for (Entry var8 : List.copyOf(this.items.entrySet())) {
            RuntimeInventory.Item var9 = (RuntimeInventory.Item)var8.getValue();
            boolean var10 = var9.objectId() == var1 ? var5 : (var9.definition().weapon() && var9.equipped() ? false : var9.equipped());
            if (var10 != var9.equipped()) {
               RuntimeInventory.Item var11 = var9.withEquipped(var10);
               this.items.put((Long)var8.getKey(), var11);
               var6.add(var11);
            }
         }

         return List.copyOf(var6);
      } else {
         return List.of();
      }
   }

   synchronized void qualifySourceWeapon(long var1, SourceWeaponPhysicalStats var3) {
      RuntimeInventory.Item var4 = this.items.get(var1);
      if (var4 != null
         && var3 != null
         && var3.present()
         && var4.sourceBound()
         && !var4.litoOwned()
         && var4.definition().weapon()
         && var4.definition().catalogId() == var3.itemId()
         && var4.enchantLevel() == (byte)var3.enchantLevel()) {
         this.sourcePhysicalWeapons.put(var1, var3);
      } else {
         throw new IllegalArgumentException("source weapon qualification does not match runtime instance");
      }
   }

   synchronized List<RuntimeInventory.Item> unequipWeapons() {
      ArrayList var1 = new ArrayList();

      for (Entry var3 : List.copyOf(this.items.entrySet())) {
         RuntimeInventory.Item var4 = (RuntimeInventory.Item)var3.getValue();
         if (var4.equipped() && var4.definition().weapon()) {
            RuntimeInventory.Item var5 = var4.withEquipped(false);
            this.items.put((Long)var3.getKey(), var5);
            var1.add(var5);
         }
      }

      return List.copyOf(var1);
   }

   synchronized List<RuntimeInventory.Item> toggleLight(long var1) {
      RuntimeInventory.Item var3 = this.items.get(var1);
      if (var3 != null && var3.definition().light()) {
         RuntimeInventory.Item var4 = var3.withEquipped(!var3.equipped());
         this.items.put(var1, var4);
         return List.of(var4);
      } else {
         return List.of();
      }
   }

   synchronized List<RuntimeInventory.Item> toggleRing(long var1) {
      RuntimeInventory.Item var3 = this.items.get(var1);
      if (var3 != null && var3.definition().ring() && !var3.definition().sourceArmor()) {
         if (!var3.equipped() && this.equippedRingCount() >= 2) {
            return List.of();
         } else {
            RuntimeInventory.Item var4 = var3.withEquipped(!var3.equipped());
            this.items.put(var1, var4);
            return List.of(var4);
         }
      } else {
         return List.of();
      }
   }

   synchronized int equippedRingCount() {
      return (int)this.items.values().stream().filter(RuntimeInventory.Item::equipped).filter(var0 -> var0.definition().ring()).count();
   }

   synchronized List<RuntimeInventory.Item> toggleSourceArmor(long var1) {
      RuntimeInventory.Item var3 = this.items.get(var1);
      return var3 != null && var3.sourceBound() && var3.definition().sourceArmor() ? this.toggleNativeArmor(var1) : List.of();
   }

   synchronized List<RuntimeInventory.Item> toggleNativeArmor(long var1) {
      RuntimeInventory.Item var3 = this.items.get(var1);
      if (var3 != null && var3.definition().sourceArmor()) {
         InventoryCatalog.NativeArmorStatus var4 = var3.definition().sourceArmorStatus();
         int var5 = var4.armorType();
         if (var3.equipped()) {
            if (var3.blessing() == 2 || this.nativeArmorRemovalBlocked(var5)) {
               return List.of();
            }
         } else if (this.nativeArmorEquipBlocked(var3, var5)) {
            return List.of();
         }

         RuntimeInventory.Item var6 = var3.withEquipped(!var3.equipped());
         this.items.put(var1, var6);
         return List.of(var6);
      } else {
         return List.of();
      }
   }

   synchronized int equippedSourceArmorCount() {
      return (int)this.items
         .values()
         .stream()
         .filter(RuntimeInventory.Item::equipped)
         .filter(var0 -> var0.sourceBound() && var0.definition().sourceArmor())
         .count();
   }

   synchronized boolean hasEquippedHasteArmor() {
      return this.items
         .values()
         .stream()
         .filter(RuntimeInventory.Item::equipped)
         .map(RuntimeInventory.Item::definition)
         .map(InventoryCatalog.Definition::sourceArmorStatus)
         .filter(Objects::nonNull)
         .anyMatch(InventoryCatalog.NativeArmorStatus::haste);
   }

   synchronized RuntimeInventory.Item equippedSourceArmor(int var1) {
      return this.items
         .values()
         .stream()
         .filter(RuntimeInventory.Item::equipped)
         .filter(var0 -> var0.sourceBound() && var0.definition().sourceArmor())
         .filter(var1x -> var1x.definition().sourceArmorStatus().armorType() == var1)
         .findFirst()
         .orElse(null);
   }

   synchronized RuntimeInventory.NativeArmorBonuses equippedNativeArmorBonuses() {
      int var1 = 0;
      int var2 = 0;
      int var3 = 0;
      int var4 = 0;

      for (RuntimeInventory.Item var6 : this.items.values()) {
         if (var6.equipped() && !var6.sourceBound() && var6.definition().sourceArmor()) {
            InventoryCatalog.NativeArmorStatus var7 = var6.definition().sourceArmorStatus();
            var1 = Math.addExact(var1, Math.subtractExact(-var7.displayArmorClass(), var6.enchantLevel()));
            var2 = Math.addExact(var2, var7.hitPoints());
            var3 = Math.addExact(var3, var7.manaPoints());
            var4 = Math.addExact(var4, var7.charisma());
         }
      }

      return new RuntimeInventory.NativeArmorBonuses(var1, var2, var3, var4);
   }

   synchronized RentalEquipmentBonuses equippedRentalBonuses() {
      return RentalEquipmentBonuses.from(this.items());
   }

   private synchronized boolean nativeArmorEquipBlocked(RuntimeInventory.Item var1, int var2) {
      if (var2 != 3 || !this.hasEquippedNativeArmorType(2) && !this.hasEquippedNativeArmorType(4)) {
         if (var2 == 2 && this.hasEquippedNativeArmorType(4)) {
            return true;
         } else {
            return switch (var2) {
               case 7, 13 -> this.hasEquippedNativeArmorType(7) || this.hasEquippedNativeArmorType(13);
               default -> this.hasEquippedNativeArmorType(var2);
               case 9, 11 -> this.equippedRingCount() >= 2;
               case 12 -> this.hasEquippedNativeArmorType(12);
               case 14, 15 -> this.equippedNativeArmorTypeCount(14, 15) >= 2 || this.hasEquippedNativeArmorCatalog(var1.definition().catalogId());
               case 19, 20, 21, 22, 23, 24 -> this.hasEquippedNativeArmorType(var2);
               case 25 -> this.equippedNativeArmorTypeCount(25) >= 4;
            };
         }
      } else {
         return true;
      }
   }

   private synchronized boolean nativeArmorRemovalBlocked(int var1) {
      return var1 == 3 && this.hasEquippedNativeArmorType(2) ? true : (var1 == 2 || var1 == 3) && this.hasEquippedNativeArmorType(4);
   }

   private synchronized boolean hasEquippedNativeArmorType(int var1) {
      return this.equippedNativeArmorTypeCount(var1) > 0;
   }

   private synchronized int equippedNativeArmorTypeCount(int... var1) {
      int var2 = 0;

      for (RuntimeInventory.Item var4 : this.items.values()) {
         if (var4.equipped() && var4.definition().sourceArmor()) {
            int var5 = var4.definition().sourceArmorStatus().armorType();

            for (int var9 : var1) {
               if (var5 == var9) {
                  var2++;
                  break;
               }
            }
         }
      }

      return var2;
   }

   private synchronized boolean hasEquippedNativeArmorCatalog(long var1) {
      return this.items.values().stream().anyMatch(var2 -> var2.equipped() && var2.definition().sourceArmor() && var2.definition().catalogId() == var1);
   }

   synchronized List<RuntimeInventory.Item> toggleControlRing(long var1) {
      return this.toggleRing(var1);
   }

   synchronized int equippedControlRingCount() {
      return this.equippedRingCount();
   }

   synchronized boolean controlAbilityEnabled(int var1) {
      return var1 >= 0 && var1 < 6
         ? this.items.values().stream().filter(RuntimeInventory.Item::equipped).anyMatch(var1x -> var1x.definition().controlAbilityType() == var1)
         : false;
   }

   synchronized int activeLightRange() {
      return this.items
         .values()
         .stream()
         .filter(RuntimeInventory.Item::equipped)
         .map(RuntimeInventory.Item::definition)
         .filter(InventoryCatalog.Definition::light)
         .mapToInt(InventoryCatalog.Definition::lightRange)
         .max()
         .orElse(0);
   }

   synchronized int activeFireResistance() {
      int var1 = this.items
         .values()
         .stream()
         .filter(RuntimeInventory.Item::equipped)
         .filter(var0 -> !var0.definition().sourceArmor())
         .mapToInt(var0 -> var0.definition().fireResistance())
         .sum();
      return Math.max(-128, Math.min(127, var1));
   }

   synchronized int equippedWalkAction() {
      return this.items
         .values()
         .stream()
         .filter(RuntimeInventory.Item::equipped)
         .map(RuntimeInventory.Item::definition)
         .filter(InventoryCatalog.Definition::weapon)
         .mapToInt(InventoryCatalog.Definition::weaponWalkAction)
         .findFirst()
         .orElse(0);
   }

   synchronized boolean hasEquippedWeapon() {
      return this.items.values().stream().anyMatch(var0 -> var0.equipped() && var0.definition().weapon());
   }

   synchronized boolean hasEquippedCatalog(long var1) {
      return var1 > 0L && var1 <= 4294967295L ? this.items.values().stream().anyMatch(var2 -> var2.equipped() && var2.definition().catalogId() == var1) : false;
   }

   synchronized RuntimeInventory.Item equippedWeaponItem() {
      return this.items.values().stream().filter(var0 -> var0.equipped() && var0.definition().weapon()).findFirst().orElse(null);
   }

   private static int nativeRuntimeWeaponRange(int var0, int var1) {
      if (var1 >= 0) {
         return var1;
      } else if (var0 == 10) {
         return 10;
      } else {
         throw new IllegalStateException("unsupported negative rental weapon range " + var1 + " for type " + var0);
      }
   }

   private synchronized SourceProjectilePhysicalStats qualifiedRuntimeProjectile(int var1) {
      int var2 = var1 == 10 ? 253 : 254;

      for (RuntimeInventory.Item var4 : this.items.values()) {
         if (!var4.equipped() && var4.quantity() > 0 && var4.definition().useType() == var2) {
            InventoryCatalog.NativeProjectileStatus var5 = InventoryCatalog.nativeProjectileStatus(var4.definition().catalogId());
            if (var5 != null) {
               return new SourceProjectilePhysicalStats(
                  var4.objectId(),
                  (int)var4.definition().catalogId(),
                  var1 == 10 ? -3 : -2,
                  var5.damageSmall(),
                  var5.damageLarge(),
                  var5.material(),
                  var4.blessing(),
                  var5.arrowGfx(),
                  var5.consumeOnAttack(),
                  var4.quantity()
               );
            }
         }
      }

      return SourceProjectilePhysicalStats.none();
   }

   synchronized boolean hasQualifiedPhysicalWeapon(long var1) {
      RuntimeInventory.Item var3 = this.items.get(var1);
      return var3 != null && var3.definition().weapon() ? !var3.sourceBound() || this.sourcePhysicalWeapons.containsKey(var1) : false;
   }

   private static int syntheticWeaponDamage(RuntimeInventory.Item var0) {
      return var0.definition().catalogId() == 1001L ? 10 : 0;
   }

   private static int weaponTypeForWalk(int var0) {
      return switch (var0) {
         case 4 -> 1;
         case 11 -> 6;
         case 20 -> 4;
         case 24 -> 5;
         case 40 -> 7;
         case 46 -> 2;
         case 50 -> 3;
         case 54 -> 12;
         case 58 -> 11;
         case 62 -> 10;
         case 83 -> 18;
         default -> throw new IllegalStateException("runtime weapon has unsupported native walk action " + var0);
      };
   }

   synchronized Map<Long, Integer> catalogCounts() {
      LinkedHashMap<Long, Integer> var1 = new LinkedHashMap<>();

      for (RuntimeInventory.Item var3 : this.items.values()) {
         var1.merge(var3.definition().catalogId(), var3.quantity(), Math::addExact);
      }

      return var1;
   }

   synchronized Map<Long, List<Integer>> catalogEnchantments() {
      LinkedHashMap<Long, List<Integer>> var1 = new LinkedHashMap<>();
      LinkedHashMap<Long, List<Integer>> var2 = new LinkedHashMap<>();

      for (RuntimeInventory.Item var4 : this.items.values()) {
         if (var4.definition().weapon()) {
            var2.computeIfAbsent(var4.definition().catalogId(), var0 -> new ArrayList<Integer>()).add(var4.enchantLevel());
         }
      }

      var2.forEach((var1x, var2x) -> {
         int var3 = -1;

         for (int var4x = 0; var4x < var2x.size(); var4x++) {
            if ((Integer)var2x.get(var4x) != 0) {
               var3 = var4x;
            }
         }

         if (var3 >= 0) {
            var1.put(var1x, List.copyOf(var2x.subList(0, var3 + 1)));
         }
      });
      return Map.copyOf(var1);
   }

   synchronized Map<Long, List<RuntimeInventory.PersistentItem>> catalogItemStates() {
      LinkedHashMap<Long, List<RuntimeInventory.PersistentItem>> var1 = new LinkedHashMap<>();
      LinkedHashMap<Long, Boolean> var2 = new LinkedHashMap<>();

      for (RuntimeInventory.Item var4 : this.items.values()) {
         long var5 = var4.definition().catalogId();
         RuntimeInventory.PersistentItem var7 = var4.persistentItem();
         var1.computeIfAbsent(var5, var0 -> new ArrayList<RuntimeInventory.PersistentItem>()).add(var7);
         if (!var7.isDefaultFor(var4.definition())) {
            var2.put(var5, true);
         }
      }

      LinkedHashMap<Long, List<RuntimeInventory.PersistentItem>> var8 = new LinkedHashMap<>();
      var1.forEach((var2x, var3) -> {
         if (var2.getOrDefault(var2x, false)) {
            var8.put(var2x, List.copyOf(var3));
         }
      });
      return Map.copyOf(var8);
   }

   private static InventoryCatalog.Definition durableDefinitionOverride(InventoryCatalog.Definition var0) {
      if (var0 == null) {
         return null;
      } else {
         InventoryCatalog.Definition var1 = InventoryCatalog.find(var0.catalogId());
         return var0.equals(var1) ? null : var0;
      }
   }

   private synchronized void applyCatalogEnchantments(Map<Long, List<Integer>> var1) {
      if (var1 != null && !var1.isEmpty()) {
         LinkedHashMap<Long, Integer> var2 = new LinkedHashMap<>();

         for (Entry<Long, RuntimeInventory.Item> var4 : List.copyOf(this.items.entrySet())) {
            RuntimeInventory.Item var5 = var4.getValue();
            List<Integer> var6 = var1.get(var5.definition().catalogId());
            if (var6 != null) {
               int var7 = var2.getOrDefault(var5.definition().catalogId(), 0);
               var2.put(var5.definition().catalogId(), var7 + 1);
               if (var7 < var6.size()) {
                  Integer var8 = (Integer)var6.get(var7);
                  if (var8 != null && var8 != 0 && var5.definition().weapon() && var8 >= -128 && var8 <= 127) {
                     this.items.put((Long)var4.getKey(), var5.withEnchantLevel(var8));
                  }
               }
            }
         }
      }
   }

   private synchronized long uniqueObjectId(LongSupplier var1) {
      for (int var2 = 0; var2 < 1024; var2++) {
         long var3 = var1.getAsLong();
         if (var3 > 0L && var3 <= 4294967295L && !this.items.containsKey(var3)) {
            return var3;
         }
      }

      throw new IllegalStateException("cannot allocate a unique native inventory object id");
   }

   private synchronized long admittedStableObjectId(long var1) {
      if (var1 > 0L && var1 <= 4294967295L && !this.items.containsKey(var1)) {
         reserveDefaultObjectId(var1);
         return var1;
      } else {
         return 0L;
      }
   }

   private static void reserveDefaultObjectId(long var0) {
      if (var0 >= 1879048192L && var0 < 2147483647L) {
         long var2 = var0 + 1L;
         NEXT_OBJECT_ID.getAndUpdate(var2x -> var2x >= var2 && var2x <= 2147483647L ? var2x : var2);
      }
   }

   private static long nextObjectId() {
      return NEXT_OBJECT_ID.getAndUpdate(var0 -> var0 >= 2147483647L ? 1879048192L : var0 + 1L);
   }

   synchronized SourceWeaponPhysicalStats equippedPhysicalWeapon() {
      RuntimeInventory.Item var1 = this.equippedWeaponItem();
      if (var1 == null) {
         return SourceWeaponPhysicalStats.none();
      } else {
         SourceWeaponPhysicalStats var2 = this.sourcePhysicalWeapons.get(var1.objectId());
         if (var2 != null) {
            SourceWeaponPhysicalStats var11 = var2.withDurability(var1.durability());
            if (var11.ranged()) {
               var11 = var11.withoutProjectile();
               SourceProjectilePhysicalStats var12 = this.qualifiedRuntimeProjectile(var11.weaponType());
               if (var12.present()) {
                  var11 = var11.withProjectile(var12);
               }
            }

            return var11;
         } else if (var1.sourceBound() && !var1.litoOwned()) {
            throw new IllegalStateException("equipped source weapon has no per-instance combat provenance: " + var1.objectId());
         } else {
            InventoryCatalog.NativeWeaponStatus var3 = var1.definition().effectiveWeaponStatus();
            int var4 = var3 == null ? syntheticWeaponDamage(var1) : var3.damageSmall();
            int var5 = var3 == null ? syntheticWeaponDamage(var1) : var3.damageLarge();
            int var6 = weaponTypeForWalk(var1.definition().weaponWalkAction());
            RentalItemDefinitionCatalog.SourceTemplate var7 = RentalItemDefinitionCatalog.source((int)var1.definition().catalogId());
            int var10000;
            if (var7 != null && "weapon".equals(var7.kind())) {
               var10000 = nativeRuntimeWeaponRange(var6, var7.integer("range"));
            } else {
               switch (var6) {
                  case 4:
                  case 13:
                     var10000 = 15;
                     break;
                  case 10:
                     var10000 = 10;
                     break;
                  default:
                     var10000 = 1;
               }
            }

            int var8 = var10000;
            SourceWeaponPhysicalStats var9 = new SourceWeaponPhysicalStats(
               (int)var1.definition().catalogId(),
               var6,
               var4,
               var5,
               var8,
               var3 == null ? 0 : var3.hitModifier(),
               var3 == null ? 0 : var3.damageModifier(),
               var1.enchantLevel(),
               var1.durability()
            );
            if (var7 != null && "weapon".equals(var7.kind())) {
               var9 = var9.withCombatTraits(
                  new SourceWeaponCombatTraits(
                     ClassicWorldDropCatalog.materialCode(var7.fields().get("material")),
                     var1.blessing(),
                     0,
                     0,
                     0,
                     var7.integer("dmg_cri_chance"),
                     var7.decimal("dmg_cri"),
                     var7.integer("dmg_max_chance"),
                     var7.integer("dmg_cri_effect") == 1,
                     var7.integer("dmg_max_effect") == 1,
                     var7.integer("invgfx")
                  )
               );
            }

            if (var9.ranged()) {
               SourceProjectilePhysicalStats var10 = this.qualifiedRuntimeProjectile(var9.weaponType());
               if (var10.present()) {
                  var9 = var9.withProjectile(var10);
               }
            }

            return var9.withNameId(var1.definition().name());
         }
      }
   }

   synchronized Map nativeDeItems() {
      return this.items;
   }

   synchronized Map nativeDePhysical() {
      return this.sourcePhysicalWeapons;
   }

   synchronized void nativeDeBind(Map var1) {
      this.items = var1;
   }

   record Item(
      long objectId,
      InventoryCatalog.Definition definition,
      int quantity,
      boolean equipped,
      int durability,
      int enchantLevel,
      boolean identified,
      int blessing,
      boolean sourceBound,
      boolean tournamentReward,
      int chargeCount,
      long expiresAtEpochMillis,
      boolean sealed,
      boolean litoOwned
   ) {
      Item(
         long objectId,
         InventoryCatalog.Definition definition,
         int quantity,
         boolean equipped,
         int durability,
         int enchantLevel,
         boolean identified,
         int blessing,
         boolean sourceBound,
         boolean tournamentReward,
         int chargeCount,
         long expiresAtEpochMillis,
         boolean sealed
      ) {
         this(
            objectId,
            definition,
            quantity,
            equipped,
            durability,
            enchantLevel,
            identified,
            blessing,
            sourceBound,
            tournamentReward,
            chargeCount,
            expiresAtEpochMillis,
            sealed,
            false
         );
      }

      Item(
         long objectId,
         InventoryCatalog.Definition definition,
         int quantity,
         boolean equipped,
         int durability,
         int enchantLevel,
         boolean identified,
         int blessing,
         boolean sourceBound,
         boolean tournamentReward,
         int chargeCount
      ) {
         this(objectId, definition, quantity, equipped, durability, enchantLevel, identified, blessing, sourceBound, tournamentReward, chargeCount, 0L, false);
      }

      Item(
         long objectId,
         InventoryCatalog.Definition definition,
         int quantity,
         boolean equipped,
         int durability,
         int enchantLevel,
         boolean identified,
         int blessing,
         boolean sourceBound,
         int chargeCount
      ) {
         this(objectId, definition, quantity, equipped, durability, enchantLevel, identified, blessing, sourceBound, false, chargeCount);
      }

      Item(
         long objectId,
         InventoryCatalog.Definition definition,
         int quantity,
         boolean equipped,
         int durability,
         int enchantLevel,
         boolean identified,
         int blessing,
         boolean sourceBound
      ) {
         this(
            objectId,
            definition,
            quantity,
            equipped,
            durability,
            enchantLevel,
            identified,
            blessing,
            sourceBound,
            false,
            definition == null ? 0 : definition.initialChargeCount()
         );
      }

      Item(
         long objectId,
         InventoryCatalog.Definition definition,
         int quantity,
         boolean equipped,
         int durability,
         int enchantLevel,
         boolean identified,
         int blessing
      ) {
         this(
            objectId,
            definition,
            quantity,
            equipped,
            durability,
            enchantLevel,
            identified,
            blessing,
            false,
            false,
            definition == null ? 0 : definition.initialChargeCount()
         );
      }

      Item(long objectId, InventoryCatalog.Definition definition, int quantity, boolean equipped) {
         this(objectId, definition, quantity, equipped, 0, 0, definition.identified(), definition.blessing(), false, false, definition.initialChargeCount());
      }

      Item(long objectId, InventoryCatalog.Definition definition, int quantity, boolean equipped, int durability) {
         this(
            objectId,
            definition,
            quantity,
            equipped,
            durability,
            0,
            definition.identified(),
            definition.blessing(),
            false,
            false,
            definition.initialChargeCount()
         );
      }

      Item(long objectId, InventoryCatalog.Definition definition, int quantity, boolean equipped, int durability, boolean identified) {
         this(objectId, definition, quantity, equipped, durability, 0, identified, definition.blessing(), false, false, definition.initialChargeCount());
      }

      Item(
         long objectId,
         InventoryCatalog.Definition definition,
         int quantity,
         boolean equipped,
         int durability,
         int enchantLevel,
         boolean identified,
         int blessing,
         boolean sourceBound,
         boolean tournamentReward,
         int chargeCount,
         long expiresAtEpochMillis,
         boolean sealed,
         boolean litoOwned
      ) {
         if (objectId > 0L
            && objectId <= 4294967295L
            && definition != null
            && quantity > 0
            && durability >= 0
            && durability <= 127
            && enchantLevel >= -128
            && enchantLevel <= 127
            && chargeCount >= 0
            && chargeCount <= definition.maximumChargeCount()
            && (!litoOwned || sourceBound)
            && blessing >= 0
            && blessing <= 3
            && (durability <= 0 && enchantLevel == 0 || definition.weapon() || definition.armor())) {
            this.objectId = objectId;
            this.definition = definition;
            this.quantity = quantity;
            this.equipped = equipped;
            this.durability = durability;
            this.enchantLevel = enchantLevel;
            this.identified = identified;
            this.blessing = blessing;
            this.sourceBound = sourceBound;
            this.tournamentReward = tournamentReward;
            this.chargeCount = chargeCount;
            this.expiresAtEpochMillis = expiresAtEpochMillis;
            this.sealed = sealed;
            this.litoOwned = litoOwned;
         } else {
            throw new IllegalArgumentException("invalid runtime inventory item");
         }
      }

      RuntimeInventory.Item withQuantity(int value) {
         return new RuntimeInventory.Item(
            this.objectId,
            this.definition,
            value,
            this.equipped,
            this.durability,
            this.enchantLevel,
            this.identified,
            this.blessing,
            this.sourceBound,
            this.tournamentReward,
            this.chargeCount,
            this.expiresAtEpochMillis,
            this.sealed,
            this.litoOwned
         );
      }

      RuntimeInventory.Item withEquipped(boolean value) {
         return new RuntimeInventory.Item(
            this.objectId,
            this.definition,
            this.quantity,
            value,
            this.durability,
            this.enchantLevel,
            this.identified,
            this.blessing,
            this.sourceBound,
            this.tournamentReward,
            this.chargeCount,
            this.expiresAtEpochMillis,
            this.sealed,
            this.litoOwned
         );
      }

      RuntimeInventory.Item withDurability(int value) {
         return new RuntimeInventory.Item(
            this.objectId,
            this.definition,
            this.quantity,
            this.equipped,
            value,
            this.enchantLevel,
            this.identified,
            this.blessing,
            this.sourceBound,
            this.tournamentReward,
            this.chargeCount,
            this.expiresAtEpochMillis,
            this.sealed,
            this.litoOwned
         );
      }

      RuntimeInventory.Item withEnchantLevel(int value) {
         return new RuntimeInventory.Item(
            this.objectId,
            this.definition,
            this.quantity,
            this.equipped,
            this.durability,
            value,
            this.identified,
            this.blessing,
            this.sourceBound,
            this.tournamentReward,
            this.chargeCount,
            this.expiresAtEpochMillis,
            this.sealed,
            this.litoOwned
         );
      }

      RuntimeInventory.Item withIdentified(boolean value) {
         return new RuntimeInventory.Item(
            this.objectId,
            this.definition,
            this.quantity,
            this.equipped,
            this.durability,
            this.enchantLevel,
            value,
            this.blessing,
            this.sourceBound,
            this.tournamentReward,
            this.chargeCount,
            this.expiresAtEpochMillis,
            this.sealed,
            this.litoOwned
         );
      }

      RuntimeInventory.Item withBlessing(int value) {
         return new RuntimeInventory.Item(
            this.objectId,
            this.definition,
            this.quantity,
            this.equipped,
            this.durability,
            this.enchantLevel,
            this.identified,
            value,
            this.sourceBound,
            this.tournamentReward,
            this.chargeCount,
            this.expiresAtEpochMillis,
            this.sealed,
            this.litoOwned
         );
      }

      RuntimeInventory.Item withDefinition(InventoryCatalog.Definition value) {
         return new RuntimeInventory.Item(
            this.objectId,
            value,
            this.quantity,
            this.equipped,
            this.durability,
            this.enchantLevel,
            this.identified,
            this.blessing,
            this.sourceBound,
            this.tournamentReward,
            Math.min(this.chargeCount, value.maximumChargeCount()),
            this.expiresAtEpochMillis,
            this.sealed,
            this.litoOwned
         );
      }

      RuntimeInventory.Item withChargeCount(int value) {
         return new RuntimeInventory.Item(
            this.objectId,
            this.definition,
            this.quantity,
            this.equipped,
            this.durability,
            this.enchantLevel,
            this.identified,
            this.blessing,
            this.sourceBound,
            this.tournamentReward,
            value,
            this.expiresAtEpochMillis,
            this.sealed,
            this.litoOwned
         );
      }

      RuntimeInventory.Item asDerivedLitoOwned() {
         return new RuntimeInventory.Item(
            this.objectId,
            this.definition,
            this.quantity,
            this.equipped,
            this.durability,
            this.enchantLevel,
            this.identified,
            this.blessing,
            true,
            this.tournamentReward,
            this.chargeCount,
            this.expiresAtEpochMillis,
            this.sealed,
            true
         );
      }

      RuntimeInventory.Item withoutSourceBinding() {
         return new RuntimeInventory.Item(
            this.objectId,
            this.definition,
            this.quantity,
            this.equipped,
            this.durability,
            this.enchantLevel,
            this.identified,
            this.blessing,
            false,
            this.tournamentReward,
            this.chargeCount,
            this.expiresAtEpochMillis,
            this.sealed,
            false
         );
      }

      RuntimeInventory.PersistentItem persistentItem() {
         return new RuntimeInventory.PersistentItem(
            this.objectId,
            this.quantity,
            this.enchantLevel,
            this.durability,
            this.identified,
            this.blessing,
            this.sourceBound,
            this.tournamentReward,
            this.equipped,
            RuntimeInventory.durableDefinitionOverride(this.definition),
            this.chargeCount,
            this.expiresAtEpochMillis,
            this.sealed,
            this.litoOwned
         );
      }

      boolean timed() {
         return this.expiresAtEpochMillis != 0L;
      }

      boolean transferable() {
         return this.definition.tradable()
            && (!this.sourceBound || this.litoOwned || !this.definition.weapon() && !this.definition.sourceArmor() && !this.definition.projectile());
      }

      boolean blocksNativeTransferAsEquipped() {
         return this.equipped && !this.definition.light();
      }

      String nativeLogName() {
         return this.identified && (this.definition.weapon() || this.definition.armor())
            ? (this.enchantLevel >= 0 ? "+" + this.enchantLevel : this.enchantLevel) + " " + this.definition.name()
            : this.definition.name();
      }

      private static String signed(int value) {
         return value >= 0 ? "+" + value : Integer.toString(value);
      }

      Protocol.InventoryWireItem wireItem() {
         return new Protocol.InventoryWireItem(
            this.objectId,
            this.definition.useType(),
            this.chargeCount,
            this.definition.icon(),
            this.blessing,
            this.quantity,
            this.identified ? 1 : 0,
            this.nativeViewName(),
            this.identified ? this.nativeStatusDetail() : new byte[0]
         );
      }

      String nativeViewName() {
         String var1 = this.nativeLogName();
         String var2 = !this.equipped
            ? var1
            : (this.definition.weapon() ? var1 + " ($9)" : (this.definition.light() ? var1 + " ($10)" : (this.definition.armor() ? var1 + " ($117)" : var1)));
         String var3 = this.definition.maximumChargeCount() > 0 && this.identified ? var2 + " (" + this.chargeCount + ")" : var2;
         return this.quantity > 1 ? var3 + " (" + this.quantity + ")" : var3;
      }

      byte[] nativeStatusDetail() {
         InventoryCatalog.NativeArmorStatus var1 = this.definition.sourceArmorStatus();
         if (var1 != null) {
            return var1.detail(this.definition.useType(), this.quantity, this.enchantLevel, this.durability);
         } else {
            InventoryCatalog.NativeProjectileStatus var2 = InventoryCatalog.nativeProjectileStatus(this.definition.catalogId());
            if (this.definition.projectile() && var2 != null) {
               long var6 = Math.min(4294967295L, (long)this.definition.unitWeight() * this.quantity / 1000L);
               return new Protocol.Writer().c(1).c(var2.damageSmall()).c(var2.damageLarge()).c(var2.material()).d(var6).bytes();
            } else {
               InventoryCatalog.NativeWeaponStatus var3 = this.definition.effectiveWeaponStatus();
               if (var3 == null) {
                  long var7 = Math.min(4294967295L, (long)this.definition.unitWeight() * this.quantity / 1000L);
                  return new Protocol.Writer().c(23).c(0).d(var7).bytes();
               } else {
                  Protocol.Writer var4 = new Protocol.Writer()
                     .c(1)
                     .c(var3.damageSmall())
                     .c(var3.damageLarge())
                     .c(var3.material())
                     .d(var3.packetWeight(this.quantity));
                  if (this.enchantLevel != 0) {
                     var4.c(2).c(this.enchantLevel);
                  }

                  if (this.durability != 0) {
                     var4.c(3).c(this.durability);
                  }

                  if (var3.hitModifier() != 0) {
                     var4.c(5).c(NativeCharacterStats.signedByte(var3.hitModifier()));
                  }

                  if (var3.damageModifier() != 0) {
                     var4.c(6).c(NativeCharacterStats.signedByte(var3.damageModifier()));
                  }

                  return var4.c(7).c(var3.classMask()).bytes();
               }
            }
         }
      }
   }

   record NativeArmorBonuses(int armorClassModifier, int maximumHp, int maximumMp, int charisma) {
   }

   record PersistentItem(
      long objectId,
      int quantity,
      int enchantLevel,
      int durability,
      boolean identified,
      int blessing,
      boolean sourceBound,
      boolean tournamentReward,
      boolean equipped,
      InventoryCatalog.Definition definitionOverride,
      int chargeCount,
      long expiresAtEpochMillis,
      boolean sealed,
      boolean litoOwned
   ) {
      PersistentItem(
         long objectId,
         int quantity,
         int enchantLevel,
         int durability,
         boolean identified,
         int blessing,
         boolean sourceBound,
         boolean tournamentReward,
         boolean equipped,
         InventoryCatalog.Definition definitionOverride,
         int chargeCount,
         long expiresAtEpochMillis,
         boolean sealed
      ) {
         this(
            objectId,
            quantity,
            enchantLevel,
            durability,
            identified,
            blessing,
            sourceBound,
            tournamentReward,
            equipped,
            definitionOverride,
            chargeCount,
            expiresAtEpochMillis,
            sealed,
            false
         );
      }

      PersistentItem(
         long objectId,
         int quantity,
         int enchantLevel,
         int durability,
         boolean identified,
         int blessing,
         boolean sourceBound,
         boolean tournamentReward,
         boolean equipped,
         InventoryCatalog.Definition definitionOverride,
         int chargeCount
      ) {
         this(
            objectId,
            quantity,
            enchantLevel,
            durability,
            identified,
            blessing,
            sourceBound,
            tournamentReward,
            equipped,
            definitionOverride,
            chargeCount,
            0L,
            false
         );
      }

      PersistentItem(
         long objectId,
         int quantity,
         int enchantLevel,
         int durability,
         boolean identified,
         int blessing,
         boolean sourceBound,
         boolean equipped,
         InventoryCatalog.Definition definitionOverride,
         int chargeCount
      ) {
         this(objectId, quantity, enchantLevel, durability, identified, blessing, sourceBound, false, equipped, definitionOverride, chargeCount);
      }

      PersistentItem(
         long objectId,
         int quantity,
         int enchantLevel,
         int durability,
         boolean identified,
         int blessing,
         boolean sourceBound,
         boolean equipped,
         InventoryCatalog.Definition definitionOverride
      ) {
         this(objectId, quantity, enchantLevel, durability, identified, blessing, sourceBound, false, equipped, definitionOverride, 0);
      }

      PersistentItem(long objectId, int quantity, int enchantLevel, int durability, boolean identified, int blessing, boolean sourceBound, boolean equipped) {
         this(objectId, quantity, enchantLevel, durability, identified, blessing, sourceBound, false, equipped, null, 0);
      }

      PersistentItem(long objectId, int quantity, int enchantLevel, int durability, boolean identified, int blessing) {
         this(objectId, quantity, enchantLevel, durability, identified, blessing, false, false, false, null, 0);
      }

      PersistentItem(long objectId, int quantity, int enchantLevel, int durability, boolean identified, int blessing, boolean sourceBound) {
         this(objectId, quantity, enchantLevel, durability, identified, blessing, sourceBound, false, false, null, 0);
      }

      PersistentItem(int quantity, int enchantLevel, int durability, boolean identified, int blessing) {
         this(0L, quantity, enchantLevel, durability, identified, blessing, false, false, false, null, 0);
      }

      PersistentItem(
         long objectId,
         int quantity,
         int enchantLevel,
         int durability,
         boolean identified,
         int blessing,
         boolean sourceBound,
         boolean tournamentReward,
         boolean equipped,
         InventoryCatalog.Definition definitionOverride,
         int chargeCount,
         long expiresAtEpochMillis,
         boolean sealed,
         boolean litoOwned
      ) {
         if (objectId < 0L
            || objectId > 4294967295L
            || quantity <= 0
            || enchantLevel < -128
            || enchantLevel > 127
            || durability < 0
            || durability > 127
            || chargeCount < 0
            || chargeCount > 255
            || expiresAtEpochMillis < 0L
            || litoOwned && !sourceBound
            || blessing < 0
            || blessing > 3) {
            throw new IllegalArgumentException("invalid durable inventory item state");
         }
         this.objectId = objectId;
         this.quantity = quantity;
         this.enchantLevel = enchantLevel;
         this.durability = durability;
         this.identified = identified;
         this.blessing = blessing;
         this.sourceBound = sourceBound;
         this.tournamentReward = tournamentReward;
         this.equipped = equipped;
         this.definitionOverride = definitionOverride;
         this.chargeCount = chargeCount;
         this.expiresAtEpochMillis = expiresAtEpochMillis;
         this.sealed = sealed;
         this.litoOwned = litoOwned;
      }

      static RuntimeInventory.PersistentItem defaults(InventoryCatalog.Definition definition, int quantity) {
         if (definition == null) {
            throw new IllegalArgumentException("item definition is required");
         } else {
            return new RuntimeInventory.PersistentItem(
               0L, quantity, 0, 0, definition.identified(), definition.blessing(), false, false, false, null, definition.initialChargeCount(), 0L, false
            );
         }
      }

      boolean isDefaultFor(InventoryCatalog.Definition definition) {
         return this.objectId == 0L
            && this.enchantLevel == 0
            && this.durability == 0
            && this.chargeCount == definition.initialChargeCount()
            && this.expiresAtEpochMillis == 0L
            && !this.sealed
            && !this.sourceBound
            && !this.tournamentReward
            && !this.litoOwned
            && !this.equipped
            && this.definitionOverride == null
            && this.identified == definition.identified()
            && this.blessing == definition.blessing();
      }

      InventoryCatalog.Definition effectiveDefinition(InventoryCatalog.Definition fallback, long catalogId) {
         InventoryCatalog.Definition effective = this.definitionOverride == null ? fallback : this.definitionOverride;
         if (effective == null || effective.catalogId() != catalogId) {
            throw new IllegalArgumentException("durable inventory definition/catalog id mismatch");
         }
         return effective;
      }
   }
}
