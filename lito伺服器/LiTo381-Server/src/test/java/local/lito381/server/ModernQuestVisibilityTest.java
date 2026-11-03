package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

/** Real service projection and collision authority, not a client-side hide flag. */
class ModernQuestVisibilityTest {
    static ModernWorldProtocol.Frame copy(ModernWorldProtocol.Frame f,List<ModernBattleEngine.Actor> actors,int audience) {
        return new ModernWorldProtocol.Frame(f.matchId(),f.generation(),f.revision(),f.phase(),f.abortReason(),f.room(),f.manifest(),
                actors,f.completion(),f.casts(),f.descriptor(),f.race(),audience);
    }
    static void own(ModernWorldProtocol.Frame frame,int lane) {
        assertNotNull(frame);assertTrue(frame.isolatedForces());assertEquals(lane,frame.audienceLane());
        assertEquals(2,frame.room().members().size());assertEquals(1,frame.actors().size());
        assertEquals(lane+1,frame.actors().get(0).entityId());assertEquals(lane,frame.actors().get(0).member().team());
    }
    static ModernMatchDescriptor descriptor(boolean regen) {
        return new ModernMatchDescriptor(ModernMatchDescriptor.Ruleset.NPC_ENCOUNTER,
                ModernMatchDescriptor.COMBAT|ModernMatchDescriptor.NPC|ModernMatchDescriptor.ISOLATED_FORCES
                        |(regen?ModernMatchDescriptor.ITEMS|ModernMatchDescriptor.WEIGHT|ModernMatchDescriptor.REGEN:0),2000,2000,0,60_000);
    }
    @Test void sharedGeometryCanUseIdenticalCampsButEveryAuthenticatedReplyContainsOnlyOwnForce()throws Exception {
        try(var f=new ModernNpcServiceTest.Fixture(100,true,true)) {
            var w=f.loading();own(w,0);var guest=f.read(f.b,w);own(guest.frame().combat().world(),1);
            assertEquals(w.actors().get(0).position(),guest.frame().combat().world().actors().get(0).position());
            f.ack(f.a,w);f.ack(f.b,w);
            for(var p:List.of(f.a,f.b)) {
                int lane=p==f.a?0:1;var reply=f.read(p,w);ModernNpcServiceTest.wire(f.request(p,w,0,0,0),reply);
                own(reply.frame().combat().world(),lane);assertEquals(1,reply.frame().actors().size());assertEquals(lane,reply.frame().actors().get(0).lane());
                assertEquals(ModernNpcServiceTest.HUGE,reply.frame().combat().vital(lane+1).maxHp());
                var read=new ModernCombatProtocol.Request(p.scope(),ModernCombatProtocol.Action.READ,w.matchId(),w.generation(),0,0);
                var combat=f.service.combat(p.owner(),read);own(combat.frame().world(),lane);
                var encoded=new ModernApplicationProtocol.CombatResult(1,read,combat);
                assertEquals(encoded,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(encoded,20),20));
                // Unsupported capabilities are rejected before any snapshot is returned.
                assertThrows(SecurityException.class,()->f.service.item(p.owner(),new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.READ,w.matchId(),w.generation(),0,0)));
                assertThrows(SecurityException.class,()->f.service.ammo(p.owner(),new ModernAmmoProtocol.Request(p.scope(),ModernAmmoProtocol.Action.READ,w.matchId(),w.generation(),0,0)));
                assertThrows(SecurityException.class,()->f.service.cast(p.owner(),new ModernSpellProtocol.Request(p.scope(),w.matchId(),w.generation(),1,5)));
                var actor=reply.frame().combat().world().actors().get(0);
                var move=new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.MOVE,0,0,0,w.matchId(),w.generation(),1,actor.position().x(),actor.position().y(),ModernWorldProtocol.ZERO_DIGEST);
                var moved=f.service.handle(p.owner(),move);assertEquals(ModernWorldProtocol.Status.OK,moved.status());own(moved.frame(),lane);
                var message=new ModernApplicationProtocol.WorldResult(2,move,moved);
                assertEquals(message,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message,20),20));
            }
            assertThrows(SecurityException.class,()->f.service.npc(f.b.owner(),f.request(f.a,w,0,0,0)));
            f.service.handle(f.a.owner(),new ModernWorldProtocol.Request(f.a.scope(),ModernWorldProtocol.Action.LEAVE,0,0,0,w.matchId(),w.generation(),0,0,0,ModernWorldProtocol.ZERO_DIGEST));
            var aborted=f.read(f.b,w);own(aborted.frame().combat().world(),1);assertTrue(aborted.frame().closed());
            ModernNpcServiceTest.wire(f.request(f.b,w,0,0,0),aborted);
        }
    }
    @Test void v19CannotReserveIsolatedWorldOrReadAnyCharacterProfiles()throws Exception {
        try(var f=new ModernNpcServiceTest.Fixture(100,true)) {
            f.register(f.b,19,(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_NPC_FIRE_STORM)),null);
            assertEquals(ModernWorldProtocol.Status.UNAVAILABLE,f.prepare().status());
            assertEquals(0,f.physicalReads.get());assertEquals(0,f.nightReads.get());assertEquals(0,f.service.active());
        }
    }
    @Test void isolatedWireRequiresProjectionAndExactGlobalIdentityNotRenumberedOrOpposingActors()throws Exception {
        try(var f=ModernQuestEngineTest.fixture(128,false)) {
            var snapshot=f.active();var full=ModernNpcProtocol.Frame.from(snapshot,descriptor(false));
            var guest=f.players.get(1);var view=ModernQuestVisibility.npc(full,guest.scope());own(view.combat().world(),1);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernWorldProtocol.putFrame(ByteBuffer.allocate(65536),full.combat().world(),20));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernWorldProtocol.putFrame(ByteBuffer.allocate(65536),view.combat().world(),19));
            var bytes=ByteBuffer.allocate(65536);ModernWorldProtocol.putFrame(bytes,view.combat().world(),20);bytes.flip();
            assertEquals(view.combat().world(),ModernWorldProtocol.getFrame(bytes,20));assertFalse(bytes.hasRemaining());
            assertThrows(IllegalArgumentException.class,()->copy(view.combat().world(),List.of(),1));
            assertThrows(IllegalArgumentException.class,()->copy(view.combat().world(),List.of(full.combat().world().actors().get(0)),1));
            assertThrows(IllegalArgumentException.class,()->copy(view.combat().world(),view.combat().world().actors(),0));
            assertThrows(SecurityException.class,()->ModernQuestVisibility.world(view.combat().world(),f.players.get(0).scope()));
            assertThrows(IllegalArgumentException.class,()->view.combat().world().withDescriptor(null));
            var r=new ModernNpcProtocol.Request(guest.scope(),ModernNpcProtocol.Action.READ,view.scope(),0,0,0);
            var message=new ModernApplicationProtocol.NpcResult(3,r,new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,view.withResources(ModernNpcProtocol.Resources.from(f.config.catalog()))));
            assertEquals(message,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message,20),20));
        }
    }
    @Test void sparseGuestVitalsItemsWeightAndRegenRetainWideValuesAndWireIdentity()throws Exception {
        try(var f=new ModernBattleEngineNpcTest.Fixture(2,0,true,ModernNpcServiceTest.HUGE,0,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(),false)) {
            var plan=ModernQuestProgressTest.plan(f.binding.key(),f.config.catalog());
            f.engine=f.build(new ModernNpcBattle.Config(f.config.key(),f.config.catalog(),f.config.players(),f.config.laneAnchors(),128,1000,plan));
            f.active();var p=f.players.get(1);var frame=ModernQuestVisibility.npc(ModernNpcProtocol.Frame.from(f.view(),descriptor(true)),p.scope());
            own(frame.combat().world(),1);assertEquals(ModernNpcServiceTest.HUGE,frame.combat().vital(2).maxHp());
            assertEquals(2,frame.combat().items().get(0).entityId());assertEquals(2,frame.combat().weights().get(0).entityId());assertEquals(2,frame.combat().regen().get(0).entityId());
            var request=new ModernNpcProtocol.Request(p.scope(),ModernNpcProtocol.Action.READ,frame.scope(),0,0,0);
            ModernNpcServiceTest.wire(request,new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,
                    frame.withResources(ModernNpcProtocol.Resources.from(f.config.catalog())).withNames(ModernNpcNames.Table.from(f.config.catalog()))));
        }
    }
    @Test void opposingPlayersAndNpcsCanOverlapButDoNotBlockTargetOrDamageEachOther()throws Exception {
        var map=ModernWorldMap.fromSectorBytes(554,Map.of(ModernBattleEngineBotTest.SECTOR,ModernWorldMapTest.openSectorBytes()),()->false);
        var positions=List.of(ModernQuestProgressTest.cell(10,10),ModernQuestProgressTest.cell(11,10));
        try(var f=new ModernBattleEngineNpcTest.Fixture(2,0,false,100,0,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(),false,map,positions)) {
            var plan=ModernQuestProgressTest.encounterPlan(f.binding.key(),ModernNpcProtocolTest.futureCatalog(),List.of(ModernQuestProgressTest.cell(12,10),ModernQuestProgressTest.cell(12,10)));
            // Use the same catalog as the qualified plan, but preserve real source fighter bindings.
            f.engine=f.build(new ModernNpcBattle.Config(f.config.key(),ModernNpcProtocolTest.futureCatalog(),f.config.players(),positions,128,1000,plan));
            f.active();var own=ModernQuestEngineTest.living(f,0);var other=ModernQuestEngineTest.living(f,1);
            assertEquals(own.cell(),other.cell());
            assertEquals(ModernNpcBattle.Reason.TARGET_DEAD,f.attack(0,1,other).reason());
            assertEquals(0,f.draws.get());assertEquals(other.hp(),ModernQuestEngineTest.living(f,1).hp());
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,10,10,2).status());
            assertEquals(f.view().world().actors().get(0).position(),f.view().world().actors().get(1).position());
            f.elapsed(200);assertEquals(ModernBattleEngine.MoveStatus.OCCUPIED,f.move(0,2,11,10,2).status());
            long otherHp=f.view().world().vitals().get(1).hp();var hit=f.step(own);
            assertEquals(ModernNpcBattle.Reason.ATTACKED,hit.reason());assertEquals(otherHp,hit.frame().world().vitals().get(1).hp());
            assertTrue(hit.frame().exchanges().stream().allMatch(e->e.participant().equals(ModernParticipantId.human(7,1))));
        }
    }
}
