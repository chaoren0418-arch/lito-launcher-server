package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcCatalog.Field.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class ModernNpcProtocolTest {
    static final long HUGE=9_007_199_254_740_993L;
    static ModernNpcCatalog.Catalog futureCatalog(){
        var catalog=ModernNpcMeleeTest.catalog();var first=catalog.npcs().get(ModernNpcMeleeTest.NPC);
        var values=new java.util.HashMap<>(first.facts().values());values.put(GRAPHIC,2158L);
        var second=new ModernNpcCatalog.Npc(new ModernNpcCatalog.Key(catalog.sourceId(),catalog.database(),first.key().npcId()+1),
                first.name(),first.nameId(),first.implementation(),first.large(),new ModernNpcCatalog.Facts(values),false);
        return new ModernNpcCatalog.Catalog(catalog.sourceId(),catalog.database(),Map.of(first.key().npcId(),first,second.key().npcId(),second),catalog.growth());
    }
    static ModernMatchDescriptor descriptor(ModernBattleEngineNpcTest.Fixture f){
        int features=ModernMatchDescriptor.NPC|ModernMatchDescriptor.COMBAT;
        if(f.prepared.get(0).regenEnabled())features|=ModernMatchDescriptor.ITEMS|ModernMatchDescriptor.WEIGHT|ModernMatchDescriptor.REGEN;
        return new ModernMatchDescriptor(ModernMatchDescriptor.Ruleset.NPC_ENCOUNTER,features,2000,f.limits.loadTimeoutMillis(),f.limits.countdownNanos(),f.limits.activeTimeoutMillis());
    }
    static ModernNpcProtocol.Request request(ModernBattleEngineNpcTest.Fixture f,long sequence,long serial,long life){return new ModernNpcProtocol.Request(f.players.get(0).scope(),
            sequence==0?ModernNpcProtocol.Action.READ:ModernNpcProtocol.Action.ATTACK,f.scope,sequence,serial,life);}
    static ModernApplicationProtocol.NpcResult read(ModernBattleEngineNpcTest.Fixture f){return new ModernApplicationProtocol.NpcResult(0xfedcba98,request(f,0,0,0),
            new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,ModernNpcProtocol.Frame.from(f.view(),descriptor(f))));}
    static void roundtrip(ModernApplicationProtocol.Message value)throws Exception{assertEquals(value,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(value,17),17));}
    static ModernApplicationProtocol.NpcResult resources(ModernApplicationProtocol.NpcResult old){return new ModernApplicationProtocol.NpcResult(old.requestId(),old.echo(),
            new ModernNpcProtocol.Reply(old.reply().status(),old.reply().reason(),old.reply().frame().withResources(ModernNpcProtocol.Resources.from(futureCatalog()))));}
    @Test void versionEighteenPreflightIncludesUnspawnedCatalogFormsWithoutChangingLegacyPayload()throws Exception {
        try(var f=new ModernBattleEngineNpcTest.Fixture()) {
            var legacy=read(f);var planned=resources(legacy);var bytes=ModernApplicationProtocol.encode(planned,18);var old=ModernApplicationProtocol.encode(legacy,17);
            assertTrue(planned.reply().frame().actors().isEmpty());assertEquals(List.of(1649,2158),planned.reply().frame().resources().graphics());
            assertThrows(UnsupportedOperationException.class,()->planned.reply().frame().resources().graphics().clear());
            assertEquals(old.length+16,bytes.length);ByteBuffer.wrap(old).putInt(4,bytes.length);
            assertArrayEquals(old,Arrays.copyOf(bytes,old.length));assertEquals("0001000200000000000006710000086e",HexFormat.of().formatHex(Arrays.copyOfRange(bytes,old.length,bytes.length)));
            assertEquals(planned,ModernApplicationProtocol.decode(bytes,18));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(planned,17));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(legacy,18));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,17));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(legacy,17),18));
        }
    }
    @Test void resourcePreflightRejectsMissingResidentAndUnboundedUnsortedOrUnknownForms()throws Exception {
        for(var list:List.of(List.<Integer>of(),List.of(1649,1649),List.of(2158,1649),List.of(9999)))
            assertThrows(IllegalArgumentException.class,()->new ModernNpcProtocol.Resources(list));
        try(var f=new ModernBattleEngineNpcTest.Fixture()) {
            f.active();f.spawn(0,11,10);var frame=read(f).reply().frame();
            assertThrows(IllegalArgumentException.class,()->frame.withResources(new ModernNpcProtocol.Resources(List.of(2158))));
            var message=resources(read(f));var bytes=ModernApplicationProtocol.encode(message,18);int base=bytes.length-16;
            for(int field:new int[]{0,2,4,8,12}) {
                var bad=bytes.clone();var buffer=ByteBuffer.wrap(bad);
                switch(field){case 0->buffer.putShort(base,(short)2);case 2->buffer.putShort(base+2,(short)11);case 4->buffer.putInt(base+4,1);
                    case 8->buffer.putInt(base+8,2158);case 12->buffer.putInt(base+12,9999);}
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,18));
            }
            for(int length=bytes.length-16;length<bytes.length;length++) {
                var truncated=Arrays.copyOf(bytes,length);ByteBuffer.wrap(truncated).putInt(4,length);
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(truncated,18));
            }
        }
    }
    @Test void namespaceDependenciesAndDowngradeNeverAdvertiseRuntimeNpcBeforeProviderExists()throws Exception {
        assertEquals(262144,ModernSessionProtocol.CAP_NPC);assertEquals(2097151,ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_ACTION_LIFE|ModernSessionProtocol.CAP_LARGE_MAPS|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_MATCHMAKING|ModernSessionProtocol.CAP_NPC_FIRE_STORM|ModernSessionProtocol.CAP_CHAT|ModernSessionProtocol.CAP_DIRECT_CHAT|ModernSessionProtocol.CAP_FILTERED_CHAT));assertEquals(262143,ModernSessionProtocol.RUNTIME_CAPABILITIES);
        for(var p:List.of(ModernSessionBoundary.Policy.current(),ModernSessionBoundary.Policy.equipment(),ModernSessionBoundary.Policy.rooms(true),
                ModernSessionBoundary.Policy.world(true,true,true,true,true,true,true,true,true,true)))assertEquals(0,p.supportedCapabilities()&ModernSessionProtocol.CAP_NPC);
        for(int version=1;version<=17;version++) {
            var p=new ModernSessionBoundary.Policy(ModernSessionProtocol.KNOWN_CAPABILITIES,3,version);var b=new ModernSessionBoundary(p);
            var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(b.acceptHello(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,17,ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_ACTION_LIFE|ModernSessionProtocol.CAP_LARGE_MAPS|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_MATCHMAKING|ModernSessionProtocol.CAP_CHAT|ModernSessionProtocol.CAP_DIRECT_CHAT|ModernSessionProtocol.CAP_FILTERED_CHAT|ModernSessionProtocol.CAP_DISINTEGRATE|ModernSessionProtocol.CAP_NPC_DISINTEGRATE|ModernSessionProtocol.CAP_NPC_FIRE_STORM),3,1))));
            assertEquals(version,ack.version());assertEquals(version==17?ModernSessionProtocol.CAP_NPC:0,ack.capabilities()&ModernSessionProtocol.CAP_NPC);
        }
        for(int bit:new int[]{ModernSessionProtocol.CAP_COMBAT,ModernSessionProtocol.CAP_MATCH_DESCRIPTOR})assertThrows(ModernSessionProtocol.ProtocolException.class,()->
                ModernSessionProtocol.validateCapabilities(ModernSessionProtocol.KNOWN_CAPABILITIES&~bit,0));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,16,ModernSessionProtocol.KNOWN_CAPABILITIES,3,1)));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(1,16,1,ModernSessionProtocol.KNOWN_CAPABILITIES)));
        assertThrows(IllegalArgumentException.class,()->new ModernSessionBoundary.Policy(ModernSessionProtocol.KNOWN_CAPABILITIES,ModernSessionProtocol.KNOWN_CAPABILITIES,16));
    }
    @Test void exactSeventyTwoByteRequestHasIndependentNpcScopeAndNoDamageOrTimeFields()throws Exception {
        var r=new ModernNpcProtocol.Request(new ModernRooms.Scope(42,Long.MAX_VALUE,-1,3),ModernNpcProtocol.Action.ATTACK,new ModernNpcRoster.Scope(11,22,33),Long.MAX_VALUE,44,55);
        var bytes=ModernApplicationProtocol.encode(new ModernApplicationProtocol.NpcRequest(0xfedcba98,r),17);assertEquals(88,bytes.length);roundtrip(new ModernApplicationProtocol.NpcRequest(0xfedcba98,r));
        assertEquals("4c544d3100000058fedcba98002600000000002a7fffffffffffffffffffffffffffffff03010000000000000000000b000000000000001600000000000000217fffffffffffffff000000000000002c0000000000000037",HexFormat.of().formatHex(bytes));
        assertEquals(new ModernNpcRoster.Id(r.world(),44),r.target());
        for(int version=1;version<17;version++){int v=version;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,v));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.NpcRequest(1,r),v));}
    }
    @Test void engineSnapshotsRoundtripThroughLoadingActiveBidirectionalCombatAndAbort()throws Exception {
        try(var f=new ModernBattleEngineNpcTest.Fixture()) {
            roundtrip(read(f));f.active();var npc=f.spawn(0,11,10);roundtrip(read(f));f.elapsed(100);f.step(npc);roundtrip(read(f));
            var struck=f.attack(0,1,npc);var message=new ModernApplicationProtocol.NpcResult(1,request(f,1,npc.id().serial(),npc.life()),
                    new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,ModernNpcProtocol.Frame.from(struck.frame(),descriptor(f))));roundtrip(message);
            assertEquals(message.reply().frame().combat().world().revision(),message.reply().frame().exchanges().get(1).revision());
            f.engine.abort();roundtrip(read(f));assertTrue(read(f).reply().frame().closed());
        }
    }
    @Test void validDenialConsumesRequestButNotLastStrikeAndUnavailableIsExplicit()throws Exception {
        try(var f=new ModernBattleEngineNpcTest.Fixture()) {
            f.active();var npc=f.spawn(0,11,10);f.attack(0,1,npc);var denied=f.attack(0,2,npc);
            var reply=new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.DENIED,ModernNpcProtocol.Reason.TOO_FAST,ModernNpcProtocol.Frame.from(denied.frame(),descriptor(f)));
            roundtrip(new ModernApplicationProtocol.NpcResult(1,request(f,2,npc.id().serial(),1),reply));
            roundtrip(new ModernApplicationProtocol.NpcResult(1,request(f,0,0,0),new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.UNAVAILABLE,ModernNpcProtocol.Reason.NOT_ACTIVE,null)));
            assertThrows(IllegalArgumentException.class,()->new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,null));
        }
    }
    @Test void regenAndBotDefenseUseExistingCombatLedgerInsideSameCompleteFrame()throws Exception {
        try(var f=new ModernBattleEngineNpcTest.Fixture(1,1,true,100,0,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(),true)) {
            f.active();var npc=f.spawn(1,21,10);f.elapsed(100);f.step(npc);var message=read(f);roundtrip(message);
            var frame=message.reply().frame();assertEquals(ModernParticipantId.bot(7,1),frame.exchanges().get(0).participant());
            assertEquals(ModernRegenProtocol.State.initial(2).damaged(frame.combat().regen().get(1).damageLoss()),frame.combat().regen().get(1));
        }
    }
    @Test void truncatedTrailingReservedOvercountAndOldVersionInputsFailWithoutFallback()throws Exception {
        try(var f=new ModernBattleEngineNpcTest.Fixture()) {
            f.active();f.spawn(0,11,10);var message=read(f);var bytes=ModernApplicationProtocol.encode(message,17);
            for(int length=0;length<bytes.length;length++){var prefix=Arrays.copyOf(bytes,length);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(prefix,17));}
            var longer=Arrays.copyOf(bytes,bytes.length+1);ByteBuffer.wrap(longer).putInt(4,longer.length);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(longer,17));
            int appendix=bytes.length-56-ModernNpcProtocol.ACTOR_BYTES;
            for(int at:new int[]{15,39,91,appendix+49,appendix+50}){var bad=bytes.clone();bad[at]=1;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,17));}
            for(int field:new int[]{appendix+52,appendix+54}){var bad=bytes.clone();ByteBuffer.wrap(bad).putShort(field,(short)65535);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,17));}
            for(int v=1;v<17;v++){int version=v;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,version));}
        }
    }
    @Test void streamReaderConsumesExactlyOneMessageAndRejectsLengthBeforePayloadRead()throws Exception {
        try(var f=new ModernBattleEngineNpcTest.Fixture()) {
            byte[] one=ModernApplicationProtocol.encode(read(f),17),both=new byte[one.length*2];System.arraycopy(one,0,both,0,one.length);System.arraycopy(one,0,both,one.length,one.length);
            var stream=new ByteArrayInputStream(both);assertArrayEquals(one,ModernApplicationProtocol.readFrame(stream,17));assertEquals(one.length,stream.available());assertArrayEquals(one,ModernApplicationProtocol.readFrame(stream,17));
            var header=Arrays.copyOf(one,16);ByteBuffer.wrap(header).putInt(4,ModernNpcProtocol.MAX_FRAME_BYTES+1);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(header),17));
        }
    }
    @Test void npcDescriptorCannotLeakIntoOlderWorldPayloadOrLoseMandatoryCapabilities()throws Exception {
        try(var f=new ModernBattleEngineNpcTest.Fixture()) {
            var m=read(f);var d=descriptor(f);assertFalse(d.supportedBy(16,ModernSessionProtocol.KNOWN_CAPABILITIES));assertFalse(d.supportedBy(17,ModernSessionProtocol.RUNTIME_CAPABILITIES));
            assertTrue(d.supportedBy(17,ModernSessionProtocol.KNOWN_CAPABILITIES));assertThrows(IllegalArgumentException.class,()->d.validateMode(1));
            assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(m,new ModernSessionBoundary.Negotiated(1,17,1,ModernSessionProtocol.RUNTIME_CAPABILITIES)));
            assertEquals(m,ModernMatchDescriptor.forPeer(m,new ModernSessionBoundary.Negotiated(1,17,1,ModernSessionProtocol.KNOWN_CAPABILITIES)));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernCombatProtocol.putFrame(ByteBuffer.allocate(10000),m.reply().frame().combat(),16));
        }
    }
    @Test void forgedSuccessCannotBorrowAReadFrameOrAnotherNpcLife()throws Exception {
        try(var f=new ModernBattleEngineNpcTest.Fixture()) {
            f.active();var npc=f.spawn(0,11,10);var before=read(f).reply();
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.NpcResult(1,request(f,1,1,1),before),17));
            var hit=f.attack(0,1,npc);var reply=new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,ModernNpcProtocol.Frame.from(hit.frame(),descriptor(f)));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.NpcResult(1,request(f,1,1,2),reply),17));
        }
    }
    @Test void completeMaxResidentNpcFrameIsBoundedAndRetainsEveryWideIdentity()throws Exception {
        try(var f=new ModernBattleEngineNpcTest.Fixture()) {
            f.active();f.spawn(0,11,10);var sample=read(f);var frame=sample.reply().frame();var a=frame.actors().get(0);var actors=new ArrayList<ModernNpcProtocol.Actor>();
            for(int i=0;i<128;i++)actors.add(new ModernNpcProtocol.Actor(i+1,88,a.npcId(),a.graphic(),i%2,1,0,a.action(),ModernBattleEngineNpcTest.cell(25+i%16,20+i/16),
                    HUGE,HUGE,Long.MAX_VALUE,Long.MAX_VALUE,1,a.bornRevision(),a.changedRevision(),0,100,0));
            var maximum=new ModernNpcProtocol.Frame(frame.combat(),frame.scope(),frame.revision(),frame.elapsedMillis(),128,false,actors,List.of());
            var message=new ModernApplicationProtocol.NpcResult(1,sample.echo(),new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,maximum));
            var encoded=ModernApplicationProtocol.encode(message,17);assertTrue(encoded.length<ModernNpcProtocol.MAX_FRAME_BYTES);roundtrip(message);
            assertEquals(128,((ModernApplicationProtocol.NpcResult)ModernApplicationProtocol.decode(encoded,17)).reply().frame().actors().size());
        }
    }
    @Test void historicalReceiptCannotReferenceAnNpcLifeThatHasNotOccurred()throws Exception {
        try(var f=new ModernBattleEngineNpcTest.Fixture()) {
            f.active();var npc=f.spawn(0,11,10);f.elapsed(100);f.step(npc);f.elapsed(101);
            var frame=read(f).reply().frame();var e=frame.exchanges().get(0);
            assertTrue(e.revision()<frame.combat().world().revision());
            var future=new ModernNpcBattle.Exchange(e.direction(),e.participant(),e.participantLife(),e.npc(),2,e.requestSequence(),e.revision(),e.outcome());
            assertThrows(IllegalArgumentException.class,()->new ModernNpcProtocol.Frame(frame.combat(),frame.scope(),frame.revision(),frame.elapsedMillis(),frame.spawned(),false,frame.actors(),List.of(future)));
        }
    }
    @Test void independentRustReferenceContainsHumanBotSameNumericIdAndWideNpcHpWithoutDatabaseNames()throws Exception {
        try(var f=new ModernBattleEngineNpcTest.Fixture(2,1,false,HUGE,0,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(Map.of(HP,HUGE),true,0),true)) {
            f.active();var first=f.spawn(0,11,10);var second=f.spawn(0,31,10);f.elapsed(100);f.step(first);f.step(second);var result=f.attack(0,1,first);
            var message=new ModernApplicationProtocol.NpcResult(0xfedcba98,request(f,1,first.id().serial(),1),new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,
                    ModernNpcProtocol.Frame.from(result.frame(),descriptor(f))));roundtrip(message);var bytes=ModernApplicationProtocol.encode(message,17);
            assertEquals(3,message.reply().frame().exchanges().size());assertEquals(2,message.reply().frame().actors().size());assertEquals(HUGE,message.reply().frame().actors().get(0).maxHp());
            String encoded=new String(bytes,StandardCharsets.ISO_8859_1);assertFalse(encoded.contains("npc_static"));assertFalse(encoded.contains("catalog"));
            System.out.println("npc-v17-golden="+HexFormat.of().formatHex(bytes));
        }
    }
}
