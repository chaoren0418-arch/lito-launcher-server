package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernShopAuthority.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;

class ModernSpellMaterialsRealMysqlTest {
    static final String FIRST=ModernQuestSqlFixture.CATALOG, SECOND="r269_material_catalog_42";
    static String table(String db,String name){return Character.toString(96)+db+Character.toString(96)+"."+Character.toString(96)+name+Character.toString(96);}
    @Test @Timeout(value=180,unit=TimeUnit.SECONDS)
    void ownedSqlCatalogSupportsIsolatedPurchaseCaptureDebitAndSourceInventoryStaysUntouched() throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null,"explicit owned MySQL/shared root required");
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var f=ModernQuestSqlFixture.open(Path.of(mysql),Path.of(shared),tls);
                var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
            admin(f,"INSERT INTO "+table(FIRST,"etcitem")+"(item_id,name_id,classname,item_type,use_type,material,invgfx,grdgfx,weight,stackable,limit_class) VALUES(40318,'$508','0','material','none','gemstone',235,773,2000,1,127)");
            admin(f,"INSERT INTO "+table(FIRST,"lito_tournament_shop")+" VALUES(40318,1,100,50,1)");
            admin(f,"CREATE DATABASE "+SECOND);
            for(String name:List.of("etcitem","weapon","armor","lito_tournament_shop")) {
                admin(f,"CREATE TABLE "+table(SECOND,name)+" LIKE "+table(FIRST,name));
                admin(f,"INSERT INTO "+table(SECOND,name)+" SELECT * FROM "+table(FIRST,name));
                admin(f,"GRANT SELECT ON "+table(SECOND,name)+" TO '"+ModernQuestSqlFixture.READER+"'@'127.0.0.1'");
            }
            admin(f,"UPDATE "+table(SECOND,"etcitem")+" SET weight=5000000000 WHERE item_id=40318");
            admin(f,"ALTER TABLE "+table(ModernQuestSqlFixture.source(42),"characters")+" MODIFY account_name VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL");
            admin(f,"UPDATE "+table(ModernQuestSqlFixture.source(42),"accounts")+" SET login='FIXTURE-USER'");
            admin(f,"UPDATE "+table(ModernQuestSqlFixture.source(42),"characters")+" SET account_name='FIXTURE-USER'");
            var sun=new ModernJdbcBackend(f.config.sources().get(0).database(),1000,5000);
            var moon=new ModernJdbcBackend(f.config.sources().get(1).database(),1000,5000);
            var verifier=ModernAccountAuthentication.repositories(Map.of(
                    7,ModernAccountAuthentication.SourceBinding.direct("太陽神",sun::authenticate),
                    42,ModernAccountAuthentication.SourceBinding.direct("月亮神",moon::authenticate)),account->false);
            var principal=gate.call(10000,scope->ModernAccountAuthentication.verify(verifier,ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD.toCharArray()));
            var first=gate.call(10000,scope->sun.shopCharacter(7,principal.accountForSource(7),3333,FIRST)).orElseThrow();
            var second=gate.call(10000,scope->moon.shopCharacter(42,principal.accountForSource(42),3333,SECOND)).orElseThrow();
            assertEquals(2000,first.offers().get(1).material().unitWeight());assertEquals(5_000_000_000L,second.offers().get(1).material().unitWeight());
            assertEquals("FIXTURE-USER",principal.accountForSource(42));
            assertEquals(ModernBackendExecutor.Failure.BACKEND,assertThrows(ModernBackendExecutor.BackendException.class,
                    ()->gate.call(10000,scope->moon.shopCharacter(42,"fixture-user",3333,SECOND))).failure());
            assertTrue(gate.call(10000,scope->sun.shopCharacter(7,principal.accountForSource(7),3334,FIRST)).isEmpty());
            long inventoryBefore=sourceQuantity(f,7);
            // Synthetic weight capacity is explicitly fixture-owned, not claimed as SQL combat qualification.
            var stub=ModernInventoryWeightTest.source(List.of());
            var basis=new ModernInventoryWeight.Basis(new ModernCharacterData.Snapshot(7,first.character(),stub.persisted(),List.of()),0,0,3150,1,-1);
            try(var shop=new ModernShopAuthority(1000,7,first.character(),first.canonicalAccount(),true,false,true)) {
                var initial=shop.apply(Action.READ,0,0,null,0,0,first,basis).snapshot();
                var bought=shop.apply(Action.BUY,initial.roundId(),initial.revision(),initial.token(),40318,2,first,basis).snapshot();
                assertEquals(800,bought.balance());assertEquals(4,bought.weight().view().suppliesWeight());
                assertThrows(SecurityException.class,()->shop.apply(Action.READ,0,0,null,0,0,second,basis));
                admin(f,"UPDATE "+table(FIRST,"lito_tournament_shop")+" SET enabled=0 WHERE item_id=40318");
                var disabled=gate.call(10000,scope->sun.shopCharacter(7,principal.accountForSource(7),3333,FIRST)).orElseThrow();
                assertEquals(1,disabled.offers().size());
                assertThrows(IllegalArgumentException.class,()->shop.capture(2,3,ModernShopAuthorityTest.deadline(),disabled,basis));assertFalse(shop.frozen());
                admin(f,"UPDATE "+table(FIRST,"lito_tournament_shop")+" SET enabled=1 WHERE item_id=40318");
                var reloaded=gate.call(10000,scope->sun.shopCharacter(7,principal.accountForSource(7),3333,FIRST)).orElseThrow();
                var captured=shop.capture(2,3,ModernShopAuthorityTest.deadline(),reloaded,basis);
                var stock=ModernSpellMaterials.State.initial(new ModernTargetHistory.World(1,2,3),new ModernRooms.Scope(7,3333,9,0),
                        new ModernTargetHistory.Participant(1,ModernParticipantId.human(7,3333)),captured);
                var next=stock.slow(1,1,1,ModernSpellMaterialsTest.profile(7,0),ModernSpellMaterialsTest.learned(7));
                assertEquals(2,stock.stocks().get(0).remaining());assertEquals(1,next.stocks().get(0).remaining());
                assertEquals(2,next.reweight(basis,List.of(),List.of(),bought.weight().units()).view().suppliesWeight());
                shop.terminal(2,3);assertTrue(shop.apply(Action.READ,0,0,null,0,0,reloaded,basis).snapshot().stacks().isEmpty());
            }
            assertEquals(inventoryBefore,sourceQuantity(f,7));
            for(String invalid:List.of("classname='custom'","stackable=0","invgfx=234","save_at_once=1")) {
                admin(f,"UPDATE "+table(FIRST,"etcitem")+" SET "+invalid+" WHERE item_id=40318");
                var unsupported=gate.call(10000,scope->sun.shopCharacter(7,principal.accountForSource(7),3333,FIRST)).orElseThrow();
                assertEquals(ModernShopCatalog.Support.UNSUPPORTED_TEMPLATE,unsupported.offers().get(1).support());
                admin(f,"UPDATE "+table(FIRST,"etcitem")+" SET classname='0',stackable=1,invgfx=235,save_at_once=0 WHERE item_id=40318");
            }
            var connections=new ModernSettlementConnections(f.config.sources().get(0).database(),1000,3000,1);
            try {
                try(var c=connections.open(ModernSettlementMysqlFixture.deadline(5));var statement=c.prepareStatement("UPDATE "+table(FIRST,"etcitem")+" SET weight=1 WHERE item_id=40318")) {
                    c.setReadOnly(false);statement.setQueryTimeout(2);
                    assertEquals(1142,assertThrows(SQLException.class,statement::executeUpdate).getErrorCode());
                }
            } finally {connections.close();assertTrue(connections.awaitTermination(5000));assertTrue(connections.isQuiescent());}
            assertArrayEquals(new long[]{0,0,0},f.mysql.counts());assertEquals(0,f.mysql.botCount());
            executor.close();assertTrue(executor.awaitTermination(5000));assertTrue(executor.isQuiescent());assertEquals(0,f.mysql.activeConnections());
            assertFalse(f.mysql.forcedTermination());f.successful();
            System.err.println("MATERIAL_SQL: two authenticated sources/catalogs; bounded SELECT read, wide source weight, internal buy/capture/debit/reweight, revoke/reload; no source inventory changes or public cast");
        }
    }
    static void admin(ModernQuestSqlFixture f,String sql)throws Exception {
        try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.prepareStatement(sql)){s.setQueryTimeout(3);s.execute();}
    }
    static long sourceQuantity(ModernQuestSqlFixture f,int source)throws Exception {
        try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.prepareStatement("SELECT SUM(count) FROM "+table(ModernQuestSqlFixture.source(source),"character_items")+" WHERE char_id=3333")) {
            s.setQueryTimeout(3);try(var r=s.executeQuery()){assertTrue(r.next());return r.getLong(1);}
        }
    }
}
