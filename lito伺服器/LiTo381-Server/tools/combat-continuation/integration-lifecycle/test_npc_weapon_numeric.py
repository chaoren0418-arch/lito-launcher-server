from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,re
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
b=read(D/'npc-weapon-build-current.json');manifest=read(Path(b['manifest']['path']));source=Path(manifest['Source_L1AttackNpc_readonly']['path']);assert sha(source)==manifest['Source_L1AttackNpc_readonly']['sha256'];text=source.read_text('utf-8');signature='private double npcDmgMode(double dmg)';assert text.count(signature)==1;start=text.index(signature);brace=text.index('{',start);depth=1;end=brace+1
while depth:
 if text[end]=='{':depth+=1
 elif text[end]=='}':depth-=1
 end+=1
method=text[start:end];assert method.count('ThreadLocalRandom.current().nextInt(100)')==1;method=method.replace('ThreadLocalRandom.current().nextInt(100)','criticalRoll')
pc=text[text.index('private int calcPcDamage()'):text.index('private int calcNpcDamage()')];prefix=re.findall(r'^\s*dmg = ThreadLocalRandom\.current\(\)\.nextInt\(lvl\) \+ \(_npc\.getStr\(\) \* 0\.8\) \+ dmgStr;',pc,re.M);assert len(prefix)==1;prefix=prefix[0].strip().replace('ThreadLocalRandom.current().nextInt(lvl)','levelRoll').replace('_npc.getStr()','strength').replace('dmgStr','strengthDamage')
oracle='''package local.lito381.server;
final class SourceNpcDmgModeOracle {
 int criticalRoll,leverage;boolean night;Npc _npc;
 record Template(boolean isBoss){}
 record Npc(int stacks,boolean boss){int getDmgup(){return 0;}int getWeaponBreakStack(){return stacks;}Template getNpcTemplate(){return new Template(boss);}}
 boolean isUndeadDamage(){return night;}int getLeverage(){return leverage;}
 double calculate(int levelRoll,int strength,int strengthDamage,int critical,boolean undeadNight,boolean boss,int leverage,int stacks){this.criticalRoll=critical;this.leverage=leverage;night=undeadNight;_npc=new Npc(stacks,boss);double dmg;
'''+prefix+'\nreturn npcDmgMode(dmg);}\n'+method+'\n}\n'
run=D/'npc-weapon-numeric'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);classes=run/'classes';classes.mkdir();shutil.copyfile(Path(__file__),inputs/Path(__file__).name);shutil.copyfile(R/'NpcWeaponBreakNumericProbe.java',inputs/'NpcWeaponBreakNumericProbe.java');(inputs/'SourceNpcDmgModeOracle.java').write_text(oracle,encoding='utf-8');(inputs/'source-npcDmgMode.txt').write_text(text[start:end],encoding='utf-8');shutil.copyfile(source,inputs/'readonly-L1AttackNpc.java')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'test.log';v=dict(status='running',runtime=b['runtime'],base=manifest['base'],Source_readonly=pin(source),inputs=[pin(p) for p in inputs.iterdir()],Source_or_player_SQL_or_WAL_written=False,formal_deployment_changed=False,full_goal_achieved=False)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,check=True,stdout=out,stderr=subprocess.STDOUT,timeout=180)
  jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256'];cp=';'.join(map(str,[classes,jar,S/'lib/*']));invoke([J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',classes,inputs/'NpcWeaponBreakNumericProbe.java',inputs/'SourceNpcDmgModeOracle.java'])
  for name,ref in [('before',manifest['base']),('after',b['runtime'])]:
   jar=Path(ref['path']);assert sha(jar)==ref['sha256'];dest=run/name;dest.mkdir();invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[classes,jar,S/'lib/*'])),'local.lito381.server.NpcWeaponBreakNumericProbe',jar,dest/'observations.json',name]);v[name]=read(dest/'observations.json')
 v['status']='passed-independent-Source-prefix-and-actual-Tournament-consumer'
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-6500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'npc-weapon-numeric-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),runtime=b['runtime']))
print(json.dumps({k:v[k] for k in ['status','before','after']}))
