"""Sequential exact-SHA retained tests. Only driver runtime/receipt selectors differ; Java cases stay unchanged."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,sys
R=Path(__file__).resolve().parent;D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/'source-kiringku-admission-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256']=='89e2b6877c6215c029d3b760ade4013292bb85f9944f07014424cd2414452b58'
groups=[('weapon_buff_targets',216),('weapon_buff_boundary',100),('weapon_buff_io',72),('lightning_area',128),('lightning_boundary',56),('lightning_io',48),('burning_weapon_retained',12)]
run=D/'source-kiringku-admission-retained'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(__file__,inputs/Path(__file__).name)
v=dict(status='running',runtime=build['runtime'],groups=[],native_cases=0,checks=0,production_class_overlays=0,full_goal_achieved=False,qualified_for_release=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 for kind,count in groups:
  source=R/('test_source_'+('lightning_stable_io'if kind=='lightning_io'else kind)+'.py');text=source.read_text('utf-8');shutil.copyfile(source,inputs/('original-'+source.name));start=text.index('build=read(');end=text.index(';jar=',start);text=text[:start]+"build=read(D/'source-kiringku-admission-build-current.json')"+text[end:];start=text.index(';name=');end=text.index(';run=',start);name='source-kiringku-admission-retained-'+kind;text=text[:start]+';name='+repr(name)+text[end:];driver=R/('test_source_admission_retained_'+kind+'.py');driver.write_text(text,'utf-8');shutil.copyfile(driver,inputs/driver.name)
  log=run/(kind+'.log')
  with log.open('w',encoding='utf-8')as out:subprocess.run([sys.executable,str(driver)],cwd=R,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=1200)
  index=read(D/(name+'-current.json'));assert index['runtime']==build['runtime'];m=read(Path(index['manifest']['path']));o=m['observations'];assert o['cases']==count and not o.get('observation_only',False);v['groups'].append(dict(name=kind,cases=count,checks=o['checks'],manifest=index['manifest'],log=pin(log)));v['native_cases']+=count;v['checks']+=o['checks'];write(run/'manifest.json',v);print(json.dumps(dict(completed=kind,cases=count,total=v['native_cases'])),flush=True)
 assert v['native_cases']==632;v['status']='passed632-retained-native-cases-on-Source-kiringku-candidate'
except BaseException as error:v.update(status='failed',error=str(error));raise
finally:v['inputs']=[pin(p)for p in inputs.iterdir()];write(run/'manifest.json',v);write(D/'source-kiringku-admission-retained-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],native_cases=v['native_cases'],checks=v['checks'])),flush=True)
