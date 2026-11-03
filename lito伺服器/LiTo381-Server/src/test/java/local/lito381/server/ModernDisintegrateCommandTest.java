package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernDisintegrateState.Reason.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;

/** Typed requests travel through the application codec; snapshots are real engine results. */
class ModernDisintegrateCommandTest {
    static final Map<String,String> HASHES=Map.ofEntries(
        Map.entry("unavailable","266D1D384BCAEC02DD65268B274419175B599F83D4EFC313348330A08AE77419"),
        Map.entry("normal-hit","CED3D535092D96C352E7FBD595E688D35CDC22A5C346AFE450EEBFE2BF379C22"),
        Map.entry("normal-denied","0BA31BC8E3F9A74C32BDA92CA68B455C0D6362557ABD786406F02BD7C2733D66"),
        Map.entry("critical-hit","AD5B10BFF4B75828E0530D0C254E68BC2A5BC3AEAE425B1354D62344FBE6BCFC"),
        Map.entry("critical-denied","182CD675BF211BFBACFEEE081A0F951921C64B01A24D6119A30B2533BEB2CD3C"),
        Map.entry("honor-death","FB863643E4E502064A5C2DD4D718E4273F14A1E5FEDFBEB4AB62D34489A49025"),
        Map.entry("honor-stale","B7D16C2ABE3A026A5659211EFEC36DC7738E728FE0416DEDF7E10D5C250979DE"),
        Map.entry("honor-recast","AD637E1BB848FC0E796C3DC528B41A82F61E1262D7E2E44E43D862E98FFB040D"),
        Map.entry("terminal","38FBF4869E34E5D2D7A0E614CCBB7FD3FA09252B0F8E23A6DA187F609EABEACF"),
        Map.entry("not-active","4DB6D2A15808903B754E27A84B291D78B17B8F698A619D02116D8FE480A70E29"));
    static ModernApplicationProtocol.DisintegrateResult result(ModernBattleEngineHonorTest.Fixture f,
            ModernDisintegrateState.Request request,ModernBattleEngine.DisintegrateResult result) {
        return new ModernApplicationProtocol.DisintegrateResult(77,request,new ModernDisintegrateProtocol.Reply(
                result.reason()==NONE?ModernSpellProtocol.Status.OK:ModernSpellProtocol.Status.DENIED,result.reason(),ModernDisintegrateProtocolTest.frame(result.snapshot())));
    }
    static void golden(String name,ModernApplicationProtocol.DisintegrateResult m)throws Exception {
        byte[] raw=ModernApplicationProtocol.encode(m,27);
        assertEquals(HASHES.get(name),HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(raw)),name);
        assertEquals(m,ModernApplicationProtocol.decode(raw,27));
        assertArrayEquals(raw,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(raw),27));
        var zip=new ByteArrayOutputStream();try(var out=new GZIPOutputStream(zip)){out.write(raw);}
        System.out.println("disintegrate-command-v27="+name+","+raw.length+","+HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(raw))+","+Base64.getEncoder().encodeToString(zip.toByteArray()));
    }
    @Test void requestLayoutIsFixedLifeBoundAndOnlyVersion27()throws Exception {
        var r=new ModernDisintegrateState.Request(new ModernRooms.Scope(7,3333,-1L,3),9,6,1,2,4,3);
        var m=new ModernApplicationProtocol.DisintegrateRequest(0xfedcba98,r);var raw=ModernApplicationProtocol.encode(m,27);
        assertEquals(88,raw.length);assertEquals("4c544d3100000058fedcba98002c0000000000070000000000000d05ffffffffffffffff03000000000000000000000900000000000000060000000000000001000000000000000200000000000000040000000000000003",HexFormat.of().formatHex(raw));
        assertEquals(m,ModernApplicationProtocol.decode(raw,27));
        for(int version=1;version<27;version++){int v=version;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(m,v));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(raw,v));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(raw),v));}
        for(int at:new int[]{37,38,39}){var bad=raw.clone();bad[at]=1;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,27));}
        for(int at:new int[]{40,48,56,64,72,80})for(long value:new long[]{0,-1,Long.MIN_VALUE}){
            var bad=raw.clone();ByteBuffer.wrap(bad).putLong(at,value);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,27));}
        for(int length=0;length<raw.length;length++){var shortFrame=Arrays.copyOf(raw,length);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(shortFrame,27));}
    }
    @Test void typedEngineGoldensNormalCriticalCooldownHonorStaleLifeAndTerminal()throws Exception {
        for(boolean critical:new boolean[]{false,true})try(var f=ModernBattleEngineDisintegrateTest.fixture(false,2,10000,1000,500,false)){
            f.active();f.randomOverride=b->b==100?(critical?0:99):0;
            var r=ModernBattleEngineDisintegrateTest.request(f,0,1,2,1,1);
            golden(critical?"critical-hit":"normal-hit",result(f,r,f.engine.disintegrate(f.players.get(0).owner(),r)));
            r=ModernBattleEngineDisintegrateTest.request(f,0,2,2,1,1);
            golden(critical?"critical-denied":"normal-denied",result(f,r,f.engine.disintegrate(f.players.get(0).owner(),r)));
        }
        try(var f=ModernBattleEngineDisintegrateTest.fixture(true,0,1,1000,500,false)){
            f.active();var r=ModernBattleEngineDisintegrateTest.request(f,0,1,2,1,1);
            golden("honor-death",result(f,r,f.engine.disintegrate(f.players.get(0).owner(),r)));
            f.elapsed(5_000_000_000L);f.engine.tick();r=ModernBattleEngineDisintegrateTest.request(f,0,2,2,1,1);
            golden("honor-stale",result(f,r,f.engine.disintegrate(f.players.get(0).owner(),r)));
            r=ModernBattleEngineDisintegrateTest.request(f,0,3,2,1,2);
            golden("honor-recast",result(f,r,f.engine.disintegrate(f.players.get(0).owner(),r)));
        }
        try(var f=ModernBattleEngineDisintegrateTest.fixture(false,0,1,1L<<40,500,false)){
            f.active();var r=ModernBattleEngineDisintegrateTest.request(f,0,1,2,1,1);
            golden("terminal",result(f,r,f.engine.disintegrate(f.players.get(0).owner(),r)));
            r=ModernBattleEngineDisintegrateTest.request(f,0,2,2,1,1);
            golden("not-active",result(f,r,f.engine.disintegrate(f.players.get(0).owner(),r)));
        }
    }
    @Test void nullDenialStillNeedsExplicitNegotiationAndCannotPretendSuccess()throws Exception {
        var r=new ModernDisintegrateState.Request(new ModernRooms.Scope(7,3333,1,0),9,6,1,1,2,1);
        var m=new ModernApplicationProtocol.DisintegrateResult(77,r,new ModernDisintegrateProtocol.Reply(ModernSpellProtocol.Status.UNAVAILABLE,NOT_ACTIVE,null));
        golden("unavailable",m);assertEquals(92,ModernApplicationProtocol.encode(m,27).length);
        int all=ModernSessionProtocol.KNOWN_CAPABILITIES;
        assertEquals(m,ModernMatchDescriptor.forPeer(m,new ModernSessionBoundary.Negotiated(1,27,1,all)));
        for(int caps:new int[]{all&~ModernSessionProtocol.CAP_DISINTEGRATE,all&~ModernSessionProtocol.CAP_MATCH_DESCRIPTOR})
            assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(m,new ModernSessionBoundary.Negotiated(1,27,1,caps)));
        assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(m,new ModernSessionBoundary.Negotiated(1,26,1,all)));
        assertThrows(IllegalArgumentException.class,()->new ModernDisintegrateProtocol.Reply(ModernSpellProtocol.Status.OK,NONE,null));
        assertThrows(IllegalArgumentException.class,()->new ModernDisintegrateProtocol.Reply(ModernSpellProtocol.Status.DENIED,NOT_ACTIVE,null));
        var raw=ModernApplicationProtocol.encode(m,27);
        for(int at:new int[]{88,89,90,91}){var bad=raw.clone();bad[at]=(byte)255;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,27));}
    }
    @Test void typedSuccessCannotAliasSourceTargetLifeOrOldCastKind()throws Exception {
        try(var f=ModernBattleEngineDisintegrateTest.fixture(false,2,10000,1000,500,false)){
            f.active();var r=ModernBattleEngineDisintegrateTest.request(f,0,1,2,1,1);
            var m=result(f,r,f.engine.disintegrate(f.players.get(0).owner(),r));var raw=ModernApplicationProtocol.encode(m,27);
            assertThrows(IllegalArgumentException.class,()->new ModernDisintegrateProtocol.Reply(ModernSpellProtocol.Status.BUSY,TOO_FAST,m.reply().frame()));
            assertThrows(IllegalArgumentException.class,()->ModernDisintegrateProtocol.validate(new ModernApplicationProtocol.DisintegrateResult(77,r,
                    new ModernDisintegrateProtocol.Reply(ModernSpellProtocol.Status.DENIED,NOT_ACTIVE,m.reply().frame()))));
            for(int at:new int[]{40,48,56,64,72,80}){var bad=raw.clone();ByteBuffer.wrap(bad).putLong(at,99);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,27));}
            var aliased=raw.clone();ByteBuffer.wrap(aliased).putInt(16,42);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(aliased,27));
            var wrongKind=raw.clone();ByteBuffer.wrap(wrongKind).putShort(12,(short)27);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(wrongKind,27));
            var trailing=Arrays.copyOf(raw,raw.length+1);ByteBuffer.wrap(trailing).putInt(4,trailing.length);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(trailing,27));
            for(int v:new int[]{1,14,26})assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(raw,v));
        }
    }
}
