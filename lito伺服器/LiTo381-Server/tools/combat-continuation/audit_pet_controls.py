"""Locate actual collar/taming charisma consumers without modifying the runtime."""
from pathlib import Path
import hashlib,json,subprocess,os
R=Path(__file__).resolve().parent;S=R.parents[1];D=R/'build/pet-control-audit';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
runtime=read(R/'poison-publication/build/build.json')['runtime'];jar=Path(runtime['path']);assert sha(jar)==runtime['sha256']
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=D/'tools';classes.mkdir(exist_ok=True)
asm=[Path(p['path']) for p in read(R/'build/integration-regression/preparation.json')['asm']];cp=';'.join(map(str,[classes,*asm]))
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
subprocess.run([str(J/'javac.exe'),'-g:none','-encoding','UTF-8','-cp',cp,'-d',str(classes),str(R/'DumpMethod.java')],env=env,check=True,capture_output=True,timeout=30)
outputs=[]
for method in ['usePetCollar','canTakeControlledCompanion']:
 out=subprocess.run([str(J/'java.exe'),'-cp',cp,'DumpMethod',str(jar),method],env=env,check=True,capture_output=True,timeout=30).stdout
 assert out.strip(),method
 p=D/(method+'.txt');p.write_bytes(out);outputs.append(pin(p))
base=Path('D:/lineage開發/1-伺服器/src');sources=[base/'com/lineage/config/ConfigCharSetting.java',base/'com/lineage/server/model/Instance/L1PcInstance.java',base/'com/lineage/server/model/skill/L1SkillUse.java',base/'com/lineage/data/item_etcitem/Pet_Collar.java']
v=dict(status='read-only-consumer-audit-not-implementation',runtime=runtime,outputs=outputs,sources=[pin(p)for p in sources],driver=pin(Path(__file__)),properties=dict(PrinceChaForPet=6,KnightChaForPet=0,ElfChaForPet=12,WizardChaForPet=6,DarkelfChaForPet=6,DragonKnightChaForPet=6,IllusionistChaForPet=6),scope=['Source canTakePet uses current effective charisma plus the per-class setting, subtracts active pet costs and requires fewer than eight controlled entities','Source L1SkillUse also calls canTakePet(6) in taming/create-zombie flows; not collar-only','Summon Monster and Elemental Summon retain their separately verified source rules; no replacement authorized by this audit','Candidate bytecode is the authority; mixed src files are discovery only'],formal_deployment_changed=False,source_database_mutated=False)
(D/'current.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8');print(v['status'])
