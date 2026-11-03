from pathlib import Path
import hashlib,json,subprocess,os,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build/load';D.mkdir(parents=True,exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p),sha256=sha(p));read=lambda p:json.loads(p.read_text('utf-8'))
runtime=read(R/'build/build.json')['runtime'];jar=Path(runtime['path']);assert sha(jar)==runtime['sha256'];classes=D/'classes';classes.mkdir(exist_ok=True);frozen=D/'frozen';frozen.mkdir(exist_ok=True)
for p in [Path(__file__),R/'PetControlLoadProbe.java']:shutil.copyfile(p,frozen/(sha(p)+'-'+p.name))
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx128m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8');v=dict(status='running',runtime=runtime,inputs=[pin(p)for p in frozen.iterdir()],formal_deployment_changed=False,source_database_mutated=False)
try:
 with (D/'test.log').open('w',encoding='utf-8')as log:
  for args in [[J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',jar,'-d',classes,R/'PetControlLoadProbe.java'],[J/'java.exe','-Xverify:all','-cp',str(classes)+';'+str(jar),'local.lito381.server.PetControlLoadProbe',S/'config/server.properties',D/'fixture',jar]]:subprocess.run(list(map(str,args)),env=env,check=True,stdout=log,stderr=subprocess.STDOUT,timeout=60)
 v.update(status='passed-configuration-only',results=read(D/'fixture/observations.json'),observations=pin(D/'fixture/observations.json'))
except BaseException as error:v.update(status='failed',error=str(error));print((D/'test.log').read_text('utf-8',errors='replace')[-5000:]);raise
finally:(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v.get('results',{})))
