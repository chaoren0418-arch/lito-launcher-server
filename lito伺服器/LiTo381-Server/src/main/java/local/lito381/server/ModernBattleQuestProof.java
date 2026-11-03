package local.lito381.server;

import java.util.HashSet;
import java.util.List;

/** Internal, full two-force proof. Quest progress is MODE-local; objective,
 * combat and result revisions are ENGINE-domain. Never equate those clocks,
 * reconstruct fixed-price legacy frames, or require a hidden opposing target
 * from a filtered audience. Map/source qualification remains upstream. */
final class ModernBattleQuestProof {
    static void validate(long revision,ModernBattleEngine.Phase phase,ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,
            List<ModernBattleObservation.Actor> players,boolean isolated,ModernBattleObservation.Quest q,
            ModernBattleObservation.Npcs npcs,ModernMatchSettlement.Event result) {
        var progress=q.progress();
        if(progress==null){if(result!=null&&result.mode()==2)throw bad();return;}
        var plan=progress.plan();var config=plan.objectives();var roomConfig=room.summary().config();
        boolean initial=phase==ModernBattleEngine.Phase.LOADING||phase==ModernBattleEngine.Phase.COUNTDOWN;
        boolean finished=progress.status()==ModernQuestProgress.Status.FINISHED;
        if(!isolated||npcs==null||!plan.key().equals(new ModernWorldBinding.Key(roomConfig.mode(),roomConfig.mapId()))
                ||(phase==ModernBattleEngine.Phase.FINISHED)!=finished
                ||(phase==ModernBattleEngine.Phase.ABORTED)!=(progress.status()==ModernQuestProgress.Status.ABORTED)
                ||finished!=(result!=null)||q.objectives().size()>2||q.exchanges().size()>players.size()
                ||initial&&(progress.revision()!=1||!q.objectives().isEmpty()||!q.exchanges().isEmpty()
                    ||progress.lanes().stream().anyMatch(l->l.stage()!=1||l.phase()!=ModernQuestProgress.Phase.SPAWN_PENDING))
                ||config==null&&(!q.objectives().isEmpty()||!q.exchanges().isEmpty()||finished))throw bad();
        var roster=npcs.roster();
        for(int lane=0;lane<2;lane++) {
            var l=progress.lanes().get(lane);var living=new HashSet<ModernNpcRoster.Id>();
            for(var n:roster.actors())if(n.lane()==lane&&n.alive()) {
                if(n.stage()!=l.stage())throw bad();living.add(n.id());
            }
            if(!roster.closed()&&!living.equals(l.living())||l.living().stream().anyMatch(id->id.serial()>roster.spawned()))throw bad();
        }
        if(config!=null)for(var target:config.targets())if(!ModernWorldProtocol.contains(manifest,target.cell()))throw bad();
        var lanes=new HashSet<Integer>();int previousSlot=0;
        for(var a:q.objectives()) {
            var id=a.id();var lane=progress.lanes().get(id.lane());
            if(!id.scope().equals(progress.scope())||id.slot()<=previousSlot||!lanes.add(id.lane())
                    ||!a.target().equals(config.target(id.lane(),id.stage()))||a.revision()>revision||id.stage()>lane.stage()
                    ||a.kind()!=kind(plan,id.stage())
                    ||a.alive()&&(id.stage()!=lane.stage()||lane.phase()!=ModernQuestProgress.Phase.OBJECTIVE
                        ||players.stream().anyMatch(p->p.pose().member().team()==id.lane()&&p.pose().position().equals(a.cell()))
                        ||roster.actors().stream().anyMatch(n->n.alive()&&n.lane()==id.lane()&&n.cell().equals(a.cell())))
                    ||!a.alive()&&(id.stage()==lane.stage()?(!finished||progress.winningLane()!=id.lane()||id.stage()!=4)
                        :id.stage()!=lane.stage()-1||a.kind()!=ModernQuestObjectives.Kind.ROOM_SYMBOL))throw bad();
            previousSlot=id.slot();
        }
        var humans=new HashSet<ModernParticipantId>();var eventRevisions=new HashSet<Long>();long previous=0;
        var lethal=new HashSet<ModernQuestObjectives.Id>();
        for(var e:q.exchanges()) {
            var p=players.stream().filter(a->a.pose().member().identity().equals(e.participant())).findFirst().orElseThrow(ModernBattleQuestProof::bad);
            var target=config.target(e.target().lane(),e.target().stage());var current=objective(q,e.target());var hit=e.outcome();
            if(!e.target().scope().equals(progress.scope())||!humans.add(e.participant())||!eventRevisions.add(e.revision())
                    ||e.target().lane()!=p.pose().member().team()||e.target().stage()>progress.lanes().get(e.target().lane()).stage()
                    ||e.revision()<=previous||e.revision()>revision||p.vital()==null||e.requestSequence()>p.vital().attackSequence()
                    ||hit.remainingHp()>target.maximumHp()||hit.appliedDamage()>target.maximumHp()-hit.remainingHp()
                    ||hit.appliedDamage()==0&&hit.remainingHp()==0)throw bad();
            if(current!=null)impact(current,e.revision(),0,hit.remainingHp(),hit.appliedDamage(),hit.death());
            if(e.revision()==revision&&(current==null||p.vital().values().hp()==0||e.requestSequence()!=p.vital().attackSequence()))throw bad();
            if(e.revision()==revision&&hit.death())lethal.add(e.target());previous=e.revision();
        }
        for(var p:players) {
            var s=p.spells().structure();if(s==null)continue;
            var history=p.spells().histories().get(ModernUseHistory.Lane.NPC_FIRE_STORM);var use=s.impact().use();
            var origin=new ModernWorldMap.Cell(use.before().x(),use.before().y());
            if(config==null||!p.pose().member().identity().isHuman()||history==null||s.casts()>history.current().casts()
                    ||use.revision()>revision||use.sequence()>p.spells().sequence()||!eventRevisions.add(use.revision())
                    ||!ModernWorldProtocol.contains(manifest,origin)||initial)throw bad();
            long incarnation=Math.incrementExact(p.resources().life());
            if(use.before().life()>incarnation||use.before().life()==incarnation&&p.vital().values().hp()==0&&use.revision()>=p.vital().deathRevision())throw bad();
            if(!q.lives().isEmpty()) {
                var life=q.lives().get((int)p.pose().entityId()-1);
                if(use.before().life()==incarnation?use.revision()<=life.respawnRevision():use.revision()>life.respawnRevision())throw bad();
            }
            if(use.revision()==revision&&(p.vital().values().hp()==0||!origin.equals(p.pose().position())||use.sequence()!=p.spells().sequence()))throw bad();
            int count=0;
            for(var effect:s.impact().targets())if(effect.before().target() instanceof ModernTargetHistory.Quest hit) {
                count++;var id=hit.id();var h=effect.before();
                if(!id.scope().equals(progress.scope())||id.lane()!=p.pose().member().team()||id.stage()>progress.lanes().get(id.lane()).stage())throw bad();
                var target=config.target(id.lane(),id.stage());var current=q.objectives().stream().filter(a->a.id().lane()==id.lane()).findFirst().orElseThrow(ModernBattleQuestProof::bad);
                long distance=Math.max(Math.abs((long)origin.x()-h.cell().x()),Math.abs((long)origin.y()-h.cell().y()));
                if(s.questKind()!=kind(plan,id.stage())||!h.cell().equals(target.cell())||h.maximumHp()!=target.maximumHp()
                        ||effect.kind()!=ModernTargetHistory.Kind.DAMAGE||hit.bornRevision()>=use.revision()
                        ||distance<1||distance>ModernFireStorm.RADIUS||current.id().stage()<id.stage())throw bad();
                if(current.id().equals(id))impact(current,use.revision(),hit.bornRevision(),effect.hpAfter(),effect.applied(),effect.death());
                if(use.revision()==revision&&effect.death())lethal.add(id);
            }
            if(count!=1)throw bad();
        }
        if(finished) {
            var winner=q.objectives().stream().filter(a->a.id().lane()==progress.winningLane()).findFirst().orElseThrow(ModernBattleQuestProof::bad);
            if(result.mode()!=2||winner.alive()||winner.id().stage()!=4||winner.deathRevision()!=revision||!lethal.contains(winner.id()))throw bad();
            for(var p:result.participants())if((p.verdict()==ModernMatchSettlement.Verdict.WIN)!=(p.team()==progress.winningLane()))throw bad();
        }
    }
    private static ModernQuestObjectives.Actor objective(ModernBattleObservation.Quest q,ModernQuestObjectives.Id id) {
        return q.objectives().stream().filter(a->a.id().equals(id)).findFirst().orElse(null);
    }
    private static void impact(ModernQuestObjectives.Actor target,long revision,long birth,long hpAfter,long applied,boolean death) {
        if(revision<=target.bornRevision()||revision>target.revision()||birth!=0&&birth!=target.bornRevision()
                ||hpAfter<target.hp()||death&&target.deathRevision()!=revision
                ||revision==target.revision()&&(hpAfter!=target.hp()||applied>0&&target.hurtRevision()!=revision))throw bad();
    }
    private static ModernQuestObjectives.Kind kind(ModernQuestProgress.Plan plan,int stage) {
        return plan.flow()==ModernQuestProgress.Flow.FINAL_GUARDIAN?ModernQuestObjectives.Kind.FINAL_GUARDIAN:
                stage==4?ModernQuestObjectives.Kind.FINAL_SYMBOL:ModernQuestObjectives.Kind.ROOM_SYMBOL;
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid canonical Quest proof");}
    private ModernBattleQuestProof(){}
}
