package local.lito381.server;

import java.nio.file.*;
import java.util.*;

/** Read source rows only; reconcile into an isolated, non-persistent memory store. */
public final class SourceSelectionReadProbe {
    public static void main(String[] args) throws Exception {
        ServerConfig config=ServerConfig.load(Path.of(args[0]));
        String source=config.classicServerNames().get(0);
        // Do not open SharedDatabase or its battle settlement journal.
        try(AccountRepository repo=new LegacyMySqlRepository(
                config.legacySqlProperties(),source,config.classicServerIds().get(0),
                config.legacySourceDatabases().get(0),
                config.legacySourceResourceDatabases().get(0),
                config.legacySourceWeightRates().get(0),true,"legacy_mysql")) {
            var field=repo.getClass().getDeclaredField("dataSource");
            field.setAccessible(true);
            try(var connection=((javax.sql.DataSource)field.get(repo)).getConnection();
                var statement=connection.prepareStatement(
                    "SELECT i.id,i.item_id,a.ac,a.material,a.weight,a.safenchant "
                    +"FROM character_items i JOIN `"+config.legacySourceResourceDatabases().get(0)
                    +"`.armor a ON a.item_id=i.item_id WHERE i.id=10447");
                var rows=statement.executeQuery()) {
                while(rows.next()) System.out.println("FAILED ROW id="+rows.getLong(1)
                    +" item="+rows.getInt(2)+" ac="+rows.getInt(3)+" material="+rows.getString(4)
                    +" weight="+rows.getInt(5)+" safeEnchant="+rows.getInt(6));
            }
            int failures=0,checked=0;
            for(CharacterSummary c:repo.characters(args[1])) {
                checked++;
                System.out.println("PROBE character="+c.name());
                try {
                    var items=repo.sourceInventory(c.objectId());
                    var equipment=repo.equipmentMagicStats(c.objectId());
                    for(var item:items) {
                        try { item.definition(); }
                        catch(Exception e) {
                            failures++;
                            System.out.println("DEFINITION FAILED catalog="+item.catalogId());
                            e.printStackTrace(System.out);
                        }
                    }
                    GameStateStore state=new GameStateStore(Path.of(args[2]),false,true);
                    var imported=state.reconcileTournamentSourceInventory(
                        GameStateStore.sourceCharacter(source,c.name()),items,equipment,true);
                    state.importSourcePets(GameStateStore.sourceCharacter(source,c.name()),
                        repo.pets(c.objectId()),imported.definitions());
                    System.out.println("PASS rows="+items.size());
                } catch(Exception e) { failures++;e.printStackTrace(System.out); }
            }
            if(failures!=0)throw new AssertionError("source selection failures="+failures);
            System.out.println("PASS all source characters="+checked+" SQL reads only; memory persistence disabled");
        }
    }
}
