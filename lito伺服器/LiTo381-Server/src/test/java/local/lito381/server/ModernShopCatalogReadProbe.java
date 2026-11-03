package local.lito381.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Explicit test-classpath SELECT-only probe. Never buys, consumes, writes, or opens native Accounts. */
public final class ModernShopCatalogReadProbe {
    private ModernShopCatalogReadProbe() {}
    public static void main(String[] args) {
        ModernDatabaseReadProbe.Settings settings=null;ModernServerConfig config=null;ModernBackendExecutor executor=null;
        char[] password=null;String stage="CONFIGURATION",success=null;boolean retired=true;int exit=2;
        try {
            if(args.length!=1)throw new IllegalArgumentException("explicit configuration required");
            Path file=Path.of(args[0]);String account=System.getenv("LITO_MODERN_PROBE_ACCOUNT"),secret=System.getenv("LITO_MODERN_PROBE_PASSWORD");
            if(!ModernCharacterDataReadProbe.boundedText(account,64,false)||!ModernCharacterDataReadProbe.boundedText(secret,256,true))
                throw new IllegalArgumentException("explicit credentials required");
            password=secret.toCharArray();settings=ModernDatabaseReadProbe.load(file);
            config=ModernDatabaseReadProbe.modernConfig(settings,ModernCharacterDataReadProbe.catalogDatabase(file));
            final ModernServerConfig configured=config;executor=new ModernBackendExecutor(1,1);retired=false;
            int characters=0;var sourceJson=new ArrayList<String>();
            try(var gate=executor.openSession()) {
                for(var source:config.sources()) {
                    var audit=new ModernEquipmentReadProbe.Audit();
                    var dao=new ModernJdbcBackend(source.database(),config.dbConnectMillis(),config.dbNetworkMillis(),
                            (url,properties)->ModernEquipmentReadProbe.inspectOwned(new com.mysql.cj.jdbc.Driver().connect(url,properties),
                                    ModernBackendScope.current(),source.database().database(),source.catalogDatabase(),configured.dbNetworkMillis(),null,audit,null));
                    var verifier=ModernAccountAuthentication.repositories(Map.of(source.id(),
                            ModernAccountAuthentication.SourceBinding.direct(source.name(),dao::authenticate)),ignored->false);
                    stage="AUTHENTICATION";var principal=ModernEquipmentRepositoryReadProbe.authenticate(gate,verifier,account,password);
                    stage="ROSTER";var roster=gate.call(5000,scope->dao.characters(principal.accountForSource(source.id())));
                    if(roster.isEmpty())throw new IllegalStateException("no probe character");
                    var repository=ModernShopCatalogRepository.jdbc(Map.of(source.id(),new ModernShopCatalogRepository.Source(source,dao)));
                    List<ModernShopCatalog.Offer> first=null;
                    for(var expected:roster) {
                        if(++characters>4096)throw new IllegalStateException("probe bound");stage="SHOP_QUALIFICATION";
                        var snapshot=gate.call(5000,scope->repository.character(principal,source.id(),expected.objectId())).orElseThrow();
                        if(snapshot.sourceId()!=source.id() || !snapshot.canonicalAccount().equals(principal.accountForSource(source.id()))
                                || !snapshot.character().equals(expected))throw new IllegalStateException("source changed");
                        if(first==null)first=snapshot.offers();else if(!first.equals(snapshot.offers()))throw new IllegalStateException("catalog changed during probe");
                    }
                    stage="ROSTER_RECHECK";
                    if(!gate.call(5000,scope->dao.characters(principal.accountForSource(source.id()))).equals(roster))throw new IllegalStateException("roster changed");
                    sourceJson.add(sourceJson(source.id(),roster.size(),first));
                }
            }
            stage="RETIREMENT";executor.close();retired=executor.awaitTermination(5000);
            if(!retired)throw new IllegalStateException("worker not retired");
            success="{\"phase\":\"production-shop-read\",\"status\":\"OK\",\"characters\":"+characters
                    +",\"sources\":["+String.join(",",sourceJson)+"],\"source_database_verified\":true,\"atomic_generation\":false,"
                    +"\"purchase_executed\":false,\"database_written\":false}";
            if(success.length()>4*1024*1024)throw new IllegalStateException("probe output bound");exit=0;
        } catch(Exception failure) {
            String reason=failure instanceof ModernBackendExecutor.BackendException backend?backend.failure().name():"INVALID_RESULT_OR_SETUP";
            System.out.println("{\"phase\":\"production-shop-read\",\"status\":\"FAILED\",\"stage\":\""+stage+"\",\"reason\":\""+reason+"\"}");
        } finally {
            if(executor!=null)try{executor.close();retired=executor.awaitTermination(5000);}catch(Exception failure){retired=false;}
            if(retired) {
                try{try{if(config!=null)config.close();}finally{if(settings!=null)settings.close();}}
                catch(Exception failure){exit=2;System.out.println("{\"phase\":\"production-shop-read\",\"status\":\"FAILED\",\"reason\":\"OWNER_CLOSE_FAILED\"}");}
            }else{exit=2;System.out.println("{\"phase\":\"production-shop-read\",\"status\":\"FAILED\",\"reason\":\"WORKER_NOT_RETIRED\"}");}
            if(password!=null)Arrays.fill(password,'\0');
        }
        if(exit==0)System.out.println(success);else System.exit(exit);
    }
    /** Public source metadata only: never owner/account, SQL, driver messages, paths or password. */
    static String sourceJson(int source,int characters,List<ModernShopCatalog.Offer> offers) {
        if(source<1 || characters<1 || characters>64 || offers==null || offers.size()>ModernShopCatalog.MAX_ROWS)throw new IllegalArgumentException("probe value");
        var result=new StringBuilder("{\"source_id\":").append(source).append(",\"characters\":").append(characters).append(",\"offers\":[");
        boolean first=true;
        for(var offer:offers) {
            if(!first)result.append(',');first=false;
            result.append("{\"item_id\":").append(offer.itemId()).append(",\"order\":").append(offer.sortOrder())
                    .append(",\"buy_price\":").append(offer.buyPrice()).append(",\"sell_price\":").append(offer.sellPrice())
                    .append(",\"name_token\":\"").append(json(offer.nameToken())).append("\",\"inventory_graphic\":").append(offer.inventoryGraphic())
                    .append(",\"ground_graphic\":").append(offer.groundGraphic()).append(",\"support\":\"").append(offer.support()).append('"');
            if(offer.healing()!=null) {var h=offer.healing();result.append(",\"heal_min\":").append(h.min()).append(",\"heal_max\":").append(h.max())
                    .append(",\"effect\":").append(h.effect()).append(",\"delay_group\":").append(h.delayGroup()).append(",\"delay_ms\":").append(h.delayMillis());}
            if(offer.timed()!=null) {var kind=offer.timed().kind();result.append(",\"timed_kind\":\"").append(kind.name()).append('"')
                    .append(",\"effect\":").append(kind.effect()).append(",\"duration_ms\":").append(kind.durationMillis())
                    .append(",\"class_mask\":").append(kind.classMask());}
            result.append('}');
        }
        return result.append("]}").toString();
    }
    private static String json(String text){return text.replace("\\","\\\\").replace("\"","\\\"");}
}
