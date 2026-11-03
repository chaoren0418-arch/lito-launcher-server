package local.lito381.server;

import static local.lito381.server.ModernRooms.*;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/** Waiting-room capability only: no native packet, countdown or world-ready assertion. */
final class ModernRoomProtocol {
    static final int MAX_FRAME_BYTES=128*1024;
    static final int COMMON_BYTES=40;
    record Echo(Scope scope,Action action,long roomId,long expectedRevision,int value) {
        Echo {
            Objects.requireNonNull(scope);Objects.requireNonNull(action);
            if (action==Action.READ || action==Action.CREATE) {
                if(roomId!=0 || expectedRevision!=0)throw bad();
            } else if(roomId<=0 || expectedRevision<1)throw bad();
            if(action==Action.TEAM || action==Action.READY) {
                if(value<0 || value>1)throw bad();
            } else if(value!=0)throw bad();
        }
        static Echo of(Command command) { return new Echo(command.scope(),command.action(),command.roomId(),command.expectedRevision(),command.value()); }
    }
    private ModernRoomProtocol() {}
    static void putRequest(ByteBuffer out,Command command) throws ModernApplicationProtocol.ProtocolException {
        putEcho(out,Echo.of(command));
        if(command.action()==Action.CREATE || command.action()==Action.CONFIGURE) {
            Draft draft=command.draft();
            out.putShort((short)draft.capacity()).put((byte)draft.mode()).put((byte)draft.attributes()).putInt(draft.mapId());
            text(out,draft.name(),64,false);text(out,draft.password(),64,true);
        } else if(command.action()==Action.JOIN)text(out,command.password(),64,true);
    }
    static Command getRequest(ByteBuffer in) throws ModernApplicationProtocol.ProtocolException {
        Echo echo=getEcho(in);Draft draft=null;String password="";
        if(echo.action()==Action.CREATE || echo.action()==Action.CONFIGURE) {
            int capacity=u16(in),mode=u8(in),attributes=u8(in),map=in.getInt();
            draft=new Draft(text(in,64,false),text(in,64,true),capacity,mode,map,attributes);
        } else if(echo.action()==Action.JOIN)password=text(in,64,true);
        return new Command(echo.scope(),echo.action(),echo.roomId(),echo.expectedRevision(),echo.value(),draft,password);
    }
    private static void putEcho(ByteBuffer out,Echo echo) {
        Scope scope=echo.scope();
        out.putInt(scope.sourceId()).putLong(scope.objectId()).putLong(scope.epoch()).put((byte)scope.channelId())
                .put((byte)echo.action().ordinal()).put((byte)echo.value()).put((byte)0).putLong(echo.roomId()).putLong(echo.expectedRevision());
    }
    private static Echo getEcho(ByteBuffer in) throws ModernApplicationProtocol.ProtocolException {
        int source=in.getInt();long object=in.getLong(),epoch=in.getLong();int channel=u8(in);
        Action action=at(Action.values(),u8(in));int value=u8(in);reserved(in,1);
        return new Echo(new Scope(source,object,epoch,channel),action,in.getLong(),in.getLong(),value);
    }
    static void putResult(ByteBuffer out,ModernApplicationProtocol.RoomResult message) throws ModernApplicationProtocol.ProtocolException {
        putResult(out,message,1);
    }
    static void putResult(ByteBuffer out,ModernApplicationProtocol.RoomResult message,int version) throws ModernApplicationProtocol.ProtocolException {
        putEcho(out,message.echo());var result=message.result();var view=result.view();
        out.put((byte)result.status().ordinal()).put((byte)result.reason().ordinal()).putShort((short)0).putLong(view.listRevision())
                .putShort((short)view.rooms().size()).putShort((short)view.options().size()).put((byte)(view.joined()==null?0:1)).put((byte)0).putShort((short)0);
        for(Option option:view.options()) {
            out.put((byte)option.mode()).put((byte)(option.teamBased()?1:0)).put((byte)(option.worldAvailable()?1:0)).put((byte)0)
                    .putInt(option.mapId()).putShort((short)option.minimumPlayers());
            if(version>=19)out.putShort((short)option.maximumPlayers());
            out.putInt(option.timeLimitSeconds());
            text(out,option.modeName(),64,false);
        }
        for(Summary room:view.rooms())putSummary(out,room);
        if(view.joined()!=null) putJoined(out,view.joined(),version);
    }
    static void putJoined(ByteBuffer out,RoomSnapshot room) throws ModernApplicationProtocol.ProtocolException {
        putJoined(out,room,1);
    }
    static void putJoined(ByteBuffer out,RoomSnapshot room,int version) throws ModernApplicationProtocol.ProtocolException {
            putSummary(out,room.summary());putKey(out,room.owner());
            out.putShort((short)room.members().size()).putShort((short)0);
            for(Member member:room.members()) {
                putIdentity(out,member.identity(),version);out.putLong(member.level()).putShort((short)member.characterClass())
                        .put((byte)member.sex()).put((byte)member.team()).put((byte)(member.ready()?1:0)).put((byte)0).putShort((short)0);
                text(out,member.name(),256,false);text(out,member.sourceName(),256,false);
            }
    }
    static ModernApplicationProtocol.RoomResult getResult(ByteBuffer in,int requestId) throws ModernApplicationProtocol.ProtocolException {
        return getResult(in,requestId,1);
    }
    static ModernApplicationProtocol.RoomResult getResult(ByteBuffer in,int requestId,int version) throws ModernApplicationProtocol.ProtocolException {
        Echo echo=getEcho(in);Status status=at(Status.values(),u8(in));Reason reason=at(Reason.values(),u8(in));reserved(in,2);
        long listRevision=in.getLong();int rooms=count(in,100),options=count(in,40);if(options!=40)fail();
        boolean present=bool(in);reserved(in,3);
        var optionRows=new ArrayList<Option>(options);
        for(int i=0;i<options;i++) {
            int mode=u8(in);boolean team=bool(in),world=bool(in);reserved(in,1);
            int map=in.getInt(),minimum=u16(in),maximum=version>=19?u16(in):100,seconds=in.getInt();
            optionRows.add(new Option(mode,map,minimum,seconds,team,text(in,64,false),world,maximum));
        }
        var summaries=new ArrayList<Summary>(rooms);for(int i=0;i<rooms;i++)summaries.add(getSummary(in));
        RoomSnapshot joined=null;
        if(present) joined=getJoined(in,version);
        return new ModernApplicationProtocol.RoomResult(requestId,echo,new Result(status,reason,new View(listRevision,summaries,joined,optionRows)));
    }
    static RoomSnapshot getJoined(ByteBuffer in) throws ModernApplicationProtocol.ProtocolException {
        return getJoined(in,1);
    }
    static RoomSnapshot getJoined(ByteBuffer in,int version) throws ModernApplicationProtocol.ProtocolException {
            var summary=getSummary(in);var owner=getKey(in);int members=count(in,100);reserved(in,2);
            var rows=new ArrayList<Member>(members);
            for(int i=0;i<members;i++) {
                var key=getIdentity(in,version);long level=in.getLong();int characterClass=u16(in),sex=u8(in),team=u8(in);boolean ready=bool(in);reserved(in,3);
                rows.add(new Member(key,text(in,256,false),text(in,256,false),level,characterClass,sex,team,ready));
            }
            return new RoomSnapshot(summary,owner,rows);
    }
    static void validate(ModernApplicationProtocol.RoomResult message) throws ModernApplicationProtocol.ProtocolException {
        validate(message,false);
    }
    private static void validate(ModernApplicationProtocol.RoomResult message,boolean botResult) throws ModernApplicationProtocol.ProtocolException {
        if(message.echo()==null || message.result()==null)fail();
        var echo=message.echo();var result=message.result();var view=result.view();
        boolean legalReason=switch(result.status()) {
            case OK -> result.reason()==Reason.NONE;
            case CONFLICT -> result.reason()==Reason.REVISION_MISMATCH;
            case FULL -> result.reason()==Reason.CAPACITY;
            case UNAVAILABLE -> result.reason()==Reason.REVISION_EXHAUSTED;
            case DENIED -> result.reason().ordinal()>=1 && result.reason().ordinal()<=6 || result.reason()==Reason.INVALID_CONFIGURATION
                    || botResult && result.reason()==Reason.GM_NOT_AUTHORIZED;
        };
        if(!legalReason || view.listRevision()<1 || view.rooms().size()>100 || view.options().size()!=40)fail();
        int previousMode=-1,previousMap=0;
        for(Option option:view.options()) {
            if(option.mode()<0 || option.mode()>5 || option.mapId()<1 || option.minimumPlayers()<1 || option.minimumPlayers()>100
                    || option.maximumPlayers()<Math.max(2,option.minimumPlayers())||option.maximumPlayers()>100
                    || option.timeLimitSeconds()<1 || option.timeLimitSeconds()>86400
                    || option.mode()<previousMode || option.mode()==previousMode && option.mapId()<=previousMap)fail();
            var mode=BattleRules.Mode.fromWire(option.mode());
            if(!mode.maps().contains(option.mapId()) || option.teamBased()!=mode.teamBased())fail();
            validText(option.modeName(),64,false);previousMode=option.mode();previousMap=option.mapId();
        }
        long previousId=0;
        for(Summary room:view.rooms()) {
            validateSummary(room);if(room.id()<=previousId)fail();previousId=room.id();
            var option=view.options().stream().filter(row->row.mode()==room.config().mode()&&row.mapId()==room.config().mapId()).findFirst();
            if(option.isEmpty()||room.config().capacity()>option.get().maximumPlayers())fail();
        }
        var joined=view.joined();
        if(joined!=null) {
            validateSummary(joined.summary());
            if(!view.rooms().contains(joined.summary()) || joined.members().size()!=joined.summary().users())fail();
            var self=ModernParticipantId.human(echo.scope().sourceId(),echo.scope().objectId());
            boolean foundSelf=false,foundOwner=false;int ready=0;ModernParticipantId previous=null;
            for(Member member:joined.members()) {
                if(previous!=null && member.identity().compareTo(previous)<=0 || member.level()<1
                        || member.characterClass()<0 || member.characterClass()>6 || member.sex()<0 || member.sex()>1 || member.team()<0 || member.team()>1)fail();
                previous=member.identity();
                validText(member.name(),256,false);validText(member.sourceName(),256,false);
                foundSelf|=member.identity().equals(self);foundOwner|=member.identity().equals(ModernParticipantId.human(joined.owner()));if(member.ready())ready++;
            }
            if(!foundSelf || !foundOwner || ready!=joined.summary().ready())fail();
        }
        if(result.status()==Status.OK) {
            switch(echo.action()) {
                case CREATE -> { if(joined==null || joined.summary().revision()!=1
                        || !joined.owner().equals(new TournamentPresence.CharacterKey(echo.scope().sourceId(),echo.scope().objectId())))fail(); }
                case JOIN -> { if(joined==null || joined.summary().id()!=echo.roomId()
                        || joined.summary().revision()!=Math.addExact(echo.expectedRevision(),1))fail(); }
                case TEAM,READY,CONFIGURE -> { if(joined==null || joined.summary().id()!=echo.roomId())fail(); }
                case LEAVE -> { if(joined!=null)fail(); }
                case READ -> {}
            }
            if(echo.action()==Action.TEAM || echo.action()==Action.READY || echo.action()==Action.CONFIGURE) {
                long actual=joined.summary().revision();
                if(actual!=echo.expectedRevision() && actual!=Math.addExact(echo.expectedRevision(),1))fail();
                if(echo.action()!=Action.CONFIGURE) {
                    Member self=joined.members().stream().filter(row->row.identity().equals(
                            ModernParticipantId.human(echo.scope().sourceId(),echo.scope().objectId()))).findFirst().orElseThrow();
                    if(echo.action()==Action.TEAM?self.team()!=echo.value():self.ready()!=(echo.value()==1))fail();
                }
            }
        }
    }
    static void putBotResult(ByteBuffer out,Scope scope,Result result,int version) throws ModernApplicationProtocol.ProtocolException {
        if(version<7||version>ModernSessionProtocol.MAX_VERSION)fail();
        var nested=new ModernApplicationProtocol.RoomResult(1,new Echo(scope,Action.READ,0,0,0),result);
        validate(nested,true);putResult(out,nested,version);
    }
    static Result getBotResult(ByteBuffer in,Scope scope,int version) throws ModernApplicationProtocol.ProtocolException {
        if(version<7||version>ModernSessionProtocol.MAX_VERSION)fail();
        var nested=getResult(in,1,version);
        if(!nested.echo().equals(new Echo(scope,Action.READ,0,0,0)))fail();
        validate(nested,true);return nested.result();
    }
    static void putIdentity(ByteBuffer out,ModernParticipantId identity,int version) throws ModernApplicationProtocol.ProtocolException {
        if(version>=7)out.put((byte)identity.kind().ordinal()).put((byte)0).putShort((short)0);
        else if(!identity.isHuman())fail();
        out.putInt(identity.sourceId()).putLong(identity.id());
    }
    static ModernParticipantId getIdentity(ByteBuffer in,int version) throws ModernApplicationProtocol.ProtocolException {
        var kind=ModernParticipantId.Kind.HUMAN;
        if(version>=7){kind=at(ModernParticipantId.Kind.values(),u8(in));reserved(in,3);}
        return new ModernParticipantId(kind,in.getInt(),in.getLong());
    }
    private static void validateSummary(Summary room) throws ModernApplicationProtocol.ProtocolException {
        Config c=room.config();
        if(room.id()<1 || room.revision()<1 || c.capacity()<2 || c.capacity()>100 || c.mode()<0 || c.mode()>5
                || (c.attributes() & ~52)!=0 || !BattleRules.Mode.fromWire(c.mode()).maps().contains(c.mapId())
                || room.users()<1 || room.users()>c.capacity() || room.ready()<0 || room.ready()>room.users()
                || room.readySatisfied() && room.ready()!=room.users())fail();
        validText(c.name(),64,false);
    }
    private static void putSummary(ByteBuffer out,Summary room) throws ModernApplicationProtocol.ProtocolException {
        Config c=room.config();out.putLong(room.id()).putLong(room.revision()).putShort((short)c.capacity())
                .put((byte)c.mode()).put((byte)c.attributes()).putInt(c.mapId());text(out,c.name(),64,false);
        out.putShort((short)room.users()).putShort((short)room.ready()).put((byte)(room.readySatisfied()?1:0)).put((byte)0).putShort((short)0);
    }
    private static Summary getSummary(ByteBuffer in) throws ModernApplicationProtocol.ProtocolException {
        long id=in.getLong(),revision=in.getLong();int capacity=u16(in),mode=u8(in),attributes=u8(in),map=in.getInt();String name=text(in,64,false);
        int users=u16(in),ready=u16(in);boolean satisfied=bool(in);reserved(in,3);
        return new Summary(id,revision,new Config(name,capacity,mode,map,attributes),users,ready,satisfied);
    }
    private static void putKey(ByteBuffer out,TournamentPresence.CharacterKey key) { out.putInt(key.sourceId()).putLong(key.objectId()); }
    private static TournamentPresence.CharacterKey getKey(ByteBuffer in) { return new TournamentPresence.CharacterKey(in.getInt(),in.getLong()); }
    private static void validText(String value,int maximum,boolean password) throws ModernApplicationProtocol.ProtocolException {
        if(value==null || value.chars().anyMatch(Character::isISOControl) || !StandardCharsets.UTF_8.newEncoder().canEncode(value)
                || value.getBytes(StandardCharsets.UTF_8).length>maximum
                || !password && value.codePoints().noneMatch(p->!Character.isWhitespace(p)&&!Character.isSpaceChar(p)))fail();
    }
    private static void text(ByteBuffer out,String value,int maximum,boolean password) throws ModernApplicationProtocol.ProtocolException {
        validText(value,maximum,password);byte[] bytes=value.getBytes(StandardCharsets.UTF_8);
        try { out.putShort((short)bytes.length).put(bytes); }finally{Arrays.fill(bytes,(byte)0);}
    }
    private static String text(ByteBuffer in,int maximum,boolean password) throws ModernApplicationProtocol.ProtocolException {
        int length=u16(in);if(length>maximum || length>in.remaining())fail();byte[] bytes=new byte[length];in.get(bytes);
        try {
            String value=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            validText(value,maximum,password);return value;
        }catch(java.nio.charset.CharacterCodingException failure){throw new ModernApplicationProtocol.ProtocolException();}
        finally{Arrays.fill(bytes,(byte)0);}
    }
    private static int u8(ByteBuffer in){return Byte.toUnsignedInt(in.get());}
    private static int u16(ByteBuffer in){return Short.toUnsignedInt(in.getShort());}
    private static int count(ByteBuffer in,int maximum) throws ModernApplicationProtocol.ProtocolException {int value=u16(in);if(value>maximum)fail();return value;}
    private static boolean bool(ByteBuffer in) throws ModernApplicationProtocol.ProtocolException {int value=u8(in);if(value>1)fail();return value!=0;}
    private static void reserved(ByteBuffer in,int count) throws ModernApplicationProtocol.ProtocolException {for(int i=0;i<count;i++)if(in.get()!=0)fail();}
    private static <T> T at(T[] values,int index) throws ModernApplicationProtocol.ProtocolException {if(index>=values.length)fail();return values[index];}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid room echo");}
    private static void fail() throws ModernApplicationProtocol.ProtocolException {throw new ModernApplicationProtocol.ProtocolException();}
}
