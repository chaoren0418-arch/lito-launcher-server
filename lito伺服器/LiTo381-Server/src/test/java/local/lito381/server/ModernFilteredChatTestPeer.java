package local.lito381.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Owned ephemeral production listener. Only source rows/auth/rate clock are synthetic.
 * No production database, moderation override, client package or native bin. */
public final class ModernFilteredChatTestPeer {
    public static void main(String[] args)throws Exception {
        if(args.length!=4||!args[0].equals("serve")||!Set.of("normal","reject","unavailable").contains(args[2]))
            throw new IllegalArgumentException("filtered peer arguments");
        int lifetime=Integer.parseInt(args[3]);if(lifetime<1000||lifetime>30000)throw new IllegalArgumentException("bounded peer lifetime");
        var backend=new ModernApplicationTestBackend("auth");var presence=new TournamentPresence();
        var sources=Map.of(7,new ModernSpeechAuthority.Source("太陽神",Set.of()),42,new ModernSpeechAuthority.Source("月亮",Set.of()));
        var rooms=new ModernRooms(Set.of(),Set.of(),Map.of(),new ModernChat(()->0),
                new ModernSpeechAuthority(args[2].equals("unavailable")?Map.of():sources));
        var limits=new ModernServer.Limits(4,2,600,4,2,4,2000,2000,2000,10000);
        var server=new ModernServer(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127,0,0,1}),0),
                ModernTlsTestMaterial.serverContext(Path.of(args[1]),false),limits,backend.verifier,backend.characters,presence,
                ModernCharacterDataRepository.unavailable(),null,null,rooms);
        try {
            System.out.println("READY "+server.address().getPort());System.out.flush();
            long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(lifetime);
            for(;;){var s=server.snapshot();if(s.accepted()==2&&s.sessions()==0)break;
                if(server.listenerFailed()||System.nanoTime()>=deadline)throw new IllegalStateException("filtered listener incomplete");
                Thread.sleep(10);}
            if(backend.authenticationCalls.get()!=4||server.snapshot().failed()!=(args[2].equals("reject")?1:0))
                throw new IllegalStateException("filtered listener flow failed");
        } finally {
            server.close();
            if(!server.awaitTermination(3000)||presence.ownerCount()!=0||presence.userCount()!=0||rooms.ownerCount()!=0
                    ||rooms.roomCount()!=0||rooms.memberCount()!=0||rooms.chatOwnerCount()!=0||rooms.directChatOwnerCount()!=0
                    ||rooms.filteredChatOwnerCount()!=0||rooms.speechOwnerCount()!=0)
                throw new IllegalStateException("filtered listener ownership did not retire");
        }
    }
}
