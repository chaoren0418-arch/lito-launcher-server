// DECOMPILED from base-0.6.71.jar, TODO review
// Decompiled with Vineflower 1.11.2; reviewed against formal bytecode.
package local.lito381.server;

import java.util.Objects;
import java.util.function.IntFunction;
import local.lito381.server.NativeDeInstancePorts.Scene;

final class NativeDeInstanceScene implements Scene {
   final NativeDeWorldContext context;
   final IntFunction<NativeDePerceptionEmblemData> emblems;

   NativeDeInstanceScene(NativeDeWorldContext var1, IntFunction<NativeDePerceptionEmblemData> var2) {
      this.context = Objects.requireNonNull(var1);
      this.emblems = Objects.requireNonNull(var2);
   }

   static NativeDeInitializerKernel.Packet visual(int var0, int var1) {
      NativeDeInstanceState var2 = NativeDeInstanceScope.actor();
      if (var2.getId() != var0) {
         throw new IllegalArgumentException("initializer visual source identity mismatch");
      } else {
         return new NativeDeInstanceScene.Visual(new NativeDePerceptionCharVisualUpdate(var0, var1));
      }
   }

   @Override
   public NativeDePerceptionEmblemData emblem(NativeDeInitializerKernel.Emblem var1) {
      return var1 == null ? null : this.emblems.apply(var1.id());
   }

   @Override
   public void publish(NativeDeInstanceState var1, NativeDeInitializerKernel.Packet var2) {
      if (var1.deletionOwner.services == this.context
         && NativeDeInstanceScope.actor() == var1
         && var2 instanceof NativeDeInstanceScene.Visual var3
         && var3.packet().subject == var1) {
         NativeDeInstanceBroadcast.toVisiblePlayers(var1, -1, var3.packet());
      } else {
         throw new IllegalArgumentException("foreign initializer visual binding");
      }
   }

   record Visual(NativeDePerceptionCharVisualUpdate packet) implements NativeDeInitializerKernel.Packet {
   }
}
