package local.lito381.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernPlayerHealingHistoryTest.*;

/** Local Java -> Rust -> Java bound section verification; no TLS/UI route. */
public final class ModernPlayerHealingHistoryWirePeer {
    private final BufferedReader input=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.US_ASCII));
    private int count;
    private String line()throws Exception {
        var value=new StringBuilder();int c;
        while((c=input.read())!=-1&&c!='\n') {if(c=='\r')continue;if(value.length()>=ModernEquipmentHistoryWire.MAX_BYTES*2)throw new IllegalArgumentException("peer line too long");value.append((char)c);}
        if(c<0)throw new IllegalStateException("peer input closed");return value.toString();
    }
    private void exchange(String label,ModernEquipmentHistory.State expected)throws Exception {
        var encoded=ModernPlayerHealingHistoryWireTest.encode(expected);
        System.out.println("STATE:"+label+":"+HexFormat.of().formatHex(encoded));System.out.flush();
        for(int attempt=0;attempt<2;attempt++)try {
            var decoded=ModernEquipmentHistoryWire.decodeBound(HexFormat.of().parseHex(line()),expected.identity(),expected.worldRevision(),5);
            if(!expected.equals(decoded)||!java.util.Arrays.equals(encoded,ModernPlayerHealingHistoryWireTest.encode(decoded)))throw new IllegalArgumentException("rewritten history");
            System.out.println("ACK:"+(++count));System.out.flush();return;
        }catch(IllegalArgumentException rejected){System.out.println("REJECTED");System.out.flush();}
        throw new IllegalStateException("peer acknowledgement rejected");
    }
    private void frames(String label,ModernBattleObservation.Capture c)throws Exception {
        for(var scope:c.ownerViews(c.world()).keySet().stream().sorted(java.util.Comparator.comparingInt(ModernRooms.Scope::sourceId)).toList())
            exchange(label+"-"+scope.sourceId(),c.forOwner(scope).owner().history().history());
    }
    private void run()throws Exception {
        var a=initial(7,1,true);var b=initial(42,2,false);
        exchange("initial-caster",a);exchange("initial-recipient",b);
        var self=cast(a,a,1).caster();exchange("self",self);
        var pair=cast(a,b,1);exchange("cross-caster",pair.caster());exchange("cross-recipient",pair.target());
        var p=new ModernEquipmentHistory.Profile(1,100,100,java.util.Map.of(ModernUseHistory.Lane.TELEPORT,2),0L);
        a=self.reprofile(3,p,0);exchange("repriced",a);
        a=resources(a,a.resources().regenerate(4,100-a.resources().vitals().hp(),0,0,0));
        a=cast(a,a,2).caster();exchange("full-free",a);
        a=resources(a,a.resources().damage(6,100).respawn(7,20,0));exchange("revived",a);
        a=cast(a,a,3).caster();exchange("revived-free",a);
        exchange("maximum",ModernPlayerHealingHistoryWireTest.enabled(ModernEquipmentHistoryWireTest.maximum()));
        exchange("wide",ModernPlayerHealingHistoryWireTest.enabled(ModernEquipmentHistoryWireTest.empty(-1,Long.MAX_VALUE)));
        for(int flags=0;flags<16;flags++) {
            var state=ModernPlayerHealingTransactionTest.state(false);
            var effects=new ModernPlayerHealing.Effects((flags&1)!=0,(flags&2)!=0,(flags&4)!=0,(flags&8)!=0);
            var prepared=ModernPlayerHealingTransaction.prepare(ModernPlayerHealingTransactionTest.target(state,
                    ModernPlayerHealingTransactionTest.effects(state.target(),effects)),2,1,bound->3);
            var result=ModernPlayerHealingTransaction.complete(prepared,initial(7,1,true),initial(42,2,false));
            exchange("effects-caster-"+flags,result.caster());exchange("effects-recipient-"+flags,result.target());
        }
        try(var f=ModernBattlePlayerHealingTest.fixture(1,100,100)) {
            frames("engine-initial",ModernBattlePlayerHealingTest.active(f));
            var bot=ModernBattlePlayerHealingTest.heal(f,0,1,2);assertEquals(ModernPlayerHealingCast.Reason.NONE,bot.reason());frames("engine-bot",bot.observation());
            var denied=ModernBattlePlayerHealingTest.heal(f,0,2,1);assertEquals(ModernPlayerHealingCast.Reason.TOO_FAST,denied.reason());frames("engine-denied",denied.observation());
            f.elapsed(2*ModernBattlePlayerHealingTest.SECOND);
            var cross=ModernBattlePlayerHealingTest.heal(f,0,3,1);assertEquals(ModernPlayerHealingCast.Reason.NONE,cross.reason());frames("engine-cross",cross.observation());
            f.elapsed(4*ModernBattlePlayerHealingTest.SECOND);
            assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(1,1,4,1).reason());frames("engine-hurt",f.engine.serviceObservation());
            var selfHeal=ModernBattlePlayerHealingTest.heal(f,0,4,0);assertEquals(ModernPlayerHealingCast.Reason.NONE,selfHeal.reason());frames("engine-self",selfHeal.observation());
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    public static void main(String[] args)throws Exception {if(args.length!=0)throw new IllegalArgumentException("no arguments");new ModernPlayerHealingHistoryWirePeer().run();}
}
