// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import local.lito381.server.NativeDeDarkItem;
import local.lito381.server.NativeDeItemAbility;
import local.lito381.server.NativeDeItemMetadata;
import local.lito381.server.NativeDeItemTemplate;
import local.lito381.server.NativeDeItemVipTemplate;
import local.lito381.server.NativeDeWorldKinds;

final class NativeDeItemStateCopy {
    static void copy(NativeDeWorldKinds.Item item, NativeDeWorldKinds.Item item2) {
        try {
            for (Field field : NativeDeWorldKinds.Item.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object object = field.get(item);
                if (object instanceof ScheduledFuture || field.getType().getSimpleName().equals("EquipmentTimerLease") && object != null) {
                    throw new IllegalStateException("active source item lifetime cannot be duplicated");
                }
                if ((!field.getName().equals("_magicStoneExpirySubmissionInFlight") && !field.getName().equals("_magicStoneExpiryRetryScheduled") || !Boolean.TRUE.equals(object)) && (!field.getName().equals("_pendingMagicStoneExpiryGeneration") || (Long)object < 0L)) continue;
                throw new IllegalStateException("pending source item expiry cannot be duplicated");
            }
            IdentityHashMap identityHashMap = new IdentityHashMap();
            identityHashMap.put(item, item2);
            NativeDeItemStateCopy.copyFields(item, item2, NativeDeWorldKinds.Item.class, identityHashMap);
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException("source item state copy failed", reflectiveOperationException);
        }
    }

    private static void copyFields(Object object, Object object2, Class<?> clazz, IdentityHashMap<Object, Object> identityHashMap) throws ReflectiveOperationException {
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
            field.setAccessible(true);
            Object object3 = field.get(object);
            Object object4 = field.get(object2);
            if (object3 instanceof Lock || field.getName().equals("_delayEffectStateLock")) continue;
            if (object3 instanceof AtomicLong) {
                AtomicLong atomicLong = (AtomicLong)object3;
                if (object4 instanceof AtomicLong) {
                    AtomicLong atomicLong2 = (AtomicLong)object4;
                    atomicLong2.set(atomicLong.get());
                    identityHashMap.put(atomicLong, atomicLong2);
                    continue;
                }
            }
            if (object3 instanceof AtomicInteger) {
                AtomicInteger atomicInteger = (AtomicInteger)object3;
                if (object4 instanceof AtomicInteger) {
                    AtomicInteger atomicInteger2 = (AtomicInteger)object4;
                    atomicInteger2.set(atomicInteger.get());
                    identityHashMap.put(atomicInteger, atomicInteger2);
                    continue;
                }
            }
            if (object3 instanceof AtomicReference) {
                AtomicReference atomicReference = (AtomicReference)object3;
                if (object4 instanceof AtomicReference) {
                    AtomicReference atomicReference2 = (AtomicReference)object4;
                    atomicReference2.set(NativeDeItemStateCopy.deep(atomicReference.get(), identityHashMap));
                    identityHashMap.put(atomicReference, atomicReference2);
                    continue;
                }
            }
            if (object3 instanceof NativeDeItemAbility) {
                NativeDeItemAbility nativeDeItemAbility = (NativeDeItemAbility)object3;
                if (object4 instanceof NativeDeItemAbility) {
                    NativeDeItemAbility nativeDeItemAbility2 = (NativeDeItemAbility)object4;
                    NativeDeItemStateCopy.copyAbility(nativeDeItemAbility, nativeDeItemAbility2);
                    identityHashMap.put(nativeDeItemAbility, nativeDeItemAbility2);
                    continue;
                }
            }
            if (object3 != null && object3.getClass().getName().equals("local.lito381.server.NativeDeWorldKinds$Item$LastStatus") && object4 != null && object4.getClass() == object3.getClass()) {
                identityHashMap.put(object3, object4);
                NativeDeItemStateCopy.copyFields(object3, object4, object3.getClass(), identityHashMap);
                continue;
            }
            if (object3 instanceof List) {
                List list = (List)object3;
                if (object4 instanceof List) {
                    List list2 = (List)object4;
                    list2.clear();
                    list2.addAll(list);
                    identityHashMap.put(object3, object4);
                    continue;
                }
            }
            field.set(object2, NativeDeItemStateCopy.deep(object3, identityHashMap));
        }
    }

    private static void copyAbility(NativeDeItemAbility nativeDeItemAbility, NativeDeItemAbility nativeDeItemAbility2) {
        nativeDeItemAbility2.clear();
        nativeDeItemAbility.getAll().forEach(nativeDeItemAbility2::set);
        nativeDeItemAbility2.setDebugContext(nativeDeItemAbility.getDebugContext());
    }

    private static Object deep(Object object, IdentityHashMap<Object, Object> identityHashMap) throws ReflectiveOperationException {
        Object object2;
        if (object == null || object instanceof String || object instanceof Number || object instanceof Boolean || object instanceof Character || object instanceof Enum || object instanceof NativeDeItemTemplate) {
            return object;
        }
        if (identityHashMap.containsKey(object)) {
            return identityHashMap.get(object);
        }
        if (object instanceof Timestamp) {
            Timestamp timestamp = (Timestamp)object;
            Timestamp timestamp2 = new Timestamp(timestamp.getTime());
            timestamp2.setNanos(timestamp.getNanos());
            object2 = timestamp2;
        } else if (object instanceof int[]) {
            int[] nArray = (int[])object;
            object2 = nArray.clone();
        } else if (object instanceof byte[]) {
            byte[] byArray = (byte[])object;
            object2 = byArray.clone();
        } else if (object instanceof NativeDeDarkItem) {
            NativeDeDarkItem nativeDeDarkItem = (NativeDeDarkItem)object;
            NativeDeDarkItem nativeDeDarkItem2 = new NativeDeDarkItem(nativeDeDarkItem.getItemObjId(), nativeDeDarkItem.getConfigId(), nativeDeDarkItem.getTitle());
            NativeDeItemStateCopy.copyAbility(nativeDeDarkItem.getAbility(), nativeDeDarkItem2.getAbility());
            object2 = nativeDeDarkItem2;
        } else if (object instanceof NativeDeItemVipTemplate) {
            NativeDeItemVipTemplate nativeDeItemVipTemplate = (NativeDeItemVipTemplate)object;
            NativeDeItemVipTemplate nativeDeItemVipTemplate2 = new NativeDeItemVipTemplate();
            nativeDeItemVipTemplate2.set_type(nativeDeItemVipTemplate.get_type());
            nativeDeItemVipTemplate2.set_effect_icon(nativeDeItemVipTemplate.get_effect_icon());
            nativeDeItemVipTemplate2.set_polyId(nativeDeItemVipTemplate.get_polyId());
            NativeDeItemStateCopy.copyAbility(nativeDeItemVipTemplate.getAbility(), nativeDeItemVipTemplate2.getAbility());
            object2 = nativeDeItemVipTemplate2;
        } else if (object instanceof NativeDeItemMetadata) {
            NativeDeItemMetadata nativeDeItemMetadata = (NativeDeItemMetadata)object;
            Field field = nativeDeItemMetadata.getClass().getDeclaredField("objectId");
            field.setAccessible(true);
            int n = field.getInt(nativeDeItemMetadata);
            object2 = new NativeDeItemMetadata(n, (NativeDeDarkItem)NativeDeItemStateCopy.deep(nativeDeItemMetadata.getDarkItem(n), identityHashMap));
        } else {
            throw new IllegalStateException("unqualified mutable source item copy field: " + object.getClass().getName());
        }
        identityHashMap.put(object, object2);
        return object2;
    }

    private NativeDeItemStateCopy() {
    }
}
