package local.lito381.server;

import static local.lito381.server.ModernBattleWire.*;

/** Only the authenticated selected owner's data. Source names/databases,
 * evaluator internals and other owners' inventories are not in the schema. */
final class ModernBattleWirePersonal {
    // LTB1 retains raw source facts. The independent v42 menu qualification
    // must not silently add a new Support ordinal to any battle-data schema.
    static ModernSkills.Support sourceSupport(ModernSkills.Skill skill) {
        return skill.skillId()==54||skill.skillId()==43||skill.skillId()==29 ? ModernSkills.Support.UNIMPLEMENTED : skill.support();
    }
    static void put(Writer w,ModernBattleAudience.Personal v) {
        put(w,v,SCHEMA);
    }
    static void put(Writer w,ModernBattleAudience.Personal v,int schema) {
        if(schema!=ModernBattleHealingWire.SCHEMA&&schema!=ModernBattleArmorWire.SCHEMA&&(schema==ModernBattleSpeedWire.SCHEMA)!=(v.materials()!=null))throw bad();
        player(w,v.scope());w.l(v.entityId());w.optional(v.equipment(),ModernBattleWirePersonal::equipment);
        tail(w,schema,v.skills(),v.history(),v.spellBirthRevision(),v.supplies(),v.weight(),v.ammo(),v.regenClock(),v.materials());
    }
    /** The common revision field is the runtime version, never the saved SET
     * version. Schema10 carries all three explicitly in its owner section. */
    static void putVip(Writer w,ModernBattleVipObservation.Personal v) {
        player(w,v.scope());w.l(v.entityId());w.flag(true);var e=v.equipment();
        equipment(w,e.source(),e.versions().runtimeAbilityRevision(),e.effective(),e.equipped(),e.lifetimes(),e.evaluatedAtEpochMillis(),e.validUntilEpochMillis());
        tail(w,ModernBattleArmorWire.SCHEMA,v.skills(),v.history(),v.spellBirthRevision(),v.supplies(),v.weight(),v.ammo(),v.regenClock(),v.materials());
    }
    private static void tail(Writer w,int schema,java.util.List<ModernSkills.Skill> skills,ModernEquipmentHistory.State history,
            long spellBirthRevision,ModernBattleObservation.Supplies supplies,ModernInventoryWeight.Snapshot weight,
            ModernAmmo.State ammo,ModernNaturalRegen.Clock regenClock,ModernSpellMaterials.State materials) {
        w.list(skills,220,(b,s)->{b.i(s.skillId());b.text(s.nameToken(),256);b.l(s.mpCost());b.l(s.reuseMillis());b.e(sourceSupport(s));});
        w.optional(history,(b,h)->{
            int historySchema;
            if(schema==ModernBattleArmorWire.SCHEMA){b.flag(h.healing()!=null);historySchema=h.healing()!=null?ModernEquipmentHistoryWire.PLAYER_HEALING_SCHEMA:ModernEquipmentHistoryWire.SLOW_SCHEMA;}
            else historySchema=schema==ModernBattleHealingWire.SCHEMA?ModernEquipmentHistoryWire.PLAYER_HEALING_SCHEMA:schema==ModernBattleSpeedWire.SCHEMA?ModernEquipmentHistoryWire.SLOW_SCHEMA:schema==ModernBattleHasteWire.SCHEMA?ModernEquipmentHistoryWire.HASTE_SCHEMA:schema==GREATER_HASTE_SCHEMA?ModernEquipmentHistoryWire.GREATER_HASTE_SCHEMA:ModernEquipmentHistoryWire.SCHEMA;
            var bytes=ModernEquipmentHistoryWire.encode(h,historySchema);b.count(bytes.length,ModernEquipmentHistoryWire.MAX_BYTES);b.bytes(bytes);
        });w.l(spellBirthRevision);
        w.optional(supplies,(b,s)->{b.l(s.requestSequence());b.l(s.requestRevision());items(b,s.history());b.list(s.stocks(),7,(c,t)->{c.l(t.itemId());c.l(t.initial());c.l(t.remaining());});b.i(s.effect());b.i(s.delayMillis());});
        w.optional(weight,(b,s)->{var x=s.view();b.l(x.sourceGearWeight());b.l(x.suppliesWeight());b.l(x.totalWeight());b.l(Double.doubleToLongBits(x.maximumWeight()));b.i(x.stage());
            b.list(s.units(),ModernInventoryWeight.MAX_UNITS,(c,u)->{c.l(u.itemId());c.l(u.unitWeight());});});
        w.optional(ammo,ModernBattleWirePersonal::ammo);
        w.optional(regenClock,(b,c)->{b.l(c.nextSecondElapsedNanos());b.i(c.hpCounter());b.i(c.mpCounter());b.e(c.hpActivity());b.e(c.mpActivity());});
        if(schema==ModernBattleSpeedWire.SCHEMA)ModernBattleSpeedWire.materials(w,materials);
        if(schema==ModernBattleHealingWire.SCHEMA||schema==ModernBattleArmorWire.SCHEMA)w.optional(materials,ModernBattleSpeedWire::materials);
    }
    private static void equipment(Writer w,ModernBattleAudience.Equipment v) {
        equipment(w,v.source(),v.revision(),v.effective(),v.equipped(),v.lifetimes(),v.evaluatedAtEpochMillis(),v.validUntilEpochMillis());
    }
    private static void equipment(Writer w,ModernCharacterData.Snapshot s,long revision,ModernCombatProfile.Effective e,
            java.util.List<ModernEquipmentAuthority.Equipped> equipped,java.util.List<ModernItemLifetime.View> lifetimes,long evaluatedAt,long validUntil) {
        var c=s.character();w.i(s.sourceId());w.l(c.objectId());w.text(c.name(),256);w.i(c.characterClass());w.i(c.sex());w.l(c.level());
        var p=s.persisted();w.l(p.maxHp());w.l(p.maxMp());w.l(p.savedHp());w.l(p.savedMp());w.l(p.armorClass());w.l(p.strength());w.l(p.dexterity());w.l(p.constitution());
        w.l(p.intelligence());w.l(p.wisdom());w.l(p.charisma());w.l(p.experience());w.l(p.lawful());w.l(p.originalStrength());w.l(p.originalDexterity());w.l(p.originalConstitution());
        w.l(p.originalIntelligence());w.l(p.originalWisdom());w.l(p.food225());
        w.list(s.inventory(),512,(b,i)->{var t=i.template();b.l(i.objectId());b.l(t.catalogId());b.e(t.kind());b.text(t.nameToken(),256);b.l(t.inventoryGraphic());b.l(t.groundGraphic());b.l(t.unitWeight());
            b.l(i.quantity());b.flag(i.equipped());b.l(i.enchantment());b.l(i.durability());b.flag(i.identified());b.l(i.blessing());b.flag(i.sealed());b.l(i.chargeCount());});
        w.l(revision);w.l(e.maximumHp());w.l(e.maximumMp());w.l(e.initialHp());w.l(e.initialMp());w.l(e.armorClass());
        w.l(e.strength());w.l(e.dexterity());w.l(e.constitution());w.l(e.intelligence());w.l(e.wisdom());w.l(e.charisma());
        w.list(equipped,512,(b,i)->{b.l(i.objectId());b.flag(i.equipped());});
        w.list(lifetimes,512,(b,l)->{b.l(l.itemObjectId());b.e(l.state());b.l(l.expiresAtMillis());});w.l(evaluatedAt);w.l(validUntil);
    }
    private static void items(Writer w,ModernUseHistory.Items i) {
        w.l(i.equipmentRevision());w.l(i.profileRevision());w.l(i.fixedRecovery());w.l(i.uses());w.l(i.restored());
        w.optional(i.last(),(b,u)->{b.l(u.revision());b.l(u.sequence());b.l(u.itemId());anchor(b,u.before());b.l(u.fixedRecovery());b.l(u.rolledBase());b.l(u.restored());b.l(u.hpAfter());});
    }
    private static void ammo(Writer w,ModernAmmo.State a) {
        w.l(a.entityId());w.l(a.requestSequence());w.l(a.revision());w.l(a.selectionRevision());w.l(a.selectedItemId());w.l(a.shotCount());w.l(a.shotRevision());w.l(a.shotItemId());w.l(a.attackSequence());w.l(a.lastTargetEntityId());
        w.i(a.fromX());w.i(a.fromY());w.i(a.toX());w.i(a.toY());w.i(a.projectileSprite());w.flag(a.hit());
        w.list(a.stocks(),2,(b,s)->{var f=s.facts();b.l(f.itemId());b.l(f.small());b.l(f.large());b.i(f.rawArrowSprite());b.flag(f.consume());b.i(f.material());b.i(f.blessing());b.l(s.initial());b.l(s.remaining());});
    }
    private ModernBattleWirePersonal(){}
}
