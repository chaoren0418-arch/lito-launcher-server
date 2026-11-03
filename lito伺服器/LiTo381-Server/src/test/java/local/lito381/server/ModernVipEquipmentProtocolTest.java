package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernVipEquipment.Field.*;
import static local.lito381.server.ModernVipEquipmentProtocol.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Actual engine+authority+durable store -> selected application codec.
 * Pipe peer is headless and does not register Main/WorldService or player UI. */
public class ModernVipEquipmentProtocolTest {
    @TempDir Path directory;
    record Frame(String label,Request request,Reply reply) {}
    static ModernSessionBoundary.Negotiated peer()throws Exception{return ModernVipSelectedProtocolTest.handshake(63).peer();}
    static Request set(int id,ModernVipEquipmentAuthority.Quote quote,long item,boolean desired) {
        return new Request(id,quote.proof(),Action.SET,item,desired,quote.token());
    }
    static ModernBattleVipWire.Payload payload(ModernBattleObservation.Capture capture,Request q) {
        return ModernBattleVipWire.candidate(capture,ModernBattleVipWireTest.descriptor(capture,capture.result()!=null),q.proof().life().player());
    }
    static Reply run(ModernVipEquipmentAuthorityTest.Fixture f,int index,Request request)throws Exception {
        var selected=peer();var bytes=ModernApplicationProtocol.encode(new ModernApplicationProtocol.VipEquipmentRequest(request),selected);
        var message=(ModernApplicationProtocol.VipEquipmentRequest)ModernApplicationProtocol.decode(bytes,selected);var q=message.request();
        assertEquals(request,q);
        var admission=f.world.engine.admitVipEquipmentChange(f.player(index).owner(),q.proof());
        if(admission.change()==null) {
            var p=payload(admission.observation(),q);return rejected(q,admission,p.descriptor(),ModernBattleLifecycle.fromVip(p));
        }
        if(q.action()==Action.INSPECT) {
            var inspection=f.call(index,operation->f.authority.inspectCaptured(f.principal,f.row(index),operation,q.proof(),admission.change()));
            var p=payload(inspection.observation(),q);return inspected(q,inspection,p.descriptor(),ModernBattleLifecycle.fromVip(p));
        }
        var result=f.call(index,operation->f.authority.execute(f.principal,f.row(index),operation,q.command(),admission.change()));
        var p=payload(result.observation(),q);return executed(q,result,p.descriptor(),ModernBattleLifecycle.fromVip(p));
    }
    static ModernVipEquipmentAuthority.Quote quote(Reply reply){return new ModernVipEquipmentAuthority.Quote(reply.payload().view(),reply.token());}
    static List<Frame> frames(Path root)throws Exception {
        var result=new ArrayList<Frame>();
        try(var f=new ModernVipEquipmentAuthorityTest.Fixture(Files.createDirectory(root.resolve("wide")),false,Map.of(MAX_HP,4_294_967_299L,CHA,5L))) {
            var inspect=Request.inspect(0xfedcba98,f.proof(0));var first=run(f,0,inspect);result.add(new Frame("wide-inspect",inspect,first));
            var sibling=Request.inspect(2,f.proof(1));result.add(new Frame("moon-inspect",sibling,run(f,1,sibling)));
            var same=set(3,quote(first),1,true);result.add(new Frame("wide-noop",same,run(f,0,same)));
            var missing=set(4,quote(first),99999,true);result.add(new Frame("wide-denied",missing,run(f,0,missing)));
            f.sources.set(0,ModernVipLifecycleTest.change(f.sources.get(0),300086,CHA,9000,0,null));
            var conflict=set(5,quote(first),1,false);result.add(new Frame("wide-conflict",conflict,run(f,0,conflict)));
            var current=Request.inspect(6,f.proof(0));var refreshed=run(f,0,current);result.add(new Frame("wide-refreshed",current,refreshed));
            var remove=set(7,quote(refreshed),1,false);var removed=run(f,0,remove);result.add(new Frame("wide-remove",remove,removed));
            result.add(new Frame("wide-stale-saved",remove,run(f,0,remove)));
            var equip=set(8,quote(removed),1,true);var equipped=run(f,0,equip);result.add(new Frame("wide-reequip",equip,equipped));
            assertEquals(9000,equipped.payload().view().personal().equipment().vipAbilities().get(CHA));assertTrue(f.stored(1).isEmpty());
        }
        try(var f=new ModernVipEquipmentAuthorityTest.Fixture(Files.createDirectory(root.resolve("expiry")),true,Map.of(MAX_HP,100L))) {
            var before=Request.inspect(20,f.proof(0));var a=run(f,0,before);result.add(new Frame("expiry-before",before,a));
            f.world.epoch.set(2000);var stale=set(21,quote(a),3,true);result.add(new Frame("expiry-stale-runtime",stale,run(f,0,stale)));
            var after=Request.inspect(22,f.proof(0));var b=run(f,0,after);result.add(new Frame("expiry-inspect",after,b));
            var ordinary=set(23,quote(b),3,true);var c=run(f,0,ordinary);result.add(new Frame("expiry-ordinary",ordinary,c));
            assertEquals(new ModernBattleVipObservation.Versions(1,2,2),c.payload().view().personal().equipment().versions());
            var denied=set(24,quote(c),1,true);result.add(new Frame("expiry-denied",denied,run(f,0,denied)));
        }
        var move=new AtomicReference<Runnable>();
        try(var f=new ModernVipEquipmentAuthorityTest.Fixture(Files.createDirectory(root.resolve("retired")),true,Map.of(MAX_HP,100L),
                ModernVipEquipmentAuthorityTest.ops(new AtomicReference<>(),move))) {
            var first=Request.inspect(30,f.proof(0));var a=run(f,0,first);result.add(new Frame("retired-inspect",first,a));
            move.set(()->{f.world.epoch.set(2000);f.world.view();});var remove=set(31,quote(a),1,false);
            var r=run(f,0,remove);result.add(new Frame("retired-save",remove,r));assertEquals(Outcome.PERSISTED_RETIRED,r.outcome());
            var after=Request.inspect(32,f.proof(0));result.add(new Frame("retired-rejected",after,run(f,0,after)));
        }
        return List.copyOf(result);
    }
    @Test void actualAuthorityRepliesRetainEveryDispositionAndIndependentVersions()throws Exception {
        var frames=frames(directory);assertEquals(17,frames.size());var outcomes=EnumSet.noneOf(Outcome.class);
        for(var f:frames) {
            outcomes.add(f.reply().outcome());var bytes=ModernApplicationProtocol.encode(new ModernApplicationProtocol.VipEquipmentResult(f.reply()),peer());
            assertEquals(RESULT_KIND,ByteBuffer.wrap(bytes).getShort(12));assertEquals(bytes.length,ByteBuffer.wrap(bytes).getInt(4));
            assertArrayEquals(ModernBattleVipWire.encode(f.reply().payload()),Arrays.copyOfRange(bytes,RESULT_PREFIX_BYTES,bytes.length));
            assertEquals("VipEquipmentReceipt[redacted]",f.reply().toString());
        }
        assertEquals(EnumSet.allOf(Outcome.class),outcomes);
    }
    @Test void dataOnlyAndLegacyContextsCannotReadOrWriteDedicatedRequests()throws Exception {
        try(var f=new ModernVipEquipmentAuthorityTest.Fixture(directory,false,Map.of(MAX_HP,100L))) {
            var q=Request.inspect(1,f.proof(0));var bytes=encodeRequest(q,peer());
            for(int bits:new int[]{0,1,3,4,7,12,15,16,17,28,29,31}) {
                var denied=ModernVipSelectedProtocolTest.handshake(bits).peer();
                assertFalse(ModernSessionExtensions.allowsVipEquipment(denied));
                assertThrows(Exception.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.VipEquipmentRequest(q),denied));
                assertThrows(Exception.class,()->ModernApplicationProtocol.decode(bytes,denied));
                var input=new ByteArrayInputStream(bytes);assertThrows(Exception.class,()->ModernApplicationProtocol.readFrame(input,denied));assertEquals(bytes.length-16,input.available());
            }
            for(int v=1;v<=47;v++) {int version=v;assertThrows(Exception.class,()->ModernApplicationProtocol.decode(bytes,version));
                assertThrows(Exception.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.VipEquipmentRequest(q),version));}
            assertThrows(IllegalArgumentException.class,()->new ModernSessionExtensions.Offer(32,32));
            assertThrows(IllegalArgumentException.class,()->new ModernSessionExtensions.Offer(64,0));
            for(int bits:new int[]{48,49,60,61,63})assertTrue(ModernSessionExtensions.allowsVipEquipment(ModernVipSelectedProtocolTest.handshake(bits).peer()));
        }
    }
    @Test void completeRequestsAreExactBoundedAndCoalescedFramesStayUnread()throws Exception {
        try(var f=new ModernVipEquipmentAuthorityTest.Fixture(directory,false,Map.of(MAX_HP,100L))) {
            var p=peer();var q=Request.inspect(0xabcdef01,f.proof(0));var bytes=encodeRequest(q,p);
            var stream=new ByteArrayInputStream(concat(bytes,bytes));assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(stream,p));assertEquals(bytes.length,stream.available());
            for(int length:new int[]{0,15,155,157,Integer.MAX_VALUE,Integer.MIN_VALUE}) {
                var bad=bytes.clone();ByteBuffer.wrap(bad).putInt(4,length);var input=new ByteArrayInputStream(bad);
                assertThrows(Exception.class,()->ModernApplicationProtocol.readFrame(input,p));assertEquals(bad.length-16,input.available());
            }
            for(int length=0;length<bytes.length;length++) {var cut=Arrays.copyOf(bytes,length);assertThrows(Exception.class,()->decodeRequest(cut,p));}
            for(int at:new int[]{0,8,16,20,28,40,48,56,64,72,80}) {
                var bad=bytes.clone();if(at==0||at==8||at==16)ByteBuffer.wrap(bad).putInt(at,0);else ByteBuffer.wrap(bad).putLong(at,0);
                assertThrows(Exception.class,()->decodeRequest(bad,p));
            }
            for(int at:new int[]{14,15,113,114,115,123,124}) {var bad=bytes.clone();bad[at]=1;assertThrows(Exception.class,()->decodeRequest(bad,p));}
            var runtime=bytes.clone();ByteBuffer.wrap(runtime).putLong(88,1);assertThrows(Exception.class,()->decodeRequest(runtime,p));
            var action=bytes.clone();action[112]=2;assertThrows(Exception.class,()->decodeRequest(action,p));
            var channel=bytes.clone();ByteBuffer.wrap(channel).putInt(36,4);assertThrows(Exception.class,()->decodeRequest(channel,p));
        }
    }
    @Test void receiptCannotClaimRuntimeAsSavedOrSubstituteTokensForFailure()throws Exception {
        for(var f:frames(directory)) {
            var r=f.reply();var p=r.payload().view().personal().equipment().versions();
            assertThrows(Exception.class,()->copy(r,r.rejection(),r.outcome(),r.denial(),r.blockingItem(),r.durableRevision()+1,r.token(),r.payload(),r.lifecycle()));
            if(r.outcome()==Outcome.APPLIED&&p.savedEquipmentRevision()!=p.runtimeAbilityRevision())
                assertThrows(Exception.class,()->copy(r,r.rejection(),r.outcome(),r.denial(),0,p.runtimeAbilityRevision(),r.token(),r.payload(),r.lifecycle()));
            if(r.token()!=null)assertThrows(Exception.class,()->copy(r,r.rejection(),r.outcome(),r.denial(),r.blockingItem(),r.durableRevision(),null,r.payload(),r.lifecycle()));
            else assertThrows(Exception.class,()->copy(r,r.rejection(),r.outcome(),r.denial(),r.blockingItem(),r.durableRevision(),ModernEquipmentAuthority.ZERO,r.payload(),r.lifecycle()));
            assertThrows(Exception.class,()->copy(r,r.rejection(),r.outcome(),r.denial()==Denial.NONE?Denial.UNKNOWN_ITEM:Denial.NONE,r.blockingItem(),r.durableRevision(),r.token(),r.payload(),r.lifecycle()));
        }
    }
    @Test void receiptsRejectForeignOwnerFutureWorldAndMismatchedLifecycle()throws Exception {
        try(var f=new ModernVipEquipmentAuthorityTest.Fixture(directory,false,Map.of(MAX_HP,100L))) {
            var q=Request.inspect(1,f.proof(0));var r=run(f,0,q);var c=f.world.view();
            var foreign=ModernBattleVipWire.candidate(c,ModernBattleVipWireTest.descriptor(c,false),f.player(1).scope());
            assertThrows(Exception.class,()->copy(r,r.rejection(),r.outcome(),r.denial(),0,-1,r.token(),foreign,r.lifecycle()));
            var l=r.lifecycle();var wrong=new ModernBattleLifecycle(r.payload().view().world().revision(),l.phase(),l.abortReason(),l.result(),l.settlement(),l.race(),false,false);
            assertThrows(Exception.class,()->copy(r,r.rejection(),r.outcome(),r.denial(),0,-1,r.token(),r.payload(),wrong));
            var aborted=new ModernBattleLifecycle(l.revision(),ModernWorldProtocol.Phase.ABORTED,ModernWorldProtocol.AbortReason.SERVER_ABORT,null,null,l.race(),false,false);
            assertThrows(Exception.class,()->copy(r,r.rejection(),r.outcome(),r.denial(),0,-1,r.token(),r.payload(),aborted));
            assertThrows(Exception.class,()->copy(r,ModernBattleVipCommandState.Rejection.STALE_RUNTIME_ABILITY,Outcome.REJECTED,Denial.NONE,0,-1,null,r.payload(),r.lifecycle()));
        }
    }
    @Test void readonlyInspectCannotMasqueradeAsMutationAndSetMustReachDesiredFlag()throws Exception {
        try(var f=new ModernVipEquipmentAuthorityTest.Fixture(directory,false,Map.of(MAX_HP,100L))) {
            var q=Request.inspect(1,f.proof(0));var r=run(f,0,q);
            assertThrows(Exception.class,()->copy(r,r.rejection(),Outcome.UNCHANGED,Denial.NONE,0,0,r.token(),r.payload(),r.lifecycle()));
            var set=set(2,quote(r),1,false);
            assertThrows(Exception.class,()->new Reply(set,r.rejection(),Outcome.UNCHANGED,Denial.NONE,0,0,r.token(),r.payload(),r.lifecycle()));
            assertThrows(Exception.class,q::command);
        }
    }
    static Reply copy(Reply r,ModernBattleVipCommandState.Rejection reason,Outcome outcome,Denial denial,long block,long durable,
            ModernEquipmentOverrideStore.Fingerprint token,ModernBattleVipWire.Payload p,ModernBattleLifecycle l) {
        return new Reply(r.echo(),reason,outcome,denial,block,durable,token,p,l);
    }
    static byte[] concat(byte[] a,byte[] b){var out=Arrays.copyOf(a,a.length+b.length);System.arraycopy(b,0,out,a.length,b.length);return out;}
    public static void main(String[] args)throws Exception {
        var root=Files.createTempDirectory("modern-vip-equipment-wire-");var h=ModernVipSelectedProtocolTest.handshake(63);
        var input=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.US_ASCII));var hex=HexFormat.of();int count=0;
        System.out.println("SESSION:"+hex.formatHex(h.hello())+":"+hex.formatHex(h.ack()));
        for(var f:frames(root)) {
            var q=encodeRequest(f.request(),h.peer());var r=ModernApplicationProtocol.encode(new ModernApplicationProtocol.VipEquipmentResult(f.reply()),h.peer());
            System.out.println("REPLY:"+f.label()+":"+hex.formatHex(q)+":"+hex.formatHex(r));System.out.flush();
            var digest=new StringBuilder();int ch;while((ch=input.read())!=-1&&ch!='\n'){if(ch=='\r')continue;if(digest.length()>=64)throw new IOException("oversized digest");digest.append((char)ch);}
            assertEquals(hex.formatHex(MessageDigest.getInstance("SHA-256").digest(r)),digest.toString());count++;
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
}
