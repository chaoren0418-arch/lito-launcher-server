"""Run owned native fixtures sequentially and freeze each exact-SHA receipt."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,sys,shutil
R=Path(__file__).resolve().parent;D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'))
pin=lambda p:dict(path=str(p.resolve()),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/'companion-forest-area-build-current.json');runtime=build['runtime']
run=D/'companion-forest-suite'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True)
shutil.copyfile(__file__,run/Path(__file__).name)
specs=[('test_selector_only_ctrl.py','selector-only-ctrl'),('test_companion_forest_battle.py','companion-forest-battle-after'),('test_companion_forest_scene.py','companion-forest-scene-after'),('test_companion_forest_lifecycle.py','companion-forest-lifecycle-after'),('test_companion_forest_publication.py','companion-forest-publication-after'),('test_companion_forest_area_regressions.py','companion-forest-area-regressions'),('test_companion_forest_gates.py','companion-forest-gates')]
v=dict(status='running',runtime=runtime,results=[],full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 for script,name in specs:
  assert read(D/'companion-forest-area-build-current.json')['runtime']==runtime
  print('RUN '+script,flush=True);log=run/(script+'.log')
  with log.open('w',encoding='utf-8') as out:subprocess.run([sys.executable,'-B','-X','utf8',str(R/script)],cwd=R,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=2400)
  index=D/(name+'-current.json');i=read(index);assert i['runtime']==runtime and i['status'].startswith('passed')
  frozen=run/index.name;shutil.copyfile(index,frozen);v['results'].append(dict(driver=pin(R/script),receipt=pin(frozen),manifest=i['manifest'],log=pin(log)));write(run/'manifest.json',v)
  print('PASS '+script,flush=True)
 v['status']='passed-sequential-exact-SHA-area161-scoped-suite'
except BaseException as e:v.update(status='failed',error=str(e));print(log.read_text('utf-8',errors='replace')[-2500:]);raise
finally:write(run/'manifest.json',v);write(D/'companion-forest-suite-current.json',dict(status=v['status'],runtime=runtime,manifest=pin(run/'manifest.json')))
print(v['status'])
