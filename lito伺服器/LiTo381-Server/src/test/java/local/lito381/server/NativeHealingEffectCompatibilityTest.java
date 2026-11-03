package local.lito381.server;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NativeHealingEffectCompatibilityTest {
 @Test void nativeObjectEffectUsesEmbeddedHealSpritesForAllTargetKinds() {
  // Captured crashing C_USE_SPELL 14 02 02 03 00 00 60 produced gfx 11723.
  // Native embedded list.spr: heal 744, extra 829, greater 830, full 831.
  int[][] values={{11722,744},{11723,829},{11724,830},{11725,831}};
  for(long target:new long[]{11554,0x38102002L,0x60000003L})for(int[] pair:values){
   byte[] p=Protocol.effectObject(target,pair[0]);var r=new Protocol.Reader(p);
   assertEquals(7,p.length);assertEquals(0x37,r.c());assertEquals(target,r.d());assertEquals(pair[1],r.h());
  }
 }
 @Test void existingNativeEffectsKeepTheirIdentifiers() {
  for(int gfx:new int[]{66,744,755,759,829,830,831}){
   var r=new Protocol.Reader(Protocol.effectObject(11554,gfx));r.c();r.d();assertEquals(gfx,r.h());
  }
 }
 @Test void attackEffectsPreserveCombatFieldsAndUseNativeSprites() {
  for(int[] pair:new int[][]{{11736,10},{11738,1809},{11746,1815},{11748,1815},{66,66}}){
   byte[] p=Protocol.attackSpell(18,11554,0x60000003L,10,3,4500001,pair[0],32729,32795,32730,32797);
   var r=new Protocol.Reader(p);assertEquals(29,p.length);assertEquals(0x23,r.c());assertEquals(18,r.c());
   assertEquals(11554,r.d());assertEquals(0x60000003L,r.d());assertEquals(10,r.c());assertEquals(3,r.c());assertEquals(4500001,r.d());
   assertEquals(pair[1],r.h());assertEquals(0,r.c());assertEquals(32729,r.h());assertEquals(32795,r.h());assertEquals(32730,r.h());assertEquals(32797,r.h());assertEquals(0,r.c());assertEquals("",r.s());assertEquals(0,r.remaining());
  }
 }
}
