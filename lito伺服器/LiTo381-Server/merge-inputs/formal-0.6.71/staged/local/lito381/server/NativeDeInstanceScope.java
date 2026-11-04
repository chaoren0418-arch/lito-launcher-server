// DECOMPILED from base-0.6.71.jar, TODO review
// Decompiled with Vineflower 1.11.2; reviewed against formal bytecode.
package local.lito381.server;

import java.util.Objects;
import java.util.function.Consumer;

final class NativeDeInstanceScope {
   private static final ThreadLocal<NativeDeInstanceState> current = new ThreadLocal<>();

   static NativeDeInstanceState actor() {
      return Objects.requireNonNull(current.get(), "De initializer subject absent");
   }

   static void run(NativeDeInstanceState var0, Runnable var1) {
      NativeDeInstanceState var2 = current.get();
      current.set(Objects.requireNonNull(var0));

      try {
         NativeDePerceptionScope.callNested(var0, () -> {
            var1.run();
            return null;
         });
      } finally {
         if (var2 == null) {
            current.remove();
         } else {
            current.set(var2);
         }
      }
   }

   static boolean present() {
      return current.get() != null;
   }

   static NativeDeInitializerKernel.Log log() {
      return actor().construction.initialization().log();
   }

   static void send(NativeDeDeletionPorts.Viewer var0, NativeDePerceptionPacket var1) {
      var0.sendPackets(var1);
   }

   private NativeDeInstanceScope() {
   }

   static final class World {
      static NativeDeInstanceScope.World get() {
         return new NativeDeInstanceScope.World();
      }

      void forEachNearbyPlayer(NativeDeObjectFields var1, int var2, NativeDeWorldPlayerFilter var3, Consumer<NativeDeDeletionPorts.Viewer> var4) {
         NativeDeInstanceState var5 = NativeDeInstanceScope.actor();
         if (var1 == var5 && var5.deletionOwner.services instanceof NativeDeWorldContext var6) {
            var6.call(
               "forEachNearbyPlayer",
               "(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;ILlocal/lito381/server/NativeDeWorldPlayerFilter;Ljava/util/function/Consumer;)V",
               var1,
               var2,
               var3,
               var4
            );
         } else {
            throw new IllegalArgumentException("De broadcast requires the owning concrete World");
         }
      }
   }
}
