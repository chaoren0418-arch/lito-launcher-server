package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

/** Internal aggregate tests; deliberately NOT an engine/TLS/UI acceptance test. */
class ModernSlowTransactionTest {
    static final ModernUseHistory.Lane LANE=ModernUseHistory.Lane.SLOW;
    static final ModernWorldMap.Cell ORIGIN=new ModernWorldMap.Cell(10,10),CELL=new ModernWorldMap.Cell(11,10);
    static ModernSlowTransaction.State initial(long mp,long quantity) {
        var seed=ModernSpellMaterialsTest.state(1);
        var materials=ModernSpellMaterials.State.initial(seed.world(),seed.scope(),seed.caster(),quantity==0?List.of():
                List.of(new ModernShopAuthority.Stack(ModernSpellMaterialsTest.offer(2000),quantity)));
        var id=new ModernEquipmentHistory.Identity(materials.world(),materials.scope(),materials.caster());
        var h=new ModernEquipmentHistory.State(id,10,ModernVitalBalance.State.initial(1,0,
                new ModernEquipmentVitals.Vitals(100,100,mp,mp)),Map.of(LANE,ModernUseHistory.Spell.initial(0,1,20)),null,Map.of());
        return new ModernSlowTransaction.State(h,ModernSpellRuntime.State.initial(id,h.spells().keySet(),1,0),materials);
    }
    static ModernSlowCatalog.Snapshot catalog(int source) {
        return new ModernSlowCatalog.Snapshot(new TournamentPresence.CharacterKey(source,3333),ModernSlowCatalog.Status.QUALIFIED,OptionalLong.of(0));
    }
    static ModernSlow.Profile profile(ModernSlowTransaction.State s) {
        return new ModernSlow.Profile(catalog(7).key(),s.history().resources().equipmentRevision(),s.history().spells().get(LANE).current().cost());
    }
    static ModernInventoryWeight.Basis basis(long gear,double maximum) {
        var b=ModernSpellMaterialsTest.basis(7,0,maximum);
        return new ModernInventoryWeight.Basis(b.source(),gear,b.sourceGearWeight(),b.maximumWeight(),b.sourceRate(),b.validUntilMillis());
    }
    static List<ModernInventoryWeight.Unit> units(ModernSlowTransaction.State s) {
        return s.materials().stocks().stream().map(x->new ModernInventoryWeight.Unit(x.itemId(),x.facts().unitWeight())).toList();
    }
    static ModernSlowReceipt player(ModernSlowTransaction.State s,int roll,long immune,ModernTimedEffects.State before) {
        long rev=s.history().worldRevision()+1;
        var after=roll<=45&&immune==0?ModernTimedEffects.applySlow(before,0,rev):before;
        return new ModernSlowReceipt(0,45,roll,immune,new ModernSlowReceipt.Player(
                new ModernTargetHistory.Participant(2,ModernParticipantId.human(42,3333)),1,0,CELL,before,after));
    }
    static ModernSlowTransaction.Prepared prepare(ModernSlowTransaction.State s,ModernSlowReceipt effect) {
        return ModernSlowTransaction.prepare(s,profile(s),catalog(7),s.history().worldRevision()+1,s.runtime().sequence()+1,
                s.runtime().sequence()*1_000_000_000L,ORIGIN,effect,basis(profile(s).equipmentRevision(),1000),List.of(),List.of(),units(s));
    }
    @Test void hitMissAndImmuneEachDebitExactlyOnceAndKeepSourceNamespaces() {
        for(int roll:new int[]{1,45,46,100})for(long immunity:new long[]{0,1,Long.MAX_VALUE}) {
            var s=initial(100,2);var before=ModernTimedEffects.initial(2);var receipt=player(s,roll,immunity,before);
            var p=prepare(s,receipt);var after=p.after();
            assertEquals(80,after.history().resources().vitals().mp());assertEquals(20,after.history().resources().flows().spent());
            assertEquals(1,after.materials().stocks().get(0).remaining());assertEquals(1,after.materials().uses());
            assertEquals(2,p.weight().view().suppliesWeight());assertEquals(1,after.runtime().sequence());
            assertSame(receipt,after.runtime().receipts().get(LANE).detail());assertTrue(after.history().targets().get(LANE).last().targets().isEmpty());
            var outcome=roll>45?ModernSlowReceipt.Outcome.MISS:immunity>0?ModernSlowReceipt.Outcome.EQUIPMENT_IMMUNE:ModernSlowReceipt.Outcome.APPLIED;
            assertEquals(outcome,receipt.outcome());var target=(ModernSlowReceipt.Player)receipt.target();
            if(outcome==ModernSlowReceipt.Outcome.APPLIED)assertEquals(64_000_000_000L,target.after().slowUntilNanos());
            else assertEquals(before,target.after());
            assertEquals(100,s.history().resources().vitals().mp());assertEquals(2,s.materials().stocks().get(0).remaining());
            assertTrue(p.matchesCaster(s));assertFalse(p.matchesCaster(after));
            assertEquals(42,target.participant().identity().sourceId());assertEquals(7,after.materials().scope().sourceId());
        }
    }
    @Test void hasteCancellationIsAPaidNeutralizationAndPreservesOtherBuffs() {
        var s=initial(100,2);
        var brave=ModernTimedEffects.apply(ModernTimedEffects.initial(2),ModernTimedEffects.Kind.BRAVE,0,2,3);
        var wisdom=ModernTimedEffects.apply(brave,ModernTimedEffects.Kind.WISDOM,0,3,3);
        var haste=ModernTimedEffects.applyHaste(wisdom,0,4);
        var p=prepare(s,player(s,1,0,haste));assertEquals(ModernSlowReceipt.Outcome.NEUTRALIZED,p.effect().outcome());
        var neutral=((ModernSlowReceipt.Player)p.effect().target()).after();assertEquals(0,neutral.hasteUntilNanos());assertEquals(0,neutral.slowUntilNanos());
        assertEquals(wisdom.braveUntilNanos(),neutral.braveUntilNanos());assertEquals(wisdom.wisdomUntilNanos(),neutral.wisdomUntilNanos());
        var second=prepare(p.after(),player(p.after(),1,0,neutral));assertEquals(ModernSlowReceipt.Outcome.APPLIED,second.effect().outcome());
        assertEquals(60,second.after().history().resources().vitals().mp());assertFalse(second.after().materials().hasSlowMaterial());
    }
    @Test void noMpNoMaterialsStaleSequenceWrongPriceAndWrongSourceProduceNoCommit() {
        for(var s:List.of(initial(19,2),initial(100,0))) {
            assertThrows(IllegalArgumentException.class,()->prepare(s,player(s,1,0,ModernTimedEffects.initial(2))));
            assertEquals(0,s.runtime().sequence());assertEquals(0,s.materials().uses());assertEquals(0,s.history().resources().flows().spent());
        }
        var s=initial(100,2);var r=player(s,1,0,ModernTimedEffects.initial(2));
        for(long seq:new long[]{0,2,Long.MAX_VALUE})assertThrows(IllegalArgumentException.class,()->ModernSlowTransaction.prepare(s,profile(s),catalog(7),11,seq,0,ORIGIN,r,basis(0,1000),List.of(),List.of(),units(s)));
        for(long rev:new long[]{10,12})assertThrows(IllegalArgumentException.class,()->ModernSlowTransaction.prepare(s,profile(s),catalog(7),rev,1,0,ORIGIN,r,basis(0,1000),List.of(),List.of(),units(s)));
        for(var profile:List.of(new ModernSlow.Profile(catalog(7).key(),1,20),new ModernSlow.Profile(catalog(7).key(),0,19),new ModernSlow.Profile(catalog(42).key(),0,20)))
            assertThrows(IllegalArgumentException.class,()->ModernSlowTransaction.prepare(s,profile,catalog(profile.key().sourceId()),11,1,0,ORIGIN,r,basis(0,1000),List.of(),List.of(),units(s)));
        var unknown=new ModernSlowCatalog.Snapshot(catalog(7).key(),ModernSlowCatalog.Status.QUALIFIED);
        assertThrows(IllegalArgumentException.class,()->ModernSlowTransaction.prepare(s,profile(s),unknown,11,1,0,ORIGIN,r,basis(0,1000),List.of(),List.of(),units(s)));
    }
    @Test void materialAndSpellLedgersCannotBeRecombinedOrResetAfterReprice() {
        var s=initial(100,3);var one=prepare(s,player(s,100,0,ModernTimedEffects.initial(2))).after();
        assertThrows(IllegalArgumentException.class,()->new ModernSlowTransaction.State(one.history(),one.runtime(),s.materials()));
        assertThrows(IllegalArgumentException.class,()->new ModernSlowTransaction.State(s.history(),s.runtime(),one.materials()));
        var h=one.history().reprofile(12,new ModernEquipmentHistory.Profile(1,100,100,Map.of(LANE,17),null));
        var equipped=new ModernSlowTransaction.State(h,one.runtime(),one.materials());
        var second=prepare(equipped,player(equipped,100,0,ModernTimedEffects.initial(2))).after();
        assertEquals(37,second.history().resources().flows().spent());assertEquals(63,second.history().resources().vitals().mp());
        assertEquals(2,second.materials().uses());assertEquals(1,second.materials().stocks().get(0).remaining());
        assertEquals(1,second.materials().lastEquipmentRevision());assertEquals(20,one.history().spells().get(LANE).last().cost());
        assertEquals(17,second.history().spells().get(LANE).last().cost());
    }
    @Test void deniedSequenceIsNotAnAttemptAndCannotBeReplayedAsAnUnpaidCast() {
        var s=initial(100,2);var h=s.history();var denied=new ModernSlowTransaction.State(
                new ModernEquipmentHistory.State(h.identity(),11,h.resources(),h.spells(),h.items(),h.targets()),s.runtime().denied(11,1),s.materials());
        var p=prepare(denied,player(denied,100,0,ModernTimedEffects.initial(2)));
        assertEquals(2,p.after().runtime().sequence());assertEquals(1,p.after().materials().uses());assertEquals(80,p.after().history().resources().vitals().mp());
        assertEquals(0,denied.materials().uses());assertFalse(p.matchesCaster(s));
    }
    @Test void weightChecksOriginalInventoryAndCannotForgetZeroQuantityUnitFacts() {
        var s=initial(100,1);var r=player(s,100,0,ModernTimedEffects.initial(2));
        assertThrows(IllegalArgumentException.class,()->ModernSlowTransaction.prepare(s,profile(s),catalog(7),11,1,0,ORIGIN,r,basis(0,2),List.of(),List.of(),units(s)));
        assertThrows(IllegalArgumentException.class,()->ModernSlowTransaction.prepare(s,profile(s),catalog(7),11,1,0,ORIGIN,r,basis(0,1000),List.of(),List.of(),List.of()));
        var p=prepare(s,r);assertEquals(0,p.weight().view().suppliesWeight());assertEquals(units(s),p.weight().units());
    }
    @Test void npcAndBotReceiptsRetainTypedIdentityWithoutPretendingTheyAreSourceCharacters() {
        var s=initial(100,2);var speed=ModernNpcSpeed.initial();
        for(int roll:new int[]{1,100}) {
            var after=roll==1?ModernNpcSpeed.slow(speed,0,3):speed;
            var target=new ModernSlowReceipt.Npc(new ModernNpcRoster.Id(new ModernNpcRoster.Scope(1,2,3),1),1,0,CELL,2,speed,after);
            var p=prepare(s,new ModernSlowReceipt(0,45,roll,0,target));assertEquals(1,p.after().materials().uses());
            assertThrows(IllegalArgumentException.class,()->new ModernSlowReceipt(0,45,roll,1,target));
        }
        var before=ModernTimedEffects.initial(2);var after=ModernTimedEffects.applySlow(before,0,11);
        var bot=new ModernSlowReceipt.Player(new ModernTargetHistory.Participant(2,ModernParticipantId.bot(7,3333)),1,0,CELL,before,after);
        assertEquals(ModernParticipantId.Kind.SERVER_BOT,((ModernSlowReceipt.Player)prepare(s,new ModernSlowReceipt(0,45,1,0,bot)).effect().target()).participant().identity().kind());
    }
    @Test void forgedEffectProbabilityRangeSelfIdentityAndNpcWorldCannotEnterReceipt() {
        var s=initial(100,2);var before=ModernTimedEffects.initial(2);var good=player(s,1,0,before);var t=(ModernSlowReceipt.Player)good.target();
        for(int probability:new int[]{0,91,100})assertThrows(IllegalArgumentException.class,()->new ModernSlowReceipt(0,probability,1,0,t));
        for(int roll:new int[]{0,101})assertThrows(IllegalArgumentException.class,()->new ModernSlowReceipt(0,45,roll,0,t));
        assertThrows(IllegalArgumentException.class,()->new ModernSlowReceipt(0,45,100,0,t));
        assertThrows(IllegalArgumentException.class,()->new ModernSlowReceipt(0,45,1,1,t));
        var far=new ModernSlowReceipt(0,45,1,0,new ModernSlowReceipt.Player(t.participant(),1,0,new ModernWorldMap.Cell(50,50),before,t.after()));
        assertThrows(IllegalArgumentException.class,()->prepare(s,far));
        var alias=new ModernSlowReceipt(0,45,1,0,new ModernSlowReceipt.Player(new ModernTargetHistory.Participant(2,s.history().identity().actor().identity()),1,0,CELL,before,t.after()));
        assertThrows(IllegalArgumentException.class,()->prepare(s,alias));
        var npc=new ModernSlowReceipt(0,45,1,0,new ModernSlowReceipt.Npc(new ModernNpcRoster.Id(new ModernNpcRoster.Scope(1,2,4),1),1,0,CELL,2,ModernNpcSpeed.initial(),ModernNpcSpeed.slow(ModernNpcSpeed.initial(),0,3)));
        assertThrows(IllegalArgumentException.class,()->prepare(s,npc));
        var current=new ModernTimedEffects.State(2,11,0,0,0,0,0);
        var stale=new ModernSlowReceipt(0,45,100,0,new ModernSlowReceipt.Player(t.participant(),1,0,CELL,current,current));
        assertThrows(IllegalArgumentException.class,()->prepare(s,stale));
    }
    @Test void copiedMaterialOwnershipAndWrongSkillDetailCannotPassTheAggregate() {
        var s=initial(100,2);var m=s.materials();
        var other=ModernSpellMaterials.State.initial(m.world(),new ModernRooms.Scope(42,3333,m.scope().epoch(),0),
                new ModernTargetHistory.Participant(2,ModernParticipantId.human(42,3333)),
                List.of(new ModernShopAuthority.Stack(ModernSpellMaterialsTest.offer(2000),2)));
        assertThrows(IllegalArgumentException.class,()->new ModernSlowTransaction.State(s.history(),s.runtime(),other));
        var event=s.history().cast(LANE,11,1,ORIGIN,List.of());
        assertThrows(IllegalArgumentException.class,()->s.runtime().cast(s.history(),event,LANE,0,ModernSpellRuntime.Ordinary.INSTANCE));
        var selfBefore=ModernTimedEffects.initial(1);
        var self=new ModernSlowReceipt(0,1,1,0,new ModernSlowReceipt.Player(s.history().identity().actor(),1,0,ORIGIN,
                selfBefore,ModernTimedEffects.applySlow(selfBefore,0,11)));
        assertEquals(1,prepare(s,self).after().materials().uses());
        var wrongLife=new ModernSlowReceipt(0,1,1,0,new ModernSlowReceipt.Player(s.history().identity().actor(),2,0,ORIGIN,
                selfBefore,ModernTimedEffects.applySlow(selfBefore,0,11)));
        assertThrows(IllegalArgumentException.class,()->prepare(s,wrongLife));
        var wrongGear=new ModernSlowReceipt(0,1,1,0,new ModernSlowReceipt.Player(s.history().identity().actor(),1,1,ORIGIN,
                selfBefore,ModernTimedEffects.applySlow(selfBefore,0,11)));
        assertThrows(IllegalArgumentException.class,()->prepare(s,wrongGear));
    }
    @Test void existingHistorySchemasAndLegacyProjectionRejectEvenAnUnusedSlowLane() {
        var s=initial(100,2);var after=prepare(s,player(s,100,0,ModernTimedEffects.initial(2))).after();
        for(var state:List.of(s,after)) {
            for(int schema:new int[]{1,2,3})assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.encode(state.history(),schema));
            assertThrows(IllegalArgumentException.class,()->ModernLegacySpellProjection.from(1,0,1000,state.runtime(),state.history().spells()));
        }
        assertEquals(10,LANE.ordinal());assertEquals(9,ModernUseHistory.Lane.HASTE.ordinal());
    }
}
