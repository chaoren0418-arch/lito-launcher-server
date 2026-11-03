package local.lito381.server;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Test-only owned process. No native Main, configuration, credentials, JDBC connection, or JUnit calls. */
public final class ModernMatchOutboxTestPeer {
    private ModernMatchOutboxTestPeer() {}
    static byte[] binding() { byte[] value = new byte[32]; for (int i = 0; i < value.length; i++) value[i] = (byte) i; return value; }
    static ModernMatchSettlement.Event event(long id) {
        return new ModernMatchSettlement.Event(new UUID(0, id), id, Long.MAX_VALUE, 9, 1, 514, 0x20_0000_0000_0001L,
                List.of(new ModernMatchSettlement.Participant(1, new TournamentPresence.CharacterKey(7, Long.MAX_VALUE), 0, ModernMatchSettlement.Verdict.WIN),
                        new ModernMatchSettlement.Participant(2, new TournamentPresence.CharacterKey(42, Long.MAX_VALUE), 1, ModernMatchSettlement.Verdict.LOSS)));
    }
    static ModernMatchSettlement.Event cooperativeEvent(long id) {
        var proof=new ModernTimeAttackSettlement.Proof(11,554,"11".repeat(32),"22".repeat(32),ModernTimeAttack.End.CLEARED,2,3,1,20_000,0,1000,1,0,5);
        return new ModernMatchSettlement.Event(new UUID(0,id),id,Long.MAX_VALUE,9,4,540,0x20_0000_0000_0001L,
                List.of(new ModernMatchSettlement.Participant(1,new TournamentPresence.CharacterKey(7,Long.MAX_VALUE),0,ModernMatchSettlement.Verdict.WIN)),proof);
    }
    static long deadline() { return System.nanoTime() + TimeUnit.SECONDS.toNanos(5); }
    private static void barrier(String marker) throws IOException {
        System.out.println(marker); System.out.flush();
        try { new CountDownLatch(1).await(10, TimeUnit.SECONDS); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        throw new IOException("owned outbox crash barrier ended without process termination");
    }
    public static void main(String[] args) {
        try {
            if (args.length != 2) throw new IOException("invalid peer arguments");
            boolean cooperative=args[0].endsWith("-timeattack");
            String mode = cooperative?args[0].substring(0,args[0].length()-11):args[0]; Path directory = Path.of(args[1]);
            var value=cooperative?cooperativeEvent(1):event(1);
            var operations = new ModernMatchOutbox.FileOps() {
                @Override public void force(FileChannel file) throws IOException {
                    ModernMatchOutbox.FileOps.super.force(file);
                    if (mode.equals("hold-force")) barrier("FORCED");
                }
                @Override public void move(Path from, Path to) throws IOException {
                    ModernMatchOutbox.FileOps.super.move(from, to);
                    if (mode.equals("hold-move")) barrier("MOVED");
                    if (mode.equals("hold-ack")) barrier("ACK_MOVED");
                }
            };
            try (var outbox = new ModernMatchOutbox(directory, binding(), operations)) {
                switch (mode) {
                    case "hold-force", "hold-move" -> outbox.record(value, deadline());
                    case "hold-record" -> { outbox.record(value, deadline()); barrier("RECORDED"); }
                    case "hold-ack" -> outbox.acknowledge(value, deadline());
                    case "read" -> {
                        var events = outbox.pending(deadline());
                        if (events.isEmpty()) System.out.println("EMPTY");
                        else if (events.equals(List.of(value))) System.out.println("PRESENT");
                        else throw new IOException("unexpected peer content");
                    }
                    case "busy" -> throw new IOException("unexpected writer admission");
                    default -> throw new IOException("unknown peer mode");
                }
            } catch (ModernMatchOutbox.OutboxException failure) {
                if (!mode.equals("busy") || failure.failure() != ModernMatchOutbox.Failure.BUSY) throw failure;
                System.out.println("BUSY");
            }
        } catch (Exception failure) {
            System.out.println("PEER_ERROR"); System.out.flush(); System.exit(2);
        }
    }
}
