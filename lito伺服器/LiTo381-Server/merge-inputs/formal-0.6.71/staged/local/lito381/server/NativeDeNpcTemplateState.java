// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Objects;
import local.lito381.server.NativeDeNpcTemplatePorts.Services;
import local.lito381.server.NativeDeNpcTemplatePorts.WorkTimer;

class NativeDeNpcTemplateState extends NativeDePerceptionState {
   final Services initialization;
   public NativeDeNpcMobSkillUse _mobSkill;
   public NativeDeNpcExecutor TALK = null;
   public NativeDeNpcExecutor ACTION = null;
   public NativeDeNpcExecutor ATTACK = null;
   public NativeDeNpcExecutor DEATH = null;
   public NativeDeNpcExecutor WORK = null;
   public NativeDeNpcExecutor SPAWN = null;
   private int _passispeed;
   private int _atkspeed;
   private boolean _pickupItem;
   private boolean _Agro;
   private boolean _Agrocoi;
   private boolean _Agrososc;
   private int _lightSize;
   private int _tempLawful = 0;

   NativeDeNpcTemplateState(
      int var1,
      NativeDeResurrectionEffects var2,
      NativeDeDeletionOwner var3,
      local.lito381.server.NativeDePerceptionPorts.Services var4,
      Services var5,
      NativeDeNpcTemplate var6
   ) {
      super(var1, var2, var3, var4);
      this.initialization = Objects.requireNonNull(var5);
      if (var6 != null) {
         this.setting_template(var6);
      }
   }

   public void setting_template(NativeDeNpcTemplate var1) {
      NativeDeNpcTemplateScope.run(this, () -> this.sourceSettingTemplate(var1));
   }

   private void sourceSettingTemplate(NativeDeNpcTemplate var1) {
      super.template = var1;
      int var2 = 0;
      double var3 = 0.0;
      double var5 = 0.0;
      this.setName(var1.get_name());
      String var7 = this.newName(var1.get_npcId()) + var1.get_nameid();
      this.setNameId(var7);
      if (var1.get_randomlevel() == 0) {
         this.setLevel(var1.get_level());
      } else {
         var2 = NativeDeNpcTemplateScope.random().nextInt(var1.get_randomlevel() - var1.get_level() + 1);
         var5 = var1.get_randomlevel() - var1.get_level();
         var3 = var2 / var5;
         var2 += var1.get_level();
         this.setLevel(var2);
      }

      if (var1.get_randomhp() == 0) {
         this.setMaxHp(var1.get_hp());
         this.setCurrentHpDirect(var1.get_hp());
      } else {
         double var8 = var3 * (var1.get_randomhp() - var1.get_hp());
         int var10 = (int)(var1.get_hp() + var8);
         this.setMaxHp(var10);
         this.setCurrentHpDirect(var10);
      }

      if (var1.get_randommp() == 0) {
         this.setMaxMp(var1.get_mp());
         this.setCurrentMpDirect(var1.get_mp());
      } else {
         double var14 = var3 * (var1.get_randommp() - var1.get_mp());
         int var18 = (int)(var1.get_mp() + var14);
         this.setMaxMp(var18);
         this.setCurrentMpDirect(var18);
      }

      if (var1.get_randomac() == 0) {
         this.setAc(var1.get_ac());
      } else {
         double var15 = var3 * (var1.get_randomac() - var1.get_ac());
         int var19 = (int)(var1.get_ac() + var15);
         this.setAc(var19);
      }

      if (var1.get_randomlevel() == 0) {
         this.setStr(var1.get_str());
         this.setCon(var1.get_con());
         this.setDex(var1.get_dex());
         this.setInt(var1.get_int());
         this.setWis(var1.get_wis());
         this.setMr(var1.get_mr());
      } else {
         this.setStr((byte)Math.min(var1.get_str() + var5, 127.0));
         this.setCon((byte)Math.min(var1.get_con() + var5, 127.0));
         this.setDex((byte)Math.min(var1.get_dex() + var5, 127.0));
         this.setInt((byte)Math.min(var1.get_int() + var5, 127.0));
         this.setWis((byte)Math.min(var1.get_wis() + var5, 127.0));
         this.setMr((byte)Math.min(var1.get_mr() + var5, 127.0));
         this.addHitup((int)var5 * 2);
         this.addDmgup((int)var5 * 2);
      }

      this.setPassispeed(var1.get_passispeed());
      this.setAtkspeed(var1.get_atkspeed());
      this.setAgro(var1.is_agro());
      this.setAgrocoi(var1.is_agrocoi());
      this.setAgrososc(var1.is_agrososc());
      if (this.getNpcTemplate().get_weakAttr() != 0) {
         switch (this.getNpcTemplate().get_weakAttr()) {
            case 1: this.addEarth(-50); break;
            case 2: this.addFire(-50); break;
            case 4: this.addWater(-50); break;
            case 8: this.addWind(-50); break;
            case -1: this.addEarth(50); break;
            case -2: this.addFire(50); break;
            case -4: this.addWater(50); break;
            case -8: this.addWind(50); break;
         }
      }

      int var16 = this.newGfx(var1.get_gfxid());
      this.setTempCharGfx(var16);
      this.setGfxId(var16);
      this.setGfxidInStatus(var16);
      if (var1.get_randomexp() == 0) {
         this.setExp(var1.get_exp());
      } else {
         int var9 = var1.get_exp() + NativeDeNpcTemplateScope.random().nextInt(var1.get_randomexp());
         this.setExp(var9);
      }

      if (var1.get_randomlawful() == 0) {
         this.setLawful(var1.get_lawful());
         this.setTempLawful(var1.get_lawful());
      } else {
         double var17 = var3 * (var1.get_randomlawful() - var1.get_lawful());
         int var11 = (int)(var1.get_lawful() + var17);
         this.setLawful(var11);
         this.setTempLawful(var11);
      }

      this.setPickupItem(var1.is_picupitem());
      if (var1.is_bravespeed()) {
         this.setBraveSpeed(1);
      } else {
         this.setBraveSpeed(0);
      }

      this.setKarma(var1.getKarma());
      this.setLightSize(var1.getLightSize());
      if (var1.talk()) {
         this.TALK = var1.getNpcExecutor();
      }

      if (var1.action()) {
         this.ACTION = var1.getNpcExecutor();
      }

      if (var1.attack()) {
         this.ATTACK = var1.getNpcExecutor();
      }

      if (var1.death()) {
         this.DEATH = var1.getNpcExecutor();
      }

      if (var1.work()) {
         this.WORK = var1.getNpcExecutor();
         if (this.WORK.workTime() != 0) {
            WorkTimer.put(this, this.WORK.workTime());
         } else {
            this.WORK.work(this);
         }
      }

      if (var1.spawn()) {
         this.SPAWN = var1.getNpcExecutor();
         this.SPAWN.spawn(this);
      }

      this._mobSkill = new NativeDeNpcMobSkillUse(this);
      this.setWeaponBreakStack(0);
   }

   public void setGfxidInStatus(int var1) {
      switch (var1) {
         case 51:
         case 110:
         case 147:
            this.setStatus(24);
            break;
         case 57:
         case 816:
         case 3137:
         case 3140:
         case 3145:
         case 3148:
         case 3151:
         case 7621:
            this.setStatus(20);
            break;
         case 111:
            this.setStatus(4);
      }
   }

   private String newName(int var1) {
      return "";
   }

   private int newGfx(int var1) {
      int[] var2 = null;
      switch (var1) {
         case 998:
         case 999:
         case 1002:
         case 1003:
            var2 = new int[]{998, 999, 1002, 1003};
            break;
         case 1318:
         case 1321:
            var2 = new int[]{1318, 1321};
            break;
         case 1597:
         case 1600:
            var2 = new int[]{1597, 1600};
            break;
         case 5942:
            var2 = new int[]{5942, 5135, 5137, 5139, 5141, 5143, 5145, 5156, 5158, 5160, 5162};
      }

      if (var2 != null) {
         int var3 = NativeDeNpcTemplateScope.random().nextInt(var2.length);
         var1 = var2[var3];
      }

      return var1;
   }

   public int getPassispeed() {
      return this._passispeed;
   }

   public void setPassispeed(int var1) {
      this._passispeed = var1;
   }

   public int getAtkspeed() {
      return this._atkspeed;
   }

   public void setAtkspeed(int var1) {
      this._atkspeed = var1;
   }

   public boolean isPickupItem() {
      return this._pickupItem;
   }

   public void setPickupItem(boolean var1) {
      this._pickupItem = var1;
   }

   public NativeDeNpcTemplate getNpcTemplate() {
      return (NativeDeNpcTemplate)super.template;
   }

   @Override
   public String getNameId() {
      return super.nameId;
   }

   public void setNameId(String var1) {
      super.nameId = var1;
   }

   public boolean isAgro() {
      return this._Agro;
   }

   public void setAgro(boolean var1) {
      this._Agro = var1;
   }

   public boolean isAgrocoi() {
      return this._Agrocoi;
   }

   public void setAgrocoi(boolean var1) {
      this._Agrocoi = var1;
   }

   public boolean isAgrososc() {
      return this._Agrososc;
   }

   public void setAgrososc(boolean var1) {
      this._Agrososc = var1;
   }

   public int getLightSize() {
      return this._lightSize;
   }

   public void setLightSize(int var1) {
      this._lightSize = var1;
   }

   public int getTempLawful() {
      return this._tempLawful;
   }

   public void setTempLawful(int var1) {
      this._tempLawful = var1;
   }
}
