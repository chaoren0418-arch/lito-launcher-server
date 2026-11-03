from pathlib import Path
import subprocess,sys,json,hashlib
R=Path(__file__).resolve().parent;P=R.parent/'cancellation-npc-material';D=R/'build/inherited';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'build/build.json');rows=[]
for name,folder in [('test.py','tests'),('test_lifecycle.py','lifecycle'),('test_companions.py','companions')]:
 t=(P/name).read_text('utf-8');old="R=Path(__file__).resolve().parent";assert t.count(old)==1;t=t.replace(old,'R=Path('+repr(str(P))+')')
 old="D=R/'build/"+folder+"'";assert t.count(old)==1;t=t.replace(old,'D=Path('+repr(str(D/folder))+')')
 old="b=read(R/'build/build.json')";assert t.count(old)==1;t=t.replace(old,'b=read(Path('+repr(str(R/'build/build.json'))+'))')
 adapted=D/name;adapted.write_text(t,encoding='utf-8')
 subprocess.run([sys.executable,'-B','-X','utf8',str(adapted)],check=True)
 result=D/folder/'result.json';assert read(result)['runtime']==b['runtime']
 rows.append(dict(original=pin(P/name),adapted=pin(adapted),result=pin(result),change='Candidate JAR and evidence destination only; all assertions preserved'))
(D/'receipt.json').write_text(json.dumps(dict(status='passed',runtime=b['runtime'],tests=rows,formal_deployment_changed=False),ensure_ascii=False,indent=2),encoding='utf-8')
