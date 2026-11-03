package local.lito381.server;
import java.lang.reflect.*;
/** Source arithmetic/faction and actual metadata schema only; no bound role, HP, Hub or SQL fixture. */
public final class SourceGroundNpcPlan {
 static int cases;static void check(boolean v,String why){if(!v)throw new AssertionError(why);cases++;}
 public static void main(String[]a)throws Exception{
  check(NativeSourceGroundNpc.fire(2)==-50,"Source weak fire template");check(NativeSourceGroundNpc.fire(-2)==50,"Source resist fire template");check(NativeSourceGroundNpc.fire(0)==0,"no template affinity");check(NativeSourceGroundNpc.fire(1)==0,"earth does not imply fire resist");check(NativeSourceGroundNpc.fire(-4)==0,"water does not imply fire resist");
  check(NativeSourceGroundNpc.bound(0)==1,"minimum random bound");check(NativeSourceGroundNpc.bound(-4)==1,"negative INT clamps bound");check(NativeSourceGroundNpc.bound(1)==1,"odd low INT");check(NativeSourceGroundNpc.bound(19)==9,"integer INT half");
  check(NativeSourceGroundNpc.damage(0,0)==19,"Source base damage");check(NativeSourceGroundNpc.damage(0,8)==27,"highest INT19 roll");check(NativeSourceGroundNpc.damage(50,0)==9,"Source positive resist truncates after multiplication");check(NativeSourceGroundNpc.damage(-50,0)==19,"Source ignores negative fire on firewall path");check(NativeSourceGroundNpc.damage(100,0)==0,"full resistance");check(NativeSourceGroundNpc.damage(150,0)==0,"damage clamps nonpositive");
  check(NativeSourceGroundNpc.allowed(1,0,0),"same quest lane");check(!NativeSourceGroundNpc.allowed(1,0,1),"other quest lane rejected");check(!NativeSourceGroundNpc.allowed(1,null,0),"missing original faction rejects");check(NativeSourceGroundNpc.allowed(2,null,-1),"time natural target has no quest lane");check(!NativeSourceGroundNpc.allowed(3,0,0),"controlled NPC not recast as natural monster");
  check(NativeSourceGroundOrigin.Entity.class.getRecordComponents().length==5&&Modifier.isFinal(NativeSourceGroundOrigin.Entity.class.getDeclaredField("nativeSourceGroundFaction").getModifiers()),"same Entity schema, immutable captured faction");
  check(NativeSourcePhysicalNpc.Origin.class.getRecordComponents().length==7&&Modifier.isFinal(NativeSourcePhysicalNpc.Origin.class.getDeclaredField("nativeSourceGroundNpcProof").getModifiers()),"same shared FIFO Origin schema, immutable detached lease proof");
  check(NativeSourceGroundNpc.capture(null)==null,"unscoped origin cannot borrow proof");check(!NativeSourceGroundNpc.entity(null),"missing proof rejects");
  var resistance=new BattleElementalFallDownStatus();resistance.activate(1,5,2);check(NativeSourceGroundNpc.damage(50+resistance.modifier(2,2),0)==19,"actual active fire fall-down removes base resistance");resistance.clear();check(NativeSourceGroundNpc.damage(50+resistance.modifier(2,2),0)==9,"cleared original lease restores base resistance");
  if(cases!=26)throw new AssertionError(cases);System.out.println("PASS26 Source firewall NPC arithmetic/faction/schema contracts; no bound HP, pair, life, Main, cursor or MySQL qualification");
 }
}
