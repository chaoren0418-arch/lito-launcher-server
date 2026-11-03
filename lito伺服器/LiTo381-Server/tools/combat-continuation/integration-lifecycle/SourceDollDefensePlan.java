package local.lito381.server;
/** Actual defense audience predicate, no player/Hub/socket/SQL fixture. */
public final class SourceDollDefensePlan {
 static int cases;static void check(boolean b,String name){if(!b)throw new AssertionError(name);cases++;}
 public static void main(String[]args){
  check(NativeSourceDollDefense.visible(900,true,100,100,100,100),"positive defense gfx at victim");
  check(NativeSourceDollDefense.visible(900,true,110,110,100,100),"positive gfx radius10 diagonal, not native screen");
  check(!NativeSourceDollDefense.visible(900,true,111,100,100,100),"positive gfx outside radius10");
  check(!NativeSourceDollDefense.visible(900,false,100,100,100,100),"positive gfx disabled preference");
  check(NativeSourceDollDefense.visible(0,true,118,100,100,100),"doll aggression visible at original screen distance18");
  check(!NativeSourceDollDefense.visible(0,true,120,100,100,100),"doll aggression outside original native screen");
  check(!NativeSourceDollDefense.visible(0,false,100,100,100,100),"doll aggression disabled preference");
  check(!NativeSourceDollDefense.visible(0,true,110,110,100,100),"aggression does not use target Chebyshev radius10");
  check(NativeSourceDollDefense.visible(901,true,90,90,100,100),"positive gfx opposite diagonal boundary");
  check(!NativeSourceDollDefense.visible(901,true,89,100,100,100),"positive gfx negative offset outside radius10");
  if(cases!=10)throw new AssertionError(cases);System.out.println("PASS10 actual defense proc preference/radius/native-screen predicate cases; no player/Hub/socket/SQL");
 }
}
