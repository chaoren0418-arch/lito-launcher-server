package local.lito381.server;
/** Event-owned accepted participants contain values, never a Frame/Context/calculator back-reference. */
final class NativeSourceAcceptedPlayers {
 record Participant(ClientSession pc,NativePlayerCommit.Binding binding,Object life,int x,int y,boolean dead){}
 record Proof(Participant target,Participant source){}
 static Participant actor(NativeSourceDamageDisplay.Actor actor){return actor==null?null:new Participant(actor.pc(),actor.binding(),actor.life(),actor.x(),actor.y(),false);}
 static Participant source(NativePlayerCommit.Frame frame,NativeSourceDamageDisplay.Plan plan){
  var pc=frame.casterSession;var binding=frame.event.caster();if(pc==null||binding==null)return null;
  var member=plan.nativeRawGuard==null?null:plan.nativeRawGuard.source();
  if(member!=null&&member.pc==pc&&member.binding.equals(binding))return new Participant(pc,binding,member.life,member.x,member.y,member.dead);
  return plan.caster!=null&&plan.caster.pc()==pc&&plan.caster.binding().equals(binding)?actor(plan.caster):null;
 }
 static void freeze(NativePlayerCommit.Frame frame){
  if(frame==null||frame.nativeSourceAcceptedPlayers!=null||frame.queue==null)return;var vital=CombatAccess.field(frame.queue,"vitalLock");if(!(vital instanceof java.util.concurrent.locks.ReentrantLock lock)||!lock.isHeldByCurrentThread())return;
  Participant target=null,source=null;var plan=frame.event.sourceDisplay();
  if(frame.accepted&&!frame.event.world()&&frame.event.target().phase().equals("BATTLE")&&plan!=null){
   Object life=null;var member=plan.nativeRawGuard==null?null:plan.nativeRawGuard.target();
   if(plan.target!=null&&plan.target.pc()==frame.pc&&plan.target.binding().equals(frame.event.target()))life=plan.targetLife;
   else if(member!=null&&member.pc==frame.pc&&member.binding.equals(frame.event.target()))life=member.acceptedLife;
   if(life!=null){target=new Participant(frame.pc,frame.event.target(),life,NativeSourceRawDamage.number(frame.pc,"positionX"),NativeSourceRawDamage.number(frame.pc,"positionY"),frame.fatal);source=source(frame,plan);}
  }
  frame.nativeSourceAcceptedPlayers=new Proof(NativeSourceIndirectPlayerProof.accepted(target,frame),source);
 }
 static NativeSourceRawDamage.Member copy(Participant proof){var member=new NativeSourceRawDamage.Member(proof.pc(),proof.binding(),proof.life(),proof.x(),proof.y());member.dead=proof.dead();return member;}
 static NativeSourceRawDamage.Member target(ClientSession pc,NativePlayerCommit.Binding binding,NativePlayerCommit.Frame frame){if(frame!=null&&frame.nativeSourceAcceptedPlayers instanceof Proof p&&p.target()!=null&&p.target().pc()==pc&&p.target().binding().equals(binding))return copy(p.target());return new NativeSourceRawDamage.Member(pc,binding);}
 static NativeSourceRawDamage.Member source(ClientSession pc,NativePlayerCommit.Binding binding,Object fallback,NativePlayerCommit.Frame frame){if(frame!=null&&frame.nativeSourceAcceptedPlayers instanceof Proof p&&p.source()!=null&&p.source().pc()==pc&&p.source().binding().equals(binding))return copy(p.source());return new NativeSourceRawDamage.Member(pc,binding,fallback);}
 static NativeSourceRawDamage.Member fromActor(ClientSession pc,NativePlayerCommit.Binding binding,NativeSourceDamageDisplay.Actor original,Object life){if(binding!=null&&binding.phase().equals("BATTLE")&&original!=null&&original.pc()==pc&&original.binding().equals(binding)&&original.life()==life)return new NativeSourceRawDamage.Member(pc,binding,life,original.x(),original.y());return new NativeSourceRawDamage.Member(pc,binding,life);}
 static NativeSourceRawDamage.Member ingress(ClientSession pc){if(!CombatRules.loaded()||!String.valueOf(CombatAccess.field(pc,"phase")).equals("BATTLE"))return new NativeSourceRawDamage.Member(pc,NativePlayerCommit.Binding.capture(pc));Object life=NativeSourceControlledDisplay.token(pc);int x=NativeSourceRawDamage.number(pc,"positionX"),y=NativeSourceRawDamage.number(pc,"positionY");return new NativeSourceRawDamage.Member(pc,NativePlayerCommit.Binding.capture(pc),life,x,y);}
 static NativeSourceRawDamage.Member entryMember(ClientSession pc,NativePlayerCommit.Binding binding,Object fallback,NativeSourceRawDamage.Member original){return original!=null&&original.pc==pc&&original.binding.equals(binding)?original:new NativeSourceRawDamage.Member(pc,binding,fallback);}
 private NativeSourceAcceptedPlayers(){}
}
