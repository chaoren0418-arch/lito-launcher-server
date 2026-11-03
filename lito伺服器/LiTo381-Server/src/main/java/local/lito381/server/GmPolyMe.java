package local.lito381.server;

import java.sql.Connection;
import java.util.Set;

/** Source L1PolyMe: self morph, GM cause (2), 7200 seconds; runtime state only. */
final class GmPolyMe {
    static final int DURATION_SECONDS = 7200;
    private static final Set<Integer> BLOCKED_MAPS = Set.of(5300,9000,9100,9101,9102,9202);
    @FunctionalInterface interface Forms {
        BattlePolymorphCatalog.Form load(AccountRepository repository, int gfx) throws Exception;
    }

    static boolean command(ClientSession session, String message) throws Exception {
        return command(session, message, GmPolyMe::readForm);
    }

    static boolean command(ClientSession s, String message, Forms forms) throws Exception {
        String[] args = (message == null ? "" : message.trim()).split("\\s+");
        if (!args[0].equalsIgnoreCase(".polyme")) return false;
        try {
            var repo = (AccountRepository) GmTravel.call(s,"selectedSourceAccounts",new Class[]{});
            var rule = repo.gmCommand("polyme");
            if (rule == null || !"polyme".equals(rule.name()) || rule.systemCommand()
                    || !"L1PolyMe".equals(rule.executor())) {
                throw new IllegalArgumentException("資料庫未啟用 .polyme，或執行器不是 L1PolyMe。");
            }
            int required = Math.max(1, rule.requiredAccessLevel());
            if (SourceAdministration.accessLevel(repo,s.accountName(),s) < required)
                throw new IllegalArgumentException("GM 權限不足：需要 access_level " + required + "。");
            int gfx = parse(args);
            Object phase = GmTravel.get(s,"phase");
            long room = (long) GmTravel.get(s,"currentRoomId");
            int map = map(s);
            int instance = (int) GmTravel.get(s,"classicWorldInstanceId");
            var binding = NativePlayerCommit.Binding.capture(s);
            var form = forms.load(repo,gfx);
            NativeLightAction.commitPair(s,s,() -> {
              try {
                if (!binding.matches(s)) throw new IllegalArgumentException("角色已切換，請重新輸入變身指令。");
            Object equipment;
            synchronized (s) {
                if (phase != GmTravel.get(s,"phase") || room != (long) GmTravel.get(s,"currentRoomId")
                        || map != map(s) || instance != (int) GmTravel.get(s,"classicWorldInstanceId"))
                    throw new IllegalArgumentException("場景已切換，請重新輸入變身指令。");
                if ((boolean) GmTravel.get(s,"observer") || (int) GmTravel.get(s,"playerHp") <= 0)
                    throw new IllegalArgumentException("觀戰或死亡狀態不能變身。");
                if (GmTravel.get(s,"pendingPortal") != null || (boolean) GmTravel.get(s,"movementLockPending"))
                    throw new IllegalArgumentException("正在切換場景，請稍後再變身。");
                int currentGfx = (int) GmTravel.call(s,"battleGfx",new Class[]{});
                if (currentGfx == 6034 || currentGfx == 6035)
                    throw new IllegalArgumentException("目前外型禁止變身。");
                equipment = GmTravel.call(s,"preparePolymorphEquipment",new Class[]{BattlePolymorphCatalog.Form.class},form);
                ((BattlePolymorphStatus) GmTravel.get(s,"polymorphStatus"))
                        .activate(System.nanoTime(),form,DURATION_SECONDS);
            }
            GmTravel.call(s,"publishPolymorphEquipmentChange",new Class[]{equipment.getClass(),String.class},equipment,"GM-polyme");
            GmTravel.call(s,"broadcastPolymorphAppearance",new Class[]{String.class},"GM-polyme");
            GmTravel.reply(s,"變身外型 " + gfx + "，持續 " + DURATION_SECONDS + " 秒。");
              } catch (IllegalArgumentException failure) { throw failure; }
                catch (Exception failure) { throw new java.io.IOException("GM polymorph commit failed",failure); }
            });
        } catch (IllegalArgumentException failure) {
            GmTravel.reply(s,failure.getMessage());
        } catch (Exception failure) {
            System.err.println("[LiTo] GM polyme failed: " + failure.getClass().getSimpleName());
            GmTravel.reply(s,"變身指令未完成，請查看伺服器紀錄。");
        }
        return true;
    }

    static int parse(String[] args) {
        if (args.length == 2) try {
            int gfx = Integer.parseInt(args[1]);
            if (gfx > 0 && gfx <= 65535) return gfx;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("用法：.polyme 外型代號（1～65535），例如 .polyme 6080。");
    }

    private static int map(ClientSession s) throws Exception {
        String phase = GmTravel.get(s,"phase").toString();
        int map;
        if (phase.equals("BATTLE") && (long) GmTravel.get(s,"currentRoomId") != 0)
            map = ((GameHub) GmTravel.get(s,"hub")).battleMapId((long) GmTravel.get(s,"currentRoomId"),s);
        else if (phase.equals("WORLD")) map = (int) GmTravel.get(s,"classicWorldMapId");
        else throw new IllegalArgumentException("請先進入遊戲世界再使用 .polyme。");
        if (BLOCKED_MAPS.contains(map)) throw new IllegalArgumentException("此地圖禁止變身。");
        return map;
    }

    static void exitRoom(ClientSession s, boolean notifySelf) throws java.io.IOException {
        try {
            GmTravel.call(s,"exitRoomBeforePolyMe",new Class[]{boolean.class},notifySelf);
        } catch (java.io.IOException failure) { throw failure; }
          catch (Exception failure) { throw new java.io.IOException("Room exit failed",failure); }
        finally {
            try {
                String phase=GmTravel.get(s,"phase").toString();
                if (!phase.equals("BATTLE") && !phase.equals("WORLD"))
                    ((BattlePolymorphStatus)GmTravel.get(s,"polymorphStatus")).clear();
            } catch (Exception failure) { throw new java.io.IOException("Polymorph cleanup failed",failure); }
        }
    }

    static BattlePolymorphCatalog.Form readForm(AccountRepository repo, int gfx) throws Exception {
        try (Connection connection = LiToGmCommands.connection(repo)) {
            return readForm(connection,LiToGmCommands.table(repo,"polymorphs"),gfx);
        }
    }

    static BattlePolymorphCatalog.Form readForm(Connection connection,String table,int gfx) throws Exception {
        String name = "GM-" + gfx;
        int level = 1, weapons = 0x7ff, armor = 0xfff, cause = 2;
        boolean skills = true;
        // Same as source PolyTable: the last matching SQL row wins; missing rows permit GM cause.
        try (var query = connection.prepareStatement("SELECT name,minlevel,weaponequip,armorequip,isSkillUse,cause FROM " + table + " WHERE polyid=?")) {
            query.setInt(1,gfx);
            try (var rows = query.executeQuery()) {
                while (rows.next()) {
                    name=rows.getString(1); level=Math.max(1,rows.getInt(2)); weapons=rows.getInt(3);
                    armor=rows.getInt(4); skills=rows.getBoolean(5); cause=rows.getInt(6);
                }
            }
        }
        if ((cause & 2) == 0) throw new IllegalArgumentException("該外型的 polymorphs.cause 未開放 GM 變身。");
        try {
            return new BattlePolymorphCatalog.Form(name,gfx,level,weapons,armor,skills,cause,false,0);
        } catch (IllegalArgumentException unsupported) {
            throw new IllegalArgumentException("外型 " + gfx + " 缺少可用動畫或超出目前支援的變身資料範圍。");
        }
    }
    private GmPolyMe() { }
}
