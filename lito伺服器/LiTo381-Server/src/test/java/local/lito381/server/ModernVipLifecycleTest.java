package local.lito381.server;

import static local.lito381.server.ModernVipEquipment.Field.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModernVipLifecycleTest {
    @Test void equipReplaceShapeThenRemoveNoncurrentShapeItemUsesExactOldContributions() {
        var source=ModernVipLoadoutTest.source(true,false);var before=start(source,5641);
        var equip=ModernVipLifecycle.prepareEquip(before,load(source,1,Map.of(2L,true),1000,5642),2,1000);
        assertEquals(0,before.sequence());assertEquals(1,before.items().size());
        var active=accept(equip,1100);
        assertEquals(1,active.sequence());assertEquals(1,active.equipmentRevision());assertEquals(5642,active.shape().orElseThrow().graphicsId());
        for(var field:ModernVipEquipment.Field.values()) {
            long expected=(field.ordinal()+1L)*(field.polyColumn==null?3:23);
            assertEquals(expected,active.total().get(field),field.name());
            assertEquals(expected-before.total().get(field),equip.delta().get(field),field.name());
        }
        var unequip=ModernVipLifecycle.prepareUnequip(active,load(source,2,Map.of(1L,false,2L,true),1100,0),1,1100);
        var after=accept(unequip,1200);
        assertTrue(after.shape().isEmpty());assertEquals(2,after.items().get(0).objectId());
        for(var field:ModernVipEquipment.Field.values())assertEquals((field.ordinal()+1L)*2,after.total().get(field),field.name());
        assertEquals(Set.of(505L),after.icons());assertTrue(unequip.removedIcons().isEmpty());
        var last=ModernVipLifecycle.prepareRetainedUnequip(after,2,1300);assertEquals(Set.of(505L),last.removedIcons());
        assertEquals(ModernVipEquipment.Abilities.zero(),accept(last,1300).total());
    }

    @Test void reloadNeverChangesRemainingAppliedVipOrShapeAndRemovalDoesNotNeedCatalog() {
        var source=ModernVipLoadoutTest.source(true,false);var before=start(source,5641);
        var changed=change(source,300086,CHA,9000,3000,null);
        var next=load(changed,1,Map.of(),1100,5641);assertNotEquals(before.total(),next.total());
        var prepared=ModernVipLifecycle.prepareEquipmentRevision(before,next,1100);var after=accept(prepared,1200);
        assertEquals(before.total(),after.total());assertEquals(before.shape(),after.shape());
        assertEquals(before.items().get(0).template(),after.items().get(0).template());
        assertEquals(ModernVipEquipment.Abilities.zero(),prepared.delta());
        // No fresh source or catalog is accepted by this removal entry point.
        var remove=ModernVipLifecycle.prepareRetainedUnequip(after,1,1300);
        assertEquals(-before.total().get(CHA),remove.delta().get(CHA));
        assertEquals(ModernVipEquipment.Abilities.zero(),accept(remove,1300).total());
        assertEquals(66,before.total().get(CHA)); // the immutable old ledger remains inspectable
        assertEquals(0,before.sequence());assertEquals(1,before.items().size());
    }

    @Test void reequippingUsesNewTemplateOnlyAfterOldAppliedTemplateWasRemoved() {
        var source=ModernVipLoadoutTest.source(true,false);var first=start(source,5641);
        var empty=accept(ModernVipLifecycle.prepareRetainedUnequip(first,1,1000),1000);
        var changed=change(source,300086,CHA,9000,3000,null);
        var equip=ModernVipLifecycle.prepareEquip(empty,load(changed,2,Map.of(1L,true),1100,5641),1,1100);
        var after=accept(equip,1100);assertEquals(12000,after.total().get(CHA));
        assertEquals(9000,after.items().get(0).template().abilities().get(CHA));
        assertNotEquals(first.total(),after.total());
    }

    @Test void oldExclusiveTypeWinsOverReloadedTypeWhenAdmittingAnotherItem() {
        var source=ModernVipLoadoutTest.source(true,false);var before=start(source,5641);
        var changed=change(source,300086,null,0,0,2L);
        changed=change(changed,300087,null,0,0,1L); // Fresh types 2 and 1 appear compatible; applied type is still 1.
        var next=load(changed,1,Map.of(2L,true),1100,5642);
        assertThrows(IllegalArgumentException.class,()->ModernVipLifecycle.prepareEquip(before,next,2,1100));
        assertEquals(1,before.items().size());assertEquals(1,before.items().get(0).template().exclusiveType());
    }

    @Test void hiddenSiblingsWrongShapeDuplicateActionsAndRevisionGapsAreRejected() {
        var source=ModernVipLoadoutTest.source(true,false);var before=start(source,5641);
        var hidden=load(source,1,Map.of(1L,false,2L,true),1100,5642);
        assertThrows(IllegalArgumentException.class,()->ModernVipLifecycle.prepareEquip(before,hidden,2,1100));
        var wrongShape=load(source,1,Map.of(2L,true),1100,5641);
        assertThrows(IllegalArgumentException.class,()->ModernVipLifecycle.prepareEquip(before,wrongShape,2,1100));
        var gap=load(source,2,Map.of(2L,true),1100,5642);
        assertThrows(IllegalArgumentException.class,()->ModernVipLifecycle.prepareEquip(before,gap,2,1100));
        assertThrows(IllegalArgumentException.class,()->ModernVipLifecycle.prepareEquip(before,wrongShape,1,1100));
        assertThrows(IllegalArgumentException.class,()->ModernVipLifecycle.prepareRetainedUnequip(before,2,1100));
        var both=accept(ModernVipLifecycle.prepareEquip(before,load(source,1,Map.of(2L,true),1100,5642),2,1100),1100);
        var keepShape=load(source,2,Map.of(1L,false,2L,true),1200,5642);
        assertThrows(IllegalArgumentException.class,()->ModernVipLifecycle.prepareUnequip(both,keepShape,1,1200));
    }

    @Test void contextOwnerEntityLifeIdentityRevisionAndReplayCannotCommitAnOldCandidate() {
        var source=ModernVipLoadoutTest.source(true,false);var before=start(source,5641);
        var candidate=ModernVipLifecycle.prepareRetainedUnequip(before,1,1100);var a=before.actor();
        for(var wrong:java.util.List.of(new ModernVipLifecycle.Actor(UUID.randomUUID(),a.owner(),a.entity(),a.life()),
                new ModernVipLifecycle.Actor(a.context(),new TournamentPresence.CharacterKey(42,a.owner().objectId()),a.entity(),a.life()),
                new ModernVipLifecycle.Actor(a.context(),a.owner(),a.entity()+1,a.life()),new ModernVipLifecycle.Actor(a.context(),a.owner(),a.entity(),a.life()+1)))
            assertThrows(IllegalArgumentException.class,()->ModernVipLifecycle.acceptAt(candidate,wrong,before,0,1100));
        assertThrows(IllegalArgumentException.class,()->ModernVipLifecycle.acceptAt(candidate,a,before,1,1100));
        assertThrows(IllegalArgumentException.class,()->ModernVipLifecycle.acceptAt(candidate,a,before,0,1099));
        var duplicate=ModernVipLifecycle.start(a,load(source,0,Map.of(),1000,5641),1000);
        assertThrows(IllegalArgumentException.class,()->ModernVipLifecycle.acceptAt(candidate,a,duplicate,0,1100));
        var after=accept(candidate,1100);
        assertThrows(IllegalArgumentException.class,()->ModernVipLifecycle.acceptAt(candidate,a,after,1,1100));
    }

    @Test void changedSourceBindingOrAppliedInstanceFingerprintCannotRefreshLedger() {
        var source=ModernVipLoadoutTest.source(true,false);var before=start(source,5641);var q=source.input().equipment();
        byte[] bytes=new byte[32];bytes[0]=9;var fp=new ModernEquipmentOverrideStore.Fingerprint(bytes);
        var binding=new ModernEquipmentReconciliation.ExpectedBinding(before.actor().owner(),fp);
        var stored=ModernCombatProfileTest.stored(q,1,Map.of()).orElseThrow();
        var replacement=Optional.of(new ModernEquipmentOverrideStore.Stored(stored.key(),1,new ModernEquipmentOverrideStore.Replacement(fp,stored.value().overrides())));
        var wrong=ModernVipLoadout.qualify(source,binding,replacement,1100,5641);
        assertThrows(IllegalArgumentException.class,()->ModernVipLifecycle.prepareEquipmentRevision(before,wrong,1100));
        var timed=timed(source,1000,"2026-10-01 00:00:00",1);
        var changed=load(timed,1,Map.of(),1100,5641);
        assertThrows(IllegalArgumentException.class,()->ModernVipLifecycle.prepareEquipmentRevision(before,changed,1100));
    }

    @Test void expiryUsesPinnedTemplateAndClearsShapeWithoutChangingDurableRevision() {
        var source=timed(ModernVipLoadoutTest.source(true,true),1000,"2026-10-01 00:00:00",1);
        var before=start(source,5642);assertTrue(ModernVipLifecycle.prepareExpiry(before,1999).isEmpty());
        var candidate=ModernVipLifecycle.prepareExpiry(before,2000).orElseThrow();
        assertEquals(ModernVipLifecycle.Operation.EXPIRE,candidate.operation());assertEquals(0,candidate.after().equipmentRevision());
        var after=accept(candidate,2100);
        assertEquals(1,after.items().size());assertEquals(2,after.items().get(0).objectId());assertTrue(after.shape().isEmpty());
        assertEquals(Set.of(505L),after.icons());assertTrue(candidate.removedIcons().isEmpty());
        assertTrue(ModernVipLifecycle.prepareExpiry(after,2200).isEmpty());
        assertThrows(IllegalArgumentException.class,()->ModernVipLifecycle.prepareRetainedUnequip(before,1,2000));
        assertThrows(IllegalArgumentException.class,()->ModernVipLifecycle.prepareExpiry(after,2099));
    }

    @Test void deadlineCrossingWhileCandidatePendingRejectsInsteadOfRenewingOrDroppingAnotherExpiry() {
        var source=timed(ModernVipLoadoutTest.source(true,false),1000,"2026-10-01 00:00:00",1);var before=start(source,5641);
        var clear=ModernVipLifecycle.prepareClearShape(before,1900).orElseThrow();
        assertThrows(IllegalArgumentException.class,()->accept(clear,2000));
        var remove=ModernVipLifecycle.prepareRetainedUnequip(before,1,1900);
        assertThrows(IllegalArgumentException.class,()->accept(remove,2000));
        assertEquals(0,before.sequence());
        var expired=ModernVipLifecycle.prepareExpiry(before,2000).orElseThrow();assertTrue(accept(expired,9999).items().isEmpty());
    }

    @Test void freshClockRoundingNeverRenewsAppliedDeadlineAndShorterFreshDeadlineWins() {
        var base=ModernVipLoadoutTest.source(true,false);var source=timed(base,1000,"2026-10-01 00:00:00",1);var before=start(source,5641);
        var longer=timed(base,1500,"2026-10-01 00:00:00",1);
        var candidate=ModernVipLifecycle.prepareEquipmentRevision(before,load(longer,1,Map.of(),1600,5641),1600);
        assertEquals(2000,candidate.after().items().get(0).lifetime().expiresAtMillis());
        assertThrows(IllegalArgumentException.class,()->accept(candidate,2000));assertNotNull(accept(candidate,1999));
        var shorter=timed(base,1000,"2026-10-01 00:00:00.500000",1);
        var early=ModernVipLifecycle.prepareEquipmentRevision(before,load(shorter,1,Map.of(),1100,5641),1100);
        assertEquals(1500,early.after().items().get(0).lifetime().expiresAtMillis());
        assertThrows(IllegalArgumentException.class,()->accept(early,1500));
    }

    @Test void anotherDeadlineCrossingWhileExpiryPendingRequiresFreshCombinedExpiry() {
        var source=timed(ModernVipLoadoutTest.source(true,true),1000,"2026-10-01 00:00:00",1);
        var q=source.input().equipment();var facts=new HashMap<>(q.lifetimes().items());var item=q.items().get(2L).source();
        facts.put(2L,new ModernItemLifetime.Fact(2,item.template().catalogId(),item.template().kind(),true,OptionalLong.of(0),true,
                OptionalLong.of(1),Optional.of("2026-10-01 00:00:02"),0,false));
        source=ModernVipLoadoutTest.withEquipment(source,new ModernEquipmentFacts.Qualified(q.source(),q.items(),q.catalog(),
                new ModernItemLifetime.Observation(7,q.source().character().objectId(),"2026-10-01 00:00:00",1000,0,facts)));
        var before=start(source,5642);var first=ModernVipLifecycle.prepareExpiry(before,2000).orElseThrow();
        assertEquals(1,accept(first,2999).items().size());
        assertThrows(IllegalArgumentException.class,()->accept(first,3000));
        var combined=ModernVipLifecycle.prepareExpiry(before,3000).orElseThrow();var after=accept(combined,4000);
        assertTrue(after.items().isEmpty());assertEquals(Set.of(505L),combined.removedIcons());assertEquals(0,after.equipmentRevision());
        assertEquals(ModernVipEquipment.Abilities.zero(),after.total());
    }

    @Test void noPolyVipDoesNotClearAnotherVipShapeOnEquipOrRemoval() {
        var source=ModernVipLoadoutTest.source(true,false);var facts=source.vipCatalog();var vips=new HashMap<>(facts.vips());
        var old=vips.get(300087L);vips.put(300087L,new ModernVipEquipment.Vip(old.catalogId(),old.exclusiveType(),0,0,old.abilities()));
        var polys=new HashMap<>(facts.polymorphs());polys.remove(5642L);
        source=ModernVipLoadoutTest.withCatalog(source,new ModernVipEquipment.Catalog(7,"catalog",vips,polys));
        var before=start(source,5641);var equip=ModernVipLifecycle.prepareEquip(before,load(source,1,Map.of(2L,true),1000,5641),2,1000);
        var both=accept(equip,1000);assertEquals(before.shape(),both.shape());assertTrue(equip.addedIcons().isEmpty());
        var removal=ModernVipLifecycle.prepareUnequip(both,load(source,2,Map.of(2L,false),1100,5641),2,1100);
        var after=accept(removal,1100);assertEquals(before.total(),after.total());assertEquals(before.shape(),after.shape());
    }

    @Test void explicitShapeClearKeepsItemAbilitiesAndDoesNotInventCancellationPermission() {
        var source=ModernVipLoadoutTest.source(true,false);var before=start(source,5641);
        assertTrue(before.shape().orElseThrow().retainsShapeOnCancellation());
        var clear=ModernVipLifecycle.prepareClearShape(before,1000).orElseThrow();var after=accept(clear,1000);
        assertEquals(1,after.items().size());assertEquals(before.icons(),after.icons());
        assertEquals(source.vipCatalog().vips().get(300086L).abilities(),after.total());
        assertTrue(ModernVipLifecycle.prepareClearShape(after,1001).isEmpty());
        assertTrue(clear.addedIcons().isEmpty());assertTrue(clear.removedIcons().isEmpty());
    }

    @Test void liveMaximumIncreaseNeverHealsAndDecreaseOnlyClipsExcess() {
        var source=ModernVipLoadoutTest.source(true,false);var before=start(source,5641);
        var live=new ModernEquipmentVitals.Vitals(150,188,100,149);
        var equip=ModernVipLifecycle.prepareEquip(before,load(source,1,Map.of(2L,true),1000,5642),2,1000);
        var increased=equip.retainVitals(live);assertEquals(new ModernEquipmentVitals.Vitals(150,284,100,257),increased);
        var active=accept(equip,1000);var remove=ModernVipLifecycle.prepareRetainedUnequip(active,1,1000);
        assertEquals(new ModernEquipmentVitals.Vitals(116,116,68,68),remove.retainVitals(increased));
        var dead=equip.retainVitals(new ModernEquipmentVitals.Vitals(0,188,0,149));assertEquals(0,dead.hp());assertEquals(0,dead.mp());
    }

    @Test void checkedDeltasAndImmutableCandidatesCannotPublishPartialOverflow() {
        for(var field:ModernVipEquipment.Field.values()) {
            var a=new EnumMap<ModernVipEquipment.Field,Long>(ModernVipEquipment.Abilities.zero().values());var b=new EnumMap<>(a);
            a.put(field,Long.MIN_VALUE);b.put(field,1L);
            assertThrows(ArithmeticException.class,()->new ModernVipEquipment.Abilities(a).minus(new ModernVipEquipment.Abilities(b)));
            a.put(field,Long.MAX_VALUE);b.put(field,-1L);
            assertThrows(ArithmeticException.class,()->new ModernVipEquipment.Abilities(a).minus(new ModernVipEquipment.Abilities(b)));
            a.put(field,4_294_967_306L);b.put(field,7L);
            assertEquals(4_294_967_299L,new ModernVipEquipment.Abilities(a).minus(new ModernVipEquipment.Abilities(b)).get(field));
        }
        var source=change(ModernVipLoadoutTest.source(true,false),300086,CHA,Long.MIN_VALUE,0,null);var before=start(source,0);
        assertThrows(ArithmeticException.class,()->ModernVipLifecycle.prepareRetainedUnequip(before,1,1000));
        assertEquals(Long.MIN_VALUE,before.total().get(CHA));assertEquals(0,before.sequence());
        assertThrows(UnsupportedOperationException.class,()->before.icons().clear());assertThrows(UnsupportedOperationException.class,()->before.items().clear());
        assertFalse(before.toString().contains("catalog"));assertFalse(before.actor().toString().contains("3333"));
    }

    static ModernVipLoadout load(ModernPhysicalCombat.Source source,long revision,Map<Long,Boolean> flags,long now,long shape) {
        var q=source.input().equipment();return ModernVipLoadout.qualify(source,ModernCombatProfileTest.binding(q),
                revision==0?Optional.empty():ModernCombatProfileTest.stored(q,revision,flags),now,shape);
    }
    static ModernVipLifecycle.State start(ModernPhysicalCombat.Source source,long shape) {
        var load=load(source,0,Map.of(),1000,shape);
        return ModernVipLifecycle.start(new ModernVipLifecycle.Actor(UUID.randomUUID(),load.owner(),1,0),load,1000);
    }
    static ModernVipLifecycle.State accept(ModernVipLifecycle.Candidate candidate,long now) {
        return ModernVipLifecycle.acceptAt(candidate,candidate.before().actor(),candidate.before(),candidate.before().equipmentRevision(),now);
    }
    static ModernPhysicalCombat.Source change(ModernPhysicalCombat.Source source,long catalog,ModernVipEquipment.Field field,long direct,long poly,Long type) {
        var facts=source.vipCatalog();var vips=new HashMap<>(facts.vips());var old=vips.get(catalog);
        var values=new EnumMap<ModernVipEquipment.Field,Long>(old.abilities().values());if(field!=null)values.put(field,direct);
        vips.put(catalog,new ModernVipEquipment.Vip(catalog,type==null?old.exclusiveType():type,old.iconId(),old.polymorphId(),new ModernVipEquipment.Abilities(values)));
        var polys=new HashMap<>(facts.polymorphs());var shape=polys.get(old.polymorphId());
        if(field!=null&&shape!=null){var v=new EnumMap<ModernVipEquipment.Field,Long>(shape.abilities().values());v.put(field,poly);polys.put(shape.graphicsId(),new ModernVipEquipment.Polymorph(shape.graphicsId(),true,shape.noDeathExpLoss(),shape.retainsShapeOnCancellation(),new ModernVipEquipment.Abilities(v)));}
        return ModernVipLoadoutTest.withCatalog(source,new ModernVipEquipment.Catalog(facts.sourceId(),facts.database(),vips,polys));
    }
    static ModernPhysicalCombat.Source timed(ModernPhysicalCombat.Source source,long queryStart,String dbNow,long timedId) {
        var q=source.input().equipment();var facts=new HashMap<>(q.lifetimes().items());var item=q.items().get(timedId).source();
        facts.put(timedId,new ModernItemLifetime.Fact(timedId,item.template().catalogId(),item.template().kind(),item.equipped(),OptionalLong.of(0),true,OptionalLong.of(1),Optional.of("2026-10-01 00:00:01"),0,false));
        return ModernVipLoadoutTest.withEquipment(source,new ModernEquipmentFacts.Qualified(q.source(),q.items(),q.catalog(),
                new ModernItemLifetime.Observation(q.source().sourceId(),q.source().character().objectId(),dbNow,queryStart,0,facts)));
    }
}
