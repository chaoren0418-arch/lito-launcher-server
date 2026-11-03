package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** Independent occupied-grid BFS checks the allocator, never teleport/attack/AI. */
class ModernWorldSpawnsTest {
    static final ModernWorldMap.SectorKey SECTOR = ModernWorldMapTest.KEY;
    static ModernWorldMap.Cell cell(int x, int y) {
        return new ModernWorldMap.Cell(Math.addExact(SECTOR.origin().x(),x),Math.addExact(SECTOR.origin().y(),y));
    }
    static ModernWorldMap open(int map) throws Exception {
        return ModernWorldMap.fromSectorBytes(map,Map.of(SECTOR,ModernWorldMapTest.openSectorBytes()),()->false);
    }
    static List<ModernRooms.Member> roster(int humans, int bots) {
        var result = new ArrayList<ModernRooms.Member>();
        for (int i=0;i<humans+bots;i++) result.add(new ModernRooms.Member(i<humans
                ?ModernParticipantId.human(7,i+1):ModernParticipantId.bot(7,i-humans+1),
                "Actor"+i,"Fixture",80,1,0,i%2,true));
        return List.copyOf(result);
    }

    @Test void safeLegacyLayoutAndIdentityOrderArePreservedExactly() throws Exception {
        var map=open(514);var roster=roster(2,0);var old=map.serverChosenSpawns(cell(10,10),2);
        var plan=ModernWorldSpawns.allocate(map,roster,List.of(cell(10,10)),Set.of(),()->false);
        assertFalse(plan.repaired());assertEquals(old,plan.positions());
        assertPlan(map,roster,plan,Set.of());
        assertThrows(UnsupportedOperationException.class,()->plan.positions().clear());
    }
    @Test void cooperativeFutureReservationBudgetIsExplicitFiniteAndStillChecksEscapeAndCancellation()throws Exception {
        var sectors=new HashMap<ModernWorldMap.SectorKey,byte[]>();
        for(int y=0;y<4;y++)sectors.put(new ModernWorldMap.SectorKey(SECTOR.x(),SECTOR.y()+y),ModernWorldMapTest.openSectorBytes());
        var map=ModernWorldMap.fromSectorBytes(554,sectors,()->false);var obstacles=new HashSet<ModernWorldMap.Cell>();
        for(int i=0;i<ModernWorldSpawns.MAX_RESERVED_CELLS;i++)obstacles.add(cell(32+i%24,32+i/24));
        var roster=roster(4,0);var plan=ModernWorldSpawns.allocate(map,roster,List.of(cell(10,10)),obstacles,()->false);
        assertPlan(map,roster,plan,obstacles);
        assertThrows(ModernWorldMap.MapException.class,()->ModernWorldSpawns.allocate(map,roster,List.of(cell(10,10)),obstacles,()->true));
        obstacles.add(cell(60,60));assertEquals(ModernWorldSpawns.MAX_RESERVED_CELLS+1,obstacles.size());
        assertThrows(ModernWorldMap.MapException.class,()->ModernWorldSpawns.allocate(map,roster,List.of(cell(10,10)),obstacles,()->false));
    }

    @Test void cornerOneHumanWithThreeEightTwentyAndNinetyNinePassiveBotsActuallyEscapes() throws Exception {
        var map=open(514);
        for(int count:List.of(3,8,20,99)) {
            var roster=roster(1,count);var old=map.serverChosenSpawns(cell(0,0),count+1);
            assertThrows(ModernWorldMap.MapException.class,()->ModernWorldSpawns.qualify(map,roster,old,Set.of(),()->false));
            var plan=ModernWorldSpawns.allocate(map,roster,List.of(cell(0,0)),Set.of(),()->false);
            assertTrue(plan.repaired(),"old corner is genuinely enclosed by passive actors: "+count);
            assertEquals(count+1,plan.positions().size());assertPlan(map,roster,plan,Set.of());
            assertEquals(plan,ModernWorldSpawns.allocate(map,roster,List.of(cell(0,0)),Set.of(),()->false));
            assertTrue(route(map,plan.positions(),Set.of(),0,plan.escapeCell()).size()>1);
        }
    }

    @Test void allOneHundredHumansHaveIndependentRoutesWhileOtherNinetyNineNeverMove() throws Exception {
        var map=open(514);var roster=roster(100,0);
        var plan=ModernWorldSpawns.allocate(map,roster,List.of(cell(0,0)),Set.of(),()->false);
        assertEquals(100,plan.positions().size());assertPlan(map,roster,plan,Set.of());
    }

    @Test void honorTwoCampsKeepObjectivesFixedAndRepairUsesFrozenOwnCampDistancesIncludingTies() throws Exception {
        var map=open(532);var camps=List.of(cell(0,0),cell(20,0));var obstacles=Set.of(cell(0,1),cell(20,1));
        var roster=roster(2,20);
        var honor=new ModernHonor.Config(List.of(new ModernHonor.Tower(0,cell(0,1),2665,3000),
                new ModernHonor.Tower(1,cell(20,1),2669,3000)),camps);
        var old=honor.playerSpawns(map,roster,cell(10,10));
        assertThrows(ModernWorldMap.MapException.class,()->ModernWorldSpawns.qualify(map,roster,old,obstacles,()->false));
        var plan=ModernWorldSpawns.allocate(map,roster,camps,obstacles,()->false);
        assertTrue(plan.repaired());assertPlan(map,roster,plan,obstacles);
        var distances=List.of(distances(map,camps.get(0),obstacles),distances(map,camps.get(1),obstacles));
        for(int i=0;i<roster.size();i++) {
            int team=roster.get(i).team();var p=plan.positions().get(i);
            assertTrue(distances.get(team).get(p)<=distances.get(1-team).get(p),"no cross-camp repair placement");
        }
        assertEquals(plan,ModernWorldSpawns.allocate(map,roster,camps,obstacles,()->false));
    }

    @Test void diagonalDestinationDoesNotEscapeThroughTwoOccupiedIntermediateCells() throws Exception {
        var map=open(514);var a=cell(0,0);var diagonal=cell(1,1);var blocked=Set.of(cell(1,0),cell(0,1));
        assertTrue(map.canStep(a,diagonal),"static geometry alone would permit the diagonal");
        assertFalse(ModernWorldSpawns.stepOpen(map,a,diagonal,blocked));
        assertTrue(ModernWorldSpawns.stepOpen(map,a,diagonal,Set.of(cell(1,0))));
        var seats=List.of(a,cell(1,0),cell(0,1));
        assertThrows(ModernWorldMap.MapException.class,()->ModernWorldSpawns.qualify(map,roster(1,2),seats,Set.of(),()->false));
    }

    @Test void aSingleFreeNeighbourInsideTheBirthHaloIsNotAnExitAndTinyPocketFailsClosed() throws Exception {
        byte[] bytes=blocked();
        // Connected four-cell corridor: the free third cell is inside the halo
        // of either occupied end; there is no outside-halo destination.
        for(int x=0;x<3;x++)ModernWorldMapTest.setAttributes(bytes,x,0,(short)1,(short)0);
        var map=ModernWorldMap.fromSectorBytes(514,Map.of(SECTOR,bytes),()->false);
        var seats=List.of(cell(0,0),cell(3,0));
        assertTrue(map.canStep(seats.get(0),cell(1,0)));
        assertThrows(ModernWorldMap.MapException.class,()->ModernWorldSpawns.qualify(map,roster(2,0),seats,Set.of(),()->false));
        assertThrows(ModernWorldMap.MapException.class,()->ModernWorldSpawns.allocate(map,roster(1,3),List.of(cell(0,0)),Set.of(),()->false));
    }

    @Test void towersCannotBeSpawnedThroughOrUsedAsTheOnlyConnectionBetweenCamps() throws Exception {
        var map=open(532);var members=roster(2,0);
        assertThrows(ModernWorldMap.MapException.class,()->ModernWorldSpawns.qualify(map,members,
                List.of(cell(0,0),cell(1,0)),Set.of(cell(1,0)),()->false));
        byte[] bytes=blocked();for(int x=0;x<12;x++)ModernWorldMapTest.setAttributes(bytes,x,0,(short)1,(short)0);
        var corridor=ModernWorldMap.fromSectorBytes(532,Map.of(SECTOR,bytes),()->false);
        assertThrows(ModernWorldMap.MapException.class,()->ModernWorldSpawns.allocate(corridor,members,
                List.of(cell(0,0),cell(12,0)),Set.of(cell(6,0)),()->false));
    }

    @Test void budgetCancellationAndInterruptionCoverEnumerationSearchAndFinalQualification() throws Exception {
        var map=open(514);var roster=roster(1,20);var old=map.serverChosenSpawns(cell(0,0),21);
        assertEquals(ModernWorldMap.Failure.CANCELLED,assertThrows(ModernWorldMap.MapException.class,
                ()->ModernWorldSpawns.allocate(map,roster,List.of(cell(0,0)),Set.of(),()->true)).failure());
        var calls=new AtomicInteger();
        assertEquals(ModernWorldMap.Failure.CANCELLED,assertThrows(ModernWorldMap.MapException.class,
                ()->ModernWorldSpawns.allocate(map,roster,List.of(cell(0,0)),Set.of(),()->calls.incrementAndGet()>50)).failure());
        assertEquals(ModernWorldMap.Failure.TOO_LARGE,assertThrows(ModernWorldMap.MapException.class,
                ()->ModernWorldSpawns.allocate(map,roster,old,List.of(cell(0,0)),Set.of(),()->false,200)).failure());
        Thread.currentThread().interrupt();
        try { assertEquals(ModernWorldMap.Failure.CANCELLED,assertThrows(ModernWorldMap.MapException.class,
                ()->ModernWorldSpawns.qualify(map,roster,old,Set.of(),()->false)).failure()); }
        finally { assertTrue(Thread.interrupted()); }
    }

    @Test void duplicateBlockedMissingAndOversizedRostersCannotBecomeAuthority() throws Exception {
        var map=open(514);
        assertThrows(ModernWorldMap.MapException.class,()->ModernWorldSpawns.allocate(map,roster(1,100),List.of(cell(0,0)),Set.of(),()->false));
        assertThrows(ModernWorldMap.MapException.class,()->ModernWorldSpawns.qualify(map,roster(2,0),List.of(cell(0,0),cell(0,0)),Set.of(),()->false));
        assertThrows(ModernWorldMap.MapException.class,()->ModernWorldSpawns.qualify(map,roster(2,0),List.of(cell(0,0),new ModernWorldMap.Cell(Integer.MAX_VALUE,Integer.MIN_VALUE)),Set.of(),()->false));
        assertThrows(ModernWorldMap.MapException.class,()->ModernWorldSpawns.allocate(map,roster(0,2),List.of(cell(0,0)),Set.of(),()->false));
    }

    @Test void explicitReal514AndComplete532PinsAlsoQualifyLargeFrozenRosters() throws Exception {
        String root=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(root!=null&&!root.isBlank(),"explicit shared resource root required");
        var dm=ModernWorldTestPeer.sharedMap(Path.of(root));
        ModernMapRotationTestPeer.qualify514(dm);
        var honor=ModernHonorTestPeer.qualified532(Path.of(root));
        var obstacles=Set.copyOf(ModernHonorTestPeer.config().towers().stream().map(ModernHonor.Tower::cell).toList());
        for(int bots:List.of(3,8,20,99)) {
            var members=roster(1,bots);
            assertPlan(dm,members,ModernWorldSpawns.allocate(dm,members,List.of(ModernWorldTestPeer.ANCHOR),Set.of(),()->false),Set.of());
            assertPlan(honor,members,ModernWorldSpawns.allocate(honor,members,ModernHonorTestPeer.SPAWNS,obstacles,()->false),obstacles);
        }
        var humans=roster(100,0);
        assertPlan(dm,humans,ModernWorldSpawns.allocate(dm,humans,List.of(ModernWorldTestPeer.ANCHOR),Set.of(),()->false),Set.of());
        assertPlan(honor,humans,ModernWorldSpawns.allocate(honor,humans,ModernHonorTestPeer.SPAWNS,obstacles,()->false),obstacles);
    }

    static void assertPlan(ModernWorldMap map,List<ModernRooms.Member> members,ModernWorldSpawns.Plan plan,
            Set<ModernWorldMap.Cell> objectives) throws Exception {
        assertEquals(members.size(),plan.positions().size());assertEquals(members.size(),Set.copyOf(plan.positions()).size());
        assertTrue(plan.positions().stream().noneMatch(objectives::contains));
        assertTrue(plan.positions().stream().allMatch(map::standing));
        assertTrue(outside(plan.escapeCell(),plan.positions()));
        assertEquals(plan.escapeCell(),ModernWorldSpawns.qualify(map,members,plan.positions(),objectives,()->false));
        for(int i=0;i<members.size();i++)if(members.get(i).identity().isHuman()) {
            var path=route(map,plan.positions(),objectives,i,plan.escapeCell());
            assertTrue(path.size()>1,"human "+i+" must walk, not disappear/teleport");
        }
    }
    static boolean outside(ModernWorldMap.Cell p,List<ModernWorldMap.Cell> seats) {
        return seats.stream().allMatch(s->Math.max(Math.abs((long)p.x()-s.x()),Math.abs((long)p.y()-s.y()))>1);
    }
    /** Test oracle is deliberately an eight-edge BFS with explicit occupied corner routes. */
    static List<ModernWorldMap.Cell> route(ModernWorldMap map,List<ModernWorldMap.Cell> seats,
            Set<ModernWorldMap.Cell> towers,int self,ModernWorldMap.Cell goal) {
        var occupied=new HashSet<>(seats);occupied.addAll(towers);var start=seats.get(self);occupied.remove(start);
        var parent=new HashMap<ModernWorldMap.Cell,ModernWorldMap.Cell>();var queue=new ArrayDeque<ModernWorldMap.Cell>();
        parent.put(start,start);queue.add(start);
        while(!queue.isEmpty()) {
            var p=queue.remove();if(p.equals(goal)) {
                var result=new ArrayList<ModernWorldMap.Cell>();
                for(var c=goal;!c.equals(start);c=parent.get(c))result.add(c);
                result.add(start);java.util.Collections.reverse(result);return List.copyOf(result);
            }
            for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++)if(x!=0||y!=0) {
                var q=new ModernWorldMap.Cell(Math.addExact(p.x(),x),Math.addExact(p.y(),y));
                if(parent.containsKey(q)||occupied.contains(q)||!map.canStep(p,q))continue;
                if(x!=0&&y!=0) {
                    var a=new ModernWorldMap.Cell(q.x(),p.y());var b=new ModernWorldMap.Cell(p.x(),q.y());
                    if((occupied.contains(a)||!map.canStep(p,a)||!map.canStep(a,q))
                            &&(occupied.contains(b)||!map.canStep(p,b)||!map.canStep(b,q)))continue;
                }
                parent.put(q,p);queue.add(q);
            }
        }
        return List.of();
    }
    private static Map<ModernWorldMap.Cell,Integer> distances(ModernWorldMap map,ModernWorldMap.Cell start,Set<ModernWorldMap.Cell> blocked) {
        var result=new HashMap<ModernWorldMap.Cell,Integer>();var queue=new ArrayDeque<ModernWorldMap.Cell>();result.put(start,0);queue.add(start);
        while(!queue.isEmpty()) {
            var p=queue.remove();for(var d:List.of(new int[]{0,-1},new int[]{1,0},new int[]{0,1},new int[]{-1,0})) {
                var q=new ModernWorldMap.Cell(p.x()+d[0],p.y()+d[1]);
                if(!blocked.contains(q)&&!result.containsKey(q)&&map.canStep(p,q)){result.put(q,result.get(p)+1);queue.add(q);}
            }
        }
        return result;
    }
    private static byte[] blocked() {
        byte[] bytes=ModernWorldMapTest.openSectorBytes();
        for(int y=0;y<64;y++)for(int x=0;x<64;x++)ModernWorldMapTest.setAttributes(bytes,x,y,(short)1,(short)1);
        return bytes;
    }
}
