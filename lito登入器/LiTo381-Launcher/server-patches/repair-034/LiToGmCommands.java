package local.lito381.server;

import java.io.IOException;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.function.LongSupplier;
import javax.sql.DataSource;

/** Native chat commands. Only these two source executors are executable. */
final class LiToGmCommands {
    static Object get(Object o,String n) throws Exception {Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    static void set(Object o,String n,Object v) throws Exception {Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
    static Object call(Object o,String n,Class<?>[] t,Object...v) throws Exception {
        Method m=o.getClass().getDeclaredMethod(n,t);m.setAccessible(true);
        try{return m.invoke(o,v);}catch(InvocationTargetException e){if(e.getCause() instanceof Exception x)throw x;throw e;}
    }
    static void reply(ClientSession s,String text) throws Exception {call(s,"sendGmCommandMessage",new Class[]{String.class},text);}
    static boolean handle(ClientSession s,String text) throws IOException {
        String[] a=(text==null?"":text.trim()).split("\\s+");
        String command=a[0].toLowerCase(Locale.ROOT);
        if(!command.equals(".item")&&!command.equals(".spawn"))return false;
        try {
            AccountRepository repo=(AccountRepository)call(s,"selectedSourceAccounts",new Class[]{});
            GmCommandDefinition policy=repo.gmCommand(command.substring(1));
            String executor=command.equals(".item")?"L1CreateItem":"L1SpawnCmd";
            if(policy==null||!command.substring(1).equals(policy.name())||policy.systemCommand()||!executor.equals(policy.executor())){
                reply(s,"資料庫未啟用 "+command+"，或執行器設定不符。");return true;
            }
            String account=(String)call(s,"accountName",new Class[]{});
            int access=repo.accessLevel(account);
            // A configuration administrator is not a replacement for source GM authority.
            if(access<=0||access<policy.requiredAccessLevel()){
                reply(s,"GM 權限不足：需要 access_level "+Math.max(1,policy.requiredAccessLevel())+"。");return true;
            }
            if(a.length<2||a.length>4){usage(s,command);return true;}
            int count=a.length>2?positive(a[2]):1;
            int last=a.length>3?integer(a[3]):0;
            String phase=get(s,"phase").toString();
            if(!Set.of("CHANNEL","ROOM","BATTLE","WORLD").contains(phase)||((String)get(s,"selectedCharacter")).isBlank()){
                reply(s,"請先選擇角色並進入大廳或遊戲世界。");return true;
            }
            if(command.equals(".item"))createItem(s,repo,a[1],count,last);
            else LiToGmNpcs.spawn(s,repo,a[1],count,last);
        }catch(NumberFormatException e){try{usage(s,command);}catch(Exception send){throw new IOException(send);}}
        catch(IllegalArgumentException e){try{reply(s,e.getMessage());}catch(Exception send){throw new IOException(send);}}
        catch(Exception e){
            System.err.println("[LiTo] GM "+command+" failed: "+e.getClass().getSimpleName());
            try{reply(s,"GM 指令未完成，請查看伺服器紀錄；請勿連續重送。");}catch(Exception send){throw new IOException(send);}
            e.printStackTrace(System.err);
        }
        return true;
    }
    static int integer(String value){if(!value.matches("-?[0-9]{1,10}"))throw new NumberFormatException();return Integer.parseInt(value);}
    static int positive(String value){int n=integer(value);if(n<=0)throw new NumberFormatException();return n;}
    static void usage(ClientSession s,String c)throws Exception{reply(s,c.equals(".item")?"用法：.item 道具ID或名稱 [數量=1] [強化值=0]；非堆疊最多10個，強化 -128～127。":"用法：.spawn NPC_ID或名稱 [數量=1] [範圍=0]；數量1～100，範圍0～20。");}
    static Connection connection(AccountRepository r)throws Exception{
        if(!r.getClass().getSimpleName().equals("LegacyMySqlRepository"))throw new IllegalArgumentException("GM 建立指令需要正式 SQL 來源。");
        Connection c=((DataSource)get(r,"dataSource")).getConnection();c.setReadOnly(true);return c;
    }
    static String table(AccountRepository r,String name)throws Exception{
        String db=(String)get(r,"resourceDatabase");if(!db.matches("[A-Za-z0-9_]+"))throw new SQLException("invalid schema");return "`"+db+"`.`"+name+"`";
    }
    static int resolve(AccountRepository r,Connection c,String token,boolean npc)throws Exception{
        if(token.matches("[0-9]+"))return positive(token);
        Set<Integer> ids=new LinkedHashSet<>();String id=npc?"npcid":"item_id";
        for(String t:npc?List.of("npc"):List.of("weapon","armor","etcitem")){
            try(var q=c.prepareStatement("SELECT "+id+" FROM "+table(r,t)+" WHERE REPLACE(name,' ','')=? LIMIT 2")){
                q.setString(1,token);try(var rows=q.executeQuery()){while(rows.next())ids.add(rows.getInt(1));}
            }
        }
        if(ids.size()!=1)throw new IllegalArgumentException(ids.isEmpty()?"找不到指定名稱，請改用資料表 ID。":"名稱重複，請改用資料表 ID。");return ids.iterator().next();
    }
    static void createItem(ClientSession s,AccountRepository r,String token,int count,int enchant)throws Exception{
        if(enchant< -128||enchant>127)throw new IllegalArgumentException("強化值必須介於 -128～127，避免原生封包溢位。");
        InventoryCatalog.Definition d;
        try(Connection c=connection(r)){int id=resolve(r,c,token,false);d=(InventoryCatalog.Definition)call(r,"rewardItemDefinition",new Class[]{Connection.class,int.class},c,id);}
        if(d==null)throw new IllegalArgumentException("道具不存在或資料表定義不完整。");
        if(!d.stackable()&&count>10)throw new IllegalArgumentException("非堆疊道具每次最多建立10個。");
        if(d.stackable())enchant=0;
        if(enchant!=0&&!d.weapon()&&!d.armor())throw new IllegalArgumentException("這個道具不支援強化值。");
        RuntimeInventory before=(RuntimeInventory)get(s,"inventory"),next=before.copy();
        if(!(boolean)call(s,"permitsInventoryAddition",new Class[]{RuntimeInventory.class,InventoryCatalog.Definition.class,long.class},before,d,(long)count))throw new IllegalArgumentException("背包負重不足，沒有建立道具。");
        // Durable grant provenance is deliberately distinct from tournament shop purchases.
        var state=new RuntimeInventory.PersistentItem(0,count,enchant,0,true,d.blessing(),false,true,false,d,d.initialChargeCount(),0L,false,false);
        LongSupplier allocator=()->{try{return (long)callStatic(RuntimeInventory.class,"nextObjectId",new Class[]{});}catch(Exception e){throw new IllegalStateException(e);}};
        if(!(boolean)call(next,"addState",new Class[]{InventoryCatalog.Definition.class,RuntimeInventory.PersistentItem.class,LongSupplier.class},d,state,allocator))throw new IllegalArgumentException("背包空間或堆疊數量已達上限，沒有建立道具。");
        call(s,"persistSqlTournamentRewards",new Class[]{RuntimeInventory.class},next);
        GameStateStore store=(GameStateStore)get(s,"state");String character=(String)call(s,"stateCharacter",new Class[]{});
        store.saveEconomyState(character,(long)get(s,"shopPoints"),next.catalogCounts(),next.catalogItemStates());
        set(s,"inventory",next);call(s,"sendInventoryDelta",new Class[]{RuntimeInventory.class,RuntimeInventory.class},before,next);
        reply(s,"已建立 "+d.name()+" ×"+count+(enchant==0?"":"（強化 "+enchant+"）")+"，已加入背包並保存。");
        System.out.println("[LiTo] GM item character="+character+" item="+d.catalogId()+" count="+count+" enchant="+enchant);
    }
    static Object callStatic(Class<?> c,String n,Class<?>[] t,Object...v)throws Exception{Method m=c.getDeclaredMethod(n,t);m.setAccessible(true);return m.invoke(null,v);}
}
