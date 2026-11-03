package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/** v13 append-only objective/life/healing proof. Never changes frozen participant identity. */
final class ModernHonorProtocol {
    static final int OBJECTIVE_BYTES=96,LIFE_BYTES=64,HEAL_BYTES=88;
    static void validate(ModernWorldProtocol.Frame world,List<ModernCombatProtocol.Vital> vitals,
            List<ModernDamageSpellProtocol.State> damages,ModernHonor.Snapshot state) {
        validate(world,vitals,damages,state,List.of());
    }
    static void validate(ModernWorldProtocol.Frame world,List<ModernCombatProtocol.Vital> vitals,
            List<ModernDamageSpellProtocol.State> damages,ModernHonor.Snapshot state,List<ModernFireStormProtocol.State> fires) {
        if(!state.enabled())return;
        int n=world.actors().size();
        if(world.room().summary().config().mode()!=5||state.lives().size()!=n||world.casts().size()!=n)throw bad();
        boolean initial=world.phase()==ModernWorldProtocol.Phase.LOADING||world.phase()==ModernWorldProtocol.Phase.COUNTDOWN;
        for(var o:state.objectives()) {
            if(!ModernWorldProtocol.contains(world.manifest(),o.cell())||o.revision()>world.revision()
                    ||world.actors().stream().anyMatch(a->a.position().equals(o.cell()))||initial&&o.revision()!=0)throw bad();
            if(o.lastActorEntityId()>0) {
                int a=Math.toIntExact(o.lastActorEntityId()-1),team=world.actors().get(a).member().team();
                if((o.lastKind()==3)!=(team==o.team()))throw bad();
                if(o.lastKind()==4&&(fires.size()!=n||fires.get(a).castRevision()<o.revision()))throw bad();
                if(o.revision()==world.revision()) {
                    if(o.lastKind()==1&&(vitals.get(a).attackRevision()!=o.revision()||vitals.get(a).lastTargetEntityId()!=o.entityId())
                            ||o.lastKind()==2&&(damages.isEmpty()||damages.get(a).castRevision()!=o.revision()||damages.get(a).targetEntityId()!=o.entityId())
                            ||o.lastKind()==3&&(state.heals().get(a).revision()!=o.revision()||state.heals().get(a).targetEntityId()!=o.entityId())
                            ||o.lastKind()==4&&(fires.get(a).castRevision()!=o.revision()||fires.get(a).targets().stream().noneMatch(t->t.entityId()==o.entityId())))throw bad();
                }
            }
        }
        for(int i=0;i<n;i++) {
            var l=state.lives().get(i);var h=state.heals().get(i);var v=vitals.get(i);var a=world.actors().get(i);var cast=world.casts().get(i);
            if(l.lastDeathRevision()!=v.deathRevision()||l.lastDeathRevision()>world.revision()||l.respawnRevision()>world.revision()
                    ||(v.hp()==0)!=(l.deathCount()>l.respawnCount())||Math.multiplyExact(l.respawnCount(),v.maxHp())!=l.respawnHpGain()
                    ||initial&&(l.deathCount()!=0||h.castCount()!=0)||world.phase()!=ModernWorldProtocol.Phase.ACTIVE&&l.pendingDueMillis()!=0
                    ||world.phase()==ModernWorldProtocol.Phase.ACTIVE&&v.hp()==0&&l.pendingDueMillis()==0
                    ||l.respawnRevision()==world.revision()&&(v.hp()!=v.maxHp()||a.position().x()!=l.deathX()||a.position().y()!=l.deathY())
                    ||h.revision()>world.revision()||Math.addExact(Math.addExact(h.castCount(),cast.castCount()),damages.isEmpty()?0:damages.get(i).castCount())>cast.requestSequence())throw bad();
            if(!a.member().identity().isHuman()&&!h.equals(ModernHonor.Heal.initial(a.entityId(),0)))throw bad();
            if(h.castCount()>0) {
                var target=state.objective(h.targetEntityId());
                if(target==null||target.team()!=a.member().team()||h.toX()!=target.x()||h.toY()!=target.y()
                        ||target.revision()<h.revision()||target.healGain()<h.restored()
                        ||h.hpAfter()>target.maximumHp()||!ModernWorldProtocol.contains(world.manifest(),new ModernWorldMap.Cell(h.fromX(),h.fromY())))throw bad();
                if(h.revision()==world.revision()&&(target.revision()!=h.revision()||target.lastKind()!=3||target.hp()!=h.hpAfter()
                        ||target.lastActorEntityId()!=a.entityId()||a.position().x()!=h.fromX()||a.position().y()!=h.fromY()))throw bad();
            }
        }
        if(world.completion()!=null) {
            var event=world.completion().event();long end=Math.incrementExact(event.engineRevision());
            var dead=state.objectives().stream().filter(o->o.hp()==0).toList();
            if(event.mode()!=5||dead.size()!=1||dead.get(0).deathRevision()!=end)throw bad();
            for(var p:event.participants())if((p.verdict()==ModernMatchSettlement.Verdict.LOSS)!=(p.team()==dead.get(0).team()))throw bad();
            for(var o:state.objectives())if(o.revision()>end)throw bad();
            for(var l:state.lives())if(l.respawnRevision()>end||l.lastDeathRevision()>end)throw bad();
            for(var h:state.heals())if(h.revision()>end)throw bad();
        } else if(state.objectives().stream().anyMatch(o->o.hp()==0))throw bad();
    }
    static void put(ByteBuffer out,ModernHonor.Snapshot s) {
        out.put((byte)(s.enabled()?1:0)).put((byte)s.objectives().size()).putShort((short)s.lives().size()).putShort((short)s.heals().size()).putShort((short)0);
        for(var o:s.objectives()) {
            out.putLong(o.entityId()).put((byte)o.team()).put((byte)o.action()).putShort((short)0).putInt(o.graphic()).putInt(o.x()).putInt(o.y()).putLong(o.hp()).putLong(o.maximumHp())
                    .putLong(o.revision()).putLong(o.hurtRevision()).putLong(o.deathRevision()).putLong(o.damageLoss()).putLong(o.healGain()).putLong(o.lastActorEntityId()).put((byte)o.lastKind());zero(out,7);
        }
        for(var l:s.lives())out.putLong(l.entityId()).putLong(l.deathCount()).putLong(l.lastDeathRevision()).putLong(l.respawnCount()).putLong(l.respawnRevision()).putLong(l.respawnHpGain()).putLong(l.pendingDueMillis()).putInt(l.deathX()).putInt(l.deathY());
        for(var h:s.heals())out.putLong(h.entityId()).putLong(h.castCount()).putLong(h.revision()).putLong(h.mpSpent()).putLong(h.targetEntityId()).putLong(h.hpBefore()).putLong(h.hpAfter()).putLong(h.restored()).putInt(h.fromX()).putInt(h.fromY()).putInt(h.toX()).putInt(h.toY()).putInt(h.skillId()).putInt(h.mpCost());
    }
    static ModernHonor.Snapshot get(ByteBuffer in,int actors)throws ModernApplicationProtocol.ProtocolException {
        int enabled=Byte.toUnsignedInt(in.get()),oc=Byte.toUnsignedInt(in.get()),lc=Short.toUnsignedInt(in.getShort()),hc=Short.toUnsignedInt(in.getShort());checkZero(in,2);
        if(enabled>1||enabled==0&&(oc!=0||lc!=0||hc!=0)||enabled==1&&(oc!=2||lc!=actors||hc!=actors||actors<2||actors>100)
                ||in.remaining()<oc*OBJECTIVE_BYTES+lc*LIFE_BYTES+hc*HEAL_BYTES)fail();
        var objectives=new ArrayList<ModernHonor.Objective>();var lives=new ArrayList<ModernHonor.Life>();var heals=new ArrayList<ModernHonor.Heal>();
        for(int i=0;i<oc;i++){long id=in.getLong();int team=Byte.toUnsignedInt(in.get()),action=Byte.toUnsignedInt(in.get());checkZero(in,2);
            int gfx=in.getInt(),x=in.getInt(),y=in.getInt();long hp=in.getLong(),max=in.getLong(),rev=in.getLong(),hurt=in.getLong(),death=in.getLong(),damage=in.getLong(),gain=in.getLong(),actor=in.getLong();int kind=Byte.toUnsignedInt(in.get());checkZero(in,7);
            objectives.add(new ModernHonor.Objective(id,team,action,gfx,x,y,hp,max,rev,hurt,death,damage,gain,actor,kind));}
        for(int i=0;i<lc;i++)lives.add(new ModernHonor.Life(in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getInt(),in.getInt()));
        for(int i=0;i<hc;i++)heals.add(new ModernHonor.Heal(in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getInt(),in.getInt(),in.getInt(),in.getInt(),in.getInt(),in.getInt()));
        return new ModernHonor.Snapshot(objectives,lives,heals);
    }
    private static void zero(ByteBuffer out,int n){for(int i=0;i<n;i++)out.put((byte)0);}
    private static void checkZero(ByteBuffer in,int n)throws ModernApplicationProtocol.ProtocolException{for(int i=0;i<n;i++)if(in.get()!=0)fail();}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid honor proof");}
    private static void fail()throws ModernApplicationProtocol.ProtocolException{throw new ModernApplicationProtocol.ProtocolException();}
    private ModernHonorProtocol(){}
}
