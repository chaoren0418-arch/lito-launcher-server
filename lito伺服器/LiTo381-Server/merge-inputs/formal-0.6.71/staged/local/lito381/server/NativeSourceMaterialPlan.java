// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

final class NativeSourceMaterialPlan {
   private static final NativeReuseLog.Logger _log = NativeReuseLog.getLogger(NativeSourceMaterialPlan.class);
   private final List<NativeMaterialItem> _items;
   private final int owner;

   private NativeSourceMaterialPlan(List<NativeMaterialItem> var1, int var2) {
      this._items = var1;
      this.owner = var2;
   }

   private int ownerId() {
      return this.owner;
   }

   static NativeSourceMaterialPlan.Result plan(List<NativeMaterialItem> var0, int var1, int var2, long var3) {
      if (var2 > 0 && var3 > 0L) {
         NativeSourceMaterialPlan var5 = new NativeSourceMaterialPlan(var0, var1);
         ArrayList<NativeSourceMaterialPlan.PlannedConsumption> var6 = new ArrayList<>();
         NativeSourceMaterialPlan.ItemConsumeCommitResult var7 = var5.appendGeneralConsumePlan(
            var6, new NativeSourceMaterialPlan.ConsumeRequirement(var2, var3, -1)
         );
         if (var7 != null) {
            return new NativeSourceMaterialPlan.Result(var7, List.of());
         } else {
            ArrayList<NativeSourceMaterialPlan.Debit> var8 = new ArrayList<>();

            for (NativeSourceMaterialPlan.PlannedConsumption var10 : var6) {
               var8.add(new NativeSourceMaterialPlan.Debit(Integer.toUnsignedLong(var10.item.getId()), var10.oldCount, var10.consumeCount, var10.newCount));
            }

            return new NativeSourceMaterialPlan.Result(NativeSourceMaterialPlan.ItemConsumeCommitResult.COMMITTED, List.copyOf(var8));
         }
      } else {
         return new NativeSourceMaterialPlan.Result(NativeSourceMaterialPlan.ItemConsumeCommitResult.INVALID_REQUEST, List.of());
      }
   }

   private NativeSourceMaterialPlan.ItemConsumeCommitResult appendGeneralConsumePlan(
      List<NativeSourceMaterialPlan.PlannedConsumption> var1, NativeSourceMaterialPlan.ConsumeRequirement var2
   ) {
      int var3 = var2.itemId;
      long var4 = var2.count;
      int var6 = var2.minEnchant;
      ArrayList<NativeMaterialItem> var7 = new ArrayList<>();
      Set var8 = Collections.newSetFromMap(new IdentityHashMap());
      Boolean var9 = null;

      for (NativeMaterialItem var11 : this._items) {
         if (var11 != null
            && var11.getItem() != null
            && var11.getItemId() == var3
            && var11.get_char_objid() == this.ownerId()
            && !var11.isEquipped()
            && !var11.isSealed()
            && var11.getCount() > 0L
            && var8.add(var11)) {
            boolean var12 = var11.isStackable();
            if (var9 == null) {
               var9 = var12;
            } else if (var9 != var12) {
               _log.error("背包同 itemId 混用 stackable 屬性，拒絕扣款 charId={} itemId={}", this.ownerId(), var3);
               return NativeSourceMaterialPlan.ItemConsumeCommitResult.INVALID_REQUEST;
            }

            if (var12) {
               if (var6 > 0) {
                  return NativeSourceMaterialPlan.ItemConsumeCommitResult.INVALID_REQUEST;
               }
            } else if (var6 >= 0) {
               int var13 = var11.getEnchantLevel();
               if (var6 == 0 && var13 != 0 || var6 > 0 && var13 < var6) {
                  continue;
               }
            }

            var7.add(var11);
         }
      }

      if (var9 != null && !var7.isEmpty()) {
         var7.sort(Comparator.comparingInt(NativeMaterialItem::getEnchantLevel).thenComparingInt(NativeMaterialItem::getId));
         long var16 = var4;
         if (var9) {
            for (NativeMaterialItem var19 : var7) {
               if (var16 == 0L) {
                  break;
               }

               long var14 = Math.min(var16, var19.getCount());
               var1.add(new NativeSourceMaterialPlan.PlannedConsumption(var19, var14));
               var16 -= var14;
            }
         } else {
            if (var7.size() < var4) {
               return NativeSourceMaterialPlan.ItemConsumeCommitResult.INSUFFICIENT_ITEMS;
            }

            int var18 = (int)var4;

            for (int var20 = 0; var20 < var18; var20++) {
               var1.add(new NativeSourceMaterialPlan.PlannedConsumption((NativeMaterialItem)var7.get(var20), 1L));
               var16--;
            }
         }

         return var16 == 0L ? null : NativeSourceMaterialPlan.ItemConsumeCommitResult.INSUFFICIENT_ITEMS;
      } else {
         return NativeSourceMaterialPlan.ItemConsumeCommitResult.INSUFFICIENT_ITEMS;
      }
   }

   private static final class ConsumeRequirement {
      private final int itemId;
      private final long count;
      private final int minEnchant;

      private ConsumeRequirement(int var1, long var2, int var4) {
         this.itemId = var1;
         this.count = var2;
         this.minEnchant = var4;
      }
   }

   record Debit(long objectId, long oldCount, long count, long newCount) {
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

      private ItemConsumeCommitResult(boolean var3) {
         this.effectAllowed = var3;
      }

      public boolean isEffectAllowed() {
         return this.effectAllowed;
      }
   }

   private static final class PlannedConsumption {
      private final NativeMaterialItem item;
      private final long oldCount;
      private final long consumeCount;
      private final long newCount;

      private PlannedConsumption(NativeMaterialItem var1, long var2) {
         this.item = var1;
         this.oldCount = var1.getCount();
         this.consumeCount = var2;
         this.newCount = this.oldCount - var2;
      }
   }

   record Result(NativeSourceMaterialPlan.ItemConsumeCommitResult result, List<NativeSourceMaterialPlan.Debit> debits) {
      boolean allowed() {
         return this.result == NativeSourceMaterialPlan.ItemConsumeCommitResult.COMMITTED;
      }
   }
}
