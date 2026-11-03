from pathlib import Path
R=Path(__file__).resolve().parent;E=R.parent/'elemental-material'
for name in ['Fault','Stopped','Edges']:
    t=(E/('Elemental'+name+'Probe.java')).read_text('utf-8')
    t=t.replace('Elemental','Summon').replace('"type",2','"type",3')
    t=t.replace('set(pc,"elfAttribute",0);','set(pc,"positionX",100);set(pc,"positionY",120);')
    t=t.replace('faults==16','faults==8').replace('\\"self_skills\\":2','\\"self_skills\\":1')
    t=t.replace('zero attribute still suppresses summon after accepted material','outside-map landing prevents actor creation after accepted material')
    t=t.replace('zero attribute never creates a summon during lifecycle edges','outside-map landing prevents actor creation during material lifecycle edges')
    if name=='Fault':
        needle='need(((List<?>)get(active,"tamedCompanions")).isEmpty(),"no summoned effect before material save");'
        assert needle in t
        t=t.replace(needle,needle+'need((int)get(active,"playerHp")==4000-(before?value(spell(skill),"hpCost"):0),"HP unchanged until accepted material save");')
        needle='need(((List<?>)get(pc,"tamedCompanions")).isEmpty(),"failed material cannot create summon");'
        t=t.replace(needle,needle+'need((int)get(pc,"playerHp")==4000-(before?value(spell(skill),"hpCost"):0),"material failure cannot debit HP");')
    (R/('Summon'+name+'Probe.java')).write_text(t,encoding='utf-8')
t=(E/'test_boundaries.py').read_text('utf-8').replace('Elemental','Summon')
t=t.replace("[classes,B/'pc-native", "[classes,R.parent/'elemental-material/build/lifecycle/classes',R.parent/'elemental-material/build/boundaries/classes',B/'pc-native")
t=t.replace("('Fault','before',old,16),('Fault','after',jar,16),('Stopped','after',jar,16),('Edges','after',jar,64)","('Fault','before',old,8),('Fault','after',jar,8),('Stopped','after',jar,8),('Edges','after',jar,32)")
t=t.replace("row['actual_memory_faults']==16", "row['actual_memory_faults']==8").replace("(16 if mode=='before' else 0)","(8 if mode=='before' else 0)")
t=t.replace('import hashlib,json,os,subprocess,zipfile,shutil','import hashlib,json,os,subprocess,zipfile,shutil,sys')
t=t.replace("  asm=Path.home()", "  if '--compile-only' in sys.argv:\n   result['status']='compiled-only-not-qualified';print(result['status']);sys.exit(0)\n  asm=Path.home()")
t=t.replace('except BaseException as error:', 'except Exception as error:')
(R/'test_boundaries.py').write_text(t,encoding='utf-8')
print('Prepared skill51 atomic HP/MP/material, STOPPED, retirement and socket-failure fixtures; NPC branches qualified separately')
