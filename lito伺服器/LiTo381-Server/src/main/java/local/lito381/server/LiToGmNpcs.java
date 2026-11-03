// ORIGINAL SOURCE moved from lito登入器/LiTo381-Launcher/server-patches/repair-034/LiToGmNpcs.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;

import java.sql.*;
import java.util.*;
import java.lang.reflect.*;
import static local.lito381.server.LiToGmCommands.*;

/** GM monsters reuse the complete authoritative NPC combat/status/AI engine. */
final class LiToGmNpcs {
    private static final Map<BattleTimeAttackEncounter,BattleRules.RuleSet> GM=Collections.synchronizedMap(new WeakHashMap<>());
    static boolean active(BattleTimeAttackEncounter e){return GM.containsKey(e);}
    static void clear(BattleTimeAttackEncounter e){GM.remove(e);}
    static BattleRules.Mode combatMode(BattleRules.RuleSet rules){
        synchronized(GM){for(var r:GM.values())if(r==rules)return BattleRules.Mode.TIME_ATTACK;}
        return rules.mode();
    }
    static int number(Map<String,String> row,String key,int fallback){String v=row.get(key);return v==null||v.isBlank()?fallback:Integer.parseInt(v);}
    static Map<String,String> npc(AccountRepository r,String token)throws Exception{
        try(Connection c=connection(r)){int id=resolve(r,c,token,true);
            try(var q=c.prepareStatement("SELECT * FROM "+table(r,"npc")+" WHERE npcid=?")){
                q.setInt(1,id);try(var rows=q.executeQuery()){
                    if(!rows.next())throw new IllegalArgumentException("找不到 NPC ID "+id+"。");
                    Map<String,String> result=new HashMap<>();var md=rows.getMetaData();for(int i=1;i<=md.getColumnCount();i++){String v=rows.getString(i);if(v!=null)result.put(md.getColumnLabel(i).toLowerCase(Locale.ROOT),v);}return result;
                }
            }
        }
    }
    static void spawn(ClientSession s,AccountRepository r,String token,int count,int range)throws Exception{
        if(count>100||range<0||range>20)throw new IllegalArgumentException("NPC 數量1～100，範圍0～20；每房最多100個 GM NPC。");
        if(!get(s,"phase").toString().equals("BATTLE")||(boolean)get(s,"observer")||(int)get(s,"playerHp")<=0)
            throw new IllegalArgumentException(".spawn 請在進入戰鬥地圖後使用，觀戰或死亡狀態無法建立。");
        GameHub hub=(GameHub)get(s,"hub");long roomId=(long)get(s,"currentRoomId");
        Map<String,String> row=npc(r,token);String impl=row.getOrDefault("impl","");
        if(!impl.equals("L1Monster"))throw new IllegalArgumentException("目前 .spawn 支援戰鬥怪物（L1Monster）；商人、守衛及特殊功能 NPC 尚未開放。");
        int npc=number(row,"npcid",0),gfx=number(row,"gfxid",0);
        BattleAnimationCatalog.get().requireBattleCapableSprite(gfx,BattleActionCodes.WALK);
        BattleAnimationCatalog.get().requireTimedAction(gfx,BattleActionCodes.ATTACK);
        var template=new BattleTimeAttackEncounter.Template(npc,gfx,row.getOrDefault("nameid",row.getOrDefault("name","NPC")),number(row,"lvl",1),number(row,"hp",1),number(row,"mp",0),number(row,"int",10));
        List<BattleTimeAttackEncounter.Snapshot> created=new ArrayList<>();List<ClientSession> members;
        synchronized(hub){
            Object room=((Map<?,?>)get(hub,"rooms")).get(roomId);
            if(room==null||!(boolean)get(room,"battleStarted"))throw new IllegalArgumentException("戰鬥已結束，沒有建立 NPC。");
            BattleRules.RuleSet rules=hub.roomRules(roomId);
            if(rules.mode()==BattleRules.Mode.TIME_ATTACK||rules.mode()==BattleRules.Mode.QUEST_BATTLE||rules.mode()==BattleRules.Mode.PET_RACE)
                throw new IllegalArgumentException("請在一般戰鬥模式使用 .spawn；競速／闖關／寵物賽的固定波次不接受額外 NPC。");
            BattleTimeAttackEncounter encounter=(BattleTimeAttackEncounter)get(room,"timeAttackEncounter");
            List<Object> actors=(List<Object>)get(encounter,"actors");
            if(actors.size()+count>100)throw new IllegalArgumentException("本場 GM NPC 已達100個上限（含屍體）。");
            ArenaMapCatalog.Arena arena=ArenaMapCatalog.forMap(rules.mapId());
            int px=(int)get(s,"positionX"),py=(int)get(s,"positionY");
            List<ArenaMapCatalog.Point> cells=new ArrayList<>();int radius=range==0?3:range;
            for(int y=py-radius;y<=py+radius;y++)for(int x=px-radius;x<=px+radius;x++){
                if(!arena.nativeStandingFloor(x,y)||encounter.blocks(x,y)||x==px&&y==py)continue;
                boolean occupied=false;for(ClientSession p:hub.roomMembers(roomId))if(p.battleX()==x&&p.battleY()==y){occupied=true;break;}
                if(!occupied)cells.add(new ArenaMapCatalog.Point(x,y));
            }
            if(cells.isEmpty())throw new IllegalArgumentException("附近沒有可放置 NPC 的空地。");
            Collections.shuffle(cells);
            Class<?> mc=Class.forName("local.lito381.server.BattleTimeAttackEncounter$Monster");
            Constructor<?> ctor=mc.getDeclaredConstructor(BattleTimeAttackEncounter.Template.class,int.class,int.class,String.class,String.class);ctor.setAccessible(true);
            List<Object> pending=new ArrayList<>();for(int i=0;i<Math.min(count,cells.size());i++){
                var p=cells.get(i);Object actor=ctor.newInstance(template,p.x(),p.y(),"selected-source-npc GM","GM one-shot, no wave progression");pending.add(actor);created.add((BattleTimeAttackEncounter.Snapshot)call(actor,"snapshot",new Class[]{}));
            }
            set(encounter,"arena",arena);set(encounter,"phase",BattleTimeAttackEncounter.Phase.FINAL_BOSSES);
            GM.put(encounter,rules);actors.addAll(pending);set(room,"pveAiPublishAfterNanos",System.nanoTime()+2_000_000_000L);
            members=hub.roomMembers(roomId);
        }
        // Publish once, through the same packet and target type used by ordinary NPCs.
        for(ClientSession p:members)for(var actor:created)call(p,"sendEncrypted",new Class[]{byte[].class,String.class},Protocol.timeAttackActor(actor),"GM NPC spawn="+actor.objectId());
        reply(s,"已建立 NPC "+row.getOrDefault("name",token)+"（"+npc+"）×"+created.size()+(created.size()<count?"；附近空地不足，已依可用位置減少數量。":"。"));
        System.out.println("[LiTo] GM spawn room="+roomId+" npc="+npc+" count="+created.size());
    }
    // A GM monster's death/capture must never complete a Time Attack match or spawn waves.
    static BattleTimeAttackEncounter.Hit damage(BattleTimeAttackEncounter e,Object actor,int amount,boolean poison){
        try{
            var d=(BattleTimeAttackEncounter.Damage)call(actor,poison?"poisonDamage":"damage",new Class[]{int.class},amount);
            if(d!=null&&d.diedNow())call(e,"rememberCorpse",new Class[]{BattleTimeAttackEncounter.Snapshot.class,long.class},d.after(),System.nanoTime());
            return new BattleTimeAttackEncounter.Hit(d,false,e.snapshots().size(),List.of());
        }catch(Exception ex){throw new IllegalStateException(ex);}
    }
    static BattleTimeAttackEncounter.Capture capture(BattleTimeAttackEncounter e,long id){
        try{var snapshot=e.find(id);if(snapshot==null)return null;List<Object> actors=(List<Object>)get(e,"actors");actors.removeIf(o->{try{return (long)get(o,"objectId")==id;}catch(Exception ex){throw new IllegalStateException(ex);}});return new BattleTimeAttackEncounter.Capture(snapshot,e.snapshots().size(),List.of());}
        catch(Exception ex){throw new IllegalStateException(ex);}
    }
}
