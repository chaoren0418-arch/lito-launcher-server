package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ModernBattleEngineAmmoTest {
    static final class Fixture implements AutoCloseable {
        final ModernBattleEngineBotTest.RoomFixture room;final ModernRooms.FrozenPreparation ticket;
        final List<ModernBattleEngine.PreparedMember> prepared;final ModernBattleEngine engine;
        final AtomicLong clock=new AtomicLong(1_000_000_000L);final AtomicInteger draws=new AtomicInteger();
        long jump;int randomMode;boolean miss;
        Runnable onRandom=()->{};
        Fixture()throws Exception{this(0,2,7,false,0);}
        Fixture(int bots,long quantity,long small,boolean settlement,long countdown)throws Exception {
            room=new ModernBattleEngineBotTest.RoomFixture(bots==0?2:1,bots);ticket=room.capture();var members=new ArrayList<ModernBattleEngine.PreparedMember>();
            for(var p:room.players) {
                boolean self=p.scope().sourceId()==7;
                var base=ModernPhysicalCombatTest.fixtureSource(p.scope().sourceId(),p.row(),100,30,0);
                var source=ModernCombatTestPeer.ammoSource(ModernCombatTestPeer.weightSource(base),self);
                var fighter=ModernPhysicalCombatTest.fighter(source);
                var arrow=ModernCombatTestPeer.arrow(40744,0,20);var t=arrow.template();
                arrow=ModernShopCatalog.Offer.fromSource(40744,0,20,20,new ModernShopCatalog.Template(t.kind(),t.nameToken(),t.inventoryGraphic(),t.groundGraphic(),t.weight(),t.etc(),new ModernShopCatalog.Arrow(small,6,-1,1)));
                var stocks=self?List.of(new ModernShopAuthority.Stack(arrow,quantity),new ModernShopAuthority.Stack(ModernCombatTestPeer.arrow(40746,1,30),2)):List.<ModernShopAuthority.Stack>of();
                members.add(new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.row(),ModernBattleEngineBotTest.cell(10+members.size(),10),200_000_000L,
                        fighter,1_000_000_000L,null,0,false,stocks,ModernInventoryWeight.qualify(fighter.profile(),100),true,true));
            }
            prepared=List.copyOf(members);
            engine=new ModernBattleEngine(ticket,room.binding,prepared,room.bots(ticket),new ModernBattleEngine.Limits(2000,countdown,60000),clock::get,()->1000,
                    bound->{onRandom.run();int n=draws.incrementAndGet();if(jump!=0){clock.addAndGet(jump);jump=0;}return randomMode==1?-1:randomMode==2?bound:miss&&(n==2||n==3)?0:bound-1;},settlement);
        }
        ModernBattleEngine.Snapshot ack(int i){var p=prepared.get(i);return engine.ackLoaded(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),room.map.digest());}
        ModernBattleEngine.Snapshot active(){for(int i=0;i<prepared.size();i++)ack(i);return engine.tick();}
        ModernBattleEngine.AttackResult attack(long seq){var p=prepared.get(0);return engine.attack(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),seq,2);}
        ModernAmmoProtocol.Request request(long seq,long id){var p=prepared.get(0);return new ModernAmmoProtocol.Request(p.scope(),ModernAmmoProtocol.Action.SELECT,ticket.matchId(),ticket.generation(),seq,id);}
        ModernBattleEngine.AmmoResult select(long seq,long id){return engine.ammo(prepared.get(0).owner(),request(seq,id));}
        @Override public void close(){engine.close();room.rooms.releasePreparation(ticket);room.close();}
    }
    @Test void loadingCountdownAndTerminalSelectionNeverConsumeOrChangeLedger()throws Exception {
        try(var f=new Fixture(0,2,7,false,3_000_000_000L)) {
            var loading=f.engine.tick();assertEquals(ModernAmmoProtocol.Reason.NOT_ACTIVE,f.select(99,40746).reason());assertEquals(loading,f.engine.tick());
            var countdown=f.active();assertEquals(ModernBattleEngine.Phase.COUNTDOWN,countdown.phase());
            assertEquals(ModernAmmoProtocol.Reason.NOT_ACTIVE,f.select(99,40746).reason());assertEquals(countdown,f.engine.tick());
            f.clock.addAndGet(3_000_000_000L);assertEquals(ModernAmmoProtocol.Reason.NONE,f.select(1,40746).reason());
            f.clock.addAndGet(60_000_000_000L);var terminal=f.engine.tick();assertEquals(ModernAmmoProtocol.Reason.NOT_ACTIVE,f.select(99,40744).reason());assertEquals(terminal,f.engine.tick());
        }
    }
    @Test void hitCommitsArrowWeightAttackAndHpInOneRevisionWithoutPotionUse()throws Exception {
        try(var f=new Fixture()) {
            var before=f.active();var result=f.attack(1);var after=result.snapshot();assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,result.status());
            assertEquals(before.revision()+1,after.revision());assertEquals(after.revision(),after.ammo().get(0).shotRevision());
            assertEquals(after.revision(),after.vitals().get(0).attackRevision());assertEquals(after.revision(),after.vitals().get(1).hurtRevision());
            assertEquals(1,after.ammo().get(0).selected().remaining());assertTrue(after.vitals().get(1).hp()<100);
            assertEquals(before.items(),after.items());assertEquals(0,after.items().get(0).useCount());assertEquals(2,after.weights().get(0).snapshot().units().size());
            assertEquals(after, f.engine.tick());
        }
    }
    @Test void missStillRollsDamageAndDebitsButDoesNotCreateHurt()throws Exception {
        try(var f=new Fixture()) {
            f.miss=true;var before=f.active();var result=f.attack(1);var after=result.snapshot();
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,result.status());assertEquals(4,f.draws.get());
            assertFalse(after.ammo().get(0).hit());assertEquals(1,after.ammo().get(0).shotCount());assertEquals(before.vitals().get(1),after.vitals().get(1));
            assertEquals(after.revision(),after.vitals().get(0).attackRevision());assertEquals(0,after.vitals().get(1).hurtRevision());
        }
    }
    @Test void exhaustionDoesNotAutoSelectAndNoAmmoConsumesOnlyAttackSequence()throws Exception {
        try(var f=new Fixture(0,1,7,false,0)) {
            f.active();f.attack(1);f.clock.addAndGet(1_000_000_000L);var before=f.engine.tick();int draws=f.draws.get();var denied=f.attack(2);
            assertEquals(ModernBattleEngine.AttackStatus.NO_AMMO,denied.status());assertEquals(draws,f.draws.get());assertEquals(before.ammo(),denied.snapshot().ammo());
            assertEquals(before.weights(),denied.snapshot().weights());assertEquals(before.vitals().get(1),denied.snapshot().vitals().get(1));
            assertEquals(2,denied.snapshot().vitals().get(0).attackSequence());assertEquals(ModernAmmoProtocol.Reason.NONE,f.select(1,40746).reason());
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(3).status());
        }
    }
    @Test void independentSelectionSequenceRejectsReplayAndDenialNeverReplacesHistory()throws Exception {
        try(var f=new Fixture()) {
            f.active();f.attack(1);var shot=f.engine.tick().ammo().get(0);var denied=f.select(1,40010);
            assertEquals(ModernAmmoProtocol.Reason.UNSUPPORTED,denied.reason());assertEquals(shot.shotRevision(),denied.snapshot().ammo().get(0).shotRevision());
            assertEquals(shot.stocks(),denied.snapshot().ammo().get(0).stocks());assertEquals(1,denied.snapshot().ammo().get(0).requestSequence());
            assertThrows(SecurityException.class,()->f.select(1,40746));assertEquals(ModernAmmoProtocol.Reason.NONE,f.select(2,40746).reason());
        }
    }
    @Test void invalidRandomAndLateRandomNeverPartiallyDebitArrowWeightOrHp()throws Exception {
        for(int mode=0;mode<3;mode++)try(var f=new Fixture()) {
            var before=f.active();f.randomMode=mode;f.jump=60_000_000_000L;var result=f.attack(1);
            assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE,result.status());assertEquals(ModernBattleEngine.Phase.ABORTED,result.snapshot().phase());
            assertEquals(before.ammo(),result.snapshot().ammo());assertEquals(before.weights(),result.snapshot().weights());assertEquals(before.vitals(),result.snapshot().vitals());
        }
        try(var f=new Fixture()){var before=f.active();f.randomMode=1;var result=f.attack(1);assertEquals(ModernBattleEngine.AttackStatus.NUMERIC,result.status());
            assertEquals(before.ammo(),result.snapshot().ammo());assertEquals(before.vitals().get(1),result.snapshot().vitals().get(1));assertEquals(1,result.snapshot().vitals().get(0).attackSequence());}
    }
    @Test void bowHitsTypedDummyWithExplicitZeroErAndSettlementNeverMakesBotACharacter()throws Exception {
        try(var f=new Fixture(1,2,10000,true,0)) {
            f.active();var result=f.attack(1);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,result.status());
            assertEquals(ModernBattleEngine.Phase.FINISHED,result.snapshot().phase());assertEquals(1,result.snapshot().ammo().get(0).shotCount());
            assertEquals(ModernAmmo.State.initial(2,List.of()),result.snapshot().ammo().get(1));
            var event=result.snapshot().result();assertEquals(2,event.participants().size());assertEquals(1,event.participants().stream().filter(p->p.identity().isHuman()).count());
            assertEquals(ModernMatchSettlement.Verdict.WIN,event.participants().get(0).verdict());assertEquals(ModernMatchSettlement.Verdict.LOSS,event.participants().get(1).verdict());
        }
    }
    @Test void ordinaryBowRejectsSelectionChangesDuringRandomWithoutArmorDependency()throws Exception {
        try(var f=new Fixture()) {
            var before=f.active();f.onRandom=()->{f.onRandom=()->{};assertEquals(ModernAmmoProtocol.Reason.NONE,f.select(1,40746).reason());};
            var out=f.attack(1);assertEquals(ModernBattleEngine.AttackStatus.STALE_EFFECT,out.status());
            assertEquals(before.ammo().get(0).stocks(),out.snapshot().ammo().get(0).stocks());
            assertEquals(before.vitals().get(1),out.snapshot().vitals().get(1));assertEquals(0,out.snapshot().ammo().get(0).shotCount());
        }
    }
    @Test void ordinaryBowCannotRewindTwoNestedAttackSequences()throws Exception {
        try(var f=new Fixture()) {
            f.active();f.onRandom=()->{f.onRandom=()->{};assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(1).status());
                assertEquals(ModernBattleEngine.AttackStatus.TOO_FAST,f.attack(2).status());};
            assertThrows(SecurityException.class,()->f.attack(1));
            assertEquals(2,f.engine.tick().vitals().get(0).attackSequence());assertEquals(1,f.engine.tick().ammo().get(0).shotCount());
        }
    }
}
