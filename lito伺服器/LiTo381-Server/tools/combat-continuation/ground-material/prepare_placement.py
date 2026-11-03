from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent
t=(R/'GroundLifecycleProbe.java').read_text('utf-8');main=t[t.index(' public static void main('):]
main=main.replace('mode<6','mode<2')
body='''package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import java.lang.management.*;
/** Native cast waits on actual GameHub monitor while room eligibility changes. */
public final class GroundPlacementProbe extends GroundMaterialProbe {
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);var row=BattleSpellCatalog.groundSpell(selectedSkill);int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();if(item!=0)NativeDeItemPublication.grant(pc,item,count+3);int mpCost=cost(pc,selectedSkill,row);var error=new AtomicReference<Throwable>();var done=new AtomicBoolean();
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   var worker=new Thread(()->{try{cast(pc,selectedSkill,row);done.set(NativeLightStoppedProbe.idleContext());}catch(Throwable e){error.set(e);}},"ground-real-hub-wait");
   synchronized(hub){worker.start();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);boolean waiting=false;while(worker.isAlive()&&System.nanoTime()<deadline){var info=ManagementFactory.getThreadMXBean().getThreadInfo(worker.getId());waiting=info!=null&&info.getThreadState()==Thread.State.BLOCKED&&info.getLockOwnerId()==Thread.currentThread().getId()&&info.getLockInfo().getIdentityHashCode()==System.identityHashCode(hub);if(waiting)break;Thread.sleep(1);}
    need(waiting&&pc.nativePlayerActionLock().isLocked()&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==1,"actual native producer waits for actual hub after owned admission");need((int)get(pc,"playerMp")==1000&&active(pc,hub,world)==0&&wire.raw.size()==0,"no mutation or output while hub unresolved");
    if(mode==0){if(world)((Map<?,?>)get(hub,"classicWorldMembership")).remove(pc);else set(room(pc,hub),"battleStarted",false);}
   }
   worker.join(3000);if(error.get()!=null)throw new AssertionError(error.get());need(!worker.isAlive()&&done.get(),"ground hub wait finishes without lock inversion");boolean accept=mode==1;need((int)get(pc,"playerMp")==1000-(accept?mpCost:0)&&(active(pc,hub,world)>0)==accept,"actual placement rechecks membership after waiting");if(item!=0)need(pc.nativeDeInventory(0).countCatalog(item)==count+3-(accept?count:0),"revoked room does not consume material");if(!accept)need(wire.raw.size()+other.raw.size()==0&&!((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(selectedSkill),"revoked placement does not emit packets or reuse");collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"output unlocked and admission drained");
  }cases++;
 }
'''
(R/'GroundPlacementProbe.java').write_text(body+main,encoding='utf-8')
t=(R/'test_clock.py').read_text('utf-8').replace("R/'build/clock'","R/'build/placement'")
t=t.replace("R/'build/boundaries/memory-observer',",'')
t=t.replace(" hook=D/'DeItemTransactionProbe.java';hook.write_text('package local.lito381.server; public final class DeItemTransactionProbe {public static void beforeSave(){GroundClockProbe.beforeSave();}public static void beforeFlush()throws Exception{GroundClockProbe.beforeFlush();}}',encoding='utf-8')\n",'')
t=t.replace("[R/'GroundClockProbe.java',hook,Path(__file__)]","[R/'GroundPlacementProbe.java',Path(__file__)]").replace("R/'GroundClockProbe.java',hook)","R/'GroundPlacementProbe.java')").replace('GroundClockProbe','GroundPlacementProbe')
t=t.replace("row['cases']==48 and row['locked_writes']==0","row['cases']==32 and row['locked_write_calls']==0")
(R/'test_placement.py').write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'placement-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[R/'GroundLifecycleProbe.java',R/'test_clock.py',Path(__file__)]],scope='actual hub monitor contention and placement revocation, native58/63, WORLD/BATTLE, both state and item-app flags'),indent=2),encoding='utf-8')
