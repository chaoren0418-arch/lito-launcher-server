package local.lito381.server;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/** Common skills numerical snapshot; no account/character writes or per-hit SQL. */
final class CombatDatabase {
    private static final java.util.Set<String> NUMERIC_FIELDS=java.util.Set.of(
            "mpCost","hpCost","reuseDelayMillis","durationSeconds",
            "baseDamage","baseDiceCount","dieSides","diceCount","itemConsumeId","itemConsumeCount",
            "probabilityDice","probabilityValue");
    record Skill(int mp, int hp, int reuse, int duration, int damage, int die,
                 int dice, int reagent, int reagentCount,int probabilityDice,int probabilityValue) {
        Skill(int mp,int hp,int reuse,int duration,int damage,int die,int dice,int reagent,int reagentCount) {
            this(mp,hp,reuse,duration,damage,die,dice,reagent,reagentCount,0,0);
        }
        Skill withBasics(int mp,int hp,int reuse,int duration) {
            return new Skill(mp,hp,reuse,duration,damage,die,dice,reagent,reagentCount,probabilityDice,probabilityValue);
        }
        Skill withProbability(int dice,int value) {return new Skill(mp,hp,reuse,duration,damage,die,this.dice,reagent,reagentCount,dice,value);}
        int field(String name) {
            return switch(name) {
                case "mpCost" -> mp;case "hpCost" -> hp;
                case "reuseDelayMillis" -> reuse;case "durationSeconds" -> duration;
                case "baseDamage", "baseDiceCount" -> damage;
                case "dieSides" -> die;case "diceCount" -> dice;
                case "itemConsumeId" -> reagent;case "itemConsumeCount" -> reagentCount;
                case "probabilityDice" -> probabilityDice;case "probabilityValue" -> probabilityValue;
                default -> throw new IllegalArgumentException("unbound skills column: "+name);
            };
        }
    }
    record Snapshot(Map<Integer, Skill> skills,SourceSkillControls.Snapshot controls) {
        Snapshot { skills = Map.copyOf(skills); }
        Snapshot withSkills(Map<Integer,Skill> replacement) { return new Snapshot(replacement,controls); }
    }
    private static volatile Snapshot current;
    static synchronized void load(ServerConfig config) throws Exception {
        if (current != null) return;
        // Some baseline record constructors validate other records via accessors.
        // Initialize their structural/native contracts before publishing dynamic
        // overrides, so an edited SQL MP cost cannot break lazy catalog init.
        Class.forName(BattleSpellCatalog.class.getName(), true, CombatDatabase.class.getClassLoader());
        var p = UnifiedConfiguration.read(config.source());
        String db = SharedDatabase.identifier(p.getProperty("database.static.database"));
        CombatRules.load(config);
        Map<Integer, Skill> skills = new HashMap<>();
        final SourceSkillControls.Snapshot controls;
        try (var c = DriverManager.getConnection(p.getProperty("database.jdbc.base.url") + db
                + p.getProperty("database.jdbc.options"), p.getProperty("database.user"),
                p.getProperty("database.password"))) {
            c.setReadOnly(true);
            c.setAutoCommit(false);
            c.setTransactionIsolation(java.sql.Connection.TRANSACTION_REPEATABLE_READ);
            try (var st = c.createStatement(); var r = st.executeQuery(
                    "SELECT skill_id,mpConsume,hpConsume,reuseDelay,buffDuration,damage_value,damage_dice,damage_dice_count,itemConsumeId,itemConsumeCount,probability_dice,probability_value FROM skills")) {
                while (r.next()) {
                    int id = integer(r, "skill_id", 1, 1000000);
                    Skill row = new Skill(integer(r, "mpConsume", 0, 65535),
                            integer(r, "hpConsume", 0, 65535),
                            integer(r, "reuseDelay", 0, 86400000),
                            integer(r, "buffDuration", 0, 31536000),
                            integer(r, "damage_value", 0, 1000000),
                            integer(r, "damage_dice", 0, 65535),
                            integer(r, "damage_dice_count", 0, 1000),
                            integer(r, "itemConsumeId", 0, Integer.MAX_VALUE),
                            integer(r, "itemConsumeCount", 0, 1000000),integer(r,"probability_dice",0,65535),
                            integer(r,"probability_value",Integer.MIN_VALUE,Integer.MAX_VALUE));
                    if (skills.putIfAbsent(id, row) != null)
                        throw new SQLException("skills: duplicate skill_id=" + id);
                }
            }
            controls=SourceSkillControls.read(c);
            c.rollback(); // Read transaction only; make the write boundary explicit.
        }
        if (skills.isEmpty()) throw new SQLException(db + ".skills is empty");
        validateConsumers(skills);
        current = new Snapshot(skills,controls);
        System.out.println("[LiTo] common combat skills loaded: " + skills.size()
                + "; columns=consumption,cooldown,duration,damage,dice,materials; skill208 time=2 source executor");
        System.out.println("[LiTo] source control snapshots: probabilities="+controls.probabilities().size()
                +" bosses="+controls.bossModifiers().size()+" ability-skills="+controls.abilities().size()
                +" npc-attributes="+controls.npcAttributes().size()
                +" source-coexist="+controls.skillCoexist()+" cancellation-immune="+controls.cancellationImmune().size()
                +" source-eyes="+controls.eyes().size()
                +"; probability actor bindings partial; configured primary/special BUFF fields installed; other types/producers/paths pending");
    }
    private static void validateConsumers(Map<Integer,Skill> skills) throws Exception {
        if(!skills.containsKey(5))throw new SQLException("skills: missing defined teleport skill_id=5");
        java.util.Set<String> seen=new java.util.HashSet<>();
        for(var f:BattleSpellCatalog.class.getDeclaredFields()) {
            if(!java.lang.reflect.Modifier.isStatic(f.getModifiers()) || !Map.class.isAssignableFrom(f.getType()))continue;
            f.setAccessible(true);
            for(Object consumer:((Map<?,?>)f.get(null)).values()) {
                if(consumer==null)continue;
                var type=consumer.getClass();
                final java.lang.reflect.Method getter;
                try { getter=type.getDeclaredMethod("skillId"); }
                catch(NoSuchMethodException ignored) { continue; }
                getter.setAccessible(true);int id=(int)getter.invoke(consumer);
                if(!seen.add(id+":"+type.getSimpleName()))continue;
                if(type.getSimpleName().equals("ReservedSkillSlot"))continue;
                Skill row=skills.get(id);
                if(row==null)throw new SQLException("skills: missing defined catalog skill_id="+id+" type="+type.getSimpleName());
                for(String field:java.util.List.of("mpCost","hpCost","durationSeconds","dieSides","itemConsumeCount","probabilityDice")) {
                    try {
                        var method=type.getDeclaredMethod(field);method.setAccessible(true);
                        int original=((Number)method.invoke(consumer)).intValue();
                        int configured=row.field(field);
                        int max=field.equals("durationSeconds") || field.equals("dieSides") || field.equals("probabilityDice") ? 65535
                                : field.equals("itemConsumeCount") ? 1000000 : 32767;
                        if(configured>max || field.equals("durationSeconds")&&original>0&&configured==0
                                || (field.equals("dieSides")||field.equals("probabilityDice"))&&original>0&&configured==0)
                            throw new SQLException("skills skill_id="+id+" type="+type.getSimpleName()+": unsupported consumer boundary "+field+"="+configured);
                    } catch(NoSuchMethodException ignored) { }
                }
            }
        }
    }
    private static int integer(ResultSet r, String key, int min, int max) throws SQLException {
        String text = r.getString(key);
        try {
            int n = Integer.parseInt(text);
            if (n < min || n > max) throw new NumberFormatException();
            return n;
        } catch (RuntimeException e) {
            throw new SQLException("skills skill_id=" + r.getString("skill_id")
                    + ": invalid " + key + "=" + text, e);
        }
    }
    static boolean bound(String field) {
        return NUMERIC_FIELDS.contains(field);
    }
    static int value(int original, int id, String type, String field) {
        Snapshot s = current;
        if (s == null || !bound(field)) return original;
        // BONE_BREAK.execute sets time=2; SQL buffDuration is not its stun lifetime.
        if (id == 208 && type.equals("BoneBreakSpell") && field.equals("durationSeconds")) return 2;
        Skill row = s.skills().get(id);
        if (row == null && type.equals("ReservedSkillSlot")) return original;
        if (row == null) throw new IllegalStateException("missing skills snapshot row: skill_id="+id+" type="+type);
        return row.field(field);
    }
    static long longValue(long original,int id,String field) {
        if(current==null)return original;
        return value(Math.toIntExact(original),id,"long-material",field);
    }
    static boolean consumeMaterial(RuntimeInventory inventory,long id,long count) {
        // Source L1SkillUse explicitly skips materials when itemConsumeId=0.
        // Positive-ID/zero-count still fails, as commitRequiredMaterialHeld does.
        if(id==0)return true;
        return inventory.removeCatalog(id,count);
    }
    static SourceSkillControls.Snapshot controls() {
        var s=current;if(s==null)throw new IllegalStateException("source skill controls not loaded");return s.controls();
    }
    static SourceSkillControls.Snapshot controlsOrNull() {var s=current;return s==null?null:s.controls();}
}
