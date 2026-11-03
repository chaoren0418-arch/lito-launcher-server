from pathlib import Path
import json,hashlib,subprocess,os,zipfile,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/('build-repro' if '--reproduce' in sys.argv else 'build');D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
jar=R.parent/'armor-material/build/candidate-armor-material.jar';assert sha(jar)=='0d8296a15858f06287c088454f1988c89f43e402e85a30adf0d94c4a97cec30d'
inputs=D/'inputs';inputs.mkdir(exist_ok=True)
for p in [Path(__file__),R/'ArmorStateMethods.java',R/'NativeArmorState.java',B/'NativeSkillConsumptionMethods.java']:shutil.copyfile(p,inputs/p.name)
asm=[Path(p['path']) for p in read(R.parent/'build/integration-regression/preparation.json')['asm']];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8');v=dict(status='building',base=pin(jar),formal_deployment_changed=False,full_goal_achieved=False)
try:
 with (D/'build.log').open('w',encoding='utf-8') as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=90)
  tool=D/'tools';tool.mkdir(exist_ok=True);tcp=';'.join(map(str,[tool,*asm]));run(J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',tool,inputs/'ArmorStateMethods.java',inputs/'NativeSkillConsumptionMethods.java')
  classes=D/'classes';classes.mkdir(exist_ok=True);run(J/'java.exe','-Xverify:all','-cp',tcp,'ArmorStateMethods','splice',jar,classes)
  run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',';'.join(map(str,[classes,jar,S/'lib/*'])),'-d',classes,inputs/'NativeArmorState.java')
  with zipfile.ZipFile(jar) as z:entries={n:z.read(n) for n in z.namelist()}
  changed=[];fresh=[]
  for p in classes.rglob('*.class'):
   n=p.relative_to(classes).as_posix();data=p.read_bytes()
   if n not in entries:fresh.append(n)
   elif data!=entries[n]:changed.append(n)
   entries[n]=data
  assert changed==['local/lito381/server/ClientSession.class'];assert fresh==['local/lito381/server/NativeArmorState.class']
  candidate=D/'candidate-armor-state.jar'
  with zipfile.ZipFile(candidate,'w') as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  run(J/'java.exe','-Xverify:all','-cp',tcp,'ArmorStateMethods','verify',jar,candidate)
 v.update(status='built-unqualified',runtime=pin(candidate),changed=changed,added=fresh,inputs=[pin(p) for p in inputs.iterdir()])
except BaseException as e:v.update(status='failed',error=str(e));print((D/'build.log').read_text('utf-8',errors='replace')[-4500:]);raise
finally:(D/'build.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v['runtime']))
