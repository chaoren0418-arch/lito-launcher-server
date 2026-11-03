package local.lito381.server;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Shared Lineage inputs are immutable; only character_lito contains durable LiTo state. */
final class SharedDatabase {
    // Only the settlement call site may open this narrow write capability.
    private static final ThreadLocal<Boolean> SETTLEMENT = new ThreadLocal<>();
    static RepositoryTournamentMutation commitResult(AccountRepository repository, long id, String name,
            RepositoryTournamentProfile expected, GameStateStore.TournamentProfileState replacement) throws Exception {
        Boolean previous = SETTLEMENT.get();
        SETTLEMENT.set(Boolean.TRUE);
        try { return repository.persistTournamentProfile(id, name, expected, replacement); }
        finally { if (previous == null) SETTLEMENT.remove(); else SETTLEMENT.set(previous); }
    }
    static final String POLICY = "source-readonly-stats-only";
    static final Set<String> TABLES = Set.of("lito_battle_rule", "lito_battle_object",
            "lito_battle_spawn", "lito_battle_player_spawn", "lito_tournament_shop", "commands");
    static final Map<LegacyMySqlRepository, Route> ROUTES =
            Collections.synchronizedMap(new WeakHashMap<>());
    static Properties properties(Path file) throws Exception {
        return UnifiedConfiguration.read(file);
    }

    static boolean enabled(ServerConfig config) throws Exception {
        String policy = properties(config.source()).getProperty("database.persistence.policy", "");
        if (!policy.isBlank() && !POLICY.equals(policy)) throw new IllegalArgumentException("Unknown persistence policy");
        return POLICY.equals(policy);
    }
    static String identifier(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_]+")) throw new IllegalArgumentException("Invalid database identifier");
        return value;
    }
    static AccountRepository open(ServerConfig config) throws Exception {
        if (!config.databaseReadOnly() || config.statePersistenceEnabled() || config.classicWorldCompatibilityEnabled())
            throw new IllegalArgumentException("Shared sources require read-only, memory state and arena mode");
        Properties settings = properties(config.source()), credentials = properties(config.legacySqlProperties());
        String controls = identifier(settings.getProperty("database.lito.database"));
        if (!"character_lito".equals(settings.getProperty("database.lito.character.table")))
            throw new IllegalArgumentException("Only character_lito writes are supported");
        Map<String, AccountRepository> sources = new LinkedHashMap<>();
        try {
            for (int i = 0; i < config.classicServerNames().size(); i++) {
                String name = config.classicServerNames().get(i), db = identifier(config.legacySourceDatabases().get(i));
                // legacy_mysql suppresses every LiTo extension join and all source mutations.
                LegacyMySqlRepository repository = new LegacyMySqlRepository(config.legacySqlProperties(), name,
                        config.classicServerIds().get(i), db, config.legacySourceResourceDatabases().get(i),
                        config.legacySourceWeightRates().get(i), true, "legacy_mysql");
                ROUTES.put(repository, new Route(name, db, controls, credentials));
                sources.put(name, repository);
            }
            DurableBattleSettlement.open(config);
            return sources.size()==1 ? sources.values().iterator().next() : new SourceRoutedAccountRepository(sources);
        } catch (Exception e) {
            for (AccountRepository repository : sources.values()) repository.close();
            throw e;
        }
    }
    static boolean registered(LegacyMySqlRepository repository) { return ROUTES.containsKey(repository); }
    static String resourceTable(LegacyMySqlRepository repository, String table, String ordinary) {
        identifier(table);
        Route route = ROUTES.get(repository);
        return "`" + (route != null && TABLES.contains(table) ? route.controls : identifier(ordinary)) + "`.`" + table + "`";
    }
    static void check(LegacyMySqlRepository repository) throws Exception {
        Route route = ROUTES.get(repository);
        if (route == null) return;
        try (Connection c = route.connection(); Statement s = c.createStatement()) {
            c.setReadOnly(true);
            try (ResultSet r = s.executeQuery("SELECT char_obj_id,char_name,wins,losses,version,updated_at FROM character_lito LIMIT 0")) {}
            try (ResultSet r = s.executeQuery("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='character_lito'")) {
                if (!r.next() || !("InnoDB".equalsIgnoreCase(r.getString(1)) || "MyISAM".equalsIgnoreCase(r.getString(1))))
                    throw new SQLException("character_lito requires InnoDB or MyISAM");
            }
        }
        System.out.println("[LiTo] shared-source=" + route.database + " controls=" + route.controls + " source-read-only=true write-table=character_lito");
    }
    static RepositoryTournamentRoster profiles(LegacyMySqlRepository repository) throws Exception {
        Route route = ROUTES.get(repository);
        synchronized (route) {
            List<RepositoryTournamentProfile> rows = new ArrayList<>();
            try (Connection c = route.connection(); Statement s = c.createStatement()) {
                c.setReadOnly(true);
                try (ResultSet r = s.executeQuery("SELECT c.objid,c.char_name,t.wins,t.losses,t.version FROM characters c LEFT JOIN character_lito t ON t.char_obj_id=c.objid WHERE c.Banned=0 ORDER BY c.objid")) {
                    while (r.next()) rows.add(route.profile(r.getLong(1), r.getString(2), r.getInt(3), r.getInt(4), r.getLong(5)));
                }
            }
            Set<Long> liveIds = new HashSet<>();
            for (var row : rows) liveIds.add(row.characterObjectId());
            route.runtime.keySet().retainAll(liveIds);
            return new RepositoryTournamentRoster(true, rows);
        }
    }
    static RepositoryTournamentMutation persist(LegacyMySqlRepository repository, long id, String name,
            RepositoryTournamentProfile expected, GameStateStore.TournamentProfileState replacement) throws Exception {
        Route route = ROUTES.get(repository);
        if (!Boolean.TRUE.equals(SETTLEMENT.get()))
            throw new IllegalStateException("character_lito writes require battle settlement");
        synchronized (route) {
            if (id <= 0 || replacement == null || !route.name.equals(replacement.classicServerName())
                    || expected != null && (expected.characterObjectId() != id || !expected.characterName().equals(name)))
                throw new IllegalArgumentException("Invalid source-qualified result");
            try (Connection c = route.connection()) {
                c.setAutoCommit(false);
                try {
                    String canonical;
                    try (PreparedStatement p = c.prepareStatement("SELECT char_name FROM characters WHERE objid=? AND Banned=0")) {
                        p.setLong(1,id); try (ResultSet r=p.executeQuery()) { canonical=r.next()?r.getString(1):null; }
                    }
                    if (!Objects.equals(canonical,name)) { c.rollback(); return new RepositoryTournamentMutation(RepositoryTournamentMutationStatus.NOT_FOUND,null); }
                    RepositoryTournamentProfile current;
                    boolean exists;
                    try (PreparedStatement p=c.prepareStatement("SELECT wins,losses,version FROM character_lito WHERE char_obj_id=?")) {
                        p.setLong(1,id); try(ResultSet r=p.executeQuery()) {
                            exists=r.next();
                            current=exists?route.profile(id,name,r.getInt(1),r.getInt(2),r.getLong(3)):route.profile(id,name,0,0,0);
                        }
                    }
                    if (expected == null ? current.snapshotVersion()!=0 :
                            expected.snapshotVersion()!=current.snapshotVersion()
                            || expected.state().wins()!=current.state().wins()
                            || expected.state().losses()!=current.state().losses()) {
                        c.rollback(); return new RepositoryTournamentMutation(RepositoryTournamentMutationStatus.CONFLICT,current);
                    }
                    boolean changed=current.state().wins()!=replacement.wins() || current.state().losses()!=replacement.losses();
                    long version=current.snapshotVersion();
                    if (changed) {
                        long winsDelta = (long) replacement.wins() - current.state().wins();
                        long lossesDelta = (long) replacement.losses() - current.state().losses();
                        if (!((winsDelta == 1 && lossesDelta == 0) || (winsDelta == 0 && lossesDelta == 1)))
                            throw new SQLException("Settlement must add exactly one win or loss");
                        version=Math.addExact(version,1);
                        if(version>0xFFFF_FFFFL)throw new SQLException("Result version exhausted");
                        String sql=!exists
                            ? "INSERT INTO character_lito(char_obj_id,char_name,wins,losses,version) VALUES(?,?,?,?,?)"
                            : "UPDATE character_lito SET char_name=?,wins=?,losses=?,version=? WHERE char_obj_id=? AND version=? AND wins=? AND losses=?";
                        try (PreparedStatement p=c.prepareStatement(sql)) {
                            if(!exists){p.setLong(1,id);p.setString(2,name);p.setInt(3,replacement.wins());p.setInt(4,replacement.losses());p.setLong(5,version);}
                            else{p.setString(1,name);p.setInt(2,replacement.wins());p.setInt(3,replacement.losses());p.setLong(4,version);p.setLong(5,id);p.setLong(6,current.snapshotVersion());p.setInt(7,current.state().wins());p.setInt(8,current.state().losses());}
                            // Exactly one atomic write, including on MyISAM. Never depend on rollback to undo a partial result.
                            if(p.executeUpdate()!=1){c.rollback();return new RepositoryTournamentMutation(RepositoryTournamentMutationStatus.CONFLICT,current);}
                        } catch(SQLException race) {
                            if(!exists && race.getErrorCode()==1062){c.rollback();return new RepositoryTournamentMutation(RepositoryTournamentMutationStatus.CONFLICT,current);}
                            throw race;
                        }
                    }
                    c.commit();
                    RepositoryTournamentProfile result=new RepositoryTournamentProfile(id,name,version,replacement);
                    route.runtime.put(id,result);
                    return new RepositoryTournamentMutation(RepositoryTournamentMutationStatus.SUCCESS,result);
                } catch(Exception failure) { c.rollback(); throw failure; }
            }
        }
    }
    static final class Route {
        final String name,database,controls,url,user,password;
        final Map<Long,RepositoryTournamentProfile> runtime=new HashMap<>();
        Route(String name,String database,String controls,Properties p) {
            this.name=name;this.database=database;this.controls=controls;
            url=p.getProperty("URL1_LOGIN")+database+p.getProperty("URL3_LOGIN","");
            user=p.getProperty("Login_LOGIN",p.getProperty("Login"));
            password=p.getProperty("Password_LOGIN",p.getProperty("Password",""));
        }
        Connection connection() throws SQLException { return DriverManager.getConnection(url,user,password); }
        RepositoryTournamentProfile profile(long id,String character,int wins,int losses,long version) {
            RepositoryTournamentProfile cached=runtime.get(id);
            if(cached!=null && cached.characterName().equals(character) && cached.snapshotVersion()==version
                    && cached.state().wins()==wins && cached.state().losses()==losses)return cached;
            var state=new GameStateStore.TournamentProfileState(0,wins,losses,0,0,0,0,0,
                    Collections.nCopies(12,0),Collections.nCopies(5,0),false,name,-1,-1,-1,false,
                    0,0,0,"",-1,-1,-1,false,0,0,0,0,0,0,0,0,0,0,0,1,0,0);
            var result=new RepositoryTournamentProfile(id,character,version,state);
            // Do not cache every source character on SELECT. Only committed
            // compact ranking records survive sessions; no inventory/actor data.
            if (cached != null) runtime.put(id,result);
            return result;
        }
    }
    /** Eight stable slots, with independent enable switches; IDs never renumber. */
    static boolean applySources(Properties target,Path base) throws Exception {
        if (target.containsKey("database.jdbc.base.url") || target.containsKey("configuration.base.file")) { UnifiedConfiguration.apply(target,base); return true; }
        String file=target.getProperty("source.servers.file","");
        if(file.isBlank())return false;
        Path path=base.resolve(file).normalize();
        if(!Files.isRegularFile(path))return false;
        Properties sources=properties(path);
        if(!sources.containsKey("source.slots"))return false;
        int slots=Integer.parseInt(sources.getProperty("source.slots"));
        if(slots<1||slots>8)throw new IllegalArgumentException("source.slots must be 1..8");
        List<String> names=new ArrayList<>(),ids=new ArrayList<>(),dbs=new ArrayList<>(),rates=new ArrayList<>();
        for(int i=1;i<=slots;i++) {
            String prefix="source."+i+".",enabled=sources.getProperty(prefix+"enabled", "false");
            if(!Set.of("true","false").contains(enabled))throw new IllegalArgumentException("Invalid source enabled flag");
            if(!Boolean.parseBoolean(enabled))continue;
            String name=sources.getProperty(prefix+"name","").trim(),id=sources.getProperty(prefix+"id","").trim();
            if(name.isEmpty()||name.contains(",")||!id.matches("[1-8]"))throw new IllegalArgumentException("Invalid source name/id");
            names.add(name);ids.add(id);dbs.add(identifier(sources.getProperty(prefix+"database","")));rates.add(sources.getProperty(prefix+"weight.rate","100.0"));
        }
        if(names.isEmpty()||new HashSet<>(names).size()!=names.size()||new HashSet<>(ids).size()!=ids.size()||new HashSet<>(dbs).size()!=dbs.size())
            throw new IllegalArgumentException("Sources must be nonempty and unique");
        target.setProperty("login.classic.server.names",String.join(",",names));target.setProperty("login.classic.server.ids",String.join(",",ids));
        target.setProperty("database.dynamic.databases",String.join(",",dbs));target.setProperty("database.source.weight.rates",String.join(",",rates));
        return true;
    }
}
