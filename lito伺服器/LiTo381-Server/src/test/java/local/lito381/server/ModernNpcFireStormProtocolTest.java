package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcFireStormState.Reason.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.*;
import org.junit.jupiter.api.Test;

class ModernNpcFireStormProtocolTest {
    static ModernApplicationProtocol.NpcFireStormResult result(ModernRooms.Scope player,ModernNpcProtocol.Frame frame,long sequence,long life,ModernNpcFireStormState.Reason reason) {
        return new ModernApplicationProtocol.NpcFireStormResult(91,new ModernNpcFireStormState.Request(player,frame.scope(),sequence,life),
                new ModernNpcFireStormProtocol.Reply(reason==NONE?ModernNpcProtocol.Status.OK:ModernNpcProtocol.Status.DENIED,reason,frame));
    }
    static byte[] roundtrip(ModernApplicationProtocol.Message message)throws Exception {
        var bytes=ModernApplicationProtocol.encode(message,31);
        assertEquals(message,ModernApplicationProtocol.decode(bytes,31));
        assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),31));return bytes;
    }
    static ModernApplicationProtocol.NpcResult read(ModernNpcFireStormFramePeer.Row row) {
        return ModernNpcDisintegrateFrameTest.message(row.player(),row.frame());
    }
    @Test void exactRequestAndAbsentReplyKeepSourceObjidEpochAndIndependentKinds()throws Exception {
        var r=new ModernNpcFireStormState.Request(new ModernRooms.Scope(7,9007199254740993L,-1,3),new ModernNpcRoster.Scope(11,12,13),17,19);
        var q=roundtrip(new ModernApplicationProtocol.NpcFireStormRequest(-1,r));assertEquals(80,q.length);assertEquals(48,ByteBuffer.wrap(q).getShort(12));
        assertEquals("000000070020000000000001ffffffffffffffff03000000000000000000000b000000000000000c000000000000000d00000000000000110000000000000013",HexFormat.of().formatHex(Arrays.copyOfRange(q,16,80)));
        var m=new ModernApplicationProtocol.NpcFireStormResult(-1,r,new ModernNpcFireStormProtocol.Reply(ModernNpcProtocol.Status.UNAVAILABLE,NOT_ACTIVE,null));
        assertEquals(m,ModernMatchDescriptor.forPeer(m,new ModernSessionBoundary.Negotiated(1,31,1,ModernSessionProtocol.KNOWN_CAPABILITIES)));
        assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(m,new ModernSessionBoundary.Negotiated(1,31,1,ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_NPC_FIRE_STORM))));
        var bytes=roundtrip(m);assertEquals(84,bytes.length);assertEquals(49,ByteBuffer.wrap(bytes).getShort(12));
        for(int v=1;v<=30;v++){int version=v;assertThrows(Exception.class,()->ModernApplicationProtocol.encode(m,version));assertThrows(Exception.class,()->ModernApplicationProtocol.decode(bytes,version));assertThrows(Exception.class,()->ModernApplicationProtocol.decode(q,version));}
        assertEquals(m,ModernApplicationProtocol.decode(bytes,32));
        assertArrayEquals(bytes,ModernApplicationProtocol.encode(m,32));
        assertArrayEquals(q,ModernApplicationProtocol.encode(new ModernApplicationProtocol.NpcFireStormRequest(-1,r),32));
        assertThrows(Exception.class,()->ModernApplicationProtocol.decode(bytes,ModernSessionProtocol.MAX_VERSION+1));
    }
    @Test void engineSuccessCooldownNoMpUnlearnedAndUnavailableCannotBecomeFalseSuccess()throws Exception {
        for(int variant=0;variant<3;variant++)try(var f=ModernBattleEngineNpcFireStormTest.fixture(variant==1?0:1000,10000,0,variant!=2,false,false,0)) {
            f.active();f.spawn(0,11,10);var r=ModernBattleEngineNpcFireStormTest.request(f,1);
            var actual=f.engine.fireStormNpc(f.players.get(0).owner(),r);var reason=variant==0?NONE:variant==1?NO_MP:UNLEARNED;assertEquals(reason,actual.reason());
            var m=result(r.player(),ModernNpcFireStormFrameTest.frame(f,0),1,1,reason);roundtrip(m);
            if(variant!=0)assertThrows(Exception.class,()->roundtrip(result(r.player(),m.reply().frame(),1,1,NONE)));
            else {assertEquals(TOO_FAST,ModernBattleEngineNpcFireStormTest.cast(f,2).reason());roundtrip(result(r.player(),ModernNpcFireStormFrameTest.frame(f,0),2,1,TOO_FAST));}
        }
    }
    @Test void privateSecondEntityAndAllQuestMagicTerminalsHaveCorrectSourceProof()throws Exception {
        for(int map:BattleRules.Mode.QUEST_BATTLE.maps())try(var f=ModernNpcFireStormStructuresTest.quest(map,1)) {
            f.active();long sequence=0;
            while(f.view().world().phase()==ModernBattleEngine.Phase.ACTIVE) {
                f.elapsed(sequence*2000);assertEquals(NONE,ModernNpcFireStormStructuresTest.cast(f,1,++sequence).reason());
                var frame=ModernNpcFireStormFrameTest.frame(f,1);roundtrip(result(f.players.get(1).scope(),frame,sequence,1,NONE));
                assertEquals(2,frame.combat().npcFireStorms().get(0).entityId());
                long seq=sequence;assertThrows(Exception.class,()->roundtrip(result(f.players.get(0).scope(),frame,seq,1,NONE)));
            }
        }
    }
    @Test void timeTowerFourWavesTerminalAndSubsequentNotActiveAreDistinct()throws Exception {
        try(var f=ModernNpcFireStormStructuresTest.time(1,2)) {
            f.active();for(int i=0;i<5;i++) {f.elapsed(i*2000L);assertEquals(NONE,ModernNpcFireStormStructuresTest.cast(f,0,i+1).reason());roundtrip(result(f.players.get(0).scope(),ModernNpcFireStormFrameTest.frame(f),i+1,1,NONE));}
            assertEquals(ModernBattleEngine.Phase.FINISHED,f.view().world().phase());
            var n=ModernNpcFireStormFrameTest.frame(f);roundtrip(result(f.players.get(0).scope(),n,6,1,NOT_ACTIVE));
            assertThrows(Exception.class,()->roundtrip(result(f.players.get(0).scope(),n,6,1,NONE)));
        }
    }
    @Test void malformedHeaderReplyFlagsContextRevisionAndEveryTruncationAreRejected()throws Exception {
        var row=ModernNpcFireStormFramePeer.normal().stream().filter(r->r.name().equals("hit")).findFirst().orElseThrow();var m=result(row.player(),row.frame(),1,1,NONE);var bytes=roundtrip(m);
        for(int n=0;n<bytes.length;n++){var shortBytes=Arrays.copyOf(bytes,n);assertThrows(Exception.class,()->ModernApplicationProtocol.decode(shortBytes,31));}
        for(int offset:List.of(15,37,38,39,80,81,82,83)){var bad=bytes.clone();bad[offset]=(byte)255;assertThrows(Exception.class,()->ModernApplicationProtocol.decode(bad,31));}
        for(var reason:List.of(NOT_ACTIVE,DEAD,NO_MP))assertThrows(Exception.class,()->roundtrip(result(row.player(),row.frame(),1,1,reason)));
        assertThrows(Exception.class,()->roundtrip(result(row.player(),row.frame(),2,1,NONE)));
        assertThrows(Exception.class,()->roundtrip(result(row.player(),row.frame(),1,2,NONE)));
        var extra=Arrays.copyOf(bytes,bytes.length+1);ByteBuffer.wrap(extra).putInt(4,extra.length);assertThrows(Exception.class,()->ModernApplicationProtocol.decode(extra,31));
        assertThrows(Exception.class,()->new ModernNpcFireStormProtocol.Reply(ModernNpcProtocol.Status.BUSY,TOO_FAST,row.frame()));
    }
    static List<ModernApplicationProtocol.Message> largeMessages()throws Exception {
        var row=ModernNpcFireStormFramePeer.wide().get(0);var n=row.frame();var c=n.combat();var w=c.world();var d=w.descriptor();
        // Extra empty NPC77 ledgers allow every old NPC reply kind to carry the
        // same large NPC70 history without fabricating a second successful cast.
        var descriptor=new ModernMatchDescriptor(d.ruleset(),d.features()|ModernMatchDescriptor.NPC_DISINTEGRATE,d.preparationTimeoutMillis(),d.loadTimeoutMillis(),d.countdownNanos(),d.activeTimeoutMillis());
        var world=w.withDescriptor(descriptor);
        var states=w.actors().stream().map(a->ModernNpcDisintegrateState.State.initial(a.entityId(),70,500)).toList();
        n=n.withCombat(new ModernCombatProtocol.Frame(world,c.vitals(),c.damages(),c.items(),c.weights(),c.buffs(),c.ammo(),c.regen(),c.honor(),c.fireStorms(),c.npcSpells(),c.disintegrations(),states,c.npcFireStorms()));
        var r=row.player();var scope=n.scope();
        return List.of(read(new ModernNpcFireStormFramePeer.Row("large-read",r,n)),
                new ModernApplicationProtocol.NpcSpellResult(91,new ModernNpcSpellProtocol.Request(r,scope,1,1,1,1),new ModernNpcSpellProtocol.Reply(ModernNpcProtocol.Status.DENIED,ModernNpcMagic.Reason.STALE_STATE,n)),
                new ModernApplicationProtocol.NpcDisintegrateResult(91,new ModernNpcDisintegrateState.Request(r,new ModernNpcRoster.Id(scope,1),1,1,1),new ModernNpcDisintegrateProtocol.Reply(ModernNpcProtocol.Status.DENIED,ModernNpcDisintegrateState.Reason.STALE_STATE,n)),
                result(r,n,1,1,STALE_STATE));
    }
    @Test void everyNpcReplyKindCarriesMoreThanLegacyLimitWithoutDroppingHistory()throws Exception {
        for(var message:largeMessages()) {
            var bytes=roundtrip(message);assertTrue(bytes.length>320*1024);assertTrue(bytes.length<1024*1024);
            assertThrows(Exception.class,()->ModernApplicationProtocol.decode(bytes,30));
            assertThrows(Exception.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),30));
        }
    }
    @Test void versionKindAndLengthAreRejectedBeforeReadingAnyBody()throws Exception {
        for(int kind:List.of(39,41,47,49)) {
            var header=ByteBuffer.allocate(16).putInt(0x4c544d31).putInt(256*1024+1).putInt(1).putShort((short)kind).putShort((short)0).array();
            class HeaderOnly extends ByteArrayInputStream { HeaderOnly(){super(header);} @Override public synchronized int read(byte[] b,int off,int len){assertTrue(pos<16,"body read despite invalid negotiated header");return super.read(b,off,len);} }
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new HeaderOnly(),30));
            ByteBuffer.wrap(header).putInt(4,1024*1024+1);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new HeaderOnly(),31));
        }
        var bytes=roundtrip(new ModernApplicationProtocol.NpcFireStormRequest(91,new ModernNpcFireStormState.Request(new ModernRooms.Scope(1,1,1,0),new ModernNpcRoster.Scope(1,1,1),1,1)));
        var all=new byte[bytes.length*2];System.arraycopy(bytes,0,all,0,bytes.length);System.arraycopy(bytes,0,all,bytes.length,bytes.length);
        var stream=new ByteArrayInputStream(all);assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(stream,31));assertEquals(bytes.length,stream.available());assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(stream,31));assertNull(ModernApplicationProtocol.readFrame(stream,31));
    }
}
