from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent
t=(R/'ResurrectionAnswerLifetimeProbe.java').read_text('utf-8')
start=t.index(' static void exercise(');end=t.index(' public static void main',start)
body=r''' static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);int skill=selectedSkill;Object row=spell(skill);int change=mode/2;boolean nativeEntry=mode%2==0;
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});if(item>0)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   cast(pc,skill,row);need(active(peer,skill),"actual cast installs question before queued reply");
   int question=(int)call(row,"questionId",new Class[]{});int priorSelf=wire.raw.size(),priorPeer=other.raw.size(),mp=(int)get(pc,"playerMp");
   // Hold only the actor whose transition is injected. Holding the target lock
   // while selecting the caster would itself invert the Source pair lock order.
   var lock=(change==1||change==3?pc:peer).nativePlayerActionLock();var error=new java.util.concurrent.atomic.AtomicReference<Throwable>();var clean=new java.util.concurrent.atomic.AtomicBoolean();
   Thread worker=new Thread(()->{try{if(nativeEntry)answer(peer,question,1);else call(peer,"answerResurrection",new Class[]{Protocol.AskAnswer.class},new Protocol.AskAnswer(question,1,""));clean.set(NativeLightStoppedProbe.idleContext());}catch(Throwable e){error.set(e);}},"resurrection-answer-role-race");
   lock.lock();try{
    worker.start();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
    while(!lock.hasQueuedThread(worker)&&worker.isAlive()&&System.nanoTime()<deadline)Thread.sleep(1);
    need(lock.hasQueuedThread(worker),"actual native/direct reply waits for ordered target lock");
    need((int)get(peer,"playerHp")==0&&active(peer,skill)&&wire.raw.size()==priorSelf&&other.raw.size()==priorPeer,"queued reply has no premature HP or output");
    if(change==1)NativePlayerCommit.selected(pc,pc.selectedCharacterName());
    else if(change==2)NativePlayerCommit.selected(peer,peer.selectedCharacterName());
    else if(change==3)set(pc,NativeDeItemApplication.DISCONNECTED,true);
    else if(change==4)NativePlayerCommit.scene(peer,(int)get(peer,"classicWorldMapId")+1,"classicWorldMapId");
    else if(change==5)NativePlayerLifetime.shutdown(hub,false,0);
    else if(change==6)peer.nativeRejectResurrectionQuestion();
   }finally{lock.unlock();worker.join(3000);}
   if(error.get()!=null)throw new AssertionError("queued original reply",error.get());
   need(!worker.isAlive()&&clean.get(),"reply finished and thread-local contexts clean");
   boolean accepted=change==0||change==5;
   need(((int)get(peer,"playerHp")>0)==accepted,"role/scene/disconnect/cleared reply rejected; already owned STOPPED receipt commits");
   need(!active(peer,skill)&&get(peer,"nativeResurrectionAnswerLease")==null,"completed or rejected pending question and lease released");
   need((int)get(pc,"playerMp")==mp,"answer never repeats caster debit");
   int bytes=other.raw.size();answer(peer,question,1);need(other.raw.size()==bytes,"repeat answer cannot resurrect or publish twice");
   collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(peer))==0&&SourceActionPairProbe.state(pc).queue.isEmpty()&&SourceActionPairProbe.state(peer).queue.isEmpty(),"both lifetime receipts, deferred outputs and queues drained");
  }cases++;
 }
'''
t=t[:start]+body+t[end:]
t=t.replace('/** Observational audit, not a claim that the current answer lifetime is qualified. */','/** Actual native/direct replies queued across role, scene, disconnect and STOPPED transitions. */')
t=t.replace('ResurrectionAnswerLifetimeProbe','ResurrectionAnswerRaceProbe').replace('mode<8','mode<14')
(R/'ResurrectionAnswerRaceProbe.java').write_text(t,encoding='utf-8')
d=(R/'test_answers.py').read_text('utf-8').replace("build/answers","build/answer-races").replace('ResurrectionAnswerLifetimeProbe','ResurrectionAnswerRaceProbe')
d=d.replace("[('before',b['base']),('after',b['runtime'])]","[('after',b['runtime'])]")
d=d.replace("v[mode]['cases']==64", "v[mode]['cases']==112").replace("(16 if mode=='before' else 0)",'0')
(R/'test_races.py').write_text(d,encoding='utf-8')
