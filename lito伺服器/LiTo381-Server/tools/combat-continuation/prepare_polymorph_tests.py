"""Derive lifecycle/fault fixtures with explicit skill67 differences."""
from pathlib import Path
R=Path(__file__).resolve().parent;E=R/'earth-bind-material';P=R/'polymorph-material'
def put(name,t):
 p=P/name;assert not p.exists(),p;p.write_text(t,encoding='utf-8')
def rep(t,a,b):
 assert t.count(a)==1,(a,t.count(a));return t.replace(a,b)
t=(E/'EarthBindFaultProbe.java').read_text('utf-8').replace('EarthBind','Polymorph')
t=rep(t,'setup(pc,peer,hub,world);recipient=peer;','setup(pc,peer,hub,world);ring(peer,true);recipient=peer;')
t=rep(t,'need(!active(recipient,skill),"original target effect follows successful material save")','need(active(recipient,skill)==before,"old pending shape precedes failing save; new shape requires accepted material")')
t=rep(t,'need(!active(peer,skill),"failed material cannot activate target status")','need(active(peer,skill)==before,"candidate failed material cannot leave pending shape selection")')
put('PolymorphFaultProbe.java',t)
t=(E/'EarthBindLifecycleProbe.java').read_text('utf-8').replace('EarthBind','Polymorph')
t=rep(t,'.d(target.playerObjectId()).h((int)get(target,"positionX")).h((int)get(target,"positionY"))','.d(target.playerObjectId())')
t=rep(t,'setup(pc,peer,hub,world);var row=','setup(pc,peer,hub,world);ring(pc,true);ring(peer,true);var row=')
start=t.index('    // Source C_UseSkill rejects an already paralyzed caster')
end=t.index('    need(((Map<?,?>)get(pc,"spellReadyNanos"))',start)
t=t[:start]+'''    boolean forward=active(peer,selectedSkill),reverse=active(pc,selectedSkill);
    need(forward&&reverse,"both ring-controlled casts accepted without inversion or paralysis");
    need((int)get(pc,"playerMp")==1000-mpCost&&(int)get(peer,"playerMp")==1000-peerCost&&pc.nativeDeInventory(0).countCatalog(item)==3&&peer.nativeDeInventory(0).countCatalog(item)==3,"each accepted direction pays once");
'''+t[end:]
put('PolymorphLifecycleProbe.java',t)
t=(E/'test_boundaries.py').read_text('utf-8').replace('EarthBind','Polymorph')
put('test_boundaries.py',t)
