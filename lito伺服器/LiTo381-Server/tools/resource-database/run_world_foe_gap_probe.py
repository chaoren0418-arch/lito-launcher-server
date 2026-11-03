"""Observe real WORLD Foe extra-damage ingress and nonpositive external control against source mode and HP references."""
from pathlib import Path
import subprocess,hashlib,json,os,sys,re
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build';L=S.parent.parent/'lito登入器/LiTo381-Launcher';J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
bind=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
env=dict(os.environ);env.setdefault('JAVA_TOOL_OPTIONS','-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m')
for runner in ['run_monster_receive_source_test.py','run_foe_mode_source_test.py']:subprocess.run([sys.executable,'-B','-X','utf8',str(R/runner)],cwd=S,env=env,check=True)
foe=json.loads((B/'foe-mode-source-oracle-current.json').read_text('utf-8'));hp=json.loads((B/'monster-receive-source-oracle-current.json').read_text('utf-8'));assert foe['status']==hp['status']=='passed';pins=list({r['path']:r for r in foe['source_files']+hp['source_files']}.values())
sources=[R/(n+'.java') for n in ['WorldFoeGapProbe','WorldMagicCommitRegression','NpcMagicRegression','NpcDamageQueueRegression','MonsterDamageCallbackRegression','RangedDamageRegression','KiringkuRegression','CombatRegression','CounterDetectionRegression','PlayerMagicRegression','PlayerMirrorRegression','PlayerDamageRegression','PatienceRegression','NpcPhysicalRegression','NpcPoisonRegression','PoisonAtomicRegression','PoisonParalysisRegression','PoisonCallbackRegression','SilenceCallbackRegression','ParalysisCallbackRegression','CurseCallbackRegression','MonsterParalysisRegression','MonsterCurseCallbackRegression','MonsterSilenceCallbackRegression','WorldCompanionPhysicalRegression','CompanionDamageRegression','ClassMagicRegression','ClassDamageRegression','WeaponBuffRegression','AwakenCombatRegression','PolluteHealingRegression','NpcDebuffRegression','ReturnNatureRegression']]
sources.extend(S/n for n in ['tools/durable-settlement/FlowRegression.java','tools/dragon-combat/Regression.java','tools/native-room-state/NativeDirectoryRegression.java','tools/shared-database/SourceLoginRegression.java'])
inputs=[bind(p) for p in [Path(__file__),*sources]];candidate=bind(B/'candidate.jar');formal=bind(L/'app/server/LiTo381-SharedResources-candidate.jar');receipt_path=B/'world-foe-gap-oracle-current.json'
receipt=dict(status='running',candidate_sha256=candidate['sha256'],source_files=pins,verification_inputs=inputs,generated_executable_files=foe['generated_executable_files']+hp['generated_executable_files'],source_foe_mode_reference_receipt=bind(B/'foe-mode-source-oracle-current.json'),source_foe_mode_contract=foe['source_foe_mode_contract'],source_monster_receive_reference_receipt=bind(B/'monster-receive-source-oracle-current.json'),source_monster_receive_contract=hp['source_monster_receive_contract'],full_behavior_parity_verified=False,formal_deployment_changed=False)
save=lambda:receipt_path.write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');save()
cp=str(B/'candidate.jar')+';'+str(S/'lib/*')
try:
 subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-implicit:none','-cp',cp,'-d',str(B/'tests'),*map(str,sources)],cwd=S,env=env,check=True)
 with (B/'WorldFoeGapProbe.log').open('w',encoding='utf-8') as log:
  result=subprocess.run([str(J/'java.exe'),'-Dfile.encoding=UTF-8','-Dlito.resourceProfile=tw13081901-v1','-Dlito.commonCatalog='+str(L/'app/common_catalog.bin'),'-Dlineage.damage.commitWorkers=2','-Dlineage.damage.commitDispatchCapacity=256','-Xverify:all','-cp',str(B/'tests')+';'+cp,'local.lito381.server.WorldFoeGapProbe',str(S/'config/server.properties'),str(S/'runtime/combat-world-foe-gap-observation'),str(B/'world-foe-native-observations.json')],cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,timeout=100)
 output=(B/'WorldFoeGapProbe.log').read_text('utf-8');print(output[-5000:]);assert result.returncode==0,'native magic observation fixture failed'
 for item in [candidate,formal,*pins,*inputs]:assert sha(Path(item['path']))==item['sha256'],'input changed: '+item['path']
 observed=json.loads((B/'world-foe-native-observations.json').read_text('utf-8'))
 assert observed['real_C_USE_SPELL187'] and observed['native_physical_strikes']==5 and observed['native_physical_damage_sum']>0
 assert observed['known_differences']==int(observed['native_held_HP']!=40000)+int(observed['negative_control_throws'])
 assert (observed['native_held_HP'],observed['native_pending_events']) in [(39999,6),(40000,7)]
 assert observed['negative_control_rng_draws']==int(observed['negative_control_throws']) and observed['negative_control_real_packet_rejected_as_malformed']==observed['negative_control_throws']
 assert not observed['native_full_Foe_parity_verified']
 receipt.update(status='passed',native_Foe_ingress_and_nonpositive_control_observation_verified=True,native_foe_commit_port_verified=False,native_observation_contract=observed,native_observation_receipt=bind(B/'world-foe-native-observations.json'),native_fixture_classes=[bind(p) for p in sorted((B/'tests/local/lito381/server').glob('WorldFoeGapProbe*.class'))],native_observation_checks=int(re.search(r'PASS WORLD Foe gap observations checks=(\d+);',output)[1]))

except BaseException as error:
 receipt.update(status='failed',failure=str(error));raise
finally:save()
