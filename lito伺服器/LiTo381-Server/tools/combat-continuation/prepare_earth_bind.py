"""Derive narrowly scoped Earth Bind material candidate from verified Blind machinery.

The exact two native roots and local slots are audited against the pinned parent;
no Source repository, SQL or formal runtime is modified.
"""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent
src=R/'blind-material';dest=R/'earth-bind-material'
dest.mkdir(exist_ok=True)
def adapt(name,text):
    p=dest/name
    assert not p.exists(),p
    p.write_text(text,encoding='utf-8')
def replace(text,old,new):
    assert text.count(old)==1,(old,text.count(old))
    return text.replace(old,new)
t=(src/'BlindMaterialMethods.java').read_text('utf-8')
t=t.replace('BlindMaterial','EarthBindMaterial').replace('NativeBlind','NativeEarthBind')
t=replace(t,'new Spec("useBlindSpell","nativeMaterialBlindBody","battle","BlindSpell",42,45,38,40,7,6),new Spec("useClassicWorldBlindSpell","nativeMaterialWorldBlindBody","world","BlindSpell",46,51,42,44,10,5)',
    'new Spec("useBindSpell","nativeMaterialEarthBindBody","battle","BindSpell",38,41,34,36,5,5),new Spec("useClassicWorldBindSpell","nativeMaterialWorldEarthBindBody","world","BindSpell",49,55,45,47,9,5)')
adapt('EarthBindMaterialMethods.java',t)
t=(src/'NativeBlindMaterialAction.java').read_text('utf-8').replace('NativeBlindMaterialAction','NativeEarthBindMaterialAction').replace('BattleSpellCatalog.BlindSpell','BattleSpellCatalog.BindSpell').replace('nativeMaterialBlindBody','nativeMaterialEarthBindBody').replace('nativeMaterialWorldBlindBody','nativeMaterialWorldEarthBindBody').replace('skill!=40&&skill!=103','skill!=157').replace('unreviewed Blind','unreviewed Earth Bind')
adapt('NativeEarthBindMaterialAction.java',t)
t=(src/'build.py').read_text('utf-8').replace('BlindMaterial','EarthBindMaterial').replace('NativeBlind','NativeEarthBind').replace('candidate-blind-material.jar','candidate-earth-bind-material.jar')
t=replace(t,"target-negative-material/build/candidate-target-negative-material.jar","poison-publication/build/candidate-poison-publication.jar")
t=replace(t,'c3c4c84f7efcf8ab9c8513f10b58457f5c7056c436a13872f50e049dfeedf882','a53513d544124cbd1aac543f34302a15067f2a98422ed14576bcaaa6c991d4a3')
t=replace(t,"target-negative-material/build/inputs/sources/NativeLightAction.java","poison-publication/build/inputs/sources/NativeLightAction.java")
t=replace(t,"needle+'&&skill!=40&&skill!=103'","needle+'&&skill!=157'")
t=replace(t,"t=t.replace(needle,'('+needle+'||note.contains(\"Blind target=\")||note.contains(\"blind target=\"))')", "t=t.replace(needle,'('+needle+'||note.contains(\"Earth Bind target=\"))')")
adapt('build.py',t)
proof={'status':'prepared-not-qualified','basis':[], 'audited_parent':'a53513d544124cbd1aac543f34302a15067f2a98422ed14576bcaaa6c991d4a3','scope':'skill157 only; original target/probability/effect branches retained; build requires exact inverse and C0 equivalence','formal_deployment_changed':False}
for p in [src/'BlindMaterialMethods.java',src/'NativeBlindMaterialAction.java',src/'build.py',R/'build/earth-bind-audit/inputs.json',Path(__file__)]:
    proof['basis'].append({'path':str(p),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
(dest/'adaptation.json').write_text(json.dumps(proof,ensure_ascii=False,indent=2),encoding='utf-8')
print('Prepared Earth Bind candidate sources; behavioral qualification still required.')
