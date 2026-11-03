package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernVipEquipment.Field.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ModernVipEquipmentSqlTest {
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void fullCarriedVipCatalogFeedsSourceBoundActiveContributionWithoutWritingPlayerState()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var fixture=ModernQuestSqlFixture.open(Path.of(mysql),Path.of(shared),tls)) {
            ModernExpiredVipSqlFixture.seed(fixture);
            long carried=3_000_087L;
            try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
                s.setQueryTimeout(3);String catalog="`"+ModernQuestSqlFixture.CATALOG+"`.";
                s.execute("INSERT INTO "+catalog+"`etcitem` (item_id,name_id,classname,item_type,use_type,material,invgfx,grdgfx,weight,stackable,limit_class)"
                        +" VALUES(300087,'$26','add.VIP','other','none','none',80,45,1,0,127)");
                s.execute("INSERT INTO "+catalog+"`道具_自訂穿脫能力` (`道具編號`,`類型`,`力量`,`魅力`,`血量`,`魔量`,`魔法傷害減免`,`金幣獲取率`)"
                        +" VALUES(300087,1,7,11,4294967299,45,13,77)");
                for(int id:List.of(7,42))s.execute("INSERT INTO `"+ModernQuestSqlFixture.source(id)+"`.character_items"
                        +"(id,char_id,item_id,count,is_equipped,is_id,bless) VALUES("+carried+",3333,300087,1,"+(id==7?0:1)+",1,0)");
                s.execute("UPDATE `"+ModernQuestSqlFixture.source(7)+"`.character_items_time SET usertime='2999-01-01 00:00:00' WHERE itemr_obj_id="+ModernExpiredVipSqlFixture.VIP_OBJECT);
            }
            try(var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
                try {
                    for(var source:fixture.config.sources()) {
                        var dao=new ModernJdbcBackend(source.database(),1000,5000);
                        var data=gate.call(5000,scope->{
                            var auth=dao.authenticate(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD);
                            assertTrue(auth.accepted());
                            return dao.physicalCharacter(source.id(),auth.account(),3333,source.catalogDatabase(),false,false,true,false).orElseThrow();
                        });
                        assertEquals(java.util.Set.of(300086L,300087L),data.vipCatalog().vips().keySet());
                        var q=data.input().equipment();var binding=ModernCombatProfileTest.binding(q);long now=System.currentTimeMillis();
                        long shape=source.id()==7?5641:0;
                        var current=ModernVipLoadout.qualify(data,binding,Optional.empty(),now,shape);
                        assertEquals(source.id(),current.owner().sourceId());
                        if(source.id()==7) {
                            assertEquals(22,current.total().get(STR));assertEquals(22,current.total().get(CHA));
                            assertEquals(0,current.total().get(MAX_HP));assertEquals(100,current.total().get(ADENA_RATE));
                            var stored=ModernCombatProfileTest.stored(q,3,Map.of(carried,true));
                            var next=ModernVipLoadout.qualify(data,binding,stored,now,5641);
                            assertEquals(3,next.equipmentRevision());assertEquals(2,next.items().size());
                            assertEquals(29,next.total().get(STR));assertEquals(33,next.total().get(CHA));
                            assertEquals(4_294_967_299L,next.total().get(MAX_HP));assertEquals(45,next.total().get(MAX_MP));
                            assertEquals(13,next.total().get(MAGIC_REDUCTION));assertEquals(177,next.total().get(ADENA_RATE));
                            assertFalse(q.source().inventory().stream().filter(i->i.objectId()==carried).findFirst().orElseThrow().equipped());
                        } else {
                            assertEquals(7,current.total().get(STR));assertEquals(11,current.total().get(CHA));
                            assertEquals(4_294_967_299L,current.total().get(MAX_HP));assertEquals(77,current.total().get(ADENA_RATE));
                            assertThrows(IllegalArgumentException.class,()->ModernVipLoadout.qualify(data,binding,Optional.empty(),now,5641));
                        }
                        assertFalse(ModernPhysicalCombat.qualify(data,binding,Optional.empty(),now).supported());
                    }
                } finally {executor.close();assertTrue(executor.awaitTermination(5000));}
            }
            try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
                for(int id:List.of(7,42)) {
                    try(var s=c.prepareStatement("SELECT is_equipped,count FROM `"+ModernQuestSqlFixture.source(id)+"`.character_items WHERE id=? AND char_id=3333")) {
                        s.setQueryTimeout(3);s.setLong(1,carried);try(var rows=s.executeQuery()) {
                            assertTrue(rows.next());assertEquals(id==7?0:1,rows.getLong(1));assertEquals(1,rows.getLong(2));assertFalse(rows.next());
                        }
                    }
                    try(var s=c.prepareStatement("SELECT i.is_equipped,t.usertime FROM `"+ModernQuestSqlFixture.source(id)+"`.character_items i JOIN `"
                            +ModernQuestSqlFixture.source(id)+"`.character_items_time t ON t.itemr_obj_id=i.id WHERE i.id=?")) {
                        s.setQueryTimeout(3);s.setLong(1,ModernExpiredVipSqlFixture.VIP_OBJECT);try(var rows=s.executeQuery()) {
                            assertTrue(rows.next());assertEquals(1,rows.getLong(1));
                            assertTrue(rows.getString(2).startsWith(id==7?"2999-01-01":"2000-01-01"));assertFalse(rows.next());
                        }
                    }
                }
            }
            System.err.println("ACTIVE_VIP_SQL_CANDIDATE sources=2 carriedCatalog=true fullI64=true explicitSingleShape=true rawFlagsAndDeadlinesPreserved=true combatStillRejected=true");
            fixture.successful();
        }
    }

    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void realIsolatedMysqlReadsFullVipAndPolyThenPreservesBothSourceQualifiedPlayers()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var fixture=ModernQuestSqlFixture.open(Path.of(mysql),Path.of(shared),tls)) {
            ModernExpiredVipSqlFixture.seed(fixture);
            ModernExpiredVipSqlFixture.verifyQualified(fixture);
            ModernExpiredVipSqlFixture.auditPreserved(fixture);
            fixture.successful();
        }
    }

    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void expiredVipAlsoCoexistsWithAllModesExistingSuppliesWithoutReplacingThem()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var fixture=ModernQuestSqlFixture.openMixedModes(Path.of(mysql),Path.of(shared),tls,true,true)) {
            ModernExpiredVipSqlFixture.seed(fixture);
            ModernExpiredVipSqlFixture.auditPreserved(fixture);
            fixture.successful();
        }
    }
}
