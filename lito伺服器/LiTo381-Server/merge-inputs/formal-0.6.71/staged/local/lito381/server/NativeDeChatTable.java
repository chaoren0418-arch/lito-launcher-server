// DECOMPILED from base-0.6.71.jar, TODO review
// Decompiled with Vineflower 1.11.2; reviewed against formal bytecode.
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import local.lito381.server.NativeDeTerrainScope.Database;

public class NativeDeChatTable {
   private static final NativeDeWorldLog _log = NativeDeChatScope.Logs.getLogger(NativeDeChatTable.class);
   private static final Map<Integer, NativeDeChatData> _npcChatAppearance = new HashMap<>();
   private static final Map<Integer, NativeDeChatData> _npcChatDead = new HashMap<>();
   private static final Map<Integer, NativeDeChatData> _npcChatHide = new HashMap<>();
   private static final Map<Integer, NativeDeChatData> _npcChatGameTime = new HashMap<>();
   private Collection<NativeDeChatData> _allTimeValues;
   final NativeDeChatScope.Data nativeData = NativeDeChatScope.loadingData();

   public void load() {
      NativeDeTerrainTimer var1 = new NativeDeTerrainTimer();

      try (
         Connection var2 = Database.get().getConnection();
         PreparedStatement var3 = var2.prepareStatement("SELECT * FROM `npcchat`");
         ResultSet var4 = var3.executeQuery();
      ) {
         while (var4.next()) {
            int var5 = -1;

            try {
               var5 = var4.getInt("npc_id");
               NativeDeChatData var6 = new NativeDeChatData();
               var6.setNpcId(var5);
               var6.setChatTiming(var4.getInt("chat_timing"));
               var6.setStartDelayTime(var4.getInt("start_delay_time"));
               var6.setChatId1(var4.getString("chat_id1"));
               var6.setChatId2(var4.getString("chat_id2"));
               var6.setChatId3(var4.getString("chat_id3"));
               var6.setChatId4(var4.getString("chat_id4"));
               var6.setChatId5(var4.getString("chat_id5"));
               var6.setChatInterval(var4.getInt("chat_interval"));
               var6.setShout(var4.getBoolean("is_shout"));
               var6.setWorldChat(var4.getBoolean("is_world_chat"));
               var6.setRepeat(var4.getBoolean("is_repeat"));
               var6.setRepeatInterval(var4.getInt("repeat_interval"));
               var6.setGameTime(var4.getInt("game_time"));
               if (var6.getChatTiming() == 0) {
                  NativeDeChatScope._npcChatAppearance(this).put(var6.getNpcId(), var6);
               } else if (var6.getChatTiming() == 1) {
                  NativeDeChatScope._npcChatDead(this).put(var6.getNpcId(), var6);
               } else if (var6.getChatTiming() == 2) {
                  NativeDeChatScope._npcChatHide(this).put(var6.getNpcId(), var6);
               } else if (var6.getChatTiming() == 3) {
                  NativeDeChatScope._npcChatGameTime(this).put(var6.getNpcId(), var6);
               }
            } catch (Exception var10) {
               NativeDeChatScope.log().error("載入資料失敗，已略過該筆 (npcid: " + var5 + ")", (Throwable)var10);
            }
         }
      } catch (SQLException var14) {
         NativeDeChatScope.log().error("載入 npcchat 失敗! 請檢查資料表!", (Throwable)var14);
      }

      int var15 = NativeDeChatScope._npcChatAppearance(this).size()
         + NativeDeChatScope._npcChatDead(this).size()
         + NativeDeChatScope._npcChatHide(this).size()
         + NativeDeChatScope._npcChatGameTime(this).size();
      NativeDeChatScope.log().info("載入 npcchat ok: " + var15 + " (" + var1.get() + "ms)");
   }

   public NativeDeChatData getTemplateAppearance(int var1) {
      return NativeDeChatScope._npcChatAppearance(this).get(var1);
   }

   public NativeDeChatData getTemplateDead(int var1) {
      return NativeDeChatScope._npcChatDead(this).get(var1);
   }

   public NativeDeChatData getTemplateHide(int var1) {
      return NativeDeChatScope._npcChatHide(this).get(var1);
   }

   public NativeDeChatData getTemplateGameTime(int var1) {
      return NativeDeChatScope._npcChatGameTime(this).get(var1);
   }

   public Collection<NativeDeChatData> all() {
      try {
         Collection var1 = this._allTimeValues;
         return var1 != null ? var1 : (this._allTimeValues = Collections.unmodifiableCollection(NativeDeChatScope._npcChatGameTime(this).values()));
      } catch (Exception var2) {
         NativeDeChatScope.log().error(var2.getLocalizedMessage(), (Throwable)var2);
         return null;
      }
   }

   public NativeDeChatData[] getAllGameTime() {
      return NativeDeChatScope._npcChatGameTime(this).values().toArray(new NativeDeChatData[NativeDeChatScope._npcChatGameTime(this).size()]);
   }

   public static NativeDeChatTable get() {
      return NativeDeChatResources.table();
   }
}
