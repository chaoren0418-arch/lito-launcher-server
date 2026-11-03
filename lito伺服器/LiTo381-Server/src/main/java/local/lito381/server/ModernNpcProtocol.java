package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** v17 explicit NPC namespace. No database names, client damage, dice or time are transmitted. */
final class ModernNpcProtocol {
    static final int VERSION=17, RESOURCE_VERSION=18, REQUEST_KIND=38, RESULT_KIND=39, REQUEST_BYTES=72, MAX_FRAME_BYTES=256*1024;
    /** Candidate v31 atomic frame bound. Application framing remains v30 until admitted. */
    static int maxFrameBytes(int version){return version>=31?1024*1024:MAX_FRAME_BYTES;}
    static final int ACTOR_BYTES=112, EXCHANGE_BYTES=104;
    enum Action { READ, ATTACK, ATTACK_OBJECTIVE, CONFIRM_DEATH, ATTACK_TOWER }
    enum Status { OK, DENIED, BUSY, UNAVAILABLE }
    enum Reason { NONE, NOT_ACTIVE, DEAD, TARGET_DEAD, WRONG_LANE, OUT_OF_RANGE, BLOCKED, TOO_FAST, TOO_HEAVY, UNSUPPORTED, NUMERIC, STALE_STATE, NOT_DEAD, STALE_LIFE, SPAWN_BLOCKED }
    record Request(ModernRooms.Scope player, Action action, ModernNpcRoster.Scope world, long sequence, long serial, long life) {
        Request {
            Objects.requireNonNull(player);Objects.requireNonNull(action);Objects.requireNonNull(world);
            if(action==Action.READ?sequence!=0||serial!=0||life!=0:
                    action==Action.ATTACK_TOWER?sequence<1||serial!=0||life!=0:sequence<1||serial<1
                    ||(action==Action.ATTACK_OBJECTIVE?serial>8||life!=0:life<1))throw bad();
            if(action==Action.CONFIRM_DEATH&&(serial>100||sequence<2))throw bad();
        }
        ModernNpcRoster.Id target(){if(action!=Action.ATTACK)throw bad();return new ModernNpcRoster.Id(world,serial);}
        ModernQuestObjectives.Id objective(){if(action!=Action.ATTACK_OBJECTIVE)throw bad();return new ModernQuestObjectives.Id(world,Math.toIntExact(serial));}
        ModernTimeAttack.TowerId tower(){if(action!=Action.ATTACK_TOWER)throw bad();return new ModernTimeAttack.TowerId(world);}
    }
    record Actor(long serial,int catalogSourceId,int npcId,int graphic,int lane,int stage,int heading,ModernNpcRoster.Action action,
                 ModernWorldMap.Cell cell,long hp,long maxHp,long mp,long maxMp,long life,long bornRevision,long changedRevision,
                 long corpseUntilMillis,long nextMoveMillis,long nextAttackMillis) {
        Actor {
            Objects.requireNonNull(action);Objects.requireNonNull(cell);
            long maxTime=ModernNpcRoster.MAX_ELAPSED_MILLIS+60_000;
            if(serial<1||catalogSourceId<1||npcId<1||!ModernNpcRoster.supportedGraphic(graphic)||lane<0||lane>1||stage<1||stage>4||heading<0||heading>7
                    ||hp<0||maxHp<1||hp>maxHp||mp<0||maxMp<0||mp>maxMp||life<1||bornRevision<1||changedRevision<bornRevision
                    ||corpseUntilMillis<0||corpseUntilMillis>maxTime||nextMoveMillis<0||nextMoveMillis>maxTime||nextAttackMillis<0||nextAttackMillis>maxTime
                    ||(hp==0)!=(action==ModernNpcRoster.Action.DEATH)||(hp==0)!=(corpseUntilMillis>0)||hp==0&&(nextMoveMillis!=0||nextAttackMillis!=0))throw bad();
        }
        static Actor from(ModernNpcRoster.Actor a){ModernNpcSpeed.requireFrozen(a);var t=a.template();return new Actor(a.id().serial(),t.catalogSourceId(),t.npcId(),t.graphic(),a.lane(),a.stage(),a.heading(),a.action(),a.cell(),
                a.hp(),t.maximumHp(),a.mp(),t.maximumMp(),a.life(),a.bornRevision(),a.changedRevision(),a.corpseUntilMillis(),a.nextMoveMillis(),a.nextAttackMillis());}
    }
    /** Immutable visual requirements for the WHOLE generation, including forms
     * not spawned yet. No database key/name or NPC combat stats in this list.
     * Entry owns the matching scope/manifest; it cannot change after LOADED. */
    record Resources(List<Integer> graphics) {
        Resources {
            graphics=List.copyOf(graphics);
            if(graphics.isEmpty()||graphics.size()>10)throw bad();
            int previous=0;
            for(int graphic:graphics) { if(graphic<=previous||!ModernNpcRoster.supportedGraphic(graphic))throw bad(); previous=graphic; }
        }
        static Resources from(ModernNpcCatalog.Catalog catalog) {
            return new Resources(catalog.npcs().values().stream().map(n->Math.toIntExact(n.get(ModernNpcCatalog.Field.GRAPHIC))).distinct().sorted().toList());
        }
    }
    record Frame(ModernCombatProtocol.Frame combat,ModernNpcRoster.Scope scope,long revision,long elapsedMillis,long spawned,boolean closed,
                 List<Actor> actors,List<ModernNpcBattle.Exchange> exchanges,Resources resources,ModernNpcNames.Table names) {
        Frame(ModernCombatProtocol.Frame combat,ModernNpcRoster.Scope scope,long revision,long elapsedMillis,long spawned,boolean closed,
              List<Actor> actors,List<ModernNpcBattle.Exchange> exchanges,Resources resources) {
            this(combat,scope,revision,elapsedMillis,spawned,closed,actors,exchanges,resources,null);
        }
        Frame(ModernCombatProtocol.Frame combat,ModernNpcRoster.Scope scope,long revision,long elapsedMillis,long spawned,boolean closed,
              List<Actor> actors,List<ModernNpcBattle.Exchange> exchanges) {
            this(combat,scope,revision,elapsedMillis,spawned,closed,actors,exchanges,null);
        }
        Frame {
            Objects.requireNonNull(combat);Objects.requireNonNull(scope);actors=List.copyOf(actors);exchanges=List.copyOf(exchanges);
            if(names!=null)names.validate(actors,resources);
            var world=combat.world();var d=world.descriptor();
            if(d==null||!d.has(ModernMatchDescriptor.NPC)||scope.roomId()!=world.room().summary().id()||scope.matchId()!=world.matchId()||scope.generation()!=world.generation()
                    ||revision<1||revision>=world.revision()||elapsedMillis<0||elapsedMillis>ModernNpcRoster.MAX_ELAPSED_MILLIS||spawned<0
                    ||actors.size()>128||exchanges.size()>228||closed!=(world.phase()==ModernWorldProtocol.Phase.ABORTED)||closed&&!actors.isEmpty()
                    ||world.phase()==ModernWorldProtocol.Phase.PREPARING||world.phase()==ModernWorldProtocol.Phase.FINISHED&&!world.quest().enabled()&&world.timeAttack()==null
                    ||world.phase()!=ModernWorldProtocol.Phase.ACTIVE&&world.phase()!=ModernWorldProtocol.Phase.ABORTED&&world.phase()!=ModernWorldProtocol.Phase.FINISHED&&(spawned!=0||elapsedMillis!=0||!exchanges.isEmpty()))throw bad();
            d.validateCombat(combat);
            ModernQuestProtocol.validateNpcs(world,actors);
            ModernNpcSpellProtocol.validateNpcs(combat,scope,spawned,actors);
            ModernNpcDisintegrateWire.validateNpcs(combat,scope,spawned,actors,exchanges);
            ModernNpcFireStormFrame.validateNpcs(combat,scope,spawned,actors,exchanges);
            ModernTimeAttackProtocol.validateNpcs(combat,spawned,closed,actors);
            var cells=new HashSet<ModernQuestSpace.Cell>();for(var p:world.actors())if(world.timeAttack()==null||!world.timeAttack().departed(p.entityId()))cells.add(ModernQuestSpace.cell(world.isolatedForces(),p.member().team(),p.position()));
            if(world.timeAttack()!=null&&world.timeAttack().towerHp()>0)cells.add(ModernQuestSpace.cell(false,0,world.timeAttack().tower().cell()));long previous=0;
            for(var a:actors) {
                if(a.serial()<=previous||a.serial()>spawned||a.changedRevision()>revision||!ModernWorldProtocol.contains(world.manifest(),a.cell())
                        ||world.audienceLane()>=0&&a.lane()!=world.audienceLane()
                        ||a.hp()>0&&!cells.add(ModernQuestSpace.cell(world.isolatedForces(),a.lane(),a.cell()))||a.hp()==0&&a.corpseUntilMillis()<=elapsedMillis)throw bad();
                previous=a.serial();
                if(resources!=null&&!resources.graphics().contains(a.graphic()))throw bad();
            }
            var humans=new HashSet<ModernParticipantId>();var npcs=new HashSet<ModernNpcRoster.Id>();previous=0;
            for(var e:exchanges) {
                if(!e.npc().scope().equals(scope)||e.npc().serial()>spawned||e.revision()<=previous||e.revision()>world.revision())throw bad();
                var p=world.actors().stream().filter(a->a.member().identity().equals(e.participant())).findFirst().orElseThrow(ModernNpcProtocol::bad);
                var v=combat.vital(p.entityId());var n=actors.stream().filter(a->a.serial()==e.npc().serial()).findFirst().orElse(null);
                var playerLife=world.quest().life(p.entityId());
                if(playerLife==null?e.participantLife()!=1:e.participantLife()>playerLife.incarnation()
                        ||(e.participantLife()==playerLife.incarnation()?e.revision()<=playerLife.respawnRevision():e.revision()>playerLife.respawnRevision()))throw bad();
                if(n!=null&&world.isolatedForces()&&n.lane()!=p.member().team())throw bad();
                if(n!=null&&e.npcLife()>n.life())throw bad();
                if(e.direction()==ModernNpcBattle.Direction.PLAYER_TO_NPC) {
                    if(!humans.add(e.participant())||e.requestSequence()>v.attackSequence())throw bad();
                    if(e.revision()==world.revision()&&(n==null||n.life()!=e.npcLife()||n.hp()!=e.outcome().remainingHp()||v.hp()==0||e.requestSequence()!=v.attackSequence()))throw bad();
                } else {
                    if(!npcs.add(e.npc())||n==null)throw bad();
                    if(e.revision()==world.revision()&&(v.hp()!=e.outcome().remainingHp()||n.life()!=e.npcLife()||n.action()!=ModernNpcRoster.Action.ATTACK
                            ||e.outcome().appliedDamage()>0&&v.hurtRevision()!=e.revision()))throw bad();
                }
                previous=e.revision();
            }
        }
        static Frame from(ModernNpcBattle.Frame snapshot,ModernMatchDescriptor descriptor) {
            if(snapshot.world().isolatedForces()!=descriptor.has(ModernMatchDescriptor.ISOLATED_FORCES))throw bad();
            var w=snapshot.world();var n=snapshot.npcs();var world=ModernWorldService.fromEngine(w,Math.incrementExact(w.revision()),descriptor);
            return new Frame(ModernWorldService.fromCombat(w,world),n.scope(),n.revision(),n.elapsedMillis(),n.spawned(),n.closed(),n.actors().stream().map(Actor::from).toList(),
                    snapshot.exchanges().stream().map(e->new ModernNpcBattle.Exchange(e.direction(),e.participant(),e.participantLife(),e.npc(),e.npcLife(),e.requestSequence(),Math.incrementExact(e.revision()),e.outcome())).toList());
        }
        Frame withResources(Resources required) {
            return new Frame(combat,scope,revision,elapsedMillis,spawned,closed,actors,exchanges,Objects.requireNonNull(required),names);
        }
        Frame withCombat(ModernCombatProtocol.Frame frame) {
            return new Frame(frame,scope,revision,elapsedMillis,spawned,closed,actors,exchanges,resources,names);
        }
        Frame withNames(ModernNpcNames.Table table) {
            return new Frame(combat,scope,revision,elapsedMillis,spawned,closed,actors,exchanges,resources,table);
        }
    }
    record Reply(Status status,Reason reason,Frame frame) {
        Reply {Objects.requireNonNull(status);Objects.requireNonNull(reason);if((status==Status.OK)!=(reason==Reason.NONE)||frame==null&&!(status==Status.UNAVAILABLE&&reason==Reason.NOT_ACTIVE))throw bad();}
    }
    static void validate(ModernApplicationProtocol.NpcResult message) {
        var r=Objects.requireNonNull(message.echo());var reply=Objects.requireNonNull(message.reply());var frame=reply.frame();if(frame==null)return;
        var world=frame.combat().world();if(!frame.scope().equals(r.world()))throw bad();
        var id=ModernParticipantId.human(r.player().sourceId(),r.player().objectId());
        var self=world.actors().stream().filter(a->a.member().identity().equals(id)).findFirst().orElseThrow(ModernNpcProtocol::bad);
        var vital=frame.combat().vital(self.entityId());
        if(r.action()==Action.CONFIRM_DEATH) {
            var life=world.quest().life(self.entityId());
            if(life==null||r.serial()!=self.entityId())throw bad();
            if(reply.status()==Status.OK&&(world.phase()!=ModernWorldProtocol.Phase.ACTIVE||life.dead()||vital.hp()!=vital.maxHp()
                    ||life.deathRevision()!=r.sequence()||life.incarnation()!=Math.incrementExact(r.life())||life.respawnRevision()!=world.revision()
                    ||!self.position().equals(life.spawn())))throw bad();
            if(reply.status()==Status.DENIED&&!(reply.reason()==Reason.NOT_ACTIVE||reply.reason()==Reason.NOT_DEAD||reply.reason()==Reason.STALE_LIFE
                    ||reply.reason()==Reason.SPAWN_BLOCKED||reply.reason()==Reason.NUMERIC))throw bad();
            if(reply.status()==Status.DENIED&&reply.reason()!=Reason.NOT_ACTIVE&&world.phase()!=ModernWorldProtocol.Phase.ACTIVE)throw bad();
            if(reply.reason()==Reason.NOT_DEAD&&life.dead()||reply.reason()==Reason.SPAWN_BLOCKED&&!life.dead())throw bad();
            return;
        }
        if(reply.reason().ordinal()>=Reason.NOT_DEAD.ordinal())throw bad();
        if(r.action()==Action.ATTACK_TOWER) {
            var d=world.timeAttack();if(d==null)throw bad();
            if(reply.status()==Status.OK&&(world.phase()!=ModernWorldProtocol.Phase.ACTIVE||vital.hp()==0||d.departed(self.entityId())||vital.attackSequence()!=r.sequence()
                    ||d.exchanges().stream().noneMatch(e->e.entityId()==self.entityId()&&e.sequence()==r.sequence()&&Math.incrementExact(e.revision())==world.revision())))throw bad();
        }
        if(r.action()==Action.ATTACK_OBJECTIVE) {
            if(!world.quest().enabled())throw bad();
            if(reply.status()==Status.OK&&(world.phase()!=ModernWorldProtocol.Phase.ACTIVE&&world.phase()!=ModernWorldProtocol.Phase.FINISHED
                    ||vital.hp()==0||vital.attackSequence()!=r.sequence()
                    ||world.quest().exchanges().stream().noneMatch(e->e.entityId()==self.entityId()&&e.slot()==r.serial()&&e.sequence()==r.sequence()
                        &&e.revision()==(world.completion()==null?world.revision():Math.incrementExact(world.completion().event().engineRevision())))))throw bad();
        }
        if(r.action()==Action.ATTACK&&reply.status()==Status.OK) {
            // SQL may publish SAVED/FAILED before this final attack is encoded.
            // The receipt pins its engine event; never accept an arbitrary older hit.
            long attackRevision=world.timeAttack()!=null&&world.timeAttack().receipt()!=null
                    ?Math.incrementExact(world.timeAttack().receipt().revision()):world.revision();
            if(world.phase()!=ModernWorldProtocol.Phase.ACTIVE&&!(world.phase()==ModernWorldProtocol.Phase.FINISHED&&world.timeAttack()!=null)||vital.hp()==0||vital.attackSequence()!=r.sequence()
                    ||frame.exchanges().stream().noneMatch(e->e.direction()==ModernNpcBattle.Direction.PLAYER_TO_NPC&&e.participant().equals(id)
                        &&e.npc().equals(r.target())&&e.npcLife()==r.life()&&e.requestSequence()==r.sequence()&&e.revision()==attackRevision))throw bad();
        }
        if(reply.status()==Status.DENIED&&reply.reason()!=Reason.NOT_ACTIVE) {
            if(r.action()==Action.READ||world.phase()!=ModernWorldProtocol.Phase.ACTIVE
                    ||(reply.reason()==Reason.STALE_STATE?vital.attackSequence()<r.sequence():vital.attackSequence()!=r.sequence()))throw bad();
            if(reply.reason()==Reason.DEAD&&vital.hp()!=0)throw bad();
        }
    }
    static void putRequest(ByteBuffer b,Request r) {
        putRequest(b,r,VERSION);
    }
    static void putRequest(ByteBuffer b,Request r,int version) {
        if(r.action()==Action.ATTACK_TOWER&&version<25)throw bad();
        if(r.action()==Action.ATTACK_OBJECTIVE&&version<21)throw bad();
        if(r.action()==Action.CONFIRM_DEATH&&version<22)throw bad();
        var p=r.player();var s=r.world();b.putInt(p.sourceId()).putLong(p.objectId()).putLong(p.epoch()).put((byte)p.channelId()).put((byte)r.action().ordinal()).putShort((short)0);
        b.putLong(s.roomId()).putLong(s.matchId()).putLong(s.generation()).putLong(r.sequence()).putLong(r.serial()).putLong(r.life());
    }
    static Request getRequest(ByteBuffer b){return getRequest(b,VERSION);}
    static Request getRequest(ByteBuffer b,int version){int source=b.getInt();long object=b.getLong(),epoch=b.getLong();int channel=u8(b);var a=at(Action.values(),u8(b));zero(b,2);
        if(a==Action.ATTACK_TOWER&&version<25)throw bad();
        if(a==Action.ATTACK_OBJECTIVE&&version<21)throw bad();
        if(a==Action.CONFIRM_DEATH&&version<22)throw bad();
        return new Request(new ModernRooms.Scope(source,object,epoch,channel),a,new ModernNpcRoster.Scope(b.getLong(),b.getLong(),b.getLong()),b.getLong(),b.getLong(),b.getLong());}
    static void putResult(ByteBuffer b,ModernApplicationProtocol.NpcResult m,int version)throws ModernApplicationProtocol.ProtocolException {
        if(version<VERSION)throw bad();validate(m);putRequest(b,m.echo(),version);var r=m.reply();b.put((byte)r.status().ordinal()).put((byte)r.reason().ordinal()).put((byte)(r.frame()==null?0:1)).put((byte)0);
        if(r.frame()!=null)putFrame(b,r.frame(),version);
    }
    static ModernApplicationProtocol.NpcResult getResult(ByteBuffer b,int requestId,int version)throws ModernApplicationProtocol.ProtocolException {
        if(version<VERSION)throw bad();var r=getRequest(b,version);var s=at(Status.values(),u8(b));var reason=at(Reason.values(),u8(b));int present=u8(b);if(present>1)throw bad();zero(b,1);
        return new ModernApplicationProtocol.NpcResult(requestId,r,new Reply(s,reason,present==1?getFrame(b,version):null));
    }
    static void putFrame(ByteBuffer b,Frame f,int version)throws ModernApplicationProtocol.ProtocolException {
        int start=b.position();
        if((version>=RESOURCE_VERSION)!=(f.resources()!=null))throw bad();
        ModernCombatProtocol.putFrame(b,f.combat(),version);var s=f.scope();b.putLong(s.roomId()).putLong(s.matchId()).putLong(s.generation()).putLong(f.revision()).putLong(f.elapsedMillis()).putLong(f.spawned());
        b.put((byte)(f.closed()?1:0)).put((byte)0).putShort((short)0).putShort((short)f.actors().size()).putShort((short)f.exchanges().size());
        for(var a:f.actors())b.putLong(a.serial()).putInt(a.catalogSourceId()).putInt(a.npcId()).putInt(a.graphic()).put((byte)a.lane()).put((byte)a.stage()).put((byte)a.heading()).put((byte)a.action().ordinal())
                .putInt(a.cell().x()).putInt(a.cell().y()).putLong(a.hp()).putLong(a.maxHp()).putLong(a.mp()).putLong(a.maxMp()).putLong(a.life()).putLong(a.bornRevision()).putLong(a.changedRevision())
                .putLong(a.corpseUntilMillis()).putLong(a.nextMoveMillis()).putLong(a.nextAttackMillis());
        for(var e:f.exchanges()) {
            var o=e.outcome();int flags=(o.hit()?1:0)|(o.death()?2:0)|(o.maximumTriggered()?4:0)|(o.criticalTriggered()?8:0);var p=e.participant();
            b.put((byte)p.kind().ordinal()).put((byte)e.direction().ordinal()).put((byte)flags).put((byte)0).putInt(p.sourceId()).putLong(p.id())
                    .putLong(e.participantLife()).putLong(e.npc().serial()).putLong(e.npcLife()).putLong(e.requestSequence()).putLong(e.revision())
                    .putLong(o.rawDamage()).putLong(o.damage()).putLong(o.appliedDamage()).putLong(o.remainingHp()).putLong(o.materialBonus()).putLong(o.blessingBonus());
        }
        if(version>=RESOURCE_VERSION) {
            b.putShort((short)1).putShort((short)f.resources().graphics().size()).putInt(0);
            for(int graphic:f.resources().graphics())b.putInt(graphic);
        }
        // Older peers retain their exact original payload; labels have no
        // gameplay authority and are explicitly absent in the old projection.
        if(version>=ModernNpcNames.VERSION)ModernNpcNames.put(b,f.names());
        if(b.position()-start>maxFrameBytes(version)-ModernSessionProtocol.HEADER_BYTES)throw bad();
    }
    static Frame getFrame(ByteBuffer b,int version)throws ModernApplicationProtocol.ProtocolException {
        int start=b.position();
        var combat=ModernCombatProtocol.getFrame(b,version);var scope=new ModernNpcRoster.Scope(b.getLong(),b.getLong(),b.getLong());long revision=b.getLong(),elapsed=b.getLong(),spawned=b.getLong();
        int closed=u8(b);if(closed>1)throw bad();zero(b,3);int count=Short.toUnsignedInt(b.getShort()),events=Short.toUnsignedInt(b.getShort());
        if(count>128||events>228||b.remaining()<count*ACTOR_BYTES+events*EXCHANGE_BYTES)throw bad();var actors=new ArrayList<Actor>(count);
        for(int i=0;i<count;i++)actors.add(new Actor(b.getLong(),b.getInt(),b.getInt(),b.getInt(),u8(b),u8(b),u8(b),at(ModernNpcRoster.Action.values(),u8(b)),
                new ModernWorldMap.Cell(b.getInt(),b.getInt()),b.getLong(),b.getLong(),b.getLong(),b.getLong(),b.getLong(),b.getLong(),b.getLong(),b.getLong(),b.getLong(),b.getLong()));
        var exchanges=new ArrayList<ModernNpcBattle.Exchange>(events);
        for(int i=0;i<events;i++) {
            var kind=at(ModernParticipantId.Kind.values(),u8(b));var direction=at(ModernNpcBattle.Direction.values(),u8(b));int flags=u8(b);if(flags>15)throw bad();zero(b,1);
            var participant=new ModernParticipantId(kind,b.getInt(),b.getLong());long playerLife=b.getLong();var id=new ModernNpcRoster.Id(scope,b.getLong());
            long life=b.getLong(),sequence=b.getLong(),eventRevision=b.getLong();
            var outcome=new ModernNpcMelee.Outcome((flags&1)!=0,b.getLong(),b.getLong(),b.getLong(),b.getLong(),(flags&2)!=0,(flags&4)!=0,(flags&8)!=0,b.getLong(),b.getLong());
            exchanges.add(new ModernNpcBattle.Exchange(direction,participant,playerLife,id,life,sequence,eventRevision,outcome));
        }
        Resources resources=null;
        if(version>=RESOURCE_VERSION) {
            if(b.getShort()!=1)throw bad();int forms=Short.toUnsignedInt(b.getShort());zero(b,4);
            if(forms<1||forms>10||b.remaining()<forms*4)throw bad();
            var graphics=new ArrayList<Integer>(forms);for(int i=0;i<forms;i++)graphics.add(b.getInt());resources=new Resources(graphics);
        }
        var names=version>=ModernNpcNames.VERSION?ModernNpcNames.get(b):null;
        if(b.position()-start>maxFrameBytes(version)-ModernSessionProtocol.HEADER_BYTES)throw bad();
        return new Frame(combat,scope,revision,elapsed,spawned,closed==1,actors,exchanges,resources,names);
    }
    private static int u8(ByteBuffer b){return Byte.toUnsignedInt(b.get());}
    private static void zero(ByteBuffer b,int n){while(n-->0)if(b.get()!=0)throw bad();}
    private static <T>T at(T[] values,int index){if(index>=values.length)throw bad();return values[index];}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid modern NPC wire value");}
    private ModernNpcProtocol(){}
}
