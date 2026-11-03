package local.lito381.server;

import java.nio.file.Path;
import java.util.*;

/** Follow-up PC branch probe using the actual native ingress and original material transaction. */
public final class SourceWeaponPlayerFullPathPlan {
    static final java.util.concurrent.atomic.AtomicInteger cases=new java.util.concurrent.atomic.AtomicInteger();static int casts,frames,pairWaits;
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);cases.incrementAndGet();}
    static void put(Object o,String k,Object v){SourceWeaponFullPathPlan.put(o,k,v);}
    static ServerConfig config(int roll)throws Exception{
        var old=SourceWeaponFullPathPlan.config(roll);var parts=ServerConfig.class.getRecordComponents();
        var types=Arrays.stream(parts).map(java.lang.reflect.RecordComponent::getType).toArray(Class<?>[]::new);var values=new Object[parts.length];
        for(int i=0;i<parts.length;i++)values[i]=parts[i].getName().equals("weaponBreakFixtureDamageRoll")?3:parts[i].getAccessor().invoke(old);
        var ctor=ServerConfig.class.getDeclaredConstructor(types);ctor.setAccessible(true);return ctor.newInstance(values);
    }
    static final class CheckedBytes extends java.io.ByteArrayOutputStream{
        final ClientSession source,target;final GameHub hub;
        CheckedBytes(ClientSession a,ClientSession b,GameHub h){source=a;target=b;hub=h;}
        @Override public synchronized void write(byte[] bytes,int offset,int length){
            check(!source.nativePlayerActionLock().isHeldByCurrentThread()&&!target.nativePlayerActionLock().isHeldByCurrentThread()&&!Thread.holdsLock(hub),"actual original cipher/socket output occurs after source/target pair and Hub locks release");super.write(bytes,offset,length);
        }
    }
    static final class GateLock extends java.util.concurrent.locks.ReentrantLock{
        final java.util.concurrent.CountDownLatch tried=new java.util.concurrent.CountDownLatch(1),resume=new java.util.concurrent.CountDownLatch(1);
        boolean captured;
        @Override public void lock(){if(isLocked()&&!isHeldByCurrentThread()&&!captured){captured=true;var c=NativeAreaMaterialAction.current.get();check(NativeSourceWeaponTargetLife.marked(c)&&NativeSourceTargetNegativeLife.proof(c)!=null,"actual native blocking pair lock retains first Context/proof");tried.countDown();}super.lock();}
        @Override public boolean tryLock(){boolean locked=super.tryLock();if(!locked&&!captured){captured=true;var c=NativeAreaMaterialAction.current.get();check(NativeSourceWeaponTargetLife.marked(c)&&NativeSourceTargetNegativeLife.proof(c)!=null,"actual native pair wait retains its original first Context/proof");tried.countDown();try{if(!resume.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("pair release missing");}catch(InterruptedException e){throw new AssertionError(e);}}return locked;}
    }
    static SourceWeaponFullPathPlan.Fixture fixture(String mode)throws Exception{
        var f=SourceWeaponFullPathPlan.fixture();var b=f.base().first().base().base();
        Object room=((Map<?,?>)CombatAccess.field(b.hub(),"rooms")).get(1L);
        put(room,"questEncounter",new BattleQuestEncounter());put(room,"timeAttackEncounter",new BattleTimeAttackEncounter());
        put(b.caster(),"sourceCounterMagicBypassSkills",mode.equals("bypass")?Set.of(27):Set.of());put(b.caster(),"config",config(mode.equals("miss")?100:1));
        int durability=mode.equals("cap")?11:mode.equals("over-cap")?20:0;
        var inventory=mode.equals("no-weapon")?new RuntimeInventory():RuntimeInventory.fromDurableState(Map.of(24L,1),Map.of(24L,List.of(new RuntimeInventory.PersistentItem(99991L,1,6,durability,true,1,false,true))));
        put(b.target(),"inventory",inventory);
        if(!mode.equals("no-weapon"))check(inventory.equippedWeaponItem()!=null&&inventory.equippedWeaponItem().objectId()==99991L,"actual equipped runtime weapon retains exact object identity");
        var timed=(BattleTimedStatus)CombatAccess.field(b.target(),"timedStatus");
        if(Set.of("counter","bypass","miss","earth").contains(mode))timed.activateCounterMagic(System.nanoTime(),60);
        if(mode.equals("earth"))timed.activateEarthBind(System.nanoTime(),60);
        if(mode.equals("no-mp"))put(b.caster(),"playerMp",0);
        if(mode.equals("no-material"))put(b.caster(),"inventory",new RuntimeInventory());
        if(mode.equals("ready"))((Map<Integer,Long>)CombatAccess.field(b.caster(),"spellReadyNanos")).put(27,Long.MAX_VALUE);
        for(var pc:List.of(b.caster(),b.target())){var inv=(RuntimeInventory)CombatAccess.field(pc,"inventory");var state=(GameStateStore)CombatAccess.field(pc,"state");state.saveEconomyState((String)CombatAccess.call(pc,"stateCharacter","()Ljava/lang/String;"),0L,inv.catalogCounts(),inv.catalogItemStates());}
        for(var socket:f.sockets().values())put(socket,"bytes",new CheckedBytes(b.caster(),b.target(),b.hub()));
        return f;
    }
    static void execute(SourceWeaponFullPathPlan.Fixture f)throws Throwable{var b=f.base().first().base().base();NativeLightAction.dispatch(b.caster(),SourceTargetNegativeLifePlan.packet(27,b.target().playerObjectId()));}
    static void exercise(String mode)throws Throwable{
        var f=fixture(mode);var b=f.base().first().base().base();var timed=(BattleTimedStatus)CombatAccess.field(b.target(),"timedStatus");
        var before=(RuntimeInventory)CombatAccess.field(b.target(),"inventory");var weapon=before.equippedWeaponItem();int hp=(int)CombatAccess.field(b.target(),"playerHp");
        boolean admitted=!Set.of("no-mp","no-material","ready").contains(mode),success=admitted&&!mode.equals("miss");
        boolean counter=Set.of("counter","bypass","miss","earth").contains(mode);var source=SourceWeaponCounterMagicOracle.evaluate(success,counter,mode.equals("bypass"));
        boolean applied=success&&!source.blocked();
        try{
            execute(f);casts++;
            check(f.queries().get()==0,"PC target never looks up an unrelated NPC row");
            check((int)CombatAccess.field(b.caster(),"playerMp")== (mode.equals("no-mp")?0:admitted?71:100)
                &&((RuntimeInventory)CombatAccess.field(b.caster(),"inventory")).countCatalog(40318)==(mode.equals("no-material")?0:admitted?2:3),"actual PC native material/MP exactly once or original resource/reuse rejection");
            check(timed.counterMagicActive(System.nanoTime())==source.counterRemaining(),"actual PC Source27 dynamic Counter Magic exception retained: mode="+mode);
            var after=(RuntimeInventory)CombatAccess.field(b.target(),"inventory");
            check((int)CombatAccess.field(b.target(),"playerHp")==hp,"original weapon damage never changes target HP");
            if(weapon!=null){
                var current=after.equippedWeaponItem();int expected=applied?SourceWeaponDurabilityOracle.evaluate(weapon.durability(),weapon.enchantLevel(),3):weapon.durability();
                check(current!=null&&current.objectId()==weapon.objectId()&&current.durability()==expected,"full original target durability matches exact Source receiveDamage, including cap/observed over-cap "+mode);
                check(after.catalogCounts().equals(before.catalogCounts()),"target weapon quantity and catalog unchanged");
                var state=(GameStateStore)CombatAccess.field(b.target(),"state");String key=(String)CombatAccess.call(b.target(),"stateCharacter","()Ljava/lang/String;");
                check(state.economy(key).inventory().equals(after.catalogCounts())&&state.economy(key).itemStates().equals(after.catalogItemStates()),"nonpersistent original target economy matches actual damaged instance");
            }
            for(var receiver:List.of(b.caster(),b.target(),b.viewer(),b.observer())){
                var packets=SourceWeaponFullPathPlan.packets(f.sockets().get(receiver));frames+=packets.size();
                check(SourceWeaponFullPathPlan.count(packets,Protocol.effectObject(b.target().playerObjectId(),f.spell().effectGfx()))==(applied?1:0),"exact original PC weapon effect only after successful uncountered probability");
                check(SourceWeaponFullPathPlan.count(packets,Protocol.effectObject(b.target().playerObjectId(),BattleSpellCatalog.counterMagicSpell(31).consumeEffectGfx()))==(source.blocked()?1:0),"native PC counter effect follows exact Source consumption");
                if(receiver==b.target()&&weapon!=null){var current=after.equippedWeaponItem();check(SourceWeaponFullPathPlan.count(packets,Protocol.messageCode(268,current.definition().name()))==(applied?1:0),"original target weapon damage message once, including capped no-op");}
            }
        }finally{SourceWeaponFullPathPlan.cleanup(f);}
    }
    static void pairWait(String mode)throws Throwable{
        var f=fixture("normal");var b=f.base().first().base().base();var target=b.target();var original=target.nativePlayerActionLock();
        var field=Arrays.stream(ClientSession.class.getDeclaredFields()).filter(v->{try{v.setAccessible(true);return v.get(target)==original;}catch(Exception e){throw new AssertionError(e);}}).findFirst().orElseThrow();
        var gate=new GateLock();field.set(target,gate);var error=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        var worker=new Thread(()->{try{execute(f);}catch(Throwable t){error.set(t);}},"actual-PC-weapon-first-target-pair-"+mode);
        try{
            gate.lock();try{worker.start();check(gate.tried.await(3,java.util.concurrent.TimeUnit.SECONDS),"original PC weapon target pair actually waits");
                check(Arrays.stream(worker.getStackTrace()).anyMatch(x->x.getClassName().endsWith("NativeWeaponBreakMaterialAction")),"blocked pair belongs to full original PC material path");
                SourceTargetNegativeReservePlan.mutate(f.base(),mode);
            }finally{gate.unlock();gate.resume.countDown();}
            worker.join(3000);if(error.get()!=null)error.get().printStackTrace();check(!worker.isAlive()&&error.get()==null,"full PC pair worker terminates "+mode+" error="+error.get());
            var inventory=(RuntimeInventory)CombatAccess.field(target,"inventory");boolean allowed=mode.equals("normal");
            check((int)CombatAccess.field(b.caster(),"playerMp")== (allowed?71:100)&&((RuntimeInventory)CombatAccess.field(b.caster(),"inventory")).countCatalog(40318)==(allowed?2:3)&&inventory.equippedWeaponItem().durability()==(allowed?3:0),"stale first source/target/lifetime never charges resources or damages the old role weapon "+mode);
            check(allowed||f.sockets().values().stream().allMatch(socket->socket.bytes.size()==0),"rejected pair emits no memory socket frame "+mode);
            if(allowed)for(var socket:f.sockets().values())frames+=SourceWeaponFullPathPlan.packets(socket).size();
            SourceTargetNegativeReservePlan.noInit(f.base(),mode);pairWaits++;
        }finally{gate.resume.countDown();if(gate.isHeldByCurrentThread())gate.unlock();worker.join(3000);check(!worker.isAlive(),"native PC pair worker terminal before original lock restore");field.set(target,original);SourceWeaponFullPathPlan.cleanup(f);}
    }
    public static void main(String[]args)throws Throwable{
        var rules=CombatRules.class.getDeclaredField("current");rules.setAccessible(true);Object old=rules.get(null);
        var controls=NativeSourceOtherControls.class.getDeclaredField("current");controls.setAccessible(true);Object oldControls=controls.get(null);
        var database=CombatDatabase.class.getDeclaredField("current");database.setAccessible(true);Object oldDatabase=database.get(null);
        var frames=SourceNpcHpOutputPlan.frames();var oldFrame=frames.get();var area=NativeAreaMaterialAction.current;var oldArea=area.get();
        try{
            var path=Path.of("detached-weapon-player-no-SQL");
            rules.set(null,new CombatRules.Snapshot(Map.of("DragonKnightAwakeClassicMode",0D,"INVIS_CAST_BREAK",1D),path,path,path,Set.of(),path));
            var props=new Properties();props.setProperty("hpbar","true");
            controls.set(null,new NativeSourceOtherControls.Snapshot(NativeSourceDisplayPlan.Settings.read(props),path));
            BattleSpellCatalog.weaponBreakSpell(27);
            database.set(null,new CombatDatabase.Snapshot(Map.of(27,new CombatDatabase.Skill(32,0,3000,0,0,0,0,40318,1)),
                new SourceSkillControls.Snapshot(Map.of(),Map.of(),Map.of(),Map.of())));
            for(String mode:args.length==0?new String[]{"counter","bypass","normal","miss","earth","no-mp","no-material","ready","cap","over-cap","no-weapon"}:args)exercise(mode);if(args.length==0)for(String mode:List.of("normal","Main","STOPPED","lifetime","foreign-life","source-role","owner-role","source-status","owner-status","owner-life"))pairWait(mode);
        }finally{
            database.set(null,oldDatabase);controls.set(null,oldControls);rules.set(null,old);
            if(oldFrame==null)frames.remove();else frames.set(oldFrame);
            if(oldArea==null)area.remove();else area.set(oldArea);
        }
        System.out.println("PASS"+cases.get()+" PC weapon full material Source-counter/dynamic-exception/durability/cap/resources contracts; original-casts="+casts+", encrypted-memory-frames="+SourceWeaponPlayerFullPathPlan.frames+", original-pair-waits="+pairWaits+"; remaining paired output/source equipment/backend/Main/game unqualified");
    }
}
