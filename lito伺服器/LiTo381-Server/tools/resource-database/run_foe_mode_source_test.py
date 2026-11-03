"""Execute actual source Foe mode bodies with typed actions, transport and RNG; never boots source."""
from pathlib import Path
import hashlib,json,os,subprocess
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build';J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');origin=S.parents[2]/'1-伺服器';source=origin/'target/classes';out=B/'foe-mode-source-oracle';out.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();bind=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
names=['model/skill/skillmode/FOE_SLAYER','model/skill/skillmode/SkillMode','model/L1Character','model/L1Magic','model/Instance/L1PcInstance','model/Instance/L1NpcInstance','serverpackets/S_SkillSound','serverpackets/ServerBasePacket','serverpackets/OpcodesServer']
pins=[bind(p) for n in names for p in [source/('com/lineage/server/'+n+'.class'),origin/('src/com/lineage/server/'+n+'.java')]]
pins.extend(bind(p) for p in [source/'com/lineage/config/ConfigSkill.class',origin/'src/com/lineage/config/ConfigSkill.java'])
inputs=[bind(p) for p in [Path(__file__),R/'BuildFoeSlayerOracle.java',R/'BuildPoisonLeaseOracle.java']];candidate=bind(B/'candidate.jar');receipt_path=B/'foe-mode-source-oracle-current.json'
receipt=dict(status='running',candidate_sha256=candidate['sha256'],source_files=pins,verification_inputs=inputs,full_behavior_parity_verified=False,formal_deployment_changed=False,native_foe_commit_port_verified=False)
save=lambda:receipt_path.write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');save()
asm=Path.home()/'.m2/repository/org/ow2/asm';cp=';'.join(str(asm/f'{n}/9.7/{n}-9.7.jar') for n in ['asm','asm-tree']);env=dict(os.environ);env.setdefault('JAVA_TOOL_OPTIONS','-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m')
try:
 with (B/'FoeModeSourceOracle.log').open('w',encoding='utf-8') as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,check=True,stdout=log,stderr=subprocess.STDOUT,timeout=90)
  run(J/'javac.exe','-encoding','UTF-8','-cp',cp,'-d',out,R/'BuildFoeSlayerOracle.java',R/'BuildPoisonLeaseOracle.java')
  run(J/'java.exe','-Xverify:all','-cp',str(out)+';'+cp,'BuildFoeSlayerOracle',source,out,out/'source-foe-mode-contract.json')
 for item in [candidate,*pins,*inputs]:assert sha(Path(item['path']))==item['sha256'],'input changed: '+item['path']
 receipt.update(status='passed',source_foe_mode_contract=json.loads((out/'source-foe-mode-contract.json').read_text('utf-8')),generated_executable_files=[bind(p) for p in sorted((out/'local/lito381/server').glob('*.class'))],executable_contracts=[bind(out/'source-foe-mode-contract.json')])
except BaseException as error:receipt.update(status='failed',failure=str(error));raise
finally:save();print((B/'FoeModeSourceOracle.log').read_text('utf-8',errors='replace')[-4500:])
