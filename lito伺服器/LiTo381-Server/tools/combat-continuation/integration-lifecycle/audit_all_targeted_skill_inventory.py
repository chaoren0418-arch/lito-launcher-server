"""Inventory every shared skill row through the existing readonly startup; no Main, Hub, cursor or effect claims."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';T=Path('D:/lineage開發/1-伺服器/src/com/lineage/server')
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/'source-mass-slow-pipeline-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];bm=read(Path(build['manifest']['path']))
run=D/'all-targeted-skill-inventory'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in ['ObserveTargetIntentInventory.java','TargetIntentInventoryMetadata.java','TargetIntentInventoryProbe.java','FreezeAcceptedMethods.java','audit_all_targeted_skill_inventory.py']:shutil.copyfile(R/name,inputs/name)
sources=[]
for p in [T/'templates/L1Skills.java',T/'model/skill/L1SkillUse.java',T/'model/skill/L1SkillMode.java',T/'datatables/SkillsTable.java']:
 if p.exists():shutil.copyfile(p,run/p.name);sources.append(dict(original=pin(p),frozen=pin(run/p.name)))
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';overlay=run/'readonly-startup-observer';log=run/'audit.log';env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8')
cp=';'.join(map(str,[classes,overlay,jar,S/'lib/*']));tcp=';'.join(map(str,[run/'tools',*[x['path']for x in bm['asm']]]))
v=dict(status='running',runtime=build['runtime'],Source=sources,user_report=read(D/'all-targeted-skill-intent-scope-current.json')['manifest'],inputs=[pin(p)for p in inputs.iterdir()],created_Main=False,created_Hub=False,created_SQL_session_beyond_existing_readonly_startup_transaction=False,production_PC_or_NPC_overlays=0,audit_only_CombatDatabase_metadata_callback_overlay=True,native_runtime_cases=0,qualified_for_new_target_intent_policy=False,all_skill_target_coverage_complete=False,full_goal_achieved=False,formal_deployment_changed=False,new_baseline_created=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',tcp,'-d',run/'tools',inputs/'ObserveTargetIntentInventory.java',inputs/'FreezeAcceptedMethods.java']);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'ObserveTargetIntentInventory',jar,overlay]);assert [p.name for p in overlay.rglob('*.class')]==['CombatDatabase.class']
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,inputs/'TargetIntentInventoryMetadata.java',inputs/'TargetIntentInventoryProbe.java']);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.TargetIntentInventoryProbe',S/'config/server.properties',run/'target-matrix.tsv',run/'observations.json',jar])
 o=read(run/'observations.json');assert o['static_skill_rows']==807 and o['native_runtime_cases']==0
 v.update(status='inventoried-all807-static-rows-target-cursor-admission-and-effect-cells-pending',observations=o,matrix=pin(run/'target-matrix.tsv'),reported_skill_matches=pin(run/'reported-skill-name-matches.tsv'),overlay=pin(overlay/'local/lito381/server/CombatDatabase.class'))
 for row in sources:assert sha(Path(row['original']['path']))==row['original']['sha256']
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'all-targeted-skill-inventory-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'],reported_skill_matches=Path(v['reported_skill_matches']['path']).read_text('utf-8').splitlines())))
