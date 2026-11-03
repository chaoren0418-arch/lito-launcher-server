package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class ModernVipCharacterSqlTest {
    @TempDir Path directory;
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void repositoryAndSelectedAuthorityRebuildBothSourceCharactersWithoutSavedAcOrPartialVipReuse()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var fixture=ModernQuestSqlFixture.open(Path.of(mysql),Path.of(shared),tls)) {
            ModernExpiredVipSqlFixture.seed(fixture);
            try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
                s.setQueryTimeout(3);String catalog="`"+ModernQuestSqlFixture.CATALOG+"`.";
                s.execute("UPDATE "+catalog+"`道具_自訂穿脫能力` SET `力量`=7,`魅力`=11,`防禦`=-7,`血量`=4294967299 WHERE `道具編號`=300086");
                s.execute("UPDATE "+catalog+"`poly_變身賦予能力` SET addAc=9 WHERE gfxId=5641");
                for(int id:List.of(7,42)) {
                    s.execute("UPDATE `"+ModernQuestSqlFixture.source(id)+"`.character_items_time SET usertime='2999-01-01 00:00:00' WHERE itemr_obj_id="+ModernExpiredVipSqlFixture.VIP_OBJECT);
                    s.execute("UPDATE `"+ModernQuestSqlFixture.source(id)+"`.characters SET Ac="+(id==7?-9999:8888)+" WHERE objid=3333");
                }
            }
            var configured=new HashMap<Integer,ModernEquipmentRepository.Source>();var accounts=new HashMap<Integer,String>();
            var authorized=new HashSet<ModernAccountAuthentication.Source>();var connections=new AtomicInteger();
            try(var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession();var store=new ModernEquipmentOverrideStore(directory)) {
                try {
                    for(var source:fixture.config.sources()) {
                        var dao=new ModernJdbcBackend(source.database(),1000,5000,(url,properties)->{
                            connections.incrementAndGet();return new com.mysql.cj.jdbc.Driver().connect(url,properties);});
                        var auth=gate.call(5000,scope->dao.authenticate(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD));assertTrue(auth.accepted());
                        accounts.put(source.id(),auth.account());authorized.add(new ModernAccountAuthentication.Source(source.id(),"fixture-"+source.id()));
                        configured.put(source.id(),new ModernEquipmentRepository.Source(source,dao));
                    }
                    var principal=new ModernAccountAuthentication.Principal("vip-rebuild",authorized,accounts);
                    var provider=ModernEquipmentRepository.physicalJdbc(configured);
                    var authority=new ModernEquipmentAuthority((p,id,obj)->{throw new AssertionError("second equipment read");},null,provider,store);
                    for(int id:List.of(7,42)) {
                        var loaded=gate.call(5000,scope->provider.read(principal,id,3333));var q=loaded.source().input().equipment();var row=q.source().character();
                        int before=connections.get();
                        try(var lease=new ModernEquipmentLease.Issuer().open(new Object(),new TournamentPresence.CharacterKey(id,3333),1,()->{})) {
                            var result=gate.call(5000,scope->{try(var op=lease.operation(scope)) {
                                var profile=authority.prepareVipCharacter(principal,row,op,5641);assertTrue(op.isCommitAdmitted());return profile;
                            }});
                            assertEquals(before+1,connections.get());var p=result.character();var saved=p.source().persisted();
                            assertEquals(id,p.source().sourceId());assertEquals(id==7?-9999:8888,saved.armorClass());
                            assertEquals(saved.maxHp()+4_294_967_299L,p.effective().maximumHp());assertEquals(saved.strength()+29,p.effective().strength());
                            assertEquals(saved.charisma()+33,p.effective().charisma());
                            long ordinary=p.equipmentEvaluation().total(ModernEquipmentFacts.Stat.AC)-result.vip().total().get(ModernVipEquipment.Field.AC);
                            assertEquals(-16,result.vip().total().get(ModernVipEquipment.Field.AC));
                            assertEquals(ModernCharacterArmor.intrinsic(row.characterClass(),row.level(),saved.originalDexterity())+ordinary-16,p.effective().armorClass());
                            assertNotEquals(saved.armorClass(),p.effective().armorClass());assertEquals(0,p.equipmentRevision());
                            assertFalse(lease.hasActivePermit());
                        }
                        var binding=new ModernEquipmentReconciliation.ExpectedBinding(new TournamentPresence.CharacterKey(id,3333),loaded.ownerBinding());
                        var old=ModernPhysicalCombat.qualify(loaded.source(),binding,Optional.empty(),System.currentTimeMillis());
                        assertEquals(id==7?-9999:8888,old.profile().effective().armorClass());assertFalse(old.supported());
                    }
                    try(var files=java.nio.file.Files.list(directory)){assertEquals(0,files.filter(p->p.toString().endsWith(".meo")).count());}
                }finally{executor.close();assertTrue(executor.awaitTermination(5000));}
            }
            try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
                s.setQueryTimeout(3);
                for(int id:List.of(7,42))try(var rows=s.executeQuery("SELECT c.Ac,i.is_equipped,i.count,t.usertime FROM `"+ModernQuestSqlFixture.source(id)+"`.characters c JOIN `"
                        +ModernQuestSqlFixture.source(id)+"`.character_items i ON i.char_id=c.objid JOIN `"+ModernQuestSqlFixture.source(id)
                        +"`.character_items_time t ON t.itemr_obj_id=i.id WHERE c.objid=3333 AND i.id="+ModernExpiredVipSqlFixture.VIP_OBJECT)) {
                    assertTrue(rows.next());assertEquals(id==7?-9999:8888,rows.getLong(1));assertEquals(1,rows.getLong(2));assertEquals(1,rows.getLong(3));
                    assertTrue(rows.getString(4).startsWith("2999-01-01"));assertFalse(rows.next());
                }
            }
            assertEquals(0,fixture.mysql.activeConnections());
            System.err.println("VIP_CHARACTER_SQL sources=2 repository=true authority=true oneConnection=true rebuiltAc=true vipOnce=true fullI64=true sourcePreserved=true oldCombatStillRejected=true");
            fixture.successful();
        }
    }
}
