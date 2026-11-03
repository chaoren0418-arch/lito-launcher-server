package local.lito381.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Outbound-safe projection of ONE validated service publication. Engine
 * revisions are never shifted. No database, full encounter plan, raw monotonic
 * origin, loadout/evaluator or another owner's private state is reachable.
 * Trusted projection only: this is not a wire decoder or permission for SET. */
final class ModernBattleAudience {
    record Npc(long serial,int sourceId,int npcId,int graphic,int lane,int stage,int heading,
            ModernNpcRoster.Action action,ModernWorldMap.Cell cell,long hp,long maximumHp,long mp,long maximumMp,
            long life,long bornRevision,long changedRevision,long corpseUntilMillis,long nextMoveMillis,long nextAttackMillis) {
        static Npc from(ModernNpcRoster.Actor a) {
            ModernNpcSpeed.requireFrozen(a);
            return fromStatus(a);
        }
        /** Only paired with the separately validated skill43 status/motion extension. */
        static Npc fromStatus(ModernNpcRoster.Actor a) {
            var t=a.template();return new Npc(a.id().serial(),t.catalogSourceId(),t.npcId(),t.graphic(),a.lane(),a.stage(),a.heading(),
                    a.action(),a.cell(),a.hp(),t.maximumHp(),a.mp(),t.maximumMp(),a.life(),a.bornRevision(),a.changedRevision(),
                    a.corpseUntilMillis(),a.nextMoveMillis(),a.nextAttackMillis());
        }
    }
    record Npcs(long revision,long elapsedMillis,long spawned,boolean closed,List<Npc> actors,
            List<ModernNpcBattle.Exchange> exchanges,ModernNpcProtocol.Resources resources,ModernNpcNames.Table names) {
        Npcs {actors=List.copyOf(actors);exchanges=List.copyOf(exchanges);Objects.requireNonNull(resources);Objects.requireNonNull(names);}
    }
    record Lane(int lane,int stage,ModernQuestProgress.Phase phase,List<Long> living) {
        Lane {living=List.copyOf(living);}
    }
    record Quest(long revision,ModernQuestProgress.Flow flow,ModernQuestProgress.Status status,List<Lane> lanes,
            List<ModernQuestObjectives.Target> targets,List<ModernQuestObjectives.Actor> objectives,
            List<ModernQuestObjectives.Exchange> exchanges,List<ModernQuestLife.State> lives,int winningLane,long deathRevision) {
        Quest {lanes=List.copyOf(lanes);targets=List.copyOf(targets);objectives=List.copyOf(objectives);exchanges=List.copyOf(exchanges);lives=List.copyOf(lives);}
    }
    record TimeAttack(long revision,ModernTimeAttack.Phase phase,ModernTimeAttack.End end,
            List<ModernTimeAttack.Kind> waves,int wave,long leader,int durationMillis,long startedMillis,long observedMillis,
            List<Long> departed,List<Long> living,ModernTimeAttack.Tower tower,long towerHp,long towerHurtRevision,long towerDeathRevision,
            String planSha256,List<ModernTimeAttack.Exchange> exchanges) {
        TimeAttack {waves=List.copyOf(waves);departed=List.copyOf(departed);living=List.copyOf(living);exchanges=List.copyOf(exchanges);}
    }
    record Race(long revision,ModernPetRace.Phase phase,String courseSha256,List<ModernPetRace.Gate> gates,
            long activeLimitNanos,long observedElapsedNanos,long firstFinisherEntity,long finishWindowDeadlineElapsedNanos,
            long terminalRevision,UUID resultId,List<ModernPetRace.Runner> runners) {
        Race {gates=List.copyOf(gates);runners=List.copyOf(runners);}
    }
    record Modes(ModernBattleObservation.Honor honor,Quest quest,TimeAttack timeAttack,ModernSiege.Snapshot siege,Race race) {}
    record World(ModernTargetHistory.World identity,long revision,ModernBattleEngine.Phase phase,ModernBattleEngine.AbortReason abortReason,
            ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,ModernMatchDescriptor descriptor,
            ModernBattleObservation.Clock clock,int lane,List<ModernBattleObservation.Actor> actors,Modes modes,Npcs npcs,
            ModernMatchSettlement.Event result,ModernHasteEffects.State hasteEffects,ModernSpeedEffects.State speedEffects) {
        World(ModernTargetHistory.World identity,long revision,ModernBattleEngine.Phase phase,ModernBattleEngine.AbortReason abortReason,
                ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,ModernMatchDescriptor descriptor,
                ModernBattleObservation.Clock clock,int lane,List<ModernBattleObservation.Actor> actors,Modes modes,Npcs npcs,
                ModernMatchSettlement.Event result,ModernHasteEffects.State hasteEffects) {
            this(identity,revision,phase,abortReason,room,manifest,descriptor,clock,lane,actors,modes,npcs,result,hasteEffects,null);
        }
        World(ModernTargetHistory.World identity,long revision,ModernBattleEngine.Phase phase,ModernBattleEngine.AbortReason abortReason,
                ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,ModernMatchDescriptor descriptor,
                ModernBattleObservation.Clock clock,int lane,List<ModernBattleObservation.Actor> actors,Modes modes,Npcs npcs,
                ModernMatchSettlement.Event result) {
            this(identity,revision,phase,abortReason,room,manifest,descriptor,clock,lane,actors,modes,npcs,result,null);
        }
        World {
            actors=List.copyOf(actors);
            if(actors.stream().anyMatch(a->(a.playerHealing()!=null)!=descriptor.has(ModernMatchDescriptor.PLAYER_HEALING)))throw bad();
            for(var actor:actors) {
                if((descriptor.has(ModernMatchDescriptor.ARMOR_PROTECTION)&&actor.pose().member().identity().isHuman())!=(actor.armorProtection()!=null))throw bad();
                if(actor.armorProtection()!=null)actor.armorProtection().validate(revision,phase,clock,actor.pose(),actor.resources(),
                        modes.timeAttack()==null||!modes.timeAttack().departed().contains(actor.pose().entityId()));
            }
            final var visibleActors=actors;
            for(var actor:actors)if(actor.playerHealing()!=null) {
                var healing=actor.playerHealing();
                healing.validate(identity,new ModernTargetHistory.Participant(actor.pose().entityId(),actor.pose().member().identity()),actor.resources(),actor.spells(),clock);
                for(var impact:java.util.stream.Stream.of(healing.incoming().last(),healing.receipt()==null?null:healing.receipt().impact()).filter(Objects::nonNull).toList()) {
                    var target=(ModernTargetHistory.Participant)impact.target().before().target();
                    if(actors.stream().noneMatch(a->a.pose().entityId()==impact.caster().entityId()&&a.pose().member().identity().equals(impact.caster().identity()))
                            ||actors.stream().noneMatch(a->a.pose().entityId()==target.entityId()&&a.pose().member().identity().equals(target.identity())))throw bad();
                }
            }
            if(lane< -1||lane>1||descriptor.has(ModernMatchDescriptor.ISOLATED_FORCES)!=(lane>=0)||actors.isEmpty())throw bad();
            var expected=room.members().stream().filter(m->lane<0||m.team()==lane).toList();
            if(actors.size()!=expected.size())throw bad();
            for(int i=0;i<actors.size();i++) {
                var a=actors.get(i).pose();
                if(!a.member().equals(expected.get(i))||a.entityId()<1||a.entityId()>room.members().size()
                        ||!a.member().equals(room.members().get(Math.toIntExact(a.entityId()-1))))throw bad();
            }
            if(npcs!=null&&(npcs.actors().stream().anyMatch(a->lane>=0&&a.lane()!=lane)
                    ||npcs.exchanges().stream().anyMatch(e->!contains(visibleActors,e.participant()))))throw bad();
            var q=modes.quest();if(q!=null&&(q.lanes().size()!=(lane<0?2:1)
                    ||q.lanes().stream().anyMatch(l->lane>=0&&l.lane()!=lane)
                    ||q.targets().stream().anyMatch(t->lane>=0&&t.lane()!=lane)
                    ||q.objectives().stream().anyMatch(a->lane>=0&&a.id().lane()!=lane)
                    ||q.exchanges().stream().anyMatch(e->!contains(visibleActors,e.participant())||lane>=0&&e.target().lane()!=lane)
                    ||q.lives().stream().anyMatch(l->lane>=0&&l.lane()!=lane||visibleActors.stream().noneMatch(a->a.pose().entityId()==l.entityId()))))throw bad();
            if((descriptor.has(ModernMatchDescriptor.HASTE)&&!descriptor.has(ModernMatchDescriptor.SLOW))!=(hasteEffects!=null)
                    ||descriptor.has(ModernMatchDescriptor.SLOW)!=(speedEffects!=null)
                    ||actors.stream().anyMatch(a->a.spells().histories().containsKey(ModernUseHistory.Lane.HASTE)!=descriptor.has(ModernMatchDescriptor.HASTE)
                        ||a.spells().histories().containsKey(ModernUseHistory.Lane.SLOW)!=descriptor.has(ModernMatchDescriptor.SLOW)))throw bad();
            if(hasteEffects!=null)hasteEffects.validateAudience(identity,revision,phase,clock,manifest,actors,lane,npcs);
            if(speedEffects!=null)speedEffects.validateAudience(identity,revision,phase,clock,manifest,actors,lane,npcs);
        }
        @Override public String toString(){return "BattleAudienceWorld[redacted]";}
    }
    /** Only presentation/source facts; no Fighter, Source/JDBC binding,
     * evaluator, growth catalog, random provider or raw monotonic deadline.
     * Lifetime fields remain explicit source-observation epoch milliseconds. */
    record Equipment(ModernCharacterData.Snapshot source,long revision,ModernCombatProfile.Effective effective,
            List<ModernEquipmentAuthority.Equipped> equipped,List<ModernItemLifetime.View> lifetimes,
            long evaluatedAtEpochMillis,long validUntilEpochMillis) {
        Equipment {equipped=List.copyOf(equipped);lifetimes=List.copyOf(lifetimes);}
    }
    record Personal(ModernRooms.Scope scope,long entityId,Equipment equipment,List<ModernSkills.Skill> skills,
            ModernEquipmentHistory.State history,long spellBirthRevision,ModernBattleObservation.Supplies supplies,
            ModernInventoryWeight.Snapshot weight,ModernAmmo.State ammo,ModernNaturalRegen.Clock regenClock,ModernSpellMaterials.State materials) {
        Personal(ModernRooms.Scope scope,long entityId,Equipment equipment,List<ModernSkills.Skill> skills,
                ModernEquipmentHistory.State history,long spellBirthRevision,ModernBattleObservation.Supplies supplies,
                ModernInventoryWeight.Snapshot weight,ModernAmmo.State ammo,ModernNaturalRegen.Clock regenClock) {
            this(scope,entityId,equipment,skills,history,spellBirthRevision,supplies,weight,ammo,regenClock,null);
        }
        Personal {skills=List.copyOf(skills);}
        @Override public String toString(){return "BattleAudiencePersonal[redacted]";}
    }
    /** Only publication construction can pair a world and personal facts. */
    static final class View {
        private final World world;
        private final Personal personal;
        // Immutable public schema-4 extension, bound while the exact owner
        // publication is still available. Never reconstructed from later ticks.
        private final ModernNpcMotion.State npcMotion;
        private final ModernArmorPresentation.Candidate armorPresentation;
        private View(World world,ModernBattleObservation.OwnerView owner,ModernNpcMotion.State npcMotion,ModernBattleObservation.Capture capture) {
            this.world=world;var o=owner.owner();
            var visible=world.actors().stream().filter(a->a.pose().entityId()==o.entityId()).findFirst().orElseThrow(ModernBattleAudience::bad);
            if(!visible.equals(owner.world().actors().get(Math.toIntExact(o.entityId()-1)))
                    ||!world.identity().equals(owner.world().identity())||world.revision()!=owner.world().revision())throw bad();
            Equipment equipment=null;
            if(o.loadout()!=null) {
                var p=o.loadout().fighter().profile();equipment=new Equipment(p.source(),p.equipmentRevision(),p.effective(),p.equipped(),p.sourceLifetimes(),p.evaluatedAtMillis(),p.validUntilMillis());
            }
            personal=new Personal(o.scope(),o.entityId(),equipment,o.skills()==null?List.of():o.skills().skills(),
                    o.history()==null?null:o.history().history(),o.history()==null?0:o.history().spells().birthRevision(),
                    o.supplies(),o.weight(),o.ammo(),o.regenClock(),o.materials());
            var n=owner.world().npcs();
            this.npcMotion=npcMotion;
            if((n==null||world.hasteEffects()!=null||world.speedEffects()!=null)!=(npcMotion==null)||npcMotion!=null&&(!npcMotion.scope().equals(n.roster().scope())
                    ||npcMotion.revision()!=n.roster().revision()||npcMotion.elapsedMillis()!=n.roster().elapsedMillis()))throw bad();
            if(npcMotion!=null&&!npcMotion.actors().stream().map(v->v.actor().serial()).toList().equals(
                    world.npcs().actors().stream().map(Npc::serial).toList()))throw bad();
            armorPresentation=capture.features().armorProtection()?ModernArmorPresentation.from(capture,this):null;
        }
        World world(){return world;}
        Personal personal(){return personal;}
        ModernNpcMotion.State npcMotion(){return npcMotion;}
        ModernArmorPresentation.Candidate armorPresentation(){return armorPresentation;}
        @Override public String toString(){return "BattleAudienceView[redacted]";}
    }
    static Map<ModernRooms.Scope,View> project(ModernBattleObservation.World source,ModernMatchDescriptor descriptor,
            ModernNpcProtocol.Resources resources,ModernNpcNames.Table names,Map<ModernRooms.Scope,ModernBattleObservation.OwnerView> owners,
            ModernBattleObservation.Capture capture) {
        if(source.vip()||capture.hasVip())throw new IllegalArgumentException("VIP requires an explicit saved/runtime audience contract");
        var worlds=new HashMap<Integer,World>();var views=new HashMap<ModernRooms.Scope,View>();
        var motionByLane=new HashMap<Integer,ModernNpcMotion.State>();
        for(var entry:owners.entrySet()) {
            var owner=entry.getValue();if(!entry.getKey().equals(owner.owner().scope())||owner.world()!=source)throw bad();
            var actor=source.actors().get(Math.toIntExact(owner.owner().entityId()-1));
            int lane=source.modes().isolatedForces()?actor.pose().member().team():-1;
            var world=worlds.computeIfAbsent(lane,l->world(source,descriptor,l,resources,names));
            var motion=source.npcs()==null||source.hasteEffects()!=null||source.speedEffects()!=null?null:motionByLane.computeIfAbsent(lane,l->ModernNpcMotion.state(source.npcs().roster(),l));
            views.put(entry.getKey(),new View(world,owner,motion,capture));
        }
        return Map.copyOf(views);
    }
    private static World world(ModernBattleObservation.World w,ModernMatchDescriptor descriptor,int lane,
            ModernNpcProtocol.Resources resources,ModernNpcNames.Table names) {
        var actors=w.actors().stream().filter(a->lane<0||a.pose().member().team()==lane).toList();
        var m=w.modes();Quest quest=null;TimeAttack time=null;Race race=null;Npcs npcs=null;
        var q=m.quest();if(q.progress()!=null) {
            var p=q.progress();var lanes=new java.util.ArrayList<Lane>();
            for(int n=0;n<2;n++)if(lane<0||lane==n){var l=p.lanes().get(n);lanes.add(new Lane(n,l.stage(),l.phase(),l.living().stream().map(ModernNpcRoster.Id::serial).sorted().toList()));}
            var targets=p.plan().objectives()==null?List.<ModernQuestObjectives.Target>of():p.plan().objectives().targets().stream().filter(t->lane<0||t.lane()==lane).toList();
            quest=new Quest(p.revision(),p.plan().flow(),p.status(),lanes,targets,q.objectives().stream().filter(a->lane<0||a.id().lane()==lane).toList(),
                    q.exchanges().stream().filter(e->contains(actors,e.participant())).toList(),q.lives().stream().filter(l->lane<0||l.lane()==lane).toList(),
                    p.winningLane(),w.result()==null?0:w.result().engineRevision());
        }
        if(m.timeAttack().enabled()) {
            var s=m.timeAttack().state();time=new TimeAttack(s.revision(),s.phase(),s.end(),s.plan().waves().stream().map(ModernTimeAttack.Wave::kind).toList(),
                    s.wave(),s.leader(),s.plan().durationMillis(),s.startedMillis(),s.observedMillis(),s.departed().stream().sorted().toList(),
                    s.living().stream().map(ModernNpcRoster.Id::serial).sorted().toList(),s.plan().tower(),s.towerHp(),s.towerHurtRevision(),s.towerDeathRevision(),
                    ModernTimeAttackSettlement.planHash(s.plan()),m.timeAttack().exchanges());
        }
        if(m.race()!=null) {
            var s=m.race();race=new Race(s.revision(),s.phase(),s.course().digest(),s.course().gates(),s.activeLimitNanos(),s.observedElapsedNanos(),
                    s.firstFinisherEntity(),s.finishWindowDeadlineElapsedNanos(),s.terminalRevision(),m.raceResultId(),s.runners());
        }
        if(w.npcs()!=null) {
            var n=w.npcs().roster();npcs=new Npcs(n.revision(),n.elapsedMillis(),n.spawned(),n.closed(),
                    n.actors().stream().filter(a->lane<0||a.lane()==lane).map(a->w.hasteEffects()==null&&w.speedEffects()==null?Npc.from(a):Npc.fromStatus(a)).toList(),
                    w.npcs().exchanges().stream().filter(e->contains(actors,e.participant())).toList(),resources,names);
        }
        var result=w.result()!=null?w.result():m.timeAttackResult()!=null?m.timeAttackResult():m.siegeResult();
        return new World(w.identity(),w.revision(),w.phase(),w.abortReason(),w.room(),w.manifest(),descriptor,w.clock(),lane,actors,
                new Modes(m.honor(),quest,time,m.siege(),race),npcs,result,
                w.hasteEffects()==null?null:w.hasteEffects().select(actors,npcs==null?null:npcs.actors()),
                w.speedEffects()==null?null:w.speedEffects().select(actors,npcs==null?null:npcs.actors()));
    }
    private static boolean contains(List<ModernBattleObservation.Actor> actors,ModernParticipantId id) {
        return actors.stream().anyMatch(a->a.pose().member().identity().equals(id));
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid battle audience projection");}
    private ModernBattleAudience(){}
}
