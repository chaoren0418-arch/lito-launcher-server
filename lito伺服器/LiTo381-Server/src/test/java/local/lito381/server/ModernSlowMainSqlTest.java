package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.*;
import org.junit.jupiter.api.*;

/** Real Main + authenticated TLS + newly owned SQL only.
 * A test-only canonical READ probe supplies intent proofs and checks aftermath.
 * It does not dispatch writes, replace repositories or constitute a Rust/UI test. */
class ModernSlowMainSqlTest {
    @Test @Timeout(value=150,unit=TimeUnit.SECONDS)
    void twoRealSourcesPurchaseLoadCastAndReturnWithoutWritingImportedCharacters()throws Exception {
        String installation=System.getenv(ModernSettlementMysqlFixture.ENV),shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(installation!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        var context=ModernTlsTestMaterial.clientContext(tls);
        try(var f=ModernQuestSqlFixture.openSlow(Path.of(installation),Path.of(shared),tls)) {
            var original=sourceRows(f);
            assertTrue(f.config.worlds().slowEnabled());assertFalse(f.config.worlds().hasteEnabled());
            var server=ModernMain.start(f.config);
            try {
                try(var old=new Client(server,context,46)) {
                    assertEquals(0,old.ack.capabilities()&ModernSessionProtocol.CAP_SLOW_DATA);
                }
                try(var sun=new Client(server,context,47);var moon=new Client(server,context,47)) {
                    assertNotEquals(0,sun.ack.capabilities()&ModernSessionProtocol.CAP_SLOW_DATA);
                    assertNotEquals(0,moon.ack.capabilities()&ModernSessionProtocol.CAP_SLOW_DATA);
                    sun.login();moon.login();sun.enter(7,1);moon.enter(42,1);
                    assertEquals(sun.scope.objectId(),moon.scope.objectId());
                    assertTrue(sun.skills().stream().anyMatch(s->s.skillId()==29&&s.support()==ModernSkills.Support.SLOW));
                    assertFalse(moon.skills().stream().anyMatch(s->s.skillId()==29));
                    for(var c:List.of(sun,moon)) {
                        var before=c.shop();assertTrue(before.materialsEnabled());
                        assertTrue(before.offers().stream().anyMatch(p->p.itemId()==40318&&p.support()==ModernShopCatalog.Support.MATERIAL));
                        var after=c.buy(before,4);
                        assertEquals(before.balance()-400,after.balance());
                        assertEquals(4,after.stacks().stream().filter(s->s.itemId()==40318).findFirst().orElseThrow().quantity());
                    }
                    var created=sun.room(ModernRooms.Action.CREATE,0,0,0,new ModernRooms.Draft("Slow Main fixture","",2,1,514,0));
                    var summary=created.view().joined().summary();
                    moon.room(ModernRooms.Action.JOIN,summary.id(),summary.revision(),0,null);
                    summary=moon.readRoom().view().joined().summary();
                    moon.room(ModernRooms.Action.TEAM,summary.id(),summary.revision(),1,null);
                    for(var c:List.of(sun,moon)) {
                        summary=c.readRoom().view().joined().summary();
                        c.room(ModernRooms.Action.READY,summary.id(),summary.revision(),1,null);
                    }
                    summary=sun.readRoom().view().joined().summary();assertTrue(summary.readySatisfied());
                    var preparing=sun.world(new ModernBattleWorldProtocol.Request(sun.scope,ModernBattleWorldProtocol.Action.PREPARE,
                            summary.id(),summary.revision(),0,0,ModernWorldProtocol.ZERO_DIGEST));
                    assertEquals(ModernBattleWorldProtocol.State.BEFORE_ENGINE.ordinal(),preparing[114]);
                    var b=ByteBuffer.wrap(preparing);var identity=new ModernTargetHistory.World(b.getLong(116),b.getLong(124),b.getLong(132));
                    sun.world=identity;moon.world=identity;
                    long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
                    ModernBattleWorldProtocol.Reply observed;
                    do {
                        sun.world(sun.worldRequest(ModernBattleWorldProtocol.Action.READ,ModernWorldProtocol.ZERO_DIGEST));
                        observed=probe(server,sun.scope);
                        assertNotEquals(ModernWorldProtocol.Phase.ABORTED,observed.lifecycle().phase());
                        assertTrue(System.nanoTime()<deadline,"source preparation deadline");
                        if(observed.view()==null)Thread.sleep(10);
                    }while(observed.view()==null);
                    String digest=observed.view().world().manifest().digest();
                    for(var c:List.of(sun,moon))c.world(c.worldRequest(ModernBattleWorldProtocol.Action.LOADED,digest));
                    assertEquals(ModernWorldProtocol.Phase.ACTIVE,probe(server,sun.scope).lifecycle().phase());
                    var before=probe(server,sun.scope).view();
                    var targetIndex=0;
                    for(int i=0;i<before.world().actors().size();i++)if(before.world().actors().get(i).pose().member().identity().sourceId()==42)targetIndex=i;
                    var q=intent(sun.next(),before,targetIndex,1);
                    var result=sun.raw(new ModernApplicationProtocol.SlowRequest(q));
                    checkSlowResult(result,q,ModernSlowCast.Reason.NONE);
                    var after=probe(server,sun.scope).view();
                    assertEquals(285,actor(after,7).vital().values().mp());
                    assertEquals(300,actor(after,42).vital().values().mp());
                    assertEquals(3,after.personal().materials().stocks().get(0).remaining());
                    assertEquals(1,after.personal().materials().uses());
                    assertEquals(4,probe(server,moon.scope).view().personal().materials().stocks().get(0).remaining());
                    // The production RNG decides hit/miss; both legally consume resources once.
                    var other=probe(server,moon.scope).view();
                    var denied=intent(moon.next(),other,0,1);
                    checkSlowResult(moon.raw(new ModernApplicationProtocol.SlowRequest(denied)),denied,ModernSlowCast.Reason.UNLEARNED);
                    assertEquals(0,probe(server,moon.scope).view().personal().materials().uses());
                    assertEquals(300,actor(probe(server,moon.scope).view(),42).vital().values().mp());
                    for(var c:List.of(sun,moon))c.world(c.worldRequest(ModernBattleWorldProtocol.Action.LEAVE,ModernWorldProtocol.ZERO_DIGEST));
                    for(var c:List.of(sun,moon)) {
                        summary=c.readRoom().view().joined().summary();
                        c.room(ModernRooms.Action.LEAVE,summary.id(),summary.revision(),0,null);
                        assertTrue(c.shop().stacks().isEmpty());
                        c.back();
                    }
                    // Both selected characters must be released before exchanging sources.
                    // A peer still owning the same source+objid is correctly rejected.
                    sun.enter(42,2);moon.enter(7,2);
                    assertFalse(sun.skills().stream().anyMatch(s->s.skillId()==29));
                    assertTrue(moon.skills().stream().anyMatch(s->s.skillId()==29&&s.support()==ModernSkills.Support.SLOW));
                    sun.back();moon.back();
                }
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
                while(server.snapshot().sessions()!=0){assertTrue(System.nanoTime()<deadline);Thread.sleep(10);}
                assertEquals(3,server.snapshot().accepted());assertEquals(0,server.snapshot().failed());assertFalse(server.listenerFailed());
            } finally {server.close();assertTrue(server.awaitTermination(5000));}
            assertEquals(original,sourceRows(f),"all imported stats, learned skills and inventory stay unchanged");
            assertArrayEquals(new long[]{0,0,0},f.mysql.counts());assertEquals(0,f.mysql.botCount());
            assertEquals(0,f.mysql.activeConnections());assertFalse(f.mysql.forcedTermination());f.successful();
        }
    }
    private static ModernBattleObservation.Actor actor(ModernBattleAudience.View view,int source) {
        return view.world().actors().stream().filter(a->a.pose().member().identity().sourceId()==source).findFirst().orElseThrow();
    }
    private static ModernSlowProtocol.Request intent(int id,ModernBattleAudience.View v,int target,long sequence) {
        var q=ModernSlowProtocolTest.intent(v,target,sequence);
        return new ModernSlowProtocol.Request(id,q.proof(),q.sequence(),q.equipmentRevision(),q.x(),q.y(),q.buffRevision(),q.target());
    }
    private static void checkSlowResult(byte[] wire,ModernSlowProtocol.Request q,ModernSlowCast.Reason reason)throws Exception {
        var b=ByteBuffer.wrap(wire);assertEquals(77,b.getShort(12));assertEquals(reason.ordinal(),Byte.toUnsignedInt(wire[184]));
        assertEquals(wire.length-216,b.getInt(212));assertEquals(6,b.getShort(220));
        byte[] request=ModernSlowProtocol.encodeRequest(q,47);
        assertArrayEquals(Arrays.copyOfRange(request,16,184),Arrays.copyOfRange(wire,16,184));
        assertEquals(0,wire[185]);assertEquals(0,wire[186]);assertEquals(0,wire[187]);
    }
    private static Object field(Object object,String name)throws Exception {
        var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);
    }
    private static ModernBattleWorldProtocol.Reply probe(ModernServer server,ModernRooms.Scope scope)throws Exception {
        var worlds=(ModernWorldService)field(server,"worlds");TournamentPresence.Owner owner=null;
        synchronized(field(worlds,"gate")) {
            for(var e:((Map<?,?>)field(worlds,"registrations")).entrySet())if(scope.equals(field(e.getValue(),"scope"))) {
                assertNull(owner,"scope owner must be unique");owner=(TournamentPresence.Owner)e.getKey();
            }
        }
        assertNotNull(owner);
        return worlds.battleWorld(owner,new ModernBattleWorldProtocol.Request(scope,ModernBattleWorldProtocol.Action.READ,0,0,0,0,ModernWorldProtocol.ZERO_DIGEST));
    }
    static List<List<String>> sourceRows(ModernQuestSqlFixture f)throws Exception {
        var rows=new ArrayList<List<String>>();
        try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
            for(int source:new int[]{7,42})for(var table:List.of("characters ORDER BY objid","character_items ORDER BY id","character_skills ORDER BY char_obj_id,skill_id")) {
                rows.add(List.of(Integer.toString(source),table));
                try(var s=c.prepareStatement("SELECT * FROM `"+ModernQuestSqlFixture.source(source)+"`."+table)) {
                    s.setQueryTimeout(3);try(var r=s.executeQuery()) {
                        int columns=r.getMetaData().getColumnCount();
                        while(r.next()){var row=new ArrayList<String>();for(int i=1;i<=columns;i++)row.add(r.getString(i));rows.add(Collections.unmodifiableList(row));}
                    }
                }
            }
        }
        return List.copyOf(rows);
    }
    private static final class Client implements AutoCloseable {
        final SSLSocket socket;final int version;final ModernSessionProtocol.Ack ack;
        int id=2;ModernRooms.Scope scope;ModernTargetHistory.World world;
        Client(ModernServer server,SSLContext context,int version)throws Exception {
            this.version=version;int port=server.address().getPort();
            socket=(SSLSocket)context.getSocketFactory().createSocket(new Socket("127.0.0.1",port),"localhost",port,true);
            try {
                var p=socket.getSSLParameters();p.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(p);socket.setSoTimeout(8000);socket.startHandshake();
                socket.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,version,version,ModernSessionProtocol.KNOWN_CAPABILITIES,7,1)));
                socket.getOutputStream().flush();ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream()));assertEquals(version,ack.version());
            }catch(Throwable failure){socket.close();throw failure;}
        }
        int next(){return id++;}
        byte[] raw(ModernApplicationProtocol.Message q)throws Exception {
            var wire=ModernApplicationProtocol.encode(q,version);
            try{socket.getOutputStream().write(wire);socket.getOutputStream().flush();}finally{Arrays.fill(wire,(byte)0);}
            var in=socket.getInputStream();byte[] header=in.readNBytes(8);assertEquals(8,header.length);
            var b=ByteBuffer.wrap(header);assertEquals(0x4c544d31,b.getInt());int size=b.getInt();assertTrue(size>=16&&size<=ModernBattleWorldProtocol.MAX_RESULT_BYTES);
            byte[] result=Arrays.copyOf(header,size),tail=in.readNBytes(size-8);assertEquals(size-8,tail.length);System.arraycopy(tail,0,result,8,tail.length);
            assertEquals(q.requestId(),ByteBuffer.wrap(result).getInt(8));return result;
        }
        ModernApplicationProtocol.Message exchange(ModernApplicationProtocol.Message q)throws Exception{return ModernApplicationProtocol.decode(raw(q),version);}
        void login()throws Exception {
            try(var auth=new ModernApplicationProtocol.Auth(next(),ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD.toCharArray())) {
                var r=(ModernApplicationProtocol.AuthResult)exchange(auth);assertEquals(0,r.status());assertEquals(List.of(7,42),r.sources().stream().map(ModernApplicationProtocol.Source::id).toList());
            }
        }
        void enter(int source,long epoch)throws Exception {
            var rows=(ModernApplicationProtocol.CharacterResult)exchange(new ModernApplicationProtocol.CharacterRequest(next(),source,epoch));
            assertEquals(0,rows.status());assertTrue(rows.characters().stream().anyMatch(c->c.objectId()==3333));
            assertEquals(1,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.CharacterSelection(next(),source,3333,epoch))).phase());
            scope=new ModernRooms.Scope(source,3333,epoch,0);
            assertEquals(2,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.ChannelEnter(next(),source,3333,epoch,0))).phase());
        }
        List<ModernSkills.Skill> skills()throws Exception {
            var r=(ModernApplicationProtocol.SkillsResult)exchange(new ModernApplicationProtocol.SkillsRequest(next(),new ModernSpellProtocol.Context(scope.sourceId(),scope.objectId(),scope.epoch())));
            assertEquals(ModernSpellProtocol.Status.OK,r.reply().status());return r.reply().skills();
        }
        ModernShopAuthority.Snapshot shop()throws Exception {
            var q=new ModernShopProtocol.Request(scope,ModernShopAuthority.Action.READ,0,0,0,0,new ModernEquipmentOverrideStore.Fingerprint(new byte[32]),0,0);
            var r=(ModernApplicationProtocol.ShopResult)exchange(new ModernApplicationProtocol.ShopRequest(next(),q));assertEquals(ModernShopAuthority.Status.OK,r.reply().outcome().status());return r.reply().outcome().snapshot();
        }
        ModernShopAuthority.Snapshot buy(ModernShopAuthority.Snapshot first,int count)throws Exception {
            var q=new ModernShopProtocol.Request(scope,ModernShopAuthority.Action.BUY,0,0,first.roundId(),first.revision(),first.token(),40318,count);
            var r=(ModernApplicationProtocol.ShopResult)exchange(new ModernApplicationProtocol.ShopRequest(next(),q));assertEquals(ModernShopAuthority.Status.OK,r.reply().outcome().status());return r.reply().outcome().snapshot();
        }
        ModernRooms.Result room(ModernRooms.Action action,long room,long revision,int value,ModernRooms.Draft draft)throws Exception {
            var r=(ModernApplicationProtocol.RoomResult)exchange(new ModernApplicationProtocol.RoomRequest(next(),new ModernRooms.Command(scope,action,room,revision,value,draft,"")));
            assertEquals(ModernRooms.Status.OK,r.result().status());return r.result();
        }
        ModernRooms.Result readRoom()throws Exception{return room(ModernRooms.Action.READ,0,0,0,null);}
        ModernBattleWorldProtocol.Request worldRequest(ModernBattleWorldProtocol.Action action,String digest){return new ModernBattleWorldProtocol.Request(scope,action,world.roomId(),0,world.matchId(),world.generation(),digest);}
        byte[] world(ModernBattleWorldProtocol.Request q)throws Exception {
            var wire=raw(new ModernApplicationProtocol.BattleWorldRequest(next(),q));assertEquals(67,ByteBuffer.wrap(wire).getShort(12));
            assertEquals(ModernWorldProtocol.Status.OK.ordinal(),Byte.toUnsignedInt(wire[112]));
            assertEquals(wire.length-168,ByteBuffer.wrap(wire).getInt(164));
            if(wire.length>168)assertEquals(6,ByteBuffer.wrap(wire).getShort(172));return wire;
        }
        void back()throws Exception {
            assertEquals(1,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.LobbyLeave(next(),scope.sourceId(),scope.objectId(),scope.epoch()))).phase());
            assertEquals(0,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.CharacterReturn(next(),scope.sourceId(),scope.objectId(),scope.epoch()))).phase());
        }
        public void close()throws IOException{socket.close();}
    }
}
