package local.lito381.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** Server-owned force instances share immutable geometry, never occupancy.
 * The room and participant IDs remain global; coordinates are force-qualified. */
final class ModernQuestSpace {
    record Cell(int lane,ModernWorldMap.Cell position) {
        Cell {Objects.requireNonNull(position);if(lane< -1||lane>1)throw new IllegalArgumentException("invalid force cell");}
    }
    static Cell cell(boolean isolated,int lane,ModernWorldMap.Cell position){return new Cell(isolated?lane:-1,position);}
    static boolean isolated(ModernWorldService.Mapping mapping){return mapping.npc()!=null&&mapping.npc().questPlan()!=null;}
    static List<ModernWorldMap.Cell> allocate(ModernWorldService.Mapping mapping,List<ModernRooms.Member> roster,
            Set<ModernWorldMap.Cell> obstacles,BooleanSupplier cancelled)throws ModernWorldMap.MapException {
        if(!isolated(mapping))return ModernWorldSpawns.allocate(mapping.binding().map(),roster,mapping.spawnCamps(),obstacles,cancelled).positions();
        requireHumanRoom(roster);
        var result=new ArrayList<ModernWorldMap.Cell>(java.util.Collections.nCopies(roster.size(),null));
        for(int lane=0;lane<2;lane++) {
            int force=lane;var members=roster.stream().filter(m->m.team()==force).toList();
            // Entry/respawn positions must never occupy a later configured
            // birth or objective. Only the owning force reserves these cells.
            var blocked=new HashSet<ModernWorldMap.Cell>();
            var quest=mapping.npc().questPlan();
            for(var wave:quest.waves())if(wave.lane()==lane)for(var npc:wave.spawns())blocked.add(npc.cell());
            if(quest.objectives()!=null)for(var target:quest.objectives().targets())if(target.lane()==lane)blocked.add(target.cell());
            var plan=ModernWorldSpawns.allocateQuestForce(mapping.binding().map(),members,List.of(mapping.spawnCamps().get(lane)),blocked,cancelled);
            int next=0;for(int i=0;i<roster.size();i++)if(roster.get(i).team()==lane)result.set(i,plan.positions().get(next++));
        }
        return List.copyOf(result);
    }
    static void qualify(ModernWorldMap map,List<ModernRooms.Member> roster,List<ModernWorldMap.Cell> positions,
            Set<ModernWorldMap.Cell> obstacles,BooleanSupplier cancelled,boolean isolated)throws ModernWorldMap.MapException {
        if(!isolated){ModernWorldSpawns.qualify(map,roster,positions,obstacles,cancelled);return;}
        requireHumanRoom(roster);
        if(positions.size()!=roster.size())throw new ModernWorldMap.MapException(ModernWorldMap.Failure.INVALID_SPAWNS);
        for(int lane=0;lane<2;lane++) {
            var members=new ArrayList<ModernRooms.Member>();var cells=new ArrayList<ModernWorldMap.Cell>();
            for(int i=0;i<roster.size();i++)if(roster.get(i).team()==lane){members.add(roster.get(i));cells.add(positions.get(i));}
            ModernWorldSpawns.qualifyQuestForce(map,members,cells,obstacles,cancelled);
        }
    }
    private static void requireHumanRoom(List<ModernRooms.Member> roster)throws ModernWorldMap.MapException {
        if(roster==null||roster.isEmpty()||roster.size()>100
                ||roster.stream().noneMatch(m->m.identity().isHuman())
                ||roster.stream().map(ModernRooms.Member::identity).distinct().count()!=roster.size())
            throw new ModernWorldMap.MapException(ModernWorldMap.Failure.INVALID_SPAWNS);
    }
    private ModernQuestSpace(){}
}
