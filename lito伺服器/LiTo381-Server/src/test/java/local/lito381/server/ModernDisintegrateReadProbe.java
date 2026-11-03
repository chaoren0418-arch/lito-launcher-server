package local.lito381.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/** Test classpath only. Authenticated source/roster and skill77 SELECT proof;
 * the exact statement allowlist rejects writes even if DB credentials can write.
 * Never opens a listener/store, grants a skill or executes a cast.
 */
public final class ModernDisintegrateReadProbe {
    private ModernDisintegrateReadProbe(){}
    static Connection observe(Connection connection,String catalog) {
        var permitted=Set.of("SELECT DATABASE()",ModernJdbcBackend.AUTH_SQL,ModernJdbcBackend.CHARACTERS_SQL,
                "SELECT `effect_skill_id` FROM `"+catalog+"`.`skills` LIMIT 0",ModernDisintegrateCatalog.sql(catalog,true),
                ModernDisintegrateCatalog.sql(catalog,false),ModernDisintegrateCatalog.extrasSql(catalog));
        return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->{
            String name=m.getName();
            if(name.equals("createStatement")||name.equals("prepareCall")||name.equals("commit")||name.equals("unwrap")
                    ||name.equals("setReadOnly")&&!Boolean.TRUE.equals(a[0]))throw new SQLException("unqualified probe operation");
            if(name.equals("prepareStatement")&&(a.length!=1||!(a[0] instanceof String sql)||!permitted.contains(sql)))throw new SQLException("unqualified probe statement");
            try {
                Object result=m.invoke(connection,a);
                return name.equals("prepareStatement")?observe((PreparedStatement)result):result;
            }catch(InvocationTargetException e){throw e.getCause();}
        });
    }
    private static PreparedStatement observe(PreparedStatement statement) {
        return (PreparedStatement)Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class<?>[]{PreparedStatement.class},(p,m,a)->{
            String name=m.getName();
            if(name.startsWith("execute")&&(!name.equals("executeQuery")||a!=null&&a.length!=0)
                    ||name.equals("addBatch")||name.equals("unwrap")||name.equals("getConnection"))throw new SQLException("unqualified probe execution");
            try{return m.invoke(statement,a);}catch(InvocationTargetException e){throw e.getCause();}
        });
    }
    public static void main(String[] args) {
        int exit=2;String stage="CONFIGURATION";char[] password=null;
        try {
            if(args.length!=1)throw new IllegalArgumentException();Path file=Path.of(args[0]);
            String account=System.getenv("LITO_MODERN_PROBE_ACCOUNT"),secret=System.getenv("LITO_MODERN_PROBE_PASSWORD");
            if(!ModernCharacterDataReadProbe.boundedText(account,64,false)||!ModernCharacterDataReadProbe.boundedText(secret,256,true))throw new IllegalArgumentException();
            password=secret.toCharArray();
            try(var settings=ModernDatabaseReadProbe.load(file);
                    var config=ModernDatabaseReadProbe.modernConfig(settings,ModernCharacterDataReadProbe.catalogDatabase(file));
                    var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
                try {
                    int characters=0;int[] counts=new int[ModernDisintegrateCatalog.Status.values().length];
                    for(var source:config.sources()) {
                        var audit=new ModernEquipmentReadProbe.Audit();
                        var dao=new ModernJdbcBackend(source.database(),config.dbConnectMillis(),config.dbNetworkMillis(),
                                (url,props)->ModernEquipmentReadProbe.inspectOwned(observe(new com.mysql.cj.jdbc.Driver().connect(url,props),source.catalogDatabase()),
                                        ModernBackendScope.current(),source.database().database(),source.catalogDatabase(),config.dbNetworkMillis(),null,audit,null));
                        var verifier=ModernAccountAuthentication.repositories(Map.of(source.id(),ModernAccountAuthentication.SourceBinding.direct(source.name(),dao::authenticate)),id->false);
                        stage="AUTHENTICATION";var principal=ModernEquipmentRepositoryReadProbe.authenticate(gate,verifier,account,password);
                        if(!principal.authorizesSource(source.id()))throw new SecurityException();
                        stage="ROSTER";var roster=gate.call(5000,scope->dao.characters(principal.accountForSource(source.id())));
                        var repository=ModernDisintegrateRepository.jdbc(Map.of(source.id(),new ModernDisintegrateRepository.Source(dao,source.catalogDatabase())));
                        for(var character:roster) {
                            stage="SKILL77_QUALIFICATION";
                            var snapshot=gate.call(5000,scope->repository.character(principal,source.id(),character.objectId())).orElseThrow();
                            if(!snapshot.key().equals(new TournamentPresence.CharacterKey(source.id(),character.objectId())))throw new IllegalStateException();
                            characters++;counts[snapshot.status().ordinal()]++;
                        }
                        stage="ROSTER_RECHECK";if(!roster.equals(gate.call(5000,scope->dao.characters(principal.accountForSource(source.id())))))throw new IllegalStateException();
                    }
                    if(characters==0)throw new IllegalStateException();stage="RETIREMENT";
                    executor.close();if(!executor.awaitTermination(5000))throw new IllegalStateException();
                    System.out.println("{\"phase\":\"production-disintegrate-source\",\"status\":\"OK\",\"sources\":"+config.sources().size()
                            +",\"characters\":"+characters+",\"qualified\":"+counts[ModernDisintegrateCatalog.Status.QUALIFIED.ordinal()]
                            +",\"unlearned\":"+counts[ModernDisintegrateCatalog.Status.UNLEARNED.ordinal()]+",\"unsupported\":"+counts[ModernDisintegrateCatalog.Status.UNSUPPORTED_TEMPLATE.ordinal()]
                            +",\"exact_select_allowlist\":true,\"source_route_verified\":true,\"atomic_generation\":false,\"cast_executed\":false,\"database_written\":false}");exit=0;
                }finally{executor.close();if(!executor.awaitTermination(5000))throw new IllegalStateException();}
            }
        }catch(Exception e){
            String reason=e instanceof ModernBackendExecutor.BackendException b?b.failure().name():"INVALID_RESULT_OR_SETUP";
            System.out.println("{\"phase\":\"production-disintegrate-source\",\"status\":\"FAILED\",\"stage\":\""+stage+"\",\"reason\":\""+reason+"\"}");
        }finally{if(password!=null)Arrays.fill(password,'\0');}
        if(exit!=0)System.exit(exit);
    }
}
