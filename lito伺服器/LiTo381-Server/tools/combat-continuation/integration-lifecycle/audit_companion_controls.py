"""Read the actual candidate's configured values; keep consumer qualification explicitly separate."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,re,subprocess,shutil,os
R=Path(__file__).resolve().parent;C=R.parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/'companion-earth-scene-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256']
run=D/'companion-control-inventory'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
matrix=S/'docs/maintenance/combat-coverage-current.json';source=Path('D:/lineage開發/1-伺服器/src/com/lineage/config');files=[Path(__file__),C/'ControlAudit.java',matrix,source/'ConfigSkill.java',source/'ConfigPcDamage.java',S/'config/server.properties']
entry=S/'config/combat.properties';files.append(entry)
for line in entry.read_text('utf-8').splitlines():
 if line.startswith('source.') and '.file=' in line:files.append((entry.parent/line.split('=',1)[1]).resolve())
for p in files:shutil.copyfile(p,inputs/p.name)
aliases=dict(re.findall(r'\b(\w+)\s*=\s*_getDouble\(p,\s*"([^"]+)"',(inputs/'ConfigPcDamage.java').read_text('utf-8')));assert len(aliases)==25
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=';'.join(map(str,[run/'classes',jar,S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'audit.log';v=dict(status='running',runtime=build['runtime'],inputs=[pin(p) for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8') as out:
  for args in [[J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',run/'classes',inputs/'ControlAudit.java'],[J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.ControlAudit',S/'config/server.properties',jar,run/'actual.properties']]:subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=90)
 loaded={line.split('=',1)[0] for line in (run/'actual.properties').read_text('utf-8').splitlines() if line and not line.startswith('#')};rows=[]
 for row in read(matrix)['external_controls']:
  if row['owner'] not in ['ConfigSkill','ConfigPcDamage']:continue
  key=aliases[row['key']] if row['owner']=='ConfigPcDamage' else 'Triple_Arrow_Dmg' if row['key']=='TRIPLE_ARROW_DMG' else row['key'];keys=[key+'.base',key+'.random'] if key.endswith('_TIMER') else [key];rows.append(dict(owner=row['owner'],source_key=row['key'],runtime_keys=keys,loaded=all(k in loaded for k in keys)))
 assert all(row['loaded'] for row in rows)
 for p in files:assert sha(p)==sha(inputs/p.name),'audit input changed during read'
 v.update(status='verified-readonly-current-configured-key-inventory-consumer-coverage-open',original_inputs=[pin(p) for p in files],historical_matrix_date=read(matrix)['date'],loaded_key_count=len(loaded),historical_Source_key_rows=len(rows),rows=rows,actual=pin(run/'actual.properties'),limits=['Only key loading for the dated ConfigSkill/ConfigPcDamage matrix and exact Source property aliases; no new live accounts, actors, Hub, listeners or SQL session.', 'Loading or unchanged archive entries do not qualify every current Source control, formula, selector, status consumer, UI display or the full 34 material skills.'])
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'companion-control-inventory-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],loaded=v['loaded_key_count'],Source_rows=v['historical_Source_key_rows'])))
