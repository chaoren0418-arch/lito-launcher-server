package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Independent v6 widths; the v1-v5 golden fixtures are never regenerated. */
class ModernDamageSpellProtocolTest {
    private static final int ID=0xfedcba98;
    static final String REQUEST_HEX="4c544d3100000050fedcba98001a0000"
            +"000000070000000000000d05ffffffffffffffff03000000"
            +"0000000000000009000000000000000600000000000000020000000400000000"
            +"0000000000000002";
    static final String STATE_HEX="0000000000000001000000000000000100000000000000080000000000000003"
            +"00000004000000000000000200007fd40000801000007fd50000801000000003";
    private static final ModernRooms.Scope SCOPE=new ModernRooms.Scope(7,3333,-1,3);
    static ModernSpellProtocol.Request request() { return new ModernSpellProtocol.Request(SCOPE,9,6,2,4,2); }
    static ModernDamageSpellProtocol.State state() {
        return new ModernDamageSpellProtocol.State(1,1,8,3,4,2,32724,32784,32725,32784,3);
    }
    static ModernCombatProtocol.Frame frame(boolean finished) {
        var old=ModernSettlementProtocolTest.finished();
        var casts=List.of(new ModernSpellProtocol.CastState(1,2,1,6,10,5,32723,32784,32724,32784,700),
                ModernSpellProtocol.CastState.initial(2,900));
        var world=new ModernWorldProtocol.Frame(9,6,8,finished?ModernWorldProtocol.Phase.FINISHED:ModernWorldProtocol.Phase.ACTIVE,
                old.abortReason(),old.room(),old.manifest(),old.actors(),finished?old.completion():null,casts);
        var vitals=List.of(new ModernCombatProtocol.Vital(1,20,20,17,30,-9,0,0,0,0,0,0,1_000_000_000),
                new ModernCombatProtocol.Vital(2,finished?0:13,20,30,30,-10,0,0,0,8,finished?8:0,0,1_000_000_000));
        return new ModernCombatProtocol.Frame(world,vitals,List.of(state(),ModernDamageSpellProtocol.State.initial(2,3)));
    }
    static ModernApplicationProtocol.CastResult success(boolean finished) {
        return new ModernApplicationProtocol.CastResult(ID,request(),new ModernSpellProtocol.Reply(
                ModernSpellProtocol.Status.OK,ModernSpellProtocol.Reason.NONE,frame(finished)));
    }
    private static byte[] encode(ModernApplicationProtocol.Message message) throws Exception { return ModernApplicationProtocol.encode(message,6); }
    private static void rejected(byte[] bytes) { assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,6)); }

    @Test void literalTargetedRequestAndIndependentSixtyFourByteHistory() throws Exception {
        var message=new ModernApplicationProtocol.CastRequest(ID,request());var bytes=encode(message);
        assertEquals(80,bytes.length);assertEquals(REQUEST_HEX,HexFormat.of().formatHex(bytes));
        assertEquals(message,ModernApplicationProtocol.decode(HexFormat.of().parseHex(REQUEST_HEX),6));
        var out=ByteBuffer.allocate(68);ModernDamageSpellProtocol.put(out,List.of(state()));
        assertEquals(68,out.position());assertEquals("00010000"+STATE_HEX,HexFormat.of().formatHex(out.array()));
        assertEquals(List.of(state()),ModernDamageSpellProtocol.get(ByteBuffer.wrap(out.array()),1));
        var success=success(false);byte[] full=encode(success);
        assertEquals(success,ModernApplicationProtocol.decode(full,6));
        assertArrayEquals(HexFormat.of().parseHex(STATE_HEX),Arrays.copyOfRange(full,full.length-128,full.length-64));
    }
    @Test void allTruncationsTrailingBytesAndTargetReservedFieldsAreRejected() throws Exception {
        for(var message:List.<ModernApplicationProtocol.Message>of(new ModernApplicationProtocol.CastRequest(ID,request()),success(false),success(true))) {
            byte[] good=encode(message);
            for(int n=0;n<good.length;n++)rejected(Arrays.copyOf(good,n));
            byte[] trailing=Arrays.copyOf(good,good.length+1);ByteBuffer.wrap(trailing).putInt(4,trailing.length);rejected(trailing);
        }
        byte[] good=encode(new ModernApplicationProtocol.CastRequest(ID,request()));
        for(int offset:new int[]{14,37,38,39,68,69,70,71}){byte[] bad=good.clone();bad[offset]=1;rejected(bad);}
        for(long target:new long[]{0,-1,Long.MIN_VALUE}){byte[] bad=good.clone();ByteBuffer.wrap(bad).putLong(72,target);rejected(bad);}
        byte[] wrongSkill=good.clone();ByteBuffer.wrap(wrongSkill).putInt(64,5);rejected(wrongSkill);
        byte[] full=encode(success(false));int appendix=full.length-132;
        for(int count:new int[]{1,3,101,65535}){byte[] bad=full.clone();ByteBuffer.wrap(bad).putShort(appendix,(short)count);rejected(bad);}
        byte[] reserved=full.clone();reserved[appendix+3]=1;rejected(reserved);
    }
    @Test void versionSixAddsOnlyCombatAppendixAndTargetEchoNotWorldLayout() throws Exception {
        var old=ModernCombatProtocolTest.frame();
        var request=ModernCombatProtocolTest.read();
        var message=new ModernApplicationProtocol.CombatResult(ID,request,new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,old));
        byte[] v5=ModernApplicationProtocol.encode(message,5),v6=encode(message);
        byte[] expected=Arrays.copyOf(v5,v5.length+4);ByteBuffer.wrap(expected).putInt(4,expected.length);
        assertArrayEquals(expected,v6);assertEquals(message,ModernApplicationProtocol.decode(v6,6));
        var world=ModernSettlementProtocolTest.message(frame(false).world());
        assertArrayEquals(ModernApplicationProtocol.encode(world,5),encode(world));
        var targeted=new ModernApplicationProtocol.CastRequest(ID,request());
        for(int version:List.of(4,5)) {
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(targeted,version));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(success(false),version));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(HexFormat.of().parseHex(REQUEST_HEX),version));
            // Old clients still may ask for unsupported skill4 with no target.
            var legacy=new ModernApplicationProtocol.CastRequest(ID,new ModernSpellProtocol.Request(SCOPE,9,6,2,4));
            assertEquals(legacy,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(legacy,version),version));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->encode(legacy));
        }
        var teleport=new ModernApplicationProtocol.CastRequest(ID,new ModernSpellProtocol.Request(SCOPE,9,6,2,5));
        byte[] oldTeleport=ModernApplicationProtocol.encode(teleport,5),newTeleport=encode(teleport);
        assertEquals(oldTeleport.length+8,newTeleport.length);assertEquals(0,ByteBuffer.wrap(newTeleport).getLong(72));
        assertEquals(teleport,ModernApplicationProtocol.decode(newTeleport,6));
    }
    @Test void skillFourCapabilityIsExplicitAndNeverPromotedOnOldVersions() throws Exception {
        var context=new ModernSpellProtocol.Context(7,3333,-1);
        var arrow=new ModernSkills.Skill(4,"$1439",3,10,ModernSkills.Support.LIGHT_ARROW);
        var message=new ModernApplicationProtocol.SkillsResult(ID,context,new ModernSpellProtocol.SkillsReply(ModernSpellProtocol.Status.OK,List.of(arrow)));
        assertEquals(message,ModernApplicationProtocol.decode(encode(message),6));
        for(int version:List.of(4,5)) {
            var decoded=(ModernApplicationProtocol.SkillsResult)ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message,version),version);
            assertEquals(ModernSkills.Support.UNIMPLEMENTED,decoded.reply().skills().get(0).support());
            assertEquals(3,decoded.reply().skills().get(0).mpCost());
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(encode(message),version));
        }
    }
    @Test void countMpContextAndEventBoundsCannotBeForgedOrPartiallyDropped() {
        var f=frame(false);assertThrows(UnsupportedOperationException.class,()->f.damages().clear());
        assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(f.world(),f.vitals()));
        assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(f.world(),f.vitals(),List.of(state())));
        for(int cost:new int[]{0,4,Integer.MAX_VALUE})assertThrows(IllegalArgumentException.class,()->ModernDamageSpellProtocol.State.initial(1,cost));
        assertThrows(ArithmeticException.class,()->new ModernDamageSpellProtocol.State(1,Long.MAX_VALUE,8,0,4,2,32724,32784,32725,32784,3));
        for(long[] row:new long[][]{{1,9,3},{2,8,6},{1,8,4}}) {
            assertThrows(RuntimeException.class,()->new ModernCombatProtocol.Frame(f.world(),f.vitals(),List.of(
                    new ModernDamageSpellProtocol.State(1,row[0],row[1],row[2],4,2,32724,32784,32725,32784,3),f.damages().get(1))));
        }
        for(long target:new long[]{1,3})assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(f.world(),f.vitals(),List.of(
                new ModernDamageSpellProtocol.State(1,1,8,3,4,target,32724,32784,32725,32784,3),f.damages().get(1))));
        assertThrows(IllegalArgumentException.class,()->new ModernDamageSpellProtocol.State(1,1,8,3,4,2,32724,32784,32735,32784,3));
    }
    @Test void invalidLightArrowTemplateAlsoDowngradesForOldPeersInsteadOfDisconnectingSkills() throws Exception {
        var context=new ModernSpellProtocol.Context(7,3333,-1);
        var arrow=new ModernSkills.Skill(4,"$1439",4,10,ModernSkills.Support.UNSUPPORTED_TEMPLATE);
        var message=new ModernApplicationProtocol.SkillsResult(ID,context,new ModernSpellProtocol.SkillsReply(ModernSpellProtocol.Status.OK,List.of(arrow)));
        assertEquals(message,ModernApplicationProtocol.decode(encode(message),6));
        for(int version:List.of(4,5)) {
            var decoded=(ModernApplicationProtocol.SkillsResult)ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message,version),version);
            var skill=decoded.reply().skills().get(0);
            assertEquals(ModernSkills.Support.UNIMPLEMENTED,skill.support());assertEquals(4,skill.mpCost());assertEquals(10,skill.reuseMillis());
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(encode(message),version));
        }
    }
    @Test void exactMagicLethalAckThenSavedReceiptPreserveBothIndependentHistories() throws Exception {
        var message=success(true);assertEquals(message,ModernApplicationProtocol.decode(encode(message),6));
        var f=message.reply().frame();var w=f.world();
        var savedWorld=new ModernWorldProtocol.Frame(w.matchId(),w.generation(),9,w.phase(),w.abortReason(),w.room(),w.manifest(),w.actors(),
                new ModernWorldProtocol.Completion(w.completion().event(),ModernWorldProtocol.SettlementState.SAVED),w.casts());
        var saved=new ModernCombatProtocol.Frame(savedWorld,f.vitals(),f.damages());
        var read=new ModernApplicationProtocol.CombatResult(ID,ModernCombatProtocolTest.read(),new ModernCombatProtocol.Reply(
                ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,saved));
        assertEquals(read,ModernApplicationProtocol.decode(encode(read),6));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->encode(new ModernApplicationProtocol.CastResult(ID,request(),
                new ModernSpellProtocol.Reply(ModernSpellProtocol.Status.OK,ModernSpellProtocol.Reason.NONE,saved))));
        var wrong=new ModernSpellProtocol.Request(SCOPE,9,6,3,4,2);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->encode(new ModernApplicationProtocol.CastResult(ID,wrong,message.reply())));
        var late=new ModernApplicationProtocol.CastResult(ID,wrong,new ModernSpellProtocol.Reply(ModernSpellProtocol.Status.DENIED,ModernSpellProtocol.Reason.NOT_ACTIVE,saved));
        assertEquals(late,ModernApplicationProtocol.decode(encode(late),6));
    }
    @Test void appendedDenialReasonsAreVersionSixOnly() throws Exception {
        for(var reason:List.of(ModernSpellProtocol.Reason.TARGET_DEAD,ModernSpellProtocol.Reason.SAME_TEAM,ModernSpellProtocol.Reason.OUT_OF_RANGE,ModernSpellProtocol.Reason.BLOCKED)) {
            var reply=new ModernApplicationProtocol.CastResult(ID,request(),new ModernSpellProtocol.Reply(ModernSpellProtocol.Status.DENIED,reason,frame(false)));
            assertEquals(reply,ModernApplicationProtocol.decode(encode(reply),6));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(reply,5));
        }
    }
}
