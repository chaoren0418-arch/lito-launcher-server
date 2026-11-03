package local.lito381.server;

import java.nio.file.Path;
import java.util.*;

/** Actual material27 source-equipped weapon path, with nonpersistent native state and no SQL. */
public final class SourceWeaponOverlayPlan {
    static final java.util.concurrent.atomic.AtomicInteger checks=new java.util.concurrent.atomic.AtomicInteger();static int casts,frames,pairWaits,receiverWaits,detachedWaits;
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks.incrementAndGet();}
    static void put(Object target,String name,Object value){SourceWeaponFullPathPlan.put(target,name,value);}
    @SuppressWarnings("unchecked") static <T>T copy(T value,Map<String,Object> changes)throws Exception{
        var parts=value.getClass().getRecordComponents();var types=Arrays.stream(parts).map(java.lang.reflect.RecordComponent::getType).toArray(Class<?>[]::new);var args=new Object[parts.length];
        for(int i=0;i<parts.length;i++)args[i]=changes.containsKey(parts[i].getName())?changes.get(parts[i].getName()):parts[i].getAccessor().invoke(value);
        var ctor=value.getClass().getDeclaredConstructor(types);ctor.setAccessible(true);return(T)ctor.newInstance(args);
    }
    record Fixture(SourceWeaponFullPathPlan.Fixture nativeFixture,SourceWeaponPhysicalStats weapon,SourceEquipmentMagicStats equipment,String key,GameStateStore state){}
    static Fixture fixture(String mode)throws Exception{
        var f=SourceWeaponPlayerFullPathPlan.fixture(mode);var b=f.base().first().base().base();
        int durability=mode.equals("cap")?11:mode.equals("over-cap")?20:0;
        var weapon=new SourceWeaponPhysicalStats(24,1,8,8,1,0,0,6,durability,0,0,0,0,SourceWeaponCombatTraits.none(),SourceProjectilePhysicalStats.none(),"memory-source-weapon");
        var physical=copy(SourceEquipmentPhysicalStats.none(),Map.of("weapon",weapon));
        var equipment=copy(SourceEquipmentMagicStats.none(),Map.of("physical",physical));
        put(b.target(),"sourceEquipmentMagicStats",equipment);put(b.target(),"sourceWeaponSuppressed",mode.equals("suppressed"));
        if(!mode.equals("runtime-priority"))put(b.target(),"inventory",new RuntimeInventory());
        var state=(GameStateStore)CombatAccess.field(b.target(),"state");String key=(String)CombatAccess.call(b.target(),"stateCharacter","()Ljava/lang/String;");
        var inv=(RuntimeInventory)CombatAccess.field(b.target(),"inventory");state.saveEconomyState(key,0L,inv.catalogCounts(),inv.catalogItemStates());
        state.sourceWeaponDurabilityBalance(key,weapon);
        check(!(boolean)CombatAccess.field(state,"persistent"),"source overlay fixture is explicitly nonpersistent");
        return new Fixture(f,weapon,equipment,key,state);
    }
    static void execute(Fixture f)throws Throwable{SourceWeaponPlayerFullPathPlan.execute(f.nativeFixture());casts++;}
    static void exercise(String mode)throws Throwable{
        var f=fixture(mode);var b=f.nativeFixture().base().first().base().base();
        var inventory=(RuntimeInventory)CombatAccess.field(b.target(),"inventory");int hp=(int)CombatAccess.field(b.target(),"playerHp");
        boolean admitted=!Set.of("no-mp","no-material","ready").contains(mode);
        var source=SourceWeaponCounterMagicOracle.evaluate(admitted&&!mode.equals("miss"),Set.of("counter","bypass","miss","earth").contains(mode),mode.equals("bypass"));boolean success=admitted&&!mode.equals("miss")&&!source.blocked();
        boolean changed=success&&!Set.of("suppressed","runtime-priority").contains(mode);
        try{
            execute(f);
            var balance=f.state().sourceWeaponDurabilityBalance(f.key(),f.weapon());
            int expected=changed?SourceWeaponDurabilityOracle.evaluate(f.weapon().durability(),f.weapon().enchantLevel(),3):f.weapon().durability();
            check(balance.itemId()==24&&balance.observedDurability()==f.weapon().durability()&&balance.effectiveDurability()==expected,"actual source overlay follows exact Source durability and original priority/suppression: "+mode);
            check(f.weapon().durability()==(mode.equals("cap")?11:mode.equals("over-cap")?20:0)&&CombatAccess.field(b.target(),"sourceEquipmentMagicStats")==f.equipment(),"read-only source snapshot is never mutated or replaced");
            check(((BattleTimedStatus)CombatAccess.field(b.target(),"timedStatus")).counterMagicActive(System.nanoTime())==source.counterRemaining(),"source-equipped target counter follows exact Source consumer oracle");
            check((int)CombatAccess.field(b.target(),"playerHp")==hp,"source weapon debuff leaves target HP unchanged");
            check((int)CombatAccess.field(b.caster(),"playerMp")== (mode.equals("no-mp")?0:admitted?71:100)&&((RuntimeInventory)CombatAccess.field(b.caster(),"inventory")).countCatalog(40318)==(mode.equals("no-material")?0:admitted?2:3),"source-equipped native cast retains original MP and material costs");
            check(f.nativeFixture().queries().get()==0,"source-equipped PC target never performs unrelated NPC SQL lookup");
            var after=(RuntimeInventory)CombatAccess.field(b.target(),"inventory");
            check(after.catalogCounts().equals(inventory.catalogCounts()),"source overlay never creates, removes or replaces a physical inventory item");
            if(mode.equals("runtime-priority"))check(after.equippedWeaponItem().durability()==3,"runtime equipped instance has original precedence over read-only source weapon");
            for(var receiver:List.of(b.caster(),b.target(),b.viewer(),b.observer())){
                var packets=SourceWeaponFullPathPlan.packets(f.nativeFixture().sockets().get(receiver));frames+=packets.size();
                check(SourceWeaponFullPathPlan.count(packets,Protocol.effectObject(b.target().playerObjectId(),f.nativeFixture().spell().effectGfx()))==(success?1:0),"source path retains original successful PC weapon effect");
                if(receiver==b.target())check(SourceWeaponFullPathPlan.count(packets,Protocol.messageCode(268,"+6 memory-source-weapon"))==(changed?1:0),"source weapon message follows original source branch, including capped no-op");
            }
            check(!java.nio.file.Files.exists(Path.of("detached-memory-state-never-written")),"source weapon overlay produced no state file");
        }finally{SourceWeaponFullPathPlan.cleanup(f.nativeFixture());}
    }
    static void observations()throws Throwable{
        var f=fixture("normal");
        try{
            execute(f);for(var socket:f.nativeFixture().sockets().values())frames+=SourceWeaponFullPathPlan.packets(socket).size();check(f.state().sourceWeaponDurabilityBalance(f.key(),f.weapon()).effectiveDurability()==3,"first native source hit retained");
            var fresh=copy(f.weapon(),Map.of("durability",2));var refreshed=f.state().sourceWeaponDurabilityBalance(f.key(),fresh);
            check(refreshed.observedDurability()==2&&refreshed.effectiveDurability()==2,"new read-only source durability observation resets the old overlay");
            var replacement=copy(fresh,Map.of("itemId",30,"durability",0));var replaced=f.state().sourceWeaponDurabilityBalance(f.key(),replacement);
            check(replaced.itemId()==30&&replaced.effectiveDurability()==0,"different source weapon kind does not inherit the previous overlay");
            String other=f.key()+"/different-detached-role";var separate=f.state().sourceWeaponDurabilityBalance(other,f.weapon());
            check(separate.effectiveDurability()==0&&f.state().sourceWeaponDurabilityBalance(f.key(),replacement).itemId()==30,"separate role key has an independent overlay");
        }finally{SourceWeaponFullPathPlan.cleanup(f.nativeFixture());}
    }
    static void pairWait(String mode)throws Throwable{
        var f=fixture("normal");var b=f.nativeFixture().base().first().base().base();var target=b.target();var original=target.nativePlayerActionLock();
        var field=Arrays.stream(ClientSession.class.getDeclaredFields()).filter(v->{try{v.setAccessible(true);return v.get(target)==original;}catch(Exception e){throw new AssertionError(e);}}).findFirst().orElseThrow();
        var profile=CombatAccess.call(f.state(),"profile","(Ljava/lang/String;)Llocal/lito381/server/GameStateStore$MutableProfile;",f.key());
        var overlay=CombatAccess.field(profile,"sourceWeaponDurability");var profiles=(Map<?,?>)CombatAccess.field(f.state(),"profiles");var keys=Set.copyOf(profiles.keySet());
        var gate=new SourceWeaponPlayerFullPathPlan.GateLock();field.set(target,gate);var error=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        var worker=new Thread(()->{try{execute(f);}catch(Throwable t){error.set(t);}},"source-weapon-overlay-pair-"+mode);
        try{
            gate.lock();try{worker.start();check(gate.tried.await(3,java.util.concurrent.TimeUnit.SECONDS),"actual source-equipped original pair wait reached");SourceTargetNegativeReservePlan.mutate(f.nativeFixture().base(),mode);}finally{gate.unlock();gate.resume.countDown();}
            worker.join(3000);check(!worker.isAlive()&&error.get()==null,"source-equipped pair continuation terminates "+mode+" error="+error.get());
            boolean allowed=mode.equals("normal");check((int)CombatAccess.field(b.caster(),"playerMp")== (allowed?71:100)&&((RuntimeInventory)CombatAccess.field(b.caster(),"inventory")).countCatalog(40318)==(allowed?2:3),"stale source-equipped role does not consume material/MP "+mode);
            check(allowed?f.state().sourceWeaponDurabilityBalance(f.key(),f.weapon()).effectiveDurability()==3:CombatAccess.field(profile,"sourceWeaponDurability")==overlay,"stale role preserves exact source overlay reference "+mode);
            check(profiles.keySet().equals(keys),"stale role never creates another source profile");
            check(allowed||f.nativeFixture().sockets().values().stream().allMatch(socket->socket.bytes.size()==0),"stale source-equipped attempt emits no frame "+mode);
            if(allowed)for(var socket:f.nativeFixture().sockets().values())frames+=SourceWeaponFullPathPlan.packets(socket).size();
            SourceTargetNegativeReservePlan.noInit(f.nativeFixture().base(),mode);pairWaits++;
        }finally{gate.resume.countDown();if(gate.isHeldByCurrentThread())gate.unlock();worker.join(3000);check(!worker.isAlive(),"source-equipped pair worker terminal before restore");field.set(target,original);SourceWeaponFullPathPlan.cleanup(f.nativeFixture());}
    }
    static void receiverWait(String mode)throws Throwable{receiverWait(mode,false);}
    static void receiverWait(String mode,boolean spectator)throws Throwable{
        var f=fixture("normal");var b=f.nativeFixture().base().first().base().base();var viewer=spectator?b.observer():b.viewer();var socket=f.nativeFixture().sockets().get(viewer);
        long cipher=((Protocol.CipherState)CombatAccess.field(viewer,"outgoing")).value();var error=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        var worker=new Thread(()->{try{execute(f);}catch(Throwable t){error.set(t);}},"source-weapon-original-final-viewer-"+mode);
        try{
            synchronized(viewer){
                worker.start();SourceElementAudiencePlan.blocked(worker,"source weapon original viewer monitor");
                System.out.println("ORIGINAL-VIEWER-WAIT "+mode+" spectator="+spectator+" "+Arrays.stream(worker.getStackTrace()).limit(8).toList());
                check(Arrays.stream(worker.getStackTrace()).anyMatch(x->x.getClassName().endsWith("ClientSession")&&(x.getMethodName().contains("Send")||x.getMethodName().contains("Frame"))),"source-equipped output waits in actual original ClientSession sender");
                check(!b.caster().nativePlayerActionLock().isLocked()&&!b.target().nativePlayerActionLock().isLocked(),"producer pair released before actual receiver wait");
                check(f.state().sourceWeaponDurabilityBalance(f.key(),f.weapon()).effectiveDurability()==3,"accepted source durability committed before final viewer output");
                switch(mode){
                    case "normal"->{}
                    case "DRAINING"->put(b.life(),"mode",NativePlayerLifetime.Mode.DRAINING);
                    case "observer"->put(viewer,"observer",!(boolean)CombatAccess.field(viewer,"observer"));
                    case "target-life"->CombatAccess.set(CombatAccess.field(CombatAccess.field(b.target(),"timedStatus"),"sourcePoisonLifecycle"),"sourceNpcOwner",new Object());
                    case "role"->put(viewer,"selectedCharacter","retired-viewer");
                    case "status"->put(viewer,"timedStatus",null);
                    case "life"->CombatAccess.set(CombatAccess.field(CombatAccess.field(viewer,"timedStatus"),"sourcePoisonLifecycle"),"sourceNpcOwner",new Object());
                    case "Main"->put(b.hub(),"nativeStaticMotionClosed",true);
                    case "STOPPED"->put(b.life(),"mode",NativePlayerLifetime.Mode.STOPPED);
                    default->throw new AssertionError(mode);
                }
            }
            worker.join(3000);if(error.get()!=null)error.get().printStackTrace();check(!worker.isAlive()&&error.get()==null,"original final viewer worker terminates "+mode);
            boolean allowed=mode.equals("normal")||mode.equals("DRAINING");check(allowed?socket.bytes.size()>0:socket.bytes.size()==0,"actual original viewer frame rejects stale role/status/life/Main before cipher "+mode+" bytes="+socket.bytes.size());
            check(allowed||((Protocol.CipherState)CombatAccess.field(viewer,"outgoing")).value()==cipher,"rejected final viewer never advances cipher "+mode);
            for(var value:f.nativeFixture().sockets().values())frames+=SourceWeaponFullPathPlan.packets(value).size();
            if(mode.equals("target-life"))check(SourceWeaponFullPathPlan.count(SourceWeaponFullPathPlan.packets(f.nativeFixture().sockets().get(b.caster())),Protocol.manaPoint(71,100))==1,"accepted source MP update survives later target life invalidation");receiverWaits++;
        }finally{worker.join(3000);check(!worker.isAlive(),"original viewer worker terminal before fixture cleanup");SourceWeaponFullPathPlan.cleanup(f.nativeFixture());}
    }
    static NativeSourceWeaponFinalFrame.Output detach(Fixture f,boolean npc)throws Throwable{
        var b=f.nativeFixture().base().first().base().base();var c=new NativeAreaMaterialAction.Context(b.caster());long id=npc?777:b.target().playerObjectId();
        c.nativeSourceWeaponFirstLife=Protocol.attackSpellRequest(SourceTargetNegativeLifePlan.packet(27,id));
        check(NativeSourceTargetNegativeLife.capture(c,id),"detached output borrows actual first Context participants");
        var output=NativeSourceWeaponFinalFrame.output(c,b.viewer(),Protocol.effectObject(id,f.nativeFixture().spell().effectGfx()),"detached-original-weapon-frame");
        check(output!=null,"first immutable output available before producer scope closes");NativeSourceTargetNegativeLife.close(c);
        check(c.nativeSourceTargetNegativeProof==null,"producing Context proof closed before detached worker starts");return output;
    }
    static void detached(String mode,boolean npc)throws Throwable{
        var f=fixture("normal");var b=f.nativeFixture().base().first().base().base();var receiver=b.viewer();var socket=f.nativeFixture().sockets().get(receiver);var out=detach(f,npc);
        var error=new java.util.concurrent.atomic.AtomicReference<Throwable>();var worker=new Thread(()->{try{check(NativeAreaMaterialAction.current.get()==null&&NativePlayerCommit.frame()==null,"detached final sender has no producing Context or Frame");NativeSourceWeaponFinalFrame.send(out);}catch(Throwable t){error.set(t);}},"detached-weapon-explicit-proof-"+mode);
        long cipher=((Protocol.CipherState)CombatAccess.field(receiver,"outgoing")).value();
        try{
            synchronized(receiver){worker.start();SourceElementAudiencePlan.blocked(worker,"detached original final monitor");
                check(Arrays.stream(worker.getStackTrace()).anyMatch(x->x.getMethodName().equals("nativeSourceWeaponStaticFrame")),"explicit proof reaches complete original synchronized final sibling");
                switch(mode){case "normal"->{}case "role"->put(receiver,"selectedCharacter","retired-detached-viewer");case "status"->put(receiver,"timedStatus",null);case "life"->CombatAccess.set(CombatAccess.field(CombatAccess.field(receiver,"timedStatus"),"sourcePoisonLifecycle"),"sourceNpcOwner",new Object());case "DRAINING"->put(b.life(),"mode",NativePlayerLifetime.Mode.DRAINING);case "Main"->put(b.hub(),"nativeStaticMotionClosed",true);case "STOPPED"->put(b.life(),"mode",NativePlayerLifetime.Mode.STOPPED);case "target-life"->CombatAccess.set(CombatAccess.field(CombatAccess.field(b.target(),"timedStatus"),"sourcePoisonLifecycle"),"sourceNpcOwner",new Object());case "source-status"->put(b.caster(),"timedStatus",null);case "npc-life"->CombatAccess.set(CombatAccess.field(CombatAccess.field(f.nativeFixture().base().first().pet(),"timedStatus"),"sourcePoisonLifecycle"),"sourceNpcOwner",new Object());case "npc-status"->put(f.nativeFixture().base().first().pet(),"timedStatus",null);case "attachment"->put(b.target(),"battleCompanion",null);default->throw new AssertionError(mode);}
            }
            worker.join(3000);if(error.get()!=null)error.get().printStackTrace();check(!worker.isAlive()&&error.get()==null,"detached original chain terminates "+mode);
            boolean allowed=mode.equals("normal")||mode.equals("DRAINING");check(socket.bytes.size()==(allowed?out.packet().length+2:0),"explicit first proof remains effective after producing scope is gone "+mode);
            check(allowed||((Protocol.CipherState)CombatAccess.field(receiver,"outgoing")).value()==cipher,"rejected detached output never advances cipher "+mode);
            if(allowed){frames+=SourceWeaponFullPathPlan.packets(socket).size();byte[] sent=socket.bytes.toByteArray();var original=SourceNpcHpOutputPlan.io(receiver);receiver.nativeSelfOriginalSend(out.packet(),out.note());check(Arrays.equals(sent,original.bytes.toByteArray()),"complete explicit-proof sender/motion/cipher chain matches original encrypted frame");}
            detachedWaits++;
        }finally{worker.join(3000);check(!worker.isAlive(),"detached worker terminal before cleanup");SourceWeaponFullPathPlan.cleanup(f.nativeFixture());}
    }
    static void outputShape(){
        for(var type:List.of(NativeSourceWeaponFinalFrame.Participant.class,NativeSourceWeaponFinalFrame.Npc.class,NativeSourceWeaponFinalFrame.Proof.class,NativeSourceWeaponFinalFrame.Output.class))for(var field:type.getDeclaredFields())check(!Set.of(NativeAreaMaterialAction.Context.class,NativePlayerCommit.Frame.class,NativeCombatAttacker.Ref.class,Map.class,List.class).contains(field.getType()),"detached output has no producing Context/Frame/Ref/audience map or list: "+field.getName());
        check(NativeSourceWeaponFinalFrame.class.getDeclaredFields().length==0,"final output helper has no registry, TLS or worker field");
    }
    @FunctionalInterface interface Task{void run()throws Throwable;}
    static void environment(Task task)throws Throwable{
        var rules=CombatRules.class.getDeclaredField("current");rules.setAccessible(true);Object old=rules.get(null);
        var controls=NativeSourceOtherControls.class.getDeclaredField("current");controls.setAccessible(true);Object oldControls=controls.get(null);
        var database=CombatDatabase.class.getDeclaredField("current");database.setAccessible(true);Object oldDatabase=database.get(null);
        var frames=SourceNpcHpOutputPlan.frames();var oldFrame=frames.get();var area=NativeAreaMaterialAction.current;var oldArea=area.get();
        try{
            var path=Path.of("detached-source-weapon-overlay-no-SQL");rules.set(null,new CombatRules.Snapshot(Map.of("DragonKnightAwakeClassicMode",0D,"INVIS_CAST_BREAK",1D),path,path,path,Set.of(),path));
            var props=new Properties();props.setProperty("hpbar","true");controls.set(null,new NativeSourceOtherControls.Snapshot(NativeSourceDisplayPlan.Settings.read(props),path));
            BattleSpellCatalog.weaponBreakSpell(27);database.set(null,new CombatDatabase.Snapshot(Map.of(27,new CombatDatabase.Skill(32,0,3000,0,0,0,0,40318,1)),new SourceSkillControls.Snapshot(Map.of(),Map.of(),Map.of(),Map.of())));
            task.run();
        }finally{database.set(null,oldDatabase);controls.set(null,oldControls);rules.set(null,old);if(oldFrame==null)frames.remove();else frames.set(oldFrame);if(oldArea==null)area.remove();else area.set(oldArea);}
    }
    public static void main(String[]args)throws Throwable{
        environment(()->{if(args.length>0&&args[0].startsWith("final:")){receiverWait(args[0].substring(6));return;}for(String mode:List.of("normal","counter","bypass","miss","earth","no-mp","no-material","ready","cap","over-cap","suppressed","runtime-priority"))exercise(mode);observations();for(String mode:List.of("normal","Main","STOPPED","lifetime","foreign-life","source-role","owner-role","source-status","owner-status","owner-life"))pairWait(mode);for(String mode:List.of("normal","role","status","life","Main","STOPPED","DRAINING","observer","target-life"))receiverWait(mode);for(String mode:List.of("normal","role","status","life","Main","STOPPED","observer"))receiverWait(mode,true);for(String mode:List.of("normal","role","status","life","DRAINING","Main","STOPPED","target-life","source-status"))detached(mode,false);for(String mode:List.of("normal","npc-life","npc-status","attachment"))detached(mode,true);outputShape();});
        System.out.println("PASS"+checks.get()+" source-equipped weapon native-material/overlay/Source-snapshot/cap/priority/observation contracts; original-casts="+casts+", encrypted-memory-frames="+frames+", original-pair-waits="+pairWaits+", original-final-viewer-waits="+receiverWaits+", detached-original-final-waits="+detachedWaits+"; physical consumers and remaining lifecycle/backend/MySQL/game unqualified");
    }
}
