package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Pure parameter, anonymous aggregate and proxy tests; no driver/socket/DB execution. */
class ModernSkillsReadProbeTest {
    @Test void explicitSecondFlagIsTheOnlyWayToOptInAndNeverChangesDefaultMode(){
        assertFalse(ModernSkillsReadProbe.fireStormOptIn(new String[]{"fixture.properties"}));
        assertTrue(ModernSkillsReadProbe.fireStormOptIn(new String[]{"fixture.properties","--fire-storm"}));
        for(String[] args:new String[][]{null,{}, {""}, {"fixture","fire"}, {"fixture"," --fire-storm"}, {"fixture","--fire-storm","extra"}})
            assertThrows(IllegalArgumentException.class,()->ModernSkillsReadProbe.fireStormOptIn(args));
    }
    @Test void defaultAggregateKeepsOldFieldsAndEveryCurrentSupportIsCountedWithoutIdentityText(){
        var old=new ModernSkillsReadProbe.Counts();old.add(List.of(new ModernSkills.Skill(5,"DO_NOT_PRINT",10,10,ModernSkills.Support.TELEPORT)));old.validate();
        assertEquals(",\"characters\":1,\"skills\":1,\"teleport_qualified\":1,\"light_arrow_qualified\":0,\"unimplemented\":0,\"unsupported_template\":0,\"empty_skill_characters\":0",old.json(false));
        var all=new ModernSkillsReadProbe.Counts();all.add(List.of(
                new ModernSkills.Skill(1,"DO_NOT_PRINT",4,10,ModernSkills.Support.BASIC_HEAL),
                new ModernSkills.Skill(4,"DO_NOT_PRINT",3,10,ModernSkills.Support.LIGHT_ARROW),
                new ModernSkills.Skill(5,"DO_NOT_PRINT",10,10,ModernSkills.Support.TELEPORT),
                new ModernSkills.Skill(6,"DO_NOT_PRINT",9,10,ModernSkills.Support.UNIMPLEMENTED),
                new ModernSkills.Skill(70,"DO_NOT_PRINT",48,2000,ModernSkills.Support.FIRE_STORM)));
        all.add(List.of(new ModernSkills.Skill(70,"DO_NOT_PRINT",48,2000,ModernSkills.Support.UNSUPPORTED_TEMPLATE)));all.add(List.of());all.validate();
        assertEquals(6,all.skills);assertEquals(3,all.characters);assertEquals(1,all.fireQualified);assertEquals(1,all.fireUnsupported);assertEquals(1,all.basicHeal);
        assertTrue(all.json(true).contains("\"fire_qualified\":1"));assertFalse(all.json(true).contains("DO_NOT_PRINT"));
        var heal=new ModernSkillsReadProbe.Counts();heal.add(List.of(new ModernSkills.Skill(1,"private",4,10,ModernSkills.Support.BASIC_HEAL)));heal.validate();
        assertTrue(heal.json(false).contains("\"basic_heal_qualified\":1"));
    }
    @Test void exactOptInQueriesAreSourceBoundAndExtensionCompletionAuditIsNotAQueryCountGuess(){
        assertEquals(ModernSkillsReadProbe.Query.SKILLS,ModernSkillsReadProbe.classify(ModernSkillsJdbc.sql("catalog",true),"catalog",false));
        assertEquals(ModernSkillsReadProbe.Query.SKILLS,ModernSkillsReadProbe.classify(ModernSkillsJdbc.sql("catalog",false,true),"catalog",true));
        assertEquals(ModernSkillsReadProbe.Query.FIRE_EXTRAS,ModernSkillsReadProbe.classify(ModernSkillsJdbc.fireExtrasSql("catalog"),"catalog",true));
        assertEquals(ModernSkillsReadProbe.Query.UNKNOWN,ModernSkillsReadProbe.classify(ModernSkillsJdbc.fireExtrasSql("catalog"),"catalog",false));
        assertEquals(ModernSkillsReadProbe.Query.UNKNOWN,ModernSkillsReadProbe.classify(ModernSkillsJdbc.fireExtrasSql("other"),"catalog",true));
        var d=new ModernSkillsReadProbe.Diagnostics();d.prepare(ModernSkillsReadProbe.Query.FIRE_EXTRAS);d.row();
        assertThrows(IllegalStateException.class,d::requireCompletedExtras);d.endRows();d.endRows();d.requireCompletedExtras();
        assertEquals(",\"fire_storm_opt_in\":true,\"fire_extras_queries\":1,\"fire_extras_completed\":1,\"fire_extras_rows\":1",d.extrasJson());
    }
    @Test void optInAuditRejectsUnknownOrWriteExecutionBeforeDriverAndPreservesOriginalSqlFailure()throws Exception{
        var calls=new AtomicInteger();var error=new SQLException("DO_NOT_PRINT SQL account database","42000",1142);
        var statement=(PreparedStatement)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PreparedStatement.class},(p,m,a)->{
            if(m.getName().equals("executeQuery"))throw error;throw new AssertionError("driver operation must not run");});
        var raw=(Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->{
            calls.incrementAndGet();if(m.getName().equals("prepareStatement"))return statement;throw new AssertionError("driver operation must not run");});
        var d=new ModernSkillsReadProbe.Diagnostics();var c=ModernSkillsReadProbe.observe(raw,"catalog",d,true);
        assertThrows(SQLException.class,()->c.prepareStatement("UPDATE forbidden SET value=1"));assertThrows(SQLException.class,c::createStatement);
        assertThrows(SQLException.class,()->c.setReadOnly(false));assertThrows(SQLException.class,c::commit);assertEquals(0,calls.get());
        var s=c.prepareStatement(ModernSkillsJdbc.fireExtrasSql("catalog"));assertThrows(SQLException.class,s::executeUpdate);
        assertThrows(SQLException.class,()->s.executeQuery("SELECT unqualified"));assertEquals(1,calls.get());
        assertSame(error,assertThrows(SQLException.class,s::executeQuery));String json=d.json();assertFalse(json.contains("DO_NOT_PRINT"));assertFalse(json.contains("catalog"));
        assertTrue(json.contains("\"jdbc_query\":\"FIRE_EXTRAS\""));assertTrue(json.contains("\"jdbc_sqlstate\":\"42000\""));assertTrue(json.contains("\"jdbc_vendor\":1142"));
    }
}
