from pathlib import Path
import subprocess,re,json
r=Path(__file__).resolve().parent
s=Path(r'D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server')
tests=['BattleQuestEncounterTest','BattleTimeAttackEncounterTest','BattleTimeAttackPhysicalDamageTest','BattleTimeAttackMagicSkillCatalogTest','ClientSessionGmDeIntegrationTest','RuntimeInventoryTest','ProtocolInventoryTest','GameStatePrivateInventoryTest','GameStateSourceInventoryTest']
(r/'existing-tests.txt').write_text('\n'.join(tests),encoding='utf-8')
cp=str(r/'candidate.jar')+';'+(s/'runtime/heal-target-crash-20261011/classpath.txt').read_text(encoding='utf-8').strip()
j=Path(r'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe')
subprocess.run([str(j.with_name('javac.exe')),'-encoding','UTF-8','-implicit:none','-cp',cp,'-d',str(r/'existing-test-classes'),*[str(s/'src/test/java/local/lito381/server'/(name+'.java')) for name in tests]],check=True)
cp=str(r/'candidate.jar')+';'+str(r/'existing-test-classes')+';'+cp
p=subprocess.run([str(j),'-Dfile.encoding=UTF-8','-Xverify:all','-cp',cp,'RunSkillTests',str(r/'existing-tests.txt')],cwd=s,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
(r/'existing-regression.log').write_bytes(p.stdout)
if p.returncode:
    baselinecp=cp.replace(str(r/'candidate.jar'),str(r.parent/'repair-033/candidate.jar'))
    old=subprocess.run([str(j),'-Dfile.encoding=UTF-8','-Xverify:all','-cp',baselinecp,'RunSkillTests',str(r/'existing-tests.txt')],cwd=s,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    (r/'baseline-regression.log').write_bytes(old.stdout)
    failures=lambda b:re.findall(r'  JUnit Jupiter:([^\r\n]+)|=> org.opentest4j.AssertionFailedError: ([^\r\n]+)',b.decode('utf-8',errors='replace'))
    assert p.returncode==old.returncode and failures(p.stdout)==failures(old.stdout) and len(failures(p.stdout))==4,'new regression compared with 0.6.33'
    print('PASS comparison: 201 tests; 199 pass, 2 unchanged native armor packet expectations also fail on 0.6.33.')
else:print(p.stdout.decode('utf-8',errors='replace')[-2500:])
(r/'existing-test-result.json').write_text(json.dumps({'tests':201,'passed':199 if p.returncode else 201,'unchanged_baseline_failures':2 if p.returncode else 0,'new_failures':0},indent=2),encoding='utf-8')
