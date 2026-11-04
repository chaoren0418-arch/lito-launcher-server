// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import local.lito381.server.NativeMaterialItem;
import local.lito381.server.NativeReuseLog;

final class NativeSourceMaterialPlan {
    private static final NativeReuseLog.Logger _log = NativeReuseLog.getLogger(NativeSourceMaterialPlan.class);
    private final List<NativeMaterialItem> _items;
    private final int owner;

    private NativeSourceMaterialPlan(List<NativeMaterialItem> list, int n) {
        this._items = list;
        this.owner = n;
    }

    private int ownerId() {
        return this.owner;
    }

    static Result plan(List<NativeMaterialItem> list, int n, int n2, long l) {
        if (n2 <= 0 || l <= 0L) {
            return new Result(ItemConsumeCommitResult.INVALID_REQUEST, List.of());
        }
        NativeSourceMaterialPlan nativeSourceMaterialPlan = new NativeSourceMaterialPlan(list, n);
        ArrayList<PlannedConsumption> arrayList = new ArrayList<PlannedConsumption>();
        ItemConsumeCommitResult itemConsumeCommitResult = nativeSourceMaterialPlan.appendGeneralConsumePlan(arrayList, new ConsumeRequirement(n2, l, -1));
        if (itemConsumeCommitResult != null) {
            return new Result(itemConsumeCommitResult, List.of());
        }
        ArrayList<Debit> arrayList2 = new ArrayList<Debit>();
        for (PlannedConsumption plannedConsumption : arrayList) {
            arrayList2.add(new Debit(Integer.toUnsignedLong(plannedConsumption.item.getId()), plannedConsumption.oldCount, plannedConsumption.consumeCount, plannedConsumption.newCount));
        }
        return new Result(ItemConsumeCommitResult.COMMITTED, List.copyOf(arrayList2));
    }

    private ItemConsumeCommitResult appendGeneralConsumePlan(List<PlannedConsumption> list, ConsumeRequirement consumeRequirement) {
        int n;
        int n2;
        int n3 = consumeRequirement.itemId;
        long l = consumeRequirement.count;
        int n4 = consumeRequirement.minEnchant;
        ArrayList<NativeMaterialItem> arrayList = new ArrayList<NativeMaterialItem>();
        Set set = Collections.newSetFromMap(new IdentityHashMap());
        Boolean bl = null;
        for (NativeMaterialItem nativeMaterialItem : this._items) {
            if (nativeMaterialItem == null || nativeMaterialItem.getItem() == null || nativeMaterialItem.getItemId() != n3 || nativeMaterialItem.get_char_objid() != this.ownerId() || nativeMaterialItem.isEquipped() || nativeMaterialItem.isSealed() || nativeMaterialItem.getCount() <= 0L || !set.add(nativeMaterialItem)) continue;
            n2 = nativeMaterialItem.isStackable() ? 1 : 0;
            if (bl == null) {
                bl = n2 != 0;
            } else if (bl.booleanValue() != (n2 != 0)) {
                _log.error("\u80cc\u5305\u540c itemId \u6df7\u7528 stackable \u5c6c\u6027\uff0c\u62d2\u7d55\u6263\u6b3e charId={} itemId={}", this.ownerId(), n3);
                return ItemConsumeCommitResult.INVALID_REQUEST;
            }
            if (n2 != 0) {
                if (n4 > 0) {
                    return ItemConsumeCommitResult.INVALID_REQUEST;
                }
            } else if (n4 >= 0) {
                n = nativeMaterialItem.getEnchantLevel();
                if (n4 == 0 && n != 0 || n4 > 0 && n < n4) continue;
            }
            arrayList.add(nativeMaterialItem);
        }
        if (bl == null || arrayList.isEmpty()) {
            return ItemConsumeCommitResult.INSUFFICIENT_ITEMS;
        }
        arrayList.sort(Comparator.comparingInt(NativeMaterialItem::getEnchantLevel).thenComparingInt(NativeMaterialItem::getId));
        long l2 = l;
        if (bl.booleanValue()) {
            for (NativeMaterialItem nativeMaterialItem : arrayList) {
                if (l2 != 0L) {
                    long l3 = Math.min(l2, nativeMaterialItem.getCount());
                    list.add(new PlannedConsumption(nativeMaterialItem, l3));
                    l2 -= l3;
                    continue;
                }
                break;
            }
        } else {
            if ((long)arrayList.size() < l) {
                return ItemConsumeCommitResult.INSUFFICIENT_ITEMS;
            }
            n2 = (int)l;
            for (n = 0; n < n2; ++n) {
                list.add(new PlannedConsumption((NativeMaterialItem)arrayList.get(n), 1L));
                --l2;
            }
        }
        return l2 == 0L ? null : ItemConsumeCommitResult.INSUFFICIENT_ITEMS;
    }

    record Result(ItemConsumeCommitResult result, List<Debit> debits) {
        boolean allowed() {
            return this.result == ItemConsumeCommitResult.COMMITTED;
        }
    }

    public static enum ItemConsumeCommitResult {
        COMMITTED(true),
        COMMITTED_MEMORY_REPAIRED(true),
        INVALID_REQUEST(false),
        INSUFFICIENT_ITEMS(false),
        PERSISTENCE_FAILED(false),
        COMPENSATION_FAILED(false),
        MEMORY_SYNC_FAILED(false);

        private final boolean effectAllowed;

        private ItemConsumeCommitResult(boolean bl) {
            this.effectAllowed = bl;
        }

        public boolean isEffectAllowed() {
            return this.effectAllowed;
        }
    }

    private static final class ConsumeRequirement {
        private final int itemId;
        private final long count;
        private final int minEnchant;

        private ConsumeRequirement(int n, long l, int n2) {
            this.itemId = n;
            this.count = l;
            this.minEnchant = n2;
        }
    }

    private static final class PlannedConsumption {
        private final NativeMaterialItem item;
        private final long oldCount;
        private final long consumeCount;
        private final long newCount;

        private PlannedConsumption(NativeMaterialItem nativeMaterialItem, long l) {
            this.item = nativeMaterialItem;
            this.oldCount = nativeMaterialItem.getCount();
            this.consumeCount = l;
            this.newCount = this.oldCount - l;
        }
    }

    record Debit(long objectId, long oldCount, long count, long newCount) {
    }
}
