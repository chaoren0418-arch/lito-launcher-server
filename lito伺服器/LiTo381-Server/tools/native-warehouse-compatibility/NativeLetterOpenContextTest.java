package local.lito381.server;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class NativeLetterOpenContextTest {
 @TempDir Path temporary;
 static RepositoryLetter opened(RepositoryLetter letter){return new RepositoryLetter(letter.itemObjectId(),letter.code(),letter.sender(),letter.receiver(),letter.date(),letter.templateId(),letter.subject(),letter.content(),true);}
 interface Open {RepositoryLetterMutation apply(Object[] args)throws Exception;}
 static void source(NativePlayerTradeBoundaryTest.Pair pair,Open open,AtomicReference<RepositoryLetter> current,AtomicInteger reads)throws Exception{
  AccountRepository repository=NativeLetterSqlFaultTest.proxy(AccountRepository.class,(p,m,a)->{
   if(m.getName().equals("repositoryForClassicSource"))return p;if(m.getName().equals("openLetter"))return open.apply(a);
   if(m.getName().equals("letters")){reads.incrementAndGet();return new RepositoryLetterSnapshot(true,current.get()==null?List.of():List.of(current.get()));}
   return m.invoke(pair.f.accounts,a);
  });NativeWarehouseCompatibilityTest.set(pair.f.session,"accounts",repository);
 }
 void late(boolean baseline)throws Exception{
  try(var pair=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=pair.f;var original=NativeLetterDeleteContextTest.letter(f.session,"SecretOld");var inventory=NativeLetterDeleteContextTest.install(f.session,original);AtomicReference<RepositoryLetter> current=new AtomicReference<>(original);AtomicBoolean committed=new AtomicBoolean();
   source(pair,a->{assertEquals(original,a[1]);committed.set(true);current.set(opened(original));assertTrue(f.hub.selectCharacter(f.session,2,"other-source",f.accounts.characters("test").get(0)));NativeWarehouseCompatibilityTest.set(f.session,"selectedClassicServerName","other-source");NativeLetterDeleteContextTest.install(f.session,NativeLetterDeleteContextTest.letter(f.session,"ForeignSameID"));return new RepositoryLetterMutation(RepositoryLetterMutationStatus.SUCCESS,current.get());},current,new AtomicInteger());
   f.call("useSqlPhysicalLetter",inventory.item(NativeLetterDeleteContextTest.ID));assertTrue(committed.get());var snapshot=(RepositoryLetterSnapshot)NativeWarehouseCompatibilityTest.get(f.session,"sqlLetterSnapshot");
   if(baseline){assertEquals(InventoryCatalog.LETTER_OPEN,f.inv().item(NativeLetterDeleteContextTest.ID).definition().catalogId());assertEquals("SecretOld",snapshot.letters().get(0).subject());var packets=f.drain();assertEquals(1,packets.size());assertArrayEquals(Protocol.readMail(original.itemObjectId(),InventoryCatalog.LETTER_OPEN_INVENTORY_GFX,original.code(),original.sender(),original.receiver(),original.subject(),original.content(),original.templateId(),original.date()),packets.get(0));}
   else{assertEquals(InventoryCatalog.LETTER_CLOSED,f.inv().item(NativeLetterDeleteContextTest.ID).definition().catalogId());assertEquals("ForeignSameID",snapshot.letters().get(0).subject());assertTrue(f.drain().isEmpty());}
  }
 }
 @Test void baselineLateOpenSuccessOverwritesForeignCacheAndPublishesOldContent()throws Exception{late(true);}
 @Test void lateOpenSuccessCannotOverwriteForeignCacheOrPublishOldContent()throws Exception{late(false);}
 @Test void normalOpenAndRepeatReadUseOwnedAuthoritativeSourceWithoutReopening()throws Exception{
  try(var pair=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=pair.f;var original=NativeLetterDeleteContextTest.letter(f.session,"Original");var inventory=NativeLetterDeleteContextTest.install(f.session,original);AtomicReference<RepositoryLetter> current=new AtomicReference<>(original);AtomicInteger opens=new AtomicInteger(),reads=new AtomicInteger();
   source(pair,a->{opens.incrementAndGet();current.set(opened(original));return new RepositoryLetterMutation(RepositoryLetterMutationStatus.SUCCESS,current.get());},current,reads);
   f.call("useSqlPhysicalLetter",inventory.item(NativeLetterDeleteContextTest.ID));assertTrue(((RepositoryLetterSnapshot)NativeWarehouseCompatibilityTest.get(f.session,"sqlLetterSnapshot")).letters().get(0).opened());
   f.call("useSqlPhysicalLetter",f.inv().item(NativeLetterDeleteContextTest.ID));assertEquals(1,opens.get());assertEquals(1,reads.get());var packets=f.drain();assertEquals(2,packets.size());assertArrayEquals(packets.get(0),packets.get(1));
   current.set(null);f.call("useSqlPhysicalLetter",f.inv().item(NativeLetterDeleteContextTest.ID));assertTrue(f.drain().isEmpty());assertEquals(2,reads.get());
  }
 }
 @Test void readOnlyRefusalAndDisconnectedSelectionCannotOpenOrRead()throws Exception{
  try(var pair=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=pair.f;var original=NativeLetterDeleteContextTest.letter(f.session,"Original");var inventory=NativeLetterDeleteContextTest.install(f.session,original);AtomicInteger opens=new AtomicInteger(),reads=new AtomicInteger();
   source(pair,a->{opens.incrementAndGet();return RepositoryLetterMutation.readOnly(original);},new AtomicReference<>(original),reads);
   f.call("useSqlPhysicalLetter",inventory.item(NativeLetterDeleteContextTest.ID));assertEquals(1,opens.get());assertTrue(f.drain().isEmpty());assertEquals(InventoryCatalog.LETTER_CLOSED,f.inv().item(NativeLetterDeleteContextTest.ID).definition().catalogId());
   f.hub.disconnect(f.session);f.call("useSqlPhysicalLetter",inventory.item(NativeLetterDeleteContextTest.ID));assertEquals(1,opens.get());assertEquals(0,reads.get());assertTrue(f.drain().isEmpty());
  }
 }
}
