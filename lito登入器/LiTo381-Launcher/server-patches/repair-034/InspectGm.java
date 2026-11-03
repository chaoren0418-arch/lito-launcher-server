package local.lito381.server;
import java.nio.file.*;import java.sql.*;
public class InspectGm {
 public static void main(String[] a)throws Exception{
  var config=ServerConfig.load(Path.of(a[0]));try(var accounts=AccountRepository.open(config)){
   var r=accounts.repositoryForClassicSource(config.classicServerNames().get(0));
   for(String name:new String[]{"item","spawn"})System.out.println(r.gmCommand(name));
   try(var c=LiToGmCommands.connection(r);var st=c.createStatement();var rows=st.executeQuery("SELECT npcid,name,nameid,impl,gfxid,lvl,hp,mp FROM "+LiToGmCommands.table(r,"npc")+" WHERE npcid IN (45001,45002,45003,45161,45601)")){while(rows.next())System.out.println("NPC "+rows.getInt(1)+" "+rows.getString(2)+" nameid="+rows.getString(3)+" impl="+rows.getString(4)+" gfx="+rows.getInt(5)+" level="+rows.getInt(6)+" hp="+rows.getInt(7)+" mp="+rows.getInt(8));}
   for(String id:new String[]{"40743","88889","40010"}){try(var c=LiToGmCommands.connection(r)){var d=(InventoryCatalog.Definition)LiToGmCommands.call(r,"rewardItemDefinition",new Class[]{Connection.class,int.class},c,Integer.parseInt(id));System.out.println("ITEM "+id+" "+(d==null?"missing":d.name()+" stack="+d.stackable()));}}
  }
 }
}
