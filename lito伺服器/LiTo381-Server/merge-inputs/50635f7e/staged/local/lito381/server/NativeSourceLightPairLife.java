package local.lito381.server;
/** Freeze synchronous shared pair task identities before receipts and action-lock waits. */
final class NativeSourceLightPairLife {
 record Actor(ClientSession pc,NativePlayerCommit.Binding binding,Object life,boolean dead){
  boolean current(GameHub hub){return binding!=null&&life!=null&&NativeAcceptedFreezeMutation.roleFrame(binding,pc)&&NativeSourceControlledDisplay.rawLife(pc)==life&&CombatAccess.field(pc,"hub")==hub&&!(boolean)CombatAccess.field(pc,"sourceItemDisconnected")&&(!dead||NativeSourceRawDamage.number(pc,"playerHp")==0);}
 }
 record Proof(Actor caster,Actor target,GameHub hub,NativePlayerLifetime lifetime){
  boolean current(){return caster!=null&&target!=null&&lifetime.mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(hub,"nativeStaticMotionClosed")&&caster.current(hub)&&target.current(hub);}
  boolean locked(){return current()&&caster.pc().nativePlayerActionLock().isHeldByCurrentThread()&&target.pc().nativePlayerActionLock().isHeldByCurrentThread();}
 }
 static Actor copy(NativeSourceAcceptedPlayers.Participant p){return new Actor(p.pc(),p.binding(),p.life(),p.dead());}
 static Actor actor(ClientSession pc,NativeAreaMaterialAction.Context context,NativePlayerCommit.Frame frame){
  if(frame!=null&&frame.accepted&&!frame.event.world()&&frame.nativeSourceAcceptedPlayers instanceof NativeSourceAcceptedPlayers.Proof p){if(p.target()!=null&&p.target().pc()==pc)return copy(p.target());if(p.source()!=null&&p.source().pc()==pc)return copy(p.source());}
  NativeSourceRawDamage.Member m;
  if(context!=null&&"BATTLE".equals(context.phase)){
   if(!NativeSourcePlayerFactories.scoped(context))return null;
   var binding=pc==context.pc?context.caster:context.bindings.get(pc);if(binding==null)return null;
   m=NativeSourceSignedRecovery.member(pc,binding,context);
  }else m=NativeSourceAcceptedPlayers.ingress(pc);
  return m==null?null:new Actor(pc,m.binding,m.acceptedLife,m.dead);
 }
 static Proof capture(ClientSession caster,ClientSession target){var context=NativeAreaMaterialAction.current.get();var frame=NativePlayerCommit.frame();var source=actor(caster,context,frame);var peer=caster==target?source:actor(target,context,frame);var hub=(GameHub)CombatAccess.field(caster,"hub");return new Proof(source,peer,hub,NativePlayerLifetime.of(hub));}

    static NativeSelfCastAction.Task task(ClientSession clientSession, ClientSession clientSession2, NativeSelfCastAction.Task task) {
        return NativeSourceMaterialPairGuard.task(clientSession, clientSession2, task);
    }
 private NativeSourceLightPairLife(){}

    static NativeSelfCastAction.Task nativeSourceMaterialOriginalTask(ClientSession clientSession, ClientSession clientSession2, NativeSelfCastAction.Task task) {
        if (!NativeSourcePairedScope.enabled(clientSession) || clientSession2 == null) {
            return task;
        }
        Proof proof = NativeSourceLightPairLife.capture(clientSession, clientSession2);
        return () -> {
            if (proof.locked()) {
                task.run();
            }
        };
    }
}
