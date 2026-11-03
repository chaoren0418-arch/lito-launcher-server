package local.lito381.server;
import java.lang.invoke.*;import java.lang.ref.WeakReference;import java.util.*;
/** Per-status scalar publication versions. Class metadata caches retain no actors, status instances or callbacks. */
final class NativeSourceIcons1Epoch {
 static final String FIELD="nativeSourceIcons1Version";
 static final Map<Class<?>,Set<String>> FIELDS=fields();
 static final ClassValue<VarHandle> VERSIONS=new ClassValue<>(){protected VarHandle computeValue(Class<?> type){try{return MethodHandles.privateLookupIn(type,MethodHandles.lookup()).findVarHandle(type,FIELD,long.class);}catch(ReflectiveOperationException failure){throw new ExceptionInInitializerError(failure);}}};
 record Cell(String field,WeakReference<Object> status,long version){}
 static Map<Class<?>,Set<String>> fields(){var values=new HashMap<Class<?>,Set<String>>();try{for(var entry:NativeSourceIcons1Projection.ENTRIES){Class<?> type=ClientSession.class.getDeclaredField(entry.status()).getType();values.computeIfAbsent(type,k->new HashSet<>()).add(entry.expiry());}var timed=values.get(BattleTimedStatus.class);timed.addAll(List.of("braveExpiresAtNanos","braveMode","nativeSourceIcons1BraveSkill"));}catch(ReflectiveOperationException failure){throw new ExceptionInInitializerError(failure);}var result=new HashMap<Class<?>,Set<String>>();values.forEach((type,names)->result.put(type,Set.copyOf(names)));return Map.copyOf(result);}
 static void bump(Object status){VERSIONS.get(status.getClass()).getAndAdd(status,1L);}
 static void reflected(Object status,String field){if(status!=null){var names=FIELDS.get(status.getClass());if(names!=null&&names.contains(field))bump(status);}}
 static long version(Object status){return (long)VERSIONS.get(status.getClass()).getVolatile(status);}
 static List<Cell> capture(ClientSession pc){if(pc==null||!pc.nativePlayerActionLock().isHeldByCurrentThread())return null;var found=new LinkedHashMap<String,Cell>();for(var entry:NativeSourceIcons1Projection.ENTRIES)if(!found.containsKey(entry.status())){Object status=CombatAccess.field(pc,entry.status());if(status==null)return null;found.put(entry.status(),new Cell(entry.status(),new WeakReference<>(status),version(status)));}return List.copyOf(found.values());}
 static boolean current(ClientSession pc,List<Cell> cells){if(pc==null||cells==null)return false;for(var cell:cells){Object status=cell.status().get();if(status==null||CombatAccess.field(pc,cell.field())!=status||version(status)!=cell.version())return false;}return true;}
 private NativeSourceIcons1Epoch(){}
}
