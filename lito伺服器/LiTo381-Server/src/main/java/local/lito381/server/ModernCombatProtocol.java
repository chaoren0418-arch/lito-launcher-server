package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Atomic authority world/vitals snapshots; requests contain no client damage or dice. */
final class ModernCombatProtocol {
    static final int REQUEST_KIND=22,RESULT_KIND=23,REQUEST_BYTES=56,MAX_FRAME_BYTES=128*1024;
    static int maxFrameBytes(int version){return version>=14?1024*1024:version>=10?256*1024:MAX_FRAME_BYTES;}
    enum Action { READ, ATTACK }
    enum Status { OK, DENIED, BUSY, UNAVAILABLE }
    enum Reason { NONE, NOT_ACTIVE, DEAD, TARGET_DEAD, SAME_TEAM, OUT_OF_RANGE, BLOCKED, TOO_FAST, UNSUPPORTED, NUMERIC, TOO_HEAVY, STALE_EFFECT, NO_AMMO, STALE_REGEN }
    record Request(ModernRooms.Scope scope,Action action,long matchId,long generation,long sequence,long targetEntityId) {
        Request {
            Objects.requireNonNull(scope);Objects.requireNonNull(action);
            if(matchId<1 || generation<1 || (action==Action.READ ? sequence!=0 || targetEntityId!=0 : sequence<1 || targetEntityId<1))throw bad();
        }
    }
    record Vital(long entityId,long hp,long maxHp,long mp,long maxMp,long ac,int weaponType,
                 long attackSequence,long attackRevision,long hurtRevision,long deathRevision,long lastTargetEntityId,long attackIntervalNanos) {
        Vital {
            if(entityId<1 || maxHp<1 || hp<0 || hp>maxHp || maxMp<0 || mp<0 || mp>maxMp
                    || weaponType<0 || weaponType>18 || attackSequence<0 || attackRevision<0 || hurtRevision<0 || deathRevision<0
                    || hp==0&&deathRevision==0 || lastTargetEntityId<0
                    || (attackRevision==0)!=(lastTargetEntityId==0)
                    || attackIntervalNanos<1_000_000 || attackIntervalNanos>10_000_000_000L)throw bad();
        }
    }
    record Frame(ModernWorldProtocol.Frame world,List<Vital> vitals,List<ModernDamageSpellProtocol.State> damages,
                 List<ModernItemProtocol.State> items, List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo,List<ModernRegenProtocol.State> regen,ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fireStorms,List<ModernNpcMagic.CastState> npcSpells,List<ModernDisintegrateState.State> disintegrations,List<ModernNpcDisintegrateState.State> npcDisintegrations,List<ModernNpcFireStormState.State> npcFireStorms) {
        Frame(ModernWorldProtocol.Frame world,List<Vital> vitals,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
              List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo,List<ModernRegenProtocol.State> regen,
              ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fireStorms,List<ModernNpcMagic.CastState> npcSpells,List<ModernDisintegrateState.State> disintegrations,List<ModernNpcDisintegrateState.State> npcDisintegrations){
            this(world,vitals,damages,items,weights,buffs,ammo,regen,honor,fireStorms,npcSpells,disintegrations,npcDisintegrations,List.of());
        }
        Frame(ModernWorldProtocol.Frame world,List<Vital> vitals,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
              List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo,List<ModernRegenProtocol.State> regen,
              ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fireStorms,List<ModernNpcMagic.CastState> npcSpells,List<ModernDisintegrateState.State> disintegrations){
            this(world,vitals,damages,items,weights,buffs,ammo,regen,honor,fireStorms,npcSpells,disintegrations,List.of());
        }
        Frame(ModernWorldProtocol.Frame world,List<Vital> vitals,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
              List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo,List<ModernRegenProtocol.State> regen,
              ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fireStorms,List<ModernNpcMagic.CastState> npcSpells){
            this(world,vitals,damages,items,weights,buffs,ammo,regen,honor,fireStorms,npcSpells,List.of());
        }
        Frame(ModernWorldProtocol.Frame world,List<Vital> vitals,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
              List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo,List<ModernRegenProtocol.State> regen,ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fireStorms){
            this(world,vitals,damages,items,weights,buffs,ammo,regen,honor,fireStorms,List.of());
        }
        Vital vital(long entityId){return vitals.stream().filter(v->v.entityId()==entityId).findFirst().orElseThrow(ModernCombatProtocol::bad);}
        Frame(ModernWorldProtocol.Frame world,List<Vital> vitals,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
              List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo,List<ModernRegenProtocol.State> regen,ModernHonor.Snapshot honor){
            this(world,vitals,damages,items,weights,buffs,ammo,regen,honor,List.of());
        }
        Frame(ModernWorldProtocol.Frame world,List<Vital> vitals,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
              List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo,List<ModernRegenProtocol.State> regen){this(world,vitals,damages,items,weights,buffs,ammo,regen,ModernHonor.Snapshot.disabled());}
        Frame(ModernWorldProtocol.Frame world,List<Vital> vitals,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
              List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo){this(world,vitals,damages,items,weights,buffs,ammo,List.of());}
        Frame(ModernWorldProtocol.Frame world,List<Vital> vitals,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
              List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs){this(world,vitals,damages,items,weights,buffs,List.of());}
        Frame(ModernWorldProtocol.Frame world,List<Vital> vitals,List<ModernDamageSpellProtocol.State> damages,
              List<ModernItemProtocol.State> items,List<ModernWeightProtocol.State> weights) {this(world,vitals,damages,items,weights,List.of());}
        Frame(ModernWorldProtocol.Frame world,List<Vital> vitals,List<ModernDamageSpellProtocol.State> damages,
              List<ModernItemProtocol.State> items) { this(world,vitals,damages,items,List.of()); }
        Frame(ModernWorldProtocol.Frame world,List<Vital> vitals) { this(world,vitals,List.of()); }
        Frame(ModernWorldProtocol.Frame world,List<Vital> vitals,List<ModernDamageSpellProtocol.State> damages) { this(world,vitals,damages,List.of()); }
        Frame {
            Objects.requireNonNull(world);vitals=List.copyOf(vitals);damages=List.copyOf(damages);items=List.copyOf(items);weights=List.copyOf(weights);buffs=List.copyOf(buffs);ammo=List.copyOf(ammo);regen=List.copyOf(regen);fireStorms=List.copyOf(fireStorms);
            npcSpells=List.copyOf(npcSpells);ModernNpcSpellProtocol.validateStates(world,vitals,npcSpells);
            disintegrations=List.copyOf(disintegrations);
            npcDisintegrations=List.copyOf(npcDisintegrations);
            npcFireStorms=List.copyOf(npcFireStorms);
            if(!disintegrations.isEmpty()&&(world.descriptor()==null||!world.descriptor().has(ModernMatchDescriptor.DISINTEGRATE)
                    ||!npcSpells.isEmpty()||world.siege()!=null||world.timeAttack()!=null||world.quest().enabled()))throw bad();
            if(world.descriptor()!=null){var d=world.descriptor();
                if(!d.has(ModernMatchDescriptor.COMBAT)||d.has(ModernMatchDescriptor.LIGHT_ARROW)!=!damages.isEmpty()
                        ||d.has(ModernMatchDescriptor.ITEMS)!=!items.isEmpty()||d.has(ModernMatchDescriptor.WEIGHT)!=!weights.isEmpty()
                        ||d.has(ModernMatchDescriptor.TIMED)!=!buffs.isEmpty()||d.has(ModernMatchDescriptor.AMMO)!=!ammo.isEmpty()
                        ||d.has(ModernMatchDescriptor.REGEN)!=!regen.isEmpty()||d.has(ModernMatchDescriptor.HONOR)!=honor.enabled()
                        ||d.has(ModernMatchDescriptor.FIRE_STORM)!=!fireStorms.isEmpty()
                        ||d.has(ModernMatchDescriptor.DISINTEGRATE)!=!disintegrations.isEmpty()
                        ||d.has(ModernMatchDescriptor.NPC_DISINTEGRATE)!=!npcDisintegrations.isEmpty()
                        ||d.has(ModernMatchDescriptor.NPC_FIRE_STORM)!=!npcFireStorms.isEmpty())throw bad();}
            if(!npcFireStorms.isEmpty()&&(!disintegrations.isEmpty()||!damages.isEmpty()||!buffs.isEmpty()||!ammo.isEmpty()||!fireStorms.isEmpty()||honor.enabled()||world.siege()!=null))throw bad();
            if(!npcDisintegrations.isEmpty()&&(!disintegrations.isEmpty()||!damages.isEmpty()||!buffs.isEmpty()||!ammo.isEmpty()||!fireStorms.isEmpty()||honor.enabled()||world.siege()!=null))throw bad();
            if(!regen.isEmpty()&&regen.size()!=world.actors().size())throw bad();
            Objects.requireNonNull(honor);
            if(!fireStorms.isEmpty()&&(fireStorms.size()!=world.actors().size()||regen.size()!=world.actors().size()||weights.size()!=world.actors().size()))throw bad();
            if(world.completion()!=null&&world.completion().event().mode()==5&&!honor.enabled())throw bad();
            ModernQuestProtocol.validateCombat(world,vitals);
            ModernTimeAttackProtocol.validateCombat(world,vitals);
            ModernSiegeProtocol.validateCombat(world,vitals);
            if(honor.enabled()&&(regen.size()!=world.actors().size()||weights.size()!=world.actors().size()))throw bad();
            ModernDamageSpellProtocol.validate(world,damages,honor);
            ModernItemProtocol.validateStates(world,vitals,items,regen,honor);
            ModernWeightProtocol.validateStates(world,items,weights,ammo);
            ModernAmmoProtocol.validateStates(world,vitals,ammo,honor);
            ModernBuffProtocol.validateStates(world,vitals,items,buffs);
            ModernRegenProtocol.validateStates(world,vitals,damages,items,regen,honor,fireStorms,npcSpells,disintegrations,npcDisintegrations,npcFireStorms);
            ModernNpcDisintegrateWire.validateStates(world,vitals,items,regen,npcSpells,npcDisintegrations);
            ModernNpcFireStormFrame.validateCombat(world,vitals,items,regen,npcSpells,npcDisintegrations,npcFireStorms);
            ModernHonorProtocol.validate(world,vitals,damages,honor,fireStorms);
            ModernFireStormProtocol.validate(world,vitals,damages,items,regen,honor,fireStorms);
            ModernDisintegrateState.validate(world.revision(),ModernBattleEngine.Phase.valueOf(world.phase().name()),
                    world.room(),world.manifest(),world.actors(),vitals,world.casts(),damages,items,regen,honor,fireStorms,disintegrations);
            if(world.completion()!=null&&disintegrations.stream().anyMatch(s->s.revision()>Math.incrementExact(world.completion().event().engineRevision())))throw bad();
            if(!regen.isEmpty()&&weights.size()!=world.actors().size())throw bad();
            if(!buffs.isEmpty() && weights.size()!=world.actors().size())throw bad();
            if(!ammo.isEmpty()&&buffs.size()!=world.actors().size())throw bad();
            if(world.phase()==ModernWorldProtocol.Phase.PREPARING || vitals.size()!=world.actors().size())throw bad();
            for(int i=0;i<vitals.size();i++) {
                var v=vitals.get(i);
                if(!honor.enabled()&&world.quest().lives().isEmpty()&&world.siege()==null&&(v.hp()==0)!=(v.deathRevision()>0))throw bad();
                if(regen.isEmpty()&&!world.casts().isEmpty()) {
                    long spent=Math.addExact(Math.addExact(world.casts().get(i).mpSpent(),damages.isEmpty()?0:damages.get(i).mpSpent()),npcSpells.isEmpty()?0:npcSpells.get(i).mpSpent());
                    if(spent>v.maxMp() || v.mp()!=v.maxMp()-spent)throw bad();
                }
                if(v.entityId()!=world.actors().get(i).entityId() || v.attackRevision()>world.revision()
                        || v.hurtRevision()>world.revision() || v.deathRevision()>world.revision()
                        || v.attackRevision()>0 && (v.attackSequence()==0 || v.lastTargetEntityId()==v.entityId()
                        || world.actors().stream().noneMatch(a->a.entityId()==v.lastTargetEntityId())&&honor.objective(v.lastTargetEntityId())==null))throw bad();
            }
            if(world.completion()!=null&&world.completion().event().mode()==1) {
                var event=world.completion().event();long finalRevision=Math.addExact(event.engineRevision(),1);
                boolean livingWinner=false,finalDeath=false;
                for(int i=0;i<vitals.size();i++) {
                    var v=vitals.get(i);var p=event.participants().get(i);
                    if(v.attackRevision()>finalRevision || v.hurtRevision()>finalRevision || v.deathRevision()>finalRevision)throw bad();
                    if(p.verdict()==ModernMatchSettlement.Verdict.LOSS) {
                        if(v.hp()!=0)throw bad();if(v.deathRevision()==finalRevision)finalDeath=true;
                    } else if(v.hp()>0)livingWinner=true;
                }
                if(!livingWinner || !finalDeath)throw bad();
            }
        }
    }
    record Reply(Status status,Reason reason,Frame frame) {
        Reply {
            Objects.requireNonNull(status);Objects.requireNonNull(reason);
            if((status==Status.OK)!=(reason==Reason.NONE)
                    || frame==null && !(status==Status.UNAVAILABLE && reason==Reason.NOT_ACTIVE))throw bad();
        }
    }
    private ModernCombatProtocol() {}
    static void validate(ModernApplicationProtocol.CombatResult message) throws ModernApplicationProtocol.ProtocolException {
        if(message.echo()==null || message.reply()==null)fail();
        var request=message.echo();var reply=message.reply();var frame=reply.frame();
        if(frame==null)return;
        var world=frame.world();
        if(world.matchId()!=request.matchId() || world.generation()!=request.generation())fail();
        var key=ModernParticipantId.human(request.scope().sourceId(),request.scope().objectId());
        int self=-1;
        for(int i=0;i<world.actors().size();i++)if(world.actors().get(i).member().identity().equals(key))self=i;
        if(self<0)fail();
        if(reply.reason()==Reason.TOO_HEAVY) {
            if(reply.status()!=Status.DENIED || request.action()!=Action.ATTACK
                    || world.phase()!=ModernWorldProtocol.Phase.ACTIVE || frame.weights().isEmpty()
                    || frame.vitals().get(self).hp()==0
                    || !frame.weights().get(self).snapshot().view().applicable()
                    || frame.weights().get(self).snapshot().view().permitsAttackCast()
                    || frame.vitals().get(self).attackSequence()!=request.sequence())fail();
        }
        if(reply.reason()==Reason.STALE_EFFECT && (reply.status()!=Status.DENIED || request.action()!=Action.ATTACK
                || world.phase()!=ModernWorldProtocol.Phase.ACTIVE || frame.buffs().isEmpty()
                || frame.buffs().get(self).revision()==0
                || frame.vitals().get(self).attackSequence()!=request.sequence() || frame.vitals().get(self).hp()==0))fail();
        if(reply.reason()==Reason.NO_AMMO) {
            var v=frame.vitals().get(self);
            if(reply.status()!=Status.DENIED||request.action()!=Action.ATTACK||world.phase()!=ModernWorldProtocol.Phase.ACTIVE
                    ||v.hp()==0||v.attackSequence()!=request.sequence()||v.weaponType()!=4&&v.weaponType()!=13||frame.ammo().isEmpty())fail();
            var selected=frame.ammo().get(self).selected();if(selected!=null&&selected.remaining()>0)fail();
        }
        if(reply.reason()==Reason.STALE_REGEN) {
            if(reply.status()!=Status.DENIED||request.action()!=Action.ATTACK||world.phase()!=ModernWorldProtocol.Phase.ACTIVE
                    ||frame.regen().isEmpty()||frame.vitals().get(self).hp()==0||frame.vitals().get(self).attackSequence()!=request.sequence()
                    ||frame.vitals().stream().noneMatch(v->v.entityId()==request.targetEntityId()&&v.hp()>0)
                    ||frame.regen().stream().noneMatch(s->s.entityId()==request.targetEntityId()&&s.hpRevision()>0))fail();
        }
        if(request.action()==Action.ATTACK) {
            var vital=frame.vitals().get(self);
            if(request.targetEntityId()==vital.entityId() || world.actors().stream().noneMatch(a->a.entityId()==request.targetEntityId())&&frame.honor().objective(request.targetEntityId())==null)fail();
            if(reply.status()==Status.OK && ((world.phase()!=ModernWorldProtocol.Phase.ACTIVE && world.phase()!=ModernWorldProtocol.Phase.FINISHED)
                    || vital.attackSequence()!=request.sequence() || vital.attackRevision()!=world.revision()
                    || vital.lastTargetEntityId()!=request.targetEntityId()))fail();
        }
    }
    static void putRequest(ByteBuffer out,Request request) {
        var s=request.scope();
        out.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch()).put((byte)s.channelId())
                .put((byte)request.action().ordinal()).putShort((short)0).putLong(request.matchId()).putLong(request.generation())
                .putLong(request.sequence()).putLong(request.targetEntityId());
    }
    static Request getRequest(ByteBuffer in) throws ModernApplicationProtocol.ProtocolException {
        var scope=new ModernRooms.Scope(in.getInt(),in.getLong(),in.getLong(),u8(in));
        var action=at(Action.values(),u8(in));zero(in,2);
        return new Request(scope,action,in.getLong(),in.getLong(),in.getLong(),in.getLong());
    }
    static void putResult(ByteBuffer out,ModernApplicationProtocol.CombatResult result) throws ModernApplicationProtocol.ProtocolException {
        putResult(out,result,1);
    }
    static void putResult(ByteBuffer out,ModernApplicationProtocol.CombatResult result,int version) throws ModernApplicationProtocol.ProtocolException {
        if(version<9 && result.reply().reason()==Reason.TOO_HEAVY)fail();
        if(version<10 && result.reply().reason()==Reason.STALE_EFFECT)fail();
        if(version<11 && result.reply().reason()==Reason.NO_AMMO)fail();
        if(version<12 && result.reply().reason()==Reason.STALE_REGEN)fail();
        putRequest(out,result.echo());var reply=result.reply();
        out.put((byte)reply.status().ordinal()).put((byte)reply.reason().ordinal()).put((byte)(reply.frame()==null?0:1)).put((byte)0);
        if(reply.frame()==null)return;
        putFrame(out,reply.frame(),version);
    }
    static void putFrame(ByteBuffer out,Frame frame,int version) throws ModernApplicationProtocol.ProtocolException {
        if(version<29&&!frame.npcDisintegrations().isEmpty())fail();
        if(version<31&&!frame.npcFireStorms().isEmpty())fail();
        if(version<27&&!frame.disintegrations().isEmpty())fail();
        if(version<24&&!frame.npcSpells().isEmpty())fail();
        if(version<6 && !frame.damages().isEmpty())fail();
        if(version<8 && !frame.items().isEmpty())fail();
        if(version<9 && !frame.weights().isEmpty())fail();
        if(version<10 && !frame.buffs().isEmpty())fail();
        if(version<11 && !frame.ammo().isEmpty())fail();
        if(version<12 && !frame.regen().isEmpty())fail();
        if(version<13&&frame.honor().enabled())fail();
        if(version<14&&(!frame.fireStorms().isEmpty()||frame.honor().objectives().stream().anyMatch(o->o.lastKind()==4)))fail();
        ModernWorldProtocol.putFrame(out,frame.world(),version);
        out.putShort((short)frame.vitals().size()).putShort((short)0);
        for(var v:frame.vitals()) {
            out.putLong(v.entityId()).putLong(v.hp()).putLong(v.maxHp()).putLong(v.mp()).putLong(v.maxMp()).putLong(v.ac())
                    .put((byte)v.weaponType());
            for(int i=0;i<7;i++)out.put((byte)0);
            out.putLong(v.attackSequence()).putLong(v.attackRevision()).putLong(v.hurtRevision()).putLong(v.deathRevision())
                    .putLong(v.lastTargetEntityId()).putLong(v.attackIntervalNanos());
        }
        if(version>=6)ModernDamageSpellProtocol.put(out,frame.damages());
        if(version>=8)ModernItemProtocol.putStates(out,frame.items(),version);
        if(version>=9)ModernWeightProtocol.putStates(out,frame.weights());
        if(version>=10)ModernBuffProtocol.putStates(out,frame.buffs());
        if(version>=11)ModernAmmoProtocol.putStates(out,frame.ammo());
        if(version>=12)ModernRegenProtocol.putStates(out,frame.regen());
        if(version>=13)ModernHonorProtocol.put(out,frame.honor());
        if(version>=14)ModernFireStormProtocol.put(out,frame.fireStorms());
        if(version>=24)ModernNpcSpellProtocol.putStates(out,frame.npcSpells());
        if(version>=27)ModernDisintegrateState.put(out,frame.disintegrations());
        if(version>=29)ModernNpcDisintegrateWire.putStates(out,new ModernNpcRoster.Scope(frame.world().room().summary().id(),frame.world().matchId(),frame.world().generation()),frame.npcDisintegrations(),frame.world().actors().size(),version);
        if(version>=31)ModernNpcFireStormWire.putStates(out,new ModernNpcRoster.Scope(frame.world().room().summary().id(),frame.world().matchId(),frame.world().generation()),frame.npcFireStorms(),frame.world().actors().size(),version);
    }
    static ModernApplicationProtocol.CombatResult getResult(ByteBuffer in,int id) throws ModernApplicationProtocol.ProtocolException {
        return getResult(in,id,1);
    }
    static ModernApplicationProtocol.CombatResult getResult(ByteBuffer in,int id,int version) throws ModernApplicationProtocol.ProtocolException {
        var echo=getRequest(in);var status=at(Status.values(),u8(in));var reason=at(Reason.values(),u8(in));
        if(version<9 && reason==Reason.TOO_HEAVY)fail();
        if(version<10 && reason==Reason.STALE_EFFECT)fail();
        if(version<11 && reason==Reason.NO_AMMO)fail();
        if(version<12 && reason==Reason.STALE_REGEN)fail();
        int present=u8(in);if(present>1)fail();zero(in,1);Frame frame=null;
        if(present==1) frame=getFrame(in,version);
        return new ModernApplicationProtocol.CombatResult(id,echo,new Reply(status,reason,frame));
    }
    static Frame getFrame(ByteBuffer in,int version) throws ModernApplicationProtocol.ProtocolException {
            var world=ModernWorldProtocol.getFrame(in,version);int count=Short.toUnsignedInt(in.getShort());zero(in,2);
            if(count<1 || count>100 || count!=world.actors().size() || in.remaining()<count*104)fail();
            var vitals=new ArrayList<Vital>(count);
            for(int i=0;i<count;i++) {
                long entity=in.getLong(),hp=in.getLong(),maxHp=in.getLong(),mp=in.getLong(),maxMp=in.getLong(),ac=in.getLong();
                int weapon=u8(in);zero(in,7);
                vitals.add(new Vital(entity,hp,maxHp,mp,maxMp,ac,weapon,in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong()));
            }
            var damages=version>=6?ModernDamageSpellProtocol.get(in,count):List.<ModernDamageSpellProtocol.State>of();
            var items=version>=8?ModernItemProtocol.getStates(in,count,version):List.<ModernItemProtocol.State>of();
            var weights=version>=9?ModernWeightProtocol.getStates(in,count):List.<ModernWeightProtocol.State>of();
            var buffs=version>=10?ModernBuffProtocol.getStates(in,count):List.<ModernTimedEffects.State>of();
            var ammo=version>=11?ModernAmmoProtocol.getStates(in,count):List.<ModernAmmo.State>of();
            var regen=version>=12?ModernRegenProtocol.getStates(in,count):List.<ModernRegenProtocol.State>of();
            var honor=version>=13?ModernHonorProtocol.get(in,count):ModernHonor.Snapshot.disabled();
            if(version<14&&honor.objectives().stream().anyMatch(o->o.lastKind()==4))fail();
            var fires=version>=14?ModernFireStormProtocol.get(in,count):List.<ModernFireStormProtocol.State>of();
            var npcSpells=version>=24?ModernNpcSpellProtocol.getStates(in,world):List.<ModernNpcMagic.CastState>of();
            return new Frame(world,vitals,damages,items,weights,buffs,ammo,regen,honor,fires,npcSpells,
                    version>=27?ModernDisintegrateState.get(in,count):List.of(),version>=29?ModernNpcDisintegrateWire.getStates(in,new ModernNpcRoster.Scope(world.room().summary().id(),world.matchId(),world.generation()),count,version):List.of(),
                    version>=31?ModernNpcFireStormWire.getStates(in,new ModernNpcRoster.Scope(world.room().summary().id(),world.matchId(),world.generation()),count,version):List.of());
    }
    private static int u8(ByteBuffer in){return Byte.toUnsignedInt(in.get());}
    private static void zero(ByteBuffer in,int count) throws ModernApplicationProtocol.ProtocolException {for(int i=0;i<count;i++)if(in.get()!=0)fail();}
    private static <T>T at(T[] values,int index) throws ModernApplicationProtocol.ProtocolException {if(index>=values.length)fail();return values[index];}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid combat wire value");}
    private static void fail() throws ModernApplicationProtocol.ProtocolException {throw new ModernApplicationProtocol.ProtocolException();}
}
