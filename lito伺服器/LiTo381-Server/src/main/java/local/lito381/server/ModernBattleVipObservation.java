package local.lito381.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Full-VIP observation contract, deliberately not an old Audience.View or a
 * wire capability. Only human deathmatch is currently qualified. Projection
 * never ticks the engine, rereads SQL, or grants authentication/equipment SET.
 * Public state contains runtime combat facts; saved versions and applied VIP
 * inventory belong only to their exact source/selection owner. No JDBC source,
 * template catalog, binding fingerprint, private VIP context or RNG escapes.
 * Shape/icon ids below are configuration facts, NOT shared-resource LOAD acks. */
final class ModernBattleVipObservation {
    /** Public applied appearance only. graphicsId=0 means no VIP override, not
     * a request to draw sprite zero. Class/sex/weapon appearance stays in the
     * existing public roster/presentation. No saved revision or VIP abilities. */
    record Appearance(long entityId,int sourceId,long objectId,long life,long runtimeAbilityRevision,
            long lifecycleSequence,long graphicsId) {
        Appearance {
            if(entityId<1||sourceId<1||objectId<1||life<0||runtimeAbilityRevision<0||lifecycleSequence<0||graphicsId<0)throw bad();
        }
    }
    record Versions(long savedEquipmentRevision,long runtimeAbilityRevision,long lifecycleSequence) {
        Versions {
            if(savedEquipmentRevision<0||runtimeAbilityRevision<savedEquipmentRevision
                    ||lifecycleSequence<0)throw bad();
            // The lifecycle ordinal is not an equipment or ability version.
            // Binding to an actual frame is checked separately below.
        }
    }
    /** No saved version, source inventory or selection epoch in this type. */
    record World(ModernTargetHistory.World identity,long revision,ModernBattleEngine.Phase phase,
            ModernBattleEngine.AbortReason abortReason,ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,
            ModernBattleObservation.Features features,ModernBattleObservation.Clock clock,
            List<ModernBattleObservation.Actor> actors,ModernMatchSettlement.Event result,
            ModernHasteEffects.State hasteEffects,ModernSpeedEffects.State speedEffects,List<Appearance> appearances) {
        World {
            actors=List.copyOf(actors);appearances=List.copyOf(appearances);
            if(actors.size()!=appearances.size())throw bad();
            for(int i=0;i<actors.size();i++) {
                var a=actors.get(i);var p=appearances.get(i);var key=a.pose().member().key();
                if(a.pose().entityId()!=p.entityId()||key.sourceId()!=p.sourceId()||key.objectId()!=p.objectId()
                        ||a.resources().life()!=p.life()||a.resources().equipmentRevision()!=p.runtimeAbilityRevision())throw bad();
            }
        }
        private static World from(ModernBattleObservation.World w,Map<ModernRooms.Scope,ModernBattleObservation.OwnerView> owners) {
            if(!w.vip())throw bad();
            var appearances=new java.util.ArrayList<Appearance>();
            for(var a:w.actors()) {
                var owner=owners.values().stream().filter(v->v.owner().entityId()==a.pose().entityId()).findFirst().orElseThrow(ModernBattleVipObservation::bad).owner();
                var e=Equipment.from(owner);var r=owner.history().history().resources();
                appearances.add(new Appearance(owner.entityId(),owner.scope().sourceId(),owner.scope().objectId(),r.life(),
                        e.versions().runtimeAbilityRevision(),e.versions().lifecycleSequence(),e.configuredShape()==null?0:e.configuredShape().graphicsId()));
            }
            return new World(w.identity(),w.revision(),w.phase(),w.abortReason(),w.room(),w.manifest(),w.features(),w.clock(),
                    w.actors(),w.result(),w.hasteEffects(),w.speedEffects(),appearances);
        }
        @Override public String toString(){return "VipAudienceWorld[redacted]";}
    }
    /** Copies only the applied template; never exposes a source fingerprint. */
    record Item(long objectId,ModernVipEquipment.Vip applied,ModernItemLifetime.View lifetime) {
        Item {Objects.requireNonNull(applied);Objects.requireNonNull(lifetime);if(objectId<1||objectId!=lifetime.itemObjectId()||!lifetime.usable())throw bad();}
    }
    record Equipment(ModernCharacterData.Snapshot source,Versions versions,ModernCombatProfile.Effective effective,
            List<ModernEquipmentAuthority.Equipped> equipped,List<ModernItemLifetime.View> lifetimes,
            long evaluatedAtEpochMillis,long validUntilEpochMillis,List<Item> vipItems,
            ModernVipEquipment.Abilities vipAbilities,List<Long> iconIds,ModernVipEquipment.Polymorph configuredShape) {
        Equipment {
            Objects.requireNonNull(source);Objects.requireNonNull(versions);Objects.requireNonNull(effective);Objects.requireNonNull(vipAbilities);
            equipped=List.copyOf(equipped);lifetimes=List.copyOf(lifetimes);vipItems=List.copyOf(vipItems);iconIds=List.copyOf(iconIds);
            if(evaluatedAtEpochMillis<0||validUntilEpochMillis< -1
                    ||validUntilEpochMillis>=0&&validUntilEpochMillis<=evaluatedAtEpochMillis)throw bad();
        }
        private static Equipment from(ModernBattleObservation.PrivateOwner owner) {
            var fighter=owner.loadout().fighter();var p=fighter.profile();var state=fighter.vip().state();var vip=state.ledger();
            var saved=state.profile().character();var resources=owner.history().history().resources();
            if(!vip.actor().owner().equals(new TournamentPresence.CharacterKey(owner.scope().sourceId(),owner.scope().objectId()))
                    ||vip.actor().entity()!=owner.entityId()||vip.actor().life()!=resources.life()
                    ||p.equipmentRevision()!=resources.equipmentRevision()||!p.source().equals(saved.source())
                    ||vip.equipmentRevision()!=saved.equipmentRevision()
                    ||vip.equipmentRevision()!=state.stored().map(ModernEquipmentOverrideStore.Stored::revision).orElse(0L)
                    ||!p.effective().equals(saved.effective())||!p.equipped().equals(saved.equipped())
                    ||p.evaluatedAtMillis()!=vip.observedMillis()||p.validUntilMillis()!=saved.validUntilMillis()
                    ||!p.sourceLifetimes().equals(saved.sourceLifetimes()))throw bad();
            return new Equipment(p.source(),new Versions(vip.equipmentRevision(),p.equipmentRevision(),vip.sequence()),p.effective(),
                    p.equipped(),p.sourceLifetimes(),p.evaluatedAtMillis(),p.validUntilMillis(),
                    vip.items().stream().map(i->new Item(i.objectId(),i.template(),i.lifetime())).toList(),
                    vip.total(),vip.icons().stream().sorted().toList(),vip.shape().orElse(null));
        }
        @Override public String toString(){return "VipAudienceEquipment[redacted]";}
    }
    record Personal(ModernRooms.Scope scope,long entityId,Equipment equipment,List<ModernSkills.Skill> skills,
            ModernEquipmentHistory.State history,long spellBirthRevision,ModernBattleObservation.Supplies supplies,
            ModernInventoryWeight.Snapshot weight,ModernAmmo.State ammo,ModernNaturalRegen.Clock regenClock,ModernSpellMaterials.State materials) {
        Personal {skills=List.copyOf(skills);}
        private static Personal from(ModernBattleObservation.PrivateOwner o) {
            return new Personal(o.scope(),o.entityId(),Equipment.from(o),o.skills()==null?List.of():o.skills().skills(),
                    o.history().history(),o.history().spells().birthRevision(),o.supplies(),o.weight(),o.ammo(),o.regenClock(),o.materials());
        }
        @Override public String toString(){return "VipAudiencePersonal[redacted]";}
    }
    /** Only a fully validated, single-capture publication can create a view. */
    static final class View {
        private final World world;
        private final Personal personal;
        private final ModernArmorPresentation.Candidate armorPresentation;
        private View(World world,ModernBattleObservation.OwnerView owner,ModernBattleObservation.Capture capture) {
            this.world=world;personal=Personal.from(owner.owner());
            if(!owner.world().vip()||world.revision()!=owner.world().revision()||!world.identity().equals(owner.world().identity()))throw bad();
            armorPresentation=world.features().armorProtection()?ModernArmorPresentation.from(capture,world,personal):null;
        }
        World world(){return world;}
        Personal personal(){return personal;}
        ModernArmorPresentation.Candidate armorPresentation(){return armorPresentation;}
        @Override public String toString(){return "VipAudienceView[redacted]";}
    }
    static final class Publication {
        private final World world;
        private final Map<ModernRooms.Scope,View> owners;
        private Publication(ModernBattleObservation.Capture capture) {
            var canonical=capture.vipWorld();var checked=capture.vipOwnerViews(canonical);
            if(checked.size()!=canonical.actors().size())throw bad();
            world=World.from(canonical,checked);var views=new HashMap<ModernRooms.Scope,View>();
            checked.forEach((scope,owner)->views.put(scope,new View(world,owner,capture)));owners=Map.copyOf(views);
        }
        World world(){return world;}
        /** Trusted lookup, NOT authentication. Caller must authorize selection. */
        View forOwner(ModernRooms.Scope scope) {
            var view=owners.get(Objects.requireNonNull(scope));
            if(view==null)throw new SecurityException("VIP observation owner not present");
            return view;
        }
        @Override public String toString(){return "VipPublication[redacted]";}
    }
    static Publication from(ModernBattleObservation.Capture capture){return new Publication(Objects.requireNonNull(capture));}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid VIP battle observation");}
    private ModernBattleVipObservation(){}
}
