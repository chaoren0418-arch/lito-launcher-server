package local.lito381.server;
import java.util.*;
/** Original Origin schema and detached same-actor authority contracts; no PC/Hub/HP/socket/SQL fixture. */
public final class SourceRawNpcPlan {
 static int cases;static void check(boolean value,String why){if(!value)throw new AssertionError(why);cases++;}
 static NativeSourcePhysicalNpc.Origin origin(NativeSourceCubeOrigin.Target target,NativeSourceDamageDisplay.Actor source){return new NativeSourcePhysicalNpc.Origin(null,target,source,null,0,4,5);}
 public static void main(String[]a)throws Exception{
  var ordinary=origin(null,null);check(ordinary.nativeSourceRawNpcSource==null,"ordinary Origin cannot acquire raw source authority");check(NativeSourceRawNpc.sourceState(null)==-1&&NativeSourceRawNpc.sourceState(ordinary)==-1,"unmarked Source retains exact ordinary source policy");
  var access=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");access.setAccessible(true);var unsafe=(sun.misc.Unsafe)access.get(null);var c=(NativeAreaMaterialAction.Context)unsafe.allocateInstance(NativeAreaMaterialAction.Context.class);var actor=new Object();var owner=new Object();var life=new Object();var generation=new Object();var source=new NativeSourceDamageDisplay.Actor(null,null,life,4,5);var proof=new NativeSourceRawNpc.Source(source,true,true);var target=new NativeSourceCubeOrigin.Target(7,1,null,null,actor,owner,life,generation);var admission=new NativeSourceRawNpc.Admission(proof,actor,owner,life,generation);var previous=NativeAreaMaterialAction.current.get();try{NativeAreaMaterialAction.current.set(c);c.nativeSourceMagicAdmission=admission;
   var exact=origin(target,source);check(exact.nativeSourceRawNpcSource==proof,"only original source/actual target admission attaches detached proof");check(NativeSourcePhysicalNpc.Origin.class.getRecordComponents().length==7,"native canonical Origin record shape retained");
   check(origin(new NativeSourceCubeOrigin.Target(7,1,null,null,new Object(),owner,life,generation),source).nativeSourceRawNpcSource==null,"same numerical id cannot borrow another actual actor");
   check(origin(new NativeSourceCubeOrigin.Target(7,1,null,null,actor,new Object(),life,generation),source).nativeSourceRawNpcSource==null,"another owner cannot borrow admission");
   check(origin(new NativeSourceCubeOrigin.Target(7,1,null,null,actor,owner,new Object(),generation),source).nativeSourceRawNpcSource==null,"another life cannot borrow admission");
   check(origin(new NativeSourceCubeOrigin.Target(7,1,null,null,actor,owner,life,new Object()),source).nativeSourceRawNpcSource==null,"another encounter generation cannot borrow admission");
   check(origin(target,new NativeSourceDamageDisplay.Actor(null,null,life,4,5)).nativeSourceRawNpcSource==null,"equal-valued reconstructed source is not the original producer Actor");
   c.nativeSourceMagicAdmission=new Object();check(origin(target,source).nativeSourceRawNpcSource==null,"untyped metadata grants no raw policy");
   var snapshot=new NativeSourceMagicNpc.Snapshot(null,Map.of(),List.of(),Map.of(),Map.of());unsafe.putObject(c,unsafe.objectFieldOffset(NativeAreaMaterialAction.Context.class.getDeclaredField("nativeSourceMagicSnapshot")),snapshot);c.nativeSourceMagicAdmission=admission;check(NativeSourceRawNpc.admission()==snapshot,"raw admission shares only the original immutable audience snapshot");
   c.nativeSourceMagicAdmission=null;check(NativeSourceRawNpc.admission()==null,"restored producer carrier cannot leak raw audience policy");
  }finally{if(previous==null)NativeAreaMaterialAction.current.remove();else NativeAreaMaterialAction.current.set(previous);c.nativeSourceMagicAdmission=null;}
  if(cases!=12)throw new AssertionError(cases);System.out.println("PASS12 actual Origin/record-schema and detached original source-target-owner-life-generation admission contracts; no bound raw HP/dead-source/Main/game/SQL qualification");
 }
}
