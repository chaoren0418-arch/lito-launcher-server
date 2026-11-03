package local.lito381.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Complete outbound speed provenance for combined skill29/43 worlds.
 * Current status and the last committed NPC walk may come from DIFFERENT
 * lanes/casters. One target entry retains both, never two competing speeds.
 * No private scope, source evaluator, clock origin or cast authorization. */
final class ModernSpeedEffects {
    sealed interface Target permits Player,Npc {}
    record Player(ModernTargetHistory.Participant participant) implements Target {
        Player {Objects.requireNonNull(participant);}
    }
    record Npc(ModernNpcRoster.Id id) implements Target {Npc {Objects.requireNonNull(id);}}
    private sealed interface Proof permits PlayerProof,NpcProof {
        ModernWorldMap.Cell cell();long life();
    }
    private record PlayerProof(ModernTargetHistory.Participant participant,long life,long equipmentRevision,
            ModernWorldMap.Cell cell,ModernTimedEffects.State before,ModernTimedEffects.State after) implements Proof {}
    private record NpcProof(ModernNpcRoster.Id id,long life,int lane,ModernWorldMap.Cell cell,long rosterRevision,
            ModernNpcSpeed.State before,ModernNpcSpeed.State after,ModernSlowNativeRules.MissingEffect missingEffect) implements Proof {}
    private record Effect(long activeElapsedNanos,Proof target,boolean changesSpeed) {}
    record Grant(ModernTargetHistory.Participant caster,ModernBattleObservation.SpellReceipt receipt) {
        Grant {
            Objects.requireNonNull(caster);Objects.requireNonNull(receipt);
            var u=receipt.impact().use();var lane=receipt.impact().lane();
            if(!caster.identity().isHuman()||(lane!=ModernUseHistory.Lane.HASTE&&lane!=ModernUseHistory.Lane.SLOW)
                    ||lane==ModernUseHistory.Lane.HASTE&&!(receipt.detail() instanceof ModernHasteReceipt)
                    ||lane==ModernUseHistory.Lane.SLOW&&!(receipt.detail() instanceof ModernSlowReceipt)
                    ||receipt.casts()>u.sequence()||u.sequence()>=u.revision()
                    ||receipt.casts()>receipt.spent()/15||receipt.spent()>Math.multiplyExact(receipt.casts(),maximum(lane))
                    ||receipt.casts()==1&&receipt.spent()!=u.cost())throw bad();
        }
        Effect effect(){return ModernSpeedEffects.effect(receipt.detail());}
        long revision(){return receipt.impact().use().revision();}
        ModernUseHistory.Lane lane(){return receipt.impact().lane();}
        void validate(ModernTargetHistory.World world) {
            var i=receipt.impact();
            if(receipt.detail() instanceof ModernHasteReceipt d)d.validate(world,caster,i.lane(),i.use(),i.targets());
            else ((ModernSlowReceipt)receipt.detail()).validate(world,caster,i.lane(),i.use(),i.targets());
        }
    }
    record Entry(Target target,Grant active,Grant walk) {
        Entry {
            Objects.requireNonNull(target);
            if(active==null&&walk==null||active!=null&&!target.equals(ModernSpeedEffects.target(active))
                    ||walk!=null&&(!(target instanceof Npc)||!target.equals(ModernSpeedEffects.target(walk))))throw bad();
            if(active!=null&&walk!=null&&(active.revision()<walk.revision()
                    ||active.revision()==walk.revision()&&!active.equals(walk)
                    ||active.effect().target().life()<walk.effect().target().life()))throw bad();
        }
        List<Grant> grants(){return active==null?List.of(walk):walk==null||walk.equals(active)?List.of(active):List.of(active,walk);}
    }
    /** Frozen base pace, current status, and the status used by the LAST step.
     * Current haste is never retroactively applied to an older normal step. */
    record Motion(ModernNpcRoster.Id actor,long life,ModernNpcMotion.Pace base,
                  ModernNpcSpeed.State speed,ModernNpcSpeed.State walkSpeed,ModernNpcMotion.Walk walk) {
        Motion {
            Objects.requireNonNull(actor);Objects.requireNonNull(base);Objects.requireNonNull(speed);Objects.requireNonNull(walkSpeed);
            ModernNpcSpeed.requireBase(base);
            if(life<1||walk==null&&!walkSpeed.equals(ModernNpcSpeed.initial())
                    ||walk!=null&&(!walk.actor().equals(actor)||walk.life()>life
                        ||walk.durationMillis()!=ModernNpcSpeed.pace(base,walkSpeed).walkMillis()
                        ||walkSpeed.revision()>=walk.rosterRevision()))throw bad();
            if(walk!=null)walkSpeed.at(walk.elapsedMillis());
        }
        static Motion from(ModernNpcRoster.Actor a){return new Motion(a.id(),a.life(),ModernNpcMotion.Pace.from(a.template()),a.speed(),a.walkSpeed(),a.walk());}
        ModernNpcMotion.Pace currentPace(){return ModernNpcSpeed.pace(base,speed);}
    }
    record Npcs(ModernNpcRoster.Scope scope,long revision,long elapsedMillis,List<Motion> actors) {
        Npcs {
            Objects.requireNonNull(scope);actors=List.copyOf(actors);
            if(revision<1||elapsedMillis<0||elapsedMillis>ModernNpcRoster.MAX_ELAPSED_MILLIS||actors.size()>128)throw bad();
            long previous=0;
            for(var a:actors) {
                if(!scope.equals(a.actor().scope())||a.actor().serial()<=previous||a.speed().revision()>revision
                        ||a.walk()!=null&&(a.walk().rosterRevision()>revision||a.walk().elapsedMillis()>elapsedMillis))throw bad();
                a.speed().at(elapsedMillis);previous=a.actor().serial();
            }
        }
        static Npcs from(ModernNpcRoster.Snapshot n){return new Npcs(n.scope(),n.revision(),n.elapsedMillis(),n.actors().stream().map(Motion::from).toList());}
    }
    record State(ModernTargetHistory.World world,long revision,List<Entry> targets,Npcs npcs) {
        State {
            Objects.requireNonNull(world);targets=List.copyOf(targets);
            if(revision<1||targets.size()>228||npcs!=null&&!world.contains(npcs.scope()))throw bad();
            Target previous=null;int players=0,monsters=0;var events=new HashMap<Long,Grant>();
            for(var e:targets) {
                if(previous!=null&&compare(previous,e.target())>=0)throw bad();previous=e.target();
                if(e.target() instanceof Player)players++;else{monsters++;if(!world.contains(((Npc)e.target()).id().scope()))throw bad();}
                for(var g:e.grants()){if(g.revision()>revision)throw bad();event(events,g);}
            }
            if(players>100||monsters>128)throw bad();
        }
        static State from(ModernBattleObservation.Capture c) {
            if(!c.features().slow()||c.slowTargets()==null)throw bad();
            c.slowTargets().validate(c);
            var entries=new HashMap<Target,Entry>();
            if(c.hasteTargets()!=null) {
                c.hasteTargets().validate(c);
                for(var e:c.hasteTargets().targets().values())merge(entries,e.active(),e.walk(),c.loadingOriginNanos());
            }
            for(var e:c.slowTargets().targets().values())merge(entries,e.active(),e.walk(),c.loadingOriginNanos());
            var rows=entries.values().stream().sorted((x,y)->compare(x.target(),y.target())).toList();
            return new State(c.identity(),c.revision(),rows,c.npcs()==null?null:Npcs.from(c.npcs().roster()));
        }
        void validate(ModernTargetHistory.World id,long rev,ModernBattleEngine.Phase phase,ModernBattleObservation.Clock clock,
                      ModernWorldMap.Manifest manifest,List<ModernBattleObservation.Actor> actors,boolean isolated,ModernBattleObservation.Npcs raw) {
            if(!Objects.equals(npcs,raw==null?null:Npcs.from(raw.roster())))throw bad();
            validate(id,rev,phase,clock,manifest,actors,isolated,raw==null?null:raw.roster().actors().stream().map(ModernBattleAudience.Npc::fromStatus).toList(),
                    raw==null?0:raw.roster().spawned());
        }
        void validateAudience(ModernBattleAudience.World w) {
            validateAudience(w.identity(),w.revision(),w.phase(),w.clock(),w.manifest(),w.actors(),w.lane(),w.npcs());
        }
        void validateAudience(ModernTargetHistory.World id,long rev,ModernBattleEngine.Phase phase,ModernBattleObservation.Clock clock,
                ModernWorldMap.Manifest manifest,List<ModernBattleObservation.Actor> actors,int lane,ModernBattleAudience.Npcs n) {
            if((npcs==null)!=(n==null)||n!=null&&(npcs.revision()!=n.revision()||npcs.elapsedMillis()!=n.elapsedMillis()))throw bad();
            validate(id,rev,phase,clock,manifest,actors,lane>=0,n==null?null:n.actors(),n==null?0:n.spawned());
        }
        State select(List<ModernBattleObservation.Actor> actors,List<ModernBattleAudience.Npc> monsters) {
            var pc=new HashSet<Long>();for(var a:actors)pc.add(a.pose().entityId());
            var npc=new HashSet<Long>();if(monsters!=null)for(var a:monsters)npc.add(a.serial());
            var selected=new ArrayList<Entry>();
            for(var e:targets) {
                boolean visible=e.target() instanceof Player p?pc.contains(p.participant().entityId()):npc.contains(((Npc)e.target()).id().serial());
                for(var g:e.grants())if(visible!=pc.contains(g.caster().entityId()))throw bad();
                if(visible)selected.add(e);
            }
            return new State(world,revision,selected,npcs==null?null:new Npcs(npcs.scope(),npcs.revision(),npcs.elapsedMillis(),
                    npcs.actors().stream().filter(a->npc.contains(a.actor().serial())).toList()));
        }
        private void validate(ModernTargetHistory.World id,long rev,ModernBattleEngine.Phase phase,ModernBattleObservation.Clock clock,
                      ModernWorldMap.Manifest manifest,List<ModernBattleObservation.Actor> actors,boolean isolated,List<ModernBattleAudience.Npc> monsters,long spawned) {
            if(!world.equals(id)||revision!=rev||(npcs==null)!=(monsters==null))throw bad();
            var pc=new HashMap<Long,ModernBattleObservation.Actor>();for(var a:actors)if(pc.put(a.pose().entityId(),a)!=null)throw bad();
            var npc=new HashMap<Long,ModernBattleAudience.Npc>();if(monsters!=null)for(var a:monsters)if(npc.put(a.serial(),a)!=null)throw bad();
            var entries=new HashMap<Target,Entry>();for(var e:targets)entries.put(e.target(),e);
            var used=new HashSet<Target>();var latestTargets=new HashMap<Target,Long>();var events=new HashMap<Long,Grant>();
            for(var a:actors) {
                for(var lane:List.of(ModernUseHistory.Lane.HASTE,ModernUseHistory.Lane.SLOW)) {
                    var r=a.spells().receipts().get(lane);
                    if(r!=null) {
                        var g=new Grant(participant(a),r);validateGrant(g,phase,clock,manifest,pc,npc,spawned,isolated);
                        if(g.effect().changesSpeed())latestTargets.merge(target(g),g.revision(),Math::max);
                        event(events,g);
                    }
                }
                var b=a.buffs();if(b==null||b.slowUntilNanos()==0&&b.hasteOrigin()!=ModernTimedEffects.HasteOrigin.HASTE)continue;
                var key=new Player(participant(a));var e=entries.get(key);
                if(phase!=ModernBattleEngine.Phase.ACTIVE||a.vital().values().hp()==0||e==null||e.active()==null||e.walk()!=null)throw bad();
                var g=e.active();var p=(PlayerProof)g.effect().target();
                boolean slow=b.slowUntilNanos()!=0;
                if(g.lane()!=(slow?ModernUseHistory.Lane.SLOW:ModernUseHistory.Lane.HASTE)||!g.effect().changesSpeed()
                        ||p.life()!=Math.incrementExact(a.resources().life())||p.equipmentRevision()>a.resources().equipmentRevision()
                        ||(slow?(p.after().slowUntilNanos()!=b.slowUntilNanos()||p.after().slowRevision()!=b.slowRevision())
                            :p.after().hasteUntilNanos()!=b.hasteUntilNanos())
                        ||b.revision()<p.after().revision()||b.revision()>revision
                        ||b.observedNanos()<g.effect().activeElapsedNanos()||b.observedNanos()>clock.activeElapsedNanos()
                        ||(slow?b.slowUntilNanos():b.hasteUntilNanos())<=clock.activeElapsedNanos())throw bad();
                used.add(key);
            }
            if(npcs!=null) {
                // Aborting can close the roster before the last world clock
                // tick is applied. A historical roster is valid; a future one
                // must not make an expired lease look live in an audience.
                if(npcs.actors().size()!=npc.size()||npcs.revision()>revision
                        ||npcs.elapsedMillis()>Math.max(0,clock.activeElapsedNanos())/1_000_000L)throw bad();
                for(var m:npcs.actors()) {
                    var a=npc.get(m.actor().serial());if(a==null||a.life()!=m.life()||m.speed().revision()>a.changedRevision())throw bad();
                    if(m.walk()!=null)m.walk().validate(m.actor(),ModernNpcSpeed.pace(m.base(),m.walkSpeed()),a.life(),a.bornRevision(),a.changedRevision(),a.cell());
                    var key=new Npc(m.actor());var e=entries.get(key);
                    if(m.speed().hasEffects()) {
                        if(phase!=ModernBattleEngine.Phase.ACTIVE||a.hp()==0||e==null||e.active()==null||!matches(e.active(),a.life(),a.lane(),m.speed()))throw bad();
                    } else if(e!=null&&e.active()!=null)throw bad();
                    if(m.walkSpeed().hasEffects()) {
                        if(e==null||e.walk()==null||m.walk()==null||!matches(e.walk(),m.walk().life(),a.lane(),m.walkSpeed()))throw bad();
                    } else if(e!=null&&e.walk()!=null)throw bad();
                    if(e!=null)used.add(key);
                }
            }
            if(used.size()!=targets.size())throw bad();
            for(var e:targets) {
                if(e.active()!=null&&latestTargets.getOrDefault(e.target(),0L)>e.active().revision())throw bad();
                for(var g:e.grants()) {
                    validateGrant(g,phase,clock,manifest,pc,npc,spawned,isolated);
                    var latest=pc.get(g.caster().entityId()).spells().receipts().get(g.lane());
                    follows(g.receipt(),latest);event(events,g);
                }
            }
            var prior=new HashMap<ModernTargetHistory.Participant,Grant>();
            var byLane=new HashMap<ModernTargetHistory.Participant,Map<ModernUseHistory.Lane,Grant>>();
            for(var g:events.values().stream().sorted(Comparator.comparingLong(Grant::revision)).toList()) {
                var before=prior.put(g.caster(),g);if(before!=null)eventFollows(before.receipt(),g.receipt());
                var lanes=byLane.computeIfAbsent(g.caster(),unused->new HashMap<>());
                var earlier=lanes.put(g.lane(),g);if(earlier!=null)follows(earlier.receipt(),g.receipt());
            }
        }
        private void validateGrant(Grant g,ModernBattleEngine.Phase phase,ModernBattleObservation.Clock clock,ModernWorldMap.Manifest manifest,
                Map<Long,ModernBattleObservation.Actor> actors,Map<Long,ModernBattleAudience.Npc> monsters,long spawned,boolean isolated) {
            var a=actors.get(g.caster().entityId());var r=g.receipt();var u=r.impact().use();var d=g.effect();
            g.validate(world);
            if(a==null||!g.caster().equals(participant(a))||a.resources()==null||!a.spells().histories().containsKey(g.lane())
                    ||u.revision()>revision||u.before().life()>Math.incrementExact(a.resources().life())||u.before().equipmentRevision()>a.resources().equipmentRevision()
                    ||r.loadingElapsedNanos()>clock.loadingElapsedNanos()||d.activeElapsedNanos()>clock.activeElapsedNanos()
                    ||r.loadingElapsedNanos()-d.activeElapsedNanos()!=clock.loadingElapsedNanos()-clock.activeElapsedNanos()
                    ||!ModernWorldProtocol.contains(manifest,new ModernWorldMap.Cell(u.before().x(),u.before().y()))
                    ||!ModernWorldProtocol.contains(manifest,d.target().cell()))throw bad();
            int lane;
            if(d.target() instanceof PlayerProof p) {
                var t=actors.get(p.participant().entityId());
                if(t==null||!p.participant().equals(participant(t))||t.resources()==null
                        ||p.life()>Math.incrementExact(t.resources().life())||p.equipmentRevision()>t.resources().equipmentRevision())throw bad();
                if(u.revision()==revision&&(phase!=ModernBattleEngine.Phase.ACTIVE||t.resources().vitals().hp()==0
                        ||p.life()!=Math.incrementExact(t.resources().life())||p.equipmentRevision()!=t.resources().equipmentRevision()
                        ||!p.cell().equals(t.pose().position())||!p.after().equals(t.buffs())))throw bad();
                lane=t.pose().member().team();
            } else {
                var n=(NpcProof)d.target();var t=monsters.get(n.id().serial());
                if(npcs==null||!world.contains(n.id().scope())||n.id().serial()>spawned||n.rosterRevision()>npcs.revision()
                        ||n.after().revision()>npcs.revision()
                        ||t!=null&&(n.life()>t.life()||n.lane()!=t.lane()||n.missingEffect()!=null&&n.missingEffect().npcTemplateId()!=t.npcId()))throw bad();
                if(u.revision()==revision) {
                    var motion=npcs.actors().stream().filter(m->m.actor().equals(n.id())).findFirst().orElseThrow(ModernSpeedEffects::bad);
                    if(phase!=ModernBattleEngine.Phase.ACTIVE||t==null||t.hp()==0||n.life()!=t.life()
                            ||!n.cell().equals(t.cell())||!n.after().equals(motion.speed()))throw bad();
                }
                lane=n.lane();
            }
            if(isolated&&lane!=a.pose().member().team())throw bad();
        }
        @Override public String toString(){return "SpeedEffects[redacted]";}
    }
    private static boolean matches(Grant g,long life,int lane,ModernNpcSpeed.State speed) {
        return g.effect().changesSpeed()&&g.effect().target() instanceof NpcProof n&&n.life()==life&&n.lane()==lane&&n.after().equals(speed)
                &&g.lane()==(speed.slowed()?ModernUseHistory.Lane.SLOW:ModernUseHistory.Lane.HASTE);
    }
    private static void event(Map<Long,Grant> events,Grant g){var old=events.putIfAbsent(g.revision(),g);if(old!=null&&!old.equals(g))throw bad();}
    private static void follows(ModernBattleObservation.SpellReceipt before,ModernBattleObservation.SpellReceipt after) {
        if(after==null||before.impact().lane()!=after.impact().lane())throw bad();long count=after.casts()-before.casts(),spent=after.spent()-before.spent();
        var b=before.impact().use();var a=after.impact().use();
        if(count<0||spent<0||count==0&&!before.equals(after)||count>0&&(b.revision()>=a.revision()||b.sequence()>=a.sequence()
                ||before.loadingElapsedNanos()>after.loadingElapsedNanos()||b.before().life()>a.before().life()
                ||b.before().equipmentRevision()>a.before().equipmentRevision()
                ||count>a.sequence()-b.sequence()||count>a.revision()-b.revision()
                ||spent<a.cost()||spent-a.cost()<Math.multiplyExact(count-1,15)
                ||spent-a.cost()>Math.multiplyExact(count-1,maximum(before.impact().lane()))))throw bad();
    }
    private static long maximum(ModernUseHistory.Lane lane){return lane==ModernUseHistory.Lane.SLOW?ModernSlow.BASE_MP:ModernHaste.BASE_MP;}
    private static void eventFollows(ModernBattleObservation.SpellReceipt before,ModernBattleObservation.SpellReceipt after) {
        var b=before.impact().use();var a=after.impact().use();
        if(b.revision()>=a.revision()||b.sequence()>=a.sequence()||before.loadingElapsedNanos()>after.loadingElapsedNanos()
                ||b.before().life()>a.before().life()||b.before().equipmentRevision()>a.before().equipmentRevision())throw bad();
    }
    private static Effect effect(ModernSpellRuntime.Detail detail) {
        if(detail instanceof ModernHasteReceipt d) {
            var t=d.target();
            Proof p=t instanceof ModernHasteReceipt.Player x?new PlayerProof(x.participant(),x.life(),x.equipmentRevision(),x.cell(),x.before(),x.after())
                    :new NpcProof(((ModernHasteReceipt.Npc)t).id(),t.life(),((ModernHasteReceipt.Npc)t).lane(),t.cell(),((ModernHasteReceipt.Npc)t).rosterRevision(),
                        ((ModernHasteReceipt.Npc)t).before(),((ModernHasteReceipt.Npc)t).after(),null);
            return new Effect(d.activeElapsedNanos(),p,true);
        }
        if(detail instanceof ModernSlowReceipt d) {
            var t=d.target();
            Proof p=t instanceof ModernSlowReceipt.Player x?new PlayerProof(x.participant(),x.life(),x.equipmentRevision(),x.cell(),x.before(),x.after())
                    :new NpcProof(((ModernSlowReceipt.Npc)t).id(),t.life(),((ModernSlowReceipt.Npc)t).lane(),t.cell(),((ModernSlowReceipt.Npc)t).rosterRevision(),
                        ((ModernSlowReceipt.Npc)t).before(),((ModernSlowReceipt.Npc)t).after(),d.missingEffect());
            return new Effect(d.activeElapsedNanos(),p,d.outcome()==ModernSlowReceipt.Outcome.APPLIED||d.outcome()==ModernSlowReceipt.Outcome.NEUTRALIZED);
        }
        throw bad();
    }
    private static void merge(Map<Target,Entry> entries,ModernSpellRuntime.Receipt active,ModernSpellRuntime.Receipt walk,long origin) {
        var a=active==null?null:grant(active,origin);var w=walk==null?null:grant(walk,origin);
        var key=target(a==null?w:a);var old=entries.get(key);
        if(old!=null) {
            if(a!=null&&old.active()!=null||w!=null&&old.walk()!=null)throw bad();
            if(a==null)a=old.active();if(w==null)w=old.walk();
        }
        entries.put(key,new Entry(key,a,w));
    }
    static Grant grant(ModernSpellRuntime.Receipt r,long origin) {
        return new Grant(r.batch().caster(),new ModernBattleObservation.SpellReceipt(ModernBattleObservation.Impact.from(r.batch()),r.casts(),r.spent(),r.atNanos()-origin,r.detail()));
    }
    static Target target(Grant g){var t=g.effect().target();return t instanceof PlayerProof p?new Player(p.participant()):new Npc(((NpcProof)t).id());}
    private static ModernTargetHistory.Participant participant(ModernBattleObservation.Actor a){return new ModernTargetHistory.Participant(a.pose().entityId(),a.pose().member().identity());}
    private static int compare(Target a,Target b) {
        if(a instanceof Player p)return b instanceof Player q?Long.compare(p.participant().entityId(),q.participant().entityId()):-1;
        return b instanceof Player?1:Long.compare(((Npc)a).id().serial(),((Npc)b).id().serial());
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid public speed effects");}
    private ModernSpeedEffects(){}
}
