package local.lito381.server;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.charset.Charset;
import java.util.*;
/** Versioned LiTo-only, lossless text alongside the bounded legacy status. */
final class LiToItemDetails {
 static final Charset BIG5=Charset.forName("MS950");
 static final String PREFIX="LITOITEM1:";
 static void row(StringBuilder b,String name,long value){if(value!=0)b.append(name).append(": ").append(value>0?"+":"").append(value).append('\n');}
 static String describe(RuntimeInventory.Item item) {
  var d=item.definition();StringBuilder b=new StringBuilder(item.nativeViewName()).append('\n');
  if(!item.identified())return b.append("尚未鑑定").toString();
  var a=d.sourceArmorStatus();var w=d.effectiveWeaponStatus();var p=InventoryCatalog.nativeProjectileStatus(d.catalogId());
  if(a!=null){
   b.append("防禦力: ").append(-a.displayArmorClass()).append('\n');
   row(b,"力量",a.strength());row(b,"敏捷",a.dexterity());row(b,"體質",a.constitution());row(b,"精神",a.wisdom());row(b,"智力",a.intelligence());row(b,"魅力",a.charisma());
   row(b,"體力上限",a.hitPoints());row(b,"魔力上限",a.manaPoints());row(b,"魔法防禦",a.magicResistance());row(b,"魔攻",a.spellPower());
   row(b,"火屬性防禦",a.fireResistance());row(b,"水屬性防禦",a.waterResistance());row(b,"風屬性防禦",a.windResistance());row(b,"地屬性防禦",a.earthResistance());
   row(b,"暗黑耐性",a.blindResistance());row(b,"睡眠耐性",a.sleepResistance());row(b,"冰凍耐性",a.freezeResistance());row(b,"石化耐性",a.stoneResistance());row(b,"支撐耐性",a.sustainResistance());row(b,"昏迷耐性",a.stunResistance());
   if(a.haste())b.append("加速效果\n");
   b.append("安全強化: ").append(a.safeEnchant()).append('\n');
   classes(b,a.classMask());
  }else if(d.projectile() && p!=null){b.append("攻擊力 (小型/大型): ").append(p.damageSmall()).append('/').append(p.damageLarge()).append('\n');}
  else if(w!=null){b.append("攻擊力 (小型/大型): ").append(w.damageSmall()).append('/').append(w.damageLarge()).append('\n');row(b,"攻擊命中",w.hitModifier());row(b,"額外傷害",w.damageModifier());b.append("安全強化: ").append(w.safeEnchant()).append('\n');classes(b,w.classMask());}
  if(item.enchantLevel()!=0)row(b,"強化值",item.enchantLevel());
  if(item.durability()!=0)row(b,"損壞度",item.durability());
  if(d.maximumChargeCount()>0)b.append("剩餘次數: ").append(item.chargeCount()).append('\n');
  b.append("數量: ").append(item.quantity()).append('\n');
  b.append("重量: ").append((long)d.unitWeight()*item.quantity()/1000L).append('\n');
  // Retained description is plain source text; never derive values from truncated wire bytes.
  if(d.detail()!=null && !d.detail().isBlank())b.append(d.detail()).append('\n');
  return b.toString();
 }
 static void classes(StringBuilder b,int mask){String[] n={"王族","騎士","妖精","法師","黑暗妖精","龍騎士","幻術士"};b.append("可用職業: ");for(int i=0;i<n.length;i++)if((mask&(1<<i))!=0)b.append(n[i]).append(' ');b.append('\n');}
 static byte[] packet(long id,String text){
  byte[] bytes=text.getBytes(BIG5);
  if(bytes.length>8192)throw new IllegalArgumentException("LiTo item detail exceeds 8192 bytes");
  return new Protocol.Writer().c(Protocol.S_MESSAGE).c(254).s(PREFIX+Long.toUnsignedString(id,16)+":"+HexFormat.of().formatHex(bytes)).bytes();
 }
 static String describe(ClientSession session,RuntimeInventory.Item item)throws ReflectiveOperationException {
  if(!item.identified())return "";
  var repository=(AccountRepository)SOURCE.invoke(session);
  var template=LiToItemTemplates.get(repository,item.definition().catalogId());
  String custom=LiToItemTemplates.custom(repository,item.definition().catalogId());
  if(template!=null && Set.of("armor","weapon").contains(template.get("kind")))return LiToItemTemplates.needsExtension(template)||!custom.isEmpty()?LiToItemTemplates.describe(item,template)+custom:"";
  if(!custom.isEmpty())return describe(item)+custom;
  var a=item.definition().sourceArmorStatus();
  if(a==null)return "";
  String text=describe(item);
  return text.lines().count()>19 || Math.abs((long)a.hitPoints())>32767 || Math.abs((long)a.manaPoints())>32767 || Math.abs((long)a.magicResistance())>127 || Math.abs((long)a.spellPower())>127 ? text:"";
 }
 static Field INVENTORY,SHOP;static Method SEND,OFFERS,SOURCE;
 static {try{INVENTORY=ClientSession.class.getDeclaredField("inventory");INVENTORY.setAccessible(true);SEND=ClientSession.class.getDeclaredMethod("sendEncryptedFrame",byte[].class,String.class);SEND.setAccessible(true);SHOP=ClientSession.class.getDeclaredField("pendingTournamentShop");SHOP.setAccessible(true);OFFERS=Class.forName("local.lito381.server.ClientSession$PendingTournamentShop").getDeclaredMethod("offers");OFFERS.setAccessible(true);SOURCE=ClientSession.class.getDeclaredMethod("selectedSourceAccounts");SOURCE.setAccessible(true);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
 static void publish(ClientSession session,byte[] original)throws IOException {
  if(!Boolean.getBoolean("lito.extendedItemDetails") || original.length==0)return;
  int op=original[0]&255;
  if(op!=Protocol.S_ADD_INVENTORY && op!=Protocol.S_ADD_INVENTORY_BATCH && op!=Protocol.S_CHANGE_ITEM_DESC_EX && op!=Protocol.S_CHANGE_ITEM_DESC && op!=Protocol.S_REMOVE_INVENTORY && op!=Protocol.S_LITO_BUY_LIST)return;
  try{
   if(op==Protocol.S_LITO_BUY_LIST){
    Object shop=SHOP.get(session);if(shop==null)return;
    for(Object raw:(List<?>)OFFERS.invoke(shop)) {
     var offer=(TournamentShopCatalog.Offer)raw;var d=offer.definition();
     SEND.invoke(session,packet(d.catalogId(),describe(session,new RuntimeInventory.Item(d.catalogId(),d,1,false))),"S_LITO_ITEM_DETAIL shop="+d.catalogId());
    }
    return;
   }
   RuntimeInventory inventory=(RuntimeInventory)INVENTORY.get(session);if(inventory==null)return;
   long id=original.length>=5?((original[1]&255L)|((original[2]&255L)<<8)|((original[3]&255L)<<16)|((original[4]&255L)<<24)):0;
   if(op==Protocol.S_REMOVE_INVENTORY){SEND.invoke(session,packet(id,""),"S_LITO_ITEM_DETAIL remove="+id);return;}
   for(var item:inventory.items())if(op==Protocol.S_ADD_INVENTORY_BATCH || item.objectId()==id)
    SEND.invoke(session,packet(item.objectId(),describe(session,item)),"S_LITO_ITEM_DETAIL v1 object="+item.objectId());
  }catch(InvocationTargetException e){if(e.getCause() instanceof IOException io)throw io;throw new IOException("LiTo item detail publication",e.getCause());}catch(ReflectiveOperationException e){throw new IOException(e);}
 }
}
