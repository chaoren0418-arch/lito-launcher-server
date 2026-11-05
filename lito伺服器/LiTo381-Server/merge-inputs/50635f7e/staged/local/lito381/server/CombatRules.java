// ported from candidate 50635f7e; B07 三方移植（base-0.6.71 / candidate / src/main）。
package local.lito381.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Read-only source controls. Publication occurs only after all inputs validate. */
final class CombatRules {
    record Snapshot(Map<String, Double> values, Path skillFile, Path damageFile, Path characterFile,Set<String> defaultedKeys,Path mapDirectory) {
        Snapshot { values = Map.copyOf(values);defaultedKeys=Set.copyOf(defaultedKeys); }
        Snapshot(Map<String,Double> values,Path skillFile,Path damageFile,Path characterFile,Set<String> defaults) {this(values,skillFile,damageFile,characterFile,defaults,null);}
        Snapshot(Map<String,Double> values,Path skillFile,Path damageFile,Set<String> defaults) {this(values,skillFile,damageFile,null,defaults);}
        Snapshot(Map<String,Double> values,Path skillFile,Path damageFile) {this(values,skillFile,damageFile,null,Set.of());}
    }
    private static volatile Snapshot current;
    // Only keys with installed consumers are included here. Remaining source controls
    // are tracked by the coverage audit, never reported as implemented by this loader.
    static final Set<String> SKILL_KEYS = Set.of("Triple_Arrow_Dmg", "SLAY_BREAK_NOT_TELEPORT", "INVIS_CAST_BREAK", "SOLID_CARRIAGE_MODE", "NATURES_BLESSING_HEAL", "ILLUSION_AVATAR_DAMAGE_RATE", "IMMUNE_TO_HARM", "BURNING_CHANCE", "BURNING_DMG", "DOUBLE_BREAK_CHANCE", "DOUBLE_BREAK_DMG", "ELEMENTAL_FIRE", "ELEMENTAL_FIRE_RND", "SOUL_OF_FLAME_DAMAGE", "STRIKER_DMG2", "JOY_OF_PAIN_COUNTDMG", "JOY_OF_PAIN_DMG", "COUNTER_BARRIER_DMG", "DISINTEGRATE_CRITICAL_RND", "CRITICAL_SPR", "CURSE_READY_TIME", "CURSE_TIME", "BLOODY_SOULADDMP", "SHOCK_STUN_TIMER", "SLAY_BREAK_TIMER", "MORTAL_BODY_DMG", "MORTAL_BODY_ROM", "FOE_SLAYER_DMG", "VULNERABILITY_ROM", "VULNERABILITY_1", "VULNERABILITY_2", "VULNERABILITY_3");
    static final Set<String> DAMAGE_KEYS;
    static {var keys=new java.util.HashSet<>(CombatClassDamage.keys());keys.addAll(CombatMagicDamage.keys());keys.add(CombatKiringku.MR_KEY);keys.add(CombatCompanionDamage.KEY);keys.add("AC_REDUCTION_DIVISOR");keys.add("PET_TO_PC_DAMAGE_MULTIPLIER");DAMAGE_KEYS=Set.copyOf(keys);}
    static final String AWAKE_CLASSIC_MODE = "DragonKnightAwakeClassicMode";

    static synchronized void load(ServerConfig config) throws IOException {
        if (current != null) return;
        Path entry = NativeCombatConfigOrigin.entry(config);
        Properties paths = read(entry);
        if (!paths.stringPropertyNames().equals(Set.of("source.skill.file", "source.damage.file","source.character.file","source.map.directory","arena.bot.con","arena.bot.wis")))
            throw new IOException(entry + ": requires source.skill.file, source.damage.file, source.character.file, source.map.directory, arena.bot.con, arena.bot.wis only");
        Path skill = resolve(entry, paths.getProperty("source.skill.file"));
        Path damage = resolve(entry, paths.getProperty("source.damage.file"));
        Path character = resolve(entry, paths.getProperty("source.character.file"));
        Path maps=resolve(entry,paths.getProperty("source.map.directory"));
        if(!Files.isDirectory(maps))throw new IOException("missing source map directory: "+maps);
        var source=snapshot(read(skill), read(damage), skill, damage);
        var combined=new HashMap<>(source.values());
        var defaults=new java.util.HashSet<>(source.defaultedKeys());
        var destination = combined;
        String awakeKey = AWAKE_CLASSIC_MODE;
        Properties characterProperties = read(character);
        NativePetControls.loadFromProperties(characterProperties, combined, defaults, character);
        destination.put(awakeKey, awakeClassicMode(characterProperties, character, defaults) ? 1.0 : 0.0);
        parse(combined,paths,"arena.bot.con",entry);parse(combined,paths,"arena.bot.wis",entry);
        current = NativeSourceOtherControls.prepare(new Snapshot(combined,skill,damage,character,defaults,maps));
        System.out.println("[LiTo] source combat controls loaded: " + current.values().size()
                + "; coverage is limited to installed consumers (see combat coverage audit)");
        if(!current.defaultedKeys().isEmpty())System.out.println("[LiTo] source loader defaults used: "+current.defaultedKeys());
    }

    // ConfigCharSetting prefers the new key; otherwise both legacy restrictions
    // must be true. Its loose parser warns and defaults malformed values to true.
    static boolean awakeClassicMode(Properties p,Path file,Set<String> defaults) {
        String raw=p.getProperty(AWAKE_CLASSIC_MODE);
        if(raw!=null&&!raw.isBlank())return sourceBoolean(p,AWAKE_CLASSIC_MODE,file,defaults);
        boolean skill=sourceBoolean(p,"DragonKnightAwakeSkillRestrict",file,defaults);
        boolean poly=sourceBoolean(p,"DragonKnightAwakePolyRestrict",file,defaults);
        return skill&&poly;
    }
    private static boolean sourceBoolean(Properties p,String key,Path file,Set<String> defaults) {
        String raw=p.getProperty(key);
        if(raw!=null&&raw.trim().equalsIgnoreCase("false"))return false;
        if(raw!=null&&raw.trim().equalsIgnoreCase("true"))return true;
        defaults.add(key);
        if(raw!=null&&!raw.isBlank())System.out.println("[LiTo] "+file+": invalid boolean "+key+"; source default=true");
        return true;
    }

    static Snapshot nativeSourceSkillDefaultsOriginalSnapshot(Properties properties, Properties properties2, Path path, Path path2) throws IOException {
        Map<String, Double> hashMap = new HashMap<String, Double>();
        java.util.HashSet<String> hashSet = new java.util.HashSet<String>();
        for (String string : SKILL_KEYS) {
            boolean bl;
            if (string.equals("Triple_Arrow_Dmg")) {
                CombatClassDamage.parse(hashMap, properties, string, path, hashSet, 1.0);
            } else if (string.equals("SLAY_BREAK_NOT_TELEPORT")) {
                String string2 = properties.getProperty(string);
                bl = true;
                if (string2 == null || string2.isBlank()) {
                    hashSet.add(string);
                } else {
                    bl = Boolean.parseBoolean(string2.trim());
                }
                hashMap.put(string, bl ? 1.0 : 0.0);
            } else if (string.equals("INVIS_CAST_BREAK")) {
                String string3 = properties.getProperty(string);
                bl = false;
                if (string3 == null || string3.isBlank()) {
                    hashSet.add(string);
                } else {
                    bl = Boolean.parseBoolean(string3.trim());
                }
                hashMap.put(string, bl ? 1.0 : 0.0);
            } else if (string.equals("SOLID_CARRIAGE_MODE")) {
                int n = 1;
                String string4 = properties.getProperty(string);
                try {
                    if (string4 == null || string4.isBlank()) {
                        throw new NumberFormatException();
                    }
                    n = Integer.parseInt(string4.trim());
                }
                catch (NumberFormatException numberFormatException) {
                    hashSet.add(string);
                }
                hashMap.put(string, Double.valueOf(n));
            } else if (string.equals("NATURES_BLESSING_HEAL")) {
                hashMap.put(string, 0.0);
                hashSet.add(string);
            } else if (string.equals("ILLUSION_AVATAR_DAMAGE_RATE")) {
                CombatClassDamage.parse(hashMap, properties, string, path, hashSet, 1.5);
                if (hashMap.get(string) < 0.0) throw new IOException(String.valueOf(path) + ": negative Avatar incoming rate requires signed fatal-commit support");
            } else if (string.equals("MORTAL_BODY_ROM") && !properties.containsKey(string)) {
                hashMap.put(string, 23.0);
                hashSet.add(string);
            } else if (string.endsWith("_TIMER")) {
                CombatRules.parseTimer(hashMap, properties, string, path);
            } else CombatRules.parse(hashMap, properties, string, path);
        }
        for (String string : DAMAGE_KEYS) {
            if (string.equals("PC_TO_PET_DAMAGE_MULTIPLIER")) {
                CombatClassDamage.parse(hashMap, properties2, string, path2, hashSet, 0.125);
            } else if (string.equals("MR_REDUCTION_PER_10")) {
                CombatClassDamage.parse(hashMap, properties2, string, path2, hashSet, 3.0);
            } else if (CombatClassDamage.keys().contains(string) || CombatMagicDamage.keys().contains(string)) {
                CombatClassDamage.parse(hashMap, properties2, string, path2, hashSet);
            } else CombatRules.parse(hashMap, properties2, string, path2);
        }
        return new Snapshot(hashMap, path, path2, hashSet);
    }
    private static void parseTimer(Map<String, Double> out, Properties p, String key, Path file)
            throws IOException {
        String value=p.getProperty(key, "");
        String[] parts=value.split(",",-1);
        try {
            if(parts.length!=2)throw new NumberFormatException();
            int base=Integer.parseInt(parts[0].trim()), random=Integer.parseInt(parts[1].trim());
            if(base<1 || random<0 || (long)base+random>65535)throw new NumberFormatException();
            out.put(key+".base",(double)base);out.put(key+".random",(double)random);
        } catch(NumberFormatException e) { throw new IOException(file+": invalid timer "+key+"="+value,e); }
    }

    private static void parse(Map<String, Double> out, Properties p, String key, Path file)
            throws IOException {
        String text = p.getProperty(key);
        if (text == null || text.isBlank()) throw new IOException(file + ": missing " + key);
        final double value;
        try { value = Double.parseDouble(text.trim()); }
        catch (NumberFormatException e) { throw new IOException(file + ": invalid " + key, e); }
        boolean integer = key.endsWith("CHANCE") || key.endsWith("_RND")
                || key.equals("NATURES_BLESSING_HEAL") || key.equals("JOY_OF_PAIN_COUNTDMG")
                || key.equals("JOY_OF_PAIN_DMG") || key.equals("CRITICAL_SPR")
                || key.equals("CURSE_READY_TIME") || key.equals("CURSE_TIME") || key.equals("BLOODY_SOULADDMP")
                || key.equals("MORTAL_BODY_DMG") || key.equals("MORTAL_BODY_ROM")
                || key.equals("FOE_SLAYER_DMG") || key.startsWith("VULNERABILITY_")
                || key.startsWith("arena.bot.");
        double max = key.equals("NATURES_BLESSING_HEAL") || key.equals("JOY_OF_PAIN_DMG") || key.equals("MORTAL_BODY_DMG")
                || key.equals("FOE_SLAYER_DMG") || key.matches("VULNERABILITY_[123]") ? 100000
                : key.equals("CRITICAL_SPR") ? 65535 : key.startsWith("arena.bot.") ? 254
                : key.endsWith("CHANCE") || key.endsWith("_RND") || key.equals("MORTAL_BODY_ROM") || key.equals("VULNERABILITY_ROM") ? 100 : 1000;
        double min = key.equals("IMMUNE_TO_HARM") ? 0.001
                : key.equals("JOY_OF_PAIN_COUNTDMG") || key.equals("CURSE_TIME") || key.startsWith("arena.bot.") ? 1 : 0;
        if (!Double.isFinite(value) || value < min || value > max
                || integer && value != Math.rint(value))
            throw new IOException(file + ": out of range " + key + "=" + text);
        out.put(key, value);
    }

    static Properties read(Path file) throws IOException {
        Properties p = new Properties() {
            @Override public synchronized Object put(Object key, Object value) {
                if (containsKey(key)) throw new IllegalArgumentException("duplicate key: " + key);
                return super.put(key, value);
            }
        };
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { p.load(reader); }
        catch (IllegalArgumentException e) { throw new IOException(file + ": " + e.getMessage(), e); }
        return p;
    }

    private static Path resolve(Path entry, String value) throws IOException {
        if (value == null || value.isBlank()) throw new IOException(entry + ": empty source path");
        return entry.getParent().resolve(value.trim()).normalize();
    }

    static double number(String key, double isolatedLegacyValue) {
        Snapshot s = current;
        // Legacy isolated regression/diagnostic cores do not load SQL or this profile.
        // Production always loads both snapshots before exposing listeners.
        if (s == null) return isolatedLegacyValue;
        Double value = s.values().get(key);
        if (value == null) throw new IllegalArgumentException("unbound combat control: " + key);
        return value;
    }
    static double avatarIncomingRate(double legacy,int remainingSeconds){return remainingSeconds>0?number("ILLUSION_AVATAR_DAMAGE_RATE",legacy):legacy;}
    static boolean loaded() {return current!=null;}
    /** Source predicates use L1Item.getType1; native physical weapons carry getType. */
    static int weaponActionType(int subtype) {
        if(!loaded())return subtype;
        return subtype==4||subtype==13?20:subtype==10?62:subtype;
    }
    static int strikerBeforeClass(int damage,boolean active,boolean ranged) {
        return loaded()?damage:BattleStrikerGale.damageAfterStatus(damage,active,ranged);
    }
    static int strikerAfterClass(int damage,boolean active) {
        return loaded()?BattleStrikerGale.damageAfterStatus(damage,active,true):damage;
    }
    static Path sourceMapDirectory(){if(current==null||current.mapDirectory()==null)throw new IllegalStateException("source maps are not configured");return current.mapDirectory();}

    static int naturesBlessing(int lawfulHealing) {
        return lawfulHealing + (int) number("NATURES_BLESSING_HEAL", 0);
    }
    static double replace(double original, String key) { return number(key, original); }
    static int replaceInt(int original, String key) { return (int) number(key, original); }
    static int intField(int original, int id, String field) {
        if(current!=null&&id==187) {
            if(field.equals("hitCount"))return 5; // current player FOE_SLAYER.start loop
            if(field.equals("exposureDamageStep"))return 0; // no per-strike flat exposure bonus
        }
        String key = switch(id) {
            case 77 -> switch(field) {
                case "specialCriticalChancePercent" -> "DISINTEGRATE_CRITICAL_RND";
                case "specialCriticalGfx" -> "CRITICAL_SPR";
                default -> null;
            };
            case 146 -> field.equals("manaGain") ? "BLOODY_SOULADDMP" : null;
            case 191 -> field.equals("baseReflectionDamage") ? "MORTAL_BODY_DMG" : null;
            case 33 -> switch(field) {
                case "warningDelayMillis" -> "CURSE_READY_TIME";
                case "paralysisDurationMillis" -> "CURSE_TIME";
                default -> null;
            };
            case 87 -> switch(field) {
                case "baseStunSeconds" -> "SHOCK_STUN_TIMER.base";
                case "randomStunSeconds" -> "SHOCK_STUN_TIMER.random";
                default -> null;
            };
            case 192 -> field.equals("minimumBindSeconds") ? "SLAY_BREAK_TIMER.base" : null;
            default -> null;
        };
        if (id == 192 && field.equals("maximumBindSeconds"))
            return current==null ? original : (int)(number("SLAY_BREAK_TIMER.base",0)+number("SLAY_BREAK_TIMER.random",0));
        if(key==null)return original;
        boolean millis=id==33;
        return (int)number(key,millis?original/1000.0:original)*(millis?1000:1);
    }
    static double doubleField(double original, int id, String field) {
        return id==91 && field.equals("damageMultiplier") ? number("COUNTER_BARRIER_DMG",original) : original;
    }
    static int extraAcReduction(int ac) {
        double divisor = number("AC_REDUCTION_DIVISOR", 0);
        if (ac >= 0 || divisor == 0) return 0;
        double reduction = -(double) ac / divisor;
        return (int) Math.min(Integer.MAX_VALUE, reduction);
    }
    static int damageAfterExtraAc(int damage, int ac) {
        return Math.max(0, damage - extraAcReduction(ac));
    }
    static int protectedDamage(int damage) {
        if (damage < 0) throw new IllegalArgumentException("negative combat damage");
        return (int) Math.min(Integer.MAX_VALUE, damage / number("IMMUNE_TO_HARM", 2));
    }
    static int magicLevel(int type,int level) {
        if(level<0)throw new IllegalArgumentException("negative player level");
        // characters.Type is imported unchanged: knight=1, wizard=3.
        // Source: seven L1*ClassFeature.getMagicLevel implementations.
        return switch(type) {
            case 0 -> Math.min(2,level/10);
            case 1 -> level/50;
            case 2,6 -> Math.min(6,level>>3);
            case 3 -> Math.min(10,level>>2);
            case 4 -> Math.min(2,level/12);
            case 5 -> Math.min(4,level/9);
            default -> throw new IllegalArgumentException("unknown source character type="+type);
        };
    }

    static Snapshot snapshot(Properties properties, Properties properties2, Path path, Path path2) throws IOException {
        return NativeSourceSkillDefaults.snapshot(properties, properties2, path, path2);
    }
}
