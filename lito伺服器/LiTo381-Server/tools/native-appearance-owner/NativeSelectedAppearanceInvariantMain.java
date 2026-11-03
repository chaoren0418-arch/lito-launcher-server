package local.lito381.server;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Program invariants with trusted in-memory test material; no auth/SQL/GUI claim. */
public final class NativeSelectedAppearanceInvariantMain {
    static final List<String> passed=new ArrayList<>();
    interface Checked {void run()throws Exception;}
    static void require(boolean ok,String name){if(!ok)throw new AssertionError(name);}
    static void rejects(Checked action,String name)throws Exception{try{action.run();}catch(IllegalStateException|IllegalArgumentException expected){return;}throw new AssertionError(name);}
    static final class Harness implements AutoCloseable {
        final Object capability=new Object(),attachment=new Object();
        final ModernRooms.Scope selection=new ModernRooms.Scope(7,77,Long.MIN_VALUE+1,0);
        final AtomicLong wall=new AtomicLong(),nanos=new AtomicLong(System.nanoTime());
        final ModernBackendScope scope=new ModernBackendScope(System.nanoTime()+TimeUnit.SECONDS.toNanos(10),new ModernBackendScope.AbortScheduler(){public boolean execute(Runnable r){return false;}public void remove(Runnable r){}});
        final ModernEquipmentLease lease=new ModernEquipmentLease.Issuer().open(capability,new TournamentPresence.CharacterKey(7,77),selection.epoch(),()->{});
        final NativeSelectedAppearanceOwner owner=new NativeSelectedAppearanceOwner(capability,attachment,lease,lease.key(),selection.epoch(),5,wall::get,nanos::get);
        ModernEquipmentLease.Operation op;
        final NativeAppearanceReadProvider.Bundle bundle;final ModernPhysicalCombat.Fighter fighter;
        Harness(boolean timed)throws Exception {
            ModernBackendScope.bind(scope);op=lease.operation(scope);owner.lobby(0);
            var row=new ModernApplicationProtocol.CharacterRow(77,"invariantfixture",3,0,60);
            var source=ModernPhysicalCombatTest.fixtureSource(7,row,100,10,1);
            if(timed){
                var q=source.input().equipment();var item=q.source().inventory().get(0);
                var f=new ModernItemLifetime.Fact(item.objectId(),item.template().catalogId(),item.template().kind(),item.equipped(),OptionalLong.of(0),false,OptionalLong.empty(),Optional.of("2026-10-04 00:00:01"),0,false);
                var lifetime=new ModernItemLifetime.Observation(7,77,"2026-10-04 00:00:00",0,System.nanoTime(),Map.of(item.objectId(),f));
                q=new ModernEquipmentFacts.Qualified(q.source(),q.items(),q.catalog(),lifetime);
                source=new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(q,true,source.input().weapons()),source.growth(),source.templates(),source.procCatalogIds());
            }
            var binding=ModernCombatProfileTest.binding(source.input().equipment());
            fighter=ModernPhysicalCombat.qualify(source,binding,Optional.empty(),0);
            bundle=new NativeAppearanceReadProvider.Bundle(new NativeAppearanceReadProvider.Base(7,77,3,0,734),new ModernEquipmentAuthority.PhysicalLoaded(source,binding.fingerprint()),scope);
        }
        NativeSelectedAppearanceOwner.Retained commit()throws Exception {
            var t=owner.begin(selection,op);op.restrictCommit(fighter.profile().evaluatedAtMillis(),fighter.profile().validUntilMillis(),wall::get);
            require(op.tryCommit(),"actual equipment permit");return owner.publish(t,bundle,fighter);
        }
        void nextOperation()throws Exception{op.close();op=lease.operation(scope);}
        public void close(){op.close();lease.invalidate();scope.finish();ModernBackendScope.unbind();}
    }
    static void test(String name,Checked action)throws Exception{action.run();passed.add(name);}
    public static void main(String[] args)throws Exception {
        test("immutable_bound_unknown_observation",()->{try(var h=new Harness(false)){var r=h.commit();var v=r.read(h.capability,h.attachment,5,r.observationRevision());require(!v.qualified()&&v.overrideState()==NativeSelectedAppearanceOwner.OverrideState.UNKNOWN&&v.appearanceRevision()==null&&v.overrideCutoffExclusiveMillis()==null,"UNKNOWN no authority");require(v.source()==h.bundle&&v.reconciled()==h.fighter&&v.privateRegistration()==5,"same immutable material");}});
        test("wrong_owner_attachment_generation_revision",()->{try(var h=new Harness(false)){var r=h.commit();for(int i=0;i<4;i++){final int n=i;rejects(()->r.read(n==0?new Object():h.capability,n==1?new Object():h.attachment,n==2?6:5,n==3?r.observationRevision()+1:r.observationRevision()),"foreign retained access");}}});
        test("wrong_selection_source_object_epoch_channel",()->{try(var h=new Harness(false)){for(var s:List.of(new ModernRooms.Scope(42,77,h.selection.epoch(),0),new ModernRooms.Scope(7,78,h.selection.epoch(),0),new ModernRooms.Scope(7,77,-1,0),new ModernRooms.Scope(7,77,h.selection.epoch(),1)))rejects(()->h.owner.begin(s,h.op),"foreign selection");}});
        test("wrong_immutable_source_bundle_identity",()->{try(var h=new Harness(false)){rejects(()->new NativeAppearanceReadProvider.Bundle(new NativeAppearanceReadProvider.Base(42,77,3,0,734),h.bundle.physical(),h.scope),"source mismatch");rejects(()->new NativeAppearanceReadProvider.Bundle(new NativeAppearanceReadProvider.Base(7,77,3,1,734),h.bundle.physical(),h.scope),"sex mismatch");}});
        test("no_publication_without_actual_operation_permit",()->{try(var h=new Harness(false)){var t=h.owner.begin(h.selection,h.op);rejects(()->h.owner.publish(t,h.bundle,h.fighter),"unadmitted Work");}});
        test("old_ticket_after_real_owner_update",()->{try(var h=new Harness(false)){var t=h.owner.begin(h.selection,h.op);h.owner.changed(NativeSelectedAppearanceOwner.Cause.EQUIPMENT_WORK_STARTED);require(h.op.tryCommit(),"permit admitted");rejects(()->h.owner.publish(t,h.bundle,h.fighter),"old observation revision");}});
        test("wrong_owner_ticket_same_numeric_selection",()->{try(var h=new Harness(false)){var other=new NativeSelectedAppearanceOwner(h.capability,new Object(),h.lease,h.lease.key(),h.selection.epoch(),5,h.wall::get,h.nanos::get);other.lobby(0);var t=other.begin(h.selection,h.op);require(h.op.tryCommit(),"actual permit");rejects(()->h.owner.publish(t,h.bundle,h.fighter),"different owner object");}});
        test("refresh_supersedes_old_retained",()->{try(var h=new Harness(false)){var old=h.commit();h.nextOperation();var fresh=h.commit();require(!old.current()&&fresh.current()&&fresh.observationRevision()>old.observationRevision(),"refresh revision");}});
        test("equipment_work_revokes_retained_even_uncertain_commit",()->{try(var h=new Harness(false)){var r=h.commit();h.owner.changed(NativeSelectedAppearanceOwner.Cause.EQUIPMENT_WORK_STARTED);require(!r.current(),"update revoked");}});
        test("failed_publication_revokes_prepared_retained",()->{try(var h=new Harness(false)){var r=h.commit();h.owner.changed(NativeSelectedAppearanceOwner.Cause.OBSERVATION_FAILED);require(!r.current(),"failed publication revoked");}});
        test("BACK_reselect_same_key_epoch_never_revives_old_handle",()->{try(var h=new Harness(false)){var r=h.commit();h.op.close();require(h.lease.tryInvalidateIfNoPermit(),"actual BACK lease retirement");h.owner.close(NativeSelectedAppearanceOwner.Cause.BACK);var newLease=new ModernEquipmentLease.Issuer().open(h.capability,h.lease.key(),h.selection.epoch(),()->{});var next=new NativeSelectedAppearanceOwner(h.capability,new Object(),newLease,h.lease.key(),h.selection.epoch(),9);next.lobby(0);require(!r.current(),"old owner stays revoked");newLease.invalidate();}});
        test("disconnect_and_lobby_reentry_revoke",()->{try(var h=new Harness(false)){var r=h.commit();h.owner.lobby(255);h.owner.lobby(0);require(!r.current(),"same-channel reentry");h.nextOperation();r=h.commit();h.owner.close(NativeSelectedAppearanceOwner.Cause.DISCONNECT);require(!r.current(),"disconnect");}});
        test("source_item_exclusive_cutoff_before_and_at",()->{try(var h=new Harness(true)){var r=h.commit();require(h.fighter.profile().validUntilMillis()==1000,"derived item cutoff");h.wall.set(999);require(r.current(),"before item cutoff");h.wall.set(1000);require(!r.current(),"at exclusive item cutoff");}});
        test("source_item_elapsed_before_publication",()->{try(var h=new Harness(true)){var t=h.owner.begin(h.selection,h.op);h.op.restrictCommit(0,1000,h.wall::get);h.wall.set(1000);require(!h.op.tryCommit(),"late operation rejects");rejects(()->h.owner.publish(t,h.bundle,h.fighter),"late observation rejects");}});
        test("actual_Work_deadline_exclusive_retained_window",()->{try(var h=new Harness(false)){var r=h.commit();h.op.close();h.scope.finish();h.nanos.set(h.scope.deadlineNanos()-1);require(r.current(),"committed diagnostic after Work close before cutoff");h.nanos.set(h.scope.deadlineNanos());require(!r.current(),"at actual Work deadline");}});
        test("backend_cancel_and_clock_regression_revoke",()->{try(var h=new Harness(false)){var r=h.commit();h.wall.set(-1);require(!r.current(),"wall clock regressed");h.wall.set(0);h.scope.cancel(ModernBackendExecutor.Failure.CANCELLED);require(!r.current(),"cancelled scope");}});
        var report=Map.of("state","PASSED_PROGRAM_INVARIANTS_ONLY","cases",passed,"count",passed.size(),"real_SQL",false,"actual_authenticated_selection",false,"qualified",false,"GUI",false);
        Files.writeString(Path.of(args[0]),NativeTerrainProbeMain.json(report));System.out.println("Appearance owner invariants: "+passed.size());
    }
}
