package local.lito381.server;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class NativeLetterDeleteContextTest {
 @TempDir Path temporary;
 static final long ID=30000;
 static RepositoryLetter letter(ClientSession s,String subject){return new RepositoryLetter(ID,1,"Sender",s.playerName(),"26/10/04",0,subject,"Body",false);}
 static RuntimeInventory install(ClientSession s,RepositoryLetter letter)throws Exception{
  RuntimeInventory inventory=new RuntimeInventory();InventoryCatalog.Definition def=InventoryCatalog.require(InventoryCatalog.LETTER_CLOSED);
  assertEquals(ID,inventory.addDistinctState(def,new RuntimeInventory.PersistentItem(ID,1,0,0,true,def.blessing(),true,false,false,null,def.initialChargeCount(),0L,false,true)));
  NativeWarehouseCompatibilityTest.set(s,"inventory",inventory);NativeLetterAdmissionTest.ready(s);NativeWarehouseCompatibilityTest.set(s,"sqlLetterSnapshot",new RepositoryLetterSnapshot(true,List.of(letter)));
  ((Map<Long,Long>)NativeWarehouseCompatibilityTest.get(s,"letterObjectBindings")).put(ID,ID);return inventory;
 }
 interface Delete {RepositoryLetterMutation apply(Object[] a)throws Exception;}
 static void source(NativePlayerTradeBoundaryTest.Pair p,Delete deletion)throws Exception{
  AccountRepository repository=NativeLetterSqlFaultTest.proxy(AccountRepository.class,(proxy,m,a)->{
   if(m.getName().equals("repositoryForClassicSource"))return proxy;if(m.getName().equals("deleteLetter"))return deletion.apply(a);return m.invoke(p.f.accounts,a);
  });NativeWarehouseCompatibilityTest.set(p.f.session,"accounts",repository);
 }
 void late(boolean baseline)throws Exception{
  try(var p=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=p.f;RepositoryLetter expected=letter(f.session,"original-source");RuntimeInventory old=install(f.session,expected);AtomicBoolean committed=new AtomicBoolean();
   source(p,a->{assertEquals(f.session.playerObjectId(),a[0]);assertEquals(expected,a[1]);committed.set(true);assertTrue(f.hub.selectCharacter(f.session,2,"other-source",f.accounts.characters("test").get(0)));NativeWarehouseCompatibilityTest.set(f.session,"selectedClassicServerName","other-source");install(f.session,letter(f.session,"ForeignSameID"));return new RepositoryLetterMutation(RepositoryLetterMutationStatus.SUCCESS,expected);});
   f.call("destroySqlPhysicalLetter",old.item(ID));assertTrue(committed.get());
   var snapshot=(RepositoryLetterSnapshot)NativeWarehouseCompatibilityTest.get(f.session,"sqlLetterSnapshot");
   if(baseline){assertNull(f.inv().item(ID));assertTrue(snapshot.letters().isEmpty());assertFalse(f.drain().isEmpty());}
   else{assertNotNull(f.inv().item(ID));assertEquals("ForeignSameID",snapshot.letters().get(0).subject());assertTrue(f.drain().isEmpty());}
  }
 }
 @Test void baselineLateDeleteSuccessRemovesNewSourceSameIdRuntimeAndCache()throws Exception{late(true);}
 @Test void lateDeleteSuccessCannotRemoveNewSourceSameIdRuntimeOrCache()throws Exception{late(false);}
 @Test void actualSelectedOwnerRequiredAndSqlRefusalKeepsRuntime()throws Exception{
  try(var p=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=p.f;RepositoryLetter expected=letter(f.session,"Original");RuntimeInventory inventory=install(f.session,expected);int[] calls={0};
   source(p,a->{calls[0]++;return new RepositoryLetterMutation(RepositoryLetterMutationStatus.CONFLICT,expected);});f.call("destroySqlPhysicalLetter",inventory.item(ID));assertEquals(1,calls[0]);assertNotNull(f.inv().item(ID));assertTrue(f.drain().isEmpty());
   f.hub.disconnect(f.session);f.call("destroySqlPhysicalLetter",inventory.item(ID));assertEquals(1,calls[0]);assertNotNull(f.inv().item(ID));assertTrue(f.drain().isEmpty());
  }
 }
 @Test void currentSqlSuccessPublishesOnlyExactPhysicalRemoval()throws Exception{
  try(var p=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=p.f;RepositoryLetter expected=letter(f.session,"Original");RuntimeInventory inventory=install(f.session,expected);
   source(p,a->new RepositoryLetterMutation(RepositoryLetterMutationStatus.SUCCESS,expected));f.call("destroySqlPhysicalLetter",inventory.item(ID));assertNull(f.inv().item(ID));assertTrue(((RepositoryLetterSnapshot)NativeWarehouseCompatibilityTest.get(f.session,"sqlLetterSnapshot")).letters().isEmpty());var packets=f.drain();assertEquals(1,packets.size());assertArrayEquals(Protocol.removeInventory(ID),packets.get(0));
  }
 }
}
