// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import local.lito381.server.NativeDeChatData;
import local.lito381.server.NativeDeChatResources;
import local.lito381.server.NativeDeChatScope;
import local.lito381.server.NativeDeTerrainScope;
import local.lito381.server.NativeDeTerrainTimer;
import local.lito381.server.NativeDeWorldLog;

public class NativeDeChatTable {
    private static final NativeDeWorldLog _log = NativeDeChatScope.Logs.getLogger(NativeDeChatTable.class);
    private static final Map<Integer, NativeDeChatData> _npcChatAppearance = new HashMap<Integer, NativeDeChatData>();
    private static final Map<Integer, NativeDeChatData> _npcChatDead = new HashMap<Integer, NativeDeChatData>();
    private static final Map<Integer, NativeDeChatData> _npcChatHide = new HashMap<Integer, NativeDeChatData>();
    private static final Map<Integer, NativeDeChatData> _npcChatGameTime = new HashMap<Integer, NativeDeChatData>();
    private Collection<NativeDeChatData> _allTimeValues;
    final NativeDeChatScope.Data nativeData = NativeDeChatScope.loadingData();

    public void load() {
        NativeDeTerrainTimer nativeDeTerrainTimer;
        block27: {
            nativeDeTerrainTimer = new NativeDeTerrainTimer();
            try (Connection connection = NativeDeTerrainScope.Database.get().getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `npcchat`");){
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    while (resultSet.next()) {
                        int n = -1;
                        try {
                            n = resultSet.getInt("npc_id");
                            NativeDeChatData nativeDeChatData = new NativeDeChatData();
                            nativeDeChatData.setNpcId(n);
                            nativeDeChatData.setChatTiming(resultSet.getInt("chat_timing"));
                            nativeDeChatData.setStartDelayTime(resultSet.getInt("start_delay_time"));
                            nativeDeChatData.setChatId1(resultSet.getString("chat_id1"));
                            nativeDeChatData.setChatId2(resultSet.getString("chat_id2"));
                            nativeDeChatData.setChatId3(resultSet.getString("chat_id3"));
                            nativeDeChatData.setChatId4(resultSet.getString("chat_id4"));
                            nativeDeChatData.setChatId5(resultSet.getString("chat_id5"));
                            nativeDeChatData.setChatInterval(resultSet.getInt("chat_interval"));
                            nativeDeChatData.setShout(resultSet.getBoolean("is_shout"));
                            nativeDeChatData.setWorldChat(resultSet.getBoolean("is_world_chat"));
                            nativeDeChatData.setRepeat(resultSet.getBoolean("is_repeat"));
                            nativeDeChatData.setRepeatInterval(resultSet.getInt("repeat_interval"));
                            nativeDeChatData.setGameTime(resultSet.getInt("game_time"));
                            if (nativeDeChatData.getChatTiming() == 0) {
                                NativeDeChatScope._npcChatAppearance(this).put(nativeDeChatData.getNpcId(), nativeDeChatData);
                                continue;
                            }
                            if (nativeDeChatData.getChatTiming() == 1) {
                                NativeDeChatScope._npcChatDead(this).put(nativeDeChatData.getNpcId(), nativeDeChatData);
                                continue;
                            }
                            if (nativeDeChatData.getChatTiming() == 2) {
                                NativeDeChatScope._npcChatHide(this).put(nativeDeChatData.getNpcId(), nativeDeChatData);
                                continue;
                            }
                            if (nativeDeChatData.getChatTiming() != 3) continue;
                            NativeDeChatScope._npcChatGameTime(this).put(nativeDeChatData.getNpcId(), nativeDeChatData);
                            continue;
                        }
                        catch (Exception exception) {
                            NativeDeChatScope.log().error("\u8f09\u5165\u8cc7\u6599\u5931\u6557\uff0c\u5df2\u7565\u904e\u8a72\u7b46 (npcid: " + n + ")", exception);
                        }
                    }
                }
                finally {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
            }
            catch (SQLException sQLException) {
                NativeDeChatScope.log().error("\u8f09\u5165 npcchat \u5931\u6557! \u8acb\u6aa2\u67e5\u8cc7\u6599\u8868!", sQLException);
            }
        }
        int n = NativeDeChatScope._npcChatAppearance(this).size() + NativeDeChatScope._npcChatDead(this).size() + NativeDeChatScope._npcChatHide(this).size() + NativeDeChatScope._npcChatGameTime(this).size();
        NativeDeChatScope.log().info("\u8f09\u5165 npcchat ok: " + n + " (" + nativeDeTerrainTimer.get() + "ms)");
    }

    public NativeDeChatData getTemplateAppearance(int n) {
        return NativeDeChatScope._npcChatAppearance(this).get(n);
    }

    public NativeDeChatData getTemplateDead(int n) {
        return NativeDeChatScope._npcChatDead(this).get(n);
    }

    public NativeDeChatData getTemplateHide(int n) {
        return NativeDeChatScope._npcChatHide(this).get(n);
    }

    public NativeDeChatData getTemplateGameTime(int n) {
        return NativeDeChatScope._npcChatGameTime(this).get(n);
    }

    public Collection<NativeDeChatData> all() {
        try {
            Collection<NativeDeChatData> collection = this._allTimeValues;
            return collection != null ? collection : (this._allTimeValues = Collections.unmodifiableCollection(NativeDeChatScope._npcChatGameTime(this).values()));
        }
        catch (Exception exception) {
            NativeDeChatScope.log().error(exception.getLocalizedMessage(), exception);
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
