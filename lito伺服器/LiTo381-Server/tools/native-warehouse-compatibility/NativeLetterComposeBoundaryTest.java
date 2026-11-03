package local.lito381.server;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class NativeLetterComposeBoundaryTest {
 @TempDir Path temporary;
 @Test void repeatedSingleRecipientConsumesOneEachWithoutDedupe()throws Exception {
  GameStateStore s=new GameStateStore(temporary.resolve("state"),true);
  s.saveEconomy("source:7:sender",0,Map.of(InventoryCatalog.LETTER_PAPER,2));
  var a=s.deliverLetter("source:7:sender","source:7:receiver",1,"26/10/04","Title","Body");
  var b=s.deliverLetter("source:7:sender","source:7:receiver",1,"26/10/04","Title","Body");
  assertTrue(a.delivered());assertTrue(b.delivered());assertNotEquals(a.letter().id(),b.letter().id());
  assertEquals(0,s.economy("source:7:sender").inventory().getOrDefault(InventoryCatalog.LETTER_PAPER,0));
  assertEquals(2,new GameStateStore(temporary.resolve("state"),true).letters("source:7:receiver").size());
  assertTrue(s.letters("source:42:receiver").isEmpty());
 }
 @Test void pairSaveIOExceptionRestoresPaperRecipientRowsAndNextId()throws Exception {
  Path path=temporary.resolve("state");GameStateStore s=new GameStateStore(path,true);
  s.saveEconomy("sender",0,Map.of(InventoryCatalog.LETTER_PAPER,2));
  var sender=s.economy("sender").inventory();var target=s.economy("receiver").inventory();
  Files.delete(path);Files.createDirectory(path);Files.writeString(path.resolve("block"),"owned test obstruction");
  assertThrows(java.io.IOException.class,()->s.deliverLetter("sender","receiver",1,"26/10/04","Title","Body"));
  assertEquals(sender,s.economy("sender").inventory());assertEquals(target,s.economy("receiver").inventory());assertTrue(s.letters("receiver").isEmpty());
  Files.delete(path.resolve("block"));Files.delete(path);
  var after=s.deliverLetter("sender","receiver",1,"26/10/04","Title","Body");
  GameStateStore fresh=new GameStateStore(temporary.resolve("fresh"),false);fresh.saveEconomy("sender",0,Map.of(InventoryCatalog.LETTER_PAPER,2));
  assertEquals(fresh.deliverLetter("sender","receiver",1,"26/10/04","Title","Body").letter().id(),after.letter().id());
 }
 @Test void semicolonAndDuplicateTokensRemainRejectedBeforeDelivery()throws Exception {
  for(String names:List.of("A;B","A;A","A;",";A","A".repeat(28)," A","A\n"))assertThrows(IllegalArgumentException.class,()->Protocol.letterComposeInventoryUse(packet(1,names)));
  assertEquals("A".repeat(27),Protocol.letterComposeInventoryUse(packet(1,"A".repeat(27))).receiver());
 }
 @Test void actualHandlerOwnsPaperAndRejectsWorldDeadObserverAndMissingRecipient()throws Exception {
  try(var f=new NativeWarehouseCompatibilityTest.Fixture(temporary)){
   seed(f,2);long id=f.item(InventoryCatalog.LETTER_PAPER).objectId();byte[] before=Files.readAllBytes(f.statePath);
   f.call("useInventoryItem",new byte[]{0x1C,1,0,0,0}); // unknown object: no compose tail read
   f.call("useInventoryItem",packet(id,f.session.playerName())); // WORLD gate
   assertArrayEquals(before,Files.readAllBytes(f.statePath));assertEquals(2,f.count(InventoryCatalog.LETTER_PAPER));assertTrue(f.drain().isEmpty());
   NativeWarehouseCompatibilityTest.set(f.session,"phase",Enum.valueOf((Class)NativeWarehouseCompatibilityTest.get(f.session,"phase").getClass(),"BATTLE"));
   NativeWarehouseCompatibilityTest.set(f.session,"playerHp",0);f.call("useInventoryItem",packet(id,f.session.playerName()));
   NativeWarehouseCompatibilityTest.set(f.session,"playerHp",100);NativeWarehouseCompatibilityTest.set(f.session,"observer",true);f.call("useInventoryItem",packet(id,f.session.playerName()));
   NativeWarehouseCompatibilityTest.set(f.session,"observer",false);f.call("useInventoryItem",packet(id,"NoSuchLetterTarget"));
   assertEquals(2,f.count(InventoryCatalog.LETTER_PAPER));assertArrayEquals(before,Files.readAllBytes(f.statePath));assertEquals(1,f.drain().size());
   f.call("useInventoryItem",packet(id,f.session.playerName()));assertEquals(1,f.count(InventoryCatalog.LETTER_PAPER));assertEquals(1,f.count(InventoryCatalog.LETTER_CLOSED));assertEquals(2,f.drain().size());
   assertEquals(1,f.state.letters(f.session.sourceCharacterIdentity()).size());
   f.call("useInventoryItem",packet(id,f.session.playerName()));assertEquals(0,f.count(InventoryCatalog.LETTER_PAPER));assertEquals(2,f.count(InventoryCatalog.LETTER_CLOSED));assertEquals(2,f.drain().size());
   byte[] durable=Files.readAllBytes(f.statePath);f.call("useInventoryItem",packet(id,f.session.playerName()));assertArrayEquals(durable,Files.readAllBytes(f.statePath));assertTrue(f.drain().isEmpty());
  }
 }
 private static byte[] packet(long id,String name){return new Protocol.Writer().c(Protocol.C_USE_ITEM).d(id).h(0x1234).s(name).S("Title").S("Body").bytes();}
 private static void seed(NativeWarehouseCompatibilityTest.Fixture f,int quantity)throws Exception {
  AtomicLong ids=new AtomicLong(0x79000000L);NativeWarehouseCompatibilityTest.set(f.session,"inventory",RuntimeInventory.fromCatalogCounts(Map.of(InventoryCatalog.LETTER_PAPER,quantity),ids::incrementAndGet));
  f.state.saveEconomy(f.session.sourceCharacterIdentity(),0,f.inv().catalogCounts());
 }
}
