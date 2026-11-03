package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** v8 match-local consumables, never source inventory object IDs or client supplied healing. */
final class ModernItemProtocol {
    static final int REQUEST_KIND = 34, RESULT_KIND = 35, REQUEST_BYTES = 56;
    static final int MAX_FRAME_BYTES = ModernCombatProtocol.MAX_FRAME_BYTES;
    static final int STATE_PREFIX_BYTES = 92, STOCK_BYTES = 24, MAX_STOCKS = 3;
    static final int MAX_TIMED_STOCKS = 7;
    static final long MAX_QUANTITY = 1_000_000;
    enum Action { READ, USE }
    enum Status { OK, DENIED, BUSY, UNAVAILABLE }
    enum Reason { NONE, NOT_ACTIVE, DEAD, EMPTY, TOO_FAST, UNSUPPORTED, NUMERIC, CLASS_RESTRICTED, STALE_REGEN }

    /** These bindings are admitted only after the source executor/template is independently qualified. */
    enum HealingKind {
        HEALING(40010, 10, 20, 189, 500, 26, 80),
        EXTRA(40011, 30, 50, 194, 1000, 235, 78),
        GREATER(40012, 60, 80, 197, 1000, 238, 81);
        final long itemId, minimum, maximum;
        final int effect, delayMillis, description, inventoryGraphic;
        HealingKind(long id, long minimum, long maximum, int effect, int delay, int description, int icon) {
            this.itemId=id; this.minimum=minimum; this.maximum=maximum; this.effect=effect;
            this.delayMillis=delay; this.description=description; this.inventoryGraphic=icon;
        }
        static HealingKind find(long itemId) {
            for (var kind : values()) if (kind.itemId == itemId) return kind;
            return null;
        }
    }
    record Request(ModernRooms.Scope scope, Action action, long matchId, long generation, long sequence, long itemId) {
        Request {
            Objects.requireNonNull(scope); Objects.requireNonNull(action);
            if (matchId < 1 || generation < 1 || (action == Action.READ ? sequence != 0 || itemId != 0 : sequence < 1 || itemId < 1)) throw bad();
        }
    }
    record Stock(long itemId, long initial, long remaining) {
        Stock {
            if (HealingKind.find(itemId) == null && ModernTimedEffects.Kind.find(itemId)==null
                    || initial < 1 || initial > MAX_QUANTITY || remaining < 0 || remaining > initial) throw bad();
        }
    }
    record State(long entityId, long requestSequence, long useCount, long useRevision, long itemId,
                 long restored, long hpBefore, long hpAfter, int effect, int delayMillis,
                 int x, int y, long fixedRecovery, List<Stock> stocks) {
        State {
            stocks = List.copyOf(stocks);
            if (entityId < 1 || requestSequence < 0 || useCount < 0 || useCount > requestSequence || useRevision < 0
                    || stocks.size() > MAX_TIMED_STOCKS || restored < 0 || hpBefore < 0 || hpAfter < 0 || fixedRecovery < 0) throw bad();
            long previous = 0, consumed = 0;
            for (var stock : stocks) {
                if (stock.itemId() <= previous) throw bad();
                previous = stock.itemId(); consumed = Math.addExact(consumed, stock.initial() - stock.remaining());
            }
            if (consumed != useCount) throw bad();
            if (useCount == 0) {
                if (useRevision != 0 || itemId != 0 || restored != 0 || hpBefore != 0 || hpAfter != 0
                        || effect != 0 || delayMillis != 0 || x != 0 || y != 0) throw bad();
            } else {
                var kind = HealingKind.find(itemId);
                var timed=ModernTimedEffects.Kind.find(itemId);
                if(useRevision==0 || hpBefore==0 || hpAfter<hpBefore || Math.subtractExact(hpAfter,hpBefore)!=restored)throw bad();
                if(kind!=null) {
                    if(effect!=kind.effect || delayMillis!=kind.delayMillis || restored>kind.maximum && restored-kind.maximum>fixedRecovery)throw bad();
                } else if(timed==null || restored!=0 || hpAfter!=hpBefore || effect!=timed.effect() || delayMillis!=0)throw bad();
                if (stocks.stream().noneMatch(s -> s.itemId() == itemId && s.remaining() < s.initial())) throw bad();
            }
        }
        static State initial(long entityId, long fixedRecovery, List<Stock> stocks) {
            return new State(entityId,0,0,0,0,0,0,0,0,0,0,0,fixedRecovery,stocks);
        }
        State wireRevision() {
            return new State(entityId,requestSequence,useCount,useRevision == 0 ? 0 : Math.addExact(useRevision,1),
                    itemId,restored,hpBefore,hpAfter,effect,delayMillis,x,y,fixedRecovery,stocks);
        }
    }
    record Reply(Status status, Reason reason, ModernCombatProtocol.Frame frame) {
        Reply {
            Objects.requireNonNull(status); Objects.requireNonNull(reason);
            if ((status == Status.OK) != (reason == Reason.NONE)
                    || frame == null && !(status == Status.UNAVAILABLE && reason == Reason.NOT_ACTIVE)) throw bad();
        }
    }
    private ModernItemProtocol() {}

    static void validateStates(ModernWorldProtocol.Frame world, List<ModernCombatProtocol.Vital> vitals, List<State> states) {
        validateStates(world,vitals,states,List.of());
    }
    static void validateStates(ModernWorldProtocol.Frame world,List<ModernCombatProtocol.Vital> vitals,List<State> states,List<ModernRegenProtocol.State> regen) {
        validateStates(world,vitals,states,regen,ModernHonor.Snapshot.disabled());
    }
    static void validateStates(ModernWorldProtocol.Frame world,List<ModernCombatProtocol.Vital> vitals,List<State> states,List<ModernRegenProtocol.State> regen,ModernHonor.Snapshot honor) {
        if (states.isEmpty()) return;
        if (states.size() != world.actors().size() || states.size() != vitals.size()) throw bad();
        for (int i=0;i<states.size();i++) {
            var state=states.get(i); var actor=world.actors().get(i); var vital=vitals.get(i);
            if (state.entityId()!=actor.entityId() || state.useRevision()>world.revision() || state.hpAfter()>vital.maxHp()) throw bad();
            if (!actor.member().identity().isHuman() && (!state.stocks().isEmpty() || state.requestSequence()!=0 || state.fixedRecovery()!=0)) throw bad();
            if (state.useCount()>0) {
                var kind=HealingKind.find(state.itemId());
                boolean laterRegen=!regen.isEmpty()&&regen.get(i).hpRevision()>state.useRevision()
                        ||honor.enabled()&&honor.lives().get(i).respawnRevision()>state.useRevision();
                if (!laterRegen&&(vital.hp()>state.hpAfter() || vital.hurtRevision()<=state.useRevision() && vital.hp()!=state.hpAfter())
                        || !ModernWorldProtocol.contains(world.manifest(),new ModernWorldMap.Cell(state.x(),state.y()))
                        || kind!=null && state.hpAfter()<vital.maxHp() && (state.restored()<kind.minimum
                            || state.restored()-kind.minimum<state.fixedRecovery())
                        || state.useRevision()==world.revision() && (vital.hp()!=state.hpAfter()
                            || state.x()!=actor.position().x() || state.y()!=actor.position().y())) throw bad();
            }
            if (world.completion()!=null && state.useRevision()>Math.addExact(world.completion().event().engineRevision(),1)) throw bad();
        }
    }
    static void validate(ModernApplicationProtocol.ItemResult message) throws ModernApplicationProtocol.ProtocolException {
        if(message.echo()==null || message.reply()==null)fail();
        var request=message.echo();var reply=message.reply();var frame=reply.frame();
        if(frame==null)return;
        var world=frame.world();
        if(world.matchId()!=request.matchId() || world.generation()!=request.generation() || frame.items().isEmpty())fail();
        var key=ModernParticipantId.human(request.scope().sourceId(),request.scope().objectId());
        int self=-1;
        for(int i=0;i<world.actors().size();i++)if(world.actors().get(i).member().identity().equals(key))self=i;
        if(self<0)fail();
        if(reply.reason()==Reason.CLASS_RESTRICTED) {
            var kind=ModernTimedEffects.Kind.find(request.itemId());
            if(reply.status()!=Status.DENIED || request.action()!=Action.USE || kind==null
                    || frame.buffs().isEmpty() || world.phase()!=ModernWorldProtocol.Phase.ACTIVE
                    || frame.vitals().get(self).hp()==0 || kind.eligible(world.actors().get(self).member().characterClass())
                    || frame.items().get(self).requestSequence()!=request.sequence()
                    || frame.items().get(self).stocks().stream().noneMatch(s->s.itemId()==request.itemId()&&s.remaining()>0))fail();
        }
        if(reply.reason()==Reason.STALE_REGEN && (reply.status()!=Status.DENIED||request.action()!=Action.USE
                ||HealingKind.find(request.itemId())==null||world.phase()!=ModernWorldProtocol.Phase.ACTIVE
                ||frame.regen().isEmpty()||frame.regen().get(self).hpRevision()==0||frame.vitals().get(self).hp()==0
                ||frame.items().get(self).requestSequence()!=request.sequence()
                ||frame.items().get(self).stocks().stream().noneMatch(s->s.itemId()==request.itemId()&&s.remaining()>0)))fail();
        if(request.action()==Action.USE && reply.status()==Status.OK) {
            var state=frame.items().get(self);
            if(world.phase()!=ModernWorldProtocol.Phase.ACTIVE || state.requestSequence()!=request.sequence()
                    || state.useRevision()!=world.revision() || state.itemId()!=request.itemId()
                    || frame.vitals().get(self).hp()!=state.hpAfter())fail();
        }
    }
    static void putRequest(ByteBuffer out,Request request) {
        var s=request.scope();
        out.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch()).put((byte)s.channelId())
                .put((byte)request.action().ordinal()).putShort((short)0).putLong(request.matchId()).putLong(request.generation())
                .putLong(request.sequence()).putLong(request.itemId());
    }
    static Request getRequest(ByteBuffer in) throws ModernApplicationProtocol.ProtocolException {
        var scope=new ModernRooms.Scope(in.getInt(),in.getLong(),in.getLong(),u8(in));
        var action=at(Action.values(),u8(in));zero(in,2);
        return new Request(scope,action,in.getLong(),in.getLong(),in.getLong(),in.getLong());
    }
    static void putResult(ByteBuffer out,ModernApplicationProtocol.ItemResult result,int version) throws ModernApplicationProtocol.ProtocolException {
        if(version<12&&result.reply().reason()==Reason.STALE_REGEN)fail();
        if(version<8 || version<10 && result.reply().reason()==Reason.CLASS_RESTRICTED)fail();validate(result);putRequest(out,result.echo());var reply=result.reply();
        out.put((byte)reply.status().ordinal()).put((byte)reply.reason().ordinal()).put((byte)(reply.frame()==null?0:1)).put((byte)0);
        if(reply.frame()!=null)ModernCombatProtocol.putFrame(out,reply.frame(),version);
    }
    static ModernApplicationProtocol.ItemResult getResult(ByteBuffer in,int requestId,int version) throws ModernApplicationProtocol.ProtocolException {
        if(version<8)fail();var echo=getRequest(in);var status=at(Status.values(),u8(in));var reason=at(Reason.values(),u8(in));
        if(version<10 && reason==Reason.CLASS_RESTRICTED)fail();
        if(version<12&&reason==Reason.STALE_REGEN)fail();
        int present=u8(in);if(present>1)fail();zero(in,1);
        var result=new ModernApplicationProtocol.ItemResult(requestId,echo,new Reply(status,reason,present==0?null:ModernCombatProtocol.getFrame(in,version)));
        validate(result);return result;
    }
    static void putStates(ByteBuffer out,List<State> states) {
        putStates(out,states,8);
    }
    static void putStates(ByteBuffer out,List<State> states,int version) {
        out.putShort((short)states.size()).putShort((short)0);
        for(var s:states) {
            validateVersion(s,version);
            out.putLong(s.entityId()).putLong(s.requestSequence()).putLong(s.useCount()).putLong(s.useRevision())
                    .putLong(s.itemId()).putLong(s.restored()).putLong(s.hpBefore()).putLong(s.hpAfter())
                    .putInt(s.effect()).putInt(s.delayMillis()).putInt(s.x()).putInt(s.y()).putLong(s.fixedRecovery())
                    .putShort((short)s.stocks().size()).putShort((short)0);
            for(var stock:s.stocks())out.putLong(stock.itemId()).putLong(stock.initial()).putLong(stock.remaining());
        }
    }
    static List<State> getStates(ByteBuffer in,int actorCount) throws ModernApplicationProtocol.ProtocolException {
        return getStates(in,actorCount,8);
    }
    static List<State> getStates(ByteBuffer in,int actorCount,int version) throws ModernApplicationProtocol.ProtocolException {
        int count=Short.toUnsignedInt(in.getShort());zero(in,2);
        if(count!=0 && count!=actorCount || count>100 || in.remaining()<count*STATE_PREFIX_BYTES)fail();
        var states=new ArrayList<State>(count);
        for(int i=0;i<count;i++) {
            long entity=in.getLong(),sequence=in.getLong(),used=in.getLong(),revision=in.getLong(),item=in.getLong(),restored=in.getLong(),before=in.getLong(),after=in.getLong();
            int effect=in.getInt(),delay=in.getInt(),x=in.getInt(),y=in.getInt();long fixed=in.getLong();
            int stockCount=Short.toUnsignedInt(in.getShort());zero(in,2);
            if(stockCount>(version>=10?MAX_TIMED_STOCKS:MAX_STOCKS) || in.remaining()<stockCount*STOCK_BYTES)fail();
            var stocks=new ArrayList<Stock>(stockCount);
            for(int j=0;j<stockCount;j++)stocks.add(new Stock(in.getLong(),in.getLong(),in.getLong()));
            var state=new State(entity,sequence,used,revision,item,restored,before,after,effect,delay,x,y,fixed,stocks);
            validateVersion(state,version);states.add(state);
        }
        return List.copyOf(states);
    }
    private static void validateVersion(State state,int version) {
        if(version<10 && (state.stocks().size()>MAX_STOCKS || state.useCount()>0 && HealingKind.find(state.itemId())==null
                || state.stocks().stream().anyMatch(s->HealingKind.find(s.itemId())==null)))throw bad();
    }
    private static int u8(ByteBuffer in){return Byte.toUnsignedInt(in.get());}
    private static void zero(ByteBuffer in,int count) throws ModernApplicationProtocol.ProtocolException {for(int i=0;i<count;i++)if(in.get()!=0)fail();}
    private static <T>T at(T[] values,int index) throws ModernApplicationProtocol.ProtocolException {if(index>=values.length)fail();return values[index];}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid match item wire value");}
    private static void fail() throws ModernApplicationProtocol.ProtocolException {throw new ModernApplicationProtocol.ProtocolException();}
}
