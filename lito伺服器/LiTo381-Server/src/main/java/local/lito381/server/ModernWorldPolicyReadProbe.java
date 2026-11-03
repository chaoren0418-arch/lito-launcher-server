package local.lito381.server;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.function.Function;

/** Explicit SELECT-only administrator workflow. Never opens a listener, edits config, publishes SQL or awards results. */
public final class ModernWorldPolicyReadProbe {
    private ModernWorldPolicyReadProbe() {}
    public static void main(String[] args) {
        if(run(args,System::getenv,System.out,System.err)!=0)System.exit(2);
    }
    static int run(String[] args,Function<String,String> environment,PrintStream out,PrintStream error) {
        try {
            boolean candidate=candidate(args);
            try(var config=ModernServerConfig.load(Path.of(args[0]),environment)) {
                var result=ModernMain.loadWorldPolicy(config,candidate);
                out.println("world_policy_qualified=true candidate="+candidate+" bindings="+result.mappings().size()+" sql_writes=0 listener_started=false");
                if(candidate)out.println("worldPolicy.contentSha256="+result.snapshot().pin().contentSha256());
                out.flush();return 0;
            }
        }catch(Exception failure) {
            if(failure instanceof InterruptedException)Thread.currentThread().interrupt();
            error.println("MODERN_WORLD_POLICY_READ_FAILED");error.flush();return 2;
        }
    }
    static boolean candidate(String[] args) {
        if(args==null || args.length<1 || args.length>2 || args[0]==null || !Path.of(args[0]).isAbsolute()
                || args.length==2&&!"--candidate".equals(args[1]))throw new IllegalArgumentException("invalid world policy probe arguments");
        return args.length==2;
    }
}
