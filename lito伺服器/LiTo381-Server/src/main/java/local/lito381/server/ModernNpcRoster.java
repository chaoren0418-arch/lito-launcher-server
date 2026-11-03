package local.lito381.server;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Server-owned NPC lifetime and occupied-grid navigation for a single modern match.
 * NPCs are NOT room participants, SQL characters, guardian towers, or test BOTs.
 * This component does not read a database, authorize a player, calculate damage,
 * advance a Quest stage, or create a network worker. The match owner must qualify
 * templates, validate combat/spell permission, and atomically commit the attacker
 * and victim together before exposing a frame. Current public NPC projections
 * reject target-speed mutations until a new negotiated contract carries them.
 *
 * <p>Time is match-relative elapsed milliseconds supplied by the owning engine,
 * never client time. Explicit advance() retires expired corpses; failed mutations
 * do not implicitly tick time, discard corpses, or consume an identity/revision.
 * Full IDs and life numbers must survive into future target requests.</p>
 */
final class ModernNpcRoster implements AutoCloseable {
    static final int MAX_RESIDENT = 128, MAX_PLAYERS = 100, MAX_PATH_CELLS = 4096;
    static final long MAX_ELAPSED_MILLIS = 1_860_000; // 30-minute match + bounded corpse retirement.
    private static final int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] DY = {-1, -1, 0, 1, 1, 1, 0, -1};
    private static final Set<Integer> GRAPHICS = Set.of(1649, 2158, 1082, 1047, 1020, 1578, 1517, 2535, 240, 145);
    static boolean supportedGraphic(int graphic) { return GRAPHICS.contains(graphic); }

    record Scope(long roomId, long matchId, long generation) {
        Scope { if (roomId < 1 || matchId < 1 || generation < 1) throw bad(); }
    }
    record Id(Scope scope, long serial) {
        Id { Objects.requireNonNull(scope); if (serial < 1) throw bad(); }
    }
    /** Source identifies a selected NPC catalog, not the source of a player attacking it. */
    record Template(int catalogSourceId, String catalogDatabase, int npcId, int graphic, long maximumHp, long maximumMp, int walkMillis, int attackMillis) {
        Template {
            ModernPhysicalCombat.identifier(catalogDatabase);
            if (catalogSourceId < 1 || npcId < 1 || !GRAPHICS.contains(graphic) || maximumHp < 1
                    || maximumMp < 0 || walkMillis < 1 || walkMillis > 60_000 || attackMillis < 1 || attackMillis > 60_000) throw bad();
        }
        @Override public String toString() { return "NpcRosterTemplate[redacted]"; }
    }
    record Spawn(Template template, int lane, int stage, ModernWorldMap.Cell cell, int heading) {
        Spawn {
            Objects.requireNonNull(template); Objects.requireNonNull(cell);
            if (lane < 0 || lane > 1 || stage < 1 || stage > 4 || heading < 0 || heading > 7) throw bad();
        }
    }
    /** Last committed animation event, not proof that a client animation is still playing. */
    enum Action { IDLE, WALK, HURT, DEATH, ATTACK }
    record Actor(Id id, Template template, int lane, int stage, ModernWorldMap.Cell cell, int heading,
                 long hp, long mp, long life, Action action, long bornRevision, long changedRevision,
                 long corpseUntilMillis, long nextMoveMillis, long nextAttackMillis, ModernNpcMotion.Walk walk,
                 ModernNpcSpeed.State speed, ModernNpcSpeed.State walkSpeed) {
        Actor(Id id, Template template, int lane, int stage, ModernWorldMap.Cell cell, int heading,
                long hp, long mp, long life, Action action, long bornRevision, long changedRevision,
                long corpseUntilMillis, long nextMoveMillis, long nextAttackMillis, ModernNpcMotion.Walk walk) {
            this(id, template, lane, stage, cell, heading, hp, mp, life, action, bornRevision, changedRevision,
                    corpseUntilMillis, nextMoveMillis, nextAttackMillis, walk, ModernNpcSpeed.initial(), ModernNpcSpeed.initial());
        }
        Actor {
            Objects.requireNonNull(id); Objects.requireNonNull(template); Objects.requireNonNull(cell); Objects.requireNonNull(action);
            Objects.requireNonNull(speed); Objects.requireNonNull(walkSpeed);
            if (lane < 0 || lane > 1 || stage < 1 || stage > 4 || heading < 0 || heading > 7
                    || hp < 0 || hp > template.maximumHp() || mp < 0 || mp > template.maximumMp() || life < 1
                    || bornRevision < 1 || changedRevision < bornRevision || corpseUntilMillis < 0
                    || corpseUntilMillis > MAX_ELAPSED_MILLIS + 60_000 || nextMoveMillis < 0
                    || nextMoveMillis > MAX_ELAPSED_MILLIS + 120_000 || nextAttackMillis < 0 || nextAttackMillis > MAX_ELAPSED_MILLIS + 120_000
                    || (hp == 0) != (action == Action.DEATH) || (hp == 0) != (corpseUntilMillis > 0)
                    || hp == 0 && (nextMoveMillis != 0 || nextAttackMillis != 0 || speed.hasEffects())
                    || speed.revision() > changedRevision || speed.revision() != 0 && speed.revision() <= bornRevision
                    || walk == null && walkSpeed.revision() != 0
                    || walk != null && (walkSpeed.revision() > speed.revision()
                        || walkSpeed.revision() == speed.revision() && !walkSpeed.equals(speed)
                        || walkSpeed.revision() >= walk.rosterRevision()
                        || walkSpeed.revision() != 0 && walkSpeed.revision() <= bornRevision)) throw bad();
            if (walk != null) {
                walkSpeed.at(walk.elapsedMillis());
                walk.validate(id, ModernNpcSpeed.pace(ModernNpcMotion.Pace.from(template), walkSpeed), life, bornRevision, changedRevision, cell);
            }
            if (action == Action.WALK && (walk == null || walk.life() != life || walk.heading() != heading
                    || nextMoveMillis != Math.addExact(walk.elapsedMillis(), walk.durationMillis()))) throw bad();
        }
        boolean alive() { return hp > 0; }
        ModernNpcMotion.Pace pace() { return ModernNpcSpeed.pace(ModernNpcMotion.Pace.from(template), speed); }
    }
    record Snapshot(Scope scope, ModernWorldMap.Manifest manifest, long revision, long elapsedMillis,
                    long spawned, boolean closed, List<Actor> actors,boolean isolatedForces) {
        Snapshot(Scope scope,ModernWorldMap.Manifest manifest,long revision,long elapsedMillis,long spawned,boolean closed,List<Actor> actors) {
            this(scope,manifest,revision,elapsedMillis,spawned,closed,actors,false);
        }
        Snapshot {
            Objects.requireNonNull(scope); Objects.requireNonNull(manifest); actors = List.copyOf(actors);
            if (revision < 1 || elapsedMillis < 0 || elapsedMillis > MAX_ELAPSED_MILLIS || spawned < 0
                    || actors.size() > MAX_RESIDENT || closed && !actors.isEmpty()) throw bad();
            long previous = 0; var living = new HashSet<ModernQuestSpace.Cell>();
            for (var actor : actors) {
                actor.speed().at(elapsedMillis);
                if (!actor.id().scope().equals(scope) || actor.id().serial() <= previous || actor.id().serial() > spawned
                        || actor.changedRevision() > revision || !ModernWorldProtocol.contains(manifest, actor.cell())
                        || actor.alive() && !living.add(ModernQuestSpace.cell(isolatedForces,actor.lane(),actor.cell()))
                        || !actor.alive() && actor.corpseUntilMillis() <= elapsedMillis
                        || actor.walk() != null && (actor.walk().elapsedMillis() > elapsedMillis
                            || !ModernWorldProtocol.contains(manifest, actor.walk().from()))) throw bad();
                previous = actor.id().serial();
            }
        }
    }
    /** Frozen owner-qualified participant target, never an NPC cast to a character key. */
    record Target(ModernParticipantId participant, int lane, ModernWorldMap.Cell cell, boolean alive) {
        Target {
            Objects.requireNonNull(participant); Objects.requireNonNull(cell);
            if (lane < 0 || lane > 1) throw bad();
        }
    }
    enum PlanKind { NONE, WAIT, MOVE, IN_RANGE, BUDGET, CANCELLED }
    /** IN_RANGE is a proposal for the combat owner, NOT a hit, cooldown or damage commitment. */
    record Plan(Id actor, long life, long revision, PlanKind kind, int heading,
                ModernParticipantId target, int visited) {
        Plan {
            Objects.requireNonNull(actor); Objects.requireNonNull(kind);
            if (life < 1 || revision < 1 || visited < 0 || visited > MAX_PATH_CELLS
                    || (kind == PlanKind.MOVE ? heading < 0 || heading > 7 : heading != -1)
                    || (kind == PlanKind.MOVE || kind == PlanKind.IN_RANGE) != (target != null)) throw bad();
        }
    }
    private record PathNode(ModernWorldMap.Cell cell, ModernWorldMap.Cell from, int heading,
                            int cost, int firstHeading, int estimate, int order) {
        PathNode withEstimate(int value) { return new PathNode(cell, from, heading, cost, firstHeading, value, order); }
    }
    private static final java.util.Comparator<PathNode> PATH_ORDER = java.util.Comparator
            .comparingLong((PathNode n) -> (long)n.cost() + n.estimate())
            .thenComparingInt(PathNode::firstHeading)
            .thenComparingInt(PathNode::estimate)
            .thenComparingInt(PathNode::order);
    /** Consistent lower bound to any eligible target's attack-neighbour cell.
     * Obstacles can only lengthen the route. This never grants a straight-line
     * move, ignores neither occupancy nor a wall, and narrows no world value. */
    private static int estimate(ModernWorldMap.Cell cell, List<Target> targets) {
        long closest = Integer.MAX_VALUE;
        for (var target : targets) closest = Math.min(closest, Math.max(
                Math.abs((long)cell.x() - target.cell().x()), Math.abs((long)cell.y() - target.cell().y())));
        return (int)Math.max(0, closest - 1);
    }
    /** Private navigation work, never an actor revision or a movement receipt.
     * At most one bounded search/route per resident NPC. The expanded-cell
     * certificate records exactly which one-cell edge neighbourhoods have
     * been examined. Unrelated moving actors need not restart that work. */
    private static final class Pursuit {
        final long life;
        ModernWorldMap.Cell origin;
        List<Target> targets;
        Set<ModernWorldMap.Cell> occupied;
        final Map<ModernWorldMap.Cell, PathNode> seen = new HashMap<>();
        final java.util.TreeSet<PathNode> pending = new java.util.TreeSet<>(PATH_ORDER);
        final Set<ModernWorldMap.Cell> expanded = new HashSet<>();
        final ArrayDeque<Integer> route = new ArrayDeque<>();
        ModernParticipantId target;
        boolean truncated;
        int nextOrder = 1;
        Pursuit(Actor actor, List<Target> targets, Set<ModernWorldMap.Cell> occupied) {
            life = actor.life(); origin = actor.cell();
            this.targets = List.copyOf(targets); this.occupied = Set.copyOf(occupied);
            var root = new PathNode(origin, null, -1, 0, -1, estimate(origin, this.targets), 0);
            seen.put(origin, root); pending.add(root);
        }
        boolean matches(Actor actor, List<Target> targets, Set<ModernWorldMap.Cell> occupied) {
            if (life != actor.life() || !origin.equals(actor.cell()) || this.targets.size() != targets.size()) return false;
            boolean sameTargets = this.targets.equals(targets), sameOccupied = this.occupied.equals(occupied);
            if (sameTargets && sameOccupied) return true;
            if (truncated || target != null && !sameTargets) return false;
            for (int i = 0; i < targets.size(); i++) {
                var previous = this.targets.get(i); var current = targets.get(i);
                if (!previous.participant().equals(current.participant()) || previous.lane() != current.lane()) return false;
            }
            // Every inspected edge (including its diagonal side cells) lies
            // within one cell of an expanded node. An added OR removed block
            // there can invalidate cost/parent proofs, even off the final path.
            for (var cell : this.occupied) if (!occupied.contains(cell) && touchesExpanded(cell)) return false;
            for (var cell : occupied) if (!this.occupied.contains(cell) && touchesExpanded(cell)) return false;
            return true;
        }
        private boolean touchesExpanded(ModernWorldMap.Cell cell) {
            if (expanded.contains(cell)) return true;
            for (int h = 0; h < 8; h++) if (expanded.contains(neighbor(cell, h))) return true;
            return false;
        }
        /** Called only after matches proved all inspected graph edges intact.
         * Settled costs stay valid; frontier keys must use the new target.
         * Re-keying is bounded by the retained frontier, separate from the
         * per-call edge-expansion budget. No heap duplicates are retained. */
        boolean retarget(List<Target> targets, Map<ModernWorldMap.Cell, Target> goals, BooleanSupplier cancelled) {
            if (this.targets.equals(targets)) return true;
            this.targets = List.copyOf(targets);
            var frontier = new ArrayList<>(pending); pending.clear();
            for (var previous : frontier) {
                if (cancelled.getAsBoolean()) return false;
                var current = previous.withEstimate(estimate(previous.cell(), targets));
                seen.put(current.cell(), current); pending.add(current);
            }
            // A moving target may turn an already settled cell into a goal.
            // Reinsert it with h=0; the ordinary A* ordering still proves the
            // shortest live goal rather than immediately accepting stale work.
            for (var cell : goals.keySet()) {
                if (cancelled.getAsBoolean()) return false;
                var previous = seen.get(cell);
                if (previous == null || !expanded.contains(cell)) continue;
                pending.remove(previous);
                var current = previous.withEstimate(0); seen.put(cell, current); pending.add(current);
            }
            return true;
        }
        void complete(ModernWorldMap.Cell end, ModernParticipantId target) {
            for (var cell = end; !cell.equals(origin);) {
                var link = seen.get(cell); route.addFirst(link.heading()); cell = link.from();
            }
            this.target = target; seen.clear(); pending.clear();
            // Keep only the bounded dependency certificate with the route.
            // Removing an off-route wall could otherwise hide a shorter path.
        }
    }

    private final Scope scope;
    private final ModernWorldMap map;
    private final List<ModernWorldMap.Cell> laneAnchors;
    private final int capacity;
    private final long corpseMillis;
    private final boolean isolatedForces;
    private final boolean cooperative;
    private Map<Id, Actor> actors = new LinkedHashMap<>();
    private final Map<Id, Pursuit> pursuits = new HashMap<>();
    private long revision = 1, elapsedMillis, spawned;
    private boolean closed;

    ModernNpcRoster(Scope scope, ModernWorldMap map, List<ModernWorldMap.Cell> laneAnchors, int capacity, long corpseMillis) {
        this(scope,map,laneAnchors,capacity,corpseMillis,false);
    }
    ModernNpcRoster(Scope scope, ModernWorldMap map, List<ModernWorldMap.Cell> laneAnchors, int capacity, long corpseMillis,boolean isolatedForces) {
        this(scope,map,laneAnchors,capacity,corpseMillis,isolatedForces,false);
    }
    ModernNpcRoster(Scope scope,ModernWorldMap map,List<ModernWorldMap.Cell> laneAnchors,int capacity,long corpseMillis,boolean isolatedForces,boolean cooperative) {
        this.scope = Objects.requireNonNull(scope); this.map = Objects.requireNonNull(map);
        this.isolatedForces=isolatedForces;
        this.cooperative=cooperative;
        this.laneAnchors = List.copyOf(laneAnchors);
        if (cooperative&&isolatedForces||this.laneAnchors.size() != (cooperative?1:2) || !cooperative&&!isolatedForces&&this.laneAnchors.get(0).equals(this.laneAnchors.get(1))
                || this.laneAnchors.stream().anyMatch(c -> !map.standing(c))) throw bad();
        if (capacity < 1 || capacity > MAX_RESIDENT || corpseMillis < 1 || corpseMillis > 60_000) throw bad();
        this.capacity = capacity; this.corpseMillis = corpseMillis;
    }

    synchronized Snapshot snapshot() {
        return new Snapshot(scope, map.manifest(), revision, elapsedMillis, spawned, closed, List.copyOf(actors.values()),isolatedForces);
    }

    /** Spawn a complete wave/summon batch atomically, including source and occupied-cell checks. */
    synchronized List<Actor> spawn(Scope requested, List<Spawn> batch, Set<ModernWorldMap.Cell> players) {
        requireScope(requested);
        if (batch.size() > capacity - actors.size()) throw bad();
        batch = List.copyOf(batch);
        if(cooperative&&batch.stream().anyMatch(s->s.lane()!=0))throw bad();
        if (batch.isEmpty() || batch.size() > capacity - actors.size()) throw bad();
        int lane=batch.get(0).lane();if(isolatedForces&&batch.stream().anyMatch(s->s.lane()!=lane))throw bad();
        var occupied = occupied(players, null,lane);
        for (var row : batch) {
            if (!map.standing(row.cell()) || !occupied.add(row.cell())) throw bad();
        }
        // Each lane is a separate encounter. All actors of a new batch within a
        // lane must be reachable from that lane's existing living actors.
        for (int force = 0; force < laneAnchors.size(); force++) {
            var cells = new ArrayList<ModernWorldMap.Cell>(); cells.add(laneAnchors.get(force));
            for (var actor : actors.values()) if (actor.alive() && actor.lane() == force
                    && !actor.cell().equals(laneAnchors.get(force))) cells.add(actor.cell());
            for (var row : batch) if (row.lane() == force && !row.cell().equals(laneAnchors.get(force))) cells.add(row.cell());
            if (!cells.isEmpty()) validateReachable(cells);
        }
        long next = Math.incrementExact(revision), last = Math.addExact(spawned, batch.size());
        var planned = new LinkedHashMap<>(actors);
        var added = new ArrayList<Actor>();
        long serial = spawned;
        for (var row : batch) {
            var id = new Id(scope, ++serial);
            var actor = new Actor(id, row.template(), row.lane(), row.stage(), row.cell(), row.heading(),
                    row.template().maximumHp(), row.template().maximumMp(), 1, Action.IDLE, next, next, 0,
                    Math.addExact(elapsedMillis, row.template().walkMillis()), 0, null);
            planned.put(id, actor); added.add(actor);
        }
        actors = planned; spawned = last; revision = next;
        return List.copyOf(added);
    }

    private void validateReachable(List<ModernWorldMap.Cell> cells) {
        // Existing map API has a 100-player spawn bound. NPC residency is 128;
        // validate overlapping chunks from the same anchor without relaxing it.
        for (int from = 1; from < cells.size(); from += ModernWorldMap.MAX_SPAWNS - 1) {
            var part = new ArrayList<ModernWorldMap.Cell>(); part.add(cells.get(0));
            part.addAll(cells.subList(from, Math.min(cells.size(), from + ModernWorldMap.MAX_SPAWNS - 1)));
            try { map.validateSpawns(part); } catch (ModernWorldMap.MapException e) { throw bad(); }
        }
    }

    synchronized Snapshot advance(Scope requested, long nowMillis) {
        requireScope(requested);
        if (nowMillis < elapsedMillis || nowMillis > MAX_ELAPSED_MILLIS) throw bad();
        if (nowMillis == elapsedMillis) return snapshot();
        long next = Math.incrementExact(revision);
        var planned = new LinkedHashMap<>(actors);
        planned.values().removeIf(a -> !a.alive() && nowMillis >= a.corpseUntilMillis());
        planned.replaceAll((id, a) -> {
            var speed = ModernNpcSpeed.expire(a.speed(), nowMillis, next);
            return speed == a.speed() ? a : withSpeed(a, speed, next);
        });
        actors = planned; elapsedMillis = nowMillis; revision = next;
        pursuits.keySet().removeIf(id -> !actors.containsKey(id));
        return snapshot();
    }

    /** Damage amount is already qualified by the combat owner; full-width HP is never narrowed. */
    synchronized Actor damage(Scope requested, Id id, long life, long amount) {
        return damage(requested,id,life,amount,false);
    }
    /** Final cooperative damage and effect retirement publish one roster swap. */
    synchronized Actor damage(Scope requested, Id id, long life, long amount, boolean clearHaste) {
        var actor = requireAlive(requested, id, life);
        if (amount < 0) throw bad();
        long loss = Math.min(amount, actor.hp());
        if (loss == 0) {
            if(clearHaste)clearHaste(requested);
            return actors.get(id);
        }
        long hp = actor.hp() - loss;
        return replace(actor, actor.cell(), actor.heading(), hp, actor.mp(), life,
                hp == 0 ? Action.DEATH : Action.HURT,
                hp == 0 ? Math.addExact(elapsedMillis, corpseMillis) : 0, hp == 0 ? 0 : actor.nextMoveMillis(),
                hp == 0 ? 0 : actor.nextAttackMillis(),actor.walk(),clearHaste);
    }

    /**
     * Victim-side compare-and-commit only. The enclosing match owner still owns
     * authorization, attacker life/equipment/cooldown and atomic publication of
     * the complete exchange. An old frame, changed victim, or revived life must
     * never apply an already calculated hit to a different current state.
     */
    synchronized Actor commitIncoming(ModernNpcMelee.Incoming hit) {
        Objects.requireNonNull(hit);
        return commitIncomingDamage(hit.scope(),hit.revision(),hit.before(),hit.outcome().appliedDamage(),hit.outcome().remainingHp());
    }

    /** The same victim owner/CAS as melee; does not debit the caster's MP. */
    synchronized Actor commitIncoming(ModernNpcMagic.Incoming hit) {
        Objects.requireNonNull(hit);
        return commitIncomingDamage(hit.scope(),hit.revision(),hit.before(),hit.outcome().appliedDamage(),hit.outcome().remainingHp());
    }

    /** Skill77 retains its typed NPC target, source proof and distinct outcome.
     * This compare-and-commit never grants casting or debits the participant. */
    synchronized Actor commitIncoming(ModernNpcDisintegrate.Incoming hit) {
        Objects.requireNonNull(hit);
        return commitIncomingDamage(hit.scope(),hit.revision(),hit.before(),hit.outcome().appliedDamage(),hit.outcome().remainingHp());
    }

    /** Target-only atomic transition. Does not debit MP or authorize a cast. */
    synchronized Actor commitIncoming(ModernNpcHaste.Incoming plan) {
        Objects.requireNonNull(plan);
        var current = requireAlive(plan.scope(), plan.before().id(), plan.before().life());
        if (plan.revision() != revision || plan.elapsedMillis() != elapsedMillis || !plan.before().equals(current)) throw bad();
        long next = Math.incrementExact(revision);
        var after = withSpeed(current, plan.after(), next);
        var planned = new LinkedHashMap<>(actors); planned.put(after.id(), after);
        actors = planned; revision = next;
        return after;
    }

    /** Slow victim CAS only. Material, MP, immunity and hit qualification belong to the engine. */
    synchronized Actor commitIncoming(ModernNpcSlow.Incoming plan) {
        Objects.requireNonNull(plan);
        var current = requireAlive(plan.scope(), plan.before().id(), plan.before().life());
        if (plan.revision() != revision || plan.elapsedMillis() != elapsedMillis || !plan.before().equals(current)) throw bad();
        long next = Math.incrementExact(revision);
        var after = withSpeed(current, plan.after(), next);
        var planned = new LinkedHashMap<>(actors); planned.put(after.id(), after);
        actors = planned; revision = next;
        return after;
    }

    private static Actor withSpeed(Actor before, ModernNpcSpeed.State speed, long nextRevision) {
        return new Actor(before.id(), before.template(), before.lane(), before.stage(), before.cell(), before.heading(),
                before.hp(), before.mp(), before.life(), before.action(), before.bornRevision(), nextRevision,
                before.corpseUntilMillis(), before.nextMoveMillis(), before.nextAttackMillis(), before.walk(), speed, before.walkSpeed());
    }

    private Actor commitIncomingDamage(Scope scope,long plannedRevision,Actor before,long applied,long remaining) {
        var current=requireAlive(scope,before.id(),before.life());
        if(plannedRevision!=revision||!current.equals(before)||applied<0||applied>current.hp()||remaining!=current.hp()-applied)throw bad();
        // Consume a miss/fully-reduced plan once too, without fabricating HURT
        // or retriggering the victim's last animation event.
        if(applied==0){revision=Math.incrementExact(revision);return current;}
        return damage(scope,current.id(),current.life(),applied);
    }

    /** One bounded AoE CAS, including empty/zero-damage casts. Every new actor
     * and the complete immutable return value are built before publishing; a
     * stale or invalid last target cannot leave earlier victims half-damaged. */
    synchronized List<Actor> commitIncoming(ModernNpcFireStorm.Incoming batch) {
        return commitIncoming(batch,false);
    }
    /** Final Quest AoE damage and status retirement share one roster CAS. */
    synchronized List<Actor> commitIncoming(ModernNpcFireStorm.Incoming batch,boolean clearHaste) {
        Objects.requireNonNull(batch);requireScope(batch.scope());
        if(batch.revision()!=revision)throw bad();
        long next=Math.incrementExact(revision);var planned=new LinkedHashMap<>(actors);var changed=new ArrayList<Actor>();
        for(var hit:batch.hits()) {
            var before=hit.before();var current=requireAlive(batch.scope(),before.id(),before.life());
            if(!before.equals(current))throw bad();
            var out=hit.outcome();Actor after=current;
            if(out.appliedDamage()>0)after=new Actor(current.id(),current.template(),current.lane(),current.stage(),current.cell(),current.heading(),
                    out.remainingHp(),current.mp(),current.life(),out.death()?Action.DEATH:Action.HURT,current.bornRevision(),next,
                    out.death()?Math.addExact(elapsedMillis,corpseMillis):0,out.death()?0:current.nextMoveMillis(),out.death()?0:current.nextAttackMillis(),current.walk(),
                    out.death()?ModernNpcSpeed.clear(current.speed(),elapsedMillis,next):current.speed(),current.walkSpeed());
            planned.put(current.id(),after);changed.add(after);
        }
        if(clearHaste)clearHaste(planned,next);
        var result=changed.stream().map(a->planned.get(a.id())).toList();actors=planned;revision=next;
        pursuits.keySet().removeIf(id -> !actors.get(id).alive());
        return result;
    }

    /** Terminal melee does not damage NPCs. Build ALL replacement actors
     * before publishing; never touch their committed walk or action clocks. */
    synchronized void clearHaste(Scope requested) {
        requireScope(requested);
        if(actors.values().stream().noneMatch(a->a.speed().hasEffects()))return;
        long next=Math.incrementExact(revision);var planned=new LinkedHashMap<>(actors);
        clearHaste(planned,next);actors=planned;revision=next;
    }
    private void clearHaste(Map<Id,Actor> planned,long next) {
        planned.replaceAll((id,a)->a.speed().hasEffects()?withSpeed(a,ModernNpcSpeed.clear(a.speed(),elapsedMillis,next),next):a);
    }

    synchronized Actor heal(Scope requested, Id id, long life, long amount) {
        var actor = requireAlive(requested, id, life);
        if (amount < 0) throw bad();
        long gain = Math.min(amount, actor.template().maximumHp() - actor.hp());
        if (gain == 0) return actor;
        return replace(actor, actor.cell(), actor.heading(), actor.hp() + gain, actor.mp(), life,
                actor.action(), 0, actor.nextMoveMillis());
    }

    synchronized Actor drainMana(Scope requested, Id id, long life, long amount) {
        var actor = requireAlive(requested, id, life);
        if (amount < 0) throw bad();
        long loss = Math.min(amount, actor.mp());
        if (loss == 0) return actor;
        return replace(actor, actor.cell(), actor.heading(), actor.hp(), actor.mp() - loss, life,
                actor.action(), 0, actor.nextMoveMillis());
    }

    /** Reuses the corpse identity but increments life; old-life attacks cannot hit the revived NPC. */
    synchronized Actor resurrect(Scope requested, Id id, long life, long hp, long mp, Set<ModernWorldMap.Cell> players) {
        var actor = requireActor(requested, id, life);
        if (actor.alive() || elapsedMillis >= actor.corpseUntilMillis() || hp < 1 || hp > actor.template().maximumHp()
                || mp < 0 || mp > actor.template().maximumMp() || occupied(players, id).contains(actor.cell())) throw bad();
        return replace(actor, actor.cell(), actor.heading(), hp, mp, Math.incrementExact(life), Action.IDLE, 0,
                Math.addExact(elapsedMillis, actor.template().walkMillis()), Math.addExact(elapsedMillis, actor.template().attackMillis()));
    }

    /** Apply only the current plan and current occupancy; no skipped ticks or delayed move burst. */
    synchronized Actor move(Scope requested, Plan plan, Set<ModernWorldMap.Cell> players) {
        Objects.requireNonNull(plan);
        var actor = requireAlive(requested, plan.actor(), plan.life());
        if (plan.kind() != PlanKind.MOVE || plan.revision() != revision || plan.heading() < 0 || plan.heading() > 7
                || elapsedMillis < actor.nextMoveMillis() || elapsedMillis < actor.nextAttackMillis()) throw bad();
        var to = neighbor(actor.cell(), plan.heading());
        var occupied = occupied(players, actor.id());
        if (!step(actor.cell(), to, occupied)) throw bad();
        var pursuit = pursuits.get(actor.id());
        var walk = ModernNpcMotion.Walk.next(actor, Math.incrementExact(revision), elapsedMillis, to, plan.heading());
        var after = replace(actor, to, plan.heading(), actor.hp(), actor.mp(), actor.life(), Action.WALK, 0,
                Math.addExact(elapsedMillis, actor.pace().walkMillis()), actor.nextAttackMillis(), walk);
        if (pursuit != null && pursuit.life == actor.life() && pursuit.origin.equals(actor.cell())
                && pursuit.occupied.equals(occupied) && !pursuit.route.isEmpty()
                && pursuit.route.peekFirst() == plan.heading() && Objects.equals(pursuit.target, plan.target())) {
            pursuit.route.removeFirst(); pursuit.origin = to; pursuits.put(actor.id(), pursuit);
        }
        return after;
    }

    /**
     * Commit the NPC's ordinary-melee action/clock, not the participant's HP.
     * The outer combat owner must finish calculation and validate its target
     * life/HP/equipment before this, then publish both sides atomically. Source
     * attackMillis is separate from walkMillis; late ticks never replay a burst.
     */
    synchronized Actor attack(Scope requested, Plan plan, Target currentTarget, Set<ModernWorldMap.Cell> players) {
        return attack(requested,plan,currentTarget,players,false);
    }
    /** Preserve the committed attack clock while retiring terminal haste. */
    synchronized Actor attack(Scope requested, Plan plan, Target currentTarget, Set<ModernWorldMap.Cell> players, boolean clearHaste) {
        Objects.requireNonNull(plan); Objects.requireNonNull(currentTarget);
        var actor = requireAlive(requested, plan.actor(), plan.life());
        if (plan.kind() != PlanKind.IN_RANGE || plan.revision() != revision || !currentTarget.alive()
                || !currentTarget.participant().equals(plan.target()) || currentTarget.lane() != actor.lane()
                || !players.contains(currentTarget.cell()) || elapsedMillis < actor.nextMoveMillis() || elapsedMillis < actor.nextAttackMillis()) throw bad();
        int dx = currentTarget.cell().x() - actor.cell().x(), dy = currentTarget.cell().y() - actor.cell().y(), heading = -1;
        for (int h = 0; h < 8; h++) if (dx == DX[h] && dy == DY[h]) { heading = h; break; }
        if (heading < 0 || !step(actor.cell(), currentTarget.cell(), occupied(players, actor.id()), true)) throw bad();
        return replace(actor, actor.cell(), heading, actor.hp(), actor.mp(), actor.life(), Action.ATTACK, 0,
                actor.nextMoveMillis(), Math.addExact(elapsedMillis, actor.pace().attackMillis()),actor.walk(),clearHaste);
    }

    /**
     * Deterministic shortest occupied-grid route to any alive participant in this
     * NPC's lane. Targets use full source-qualified identities. Budget bounds
     * A* node expansions per call; at most MAX_PATH_CELLS nodes are retained per
     * NPC. Unrelated occupancy changes preserve work; changed inspected edges
     * invalidate it, and target movement re-keys a still-valid frontier.
     * No guessed step or actor mutation is published by planning. Cancellation
     * discards private work; successful routes still require move()'s live CAS.
     */
    synchronized Plan pursue(Scope requested, Id id, long life, List<Target> targets,
                            Set<ModernWorldMap.Cell> players, int budget, BooleanSupplier cancelled) {
        var actor = requireAlive(requested, id, life);
        Objects.requireNonNull(cancelled);
        if (budget < 1 || budget > MAX_PATH_CELLS || targets.size() > MAX_PLAYERS) throw bad();
        var occupied = occupied(players, actor.id());
        var identities = new HashSet<ModernParticipantId>();
        var livingCells = new HashSet<ModernWorldMap.Cell>();
        var eligible = new ArrayList<Target>();
        for (var target : List.copyOf(targets)) {
            if(isolatedForces&&target.lane()!=actor.lane())throw bad();
            if (!identities.add(target.participant()) || !map.standing(target.cell())) throw bad();
            if (target.alive() && (!players.contains(target.cell()) || !livingCells.add(target.cell())
                    || target.cell().equals(actor.cell()))) throw bad();
            if (target.alive() && target.lane() == actor.lane()) eligible.add(target);
        }
        eligible.sort(java.util.Comparator.comparing(Target::participant));
        if (cancelled.getAsBoolean()) return cancelPursuit(actor, 0);
        if (eligible.isEmpty()) { pursuits.remove(id); return plan(actor, PlanKind.NONE, -1, null, 0); }
        // Cooldown ticks need only the current attack neighbour, not every
        // possible endpoint for an A* search that cannot run yet. Preserve
        // source ordering and cancellation before choosing that neighbour.
        Target immediate = null;
        for (var target : eligible) {
            if (cancelled.getAsBoolean()) return cancelPursuit(actor, 0);
            if (immediate == null && step(actor.cell(), target.cell(), occupied, true)) immediate = target;
        }
        if (immediate != null) { pursuits.remove(id); return plan(actor, PlanKind.IN_RANGE, -1, immediate.participant(), 1); }
        // In-range is only a proposal; a movement cooldown must not masquerade
        // as permission to bypass the separate attack clock checked by attack().
        if (elapsedMillis < actor.nextMoveMillis() || elapsedMillis < actor.nextAttackMillis()) return plan(actor, PlanKind.WAIT, -1, null, 0);
        // At most eight goal cells per target. Never rescan 100 targets or copy
        // a 228-cell occupancy set for each of the 4096 visited cells.
        var goals = new HashMap<ModernWorldMap.Cell, Target>();
        for (var target : eligible) {
            if (cancelled.getAsBoolean()) return cancelPursuit(actor, 0);
            for (int heading = 0; heading < 8; heading++) {
                var goal = neighbor(target.cell(), heading);
                if (!occupied.contains(goal) && step(goal, target.cell(), occupied, true)) goals.putIfAbsent(goal, target);
            }
        }
        var pursuit = pursuits.get(id);
        if (pursuit == null || !pursuit.matches(actor, eligible, occupied)) {
            pursuit = new Pursuit(actor, eligible, occupied); pursuits.put(id, pursuit);
        }
        if (!pursuit.retarget(eligible, goals, cancelled)) return cancelPursuit(actor, 0);
        if (!pursuit.occupied.equals(occupied)) pursuit.occupied = Set.copyOf(occupied);
        if (!pursuit.route.isEmpty()) return plan(actor, PlanKind.MOVE, pursuit.route.peekFirst(), pursuit.target, 0);
        int visited = 0;
        while (!pursuit.pending.isEmpty() && visited < budget) {
            if (cancelled.getAsBoolean()) return cancelPursuit(actor, visited);
            var node = pursuit.pending.pollFirst(); var cell = node.cell(); visited++;
            pursuit.expanded.add(cell);
            var target = goals.get(cell);
            if (target != null) {
                pursuit.complete(cell, target.participant());
                return plan(actor, PlanKind.MOVE, pursuit.route.peekFirst(), pursuit.target, visited);
            }
            for (int heading = 0; heading < 8; heading++) {
                var next = neighbor(cell, heading);
                int cost = node.cost() + 1, first = node.firstHeading() < 0 ? heading : node.firstHeading();
                var previous = pursuit.seen.get(next);
                if (previous != null && (previous.cost() < cost || previous.cost() == cost && previous.firstHeading() <= first)) continue;
                if (!step(cell, next, occupied)) continue;
                if (previous == null && pursuit.seen.size() == MAX_PATH_CELLS) {
                    // Once a potentially better frontier cannot be retained,
                    // accepting another goal could claim a non-shortest route.
                    // Keep the bounded diagnostic, never guess a move.
                    pursuit.truncated = true; pursuit.pending.clear();
                    return plan(actor, PlanKind.BUDGET, -1, null, visited);
                }
                var added = new PathNode(next, cell, heading, cost, first,
                        estimate(next, eligible),
                        previous == null ? pursuit.nextOrder++ : previous.order());
                // Remove before replacing: no duplicate heap entries may evade
                // the per-NPC memory or per-call expansion bounds.
                if (previous != null) pursuit.pending.remove(previous);
                pursuit.seen.put(next, added); pursuit.pending.add(added);
            }
        }
        return plan(actor, pursuit.truncated || !pursuit.pending.isEmpty() ? PlanKind.BUDGET : PlanKind.NONE,
                -1, null, visited);
    }

    private Plan cancelPursuit(Actor actor, int visited) {
        pursuits.remove(actor.id()); return plan(actor, PlanKind.CANCELLED, -1, null, visited);
    }

    private Plan plan(Actor actor, PlanKind kind, int heading, ModernParticipantId target, int visited) {
        return new Plan(actor.id(), actor.life(), revision, kind, heading, target, visited);
    }
    private Set<ModernWorldMap.Cell> occupied(Set<ModernWorldMap.Cell> players, Id except) {
        return occupied(players,except,except==null?-1:actors.get(except).lane());
    }
    private Set<ModernWorldMap.Cell> occupied(Set<ModernWorldMap.Cell> players, Id except,int lane) {
        if(isolatedForces&&(lane<0||lane>1))throw bad();
        if (players.size() > MAX_PLAYERS) throw bad();
        players = Set.copyOf(players);
        if (players.size() > MAX_PLAYERS) throw bad();
        var result = new HashSet<>(players);
        for (var cell : players) if (!map.standing(cell)) throw bad();
        for (var actor : actors.values()) if (actor.alive()&&(!isolatedForces||actor.lane()==lane)) {
            if (players.contains(actor.cell())) throw bad();
            if (!actor.id().equals(except)) result.add(actor.cell());
        }
        return result;
    }
    private boolean step(ModernWorldMap.Cell from, ModernWorldMap.Cell to, Set<ModernWorldMap.Cell> occupied) {
        return step(from, to, occupied, false);
    }
    private boolean step(ModernWorldMap.Cell from, ModernWorldMap.Cell to, Set<ModernWorldMap.Cell> occupied,
                         boolean targetOwnsDestination) {
        if (!targetOwnsDestination && occupied.contains(to) || !map.canStep(from, to)) return false;
        if (from.x() == to.x() || from.y() == to.y()) return true;
        var a = new ModernWorldMap.Cell(to.x(), from.y());
        var b = new ModernWorldMap.Cell(from.x(), to.y());
        return !occupied.contains(a) && map.canStep(from, a) && map.canStep(a, to)
                || !occupied.contains(b) && map.canStep(from, b) && map.canStep(b, to);
    }
    private static ModernWorldMap.Cell neighbor(ModernWorldMap.Cell from, int heading) {
        return new ModernWorldMap.Cell(Math.addExact(from.x(), DX[heading]), Math.addExact(from.y(), DY[heading]));
    }
    private Actor replace(Actor before, ModernWorldMap.Cell cell, int heading, long hp, long mp, long life,
                          Action action, long corpseUntil, long nextMove) {
        return replace(before, cell, heading, hp, mp, life, action, corpseUntil, nextMove, hp == 0 ? 0 : before.nextAttackMillis());
    }
    private Actor replace(Actor before, ModernWorldMap.Cell cell, int heading, long hp, long mp, long life,
                          Action action, long corpseUntil, long nextMove, long nextAttack) {
        return replace(before, cell, heading, hp, mp, life, action, corpseUntil, nextMove, nextAttack, before.walk());
    }
    private Actor replace(Actor before, ModernWorldMap.Cell cell, int heading, long hp, long mp, long life,
                          Action action, long corpseUntil, long nextMove, long nextAttack, ModernNpcMotion.Walk walk) {
        return replace(before,cell,heading,hp,mp,life,action,corpseUntil,nextMove,nextAttack,walk,false);
    }
    private Actor replace(Actor before, ModernWorldMap.Cell cell, int heading, long hp, long mp, long life,
                          Action action, long corpseUntil, long nextMove, long nextAttack, ModernNpcMotion.Walk walk, boolean clearHaste) {
        long next = Math.incrementExact(revision);
        var after = new Actor(before.id(), before.template(), before.lane(), before.stage(), cell, heading,
                hp, mp, life, action, before.bornRevision(), next, corpseUntil, nextMove, nextAttack, walk,
                hp == 0 || life != before.life() ? ModernNpcSpeed.clear(before.speed(), elapsedMillis, next) : before.speed(),
                walk == before.walk() ? before.walkSpeed() : before.speed());
        var planned = new LinkedHashMap<>(actors); planned.put(after.id(), after);
        if(clearHaste)clearHaste(planned,next);
        actors = planned; revision = next;
        if (!after.alive() || life != before.life() || !cell.equals(before.cell())) pursuits.remove(before.id());
        return planned.get(after.id());
    }
    private void requireScope(Scope requested) {
        if (closed || !scope.equals(requested)) throw bad();
    }
    private Actor requireActor(Scope requested, Id id, long life) {
        requireScope(requested);
        if (id == null || !scope.equals(id.scope())) throw bad();
        var actor = actors.get(id);
        if (actor == null || actor.life() != life) throw bad();
        return actor;
    }
    private Actor requireAlive(Scope requested, Id id, long life) {
        var actor = requireActor(requested, id, life);
        if (!actor.alive()) throw bad(); return actor;
    }
    @Override public synchronized void close() {
        if (closed) return;
        long next = Math.incrementExact(revision);
        actors = new LinkedHashMap<>(); pursuits.clear(); revision = next; closed = true;
    }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid modern NPC authority operation"); }
}
