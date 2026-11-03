from pathlib import Path
import json,hashlib,subprocess,os
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build/tests';D.mkdir(exist_ok=True);C=D/'classes';C.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));b=json.loads((R/'build/build.json').read_text('utf8'));jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256'];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=';'.join(map(str,[C,jar,S/'lib/*']));v=dict(status='running',runtime=b['runtime'],inputs=[pin(R/'CombatConfigOriginProbe.java'),pin(Path(__file__))],formal_deployment_changed=False)
try:
 with (D/'test.log').open('w',encoding='utf8') as log:
  for cmd in [[J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',C,R/'CombatConfigOriginProbe.java'],[J/'java.exe','-Xverify:all','-Dfile.encoding=UTF-8','-cp',cp,'local.lito381.server.CombatConfigOriginProbe',S/'config/server.properties',D/'fixture',jar]]:subprocess.run(list(map(str,cmd)),stdout=log,stderr=subprocess.STDOUT,check=True,timeout=60)
 v.update(status='passed',observations=json.loads((D/'fixture/observations.json').read_text('utf8')))
except BaseException as e:v.update(status='failed',error=str(e));print((D/'test.log').read_text('utf8')[-5000:]);raise
finally:(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf8')
print(json.dumps(v['observations']))
