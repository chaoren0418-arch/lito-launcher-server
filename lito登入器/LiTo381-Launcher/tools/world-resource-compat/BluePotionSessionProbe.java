package local.lito381.server;

import java.lang.reflect.*;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Real use-item branch and encrypted output, using a memory store and loopback
 * socket fixture only. No Main, SQL repositories, live battle or source writes.
 */
public final class BluePotionSessionProbe {
    private static void set(Object target,String name,Object value)throws Exception{
        Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);
    }
    private static Object get(Object target,String name)throws Exception{
        Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void phase(Object session,String name)throws Exception{
        Field f=session.getClass().getDeclaredField("phase");f.setAccessible(true);
        f.set(session,Enum.valueOf((Class)f.getType(),name));
    }
    private static void run(Path folder,long catalogId,int expectedSeconds)throws Exception {
        Files.createDirectories(folder);
        Path configPath=folder.resolve("server.properties");
        Files.writeString(configPath,"bind=127.0.0.1\nport=32100\nserver.name=Presentation Fixture\n"
            +"database.mode=memory\ndatabase.read.only=true\nmemory.account=test\nmemory.password=test\n"
            +"state.persistence.enabled=false\nstate.file="+folder.resolve("state.properties").toString().replace('\\','/')+"\n"
            +"capture.directory="+folder.resolve("capture").toString().replace('\\','/')+"\n"
            +"battle.ai.enabled=false\nbattle.natural.hp.regen.enabled=false\n");
        ServerConfig config=ServerConfig.load(configPath);
        GameHub hub=new GameHub(config);
        GameStateStore state=new GameStateStore(folder.resolve("state.properties"),false);
        CaptureLog capture=new CaptureLog(folder.resolve("capture"));
        try(ServerSocket listener=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));
            Socket client=new Socket("127.0.0.1",listener.getLocalPort());
            Socket server=listener.accept()){
            client.setSoTimeout(200);
            ClientSession session=new ClientSession(server,ClientSession.Endpoint.GAME,config,null,state,capture,hub);
            int seed=0x38108800;
            set(session,"outgoing",new Protocol.CipherState(seed));
            Protocol.CipherState receiver=new Protocol.CipherState(seed);
            phase(session,"BATTLE");
            set(session,"account","test");set(session,"selectedCharacter","Presentation");
            set(session,"selectedClassicServerName","SelfTest");
            CharacterSummary character=new CharacterSummary("Presentation","",1,0,80,
                300,100,0,14,14,12,12,12,12,14,14,0x72800001L,0,0,0,0,
                32768,32768,0,8,40,1,8,8,0);
            set(session,"characters",List.of(character));
            set(session,"sourceBaseMagicResistance",SourceBaseMagicResistance.from(character));
            set(session,"sourceEquipmentMagicStats",SourceEquipmentMagicStats.none());
            set(session,"sourceEquipmentVitalStats",SourceEquipmentVitalStats.none());
            set(session,"sourcePhysicalGrowthStats",SourcePhysicalGrowthStats.classic381());
            set(session,"sourceInventoryWeightStats",SourceInventoryWeightStats.none(1.0));
            set(session,"playerHp",300);set(session,"playerMaxHp",300);
            set(session,"playerMp",100);set(session,"playerMaxMp",100);
            set(session,"playerClassicLevel",80);
            AtomicLong ids=new AtomicLong(0x72801000L);
            RuntimeInventory inventory=RuntimeInventory.fromCatalogCounts(Map.of(catalogId,2),ids::incrementAndGet);
            set(session,"inventory",inventory);
            long object=inventory.items().get(0).objectId();
            byte[] use=ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
                .put((byte)Protocol.C_USE_ITEM).putInt((int)object).put((byte)0).array();
            Method method=ClientSession.class.getDeclaredMethod("useInventoryItemBeforeTravel",byte[].class);
            method.setAccessible(true);
            try {method.invoke(session,(Object)use);}
            catch(InvocationTargetException e){throw new AssertionError("use-item branch failed",e.getCause());}
            if(inventory.catalogCounts().get(catalogId)!=1)throw new AssertionError("consumption changed");
            BattleTimedStatus status=(BattleTimedStatus)get(session,"timedStatus");
            if(!status.bluePotionActive(System.nanoTime()))throw new AssertionError("authoritative status missing");
            List<byte[]> packets=new ArrayList<>();
            while(true){
                try{
                    byte[] encrypted=Protocol.readFrame(client.getInputStream(),65535);
                    if(encrypted==null)break;
                    packets.add(receiver.decrypt(encrypted));
                }catch(SocketTimeoutException done){break;}
            }
            byte[] expected=SharedPresentationPackets.bluePotion(expectedSeconds);
            long icons=packets.stream().filter(p->Arrays.equals(p,expected)).count();
            if(icons!=1)throw new AssertionError("expected one icon, found "+icons);
            if(packets.stream().anyMatch(p->p.length==4 && (p[0]&255)==250 && p[1]==34))
                throw new AssertionError("old incompatible packet emitted");
            System.out.println("PASS blue-potion item="+catalogId+" seconds="+expectedSeconds
                +" consumed=1 active=true encrypted-icon=1");
        } finally {capture.close();hub.close();}
    }
    public static void main(String[] args)throws Exception{
        Path folder=Path.of(args[0]);
        run(folder.resolve("normal"),InventoryCatalog.BLUE_POTION,600);
        run(folder.resolve("blessed"),InventoryCatalog.BLESSED_BLUE_POTION,700);
    }
}
