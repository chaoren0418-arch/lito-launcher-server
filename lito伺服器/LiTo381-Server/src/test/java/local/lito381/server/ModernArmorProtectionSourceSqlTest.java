package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Newly owned databases only; exercises the production scoped provider. */
class ModernArmorProtectionSourceSqlTest {
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void providerReadsOwnerEquipmentAndCatalogOnOneConnectionWithoutLearnedSkillOrRuntimeGrant()throws Exception {
        var home=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(home!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var fixture=ModernQuestSqlFixture.open(Path.of(home),Path.of(shared),tls)) {
            ModernArmorProcsSqlTest.seed(fixture);seedSkill(fixture);
            var original=ModernArmorProcsSqlTest.snapshot(fixture);
            var bundles=new ArrayList<ModernArmorProtectionCatalog.BoundSource>();
            try(var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
                try {
                    for(var source:fixture.config.sources()) {
                        var connections=new AtomicInteger();
                        var dao=new ModernJdbcBackend(source.database(),1000,5000,(url,properties)->{
                            connections.incrementAndGet();return new com.mysql.cj.jdbc.Driver().connect(url,properties);
                        });
                        var account=gate.call(5000,scope->dao.authenticate(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD));
                        assertTrue(account.accepted());int before=connections.get();
                        var bundle=gate.call(5000,scope->dao.armorProtectionCharacter(source.id(),account.account(),3333,
                                source.catalogDatabase(),false,false,false,false,false,false).orElseThrow());
                        assertEquals(before+1,connections.get(),"character, gear and skill share one owned connection");
                        assertEquals(ModernArmorProtectionCatalog.Status.QUALIFIED,bundle.skill().status());
                        var fighter=fighter(bundle);
                        assertEquals(ModernPhysicalCombat.UnsupportedReason.EQUIPMENT_PROC,fighter.unsupportedReason());
                        var actor=actor(source.id());var selection=bundle.select(fighter.profile(),actor);
                        assertEquals(source.id()==7?3:6,selection.rules().get(0).rule().seconds());
                        assertEquals(11101,selection.rules().get(0).rule().graphics(),"no resource substitution");
                        bundles.add(bundle);
                    }
                    var first=bundles.get(0);var other=bundles.get(1);
                    assertThrows(IllegalArgumentException.class,()->other.select(fighter(first).profile(),actor(7)));
                    var source=fixture.config.sources().stream().filter(s->s.id()==7).findFirst().orElseThrow();
                    var dao=new ModernJdbcBackend(source.database(),1000,5000);
                    // A later source revision is read again; old immutable evidence
                    // stays unchanged and cannot authorize the different catalog.
                    try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5))) {
                        execute(c,"UPDATE `"+ModernQuestSqlFixture.CATALOG+"`.skills SET castgfx=228+4294967296 WHERE skill_id=68");
                    }
                    var changed=gate.call(5000,scope->dao.armorProtectionCharacter(7,ModernQuestSqlFixture.ACCOUNT,3333,
                            source.catalogDatabase(),false,false,false,false,false,false).orElseThrow());
                    assertEquals(ModernArmorProtectionCatalog.Status.UNSUPPORTED_TEMPLATE,changed.skill().status());
                    assertThrows(IllegalArgumentException.class,()->changed.select(fighter(changed).profile(),actor(7)));
                    assertEquals(ModernArmorProtectionCatalog.Status.QUALIFIED,first.skill().status());
                    try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5))) {
                        execute(c,"UPDATE `"+ModernQuestSqlFixture.CATALOG+"`.skills SET castgfx=228 WHERE skill_id=68");
                        execute(c,"DROP TABLE `"+ModernQuestSqlFixture.CATALOG+"`.skills_buff_ability");
                    }
                    assertTrue(gate.call(5000,scope->dao.armorProtectionCharacter(7,"not-owner",3333,
                            source.catalogDatabase(),false,false,false,false,false,false)).isEmpty(),"owner rejection precedes unavailable catalog");
                    assertThrows(Exception.class,()->gate.call(5000,scope->dao.armorProtectionCharacter(7,ModernQuestSqlFixture.ACCOUNT,3333,
                            source.catalogDatabase(),false,false,false,false,false,false)));
                    var ordinary=gate.call(5000,scope->dao.physicalCharacter(7,ModernQuestSqlFixture.ACCOUNT,3333,source.catalogDatabase()).orElseThrow());
                    assertEquals(ModernPhysicalCombat.UnsupportedReason.EQUIPMENT_PROC,
                            ModernPhysicalCombat.qualify(ordinary,ModernCombatProfileTest.binding(ordinary.input().equipment()),Optional.empty(),System.currentTimeMillis()).unsupportedReason(),
                            "ordinary source read does not gain a new catalog dependency or combat grant");
                }finally{executor.close();assertTrue(executor.awaitTermination(5000));}
            }
            assertEquals(original,ModernArmorProcsSqlTest.snapshot(fixture));
            assertEquals(0,fixture.mysql.activeConnections());
            System.err.println("ARMOR_PROTECTION_SOURCE_SQL_OK sources=2 sameOwnedConnection=true learnedTablesAbsent=true wideTemplateRejected=true ownerBeforeCatalog=true ordinaryRoutePreserved=true gearRowsUnchanged=true runtimeGrant=false");
            fixture.successful();
        }
    }
    private static ModernPhysicalCombat.Fighter fighter(ModernArmorProtectionCatalog.BoundSource bundle) {
        return ModernPhysicalCombat.qualify(bundle.physical(),ModernCombatProfileTest.binding(bundle.physical().input().equipment()),Optional.empty(),System.currentTimeMillis());
    }
    private static ModernArmorProcProtection.Actor actor(int source) {
        return new ModernArmorProcProtection.Actor(new UUID(0,329),new TournamentPresence.CharacterKey(source,3333),1,0);
    }
    static void seedSkill(ModernQuestSqlFixture fixture)throws Exception {
        try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
            String p="`"+ModernQuestSqlFixture.CATALOG+"`.";
            execute(c,"ALTER TABLE "+p+"skills ADD id BIGINT NOT NULL DEFAULT 0,ADD sysmsgID_happen BIGINT NOT NULL DEFAULT 0,ADD sysmsgID_stop BIGINT NOT NULL DEFAULT 0,ADD sysmsgID_fail BIGINT NOT NULL DEFAULT 0");
            execute(c,"INSERT INTO "+p+"skills(skill_id,skill_level,skill_number,mpConsume,itemConsumeId,itemConsumeCount,reuseDelay,buffDuration,target,"
                    +"target_to,type,ranged,id,action_id,castgfx,sysmsgID_happen,sysmsgID_stop,sysmsgID_fail) VALUES(68,9,3,30,40318,2,1500,32,'buff',4,2,-1,8,19,228,314,315,280)");
            execute(c,"CREATE TABLE "+p+"skills_item(skill_id BIGINT,itemids VARCHAR(10),counts VARCHAR(10))");
            execute(c,"INSERT INTO "+p+"skills_item VALUES(68,NULL,NULL)");
            execute(c,"CREATE TABLE "+p+"skills_buff_ability(skill_id BIGINT)");
            execute(c,"CREATE TABLE "+p+"`skills_負面技能控制`(skill_id BIGINT)");
            for(int id:List.of(7,42))execute(c,"DROP TABLE `"+ModernQuestSqlFixture.source(id)+"`.character_skills");
        }
    }
    private static void execute(Connection connection,String sql)throws Exception {
        try(var s=connection.createStatement()){s.setQueryTimeout(3);s.execute(sql);}
    }
}
