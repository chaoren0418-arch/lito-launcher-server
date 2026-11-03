package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

/**
 * SELECT-only item facts for the traced LegacyEquipmentMagicLoader ability subset.
 * One data statement joins the owned character, all instances, exact template and
 * per-instance dark/card data; join multiplication is an error, never deduplicated.
 * The caller owns the read-only connection, its leases and the backend deadline.
 * This does not open a connection, mutate source data or establish a transaction.
 * Separately reading CatalogJdbc does NOT establish an atomic MyISAM generation.
 *
 * A preceding bounded metadata SELECT qualifies six retained optional weapon
 * resistance columns. Metadata-missing columns additionally require a fixed
 * LIMIT 0 lookup to fail with vendor 1054 / SQLState 42S22 and clean resource
 * cleanup, so filtered metadata privileges cannot masquerade as absence.
 * Only verified absence produces SQL zero; present
 * columns stay required numeric reads, including strict NULL rejection. All other
 * referenced columns must exist. This is at most NINE queries (one metadata,
 * zero to six absence guards, one data query and one source lifetime query), not an atomic schema/data
 * snapshot, and it holds no schema-generation lock. Known loader-unread dimensions
 * are explicit SQL zeroes. Every numeric field uses an exact i64 reader: integral
 * DECIMAL is accepted, while fractional, out-of-range, approximate and text types fail.
 * Queried nullable extension values have a narrow, source-proven interpretation:
 * NULL set_id means no set; NULL greater/stone IDs or tier, card slots and mapped
 * dark modifiers mean zero. Missing columns still fail at the JDBC boundary.
 * Weapon attacks, attribute enchant combat/procs and newer VIP fields are outside
 * this existing Instance/evaluator model, not represented as qualified zeroes.
 */
final class ModernEquipmentSourceJdbc {
    private static final int MAX_SET_TEXT = 4096;
    private static final int MAX_METADATA_COLUMNS = 256;
    private static final Set<String> OPTIONAL_WEAPON_RESISTANCES = Set.of(
            "regist_blind","regist_sleep","regist_freeze","regist_stone","regist_sustain","regist_stun");
    private static final String WEAPON_SCHEMA_SQL = "SELECT LEFT(COLUMN_NAME,257) AS COLUMN_NAME,"
            + "OCTET_LENGTH(COLUMN_NAME) AS COLUMN_NAME_BYTES FROM INFORMATION_SCHEMA.COLUMNS"
            + " WHERE TABLE_SCHEMA=? AND TABLE_NAME=? ORDER BY ORDINAL_POSITION LIMIT 257";
    private static final String[] CLASS_COLUMNS = {
            "use_royal", "use_knight", "use_elf", "use_mage", "use_darkelf", "use_dragonknight", "use_illusionist"};
    private static final Map<String, Integer> WEAPON_TYPES = Map.ofEntries(
            Map.entry("sword",1),Map.entry("dagger",2),Map.entry("tohandsword",3),Map.entry("bow",4),
            Map.entry("spear",5),Map.entry("blunt",6),Map.entry("staff",7),Map.entry("throwingknife",8),
            Map.entry("arrow",9),Map.entry("gauntlet",10),Map.entry("claw",11),Map.entry("edoryu",12),
            Map.entry("singlebow",13),Map.entry("singlespear",14),Map.entry("tohandblunt",15),
            Map.entry("tohandstaff",16),Map.entry("kiringku",17),Map.entry("chainsword",18));
    private static final Map<String, EquipmentRules.ArmorSlot> ARMOR_TYPES = Map.ofEntries(
            Map.entry("helm",EquipmentRules.ArmorSlot.HELMET),Map.entry("armor",EquipmentRules.ArmorSlot.BODY),
            Map.entry("T",EquipmentRules.ArmorSlot.SHIRT),Map.entry("cloak",EquipmentRules.ArmorSlot.CLOAK),
            Map.entry("glove",EquipmentRules.ArmorSlot.GLOVES),Map.entry("boots",EquipmentRules.ArmorSlot.BOOTS),
            Map.entry("shield",EquipmentRules.ArmorSlot.SHIELD),Map.entry("amulet",EquipmentRules.ArmorSlot.AMULET),
            Map.entry("ring",EquipmentRules.ArmorSlot.RING),Map.entry("belt",EquipmentRules.ArmorSlot.BELT),
            Map.entry("ring2",EquipmentRules.ArmorSlot.RING_2),Map.entry("earring",EquipmentRules.ArmorSlot.EARRING),
            Map.entry("guarder",EquipmentRules.ArmorSlot.GUARDER),Map.entry("aid_left",EquipmentRules.ArmorSlot.AID_LEFT),
            Map.entry("aid_right",EquipmentRules.ArmorSlot.AID_RIGHT),Map.entry("pants",EquipmentRules.ArmorSlot.PANTS),
            Map.entry("talisman",EquipmentRules.ArmorSlot.TALISMAN_1),Map.entry("talisman2",EquipmentRules.ArmorSlot.TALISMAN_2),
            Map.entry("talisman3",EquipmentRules.ArmorSlot.TALISMAN_3),Map.entry("talisman4",EquipmentRules.ArmorSlot.TALISMAN_4),
            Map.entry("shoulder_left",EquipmentRules.ArmorSlot.SHOULDER_LEFT),Map.entry("shoulder_right",EquipmentRules.ArmorSlot.SHOULDER_RIGHT),
            Map.entry("talisman5",EquipmentRules.ArmorSlot.TALISMAN_5));
    // L1Weapon.isTwohandedWeapon: gauntlet/singlebow/singlespear are NOT two-handed.
    private static final Set<Integer> TWO_HANDED = Set.of(3,4,5,11,12,15,16,18);
    private static final Map<Stat,String> COMMON_BASE = fields("STR:add_str,DEX:add_dex,CON:add_con,INT:add_int,WIS:add_wis,"
            + "CHA:add_cha,MAX_HP:add_hp,MAX_MP:add_mp,POTION_RECOVERY:add_potion_recovery,SP:add_sp,MAGIC_DAMAGE:magicdmgmodifier,"
            + "MAGIC_HIT:add_magic_hit,BLIND:regist_blind,SLEEP:regist_sleep,FREEZE:regist_freeze,STONE:regist_stone,"
            + "SUSTAIN:regist_sustain,STUN:regist_stun,STUN_SUCCESS:add_stun_success,HP_REGEN:add_hpr,MP_REGEN:add_mpr,"
            + "MELEE_HIT:add_hitup,MELEE_DAMAGE:add_dmgup,RANGED_HIT:add_bow_hitup,RANGED_DAMAGE:add_bow_dmgup,"
            + "EVASION:add_er,MELEE_REDUCTION:add_melee_redux,RANGED_REDUCTION:add_bow_redux,"
            + "IGNORE_REDUCTION:add_ignore_reduction,PVP_REDUCTION:add_pvp_redux");
    private static final Map<Stat,String> ARMOR_BASE = extend(COMMON_BASE,fields(
            "MR:m_def,FIRE:defense_fire,WATER:defense_water,WIND:defense_wind,EARTH:defense_earth,AC:ac"));
    private static final Map<Stat,String> WEAPON_BASE = extend(COMMON_BASE,fields("PIERCE:add_pierce"));
    private static final Map<Stat,String> DARK = fields("STR:add_str,DEX:add_dex,CON:add_con,INT:add_int,WIS:add_wis,CHA:add_cha,"
            + "MAX_HP:add_hp,MAX_MP:add_mp,POTION_RECOVERY:add_potion_recovery,SP:add_sp,MR:add_mr,"
            + "MAGIC_DAMAGE:add_magic_dmg_modifier,MAGIC_HIT:add_magic_hit,BLIND:add_regist_blind,SLEEP:add_regist_sleep,"
            + "FREEZE:add_regist_freeze,STONE:add_regist_stone,SUSTAIN:add_regist_sustain,STUN:add_regist_stun,"
            + "STUN_SUCCESS:add_stun_success,FIRE:add_def_fire,WATER:add_def_water,WIND:add_def_wind,EARTH:add_def_earth,"
            + "REDUCTION:add_dmg_redux,HP_REGEN:add_hp_regen,MP_REGEN:add_mp_regen,WEIGHT_REDUCTION:add_weight_reduction,"
            + "MELEE_HIT:add_hit,MELEE_DAMAGE:add_dmg,RANGED_HIT:add_bow_hit,RANGED_DAMAGE:add_bow_dmg,AC:add_ac,"
            + "EVASION:add_er,MELEE_REDUCTION:add_melee_redux,RANGED_REDUCTION:add_bow_redux,IGNORE_REDUCTION:add_ignore_reduction");

    record Source(String resourceDatabase, ModernCharacterData.Snapshot snapshot, List<Instance> instances,
                  ModernItemLifetime.Observation lifetimes) {
        Source(String resourceDatabase, ModernCharacterData.Snapshot snapshot, List<Instance> instances) {
            this(resourceDatabase, snapshot, instances, ModernItemLifetime.Observation.unqualified(snapshot));
        }
        Source {
            identifier(resourceDatabase);
            Objects.requireNonNull(snapshot); Objects.requireNonNull(instances);
            Objects.requireNonNull(lifetimes); lifetimes.validateSource(snapshot);
            instances = List.copyOf(instances);
            if (snapshot.character().characterClass() > 6 || instances.size() != snapshot.inventory().size()) throw invalid();
            long previous = 0;
            for (int index = 0; index < instances.size(); index++) {
                Instance item = Objects.requireNonNull(instances.get(index));
                if (item.source().objectId() <= previous || !item.source().equals(snapshot.inventory().get(index))) throw invalid();
                previous = item.source().objectId();
            }
        }
        Qualified qualify(Catalog catalog) {
            Objects.requireNonNull(catalog);
            if (snapshot.sourceId() != catalog.sourceId() || !resourceDatabase.equals(catalog.resourceDatabase())) throw invalid();
            Map<Long,Instance> items = new LinkedHashMap<>();
            for (Instance instance : instances) items.put(instance.source().objectId(),instance);
            return new Qualified(snapshot,items,catalog,lifetimes);
        }
        @Override public String toString() { return "ModernEquipmentSource[redacted]"; }
    }
    private ModernEquipmentSourceJdbc() {}

    static String sql(String resourceDatabase) {
        // Full-schema form, useful for inspection. read() always qualifies metadata first.
        return sql(resourceDatabase,OPTIONAL_WEAPON_RESISTANCES);
    }
    private static String sql(String resourceDatabase,Set<String> presentOptional) {
        return sql(resourceDatabase,presentOptional,false);
    }
    private static String sql(String resourceDatabase,Set<String> presentOptional,boolean combat) {
        identifier(resourceDatabase);
        if (!OPTIONAL_WEAPON_RESISTANCES.containsAll(presentOptional)) throw invalid();
        var weaponFields = new EnumMap<Stat,String>(Stat.class);
        WEAPON_BASE.forEach((stat,column) -> {
            if (!OPTIONAL_WEAPON_RESISTANCES.contains(column) || presentOptional.contains(column)) weaponFields.put(stat,column);
        });
        return "SELECT " + ModernCharacterDataJdbc.sourceProjection() + ",i.greater,"
                + "i.magic_stone_item_id,i.magic_stone_level,i.magic_stone_time,"
                + "ic.item_objid AS card_owner,ic.card_1,ic.card_2,ic.card_3,ic.card_4,ic.card_5,"
                + "d.item_objid AS dark_owner," + abilityProjection("d","dark",DARK) + ","
                + templateProjection("w",weaponFields,false) + "," + templateProjection("a",ARMOR_BASE,true)
                + (combat ? ModernCombatSourceJdbc.projection() : "")
                + ModernCharacterDataJdbc.sourceJoins(resourceDatabase)
                + " LEFT JOIN character_items_card ic ON ic.item_objid=i.id"
                + " LEFT JOIN character_items_dark d ON d.item_objid=i.id"
                + ModernCharacterDataJdbc.ownerPredicateAndOrder();
    }
    static Optional<Source> read(Connection connection, ModernBackendScope scope, int sourceId,
            String canonicalAccount, long objectId, String resourceDatabase) throws Exception {
        return readCombat(connection,scope,sourceId,canonicalAccount,objectId,resourceDatabase,null);
    }
    static Optional<Source> readCombat(Connection connection, ModernBackendScope scope, int sourceId,
            String canonicalAccount, long objectId, String resourceDatabase, ModernCombatSourceJdbc.Collector combat) throws Exception {
        Objects.requireNonNull(scope);
        scope.checkActive();
        try {
            identifier(resourceDatabase);
            if (connection == null || sourceId < 1 || objectId < 1 || !ModernCharacterData.validText(canonicalAccount)
                    || canonicalAccount.getBytes(StandardCharsets.UTF_8).length > 64
                    || ModernBackendScope.current() != scope || !connection.isReadOnly()) throw unavailable();
            Set<String> presentOptional = weaponOptionalColumns(connection,scope,resourceDatabase);
            var instances = new ArrayList<Instance>();
            Optional<ModernCharacterData.Snapshot> snapshot;
            try (PreparedStatement statement = connection.prepareStatement(sql(resourceDatabase,presentOptional,combat!=null))) {
                statement.setMaxRows(ModernCharacterData.MAX_ITEMS + 1);
                statement.setQueryTimeout((int)Math.max(1L,((long)scope.remainingMillis()+999L)/1000L));
                statement.setLong(1,objectId); statement.setString(2,canonicalAccount);
                try (ResultSet rows = statement.executeQuery()) {
                    snapshot = ModernCharacterDataJdbc.decode(rows,scope,sourceId,objectId,(row,item) -> {
                        Instance qualified = instance(row,item);
                        instances.add(qualified);
                        if (combat != null) combat.accept(row,qualified);
                    });
                }
            }
            Optional<Source> result = snapshot.isEmpty() ? Optional.empty()
                    : Optional.of(new Source(resourceDatabase,snapshot.orElseThrow(),instances,
                            ModernItemLifetimeJdbc.read(connection,scope,snapshot.orElseThrow(),canonicalAccount,resourceDatabase)));
            scope.checkActive();
            return result;
        } catch (Exception failure) {
            scope.checkActive();
            // No driver message, cause or suppressed exception may disclose DB/account data.
            throw unavailable();
        }
    }
    private static Set<String> weaponOptionalColumns(Connection connection,ModernBackendScope scope,String resourceDatabase) throws Exception {
        Set<String> columns = new HashSet<>();
        scope.checkActive();
        try (PreparedStatement statement = connection.prepareStatement(WEAPON_SCHEMA_SQL)) {
            statement.setMaxRows(MAX_METADATA_COLUMNS+1);
            statement.setQueryTimeout((int)Math.max(1L,((long)scope.remainingMillis()+999L)/1000L));
            statement.setString(1,resourceDatabase);statement.setString(2,"weapon");
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    scope.checkActive();
                    String column = rows.getString("COLUMN_NAME");
                    long bytes = number(rows,"COLUMN_NAME_BYTES");
                    if (bytes < 1 || bytes > 256 || !ModernCharacterData.validText(column)
                            || column.getBytes(StandardCharsets.UTF_8).length != bytes
                            || !column.equals(column.strip()) || columns.size() == MAX_METADATA_COLUMNS
                            || !columns.add(column.toLowerCase(Locale.ROOT))) throw unavailable();
                }
            }
        }
        // No rows means unavailable table/metadata permission, not proof that an
        // otherwise qualified weapon catalog has no optional resistance columns.
        if (columns.isEmpty() || !columns.contains("item_id")) throw unavailable();
        columns.retainAll(OPTIONAL_WEAPON_RESISTANCES);
        for (String column : OPTIONAL_WEAPON_RESISTANCES.stream().sorted().toList()) {
            if (!columns.contains(column)) verifyOptionalColumnAbsent(connection,scope,resourceDatabase,column);
        }
        scope.checkActive();
        return Set.copyOf(columns);
    }
    private static void verifyOptionalColumnAbsent(Connection connection,ModernBackendScope scope,
            String database,String column) throws Exception {
        identifier(database);
        if (!OPTIONAL_WEAPON_RESISTANCES.contains(column)) throw invalid();
        scope.checkActive();
        String query = "SELECT `"+column+"` FROM `"+database+"`.`weapon` LIMIT 0";
        try {
            try (PreparedStatement statement = prepareOptionalGuard(connection,query)) {
                statement.setMaxRows(1);
                statement.setQueryTimeout((int)Math.max(1L,((long)scope.remainingMillis()+999L)/1000L));
                try (ResultSet rows = executeOptionalGuard(statement)) {
                    if (rows.next()) throw unavailable();
                    scope.checkActive();
                }
            }
            // Successful lookup contradicts metadata: visibility/schema changed.
            // Do not silently read the value or certify a consistent generation.
            throw unavailable();
        } catch (MissingOptionalColumn absent) {
            scope.checkActive();
            if (absent.getSuppressed().length != 0 || absent.sql.getSuppressed().length != 0) throw unavailable();
        }
    }
    private static PreparedStatement prepareOptionalGuard(Connection connection,String query) throws SQLException,MissingOptionalColumn {
        try { return connection.prepareStatement(query); }
        catch (SQLException failed) { throwOptionalAbsence(failed); throw failed; }
    }
    private static ResultSet executeOptionalGuard(PreparedStatement statement) throws SQLException,MissingOptionalColumn {
        try { return statement.executeQuery(); }
        catch (SQLException failed) { throwOptionalAbsence(failed); throw failed; }
    }
    private static void throwOptionalAbsence(SQLException failure) throws MissingOptionalColumn {
        if (failure.getErrorCode() == 1054 && "42S22".equals(failure.getSQLState())) throw new MissingOptionalColumn(failure);
    }
    /** A query-phase marker only: setup/ResultSet/statement-close failures cannot certify absence. */
    private static final class MissingOptionalColumn extends Exception {
        final SQLException sql;
        MissingOptionalColumn(SQLException sql) { super("optional equipment column candidate absent"); this.sql=sql; }
    }
    private static Instance instance(ResultSet row, ModernCharacterData.InventoryItem item) throws Exception {
        // CharacterItemPersistenceService and the retained loader read these
        // nullable-default-zero extension fields via getInt / getIntOrDefault.
        long greater = nullableExtensionNumber(row,"greater");
        var darkOwner = ModernJdbcNumbers.nullable(row,"dark_owner");
        boolean darkAbsent = darkOwner.isEmpty();
        if (!darkAbsent && darkOwner.getAsLong() != item.objectId()) throw unavailable();
        RuleValue dark = darkAbsent ? RuleValue.absent() : RuleValue.present(darkAbility(row));
        var cardOwner = ModernJdbcNumbers.nullable(row,"card_owner");
        boolean cardAbsent = cardOwner.isEmpty();
        if (!cardAbsent && cardOwner.getAsLong() != item.objectId()) throw unavailable();
        var cards = new ArrayList<Long>(5);
        // CharacterInlayService and Legacy's explicit COALESCE give NULL slots
        // a defined empty-slot meaning. The owner key remains strictly verified.
        for (int index = 1; index <= 5; index++) cards.add(cardAbsent ? 0L : nullableExtensionNumber(row,"card_"+index));
        Timestamp expiry = row.getTimestamp("magic_stone_time");
        if (row.wasNull() != (expiry == null)) throw unavailable();
        // The model is exact milliseconds; reject extra fractional precision rather than truncate it.
        if (expiry != null && expiry.getNanos() % 1_000_000 != 0) throw unavailable();
        var stone = new Stone(nullableExtensionNumber(row,"magic_stone_item_id"),nullableExtensionNumber(row,"magic_stone_level"),
                expiry == null ? OptionalLong.empty() : OptionalLong.of(expiry.toInstant().toEpochMilli()));
        Template template;
        if (item.template().kind() == ModernCharacterData.ItemKind.ETC) {
            // ItemTable explicitly constructs all exact etcitem templates as type2==0.
            // This only proves no weapon/armor slot; it never grants use/toggle or
            // claims the activation marker or VIP/card effects are absent.
            template = new Template(item.template(),EquipmentRules.NonEquipmentFacts.INSTANCE,0,0,List.of(),
                    Ability.zero(),TemplatePhysical.zero());
        } else {
            boolean armor = item.template().kind() == ModernCharacterData.ItemKind.ARMOR;
            String alias = armor ? "a" : "w";
            String typeName = boundedText(row,alias+"_type",32,false);
            int mask = 0;
            for (int bit = 0; bit < CLASS_COLUMNS.length; bit++) if (classAllowed(row,alias+"_"+CLASS_COLUMNS[bit])) mask |= 1<<bit;
            long minimum = number(row,alias+"_min_lvl"), maximum = number(row,alias+"_max_lvl");
            if (minimum < 0 || maximum < 0) throw unavailable();
            // C_ItemUSe explicitly interprets min/max 0 as no bound.
            var range = new EquipmentRules.LevelRange(minimum == 0 ? 1 : minimum,maximum == 0 ? Long.MAX_VALUE : maximum);
            EquipmentRules.Facts rules;
            int type;
            if (armor) {
                var slot = ARMOR_TYPES.get(typeName);
                if (slot == null) throw unavailable();
                type = slot.retainedType(); rules = new EquipmentRules.ArmorFacts(mask,range,slot);
            } else {
                Integer matched = WEAPON_TYPES.get(typeName);
                if (matched == null) throw unavailable();
                type = matched; rules = new EquipmentRules.WeaponFacts(mask,range,TWO_HANDED.contains(type));
            }
            TemplatePhysical physical = new TemplatePhysical(number(row,alias+"_physical_melee_hit"),
                    number(row,alias+"_physical_melee_damage"),number(row,alias+"_physical_ranged_hit"),
                    number(row,alias+"_physical_ranged_damage"),number(row,alias+"_physical_reduction"),number(row,alias+"_physical_weight"));
            template = new Template(item.template(),rules,type,number(row,alias+"_safenchant"),
                    setIds(row,alias+"_set_ids"),ability(row,alias+"_base"),physical);
        }
        return new Instance(item,template,greater,dark,cards,stone);
    }
    private static String templateProjection(String alias, Map<Stat,String> fields, boolean armor) {
        var parts = new ArrayList<String>();
        parts.add("LEFT("+alias+".type,33) AS "+alias+"_type");
        parts.add("OCTET_LENGTH("+alias+".type) AS "+alias+"_type_bytes");
        parts.add("LEFT("+alias+".set_id,4097) AS "+alias+"_set_ids");
        parts.add("OCTET_LENGTH("+alias+".set_id) AS "+alias+"_set_ids_bytes");
        for (String column : CLASS_COLUMNS) parts.add(alias+"."+column+" AS "+alias+"_"+column);
        for (String column : List.of("min_lvl","max_lvl","safenchant")) parts.add(alias+"."+column+" AS "+alias+"_"+column);
        parts.add(abilityProjection(alias,alias+"_base",fields));
        parts.add(alias+"."+(armor?"hit_modifier":"hitmodifier")+" AS "+alias+"_physical_melee_hit");
        parts.add(alias+"."+(armor?"dmg_modifier":"dmgmodifier")+" AS "+alias+"_physical_melee_damage");
        String[] physical = {"bow_hit_modifier","bow_dmg_modifier","damage_reduction","weight_reduction"};
        String[] aliases = {"ranged_hit","ranged_damage","reduction","weight"};
        for (int index = 0; index < physical.length; index++) parts.add((armor?alias+"."+physical[index]:"0")+" AS "+alias+"_physical_"+aliases[index]);
        return String.join(",",parts);
    }
    private static String abilityProjection(String alias,String prefix,Map<Stat,String> fields) {
        var values = new ArrayList<String>();
        for (Stat stat : Stat.values()) values.add((fields.containsKey(stat)?alias+".`"+fields.get(stat)+"`":"0")+" AS "+prefix+"_"+stat.name());
        return String.join(",",values);
    }
    private static Ability ability(ResultSet row,String prefix) throws SQLException {
        var values = new EnumMap<Stat,Long>(Stat.class);
        for (Stat stat : Stat.values()) values.put(stat,number(row,prefix+"_"+stat.name()));
        return new Ability(values);
    }
    private static Ability darkAbility(ResultSet row) throws SQLException {
        var values = new EnumMap<Stat,Long>(Stat.class);
        // Legacy's per-column COALESCE and CharacterDarkItemService.safeSet
        // establish NULL-as-zero only for these explicitly mapped dark fields.
        for (Stat stat : Stat.values()) values.put(stat,DARK.containsKey(stat)
                ? nullableExtensionNumber(row,"dark_"+stat.name()) : number(row,"dark_"+stat.name()));
        return new Ability(values);
    }
    private static List<Long> setIds(ResultSet row,String column) throws SQLException {
        String value = row.getString(column);
        if (value == null) {
            if (ModernJdbcNumbers.nullable(row,column+"_bytes").isPresent()) throw unavailable();
            // ItemTable/L1Item.setSetId and Legacy.parseSetIds: queried NULL is
            // a verified no-set marker, not an unqualified/missing SQL column.
            return List.of();
        }
        return setIds(boundedText(row,column,MAX_SET_TEXT,true));
    }
    private static List<Long> setIds(String value) throws SQLException {
        if (value.isBlank() || value.equals("0")) return List.of();
        String[] tokens = value.split(",",-1);
        if (tokens.length > MAX_SET_IDS_PER_ITEM) throw unavailable();
        var result = new ArrayList<Long>();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (!trimmed.matches("[0-9]{1,19}")) throw unavailable();
            long id;
            try { id = Long.parseLong(trimmed); } catch (NumberFormatException invalid) { throw unavailable(); }
            if (id < 1) throw unavailable();
            result.add(id);
        }
        return List.copyOf(result);
    }
    private static String boundedText(ResultSet row,String column,int limit,boolean allowEmpty) throws SQLException {
        String value = row.getString(column); long bytes = number(row,column+"_bytes");
        if (value == null || bytes < 0 || bytes > limit || value.getBytes(StandardCharsets.UTF_8).length != bytes
                || !allowEmpty && !ModernCharacterData.validText(value)) throw unavailable();
        // Type/set grammar is ASCII; no decoder replacements, NUL or hidden controls.
        for (int index = 0; index < value.length(); index++) if (value.charAt(index) < 32 || value.charAt(index) > 126) throw unavailable();
        return value;
    }
    private static long number(ResultSet row,String column) throws SQLException {
        return ModernJdbcNumbers.required(row,column);
    }
    private static long nullableExtensionNumber(ResultSet row,String column) throws SQLException {
        return ModernJdbcNumbers.nullable(row,column).orElse(0L);
    }
    private static boolean classAllowed(ResultSet row,String column) throws SQLException {
        // ItemTable uses != 0; the source columns are UNSIGNED NOT NULL integers,
        // not BOOLEAN. Preserve positive i64 flags without narrowing to native int.
        long value = number(row,column); if (value < 0) throw unavailable(); return value != 0;
    }
    private static Map<Stat,String> fields(String specification) {
        var result = new EnumMap<Stat,String>(Stat.class);
        for (String pair : specification.split(",")) { String[] split = pair.split(":"); result.put(Stat.valueOf(split[0]),split[1]); }
        return Map.copyOf(result);
    }
    private static Map<Stat,String> extend(Map<Stat,String> base,Map<Stat,String> extra) {
        var result = new EnumMap<Stat,String>(Stat.class); result.putAll(base); result.putAll(extra); return Map.copyOf(result);
    }
    private static void identifier(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_]{1,64}")) throw invalid();
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid modern equipment source"); }
    private static SQLException unavailable() { return new SQLException("modern equipment source unavailable"); }
}
