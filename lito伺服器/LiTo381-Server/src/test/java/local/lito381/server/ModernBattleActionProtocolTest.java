package local.lito381.server;

import static local.lito381.server.ModernApplicationProtocol.*;
import static local.lito381.server.ModernBattleActionProtocol.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernBattleActionProtocolTest {
    static final ModernRooms.Scope PLAYER=new ModernRooms.Scope(7,3333,Long.MIN_VALUE,3);
    static final ModernTargetHistory.World WORLD=new ModernTargetHistory.World(101,102,103);
    static final ModernBattleCommandLife.Proof PROOF=new ModernBattleCommandLife.Proof(PLAYER,WORLD,1,2,4);
    static final ModernNpcRoster.Scope NPC_WORLD=new ModernNpcRoster.Scope(101,102,103);
    static Request wrapped(Message command,Target target){return new Request(PROOF,target,command);}
    static List<Request> requests(){
        return List.of(
            wrapped(new WorldRequest(9,new ModernWorldProtocol.Request(PLAYER,ModernWorldProtocol.Action.MOVE,2,0,0,102,103,1,32768,32768,ModernWorldProtocol.ZERO_DIGEST)),NO_TARGET),
            wrapped(new CombatRequest(9,new ModernCombatProtocol.Request(PLAYER,ModernCombatProtocol.Action.ATTACK,102,103,1,2)),new Target(Kind.PLAYER,2,3)),
            wrapped(new CastRequest(9,new ModernSpellProtocol.Request(PLAYER,102,103,1,5,0)),NO_TARGET),
            wrapped(new ItemRequest(9,new ModernItemProtocol.Request(PLAYER,ModernItemProtocol.Action.USE,102,103,1,40010)),NO_TARGET),
            wrapped(new AmmoRequest(9,new ModernAmmoProtocol.Request(PLAYER,ModernAmmoProtocol.Action.SELECT,102,103,1,40743)),NO_TARGET),
            wrapped(new NpcRequest(9,new ModernNpcProtocol.Request(PLAYER,ModernNpcProtocol.Action.ATTACK,NPC_WORLD,1,12,3)),new Target(Kind.NPC,12,3)),
            wrapped(new NpcSpellRequest(9,new ModernNpcSpellProtocol.Request(PLAYER,NPC_WORLD,1,2,12,3)),new Target(Kind.NPC,12,3)),
            wrapped(new SiegeRequest(9,new ModernSiegeProtocol.Request(PLAYER,new ModernSiege.Scope(101,102,103),ModernSiegeProtocol.Action.PORTAL,1,null)),NO_TARGET),
            wrapped(new DisintegrateRequest(9,new ModernDisintegrateState.Request(PLAYER,102,103,1,2,2,3)),new Target(Kind.PLAYER,2,3)),
            wrapped(new NpcDisintegrateRequest(9,new ModernNpcDisintegrateState.Request(PLAYER,new ModernNpcRoster.Id(NPC_WORLD,12),1,2,3)),new Target(Kind.NPC,12,3)),
            wrapped(new NpcFireStormRequest(9,new ModernNpcFireStormState.Request(PLAYER,NPC_WORLD,1,2)),NO_TARGET)
        );
    }
    @Test void allTypedMutationsRoundTripWithUnchangedInnerBytesAndOpaqueEpoch()throws Exception {
        assertEquals("4c544d31000000bc00000009003e0000000000070000000000000d05800000000000000000000003000000000000006500000000000000660000000000000067000000000000000100000000000000020000000000000004010000000000000000000000000000020000000000000003000000484c544d31000000480000000900160000000000070000000000000d058000000000000000030100000000000000000066000000000000006700000000000000010000000000000002",java.util.HexFormat.of().formatHex(ModernApplicationProtocol.encode(new BattleActionRequest(9,requests().get(1)),37)));
        for(var request:requests()){
            var m=new BattleActionRequest(9,request);var b=ModernApplicationProtocol.encode(m,37);
            assertTrue(b.length>=188&&b.length<=236);assertEquals(62,ByteBuffer.wrap(b).getShort(12));
            assertEquals(Long.MIN_VALUE,ByteBuffer.wrap(b).getLong(28));
            assertArrayEquals(ModernApplicationProtocol.encode(request.command(),36),java.util.Arrays.copyOfRange(b,116,b.length));
            assertEquals(m,ModernApplicationProtocol.decode(b,37));
            for(int v:new int[]{27,35,36})assertThrows(ProtocolException.class,()->ModernApplicationProtocol.decode(b,v));
            for(int n=0;n<b.length;n++){var truncated=java.util.Arrays.copyOf(b,n);assertThrows(ProtocolException.class,()->ModernApplicationProtocol.decode(truncated,37));}
        }
    }
    @Test void noCredentialNavigationReadLoadLeaveOrNestedEnvelopeCanBeWrapped()throws Exception {
        var read=new WorldRequest(9,new ModernWorldProtocol.Request(PLAYER,ModernWorldProtocol.Action.READ,0,0,0,102,103,0,0,0,ModernWorldProtocol.ZERO_DIGEST));
        assertThrows(IllegalArgumentException.class,()->wrapped(read,NO_TARGET));
        assertThrows(IllegalArgumentException.class,()->wrapped(new Ping(9),NO_TARGET));
        assertThrows(IllegalArgumentException.class,()->wrapped(new BattleActionRequest(9,requests().get(0)),NO_TARGET));
        assertThrows(IllegalArgumentException.class,()->wrapped(requests().get(1).command(),new Target(Kind.NPC,2,3)));
        assertThrows(IllegalArgumentException.class,()->new Request(new ModernBattleCommandLife.Proof(PLAYER,new ModernTargetHistory.World(101,104,103),1,2,4),NO_TARGET,requests().get(0).command()));
        var b=ModernApplicationProtocol.encode(new BattleActionRequest(9,requests().get(0)),37);
        for(int at:new int[]{14,89,90,91,92,93,94,95,128,129,130,131}) {
            var bad=b.clone();bad[at]^=127;assertThrows(ProtocolException.class,()->ModernApplicationProtocol.decode(bad,37));
        }
    }
    @Test void capabilityMustBeExplicitAndDropsOnOlderServerWithoutChangingLegacyPolicy()throws Exception {
        var base=ModernSessionBoundary.Policy.world(false);assertEquals(0,base.supportedCapabilities()&ModernSessionProtocol.CAP_ACTION_LIFE);
        var policy=base.withBattleActions();int caps=policy.supportedCapabilities();
        var hello=ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,37,caps,7,1));
        var b=new ModernSessionBoundary(policy);b.acceptHello(hello);assertTrue(negotiated(b.negotiated().orElseThrow()));
        for(int v:new int[]{27,35,36}){
            var old=new ModernSessionBoundary(policy.withMaxVersion(v));old.acceptHello(hello);
            assertEquals(0,old.negotiated().orElseThrow().capabilities()&ModernSessionProtocol.CAP_ACTION_LIFE);
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(1,v,1,caps)));
        }
        for(int bit:new int[]{ModernSessionProtocol.CAP_BATTLE_DATA,ModernSessionProtocol.CAP_WORLD,ModernSessionProtocol.CAP_MATCH_DESCRIPTOR})
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(caps&~bit,7));
    }
    @Test void outboundResultAndBadRequestLengthRejectBeforeBodyAllocationOrRead() {
        for(int[] spec:new int[][]{{63,MAX_RESULT_BYTES},{62,187},{62,237},{62,Integer.MAX_VALUE}}){
            var h=ByteBuffer.allocate(16).putInt(0x4c544d31).putInt(spec[1]).putInt(9).putShort((short)spec[0]).putShort((short)0).array();
            var input=new InputStream(){int at;public int read(){if(at==16)throw new AssertionError("read forbidden attacker body");return Byte.toUnsignedInt(h[at++]);}};
            assertThrows(ProtocolException.class,()->ModernApplicationProtocol.readFrame(input,37));
        }
    }
    @Test void acceptedEchoIsExactAndRejectionCannotImpersonateAnAckOrEmptyRead()throws Exception {
        var r=requests().get(1);var q=((CombatRequest)r.command()).request();
        var ok=new CombatResult(9,q,new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.UNAVAILABLE,ModernCombatProtocol.Reason.NOT_ACTIVE,null));
        assertEquals(63,ByteBuffer.wrap(ModernApplicationProtocol.encode(new BattleActionResult(9,r,ModernBattleCommandLife.Rejection.NONE,ok),37)).getShort(12));
        var other=new CombatResult(9,new ModernCombatProtocol.Request(PLAYER,q.action(),102,103,2,2),ok.reply());
        assertThrows(IllegalArgumentException.class,()->validateResult(new BattleActionResult(9,r,ModernBattleCommandLife.Rejection.NONE,other)));
        assertThrows(IllegalArgumentException.class,()->validateResult(new BattleActionResult(9,r,ModernBattleCommandLife.Rejection.STALE_LIFE,ok)));
        var read=new CombatResult(9,new ModernCombatProtocol.Request(PLAYER,ModernCombatProtocol.Action.READ,102,103,0,0),ok.reply());
        assertThrows(IllegalArgumentException.class,()->validateResult(new BattleActionResult(9,r,ModernBattleCommandLife.Rejection.STALE_LIFE,read)));
    }
}
