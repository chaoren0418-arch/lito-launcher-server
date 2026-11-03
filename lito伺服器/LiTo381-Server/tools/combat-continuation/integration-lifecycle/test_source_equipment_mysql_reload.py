"""Invoke existing native MySQL readers, with fixture-only non-write/resource-close observation."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
origin='--readonly-origin'in sys.argv;build=read(D/('source-readonly-inventory-origin-build-current.json'if origin else'source-legacy-equipment-sql-build-current.json'));jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];run=D/('source-equipment-mysql-reload-readonly-origin'if origin else'source-equipment-mysql-reload')/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for p in [Path(__file__),R/'SourceEquipmentMysqlReloadProbe.java']:shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';cp=';'.join(map(str,[classes,jar,S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],inputs=[pin(p)for p in inputs.iterdir()],production_class_overlays=0,fixture_authors_SQL=False,real_player_row_mapping_qualified=False,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,inputs/'SourceEquipmentMysqlReloadProbe.java']);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.SourceEquipmentMysqlReloadProbe',S/'config/server.properties',run/'observations.json',jar])
 v.update(status='passed-real-MySQL-native-readonly-reload-scoped',observations=read(run/'observations.json'))
except BaseException as error:
 v.update(status='failed',error=type(error).__name__)
 if(run/'observations.json').exists():v['observations']=read(run/'observations.json');print(json.dumps(v['observations'],ensure_ascii=True))
 else:print(log.read_text('utf-8',errors='replace')[-1800:])
 raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/('source-equipment-mysql-reload-readonly-origin-current.json'if origin else'source-equipment-mysql-reload-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
