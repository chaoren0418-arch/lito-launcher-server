package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** New owned MySQL only; no native configuration, account or production schema. */
class ModernArmorProtectionCatalogSqlTest {
    @Test @Timeout(value=90,unit=TimeUnit.SECONDS)
    void realConnectorKeepsTwoCatalogsDistinctAndNeverNeedsCharacterSkillsOrConsumesResources()throws Exception {
        var home=System.getenv(ModernSettlementMysqlFixture.ENV);Assumptions.assumeTrue(home!=null);
        try(var mysql=ModernSettlementMysqlFixture.open(Path.of(home))) {
            try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
                for(var db:List.of("catalog","armor_catalog_b"))seed(c,db);
                execute(c,"UPDATE armor_catalog_b.skills SET castgfx=228+4294967296 WHERE skill_id=68");
            }
            var a=read(mysql,7,"catalog");var b=read(mysql,42,"armor_catalog_b");
            assertEquals(ModernArmorProtectionCatalog.Status.QUALIFIED,a.status());
            assertEquals(ModernArmorProtectionCatalog.Status.UNSUPPORTED_TEMPLATE,b.status());
            var source=ModernArmorProtectionCatalogTest.source(7);var profile=ModernPhysicalCombatTest.fighter(source).profile();
            var actor=ModernArmorProcProtectionTest.actor();var selection=a.select(source,profile,actor);
            assertEquals(3,selection.rules().get(0).rule().seconds());assertThrows(IllegalArgumentException.class,()->b.select(source,profile,actor));
            try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
                try(var q=c.prepareStatement("SELECT mpConsume,itemConsumeId,itemConsumeCount,buffDuration,castgfx FROM catalog.skills WHERE skill_id=68")) {
                    q.setQueryTimeout(2);try(var rows=q.executeQuery()) {
                        assertTrue(rows.next());assertEquals(30,rows.getLong(1));assertEquals(40318,rows.getLong(2));
                        assertEquals(2,rows.getLong(3));assertEquals(32,rows.getLong(4));assertEquals(228,rows.getLong(5));assertFalse(rows.next());
                    }
                }
                execute(c,"INSERT INTO catalog.skills_buff_ability VALUES(68)");
            }
            assertEquals(ModernArmorProtectionCatalog.Status.UNSUPPORTED_TEMPLATE,read(mysql,7,"catalog").status());
            try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
                execute(c,"DELETE FROM catalog.skills_buff_ability WHERE skill_id=68");
                execute(c,"DELETE FROM catalog.skills WHERE skill_id=68");
            }
            assertEquals(ModernArmorProtectionCatalog.Status.MISSING,read(mysql,7,"catalog").status());
            assertThrows(SQLException.class,()->read(mysql,7,"uncreated_catalog"));
            System.err.println("ARMOR_PROTECTION_CATALOG_SQL_OK sources=2 widenedGfxRejected=true unknownAbilityRejected=true learnedTablesAbsent=true manualTupleUnchanged=true runtimeGrant=false");
            mysql.successful();
        }
    }
    private static ModernArmorProtectionCatalog.Snapshot read(ModernSettlementMysqlFixture mysql,int source,String db)throws Exception {
        var scope=ModernDisintegrateCatalogTest.scope();ModernBackendScope.bind(scope);
        try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(5))) {
            c.setReadOnly(true);
            return ModernArmorProtectionCatalog.read(c,scope,source,db);
        }finally{scope.finish();ModernBackendScope.unbind();}
    }
    private static void seed(Connection c,String db)throws Exception {
        String p="`"+db+"`.";
        execute(c,"CREATE DATABASE `"+db+"` CHARACTER SET utf8mb4");
        String columns="skill_id effect_skill_id skill_level skill_number mpConsume hpConsume itemConsumeId itemConsumeCount reuseDelay buffDuration "
                +"target_to damage_value damage_dice damage_dice_count probability_value probability_dice attr type lawful ranged area through id action_id castgfx castgfx2 sysmsgID_happen sysmsgID_stop sysmsgID_fail";
        String definition=String.join(",",java.util.Arrays.stream(columns.split(" ")).map(n->"`"+n+"` BIGINT NOT NULL DEFAULT 0").toList());
        execute(c,"CREATE TABLE "+p+"skills("+definition+",target VARCHAR(16) NOT NULL)");
        execute(c,"INSERT INTO "+p+"skills(skill_id,skill_level,skill_number,mpConsume,itemConsumeId,itemConsumeCount,reuseDelay,buffDuration,target,"
                +"target_to,type,ranged,id,action_id,castgfx,sysmsgID_happen,sysmsgID_stop,sysmsgID_fail) VALUES(68,9,3,30,40318,2,1500,32,'buff',4,2,-1,8,19,228,314,315,280)");
        execute(c,"CREATE TABLE "+p+"skills_item(skill_id BIGINT,itemids VARCHAR(10),counts VARCHAR(10))");
        execute(c,"INSERT INTO "+p+"skills_item VALUES(68,NULL,NULL)");
        execute(c,"CREATE TABLE "+p+"skills_buff_ability(skill_id BIGINT)");
        execute(c,"CREATE TABLE "+p+"`skills_負面技能控制`(skill_id BIGINT)");
    }
    private static void execute(Connection c,String sql)throws Exception {
        try(var s=c.createStatement()){s.setQueryTimeout(3);s.execute(sql);}
    }
}
