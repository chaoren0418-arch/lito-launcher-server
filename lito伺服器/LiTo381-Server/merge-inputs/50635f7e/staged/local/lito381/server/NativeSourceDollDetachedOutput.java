package local.lito381.server;
import java.io.IOException;
import java.util.Arrays;
/** The existing Batch receives only one output proof, never its producer Context. */
final class NativeSourceDollDetachedOutput {
 static NativeSelfCastAction.Task task(NativeAreaMaterialAction.Context unused,NativeSourceDollOutput.Output out,ClientSession receiver,ClientSession caster){
  if(receiver!=out.receiver().pc())throw new IllegalArgumentException("doll output receiver");
  return ()->send(out,caster);
 }
 static void send(NativeSourceDollOutput.Output out,ClientSession caster){
  try{out.receiver().pc().nativeSourceDollDetachedFinalFrame(out.packet().clone(),out.note(),out);}
  catch(IOException|RuntimeException failure){NativeSourceDollSkills.failed(caster);}
 }
 /** Called at the original doll guard, inside the complete original synchronized frame body. */
 static boolean allow(ClientSession receiver,byte[] packet,Object value){
  if(!(value instanceof NativeSourceDollOutput.Output out)||receiver!=out.receiver().pc()||packet==null||!Arrays.equals(packet,out.packet())||!NativeSourceDollOutput.current(out))return false;
  return switch(out.kind()){
   case 0->packet.length==7&&(packet[0]&255)==55;
   case 1->packet.length==6&&(packet[0]&255)==32;
   case 2->packet.length==8&&(packet[0]&255)==Protocol.S_SPEED&&packet[5]==(byte)out.proof().speed().mode();
   default->false;
  };
 }
 private NativeSourceDollDetachedOutput(){}
}
