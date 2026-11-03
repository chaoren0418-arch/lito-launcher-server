package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernMatchDescriptor.*;
import static local.lito381.server.ModernDisintegrateState.Reason.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;

/** Real engine -> service projection -> application bytes. Emitted compressed
 * goldens are consumed independently by the Rust decoder, not a Rust encoder.
 * No listener/production capability, source writes or renderer admission. */
class ModernDisintegrateProtocolTest {
    static final java.util.Map<String,String> GOLDEN_HASHES=java.util.Map.ofEntries(
        java.util.Map.entry("honor-active","5534711B17E394F3145F701DE47FADDAEDF33199AFD15132E1F98BAD8177FA3E"),
        java.util.Map.entry("honor-death","0BBAC3E98C0CD6EB118ABA403AB7AB97FDDB3290DBF7ADBDE3C518727757D1E7"),
        java.util.Map.entry("honor-respawn","A65301E2A28765142190012A63F4BEB3F9EEC5331E37410B8AF8C0679258B9A9"),
        java.util.Map.entry("honor-stale","28AEA89CD1F13981E0ABF13EE6729FC8BC3334FF0EC75E7FE3F82C8629C20A91"),
        java.util.Map.entry("honor-recast","5C2E8FDBD4300FC0D2D2AFA962DFE8006EDB0E264F8939C5B8B5EBC43BA3A8B9"),
        java.util.Map.entry("mixed-active","0B45320D6B8633C5439D973B293CBEAF0D6B3271C31A01B7FEE842905DBEF113"),
        java.util.Map.entry("mixed-dis","AB78E113BEFC1A027364169E7D70DA68644252504833E037F832D3E09D323F78"),
        java.util.Map.entry("mixed-fire","66EBD9B1B2A8BA7ADC83C1E6D4DA726C569261B7C3398FF2242258F587370E3E"),
        java.util.Map.entry("mixed-second","2F619A15503519E7FDEDE431D639D4E9B01E97976CB56C3929D977687E69A20A"),
        java.util.Map.entry("mixed-heal","A1A9AD0A9000242DDC2D7AE014C98A720797CA9D3799AEB305CA6B0D820E3A53"),
        java.util.Map.entry("terminal-active","E9F3E0B8D7E494C0B55C8F7BBDC5003126B8A6128DC6E3AFC8248C84EECFCA4F"),
        java.util.Map.entry("terminal-finished","6F4CD43FE54A352C17BB27129FE8851AB2DFF18E8D12585C073FACEE31870BB8"),
        java.util.Map.entry("normal-loading","D74A3ABBC84B4ACF0B599B62C3CCC2BFB19D3C3BB6527DD126E2D9DC78B02F1F"),
        java.util.Map.entry("normal-active","A0083DF47D772EBEB4B67CA10381FBDF120930A4AFCD26012C71C4FC78F05BF1"),
        java.util.Map.entry("normal-hit","F83244C0144FAAF3B16DAECD4875C895BAB94DD368C70EFB109491DBF0D738CC"),
        java.util.Map.entry("normal-denied","DB5848298404810C68241FE0FAE4CA69E1EA0E9A54FD8B916D2F89E9C7BCF5CF"),
        java.util.Map.entry("normal-second","B43CA3A465F55DDBFBA10B0C7D52CA1E545962D164F1C4A647610D54F1531A98"),
        java.util.Map.entry("critical-loading","D74A3ABBC84B4ACF0B599B62C3CCC2BFB19D3C3BB6527DD126E2D9DC78B02F1F"),
        java.util.Map.entry("critical-active","A0083DF47D772EBEB4B67CA10381FBDF120930A4AFCD26012C71C4FC78F05BF1"),
        java.util.Map.entry("critical-hit","EA576C683EACC2F5EEABCA6D640B806AD912D494C3E03C857D193A86FA1049A3"),
        java.util.Map.entry("critical-denied","201E2F7DAE00168B57E805FB57BCF7BEC485AE56FC2A8BEADD1491F565C8B418"),
        java.util.Map.entry("critical-second","580FDE972DCDAC9D691F3CE207875D99B9D7C775E8E2C1F27A9F39C2F428E7AE"),
        java.util.Map.entry("regen-active","0B45320D6B8633C5439D973B293CBEAF0D6B3271C31A01B7FEE842905DBEF113"),
        java.util.Map.entry("regen-hit","AB78E113BEFC1A027364169E7D70DA68644252504833E037F832D3E09D323F78"),
        java.util.Map.entry("regen-hp","7306DDA934E22F291FC4570505191C70EDD6EB460E5A4159B7F1F3C7E9F70542"),
        java.util.Map.entry("regen-mp","95AFF0E6B2A1B8E1238338511B495F072FE98E1DE871337DE7CB313CB85A6B9C"),
        java.util.Map.entry("regen-recast","02F2B401486A3EE9C1E35B2154DF53CB787B897F5E43D0563DDC60BF5B70FC92"));
    static ModernMatchDescriptor descriptor(ModernBattleEngine.Snapshot s) {
        return new ModernMatchDescriptor(s.honor().enabled()?Ruleset.HONOR_TOWERS:Ruleset.DEATHMATCH_ELIMINATION,
                COMBAT|SPELLS|LIGHT_ARROW|ITEMS|WEIGHT|TIMED|REGEN|FIRE_STORM|DISINTEGRATE|(s.honor().enabled()?HONOR:0),
                2000,2000,0,1_800_000);
    }
    static ModernCombatProtocol.Frame frame(ModernBattleEngine.Snapshot s) {
        var w=ModernWorldService.fromEngine(s,Math.incrementExact(s.revision()),descriptor(s));
        // Freeze only the random persistence UUID for reproducible wire goldens.
        if(w.completion()!=null) {
            var e=w.completion().event();var fixed=new ModernMatchSettlement.Event(
                    java.util.UUID.fromString("12345678-1234-5678-9abc-def012345678"),e.matchId(),e.generation(),e.engineRevision(),e.mode(),e.mapId(),e.finishedAtMillis(),e.participants());
            w=new ModernWorldProtocol.Frame(w.matchId(),w.generation(),w.revision(),w.phase(),w.abortReason(),w.room(),w.manifest(),w.actors(),
                    new ModernWorldProtocol.Completion(fixed,w.completion().state()),w.casts(),w.descriptor(),w.race(),w.audienceLane(),w.quest());
        }
        return ModernWorldService.fromCombat(s,w);
    }
    static ModernApplicationProtocol.CombatResult message(ModernBattleEngineHonorTest.Fixture f,ModernBattleEngine.Snapshot s) {
        return new ModernApplicationProtocol.CombatResult(77,new ModernCombatProtocol.Request(f.players.get(0).scope(),ModernCombatProtocol.Action.READ,s.matchId(),s.generation(),0,0),
                new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,frame(s)));
    }
    static void golden(String name,ModernBattleEngineHonorTest.Fixture f,ModernBattleEngine.Snapshot s)throws Exception {
        var m=message(f,s);var bytes=ModernApplicationProtocol.encode(m,27);
        assertEquals(m,ModernApplicationProtocol.decode(bytes,27));
        assertEquals(GOLDEN_HASHES.get(name),HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),name);
        assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),27));
        var zip=new ByteArrayOutputStream();try(var out=new GZIPOutputStream(zip)){out.write(bytes);}
        System.out.println("disintegrate-v27="+name+","+bytes.length+","+HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))+","+Base64.getEncoder().encodeToString(zip.toByteArray()));
    }
    @Test void engineWireGoldensCoverLoadingNormalCriticalBotsDenialsAndNoDoubleSpend()throws Exception {
        for(boolean critical:new boolean[]{false,true})try(var f=ModernBattleEngineDisintegrateTest.fixture(false,2,10000,1000,500,false)) {
            String prefix=critical?"critical":"normal";golden(prefix+"-loading",f,f.engine.tick());golden(prefix+"-active",f,f.active());
            f.randomOverride=b->b==100?(critical?0:99):0;
            var result=ModernBattleEngineDisintegrateTest.cast(f,1,2);assertEquals(NONE,result.reason());
            assertEquals(critical,result.snapshot().disintegrations().get(0).hit().critical());golden(prefix+"-hit",f,result.snapshot());
            assertEquals(TOO_FAST,ModernBattleEngineDisintegrateTest.cast(f,2,2).reason());golden(prefix+"-denied",f,f.engine.tick());
            f.elapsed(5_000_000_000L);assertEquals(NONE,ModernBattleEngineDisintegrateTest.cast(f,3,2).reason());golden(prefix+"-second",f,f.engine.tick());
        }
    }
    @Test void honorDeathRespawnRecastCoalescedFireAndHealingKeepIndependentLedgers()throws Exception {
        try(var f=ModernBattleEngineDisintegrateTest.fixture(true,0,1,1000,500,false)) {
            golden("honor-active",f,f.active());assertEquals(NONE,ModernBattleEngineDisintegrateTest.cast(f,1,2).reason());golden("honor-death",f,f.engine.tick());
            f.elapsed(5_000_000_000L);golden("honor-respawn",f,f.engine.tick());
            assertEquals(STALE_LIFE,ModernBattleEngineDisintegrateTest.cast(f,2,2).reason());golden("honor-stale",f,f.engine.tick());
            assertEquals(NONE,ModernBattleEngineDisintegrateTest.cast(f,0,3,2,1,2).reason());golden("honor-recast",f,f.engine.tick());
        }
        try(var f=ModernBattleEngineDisintegrateTest.fixture(true,0,10000,1000,500,false)) {
            golden("mixed-active",f,f.active());assertEquals(NONE,ModernBattleEngineDisintegrateTest.cast(f,1,2).reason());golden("mixed-dis",f,f.engine.tick());
            f.elapsed(2_000_000_000L);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,2,70,0).reason());golden("mixed-fire",f,f.engine.tick());
            f.elapsed(5_000_000_000L);assertEquals(NONE,ModernBattleEngineDisintegrateTest.cast(f,3,2).reason());golden("mixed-second",f,f.engine.tick());
            f.elapsed(7_000_000_000L);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(1,1,1,4).reason());golden("mixed-heal",f,f.engine.tick());
        }
    }
    @Test void deathmatchTerminalIsOneResultWithWideMpAndSourceScopedSameObjectIds()throws Exception {
        try(var f=ModernBattleEngineDisintegrateTest.fixture(false,0,1,1L<<40,500,false)) {
            golden("terminal-active",f,f.active());var killed=ModernBattleEngineDisintegrateTest.cast(f,1,2);assertEquals(NONE,killed.reason());
            assertEquals(ModernBattleEngine.Phase.FINISHED,killed.snapshot().phase());golden("terminal-finished",f,killed.snapshot());
            assertEquals(killed.snapshot(),f.engine.tick());
        }
    }
    @Test void recoveryBetweenCastsKeepsMpAndHpAnchorsSeparateFromSkillReceipts()throws Exception {
        try(var f=ModernBattleEngineDisintegrateTest.fixture(true,0,10000,1000,500,true)) {
            golden("regen-active",f,f.active());assertEquals(NONE,ModernBattleEngineDisintegrateTest.cast(f,1,2).reason());golden("regen-hit",f,f.engine.tick());
            f.elapsed(2_000_000_000L);var healed=f.engine.tick();assertTrue(healed.regen().get(1).hpGain()>0);golden("regen-hp",f,healed);
            f.elapsed(64_000_000_000L);var restored=f.engine.tick();assertTrue(restored.regen().get(0).mpGain()>0);golden("regen-mp",f,restored);
            assertEquals(NONE,ModernBattleEngineDisintegrateTest.cast(f,2,2).reason());golden("regen-recast",f,f.engine.tick());
        }
    }
    @Test void v27IsExplicitDependentNotProductionAndOldPeersCannotStripTheDescriptor()throws Exception {
        int cap=ModernSessionProtocol.CAP_DISINTEGRATE,all=(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_ACTION_LIFE|ModernSessionProtocol.CAP_LARGE_MAPS|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_MATCHMAKING|ModernSessionProtocol.CAP_NPC_FIRE_STORM|ModernSessionProtocol.CAP_CHAT|ModernSessionProtocol.CAP_DIRECT_CHAT|ModernSessionProtocol.CAP_FILTERED_CHAT))&~ModernSessionProtocol.CAP_NPC_DISINTEGRATE;
        assertEquals(524288,cap);assertEquals(0,ModernSessionProtocol.RUNTIME_CAPABILITIES&cap);
        assertEquals(0,ModernSessionBoundary.Policy.world(true,true,true,true,true,true,true,true,true,true,true).supportedCapabilities()&cap);
        for(int missing:new int[]{ModernSessionProtocol.CAP_REGEN,ModernSessionProtocol.CAP_MATCH_DESCRIPTOR,ModernSessionProtocol.CAP_COMBAT})
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(all&~missing,0));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(all,cap));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,26,all,3,1)));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(1,26,1,all)));
        assertThrows(IllegalArgumentException.class,()->new ModernSessionBoundary.Policy(all,all,26));
        for(int version:new int[]{26,27}) {
            var b=new ModernSessionBoundary(new ModernSessionBoundary.Policy(all,3,version));
            var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(b.acceptHello(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,27,all,3,1))));
            assertEquals(version,ack.version());assertEquals(version==27?cap:0,ack.capabilities()&cap);
        }
        try(var f=ModernBattleEngineDisintegrateTest.fixture(false,0,10000,1000,500,false)) {
            var m=message(f,f.active());assertEquals(m,ModernMatchDescriptor.forPeer(m,new ModernSessionBoundary.Negotiated(1,27,1,all)));
            for(int v=1;v<27;v++){int version=v;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(m,version));
                assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(m,new ModernSessionBoundary.Negotiated(1,version,1,all)));}
            assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(m,new ModernSessionBoundary.Negotiated(1,27,1,all&~cap)));
            assertFalse(m.reply().frame().world().descriptor().supportedBy(27,all&~ModernSessionProtocol.CAP_MATCH_DESCRIPTOR));
            assertThrows(IllegalArgumentException.class,()->ModernWorldService.fromEngine(f.engine.tick(),100,null));
            assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(m.reply().frame().world(),m.reply().frame().vitals()));
        }
    }
    @Test void disabledAppendixAddsOnlyEightZerosAndOldGoldenBytesStayIdentical()throws Exception {
        var m=new ModernApplicationProtocol.CombatResult(0xfedcba98,ModernCombatProtocolTest.read(),new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,ModernCombatProtocolTest.frame()));
        assertEquals(ModernCombatProtocolTest.FRAME_HEX,HexFormat.of().formatHex(ModernApplicationProtocol.encode(m,1)));
        for(int version=1;version<=27;version++)assertEquals(m,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(m,version),version));
        var old=ModernApplicationProtocol.encode(m,26);var now=ModernApplicationProtocol.encode(m,27);
        assertEquals(old.length+8,now.length);assertArrayEquals(new byte[8],Arrays.copyOfRange(now,old.length,now.length));
        var prefix=Arrays.copyOf(now,old.length);ByteBuffer.wrap(prefix).putInt(4,old.length);assertArrayEquals(old,prefix);
    }
    @Test void fullPacketRejectsTruncationPaddingInvalidLifeRosterAndOldVersion()throws Exception {
        try(var f=ModernBattleEngineDisintegrateTest.fixture(true,0,10000,1000,500,false)) {
            f.active();ModernBattleEngineDisintegrateTest.cast(f,1,2);var bytes=ModernApplicationProtocol.encode(message(f,f.engine.tick()),27);
            int appendix=bytes.length-8-2*ModernDisintegrateState.STATE_BYTES;
            for(int n=appendix;n<bytes.length;n++){var cut=Arrays.copyOf(bytes,n);ByteBuffer.wrap(cut).putInt(4,n);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(cut,27));}
            for(int offset:new int[]{0,2,7,8+144,8+145,8+151,8+152+64}) {
                var broken=bytes.clone();broken[appendix+offset]=2;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(broken,27));
            }
            var broken=bytes.clone();ByteBuffer.wrap(broken).putLong(appendix+8+80,2);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(broken,27));
            var stripped=Arrays.copyOf(bytes,appendix);ByteBuffer.wrap(stripped).putInt(4,stripped.length);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(stripped,26));
            var trailing=Arrays.copyOf(bytes,bytes.length+1);ByteBuffer.wrap(trailing).putInt(4,trailing.length);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(trailing,27));
        }
    }
}
