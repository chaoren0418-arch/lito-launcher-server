// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import local.lito381.server.NativeDeDeletionPorts.Inventory;
import local.lito381.server.NativeDeNpcConstructionPorts.Services;
import local.lito381.server.NativeDeNpcInventoryScope.World;
import local.lito381.server.NativeDeWorldKinds.Item;

class NativeDeNpcInventory extends NativeDeObjectFields implements Inventory {
   final Services nativeServices;
   protected List<Item> _items = new CopyOnWriteArrayList<>();
   protected volatile boolean _weightDirty = true;
   protected int _cachedWeight = 0;
   protected final ReentrantLock _weightMutex = new ReentrantLock();
   private final ReentrantLock _invLock = new ReentrantLock();

   @Override
   public void clearItems() {
      NativeDeNpcInventoryScope.run(this.nativeServices, this::sourceClearItems);
   }

   static boolean countAdditionFits(long var0, long var2) {
      return var0 >= 0L && var2 > 0L && var0 <= Long.MAX_VALUE - var2;
   }

   static long checkedAddedWeight(int var0, long var1) {
      if (var0 < 0 || var1 <= 0L) {
         return -1L;
      } else if (var0 == 0) {
         return 0L;
      } else {
         try {
            return Math.max(Math.multiplyExact((long)var0, var1) / 1000L, 1L);
         } catch (ArithmeticException var4) {
            return -1L;
         }
      }
   }

   public NativeDeNpcInventory(Services var1) {
      this.nativeServices = Objects.requireNonNull(var1);
   }

   public int getSize() {
      return this._items.isEmpty() ? 0 : this._items.size();
   }

   public List<Item> getItems() {
      return this._items;
   }

   private void sourceClearItems() {
      this._weightMutex.lock();

      try {
         this._weightDirty = true;
      } finally {
         this._weightMutex.unlock();
      }

      for (Object var2 : this._items) {
         Item var3 = (Item)var2;
         World.get().removeObject(var3);
      }
   }

   @Override
   protected int getId() {
      return 0;
   }
}
