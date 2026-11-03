"""Pure comparison with the exact current Source selection body; no database or native server."""
from pathlib import Path
import json,hashlib,subprocess,shutil
R=Path(__file__).resolve().parent;D=R/'build/plan';D.mkdir(parents=True,exist_ok=True);C=D/'classes';C.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
source=Path('D:/lineage開發/1-伺服器/src/com/lineage/server/datatables/WandPolyTable.java');text=source.read_text('utf-8');start=text.index('public int getRandomPolyId()');left=text.index('{',start);depth=1;right=left+1
while depth:
 if text[right]=='{':depth+=1
 elif text[right]=='}':depth-=1
 right+=1
body=text[start:right];assert 'return 29' in body and '_random.nextInt(TOTAL_PROBABILITY) + 1' in body
frozen=D/'frozen';frozen.mkdir(exist_ok=True)
for p in [source,R/'NativePolymorphWeights.java',R/'PolymorphWeightsProbe.java',Path(__file__)]:
 q=frozen/(sha(p)+'-'+p.name)
 if not q.exists():shutil.copyfile(p,q)
reference=D/'SourceWandPolyReference.java';reference.write_text('package local.lito381.server;import java.util.*;final class SourceWandPolyReference{static final int TOTAL_PROBABILITY=1000000;record PolyDrop(int polyId,int chance){}final List<PolyDrop> _polys;final Random _random;SourceWandPolyReference(List<NativePolymorphWeights.Row> rows,int roll){_polys=rows.stream().map(x->new PolyDrop(x.polyId(),x.chance())).toList();_random=new Random(){public int nextInt(int bound){if(bound!=1000000)throw new AssertionError();return roll-1;}};}'+body+'static int select(List<NativePolymorphWeights.Row> rows,int roll){return new SourceWandPolyReference(rows,roll).getRandomPolyId();}}',encoding='utf-8')
v=dict(status='running',scope='pure selection only; shared SQL loading and native effects not yet qualified',source=pin(source),exact_method_sha256=hashlib.sha256(body.encode()).hexdigest(),reference=pin(reference),inputs=[pin(p)for p in frozen.iterdir()],formal_deployment_changed=False,full_goal_achieved=False)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
try:
 with (D/'test.log').open('w',encoding='utf-8')as log:
  for args in [[J/'javac.exe','-g:none','-encoding','UTF-8','-d',C,R/'NativePolymorphWeights.java',R/'PolymorphWeightsProbe.java',reference],[J/'java.exe','-Xmx64m','-Xverify:all','-cp',C,'local.lito381.server.PolymorphWeightsProbe',D/'observations.json']]:subprocess.run(list(map(str,args)),stdout=log,stderr=subprocess.STDOUT,check=True,timeout=60)
 obs=json.loads((D/'observations.json').read_text('utf-8'));assert obs['cases']==5088;v.update(status='passed-pure-source-selection',results=obs,observation=pin(D/'observations.json'))
except BaseException as error:
 v.update(status='failed',error=str(error));print((D/'test.log').read_text('utf-8',errors='replace')[-4000:]);raise
finally:(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v['results']))
