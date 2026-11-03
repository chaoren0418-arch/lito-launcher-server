from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,difflib,sys
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
closed='--tamed-close' in sys.argv;weapon='--weapon' in sys.argv;summon='--summon-owner' in sys.argv;zombie='--create-zombie' in sys.argv;retirement='--summon-retirement' in sys.argv;combat='--summon-combat' in sys.argv;terminal='--summon-terminal' in sys.argv;grouped='--summon-group' in sys.argv;prefix='on-summon-retirement-' if retirement else 'on-summon-combat-' if combat else 'on-summon-terminal-' if terminal else 'on-summon-group-' if grouped else 'on-create-zombie-' if zombie else 'on-summon-owner-' if summon else 'on-close-' if closed else 'on-weapon-' if weapon else 'on-static-' if '--static' in sys.argv else '';bp=read(D/('summon-retirement-build-current.json' if retirement else 'summon-combat-build-current.json' if combat else 'summon-terminal-build-current.json' if terminal else 'summon-group-build-current.json' if grouped else 'create-zombie-build-current.json' if zombie else 'summon-owner-build-current.json' if summon else 'tamed-close-build-current.json' if closed else 'weapon-target-build-current.json' if weapon else 'static-motion-build-current.json' if prefix else 'pair-build-current.json'));prior=read(B/'de-native-pair-material-development/source-drift-review-current.json');old=read(Path(prior['manifest']['path']));run=D/(prefix+'source-reviews')/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);rows=[]
for row in old['Source_inputs']:
 p=Path(row['original']['path']);f=Path(row['frozen']['path']);assert sha(f)==row['frozen']['sha256'];a=f.read_text('utf-8');b=p.read_text('utf-8')
 if p.name=='CharacterTable.java':
  comparison=b;extra=''
  if sha(p)!='82771e85336a356c5aa4b4f2729d3fc47dafd6c8017025353449a9121b051a2d':
   assert sha(p)=='246e56d7ee72461e45fab843d159ba800348dc4898bb2c149ea45fb48de3a0c5'
   start=b.index('\n\t/** Merge save:');end=b.index('\n    public void storeCharacterSealed',start);fragment=b[start:end]
   assert hashlib.sha256(fragment.encode('utf-8')).hexdigest()=='e21254d12dad47565f0a5a25454e6c50ed342fce1a75ee8e0f6685a55a640813'
   assert fragment.count('public void transferClanMembershipSync(')==fragment.count('public CharacterSaveSnapshot captureWenyangUpgrade(')==1
   comparison=b[:start]+b[end:];extra='; exact inverse of two added standalone clan-merge/Wenyang capture methods (48 lines); no pre-existing method/callsite changed'
  changes=[('final String clanName, final String memberName, final boolean online) throws Exception {','final String clanName, final String memberName, final boolean online,\n\t\t\tfinal L1Clan.NpcDissolutionAttempt attempt) throws Exception {'),('\t\tfinal int memberId = pc.getId();\n','\t\tif (attempt == null) throw new IllegalArgumentException("Missing NPC dissolution attempt");\n\t\tfinal int memberId = pc.getId();\n'),('\t\t\t\tcom.lineage.server.model.MigrationDeparture.requireWritable(pc.getId());\n','\t\t\t\tcom.lineage.server.model.MigrationDeparture.requireWritable(pc.getId());\n\t\t\t\tattempt.beforeMutation();\n')]
  restored=a
  for before,after in changes:assert restored.count(before)==1;restored=restored.replace(before,after)
  assert restored==comparison;scope='whole file inverse exact for three NPC-clan-dissolution attempt substitutions; selected-role AccessLevel/load unchanged; no clan task run or Source write'+extra
 elif p.name=='L1PcInventory.java':
  if a!=b:
   assert sha(f)=='445a1523a87da8c5a455ee4ce941c4b1860c90a798315315326c11623b2aa57b' and sha(p) in {'6583c051a06b77b30cfe69b39caf7db7f827dc7e05f5526e10c2896519046abf','220b001573ec6766dcdd16370c4e1cbd522721367e00c127c90bc2d3779926e4'}
   before='\t\t\t\tcom.lineage.server.datatables.user.CharacterDarkItemTable.getInstance().storeDarkItem(item.getDarkItem());'
   after='\t\t\t\tif (!com.lineage.server.datatables.user.CharacterDarkItemTable.getInstance()\n\t\t\t\t\t\t.storeDarkItem(item.getDarkItem())) {\n\t\t\t\t\tthrow new IllegalStateException("item dark auxiliary persistence failed");\n\t\t\t\t}'
   comparison=b
   if sha(p)=='220b001573ec6766dcdd16370c4e1cbd522721367e00c127c90bc2d3779926e4':
    guard='\t\t\t\t\tif (hours > Integer.MAX_VALUE / 3600) { _log.error("Timed item hours exceed remaining_time range itemId={} hours={}", item.getItemId(), hours); return null; }\n'
    assert b.count(guard)==2
    comparison=b.replace(guard,'')
    assert hashlib.sha256(comparison.replace('\n','\r\n').encode('utf-8')).hexdigest()=='6583c051a06b77b30cfe69b39caf7db7f827dc7e05f5526e10c2896519046abf'
   assert a.count(before)==1 and a.replace(before,after)==comparison
   scope='whole-file inverse exact for the prior dark auxiliary failure propagation plus exactly two timed-item deadline initialization overflow guards; material planner/consumption, skill68 selector and role/GM policy unchanged; readonly review only, no Source persistence run'
  else:scope='whole file unchanged since exact skill68 material/GM review'
 else:assert a==b;scope='whole file unchanged since exact skill68 material/GM review'
 q=run/p.name;shutil.copyfile(p,q);diff=run/(p.name+'.diff');diff.write_text(''.join(difflib.unified_diff(a.splitlines(True),b.splitlines(True))),encoding='utf-8');rows.append(dict(original=pin(p),frozen=pin(q),previous=row['frozen'],diff=pin(diff),scope=scope))
shutil.copyfile(__file__,run/Path(__file__).name);v=dict(status='passed-reviewed-current-Source-material-selector-and-role-policy',runtime=bp['runtime'],build=bp['manifest'],prior_review=prior['manifest'],Source_inputs=rows,reviewer=pin(run/Path(__file__).name),Source_or_player_SQL_or_WAL_written=False,formal_deployment_changed=False,full_goal_achieved=False);write(run/'manifest.json',v);write(D/(prefix+'source-drift-review-current.json'),dict(status=v['status'],runtime=bp['runtime'],manifest=pin(run/'manifest.json')));print(v['status'])
