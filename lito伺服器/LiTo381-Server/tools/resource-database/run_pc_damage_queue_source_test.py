"""Execute source PC actor FIFO, bounded workers/retries, actual HP/applier and terminal fences without source boot/SQL."""
from pathlib import Path
import hashlib,json,os,subprocess
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build'
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');source=S.parents[2]/'1-伺服器/target/classes'
output=B/'pc-damage-queue-source-oracle';output.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
bind=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
reference=B/'pc-damage-commit-source-oracle-current.json'
hp=json.loads(reference.read_text('utf-8'));assert hp['status']=='passed'
pins=list(hp['source_files'])
for name in ['DamageCommit','DamageCommit$ShutdownResult','DamageCommitQueue','DamageCommitQueue$TimerRegistration','DamageCommitExecutor','DamageCommitExecutor$Snapshot','DamageCommitExecutor$CommitThreadFactory','DamageCommitExecutor$RetryThreadFactory']:
 pins.append(bind(source/('com/lineage/server/model/combat/'+name+'.class')))
 if '$' not in name:pins.append(bind(source.parents[1]/('src/com/lineage/server/model/combat/'+name+'.java')))
pins.extend(bind(p) for p in [source/'com/lineage/server/utils/ShutdownAwait.class',source.parents[1]/'src/com/lineage/server/utils/ShutdownAwait.java'])
pins=list({p['path']:p for p in pins}.values())
inputs=[bind(R/n) for n in [Path(__file__).name,'BuildPlayerDamageQueueOracle.java','BuildPlayerDamageCommitOracle.java','BuildPoisonLeaseOracle.java','BuildDeathCancellationOracle.java']]
candidate=bind(B/'candidate.jar');reference_pin=bind(reference);assert candidate['sha256']==hp['candidate_sha256']
receipt_path=B/'pc-damage-queue-source-oracle-current.json'
receipt=dict(status='running',candidate_sha256=candidate['sha256'],source_files=pins,verification_inputs=inputs,source_PC_HP_reference_receipt=reference_pin,full_behavior_parity_verified=False,native_PC_FIFO_port_verified=False,formal_deployment_changed=False,runtime_changed=False)
save=lambda:receipt_path.write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');save()
asm=Path.home()/'.m2/repository/org/ow2/asm';cp=';'.join(str(asm/f'{n}/9.7/{n}-9.7.jar') for n in ['asm','asm-tree'])
env=dict(os.environ);env.setdefault('JAVA_TOOL_OPTIONS','-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m')
try:
 with (B/'PlayerDamageQueueSourceOracle.log').open('w',encoding='utf-8') as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,check=True,stdout=log,stderr=subprocess.STDOUT,timeout=90)
  run(J/'javac.exe','-encoding','UTF-8','-cp',cp,'-d',output,*[R/n for n in ['BuildPoisonLeaseOracle.java','BuildDeathCancellationOracle.java','BuildPlayerDamageCommitOracle.java','BuildPlayerDamageQueueOracle.java']])
  run(J/'java.exe','-Xverify:all','-cp',str(output)+';'+cp,'BuildPlayerDamageQueueOracle',source,output,output/'source-pc-damage-queue-contract.json')
 for item in [candidate,reference_pin,*pins,*inputs]:assert sha(Path(item['path']))==item['sha256'],'input changed: '+item['path']
 contract=json.loads((output/'source-pc-damage-queue-contract.json').read_text('utf-8'))
 prerequisite=json.loads((output/'source-PC-HP-prerequisite.json').read_text('utf-8'))
 assert (prerequisite['cases'],prerequisite['checks'])==(535,8260) and (contract['cases'],contract['checks'])==(18,473)
 assert len(pins)==60 and len(list((output/'local/lito381/server').glob('*.class')))==53
 receipt.update(status='passed',source_PC_damage_queue_contract=contract,source_PC_HP_prerequisite_contract=prerequisite,fixture_properties=dict(commitWorkers=2,commitDispatchCapacity=256,maxTargetPending=64,maxGlobalPending=128),actual_retry_delays_ms=[5,10,20,40,80,160,320,640,1000,1000,1000,1000],generated_executable_files=[bind(p) for p in sorted((output/'local/lito381/server').glob('*.class'))],executable_contracts=[bind(output/n) for n in ['source-pc-damage-queue-contract.json','source-PC-HP-prerequisite.json']])
except BaseException as error:
 receipt.update(status='failed',failure=str(error));raise
finally:
 save();print((B/'PlayerDamageQueueSourceOracle.log').read_text('utf-8',errors='replace')[-5000:])
