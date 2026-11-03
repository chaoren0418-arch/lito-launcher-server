package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleEngineArmorProtectionTest.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Explicit render fixtures only. Never rewrite the ordinary11101 fixture or
 * operator SQL: 0 means no configured proc image; 228 is explicitly configured. */
class ModernBattleArmorRenderTraceTest {
    static void emit(ModernArmorSelectedProtocolTest.Sink sink,String label,ModernBattleObservation.Capture c,
            List<ModernBattleEngineBotTest.Player> players)throws Exception {
        ModernBattleArmorWireTest.send((name,v)->{
            ModernBattleArmorPresentationWireTest.check(v);
            var peer=ModernArmorSelectedProtocolTest.handshake(12).selected();
            var q=new ModernBattleReadProtocol.Request(v.personal().scope(),v.world().identity());
            var reply=new ModernApplicationProtocol.BattleReadResult(2,q,
                    new ModernBattleReadProtocol.Reply(ModernBattleReadProtocol.State.DATA,v));
            sink.send(name,12,ModernApplicationProtocol.encode(new ModernApplicationProtocol.BattleReadRequest(2,q),peer),
                    ModernApplicationProtocol.encode(reply,peer));
        },label,c,players);
    }
    static void trace(ModernArmorSelectedProtocolTest.Sink sink)throws Exception {
        for(int graphic:new int[]{0,228})try(var f=ModernArmorPresentationTest.variant(graphic,2,2)) {
            String prefix="gfx"+graphic;
            emit(sink,prefix+"-loading",f.engine.serviceObservation(),f.players);
            active(f);emit(sink,prefix+"-active",f.engine.serviceObservation(),f.players);
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());
            emit(sink,prefix+"-hit",f.engine.serviceObservation(),f.players);
            f.elapsed(200_000_000L);assertEquals(ModernBattleEngine.MoveStatus.MOVED,move(f,1,1,2).status());
            emit(sink,prefix+"-moved",f.engine.serviceObservation(),f.players);
            f.elapsed(400_000_000L);assertEquals(ModernBattleEngine.MoveStatus.MOVED,move(f,1,2,6).status());
            emit(sink,prefix+"-back",f.engine.serviceObservation(),f.players);
            f.elapsed(SECOND);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,2,2).status());
            emit(sink,prefix+"-again",f.engine.serviceObservation(),f.players);
            f.elapsed(4*SECOND);emit(sink,prefix+"-expired",f.engine.serviceObservation(),f.players);
            emit(sink,prefix+"-abort",f.engine.abortObservation(),f.players);
        }
        try(var f=ModernArmorPresentationTest.variant(228,2,ModernArmorProcs.MAX_ROWS)) {
            active(f);emit(sink,"max-active",f.engine.serviceObservation(),f.players);
            f.attack(0,1,2);emit(sink,"max-hit",f.engine.serviceObservation(),f.players);
        }
    }
    @Test void explicitSharedGraphicsTraceRetainsTheActualEngineMultiplicityAndAnchors()throws Exception {
        int[] count={0};trace((label,ext,q,r)->count[0]++);assertEquals(36,count[0]);
    }
}
