package local.lito381.server;

import java.util.List;
import java.util.Objects;

/** Public, event-time visual facts. No inventory, item ID, source epoch,
 * recovery roll, damage calculation or raw monotonic clock is reachable.
 * Latest successful event per lane only; not an input/HP authority or ACK. */
final class ModernBattlePresentation {
    record Shot(long count,long revision,long attackSequence,long loadingElapsedNanos,long life,long equipmentRevision,
            ModernWorldMap.Cell from,ModernTargetHistory.Target target,long targetLife,ModernWorldMap.Cell to,int sprite,boolean hit) {
        Shot {
            Objects.requireNonNull(from);Objects.requireNonNull(to);Objects.requireNonNull(target);
            if(count<1||revision<2||attackSequence<count||attackSequence>=revision||loadingElapsedNanos<0||life<1||equipmentRevision<0
                    ||targetLife<1||sprite!=ModernAmmo.SPRITE||from.equals(to)
                    ||!(target instanceof ModernTargetHistory.Participant)&&!(target instanceof ModernTargetHistory.Honor)
                    ||target instanceof ModernTargetHistory.Honor&&targetLife!=1)throw bad();
        }
    }
    record Item(long count,long revision,long loadingElapsedNanos,long life,long equipmentRevision,ModernWorldMap.Cell cell,int effect) {
        Item {
            Objects.requireNonNull(cell);
            if(count<1||revision<=count||loadingElapsedNanos<0||life<1||equipmentRevision<0
                    ||effect!=189&&effect!=194&&effect!=197&&effect!=191&&effect!=750&&effect!=751)throw bad();
        }
        static Item from(ModernUseHistory.Items history,long elapsed) {
            var u=Objects.requireNonNull(history.last());var a=u.before();
            return new Item(history.uses(),u.revision(),elapsed,a.life(),a.equipmentRevision(),
                    new ModernWorldMap.Cell(a.x(),a.y()),ModernItemRuntime.effect(history));
        }
    }
    record State(Shot shot,Item item) {
        static final State EMPTY=new State(null,null);
        State withShot(Shot value){return new State(Objects.requireNonNull(value),item);}
        State withItem(Item value){return new State(shot,Objects.requireNonNull(value));}
    }
    /** Capture keeps the event-time clock/life, never recreates them from a
     * later melee attack, current position, respawn or changed equipment. */
    static void capture(ModernBattleObservation.CapturedActor a) {
        var p=Objects.requireNonNull(a.presentation());var shot=p.shot();var ammo=a.ammo();
        if((ammo!=null&&ammo.shotCount()>0)!=(shot!=null))throw bad();
        if(shot!=null&&(shot.count()!=ammo.shotCount()||shot.revision()!=ammo.shotRevision()
                ||shot.attackSequence()!=ammo.attackSequence()||shot.from().x()!=ammo.fromX()||shot.from().y()!=ammo.fromY()
                ||shot.to().x()!=ammo.toX()||shot.to().y()!=ammo.toY()||shot.sprite()!=ammo.projectileSprite()||shot.hit()!=ammo.hit()
                ||targetEntity(shot.target())!=ammo.lastTargetEntityId()))throw bad();
        var supplies=a.supplies();var item=p.item();
        if((supplies!=null&&supplies.history().last()!=null)!=(item!=null))throw bad();
        if(item!=null&&!item.equals(Item.from(supplies.history(),item.loadingElapsedNanos())))throw bad();
    }
    static long targetEntity(ModernTargetHistory.Target t) {
        if(t instanceof ModernTargetHistory.Participant p)return p.entityId();
        if(t instanceof ModernTargetHistory.Honor h)return h.entityId();
        throw bad();
    }
    static void world(long revision,ModernBattleObservation.Clock clock,ModernWorldMap.Manifest manifest,
            ModernBattleObservation.Features features,List<ModernBattleObservation.Actor> actors,ModernBattleObservation.Modes modes) {
        for(var a:actors) {
            var p=Objects.requireNonNull(a.presentation());var s=p.shot();var item=p.item();
            if(clock.activeElapsedNanos()<0&&(s!=null||item!=null))throw bad();
            if(s!=null) {
                if(!features.ammo()||a.vital()==null||s.attackSequence()>a.vital().attackSequence()||s.revision()>a.vital().attackRevision()
                        ||s.revision()==a.vital().attackRevision()&&targetEntity(s.target())!=a.vital().target())throw bad();
                anchor(a,revision,clock,manifest,s.revision(),s.loadingElapsedNanos(),s.life(),s.equipmentRevision(),s.from());
                if(!ModernWorldProtocol.contains(manifest,s.to()))throw bad();
                if(s.target() instanceof ModernTargetHistory.Participant t) {
                    var target=actors.stream().filter(v->v.pose().entityId()==t.entityId()).findFirst().orElseThrow(ModernBattlePresentation::bad);
                    if(t.entityId()==a.pose().entityId()||!t.identity().equals(target.pose().member().identity())||target.resources()==null
                            ||s.targetLife()>Math.incrementExact(target.resources().life()))throw bad();
                } else if(s.target() instanceof ModernTargetHistory.Honor t) {
                    var target=modes.honor().objectives().stream().filter(v->v.entityId()==t.entityId()).findFirst().orElseThrow(ModernBattlePresentation::bad);
                    if(t.team()!=target.team()||!s.to().equals(target.cell()))throw bad();
                } else throw bad();
            }
            if(item!=null) {
                if(!features.items()||!features.timed()&&(item.effect()==191||item.effect()==750||item.effect()==751))throw bad();
                anchor(a,revision,clock,manifest,item.revision(),item.loadingElapsedNanos(),item.life(),item.equipmentRevision(),item.cell());
            }
        }
    }
    private static void anchor(ModernBattleObservation.Actor actor,long revision,ModernBattleObservation.Clock clock,
            ModernWorldMap.Manifest manifest,long eventRevision,long elapsed,long life,long gear,ModernWorldMap.Cell cell) {
        var r=actor.resources();
        if(r==null||eventRevision>revision||elapsed>clock.loadingElapsedNanos()||life>Math.incrementExact(r.life())
                ||gear>r.equipmentRevision()||!ModernWorldProtocol.contains(manifest,cell))throw bad();
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid public battle presentation facts");}
    private ModernBattlePresentation(){}
}
