package local.lito381.server;

/** Bounded shared Task carries first identities through the original pair wait. */
final class NativeSourceMaterialPairGuard {
 record Guarded(NativeSourceLightPairLife.Proof proof,Object sourceTimed,Object targetTimed,NativeSelfCastAction.Task original) implements NativeSelfCastAction.Task {
  boolean ready(){
   if(proof==null||proof.caster()==null||proof.target()==null||sourceTimed==null||targetTimed==null||!CombatRules.loaded())return false;
   if(CombatAccess.field(proof.caster().pc(),"timedStatus")!=sourceTimed||CombatAccess.field(proof.target().pc(),"timedStatus")!=targetTimed||CombatAccess.field(proof.hub(),"sourcePlayerCommitLifetime")!=proof.lifetime())return false;
   return proof.locked();
  }
  public void run() throws java.io.IOException {if(ready())original.run();}
 }
 static NativeSourceLightPairLife.Actor actor(NativeSourceRawDamage.Member m){return m==null?null:new NativeSourceLightPairLife.Actor(m.pc,m.binding,m.acceptedLife,m.dead);}
 static NativeSelfCastAction.Task task(ClientSession caster,ClientSession target,NativeSelfCastAction.Task original){
  var c=NativeAreaMaterialAction.current.get();var p=NativeSourceTargetNegativeLife.proof(c);var f=NativePlayerCommit.frame();
  boolean accepted=f!=null&&f.accepted&&!f.event.world()&&f.nativeSourceAcceptedPlayers instanceof NativeSourceAcceptedPlayers.Proof;
  boolean inherited=c!=null&&c.pc==caster&&"BATTLE".equals(c.phase)&&(p!=null||NativeSourceWeaponTargetLife.marked(c));
  if(target==null||!NativeSourcePairedScope.enabled(caster)&&!inherited&&!accepted)return NativeSourceLightPairLife.nativeSourceMaterialOriginalTask(caster,target,original);
  if(!CombatRules.loaded())return new Guarded(null,null,null,original);
  Object source=CombatAccess.field(caster,"timedStatus"),peer=CombatAccess.field(target,"timedStatus");
  NativeSourceLightPairLife.Proof proof;
  if(inherited&&!accepted){
   if(p==null||p.source()==null||p.source().pc!=caster||p.peer()!=target)return new Guarded(null,null,null,original);
   source=p.sourceTimed();peer=p.peerTimed();proof=new NativeSourceLightPairLife.Proof(actor(p.source()),actor(p.member()),c.hub,p.life());
  }else proof=NativeSourceLightPairLife.capture(caster,target);
  return new Guarded(proof,source,peer,original);
 }
 static boolean matches(NativePlayerCommit.Binding binding,ClientSession pc,NativeSelfCastAction.Task task){
  if(!(task instanceof Guarded g))return NativeSourceWeaponPlayerCounter.matches(binding,pc);
  if(!g.ready()||binding==null)return false;
  var p=g.proof();var actor=pc==p.caster().pc()?p.caster():pc==p.target().pc()?p.target():null;
  return actor!=null&&binding.equals(actor.binding());
 }
 private NativeSourceMaterialPairGuard(){}
}
