"""Promote a scoped, hash-verified development checkpoint; never deploy."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil
R=Path(__file__).resolve().parent;C=R.parent;S=R.parents[2];D=R/'build';B=S/'tools/resource-database/build';L=S.parents[1]/'lito登入器/LiTo381-Launcher'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
protected=[(B/'candidate.jar','efd4bc36b47db13cc0bd52426355a4250ea7db4b88ac724afa653a416954ab11'),(L/'app/server/LiTo381-SharedResources-candidate.jar','75da69b0d2f7f9d394ee6bf2fdb396b153c75d9b938069b0999ed8dac4f50371'),(S/'tools/resource-database/inputs/base-0.6.61.jar','9719f45db324206617726ce06d1a9e2184e9a3b3aefab3d50f3ebbdfbf09cfac'),(L/'backups/baseline-0.6.66.zip','d6717e7c8bc754d1fcb133e76d3fd6adb838dba71d2d74010fa62305891a829b'),(B/'native-integrated-current.json','09ba0e331193cdd37f23a1048822b04db89b1101bf5315e4d75ff6cb2d6a45d9')]
for p,digest in protected:assert sha(p)==digest,(str(p),sha(p))
build=read(D/'weapon-target-build-current.json');suite=read(D/'weapon-target-suite-current.json');assert suite['status'].startswith('passed') and suite['runtime']==build['runtime'];assert sha(Path(build['runtime']['path']))==build['runtime']['sha256']
for ref in [build['manifest'],suite['manifest']]:assert sha(Path(ref['path']))==ref['sha256']
receipt=read(Path(suite['manifest']['path']));assert receipt['cases']==2312 and len(receipt['results'])==17 and not receipt['same_SHA_full_regression_passed']
for result in receipt['results']:
 for ref in [result['receipt'],result['manifest'],result['log']]:assert sha(Path(ref['path']))==ref['sha256']
 current=read(Path(result['receipt']['path']));assert current['runtime']==build['runtime'] and current['status'].startswith('passed')
parent_path=R/'current.json';parent=read(parent_path);old=C/'continuation-current.json';prior=read(old);old_hash=sha(old)
assert parent['runtime'] in [read(Path(build['manifest']['path']))['base'],build['runtime']],'checkpoint parent changed; reconcile before promotion'
run=D/'checkpoints'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True)
shutil.copyfile(__file__,run/Path(__file__).name);shutil.copyfile(old,run/'previous-continuation.json');shutil.copyfile(parent_path,run/'previous-integration.json')
reproduction=read(D/'weapon-battle-companion-reproduction-current.json');assert reproduction['status'].startswith('passed') and reproduction['runtime']==build['runtime'];assert sha(Path(reproduction['manifest']['path']))==reproduction['manifest']['sha256']
refs={}
for name in ['weapon-target-build-current','weapon-target-suite-current','weapon-battle-companion-reproduction-current']:
 p=run/(name+'.json');shutil.copyfile(D/p.name,p);refs[name]=pin(p)
v=dict(time_utc=datetime.now(timezone.utc).isoformat(),status='integrated-weapon-target-pair-Ctrl-Main-source-coverage-open',runtime=build['runtime'],receipts=refs,driver=pin(run/Path(__file__).name),previous_continuation=pin(run/'previous-continuation.json'),previous_integration=pin(run/'previous-integration.json'),protected=[pin(p) for p,_ in protected],qualified_scope=dict(latest_same_SHA_cases=2312,latest_same_SHA_known_checks=receipt['known_checks'],check_count_complete=False,skill27_NPC_normal_cases=96,skill27_NPC_Ctrl_cases=96,skill27_WORLD_companion_normal_cases=60,skill27_WORLD_companion_Ctrl_cases=60,skill27_BATTLE_companion_normal_cases=60,skill27_BATTLE_companion_Ctrl_cases=60,skill27_WORLD_companion_semantics_cases=60,skill27_BATTLE_companion_semantics_cases=60,skill27_NPC_companion_material_fault_cases=32,skill27_MP_payload_alias_cases=20,skill27_class_original_INT_target_Ctrl_cases=1176,skill27_player_material_fault_lifecycle_policy_cases=200,pair_and_Ctrl_cases=288,NPC_physical_area_cases=26,Main_skill68_cases=10,Main_skill73_cases=8),remaining=['Skill27 expanded class/target/competition/control matrix and NPC close subpaths remain open; qualified WORLD/BATTLE companion cases do not establish every Source case','All remaining material skills and full Source semantics including class/target/Ctrl/area/effect lifetime coverage; static census/call counts are not behavioral completion','NPC bag Source capacity/long-count/provenance parity; preserve complete over-capacity capture rejection without item loss','Broader NPC factories/expiry/close, exact obstacle-home routing/LOS selection and forced relocation/spawn collision parity','Final same-SHA affected/composed/full regression, readonly Source startup/close, reproducible rebuild, then one consolidated human UI checklist'],same_SHA_full_regression_passed=False,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False,native_client_launched=False,platform_goal_reported_status='blocked-not-reactivatable-by-update-tool')
write(run/'manifest.json',v)
assert sha(old)==old_hash and read(parent_path)==parent,'concurrent continuation update: reconcile before write'
write(parent_path,dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),full_goal_achieved=False))
prior.update(time_utc=v['time_utc'],status=v['status'],integration_lifecycle=pin(parent_path),integration_lifecycle_candidate=v['runtime'],integration_lifecycle_candidate_full_regression_passed=False);write(old,prior)
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],same_SHA_targeted_cases=2312,full_goal_achieved=False)))
