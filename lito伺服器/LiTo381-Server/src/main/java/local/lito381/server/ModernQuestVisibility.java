package local.lito381.server;

import java.util.List;
import java.util.function.ToLongFunction;

/** Project only after the service has authenticated the request's owner/scope.
 * No fabricated hidden actors: stable global IDs survive in a sparse view.
 * Room membership and match high-water revisions are public; opposing force
 * positions, equipment-derived vitals, NPCs and combat receipts are not. */
final class ModernQuestVisibility {
    static ModernWorldProtocol.Frame world(ModernWorldProtocol.Frame frame,ModernRooms.Scope viewer) {
        if(frame==null||!frame.isolatedForces())return frame;
        var id=ModernParticipantId.human(viewer.sourceId(),viewer.objectId());
        int lane=frame.room().members().stream().filter(m->m.identity().equals(id)).findFirst().orElseThrow(ModernQuestVisibility::bad).team();
        if(frame.audienceLane()>=0){if(frame.audienceLane()!=lane)throw bad();return frame;}
        var actors=frame.actors().stream().filter(a->a.member().team()==lane).toList();
        var casts=frame.casts().stream().filter(c->actors.stream().anyMatch(a->a.entityId()==c.entityId())).toList();
        return new ModernWorldProtocol.Frame(frame.matchId(),frame.generation(),frame.revision(),frame.phase(),frame.abortReason(),frame.room(),frame.manifest(),
                actors,frame.completion(),casts,frame.descriptor(),frame.race(),lane,frame.quest().forLane(lane));
    }
    static ModernCombatProtocol.Frame combat(ModernCombatProtocol.Frame frame,ModernRooms.Scope viewer) {
        if(frame==null||!frame.world().isolatedForces())return frame;
        var world=world(frame.world(),viewer);
        return new ModernCombatProtocol.Frame(world,visible(frame.vitals(),ModernCombatProtocol.Vital::entityId,world),frame.damages(),
                visible(frame.items(),ModernItemProtocol.State::entityId,world),visible(frame.weights(),ModernWeightProtocol.State::entityId,world),
                frame.buffs(),frame.ammo(),visible(frame.regen(),ModernRegenProtocol.State::entityId,world),frame.honor(),frame.fireStorms(),visible(frame.npcSpells(),ModernNpcMagic.CastState::entityId,world),frame.disintegrations(),visible(frame.npcDisintegrations(),ModernNpcDisintegrateState.State::entityId,world),visible(frame.npcFireStorms(),ModernNpcFireStormState.State::entityId,world));
    }
    static ModernNpcProtocol.Frame npc(ModernNpcProtocol.Frame frame,ModernRooms.Scope viewer) {
        if(frame==null||!frame.combat().world().isolatedForces())return frame;
        var combat=combat(frame.combat(),viewer);int lane=combat.world().audienceLane();
        return new ModernNpcProtocol.Frame(combat,frame.scope(),frame.revision(),frame.elapsedMillis(),frame.spawned(),frame.closed(),
                frame.actors().stream().filter(a->a.lane()==lane).toList(),
                frame.exchanges().stream().filter(e->combat.world().actors().stream().anyMatch(a->a.member().identity().equals(e.participant()))).toList(),frame.resources(),frame.names());
    }
    private static <T> List<T> visible(List<T> rows,ToLongFunction<T> entity,ModernWorldProtocol.Frame world) {
        return rows.stream().filter(row->world.actors().stream().anyMatch(a->a.entityId()==entity.applyAsLong(row))).toList();
    }
    private static SecurityException bad(){return new SecurityException("force view owner mismatch");}
    private ModernQuestVisibility(){}
}
