"""Prepare skill67 from the audited native root, without touching deployment."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;E=R/'earth-bind-material';P=R/'polymorph-material';P.mkdir(exist_ok=True)
def write(name,text):
 p=P/name;assert not p.exists(),p;p.write_text(text,encoding='utf-8')
def replace(text,old,new):
 assert text.count(old)==1,(old,text.count(old));return text.replace(old,new)
t=(E/'EarthBindMaterialMethods.java').read_text('utf-8').replace('EarthBindMaterial','PolymorphMaterial')
t=replace(t,'new Spec("useBindSpell","nativeMaterialEarthBindBody","battle","BindSpell",38,41,34,36,5,5),new Spec("useClassicWorldBindSpell","nativeMaterialWorldEarthBindBody","world","BindSpell",49,55,45,45' if False else 'new Spec("useBindSpell","nativeMaterialEarthBindBody","battle","BindSpell",38,41,34,36,5,5),new Spec("useClassicWorldBindSpell","nativeMaterialWorldEarthBindBody","world","BindSpell",49,55,45,47,9,5)','new Spec("usePolymorphSpell","nativeMaterialPolymorphBody","cast","PolymorphSpell",32,38,22,28,12,10)')
t=t.replace('Two exact original roots','One exact original root').replace('two negative roots','one polymorph root')
write('PolymorphMaterialMethods.java',t)
t=(E/'build.py').read_text('utf-8').replace('EarthBindMaterial','PolymorphMaterial').replace('candidate-earth-bind-material.jar','candidate-polymorph-material.jar')
t=replace(t,"poison-publication/build/candidate-poison-publication.jar","pet-controls/build/candidate-pet-controls.jar")
t=replace(t,'a53513d544124cbd1aac543f34302a15067f2a98422ed14576bcaaa6c991d4a3','4319a8e7f5c9dff541c263b9d85fbfafc03fe963dd3007677b54b181f0c9253e')
t=t.replace('poison-publication/build/inputs/sources/NativeLightAction.java','earth-bind-material/build/inputs/sources/NativeLightAction.java')
t=replace(t,"needle+'&&skill!=157'","needle+'&&skill!=67'")
t=replace(t,'Earth Bind target=','shape-change target=')
write('build.py',t)
write('NativePolymorphMaterialAction.java','''package local.lito381.server;
import java.io.IOException;
import java.util.List;
/** Skill67 preserves native control-ring/random-form branches inside ordered locks. */
final class NativePolymorphMaterialAction {
 static void cast(ClientSession pc,byte[] packet,BattleSpellCatalog.PolymorphSpell spell)throws IOException {
  var request=Protocol.objectSpellRequest(packet);var hub=(GameHub)CombatAccess.field(pc,"hub");
  boolean world=String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD");
  List<ClientSession> players=world?(List<ClientSession>)CombatAccess.call(pc,"classicWorldMembers","(I)Ljava/util/List;",(int)CombatAccess.field(pc,"classicWorldMapId")):hub.roomCombatants((long)CombatAccess.field(pc,"currentRoomId"));
  var target=players.stream().filter(p->p.playerObjectId()==request.targetObjectId()).findFirst().orElse(pc);
  NativeLightAction.commitPair(pc,target,()->pc.nativeMaterialPolymorphBody(packet,spell));
 }
 static boolean admit(ClientSession pc,ClientSession target,int skill,int action,int reuse){
  if(skill!=67)throw new IllegalStateException("unreviewed polymorph skill");
  return NativeLightAction.admitPair(pc,target==null?pc:target,skill,action,reuse);
 }
 private NativePolymorphMaterialAction(){}
}
''')
files=[Path(__file__),E/'EarthBindMaterialMethods.java',E/'build.py',R/'build/remaining-material-consumers/usePolymorphSpell.txt',Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java')]
(P/'adaptation.json').write_text(json.dumps(dict(status='prepared-not-qualified',skill=67,scope='Source target control-ring/random branches retained; material ownership and ordered PC lifecycle only',inputs=[dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())for p in files],formal_deployment_changed=False),ensure_ascii=False,indent=2),encoding='utf-8')
