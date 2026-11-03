"""Execute actual compiled source physical scalar prefix; no source boot or SQL."""
from pathlib import Path
import hashlib,json,os,subprocess
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build'
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
source=S.parents[2]/'1-伺服器/target/classes';output=B/'world-physical-source-oracle';output.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
bind=lambda p:{'path':str(p.resolve()),'sha256':sha(p)}
names=['server/model/L1AttackPc','server/model/combat/PcPhysicalAttackFlow','server/model/combat/PhysicalHitCommitPlan','server/model/skill/L1SkillId','config/ConfigSkill']
pins=[bind(p) for name in names for p in [source/('com/lineage/'+name+'.class'),source.parents[1]/('src/com/lineage/'+name+'.java')]]
inputs=[bind(R/n) for n in [Path(__file__).name,'BuildWorldPhysicalCommitOracle.java','BuildPoisonLeaseOracle.java','BuildDeathCancellationOracle.java']]
candidate=bind(B/'candidate.jar');receipt_path=B/'world-physical-source-oracle-current.json'
receipt=dict(status='running',candidate_sha256=candidate['sha256'],source_files=pins,verification_inputs=inputs,full_behavior_parity_verified=False,formal_deployment_changed=False)
save=lambda:receipt_path.write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
save()
asm=Path.home()/'.m2/repository/org/ow2/asm';cp=';'.join(str(asm/f'{n}/9.7/{n}-9.7.jar') for n in ['asm','asm-tree'])
env=dict(os.environ);env.setdefault('JAVA_TOOL_OPTIONS','-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m')
try:
 with (B/'WorldPhysicalSourceOracle.log').open('w',encoding='utf-8') as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,check=True,stdout=log,stderr=subprocess.STDOUT,timeout=90)
  run(J/'javac.exe','-encoding','UTF-8','-cp',cp,'-d',output,R/'BuildPoisonLeaseOracle.java',R/'BuildDeathCancellationOracle.java',R/'BuildWorldPhysicalCommitOracle.java')
  run(J/'java.exe','-Xverify:all','-cp',str(output)+';'+cp,'BuildWorldPhysicalCommitOracle',source,output,output/'source-world-physical-contract.json')
 for item in [candidate,*pins,*inputs]:assert sha(Path(item['path']))==item['sha256'],'input changed: '+item['path']
 receipt.update(status='passed',source_world_physical_contract=json.loads((output/'source-world-physical-contract.json').read_text('utf-8')),generated_executable_files=[bind(p) for p in sorted((output/'local/lito381/server').glob('*.class'))],executable_contracts=[bind(output/'source-world-physical-contract.json')])
except BaseException as error:
 receipt.update(status='failed',failure=str(error));raise
finally:
 save();print((B/'WorldPhysicalSourceOracle.log').read_text('utf-8',errors='replace')[-5000:])
