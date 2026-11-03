from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;W=R.parent/'weakness-material';N=R.parent/'negative-material'
t=(W/'WeaknessMaterialMethods.java').read_text('utf-8').replace('Weakness','Silence')
t=t.replace('48,56,42,44,14,12','48,56,42,44,10,10').replace('47,55,41,43,12,10','47,55,41,43,10,10')
(R/'SilenceMaterialMethods.java').write_text(t,encoding='utf-8')
t=(W/'NativeWeaknessMaterialAction.java').read_text('utf-8').replace('Weakness','Silence').replace('skill!=47&&skill!=56','skill!=64')
(R/'NativeSilenceMaterialAction.java').write_text(t,encoding='utf-8')
t=(W/'build.py').read_text('utf-8').replace('negative-material/build/candidate-negative-material.jar','ground-material/build/candidate-ground-material.jar').replace('18fbe46a414e29974bdb4d625b33b6aca9b3271cac0eaf9994a14ed7801f244e','9c5e60af1702d4e99d6a25da0fad1ce4ad66b1eeeb62db6637423b8145d7572d').replace('negative-material/build/inputs/sources/NativeLightAction.java','ground-material/build/inputs/sources/NativeLightAction.java')
t=t.replace("needle='&&skill!=153'","needle='&&skill!=63'").replace("needle+'&&skill!=47&&skill!=56'","needle+'&&skill!=64'")
a=t.index("needle='note.contains(");b=t.index('(src/light.name)',a)
t=t[:a]+'''needle='note.contains("Disease target=")';assert t.count(needle)==1
t=t.replace(needle,'('+needle+'||note.contains("Silence target="))')
'''+t[b:]
t=t.replace('Weakness','Silence').replace('candidate-weakness-material.jar','candidate-silence-material.jar')
(R/'build.py').write_text(t,encoding='utf-8')
t=(N/'NegativeMaterialProbe.java').read_text('utf-8').replace('Negative','Silence')
t=t.replace('SKILLS={29,152,153}','SKILLS={64}').replace('return skill==153?BattleSpellCatalog.eraseMagicSpell(skill):BattleSpellCatalog.entangleSpell(skill);','return BattleSpellCatalog.silenceSpell(skill);')
t=t.replace('if(selectedMode==12)call(pc,"handle",new Class[]{byte[].class},packet);\n  else call(pc,skill==153?"useEraseMagicSpell":"useEntangleSpell",new Class[]{byte[].class,row.getClass(),int.class},packet,row,selectedMode==1?100:1);','call(pc,"handle",new Class[]{byte[].class},packet);')
t=t.replace('return skill==153?((BattleEraseMagicStatus)get(pc,"eraseMagicStatus")).active(System.nanoTime()):((BattleTimedStatus)get(pc,"timedStatus")).speed(System.nanoTime()).type()==BattleTimedStatus.SLOW;','return ((BattleTimedStatus)get(pc,"timedStatus")).silenceActive(System.nanoTime());')
t=t.replace('selectedSkill==29?3:2','3').replace('Set.of(2,29,152,153)','Set.of(2,64)')
t=t.replace('if(mode!=12){boolean applied=accepted&&mode!=1&&(skill==153||mode!=10&&mode!=11);','{boolean applied=accepted&&mode!=1&&mode!=10;')
t=t.replace('Map.of("battleNaturalHpRegenEnabled",false,','Map.of("silenceFixturePercentileRoll",mode==1?100:1,"battleNaturalHpRegenEnabled",false,')
t=t.replace('Actual selected roots with existing deterministic roll arguments; one native packet case per skill/phase/configuration.','Native skill64 packet with existing percentile fixture; original two-phase target, counter and probability rules.')
t=t.replace('var self=collect(wire);var peers=collect(other);','''if(mode==12){
    set(peer,"learnedSkills",new HashSet<>(Set.of(2)));int peerMp=(int)get(peer,"playerMp");int sent=other.raw.size();
    NativeCastMaterialProbe.cast(peer,2);need((int)get(peer,"playerMp")==peerMp&&!((Map<?,?>)get(peer,"spellReadyNanos")).containsKey(2)&&other.raw.size()>sent,"active Silence blocks actual native Light with visible response and no resources/reuse");
    var status=(BattleTimedStatus)get(peer,"timedStatus");long end=(long)get(status,"silenceExpiresAtNanos");need(status.expireSilence(end)&&!active(peer,skill),"ordinary Silence expires at its actual stored deadline");
    NativeCastMaterialProbe.cast(peer,2);need((int)get(peer,"playerMp")==Math.max(0,Math.min(1000,peerMp-cost(peer,2,BattleSpellCatalog.lightSpell(2))))&&((Map<?,?>)get(peer,"spellReadyNanos")).containsKey(2),"actual Light works immediately after expiry; rejected cast did not reserve action");
   }
   var self=collect(wire);var peers=collect(other);
   if(mode==12)need(peers.contains(HexFormat.of().formatHex(Protocol.messageCode(285))),"native Silence rejection message285 reaches the player");''')
(R/'SilenceMaterialProbe.java').write_text(t,encoding='utf-8')
t=(N/'test.py').read_text('utf-8').replace('Negative','Silence').replace("v[mode]['cases']==312","v[mode]['cases']==104").replace("(24 if mode=='before' else 0)","(8 if mode=='before' else 0)")
(R/'test.py').write_text(t,encoding='utf-8')
for name in ['Fault','Lifecycle']:
 t=(W/('Weakness'+name+'Probe.java')).read_text('utf-8').replace('Weakness','Silence').replace('weaknessFixturePercentileRoll','silenceFixturePercentileRoll').replace('faults==16','faults==8')
 if name=='Lifecycle':
  t=t.replace('need(active(peer,selectedSkill)&&active(pc,selectedSkill),"both real targeted effects apply");','boolean aApplied=active(peer,selectedSkill),bApplied=active(pc,selectedSkill);need(aApplied||bApplied,"at least one concurrent Silence is admitted; later caster may already be silenced");')
  t=t.replace('Math.max(0,Math.min(1000-mpCost,1000))','Math.max(0,Math.min(1000-(aApplied?mpCost:0),1000))').replace('Math.max(0,Math.min(1000-peerCost,1000))','Math.max(0,Math.min(1000-(bApplied?peerCost:0),1000))')
  t=t.replace('pc.nativeDeInventory(0).countCatalog(item)==3&&peer.nativeDeInventory(0).countCatalog(item)==3','pc.nativeDeInventory(0).countCatalog(item)==count+3-(aApplied?count:0)&&peer.nativeDeInventory(0).countCatalog(item)==count+3-(bApplied?count:0)')
 (R/('Silence'+name+'Probe.java')).write_text(t,encoding='utf-8')
t=(W/'test_boundaries.py').read_text('utf-8').replace('Weakness','Silence').replace("('Fault','before',old,16),('Fault','after',jar,16),('Lifecycle','after',jar,112)","('Fault','before',old,8),('Fault','after',jar,8),('Lifecycle','after',jar,56)").replace("row['actual_memory_faults']==16","row['actual_memory_faults']==8").replace("(16 if mode=='before' else 0)","(8 if mode=='before' else 0)")
(R/'test_boundaries.py').write_text(t,encoding='utf-8')
t=(N/'NegativeWorldNpcProbe.java').read_text('utf-8').replace('Negative','Silence')
t=t.replace('return skill==153?hub.classicWorldMonsterEffectiveMagicResistance(map,instance,object,100,System.nanoTime())==25:hub.classicWorldMonsterSlowActive(map,instance,object,System.nanoTime());','return hub.classicWorldMonsterSilenceActive(map,instance,object,System.nanoTime());')
t=t.replace('if(mode!=7)need(active(hub,pc,object,skill)==(accepted&&mode!=1),','need(active(hub,pc,object,skill)==(accepted&&mode!=1),')
t=t.replace('Map.of("battleNaturalHpRegenEnabled",false,','Map.of("silenceFixturePercentileRoll",mode==1?100:1,"battleNaturalHpRegenEnabled",false,')
(R/'SilenceWorldNpcProbe.java').write_text(t,encoding='utf-8')
t=(N/'test_world_npcs.py').read_text('utf-8').replace('Negative','Silence').replace("v[mode]['cases']==96","v[mode]['cases']==32").replace("(12 if mode=='before' else 0)","(4 if mode=='before' else 0)")
(R/'test_world_npcs.py').write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[W/'WeaknessMaterialMethods.java',W/'NativeWeaknessMaterialAction.java',W/'build.py',N/'NegativeMaterialProbe.java',N/'test.py',W/'WeaknessFaultProbe.java',W/'WeaknessLifecycleProbe.java',W/'test_boundaries.py',Path(__file__)]],scope='single-target silence64 only; preserve two original target/gate/effect bodies; area silence161 not included'),indent=2),encoding='utf-8')
