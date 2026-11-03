package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernApplicationProtocol.*;
import static local.lito381.server.ModernBattleActionProtocol.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real service admission, engine commands and durable internal equipment SET.
 * No new transport/SET permission, synthetic combat or reflected state writes. */
class ModernBattleCanonicalActionServiceTest {
    @TempDir Path root;
    static BattleActionRequest request(ModernBattleLifecycleServiceTest.Fixture f,Message command,Target target) {
        return new BattleActionRequest(command.requestId(),new Request(ModernBattleCommandLifeTest.proof(f.engine,0),target,command));
    }
    static WorldRequest move(ModernBattleLifecycleServiceTest.Fixture f,long sequence,int heading) {
        var a=f.engine.serviceObservation().actors().get(0).pose();
        return new WorldRequest(9,new ModernWorldProtocol.Request(f.player(0).scope(),ModernWorldProtocol.Action.MOVE,
                heading,0,0,f.world.matchId(),f.world.generation(),sequence,a.position().x(),a.position().y(),ModernWorldProtocol.ZERO_DIGEST));
    }
    static ModernBattleActionReceipt move(ModernBattleLifecycleServiceTest.Fixture f,int heading)throws Exception {
        Thread.sleep(2);var message=request(f,move(f,++f.moves,heading),NO_TARGET);
        var receipt=f.service.canonicalAction(f.player(0).owner(),message);
        assertEquals(ModernBattleCommandLife.Rejection.NONE,receipt.rejection());
        assertEquals(ModernBattleEngine.MoveStatus.MOVED,((ModernBattleEngine.MoveResult)receipt.outcome()).status());
        assertEquals(f.moves,receipt.view().world().actors().get(0).pose().lastMoveSequence());return receipt;
    }
    @Test void changedPricesSurviveActualServiceMoveCastVictoryAndImmutableReceipts()throws Exception {
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null)) {
            f.reprice();ModernBattleActionReceipt first=null;
            for(int i=0;i<8;i++){var receipt=move(f,4);if(first==null)first=receipt;}
            var retained=first;assertEquals(1,retained.view().personal().equipment().revision());
            assertEquals(0,f.read(1).view().personal().equipment().revision());
            Thread.sleep(2);var cast=new CastRequest(10,new ModernSpellProtocol.Request(f.player(0).scope(),f.world.matchId(),f.world.generation(),3,70,0));
            var fired=f.service.canonicalAction(f.player(0).owner(),request(f,cast,NO_TARGET));
            // The two real casts plus reduced equipment capacity leave too
            // little MP for a third. Preserve that denial, not fabricated mana.
            assertEquals(ModernSpellProtocol.Reason.NO_MP,((ModernBattleEngine.CastResult)fired.outcome()).reason());
            assertEquals(2,fired.view().world().actors().get(0).spells().histories().get(ModernUseHistory.Lane.FIRE_STORM).current().casts());
            assertThrows(IllegalArgumentException.class,()->ModernLegacyBattleProjection.from(fired.outcome().observation()));
            for(int i=0;i<8;i++)move(f,0);
            ModernBattleActionReceipt won=null;
            for(int sequence=1;sequence<=100;sequence++) {
                Thread.sleep(2);var attack=new CombatRequest(11,new ModernCombatProtocol.Request(f.player(0).scope(),ModernCombatProtocol.Action.ATTACK,f.world.matchId(),f.world.generation(),sequence,2));
                var receipt=f.service.canonicalAction(f.player(0).owner(),request(f,attack,new Target(Kind.PLAYER,2,1)));
                assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,((ModernBattleEngine.AttackResult)receipt.outcome()).status());
                assertEquals(receipt.outcome().observation().revision(),receipt.view().world().revision());
                if(receipt.view().world().phase()==ModernBattleEngine.Phase.FINISHED){won=receipt;break;}
            }
            assertNotNull(won);assertEquals(won.view().world().result(),won.lifecycle().result());
            var terminal=won.lifecycle();
            assertThrows(IllegalArgumentException.class,()->new ModernBattleActionReceipt(retained.rejection(),true,retained.outcome(),retained.view(),terminal));
            ModernBattleLifecycleServiceTest.ModernWorldServiceItemTestAwait.until(()->f.lifecycle().settlement()==ModernWorldProtocol.SettlementState.SAVED);
            assertEquals(1,f.persisted.get());assertEquals(f.event.get(),won.lifecycle().result());
            assertEquals(ModernBattleEngine.Phase.ACTIVE,retained.view().world().phase());
            assertNull(retained.lifecycle().result());assertNull(retained.lifecycle().settlement());
            assertEquals(2,retained.view().world().actors().get(0).spells().histories().get(ModernUseHistory.Lane.FIRE_STORM).current().casts());
            var stopped=f.service.canonicalAction(f.player(0).owner(),request(f,move(f,f.moves+1,4),NO_TARGET));
            assertEquals(ModernBattleCommandLife.Rejection.NOT_ACTIVE,stopped.rejection());assertNull(stopped.outcome());
            assertEquals(ModernWorldProtocol.SettlementState.SAVED,stopped.lifecycle().settlement());
            f.leave(0);f.leave(1);assertEquals(0,f.service.matchCount());assertEquals(1,f.persisted.get());
        }
    }
    @Test void canonicalOwnerAndLifeAdmissionCannotConsumeSequenceAndLegacyAdapterStillEncodes()throws Exception {
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null)) {
            var command=move(f,1,4);var message=request(f,command,NO_TARGET);var proof=message.request().proof();
            assertThrows(SecurityException.class,()->f.service.canonicalAction(f.player(1).owner(),message));
            var future=new ModernBattleCommandLife.Proof(proof.player(),proof.world(),proof.entity(),proof.life(),proof.revision()+100);
            assertThrows(SecurityException.class,()->f.service.canonicalAction(f.player(0).owner(),new BattleActionRequest(9,new Request(future,NO_TARGET,command))));
            var stale=new ModernBattleCommandLife.Proof(proof.player(),proof.world(),proof.entity(),proof.life()+1,proof.revision());
            var rejected=f.service.canonicalAction(f.player(0).owner(),new BattleActionRequest(9,new Request(stale,NO_TARGET,command)));
            assertEquals(ModernBattleCommandLife.Rejection.STALE_LIFE,rejected.rejection());assertNull(rejected.outcome());
            assertEquals(0,f.engine.serviceObservation().actors().get(0).pose().lastMoveSequence());
            var accepted=f.service.action(f.player(0).owner(),message);
            assertEquals(ModernBattleCommandLife.Rejection.NONE,accepted.rejection());
            assertEquals(ModernWorldProtocol.Status.OK,((WorldResult)accepted.result()).reply().status());
            // Java accepts client requests, not its own server result opcode;
            // the full result decode is exercised by the actual Rust peer.
            assertTrue(ModernApplicationProtocol.encode(accepted,37).length>=MIN_RESULT_BYTES);
            assertEquals(1,f.engine.serviceObservation().actors().get(0).pose().lastMoveSequence());
            f.moves=1;move(f,0);
        }
    }
    static ModernBattleEngine engine(ModernWorldService service,TournamentPresence.Owner owner)throws Exception {
        ModernBattlePublicationTest.stopScheduler(service);
        return (ModernBattleEngine)ModernBattlePublicationTest.field(ModernBattlePublicationTest.entry(service,owner),"engine");
    }
    static BattleActionRequest wrap(ModernBattleEngine engine,Message command,Target target) {
        return new BattleActionRequest(command.requestId(),new Request(ModernBattleCommandLifeTest.proof(engine,0),target,command));
    }
    static Message legacy(ModernWorldService service,TournamentPresence.Owner owner,ModernBattleEngine engine,Message command,Target target)throws Exception {
        var result=service.action(owner,wrap(engine,command,target));
        assertEquals(ModernBattleCommandLife.Rejection.NONE,result.rejection());
        assertTrue(ModernApplicationProtocol.encode(result,37).length>=MIN_RESULT_BYTES);return result.result();
    }
    @Test void itemEmptyStockIsAnExactCommandDenialAndUnnegotiatedAmmoCannotExecute()throws Exception {
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null)) {
            var ammo=new AmmoRequest(20,new ModernAmmoProtocol.Request(f.player(0).scope(),ModernAmmoProtocol.Action.SELECT,f.world.matchId(),f.world.generation(),1,40743));
            assertThrows(SecurityException.class,()->f.service.canonicalAction(f.player(0).owner(),request(f,ammo,NO_TARGET)));
            var item=new ItemRequest(21,new ModernItemProtocol.Request(f.player(0).scope(),ModernItemProtocol.Action.USE,f.world.matchId(),f.world.generation(),1,40010));
            var receipt=f.service.canonicalAction(f.player(0).owner(),request(f,item,NO_TARGET));
            assertEquals(ModernBattleCommandLife.Rejection.NONE,receipt.rejection());
            assertEquals(ModernItemProtocol.Reason.EMPTY,((ModernBattleEngine.ItemResult)receipt.outcome()).reason());
            var next=new ItemRequest(22,new ModernItemProtocol.Request(f.player(0).scope(),ModernItemProtocol.Action.USE,f.world.matchId(),f.world.generation(),2,40010));
            assertEquals(ModernItemProtocol.Reason.EMPTY,((ItemResult)legacy(f.service,f.player(0).owner(),f.engine,next,NO_TARGET)).reply().reason());
        }
    }
    @Test void npcLightArrowUsesSharedAdmissionAndExactOwnerFilteredReply()throws Exception {
        try(var f=new ModernNpcServiceTest.Fixture(100,false,false,null,554,true,bound->0)) {
            ModernBattleActionServiceTest.enable(f);var w=f.active();var e=engine(f.service,f.a.owner());
            var q=ModernNpcSpellServiceTest.request(f,f.a,w,1);var target=new Target(Kind.NPC,q.serial(),q.targetLife());
            var result=(NpcSpellResult)legacy(f.service,f.a.owner(),e,new NpcSpellRequest(12,q),target);
            assertEquals(ModernNpcMagic.Reason.NONE,result.reply().reason());
            var next=new ModernNpcSpellProtocol.Request(q.player(),q.world(),2,1,q.serial(),q.targetLife());
            var denied=f.service.canonicalAction(f.a.owner(),wrap(e,new NpcSpellRequest(13,next),target));
            assertEquals(ModernNpcMagic.Reason.TOO_FAST,((ModernNpcMagic.Result)denied.outcome()).reason());
        }
    }
    @Test void npcDisintegrateUsesSharedAdmissionAndExactOwnerFilteredReply()throws Exception {
        try(var f=new ModernNpcDisintegrateServiceTest.Fixture(true,0,37)) {
            var w=f.active();var owner=f.players.get(0).owner();var e=engine(f.service,owner);var q=f.request(w,1);
            var target=new Target(Kind.NPC,q.target().serial(),q.targetLife());
            var result=(NpcDisintegrateResult)legacy(f.service,owner,e,new NpcDisintegrateRequest(14,q),target);
            assertEquals(ModernNpcDisintegrateState.Reason.NONE,result.reply().reason());
            var denied=f.service.canonicalAction(owner,wrap(e,new NpcDisintegrateRequest(15,f.request(result.reply().frame(),2)),target));
            assertEquals(ModernNpcDisintegrateState.Reason.TOO_FAST,((ModernNpcDisintegrateState.Result)denied.outcome()).reason());
        }
    }
    @Test void npcFireStormUsesSharedAdmissionAndExactOwnerFilteredReply()throws Exception {
        try(var f=new ModernNpcFireStormServiceTest.Fixture(true,0,true,37)) {
            var w=f.active();var owner=f.players.get(0).owner();var e=engine(f.service,owner);var q=f.request(w,1);
            var result=(NpcFireStormResult)legacy(f.service,owner,e,new NpcFireStormRequest(16,q),NO_TARGET);
            assertEquals(ModernNpcFireStormState.Reason.NONE,result.reply().reason());
            var denied=f.service.canonicalAction(owner,wrap(e,new NpcFireStormRequest(17,f.request(result.reply().frame(),2)),NO_TARGET));
            assertEquals(ModernNpcFireStormState.Reason.TOO_FAST,((ModernNpcFireStormState.Result)denied.outcome()).reason());
        }
    }
    @Test void siegeTowerAndPortalUseDomainDispatchAndKeepLegacyEncoding()throws Exception {
        try(var f=new ModernSiegeServiceTest.Fixture(0,60000,new ModernTimeAttackServiceTest.Journal())) {
            for(var p:f.players)f.service.register(p.owner(),p.scope(),deadline->p.row(),
                    deadline->ModernPhysicalCombatTest.fighter(ModernBattleEngineHonorTest.source(p.scope().sourceId(),p.row(),100,100)),
                    true,null,false,null,false,false,false,false,false,false,
                    new ModernSessionBoundary.Negotiated(1,37,1,ModernBattleReadProtocolTest.CAPS|ModernSessionProtocol.CAP_ACTION_LIFE));
            var w=f.active();var owner=f.players.get(0).owner();var e=engine(f.service,owner);
            var slot=ModernSiege.Slot.values()[0];var scope=w.siege().snapshot().state().scope();
            var hit=new ModernSiegeProtocol.Request(f.players.get(0).scope(),scope,ModernSiegeProtocol.Action.ATTACK_TOWER,1,slot);
            assertInstanceOf(SiegeResult.class,legacy(f.service,owner,e,new SiegeRequest(18,hit),new Target(Kind.SIEGE,slot.ordinal(),0)));
            // Portal attempts consume MOVE sequencing, not tower ATTACK sequencing.
            var portal=new ModernSiegeProtocol.Request(f.players.get(0).scope(),scope,ModernSiegeProtocol.Action.PORTAL,1,null);
            assertInstanceOf(SiegeResult.class,legacy(f.service,owner,e,new SiegeRequest(19,portal),NO_TARGET));
        }
    }
    @Test void terminalLegacyRecoveryKeepsSavedReceiptInsteadOfRebuildingPendingEngineFrame()throws Exception {
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null)) {
            f.win();ModernBattleLifecycleServiceTest.ModernWorldServiceItemTestAwait.until(()->f.lifecycle().settlement()==ModernWorldProtocol.SettlementState.SAVED);
            var result=f.service.action(f.player(0).owner(),request(f,move(f,1,4),NO_TARGET));
            assertEquals(ModernBattleCommandLife.Rejection.NOT_ACTIVE,result.rejection());
            var read=(CombatResult)result.result();assertEquals(ModernCombatProtocol.Action.READ,read.echo().action());
            assertEquals(ModernWorldProtocol.SettlementState.SAVED,read.reply().frame().world().completion().state());
            assertEquals(f.event.get(),read.reply().frame().world().completion().event());
            assertTrue(ModernApplicationProtocol.encode(result,37).length>=MIN_RESULT_BYTES);
        }
    }
    @Test void canonicalExecutionDoesNotReachLegacyProjectionOrTypedHandlers()throws Exception {
        var source=Files.readString(Path.of("src/main/java/local/lito381/server/ModernWorldService.java"));
        var start=source.indexOf("private ActionExecution executeAction(");
        var area=source.substring(start,source.indexOf("private ModernApplicationProtocol.Message legacyAction(",start));
        for(var forbidden:List.of("ModernLegacyBattleProjection",".legacy()","entry.frame()","entry.combat()","entry.npc()","result.snapshot()","result.frame()","recoverAction("))
            assertFalse(area.contains(forbidden),forbidden);
        assertTrue(area.contains("engine.observeLifeCommand(owner,proof,guard.target()"));
        assertTrue(source.substring(source.indexOf("ModernApplicationProtocol.BattleActionResult action("),start).contains("executeAction(owner,message)"));
    }
}
