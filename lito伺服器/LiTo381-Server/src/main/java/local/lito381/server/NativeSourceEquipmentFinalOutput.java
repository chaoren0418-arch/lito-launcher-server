package local.lito381.server;
import java.io.IOException;import java.lang.ref.WeakReference;import java.util.Arrays;
/** Existing proc output owns weak first-viewer metadata and uses the original final receiver monitor. */
final class NativeSourceEquipmentFinalOutput {
 record First(WeakReference<Object> state,boolean observer){}
 static First first(ClientSession pc){boolean observer=(boolean)CombatAccess.field(pc,"observer");return new First(new WeakReference<>(CombatAccess.field(pc,"timedStatus")),observer);}
 static NativeSourceEquipmentOutput.Viewer viewer(ClientSession pc,NativePlayerCommit.Binding binding,Object life,boolean laterObserver,First first){return new NativeSourceEquipmentOutput.Viewer(pc,binding,life,first.observer(),first.state());}
 static boolean viewerCurrent(NativeSourceEquipmentOutput.Viewer viewer){
  if(viewer==null||viewer.pc()==null)return false;
  if((boolean)CombatAccess.field(viewer.pc(),"observer")!=viewer.observer())return false;
  Object value=viewer.nativeSourceEquipmentViewerState;if(value==null)return true;
  if(!(value instanceof WeakReference<?> weak))return false;Object state=weak.get();
  return state!=null&&state==CombatAccess.field(viewer.pc(),"timedStatus")&&NativeSourceControlledDisplay.rawLife(viewer.pc())==viewer.life();
 }
 static Runnable task(NativeSourceEquipmentOutput.Output out){return()->send(out);}
 static void send(NativeSourceEquipmentOutput.Output out){
  var old=NativeAreaMaterialAction.current.get();NativeAreaMaterialAction.current.remove();
  try{out.viewer().pc().nativeSourceEquipmentFinalFrame(out.packet().clone(),out.note(),out);}
  catch(IOException|RuntimeException failure){NativeSourcePhysicalNpc.failed(out.proof().lifetime());}
  finally{if(old==null)NativeAreaMaterialAction.current.remove();else NativeAreaMaterialAction.current.set(old);}
 }
 static boolean allow(ClientSession pc,byte[] packet,Object value){return value instanceof NativeSourceEquipmentOutput.Output out&&out.viewer().pc()==pc&&packet!=null&&Arrays.equals(packet,out.packet())&&NativeSourceEquipmentOutput.current(out);}
 private NativeSourceEquipmentFinalOutput(){}
}
