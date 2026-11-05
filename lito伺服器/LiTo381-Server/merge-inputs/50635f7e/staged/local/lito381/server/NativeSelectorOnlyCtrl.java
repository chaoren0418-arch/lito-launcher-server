package local.lito381.server;
import java.util.Arrays;
/** 技能161的無目標Ctrl封包；其餘格式與敵我／指定目標權限沿用原核心。 */
final class NativeSelectorOnlyCtrl {
 static byte[] unwrap(byte[] raw){
  if(raw==null||raw.length!=11||(raw[0]&255)!=Protocol.C_USE_SPELL||(raw[1]&255)!=20||raw[2]!=0||raw[3]!=0x4c||raw[4]!=0x46||raw[5]!=0x43||raw[6]!=0x31)return ForcedCombat.nativeForestOriginalUnwrap(raw);
  if(raw[7]!=raw[0]||raw[8]!=1||raw[9]!=0x67||(raw[10]&255)!=0xa5)throw new IllegalArgumentException("invalid forced-combat extension");
  byte[] body=Arrays.copyOf(raw,3);Protocol.selectorOnlySpellRequest(body);return body;
 }
 private NativeSelectorOnlyCtrl(){}
}
