package local.lito381.server;
/** First controlled29/152/153 intent survives the original roster-lock retry, never re-captured. */
final class NativeSourceNegativePairFirst {
 record First(NativeSourceSelfFinalFrame.First source,NativeSourceTargetNegativeLife.Proof proof){}
 static boolean scoped(NativeSourceNegativeTargets.Aim a){return a!=null&&"BATTLE".equals(a.c.phase)&&a.target!=null&&a.target.kind()==3&&(a.skill==29||a.skill==152||a.skill==153);}
 static void capture(NativeSourceNegativeTargets.Aim a){
  if(!scoped(a))return;
  var first=NativeSourceMassTeleportExit.capture(a.c.pc,a.c.caster).first();
  Object original=a.c.nativeSourceTargetNegativeProof;
  try{a.nativeSourceNegativePairFirst=first!=null&&original==null&&NativeSourceTargetNegativeLife.capture(a.c,a.target.id())?new First(first,NativeSourceTargetNegativeLife.proof(a.c)):Boolean.FALSE;}
  finally{a.c.nativeSourceTargetNegativeProof=original;}
 }
 static boolean current(NativeSourceNegativeTargets.Aim a){
  if(!scoped(a))return true;
  if(!(a.nativeSourceNegativePairFirst instanceof First value)||!NativeSourceSelfFinalFrame.current(value.source()))return false;
  var c=a.c;var first=value.source();var p=value.proof();
  if(p==null||p.life()!=first.lifetime()||!NativeSourceTargetNegativeLife.player(p.source(),p.sourceTimed()))return false;
  var m=p.member();
  if(m==null||p.peer()!=a.owner||p.peerTimed()==null||CombatAccess.field(a.owner,"timedStatus")!=p.peerTimed()||NativeSourceTargetNegativeLife.raw(p.peerTimed())!=m.life||m.acceptedLife!=m.life||!m.current(false)||NativeSourceRawDamage.number(a.owner,"playerHp")<=0)return false;
  var n=p.npc();
  return n==a.target&&c.npcs.get(n.id())==n&&n.actor()==a.pet&&p.actorTimed()!=null&&CombatAccess.field(a.pet,"timedStatus")==p.actorTimed()&&p.actorLife()!=null&&NativeSourceTargetNegativeLife.raw(p.actorTimed())==p.actorLife()&&NativeSourceNpcFirstLife.first(a.pet,n,c)==p.actorLife()&&NativeSourceControlledDisplay.attached(a.owner,a.pet)&&NativeSourceRawDamage.number(a.pet,"hp")>0;
 }
 static boolean current(NativeAreaMaterialAction.Context c){return c==null||!(c.nativeSourceNegativeAim instanceof NativeSourceNegativeTargets.Aim a)||current(a);}
 static boolean retry(NativePlayerCommit.Binding binding,ClientSession pc,NativeAreaMaterialAction.Context c){return current(c)&&binding.matches(pc);}
 static boolean scene(NativeAreaMaterialAction.Context c){return current(c)&&NativeSourceAreaPlayerProof.scene(c);}
 static void close(NativeSourceNegativeTargets.Aim a){if(scoped(a))a.nativeSourceNegativePairFirst=null;}
 private NativeSourceNegativePairFirst(){}
}
