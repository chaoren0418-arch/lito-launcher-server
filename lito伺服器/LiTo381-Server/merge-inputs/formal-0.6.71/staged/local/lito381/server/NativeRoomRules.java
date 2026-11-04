// ORIGINAL SOURCE aligned to base-0.6.71.jar (formal behavior restored from reference JAR)
package local.lito381.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

final class NativeRoomRules {
   private static final Map<GameHub, NativeRoomRules.Policy> POLICIES = Collections.synchronizedMap(new WeakHashMap<>());

   static Object get(Object var0, String var1) {
      return DurableBattleSettlement.get(var0, var1);
   }

   static Object call(Object var0, String var1, Class<?>[] var2, Object... var3) {
      return DurableBattleSettlement.call(var0, var1, var2, var3);
   }

   static NativeRoomRules.Policy read(ServerConfig var0) throws Exception {
      Properties var1 = UnifiedConfiguration.read(var0.source());
      String var2 = SharedDatabase.identifier(var1.getProperty("database.lito.database"));

      NativeRoomRules.Policy var8;
      try (
         Connection var3 = DriverManager.getConnection(
            var1.getProperty("database.jdbc.base.url") + var2 + var1.getProperty("database.jdbc.options"),
            var1.getProperty("database.user"),
            var1.getProperty("database.password")
         );
         Statement var4 = var3.createStatement();
         ResultSet var5 = var4.executeQuery("SELECT policy_id,test_room_enabled,test_bot_enabled FROM lito_room_policy");
      ) {
         if (!var5.next() || var5.getInt(1) != 1) {
            throw new IllegalStateException("lito_room_policy requires policy_id=1");
         }

         int var6 = var5.getInt(2);
         int var7 = var5.getInt(3);
         if (var6 != 0 && var6 != 1 || var7 != 0 && var7 != 1 || var5.next()) {
            throw new IllegalStateException("invalid lito_room_policy; expected one row with boolean flags");
         }

         var8 = new NativeRoomRules.Policy(var6 == 1, var7 == 1);
      }

      return var8;
   }

   static void install(GameHub var0, ServerConfig var1, AccountRepository var2) {
      try {
         NativeRoomRules.Policy var3 = !(var2 instanceof MemoryAccountRepository) && var2 != null ? read(var1) : new NativeRoomRules.Policy(true, true);
         if (var2 != null && !(var2 instanceof MemoryAccountRepository)) {
            ResourceDatabase.load(var1);
         }

         POLICIES.put(var0, var3);
      } catch (Exception var4) {
         throw new IllegalStateException("Cannot load LiTo room policy or qualified resource tables; inspect the nested database error", var4);
      }
   }

   static NativeRoomRules.Policy policy(GameHub var0) {
      NativeRoomRules.Policy var1 = POLICIES.get(var0);
      if (var1 == null) {
         throw new IllegalStateException("room policy not initialized");
      } else {
         return var1;
      }
   }

   static boolean enabled(GameHub var0) {
      return policy(var0).testRoomEnabled();
   }

   static boolean testRoom(GameHub var0, long var1) {
      if (var1 == 0L) {
         return false;
      } else {
         synchronized (var0) {
            for (Object var5 : ((Map)get(var0, "channels")).values()) {
               if ((Long)get(var5, "defaultRoomId") == var1) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   static boolean bot(BattleRules.Mode var0, GameHub var1, long var2) {
      return var0 == BattleRules.Mode.DEATHMATCH && policy(var1).testBotEnabled() && testRoom(var1, var2);
   }

   static boolean bot(BattleRules.Mode var0, ClientSession var1) {
      return bot(var0, (GameHub)get(var1, "hub"), (Long)get(var1, "currentRoomId"));
   }

   static boolean activeBot(GameHub var0, long var1) {
      synchronized (var0) {
         return bot(var0.roomRules(var1).mode(), var0, var1) && var0.roomCombatantCount(var1) == 1 && var0.roomVirtualPlayers(var1).isEmpty();
      }
   }

   static boolean activeBot(ClientSession var0) {
      return activeBot((GameHub)get(var0, "hub"), (Long)get(var0, "currentRoomId"));
   }

   static int botHp(Object var0, GameHub var1) {
      return activeBot(var1, (Long)get(var0, "id")) ? 100 : 0;
   }

   static int botSingle(GameHub var0, long var1) {
      return activeBot(var0, var1) ? 1 : -1;
   }

   static String refusal(GameHub var0, Object var1, List<ClientSession> var2) {
      BattleRules.MatchState var3 = (BattleRules.MatchState)get(var1, "match");
      BattleRules.RuleSet var4 = var3.rules();
      long var5 = (Long)get(var1, "id");
      if (!var4.mode().isBattleScene()) {
         return "此模式不是戰鬥房。";
      } else {
         int var7 = var2.size();
         if (activeBot(var0, var5)) {
            Map var14 = (Map)get(var1, "forces");
            return Integer.valueOf(1).equals(var14.get(var2.get(0))) ? "無法開始：單人測試請選紅隊，ArenaBot 位於藍隊。" : "";
         } else {
            int var8 = Math.max(var4.minimumPlayers(), var4.teamBased() ? 2 : 1);
            if (var7 < var8) {
               return "無法開始：至少需要 " + var8 + " 位參戰玩家，目前 " + var7 + " 位（觀戰者與假人不計）。";
            } else {
               if (var4.teamBased()) {
                  Map var9 = (Map)get(var1, "forces");
                  int var10 = 0;
                  int var11 = 0;

                  for (ClientSession var13 : var2) {
                     if (Integer.valueOf(1).equals(var9.get(var13))) {
                        var11++;
                     } else {
                        var10++;
                     }
                  }

                  if (var10 == 0 || var11 == 0) {
                     return "無法開始：紅隊與藍隊都必須有參戰玩家。";
                  }

                  GameHub.RoomSettings var15 = (GameHub.RoomSettings)get(var1, "settings");
                  if ((var15.attributes() & 32) != 0 && var10 != var11) {
                     return "無法開始：此房間要求雙方人數一致。";
                  }

                  if (var4.mode() == BattleRules.Mode.SIEGE_WAR
                     && (Boolean)get(var0, "siegeRosterEnforced")
                     && (var10 < 15 || var11 < 15 || var10 > 50 || var11 > 50)) {
                     return "無法開始：攻城模式每隊需要 15～50 位參戰玩家。";
                  }
               }

               return "";
            }
         }
      }
   }

   static boolean rosterReady(GameHub var0, Object var1, List<ClientSession> var2) {
      return refusal(var0, var1, var2).isEmpty();
   }

   static void feedback(ClientSession var0) {
      GameHub var1 = (GameHub)get(var0, "hub");
      long var2 = (Long)get(var0, "currentRoomId");
      String var4;
      synchronized (var1) {
         Object var6 = ((Map)get(var1, "rooms")).get(var2);
         if (var6 == null || !(Boolean)get(var0, "ready") || (Boolean)get(var6, "battleStarted")) {
            return;
         }

         List var7 = (List)call(var6, "combatants", new Class[0]);
         var4 = refusal(var1, var6, var7);
      }

      if (!var4.isEmpty()) {
         call(var0, "sendEncrypted", new Class[]{byte[].class, String.class}, Protocol.systemMessage(9, var4), "S_MESSAGE room-start-refused");
      }
   }

   static void joined(ClientSession var0) {
      GameHub var1 = (GameHub)get(var0, "hub");
      long var2 = (Long)get(var0, "currentRoomId");
      if (testRoom(var1, var2)) {
         call(
            var0,
            "sendEncrypted",
            new Class[]{byte[].class, String.class},
            Protocol.systemMessage(9, "Local Lobby 為測試房，不計勝敗戰績；死亡競賽單人測試會加入 ArenaBot。"),
            "S_MESSAGE test-room"
         );
      }
   }

   static void restoreBot(ClientSession var0) {
      GameHub var1 = (GameHub)get(var0, "hub");
      long var2 = (Long)get(var0, "currentRoomId");
      List<ClientSession> var4;
      synchronized (var1) {
         if (!activeBot(var1, var2)) {
            return;
         }

         Object var6 = ((Map)get(var1, "rooms")).get(var2);
         if (var6 == null || (Boolean)get(var6, "battleStarted") || (Boolean)get(var6, "resultPending")) {
            return;
         }

         var4 = var1.roomMembers(var2);
      }

      for (ClientSession var10 : var4) {
         if (get(var10, "phase").toString().equals("ROOM")) {
            try {
               call(
                  var10,
                  "sendRoomParticipant",
                  new Class[]{long.class, String.class, Protocol.LitoForce.class, int.class, int.class, String.class, int.class, String.class},
                  var2,
                  "ArenaBot",
                  Protocol.LitoForce.FORCE_1,
                  0,
                  0,
                  "",
                  0,
                  "test-bot-restored"
               );
            } catch (IllegalStateException var8) {
               if (!(var8.getCause() instanceof IOException)) {
                  throw var8;
               }

               System.out.println("[LiTo] test-bot roster recipient disconnected");
            }
         }
      }
   }

   record Policy(boolean testRoomEnabled, boolean testBotEnabled) {
   }
}
