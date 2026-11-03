"""Freeze/replay bounded actual Source method excerpts; no game classes, sockets or database access."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,sys,shutil
R=Path(__file__).resolve().parent;D=R/'build';T=Path('D:/lineage開發/1-伺服器/src/com/lineage')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8');read=lambda p:json.loads(p.read_text('utf-8'))
def method(text,marker):
 start=text.index(marker);opening=text.index('{',start);depth=1;end=opening+1
 while depth:
  depth+=(text[end]=='{')-(text[end]=='}');end+=1
 return text[start:end]
if len(sys.argv)>1:
 snapshot=Path(sys.argv[1]);value=read(snapshot)
else:
 files={
 'wand':('data/item_etcitem/wand/Lightning_Magic_Wand.java','private void doWandAction('),
 'poison':('server/model/poison/L1DamagePoison.java','private void poisonTickCurrent()'),
 'poisonKinds':('server/model/poison/L1DamagePoison.java','private static boolean isDamageTarget('),
 'event':('server/model/combat/DamageEvent.java','public DamageEvent(final L1Character attacker, final L1Character target, final int damage,\n\t\t\tfinal int attr, final boolean magicDamage, final boolean counterBarrier) {'),
 'feedback':('server/model/combat/LegacyDefenseFeedbackPlan.java','private void applyFeedbackDamage('),
 'mortal':('server/model/Instance/L1PcInstance.java','public void receiveMortalBodyReflectDamage('),
 'barrier':('server/model/L1AttackPc.java','public void commitCounterBarrier()'),
 'magicPlan':('server/model/combat/MagicHitCommitPlan.java','public void afterAccepted(')
 }
 value={'sources':{}}
 for key,(relative,marker)in files.items():
  p=T/relative;text=p.read_text('utf-8');value['sources'][key]=dict(source=pin(p),excerpt=method(text,marker))
 mirror=T/'server/model/L1MagicPc.java';text=mirror.read_text('utf-8');index=text.index('// 技能鏡反射');value['sources']['mirror']=dict(source=pin(mirror),excerpt=text[index:text.index('// [職業傷害倍數]',index)])
 run=D/'source-raw-producer-policy'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);snapshot=run/'source-method-excerpts.json';write(snapshot,value);shutil.copyfile(__file__,run/Path(__file__).name)
s={key:ref['excerpt']for key,ref in value['sources'].items()};none='DamageCommitPlan.none()'
assert none in s['event'] and 'DamageCommitPlan'not in s['wand'] and 'new DamageEvent(user, pc, dmg, 0, true, false)'in s['wand'] and 'new DamageEvent(user, mob, dmg, 0, true, false)'in s['wand']
assert 'target instanceof L1PcInstance'in s['wand'] and 'target instanceof L1MonsterInstance'in s['wand'] and 'target instanceof L1NpcInstance'not in s['wand'] and 'S_EffectLocation'in s['wand']
assert 'L1PetInstance'not in s['poisonKinds'] and 'L1SummonInstance'not in s['poisonKinds'] and 'L1PcInstance'in s['poisonKinds'] and 'L1MonsterInstance'in s['poisonKinds']
assert 'player.receiveDamage(_attacker, _damage, false, true)'in s['poison'] and 'mob.receiveDamage(_attacker, _damage)'in s['poison']
assert 'attackerPc.receiveMortalBodyReflectDamage(defender, damage)'in s['feedback'] and 'attackerPc.receiveDamage(defender, damage, false, true)'in s['feedback']
assert 'new DamageEvent(attacker, this, damage, 0, false, true)'in s['mortal']
assert '_pc.receiveDamage(_target, damage, false, true)'in s['barrier'] and '_pc.receiveDamage(_targetPc, dmg, false, false)'in s['mirror']
assert 'DamageDisplayUtil.showMagicDamage'in s['magicPlan']
for key in s:
 if key!='magicPlan':assert 'DamageDisplayUtil'not in s[key] and 'showMagicDamage'not in s[key] and 'showPhysicalDamage'not in s[key],key
if len(sys.argv)>1:print('PASS frozen Source raw-event/no-digit and wand/poison actor-kind contracts; replay without Source/DB access')
else:
 policy={key:dict(damage_digits='none',basis='raw event / receiveDamage without accepted display plan')for key in ['wand','poison','counter_barrier','counter_mirror','mortal_body','joy_of_pain']};policy['wand']['target_damage_kinds']=['PC','Monster'];policy['poison']['target_damage_kinds']=['PC','Monster']
 manifest=run/'manifest.json';write(manifest,dict(status='passed-frozen-actual-Source-raw-producer-policy',snapshot=pin(snapshot),policy=policy,reference_contracts=8,new_game_runtime_cases=0,new_database_connections=0,full_goal_achieved=False,Source_or_player_SQL_or_WAL_written=False));write(D/'source-raw-producer-policy-current.json',dict(status='passed-frozen-actual-Source-raw-producer-policy',manifest=pin(manifest),snapshot=pin(snapshot)));print(json.dumps(dict(status='passed-frozen-actual-Source-raw-producer-policy',snapshot=pin(snapshot))))
