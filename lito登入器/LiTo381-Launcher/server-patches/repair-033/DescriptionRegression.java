package local.lito381.server;
import java.nio.file.*;import java.net.*;import java.lang.reflect.*;import java.util.*;import java.nio.charset.*;
public class DescriptionRegression {
 static void check(boolean b,String label){if(!b)throw new AssertionError(label);}
 static void set(Object o,String name,Object value)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
 static Object call(Object o,String name,Class<?>[] types,Object...v)throws Exception{var m=o.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(o,v);}
 static RuntimeInventory.Item item(long id){var a=new InventoryCatalog.NativeArmorStatus(0,1,1000,127,-1,0,0,0,0,0,99,999999,999999,0,0,false,0,0,0,0,0,0,0,0,0,0,9);var d=new InventoryCatalog.Definition(id,23,87,21,0,true,"GM戒指","",0,1,false,false,0,0,InventoryCatalog.Effect.NONE,0,1000,0,null,a);return new RuntimeInventory.Item(0x70003300+id,d,1,false);}
 public static void main(String[] args)throws Exception{
  System.setProperty("lito.extendedItemDetails","true");var config=ServerConfig.load(Path.of(args[0]));var tmp=Files.createTempDirectory("lito-detail-033-");
  try(var accounts=AccountRepository.open(config);var hub=new GameHub(config);var listener=new ServerSocket(0,1,InetAddress.getLoopbackAddress());var client=new Socket(InetAddress.getLoopbackAddress(),listener.getLocalPort());var socket=listener.accept()){
   var session=new ClientSession(socket,ClientSession.Endpoint.GAME,config,accounts,new GameStateStore(tmp.resolve("state"),false),new CaptureLog(tmp.resolve("capture")),hub);
   set(session,"selectedClassicServerName",config.classicServerNames().get(0));set(session,"outgoing",new Protocol.CipherState(123));
   var selected=(AccountRepository)call(session,"selectedSourceAccounts",new Class[]{});
   check(LiToItemTemplates.CACHE.containsKey(selected),"real source selection preloads templates before sending");
   String one=LiToItemDetails.describe(session,item(88889));String two=LiToItemDetails.describe(session,item(88890));String four=LiToItemDetails.describe(session,item(88892));
   check(one.contains("\\f0擴充1行")&&one.contains("\\fK擴充21行"),"GM1 all 21 configured rows and colors");
   check(two.contains("\\fL擴充22行")&&two.contains("\\f@擴充42行"),"GM2 remaining 21 rows and colors");
   check(one.contains("體力上限 +999999")&&one.contains("魔力上限 +999999")&&one.contains("魔法防禦 +999"),"raw wide template values");
   check(two.contains("防禦力 -999")&&!two.contains("防禦力 -255"),"AC fetched before native byte clamp");check(four.contains("魔攻 +888"),"SP full width");
   check(!one.contains("安全強化: -1")&&!one.contains("數量:"),"no synthetic debug/invalid accessory rows");
   var inv=new RuntimeInventory();var it=item(88889);inv.add(it.definition(),1);set(session,"inventory",inv);it=inv.items().get(0);
   byte[] original=Protocol.inventoryBatch(List.of(it.wireItem()));call(session,"sendEncrypted",new Class[]{byte[].class,String.class},original,"033 SQL tooltip");
   client.setSoTimeout(3000);var cipher=new Protocol.CipherState(123);check(Arrays.equals(original,cipher.decrypt(Protocol.readFrame(client.getInputStream(),65535))),"legacy encrypted inventory unchanged");
   byte[] side=cipher.decrypt(Protocol.readFrame(client.getInputStream(),65535));check(Arrays.equals(side,LiToItemDetails.packet(it.objectId(),LiToItemDetails.describe(session,it))),"complete custom rows on actual encrypted publication");
   var raw=new HashMap<>(LiToItemTemplates.get(selected,88889));raw.put("add_adena_rate","125");raw.put("add_hpr","-2000");raw.put("add_pvp_redux","80000");String full=LiToItemTemplates.describe(it,raw);check(full.contains("金幣獲取 +125%")&&full.contains("HP回復 -2000")&&full.contains("PVP減免 +80000"),"extended signed fields and rate units");
   var snapshot=LiToItemTemplates.CACHE.get(selected);var withGlobal=new HashMap<>(snapshot);withGlobal.put(-1L,Map.of("_custom","\\fY全域說明\n"));LiToItemTemplates.CACHE.put(selected,withGlobal);check(LiToItemTemplates.custom(selected,88889).startsWith("\\fY全域說明\n\\f0擴充1行"),"source1 global then specific order");LiToItemTemplates.CACHE.put(selected,snapshot);
   var plain=Map.of("kind","armor","ac","-2","add_hp","10");check(!LiToItemTemplates.needsExtension(plain),"ordinary bounded tooltip stays native");
   Files.writeString(Path.of(args[1]),one+"\n--- GM2 ---\n"+two+"\n--- GM4 ---\n"+four,StandardCharsets.UTF_8);
   System.out.println("PASS: actual SQL custom descriptions (42 colored rows), global-before-specific order, full HP/MP/MR/AC/SP, signed extras, original encrypted inventory plus complete publication, ordinary native fallback");
  }
 }
}
