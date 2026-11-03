package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleEngineArmorProtectionTest.*;
import static local.lito381.server.ModernApplicationProtocol.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernBattleArmorPresentationWireTest {
    @TempDir Path root;
    static void check(ModernBattleAudience.View v) {
        var old=ModernBattleWire.encodeArmorProtection(v);var oldParts=ModernBattleArmorWireTest.sections(old);
        var bytes=ModernBattleArmorPresentationWire.encode(v);var b=ByteBuffer.wrap(bytes);
        assertTrue(bytes.length<=ModernBattleArmorPresentationWire.MAX_BYTES);
        assertEquals(ModernBattleWire.MAGIC,b.getInt());assertEquals(9,b.getShort());assertEquals(0,b.getShort());
        assertEquals(bytes.length,b.getInt());assertEquals(17,b.getShort());assertEquals(0,b.getShort());
        for(int id=1;id<=16;id++) {
            assertEquals(id,b.getShort());assertEquals(0,b.getShort());var part=new byte[b.getInt()];b.get(part);
            assertArrayEquals(oldParts.get(id),part,"unchanged schema8 section "+id);
        }
        assertEquals(17,b.getShort());assertEquals(0,b.getShort());int length=b.getInt();
        assertEquals(b.remaining(),length);assertTrue(length<=ModernBattleArmorPresentationWire.LIMIT);
        var c=v.armorPresentation();assertEquals(c.world().roomId(),b.getLong());assertEquals(c.world().matchId(),b.getLong());assertEquals(c.world().generation(),b.getLong());
        assertEquals(c.worldRevision(),b.getLong());assertEquals(c.entityId(),b.getLong());assertEquals(c.life(),b.getLong());assertEquals(c.equipmentRevision(),b.getLong());
        assertEquals(c.required().graphics().size(),b.getInt());for(int id:c.required().graphics())assertEquals(id,b.getInt());
        assertEquals(c.required().icons().size(),b.getInt());for(int id:c.required().icons())assertEquals(id,b.getInt());
        assertEquals(c.batch()==null?0:1,b.get());
        if(c.batch()!=null) {
            var e=c.batch();assertEquals(e.sequence(),b.getLong());assertEquals(e.revision(),b.getLong());assertEquals(e.observedNanos(),b.getLong());
            assertEquals(e.anchor().moveSequence(),b.getLong());assertEquals(e.anchor().cell().x(),b.getInt());assertEquals(e.anchor().cell().y(),b.getInt());
            assertEquals(e.visuals().size(),b.getInt());for(var x:e.visuals()){assertEquals(x.ordinal(),b.getInt());assertEquals(x.graphic(),b.getInt());assertEquals(x.role().ordinal(),b.get());}
        }
        assertFalse(b.hasRemaining());assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encode(v,9));
        boolean healing=v.world().descriptor().has(ModernMatchDescriptor.PLAYER_HEALING);
        var peer=ModernPlayerHealingApplicationTest.peer(healing?15:12);
        assertArrayEquals(bytes,ModernBattleWire.Encoding.selected(peer).payload(v));
        assertEquals(ModernBattleArmorPresentationWire.MAX_BYTES,ModernBattleWire.Encoding.selected(peer).maximum());
        assertArrayEquals(old,ModernBattleWire.Encoding.selected(ModernPlayerHealingApplicationTest.peer(healing?7:4)).payload(v));
        if(healing)assertArrayEquals(bytes,ModernBattleWire.Encoding.selected(ModernPlayerHealingApplicationTest.peer(13)).payload(v));
    }
    static void trace(Path root,ModernBattleArmorWireTest.Sink sink)throws Exception {
        ModernBattleArmorWireTest.trace(root,sink);
        try(var f=fixture(10000,false)) {
            ModernBattleArmorWireTest.send(sink,"anchor-loading",f.engine.serviceObservation(),f.players);active(f);
            f.attack(0,1,2);ModernBattleArmorWireTest.send(sink,"anchor-hit",f.engine.serviceObservation(),f.players);
            f.elapsed(SECOND);assertEquals(ModernBattleEngine.MoveStatus.MOVED,move(f,1,1,2).status());
            ModernBattleArmorWireTest.send(sink,"anchor-moved",f.engine.serviceObservation(),f.players);
            f.elapsed(2*SECOND);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(1,1,5,0).reason());
            ModernBattleArmorWireTest.send(sink,"anchor-teleport",f.engine.serviceObservation(),f.players);
            f.elapsed(3*SECOND);ModernBattleArmorWireTest.send(sink,"anchor-clear",f.engine.serviceObservation(),f.players);
        }
        for(int mode=0;mode<=2;mode++)try(var f=ModernArmorPresentationTest.variant(11101,mode,2)) {
            active(f);f.attack(0,1,2);ModernBattleArmorWireTest.send(sink,"mode"+mode+"-hit",f.engine.serviceObservation(),f.players);
            f.elapsed(SECOND);f.attack(0,2,2);ModernBattleArmorWireTest.send(sink,"mode"+mode+"-again",f.engine.serviceObservation(),f.players);
        }
        try(var f=ModernArmorPresentationTest.variant(11101,2,ModernArmorProcs.MAX_ROWS)) {
            active(f);f.attack(0,1,2);ModernBattleArmorWireTest.send(sink,"max-hit",f.engine.serviceObservation(),f.players);
        }
    }
    static void selectedTrace(Path root,ModernArmorSelectedProtocolTest.Sink sink)throws Exception {
        trace(root,(label,v)->{
            check(v);int ext=v.world().descriptor().has(ModernMatchDescriptor.PLAYER_HEALING)?15:12;
            var peer=ModernArmorSelectedProtocolTest.handshake(ext).selected();
            var q=new ModernBattleReadProtocol.Request(v.personal().scope(),v.world().identity());
            var result=new BattleReadResult(2,q,new ModernBattleReadProtocol.Reply(ModernBattleReadProtocol.State.DATA,v));
            var raw=ModernApplicationProtocol.encode(result,peer);
            assertArrayEquals(ModernBattleArmorPresentationWire.encode(v),Arrays.copyOfRange(raw,72,raw.length));
            sink.send(label,ext,ModernApplicationProtocol.encode(new BattleReadRequest(2,q),peer),raw);
        });
    }
    @Test void everyRealCaptureKeepsOldSectionsAndAddsOnlyBoundedOwnerPresentation()throws Exception {
        int[] count={0};selectedTrace(root,(label,ext,q,r)->count[0]++);assertEquals(92,count[0]);
        assertEquals(22654,ModernBattleArmorPresentationWire.LIMIT);
        assertEquals(18485,ModernBattleArmorPresentationWire.BATCH_LIMIT);
    }
    @Test void newExtensionRequiresArmorButCannotChangeOldNegotiationOrGrantHealingCommands()throws Exception {
        for(int ext:new int[]{12,13,15}) {
            var n=ModernArmorSelectedProtocolTest.handshake(ext).selected();
            assertTrue(ModernSessionExtensions.allowsArmorPresentation(n));assertTrue(ModernSessionExtensions.allowsArmor(n));
            assertEquals(ext!=12,ModernSessionExtensions.allowsData(n));assertEquals(ext==15,ModernSessionExtensions.allowsCommands(n));
        }
        for(int ext:new int[]{0,1,3,4,5,7})assertFalse(ModernSessionExtensions.allowsArmorPresentation(ModernPlayerHealingApplicationTest.peer(ext)));
        for(int ext:new int[]{8,9,10,11,14,32})assertThrows(IllegalArgumentException.class,()->new ModernSessionExtensions.Offer(ext,0));
        for(var old:List.of(ModernSessionExtensions.HEALING,ModernSessionExtensions.ARMOR,ModernSessionExtensions.HEALING_ARMOR)) {
            var boundary=new ModernSessionBoundary(ModernSessionExtensionsTest.policy(),old);
            var hello=ModernSessionProtocol.encode(new ModernSessionProtocol.ExtendedHello(ModernSessionExtensionsTest.base(),ModernSessionExtensions.ARMOR_PRESENTATION));
            assertThrows(ModernSessionProtocol.ProtocolException.class,()->boundary.acceptHello(hello));
        }
        assertEquals(7,ModernSessionExtensions.HEALING_ARMOR.required());assertEquals(3,ModernSessionExtensions.HEALING.required());
    }
    @Test void selectedHealingCommandRetainsTheOwnerPresentationSection()throws Exception {
        int[] count={0};ModernArmorSelectedProtocolTest.healingTrace(root,(label,ext,q,r)->{
            assertEquals(15,ext);int prefix=label.equals("heal-before")?72:192;
            assertEquals(9,ByteBuffer.wrap(r,prefix+4,2).getShort());count[0]++;
        },15);assertEquals(2,count[0]);
    }
}
