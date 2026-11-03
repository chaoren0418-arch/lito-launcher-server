package local.lito381.server;
/** Armor Break's canonical WORLD state uses the same action lock as its paired producer. */
final class NativeArmorState {
 static boolean activate(ClientSession pc,long now,int seconds){NativeLightAction.enterState(pc);try{return pc.nativeArmorStateActivate(now,seconds);}finally{NativeLightAction.exitState(pc);}}
 static boolean active(ClientSession pc,long now){NativeLightAction.enterState(pc);try{return pc.nativeArmorStateActive(now);}finally{NativeLightAction.exitState(pc);}}
 static boolean cancel(ClientSession pc){NativeLightAction.enterState(pc);try{return pc.nativeArmorStateCancel();}finally{NativeLightAction.exitState(pc);}}
 private NativeArmorState(){}
}
