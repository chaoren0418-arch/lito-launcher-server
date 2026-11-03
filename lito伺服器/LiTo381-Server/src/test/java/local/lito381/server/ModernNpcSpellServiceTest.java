package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernNpcSpellServiceTest {
    @TempDir Path temporary;
    static ModernNpcServiceTest.Fixture fixture()throws Exception{return new ModernNpcServiceTest.Fixture(100,false,false,null,554,true);}
    static ModernNpcSpellProtocol.Request request(ModernNpcServiceTest.Fixture f,ModernRoomsTest.Player p,ModernNpcProtocol.Frame frame,long sequence) {
        var npc=frame.actors().stream().filter(n->n.hp()>0).findFirst().orElseThrow();
        return new ModernNpcSpellProtocol.Request(p.scope(),frame.scope(),sequence,1,npc.serial(),npc.life());
    }
    static void wire(ModernNpcSpellProtocol.Request request,ModernNpcSpellProtocol.Reply reply)throws Exception {
        var m=new ModernApplicationProtocol.NpcSpellResult(42,request,reply);
        ModernNpcServiceTest.wireVersions(m,24);
    }
    @Test void serviceDispatchConsumesManaOnceAndLeavesTypedNpcAndTeleportStateIndependent()throws Exception {
        // The real damage die can kill this 280-HP NPC. Exercise both ends
        // deterministically; a dead target correctly precedes cooldown denial.
        for(boolean lethal:List.of(false,true))try(var f=new ModernNpcServiceTest.Fixture(100,false,false,null,554,true,bound->lethal?bound-1:0)){
            var initial=f.active();var r=request(f,f.a,initial,1);var hit=f.service.castNpc(f.a.owner(),r);wire(r,hit);
            assertEquals(ModernNpcProtocol.Status.OK,hit.status());var c=hit.frame().combat();
            assertEquals(1,initial.combat().npcSpells().get(0).mpCost()); // Original INT reduction, not the SQL base cost of 3.
            assertEquals(initial.combat().vitals().get(0).mp()-1,c.vitals().get(0).mp());assertEquals(1,c.npcSpells().get(0).castCount());
            assertEquals(0,c.world().casts().get(0).mpSpent());assertTrue(c.damages().isEmpty());
            assertEquals(lethal,c.npcSpells().get(0).outcome().death());
            var fast=new ModernNpcSpellProtocol.Request(r.player(),r.world(),2,1,r.serial(),r.targetLife());var denied=f.service.castNpc(f.a.owner(),fast);wire(fast,denied);
            assertEquals(lethal?ModernNpcMagic.Reason.TARGET_DEAD:ModernNpcMagic.Reason.TOO_FAST,denied.reason());assertEquals(c.npcSpells(),denied.frame().combat().npcSpells());
            assertThrows(SecurityException.class,()->f.service.castNpc(f.a.owner(),fast));
            var tp=f.service.cast(f.a.owner(),new ModernSpellProtocol.Request(f.a.scope(),r.world().matchId(),r.world().generation(),3,5));
            assertEquals(ModernSpellProtocol.Reason.TOO_FAST,tp.reason());assertEquals(c.npcSpells(),tp.frame().npcSpells());
        }
    }
    @Test void sourceOwnerEpochChannelMatchAndBothLivesCannotCrossOrSpendMana()throws Exception {
        try(var f=fixture()){
            var initial=f.active();var r=request(f,f.a,initial,1);
            assertThrows(SecurityException.class,()->f.service.castNpc(f.b.owner(),r));
            for(var bad:List.of(
                    new ModernNpcSpellProtocol.Request(new ModernRooms.Scope(7,3333,2,0),r.world(),1,1,r.serial(),1),
                    new ModernNpcSpellProtocol.Request(new ModernRooms.Scope(7,3333,1,1),r.world(),1,1,r.serial(),1),
                    new ModernNpcSpellProtocol.Request(r.player(),new ModernNpcRoster.Scope(r.world().roomId(),r.world().matchId(),r.world().generation()+1),1,1,r.serial(),1),
                    new ModernNpcSpellProtocol.Request(r.player(),r.world(),1,2,r.serial(),1),
                    new ModernNpcSpellProtocol.Request(r.player(),r.world(),1,1,r.serial(),2))){
                assertThrows(SecurityException.class,()->f.service.castNpc(f.a.owner(),bad));
            }
            var next=f.read(f.a,initial.combat().world()).frame().combat();assertEquals(initial.combat().npcSpells(),next.npcSpells());
            assertEquals(0,next.world().casts().get(0).requestSequence());assertEquals(initial.combat().vitals().get(0).mp(),next.vitals().get(0).mp());
        }
    }
    @Test void freshUnlearnedSkillAndUnnegotiatedPeerHaveDifferentSafeOutcomes()throws Exception {
        try(var f=fixture()){f.learned=false;var initial=f.active();var r=request(f,f.a,initial,1);var denied=f.service.castNpc(f.a.owner(),r);wire(r,denied);
            assertEquals(ModernNpcMagic.Reason.UNLEARNED,denied.reason());assertEquals(initial.combat().npcSpells(),denied.frame().combat().npcSpells());}
        for(int version:List.of(23,24))try(var f=fixture()){
            f.register(f.b,version,version==24?ModernSessionProtocol.RUNTIME_CAPABILITIES:(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_NPC_FIRE_STORM)),null);
            assertEquals(ModernWorldProtocol.Status.UNAVAILABLE,f.prepare().status());assertEquals(0,f.physicalReads.get());
        }
    }
    @Test void fourMagicWavesThenGuardianSettlementKeepBothForceLedgersAndNormalLeave()throws Exception {
        var journal=new ModernQuestServiceTest.Journal();
        try(var f=new ModernNpcServiceTest.Fixture(100,true,true,journal,554,true)){
            var initial=f.active();var w=initial.combat().world();long sequence=0;
            for(int stage=1;stage<=4;stage++){
                int wanted=stage;ModernNpcServiceTest.await(()->f.read(f.a,w).frame().actors().stream().anyMatch(n->n.hp()>0&&n.stage()==wanted));
                var r=request(f,f.a,f.read(f.a,w).frame(),++sequence);var hit=f.service.castNpc(f.a.owner(),r);wire(r,hit);
                assertEquals(ModernNpcProtocol.Status.OK,hit.status());assertTrue(hit.frame().combat().npcSpells().get(0).outcome().death());
                assertEquals(stage,hit.frame().combat().npcSpells().get(0).castCount());Thread.sleep(15);
            }
            var before=f.read(f.a,w).frame();var target=before.combat().world().quest().actors().stream().filter(t->t.hp()>0).findFirst().orElseThrow();
            var strike=new ModernNpcProtocol.Request(f.a.scope(),ModernNpcProtocol.Action.ATTACK_OBJECTIVE,before.scope(),1,target.slot(),0);
            assertEquals(ModernNpcProtocol.Status.OK,f.service.npc(f.a.owner(),strike).status());
            var winner=f.read(f.a,w).frame();var loser=f.read(f.b,w).frame();
            assertEquals(ModernWorldProtocol.SettlementState.SAVED,winner.combat().world().completion().state());
            assertEquals(4,winner.combat().npcSpells().get(0).castCount());assertEquals(4,winner.combat().npcSpells().get(0).mpSpent());
            assertEquals(2,loser.combat().npcSpells().get(0).entityId());assertEquals(0,loser.combat().npcSpells().get(0).castCount());assertEquals(1,journal.records.get());
            var inactive=new ModernNpcSpellProtocol.Request(f.b.scope(),loser.scope(),1,1,1,1);var denied=f.service.castNpc(f.b.owner(),inactive);wire(inactive,denied);assertEquals(ModernNpcMagic.Reason.NOT_ACTIVE,denied.reason());
            for(var p:List.of(f.a,f.b))assertNull(f.service.handle(p.owner(),new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.LEAVE,0,0,0,w.matchId(),w.generation(),0,0,0,ModernNpcServiceTest.ZERO)).frame());
        }
    }
    @Test void leaveAfterMagicKeepsOtherPlayersLedgerAndClosedNpcSnapshot()throws Exception {
        try(var f=fixture()){
            var initial=f.active();var r=request(f,f.a,initial,1);var hit=f.service.castNpc(f.a.owner(),r);var w=hit.frame().combat().world();
            f.service.handle(f.a.owner(),new ModernWorldProtocol.Request(f.a.scope(),ModernWorldProtocol.Action.LEAVE,0,0,0,w.matchId(),w.generation(),0,0,0,ModernNpcServiceTest.ZERO));
            var after=f.read(f.b,w).frame();assertTrue(after.closed());assertEquals(hit.frame().combat().npcSpells(),after.combat().npcSpells());
            var read=new ModernApplicationProtocol.NpcResult(1,f.request(f.b,w,0,0,0),new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,after));
            ModernNpcServiceTest.wireVersions(read,24);
            assertThrows(SecurityException.class,()->f.service.castNpc(f.a.owner(),r));
        }
    }
    @Test void explicitNpcRulesEnableOnlyNpcMagicQualificationAndSurviveCampOverrides()throws Exception {
        var p=ModernNpcConfigurationTest.world(temporary);var prefix=ModernNpcConfigurationTest.PREFIX;
        p.setProperty(prefix+"npcMagicRules",ModernNpcMagic.RULESET);
        assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(p));
        p.setProperty(prefix+"spellRules",ModernWorldConfiguration.SPELL_RULESET);p.setProperty(prefix+"castMillis","1200");
        try(var c=ModernNpcConfigurationTest.config(temporary,p)){
            assertTrue(c.worlds().npcMagicEnabled());assertTrue(c.worlds().magicQualificationEnabled());assertFalse(c.worlds().damageMagicEnabled());
            var encounters=ModernNpcConfigurationTest.encounter(c.worlds());var m=c.worlds().load(null,()->false,encounters).get(ModernNpcConfigurationTest.KEY);
            assertTrue(m.npc().lightArrow());var flags=ModernMatchDescriptor.of(m,new ModernWorldService.Limits(2,1,1,2000,50,new ModernBattleEngine.Limits(2000,0,60000)),false);
            assertTrue(flags.has(ModernMatchDescriptor.NPC_LIGHT_ARROW));assertFalse(flags.has(ModernMatchDescriptor.LIGHT_ARROW));
            assertFalse(flags.supportedBy(23,(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_NPC_FIRE_STORM))));
            var camps=List.of(new ModernWorldMap.Cell(32778,32780),new ModernWorldMap.Cell(32788,32780));
            assertTrue(c.worlds().load(ModernNpcConfigurationTest.policy(m,camps),()->false,encounters).get(m.binding().key()).npc().lightArrow());
            assertThrows(java.io.IOException.class,()->c.worlds().load(null,()->false,Map.of(m.binding().key(),m.npc().withLightArrow(false))));
        }
        for(String bad:List.of("true","AUTO",ModernWorldConfiguration.DAMAGE_SPELL_RULESET)){p.setProperty(prefix+"npcMagicRules",bad);assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(p));}
    }
    @Test void questOutboxMagicPolicyCannotReuseTeleportOnlyNamespace()throws Exception {
        var p=ModernQuestConfigurationTest.world(temporary);var prefix=ModernNpcConfigurationTest.PREFIX;
        p.setProperty(prefix+"spellRules",ModernWorldConfiguration.SPELL_RULESET);p.setProperty(prefix+"castMillis","1200");
        byte[] old;try(var c=ModernQuestConfigurationTest.config(temporary,p)){old=ModernSettlementCoordinator.binding(c);}
        p.setProperty(prefix+"npcMagicRules",ModernNpcMagic.RULESET);
        try(var c=ModernQuestConfigurationTest.config(temporary,p)){assertFalse(java.util.Arrays.equals(old,ModernSettlementCoordinator.binding(c)));}
        p.remove(prefix+"npcMagicRules");try(var c=ModernQuestConfigurationTest.config(temporary,p)){assertArrayEquals(old,ModernSettlementCoordinator.binding(c));}
    }
}
