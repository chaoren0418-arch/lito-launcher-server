package local.lito381.server;
import java.util.*;import java.util.concurrent.atomic.*;
/** Actual one-selection resolver with real immutable Source powers; no player/Hub/socket/SQL fixture. */
public final class SourceDollPassivePlan {
 static int cases;static void check(boolean b,String name){if(!b)throw new AssertionError(name);cases++;}
 static NativeSourceDollAction.Stamp stamp(SourceMagicDollType type){var doll=new NativeMagicDoll(7001,8001,type,100,100,4,7,5,Long.MAX_VALUE);return new NativeSourceDollAction.Stamp(null,null,doll,null,new AtomicReference<>());}
 public static void main(String[]args){
  var reduction=new SourceMagicDollPower(1,"","PASSIVE_DMG_REDUX",7,0,0,0,0,0,0,0,0,0,0,0,0);
  var intelligence=new SourceMagicDollPower(2,"","PASSIVE_INT",3,0,0,0,0,0,0,0,0,0,0,0,0);
  var type=new SourceMagicDollType(55000,0,0,List.of(1,2),List.of(),List.of(),1800,6100,"original",List.of(reduction,intelligence));
  var choice=new NativeSourceDollPassive.Choice(stamp(type));var first=choice.resolve(true,true);
  check(first.value("PASSIVE_DMG_REDUX")==7&&first.value("PASSIVE_INT")==3,"real Source passive values retained");
  check(choice.resolve(true,false)==first,"retired summon does not mix scalar powers after first selection");
  check(choice.resolve(true,true)==first,"multiple stat getters reuse the same exact immutable result");
  check(choice.resolve(false,true)==NativeMagicDollPowers.empty(),"different role/inventory cannot use sealed old powers");
  check(choice.frozen==first,"invalid actor lookup never overwrites a sealed selection");
  var absent=new NativeSourceDollPassive.Choice(null);check(absent.resolve(true,true)==NativeMagicDollPowers.empty(),"no captured summon selects empty");
  check(absent.resolve(true,false)==NativeMagicDollPowers.empty(),"empty selection remains empty");
  var stale=new NativeSourceDollPassive.Choice(stamp(type));check(stale.resolve(true,false)==NativeMagicDollPowers.empty(),"replaced/expired original before first locked read selects empty");
  check(stale.resolve(true,true)==NativeMagicDollPowers.empty(),"later original/replacement cannot grant fresh powers to empty action");
  var unbound=new NativeSourceDollPassive.Choice(stamp(type));check(unbound.resolve(false,true)==NativeMagicDollPowers.empty()&&unbound.frozen==null,"invalid actor does not initialize a new selection");
  if(cases!=10)throw new AssertionError(cases);System.out.println("PASS10 actual captured power resolver/immutable Source row scenarios, no player/Hub/socket/SQL");
 }
}
