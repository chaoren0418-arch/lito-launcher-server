package local.lito381.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentHistoryTest.*;
import static local.lito381.server.ModernTargetHistoryTest.CELL;

/** Real engine -> Java encoder -> Rust decoder/encoder -> Java bound decoder.
 * A local payload peer, not a production SET endpoint or TLS/GUI acceptance. */
public final class ModernEquipmentHistoryWirePeer {
    private final BufferedReader input=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.US_ASCII));
    private int count;
    private String line()throws Exception {
        var text=new StringBuilder();int c;
        while((c=input.read())!=-1&&c!='\n') {if(c=='\r')continue;if(text.length()>=ModernEquipmentHistoryWire.MAX_BYTES*2)throw new IllegalArgumentException("peer line too long");text.append((char)c);}
        if(c<0)throw new IllegalStateException("peer input closed");return text.toString();
    }
    private void exchange(String label,ModernEquipmentHistory.State expected)throws Exception {
        var encoded=ModernEquipmentHistoryWire.encode(expected);
        System.out.println("STATE:"+label+":"+HexFormat.of().formatHex(encoded));System.out.flush();
        for(int attempt=0;attempt<2;attempt++) {
            var hex=line();
            try {
                var decoded=ModernEquipmentHistoryWire.decodeBound(HexFormat.of().parseHex(hex),expected.identity(),expected.worldRevision());
                if(!expected.equals(decoded)||!java.util.Arrays.equals(encoded,ModernEquipmentHistoryWire.encode(decoded)))throw new IllegalArgumentException("rewritten history");
                System.out.println("ACK:"+(++count));System.out.flush();return;
            }catch(IllegalArgumentException rejected){System.out.println("REJECTED");System.out.flush();}
        }
        throw new IllegalStateException("peer acknowledgement rejected");
    }
    private void run()throws Exception {
        var state=populated();exchange("all-lanes",state);
        state=state.reprofile(12,next(state,100,50));exchange("repriced",state);
        for(var lane:LEGACY_LANES) {
            state=state.cast(lane,state.worldRevision()+1,9+lane.ordinal(),CELL,impacts(lane));exchange("mixed-"+lane.ordinal(),state);
        }
        exchange("maximum",ModernEquipmentHistoryWireTest.maximum());exchange("wide",ModernEquipmentHistoryWireTest.empty(-1,Long.MAX_VALUE));
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();var p=f.players.get(0);exchange("real-honor-before",ModernBattleEquipmentHistoryTest.read(f.engine,f.ticket,p));
            assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,4,2).reason());
            exchange("real-honor-cast",ModernBattleEquipmentHistoryTest.read(f.engine,f.ticket,p));
            assertThrows(IllegalStateException.class,()->f.engine.beginEquipmentChange(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()));
        }
        try(var f=ModernBattleEngineNpcFireStormTest.fixture(1000,10000,0,true,true,false,0)) {
            f.active();var p=f.players.get(0);var n=f.spawn(0,11,10);exchange("real-npc-before",ModernBattleEquipmentHistoryTest.read(f.engine,f.ticket,p));
            ModernBattleEngineNpcMagicTest.cast(f,0,1,n);exchange("real-npc-arrow",ModernBattleEquipmentHistoryTest.read(f.engine,f.ticket,p));
            f.elapsed(1200);ModernBattleEngineNpcFireStormTest.cast(f,2);exchange("real-npc-fire",ModernBattleEquipmentHistoryTest.read(f.engine,f.ticket,p));
            f.elapsed(2400);ModernBattleEngineNpcDisintegrateTest.cast(f,0,3,n);exchange("real-npc-dis",ModernBattleEquipmentHistoryTest.read(f.engine,f.ticket,p));
        }
        try(var f=ModernNpcFireStormStructuresTest.quest(513,1)) {
            f.active();var p=f.players.get(0);ModernNpcFireStormStructuresTest.cast(f,0,1);f.elapsed(2000);ModernNpcFireStormStructuresTest.cast(f,0,2);
            exchange("real-quest-structure",ModernBattleEquipmentHistoryTest.read(f.engine,f.ticket,p));
            f.elapsed(4000);ModernNpcFireStormStructuresTest.cast(f,0,3);exchange("real-quest-retained",ModernBattleEquipmentHistoryTest.read(f.engine,f.ticket,p));
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    public static void main(String[] args)throws Exception {if(args.length!=0)throw new IllegalArgumentException("no arguments");new ModernEquipmentHistoryWirePeer().run();}
}
