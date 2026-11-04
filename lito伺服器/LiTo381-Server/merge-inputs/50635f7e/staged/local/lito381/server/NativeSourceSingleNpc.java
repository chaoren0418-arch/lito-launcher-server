package local.lito381.server;
import java.io.*;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
/** Single-target NPC magic carries only already calculated target mutations/heal into the original FIFO. */
final class NativeSourceSingleNpc {
 static final String P="local/lito381/server/";
 record Single(BattleSpellCatalog.AttackSpell spell,BattleEraseMagicStatus.Lease erase,int confusionSeconds,int thunderSeconds,int heal,AtomicBoolean applied){
  Single(BattleSpellCatalog.AttackSpell spell,BattleEraseMagicStatus.Lease erase,int confusion,int thunder,int heal){this(spell,erase,Math.max(0,confusion),Math.max(0,thunder),Math.max(0,heal),new AtomicBoolean());}
 }
 record Output(NativeSourcePhysicalNpc.Commit commit,ClientSession receiver,NativePlayerCommit.Binding binding,byte[] packet,String note,boolean own){}
 static boolean enter(ClientSession pc,Object[] args,String original,String descriptor,boolean physical)throws IOException{
  if(physical||!original.equals("nativeSourceControlledOriginal_useAttackSpell")||!NativeSourceDollCommit.enabled(pc))return false;
  var old=NativeAreaMaterialAction.current.get();if(old!=null&&old.pc==pc&&old.nativeSourceMagicSnapshot instanceof NativeSourceMagicNpc.Snapshot){NativeSourceControlledDisplay.nativeSourceMagicOriginalEnter(pc,args,original,descriptor,false);return true;}
  var c=new NativeAreaMaterialAction.Context(pc);NativeAreaMaterialAction.owned(c,()->NativeSourceControlledDisplay.nativeSourceMagicOriginalEnter(pc,args,original,descriptor,false));return true;
 }
 static boolean before(ClientSession pc,Protocol.AttackSpellRequest request){
  if(!NativeSourceDollCommit.enabled(pc))return true;if(request==null)return false;var c=NativeAreaMaterialAction.current.get();if(c==null||c.pc!=pc||!(c.nativeSourceMagicSnapshot instanceof NativeSourceMagicNpc.Snapshot snapshot)||!NativePlayerLifetime.of(c.hub).admits()||!snapshot.source().valid(snapshot.source().life())||!c.scene()||(boolean)CombatAccess.field(c.hub,"nativeStaticMotionClosed")||NativeSourceDamageDisplay.n(pc,"playerHp")<=0||(boolean)CombatAccess.field(pc,"observer"))return false;
  var target=snapshot.targets().get(request.targetObjectId());if(target==null)return true;var aim=NativeSourceMagicNpc.aim(pc,request.targetObjectId(),target.actor().kind()==1);if(aim==null)return false;synchronized(c.hub){synchronized(aim.npc().owner()){return current(aim)&&(int)CombatAccess.field(aim.npc().actor(),"hp")>0;}}
 }
 static Object payload(ClientSession pc){var c=NativeAreaMaterialAction.current.get();return c!=null&&c.pc==pc&&c.nativeSourceMagicAdmission instanceof Single s?s:null;}
 static NativeSourceMagicNpc.Snapshot admission(){var c=NativeAreaMaterialAction.current.get();return c!=null&&c.nativeSourceMagicAdmission instanceof Single&&c.nativeSourceMagicSnapshot instanceof NativeSourceMagicNpc.Snapshot s?s:null;}
 static boolean quest(ClientSession pc,BattleQuestEncounter.Snapshot snapshot,int damage,BattleSpellCatalog.AttackSpell spell,BattleEraseMagicStatus.Lease erase,boolean confusion,BattleSpellCatalog.ConfusionSpell silence,BattleSpellCatalog.ThunderGrabSpell thunder,SourceNpcPhysicalStats stats,int casterLevel,int chanceFixture,int durationFixture)throws IOException{
  if(!NativeSourceDollCommit.enabled(pc)||!queued(spell))return false;if(snapshot==null)return true;return submit(pc,snapshot.objectId(),snapshot,damage,spell,erase,confusion,silence,thunder,stats!=null&&stats.boss(),casterLevel,snapshot.level(),chanceFixture,durationFixture,true);
 }
 static boolean time(ClientSession pc,BattleTimeAttackEncounter.Snapshot snapshot,int damage,BattleSpellCatalog.AttackSpell spell,BattleEraseMagicStatus.Lease erase,boolean confusion,BattleSpellCatalog.ConfusionSpell silence,BattleSpellCatalog.ThunderGrabSpell thunder,SourceNpcPhysicalStats stats,int casterLevel,int chanceFixture,int durationFixture)throws IOException{
  if(!NativeSourceDollCommit.enabled(pc)||!queued(spell))return false;if(snapshot==null)return true;return submit(pc,snapshot.objectId(),snapshot,damage,spell,erase,confusion,silence,thunder,snapshot.finalBoss()||stats!=null&&stats.boss(),casterLevel,snapshot.level(),chanceFixture,durationFixture,false);
 }
 /** These legacy families mutate MP/status before their HP leaf; retain their complete old path until their accepted plans are ported. */
 static boolean queued(BattleSpellCatalog.AttackSpell spell){return spell!=null&&BattleSpellCatalog.mindBreakSpell(spell.skillId())==null&&BattleSpellCatalog.boneBreakSpell(spell.skillId())==null&&BattleSpellCatalog.armBreakerSpell(spell.skillId())==null;}
 static boolean current(NativeSourceMonsterHp.Aim aim){return aim!=null&&NativePlayerLifetime.of(aim.c().hub).processes()&&!(boolean)CombatAccess.field(aim.c().hub,"nativeStaticMotionClosed")&&aim.caster().valid(aim.caster().life())&&NativeSourceDamageDisplay.n(aim.caster().pc(),"playerHp")>0&&!(boolean)CombatAccess.field(aim.caster().pc(),"observer")&&aim.c().scene()&&aim.c().npc(aim.npc().id())&&NativeSourceArenaPoison.rawLife(aim.npc().actor())==aim.npc().epoch()&&NativeProcHealingEncounter.generation(aim.npc().owner())==aim.generation()&&(int)CombatAccess.field(aim.npc().actor(),"x")==aim.x()&&(int)CombatAccess.field(aim.npc().actor(),"y")==aim.y()&&NativeSourceArenaPoison.actor(aim.npc().owner(),aim.npc().id(),aim.npc().kind()==1)==aim.npc().actor();}
 static boolean submit(ClientSession pc,long id,Object snapshot,int damage,BattleSpellCatalog.AttackSpell spell,BattleEraseMagicStatus.Lease erase,boolean confusion,BattleSpellCatalog.ConfusionSpell silence,BattleSpellCatalog.ThunderGrabSpell thunder,boolean boss,int casterLevel,int targetLevel,int chanceFixture,int durationFixture,boolean quest)throws IOException{
  var aim=NativeSourceMagicNpc.aim(pc,id,quest);if(aim==null||spell==null)return true;boolean live;synchronized(aim.c().hub){synchronized(aim.npc().owner()){live=current(aim)&&(int)CombatAccess.field(aim.npc().actor(),"hp")>0;}}if(!live)return true;
  // Fixed Source calculates target mutation inputs before HP enqueue; the accepted callback never rolls again.
  int seconds=thunder!=null&&damage>0?CombatProbability.npcThunderDuration(pc,thunder,casterLevel,targetLevel,boss,chanceFixture,durationFixture,id,quest?"battle-quest":"battle-time-attack",new Object[]{snapshot}):0;
  var plan=new Single(spell,erase,confusion&&silence!=null?silence.durationSeconds():0,seconds,requestedHeal(spell.drainsLife(),damage));
  if(damage<=0){withoutHp(aim,plan);return true;}
  var c=aim.c();var prior=c.nativeSourceMagicAdmission;c.nativeSourceMagicAdmission=plan;try{NativeSourcePhysicalNpc.submit(aim,damage,2,false,"single-magic-"+spell.skillId(),null,null,null);}finally{c.nativeSourceMagicAdmission=prior;}return true;
 }
 static int requestedHeal(boolean drainsLife,int damage){return drainsLife&&damage>0?damage:0;}
 static boolean claim(Single plan,boolean accepted){return plan!=null&&accepted&&plan.applied().compareAndSet(false,true);}
 static void withoutHp(NativeSourceMonsterHp.Aim aim,Single plan){
  NativeSourceMagicNpc.eraseWithoutHp(aim,plan.erase());var life=NativePlayerLifetime.of(aim.c().hub);NativeSourcePlayerActionLock.runPairOrdered(aim.caster().pc(),aim.caster().pc(),()->{synchronized(life.admission){if(!life.admits())return;synchronized(aim.c().hub){synchronized(aim.npc().owner()){if(!current(aim))return;applyTargets(aim.c().hub,aim.c().room,aim.npc().kind(),aim.caster().pc(),aim.npc().id(),plan);}}}});
 }
 /** Runs before native wave/end publication, after HP locks were released; exactly once for this accepted event. */
 static void early(NativeSourcePhysicalNpc.Commit p){NativeSourceTargetNpc.early(p);if(p==null||!(p.nativeSourceMagicAfter instanceof Single plan)||!claim(plan,p.accepted))return;var pc=p.origin.source().pc();try{NativeSourcePlayerActionLock.runPairOrdered(pc,pc,()->{synchronized(p.lifetime.admission){if(!p.current()||!NativeSourcePhysicalNpc.sourceCurrent(p.origin))return;try{NativeLightAction.commitSelf(pc,()->release(p,plan));}catch(IOException failure){throw new UncheckedIOException(failure);}}});}catch(RuntimeException failure){NativeSourcePhysicalNpc.failed(p.lifetime);}}
 static void release(NativeSourcePhysicalNpc.Commit p,Single plan){
  try{NativeSourceDamageDisplay.publish(p.visual);}catch(RuntimeException failure){NativeSourcePhysicalNpc.failed(p.lifetime);}
  var s=p.origin.scene();try{synchronized(s.hub()){synchronized(p.target.owner()){if(p.current()&&NativeSourcePhysicalNpc.sourceCurrent(p.origin)){
   if(plan.erase()!=null)((BattleEraseMagicStatus)CombatAccess.field(p.target.actor(),"eraseMagicStatus")).consume(plan.erase());
   if((int)CombatAccess.field(p.target.actor(),"hp")>0&&NativeSourceArenaPoison.actor(p.target.owner(),p.target.id(),p.target.kind()==1)==p.target.actor())applyTargets(s.hub(),s.id(),p.target.kind(),p.origin.source().pc(),p.target.id(),plan);
  }}}}catch(RuntimeException failure){NativeSourcePhysicalNpc.failed(p.lifetime);}
  // Retained original capped HP mutation/output sibling; fatal target acceptance still earns the original heal.
  if(plan.heal()>0&&p.current()&&NativeSourcePhysicalNpc.sourceCurrent(p.origin))try{ClientSession.nativeSourceSingleNpcLifeDrain(p.origin.source().pc(),plan.spell(),plan.heal(),"source-single-NPC",p);}catch(IOException|RuntimeException failure){NativeSourcePhysicalNpc.failed(p.lifetime);}
 }
 static void applyTargets(GameHub hub,long room,int kind,ClientSession pc,long id,Single plan){long now=System.nanoTime();if(plan.confusionSeconds()>0){if(kind==1)hub.confusionSilenceQuestMonster(room,pc,id,now,plan.confusionSeconds());else hub.confusionSilenceTimeAttackActor(room,pc,id,now,plan.confusionSeconds());}if(plan.thunderSeconds()>0){if(kind==1)hub.thunderGrabQuestMonster(room,pc,id,now,plan.thunderSeconds());else hub.thunderGrabTimeAttackActor(room,pc,id,now,plan.thunderSeconds());}}
 static boolean current(Output out){var p=out.commit();var source=p.origin.source();var s=p.origin.scene();if(!p.accepted||p.lifetime.mode==NativePlayerLifetime.Mode.STOPPED||(boolean)CombatAccess.field(s.hub(),"nativeStaticMotionClosed")||!NativeSourcePhysicalNpc.sourceCurrent(p.origin)||((Map<?,?>)CombatAccess.field(s.hub(),"rooms")).get(s.id())!=s.room()||!NativeSourcePhysicalNpc.receiver(p,out.receiver(),out.binding()))return false;
  int hp=NativeSourceDamageDisplay.n(source.pc(),"playerHp"),max=NativeSourceDamageDisplay.n(source.pc(),"playerMaxHp");var b=out.packet();if(out.own())return out.receiver()==source.pc()&&b.length==5&&(b[0]&255)==Protocol.S_HIT_POINT&&((b[1]&255)|((b[2]&255)<<8))==(hp&65535)&&((b[3]&255)|((b[4]&255)<<8))==(max&65535);
  return b.length==6&&(b[0]&255)==Protocol.S_HIT_RATIO&&NativeSourcePhysicalNpc.object(b)==source.binding().object()&&(b[5]&255)==Protocol.healthRatio(hp,max);
 }
 static void output(NativeSourcePhysicalNpc.Commit p,ClientSession pc,byte[] packet,String note,boolean own){var binding=own?p.origin.source().binding():p.viewers.get(pc);var out=new Output(p,pc,binding,packet.clone(),note,own);NativeLightAction.deferScoped(()->{try{var c=new NativeAreaMaterialAction.Context(pc);var prior=c.nativeSourceSingleNpcOutput;c.nativeSourceSingleNpcOutput=out;try{NativeAreaMaterialAction.owned(c,()->{synchronized(pc){if(current(out))pc.nativeSelfOriginalSend(out.packet(),out.note());}});}finally{c.nativeSourceSingleNpcOutput=prior;}}catch(IOException|RuntimeException failure){NativeSourcePhysicalNpc.failed(p.lifetime);}});}
 static void sendSelf(ClientSession pc,byte[] packet,String note,NativeSourcePhysicalNpc.Commit p){if(p!=null&&pc==p.origin.source().pc())output(p,pc,packet,note,true);}
 static void broadcast(ClientSession pc,String note,NativeSourcePhysicalNpc.Commit p){if(p==null||pc!=p.origin.source().pc())return;var packet=Protocol.hitRatio(p.origin.source().binding().object(),NativeSourceDamageDisplay.n(pc,"playerHp"),NativeSourceDamageDisplay.n(pc,"playerMaxHp"));for(var receiver:p.members)if(receiver!=pc)output(p,receiver,packet,note,false);}
 static void inherit(NativeAreaMaterialAction.Context child,NativeAreaMaterialAction.Context parent){if(parent!=null)child.nativeSourceSingleNpcOutput=parent.nativeSourceSingleNpcOutput;}
 static void inherit(NativeAreaMaterialAction.Context child){inherit(child,NativeAreaMaterialAction.current.get());}
 static boolean allowFrame(ClientSession pc,byte[] packet){var c=NativeAreaMaterialAction.current.get();return c==null||!(c.nativeSourceSingleNpcOutput instanceof Output out)||out.receiver()==pc&&current(out);}
 private NativeSourceSingleNpc(){}
}
