"""Serial test queue; only a sealed, exact parent replay may release the next JVM."""
from pathlib import Path
import hashlib,json,subprocess,sys,time
R=Path(__file__).resolve().parent;C=R.parent;S=C.parents[1];D=R/'build';current=C/'pet-world-lifetime/build/composed-replay/current.json'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
v=dict(status='waiting-for-parent',runtime=read(D/'build.json')['runtime'],formal_deployment_changed=False,full_goal_achieved=False)
try:
 deadline=time.monotonic()+3600
 while not current.exists():
  if time.monotonic()>=deadline:raise TimeoutError('parent replay still has no sealed receipt')
  time.sleep(1)
 parent=read(current);assert parent['runtime']['sha256']=='18738d9d5728bfeef5f2b4cd2466f8fc87b2104352000e6b196cf17b9d216de3';assert sha(Path(parent['receipt']['path']))==parent['receipt']['sha256'];assert parent['status']=='passed',parent['status'];v['parent_composed']=pin(current)
 print('Parent replay sealed; begin serial WORLD publication tests',flush=True)
 for relative,args in [('pet-world-publication/test.py',[]),('pet-world-publication/replay_lifetime.py',[]),('pet-world-publication/replay_material.py',[]),('return-nature-transfer/replay_pet_controls.py',['pet-world-publication'])]:
  assert sha(Path(v['runtime']['path']))==v['runtime']['sha256'];print('RUN',relative,flush=True);subprocess.run([sys.executable,'-B','-X','utf8',str(C/relative),*args],cwd=S,check=True,timeout=1200)
 v['status']='passed-targeted-publication-and-inherited-pet-replays-only'
except BaseException as error:v.update(status='failed',error=str(error));raise
finally:(D/'serial-qualification.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
