package local.lito381.server;
import java.util.*;
public final class PacketAdoptionGoldenMain {
 static int positives,guards;
 static void golden(String name,byte[] bytes,String expected){
  String actual=HexFormat.of().formatHex(bytes);if(!actual.equals(expected))throw new AssertionError(name+" actual="+actual+" expected="+expected);
  positives++;System.out.println("{\"name\":\""+name+"\",\"payload_hex\":\""+actual+"\",\"bytes\":"+bytes.length+"}");
 }
 static void rejected(String name,Runnable run){try{run.run();throw new AssertionError(name+" accepted invalid producer input");}catch(IllegalArgumentException expected){guards++;System.out.println("{\"guard\":\""+name+"\",\"result\":\"IllegalArgumentException\"}");}}
 public static void main(String[] args){
  var a=Protocol.LitoVsServerAvailability.AVAILABLE;var u=Protocol.LitoVsServerAvailability.UNAVAILABLE;
  golden("F3-empty",Protocol.vsServerStatus(),"f300");golden("F3-available",Protocol.vsServerStatus(a),"f30100000000");golden("F3-unavailable",Protocol.vsServerStatus(u),"f301ffffffff");golden("F3-prefix-mixed",Protocol.vsServerStatus(a,u,a),"f30300000000ffffffff00000000");
  var ten=new Protocol.LitoVsServerAvailability[]{a,u,a,u,a,u,a,u,a,u};golden("F3-ten-producer-limit",Protocol.vsServerStatus(ten),"f30a00000000ffffffff00000000ffffffff00000000ffffffff00000000ffffffff00000000ffffffff");
  long[] words={0x0100007fL,0x0200007fL,0x04030201L};String[] octets={"7f000001","7f000002","01020304"};int[] ports={1,2000,65535};String[] portBytes={"0100","d007","ffff"};
  for(int ip=0;ip<words.length;ip++)for(int p=0;p<ports.length;p++)for(int index:new int[]{0,9})golden("F4-ip"+ip+"-port"+ports[p]+"-index"+index,Protocol.litoVsServerIp(index,words[ip],ports[p]),"f4"+(index==0?"00":"09")+octets[ip]+portBytes[p]);
  golden("F4-high-ip-bit",Protocol.litoVsServerIp(0,0xffffffffL,2000),"f400ffffffffd007");
  rejected("F3-count11",()->Protocol.vsServerStatus(new Protocol.LitoVsServerAvailability[11]));rejected("F3-null-array",()->Protocol.vsServerStatus((Protocol.LitoVsServerAvailability[])null));rejected("F3-null-entry",()->Protocol.vsServerStatus(new Protocol.LitoVsServerAvailability[]{null}));
  rejected("F4-negative-index",()->Protocol.litoVsServerIp(-1,words[0],2000));rejected("F4-index10",()->Protocol.litoVsServerIp(10,words[0],2000));rejected("F4-index128",()->Protocol.litoVsServerIp(128,words[0],2000));rejected("F4-negative-ip",()->Protocol.litoVsServerIp(0,-1,2000));rejected("F4-ip-overflow",()->Protocol.litoVsServerIp(0,0x100000000L,2000));rejected("F4-port0",()->Protocol.litoVsServerIp(0,words[0],0));rejected("F4-port65536",()->Protocol.litoVsServerIp(0,words[0],65536));
  System.out.println("{\"summary\":true,\"positive_golden_vectors\":"+positives+",\"producer_guards\":"+guards+",\"network_or_SQL\":false,\"Protocol_codeSource\":\""+Protocol.class.getProtectionDomain().getCodeSource().getLocation().toString().replace("\\","\\\\")+"\"}");
 }
}
