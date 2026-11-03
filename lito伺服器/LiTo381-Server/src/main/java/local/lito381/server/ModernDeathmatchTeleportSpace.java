package local.lito381.server;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** A deathmatch may blink across obstacles within its arena, never into a
 * disconnected arena. Static geometry only: live occupancy remains the
 * engine's authority. Qualified outside the preparation Guard, with no I/O. */
final class ModernDeathmatchTeleportSpace {
    private static final int[] DX={0,1,1,1,0,-1,-1,-1},DY={-1,-1,0,1,1,1,0,-1};
    private ModernDeathmatchTeleportSpace(){}
    static List<ModernWorldMap.Cell> candidates(ModernWorldMap map,ModernWorldMap.Cell spawn,BooleanSupplier cancelled)
            throws ModernWorldMap.MapException {
        Objects.requireNonNull(map);Objects.requireNonNull(cancelled);active(cancelled);
        if(!map.standing(spawn))throw new ModernWorldMap.MapException(ModernWorldMap.Failure.INVALID_SPAWNS);
        var bounds=map.sectorBounds();int maximum=bounds.size()*4096;
        var seen=new HashSet<ModernWorldMap.Cell>();var queue=new ArrayDeque<ModernWorldMap.Cell>();
        seen.add(spawn);queue.add(spawn);int work=0;
        while(!queue.isEmpty()) {
            if((work++&255)==0)active(cancelled);
            var from=queue.remove();
            for(int heading=0;heading<8;heading++) {
                var to=new ModernWorldMap.Cell(from.x()+DX[heading],from.y()+DY[heading]);
                if(map.canStep(from,to)&&map.canStep(to,from)&&seen.add(to)) {
                    if(seen.size()>maximum)throw new ModernWorldMap.MapException(ModernWorldMap.Failure.INVALID_SPAWNS);
                    queue.add(to);
                }
            }
        }
        var result=new ArrayList<ModernWorldMap.Cell>(seen.size());
        // Preserve the pre-existing manifest/y/x RNG order. In a connected
        // map this is byte-for-byte the old candidate list, not BFS order.
        for(var sector:bounds)for(int y=0;y<sector.height();y++)for(int x=0;x<sector.width();x++) {
            if((work++&255)==0)active(cancelled);
            var cell=new ModernWorldMap.Cell(sector.origin().x()+x,sector.origin().y()+y);
            if(seen.contains(cell))result.add(cell);
        }
        active(cancelled);return List.copyOf(result);
    }
    private static void active(BooleanSupplier cancelled)throws ModernWorldMap.MapException {
        if(Thread.currentThread().isInterrupted()||cancelled.getAsBoolean())
            throw new ModernWorldMap.MapException(ModernWorldMap.Failure.CANCELLED);
    }
}
