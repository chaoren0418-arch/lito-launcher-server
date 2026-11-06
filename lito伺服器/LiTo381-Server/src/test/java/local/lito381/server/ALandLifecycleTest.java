package local.lito381.server;

import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.lang.invoke.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static local.lito381.server.ALandInstanceStateTest.*;
import static org.junit.jupiter.api.Assertions.*;

class ALandLifecycleTest {
    @Test void leaveInstallFailureReleasesStatusMonitor() throws Exception {
        for(boolean npc:List.of(false,true)) checkLeaveFailure(npc);
    }
    private void checkLeaveFailure(boolean npc) throws Exception {
        var status=new BattleTimedStatus();
        var pc=allocate(ClientSession.class);
        set(pc,"timedStatus",status);
        set(pc,"characters",List.of());
        set(pc,"account","a-land");
        set(pc,"selectedCharacter","a-land");
        set(pc,"selectedClassicServerName","a-land-source");
        status.activatePoison(1L,60,1); // occupied 分支保證進入且退出同一把 status monitor。
        Field rules=CombatRules.class.getDeclaredField("current"); rules.setAccessible(true);
        Object oldRules=rules.get(null);
        Field installing=NativeCombatOrigin.class.getDeclaredField("installing"); installing.setAccessible(true);
        Object oldInstalling=installing.get(null);
        Field nativeState=NativeCombatOrigin.class.getDeclaredField("NPC_STATE"); nativeState.setAccessible(true);
        Object oldNativeState=nativeState.get(null);
        var failure=new IllegalStateException("injected leaveInstall failure");
        AtomicInteger leaveCalls=new AtomicInteger();
        ThreadLocal<Object> fault=new ThreadLocal<>() {
            @Override public Object get() {
                if (super.get()!=null && StackWalker.getInstance().walk(frames -> frames.anyMatch(
                        f -> f.getClassName().equals(NativeCombatOrigin.class.getName()) && f.getMethodName().equals("leaveInstall")))) {
                    leaveCalls.incrementAndGet();
                    assertFalse(Thread.holdsLock(status),"leaveInstall 仍位於 status monitor 內");
                    throw failure;
                }
                return super.get();
            }
        };
        var executor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"a-land-monitor-check");t.setDaemon(true);return t;});
        try {
            rules.set(null,new CombatRules.Snapshot(Map.of(),null,null));
            replaceStaticFinal(installing,fault);
            // B-land 尚未加入 ClientSession.sourceNpcPoisonState；只替換此邊界，交易與清理使用 Maven 類別。
            replaceStaticFinal(nativeState,MethodHandles.lookup().findStatic(ALandLifecycleTest.class,"npcState",
                    MethodType.methodType(CombatPoisonAtomic.NpcState.class,ClientSession.class,long.class,int.class,int.class,int.class,long.class,String.class)));
            NativeCombatAttacker.Ingress ingress=NativeCombatAttacker.enterPoison();
            npcCalls.set(0);
            try {
                assertSame(failure,assertThrows(IllegalStateException.class,()->{
                    if(npc) CombatPoisonAtomic.npc(pc,7L,1,1,0,1L,"test");
                    else CombatPoisonAtomic.player(status,1L,60,1,pc,null);
                }));
                assertEquals(npc?1:0,npcCalls.get());
            } finally {NativeCombatAttacker.leave(ingress);}
            assertEquals(2,leaveCalls.get(),"正常出口及 catch 清理都必須嘗試 leaveInstall");
            assertTrue(executor.submit(()->{synchronized(status){return true;}}).get(2,TimeUnit.SECONDS));
        } finally {
            replaceStaticFinal(installing,oldInstalling);
            replaceStaticFinal(nativeState,oldNativeState);
            rules.set(null,oldRules);
            fault.remove();
            executor.shutdownNow();
        }
    }

    private static final AtomicInteger npcCalls=new AtomicInteger();
    private static CombatPoisonAtomic.NpcState npcState(ClientSession pc,long object,int npc,int poison,int paralysis,long now,String source) {
        assertTrue(Thread.holdsLock(CombatPoisonAtomic.status(pc)));
        npcCalls.incrementAndGet();
        return new CombatPoisonAtomic.NpcState(ClassicWorldMonsterPoisonAttack.Kind.NONE,0,false,true,false);
    }

    private static void replaceStaticFinal(Field field,Object value) throws Exception {
        Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        var unsafe=(sun.misc.Unsafe)f.get(null);
        unsafe.putObjectVolatile(unsafe.staticFieldBase(field),unsafe.staticFieldOffset(field),value);
    }

    @Test void bothDeletionPathsRetireChatBeforeCallingRemainingServices() throws Exception {
        assertTrue(Modifier.isPrivate(NativeDeNpcConstructionState.class.getDeclaredMethod("cancelNpcChatTimer$own").getModifiers()));
        assertThrows(NoSuchMethodException.class,()->NativeDeNpcConstructionState.class.getDeclaredMethod("cancelNpcChatTimer"));
        for(boolean abort:List.of(false,true)) {
            List<String> calls=new ArrayList<>();
            var context=allocate(NativeDeWorldContext.class);
            var life=allocate(NativePlayerLifetime.class);
            set(life,"admission",new Object());
            set(life,"pending",new AtomicLong(1));
            set(context,"life",life);
            var chat=allocate(NativeDeChatJobs.Owner.class);
            var visibility=allocate(NativeDeVisibilityRuntime.Owner.class);
            set(visibility,"context",context);
            set(chat,"context",context); set(chat,"visibility",visibility);
            set(chat,"tickets",ConcurrentHashMap.<NativeDeChatJobs.Ticket>newKeySet());
            set(context,NativeDeChatJobs.FIELD,chat);
            var reached=new IllegalStateException("remaining.cancelChat reached");
            var remaining=ALandItemDefinitionsTest.proxy(NativeDeDeletionPorts.Services.class,(p,m,a)->{
                calls.add(m.getName());
                if(m.getName().equals("cancelChat")) {
                    assertTrue(chat.tickets.isEmpty(),"retire 必須先取消 ticket");
                    assertEquals(0,life.pending.get());
                    throw reached; // 在測試邊界結束，不執行無關的世界登錄清理。
                }
                if(m.getName().equals("timer") || m.getName().equals("onDeleteClaimed")) return null;
                throw new AssertionError("unexpected service: "+m);
            });
            set(context,"remaining",remaining);
            var boot=construction(context,calls);
            var npc=new NativeDeNpcConstructionState(78,boot.effects(),boot.owner(),boot.perception(),boot.template(),boot.npc(),null);
            set(npc,"template",new NativeDeNpcTemplate());
            var entry=allocate(NativeDeWorldContext.Entry.class); set(entry,"context",context);
            set(npc,NativeDeWorldContext.ENTRY,entry);
            var credit=allocate(NativeDeVisibilityJobs.Credit.class);
            set(credit,"owner",visibility); set(credit,"refs",1);
            var ticket=new NativeDeChatJobs.Ticket(chat,npc,()->{},credit,true,0);
            chat.tickets.add(ticket);
            assertSame(reached,assertThrows(IllegalStateException.class,()->{
                if(abort) npc.abortFailedWorldRegistration();
                else NativeDeDeletionScope.run(boot.owner(),npc::sourceDeleteNpc);
            }));
            assertTrue(ticket.isCancelled());
            assertTrue(ticket.isDone());
            assertEquals(1,calls.stream().filter("cancelChat"::equals).count());
            assertTrue(npc.destroyed());
            assertTrue(calls.stream().anyMatch("timer"::equals));
        }
    }
}
