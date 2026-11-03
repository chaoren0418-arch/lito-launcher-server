package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleActionProtocol.*;
import static local.lito381.server.ModernApplicationProtocol.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernBattleActionServiceTest {
    static void enable(ModernNpcServiceTest.Fixture f) {
        for(var p:List.of(f.a,f.b))f.register(p,37,ModernBattleReadProtocolTest.CAPS|ModernSessionProtocol.CAP_ACTION_LIFE,null);
    }
    static ModernBattleEngine engine(ModernNpcServiceTest.Fixture f)throws Exception {
        return (ModernBattleEngine)ModernBattlePublicationTest.field(ModernBattlePublicationTest.entry(f.service,f.a.owner()),"engine");
    }
    @Test void actualServiceGuardedKillThenStaleTargetReturnsPrivateReadWithoutConsumingNextSequence()throws Exception {
        try(var f=new ModernNpcServiceTest.Fixture(100,true)) {
            enable(f);var initial=f.active();ModernBattlePublicationTest.stopScheduler(f.service);var engine=engine(f);
            var target=initial.actors().get(0);var scope=new Target(Kind.NPC,target.serial(),target.life());
            var q=new NpcRequest(9,f.request(f.a,initial.combat().world(),1,target.serial(),target.life()));
            var guard=new Request(ModernBattleCommandLifeTest.proof(engine,0),scope,q);
            var accepted=f.service.action(f.a.owner(),new BattleActionRequest(9,guard));
            assertEquals(ModernBattleCommandLife.Rejection.NONE,accepted.rejection());
            var hit=(NpcResult)accepted.result();assertEquals(ModernNpcProtocol.Status.OK,hit.reply().status());
            assertEquals(1,hit.reply().frame().combat().vitals().get(0).attackSequence());assertTrue(hit.reply().frame().actors().stream().allMatch(a->a.lane()==0));
            ModernApplicationProtocol.encode(accepted,37);
            var queued=new NpcRequest(10,f.request(f.a,initial.combat().world(),2,target.serial(),target.life()));
            var stale=new Request(ModernBattleCommandLifeTest.proof(engine,0),scope,queued);
            var rejected=f.service.action(f.a.owner(),new BattleActionRequest(10,stale));
            assertEquals(ModernBattleCommandLife.Rejection.STALE_TARGET,rejected.rejection());
            var read=(NpcResult)rejected.result();assertEquals(ModernNpcProtocol.Action.READ,read.echo().action());
            assertEquals(ModernNpcProtocol.Status.OK,read.reply().status());assertEquals(0,read.echo().sequence());
            assertEquals(1,read.reply().frame().combat().vitals().get(0).attackSequence());
            assertEquals(1,read.reply().frame().combat().world().actors().size());assertEquals(0,read.reply().frame().combat().world().audienceLane());
            assertEquals(1,read.reply().frame().combat().world().actors().get(0).entityId());
            ModernApplicationProtocol.encode(ModernMatchDescriptor.forPeer(rejected,new ModernSessionBoundary.Negotiated(1,37,1,ModernBattleReadProtocolTest.CAPS|ModernSessionProtocol.CAP_ACTION_LIFE)),37);
            assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(rejected,new ModernSessionBoundary.Negotiated(1,36,1,ModernBattleReadProtocolTest.CAPS)));
        }
    }
    @Test void staleLifeRecoveryAndSubsequentFreshMoveUseSameUnusedSequenceAndExactOwner()throws Exception {
        try(var f=new ModernNpcServiceTest.Fixture(100,true)) {
            enable(f);var initial=f.active();ModernBattlePublicationTest.stopScheduler(f.service);var e=engine(f);var p=ModernBattleCommandLifeTest.proof(e,0);
            var a=initial.combat().world().actors().get(0);var w=p.world();
            var q=new WorldRequest(9,new ModernWorldProtocol.Request(f.a.scope(),ModernWorldProtocol.Action.MOVE,0,0,0,w.matchId(),w.generation(),1,a.position().x(),a.position().y(),ModernWorldProtocol.ZERO_DIGEST));
            var old=new ModernBattleCommandLife.Proof(p.player(),w,p.entity(),p.life()+1,p.revision());
            var request=new BattleActionRequest(9,new Request(old,NO_TARGET,q));
            assertThrows(SecurityException.class,()->f.service.action(f.b.owner(),request));
            var rejected=f.service.action(f.a.owner(),request);assertEquals(ModernBattleCommandLife.Rejection.STALE_LIFE,rejected.rejection());
            assertTrue(rejected.result() instanceof NpcResult);ModernApplicationProtocol.encode(rejected,37);
            assertEquals(0,e.serviceObservation().actors().get(0).pose().lastMoveSequence());
            var fresh=new BattleActionRequest(9,new Request(ModernBattleCommandLifeTest.proof(e,0),NO_TARGET,q));
            var accepted=f.service.action(f.a.owner(),fresh);assertEquals(ModernBattleCommandLife.Rejection.NONE,accepted.rejection());
            assertEquals(1,e.serviceObservation().actors().get(0).pose().lastMoveSequence());
            ModernApplicationProtocol.encode(accepted,37);
        }
    }
    @Test void targetLifeCannotFollowARealHonorRespawnEvenWhenCasterRemainsAlive()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(100,1,100,1_000_000_000L)) {
            f.active();var oldTarget=new Target(Kind.PLAYER,2,1);var p=ModernBattleCommandLifeTest.proof(f.engine,0);
            assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,4,2).reason());f.elapsed(5_000_000_000L);f.engine.tick();
            assertEquals(2,ModernBattleCommandLifeTest.proof(f.engine,1).life());
            var rejected=f.engine.observeLifeCommand(f.players.get(0).owner(),p,oldTarget,()->{fail("old target life called combat");return f.attack(0,1,2);});
            assertEquals(ModernBattleCommandLife.Rejection.STALE_TARGET,rejected.rejection());
            assertEquals(0,rejected.observation().actors().get(0).vital().attackSequence());
            var fresh=f.engine.observeLifeCommand(f.players.get(0).owner(),ModernBattleCommandLifeTest.proof(f.engine,0),new Target(Kind.PLAYER,2,2),()->f.attack(0,1,2));
            assertEquals(ModernBattleCommandLife.Rejection.NONE,fresh.rejection());assertEquals(1,fresh.observation().actors().get(0).vital().attackSequence());
        }
    }
}
