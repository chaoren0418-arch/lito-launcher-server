from pathlib import Path
R=Path(__file__).resolve().parent
t=(R/'ResurrectionCompanionProbe.java').read_text('utf-8')
start=t.index(' static void exercise(');end=t.index(' public static void main',start)
body=r''' static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,true);set(peer,"playerHp",4000);int skill=selectedSkill;Object row=spell(skill);
  var pet=companion(peer,(int)get(pc,"positionX")+2,(int)get(pc,"positionY")+2);targetId=pet.snapshot().objectId();pet.applyDamage(Integer.MAX_VALUE);
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});if(item>0)NativeDeItemPublication.grant(pc,item,count+3);
  var error=new java.util.concurrent.atomic.AtomicReference<Throwable>();var clean=new java.util.concurrent.atomic.AtomicBoolean();
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   Thread worker=new Thread(()->{try{cast(pc,skill,row);clean.set(NativeLightStoppedProbe.idleContext());}catch(Throwable e){error.set(e);}},"resurrection-companion-audit");
   var lock=pc.nativePlayerActionLock();BattleCompanion replacement=null;lock.lock();try{
    worker.start();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
    while(!lock.hasQueuedThread(worker)&&worker.isAlive()&&System.nanoTime()<deadline)Thread.sleep(1);
    need(lock.hasQueuedThread(worker)&&(int)get(pc,"playerMp")==1000&&pet.snapshot().hp()==0,"actual native companion cast queued before mutations");
    if(mode==1)NativePlayerCommit.selected(peer,peer.selectedCharacterName());
    else if(mode==2){((List<BattleCompanion>)get(peer,"tamedCompanions")).remove(pet);replacement=companion(peer,(int)get(pc,"positionX")+2,(int)get(pc,"positionY")+2);replacement.applyDamage(Integer.MAX_VALUE);}
    else if(mode==3)set(peer,NativeDeItemApplication.DISCONNECTED,true);
   }finally{lock.unlock();worker.join(3000);}
   if(error.get()!=null)throw new AssertionError("native companion audit",error.get());need(!worker.isAlive()&&clean.get(),"audit worker and contexts drain");
   boolean revived=(mode==2?replacement:pet).snapshot().hp()>0;
   if(mode==0)need(revived,"actual companion resurrection control");
   else if(revived)gaps++;
   collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(peer))==0,"audit output and receipts drain");
  }cases++;
 }
'''
t=t[:start]+body+t[end:]
t=t.replace('ResurrectionCompanionProbe','ResurrectionCompanionAuditProbe').replace('new boolean[]{false,true})for(boolean world','new boolean[]{true})for(boolean world').replace('mode<10','mode<4')
t=t.replace('Source_window_bypasses','stale_companion_acceptances')
(R/'ResurrectionCompanionAuditProbe.java').write_text(t,encoding='utf-8')
d=(R/'test_companions.py').read_text('utf-8').replace('build/companions','build/companion-audit').replace('ResurrectionCompanionProbe','ResurrectionCompanionAuditProbe')
d=d.replace("v[mode]['cases']==80", "v[mode]['cases']==16").replace(" and v[mode]['Source_window_bypasses']==0",'').replace("status='passed'", "status='observed-not-qualified'")
(R/'audit_companion_lifetime.py').write_text(d,encoding='utf-8')
