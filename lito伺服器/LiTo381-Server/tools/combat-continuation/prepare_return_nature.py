"""Prepare a distinct skill145 candidate; preserve its probability proposal and release branches."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;P=R/'return-nature-material';E=R/'earth-bind-material';P.mkdir(exist_ok=True)
def rep(t,a,b):
 assert t.count(a)==1,(a,t.count(a));return t.replace(a,b)
def put(n,t):
 p=P/n;assert not p.exists(),p;p.write_text(t,encoding='utf-8')
t=(E/'EarthBindMaterialMethods.java').read_text('utf-8').replace('EarthBindMaterial','ReturnNatureMaterial')
t=rep(t,'new Spec("useBindSpell","nativeMaterialEarthBindBody","battle","BindSpell",38,41,34,36,5,5),new Spec("useClassicWorldBindSpell","nativeMaterialWorldEarthBindBody","world","BindSpell",49,55,45,47,9,5)','new Spec("useReturnToNatureSpell","nativeMaterialReturnNatureBody","cast","ReturnToNatureSpell",30,33,26,28,9,5)')
t=rep(t,'var inventory=prev(delta);','var inventory=prev(delta);if(!(inventory instanceof MethodInsnNode commit&&commit.owner.equals(P+"CombatReturnNature")&&commit.name.equals("commit")))throw new AssertionError("missing retained probability commit");for(int i=0;i<3;i++)inventory=prev(inventory);')
t=rep(t,'for(int i=0;i<3;i++)wrapper.instructions.add(new VarInsnNode(ALOAD,i));','for(int i=0;i<3;i++)wrapper.instructions.add(new VarInsnNode(ALOAD,i));wrapper.instructions.add(new VarInsnNode(ILOAD,3));')
t=t.replace('Two exact original roots','One exact original root').replace('two negative roots','one Return to Nature root')
put('ReturnNatureMaterialMethods.java',t)
t=(E/'build.py').read_text('utf-8').replace('EarthBindMaterial','ReturnNatureMaterial').replace('candidate-earth-bind-material.jar','candidate-return-nature-material.jar')
t=rep(t,'poison-publication/build/candidate-poison-publication.jar','polymorph-material/build/candidate-polymorph-material.jar')
t=rep(t,'a53513d544124cbd1aac543f34302a15067f2a98422ed14576bcaaa6c991d4a3','a1b7d3390f04262f240364fbd6ef0ad7f6009ff27cc80bab170985ba151948e4')
t=t.replace('poison-publication/build/inputs/sources/NativeLightAction.java','polymorph-material/build/inputs/sources/NativeLightAction.java')
t=rep(t,"needle+'&&skill!=157'","needle+'&&skill!=145'")
t=rep(t,"needle='note.contains(\"Striker Gale target=\")';assert t.count(needle)==1\nt=t.replace(needle,'('+needle+'||note.contains(\"Earth Bind target=\"))')",'''needle='NativeResurrectionMaterialAction.dispatch(pc,packet,()->dispatchResurrectionOriginal(pc,packet));';assert t.count(needle)==1
t=t.replace(needle,'NativeReturnNatureMaterialAction.dispatch(pc,packet,()->NativeResurrectionMaterialAction.dispatch(pc,packet,()->dispatchResurrectionOriginal(pc,packet)));')
needle='byte[] snapshot=plain.clone();batch.outputs.add(()->{if(batch.target!=null';assert t.count(needle)==1
t=t.replace(needle,'if(NativeReturnNatureMaterialAction.captureOutput(receiver,plain,note))return true;'+needle)''')
t=rep(t,"assert fresh==['local/lito381/server/NativeReturnNatureMaterialAction.class'],fresh","assert set(fresh)=={'local/lito381/server/NativeReturnNatureMaterialAction.class','local/lito381/server/NativeReturnNatureMaterialAction$Target.class'},fresh")
put('build.py',t)
files=[Path(__file__),E/'EarthBindMaterialMethods.java',E/'build.py',R/'build/remaining-material-consumers/useReturnToNatureSpell.txt',R/'resurrection-ingress/NativeResurrectionMaterialAction.java']
(P/'adaptation.json').write_text(json.dumps(dict(status='prepared-not-qualified',skill=145,parent_composed_qualification_pending=True,scope='Material/role/companion binding only; probability, invalid pet79 and SUMMON/TAMED release behavior retained',inputs=[dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())for p in files],formal_deployment_changed=False),ensure_ascii=False,indent=2),encoding='utf-8')
