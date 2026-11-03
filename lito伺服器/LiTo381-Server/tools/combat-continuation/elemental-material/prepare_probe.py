from pathlib import Path
R=Path(__file__).resolve().parent;B=R.parents[2]/'tools/resource-database/build'
t=(B/'NativeSelfMaterialCastProbe.java').read_text('utf-8').replace('NativeSelfMaterialCastProbe','ElementalMaterialProbe')
t=t.replace('static final int[] SKILLS={31,60,78,97,98,102,105,129,134,137,138,147,169,171,175,176};','static final int[] SKILLS={154,162};')
a=t.index(' static Object spell(');b=t.index('\n static int value(',a);t=t[:a]+' static Object spell(int skill){return BattleSpellCatalog.elementalSummonSpell(skill);}\n'+t[b:]
t=t.replace('set(pc,"elfAttribute",Math.max(1,value(row,"attribute")));','set(pc,"elfAttribute",mode==4?1:0);if(mode==3)set(pc,"playerMp",0);if(mode==4&&!world){var arena=ArenaMapCatalog.forMap(hub.battleMapId((long)get(pc,"currentRoomId"),pc));var point=arena.spawns().get(0);set(pc,"positionX",point.x());set(pc,"positionY",point.y());set(peer,"positionX",point.x()+2);set(peer,"positionY",point.y());}')
t=t.replace('boolean accepted=mode==0||mode==2&&item==0||mode==1&&before&&skill!=31;','boolean accepted=mode==0||mode==4||mode==1&&before;')
t=t.replace('mode==1){need((wire.raw.size()>bytes)==(before&&skill!=31)','mode==1){need((wire.raw.size()>bytes)==before').replace('if(before&&skill!=31)gaps++','if(before)gaps++')
t=t.replace('for(int mode=0;mode<3;mode++)','for(int mode=0;mode<5;mode++)')
needle='   var self=collect(wire);'
assert t.count(needle)==1
t=t.replace(needle,'''   var pets=(List<BattleCompanion>)get(pc,"tamedCompanions");if(mode!=4)need(pets.isEmpty(),"zero attribute or rejected cast creates no companion");else{boolean allowed=world?ClassicWorldMapCatalog.forMap((int)get(pc,"classicWorldMapId")).allowsPetRecall():ArenaMapCatalog.forMap(hub.battleMapId((long)get(pc,"currentRoomId"),pc)).allowsPetRecall();need(pets.size()==(allowed?1:0),"original source map gate and summon count");if(allowed){var summoned=pets.get(0).snapshot();need(summoned.npcId()==((BattleSpellCatalog.ElementalSummonSpell)row).summonNpcId(1)&&summoned.active(),"source earth elemental NPC identity");need(summoned.moveSpeed()==1&&summoned.expiresAtNanos()>System.nanoTime(),"original haste and lifetime preserved");}}
'''+needle)
t=t.replace('\\"self_skills\\":16','\\"self_skills\\":2').replace('\\"material_skills\\":14','\\"material_skills\\":2')
(R/'ElementalMaterialProbe.java').write_text(t,encoding='utf-8');print('Prepared 80-case exact dispatcher comparison including no-attribute, shared-window, missing-material, no-MP and actual summons')
