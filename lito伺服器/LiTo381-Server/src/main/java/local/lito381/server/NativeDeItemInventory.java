// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import local.lito381.server.InventoryCatalog.Definition;
import local.lito381.server.NativeDeItemApplication.Owner;
import local.lito381.server.NativeDeItemDefinitionsResources.Snapshot;
import local.lito381.server.RuntimeInventory.Item;

final class NativeDeItemInventory extends AbstractMap<Long, Item> {
   final RuntimeInventory inventory;
   final NativeDeItemTemplatesResources.Snapshot templates;
   final Snapshot definitions;
   volatile Owner owner;
   String rentalCharacter;
   String rentalAccount;
   private long mutationRevision;
   private WeakReference<NativeDeItemInventory> copySource;
   private long copyRevision;
   private final LinkedHashMap<Long, NativeDeItemInventory.EntryState> entries = new LinkedHashMap<>();

   static NativeDeItemInventory bind(RuntimeInventory var0, NativeDeItemTemplatesResources.Snapshot var1, Snapshot var2) {
      Objects.requireNonNull(var1);
      Objects.requireNonNull(var2);
      synchronized (var0) {
         if (var0.nativeDeItems() instanceof NativeDeItemInventory var9) {
            if (var9.templates != var1 || var9.definitions != var2) {
               throw new IllegalStateException("inventory already captures another source generation");
            }
            return var9;
         } else {
            NativeDeItemInventory var4 = new NativeDeItemInventory(var0, var1, var2);

            for (Item var6 : var0.items()) {
               var4.put(var6.objectId(), var6);
            }

            var0.nativeDeBind(var4);
            return var4;
         }
      }
   }

   private NativeDeItemInventory(RuntimeInventory var1, NativeDeItemTemplatesResources.Snapshot var2, Snapshot var3) {
      this.inventory = var1;
      this.templates = var2;
      this.definitions = var3;
   }

   private NativeDeItemTemplate template(long var1) {
      return var1 > 0L && var1 <= 2147483647L ? this.templates.table().getTemplate((int)var1) : null;
   }

   private NativeDeItemTemplate template(Definition var1) {
      NativeDeItemTemplate var2 = this.template(var1.catalogId());
      return var2 != null && var2 instanceof NativeDeWeaponTemplate == var1.weapon() && var2 instanceof NativeDeArmorTemplate == var1.armor() ? var2 : null;
   }

   static boolean shuffle(RuntimeInventory var0, Random var1) {
      if (!(var0.nativeDeItems() instanceof NativeDeItemInventory var2)) {
         return false;
      } else if (var1 == null) {
         throw new IllegalArgumentException("missing inventory random");
      } else {
         ArrayList<Entry<Long, NativeDeItemInventory.EntryState>> var6 = new ArrayList<>(var2.entries.entrySet());
         Collections.shuffle(var6, var1);
         var2.entries.clear();

         for (Entry var5 : var6) {
            var2.entries.put((Long)var5.getKey(), (NativeDeItemInventory.EntryState)var5.getValue());
         }

         var2.mutationRevision++;
         return true;
      }
   }

   local.lito381.server.NativeDeWorldKinds.Item actor(long var1) {
      synchronized (this.inventory) {
         NativeDeItemInventory.EntryState var4 = this.entries.get(var1);
         return var4 == null ? null : var4.actor;
      }
   }

   Set<Long> opaqueObjects() {
      synchronized (this.inventory) {
         LinkedHashSet var2 = new LinkedHashSet();
         this.entries.forEach((var1, var2x) -> {
            if (var2x.actor == null) {
               var2.add(var1);
            }
         });
         return Collections.unmodifiableSet(var2);
      }
   }

   static RuntimeInventory preserveCopy(RuntimeInventory var0, RuntimeInventory var1) {
      synchronized (var0) {
         if (!(var0.nativeDeItems() instanceof NativeDeItemInventory var3)) { return var1; }
            NativeDeItemInventory var11 = new NativeDeItemInventory(var1, var3.templates, var3.definitions);
            var11.owner = var3.owner;
            var11.rentalCharacter = var3.rentalCharacter;
            var11.rentalAccount = var3.rentalAccount;
            var11.copySource = new WeakReference<>(var3);
            var11.copyRevision = var3.mutationRevision;

            for (Item var6 : var1.items()) {
               NativeDeItemInventory.EntryState var7 = var3.entries.get(var6.objectId());
               if (var7 == null) {
                  throw new IllegalStateException("copy introduced an unknown object");
               }

               NativeDeItemInventory.BoundItem var8 = null;
               if (var7.actor != null) {
                  var8 = new NativeDeItemInventory.BoundItem(var11, var6);
                  NativeDeItemStateCopy.copy(var7.actor, var8);
               }

               var11.entries.put(var6.objectId(), new NativeDeItemInventory.EntryState(var6, var8));
            }

            var1.nativeDeBind(var11);
            return var1;
      }
   }

   private Item snapshot(NativeDeItemInventory.EntryState var1) {
      if (var1.actor == null) {
         return var1.view;
      } else {
         NativeDeItemInventory.BoundItem var2 = var1.actor;
         Item var3 = var1.view;
         Item var4 = new Item(
            var3.objectId(),
            var3.definition(),
            Math.toIntExact(var2.getCount()),
            var2.isEquipped(),
            var2.get_durability(),
            var2.getEnchantLevel(),
            var2.isIdentified(),
            packetBless(var2.getBless()),
            var3.sourceBound(),
            var3.tournamentReward(),
            var2.getChargeCount(),
            var3.expiresAtEpochMillis(),
            var2.isSealed(),
            var3.litoOwned()
         );
         if (!var4.equals(var3)) {
            var1.view = var4;
         }

         return var1.view;
      }
   }

   static int packetBless(int var0) {
      return var0 == 0 ? 1 : (var0 == 1 ? 0 : var0);
   }

   public Item get(Object var1) {
      synchronized (this.inventory) {
         NativeDeItemInventory.EntryState var3 = this.entries.get(var1);
         return var3 == null ? null : this.snapshot(var3);
      }
   }

   @Override
   public int size() {
      synchronized (this.inventory) {
         return this.entries.size();
      }
   }

   @Override
   public boolean containsKey(Object var1) {
      synchronized (this.inventory) {
         return this.entries.containsKey(var1);
      }
   }

   public Item put(Long var1, Item var2) {
      Objects.requireNonNull(var2);
      if (var1 != null && var1 == var2.objectId()) {
         synchronized (this.inventory) {
            NativeDeItemInventory.EntryState var4 = this.entries.get(var1);
            Item var5 = var4 == null ? null : this.snapshot(var4);
            NativeDeItemTemplate var6 = this.template(var2.definition());
            if (var4 == null) {
               NativeDeItemInventory.BoundItem var7 = var6 == null ? null : new NativeDeItemInventory.BoundItem(this, var2);
               this.entries.put(var1, new NativeDeItemInventory.EntryState(var2, var7));
            } else {
               if (var4.actor != null && var6 == null) {
                  throw new IllegalArgumentException("bound item morph has no captured source template");
               }

               if (var4.actor == null && var6 != null) {
                  var4.actor = new NativeDeItemInventory.BoundItem(this, var2);
               } else if (var4.actor != null) {
                  var4.actor.apply(var2, var6);
               }

               var4.view = var2;
            }

            this.updatePhysical(var1, var5, var2);
            this.mutationRevision++;
            return var5;
         }
      } else {
         throw new IllegalArgumentException("inventory object identity mismatch");
      }
   }

   private void updatePhysical(long var1, Item var3, Item var4) {
      Map var5 = this.inventory.nativeDePhysical();
      SourceWeaponPhysicalStats var6 = (SourceWeaponPhysicalStats)var5.get(var1);
      if (var6 != null) {
         if (var3 != null && var3.definition().catalogId() != var4.definition().catalogId()) {
            var5.remove(var1);
         } else {
            // T120 (#339): the blessed-weapon d4 follows the weapon template's
            // bless like 1-伺服器 L1AttackPc.java:113
            // (`_weapon.getItem().getBless()`). The combat traits already hold
            // the converted template bless from LegacyEquipmentMagicLoader, so
            // the instance bless (e.g. after a blessing scroll) no longer
            // overwrites it here.
            var6 = var6.withEnchantLevel(var4.enchantLevel()).withDurability(var4.durability());
            var5.put(var1, var6);
         }
      }
   }

   public Item remove(Object var1) {
      synchronized (this.inventory) {
         NativeDeItemInventory.EntryState var3 = this.entries.remove(var1);
         this.inventory.nativeDePhysical().remove(var1);
         if (var3 != null) {
            this.mutationRevision++;
         }

         return var3 == null ? null : this.snapshot(var3);
      }
   }

   @Override
   public void clear() {
      synchronized (this.inventory) {
         if (!this.entries.isEmpty()) {
            this.mutationRevision++;
         }

         this.entries.clear();
         this.inventory.nativeDePhysical().clear();
      }
   }

   @Override
   public Set<Entry<Long, Item>> entrySet() {
      synchronized (this.inventory) {
         LinkedHashSet var2 = new LinkedHashSet();
         this.entries.forEach((var2x, var3) -> var2.add(new SimpleImmutableEntry<>(var2x, this.snapshot(var3))));
         return Collections.unmodifiableSet(var2);
      }
   }

   private void changed(NativeDeItemInventory.BoundItem var1) {
      NativeDeItemInventory.EntryState var2 = this.entries.get(Integer.toUnsignedLong(var1.getId()));
      if (var2 != null && var2.actor == var1) {
         Item var3 = var2.view;
         Item var4 = this.snapshot(var2);
         this.updatePhysical(var4.objectId(), var3, var4);
         this.mutationRevision++;
      } else {
         throw new IllegalStateException("source item is detached from its inventory");
      }
   }

   void validateReplacement(RuntimeInventory var1) {
      NativeDeItemInventory var2 = this.copySource == null ? null : this.copySource.get();
      if (var2 != null && var2.inventory == var1 && var2.mutationRevision != this.copyRevision) {
         throw new IllegalStateException("live source item changed after native inventory copy");
      }
   }

   void ownerFields(int var1) {
      for (NativeDeItemInventory.EntryState var3 : this.entries.values()) {
         if (var3.actor != null) {
            var3.actor.setItemOwnerId(var1);
            var3.actor.set_char_objid(var1);
         }
      }
   }

   static RuntimeInventory rentalSnapshot(RuntimeInventory var0, String var1) {
      synchronized (var0) {
         if (var0.nativeDeItems() instanceof NativeDeItemInventory var3) {
            Owner var8 = var3.owner;
            String var5 = Objects.requireNonNull(var1).toLowerCase(Locale.ROOT);
            if (var8 != null && !var8.source.toLowerCase(Locale.ROOT).equals(var5) || var3.rentalCharacter != null && !var3.rentalCharacter.equals(var5)) {
               throw new IllegalArgumentException("rental source character differs from capsule");
            }

            if (var8 != null) {
               var3.rentalAccount = var8.account.toLowerCase(Locale.ROOT);
            }

            var3.rentalCharacter = var5;
            var3.owner = null;
            var3.copySource = null;
            var3.ownerFields(0);
         }

         return var0;
      }
   }

   private void requireAttached(NativeDeItemInventory.BoundItem var1) {
      NativeDeItemInventory.EntryState var2 = this.entries.get(Integer.toUnsignedLong(var1.getId()));
      if (var2 == null || var2.actor != var1) {
         throw new IllegalStateException("source item is detached from its inventory");
      } else if (this.rentalCharacter != null) {
         throw new IllegalStateException("rental source item awaits authenticated character admission");
      } else {
         Owner var3 = this.owner;
         if (var3 != null) {
            var3.requireActive(this.inventory);
         }
      }
   }

   static final class BoundItem extends local.lito381.server.NativeDeWorldKinds.Item {
      private final NativeDeItemInventory store;

      BoundItem(NativeDeItemInventory var1, Item var2) {
         super((int)var2.objectId());
         this.store = var1;
         this.apply(var2, Objects.requireNonNull(var1.template(var2.definition())));
      }

      private void apply(Item var1, NativeDeItemTemplate var2) {
         boolean var3 = this.getItem() == null;
         if (this.getItem() != var2) {
            super.setItem(var2);
         }

         if (var3 || this.getCount() != var1.quantity()) {
            super.setCount(var1.quantity());
         }

         if (this.isEquipped() != var1.equipped()) {
            super.setEquipped(var1.equipped());
         }

         if (this.get_durability() != var1.durability()) {
            super.set_durability(var1.durability());
         }

         if (this.getEnchantLevel() != var1.enchantLevel()) {
            super.setEnchantLevel(var1.enchantLevel());
         }

         if (this.isIdentified() != var1.identified()) {
            super.setIdentified(var1.identified());
         }

         if (this.getBless() != NativeDeItemInventory.packetBless(var1.blessing())) {
            super.setBless(NativeDeItemInventory.packetBless(var1.blessing()));
         }

         if (this.getChargeCount() != var1.chargeCount()) {
            super.setChargeCount(var1.chargeCount());
         }

         if (this.isSealed() != var1.sealed()) {
            super.setSealed(var1.sealed());
         }
      }

      private void check() {
         this.store.requireAttached(this);
      }

      private void validate(int var1, boolean var2, int var3, int var4, boolean var5, int var6, int var7, boolean var8) {
         Item var9 = this.store.entries.get(Integer.toUnsignedLong(this.getId())).view;
         new Item(
            var9.objectId(),
            var9.definition(),
            var1,
            var2,
            var3,
            var4,
            var5,
            NativeDeItemInventory.packetBless(var6),
            var9.sourceBound(),
            var9.tournamentReward(),
            var7,
            var9.expiresAtEpochMillis(),
            var8,
            var9.litoOwned()
         );
      }

      @Override
      public void setCount(long var1) {
         if (this.store == null) {
            super.setCount(var1);
         } else {
            synchronized (this.store.inventory) {
               this.check();
               if (var1 < 0L || var1 > 2147483647L) {
                  throw new IllegalArgumentException("native item count range");
               } else if (var1 == 0L) {
                  this.store.remove(Integer.toUnsignedLong(this.getId()));
                  super.setCount(0L);
               } else {
                  super.setCount(var1);
                  this.store.changed(this);
               }
            }
         }
      }

      @Override
      public void setEquipped(boolean var1) {
         if (this.store == null) {
            super.setEquipped(var1);
         } else {
            synchronized (this.store.inventory) {
               this.check();
               super.setEquipped(var1);
               this.store.changed(this);
            }
         }
      }

      @Override
      public void setIdentified(boolean var1) {
         if (this.store == null) {
            super.setIdentified(var1);
         } else {
            synchronized (this.store.inventory) {
               this.check();
               super.setIdentified(var1);
               this.store.changed(this);
            }
         }
      }

      @Override
      public void setSealed(boolean var1) {
         if (this.store == null) {
            super.setSealed(var1);
         } else {
            synchronized (this.store.inventory) {
               this.check();
               super.setSealed(var1);
               this.store.changed(this);
            }
         }
      }

      @Override
      public void setEnchantLevel(int var1) {
         if (this.store == null) {
            super.setEnchantLevel(var1);
         } else {
            synchronized (this.store.inventory) {
               this.check();
               this.validate(
                  Math.toIntExact(this.getCount()),
                  this.isEquipped(),
                  this.get_durability(),
                  var1,
                  this.isIdentified(),
                  this.getBless(),
                  this.getChargeCount(),
                  this.isSealed()
               );
               super.setEnchantLevel(var1);
               this.store.changed(this);
            }
         }
      }

      @Override
      public void set_durability(int var1) {
         if (this.store == null) {
            super.set_durability(var1);
         } else {
            synchronized (this.store.inventory) {
               this.check();
               this.validate(
                  Math.toIntExact(this.getCount()),
                  this.isEquipped(),
                  var1,
                  this.getEnchantLevel(),
                  this.isIdentified(),
                  this.getBless(),
                  this.getChargeCount(),
                  this.isSealed()
               );
               super.set_durability(var1);
               this.store.changed(this);
            }
         }
      }

      @Override
      public void setBless(int var1) {
         if (this.store == null) {
            super.setBless(var1);
         } else {
            synchronized (this.store.inventory) {
               this.check();
               this.validate(
                  Math.toIntExact(this.getCount()),
                  this.isEquipped(),
                  this.get_durability(),
                  this.getEnchantLevel(),
                  this.isIdentified(),
                  var1,
                  this.getChargeCount(),
                  this.isSealed()
               );
               super.setBless(var1);
               this.store.changed(this);
            }
         }
      }

      @Override
      public void setChargeCount(int var1) {
         if (this.store == null) {
            super.setChargeCount(var1);
         } else {
            synchronized (this.store.inventory) {
               this.check();
               this.validate(
                  Math.toIntExact(this.getCount()),
                  this.isEquipped(),
                  this.get_durability(),
                  this.getEnchantLevel(),
                  this.isIdentified(),
                  this.getBless(),
                  var1,
                  this.isSealed()
               );
               super.setChargeCount(var1);
               this.store.changed(this);
            }
         }
      }

      @Override
      public void setItem(NativeDeItemTemplate var1) {
         if (this.store == null) {
            super.setItem(var1);
         } else {
            synchronized (this.store.inventory) {
               this.check();
               if (var1 != this.getItem()) {
                  throw new IllegalArgumentException("morph requires native definition and captured source template together");
               } else {
                  super.setItem(var1);
                  this.store.changed(this);
               }
            }
         }
      }

      @Override
      public void setItemId(int var1) {
         if (this.store == null) {
            super.setItemId(var1);
         } else {
            synchronized (this.store.inventory) {
               this.check();
               if (var1 != this.getItemId()) {
                  throw new IllegalArgumentException("source item ID must match captured template");
               }
            }
         }
      }
   }

   private static final class EntryState {
      Item view;
      NativeDeItemInventory.BoundItem actor;

      EntryState(Item var1, NativeDeItemInventory.BoundItem var2) {
         this.view = var1;
         this.actor = var2;
      }
   }
}
