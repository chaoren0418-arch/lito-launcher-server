package local.lito381.server;

import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

/** Bounded, canonical owner-history section for the future atomic world
 * envelope. Not an application opcode, SET permission, or standalone world.
 * All integers are big endian; selection epoch is opaque 64-bit data. */
final class ModernEquipmentHistoryWire {
    static final int MAGIC=0x4c484931, SCHEMA=1, HEADER_BYTES=256, MAX_BYTES=32*1024, EFFECT_BYTES=112;
    static final int GREATER_HASTE_SCHEMA=2;
    static final int HASTE_SCHEMA=3, SLOW_SCHEMA=4;
    // Candidate only. No application version selects this until the complete
    // public world contract and its client follower also carry healing.
    static final int PLAYER_HEALING_SCHEMA=5;
    static byte[] encode(ModernEquipmentHistory.State state) {
        return encode(state,SCHEMA);
    }
    static byte[] encode(ModernEquipmentHistory.State state,int schema) {
        if(state==null||(state.healing()!=null)!=(schema==PLAYER_HEALING_SCHEMA)||schema<SLOW_SCHEMA&&state.spells().containsKey(ModernUseHistory.Lane.SLOW)||schema<SCHEMA||schema>PLAYER_HEALING_SCHEMA
                ||schema<HASTE_SCHEMA&&state.spells().containsKey(ModernUseHistory.Lane.HASTE)
                ||schema==SCHEMA&&state.spells().containsKey(ModernUseHistory.Lane.GREATER_HASTE))throw bad();
        try {
        var out=ByteBuffer.allocate(MAX_BYTES);
        out.putInt(MAGIC).putShort((short)schema).putShort((short)0).putInt(0);
        putIdentity(out,state.identity());out.putLong(state.worldRevision());putResources(out,state.resources());
        out.put((byte)state.spells().size()).put((byte)(state.items()==null?0:1)).putShort((short)0);
        for(var lane:ModernUseHistory.Lane.values()) {
            var spell=state.spells().get(lane);if(spell==null)continue;
            out.put((byte)lane.ordinal());zero(out,3);putLedger(out,spell.current());
            var history=state.targets().get(lane);
            int retained=history==null||history.structure()==null?0:history.structure().equals(history.last())?1:2;
            out.put((byte)(spell.last()==null?0:1)).put((byte)retained).putShort((short)0);
            if(spell.last()!=null){putCast(out,spell.last());putEffects(out,history.last().targets());}
            if(retained==2){putCast(out,history.structure().use());putEffects(out,history.structure().targets());}
        }
        if(state.items()!=null)putItems(out,state.items());
        if(schema==PLAYER_HEALING_SCHEMA)putHealing(out,state.healing());
        int size=out.position();out.putInt(8,size);return Arrays.copyOf(out.array(),size);
        }catch(java.nio.BufferOverflowException tooLarge){throw bad();}
    }
    static ModernEquipmentHistory.State decode(byte[] bytes) {
        return decode(bytes,SCHEMA);
    }
    static ModernEquipmentHistory.State decode(byte[] bytes,int schema) {
        if(schema<SCHEMA||schema>PLAYER_HEALING_SCHEMA)throw bad();
        if(bytes==null||bytes.length<HEADER_BYTES||bytes.length>MAX_BYTES)throw bad();
        try {
            var in=ByteBuffer.wrap(bytes);
            if(in.getInt()!=MAGIC||Short.toUnsignedInt(in.getShort())!=schema||in.getShort()!=0||in.getInt()!=bytes.length)throw bad();
            var identity=getIdentity(in);long revision=in.getLong();var resources=getResources(in);
            int maximum=schema==SCHEMA?8:schema==GREATER_HASTE_SCHEMA?9:schema==HASTE_SCHEMA?10:11;
            int count=u8(in),items=u8(in);zeros(in,2);if(count>maximum||items>1)throw bad();
            var spells=new EnumMap<ModernUseHistory.Lane,ModernUseHistory.Spell>(ModernUseHistory.Lane.class);
            var targets=new EnumMap<ModernUseHistory.Lane,ModernTargetHistory.History>(ModernUseHistory.Lane.class);
            int previous=-1;
            for(int i=0;i<count;i++) {
                int index=u8(in);zeros(in,3);if(index<=previous||index>=maximum)throw bad();previous=index;
                var lane=ModernUseHistory.Lane.values()[index];var ledger=getLedger(in);
                int used=u8(in),retained=u8(in);zeros(in,2);if(used>1||retained>2||used==0&&retained!=0)throw bad();
                ModernUseHistory.Cast cast=null;
                if(used==1) {
                    cast=getCast(in);var last=batch(identity,lane,cast,getEffects(in,lane));
                    var structure=retained==1?last:retained==2?batch(identity,lane,getCast(in),getEffects(in,lane)):null;
                    if(retained==2&&last.hasStructure())throw bad();
                    targets.put(lane,new ModernTargetHistory.History(last,structure));
                }
                spells.put(lane,new ModernUseHistory.Spell(ledger,cast));
            }
            var supplies=items==1?getItems(in):null;
            var healing=schema==PLAYER_HEALING_SCHEMA?getHealing(in,identity):null;if(in.hasRemaining())throw bad();
            return new ModernEquipmentHistory.State(identity,revision,resources,spells,supplies,targets,healing);
        }catch(BufferUnderflowException|ArithmeticException|IllegalArgumentException rejected){throw bad();}
    }
    static ModernEquipmentHistory.State decodeBound(byte[] bytes,ModernEquipmentHistory.Identity expected,long revision) {
        return decodeBound(bytes,expected,revision,SCHEMA);
    }
    static ModernEquipmentHistory.State decodeBound(byte[] bytes,ModernEquipmentHistory.Identity expected,long revision,int schema) {
        var state=decode(bytes,schema);
        if(!state.identity().equals(expected)||state.worldRevision()!=revision)throw bad();
        return state;
    }
    /** Mandatory schema5 tail, even when both counts and amounts are zero.
     * Incoming impact has no foreign epoch. The outgoing receipt's scope is
     * exactly the containing owner identity, not a second untrusted scope. */
    private static void putHealing(ByteBuffer b,ModernPlayerHealingHistory.Entry h) {
        var i=h.incoming();putWorld(b,i.world());putParticipant(b,i.recipient());
        b.putLong(i.received()).putLong(i.restored());putPresence(b,i.last()!=null);
        if(i.last()!=null)putHealingImpact(b,i.last());
        var o=h.outgoing();putPresence(b,o!=null);
        if(o!=null){putLedger(b,o.spending().current());putPresence(b,o.last()!=null);if(o.last()!=null)putHealingImpact(b,o.last().impact());}
    }
    private static ModernPlayerHealingHistory.Entry getHealing(ByteBuffer b,ModernEquipmentHistory.Identity owner) {
        var world=getWorld(b);var recipient=getParticipant(b);long received=b.getLong(),restored=b.getLong();
        var incoming=new ModernPlayerHealingHistory.Incoming(world,recipient,received,restored,getPresence(b)?getHealingImpact(b):null);
        ModernPlayerHealingHistory.Outgoing outgoing=null;
        if(getPresence(b)) {
            var ledger=getLedger(b);var impact=getPresence(b)?getHealingImpact(b):null;
            var receipt=impact==null?null:new ModernPlayerHealingTransaction.Receipt(impact.world(),owner.source(),impact.caster(),impact.use(),
                    impact.targetEffectRevision(),impact.effects(),impact.baseAmount(),impact.adjustedAmount(),impact.target());
            outgoing=new ModernPlayerHealingHistory.Outgoing(new ModernUseHistory.Spell(ledger,impact==null?null:impact.use()),receipt);
        }
        return new ModernPlayerHealingHistory.Entry(incoming,outgoing);
    }
    private static void putHealingImpact(ByteBuffer b,ModernPlayerHealingHistory.Impact h) {
        putWorld(b,h.world());putParticipant(b,h.caster());putCast(b,h.use());b.putLong(h.targetEffectRevision());
        var e=h.effects();b.put((byte)((e.waterLife()?1:0)|(e.polluteWater()?2:0)|(e.dirtyCurrent()?4:0)|(e.healingErosion()?8:0)));zero(b,3);
        b.putLong(h.baseAmount()).putLong(h.adjustedAmount());putEffects(b,List.of(h.target()));
    }
    private static ModernPlayerHealingHistory.Impact getHealingImpact(ByteBuffer b) {
        var world=getWorld(b);var caster=getParticipant(b);var use=getCast(b);long effectRevision=b.getLong();
        int flags=u8(b);zeros(b,3);if(flags>15)throw bad();
        var effects=new ModernPlayerHealing.Effects((flags&1)!=0,(flags&2)!=0,(flags&4)!=0,(flags&8)!=0);
        long base=b.getLong(),adjusted=b.getLong();var targets=getEffects(b,ModernUseHistory.Lane.HEALING);if(targets.size()!=1)throw bad();
        return new ModernPlayerHealingHistory.Impact(world,caster,use,effectRevision,effects,base,adjusted,targets.get(0));
    }
    private static void putPresence(ByteBuffer b,boolean present){b.put((byte)(present?1:0));zero(b,3);}
    private static boolean getPresence(ByteBuffer b){int p=u8(b);zeros(b,3);if(p>1)throw bad();return p==1;}
    private static ModernTargetHistory.Batch batch(ModernEquipmentHistory.Identity id,ModernUseHistory.Lane lane,
            ModernUseHistory.Cast cast,List<ModernTargetHistory.Effect> targets) {
        return new ModernTargetHistory.Batch(id.world(),id.source(),id.actor(),lane,cast,targets);
    }
    private static void putWorld(ByteBuffer b,ModernTargetHistory.World w){b.putLong(w.roomId()).putLong(w.matchId()).putLong(w.generation());}
    private static ModernTargetHistory.World getWorld(ByteBuffer b){return new ModernTargetHistory.World(b.getLong(),b.getLong(),b.getLong());}
    private static void putScope(ByteBuffer b,ModernNpcRoster.Scope s){b.putLong(s.roomId()).putLong(s.matchId()).putLong(s.generation());}
    private static ModernNpcRoster.Scope getScope(ByteBuffer b){return new ModernNpcRoster.Scope(b.getLong(),b.getLong(),b.getLong());}
    private static void putParticipant(ByteBuffer b,ModernTargetHistory.Participant p) {
        b.putLong(p.entityId()).put((byte)p.identity().kind().ordinal());zero(b,3);b.putInt(p.identity().sourceId()).putLong(p.identity().id());
    }
    private static ModernTargetHistory.Participant getParticipant(ByteBuffer b) {
        long entity=b.getLong();int kind=u8(b);zeros(b,3);if(kind>1)throw bad();
        return new ModernTargetHistory.Participant(entity,new ModernParticipantId(ModernParticipantId.Kind.values()[kind],b.getInt(),b.getLong()));
    }
    private static void putIdentity(ByteBuffer b,ModernEquipmentHistory.Identity id) {
        putWorld(b,id.world());var s=id.source();b.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch()).put((byte)s.channelId());zero(b,3);putParticipant(b,id.actor());
    }
    private static ModernEquipmentHistory.Identity getIdentity(ByteBuffer b) {
        var world=getWorld(b);var source=new ModernRooms.Scope(b.getInt(),b.getLong(),b.getLong(),u8(b));zeros(b,3);
        return new ModernEquipmentHistory.Identity(world,source,getParticipant(b));
    }
    private static void putVitals(ByteBuffer b,ModernEquipmentVitals.Vitals v){b.putLong(v.hp()).putLong(v.maximumHp()).putLong(v.mp()).putLong(v.maximumMp());}
    private static ModernEquipmentVitals.Vitals getVitals(ByteBuffer b){return new ModernEquipmentVitals.Vitals(b.getLong(),b.getLong(),b.getLong(),b.getLong());}
    private static void putResources(ByteBuffer b,ModernVitalBalance.State r) {
        b.putLong(r.revision()).putLong(r.equipmentRevision()).putLong(r.life()).putLong(r.birthHp()).putLong(r.birthMp());putVitals(b,r.vitals());
        var f=r.flows();b.putLong(f.hpGain()).putLong(f.hpLoss()).putLong(f.restored()).putLong(f.damaged()).putLong(f.hpGranted())
                .putLong(f.mpGain()).putLong(f.mpLoss()).putLong(f.spent()).putLong(f.mpGranted()).putLong(f.hpClipped()).putLong(f.mpClipped());
    }
    private static ModernVitalBalance.State getResources(ByteBuffer b) {
        long revision=b.getLong(),gear=b.getLong(),life=b.getLong(),hp=b.getLong(),mp=b.getLong();var v=getVitals(b);
        return new ModernVitalBalance.State(revision,gear,life,hp,mp,v,new ModernVitalBalance.Flows(b.getLong(),b.getLong(),b.getLong(),b.getLong(),b.getLong(),
                b.getLong(),b.getLong(),b.getLong(),b.getLong(),b.getLong(),b.getLong()));
    }
    private static void putLedger(ByteBuffer b,ModernSkillSpending.Ledger l) {
        b.putLong(l.equipmentRevision()).putLong(l.profileRevision()).putInt(l.cost()).putInt(0).putLong(l.priorCasts()).putLong(l.priorSpent())
                .putLong(l.casts()).putLong(l.spent()).putLong(l.lastCastRevision()).putLong(l.lastCastSequence());
    }
    private static ModernSkillSpending.Ledger getLedger(ByteBuffer b) {
        long gear=b.getLong(),revision=b.getLong();int cost=b.getInt();zeros(b,4);
        return new ModernSkillSpending.Ledger(gear,revision,cost,b.getLong(),b.getLong(),b.getLong(),b.getLong(),b.getLong(),b.getLong());
    }
    private static void putAnchor(ByteBuffer b,ModernUseHistory.Anchor a){b.putLong(a.equipmentRevision()).putLong(a.life());putVitals(b,a.vitals());b.putInt(a.x()).putInt(a.y());}
    private static ModernUseHistory.Anchor getAnchor(ByteBuffer b){return new ModernUseHistory.Anchor(b.getLong(),b.getLong(),getVitals(b),b.getInt(),b.getInt());}
    private static void putCast(ByteBuffer b,ModernUseHistory.Cast c){b.putLong(c.revision()).putLong(c.sequence());putAnchor(b,c.before());b.putInt(c.cost()).putInt(0).putLong(c.mpAfter());}
    private static ModernUseHistory.Cast getCast(ByteBuffer b) {
        long revision=b.getLong(),sequence=b.getLong();var a=getAnchor(b);int cost=b.getInt();zeros(b,4);return new ModernUseHistory.Cast(revision,sequence,a,cost,b.getLong());
    }
    private static void putItems(ByteBuffer b,ModernUseHistory.Items i) {
        b.putLong(i.equipmentRevision()).putLong(i.profileRevision()).putLong(i.fixedRecovery()).putLong(i.uses()).putLong(i.restored()).put((byte)(i.last()==null?0:1));zero(b,3);
        if(i.last()!=null){var u=i.last();b.putLong(u.revision()).putLong(u.sequence()).putLong(u.itemId());putAnchor(b,u.before());
            b.putLong(u.fixedRecovery()).putLong(u.rolledBase()).putLong(u.restored()).putLong(u.hpAfter());}
    }
    private static ModernUseHistory.Items getItems(ByteBuffer b) {
        long gear=b.getLong(),revision=b.getLong(),fixed=b.getLong(),uses=b.getLong(),restored=b.getLong();int present=u8(b);zeros(b,3);if(present>1)throw bad();
        var last=present==0?null:new ModernUseHistory.Use(b.getLong(),b.getLong(),b.getLong(),getAnchor(b),b.getLong(),b.getLong(),b.getLong(),b.getLong());
        return new ModernUseHistory.Items(gear,revision,fixed,uses,restored,last);
    }
    private static int maximum(ModernUseHistory.Lane lane){return switch(lane){case TELEPORT,GREATER_HASTE,HASTE->0;case FIRE_STORM->101;case NPC_FIRE_STORM->48;default->1;};}
    private static void putEffects(ByteBuffer b,List<ModernTargetHistory.Effect> effects) {
        b.putShort((short)effects.size()).putShort((short)0);
        for(var e:effects) {
            var h=e.before();var t=h.target();int tag=t instanceof ModernTargetHistory.Participant?0:t instanceof ModernTargetHistory.Npc?1:
                    t instanceof ModernTargetHistory.Honor?2:t instanceof ModernTargetHistory.Quest?3:4;
            b.put((byte)tag).put((byte)e.kind().ordinal());zero(b,6);int start=b.position();
            if(t instanceof ModernTargetHistory.Participant p)putParticipant(b,p);
            else if(t instanceof ModernTargetHistory.Npc n){putScope(b,n.id().scope());b.putLong(n.id().serial());}
            else if(t instanceof ModernTargetHistory.Honor o){b.putLong(o.entityId()).put((byte)o.team());}
            else if(t instanceof ModernTargetHistory.Quest q){putScope(b,q.id().scope());b.putInt(q.id().slot()).putInt(0).putLong(q.bornRevision());}
            else putScope(b,((ModernTargetHistory.TimeTower)t).id().scope());
            zero(b,40-(b.position()-start));b.putLong(h.life()).putLong(h.equipmentRevision()).putInt(h.cell().x()).putInt(h.cell().y())
                    .putLong(h.hp()).putLong(h.maximumHp()).putLong(e.raw()).putLong(e.applied()).putLong(e.hpAfter());
        }
    }
    private static List<ModernTargetHistory.Effect> getEffects(ByteBuffer b,ModernUseHistory.Lane lane) {
        int count=Short.toUnsignedInt(b.getShort());zeros(b,2);
        if(count>maximum(lane)||b.remaining()<count*EFFECT_BYTES)throw bad();
        var effects=new ArrayList<ModernTargetHistory.Effect>(count);
        for(int i=0;i<count;i++) {
            int tag=u8(b),kind=u8(b);zeros(b,6);if(tag>4||kind>1)throw bad();int start=b.position();
            ModernTargetHistory.Target target=switch(tag) {
                case 0->getParticipant(b);
                case 1->new ModernTargetHistory.Npc(new ModernNpcRoster.Id(getScope(b),b.getLong()));
                case 2->new ModernTargetHistory.Honor(b.getLong(),u8(b));
                case 3->{var scope=getScope(b);int slot=b.getInt();zeros(b,4);yield new ModernTargetHistory.Quest(new ModernQuestObjectives.Id(scope,slot),b.getLong());}
                case 4->new ModernTargetHistory.TimeTower(new ModernTimeAttack.TowerId(getScope(b)));
                default->throw bad();
            };
            zeros(b,40-(b.position()-start));var health=new ModernTargetHistory.Health(target,b.getLong(),b.getLong(),new ModernWorldMap.Cell(b.getInt(),b.getInt()),b.getLong(),b.getLong());
            effects.add(new ModernTargetHistory.Effect(ModernTargetHistory.Kind.values()[kind],health,b.getLong(),b.getLong(),b.getLong()));
        }
        return List.copyOf(effects);
    }
    private static int u8(ByteBuffer b){return Byte.toUnsignedInt(b.get());}
    private static void zeros(ByteBuffer b,int count){for(int i=0;i<count;i++)if(b.get()!=0)throw bad();}
    private static void zero(ByteBuffer b,int count){for(int i=0;i<count;i++)b.put((byte)0);}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid complete equipment history section");}
    private ModernEquipmentHistoryWire(){ }
}
