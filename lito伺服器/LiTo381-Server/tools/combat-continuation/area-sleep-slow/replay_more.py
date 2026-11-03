from pathlib import Path
import subprocess,sys,json,hashlib
R=Path(__file__).resolve().parent;D=R/'build/inherited-more';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'build/build.json');rows=[]
for stage,name,folder in [('cancellation-npc-runtime','test.py','tests'),('cancellation-npc-runtime','test_boundaries.py','boundaries'),('area-material','test_boundaries.py','boundaries')]:
 P=R.parent/stage;dest=D/stage;dest.mkdir(exist_ok=True)
 t=(P/name).read_text('utf-8');old="R=Path(__file__).resolve().parent";assert t.count(old)==1;t=t.replace(old,'R=Path('+repr(str(P))+')')
 if name=='test_boundaries.py':
  old="D=R/'build'";assert t.count(old)==1;t=t.replace(old,'D=Path('+repr(str(dest))+')');(dest/'build.json').write_text(json.dumps(b,ensure_ascii=False),encoding='utf-8')
 else:
  old="D=R/'build/"+folder+"'";assert t.count(old)==1;t=t.replace(old,'D=Path('+repr(str(dest/folder))+')')
  old="b=read(R/'build/build.json')";assert t.count(old)==1;t=t.replace(old,'b=read(Path('+repr(str(R/'build/build.json'))+'))')
 adapted=dest/name;adapted.write_text(t,encoding='utf-8');subprocess.run([sys.executable,'-B','-X','utf8',str(adapted)],check=True)
 result=dest/folder/'result.json';assert read(result)['runtime']==b['runtime'];rows.append(dict(original=pin(P/name),adapted=pin(adapted),result=pin(result),change='Runtime and evidence paths only; assertions preserved'))
(D/'receipt.json').write_text(json.dumps(dict(status='passed',runtime=b['runtime'],tests=rows,formal_deployment_changed=False),ensure_ascii=False,indent=2),encoding='utf-8')
