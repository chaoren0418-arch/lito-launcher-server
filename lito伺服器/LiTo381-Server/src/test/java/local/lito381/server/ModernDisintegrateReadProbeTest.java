package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ModernDisintegrateReadProbeTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    @Test void statementAndExecutionAllowlistBlocksMutationBeforeInvokingDriver()throws Exception {
        int[] prepares={0};var delegate=ModernDisintegrateCatalogTest.proxy(Connection.class,(m,a)->{
            if(m.equals("prepareStatement")){prepares[0]++;return ModernDisintegrateCatalogTest.proxy(java.sql.PreparedStatement.class,(n,b)->{
                if(n.equals("close"))return null;throw new AssertionError("unexpected driver statement "+n);
            });}
            throw new AssertionError("unexpected driver connection "+m);
        });
        var c=ModernDisintegrateReadProbe.observe(delegate,"catalog");
        for(String sql:List.of("UPDATE characters SET Lawful=500","SELECT 1","SELECT DATABASE();DELETE FROM characters",ModernDisintegrateCatalog.sql("other",true)))
            assertThrows(SQLException.class,()->c.prepareStatement(sql));
        assertEquals(0,prepares[0]);assertThrows(SQLException.class,c::createStatement);assertThrows(SQLException.class,()->c.prepareCall("CALL x()"));
        assertThrows(SQLException.class,()->c.setReadOnly(false));assertThrows(SQLException.class,c::commit);assertThrows(SQLException.class,()->c.unwrap(Connection.class));
        try(var s=c.prepareStatement(ModernDisintegrateCatalog.sql("catalog",true))) {
            assertEquals(1,prepares[0]);assertThrows(SQLException.class,s::execute);assertThrows(SQLException.class,s::executeUpdate);
            assertThrows(SQLException.class,s::executeLargeUpdate);assertThrows(SQLException.class,s::executeBatch);assertThrows(SQLException.class,s::addBatch);
            assertThrows(SQLException.class,()->s.executeQuery("SELECT 1"));assertThrows(SQLException.class,s::getConnection);
        }
    }
    @Test void selectedSourcePinsCircleTenLawfulDeltaAndCriticalConfigWithoutReadingLTAssets()throws Exception {
        var sql=Files.readString(Path.of("../1-伺服器/db/380_db.sql"));
        assertTrue(sql.contains("INSERT INTO `skills` VALUES (77, 0, '究極光裂術', 10, 4, 70, 0, 0, 0, 5000, 0, 'attack', 3, 190, 15, 8, 0, 0, 16, 64, 1, 10, 0, 0, 16, '$1485', 18, 11746, 0, 0, 0, 0);"));
        assertTrue(sql.contains("INSERT INTO `skills_item` VALUES (77, '究極光裂術', NULL, NULL);"));
        assertFalse(sql.contains("INSERT INTO `skills_buff_ability` VALUES (77,"));assertFalse(sql.contains("INSERT INTO `skills_負面技能控制` VALUES (77,"));
        var config=Files.readString(Path.of("../1-伺服器/config/其他控制端/4-職業_技能設定表.properties"));
        assertTrue(config.contains("DISINTEGRATE_CRITICAL_RND = 40"));assertTrue(config.contains("CRITICAL_SPR = 11748"));
        var use=Files.readString(Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("(this._skillId == DISINTEGRATE) && (pc.getLawful() < 500)"));
        assertTrue(use.contains("this._player.addLawful(this._skill.getLawful())"));
    }
}
