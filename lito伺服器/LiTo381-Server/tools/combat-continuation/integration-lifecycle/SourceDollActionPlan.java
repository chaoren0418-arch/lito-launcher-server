package local.lito381.server;
import java.util.*;
/** Pure summon-identity scenarios. No ClientSession, Hub, repository, socket, or player fixture is initialized. */
public final class SourceDollActionPlan {
 static int cases;
 static void check(boolean value,String name){if(!value)throw new AssertionError(name);cases++;}
 static NativeMagicDoll doll(long id,long item,SourceMagicDollType type,int x,long expires){return new NativeMagicDoll(id,item,type,x,101,4,7,5,expires);}
 public static void main(String[]args){
  var type=new SourceMagicDollType(55000,0,0,List.of(),List.of(),List.of(),1800,6100,"doll",List.of());
  var other=new SourceMagicDollType(55001,0,0,List.of(),List.of(),List.of(),1800,6101,"other",List.of());
  var before=doll(7001,8001,type,100,1000);
  check(NativeSourceDollAction.sameSummon(null,null,900),"no doll is stable");
  check(!NativeSourceDollAction.sameSummon(null,before,900),"new summon cannot inherit an empty action");
  check(!NativeSourceDollAction.sameSummon(before,null,900),"unsummoned original");
  check(NativeSourceDollAction.sameSummon(before,before,999),"original summon before expiry");
  check(NativeSourceDollAction.sameSummon(before,doll(7001,8001,type,108,1000),900),"ordinary movement keeps summon identity");
  check(!NativeSourceDollAction.sameSummon(before,doll(7002,8001,type,100,1000),900),"new object same backing item");
  check(!NativeSourceDollAction.sameSummon(before,doll(7001,8001,type,100,2000),900),"same-id resummon expiry differs");
  check(!NativeSourceDollAction.sameSummon(before,doll(7001,8002,type,100,1000),900),"different backing item");
  check(!NativeSourceDollAction.sameSummon(before,doll(7001,8001,other,100,1000),900),"changed doll powers/type");
  check(!NativeSourceDollAction.sameSummon(before,before,1000),"expiry boundary rejects old proc");
  check(!NativeSourceDollAction.sameSummon(before,before,1001),"expired original");
  if(cases!=11)throw new AssertionError();System.out.println("PASS11 pure actual summon-identity scenarios, no player/Hub/SQL/socket fixture");
 }
}
