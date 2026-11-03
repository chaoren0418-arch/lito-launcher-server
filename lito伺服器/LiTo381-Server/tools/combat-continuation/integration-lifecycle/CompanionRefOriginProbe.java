import java.nio.file.*;import java.util.*;
/** Class loading only, without initializing the server or starting any runtime owner. */
public final class CompanionRefOriginProbe {
 public static void main(String[] args)throws Exception{
  var jar=Path.of(args[0]).toRealPath();int classes=0;
  for(var name:List.of("ClientSession","GameHub","NativeCombatAttacker","NativeCombatAttacker$Ref","NativeCompanionRefScene","NativePlayerCommit","NativePlayerCommit$Binding","NativePlayerLifetime","NativePairedCastSafety","NativeCompanionPlacement","NativeCompanionPlacement$State","NativeCompanionPlacement$Scene","NativeCompanionRoleIdentity","NativeCompanionRoleIdentity$Role","NativeCompanionRoleIdentity$Epoch","NativeCompanionTargets","NativeCompanionTargets$Scope","NativeAreaMaterialAction","NativeAreaMaterialAction$Context","NativeRolePublication","NativeSourcePlayerActionLock","NativePlayerVitalCompletions","NativeSummonTerminalLifecycle")){
   var type=Class.forName("local.lito381.server."+name,false,CompanionRefOriginProbe.class.getClassLoader());if(!Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar))throw new AssertionError("gameplay class overlay "+name);classes++;
  }System.out.println("PASS "+classes+" gameplay classes from the exact immutable runtime; no initialization");
 }
}
