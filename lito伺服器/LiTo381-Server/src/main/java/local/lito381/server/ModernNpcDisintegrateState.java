package local.lito381.server;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Skill77 NPC authority ledger; independent of the v27 player-target receipt.
 * ModernNpcDisintegrateProtocol binds its v29 negotiated request/result route. */
final class ModernNpcDisintegrateState {
    enum Reason { NONE, NOT_ACTIVE, DEAD, UNLEARNED, UNSUPPORTED, TOO_FAST, NO_MP,
        TARGET_DEAD, WRONG_LANE, OUT_OF_RANGE, BLOCKED, TOO_HEAVY, NUMERIC,
        STALE_STATE, STALE_REGEN, LAWFUL }
    record Request(ModernRooms.Scope player,ModernNpcRoster.Id target,long sequence,long casterLife,long targetLife) {
        Request {Objects.requireNonNull(player);Objects.requireNonNull(target);
            if(sequence<1||casterLife<1||targetLife<1)throw bad();}
    }
    record Hit(ModernNpcRoster.Id target,long targetLife,int lane,long casterLife,
               ModernWorldMap.Cell from,ModernWorldMap.Cell to,long hpBefore,ModernDisintegrate.Outcome outcome) {
        Hit {
            Objects.requireNonNull(target);Objects.requireNonNull(from);Objects.requireNonNull(to);Objects.requireNonNull(outcome);
            long range=Math.max(Math.abs((long)from.x()-to.x()),Math.abs((long)from.y()-to.y()));
            if(targetLife<1||lane<0||lane>1||casterLife<1||range<1||range>ModernDisintegrate.RANGE
                    ||hpBefore<1||outcome.appliedDamage()!=Math.min(hpBefore,outcome.rawDamage())
                    ||outcome.remainingHp()!=hpBefore-outcome.appliedDamage())throw bad();
        }
    }
    record State(long entityId,long castCount,long revision,long lastSequence,long mpSpent,
                 int mpCost,long initialLawful,long lawful,Hit hit) {
        State {
            if(entityId<1||castCount<0||revision<0||lastSequence<castCount||mpSpent<0
                    ||mpCost!=0&&(mpCost<59||mpCost>70)||mpSpent!=Math.multiplyExact(castCount,mpCost)
                    ||lawful!=Math.addExact(initialLawful,castCount))throw bad();
            if(castCount==0){if(revision!=0||lastSequence!=0||hit!=null)throw bad();}
            else if(revision<1||mpCost==0||hit==null||!ModernDisintegrate.lawfulAllows(initialLawful))throw bad();
            if(mpCost==0&&(castCount!=0||initialLawful!=0))throw bad();
        }
        static State initial(long entityId,int cost,long lawful){return new State(entityId,0,0,0,0,cost,lawful,lawful,null);}
        State cast(long next,long sequence,Hit hit) {
            if(next<=revision||sequence<=lastSequence)throw bad();var spending=ModernSkillSpending.advance(castCount,mpSpent,mpCost);
            return new State(entityId,spending.casts(),next,sequence,spending.spent(),mpCost,
                    initialLawful,ModernDisintegrate.lawfulAfterCast(lawful),hit);
        }
        int reuseMillis(){return mpCost==0?0:ModernDisintegrate.REUSE_MILLIS;}
        State wireRevision(){return new State(entityId,castCount,revision==0?0:Math.incrementExact(revision),lastSequence,mpSpent,mpCost,initialLawful,lawful,hit);}
    }
    record Result(Reason reason,ModernBattleObservation.Capture observation) implements ModernBattleCommand {
        Result {Objects.requireNonNull(reason);Objects.requireNonNull(observation);}
        ModernNpcBattle.Frame frame(){return ModernLegacyBattleProjection.npcs(observation);}
    }
    static void validate(long match,long generation,long revision,ModernBattleEngine.Phase phase,
            ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest map,List<ModernBattleEngine.Actor> actors,
            List<ModernCombatProtocol.Vital> vitals,List<ModernSpellProtocol.CastState> casts,
            List<ModernItemProtocol.State> items,List<ModernRegenProtocol.State> regen,
            ModernQuestObjectives.Snapshot quest,ModernTimeAttack.Snapshot timeAttack,
            List<ModernNpcMagic.CastState> arrows,List<State> states) {
        if(states.isEmpty())return;int size=actors.size();
        if(size<1||size>100||states.size()!=size||vitals.size()!=size||casts.size()!=size||regen.size()!=size
                ||!arrows.isEmpty()&&arrows.size()!=size||!items.isEmpty()&&items.size()!=size||!quest.lives().isEmpty()&&quest.lives().size()!=size
                ||room.summary().config().mode()!=(timeAttack.enabled()?4:2))throw bad();
        var revisions=new HashSet<Long>();var occupiedRevisions=new HashSet<Long>();
        for(var c:casts)occupiedRevisions.add(c.castRevision());
        for(var a:arrows)occupiedRevisions.add(a.revision());
        for(var item:items)occupiedRevisions.add(item.useRevision());
        for(var v:vitals){occupiedRevisions.add(v.attackRevision());occupiedRevisions.add(v.hurtRevision());occupiedRevisions.add(v.deathRevision());}
        for(var r:regen){occupiedRevisions.add(r.hpRevision());occupiedRevisions.add(r.mpRevision());}
        for(int i=0;i<size;i++) {
            var s=states.get(i);var a=actors.get(i);var v=vitals.get(i);var c=casts.get(i);
            if(s.entityId()!=a.entityId()||s.revision()>revision||s.lastSequence()>c.requestSequence()
                    ||Math.addExact(Math.addExact(c.castCount(),arrows.isEmpty()?0:arrows.get(i).castCount()),s.castCount())>c.requestSequence()
                    ||(a.member().identity().isHuman()?s.mpCost()==0:!s.equals(State.initial(s.entityId(),0,0)))
                    ||(phase==ModernBattleEngine.Phase.LOADING||phase==ModernBattleEngine.Phase.COUNTDOWN)&&s.castCount()!=0)throw bad();
            if(s.hit()==null)continue;var h=s.hit();var scope=h.target().scope();
            var life=quest.lives().isEmpty()?null:quest.lives().get(i);long incarnation=life==null?1:life.incarnation();
            if(!revisions.add(s.revision())||occupiedRevisions.contains(s.revision())||scope.roomId()!=room.summary().id()||scope.matchId()!=match||scope.generation()!=generation
                    ||h.lane()!=a.member().team()||!ModernWorldProtocol.contains(map,h.from())||!ModernWorldProtocol.contains(map,h.to())
                    ||h.casterLife()>incarnation||life!=null&&(h.casterLife()==incarnation?s.revision()<=life.respawnRevision():s.revision()>life.respawnRevision())
                    ||h.casterLife()==incarnation&&v.hp()==0&&s.revision()>=v.deathRevision()
                    ||s.revision()==revision&&(v.hp()==0||c.requestSequence()!=s.lastSequence()||h.casterLife()!=incarnation
                        ||!a.position().equals(h.from())||timeAttack.enabled()&&timeAttack.state().departed().contains(s.entityId())))throw bad();
        }
    }
    static void validateNpcs(ModernBattleEngine.Snapshot world,ModernNpcRoster.Snapshot npcs) {
        for(var s:world.npcDisintegrations())if(s.hit()!=null) {
            var h=s.hit();
            if(!h.target().scope().equals(npcs.scope())||h.target().serial()>npcs.spawned())throw bad();
            var target=npcs.actors().stream().filter(n->n.id().equals(h.target())).findFirst().orElse(null);
            if(target!=null&&(target.lane()!=h.lane()||target.life()<h.targetLife()||h.hpBefore()>target.template().maximumHp()
                    ||h.outcome().death()&&h.targetLife()==target.life()&&target.alive()))throw bad();
            if(s.revision()==world.revision()&&(target==null||target.life()!=h.targetLife()||!target.cell().equals(h.to())
                    ||target.hp()!=h.outcome().remainingHp()||world.vitals().get((int)s.entityId()-1).hp()==0
                    ||h.outcome().appliedDamage()>0&&target.action()!=(h.outcome().death()?ModernNpcRoster.Action.DEATH:ModernNpcRoster.Action.HURT)))throw bad();
        }
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid NPC disintegrate ledger");}
    private ModernNpcDisintegrateState(){}
}
