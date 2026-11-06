package local.lito381.server;

import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import static org.junit.jupiter.api.Assertions.*;

class ALandInstanceStateTest {
    /** 只略過持有執行緒／排程的 owner 建立；受測 NPC 的完整建構鏈照常執行。 */
    static <T> T allocate(Class<T> type) {
        try {
            Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return type.cast(((sun.misc.Unsafe) field.get(null)).allocateInstance(type));
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    static void set(Object target, String name, Object value) {
        try { field(target.getClass(), name).set(target, value); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    static Object get(Object target, String name) {
        try { return field(target.getClass(), name).get(target); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> c=type; c!=null; c=c.getSuperclass()) {
            try { Field f=c.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
    static <T> T ports(Class<T> type, List<String> calls) {
        return ALandItemDefinitionsTest.proxy(type, (p, m, a) -> {
            calls.add(type.getSimpleName()+"."+m.getName());
            Class<?> result=m.getReturnType();
            if (result==void.class) return null;
            if (result==boolean.class) return false;
            if (result==int.class) return 0;
            if (result==long.class) return 0L;
            if (result==short.class) return (short)0;
            if (result==ArrayList.class) return new ArrayList<>();
            if (result==ReentrantLock.class) return new ReentrantLock();
            if (result==NativeDeInitializerKernel.Name[].class) return new NativeDeInitializerKernel.Name[0];
            if (result.isInterface()) return ports(result,calls);
            return null;
        });
    }
    static NativeDeDeletionOwner owner(NativeDeDeletionPorts.Services services) {
        var owner=allocate(NativeDeDeletionOwner.class);
        set(owner,"services",services);
        set(owner,"clock",(java.util.function.LongSupplier)()->123L);
        set(owner,"timer",new NativeDeCorpseTimers.Owner(null,owner,()->123L));
        set(owner,"inFlight",new ConcurrentHashMap<>());
        for (String f:List.of("duplicate","verifyFailure","lateViewer","stall","auditFailure")) set(owner,f,new AtomicLong());
        return owner;
    }
    static NativeDeInstancePorts.Construction construction(NativeDeDeletionPorts.Services services, List<String> calls) {
        return new NativeDeInstancePorts.Construction(78,NativeDeResurrectionEffects.UNBOUND,owner(services),
                ports(NativeDePerceptionPorts.Services.class,calls), ports(NativeDeNpcTemplatePorts.Services.class,calls),
                ports(NativeDeNpcConstructionPorts.Services.class,calls), ports(NativeDeInitializerKernel.Services.class,calls),
                ports(NativeDeInstancePorts.Scene.class,calls));
    }

    @Test void nameFailurePrecedesEverySuperclassEffectForAllOverloads() {
        List<String> calls=new ArrayList<>();
        var boot=construction(ports(NativeDeDeletionPorts.Services.class,calls),calls);
        var name=new NativeDeInitializerKernel.Name(1,"name",0,0,0,0,"","");
        // null template 以外也測試：若錯誤地先進父鏈，模板會立刻觸發觀測回呼。
        NativeDeNpcTemplate template=new NativeDeNpcTemplate();
        for(int overload=1;overload<=4;overload++) {
            final int selected=overload;
            calls.clear();
            Observed.callbacks.clear();
            assertEquals("source mutable names require a concrete World",
                    assertThrows(IllegalArgumentException.class,()->create(selected,boot,template,name,true,true,"1")).getMessage());
            assertTrue(calls.isEmpty(),calls.toString());
            assertTrue(Observed.callbacks.isEmpty(),Observed.callbacks.toString());
            assertTrue(boot.owner().inFlight.isEmpty());
            assertNull(boot.owner().timer.ticker.get());
        }
    }

    @Test void constructorsPublishConstructionAfterParentCallbacksAndKeepFlags() {
        for(int overload=0;overload<=4;overload++) for(boolean morph:List.of(false,true))
            for(boolean doll:List.of(false,true)) for(String restrict:Arrays.asList(null,"","  ","1","invalid")) {
                List<String> calls=new ArrayList<>();
                var boot=construction(ports(NativeDeDeletionPorts.Services.class,calls),calls);
                // 空名稱是正式初始化器的明確拒絕邊界，仍須完成建構與 startNpc 的 level/int 下限。
                var name=new NativeDeInstanceName(1,"",0,0,0,0,"","");
                Observed.callbacks.clear();
                var npc=create(overload,boot,null,name,morph,doll,restrict);
                assertSame(boot,npc.construction);
                assertFalse(Observed.callbacks.isEmpty());
                assertTrue(Observed.callbacks.stream().allMatch(Objects::isNull),"父鏈回呼看到提前賦值");
                assertEquals(overload>=2 && morph,npc.isMorphEnable());
                assertEquals(overload==4 && doll,get(npc,"_isDollEnable"));
                assertEquals(overload>=3?restrict:null,get(npc,"_classRestrict"));
                if(overload>0) { assertSame(name,npc.get_deName()); assertEquals(1,npc.getLevel()); assertEquals(10,npc.getInt()); }
                npc.set_deName(name);
                assertSame(name,npc.get_deName());
                npc.broadcastPacketAll((NativeDeInitializerKernel.Packet)null);
                assertTrue(calls.contains("Scene.publish"));
            }
    }

    @Test void concreteWorldResolvesNameBeforeParentAndRejectsClosedWorld() {
        List<String> calls=new ArrayList<>();
        var context=allocate(NativeDeWorldContext.class);
        set(context,"admission",new ReentrantReadWriteLock());
        var boot=construction(context,calls);
        var name=new NativeDeInitializerKernel.Name(1,"",0,0,0,0,"","");
        for(int overload=1;overload<=4;overload++) {
            Observed.callbacks.clear();
            var npc=create(overload,boot,null,name,false,false,null);
            assertInstanceOf(NativeDeInstanceName.class,npc.get_deName());
            assertSame(NativeDeInstanceNames.name(boot,name),npc.get_deName());
            assertSame(boot,npc.construction);
        }
        context.closed=true;
        for(int overload=1;overload<=4;overload++) {
            final int selected=overload;
            Observed.callbacks.clear();
            assertThrows(IllegalStateException.class,()->create(selected,boot,null,name,false,false,null));
            assertTrue(Observed.callbacks.isEmpty());
        }
    }

    @Test void validNamesCompleteInitializationWithConstructionVisibleToLaterCallbacks() {
        for(int overload=1;overload<=4;overload++) for(boolean morph:List.of(false,true))
            for(boolean doll:List.of(false,true)) for(String restrict:Arrays.asList(null,"","0","invalid")) {
                List<String> calls=new ArrayList<>();
                var boot=construction(ports(NativeDeDeletionPorts.Services.class,calls),calls);
                var name=new NativeDeInstanceName(1,"A-land",0,0,0,4,"title","");
                Observed.callbacks.clear();
                var npc=create(overload,boot,null,name,morph,doll,restrict);
                assertFalse(npc.isInitFailed(),calls.toString());
                assertSame(name,npc.get_deName());
                assertEquals("A-land",npc.getNameId());
                assertNull(Observed.callbacks.get(0));
                assertSame(boot,Observed.callbacks.get(Observed.callbacks.size()-1));
                assertTrue(calls.contains("Scene.publish"));
                assertEquals(1,npc.getLevel());
                assertEquals(10,npc.getInt());
            }
    }

    private static Observed create(int overload,NativeDeInstancePorts.Construction c,NativeDeNpcTemplate t,
                                   NativeDeInitializerKernel.Name n,boolean a,boolean b,String s) {
        return switch(overload) { case 0->new Observed(c,t); case 1->new Observed(c,t,n);
            case 2->new Observed(c,t,n,a); case 3->new Observed(c,t,n,a,s); default->new Observed(c,t,n,a,b,s); };
    }
    static class Observed extends NativeDeInstanceState {
        static final List<Object> callbacks=new ArrayList<>();
        Observed(NativeDeInstancePorts.Construction c,NativeDeNpcTemplate t){super(c,t);}
        Observed(NativeDeInstancePorts.Construction c,NativeDeNpcTemplate t,NativeDeInitializerKernel.Name n){super(c,t,n);}
        Observed(NativeDeInstancePorts.Construction c,NativeDeNpcTemplate t,NativeDeInitializerKernel.Name n,boolean a){super(c,t,n,a);}
        Observed(NativeDeInstancePorts.Construction c,NativeDeNpcTemplate t,NativeDeInitializerKernel.Name n,boolean a,String s){super(c,t,n,a,s);}
        Observed(NativeDeInstancePorts.Construction c,NativeDeNpcTemplate t,NativeDeInitializerKernel.Name n,boolean a,boolean b,String s){super(c,t,n,a,b,s);}
        @Override public void setNameId(String name){callbacks.add(construction);super.setNameId(name);}
        @Override public void setting_template(NativeDeNpcTemplate template){callbacks.add(construction);super.setting_template(template);}
    }
}
