package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernArmorProcProtection.*;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** SQL writes require this newly owned fixture. No native server configuration is accepted. */
class ModernArmorProcsSqlTest {
    private static final long OBJECT=3_000_089;

    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void realMysqlSelectsExactOrWildcardRulesForSourceQualifiedGearButKeepsCombatBlocked()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var fixture=ModernQuestSqlFixture.open(Path.of(mysql),Path.of(shared),tls)) {
            seed(fixture);
            var original=snapshot(fixture);
            var selections=new ArrayList<Selection>();var candidates=new ArrayList<Candidate>();
            try(var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
                try {
                    for(var source:fixture.config.sources()) {
                        var dao=new ModernJdbcBackend(source.database(),1000,5000);
                        var data=gate.call(5000,scope->{
                            var auth=dao.authenticate(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD);
                            if(!auth.accepted())throw new IllegalStateException("armor fixture authentication");
                            return dao.physicalCharacter(source.id(),auth.account(),3333,source.catalogDatabase()).orElseThrow();
                        });
                        var fighter=ModernPhysicalCombat.qualify(data,ModernCombatProfileTest.binding(data.input().equipment()),
                                java.util.Optional.empty(),System.currentTimeMillis());
                        assertFalse(fighter.supported());assertEquals(ModernPhysicalCombat.UnsupportedReason.EQUIPMENT_PROC,fighter.unsupportedReason());
                        var actor=new Actor(new UUID(0,326),new TournamentPresence.CharacterKey(source.id(),3333),1,0);
                        var selection=select(data,fighter.profile(),actor);selections.add(selection);
                        int seconds=source.id()==7?3:6;
                        assertEquals(source.id()==7?1:2,selection.rules().size());
                        assertEquals(source.id()==7?50:51,selection.rules().get(0).rule().id());
                        assertTrue(selection.rules().stream().allMatch(e->e.objectId()==OBJECT));
                        var candidate=prepareHurt(selection,initial(actor),0,1000,100,100,true,bound->0);candidates.add(candidate);
                        var applied=accept(candidate,selection,initial(actor),1000);
                        assertEquals(900,candidate.hpAfter());assertEquals(seconds*SECOND,applied.untilNanos());
                        assertEquals(1,candidate.triggers().size());assertEquals(11101,candidate.triggers().get(0).graphics());
                        assertEquals(50,protectedDamage(applied,SECOND,101));
                        assertEquals(101,protectedDamage(applied,seconds*SECOND,101));
                        var repeated=prepareHurt(selection,applied,SECOND,900,50,50,true,bound->0);
                        assertEquals(applied.untilNanos(),repeated.after().untilNanos());assertFalse(repeated.triggers().get(0).appliedProtection());
                    }
                }finally{executor.close();if(!executor.awaitTermination(5000))throw new IllegalStateException("armor fixture worker retained");}
            }
            assertEquals(2,selections.size());
            assertThrows(ProcException.class,()->accept(candidates.get(0),selections.get(1),initial(selections.get(1).actor()),1000));
            assertEquals(original,snapshot(fixture));
            System.err.println("ARMOR_PROC_SQL_OK sources=2 exactAndWildcard=true combatStillBlocked=true pureCandidatesOnly=true sourceRowsUnchanged=true");
            fixture.successful();
        }
    }

    static void seed(ModernQuestSqlFixture fixture)throws Exception {
        try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
            String catalog="`"+ModernQuestSqlFixture.CATALOG+"`.";
            // The base fixture has only weapon lookup tables. Armor requires
            // its own complete (here explicitly empty) rule tables, including
            // exact and over-safe enchantment lookups for source 42's +5 item.
            for(var suffix:List.of("greater_類型","bless_類型","bless_指定")) {
                execute(c,"CREATE TABLE "+catalog+"`裝備_armor_"+suffix+"` LIKE "+catalog+"`裝備_weapon_"+suffix+"`");
                if(!suffix.equals("bless_指定"))execute(c,"ALTER TABLE "+catalog+"`裝備_armor_"+suffix+"` CHANGE weapon_type armor_type BIGINT NOT NULL DEFAULT 0");
            }
            execute(c,"CREATE TABLE "+catalog+"`裝備_armor_enchant_指定` LIKE "+catalog+"`裝備_armor_bless_指定`");
            execute(c,"ALTER TABLE "+catalog+"`裝備_armor_enchant_指定` CHANGE bless_id enchant_level BIGINT NOT NULL DEFAULT 0");
            execute(c,"CREATE TABLE "+catalog+"`裝備_armor_enchant_類型` LIKE "+catalog+"`裝備_armor_bless_類型`");
            execute(c,"ALTER TABLE "+catalog+"`裝備_armor_enchant_類型` CHANGE bless_id over_enchant BIGINT NOT NULL DEFAULT 0");
            for(var column:ModernArmorProcs.COLUMNS)if(!column.equals("id")&&!column.equals("item_id"))
                execute(c,"ALTER TABLE "+catalog+"`裝備_armor_魔法` ADD COLUMN `"+column+"` BIGINT NOT NULL DEFAULT 0");
            execute(c,"INSERT INTO "+catalog+"armor(item_id,name_id,type,invgfx,grdgfx,weight,use_royal,use_knight,use_elf,use_mage,use_darkelf,use_dragonknight,use_illusionist)"
                    +" VALUES(88889,'$1','armor',1,1,1,1,1,1,1,1,1,1)");
            execute(c,"INSERT INTO "+catalog+"`裝備_armor_魔法`(id,item_id,enchant_level,proc_group,on_hurt_chance,trigger_skill,skill_time_sec,skill_target,gfx_id,gfx_mode)"
                    +" VALUES(50,88889,-1,88,10,68,3,1,11101,1),(51,88889,5,88,10,68,6,1,11101,1),(52,88889,5,89,0,999,0,0,0,0)");
            for(int id:List.of(7,42))execute(c,"INSERT INTO `"+ModernQuestSqlFixture.source(id)+"`.character_items(id,char_id,item_id,count,is_equipped,enchantlvl,is_id,bless)"
                    +" VALUES("+OBJECT+",3333,88889,1,1,"+(id==7?0:5)+",1,0)");
        }
    }
    static List<Long> snapshot(ModernQuestSqlFixture fixture)throws Exception {
        var result=new ArrayList<Long>();
        try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
            for(int id:List.of(7,42))try(var s=c.prepareStatement("SELECT char_id,item_id,count,is_equipped,enchantlvl FROM `"
                    +ModernQuestSqlFixture.source(id)+"`.character_items WHERE id="+OBJECT)) {
                s.setQueryTimeout(3);try(var rows=s.executeQuery()) {
                    assertTrue(rows.next());for(int col=1;col<=5;col++)result.add(rows.getLong(col));assertFalse(rows.next());
                }
            }
        }
        return List.copyOf(result);
    }
    private static void execute(Connection c,String sql)throws Exception {
        try(var s=c.createStatement()){s.setQueryTimeout(3);s.execute(sql);}
    }
}
