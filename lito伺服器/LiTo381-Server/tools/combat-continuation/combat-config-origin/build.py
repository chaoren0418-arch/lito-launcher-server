from pathlib import Path
import json,hashlib,subprocess,os,zipfile,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/('build-repro'if '--reproduce'in sys.argv else'build');D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
jar=R.parent/'area-sleep-slow/build/candidate-area-sleep-slow.jar';assert sha(jar)=='8d44fac4fb8e02cf9d7bee84a770a4b01a31a043620b796529a9cded47ed445f'
inputs=D/'inputs';inputs.mkdir(exist_ok=True)
for p in [Path(__file__),R/'CombatConfigOriginMethods.java',R/'NativeCombatConfigOrigin.java',B/'NativeSkillConsumptionMethods.java']:shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8');asm=[Path(p['path'])for p in read(R.parent/'build/integration-regression/preparation.json')['asm']]
v=dict(status='building',base=pin(jar),formal_deployment_changed=False,full_goal_achieved=False,scope='Resolve combat controls from existing configuration.base.file; targeted qualification pending')
try:
 with (D/'build.log').open('w',encoding='utf-8')as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=90)
  tools=D/'tools';classes=D/'classes'
  for p in [tools,classes]:p.mkdir(exist_ok=True)
  cp=';'.join(map(str,[tools,*asm]));run(J/'javac.exe','-g:none','-encoding','UTF-8','-cp',cp,'-d',tools,inputs/'NativeSkillConsumptionMethods.java',inputs/'CombatConfigOriginMethods.java')
  run(J/'java.exe','-Xverify:all','-cp',cp,'CombatConfigOriginMethods','splice',jar,classes)
  run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',';'.join(map(str,[classes,jar,S/'lib/*'])),'-d',classes,inputs/'NativeCombatConfigOrigin.java')
  with zipfile.ZipFile(jar)as z:entries={n:z.read(n)for n in z.namelist()}
  changed=[];added=[]
  for p in classes.rglob('*.class'):
   name=p.relative_to(classes).as_posix();data=p.read_bytes()
   if name not in entries:added.append(name)
   elif entries[name]!=data:changed.append(name)
   entries[name]=data
  prefix='local/lito381/server/'
  assert set(changed)=={prefix+n+'.class' for n in ['CombatRules']},changed
  assert set(added)=={prefix+n+'.class' for n in ['NativeCombatConfigOrigin']},added
  candidate=D/'candidate-combat-config-origin.jar'
  with zipfile.ZipFile(candidate,'w')as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  run(J/'java.exe','-Xverify:all','-cp',cp,'CombatConfigOriginMethods','verify',jar,candidate)
 v.update(status='built-unqualified',runtime=pin(candidate),exact_combat_config_path_inverse=True,changed=changed,added=added,inputs=[pin(p)for p in inputs.iterdir()if p.is_file()])
except BaseException as error:v.update(status='failed',error=str(error));print((D/'build.log').read_text('utf-8',errors='replace')[-5000:]);raise
finally:(D/'build.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v['runtime']))
