package local.lito381.server;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.TreeSet;

/** Existing dragon-world/eye leases projected into source table abilities. */
final class CombatSpecialBuffs {
    private static final List<Integer> BLESSINGS=List.of(4009,4010,4016);
    private static final Map<String,String> EYE_ABILITIES=Map.of("meleeDamage","DMG","magicHit","MAGIC_HIT","damageReduction","DMG_REDUX",
            "evasionRate","ER","stoneResistance","REGIST_STONE","sleepResistance","REGIST_SLEEP","freezeResistance","REGIST_FREEZE","poisonResistance","POISON_RESIST");
    private CombatSpecialBuffs() { }
    static List<ClassicWorldNpcCatalog.StaticNpc> npcs(List<ClassicWorldNpcCatalog.StaticNpc> old,int map) {
        var controls=CombatDatabase.controlsOrNull();if(controls==null)return old;
        var added=controls.blessings().npcs().getOrDefault(map,List.of());
        // The pinned core only embeds Saell, on its original map1011.
        if(added.isEmpty()&&map!=ClassicWorldWaterDragonCatalog.MAP_ID)return old;
        var result=new ArrayList<ClassicWorldNpcCatalog.StaticNpc>();for(var npc:old)if(npc.npcId()!=70670&&npc.npcId()!=70689)result.add(npc);result.addAll(added);return List.copyOf(result);
    }
    static ClassicWorldMapCatalog.ClassicMap sourceMap(int map) {var c=CombatDatabase.controlsOrNull();return c==null?null:c.blessings().maps().get(map);}
    static boolean mapSupported(boolean old,int map){return old||sourceMap(map)!=null;}
    static List<Integer> mapIds(List<Integer> old){var c=CombatDatabase.controlsOrNull();if(c==null||c.blessings().maps().isEmpty())return old;var result=new ArrayList<>(old);for(int id:new TreeSet<>(c.blessings().maps().keySet()))if(!result.contains(id))result.add(id);return List.copyOf(result);}
    static boolean blessingNpc(boolean old,ClassicWorldNpcCatalog.StaticNpc npc) {
        var c=CombatDatabase.controlsOrNull();if(c==null)return old;if(npc==null)return false;
        return c.blessings().npcs().getOrDefault(npc.mapId(),List.of()).contains(npc);
    }
    /** Source Npc_Cray/Npc_Saell ordering: remove other blessings before no-refresh. */
    @SuppressWarnings("unchecked") static boolean tryTalk(ClientSession caster,ClassicWorldNpcCatalog.StaticNpc npc) {
        if(!CombatBuffLeases.loaded())return false;
        if(!blessingNpc(false,npc)||!((Enum<?>)CombatAccess.field(caster,"phase")).name().equals("WORLD")
                ||(boolean)CombatAccess.field(caster,"observer")||((Number)CombatAccess.field(caster,"playerHp")).intValue()<=0
                ||((Number)CombatAccess.field(caster,"classicWorldMapId")).intValue()!=npc.mapId())return true;
        int x=((Number)CombatAccess.field(caster,"positionX")).intValue(),y=((Number)CombatAccess.field(caster,"positionY")).intValue();
        if(Math.max(Math.abs(x-NativeStaticNpcMotion.x(npc,caster)),Math.abs(y-NativeStaticNpcMotion.y(npc,caster)))>5)return true;
        synchronized(caster) {
            int skill=npc.npcId()==70670?4009:4010;var state=story(caster);boolean changed=false;
            for(int id:BLESSINGS)if(id!=skill)changed|=state.remove(id)!=null;
            long now=System.nanoTime();boolean started=startBlessing(caster,skill,2400);if(!started&&!changed)return true;
            if(!started)CombatAccess.call(caster,"rebuildMaximumVitals","(J)V",now);
            if(started) {
                byte[] effect=Protocol.effectObject(caster.playerObjectId(),skill==4009?7681:7680);
                var members=(List<ClientSession>)CombatAccess.call(caster,"classicWorldMembers","(I)Ljava/util/List;",npc.mapId());
                for(var member:members)if(Math.max(Math.abs(x-((Number)CombatAccess.field(member,"positionX")).intValue()),Math.abs(y-((Number)CombatAccess.field(member,"positionY")).intValue()))<=8)
                    send(member,effect,"S_EFFECT source-dragon-blessing skill="+skill);
            }
            int hp=((Number)CombatAccess.field(caster,"playerHp")).intValue(),maxHp=((Number)CombatAccess.field(caster,"playerMaxHp")).intValue();
            int mp=((Number)CombatAccess.field(caster,"playerMp")).intValue(),maxMp=((Number)CombatAccess.field(caster,"playerMaxMp")).intValue();
            send(caster,new Protocol.Writer().c(Protocol.S_HIT_POINT).h(hp).h(maxHp).bytes(),"S_HIT_POINT source-dragon-blessing="+hp+"/"+maxHp);
            CombatAccess.call(caster,"broadcastOwnHitRatio","(Ljava/lang/String;)V","source-dragon-blessing-max-hp");
            send(caster,Protocol.manaPoint(mp,maxMp),"S_MANA_POINT source-dragon-blessing="+mp+"/"+maxMp);
            send(caster,(byte[])CombatAccess.call(caster,"ownCharacterAttrDef","()[B"),"S_AC source-dragon-blessing="+caster.battleArmorClass());
            CombatAccess.call(caster,"sendNativeBattleStatus","(JLjava/lang/String;)V",now,"source-dragon-blessing");
        }
        return true;
    }
    private static void send(ClientSession caster,byte[] packet,String label){CombatAccess.call(caster,"sendEncrypted","([BLjava/lang/String;)V",packet,label);}
    @SuppressWarnings("unchecked") static Map<Integer,Long> story(ClientSession caster){return (Map<Integer,Long>)CombatAccess.field(caster,"storySkillEffectDeadlines");}
    static boolean active(Map<Integer,Long> state,int id,long now){Long deadline=state.get(id);return deadline!=null&&now<deadline;}
    static BattleDragonEyeStatus.Snapshot eye(ClientSession caster,long now){return ((BattleDragonEyeStatus)CombatAccess.field(caster,"dragonEyeStatus")).snapshot(now);}
    private static SourceEyeControls.Config config(BattleDragonEyeCatalog.Eye eye) {
        var source=CombatDatabase.controlsOrNull();if(source==null)return null;
        var result=source.eyes().get(eye.itemId());if(result==null)throw new IllegalStateException("unbound source eye="+eye.itemId());return result;
    }
    static int eyeField(int old,BattleDragonEyeCatalog.Eye eye,String field) {
        var config=config(eye);if(config==null)return old;
        String ability=EYE_ABILITIES.get(field);if(ability!=null)return SourceSkillControls.ability(CombatDatabase.controls(),eye.buffId(),ability);
        return switch(field) {case "buffId"->config.buff();case "durationSeconds"->config.seconds();case "castGfx"->config.gfx();case "statusIcon"->config.icon();
            case "sharedDelayId"->config.sharedDelay();case "sharedDelayMillis"->config.sharedMillis();case "itemDelayMillis"->config.itemMillis();case "canOverwrite"->config.overwrite()?1:0;
            default->throw new IllegalArgumentException("unknown eye field="+field);};
    }
    static String eyeText(String old,BattleDragonEyeCatalog.Eye eye,String field) {
        var config=config(eye);if(config==null)return old;
        return field.equals("group")?config.group():config.endMessage()==null?"":config.endMessage();
    }
    private static int row(SourceSkillControls.Snapshot source,int id,String type){return SourceSkillControls.ability(source,id,type);}
    static int storyAbility(ClientSession caster,String type,long now) {
        var source=CombatDatabase.controlsOrNull();if(source==null)return 0;
        int value=0;var state=story(caster);for(int id:BLESSINGS)if(active(state,id,now))value=Math.addExact(value,row(source,id,type));return value;
    }
    static int ability(ClientSession caster,String type,long now) {
        int value=storyAbility(caster,type,now);var eye=eye(caster,now);
        if(CombatBuffLeases.loaded()&&eye.active())value=Math.addExact(value,row(CombatDatabase.controls(),eye.eye().buffId(),type));return value;
    }
    static long add(long old,ClientSession caster,String type){return Math.addExact(old,(long)ability(caster,type,System.nanoTime()));}
    static int replace(int old,ClientSession caster,long now,String type){return CombatBuffLeases.loaded()?ability(caster,type,now):old;}
    static int addScalar(int old,ClientSession caster,long now,String type) {
        if(!CombatBuffLeases.loaded())return old;
        int value=type.equals("DMG_REDUX")?storyAbility(caster,type,now):ability(caster,type,now);
        if(type.equals("SP")){var eye=eye(caster,now);if(eye.active()&&List.of(6686,6687,6689).contains(eye.eye().buffId()))value=Math.addExact(value,1);}
        return Math.addExact(old,value);
    }
    static int legacy(int old){return CombatBuffLeases.loaded()?0:old;}
    static BattleWeaponBuffStatus.Modifier weapon(BattleWeaponBuffStatus.Modifier old,ClientSession caster,long now) {
        if(!CombatBuffLeases.loaded())return old;
        int hit=old.meleeHit(),damage=old.meleeDamage(),bowHit=old.rangedHit(),bowDamage=old.rangedDamage();
        var state=story(caster);var source=CombatDatabase.controls();
        for(int id:BLESSINGS)if(active(state,id,now)) {
            hit=Math.addExact(hit,row(source,id,"HIT"));damage=Math.addExact(damage,row(source,id,"DMG"));
            bowHit=Math.addExact(bowHit,row(source,id,"BOW_HIT"));bowDamage=Math.addExact(bowDamage,row(source,id,"BOW_DMG"));
        }
        var eye=eye(caster,now);if(eye.active()) {
            int id=eye.eye().buffId();hit=Math.addExact(hit,row(source,id,"HIT"));damage=Math.addExact(damage,row(source,id,"DMG"));
            bowHit=Math.addExact(bowHit,row(source,id,"BOW_HIT"));bowDamage=Math.addExact(bowDamage,row(source,id,"BOW_DMG"));
        }
        return new BattleWeaponBuffStatus.Modifier(old.skillId(),hit,damage,bowHit,bowDamage,old.remainingSeconds());
    }
    static long element(long old,ClientSession caster,int kind,long now) {
        if(!CombatBuffLeases.loaded())return old;
        var state=story(caster);int direct=active(state,4016,now)?30:0;
        if(kind==1&&active(state,4009,now)||kind==4&&active(state,4010,now))direct=Math.addExact(direct,30);
        String type=switch(kind){case 1->"DEF_EARTH";case 2->"DEF_FIRE";case 4->"DEF_WATER";case 8->"DEF_WIND";default->throw new IllegalArgumentException("non-classic element");};
        return Math.addExact(old,Math.addExact(direct,ability(caster,type,now)));
    }
    static int evasion(int old,ClientSession caster,long now) {
        if(!CombatBuffLeases.loaded())return old;var state=story(caster);int direct=0;
        if(active(state,4009,now))direct+=30;if(active(state,4010,now))direct+=15;
        return Math.addExact(old,Math.addExact(direct,storyAbility(caster,"ER",now)));
    }
    static int weight(int old,ClientSession caster,long now){return !CombatBuffLeases.loaded()?old:Math.addExact(old,active(story(caster),4009,now)?4:0);}
    static boolean expire(boolean old,ClientSession caster,long now) {
        if(!CombatBuffLeases.loaded())return old;var state=story(caster);boolean removed=old;
        for(int id:BLESSINGS){Long deadline=state.get(id);if(deadline!=null&&now>=deadline)removed|=state.remove(id,deadline);}return removed;
    }
    /** Source SkillMode boundary; NPC producers choose duration and exclusions. */
    static boolean startBlessing(ClientSession caster,int id,int seconds) {
        if(!BLESSINGS.contains(id)||seconds<1||seconds>65535)throw new IllegalArgumentException("invalid dragon-world blessing");
        synchronized(caster){long now=System.nanoTime();var state=story(caster);if(active(state,id,now))return false;
            state.put(id,Math.addExact(now,seconds*1_000_000_000L));CombatAccess.call(caster,"rebuildMaximumVitals","(J)V",now);return true;}
    }
    static Object eyePhase(Object current,Object battle,ClientSession caster){return CombatBuffLeases.loaded()&&((Enum<?>)current).name().equals("WORLD")?battle:current;}
    @SuppressWarnings("unchecked") static List<ClientSession> eyeRecipients(GameHub hub,long room,ClientSession caster) {
        if(CombatBuffLeases.loaded()&&((Enum<?>)CombatAccess.field(caster,"phase")).name().equals("WORLD"))
            return (List<ClientSession>)CombatAccess.call(caster,"classicWorldMembers","(I)Ljava/util/List;",((Number)CombatAccess.field(caster,"classicWorldMapId")).intValue());
        return hub.roomMembers(room);
    }
}
