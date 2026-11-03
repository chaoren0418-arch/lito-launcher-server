package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** v21 world appendix. Own-force plan/state, not NPC identities or client damage.
 * Only winner + final death revision are public across the force boundary. */
final class ModernQuestProtocol {
    static final int VERSION=21, TARGET_BYTES=24, LANE_BYTES=8, ACTOR_BYTES=48, EXCHANGE_BYTES=64;
    record Lane(int lane,int stage,ModernQuestProgress.Phase phase,int living) {
        Lane {Objects.requireNonNull(phase);if(lane<0||lane>1||stage<1||stage>4||living<0||living>128
                ||(phase==ModernQuestProgress.Phase.FIGHTING)!=(living>0))throw bad();}
    }
    record Actor(int slot,long hp,long bornRevision,long revision,long hurtRevision,long deathRevision) {
        Actor {if(slot<1||slot>8||hp<0||bornRevision<1||revision<bornRevision||hurtRevision<0||hurtRevision>revision
                ||hurtRevision>0&&hurtRevision<=bornRevision||deathRevision<0||deathRevision>revision
                ||(hp==0)!=(deathRevision>0)||deathRevision>0&&deathRevision!=hurtRevision)throw bad();}
        int lane(){return (slot-1)/4;}int stage(){return (slot-1)%4+1;}
    }
    record Exchange(long entityId,int slot,long sequence,long revision,ModernPhysicalCombat.Outcome outcome) {
        Exchange {Objects.requireNonNull(outcome);if(entityId<1||entityId>100||slot<1||slot>8||sequence<1||revision<1
                ||(outcome.remainingHp()==0)!=outcome.death())throw bad();}
        int lane(){return (slot-1)/4;}
    }
    record Data(ModernQuestProgress.Flow flow,long progressRevision,List<Lane> lanes,List<ModernQuestObjectives.Target> targets,
                List<Actor> actors,List<Exchange> exchanges,int winningLane,long deathRevision,List<ModernQuestLife.State> lives) {
        Data(ModernQuestProgress.Flow flow,long progressRevision,List<Lane> lanes,List<ModernQuestObjectives.Target> targets,
                List<Actor> actors,List<Exchange> exchanges,int winningLane,long deathRevision){this(flow,progressRevision,lanes,targets,actors,exchanges,winningLane,deathRevision,List.of());}
        Data {
            lanes=List.copyOf(lanes);targets=List.copyOf(targets);actors=List.copyOf(actors);exchanges=List.copyOf(exchanges);lives=List.copyOf(lives);
            if(progressRevision==0) {
                if(flow!=null||!lanes.isEmpty()||!targets.isEmpty()||!actors.isEmpty()||!exchanges.isEmpty()||!lives.isEmpty()||winningLane!=-1||deathRevision!=0)throw bad();
            } else if(flow==null||progressRevision<1||lanes.isEmpty()||lanes.size()>2||targets.isEmpty()||targets.size()>8
                    ||actors.size()>2||exchanges.size()>100||winningLane< -1||winningLane>1||deathRevision<0||(winningLane>=0)!=(deathRevision>0))throw bad();
        }
        static Data disabled(){return new Data(null,0,List.of(),List.of(),List.of(),List.of(),-1,0);}
        boolean enabled(){return progressRevision>0;}
        ModernQuestObjectives.Target target(int slot){return targets.stream().filter(t->t.slot()==slot).findFirst().orElseThrow(ModernQuestProtocol::bad);}
        Actor actor(int slot){return actors.stream().filter(a->a.slot()==slot).findFirst().orElse(null);}
        Lane lane(int lane){return lanes.stream().filter(l->l.lane()==lane).findFirst().orElseThrow(ModernQuestProtocol::bad);}
        Data forLane(int lane){
            if(!enabled())return this;
            if(lane<0||lane>1)throw bad();lane(lane);
            return new Data(flow,progressRevision,lanes.stream().filter(l->l.lane()==lane).toList(),targets.stream().filter(t->t.lane()==lane).toList(),
                    actors.stream().filter(a->a.lane()==lane).toList(),exchanges.stream().filter(e->e.lane()==lane).toList(),winningLane,deathRevision,lives.stream().filter(l->l.lane()==lane).toList());
        }
        ModernQuestLife.State life(long entity){return lives.stream().filter(l->l.entityId()==entity).findFirst().orElse(null);}
        @Override public String toString(){return "QuestWorldData[redacted]";}
    }
    static Data initial(ModernQuestProgress.Plan plan) {
        return initial(plan,List.of());
    }
    static Data initial(ModernQuestProgress.Plan plan,List<ModernBattleEngine.Actor> players) {
        if(plan==null||plan.objectives()==null)return Data.disabled();
        if(plan.participantLives()&&players.isEmpty())throw bad();
        return new Data(plan.flow(),1,List.of(new Lane(0,1,ModernQuestProgress.Phase.SPAWN_PENDING,0),new Lane(1,1,ModernQuestProgress.Phase.SPAWN_PENDING,0)),
                plan.objectives().targets(),List.of(),List.of(),-1,0,plan.participantLives()?players.stream().map(p->ModernQuestLife.State.initial(p.entityId(),p.member().team(),p.position())).toList():List.of());
    }
    static Data from(ModernBattleEngine.Snapshot world) {
        if(!world.npcFireStorms().isEmpty())throw new IllegalArgumentException("NPC fire storm requires its own typed wire contract");
        return fromFireStorm(world);
    }
    /** Explicit atomic-frame projection seam; not runtime admission. */
    static Data fromFireStorm(ModernBattleEngine.Snapshot world) {
        var q=world.quest();if(!q.enabled())return Data.disabled();var p=q.progress();
        var lanes=new ArrayList<Lane>();for(int i=0;i<2;i++){var l=p.lanes().get(i);lanes.add(new Lane(i,l.stage(),l.phase(),l.living().size()));}
        var actors=q.actors().stream().map(a->new Actor(a.id().slot(),a.hp(),wire(a.bornRevision()),wire(a.revision()),wire(a.hurtRevision()),wire(a.deathRevision()))).toList();
        var exchanges=q.exchanges().stream().map(e->{var a=world.actors().stream().filter(v->v.member().identity().equals(e.participant())).findFirst().orElseThrow(ModernQuestProtocol::bad);
            return new Exchange(a.entityId(),e.target().slot(),e.requestSequence(),wire(e.revision()),e.outcome());}).toList();
        return new Data(p.plan().flow(),p.revision(),lanes,p.plan().objectives().targets(),actors,exchanges,p.winningLane(),
                p.winningLane()<0?0:wire(world.result().engineRevision()),q.lives().stream().map(ModernQuestLife.State::wire).toList());
    }
    static void validate(ModernWorldProtocol.Frame world,Data q) {
        Objects.requireNonNull(q);var d=world.descriptor();
        if(q.enabled()!=(d!=null&&d.has(ModernMatchDescriptor.QUEST_OBJECTIVES)))throw bad();
        if(!q.enabled()){if(world.completion()!=null&&world.completion().event().mode()==2)throw bad();return;}
        if(d.has(ModernMatchDescriptor.QUEST_LIVES)!=!q.lives().isEmpty()||q.lives().size()>100)throw bad();
        if(!q.lives().isEmpty()) {
            if(q.lives().size()!=world.actors().size())throw bad();
            for(int i=0;i<q.lives().size();i++) {
                var life=q.lives().get(i);var player=world.actors().get(i);
                if(life.entityId()!=player.entityId()||life.lane()!=player.member().team()
                        ||life.deathRevision()>world.revision()||life.respawnRevision()>world.revision()
                        ||!ModernWorldProtocol.contains(world.manifest(),life.spawn())
                        ||life.deaths()>0&&!ModernWorldProtocol.contains(world.manifest(),life.deathCell())
                        ||life.respawnRevision()==world.revision()&&!player.position().equals(life.spawn())
                        ||life.deathRevision()==world.revision()&&!player.position().equals(life.deathCell())
                        ||(world.phase()==ModernWorldProtocol.Phase.PREPARING||world.phase()==ModernWorldProtocol.Phase.LOADING||world.phase()==ModernWorldProtocol.Phase.COUNTDOWN)
                            &&(life.deaths()!=0||!life.spawn().equals(player.position())))throw bad();
            }
        }
        int map=world.room().summary().config().mapId(),audience=world.audienceLane();
        var expected=map==513||map==527?ModernQuestProgress.Flow.ROOM_SYMBOLS:ModernQuestProgress.Flow.FINAL_GUARDIAN;
        if(!world.isolatedForces()||world.room().summary().config().mode()!=2||q.flow()!=expected||q.progressRevision()>world.revision()
                ||q.lanes().size()!=(audience<0?2:1)||(world.phase()==ModernWorldProtocol.Phase.FINISHED)!=(q.winningLane()>=0))throw bad();
        int perLane=q.flow()==ModernQuestProgress.Flow.ROOM_SYMBOLS?4:1;
        if(q.targets().size()!=q.lanes().size()*perLane)throw bad();
        for(int i=0;i<q.lanes().size();i++) {
            var lane=q.lanes().get(i);if(lane.lane()!=(audience<0?i:audience))throw bad();
            for(int n=0;n<perLane;n++) {
                var target=q.targets().get(i*perLane+n);
                if(target.lane()!=lane.lane()||target.stage()!=(perLane==1?4:n+1)||!ModernWorldProtocol.contains(world.manifest(),target.cell()))throw bad();
            }
        }
        int previous=0;var seenLanes=new HashSet<Integer>();
        for(var a:q.actors()) {
            var t=q.target(a.slot());var l=q.lane(a.lane());
            if(a.slot()<=previous||!seenLanes.add(a.lane())||a.hp()>t.maximumHp()||a.revision()>world.revision()
                    ||(a.hp()<t.maximumHp())!=(a.hurtRevision()>0)||a.revision()==a.bornRevision()&&(a.hp()!=t.maximumHp()||a.hurtRevision()!=0)
                    ||a.hp()>0&&(a.stage()!=l.stage()||l.phase()!=ModernQuestProgress.Phase.OBJECTIVE
                        ||world.actors().stream().anyMatch(p->p.member().team()==a.lane()&&p.position().equals(t.cell())))
                    ||a.hp()==0&&(a.stage()==l.stage()?q.winningLane()!=a.lane()||a.stage()!=4:a.stage()!=l.stage()-1||a.stage()==4))throw bad();
            previous=a.slot();
        }
        long eventRevision=0;var humans=new HashSet<Long>();
        for(var e:q.exchanges()) {
            var t=q.target(e.slot());var p=world.actors().stream().filter(a->a.entityId()==e.entityId()).findFirst().orElseThrow(ModernQuestProtocol::bad);
            long priorHp;try{priorHp=Math.addExact(e.outcome().appliedDamage(),e.outcome().remainingHp());}catch(ArithmeticException failure){throw bad();}
            var a=q.actor(e.slot());
            if(!p.member().identity().isHuman()||p.member().team()!=e.lane()||!humans.add(e.entityId())||e.revision()<=eventRevision||e.revision()>world.revision()
                    ||priorHp<1||priorHp>t.maximumHp()||t.stage()>q.lane(e.lane()).stage()
                    ||a!=null&&(e.revision()<=a.bornRevision()||e.revision()>a.revision()||e.outcome().remainingHp()<a.hp()
                        ||e.revision()==a.revision()&&(e.outcome().remainingHp()!=a.hp()||e.outcome().appliedDamage()>0&&a.hurtRevision()!=e.revision())))throw bad();
            eventRevision=e.revision();
        }
        if(world.phase()==ModernWorldProtocol.Phase.PREPARING||world.phase()==ModernWorldProtocol.Phase.LOADING||world.phase()==ModernWorldProtocol.Phase.COUNTDOWN) {
            if(q.progressRevision()!=1||!q.actors().isEmpty()||!q.exchanges().isEmpty()
                    ||q.lanes().stream().anyMatch(l->l.stage()!=1||l.phase()!=ModernQuestProgress.Phase.SPAWN_PENDING))throw bad();
        }
        if(q.winningLane()>=0) {
            var event=world.completion().event();
            if(event.mode()!=2||q.deathRevision()!=wire(event.engineRevision())||q.deathRevision()>world.revision()
                    ||event.participants().stream().anyMatch(p->(p.verdict()==ModernMatchSettlement.Verdict.WIN)!=(p.team()==q.winningLane())))throw bad();
            if(audience<0||audience==q.winningLane()) {
                var winner=q.actor(q.winningLane()*4+4);var lane=q.lane(q.winningLane());
                if(winner==null||winner.hp()!=0||winner.deathRevision()!=q.deathRevision()||lane.stage()!=4||lane.phase()!=ModernQuestProgress.Phase.OBJECTIVE
                        ||!d.has(ModernMatchDescriptor.NPC_FIRE_STORM)&&q.exchanges().stream().noneMatch(e->e.slot()==winner.slot()&&e.revision()==q.deathRevision()&&e.outcome().death()))throw bad();
            }
        }
    }
    static void validateCombat(ModernWorldProtocol.Frame world,List<ModernCombatProtocol.Vital> vitals) {
        for(int i=0;i<world.quest().lives().size();i++)world.quest().lives().get(i).validate(world.revision(),world.manifest(),world.actors().get(i),vitals.get(i));
        for(var e:world.quest().exchanges()) {
            var v=vitals.stream().filter(a->a.entityId()==e.entityId()).findFirst().orElseThrow(ModernQuestProtocol::bad);
            if(e.sequence()>v.attackSequence()||e.revision()==world.revision()&&(v.hp()==0||e.sequence()!=v.attackSequence()))throw bad();
        }
    }
    static void validateNpcs(ModernWorldProtocol.Frame world,List<ModernNpcProtocol.Actor> npcs) {
        var q=world.quest();if(!q.enabled()||world.phase()==ModernWorldProtocol.Phase.ABORTED)return;
        for(var lane:q.lanes()) {
            var living=npcs.stream().filter(a->a.lane()==lane.lane()&&a.hp()>0).toList();
            if(living.size()!=lane.living()||living.stream().anyMatch(a->a.stage()!=lane.stage()))throw bad();
            for(var a:q.actors())if(a.lane()==lane.lane()&&a.hp()>0&&living.stream().anyMatch(n->n.cell().equals(q.target(a.slot()).cell())))throw bad();
        }
    }
    static void put(ByteBuffer out,Data q) {
        put(out,q,21);
    }
    static void put(ByteBuffer out,Data q,int version) {
        if(!q.enabled()){out.putLong(0);return;}
        if(!q.lives().isEmpty()&&(version<22||q.lives().size()>100))throw bad();
        out.put((byte)1).put((byte)(q.lives().isEmpty()?1:2)).put((byte)q.flow().ordinal()).put((byte)q.targets().size()).put((byte)q.lanes().size()).put((byte)q.actors().size()).putShort((short)q.exchanges().size());
        out.putLong(q.progressRevision()).putLong(q.deathRevision()).put((byte)(q.winningLane()+1)).put((byte)q.lives().size());for(int i=0;i<6;i++)out.put((byte)0);
        for(var l:q.lanes())out.put((byte)l.lane()).put((byte)l.stage()).put((byte)l.phase().ordinal()).put((byte)0).putShort((short)l.living()).putShort((short)0);
        for(var t:q.targets())out.put((byte)t.slot()).put((byte)0).putShort((short)0).putInt(t.graphic()).putInt(t.cell().x()).putInt(t.cell().y()).putLong(t.maximumHp());
        for(var a:q.actors())out.put((byte)a.slot()).put((byte)0).putShort((short)0).putInt(0).putLong(a.hp()).putLong(a.bornRevision()).putLong(a.revision()).putLong(a.hurtRevision()).putLong(a.deathRevision());
        for(var e:q.exchanges()) {
            var o=e.outcome();int flags=(o.hit()?1:0)|(o.death()?2:0)|(o.maximumTriggered()?4:0)|(o.criticalTriggered()?8:0);
            out.putLong(e.entityId()).put((byte)e.slot()).put((byte)flags).putShort((short)0).putInt(0).putLong(e.sequence()).putLong(e.revision())
                    .putLong(o.rawDamage()).putLong(o.appliedDamage()).putLong(o.remainingHp()).putLong(0);
        }
        for(var l:q.lives())out.putLong(l.entityId()).put((byte)l.lane()).put((byte)0).putShort((short)0).putInt(0)
                .putLong(l.deaths()).putLong(l.deathRevision()).putLong(l.respawns()).putLong(l.respawnRevision()).putLong(l.hpGain())
                .putInt(l.spawn().x()).putInt(l.spawn().y()).putInt(l.deathCell().x()).putInt(l.deathCell().y());
    }
    static Data get(ByteBuffer in) {
        return get(in,21);
    }
    static Data get(ByteBuffer in,int version) {
        int present=u8(in);if(present==0){zero(in,7);return Data.disabled();}if(present!=1)throw bad();
        int schema=u8(in);if(schema!=1&&(schema!=2||version<22))throw bad();
        var flow=at(ModernQuestProgress.Flow.values(),u8(in));int targets=u8(in),lanes=u8(in),actors=u8(in),events=Short.toUnsignedInt(in.getShort());
        long revision=in.getLong(),death=in.getLong();int winner=u8(in)-1,lives=u8(in);zero(in,6);
        if((schema==2)!=(lives>0)||lives>100||targets>8||lanes>2||actors>2||events>100||in.remaining()<targets*TARGET_BYTES+lanes*LANE_BYTES+actors*ACTOR_BYTES+events*EXCHANGE_BYTES+lives*72)throw bad();
        var ls=new ArrayList<Lane>();var ts=new ArrayList<ModernQuestObjectives.Target>();var as=new ArrayList<Actor>();var es=new ArrayList<Exchange>();
        for(int i=0;i<lanes;i++){int lane=u8(in),stage=u8(in);var phase=at(ModernQuestProgress.Phase.values(),u8(in));zero(in,1);int living=Short.toUnsignedInt(in.getShort());zero(in,2);ls.add(new Lane(lane,stage,phase,living));}
        for(int i=0;i<targets;i++){int slot=u8(in);zero(in,3);int gfx=in.getInt(),x=in.getInt(),y=in.getInt();long hp=in.getLong();if(slot<1||slot>8)throw bad();ts.add(new ModernQuestObjectives.Target((slot-1)/4,(slot-1)%4+1,new ModernWorldMap.Cell(x,y),gfx,hp));}
        for(int i=0;i<actors;i++){int slot=u8(in);zero(in,7);as.add(new Actor(slot,in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong()));}
        for(int i=0;i<events;i++){long entity=in.getLong();int slot=u8(in),flags=u8(in);if(flags>15)throw bad();zero(in,6);long seq=in.getLong(),rev=in.getLong();
            var hit=new ModernPhysicalCombat.Outcome((flags&1)!=0,in.getLong(),in.getLong(),in.getLong(),(flags&2)!=0,(flags&4)!=0,(flags&8)!=0);zero(in,8);es.add(new Exchange(entity,slot,seq,rev,hit));}
        var lifeRows=new ArrayList<ModernQuestLife.State>();
        for(int i=0;i<lives;i++){long entity=in.getLong();int lane=u8(in);zero(in,7);long deaths=in.getLong(),deathRev=in.getLong(),respawns=in.getLong(),respawnRev=in.getLong(),gain=in.getLong();
            lifeRows.add(new ModernQuestLife.State(entity,lane,new ModernWorldMap.Cell(in.getInt(),in.getInt()),deaths,deathRev,respawns,respawnRev,gain,new ModernWorldMap.Cell(in.getInt(),in.getInt())));}
        return new Data(flow,revision,ls,ts,as,es,winner,death,lifeRows);
    }
    private static long wire(long revision){return revision==0?0:Math.incrementExact(revision);}
    private static int u8(ByteBuffer in){return Byte.toUnsignedInt(in.get());}
    private static void zero(ByteBuffer in,int n){while(n-->0)if(in.get()!=0)throw bad();}
    private static <T>T at(T[] values,int i){if(i<0||i>=values.length)throw bad();return values[i];}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid Quest world appendix");}
    private ModernQuestProtocol(){}
}
