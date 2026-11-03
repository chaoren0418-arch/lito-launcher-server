package local.lito381.server;

import java.sql.Connection;

/** Owned test MySQL only. Exact selected skill70, not permissive spell import. */
final class ModernNpcFireStormSqlFixture {
    private ModernNpcFireStormSqlFixture() {}
    static void install(Connection c)throws Exception {
        String catalog="`"+ModernQuestSqlFixture.CATALOG+"`.";
        sql(c,"ALTER TABLE "+catalog+"`skills` ADD id BIGINT NOT NULL DEFAULT 0,ADD sysmsgID_happen BIGINT NOT NULL DEFAULT 0,ADD sysmsgID_stop BIGINT NOT NULL DEFAULT 0,ADD sysmsgID_fail BIGINT NOT NULL DEFAULT 0");
        sql(c,"CREATE TABLE "+catalog+"`skills_item` (skill_id BIGINT PRIMARY KEY,itemids VARCHAR(64) NULL,counts VARCHAR(64) NULL) ENGINE=InnoDB");
        sql(c,"CREATE TABLE "+catalog+"`skills_buff_ability` (skill_id BIGINT PRIMARY KEY) ENGINE=InnoDB");
        sql(c,"INSERT INTO "+catalog+"`skills`(skill_id,id,nameid,name,target,skill_level,skill_number,mpConsume,reuseDelay,type,action_id,castgfx,target_to,damage_value,damage_dice,damage_dice_count,attr,area) VALUES(70,32,'$1975','火風暴','none',9,5,48,2000,64,18,1819,3,24,15,8,2,3)");
        sql(c,"INSERT INTO "+catalog+"`skills_item` VALUES(70,NULL,NULL)");
        for(int id:new int[]{7,42}) {
            String source="`"+ModernQuestSqlFixture.source(id)+"`.";
            sql(c,"INSERT INTO "+source+"`character_skills` VALUES(3333,70)");
            // Explicit test survival budget. Production RNG/time stay real;
            // four waves plus a structure fit in each source's mana budget.
            sql(c,"UPDATE "+source+"`characters` SET MaxHp=1000,CurHp=1000,MaxMp=300,CurMp=300,Lawful=500");
        }
        sql(c,"UPDATE "+catalog+"`npc` SET hp=1,mr=50,damage_reduction=9223372036854775807");
    }
    static void assertSourceUnchanged(ModernSettlementMysqlFixture mysql)throws Exception {
        try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(5))) {
            for(int id:new int[]{7,42}) {
                String source="`"+ModernQuestSqlFixture.source(id)+"`.";
                try(var s=c.prepareStatement("SELECT MaxHp,CurHp,MaxMp,CurMp,Lawful,(SELECT COUNT(*) FROM "+source+"character_skills WHERE char_obj_id=3333 AND skill_id=70) FROM "+source+"characters WHERE objid=3333")) {
                    s.setQueryTimeout(2);try(var r=s.executeQuery()) {
                        if(!r.next()||r.getLong(1)!=1000||r.getLong(2)!=1000||r.getLong(3)!=300||r.getLong(4)!=300||r.getLong(5)!=500||r.getLong(6)!=1||r.next())
                            throw new IllegalStateException("NPC70 imported source changed");
                    }
                }
            }
        }
    }
    private static void sql(Connection c,String statement)throws Exception {
        try(var s=c.createStatement()){s.setQueryTimeout(3);s.execute(statement);}
    }
}
