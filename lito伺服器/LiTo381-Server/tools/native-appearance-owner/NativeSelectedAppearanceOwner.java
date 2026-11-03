package local.lito381.server;

import java.util.Objects;
import java.util.function.LongSupplier;

/** Selection-owned observation retention ONLY. UNKNOWN never grants render admission.
 * Local observationRevision is not a persisted/Classic appearance revision.
 * All methods under this monitor are bounded, with no JDBC/file/user callback.
 * The actual equipment Operation admits before publication; no new lease exists. */
final class NativeSelectedAppearanceOwner {
    enum OverrideState { UNKNOWN }
    enum Cause { SOURCE_REFRESH, EQUIPMENT_WORK_STARTED, OBSERVATION_FAILED, LOBBY_ENTER, LOBBY_LEAVE, BACK, DISCONNECT }
    record Observation(ModernRooms.Scope selection,long privateRegistration,long observationRevision,
                       NativeAppearanceReadProvider.Bundle source,ModernPhysicalCombat.Fighter reconciled,
                       long equipmentRevision,long observedAtMillis,long sourceItemCutoffExclusiveMillis,
                       long actualWorkDeadlineNanos) {
        boolean qualified(){return false;}
        OverrideState overrideState(){return OverrideState.UNKNOWN;}
        Long appearanceRevision(){return null;}
        Long overrideCutoffExclusiveMillis(){return null;}
        @Override public String toString(){return "NativeAppearanceObservation[redacted,qualified=false,UNKNOWN]";}
    }
    static final class ReadTicket {
        private final NativeSelectedAppearanceOwner owner;private final long revision;
        private final ModernRooms.Scope selection;private final ModernBackendScope scope;
        private final ModernEquipmentLease.Operation operation;
        private ReadTicket(NativeSelectedAppearanceOwner o,long rev,ModernRooms.Scope s,ModernEquipmentLease.Operation op){owner=o;revision=rev;selection=s;scope=op.scope();operation=op;}
    }
    static final class Retained {
        private final NativeSelectedAppearanceOwner owner;private final Observation value;private final ModernBackendScope scope;
        private Retained(NativeSelectedAppearanceOwner o,Observation v,ModernBackendScope s){owner=o;value=v;scope=s;}
        Observation read(Object capability,Object attachment,long generation,long expectedObservationRevision){
            synchronized(owner){
                if(capability!=owner.capability||attachment!=owner.attachment||generation!=owner.privateRegistration||expectedObservationRevision!=value.observationRevision()
                        ||!owner.current(value,scope))throw new IllegalStateException("appearance observation retired or owner differs");
                return value;
            }
        }
        boolean current(){synchronized(owner){return owner.current(value,scope);}}
        long observationRevision(){return value.observationRevision();}
        @Override public String toString(){return "NativeRetainedObservation[redacted,no appearance grant]";}
    }
    private final Object capability,attachment;
    private final ModernEquipmentLease actualSelectionLease;
    private final TournamentPresence.CharacterKey key;
    private final long epoch,privateRegistration;
    private final LongSupplier wall,nanos;
    private long revision;private boolean closed;private int channel=255;private Cause lastCause;
    NativeSelectedAppearanceOwner(Object capability,Object attachment,ModernEquipmentLease lease,
                                 TournamentPresence.CharacterKey key,long epoch,long generation){
        this(capability,attachment,lease,key,epoch,generation,System::currentTimeMillis,System::nanoTime);
    }
    NativeSelectedAppearanceOwner(Object capability,Object attachment,ModernEquipmentLease lease,
                                 TournamentPresence.CharacterKey key,long epoch,long generation,LongSupplier wall,LongSupplier nanos){
        this.capability=Objects.requireNonNull(capability);this.attachment=Objects.requireNonNull(attachment);actualSelectionLease=Objects.requireNonNull(lease);
        this.key=Objects.requireNonNull(key);this.epoch=epoch;privateRegistration=generation;this.wall=Objects.requireNonNull(wall);this.nanos=Objects.requireNonNull(nanos);
        if(!lease.matches(capability,key,epoch))throw new IllegalArgumentException("actual selected lease required");
    }
    static boolean enabled(){return Boolean.getBoolean("native.selected.appearance.observation.only");}
    synchronized void lobby(int next){if(closed)throw new IllegalStateException("appearance owner retired");if(next!=255&&(next<0||next>3))throw new IllegalArgumentException("channel");channel=next;advance(next==255?Cause.LOBBY_LEAVE:Cause.LOBBY_ENTER);}
    synchronized void changed(Cause cause){if(!closed)advance(cause);}
    synchronized void close(Cause cause){if(!closed){closed=true;advance(cause);}}
    private void advance(Cause cause){try{revision=Math.addExact(revision,1);}catch(ArithmeticException overflow){closed=true;throw overflow;}lastCause=Objects.requireNonNull(cause);}
    ReadTicket begin(ModernRooms.Scope selection,ModernEquipmentLease.Operation operation)throws Exception {
        operation.scope().checkActive();
        synchronized(this){
            if(!selectionMatches(selection)||!operation.matchesKey(key)||!operation.matchesSelection(capability,epoch))throw new IllegalStateException("actual appearance selection differs");
            return new ReadTicket(this,revision,selection,operation);
        }
    }
    Retained publish(ReadTicket ticket,NativeAppearanceReadProvider.Bundle source,ModernPhysicalCombat.Fighter fighter){
        synchronized(this){
            if(ticket.owner!=this||ticket.revision!=revision||!selectionMatches(ticket.selection)||!source.belongsTo(ticket.scope)
                    ||ticket.scope.cancellation()!=null||nanos.getAsLong()-ticket.scope.deadlineNanos()>=0
                    ||!ticket.operation.isCommitAdmitted()||!ticket.operation.matchesSelection(capability,epoch))throw new IllegalStateException("appearance observation commit retired");
            var p=fighter.profile();var c=source.physical().source().input().equipment().source();
            if(!p.source().equals(c)||c.sourceId()!=key.sourceId()||c.character().objectId()!=key.objectId()
                    ||p.equipmentRevision()<0||!fighter.supported()||!validAt(p.evaluatedAtMillis(),p.validUntilMillis()))throw new IllegalStateException("appearance observation source or lifetime differs");
            advance(Cause.SOURCE_REFRESH);
            var value=new Observation(ticket.selection,privateRegistration,revision,source,fighter,p.equipmentRevision(),p.evaluatedAtMillis(),p.validUntilMillis(),ticket.scope.deadlineNanos());
            return new Retained(this,value,ticket.scope);
        }
    }
    private boolean current(Observation v,ModernBackendScope work){
        return v.observationRevision()==revision&&selectionMatches(v.selection())&&work.cancellation()==null
                &&nanos.getAsLong()-v.actualWorkDeadlineNanos()<0&&validAt(v.observedAtMillis(),v.sourceItemCutoffExclusiveMillis());
    }
    private boolean validAt(long observed,long cutoff){long now=wall.getAsLong();return now>=observed&&(cutoff<0||now<cutoff);}
    private boolean selectionMatches(ModernRooms.Scope s){return !closed&&channel==s.channelId()&&s.sourceId()==key.sourceId()&&s.objectId()==key.objectId()&&s.epoch()==epoch&&actualSelectionLease.matches(capability,key,epoch);}
    synchronized long observationRevision(){return revision;}
    synchronized boolean closed(){return closed;}
    long privateRegistration(){return privateRegistration;}
    synchronized String lastCause(){return lastCause==null?"SELECTED":lastCause.name();}
}
