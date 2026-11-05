package local.lito381.server;
/** First material intent stays in its existing bounded Context; no new actor registry or worker. */
final class NativeSourceTargetNegativeLife {
 record Proof(NativePlayerLifetime life,NativeSourceRawDamage.Member source,Object sourceTimed,ClientSession peer,NativeSourceRawDamage.Member member,Object peerTimed,NativeAreaMaterialAction.Npc npc,Object actorTimed,Object actorLife,Object generation){}
 static boolean enabled(NativeAreaMaterialAction.Context c){return c!=null&&CombatRules.loaded()&&"BATTLE".equals(c.phase);}
 static Proof proof(NativeAreaMaterialAction.Context c){return c!=null&&c.nativeSourceTargetNegativeProof instanceof Proof p?p:null;}
 static Protocol.AttackSpellRequest request(Protocol.AttackSpellRequest r,NativeAreaMaterialAction.Context c){return NativeSourceNoneTargetRequest.request(c,c.pc,r,r.skillId());}
 static Object timed(Object actor,boolean natural){return CombatAccess.field(actor,natural?"damagePoison":"timedStatus");}
 static Object raw(Object status){if(status==null)return null;Object state=CombatAccess.field(status,"sourcePoisonLifecycle");return state==null?null:CombatAccess.field(state,"sourceNpcOwner");}
 static boolean capture(NativeAreaMaterialAction.Context c,long id){
  if(!enabled(c))return true;if(c.nativeSourceTargetNegativeProof!=null||!(c.nativeSourceFirstPlayers instanceof NativeSourceFirstPlayers.Snapshot))return false;
  Object value=CombatAccess.field(c.hub,"sourcePlayerCommitLifetime");if(!(value instanceof NativePlayerLifetime life)||life.mode!=NativePlayerLifetime.Mode.OPEN||life.admission==null||(boolean)CombatAccess.field(c.hub,"nativeStaticMotionClosed"))return false;
  var source=NativeSourceMaterialFirstLife.member(c.pc,c);if(source==null)return false;ClientSession peer=null;for(var pc:c.players){var b=c.bindings.get(pc);if(b!=null&&b.object()==id){peer=pc;break;}}
  var npc=c.npcs.get(id);Object actorTimed=null,actorLife=null,generation=null;if(npc!=null){if(npc.kind()==3&&npc.owner() instanceof ClientSession owner&&npc.actor() instanceof BattleCompanion){peer=owner;actorTimed=timed(npc.actor(),false);actorLife=NativeSourceNpcFirstLife.first(npc.actor(),npc,c);}else if(npc.kind()==1||npc.kind()==2){peer=c.pc;actorTimed=timed(npc.actor(),true);actorLife=npc.epoch();generation=CombatAccess.field(npc.owner(),"sourceProcEncounterGeneration");}else return false;}
  if(peer==null)return false;var member=NativeSourceMaterialFirstLife.member(peer,c);if(member==null)return false;var p=new Proof(life,source,timed(c.pc,false),peer,member,timed(peer,false),npc,actorTimed,actorLife,generation);if(!current(c,p))return false;c.nativeSourceTargetNegativeProof=p;return true;
 }
 static boolean player(NativeSourceRawDamage.Member m,Object status){return status!=null&&CombatAccess.field(m.pc,"timedStatus")==status&&raw(status)==m.life&&m.acceptedLife==m.life&&m.current(true)&&NativeSourceRawDamage.number(m.pc,"playerHp")>0;}
 static boolean current(NativeAreaMaterialAction.Context c,Proof p){
  if(!enabled(c))return true;if(p==null||CombatAccess.field(c.hub,"sourcePlayerCommitLifetime")!=p.life()||p.life().mode==NativePlayerLifetime.Mode.STOPPED||(boolean)CombatAccess.field(c.hub,"nativeStaticMotionClosed")||!player(p.source(),p.sourceTimed())||!player(p.member(),p.peerTimed()))return false;
  var n=p.npc();if(n==null)return true;if(c.npcs.get(n.id())!=n||p.actorTimed()==null||timed(n.actor(),n.kind()!=3)!=p.actorTimed()||p.actorLife()==null||raw(p.actorTimed())!=p.actorLife())return false;
  if(n.kind()==3)return NativeSourceNpcFirstLife.first(n.actor(),n,c)==p.actorLife()&&NativeSourceControlledDisplay.attached(p.peer(),(BattleCompanion)n.actor())&&NativeSourceRawDamage.number(n.actor(),"hp")>0;
  return n.epoch()==p.actorLife()&&CombatAccess.field(n.owner(),"sourceProcEncounterGeneration")==p.generation()&&NativeSourceRawDamage.number(n.actor(),"hp")>0;
 }
 static boolean current(NativeAreaMaterialAction.Context c){return !enabled(c)||current(c,proof(c));}
 static boolean ready(NativeAreaMaterialAction.Context c,Proof p,BattleCompanion pet,ClientSession owner){return !enabled(c)||p!=null&&p.npc()!=null&&p.npc().kind()==3&&p.npc().actor()==pet&&p.peer()==owner&&current(c,p);}
 static Object firstTimed(Object original,Proof p){return p==null?original:p.actorTimed();}
 static NativePlayerLifetime lifetime(GameHub hub,Proof p){return p==null?NativePlayerLifetime.of(hub):p.life();}
 static void close(NativeAreaMaterialAction.Context c){NativeSourceControlledNegativeFinal.close(c);if(c!=null)c.nativeSourceTargetNegativeProof=null;}
 private NativeSourceTargetNegativeLife(){}
}
