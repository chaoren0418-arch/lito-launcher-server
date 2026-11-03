from pathlib import Path
R=Path(__file__).resolve().parent;parent=R.parent
t=(parent/'NativeMagicalWeaponFaultProbe.java').read_text('utf-8').replace('NativeMagicalWeaponFaultProbe','ElementalFaultProbe').replace('extends NativeMagicalWeaponProbe','extends ElementalMaterialProbe')
t=t.replace('static int expectedCost,material,amount;','static int expectedCost,material,amount,skill;').replace('73','skill')
t=t.replace('boolean world)throws Exception{','boolean world,int selectedSkill)throws Exception{\n  skill=selectedSkill;')
t=t.replace('object=weapon(pc,true);','set(pc,"elfAttribute",0);')
t=t.replace('reset(pc,peer,hub,world);','reset(pc,peer,hub,world);var characters=(List<CharacterSummary>)get(pc,"characters");set(pc,"characters",List.of(copy(characters.get(0),Map.of("type",2,"originalIntelligence",10))));')
t=t.replace('need(active.nativeDeInventory(0).item(object).enchantLevel()==(before?1:0),"candidate original timed effect not yet activated at fault");','need(((List<?>)get(active,"tamedCompanions")).isEmpty(),"no summoned effect before material save");')
t=t.replace('need(pc.nativeDeInventory(0).item(object).enchantLevel()==(before?1:0),"failed material cannot install timed effect");','need(((List<?>)get(pc,"tamedCompanions")).isEmpty(),"failed material cannot create summon");')
t=t.replace('NativeMagicalWeaponProbe.cast(pc,skill,object)','ElementalMaterialProbe.cast(pc,skill,spell(skill))').replace('&&pc.nativeDeInventory(0).item(object).enchantLevel()==1','')
t=t.replace('need(self.isEmpty(),"original item-weapon save precedes its entire output tail")','need(!self.isEmpty(),"original elemental inventory delta precedes failing memory save")')
t=t.replace('for(boolean app:new boolean[]{false,true}){','for(boolean app:new boolean[]{false,true})for(int selectedSkill:SKILLS){').replace('flag+"-"+world+"-"+app);','flag+"-"+world+"-"+app+"-"+selectedSkill);').replace('exercise(p.session,m.session,hub,state,world);','exercise(p.session,m.session,hub,state,world,selectedSkill);').replace('faults==8','faults==16')
assert 'weapon(pc' not in t and 'enchantLevel' not in t
(R/'ElementalFaultProbe.java').write_text(t,encoding='utf-8')
t=(parent/'NativeMagicalWeaponStoppedProbe.java').read_text('utf-8').replace('NativeMagicalWeaponStoppedProbe','ElementalStoppedProbe').replace('extends NativeMagicalWeaponProbe','extends ElementalMaterialProbe')
t=t.replace('skill==107?4:1','2').replace('long object=weapon(pc,true);','').replace('set(pc,"elfAttribute",Math.max(1,value(row,"attribute")));','set(pc,"elfAttribute",0);').replace('cast(pc,skill,object)','cast(pc,skill,row)')
t=t.replace('need(pc.nativeDeInventory(0).item(object).enchantLevel()==1,"accepted weapon effect survives STOPPED with its material");','need(((List<?>)get(pc,"tamedCompanions")).isEmpty(),"zero attribute still suppresses summon after accepted material");').replace('\\"self_skills\\":1','\\"self_skills\\":2')
assert 'weapon(pc' not in t and 'enchantLevel' not in t
(R/'ElementalStoppedProbe.java').write_text(t,encoding='utf-8')
t=(parent/'NativeMagicalWeaponEdgesProbe.java').read_text('utf-8').replace('NativeMagicalWeaponEdgesProbe','ElementalEdgesProbe').replace('extends NativeMagicalWeaponProbe','extends ElementalMaterialProbe')
t=t.replace('skill==107?4:1','2').replace('long object=weapon(pc,true);','').replace('set(pc,"elfAttribute",Math.max(1,value(row,"attribute")));','set(pc,"elfAttribute",0);').replace('cast(pc,skill,object)','cast(pc,skill,row)')
t=t.replace('need(pc.nativeDeInventory(0).item(object).enchantLevel()==(mode==1||mode==2?0:1),"weapon effect follows the exact same commit as material");','need(((List<?>)get(pc,"tamedCompanions")).isEmpty(),"zero attribute never creates a summon during lifecycle edges");').replace('\\"self_skills\\":1','\\"self_skills\\":2')
assert 'weapon(pc' not in t and 'enchantLevel' not in t
(R/'ElementalEdgesProbe.java').write_text(t,encoding='utf-8');print('Prepared 16 memory-failure, 16 STOPPED and 64 material lifecycle cases; actual NPC lifetime races remain separate')
