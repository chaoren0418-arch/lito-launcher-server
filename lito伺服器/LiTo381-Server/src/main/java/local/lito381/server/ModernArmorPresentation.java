package local.lito381.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Qualified self-target skill68 presentation producer. This is a server-side
 * candidate, NOT schema8 or evidence of assets loaded by a client. The native
 * EquipProcAction sends proc GFX before ProcSkillEffectHelper sends castGfx;
 * mode2 emits BOTH packets even for the same self target. The helper does not
 * repeat castGfx for an existing buff and does not send a body spell action.
 * A recipient may suppress proc graphics in its own presentation settings;
 * that preference must not change protection, resource admission or HP. */
final class ModernArmorPresentation {
    static final int STATUS_ICON=1562,MAX_VISUALS=2*ModernArmorProcs.MAX_ROWS+1;
    record Anchor(long moveSequence,ModernWorldMap.Cell cell) {
        Anchor {Objects.requireNonNull(cell);if(moveSequence<0)throw bad();}
        void validate(ModernWorldMap.Manifest manifest,ModernBattleEngine.Actor actor,long eventRevision,long worldRevision) {
            if(moveSequence>actor.lastMoveSequence()||eventRevision>worldRevision
                    ||eventRevision==worldRevision&&!cell.equals(actor.position())
                    ||manifest.sectors().stream().noneMatch(s->{var o=s.key().origin();
                        return (long)cell.x()-o.x()>=0&&(long)cell.x()-o.x()<ModernWorldMap.SIDE
                            &&(long)cell.y()-o.y()>=0&&(long)cell.y()-o.y()<ModernWorldMap.SIDE;}))throw bad();
            // Teleport changes position without consuming a MOVE sequence.
            // Equal moveSequence in a later revision does NOT imply equal cell.
        }
    }
    record Requirements(List<Integer> graphics,List<Integer> icons) {
        Requirements {
            graphics=List.copyOf(graphics);icons=List.copyOf(icons);
            if(graphics.size()>ModernArmorProcs.MAX_ROWS+1||!graphics.equals(graphics.stream().distinct().sorted().toList())
                    ||graphics.stream().anyMatch(g->g<1||g>65535)
                    ||!(icons.isEmpty()||icons.equals(List.of(STATUS_ICON)))
                    ||graphics.isEmpty()!=icons.isEmpty()
                    ||!graphics.isEmpty()&&!graphics.contains(ModernArmorProtectionCatalog.GRAPHIC))throw bad();
        }
    }
    enum Role { PROC_TARGET,PROC_ACTOR,SKILL_CAST }
    record Visual(int ordinal,int graphic,Role role) {
        Visual {Objects.requireNonNull(role);if(ordinal<0||ordinal>=MAX_VISUALS||graphic<1||graphic>65535
                ||role==Role.SKILL_CAST&&graphic!=ModernArmorProtectionCatalog.GRAPHIC)throw bad();}
    }
    /** Latest accepted trigger batch, not a complete reliable event history.
     * Identity uses enclosing world/entity/life and this revision/sequence;
     * unrelated publications must not replay it. Position and time stay frozen. */
    record Batch(long sequence,long revision,long observedNanos,Anchor anchor,List<Visual> visuals) {
        Batch {
            Objects.requireNonNull(anchor);visuals=List.copyOf(visuals);
            if(sequence<1||revision<1||observedNanos<0||observedNanos>ModernArmorProcProtection.MAX_TIME||visuals.size()>MAX_VISUALS)throw bad();
            int casts=0;
            for(int i=0;i<visuals.size();i++)if(visuals.get(i).ordinal()!=i
                    ||visuals.get(i).role()==Role.SKILL_CAST&&++casts>1)throw bad();
        }
    }
    /** No row ID, inventory objid, database binding, loadout or other player
     * receipt. The owner entity is explicit even when sources share an objid. */
    record Candidate(ModernTargetHistory.World world,long worldRevision,long entityId,long life,long equipmentRevision,
            Requirements required,Batch batch) {
        Candidate {
            Objects.requireNonNull(world);Objects.requireNonNull(required);
            if(worldRevision<1||entityId<1||life<0||equipmentRevision<0||batch!=null&&(batch.revision()>worldRevision
                    ||batch.visuals().stream().anyMatch(v->!required.graphics().contains(v.graphic()))))throw bad();
        }
    }
    static Candidate from(ModernBattleObservation.Capture capture,ModernBattleAudience.View audience) {
        var w=audience.world();var personal=audience.personal();
        if(!capture.features().armorProtection()||!capture.identity().equals(w.identity())||capture.revision()!=w.revision())throw bad();
        var a=capture.actors().stream().filter(v->personal.scope().equals(v.scope())).findFirst().orElseThrow(ModernArmorPresentation::bad);
        var visible=w.actors().stream().filter(v->v.pose().entityId()==personal.entityId()).findFirst().orElseThrow(ModernArmorPresentation::bad);
        if(personal.equipment()==null)throw bad();
        return from(capture,a,visible,personal.equipment().revision());
    }
    static Candidate from(ModernBattleObservation.Capture capture,ModernBattleVipObservation.World world,ModernBattleVipObservation.Personal personal) {
        if(!capture.hasVip()||!capture.features().armorProtection()||!capture.identity().equals(world.identity())||capture.revision()!=world.revision())throw bad();
        var a=capture.actors().stream().filter(v->personal.scope().equals(v.scope())).findFirst().orElseThrow(ModernArmorPresentation::bad);
        var visible=world.actors().stream().filter(v->v.pose().entityId()==personal.entityId()).findFirst().orElseThrow(ModernArmorPresentation::bad);
        return from(capture,a,visible,personal.equipment().versions().runtimeAbilityRevision());
    }
    private static Candidate from(ModernBattleObservation.Capture capture,ModernBattleObservation.CapturedActor a,
            ModernBattleObservation.Actor visible,long equipmentRevision) {
        var view=a.armorProtection();var state=visible.armorProtection();
        if(view==null||!Objects.equals(state,ModernArmorProtectionEffects.State.from(view))||!a.pose().equals(visible.pose())
                ||a.loadout()==null||a.resources().equipmentRevision()!=equipmentRevision)throw bad();
        var requirements=a.loadout().fighter().armorProtection().presentationRequirements();
        Batch batch=null;
        if(view.anchor()!=null) {
            var visuals=new ArrayList<Visual>();
            for(var effect:state.effects()) {
                int gfx=Math.toIntExact(effect.graphics());long mode=effect.graphicsMode();
                if(gfx>0) {
                    if(mode==0||mode==2)visuals.add(new Visual(visuals.size(),gfx,Role.PROC_TARGET));
                    if(mode==1||mode==2)visuals.add(new Visual(visuals.size(),gfx,Role.PROC_ACTOR));
                }
                if(effect.appliedProtection())visuals.add(new Visual(visuals.size(),ModernArmorProtectionCatalog.GRAPHIC,Role.SKILL_CAST));
            }
            batch=new Batch(state.sequence(),state.revision(),state.observedNanos(),view.anchor(),visuals);
        }
        return new Candidate(capture.identity(),capture.revision(),visible.pose().entityId(),state.life(),equipmentRevision,requirements,batch);
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid armor presentation candidate");}
    private ModernArmorPresentation(){}
}
