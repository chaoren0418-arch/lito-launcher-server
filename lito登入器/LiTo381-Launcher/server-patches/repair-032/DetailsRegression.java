package local.lito381.server;
import java.util.*;import java.nio.charset.Charset;
public class DetailsRegression {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] a)throws Exception {
  var armor=new InventoryCatalog.NativeArmorStatus(10,1,1000,127,6,200,300,400,500,600,700,100000,80000,90000,1000,true,201,202,203,204,205,206,207,208,209,210,9);
  var def=new InventoryCatalog.Definition(88888,23,1,1,0,true,"GM戒指","說明測試",1,1,false,false,0,0,InventoryCatalog.Effect.NONE,0,1000,0,null,armor);
  RuntimeInventory inv=new RuntimeInventory();inv.add(def,1);var item=inv.items().get(0);String text=LiToItemDetails.describe(item);
  check(text.contains("魔力上限: +80000"),"MP not signed byte clamp");check(text.contains("魔法防禦: +90000"),"MR not signed short clamp");check(text.contains("魅力: +700"),"ability not signed byte clamp");check(text.contains("昏迷耐性: +210"),"late rows retained");check(text.lines().count()>20,"more than 20 lines");
  byte[] packet=LiToItemDetails.packet(item.objectId(),text);String wire=new String(packet,2,packet.length-3,Charset.forName("MS950"));String decoded=new String(HexFormat.of().parseHex(wire.substring(wire.indexOf(':',10)+1)),Charset.forName("MS950"));check(decoded.equals(text),"lossless complete description packet");
  System.out.println("PASS: GM ring >20 lines, STR/MP/MR/resistance full values and complete Big5 wire roundtrip");
 }
}
