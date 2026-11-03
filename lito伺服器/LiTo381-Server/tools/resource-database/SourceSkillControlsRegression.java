package local.lito381.server;
import java.nio.file.Path;
import java.util.*;

/** Independent expected-value vectors for source formula, not actor-path acceptance. */
public class SourceSkillControlsRegression {
    static int checks;
    static void check(boolean condition,String name) {checks++;if(!condition)throw new AssertionError(name);}
    static SourceSkillControls.Probability row(int id,int type,boolean level,boolean hit,double str,double dex,double intel,double mr,double con,double wis) {
        return new SourceSkillControls.Probability(id,70,50,30,str,dex,intel,type,level,hit,mr,con,wis);
    }
    public static void main(String[] args)throws Exception {
        ResourceDatabase.load(ServerConfig.load(Path.of(args[0]),"standard"));
        var sql=CombatDatabase.controls();
        check(sql.probabilities().containsKey(87)&&sql.probabilities().containsKey(208),"real probability table loaded");
        check(sql.bossModifiers().get(11)==0,"real boss immunity loaded");
        check(SourceSkillControls.ability(sql,26,"DEX")==5,"real buff table loaded");
        var caster=new SourceSkillControls.Caster(80,20,16,24,10,3,5);
        var target=new SourceSkillControls.Target(79,100,20,16,Map.of(5,8,1,88),false);
        var normal=row(87,5,true,true,.5,.25,.75,.2,2,4);
        check(SourceSkillControls.chance(normal,caster,target,-1)==78,"STR/DEX/INT + magic + MR/CON/WIS/resist + stun");
        check(SourceSkillControls.chance(normal,caster,new SourceSkillControls.Target(80,100,20,16,Map.of(5,8),false),-1)==58,"equal-level base");
        check(SourceSkillControls.chance(normal,caster,new SourceSkillControls.Target(81,100,20,16,Map.of(5,8),false),-1)==38,"lower-level base");
        var boss=new SourceSkillControls.Target(79,100,20,16,Map.of(5,8),true);
        check(SourceSkillControls.chance(normal,caster,boss,0)==0,"boss immune before chance floor");
        check(SourceSkillControls.chance(normal,caster,boss,10)==7,"boss division after cap and integer truncation");
        check(SourceSkillControls.chance(normal,caster,boss,-1)==78,"boss absent row has ordinary chance");
        check(SourceSkillControls.chance(normal,caster,boss,Integer.MAX_VALUE)==1,"boss division retains one-percent floor");
        check(SourceSkillControls.chance(row(87,1,true,true,.5,.25,.75,.2,2,4),caster,target,-1)==6,"changed resistance kind changes result");
        check(SourceSkillControls.chance(row(87,0,true,true,.5,.25,.75,.2,2,4),caster,target,-1)==86,"resistance disabled");
        check(SourceSkillControls.chance(row(87,5,false,false,.5,.25,.75,.2,2,4),caster,target,-1)==65,"magic flags disabled");
        check(SourceSkillControls.chance(row(87,5,true,true,0,.25,.75,.2,2,4),caster,target,-1)==68,"changed STR coefficient");
        check(SourceSkillControls.chance(row(87,5,true,true,.5,0,.75,.2,2,4),caster,target,-1)==74,"changed DEX coefficient");
        check(SourceSkillControls.chance(row(87,5,true,true,.5,.25,0,.2,2,4),caster,target,-1)==60,"changed INT coefficient");
        check(SourceSkillControls.chance(row(87,5,true,true,.5,.25,.75,0,2,4),caster,target,-1)==90,"changed MR coefficient and cap");
        check(SourceSkillControls.chance(row(87,5,true,true,.5,.25,.75,.2,0,0),caster,target,-1)==90,"CON/WIS disabled when divisors zero");
        check(SourceSkillControls.chance(row(87,5,true,true,.5,.25,.75,.2,-1,-1),caster,target,-1)==90,"source nonpositive CON/WIS divisors disabled");
        var noStun=new SourceSkillControls.Caster(80,20,16,24,10,3,-100);
        check(SourceSkillControls.chance(normal,noStun,target,-1)==1,"negative stun success floor");
        var overflow=new SourceSkillControls.Caster(80,20,16,24,10,3,Integer.MAX_VALUE);
        check(SourceSkillControls.chance(normal,overflow,target,-1)==90,"stun addition does not overflow");
        check(SourceSkillControls.chance(row(20,5,true,true,.5,.25,.75,.2,2,4),caster,target,-1)==73,"stun success applies only skill87");
        check(SourceSkillControls.chance(normal,new SourceSkillControls.Caster(80,21,16,24,10,3,5),target,-1)==78,"fractional source sum truncated before stun success");
        var changed=new HashMap<>(sql.probabilities());changed.put(87,normal);
        var changedBosses=new HashMap<>(sql.bossModifiers());changedBosses.put(87,10);
        var changedAbilities=new HashMap<>(sql.abilities());changedAbilities.put(26,Map.of("DEX",-3));
        var altered=new SourceSkillControls.Snapshot(changed,changedBosses,changedAbilities,sql.npcAttributes(),sql.skillCoexist(),sql.cancellationImmune(),sql.eyes(),sql.blessings());
        check(SourceSkillControls.chance(altered,87,caster,boss)==7,"modified actual probability/boss snapshots consumed");
        check(SourceSkillControls.ability(altered,26,"DEX")==-3,"modified actual buff snapshot consumed");
        check(SourceSkillControls.ability(altered,26,"STR")==0,"missing source buff ability contributes zero");
        boolean immutable=false;
        try{altered.abilities().get(26).put("DEX",1);}catch(UnsupportedOperationException expected){immutable=true;}
        check(immutable,"nested ability maps immutable");
        check(!sql.npcAttributes().isEmpty(),"NPC CON/WIS snapshot loaded");
        var npc=new SourceSkillControls.NpcAttributes(0,16,30,false);
        check(npc.constitution(0)==1&&npc.constitution(-1)==1,"source zero NPC CON is clamped by L1Character");
        check(npc.wisdom(-1)==15,"NPC panic modifier applies to source WIS");
        int[] sourceLevels={2,1,6,10,2,4,6};
        int[] sourceDice={1,1,5,11,1,3,5};
        for(int type=0;type<7;type++) {
            check(BattleTurnUndead.magicLevel(type,50)==sourceLevels[type],"source magic level class="+type);
            check(BattleTurnUndead.probability(type,50,11,0,0,0,12,-1,sides->1)==sourceDice[type],
                    "source fixed-roll legacy dice class="+type);
        }
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY}) {
            boolean rejected=false;
            try{row(87,5,true,true,invalid,.25,.75,.2,2,4);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"nonfinite coefficient rejected");
        }
        System.out.println("PASS source table and pure formula checks="+checks+"; complete configured STR/DEX/INT, magic flags, MR, CON/WIS, resistance selection, boss caps/floor and immutable buffs; actor-path integration still pending");
    }
}
