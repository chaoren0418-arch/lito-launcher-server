package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

/** Geometry component evidence only; never registers a mode binding or chooses spawns. */
public final class ModernRemainingMapResourceAuditMain {
    public static void main(String[] args)throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("shared root, physical IDs, owned output file required");
        Path root=Path.of(args[0]).toRealPath();var ids=Arrays.stream(args[1].split(",")).map(Integer::parseInt).sorted().toList();
        if(ids.size()>40 || new HashSet<>(ids).size()!=ids.size())throw new IllegalArgumentException("Invalid bounded resource selection");
        var rows=new ArrayList<String>();
        for(int id:ids) {
            var keys=new HashSet<ModernWorldMap.SectorKey>();
            try(var files=Files.list(root.resolve("map").resolve(Integer.toString(id)))) {
                for(var file:files.toList()) {
                    String name=file.getFileName().toString();
                    if(name.matches("[0-9a-f]{8}\\.s32"))keys.add(new ModernWorldMap.SectorKey(Integer.parseInt(name.substring(0,4),16),Integer.parseInt(name.substring(4,8),16)));
                }
            }
            try {
                var map=ModernWorldMap.load(root,id,keys,()->false);
                String pins=map.manifest().sectors().stream().map(s->"{\"sector\":\""+String.format("%04x%04x",s.key().x(),s.key().y())+"\",\"bytes\":"+s.byteLength()+",\"sha256\":\""+s.rawSha256()+"\"}").collect(Collectors.joining(","));
                rows.add("{\"physical_map\":"+id+",\"geometry_parse\":\"passed\",\"manifest_sha256\":\""+map.digest()+"\",\"standable_cells\":"+map.teleportCells().size()+",\"sector_pins\":["+pins+"],\"mode_qualified\":false}");
            }catch(ModernWorldMap.MapException failure){rows.add("{\"physical_map\":"+id+",\"geometry_parse\":\"failed\",\"failure\":\""+failure.failure()+"\",\"mode_qualified\":false}");}
        }
        Files.writeString(Path.of(args[2]),"{\"scope\":\"Shared physical geometry parse/pins only; no Native alias, player spawn, encounter, objective or race qualification\",\"rows\":["+String.join(",",rows)+"]}",StandardCharsets.UTF_8);
        System.out.println("[remaining-resource-components] physical maps observed="+rows.size());
    }
}
