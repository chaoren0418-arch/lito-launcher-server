package local.lito381.server;
import java.io.*;
/** Source cube skills-table visuals through the existing owned output and exact primary proof. */
final class NativeSourceCubePresentation {
 static NativeSourceCubeOrigin.Entity entity(Object proof){return proof instanceof NativeSourceCubeRemainder.Notice n?n.effect():proof instanceof NativeSourceCubeStatus.Emission m?NativeSourceCubeStatus.effect(m.origin()):null;}
 static NativeSourceCubeOrigin.Target target(Object proof){return proof instanceof NativeSourceCubeRemainder.Notice n?n.target():proof instanceof NativeSourceCubeStatus.Emission m?NativeSourceCubeStatus.target(m.origin()):null;}
 static Object actual(Object proof){if(proof instanceof NativeSourceCubeRemainder.Notice n)return n.actual();if(proof instanceof NativeSourceCubeStatus.Emission m)return m.origin() instanceof NativeSourceCubeOrigin.Lease l?l.actual():m.origin() instanceof NativeSourceCubeStatus.Link l?l.actual():null;return null;}
 static boolean expired(Object proof){return proof instanceof NativeSourceCubeRemainder.Notice n?n.expired():proof instanceof NativeSourceCubeStatus.Emission m&&m.expired();}
 static boolean source(Object proof){var e=entity(proof);return e!=null&&e.scene()!=null&&target(proof)!=null;}
 static int skill(int npc){return switch(npc){case 80149->205;case 80150->210;case 80151->215;case 80152->220;default->throw new IllegalArgumentException("not a Source cube effect");};}
 static boolean ally(Object proof){var t=target(proof);var e=entity(proof);var actual=actual(proof);if(t==null||e==null||actual==null||t.kind()!=0)return false;return e.npcId()==80152?CombatAccess.field(actual,"nativeSourceCubePresentationRelation")==BattleCubeHarmonyEffects.Relation.ALLY:String.valueOf(CombatAccess.field(actual,"kind")).equals("ALLY");}
 static int gfx(NativeSourceDollSkillData.Row row,boolean ally){return ally?row.gfx():row.secondGfx();}
 static byte[] packet(long target,int gfx){if(gfx<0||gfx>65535)throw new IllegalArgumentException("Source cube gfx out of native wire range");var packet=Protocol.effectObject(target,gfx==0?1:gfx);if(gfx==0){packet[packet.length-2]=0;packet[packet.length-1]=0;}return packet;}
 static byte[] naturalEffect(long id,int fallback,Object proof){if(!source(proof))return Protocol.effectObject(id,fallback);return packet(id,gfx(NativeSourceDollSkillData.row(skill(entity(proof).npcId())),ally(proof)));}
 static Object proof(){var c=NativeAreaMaterialAction.current.get();return c==null?null:c.nativeSourceCubeStatusProof;}
 static byte[] playerEffect(long id,int fallback,ClientSession pc)throws IOException{var proof=proof();if(!source(proof))return Protocol.effectObject(id,fallback);var packet=naturalEffect(id,fallback,proof);if(!expired(proof)&&ally(proof))pc.nativeSourceCubePresentationStatus(System.nanoTime(),"Source-cube-ally-before-effect");return packet;}
 static void status(ClientSession pc,long now,String reason)throws IOException{var proof=proof();if(!source(proof)||expired(proof)||!ally(proof))pc.nativeSourceCubePresentationStatus(now,reason);}
 /** Original queries retain their native clock policy outside a bound Source primary/secondary. */
 static boolean allowInstalled(Object origin){var e=NativeSourceCubeStatus.effect(origin);if(e==null||e.scene()==null)return true;Object cell=origin instanceof NativeSourceCubeOrigin.Lease l?l.actual():origin instanceof NativeSourceCubeStatus.Link l?l.actual():null;return cell!=null&&(long)CombatAccess.field(cell,"expiresAtNanos")>System.nanoTime();}
 private NativeSourceCubePresentation(){}
}
