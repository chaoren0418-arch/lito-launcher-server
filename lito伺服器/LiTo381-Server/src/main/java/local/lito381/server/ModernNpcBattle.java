package local.lito381.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Server-internal ordinary NPC combat contract, owned by ModernBattleEngine.
 * Not a wire version, Quest stage controller, map admission or client feature.
 * In particular an NPC serial must NEVER be serialized as a participant entity.
 */
final class ModernNpcBattle {
    record Config(ModernWorldBinding.Key key, ModernNpcCatalog.Catalog catalog,
                  Map<ModernParticipantId, ModernNpcMelee.BlessingBinding> players,
                  List<ModernWorldMap.Cell> laneAnchors, int capacity, long corpseMillis,ModernQuestProgress.Plan questPlan,boolean lightArrow,ModernTimeAttack.Plan timeAttack,boolean disintegrate,boolean fireStorm) {
        Config(ModernWorldBinding.Key key,ModernNpcCatalog.Catalog catalog,Map<ModernParticipantId,ModernNpcMelee.BlessingBinding> players,
                List<ModernWorldMap.Cell> laneAnchors,int capacity,long corpseMillis,ModernQuestProgress.Plan questPlan,boolean lightArrow,ModernTimeAttack.Plan timeAttack,boolean disintegrate) {
            this(key,catalog,players,laneAnchors,capacity,corpseMillis,questPlan,lightArrow,timeAttack,disintegrate,false);
        }
        Config withFireStorm(boolean enabled){return new Config(key,catalog,players,laneAnchors,capacity,corpseMillis,questPlan,lightArrow,timeAttack,disintegrate,enabled);}
        Config(ModernWorldBinding.Key key,ModernNpcCatalog.Catalog catalog,Map<ModernParticipantId,ModernNpcMelee.BlessingBinding> players,
                List<ModernWorldMap.Cell> laneAnchors,int capacity,long corpseMillis,ModernQuestProgress.Plan questPlan,boolean lightArrow,ModernTimeAttack.Plan timeAttack) {
            this(key,catalog,players,laneAnchors,capacity,corpseMillis,questPlan,lightArrow,timeAttack,false);
        }
        Config withDisintegrate(boolean enabled){return new Config(key,catalog,players,laneAnchors,capacity,corpseMillis,questPlan,lightArrow,timeAttack,enabled,fireStorm);}
        Config(ModernWorldBinding.Key key,ModernNpcCatalog.Catalog catalog,Map<ModernParticipantId,ModernNpcMelee.BlessingBinding> players,
                List<ModernWorldMap.Cell> laneAnchors,int capacity,long corpseMillis,ModernQuestProgress.Plan questPlan,boolean lightArrow) {
            this(key,catalog,players,laneAnchors,capacity,corpseMillis,questPlan,lightArrow,null);
        }
        Config(ModernWorldBinding.Key key,ModernNpcCatalog.Catalog catalog,Map<ModernParticipantId,ModernNpcMelee.BlessingBinding> players,
                List<ModernWorldMap.Cell> laneAnchors,int capacity,long corpseMillis,ModernQuestProgress.Plan questPlan) {
            this(key,catalog,players,laneAnchors,capacity,corpseMillis,questPlan,false);
        }
        Config(ModernWorldBinding.Key key,ModernNpcCatalog.Catalog catalog,Map<ModernParticipantId,ModernNpcMelee.BlessingBinding> players,
                List<ModernWorldMap.Cell> laneAnchors,int capacity,long corpseMillis) {
            this(key,catalog,players,laneAnchors,capacity,corpseMillis,null);
        }
        Config {
            Objects.requireNonNull(key); Objects.requireNonNull(catalog);
            players = Map.copyOf(players); laneAnchors = List.copyOf(laneAnchors);
            if ((timeAttack==null?key.mode()!=2:key.mode()!=4||!timeAttack.key().equals(key)||questPlan!=null)
                    || players.isEmpty() || players.size() > (timeAttack==null?100:60)
                    || laneAnchors.size() != (timeAttack==null?2:1) || timeAttack==null&&questPlan==null&&laneAnchors.get(0).equals(laneAnchors.get(1))
                    || capacity < 1 || capacity > ModernNpcRoster.MAX_RESIDENT || corpseMillis < 1 || corpseMillis > 60_000) throw bad();
            if(questPlan!=null&&!questPlan.key().equals(key))throw bad();
            var sources = new HashMap<Integer, ModernNpcMelee.BlessingBinding>();
            for (var entry : players.entrySet()) {
                var id = entry.getKey(); var binding = entry.getValue();
                if (!id.isHuman() || id.humanKey().sourceId() != binding.sourceId()) throw bad();
                var previous = sources.putIfAbsent(binding.sourceId(), binding);
                if (previous != null && !previous.equals(binding)) throw bad();
            }
            // Freeze the complete selected catalog for this generation. No SQL
            // refresh or late replacement of AC/growth behind an existing actor.
            for (var npc : catalog.npcs().values()) {
                if (!npc.pendingCleanMelee().isEmpty() || npc.get(ModernNpcCatalog.Field.BOSS) != 0) throw bad();
                npc.rosterTemplate();
            }
        }
        @Override public String toString() { return "ModernNpcBattleConfig[redacted]"; }
    }
    enum Direction { PLAYER_TO_NPC, NPC_TO_PARTICIPANT }
    enum Reason { ATTACKED, MOVED, WAIT, NO_TARGET, BUDGET, NOT_ACTIVE, DEAD, TARGET_DEAD,
                  WRONG_LANE, OUT_OF_RANGE, BLOCKED, TOO_FAST, TOO_HEAVY, UNSUPPORTED, NUMERIC, STALE_STATE }
    /** Latest committed exchange per attacker, not an unbounded combat history. */
    record Exchange(Direction direction, ModernParticipantId participant, long participantLife,
                    ModernNpcRoster.Id npc, long npcLife, long requestSequence, long revision,
                    ModernNpcMelee.Outcome outcome) {
        Exchange {
            Objects.requireNonNull(direction); Objects.requireNonNull(participant); Objects.requireNonNull(npc); Objects.requireNonNull(outcome);
            if (participantLife < 1 || npcLife < 1 || revision < 1
                    || (direction == Direction.PLAYER_TO_NPC ? !participant.isHuman() || requestSequence < 1 : requestSequence != 0)) throw bad();
        }
    }
    /** Both snapshots and typed receipts are captured under the one engine lock. */
    record Frame(ModernBattleEngine.Snapshot world, ModernNpcRoster.Snapshot npcs, List<Exchange> exchanges) {
        Frame {
            Objects.requireNonNull(world); Objects.requireNonNull(npcs); exchanges = List.copyOf(exchanges);
            var scope = npcs.scope();
            if (world.room().summary().config().mode() != (world.timeAttack().enabled()?4:2)
                    || scope.roomId() != world.room().summary().id() || scope.matchId() != world.matchId()
                    || scope.generation() != world.generation() || !npcs.manifest().equals(world.manifest())
                    || npcs.revision() > world.revision() || npcs.closed() != (world.phase() == ModernBattleEngine.Phase.ABORTED)
                    || world.isolatedForces()!=npcs.isolatedForces()||world.result() != null&&!world.quest().enabled() || world.honor().enabled() || world.race().enabled()
                    || exchanges.size() > 100 + ModernNpcRoster.MAX_RESIDENT) throw bad();
            world.timeAttack().validate(world);
            ModernNpcDisintegrateState.validateNpcs(world,npcs);
            if(world.timeAttack().enabled()) {
                var state=world.timeAttack().state();
                var living=npcs.actors().stream().filter(ModernNpcRoster.Actor::alive).map(ModernNpcRoster.Actor::id).collect(java.util.stream.Collectors.toSet());
                if(npcs.actors().stream().anyMatch(n->n.lane()!=0||n.life()!=1)
                        ||!npcs.closed()&&!living.equals(state.living())
                        ||state.towerHp()>0&&(world.actors().stream().anyMatch(a->!state.departed().contains(a.entityId())&&a.position().equals(state.plan().tower().cell()))
                            ||npcs.actors().stream().anyMatch(n->n.alive()&&n.cell().equals(state.plan().tower().cell()))))throw bad();
            }
            var playerEvents = new HashSet<ModernParticipantId>(); var npcEvents = new HashSet<ModernNpcRoster.Id>();
            long previous = 0;
            for (var event : exchanges) {
                if (!event.npc().scope().equals(scope) || event.npc().serial() > npcs.spawned()
                        || event.revision() <= previous || event.revision() > world.revision()) throw bad();
                var actor = world.actors().stream().filter(a -> a.member().identity().equals(event.participant())).findFirst().orElseThrow(ModernNpcBattle::bad);
                var life=world.quest().lives().stream().filter(l->l.entityId()==actor.entityId()).findFirst().orElse(null);
                if(life==null?event.participantLife()!=1:event.participantLife()>life.incarnation()
                        ||(event.participantLife()==life.incarnation()?event.revision()<=life.respawnRevision():event.revision()>life.respawnRevision()))throw bad();
                if (event.direction() == Direction.PLAYER_TO_NPC) {
                    if (!playerEvents.add(event.participant()) || event.requestSequence() > world.vitals().get((int) actor.entityId() - 1).attackSequence()) throw bad();
                } else if (!npcEvents.add(event.npc()) || npcs.actors().stream().noneMatch(a -> a.id().equals(event.npc()))) throw bad();
                previous = event.revision();
            }
            ModernNpcFireStormState.validateNpcs(world,npcs);
            var occupied = new HashSet<ModernQuestSpace.Cell>();
            for(var cast:world.npcSpells())if(cast.castCount()>0) {
                if(!cast.target().scope().equals(scope)||cast.target().serial()>npcs.spawned())throw bad();
                var target=npcs.actors().stream().filter(a->a.id().equals(cast.target())).findFirst().orElse(null);
                if(target!=null&&(target.lane()!=cast.targetLane()||target.life()<cast.targetLife()))throw bad();
                if(cast.revision()==world.revision()&&(target==null||target.life()!=cast.targetLife()
                        ||target.hp()!=cast.outcome().remainingHp()||world.vitals().get((int)cast.entityId()-1).hp()==0))throw bad();
            }
            for (var actor : world.actors())if(!world.timeAttack().enabled()||!world.timeAttack().state().departed().contains(actor.entityId()))occupied.add(ModernQuestSpace.cell(world.isolatedForces(),actor.member().team(),actor.position()));
            for (var npc : npcs.actors()) if (npc.alive() && occupied.contains(ModernQuestSpace.cell(world.isolatedForces(),npc.lane(),npc.cell()))) throw bad();
            for(var objective:world.quest().actors())if(objective.alive()&&npcs.actors().stream().anyMatch(n->n.alive()&&n.lane()==objective.id().lane()&&n.cell().equals(objective.cell())))throw bad();
        }
    }
    record Result(Reason reason, ModernBattleObservation.Capture observation) implements ModernBattleCommand {
        Result { Objects.requireNonNull(reason); Objects.requireNonNull(observation); }
        Frame frame(){return ModernLegacyBattleProjection.npcs(observation);}
    }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid modern NPC battle contract"); }
    private ModernNpcBattle() {}
}
