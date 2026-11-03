package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernApplicationProtocol.*;
import static local.lito381.server.ModernBattleEquipmentProtocol.*;
import static local.lito381.server.ModernEquipmentAuthority.READ;
import static local.lito381.server.ModernEquipmentAuthority.SET;
import static local.lito381.server.ModernEquipmentAuthority.ZERO;
import static local.lito381.server.ModernEquipmentAuthority.CONFLICT;
import local.lito381.server.ModernEquipmentAuthority.Snapshot;
import static local.lito381.server.ModernCombatProfileTest.binding;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernBattleEquipmentProtocolTest {
    @TempDir Path root;
    @FunctionalInterface interface Work<T>{T run(ModernEquipmentLease.Operation operation)throws Exception;}
    static <T>T call(ModernBattleLifecycleServiceTest.Fixture f,Work<T> work)throws Exception {
        var scope=ModernEquipmentOverrideStoreTest.scope();var p=f.player(0);
        var lease=new ModernEquipmentLease.Issuer().open(p.owner(),binding(f.source.input().equipment()).key(),p.scope().epoch(),()->{});
        try(var op=lease.operation(scope)) {
            ModernBackendScope.bind(scope);try{return work.run(op);}finally{ModernBackendScope.unbind();}
        }finally{lease.invalidate();scope.finish();}
    }
    static Snapshot read(ModernBattleLifecycleServiceTest.Fixture f)throws Exception {
        var p=f.player(0);var s=p.scope();
        return call(f,op->f.gear.execute(f.principal,p.row(),op,new ModernEquipmentAuthority.Request(s.sourceId(),s.objectId(),s.epoch(),READ,false,0,0,ZERO)).snapshot());
    }
    static boolean emptyStore(ModernBattleLifecycleServiceTest.Fixture f)throws Exception {
        return call(f,op->f.store.read(op.key(),op.scope()).isEmpty());
    }
    static BattleEquipmentRequest request(ModernBattleLifecycleServiceTest.Fixture f,Snapshot gear,boolean desired) {
        var p=ModernBattleCommandLifeTest.proof(f.engine,0);var s=p.player();
        return new BattleEquipmentRequest(9,new Request(p,new ModernEquipmentAuthority.Request(
                s.sourceId(),s.objectId(),s.epoch(),SET,desired,1,gear.revision(),gear.viewToken())));
    }
    static BattleEquipmentResult execute(ModernBattleLifecycleServiceTest.Fixture f,BattleEquipmentRequest q)throws Exception {
        var result=call(f,op->f.service.equipment(f.player(0).owner(),q,f.gear,f.principal,f.player(0).row(),op));
        encoded(result);return result;
    }
    static byte[] encoded(BattleEquipmentResult r)throws Exception {
        ModernNpcMotionWireTest.assertPayload43(ModernApplicationProtocol.encode(r,43),r.reply().view());
        var bytes=ModernApplicationProtocol.encode(r,41);var b=ByteBuffer.wrap(bytes);
        assertEquals(71,b.getShort(12));assertEquals(bytes.length,b.getInt(4));
        int equipment=b.getInt(196),data=b.getInt(200);
        assertEquals(bytes.length,204+equipment+data);
        assertArrayEquals(ModernBattleWire.encode(r.reply().view(),2),Arrays.copyOfRange(bytes,204+equipment,bytes.length));
        if(r.reply().equipment()!=null)assertArrayEquals(ModernApplicationProtocol.encode(new EquipmentResult(r.requestId(),r.echo().equipment(),r.reply().equipment()),41),Arrays.copyOfRange(bytes,204,204+equipment));
        assertThrows(ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,41));
        assertThrows(ProtocolException.class,()->ModernApplicationProtocol.encode(r,40));
        assertSame(r,ModernMatchDescriptor.forPeer(r,new ModernSessionBoundary.Negotiated(1,41,1,ModernSessionProtocol.KNOWN_CAPABILITIES)));
        assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(r,new ModernSessionBoundary.Negotiated(1,40,1,ModernSessionProtocol.KNOWN_CAPABILITIES)));
        return bytes;
    }
    static void golden(String name,BattleEquipmentResult r)throws Exception {
        if(!Boolean.getBoolean("modern.equipment.goldens"))return;
        var root=Path.of("target","modern-equipment-test-fixtures");java.nio.file.Files.createDirectories(root);
        java.nio.file.Files.write(root.resolve(name+".request"),ModernApplicationProtocol.encode(new BattleEquipmentRequest(r.requestId(),r.echo()),41));
        java.nio.file.Files.write(root.resolve(name+".reply"),encoded(r));
    }
    static void beforeGolden(String name,ModernBattleLifecycleServiceTest.Fixture f)throws Exception {
        if(!Boolean.getBoolean("modern.equipment.goldens"))return;
        var p=ModernBattleCommandLifeTest.proof(f.engine,0);var q=ModernBattleWorldProtocolTest.read(p.player(),p.world());
        var root=Path.of("target","modern-equipment-test-fixtures");java.nio.file.Files.createDirectories(root);
        java.nio.file.Files.write(root.resolve(name+".request"),ModernApplicationProtocol.encode(new BattleWorldRequest(8,q),41));
        java.nio.file.Files.write(root.resolve(name+".reply"),ModernApplicationProtocol.encode(new BattleWorldResult(8,q,f.service.battleWorld(f.player(0).owner(),q)),41));
    }
    @Test void exactRequestBindingsVersionBoundsAndAllocationLimit()throws Exception {
        var p=new ModernBattleCommandLife.Proof(new ModernRooms.Scope(7,3333,Long.MIN_VALUE,3),new ModernTargetHistory.World(11,12,13),1,2,7);
        var q=new BattleEquipmentRequest(9,new Request(p,new ModernEquipmentAuthority.Request(7,3333,Long.MIN_VALUE,SET,false,123,4,ZERO)));
        var b=ModernApplicationProtocol.encode(q,41);assertEquals(160,b.length);assertEquals(q,ModernApplicationProtocol.decode(b,41));
        assertEquals(Long.MIN_VALUE,ByteBuffer.wrap(b).getLong(28));assertEquals(4,ByteBuffer.wrap(b).getLong(120));
        assertThrows(ProtocolException.class,()->ModernApplicationProtocol.encode(q,40));
        assertThrows(ProtocolException.class,()->ModernApplicationProtocol.decode(b,40));
        for(int n=0;n<b.length;n++){var cut=Arrays.copyOf(b,n);assertThrows(ProtocolException.class,()->ModernApplicationProtocol.decode(cut,41));}
        for(int at:new int[]{14,36,40,48,56,64,72,80,88,92,100,108,109,110,112,120}) {
            var bad=b.clone();bad[at]=(byte)255;assertThrows(ProtocolException.class,()->ModernApplicationProtocol.decode(bad,41),"offset "+at);
        }
        for(int[] spec:new int[][]{{70,159},{70,161},{71,204},{70,Integer.MAX_VALUE}}) {
            var h=ByteBuffer.allocate(16).putInt(0x4c544d31).putInt(spec[1]).putInt(9).putShort((short)spec[0]).putShort((short)0).array();
            var in=new java.io.InputStream(){int at;public int read(){if(at==16)fail("invalid request read its body");return h[at++]&255;}};
            assertThrows(ProtocolException.class,()->ModernApplicationProtocol.readFrame(in,41));
        }
        assertThrows(IllegalArgumentException.class,()->new Request(p,new ModernEquipmentAuthority.Request(42,3333,Long.MIN_VALUE,SET,false,1,0,ZERO)));
        assertThrows(IllegalArgumentException.class,()->new Request(p,new ModernEquipmentAuthority.Request(7,3333,Long.MIN_VALUE,READ,false,0,0,ZERO)));
    }
    @Test void durableSetNoopConflictAndReequipPublishExactOwnerAndKeepRetainedReceipt()throws Exception {
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null,41)) {
            var before=read(f);var old=f.read(0).view();var q=request(f,before,false);
            beforeGolden("before",f);
            var changed=execute(f,q);var r=changed.reply();
            golden("applied",changed);
            assertEquals(Reason.NONE,r.reason());assertEquals(Disposition.APPLIED,r.disposition());assertEquals(1,r.durableRevision());
            assertEquals(1,r.view().personal().equipment().revision());assertEquals(0,old.personal().equipment().revision());
            assertEquals(0,f.read(1).view().personal().equipment().revision());
            var same=execute(f,request(f,r.equipment().snapshot(),false));
            golden("unchanged",same);
            assertEquals(Disposition.UNCHANGED,same.reply().disposition());assertEquals(1,same.reply().durableRevision());
            var conflict=execute(f,q);assertEquals(CONFLICT,conflict.reply().equipment().status());
            golden("conflict",conflict);
            assertEquals(Disposition.NOT_COMMITTED,conflict.reply().disposition());assertEquals(-1,conflict.reply().durableRevision());
            var up=execute(f,request(f,conflict.reply().equipment().snapshot(),true));
            golden("reequipped",up);
            assertEquals(2,up.reply().durableRevision());assertEquals(1,r.view().personal().equipment().revision());
            long persisted=call(f,op->f.store.read(op.key(),op.scope()).orElseThrow().revision());
            assertEquals(2,persisted);
            f.move(4);assertEquals(2,f.read(0).view().personal().equipment().revision());
            assertEquals(0,f.read(1).view().personal().equipment().revision());
        }
    }
    @Test void staleLifeBusyWrongOwnerAndOldPeerCannotReachDurableSet(@TempDir Path oldRoot)throws Exception {
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null,41)) {
            var gear=read(f);var q=request(f,gear,false);var p=q.request().proof();
            var stale=new BattleEquipmentRequest(10,new Request(new ModernBattleCommandLife.Proof(p.player(),p.world(),p.entity(),p.life()+1,p.revision()),q.request().equipment()));
            var staleResult=execute(f,stale);assertEquals(Reason.STALE_LIFE,staleResult.reply().reason());golden("stale",staleResult);
            try(var held=f.engine.beginCanonicalEquipmentChange(f.player(0).owner(),p)) {
                var busy=execute(f,q);assertEquals(Reason.BUSY,busy.reply().reason());golden("busy",busy);
            }
            assertThrows(SecurityException.class,()->call(f,op->f.service.equipment(f.player(1).owner(),q,f.gear,f.principal,f.player(1).row(),op)));
            assertTrue(emptyStore(f));
            assertEquals(Disposition.APPLIED,execute(f,q).reply().disposition());
        }
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(oldRoot,null,40)) {
            var q=request(f,read(f),false);
            assertThrows(SecurityException.class,()->execute(f,q));
            assertTrue(emptyStore(f));
        }
    }
    @Test void terminalBeforeAdmissionCannotCommitAndReceiptCannotClaimApplied()throws Exception {
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null,41)) {
            var q=request(f,read(f),false);f.leave(1);var result=execute(f,q);var r=result.reply();
            golden("terminal",result);
            assertEquals(Reason.NOT_ACTIVE,r.reason());assertEquals(Disposition.NOT_COMMITTED,r.disposition());
            assertTrue(emptyStore(f));
            assertThrows(IllegalArgumentException.class,()->encoded(new BattleEquipmentResult(9,q.request(),new Reply(r.reason(),Disposition.APPLIED,1,null,r.view(),r.lifecycle()))));
        }
    }
    static void abortOnOtherThread(ModernBattleLifecycleServiceTest.Fixture f)throws java.io.IOException {
        var executor=java.util.concurrent.Executors.newSingleThreadExecutor();
        try{executor.submit(f.engine::abortObservation).get(3,java.util.concurrent.TimeUnit.SECONDS);}
        catch(Exception error){throw new java.io.IOException("equipment I/O retained engine monitor",error);}
        finally{
            executor.shutdownNow();
            try{assertTrue(executor.awaitTermination(3,java.util.concurrent.TimeUnit.SECONDS));}
            catch(InterruptedException error){Thread.currentThread().interrupt();throw new java.io.IOException(error);}
        }
    }
    @Test void abortDuringForceBeforeCommitPublishesNoDurableSuccess()throws Exception {
        var ref=new java.util.concurrent.atomic.AtomicReference<ModernBattleLifecycleServiceTest.Fixture>();
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null,41,new ModernEquipmentOverrideStore.FileOps(){
            @Override public void force(java.nio.channels.FileChannel file)throws java.io.IOException {
                ModernEquipmentOverrideStore.FileOps.super.force(file);abortOnOtherThread(ref.get());
            }
        })) {
            ref.set(f);var q=request(f,read(f),false);
            assertEquals(ModernEquipmentOverrideStore.Failure.INVALID_OPERATION,
                    assertThrows(ModernEquipmentOverrideStore.StoreException.class,()->execute(f,q)).failure());
            assertTrue(emptyStore(f));assertEquals(0,f.read(0).view().personal().equipment().revision());
        }
    }
    @Test void abortAfterRenameKeepsExplicitDurableReceiptWithoutApplyingOrClaimingRollback()throws Exception {
        var ref=new java.util.concurrent.atomic.AtomicReference<ModernBattleLifecycleServiceTest.Fixture>();
        try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null,41,new ModernEquipmentOverrideStore.FileOps(){
            @Override public void move(Path from,Path to)throws java.io.IOException {
                ModernEquipmentOverrideStore.FileOps.super.move(from,to);abortOnOtherThread(ref.get());
            }
        })) {
            ref.set(f);var q=request(f,read(f),false);beforeGolden("retired-before",f);var result=execute(f,q);var r=result.reply();golden("retired",result);
            assertEquals(Disposition.PERSISTED_RETIRED,r.disposition());assertEquals(1,r.durableRevision());
            assertEquals(0,r.view().personal().equipment().revision());assertNull(r.equipment().snapshot());
            assertEquals(ModernWorldProtocol.Phase.ABORTED,r.lifecycle().phase());
            long persisted=call(f,op->f.store.read(op.key(),op.scope()).orElseThrow().revision());assertEquals(1,persisted);
            // The real service/store prove the disposition. The codec checks
            // its internal consistency; it cannot independently read storage.
            var bytes=encoded(result);assertEquals(Disposition.PERSISTED_RETIRED.ordinal(),bytes[161]);
            assertEquals(1,ByteBuffer.wrap(bytes).getLong(164));
            assertThrows(IllegalArgumentException.class,()->encoded(new BattleEquipmentResult(9,q.request(),new Reply(
                    r.reason(),Disposition.NOT_COMMITTED,1,r.equipment(),r.view(),r.lifecycle()))));
            assertThrows(IllegalArgumentException.class,()->encoded(new BattleEquipmentResult(9,q.request(),new Reply(
                    r.reason(),Disposition.PERSISTED_RETIRED,-1,r.equipment(),r.view(),r.lifecycle()))));
        }
    }
    static void rejectedLifeGolden(String name,ModernBattleEngine engine,ModernBattleEngineBotTest.Player player,
            ModernBattleCommandLife.Proof proof,Reason expected)throws Exception {
        var admission=engine.admitCanonicalEquipmentChange(player.owner(),proof);
        assertEquals(expected,admission.reason());assertNull(admission.change());
        var publication=ModernBattleWireTest.publication(admission.observation(),null);var view=publication.audience(player.scope());
        assertEquals(2,ModernBattleEquipmentProtocol.life(view));
        var s=proof.player();var q=new Request(proof,new ModernEquipmentAuthority.Request(s.sourceId(),s.objectId(),s.epoch(),SET,false,1,0,ZERO));
        var r=new BattleEquipmentResult(9,q,new Reply(expected,Disposition.NOT_COMMITTED,-1,null,view,ModernBattleLifecycle.from(publication)));
        encoded(r);golden(name,r);
    }
    @Test void honorRespawnUsesSharedIncarnationForStaleAndSecondDeathReplies()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(100,1,100,1_000_000_000L)) {
            f.active();var old=ModernBattleCommandLifeTest.proof(f.engine,1);
            assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,4,2).reason());f.elapsed(5_000_000_000L);f.engine.tick();
            var fresh=ModernBattleCommandLifeTest.proof(f.engine,1);assertEquals(2,fresh.life());
            rejectedLifeGolden("honor-stale",f.engine,f.players.get(1),old,Reason.STALE_LIFE);
            try(var reservation=f.engine.beginCanonicalEquipmentChange(f.players.get(1).owner(),fresh)){assertNotNull(reservation);}
            assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,2,4,2).reason());
            rejectedLifeGolden("honor-dead",f.engine,f.players.get(1),fresh,Reason.DEAD);
        }
    }
    @Test void siegeRespawnUsesSharedIncarnationAndFreshEquipmentAdmission()throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture(0,1,120_000,1234,0,false)) {
            f.active();var old=ModernBattleCommandLifeTest.proof(f.engine,1);
            f.walk(1,ModernBattleEngineSiegeTest.cell(31,11));f.waitMillis(200);
            assertEquals(ModernSiege.PortalReason.MOVED,f.portal(1).reason());
            f.walk(1,ModernBattleEngineSiegeTest.cell(11,10));f.waitMillis(200);
            assertEquals(0,f.attack(0,1,2).snapshot().vitals().get(1).hp());f.waitMillis(1234);f.tick();
            var fresh=ModernBattleCommandLifeTest.proof(f.engine,1);assertEquals(2,fresh.life());
            rejectedLifeGolden("siege-stale",f.engine,f.players.get(1),old,Reason.STALE_LIFE);
            try(var reservation=f.engine.beginCanonicalEquipmentChange(f.players.get(1).owner(),fresh)){assertNotNull(reservation);}
        }
    }
}
