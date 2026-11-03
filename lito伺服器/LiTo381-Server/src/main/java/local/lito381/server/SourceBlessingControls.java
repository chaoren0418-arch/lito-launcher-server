package local.lito381.server;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Immutable source NPC/map metadata only; no player or encounter state. */
final class SourceBlessingControls {
    record Snapshot(Map<Integer,List<ClassicWorldNpcCatalog.StaticNpc>> npcs,Map<Integer,ClassicWorldMapCatalog.ClassicMap> maps,Map<Path,String> mapHashes) {
        Snapshot {var copy=new HashMap<Integer,List<ClassicWorldNpcCatalog.StaticNpc>>();npcs.forEach((id,list)->copy.put(id,List.copyOf(list)));npcs=Map.copyOf(copy);maps=Map.copyOf(maps);mapHashes=Map.copyOf(mapHashes);}
        static Snapshot empty(){return new Snapshot(Map.of(),Map.of(),Map.of());}
    }
    static Snapshot read(Connection c)throws SQLException {
        var npcs=new HashMap<Integer,List<ClassicWorldNpcCatalog.StaticNpc>>();var maps=new HashMap<Integer,ClassicWorldMapCatalog.ClassicMap>();var hashes=new HashMap<Path,String>();
        try(var st=c.createStatement();var r=st.executeQuery("SELECT id,event_type,action_name FROM `系統_劇情任務` WHERE enabled=1 AND npc_id IN (70670,70689)")) {
            while(r.next())if("NPC_TALK".equals(r.getString("event_type"))||"NPC_ACTION".equals(r.getString("event_type"))&&"talk".equals(r.getString("action_name")))
                throw new SQLException("source blessing NPC talk is overridden by story rule="+r.getInt("id")+"; native adapter must handle the override first");
        }
        String query="SELECT s.*,n.nameid,n.classname,n.gfxid,n.lvl,n.hp,n.mp,n.exp,n.lawful,n.light_size,n.passispeed,n.atkspeed,n.change_head FROM spawnlist_npc s INNER JOIN npc n ON n.npcid=s.npc_templateid WHERE s.npc_templateid IN (70670,70689) AND s.count>0 ORDER BY s.id";
        try(var st=c.createStatement();var r=st.executeQuery(query)) {
            while(r.next()) {
                int id=r.getInt("npc_templateid"),spawn=r.getInt("id"),map=r.getInt("mapid");
                if(r.getInt("count")!=1||r.getInt("randomx")!=0||r.getInt("randomy")!=0||r.getInt("movement_distance")!=0)
                    throw new SQLException("blessing NPC="+id+": unsupported randomized/moving/multiple spawn profile");
                String impl=r.getString("classname");if(!(id==70670?"quest2.Npc_Cray":"quest2.Npc_Saell").equals(impl))throw new SQLException("unexpected blessing NPC executor="+impl);
                var npc=new ClassicWorldNpcCatalog.StaticNpc(map,spawn,ClassicWorldNpcCatalog.objectId(spawn),id,r.getInt("locx"),r.getInt("locy"),r.getInt("heading"),0,
                        r.getString("nameid"),impl,r.getInt("gfxid"),r.getInt("lvl"),r.getInt("hp"),r.getInt("mp"),r.getLong("exp"),r.getInt("lawful"),r.getInt("light_size"),r.getInt("passispeed"),r.getInt("atkspeed"),r.getBoolean("change_head"),"","");
                npcs.computeIfAbsent(map,key->new ArrayList<>()).add(npc);
                if(ClassicWorldMapCatalog.embeddedSupports(map)&&!ClassicWorldMapCatalog.forMap(map).contains(npc.x(),npc.y()))throw new SQLException("blessing NPC outside native map="+map);
            }
        } catch(IllegalArgumentException bad){throw new SQLException("invalid source blessing NPC",bad);}
        for(int map:npcs.keySet())if(!ClassicWorldMapCatalog.embeddedSupports(map)) {
            try(var st=c.prepareStatement("SELECT mapid,startX,endX,startY,endY,CopyMapId,markable,teleportable,escapable,penalty,recall_pets FROM mapids WHERE mapid=?")) {
                st.setInt(1,map);try(var r=st.executeQuery()) {
                    if(!r.next())throw new SQLException("missing source blessing map="+map);int physical=r.getInt("CopyMapId");if(physical<=0)physical=map;
                    Path directory=CombatRules.sourceMapDirectory().toAbsolutePath().normalize(),file=directory.resolve(physical+".txt").toRealPath();
                    if(!file.startsWith(directory.toRealPath()))throw new SQLException("source blessing map escaped configured directory");
                    var definition=new LitoMapRepository.Definition(map,physical,r.getInt("startX"),r.getInt("endX"),r.getInt("startY"),r.getInt("endY"),r.getBoolean("markable"),r.getBoolean("teleportable"),r.getBoolean("escapable"),r.getBoolean("penalty"),r.getBoolean("recall_pets"),file);
                    var loaded=LitoMapRepository.loadTextMap(definition);for(var npc:npcs.get(map))if(!loaded.contains(npc.x(),npc.y()))throw new SQLException("blessing NPC outside source map="+map);
                    maps.put(map,loaded);hashes.put(file,loaded.sourceSha256());
                }
            } catch(java.io.IOException bad){throw new SQLException("cannot load source blessing map="+map,bad);}
        }
        return new Snapshot(npcs,maps,hashes);
    }
}
