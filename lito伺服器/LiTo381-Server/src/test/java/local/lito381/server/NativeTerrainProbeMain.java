package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Pure-byte candidate experiment with explicit Native pins; never a production namespace alias. */
public final class NativeTerrainProbeMain {
    public static void main(String[] args)throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("Pinned Native inputs, owned output required");
        var p=new Properties();try(var r=Files.newBufferedReader(Path.of(args[0]),StandardCharsets.UTF_8)){p.load(r);}
        if(!"NATIVE_LITO_LTMAP_V1".equals(p.getProperty("source.namespace")))throw new IllegalArgumentException("Explicit Native namespace required");
        Path root=Path.of(p.getProperty("source.root")).toRealPath();
        var result=new LinkedHashMap<String,Object>();result.put("namespace",p.getProperty("source.namespace"));
        result.put("scope","Pinned original LTmap bytes through unchanged Java pure S32 parser; no production registration, Shared alias or playable/rendering acceptance");
        var rows=new ArrayList<Object>();
        for(String text:p.getProperty("maps").split(",")) {
            int id=Integer.parseInt(text);var row=new LinkedHashMap<String,Object>();rows.add(row);row.put("map",id);
            var inputs=new LinkedHashMap<ModernWorldMap.SectorKey,byte[]>();var raw=new ArrayList<Object>();
            for(String token:p.getProperty("map."+id+".sectors").split(",")) {
                if(!token.matches("[0-9a-f]{8}"))throw new IllegalArgumentException("Invalid pin key");
                var key=new ModernWorldMap.SectorKey(Integer.parseInt(token.substring(0,4),16),Integer.parseInt(token.substring(4),16));
                Path file=root.resolve(Integer.toString(id)).resolve(key.fileName());
                if(!file.toRealPath().equals(file.toAbsolutePath().normalize()))throw new IllegalArgumentException("Aliased source leaf");
                String[] pin=p.getProperty("map."+id+"."+token).split(":");long bytes=Files.size(file);
                if(bytes!=Long.parseLong(pin[0]) || bytes>ModernWorldMap.MAX_SECTOR_BYTES)throw new IllegalArgumentException("Pinned sector size changed");
                byte[] data=Files.readAllBytes(file);
                if(!hash(data).equalsIgnoreCase(pin[1]))throw new IllegalArgumentException("Pinned sector SHA changed");
                inputs.put(key,data);raw.add(Map.of("sector",token,"bytes",data.length,"sha256",hash(data)));
            }
            row.put("raw_pins",raw);
            try{ModernWorldMap.load(root,id,inputs.keySet(),()->false);row.put("existing_path_loader","unexpectedly admitted");}
            catch(ModernWorldMap.MapException e){row.put("existing_path_loader",e.failure().name());}
            final ModernWorldMap map;
            try{map=ModernWorldMap.fromSectorBytes(id,inputs,()->false);}
            catch(ModernWorldMap.MapException e){row.put("pure_parser",e.failure().name());continue;}
            row.put("pure_parser","passed");row.put("canonical_sha256",map.digest());row.put("standable_cells",map.teleportCells().size());
            var arena=ArenaMapCatalog.forMap(id);var births=arena.spawns().stream().map(s->new ModernWorldMap.Cell(s.x(),s.y())).toList();
            row.put("native_builtin_spawns",births);row.put("native_spawn_standing",births.stream().allMatch(s->arena.nativeStandingFloor(s.x(),s.y())));
            row.put("modern_spawn_standing",births.stream().allMatch(map::standing));
            try{map.validateSpawns(births);row.put("native_spawns_one_modern_component",true);}catch(ModernWorldMap.MapException e){row.put("native_spawns_one_modern_component",false);row.put("spawn_failure",e.failure().name());}
            var capacities=new LinkedHashMap<String,Object>();
            for(int capacity:List.of(2,8,100))try{var chosen=map.serverChosenSpawns(births.get(0),capacity);map.validateSpawns(chosen);capacities.put(Integer.toString(capacity),true);}catch(ModernWorldMap.MapException e){capacities.put(Integer.toString(capacity),e.failure().name());}
            row.put("modern_distinct_bfs_capacity",capacities);row.put("capacity_scope","Original built-in eight coordinates are separate from modern deterministic distinct BFS capacity policy");
            long tested=0,cardinalMismatch=0,modernOnly=0,nativeOnly=0;var differences=new ArrayList<Object>();
            int[] dx={0,1,1,1,0,-1,-1,-1},dy={-1,-1,0,1,1,1,0,-1};
            for(var bounds:map.sectorBounds())for(int y=bounds.origin().y();y<bounds.origin().y()+64;y++)for(int x=bounds.origin().x();x<bounds.origin().x()+64;x++)for(int h=0;h<8;h++) {
                var from=new ModernWorldMap.Cell(x,y);var to=new ModernWorldMap.Cell(x+dx[h],y+dy[h]);if(!map.contains(to))continue;
                boolean modern=map.canStep(from,to),nativeStep=arena.nativeCanStep(x,y,to.x(),to.y());tested++;
                if(modern!=nativeStep){if(h%2==0)cardinalMismatch++;if(modern)modernOnly++;else nativeOnly++;if(differences.size()<20)differences.add(Map.of("from",from,"to",to,"heading",h,"modern",modern,"native",nativeStep));}
            }
            row.put("collision_comparisons",tested);row.put("cardinal_mismatches",cardinalMismatch);row.put("modern_only_edges",modernOnly);row.put("native_only_edges",nativeOnly);row.put("first_differences",differences);
        }
        result.put("maps",rows);result.put("gui_acceptance",false);
        Files.writeString(Path.of(args[1]),json(result),StandardCharsets.UTF_8);
        System.out.println("[native-terrain-probe] pinned maps="+rows.size()+"; inspect per-map format/geometry/policy result");
    }
    static String hash(byte[] bytes)throws Exception{return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    static String json(Object value)throws Exception {
        if(value==null)return "null";if(value instanceof String s)return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";
        if(value instanceof Number||value instanceof Boolean)return value.toString();
        if(value instanceof Map<?,?> m){var a=new ArrayList<String>();for(var e:m.entrySet())a.add(json(e.getKey().toString())+":"+json(e.getValue()));return "{"+String.join(",",a)+"}";}
        if(value instanceof Iterable<?> it){var a=new ArrayList<String>();for(var v:it)a.add(json(v));return "["+String.join(",",a)+"]";}
        if(value.getClass().isRecord()){var m=new LinkedHashMap<String,Object>();for(var c:value.getClass().getRecordComponents()){var method=c.getAccessor();method.setAccessible(true);m.put(c.getName(),method.invoke(value));}return json(m);}
        throw new IllegalArgumentException("Unsupported evidence value");
    }
}
