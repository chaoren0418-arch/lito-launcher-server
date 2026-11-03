package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernQuestProtocolTest {
    static ModernMatchDescriptor descriptor() {
        var d=ModernQuestVisibilityTest.descriptor(true);
        return new ModernMatchDescriptor(d.ruleset(),d.features()|ModernMatchDescriptor.QUEST_OBJECTIVES,d.preparationTimeoutMillis(),d.loadTimeoutMillis(),d.countdownNanos(),d.activeTimeoutMillis());
    }
    static ModernNpcProtocol.Frame frame(ModernBattleEngineNpcTest.Fixture f,ModernNpcBattle.Frame snapshot,int lane) {
        var raw=ModernNpcProtocol.Frame.from(snapshot,descriptor()).withResources(ModernNpcProtocol.Resources.from(f.config.catalog()));
        return ModernQuestVisibility.npc(raw,f.players.get(lane).scope());
    }
    static ModernApplicationProtocol.NpcResult read(ModernBattleEngineNpcTest.Fixture f,ModernNpcBattle.Frame snapshot,int lane) {
        return new ModernApplicationProtocol.NpcResult(71,new ModernNpcProtocol.Request(f.players.get(lane).scope(),ModernNpcProtocol.Action.READ,f.scope,0,0,0),
                new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,frame(f,snapshot,lane)));
    }
    static byte[] wire(ModernApplicationProtocol.Message message)throws Exception {
        var bytes=ModernApplicationProtocol.encode(message,21);assertEquals(message,ModernApplicationProtocol.decode(bytes,21));return bytes;
    }
    @Test void ownForceWorldAppendixIsPresentBeforeLoadedAndCarriesI64FutureTargets()throws Exception {
        try(var f=ModernQuestObjectivesTest.fixture(513,Long.MAX_VALUE,false)) {
            var snapshot=f.view();
            for(int lane=0;lane<2;lane++) {
                var r=read(f,snapshot,lane);wire(r);var q=r.reply().frame().combat().world().quest();
                assertEquals(4,q.targets().size());assertEquals(1,q.lanes().size());assertEquals(lane,q.lanes().get(0).lane());
                assertEquals(Long.MAX_VALUE,q.targets().get(0).maximumHp());assertTrue(q.targets().stream().allMatch(t->t.lane()==laneIndex(q)));
                assertTrue(q.actors().isEmpty());assertEquals(1,q.progressRevision());
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(r,20));
                var world=r.reply().frame().combat().world();
                wire(new ModernApplicationProtocol.WorldResult(70,new ModernWorldProtocol.Request(f.players.get(lane).scope(),ModernWorldProtocol.Action.READ,0,0,0,f.ticket.matchId(),f.ticket.generation(),0,0,0,ModernWorldProtocol.ZERO_DIGEST),
                        new ModernWorldProtocol.Reply(ModernWorldProtocol.Status.OK,ModernWorldProtocol.Reason.NONE,world)));
            }
            var raw=ModernNpcProtocol.Frame.from(snapshot,descriptor()).withResources(ModernNpcProtocol.Resources.from(f.config.catalog()));
            var full=new ModernApplicationProtocol.NpcResult(71,read(f,snapshot,0).echo(),new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,raw));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(full,21));
        }
    }
    private static int laneIndex(ModernQuestProtocol.Data q){return q.lanes().get(0).lane();}
    @Test void actualFinalStrikeAndBothAudienceResultsRoundTripWithoutLeakingOtherTargets()throws Exception {
        try(var f=ModernQuestObjectivesTest.fixture(554,1,false)) {
            f.active();for(int s=1;s<=4;s++)ModernQuestObjectivesTest.clear(f,0,s,s,s*1000L);
            var target=ModernQuestObjectivesTest.objective(f,0);f.elapsed(4700);
            var before=read(f,f.view(),0);wire(before);
            var hit=ModernQuestObjectivesTest.strike(f,0,5,target.id());
            var request=new ModernNpcProtocol.Request(f.players.get(0).scope(),ModernNpcProtocol.Action.ATTACK_OBJECTIVE,f.scope,5,target.id().slot(),0);
            var response=new ModernApplicationProtocol.NpcResult(72,request,new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,frame(f,hit.frame(),0)));
            wire(response);wire(new ModernApplicationProtocol.NpcRequest(72,request));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.NpcRequest(72,request),20));
            for(int lane=0;lane<2;lane++) {
                var r=read(f,hit.frame(),lane);wire(r);var world=r.reply().frame().combat().world();var q=world.quest();
                assertEquals(ModernWorldProtocol.Phase.FINISHED,world.phase());assertEquals(2,world.completion().event().mode());
                assertEquals(2,world.completion().event().participants().size());assertEquals(1,world.actors().size());
                assertEquals(lane+1L,world.actors().get(0).entityId());assertEquals(0,q.winningLane());
                assertEquals(world.revision(),q.deathRevision());assertTrue(q.targets().stream().allMatch(t->t.lane()==laneIndex(q)));
                assertEquals(lane==0?1:0,q.actors().size());assertEquals(lane==0?1:0,q.exchanges().size());
            }
            if(Boolean.getBoolean("lito.quest.printGolden")) {
                System.out.println("QUEST_V21_BEFORE="+HexFormat.of().formatHex(wire(before)));
                System.out.println("QUEST_V21_WIN="+HexFormat.of().formatHex(wire(response)));
                System.out.println("QUEST_V21_LOSS="+HexFormat.of().formatHex(wire(read(f,hit.frame(),1))));
            }
        }
    }
    @Test void objectiveRequestHasItsOwnNamespaceAndCannotUseNpcLifeOrOldWire()throws Exception {
        try(var f=ModernQuestObjectivesTest.fixture(554,1,false)) {
            var scope=f.players.get(0).scope();
            for(long slot:List.of(0L,9L,Long.MAX_VALUE))assertThrows(IllegalArgumentException.class,()->new ModernNpcProtocol.Request(scope,ModernNpcProtocol.Action.ATTACK_OBJECTIVE,f.scope,1,slot,0));
            assertThrows(IllegalArgumentException.class,()->new ModernNpcProtocol.Request(scope,ModernNpcProtocol.Action.ATTACK_OBJECTIVE,f.scope,1,4,1));
            var r=new ModernNpcProtocol.Request(scope,ModernNpcProtocol.Action.ATTACK_OBJECTIVE,f.scope,1,4,0);
            assertThrows(IllegalArgumentException.class,r::target);assertEquals(new ModernQuestObjectives.Id(f.scope,4),r.objective());
            var b=ByteBuffer.allocate(72);ModernNpcProtocol.putRequest(b,r,21);b.flip();assertEquals(r,ModernNpcProtocol.getRequest(b,21));
            b.rewind();assertThrows(IllegalArgumentException.class,()->ModernNpcProtocol.getRequest(b,20));
        }
    }
    @Test void boundedAppendixRejectsEveryTruncationReservedBitsAndHostileCount()throws Exception {
        try(var f=ModernQuestObjectivesTest.fixture(513,Long.MAX_VALUE,false)) {
            var q=frame(f,f.view(),0).combat().world().quest();var out=ByteBuffer.allocate(8192);ModernQuestProtocol.put(out,q);
            var bytes=java.util.Arrays.copyOf(out.array(),out.position());assertEquals(q,ModernQuestProtocol.get(ByteBuffer.wrap(bytes)));
            for(int end=0;end<bytes.length;end++){var cut=java.util.Arrays.copyOf(bytes,end);assertThrows(RuntimeException.class,()->ModernQuestProtocol.get(ByteBuffer.wrap(cut)));}
            for(int pos:List.of(0,1,2,3,4,5,6,7,24,25,31,35,38,39,41,42,43)) {
                var invalid=bytes.clone();invalid[pos]=(byte)255;
                assertThrows(RuntimeException.class,()->ModernQuestProtocol.get(ByteBuffer.wrap(invalid)),"byte "+pos);
            }
        }
    }
    @Test void v20PeerCannotNegotiateResultsAndCompletionCannotBeAttachedWithoutTheObjectiveContract()throws Exception {
        var d=descriptor();assertTrue(d.settlementEnabled());assertFalse(d.supportedBy(20,ModernSessionProtocol.KNOWN_CAPABILITIES));assertTrue(d.supportedBy(21,ModernSessionProtocol.KNOWN_CAPABILITIES));
        assertThrows(IllegalArgumentException.class,()->new ModernMatchDescriptor(d.ruleset(),d.features()&~ModernMatchDescriptor.ISOLATED_FORCES,2000,2000,0,60000));
        var world=ModernQuestObjectivesTest.finish(554,1);var full=ModernWorldService.fromEngine(world,world.revision()+1,d);
        assertThrows(IllegalArgumentException.class,()->new ModernWorldProtocol.Frame(full.matchId(),full.generation(),full.revision(),full.phase(),full.abortReason(),full.room(),full.manifest(),full.actors(),full.completion(),full.casts(),d,full.race(),-1));
    }
}
