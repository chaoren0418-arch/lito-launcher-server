"""Adapt the pair-lifetime probes; explicitly install a read-only clan fixture."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;W=R.parent/'water-life-material'
for name in ['Material','Fault','Lifecycle']:
 t=(W/('WaterLife'+name+'Probe.java')).read_text('utf-8')
 for a,b in [('WaterLife'+name+'Probe','Protection'+name+'Probe'),('WaterLifeMaterialProbe','ProtectionMaterialProbe'),('BattleWaterLifeStatus','BattleAbilityStatus'),('waterLifeStatus','abilityStatus'),('waterLifeSpell','damageProtectionSpell'),('170','68'),('Water Life','Damage protection'),('water-life','protection'),('Water-Life','immune-to-harm')]:t=t.replace(a,b)
 t=t.replace('.active(System.nanoTime())','.immuneToHarm(System.nanoTime()).divisor()>0')
 # Comparisons require the complete boolean expression, not the divisor itself.
 import re
 t=re.sub(r'(\(\(BattleAbilityStatus\)get\([^;\n]+?\)\)\.immuneToHarm\(System.nanoTime\(\)\)\.divisor\(\)>0)(==accepted|==before)',r'(\1)\2',t)
 t=t.replace('status.immuneToHarm(System.nanoTime()).divisor()>0==accepted','(status.immuneToHarm(System.nanoTime()).divisor()>0)==accepted')
 t=t.replace('!status.immuneToHarm(System.nanoTime()).divisor()>0','status.immuneToHarm(System.nanoTime()).divisor()==0')
 t=t.replace('!((BattleAbilityStatus)get(mode==0?peer:pc,"abilityStatus")).immuneToHarm(System.nanoTime()).divisor()>0','((BattleAbilityStatus)get(mode==0?peer:pc,"abilityStatus")).immuneToHarm(System.nanoTime()).divisor()==0')
 t=t.replace('((BattleAbilityStatus)get(p,"abilityStatus")).cancel();','call(get(p,"abilityStatus"),"clearImmuneToHarm",new Class[]{});')
 if name=='Material':
  needle='set(pc,"learnedSkills",new HashSet<>(Set.of(2,68)));targetId=peer.playerObjectId();'
  assert needle in t
  t=t.replace(needle,needle+'\n  var clans=(Map<String,String>)get(hub,"clanMembership");\n  for(var p:List.of(pc,peer))clans.put(p.sourceCharacterIdentity().toLowerCase(Locale.ROOT),"protection-fixture-clan");')
  t=t.replace('if(mode==7)set(peer,"positionX",(int)get(pc,"positionX")+40);','''if(mode==7)set(peer,"positionX",(int)get(pc,"positionX")+40);
  String peerIdentity=peer.sourceCharacterIdentity().toLowerCase(Locale.ROOT);
  var clans=(Map<String,String>)get(hub,"clanMembership");
  if(mode==8)clans.put(peerIdentity,"other-clan");
  if(mode==9)clans.remove(peerIdentity);
  if(mode==10)set(peer,"invisible",true);
  if(mode==11){if(world)set(peer,"classicWorldInstanceId",(int)get(peer,"classicWorldInstanceId")+1);else set(peer,"currentRoomId",(long)get(peer,"currentRoomId")+1);}
  if(mode==12)set(peer,"observer",true);''')
  t=t.replace('mode<8','mode<13').replace('real one-shot recovery status','original damage protection status')
 if name=='Fault':
  t=t.replace('need(peers.isEmpty()&&wire.locked+other.locked==0,"failure output unlocked and no peer effect");','need(peers.isEmpty()!=before&&wire.locked+other.locked==0,"original effect precedes failure; candidate rejects before target output");')
 if name=='Lifecycle':t=t.replace('.c(21).c(1)', '.c(8).c(3)')
 (R/('Protection'+name+'Probe.java')).write_text(t,encoding='utf-8')
for name in ['test.py','test_boundaries.py']:
 t=(W/name).read_text('utf-8').replace('WaterLife','Protection')
 if name=='test.py':t=t.replace("v[mode]['cases']==64", "v[mode]['cases']==104")
 (R/name).write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'test-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p)) for p in [*W.glob('*Probe.java'),W/'test.py',W/'test_boundaries.py',Path(__file__)]],scope='skill68 same-clan/self fixture; status type and pre-save baseline output differences; not qualified'),indent=2),encoding='utf-8')
