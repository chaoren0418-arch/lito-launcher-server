package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Production Main + authenticated TLS + owned SQL + shared map. This small
 * frame probe does not qualify SPR assets or replace the Rust full decoder. */
class ModernArmorProtectionMainSqlTest {
    static ModernRooms.RoomSnapshot room(ModernPlayerHealingTlsTest.Client c,ModernRooms.Action action,ModernRooms.RoomSnapshot room,int value)throws Exception {
        var result=(ModernApplicationProtocol.RoomResult)c.old(new ModernApplicationProtocol.RoomRequest(c.next(),new ModernRooms.Command(c.scope,action,room.summary().id(),room.summary().revision(),value,null,"")));
        assertEquals(ModernRooms.Status.OK,result.result().status());return result.result().view().joined();
    }
    static byte[] read(ModernPlayerHealingTlsTest.Client c,ModernTargetHistory.World world)throws Exception {
        return c.world(ModernBattleWorldProtocol.Action.READ,0,0,world,ModernWorldProtocol.ZERO_DIGEST);
    }
    record Armor(long entity,long life,long sequence,long revision,long until,List<Long> graphics) {}
    static List<Armor> armor(byte[] frame,int prefix) {
        var sections=ModernBattleArmorWireTest.sections(Arrays.copyOfRange(frame,prefix,frame.length));
        var b=ByteBuffer.wrap(sections.get(16));var result=new ArrayList<Armor>();int count=b.getInt();assertEquals(2,count);
        for(int i=0;i<count;i++) {
            long entity=b.getLong();assertEquals(1,b.get());long actual=b.getLong(),life=b.getLong(),sequence=b.getLong(),revision=b.getLong();
            b.getLong();long until=b.getLong();int effects=b.getInt();assertTrue(effects>=0&&effects<=1024);var graphics=new ArrayList<Long>();
            for(int n=0;n<effects;n++){graphics.add(b.getLong());b.getLong();b.get();b.getLong();}
            assertEquals(entity,actual);result.add(new Armor(entity,life,sequence,revision,until,List.copyOf(graphics)));
        }
        assertFalse(b.hasRemaining());return List.copyOf(result);
    }
    @Test @Timeout(value=150,unit=TimeUnit.SECONDS)
    void mainRoutesBothSqlSourcesThroughArmorWorldOverTlsWithoutChangingImportedRows()throws Exception {
        var home=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(home!=null&&shared!=null);ModernPlayerHealingTlsTest.certificates();
        try(var fixture=ModernQuestSqlFixture.openArmorProtection(Path.of(home),Path.of(shared),ModernTlsTestMaterial.defaultDirectory())) {
            seed(fixture);var original=ModernArmorProcsSqlTest.snapshot(fixture);var ordinary=ModernSlowMainSqlTest.sourceRows(fixture);
            assertTrue(fixture.config.worlds().armorProtectionEnabled());var mappings=fixture.config.worlds().load();
            assertTrue(mappings.get(new ModernWorldBinding.Key(1,514)).armorProtectionEnabled());assertFalse(mappings.get(new ModernWorldBinding.Key(1,509)).armorProtectionEnabled());
            var server=ModernMain.start(fixture.config);
            try {
                try(var a=new ModernPlayerHealingTlsTest.Client(server,7,7);var b=new ModernPlayerHealingTlsTest.Client(server,7,42)) {
                    a.login(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD);b.login(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD);
                    var room=a.room();room=room(b,ModernRooms.Action.JOIN,room,0);room=room(a,ModernRooms.Action.READY,room,1);room=room(b,ModernRooms.Action.READY,room,1);
                    assertTrue(room.summary().readySatisfied());
                    var prepared=a.world(ModernBattleWorldProtocol.Action.PREPARE,room.summary().id(),room.summary().revision(),null,ModernWorldProtocol.ZERO_DIGEST);
                    assertEquals(0,prepared[112]);var world=new ModernTargetHistory.World(ByteBuffer.wrap(prepared).getLong(116),ByteBuffer.wrap(prepared).getLong(124),ByteBuffer.wrap(prepared).getLong(132));
                    byte[] loading=null;long deadline=System.nanoTime()+10_000_000_000L;
                    while(System.nanoTime()<deadline){loading=read(a,world);if(loading.length>168)break;assertNotEquals(5,Byte.toUnsignedInt(loading[148]));Thread.sleep(10);}
                    assertNotNull(loading);assertTrue(loading.length>168,"qualified armor preparation publishes schema8");assertTrue(armor(loading,168).stream().allMatch(s->s.sequence()==0));
                    String digest=mappings.get(new ModernWorldBinding.Key(1,514)).binding().map().digest();
                    a.world(ModernBattleWorldProtocol.Action.LOADED,0,0,world,digest);b.world(ModernBattleWorldProtocol.Action.LOADED,0,0,world,digest);
                    var active=ModernPlayerHealingTlsTest.Probe.from(read(a,world),168,8);assertEquals(ModernBattleEngine.Phase.ACTIVE.ordinal(),active.phase());
                    Armor protection=null;
                    for(int sequence=1;sequence<=20;sequence++) {
                        if(sequence>1)Thread.sleep(800); // Real Main hit roll and attack cooldown remain authoritative.
                        active=ModernPlayerHealingTlsTest.Probe.from(read(a,world),168,8);
                        var target=armor(read(b,world),168).get(1);
                        var attack=new ModernApplicationProtocol.CombatRequest(a.next(),new ModernCombatProtocol.Request(a.scope,ModernCombatProtocol.Action.ATTACK,world.matchId(),world.generation(),sequence,target.entity()));
                        var proof=new ModernBattleCommandLife.Proof(a.scope,world,active.entity(),active.life()+1,active.revision());
                        var request=new ModernApplicationProtocol.BattleOperationRequest(attack.requestId(),new ModernBattleActionProtocol.Request(proof,new ModernBattleActionProtocol.Target(ModernBattleActionProtocol.Kind.PLAYER,target.entity(),target.life()+1),attack));
                        a.exchange(request,65);protection=armor(read(b,world),168).get(1);
                        if(protection.until()>0)break;
                    }
                    assertNotNull(protection);assertTrue(protection.sequence()>0);assertTrue(protection.until()>0);assertEquals(List.of(11101L),protection.graphics());
                    Thread.sleep(800); // Healing shares the server action cooldown with the preceding attack.
                    active=ModernPlayerHealingTlsTest.Probe.from(read(a,world),168,8);
                    var healed=a.exchange(new ModernApplicationProtocol.PlayerHealingRequest(active.self(a.next(),a.scope,1)),79);
                    assertEquals(ModernPlayerHealingCast.Reason.NONE.ordinal(),healed[160]);assertEquals(protection,armor(healed,192).get(1));
                    var echo=b.exchange(new ModernApplicationProtocol.BattleReadRequest(b.next(),new ModernBattleReadProtocol.Request(b.scope,world)),61);
                    assertEquals(protection,armor(echo,72).get(1));
                    assertEquals(168,a.world(ModernBattleWorldProtocol.Action.LEAVE,0,0,world,ModernWorldProtocol.ZERO_DIGEST).length);
                    assertEquals(168,b.world(ModernBattleWorldProtocol.Action.LEAVE,0,0,world,ModernWorldProtocol.ZERO_DIGEST).length);
                    int id=a.next();assertEquals(new ModernApplicationProtocol.Pong(id),a.old(new ModernApplicationProtocol.Ping(id)));
                }
                ModernNpcServiceTest.await(()->server.snapshot().sessions()==0);assertEquals(0,server.snapshot().failed());
            }finally{server.close();assertTrue(server.awaitTermination(5000));}
            assertEquals(original,ModernArmorProcsSqlTest.snapshot(fixture));assertEquals(ordinary,ModernSlowMainSqlTest.sourceRows(fixture));
            assertArrayEquals(new long[]{0,0,0},fixture.mysql.counts());assertEquals(0,fixture.mysql.activeConnections());assertFalse(fixture.mysql.forcedTermination());
            System.err.println("ARMOR_MAIN_SQL_TLS_OK sources=2 sharedMap=true loginRoomPrepareLoadAttackProtectionHealReadLeave=true importedRowsUnchanged=true noRustSprOrUiClaim=true");
            fixture.successful();
        }
    }
    static void seed(ModernQuestSqlFixture f)throws Exception {
        try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
            s.setQueryTimeout(3);String p="`"+ModernQuestSqlFixture.CATALOG+"`.";
            for(String kind:List.of("指定","類型")) {
                s.execute("CREATE TABLE "+p+"`裝備_armor_enchant_"+kind+"` LIKE "+p+"`裝備_armor_bless_"+kind+"`");
                s.execute("ALTER TABLE "+p+"`裝備_armor_enchant_"+kind+"` CHANGE bless_id "+(kind.equals("指定")?"enchant_level":"over_enchant")+" BIGINT NOT NULL DEFAULT 0");
            }
            for(String column:ModernArmorProcs.COLUMNS)if(!column.equals("id")&&!column.equals("item_id"))s.execute("ALTER TABLE "+p+"`裝備_armor_魔法` ADD COLUMN `"+column+"` BIGINT NOT NULL DEFAULT 0");
            s.execute("INSERT INTO "+p+"armor(item_id,name_id,type,invgfx,grdgfx,weight,use_royal,use_knight,use_elf,use_mage,use_darkelf,use_dragonknight,use_illusionist) VALUES(88889,'$1','armor',1,1,1,1,1,1,1,1,1,1)");
            // Deterministic proc probability belongs to this disposable fixture only.
            s.execute("INSERT INTO "+p+"`裝備_armor_魔法`(id,item_id,enchant_level,proc_group,on_hurt_chance,trigger_skill,skill_time_sec,skill_target,gfx_id,gfx_mode) VALUES(50,88889,-1,88,100,68,3,1,11101,1),(51,88889,5,88,100,68,6,1,11101,1)");
            s.execute("INSERT INTO "+p+"skills(skill_id,skill_level,skill_number,mpConsume,itemConsumeId,itemConsumeCount,reuseDelay,buffDuration,target,target_to,type,ranged,id,action_id,castgfx,sysmsgID_happen,sysmsgID_stop,sysmsgID_fail) VALUES(68,9,3,30,40318,2,1500,32,'buff',4,2,-1,8,19,228,314,315,280)");
            s.execute("INSERT INTO "+p+"skills_item VALUES(68,NULL,NULL)");
            for(int id:List.of(7,42)) {
                s.execute("INSERT INTO `"+ModernQuestSqlFixture.source(id)+"`.character_items(id,char_id,item_id,count,is_equipped,enchantlvl,is_id,bless) VALUES(3000089,3333,88889,1,1,"+(id==7?0:5)+",1,0)");
                s.execute("UPDATE `"+ModernQuestSqlFixture.source(id)+"`.characters SET MaxHp=10000,CurHp=10000");
            }
        }
    }
}
