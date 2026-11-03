package local.lito381.server;

import java.lang.reflect.*;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Source-scoped authenticated sessions, actual patched permission/GM handlers; no SQL writes. */
public class Regression {
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
    static Object call(Object object,String name,Class<?>[] types,Object...args)throws Exception {
        var m=object.getClass().getDeclaredMethod(name,types);m.setAccessible(true);
        try{return m.invoke(object,args);}catch(InvocationTargetException e){throw new RuntimeException(e.getCause());}
    }
    static ServerConfig rogueConfig(ServerConfig config)throws Exception {
        var parts=ServerConfig.class.getRecordComponents();var types=new Class<?>[parts.length];var values=new Object[parts.length];
        for(int i=0;i<parts.length;i++){types[i]=parts[i].getType();values[i]=parts[i].getName().equals("administrationAccounts")?Set.of("3333"):parts[i].getAccessor().invoke(config);}
        return ServerConfig.class.getDeclaredConstructor(types).newInstance(values);
    }
    static void drain(SourceLoginRegression.Fixture f,Protocol.CipherState cipher)throws Exception {
        f.client.setSoTimeout(60);try{while(true)cipher.decrypt(Protocol.readFrame(f.client.getInputStream(),65535));}catch(SocketTimeoutException done){}
    }
    static String command(SourceLoginRegression.Fixture f,Protocol.CipherState cipher,String text)throws Exception {
        check((boolean)call(f.session,"handleGmChatCommand",new Class[]{String.class},text),"command consumed");
        f.client.setSoTimeout(1000);byte[] packet=cipher.decrypt(Protocol.readFrame(f.client.getInputStream(),65535));
        return new String(packet,Protocol.LEGACY_TEXT);
    }
    public static void main(String[] args)throws Exception {
        Path configPath=Path.of(args[0]),output=Path.of(args[1]);Files.createDirectories(output);
        var props=UnifiedConfiguration.raw(configPath);check(props.size()==22,"22 public controls");UnifiedConfiguration.validate(props);
        props.setProperty("administration.accounts","3333");
        try{UnifiedConfiguration.validate(props);throw new AssertionError("retired allowlist accepted");}catch(IllegalArgumentException expected){}
        var config=rogueConfig(ServerConfig.load(configPath,"standard"));
        check(config.administrationAccounts().contains("3333"),"legacy allowlist fixture injected");
        int[] levels={200,0,1};int[] reads={0,0,0};boolean[] fail={false,false,false};AtomicInteger required=new AtomicInteger(100);
        Map<String,AccountRepository> routes=new LinkedHashMap<>();
        for(int i=0;i<3;i++) {
            final int index=i;
            AccountRepository delegate=new MemoryAccountRepository("3333","same-password","",0,1.0,40,52);
            AccountRepository repository=(AccountRepository)java.lang.reflect.Proxy.newProxyInstance(Regression.class.getClassLoader(),new Class[]{AccountRepository.class},(p,m,a)->{
                if(m.getName().equals("repositoryForClassicSource")){check(config.classicServerNames().get(index).equals(a[0]),"bound source retained");return p;}
                if(m.getName().equals("accessLevel")){reads[index]++;check(a[0].equals("3333"),"raw account passed to source");if(fail[index])throw new java.sql.SQLException("fixture unavailable");return levels[index];}
                if(m.getName().equals("gmCommand")){String name=(String)a[0];return new GmCommandDefinition(name,required.get(),switch(name){case "item"->"L1CreateItem";case "spawn"->"L1SpawnCmd";case "de"->"L1SpawnDe";default->throw new AssertionError(name);},"",false);}
                try{return m.invoke(delegate,a);}catch(InvocationTargetException e){throw e.getCause();}
            });routes.put(config.classicServerNames().get(i),repository);
        }
        try(var accounts=new SourceRoutedAccountRepository(routes);var capture=new CaptureLog(output.resolve("captures"))) {
            var state=new GameStateStore(output.resolve("unused.state"),false,true);
            try(var hub=new GameHub(config,state,accounts)) {
                for(int i=0;i<3;i++) {
                    try(var fixture=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,i,ClientSession.Endpoint.LOGIN)) {
                        var cipher=new Protocol.CipherState(0x37110001);
                        check(!(boolean)call(fixture.session,"isAdministrator",new Class[]{}),"unauthenticated denied");
                        fixture.login("same-password");drain(fixture,cipher);
                        boolean expected=levels[i]>0;
                        check((boolean)call(fixture.session,"isAdministrator",new Class[]{})==expected,"same-name source isolation "+i);
                        check((boolean)call(fixture.session,"requireAdministrator",new Class[]{int.class},Protocol.C_SILENCE)==expected,"native privileged gate "+i);
                        int readsBefore=reads[i];
                        for(int n=0;n<1000;n++)check((boolean)call(fixture.session,"isAdministrator",new Class[]{})==expected,"stable snapshot");
                        check(reads[i]==readsBefore,"no per-action SQL permission reads");
                        for(String command:List.of(".item",".spawn",".de 1")) {
                            String message=command(fixture,cipher,command);
                            check(message.contains(i==0?(command.startsWith(".de")?"只能":"用法"):"權限不足"),"command level enforced "+i+" "+command);
                        }
                        if(i==1) {
                            required.set(0);
                            for(String command:List.of(".item",".spawn",".de 1"))check(command(fixture,cipher,command).contains("權限不足"),"non-GM denied even command threshold zero");
                            required.set(100);
                        }
                        // Explicit lifecycle invalidation refreshes the same account/source on login.
                        int original=levels[i];levels[i]=0;SourceAdministration.clear(fixture.session);
                        check(!(boolean)call(fixture.session,"isAdministrator",new Class[]{}),"revoked level after refresh");
                        levels[i]=original;SourceAdministration.clear(fixture.session);
                        fail[i]=true;check(!(boolean)call(fixture.session,"isAdministrator",new Class[]{}),"SQL failure denied");
                        fail[i]=false;check((boolean)call(fixture.session,"isAdministrator",new Class[]{})==expected,"SQL recovery no sticky grant");
                        hub.disconnect(fixture.session);
                        var field=SourceAdministration.class.getDeclaredField("sessions");field.setAccessible(true);
                        check(!((Map<?,?>)field.get(null)).containsKey(fixture.session),"disconnect frees authority snapshot");
                    }
                }
            }
        }
        check(SourceLogin.bindings.isEmpty(),"fixture endpoints cleaned");
        // Actual SQL adapters are also read-only and return no authority for an absent account.
        if(args.length>2 && args[2].equals("--sql")) try(var live=AccountRepository.open(config)) {
            for(String source:config.classicServerNames())check(live.repositoryForClassicSource(source).accessLevel("codex-authority-missing-20261012")==0,"SQL absent account denied");
        }
        System.out.println("PASS: 3 authenticated sources/same account; legacy allowlist ignored; native gates; .item/.spawn/.de thresholds and zero-level denial; snapshot reuse/revocation/error/cleanup; 22 controls; retired setting rejected; configuration SQL read-only; optional live account probe requires --sql.");
    }
}
