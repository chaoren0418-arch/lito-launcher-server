package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernTargetHistory.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernTargetHistoryTest {
    static final World WORLD=new World(10,20,30);
    static final ModernNpcRoster.Scope NPC_WORLD=new ModernNpcRoster.Scope(10,20,30);
    static final ModernRooms.Scope SOURCE=new ModernRooms.Scope(7,3333,4,0);
    static final Participant CASTER=new Participant(1,ModernParticipantId.human(7,3333));
    static final ModernWorldMap.Cell CELL=new ModernWorldMap.Cell(32770,32771);
    static ModernUseHistory.Cast use(long revision,long sequence,long gear,long life) {
        return new ModernUseHistory.Cast(revision,sequence,ModernUseHistoryTest.anchor(gear,life,100,100,300),48,252);
    }
    static Health health(Target target,long life,long gear,long hp,long maximum) {return new Health(target,life,gear,CELL,hp,maximum);}
    static Effect hit(Target target) {return new Effect(Kind.DAMAGE,health(target,target instanceof Participant||target instanceof Npc?2:1,0,50,100),100,50,0);}
    static Batch batch(ModernUseHistory.Lane lane,long revision,long sequence,List<Effect> targets) {
        return new Batch(WORLD,SOURCE,CASTER,lane,use(revision,sequence,0,1),targets);
    }
    @Test void historicalCapacityAndLifeDoNotDependOnLaterMaxima() {
        var id=new Participant(2,ModernParticipantId.human(42,3333));
        var old=hit(id);var prior=History.first(batch(ModernUseHistory.Lane.LIGHT_ARROW,3,1,List.of(old)));
        var next=new Effect(Kind.DAMAGE,health(id,3,5,3,3),1,1,2);
        var after=prior.commit(batch(ModernUseHistory.Lane.LIGHT_ARROW,9,2,List.of(next)));
        assertEquals(100,prior.last().targets().get(0).before().maximumHp());assertEquals(2,old.before().life());
        assertEquals(3,after.last().targets().get(0).before().maximumHp());assertTrue(follows(prior,after));
    }
    @Test void rawDamageClipsAtActualHpAndHealingAtHistoricalCapacityWithoutOverflow() {
        var p=new Participant(2,ModernParticipantId.human(42,3333));
        var v=health(p,1,0,Long.MAX_VALUE,Long.MAX_VALUE);
        var dead=new Effect(Kind.DAMAGE,v,Long.MAX_VALUE,Long.MAX_VALUE,0);assertTrue(dead.death());
        var full=new Effect(Kind.HEAL,v,Long.MAX_VALUE,0,Long.MAX_VALUE);assertFalse(full.death());
        assertThrows(IllegalArgumentException.class,()->new Effect(Kind.DAMAGE,v,1,2,Long.MAX_VALUE-2));
        assertThrows(IllegalArgumentException.class,()->new Effect(Kind.HEAL,v,1,1,Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,()->health(p,1,0,0,100));
        assertThrows(IllegalArgumentException.class,()->health(p,1,0,101,100));
    }
    @Test void humanBotAndSameNumberedDifferentSourcesRetainSeparateNamespaces() {
        var targets=List.of(hit(new Participant(2,ModernParticipantId.human(42,3333))),hit(new Participant(3,ModernParticipantId.bot(7,3333))));
        var b=batch(ModernUseHistory.Lane.FIRE_STORM,3,1,targets);assertEquals(2,b.targets().size());
        assertNotEquals(((Participant)targets.get(0).before().target()).identity(),((Participant)targets.get(1).before().target()).identity());
        assertThrows(IllegalArgumentException.class,()->batch(ModernUseHistory.Lane.FIRE_STORM,3,1,List.of(hit(CASTER))));
        assertThrows(IllegalArgumentException.class,()->batch(ModernUseHistory.Lane.FIRE_STORM,3,1,List.of(hit(new Participant(2,CASTER.identity())))));
    }
    @Test void duplicateEntityIdentityOrObjectiveAliasIsRejected() {
        var p=new Participant(2,ModernParticipantId.human(42,3333));
        for(var other:List.<Target>of(p,new Participant(2,ModernParticipantId.bot(7,2)),new Participant(3,p.identity())))
            assertThrows(IllegalArgumentException.class,()->batch(ModernUseHistory.Lane.FIRE_STORM,3,1,List.of(hit(p),hit(other))));
        assertThrows(IllegalArgumentException.class,()->batch(ModernUseHistory.Lane.FIRE_STORM,3,1,
                List.of(hit(new Participant(3,ModernParticipantId.human(42,3))),hit(new Honor(3,1)))));
    }
    @Test void casterSourceAndWorldOwnershipCannotChangeInFollowingReceipt() {
        var initial=History.first(batch(ModernUseHistory.Lane.TELEPORT,3,1,List.of()));
        var altered=new Batch(new World(10,20,31),SOURCE,CASTER,ModernUseHistory.Lane.TELEPORT,use(4,2,0,1),List.of());
        assertThrows(IllegalArgumentException.class,()->initial.commit(altered));
        var epoch=new ModernRooms.Scope(7,3333,5,0);
        assertThrows(IllegalArgumentException.class,()->initial.commit(new Batch(WORLD,epoch,CASTER,ModernUseHistory.Lane.TELEPORT,use(4,2,0,1),List.of())));
        assertThrows(IllegalArgumentException.class,()->new Batch(WORLD,new ModernRooms.Scope(42,3333,4,0),CASTER,ModernUseHistory.Lane.TELEPORT,use(3,1,0,1),List.of()));
        assertThrows(IllegalArgumentException.class,()->new Batch(WORLD,SOURCE,new Participant(1,ModernParticipantId.bot(7,3333)),ModernUseHistory.Lane.TELEPORT,use(3,1,0,1),List.of()));
    }
    @Test void laneTargetKindsAndExactSingleTargetCardinalityAreEnforced() {
        var pc=hit(new Participant(2,ModernParticipantId.human(42,3333)));var npc=hit(new Npc(new ModernNpcRoster.Id(NPC_WORLD,1)));
        assertThrows(IllegalArgumentException.class,()->batch(ModernUseHistory.Lane.TELEPORT,3,1,List.of(pc)));
        assertThrows(IllegalArgumentException.class,()->batch(ModernUseHistory.Lane.LIGHT_ARROW,3,1,List.of()));
        assertThrows(IllegalArgumentException.class,()->batch(ModernUseHistory.Lane.DISINTEGRATE,3,1,List.of(npc)));
        assertThrows(IllegalArgumentException.class,()->batch(ModernUseHistory.Lane.NPC_LIGHT_ARROW,3,1,List.of(pc)));
        assertThrows(IllegalArgumentException.class,()->batch(ModernUseHistory.Lane.HEALING,3,1,List.of(hit(new Honor(3,0)))));
        assertTrue(batch(ModernUseHistory.Lane.FIRE_STORM,3,1,List.of()).targets().isEmpty());
    }
    @Test void npcAndStructureScopesAndBirthRevisionCannotBeForged() {
        var foreign=new ModernNpcRoster.Scope(10,21,30);
        assertThrows(IllegalArgumentException.class,()->batch(ModernUseHistory.Lane.NPC_FIRE_STORM,3,1,List.of(hit(new Npc(new ModernNpcRoster.Id(foreign,1))))));
        assertThrows(IllegalArgumentException.class,()->batch(ModernUseHistory.Lane.NPC_FIRE_STORM,3,1,List.of(hit(new Quest(new ModernQuestObjectives.Id(NPC_WORLD,1),3)))));
        assertThrows(IllegalArgumentException.class,()->batch(ModernUseHistory.Lane.NPC_FIRE_STORM,3,1,List.of(hit(new TimeTower(new ModernTimeAttack.TowerId(foreign))))));
        assertThrows(IllegalArgumentException.class,()->health(new Npc(new ModernNpcRoster.Id(NPC_WORLD,1)),1,1,1,1));
        assertThrows(IllegalArgumentException.class,()->health(new Honor(3,0),2,0,1,1));
    }
    @Test void oldStructureDestructionSurvivesNpcOnlyAndEmptyCastsAndCasterRespawn() {
        var structure=hit(new Quest(new ModernQuestObjectives.Id(NPC_WORLD,1),1));
        var first=History.first(batch(ModernUseHistory.Lane.NPC_FIRE_STORM,3,1,List.of(structure)));
        var next=new Batch(WORLD,SOURCE,CASTER,ModernUseHistory.Lane.NPC_FIRE_STORM,use(8,3,2,2),List.of(hit(new Npc(new ModernNpcRoster.Id(NPC_WORLD,1)))));
        var changed=first.commit(next);assertEquals(first.last(),changed.structure());assertTrue(follows(first,changed));
        var empty=new Batch(WORLD,SOURCE,CASTER,ModernUseHistory.Lane.NPC_FIRE_STORM,use(9,4,2,2),List.of());
        var later=changed.commit(empty);assertEquals(first.last(),later.structure());assertTrue(later.structure().targets().get(0).death());
        assertFalse(follows(changed,new History(empty,null)),"valid snapshot cannot erase retained destruction history");
    }
    @Test void replayRollbackAndValidButRewrittenSameRevisionAreRejected() {
        var p=new Participant(2,ModernParticipantId.human(42,3333));
        var first=History.first(batch(ModernUseHistory.Lane.DISINTEGRATE,3,1,List.of(hit(p))));
        var different=new Effect(Kind.DAMAGE,health(p,2,0,40,100),100,40,0);
        var forged=History.first(batch(ModernUseHistory.Lane.DISINTEGRATE,3,1,List.of(different)));
        assertFalse(follows(first,forged));assertTrue(follows(first,first));assertThrows(IllegalArgumentException.class,()->first.commit(first.last()));
        var next=first.commit(new Batch(WORLD,SOURCE,CASTER,ModernUseHistory.Lane.DISINTEGRATE,use(8,2,2,2),List.of(hit(p))));
        assertThrows(IllegalArgumentException.class,()->next.commit(new Batch(WORLD,SOURCE,CASTER,ModernUseHistory.Lane.DISINTEGRATE,use(9,3,1,2),List.of(hit(p)))));
        assertThrows(IllegalArgumentException.class,()->next.commit(new Batch(WORLD,SOURCE,CASTER,ModernUseHistory.Lane.DISINTEGRATE,use(9,3,2,1),List.of(hit(p)))));
    }
    @Test void aoeBoundAndImmutableCopyPreventUnboundedOrMutableHistory() {
        var many=new ArrayList<Effect>();for(int i=1;i<=48;i++)many.add(hit(new Npc(new ModernNpcRoster.Id(NPC_WORLD,i))));
        var valid=batch(ModernUseHistory.Lane.NPC_FIRE_STORM,3,1,many);many.clear();assertEquals(48,valid.targets().size());
        assertThrows(UnsupportedOperationException.class,()->valid.targets().clear());
        many.addAll(valid.targets());many.add(hit(new Npc(new ModernNpcRoster.Id(NPC_WORLD,49))));
        assertThrows(IllegalArgumentException.class,()->batch(ModernUseHistory.Lane.NPC_FIRE_STORM,3,1,many));
    }
    @Test void previouslyObservedTargetCannotRollBackLifeEquipmentOrRebindEntity() {
        var p=new Participant(3,ModernParticipantId.human(42,3333));
        var before=new Effect(Kind.DAMAGE,health(p,2,5,50,100),1,1,49);
        var first=History.first(batch(ModernUseHistory.Lane.FIRE_STORM,3,1,List.of(before)));
        for(var after:List.of(new Effect(Kind.DAMAGE,health(p,1,5,50,100),1,1,49),new Effect(Kind.DAMAGE,health(p,2,4,50,100),1,1,49),
                hit(new Participant(3,ModernParticipantId.bot(42,3333))),hit(new Participant(4,p.identity())),hit(new Honor(3,1))))
            assertThrows(IllegalArgumentException.class,()->first.commit(batch(ModernUseHistory.Lane.FIRE_STORM,4,2,List.of(after))));
    }
    @Test void fixedCrossLanguageCorpusIncludesNpcQuestTowerAndRetainedStructureHistory() throws Exception {
        var hash=java.security.MessageDigest.getInstance("SHA-256");History history=null;
        for(int i=0;i<512;i++) {
            long revision=i+3L,sequence=i+1L,life=1L+i/23,gear=i/7,hp=1L+i*17%1000,maximum=Long.MAX_VALUE-i,raw=i*29L%1200;
            long tag=1,id=i%3+1,targetLife=1+i/17;
            Target target=new Npc(new ModernNpcRoster.Id(NPC_WORLD,id));
            if(i%7==0) {targetLife=1;tag=i%2==0?2:3;id=tag==2?i%8+1:0;
                target=tag==2?new Quest(new ModernQuestObjectives.Id(NPC_WORLD,(int)id),1):new TimeTower(new ModernTimeAttack.TowerId(NPC_WORLD));}
            var hit=new Effect(Kind.DAMAGE,health(target,targetLife,0,hp,maximum),raw,Math.min(raw,hp),hp-Math.min(raw,hp));
            var batch=new Batch(WORLD,SOURCE,CASTER,ModernUseHistory.Lane.NPC_FIRE_STORM,use(revision,sequence,gear,life),List.of(hit));
            history=history==null?History.first(batch):history.commit(batch);
            var retained=history.structure();var prior=retained.targets().get(0).before();
            long[] values={10,20,30,revision,sequence,life,gear,tag,id,targetLife,hp,maximum,raw,hit.applied(),hit.hpAfter(),retained.use().revision(),prior.hp(),prior.maximumHp()};
            for(long n:values)hash.update(java.nio.ByteBuffer.allocate(8).putLong(n).array());
        }
        assertEquals("55bbc7b08be37db19a23431c87e45395707134d05b3e14f1fc913c4effb4c863",java.util.HexFormat.of().formatHex(hash.digest()));
    }
}
