package local.lito381.server;
import java.util.Arrays;
/** Source SkillHaste carries the requested writeH duration for self and every broadcast viewer. */
final class NativeSourceDollSlowWire {
 static byte[] packet(long id,Protocol.SpeedMode mode,int originalSeconds,int duration){return Protocol.speed(id,mode,mode==Protocol.SpeedMode.SLOW?duration&65535:originalSeconds);}
 static boolean allowFrame(ClientSession receiver,byte[] packet){
  var c=NativeAreaMaterialAction.current.get();
  if(c==null||!(c.nativeSourceDollOutput instanceof NativeSourceDollOutput.Output out)||out.kind()!=2)return NativeSourceDollSlow.nativeSourceDollSlowWireOriginalFrame(receiver,packet);
  return receiver==out.receiver().pc()&&packet!=null&&packet.length==8&&(packet[0]&255)==Protocol.S_SPEED&&NativeSourceDollOutput.current(out)&&packet[5]==(byte)out.proof().speed().mode()&&Arrays.equals(packet,out.packet());
 }
 private NativeSourceDollSlowWire(){}
}
