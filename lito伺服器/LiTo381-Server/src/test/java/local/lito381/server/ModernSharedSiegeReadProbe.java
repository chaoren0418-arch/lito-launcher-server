package local.lito381.server;

import java.nio.file.Path;
import java.util.*;

/** Read-only shared Lineage castle exploration. No LT, listener, SQL or file writes. */
public final class ModernSharedSiegeReadProbe {
    static Set<ModernWorldMap.Cell> region(ModernWorldMap map,ModernWorldMap.Cell start,Set<ModernWorldMap.Cell> blocked) {
        if(!map.standing(start)||blocked.contains(start))return Set.of();
        var seen=new HashSet<ModernWorldMap.Cell>();var queue=new ArrayDeque<ModernWorldMap.Cell>();seen.add(start);queue.add(start);
        while(!queue.isEmpty()) {
            var a=queue.remove();
            for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++) {
                var b=new ModernWorldMap.Cell(a.x()+dx,a.y()+dy);
                if(!map.canStep(a,b)||blocked.contains(b))continue;
                if(dx!=0&&dy!=0){var h=new ModernWorldMap.Cell(b.x(),a.y());var v=new ModernWorldMap.Cell(a.x(),b.y());
                    if(!(!blocked.contains(h)&&map.canStep(a,h)&&map.canStep(h,b)||!blocked.contains(v)&&map.canStep(a,v)&&map.canStep(v,b)))continue;}
                if(seen.add(b))queue.add(b);
            }
            if(seen.size()>65_536)throw new IllegalStateException("bounded exploration");
        }
        return seen;
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=1||!Path.of(args[0]).isAbsolute())throw new IllegalArgumentException("explicit absolute shared root required");
        var keys=new HashSet<ModernWorldMap.SectorKey>();
        for(int x=0x8004;x<=0x8006;x++)for(int y=0x7ffe;y<=0x8000;y++)keys.add(new ModernWorldMap.SectorKey(x,y));
        var map=ModernWorldMap.load(Path.of(args[0]),4,keys,()->false);
        var policy=ModernSharedSiegePreset.policy();var qualified=policy.qualify(new ModernWorldBinding(3,547,map),()->false);
        System.out.println("PINNED KENT POLICY="+ModernSiegeSettlement.configHash(policy)+" outside="+qualified.outside().size()+" inside="+qualified.inside().size()+" opened="+qualified.opened().size());
        var gate=new HashSet<ModernWorldMap.Cell>();for(int y=32767;y<=32773;y++){var p=new ModernWorldMap.Cell(33111,y);if(map.standing(p))gate.add(p);}
        for(int x=33140;x<=33160;x++){var p=new ModernWorldMap.Cell(x,32805);if(map.standing(p))gate.add(p);}
        var out=region(map,new ModernWorldMap.Cell(33105,32770),gate);var in=region(map,new ModernWorldMap.Cell(33120,32770),gate);
        var opened=region(map,new ModernWorldMap.Cell(33105,32770),Set.of());
        System.out.println("shared-map4 Kent candidate: manifest="+map.digest()+" closedOutside="+out.size()+" closedInside="+in.size()+" overlaps="+out.stream().anyMatch(in::contains)+" opened="+opened.size());
        System.out.println("standing gates="+gate.stream().sorted(Comparator.comparingInt(ModernWorldMap.Cell::x).thenComparingInt(ModernWorldMap.Cell::y)).toList());
        for(var p:List.of(new ModernWorldMap.Cell(33104,32755),new ModernWorldMap.Cell(33104,32785),new ModernWorldMap.Cell(33142,32770)))System.out.println("tower candidate="+p+" standing="+map.standing(p)+" inside="+in.contains(p)+" outside="+out.contains(p));
        for(int team=0;team<2;team++){
            var area=team==0?out:in;var first=new ModernWorldMap.Cell(team==0?33105:33120,32770);
            int cx=team==0?33088:33136,cy=team==0?32770:32788;
            var candidates=area.stream().filter(p->(p.x()&1)==0&&(p.y()&1)==0&&Math.abs(p.x()-first.x())+Math.abs(p.y()-first.y())>=12)
                    .filter(p->Math.abs(p.x()-33142)+Math.abs(p.y()-32770)>=5)
                    .sorted(Comparator.comparingInt((ModernWorldMap.Cell p)->Math.abs(p.x()-cx)+Math.abs(p.y()-cy)).thenComparingInt(ModernWorldMap.Cell::x).thenComparingInt(ModernWorldMap.Cell::y)).limit(49).toList();
            var camp=new ArrayList<ModernWorldMap.Cell>();camp.add(first);camp.addAll(candidates);
            System.out.println("camp"+team+"="+camp.stream().map(p->p.x()+","+p.y()).collect(java.util.stream.Collectors.joining(";")));
        }
        var parent=new HashMap<ModernWorldMap.Cell,ModernWorldMap.Cell>();var queue=new ArrayDeque<ModernWorldMap.Cell>();
        var start=new ModernWorldMap.Cell(33105,32770);var end=new ModernWorldMap.Cell(33120,32770);parent.put(start,start);queue.add(start);
        while(!queue.isEmpty()&&!parent.containsKey(end)) {var a=queue.remove();for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){var b=new ModernWorldMap.Cell(a.x()+d[0],a.y()+d[1]);if(!gate.contains(b)&&!parent.containsKey(b)&&map.canStep(a,b)){parent.put(b,a);queue.add(b);}}}
        if(parent.containsKey(end)){var route=new ArrayList<ModernWorldMap.Cell>();var p=end;while(!p.equals(start)){route.add(p);p=parent.get(p);}Collections.reverse(route);System.out.println("closed bypass path="+route);}
        for(var s:map.manifest().sectors())System.out.println(s.key().fileName()+" bytes="+s.byteLength()+" sha256="+s.rawSha256());
        for(int y=32745;y<=32805;y++){var row=new StringBuilder();for(int x=33095;x<=33185;x++){
            var p=new ModernWorldMap.Cell(x,y);row.append(gate.contains(p)?'G':in.contains(p)?'I':out.contains(p)?'o':map.standing(p)?'.':'#');}
            System.out.println(y+" "+row);}
    }
    private ModernSharedSiegeReadProbe(){}
}
