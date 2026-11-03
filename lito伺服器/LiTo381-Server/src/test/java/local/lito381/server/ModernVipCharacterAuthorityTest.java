package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernVipCharacterAuthorityTest {
    @TempDir Path directory;
    @Test void oneScopedSourceReadPublishesCompleteCharacterWithoutSourceOrOverrideWrites()throws Exception {
        try(var h=new Harness(directory)) {
            var p=h.read(h.principal,h.row,5641);assertEquals(1,h.reads.get());assertTrue(h.admitted);
            assertEquals(h.source.input().equipment().source().persisted().strength()+11,p.character().effective().strength());
            assertEquals(ModernCombatProfile.ArmorPolicy.REBUILT_CHARACTER_EQUIPMENT_VIP,p.character().armorPolicy());
            assertEquals(0,h.records());assertFalse(h.lease.hasActivePermit());
        }
    }
    @Test void unauthorizedSourceAndWrongObjectAreRejectedBeforeAnyProviderRead()throws Exception {
        try(var h=new Harness(directory)) {
            var wrong=new ModernAccountAuthentication.Principal("other",Set.of(new ModernAccountAuthentication.Source(42,"moon")),Map.of(42,"other"));
            assertThrows(SecurityException.class,()->h.read(wrong,h.row,0));
            assertThrows(SecurityException.class,()->h.read(h.principal,new ModernApplicationProtocol.CharacterRow(h.row.objectId()+1,h.row.name(),0,0,30),0));
            assertEquals(0,h.reads.get());assertEquals(0,h.records());
        }
    }
    @Test void identityChangeAndShapeGuessCannotPublish()throws Exception {
        try(var h=new Harness(directory)) {
            var wrong=new ModernApplicationProtocol.CharacterRow(h.row.objectId(),h.row.name(),h.row.characterClass(),h.row.sex(),h.row.level()+1);
            assertThrows(IOException.class,()->h.read(h.principal,wrong,0));
            assertThrows(IllegalArgumentException.class,()->h.read(h.principal,h.row,999));
            assertFalse(h.admitted);assertEquals(0,h.records());
        }
    }
    @Test void selectionRetirementOrCancellationBeforePublicationKeepsNoResultOrWrite()throws Exception {
        try(var h=new Harness(directory.resolve("retired"))) {
            h.beforeReturn=h.lease::invalidate;assertThrows(IOException.class,()->h.read(h.principal,h.row,5641));assertFalse(h.admitted);assertEquals(0,h.records());
        }
        try(var h=new Harness(directory.resolve("cancelled"))) {
            h.beforeReturn=()->ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);
            assertThrows(ModernBackendExecutor.BackendException.class,()->h.read(h.principal,h.row,5641));assertFalse(h.admitted);assertEquals(0,h.records());
        }
    }
    @Test void deadlineCrossingAtActualPublicationCannotLeakStaleActiveVip()throws Exception {
        try(var h=new Harness(directory)) {
            h.source=ModernVipLifecycleTest.timed(h.source,1000,"2026-10-01 00:00:00",1);
            h.clockStep=1000;assertThrows(IOException.class,()->h.read(h.principal,h.row,5641));
            assertFalse(h.admitted);assertEquals(0,h.records());
        }
    }
    @Test void ownerBoundOverrideFeedsRebuildAndWrongBindingNeverFallsBackToSql()throws Exception {
        try(var h=new Harness(directory.resolve("good"))) {
            h.writeOverride(ModernCombatProfileTest.BINDING);var p=h.read(h.principal,h.row,0);
            assertTrue(p.vip().items().isEmpty());assertEquals(1,p.character().equipmentRevision());assertEquals(1,h.records());
        }
        try(var h=new Harness(directory.resolve("bad"))) {
            byte[] bytes=new byte[32];bytes[0]=1;h.writeOverride(new ModernEquipmentOverrideStore.Fingerprint(bytes));
            assertThrows(IOException.class,()->h.read(h.principal,h.row,0));assertFalse(h.admitted);assertEquals(1,h.records());
        }
    }
    @Test void oldAuthorityWithoutPhysicalProviderCannotInventFullVipSource()throws Exception {
        try(var h=new Harness(directory)) {
            h.authority=new ModernEquipmentAuthority((p,id,obj)->{throw new AssertionError("old provider read");},h.store);
            assertThrows(IOException.class,()->h.read(h.principal,h.row,5641));assertEquals(0,h.reads.get());
        }
    }
    static final class Harness implements AutoCloseable {
        ModernPhysicalCombat.Source source=ModernVipLoadoutTest.source(true,false);
        final ModernApplicationProtocol.CharacterRow row=source.input().equipment().source().character();
        final TournamentPresence.CharacterKey key=new TournamentPresence.CharacterKey(7,row.objectId());
        final ModernAccountAuthentication.Principal principal=new ModernAccountAuthentication.Principal("fixture",Set.of(new ModernAccountAuthentication.Source(7,"sun")),Map.of(7,"owner"));
        final Path directory;final ModernEquipmentOverrideStore store;
        final ModernEquipmentLease lease=new ModernEquipmentLease.Issuer().open(new Object(),key,1,()->{});
        final AtomicInteger reads=new AtomicInteger();final AtomicLong clock=new AtomicLong(1000);long clockStep;
        ModernEquipmentAuthority authority;Runnable beforeReturn=()->{};boolean admitted;
        Harness(Path path)throws Exception {
            directory=path;Files.createDirectories(path);store=new ModernEquipmentOverrideStore(path);
            authority=new ModernEquipmentAuthority((p,id,obj)->{throw new AssertionError("second provider read");},null,(p,id,obj)->{
                assertEquals("owner",p.accountForSource(id));assertEquals(key.sourceId(),id);assertEquals(key.objectId(),obj);
                reads.incrementAndGet();beforeReturn.run();return new ModernEquipmentAuthority.PhysicalLoaded(source,ModernCombatProfileTest.BINDING);
            },store,()->clock.getAndAdd(clockStep));
        }
        ModernVipCharacterProfile read(ModernAccountAuthentication.Principal p,ModernApplicationProtocol.CharacterRow row,long shape)throws Exception {
            return ModernEquipmentSourceJdbcTest.scoped(()->{try(var op=lease.operation(ModernBackendScope.current())) {
                var result=authority.prepareVipCharacter(p,row,op,shape);admitted=op.isCommitAdmitted();return result;
            }});
        }
        void writeOverride(ModernEquipmentOverrideStore.Fingerprint owner)throws Exception {
            ModernEquipmentSourceJdbcTest.scoped(()->{var scope=ModernBackendScope.current();
                try(var writer=new ModernEquipmentLease.Issuer().open(new Object(),key,1,()->{});var op=writer.operation(scope)) {
                    var q=source.input().equipment();var item=q.items().get(1L);
                    store.compareAndSet(key,ModernEquipmentOverrideStore.Expected.absent(),new ModernEquipmentOverrideStore.Replacement(owner,List.of(
                            new ModernEquipmentOverrideStore.ItemOverride(1,item.source().template().catalogId(),ModernEquipmentReconciliation.fingerprint(item,q.lifetimes().items().get(1L)),false))),scope,op);
                }return null;
            });
        }
        long records()throws Exception{try(var files=Files.list(directory)){return files.filter(p->p.toString().endsWith(".meo")).count();}}
        @Override public void close(){lease.close();store.close();assertTrue(store.isQuiescent());}
    }
}
