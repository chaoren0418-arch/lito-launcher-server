// DECOMPILED from base-0.6.71.jar, TODO review
// Decompiled with Vineflower 1.11.2; reviewed against formal bytecode.
package local.lito381.server;

import java.util.ArrayList;

class NativeDeDeletionDeState extends NativeDeDeletionState {
   NativeDeDeletionDeState(int var1, NativeDeResurrectionEffects var2, NativeDeDeletionOwner var3) {
      super(var1, var2, var3);
   }

   @Override
   public void deleteMe() {
      NativeDeDeletionScope.run(this.deletionOwner, this::sourceDeleteDe);
   }

   void stopAI() {
      this.deletionOwner.services.stopAI(this);
   }

   void cancelDelayedMove() {
      this.deletionOwner.services.cancelDelayedMove(this);
   }

   NativeDeInitializerKernel.Name get_deName() {
      return this.deName;
   }

   static ArrayList<NativeDeInitializerKernel.Name> getDeNameList() {
      return NativeDeDeletionScope.owner().services.names();
   }

   public void sourceDeleteDe() {
      NativeDeDeletionScope.require(this);

      try {
         this.stopAI();
         this.cancelDelayedMove();

         try {
            if (!this.getPetList().isEmpty()) {
               Object[] pets = this.getPetList().values().toArray();

               for (Object obj : pets) {
                  try {
                     if (obj instanceof NativeDeDeletionPorts.Summon pet && pet != null) {
                        pet.setreSpawn(false);
                        pet.allTargetClear();
                        pet.deleteMe();
                     }
                  } catch (Exception var20) {
                     NativeDeDeletionScope._log().error("單一寵物清理失敗", var20);
                  }
               }
            }
         } catch (Exception var21) {
            NativeDeDeletionScope._log().error("假人 deleteMe 清理寵物異常: " + this.getName(), var21);
         }

         String nameId = this.getNameId();
         if (nameId != null && !nameId.isEmpty()) {
            NativeDeDeletionPorts.DeWorld.get().remove(nameId);
         }

         if (this.get_deName() != null) {
            ArrayList<NativeDeInitializerKernel.Name> denameList = getDeNameList();
            if (denameList != null) {
               NativeDeDeletionScope._denameListLock().lock();

               try {
                  denameList.remove(this.get_deName());
               } finally {
                  NativeDeDeletionScope._denameListLock().unlock();
               }
            }
         }
      } catch (Exception var22) {
         NativeDeDeletionScope._log().error("假人 deleteMe 執行失敗: " + this.getNameId(), var22);
      } finally {
         if (this.getInventory() != null) {
            this.getInventory().clearItems();
         }

         super.sourceDeleteNpc();
      }
   }
}
