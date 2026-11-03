package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernNpcNamesTest {
    static final String WIRE="0001000200000000000000580000b0ce000006710006476f626c696e0003243431000000580000b0cf0000086e0004576f6c660000";
    static ModernNpcNames.Table table(){return new ModernNpcNames.Table(List.of(
            new ModernNpcNames.Name(88,45262,1649,"Goblin","$41"),
            new ModernNpcNames.Name(88,45263,2158,"Wolf","")));}
    @Test void exactIndependentLiteralAndImmutableCatalogIncludeFutureForms()throws Exception {
        var buffer=ByteBuffer.allocate(4096);ModernNpcNames.put(buffer,table());
        assertEquals(53,buffer.position());assertEquals(WIRE,HexFormat.of().formatHex(Arrays.copyOf(buffer.array(),buffer.position())));
        assertEquals(table(),ModernNpcNames.get(ByteBuffer.wrap(HexFormat.of().parseHex(WIRE))));
        assertThrows(UnsupportedOperationException.class,()->table().rows().clear());
        var catalog=ModernNpcProtocolTest.futureCatalog();var actual=ModernNpcNames.Table.from(catalog);
        assertEquals(catalog.npcs().size(),actual.rows().size());
        for(var row:actual.rows()){var n=catalog.npcs().get(row.npcId());assertEquals(n.name(),row.name());assertEquals(n.nameId(),row.nameId());assertEquals(catalog.sourceId(),row.sourceId());}
    }
    @Test void v28AppendsNamesWithoutChangingAnyOldFrameAndRequiresExplicitCompleteLabels()throws Exception {
        try(var fixture=new ModernBattleEngineNpcTest.Fixture()) {
            fixture.active();fixture.spawn(0,11,10);
            var old=ModernNpcProtocolTest.resources(ModernNpcProtocolTest.read(fixture));
            var named=new ModernApplicationProtocol.NpcResult(old.requestId(),old.echo(),new ModernNpcProtocol.Reply(old.reply().status(),old.reply().reason(),old.reply().frame().withNames(table())));
            var v27=ModernApplicationProtocol.encode(old,27);var v28=ModernApplicationProtocol.encode(named,28);
            assertEquals(v27.length+53,v28.length);
            var prefix=v27.clone();ByteBuffer.wrap(prefix).putInt(4,v28.length);assertArrayEquals(prefix,Arrays.copyOf(v28,v27.length));
            assertEquals(WIRE,HexFormat.of().formatHex(Arrays.copyOfRange(v28,v27.length,v28.length)));
            assertEquals(named,ModernApplicationProtocol.decode(v28,28));
            var peer=new ModernSessionBoundary.Negotiated(1,27,1,(ModernSessionProtocol.KNOWN_CAPABILITIES&~ModernSessionProtocol.CAP_GREATER_HASTE));
            assertEquals(old,ModernMatchDescriptor.forPeer(named,peer));
            assertArrayEquals(v27,ModernApplicationProtocol.encode(named,27));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(old,28));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(v28,27));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(v27,28));
            for(int length=v27.length;length<v28.length;length++){
                var bad=Arrays.copyOf(v28,length);ByteBuffer.wrap(bad).putInt(4,length);
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,28));
            }
            for(int offset:new int[]{0,2,4,8,12,16,20,22}){
                var bad=v28.clone();bad[v27.length+offset]=(byte)255;
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,28));
            }
            if(Boolean.getBoolean("lito.names.oracle"))System.out.println("npc-names-v28="+HexFormat.of().formatHex(v28));
        }
    }
    @Test void namesCannotChangeActorSourceOrGraphicsOrContainUnboundedInvalidText()throws Exception {
        for(var bad:List.of("", "  ", "bad\nname", "x".repeat(257), "怪".repeat(86), "\ud800"))
            assertThrows(IllegalArgumentException.class,()->new ModernNpcNames.Name(88,45262,1649,bad,""));
        assertDoesNotThrow(()->new ModernNpcNames.Name(88,45262,1649,"骷髏&(太陽神)","$1498"));
        assertThrows(IllegalArgumentException.class,()->new ModernNpcNames.Table(List.of()));
        assertThrows(IllegalArgumentException.class,()->new ModernNpcNames.Table(List.of(table().rows().get(0),table().rows().get(0))));
        assertThrows(IllegalArgumentException.class,()->new ModernNpcNames.Table(List.of(table().rows().get(1),table().rows().get(0))));
        try(var fixture=new ModernBattleEngineNpcTest.Fixture()) {
            fixture.active();fixture.spawn(0,11,10);var frame=ModernNpcProtocolTest.resources(ModernNpcProtocolTest.read(fixture)).reply().frame();
            for(var first:List.of(new ModernNpcNames.Name(7,45262,1649,"foreign",""),new ModernNpcNames.Name(88,45261,1649,"wrong",""),new ModernNpcNames.Name(88,45262,2158,"wrong","")))
                assertThrows(IllegalArgumentException.class,()->frame.withNames(new ModernNpcNames.Table(List.of(first,table().rows().get(1)))));
            assertThrows(IllegalArgumentException.class,()->frame.withNames(new ModernNpcNames.Table(List.of(table().rows().get(0)))));
        }
    }
    @Test void npcSpellReplyPreservesNamesAtV28AndProjectsThemForTheExistingV24Peer()throws Exception {
        try(var fixture=ModernBattleEngineNpcMagicTest.fixture()) {
            fixture.active();var npc=fixture.spawn(0,11,10);var result=ModernBattleEngineNpcMagicTest.cast(fixture,0,1,npc);
            var old=ModernNpcSpellProtocolTest.message(fixture,0,1,npc,result);
            var named=new ModernApplicationProtocol.NpcSpellResult(old.requestId(),old.echo(),new ModernNpcSpellProtocol.Reply(old.reply().status(),old.reply().reason(),
                    old.reply().frame().withNames(ModernNpcNames.Table.from(fixture.config.catalog()))));
            assertEquals(named,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(named,28),28));
            assertEquals(old,ModernMatchDescriptor.forPeer(named,new ModernSessionBoundary.Negotiated(1,24,1,ModernSessionProtocol.KNOWN_CAPABILITIES&~ModernSessionProtocol.CAP_DISINTEGRATE)));
            assertArrayEquals(ModernApplicationProtocol.encode(old,24),ModernApplicationProtocol.encode(named,24));
        }
    }
}
