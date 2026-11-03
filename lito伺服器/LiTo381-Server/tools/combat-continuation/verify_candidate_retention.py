"""Byte retention is not a replacement for final SQL/native UI acceptance."""
from pathlib import Path
import hashlib,json,zipfile,subprocess,os
R=Path(__file__).resolve().parent;S=R.parents[1];L=S.parent.parent/'lito登入器/LiTo381-Launcher';D=R/'build/candidate-retention';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
protected=[(L/'app/server/LiTo381-SharedResources-candidate.jar','75da69b0d2f7f9d394ee6bf2fdb396b153c75d9b938069b0999ed8dac4f50371'),(L/'app/compat/ddraw.dll','0135e6e76594eb98695b85e9b451669adaedccfa6a13fafc49bf934d9c761de4')]
for path,expected in protected:assert sha(path)==expected,'formal changed '+str(path)
reference=R/'build/candidate-combat-buff.jar';assert sha(reference)=='97b1318aa4ab310c367ace9c3a5e5f197a899ce5875caf451e2bfa3010f50dfe'
rows=[]
with zipfile.ZipFile(reference)as source:
 for name in ['polymorph-material','return-nature-material','return-nature-transfer','polymorph-source','pet-material','pet-world-lifetime','pet-world-publication','weapon-break-numeric']:
  b=read(R/name/'build/build.json');runtime=Path(b['runtime']['path']);assert sha(runtime)==b['runtime']['sha256'];repro=read(R/name/'build-repro/build.json');assert repro['runtime']['sha256']==b['runtime']['sha256'] and sha(Path(repro['runtime']['path']))==b['runtime']['sha256']
  with zipfile.ZipFile(runtime)as candidate:
   for cls in ['SourceBuffRead','LegacyMySqlRepository']:
    key='local/lito381/server/'+cls+'.class';assert candidate.read(key)==source.read(key),'accepted BUFF consumer changed '+name+'/'+cls
  rows.append(dict(stage=name,runtime=b['runtime'],build=pin(R/name/'build/build.json'),reproduction=pin(R/name/'build-repro/build.json'),accepted_buff_classes_byte_equal=True))
name='npc-physical-source';b=read(R/name/'build/build.json');runtime=Path(b['runtime']['path']);assert sha(runtime)==b['runtime']['sha256'];repro=read(R/name/'build-repro/build.json');assert repro['runtime']['sha256']==b['runtime']['sha256'] and sha(Path(repro['runtime']['path']))==b['runtime']['sha256']
parent=Path(b['base']['path']);assert sha(parent)==b['base']['sha256']=='a58a6d1f5e547d1a30d54ec562b5ee29472330a8bb2f757f2874231d2eec9eaa'
with zipfile.ZipFile(reference)as source,zipfile.ZipFile(parent)as old,zipfile.ZipFile(runtime)as candidate:
 key='local/lito381/server/SourceBuffRead.class';assert source.read(key)==old.read(key)==candidate.read(key)
 key='local/lito381/server/LegacyMySqlRepository.class';assert source.read(key)==old.read(key)
 changed={n for n in old.namelist()if candidate.read(n)!=old.read(n)};assert changed=={'local/lito381/server/CombatDatabase.class',key}
# The repository legitimately changes one NPC read method. Prove its exact inverse;
# never waive whole-class BUFF retention for other stages or future methods.
for item in b['inputs']:assert sha(Path(item['path']))==item['sha256']
asm=[Path(item['path'])for item in read(R/'build/integration-regression/preparation.json')['asm']]
cp=';'.join(map(str,[R/name/'build/tools',*asm]));log=D/'npc-method-inverse.log'
with log.open('w',encoding='utf-8')as out:subprocess.run([r'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe','-Xverify:all','-cp',cp,'NpcPhysicalSourceMethods','verify',str(parent),str(runtime),str(R/name/'build/classes')],stdout=out,stderr=subprocess.STDOUT,check=True,timeout=60)
rows.append(dict(stage=name,runtime=b['runtime'],build=pin(R/name/'build/build.json'),reproduction=pin(R/name/'build-repro/build.json'),source_buff_class_byte_equal=True,repository_only_npc_method_changed=True,complete_method_inverse=pin(log)))
prior=b['runtime']
for name,method_tool,expected in [
 ('weapon-break-material','WeaponBreakMaterialMethods',{'ClientSession','NativeLightAction'}),
 ('pet-world-policy','WorldMaterialPolicyMethods',{'NativeCastMaterialPolicy','NativePetWorldLifetime'})]:
 b=read(R/name/'build/build.json');runtime=Path(b['runtime']['path']);assert b['base']==prior and sha(runtime)==b['runtime']['sha256'];repro=read(R/name/'build-repro/build.json');assert repro['runtime']['sha256']==b['runtime']['sha256'] and sha(Path(repro['runtime']['path']))==b['runtime']['sha256'];assert b['C0_byte_equal'] is True
 for item in b['inputs']:assert sha(Path(item['path']))==item['sha256']
 with zipfile.ZipFile(Path(prior['path']))as old,zipfile.ZipFile(runtime)as child:
  for cls in ['SourceBuffRead','LegacyMySqlRepository']:
   key='local/lito381/server/'+cls+'.class';assert child.read(key)==old.read(key)
  assert {n for n in old.namelist()if child.read(n)!=old.read(n)}=={'local/lito381/server/'+n+'.class'for n in expected}
 cp=';'.join(map(str,[R/name/'build/tools',*asm]));log=D/(name+'-method-inverse.log')
 with log.open('w',encoding='utf-8')as out:subprocess.run([r'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe','-Xverify:all','-cp',cp,method_tool,'verify',prior['path'],str(runtime)],stdout=out,stderr=subprocess.STDOUT,check=True,timeout=60)
 rows.append(dict(stage=name,runtime=b['runtime'],build=pin(R/name/'build/build.json'),reproduction=pin(R/name/'build-repro/build.json'),accepted_buff_classes_equal_to_verified_npc_parent=True,complete_method_inverse=pin(log)));prior=b['runtime']
for name,expected in [('weapon-break-lifetime',{'NativeWeaponBreakMaterialAction'}),('npc-physical-size',{'NativeNpcPhysicalSource'})]:
 b=read(R/name/'build/build.json');runtime=Path(b['runtime']['path']);assert b['base']==prior and sha(runtime)==b['runtime']['sha256'];repro=read(R/name/'build-repro/build.json');assert repro['runtime']['sha256']==b['runtime']['sha256'] and sha(Path(repro['runtime']['path']))==b['runtime']['sha256'];assert b['C0_byte_equal'] and b['all_other_entries_equal']
 for item in b['inputs']:assert sha(Path(item['path']))==item['sha256']
 with zipfile.ZipFile(Path(prior['path']))as old,zipfile.ZipFile(runtime)as child:
  assert set(old.namelist())==set(child.namelist());assert {n for n in old.namelist()if child.read(n)!=old.read(n)}=={'local/lito381/server/'+n+'.class'for n in expected}
 rows.append(dict(stage=name,runtime=b['runtime'],build=pin(R/name/'build/build.json'),reproduction=pin(R/name/'build-repro/build.json'),all_existing_other_entries_byte_equal=True));prior=b['runtime']
name='cancellation-material';b=read(R/name/'build/build.json');assert b['base']==prior;runtime=Path(b['runtime']['path']);assert sha(runtime)==b['runtime']['sha256'];repro=read(R/name/'build-repro/build.json');assert repro['runtime']['sha256']==sha(runtime)==sha(Path(repro['runtime']['path'])) and b['C0_byte_equal']
for item in b['inputs']:assert sha(Path(item['path']))==item['sha256']
with zipfile.ZipFile(Path(prior['path']))as old,zipfile.ZipFile(runtime)as child:
 assert {n for n in old.namelist()if child.read(n)!=old.read(n)}=={'local/lito381/server/'+n+'.class'for n in ['ClientSession','NativeLightAction']}
cp=';'.join(map(str,[R/name/'build/tools',*asm]));log=D/'cancellation-method-inverse.log'
with log.open('w',encoding='utf-8')as out:subprocess.run([r'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe','-Xverify:all','-cp',cp,'CancellationMaterialMethods','verify',prior['path'],str(runtime)],stdout=out,stderr=subprocess.STDOUT,check=True,timeout=60)
rows.append(dict(stage=name,runtime=b['runtime'],build=pin(R/name/'build/build.json'),reproduction=pin(R/name/'build-repro/build.json'),complete_method_inverse=pin(log),runtime_qualification_separate=True))
prior=b['runtime'];name='cancellation-speed-immunity';b=read(R/name/'build/build.json');runtime=Path(b['runtime']['path'])
assert b['base']==prior and sha(runtime)==b['runtime']['sha256'];repro=read(R/name/'build-repro/build.json');assert repro['runtime']['sha256']==sha(runtime)==sha(Path(repro['runtime']['path']))
for item in b['inputs']:assert sha(Path(item['path']))==item['sha256']
cp=';'.join(map(str,[R/name/'build/tools',*asm]));log=D/'cancellation-speed-immunity-inverse.log'
with log.open('w',encoding='utf-8')as out:subprocess.run([r'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe','-Xverify:all','-cp',cp,'CancellationSpeedImmunityMethods','verify',prior['path'],str(runtime)],stdout=out,stderr=subprocess.STDOUT,check=True,timeout=60)
rows.append(dict(stage=name,runtime=b['runtime'],build=pin(R/name/'build/build.json'),reproduction=pin(R/name/'build-repro/build.json'),complete_call_inverse=pin(log),runtime_qualification_separate=True))
prior=b['runtime'];name='cancellation-debuff-immunity';b=read(R/name/'build/build.json');runtime=Path(b['runtime']['path'])
assert b['base']==prior and sha(runtime)==b['runtime']['sha256'];repro=read(R/name/'build-repro/build.json');assert repro['runtime']['sha256']==sha(runtime)==sha(Path(repro['runtime']['path'])) and b['C0_byte_equal']
for item in b['inputs']:assert sha(Path(item['path']))==item['sha256']
with zipfile.ZipFile(Path(prior['path']))as old,zipfile.ZipFile(runtime)as child:
 assert set(old.namelist())==set(child.namelist());assert [n for n in old.namelist()if child.read(n)!=old.read(n)]==['local/lito381/server/NativeCancellationSpeedImmunity.class']
rows.append(dict(stage=name,runtime=b['runtime'],build=pin(R/name/'build/build.json'),reproduction=pin(R/name/'build-repro/build.json'),all_existing_other_entries_byte_equal=True,runtime_qualification_separate=True))
prior=b['runtime'];name='weight-material';b=read(R/name/'build/build.json');runtime=Path(b['runtime']['path'])
assert b['base']==prior and sha(runtime)==b['runtime']['sha256'];repro=read(R/name/'build-repro/build.json');assert repro['runtime']['sha256']==sha(runtime)==sha(Path(repro['runtime']['path']))
for item in b['inputs']:assert sha(Path(item['path']))==item['sha256']
cp=';'.join(map(str,[R/name/'build/tools',*asm]));log=D/'weight-material-method-inverse.log'
with log.open('w',encoding='utf-8')as out:subprocess.run([r'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe','-Xverify:all','-cp',cp,'WeightMaterialMethods','verify',prior['path'],str(runtime)],stdout=out,stderr=subprocess.STDOUT,check=True,timeout=60)
rows.append(dict(stage=name,runtime=b['runtime'],build=pin(R/name/'build/build.json'),reproduction=pin(R/name/'build-repro/build.json'),single_method_inverse=pin(log),runtime_qualification_separate=True))
pm=read(R/'polymorph-material/build/pet-replay/current.json');receipt=Path(pm['receipt']['path']);assert sha(receipt)==pm['receipt']['sha256'];driver=read(receipt)['driver'];assert sha(Path(driver['path']))==driver['sha256'],'historical PM pet replay driver changed'
v=dict(status='passed-byte-retention-only',protected=[pin(p)for p,_ in protected],accepted_buff_reference=pin(reference),candidates=rows,formal_deployment_changed=False,source_database_mutated=False,full_goal_achieved=False)
(D/'current.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8');print(v['status'])
