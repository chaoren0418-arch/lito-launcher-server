package local.lito381.server;
/** Borrow the first material lifetime; the original Receipt still owns registration and release. */
final class NativeSourceTargetNegativeReserve {
 static boolean initial(ClientSession pc,NativeAreaMaterialAction.Context c,NativeSourceTargetNegativeLife.Proof p){return p!=null&&NativeSourceTargetNegativeLife.enabled(c)&&NativeSourceTargetNegativeLife.proof(c)==p&&pc==p.peer()&&NativeSourceTargetNegativeLife.current(c,p);}
 static boolean current(ClientSession pc,NativeAreaMaterialAction.Context c,NativeSourceTargetNegativeLife.Proof p,NativeServerLifetime app){return initial(pc,c,p)&&p.life().application==app;}
 static boolean processes(boolean original,ClientSession pc,NativeAreaMaterialAction.Context c,NativeSourceTargetNegativeLife.Proof p,NativeServerLifetime app){return original&&current(pc,c,p,app);}
 static NativePlayerLifetime lifetime(ClientSession pc,NativeAreaMaterialAction.Context c,NativeSourceTargetNegativeLife.Proof p){return p.life();}
 static NativePlayerVitalCompletions.Receipt reserve(ClientSession pc,NativeAreaMaterialAction.Context c){if(!NativeSourceTargetNegativeLife.enabled(c))return NativePlayerVitalCompletions.reserve(pc);var p=NativeSourceTargetNegativeLife.proof(c);return initial(pc,c,p)?NativePlayerVitalCompletions.nativeSourceTargetNegativeReserve(pc,c,p):null;}
 private NativeSourceTargetNegativeReserve(){}
}
