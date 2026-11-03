package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** v11 independent arrow selection/shot ledger. Arrows are not ITEM USE. */
final class ModernAmmoProtocol {
    static final int REQUEST_KIND=36,RESULT_KIND=37,REQUEST_BYTES=56,STATE_BYTES=104,STOCK_BYTES=48;
    enum Action { READ, SELECT }
    enum Status { OK, DENIED, BUSY, UNAVAILABLE }
    enum Reason { NONE, NOT_ACTIVE, DEAD, EMPTY, UNSUPPORTED, NUMERIC }
    record Request(ModernRooms.Scope scope,Action action,long matchId,long generation,long sequence,long itemId) {
        Request {Objects.requireNonNull(scope);Objects.requireNonNull(action);
            if(matchId<1||generation<1||(action==Action.READ?sequence!=0||itemId!=0:sequence<1||itemId<1))throw bad();}
    }
    record Reply(Status status,Reason reason,ModernCombatProtocol.Frame frame) {
        Reply {Objects.requireNonNull(status);Objects.requireNonNull(reason);
            if((status==Status.OK)!=(reason==Reason.NONE)||frame==null&&!(status==Status.UNAVAILABLE&&reason==Reason.NOT_ACTIVE))throw bad();}
    }
    private ModernAmmoProtocol(){}
    static void validateStates(ModernWorldProtocol.Frame world,List<ModernCombatProtocol.Vital> vitals,List<ModernAmmo.State> states) {
        validateStates(world,vitals,states,ModernHonor.Snapshot.disabled());
    }
    static void validateStates(ModernWorldProtocol.Frame world,List<ModernCombatProtocol.Vital> vitals,List<ModernAmmo.State> states,ModernHonor.Snapshot honor) {
        if(states.isEmpty())return;
        if(states.size()!=world.actors().size()||states.size()!=vitals.size())throw bad();
        for(int i=0;i<states.size();i++) {
            var s=states.get(i);var actor=world.actors().get(i);var v=vitals.get(i);
            if(s.entityId()!=actor.entityId()||s.revision()>world.revision()||s.shotCount()>s.attackSequence()
                    ||s.attackSequence()>v.attackSequence()||!actor.member().identity().isHuman()&&(!s.stocks().isEmpty()||s.requestSequence()!=0||s.revision()!=0))throw bad();
            if(s.shotCount()>0) {
                if(v.weaponType()!=4&&v.weaponType()!=13||s.shotRevision()!=v.attackRevision()||s.lastTargetEntityId()!=v.lastTargetEntityId()
                        ||s.lastTargetEntityId()>world.actors().size()&&honor.objective(s.lastTargetEntityId())==null
                        ||!ModernWorldProtocol.contains(world.manifest(),new ModernWorldMap.Cell(s.fromX(),s.fromY()))
                        ||!ModernWorldProtocol.contains(world.manifest(),new ModernWorldMap.Cell(s.toX(),s.toY())))throw bad();
                int target=Math.toIntExact(s.lastTargetEntityId()-1);
                var objective=honor.objective(s.lastTargetEntityId());
                var targetPosition=objective==null?world.actors().get(target).position():objective.cell();
                long targetHurt=objective==null?vitals.get(target).hurtRevision():objective.hurtRevision();
                if(s.shotRevision()==world.revision()&&(actor.position().x()!=s.fromX()||actor.position().y()!=s.fromY()
                        ||targetPosition.x()!=s.toX()||targetPosition.y()!=s.toY()
                        ||!s.hit()&&targetHurt==s.shotRevision()))throw bad();
            } else if((v.weaponType()==4||v.weaponType()==13)&&v.attackRevision()!=0)throw bad();
            if(world.phase()==ModernWorldProtocol.Phase.LOADING||world.phase()==ModernWorldProtocol.Phase.COUNTDOWN) {
                if(s.revision()!=0||s.requestSequence()!=0||s.shotCount()!=0)throw bad();
            }
            if(world.completion()!=null&&s.shotRevision()>Math.incrementExact(world.completion().event().engineRevision()))throw bad();
        }
    }
    static void validate(ModernApplicationProtocol.AmmoResult message)throws ModernApplicationProtocol.ProtocolException {
        if(message.echo()==null||message.reply()==null)fail();var r=message.echo();var reply=message.reply();var f=reply.frame();if(f==null)return;
        if(r.action()==Action.READ&&(reply.status()!=Status.OK||reply.reason()!=Reason.NONE))fail();
        if(f.world().matchId()!=r.matchId()||f.world().generation()!=r.generation()||f.ammo().isEmpty())fail();
        int self=-1;var key=ModernParticipantId.human(r.scope().sourceId(),r.scope().objectId());
        for(int i=0;i<f.world().actors().size();i++)if(f.world().actors().get(i).member().identity().equals(key))self=i;
        if(self<0)fail();var s=f.ammo().get(self);
        if(r.action()==Action.SELECT&&reply.status()==Status.OK) {
            if(f.world().phase()!=ModernWorldProtocol.Phase.ACTIVE||f.vitals().get(self).hp()==0
                    ||s.requestSequence()!=r.sequence()||s.selectionRevision()!=f.world().revision()||s.revision()!=f.world().revision()
                    ||s.selectedItemId()!=r.itemId()||s.selected()==null||s.selected().remaining()==0)fail();
        }
        if(reply.reason()==Reason.DEAD||reply.reason()==Reason.EMPTY||reply.reason()==Reason.UNSUPPORTED||reply.reason()==Reason.NUMERIC) {
            if(r.action()!=Action.SELECT||reply.status()!=Status.DENIED||f.world().phase()!=ModernWorldProtocol.Phase.ACTIVE
                    ||s.requestSequence()!=r.sequence()||s.revision()!=f.world().revision()||s.selectionRevision()==s.revision())fail();
            if(reply.reason()==Reason.DEAD ? f.vitals().get(self).hp()!=0 : f.vitals().get(self).hp()==0)fail();
            if(reply.reason()==Reason.EMPTY&&(ModernAmmo.Kind.find(r.itemId())==null
                    ||s.stocks().stream().anyMatch(x->x.itemId()==r.itemId()&&x.remaining()>0)))fail();
            if(reply.reason()==Reason.UNSUPPORTED&&ModernAmmo.Kind.find(r.itemId())!=null)fail();
        }
        if(reply.reason()==Reason.NOT_ACTIVE&&(reply.status()!=Status.DENIED||r.action()!=Action.SELECT||f.world().phase()==ModernWorldProtocol.Phase.ACTIVE))fail();
    }
    static void putRequest(ByteBuffer out,Request r){var s=r.scope();out.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch())
            .put((byte)s.channelId()).put((byte)r.action().ordinal()).putShort((short)0).putLong(r.matchId()).putLong(r.generation()).putLong(r.sequence()).putLong(r.itemId());}
    static Request getRequest(ByteBuffer in)throws ModernApplicationProtocol.ProtocolException {
        var s=new ModernRooms.Scope(in.getInt(),in.getLong(),in.getLong(),u8(in));var a=at(Action.values(),u8(in));zero(in,2);
        return new Request(s,a,in.getLong(),in.getLong(),in.getLong(),in.getLong());
    }
    static void putResult(ByteBuffer out,ModernApplicationProtocol.AmmoResult r,int version)throws ModernApplicationProtocol.ProtocolException {
        if(version<11)fail();validate(r);putRequest(out,r.echo());var p=r.reply();out.put((byte)p.status().ordinal()).put((byte)p.reason().ordinal()).put((byte)(p.frame()==null?0:1)).put((byte)0);
        if(p.frame()!=null)ModernCombatProtocol.putFrame(out,p.frame(),version);
    }
    static ModernApplicationProtocol.AmmoResult getResult(ByteBuffer in,int id,int version)throws ModernApplicationProtocol.ProtocolException {
        if(version<11)fail();var echo=getRequest(in);var status=at(Status.values(),u8(in));var reason=at(Reason.values(),u8(in));int p=u8(in);if(p>1)fail();zero(in,1);
        var result=new ModernApplicationProtocol.AmmoResult(id,echo,new Reply(status,reason,p==0?null:ModernCombatProtocol.getFrame(in,version)));validate(result);return result;
    }
    static void putFacts(ByteBuffer out,ModernAmmo.Facts f){out.putLong(f.itemId()).putLong(f.small()).putLong(f.large()).putInt(f.rawArrowSprite()).put((byte)(f.consume()?1:0)).put((byte)f.material()).put((byte)f.blessing()).put((byte)0);}
    static ModernAmmo.Facts getFacts(ByteBuffer in)throws ModernApplicationProtocol.ProtocolException {
        long id=in.getLong(),small=in.getLong(),large=in.getLong();int sprite=in.getInt(),consume=u8(in),material=u8(in),bless=u8(in);if(consume>1)fail();zero(in,1);
        return new ModernAmmo.Facts(id,small,large,sprite,consume==1,material,bless);
    }
    static void putStates(ByteBuffer out,List<ModernAmmo.State> states) {
        out.putShort((short)states.size());for(int i=0;i<6;i++)out.put((byte)0);
        for(var s:states) {
            out.putLong(s.entityId()).putLong(s.requestSequence()).putLong(s.revision()).putLong(s.selectionRevision()).putLong(s.selectedItemId())
                    .putLong(s.shotCount()).putLong(s.shotRevision()).putLong(s.shotItemId()).putLong(s.attackSequence()).putLong(s.lastTargetEntityId())
                    .putInt(s.fromX()).putInt(s.fromY()).putInt(s.toX()).putInt(s.toY()).putInt(s.projectileSprite()).put((byte)(s.hit()?1:0)).put((byte)s.stocks().size()).putShort((short)0);
            for(var stock:s.stocks()){var f=stock.facts();out.putLong(f.itemId()).putLong(stock.initial()).putLong(stock.remaining()).putLong(f.small()).putLong(f.large())
                    .putInt(f.rawArrowSprite()).put((byte)(f.consume()?1:0)).put((byte)f.material()).put((byte)f.blessing()).put((byte)0);}
        }
    }
    static List<ModernAmmo.State> getStates(ByteBuffer in,int actors)throws ModernApplicationProtocol.ProtocolException {
        int count=Short.toUnsignedInt(in.getShort());zero(in,6);if(count>100||count!=0&&count!=actors||in.remaining()<count*STATE_BYTES)fail();
        var result=new ArrayList<ModernAmmo.State>(count);
        for(int i=0;i<count;i++) {
            long entity=in.getLong(),seq=in.getLong(),rev=in.getLong(),selection=in.getLong(),selected=in.getLong(),shots=in.getLong(),shotRev=in.getLong(),shotItem=in.getLong(),attack=in.getLong(),target=in.getLong();
            int x=in.getInt(),y=in.getInt(),tx=in.getInt(),ty=in.getInt(),sprite=in.getInt(),hit=u8(in),stocks=u8(in);zero(in,2);
            if(hit>1||stocks>2||in.remaining()<stocks*STOCK_BYTES)fail();var rows=new ArrayList<ModernAmmo.Stock>(stocks);
            for(int j=0;j<stocks;j++) {
                long id=in.getLong(),initial=in.getLong(),remaining=in.getLong(),small=in.getLong(),large=in.getLong();int raw=in.getInt(),consume=u8(in),material=u8(in),bless=u8(in);zero(in,1);if(consume>1)fail();
                rows.add(new ModernAmmo.Stock(new ModernAmmo.Facts(id,small,large,raw,consume==1,material,bless),initial,remaining));
            }
            result.add(new ModernAmmo.State(entity,seq,rev,selection,selected,shots,shotRev,shotItem,attack,target,x,y,tx,ty,sprite,hit==1,rows));
        }
        return List.copyOf(result);
    }
    private static int u8(ByteBuffer in){return Byte.toUnsignedInt(in.get());}
    private static void zero(ByteBuffer in,int count)throws ModernApplicationProtocol.ProtocolException{for(int i=0;i<count;i++)if(in.get()!=0)fail();}
    private static <T>T at(T[] values,int n)throws ModernApplicationProtocol.ProtocolException{if(n>=values.length)fail();return values[n];}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid arrow wire value");}
    private static void fail()throws ModernApplicationProtocol.ProtocolException{throw new ModernApplicationProtocol.ProtocolException();}
}
