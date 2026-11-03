from pathlib import Path
import json,hashlib,shutil,subprocess,os
R=Path(__file__).resolve().parent;S=R.parents[1];B=S/'tools/resource-database/build';D=R/'build/integration-regression';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(D/'build-current.json');assert sha(Path(b['runtime']['path']))==b['runtime']['sha256'];seed=read(Path(b['manifest']['path']));out=D/'remaining-census';out.mkdir(exist_ok=True);classes=out/'classes';classes.mkdir(exist_ok=True);source=out/'InspectNativeMaterialBodies.java';shutil.copyfile(B/source.name,source)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=';'.join(map(str,[classes,*[r['path'] for r in seed['asm']]]));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2')
with (out/'census.log').open('w',encoding='utf-8') as log:
 subprocess.run([str(J/'javac.exe'),'-g:none','-encoding','UTF-8','-cp',cp,'-d',str(classes),str(source)],stdout=log,stderr=subprocess.STDOUT,env=env,check=True,timeout=90)
 with (out/'bodies.tsv').open('w',encoding='utf-8') as data:subprocess.run([str(J/'java.exe'),'-Xverify:all','-cp',cp,'InspectNativeMaterialBodies',b['runtime']['path']],stdout=data,stderr=log,env=env,check=True,timeout=90)
rows=(out/'bodies.tsv').read_text('utf-8').splitlines();assert rows[-1]=='TOTAL\t45\t46';assert not any('\tuseMagicalWeaponSpell\t' in r or '\tnativeMaterialMagicalWeaponBody\t' in r for r in rows)
v={'status':'passed-static-material-census','runtime':b['runtime'],'remaining_methods':45,'remaining_legacy_calls':46,'inputs':[pin(source),pin(Path(__file__))],'output':pin(out/'bodies.tsv'),'formal_deployment_changed':False,'full_goal_achieved':False};(out/'receipt.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8');print(rows[-1])
