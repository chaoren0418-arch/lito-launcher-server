package local.lito381.server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * NEW_MATCH_PET_RACE_V1, not an import of LT coordinates or an inferred native
 * random transformation. Four ordered laps and a fixed ten-second first-finisher
 * window are explicit modern policy. The latter is not a recovered BIN timer.
 * All values are immutable plans: Engine commits a MOVE and its race progress
 * together, after the existing owner/ticket/sequence/geometry/cadence checks.
 */
final class ModernPetRace {
    static final String RULESET="NEW_MATCH_PET_RACE_V1";
    static final int LAPS=4, MIN_RUNNERS=2, MAX_RUNNERS=10, MAX_GATES=64;
    static final long FINISH_WINDOW_NANOS=10_000_000_000L, MAX_ACTIVE_NANOS=1_800_000_000_000L;
    static final Set<Long> BASE_GRAPHICS=Set.of(5065L,938L,2145L,3182L);
    enum Phase { WAITING, RUNNING, FINISH_WINDOW, COMPLETE, ABORTED }
    enum Dnf { NONE, LEFT, DISCONNECTED, WINDOW_CLOSED }

    record Gate(int minX,int maxX,int minY,int maxY) {
        Gate { if(minX>maxX||minY>maxY)throw invalid(); }
        boolean contains(ModernWorldMap.Cell cell) {
            return cell!=null&&cell.x()>=minX&&cell.x()<=maxX&&cell.y()>=minY&&cell.y()<=maxY;
        }
    }

    /** Only qualify() creates a course: the full physical manifest is authority. */
    static final class Course {
        private final ModernWorldBinding.Key key;
        private final ModernWorldMap map;
        private final List<Gate> gates;
        private final long baseGraphicsId,stepIntervalNanos;
        private final String digest;
        private Course(ModernWorldBinding.Key key,ModernWorldMap map,List<Gate> gates,long graphics,long interval) {
            this.key=key;this.map=map;this.gates=List.copyOf(gates);this.baseGraphicsId=graphics;this.stepIntervalNanos=interval;
            this.digest=courseHash(key,map.manifest(),gates,graphics,interval);
        }
        ModernWorldBinding.Key key(){return key;}
        ModernWorldMap map(){return map;}
        List<Gate> gates(){return gates;}
        long baseGraphicsId(){return baseGraphicsId;}
        long stepIntervalNanos(){return stepIntervalNanos;}
        String digest(){return digest;}
    }

    /** Public receipt fields. Actor identity/position/attempt sequence stay in World. */
    record Runner(long entityId,long revision,long progressMoveSequence,long completedLaps,long nextCheckpoint,
                  long placement,long finishElapsedNanos,Dnf dnf,long baseGraphicsId) {
        Runner {
            if(entityId<1||entityId>MAX_RUNNERS||revision<0||progressMoveSequence<0||completedLaps<0||completedLaps>LAPS
                    ||nextCheckpoint<0||nextCheckpoint>=MAX_GATES||placement<0||placement>MAX_RUNNERS||finishElapsedNanos<0
                    ||dnf==null||!BASE_GRAPHICS.contains(baseGraphicsId))throw invalid();
            if((placement>0)!=(completedLaps==LAPS)||placement>0&&(nextCheckpoint!=0||finishElapsedNanos==0||dnf!=Dnf.NONE)
                    ||placement==0&&finishElapsedNanos!=0||revision==0&&(progressMoveSequence!=0||completedLaps!=0||nextCheckpoint!=0||dnf!=Dnf.NONE)
                    ||(progressMoveSequence==0)!=(completedLaps==0&&nextCheckpoint==0)
                    ||(revision==0)!=(progressMoveSequence==0&&dnf==Dnf.NONE))throw invalid();
        }
        boolean competing(){return placement==0&&dnf==Dnf.NONE;}
    }
    /** Internal immutable position/sequence mirror prevents a repeated crossing from becoming progress. */
    record Entrant(TournamentPresence.CharacterKey source,ModernWorldMap.Cell position,long moveSequence,Runner runner) {
        Entrant { Objects.requireNonNull(source);Objects.requireNonNull(position);Objects.requireNonNull(runner);
            if(moveSequence<runner.progressMoveSequence())throw invalid(); }
    }
    record State(long matchId,long generation,long revision,Course course,Phase phase,long activeLimitNanos,
                 long observedElapsedNanos,long firstFinisherEntity,long finishWindowDeadlineElapsedNanos,long terminalRevision,
                 List<Entrant> entrants) {
        State {
            Objects.requireNonNull(course);Objects.requireNonNull(phase);entrants=List.copyOf(entrants);
            if(matchId<1||generation<1||revision<0||activeLimitNanos<50_000_000L||activeLimitNanos>MAX_ACTIVE_NANOS
                    ||observedElapsedNanos<0||entrants.size()<MIN_RUNNERS||entrants.size()>MAX_RUNNERS
                    ||firstFinisherEntity<0||firstFinisherEntity>entrants.size()||finishWindowDeadlineElapsedNanos<0
                    ||terminalRevision<0||terminalRevision>revision)throw invalid();
            var identities=new HashSet<TournamentPresence.CharacterKey>();var occupied=new HashSet<ModernWorldMap.Cell>();
            var places=new HashSet<Long>();long first=0;
            for(int i=0;i<entrants.size();i++) {
                var e=entrants.get(i);var r=e.runner();
                if(r.entityId()!=i+1L||r.revision()>revision||r.nextCheckpoint()>=course.gates().size()
                        ||r.progressMoveSequence()<r.completedLaps()*course.gates().size()+r.nextCheckpoint()
                        ||r.finishElapsedNanos()>activeLimitNanos||r.finishElapsedNanos()>observedElapsedNanos
                        ||r.baseGraphicsId()!=course.baseGraphicsId()||!identities.add(e.source())||!course.map().standing(e.position())
                        ||r.competing()&&!occupied.add(e.position()))throw invalid();
                if(i>0&&compare(entrants.get(i-1).source(),e.source())>=0)throw invalid();
                if(r.placement()>0&&!places.add(r.placement()))throw invalid();
                if(r.placement()==1)first=r.entityId();
            }
            for(long i=1;i<=places.size();i++)if(!places.contains(i))throw invalid();
            long previousFinish=0;
            for(long p=1;p<=places.size();p++)for(var e:entrants)if(e.runner().placement()==p){
                if(e.runner().finishElapsedNanos()<previousFinish)throw invalid();previousFinish=e.runner().finishElapsedNanos();}
            if(first!=firstFinisherEntity||(first==0)!=(finishWindowDeadlineElapsedNanos==0))throw invalid();
            if(first>0&&finishWindowDeadlineElapsedNanos!=Math.addExact(entrants.get((int)first-1).runner().finishElapsedNanos(),FINISH_WINDOW_NANOS))throw invalid();
            if(phase==Phase.WAITING&&(revision!=0||observedElapsedNanos!=0||first!=0)
                    ||phase==Phase.RUNNING&&first!=0||phase==Phase.FINISH_WINDOW&&first==0
                    ||(phase==Phase.COMPLETE||phase==Phase.ABORTED)!=(terminalRevision>0)
                    ||phase==Phase.COMPLETE&&(first==0||finishWindowDeadlineElapsedNanos>activeLimitNanos
                        ||entrants.stream().anyMatch(e->e.runner().competing())))throw invalid();
        }
        List<Runner> runners(){return entrants.stream().map(Entrant::runner).toList();}
        boolean active(){return phase==Phase.RUNNING||phase==Phase.FINISH_WINDOW;}
        Entrant entrant(long entity){if(entity<1||entity>entrants.size())throw new SecurityException("race entity mismatch");return entrants.get((int)entity-1);}
    }
    private ModernPetRace(){}

    static Course qualify(ModernWorldBinding.Key key,ModernWorldMap map,ModernWorldMap.Manifest expected,
                          List<Gate> gates,long graphics,long interval,List<ModernWorldMap.Cell> spawns,BooleanSupplier cancelled)
            throws ModernWorldMap.MapException {
        Objects.requireNonNull(key);Objects.requireNonNull(map);Objects.requireNonNull(expected);Objects.requireNonNull(cancelled);
        if(key.mode()!=0||!map.manifest().equals(expected))throw new ModernWorldMap.MapException(ModernWorldMap.Failure.CHANGED_FILE);
        if(gates==null||gates.size()<2||gates.size()>MAX_GATES||new HashSet<>(gates).size()!=gates.size()
                ||!BASE_GRAPHICS.contains(graphics)||interval<1_000_000L||interval>10_000_000_000L
                ||spawns==null||spawns.size()<MIN_RUNNERS||spawns.size()>MAX_RUNNERS)throw invalid();
        var safe=List.copyOf(gates);var work=new Work(cancelled);work.check();map.validateSpawns(spawns);
        // Scan only explicit map sectors, never the possibly huge integer area of a gate.
        boolean[] found=new boolean[safe.size()];
        for(var bound:map.sectorBounds())for(int y=0;y<bound.height();y++)for(int x=0;x<bound.width();x++) {
            work.use();var cell=new ModernWorldMap.Cell(Math.addExact(bound.origin().x(),x),Math.addExact(bound.origin().y(),y));
            if(!map.standing(cell))continue;int matches=0;
            for(int i=0;i<safe.size();i++){work.use();if(safe.get(i).contains(cell)){found[i]=true;matches++;}}
            if(matches>1)throw invalid(); // One committed cell cannot be two ordered checkpoints.
        }
        for(boolean value:found)if(!value)throw invalid();
        for(var spawn:spawns)enter(map,spawn,safe.get(0),work);
        var current=spawns.get(0);
        for(var gate:safe)current=enter(map,current,gate,work);
        enter(map,current,safe.get(0),work); // The actual directed graph closes into the next lap.
        work.check();return new Course(key,map,safe,graphics,interval);
    }
    static State initial(long match,long generation,Course course,List<TournamentPresence.CharacterKey> identities,
                         List<ModernWorldMap.Cell> spawns,int activeTimeoutMillis) {
        if(identities==null||spawns==null||identities.size()!=spawns.size())throw invalid();
        var entrants=new ArrayList<Entrant>();
        for(int i=0;i<identities.size();i++)entrants.add(new Entrant(identities.get(i),spawns.get(i),0,
                new Runner(i+1L,0,0,0,0,0,0,Dnf.NONE,course.baseGraphicsId())));
        return new State(match,generation,0,course,Phase.WAITING,Math.multiplyExact((long)activeTimeoutMillis,1_000_000L),0,0,0,0,entrants);
    }
    static State activate(State old,long nextRevision) {
        if(old.phase()!=Phase.WAITING)throw new IllegalStateException("race activation already consumed");
        revision(old,nextRevision);return replace(old,nextRevision,Phase.RUNNING,0,0,0,0,old.entrants());
    }
    /** elapsed is signed-nano subtraction from the authoritative ACTIVE start, never wall-clock time. */
    static State advance(State old,long elapsed,long nextRevision) {
        if(elapsed<0||elapsed<old.observedElapsedNanos())throw new IllegalArgumentException("race clock regressed");
        if(!old.active())return old;
        if(old.firstFinisherEntity()>0&&old.finishWindowDeadlineElapsedNanos()<=old.activeLimitNanos()
                &&elapsed>=old.finishWindowDeadlineElapsedNanos()) {
            revision(old,nextRevision);var rows=new ArrayList<Entrant>();
            for(var e:old.entrants())rows.add(e.runner().competing()?dnf(e,Dnf.WINDOW_CLOSED,nextRevision):e);
            return replace(old,nextRevision,Phase.COMPLETE,elapsed,old.firstFinisherEntity(),old.finishWindowDeadlineElapsedNanos(),nextRevision,rows);
        }
        if(elapsed>=old.activeLimitNanos())return abort(old,elapsed,nextRevision);
        return replace(old,old.revision(),old.phase(),elapsed,old.firstFinisherEntity(),old.finishWindowDeadlineElapsedNanos(),0,old.entrants());
    }
    /** Called for each accepted scoped MOVE attempt, including a rejected MOVE. No rejected move advances a gate. */
    static State moved(State old,long match,long generation,long entity,long sequence,ModernWorldMap.Cell from,
                       ModernWorldMap.Cell to,boolean moved,long elapsed,long nextRevision) {
        scope(old,match,generation);var before=old.entrant(entity);
        if(sequence<1||before.moveSequence()==Long.MAX_VALUE||sequence!=before.moveSequence()+1)
            throw new SecurityException("race movement sequence mismatch");
        if(old.phase()==Phase.WAITING) {
            if(moved||!before.position().equals(from)||!from.equals(to))throw invalid();revision(old,nextRevision);
            return old;
        }
        var current=advance(old,elapsed,nextRevision);if(!current.active())return current;
        revision(current,nextRevision);var runner=before.runner();
        if(!before.position().equals(from)||to==null)throw new SecurityException("race movement origin mismatch");
        if(moved&&!old.course().map().canStep(from,to)||!moved&&!from.equals(to))throw invalid();
        if(!runner.competing()){if(moved)throw invalid();return current;}
        var next=runner;long first=current.firstFinisherEntity(),deadline=current.finishWindowDeadlineElapsedNanos();
        var phase=current.phase();long rev=current.revision();
        Gate gate=old.course().gates().get((int)runner.nextCheckpoint());
        if(moved&&!gate.contains(from)&&gate.contains(to)) {
            revision(current,nextRevision);long checkpoint=runner.nextCheckpoint()+1,laps=runner.completedLaps();
            if(checkpoint==old.course().gates().size()){checkpoint=0;laps++;}
            long place=0,finish=0;
            if(laps==LAPS){place=current.entrants().stream().filter(e->e.runner().placement()>0).count()+1;finish=elapsed;
                if(finish<=0)throw invalid();if(first==0){first=entity;deadline=Math.addExact(elapsed,FINISH_WINDOW_NANOS);phase=Phase.FINISH_WINDOW;}}
            next=new Runner(entity,nextRevision,sequence,laps,checkpoint,place,finish,Dnf.NONE,runner.baseGraphicsId());rev=nextRevision;
        }
        var entries=new ArrayList<>(current.entrants());entries.set((int)entity-1,new Entrant(before.source(),to,sequence,next));
        return replace(current,rev,phase,elapsed,first,deadline,0,entries);
    }
    static State withdraw(State old,long match,long generation,long entity,Dnf reason,long elapsed,long nextRevision) {
        scope(old,match,generation);if(reason!=Dnf.LEFT&&reason!=Dnf.DISCONNECTED)throw invalid();old.entrant(entity);
        var current=advance(old,elapsed,nextRevision);if(!current.active())return current;
        var e=current.entrant(entity);if(!e.runner().competing())return current;
        revision(current,nextRevision);var entries=new ArrayList<>(current.entrants());entries.set((int)entity-1,dnf(e,reason,nextRevision));
        var next=replace(current,nextRevision,current.phase(),elapsed,current.firstFinisherEntity(),current.finishWindowDeadlineElapsedNanos(),0,entries);
        // With no finisher and no remaining racer, there is no ranked event to award.
        return entries.stream().noneMatch(v->v.runner().competing())&&next.firstFinisherEntity()==0
                ?replace(next,nextRevision,Phase.ABORTED,elapsed,0,0,nextRevision,entries):next;
    }
    static State abort(State old,long elapsed,long nextRevision) {
        if(old.phase()==Phase.COMPLETE||old.phase()==Phase.ABORTED)return old;
        if(elapsed<old.observedElapsedNanos())throw invalid();revision(old,nextRevision);
        return replace(old,nextRevision,Phase.ABORTED,elapsed,old.firstFinisherEntity(),old.finishWindowDeadlineElapsedNanos(),nextRevision,old.entrants());
    }
    private static Entrant dnf(Entrant e,Dnf reason,long revision) {
        var r=e.runner();return new Entrant(e.source(),e.position(),e.moveSequence(),new Runner(r.entityId(),revision,r.progressMoveSequence(),r.completedLaps(),r.nextCheckpoint(),0,0,reason,r.baseGraphicsId()));
    }
    private static State replace(State s,long revision,Phase phase,long elapsed,long first,long deadline,long terminal,List<Entrant> entries) {
        return new State(s.matchId(),s.generation(),revision,s.course(),phase,s.activeLimitNanos(),elapsed,first,deadline,terminal,entries);
    }
    private static void scope(State state,long match,long generation){if(state.matchId()!=match||state.generation()!=generation)throw new SecurityException("race generation mismatch");}
    private static void revision(State old,long next){if(next<=old.revision())throw new IllegalArgumentException("race revision did not advance");}
    private static int compare(TournamentPresence.CharacterKey a,TournamentPresence.CharacterKey b){int c=Integer.compare(a.sourceId(),b.sourceId());return c==0?Long.compare(a.objectId(),b.objectId()):c;}
    private static ModernWorldMap.Cell enter(ModernWorldMap map,ModernWorldMap.Cell from,Gate gate,Work work)throws ModernWorldMap.MapException {
        var outside=gate.contains(from)?find(map,from,c->!gate.contains(c),work):from;
        return find(map,outside,gate::contains,work);
    }
    private static ModernWorldMap.Cell find(ModernWorldMap map,ModernWorldMap.Cell from,Predicate<ModernWorldMap.Cell> target,Work work)throws ModernWorldMap.MapException {
        var pending=new ArrayDeque<ModernWorldMap.Cell>();var seen=new HashSet<ModernWorldMap.Cell>();pending.add(from);seen.add(from);
        int[] dx={0,1,0,-1},dy={-1,0,1,0};
        while(!pending.isEmpty()){work.use();var current=pending.remove();if(target.test(current))return current;
            for(int i=0;i<4;i++){work.use();var to=new ModernWorldMap.Cell(Math.addExact(current.x(),dx[i]),Math.addExact(current.y(),dy[i]));
                if(map.canStep(current,to)&&seen.add(to))pending.add(to);}}
        throw new ModernWorldMap.MapException(ModernWorldMap.Failure.INVALID_SPAWNS);
    }
    private static final class Work {
        long left=32_000_000;final BooleanSupplier cancelled;
        Work(BooleanSupplier cancelled){this.cancelled=cancelled;}
        void use()throws ModernWorldMap.MapException{if(--left<0)throw new ModernWorldMap.MapException(ModernWorldMap.Failure.TOO_LARGE);if((left&1023)==0)check();}
        void check()throws ModernWorldMap.MapException{if(Thread.currentThread().isInterrupted()||cancelled.getAsBoolean())throw new ModernWorldMap.MapException(ModernWorldMap.Failure.CANCELLED);}
    }
    static String courseHash(ModernWorldBinding.Key key,ModernWorldMap.Manifest map,List<Gate> gates,long graphics,long interval) {
        try{var bytes=new ByteArrayOutputStream();try(var out=new DataOutputStream(bytes)){
            out.writeUTF(RULESET);out.writeInt(key.mode());out.writeInt(key.logicalMapId());out.writeInt(map.mapId());out.write(HexFormat.of().parseHex(map.digest()));
            out.writeInt(map.sectors().size());for(var sector:map.sectors()){out.writeInt(sector.key().x());out.writeInt(sector.key().y());out.writeInt(sector.byteLength());out.write(HexFormat.of().parseHex(sector.rawSha256()));}
            out.writeLong(graphics);out.writeLong(interval);out.writeInt(LAPS);out.writeLong(FINISH_WINDOW_NANOS);out.writeInt(gates.size());
            for(var gate:gates){out.writeInt(gate.minX());out.writeInt(gate.maxX());out.writeInt(gate.minY());out.writeInt(gate.maxY());}}
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        }catch(java.io.IOException|java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("invalid modern pet race");}
}
