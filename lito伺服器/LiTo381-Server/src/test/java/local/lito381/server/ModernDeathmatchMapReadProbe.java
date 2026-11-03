package local.lito381.server;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Explicit whole shared-map candidates, read-only. Never enables a binding,
 * trims sectors to fit a budget, changes source files or loads LT resources. */
public final class ModernDeathmatchMapReadProbe {
    private ModernDeathmatchMapReadProbe() {}
    static Set<ModernWorldMap.SectorKey> sectors(int id) {
        var names = switch (id) {
            case 504 -> List.of("7ffd7fff","7ffd8000","7ffe7fff","7ffe8000","7ffe8001","7fff7fff","7fff8000","7fff8001","7fff8002","80007fff","80008000","80008001","80008002","80017ffd","80017ffe","80018000","80018001","80018002","80028000","80028001","80028002","80038000","80038001","80038002","80047ffe","80047fff","80048000","80057fff","80058000","80067fff","80068000");
            case 505 -> List.of("7ffd7fff","7ffd8000","7ffe7fff","7ffe8000","7ffe8001","7fff7fff","7fff8000","7fff8001","7fff8002","80007fff","80008000","80008001","80008002","80017ffd","80017ffe","80018000","80018001","80018002","80028000","80028001","80028002","80038000","80038001","80038002","80047ffe","80047fff","80048000","80057fff","80058000","80067fff","80068000");
            case 506 -> List.of("7ffc7fff","7ffc8000","7ffd7ffe","7ffd7fff","7ffd8000","7ffd8001","7ffe7fff","7ffe8000","7ffe8001","7fff7fff","7fff8000","7fff8001","7fff8002","80007fff","80008000","80008001","80008002","80017ffd","80017ffe","80018000","80018001","80018002","80028000","80028001","80028002","80038000","80038001","80038002","80047ffe","80047fff","80048000","80057fff","80058000","80067fff","80068000");
            case 508 -> List.of("80008005","80008006","80018005","80018006");
            case 531 -> List.of("7fff7fff","7fff8000","7fff8001","80007fff","80008000","80008001",
                    "80017fff","80018000","80018001","80018002");
            case 534 -> List.of("7fff8000","7fff8001","80008000","80008001","80018000","80018001","80028000","80028001");
            default -> throw new IllegalArgumentException("unselected whole shared map");
        };
        return names.stream().map(s -> new ModernWorldMap.SectorKey(Integer.parseInt(s.substring(0,4),16),
                Integer.parseInt(s.substring(4),16))).collect(Collectors.toUnmodifiableSet());
    }
    public static void main(String[] args) throws Exception {
        if(args.length<1 || args.length>3 || !Path.of(args[0]).isAbsolute()
                ||args.length==3&&!args[2].equals("geometry-only"))throw new IllegalArgumentException("explicit shared root required");
        var ids=args.length==1?List.of(508,531,534):java.util.Arrays.stream(args[1].split(",")).map(Integer::parseInt).toList();
        for(int id:ids) {
            var map=ModernWorldMap.load(Path.of(args[0]),id,sectors(id),()->false);
            var components=ModernDeathmatchSharedMapTest.components(map);
            var anchor=ModernDeathmatchSharedMapTest.centre(components.get(0));
            System.out.println("SHARED_DM_CANDIDATE map="+id+" canonical="+map.digest()+" directions="+ModernDeathmatchSharedMapTest.directionDigest(map)
                    +" standing="+map.teleportCells().size()+" components="+components.size()+" largest="+components.stream().map(Set::size).toList()
                    +" anchor="+anchor+" enabled=false nativeEquivalent=false");
            for(var sector:map.manifest().sectors())System.out.println("SHARED_DM_PIN map="+id+" sector="+sector.key().fileName()+" bytes="+sector.byteLength()+" sha256="+sector.rawSha256());
            if(args.length==3)continue; // Inspection only, no capacity admission.
            int completed=0;
            for(int size=2;size<=100;size++) {
                boolean passed=true;
                for(int humans:List.of(1,size/2,size)) {
                    var roster=ModernWorldSpawnsTest.roster(humans,size-humans);
                    try {
                        var plan=ModernWorldSpawns.allocate(map,roster,List.of(anchor),Set.of(),()->false);
                        ModernWorldSpawnsTest.assertPlan(map,roster,plan,Set.of());
                        completed++;
                    } catch(ModernWorldMap.MapException failure) {
                        System.out.println("SHARED_DM_LAYOUT_REJECTED map="+id+" size="+size+" humans="+humans+" reason="+failure.failure());
                        passed=false;break;
                    }
                }
                if(!passed)break;
            }
            System.out.println("SHARED_DM_LAYOUTS map="+id+" passed="+completed+" maximumExpected=297");
        }
    }
}
