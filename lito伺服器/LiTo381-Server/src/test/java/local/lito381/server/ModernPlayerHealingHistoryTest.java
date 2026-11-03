package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernPlayerHealingHistory.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernPlayerHealingHistoryTest {
    static final ModernWorldMap.Cell CELL=new ModernWorldMap.Cell(10,10);
    static ModernEquipmentHistory.State initial(int source,long entity,boolean canCast) {
        var actor=ModernPlayerHealingTransactionTest.actor(entity,source,40);
        var id=new ModernEquipmentHistory.Identity(actor.world(),new ModernRooms.Scope(source,3333,source==7?1:991,0),actor.participant());
        return new ModernEquipmentHistory.State(id,1,actor.resources(),
                canCast?Map.of(ModernUseHistory.Lane.TELEPORT,ModernUseHistory.Spell.initial(0,1,1)):Map.of(),
                ModernUseHistory.Items.initial(0,1,0),Map.of(),
                new Entry(Incoming.initial(id.world(),id.actor()),canCast?Outgoing.initial(0,1,4):null));
    }
    static ModernEquipmentHistory.State resources(ModernEquipmentHistory.State before,ModernVitalBalance.State resources) {
        return new ModernEquipmentHistory.State(before.identity(),resources.revision(),resources,before.spells(),before.items(),before.targets(),before.healing());
    }
    static ModernEquipmentHistory.State healing(ModernEquipmentHistory.State before,Entry entry) {
        return new ModernEquipmentHistory.State(before.identity(),before.worldRevision(),before.resources(),before.spells(),before.items(),before.targets(),entry);
    }
    static ModernPlayerHealingTransaction.Actor actor(ModernEquipmentHistory.State history) {
        return new ModernPlayerHealingTransaction.Actor(history.identity().world(),history.identity().actor(),history.resources(),CELL,0,ModernPlayerHealing.Effects.NONE);
    }
    static ModernPlayerHealingTransaction.Prepared plan(ModernEquipmentHistory.State caster,ModernEquipmentHistory.State target,long sequence) {
        var p=ModernPlayerHealingTest.profile();var r=caster.resources();
        p=new ModernPlayerHealing.Profile(p.key(),p.character(),r.equipmentRevision(),r.vitals().maximumHp(),r.vitals().maximumMp(),
                new ModernHonorHealing.Profile(true,2,caster.healing().outgoing().spending().current().cost(),0));
        var state=new ModernPlayerHealingTransaction.State(caster.worldRevision(),caster.identity().source(),actor(caster),actor(target),
                p,ModernPlayerHealingTest.skills(p),caster.healing().outgoing().spending().current(),sequence-1,0);
        return ModernPlayerHealingTransaction.prepare(state,caster.worldRevision()+1,sequence,b->3);
    }
    static ModernPlayerHealingTransaction.Complete cast(ModernEquipmentHistory.State caster,ModernEquipmentHistory.State target,long sequence) {
        return ModernPlayerHealingTransaction.complete(plan(caster,target,sequence),caster,target);
    }
    static Impact event(Impact r,long revision,long sequence,ModernTargetHistory.Participant caster) {
        var u=r.use();
        return new Impact(r.world(),caster,new ModernUseHistory.Cast(revision,sequence,u.before(),u.cost(),u.mpAfter()),
                r.targetEffectRevision(),r.effects(),r.baseAmount(),r.adjustedAmount(),r.target());
    }
    @Test void selfCommitHasTwoLinkedHistoriesOneResourceRevisionAndNoLegacyProjection() {
        var before=initial(7,1,true);var result=cast(before,before,1);var after=result.caster();
        assertSame(after,result.target());assertEquals(48,after.resources().vitals().hp());assertEquals(96,after.resources().vitals().mp());
        assertEquals(1,after.healing().incoming().received());assertEquals(8,after.healing().incoming().restored());
        assertEquals(after.healing().outgoing().last().impact(),after.healing().incoming().last());
        for(int schema=1;schema<=4;schema++) {final int version=schema;
            assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.encode(after,version));
            assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.encode(before,version));}
        assertThrows(IllegalArgumentException.class,()->healing(after,null));
        assertEquals(40,before.resources().vitals().hp());assertEquals(0,before.healing().incoming().received());
    }
    @Test void crossSourceSameObjidPaysOnlyCasterAndRecipientHasNoForeignSelectionEpoch() {
        var a=initial(7,1,true);var b=initial(42,2,false);var result=cast(a,b,1);
        assertEquals(40,result.caster().resources().vitals().hp());assertEquals(48,result.target().resources().vitals().hp());
        assertEquals(96,result.caster().resources().vitals().mp());assertEquals(100,result.target().resources().vitals().mp());
        assertNull(result.target().healing().outgoing());assertEquals(991,result.target().identity().source().epoch());
        var receipt=result.caster().healing().outgoing().last();
        var otherEpoch=new ModernPlayerHealingTransaction.Receipt(receipt.world(),new ModernRooms.Scope(7,3333,88,0),receipt.caster(),
                receipt.use(),receipt.targetEffectRevision(),receipt.effects(),receipt.baseAmount(),receipt.adjustedAmount(),receipt.target());
        assertNotEquals(receipt,otherEpoch);assertEquals(receipt.impact(),otherEpoch.impact());
        assertTrue(java.util.Arrays.stream(Impact.class.getRecordComponents()).noneMatch(c->c.getType()==ModernRooms.Scope.class));
        assertFalse(result.target().healing().incoming().toString().contains("3333"));
    }
    @Test void potionRegenerationOrdinaryCastEquipmentClipAndRespawnKeepDistinctExactLedgers() {
        var a=initial(7,1,true);a=cast(a,a,1).caster();
        a=a.cast(ModernUseHistory.Lane.TELEPORT,3,2,CELL,List.of());
        var r=a.resources();var item=a.items().use(4,1,40010,new ModernUseHistory.Anchor(0,1,r.vitals(),10,10),10);
        a=new ModernEquipmentHistory.State(a.identity(),4,r.restore(4,10),a.spells(),item,a.targets(),a.healing());
        a=resources(a,a.resources().regenerate(5,2,0,3,0));
        var profile=new ModernEquipmentHistory.Profile(1,50,90,Map.of(ModernUseHistory.Lane.TELEPORT,2),3L);
        var before=a;assertThrows(IllegalArgumentException.class,()->before.reprofile(6,profile));
        a=a.reprofile(6,profile,0);assertTrue(ModernEquipmentHistory.followsReprofile(before,a));
        assertEquals(before.healing().incoming(),a.healing().incoming());assertEquals(before.healing().outgoing().last(),a.healing().outgoing().last());
        a=resources(a,a.resources().damage(7,50).respawn(8,20,0));
        a=cast(a,a,3).caster();r=a.resources();
        assertEquals(28,r.vitals().hp());assertEquals(90,r.vitals().mp());assertEquals(26,r.flows().restored());
        assertEquals(5,r.flows().spent());assertEquals(10,r.flows().hpClipped());assertEquals(8,r.flows().mpClipped());
        assertEquals(2,a.healing().incoming().received());assertEquals(16,a.healing().incoming().restored());
        assertEquals(10,a.items().restored());assertEquals(2,a.healing().outgoing().last().target().before().life());
        System.out.println("player-healing-history-v1:"+a.worldRevision()+":"+r.equipmentRevision()+":"+r.life()+":"+r.vitals().hp()+":"+r.vitals().mp()
                +":"+r.flows().restored()+":"+r.flows().spent()+":"+r.flows().hpClipped()+":"+r.flows().mpClipped()+":"+a.healing().incoming().received()+":"+a.healing().outgoing().last().use().sequence());
    }
    @Test void incomingAllowsDifferentCasterSequenceButRejectsReplayRebindingAndOverflow() {
        var a=initial(7,1,true);var b=initial(42,2,false);var impact=plan(a,b,1).receipt().impact();
        var first=event(impact,10,5,impact.caster());var incoming=b.healing().incoming().receive(first);
        var other=event(impact,11,1,new ModernTargetHistory.Participant(3,ModernParticipantId.human(43,3333)));
        assertEquals(16,incoming.receive(other).restored());
        assertThrows(IllegalArgumentException.class,()->incoming.receive(first));
        assertThrows(IllegalArgumentException.class,()->incoming.receive(event(impact,11,5,impact.caster())));
        assertThrows(IllegalArgumentException.class,()->incoming.receive(event(impact,11,6,new ModernTargetHistory.Participant(3,impact.caster().identity()))));
        assertThrows(IllegalArgumentException.class,()->new Incoming(new ModernTargetHistory.World(1,2,4),b.identity().actor(),1,8,first));
        var overflow=new Incoming(impact.world(),b.identity().actor(),Long.MAX_VALUE-3,Long.MAX_VALUE,event(impact,Long.MAX_VALUE-2,5,impact.caster()));
        assertThrows(ArithmeticException.class,()->overflow.receive(event(impact,Long.MAX_VALUE-1,1,other.caster())));
    }
    @Test void fullHealthErosionAndBotStillCountZeroOrRealIncomingEvents() {
        var state=ModernPlayerHealingTransactionTest.state(false);var target=state.target();
        var bot=new ModernPlayerHealingTransaction.Actor(target.world(),new ModernTargetHistory.Participant(2,ModernParticipantId.bot(7,3333)),
                target.resources(),target.cell(),0,target.effects());
        var receipt=ModernPlayerHealingTransaction.prepare(ModernPlayerHealingTransactionTest.target(state,bot),2,1,b->3).receipt();
        var incoming=Incoming.initial(bot.world(),bot.participant()).receive(receipt.impact());
        assertEquals(8,incoming.restored());assertFalse(incoming.recipient().identity().isHuman());
        for(boolean erosion:new boolean[]{false,true}) {
            var t=ModernPlayerHealingTransactionTest.effects(ModernPlayerHealingTransactionTest.actor(2,42,erosion?40:100),new ModernPlayerHealing.Effects(true,false,false,erosion));
            var p=ModernPlayerHealingTransaction.prepare(ModernPlayerHealingTransactionTest.target(state,t),2,1,b->3);
            var h=Incoming.initial(t.world(),t.participant()).receive(p.receipt().impact());
            assertEquals(1,h.received());assertEquals(0,h.restored());assertFalse(p.targetEffects().waterLife());
        }
    }
    @Test void orphanedSelfReceiptsAndMissingOrInflatedAccountingAreRejected() {
        var a=initial(7,1,true);var after=cast(a,a,1).caster();var h=after.healing();
        assertThrows(IllegalArgumentException.class,()->healing(after,new Entry(a.healing().incoming(),h.outgoing())));
        assertThrows(IllegalArgumentException.class,()->healing(after,new Entry(h.incoming(),null)));
        assertThrows(IllegalArgumentException.class,()->new Incoming(h.incoming().world(),h.incoming().recipient(),1,9,h.incoming().last()));
        var b=initial(42,2,false);var pair=cast(a,b,1);var target=pair.target();
        assertThrows(IllegalArgumentException.class,()->healing(target,b.healing()));
        assertThrows(IllegalArgumentException.class,()->new ModernEquipmentHistory.State(after.identity(),2,after.resources(),after.spells(),null,after.targets()));
    }
    @Test void globalCasterSequenceCannotBeReusedByOrdinaryOrPlayerHealingCast() {
        var a=initial(7,1,true);var healed=cast(a,a,1).caster();
        assertThrows(IllegalArgumentException.class,()->healed.cast(ModernUseHistory.Lane.TELEPORT,3,1,CELL,List.of()));
        var ordinary=a.cast(ModernUseHistory.Lane.TELEPORT,2,1,CELL,List.of());
        var prepared=plan(ordinary,ordinary,1);
        assertThrows(IllegalArgumentException.class,()->ModernPlayerHealingTransaction.complete(prepared,ordinary,ordinary));
        assertEquals(2,cast(ordinary,ordinary,2).caster().healing().outgoing().last().use().sequence());
    }
    @Test void completeCandidateRejectsChangedResourceHistoryWorldOrEpoch() {
        var a=initial(7,1,true);var b=initial(42,2,false);var p=plan(a,b,1);
        var changed=resources(b,b.resources().regenerate(2,1,0,0,0));
        assertThrows(IllegalArgumentException.class,()->ModernPlayerHealingTransaction.complete(p,a,changed));
        var id=a.identity();var epoch=new ModernEquipmentHistory.State(new ModernEquipmentHistory.Identity(id.world(),new ModernRooms.Scope(7,3333,2,0),id.actor()),1,
                a.resources(),a.spells(),a.items(),a.targets(),a.healing());
        assertThrows(IllegalArgumentException.class,()->ModernPlayerHealingTransaction.complete(p,epoch,b));
        assertEquals(0,a.healing().outgoing().spending().current().casts());assertEquals(0,b.healing().incoming().received());
    }
    @Test void partialTransactionCannotClaimUnaccountedSpendingOrFutureResourceReceipt() {
        var before=ModernPlayerHealingTransactionTest.state(false);var ledger=before.spending().cast(2,1);
        assertThrows(IllegalArgumentException.class,()->new ModernPlayerHealingTransaction.State(3,before.scope(),before.caster(),before.target(),before.profile(),before.skills(),ledger,1,0));
        var a=before.caster();var unpaid=new ModernPlayerHealingTransaction.Actor(a.world(),a.participant(),a.resources().spend(2,0),a.cell(),0,a.effects());
        assertThrows(IllegalArgumentException.class,()->new ModernPlayerHealingTransaction.State(3,before.scope(),unpaid,before.target(),before.profile(),before.skills(),ledger,1,0));
    }
    @Test void zeroRecoveryItemCannotReuseTheSameWorldEventAsHealing() {
        var a=initial(7,1,true);var after=cast(a,a,1).caster();
        var full=new ModernUseHistory.Anchor(0,1,new ModernEquipmentVitals.Vitals(100,100,100,100),10,10);
        var item=ModernUseHistory.Items.initial(0,1,0).use(2,1,40010,full,10);assertEquals(0,item.restored());
        assertThrows(IllegalArgumentException.class,()->new ModernEquipmentHistory.State(after.identity(),2,after.resources(),after.spells(),item,after.targets(),after.healing()));
    }
    @Test void impossiblePriorCostsRecoveryTotalsAndRegressingTargetEffectRevisionAreRejected() {
        var a=initial(7,1,true);var b=initial(42,2,false);var r=plan(a,b,1).receipt();var impact=r.impact();
        assertThrows(IllegalArgumentException.class,()->new Incoming(impact.world(),impact.recipient(),2,131069,event(impact,10,5,impact.caster())));
        var use=new ModernUseHistory.Cast(3,2,r.use().before(),4,96);
        var old=new ModernPlayerHealingTransaction.Receipt(r.world(),r.source(),r.caster(),use,0,r.effects(),8,8,r.target());
        var ledger=new ModernSkillSpending.Ledger(1,4,0,2,9,2,9,3,2);
        var spell=new ModernUseHistory.Spell(ledger,use);
        assertThrows(IllegalArgumentException.class,()->new Outgoing(spell,old));
        var first=new Impact(impact.world(),impact.caster(),event(impact,10,5,impact.caster()).use(),5,impact.effects(),8,8,impact.target());
        var incoming=b.healing().incoming().receive(first);
        assertThrows(IllegalArgumentException.class,()->incoming.receive(event(impact,11,6,impact.caster())));
    }
}
