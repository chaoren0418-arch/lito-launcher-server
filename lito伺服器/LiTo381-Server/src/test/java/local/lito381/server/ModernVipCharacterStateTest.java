package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernVipEquipment.Field.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModernVipCharacterStateTest {
    @Test void allLayersAdvanceTogetherAndCommitUsesLatestLiveVitalsWithoutHealing() {
        var source=ModernVipLoadoutTest.source(true,false);var before=start(source,5641);
        var pending=ModernVipCharacterState.prepareEquip(before,load(source,1,Map.of(2L,true),1100,5642),2,1100);
        assertEquals(0,before.ledger().sequence());assertEquals(188,before.profile().character().effective().maximumHp());
        assertEquals(284,pending.pending().character().effective().maximumHp());
        var after=accept(pending,before,1200,41,13);assertEquals(41,after.profile().character().effective().initialHp());
        assertEquals(13,after.profile().character().effective().initialMp());assertEquals(1200,after.profile().character().evaluatedAtMillis());
        assertEquals(ModernCombatProfile.VitalPolicy.RETAINED_MATCH,after.profile().character().vitalPolicy());
        assertEquals(1,after.ledger().sequence());assertEquals(1,after.stored().orElseThrow().revision());
        assertConsistent(after);assertEquals(414,after.profile().genericDamageReduction());assertEquals(437,after.profile().magicOnlyReduction());
        var remove=ModernVipCharacterState.prepareRetainedUnequip(after,1,1300);
        var last=accept(remove,after,1400,200,200);assertEquals(116,last.profile().character().effective().initialHp());
        assertEquals(68,last.profile().character().effective().initialMp());assertTrue(last.ledger().shape().isEmpty());assertConsistent(last);
        assertEquals(36,last.profile().genericDamageReduction());assertEquals(60,last.profile().experienceRate());
    }
    @Test void zeroLiveHpCannotBeRevivedByEquipmentOrShapeRemoval() {
        var source=ModernVipLoadoutTest.source(true,false);var before=start(source,5641);
        var next=ModernVipCharacterState.prepareEquip(before,load(source,1,Map.of(2L,true),1100,5642),2,1100);
        var after=accept(next,before,1100,0,0);assertEquals(0,after.profile().character().effective().initialHp());
        var clear=ModernVipCharacterState.prepareClearShape(after,1200).orElseThrow();
        assertEquals(0,accept(clear,after,1300,0,0).profile().character().effective().initialHp());
        assertThrows(IllegalArgumentException.class,()->accept(next,before,1100,189,0));
    }
    @Test void reloadedAppliedVipAndShapeDoNotChangeCharacterAttributes() {
        var source=withArmor(ModernVipLoadoutTest.source(true,false),false);var before=start(source,5641);
        var reload=ModernVipLifecycleTest.change(source,300086,CHA,9000,3000,null);
        var fresh=load(reload,1,Map.of(3L,true),1100,5641);
        var pending=ModernVipCharacterState.prepareOrdinaryEquipment(before,fresh,1100);
        var after=accept(pending,before,1200,123,12);
        assertEquals(before.ledger().total(),after.ledger().total());assertEquals(before.ledger().shape(),after.ledger().shape());
        assertEquals(before.profile().character().effective().charisma(),after.profile().character().effective().charisma());
        assertEquals(228,after.profile().character().effective().maximumHp());assertEquals(123,after.profile().character().effective().initialHp());
        assertConsistent(after);
        var removed=accept(ModernVipCharacterState.prepareRetainedUnequip(after,1,1300),after,1400,200,100);
        assertEquals(140,removed.profile().character().effective().maximumHp());assertEquals(140,removed.profile().character().effective().initialHp());
        assertEquals(0,removed.profile().genericDamageReduction());assertTrue(flag(removed,3));assertFalse(flag(removed,1));
    }
    @Test void reequippingAdoptsFreshTemplateOnlyAfterRemovingOldOne() {
        var source=ModernVipLoadoutTest.source(true,false);var before=start(source,5641);
        var empty=accept(ModernVipCharacterState.prepareRetainedUnequip(before,1,1100),before,1200,80,30);
        var changed=ModernVipLifecycleTest.change(source,300086,CHA,9000,3000,null);
        var equip=ModernVipCharacterState.prepareEquip(empty,load(changed,2,Map.of(1L,true),1300,5641),1,1300);
        var after=accept(equip,empty,1400,70,20);assertEquals(12000,after.ledger().total().get(CHA));
        assertEquals(source.input().equipment().source().persisted().charisma()+12000,after.profile().character().effective().charisma());
        assertEquals(70,after.profile().character().effective().initialHp());assertConsistent(after);
    }
    @Test void clearShapeHasSameRevisionAndOnlyDropsTheAppliedShapeLayer() {
        var before=start(ModernVipLoadoutTest.source(true,true),5642);
        var c=ModernVipCharacterState.prepareClearShape(before,1100).orElseThrow();assertEquals(c.expectedStore(),c.nextStore());
        var after=accept(c,before,1200,180,190);assertEquals(0,after.ledger().equipmentRevision());
        assertEquals(1,after.ledger().sequence());assertEquals(124,after.profile().character().effective().maximumHp());
        assertEquals(124,after.profile().character().effective().initialHp());assertEquals(77,after.profile().character().effective().initialMp());
        assertEquals(before.ledger().icons(),after.ledger().icons());assertTrue(ModernVipCharacterState.prepareClearShape(after,1200).isEmpty());assertConsistent(after);
    }
    @Test void expiryMasksRawSqlEquippedFlagWithoutWritingOrAdvancingRevision() {
        var source=ModernVipLifecycleTest.timed(ModernVipLoadoutTest.source(true,true),1000,"2026-10-01 00:00:00",1);
        var before=start(source,5642);assertTrue(ModernVipCharacterState.prepareExpiry(before,1999).isEmpty());
        var c=ModernVipCharacterState.prepareExpiry(before,2000).orElseThrow();var after=accept(c,before,2100,200,200);
        assertTrue(source.input().equipment().items().get(1L).source().equipped());assertFalse(flag(after,1));assertTrue(flag(after,2));
        assertEquals(0,after.ledger().equipmentRevision());assertTrue(after.stored().isEmpty());assertTrue(after.ledger().shape().isEmpty());
        assertEquals(116,after.profile().character().effective().maximumHp());assertEquals(116,after.profile().character().effective().initialHp());
        assertEquals(ModernItemLifetime.State.EXPIRED,after.profile().character().sourceLifetimes().get(0).state());
        assertEquals(-1,after.profile().character().validUntilMillis());assertTrue(ModernVipCharacterState.prepareExpiry(after,2200).isEmpty());assertConsistent(after);
    }
    @Test void earliestLeaseSurvivesUnequipAndCannotBeRenewedByFreshObservation() {
        var base=ModernVipLoadoutTest.source(true,false);var source=ModernVipLifecycleTest.timed(base,1000,"2026-10-01 00:00:00",1);
        var before=start(source,5641);var empty=accept(ModernVipCharacterState.prepareRetainedUnequip(before,1,1100),before,1200,50,20);
        var fresh=ModernVipLifecycleTest.timed(base,1800,"2026-10-01 00:00:00",1);
        var c=ModernVipCharacterState.prepareEquip(empty,load(fresh,2,Map.of(1L,true),1900,5641),1,1900);
        assertEquals(2000,c.pending().character().validUntilMillis());
        assertThrows(IllegalArgumentException.class,()->accept(c,empty,2000,50,20));
        assertEquals(2000,accept(c,empty,1999,50,20).ledger().items().get(0).lifetime().expiresAtMillis());
        assertThrows(IllegalArgumentException.class,()->ModernVipCharacterState.prepareEquip(empty,load(fresh,2,Map.of(1L,true),2100,5641),1,2100));
        assertEquals(2000,empty.leases().get(1L).expiresAtMillis());
    }
    @Test void aCarriedNeverEquippedVipAlsoRetainsItsFirstObservedDeadline() {
        var base=ModernVipLoadoutTest.source(false,false);var before=start(ModernVipLifecycleTest.timed(base,1000,"2026-10-01 00:00:00",1),0);
        var fresh=ModernVipLifecycleTest.timed(base,1800,"2026-10-01 00:00:00",1);
        assertThrows(IllegalArgumentException.class,()->ModernVipCharacterState.prepareEquip(before,load(fresh,1,Map.of(1L,true),2100,5641),1,2100));
        assertEquals(0,before.ledger().sequence());
    }
    @Test void expiredAppliedVipCannotReturnThroughAReReadWithALaterDeadline() {
        var base=ModernVipLoadoutTest.source(true,false);var before=start(ModernVipLifecycleTest.timed(base,1000,"2026-10-01 00:00:00",1),5641);
        var expired=accept(ModernVipCharacterState.prepareExpiry(before,2000).orElseThrow(),before,2000,50,20);
        var fresh=ModernVipLifecycleTest.timed(base,1800,"2026-10-01 00:00:00",1);
        assertThrows(IllegalArgumentException.class,()->ModernVipCharacterState.prepareEquip(expired,load(fresh,1,Map.of(1L,true),2100,5641),1,2100));
        assertFalse(flag(expired,1));assertEquals(2000,expired.leases().get(1L).expiresAtMillis());
    }
    @Test void earlierFreshDeadlinePersistsAfterLaterUnequipAndReequip() {
        var base=withArmor(ModernVipLoadoutTest.source(true,false),false);
        var source=ModernVipLifecycleTest.timed(base,1000,"2026-10-01 00:00:00",1);var before=start(source,5641);
        var shorter=ModernVipLifecycleTest.timed(base,1000,"2026-10-01 00:00:00.500000",1);
        var c=ModernVipCharacterState.prepareOrdinaryEquipment(before,load(shorter,1,Map.of(3L,true),1100,5641),1100);
        var after=accept(c,before,1200,50,20);assertEquals(1500,after.profile().character().validUntilMillis());
        var empty=accept(ModernVipCharacterState.prepareRetainedUnequip(after,1,1250),after,1300,50,20);
        var equip=load(source,3,Map.of(1L,true,3L,true),1600,5641);
        assertThrows(IllegalArgumentException.class,()->ModernVipCharacterState.prepareEquip(empty,equip,1,1600));
    }
    @Test void shapeOrRemovalPendingAcrossAnExpiryRejectsTheWholeCandidate() {
        var source=ModernVipLifecycleTest.timed(ModernVipLoadoutTest.source(true,false),1000,"2026-10-01 00:00:00",1);var before=start(source,5641);
        for(var c:List.of(ModernVipCharacterState.prepareClearShape(before,1900).orElseThrow(),ModernVipCharacterState.prepareRetainedUnequip(before,1,1900)))
            assertThrows(IllegalArgumentException.class,()->accept(c,before,2000,50,20));
        assertEquals(0,before.ledger().sequence());assertEquals(188,before.profile().character().effective().maximumHp());
    }
    @Test void ordinaryTimedEquipmentRefreshesEvenWhenNoVipExpires() {
        var source=ModernVipLifecycleTest.timed(withArmor(ModernVipLoadoutTest.source(true,false),true),1000,"2026-10-01 00:00:00",3);
        var before=start(source,5641);assertEquals(228,before.profile().character().effective().maximumHp());
        var c=ModernVipCharacterState.prepareExpiry(before,2000).orElseThrow();assertEquals(ModernVipLifecycle.Operation.TIME_REFRESH,c.vip().operation());
        var after=accept(c,before,2200,210,100);assertEquals(188,after.profile().character().effective().maximumHp());
        assertEquals(188,after.profile().character().effective().initialHp());assertFalse(flag(after,3));assertEquals(before.ledger().total(),after.ledger().total());
        assertEquals(0,after.ledger().equipmentRevision());assertConsistent(after);
    }
    @Test void noVipActionMayHideOrdinaryEquipmentChangesOrPersistentSiblingEdits() {
        var source=withArmor(ModernVipLoadoutTest.source(true,false),false);var before=start(source,5641);
        assertThrows(IllegalArgumentException.class,()->ModernVipCharacterState.prepareEquip(before,load(source,1,Map.of(2L,true,3L,true),1100,5642),2,1100));
        assertThrows(IllegalArgumentException.class,()->ModernVipCharacterState.prepareUnequip(before,load(source,1,Map.of(1L,false,3L,true),1100,0),1,1100));
        assertThrows(IllegalArgumentException.class,()->ModernVipCharacterState.prepareOrdinaryEquipment(before,load(source,1,Map.of(3L,true,1L,true),1100,5641),1100));
        assertThrows(IllegalArgumentException.class,()->ModernVipCharacterState.prepareOrdinaryEquipment(before,load(source,1,Map.of(),1100,5641),1100));
        assertThrows(IllegalArgumentException.class,()->ModernVipCharacterState.prepareOrdinaryEquipment(before,load(source,1,Map.of(3L,false),1100,5641),1100));
        var valid=ModernVipCharacterState.prepareOrdinaryEquipment(before,load(source,1,Map.of(3L,true),1100,5641),1100);
        assertTrue(flag(accept(valid,before,1200,50,20),3));
    }
    @Test void changedBaseInstanceRulesAndRawLifetimeNeedReadmission() {
        var source=withArmor(ModernVipLoadoutTest.source(true,false),false);var before=start(source,5641);var q=source.input().equipment();
        var changedBase=ModernVipLoadoutTest.withEquipment(source,ModernCombatProfileTest.withVitals(q,101,50,50,20,10));
        var changedTime=ModernVipLifecycleTest.timed(source,1000,"2026-10-01 00:00:00",1);
        var rules=new HashMap<>(q.catalog().lookups());var key=rules.keySet().stream().filter(k->k.kind()!=RuleKind.VIP).findFirst().orElseThrow();
        rules.put(key,RuleValue.present(Ability.zero().with(Stat.MAX_HP,3)));
        var c=q.catalog();var changedRules=ModernVipLoadoutTest.withEquipment(source,new Qualified(q.source(),q.items(),
                new Catalog(c.sourceId(),c.resourceDatabase(),rules,c.verifiedSetIds(),c.sets(),c.combos(),c.vipTableState()),q.lifetimes()));
        var instances=new HashMap<>(q.items());var old=instances.get(3L);
        instances.put(3L,new Instance(old.source(),old.template(),old.greater(),RuleValue.present(Ability.zero().with(Stat.MAX_HP,12)),old.cards(),old.stone()));
        var changedInstance=ModernVipLoadoutTest.withEquipment(source,new Qualified(q.source(),instances,q.catalog(),q.lifetimes()));
        for(var fresh:List.of(changedBase,changedTime,changedRules,changedInstance))
            assertThrows(IllegalArgumentException.class,()->ModernVipCharacterState.prepareOrdinaryEquipment(before,load(fresh,1,Map.of(3L,true),1100,5641),1100));
    }
    @Test void staleBindingsRevisionGapsAndOldPreparedObservationsCannotEnterCombinedState() {
        var source=ModernVipLoadoutTest.source(true,false);var before=start(source,5641);var q=source.input().equipment();
        byte[] bytes=new byte[32];bytes[0]=1;var fp=new ModernEquipmentOverrideStore.Fingerprint(bytes);
        assertNotEquals(before.ledger().binding().fingerprint(),fp);
        var binding=new ModernEquipmentReconciliation.ExpectedBinding(before.ledger().actor().owner(),fp);
        var stored=ModernCombatProfileTest.stored(q,1,Map.of(2L,true)).orElseThrow();
        var foreign=ModernVipLoadout.qualifyForRebuild(source,binding,Optional.of(new ModernEquipmentOverrideStore.Stored(stored.key(),1,
                new ModernEquipmentOverrideStore.Replacement(fp,stored.value().overrides()))),1100,5642);
        assertThrows(IllegalArgumentException.class,()->ModernVipCharacterState.prepareEquip(before,foreign,2,1100));
        assertThrows(IllegalArgumentException.class,()->ModernVipCharacterState.prepareEquip(before,load(source,2,Map.of(2L,true),1100,5642),2,1100));
        assertThrows(IllegalArgumentException.class,()->ModernVipCharacterState.prepareEquip(before,load(source,1,Map.of(2L,true),1099,5642),2,1100));
    }
    @Test void exactFrameActorRevisionTimeAndReplayGuardsHold() {
        var source=ModernVipLoadoutTest.source(true,false);var before=start(source,5641);var c=ModernVipCharacterState.prepareRetainedUnequip(before,1,1100);
        var actor=before.ledger().actor();
        for(var wrong:List.of(new ModernVipLifecycle.Actor(UUID.randomUUID(),actor.owner(),actor.entity(),actor.life()),
                new ModernVipLifecycle.Actor(actor.context(),new TournamentPresence.CharacterKey(42,actor.owner().objectId()),actor.entity(),actor.life()),
                new ModernVipLifecycle.Actor(actor.context(),actor.owner(),actor.entity()+1,actor.life()),
                new ModernVipLifecycle.Actor(actor.context(),actor.owner(),actor.entity(),actor.life()+1)))
            assertThrows(IllegalArgumentException.class,()->ModernVipCharacterState.acceptAt(c,before,wrong,0,1100,50,20));
        assertThrows(IllegalArgumentException.class,()->ModernVipCharacterState.acceptAt(c,before,actor,1,1100,50,20));
        assertThrows(IllegalArgumentException.class,()->accept(c,before,1099,50,20));
        assertThrows(IllegalArgumentException.class,()->accept(c,start(source,5641),1100,50,20));
        var after=accept(c,before,1200,50,20);assertThrows(IllegalArgumentException.class,()->accept(c,after,1200,50,20));
        assertThrows(UnsupportedOperationException.class,()->after.leases().clear());
    }
    static ModernVipCharacterState start(ModernPhysicalCombat.Source source,long shape) {
        var binding=ModernCombatProfileTest.binding(source.input().equipment());
        return ModernVipCharacterState.start(new ModernVipLifecycle.Actor(UUID.randomUUID(),binding.key(),1,0),source,binding,Optional.empty(),1000,shape);
    }
    static ModernVipLoadout load(ModernPhysicalCombat.Source source,long rev,Map<Long,Boolean> flags,long now,long shape) {
        var q=source.input().equipment();return ModernVipLoadout.qualifyForRebuild(source,ModernCombatProfileTest.binding(q),ModernCombatProfileTest.stored(q,rev,flags),now,shape);
    }
    static ModernVipCharacterState accept(ModernVipCharacterState.Candidate c,ModernVipCharacterState before,long now,long hp,long mp) {
        return ModernVipCharacterState.acceptAt(c,before,before.ledger().actor(),before.ledger().equipmentRevision(),now,hp,mp);
    }
    static boolean flag(ModernVipCharacterState s,long id) {return s.profile().character().equipped().stream().filter(i->i.objectId()==id).findFirst().orElseThrow().equipped();}
    static void assertConsistent(ModernVipCharacterState s) {
        assertEquals(s.ledger().total(),s.profile().vip().total());assertEquals(s.ledger().equipmentRevision(),s.profile().character().equipmentRevision());
        assertEquals(s.ledger().icons(),s.profile().vip().icons());assertEquals(s.ledger().shape(),s.profile().vip().shape());
        var p=s.profile().character();var base=p.source().persisted();
        assertEquals(base.charisma()+s.ledger().total().get(CHA),p.effective().charisma());
        assertEquals(ModernCharacterArmor.intrinsic(p.source().character().characterClass(),p.source().character().level(),base.originalDexterity())
                +p.equipmentEvaluation().total(Stat.AC),p.effective().armorClass());
        assertEquals(s.profile().vip().validUntilMillis(),p.validUntilMillis());assertFalse(p.attackImplemented());
    }
    static ModernPhysicalCombat.Source withArmor(ModernPhysicalCombat.Source source,boolean equipped) {
        var armor=ModernEquipmentEvaluatorTest.instance(3,500,ModernCharacterData.ItemKind.ARMOR,equipped,0,"armor",List.of(),
                Ability.zero().with(Stat.MAX_HP,40),Ability.zero(),ModernEquipmentEvaluatorTest.emptyCards(),Stone.none(),TemplatePhysical.zero());
        var items=new ArrayList<>(source.input().equipment().items().values());items.add(armor);
        var base=ModernPhysicalCombatTest.source(7,0,30,items);var q=base.input().equipment();var c=q.catalog();var rules=new HashMap<>(c.lookups());
        rules.putAll(source.input().equipment().catalog().lookups());
        q=new Qualified(q.source(),q.items(),new Catalog(7,"catalog",rules,c.verifiedSetIds(),c.sets(),c.combos(),VipTableState.VERIFIED_PRESENT),q.lifetimes());
        var policies=new HashMap<>(base.templates());policies.putAll(source.templates());
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(q,true,Map.of()),base.growth(),policies,java.util.Set.of(),null,null,null,null,null,source.vipCatalog());
    }
}
