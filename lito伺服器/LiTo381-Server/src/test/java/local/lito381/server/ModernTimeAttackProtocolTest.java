package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModernTimeAttackProtocolTest {
    static final ModernMatchDescriptor DESCRIPTOR=new ModernMatchDescriptor(ModernMatchDescriptor.Ruleset.COOPERATIVE_TIME_ATTACK,
            ModernMatchDescriptor.COMBAT|ModernMatchDescriptor.NPC|ModernMatchDescriptor.TIME_ATTACK,2000,2000,500_000_000,60_000);
    static ModernNpcProtocol.Frame frame(ModernTimeAttackEngineTest.Fixture f,ModernNpcBattle.Frame value) {
        var result=f.engine.timeAttackResult();
        if(result!=null)result=new ModernMatchSettlement.Event(UUID.fromString("12345678-1234-5678-9abc-def012345678"),result.matchId(),result.generation(),result.engineRevision(),
                result.mode(),result.mapId(),result.finishedAtMillis(),result.participants(),result.timeAttack());
        return ModernTimeAttackProtocol.frame(value,DESCRIPTOR,result,ModernWorldProtocol.SettlementState.PENDING,
                new ModernNpcProtocol.Resources(f.plan.waves().stream().flatMap(w->w.spawns().stream()).map(s->s.template().graphic()).distinct().sorted().toList()));
    }
    static ModernApplicationProtocol.NpcResult read(ModernTimeAttackEngineTest.Fixture f,int player) {
        var v=f.view();return new ModernApplicationProtocol.NpcResult(77,new ModernNpcProtocol.Request(f.players.get(player).scope(),ModernNpcProtocol.Action.READ,v.npcs().scope(),0,0,0),
                new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,frame(f,v)));
    }
    static byte[] golden(String name,ModernApplicationProtocol.NpcResult m)throws Exception {
        var b=ModernApplicationProtocol.encode(m,25);assertEquals(m,ModernApplicationProtocol.decode(b,25));
        System.out.println("time-attack-v25-"+name+"="+java.util.HexFormat.of().formatHex(b));return b;
    }
    @Test void preparingHasNoEngineRevisionOrCombatAndKeepsTheSamePolicyOnLoad()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(1,8,100,false)) {
            var loaded=read(f,0).reply().frame().combat().world();var data=ModernTimeAttackProtocol.preparing(f.plan,1,1);
            var w=new ModernWorldProtocol.Frame(loaded.matchId(),loaded.generation(),1,ModernWorldProtocol.Phase.PREPARING,ModernWorldProtocol.AbortReason.NONE,
                    loaded.room(),loaded.manifest(),loaded.actors(),null,List.of(),DESCRIPTOR,ModernPetRaceProtocol.Snapshot.disabled(),-1,ModernQuestProtocol.Data.disabled(),data);
            var r=new ModernWorldProtocol.Request(f.players.get(0).scope(),ModernWorldProtocol.Action.PREPARE,0,w.room().summary().id(),w.generation()-1,0,0,0,0,0,ModernWorldProtocol.ZERO_DIGEST);
            var m=new ModernApplicationProtocol.WorldResult(76,r,new ModernWorldProtocol.Reply(ModernWorldProtocol.Status.OK,ModernWorldProtocol.Reason.NONE,w));
            byte[] bytes=ModernApplicationProtocol.encode(m,25);assertEquals(m,ModernApplicationProtocol.decode(bytes,25));
            System.out.println("time-attack-v25-preparing="+java.util.HexFormat.of().formatHex(bytes));
            assertEquals(loaded.timeAttack().planSha256(),data.planSha256());assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(w,List.of()));
            var joined=new byte[bytes.length*2];System.arraycopy(bytes,0,joined,0,bytes.length);System.arraycopy(bytes,0,joined,bytes.length,bytes.length);
            var input=new java.io.ByteArrayInputStream(joined);assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(input,25));assertEquals(bytes.length,input.available());
            assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(input,25));assertEquals(0,input.available());
        }
    }
    @Test void actualEngineLoadingCountdownTowerWavesTerminalAndWideHpUseTheSameNpcEnvelope()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(1,8,100,false)) {
            golden("loading",read(f,0));var p=f.players.get(0);f.engine.ackLoaded(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),f.binding.map().digest());
            golden("countdown",read(f,0));f.elapsed(0);golden("waiting",read(f,0));
            var hit=f.tower(0,1);var request=new ModernNpcProtocol.Request(p.scope(),ModernNpcProtocol.Action.ATTACK_TOWER,hit.frame().npcs().scope(),1,0,0);
            var m=new ModernApplicationProtocol.NpcResult(78,request,new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,frame(f,hit.frame())));
            golden("tower",m);golden("fighting",read(f,0));
            for(int wave=0;wave<3;wave++){f.elapsed(1000+wave*1000);f.hit(0,wave+2,f.living());golden("wave"+wave,read(f,0));}
            var finalFrame=read(f,0).reply().frame().combat().world();var event=finalFrame.timeAttack().event(finalFrame);
            assertEquals(f.engine.timeAttackResult().timeAttack(),event.timeAttack());assertEquals(f.engine.timeAttackResult().participants(),event.participants());
            for(int version:List.of(1,17,21,24))assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(m,version));
        }
        try(var f=new ModernTimeAttackEngineTest.Fixture(2,8,100,false,4_000_000_000L)) {f.active();f.tower(0,1);golden("wide",read(f,0));}
    }
    @Test void finalNpcAttackAckSurvivesSettlementPublishingBeforeEncoding()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(1,8,100,false)) {
            f.active();f.tower(0,1);
            for(int wave=0;wave<3;wave++){f.elapsed(1000+wave*1000);f.hit(0,wave+2,f.living());}
            var terminal=read(f,0).reply().frame();var c=terminal.combat();var w=c.world();
            var hit=terminal.exchanges().stream().filter(e->e.direction()==ModernNpcBattle.Direction.PLAYER_TO_NPC).findFirst().orElseThrow();
            var request=new ModernNpcProtocol.Request(f.players.get(0).scope(),ModernNpcProtocol.Action.ATTACK,terminal.scope(),hit.requestSequence(),hit.npc().serial(),hit.npcLife());
            for(var state:List.of(ModernWorldProtocol.SettlementState.SAVED,ModernWorldProtocol.SettlementState.FAILED)) {
                // The asynchronous SQL receipt advances the wire frame, not the final attack event.
                var saved=new ModernWorldProtocol.Frame(w.matchId(),w.generation(),w.revision()+1,w.phase(),w.abortReason(),w.room(),w.manifest(),w.actors(),null,w.casts(),w.descriptor(),w.race(),w.audienceLane(),w.quest(),w.timeAttack().withSettlement(state));
                var frame=terminal.withCombat(new ModernCombatProtocol.Frame(saved,c.vitals(),c.damages(),c.items(),c.weights(),c.buffs(),c.ammo(),c.regen(),c.honor(),c.fireStorms(),c.npcSpells()));
                var reply=new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,frame);
                var message=new ModernApplicationProtocol.NpcResult(79,request,reply);
                for(int version:List.of(25,26))assertEquals(message,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message,version),version));
                for(var wrong:List.of(
                        new ModernNpcProtocol.Request(request.player(),request.action(),request.world(),request.sequence()+1,request.serial(),request.life()),
                        new ModernNpcProtocol.Request(request.player(),request.action(),request.world(),request.sequence(),request.serial()+1,request.life()),
                        new ModernNpcProtocol.Request(request.player(),request.action(),request.world(),request.sequence(),request.serial(),request.life()+1)))
                    assertThrows(IllegalArgumentException.class,()->ModernNpcProtocol.validate(new ModernApplicationProtocol.NpcResult(80,wrong,reply)));
            }
        }
    }
    @Test void departureTimeoutAbortAndPostResultStatePreserveWholeCooperativeAuthority()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(2,8,100,false)) {
            f.active();f.tower(0,1);f.engine.withdrawTimeAttack(f.players.get(0).owner());golden("departed",read(f,1));
            f.elapsed(20_000);var result=read(f,1);golden("timeout",result);
            assertTrue(result.reply().frame().combat().world().timeAttack().event(result.reply().frame().combat().world()).participants().stream().allMatch(p->p.verdict()==ModernMatchSettlement.Verdict.LOSS));
        }
        try(var f=new ModernTimeAttackEngineTest.Fixture(1,8,100,false)) {f.active();f.elapsed(60_000);golden("aborted",read(f,0));}
    }
    @Test void typedTowerRequestHasNoNpcSerialAndOldPeersCannotNegotiateTheDescriptor()throws Exception {
        var p=new ModernRooms.Scope(7,9007199254740993L,2,3);var s=new ModernNpcRoster.Scope(11,12,13);
        var r=new ModernNpcProtocol.Request(p,ModernNpcProtocol.Action.ATTACK_TOWER,s,Long.MAX_VALUE,0,0);
        assertEquals(new ModernTimeAttack.TowerId(s),r.tower());assertThrows(IllegalArgumentException.class,r::target);
        var m=new ModernApplicationProtocol.NpcRequest(3,r);var bytes=ModernApplicationProtocol.encode(m,25);assertEquals(88,bytes.length);assertEquals(m,ModernApplicationProtocol.decode(bytes,25));
        System.out.println("time-attack-v25-request="+java.util.HexFormat.of().formatHex(bytes));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,24));
        for(int i=0;i<bytes.length;i++){var truncated=Arrays.copyOf(bytes,i);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(truncated,25));}
        assertThrows(IllegalArgumentException.class,()->new ModernNpcProtocol.Request(p,ModernNpcProtocol.Action.ATTACK_TOWER,s,1,1,0));
        assertFalse(DESCRIPTOR.supportedBy(24,ModernSessionProtocol.KNOWN_CAPABILITIES));assertTrue(DESCRIPTOR.supportedBy(25,ModernSessionProtocol.KNOWN_CAPABILITIES));
        assertFalse(DESCRIPTOR.supportedBy(25,ModernSessionProtocol.RUNTIME_CAPABILITIES));
        assertThrows(IllegalArgumentException.class,()->DESCRIPTOR.validateMode(2));
    }
    @Test void ordinaryMemberDeathContinuesToMixedVerdictsAndKeepsWideResultTime()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(2,8,1,false)) {
            f.active();f.move(0,1,10,10,6);f.elapsed(200);f.move(0,2,9,10,6);f.tower(1,1);
            golden("beforedeath",read(f,0));var npc=f.living();f.elapsed(1000);f.engine.stepNpc(npc.id(),npc.life(),4096,false);golden("memberdeath",read(f,0));
            f.elapsed(1200);f.move(0,3,8,10,2);f.elapsed(1400);f.move(0,4,9,10,2);
            for(int wave=0;wave<3;wave++){f.elapsed(2000+wave*1000);f.epoch.set(9007199254740993L+wave);f.hit(0,wave+1,f.living());
                if(wave<2)golden("mixedwave"+wave,read(f,0));}
            var result=read(f,0);golden("mixed",result);var w=result.reply().frame().combat().world();
            assertEquals(9007199254740995L,w.timeAttack().receipt().finishedAtMillis());
            assertEquals(List.of(ModernMatchSettlement.Verdict.WIN,ModernMatchSettlement.Verdict.LOSS),w.timeAttack().event(w).participants().stream().map(ModernMatchSettlement.Participant::verdict).toList());
        }
    }
    @Test void missingExtensionLostReceiptMismatchedAliveMaskPaddingAndTruncationFailClosed()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(1,8,100,false)) {
            f.active();var m=read(f,0);var w=m.reply().frame().combat().world();
            assertThrows(IllegalArgumentException.class,()->new ModernWorldProtocol.Frame(w.matchId(),w.generation(),w.revision(),w.phase(),w.abortReason(),w.room(),w.manifest(),w.actors(),w.completion(),w.casts(),w.descriptor(),w.race(),-1,w.quest()));
            var b=ByteBuffer.allocate(8192);ModernTimeAttackProtocol.put(b,w.timeAttack());var data=Arrays.copyOf(b.array(),b.position());
            var scope=m.echo().world();assertEquals(w.timeAttack(),ModernTimeAttackProtocol.get(ByteBuffer.wrap(data),scope));
            for(int i=0;i<data.length;i++){var shortData=Arrays.copyOf(data,i);assertThrows(RuntimeException.class,()->ModernTimeAttackProtocol.get(ByteBuffer.wrap(shortData),scope));}
            for(int i:List.of(1,7,8,10,11,12,13,14,15,140,143)){var bad=data.clone();bad[i]=(byte)255;assertThrows(RuntimeException.class,()->ModernTimeAttackProtocol.get(ByteBuffer.wrap(bad),scope));}
            var encoded=ModernApplicationProtocol.encode(m,25);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(encoded,24));
            assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(m,new ModernSessionBoundary.Negotiated(1,24,1,ModernSessionProtocol.KNOWN_CAPABILITIES)));
            assertEquals(m,ModernMatchDescriptor.forPeer(m,new ModernSessionBoundary.Negotiated(1,25,1,ModernSessionProtocol.KNOWN_CAPABILITIES)));
        }
    }
}
