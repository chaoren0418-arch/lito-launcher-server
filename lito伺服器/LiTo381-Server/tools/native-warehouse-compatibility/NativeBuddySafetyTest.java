package local.lito381.server;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class NativeBuddySafetyTest {
 @TempDir Path temporary;
 static GameHub.BuddyPersistentData persisted(int groups,int rows,String source){
  var gs=new ArrayList<GameHub.BuddyGroup>();for(int id=1;id<=groups;id++)gs.add(new GameHub.BuddyGroup(id,id==1?"Friends":"Group"+id));
  var bs=new ArrayList<GameHub.BuddyPersistentEntry>();for(int i=0;i<rows;i++)bs.add(new GameHub.BuddyPersistentEntry(String.format(Locale.ROOT,"b%03d",i),8000+i,source,1+i%groups,1,1,"StoredOrigin"));
  return new GameHub.BuddyPersistentData(gs,bs);
 }
 static void admit(NativeWarehouseCompatibilityTest.Fixture f)throws Exception {
  f.hub.connect(f.session);assertTrue(f.hub.selectCharacter(f.session,1,f.config.classicServerNames().get(0),f.accounts.characters("test").get(0)));
 }
 static Map<Integer,String> groups(byte[] bytes){
  var r=new Protocol.Reader(bytes);assertEquals(0xED,r.c());int count=r.c();assertTrue(count<=10);var result=new LinkedHashMap<Integer,String>();
  for(int i=0;i<count;i++)result.put(r.h(),r.s());assertEquals(0,r.remaining());return result;
 }
 static Map<String,Integer> rows(byte[] bytes){
  var r=new Protocol.Reader(bytes);assertEquals(0xEA,r.c());int count=r.c();assertTrue(count<=127);var result=new LinkedHashMap<String,Integer>();
  for(int i=0;i<count;i++){String name=r.s();int group=r.h();r.c();r.c();r.s();r.c();result.put(name,group);}assertEquals(0,r.remaining());return result;
 }
 static List<byte[]> query(NativeWarehouseCompatibilityTest.Fixture f)throws Exception{f.call("sendBuddyData",new byte[]{(byte)0xE5});return f.drain();}
 static void ordered(List<byte[]> packets,int groupCount,int rowCount){assertEquals(2,packets.size());assertEquals(groupCount,groups(packets.get(0)).size());assertEquals(rowCount,rows(packets.get(1)).size());}
 @Test void encoderRejects128AndLegacyProjectionNeverDeletesAuthority()throws Exception {
  var wireRows=new ArrayList<Protocol.LitoBuddyRow>();for(int i=0;i<128;i++)wireRows.add(new Protocol.LitoBuddyRow("r"+i,1,1,1,"s",Protocol.LitoBuddyActivity.OFFLINE));
  assertThrows(IllegalArgumentException.class,()->Protocol.buddyList(wireRows));assertEquals(127,rows(Protocol.buddyList(wireRows.subList(0,127))).size());
  try(var f=new NativeWarehouseCompatibilityTest.Fixture(temporary)){
   admit(f);String owner=f.session.sourceCharacterIdentity(),source=f.config.classicServerNames().get(0);var raw=persisted(11,128,source);
   f.hub.replacePersistentBuddyData(owner,raw);var packets=query(f);ordered(packets,10,127);
   assertEquals(1,rows(packets.get(1)).get("b010"));assertFalse(rows(packets.get(1)).containsKey("ArenaGuide"));assertFalse(rows(packets.get(1)).containsKey("b127"));
   var again=query(f);assertArrayEquals(packets.get(0),again.get(0));assertArrayEquals(packets.get(1),again.get(1));
   AtomicReference<GameHub.BuddyPersistentData> saved=new AtomicReference<>();f.hub.registerBuddyPersistence(owner,replacement->{saved.set(replacement);return true;});
   f.call("renameBuddyGroup",new Protocol.Writer().c(0xE8).h(2).s("Renamed").bytes());ordered(f.drain(),10,127);
   assertEquals(11,saved.get().groups().size());assertEquals(128,saved.get().buddies().size());assertEquals(11,saved.get().buddies().get(10).groupId());
   assertEquals(raw.buddies(),saved.get().buddies());
   f.call("renameBuddyGroup",new Protocol.Writer().c(0xE8).h(11).s("Hidden").bytes());assertTrue(f.drain().isEmpty());
  }
 }
 @Test void actualGroup9To10AndCreate11RejectWithCoherentOrderedReplies()throws Exception {
  try(var f=new NativeWarehouseCompatibilityTest.Fixture(temporary)){
   admit(f);String owner=f.session.sourceCharacterIdentity();f.hub.replacePersistentBuddyData(owner,persisted(9,3,f.config.classicServerNames().get(0)));
   AtomicInteger commits=new AtomicInteger();f.hub.registerBuddyPersistence(owner,data->{commits.incrementAndGet();return true;});
   f.call("createBuddyGroup",new Protocol.Writer().c(0xE6).s("Tenth").bytes());ordered(f.drain(),10,4);assertEquals(1,commits.get());
   f.call("createBuddyGroup",new Protocol.Writer().c(0xE6).s("Eleventh").bytes());assertTrue(f.drain().isEmpty());assertEquals(1,commits.get());
   f.call("removeBuddyGroup",new Protocol.Writer().c(0xE7).h(2).bytes());var removed=f.drain();ordered(removed,9,4);assertEquals(1,rows(removed.get(1)).get("b001"));
   f.call("renameBuddyGroup",new Protocol.Writer().c(0xE8).h(3).s("New3").bytes());var renamed=f.drain();ordered(renamed,9,4);assertEquals("New3",groups(renamed.get(0)).get(3));
   f.call("changeBuddyGroup",new Protocol.Writer().c(0xE9).s("b000").h(3).bytes());var moved=f.drain();assertEquals(3,moved.size());assertEquals(0xEE,Byte.toUnsignedInt(moved.get(0)[0]));ordered(moved.subList(1,3),9,4);assertEquals(3,rows(moved.get(2)).get("b000"));
  }
 }
 @Test void durable126127128AndGuideBudgetUseOneWholeEA()throws Exception {
  for(int count:new int[]{126,127,128})try(var f=new NativeWarehouseCompatibilityTest.Fixture(temporary.resolve("rows"+count))){
   admit(f);String owner=f.session.sourceCharacterIdentity();f.hub.replacePersistentBuddyData(owner,persisted(1,count,f.config.classicServerNames().get(0)));
   var packets=query(f);ordered(packets,1,127);assertEquals(count==126,rows(packets.get(1)).containsKey("ArenaGuide"));
   f.call("addBuddy",new Protocol.Writer().c(Protocol.C_ADD_BUDDY).s("ZManual[0]").bytes());var reply=f.drain();
   if(count==126){ordered(reply,1,127);assertTrue(rows(reply.get(1)).containsKey("ZManual"));assertFalse(rows(reply.get(1)).containsKey("ArenaGuide"));}
   else {assertEquals(1,reply.size());assertArrayEquals(Protocol.buddyError(Protocol.LitoBuddyError.LIST_FULL),reply.get(0));}
  }
 }
 @Test void actualOnlineSyntheticRowsNeverExceedTotalAndCannotRewriteOtherSourceIdentity()throws Exception {
  try(var pair=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=pair.f;int channel=GameHub.recommendedChannel(pair.character.level()).id();NativeWarehouseCompatibilityTest.set(f.session,"currentChannelId",channel);
   String owner=f.session.sourceCharacterIdentity();var raw=persisted(1,125,pair.source);f.hub.replacePersistentBuddyData(owner,raw);
   var packet=query(f);ordered(packet,1,127);assertTrue(rows(packet.get(1)).containsKey("ArenaGuide"));assertTrue(rows(packet.get(1)).containsKey(pair.character.name()));
   var foreign=new GameHub.BuddyPersistentEntry(pair.character.name(),9999,"ForeignSource",1,1,1,"ForeignOrigin");
   var list=new ArrayList<>(raw.buddies());list.add(foreign);f.hub.replacePersistentBuddyData(owner,new GameHub.BuddyPersistentData(raw.groups(),list));
   var data=f.hub.buddyData(owner,channel);var seen=data.buddies().stream().filter(b->b.name().equals(pair.character.name())).findFirst().orElseThrow();
   assertEquals(Protocol.LitoBuddyActivity.OFFLINE,seen.activity());assertEquals("ForeignOrigin",seen.origin());
   AtomicReference<GameHub.BuddyPersistentData> saved=new AtomicReference<>();f.hub.registerBuddyPersistence(owner,value->{saved.set(value);return true;});
   assertTrue(f.hub.renameBuddyGroup(owner,1,"Friends2"));assertTrue(saved.get().buddies().contains(foreign));
  }
 }
 @Test void persistenceRejectAndStalePrivateSnapshotCannotPublishSuccess()throws Exception {
  try(var f=new NativeWarehouseCompatibilityTest.Fixture(temporary)){
   admit(f);String owner=f.session.sourceCharacterIdentity();f.hub.replacePersistentBuddyData(owner,persisted(2,2,f.config.classicServerNames().get(0)));
   GameHub.NativeBuddySnapshot prior=f.hub.nativeBuddySnapshot(f.session,-1);
   f.hub.registerBuddyPersistence(owner,replacement->false);
   for(var action:List.of(new Object[]{"createBuddyGroup",new Protocol.Writer().c(0xE6).s("Fail").bytes()},new Object[]{"removeBuddyGroup",new Protocol.Writer().c(0xE7).h(2).bytes()},new Object[]{"renameBuddyGroup",new Protocol.Writer().c(0xE8).h(2).s("Fail").bytes()},new Object[]{"changeBuddyGroup",new Protocol.Writer().c(0xE9).s("b000").h(2).bytes()})){
    f.call((String)action[0],action[1]);assertTrue(f.drain().isEmpty());assertTrue(prior.current(f.session));
   }
   f.hub.registerBuddyPersistence(owner,replacement->true);assertTrue(f.hub.renameBuddyGroup(owner,2,"New"));assertFalse(prior.current(f.session));assertFalse(prior.sameContext(f.session));
   f.call("sendBuddySnapshot",prior);assertTrue(f.drain().isEmpty());
   var current=f.hub.nativeBuddySnapshot(f.session,-1);assertTrue(f.hub.selectCharacter(f.session,1,f.config.classicServerNames().get(0),f.accounts.characters("test").get(0)));
   assertFalse(current.current(f.session));assertFalse(current.sameContext(f.session));f.call("sendBuddySnapshot",current);assertTrue(f.drain().isEmpty());
   var beforeActivity=f.hub.nativeBuddySnapshot(f.session,-1);assertTrue(f.hub.setBuddyActivity(owner,"b000",Protocol.LitoBuddyActivity.FIGHTING));assertFalse(beforeActivity.sameContext(f.session));
   NativeWarehouseCompatibilityTest.set(f.session,"selectedClassicServerName","WrongSource");f.call("createBuddyGroup",new Protocol.Writer().c(0xE6).s("Wrong").bytes());assertTrue(f.drain().isEmpty());
  }
  try(var f=new NativeWarehouseCompatibilityTest.Fixture(temporary.resolve("disconnect"))){admit(f);var snapshot=f.hub.nativeBuddySnapshot(f.session,-1);f.hub.disconnect(f.session);assertFalse(snapshot.current(f.session));assertFalse(snapshot.sameContext(f.session));f.call("sendBuddySnapshot",snapshot);assertTrue(f.drain().isEmpty());}
 }
 @Test void EDAndEAAreAdjacentUnderConcurrentStatusTransportWrites()throws Exception {
  try(var f=new NativeWarehouseCompatibilityTest.Fixture(temporary)){
   admit(f);var snapshot=f.hub.nativeBuddySnapshot(f.session,-1);AtomicReference<Throwable> failure=new AtomicReference<>();
   Method send=ClientSession.class.getDeclaredMethod("sendEncrypted",byte[].class,String.class);send.setAccessible(true);
   Thread status=new Thread(()->{try{for(int i=0;i<12;i++)send.invoke(f.session,Protocol.buddyStatus("ArenaGuide",Protocol.LitoBuddyActivity.WAITING),"bounded-test-status");}catch(Throwable e){failure.set(e);}},"owned-buddy-status");
   status.start();for(int i=0;i<12;i++)f.call("sendBuddySnapshot",snapshot);status.join(2000);assertFalse(status.isAlive());assertNull(failure.get());
   List<byte[]> packets=f.drain();int pairs=0;for(int i=0;i<packets.size();i++)if(Byte.toUnsignedInt(packets.get(i)[0])==0xED){assertEquals(0xEA,Byte.toUnsignedInt(packets.get(++i)[0]));pairs++;}assertEquals(12,pairs);
  }
 }
}
