package local.lito381.server;
import java.util.*;
/** Retire stale natural-monster leases from the original scan, without selecting a new actor by id. */
final class NativeSourceCubeNpcLease {
 static boolean natural(NativeSourceCubeOrigin.Target target){return target!=null&&(target.kind()==1||target.kind()==2)&&target.pc()==null&&target.binding()==null&&target.actor()!=null&&target.owner()!=null&&target.life()!=null&&target.generation()!=null;}
 static boolean keep(NativeSourceCubeOrigin.Lease lease,NativeSourceCubeOrigin.Scan scan,Object engine,long id){
  if(lease==null||scan==null||!natural(lease.target()))return false;
  var effect=lease.effect();var target=lease.target();return effect!=null&&effect.engine()==engine&&effect.epoch()==scan.epoch()&&target.id()==id&&NativeSourceCubePlayer.sameTarget(target,scan.targets().get(id));
 }
 /** Called at the existing PC retirement prefix, already inside original Hub/engine monitors. */
 static void retire(Object engine){
  if(!CombatRules.loaded()||!Thread.holdsLock(engine)||!(NativeSourceCubeOrigin.scene(engine)instanceof NativeSourceCubeOrigin.Scene scene)||!Thread.holdsLock(scene.hub())||!(CombatAccess.field(engine,"nativeSourceCubeScan")instanceof NativeSourceCubeOrigin.Scan scan)||scan.epoch()!=NativeSourceCubeOrigin.epoch(engine))return;
  retireScanned(engine,scan);
 }
 /** Engine-only transformation for the independently captured immutable scan; no actor/Hub lock or lookup. */
 static void retireScanned(Object engine,NativeSourceCubeOrigin.Scan scan){
  if(!Thread.holdsLock(engine))throw new IllegalStateException("original cube engine monitor required");
  if(scan==null||scan.epoch()!=NativeSourceCubeOrigin.epoch(engine))return;
  var iterator=((Map<?,?>)CombatAccess.field(engine,"statuses")).values().iterator();
  while(iterator.hasNext()){
   var actual=iterator.next();if(!String.valueOf(CombatAccess.field(actual,"targetKind")).equals("MONSTER"))continue;
   var origin=CombatAccess.field(actual,"nativeSourceCubeOrigin");var lease=origin instanceof NativeSourceCubeOrigin.Lease bound?bound:null;
   long id=(long)CombatAccess.field(actual,"targetObjectId");
   if(lease==null||lease.effect()==null||lease.actual()!=actual||CombatAccess.field(actual,"cube")!=lease.effect().cube()||!keep(lease,scan,engine,id))iterator.remove();
  }
 }
 private NativeSourceCubeNpcLease(){}
}
