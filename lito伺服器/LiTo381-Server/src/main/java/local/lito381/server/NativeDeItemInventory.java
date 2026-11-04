// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import local.lito381.server.InventoryCatalog;
import local.lito381.server.NativeDeArmorTemplate;
import local.lito381.server.NativeDeItemApplication;
import local.lito381.server.NativeDeItemDefinitionsResources;
import local.lito381.server.NativeDeItemStateCopy;
import local.lito381.server.NativeDeItemTemplate;
import local.lito381.server.NativeDeItemTemplatesResources;
import local.lito381.server.NativeDeWeaponTemplate;
import local.lito381.server.NativeDeWorldKinds;
import local.lito381.server.RuntimeInventory;
import local.lito381.server.SourceWeaponCombatTraits;
import local.lito381.server.SourceWeaponPhysicalStats;

final class NativeDeItemInventory
extends AbstractMap<Long, RuntimeInventory.Item> {
    final RuntimeInventory inventory;
    final NativeDeItemTemplatesResources.Snapshot templates;
    final NativeDeItemDefinitionsResources.Snapshot definitions;
    volatile NativeDeItemApplication.Owner owner;
    String rentalCharacter;
    String rentalAccount;
    private long mutationRevision;
    private WeakReference<NativeDeItemInventory> copySource;
    private long copyRevision;
    private final LinkedHashMap<Long, EntryState> entries = new LinkedHashMap();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static NativeDeItemInventory bind(RuntimeInventory runtimeInventory, NativeDeItemTemplatesResources.Snapshot snapshot, NativeDeItemDefinitionsResources.Snapshot snapshot2) {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(snapshot2);
        RuntimeInventory runtimeInventory2 = runtimeInventory;
        synchronized (runtimeInventory2) {
            Map map = runtimeInventory.nativeDeItems();
            if (map instanceof NativeDeItemInventory) {
                NativeDeItemInventory nativeDeItemInventory = (NativeDeItemInventory)map;
                if (nativeDeItemInventory.templates != snapshot || nativeDeItemInventory.definitions != snapshot2) {
                    throw new IllegalStateException("inventory already captures another source generation");
                }
                return nativeDeItemInventory;
            }
            NativeDeItemInventory nativeDeItemInventory = new NativeDeItemInventory(runtimeInventory, snapshot, snapshot2);
            for (RuntimeInventory.Item item : runtimeInventory.items()) {
                nativeDeItemInventory.put(item.objectId(), item);
            }
            runtimeInventory.nativeDeBind(nativeDeItemInventory);
            return nativeDeItemInventory;
        }
    }

    private NativeDeItemInventory(RuntimeInventory runtimeInventory, NativeDeItemTemplatesResources.Snapshot snapshot, NativeDeItemDefinitionsResources.Snapshot snapshot2) {
        this.inventory = runtimeInventory;
        this.templates = snapshot;
        this.definitions = snapshot2;
    }

    private NativeDeItemTemplate template(long l) {
        return l > 0L && l <= Integer.MAX_VALUE ? this.templates.table().getTemplate((int)l) : null;
    }

    private NativeDeItemTemplate template(InventoryCatalog.Definition definition) {
        NativeDeItemTemplate nativeDeItemTemplate = this.template(definition.catalogId());
        if (nativeDeItemTemplate == null || nativeDeItemTemplate instanceof NativeDeWeaponTemplate != definition.weapon() || nativeDeItemTemplate instanceof NativeDeArmorTemplate != definition.armor()) {
            return null;
        }
        return nativeDeItemTemplate;
    }

    static boolean shuffle(RuntimeInventory runtimeInventory, Random random) {
        Object object = runtimeInventory.nativeDeItems();
        if (!(object instanceof NativeDeItemInventory)) {
            return false;
        }
        NativeDeItemInventory nativeDeItemInventory = (NativeDeItemInventory)object;
        if (random == null) {
            throw new IllegalArgumentException("missing inventory random");
        }
        ArrayList<Map.Entry<Long, EntryState>> shuffled = new ArrayList<Map.Entry<Long, EntryState>>(nativeDeItemInventory.entries.entrySet());
        Collections.shuffle(shuffled, random);
        nativeDeItemInventory.entries.clear();
        Iterator<Map.Entry<Long, EntryState>> iterator = shuffled.iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, EntryState> entry = iterator.next();
            nativeDeItemInventory.entries.put(entry.getKey(), entry.getValue());
        }
        ++nativeDeItemInventory.mutationRevision;
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    NativeDeWorldKinds.Item actor(long l) {
        RuntimeInventory runtimeInventory = this.inventory;
        synchronized (runtimeInventory) {
            EntryState entryState = this.entries.get(l);
            return entryState == null ? null : entryState.actor;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    Set<Long> opaqueObjects() {
        RuntimeInventory runtimeInventory = this.inventory;
        synchronized (runtimeInventory) {
            LinkedHashSet linkedHashSet = new LinkedHashSet();
            this.entries.forEach((l, entryState) -> {
                if (entryState.actor == null) {
                    linkedHashSet.add(l);
                }
            });
            return Collections.unmodifiableSet(linkedHashSet);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static RuntimeInventory preserveCopy(RuntimeInventory runtimeInventory, RuntimeInventory runtimeInventory2) {
        RuntimeInventory runtimeInventory3 = runtimeInventory;
        synchronized (runtimeInventory3) {
            Map map = runtimeInventory.nativeDeItems();
            if (!(map instanceof NativeDeItemInventory)) {
                return runtimeInventory2;
            }
            NativeDeItemInventory nativeDeItemInventory = (NativeDeItemInventory)map;
            map = new NativeDeItemInventory(runtimeInventory2, nativeDeItemInventory.templates, nativeDeItemInventory.definitions);
            ((NativeDeItemInventory)map).owner = nativeDeItemInventory.owner;
            ((NativeDeItemInventory)map).rentalCharacter = nativeDeItemInventory.rentalCharacter;
            ((NativeDeItemInventory)map).rentalAccount = nativeDeItemInventory.rentalAccount;
            ((NativeDeItemInventory)map).copySource = new WeakReference<NativeDeItemInventory>(nativeDeItemInventory);
            ((NativeDeItemInventory)map).copyRevision = nativeDeItemInventory.mutationRevision;
            for (RuntimeInventory.Item item : runtimeInventory2.items()) {
                EntryState entryState = nativeDeItemInventory.entries.get(item.objectId());
                if (entryState == null) {
                    throw new IllegalStateException("copy introduced an unknown object");
                }
                BoundItem boundItem = null;
                if (entryState.actor != null) {
                    boundItem = new BoundItem((NativeDeItemInventory)map, item);
                    NativeDeItemStateCopy.copy(entryState.actor, boundItem);
                }
                ((NativeDeItemInventory)map).entries.put(item.objectId(), new EntryState(item, boundItem));
            }
            runtimeInventory2.nativeDeBind(map);
            return runtimeInventory2;
        }
    }

    private RuntimeInventory.Item snapshot(EntryState entryState) {
        if (entryState.actor == null) {
            return entryState.view;
        }
        BoundItem boundItem = entryState.actor;
        RuntimeInventory.Item item = entryState.view;
        RuntimeInventory.Item item2 = new RuntimeInventory.Item(item.objectId(), item.definition(), Math.toIntExact(boundItem.getCount()), boundItem.isEquipped(), boundItem.get_durability(), boundItem.getEnchantLevel(), boundItem.isIdentified(), NativeDeItemInventory.packetBless(boundItem.getBless()), item.sourceBound(), item.tournamentReward(), boundItem.getChargeCount(), item.expiresAtEpochMillis(), boundItem.isSealed(), item.litoOwned());
        if (!item2.equals(item)) {
            entryState.view = item2;
        }
        return entryState.view;
    }

    static int packetBless(int n) {
        return n == 0 ? 1 : (n == 1 ? 0 : n);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public RuntimeInventory.Item get(Object object) {
        RuntimeInventory runtimeInventory = this.inventory;
        synchronized (runtimeInventory) {
            EntryState entryState = this.entries.get(object);
            return entryState == null ? null : this.snapshot(entryState);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int size() {
        RuntimeInventory runtimeInventory = this.inventory;
        synchronized (runtimeInventory) {
            return this.entries.size();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean containsKey(Object object) {
        RuntimeInventory runtimeInventory = this.inventory;
        synchronized (runtimeInventory) {
            return this.entries.containsKey(object);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public RuntimeInventory.Item put(Long l, RuntimeInventory.Item item) {
        Objects.requireNonNull(item);
        if (l == null || l.longValue() != item.objectId()) {
            throw new IllegalArgumentException("inventory object identity mismatch");
        }
        RuntimeInventory runtimeInventory = this.inventory;
        synchronized (runtimeInventory) {
            EntryState entryState = this.entries.get(l);
            RuntimeInventory.Item item2 = entryState == null ? null : this.snapshot(entryState);
            NativeDeItemTemplate nativeDeItemTemplate = this.template(item.definition());
            if (entryState == null) {
                BoundItem boundItem = nativeDeItemTemplate == null ? null : new BoundItem(this, item);
                this.entries.put(l, new EntryState(item, boundItem));
            } else {
                if (entryState.actor != null && nativeDeItemTemplate == null) {
                    throw new IllegalArgumentException("bound item morph has no captured source template");
                }
                if (entryState.actor == null && nativeDeItemTemplate != null) {
                    entryState.actor = new BoundItem(this, item);
                } else if (entryState.actor != null) {
                    entryState.actor.apply(item, nativeDeItemTemplate);
                }
                entryState.view = item;
            }
            this.updatePhysical(l, item2, item);
            ++this.mutationRevision;
            return item2;
        }
    }

    private void updatePhysical(long l, RuntimeInventory.Item item, RuntimeInventory.Item item2) {
        Map map = this.inventory.nativeDePhysical();
        SourceWeaponPhysicalStats sourceWeaponPhysicalStats = (SourceWeaponPhysicalStats)map.get(l);
        if (sourceWeaponPhysicalStats == null) {
            return;
        }
        if (item != null && item.definition().catalogId() != item2.definition().catalogId()) {
            map.remove(l);
        } else {
            sourceWeaponPhysicalStats = sourceWeaponPhysicalStats.withEnchantLevel(item2.enchantLevel()).withDurability(item2.durability());
            if (item2.blessing() <= 2) {
                SourceWeaponCombatTraits sourceWeaponCombatTraits = sourceWeaponPhysicalStats.combatTraits();
                sourceWeaponPhysicalStats = sourceWeaponPhysicalStats.withCombatTraits(new SourceWeaponCombatTraits(sourceWeaponCombatTraits.material(), item2.blessing(), sourceWeaponCombatTraits.attributeKind(), sourceWeaponCombatTraits.attributeLevel(), sourceWeaponCombatTraits.attributeDamage(), sourceWeaponCombatTraits.doubleDamageChance(), sourceWeaponCombatTraits.doubleDamageMultiplier(), sourceWeaponCombatTraits.maximumDamageChance(), sourceWeaponCombatTraits.doubleDamageEffect(), sourceWeaponCombatTraits.maximumDamageEffect(), sourceWeaponCombatTraits.inventoryGfx()));
            }
            map.put(l, sourceWeaponPhysicalStats);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public RuntimeInventory.Item remove(Object object) {
        RuntimeInventory runtimeInventory = this.inventory;
        synchronized (runtimeInventory) {
            EntryState entryState = (EntryState)this.entries.remove(object);
            this.inventory.nativeDePhysical().remove(object);
            if (entryState != null) {
                ++this.mutationRevision;
            }
            return entryState == null ? null : this.snapshot(entryState);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void clear() {
        RuntimeInventory runtimeInventory = this.inventory;
        synchronized (runtimeInventory) {
            if (!this.entries.isEmpty()) {
                ++this.mutationRevision;
            }
            this.entries.clear();
            this.inventory.nativeDePhysical().clear();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public Set<Map.Entry<Long, RuntimeInventory.Item>> entrySet() {
        RuntimeInventory runtimeInventory = this.inventory;
        synchronized (runtimeInventory) {
            LinkedHashSet linkedHashSet = new LinkedHashSet();
            this.entries.forEach((l, entryState) -> linkedHashSet.add(new AbstractMap.SimpleImmutableEntry<Long, RuntimeInventory.Item>((Long)l, this.snapshot((EntryState)entryState))));
            return Collections.unmodifiableSet(linkedHashSet);
        }
    }

    private void changed(BoundItem boundItem) {
        EntryState entryState = this.entries.get(Integer.toUnsignedLong(boundItem.getId()));
        if (entryState == null || entryState.actor != boundItem) {
            throw new IllegalStateException("source item is detached from its inventory");
        }
        RuntimeInventory.Item item = entryState.view;
        RuntimeInventory.Item item2 = this.snapshot(entryState);
        this.updatePhysical(item2.objectId(), item, item2);
        ++this.mutationRevision;
    }

    void validateReplacement(RuntimeInventory runtimeInventory) {
        NativeDeItemInventory nativeDeItemInventory;
        NativeDeItemInventory nativeDeItemInventory2 = nativeDeItemInventory = this.copySource == null ? null : (NativeDeItemInventory)this.copySource.get();
        if (nativeDeItemInventory != null && nativeDeItemInventory.inventory == runtimeInventory && nativeDeItemInventory.mutationRevision != this.copyRevision) {
            throw new IllegalStateException("live source item changed after native inventory copy");
        }
    }

    void ownerFields(int n) {
        for (EntryState entryState : this.entries.values()) {
            if (entryState.actor == null) continue;
            entryState.actor.setItemOwnerId(n);
            entryState.actor.set_char_objid(n);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static RuntimeInventory rentalSnapshot(RuntimeInventory runtimeInventory, String string) {
        RuntimeInventory runtimeInventory2 = runtimeInventory;
        synchronized (runtimeInventory2) {
            Object object = runtimeInventory.nativeDeItems();
            if (object instanceof NativeDeItemInventory) {
                NativeDeItemInventory nativeDeItemInventory = (NativeDeItemInventory)object;
                object = nativeDeItemInventory.owner;
                String string2 = Objects.requireNonNull(string).toLowerCase(Locale.ROOT);
                if (object != null && !((NativeDeItemApplication.Owner)object).source.toLowerCase(Locale.ROOT).equals(string2) || nativeDeItemInventory.rentalCharacter != null && !nativeDeItemInventory.rentalCharacter.equals(string2)) {
                    throw new IllegalArgumentException("rental source character differs from capsule");
                }
                if (object != null) {
                    nativeDeItemInventory.rentalAccount = ((NativeDeItemApplication.Owner)object).account.toLowerCase(Locale.ROOT);
                }
                nativeDeItemInventory.rentalCharacter = string2;
                nativeDeItemInventory.owner = null;
                nativeDeItemInventory.copySource = null;
                nativeDeItemInventory.ownerFields(0);
            }
            return runtimeInventory;
        }
    }

    private void requireAttached(BoundItem boundItem) {
        EntryState entryState = this.entries.get(Integer.toUnsignedLong(boundItem.getId()));
        if (entryState == null || entryState.actor != boundItem) {
            throw new IllegalStateException("source item is detached from its inventory");
        }
        if (this.rentalCharacter != null) {
            throw new IllegalStateException("rental source item awaits authenticated character admission");
        }
        NativeDeItemApplication.Owner owner = this.owner;
        if (owner != null) {
            owner.requireActive(this.inventory);
        }
    }

    private static final class EntryState {
        RuntimeInventory.Item view;
        BoundItem actor;

        EntryState(RuntimeInventory.Item item, BoundItem boundItem) {
            this.view = item;
            this.actor = boundItem;
        }
    }

    static final class BoundItem
    extends NativeDeWorldKinds.Item {
        private final NativeDeItemInventory store;

        BoundItem(NativeDeItemInventory nativeDeItemInventory, RuntimeInventory.Item item) {
            super((int)item.objectId());
            this.store = nativeDeItemInventory;
            this.apply(item, Objects.requireNonNull(nativeDeItemInventory.template(item.definition())));
        }

        private void apply(RuntimeInventory.Item item, NativeDeItemTemplate nativeDeItemTemplate) {
            boolean bl;
            boolean bl2 = bl = this.getItem() == null;
            if (this.getItem() != nativeDeItemTemplate) {
                super.setItem(nativeDeItemTemplate);
            }
            if (bl || this.getCount() != (long)item.quantity()) {
                super.setCount(item.quantity());
            }
            if (this.isEquipped() != item.equipped()) {
                super.setEquipped(item.equipped());
            }
            if (this.get_durability() != item.durability()) {
                super.set_durability(item.durability());
            }
            if (this.getEnchantLevel() != item.enchantLevel()) {
                super.setEnchantLevel(item.enchantLevel());
            }
            if (this.isIdentified() != item.identified()) {
                super.setIdentified(item.identified());
            }
            if (this.getBless() != NativeDeItemInventory.packetBless(item.blessing())) {
                super.setBless(NativeDeItemInventory.packetBless(item.blessing()));
            }
            if (this.getChargeCount() != item.chargeCount()) {
                super.setChargeCount(item.chargeCount());
            }
            if (this.isSealed() != item.sealed()) {
                super.setSealed(item.sealed());
            }
        }

        private void check() {
            this.store.requireAttached(this);
        }

        private void validate(int n, boolean bl, int n2, int n3, boolean bl2, int n4, int n5, boolean bl3) {
            RuntimeInventory.Item item = this.store.entries.get((Object)Long.valueOf((long)Integer.toUnsignedLong((int)this.getId()))).view;
            new RuntimeInventory.Item(item.objectId(), item.definition(), n, bl, n2, n3, bl2, NativeDeItemInventory.packetBless(n4), item.sourceBound(), item.tournamentReward(), n5, item.expiresAtEpochMillis(), bl3, item.litoOwned());
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void setCount(long l) {
            if (this.store == null) {
                super.setCount(l);
                return;
            }
            RuntimeInventory runtimeInventory = this.store.inventory;
            synchronized (runtimeInventory) {
                this.check();
                if (l < 0L || l > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("native item count range");
                }
                if (l == 0L) {
                    this.store.remove(Integer.toUnsignedLong(this.getId()));
                    super.setCount(0L);
                    return;
                }
                super.setCount(l);
                this.store.changed(this);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void setEquipped(boolean bl) {
            if (this.store == null) {
                super.setEquipped(bl);
                return;
            }
            RuntimeInventory runtimeInventory = this.store.inventory;
            synchronized (runtimeInventory) {
                this.check();
                super.setEquipped(bl);
                this.store.changed(this);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void setIdentified(boolean bl) {
            if (this.store == null) {
                super.setIdentified(bl);
                return;
            }
            RuntimeInventory runtimeInventory = this.store.inventory;
            synchronized (runtimeInventory) {
                this.check();
                super.setIdentified(bl);
                this.store.changed(this);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void setSealed(boolean bl) {
            if (this.store == null) {
                super.setSealed(bl);
                return;
            }
            RuntimeInventory runtimeInventory = this.store.inventory;
            synchronized (runtimeInventory) {
                this.check();
                super.setSealed(bl);
                this.store.changed(this);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void setEnchantLevel(int n) {
            if (this.store == null) {
                super.setEnchantLevel(n);
                return;
            }
            RuntimeInventory runtimeInventory = this.store.inventory;
            synchronized (runtimeInventory) {
                this.check();
                this.validate(Math.toIntExact(this.getCount()), this.isEquipped(), this.get_durability(), n, this.isIdentified(), this.getBless(), this.getChargeCount(), this.isSealed());
                super.setEnchantLevel(n);
                this.store.changed(this);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void set_durability(int n) {
            if (this.store == null) {
                super.set_durability(n);
                return;
            }
            RuntimeInventory runtimeInventory = this.store.inventory;
            synchronized (runtimeInventory) {
                this.check();
                this.validate(Math.toIntExact(this.getCount()), this.isEquipped(), n, this.getEnchantLevel(), this.isIdentified(), this.getBless(), this.getChargeCount(), this.isSealed());
                super.set_durability(n);
                this.store.changed(this);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void setBless(int n) {
            if (this.store == null) {
                super.setBless(n);
                return;
            }
            RuntimeInventory runtimeInventory = this.store.inventory;
            synchronized (runtimeInventory) {
                this.check();
                this.validate(Math.toIntExact(this.getCount()), this.isEquipped(), this.get_durability(), this.getEnchantLevel(), this.isIdentified(), n, this.getChargeCount(), this.isSealed());
                super.setBless(n);
                this.store.changed(this);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void setChargeCount(int n) {
            if (this.store == null) {
                super.setChargeCount(n);
                return;
            }
            RuntimeInventory runtimeInventory = this.store.inventory;
            synchronized (runtimeInventory) {
                this.check();
                this.validate(Math.toIntExact(this.getCount()), this.isEquipped(), this.get_durability(), this.getEnchantLevel(), this.isIdentified(), this.getBless(), n, this.isSealed());
                super.setChargeCount(n);
                this.store.changed(this);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void setItem(NativeDeItemTemplate nativeDeItemTemplate) {
            if (this.store == null) {
                super.setItem(nativeDeItemTemplate);
                return;
            }
            RuntimeInventory runtimeInventory = this.store.inventory;
            synchronized (runtimeInventory) {
                this.check();
                if (nativeDeItemTemplate != this.getItem()) {
                    throw new IllegalArgumentException("morph requires native definition and captured source template together");
                }
                super.setItem(nativeDeItemTemplate);
                this.store.changed(this);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void setItemId(int n) {
            if (this.store == null) {
                super.setItemId(n);
                return;
            }
            RuntimeInventory runtimeInventory = this.store.inventory;
            synchronized (runtimeInventory) {
                this.check();
                if (n != this.getItemId()) {
                    throw new IllegalArgumentException("source item ID must match captured template");
                }
            }
        }
    }
}
