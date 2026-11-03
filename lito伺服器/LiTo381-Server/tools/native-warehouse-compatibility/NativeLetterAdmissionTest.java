package local.lito381.server;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class NativeLetterAdmissionTest {
 @TempDir Path temporary;
 static final AtomicLong ids=new AtomicLong(0x7B000000L);
 static RuntimeInventory inv(ClientSession s)throws Exception{return (RuntimeInventory)NativeWarehouseCompatibilityTest.get(s,"inventory");}
 static void set(ClientSession s,String name,Object value)throws Exception{NativeWarehouseCompatibilityTest.set(s,name,value);}
 static void ready(ClientSession s)throws Exception{
  set(s,"inventoryCharacter",s.sourceCharacterIdentity());set(s,"sqlLetterSnapshot",new RepositoryLetterSnapshot(true,List.of()));
 }
 static RuntimeInventory inventory(int papers,int letters){
  Map<Long,Integer> counts=new LinkedHashMap<>();if(papers>0)counts.put(InventoryCatalog.LETTER_PAPER,papers);if(letters>0)counts.put(InventoryCatalog.LETTER_CLOSED,letters);
  return RuntimeInventory.fromCatalogCounts(counts,ids::incrementAndGet);
 }
 static RuntimeInventory.Item paper(ClientSession s)throws Exception{return inv(s).items().stream().filter(i->i.definition().catalogId()==InventoryCatalog.LETTER_PAPER).findFirst().orElseThrow();}
 static void send(ClientSession sender,ClientSession recipient)throws Exception{
  RuntimeInventory.Item paper=paper(sender);Method m=ClientSession.class.getDeclaredMethod("useSqlLetterPaper",RuntimeInventory.Item.class,Protocol.LetterComposeInventoryUse.class,ClientSession.class);m.setAccessible(true);
  try{m.invoke(sender,paper,new Protocol.LetterComposeInventoryUse(paper.objectId(),1,recipient.playerName(),"Title","Body"),recipient);}
  catch(InvocationTargetException e){if(e.getCause() instanceof Exception x)throw x;throw e;}
 }
 static void reconcile(ClientSession s)throws Exception{Method m=ClientSession.class.getDeclaredMethod("reconcileNativeLetters");m.setAccessible(true);m.invoke(s);}
 interface Delivery {RepositoryLetterDelivery deliver(Object[] args)throws Exception;}
 static final class Source {
  final Map<Long,RepositoryLetter> rows=new ConcurrentHashMap<>();final AtomicInteger calls=new AtomicInteger();
  Delivery during;
  AccountRepository repository(AccountRepository original,ClientSession sender,ClientSession receiver){
   return NativeLetterSqlFaultTest.proxy(AccountRepository.class,(p,m,a)->{
    if(m.getName().equals("repositoryForClassicSource"))return p;
    if(m.getName().equals("deliverLetter")){
     calls.incrementAndGet();if(during!=null)return during.deliver(a);
     return delivered(sender,receiver,a);
    }
    if(m.getName().equals("letters"))return new RepositoryLetterSnapshot(true,List.copyOf(rows.values()));
    if(m.getName().equals("sourceInventory"))return List.of();
    return m.invoke(original,a);
   });
  }
  RepositoryLetterDelivery delivered(ClientSession s,ClientSession r,Object[] args){
   RepositoryLetter letter=new RepositoryLetter(ids.incrementAndGet(),1,s.playerName(),(String)args[2],"26/10/04",0,"Title","Body",false);
   rows.put(letter.itemObjectId(),letter);return new RepositoryLetterDelivery(RepositoryLetterDeliveryStatus.DELIVERED,letter);
  }
 }
 @Test void sharedCopyReservationsProtectCapacityAndPaperAndReleaseOnFailure(){
  RuntimeInventory sender=inventory(2,0),receiver=inventory(0,255);long paper=sender.items().get(0).objectId();
  try(var removal=sender.reserveLetterPaper(paper);var slot=receiver.reserveLetterSlot(0,0)){
   assertNotNull(removal);assertNotNull(slot);assertFalse(sender.copy().remove(paper,2));
   assertNull(sender.copy().removeEntire(paper));assertFalse(sender.copy().removeCatalog(InventoryCatalog.LETTER_PAPER,2));
   assertFalse(receiver.copy().add(InventoryCatalog.require(InventoryCatalog.LETTER_CLOSED),1));
   assertNull(receiver.copy().reserveLetterSlot(0,0));assertNull(inventory(0,0).reserveLetterPaper(paper));
  }
  assertTrue(sender.copy().remove(paper,2));assertTrue(receiver.copy().add(InventoryCatalog.require(InventoryCatalog.LETTER_CLOSED),1));
 }
 @Test void actualSqlBoundaryReservationPreventsConcurrentCapacityFill()throws Exception{
  try(var pair=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=pair.f;ready(f.session);ready(pair.other);set(f.session,"inventory",inventory(1,0));set(pair.other,"inventory",inventory(0,255));
   Source source=new Source();source.during=a->{
    RuntimeInventory copy=inv(pair.other).copy();assertFalse(copy.add(InventoryCatalog.require(InventoryCatalog.LETTER_CLOSED),1));set(pair.other,"inventory",copy);
    assertFalse(inv(f.session).copy().remove(paper(f.session).objectId(),1));
    return source.delivered(f.session,pair.other,a);
   };set(f.session,"accounts",source.repository(f.accounts,f.session,pair.other));
   send(f.session,pair.other);assertEquals(1,source.calls.get());assertEquals(0,f.count(InventoryCatalog.LETTER_PAPER));assertEquals(256,inv(pair.other).size());
   assertEquals(1,source.rows.keySet().stream().filter(id->invUnchecked(pair.other).item(id)!=null).count());assertFalse(f.hub.nativeLetterReconcilePending(f.session));assertFalse(f.hub.nativeLetterReconcilePending(pair.other));
   assertEquals(2,pair.drain().size());
  }
 }
 static RuntimeInventory invUnchecked(ClientSession s){try{return inv(s);}catch(Exception e){throw new RuntimeException(e);}}
 @Test void rebuiltFullRecipientRetainsDurableReceiptAndReconcilesOnlyAfterSpace()throws Exception{
  try(var pair=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=pair.f;ready(f.session);ready(pair.other);set(f.session,"inventory",inventory(1,0));set(pair.other,"inventory",inventory(0,255));
   Source source=new Source();source.during=a->{set(pair.other,"inventory",inventory(0,256));return source.delivered(f.session,pair.other,a);};
   AccountRepository repository=source.repository(f.accounts,f.session,pair.other);set(f.session,"accounts",repository);set(pair.other,"accounts",repository);
   send(f.session,pair.other);long id=source.rows.keySet().iterator().next();assertEquals(0,f.count(InventoryCatalog.LETTER_PAPER));assertNull(inv(pair.other).item(id));assertEquals(256,inv(pair.other).size());
   assertTrue(f.hub.nativeLetterReconcilePending(pair.other));assertTrue(pair.drain().isEmpty());
   Method persist=ClientSession.class.getDeclaredMethod("persistSqlWorldInventory",RuntimeInventory.class);persist.setAccessible(true);
   var refused=assertThrows(InvocationTargetException.class,()->persist.invoke(pair.other,inv(pair.other).copy()));assertInstanceOf(java.io.IOException.class,refused.getCause());
   inv(pair.other).removeEntire(inv(pair.other).items().get(0).objectId());reconcile(pair.other);
   assertNotNull(inv(pair.other).item(id));assertEquals(256,inv(pair.other).size());assertFalse(f.hub.nativeLetterReconcilePending(pair.other));
   assertEquals(2,pair.drain().size());reconcile(pair.other);assertTrue(pair.drain().isEmpty());
  }
 }
 @Test void failedSqlReleasesPaperAndSlotForSuccessfulRetry()throws Exception{
  try(var pair=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=pair.f;ready(f.session);ready(pair.other);set(f.session,"inventory",inventory(1,0));set(pair.other,"inventory",inventory(0,255));
   Source source=new Source();source.during=a->{throw new SQLException("owned precommit refusal");};set(f.session,"accounts",source.repository(f.accounts,f.session,pair.other));
   assertThrows(SQLException.class,()->send(f.session,pair.other));assertEquals(1,f.count(InventoryCatalog.LETTER_PAPER));assertEquals(255,inv(pair.other).size());assertFalse(f.hub.nativeLetterReconcilePending(f.session));assertFalse(f.hub.nativeLetterReconcilePending(pair.other));
   source.during=null;send(f.session,pair.other);assertEquals(2,source.calls.get());assertEquals(256,inv(pair.other).size());
  }
 }
 @Test void fullSelfSendConsumesLastPaperThenAdmitsOneStableLetter()throws Exception{
  try(var pair=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=pair.f;ready(f.session);set(f.session,"inventory",inventory(1,255));Source source=new Source();set(f.session,"accounts",source.repository(f.accounts,f.session,f.session));
   send(f.session,f.session);assertEquals(256,inv(f.session).size());assertEquals(0,f.count(InventoryCatalog.LETTER_PAPER));assertNotNull(inv(f.session).item(source.rows.keySet().iterator().next()));assertFalse(f.hub.nativeLetterReconcilePending(f.session));assertEquals(2,f.drain().size());
  }
 }
 @Test void concurrentOtherAndSelfDeliveriesUseFreshRecipientState()throws Exception{
  try(var pair=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=pair.f;ready(f.session);ready(pair.other);set(f.session,"inventory",inventory(1,0));set(pair.other,"inventory",inventory(1,254));Source source=new Source();CountDownLatch entered=new CountDownLatch(2),release=new CountDownLatch(1);
   source.during=a->{entered.countDown();assertTrue(release.await(5,TimeUnit.SECONDS));return source.delivered(f.session,pair.other,a);};
   AccountRepository repository=source.repository(f.accounts,f.session,pair.other);set(f.session,"accounts",repository);set(pair.other,"accounts",repository);
   ExecutorService pool=Executors.newFixedThreadPool(2);
   try{
    Future<?> one=pool.submit(()->{try{send(f.session,pair.other);}catch(Exception e){throw new RuntimeException(e);}});
    Future<?> two=pool.submit(()->{try{send(pair.other,pair.other);}catch(Exception e){throw new RuntimeException(e);}});
    assertTrue(entered.await(5,TimeUnit.SECONDS));release.countDown();one.get(5,TimeUnit.SECONDS);two.get(5,TimeUnit.SECONDS);
    assertEquals(2,source.calls.get());assertEquals(256,inv(pair.other).size());for(long id:source.rows.keySet())assertNotNull(inv(pair.other).item(id));
    assertEquals(0,f.count(InventoryCatalog.LETTER_PAPER));assertEquals(0,pair.otherCount(InventoryCatalog.LETTER_PAPER));assertFalse(f.hub.nativeLetterReconcilePending(pair.other));
   }finally{release.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
  }
 }
 @Test void recipientReselectAndDisconnectCannotInstallOldContext()throws Exception{
  for(boolean disconnect:List.of(false,true))try(var pair=new NativePlayerTradeBoundaryTest.Pair(temporary.resolve("ctx"+disconnect))){
   var f=pair.f;ready(f.session);ready(pair.other);set(f.session,"inventory",inventory(1,0));set(pair.other,"inventory",inventory(0,0));Source source=new Source();
   source.during=a->{
    if(disconnect)f.hub.disconnect(pair.other);else {assertTrue(f.hub.selectCharacter(pair.other,1,pair.source,pair.character));set(pair.other,"inventoryCharacter","");}
    return source.delivered(f.session,pair.other,a);
   };AccountRepository repository=source.repository(f.accounts,f.session,pair.other);set(f.session,"accounts",repository);set(pair.other,"accounts",repository);
   send(f.session,pair.other);assertEquals(0,inv(pair.other).size());assertTrue(pair.drain().isEmpty());assertTrue(f.hub.nativeLetterReconcilePending(pair.other));
   if(!disconnect){ready(pair.other);reconcile(pair.other);assertEquals(1,inv(pair.other).size());assertFalse(f.hub.nativeLetterReconcilePending(pair.other));}
  }
 }
 @Test void selectedOwnerAndSourceAdmissionCannotBeForgedByReadyFields()throws Exception{
  try(var pair=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=pair.f;ready(f.session);ready(pair.other);set(f.session,"inventory",inventory(1,0));Source source=new Source();set(f.session,"accounts",source.repository(f.accounts,f.session,pair.other));
   f.hub.disconnect(f.session);send(f.session,pair.other);assertEquals(0,source.calls.get());assertEquals(1,f.count(InventoryCatalog.LETTER_PAPER));
  }
 }
 @Test void senderReselectDefersPaperDebtAndForeignSourceCannotAdmit()throws Exception{
  try(var pair=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=pair.f;ready(f.session);ready(pair.other);set(f.session,"inventory",inventory(1,0));set(pair.other,"inventory",inventory(0,0));Source source=new Source();
   source.during=a->{assertTrue(f.hub.selectCharacter(f.session,1,pair.source,f.accounts.characters("test").get(0)));set(f.session,"inventoryCharacter","");return source.delivered(f.session,pair.other,a);};
   AccountRepository repository=source.repository(f.accounts,f.session,pair.other);set(f.session,"accounts",repository);set(pair.other,"accounts",repository);
   send(f.session,pair.other);assertEquals(1,f.count(InventoryCatalog.LETTER_PAPER));assertTrue(f.hub.nativeLetterReconcilePending(f.session));assertEquals(1,inv(pair.other).size());
   ready(f.session);reconcile(f.session);assertEquals(0,f.count(InventoryCatalog.LETTER_PAPER));assertFalse(f.hub.nativeLetterReconcilePending(f.session));
   set(f.session,"inventory",inventory(1,0));String foreign="other-source";assertTrue(f.hub.selectCharacter(pair.other,2,foreign,pair.character));set(pair.other,"selectedClassicServerName",foreign);ready(pair.other);
   send(f.session,pair.other);assertEquals(1,source.calls.get());assertEquals(1,f.count(InventoryCatalog.LETTER_PAPER));
  }
 }
 @Test void deliveredResultCannotReplaceAuthoritativeRecipientMembershipRead()throws Exception{
  try(var pair=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=pair.f;ready(f.session);ready(pair.other);set(f.session,"inventory",inventory(1,0));set(pair.other,"inventory",inventory(0,0));Source source=new Source();AtomicBoolean readable=new AtomicBoolean(false);
   AccountRepository normal=source.repository(f.accounts,f.session,pair.other);
   AccountRepository repository=NativeLetterSqlFaultTest.proxy(AccountRepository.class,(p,m,a)->{
    if(m.getName().equals("repositoryForClassicSource"))return p;
    if(m.getName().equals("letters")&&!readable.get())return RepositoryLetterSnapshot.unavailable();
    return m.invoke(normal,a);
   });set(f.session,"accounts",repository);set(pair.other,"accounts",repository);
   send(f.session,pair.other);assertEquals(1,source.rows.size());assertEquals(0,f.count(InventoryCatalog.LETTER_PAPER));assertEquals(0,inv(pair.other).size());assertTrue(f.hub.nativeLetterReconcilePending(pair.other));assertTrue(pair.drain().isEmpty());
   readable.set(true);reconcile(pair.other);assertEquals(1,inv(pair.other).size());assertFalse(f.hub.nativeLetterReconcilePending(pair.other));
  }
 }
 @Test void bootstrappedInventoryPublishesExactRemoveAddAndArrival()throws Exception{
  try(var pair=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=pair.f;ready(f.session);ready(pair.other);set(f.session,"inventory",inventory(1,0));set(pair.other,"inventory",inventory(0,0));set(f.session,"inventoryBootstrapSent",true);set(pair.other,"inventoryBootstrapSent",true);
   Source source=new Source();AccountRepository repository=source.repository(f.accounts,f.session,pair.other);set(f.session,"accounts",repository);set(pair.other,"accounts",repository);
   send(f.session,pair.other);var sender=f.drain();var recipient=pair.drain();assertEquals(1,sender.size());assertEquals(Protocol.S_REMOVE_INVENTORY,sender.get(0)[0]&255);assertEquals(3,recipient.size());assertEquals(Protocol.S_ADD_INVENTORY,recipient.get(0)[0]&255);
   assertEquals(source.rows.keySet().iterator().next().longValue(),new Protocol.Reader(Arrays.copyOfRange(recipient.get(0),1,recipient.get(0).length)).d());
  }
 }
}
