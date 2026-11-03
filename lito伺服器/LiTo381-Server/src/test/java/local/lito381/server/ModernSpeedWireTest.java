package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleSlowTest.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Actual monitor-owned publications. Synthetic terrain/catalogs, no TLS/UI
 * capability, native NPC resource availability or source inventory writes. */
class ModernSpeedWireTest {
    @FunctionalInterface interface Sink {void accept(String label,ModernBattleAudience.View view)throws Exception;}
    static void pair(Sink sink,String label,ModernBattleObservation.Capture c,ModernNpcCatalog.Catalog catalog)throws Exception {
        var p=ModernSpeedEffectsTest.publish(c,catalog);
        for(var a:c.actors())if(a.scope()!=null)sink.accept(label+"-"+a.pose().entityId(),p.audience(a.scope()));
    }
    static void trace(Sink sink)throws Exception {
        try(var f=dualSpeedFixture()) {
            pair(sink,"pc-loading",f.engine.serviceObservation(),null);
            pair(sink,"pc-active",active(f),null);f.randomMode=3;
            pair(sink,"pc-slow",cast(f,0,1,1).observation(),null);
            pair(sink,"pc-greater-neutral",ModernBattleGreaterHasteTest.cast(f,1,1).observation(),null);
            f.elapsed(2_000_000_000L);
            pair(sink,"pc-greater",ModernBattleGreaterHasteTest.cast(f,1,2).observation(),null);
            pair(sink,"pc-slow-neutral",cast(f,0,2,1).observation(),null);
            f.elapsed(4_000_000_000L);pair(sink,"pc-slow-again",cast(f,0,3,1).observation(),null);
            f.elapsed(4_400_000_000L);
            pair(sink,"pc-haste-neutral",ModernBattleHasteTest.cast(f,1,3,1).observation(),null);
            f.elapsed(6_000_000_000L);
            pair(sink,"pc-slow-final",cast(f,0,4,1).observation(),null);
            f.elapsed(8_000_000_000L);f.randomMode=0;
            pair(sink,"pc-miss-keeps",cast(f,0,5,1).observation(),null);
            f.elapsed(70_000_000_000L);pair(sink,"pc-expired",f.engine.serviceObservation(),null);
            pair(sink,"pc-abort",f.engine.abortObservation(),null);
        }
        try(var f=fixture(1,5_000_000_000L,1000,false)) {
            active(f);f.randomMode=3;pair(sink,"pc-wide",cast(f,0,1,1).observation(),null);
        }
        try(var f=ModernBattleNpcSlowTest.fixture(false,2,0)) {
            ModernBattleNpcHasteTest.active(f);var n=ModernBattleNpcHasteTest.spawn(f,0,14,10);
            pair(sink,"npc-start",f.engine.serviceObservation(),f.config.catalog());
            pair(sink,"npc-slow",ModernBattleNpcSlowTest.cast(f,0,1,n).observation(),f.config.catalog());
            f.elapsed(100);assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            pair(sink,"npc-walk",f.engine.serviceObservation(),f.config.catalog());
            f.elapsed(2000);pair(sink,"npc-neutral",ModernBattleNpcHasteTest.cast(f,0,2,ModernBattleNpcHasteTest.npc(f.engine.serviceObservation())).observation(),f.config.catalog());
            f.elapsed(4000);pair(sink,"npc-haste",ModernBattleNpcHasteTest.cast(f,0,3,ModernBattleNpcHasteTest.npc(f.engine.serviceObservation())).observation(),f.config.catalog());
            f.elapsed(6000);pair(sink,"npc-slow-neutral",ModernBattleNpcSlowTest.cast(f,0,4,ModernBattleNpcHasteTest.npc(f.engine.serviceObservation())).observation(),f.config.catalog());
            pair(sink,"npc-abort",f.engine.abortObservation(),f.config.catalog());
        }
        try(var f=ModernBattleNpcSlowTest.fixture(true,2,0)) {
            var c=ModernBattleNpcHasteTest.active(f);
            for(int i=0;i<2;i++)ModernBattleNpcSlowTest.cast(f,i,1,c.npcs().roster().actors().get(i));
            pair(sink,"quest-lanes",f.engine.serviceObservation(),f.config.catalog());
        }
        try(var f=ModernBattleNpcSlowTest.fixture(false,1,1)) {
            var c=ModernBattleNpcHasteTest.active(f);
            var result=f.engine.slow(f.players.get(0).owner(),request(c,0,1,target(c,1)));
            assertEquals(ModernSlowCast.Reason.NONE,result.reason());
            pair(sink,"bot-target",result.observation(),f.config.catalog());
        }
        try(var f=ModernBattleNpcSlowTest.fixture(false,2,0,ModernSlowNativeRulesTest.catalog(45912))) {
            ModernBattleNpcHasteTest.active(f);var n=ModernSlowNativeRulesTest.spawn(f,45912,11);
            pair(sink,"npc-prerequisite",ModernBattleNpcSlowTest.cast(f,0,1,n).observation(),f.config.catalog());
            pair(sink,"npc-prerequisite-abort",f.engine.abortObservation(),f.config.catalog());
        }
        terminalTrace(sink);
        equipmentTrace(sink);
        try(var f=fixture(6,1000,1000,false,15)) {
            active(f);f.randomMode=3;
            for(int seq=1;seq<=3;seq++){f.elapsed((seq-1)*2_000_000_000L);assertEquals(ModernSlowCast.Reason.NONE,cast(f,0,seq,1).reason());}
            pair(sink,"budget-before",f.engine.serviceObservation(),null);
            f.elapsed(6_000_000_000L);f.randomMode=0;
            pair(sink,"budget-miss",cast(f,0,4,1).observation(),null);
        }
    }
    static void equipmentTrace(Sink sink)throws Exception {
        // Own only this unique temporary tree; never touch saved player overrides.
        var root=java.nio.file.Files.createTempDirectory("lito-speed-wire-gear-").toAbsolutePath().normalize();
        try(var f=ModernSlowEquipmentEngineTest.fixture(root)) {
            var gear=f.read();ModernSlowEquipmentEngineTest.active(f);
            pair(sink,"gear-active",f.capture(),null);
            pair(sink,"gear-immune",ModernSlowEquipmentEngineTest.cast(f,0,0).observation(),null);
            var changed=f.set(gear,false);
            assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,changed.publication().disposition());
            pair(sink,"gear-unequip",changed.observation(),null);
            f.world.elapsed(2_000_000_000L);
            pair(sink,"gear-slow",ModernSlowEquipmentEngineTest.cast(f,0,0).observation(),null);
            var restored=f.set(f.read(),true);
            assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,restored.publication().disposition());
            pair(sink,"gear-reequip",restored.observation(),null);
            pair(sink,"gear-other-immune",ModernSlowEquipmentEngineTest.cast(f,1,0).observation(),null);
            pair(sink,"gear-abort",f.world.engine.abortObservation(),null);
        } finally {
            try(var paths=java.nio.file.Files.walk(root)) {
                for(var path:paths.sorted(Comparator.reverseOrder()).toList()) {
                    if(!path.toAbsolutePath().normalize().startsWith(root))throw new AssertionError("unsafe temporary path");
                    java.nio.file.Files.delete(path);
                }
            }
        }
    }
    static void terminalTrace(Sink sink)throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(2,16,100,false,1,true,true,true,1,true,true)) {
            ModernHasteTimeAttackTest.active(f);f.random=0;var c=f.engine.serviceObservation();
            ModernSlowTerminalModesTest.slow(f,0,target(c,0));
            f.elapsed(3000);f.random=19;assertEquals(ModernNpcBattle.Reason.ATTACKED,f.tower(0,1).reason());
            f.elapsed(6000);f.random=0;ModernSlowTerminalModesTest.slow(f,0,ModernBattleNpcSlowTest.target(ModernHasteTimeAttackTest.living(f)));
            pair(sink,"time-slow",f.engine.serviceObservation(),f.catalog);
            f.elapsed(63000);pair(sink,"time-timeout",f.engine.serviceObservation(),f.catalog);
        }
        for(int skill:List.of(0,4,70,77))try(var f=new ModernTimeAttackEngineTest.Fixture(2,16,100,true,1,true,true,true,1,true,true)) {
            ModernHasteTimeAttackTest.active(f);f.random=19;assertEquals(ModernNpcBattle.Reason.ATTACKED,f.tower(0,1).reason());
            for(int wave=0;wave<f.plan.waves().size();wave++){
                f.elapsed(3000+wave*7000);f.random=0;
                ModernSlowTerminalModesTest.slow(f,0,ModernBattleNpcSlowTest.target(ModernHasteTimeAttackTest.living(f)));
                f.elapsed(5000+wave*7000);ModernHasteTimeAttackTest.kill(f,skill);
            }
            pair(sink,"time-win-"+skill,f.engine.serviceObservation(),f.catalog);
        }
        try(var f=new ModernBattleEngineSiegeTest.Fixture(0,1,60000,1000,0,true,15,true)) {
            for(var p:f.players)f.engine.ackLoadedObservation(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),f.binding.map().digest());
            f.hitSlow=true;var c=f.engine.serviceObservation();
            assertEquals(ModernSlowCast.Reason.NONE,f.engine.slow(f.players.get(0).owner(),request(c,0,1,target(c,0))).reason());
            // Thirty roster members, but only two authenticated human owners;
            // BOT participants do not have a private inventory/connection.
            var p=ModernSpeedEffectsTest.publish(f.engine.serviceObservation(),null);
            for(int owner:new int[]{0,1})sink.accept("siege-slow-"+(owner+1),p.audience(f.players.get(owner).scope()));
            f.waitMillis(60000);p=ModernSpeedEffectsTest.publish(f.engine.serviceObservation(),null);
            for(int owner:new int[]{0,1})sink.accept("siege-timeout-"+(owner+1),p.audience(f.players.get(owner).scope()));
        }
    }
    @Test void allEngineFramesEncodeDeterministicallyWithoutLegacyFallback()throws Exception {
        var labels=new HashSet<String>();var details=EnumSet.noneOf(ModernSlowReceipt.Outcome.class);
        trace((label,view)->{
            assertTrue(labels.add(label));var bytes=ModernBattleWire.encodeSpeed(view);
            assertArrayEquals(bytes,ModernBattleWire.encodeSpeed(view));
            assertArrayEquals(bytes,ModernBattleWire.encodeVersioned(view,46));
            assertTrue(view.world().descriptor().supportedBy(46,ModernSessionProtocol.KNOWN_CAPABILITIES));
            assertFalse(view.world().descriptor().supportedBy(46,ModernSessionProtocol.KNOWN_CAPABILITIES&~ModernSessionProtocol.CAP_SLOW_DATA));
            var echo=new ModernBattleReadProtocol.Request(view.personal().scope(),view.world().identity());
            var result=new ModernApplicationProtocol.BattleReadResult(77,echo,new ModernBattleReadProtocol.Reply(ModernBattleReadProtocol.State.DATA,view));
            var peer=new ModernSessionBoundary.Negotiated(1,46,1,ModernSessionProtocol.KNOWN_CAPABILITIES);
            assertSame(result,ModernMatchDescriptor.forPeer(result,peer));
            assertArrayEquals(ModernBattleReadProtocol.encodeResult(result,46),ModernApplicationProtocol.encode(ModernMatchDescriptor.forPeer(result,peer),46));
            assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(result,
                new ModernSessionBoundary.Negotiated(1,46,1,peer.capabilities()&~ModernSessionProtocol.CAP_SLOW_DATA)));
            for(int old=35;old<46;old++) {
                int version=old;
                assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(result,
                    new ModernSessionBoundary.Negotiated(1,version,1,peer.capabilities()&~ModernSessionProtocol.CAP_SLOW_DATA)));
            }
            assertEquals(6,java.nio.ByteBuffer.wrap(bytes).getShort(4));
            assertEquals(14,java.nio.ByteBuffer.wrap(bytes).getShort(12));
            assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encodeVersioned(view,45));
            for(var a:view.world().actors())for(var r:a.spells().receipts().values())
                if(r.detail() instanceof ModernSlowReceipt d)details.add(d.outcome());
        });
        assertEquals(81,labels.size());
        assertTrue(details.containsAll(List.of(ModernSlowReceipt.Outcome.APPLIED,ModernSlowReceipt.Outcome.NEUTRALIZED,ModernSlowReceipt.Outcome.MISS,ModernSlowReceipt.Outcome.NATIVE_IMMUNE)));
    }
}
