"""Prepare one bounded material adapter, preserving the candidate's target/effect body."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;W=R/'water-life-material';P=R/'protection-material';P.mkdir(exist_ok=True)
t=(W/'WaterLifeMethods.java').read_text('utf-8')
for a,b in [('WaterLifeMethods','ProtectionMethods'),('NativeWaterLifeCastAction','NativeProtectionCastAction'),('useWaterLifeSpell','useDamageProtectionSpell'),('nativeMaterialWaterLifeBody','nativeMaterialProtectionBody'),('WaterLifeSpell','DamageProtectionSpell'),('Water Life one-handler','Damage protection one-handler')]:t=t.replace(a,b)
start=t.index('boolean consumptionSnapshot=');end=t.index('else throw new AssertionError("unknown monitor local "+v.var);',start)
t=t[:start]+'if(v.var==29){m.instructions.set(n,new InsnNode(POP));pcMonitors++;}else if(v.var==30){m.instructions.set(n,new InsnNode(POP));companionMonitors++;}'+t[end:]
for a,b in [('pcMonitors!=5||companionMonitors!=5','pcMonitors!=4||companionMonitors!=4'),('ALOAD,14','ALOAD,12'),('LSTORE,24','LSTORE,27'),('savedSlot.var==21','savedSlot.var==24'),('ASTORE,21','ASTORE,24')]:t=t.replace(a,b)
(P/'ProtectionMethods.java').write_text(t,encoding='utf-8')
t=(W/'NativeWaterLifeCastAction.java').read_text('utf-8')
for a,b in [('NativeWaterLifeCastAction','NativeProtectionCastAction'),('WaterLifeSpell','DamageProtectionSpell'),('nativeMaterialWaterLifeBody','nativeMaterialProtectionBody'),('spell.skillId()!=170','spell.skillId()!=68'),('hub.visibleBattleCombatants(((Number)CombatAccess.field(pc,"currentRoomId")).longValue(),pc)','hub.roomCombatants(((Number)CombatAccess.field(pc,"currentRoomId")).longValue())'),('orElse(request.targetObjectId()==pc.playerObjectId()?pc:null)','orElse(null)')]:t=t.replace(a,b)
(P/'NativeProtectionCastAction.java').write_text(t,encoding='utf-8')
t=(W/'build.py').read_text('utf-8')
for a,b in [('summon-ring/build/candidate-summon-ring.jar','water-life-material/build/candidate-water-life-material.jar'),('68f0f3381731e076d8d726938cf869d9e59f2c6f98347380611188f583aaeaa3','c83d82a84676a984a3d5e63e41a52f4e564359b3ba93c0ff6923e2748276fed7'),('summon-material/build/inputs/sources/NativeLightAction.java','water-life-material/build/inputs/sources/NativeLightAction.java'),('WaterLifeMethods','ProtectionMethods'),('NativeWaterLifeCastAction','NativeProtectionCastAction'),('candidate=D/''candidate-water-life-material.jar''','candidate=D/''candidate-protection-material.jar'''),("['ClientSession','NativeLightAction','NativeLightAction$Batch']","['ClientSession','NativeLightAction']")]:t=t.replace(a,b)
start=t.index("t=light.read_text('utf-8');");end=t.index("shutil.copyfile(R/'NativeProtectionCastAction.java'",start)
t=t[:start]+'''t=light.read_text('utf-8');needle='&&skill!=170';assert t.count(needle)==1;t=t.replace(needle,needle+'&&skill!=68')
needle='note.startsWith("S_EFFECT Water-Life target=")';assert t.count(needle)==1
t=t.replace(needle,'('+needle+'||note.startsWith("S_EFFECT immune-to-harm target="))')
(src/light.name).write_text(t,encoding='utf-8')
'''+t[end:]
t=t.replace("candidate=D/'candidate-water-life-material.jar'", "candidate=D/'candidate-protection-material.jar'")
(P/'build.py').write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(P/'adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p)) for p in [W/'WaterLifeMethods.java',W/'build.py',W/'NativeWaterLifeCastAction.java',Path(__file__)]],scope='skill68 original target/effect retained; common pair and material commit; not qualified'),indent=2),encoding='utf-8')
