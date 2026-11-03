"""Execute actual source freeze effects with typed transport/spawn edges; never boots source."""
from pathlib import Path
import hashlib,json,os,subprocess
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build';J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');origin=S.parents[2]/'1-伺服器';source=origin/'target/classes';out=B/'freeze-effect-source-oracle';out.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();bind=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
names=['model/skill/L1SkillUse','model/L1Character','templates/L1Skills','serverpackets/S_Poison','serverpackets/S_Paralysis','serverpackets/ServerBasePacket','utils/L1SpawnUtil']
pins=[bind(p) for n in names for p in [source/('com/lineage/server/'+n+'.class'),origin/('src/com/lineage/server/'+n+'.java')]]
pins.extend(bind(source/('com/lineage/server/model/Instance/L1'+n+'Instance.class')) for n in ['Pc','Npc','Monster','Summon','Pet'])
inputs=[bind(p) for p in [Path(__file__),R/'BuildFreezeEffectOracle.java',R/'BuildPoisonLeaseOracle.java',R/'BuildDeathCancellationOracle.java']];candidate=bind(B/'candidate.jar');receipt_path=B/'freeze-effect-source-oracle-current.json'
receipt=dict(status='running',candidate_sha256=candidate['sha256'],source_files=pins,verification_inputs=inputs,full_behavior_parity_verified=False,formal_deployment_changed=False,native_freeze_commit_port_verified=False)
save=lambda:receipt_path.write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');save()
asm=Path.home()/'.m2/repository/org/ow2/asm';cp=';'.join(str(asm/f'{n}/9.7/{n}-9.7.jar') for n in ['asm','asm-tree']);env=dict(os.environ);env.setdefault('JAVA_TOOL_OPTIONS','-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m')
try:
 with (B/'FreezeEffectSourceOracle.log').open('w',encoding='utf-8') as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,check=True,stdout=log,stderr=subprocess.STDOUT,timeout=90)
  run(J/'javac.exe','-encoding','UTF-8','-cp',cp,'-d',out,R/'BuildFreezeEffectOracle.java',R/'BuildPoisonLeaseOracle.java',R/'BuildDeathCancellationOracle.java')
  run(J/'java.exe','-Xverify:all','-cp',str(out)+';'+cp,'BuildFreezeEffectOracle',source,out,out/'source-freeze-effect-contract.json')
 for item in [candidate,*pins,*inputs]:assert sha(Path(item['path']))==item['sha256'],'input changed: '+item['path']
 receipt.update(status='passed',source_freeze_effect_contract=json.loads((out/'source-freeze-effect-contract.json').read_text('utf-8')),generated_executable_files=[bind(p) for p in sorted((out/'local/lito381/server').glob('*.class'))],executable_contracts=[bind(out/'source-freeze-effect-contract.json')])
except BaseException as error:receipt.update(status='failed',failure=str(error));raise
finally:save();print((B/'FreezeEffectSourceOracle.log').read_text('utf-8',errors='replace')[-4500:])
