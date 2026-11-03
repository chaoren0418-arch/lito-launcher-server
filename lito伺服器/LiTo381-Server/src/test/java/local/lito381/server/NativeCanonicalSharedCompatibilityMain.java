package local.lito381.server;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Focused unchanged Shared publication/life tests using immutable fixture dependencies. */
public final class NativeCanonicalSharedCompatibilityMain {
    public static void main(String[] args)throws Exception {
        var output=Path.of(args[0]);var cases=new LinkedHashMap<String,Object>();
        var selected=Map.of("ModernBattleWorldProtocolTest",List.of("prepareLoadingBothAcksReadAndLeaveUseOnlyCanonicalLifecycle","failedPreparationIsAttachedAbortedWithoutInventedBattleData","mixedEquipmentHistoryCanReadAcknowledgeLoadAndLeaveWithoutLegacyProjection"),
                "ModernBattleOperationProtocolTest",List.of("mixedPriceHistorySurvivesActualNewWireAndDenial","staleLifeHasNoTypedAckAndCannotConsumeSequenceOrCrossOwner"),
                "ModernBattleRevivalProtocolTest",List.of("actualDeathRevivesOnceAndKeepsExactCanonicalLifeAndDeathLedger"));
        for(var entry:selected.entrySet())for(var name:entry.getValue()) {
            var type=Class.forName("local.lito381.server."+entry.getKey());var constructor=type.getDeclaredConstructor();constructor.setAccessible(true);var instance=constructor.newInstance();
            for(var field:type.getDeclaredFields())if(field.getType()==Path.class){field.setAccessible(true);field.set(instance,Files.createDirectory(output.resolve("shared-"+UUID.randomUUID().toString().replace("-",""))));}
            var method=type.getDeclaredMethod(name);method.setAccessible(true);try{method.invoke(instance);}catch(InvocationTargetException failure){throw new AssertionError(entry.getKey()+"."+name,failure.getCause());}
            cases.put(entry.getKey()+"."+name,"PASSED");
        }
        Files.writeString(output.resolve("canonical-shared-compatibility.json"),NativeTerrainProbeMain.json(Map.of("state","PASSED_FOCUSED_SHARED_COMPATIBILITY","cases",cases,"tests",cases.size(),"original_93_suite_rerun",false)));
        System.out.println("Focused Shared canonical lifecycle/operation/revival compatibility: "+cases.size()+" passed");
    }
}
