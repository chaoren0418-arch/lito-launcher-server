package local.lito381.server;
import java.nio.file.*;import java.sql.*;import java.util.*;
public class InspectBattleStats {
 public static void main(String[] args)throws Exception{
  ServerConfig config=ServerConfig.load(Path.of(args[0]));Properties p=SharedDatabase.properties(config.legacySqlProperties());
  try(Connection c=DriverManager.getConnection(p.getProperty("URL1_LOGIN")+"380_ac_1"+p.getProperty("URL3_LOGIN",""),p.getProperty("Login_LOGIN",p.getProperty("Login")),p.getProperty("Password_LOGIN",p.getProperty("Password","")))){
   c.setReadOnly(true);
   for(String sql:List.of("SELECT c.objid,c.char_name,c.account_name,t.wins,t.losses,t.version,t.updated_at FROM characters c LEFT JOIN character_lito t ON t.char_obj_id=c.objid WHERE c.account_name='2222' OR c.char_name='2222'", "SELECT TABLE_SCHEMA,ENGINE FROM information_schema.TABLES WHERE TABLE_NAME='character_lito' AND TABLE_SCHEMA IN ('380_ac','380_ac_1','380_ac_2')"))
   try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){while(r.next()){for(int i=1;i<=r.getMetaData().getColumnCount();i++)System.out.print(r.getMetaData().getColumnLabel(i)+"="+r.getString(i)+" ");System.out.println();}}
  }
  try(AccountRepository repo=AccountRepository.open(config)){
   var selected=repo.repositoryForClassicSource(config.classicServerNames().get(1));var roster=selected.tournamentProfiles();
   System.out.println("selected_repository="+selected.getClass().getName()+" authoritative="+roster.authoritative()+" profiles="+roster.profiles().size());
   for(var row:roster.profiles())if(row.characterName().equals("2222"))System.out.println("repository character="+row.characterName()+" id="+row.characterObjectId()+" wins="+row.state().wins()+" losses="+row.state().losses()+" version="+row.snapshotVersion());
  }
 }
}
