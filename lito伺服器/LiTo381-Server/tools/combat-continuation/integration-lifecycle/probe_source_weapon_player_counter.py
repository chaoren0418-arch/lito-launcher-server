"""Fast isolated native weapon probe before the cumulative inherited regression build."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,os,shutil,subprocess,zipfile

R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2]
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
index=read(D/'source-weapon-counter-magic-build-current.json');assert pin(index['runtime']['path'])==index['runtime'];base=Path(index['runtime']['path']);build=read(index['manifest']['path'])
run=D/'weapon-player-counter-dev'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
names=['NativeSourceWeaponPlayerCounter.java','SourceWeaponPlayerCounterMethods.java','SourceWeaponPlayerCounterDelta.java','SourceWeaponPlayerFullPathPlan.java','SourceWeaponFullPathPlan.java']
for name in names:shutil.copyfile(R/name,inputs/name)
oracle=D/'source-weapon-player-counter-builds/20261026T231712630200Z/inputs/SourceWeaponDurabilityOracle.java'
shutil.copyfile(oracle,inputs/oracle.name)
cp=';'.join(map(str,[run/'tools',base.parent/'tools',*[x['path']for x in build['asm']]]))
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8');argfiles=[]
v=dict(status='running',base=index['runtime'],basis=index['manifest'],inputs=[pin(p)for p in inputs.iterdir()],new_SQL_connections=0,real_server_started=False,full_goal_achieved=False,formal_deployment_changed=False)
try:
 with (run/'probe.log').open('w',encoding='utf-8')as log:
  def invoke(args):
   args=list(map(str,args))
   if Path(args[0]).name=='javac.exe':
    p=run/('javac-'+str(len(argfiles)+1)+'.args');p.write_text('\n'.join(json.dumps(x,ensure_ascii=False)for x in args[1:]),'utf-8');argfiles.append(p);args=[args[0],'@'+str(p)]
   subprocess.run(args,cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=120)
  invoke([J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',run/'tools',inputs/'SourceWeaponPlayerCounterMethods.java',inputs/'SourceWeaponPlayerCounterDelta.java'])
  invoke([J/'java.exe','-Xverify:all','-cp',cp,'SourceWeaponPlayerCounterMethods','splice',base,run/'classes',S/'lib'])
  invoke([J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',';'.join(map(str,[run/'classes',base,S/'lib/*'])),'-d',run/'classes',inputs/'NativeSourceWeaponPlayerCounter.java'])
  with zipfile.ZipFile(base)as z:entries={n:z.read(n)for n in z.namelist()}
  for p in (run/'classes').rglob('*.class'):entries[p.relative_to(run/'classes').as_posix()]=p.read_bytes()
  jar=run/'candidate-dev-unqualified.jar'
  with zipfile.ZipFile(jar,'w')as z:
   for name,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(name,(1980,1,1,0,0,0)),data)
  v['runtime']=pin(jar)
  invoke([J/'java.exe','-Xverify:all','-cp',cp,'SourceWeaponPlayerCounterDelta',base,jar])
  invoke([J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',';'.join(map(str,[jar,base.parent/'plan',S/'lib/*'])),'-d',run/'plan',inputs/'SourceWeaponPlayerFullPathPlan.java',inputs/'SourceWeaponFullPathPlan.java',inputs/'SourceWeaponDurabilityOracle.java'])
  for plan in ['SourceWeaponPlayerFullPathPlan','SourceWeaponFullPathPlan']:
   invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,base.parent/'plan',S/'lib/*'])),'local.lito381.server.'+plan])
 v['status']='passed-isolated-native-PC-counter-durability-pair-and-controlled-Earth-probe-unqualified'
except BaseException as error:
 v.update(status='failed',error=str(error));print((run/'probe.log').read_text('utf-8',errors='replace')[-3600:]);raise
finally:
 v['log']=pin(run/'probe.log');write(run/'manifest.json',v);write(D/'weapon-player-counter-dev-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']}if'runtime'in v else{})))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'))))
