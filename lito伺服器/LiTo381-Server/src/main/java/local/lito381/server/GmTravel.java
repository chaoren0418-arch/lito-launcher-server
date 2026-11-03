package local.lito381.server;

import java.io.IOException;
import java.sql.*;
import java.util.*;

/** Native same-map travel. No character/database writes and no synthetic battle. */
final class GmTravel {
    private static volatile Set<Long> stormItems = Set.of();
    static void load(ServerConfig config) throws Exception {
        var p=UnifiedConfiguration.read(config.source());
        String db=SharedDatabase.identifier(p.getProperty("database.static.database"));
        Set<Long> ids=new HashSet<>();
        try(var c=DriverManager.getConnection(p.getProperty("database.jdbc.base.url")+db+p.getProperty("database.jdbc.options"),p.getProperty("database.user"),p.getProperty("database.password"))) {
            c.setReadOnly(true);
            try(var q=c.prepareStatement("SELECT item_id FROM `"+db+"`.`etcitem` WHERE classname=?")) {
                q.setString(1,"wand.Storm_Walk");
                try(var rows=q.executeQuery()){while(rows.next())ids.add(rows.getLong(1));}
            }
        }
        stormItems=Set.copyOf(ids);
        System.out.println("[LiTo] Storm_Walk item cores loaded: "+stormItems);
    }
    static Object get(ClientSession s,String n)throws Exception{return LiToGmCommands.get(s,n);}
    static void set(ClientSession s,String n,Object v)throws Exception{LiToGmCommands.set(s,n,v);}
    static Object call(ClientSession s,String n,Class<?>[] t,Object...v)throws Exception{return LiToGmCommands.call(s,n,t,v);}
    static void reply(ClientSession s,String text)throws Exception{LiToGmCommands.reply(s,text);}
    static void send(ClientSession s,byte[] p,String label)throws Exception{call(s,"sendEncrypted",new Class[]{byte[].class,String.class},p,label);}
    static boolean command(ClientSession s,String message)throws Exception {
        if (GmPolyMe.command(s,message)) return true;
        String[] a=(message==null?"":message.trim()).split("\\s+");
        String cmd=a[0].toLowerCase(Locale.ROOT);
        if(!cmd.equals(".loc")&&!cmd.equals(".move"))return (boolean)call(s,"handleGmChatCommandBeforeTravel",new Class[]{String.class},message);
        try {
            AccountRepository repo=(AccountRepository)call(s,"selectedSourceAccounts",new Class[]{});
            var policy=repo.gmCommand(cmd.substring(1));
            String executor=cmd.equals(".loc")?"L1Loc":"L1Move";
            if(policy==null||!policy.name().equals(cmd.substring(1))||policy.systemCommand()||!executor.equals(policy.executor())){reply(s,"資料庫未啟用 "+cmd+"，或執行器設定不符。");return true;}
            int access=SourceAdministration.accessLevel(repo,s.accountName(),s);
            if(access<Math.max(1,policy.requiredAccessLevel())){reply(s,"GM 權限不足：需要 access_level "+Math.max(1,policy.requiredAccessLevel())+"。");return true;}
            int map=map(s);
            if(cmd.equals(".loc")) {
                if(a.length!=1)throw new IllegalArgumentException("用法：.loc");
                var row=NativeLitoMapTableCatalog.forMap(map);
                reply(s,"座標 X="+get(s,"positionX")+" Y="+get(s,"positionY")+" 地圖="+map+(row==null?"":"（"+row.name()+"）")+" 方向="+get(s,"playerHeading"));
            } else {
                if(a.length<3||a.length>4)throw new IllegalArgumentException("用法：.move X Y [目前地圖ID]");
                int x=number(a[1]),y=number(a[2]);
                if(a.length==4&&number(a[3])!=map)throw new IllegalArgumentException("目前僅開放同地圖傳送，所在的地圖ID為 "+map+"。");
                travel(s,x,y,"GM .move");
                reply(s,"傳送至 X="+x+" Y="+y+" 地圖="+map+"，等待客戶端完成換景。");
            }
        } catch(IllegalArgumentException e){reply(s,e.getMessage());}
          catch(Exception e){System.err.println("[LiTo] GM travel failed: "+e.getClass().getSimpleName());reply(s,"傳送指令未完成，請查看伺服器紀錄。");}
        return true;
    }
    static int number(String v){try{int n=Integer.parseInt(v);if(n<0||n>65535)throw new NumberFormatException();return n;}catch(NumberFormatException e){throw new IllegalArgumentException("座標與地圖ID必須為 0～65535 的整數。");}}
    static int map(ClientSession s)throws Exception {
        if(!get(s,"phase").toString().equals("BATTLE")||(long)get(s,"currentRoomId")==0||(boolean)get(s,"rentalTownRoomState"))throw new IllegalArgumentException("請先進入戰鬥房間的遊戲世界。");
        return ((GameHub)get(s,"hub")).battleMapId((long)get(s,"currentRoomId"),s);
    }
    static void useItem(ClientSession s,byte[] payload)throws Exception {
        long id=Protocol.inventoryUseObjectId(payload);
        var item=((RuntimeInventory)get(s,"inventory")).item(id);
        if(item==null||!stormItems.contains(item.definition().catalogId())){call(s,"useInventoryItemBeforeTravel",new Class[]{byte[].class},payload);return;}
        try {
            // spell_long: objectId, targetId, X, Y. TargetId is intentionally ignored.
            var request=Protocol.targetLocationInventoryUse(payload);
            travel(s,request.x(),request.y(),"wand.Storm_Walk");
        }catch(IllegalArgumentException e){reply(s,e.getMessage());call(s,"sendTeleportCursorUnlock",new Class[]{String.class},"Storm-Walk-rejected");}
    }
    static void travel(ClientSession s,int x,int y,String reason)throws Exception {
        int map=map(s); long room=(long)get(s,"currentRoomId");var hub=(GameHub)get(s,"hub");
        if((boolean)get(s,"observer")||(int)get(s,"playerHp")<=0)throw new IllegalArgumentException("觀戰或死亡狀態無法傳送。");
        if(get(s,"pendingPortal")!=null||(boolean)get(s,"movementLockPending"))throw new IllegalArgumentException("正在切換場景，請稍後再試。");
        if((boolean)call(s,"rejectWhileParalyzed",new Class[]{String.class},reason))throw new IllegalArgumentException("目前狀態無法傳送。");
        var arena=ArenaMapCatalog.forMap(map);
        if(!arena.nativeStandingFloor(x,y))throw new IllegalArgumentException("該座標沒有可站立的地面，未執行傳送。");
        if(occupied(s,hub,room,map,x,y))throw new IllegalArgumentException("該座標已有單位或障礙物，未執行傳送。");
        set(s,"pendingCallClanPortal",null);set(s,"pendingRunClanPortal",null);set(s,"pendingClassicWorldPortal",null);
        set(s,"pendingPortal",new Protocol.PortalDestination(map,x,y,true));set(s,"pendingPortalReady",true);
        call(s,"sendTeleportCursorUnlock",new Class[]{String.class},reason);
        byte[] effect=Protocol.effectObject(s.playerObjectId(),ClassicWorldTeleportCatalog.TELEPORT_EFFECT_GFX);
        for(var peer:hub.roomMembers(room))if(hub.battleMapId(room,peer)==map&&BattleSpellCatalog.isInNativeScreen((int)get(peer,"positionX"),(int)get(peer,"positionY"),(int)get(s,"positionX"),(int)get(s,"positionY")))send(peer,effect,"S_EFFECT "+reason);
        send(s,Protocol.portal(map,x,y),"S_PORTAL "+reason+" map="+map+" x="+x+" y="+y);
        System.out.println("[LiTo] "+reason+" player="+s.playerName()+" map="+map+" x="+x+" y="+y+" awaiting-C_GOTO_PORTAL");
    }
    static boolean occupied(ClientSession s,GameHub hub,long room,int map,int x,int y)throws Exception {
        for(var member:hub.roomCombatants(room)){
            if(hub.battleMapId(room,member)!=map)continue;
            if(member!=s&&(int)get(member,"playerHp")>0&&(int)get(member,"positionX")==x&&(int)get(member,"positionY")==y)return true;
            for(var pet:member.controlledCompanionSnapshots())if(pet.active()&&pet.x()==x&&pet.y()==y)return true;
        }
        var bot=hub.botPosition(room);
        if(NativeRoomRules.bot(hub.roomRules(room).mode(),s)&&hub.roomCombatantCount(room)<2&&bot!=null&&hub.botHp(room)>0&&bot.x()==x&&bot.y()==y)return true;
        if(hub.visibleGuardianTowers(room,s).stream().anyMatch(t->!t.destroyed()&&t.x()==x&&t.y()==y)
          ||hub.visibleQuestMonsters(room,s).stream().anyMatch(t->t.alive()&&t.x()==x&&t.y()==y)
          ||hub.visibleTimeAttackActors(room,s).stream().anyMatch(t->t.alive()&&t.x()==x&&t.y()==y)
          ||hub.battleDoorBlocks(room,x,y)||hub.siegeGateBlocks(room,x,y))return true;
        for(String method:List.of("repairNpcPoint","spellShopNpcPoint","spellMaterialNpcPoint","classicMerchantNpcPoint","warehouseNpcPoint","castleStewardNpcPoint","houseAuctionBoardPoint","mercenaryCommanderNpcPoint")) {
            var pt=(ArenaMapCatalog.Point)call(s,method,new Class[]{});if(pt!=null&&pt.x()==x&&pt.y()==y)return true;
        }
        return false;
    }
}
