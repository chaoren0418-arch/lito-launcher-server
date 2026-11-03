package local.lito381.server;
import java.nio.file.*;import java.nio.charset.*;import java.sql.*;import java.util.*;
public class EnableCommands {
 public static void main(String[] a)throws Exception{
  var config=ServerConfig.load(Path.of(a[0]));try(var accounts=AccountRepository.open(config)){
   var r=accounts.repositoryForClassicSource(config.classicServerNames().get(0));String schema=(String)LiToGmCommands.get(r,"resourceDatabase");
   if(!schema.equals("lito_play_20261011_static"))throw new IllegalStateException("not the writable isolated static copy");
   StringBuilder backup=new StringBuilder("schema="+schema+"\n");for(String n:List.of("item","spawn"))backup.append(n).append('=').append(r.gmCommand(n)).append('\n');
   Files.writeString(Path.of(a[1]),backup,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
   try(var c=LiToGmCommands.connection(r)){c.setReadOnly(false);c.setAutoCommit(false);
    try{for(String[] row:new String[][]{{"item","L1CreateItem","建立道具：ID或名稱 [數量] [強化]"},{"spawn","L1SpawnCmd","建立戰鬥NPC：ID或名稱 [數量] [範圍]"}}){
     try(var q=c.prepareStatement("INSERT INTO "+LiToGmCommands.table(r,"commands")+" (name,access_level,class_name,note,system) SELECT ?,200,?,?,0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "+LiToGmCommands.table(r,"commands")+" WHERE name=?)")){q.setString(1,row[0]);q.setString(2,row[1]);q.setString(3,row[2]);q.setString(4,row[0]);q.executeUpdate();}
    }c.commit();}catch(Exception ex){c.rollback();throw ex;}
   }
   for(String n:List.of("item","spawn"))System.out.println(r.gmCommand(n));
  }
 }
}
