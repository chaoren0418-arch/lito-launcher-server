package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.Objects;
import static local.lito381.server.ModernEquipmentOverrideStore.Fingerprint;

/** Dedicated full-VIP inspection/SET, not the legacy equipment receipt. Both
 * require explicit DATA+EQUIPMENT selection. These codecs grant no owner,
 * database or live service attachment and do not enable a default listener. */
final class ModernVipEquipmentProtocol {
    static final int REQUEST_KIND=82,RESULT_KIND=83,REQUEST_BYTES=156,RESULT_PREFIX_BYTES=236;
    static final int MAX_RESULT_BYTES=RESULT_PREFIX_BYTES+ModernBattleVipWire.MAX_APPEARANCE_BYTES;
    enum Action { INSPECT, SET }
    enum Outcome { REJECTED, INSPECTED, UNCHANGED, APPLIED, PERSISTED_RETIRED, CONFLICT, DENIED }
    /** Fixed wire values; never an ordinal of an independently evolving rule. */
    enum Denial { NONE, INVALID_REQUEST, UNKNOWN_ITEM, UNQUALIFIED_ITEM, NOT_EQUIPMENT,
        UNQUALIFIED_EQUIPPED_ITEM, INVALID_LOADOUT, CLASS_NOT_ALLOWED, LEVEL_TOO_LOW, LEVEL_TOO_HIGH,
        CURSED, SEALED, SLOT_OCCUPIED, RING_LIMIT, AID_LIMIT, DUPLICATE_AID_CATALOG, TALISMAN_LIMIT,
        SHIELD_GUARDER_CONFLICT, TWO_HANDED_SHIELD_CONFLICT, LAYER_DEPENDENCY }
    record Request(int requestId,ModernBattleVipCommandState.Proof proof,Action action,long itemObjectId,boolean desired,Fingerprint token) {
        Request {
            Objects.requireNonNull(proof);Objects.requireNonNull(action);Objects.requireNonNull(token);
            if(requestId==0||action==Action.SET&&itemObjectId<1||action==Action.INSPECT
                    &&(itemObjectId!=0||desired||!token.equals(ModernEquipmentAuthority.ZERO)))throw bad();
        }
        static Request inspect(int id,ModernBattleVipCommandState.Proof proof) {
            return new Request(id,proof,Action.INSPECT,0,false,ModernEquipmentAuthority.ZERO);
        }
        ModernVipEquipmentAuthority.Request command() {
            if(action!=Action.SET)throw bad();return new ModernVipEquipmentAuthority.Request(proof,itemObjectId,desired,token);
        }
        @Override public String toString(){return "VipEquipmentIntent[redacted]";}
    }
    record Reply(Request echo,ModernBattleVipCommandState.Rejection rejection,Outcome outcome,Denial denial,long blockingItem,
            long durableRevision,Fingerprint token,ModernBattleVipWire.Payload payload,ModernBattleLifecycle lifecycle) {
        Reply {
            Objects.requireNonNull(echo);Objects.requireNonNull(rejection);Objects.requireNonNull(outcome);Objects.requireNonNull(denial);
            Objects.requireNonNull(payload);Objects.requireNonNull(lifecycle);
            validate(echo,rejection,outcome,denial,blockingItem,durableRevision,token,payload,lifecycle);
        }
        @Override public String toString(){return "VipEquipmentReceipt[redacted]";}
    }
    static Reply inspected(Request q,ModernVipEquipmentAuthority.Inspection inspection,ModernMatchDescriptor descriptor,ModernBattleLifecycle lifecycle) {
        var p=ModernBattleVipWire.candidate(inspection.observation(),descriptor,q.proof().life().player());
        sameQuote(inspection.quote(),p);
        return new Reply(q,ModernBattleVipCommandState.Rejection.NONE,Outcome.INSPECTED,Denial.NONE,0,-1,inspection.quote().token(),p,lifecycle);
    }
    static Reply executed(Request q,ModernVipEquipmentAuthority.Result result,ModernMatchDescriptor descriptor,ModernBattleLifecycle lifecycle) {
        var p=ModernBattleVipWire.candidate(result.observation(),descriptor,q.proof().life().player());
        if(result.quote()!=null)sameQuote(result.quote(),p);
        return new Reply(q,ModernBattleVipCommandState.Rejection.NONE,Outcome.valueOf(result.status().name()),
                result.denial()==null?Denial.NONE:Denial.valueOf(result.denial().name()),result.blockingItem(),
                result.publication()==null?-1:result.publication().equipmentRevision(),result.quote()==null?null:result.quote().token(),p,lifecycle);
    }
    static Reply rejected(Request q,ModernBattleVipCommandState.EquipmentAdmission admission,ModernMatchDescriptor descriptor,ModernBattleLifecycle lifecycle) {
        if(admission.change()!=null)throw bad();
        return new Reply(q,admission.rejection(),Outcome.REJECTED,Denial.NONE,0,-1,null,
                ModernBattleVipWire.candidate(admission.observation(),descriptor,q.proof().life().player()),lifecycle);
    }
    private static void sameQuote(ModernVipEquipmentAuthority.Quote quote,ModernBattleVipWire.Payload payload) {
        if(!quote.view().world().equals(payload.view().world())||!quote.view().personal().equals(payload.view().personal()))throw bad();
    }
    private static void validate(Request q,ModernBattleVipCommandState.Rejection rejection,Outcome outcome,Denial denial,long blocking,
            long durable,Fingerprint token,ModernBattleVipWire.Payload payload,ModernBattleLifecycle lifecycle) {
        var proof=q.proof();var p=proof.life();var view=payload.view();var w=view.world();var personal=view.personal();
        var versions=personal.equipment().versions();var seen=proof.versions();long life=Math.incrementExact(personal.history().resources().life());
        long hp=personal.history().resources().vitals().hp();
        boolean overlay=lifecycle.phase()==ModernWorldProtocol.Phase.ABORTED&&w.phase()!=ModernBattleEngine.Phase.FINISHED&&w.result()==null;
        if(!p.player().equals(personal.scope())||!p.world().equals(w.identity())||p.entity()!=personal.entityId()||p.revision()>w.revision()
                ||lifecycle.revision()<=w.revision()||!Objects.equals(lifecycle.result(),w.result())
                ||!overlay&&(lifecycle.phase()!=ModernWorldProtocol.Phase.valueOf(w.phase().name())
                    ||lifecycle.abortReason()!=ModernWorldProtocol.AbortReason.valueOf(w.abortReason().name()))
                ||lifecycle.race().enabled()||lifecycle.timeAttackFinished()||lifecycle.siegeFinished()
                ||blocking<0||(outcome==Outcome.DENIED)!=(denial!=Denial.NONE)||outcome!=Outcome.DENIED&&blocking!=0
                ||(outcome==Outcome.REJECTED)!=(rejection!=ModernBattleVipCommandState.Rejection.NONE)
                ||(outcome==Outcome.INSPECTED||outcome==Outcome.UNCHANGED||outcome==Outcome.APPLIED)!=(token!=null))throw bad();
        if(outcome==Outcome.REJECTED) {
            if(durable!=-1)throw bad();
            boolean valid=switch(rejection) {
                case NOT_ACTIVE->w.phase()!=ModernBattleEngine.Phase.ACTIVE||lifecycle.phase()!=ModernWorldProtocol.Phase.ACTIVE;
                case STALE_LIFE->life!=p.life();
                case DEAD->life==p.life()&&hp==0;
                case STALE_SAVED_EQUIPMENT->seen.savedEquipmentRevision()!=versions.savedEquipmentRevision();
                case STALE_RUNTIME_ABILITY->seen.savedEquipmentRevision()==versions.savedEquipmentRevision()&&seen.runtimeAbilityRevision()!=versions.runtimeAbilityRevision();
                case STALE_LIFECYCLE->seen.savedEquipmentRevision()==versions.savedEquipmentRevision()&&seen.runtimeAbilityRevision()==versions.runtimeAbilityRevision()&&seen.lifecycleSequence()!=versions.lifecycleSequence();
                case UNAVAILABLE,BUSY->true;
                case NONE->false;
            };
            if(!valid)throw bad();return;
        }
        if((outcome==Outcome.INSPECTED)!=(q.action()==Action.INSPECT))throw bad();
        switch(outcome) {
            case INSPECTED,UNCHANGED,APPLIED->{
                boolean applied=outcome==Outcome.APPLIED;
                var expected=applied?new ModernBattleVipObservation.Versions(Math.incrementExact(seen.savedEquipmentRevision()),
                        Math.incrementExact(seen.runtimeAbilityRevision()),Math.incrementExact(seen.lifecycleSequence())):seen;
                if(!versions.equals(expected)||life!=p.life()||w.phase()!=ModernBattleEngine.Phase.ACTIVE
                        ||lifecycle.phase()!=ModernWorldProtocol.Phase.ACTIVE||outcome!=Outcome.APPLIED&&hp==0
                        ||durable!=(outcome==Outcome.INSPECTED?-1:versions.savedEquipmentRevision())
                        ||applied&&w.revision()<=p.revision())throw bad();
                if(q.action()==Action.SET&&!personal.equipment().equipped().stream().anyMatch(e->e.objectId()==q.itemObjectId()&&e.equipped()==q.desired()))throw bad();
            }
            case PERSISTED_RETIRED->{
                if(durable!=Math.incrementExact(seen.savedEquipmentRevision())||versions.savedEquipmentRevision()!=seen.savedEquipmentRevision()
                        ||lifecycle.phase()!=ModernWorldProtocol.Phase.ABORTED&&lifecycle.phase()!=ModernWorldProtocol.Phase.FINISHED)throw bad();
            }
            case CONFLICT,DENIED->{if(durable!=-1)throw bad();}
            default->throw bad();
        }
    }
    static byte[] encodeRequest(Request q,ModernSessionBoundary.Negotiated peer) {
        requirePeer(peer);var b=ByteBuffer.allocate(REQUEST_BYTES);var p=q.proof().life();var s=p.player();var w=p.world();var v=q.proof().versions();
        b.putInt(0x4c544d31).putInt(REQUEST_BYTES).putInt(q.requestId()).putShort((short)REQUEST_KIND).putShort((short)0);
        b.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch()).putInt(s.channelId());
        b.putLong(w.roomId()).putLong(w.matchId()).putLong(w.generation()).putLong(p.entity()).putLong(p.life()).putLong(p.revision());
        b.putLong(v.savedEquipmentRevision()).putLong(v.runtimeAbilityRevision()).putLong(v.lifecycleSequence());
        b.put((byte)q.action().ordinal()).put((byte)(q.desired()?1:0)).putShort((short)0).putLong(q.itemObjectId()).put(q.token().bytes());return b.array();
    }
    static Request decodeRequest(byte[] bytes,ModernSessionBoundary.Negotiated peer) {
        requirePeer(peer);if(bytes==null||bytes.length!=REQUEST_BYTES)throw bad();var b=ByteBuffer.wrap(bytes);
        if(b.getInt()!=0x4c544d31||b.getInt()!=REQUEST_BYTES)throw bad();int id=b.getInt();
        if(b.getShort()!=REQUEST_KIND||b.getShort()!=0)throw bad();
        var s=new ModernRooms.Scope(b.getInt(),b.getLong(),b.getLong(),b.getInt());
        var w=new ModernTargetHistory.World(b.getLong(),b.getLong(),b.getLong());
        var life=new ModernBattleCommandLife.Proof(s,w,b.getLong(),b.getLong(),b.getLong());
        var versions=new ModernBattleVipObservation.Versions(b.getLong(),b.getLong(),b.getLong());
        int action=Byte.toUnsignedInt(b.get()),desired=Byte.toUnsignedInt(b.get());
        if(action>=Action.values().length||desired>1||b.getShort()!=0)throw bad();
        long item=b.getLong();var token=new byte[32];b.get(token);
        return new Request(id,new ModernBattleVipCommandState.Proof(life,versions),Action.values()[action],item,desired==1,new Fingerprint(token));
    }
    static byte[] encodeReply(Reply r,ModernSessionBoundary.Negotiated peer) {
        requirePeer(peer);var data=ModernBattleVipWire.encodeSelected(r.payload(),peer);
        int size=Math.addExact(RESULT_PREFIX_BYTES,data.length);if(size>MAX_RESULT_BYTES)throw bad();var b=ByteBuffer.allocate(size);
        b.put(encodeRequest(r.echo(),peer)).putInt(4,size).putShort(12,(short)RESULT_KIND);
        b.put((byte)r.rejection().ordinal()).put((byte)r.outcome().ordinal()).put((byte)r.denial().ordinal()).put((byte)0);
        b.putLong(r.blockingItem()).putLong(r.durableRevision()).put(r.token()==null?new byte[32]:r.token().bytes());
        ModernBattleOperationProtocol.putLifecycle(b,r.lifecycle());b.putInt(data.length).put(data);return b.array();
    }
    private static void requirePeer(ModernSessionBoundary.Negotiated peer) {
        ModernSessionExtensions.requireContext(peer);if(!ModernSessionExtensions.allowsVipEquipment(peer))throw bad();
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid full VIP equipment contract");}
    private ModernVipEquipmentProtocol(){}
}
