// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import local.lito381.server.NativeDeDeletionPorts.ObjectRef;
import local.lito381.server.NativeDeWorldContext.Entry;

final class NativeDeNpcKnowledge {
   static boolean tryAdd(NativeDeDeletionState var0, ObjectRef var1) {
      ReentrantLock var2 = (ReentrantLock)CombatAccess.field(var0, "_knownObjectLock");
      var2.lock();

      boolean var3;
      try {
         var3 = admitted(var0, var1);
      } finally {
         var2.unlock();
      }

      return var3;
   }

   private static boolean admitted(NativeDeDeletionState var0, ObjectRef var1) {
      NativePlayerLifetime var2 = var0.deletionOwner.timer.life;
      if (var2.admits() && !var0.destroyed() && !var0.deletionStarted()) {
         Entry var3 = (Entry)CombatAccess.field(var0, "sourceDeWorldEntry");
         if (var3 != null && var3.context.closed) {
            return false;
         } else if (var1 instanceof NativeDeDeletionState var4 && var4.deletionOwner.timer.life != var2) {
            return false;
         } else if (!(var1 instanceof NativeDeClientViewer var5 && (var5.life != var2 || var5.retired))) {
            if (var1 instanceof NativeDeObjectFields) {
               Entry var6 = (Entry)CombatAccess.field(var1, "sourceDeWorldEntry");
               if (var6 != null && (var6.context.life != var2 || var6.context.closed)) {
                  return false;
               }
            }

            return var0.tryAddKnownObject(var1);
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   static void collect(Object var0, NativeDeWorldContext var1, Set<Object> var2, Set<NativeDeDeletionState> var3) {
      if (var0 == null || !var2.add(var0)) { return; }
         if (var0 instanceof NativeDeDeletionState var4) {
            if (var4.deletionOwner == var1.deletion) {
               var3.add(var4);
            }
         } else if (var0 instanceof Map var5) {
            for (Object var7 : var5.values()) {
               collect(var7, var1, var2, var3);
            }
         } else if (var0.getClass().getEnclosingClass() == var1.core.getClass() && var0.getClass().getSimpleName().equals("VisibleBinding")) {
            collect(CombatAccess.call(var0, "object", "()Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;"), var1, var2, var3);
         }

   }

   static void retireWorld(NativeDeWorldContext var0) {
      Set var1 = Collections.newSetFromMap(new IdentityHashMap());
      Set<NativeDeDeletionState> var2 = Collections.newSetFromMap(new IdentityHashMap<>());

      for (String var4 : List.of("_allObjects", "_visibleObjects", "_visibleBindings", "_spatialIndex")) {
         collect(CombatAccess.field(var0.core, var4), var0, var1, var2);
      }

      for (NativeDeDeletionState var8 : var2) {
         try {
            var8.removeAllKnownObjects();
         } catch (Throwable var6) {
            CombatNpcDamageQueue.failed(var6);
         }
      }
   }
}
