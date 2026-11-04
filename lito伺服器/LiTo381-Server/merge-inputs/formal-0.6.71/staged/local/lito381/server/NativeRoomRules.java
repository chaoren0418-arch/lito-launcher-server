package local.lito381.server;

import java.sql.*;
import java.util.*;

/** SQL-owned default test room; ordinary room admission uses lito_battle_rule. */
final class NativeRoomRules {
    record Policy(boolean testRoomEnabled, boolean testBotEnabled) {}
    private static final Map<GameHub, Policy> POLICIES = Collections.synchronizedMap(new WeakHashMap<>());
    static Object get(Object o, String n) { return DurableBattleSettlement.get(o, n); }
    static Object call(Object o, String n, Class<?>[] t, Object... a) {
        return DurableBattleSettlement.call(o, n, t, a);
    }
    static Policy read(ServerConfig config) throws Exception {
        var p = UnifiedConfiguration.read(config.source());
        String database = SharedDatabase.identifier(p.getProperty("database.lito.database"));
        try (var c = DriverManager.getConnection(p.getProperty("database.jdbc.base.url") + database
                + p.getProperty("database.jdbc.options"), p.getProperty("database.user"), p.getProperty("database.password"));
             var s = c.createStatement();
             var r = s.executeQuery("SELECT policy_id,test_room_enabled,test_bot_enabled FROM lito_room_policy")) {
            if (!r.next() || r.getInt(1) != 1) throw new IllegalStateException("lito_room_policy requires policy_id=1");
            int enabled = r.getInt(2), bot = r.getInt(3);
            if ((enabled != 0 && enabled != 1) || (bot != 0 && bot != 1) || r.next())
                throw new IllegalStateException("invalid lito_room_policy; expected one row with boolean flags");
            return new Policy(enabled == 1, bot == 1);
        }
    }
    static void install(GameHub hub, ServerConfig config, AccountRepository accounts) {
        try {
            // Isolated protocol fixtures use no database and receive an explicit test policy.
            Policy policy = accounts instanceof MemoryAccountRepository || accounts == null
                    ? new Policy(true, true) : read(config);
            if (accounts != null && !(accounts instanceof MemoryAccountRepository)) ResourceDatabase.load(config);
            POLICIES.put(hub, policy);
        } catch (Exception e) { throw new IllegalStateException("Cannot load LiTo room policy or qualified resource tables; inspect the nested database error", e); }
    }
    static Policy policy(GameHub hub) {
        var p = POLICIES.get(hub);
        if (p == null) throw new IllegalStateException("room policy not initialized");
        return p;
    }
    static boolean enabled(GameHub hub) { return policy(hub).testRoomEnabled(); }
    static boolean testRoom(GameHub hub, long id) {
        if (id == 0) return false;
        synchronized (hub) {
            for (Object channel : ((Map<?, ?>) get(hub, "channels")).values())
                if ((long) get(channel, "defaultRoomId") == id) return true;
            return false;
        }
    }
    static boolean bot(BattleRules.Mode mode, GameHub hub, long id) {
        return mode == BattleRules.Mode.DEATHMATCH && policy(hub).testBotEnabled() && testRoom(hub, id);
    }
    static boolean bot(BattleRules.Mode mode, ClientSession s) {
        return bot(mode, (GameHub) get(s, "hub"), (long) get(s, "currentRoomId"));
    }
    static boolean activeBot(GameHub hub, long id) {
        synchronized (hub) {
            return bot(hub.roomRules(id).mode(), hub, id) && hub.roomCombatantCount(id) == 1
                    && hub.roomVirtualPlayers(id).isEmpty();
        }
    }
    static boolean activeBot(ClientSession s) {
        return activeBot((GameHub) get(s, "hub"), (long) get(s, "currentRoomId"));
    }
    static int botHp(Object room, GameHub hub) {
        return activeBot(hub, (long) get(room, "id")) ? GameHub.ARENA_BOT_MAX_HP : 0;
    }
    static int botSingle(GameHub hub, long id) { return activeBot(hub, id) ? 1 : -1; }
    static String refusal(GameHub hub, Object room, List<ClientSession> players) {
        var match = (BattleRules.MatchState) get(room, "match");
        var rules = match.rules();
        long id = (long) get(room, "id");
        if (!rules.mode().isBattleScene()) return "此模式不是戰鬥房。";
        int humans = players.size();
        // Only the default deathmatch test room may substitute ArenaBot for an opponent.
        if (activeBot(hub, id)) {
            Map<?, ?> forces = (Map<?, ?>) get(room, "forces");
            return Integer.valueOf(1).equals(forces.get(players.get(0)))
                    ? "無法開始：單人測試請選紅隊，ArenaBot 位於藍隊。" : "";
        }
        int minimum = Math.max(rules.minimumPlayers(), rules.teamBased() ? 2 : 1);
        if (humans < minimum) {
            return "無法開始：至少需要 " + minimum + " 位參戰玩家，目前 "
                    + humans + " 位（觀戰者與假人不計）。";
        }
        if (rules.teamBased()) {
            Map<?, ?> forces = (Map<?, ?>) get(room, "forces");
            int red = 0, blue = 0;
            for (ClientSession p : players) { if (Integer.valueOf(1).equals(forces.get(p))) blue++; else red++; }
            if (red == 0 || blue == 0) return "無法開始：紅隊與藍隊都必須有參戰玩家。";
            var settings = (GameHub.RoomSettings) get(room, "settings");
            if ((settings.attributes() & GameHub.RoomSettings.EQUAL_SIDES) != 0 && red != blue)
                return "無法開始：此房間要求雙方人數一致。";
            if (rules.mode() == BattleRules.Mode.SIEGE_WAR && (boolean) get(hub, "siegeRosterEnforced")
                    && (red < 15 || blue < 15 || red > 50 || blue > 50))
                return "無法開始：攻城模式每隊需要 15～50 位參戰玩家。";
        }
        return "";
    }
    static boolean rosterReady(GameHub hub, Object room, List<ClientSession> players) {
        return refusal(hub, room, players).isEmpty();
    }
    static void feedback(ClientSession s) {
        var hub = (GameHub) get(s, "hub"); long id = (long) get(s, "currentRoomId"); String message;
        synchronized (hub) {
            Object room = ((Map<?, ?>) get(hub, "rooms")).get(id);
            if (room == null || !(boolean) get(s, "ready") || (boolean) get(room, "battleStarted")) return;
            @SuppressWarnings("unchecked") var players = (List<ClientSession>) call(room, "combatants", new Class<?>[]{});
            message = refusal(hub, room, players);
        }
        if (!message.isEmpty()) call(s, "sendEncrypted", new Class<?>[]{byte[].class, String.class},
                Protocol.systemMessage(9, message), "S_MESSAGE room-start-refused");
    }
    static void joined(ClientSession s) {
        var hub = (GameHub) get(s, "hub"); long id = (long) get(s, "currentRoomId");
        if (testRoom(hub, id)) call(s, "sendEncrypted", new Class<?>[]{byte[].class, String.class},
                Protocol.systemMessage(9, "Local Lobby 為測試房，不計勝敗戰績；死亡競賽單人測試會加入 ArenaBot。"), "S_MESSAGE test-room");
    }
    static void restoreBot(ClientSession departing) {
        var hub = (GameHub) get(departing, "hub"); long id = (long) get(departing, "currentRoomId");
        List<ClientSession> recipients;
        synchronized (hub) {
            if (!activeBot(hub, id)) return;
            Object room = ((Map<?, ?>) get(hub, "rooms")).get(id);
            if (room == null || (boolean) get(room, "battleStarted") || (boolean) get(room, "resultPending")) return;
            recipients = hub.roomMembers(id);
        }
        for (var peer : recipients) {
            if (!get(peer, "phase").toString().equals("ROOM")) continue;
            try {
                call(peer, "sendRoomParticipant", new Class<?>[]{long.class,String.class,Protocol.LitoForce.class,
                        int.class,int.class,String.class,int.class,String.class},
                        id,"ArenaBot",Protocol.LitoForce.FORCE_1,0,0,"",0,"test-bot-restored");
            } catch (IllegalStateException e) {
                if (!(e.getCause() instanceof java.io.IOException)) throw e;
                System.out.println("[LiTo] test-bot roster recipient disconnected");
            }
        }
    }
}
