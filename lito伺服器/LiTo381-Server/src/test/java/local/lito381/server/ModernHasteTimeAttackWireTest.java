package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernHasteTimeAttackTest.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Real cooperative captures -> complete public schema5. Synthetic map554;
 * source separation, departed owners and terminal receipts are never flattened. */
class ModernHasteTimeAttackWireTest {
    static final int FRAMES=140;
    static void emit(ModernHasteWireTest.Sink sink,String label,ModernTimeAttackEngineTest.Fixture f)throws Exception {
        var p=ModernBattleWireTest.publication(view(f),f.catalog);
        ModernHasteWireTest.pair(sink,label,p);
    }
    static void trace(ModernHasteWireTest.Sink sink)throws Exception {
        for(String kind:List.of("clear0","clear4","clear70","clear77","timeout","defeat","depart","abort")) {
            try(var f=fixture(2,true,kind.equals("defeat")?1:100)) {
                emit(sink,kind+".loading",f);active(f);emit(sink,kind+".active",f);
                player(f,0,0);emit(sink,kind+".self",f);f.elapsed(2000);player(f,0,1);emit(sink,kind+".ally",f);
                f.elapsed(4000);assertEquals(ModernNpcBattle.Reason.ATTACKED,f.tower(0,1).reason());emit(sink,kind+".tower",f);
                f.elapsed(6000);npc(f,0);emit(sink,kind+".npc",f);f.elapsed(8000);
                if(kind.startsWith("clear")) {
                    int skill=Integer.parseInt(kind.substring(5));kill(f,skill);emit(sink,kind+".progress",f);
                    for(int wave=1;wave<f.plan.waves().size();wave++){
                        f.elapsed(6000+wave*5000);npc(f,0);f.elapsed(8000+wave*5000);kill(f,skill);
                    }
                    outcome(view(f),ModernTimeAttack.End.CLEARED,ModernMatchSettlement.Verdict.WIN);
                } else switch(kind) {
                    case "timeout" -> {
                        for(int i=0;i<2;i++)assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(i,1,10,10+i,6).status());
                        f.elapsed(8200);for(int i=0;i<2;i++)assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(i,2,9,10+i,6).status());
                        f.elapsed(8400);var n=living(f);assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),n.life(),4096,false));
                        emit(sink,kind+".walk",f);f.elapsed(24000);outcome(view(f),ModernTimeAttack.End.TIMEOUT,ModernMatchSettlement.Verdict.LOSS);
                    }
                    case "defeat" -> {
                        var n=living(f);assertEquals(ModernNpcBattle.Reason.ATTACKED,f.engine.stepNpcUnpublished(n.id(),n.life(),4096,false));
                        outcome(view(f),ModernTimeAttack.End.LEADER_DEFEATED,ModernMatchSettlement.Verdict.LOSS);
                    }
                    case "depart" -> {
                        f.engine.withdrawTimeAttackObservation(f.players.get(0).owner());emit(sink,kind+".left",f);
                        f.engine.withdrawTimeAttackObservation(f.players.get(1).owner());
                        outcome(view(f),ModernTimeAttack.End.PARTY_DEFEATED,ModernMatchSettlement.Verdict.LOSS);
                    }
                    case "abort" -> {f.engine.abortObservation();assertEquals(ModernBattleEngine.Phase.ABORTED,view(f).phase());cleared(view(f));}
                    default -> throw new AssertionError(kind);
                }
                emit(sink,kind+".terminal",f);emit(sink,kind+".repeat",f);
            }
        }
    }
    @Test void everyCooperativeExitKeepsAllTypedPublicOwnerAndTargetFacts()throws Exception {
        var labels=new ArrayList<String>();
        trace((label,v)->{
            ModernHasteWireTest.verified(v,45);labels.add(label);
            assertFalse(v.world().descriptor().supportedBy(44,ModernSessionProtocol.KNOWN_CAPABILITIES));
            assertTrue(v.world().descriptor().has(ModernMatchDescriptor.TIME_ATTACK|ModernMatchDescriptor.HASTE));
            assertEquals(2,v.world().actors().size());assertNotNull(v.world().hasteEffects().npcs());
            assertEquals(v.world().identity(),v.world().hasteEffects().world());
            if(label.contains(".terminal")||label.contains(".repeat")) {
                assertTrue(v.world().hasteEffects().targets().stream().allMatch(e->e.active()==null));
                if(!label.startsWith("abort"))assertNotNull(v.world().result());
            }
        });
        assertEquals(FRAMES,labels.size());assertEquals(FRAMES,labels.stream().distinct().count());
    }
}
