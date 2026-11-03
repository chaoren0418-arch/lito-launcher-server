package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ModernBattlePresentationTest {
    static ModernBattleEngine.PreparedMember bow(ModernBattleEngine.PreparedMember p) {
        var values=p.fighter().profile().effective();
        var base=ModernBattleEngineHonorTest.source(p.scope().sourceId(),p.character(),values.maximumHp(),values.maximumMp());
        var bow=ModernCombatTestPeer.ammoSource(base,true);
        var source=new ModernPhysicalCombat.Source(bow.input(),bow.growth(),bow.templates(),bow.procCatalogIds(),bow.magicGrowth(),
                bow.magicBows(),bow.weaponArrowSprites(),base.regenGrowth(),base.healingGrowth());
        var fighter=ModernPhysicalCombatTest.fighter(source);var stock=new java.util.ArrayList<>(p.matchItems());
        stock.add(new ModernShopAuthority.Stack(ModernCombatTestPeer.arrow(40744,0,20),10));
        return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),fighter,p.attackIntervalNanos(),
                p.skills(),p.castIntervalNanos(),p.damageMagic(),stock,ModernInventoryWeight.qualify(fighter.profile(),100),true,true,true,p.fireStormEnabled());
    }
    static ModernBattlePresentation.State state(ModernBattleObservation.Capture c,int index) {
        return c.world().actors().get(index).presentation();
    }
    @Test void bothOwnersShareExactShotButOnlyShooterGetsAmmoStocks()throws Exception {
        try(var f=new ModernBattleEngineAmmoTest.Fixture()) {
            f.active();assertEquals(ModernBattlePresentation.State.EMPTY,state(f.engine.serviceObservation(),0));
            f.clock.addAndGet(123_000_000L);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(1).status());
            var c=f.engine.serviceObservation();var p=ModernBattleWireTest.publication(c,null);
            var a=p.audience(f.room.players.get(0).scope());var b=p.audience(f.room.players.get(1).scope());
            var s=a.world().actors().get(0).presentation().shot();
            assertEquals(s,b.world().actors().get(0).presentation().shot());assertEquals(123_000_000L,s.loadingElapsedNanos());
            assertEquals(c.revision(),s.revision());assertEquals(1,s.life());assertEquals(1,s.targetLife());assertEquals(66,s.sprite());
            assertEquals(new ModernTargetHistory.Participant(2,c.actors().get(1).pose().member().identity()),s.target());
            assertEquals(2,a.personal().ammo().stocks().size());assertTrue(b.personal().ammo().stocks().isEmpty());
            var frozen=s;f.clock.addAndGet(1_000_000_000L);f.select(1,40746);
            assertEquals(frozen,state(f.engine.serviceObservation(),0).shot());assertEquals(s,a.world().actors().get(0).presentation().shot());
        }
    }
    @Test void missHasAnEventButDeniedAndNumericAttackNeverMintOrReplaceIt()throws Exception {
        try(var f=new ModernBattleEngineAmmoTest.Fixture()) {
            f.active();f.randomMode=1;assertEquals(ModernBattleEngine.AttackStatus.NUMERIC,f.attack(1).status());
            assertNull(state(f.engine.serviceObservation(),0).shot());f.randomMode=0;f.miss=true;f.draws.set(0);
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(2).status());
            var s=state(f.engine.serviceObservation(),0).shot();assertFalse(s.hit());assertEquals(1,s.count());assertEquals(2,s.attackSequence());
            assertEquals(ModernBattleEngine.AttackStatus.TOO_FAST,f.attack(3).status());
            assertEquals(s,state(f.engine.serviceObservation(),0).shot());
        }
    }
    @Test void timedAndHealingEventsKeepUseTimePositionAndOnlySuccessfulCount()throws Exception {
        try(var f=new ModernBattleEngineTimedTest.Fixture()) {
            f.active();long n=0;
            for(long id:new long[]{40010,40011,40012,40013,40014,40016}) {
                f.elapsed(++n*2_000_000_000L);assertEquals(ModernItemProtocol.Reason.NONE,f.use(0,id).reason());
                var c=f.engine.serviceObservation();var i=state(c,0).item();
                assertEquals(n,i.count());assertEquals(n*2_000_000_000L,i.loadingElapsedNanos());
                assertEquals(c.actors().get(0).supplies().effect(),i.effect());assertEquals(c.revision(),i.revision());
                assertEquals(c.actors().get(0).pose().position(),i.cell());
                var p=ModernBattleWireTest.publication(c,null);
                assertEquals(i,p.audience(f.players.get(1).scope()).world().actors().get(0).presentation().item());
            }
            var before=state(f.engine.serviceObservation(),0).item();
            assertEquals(ModernItemProtocol.Reason.CLASS_RESTRICTED,f.use(0,40068).reason());
            assertEquals(before,state(f.engine.serviceObservation(),0).item());
            assertEquals(ModernItemProtocol.Reason.NONE,f.use(1,40068).reason());assertEquals(751,state(f.engine.serviceObservation(),1).item().effect());
            f.elapsed(500_000_000_000L);f.engine.tick();assertEquals(before,state(f.engine.serviceObservation(),0).item());
        }
    }
    @Test void lateOrInvalidPotionResolverNeverPublishesAVisual()throws Exception {
        for(boolean late:new boolean[]{false,true})try(var f=new ModernBattleEngineItemTest.Fixture(0,false)) {
            f.active();f.random.set(bound->{if(late)f.clock.addAndGet(60_000_000_000L);return -1;});
            assertNotEquals(ModernItemProtocol.Reason.NONE,f.use(1,40010).reason());assertNull(state(f.engine.serviceObservation(),0).item());
        }
    }
    @Test void schemaTwoIsAdditiveAndTransportVersionNeverSilentlyChangesSchemaOne()throws Exception {
        try(var f=new ModernBattleEngineAmmoTest.Fixture()) {
            f.active();f.attack(1);var c=f.engine.serviceObservation();var v=ModernBattleWireTest.publication(c,null).audience(f.room.players.get(0).scope());
            var one=ModernBattleWire.encode(v);var two=ModernBattleWire.encode(v,2);
            assertEquals(1,ByteBuffer.wrap(one).getShort(4));assertEquals(2,ByteBuffer.wrap(two).getShort(4));assertEquals(13,ByteBuffer.wrap(two).getShort(12));
            assertArrayEquals(Arrays.copyOfRange(one,16,one.length),Arrays.copyOfRange(two,16,one.length));
            assertEquals(13,ByteBuffer.wrap(two).getShort(one.length));assertTrue(two.length-one.length<=8+ModernBattleWire.PRESENTATION_LIMIT);
            assertArrayEquals(one,ModernBattleWire.encode(v,1));assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encode(v,3));
            var echo=new ModernBattleReadProtocol.Request(v.personal().scope(),v.world().identity());
            var result=new ModernApplicationProtocol.BattleReadResult(1,echo,new ModernBattleReadProtocol.Reply(ModernBattleReadProtocol.State.DATA,v));
            assertArrayEquals(one,Arrays.copyOfRange(ModernApplicationProtocol.encode(result,35),72,72+one.length));
            assertArrayEquals(two,Arrays.copyOfRange(ModernApplicationProtocol.encode(result,36),72,72+two.length));
            ModernBattleWireTest.components(ModernBattlePresentation.State.class,"shot,item");
        }
    }
    @Test void eventConstructorsRejectUnqualifiedVisualsAndNonParticipantBowTargets() {
        var cell=ModernBattleEngineBotTest.cell(10,10);var to=ModernBattleEngineBotTest.cell(11,10);
        var npc=new ModernTargetHistory.Npc(new ModernNpcRoster.Id(new ModernNpcRoster.Scope(1,2,3),1));
        assertThrows(IllegalArgumentException.class,()->new ModernBattlePresentation.Shot(1,3,1,0,1,0,cell,npc,1,to,66,true));
        assertThrows(IllegalArgumentException.class,()->new ModernBattlePresentation.Item(1,3,-1,1,0,cell,189));
        assertThrows(IllegalArgumentException.class,()->new ModernBattlePresentation.Item(1,3,0,1,0,cell,999));
        assertThrows(IllegalArgumentException.class,()->new ModernBattlePresentation.Item(3,3,0,1,0,cell,189));
    }
    @Test void honorBowRetainsDeadLifeAndSeparatesTowerTargetFromParticipants()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(10000,1,500,1_000_000_000L,0,false,true,ModernBattlePresentationTest::bow)) {
            f.active();assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());
            var before=f.engine.serviceObservation();var s=state(before,0).shot();assertEquals(1,s.targetLife());
            assertEquals(0,before.actors().get(1).vital().values().hp());f.elapsed(5_100_000_000L);f.engine.tick();
            var revived=f.engine.serviceObservation();assertEquals(1,revived.actors().get(1).resources().life());assertEquals(s,state(revived,0).shot());
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,2,4).status());var tower=state(f.engine.serviceObservation(),0).shot();
            assertEquals(new ModernTargetHistory.Honor(4,1),tower.target());assertEquals(1,tower.targetLife());assertEquals(2,tower.count());
            var p=ModernBattleWireTest.publication(f.engine.serviceObservation(),null);
            assertNotNull(p.audience(f.players.get(1).scope()).world().actors().get(0).presentation().shot());
        }
    }
    @Test void publicGraphCannotReachAmmoStocksSuppliesOrSourceSelection() {
        ModernBattleAudienceTest.graph(ModernBattlePresentation.State.class,java.util.Set.of(ModernAmmo.State.class,ModernAmmo.Stock.class,
                ModernBattleObservation.Supplies.class,ModernUseHistory.Items.class,ModernRooms.Scope.class,ModernCharacterData.Snapshot.class));
    }
}
