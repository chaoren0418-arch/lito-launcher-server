package local.lito381.server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Source control tables and pure source arithmetic. Actor adapters are separate. */
final class SourceSkillControls {
    record Probability(int skillId,int high,int equal,int low,double strength,double dexterity,
                       double intelligence,int resistanceType,boolean magicLevel,boolean magicHit,
                       double mr,double constitutionDivisor,double wisdomDivisor) {
        Probability {
            if(skillId<=0 || resistanceType<0 || resistanceType>6)
                throw new IllegalArgumentException("invalid configured probability identity/resistance");
            for(double value:new double[]{strength,dexterity,intelligence,mr,constitutionDivisor,wisdomDivisor})
                if(!Double.isFinite(value)||Math.abs(value)>1000000)
                    throw new IllegalArgumentException("invalid configured probability coefficient");
        }
    }
    record NpcAttributes(int rawConstitution,int rawWisdom,int magicResistance,boolean boss) {
        NpcAttributes {
            if(rawConstitution<Byte.MIN_VALUE||rawConstitution>Byte.MAX_VALUE
                    ||rawWisdom<Byte.MIN_VALUE||rawWisdom>Byte.MAX_VALUE)
                throw new IllegalArgumentException("NPC source attributes exceed NpcTable.getByte contract");
        }
        int constitution(int modifier) {return (int)Math.max(1,Math.min(254,(long)rawConstitution+modifier));}
        int wisdom(int modifier) {return (int)Math.max(1,Math.min(254,(long)rawWisdom+modifier));}
    }
    record Snapshot(Map<Integer,Probability> probabilities,Map<Integer,Integer> bossModifiers,
                    Map<Integer,Map<String,Integer>> abilities,Map<Integer,NpcAttributes> npcAttributes, boolean skillCoexist,Set<Integer> cancellationImmune,Map<Long,SourceEyeControls.Config> eyes,SourceBlessingControls.Snapshot blessings) {
        Snapshot(Map<Integer,Probability> probabilities,Map<Integer,Integer> bossModifiers,Map<Integer,Map<String,Integer>> abilities,Map<Integer,NpcAttributes> npcAttributes,boolean skillCoexist,Set<Integer> cancellationImmune,Map<Long,SourceEyeControls.Config> eyes) {
            this(probabilities,bossModifiers,abilities,npcAttributes,skillCoexist,cancellationImmune,eyes,SourceBlessingControls.Snapshot.empty());
        }
        Snapshot(Map<Integer,Probability> probabilities,Map<Integer,Integer> bossModifiers,
                Map<Integer,Map<String,Integer>> abilities,Map<Integer,NpcAttributes> npcAttributes,boolean skillCoexist,Set<Integer> cancellationImmune) {
            this(probabilities,bossModifiers,abilities,npcAttributes,skillCoexist,cancellationImmune,Map.of());
        }
        Snapshot(Map<Integer,Probability> probabilities,Map<Integer,Integer> bossModifiers,
                Map<Integer,Map<String,Integer>> abilities,Map<Integer,NpcAttributes> npcAttributes,boolean skillCoexist) {
            this(probabilities,bossModifiers,abilities,npcAttributes,skillCoexist,Set.of(),Map.of());
        }
        Snapshot(Map<Integer,Probability> probabilities,Map<Integer,Integer> bossModifiers,
                Map<Integer,Map<String,Integer>> abilities,Map<Integer,NpcAttributes> npcAttributes) {
            this(probabilities,bossModifiers,abilities,npcAttributes,false,Set.of(),Map.of());
        }
        Snapshot {
            probabilities=Map.copyOf(probabilities);bossModifiers=Map.copyOf(bossModifiers);npcAttributes=Map.copyOf(npcAttributes);cancellationImmune=Set.copyOf(cancellationImmune);eyes=Map.copyOf(eyes);
            var frozen=new HashMap<Integer,Map<String,Integer>>();
            abilities.forEach((id,values)->frozen.put(id,Map.copyOf(values)));abilities=Map.copyOf(frozen);
        }
    }
    record Caster(int level,int strength,int dexterity,int intelligence,int magicLevel,int magicHit,int stunSuccess) {
        Caster { if(level<=0 || strength<0 || dexterity<0 || intelligence<0 || magicLevel<0)
            throw new IllegalArgumentException("invalid source caster attributes"); }
    }
    record Target(int level,int mr,int constitution,int wisdom,Map<Integer,Integer> resistances,boolean boss) {
        Target {
            if(level<=0 || constitution<0 || wisdom<0)throw new IllegalArgumentException("invalid source target attributes");
            resistances=Map.copyOf(resistances);
            for(int type:resistances.keySet())if(type<1 || type>6)throw new IllegalArgumentException("unknown source resistance type="+type);
        }
    }
    private static final Set<String> ABILITIES=Set.of("AC","BOW_DMG","BOW_HIT","CHA","CON","DEX",
            "DMG","HIT","HP","HP_REGEN","INT","MP","MP_REGEN","STR","WIS","SP","MR","MAGIC_HIT","DMG_REDUX","ER",
            "REGIST_STONE","REGIST_SLEEP","REGIST_FREEZE","POISON_RESIST","DEF_FIRE","DEF_WATER","DEF_WIND","DEF_EARTH");
    static boolean supportedAbility(String type){return ABILITIES.contains(type);}

    static Snapshot read(Connection connection) throws SQLException {
        var probabilities=new HashMap<Integer,Probability>();var bosses=new HashMap<Integer,Integer>();
        var abilities=new HashMap<Integer,Map<String,Integer>>();
        var npcAttributes=new HashMap<Integer,NpcAttributes>();
        boolean coexist=false;
        var cancellationImmune=new java.util.HashSet<Integer>();
        try(var st=connection.createStatement()) {
            try(var r=st.executeQuery("SELECT eventstart FROM server_event WHERE eventclass='SkillCoexistSet' LIMIT 1")) {
                if(r.next())coexist=r.getInt(1)==1;
            }
            try(var r=st.executeQuery("SELECT c.skill_id FROM `skills_相消免疫技能` c INNER JOIN skills s ON s.skill_id=c.skill_id")) {
                while(r.next())cancellationImmune.add(integer(r,"skill_id",1,Integer.MAX_VALUE));
            }
            try(var r=st.executeQuery("SELECT skill_id,prob_high,prob_equal,prob_low,bonus_str,bonus_dex,bonus_int,check_resist_type,check_magic_level,check_magic_hit,mr_multiplier,con_multiplier,wis_multiplier FROM `skills_負面技能控制`")) {
                while(r.next()) {
                    int id=integer(r,"skill_id",1,Integer.MAX_VALUE);
                    var row=new Probability(id,integer(r,"prob_high",-1000000,1000000),
                            integer(r,"prob_equal",-1000000,1000000),integer(r,"prob_low",-1000000,1000000),
                            decimal(r,"bonus_str"),decimal(r,"bonus_dex"),decimal(r,"bonus_int"),
                            integer(r,"check_resist_type",0,6),integer(r,"check_magic_level",0,1)==1,
                            integer(r,"check_magic_hit",0,1)==1,decimal(r,"mr_multiplier"),
                            decimal(r,"con_multiplier"),decimal(r,"wis_multiplier"));
                    if(probabilities.putIfAbsent(id,row)!=null)throw bad(r,"skill_id","duplicate probability row");
                }
            }
            try(var r=st.executeQuery("SELECT skill_id,modifier FROM `skills_負面boss免疫`")) {
                while(r.next()) {
                    int id=integer(r,"skill_id",1,Integer.MAX_VALUE);
                    if(bosses.putIfAbsent(id,integer(r,"modifier",0,Integer.MAX_VALUE))!=null)
                        throw bad(r,"skill_id","duplicate boss row");
                }
            }
            try(var r=st.executeQuery("SELECT skill_id,ability_type,ability_value FROM skills_buff_ability")) {
                while(r.next()) {
                    int id=integer(r,"skill_id",1,Integer.MAX_VALUE);
                    String raw=r.getString("ability_type");
                    String type=raw==null?"":raw.trim().toUpperCase(Locale.ROOT);
                    if(!ABILITIES.contains(type))throw bad(r,"ability_type","unimplemented source ability "+raw);
                    var row=abilities.computeIfAbsent(id,key->new HashMap<>());
                    if(row.putIfAbsent(type,integer(r,"ability_value",Integer.MIN_VALUE,Integer.MAX_VALUE))!=null)
                        throw bad(r,"ability_type","duplicate ability "+type);
                }
            }
            try(var r=st.executeQuery("SELECT npcid,`con`,`wis`,mr,boss FROM npc")) {
                while(r.next()) {
                    String id=r.getString("npcid");
                    try {
                        int npc=Integer.parseInt(id);
                        if(npc<=0)throw new IllegalArgumentException("nonpositive NPC identity");
                        var value=new NpcAttributes(Integer.parseInt(r.getString("con")),Integer.parseInt(r.getString("wis")),
                                Integer.parseInt(r.getString("mr")),r.getBoolean("boss"));
                        if(npcAttributes.putIfAbsent(npc,value)!=null)throw new IllegalArgumentException("duplicate NPC identity");
                    } catch(IllegalArgumentException e) {throw new SQLException("npc npcid="+id+": "+e.getMessage(),e);}
                }
            }
        }
        var eyes=SourceEyeControls.read(connection);
        return new Snapshot(probabilities,bosses,SourceEyeControls.merge(abilities,eyes.abilities()),npcAttributes,coexist,cancellationImmune,eyes.configs(),SourceBlessingControls.read(connection));
    }
    private static int integer(ResultSet r,String field,int min,int max)throws SQLException {
        String text=r.getString(field);
        try {
            int n=Integer.parseInt(text);if(n<min || n>max)throw new NumberFormatException();return n;
        } catch(RuntimeException e) {throw bad(r,field,"invalid integer "+text);}
    }
    private static double decimal(ResultSet r,String field)throws SQLException {
        String text=r.getString(field);
        try {
            double n=Double.parseDouble(text);
            if(!Double.isFinite(n)||Math.abs(n)>1000000)throw new NumberFormatException();return n;
        } catch(RuntimeException e) {throw bad(r,field,"invalid coefficient "+text);}
    }
    private static SQLException bad(ResultSet r,String field,String detail)throws SQLException {
        return new SQLException("source skill control skill_id="+r.getString("skill_id")+" field="+field+": "+detail);
    }

    /** Mirrors L1MagicPc.calcProbabilityWithConfig then calcProbabilityMagic caps. */
    static int chance(Probability row,Caster caster,Target target,int bossModifier) {
        if(target.boss()&&bossModifier==0)return 0;
        double chance=caster.level()>target.level()?row.high():caster.level()==target.level()?row.equal():row.low();
        chance+=caster.strength()*row.strength()+caster.dexterity()*row.dexterity()+caster.intelligence()*row.intelligence();
        if(row.magicHit())chance+=caster.magicHit();
        if(row.magicLevel())chance+=caster.magicLevel();
        chance-=target.mr()*row.mr();
        if(row.constitutionDivisor()>0)chance-=target.constitution()/row.constitutionDivisor();
        if(row.wisdomDivisor()>0)chance-=target.wisdom()/row.wisdomDivisor();
        if(row.resistanceType()!=0)chance-=target.resistances().getOrDefault(row.resistanceType(),0);
        int configured=(int)Math.max(1,Math.min(100,chance));
        if(row.skillId()==87)configured=(int)Math.max(1,Math.min(100,(long)configured+caster.stunSuccess()));
        int capped=Math.min(90,configured);
        if(target.boss()&&bossModifier>0)capped/=bossModifier;
        return Math.max(1,capped);
    }
    static int chance(Snapshot snapshot,int skill,Caster caster,Target target) {
        Probability row=snapshot.probabilities().get(skill);
        if(row==null)throw new IllegalArgumentException("skill has no configured probability: "+skill);
        return chance(row,caster,target,snapshot.bossModifiers().getOrDefault(skill,-1));
    }
    static int ability(Snapshot snapshot,int skill,String type) {
        if(!ABILITIES.contains(type))throw new IllegalArgumentException("unknown source ability="+type);
        return snapshot.abilities().getOrDefault(skill,Map.of()).getOrDefault(type,0);
    }
}
