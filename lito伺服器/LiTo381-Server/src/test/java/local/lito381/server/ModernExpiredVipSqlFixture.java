package local.lito381.server;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** Only accepts a newly owned test fixture, never native configuration/credentials. */
final class ModernExpiredVipSqlFixture {
    static final long VIP_OBJECT=3_000_086L;
    private ModernExpiredVipSqlFixture(){}
    static void seed(ModernQuestSqlFixture fixture)throws Exception {
        try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
            String catalog="`"+ModernQuestSqlFixture.CATALOG+"`.";
            for(String name:List.of("類型","狀態圖編號","變身編號","魅力","防禦","血量","魔量","傷害減免","魔法傷害減免","經驗獲取率","金幣獲取率"))
                execute(c,"ALTER TABLE "+catalog+"`道具_自訂穿脫能力` ADD COLUMN `"+name+"` BIGINT NOT NULL DEFAULT 0");
            execute(c,"CREATE TABLE "+catalog+"`poly_變身賦予能力` (gfxId BIGINT PRIMARY KEY,deadExp BIGINT NOT NULL DEFAULT 0,cancellation BIGINT NOT NULL DEFAULT 0,"
                    +String.join(",",List.of("addStr","addDex","addCon","addInt","addWis","addCha","addAc","addMaxHp","addMaxMp","addHpr","addMpr",
                    "addDmg","addBowDmg","addHit","addBowHit","reduction_dmg","reduction_magic_dmg","addMr","addSp","addFire","addWind","addEarth","addWater","addExpRate")
                    .stream().map(s->"`"+s+"` BIGINT NOT NULL DEFAULT 0").toList())+") ENGINE=InnoDB");
            execute(c,"INSERT INTO "+catalog+"`poly_變身賦予能力` (gfxId,addStr,addDex,addCon,addInt,addWis,addCha) VALUES(5641,22,22,22,22,22,22)");
            execute(c,"INSERT INTO "+catalog+"`道具_自訂穿脫能力` (`道具編號`,`狀態圖編號`,`變身編號`,`金幣獲取率`) VALUES(300086,505,5641,100)");
            execute(c,"INSERT INTO "+catalog+"`etcitem` (item_id,name_id,classname,item_type,use_type,material,invgfx,grdgfx,weight,stackable,limit_class)"
                    +" VALUES(300086,'$26','add.VIP','other','none','none',80,45,1,0,127)");
            execute(c,"INSERT INTO "+catalog+"`道具_物品時間限制` VALUES(300086,720)");
            for(int id:List.of(7,42)) {
                String source="`"+ModernQuestSqlFixture.source(id)+"`.";
                execute(c,"INSERT INTO "+source+"character_items(id,char_id,item_id,count,is_equipped,is_id,bless) VALUES("+VIP_OBJECT+",3333,300086,1,1,1,0)");
                execute(c,"INSERT INTO "+source+"character_items_time(itemr_obj_id,usertime) VALUES("+VIP_OBJECT+",'2000-01-01 00:00:00')");
            }
        }
        verifyQualified(fixture);
        System.err.println("EXPIRED_VIP_SQL_READY sources=2 rawEquipped=2 effectiveEquipped=0 catalog=300086 poly=5641 savedAcPreserved=true");
    }
    static void verifyQualified(ModernQuestSqlFixture fixture)throws Exception {
        try(var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
            try {
                for(var source:fixture.config.sources()) {
                    var dao=new ModernJdbcBackend(source.database(),1000,5000);
                    var data=gate.call(5000,scope->{
                        // The all-mode fixture deliberately has distinct canonical
                        // account casing per source. Resolve it through the same
                        // SELECT-only authentication used by the actual player.
                        var auth=dao.authenticate(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD);
                        if(!auth.accepted())throw new IllegalStateException("VIP fixture authentication");
                        return dao.physicalCharacter(source.id(),auth.account(),3333,source.catalogDatabase(),false,false,true,false).orElseThrow();
                    });
                    var qualified=ModernPhysicalCombat.qualify(data,ModernCombatProfileTest.binding(data.input().equipment()),Optional.empty(),System.currentTimeMillis());
                    if(!qualified.supported()||!qualified.regen().supported()||data.vipCatalog()==null
                                ||qualified.profile().effective().armorClass()!=data.input().equipment().source().persisted().armorClass()
                                ||qualified.profile().effective().strength()!=data.input().equipment().source().persisted().strength()
                                ||qualified.profile().equipped().stream().filter(i->i.objectId()==VIP_OBJECT).findFirst().orElseThrow().equipped())
                        throw new IllegalStateException("expired VIP fixture qualification");
                }
            } finally {executor.close();if(!executor.awaitTermination(5000))throw new IllegalStateException("VIP fixture worker retained");}
        }
    }
    static void auditPreserved(ModernQuestSqlFixture fixture)throws Exception {
        try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
            for(int id:List.of(7,42))try(var s=c.prepareStatement("SELECT i.item_id,i.count,i.is_equipped,i.remaining_time,t.usertime FROM `"
                    +ModernQuestSqlFixture.source(id)+"`.character_items i JOIN `"+ModernQuestSqlFixture.source(id)
                    +"`.character_items_time t ON t.itemr_obj_id=i.id WHERE i.id="+VIP_OBJECT+" AND i.char_id=3333")) {
                s.setQueryTimeout(3);try(var rows=s.executeQuery()) {
                    if(!rows.next()||rows.getLong(1)!=300086||rows.getLong(2)!=1||rows.getLong(3)!=1||rows.getLong(4)!=0
                            ||!rows.getString(5).startsWith("2000-01-01 00:00:00")||rows.next())throw new IllegalStateException("source VIP mutated");
                }
            }
        }
        System.err.println("EXPIRED_VIP_SQL_PRESERVED sources=2 rawFlagsUnchanged=true deadlinesUnchanged=true");
    }
    private static void execute(Connection c,String sql)throws Exception {
        try(var s=c.createStatement()){s.setQueryTimeout(3);s.execute(sql);}
    }
}
