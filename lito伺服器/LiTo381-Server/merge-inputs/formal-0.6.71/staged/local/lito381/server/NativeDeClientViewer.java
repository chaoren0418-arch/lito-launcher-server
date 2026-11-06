// DECOMPILED from base-0.6.71.jar, TODO review
// Decompiled with Vineflower 1.11.2; reviewed against formal bytecode.
package local.lito381.server;

import java.util.Set;
import local.lito381.server.NativePlayerCommit.Binding;
import local.lito381.server.NativePlayerLifetime.Mode;

final class NativeDeClientViewer extends NativeDeKnownState {
   static final String FIELD = "sourceDeKnownViewer";
   final GameHub hub;
   final NativePlayerLifetime life;
   final Binding binding;
   volatile boolean retired;

   private NativeDeClientViewer(GameHub var1, Binding var2) {
      super(objectId(var2.object()));
      this.hub = var1;
      this.life = NativePlayerLifetime.of(var1);
      this.binding = var2;
   }

   private static int objectId(long var0) {
      if (var0 <= 0L || var0 > 4294967295L) {
         throw new IllegalArgumentException("source viewer object ID outside unsigned32");
      }
      return (int)var0;
   }

   static NativeDeClientViewer of(ClientSession var0) {
      GameHub var1 = (GameHub)CombatAccess.field(var0, "hub");
      BattleTimedStatus var2 = CombatPoisonAtomic.status(var0);
      synchronized (var2) {
         if (!NativePlayerLifetime.of(var1).admits()) {
            throw new IllegalStateException("viewer application closed");
         } else {
            Binding var4 = Binding.capture(var0);
            if (!Set.of("BATTLE", "WORLD").contains(var4.phase())) {
               throw new IllegalStateException("viewer is outside a native scene");
            } else {
               NativeDeClientViewer var5 = (NativeDeClientViewer)CombatAccess.field(var2, "sourceDeKnownViewer");
               if (var5 != null && !var5.retired && var5.binding.equals(var4) && var5.hub == var1) {
                  return var5;
               } else {
                  if (var5 != null) {
                     var5.retire();
                  }

                  NativeDeClientViewer var6 = new NativeDeClientViewer(var1, var4);
                  CombatAccess.set(var2, "sourceDeKnownViewer", var6);
                  return var6;
               }
            }
         }
      }
   }

   static void retire(BattleTimedStatus var0) {
      synchronized (var0) {
         NativeDeClientViewer var2 = (NativeDeClientViewer)CombatAccess.field(var0, "sourceDeKnownViewer");
         if (var2 != null) {
            CombatAccess.set(var0, "sourceDeKnownViewer", null);

            try {
               var2.retire();
            } catch (Throwable var5) {
               CombatNpcDamageQueue.failed(var5);
            }
         }
      }
   }

   static void retireClient(ClientSession var0) {
      if (var0 == null) { return; }
      {
         BattleTimedStatus var1 = CombatPoisonAtomic.status(var0);
         if (var1 != null) {
            retire(var1);
         }
      }
   }

   static void closeHub(GameHub var0) {
      for (ClientSession var2 : var0.connectedSessionsSnapshot()) {
         retireClient(var2);
      }
   }

   private void retire() {
      this.withKnownLock(() -> {
         this.retired = true;

         try {
            NativeDeWorldContext.detach(this);
         } finally {
            this.removeAllKnownObjects();
         }

         return null;
      });
   }

   private ClientSession current() {
      if (this.retired || this.life.mode == Mode.STOPPED) { return null; }
      for (ClientSession var2 : this.hub.connectedSessionsSnapshot()) {
         if (this.binding.matches(var2)) { return var2; }
      }
      return null;
   }

   @Override
   public boolean tryAddKnownObject(NativeDeDeletionPorts.ObjectRef var1) {
      ClientSession var2 = this.current();
      if (var2 == null) {
         return false;
      } else {
         if (var1 instanceof NativeDeDeletionState var3 && var3.deletionOwner.timer.life != this.life) { return false; }
         return this.withKnownLock(() -> !this.retired && this.life.admits() && this.binding.matchesFast(var2) && super.tryAddKnownObject(var1));
      }
   }

   @Override
   NativeDeDeletionPorts.OtherList get_otherList() {
      throw new UnsupportedOperationException("native illusory ownership registry is not connected");
   }

   @Override
   void sendPackets(NativeDeDeletionPorts.Packet var1) {
      if (NativeDeVisibilityOutput.accepts(var1)) {
         NativeDeVisibilityOutput.send(this, var1);
         return;
      } else if (var1 instanceof NativeDePerceptionPacket) {
         NativeDePerceptionOutput.send(this, (NativeDePerceptionPacket)var1);
         return;
      } else if (!(var1 instanceof NativeDeDeletionPorts.RemoveObject var2 && var2.actor instanceof NativeDeDeletionState var3)) {
         throw new IllegalArgumentException("viewer accepts source NPC remove packets only");
      } else if (var3.deletionOwner.timer.life != this.life) {
         throw new IllegalArgumentException("foreign application remove packet");
      } else {
         ClientSession var9 = this.current();
         if (var9 == null) {
            throw new IllegalStateException("stale native viewer");
         } else {
            byte[] var5 = Protocol.removeObject(Integer.toUnsignedLong(var3.getId()));
            synchronized (var9) {
               if (this.retired
                  || this.life.mode == Mode.STOPPED
                  || !this.binding.matchesFast(var9)
                  || CombatAccess.field(CombatPoisonAtomic.status(var9), "sourceDeKnownViewer") != this) {
                  throw new IllegalStateException("stale native viewer publication");
               }
               {
                  Object publicationTarget = var9;
                  String publicationMethod = "sendEncryptedFrame";
                  String publicationDescriptor = "([BLjava/lang/String;)V";
                  Object[] var10003 = new Object[]{var5, "S_REMOVE_OBJECT source-De deletion id=" + Integer.toUnsignedLong(var3.getId())};
                  NativeDeVisibilityRuntime.requirePublication(this);
                  CombatAccess.call(publicationTarget, publicationMethod, publicationDescriptor, var10003);
               }
            }
         }
      }
   }

   public void updateObject() {
      NativeDeVisibilityRuntime.update(this);
   }
}
