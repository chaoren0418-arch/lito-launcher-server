package local.lito381.server;

import java.util.Objects;

/** Candidate registration-owned proof fence. Caller must retain its service generation
 * fence across validation, canonical Loaded authority, completion and publication. */
final class ModernNativeResourceLedger {
    private record Binding(TournamentPresence.Owner owner,ModernRooms.Scope scope,
                           ModernTargetHistory.World world,
                           ModernNativeCanonicalWorldProtocol.SourceAttachment source) {}
    private record Pending(TournamentPresence.Owner owner,ModernRooms.Scope scope,long room,long revision,
                           ModernNativeCanonicalWorldProtocol.SourceAttachment source) {}
    private static final class Generation {}
    static final class PrepareAdmission {
        private final ModernNativeResourceLedger ledger;private final Generation generation;
        private final ModernNativeCanonicalWorldProtocol.Request request;private final Pending pending;
        private PrepareAdmission(ModernNativeResourceLedger ledger,Generation generation,
                                 ModernNativeCanonicalWorldProtocol.Request request,Pending pending){
            this.ledger=ledger;this.generation=generation;this.request=request;this.pending=pending;
        }
    }
    static final class LoadAdmission {
        private final ModernNativeResourceLedger ledger;
        private final Generation generation;
        private final ModernNativeCanonicalWorldProtocol.Request request;
        private LoadAdmission(ModernNativeResourceLedger ledger,Generation generation,ModernNativeCanonicalWorldProtocol.Request request){this.ledger=ledger;this.generation=generation;this.request=request;}
    }
    private Binding binding;
    private Pending pending;
    private Generation generation=new Generation();
    private boolean verified;
    synchronized void rememberDiscovery(TournamentPresence.Owner owner,ModernRooms.Scope scope,long room,long revision,
                                       ModernNativeCanonicalWorldProtocol.SourceAttachment source,
                                       ModernSessionBoundary.Negotiated peer) {
        if(!ModernNativeCanonicalWorldProtocol.negotiated(peer)||source==null||!ModernLargeMaps.supported(source.manifest(),peer))throw rejected();
        if(binding!=null||room<1||revision<1||revision==Long.MAX_VALUE)throw rejected();
        var next=new Pending(Objects.requireNonNull(owner),Objects.requireNonNull(scope),room,revision,source);
        if(next.equals(pending))return;pending=next;generation=new Generation();verified=false;
    }
    synchronized PrepareAdmission admitPrepare(TournamentPresence.Owner owner,
                                               ModernNativeCanonicalWorldProtocol.Request request,
                                               long currentRoom,long currentRevision,
                                               ModernNativeCanonicalWorldProtocol.SourceAttachment currentSource,
                                               ModernSessionBoundary.Negotiated peer){
        var q=request.world();
        if(binding!=null||pending==null||q.action()!=ModernBattleWorldProtocol.Action.PREPARE||pending.owner()!=owner
                ||!pending.scope().equals(q.scope())||pending.room()!=q.roomId()||pending.revision()!=q.roomRevision()
                ||pending.room()!=currentRoom||pending.revision()!=currentRevision||!pending.source().equals(currentSource))throw rejected();
        ModernNativeCanonicalWorldProtocol.authorize(currentSource,request,peer);
        return new PrepareAdmission(this,generation,request,pending);
    }
    synchronized void completePrepare(PrepareAdmission admission,ModernBattleWorldProtocol.Reply actual){
        if(admission==null||admission.ledger!=this||admission.generation!=generation||pending==null
                ||!pending.equals(admission.pending)||binding!=null||actual==null||actual.status()!=ModernWorldProtocol.Status.OK)throw rejected();
        ModernBattleWorldProtocol.validate(new ModernApplicationProtocol.BattleWorldResult(admission.request.requestId(),admission.request.world(),actual));
        binding=new Binding(pending.owner(),pending.scope(),actual.world(),pending.source());
        pending=null;generation=new Generation();verified=false;
    }
    synchronized LoadAdmission admitLoaded(TournamentPresence.Owner owner,
                                          ModernNativeCanonicalWorldProtocol.Request request,
                                          ModernSessionBoundary.Negotiated peer){
        if(request.world().action()!=ModernBattleWorldProtocol.Action.LOADED||binding==null)throw rejected();
        requireBinding(owner,request.world().scope(),new ModernTargetHistory.World(request.world().roomId(),request.world().matchId(),request.world().generation()),binding.source());
        ModernNativeCanonicalWorldProtocol.authorize(binding.source(),request,peer);
        return new LoadAdmission(this,generation,request);
    }
    /** Only call after the canonical authority returned OK and self Loaded in DATA. */
    synchronized void completeLoaded(LoadAdmission admission,ModernBattleWorldProtocol.Reply actual){
        if(admission==null||admission.ledger!=this||admission.generation!=generation||binding==null)throw rejected();
        if(actual==null||actual.status()!=ModernWorldProtocol.Status.OK||actual.view()==null
                ||!binding.world().equals(actual.world())||!binding.source().manifest().equals(actual.view().world().manifest())
                ||!binding.scope().equals(actual.view().personal().scope()))throw rejected();
        ModernBattleWorldProtocol.validate(new ModernApplicationProtocol.BattleWorldResult(admission.request.requestId(),admission.request.world(),actual));
        verified=true;
    }
    synchronized void requireVerified(TournamentPresence.Owner owner,ModernRooms.Scope scope,
                                      ModernTargetHistory.World world,
                                      ModernNativeCanonicalWorldProtocol.SourceAttachment currentSource){
        if(!verified)throw rejected();requireBinding(owner,scope,world,currentSource);
    }
    synchronized void requirePendingAttachment(TournamentPresence.Owner owner,ModernRooms.Scope scope,long room,long revision,
                                              ModernNativeCanonicalWorldProtocol.SourceAttachment source,
                                              ModernSessionBoundary.Negotiated peer) {
        if(!ModernNativeCanonicalWorldProtocol.negotiated(peer)||pending==null||binding!=null||pending.owner()!=owner
                ||!pending.scope().equals(scope)||pending.room()!=room||pending.revision()!=revision
                ||!pending.source().equals(source)||!ModernLargeMaps.supported(source.manifest(),peer))throw rejected();
    }
    /** Service calls under its gate only after the ticket guard and all participant checks. */
    synchronized void attachPreparedWorld(TournamentPresence.Owner owner,ModernRooms.Scope scope,
                                         ModernTargetHistory.World world,
                                         ModernNativeCanonicalWorldProtocol.SourceAttachment source,
                                         ModernSessionBoundary.Negotiated peer) {
        requirePendingAttachment(owner,scope,world.roomId(),world.generation()-1,source,peer);
        binding=new Binding(owner,scope,world,source);pending=null;generation=new Generation();verified=false;
    }
    synchronized void retireDiscovery(){if(binding==null){pending=null;generation=new Generation();verified=false;}}
    synchronized void requireAttachment(TournamentPresence.Owner owner,ModernRooms.Scope scope,ModernTargetHistory.World world,
                                        ModernNativeCanonicalWorldProtocol.SourceAttachment source){requireBinding(owner,scope,world,source);}
    synchronized void retire(){binding=null;pending=null;verified=false;generation=new Generation();}
    private void requireBinding(TournamentPresence.Owner owner,ModernRooms.Scope scope,
                                ModernTargetHistory.World world,
                                ModernNativeCanonicalWorldProtocol.SourceAttachment currentSource){
        if(binding==null||binding.owner()!=owner||!binding.scope().equals(scope)||!binding.world().equals(world)
                ||!binding.source().equals(currentSource))throw rejected();
    }
    private static SecurityException rejected(){return new SecurityException("Native registration proof is missing or retired");}
}
