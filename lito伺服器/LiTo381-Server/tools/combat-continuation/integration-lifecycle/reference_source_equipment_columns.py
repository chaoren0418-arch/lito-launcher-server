from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
b=read(D/'source-equipment-columns-after-build-current.json');assert sha(Path(b['runtime']['path']))==b['runtime']['sha256'];run=D/'source-equipment-column-reference'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True)
for p in [Path(__file__),R/'SourceEquipmentColumnsProbe.java']:shutil.copyfile(p,run/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'verify.log';v=dict(status='running',runtime=b['runtime'],created_Hub=False,created_SQL_connection=False,initialized_game_classes=False,native_runtime_cases=0,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  import re
  build=read(Path(b['manifest']['path']));mapping_ref=next(x for x in build['inputs']if Path(x['path']).suffix=='.json');assert sha(Path(mapping_ref['path']))==mapping_ref['sha256'];mapping=read(Path(mapping_ref['path']));assert len(mapping)==21 and sum(map(len,mapping.values()))==899
  source_pairs=0
  for query,projection in build['canonical_queries'].items():
   table=re.fullmatch(r'SELECT \* FROM `([^`]+)`(?: ORDER BY .*)?',query)[1];pairs=re.findall(r'`([^`]+)` AS `([^`]+)`',projection);assert pairs==[(new,old)for old,new in mapping[table].items()];source_pairs+=len(pairs)
  cp=';'.join(map(str,[run/'classes',b['runtime']['path'],S/'lib/*']));invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',run/'classes',run/'SourceEquipmentColumnsProbe.java']);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.SourceEquipmentColumnsProbe',run/'observations.json']);observations=read(run/'observations.json');observations.update(Source_mapping_tables=21,Source_mapping_columns=899,independent_projected_column_pairs=source_pairs);v['observations']=observations
 v.update(status='passed-independent-Source899-column-mapping-and-existing-read-query-whitelist-reference',inputs=[pin(run/p)for p in ['SourceEquipmentColumnsProbe.java',Path(__file__).name]])
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-2000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'source-equipment-column-reference-current.json',dict(status=v['status'],runtime=b['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],verified_classes=v.get('verified_classes'))))
