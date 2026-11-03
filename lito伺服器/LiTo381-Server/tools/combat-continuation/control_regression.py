"""New evidence destination; all behavioral assertions retained from the qualified parent drivers."""
from pathlib import Path
import json,hashlib,subprocess,sys
R=Path(__file__).resolve().parent;D=R/'build/control-regression';D.mkdir(exist_ok=True);read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
b=read(R/'bind-control/build/build.json');runtime=b['runtime'];assert sha(Path(runtime['path']))==runtime['sha256'];seed=read(R/'build/integration-regression/preparation.json')
if not (D/'build-current.json').exists():
 p={'status':'controls-candidate-under-test','runtime':runtime,'parent_build':pin(R/'bind-control/build/build.json'),'asm':seed['asm'],'formal_deployment_changed':False,'full_goal_achieved':False};write(D/'preparation.json',p);write(D/'build-current.json',{'status':'candidate-under-test','runtime':runtime,'manifest':pin(D/'preparation.json')})
assert read(D/'build-current.json')['runtime']==runtime
name='affected_regressions.py' if '--affected' in sys.argv else 'full_regression.py';t=(R/name).read_text('utf-8')
if name=='full_regression.py':
 old="D=Path('D:\\\\lineage開發\\\\3-lito開發\\\\lito伺服器\\\\LiTo381-Server\\\\tools\\\\combat-continuation\\\\build\\\\integration-regression')";assert t.count(old)==1;t=t.replace(old,'D=Path('+repr(str(D))+')')
else:
 t=t.replace("R=Path(__file__).resolve().parent;S=R.parents[1];B=S/'tools/resource-database/build';D=R/'build/integration-regression'",'R=Path('+repr(str(R))+');S=R.parents[1];B=S/\'tools/resource-database/build\';D=Path('+repr(str(D))+')')
adapted=D/name;adapted.write_text(t,encoding='utf-8');write(D/(name+'.adaptation.json'),{'original':pin(R/name),'adapted':pin(adapted),'runtime':runtime,'change':'destination only; original same-candidate reuse and explicit historical formal guard reconciliation remain intact'})
sys.exit(subprocess.run([sys.executable,'-B','-X','utf8',str(adapted)],cwd=R.parents[1]).returncode)
