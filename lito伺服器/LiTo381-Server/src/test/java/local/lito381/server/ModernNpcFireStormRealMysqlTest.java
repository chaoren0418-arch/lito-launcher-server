package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** SELECT-only imports from freshly owned SQL; malformed extensions fail closed. */
class ModernNpcFireStormRealMysqlTest {
    record Read(ModernSkills.Snapshot skills,ModernFireStorm.Profile profile) {}
    private static Read read(ModernBackendExecutor.SessionGate gate,ModernServerConfig.SourceConfig source)throws Exception {
        return gate.call(10000,scope->{
            var dao=new ModernJdbcBackend(source.database(),1000,5000);
            assertTrue(dao.authenticate(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD).accepted());
            var physical=dao.physicalCharacter(source.id(),ModernQuestSqlFixture.ACCOUNT,3333,source.catalogDatabase(),true,false,true,false).orElseThrow();
            var fighter=ModernPhysicalCombat.qualify(physical,ModernCombatProfileTest.binding(physical.input().equipment()),Optional.empty(),System.currentTimeMillis());
            return new Read(dao.skillsCharacter(source.id(),ModernQuestSqlFixture.ACCOUNT,3333,source.catalogDatabase(),true).orElseThrow(),ModernFireStorm.qualify(fighter));
        });
    }
    private static ModernSkills.Support support(Read r) {
        return r.skills.skills().stream().filter(s->s.skillId()==70).findFirst().orElseThrow().support();
    }
    private static void sql(ModernQuestSqlFixture f,String statement)throws Exception {
        try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.createStatement()) {
            s.setQueryTimeout(3);s.execute(statement);
        }
    }
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void learnedSourceTemplateAndMaterialExtensionsAreReloadedWithoutWeakeningQualification()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var f=ModernQuestSqlFixture.openNpc70Quest(Path.of(mysql),Path.of(shared),tls);
            var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
            var sun=f.config.sources().get(0);var moon=f.config.sources().get(1);
            var first=read(gate,sun);var second=read(gate,moon);
            assertEquals(7,first.skills.sourceId());assertEquals(42,second.skills.sourceId());
            assertEquals(first.skills.character().objectId(),second.skills.character().objectId());
            assertEquals(ModernSkills.Support.FIRE_STORM,support(first));assertEquals(ModernSkills.Support.FIRE_STORM,support(second));
            assertEquals(46,first.profile.mpCost());assertEquals(48,second.profile.mpCost());
            String cat="`"+ModernQuestSqlFixture.CATALOG+"`.";
            String source="`"+ModernQuestSqlFixture.source(42)+"`.";
            sql(f,"DELETE FROM "+source+"character_skills WHERE char_obj_id=3333 AND skill_id=70");
            assertTrue(read(gate,moon).skills.skills().isEmpty());
            assertEquals(ModernSkills.Support.FIRE_STORM,support(read(gate,sun)),"same objid cannot borrow/revoke another source");
            assertEquals(ModernSkills.Support.FIRE_STORM,support(second),"old imported snapshot is immutable");
            sql(f,"INSERT INTO "+source+"character_skills VALUES(3333,70)");
            sql(f,"UPDATE "+cat+"skills SET mpConsume=47 WHERE skill_id=70");
            assertEquals(ModernSkills.Support.UNSUPPORTED_TEMPLATE,support(read(gate,sun)));
            sql(f,"UPDATE "+cat+"skills SET mpConsume=48 WHERE skill_id=70");
            sql(f,"UPDATE "+cat+"skills_item SET itemids='',counts='' WHERE skill_id=70");
            assertEquals(ModernSkills.Support.UNSUPPORTED_TEMPLATE,support(read(gate,sun)),"empty strings are not SQL NULL qualification");
            sql(f,"UPDATE "+cat+"skills_item SET itemids=NULL,counts=NULL WHERE skill_id=70");
            sql(f,"INSERT INTO "+cat+"skills_buff_ability VALUES(70)");
            assertEquals(ModernSkills.Support.UNSUPPORTED_TEMPLATE,support(read(gate,moon)));
            sql(f,"DELETE FROM "+cat+"skills_buff_ability WHERE skill_id=70");
            sql(f,"DELETE FROM "+cat+"skills_item WHERE skill_id=70");
            assertEquals(ModernSkills.Support.UNSUPPORTED_TEMPLATE,support(read(gate,sun)),"missing row is not implicit no materials");
            sql(f,"INSERT INTO "+cat+"skills_item VALUES(70,NULL,NULL)");
            assertEquals(ModernSkills.Support.FIRE_STORM,support(read(gate,sun)));
            assertEquals(ModernSkills.Support.FIRE_STORM,support(read(gate,moon)));
            ModernNpcFireStormSqlFixture.assertSourceUnchanged(f.mysql);
            assertArrayEquals(new long[]{0,0,0},f.mysql.counts());
            executor.close();assertTrue(executor.awaitTermination(5000));assertEquals(0,f.mysql.activeConnections());f.successful();
        }
    }
}
