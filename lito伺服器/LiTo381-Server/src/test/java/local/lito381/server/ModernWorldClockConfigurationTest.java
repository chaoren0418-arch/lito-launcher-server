package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernWorldClockConfigurationTest {
    @TempDir Path temporary;
    static final String PREFIX="world.1.514.";
    Properties properties(){
        var p=new Properties();p.setProperty("world.sharedRoot",temporary.toString().replace('\\','/'));p.setProperty("world.bindings","1:514");
        p.setProperty(PREFIX+"physicalMap","514");p.setProperty(PREFIX+"sectors","80008000");p.setProperty(PREFIX+"spawnX","32778");
        p.setProperty(PREFIX+"spawnY","32778");p.setProperty(PREFIX+"stepMillis","200");return p;
    }
    private void writeSyntheticMap()throws Exception{Files.write(Files.createDirectories(temporary.resolve("map/514")).resolve("80008000.s32"),ModernWorldMapTest.openSectorBytes());}
    private void rejectsBeforeSecrets(Properties p){
        String base="schema=1\nbind.port=0\ntls.pkcs12="+temporary.resolve("unused.p12").toString().replace('\\','/')+
                "\ntls.password.env=T\nsources=7\nsource.7.name=Source\nsource.7.mysql.host=127.0.0.1\nsource.7.mysql.port=3306\n"+
                "source.7.mysql.database=synthetic\nsource.7.mysql.username.env=U\nsource.7.mysql.password.env=P\nsource.7.mysql.passwordScheme=plain\nsource.7.mysql.tls=disabled\n";
        String text=base+p.stringPropertyNames().stream().sorted().map(k->k+"="+p.getProperty(k).replace(" ","\\ ")+"\n").reduce("",String::concat);
        var secrets=new AtomicInteger();var failure=assertThrows(ModernServerConfig.ConfigException.class,()->ModernServerConfig.parse(text.getBytes(StandardCharsets.UTF_8),key->{secrets.incrementAndGet();return "not-consulted";}));
        assertEquals(0,secrets.get());assertNull(failure.getCause());
    }
    @Test void absentAndAllEightPartialConfigurationsReachMappingsWithoutManufacturingDefaults()throws Exception{
        writeSyntheticMap();
        for(int mask=0;mask<8;mask++){
            var p=properties();if((mask&1)!=0)p.setProperty(PREFIX+"loadTimeoutMillis","25000");
            if((mask&2)!=0)p.setProperty(PREFIX+"countdownNanos","30000000000");if((mask&4)!=0)p.setProperty(PREFIX+"activeTimeoutMillis","1800000");
            var mapping=ModernWorldConfiguration.parse(p).load().get(new ModernWorldBinding.Key(1,514));
            if(mask==0)assertNull(mapping.clockOverrides());else{
                assertEquals((mask&1)==0?null:Integer.valueOf(25000),mapping.clockOverrides().loadTimeoutMillis());
                assertEquals((mask&2)==0?null:Long.valueOf(30_000_000_000L),mapping.clockOverrides().countdownNanos());
                assertEquals((mask&4)==0?null:Integer.valueOf(1800000),mapping.clockOverrides().activeTimeoutMillis());
            }
            var descriptor=ModernMatchDescriptor.of(mapping,ModernWorldClockTest.globals(),false);
            assertEquals((mask&1)==0?2000:25000,descriptor.loadTimeoutMillis());assertEquals((mask&2)==0?3_000_000_001L:30_000_000_000L,descriptor.countdownNanos());
            assertEquals((mask&4)==0?45000:1800000,descriptor.activeTimeoutMillis());assertEquals(2000,descriptor.preparationTimeoutMillis());
        }
    }
    @Test void sharedPhysicalGeometryNeverAliasesLogicalBindingClocks()throws Exception{
        writeSyntheticMap();var p=properties();p.setProperty("world.bindings","1:514,1:509");
        for(String key:List.copyOf(p.stringPropertyNames()))if(key.startsWith(PREFIX))p.setProperty(key.replace(PREFIX,"world.1.509."),p.getProperty(key));
        p.setProperty(PREFIX+"countdownNanos","1");p.setProperty("world.1.509.activeTimeoutMillis","50");
        var parsed=ModernWorldConfiguration.parse(p);p.setProperty(PREFIX+"countdownNanos","30000000000");
        var mappings=parsed.load();var a=mappings.get(new ModernWorldBinding.Key(1,514));var b=mappings.get(new ModernWorldBinding.Key(1,509));
        assertSame(a.binding().map(),b.binding().map());
        assertEquals(1,ModernMatchDescriptor.of(a,ModernWorldClockTest.globals(),false).countdownNanos());
        assertEquals(45000,ModernMatchDescriptor.of(a,ModernWorldClockTest.globals(),false).activeTimeoutMillis());
        assertEquals(3_000_000_001L,ModernMatchDescriptor.of(b,ModernWorldClockTest.globals(),false).countdownNanos());
        assertEquals(50,ModernMatchDescriptor.of(b,ModernWorldClockTest.globals(),false).activeTimeoutMillis());
    }
    @Test void malformedOutOfRangeUnknownAndUnselectedClockKeysFailBeforeSecretsOrResources()throws Exception{
        for(String suffix:List.of("loadTimeoutMillis","countdownNanos","activeTimeoutMillis")){
            for(String bad:List.of("","-1","+1","01"," 50","50 ","1e3","0x10","NaN","9223372036854775808","999999999999999999999999")){
                var p=properties();p.setProperty(PREFIX+suffix,bad);rejectsBeforeSecrets(p);
            }
        }
        for(String[] row:new String[][]{{"loadTimeoutMillis","0"},{"loadTimeoutMillis","49"},{"loadTimeoutMillis","30001"},
                {"countdownNanos","30000000001"},{"activeTimeoutMillis","0"},{"activeTimeoutMillis","49"},{"activeTimeoutMillis","1800001"}}){
            var p=properties();p.setProperty(PREFIX+row[0],row[1]);rejectsBeforeSecrets(p);
        }
        for(String key:List.of(PREFIX+"countdownMillis",PREFIX+"preparationTimeoutMillis",PREFIX+"journalTimeoutMillis",PREFIX+"timeoutPolicy","world.1.509.activeTimeoutMillis")){
            var p=properties();p.setProperty(key,"1000");rejectsBeforeSecrets(p);
        }
        assertFalse(Files.exists(temporary.resolve("map")),"static parsing must not open or manufacture resources");
    }
    @Test void exactBoundaryNanosecondsAreAcceptedWithoutMillisRounding()throws Exception{
        writeSyntheticMap();
        for(String value:List.of("0","1","2147483648","30000000000")){
            var p=properties();p.setProperty(PREFIX+"countdownNanos",value);
            var mapping=ModernWorldConfiguration.parse(p).load().get(new ModernWorldBinding.Key(1,514));assertEquals(Long.valueOf(value),mapping.clockOverrides().countdownNanos());
        }
    }
}
