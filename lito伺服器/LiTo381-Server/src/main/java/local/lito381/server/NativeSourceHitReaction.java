package local.lito381.server;
import java.sql.*;import java.util.*;
/** Source NPC-gfx control, loaded only in the already owned readonly startup transaction. */
final class NativeSourceHitReaction {
 static final String SELECT="SELECT `gfx_id` FROM `系統_受擊無動作gfx`";
 private static volatile Set<Integer> rows;
 static void read(Connection connection)throws SQLException{
  if(!connection.isReadOnly()||connection.getAutoCommit())throw new SQLException("hit reaction controls require the existing readonly startup transaction");
  var next=new HashSet<Integer>();
  try(var statement=connection.prepareStatement(SELECT);var result=statement.executeQuery()){
   while(result.next())next.add(result.getInt("gfx_id"));
  }catch(SQLException failure){
   // Source load clears first, keeps rows read before SQLException and continues startup.
   System.getLogger(NativeSourceHitReaction.class.getName()).log(System.Logger.Level.WARNING,"Source hit reaction control load failed",failure);
  }
  rows=Set.copyOf(next);
 }
 static boolean shouldSendDamageAction(int gfx){var snapshot=rows;return snapshot==null?BattleHitReactionSuppressCatalog.nativeSourceHitReactionOriginalShouldSend(gfx):!snapshot.contains(gfx);}
 static Set<Integer> suppressedGfx(){var snapshot=rows;return snapshot==null?BattleHitReactionSuppressCatalog.nativeSourceHitReactionOriginalSuppressedGfx():snapshot;}
 private NativeSourceHitReaction(){}
}
