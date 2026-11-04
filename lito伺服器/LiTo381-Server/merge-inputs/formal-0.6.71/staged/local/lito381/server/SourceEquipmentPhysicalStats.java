// Extracted from Accounts.java SourceFile; aligned to base-0.6.71.jar
package local.lito381.server;

record SourceEquipmentPhysicalStats(
   SourceWeaponPhysicalStats weapon,
   int itemMeleeHit,
   int itemMeleeDamage,
   int itemRangedHit,
   int itemRangedDamage,
   int armorMeleeHit,
   int armorMeleeDamage,
   int armorRangedHit,
   int armorRangedDamage,
   int setMeleeHit,
   int setMeleeDamage,
   int setRangedHit,
   int setRangedDamage,
   int itemDexterity,
   int setDexterity,
   int itemPierce,
   int setPierce,
   int itemIgnoreReduction,
   int setIgnoreReduction,
   int itemMeleeReduction,
   int itemRangedReduction,
   int itemArmorClassModifier,
   int setArmorClassValue,
   int itemEvasionRate,
   int setEvasionRate,
   SourceEquipmentElementalStats elemental,
   boolean shieldEquipped,
   boolean guarderEquipped,
   SourceDefensiveEquipment defensiveEquipment,
   int pvpReduction
) {
   SourceEquipmentPhysicalStats(
      SourceWeaponPhysicalStats var1,
      int var2,
      int var3,
      int var4,
      int var5,
      int var6,
      int var7,
      int var8,
      int var9,
      int var10,
      int var11,
      int var12,
      int var13,
      int var14,
      int var15,
      int var16,
      int var17,
      int var18,
      int var19,
      int var20,
      int var21,
      int var22,
      int var23,
      int var24,
      int var25,
      SourceEquipmentElementalStats var26,
      boolean var27,
      boolean var28,
      SourceDefensiveEquipment var29
   ) {
      this(
         var1,
         var2,
         var3,
         var4,
         var5,
         var6,
         var7,
         var8,
         var9,
         var10,
         var11,
         var12,
         var13,
         var14,
         var15,
         var16,
         var17,
         var18,
         var19,
         var20,
         var21,
         var22,
         var23,
         var24,
         var25,
         var26,
         var27,
         var28,
         var29,
         0
      );
   }

   SourceEquipmentPhysicalStats(
      SourceWeaponPhysicalStats var1,
      int var2,
      int var3,
      int var4,
      int var5,
      int var6,
      int var7,
      int var8,
      int var9,
      int var10,
      int var11,
      int var12,
      int var13,
      int var14,
      int var15,
      int var16,
      int var17,
      int var18,
      int var19,
      int var20,
      int var21,
      int var22,
      int var23,
      int var24,
      int var25,
      SourceEquipmentElementalStats var26
   ) {
      this(
         var1,
         var2,
         var3,
         var4,
         var5,
         var6,
         var7,
         var8,
         var9,
         var10,
         var11,
         var12,
         var13,
         var14,
         var15,
         var16,
         var17,
         var18,
         var19,
         var20,
         var21,
         var22,
         var23,
         var24,
         var25,
         var26,
         false,
         false,
         SourceDefensiveEquipment.none()
      );
   }

   SourceEquipmentPhysicalStats(SourceWeaponPhysicalStats var1, boolean var2, boolean var3) {
      this(
         var1,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         SourceEquipmentElementalStats.none(),
         var2,
         var3,
         var2 ? SourceDefensiveEquipment.memoryShield() : (var3 ? SourceDefensiveEquipment.memoryGuarder() : SourceDefensiveEquipment.none())
      );
   }

   SourceEquipmentPhysicalStats(
      SourceWeaponPhysicalStats var1,
      int var2,
      int var3,
      int var4,
      int var5,
      int var6,
      int var7,
      int var8,
      int var9,
      int var10,
      int var11,
      int var12,
      int var13
   ) {
      this(
         var1,
         var2,
         var3,
         var4,
         var5,
         var6,
         var7,
         var8,
         var9,
         var10,
         var11,
         var12,
         var13,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         SourceEquipmentElementalStats.none()
      );
   }

   SourceEquipmentPhysicalStats(
      SourceWeaponPhysicalStats var1,
      int var2,
      int var3,
      int var4,
      int var5,
      int var6,
      int var7,
      int var8,
      int var9,
      int var10,
      int var11,
      int var12,
      int var13,
      int var14,
      int var15,
      int var16,
      int var17
   ) {
      this(
         var1,
         var2,
         var3,
         var4,
         var5,
         var6,
         var7,
         var8,
         var9,
         var10,
         var11,
         var12,
         var13,
         var14,
         var15,
         var16,
         var17,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         SourceEquipmentElementalStats.none()
      );
   }

   SourceEquipmentPhysicalStats(
      SourceWeaponPhysicalStats var1,
      int var2,
      int var3,
      int var4,
      int var5,
      int var6,
      int var7,
      int var8,
      int var9,
      int var10,
      int var11,
      int var12,
      int var13,
      int var14,
      int var15,
      int var16,
      int var17,
      int var18,
      int var19,
      int var20,
      int var21
   ) {
      this(
         var1,
         var2,
         var3,
         var4,
         var5,
         var6,
         var7,
         var8,
         var9,
         var10,
         var11,
         var12,
         var13,
         var14,
         var15,
         var16,
         var17,
         var18,
         var19,
         var20,
         var21,
         0,
         0,
         0,
         0,
         SourceEquipmentElementalStats.none()
      );
   }

   SourceEquipmentPhysicalStats(
      SourceWeaponPhysicalStats var1,
      int var2,
      int var3,
      int var4,
      int var5,
      int var6,
      int var7,
      int var8,
      int var9,
      int var10,
      int var11,
      int var12,
      int var13,
      int var14,
      int var15,
      int var16,
      int var17,
      int var18,
      int var19,
      int var20,
      int var21,
      int var22,
      int var23
   ) {
      this(
         var1,
         var2,
         var3,
         var4,
         var5,
         var6,
         var7,
         var8,
         var9,
         var10,
         var11,
         var12,
         var13,
         var14,
         var15,
         var16,
         var17,
         var18,
         var19,
         var20,
         var21,
         var22,
         var23,
         0,
         0,
         SourceEquipmentElementalStats.none()
      );
   }

   SourceEquipmentPhysicalStats(
      SourceWeaponPhysicalStats var1,
      int var2,
      int var3,
      int var4,
      int var5,
      int var6,
      int var7,
      int var8,
      int var9,
      int var10,
      int var11,
      int var12,
      int var13,
      int var14,
      int var15,
      int var16,
      int var17,
      int var18,
      int var19,
      int var20,
      int var21,
      int var22,
      int var23,
      SourceEquipmentElementalStats var24
   ) {
      this(
         var1,
         var2,
         var3,
         var4,
         var5,
         var6,
         var7,
         var8,
         var9,
         var10,
         var11,
         var12,
         var13,
         var14,
         var15,
         var16,
         var17,
         var18,
         var19,
         var20,
         var21,
         var22,
         var23,
         0,
         0,
         var24
      );
   }

   SourceEquipmentPhysicalStats(
      SourceWeaponPhysicalStats weapon,
      int itemMeleeHit,
      int itemMeleeDamage,
      int itemRangedHit,
      int itemRangedDamage,
      int armorMeleeHit,
      int armorMeleeDamage,
      int armorRangedHit,
      int armorRangedDamage,
      int setMeleeHit,
      int setMeleeDamage,
      int setRangedHit,
      int setRangedDamage,
      int itemDexterity,
      int setDexterity,
      int itemPierce,
      int setPierce,
      int itemIgnoreReduction,
      int setIgnoreReduction,
      int itemMeleeReduction,
      int itemRangedReduction,
      int itemArmorClassModifier,
      int setArmorClassValue,
      int itemEvasionRate,
      int setEvasionRate,
      SourceEquipmentElementalStats elemental,
      boolean shieldEquipped,
      boolean guarderEquipped,
      SourceDefensiveEquipment defensiveEquipment,
      int pvpReduction
   ) {
      if (weapon == null) {
         throw new IllegalArgumentException("source physical snapshot has no weapon state");
      } else if (elemental == null) {
         throw new IllegalArgumentException("source physical snapshot has no elemental state");
      } else if (defensiveEquipment == null
         || shieldEquipped && guarderEquipped
         || shieldEquipped != (defensiveEquipment.present() && defensiveEquipment.armorType() == 7)
         || guarderEquipped != (defensiveEquipment.present() && defensiveEquipment.armorType() == 13)) {
         throw new IllegalArgumentException("source defensive equipment identity does not match slot state");
      } else if (pvpReduction < 0) {
         throw new IllegalArgumentException("negative source PvP reduction");
      } else {
         this.weapon = weapon;
         this.itemMeleeHit = itemMeleeHit;
         this.itemMeleeDamage = itemMeleeDamage;
         this.itemRangedHit = itemRangedHit;
         this.itemRangedDamage = itemRangedDamage;
         this.armorMeleeHit = armorMeleeHit;
         this.armorMeleeDamage = armorMeleeDamage;
         this.armorRangedHit = armorRangedHit;
         this.armorRangedDamage = armorRangedDamage;
         this.setMeleeHit = setMeleeHit;
         this.setMeleeDamage = setMeleeDamage;
         this.setRangedHit = setRangedHit;
         this.setRangedDamage = setRangedDamage;
         this.itemDexterity = itemDexterity;
         this.setDexterity = setDexterity;
         this.itemPierce = itemPierce;
         this.setPierce = setPierce;
         this.itemIgnoreReduction = itemIgnoreReduction;
         this.setIgnoreReduction = setIgnoreReduction;
         this.itemMeleeReduction = itemMeleeReduction;
         this.itemRangedReduction = itemRangedReduction;
         this.itemArmorClassModifier = itemArmorClassModifier;
         this.setArmorClassValue = setArmorClassValue;
         this.itemEvasionRate = itemEvasionRate;
         this.setEvasionRate = setEvasionRate;
         this.elemental = elemental;
         this.shieldEquipped = shieldEquipped;
         this.guarderEquipped = guarderEquipped;
         this.defensiveEquipment = defensiveEquipment;
         this.pvpReduction = pvpReduction;
      }
   }

   static SourceEquipmentPhysicalStats none() {
      return new SourceEquipmentPhysicalStats(SourceWeaponPhysicalStats.none(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
   }

   int totalHit(boolean var1) {
      return this.totalHit(var1, true);
   }

   int totalHit(boolean var1, boolean var2) {
      int var3 = var1 ? this.itemRangedHit + this.armorRangedHit + this.setRangedHit : this.itemMeleeHit + this.armorMeleeHit + this.setMeleeHit;
      if (var2 && this.weapon.present()) {
         var3 = Math.addExact(var3, var1 ? this.weapon.abilityRangedHit() : this.weapon.abilityMeleeHit());
         return Math.addExact(var3, Math.addExact(this.weapon.hitModifier(), (int)(this.weapon.enchantLevel() * 0.6)));
      } else {
         return var3;
      }
   }

   int totalDamageBonus(boolean var1) {
      return this.totalDamageBonus(var1, true);
   }

   int totalDamageBonus(boolean var1, boolean var2) {
      int var3 = var1
         ? this.itemRangedDamage + this.armorRangedDamage + this.setRangedDamage
         : this.itemMeleeDamage + this.armorMeleeDamage + this.setMeleeDamage;
      if (!var2) {
         return var3;
      }
      if (!this.weapon.present()) {
         return var3;
      }
      var3 = Math.addExact(var3, var1 ? this.weapon.abilityRangedDamage() : this.weapon.abilityMeleeDamage());
      return Math.addExact(var3, Math.addExact(this.weapon.damageModifier(), this.weapon.effectiveEnchant(var1)));
   }

   int effectiveDexterity(int var1) {
      return NumericBreakthrough.clampAbility((long)var1 + this.itemDexterity + this.setDexterity);
   }

   int totalPierce() {
      return Math.addExact(this.itemPierce, this.setPierce);
   }

   int totalIgnoreReduction() {
      return Math.addExact(this.itemIgnoreReduction, this.setIgnoreReduction);
   }

   int directionalReduction(boolean var1) {
      return var1 ? this.itemRangedReduction : this.itemMeleeReduction;
   }

   int totalPvpReduction() {
      return this.pvpReduction;
   }

   int totalEvasionRate() {
      return Math.addExact(this.itemEvasionRate, this.setEvasionRate);
   }

   boolean supportsSolidCarriage() {
      return SolidCarriageRules.equipment(this.shieldEquipped, this.guarderEquipped);
   }

   int armorClassModifier() {
      return NumericBreakthrough.clampExtendedValue((long)this.itemArmorClassModifier - this.setArmorClassValue);
   }

   SourceEquipmentPhysicalStats withRentalEquipment(RentalEquipmentBonuses var1) {
      if (var1 != null && var1.matchedItems() != 0) {
         SourceEquipmentElementalStats var2 = new SourceEquipmentElementalStats(
            Math.addExact(this.elemental.itemEarth(), var1.earthResistance()),
            Math.addExact(this.elemental.itemFire(), var1.fireResistance()),
            Math.addExact(this.elemental.itemWater(), var1.waterResistance()),
            Math.addExact(this.elemental.itemWind(), var1.windResistance()),
            this.elemental.setEarth(),
            this.elemental.setFire(),
            this.elemental.setWater(),
            this.elemental.setWind()
         );
         return new SourceEquipmentPhysicalStats(
            this.weapon,
            this.itemMeleeHit,
            this.itemMeleeDamage,
            this.itemRangedHit,
            this.itemRangedDamage,
            Math.addExact(this.armorMeleeHit, var1.meleeHit()),
            Math.addExact(this.armorMeleeDamage, var1.meleeDamage()),
            Math.addExact(this.armorRangedHit, var1.rangedHit()),
            Math.addExact(this.armorRangedDamage, var1.rangedDamage()),
            this.setMeleeHit,
            this.setMeleeDamage,
            this.setRangedHit,
            this.setRangedDamage,
            Math.addExact(this.itemDexterity, var1.dexterity()),
            this.setDexterity,
            this.itemPierce,
            this.setPierce,
            this.itemIgnoreReduction,
            this.setIgnoreReduction,
            Math.addExact(this.itemMeleeReduction, var1.meleeReduction()),
            Math.addExact(this.itemRangedReduction, var1.rangedReduction()),
            this.itemArmorClassModifier,
            this.setArmorClassValue,
            this.itemEvasionRate,
            this.setEvasionRate,
            var2,
            this.shieldEquipped,
            this.guarderEquipped,
            this.defensiveEquipment,
            this.pvpReduction
         );
      } else {
         return this;
      }
   }
}
