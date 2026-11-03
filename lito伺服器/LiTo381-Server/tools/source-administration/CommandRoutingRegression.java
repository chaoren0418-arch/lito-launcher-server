package local.lito381.server;
import java.nio.file.*;
import java.util.*;

/** Reads real command definitions via the runtime repository; never writes SQL. */
public class CommandRoutingRegression {
    static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception {
        var config=ServerConfig.load(Path.of(args[0]),"standard");
        var settings=SharedDatabase.properties(config.source());
        String controls=settings.getProperty("database.lito.database");
        String resources=settings.getProperty("database.static.database");
        try(var sources=SharedDatabase.open(config)) {
            int checked=0;
            for(var e:List.copyOf(SharedDatabase.ROUTES.entrySet())) {
                var repository=e.getKey();var route=e.getValue();
                check(SharedDatabase.resourceTable(repository,"commands",resources).equals("`"+controls+"`.`commands`"),"LiTo command route");
                check(SharedDatabase.resourceTable(repository,"skills",resources).equals("`"+resources+"`.`skills`"),"shared skills route preserved");
                var custom=new SharedDatabase.Route(route.name,route.database,"custom_lito_config",settings);
                SharedDatabase.ROUTES.put(repository,custom);
                try {
                    check(SharedDatabase.resourceTable(repository,"commands",resources).equals("`custom_lito_config`.`commands`"),"custom control database");
                }finally{SharedDatabase.ROUTES.put(repository,route);}
                // Do not depend on the third account database pending user provisioning.
                if(!route.database.equals(config.legacySourceDatabases().get(0)))continue;
                for(var command:Map.of("item","L1CreateItem","spawn","L1SpawnCmd","de","L1SpawnDe").entrySet()) {
                    var definition=repository.gmCommand(command.getKey());
                    check(definition!=null&&definition.executor().equals(command.getValue())
                          &&definition.requiredAccessLevel()==200&&!definition.systemCommand(),"live LiTo command "+command.getKey());
                    checked++;
                }
                // 'echo' exists in the shared static table; it must not leak into LiTo.
                check(repository.gmCommand("echo")==null,"no fallback to shared command table");
            }
            check(checked==3,"three real command queries");
        }
        System.out.println("PASS: live LiTo commands read through runtime SQL account; all source routes; custom database; no shared-table fallback; no SQL writes.");
    }
}
