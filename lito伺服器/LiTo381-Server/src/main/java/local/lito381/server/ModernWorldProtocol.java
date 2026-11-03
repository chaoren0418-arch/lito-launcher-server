package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** World-entry/movement wire values only. A decoded request cannot mint an owner or preparation. */
final class ModernWorldProtocol {
    static final int REQUEST_KIND=20, RESULT_KIND=21, REQUEST_BYTES=104, MAX_FRAME_BYTES=128*1024;
    static final String ZERO_DIGEST="0".repeat(64);
    enum Action { READ, PREPARE, LOADED, MOVE, LEAVE }
    enum Status { OK, DENIED, BUSY, UNAVAILABLE }
    enum Reason { NONE, INVALID_STATE, NOT_OWNER, ROOM_REVISION, NOT_READY, NO_BINDING, CAPACITY,
        PREPARATION_FAILED, NOT_ACTIVE, STALE_POSITION, TOO_FAST, BLOCKED, OCCUPIED }
    enum Phase { PREPARING, LOADING, COUNTDOWN, ACTIVE, ABORTED, FINISHED }
    enum SettlementState { PENDING, SAVED, FAILED }
    record Completion(ModernMatchSettlement.Event event, SettlementState state) {
        Completion { Objects.requireNonNull(event);Objects.requireNonNull(state); }
    }
    enum AbortReason { NONE, DISCONNECTED, LOAD_TIMEOUT, ACTIVE_TIMEOUT, SERVER_ABORT, CLOCK_REGRESSED,
        PREPARATION_CANCELLED, PREPARATION_FAILED }
    record Request(ModernRooms.Scope scope,Action action,int heading,long roomId,long expectedRoomRevision,
                   long matchId,long generation,long sequence,int fromX,int fromY,String manifestDigest) {
        Request {
            Objects.requireNonNull(scope);Objects.requireNonNull(action);
            if(!hash(manifestDigest) || heading<0 || heading>7)throw bad();
            boolean zeroMove=heading==0 && sequence==0 && fromX==0 && fromY==0;
            boolean zeroRoom=roomId==0 && expectedRoomRevision==0;
            boolean zeroHash=ZERO_DIGEST.equals(manifestDigest), hasMatch=matchId>0 && generation>0;
            boolean valid=switch(action) {
                case READ -> zeroRoom && zeroMove && zeroHash && (hasMatch || matchId==0 && generation==0);
                case PREPARE -> roomId>0 && expectedRoomRevision>0 && matchId==0 && generation==0 && zeroMove && zeroHash;
                case LOADED -> zeroRoom && hasMatch && zeroMove && !zeroHash;
                case MOVE -> zeroRoom && hasMatch && sequence>0 && zeroHash;
                case LEAVE -> zeroRoom && hasMatch && zeroMove && zeroHash;
            };
            if(!valid)throw bad();
        }
    }
    record Frame(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,
                 ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,List<ModernBattleEngine.Actor> actors,
                 Completion completion,List<ModernSpellProtocol.CastState> casts,ModernMatchDescriptor descriptor,ModernPetRaceProtocol.Snapshot race,int audienceLane,ModernQuestProtocol.Data quest,ModernTimeAttackProtocol.Data timeAttack,ModernSiegeProtocol.Data siege) {
        Frame(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,
              ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,List<ModernBattleEngine.Actor> actors,
              Completion completion,List<ModernSpellProtocol.CastState> casts,ModernMatchDescriptor descriptor,ModernPetRaceProtocol.Snapshot race,int audienceLane,ModernQuestProtocol.Data quest,ModernTimeAttackProtocol.Data timeAttack) {
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,completion,casts,descriptor,race,audienceLane,quest,timeAttack,null);
        }
        Frame(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,
              ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,List<ModernBattleEngine.Actor> actors,
              Completion completion,List<ModernSpellProtocol.CastState> casts,ModernMatchDescriptor descriptor,ModernPetRaceProtocol.Snapshot race) {
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,completion,casts,descriptor,race,-1);
        }
        Frame(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,
              ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,List<ModernBattleEngine.Actor> actors,
              Completion completion,List<ModernSpellProtocol.CastState> casts,ModernMatchDescriptor descriptor) {
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,completion,casts,descriptor,ModernPetRaceProtocol.Snapshot.disabled());
        }
        Frame(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,
              ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,List<ModernBattleEngine.Actor> actors,
              Completion completion,List<ModernSpellProtocol.CastState> casts) {
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,completion,casts,null);
        }
        Frame withDescriptor(ModernMatchDescriptor value){return new Frame(matchId,generation,revision,phase,abortReason,room,manifest,actors,completion,casts,value,race,audienceLane,quest,timeAttack,siege);}
        Frame(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,
              ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,List<ModernBattleEngine.Actor> actors,
              Completion completion) {
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,completion,List.of());
        }
        Frame(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,
              ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,List<ModernBattleEngine.Actor> actors) {
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,null);
        }
        Frame(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,
              ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,List<ModernBattleEngine.Actor> actors,
              Completion completion,List<ModernSpellProtocol.CastState> casts,ModernMatchDescriptor descriptor,ModernPetRaceProtocol.Snapshot race,int audienceLane) {
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,completion,casts,descriptor,race,audienceLane,ModernQuestProtocol.Data.disabled());
        }
        Frame(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,
              ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,List<ModernBattleEngine.Actor> actors,
              Completion completion,List<ModernSpellProtocol.CastState> casts,ModernMatchDescriptor descriptor,ModernPetRaceProtocol.Snapshot race,int audienceLane,ModernQuestProtocol.Data quest) {
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,completion,casts,descriptor,race,audienceLane,quest,null);
        }
        Frame(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,
              ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,List<ModernBattleEngine.Actor> actors,
              Completion completion,List<ModernSpellProtocol.CastState> casts,ModernMatchDescriptor descriptor,ModernPetRaceProtocol.Snapshot race,int audienceLane,ModernQuestProtocol.Data quest,ModernTimeAttackProtocol.Data timeAttack,ModernSiegeProtocol.Data siege) {
            Objects.requireNonNull(phase);Objects.requireNonNull(abortReason);Objects.requireNonNull(room);
            Objects.requireNonNull(manifest);actors=List.copyOf(actors);casts=List.copyOf(casts);
            Objects.requireNonNull(race);this.matchId=matchId;this.generation=generation;this.revision=revision;this.phase=phase;this.abortReason=abortReason;
            this.room=room;this.manifest=manifest;this.actors=actors;this.completion=completion;this.casts=casts;this.descriptor=descriptor;this.race=race;this.audienceLane=audienceLane;this.quest=Objects.requireNonNull(quest);this.timeAttack=timeAttack;this.siege=siege;
            boolean isolated=descriptor!=null&&descriptor.has(ModernMatchDescriptor.ISOLATED_FORCES);
            if(audienceLane< -1||audienceLane>1||audienceLane>=0&&!isolated)throw bad();
            int expected=audienceLane<0?room.members().size():(int)room.members().stream().filter(m->m.team()==audienceLane).count();
            if(race.enabled()&&race.runners().size()!=actors.size())throw bad();
            if(!casts.isEmpty() && casts.size()!=actors.size())throw bad();
            if(matchId<1 || generation<1 || revision<1 || generation!=room.summary().revision()
                    || (phase==Phase.ABORTED)!=(abortReason!=AbortReason.NONE)
                    || (phase==Phase.FINISHED)!=(completion!=null||race.complete()||timeAttack!=null&&timeAttack.finished()||siege!=null&&siege.finished())||completion!=null&&race.enabled()
                    || !room.summary().readySatisfied() || room.summary().ready()!=room.summary().users()
                    || actors.isEmpty() || actors.size()>100 || actors.size()!=expected)throw bad();
            validateManifest(manifest);
            var cells=new HashSet<ModernQuestSpace.Cell>();long previous=0;
            for(int i=0;i<actors.size();i++) {
                var actor=actors.get(i);
                if(actor.entityId()<=previous||actor.entityId()>room.members().size()
                        ||!actor.member().equals(room.members().get((int)actor.entityId()-1))
                        ||audienceLane<0&&actor.entityId()!=i+1||audienceLane>=0&&actor.member().team()!=audienceLane
                        || (timeAttack==null||!timeAttack.departed(actor.entityId()))&&(!race.enabled()||race.runners().get(i).competing())&&!cells.add(ModernQuestSpace.cell(isolated,actor.member().team(),actor.position())) || !contains(manifest,actor.position())
                        || phase==Phase.PREPARING && (actor.loaded() || actor.lastMoveSequence()!=0)
                        || (phase==Phase.COUNTDOWN || phase==Phase.ACTIVE || phase==Phase.FINISHED) && !actor.loaded())throw bad();
                previous=actor.entityId();
            }
            if(completion!=null)validateCompletion(completion,matchId,generation,revision,room,actors);
            for(int i=0;i<casts.size();i++) {
                var c=casts.get(i);
                if(c.entityId()!=actors.get(i).entityId() || c.castRevision()>revision
                        || phase==Phase.PREPARING && !c.equals(ModernSpellProtocol.CastState.initial(actors.get(i).entityId(),c.castIntervalMillis()))
                        || c.castCount()>0 && (!contains(manifest,new ModernWorldMap.Cell(c.fromX(),c.fromY()))
                            || !contains(manifest,new ModernWorldMap.Cell(c.toX(),c.toY()))))throw bad();
                if(completion!=null && c.castRevision()>Math.addExact(completion.event().engineRevision(),1))throw bad();
            }
            if(descriptor!=null){descriptor.validateMode(room.summary().config().mode());
                if(descriptor.has(ModernMatchDescriptor.SPELLS)!=!casts.isEmpty() || completion!=null&&!descriptor.settlementEnabled()
                        ||descriptor.has(ModernMatchDescriptor.PET_RACE)!=race.enabled())throw bad();}
            ModernPetRaceProtocol.validate(this,race);
            ModernQuestProtocol.validate(this,quest);
            ModernTimeAttackProtocol.validate(this);
            ModernSiegeProtocol.validate(this);
        }
        boolean isolatedForces(){return descriptor!=null&&descriptor.has(ModernMatchDescriptor.ISOLATED_FORCES);}
    }
    record Reply(Status status,Reason reason,Frame frame) {
        Reply {Objects.requireNonNull(status);Objects.requireNonNull(reason);if((status==Status.OK)!=(reason==Reason.NONE))throw bad();}
    }
    private ModernWorldProtocol() {}
    static void validate(ModernApplicationProtocol.WorldResult message) throws ModernApplicationProtocol.ProtocolException {
        if(message.echo()==null || message.reply()==null)fail();
        Request echo=message.echo();Reply reply=message.reply();Frame frame=reply.frame();
        if(frame!=null) {
            var self=ModernParticipantId.human(echo.scope().sourceId(),echo.scope().objectId());
            if(frame.actors().stream().noneMatch(actor->actor.member().identity().equals(self)))fail();
            if(echo.matchId()!=0 && (echo.matchId()!=frame.matchId() || echo.generation()!=frame.generation()))fail();
        }
        if(reply.status()==Status.OK) {
            switch(echo.action()) {
                case READ -> {}
                case LEAVE -> {if(frame!=null)fail();}
                case PREPARE -> {
                    if(frame==null || frame.room().summary().id()!=echo.roomId()
                            || echo.expectedRoomRevision()==Long.MAX_VALUE
                            || frame.generation()!=echo.expectedRoomRevision()+1
                            || !frame.room().owner().equals(new TournamentPresence.CharacterKey(echo.scope().sourceId(),echo.scope().objectId())))fail();
                }
                case LOADED -> {
                    if(frame==null || frame.phase()==Phase.PREPARING || !echo.manifestDigest().equals(frame.manifest().digest()))fail();
                    var self=frame.actors().stream().filter(a->a.member().identity().equals(
                            ModernParticipantId.human(echo.scope().sourceId(),echo.scope().objectId()))).findFirst().orElseThrow();
                    if(!self.loaded())fail();
                }
                case MOVE -> {
                    if(frame==null || frame.phase()!=Phase.ACTIVE)fail();
                    var self=frame.actors().stream().filter(a->a.member().identity().equals(
                            ModernParticipantId.human(echo.scope().sourceId(),echo.scope().objectId()))).findFirst().orElseThrow();
                    int[] dx={0,1,1,1,0,-1,-1,-1},dy={-1,-1,0,1,1,1,0,-1};
                    if(self.lastMoveSequence()!=echo.sequence() || self.heading()!=echo.heading()
                            || (long)self.position().x()!=(long)echo.fromX()+dx[echo.heading()]
                            || (long)self.position().y()!=(long)echo.fromY()+dy[echo.heading()])fail();
                }
            }
        }
    }
    static void putRequest(ByteBuffer out,Request r) {
        var s=r.scope();out.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch()).put((byte)s.channelId())
                .put((byte)r.action().ordinal()).put((byte)r.heading()).put((byte)0).putLong(r.roomId()).putLong(r.expectedRoomRevision())
                .putLong(r.matchId()).putLong(r.generation()).putLong(r.sequence()).putInt(r.fromX()).putInt(r.fromY());
        out.put(HexFormat.of().parseHex(r.manifestDigest()));
    }
    static Request getRequest(ByteBuffer in) throws ModernApplicationProtocol.ProtocolException {
        var scope=new ModernRooms.Scope(in.getInt(),in.getLong(),in.getLong(),u8(in));
        var action=at(Action.values(),u8(in));int heading=u8(in);zero(in,1);
        return new Request(scope,action,heading,in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getInt(),in.getInt(),digest(in));
    }
    static void putResult(ByteBuffer out,ModernApplicationProtocol.WorldResult result) throws ModernApplicationProtocol.ProtocolException {
        putResult(out,result,1);
    }
    static void putResult(ByteBuffer out,ModernApplicationProtocol.WorldResult result,int version) throws ModernApplicationProtocol.ProtocolException {
        putRequest(out,result.echo());var reply=result.reply();var frame=reply.frame();
        out.put((byte)reply.status().ordinal()).put((byte)reply.reason().ordinal()).put((byte)(frame==null?0:1)).put((byte)0);
        if(frame==null)return;
        putFrame(out,frame,version);
    }
    static void putFrame(ByteBuffer out,Frame frame) throws ModernApplicationProtocol.ProtocolException {
        putFrame(out,frame,1);
    }
    /** v32/v33 chat adds no bytes to the existing atomic world payload. */
    // v35 adds a separate canonical read kind; existing WORLD bytes stay v34-compatible.
    static final int CODEC_MAX_VERSION=ModernSessionProtocol.MAX_VERSION;
    static void putFrame(ByteBuffer out,Frame frame,int version) throws ModernApplicationProtocol.ProtocolException {
        if(version<31&&frame.descriptor()!=null&&frame.descriptor().has(ModernMatchDescriptor.NPC_FIRE_STORM))fail();
        if(version<26&&frame.siege()!=null)fail();
        if(version<27&&frame.descriptor()!=null&&frame.descriptor().has(ModernMatchDescriptor.DISINTEGRATE))fail();
        if(version<29&&frame.descriptor()!=null&&frame.descriptor().has(ModernMatchDescriptor.NPC_DISINTEGRATE))fail();
        if(version<30&&frame.descriptor()!=null&&frame.descriptor().has(ModernMatchDescriptor.TIME_ATTACK|ModernMatchDescriptor.NPC_LIGHT_ARROW))fail();
        if(version<25&&frame.timeAttack()!=null)fail();
        if(version<21&&frame.quest().enabled())fail();
        if(frame.isolatedForces()&&(version<20||frame.audienceLane()<0))fail();
        if(version<17&&frame.descriptor()!=null&&frame.descriptor().has(ModernMatchDescriptor.NPC))fail();
        if(version<23&&frame.descriptor()!=null&&frame.descriptor().has(ModernMatchDescriptor.NPC|ModernMatchDescriptor.SPELLS))fail();
        if(version<24&&frame.descriptor()!=null&&frame.descriptor().has(ModernMatchDescriptor.NPC_LIGHT_ARROW))fail();
        if(version<1 || version>CODEC_MAX_VERSION || version<3 && frame.completion()!=null
                || version<4 && !frame.casts().isEmpty()||version<16&&frame.race().enabled())fail();
        out.putLong(frame.matchId()).putLong(frame.generation()).putLong(frame.revision()).put((byte)frame.phase().ordinal())
                .put((byte)frame.abortReason().ordinal()).putShort((short)0);
        ModernRoomProtocol.putJoined(out,frame.room(),version);var manifest=frame.manifest();
        if(version<ModernLargeMaps.VERSION&&manifest.sectors().size()>ModernLargeMaps.LEGACY_SECTORS)fail();
        out.putShort((short)1).putShort((short)manifest.sectors().size()).putInt(manifest.mapId()).put(HexFormat.of().parseHex(manifest.digest()));
        for(var sector:manifest.sectors())out.putShort((short)sector.key().x()).putShort((short)sector.key().y())
                .putInt(sector.byteLength()).put(HexFormat.of().parseHex(sector.rawSha256()));
        out.putShort((short)frame.actors().size()).putShort((short)0);
        for(var actor:frame.actors())out.putLong(actor.entityId()).putInt(actor.position().x()).putInt(actor.position().y())
                .put((byte)actor.heading()).put((byte)(actor.loaded()?1:0)).putShort((short)0)
                .putLong(actor.lastMoveSequence()).putLong(actor.stepIntervalNanos());
        if(version>=3) {
            out.put((byte)(frame.completion()==null?0:1)).put((byte)0).putShort((short)0);
            if(frame.completion()!=null)putCompletion(out,frame.completion(),version);
        }
        if(version>=4)ModernSpellProtocol.putStates(out,frame.casts());
        if(version>=15)ModernMatchDescriptor.put(out,frame.descriptor());
        if(version>=16)ModernPetRaceProtocol.put(out,frame.race());
        if(version>=20)out.put((byte)(frame.audienceLane()+1)).put((byte)0).putShort((short)0);
        if(version>=21)ModernQuestProtocol.put(out,frame.quest(),version);
        if(version>=25)ModernTimeAttackProtocol.put(out,frame.timeAttack(),frame.descriptor()!=null&&frame.descriptor().has(ModernMatchDescriptor.NPC_FIRE_STORM));
        if(version>=26)ModernSiegeProtocol.put(out,frame.siege());
    }
    static ModernApplicationProtocol.WorldResult getResult(ByteBuffer in,int requestId) throws ModernApplicationProtocol.ProtocolException {
        return getResult(in,requestId,1);
    }
    static ModernApplicationProtocol.WorldResult getResult(ByteBuffer in,int requestId,int version) throws ModernApplicationProtocol.ProtocolException {
        var echo=getRequest(in);var status=at(Status.values(),u8(in));var reason=at(Reason.values(),u8(in));
        boolean present=bool(in);zero(in,1);
        Frame frame=present?getFrame(in,version):null;
        return new ModernApplicationProtocol.WorldResult(requestId,echo,new Reply(status,reason,frame));
    }
    static Frame getFrame(ByteBuffer in) throws ModernApplicationProtocol.ProtocolException {
        return getFrame(in,1);
    }
    static Frame getFrame(ByteBuffer in,int protocolVersion) throws ModernApplicationProtocol.ProtocolException {
            if(protocolVersion<1 || protocolVersion>CODEC_MAX_VERSION)fail();
            long match=in.getLong(),generation=in.getLong(),revision=in.getLong();
            var phase=at(Phase.values(),u8(in));var abort=at(AbortReason.values(),u8(in));zero(in,2);
            if(protocolVersion<3 && phase==Phase.FINISHED)fail();
            var room=ModernRoomProtocol.getJoined(in,protocolVersion);int version=u16(in),count=u16(in),map=in.getInt();String hash=digest(in);
            if(version!=1 || count<1 || count>ModernWorldMap.MAX_SECTORS
                    ||protocolVersion<ModernLargeMaps.VERSION&&count>ModernLargeMaps.LEGACY_SECTORS)fail();
            var sectors=new ArrayList<ModernWorldMap.SectorManifest>(count);
            for(int i=0;i<count;i++)sectors.add(new ModernWorldMap.SectorManifest(new ModernWorldMap.SectorKey(u16(in),u16(in)),in.getInt(),digest(in)));
            var manifest=new ModernWorldMap.Manifest(map,sectors,hash);int actorsCount=u16(in);zero(in,2);
            if(actorsCount<1 || actorsCount>100 || protocolVersion<20&&actorsCount!=room.members().size())fail();
            var actors=new ArrayList<ModernBattleEngine.Actor>(actorsCount);
            for(int i=0;i<actorsCount;i++) {
                long id=in.getLong();var position=new ModernWorldMap.Cell(in.getInt(),in.getInt());
                if(id<1||id>room.members().size())fail();
                int heading=u8(in);boolean loaded=bool(in);zero(in,2);
                actors.add(new ModernBattleEngine.Actor(id,room.members().get((int)id-1),position,heading,loaded,in.getLong(),in.getLong()));
            }
            Completion completion=null;
            if(protocolVersion>=3) {boolean present=bool(in);zero(in,3);if(present)completion=getCompletion(in,protocolVersion);}
            var casts=protocolVersion>=4?ModernSpellProtocol.getStates(in,actorsCount):List.<ModernSpellProtocol.CastState>of();
            var descriptor=protocolVersion>=15?ModernMatchDescriptor.get(in):null;
            if(protocolVersion<31&&descriptor!=null&&descriptor.has(ModernMatchDescriptor.NPC_FIRE_STORM))fail();
            if(protocolVersion<27&&descriptor!=null&&descriptor.has(ModernMatchDescriptor.DISINTEGRATE))fail();
            if(protocolVersion<29&&descriptor!=null&&descriptor.has(ModernMatchDescriptor.NPC_DISINTEGRATE))fail();
            if(protocolVersion<17&&descriptor!=null&&descriptor.has(ModernMatchDescriptor.NPC))fail();
            if(protocolVersion<23&&descriptor!=null&&descriptor.has(ModernMatchDescriptor.NPC|ModernMatchDescriptor.SPELLS))fail();
            if(protocolVersion<24&&descriptor!=null&&descriptor.has(ModernMatchDescriptor.NPC_LIGHT_ARROW))fail();
            if(protocolVersion<30&&descriptor!=null&&descriptor.has(ModernMatchDescriptor.TIME_ATTACK|ModernMatchDescriptor.NPC_LIGHT_ARROW))fail();
            var race=protocolVersion>=16?ModernPetRaceProtocol.get(in):ModernPetRaceProtocol.Snapshot.disabled();
            int audience=-1;if(protocolVersion>=20){audience=u8(in)-1;zero(in,3);}
            if(descriptor!=null&&descriptor.has(ModernMatchDescriptor.ISOLATED_FORCES)&&(protocolVersion<20||audience<0))fail();
            var quest=protocolVersion>=21?ModernQuestProtocol.get(in,protocolVersion):ModernQuestProtocol.Data.disabled();
            var cooperative=protocolVersion>=25?ModernTimeAttackProtocol.get(in,new ModernNpcRoster.Scope(room.summary().id(),match,generation),descriptor!=null&&descriptor.has(ModernMatchDescriptor.NPC_FIRE_STORM)):null;
            var siege=protocolVersion>=26?ModernSiegeProtocol.get(in,new ModernSiege.Scope(room.summary().id(),match,generation),new ModernWorldBinding.Key(room.summary().config().mode(),room.summary().config().mapId())):null;
            return new Frame(match,generation,revision,phase,abort,room,manifest,actors,completion,casts,descriptor,race,audience,quest,cooperative,siege);
    }
    private static void validateCompletion(Completion completion,long match,long generation,long revision,
                                           ModernRooms.RoomSnapshot room,List<ModernBattleEngine.Actor> actors) {
        var e=completion.event();
        if(e.resultId().equals(new UUID(0,0)) || e.matchId()!=match || e.generation()!=generation
                || e.engineRevision()<1 || e.engineRevision()>=revision || e.finishedAtMillis()<0
                || e.mode()!=1&&e.mode()!=2&&e.mode()!=5 || e.mode()!=room.summary().config().mode()
                || e.mapId()!=room.summary().config().mapId() || e.participants().size()!=room.members().size())throw bad();
        int winningTeam=-1,losingTeam=-1;
        for(int i=0;i<room.members().size();i++) {
            var p=e.participants().get(i);var a=room.members().get(i);
            if(p.entityId()!=i+1L || !p.identity().equals(a.identity()) || p.team()!=a.team())throw bad();
            if(p.verdict()==ModernMatchSettlement.Verdict.WIN) {
                if(winningTeam!=-1 && winningTeam!=p.team())throw bad();winningTeam=p.team();
            } else {if(losingTeam!=-1 && losingTeam!=p.team())throw bad();losingTeam=p.team();}
        }
        if(winningTeam<0 || losingTeam<0 || winningTeam==losingTeam)throw bad();
    }
    private static void putCompletion(ByteBuffer out,Completion c,int version) throws ModernApplicationProtocol.ProtocolException {
        if(version<13&&c.event().mode()!=1||version<21&&c.event().mode()==2)fail();
        var e=c.event();out.putLong(e.resultId().getMostSignificantBits()).putLong(e.resultId().getLeastSignificantBits())
                .putLong(e.matchId()).putLong(e.generation()).putLong(e.engineRevision()).putLong(e.finishedAtMillis())
                .put((byte)e.mode()).put((byte)c.state().ordinal()).putShort((short)0).putInt(e.mapId())
                .putShort((short)e.participants().size()).putShort((short)0);
        for(var p:e.participants()) {
            if(version>=7)out.put((byte)p.identity().kind().ordinal()).put((byte)0).putShort((short)0);
            else if(!p.identity().isHuman())fail();
            out.putLong(p.entityId()).putInt(p.identity().sourceId()).putLong(p.identity().id())
                    .put((byte)p.team()).put((byte)p.verdict().ordinal()).putShort((short)0);
        }
    }
    private static Completion getCompletion(ByteBuffer in,int version) throws ModernApplicationProtocol.ProtocolException {
        var id=new UUID(in.getLong(),in.getLong());long match=in.getLong(),generation=in.getLong(),revision=in.getLong(),at=in.getLong();
        int mode=u8(in);var state=at(SettlementState.values(),u8(in));zero(in,2);int map=in.getInt(),count=u16(in);zero(in,2);
        if(version<13&&mode!=1||version<21&&mode==2)fail();
        if(count<2 || count>100 || in.remaining()<count*(version>=7?28:24))fail();
        var participants=new ArrayList<ModernMatchSettlement.Participant>(count);
        for(int i=0;i<count;i++) {
            var kind=ModernParticipantId.Kind.HUMAN;
            if(version>=7){kind=at(ModernParticipantId.Kind.values(),u8(in));zero(in,3);}
            long entity=in.getLong();var key=new ModernParticipantId(kind,in.getInt(),in.getLong());
            int team=u8(in);var verdict=at(ModernMatchSettlement.Verdict.values(),u8(in));zero(in,2);
            participants.add(new ModernMatchSettlement.Participant(entity,key,team,verdict));
        }
        return new Completion(new ModernMatchSettlement.Event(id,match,generation,revision,mode,map,at,participants),state);
    }
    private static void validateManifest(ModernWorldMap.Manifest manifest) {
        if(manifest.mapId()<0 || manifest.sectors().isEmpty() || manifest.sectors().size()>ModernWorldMap.MAX_SECTORS || !hash(manifest.digest()) || ZERO_DIGEST.equals(manifest.digest()))throw bad();
        long total=0;int px=-1,py=-1;
        for(var sector:manifest.sectors()) {
            int x=sector.key().x(),y=sector.key().y();
            if(x<px || x==px && y<=py || sector.byteLength()<49158 || sector.byteLength()>8*1024*1024
                    || !hash(sector.rawSha256()) || ZERO_DIGEST.equals(sector.rawSha256()))throw bad();
            total+=sector.byteLength();if(total>32*1024*1024)throw bad();px=x;py=y;
        }
    }
    static boolean contains(ModernWorldMap.Manifest manifest,ModernWorldMap.Cell cell) {
        return manifest.sectors().stream().anyMatch(s->{var o=s.key().origin();return (long)cell.x()>=o.x() && (long)cell.x()<o.x()+64L
                && (long)cell.y()>=o.y() && (long)cell.y()<o.y()+64L;});
    }
    private static boolean hash(String hash){return hash!=null && hash.matches("[0-9a-f]{64}");}
    private static String digest(ByteBuffer in){byte[] hash=new byte[32];in.get(hash);return HexFormat.of().formatHex(hash);}
    private static int u8(ByteBuffer in){return Byte.toUnsignedInt(in.get());}
    private static int u16(ByteBuffer in){return Short.toUnsignedInt(in.getShort());}
    private static boolean bool(ByteBuffer in) throws ModernApplicationProtocol.ProtocolException {int value=u8(in);if(value>1)fail();return value==1;}
    private static void zero(ByteBuffer in,int n) throws ModernApplicationProtocol.ProtocolException {for(int i=0;i<n;i++)if(in.get()!=0)fail();}
    private static <T>T at(T[] values,int n) throws ModernApplicationProtocol.ProtocolException {if(n>=values.length)fail();return values[n];}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid world wire value");}
    private static void fail() throws ModernApplicationProtocol.ProtocolException {throw new ModernApplicationProtocol.ProtocolException();}
}
