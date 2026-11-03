"""Observe real native-answer versus direct-root behavior across role replacement."""
from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
t=(R/'ResurrectionMaterialProbe.java').read_text('utf-8')
main=t[t.index(' public static void main'):]
main=main.replace('new boolean[]{false,true})for(boolean world','new boolean[]{true})for(boolean world',1).replace('mode<16','mode<8')
a=main.index('  String result=');b=main.index('  Files.writeString(output,result',a)
main=main[:a]+'''  String result="{\\"cases\\":"+cases+",\\"checks\\":"+ownChecks+",\\"stale_native_acceptances\\":"+nativeGaps+",\\"stale_direct_acceptances\\":"+directGaps+",\\"frames\\":"+frames+",\\"locked_writes\\":"+lockedWrites+",\\"full_goal_achieved\\":false}";
'''+main[b:]
head='''package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Observational audit, not a claim that the current answer lifetime is qualified. */
public final class ResurrectionAnswerLifetimeProbe extends ResurrectionMaterialProbe {
 static int nativeGaps,directGaps;
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);int skill=selectedSkill;Object row=spell(skill);int change=mode/2;boolean nativeEntry=mode%2==0;
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});if(item>0)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   cast(pc,skill,row);need(active(peer,skill),"actual native cast installed question before transition");
   if(change==1)NativePlayerCommit.selected(pc,pc.selectedCharacterName());
   else if(change==2)NativePlayerCommit.selected(peer,peer.selectedCharacterName());
   else if(change==3)set(pc,"phase",Enum.valueOf((Class)get(pc,"phase").getClass(),"LOBBY"));
   int question=(int)call(row,"questionId",new Class[]{});
   if(nativeEntry)answer(peer,question,1);else call(peer,"answerResurrection",new Class[]{Protocol.AskAnswer.class},new Protocol.AskAnswer(question,1,""));
   boolean revived=(int)get(peer,"playerHp")>0;
   if(change==0)need(revived,"ordinary native/direct answer baseline");
   if(change==3)need(!revived,"existing phase rejection control");
   if((change==1||change==2)&&revived){if(nativeEntry)nativeGaps++;else directGaps++;}
   collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(peer))==0,"audit receipts drained and output unlocked");
  }cases++;
 }
'''
(R/'ResurrectionAnswerLifetimeProbe.java').write_text(head+main,encoding='utf-8')
t=(R/'test.py').read_text('utf-8').replace("D=R/'build/tests'","D=R/'build/answer-audit'").replace('ResurrectionMaterialProbe','ResurrectionAnswerLifetimeProbe')
t=t.replace("[R/'ResurrectionAnswerLifetimeProbe.java',Path(__file__)]","[R/'ResurrectionAnswerLifetimeProbe.java',R/'ResurrectionMaterialProbe.java',Path(__file__)]")
t=t.replace("[('before',b['base']),('after',b['runtime'])]","[('observed',b['runtime'])]")
t=t.replace("classes,R/'ResurrectionAnswerLifetimeProbe.java')","classes,R/'ResurrectionAnswerLifetimeProbe.java',R/'ResurrectionMaterialProbe.java')")
line="v[mode]=read(dest/'observations.json');assert v[mode]['cases']==256 and v[mode]['Source_window_bypasses']==(16 if mode=='before' else 0) and v[mode]['locked_writes']==0"
assert line in t;t=t.replace(line,"v[mode]=read(dest/'observations.json');assert v[mode]['cases']==64 and v[mode]['locked_writes']==0")
t=t.replace("   assert (D/'before/goldens.txt').read_bytes()==(D/'after/goldens.txt').read_bytes()\n",'').replace("status='passed',stable_wire_equal=True","status='observed-not-qualified'")
(R/'audit_answer_lifetime.py').write_text(t,encoding='utf-8')
(R/'answer-audit-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[R/'ResurrectionMaterialProbe.java',R/'test.py',Path(__file__)]],scope='64 observations; actual cast before Source role generation replacement; native C_ANSWER and private-root controls kept separate; no automatic parity claim'),indent=2),encoding='utf-8')
