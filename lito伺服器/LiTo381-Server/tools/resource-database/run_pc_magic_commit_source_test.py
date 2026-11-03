"""Pinned actual PC magic handoff, receiver and accepted hit plans; typed Proc/actor/queue boundaries."""
from pathlib import Path
import hashlib,json,os,subprocess
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build'
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');source=S.parents[2]/'1-伺服器/target/classes'
output=B/'pc-magic-commit-source-oracle';output.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
bind=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
names=['model/combat/'+n for n in ['MagicHitCommitPlan','DamageCommitOutcome','DamageCommitPlan','CompositeDamageCommitPlan','DamageEvent','PcIncomingHitCommitPlan','LegacyDefenseFeedbackPlan']]+['model/L1MagicPc','model/L1MagicMode','model/Instance/L1PcInstance','model/Instance/L1NpcInstance','model/Instance/L1EffectInstance','model/L1Character','model/L1Object']
pins=[bind(p) for name in names for p in [source/('com/lineage/server/'+name+'.class'),source.parents[1]/('src/com/lineage/server/'+name+'.java')]]
pins.append(bind(source/'com/lineage/server/model/combat/DamageCommitPlan$NoopPlan.class'))
pins.extend(bind(p) for n in ['ConfigSkill','ConfigOther','ConfigAlt'] for p in [source/('com/lineage/config/'+n+'.class'),source.parents[1]/('src/com/lineage/config/'+n+'.java')])
inputs=[bind(R/n) for n in [Path(__file__).name,'BuildPlayerMagicCommitOracle.java','BuildPoisonLeaseOracle.java','BuildDeathCancellationOracle.java']]
candidate=bind(B/'candidate.jar');receipt_path=B/'pc-magic-commit-source-oracle-current.json'
receipt=dict(status='running',candidate_sha256=candidate['sha256'],source_files=pins,verification_inputs=inputs,full_behavior_parity_verified=False,formal_deployment_changed=False)
save=lambda:receipt_path.write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');save()
asm=Path.home()/'.m2/repository/org/ow2/asm';cp=';'.join(str(asm/f'{n}/9.7/{n}-9.7.jar') for n in ['asm','asm-tree'])
env=dict(os.environ);env.setdefault('JAVA_TOOL_OPTIONS','-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m')
try:
 with (B/'PlayerMagicCommitSourceOracle.log').open('w',encoding='utf-8') as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,check=True,stdout=log,stderr=subprocess.STDOUT,timeout=90)
  run(J/'javac.exe','-encoding','UTF-8','-cp',cp,'-d',output,R/'BuildPoisonLeaseOracle.java',R/'BuildDeathCancellationOracle.java',R/'BuildPlayerMagicCommitOracle.java')
  run(J/'java.exe','-Xverify:all','-cp',str(output)+';'+cp,'BuildPlayerMagicCommitOracle',source,output,output/'source-pc-magic-commit-contract.json')
 for item in [candidate,*pins,*inputs]:assert sha(Path(item['path']))==item['sha256'],'input changed: '+item['path']
 receipt.update(status='passed',source_PC_magic_commit_contract=json.loads((output/'source-pc-magic-commit-contract.json').read_text('utf-8')),generated_executable_files=[bind(p) for p in sorted((output/'local/lito381/server').glob('*.class'))],executable_contracts=[bind(output/'source-pc-magic-commit-contract.json')])
except BaseException as error:
 receipt.update(status='failed',failure=str(error));raise
finally:
 save();print((B/'PlayerMagicCommitSourceOracle.log').read_text('utf-8',errors='replace')[-5000:])
