package local.lito381.server;

import java.nio.file.Path;
import java.util.*;

/** Central read-only exploration of all six actual shared map532 sectors. No SQL/listener/LT. */
public final class ModernMap532ReadProbe {
    static final int[] DX={0,1,1,1,0,-1,-1,-1}, DY={-1,-1,0,1,1,1,0,-1};
    static final int[] LENGTHS={125861,103877,150512,126210,149410,136185};
    static final String[] HASHES={
        "d35e6f1da1c1012e68f05cdc0b6b830040fea41fdb13f24f4596a62dfeab0db3",
        "147e88b94f0f0f4a3277a1e9166436e534ee2f455cd8cdbe2da39778f0cca931",
        "56bf52953d3483f3ba9399fac9b935b2059486713ee42412626bcd5a7ecb5899",
        "77c86ad27d63e457c32aba1502ce95d397b00590e737b20bad243a035b617d9f",
        "409856e787efd3dea1cd9f7a9c450d2778d3c877cf83ec51423e91b5ba39776a",
        "121e8853304cc849abbdd8e8e696d32ed1b0bd82ed4d538155f8891c9445591a"};
    private ModernMap532ReadProbe() {}
    static ModernWorldMap read(Path root) throws Exception {
        var keys=new HashSet<ModernWorldMap.SectorKey>();
        for(int x=0x7fff;x<=0x8001;x++) for(int y=0x8000;y<=0x8001;y++) keys.add(new ModernWorldMap.SectorKey(x,y));
        var map=ModernWorldMap.load(root,532,keys,()->false);
        for(int i=0;i<6;i++) {
            var sector=map.manifest().sectors().get(i);
            if(sector.byteLength()!=LENGTHS[i] || !sector.rawSha256().equals(HASHES[i]))
                throw new IllegalStateException("shared map532 raw witness changed");
        }
        return map;
    }
    static LinkedHashMap<ModernWorldMap.Cell,Integer> component(ModernWorldMap map,ModernWorldMap.Cell anchor) {
        var seen=new LinkedHashMap<ModernWorldMap.Cell,Integer>();
        var queue=new ArrayDeque<ModernWorldMap.Cell>();seen.put(anchor,0);queue.add(anchor);
        while(!queue.isEmpty()) {
            var p=queue.remove();
            for(int i=0;i<8;i++) {
                var q=new ModernWorldMap.Cell(p.x()+DX[i],p.y()+DY[i]);
                if(!seen.containsKey(q) && map.canStep(p,q)){seen.put(q,seen.get(p)+1);queue.add(q);}
            }
            if(seen.size()>6*4096)throw new IllegalStateException("map component bound");
        }
        return seen;
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=1 || !Path.of(args[0]).isAbsolute())throw new IllegalArgumentException();
        var map=read(Path.of(args[0]));
        var standing=map.teleportCells();var remaining=new HashSet<>(standing);
        var components=new ArrayList<LinkedHashMap<ModernWorldMap.Cell,Integer>>();
        for(var p:standing)if(remaining.contains(p)){var group=component(map,p);remaining.removeAll(group.keySet());components.add(group);}
        components.sort(Comparator.comparingInt((LinkedHashMap<ModernWorldMap.Cell,Integer> g)->g.size()).reversed());
        var biggest=components.get(0);
        var center=biggest.keySet().stream().min(Comparator.comparingLong((ModernWorldMap.Cell p)->
            (long)(p.x()-32800)*(p.x()-32800)+(long)(p.y()-32832)*(p.y()-32832)).thenComparingInt(ModernWorldMap.Cell::x).thenComparingInt(ModernWorldMap.Cell::y)).orElseThrow();
        map.validateSpawns(map.serverChosenSpawns(center,100));
        System.out.println("map=532 sectors=6 canonical="+map.digest()+" standing="+standing.size()+" components="+components.size()+" anchor="+center+" mask="+map.directionMask(center));
        for(int i=0;i<Math.min(5,components.size());i++)System.out.println("component "+i+" cells="+components.get(i).size());
        var reached=component(map,center);
        var far=reached.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow();
        var opposite=component(map,far.getKey()).entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow();
        System.out.println("farthest candidates NOT configured tower/spawns: "+far.getKey()+" / "+opposite.getKey()+" distance="+opposite.getValue());
        for(var sector:map.manifest().sectors()) {
            var o=sector.key().origin();var masks=new byte[4096];int count=0;
            for(int y=0;y<64;y++)for(int x=0;x<64;x++){var p=new ModernWorldMap.Cell(o.x()+x,o.y()+y);int m=map.directionMask(p);masks[y*64+x]=(byte)m;if((m&85)!=0)count++;}
            System.out.println(sector.key().fileName()+" standing="+count+" directionSHA="+HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(masks)));
        }
    }
}
