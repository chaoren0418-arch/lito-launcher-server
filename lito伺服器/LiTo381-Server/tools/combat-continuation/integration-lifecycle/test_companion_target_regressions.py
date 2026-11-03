"""Reuse frozen regression bodies with only candidate/index and receipt-directory selection changed."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/'companion-target-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256'];run=D/'companion-target-regressions'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(__file__,inputs/Path(__file__).name)
specs=[('review_source.py','on-companion-targets-source-drift-review',0),('test_skill73_main.py','on-companion-targets-skill73-Main',8),('test_pair_main.py','on-companion-targets-pair-Main',10),('test_battle_pet_lifecycle.py','on-companion-targets-battle-pet-lifecycle',88),('test_npc_physical_area_lifecycle.py','on-companion-targets-npc-physical-area-lifecycle',26)]
v=dict(status='running',runtime=build['runtime'],basis=build['manifest'],results=[],driver_adaptation='Only candidate/index selection, receipt directory names and original driver path resolution; all fixture bodies and assertions retained.',full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 for name,receipt,cases in specs:
  source=R/name;original=source.read_text('utf-8');text=original.replace('Path(__file__).resolve()',f'Path({str(source)!r}).resolve()').replace('summon-retirement-build-current.json','companion-target-build-current.json').replace('on-summon-retirement-','on-companion-targets-');assert text!=original and 'companion-target-build-current.json' in text;driver=inputs/name;driver.write_text(text,'utf-8');log=run/(name+'.log');print('RUN '+name,flush=True)
  with log.open('w',encoding='utf-8') as out:subprocess.run([sys.executable,'-B','-X','utf8',str(driver),'--summon-retirement'],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=600)
  index=D/(receipt+'-current.json');current=read(index);assert current['status'].startswith('passed') and current['runtime']==build['runtime'];assert sha(Path(current['manifest']['path']))==current['manifest']['sha256'];m=read(Path(current['manifest']['path']));observations=dict(cases=len(m['results']),checks=None) if name.endswith('_main.py') else dict(cases=0,checks=0) if name=='review_source.py' else m['observations'];assert observations['cases']==cases;frozen=run/index.name;shutil.copyfile(index,frozen);v['results'].append(dict(original=pin(source),adapted=pin(driver),receipt=pin(frozen),manifest=current['manifest'],observations=observations,log=pin(log)));write(run/'manifest.json',v);print('PASS '+name+' cases='+str(cases),flush=True)
 v.update(status='passed-same-SHA-skill73-Main-pair-Main-BATTLE-pet-and-NPC-area-lifecycle',cases=sum(row['observations']['cases'] for row in v['results']),known_checks=sum(row['observations']['checks'] or 0 for row in v['results']),check_count_complete=False)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4500:]);raise
finally:write(run/'manifest.json',v);write(D/'companion-target-regressions-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'])))
