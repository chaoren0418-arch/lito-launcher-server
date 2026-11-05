// ported from candidate 50635f7e; B07 三方移植（base-0.6.71 / candidate / src/main）。
package local.lito381.server;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.ConcurrentHashMap;

/** Cached unbound handles only: no retained session, target or cast state. */
final class CombatAccess {
    private static final ClassValue<ConcurrentHashMap<String,MethodHandle>> HANDLES=new ClassValue<>() {
        protected ConcurrentHashMap<String,MethodHandle> computeValue(Class<?> type) {return new ConcurrentHashMap<>();}
    };
    private static final MethodHandle MISSING=MethodHandles.dropArguments(
            MethodHandles.constant(Object.class,null),0,Object.class,Object[].class);
    private static final Object[] EMPTY=new Object[0];
    static Object get(Object object,String name) {return object==null?null:invoke(object,"get:"+name,name,null,EMPTY,true);}
    static Object field(Object object,String name) {return object==null?null:invoke(object,"field:"+name,name,null,EMPTY,false);}
    static void set(Object object,String name,Object value) {
        if(object==null)throw new IllegalArgumentException("missing combat field owner");
        var handle=HANDLES.get(object.getClass()).computeIfAbsent("set:"+name,key->setter(object.getClass(),name));
        try {Object ignored=(Object)handle.invokeExact(object,new Object[]{value});}
        catch(Throwable failure){throw new IllegalStateException("combat field update "+name+" failed",failure);}
        NativeSourceIcons1Epoch.reflected(object,name);
    }
    private static MethodHandle setter(Class<?> type,String name) {
        for(Class<?> owner=type;owner!=null;owner=owner.getSuperclass())try {
            var field=owner.getDeclaredField(name);return MethodHandles.privateLookupIn(owner,MethodHandles.lookup()).unreflectSetter(field)
                    .asSpreader(Object[].class,1).asType(MethodType.methodType(Object.class,Object.class,Object[].class));
        }catch(NoSuchFieldException absent){}catch(IllegalAccessException failure){throw new IllegalStateException("cannot update combat contract "+name,failure);}
        throw new IllegalStateException("missing combat field "+name);
    }
    static Object call(Object object,String name,String descriptor,Object... args) {
        if(object==null)throw new IllegalArgumentException("missing combat receiver for "+name);
        return invoke(object,name+descriptor,name,descriptor,args,false);
    }
    private static Object invoke(Object object,String key,String name,String descriptor,Object[] args,boolean optional) {
        Class<?> type=object.getClass();
        var handle=HANDLES.get(type).computeIfAbsent(key,k->find(type,name,descriptor,key.startsWith("field:"),optional));
        try {return (Object)handle.invokeExact(object,args);}
        catch(Throwable failure) {throw new IllegalStateException("combat accessor "+type.getSimpleName()+"."+name+" failed",failure);}
    }
    private static MethodHandle find(Class<?> type,String name,String descriptor,boolean field,boolean optional) {
        Class<?>[] parameters=descriptor==null?new Class<?>[0]
                :MethodType.fromMethodDescriptorString(descriptor,type.getClassLoader()).parameterArray();
        for(Class<?> owner=type;owner!=null;owner=owner.getSuperclass())try {
            MethodHandle handle;
            if(field) {
                var member=owner.getDeclaredField(name);
                handle=MethodHandles.privateLookupIn(owner,MethodHandles.lookup()).unreflectGetter(member);
            } else {
                var member=owner.getDeclaredMethod(name,parameters);
                handle=MethodHandles.privateLookupIn(owner,MethodHandles.lookup()).unreflect(member);
                if(java.lang.reflect.Modifier.isStatic(member.getModifiers()))handle=MethodHandles.dropArguments(handle,0,Object.class);
            }
            if(field)handle=MethodHandles.dropArguments(handle,1,Object[].class);
            else handle=handle.asSpreader(Object[].class,parameters.length);
            return handle.asType(MethodType.methodType(Object.class,Object.class,Object[].class));
        } catch(NoSuchMethodException|NoSuchFieldException absent) { }
        catch(IllegalAccessException failure) {throw new IllegalStateException("cannot access combat contract "+type.getName()+"."+name,failure);}
        if(optional)return MISSING;
        throw new IllegalStateException("missing combat contract "+type.getName()+"."+name+descriptor);
    }
    static int number(Object object,String name) {
        Object value=get(object,name);
        if(!(value instanceof Number n))throw new IllegalStateException("missing numeric combat contract "+name);
        return n.intValue();
    }
    static int integerCall(Object object,String name,String descriptor,Object... args) {
        return ((Number)call(object,name,descriptor,args)).intValue();
    }
}
