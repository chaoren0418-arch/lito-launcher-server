package local.lito381.server;
import java.util.*;
/** One original-summon power selection for every captured actor, owned only by the existing Context. */
final class NativeSourceDollPassive {
 static final class Choice {
  final NativeSourceDollAction.Stamp stamp;
  NativeMagicDollPowers frozen;
  Choice(NativeSourceDollAction.Stamp stamp){this.stamp=stamp;}
  /** Caller holds this actor's original action lock. Invalid role/inventory never acquires fresh powers. */
  NativeMagicDollPowers resolve(boolean sameActor,boolean originalSummon){
   if(!sameActor)return NativeMagicDollPowers.empty();
   if(frozen==null)frozen=originalSummon&&stamp!=null&&stamp.doll()!=null?NativeMagicDollPowers.from(stamp.doll().type()):NativeMagicDollPowers.empty();
   return frozen;
  }
 }
 static Object selections(NativeSourceDollDefense.Snapshot snapshot){var choices=new IdentityHashMap<ClientSession,Choice>();for(var entry:snapshot.dolls().entrySet())choices.put(entry.getKey(),new Choice(entry.getValue()));return Collections.unmodifiableMap(choices);}
 static boolean actor(NativeSourceDollAction.Stamp stamp){return stamp.source().valid(stamp.source().life())&&CombatAccess.field(stamp.caster(),"inventory")==stamp.inventory();}
 static NativeMagicDollPowers powers(ClientSession pc){
  if(!NativeSourceDollCommit.enabled(pc)||!pc.nativePlayerActionLock().isHeldByCurrentThread())return pc.nativeSourceDollOriginalPowers();
  var snapshot=NativeSourceDollDefense.current();if(snapshot==null)return pc.nativeSourceDollOriginalPowers();
  var choices=(Map<?,?>)snapshot.nativeSourceDollPassivePowers;var choice=(Choice)choices.get(pc);if(choice==null)return NativeMagicDollPowers.empty();
  boolean sameActor=actor(choice.stamp);return choice.resolve(sameActor,sameActor&&choice.frozen==null&&choice.stamp.before());
 }
 private NativeSourceDollPassive(){}
}
