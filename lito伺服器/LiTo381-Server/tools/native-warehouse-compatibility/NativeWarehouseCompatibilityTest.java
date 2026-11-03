package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class NativeWarehouseCompatibilityTest {
    @TempDir Path temporary;
    static byte[] db(boolean context,long npc,int mode,long... rows) {
        var w=new Protocol.Writer().c(Protocol.C_LITO_BUY_SELL);
        if(context)w.d(npc);w.c(mode).h(rows.length/2);
        for(long v:rows)w.d(v);
        return w.raw(new byte[]{1,2,3,4,5,6,7,8}).bytes();
    }
    @Test void exactProducerShapesAndOpaqueBytesCannotManufactureOwner() {
        for(int mode:new int[]{2,3,4,5,8,9})for(boolean context:new boolean[]{false,true}){
            byte[] wire=db(context,99,mode,123,4);
            var r=NativeWarehouseCdb.parse(wire);
            assertEquals(context,r.hasContext());assertEquals(mode,r.mode());
            assertEquals(List.of(new Protocol.ShopLine(123,4)),r.lines());
            assertEquals(99,r.forOwnedNpc(99).npcObjectId());
            if(context)assertThrows(IllegalArgumentException.class,()->r.forOwnedNpc(100));
            wire[wire.length-1]=0;byte[] tail=r.opaqueTrailer();tail[0]=0;
            assertArrayEquals(new byte[]{1,2,3,4,5,6,7,8},r.opaqueTrailer());
            assertThrows(IllegalArgumentException.class,()->Protocol.shopTransaction(db(context,99,mode,123,4)));
        }
        for(int mode:new int[]{0,1,11,13,14})assertFalse(NativeWarehouseCdb.isWarehouse(db(false,99,mode,123,1)));
        assertThrows(IllegalArgumentException.class,()->NativeWarehouseCdb.parse(db(true,0,3,123,1)));
        assertThrows(IllegalArgumentException.class,()->NativeWarehouseCdb.parse(db(false,0,3,0,1)));
        assertThrows(IllegalArgumentException.class,()->NativeWarehouseCdb.parse(db(false,0,3,123,0)));
        assertThrows(IllegalArgumentException.class,()->NativeWarehouseCdb.parse(db(false,0,3,123,0x80000000L)));
        byte[] complete=db(false,0,3,123,1);
        for(int missing=1;missing<=8;missing++){
            byte[] shortPacket=Arrays.copyOf(complete,complete.length-missing);
            assertThrows(IllegalArgumentException.class,()->NativeWarehouseCdb.parse(shortPacket));
        }
        long[] maximum=new long[1020];Arrays.fill(maximum,1);
        assertEquals(510,NativeWarehouseCdb.parse(db(false,0,3,maximum)).lines().size());
        assertThrows(IllegalArgumentException.class,()->NativeWarehouseCdb.parse(db(true,99,3,maximum)));
        assertEquals(509,NativeWarehouseCdb.parse(db(true,99,3,Arrays.copyOf(maximum,1018))).lines().size());
    }
    @Test void privateListIdentityOwnerCutoffAndReplayAreExclusive() {
        Object session=new Object(),token=new Object();
        var owner=new NativeWarehouseAdmission.Owner(session,"source:A","source","acct",1,2,"WORLD",-5,99);
        var gate=new NativeWarehouseAdmission(owner,token,Set.of(2),100);
        assertTrue(gate.matches(owner,token,2,99));assertFalse(gate.matches(owner,token,2,100));
        assertFalse(gate.matches(owner,new Object(),2,99));assertFalse(gate.matches(owner,token,3,99));
        assertFalse(gate.matches(new NativeWarehouseAdmission.Owner(new Object(),"source:A","source","acct",1,2,"WORLD",-5,99),token,2,99));
        assertFalse(gate.matches(new NativeWarehouseAdmission.Owner(session,"other:A","other","acct",1,2,"WORLD",-5,99),token,2,99));
        assertFalse(gate.matches(new NativeWarehouseAdmission.Owner(session,"source:B","source","acct",3,2,"WORLD",-5,99),token,2,99));
        assertFalse(gate.matches(new NativeWarehouseAdmission.Owner(session,"source:A","source","acct",1,2,"WORLD",-6,99),token,2,99));
        try(var permit=gate.admit(owner,token,2,99)){
            assertNotNull(permit);gate.retire();assertEquals(NativeWarehouseAdmission.State.ADMITTED,gate.state());
            assertNull(gate.admit(owner,token,2,99));
        }
        assertEquals(NativeWarehouseAdmission.State.RETIRED,gate.state());
        var cancelled=new NativeWarehouseAdmission(owner,token,Set.of(2),100);cancelled.retire();
        assertNull(cancelled.admit(owner,token,2,99));
    }
    @Test void realDbDepositRetrieveChargesSubmittedRowsAndPersistsBeforeDelta() throws Exception {
        try(Fixture f=new Fixture(temporary)){
            long potion=f.item(InventoryCatalog.NATIVE_HEALING_POTION).objectId();
            f.call("armWarehouseDeposits",f.npc.objectId());
            f.call("buySell",db(false,0,2,potion,1,potion,2));
            assertEquals(2,f.count(InventoryCatalog.NATIVE_HEALING_POTION));
            assertEquals(3,f.state.personalWarehouse(f.session.sourceAccountIdentity(),f.session.sourceCharacterIdentity()).inventory().get(InventoryCatalog.NATIVE_HEALING_POTION));
            assertNull(get(f.session,"pendingClassicList"));assertFalse(f.drain().isEmpty());
            f.call("sendWarehouseRetrieveList",3,f.npc.objectId());f.drain();
            RuntimeInventory stored=(RuntimeInventory)get(f.session,"warehouse");
            long id=stored.items().stream().filter(i->i.definition().catalogId()==InventoryCatalog.NATIVE_HEALING_POTION).findFirst().orElseThrow().objectId();
            byte[] retrieve=db(true,f.npc.objectId(),3,id,1,id,1);
            f.call("buySell",retrieve);
            assertEquals(4,f.count(InventoryCatalog.NATIVE_HEALING_POTION));assertEquals(40,f.count(InventoryCatalog.ADENA));
            assertFalse(f.drain().isEmpty());
            var reopened=new GameStateStore(f.statePath,true);
            assertEquals(4,reopened.economy(f.session.sourceCharacterIdentity()).inventory().get(InventoryCatalog.NATIVE_HEALING_POTION));
            assertEquals(1,reopened.personalWarehouse(f.session.sourceAccountIdentity(),f.session.sourceCharacterIdentity()).inventory().get(InventoryCatalog.NATIVE_HEALING_POTION));
            f.call("buySell",retrieve);assertEquals(40,f.count(InventoryCatalog.ADENA));assertTrue(f.drain().isEmpty());
        }
    }
    @Test void realC28CancellationAndOwnerRejectionsDoNotMutate() throws Exception {
        try(Fixture f=new Fixture(temporary)){
            long id=f.item(InventoryCatalog.NATIVE_HEALING_POTION).objectId();
            f.call("armWarehouseDeposits",f.npc.objectId());
            f.call("buySell",db(true,f.npc.objectId()+1,2,id,1));
            assertEquals(5,f.count(InventoryCatalog.NATIVE_HEALING_POTION));assertNull(get(f.session,"pendingClassicList"));
            f.call("armWarehouseDeposits",f.npc.objectId());
            f.call("classicBuySell",new Protocol.Writer().c(Protocol.C_BUY_SELL).d(f.npc.objectId()).c(2).h(1).d(id).d(2).bytes());
            assertEquals(3,f.count(InventoryCatalog.NATIVE_HEALING_POTION));f.drain();
            f.call("sendWarehouseRetrieveList",3,f.npc.objectId());f.drain();
            f.call("classicBuySell",new Protocol.Writer().c(Protocol.C_BUY_SELL).d(f.npc.objectId()).c(3).h(0).bytes());
            assertNull(get(f.session,"pendingClassicList"));assertNull(get(f.session,"nativeWarehouseAdmission"));assertTrue(f.drain().isEmpty());
            f.call("armWarehouseDeposits",f.npc.objectId());
            set(f.session,"positionX",f.npc.x()+20);f.call("buySell",db(false,0,2,id,1));
            assertEquals(3,f.count(InventoryCatalog.NATIVE_HEALING_POTION));assertNull(get(f.session,"pendingClassicList"));
            set(f.session,"positionX",f.npc.x());f.call("armWarehouseDeposits",f.npc.objectId());
            set(f.session,"selectedClassicServerName","differentSource");f.call("buySell",db(false,0,2,id,1));
            assertEquals(3,f.count(InventoryCatalog.NATIVE_HEALING_POTION));assertNull(get(f.session,"pendingClassicList"));
        }
    }
    @Test void staleDurableWarehouseRejectsBeforeInventoryOrSuccessDelta() throws Exception {
        try(Fixture f=new Fixture(temporary)){
            long id=f.item(InventoryCatalog.NATIVE_HEALING_POTION).objectId();
            f.call("armWarehouseDeposits",f.npc.objectId());
            assertTrue(f.state.saveInventoryAndPersonalWarehouse("other",f.session.sourceAccountIdentity(),Map.of(),Map.of(),Map.of(InventoryCatalog.SHORT_SWORD,1)));
            f.call("buySell",db(false,0,2,id,1));
            assertEquals(5,f.count(InventoryCatalog.NATIVE_HEALING_POTION));
            assertEquals(1,f.state.personalWarehouse(f.session.sourceAccountIdentity(),"other").inventory().get(InventoryCatalog.SHORT_SWORD));
            List<byte[]> packets=f.drain();assertEquals(1,packets.size());assertEquals(Protocol.S_MESSAGE_CODE,Byte.toUnsignedInt(packets.get(0)[0]));
        }
    }
    @Test void clanCommitPinSurvivesExpiryReleaseAndBlocksAllClaimants() throws Exception {
        try(Fixture f=new Fixture(temporary)){
            String a=f.session.sourceCharacterIdentity(),b=f.session.sourceCharacterIdentity("second");
            assertTrue(f.hub.createClan(a,"warehouseClan"));assertNotNull(f.hub.addClanCandidate(a,b));assertNotNull(f.hub.admitClanCandidate(a,b));
            try(Socket otherSocket=new Socket()){
                ClientSession other=new ClientSession(otherSocket,ClientSession.Endpoint.GAME,f.config,f.accounts,f.state,f.capture,f.hub);
                long cutoff=System.nanoTime()+100_000_000L;
                assertTrue(f.hub.claimClanWarehouse(a,f.session,cutoff));
                assertNull(f.hub.admitClanWarehouseCommit(b,other,f.session.sourceClanIdentity("warehouseClan"),cutoff,5,false));
                try(var pin=f.hub.admitClanWarehouseCommit(a,f.session,f.session.sourceClanIdentity("warehouseClan"),cutoff,5,true)){
                    assertNotNull(pin);Thread.sleep(110);
                    f.hub.releaseClanWarehouse(f.session);f.hub.releaseClanWarehouse(a,f.session);
                    assertFalse(f.hub.claimClanWarehouse(b,other,System.nanoTime()+1_000_000_000L));
                    assertFalse(f.hub.claimClanWarehouse(a,f.session,System.nanoTime()+1_000_000_000L));
                    assertNull(f.hub.admitClanWarehouseCommit(b,other,f.session.sourceClanIdentity("warehouseClan"),System.nanoTime()+1_000_000_000L,4,true));
                }
                assertTrue(f.hub.claimClanWarehouse(b,other,System.nanoTime()+1_000_000_000L));
                f.hub.releaseClanWarehouse(other);
                try(var pin=f.hub.admitClanWarehouseCommit(b,other,f.session.sourceClanIdentity("warehouseClan"),System.nanoTime()+1_000_000_000L,4,false)){assertNotNull(pin);}
            }
        }
    }
    @Test void realClanAndElfDbScopesAndFeesRemainIndependent() throws Exception {
        try(Fixture f=new Fixture(temporary.resolve("clan"))){
            assertTrue(f.hub.createClan(f.session.sourceCharacterIdentity(),"actualClan"));
            long id=f.item(InventoryCatalog.NATIVE_HEALING_POTION).objectId();
            f.call("armWarehouseDeposits",f.npc.objectId());f.call("buySell",db(false,0,4,id,2));f.drain();
            assertEquals(2,f.state.clanWarehouse(f.session.sourceClanIdentity("actualClan")).inventory().get(InventoryCatalog.NATIVE_HEALING_POTION));
            f.call("sendWarehouseRetrieveList",5,f.npc.objectId());f.drain();
            Object list=get(f.session,"pendingClassicList");Method wm=list.getClass().getDeclaredMethod("warehouseModes");wm.setAccessible(true);
            Object capability=((Map<?,?>)wm.invoke(list)).get(5);Method contents=capability.getClass().getDeclaredMethod("contents");contents.setAccessible(true);
            RuntimeInventory stored=(RuntimeInventory)contents.invoke(capability);long storedId=stored.items().get(0).objectId();
            f.call("buySell",db(true,f.npc.objectId(),5,storedId,1));
            assertEquals(4,f.count(InventoryCatalog.NATIVE_HEALING_POTION));assertEquals(70,f.count(InventoryCatalog.ADENA));
            assertTrue(f.state.personalWarehouse(f.session.sourceAccountIdentity(),f.session.sourceCharacterIdentity()).inventory().isEmpty());
        }
        try(Fixture f=new Fixture(temporary.resolve("elf"),"test5")){
            assertEquals(2,f.accounts.characters("test5").get(0).type());
            assertTrue(f.inv().add(InventoryCatalog.require(InventoryCatalog.PURE_MITHRIL),10));
            long id=f.item(InventoryCatalog.NATIVE_HEALING_POTION).objectId();
            f.call("armWarehouseDeposits",f.npc.objectId());f.call("buySell",db(false,0,8,id,2));f.drain();
            f.call("sendWarehouseRetrieveList",9,f.npc.objectId());f.drain();
            Object list=get(f.session,"pendingClassicList");Method wm=list.getClass().getDeclaredMethod("warehouseModes");wm.setAccessible(true);
            Object capability=((Map<?,?>)wm.invoke(list)).get(9);Method contents=capability.getClass().getDeclaredMethod("contents");contents.setAccessible(true);
            long stored=((RuntimeInventory)contents.invoke(capability)).items().get(0).objectId();
            f.call("buySell",db(false,0,9,stored,1,stored,1));
            assertEquals(5,f.count(InventoryCatalog.NATIVE_HEALING_POTION));assertEquals(6,f.count(InventoryCatalog.PURE_MITHRIL));assertEquals(100,f.count(InventoryCatalog.ADENA));
        }
    }
    @Test void actualPasswordOneShotCannotArmAListWithoutMatchingAnswer() throws Exception {
        try(Fixture f=new Fixture(temporary)){
            long id=f.item(InventoryCatalog.NATIVE_HEALING_POTION).objectId();
            f.call("armWarehouseDeposits",f.npc.objectId());f.call("buySell",db(false,0,2,id,1));f.drain();
            f.state.changeWarehousePassword(f.session.sourceAccountIdentity(),0,123);
            f.call("requestWarehouseRetrieve",1,3,f.npc.objectId());f.drain();assertNull(get(f.session,"pendingClassicList"));
            f.call("warehouseControl",new Protocol.Writer().c(0x7E).c(1).d(124).d(f.npc.objectId()).bytes());f.drain();
            assertNull(get(f.session,"pendingWarehousePassword"));assertNull(get(f.session,"pendingClassicList"));
            f.call("warehouseControl",new Protocol.Writer().c(0x7E).c(1).d(123).d(f.npc.objectId()).bytes());assertNull(get(f.session,"pendingClassicList"));
            f.call("requestWarehouseRetrieve",1,3,f.npc.objectId());f.drain();
            f.call("warehouseControl",new Protocol.Writer().c(0x7E).c(1).d(123).d(f.npc.objectId()).bytes());
            assertNotNull(get(f.session,"nativeWarehouseAdmission"));assertNull(get(f.session,"pendingWarehousePassword"));f.drain();
        }
    }
    static Object get(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    static void set(Object o,String n,Object v)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
    static final class Fixture implements AutoCloseable {
        final ServerConfig config;final GameStateStore state;final Path statePath;
        final AccountRepository accounts;final GameHub hub;final CaptureLog capture;
        final ServerSocket listener;final Socket client,server;final ClientSession session;
        final Protocol.CipherState receiver=new Protocol.CipherState(0x38105671);
        final ClassicWorldNpcCatalog.StaticNpc npc;
        Fixture(Path root)throws Exception {this(root,"test");}
        Fixture(Path root,String account)throws Exception {
            Files.createDirectories(root);
            config=ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
            assertEquals("memory",config.databaseMode());
            accounts=AccountRepository.open(config);statePath=root.resolve("state.properties");state=new GameStateStore(statePath,true);
            hub=new GameHub(config,state);capture=new CaptureLog(root.resolve("capture"));
            listener=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));client=new Socket("127.0.0.1",listener.getLocalPort());server=listener.accept();client.setSoTimeout(50);
            session=new ClientSession(server,ClientSession.Endpoint.GAME,config,accounts,state,capture,hub);
            CharacterSummary selected=accounts.characters(account).get(0);
            set(session,"account",account);set(session,"selectedCharacter",selected.name());set(session,"characters",List.of(selected));set(session,"selectedClassicServerName",config.classicServerNames().get(0));
            set(session,"phase",Enum.valueOf((Class)get(session,"phase").getClass(),"WORLD"));
            set(session,"classicWorldMapId",4);set(session,"classicWorldInstanceId",-1);
            npc=ClassicWorldNpcCatalog.fixedWarehouses(4).get(0);set(session,"positionX",npc.x());set(session,"positionY",npc.y());set(session,"playerHp",100);set(session,"playerMaxHp",100);
            AtomicLong ids=new AtomicLong(0x76000000L);
            set(session,"inventory",RuntimeInventory.fromCatalogCounts(Map.of(InventoryCatalog.ADENA,100,InventoryCatalog.NATIVE_HEALING_POTION,5),ids::incrementAndGet));
            set(session,"sourceInventoryWeightStats",SourceInventoryWeightStats.none(100.0));
            set(session,"sourceEquipmentMagicStats",SourceEquipmentMagicStats.none());
            set(session,"sourcePhysicalGrowthStats",SourcePhysicalGrowthStats.classic381());
            set(session,"sourceEquipmentVitalStats",SourceEquipmentVitalStats.none());
            set(session,"sourceEquippedFireResistance",new SourceEquippedFireResistance(0,0));
            set(session,"sourceBaseMagicResistance",SourceBaseMagicResistance.from(selected));
            set(session,"outgoing",new Protocol.CipherState(0x38105671));
        }
        void call(String name,Object... args)throws Exception {
            Class<?>[] types=Arrays.stream(args).map(v->v instanceof Integer?int.class:v instanceof Long?long.class:v.getClass()).toArray(Class<?>[]::new);
            Method m=ClientSession.class.getDeclaredMethod(name,types);m.setAccessible(true);
            try{m.invoke(session,args);}catch(InvocationTargetException e){throw new RuntimeException(e.getCause());}
        }
        RuntimeInventory inv()throws Exception{return (RuntimeInventory)get(session,"inventory");}
        RuntimeInventory.Item item(long id)throws Exception{return inv().items().stream().filter(i->i.definition().catalogId()==id).findFirst().orElseThrow();}
        long count(long id)throws Exception{return inv().items().stream().filter(i->i.definition().catalogId()==id).mapToLong(RuntimeInventory.Item::quantity).sum();}
        List<byte[]> drain()throws Exception {List<byte[]> packets=new ArrayList<>();try{while(true)packets.add(receiver.decrypt(Protocol.readFrame(client.getInputStream(),8192)));}catch(SocketTimeoutException end){}return packets;}
        @Override public void close()throws Exception{server.close();client.close();listener.close();capture.close();hub.close();accounts.close();}
    }
}
