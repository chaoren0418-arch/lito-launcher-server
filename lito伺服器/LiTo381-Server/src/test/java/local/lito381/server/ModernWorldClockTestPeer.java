package local.lito381.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** GO-gated opt-in TLS/strict shared maps/isolated SQL records. Only Scene clocks;
 * no combat, wallet, source inventory, result writer, or native timeout verdict. */
public final class ModernWorldClockTestPeer {
    static final int CAPABILITIES=65903; // Descriptor + legacy no-equipment World policy (including GM implementation, not a grant).
    static final ModernWorldService.Limits GLOBAL_LIMITS=new ModernWorldService.Limits(4,2,2,3000,50,
            new ModernBattleEngine.Limits(30000,3_000_000_000L,45000));
    private ModernWorldClockTestPeer(){}
    static Properties properties(Path sharedRoot){
        if(sharedRoot==null||!sharedRoot.isAbsolute())throw new IllegalArgumentException("explicit absolute shared root required");
        var p=new Properties();p.setProperty("world.sharedRoot",sharedRoot.toString().replace('\\','/'));p.setProperty("world.bindings","1:514,1:509");
        binding(p,514,ModernWorldTestPeer.ANCHOR,137784,ModernWorldTestPeer.RAW_SHA,ModernWorldTestPeer.MANIFEST_SHA);
        binding(p,509,ModernMapRotationTestPeer.ANCHOR509,106355,
                "8580337a4ad3593d697a416062c4da063f8fd6c2ba311f0b694fcfc59710cb9d",
                "1178261c2d6a968251f2eadfb2029bd0febade95b7151c15872f355784df87f8");
        // Missing 514 loadTimeoutMillis is deliberate: it must retain global 30000.
        p.setProperty("world.1.514.countdownNanos","1000000000");p.setProperty("world.1.514.activeTimeoutMillis","4000");
        p.setProperty("world.1.509.loadTimeoutMillis","25000");p.setProperty("world.1.509.countdownNanos","2000000000");p.setProperty("world.1.509.activeTimeoutMillis","6000");
        return p;
    }
    private static void binding(Properties p,int map,ModernWorldMap.Cell anchor,int bytes,String raw,String canonical){
        String prefix="world.1."+map+".";p.setProperty(prefix+"physicalMap",Integer.toString(map));p.setProperty(prefix+"sectors","7fff8000");
        p.setProperty(prefix+"spawnX",Integer.toString(anchor.x()));p.setProperty(prefix+"spawnY",Integer.toString(anchor.y()));p.setProperty(prefix+"stepMillis","200");
        p.setProperty(prefix+"manifestSha256",canonical);p.setProperty(prefix+"sectorPins","7fff8000:"+bytes+":"+raw);
    }
    public static void main(String[] args){
        try{
            if(args.length!=4||!args[0].equals("serve")||!args[2].equals("clock-scenes"))throw new IllegalArgumentException();
            int lifetime=Integer.parseInt(args[3]);if(lifetime<5000||lifetime>180000)throw new IllegalArgumentException();
            String root=System.getenv("LITO_SHARED_RESOURCE_ROOT");if(root==null||root.isBlank())throw new IllegalArgumentException();
            // No resource loading, SQL descendant, listener, or worker starts until assigned to the caller's owned Job.
            if(!Arrays.equals(System.in.readNBytes(3),new byte[]{'G','O','\n'}))throw new IllegalArgumentException("missing process owner gate");
            try(var sql=ModernSettlementMysqlFixture.open(Path.of(System.getenv(ModernSettlementMysqlFixture.ENV)))){
                run(Path.of(args[1]),Path.of(root),lifetime,sql);sql.successful();
            }
        }catch(Exception failure){System.err.println("MODERN_WORLD_CLOCK_PEER_FAILED");System.exit(2);}
    }
    private static void run(Path tls,Path root,int lifetime,ModernSettlementMysqlFixture sql)throws Exception{
        // Exercise the same production static parser and physical resource loader as ModernMain.
        var configuration=ModernWorldConfiguration.parse(properties(root));var mappings=configuration.load();
        if(mappings.size()!=2||configuration.combatEnabled()||configuration.itemsEnabled())throw new IllegalStateException("invalid scene fixture");
        for(var mapping:mappings.values()){
            int map=mapping.binding().key().logicalMapId();var descriptor=ModernMatchDescriptor.of(mapping,GLOBAL_LIMITS,false);
            if(mapping.binding().map().mapId()!=map||descriptor.features()!=0||descriptor.settlementEnabled()
                    ||descriptor.preparationTimeoutMillis()!=3000||descriptor.loadTimeoutMillis()!=(map==514?30000:25000)
                    ||descriptor.countdownNanos()!=(map==514?1_000_000_000L:2_000_000_000L)
                    ||descriptor.activeTimeoutMillis()!=(map==514?4000:6000))throw new IllegalStateException("clock parser/fixture mismatch");
        }
        var presence=new TournamentPresence();var rooms=new ModernRooms(mappings.keySet());var backend=new ModernApplicationTestBackend("auth",80);
        var sunReads=new AtomicInteger();var moonReads=new AtomicInteger();var sunRecords=new AtomicInteger();var moonRecords=new AtomicInteger();
        ModernCharacterRepository characters=(principal,source)->{
            if(!principal.authorizesSource(source)||source!=7&&source!=42)throw new SecurityException();
            String expected=source==7?ModernApplicationTestBackend.SUN_ACCOUNT:ModernApplicationTestBackend.MOON_ACCOUNT;
            if(!expected.equals(principal.accountForSource(source)))throw new SecurityException();
            (source==7?sunReads:moonReads).incrementAndGet();return backend.characters.characters(principal,source);
        };
        var recordStore=ModernCharacterRecordRepository.jdbc(sql::openConnection,ModernSettlementMysqlFixture.DATABASE);
        ModernCharacterRecordRepository records=(principal,source,object)->{
            if(object!=3333||source!=7&&source!=42)throw new SecurityException();
            var result=recordStore.character(principal,source,object);(source==7?sunRecords:moonRecords).incrementAndGet();return result;
        };
        if(ModernSessionBoundary.Policy.world(false,false).supportedCapabilities()!=CAPABILITIES)throw new IllegalStateException("unexpected capability fixture");
        try(var worlds=new ModernWorldService(rooms,mappings,GLOBAL_LIMITS,bound->{throw new IllegalStateException("Scene cannot draw combat RNG");})){
            ModernServer server=null;
            try{
                server=new ModernServer(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127,0,0,1}),0),ModernTlsTestMaterial.serverContext(tls,false),
                        new ModernServer.Limits(4,2,600,4,2,4,2000,2000,2000,10000),backend.verifier,characters,presence,
                        ModernCharacterDataRepository.unavailable(),null,null,rooms,worlds,15,ModernSkillsRepository.unavailable(),records,null);
                System.out.println("READY "+server.address().getPort());System.out.flush();long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(lifetime);
                for(;;){var state=server.snapshot();if(state.accepted()==2&&state.sessions()==0)break;
                    if(state.accepted()>2||server.listenerFailed()||System.nanoTime()-deadline>=0)throw new IllegalStateException("bounded clock peer incomplete");Thread.sleep(10);}
                if(server.snapshot().failed()!=0||backend.authenticationCalls.get()!=4||sunReads.get()<3||moonReads.get()<3||sunRecords.get()<2||moonRecords.get()<2)
                    throw new IllegalStateException("clock/record source qualification not exercised");
            }finally{
                if(server!=null){server.close();if(!server.awaitTermination(5000))throw new IllegalStateException("clock TLS owner did not retire");}
                worlds.close();if(!worlds.awaitTermination(5000)||!worlds.isQuiescent()||worlds.registrationCount()!=0||presence.ownerCount()!=0||presence.userCount()!=0
                        ||rooms.ownerCount()!=0||rooms.roomCount()!=0||rooms.memberCount()!=0)throw new IllegalStateException("clock world owner did not retire");
            }
        }
        if(!Arrays.equals(new long[]{0,0,0},sql.counts())||sql.botCount()!=0||sql.activeConnections()!=0||sql.forcedTermination())
            throw new IllegalStateException("Scene timeout created a settlement or leaked SQL owner");
    }
}
