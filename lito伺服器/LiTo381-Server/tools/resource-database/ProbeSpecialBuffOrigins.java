package local.lito381.server;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Read-only origin evidence, including encoded native story events. */
public class ProbeSpecialBuffOrigins {
    static void rows(Connection c,String query)throws Exception {
        try(var st=c.createStatement();var r=st.executeQuery(query)) {
            var md=r.getMetaData();int count=0;while(r.next()) {count++;var values=new ArrayList<String>();for(int i=1;i<=md.getColumnCount();i++)values.add(md.getColumnName(i)+"="+r.getString(i));System.out.println(String.join("\t",values));}System.out.println("QUERY_ROWS\t"+count+"\t"+query);
        }
    }
    public static void main(String[] args)throws Exception {
        var config=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(config);
        var inputs=new ArrayList<String>();CombatDatabase.controls().blessings().mapHashes().forEach((path,hash)->inputs.add(path+"\t"+hash.toLowerCase(Locale.ROOT)));Files.write(Path.of(args[1]),inputs);
        System.out.println("MAP_CONFIG\tmode="+config.databaseMode()+" compatibility="+config.classicWorldCompatibilityEnabled()+" directory="+config.mapDirectory());
        try(var maps=LitoMapRepository.open(config)) {
            maps.healthCheck();ClassicWorldMapCatalog.installExternalMaps(maps);
            System.out.println("SOURCE_MAP1005\tsupported="+ClassicWorldMapCatalog.supports(1005));
            if(ClassicWorldMapCatalog.supports(1005))System.out.println("SOURCE_MAP1005\tcray-in-bounds="+ClassicWorldMapCatalog.forMap(1005).contains(32807,32925));
            ClassicWorldMapCatalog.removeExternalMaps(maps);
        }
        var field=ClassicWorldNpcCatalog.class.getDeclaredField("BY_MAP");field.setAccessible(true);
        var maps=(Map<Integer,List<ClassicWorldNpcCatalog.StaticNpc>>)field.get(null);
        var ids=new TreeSet<>(maps.keySet());ids.addAll(CombatDatabase.controls().blessings().npcs().keySet());System.out.println("NATIVE_NPC_MAP_IDS\t"+ids);
        ids.stream().flatMap(id->ClassicWorldNpcCatalog.all(id).stream()).filter(n->n.npcId()==70670||n.npcId()==70689).forEach(n->System.out.println("NATIVE_NPC\t"+n));
        for(var event:ClassicWorldStoryQuestCatalog.all()) {
            var effect=event.setSkillEffect();if(effect!=null&&Set.of(4009,4010,4016).contains(effect.getKey()))System.out.println("NATIVE_STORY_EFFECT\t"+event);
        }
        var properties=UnifiedConfiguration.read(config.source());String database=SharedDatabase.identifier(properties.getProperty("database.static.database"));
        try(var c=DriverManager.getConnection(properties.getProperty("database.jdbc.base.url")+database+properties.getProperty("database.jdbc.options"),properties.getProperty("database.user"),properties.getProperty("database.password"))) {
            c.setReadOnly(true);c.setAutoCommit(false);
            rows(c,"SELECT npcid,name,nameid,classname,gfxid FROM npc WHERE npcid IN (70670,70689)");
            rows(c,"SELECT * FROM spawnlist_npc WHERE npc_templateid IN (70670,70689)");
            rows(c,"SELECT * FROM `etcitem_buff藥水設置` WHERE buff_id IN (4009,4010,4016)");
            rows(c,"SELECT * FROM skills WHERE skill_id IN (4009,4010,4016)");
            rows(c,"SELECT skill_id,name,effect_skill_id FROM skills WHERE effect_skill_id IN (4009,4010,4016)");
            rows(c,"SELECT id,quest_id,npc_id,event_type,params FROM `系統_劇情任務` WHERE enabled=1 AND params REGEXP '(^|[^0-9])(4009|4010|4016)([^0-9]|$)'");
            rows(c,"SELECT id,npc_id,event_type,action_name FROM `系統_劇情任務` WHERE enabled=1 AND npc_id IN (70670,70689)");
            rows(c,"SELECT mapid,startX,endX,startY,endY,CopyMapId,markable,teleportable,escapable,penalty,recall_pets FROM mapids WHERE mapid IN (1005,1011)");
            c.rollback();
        }
    }
}
