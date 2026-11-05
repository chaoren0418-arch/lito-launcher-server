package local.lito381.server;
/** Source movement admission belongs before the original map/collision/position/schedule body. */
final class NativeSourceNpcMovement {
 static boolean allowed(boolean original,Object actor){
  if(!original)return false;
  var proof=NativeSourceNpcFactoryType.stamp(actor);
  return proof==null||proof.nativeSourceNpcMoveInterval==null||proof.nativeSourceNpcMoveInterval>0;
 }
 private NativeSourceNpcMovement(){}
}
