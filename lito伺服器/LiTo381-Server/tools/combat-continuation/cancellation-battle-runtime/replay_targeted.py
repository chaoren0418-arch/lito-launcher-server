import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from regression_support import Options, adapt_driver, write, java_tool, formal_protection
options=Options()
from pathlib import Path
import subprocess,sys,json,hashlib
CODE=Path(__file__).resolve().parent;R=options.root/'cancellation-battle-runtime';D=options.destination('targeted', CODE/'build/targeted')
read=lambda p:json.loads(p.read_text('utf8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=options.build('cancellation-battle-runtime');rows=[];specs=[];write(D/'build.json',b)
for stage,folders in [('call-nature-material',['tests','boundaries','lifecycle','world-npc','companions','battle-npc','battle-lifecycle']),('mass-teleport-lifetime',['tests','races','material-replay','boundaries']),('area-sleep-slow',['tests','boundaries','lifecycle','npc','companions','battle-npc']),('area-material',['tests','boundaries','lifecycle','npc']),('cancellation-npc-runtime',['tests','boundaries']),('cancellation-battle-material',['tests','lifecycle'])]:
 for folder in folders:
  name={'tests':'test.py','world-npc':'test_npc.py','material-replay':'test_material.py'}.get(folder,'test_'+folder.replace('-','_')+'.py');specs.append((stage,name,folder))
if options.args.jar or options.out:
 specs=[('cancellation-battle-runtime','test.py','tests'),('cancellation-battle-runtime','test_boundaries.py','boundaries')]+specs
for folder in ([] if options.args.jar or options.out else ['tests','boundaries']):
 result=R/'build'/folder/'result.json';v=read(result);assert v['status'].startswith('passed') and v['runtime']==b['runtime'];rows.append(dict(original=pin(R/('test.py'if folder=='tests'else'test_boundaries.py')),result=pin(result),change='Reuse completed replacement/fault receipt for exact same candidate'))
status='running';protection={}
write(D/'receipt.json',dict(status=status,runtime=b['runtime'],tests=rows))
try:
 with formal_protection(options,protection):
  for stage,name,folder in specs:
   P=R.parent/stage;dest=D/stage;dest.mkdir(exist_ok=True)
   t=(CODE.parent/stage/name).read_text('utf8');old="R=Path(__file__).resolve().parent";assert t.count(old)==1;t=t.replace(old,'R=Path('+repr(str(P))+')')
   if name=='test_boundaries.py':
    old="D=R/'build'";assert t.count(old)==1;t=t.replace(old,'D=Path('+repr(str(dest))+')');(dest/'build.json').write_text(json.dumps(b,ensure_ascii=False),encoding='utf8')
   else:
    old="D=R/'build/"+folder+"'";assert t.count(old)==1;t=t.replace(old,'D=Path('+repr(str(dest/folder))+')')
    old="b=read(R/'build/build.json')";assert t.count(old)==1;t=t.replace(old,'b=read(Path('+repr(str(D/'build.json'))+'))')
   t=adapt_driver(t,options.config,dest/'runtime');adapted=dest/name;adapted.write_text(t,encoding='utf8');print('RUN '+stage+'/'+folder,flush=True);subprocess.run([sys.executable,'-B','-X','utf8',str(adapted)],check=True)
   result=dest/folder/'result.json';assert read(result)['runtime']==b['runtime'];rows.append(dict(original=pin(CODE.parent/stage/name),adapted=pin(adapted),result=pin(result),change='Candidate/evidence paths only; all behavioral assertions preserved'))
   (D/'receipt.json').write_text(json.dumps(dict(status='passed'if len(rows)==27 else'running',runtime=b['runtime'],tests=rows,formal_deployment_changed=False),ensure_ascii=False,indent=2),encoding='utf8')
  status='passed'
except BaseException:
 status='failed'
 raise
finally:
 write(D/'receipt.json',dict(status=status if status=='passed' else 'failed',runtime=b['runtime'],tests=rows,**{'formal_deployment_changed':False,**protection}))
