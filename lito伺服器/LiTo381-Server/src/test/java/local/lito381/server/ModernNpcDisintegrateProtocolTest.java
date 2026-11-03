package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcDisintegrateState.Reason.*;
import java.io.ByteArrayInputStream;
import java.util.*;
import org.junit.jupiter.api.Test;

class ModernNpcDisintegrateProtocolTest {
    static ModernApplicationProtocol.NpcDisintegrateResult message(ModernBattleEngineNpcTest.Fixture f,int who,ModernNpcDisintegrateState.Request r,ModernNpcDisintegrateState.Result result) {
        return new ModernApplicationProtocol.NpcDisintegrateResult(91,r,new ModernNpcDisintegrateProtocol.Reply(result.reason()==NONE?ModernNpcProtocol.Status.OK:ModernNpcProtocol.Status.DENIED,
                result.reason(),ModernNpcDisintegrateFrameTest.frame(f,result.frame(),who)));
    }
    static final Map<String,String> HASHES=Map.ofEntries(
        Map.entry("quest-hit","bc36bd0a2368cb03c92a457374b9edf2d1f179e4051a80b0cdfabbf5e5bba268"),
        Map.entry("normal-request","726e8b02d6e3c6e9a4a9f6360e1a14417b7e671de8f8bd659c5849639e68a073"),
        Map.entry("normal-hit","08edeca43c4762e89dbf398b06939b422b775bd379402a180a1fcaca8909b0e0"),
        Map.entry("normal-denied","d3be126d173dfdc13df1c67f6318e7fc661298161dd49aa3a1ec609fcac3cb99"),
        Map.entry("normal-second","086657e64ddda6633bcd9de2d696bbfebf6d0d6f2e26fb17ab85bce0467e158d"),
        Map.entry("critical-request","726e8b02d6e3c6e9a4a9f6360e1a14417b7e671de8f8bd659c5849639e68a073"),
        Map.entry("critical-hit","7f52977064d6d58f038d932ce876ebbca29129ee02f44d074689be37adb9ed16"),
        Map.entry("critical-denied","f9b8dfadeebb173da1cc690be7d5d59d906ba939546b4fcdfa91d96d48116e55"),
        Map.entry("critical-second","fc91ff2d966e6d87eacd1bc9cf052f245a7cb066220ec855ebcc56f780395a9b"),
        Map.entry("wide-request","969c3075609b2b89ac5c881408ccc07882d26957578543b6ad3d00e669ef9db3"),
        Map.entry("unavailable","bc8c012dadf8451bff537db924321d2390272a57229b7a83056d4af937f76a63"),
        Map.entry("time-attack-finished","d959b42e44d035deb937df10355b35416327d5257d8641b78ccfb5dd4e404044"),
        Map.entry("time-attack-inactive","fc4d3cf1ad08be973e0f2e30eb0b2b6743126bf637b37d997d6c3a99802dd778"));
    static byte[] wire(String name,ModernApplicationProtocol.Message message)throws Exception {
        var bytes=ModernApplicationProtocol.encode(message,29);assertEquals(message,ModernApplicationProtocol.decode(bytes,29));
        assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),29));
        assertEquals(HASHES.get(name),HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)),name);
        return bytes;
    }
    @Test void skillCatalogSupportsNpcOnlyNegotiationButNeverLeaksToUnnegotiatedPeers()throws Exception {
        var skills=new ModernApplicationProtocol.SkillsResult(91,new ModernSpellProtocol.Context(7,3333,1),
                new ModernSpellProtocol.SkillsReply(ModernSpellProtocol.Status.OK,List.of(new ModernSkills.Skill(77,"$1485",70,5000,ModernSkills.Support.DISINTEGRATE))));
        int caps=(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_NPC_FIRE_STORM))&~ModernSessionProtocol.CAP_DISINTEGRATE;
        var npcOnly=new ModernSessionBoundary.Negotiated(1,29,1,caps);
        assertEquals(skills,ModernMatchDescriptor.forPeer(skills,npcOnly));
        for(int version:List.of(27,28,29)) {
            var noNpc=new ModernSessionBoundary.Negotiated(1,version,1,caps&~ModernSessionProtocol.CAP_NPC_DISINTEGRATE);
            var denied=(ModernApplicationProtocol.SkillsResult)ModernMatchDescriptor.forPeer(skills,noNpc);
            assertEquals(ModernSkills.Support.UNIMPLEMENTED,denied.reply().skills().get(0).support());
            assertEquals(70,denied.reply().skills().get(0).mpCost());
        }
    }
    @Test void realEngineRequestsSuccessCooldownRecastAndUnavailableHaveDistinctKinds()throws Exception {
        for(boolean critical:List.of(false,true))try(var f=ModernBattleEngineNpcDisintegrateTest.fixture()) {
            f.active();var target=f.spawn(0,11,10);f.randomValue=critical?0:99;String prefix=critical?"critical":"normal";
            var r=ModernBattleEngineNpcDisintegrateTest.request(f,0,1,target);var request=wire(prefix+"-request",new ModernApplicationProtocol.NpcDisintegrateRequest(91,r));assertEquals(96,request.length);
            var result=f.engine.disintegrateNpc(f.players.get(0).owner(),r);assertEquals(NONE,result.reason());wire(prefix+"-hit",message(f,0,r,result));
            var fast=ModernBattleEngineNpcDisintegrateTest.request(f,0,2,target);var denied=f.engine.disintegrateNpc(f.players.get(0).owner(),fast);assertEquals(TOO_FAST,denied.reason());wire(prefix+"-denied",message(f,0,fast,denied));
            f.elapsed(5000);var next=ModernBattleEngineNpcDisintegrateTest.request(f,0,3,target);wire(prefix+"-second",message(f,0,next,f.engine.disintegrateNpc(f.players.get(0).owner(),next)));
        }
        var r=new ModernNpcDisintegrateState.Request(new ModernRooms.Scope(7,9007199254740993L,-1,3),new ModernNpcRoster.Id(new ModernNpcRoster.Scope(11,12,13),23),17,19,29);
        assertEquals(96,wire("wide-request",new ModernApplicationProtocol.NpcDisintegrateRequest(-1,r)).length);
        assertEquals(100,wire("unavailable",new ModernApplicationProtocol.NpcDisintegrateResult(-1,r,new ModernNpcDisintegrateProtocol.Reply(ModernNpcProtocol.Status.UNAVAILABLE,NOT_ACTIVE,null))).length);
    }
    @Test void sparseQuestSecondEntityHasExplicitSourceAndIndependentLife()throws Exception {
        try(var f=ModernBattleEngineNpcDisintegrateTest.fixture(true,100,2,0,true,ModernDisintegrateCatalog.Status.QUALIFIED,
                ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,1L),true,0),s->ModernBattleEngineNpcDisintegrateTest.source(s,1000,500))) {
            f.active();var target=ModernQuestEngineTest.living(f,1);var r=ModernBattleEngineNpcDisintegrateTest.request(f,1,1,target);
            var m=message(f,1,r,f.engine.disintegrateNpc(f.players.get(1).owner(),r));assertEquals(2,m.reply().frame().combat().npcDisintegrations().get(0).entityId());wire("quest-hit",m);
            var wrong=new ModernNpcDisintegrateState.Request(f.players.get(0).scope(),r.target(),r.sequence(),r.casterLife(),r.targetLife());
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.NpcDisintegrateResult(91,wrong,m.reply()),29));
        }
    }
    @Test void terminalTimeAttackSuccessIsNotMistakenForNotActiveDenial()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(2,8,100,true,1,true)) {
            f.active();f.tower(0,1);ModernNpcDisintegrateState.Request r=null;
            for(int i=0;i<f.plan.waves().size();i++){f.elapsed(1000+5000L*i);var target=f.living();r=new ModernNpcDisintegrateState.Request(f.players.get(0).scope(),target.id(),i+1,1,target.life());assertEquals(NONE,f.engine.disintegrateNpc(f.players.get(0).owner(),r).reason());}
            var value=f.view();var e=f.engine.timeAttackResult();e=new ModernMatchSettlement.Event(UUID.fromString("12345678-1234-5678-9abc-def012345678"),e.matchId(),e.generation(),e.engineRevision(),e.mode(),e.mapId(),e.finishedAtMillis(),e.participants(),e.timeAttack());
            var nf=ModernTimeAttackProtocol.frame(value,ModernNpcDisintegrateFrameTest.descriptor(value.world()),e,ModernWorldProtocol.SettlementState.PENDING,new ModernNpcProtocol.Resources(List.of(value.npcs().actors().get(0).template().graphic())))
                    .withNames(ModernNpcNames.Table.from(ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,1L),true,0)));
            wire("time-attack-finished",new ModernApplicationProtocol.NpcDisintegrateResult(91,r,new ModernNpcDisintegrateProtocol.Reply(ModernNpcProtocol.Status.OK,NONE,nf)));
            var next=new ModernNpcDisintegrateState.Request(r.player(),r.target(),r.sequence()+1,r.casterLife(),r.targetLife());
            wire("time-attack-inactive",new ModernApplicationProtocol.NpcDisintegrateResult(91,next,new ModernNpcDisintegrateProtocol.Reply(ModernNpcProtocol.Status.DENIED,NOT_ACTIVE,nf)));
        }
    }
    @Test void fakeSuccessWrongReasonSequenceLivesScopeAndOldVersionsCannotPass()throws Exception {
        try(var f=ModernBattleEngineNpcDisintegrateTest.fixture()) {
            f.active();var target=f.spawn(0,11,10);var r=ModernBattleEngineNpcDisintegrateTest.request(f,0,1,target);var m=message(f,0,r,f.engine.disintegrateNpc(f.players.get(0).owner(),r));
            for(var reason:List.of(DEAD,NO_MP,LAWFUL,TARGET_DEAD,NOT_ACTIVE)) {
                var bad=new ModernApplicationProtocol.NpcDisintegrateResult(91,r,new ModernNpcDisintegrateProtocol.Reply(ModernNpcProtocol.Status.DENIED,reason,m.reply().frame()));
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(bad,29),reason.name());
            }
            for(int kind=0;kind<4;kind++) {
                var scope=r.target().scope();var id=kind==3?new ModernNpcRoster.Id(new ModernNpcRoster.Scope(scope.roomId(),scope.matchId(),scope.generation()+1),target.id().serial()):r.target();
                var altered=new ModernNpcDisintegrateState.Request(r.player(),id,kind==0?2:1,kind==1?2:1,kind==2?2:1);
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.NpcDisintegrateResult(91,altered,m.reply()),29));
            }
            var bytes=ModernApplicationProtocol.encode(m,29);
            for(int v=1;v<29;v++){final int version=v;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(m,version));assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,version));}
            assertThrows(IllegalArgumentException.class,()->new ModernNpcDisintegrateProtocol.Reply(ModernNpcProtocol.Status.BUSY,TOO_FAST,m.reply().frame()));
        }
    }
    @Test void completeApplicationLengthReservedStatusReasonAndTruncationAreStrict()throws Exception {
        try(var f=ModernBattleEngineNpcDisintegrateTest.fixture()) {
            f.active();var target=f.spawn(0,11,10);var r=ModernBattleEngineNpcDisintegrateTest.request(f,0,1,target);var m=message(f,0,r,f.engine.disintegrateNpc(f.players.get(0).owner(),r));var bytes=ModernApplicationProtocol.encode(m,29);
            for(int n=0;n<bytes.length;n++){var shortFrame=Arrays.copyOf(bytes,n);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(shortFrame,29));}
            for(int offset:List.of(15,37,38,39,96,97,98,99)){var bad=bytes.clone();bad[offset]=(byte)255;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,29));}
            var extra=Arrays.copyOf(bytes,bytes.length+1);java.nio.ByteBuffer.wrap(extra).putInt(4,extra.length);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(extra,29));
        }
    }
    @Test void capabilityIsV29ExplicitDependencyCheckedAndNotRuntimeAdvertised()throws Exception {
        int caps=(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_NPC_FIRE_STORM));assertEquals(0,ModernSessionProtocol.RUNTIME_CAPABILITIES&ModernSessionProtocol.CAP_NPC_DISINTEGRATE);
        for(int bit:List.of(ModernSessionProtocol.CAP_NPC,ModernSessionProtocol.CAP_REGEN,ModernSessionProtocol.CAP_MATCH_DESCRIPTOR,ModernSessionProtocol.CAP_COMBAT)) {
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,29,29,caps&~bit,0,1)));
        }
        for(int version=1;version<29;version++){final int v=version;assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,v,v,caps,0,1)));assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(1,v,1,caps)));}
        assertTrue(ModernNpcDisintegrateProtocol.negotiated(new ModernSessionBoundary.Negotiated(1,29,1,caps)));
        assertFalse(ModernNpcDisintegrateProtocol.negotiated(new ModernSessionBoundary.Negotiated(1,28,1,caps)));
    }
}
