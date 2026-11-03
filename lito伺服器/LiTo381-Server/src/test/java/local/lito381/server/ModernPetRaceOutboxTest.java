package local.lito381.server;

import static local.lito381.server.ModernPetRacePersistenceTestFixture.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernPetRaceOutboxTest {
    @TempDir Path directory;
    static long deadline(){return System.nanoTime()+TimeUnit.SECONDS.toNanos(3);}
    @Test void completeEnvelopeRoundtripsAllPinsGatesSourcesAndAudit()throws Exception {
        var event=event();byte[] bytes=ModernPetRaceOutbox.encodeEvent(event);
        assertEquals(event,ModernPetRaceOutbox.decodeEvent(bytes));
        assertTrue(bytes.length<ModernPetRaceOutbox.MAX_EVENT_BYTES);
        assertEquals(List.of(7,42),event.rows().stream().map(r->r.source().sourceId()).toList());
        assertEquals(3333,event.rows().get(0).source().objectId());assertEquals(3333,event.rows().get(1).source().objectId());
        for(int end=0;end<bytes.length;end++) {
            byte[] shortBytes=Arrays.copyOf(bytes,end);
            assertThrows(ModernPetRaceOutbox.OutboxException.class,()->ModernPetRaceOutbox.decodeEvent(shortBytes),"prefix "+end);
        }
        byte[] tail=Arrays.copyOf(bytes,bytes.length+1);
        assertThrows(ModernPetRaceOutbox.OutboxException.class,()->ModernPetRaceOutbox.decodeEvent(tail));
        byte[] bad=bytes.clone();bad[3]=2;
        assertThrows(ModernPetRaceOutbox.OutboxException.class,()->ModernPetRaceOutbox.decodeEvent(bad));
        for(int offset:new int[]{20,44,52,88,96,136,160,168,200,240,300}) {
            if(offset>=bytes.length-8)continue;byte[] changed=bytes.clone();changed[offset]^=0x7f;
            assertThrows(ModernPetRaceOutbox.OutboxException.class,()->ModernPetRaceOutbox.decodeEvent(changed),"offset "+offset);
        }
    }
    @Test void journalReplayKeepsFirstAuditAndHasIndependentMagicAndFilename()throws Exception {
        var first=event();byte[] original;
        try(var box=new ModernPetRaceOutbox(directory,new byte[32])) {
            box.record(first,deadline());original=Files.readAllBytes(directory.resolve("race-outbox.rpo"));
            assertArrayEquals(new byte[]{0x52,0x50,0x4f,0x31},Arrays.copyOf(original,4));
            box.record(recorded(first,999999999),deadline());
            assertArrayEquals(original,Files.readAllBytes(directory.resolve("race-outbox.rpo")));
            assertEquals(List.of(first),box.pending(deadline()));
            assertFalse(Files.exists(directory.resolve("outbox.mso")));
        }
        try(var reopened=new ModernPetRaceOutbox(directory,new byte[32])) {
            assertEquals(List.of(first),reopened.pending(deadline()));
            reopened.acknowledge(recorded(first,999999999),deadline());assertEquals(0,reopened.size());
        }
    }
    @Test void routeMismatchCorruptionAndConcurrentWriterNeverFallBackToEmpty()throws Exception {
        try(var box=new ModernPetRaceOutbox(directory,new byte[32])) {
            box.record(event(),deadline());
            assertEquals(ModernPetRaceOutbox.Failure.BUSY,assertThrows(ModernPetRaceOutbox.OutboxException.class,
                    ()->new ModernPetRaceOutbox(directory,new byte[32])).failure());
        }
        byte[] binding=new byte[32];binding[0]=1;
        assertEquals(ModernPetRaceOutbox.Failure.BINDING_MISMATCH,assertThrows(ModernPetRaceOutbox.OutboxException.class,
                ()->new ModernPetRaceOutbox(directory,binding)).failure());
        Path path=directory.resolve("race-outbox.rpo");byte[] bytes=Files.readAllBytes(path);bytes[0]^=1;Files.write(path,bytes);
        assertEquals(ModernPetRaceOutbox.Failure.CORRUPT,assertThrows(ModernPetRaceOutbox.OutboxException.class,
                ()->new ModernPetRaceOutbox(directory,new byte[32])).failure());
        assertArrayEquals(bytes,Files.readAllBytes(path));
    }
    @Test void sameUuidDifferentAuthorityCannotReplaceOrAcknowledgePending()throws Exception {
        try(var box=new ModernPetRaceOutbox(directory,new byte[32])) {
            var first=event();box.record(first,deadline());var other=event(ID,8,first.recordedAtMillis());
            assertEquals(ModernPetRaceOutbox.Failure.CONFLICT,assertThrows(ModernPetRaceOutbox.OutboxException.class,()->box.record(other,deadline())).failure());
            assertEquals(ModernPetRaceOutbox.Failure.CONFLICT,assertThrows(ModernPetRaceOutbox.OutboxException.class,()->box.acknowledge(other,deadline())).failure());
            assertEquals(List.of(first),box.pending(deadline()));
        }
    }
    @Test void forceFailureBeforeRenamePreservesPreviousSnapshotForReplay()throws Exception {
        var forces=new AtomicInteger();
        try(var box=new ModernPetRaceOutbox(directory,new byte[32],new ModernPetRaceOutbox.FileOps(){
            @Override public void force(FileChannel f)throws IOException {
                if(forces.incrementAndGet()==2)throw new IOException("synthetic force failure");
                f.force(true);
            }
        })) {
            assertThrows(ModernPetRaceOutbox.OutboxException.class,()->box.record(event(),deadline()));
            assertEquals(List.of(),box.pending(deadline()));
        }
        try(var box=new ModernPetRaceOutbox(directory,new byte[32])){assertEquals(List.of(),box.pending(deadline()));}
    }
    @Test void uncertainAtomicMovePoisonsOwnerButReopenRecoversExactAuthority()throws Exception {
        var moves=new AtomicInteger();
        try(var box=new ModernPetRaceOutbox(directory,new byte[32],new ModernPetRaceOutbox.FileOps(){
            @Override public void move(Path from,Path to)throws IOException {
                ModernPetRaceOutbox.FileOps.super.move(from,to);
                if(moves.incrementAndGet()==2)throw new IOException("synthetic lost rename result");
            }
        })) {
            assertEquals(ModernPetRaceOutbox.Failure.COMMIT_UNCERTAIN,assertThrows(ModernPetRaceOutbox.OutboxException.class,
                    ()->box.record(event(),deadline())).failure());
            assertFalse(box.isHealthy());
            assertThrows(ModernPetRaceOutbox.OutboxException.class,()->box.pending(deadline()));
        }
        try(var box=new ModernPetRaceOutbox(directory,new byte[32])){assertEquals(List.of(event()),box.pending(deadline()));}
    }
    @Test void deadlinesAndCloseCannotWriteOrFalselyReportAnEmptyQueue()throws Exception {
        var box=new ModernPetRaceOutbox(directory,new byte[32]);box.record(event(),deadline());
        assertEquals(ModernPetRaceOutbox.Failure.DEADLINE,assertThrows(ModernPetRaceOutbox.OutboxException.class,
                ()->box.pending(System.nanoTime()-1)).failure());
        box.close();assertTrue(box.isQuiescent());assertFalse(box.isHealthy());
        assertThrows(ModernPetRaceOutbox.OutboxException.class,()->box.pending(deadline()));
        try(var reopened=new ModernPetRaceOutbox(directory,new byte[32])){assertEquals(List.of(event()),reopened.pending(deadline()));}
    }
}

