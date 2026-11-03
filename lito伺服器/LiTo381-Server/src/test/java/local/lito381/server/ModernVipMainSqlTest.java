package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernVipWorldTlsTest.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Actual configured Main, SELECT-only JDBC sources, TLS and shared S32.
 * LOADED here is a protocol acknowledgement, not renderer/resource qualification. */
class ModernVipMainSqlTest {
    static final long ITEM=3_000_087L, BONUS=4_294_967_299L;
    static ModernQuestSqlFixture fixture(boolean both)throws Exception {
        var home=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(home!=null&&shared!=null);certificates();
        return ModernQuestSqlFixture.openFullVip(Path.of(home),Path.of(shared),ModernTlsTestMaterial.defaultDirectory(),both);
    }
    static void seed(ModernQuestSqlFixture f)throws Exception {
        ModernExpiredVipSqlFixture.seed(f);
        try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
            s.setQueryTimeout(3);String cat="`"+ModernQuestSqlFixture.CATALOG+"`.";
            s.execute("INSERT INTO "+cat+"etcitem(item_id,name_id,classname,item_type,use_type,material,invgfx,grdgfx,weight,stackable,limit_class) VALUES(300087,'$26','add.VIP','other','none','none',80,45,1,0,127)");
            s.execute("INSERT INTO "+cat+"`道具_自訂穿脫能力` (`道具編號`,`類型`,`血量`) VALUES(300087,1,"+BONUS+")");
            for(int id:List.of(7,42))s.execute("INSERT INTO `"+ModernQuestSqlFixture.source(id)+"`.character_items(id,char_id,item_id,count,is_equipped,is_id,bless) VALUES("+ITEM+",3333,300087,1,"+(id==7?0:1)+",1,0)");
        }
    }
    static byte[] loading(ModernPlayerHealingTlsTest.Client c,ModernTargetHistory.World w)throws Exception {
        long end=System.nanoTime()+10_000_000_000L;byte[] result;
        do {result=read(c,w);if(result[114]==2)return result;assertNotEquals(ModernWorldProtocol.Phase.ABORTED.ordinal(),result[148]);Thread.sleep(10);}while(System.nanoTime()<end);
        fail("Main VIP preparation did not publish schema10");return null;
    }
    /** Independent bounded section18 probe; full decoder tests live in Rust. */
    static long hpBonus(byte[] frame,int prefix) {
        var b=ByteBuffer.wrap(frame,prefix,frame.length-prefix).slice();assertEquals(0x4c544231,b.getInt());assertEquals(10,b.getShort());b.getShort();assertEquals(frame.length-prefix,b.getInt());assertEquals(18,b.getShort());b.getShort();
        ByteBuffer owner=null;
        for(int i=1;i<=18;i++){assertEquals(i,b.getShort());assertEquals(0,b.getShort());int n=b.getInt();assertTrue(n>=0&&n<=b.remaining());if(i==18){owner=b.slice();owner.limit(n);}b.position(b.position()+n);}
        assertFalse(b.hasRemaining());assertNotNull(owner);owner.position(96);int count=owner.getInt();assertTrue(count>=0&&count<=512);
        owner.position(owner.position()+count*ModernBattleVipWire.ITEM_LIMIT);
        return owner.getLong(owner.position()+ModernVipEquipment.Field.MAX_HP.ordinal()*8);
    }
    static void ping(ModernPlayerHealingTlsTest.Client c)throws Exception {int id=c.next();assertEquals(new ModernApplicationProtocol.Pong(id),c.old(new ModernApplicationProtocol.Ping(id)));}
    static void stopped(ModernServer server)throws Exception {server.close();assertTrue(server.awaitTermination(5000));}
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void repeatedJdbcQualificationRetainsTheSameCharacterAuthority()throws Exception {
        try(var f=fixture(true)) {
            seed(f);
            try(var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
                var sources=new TreeMap<Integer,ModernEquipmentRepository.Source>();var grants=new TreeMap<Integer,ModernAccountAuthentication.SourceBinding>();
                for(var config:f.config.sources()) {
                    var dao=new ModernJdbcBackend(config.database(),1000,5000);sources.put(config.id(),new ModernEquipmentRepository.Source(config,dao));
                    grants.put(config.id(),ModernAccountAuthentication.SourceBinding.direct("source-"+config.id(),dao::authenticate));
                }
                var verifier=ModernAccountAuthentication.repositories(grants,a->false);
                var principal=gate.call(10000,scope->ModernAccountAuthentication.verify(verifier,ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD.toCharArray()));
                var provider=ModernEquipmentRepository.fullVipJdbc(sources,false,false,true,false,false,false,false);
                for(int id:List.of(7,42)) {
                    var first=gate.call(5000,scope->provider.read(principal,id,3333));var second=gate.call(5000,scope->provider.read(principal,id,3333));
                    var key=new TournamentPresence.CharacterKey(id,3333);var binding=new ModernEquipmentReconciliation.ExpectedBinding(key,first.ownerBinding());
                    long now=System.currentTimeMillis();var state=ModernVipCharacterState.start(new ModernVipLifecycle.Actor(UUID.randomUUID(),key,1,0),first.source(),binding,Optional.empty(),now,0);
                    assertEquals(first.ownerBinding(),second.ownerBinding());state.requireSource(second.source(),binding,Optional.empty(),now);
                }
                executor.close();assertTrue(executor.awaitTermination(5000));
            }
            f.successful();
        }
    }
    @Test @Timeout(value=150,unit=TimeUnit.SECONDS)
    void configuredMainUsesFullJdbcNumbersAndSavesWithoutMutatingEitherImportedSource()throws Exception {
        try(var f=fixture(true)) {
            seed(f);var before=ModernSlowMainSqlTest.sourceRows(f);var mappings=f.config.worlds().load();
            assertTrue(f.config.sources().stream().allMatch(ModernServerConfig.SourceConfig::fullVip));
            assertTrue(mappings.get(new ModernWorldBinding.Key(1,514)).fullVipEnabled());assertFalse(mappings.get(new ModernWorldBinding.Key(1,509)).fullVipEnabled());
            var server=ModernMain.start(f.config);
            try {
                try(var a=new ModernPlayerHealingTlsTest.Client(server,ALL,7);var b=new ModernPlayerHealingTlsTest.Client(server,ALL,42)) {
                    a.login(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD);b.login(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD);
                    assertEquals(0,read(a,null)[114]);var room=ready(a,b);
                    var prepared=world(a,ModernBattleWorldProtocol.Action.PREPARE,room.summary().id(),room.summary().revision(),null,ModernWorldProtocol.ZERO_DIGEST);
                    assertEquals(0,prepared[112]);var w=identity(prepared);assertEquals(0,hpBonus(loading(a,w),168));assertEquals(BONUS,hpBonus(loading(b,w),168));
                    String digest=mappings.get(new ModernWorldBinding.Key(1,514)).binding().map().digest();
                    world(a,ModernBattleWorldProtocol.Action.LOADED,0,0,w,digest);var active=world(b,ModernBattleWorldProtocol.Action.LOADED,0,0,w,digest);assertEquals(ModernWorldProtocol.Phase.ACTIVE.ordinal(),active[148]);
                    var current=proof(read(a,w),168);
                    var inspected=a.exchange(new ModernApplicationProtocol.VipEquipmentRequest(ModernVipEquipmentProtocol.Request.inspect(a.next(),current)),83);
                    assertEquals(ModernVipEquipmentProtocol.Outcome.INSPECTED.ordinal(),inspected[157]);
                    var token=new ModernEquipmentOverrideStore.Fingerprint(Arrays.copyOfRange(inspected,176,208));
                    var saved=a.exchange(new ModernApplicationProtocol.VipEquipmentRequest(new ModernVipEquipmentProtocol.Request(a.next(),proof(inspected,236),ModernVipEquipmentProtocol.Action.SET,ITEM,true,token)),83);
                    assertEquals(ModernVipEquipmentProtocol.Outcome.APPLIED.ordinal(),saved[157]);assertEquals(BONUS,hpBonus(saved,236));assertEquals(1,proof(saved,236).versions().savedEquipmentRevision());
                    assertEquals(0,proof(read(b,w),168).versions().savedEquipmentRevision());
                    world(a,ModernBattleWorldProtocol.Action.LEAVE,0,0,w,ModernWorldProtocol.ZERO_DIGEST);world(b,ModernBattleWorldProtocol.Action.LEAVE,0,0,w,ModernWorldProtocol.ZERO_DIGEST);
                    room=readyAfterLeave(a,b);var next=world(a,ModernBattleWorldProtocol.Action.PREPARE,room.summary().id(),room.summary().revision(),null,ModernWorldProtocol.ZERO_DIGEST);var again=identity(next);assertNotEquals(w,again);
                    var retained=loading(a,again);assertEquals(BONUS,hpBonus(retained,168));assertEquals(1,proof(retained,168).versions().savedEquipmentRevision());
                    world(a,ModernBattleWorldProtocol.Action.LEAVE,0,0,again,ModernWorldProtocol.ZERO_DIGEST);world(b,ModernBattleWorldProtocol.Action.LEAVE,0,0,again,ModernWorldProtocol.ZERO_DIGEST);ping(a);ping(b);
                }
                ModernNpcServiceTest.await(()->server.snapshot().sessions()==0);assertEquals(0,server.snapshot().failed());
            }finally{stopped(server);}
            assertEquals(before,ModernSlowMainSqlTest.sourceRows(f));ModernExpiredVipSqlFixture.auditPreserved(f);
            assertArrayEquals(new long[]{0,0,0},f.mysql.counts());assertEquals(0,f.mysql.activeConnections());assertFalse(f.mysql.forcedTermination());
            System.err.println("VIP_MAIN_SQL_TLS_OK twoSources=true i64Hp=true inspectSetReentry=true importedRowsUnchanged=true rendererNotClaimed=true");f.successful();
        }
    }
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void unselectedSourceGetsExplicitUnavailableWithoutDisconnectOrDataFallback()throws Exception {
        try(var f=fixture(false)) {
            seed(f);var before=ModernSlowMainSqlTest.sourceRows(f);var server=ModernMain.start(f.config);
            try {
                try(var c=new ModernPlayerHealingTlsTest.Client(server,ALL,42)) {
                    c.login(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD);var reply=read(c,null);
                    assertEquals(ModernWorldProtocol.Status.UNAVAILABLE.ordinal(),reply[112]);assertEquals(ModernWorldProtocol.Reason.NO_BINDING.ordinal(),reply[113]);assertEquals(168,reply.length);ping(c);
                }
                ModernNpcServiceTest.await(()->server.snapshot().sessions()==0);assertEquals(0,server.snapshot().failed());
            }finally{stopped(server);}
            assertEquals(before,ModernSlowMainSqlTest.sourceRows(f));ModernExpiredVipSqlFixture.auditPreserved(f);assertEquals(0,f.mysql.activeConnections());assertFalse(f.mysql.forcedTermination());f.successful();
        }
    }
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void ordinaryProfileStillEntersOnlyTheOrdinaryMapOnTheSameConfiguredMain()throws Exception {
        try(var f=fixture(true)) {
            var before=ModernSlowMainSqlTest.sourceRows(f);var mappings=f.config.worlds().load();var server=ModernMain.start(f.config);
            try {
                try(var a=new ModernPlayerHealingTlsTest.Client(server,0,7);var b=new ModernPlayerHealingTlsTest.Client(server,0,42)) {
                    a.login(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD);b.login(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD);
                    var vipRoom=ready(a,b);var rejected=a.world(ModernBattleWorldProtocol.Action.PREPARE,vipRoom.summary().id(),vipRoom.summary().revision(),null,ModernWorldProtocol.ZERO_DIGEST);
                    assertEquals(ModernWorldProtocol.Status.UNAVAILABLE.ordinal(),rejected[112]);assertEquals(ModernWorldProtocol.Reason.NO_BINDING.ordinal(),rejected[113]);
                    for(var c:List.of(b,a)) {
                        var current=(ModernApplicationProtocol.RoomResult)c.old(new ModernApplicationProtocol.RoomRequest(c.next(),new ModernRooms.Command(c.scope,ModernRooms.Action.READ,0,0,0,null,"")));
                        ModernArmorProtectionMainSqlTest.room(c,ModernRooms.Action.LEAVE,current.result().view().joined(),0);
                    }
                    var created=(ModernApplicationProtocol.RoomResult)a.old(new ModernApplicationProtocol.RoomRequest(a.next(),new ModernRooms.Command(a.scope,ModernRooms.Action.CREATE,0,0,0,new ModernRooms.Draft("ordinary map","",2,1,509,16),"")));
                    assertEquals(ModernRooms.Status.OK,created.result().status());var room=created.result().view().joined();
                    room=ModernArmorProtectionMainSqlTest.room(b,ModernRooms.Action.JOIN,room,0);room=ModernArmorProtectionMainSqlTest.room(a,ModernRooms.Action.READY,room,1);room=ModernArmorProtectionMainSqlTest.room(b,ModernRooms.Action.READY,room,1);
                    var prepared=a.world(ModernBattleWorldProtocol.Action.PREPARE,room.summary().id(),room.summary().revision(),null,ModernWorldProtocol.ZERO_DIGEST);assertEquals(0,prepared[112]);var w=identity(prepared);
                    long end=System.nanoTime()+10_000_000_000L;byte[] loading;
                    do {loading=a.world(ModernBattleWorldProtocol.Action.READ,0,0,w,ModernWorldProtocol.ZERO_DIGEST);if(loading[114]==2)break;assertNotEquals(ModernWorldProtocol.Phase.ABORTED.ordinal(),loading[148]);Thread.sleep(10);}while(System.nanoTime()<end);
                    assertEquals(2,loading[114]);assertNotEquals(10,ByteBuffer.wrap(loading).getShort(172));
                    String digest=mappings.get(new ModernWorldBinding.Key(1,509)).binding().map().digest();a.world(ModernBattleWorldProtocol.Action.LOADED,0,0,w,digest);var active=b.world(ModernBattleWorldProtocol.Action.LOADED,0,0,w,digest);assertEquals(ModernWorldProtocol.Phase.ACTIVE.ordinal(),active[148]);
                    a.world(ModernBattleWorldProtocol.Action.LEAVE,0,0,w,ModernWorldProtocol.ZERO_DIGEST);b.world(ModernBattleWorldProtocol.Action.LEAVE,0,0,w,ModernWorldProtocol.ZERO_DIGEST);ping(a);ping(b);
                }
                ModernNpcServiceTest.await(()->server.snapshot().sessions()==0);assertEquals(0,server.snapshot().failed());
            }finally{stopped(server);}
            assertEquals(before,ModernSlowMainSqlTest.sourceRows(f));assertArrayEquals(new long[]{0,0,0},f.mysql.counts());assertEquals(0,f.mysql.activeConnections());assertFalse(f.mysql.forcedTermination());f.successful();
        }
    }
}
