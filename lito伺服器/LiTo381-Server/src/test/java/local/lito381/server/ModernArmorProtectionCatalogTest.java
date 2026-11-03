package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.Test;
import local.lito381.server.ModernDisintegrateCatalogTest.Jdbc;

class ModernArmorProtectionCatalogTest {
    static Map<String,Object> row() {
        String[] names=("template_id skill_level skill_number mp_cost hp_cost item_id item_count reuse_millis buff_duration "
                +"target_to damage_value damage_dice damage_dice_count probability_value probability_dice attr spell_type lawful "
                +"maximum_range area through_flag template_action action_id castgfx castgfx2 sysmsg_happen sysmsg_stop sysmsg_fail effect_skill_id target_bytes").split(" ");
        long[] values={68,9,3,30,0,40318,2,1500,32,4,0,0,0,0,0,0,2,0,-1,0,0,8,19,228,0,314,315,280,0,4};
        assertEquals(names.length,values.length);var row=new HashMap<String,Object>();
        for(int i=0;i<names.length;i++)row.put(names[i],values[i]);row.put("target","buff");return row;
    }
    static ModernArmorProtectionCatalog.Snapshot read(Jdbc jdbc,int source,String database,boolean readOnly)throws Exception {
        var scope=ModernDisintegrateCatalogTest.scope();ModernBackendScope.bind(scope);
        try{return readInScope(jdbc,source,database,readOnly);}
        finally{scope.finish();ModernBackendScope.unbind();}
    }
    static ModernArmorProtectionCatalog.Snapshot readInScope(Jdbc jdbc,int source,String database,boolean readOnly)throws Exception {
        var scope=ModernBackendScope.current();
        Connection real=jdbc.connection();
        var wrapped=(Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->{
            if(m.getName().equals("isReadOnly"))return readOnly;
            try{return m.invoke(real,a);}catch(InvocationTargetException e){throw e.getCause();}
        });
        return ModernArmorProtectionCatalog.read(wrapped,scope,source,database);
    }
    static ModernArmorProtectionCatalog.Snapshot read(Jdbc jdbc)throws Exception {return read(jdbc,7,"catalog",true);}
    static ModernPhysicalCombat.Source source(int source) {
        var armor=ModernEquipmentEvaluatorTest.instance(1003,88889,ModernCharacterData.ItemKind.ARMOR,true,0,"$1",List.of(),
                ModernEquipmentFacts.Ability.zero(),ModernEquipmentFacts.Ability.zero(),ModernEquipmentEvaluatorTest.emptyCards(),
                ModernEquipmentFacts.Stone.none(),ModernEquipmentFacts.TemplatePhysical.zero());
        var s=ModernPhysicalCombatTest.source(source,0,30,List.of(armor));
        return new ModernPhysicalCombat.Source(s.input(),s.growth(),s.templates(),Set.of(88889L),null,null,null,null,null,null,
                new ModernArmorProcs.Catalog(source,"catalog",Map.of(88889L,List.of(ModernArmorProcsTest.rule(50,88889,-1,88,0,10)))));
    }
    @Test void fullRowUsesBoundedReadonlyCatalogSelectsAndDoesNotRequireLearnedSkillOrCastCosts()throws Exception {
        var jdbc=new Jdbc(List.of(row()));var result=read(jdbc);
        assertEquals(ModernArmorProtectionCatalog.Status.QUALIFIED,result.status());
        assertEquals(List.of(1,2,4),jdbc.limits);assertEquals(3,jdbc.closedResults);assertEquals(3,jdbc.closedStatements);assertEquals(0,jdbc.closedConnections);
        assertTrue(jdbc.parameters.isEmpty());assertTrue(jdbc.queries.stream().allMatch(s->s.startsWith("SELECT ")&&!s.contains("character_skills")));
        assertTrue(jdbc.queries.get(1).contains("`catalog`.`skills` WHERE skill_id=68 LIMIT 2"));
        assertTrue(jdbc.timeouts.stream().allMatch(n->n>0&&n<=10));
        var source=source(7);var fighter=ModernPhysicalCombatTest.fighter(source);var actor=ModernArmorProcProtectionTest.actor();
        var selection=result.select(source,fighter.profile(),actor);
        assertEquals(3,selection.rules().get(0).rule().seconds()); // NOT the SQL manual duration32.
        assertFalse(fighter.supported());assertEquals(ModernPhysicalCombat.UnsupportedReason.EQUIPMENT_PROC,fighter.unsupportedReason());
        assertFalse(result.toString().contains("catalog"));
        var candidate=ModernArmorProcProtection.prepareHurt(selection,ModernArmorProcProtection.initial(actor),0,1000,100,100,true,b->0);
        assertEquals(3*ModernArmorProcProtection.SECOND,candidate.after().untilNanos());
    }
    @Test void completeSourceBundleRejectsForeignCatalogAndRetainsTheExecutionRestriction()throws Exception {
        var source=source(7);var skill=read(new Jdbc(List.of(row())));
        var bundle=new ModernArmorProtectionCatalog.BoundSource(source,skill);
        var fighter=ModernPhysicalCombatTest.fighter(bundle.physical());
        assertEquals(skill,bundle.skill());
        assertEquals(1,bundle.select(fighter.profile(),ModernArmorProcProtectionTest.actor()).rules().size());
        assertEquals(ModernPhysicalCombat.UnsupportedReason.EQUIPMENT_PROC,fighter.unsupportedReason());
        assertThrows(IllegalArgumentException.class,()->new ModernArmorProtectionCatalog.BoundSource(source(42),skill));
        var foreign=read(new Jdbc(List.of(row())),7,"other_catalog",true);
        assertThrows(IllegalArgumentException.class,()->new ModernArmorProtectionCatalog.BoundSource(source,foreign));
        assertEquals("ArmorProtectionSource[redacted]",bundle.toString());
    }
    @Test void everyTupleDifferenceAndLossyNumberRejectsWithoutNativeNarrowing()throws Exception {
        for(String field:row().keySet())if(!Set.of("template_id","target","target_bytes").contains(field)) {
            var altered=row();altered.put(field,((Number)altered.get(field)).longValue()+(1L<<32));
            assertEquals(ModernArmorProtectionCatalog.Status.UNSUPPORTED_TEMPLATE,read(new Jdbc(List.of(altered))).status(),field);
            for(Object value:new Object[]{null,new BigDecimal("0.5"),new BigDecimal("9223372036854775808"),1.0,"1"}) {
                var bad=row();bad.put(field,value);assertThrows(SQLException.class,()->read(new Jdbc(List.of(bad))),field);
            }
        }
        var target=row();target.put("target","self");assertEquals(ModernArmorProtectionCatalog.Status.UNSUPPORTED_TEMPLATE,read(new Jdbc(List.of(target))).status());
        for(Object value:new Object[]{null,67L,68L+(1L<<32)}){var bad=row();bad.put("template_id",value);assertThrows(SQLException.class,()->read(new Jdbc(List.of(bad))));}
        assertThrows(SQLException.class,()->read(new Jdbc(List.of(row(),row()))));
    }
    @Test void missingCatalogIsNotAQualifiedZeroEffectAndCannotAuthorizeActiveRules()throws Exception {
        var jdbc=new Jdbc(List.of());var missing=read(jdbc);
        assertEquals(ModernArmorProtectionCatalog.Status.MISSING,missing.status());assertEquals(2,jdbc.queries.size());
        var source=source(7);var profile=ModernPhysicalCombatTest.fighter(source).profile();var actor=ModernArmorProcProtectionTest.actor();
        assertThrows(IllegalArgumentException.class,()->missing.select(source,profile,actor));
        var foreignSource=read(new Jdbc(List.of(row())),42,"catalog",true);
        assertThrows(IllegalArgumentException.class,()->foreignSource.select(source,profile,actor));
        var foreignDatabase=read(new Jdbc(List.of(row())),7,"moon_catalog",true);
        assertThrows(IllegalArgumentException.class,()->foreignDatabase.select(source,profile,actor));
        var known=read(new Jdbc(List.of(row())));
        var wrongOwner=new ModernArmorProcProtection.Actor(actor.match(),new TournamentPresence.CharacterKey(7,3334),1,0);
        assertThrows(IllegalArgumentException.class,()->known.select(source,profile,wrongOwner));
    }
    @Test void effectAliasAndSupplementalAbilityOrNegativeRulesCannotDisappear()throws Exception {
        for(var extras:List.of(List.<Map<String,Object>>of(),List.of(Jdbc.EMPTY_ITEM,Jdbc.EMPTY_ITEM),
                List.of(Map.<String,Object>of("extension_kind",0L,"ids_null",0L,"counts_null",0L)),
                List.of(Jdbc.EMPTY_ITEM,Map.<String,Object>of("extension_kind",1L,"ids_null",1L,"counts_null",1L)),
                List.of(Jdbc.EMPTY_ITEM,Map.<String,Object>of("extension_kind",2L,"ids_null",1L,"counts_null",1L)))) {
            var jdbc=new Jdbc(List.of(row()));jdbc.extras=extras;
            assertEquals(ModernArmorProtectionCatalog.Status.UNSUPPORTED_TEMPLATE,read(jdbc).status());
        }
        var absent=new Jdbc(List.of(row()));absent.guardFailure=new SQLException("private","42S22",1054);
        assertEquals(ModernArmorProtectionCatalog.Status.QUALIFIED,read(absent).status());
        assertTrue(absent.queries.get(1).contains("0 AS effect_skill_id"));
        var denied=new Jdbc(List.of(row()));denied.guardFailure=new SQLException("private endpoint","42000",1142);
        var error=assertThrows(SQLException.class,()->read(denied));assertEquals("modern armor protection catalog unavailable",error.getMessage());assertNull(error.getCause());
    }
    @Test void writableUnboundOrCancelledConnectionsCannotPublishAndOwnedResourcesClose()throws Exception {
        var writable=new Jdbc(List.of(row()));assertThrows(SQLException.class,()->read(writable,7,"catalog",false));assertTrue(writable.queries.isEmpty());
        for(int query=1;query<=3;query++)for(int next=1;next<=2;next++) {
            if(query==1&&next==2)continue;
            var jdbc=new Jdbc(List.of(row()));jdbc.cancelQuery=query;jdbc.cancelNext=next;
            assertThrows(ModernBackendExecutor.BackendException.class,()->read(jdbc));
            assertEquals(jdbc.queries.size(),jdbc.closedStatements);assertEquals(jdbc.queries.size(),jdbc.closedResults);
        }
        for(String database:new String[]{null,"","a;drop","a".repeat(65)}) {
            var jdbc=new Jdbc(List.of(row()));assertThrows(IllegalArgumentException.class,()->read(jdbc,7,database,true));assertTrue(jdbc.queries.isEmpty());
        }
    }
}
