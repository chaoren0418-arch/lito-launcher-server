// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import local.lito381.server.NativeDeDeletionPorts.Viewer;
import local.lito381.server.NativeDePerceptionPorts.Emblems;
import local.lito381.server.NativeDePerceptionPorts.Fishing;
import local.lito381.server.NativeDePerceptionPorts.Services;
import local.lito381.server.NativeDePerceptionPorts.Shop;

class NativeDePerceptionState extends NativeDeDeletionDeState {
   final Services perception;
   Shop _shopBehavior;
   Fishing _fishingBehavior;
   int _aiType = 0;
   int _clanid = 0;
   String _clan_name = null;
   volatile boolean _isBackgroundMode = false;
   final AtomicLong _aiOwnerToken = new AtomicLong(0L);
   private boolean _isShop = false;
   private volatile Set<Integer> perceptionSkillIds = Set.of();

   NativeDePerceptionState(int var1, NativeDeResurrectionEffects var2, NativeDeDeletionOwner var3, Services var4) {
      super(var1, var2, var3);
      this.perception = Objects.requireNonNull(var4);
   }

   NativeDeWorldContext context() {
      if (this.deletionOwner.services instanceof NativeDeWorldContext var1) {
         return var1;
      } else {
         throw new IllegalStateException("perception requires concrete application World");
      }
   }

   @Override
   public void onPerceive(Viewer var1) {
      NativeDePerceptionScope.run(this, () -> this.sourceOnPerceive(var1));
   }

   void startAI() {
      this.perception.startAI(this);
   }

   void wakeup() {
      this.perception.wakeup(this);
   }

   @Override
   public boolean hasSkillEffect(int var1) {
      return this.perceptionSkillIds.contains(var1);
   }

   void skillSnapshot(Set<Integer> var1) {
      this.perceptionSkillIds = Set.copyOf(var1);
   }

   private void sourceOnPerceive(Viewer var1) {
      try {
         if (this._shopBehavior != null && !this._shopBehavior.isShop()) {
            this.start_shop();
         }

         if (!NativeDePerceptionAccess.Pc_tryAddKnownObject(var1, this)) {
            return;
         }

         var1.sendPackets(new NativeDePerceptionNPCPack_De(this));
         if (this.getClanid() > 0) {
            NativeDePerceptionEmblemData var2 = Emblems.get().get(this.getClanid());
            if (var2 != null) {
               var1.sendPackets(new NativeDePerceptionEmblem(var2));
            }
         }

         if (this.isShop()) {
            if (this._shopBehavior != null) {
               String var4 = this._shopBehavior.getShopChat();
               var1.sendPackets(new NativeDePerceptionDoActionShop(this.getId(), var4 != null ? var4 : ""));
               var1.sendPackets(new NativeDePerceptionCharVisualUpdate(this.getId(), 70));
            }

            return;
         }

         if (this.isFishing()) {
            var1.sendPackets(new NativeDePerceptionFishing(this.getId(), 71, this.get_fishX(), this.get_fishY()));
            return;
         }

         if ((this._aiType == 3 || this._aiType == 4 || this._aiType == 5 || this._aiType == 6) && (!this.isAiRunning() || this._isBackgroundMode)) {
            this.startAI();
            this.wakeup();
         }

         this.sendSpeedStatusTo(var1);
      } catch (Exception var3) {
         NativeDePerceptionScope._log().error(var3.getLocalizedMessage(), var3);
      }
   }

   public boolean isFishing() {
      return this._fishingBehavior != null && this._fishingBehavior.isFishing();
   }

   public int get_fishX() {
      return this._fishingBehavior != null ? this._fishingBehavior.getFishX() : -1;
   }

   public int get_fishY() {
      return this._fishingBehavior != null ? this._fishingBehavior.getFishY() : -1;
   }

   public int getClanid() {
      return this._clanid;
   }

   public String getClanname() {
      return this._clan_name;
   }

   public void start_shop() {
      if (this._shopBehavior != null) {
         this._shopBehavior.start();
      }
   }

   protected final void sendSpeedStatusTo(Viewer var1) {
      if (var1 != null) {
         if (this.getMoveSpeed() != 0) {
            var1.sendPackets(new NativeDePerceptionSkillHaste(this.getId(), this.getMoveSpeed(), 0));
         }

         NativeDePerceptionSpeed.SpeedState var2 = NativeDePerceptionSpeed.stateOf(this);
         byte var3;
         if (var2.fastAttackable()) {
            var3 = 6;
         } else if (var2.fastMovable()) {
            var3 = 4;
         } else if (var2.elfBrave()) {
            var3 = 3;
         } else if (var2.brave()) {
            var3 = 1;
         } else {
            var3 = 0;
         }

         if (var3 != 0) {
            var1.sendPackets(new NativeDePerceptionSkillBrave(this.getId(), var3, 0));
         }

         if (var2.braveX()) {
            var1.sendPackets(new NativeDePerceptionLiquor(this.getId(), 8));
         }
      }
   }

   public boolean isAiRunning() {
      return this._aiOwnerToken.get() != 0L;
   }

   public boolean isShop() {
      return this._isShop;
   }
}
