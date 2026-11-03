package local.lito381.server;
import java.util.*;import java.nio.file.*;
/** 純封包契約：含舊指定目標格式恰與新簽章相撞的反例。 */
public final class SelectorOnlyCtrlProbe {
 static int checks;
 static void need(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 static byte[] forced(byte[] body){byte[] out=Arrays.copyOf(body,body.length+8);System.arraycopy(new byte[]{0x4c,0x46,0x43,0x31,body[0],1,0x67,(byte)0xa5},0,out,body.length,8);return out;}
 public static void main(String[]a)throws Exception{
  for(int skill=1;skill<=220;skill++){byte[] body={(byte)Protocol.C_USE_SPELL,(byte)((skill-1)>>3),(byte)((skill-1)&7)};var raw=forced(body);var out=ForcedCombat.unwrap(raw);need(skill==161?Arrays.equals(out,body)&&out!=raw:out==raw,"only the reviewed selector-only layout changes="+skill);need(ForcedCombat.target(skill==161?out:body)==0,"selector-only data never invents a forced target="+skill);}
  for(int skill:new int[]{27,36,41,44,66,68,76,145,157,167}){byte[] target=new Protocol.Writer().c(Protocol.C_USE_SPELL).c((skill-1)>>3).c((skill-1)&7).d(0x3143464cL).h(0x0114).h(0xa567).bytes();need(ForcedCombat.unwrap(target)==target,"a valid targeted layout with an identical suffix stays targeted="+skill);var raw=forced(target);need(Arrays.equals(ForcedCombat.unwrap(raw),ForcedCombat.nativeForestOriginalUnwrap(raw)),"existing targeted Ctrl is byte-identical="+skill);}
  byte[] area={(byte)Protocol.C_USE_SPELL,20,0};for(int at=7;at<11;at++){var bad=forced(area);bad[at]^=2;boolean rejected=false;try{ForcedCombat.unwrap(bad);}catch(IllegalArgumentException expected){rejected=true;}need(rejected,"corrupt declared extension rejected="+at);}
  need(ForcedCombat.unwrap(area)==area,"ordinary selector remains exact original object");need(Protocol.selectorOnlySpellRequest(ForcedCombat.unwrap(forced(area))).skillId()==161,"actual protocol selector accepts the unwrapped area request");
  Files.writeString(Path.of(a[0]),"{\"cases\":236,\"checks\":"+checks+",\"targeted_signature_collision_cases\":10,\"full_goal_achieved\":false}\n");
 }
}
