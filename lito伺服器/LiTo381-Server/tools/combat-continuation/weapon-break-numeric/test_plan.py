"""Pure integer-boundary comparison; no sockets, fixture actors, SQL or deployment."""
from pathlib import Path
import json,hashlib,subprocess,os,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build/plan';D.mkdir(exist_ok=True);classes=D/'classes';classes.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
source=Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model/L1AttackNpc.java');text=source.read_text('utf-8');a=text.index('final int stacks = _npc.getWeaponBreakStack();');e=text.index('return dmg;',a)+len('return dmg;');block=text[a:e];assert block.count('final int stacks = _npc.getWeaponBreakStack();')==1
reference='package local.lito381.server; final class SourceWeaponBreakNumeric {static double damage(double dmg,int stacks){'+block.replace('final int stacks = _npc.getWeaponBreakStack();','')+'}}'
(D/'SourceWeaponBreakNumeric.java').write_text(reference,encoding='utf-8');shutil.copyfile(source,D/'L1AttackNpc.java.reference');shutil.copyfile(R/'WeaponBreakNumericProbe.java',D/'WeaponBreakNumericProbe.java');shutil.copyfile(__file__,D/'test_plan.py')
b=read(R/'build/build.json');J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx128m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8');v=dict(status='running',runtime=b['runtime'],source=pin(source),reference=pin(D/'SourceWeaponBreakNumeric.java'),formal_deployment_changed=False,full_goal_achieved=False)
try:
 with (D/'test.log').open('w',encoding='utf-8')as out:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=60)
  run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',b['runtime']['path'],'-d',classes,D/'SourceWeaponBreakNumeric.java',D/'WeaponBreakNumericProbe.java')
  for mode,ref in [('before',b['base']),('after',b['runtime'])]:
   jar=Path(ref['path']);assert sha(jar)==ref['sha256'];dest=D/(mode+'.json');run(J/'java.exe','-Xverify:all','-cp',str(classes)+';'+str(jar),'local.lito381.server.WeaponBreakNumericProbe',jar,dest,mode);v[mode]=read(dest);print(mode,v[mode],flush=True)
 v['status']='passed-pure-source-numeric-boundary-only'
except BaseException as error:v.update(status='failed',error=str(error));print((D/'test.log').read_text('utf-8',errors='replace')[-4000:]);raise
finally:(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
