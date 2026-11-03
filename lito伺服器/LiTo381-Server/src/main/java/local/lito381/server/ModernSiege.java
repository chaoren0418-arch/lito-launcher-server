package local.lito381.server;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/** Dedicated modern siege authority. Coordinates, HP, gate footprint, portal
 * endpoints and respawn delay are explicit NEW server policy, not recovered LT
 * values. Only the two-outer/gate/inner order, random roles and timeout defender
 * victory are taken from the retained 2005 contract. No resource or JDBC I/O. */
final class ModernSiege {
    enum Slot { OUTER_LEFT, OUTER_RIGHT, INNER }
    enum Phase { WAITING, OUTER, INNER, FINISHED, ABORTED }
    enum PortalReason { MOVED, NOT_ACTIVE, DEAD, ATTACKER, NOT_AT_PORTAL, TOO_FAST, OCCUPIED }
    record Scope(long roomId,long matchId,long generation) {
        Scope { if(roomId<1||matchId<1||generation<1)throw bad(); }
    }
    record Target(Scope scope,Slot slot) {
        Target { Objects.requireNonNull(scope);Objects.requireNonNull(slot); }
    }
    record Tower(Slot slot,ModernWorldMap.Cell cell,int gfx,long maximumHp) {
        Tower { Objects.requireNonNull(slot);Objects.requireNonNull(cell);
            if(maximumHp<1||!Set.of(2665,2667,2669,2671).contains(gfx))throw bad(); }
    }
    record Portal(ModernWorldMap.Cell outside,ModernWorldMap.Cell inside) {
        Portal { Objects.requireNonNull(outside);Objects.requireNonNull(inside);if(outside.equals(inside))throw bad(); }
        ModernWorldMap.Cell destination(ModernWorldMap.Cell from) {
            return outside.equals(from)?inside:inside.equals(from)?outside:null;
        }
    }
    record Config(ModernWorldBinding.Key key,List<Tower> towers,Set<ModernWorldMap.Cell> gate,
                  List<ModernWorldMap.Cell> attackCamp,List<ModernWorldMap.Cell> defenseCamp,
                  Portal portal,long durationMillis,long respawnMillis) {
        Config {
            Objects.requireNonNull(key);Objects.requireNonNull(portal);
            towers=List.copyOf(towers);gate=Set.copyOf(gate);attackCamp=List.copyOf(attackCamp);defenseCamp=List.copyOf(defenseCamp);
            if(key.mode()!=3||!Set.of(547,563,573).contains(key.logicalMapId())||towers.size()!=3||gate.isEmpty()||gate.size()>64
                    ||attackCamp.isEmpty()||attackCamp.size()>50||defenseCamp.isEmpty()||defenseCamp.size()>50
                    ||durationMillis<50||durationMillis>1_800_000||respawnMillis<1||respawnMillis>60_000)throw bad();
            var reserved=new HashSet<>(gate);
            for(int i=0;i<3;i++)if(towers.get(i).slot().ordinal()!=i||!reserved.add(towers.get(i).cell()))throw bad();
            for(var cell:attackCamp)if(!reserved.add(cell))throw bad();
            for(var cell:defenseCamp)if(!reserved.add(cell))throw bad();
            if(!reserved.add(portal.outside())||!reserved.add(portal.inside()))throw bad();
        }
        List<ModernWorldMap.Cell> camp(int team,int attacker) { checkTeam(team);checkTeam(attacker);return team==attacker?attackCamp:defenseCamp; }
        Set<ModernWorldMap.Cell> towerCells(){return Set.copyOf(towers.stream().map(Tower::cell).toList());}
        Space qualify(ModernWorldBinding binding,BooleanSupplier cancelled) {
            Objects.requireNonNull(cancelled);if(!key.equals(binding.key()))throw bad();active(cancelled);
            var map=binding.map();var all=new HashSet<>(gate);all.addAll(towerCells());all.addAll(attackCamp);all.addAll(defenseCamp);
            all.add(portal.outside());all.add(portal.inside());
            for(var cell:all)if(!map.standing(cell))throw bad();
            var outer=reachable(map,attackCamp.get(0),false,towerCells(),gate,cancelled);
            var inner=reachable(map,defenseCamp.get(0),false,towerCells(),gate,cancelled);
            if(!outer.containsAll(attackCamp)||!inner.containsAll(defenseCamp)||outer.contains(defenseCamp.get(0))
                    ||!outer.contains(portal.outside())||!inner.contains(portal.inside()))throw bad();
            // An ornamental door is not a gate: CLOSED must be a real cutset,
            // including diagonal edges. No attack square for the inner tower
            // may be reachable from outside until both outer towers die.
            for(int i=0;i<3;i++) {
                var tower=towers.get(i).cell();
                if(i<2&&!canHitFrom(map,outer,tower,towerCells(),gate)||i==2&&(!canHitFrom(map,inner,tower,towerCells(),gate)
                        ||canHitFrom(map,outer,tower,towerCells(),gate)))throw bad();
            }
            var opened=reachable(map,attackCamp.get(0),true,towerCells(),gate,cancelled);
            if(!opened.containsAll(inner)||!opened.containsAll(outer)||!opened.containsAll(gate))throw bad();
            active(cancelled);return new Space(this,map,Set.copyOf(outer),Set.copyOf(inner),Set.copyOf(opened));
        }
    }
    /** Immutable, prequalified components. No flood fill while holding the
     * world/preparation lock; movement, LOS and random teleport use one policy. */
    static final class Space {
        private final Config config;
        private final ModernWorldMap map;
        private final Set<ModernWorldMap.Cell> outside,inside,opened;
        // Only qualify() can mint this immutable proof; a caller cannot supply
        // invented connected components to bypass the real shared-map scan.
        private Space(Config config,ModernWorldMap map,Set<ModernWorldMap.Cell> outside,Set<ModernWorldMap.Cell> inside,Set<ModernWorldMap.Cell> opened) {
            this.config=config;this.map=map;this.outside=Set.copyOf(outside);this.inside=Set.copyOf(inside);this.opened=Set.copyOf(opened);
        }
        Config config(){return config;}
        ModernWorldMap map(){return map;}
        Set<ModernWorldMap.Cell> outside(){return outside;}
        Set<ModernWorldMap.Cell> inside(){return inside;}
        Set<ModernWorldMap.Cell> opened(){return opened;}
        Space require(ModernWorldBinding binding,Config expected) {
            if(map!=binding.map()||!config.equals(expected)||!config.key().equals(binding.key()))throw bad();return this;
        }
        boolean step(ModernWorldMap.Cell from,ModernWorldMap.Cell to,boolean open) {
            return edge(map,from,to,open,config.towerCells(),config.gate(),null);
        }
        boolean rayStep(ModernWorldMap.Cell from,ModernWorldMap.Cell to,boolean open,ModernWorldMap.Cell target) {
            return edge(map,from,to,open,config.towerCells(),config.gate(),target);
        }
        boolean teleport(ModernWorldMap.Cell from,ModernWorldMap.Cell to,boolean open) {
            return open?opened.contains(from)&&opened.contains(to):outside.contains(from)&&outside.contains(to)||inside.contains(from)&&inside.contains(to);
        }
    }
    private static Set<ModernWorldMap.Cell> reachable(ModernWorldMap map,ModernWorldMap.Cell start,boolean open,
            Set<ModernWorldMap.Cell> towers,Set<ModernWorldMap.Cell> gate,BooleanSupplier cancelled) {
        var seen=new HashSet<ModernWorldMap.Cell>();var queue=new ArrayDeque<ModernWorldMap.Cell>();seen.add(start);queue.add(start);
        while(!queue.isEmpty()) {
            active(cancelled);var from=queue.remove();
            for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++) {
                var next=new ModernWorldMap.Cell(from.x()+dx,from.y()+dy);
                if(edge(map,from,next,open,towers,gate,null)&&seen.add(next))queue.add(next);
            }
            if(seen.size()>65_536)throw bad();
        }
        return seen;
    }
    private static boolean canHitFrom(ModernWorldMap map,Set<ModernWorldMap.Cell> region,ModernWorldMap.Cell target,
            Set<ModernWorldMap.Cell> towers,Set<ModernWorldMap.Cell> gate) {
        for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++) {
            var from=new ModernWorldMap.Cell(target.x()+dx,target.y()+dy);
            if(region.contains(from)&&edge(map,from,target,false,towers,gate,target))return true;
        }
        return false;
    }
    private static boolean free(ModernWorldMap.Cell cell,boolean open,Set<ModernWorldMap.Cell> towers,Set<ModernWorldMap.Cell> gate,ModernWorldMap.Cell target) {
        return (open||!gate.contains(cell))&&(!towers.contains(cell)||cell.equals(target));
    }
    private static boolean edge(ModernWorldMap map,ModernWorldMap.Cell from,ModernWorldMap.Cell to,boolean open,
            Set<ModernWorldMap.Cell> towers,Set<ModernWorldMap.Cell> gate,ModernWorldMap.Cell target) {
        if(!map.canStep(from,to)||!free(from,open,towers,gate,null)||!free(to,open,towers,gate,target))return false;
        if(from.x()==to.x()||from.y()==to.y())return true;
        var a=new ModernWorldMap.Cell(to.x(),from.y());var b=new ModernWorldMap.Cell(from.x(),to.y());
        // Match the map's OR-of-two-cardinal-routes rule, but each route must
        // satisfy BOTH static edges AND dynamic blockers (no corner bypass).
        return free(a,open,towers,gate,null)&&map.canStep(from,a)&&map.canStep(a,to)
                ||free(b,open,towers,gate,null)&&map.canStep(from,b)&&map.canStep(b,to);
    }
    static int assignAttacker(IntSupplier random){return NativeLitoSiegePolicy.resolveAttackerForce(-1,random);}
    record Objective(Tower tower,long hp,long damageLoss,long hitCount,long revision,long deathRevision,long lastActor) {
        Objective { Objects.requireNonNull(tower);
            if(hp<0||hp>tower.maximumHp()||damageLoss!=tower.maximumHp()-hp||hitCount<0||revision<0||deathRevision<0||lastActor<0
                    ||lastActor>100||(hitCount==0)!=(revision==0)||(hitCount==0)!=(lastActor==0)
                    ||(hp==0)!=(deathRevision>0)||deathRevision>revision||hitCount==0&&damageLoss!=0)throw bad(); }
        static Objective initial(Tower t){return new Objective(t,t.maximumHp(),0,0,0,0,0);}
        Objective damage(long applied,long actor,long next){
            if(hp==0||applied<0||applied>hp||actor<1||actor>100||next<=revision)throw bad();
            return new Objective(tower,hp-applied,Math.addExact(damageLoss,applied),Math.incrementExact(hitCount),next,applied==hp?next:0,actor);
        }
    }
    record State(Scope scope,Config config,int attacker,Phase phase,List<Objective> objectives,
                 long revision,long gateRevision,long finishRevision,long finishedAtMillis,int winner) {
        State {
            Objects.requireNonNull(scope);Objects.requireNonNull(config);Objects.requireNonNull(phase);checkTeam(attacker);objectives=List.copyOf(objectives);
            if(objectives.size()!=3||revision<1||gateRevision<0||gateRevision>revision||finishRevision<0||finishRevision>revision
                    ||finishedAtMillis<0||finishedAtMillis>config.durationMillis()||winner< -1||winner>1)throw bad();
            for(int i=0;i<3;i++)if(!objectives.get(i).tower().equals(config.towers().get(i))||objectives.get(i).revision()>revision)throw bad();
            boolean outerDead=objectives.get(0).hp()==0&&objectives.get(1).hp()==0,innerDead=objectives.get(2).hp()==0;
            if((gateRevision>0)!=outerDead||innerDead&&!outerDead||phase==Phase.WAITING&&objectives.stream().anyMatch(t->t.hitCount()!=0)
                    ||phase==Phase.OUTER&&outerDead||phase==Phase.INNER&&(!outerDead||innerDead)
                    ||phase!=Phase.FINISHED&&(winner!=-1||finishRevision!=0||finishedAtMillis!=0||innerDead))throw bad();
            if(outerDead&&gateRevision!=Math.max(objectives.get(0).deathRevision(),objectives.get(1).deathRevision())
                    ||innerDead&&finishRevision!=objectives.get(2).deathRevision())throw bad();
            if(phase==Phase.FINISHED&&(finishRevision!=revision||winner<0||(winner==attacker)!=innerDead
                    ||winner!=attacker&&finishedAtMillis!=config.durationMillis()
                    ||innerDead&&finishedAtMillis>=config.durationMillis()))throw bad();
        }
        boolean open(){return gateRevision>0;}
        boolean finished(){return phase==Phase.FINISHED;}
        boolean attackable(int team,Slot slot){return team==attacker&&(phase==Phase.OUTER&&slot!=Slot.INNER||phase==Phase.INNER&&slot==Slot.INNER)&&objectives.get(slot.ordinal()).hp()>0;}
        State activate(long next){if(phase!=Phase.WAITING||next<=revision)throw bad();return new State(scope,config,attacker,Phase.OUTER,objectives,next,0,0,0,-1);}
        State damage(Target target,int team,long actor,long applied,long elapsed,long next) {
            if(!target.scope().equals(scope)||!attackable(team,target.slot())||elapsed<0||elapsed>=config.durationMillis()||next<=revision)throw bad();
            var rows=new java.util.ArrayList<>(objectives);int i=target.slot().ordinal();rows.set(i,rows.get(i).damage(applied,actor,next));
            boolean outerDead=rows.get(0).hp()==0&&rows.get(1).hp()==0,innerDead=rows.get(2).hp()==0;
            long gate=gateRevision>0?gateRevision:outerDead?next:0;
            return new State(scope,config,attacker,innerDead?Phase.FINISHED:outerDead?Phase.INNER:Phase.OUTER,rows,next,gate,innerDead?next:0,innerDead?elapsed:0,innerDead?attacker:-1);
        }
        State timeout(long elapsed,long next){
            if(phase!=Phase.OUTER&&phase!=Phase.INNER||elapsed<config.durationMillis())return this;
            if(next<=revision)throw bad();return new State(scope,config,attacker,Phase.FINISHED,objectives,next,gateRevision,next,config.durationMillis(),1-attacker);
        }
        State abort(long next){if(finished()||phase==Phase.ABORTED)return this;if(next<=revision)throw bad();return new State(scope,config,attacker,Phase.ABORTED,objectives,next,gateRevision,0,0,-1);}
    }
    static State initial(Scope scope,Config config,int attacker){return new State(scope,config,attacker,Phase.WAITING,config.towers().stream().map(Objective::initial).toList(),1,0,0,0,-1);}
    record Life(long entityId,long deaths,long deathRevision,long respawns,long respawnRevision,long hpGain,long dueMillis) {
        Life { if(entityId<1||entityId>100||deaths<0||respawns<0||respawns>deaths||deaths-respawns>1||hpGain<0||dueMillis<0||dueMillis>1_860_000
                ||deathRevision<0||respawnRevision<0||(deaths==0)!=(deathRevision==0)||(respawns==0)!=(respawnRevision==0)
                ||(respawns==0)!=(hpGain==0)||deaths==respawns&&(dueMillis!=0||deaths>0&&respawnRevision<=deathRevision)
                ||deaths>respawns&&deathRevision<=respawnRevision)throw bad(); }
        static Life initial(long entity){return new Life(entity,0,0,0,0,0,0);}
        Life died(long next,long elapsed,long delay){if(deaths!=respawns||next<=respawnRevision||elapsed<0||delay<1)throw bad();return new Life(entityId,Math.incrementExact(deaths),next,respawns,respawnRevision,hpGain,Math.addExact(elapsed,delay));}
        Life respawn(long next,long hp){if(deaths!=respawns+1||dueMillis==0||next<=deathRevision||hp<1)throw bad();return new Life(entityId,deaths,deathRevision,Math.incrementExact(respawns),next,Math.addExact(hpGain,hp),0);}
        Life terminal(){return new Life(entityId,deaths,deathRevision,respawns,respawnRevision,hpGain,0);}
    }
    record Snapshot(State state,List<Life> lives) {
        Snapshot { lives=List.copyOf(lives);if(state==null?!lives.isEmpty():lives.size()<2||lives.size()>100)throw bad();
            for(int i=0;i<lives.size();i++)if(lives.get(i).entityId()!=i+1)throw bad(); }
        static Snapshot disabled(){return new Snapshot(null,List.of());}
        boolean enabled(){return state!=null;}
        boolean finished(){return enabled()&&state.finished();}
        void validate(ModernBattleEngine.Snapshot world){
            if(!enabled())return;
            if(!state.scope().equals(new Scope(world.room().summary().id(),world.matchId(),world.generation()))
                    ||!state.config().key().equals(new ModernWorldBinding.Key(world.room().summary().config().mode(),world.room().summary().config().mapId()))
                    ||state.revision()>world.revision()||world.vitals().size()!=lives.size()||world.actors().size()!=lives.size()
                    ||world.result()!=null||world.honor().enabled()||world.quest().enabled()||world.race().enabled()||world.timeAttack().enabled()
                    ||!world.damages().isEmpty()||!world.fireStorms().isEmpty()||!world.ammo().isEmpty()||!world.buffs().isEmpty()
                    ||(world.phase()==ModernBattleEngine.Phase.FINISHED)!=finished()
                    ||(world.phase()==ModernBattleEngine.Phase.ABORTED)!=(state.phase()==Phase.ABORTED)
                    ||(world.phase()==ModernBattleEngine.Phase.LOADING||world.phase()==ModernBattleEngine.Phase.COUNTDOWN)!=(state.phase()==Phase.WAITING))throw bad();
            for(int i=0;i<lives.size();i++) {
                var life=lives.get(i);var v=world.vitals().get(i);
                if(life.deathRevision()!=v.deathRevision()||life.respawnRevision()>world.revision()||(v.hp()==0)!=(life.deaths()>life.respawns())
                        ||life.dueMillis()>0&&world.phase()!=ModernBattleEngine.Phase.ACTIVE
                        ||world.phase()==ModernBattleEngine.Phase.ACTIVE&&v.hp()==0&&life.dueMillis()==0)throw bad();
            }
        }
    }
    private static void checkTeam(int team){if(team<0||team>1)throw bad();}
    private static void active(BooleanSupplier cancelled){if(Thread.currentThread().isInterrupted()||cancelled.getAsBoolean())throw new IllegalStateException("siege qualification cancelled");}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid modern siege authority");}
    private ModernSiege(){}
}
