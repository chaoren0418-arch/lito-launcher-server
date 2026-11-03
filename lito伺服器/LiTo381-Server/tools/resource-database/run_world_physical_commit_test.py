"""Hash-bound real native WORLD physical and doll commit regression."""
from pathlib import Path
import subprocess,hashlib,json,os,sys,re
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build';L=S.parent.parent/'lito登入器/LiTo381-Launcher'
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=str(B/'candidate.jar')+';'+str(S/'lib/*')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
bind=lambda p:{'path':str(p.resolve()),'sha256':sha(p)}
env=dict(os.environ);env.setdefault('JAVA_TOOL_OPTIONS','-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m')
for name in ['run_monster_receive_source_test.py','run_world_physical_source_test.py']:subprocess.run([sys.executable,'-B','-X','utf8',str(R/name)],cwd=S,env=env,check=True)
reference=json.loads((B/'monster-receive-source-oracle-current.json').read_text('utf-8'));physical=json.loads((B/'world-physical-source-oracle-current.json').read_text('utf-8'));assert reference['status']==physical['status']=='passed'
sources=[R/(n+'.java') for n in ['RangedDamageRegression','KiringkuRegression','CombatRegression','CounterDetectionRegression','PlayerMagicRegression','PlayerMirrorRegression','PlayerDamageRegression','PatienceRegression','NpcPhysicalRegression','NpcPoisonRegression','PoisonAtomicRegression','PoisonParalysisRegression','PoisonCallbackRegression','SilenceCallbackRegression','ParalysisCallbackRegression','CurseCallbackRegression','MonsterParalysisRegression','MonsterCurseCallbackRegression','MonsterSilenceCallbackRegression','WorldCompanionPhysicalRegression','CompanionDamageRegression','ClassMagicRegression','ClassDamageRegression','WeaponBuffRegression','AwakenCombatRegression','MonsterDamageCallbackRegression','NpcDamageQueueRegression','WorldPhysicalCommitRegression']]
sources.extend(S/n for n in ['tools/durable-settlement/FlowRegression.java','tools/dragon-combat/Regression.java','tools/native-room-state/NativeDirectoryRegression.java','tools/shared-database/SourceLoginRegression.java'])
inputs=[bind(p) for p in [Path(__file__),*sources]];pins=list({p['path']:p for p in reference['source_files']+physical['source_files']}.values())
candidate=bind(B/'candidate.jar');formal=bind(L/'app/server/LiTo381-SharedResources-candidate.jar');receipt_path=B/'world-physical-commit-oracle-current.json'
receipt=dict(status='running',candidate_sha256=candidate['sha256'],source_files=pins,verification_inputs=inputs,generated_executable_files=reference['generated_executable_files']+physical['generated_executable_files'],source_monster_receive_reference_receipt=bind(B/'monster-receive-source-oracle-current.json'),source_world_physical_reference_receipt=bind(B/'world-physical-source-oracle-current.json'),source_monster_receive_contract=reference['source_monster_receive_contract'],source_world_physical_contract=physical['source_world_physical_contract'],full_behavior_parity_verified=False,formal_deployment_changed=False)
save=lambda:receipt_path.write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');save()
try:
 subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-implicit:none','-cp',cp,'-d',str(B/'tests'),*map(str,sources)],cwd=S,env=env,check=True)
 with (B/'WorldPhysicalCommitRegression.log').open('w',encoding='utf-8') as log:
  result=subprocess.run([str(J/'java.exe'),'-Dfile.encoding=UTF-8','-Dlito.resourceProfile=tw13081901-v1','-Dlito.commonCatalog='+str(L/'app/common_catalog.bin'),'-Dlineage.damage.commitWorkers=2','-Dlineage.damage.commitDispatchCapacity=256','-Xverify:all','-cp',str(B/'tests')+';'+cp,'local.lito381.server.WorldPhysicalCommitRegression',str(S/'config/server.properties'),str(S/'runtime/combat-world-physical-commit-regression')],cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,timeout=100)
 output=(B/'WorldPhysicalCommitRegression.log').read_text('utf-8');print(output[-6000:]);assert result.returncode==0,'native WORLD physical commit regression failed'
 for item in [candidate,formal,*pins,*inputs]:assert sha(Path(item['path']))==item['sha256'],'input changed: '+item['path']
 receipt.update(status='passed',native_WORLD_physical_poison_doll_shared_FIFO_verified=True,native_post_action_Striker_and_signed_HP_verified=True,native_all_NPC_ingress_FIFO_or_full_HP_kernel_verified=False,native_fixture_classes=[bind(p) for p in sorted((B/'tests/local/lito381/server').glob('WorldPhysicalCommitRegression*.class'))],native_regression_checks=int(re.search(r'PASS WORLD physical commit checks=(\d+);',output)[1]))
except BaseException as error:
 receipt.update(status='failed',failure=str(error));raise
finally:save()
