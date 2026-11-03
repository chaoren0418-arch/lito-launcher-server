from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;E=R.parent/'elemental-material'
source=E/'ElementalMethods.java';text=source.read_text('utf-8')
for a,b in [('ElementalMethods','WaterLifeMethods'),('NativeElementalCastAction','NativeWaterLifeCastAction'),('useElementalSummonSpell','useWaterLifeSpell'),('nativeMaterialElementalSummonBody','nativeMaterialWaterLifeBody'),('ElementalSummonSpell','WaterLifeSpell'),('var==13','var==26'),('v.var==13','v.var==26'),('v.var==25','v.var==27'),('pcMonitors!=5||companionMonitors!=3','pcMonitors!=5||companionMonitors!=5'),('LSTORE,11','LSTORE,24'),('savedSlot.var==9','savedSlot.var==21'),('ASTORE,9','ASTORE,21')]:text=text.replace(a,b)
# Both PC monitor locals are replaced; no population monitor is part of this targeted player cast.
text=text.replace('else if(v.var==27)companionMonitors++;','else if(v.var==27){m.instructions.set(n,new InsnNode(POP));companionMonitors++;}')
old='gate.add(new VarInsnNode(ALOAD,0));gate.add(new VarInsnNode(ALOAD,2));'
assert text.count(old)==1;text=text.replace(old,'gate.add(new VarInsnNode(ALOAD,0));gate.add(new VarInsnNode(ALOAD,14));gate.add(new VarInsnNode(ALOAD,2));')
old='"(L"+PC+";L"+P+"BattleSpellCatalog$WaterLifeSpell;)Z"'
assert text.count(old)==1;text=text.replace(old,'"(L"+PC+";L"+PC+";L"+P+"BattleSpellCatalog$WaterLifeSpell;)Z"')
text=text.replace('Exact one-handler surgery; companion population monitor and the original summon tail remain.','Exact one-handler surgery; pair-ordered Source locks replace both original PC monitors.')
text=text.replace('PASS one-handler inverse; companion monitor and original NPC result path preserved','PASS Water Life one-handler inverse; original effect and target predicates preserved')
(R/'WaterLifeMethods.java').write_text(text,encoding='utf-8')
build=(E/'build.py').read_text('utf-8')
build=build.replace("['ClientSession','NativeLightAction']", "['ClientSession','NativeLightAction','NativeLightAction$Batch']")
for a,b in [('invisibility-leases/build/candidate-invisibility-leases.jar','summon-ring/build/candidate-summon-ring.jar'),('d4bc643fca4ab18ea4b6b84e9f685bbc80bd26d806b98970a6a0d98d28ae3a81','68f0f3381731e076d8d726938cf869d9e59f2c6f98347380611188f583aaeaa3'),("R.parent/'build/inputs/sources/NativeLightAction.java'","R.parent/'summon-material/build/inputs/sources/NativeLightAction.java'"),('ElementalMethods','WaterLifeMethods'),('NativeElementalCastAction','NativeWaterLifeCastAction'),('candidate-elemental-material.jar','candidate-water-life-material.jar')]:build=build.replace(a,b)
start=build.index("t=light.read_text('utf-8');");end=build.index('shutil.copyfile',start)
build=build[:start]+'''t=light.read_text('utf-8');needle='&&skill!=73';assert t.count(needle)==1;t=t.replace(needle,needle+'&&skill!=170')
t=t.replace('boolean captureSelf;NativeCastMaterial.Cast material;', 'boolean captureSelf;ClientSession target;NativePlayerCommit.Binding targetBinding;NativeCastMaterial.Cast material;')
needle='byte[] snapshot=plain.clone();batch.outputs.add(()->receiver.nativeSelfOriginalSend(snapshot,note));return true;'
assert t.count(needle)==1
t=t.replace(needle, 'byte[] snapshot=plain.clone();batch.outputs.add(()->{if(batch.target!=null&&(receiver==batch.target||note.startsWith("S_EFFECT Water-Life target="))&&!batch.targetBinding.matches(batch.target))return;receiver.nativeSelfOriginalSend(snapshot,note);});return true;')
position=t.rfind('}');t=t[:position]+(R/'PairMethods.txt').read_text('utf-8')+'\\n'+t[position:];(src/light.name).write_text(t,encoding='utf-8')
shutil.copyfile(R/'PairMethods.txt',inputs/'PairMethods.txt')
''' +build[end:]
start=build.index("for name in ['LESSER_ELEMENTAL','GREATER_ELEMENTAL']:");end=build.index('asm=',start)
build=build[:start]+'''p=Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java');shutil.copyfile(p,refs/p.name)
'''+build[end:]
(R/'build.py').write_text(build,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'adaptation.json').write_text(json.dumps(dict(source=dict(path=str(source),sha256=sha(source)),adapted=dict(path=str(R/'WaterLifeMethods.java'),sha256=sha(R/'WaterLifeMethods.java')),scope='skill170 pair target material commit; exact candidate base and inverse'),indent=2),encoding='utf-8')
