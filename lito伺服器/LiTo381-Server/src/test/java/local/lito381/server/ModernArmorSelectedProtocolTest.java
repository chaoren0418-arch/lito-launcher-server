package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernApplicationProtocol.*;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exact selected application replies from real engine captures. No listener,
 * WorldService armor activation, player assets or interactive UI are inferred. */
class ModernArmorSelectedProtocolTest {
    @TempDir Path root;
    record Handshake(byte[] hello,byte[] ack,ModernSessionBoundary.Negotiated selected) {}
    interface Sink {void send(String label,int extensions,byte[] request,byte[] reply)throws Exception;}
    static Handshake handshake(int ext)throws Exception {
        var offer=new ModernSessionExtensions.Offer(ext,ext);
        var hello=ModernSessionProtocol.encode(new ModernSessionProtocol.ExtendedHello(ModernSessionExtensionsTest.base(),offer));
        var boundary=new ModernSessionBoundary(ModernSessionExtensionsTest.policy(),offer);
        var ack=boundary.acceptHello(hello);var n=boundary.negotiated().orElseThrow();
        assertEquals(ext,n.extensions());assertTrue(ModernSessionExtensions.allowsArmor(n));
        return new Handshake(hello,ack,n);
    }
    static void trace(Path root,Sink sink)throws Exception {
        var armor=handshake(4).selected();var combined=handshake(7).selected();
        ModernBattleArmorWireTest.trace(root,(label,v)->{
            boolean healing=v.world().descriptor().has(ModernMatchDescriptor.PLAYER_HEALING);
            var peer=healing?combined:armor;var q=new ModernBattleReadProtocol.Request(v.personal().scope(),v.world().identity());
            var request=new BattleReadRequest(2,q);var result=new BattleReadResult(2,q,
                    new ModernBattleReadProtocol.Reply(ModernBattleReadProtocol.State.DATA,v));
            var encoded=ModernApplicationProtocol.encode(result,peer);
            assertArrayEquals(ModernBattleWire.encodeArmorProtection(v),Arrays.copyOfRange(encoded,72,encoded.length));
            assertEquals(ModernBattleArmorWire.MAX_BYTES,ModernBattleWire.Encoding.selected(peer).maximum());
            for(int ext:new int[]{0,1,3})assertThrows(IllegalArgumentException.class,()->ModernApplicationProtocol.encode(result,ModernPlayerHealingApplicationTest.peer(ext)));
            if(healing)assertThrows(IllegalArgumentException.class,()->ModernApplicationProtocol.encode(result,armor));
            assertArrayEquals(encoded,ModernApplicationProtocol.encode(result,ModernPlayerHealingApplicationTest.peer(healing?5:4)));
            sink.send(label,peer.extensions(),ModernApplicationProtocol.encode(request,peer),encoded);
        });
    }
    @Test void selectedReadPreservesAllArmorHealingStatusAndPrivateHistory()throws Exception {
        int[] count={0};trace(root,(label,ext,q,r)->count[0]++);assertEquals(68,count[0]);
    }
    @Test void healingRepliesUseTheSameSelectedArmorPayloadAndOldCandidateStaysClosed()throws Exception {
        int[] count={0};healingTrace(root,(label,ext,q,r)->count[0]++);assertEquals(2,count[0]);
    }
    static void healingTrace(Path root,Sink sink)throws Exception {
        healingTrace(root,sink,7);
    }
    static void healingTrace(Path root,Sink sink,int extensions)throws Exception {
        try(var f=new ModernBattleCanonicalEquipmentTest.Fixture(root,new ModernEquipmentOverrideStore.FileOps(){},
                ModernBattleEngine.PreparedMember::withPlayerHealing,ModernBattleArmorWireTest::source,true)) {
            ModernArmorProtectionEquipmentTest.active(f);ModernArmorProtectionEquipmentTest.hit(f,1);
            var c=f.capture();var q=ModernPlayerHealingProtocolTest.intent(c,0,1,0);
            var peer=handshake(extensions).selected();
            var before=ModernBattleArmorWireTest.publication(c).audience(q.proof().player());
            var read=new ModernBattleReadProtocol.Request(before.personal().scope(),before.world().identity());
            sink.send("heal-before",extensions,ModernApplicationProtocol.encode(new BattleReadRequest(2,read),peer),
                    ModernApplicationProtocol.encode(new BattleReadResult(2,read,new ModernBattleReadProtocol.Reply(ModernBattleReadProtocol.State.DATA,before)),peer));
            var cast=f.world.engine.canonicalPlayerHeal(f.world.players.get(0).owner(),q);
            assertEquals(ModernPlayerHealingCast.Reason.NONE,cast.reason());
            var p=ModernBattleArmorWireTest.publication(cast.observation());
            var reply=new ModernPlayerHealingProtocol.Reply(q,cast.reason(),p.audience(q.proof().player()),ModernBattleLifecycle.from(p));
            var encoded=ModernApplicationProtocol.encode(new PlayerHealingResult(reply),peer);
            assertArrayEquals(ModernBattleWire.Encoding.selected(peer).payload(reply.view()),Arrays.copyOfRange(encoded,192,encoded.length));
            assertThrows(IllegalArgumentException.class,()->ModernPlayerHealingProtocol.encodeCandidateReply(reply));
            for(int ext:new int[]{0,1,3,4,5})assertThrows(IllegalArgumentException.class,()->ModernPlayerHealingProtocol.encodeReply(reply,ModernPlayerHealingApplicationTest.peer(ext)));
            sink.send("heal-cast",extensions,ModernApplicationProtocol.encode(new PlayerHealingRequest(q),peer),encoded);
        }
    }
}
