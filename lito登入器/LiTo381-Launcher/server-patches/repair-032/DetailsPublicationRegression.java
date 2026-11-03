package local.lito381.server;
import java.lang.reflect.*;import java.net.*;import java.nio.file.*;import java.util.*;
public class DetailsPublicationRegression {
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  Path tmp=Files.createTempDirectory("lito-buff-031-");
  ServerConfig config=ServerConfig.load(Path.of("config/server.properties"));
  GameStateStore state=new GameStateStore(tmp.resolve("state.properties"),false);GameHub hub=new GameHub(config);
  try(ServerSocket listener=new ServerSocket(0,2,InetAddress.getByName("127.0.0.1"));Socket client=new Socket("127.0.0.1",listener.getLocalPort());Socket server=listener.accept();Socket otherClient=new Socket("127.0.0.1",listener.getLocalPort());Socket otherServer=listener.accept()){
   ClientSession caster=new ClientSession(server,ClientSession.Endpoint.GAME,config,null,state,new CaptureLog(tmp.resolve("capture")),hub);
   ClientSession other=new ClientSession(otherServer,ClientSession.Endpoint.GAME,config,null,state,new CaptureLog(tmp.resolve("other")),hub);
   initialize(caster,character("Clanless",2,11554));initialize(other,character("Stranger",2,11555));
   setField(caster,"outgoing",new Protocol.CipherState(123));setField(other,"outgoing",new Protocol.CipherState(124));
   setField(caster,"learnedSkills",Set.of(26,42,48));
   System.setProperty("lito.extendedItemDetails","true");
   var armor=new InventoryCatalog.NativeArmorStatus(10,1,1000,127,6,200,300,400,500,600,700,100000,80000,90000,1000,true,201,202,203,204,205,206,207,208,209,210,9);
   var def=new InventoryCatalog.Definition(88888,23,1,1,0,true,"GM戒指","說明測試",1,1,false,false,0,0,InventoryCatalog.Effect.NONE,0,1000,0,null,armor);
   RuntimeInventory live=(RuntimeInventory)field(caster,"inventory");live.add(def,1);var item=live.items().get(0);
   byte[] original=Protocol.inventoryBatch(List.of(item.wireItem()));
   invoke(caster,"sendEncrypted",new Class[]{byte[].class,String.class},original,"extended inventory test");
   client.setSoTimeout(2000);var decoder=new Protocol.CipherState(123);
   byte[] first=decoder.decrypt(Protocol.readFrame(client.getInputStream(),65535));check(Arrays.equals(original,first),"original inventory frame unchanged");
   byte[] second=decoder.decrypt(Protocol.readFrame(client.getInputStream(),65535));check(Arrays.equals(LiToItemDetails.packet(item.objectId(),LiToItemDetails.describe(item)),second),"actual encrypted sidecar frame lossless");
   System.out.println("PASS: actual inventoryBatch -> synchronized encrypted frames -> client decrypt, legacy record unchanged and complete wide item detail follows");

  }
 }
    private static void initialize(ClientSession session,
                                   CharacterSummary character) throws Exception {
        setField(session, "account", character.name() + "-account");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "inventory", new RuntimeInventory());
        setField(session, "sourceEquipmentMagicStats",
                SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats",
                SourceEquipmentVitalStats.none());
        setField(session, "sourceEquippedFireResistance",
                new SourceEquippedFireResistance(0, 0));
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(character));
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(100.0));
        setField(session, "retainedSourceInventoryWeight", 0);
    }

    private static void prepareBattle(ClientSession session, long roomId,
                                      int x, int y) throws Exception {
        setPhase(session, "BATTLE");
        setField(session, "currentRoomId", roomId);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 500);
        setField(session, "playerMaxHp", 500);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static void prepareWorld(ClientSession session, int mapId,
                                     int x, int y) throws Exception {
        setPhase(session, "WORLD");
        setField(session, "classicWorldMapId", mapId);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 500);
        setField(session, "playerMaxHp", 500);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static CharacterSummary character(String name, int type,
                                              long objectId) {
        return new CharacterSummary(name, "", type, 0, 50, 500, 100,
                10, 12, 12, 12, 12, 12, 12, 12, 12,
                objectId, 100, 0, 0, 0, 32740, 32798, 4,
                12, 40, 1, 12, 12, 0);
    }

    private static int invokeInt(Object target, String name,
                                 Class<?>[] parameters, Object... arguments)
            throws Exception {
        return (int) invoke(target, name, parameters, arguments);
    }

    private static int invokeInt(Object target, String name) throws Exception {
        return invokeInt(target, name, new Class<?>[0]);
    }

    private static Object invoke(Object target, String name,
                                 Class<?>[] parameters, Object... arguments)
            throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method.invoke(target, arguments);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setPhase(ClientSession session, String value)
            throws Exception {
        Class phaseType = Class.forName(ClientSession.class.getName() + "$Phase");
        setField(session, "phase", Enum.valueOf(phaseType, value));
    }
}

