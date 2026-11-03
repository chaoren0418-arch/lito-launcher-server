from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os
R=Path(__file__).resolve().parent;C=R.parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
b=read(D/'companion-combat-scene-build-current.json');assert sha(Path(b['runtime']['path']))==b['runtime']['sha256'];run=D/'companion-combat-origin'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for p in [Path(__file__),R/'CompanionCombatOriginProbe.java']:shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');tools=run/'tools';log=run/'verify.log';env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');results=[]
with log.open('w',encoding='utf-8') as out:
 subprocess.run([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-d',tools,inputs/'CompanionCombatOriginProbe.java'],stdout=out,stderr=subprocess.STDOUT,env=env,check=True,timeout=60)
 for name in ['companion-debit-boundary-after','companion-debit-scene-after','companion-debit-frame-after']:
  i=read(D/(name+'-current.json'));assert i['runtime']==b['runtime'] and i['status'].startswith('passed');assert sha(Path(i['manifest']['path']))==i['manifest']['sha256'];m=read(Path(i['manifest']['path']));classes=Path(i['manifest']['path']).parent/'classes';F=B/'baseline-PC-ingress-efd4-v2';cp=[tools,classes,C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',Path(b['runtime']['path']),S/'lib/*']
  subprocess.run([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,cp)),'CompanionCombatOriginProbe',b['runtime']['path']],stdout=out,stderr=subprocess.STDOUT,env=env,check=True,timeout=60);results.append(dict(receipt=pin(D/(name+'-current.json')),manifest=i['manifest'],classpath=list(map(str,cp)),gameplay_classes=31))
v=dict(status='passed-readonly-exact-native-fixture-classpath-origins-without-initialization',runtime=b['runtime'],inputs=[pin(p) for p in inputs.iterdir()],results=results,log=pin(log),behavior_cases_added=0,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);write(run/'manifest.json',v);write(D/'companion-combat-origin-current.json',dict(status=v['status'],runtime=b['runtime'],manifest=pin(run/'manifest.json')));print(json.dumps(dict(status=v['status'],classpaths=3,gameplay_classes_per_classpath=31,behavior_cases_added=0)))
