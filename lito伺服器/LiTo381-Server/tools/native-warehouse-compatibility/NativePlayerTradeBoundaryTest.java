package local.lito381.server;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class NativePlayerTradeBoundaryTest {
 @TempDir Path temporary;
 @Test void movedAfterFirstConfirmFailsWithoutPersistence()throws Exception {
  try(Pair p=new Pair(temporary)){
   p.open();p.offerPotion();assertEquals(GameHub.TradeConfirmStatus.WAITING,p.f.hub.confirmPlayerTrade(p.f.session).status());
   NativeWarehouseCompatibilityTest.set(p.other,"positionX",p.f.npc.x()+10);
   assertEquals(GameHub.TradeConfirmStatus.FAILED,p.f.hub.confirmPlayerTrade(p.other).status());
   assertEquals(5,p.f.count(InventoryCatalog.NATIVE_HEALING_POTION));assertEquals(0,p.otherCount(InventoryCatalog.NATIVE_HEALING_POTION));
   p.assertUnchangedState();
  }
 }
 @Test void currentIncomingWeightRecheckedAtFinalCommit()throws Exception {
  try(Pair p=new Pair(temporary)){
   p.open();p.offerPotion();assertEquals(GameHub.TradeConfirmStatus.WAITING,p.f.hub.confirmPlayerTrade(p.f.session).status());
   NativeWarehouseCompatibilityTest.set(p.other,"retainedSourceInventoryWeight",2_000_000);
   assertEquals(GameHub.TradeConfirmStatus.FAILED,p.f.hub.confirmPlayerTrade(p.other).status());
   assertEquals(5,p.f.count(InventoryCatalog.NATIVE_HEALING_POTION));p.assertUnchangedState();
  }
 }
 @Test void sameKeyReselectAndBattleGenerationCannotReviveOwner()throws Exception {
  try(Pair p=new Pair(temporary)){
   p.open();p.offerPotion();assertTrue(p.f.hub.selectCharacter(p.other,1,p.source,p.character));
   assertEquals(GameHub.TradeConfirmStatus.FAILED,p.f.hub.confirmPlayerTrade(p.f.session).status());
   p.assertUnchangedState();
  }
  try(Pair p=new Pair(temporary.resolve("generation"))){
   p.open();p.offerPotion();Map<?,?> rooms=(Map<?,?>)NativeWarehouseCompatibilityTest.get(p.f.hub,"rooms");Object room=rooms.get(p.room);
   long old=(long)NativeWarehouseCompatibilityTest.get(room,"battleGeneration");NativeWarehouseCompatibilityTest.set(room,"battleGeneration",old+1);
   assertEquals(GameHub.TradeConfirmStatus.FAILED,p.f.hub.confirmPlayerTrade(p.f.session).status());
   p.assertUnchangedState();
  }
 }
 @Test void lockedFinalOfferPairPersistsBeforeInstallAndBothConfirmAreRequired()throws Exception {
  try(Pair p=new Pair(temporary)){
   p.open();p.offerPotion();long money=((RuntimeInventory)NativeWarehouseCompatibilityTest.get(p.other,"inventory")).items().get(0).objectId();
   assertNotNull(p.f.hub.addPlayerTradeItem(p.other,money,20));
   p.f.call("confirmPlayerTrade",new byte[]{0x37});assertTrue(p.f.drain().isEmpty());assertTrue(p.drain().isEmpty());
   assertNull(p.f.hub.addPlayerTradeItem(p.other,money,1));
   p.call("confirmPlayerTrade",new byte[]{0x37});
   assertEquals(4,p.f.count(InventoryCatalog.NATIVE_HEALING_POTION));assertEquals(120,p.f.count(InventoryCatalog.ADENA));
   assertEquals(1,p.otherCount(InventoryCatalog.NATIVE_HEALING_POTION));assertEquals(80,p.otherCount(InventoryCatalog.ADENA));
   var reopened=new GameStateStore(p.f.statePath,true);assertEquals(1,reopened.economy(p.other.sourceCharacterIdentity()).inventory().get(InventoryCatalog.NATIVE_HEALING_POTION));
   List<byte[]> first=p.f.drain(), second=p.drain(); assertEquals(0x3E,Byte.toUnsignedInt(first.get(first.size()-1)[0]));assertEquals(0x3E,Byte.toUnsignedInt(second.get(second.size()-1)[0]));
   assertEquals(GameHub.TradeConfirmStatus.INVALID,p.f.hub.confirmPlayerTrade(p.other).status());
  }
 }
 @Test void staleOfferedQuantityAndCancelRestoreActualInventory()throws Exception {
  try(Pair p=new Pair(temporary)){
   p.open();p.offerPotion();assertTrue(p.f.inv().remove(p.f.item(InventoryCatalog.NATIVE_HEALING_POTION).objectId(),5));
   assertEquals(GameHub.TradeConfirmStatus.WAITING,p.f.hub.confirmPlayerTrade(p.f.session).status());
   assertEquals(GameHub.TradeConfirmStatus.FAILED,p.f.hub.confirmPlayerTrade(p.other).status());p.assertUnchangedState();
  }
  try(Pair p=new Pair(temporary.resolve("cancel"))){
   p.open();p.offerPotion();Method cancel=ClientSession.class.getDeclaredMethod("cancelActivePlayerTrade",String.class,boolean.class);cancel.setAccessible(true);cancel.invoke(p.f.session,"disconnect",false);
   assertTrue(p.f.drain().isEmpty());List<byte[]> partner=p.drain();assertEquals(0x3E,Byte.toUnsignedInt(partner.get(partner.size()-1)[0]));
   p.f.hub.disconnect(p.f.session);assertEquals(GameHub.TradeConfirmStatus.INVALID,p.f.hub.confirmPlayerTrade(p.other).status());
   assertEquals(5,p.f.count(InventoryCatalog.NATIVE_HEALING_POTION));p.assertUnchangedState();
  }
 }
 @Test void targetlessAndWorldRemainClosedAndUnregisteredActorCannotBegin()throws Exception {
  try(Pair p=new Pair(temporary)){
   assertThrows(IllegalArgumentException.class,()->Protocol.exchangeRequest(new byte[]{0x34}));
   NativeWarehouseCompatibilityTest.set(p.other,"phase",Enum.valueOf((Class)NativeWarehouseCompatibilityTest.get(p.other,"phase").getClass(),"WORLD"));
   assertNull(p.f.hub.beginPlayerTrade(p.room,p.f.session,p.other.playerObjectId()));
   p.f.hub.disconnect(p.other);assertNull(p.f.hub.beginPlayerTrade(p.room,p.f.session,p.other.playerObjectId()));
  }
 }
 static final class Pair implements AutoCloseable {
  final NativeWarehouseCompatibilityTest.Fixture f;final ClientSession other;final Socket client,server;final CharacterSummary character;
  final String source;final long room;final byte[] stateBefore;final Protocol.CipherState receiver=new Protocol.CipherState(0x38105672);
  Pair(Path root)throws Exception {
   f=new NativeWarehouseCompatibilityTest.Fixture(root);source=f.config.classicServerNames().get(0);
   client=new Socket("127.0.0.1",f.listener.getLocalPort());server=f.listener.accept();client.setSoTimeout(50);
   other=new ClientSession(server,ClientSession.Endpoint.GAME,f.config,f.accounts,f.state,f.capture,f.hub);
   character=f.accounts.characters("test4").get(0);
   NativeWarehouseCompatibilityTest.set(other,"account","test4");NativeWarehouseCompatibilityTest.set(other,"characters",List.of(character));NativeWarehouseCompatibilityTest.set(other,"selectedCharacter",character.name());NativeWarehouseCompatibilityTest.set(other,"selectedClassicServerName",source);
   NativeWarehouseCompatibilityTest.set(other,"playerHp",100);NativeWarehouseCompatibilityTest.set(other,"playerMaxHp",100);
   NativeWarehouseCompatibilityTest.set(other,"sourceEquipmentMagicStats",SourceEquipmentMagicStats.none());NativeWarehouseCompatibilityTest.set(other,"sourcePhysicalGrowthStats",SourcePhysicalGrowthStats.classic381());
   NativeWarehouseCompatibilityTest.set(other,"sourceEquipmentVitalStats",SourceEquipmentVitalStats.none());NativeWarehouseCompatibilityTest.set(other,"sourceInventoryWeightStats",SourceInventoryWeightStats.none(100.0));
   NativeWarehouseCompatibilityTest.set(other,"outgoing",new Protocol.CipherState(0x38105672));
   AtomicLong ids=new AtomicLong(0x78000000L);NativeWarehouseCompatibilityTest.set(other,"inventory",RuntimeInventory.fromCatalogCounts(Map.of(InventoryCatalog.ADENA,100),ids::incrementAndGet));
   f.hub.connect(f.session);f.hub.connect(other);
   assertTrue(f.hub.selectCharacter(f.session,1,source,f.accounts.characters("test").get(0)));assertTrue(f.hub.selectCharacter(other,1,source,character));
   int channel=GameHub.recommendedChannel(character.level()).id();assertEquals(channel,GameHub.recommendedChannel(f.accounts.characters("test").get(0).level()).id());
   assertNotNull(f.hub.enterChannel(channel,f.session));assertNotNull(f.hub.enterChannel(channel,other));
   room=f.hub.createRoom(channel,new GameHub.RoomSettings("trade-boundary","",2,0,0,0,0,0,0,List.of()));
   f.hub.joinRoom(room,f.session);f.hub.joinRoom(room,other);
   f.hub.setReady(room,f.session,true);f.hub.setReady(room,other,true);
   Method tick=GameHub.class.getDeclaredMethod("countdownWork",long.class);tick.setAccessible(true);long now=System.nanoTime();
   for(int i=1;i<=3;i++)tick.invoke(f.hub,now+i*2_000_000_000L);
   for(ClientSession s:List.of(f.session,other)){
    NativeWarehouseCompatibilityTest.set(s,"phase",Enum.valueOf((Class)NativeWarehouseCompatibilityTest.get(s,"phase").getClass(),"BATTLE"));
    NativeWarehouseCompatibilityTest.set(s,"currentRoomId",room);NativeWarehouseCompatibilityTest.set(s,"positionX",f.npc.x());NativeWarehouseCompatibilityTest.set(s,"positionY",f.npc.y());
   }
   NativeWarehouseCompatibilityTest.set(other,"positionX",f.npc.x()+1); stateBefore=Files.exists(f.statePath)?Files.readAllBytes(f.statePath):null;
  }
  void assertUnchangedState()throws Exception {assertArrayEquals(stateBefore,Files.exists(f.statePath)?Files.readAllBytes(f.statePath):null);}
  void open(){assertSame(other,f.hub.beginPlayerTrade(room,f.session,other.playerObjectId()));assertEquals(GameHub.TradeAnswerStatus.OPENED,f.hub.answerPlayerTrade(other,true).status());}
  void offerPotion()throws Exception{assertNotNull(f.hub.addPlayerTradeItem(f.session,f.item(InventoryCatalog.NATIVE_HEALING_POTION).objectId(),1));}
  long otherCount(long catalog)throws Exception{return ((RuntimeInventory)NativeWarehouseCompatibilityTest.get(other,"inventory")).items().stream().filter(i->i.definition().catalogId()==catalog).mapToLong(RuntimeInventory.Item::quantity).sum();}
  void call(String method,byte[] bytes)throws Exception{Method m=ClientSession.class.getDeclaredMethod(method,byte[].class);m.setAccessible(true);m.invoke(other,(Object)bytes);}
  List<byte[]> drain()throws Exception{var packets=new ArrayList<byte[]>();try{while(true)packets.add(receiver.decrypt(Protocol.readFrame(client.getInputStream(),8192)));}catch(SocketTimeoutException end){}return packets;}
  public void close()throws Exception{server.close();client.close();f.close();}
 }
}
