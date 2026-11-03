"""Advance the unqualified program index only; preserve historical qualification pointers."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,zipfile,re,subprocess,sys
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];C=R.parent/'continuation-current.json'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def verify(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:assert sha(Path(v['path']))==v['sha256'],v['path']
  for x in v.values():verify(x)
 elif isinstance(v,list):
  for x in v:verify(x)
index=D/'source-program-update-progress-current.json';proofpath=D/'Source-lifecycle-progress-verified-current.json';auditpath=D/'source-summon-lifetime-audit-current.json'
proof=read(proofpath)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
previous=read(index);verify(previous);old=read(Path(previous['manifest']['path']))
damagehealingselection=len(sys.argv)>1 and sys.argv[1]=='damage-healing-selection';areaclassselection=len(sys.argv)>1 and sys.argv[1]=='area-class-selection';objecttargetrequest=len(sys.argv)>1 and sys.argv[1]=='object-target-request';nonetargetrequest=len(sys.argv)>1 and sys.argv[1]=='none-target-request';massselftarget=len(sys.argv)>1 and sys.argv[1]=='mass-self-target';negativephase=len(sys.argv)>1 and sys.argv[1]=='negative-phase';npctargetfailure=len(sys.argv)>1 and sys.argv[1]=='npc-target-failure';npcfactorymemory=len(sys.argv)>1 and sys.argv[1]=='npc-factory-memory';npcfactorytype=len(sys.argv)>1 and sys.argv[1]=='npc-factory-type';indirectplayerproof=len(sys.argv)>1 and sys.argv[1]=='indirect-player-proof';areaplayerproof=len(sys.argv)>1 and sys.argv[1]=='area-player-proof';materialfirstlife=len(sys.argv)>1 and sys.argv[1]=='material-first-life';npcfirstlife=len(sys.argv)>1 and sys.argv[1]=='npc-first-life';npcorigins=len(sys.argv)>1 and sys.argv[1]=='npc-origins';acceptedplayers=len(sys.argv)>1 and sys.argv[1]=='accepted-players';playerfactories=len(sys.argv)>1 and sys.argv[1]=='player-factories';firstplayers=len(sys.argv)>1 and sys.argv[1]=='first-players';controlledhealing=len(sys.argv)>1 and sys.argv[1]=='controlled-healing';healingtype=len(sys.argv)>1 and sys.argv[1]=='healing-type';healingformula=len(sys.argv)>1 and sys.argv[1]=='healing-formula';healinghp=len(sys.argv)>1 and sys.argv[1]=='healing-hp';healingcast=len(sys.argv)>1 and sys.argv[1]=='healing-cast';signedrecovery=len(sys.argv)>1 and sys.argv[1]=='signed-recovery';npcmagicdirect=len(sys.argv)>1 and sys.argv[1]=='npc-magic-direct';npcmagiceffects=len(sys.argv)>1 and sys.argv[1]=='npc-magic-effects';npcplayer=len(sys.argv)>1 and sys.argv[1]=='npc-player';fatalitydamage=len(sys.argv)>1 and sys.argv[1]=='fatality-damage';cubetemplate=len(sys.argv)>1 and sys.argv[1]=='cube-template';cubevisibility=len(sys.argv)>1 and sys.argv[1]=='cube-visibility';cubeclock=len(sys.argv)>1 and sys.argv[1]=='cube-clock';cuberelation=len(sys.argv)>1 and sys.argv[1]=='cube-relation';cubepresentation=len(sys.argv)>1 and sys.argv[1]=='cube-presentation';cuberemainder=len(sys.argv)>1 and sys.argv[1]=='cube-remainder';cubestatus=len(sys.argv)>1 and sys.argv[1]=='cube-status';cubeplacement=len(sys.argv)>1 and sys.argv[1]=='cube-placement';groundplayer=len(sys.argv)>1 and sys.argv[1]=='ground-player';groundnpc=len(sys.argv)>1 and sys.argv[1]=='ground-npc';groundplacement=len(sys.argv)>1 and sys.argv[1]=='ground-placement';groundqueued=len(sys.argv)>1 and sys.argv[1]=='ground-queued';groundorigin=len(sys.argv)>1 and sys.argv[1]=='ground-origin';wandreaction=len(sys.argv)>1 and sys.argv[1]=='wand-reaction';npcreaction=len(sys.argv)>1 and sys.argv[1]=='npc-reaction';hitreaction=len(sys.argv)>1 and sys.argv[1]=='hit-reaction';refrawnpc=len(sys.argv)>1 and sys.argv[1]=='ref-raw-npc';rawnpc=len(sys.argv)>1 and sys.argv[1]=='raw-npc';poisonpc=len(sys.argv)>1 and sys.argv[1]=='poison-pc';specialnpc=len(sys.argv)>1 and sys.argv[1]=='special-npc';targetnpc=len(sys.argv)>1 and sys.argv[1]=='target-npc';singlenpc=len(sys.argv)>1 and sys.argv[1]=='single-npc';magicnpc=len(sys.argv)>1 and sys.argv[1]=='magic-npc';physicalnpc=len(sys.argv)>1 and sys.argv[1]=='physical-npc';poisonnpc=len(sys.argv)>1 and sys.argv[1]=='poison-npc';capacity=len(sys.argv)>1 and sys.argv[1]=='damage-capacity';cubenpcqueue=len(sys.argv)>1 and sys.argv[1]=='cube-npc-queue';cubenpc=len(sys.argv)>1 and sys.argv[1]=='cube-npc';cubencplease=len(sys.argv)>1 and sys.argv[1]=='cube-npc-lease';cubeplayer=len(sys.argv)>1 and sys.argv[1]=='cube-player';cube=len(sys.argv)>1 and sys.argv[1]=='cube';passive=len(sys.argv)>1 and sys.argv[1]=='passive';defense=len(sys.argv)>1 and sys.argv[1]=='defense';slow=len(sys.argv)>1 and sys.argv[1]=='slow';accepted=len(sys.argv)>1 and sys.argv[1]=='accepted';npc=len(sys.argv)>1 and sys.argv[1]=='npc';action=len(sys.argv)>1 and sys.argv[1]=='action';skills=len(sys.argv)>1 and sys.argv[1]=='skills';output=len(sys.argv)>1 and sys.argv[1]=='output';stage='source-damage-healing-selection'if damagehealingselection else'source-area-class-selection'if areaclassselection else'source-object-target-request'if objecttargetrequest else'source-none-target-request'if nonetargetrequest else'source-mass-self-target'if massselftarget else'source-negative-phase'if negativephase else'source-npc-target-failure'if npctargetfailure else'source-npc-factory-memory'if npcfactorymemory else'source-npc-factory-type'if npcfactorytype else'source-indirect-player-proof'if indirectplayerproof else'source-area-player-proof'if areaplayerproof else'source-material-first-life'if materialfirstlife else'source-npc-first-life'if npcfirstlife else'source-npc-origins'if npcorigins else'source-accepted-players'if acceptedplayers else'source-player-factories'if playerfactories else'source-first-players'if firstplayers else'source-controlled-healing'if controlledhealing else'source-healing-type'if healingtype else'source-healing-formula'if healingformula else'source-healing-hp'if healinghp else'source-healing-cast'if healingcast else'source-signed-recovery'if signedrecovery else'source-npc-magic-direct'if npcmagicdirect else'source-npc-magic-effects'if npcmagiceffects else'source-npc-player'if npcplayer else'source-fatality-damage'if fatalitydamage else'source-cube-template'if cubetemplate else'source-cube-visibility'if cubevisibility else'source-cube-clock'if cubeclock else'source-cube-relation'if cuberelation else'source-cube-presentation'if cubepresentation else'source-cube-remainder'if cuberemainder else'source-cube-status'if cubestatus else'source-cube-placement'if cubeplacement else'source-ground-player'if groundplayer else'source-ground-npc'if groundnpc else'source-ground-placement'if groundplacement else'source-ground-queued'if groundqueued else'source-ground-origin'if groundorigin else'source-wand-reaction'if wandreaction else'source-npc-reaction'if npcreaction else'source-hit-reaction'if hitreaction else'source-ref-raw-npc'if refrawnpc else'source-raw-npc'if rawnpc else'source-poison-pc'if poisonpc else'source-special-npc'if specialnpc else'source-target-npc'if targetnpc else'source-single-npc'if singlenpc else'source-magic-npc'if magicnpc else'source-physical-npc'if physicalnpc else'source-poison-npc'if poisonnpc else'source-damage-capacity'if capacity else'source-cube-npc-queue'if cubenpcqueue else'source-cube-npc'if cubenpc else'source-cube-npc-lease'if cubencplease else'source-cube-player'if cubeplayer else'source-cube-origin'if cube else'source-doll-passive'if passive else'source-doll-defense'if defense else'source-doll-slow'if slow else'source-doll-output'if output else'source-doll-skills'if skills else'source-doll-action'if action else'source-doll-npc'if npc else'source-doll-accepted'if accepted else'source-doll-commit'
latest=read(D/(stage+'-build-current.json'));repro=read(D/(stage+'-reproduction-current.json'));inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
if passive:
 consumers=read(D/'source-doll-consumers-current.json');verify(consumers);assert consumers['runtime']==latest['runtime'];consumer_manifest=read(Path(consumers['manifest']['path']));verify(consumer_manifest)
build=read(Path(latest['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
assert build['base']==previous['runtime'] or latest['runtime']==previous['runtime'] and old['build']==latest['manifest'] or latest['runtime']['sha256']==previous['runtime']['sha256'] and build['base']==read(Path(old['build']['path']))['base'];verify(build['inputs']);verify(build['asm']);verify(build.get('Source_inputs',[]));verify(build.get('Source_frozen_references',[]));verify(inv['artifacts']);verify(inv['Source_configuration_inputs']);verify(contract['Source_excerpts']);verify(contract['candidate_static_callers'])
assert inv['goal']['status']=='active';goal=D/'source-completion-goal-current.json';assert read(goal)['status']=='active'
assert subprocess.run(['powershell','-NoProfile','-Command',"@(Get-CimInstance Win32_Process -Filter \"Name = 'java.exe'\" | Where-Object {$_.CommandLine -like '*integration-lifecycle*'}).Count"],capture_output=True,text=True,check=True).stdout.strip()=='0'
anchors={p:sha(p)for p in [R/'current.json',D/'source-kiringku-progress-current.json',D/'competition-only-product-scope-current.json',D/'source-equipment-mysql-reload-readonly-origin-current.json',D/'source-raw-producer-build-current.json',D/'source-arena-poison-build-current.json']}
for ref in old.get('additional_protected',[]):verify(ref)
sqlbase=old['readonly_SQL_consumers_byte_retained']['base'];verify(sqlbase);runtime=latest['runtime'];digest=runtime['sha256'];retained=old['readonly_SQL_consumers_byte_retained']['entries']
with zipfile.ZipFile(sqlbase['path'])as sql,zipfile.ZipFile(runtime['path'])as now:
 for name in retained:assert sql.read(name)==now.read(name),name
assert len(retained)==20
run=D/'source-completion-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'editable-input-snapshot';inputs.mkdir(parents=True)
current=S/'docs/maintenance/combat-continuation-current.zh-TW.md';doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';work=S/'docs/maintenance/source-completion-work-current.zh-TW.md'
for p,name in [(index,'previous-program-progress.json'),(proofpath,'previous-proof.json'),(auditpath,'previous-audit.json'),(C,'previous-continuation.json'),(current,'previous-current.md'),(doc,'previous-lifecycle.md'),(work,'previous-work.md')]:shutil.copyfile(p,run/name)
for name in ['NativeSourceDollCommit.java','SourceDollCommitMethods.java','SourceDollCommitDelta.java','SourceDollCommitLoad.java','build_source_doll_commit.py','reproduce_source_doll_commit.py','audit_source_next_producers.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
if accepted or npc or action or skills or output or slow or defense or passive or cube or cubeplayer or cubencplease or cubenpc or cubenpcqueue or capacity or poisonnpc or physicalnpc or magicnpc or singlenpc or targetnpc or specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceDollAccepted.java','SourceDollAcceptedMethods.java','SourceDollAcceptedDelta.java','SourceDollAcceptedLoad.java','build_source_doll_accepted.py','reproduce_source_doll_accepted.py']:shutil.copyfile(R/name,inputs/name)
if npc or action or skills or output or slow or defense or passive or cube or cubeplayer or cubencplease or cubenpc or cubenpcqueue or capacity or poisonnpc or physicalnpc or magicnpc or singlenpc or targetnpc or specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceDollNpc.java','SourceDollNpcMethods.java','SourceDollNpcDelta.java','SourceDollNpcLoad.java','build_source_doll_npc.py','reproduce_source_doll_npc.py']:shutil.copyfile(R/name,inputs/name)
if action or skills or output or slow or defense or passive or cube or cubeplayer or cubencplease or cubenpc or cubenpcqueue or capacity or poisonnpc or physicalnpc or magicnpc or singlenpc or targetnpc or specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceDollAction.java','SourceDollActionMethods.java','SourceDollActionDelta.java','SourceDollActionLoad.java','SourceDollActionPlan.java','build_source_doll_action.py','reproduce_source_doll_action.py']:shutil.copyfile(R/name,inputs/name)
if skills or output or slow or defense or passive or cube or cubeplayer or cubencplease or cubenpc or cubenpcqueue or capacity or poisonnpc or physicalnpc or magicnpc or singlenpc or targetnpc or specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceDollSkillData.java','NativeSourceDollSkills.java','SourceDollSkillsMethods.java','SourceDollSkillsDelta.java','SourceDollSkillsLoad.java','SourceDollSkillsPlan.java','build_source_doll_skills.py','reproduce_source_doll_skills.py']:shutil.copyfile(R/name,inputs/name)
if output or slow or defense or passive or cube or cubeplayer or cubencplease or cubenpc or cubenpcqueue or capacity or poisonnpc or physicalnpc or magicnpc or singlenpc or targetnpc or specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceDollOutput.java','SourceDollOutputMethods.java','SourceDollOutputDelta.java','SourceDollOutputLoad.java','SourceDollOutputPlan.java','build_source_doll_output.py','reproduce_source_doll_output.py']:shutil.copyfile(R/name,inputs/name)
if slow or defense or passive or cube or cubeplayer or cubencplease or cubenpc or cubenpcqueue or capacity or poisonnpc or physicalnpc or magicnpc or singlenpc or targetnpc or specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceDollSlow.java','SourceDollSlowMethods.java','SourceDollSlowDelta.java','SourceDollSlowLoad.java','SourceDollSlowPlan.java','build_source_doll_slow.py','reproduce_source_doll_slow.py']:shutil.copyfile(R/name,inputs/name)
if defense or passive or cube or cubeplayer or cubencplease or cubenpc or cubenpcqueue or capacity or poisonnpc or physicalnpc or magicnpc or singlenpc or targetnpc or specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceDollDefense.java','SourceDollDefenseMethods.java','SourceDollDefenseDelta.java','SourceDollDefenseLoad.java','SourceDollDefensePlan.java','build_source_doll_defense.py','reproduce_source_doll_defense.py']:shutil.copyfile(R/name,inputs/name)
if passive or cube or cubeplayer or cubencplease or cubenpc or cubenpcqueue or capacity or poisonnpc or physicalnpc or magicnpc or singlenpc or targetnpc or specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceDollPassive.java','SourceDollPassiveMethods.java','SourceDollPassiveDelta.java','SourceDollPassiveLoad.java','SourceDollPassivePlan.java','build_source_doll_passive.py','reproduce_source_doll_passive.py','audit_source_doll_consumers.py']:shutil.copyfile(R/name,inputs/name)
if cube or cubeplayer or cubencplease or cubenpc or cubenpcqueue or capacity or poisonnpc or physicalnpc or magicnpc or singlenpc or targetnpc or specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceCubeOrigin.java','SourceCubeOriginMethods.java','SourceCubeOriginDelta.java','SourceCubeOriginLoad.java','SourceCubeOriginPlan.java','build_source_cube_origin.py','reproduce_source_cube_origin.py']:shutil.copyfile(R/name,inputs/name)
if cubeplayer or cubencplease or cubenpc or cubenpcqueue or capacity or poisonnpc or physicalnpc or magicnpc or singlenpc or targetnpc or specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceCubePlayer.java','SourceCubePlayerMethods.java','SourceCubePlayerDelta.java','SourceCubePlayerLoad.java','SourceCubePlayerPlan.java','build_source_cube_player.py','reproduce_source_cube_player.py']:shutil.copyfile(R/name,inputs/name)
if cubencplease or cubenpc or cubenpcqueue or capacity or poisonnpc or physicalnpc or magicnpc or singlenpc or targetnpc or specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceCubeNpcLease.java','SourceCubeNpcLeaseMethods.java','SourceCubeNpcLeaseDelta.java','SourceCubeNpcLeaseLoad.java','SourceCubeNpcLeasePlan.java','build_source_cube_npc_lease.py','reproduce_source_cube_npc_lease.py']:shutil.copyfile(R/name,inputs/name)
if cubenpc or cubenpcqueue or capacity or poisonnpc or physicalnpc or magicnpc or singlenpc or targetnpc or specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceCubeNpc.java','SourceCubeNpcMethods.java','SourceCubeNpcDelta.java','SourceCubeNpcLoad.java','SourceCubeNpcPlan.java','build_source_cube_npc.py','reproduce_source_cube_npc.py']:shutil.copyfile(R/name,inputs/name)
if cubenpcqueue or capacity or poisonnpc or physicalnpc or magicnpc or singlenpc or targetnpc or specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceCubeNpcQueue.java','NativeSourcePendingAdmission.java','SourceCubeNpcQueueMethods.java','SourceCubeNpcQueueDelta.java','SourceCubeNpcQueueLoad.java','SourceCubeNpcQueuePlan.java','build_source_cube_npc_queue.py','reproduce_source_cube_npc_queue.py']:shutil.copyfile(R/name,inputs/name)
if capacity or poisonnpc or physicalnpc or magicnpc or singlenpc or targetnpc or specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceDamageCapacity.java','SourceDamageCapacityMethods.java','SourceDamageCapacityDelta.java','SourceDamageCapacityLoad.java','SourceDamageCapacityPlan.java','build_source_damage_capacity.py','reproduce_source_damage_capacity.py']:shutil.copyfile(R/name,inputs/name)
if poisonnpc or physicalnpc or magicnpc or singlenpc or targetnpc or specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourcePoisonNpc.java','SourcePoisonNpcMethods.java','SourcePoisonNpcDelta.java','SourcePoisonNpcLoad.java','SourcePoisonNpcPlan.java','build_source_poison_npc.py','reproduce_source_poison_npc.py']:shutil.copyfile(R/name,inputs/name)
if physicalnpc or magicnpc or singlenpc or targetnpc or specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourcePhysicalNpc.java','SourcePhysicalNpcMethods.java','SourcePhysicalNpcDelta.java','SourcePhysicalNpcLoad.java','SourcePhysicalNpcPlan.java','build_source_physical_npc.py','reproduce_source_physical_npc.py']:shutil.copyfile(R/name,inputs/name)
if magicnpc or singlenpc or targetnpc or specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceMagicNpc.java','SourceMagicNpcMethods.java','SourceMagicNpcDelta.java','SourceMagicNpcLoad.java','SourceMagicNpcPlan.java','build_source_magic_npc.py','reproduce_source_magic_npc.py']:shutil.copyfile(R/name,inputs/name)
if singlenpc or targetnpc or specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceSingleNpc.java','SourceSingleNpcMethods.java','SourceSingleNpcDelta.java','SourceSingleNpcLoad.java','SourceSingleNpcPlan.java','build_source_single_npc.py','reproduce_source_single_npc.py']:shutil.copyfile(R/name,inputs/name)
if targetnpc or specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceTargetNpc.java','SourceTargetNpcMethods.java','SourceTargetNpcDelta.java','SourceTargetNpcLoad.java','SourceTargetNpcPlan.java','build_source_target_npc.py','reproduce_source_target_npc.py']:shutil.copyfile(R/name,inputs/name)
if specialnpc or poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceSpecialNpc.java','SourceSpecialNpcMethods.java','SourceSpecialNpcDelta.java','SourceSpecialNpcLoad.java','SourceSpecialNpcPlan.java','build_source_special_npc.py','reproduce_source_special_npc.py']:shutil.copyfile(R/name,inputs/name)
if poisonpc or rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourcePoisonPc.java','SourcePoisonPcMethods.java','SourcePoisonPcDelta.java','SourcePoisonPcLoad.java','build_source_poison_pc.py','reproduce_source_poison_pc.py']:shutil.copyfile(R/name,inputs/name)
if rawnpc or refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceRawNpc.java','SourceRawNpcMethods.java','SourceRawNpcDelta.java','SourceRawNpcLoad.java','SourceRawNpcPlan.java','build_source_raw_npc.py','reproduce_source_raw_npc.py']:shutil.copyfile(R/name,inputs/name)
if refrawnpc or hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceRefRawNpc.java','SourceRefRawNpcMethods.java','SourceRefRawNpcDelta.java','SourceRefRawNpcLoad.java','SourceRefRawNpcPlan.java','build_source_ref_raw_npc.py','reproduce_source_ref_raw_npc.py']:shutil.copyfile(R/name,inputs/name)
if hitreaction or npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceHitReaction.java','SourceHitReactionMethods.java','SourceHitReactionDelta.java','SourceHitReactionLoad.java','SourceHitReactionPlan.java','build_source_hit_reaction.py','reproduce_source_hit_reaction.py']:shutil.copyfile(R/name,inputs/name)
if npcreaction or wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceNpcReaction.java','SourceNpcReactionMethods.java','SourceNpcReactionDelta.java','SourceNpcReactionLoad.java','SourceNpcReactionPlan.java','build_source_npc_reaction.py','reproduce_source_npc_reaction.py']:shutil.copyfile(R/name,inputs/name)
if wandreaction or groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceWandReaction.java','SourceWandReactionMethods.java','SourceWandReactionDelta.java','SourceWandReactionLoad.java','SourceWandReactionPlan.java','build_source_wand_reaction.py','reproduce_source_wand_reaction.py']:shutil.copyfile(R/name,inputs/name)
if groundorigin or groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceGroundOrigin.java','SourceGroundOriginMethods.java','SourceGroundOriginDelta.java','SourceGroundOriginLoad.java','SourceGroundOriginPlan.java','build_source_ground_origin.py','reproduce_source_ground_origin.py']:shutil.copyfile(R/name,inputs/name)
if groundqueued or groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceGroundQueued.java','SourceGroundQueuedMethods.java','SourceGroundQueuedDelta.java','SourceGroundQueuedLoad.java','SourceGroundQueuedPlan.java','build_source_ground_queued.py','reproduce_source_ground_queued.py']:shutil.copyfile(R/name,inputs/name)
if groundplacement or groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceGroundPlacement.java','SourceGroundPlacementMethods.java','SourceGroundPlacementDelta.java','SourceGroundPlacementLoad.java','SourceGroundPlacementPlan.java','build_source_ground_placement.py','reproduce_source_ground_placement.py']:shutil.copyfile(R/name,inputs/name)
if groundnpc or groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceGroundNpc.java','SourceGroundNpcMethods.java','SourceGroundNpcDelta.java','SourceGroundNpcLoad.java','SourceGroundNpcPlan.java','build_source_ground_npc.py','reproduce_source_ground_npc.py']:shutil.copyfile(R/name,inputs/name)
if groundplayer or cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceGroundPlayer.java','SourceGroundPlayerMethods.java','SourceGroundPlayerDelta.java','SourceGroundPlayerLoad.java','SourceGroundPlayerPlan.java','build_source_ground_player.py','reproduce_source_ground_player.py']:shutil.copyfile(R/name,inputs/name)
if cubeplacement or cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceCubePlacement.java','SourceCubePlacementMethods.java','SourceCubePlacementDelta.java','SourceCubePlacementLoad.java','SourceCubePlacementPlan.java','build_source_cube_placement.py','reproduce_source_cube_placement.py']:shutil.copyfile(R/name,inputs/name)
if cubestatus or cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceCubeStatus.java','SourceCubeStatusMethods.java','SourceCubeStatusDelta.java','SourceCubeStatusLoad.java','SourceCubeStatusPlan.java','build_source_cube_status.py','reproduce_source_cube_status.py']:shutil.copyfile(R/name,inputs/name)
if cuberemainder or cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceCubeRemainder.java','SourceCubeRemainderMethods.java','SourceCubeRemainderDelta.java','SourceCubeRemainderLoad.java','SourceCubeRemainderPlan.java','build_source_cube_remainder.py','reproduce_source_cube_remainder.py']:shutil.copyfile(R/name,inputs/name)
if cubepresentation or cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceCubePresentation.java','SourceCubePresentationMethods.java','SourceCubePresentationDelta.java','SourceCubePresentationLoad.java','SourceCubePresentationPlan.java','build_source_cube_presentation.py','reproduce_source_cube_presentation.py']:shutil.copyfile(R/name,inputs/name)
if cuberelation or cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceCubeRelation.java','SourceCubeRelationMethods.java','SourceCubeRelationDelta.java','SourceCubeRelationLoad.java','SourceCubeRelationPlan.java','build_source_cube_relation.py','reproduce_source_cube_relation.py']:shutil.copyfile(R/name,inputs/name)
if cubeclock or cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceCubeClock.java','SourceCubeClockMethods.java','SourceCubeClockDelta.java','SourceCubeClockLoad.java','SourceCubeClockPlan.java','build_source_cube_clock.py','reproduce_source_cube_clock.py']:shutil.copyfile(R/name,inputs/name)
if cubevisibility or cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceCubeVisibility.java','SourceCubeVisibilityMethods.java','SourceCubeVisibilityDelta.java','SourceCubeVisibilityLoad.java','SourceCubeVisibilityPlan.java','build_source_cube_visibility.py','reproduce_source_cube_visibility.py']:shutil.copyfile(R/name,inputs/name)
if cubetemplate or fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceCubeTemplate.java','SourceCubeTemplateMethods.java','SourceCubeTemplateDelta.java','SourceCubeTemplateLoad.java','SourceCubeTemplatePlan.java','build_source_cube_template.py','reproduce_source_cube_template.py']:shutil.copyfile(R/name,inputs/name)
if fatalitydamage or npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceFatalityDamage.java','SourceFatalityDamageMethods.java','SourceFatalityDamageDelta.java','SourceFatalityDamageLoad.java','SourceFatalityDamagePlan.java','build_source_fatality_damage.py','reproduce_source_fatality_damage.py']:shutil.copyfile(R/name,inputs/name)
if npcplayer or npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceNpcPlayer.java','SourceNpcPlayerMethods.java','SourceNpcPlayerDelta.java','SourceNpcPlayerLoad.java','SourceNpcPlayerPlan.java','build_source_npc_player.py','reproduce_source_npc_player.py']:shutil.copyfile(R/name,inputs/name)
if npcmagiceffects or npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceNpcMagicEffects.java','SourceNpcMagicEffectsMethods.java','SourceNpcMagicEffectsDelta.java','SourceNpcMagicEffectsLoad.java','SourceNpcMagicEffectsPlan.java','build_source_npc_magic_effects.py','reproduce_source_npc_magic_effects.py']:shutil.copyfile(R/name,inputs/name)
if npcmagicdirect or signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceNpcMagicDirect.java','SourceNpcMagicDirectMethods.java','SourceNpcMagicDirectDelta.java','SourceNpcMagicDirectLoad.java','SourceNpcMagicDirectPlan.java','build_source_npc_magic_direct.py','reproduce_source_npc_magic_direct.py']:shutil.copyfile(R/name,inputs/name)
if signedrecovery or healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceSignedRecovery.java','SourceSignedRecoveryMethods.java','SourceSignedRecoveryDelta.java','SourceSignedRecoveryLoad.java','SourceSignedRecoveryPlan.java','build_source_signed_recovery.py','reproduce_source_signed_recovery.py']:shutil.copyfile(R/name,inputs/name)
if healingcast or healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceHealingCast.java','SourceHealingCastMethods.java','SourceHealingCastDelta.java','SourceHealingCastLoad.java','SourceHealingCastPlan.java','build_source_healing_cast.py','reproduce_source_healing_cast.py']:shutil.copyfile(R/name,inputs/name)
if healinghp or healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceHealingHp.java','SourceHealingHpMethods.java','SourceHealingHpDelta.java','SourceHealingHpLoad.java','SourceHealingHpPlan.java','build_source_healing_hp.py','reproduce_source_healing_hp.py']:shutil.copyfile(R/name,inputs/name)
if healingformula or healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceHealingFormula.java','SourceHealingFormulaMethods.java','SourceHealingFormulaDelta.java','SourceHealingFormulaLoad.java','SourceHealingFormulaPlan.java','build_source_healing_formula.py','reproduce_source_healing_formula.py']:shutil.copyfile(R/name,inputs/name)
if healingtype or controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceHealingType.java','SourceHealingTypeMethods.java','SourceHealingTypeDelta.java','SourceHealingTypeLoad.java','SourceHealingTypePlan.java','build_source_healing_type.py','reproduce_source_healing_type.py']:shutil.copyfile(R/name,inputs/name)
if controlledhealing or firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceControlledHealing.java','SourceControlledHealingMethods.java','SourceControlledHealingDelta.java','SourceControlledHealingLoad.java','SourceControlledHealingPlan.java','build_source_controlled_healing.py','reproduce_source_controlled_healing.py']:shutil.copyfile(R/name,inputs/name)
if firstplayers or playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceFirstPlayers.java','SourceFirstPlayersMethods.java','SourceFirstPlayersDelta.java','SourceFirstPlayersLoad.java','SourceFirstPlayersPlan.java','build_source_first_players.py','reproduce_source_first_players.py']:shutil.copyfile(R/name,inputs/name)
if playerfactories or acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourcePlayerFactories.java','SourcePlayerFactoriesMethods.java','SourcePlayerFactoriesDelta.java','SourcePlayerFactoriesLoad.java','SourcePlayerFactoriesPlan.java','build_source_player_factories.py','reproduce_source_player_factories.py']:shutil.copyfile(R/name,inputs/name)
if acceptedplayers or npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceAcceptedPlayers.java','SourceAcceptedPlayersMethods.java','SourceAcceptedPlayersDelta.java','SourceAcceptedPlayersLoad.java','SourceAcceptedPlayersPlan.java','build_source_accepted_players.py','reproduce_source_accepted_players.py']:shutil.copyfile(R/name,inputs/name)
if npcorigins or npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceNpcOrigins.java','SourceNpcOriginsMethods.java','SourceNpcOriginsDelta.java','SourceNpcOriginsLoad.java','SourceNpcOriginsPlan.java','build_source_npc_origins.py','reproduce_source_npc_origins.py']:shutil.copyfile(R/name,inputs/name)
if npcfirstlife or materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceNpcFirstLife.java','SourceNpcFirstLifeMethods.java','SourceNpcFirstLifeDelta.java','SourceNpcFirstLifeLoad.java','SourceNpcFirstLifePlan.java','build_source_npc_first_life.py','reproduce_source_npc_first_life.py']:shutil.copyfile(R/name,inputs/name)
if materialfirstlife or areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceMaterialFirstLife.java','SourceMaterialFirstLifeMethods.java','SourceMaterialFirstLifeDelta.java','SourceMaterialFirstLifeLoad.java','SourceMaterialFirstLifePlan.java','build_source_material_first_life.py','reproduce_source_material_first_life.py']:shutil.copyfile(R/name,inputs/name)
if areaplayerproof or indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceAreaPlayerProof.java','SourceAreaPlayerProofMethods.java','SourceAreaPlayerProofDelta.java','SourceAreaPlayerProofLoad.java','SourceAreaPlayerProofPlan.java','SourceAreaPlayerProofRefs.java','build_source_area_player_proof.py','reproduce_source_area_player_proof.py']:shutil.copyfile(R/name,inputs/name)
if indirectplayerproof or npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceIndirectPlayerProof.java','SourceIndirectPlayerProofMethods.java','SourceIndirectPlayerProofDelta.java','SourceIndirectPlayerProofLoad.java','SourceIndirectPlayerProofPlan.java','build_source_indirect_player_proof.py','reproduce_source_indirect_player_proof.py']:shutil.copyfile(R/name,inputs/name)
if npcfactorytype or npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceNpcFactoryType.java','SourceNpcFactoryTypeMethods.java','SourceNpcFactoryTypeDelta.java','SourceNpcFactoryTypeLoad.java','SourceNpcFactoryTypePlan.java','build_source_npc_factory_type.py','reproduce_source_npc_factory_type.py']:shutil.copyfile(R/name,inputs/name)
if npcfactorymemory or npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceNpcFactoryMemory.java','SourceNpcFactoryMemoryMethods.java','SourceNpcFactoryMemoryDelta.java','SourceNpcFactoryMemoryLoad.java','SourceNpcFactoryMemoryPlan.java','build_source_npc_factory_memory.py','reproduce_source_npc_factory_memory.py']:shutil.copyfile(R/name,inputs/name)
if npctargetfailure or negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceNpcTargetFailure.java','SourceNpcTargetFailureMethods.java','SourceNpcTargetFailureDelta.java','SourceNpcTargetFailureLoad.java','SourceNpcTargetFailurePlan.java','build_source_npc_target_failure.py','reproduce_source_npc_target_failure.py']:shutil.copyfile(R/name,inputs/name)
if negativephase or massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceNegativePhase.java','SourceNegativePhaseMethods.java','SourceNegativePhaseDelta.java','SourceNegativePhaseLoad.java','SourceNegativePhasePlan.java','build_source_negative_phase.py','reproduce_source_negative_phase.py']:shutil.copyfile(R/name,inputs/name)
if massselftarget or nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceMassSelfTarget.java','SourceMassSelfTargetMethods.java','SourceMassSelfTargetDelta.java','SourceMassSelfTargetLoad.java','SourceMassSelfTargetPlan.java','build_source_mass_self_target.py','reproduce_source_mass_self_target.py']:shutil.copyfile(R/name,inputs/name)
if nonetargetrequest or objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceNoneTargetRequest.java','SourceNoneTargetRequestMethods.java','SourceNoneTargetRequestDelta.java','SourceNoneTargetRequestLoad.java','SourceNoneTargetRequestPlan.java','build_source_none_target_request.py','reproduce_source_none_target_request.py']:shutil.copyfile(R/name,inputs/name)
if objecttargetrequest or areaclassselection or damagehealingselection:
 for name in ['NativeSourceObjectTargetRequest.java','SourceObjectTargetRequestMethods.java','SourceObjectTargetRequestDelta.java','SourceObjectTargetRequestLoad.java','SourceObjectTargetRequestPlan.java','build_source_object_target_request.py','reproduce_source_object_target_request.py']:shutil.copyfile(R/name,inputs/name)
if areaclassselection or damagehealingselection:
 for name in ['NativeSourceAreaClassSelection.java','SourceAreaClassSelectionMethods.java','SourceAreaClassSelectionFlow.java','SourceAreaClassSelectionDelta.java','SourceAreaClassSelectionLoad.java','SourceAreaClassSelectionPlan.java','build_source_area_class_selection.py','reproduce_source_area_class_selection.py']:shutil.copyfile(R/name,inputs/name)
if damagehealingselection:
 for name in ['NativeSourceDamageHealingSelection.java','SourceDamageHealingSelectionMethods.java','SourceDamageHealingSelectionDelta.java','SourceDamageHealingSelectionLoad.java','SourceDamageHealingSelectionPlan.java','build_source_damage_healing_selection.py','reproduce_source_damage_healing_selection.py']:shutil.copyfile(R/name,inputs/name)
oldref=pin(index);frozen=pin(run/'previous-program-progress.json')
def freeze_old(v):
 if isinstance(v,dict):
  if v.get('path')==oldref['path'] and v.get('sha256')==oldref['sha256']:v.update(frozen)
  else:
   for x in v.values():freeze_old(x)
 elif isinstance(v,list):
  for x in v:freeze_old(x)
count=int(re.search(r'inverse: (\d+) original methods',Path(build['log']['path']).read_text('utf-8'))[1])
scope=[
 'BATTLE + loaded Source only: preserve the full original unloaded/WORLD doll proc method; create a bounded sibling with damage gfx, skill HP before skill gfx, and additional-damage HP after the skill loop. Preserve original proc calculation and RNG calls byte-for-byte via independent inverse.',
 'Typed SKILL PC damage retains accepted magic digits and true/false flags. Additional damage remains a separate HP event with false/false flags and pre-HP magic-digit publication, matching UnifiedProcExecutor rather than merging into physical HP.',
 'Player HP consumers capture source/target bindings and life before their own ordered pair wait; guard survives even with figures disabled and reaches the existing actual HP precommit guard. Fatal acceptance updates the separate pre-HP visual life token.',
 'Quest/Time SKILL uses existing accepted magic receipts. Extra damage publishes pre-HP magic figures under a detached actual NPC/generation/life/pose proof, preserves the original HP alias and receipt, and carries only the receipt-sealed accepted life token to delayed display (also in exceptional return). Existing bounded HPBAR/display jobs remain; no new workers, TLS, registries or SQL.'
]
remaining=[
 'Doll production remains partial: bind the original physical action before waits/RNG and defer proc side effects until actual physical HP acceptance, including rejected/queued/fatal hits; wire controlled NPC targets that currently return before the doll bridge; audit all unsupported SKILL IDs (including29 slow), kind/immunity, per-proc failure continuation, defense reduction capture, RNG/cost and final doll/cast-gfx receiver proofs. Current typed consumer guards begin too late to qualify the entire physical action.',
 *old['remaining']
]
if accepted:
 scope=[
  'Source BATTLE PC physical events retain the original rolled doll proc in one detached Pending (no Context/Scope/Receipt/Frame/Plan field). Source/target binding/life/pose are captured before this physical consumer ordered-pair wait; the guard survives figures disabled and reaches the existing actual-HP admission.',
  'The original post-submission PC doll callback is suppressed only in loaded Source BATTLE; the other two natural-NPC bridges and unloaded/WORLD behavior retain their original fallback. The existing accepted HP completion claims/clears Pending exactly once before physical digit publication; rejected/abandoned HP events have no proc callback.',
  'Accepted completion revalidates the original pair/life under existing action locks and lifetime admission, using the existing completion/retirement workers. Fatal target SKILL entries are removed before magic RNG (Source executeSkillProc dead-target early return), while original gfx/bonus remain; no proc attack/defense RNG is rerolled. The earlier typed doll commit flags/order/presentation layer is retained byte-for-byte.'
 ]
 remaining=[
  'Doll remains partial: capture the whole physical action before its first wait and original proc RNG; natural Quest/Time accepted-only proc hookup and controlled NPC return branch are still open. Unsupported SKILL IDs including29 slow, per-proc failure continuation, defense reduction capture, all doll/cast-gfx receiver proofs and real DRAINING/STOPPED/Main/pair waits still need implementation or concentrated validation. New PC accepted callback has no gameplay qualification.',
  *[r for r in old['remaining']if not r.startswith('Doll production remains partial:')]
 ]
if npc:
 scope=[
  'Quest/Time physical HP receives its original Aim, already rolled doll proc and NPC stats. A synchronous Scope binds the original caster, actual NPC, encounter generation/life/pose before the existing self action/lifetime admission. Four exact return ports seal accepted provenance after the existing Hub monitor exits; no proc calculation, output or actor lock runs inside the HP monitor.',
  'Natural proc execution waits for the original physical HP alias and death/wave/reward publisher to finish (including exceptional return), then validates the sealed actual receipt before invoking doll effects, before physical figures. Rejected events and replaced/retired actors cannot invoke the proc. This avoids stale original hit/wave structures caused by nested proc damage before the original publisher completes.',
  'The controlled physical body is retained as a bounded sibling; only its original positive-applied-damage/Target.seal/publication guard invokes the existing rolled doll proc before physical figures. Actual master/pet/bag/placement/life proofs and original locks are retained. The old controlled early-return gap now has an accepted proc path.',
  'Natural and controlled SKILL/bonus HP leaves preserve their frozen actor proof across subsequent procs. Controlled SKILL uses existing CombatCompanionDamage.area (pet multiplier and NPC MR); bonus is raw, with no second pet multiplier. Accepted death updates detached visual proofs and the physical figure plan. No queued visual/HP plan retains the synchronous Scope/Context/Receipt; no new SQL, workers, TLS or registry.'
 ]
 remaining=[
  'Doll completion remains open: freeze the entire PC physical action and actual active-doll instance before its first wait/proc RNG; audit full NPC immunity/buff consumers (Source calcNpcMagicDamage includes IMMUNE_TO_HARM), unsupported SKILL IDs including29 slow, per-proc exception continuation, defense reduction, all doll/cast-gfx final receiver paths and DRAINING/STOPPED/Main/pair-wait matrices. Natural and controlled accepted paths are program updates only, no gameplay qualification.',
  *[r for r in old['remaining']if not r.startswith('Doll ')]
 ]
if action:
 scope=[
  'Loaded Source BATTLE physical ingress freezes caster binding/life/pose, runtime inventory identity and actual doll summon tuple before constructing the scene Context and before action-lock waits. Selected actual player/NPC identity is frozen from that Context before ordered pair/self or the original controlled-owner lock path. Original attack body rejects stale role/target before its first original instruction/RNG; unsupported nonproduct targets retain original validation.',
  'The original activeMagicDoll reference gains volatile publication. A frozen summon is identified by object id, backing item id, original immutable type and expiry, so ordinary doll movement records do not replace a summon. Backing item identity is sealed once under actor action locks before doll proc RNG, without an unlocked inventory-map read; replacement inventory/item, unsummon, same-id resummon, changed powers/type and expiry invalidate old proc authority.',
  'The existing doll-powers getter retains its exact original body as fallback. During this physical action its first locked read freezes original summon powers or empty powers; a replacement summon cannot grant fresh powers to the old action. Doll retirement does not reject ordinary physical HP. The original controlled lock acquisition remains separate to avoid taking an extra pair before its existing owner lock set.',
  'Existing PC Pending and synchronous natural/controlled Scope capture only detached Stamp for proc validation; queued Pending never holds Action/Context/Scope/Receipt. The existing actual HP guard, accepted callbacks, workers, DRAINING/STOPPED and close paths remain the parent implementation. Stamp.current uses direct identity/life reads; inventory membership checks happen only in action-locked proc checks.',
  'Pure summon decision scenarios cover absent/new/removed, ordinary movement, different object/backing item/type, same-id resummon expiry and exact expiry boundary. These11 pure cases are not gameplay, shutdown, cursor or native qualification.'
 ]
 remaining=[
  'Doll completion remains open: generic Source SKILL numerical/visual metadata and damage calculation (including29 slow damage/RNG then status), correct target-castgfx before doll aggression, NPC full immunity/buff consumers, per-proc exception continuation, defense reduction, all doll/cast-gfx final receivers and real ingress lookup/pair-wait/DRAINING/STOPPED/Main matrices. Source calcMagicDiceDamage can add original/equipment/talent damage even when base dice are zero; a status-only29 workaround is not sufficient.',
  *[r for r in old['remaining']if not r.startswith('Doll ')]
 ]
if skills:
 scope=[
  'Generic Source doll SKILL no longer requires a BattleSpellCatalog.areaAttackSpell entry. Actual skill_level, attribute, castgfx, duration, damage and dice/count are read as one immutable static row from the already owned readonly/autocommit=false startup transaction. One literal SELECT, no new connection or transaction authority; failed reads never publish a partial replacement. No per-hit SQL.',
  'The Source-only proc loop preserves the parent original ordered body as fallback, runs damage gfx first, each skill calculation/HP submission then target castgfx then doll aggression67 then slow29, and raw additional damage last. Per-proc calculation/output exceptions are counted and later procs continue. Receiver IO cannot roll back HP or stop speed application. Normal spell MP/material/probability/permission routing is not invoked.',
  'Two bounded copies of the original PC/NPC pre-resistance magic methods retain every original instruction except replacing derived skill-level arithmetic with a supplied level from the same frozen metadata row. No mixed row/level on metadata reload. Generic dice, existing original/equipment/critical arithmetic, PC reductions/mirror/class multiplier, FINAL_BURN MP source and DISINTEGRATE chance paths are wired; NPC armor is not spuriously subtracted, pet multiplier precedes MR, and existing Erase Magic MR and freeze guards are read.',
  'Source slow29 normal speed applies a lease, active haste is cancelled, and existing slow is not refreshed. PC, controlled and natural actor routes use the existing speed APIs under the parent action/owner/Hub guards. Duration0/oversize and innate NPC speed semantics remain an explicit gap rather than complete qualification.',
  'A volatile transient proc_spr preference defaults true and is toggled through the existing Source action handler. Target proc effects use Source Chebyshev radius10 and the preference; doll aggression uses native visible screen and the same preference. Complete role reset, actual Source menu reachability and final receiver proofs still require implementation/verification.'
 ]
 remaining=[
  'Doll program is still partial. PRIORITY: bind every proc output receiver/role/life and the original summon at final Frame; reset proc_spr at role replacement and verify actual Source action-menu reachability; sequence queued PC skill HP acceptance/rejection with later procs, death tokens and physical figures. Current per-skill loop submits existing HP events and is not proof of complete nested accepted-order semantics.',
  'Complete Source magic/NPC applicability gaps: NPC immunity/buff consumers (IMMUNE_TO_HARM and SKNPC/PLNPC), Source guardian/talent additions absent from the retained native magic body, signed NPC recovery, and duration0/oversize/innate-haste slow cases. Generic metadata support or a pure29 speed test does not prove all Source SKILL IDs correct. Defense reduction and all actual failure/retry/pair/Main/retirement matrices remain open.',
  'The new static SELECT bindings must be verified in the later real readonly Chinese-MySQL reload. This stage uses fake JDBC only (no real connection); prior actual MySQL receipts do not qualify this reader or candidate.',
  *[r for r in old['remaining']if not r.startswith('Doll ')]
 ]
if output:
 scope=[
  'A synchronous View snapshot freezes each original receiver binding once. Three explicit target-gfx/doll-aggression/speed loops carry detached Output/Proof/Receiver records through the existing owned NativeLightAction publication scope. HP/display Plans retain no View/Context/Scope/Receipt. One existing Context carrier supports final-frame proof; no new workers, TLS, registry, scheduler or admission authority.',
  'The actual nativeStaticOriginalFrame rejects stale receiver role/disconnection, STOPPED/closed Hub, target role/life and (for cosmetic proc graphics) original caster/summon/type/expiry, proc_spr and current visible range. Final proof reads existing identities/timers and takes no action/Hub/status/inventory locks; failed output cannot undo accepted HP/status or prevent original structural completion.',
  'Accepted S_SPEED results are separate from cosmetic summon authority. A detached target/status proof checks actual PC or original controlled/natural NPC life/owner/scene and exact accepted haste/slow expiries, permits movement within the original scene, and does not itself depend on a surviving caster/doll or proc_spr. The original publication scope still applies its producer/peer binding gate before running the output task; source-retirement delivery is not established by this detached check. Eight existing expiry fields gain volatile visibility; timer/state arithmetic and all original methods remain unchanged. Old speed packets are rejected after dispel/replacement/expiry, and the target self duration is refreshed on the final local packet bytes.',
  'proc_spr resets true at the original NativePlayerCommit.selected boundary immediately after the existing damage preference reset, under the original actor action lock. Source/wire/RNG/cost and ordinary role assignment bodies survive the independent inverse.',
  'Source queue contract correction: DamageCommit.enqueue returns nonblocking admission, DamageCommitQueue schedules its drainer, and afterAccepted runs only at actual HP acceptance. Source itself continues nested SKILL graphics/next procs without waiting for each child event. No synchronous wait or per-child chain is added; preserve and later validate FIFO/admission/fatality/final-output behavior.'
 ]
 remaining=[
  'Doll full runtime qualification remains open: exercise actual final receiver/role/scene/life/summon waits, proc preference changes/reset/menu, accepted speed movement/replacement/expiry/recall, nonblocking nested child FIFO/admission/rejection/fatality, output IO, bounded scope cleanup and DRAINING/STOPPED/Main. This candidate has zero new gameplay cases; program and pure timer checks do not qualify all29 or all doll paths.',
  *[r for r in old['remaining']if not r.startswith('Doll ')]
 ]
if slow:
 scope=[
  'Source doll29 uses the existing original status/pet/Hub monitors and timer fields. Normal gains slow; recognized haste is removed without applying slow; already slow and innate/unrecognized speed1 remain unchanged. This controlled-pet intrinsic-speed correction does not establish natural-NPC template-speed parity.',
  'Zero or nonpositive int(seconds*1000) uses a permanent sentinel in the original slow expiry scalar; positive wrapped milliseconds follows the original first/whole-second timer tick. Long finite states retain their real expiry, and Source writeH uses low16 duration bits rather than saturation. A bounded prefix of the original synchronized PC speed query handles the permanent sentinel without overflow and long wire durations; original cancellation/expiry/role cleanup bodies remain unchanged.',
  'Four original timer owners gain one volatile identity-only epoch each; the existing detached Speed proof captures one final epoch and checks it after original timer comparisons. A fresh accepted doll transition replaces the epoch before changing timer fields. Clear/reapply of an identical permanent expiry rejects the old output proof; no-op already slow does not manufacture a new lease. These fields hold only new Object identities, never a session, scope, receipt or registry.',
  'The original actual Frame gate retains all parent receiver/role/life/scene/status checks, and refreshes only local packet duration bytes. Live wrapped-zero and permanent-zero are distinguished from expired status; no action/Hub/status lock, new worker, timer, TLS, SQL connection or persistence authority is introduced.',
  'Five exact Source files are hash-pinned and copied as reference-only frozen inputs. Independent inverse, JVM no-init loading, frozen byte-identical rebuild and25 pure actual-status/clear/expiry/epoch scenarios are program evidence only, with zero new gameplay or database cases.'
 ]
 remaining=[
  'Doll completion remains open: full NPC immunity/SKNPC/PLNPC, guardian and talent magic contributions, signed NPC healing, defense-reduction semantics, natural-NPC template intrinsic movement speed, actual final Frame/role/scene/life/summon waits, original nonblocking child FIFO/admission/fatality, IO/cleanup and DRAINING/STOPPED/Main matrices. Zero/long doll29 and controlled innate haste now have program changes, not gameplay qualification.',
  *[r for r in old['remaining']if not r.startswith('Doll ')]
 ]
if defense:
 scope=[
  'Preserve the existing NativePlayerCommit defense Calc sum/RNG and once-only accepted/equipment publication. Six explicit class ports add Context original-defender-summon snapshots, a delegated immutable DefenseVisual constructor, actual accepted death-life sealing, frozen recipient selection and final Frame checks. Original ordinary HP is never cancelled by an expired cosmetic proof; no second defense roll or calculator is introduced.',
  'For existing Source BATTLE Context paths, original room participant bindings and summon stamps are frozen at the Context boundary before later action-pair waits. Child Contexts reuse the same snapshot. Original drawDefense still takes its original doll monitor and seals backing item identity only while the existing victim action lock is held. An absent/retired/replaced summon contributes no fresh powers to that old action. Passive doll stat consumers and no-Context/unproven-lock producers are not qualified by this change.',
  'Existing DefenseVisual objects carry only detached original-victim role/life/pose/inventory/summon and immutable audience identities, no Context/Scope/Receipt/Frame. Source calc failures or expiry between draw and visual construction are explicitly rejected rather than falling back to a fresh summon. Original WORLD/unloaded and uncovered no-Context visual paths keep their original fallback.',
  'At the actual accepted-state boundary fatal life is sealed on the original proof before any completion wait. Accepted publication retains the original Frame and equipment tail, applies proc_spr and radius10 victim GFX versus native-screen original-doll aggression67, then revalidates actual receiver role/life/scene/summon at nativeStaticOriginalFrame. Per-output dynamic Frame carrier is always restored; final proofs read existing fields without actor/Hub/status/inventory locks. No new workers, TLS, timer, registry or SQL.',
  'Thirty-five actual Source methods now include defense Calc, accepted zero-delta commit, reductionOnly and defense GFX. Independent original-method inverse,31 no-init classes, frozen byte-identical replay and10 pure actual audience predicate cases are program evidence only, not real zero/fatal/shutdown or cursor qualification.'
 ]
 remaining=[
  'Doll completion remains open: defense producers without a proven original Context/action lock and passive stat consumers still need pre-wait summon coverage; exercise actual defense zero-delta/fatal/receiver/retirement/Main matrices. Full NPC immunity/SKNPC/PLNPC, guardian/talent, signed NPC healing and natural-NPC intrinsic speed remain missing, plus all other Source/skill/producer/AI/control work and concentrated same-SHA qualification.',
  *[r for r in old['remaining']if not r.startswith('Doll ')]
 ]
if passive:
 scope=[
  'The original NativeSourceDollAction powers getter retains its exact root-physical Action branch. Only its no-Action fallback now resolves an already captured loaded-BATTLE Context actor through one original-summon Choice. All ClientSession stat/RNG/cost/HP/queue class bytes remain identical to the parent, and original unlocked/no-Context/WORLD/unloaded reads retain their original getter fallback.',
  'Each original defense Snapshot owns one final identity-keyed unmodifiable Choice map, bounded by its original captured participant map. Child Contexts share that Snapshot. A Choice lazily freezes the exact original immutable Source powers or empty powers at its first read under the original actor action lock; later getters cannot mix a replacement doll into that action. Actor role/life/pose and inventory identity remain revalidated even for a cached result.',
  'After the first selection, ordinary scalar calculations retain their sealed powers when the cosmetic summon retires; a stale original before first read freezes empty instead. No new action/Hub/status/inventory locks, RNG, SQL, workers, timers, TLS or static actor registries are added. Choices and power maps are owned by the original Context Snapshot. This stage adds no Choice/Snapshot/Context field to doll HP/defense/output proofs. Other pre-existing context-retaining event wrappers keep their parent bounds and still require the unqualified close matrix.',
  'Independent exact archive inverse proves only the root getter fallback and Snapshot constructor ports changed.37 classes load without initialization; frozen replay is byte identical.10 pure cases run the actual Choice resolver with actual immutable Source power rows, covering sealed/empty/replaced/invalid-actor decisions. These are program cases, not native role/scene/closing qualification.',
  'A same-SHA consumer receipt lists31 common getter calls in30 static methods and22 direct HP caller methods, plus exact native Fatality null-argument excerpts. Direct edges do not prove upstream Context/lock coverage. Source actual attacker kind must be resolved separately: actual effect NPC, owner, genuine null and an opaque Java null argument are not interchangeable.'
 ]
 remaining=[
  'Doll completion remains open: prove or implement original Context/lock coverage for every incoming producer and passive getter upstream; current passive change covers captured Context plus original actor lock only. Resolve actual attacker kind and null-source defense behavior for fatality/bot/periodic/AI/cube without borrowing a player or ambient identity. Full NPC immunity/SKNPC/PLNPC, guardian/talent, signed NPC healing and natural intrinsic speed, all other Source/58-material/control/AI work, and same-SHA runtime/receiver/Main matrices remain incomplete.',
  *[r for r in old['remaining']if not r.startswith('Doll ')]
 ]
if cube:
 scope=[
  'Ignition205 and Harmony220 actual native Cube objects now carry immutable Entity provenance: original cube object identity/id, effect NPC80149/80152, original owning engine/clear generation, original Room scene and non-authoritative creator binding. This does not substitute the player creator as the effect NPC or yet change the HP event attacker field.',
  'At the existing synchronized GameHub battle placement and battle/WORLD scan call sites, temporary creator/scan carriers are installed and always restored in finally. For a bound BATTLE engine, original PC binding/life and actual Quest/Time actor/owner/generation/life are captured under the original Hub before waiting on the original engine. No actor lock or new Hub/engine monitor is acquired. Native BOT/ambiguous/unbound identities are not manufactured into Source Monster/PC proofs.',
  'Every original actual StatusLease construction delegates its original constructor then saves its Entity and original target metadata. Periodic and expiry events carry that exact status object/Lease; only just-installed APPLIED events may read the just-published original status map under its existing engine monitor. A same-id renewal cannot silently adopt an old periodic event. Clear replaces only an owned metadata epoch in addition to the unchanged original clear body.',
  'The target eight-second lease retains the actual effect object after the cube leaves visibility/expires at twenty seconds, matching the retained Source _effect reference. Metadata consists of original instances and immutable records, no Context/Scope/Receipt/Frame, static actor registry, worker, timer, TLS, RNG, SQL or new lock. Original field schema and all ordinary clock/relation/cost/HP instructions survive independent inverse; WORLD compatibility behavior retains the original engine call.',
  'This is necessary provenance foundation only. PC/NPC HP source selection, raw false/true PC flags, no ordinary magic digit plan, immunity/kinds, original caster relation, admission/actual commit and final receiver must still consume the Entity/Lease/Target data. Existing HP source bodies remain byte-identical and are NOT represented as fixed by this metadata stage.15 pure actual-engine cases plus22 no-init contract loads and byte-identical replay are not BATTLE, role, Main or Source gameplay qualification.'
 ]
 remaining=[
  'Cube HP consumers remain open and are the next priority: use actual Entity/Lease/Target data for PC raw false/true damage and natural NPC source, keep effect NPC distinct from creator/Java null, bind the original eight-second target role/life before waits and carry it to actual HP and final output, preserve Source kind/immunity/reaction suppression and no ordinary magic figures. Also resolve Source/native scan cadence and creator-retirement behavior from actual consumers; metadata alone does not complete Cube semantics.',
  *old['remaining']
 ]
if cubeplayer:
 scope=[
  'Loaded BATTLE PC Ignition/Harmony damage now uses the original Entity/Lease/Target provenance. The existing HP Event carries Ref kind5 with the actual cube object id and an immutable Effect(Entity, application lifetime) descriptor, no player master/caster. The player creator is not HP authority or kill source; actor liveness is separate from target HP or eight-second status. Ref1..4 and original WORLD/unloaded HP ingress bodies remain intact.',
  'PC damage uses Source raw10/25 false/true through the original typed HP Calc/defense/equipment/Avatar/queue path, with an explicit empty digit plan. No ordinary magic MR/elemental/critical/material/MP/cast probability is added. Ignition checks original protected-zero statuses and logically publishes its pre-HP ACTION_Damage, obeying self/visibility/invisibility at the actual receiver; Harmony has no added action or ordinary magic figures.',
  'The original target role/actor/life/scene is captured at lease creation and checked before the target self-pair wait and under the original application admission. Exact active status is checked under existing Hub/engine before enqueue; after enqueue the detached HP proof checks Entity epoch/scene and target role/life, allowing movement and causal DRAINING, without cancelling an already submitted HP event merely because the status expires or cube leaves visibility. Engine clear, STOPPED, stale role/life and closed Hub reject it.',
  'The existing actual HP precommit gate validates effect Ref/typed empty Plan/target binding/false-true flags independently of display settings. Actual fatal acceptance seals a separate accepted-life token before completion waits. The existing accepted Frame and nativeStaticOriginalFrame recheck the target and frozen receiver roles; final gates take no actor/Hub/status/inventory locks. Queued Proof/Effect has no Context/Scope/Receipt/Frame/Plan field; original bounded HP/publication machinery remains, no new worker/timer/TLS/registry/SQL.',
  'Under the original synchronized engine advance, only Source PC leases whose original role/life/scene no longer matches the frozen scan are retired, so same-id replacement cannot inherit or block a fresh lease. Other target kinds retain parent behavior. Native effect-kind feedback/reflection/attacker reaction is explicitly suppressed, matching the Source raw counter flag and effect NPC behavior; ordinary defense reduction remains the existing calculator.',
  'Independent inverse2668,35 no-init contract classes, frozen byte-identical replay and22 pure flag/reference/strict identity/unbound effect adapter cases are program evidence only. No actual PC queue/Frame/role/Main gameplay case has run. NPC HP source consumers, MP/status/aura role paths, exact network ordering and the full Source cube lifecycle still require work and concentrated qualification.'
 ]
 remaining=[
  'Cube NPC/status/MP/aura consumers remain open: wire actual effect Entity and original Quest/Time actor to natural NPC HP/reaction/HPBAR without substituting a player creator, handle non-player dispatch when the creator retires, audit Source target kinds and source/native scan cadence. PC program wiring is new and still needs actual same-SHA HP queue/zero/fatal/role/scene/life/receiver/IO/DRAINING/STOPPED/Main coverage; logical pre-HP reaction order is not proof of actual wire order.',
  *[r for r in old['remaining']if not r.startswith('Cube HP consumers remain open')]
 ]
if cubencplease:
 scope=[
  'At the existing Source PC retirement port already entered by the original synchronized advance, natural MONSTER leases are retired against the immutable original Hub scan. Require the existing Hub and engine monitors, loaded Source and bound Scene/engine epoch. No new lock, actor lookup, life token creation, carrier, worker, timer, TLS, RNG, SQL or output authority.',
  'Retain only the original actual status object/cube reference, original effect engine/epoch, Quest or Time actual actor/owner/life/encounter generation and exact target id. Missing, ambiguous, same-id replacement, new life, owner or generation cannot inherit or block a fresh status. Unchanged actors retain their status while moving out of cube radius; no dependency on creator presence, active cube visibility, or a newly selected same-id actor is added.',
  'Only original MONSTER map entries change. PC/BOT entries, WORLD/unbound compatibility engines, original expiry/cadence/RNG/relation/HP/MP/queue/output bodies retain their parent behavior. Already-created events still carry their original lease; this map retirement is not an actual NPC HP guard and does not qualify old queued callbacks. NPC effect-source HP, creator-independent non-player dispatch, MP/status/aura consumers and final receivers remain open.',
  'Independent inverse30 original methods,36 no-initialization candidate classes,27 actual engine-map/detached identity scenarios and frozen byte-identical replay pass. Zero real PC/Hub/socket/BATTLE gameplay cases or database connections; these are program checks only.',
 ]
 remaining=[
  'Cube NPC next: wire actual effect Entity and the original Quest/Time actor to natural NPC HP/reaction/HPBAR at the real commit boundary; remove creator-online dependence from non-player dispatch. Stale MONSTER map leases now retire, but already emitted numeric-id NPC callbacks remain unguarded by this stage. Audit MP/status/aura roles, accepted/final receivers, actual Source/native cadence and Source Monster applicability; then concentrated same-SHA queue/role/life/IO/DRAINING/STOPPED/Main validation.',
  *old['remaining']
 ]
if cubenpc:
 scope=[
  'The two actual original periodic clock loops reach typed natural-MONSTER HP dispatch before the old creator-null branches. The two original ClientSession non-player HP ingress methods also receive handled Source BATTLE prefixes; original WORLD/unloaded/BOT fallback bodies remain byte-retained. The actual effect Cube/Entity/Lease/Target is matched by identity; a copied header or renewed same-id lease cannot gain authority.',
  'Quest/Time raw10/25 is committed to the original actor/owner/generation/life under original Hub/encounter monitors. Two bounded synchronized Hub siblings preserve original mode, death/wave/spawn/resolution code, replacing only the player-roster/lane authority with the already admitted actual effect event. Quest original creator force is captured once at original Cube construction under the existing Hub/engine context; same-lane eligibility remains even after creator retirement and never uses a replacement role or the target lane as newly granted authority.',
  'Source NPC master attribution is separate from HP admission. The effect Ref kind5 has actual Cube id/Entity and original target, with the frozen original master binding retained for attribution. Raw HP does not require an online creator. HPBAR and lawful reward only use the original still-matching creator role; no new role may inherit it. The Source hate/family-link/AI side-effect paths remain incomplete and are not claimed by this native encounter bridge.',
  'Exact original installed lease and Ignition native freeze/earth guard are checked before admission. Once admitted, HP remains independent of later status expiry, cube visibility and creator retirement while effect epoch/scene and original target lifetime still hold. Harmony avoids an extra ordinary earth guard via a typed original ingress/target check; all other earth behavior remains original. No ordinary magic MR/elemental/critical/material/MP/probability/digit plan is introduced.',
  'Ignition publishes the original Source hurt action before HP, with the existing selected-source GFX suppression catalog and radius10. No HP/Hub/engine/admission lock is held over this IO. Three detached static copies of original accepted Quest/Time/wave presentation retain death, poison removal, original spawn/boundary/announcement, reward and completion structure. Their null PC parameter is only the old stack carrier: every receiver field/call is replaced by typed effect Commit ports, with no borrowed driver/caster. Per-receiver IO failures cannot undo HP or skip the retained structural tail.',
  'Original master HPBAR honors the existing Source HPBAR setting and visibility, uses bounded original actor cache and weak viewer references, and terminal HP hides tracked prior viewers. Large ratio arithmetic is bounded; stale final percent is rejected. Original accepted actor life and newly spawned actual actor lives are sealed under original ownership before output. Existing native Frame rejects stale effect/target/receiver/spawn identities via a dynamic Context carrier restored in finally; detached Commit/Effect/Output holds no Context/Scope/Receipt/Frame/Plan.',
  'Synchronous in-flight work increments the existing application pending counter under original admission and decrements exactly once in finally. It reuses original executeCommitted causal ownership for HP and structural completion, so original DRAINING may finish admitted work and STOPPED rejects mutation/output. No new queue, worker, timer, TLS or static actor registry. This preserves the native direct encounter path but does NOT prove the Source asynchronous NPC FIFO/admission model; that remains a required program/fidelity gap.',
  'Independent inverse3508 original methods/siblings,41 no-initialization candidate classes,37 actual engine-event/detached metadata/faction/raw-gate/packet contracts and frozen byte-identical replay pass. Forty-five bounded Source methods plus frozen creation-side inputs establish effect/master and HPBAR semantics. Zero actual game/PC/Hub/socket/BATTLE/DB cases; no full lifecycle, cursor, Source parity or release qualification is claimed.'
 ]
 remaining=[
  'Cube completion remains open: reconcile native synchronous natural-NPC HP with Source nonblocking per-target FIFO/admission/accepted model; wire original master hate/family-link/NPC AI without making creator availability HP authority. Audit Source monster applicability/hidden states, full immunity and the actual selected-source hit-reaction table reader. Implement MP/status/aura role consumers and actual Source/native scan cadence, then concentrated same-SHA zero/fatal/old-event/same-id/role/life/spawn/HPBAR/master/IO/DRAINING/STOPPED/Main coverage. Current Source PC and natural-NPC HP program bridges are not full Cube qualification.',
  *[r for r in old['remaining']if not r.startswith(('Cube NPC next:','Cube NPC/status/MP/aura consumers remain open','Cube HP consumers remain open'))]
 ]
if cubenpcqueue:
 scope=[
  'Cube NPC production preserves the parent exact effect/lease/actor preparation and pre-HP hurt output, then nonblockingly enqueues a typed Ticket in the original actual actor BattleTimedStatus-owned CombatNpcDamageQueue. No numerical-id reselection, substitute player contribution, new queue, executor, timer, retry thread, TLS or static registry. The existing sealed CombatWorldCommit.Plan permits list gains exactly one Ticket kind; all prior allowed kinds remain byte-retained.',
  'The actual original encounter proc drainer handles cube Tickets before its old proc-plan assertion and reaches the same original Queue.complete in the original try/finally. Old proc plans retain their full body and ordering; cube/proc events share one actual target FIFO, original batch256, worker dispatch, deadline/rejection retries, drop and Main drain/stop ownership. The producer removes its old inline executeCommitted call and manual pending credit; queued delivery never runs or waits for HP on the producer.',
  'An event retains only the detached original Commit and exact original queue. Before actual HP, the typed drainer verifies its original active event/queue, effect id, empty player/contributor fields, raw amount and original scene, then sets a volatile claimed flag for the original HP guard. Queue-preparation and real-commit authority are separate; an unclaimed prepared event cannot invoke the Hub HP siblings. Claimed state is cleared and the event marker is settled in finally; original target/effect/accepted/final receiver guards remain parent behavior.',
  'Native NPC admission no longer waits for application admission while holding the queue. Existing PC/NPC damage capacity checks use one shared CAS reservation on the existing application pending counter and original GLOBAL constant. Failed PC deque publication releases its new reservation. NPC registration precedes the STOPPED publication fence, and original deadline, lane, queued-drop and slot accounting remains. Seven original non-damage lifecycle credit writer methods remain byte-identical; they are not converted or represented as a damage-only Source capacity domain.',
  'The original queue clear/retry-exhaustion/terminal-stop path marks only pending cube Tickets cancelled under the original queue monitor, with no IO/HP/Hub/actor/admission acquisition. Active events retain original completion ownership. A reward-output exception is counted through the original display failure metric and cannot skip the retained accepted death/wave/completion tail. No queued Context/Scope/Receipt/Frame/Runnable is introduced.',
  'Actual Source processPendingEvents releases local/global capacity at dequeue before commitInternal, not after HP completion. The retained native claim release and queued-only target credit therefore match this timing. All direct NPC damage producers still need to join the same ordering domain, and the native application counter also includes AI lifecycle work; full Source damage-capacity configuration/domain parity remains incomplete.',
  'Independent inverse116 original methods,52 no-initialization candidate classes,33 pure CAS/concurrent primitive capacity/failed publication/actual unbound FIFO/mixed old drainer/clear contracts and frozen byte-identical replay pass. Fifty bounded Source methods are frozen. Pure unbound queue/drainer cases create no PC/Hub/socket/SQL/BATTLE fixture or HP authority. Zero actual gameplay/database cases, no Main/cursor/release qualification.'
 ]
 remaining=[
  'Cube/NPC FIFO next: unify other direct natural-NPC HP producers (physical/doll/raw/poison/periodic/healing/AI) with actual accepted plans and the same target FIFO, retaining original source identity, RNG/cost, once-only accepted procs and final receivers. Cube plus old encounter proc now share the native queue; this is not all-NPC Source ordering. Reconcile damage-only Source capacity/configuration with the native application counter containing seven unchanged AI lifecycle-credit writers, while preserving existing limits and data safety. Source dequeue credit release is now pinned and matches the retained native claim timing.',
  'Cube MP/status/aura roles, original master hate/family-link/AI, full monster applicability/immunity/hidden-state and selected-source hit-reaction reader, source/native scan cadence and actual same-SHA admission/retry/clear/zero/fatal/old-event/role/life/receiver/IO/DRAINING/STOPPED/Main matrix remain open.',
  *[r for r in old['remaining']if not r.startswith(('Cube completion remains open:','Cube NPC next:','Cube NPC/status/MP/aura consumers remain open','Cube HP consumers remain open'))]
 ]
if capacity:
 scope=[
  'The original NativePlayerLifetime gains one final zero-initialized AtomicLong damage ledger immediately after its original super construction, before dependent owned helpers may see it. This is one scalar per original application, with no session/actor/queue registry, worker, timer, TLS, new connection or persistence authority. Original AI/in-flight work pending and seven non-damage lifecycle credit writer methods remain byte-retained.',
  'The existing PC queue reserves the damage ledger using the parent CAS helper and the unchanged original Source GLOBAL constant/property keys. Failed original deque publication rolls back that same damage reservation; successfully published PC events separately increment the original work counter once. NPC admission uses the same original-application damage ledger, original target FIFO/lane/deadline registration and terminal publication fence, with one original work credit per successful queue publication.',
  'At each exact original PC/NPC dequeue and queued-drop accounting boundary, the matching damage credit is released independently of the unchanged work-counter release. Source processPendingEvents releases capacity before commitInternal; queued-only target semantics, maximum values and configuration clamps stay original. The ledger release rejects mismatched/negative credits instead of silently underflowing. Queued HP order, source/target proofs, claim authority, RNG/cost and final output remain the parent archive.',
  'Original idle and terminated methods receive reject-only scalar fences while damage reservations remain. This covers the admission/publication window before work/queue registration and prevents falsely reporting application termination with outstanding damage credits. Original Main drain/stop, worker/retry shutdown and AI accounting bodies remain independently inverted; no live close/stop fixture is claimed.',
  'The previous mixed-counter NPC offer body remains only as a zero-call predecessor alias; the live offer delegates to the damage-only owned helper. All AI/calculator/HP/target/role/SQL classes outside the four explicit existing entries are byte-identical. Source lineage.damage.maxTargetPending/maxGlobalPending and their native bounds are retained in unchanged outer configuration classes.',
  'Independent inverse70 original methods,53 no-initialization actual candidate classes,16 pure damage-domain/capacity/dequeue/drop/failed-publication/concurrency scalar contracts and33 inherited actual unbound queue/drainer contracts on this same SHA pass, with frozen byte-identical replay. No life/PC/Hub/socket/SQL/BATTLE fixture or real HP/Main cases. The queued PC/NPC domain is program-wired; all direct NPC producers, actual owner shutdown/configuration and whole Source qualification remain open.'
 ]
 remaining=[
  'NPC FIFO next: route remaining direct physical/doll/raw/poison/periodic/healing/AI HP producers into the original actual target FIFO with detached accepted plans, once-only original procs/rewards/figures, original RNG/cost/target permissions and final receiver proofs. Cube and old proc already share the queue; direct producers still bypass it. Queued PC/NPC damage-only capacity is now program-wired separately from AI work, while real bound-owner capacity/global-property/admission/failure/drop/DRAINING/STOPPED/Main validation remains deferred.',
  *[r for r in old['remaining']if not r.startswith('Cube/NPC FIFO next:')]
 ]
if poisonnpc:
 scope=[
  'Natural NPC installed poison captures the original room, actual actor/encounter, life and generation once in its original Lease constructor under existing Hub/encounter monitors. No numeric-id recovery or substitute caster grants HP authority. Original installation faction, target rules, cost/RNG and timer poll remain unchanged.',
  'Quest/Time poison HP no longer executes inline in pollPoisons. Each due tick publishes a detached typed poison Ticket to the original actor status-owned queue and shared old proc drainer, alongside cube and original proc plans. Existing PC/NPC damage-only capacity, work accounting, deadlines, retries, dequeue release, drop, DRAINING/STOPPED and Main remain original. A third typed route executes inside the existing try/finally with the original once-only Queue.complete.',
  'Actual poison HP requires that exact queue active event, admitted/claimed plan and original target life/generation/room membership. The original source is the installed Lease binding; HP does not call its live-caster/source-life predicate, and status expiry/replacement after admission cannot revoke HP. Target death/replacement, original room retirement, STOPPED and closed Hub still reject. No queued Context/Scope/Receipt/Frame/Runnable or new registry/queue/worker/timer/TLS/SQL.',
  'Two bounded original Hub HP siblings switch only the actual encounter HP call to the original poison=true applyDamage body on the same actual actor/lane. Three bounded accepted publication siblings preserve original corpse/wave/spawn/boundary/AI-arm/cancel/lawful/end structure. Original attacker binding separately protects HPBAR/reward, independent of its later death; final output seals accepted fatal target life and original receiver bindings. Status-expiry palette output has a separate exact current-lease proof and cannot withdraw queued HP.',
  'Independent inverse3804 original methods plus five accepted siblings,62 no-init candidate classes,15 actual unbound mixed poison/cube/old-proc FIFO/drainer/drop cases plus same-SHA33 inherited queue and16 scalar damage-capacity cases, and frozen byte-identical replay pass. Fifty-seven fixed Source methods are frozen;20 readonly SQL consumer classes remain byte-identical. Zero bound gameplay/HP/Main/database cases; full Source/58-material/release qualification remains false.'
 ]
 remaining=[
  'NPC FIFO next: route remaining direct physical/doll/raw/periodic/healing/AI HP producers with detached original accepted plans into this same target FIFO, retaining original RNG/cost/permissions, figures/procs/reward and final receivers. Natural installed poison, cube and old proc are program-wired; all NPC ordering, Source hate/family AI/hidden/immunity math, PC poison/cross-role completion and real queue/fatality/DRAINING/STOPPED/Main remain unqualified. Continue full Source controls/58 material skills and isolated formal composition; real readonly Chinese-MySQL/cursor acceptance remains deferred until program completion.',
  *[r for r in old['remaining']if not r.startswith('NPC FIFO next:')]
 ]
if physicalnpc:
 scope=[
  'Loaded Source BATTLE natural Quest/Time physical HP and its scoped doll SKILL/bonus HP join the same original target status-owned FIFO and existing cube/poison/proc drainer. The two original physical callers discard their integer return; submission now returns no fabricated synchronous acceptance. Original attack calculation, proc RNG/cost, critical and Kiringku ingress and all other original instructions are independently retained.',
  'Detached Origin captures original player binding/life/pose, original room, actual NPC/encounter/generation/life/pose, frozen original lane and original summon Stamp before the source ordered wait. Commit carries only frozen values, already rolled AttackProc/NPC stats, original audience bindings and detached visual plans, never Context/Scope/Receipt/Runnable. Base physical HP does not depend on a still-active doll. HP requires actual queue active event, typed claimed plan, original source/target and roster/scene. Original OPEN roots, causal DRAINING child admission, STOPPED, worker/retry/deadline/drop/Main and damage-only ledger remain.',
  'Five bounded original HP/publication siblings retain original native corpse/death/wave/spawn/boundary/AI-arm/cancel/lawful/end logic. After accepted structural publication, the original rolled doll proc executes once before physical figures. A fresh synchronous Context ctor uses original frozen audience/bindings and exact accepted NPC; only stack carrier, never retained by the HP ticket. Original summon stamp/powers are restored through typed wrapper ports; role/summon/inventory replacement cannot give old proc new authority. Original source actor lock protects proc and item checks.',
  'Nested doll SKILL and bonus HP append to the common tail without inline recursion or waiting; rejected/fatal child HP follows original target life guards. Bonus keeps pre-HP figures, SKILL keeps accepted figures, and physical figures follow the accepted rolled proc. Accepted/fatal tokens update detached NPC figures. A child carries only the original parent figure Plan, and may extend its accepted life proof only across the exact same actor/owner/generation/predecessor-life/pose; unrelated replacement cannot inherit it. Final receiver/role/scene/spawn proofs are detached. Structural output runs after releasing source/admission/HP locks; rolled doll output retains its original owned publication scope, avoiding a source-to-peer action-lock edge. No new queue/worker/timer/TLS/registry/SQL.',
  'Independent inverse3501 original methods,76 no-init candidate classes,30 pure original-causal-figure-life/mixed-four-kind/nested-tail FIFO/drainer/drop cases plus same-SHA33 queue,16 damage-capacity and15 poison contracts, and frozen byte-identical replay pass. Fifty-eight bounded actual Source methods pinned;20 readonly SQL consumer classes retained. Zero actual bound HP/game/Main/SQL cases. Full Source accepted drains/durability/talent/math/AI and other NPC producers remain open.'
 ]
 remaining=[
  'NPC FIFO next: route generic player-NPC magic/raw, periodic/healing, controlled/AI HP producers into the same target FIFO with detached original accepted plans; audit all callers that depend on immediate damage returns before changing them. Natural player physical and its scoped doll SKILL/bonus, installed poison, cube and old proc are program-wired. Complete Source physical accepted MP/HP drains/durability/talent and NPC math/hate/family-AI/hidden/immunity, PC poison, actual source/target/receiver role/fatality/IO/DRAINING/STOPPED/Main. Continue all58 materials/Source controls and isolated formal composition; real readonly Chinese-MySQL and cursor acceptance remains deferred until program completion.',
  *[r for r in old['remaining']if not r.startswith('NPC FIFO next:')]
 ]
if magicnpc:
 scope=[
  'The two original Source BATTLE area cast roots enter with a frozen original Context before their existing waits. A final Context snapshot carries original caster role/life/pose, actual natural NPC/owner/life/generation/pose and original audience bindings/recipients. Child Contexts inherit the same snapshot instead of refreshing authority after waits. Original primary/Ctrl/area relation/kind/range/cost/RNG and full nonproduct/WORLD bodies remain independently inverted.',
  'Four original Quest/Time area magic HP consumers discard their result. They now submit mode2 magic through the existing original physical/proc/cube/poison target FIFO and worker/drainer. No synchronous acceptance is invented. Exact original erase-map/target-entry operands are captured as immutable After(Lease) in the actual existing Commit; no Context/Scope/Receipt/Frame/Runnable or whole snapshot is retained by this metadata. Temporary producer admission is restored in finally.',
  'The original admitted HP target/source/life/scene/claim/roster and damage-only capacity/OPEN/causal-DRAINING/STOPPED/Main guards remain. Original target/receiver/audience bindings are used through admission and final output; same-id actor, role or life replacement cannot acquire fresh proof. The same original accepted callback publishes magic figures logically before target mutation and consumes only the original ERASE_MAGIC lease, under exact accepted actor/generation/life and original Hub/encounter monitors. Old producer erase calls are suppressed only for loaded Source BATTLE area paths; unloaded/WORLD retain original calls.',
  'Fixed Source runSkill gates positive-damage mutations on HP acceptance but queues nonpositive target mutations independently. The explicit nonpositive branch performs only exact old-lease consumption under original actor/admission/Hub/encounter guards and original Hub mode/active checks; it creates no HP event or figures and does not add signed NPC healing. Original erase-status generation semantics preserve recast/cancel/clear/expiry ABA protection. No new worker/queue/timer/TLS/registry/connection/SQL.',
  'Independent inverse2552 original methods,81 no-initialization candidate classes,16 detached NPC identity/actual original erase-status once/recast/cancel/clear/expiry cases plus same-SHA30 physical figure/FIFO,33 queue,16 capacity and15 poison contracts and byte-identical frozen rebuild pass.64 actual fixed Source methods pinned;20 readonly SQL classes remain byte-identical. Zero bound HP/game/Main/SQL cases. This is area-path program wiring, not complete generic Source magic/58-material/release qualification.'
 ]
 remaining=[
  'NPC magic next: wire single-target accepted life drain/confusion/thunder and directional/freeze/shock accepted target plans without retaining calculator/Context or returning fabricated HP acceptance. The original single-target return controls life drain; freezing uses a precomputed alive estimate that must be replaced by actual accepted outcome. Preserve exact Source ERASE_MAGIC/positive-vs-nonpositive semantics, source/target pre-wait identity and source/receiver final lifetimes. The two area producers now share FIFO; remaining generic/raw/periodic/healing/controlled/AI HP producers and full Source physics/math/hate/family AI plus all58 material/control/formal-composition and later same-SHA/readonly-Chinese-MySQL/cursor/Main validation remain open.',
  *[r for r in old['remaining']if not r.startswith('NPC FIFO next:')]
 ]
if singlenpc:
 scope=[
  'Source BATTLE natural Quest/Time ordinary single-target attack, life drain, confusion and thunder leaves use the existing original target FIFO and the common frozen pre-wait source/NPC/audience snapshot. One reject-only guard precedes the original single-cast body. The two handled-only NPC prefixes skip the complete old HP/after-hit tail only for these supported paths; unloaded/WORLD and unsupported legacy Mind Break/Bone Break/Arm Break keep their full original bodies because their pre-HP debits/statuses still need separate accepted plans.',
  'The detached Single metadata stores immutable spell/erase lease and calculated confusion duration, thunder duration and requested-heal amount plus one AtomicBoolean claim. Original confusion roll is carried; thunder uses the original probability/duration calculator once before enqueue with the same level/boss/fixture/snapshot inputs, never at accepted callback. Source runSkill fixes heal to calculated damage and captures thunder mutation data before commit. No Context/Scope/Receipt/Frame/calculator/snapshot is retained by this new accepted metadata, and producer carrier is restored in finally.',
  'A once-only accepted hook runs at all three original normal/catch/finally accepted boundaries after HP/source locks were released, before native wave/end publication. It reuses original source action/admission and owned publication scope, publishes magic figures, consumes only the original erase lease, applies fixed confusion/thunder data only to the exact accepted live actor, then releases the original requested heal even on a fatal target. Nonpositive damage follows direct exact old-target mutations without inventing HP acceptance; no nested HP wait or new lane.',
  'A bounded exact static copy of native applyAcceptedLifeDrain retains its original capped long-arithmetic HP mutation, alive check and monitor. Only self HP and peer ratio output calls use detached original source/receiver bindings and the original frozen audience. Final native Frame rejects changed role/life/scene or stale HP/ratio packet values, and callback-local output carrier is restored in finally. Deferred receiver IO uses the existing owned publication receipt; no new worker/queue/timer/TLS/registry/SQL.',
  f'Independent inverse{count} original methods plus one exact capped-heal sibling,84 no-initialization classes,14 fixed-heal/immutable-input/once-only/concurrent claim/erase ABA contracts plus same-SHA110 inherited area/physical/queue/capacity/poison cases and frozen byte-identical replay pass.66 fixed Source methods pinned;20 readonly SQL classes unchanged. Zero actual bound HP/game/Main/SQL cases; full single-target/numerical/RNG applicability and whole Source/58-material qualification remain false.'
 ]
 remaining=[
  'NPC magic next: port directional/freeze/shock actual accepted target plans and remove freeze alive estimates. Port legacy Mind Break/Bone Break/Arm Break pre-HP MP/status effects using explicit calculated inputs and true accepted mutation receipts, without returning fabricated debit/HP success. Those families intentionally retain their old complete synchronous native path; ordinary/drain/confusion/thunder and the two area paths now share FIFO. Reconcile full Source numerical/RNG/type/immunity, raw/wand/reflection/ground/periodic/healing/controlled/AI/factory/hate/family-link paths, all58 material/control and isolated formal composition, then same-SHA role/fatality/receiver/IO/DRAINING/STOPPED/Main/readonly-Chinese-MySQL/cursor acceptance.',
  *[r for r in old['remaining']if not r.startswith('NPC magic next:')]
 ]
if targetnpc:
 scope=[
  'Natural NPC directional, single/centered freeze and both retained Shock BATTLE bodies have ten handled-only HP/target-tail prefixes and five reject-only original-body guards. The common pre-wait original source/NPC/generation/life/pose/audience snapshot is inherited; Shock captures it before its original wrapper creates or waits on a child Context. The original normal/Ctrl/type/range/cost/calculation/PC/controlled/WORLD bodies are inverse-retained; this stage does not qualify their whole numerical or cursor semantics.',
  'Immutable Mutation stores the original exact erase lease, already calculated freeze duration and immutable Shock spell plus original successful chance result and one accepted CAS. The original chance/d100/boss maps are consumed as scalars; snapshot-HP survival estimates no longer authorize NPC status. Positive HP joins the existing original target FIFO and the once-only pre-wave accepted hook, which publishes figures before target mutation; nonpositive Source target effects follow the separate direct original-proof path without fabricated HP acceptance.',
  'Source SHOCK_STUN.applyStunToTarget draws duration in the accepted target effect. Preserve that phase: after original chance has succeeded and true accepted live-actor proof is available, draw the native immutable spell duration once. Do not draw on queue rejection or reroll chance/calculators at completion. Fatal/corpse and complete Source numerical/RNG/type equivalence remain open for the concentrated qualification matrix.',
  'All18 existing freeze/shock expiry writes on the actual Quest/Time Monster classes route through exact scalar writers that publish a fresh per-status lease before the original value. Four scalar expiry fields are volatile and four bounded per-actor Object slots preserve identity across activate/cancel/expiry/reset, including identical-clock/expiry recast ABA. Original native Hub/owner status and effect placement methods remain authoritative. New status-effect/palette output carries detached original source/target/receiver and lease/expiry, reaches the existing final Frame carrier, and defers IO outside source/HP monitors. Prior-effect cleanup keeps original scene/receiver binding without borrowing a new source. No new queue/worker/timer/TLS/global registry or SQL.',
  f'Independent inverse{count} original methods,90 no-initialization classes,18 acceptance/threshold/actual-unbound-status-writer ABA contracts plus same-SHA124 inherited contracts and frozen byte-identical replay pass.68 fixed Source methods and20 readonly SQL classes are pinned. Zero bound HP/game/Main/DB cases. Full Source/material/control/AI/formal composition and real role/IO/Main/readonly-MySQL/cursor acceptance remain incomplete.'
 ]
 remaining=[
  'NPC magic next: port legacy Mind Break/Bone Break/Arm Break pre-HP MP/status effects into true accepted mutation plans and receipts without fake debit/HP success. They retain their complete synchronous native path. Reconcile remaining raw/wand/reflection/ground/periodic/healing/controlled/AI/factory/hate/family-link producers and complete Source numerical/RNG/type/immunity/corpse semantics. The ordinary/drain/confusion/thunder/area/directional/freeze/shock natural NPC leaves now have scoped FIFO program wiring; that is not full producer or lifecycle qualification. Complete all58 materials/control consumers and isolated formal0.6.71 composition, then same-SHA role/fatality/final IO/DRAINING/STOPPED/Main/readonly-Chinese-MySQL/cursor acceptance.',
  *[r for r in old['remaining']if not r.startswith('NPC magic next:')]
 ]
if specialnpc:
 scope=[
  'Correct the previous planning assumption with four additional fixed Source methods: MIND_BREAK/BONE_BREAK/ARM_BREAKER skillmode.start performs its MP/status mutation before returning damage, and L1SkillUse calls mode.start before HP enqueue. These are not accepted-only target mutations. Keep their Source phase and the real strict MP debit/nonrefresh status/RNG result; no fabricated debit, HP acceptance or after-HP replay.',
  'Two exact original native Mind/Bone bodies are retained behind nullable typed wrappers. For Source BATTLE natural NPCs only, use the common pre-wait source/actual-NPC/generation/life/pose proof, establish an original owned output scope before Hub/owner acquisition, revalidate under original source/action/admission/Hub/actual-owner locks, and return the actual native result. Both natural Arm ports use the same proof guard and return the actual Activation. Rejected/unexecuted prerequisites use original rejected factories and veto later HP; other PC/controlled/WORLD paths retain the complete old body.',
  'One bounded volatile carrier on the existing Context records only detached Origin, phase kind, actual immutable native result and executed flag. It resets at each original single invocation. Only a matching special phase enables the existing single NPC FIFO prefix; original target-before/claim/acceptance/Main and exact erase/figure tail remain common. A stale prerequisite is a handled drop and cannot fall through to synchronous old NPC HP. No new event metadata retains Context/Scope/Receipt/Aim or calculator.',
  'Both original Bone send ports use original receiver bindings and detached source/NPC life/status proof, deferred through the original owned publication scope and final native Frame carrier. All8 original Bone expiry writes publish new per-actor lease identity before the unchanged scalar value; two expiry fields become volatile and two bounded lease slots are added. Native activation/nonrefresh/expiry/reset and86123 placement remain original. No new queue/worker/timer/TLS/global registry or persistence.',
  f'Independent inverse{count} original methods including both exact pre-HP bodies,100 no-initialization classes,24 actual unbound strict-MP/native-result/Bone/Arm/rejected-proof cases plus same-SHA142 inherited contracts and frozen byte-identical replay pass.72 actual Source methods frozen;20 readonly SQL classes unchanged. Zero bound HP/game/Main/DB cases. Complete numerical/RNG/type/AI/final-role/Main/cursor/readonly-MySQL qualification and the full58 material/control/formal-composition objective remain open.'
 ]
 remaining=[
  'NPC magic next: the three legacy skillmode pre-HP phases are now proof guarded with actual native results and their later single NPC HP uses the original FIFO. Continue remaining raw/wand/reflection/ground/periodic/healing/controlled/AI/factory/hate/family-link producers, full Source numerical/RNG/type/immunity/corpse semantics, all58 material/control consumers and isolated formal0.6.71 composition. Then concentrate same-SHA actual role/fatality/receiver IO/DRAINING/STOPPED/Main/readonly-Chinese-MySQL/cursor acceptance. Preserve pre-HP Source side effects on later HP rejection; do not introduce an unsupported rollback or accepted-only phase.',
  *[r for r in old['remaining']if not r.startswith('NPC magic next:')]
 ]
if poisonpc:
 scope=[
  'The actual fixed L1DamagePoison tick does not require the installed attacker to remain alive/online. The retained native scoped DAMAGE poison path already uses target-held clock/role validation, raw false/true poison HP and null caster, but the outer NativeSourceRawDamage.Action constructor still selected its supplied attacker as the action pair peer. That unrelated peer/liveness precondition could discard a valid target tick before the original clock body ran.',
  'Change exactly one original constructor argument before the original pair wait: for Source BATTLE only, Scope.find(target) must return the actual original Context with clockActive and frozen DAMAGE kind, then select no attacker peer. The original Action consequently owns only its actual victim peer. All original target identity/life/scene, tickAllowed/contextValid, installed clock, raw HP flags, queue/admission/Main, expiry/cleanup and original attribution bodies are inverse-retained; no target guard is bypassed and no fresh attacker is selected.',
  'One stateless helper reads existing context fields; no new Context/plan/status field, scheduling resource, queue/worker/timer/TLS/registry/SQL or output/HP method. Unloaded/WORLD, missing/inactive clock, other poison kinds and unscoped paths keep the original supplied peer. Existing nested raw Action behavior and full PC poison attribution remain separate open audit items.',
  f'Independent inverse{count} original raw methods and byte retention for every other original class,105 no-initialization classes and same-SHA166 inherited pure contracts plus frozen byte-identical replay pass.72 actual Source methods pinned and20 readonly SQL classes retained. Zero new PC-poison/bound HP/game/Main/DB cases. The scoped peer repair has no real PC gameplay qualification; the complete goal remains open.'
 ]
 remaining=[
  'HP producers next: finish unscoped/nested and other-kind PC poison, original attacker attribution independent of HP authority, raw/wand/reflection/ground/periodic/healing/controlled/AI/factory/hate/family-link producers and full Source numerical/RNG/type/immunity/corpse semantics. The scoped DAMAGE tick no longer reacquires its supplied attacker as an HP peer, but this is not whole PC poison qualification. Complete all58 material/control consumers and isolated formal0.6.71 composition, then concentrate same-SHA actual role/fatality/final IO/DRAINING/STOPPED/Main/readonly-Chinese-MySQL/cursor acceptance.',
  *[r for r in old['remaining']if not r.startswith(('NPC magic next:','HP producers next:'))]
 ]
if rawnpc:
 scope=[
  'Six existing raw Quest/Time leaves in the retained wand, Mortal Body and Joy of Pain producers discard their original int return. Their loaded Source BATTLE natural-NPC path now submits mode0 to the original shared actor FIFO and existing worker/claim/capacity/admission/Main; return0 does not invent synchronous acceptance. Full old raw monster body remains for nonproduct/missing-Action fallback. Original caller RNG, cost, excluded kinds and pre-HP graphics remain unchanged.',
  'A final Object field on the original seven-component Origin carries only a detached Source(Actor,position,dead) copied from the original raw Member accepted life. A typed policy prefix reproduces original role/life/pose/dead checks and BATTLE scene without imposing ordinary attack HP>0 on the already accepted dead reflector. The full ordinary source policy remains unchanged for every unmarked Origin. Only explicit temporary raw Admission matching the exact source Actor/actual target/owner/life/generation can attach that proof; no queued Action/Context/Scope/Receipt/snapshot or new authority lookup.',
  'The original frozen audience is reused by the existing common Commit. Temporary admission is restored in finally. Root frame and original target proof are revalidated; original lifetime.admits handles the existing causal scope rather than adding a new OPEN-only leaf check. Common accepted HPBAR/death/wave/reward/end publication remains; live raw HPBAR still requires a live original source at both producer and final Frame, while dead-meter cleanup remains independent. Mode0 produces no ordinary magic digits and runs no doll proc. No new queue/worker/timer/TLS/registry/SQL.',
  f'Independent inverse{count} original methods plus six original POP caller ports,108 JVM no-initialization classes,12 actual Origin/schema/source-target-owner-life-generation admission contracts plus same-SHA166 inherited pure contracts and frozen byte-identical replay pass.74 actual Source methods frozen;20 readonly SQL classes unchanged. Zero bound raw HP/dead-defender/Main/game/DB cases. Full raw numerical/attribution/reaction suppression/controlled/unscoped and whole Source/material/control/formal/real role/IO/Main/cursor qualification remain open.'
 ]
 remaining=[
  'HP producers next: the scoped natural-NPC raw wand/Mortal/Joy leaves now share the original FIFO with detached original raw-source policy and raw mode0. Continue unscoped/controlled raw and wand/reflection families, PC poison other-kind/nested/original attribution, ground/periodic/healing/AI/factory/hate/family-link producers and full Source numerical/RNG/type/immunity/corpse/reaction-suppression semantics. Complete all58 material/control consumers and isolated formal0.6.71 composition, then same-SHA actual role/fatality/final IO/DRAINING/STOPPED/Main/readonly-Chinese-MySQL/cursor acceptance. Program wiring is not full raw or whole Source qualification.',
  *[r for r in old['remaining']if not r.startswith(('NPC magic next:','HP producers next:'))]
 ]
if refrawnpc:
 scope=[
  'Close the actual accepted natural-NPC feedback path missed by a direct-call census: NativePlayerCommit.acceptedTail calls NativeCombatAttacker.acceptedFeedback and its retained validated Ref body dispatches Quest/Time HP reflectively. The earlier six legacy/static POP callers do not establish reachability or coverage of this production path.',
  'Change only that one deep natural reflective port, after the original positive-damage/validFrame/live Ref checks. Pass the exact original Ref and already validated Live local to a stateless helper. For Source BATTLE within the existing matching raw Action, require the accepted non-WORLD Frame, matching Quest/Time method/descriptor/id/operand types/positive damage, original Ref room/owner and exact same actual NPC actor, then call the existing mode0 raw NPC FIFO. Stale scoped inputs are handled drops; unsupported or unscoped paths keep the exact original full fallback.',
  'All original caller calculations/chance/results, reaction suppression, reflecting TLS restoration/finally, native Ref live checks, and controlled/WORLD bodies are inverse-retained. No new field, queued Ref/Live/Action/Context, timer/queue/worker/TLS/registry, output or SQL. Existing raw Origin source proof/frozen audience, target/role/life, HPBAR, claim/capacity/admission/Main and accepted publication remain shared.',
  'The inspected sourcePlayerBattleCommit directly sets deathConfirmationPending and publishes confirmation; it contains no direct endBattle/serverEndBattle call. Do not invent a child wait or new end coordinator on that incomplete suspicion. Actual death-confirm/role/scene/end and Main races still require the concentrated matrix and remain unqualified.',
  f'Independent inverse{count} original Ref-dispatcher methods,109 JVM no-initialization classes,12 actual native Ref ABI/kind/id/operand contracts plus same-SHA178 inherited pure contracts and frozen byte-identical replay pass.74 actual Source methods frozen;20 readonly SQL classes unchanged. Zero bound accepted-feedback/dead-defender/Main/game/DB cases. Full raw/Ref/AI/numerical/receiver and the whole58-material/control/formal/real-client goal remain open.'
 ]
 remaining=[
  'HP producers next: scoped accepted natural Ref feedback now reaches the same raw NPC FIFO as the six direct wand/Mortal/Joy leaves. Continue unscoped/other Ref and controlled raw families, PC poison other-kind/nested/original attribution, ground/periodic/healing/AI/factory/hate/family-link producers and full Source numerical/RNG/type/immunity/corpse/reaction-suppression semantics. Complete all58 material/control consumers and isolated formal0.6.71 composition; concentrate actual accepted-fatal-defender/death-confirm/role/scene/end/final IO/DRAINING/STOPPED/Main/readonly-Chinese-MySQL/cursor acceptance on the final SHA. Static production wiring does not qualify those real races.',
  *[r for r in old['remaining']if not r.startswith(('NPC magic next:','HP producers next:'))]
 ]
if hitreaction:
 scope=[
  'Replace only the existing NPC-gfx catalog API with typed wrappers over a Source SQL startup snapshot. One SELECT uses the exact existing readonly, autocommit=false Connection after the existing doll reader. No new connection, transaction ownership, hit-time SQL, Source/player/schema/WAL write or player actor state.',
  'The loaded immutable Set matches Source gfx_id membership, including duplicate collapse, nonpositive ints and JDBC NULL=>0. Empty tables replace the old fixed list. SQLException continues startup with empty or already-read partial rows, matching Source load; publication is atomic. Before a successful guarded invocation, original unloaded catalog bodies remain available without reinterpreting old values.',
  'Eight existing direct NPC reaction call sites remain byte-identical, including accepted Ref, natural and controlled legacy feedback and cube NPC. The control only changes their existing gfx decision. All other old class entries, actual HP acceptance, pair/actor/life/scene/final receiver, queue/worker/admission/DRAINING/STOPPED/Main code and 20 protected SQL classes are byte-retained. No client target-id/digits protocol was changed.',
  f'Independent inverse{count} original startup/catalog methods,112 no-initialization JVM classes,14 owned-readonly JDBC/membership/failure proxy cases plus190 same-SHA inherited pure cases and frozen byte-identical replay passed.80 actual Source methods frozen. Zero actual database, game, bound HP or Main lifecycle cases. Other NPC explicit action ports, ordinary target/RO sync, multiple resource-database routing and the full Source goal remain open.'
 ]
 remaining=[
  'Hit reaction next: readonly startup gfx control now feeds the eight existing catalog consumers. Audit every other NPC ACTION_Damage and normal attack target/RO-login sync port, PC exclusion, actual temp gfx changes and multiple resource-database routing before full consumer qualification. Live Chinese MySQL and actual receiver/Main tests remain deferred.',
  *old['remaining']
 ]
if npcreaction:
 scope=[
  'Actual Source L1AttackNpc Counter Barrier and L1MagicNpc Counter Mirror separately gate NPC ACTION_Damage without removing reflected HP or defender effects. Close the two existing native publisher send ports missed by the catalog-only census, not a guessed protocol or client workaround.',
  'In loaded BATTLE, use only the original current ingress Ref for companion/Quest/Time. A supported mismatched object id drops the reaction; a matching Ref uses the original private live validator through a four-instruction Object-return bridge, then typed actual companion/Quest/Time snapshot gfx and the Source startup catalog. WORLD/unloaded/unscoped/unsupported original paths remain fallback. No new Ref capture or lookup-as-authority.',
  'Only the second encrypted send in each original Counter Barrier/Mirror NPC publisher is conditional. The original defender effect, HP transition, probabilities, cost/status consumption, recipient list/screen checks, sender and publication order are inverse-retained. Validation releases its original locks before encrypted IO. No fields, retained Ref/Live/Context, queue/worker/timer/TLS/registry or SQL were added.',
  f'{count} original methods independently inverted,113 actual JVM classes inspected without initialization,16 actual Ref/typed snapshot contracts plus204 same-SHA inherited pure contracts and frozen byte-identical replay passed.82 Source methods frozen and20 protected SQL classes byte-retained. No bound reflection/HP/recipient/Main/game/MySQL case is claimed.'
 ]
 remaining=[
  'NPC reaction next: two shared Counter Barrier/Mirror encrypted-send guards now use original supported ingress Ref/live/snapshot gfx. Unscoped/unsupported branches, all other NPC explicit ACTION_Damage, ordinary target/RO login-sync, temp-gfx and multi-resource routing remain open. Full Source reflected HP/RNG/effect phase order is not qualified; do not infer it from inverse preservation. Concentrated real role/scene/receiver/Main matrix remains deferred.',
  *old['remaining']
 ]
if wandreaction:
 scope=[
  'Freeze actual Lightning_Magic_Wand execute and doWandAction: original raw HP enqueue/calculation precedes wand/target visuals; charge mutation follows doWandAction; only Monster ACTION_Damage consults the Source gfx suppression control. Do not turn other target types into Monster HP or figures.',
  'A typed wrapper around original raw wandHurt handles only loaded BATTLE, action2 and natural Quest/Time metadata already owned by the current raw Action. Use the original Action.frame (without a new positive-HP, OPEN-only or post-charge item gate), exact immutable NPC id/kind, actual private actor class and original snapshot typed gfx. No new Ref capture or global id authority.',
  'Suppressed or stale scoped reaction returns an empty byte array consumed by the existing captureOutput zero-packet skip before encryption. Unchanged raw output capture/defer/allowFrame keeps original receiver binding, source/target life/scene and DRAINING/STOPPED/Main policy. PC/controlled/unsupported/WORLD/unloaded/unscoped and non-damage original wand bodies remain fallback. No HP/charge/RNG/item/FX/queue/worker/field/SQL change.',
  f'{count} original raw methods independently inverted,114 no-initialization JVM classes,16 actual unbound actor/Npc metadata and packet ABI contracts plus220 inherited same-SHA pure cases and frozen byte-identical replay passed.84 Source methods frozen and20 protected SQL classes byte-retained. Zero bound wand HP, charge, fatal-target, receiver, Main, game or MySQL cases.'
 ]
 remaining=[
  'Wand/periodic next: natural Quest/Time action2 suppression now uses the original raw Action and existing final output guard. Complete full Source wand numerical/type/immunity/cost/fatal-target and HP-vs-output chronology, other item/ground/periodic/healing/AI producers, unscoped controls, ordinary-target/RO-sync/multi-resource routing and whole58 material semantics. Actual role/receiver/DRAINING/STOPPED/Main/Chinese MySQL/cursor matrix remains deferred until program completion.',
  *old['remaining']
 ]
if groundorigin:
 scope=[
  'Bind actual original PC FIRE_WALL lease construction to the original scene/engine epoch and creator Binding. Final noncomponent metadata on the original lease, pulse and GroundTick preserves their old record/canonical signatures. Synchronized original advance captures actual source/target role and life before clock dispatch or pair waits; no copied id header can create proof.',
  'The actual clock PC port retains its original pulse proof and source session, validates original scene/lease membership, then enters the existing ordered raw pair action. After original waits/admission it revalidates frozen actors before the old damage RNG/body. Only the PC58 HP leaf uses existing raw false/true, empty-plan shared FIFO; original math, freeze/cooking/earth gates, reaction order, competition checks, geometry, capacity and timer remain inverse-retained.',
  'Existing raw reaction capture/defer/final Frame adds the original ground scene proof. One bounded volatile Context carrier stays through the original raw output lifecycle; original Action.close clears it on completion or unregistered failure. No queued record stores Context/Action/Frame; immutable entity/actor records and original owned fields introduce no clock/queue/worker/TLS/registry/SQL. Unscoped, unloaded/WORLD, non58, BOT and NPC-ground fallbacks remain original.',
  f'{count} original methods independently inverted,125 no-initialization JVM classes,16 actual unbound lease/pulse/tick epoch/schema contracts plus236 inherited same-SHA pure cases and frozen byte-identical replay passed.91 actual Source methods frozen;20 protected SQL classes byte-retained. No bound PC HP, role, pair, receiver, Main, game or MySQL qualification.'
 ]
 remaining=[
  'Ground P0 next: queued PC HP still needs detached original ground scene/lease proof at actual HP and accepted Frame boundaries beyond the retained raw member guard. The new synchronous Context carrier is intentionally not queued. Complete that event wiring, natural/controlled NPC firewall targets and BOT/Time-NPC applicability, original effect NPC presentation, Source interval/placement/numerical/SKM0/SKD3/type/immunity/master-death semantics; retain competitive gates. Concentrate actual role/scene/lease/expiry/pair/receiver/DRAINING/STOPPED/Main and final same-SHA MySQL/cursor matrix after program completion.',
  *old['remaining']
 ]
if groundqueued:
 scope=[
  'One visible Object on the existing raw HP Plan carries only the same detached original ground PulseProof. Bind after original plan construction before publishing it; do not retain the producing Context/Action in the queued event, new TLS or registry. Existing original queue/drainer/admission/dequeue credit and release owners remain unchanged.',
  'Actual drain resolves the original caster session before a source-only pre-defense scene/role/binding/life predicate. Marked stale events use the original empty-defense branch; one primitive local remembers rejection so a later state change cannot authorize HP after skipped defensive RNG. At the existing actual pre-HP guard, revalidate with that same resolved caster, exact old false/true flags and original event/raw-member binding/life plus original lease Entity pointer, scene/epoch and Main. Unmarked producers preserve their entire old guard/calculation path.',
  'The original final Frame predicate checks the detached scene proof and the exact target acceptedLife sealed by the original acceptedState under the HP lock. Fatal accepted output remains legal at that token; later revival/death ABA or role/scene retirement rejects old output. Source HP attribution stays the original PC caster; no extra post-admission expiry, lease-list, timestamp or OPEN-only gate is added. Individual lease expiry does not withdraw already admitted HP; scene clear and STOPPED/Main fences still reject.',
  f'{count} original methods independently inverted,126 no-initialization JVM classes,18 actual unbound lease identity/scene/individual-expiry and actor-tuple contracts plus252 same-SHA inherited pure cases and frozen byte-identical replay passed.91 Source methods frozen;20 protected SQL classes byte-retained. No actual bound HP/fatal/role/pair/receiver/Main/game/MySQL case. These checks do not qualify complete ownership linearization or all ground families.'
 ]
 remaining=[
  'Ground queued next: detached original lease/scene proof now reaches pre-defense, actual resolved-caster HP and accepted Frame ports. Complete Source scene transitions/ownership linearization and original factory before-first-wait caster evidence, natural/controlled NPC firewall and BOT/Time-NPC applicability, original effect NPC presentation, Source interval/placement/numerical/SKM0/SKD3/type/immunity/master-death policy. Concentrate real queued role/scene/expiry/fatal/dead-revive/pair/receiver/DRAINING/STOPPED/Main and final same-SHA MySQL/cursor matrix after program completion.',
  *[item for item in old['remaining']if not item.startswith('Ground P0 next:')]
 ]
if groundplacement:
 scope=[
  'Correct the actual production route: earlier ground-origin metadata covered legacy direct GameHub.placeGroundSpell, while material use calls placeBattle -> GameHub.nativeMaterialPlaceGroundSpell -> NativeGroundMaterialAction.castBattle. Its original disposable two-copy engines created unbound leases; metadata-only tests did not establish that real material factory. This child stage closes that specific route.',
  'Peek only the original NativeLightAction current Batch and reuse accepted Ingress.binding captured by reserve before original player-lock wait. Do not recapture the current caster. Under the original Hub/action locks validate same caster id, original binding and original room/live engine. Non58, WORLD/unloaded/unscoped fallbacks retain original behavior; no new scope, TLS or owner registry.',
  'Retain original preflight copy, cost callback/Rejected branch, second copy/recalculation and install order. A detached Draft transfers original scene/epoch/creator metadata to disposable copies. New staged lease Entity identifies the original live engine, not its temporary copy. Live scene is published only at original install after accepted cost; failed/rejected preflight never publishes live scene/leases. The existing installing slot is restored in finally; Draft and Entity retain no Batch/Ingress/Context/Frame.',
  f'{count} original methods independently inverted,131 no-initialization JVM classes,18 actual unbound copy/install/live-lease contracts plus270 inherited same-SHA pure cases and frozen byte-identical replay passed.91 Source methods frozen and20 protected SQL classes byte-retained. Zero bound caster/MP/reagent/rollback/HP/Main/game/MySQL cases. Full material cost/closure qualification and all NPC/other Source families remain open.'
 ]
 remaining=[
  'Ground material next: actual native material two-copy production now carries original prewait caster binding and live-engine Entity through cost/rejected/install boundaries. Complete bound role/MP/reagent/rollback/closure and final HP/scene ownership matrix; NPC/controlled firewall and BOT/Time-NPC applicability, Source effect presentation and numerical/interval/geometry/SKM0/SKD3/immunity/master-death semantics remain open. Continue full58 material and remaining Source consumers, then isolated formal composition and same-SHA backend/readonly Chinese MySQL/real cursor acceptance.',
  *[item for item in old['remaining']if not item.startswith('Ground queued next:')]
 ]
if groundnpc:
 scope=[
  'Natural Quest/Time NPC actors are appended only to actual bound Source firewall advances under the original Hub/encounter monitors. Freeze actual actor, owner, life/generation/position and original audience before source lock waits; reject duplicate ids and do not infer BOT/controlled actors are Source monsters. Original lease clock, expiry, radius and PC/BOT/unscoped fallbacks are retained.',
  'Freeze quest lane from the original creator binding at actual Entity construction. Carry the exact lease/entity/epoch/scene through pulse and original GroundTick before the native null-PC discard; original clock dispatch reaches natural targets without a fake player target or caster. Before RNG, original source role/life/room and actual NPC identity/life/pose/faction plus freeze/earth-bind are revalidated under existing locks.',
  'Use existing readonly NPC stats/effective caster INT, Source positive-only fire resistance with the actual elemental fall-down lease, and one uniform roll. Send Source typed-gfx-controlled ACTION_Damage before enqueue, with original frozen audience/receiver proofs and no action/Hub/encounter locks over receiver IO. Source permits a registered dead master; the detached raw source policy preserves that distinct from live HPBAR policy.',
  'Original shared natural actor FIFO mode0 retains bounded admission/claim/release/drainer and accepted life. Detached Origin proof extends scene/epoch/source life/Main validation to original precommit and final output; no ordinary digits or doll proc. Post-tile-admission target movement and individual lease expiry do not revoke raw HP; identity/generation/life and source/room retirement still reject. No queued Context/Batch/Frame, new queue, worker, TLS, timer or SQL.',
  f'{count} original methods inverse,135 JVM no-init classes,26 arithmetic/faction/schema contracts plus288 inherited same-SHA pure contracts and frozen byte-identical rebuild.93 Source methods/25 Source files frozen;20 SQL classes unchanged. Zero bound HP/role/lane/master-death/receiver/Main/game/MySQL qualification.'
 ]
 remaining=[
  'Ground next: full NPC SKM0/type applicability including absolute barrier and controlled/BOT/Time-NPC sources; PC SKD3/firewall numerical parity, effect NPC presentation, placement/lease/cost/rollback and scene ownership/closure. Natural Quest/Time raw firewall program route is staged, not runtime-qualified. Continue remaining58 material and all other Source controls/HP/NPC factory-AI producers, formal isolated composition, then concentrated same-SHA backend/Chinese readonly MySQL/equipped-role/cursor acceptance.',
  *[item for item in old['remaining']if not item.startswith(('Ground material next:','Ground queued next:','Ground P0 next:'))]
 ]
if groundplayer:
 scope=[
  'Actual PC serverGroundPulse gains a handled-only Source58 prefix inside the original paired raw action. Before RNG revalidate exact installed lease/entity/scene, source-target binding/life and running original competition canAttack; actual freeze/absolute-barrier/earth-bind gates run before effective INT/stat calculation and again before the original uniform draw. No replacement caster capture, actor fields or synchronous scope.',
  'Use existing effective caster INT and target effective fire resistance, with Source positive-only resistance/truncation. Original cooking recipe status IDs, dragon/patience/immune active leases select fixed Source SKD3 keys. Every active additive key applies once, then IMMUNE_TO_HARM halves with integer truncation. This replaces the old base-summary INT and generic cooking/reduction-armor/dragon/patience scalar path for this scoped pulse only. Source load oracle checks all31 actual SKD3 rows and absent IDs.',
  'Keep original raw false/true HP plan and parent receiveDamage reductionOnly defensive proc, incoming weapon/avatar modifiers, queue/accepted/fatal-life/Main pipeline. Source DamageCommitPlan.none does not bypass receiveDamageInternal reductions, so directRaw is not enabled. Original frozen action audience and deferred output are used for ACTION_Damage before HP; native-screen range is checked again at the final existing ground Frame predicate without locks.',
  f'{count} original methods inverse,136 JVM no-init classes,35 Source arithmetic/status/recipe/packet/oracle contracts plus314 inherited same-SHA pure contracts and frozen byte-identical rebuild.93 Source methods/26 files frozen;20 SQL classes byte-retained. Zero bound pair/HP/defense RNG/fatal/receiver/competition/Main/game/MySQL qualification.'
 ]
 remaining=[
  'Ground next: program paths now cover scoped PC firewall SKD3/effective stats and natural Quest/Time raw FIFO. Complete full effective-stat parity, Source clan/safety mapping against retained BATTLE competition, placement/effect NPC presentation, NPC SKM0/type applicability, controlled/BOT/Time-NPC producers and same-SHA pair/HP/accepted/fatal/receiver/Main matrix. Continue all remaining58 material, Source controls and factory-AI families, isolated formal composition, then concentrated backend/readonly Chinese MySQL/equipped role/real cursor acceptance.',
  *[item for item in old['remaining']if not item.startswith(('Ground next:','Ground material next:','Ground queued next:','Ground P0 next:'))]
 ]
if cubeplacement:
 scope=[
  'Correct actual material production: prior Source cube provenance patched legacy GameHub.placeCubeIgnition/Harmony -> engine.place, while real casts use NativeCubeCastAction -> GameHub.nativeMaterialPlaceCube -> engine.nativeMaterialPlace. Its plain Cube constructor left entity/faction unbound, so Source PC/NPC HP consumers dropped real material cubes despite passing old metadata contracts.',
  'At the two actual battle material Hub calls, prepare a detached Draft using the existing Batch.accepted Ingress.binding captured before the original player-lock wait. Validate actual caster/Hub/room/live engine and action ownership before original material admission. No fresh caster capture or retained Batch/Ingress/Context. Redirect exact original Cube constructor to its existing metadata overload; original geometry/admit/cost/object-id allocation/list order is retained.',
  'Original entity/faction construction consumes Draft epoch/scene/creator and identifies the actual installed cube/live engine. Publish live Source scene only after original accepted return; rejected geometry/material branches leave scene unpublished. Finally restore installing slot. Actual effect NPC HP source and existing target lease/FIFO/accepted/final output paths remain the parent program, independent of current creator availability.',
  'Source effect NPC scans use500ms, distinct from L1Cube initial900/period1000 status callbacks. Replace only the original scan-interval local when the engine has a Source scene; retain ignition fourth callback, harmony fourth MP/fifth HP, original expiry and unloaded/WORLD/unscoped scan. No new queue, timer, worker, TLS, fields, SQL or HP algorithm.',
  f'{count} original methods inverse,138 JVM no-init classes,20 actual unbound cube/epoch/overlap/status-clock and draft/cadence contracts plus349 inherited same-SHA pure contracts and frozen byte-identical rebuild.101 Source methods/29 files frozen;20 SQL classes unchanged. Zero bound caster/MP/reagent/cost rollback/HP/role/Main/game/MySQL qualification.'
 ]
 remaining=[
  'Cube next: actual Ignition/Harmony material factory is now connected to original Source entity/faction and500ms effect scan; independent900/1000 status callbacks are retained. Complete Quake/Shock factory/status provenance, global initial scan phase, full Source geometry/type/clan-party-safety-to-competition mapping, status presentation/MP consumers and bound cost/HP/creator-retirement/role/Main matrix. Continue full remaining58 material, all Source controls/factory-AI and formal isolated composition, then same-SHA backend/readonly equipped Chinese MySQL/real cursor acceptance.',
  *[item for item in old['remaining']if not item.startswith('Cube next:')]
 ]
if cubestatus:
 scope=[
  'Quake/Shock actual material factories now reuse original prewait Batch Ingress binding and detached accepted-only scene publication, preserving original geometry/admit/cost/construct/list bodies. Actual effect entities80150/80151 and original faction are installed on real cubes. Existing PC/natural primary leases capture original target binding/life/generation; no new player or NPC HP source.',
  'Original bind/MR cells carry detached exact primary-source links, including original MR refresh. Retire stale same-id target tuples under original engine monitors. Each original advance publishes one bounded immutable volatile identity index of current cube/status/secondary cells; final predicates use that snapshot, never iterate mutable maps or take new locks during IO. Old expiry/recast packets cannot borrow a replacement lease; clear rotates epoch and drops index.',
  'Actual Source scene scans500ms and original900/1000 status callbacks are retained. Source Quake natural monsters receive original actor activateFreeze1000 under original Hub/encounter identity checks. Fixed Source Shock only refreshes its4000 marker, without numeric MR subtraction; unscoped/WORLD/BOT native numeric fallback is preserved. Original numeric consumer bodies retain their normal fallback but reject stale Source lease identity.',
  'Source typed tick dispatch uses actual target and frozen audience bindings, original raw self ownership/deferred output/close, final role/life/scene/index and range proof. New Context proof is synchronous and cleared through original Action.close; detached Link/Emission never retain producing Context/Batch/Frame. No new HP queue, worker, timer, TLS, database connection or persistence path.',
  f'{count} original methods inverse,151 JVM no-init classes,24 actual unbound factory/status/secondary/index/clear and target-tuple ABA contracts plus369 inherited same-SHA pure contracts and frozen byte-identical rebuild.106 Source methods/32 files frozen;20 SQL classes unchanged. Zero bound role/material/cost rollback/HP/natural-freeze/final IO/Main/game/MySQL qualification.'
 ]
 remaining=[
  'Cube next: all four material factories have Source provenance; Quake/Shock primary/secondary leases and numeric/final-output identity index are staged. Complete corresponding Ignition/Harmony attribute/status/MP consumers, all cube presentation/config-GFX/target clauses, global initial scan phase and relation/clan-party-safety mapping, then actual role/cost/HP/natural-freeze/recast/receiver/Main matrix. Continue remaining58 material, Source controls/factory-AI/formal isolated composition and concentrated same-SHA backend/readonly equipped Chinese MySQL/cursor acceptance.',
  *[item for item in old['remaining']if not item.startswith('Cube next:')]
 ]
if cuberemainder:
 scope=[
  'Ignition/Harmony original advance publishes bounded immutable volatile exact cube/status identity index; actual original nonHP events carry detached primary lease, actual target role/life/generation and frozen audience. Existing PC/natural queued HP origin and FIFO bodies are unchanged. Original attribute/status queries reject stale Source leases while preserving native unscoped/WORLD/BOT values.',
  'PC Harmony recovery executes original MP body inside original raw ownership and original Hub scene monitor, recording acceptance at original MP write under player monitor. Actual accepted after/max and original role/life guard final deferred output; postacceptance primary expiry does not revoke the already committed MP notice. Newer MP values reject stale packets. Source scoped addition uses long bounded arithmetic, native overflow fallback is retained.',
  'Natural Harmony recovery uses original actual Quest/Time Monster pointer, life/generation and faction under existing lifetime admission-Hub-encounter monitor order, calling exact original alive/max/store recovery siblings with Source bounded addition. Original caster availability or fresh target id lookup cannot grant replacement actors the old recovery.',
  'Original typed nonHP status methods and original raw self/deferred Frame/close carry exact lease/scene/Main/target/receiver/range. Clear rotates epoch and clears original-owner index. Metadata retains no producing Context/Batch/Frame; no new worker, timer, TLS, queue, HP write or SQL. Original 900/1000 status clocks, 500 Source scan, original costs and WORLD fallback remain unchanged.',
  f'{count} original methods inverse plus two exact natural MP siblings,154 JVM no-init classes,25 new actual unbound index/expiry/replacement/clock/schema/MP-ABI/numeric/wire-boundary contracts plus393 inherited same-SHA pure contracts and byte-identical frozen rebuild.108 Source methods/32 files and20 unchanged SQL classes. Zero bound player/NPC MP, role, final IO/Main/game/MySQL qualification.'
 ]
 remaining=[
  'Cube next: all four material factories and primary/status numeric/final identity paths are staged, including Ignition/Harmony MP. Complete Source cube presentation/config-GFX and target clauses, global initial scan phase and relation/clan-party-safety mapping, then actual role/cost/HP/MP/natural-freeze/recast/receiver/Main matrix. Continue all58 material, Source controls/factory-AI/formal isolated composition and concentrated same-SHA backend/readonly equipped Chinese MySQL/cursor acceptance.',
  *[item for item in old['remaining']if not item.startswith('Cube next:')]
 ]
if cubepresentation:
 scope=[
  'Eight original typed PC/natural primary status visual ports now select existing immutable readonly SkillsTable castgfx/castgfx2 by actual cube skill205/210/215/220. Source natural monsters take enemy visual clause. No extra SQL reader/connection, generic Context authority, HP source or player identity is introduced.',
  'Actual Harmony advance stores its already computed original relation on the exact original status after construction and before map publication. Self, ally and enemy remain distinct for display; later relation changes, expiry or same-id recast cannot rewrite detached original relation. Original status clocks, suppression/refresh and all other original fields/methods are inverse-retained.',
  'Source ally own-attribute synchronization runs through a typed bridge to the original private status body before target effect; only that Source applied-ally original trailing status call is skipped to avoid duplication. Expiry, unscoped/WORLD/BOT and native status/visual fallback remain original. Source configured zero GFX is carried as the exact object/word payload, never substituted with native default. Positive packets retain native resource remapping and object/wire bounds.',
  'Existing raw self/deferred output/final Frame and close keep original role-life, exact status index, source scene/epoch/Main, frozen receiver and range predicates. Existing Quake/Shock installed predicate additionally rejects expired bound primary/secondary cells after waits. One volatile relation Object on original Harmony status and one stateless helper; no new queue, timer, worker, TLS, retained Context/Batch/Frame or persistence.',
  f'{count} original methods independently inverted,157 no-initialization JVM classes,27 new actual unbound Harmony relation/recast and Source visual wire/oracle contracts plus418 inherited same-SHA pure contracts; frozen byte-identical rebuild.109 actual Source methods/33 files frozen and20 readonly SQL classes unchanged. Zero bound Source status IO/role/cost/HP/MP/Main/game/MySQL qualification.'
 ]
 remaining=[
  'Cube next: four actual factories, primary/secondary identity consumers, Ignition/Harmony MP and all eight status GFX ports are staged. Complete actual effect-NPC presentation/resource selection, Source target eligibility and competition-preserving relation/clan-party-safety mapping, global initial scan phase, then full role/cost/HP/MP/natural-freeze/recast/receiver/Main matrix. Continue all58 material and Source control/factory-AI families, isolated formal0.6.71 composition and concentrated same-SHA backend/readonly equipped Chinese MySQL/cursor acceptance.',
  *[item for item in old['remaining']if not item.startswith('Cube next:')]
 ]
if cuberelation:
 scope=[
  'Four actual engine advance self comparisons and original typed Alliance calls now consume actual effect creator Binding and original prewait target Scan. Base Binding.capture bytecode is independently verified: source is already sourceCharacterIdentity, account sourceAccountIdentity, selected selectedCharacterName. Reuse that exact original qualified source String for current original-role clan/party membership, without requalifying it or reading the current caster session by object id.',
  'Exact Source self requires original creator PC and the same complete source/account/selected/owner/phase/scene Binding plus current original target life; a same-id new role cannot inherit self. New primary status selection checks existing original Hub/engine ownership, scene epoch/Main and OPEN admission, running match and current original target proof. No new locks, fields, registry, Context or metadata are retained.',
  'Source self/clan/party ally clause remains original. New hostile PC installs obey retained original match PvP/team policy using the effect original captured force and actual current target force; no ambient Ctrl relaxation is borrowed. Same-id nonself/invalid-force/non-PvP/friendly hostile selections are ignored. Natural actors keep exact kind/life/generation and original captured-faction lane; BOT and unscoped/unloaded/WORLD keep typed native fallback.',
  'Only scan-time new status selection changes. Original installed status expiry/refresh and independently admitted PC/NPC HP, original source effect entity, queued FIFO/accepted life, costs/RNG/clocks/packet output remain inverse-retained. Creator retirement is not added as an installed HP cancellation condition.',
  f'{count} original methods independently inverted,161 JVM no-init classes,33 new actual unbound four-cube self/typed native callback, source-account-role-owner tuple ABA and six-mode native match-policy contracts plus445 inherited same-SHA pure contracts and frozen byte-identical rebuild.109 actual Source methods/33 files and20 unchanged SQL classes. Zero actual bound creator/target/clan/scene/admission/HP/cost/final IO/Main/game/MySQL qualification.'
 ]
 remaining=[
  'Cube next: original prewait role identity now reaches four actual self/relation scan branches and new hostile selections retain native competition limits; existing installed HP is not revoked. Complete global initial effect scan phase, actual effect-NPC presentation/resource/visibility, any remaining Source target/cube semantics and full creator-target/clan-party/role/cost/HP/MP/natural-freeze/recast/receiver/Main matrix. Continue all58 material/Source controls/factory-AI and isolated formal0.6.71 composition, then concentrated final same-SHA backend/readonly equipped Chinese MySQL/cursor acceptance.',
  *[item for item in old['remaining']if not item.startswith('Cube next:')]
 ]
if cubeclock:
 scope=[
  'Correct a prior Source call-path error: freezing EffectCube*Timer.start proved only a500ms leaf schedule. Actual GameServer calls StartTimer_Skill.start, whose safeSchedule directly schedules the four Runnable timers with initial0/period1000. The production path bypasses those start500 methods. Preserve the exact Source files; supersede prior500-as-production scope flags and correct one inherited pure cadence oracle.',
  'Four bounded per-type Phase cells on the original Hub carry only immutable primitive next/scan times. Initialize before original ground task registration, advance once at entry of each original synchronized type collector before any room iteration, and release at original close. Empty rooms/room clear and separate placements retain service phase; the first due collector slot applies to every room, never only the first cube. Delayed callbacks scan current effects once while next deadline stays on fixed-rate origin.',
  'Four original new-effect deadline comparisons consume the exact current Hub/type scan slot for Source scenes; unbound/unloaded/WORLD retain native per-lease comparison. Correct original cadence Source branch1000. Original100ms owned drainer is retained, so actual observation remains quantized by that scheduler; there is no new scheduled task/worker/timer/queue/TLS. Four Phase records retain no Hub/player/actor/Context/Batch/Frame, and no new lock is introduced.',
  'Only new-effect global scanning changes. Original primary status expiry, separate900/1000 callbacks, fourth MP/damage and fifth HP clocks, actual effect source, original role/scene/FIFO/accepted-life/final receiver, cost/RNG and persistence remain retained. Original Source relation and visual adapters remain; creator retirement does not revoke admitted damage.',
  f'{count} original methods independently inverted,163 JVM no-init classes,33 new primitive global phase/startup/room sharing/placement delay/overflow/close/unbound-clock and actual Source bootstrap oracle contracts plus478 existing same-SHA pure contracts (477 retained expectations and1 corrected cadence oracle). Frozen byte-identical rebuild,111 Source methods/35 files and20 unchanged readonly SQL classes. Zero actual bound Hub/startup/multiroom/role/HP/cost/IO/Main/game/MySQL qualification.'
 ]
 remaining=[
  'Cube next: actual bootstrap0/1000 global initial scan phase is now staged; older leaf-start500-as-production claims are superseded. Complete actual effect-NPC presentation/resource/visibility and remaining Source target/cube semantics, then full real Hub startup/delay/multiroom/clear/creator-target/role/cost/HP/MP/recast/receiver/Main matrix. Continue all58 material/Source controls/factory-AI and isolated formal0.6.71 composition, then concentrated final same-SHA backend/readonly equipped Chinese MySQL/cursor acceptance.',
  *[item for item in old['remaining']if not item.startswith('Cube next:')]
 ]
if cubevisibility:
 scope=[
  'Four actual material object-send and four native bootstrap/portal rebuild ports now use actual effect Entity, original constructor-time audience bindings or receiver binding captured before first rebuild Hub wait. Original accepted publication seals the existing immutable actual-cube index under existing Hub-to-engine lock order. Native encoders and per-receiver resource rewrite compatibility are retained; this does not yet qualify Source NPC template numerical data.',
  'Detached Work implements the original Output interface and occupies only the original Batch output list. Actual output loop handles Work independently of retired caster root. Original success/stale/failure completion tails rescue unclaimed Work once through its atomic claim; all other original output/root/target/completion paths remain inverse-retained. Work catches per-receiver IO/runtime failure, isolates/restores only the existing area Context synchronously, and retains no Context/Batch/Action/Frame/Receipt. Compiler-only InnerClasses visibility stubs are excluded from runtime; original outer/Output classes remain byte-identical and actual private-interface ABI is JVM-loaded.',
  'Original after-move completion refreshes native screen perception. Entering visible range publishes only an unseen matching role; leaving hides a matching seen object; explicit bootstrap/portal rebuild can refresh an already known cube. Original Context carries only synchronous Rebuild/Work proof and is cleared/restored in finally. No new queue, worker, timer, TLS or socket owner is introduced.',
  'Final native pre-cipher gate checks exact live instance/index/epoch/scene/Main/individual expiry, original receiver source-account-role binding, native screen and object/geometry header. Only immediately after actual Protocol.writeFrame returns successfully are known-object bindings inserted/removed, before logging or other tails. Expiry sends removal only to a matching original role that actually received that cube; unseen roles and new same-id cubes cannot inherit old removal. Initial audience releases at expiry and stale seen bindings prune; cache stays owned only by bounded original effect/status/output lifetimes.',
  f'{count} original methods independently inverted,169 no-init JVM classes,39 new actual unbound cube encoder/index/resource-header/claim/private-output-ABI and fixed Source perceive/known-object oracle contracts plus511 inherited same-SHA pure contracts and frozen byte-identical rebuild.113 Source methods/36 files and20 unchanged SQL classes. Four cache references on actual cubes, no Context/Batch fields; zero bound queue/rescue/role/movement/rebuild/IO/Main/game/MySQL qualification. Original HP/cost/RNG, global0/1000 and status900/1000 clocks retained.'
 ]
 remaining=[
  'Cube next: original effect output, movement/rebuild perception and actual post-write known-role pairing are staged. Complete actual Source NPC template/body/GFX/resource numerical parity and full Source visibility-control mapping, remaining cube semantics, then real startup/creator retirement/failure rescue/movement/rebuild/receiver wait/expiry/same-id/role/HP/cost/IO/Main matrix. Continue all58 material/Source controls/factory-AI and isolated formal0.6.71 composition, then concentrated final same-SHA backend/readonly equipped Chinese MySQL/cursor acceptance.',
  *[item for item in old['remaining']if not item.startswith('Cube next:')]
 ]
if cubetemplate:
 scope=[
  'Actual pre-cost original Draft constructor freezes only scalar/string initial presentation data from the existing static NPC snapshot. Missing/failed/wrong subtype/invalid random bounds reject before original material body; no new SQL, actor, scheduler or resource loader. Four original effect constructors freeze random level/GFX/exp/lawful presentation once in Source order, with uniform independent JDK random draws only on Source random branches; native combat/cost/chance RNG bodies stay retained.',
  'Four original Protocol cube encoders select frozen actual-effect presentation, preserving original object/geometry and native client tail/resource rewriting. Initial Source heading/move/chaLight/title are zero/null; template lightSize is not mistaken for chaLight. Immutable Draft/cube fields follow original placement/effect lifetimes; no mutable NPC row or complete resource snapshot is retained. Existing visibility, known-role post-write pairing, final IO, Main, status clocks and HP consumers remain byte-retained.',
  f'{count} independently inverted original methods,175 no-init JVM classes,371 new actual detached row/packet/unbound factory and executable frozen Source initial-presentation cases plus550 inherited same-SHA pure contracts; frozen byte-identical rebuild.120 Source methods/38 files,20 SQL classes byte-retained. Five final Object fields on original Draft/four cubes, three new helper types. Zero bound cost/factory/role/reload/IO/Main/game/MySQL cases.'
 ]
 remaining=['Cube next: complete custom work/spawn actor hooks, full effect attributes and Source visibility/resource-control semantics; qualify real template availability, factory/cost rollback/reload, cross-role/receiver/IO/Main lifecycle after program completion. Continue all58 materials, other HP producers, factory-AI/controls and formal0.6.71 isolated composition; then same-SHA backend/readonly Chinese MySQL/real cursor acceptance.',*[item for item in old['remaining']if not item.startswith('Cube next:')]]
if fatalitydamage:
 scope=[
  'Actual native Fatality single/area entries freeze original trigger binding/life/pose before first Context Hub wait, with frozen target and receiver tuples. The original two die sites now draw and submit once under the ordered trigger-target action pair, existing lifetime admission and target-owned commitSelf. Complete original catalog selection, dice bounds/order, native area predicate and non-Source/WORLD fallback remain retained; no provisional selection weights are represented as Source parity.',
  'A detached explicit NONE Proof rides only the original raw display Plan and target Guard. After queue publication, HP authority is the original target, not a live trigger. Original fatal acceptance updates the same Member life token. Only an exact original target/guard/null-PC/null-Ref marker bypasses attacker-specific defense/incoming/magic Calc; unknown, retired PC bindings, and NPC/effect refs retain their original paths. Original outside equipment145/149 and Avatar amplification remain retained.',
  'Cosmetic outputs carry only original trigger/subject Members, receiver binding, lifetime and cloned bytes. Existing deferred lane and final native pre-cipher gate revalidate identities/life/scene/Main and resource-compatible object header. Source context is synchronous and isolated/restored in finally; no queued Context/Root/Batch/Frame/Receipt or new queue/worker/timer/TLS/SQL owner.',
  f'{count} independently inverted original methods,179 no-init classes,32 new detached classifier/role tuple/native RNG/wire/Source null-branch contracts plus921 inherited same-SHA pure contracts and frozen byte-identical rebuild.120 Source methods/38 files,20 SQL classes byte-retained. One volatile proof field on original Plan; zero bound producer/HP/waits/fatal-area/IO/Main/game/MySQL cases.'
 ]
 remaining=['Fatality next: qualify real producer/target waits, own-fatal area ordering, trigger retirement, raw acceptance and final receiver/IO/Main; complete provisional native catalog Source applicability rather than claiming fixed trap parity. Continue all other actual NONE/unknown source roots, companion/bot/NPC AI, healing/undead reversal, all58 materials/controls and formal isolated composition before concentrated same-SHA backend/readonly MySQL/cursor acceptance.',*old['remaining']]
if npcplayer:
 scope=[
  'Actual canonical MonsterAiTick captures primary PC binding/life before the original NPC lookup and freezes a bounded original audience. Existing validTick adds exact old target/life admission; original source validation and AI/RNG bodies stay retained. Original natural/controlled ingress captures only immutable role Members, original NPC Ref and detached original defense-doll snapshot; its original leave clears exact scope. No new TLS or retained Context/Batch/Frame/Receipt.',
  'Six actual natural/controlled/physical-area/magic PC sinks use the original target action lock, existing lifetime admission and commitSelf, and original source Ref. Source physical false/false versus magic true/false replaces the native common-true flag only on proven scoped sources. Original raw Plan Guard seals target-only role/life in the same FIFO and accepted fatal token/final Frame; no new attacker-liveness requirement revokes queued HP. Existing defense proofs now use original scoped summon/receiver snapshots. Unsupported/unknown/WORLD keep exact native fallback.',
  f'{count} independently inverted original methods,186 no-init classes,32 new detached source/role tuple/exact original ingress cleanup and Source physical-magic/target-owned HP contracts plus953 inherited same-SHA pure contracts and frozen byte-identical rebuild.122 Source methods/38 files,20 SQL classes retained. One final tick reference and one thread-owned original ingress reference,3 helper types. Zero bound AI/HP/role/doll/IO/Main/game/MySQL cases.'
 ]
 remaining=['NPC-PC next: complete upstream NPC actor/stat/chance/AI/factory snapshots and accepted magic side effects, then qualify natural/controlled/area damage, target-role waits, attacker/master retirement, doll replacement, final IO/Main. Complete other NPC-target producers, bot/ground pulse/healing/undead and all58 material/control/formal work before concentrated same-SHA backend/readonly MySQL/cursor acceptance.',*old['remaining']]
if npcmagiceffects:
 scope=[
  'Actual two NPC PC magic targets attach detached requested heal for Source chill10/vampiric28 and exact erase lease at the existing paired pre-HP Plan boundary. Existing accepted completion claims once; rejected HP never releases effects, accepted terminal damage may recover the original living NPC but skips target mutation. Proven-scope premature aggregate heal is suppressed, while original native/WORLD fallback, catalog and RNG stay retained. Source ALT_ATKMSG diagnostics and other magic mutations remain open.',
  'Frozen actual NPC/owner/life/generation is revalidated under existing admission-Hub-encounter order; bounded original heal siblings retain dead/invalid gates and snapshots/results with long-safe HP cap. No victim/new role authorizes source healing. Source tracked-meter refresh uses only existing weak viewers and frozen bindings, global HPBAR control, range/current ratio/life/Main final predicate; actual successful writeFrame alone updates cache ratio. Existing completionFrame temporarily isolates victim Frame and existing area Context is restored in finally. No queued Context/Scope/Definition/Frame/Receipt or new queue/worker/timer/TLS/SQL.',
  f'{count} independently inverted original methods plus2 bounded original heal siblings,194 no-init classes,33 new accepted-once/exact erase ABA/dead-heal/native meter/Source constant and plan-order contracts plus985 inherited same-SHA pure cases and frozen byte-identical rebuild.124 Source methods/40 files,20 SQL classes retained. One synchronous Scope input field and one volatile original Plan After field; zero bound HP/heal/role/IO/Main/game/MySQL cases.'
 ]
 remaining=['NPC magic next: qualify actual rejection/acceptance/fatal recovery, original source retirement/same-id replacement, erase recast, tracked-viewer IO and Main. Complete upstream formulas/actor-stat snapshots, remaining skill mutations, NPC ALT_ATKMSG control/diagnostics and controlled/bot/ground/healing/undead; all58 material/control/formal work and concentrated final same-SHA acceptance remain open.',*old['remaining']]
if npcmagicdirect:
 scope=[
  'Fixed Source runSkill uses gateOnDamageAcceptance=_dmg>0. Nonpositive target effects are queued directly; zero damage with zero mana skips magic.commit entirely, while signed negative damage submits the original HP commit before queued mutations. A separately compiled executable oracle uses the exact frozen Source gate and queueTargetSideEffects fragments, not a handwritten expectation alone.',
  'At the existing target actionlock/admission/commitSelf boundary, before the existing positive accepted-plan bind, proven NPC attack metadata captures the exact erase lease. Known zero attack applies its direct mutation and returns before the original HP sink, creating no fabricated accepted event/After. Negative damage freezes a detached Mutation on the existing Plan carrier, keeps the complete original HP body, applies only after original owned submission returns, then clears the exact carrier in finally. Positive magic, physical/nonattack, missing metadata/definition and unscoped/WORLD fallback retain their original paths. Signed HP/healing formulas, mana-only producers and other skill mutations remain open.',
  'Direct mutation requires original source-account-role binding/life, living target, matching hub/status owner, actual target lock, OPEN admission and Main open. Exact status consume rejects same-clock recast/expiry ABA. Recast, Main close or draining between signed submission and release cannot consume replacement status; carrier is still cleared. No new fields, actor/queue/worker/timer/TLS/SQL/RNG/cost port.',
  f'{count} independently inverted methods of the sole changed original class; every other parent archive entry is byte-identical.196 JVM classes checked without initialization;37 new frozen-Source branch/actual detached gate/lease/role-life/Main tests plus1018 inherited same-SHA pure cases, frozen byte-identical rebuild and20 readonly SQL classes retained.124 Source methods/40 Source files. Zero real HP/role/IO/Main/game/MySQL qualification.'
 ]
 remaining=['NPC direct next: nonpositive erase routing is staged; complete signed HP/healing/undead formulas, mana-only and other direct/accepted skill mutations, NPC ALT_ATKMSG diagnostics, upstream full NPC math/actor-stat/AI/factory and controlled/bot/ground producers. Qualify real role waits, recast/source retirement/final IO/Main after all program work; all58 materials/controls/formal0.6.71 composition and concentrated same-SHA backend/readonly Chinese MySQL/cursor acceptance remain open.',*old['remaining']]
if signedrecovery:
 scope=[
  'Audit the real signed path rather than assume the NPC FIFO lacks support: existing CalcTicket raw/earth/defense admission and NativePlayerVitals already retain signed values with long-safe HP cap/floor/GM behavior. The actual missing producer was shared CombatPlayerRecovery.receive, whose Source BATTLE negative branch wrote playerHp directly and emitted immediate HP/ratio outside the original FIFO and final role/life guard.',
  'Wrap only that shared original private receive entry. Complete original body remains as a bounded sibling for positive, WORLD and unloaded Source. Proven BATTLE negative input captures target/caster bindings before endpoint pair waits; existing Context bindings are authoritative, absent members are not freshly adopted. Reuse existing original raw Members, earlier physical Stamp or magic Snapshot source-life when present. Under ordered target/caster action locks, existing OPEN admission and commitPair, revalidate and route through existing display/raw Plan, original signed CalcTicket and same target FIFO. No new direct HP/IO, queue, worker, scheduler, TLS, actor, cost or RNG.',
  'Only target Member is retained by queued raw Guard. Caster role/source/account/life/room retirement rejects a new submission but never revokes already admitted target HP. Target role/source/account/life replacement, disconnect, STOPPED and Main reject at existing HP/final-frame boundaries; DRAINING permits admitted work while preventing new input. Root source snapshots retain old life rather than capturing a replacement after a prior wait. Generic outer cast target-life/first-wait coverage, all healing/undead formulas/producers and real bound pairs/output remain open.',
  f'{count} original methods independently inverted plus1 complete original fallback sibling; all other parent archive entries are byte-identical.201 no-init classes,31 new detached role/life/target-only FIFO and actual admission tests plus1055 inherited same-SHA pure contracts.420 additional arithmetic comparisons use an exact compiled frozen Source HP block, covering signed32-bit boundaries, minimum HP and GM.124 Source methods/40 files,20 SQL classes retained and frozen byte-identical rebuild. Zero real HP/heal/role/pair/IO/Main/game/MySQL qualification.'
 ]
 remaining=['Signed recovery next: shared BATTLE negative producer now joins original FIFO with original target-only queued authority and inherited input provenance. Complete every outer cast prewait target-life capture, all healing/undead reversal/type/immunity/numerical producers and native permission/cost/RNG/rollback paths; qualify actual pair waits, HP/GM/fatal reversal, caster retirement, target role ABA and receiver/Main only after program completion. Continue NPC accepted/direct mutations/ALT_ATKMSG/upstream stats-AI-factory, all58 materials/controls/formal0.6.71 composition, then concentrated same-SHA backend/readonly Chinese MySQL/cursor acceptance.',*old['remaining']]
if healingcast:
 scope=[
  'Inspect actual single healing, Group Healing and Natures Blessing roots: all three still contain direct PC HP stores and immediate native healing outputs, so the preceding shared signed-recovery fix did not cover their positive-heal producers. This stage freezes and validates original participants before original target waits/cost/RNG; it does not claim those direct HP stores already use FIFO or accepted final output.',
  'Wrap three actual roots with complete original siblings. Capture original caster binding/life/pose before constructing any new Context; preserve existing Context/raw/physical/magic provenance. Freeze bounded original room participants before existing player-action lock waits. Reuse actual player locks with the existing32-attempt try/release pattern and original NativePlayerActionScope; acquire all locks before lifetime admission, without an outer Hub monitor. Existing commitPair owns original action and material completion; actual native method bodies, cost/RNG/rollback and controlled-NPC paths remain inverse-retained.',
  'Single selected PC is checked immediately after actual resolution and before native cost/RNG; null/NPC retains original kind validation. Only two potential target lists (not visual recipients) are filtered against original Member/role/source/account/life/pose, retaining native order and each original party/clan/range/type/Ctrl predicate. New entrants are not adopted; changed secondary participants are individually excluded. Existing shared signed-member entry reuses exact cast Member instead of capturing a replacement life. OPEN/Main/caster proof gates body entry and nested reuse; original finish/failure/exception cleanup clears participants and Context field without a new queue/worker/timer/TLS/SQL.',
  f'{count} original methods independently inverted plus3 complete root siblings,203 JVM no-init classes,32 new detached participant/primary-secondary/pose/member-ABA and actual unbound lock/scope success/reject/exception cleanup cases plus1086 inherited same-SHA pure cases and420 inherited exact Source HP arithmetic comparisons. Frozen byte-identical rebuild,124 Source methods/40 files and20 readonly SQL classes retained. Zero real target-wait/cost/RNG/heal/HP/IO/Main/game/MySQL qualification. First Hub-discovery lifetime coverage and full outer-cast semantics remain open.'
 ]
 remaining=['Healing cast next: original single/group/nature participant guards are staged, but their three direct PC HP stores and native preview HP/ratio outputs still need Source requested-heal routing through original signed FIFO and accepted/final receiver chronology. Preserve original water-life/status effects, cost/RNG/rollback, Ctrl/party/clan/range/LOS/type and per-secondary semantics; complete first Hub-discovery lifetime proof, controlled/NPC/bot healing and undead/formula paths. Continue other NPC mutations/ALT_ATKMSG/stats-AI-factory/all58 materials/controls/formal0.6.71 composition, then concentrated same-SHA backend/readonly Chinese MySQL/cursor acceptance.',*old['remaining']]
if healinghp:
 scope=[
  'Three actual single/group/nature PC healing stores now have a conditional Source-cast branch that submits the original requested heal as signed HP through existing CalcTicket/raw Plan/target FIFO. It bypasses native addExact/IADD preview arithmetic only for the marked Source cast; complete old expressions stay as unscoped/WORLD fallback. Original zero-heal direct effects stay, without a fabricated HP event. Three existing negative-healing/reversal damage ports use actual Source PC magic true/false flags from an independently compiled frozen L1MagicPc.commitPc oracle; positive reversal retains ordinary accepted damage figures and original death handling.',
  'The Plan retains the same original target Member and no queued cast/Context. Caster retirement/cast release does not revoke admitted target HP; target role/source/account/life and Main/STOPPED remain original raw/final-frame guards. At the common display damage entry, a detached Recovery proof records original negative input on one volatile original Plan field without overwriting NPC accepted/direct mutation metadata. The actual accepted BATTLE completion consumes that proof only for nonfatal nonpositive outcomes: changed HP sends existing safe HP/ratio, unchanged/rejected sends none; healing never overwrites native combat/last-damage state. Positive/fatal/unmarked/WORLD keep the complete original completion.',
  'Only native PC healing HP preview packets and the three PC ratio ports are suppressed in the marked cast. Exact preview-note/opcode classification retains group HP cost, MP and unrelated output; companion/NPC ratio packets and the single-heal bot ratio call are retained. Other original sends keep the original sender chain. Original party/clan/Ctrl/range/LOS/type admission, RNG, status/water-life consumption and cost/rollback remain independently inverted. No new HP lane, worker, timer, TLS, actor registry or SQL.',
  f'{count} independently inverted original methods,205 JVM no-init classes,42 new actual detached Plan/Guard/input-proof/nonpositive completion/cast-release and preview-cost contracts plus1118 inherited same-SHA pure contracts and420 inherited exact Source HP comparisons; frozen byte-identical rebuild.125 Source methods/42 frozen Source files and20 SQL classes retained. One volatile original Plan proof and2 helper types; zero real heal/HP/cost/RNG/IO/Main/game/MySQL qualification. First Hub discovery, full healing/undead/type/immunity/formulas and other completion consumers remain open.'
 ]
 remaining=['Healing HP next: three principal PC healing/reversal ports and common proven negative-input HP completion are program-wired; qualify actual rejection/accepted HP/capped unchanged heal/negative amplification/fatal reversal, original status consumption, source retirement/target role ABA and final receiver/Main only after program completion. Complete exact healing/undead/type/immunity/numerical inputs, first Hub-discovery life proof and NPC/controlled/bot/periodic/other producers. Continue NPC ALT_ATKMSG/other mutations/upstream stats-AI-factory, all58 materials/Source controls/formal0.6.71 isolated composition, then concentrated same-SHA backend/readonly Chinese MySQL/real cursor acceptance.',*[item for item in old['remaining']if not item.startswith('Healing cast next:')]]
if healingformula:
 scope=[
  'Replace the hardcoded native healing INT bonus only in the proven Source healing cast with the actual immutable Source INT growth snapshot. One bounded SELECT on Role INT growth uses the same borrowed readonly/non-autocommit connection already passed from real CombatDatabase.load to NativeSkillTargetMetadata.read. The snapshot is installed only after original skill metadata validation succeeds; no new connection, transaction ownership or write exists. Dedicated Chinese healing bonus is uncapped, INT<1 is-2, above127 uses127, missing optional column uses min(magic,10), and missing key follows actual frozen BaseStatTable getters. Read/schema errors propagate through existing startup validation; real reload remains deferred.',
  'Six scoped original formula prefixes cover shared healing INT lookup, two negative-damage status functions and three original lawful revisions. Two original group/nature per-die sums and one original Nature water-life multiplication use Java int wrap in the marked cast; every original die callback, order/count/bounds/fixture validation and original native/unmarked/WORLD body remains independently inverted. The TYPE_HEAL order and negative arithmetic shifts, including erosion and32-bit edge behavior, match exact compiled Source statements. Default user-cast leverage10 is retained; all other producers/leverage/undead/type/immunity/effective-stat paths remain open.',
  f'{count} independently inverted original methods,213 JVM no-init classes,25 new actual immutable-growth/Chinese readonly JDBC proxy/optional-column/scope/native RNG/fallback contracts plus1160 inherited same-SHA pure cases,160 exact Source signed-status and30 lawful boundary comparisons, plus420 inherited Source HP comparisons. Frozen byte-identical rebuild,129 Source methods/43 frozen files and20 SQL classes retained. No new original fields; only immutable scalar growth/helper types. Zero actual database/INT-row reload/actor/heal/HP/role/IO/Main/game qualification.'
 ]
 remaining=['Healing formula next: actual readonly Source INT growth and scoped signed/lawful arithmetic are now staged; qualify real Chinese growth schema/same-transaction load/reload and edited bonus with real equipped INT only after all program work. Complete full effective-stat and other leverage/producer paths, Source undead/monster-only/tower-door/target-type/immunity semantics, first Hub-discovery lifetime and NPC/controlled/bot/periodic consumers. Continue NPC ALT_ATKMSG/mutations/stats-AI-factory/all58 materials/Source controls/formal0.6.71 isolated composition, then concentrated same-SHA backend/readonly equipped Chinese MySQL/real cursor acceptance.',*old['remaining']]
if healingtype:
 scope=[
  'Correct the actual controlled-target class at three existing undead reads: one actual single native body and both existing group/Nature companion branches. In the already proven Source healing cast PET/SUMMON/TAMED use undeadType0, matching Source L1PetInstance/L1SummonInstance rather than their reused template undead1/3. The original selected MassSlowCompanionTarget/exact owned actor final Kind is used; no fresh target/template/SQL/snapshot lookup occurs.',
  'Only ClientSession and CombatHealing change. One stateless helper has no fields or allocations and does not own HP, RNG, cost, locks, admission, Main, output, workers or services. Natural/null/ARENA/unmarked/WORLD behavior and every original operation around all three reads are independently inverted. Existing controlled accepted/lifetime gates remain; full positive-heal commit/output lifecycle is not qualified by these type contracts.',
  f'{count} independently inverted original methods,214 JVM no-init classes,40 new unbound actual-kind/actual-selected-record/scope/fallback contracts plus1185 inherited same-SHA pure cases.112 signed type-delta comparisons execute the exact frozen Source actual-class and undead conversion statements against the actual original native HealingSpell model;84 controlled cases plus28 modeled Monster arithmetic regression cases do not qualify unknown native ARENA/factory types.131 Source methods/45 frozen files,20 readonly SQL class consumers retained and frozen byte-identical replay; zero actual actor/heal/HP/role/IO/Main/MySQL/game qualification.'
 ]
 remaining=['Healing type next: controlled template-undead misclassification is corrected in the three actual reads. Complete natural Quest/Time actual Source class/factory provenance, guardian tower/door negative-damage blocking and positive erosion, unknown ARENA/bot kind, full controlled positive-healing accepted HP/final output and periodic consumers. Keep first-Hub lifetime, all materials/Source/NPC factory-AI and formal isolated composition open; concentrate real same-SHA backend/Chinese readonly MySQL/role/cursor validation only after programming is complete.',*old['remaining']]
if controlledhealing:
 scope=[
  'Capture original eligible controlled actors before the original healing player-lock wait, inside the original run try/finally. Reuse actual Context NPC admission and original owner Member with exact owner/actor life, canonical bag, placement/master/scene/pose and initial attachment. One volatile Object field is bounded by the original Cast and cleared on close. No fresh actor by object id or later owner/role is adopted.',
  'The three actual single/shared-group/Nature heal call sites use original source and owner action locks, original admission, OPEN/Main/source gate and exact original target proof, then the original synchronized companion HP body cloned with one long-safe positive sum. Original full heal remains for unmarked/WORLD/ARENA; no extra RNG, cost, status, defense, attack feedback, FIFO, worker, TLS, registry or SQL is introduced. Source pet/summon positive heal itself is synchronous rather than the natural-NPC damage FIFO.',
  'Only original healing preview ratios for the frozen controlled ids are suppressed. A changed accepted HP captures detached original receivers and target proof, after releasing actor/Hub monitors, and uses the existing scoped output service. Ephemeral original send context carries the typed output in the existing generic marker; actual final frame checks owner/actor/receiver identity/life, bag, placement/pose, actual HP and current max denominator, source hpbar, screen, DRAINING/STOPPED and Main. It does not capture Cast/Context or invent an attack event. Original other healing visuals/cost messages and non-controlled output are retained.',
  f'{count} independently inverted original methods,218 JVM no-init classes,28 new unbound proof/lock/admission/role/life/bag/max/accepted-meter/final-hook contracts plus1225 inherited same-SHA pure cases;90 exact frozen Source pet/summon positive HP arithmetic comparisons plus inherited112 type/160 status/30 lawful/420 HP comparisons.133 Source methods/45 files,20 readonly SQL consumers and frozen byte-identical replay. Unbound player fixtures now initialize the real existing completion State, eliminating previously logged null-state cleanup errors. Zero actual bound actor/heal/HP/role/IO/Main/MySQL/game qualification.'
 ]
 remaining=['Controlled healing next: actual positive HP calls and detached accepted-meter final gate are now programmed. Complete bound same-SHA role/retirement/Main/HPBAR acceptance after all code work; full controlled signed/damage/owner-meter and periodic paths, natural Quest/Time factory/type, tower-door positive erosion/negative healing, unknown ARENA/bot and first-Hub discovery remain open. Continue all materials/Source/NPC factory-AI and formal0.6.71 isolated composition before concentrated backend/Chinese readonly MySQL/real cursor validation.',*old['remaining']]
if firstplayers:
 scope=[
  'At the existing original Context in-Hub player binding capture, Source BATTLE freezes actual life BEFORE invoking the original binding capture exactly once. Original caster pre-Hub binding, roster, loop/order, original IdentityHashMap.put and all original locks/try/finally remain. First occurrence is retained; a death/revival between token and binding is stale rather than newly adopted. Unloaded/WORLD executes its original binding call without new life ownership.',
  'One volatile Object on the original Context owns a constructor-local bounded identity builder, sealed into an immutable mapping at successful original constructor return. The original Context copy shares the same snapshot. No helper snapshot holds a Context/Hub/cast back-reference. Only the shared signed member final fresh-construction fallback now borrows the original Member or returns null for missing/mismatched players. Existing healing cast/raw ingress/doll stamp/magic source precedence and original no-Context/unmarked fallback are retained. The healing target census already consumes that actual shared member path.',
  f'{count} independently inverted original methods,221 JVM no-init classes,26 new first-discovery/role/source/account/revival/duplicate/immutable-copy/actual-signed-healing-priority contracts plus1253 inherited same-SHA pure cases. Inherited90 Source pet/summon HP,112 type,160 status,30 lawful,420 HP comparisons;133 Source methods/45 frozen files,20 readonly SQL consumers and frozen byte-identical replay. No new HP/cost/RNG/queue/worker/TLS/global registry/SQL ownership, and zero actual bound Hub discovery/HP/role/IO/Main/database/game qualification.'
 ]
 remaining=['First player next: original in-Hub life-before-binding snapshot and shared signed/healing fallback are programmed, but actual Hub/role/revival contention and full first-discovery qualification are deferred. Audit other raw/doll/magic/factory consumers that still build their own later Members, and complete type/immunity/natural NPC factory-AI/tower-door-bot/periodic/full controlled owner-meter/all materials/Source/formal0.6.71 isolated composition before concentrated real backend/Chinese readonly MySQL/cursor acceptance.',*old['remaining']]
if playerfactories:
 scope=[
  'Ten original Context-bound factory owners now borrow original first player proofs. The Context caster freezes life before its original binding capture and before waiting for Hub; complete roster is sealed/inherited before the existing magic snapshot is produced. Original final seal/copy remain idempotent. Five ordinary Member constructors cover raw roster/root/explicit peer, doll selected target and Fatality target. Original accepted-frame target/source constructors remain separate and unchanged.',
  'Two explicit magic/controlled Actor factories, three natural/material aim capture calls and shared visual Actor.capture use original life/pose; earlier raw/cast Member or doll/magic Actor object keeps precedence, including accepted-death Member token. Material primary player initial token now borrows original discovery, while NPC token and every later live validation remain original. Missing/mismatched discovery yields a non-null explicitly stale legacy Member/Actor; no new live token is adopted. WORLD/unmarked/no-Context original bodies and supplied operands are retained.',
  'The raw explicit-peer function now borrows its existing Action Context solely during the original immediate computeIfAbsent. No extra field or queued function is added; the original static lambda and bootstrap are independently reconstructed for inverse comparison, and the actual bound LambdaMetafactory shape is executed on detached metadata. Existing raw/cast/doll/magic ingress priority and approved accepted-death token are retained. No HP, RNG, cost, defense, relationship, Ctrl, admission, Main, output service, worker, queue, TLS, registry or SQL ownership is added.',
  f'{count} independently inverted original methods,227 JVM no-init classes,34 new actual factory/shared-Actor/peer-bootstrap/first-life-pose/accepted-token/priority/WORLD contracts plus1279 inherited same-SHA pure cases, frozen byte-identical rebuild and inherited90/112/160/30/420 Source comparisons.133 Source methods/45 files and20 readonly SQL consumers retained. Zero actual bound cast/HP/role/IO/Main/database/game qualification; exhaustive remaining Member constructor census is attached to next-producer contracts.'
 ]
 remaining=['Player factory next: ordinary Context-bound source/member/material-player factories are wired, but accepted-frame and explicit-life/doll/tick/master/cube/source ingress constructors and their own pose ordering still need independent audit. Natural/controlled NPC first discovery, full materials/Source type-immunity/factory-AI/tower-door/bot/periodic/full owner-meter/formal0.6.71 isolated composition remain open. Run bound Hub/cast/role/accepted HP/IO/Main/same-SHA backend/readonly Chinese MySQL/cursor acceptance only after program work is complete.',*old['remaining']]
if acceptedplayers:
 scope=[
  'The existing actual HP acceptedState tail freezes event-owned participant values once, only while its original Queue vitalLock is held. NativePlayerCommit drain, HP math/enqueue/unlock, original completion/CAS/admission/queues remain byte-identical. One volatile Frame Object holds only detached immutable Participant values, not a Frame/Context/calculator/plan/Member back-reference. Canonical target acceptance life comes from this event Plan.targetLife or exact raw Guard acceptedLife; target pose is frozen at HP acceptance, source life/pose stays at its original explicit guard/Actor proof.',
  'The two actual accepted raw Member factories copy frozen accepted participant values and approved corpse token; later revival, changed shared Guard or new positions cannot replace them. Unsupported/world cases preserve full original constructors and are explicitly unqualified. An added Member constructor independently retains all original field initialization with two explicit pose operands. Five original doll/signed Actor-life conversions now keep original Actor pose; WORLD/unsupported operands remain original.',
  'The primary canonical NPC target entry freezes Source life/pose before its original binding capture and subsequent Hub lookup. Fatality pre-Hub actor shares one such participant between original binding and actor assignment; original side-effect/cost/RNG/locks/Main and world fallback remain. Other NPC viewer/master, cube and natural/controlled discovery/provenance cases are not inferred complete from these sites.',
  f'{count} independently inverted original methods,230 JVM no-init classes,28 new actual vital-lock/frozen-frame-value/role-revival/accepted-corpse/pose/explicit-Actor/ingress/fallback contracts plus1313 inherited same-SHA pure cases. Frozen byte-identical replay, inherited90/112/160/30/420 Source comparisons,133 Source methods/45 files and20 readonly SQL consumers retained. Zero actual bound HP/role/IO/Main/database/game qualification; updated exhaustive constructor census remains attached.'
 ]
 remaining=['Accepted participant next: supported accepted raw target/source values are frozen under original HP lock and explicit Actor poses/primary NPC-Fatality ingress are wired. Qualify real HP/role/revival/Main/IO only after all code. Unsupported accepted sources, NPC secondary viewer/master ingress, cube and natural/controlled NPC first discovery, full materials/Source type-immunity/factory-AI/tower-door-bot/periodic/owner-meter/formal0.6.71 isolated composition remain open.',*old['remaining']]
if npcorigins:
 scope=[
  'A bounded Source-only NPC origin sibling holds the already existing Hub across original room-members discovery and life-before-binding. Reuse original identity map, original binding call/order and earlier primary Member; duplicates cannot replace first life and mismatches produce an explicitly stale entry. No player-action-lock acquisition occurs under Hub. Full original capture is retained unchanged for unloaded/WORLD rather than sharing the new Source loop.',
  'Source companion Ref capture freezes owner life/binding/pose before the original Ref/pet-monitor wait, then copies the original Ref with one final Object holding weak owner provenance. Existing eight record identity components, equality/hash, constructor, actual NPC owner token/id/epoch/room/map/instance and all original capture/live behavior are retained. The actual controlled NPC ingress Member now copies old owner life/pose; changed role/revival/cleared weak owner cannot be newly adopted. Unsupported original Ref metadata retains native fallback and remains unqualified.',
  'New Ref metadata retains no strong ClientSession, Frame, Context, Ref or calculator back-reference, registry, worker, clock, queue, SQL, HP, RNG or cost ownership. Already admitted HP and general Ref-live validation do not consume the new master marker and remain byte-identical; source retirement does not add a cancellation gate to those existing paths. Owner birth-lifetime and other manual/unsupported Ref capture paths remain open.',
  f'{count} independently inverted original methods,233 JVM no-init classes,27 new actual Hub-ownership/original-map/primary-priority/weak-master/immutable-Ref/role-revival/WORLD contracts plus1341 inherited same-SHA pure cases. Frozen byte-identical replay, inherited90/112/160/30/420 Source comparisons,133 Source methods/45 files and20 readonly SQL consumers retained. Zero actual bound Ref capture/AI/HP/role/IO/Main/database/game qualification; Member/Ref constructor and capture caller census updated.'
 ]
 remaining=['NPC origin next: Source roster/secondary first-life and weak immutable original master Ref ingress are programmed, but bound Hub/pet capture/retirement/AI/HP/IO/Main acceptance is deferred. Audit owner birth-lifetime and remaining manual/unsupported Ref factories, NPC actor first-discovery/AI-factory/type-immunity, cube/full materials/Source/tower-door-bot/periodic/owner-meter/formal0.6.71 isolated composition before concentrated real backend/readonly Chinese MySQL/cursor acceptance.',*old['remaining']]
if npcfirstlife:
 scope=[
  'Six initial-life ports in controlled target, raw NPC, material Aim and controlled-healing capture borrow the original Context NPC discovery. Natural quest/time use original Npc.epoch; controlled actors use original Ref.owner life token, not the Ref.',
  'Original identity-map entry, actual actor, Ref kind/id and original master binding must match. Missing/unsupported Source discovery is explicitly stale. Controlled display master life borrows older Context player proof with existing healing/raw/doll/magic precedence.',
  'Only initial metadata changes. Original accepted death seals, live revalidation, competition/Ctrl/type/range/LOS/cost/RNG, paired locks/admission/HP/queue/Main and WORLD/unmarked fallback remain. One actor-free helper; no new original fields/workers/registries/SQL or queued Context.',
  f'{count} independently inverted methods,234 no-init classes,30 new detached actual-constructor/accepted-seal metadata contracts plus1368 inherited same-SHA pure cases and original frozen Source arithmetic comparisons. Bound AI/HP/role/IO/Main/DB/game0; full Source/discovery-factory qualification open.'
 ]
 remaining=['NPC first-life next: supported initial target metadata retains natural epoch/controlled Ref life and older master proof. Complete NPC actual first-discovery/factory/AI/type immunity, owner birth-life/manual Ref/unsupported sources and all materials/Source/owner-meter/formal0.6.71 paths. Bound backend/readonly Chinese MySQL/cursor acceptance deferred until all program work complete.',*old['remaining']]
if materialfirstlife:
 scope=[
  'Five original initial-life fields in four actual Aim constructors borrow earliest Source Context participant/controlled Ref life. Existing negative29/152/153, mass-slow76, wind167 and weapon-buff148/149 selector and cost bodies remain original.',
  'Eleven early validation/position/final-frame gates reject source/primary/master/receiver role or life replacement. Original source/primary pose is retained through earlier members; receiver uses original role/life without adding observer, HP or movement restrictions. No fresh participant/token lookup or new lock is introduced by the helper.',
  'Three controlled final-frame List.contains ports now validate original native battleCompanion OR tamedCompanions attachment in proven Source Context only. WORLD/unmarked preserve original list-only behavior. All original Ctrl/area/secondary selection, costs/RNG/status/paired locks/HP/Main/DRAINING bodies remain; all-material and full lifecycle qualification remain open.',
  f'{count} independently inverted methods,243 no-init classes,39 new detached actual-Aim/early-valid/final-cost-frame/receiver/controlled-attachment contracts plus1398 inherited same-SHA pure cases and existing frozen Source arithmetic comparisons; frozen byte-identical replay. No bound cast/status/HP/role/IO/Main/DB/game cases.'
 ]
 remaining=['Material first-life next: Source negative/mass-slow/wind/buff primary/owner/receiver metadata and native single-companion output attachment are programmed. Complete every material secondary/line/area/corpse/type/immunity/cost/RNG path, remaining NPC actor discovery/factory-AI/manual-Ref/owner-meter/all controls and isolated formal0.6.71 composition. Bound backend/readonly Chinese MySQL/cursor acceptance remains deferred until all program work complete.',*old['remaining']]
if areaplayerproof:
 scope=[
  'The one original area filter bound Context.player method reference now borrows the original participant proof using the original LambdaMetafactory capture shape. Source-only initial roster token is not refreshed; earlier raw approved-death acceptedLife remains authoritative. Original duplicate/order/immutable result and observer/dead/movement recipient eligibility remain.',
  'Two actual pre-body Context.scene calls in nativeCompanionAreaCapturedRun and lambda$run$10 now require original source life/pose. All original 32 bounded lock attempts, participant scope, admission, Hub/population ownership and try/finally cleanup remain independently inverse-retained. No global Context.player/scene or accepted callback replacement.',
  'One field-free helper, no new actor/Context/Frame/registry/worker/SQL/token/lock ownership. A current archive method-reference census includes local invocation Handles separately from direct calls and excludes record field Handles; counts are locator evidence, not completed Source paths.',
  f'{count} independently inverted methods,245 no-init classes,25 new detached actual bound-stream/prebody Hub-TL cleanup/approved corpse/observer/WORLD contracts plus1437 inherited same-SHA pure cases and existing Source arithmetic comparisons; frozen byte-identical replay. Real bound cast/HP/role/IO/Main/DB/game0.'
 ]
 remaining=['Area roster next: original shared area predicate and two pre-body source checks retain first participants. Audit all remaining Context.player method-reference consumers separately, especially accepted freeze/output/cancellation/weapon/forest/wind paths; do not replace approved death/resurrection proofs with initial roster life. Full material semantics, Source controls, NPC factory-AI/manual Ref/owner-meter and isolated formal0.6.71 remain open; bound backend/readonly Chinese MySQL/cursor deferred until all code complete.',*old['remaining']]
if indirectplayerproof:
 scope=[
  'Ten remaining BATTLE-related bound Context.player references in accepted-area freeze, generic area output, controlled cancellation, blind, forest, wind and weapon-break output borrow original participants. The one remaining original reference belongs to unchanged classic-WORLD NPC physical members; no WORLD product/semantic expansion is claimed.',
  'A linked freeze target lacking a visual Plan now obtains immutable accepted participant values only inside the existing Queue vitalLock at the original once-only Frame proof construction. Existing known participant wins. Actual event constructor/link provenance, target binding and original Source Context are required; later completion/deathLife cannot replace the accepted epoch. No attacker provenance is invented and original HP/source-retirement/queue/CAS remain unchanged.',
  'Original freeze Batch target and cancellation Scope pet initial life borrow original first player/Ref values. Cancellation final frame checks original actor/master/receiver life and native single-or-tamed attachment; original observer audience, DRAINING/STOPPED/Main, fields, locks, costs/RNG/status cleanup and WORLD/unmarked bodies retained. One field-free helper; no new Frame/Context/queue/registry/worker ownership.',
  f'{count} independently inverted methods,257 no-init classes,38 new detached actual-bound-predicate/original Event-link/HP-lock accepted freeze/no-visual-Plan/cancellation final-frame/ABA/WORLD contracts plus1462 inherited same-SHA pure cases and existing Source comparisons; frozen byte-identical replay. Real bound cast/HP/role/IO/Main/DB/game0.'
 ]
 remaining=['Indirect roster next: ten applicable references now consume initial or exact linked-freeze accepted target life; the remaining original reference is classic-WORLD NPC physical members, byte-retained. Complete all direct selection/accepted/final-output paths, freeze queued-HP/input/source-effect policy, other materials/NPC factory-AI/manual-Ref/owner-meter/Source controls and isolated formal0.6.71 composition. Full BATTLE Source/memory/paired lifecycle and actual backend/readonly MySQL/cursor acceptance remain open and deferred until all code.',*old['remaining']]
if npcfactorytype:
 scope=[
  'Nine existing leaf constructors of native Companion/Quest/Time actors capture one final Object scalar Source factory type/template proof each. Delegating constructors do not recapture. Original controlled PET/SUMMON/TAMED and transferred controlled snapshots retain actual Source Pet/Summon class. Other births use original static NPC snapshot id/impl/undead without SQL, actor registry or retained mutable template/snapshot.',
  'Exact frozen NpcTable naming convention and 32 actual NPC subtype declarations distinguish Monster from MonsterGuard/other NPC, and Tower/Door signed treatment. Single NPC and shared owned/group/Nature existing signed arithmetic ports use actor birth proof in proven Source healing Context only; unknown/unmarked/WORLD originals remain. No RNG/cost/HP/lock/AI/body replacement.',
  'The independent type-only oracle compiles two exact relocated Source factory methods, Source class-header stub hierarchy and exact runSkill class/undead/tower-door statements. It does not execute original full NPC constructors/AI. Snapshot-restore/rollback factory provenance, guardian tower special root, bots/periodic/all immunity/factory-AI remain open.',
  f'{count} independently inverted methods,259 no-init classes,30 new detached actual companion-leaf/birth-row/reload/type/signed-adapter contracts plus1500 inherited same-SHA pure cases;896 exact signed Source factory-class/type comparisons plus inherited Source comparisons and byte-identical replay. 73 frozen Source files/133 bounded references;20 SQL consumers byte-retained. Actual bound NPC factory/AI/healing HP/role/IO/Main/DB/game0.'
 ]
 remaining=['NPC factory type next: scalar birth metadata and two Source healing math ports now classify all32 frozen NPC classes. Complete restoration/rollback provenance rather than re-resolving current template, missing/unknown factory policy, actual Source NPC construction/AI/hate/family, guardian tower special heal entry, bot/periodic/full immunity/other skills and owner-meter. Full Source/58 materials/formal0.6.71 isolation and actual backend/readonly Chinese MySQL/cursor remain open; real validation after all program work.',*old['remaining']]
if npcfactorymemory:
 scope=[
  'Three original Snapshot records add one final transient Object and one immutable constructor sibling each; original constructors/components/equals/hash/toString remain. Three actual actor Snapshot producers carry scalar original type; the cancellation replacement Snapshot copy carries its newly selected target Snapshot proof, not the prior NPC type.',
  'Four original Quest/Time Snapshot restore constructors borrow only the primary original Snapshot proof, with exact NPC-id match. They do not re-resolve current template or adopt temporary controlled transfer class. Known proof survives missing/currently changed resources; unknown/legacy/mismatched proof remains unknown rather than promoted.',
  'Actual controlled PET evolution may adjust copied scalar NPC id through immutable known Pet/Summon native kind, without mutating original birth stamp or reading a table. Generic arena id change cannot invent a new class. One field-free helper, no new actor/row/Context/Frame/registry/worker/SQL ownership; all original HP/life/inventory/lock/WORLD and replacement bodies independently inverse-retained.',
  f'{count} independently inverted methods,264 no-init classes,30 new detached actual-Snapshot/sibling/restore/release-tamed/reload/evolved-pet/identity/transient-schema contracts plus1530 inherited same-SHA pure cases and896 inherited Source type comparisons plus earlier Source arithmetic. Frozen byte-identical replay;73 Source files/133 bounded references and20 SQL consumers retained. Actual bound restore/rollback/AI/HP/role/IO/Main/DB/game0.'
 ]
 remaining=['NPC factory memory next: all current three Snapshot/four producer/four Quest-Time restore metadata paths preserve known original type without current-template adoption; record schemas/identity stay original. Qualify actual bound rollback/retirement/life/IO/Main later. Complete missing/unknown factory policy, exact first-template selection under startup/reload, actual full factory-AI/hate/family, guardian tower/bot/periodic/all immunity/other material Source paths and formal0.6.71 isolation before concentrated backend/readonly MySQL/cursor acceptance.',*old['remaining']]
if npctargetfailure:
 scope=[
  'Two final scalar birth flags IsTU/IsErase are copied from the same original NPC template row into an immutable Stamp constructor sibling. Original five record components and old constructor/identity stay unchanged; original Snapshot memory restore carries the exact flag-bearing proof.',
  'Exact Source NPC isTargetFailure compares actual Monster/Tower/Door/other factory classes. In known BATTLE Source Context, mass-slow keeps NPCs through original cost selection and filters failed targets at the original post-cost iterator; wind uses frozen class flags at its existing post-cost gate. Original PC/Ctrl/competition selection and all numerical bodies are retained.',
  'Natural Quest/Time turn-undead and mana-drain known class failures use the already existing original controlledFailure cost replay; supported numerical paths and controlled targets retain their old dispatch. No new cost, RNG, HP, life, locks, SQL, worker or metadata registry. Single slow/erase early-probability ordering and mass-slow self-primary semantics remain explicitly open.',
  f'{count} original methods independently inverted;266 no-init classes;37 new detached contracts plus1560 inherited same-SHA pure cases;4608 exact frozen Source NPC-class/flag/skill comparisons and896 inherited signed factory-type comparisons.73 frozen Source files/133 bounded references,20 SQL classes retained, frozen byte-identical replay. Bound cost/RNG/lifecycle/HP/role/IO/Main/MySQL/game0.'
 ]
 remaining=['NPC target failure next: complete remaining skill roots and actual Source failure-phase ordering, especially single slow/erase pre-admission RNG and mass-slow self-primary scope; qualify real bound failure-cost replay later. Two scalar birth flags and 8 ports cover only selected mass-slow/wind/natural turn-undead/mana-drain paths. Full factory/AI, all58 materials and807 applicable Source rows, all252 controls/formal0.6.71 isolation and concentrated same-SHA backend/readonly Chinese MySQL/cursor acceptance remain open.',*old['remaining']]
if negativephase:
 scope=[
  'Source normal casts commit material/action before runSkill and deduct HP/MP in useConsume afterward. The exact original two negative calculation blocks (slow29/entangle152 and erase153) now execute once after the original accepted-material/action and cancelMeditation boundary but before original MP debit, only in proven Source BATTLE. A scalar local freezes that branch decision; WORLD/unmarked paths enter their unchanged original block.',
  'Six original scalar ports select known Quest/Time NPCs through the original cost admission and test immutable exact Source birth class/IsErase before calculation and before secondary status consumers. Failed class targets consume no probability draw or target status mutation. Controlled failure, PC/Ctrl/competition, fixture range, full formulas, native costs/rollback/locks/life/Main/WORLD remain original.',
  f'{count} complete original ClientSession methods independently inverse-retained;267 no-init classes;23 new detached adapter contracts plus1597 inherited same-SHA pure cases;4608 inherited exact Source failure comparisons and896 factory signed-type comparisons;73 Source files/133 bounded references/20 SQL classes, frozen byte-identical replay. Actual bound material/admission/RNG/HP/life/role/IO/Main/database/game0.'
 ]
 remaining=['Single-negative phase next: the exact original RNG blocks for29/152/153 now occur after material/action acceptance and before MP debit, with immutable NPC class failure routing. Complete all upstream target/Source numerical/status semantics, actual bound admission/rejection/rollback/life/IO validation later. Mass-slow self-primary/secondary membership and other skill roots/full NPC factory-AI/58 materials/252 controls/formal isolated composition remain open; defer backend/Chinese MySQL readonly/cursor until all program updates complete.',*old['remaining']]
if massselftarget:
 scope=[
  'Exact frozen Source self-specific type/targetTo clauses admit self for probability MassSlow, and makeTargetList adds primary before visible secondaries. Three original ports now allow a coherent proven Source BATTLE self-player or original owned-controlled primary to preserve its selected calculation lists instead of replacing all of them with empty iterators.',
  'One field-free synchronous adapter checks the original Aim/context/owner/player or exact original controlled Npc map record and accepted metadata shape. It moves only the first already selected caster to the front for self-PC primary; absent/filtered caster is not adopted, remaining order and duplicates remain. Existing NPC class-failure iterator remains, controlled primary failure remains, and no cross-role ForcedCombat permission is broadened.',
  'Actual original private scalar predicate retains native phase/HP/invisibility/radius checks after the self exception. Full Source visibility/earlier class-zone-ghost gates are not qualified by the self-specific oracle. Original cost/RNG formulas, selected actors, pair locks, memory/Main and WORLD/unmarked branches independently inverse-retained; no new registry/worker/SQL/HP/life authority.',
  f'{count} original methods independently inverted;268 no-init classes;26 new actual detached predicate/iterator/selection contracts plus1620 inherited same-SHA pure cases;4608 inherited exact Source failure comparisons and896 factory signed-type comparisons,73 Source files/133 bounded references/20 SQL classes and frozen byte-identical replay. Bound material/probability/mutation/role/life/IO/Main/database/game0.'
 ]
 remaining=['MassSlow self-target next: proven self-PC and original owned controlled primary no longer erase all selected secondaries; actual predicate and selected-primary-first iterator are wired. Complete all Source visibility/earlier class-zone-ghost/corpse/primary-secondary membership and numerical/status semantics, other skill roots, NPC factory-AI/58 materials/252 controls/formal isolated composition. Actual bound admission/rollback/mutation/role/life/IO/Main and concentrated backend/readonly Chinese MySQL/cursor remain deferred until program complete.',*old['remaining']]
if nonetargetrequest:
 scope=[
  'Fixed Source target=none overwrites packet target ID and coordinates with the user before target selection. Forty-five original typed ClientSession spell parsers now project only this metadata shape onto the already captured caster binding and earlier source Member pose. Row/column, skill, packet and original typed spell remain; ordinary pointed targets and Ctrl are unchanged.',
  'Four original MassSlow/negative/Wind/remaining-material helper run methods also project the request immediately after their existing initial Context capture and before original Aim/target lookup. No Context construction/reordering, fresh role/life/pose capture, NPC discovery or ThreadLocal installation. Missing/closed source proof remains original without adoption; WORLD/unmarked/missing metadata keeps exact request identity.',
  'Fixed Source only rejects empty target lists for NPC casters; PC normal casts may consume with empty targets. Pure NPC class rules cannot be added as an unconditional SelectedPlayerIntent rejection before material admission. This exact Source contract is frozen for the next target-selection/cost work; class/ghost/zone/hidden/corpse and all packet-kind/root header semantics remain open.',
  f'{count} original methods independently inverse-retained;269 no-init classes;22 new actual immutable request/old-source/role/revival/closed contracts plus1646 inherited same-SHA pure cases,4608 inherited exact Source failure comparisons and896 signed factory comparisons;73 Source files/133 bounded references/20 SQL classes and frozen byte-identical replay. Actual bound cast/selection/cost/RNG/HP/role/IO/Main/database/game0.'
 ]
 remaining=['Source target-selection next: target=none projection covers45 typed attack-request parsers/four original captured-context pre-Aim paths, not every packet kind or all58 material roots. Complete object/targeted spell packet kinds and individual self/header/geometry routes; NPC class veto must preserve PC empty-list normal material/action/HPMP consumption instead of early free-cast rejection. Full Source class/ghost/zone/hidden/corpse/primary-secondary/factory-AI/252 controls/formal isolated composition and concentrated same-SHA backend/readonly Chinese MySQL/cursor acceptance remain open.',*old['remaining']]
if objecttargetrequest:
 scope=[
  'Thirty original typed object-request parsers now project character target=none onto the original caster Binding only with an existing earlier source Member. Original row/column/skill and packet remain. Five exact Source item-selector cases preserve the raw object ID even when the separate Source character target is self: create magical weapon73, bring stone100, blessed armor21, enchant weapon12, shadow fang107.',
  'Two original post-Context object requests (FireWeapon/WindShot targets and controlled cancellation) now resolve before original Aim/owner lookup; one original Blind captured-context attack request uses the existing none projector at the same boundary. No constructor/discovery/order/locking changes; ordinary pointed/Ctrl/competition, WORLD/unmarked/missing/closed-proof and original cost/RNG/life/Main paths retained.',
  'Exact frozen Source target-none and independent raw item/bookmark switch execute as a separate oracle; item ID is not confused with character target. Coordinate/clan/self/summon/bookmark packet kinds and all per-root self/header/geometry/selection/NPC-empty-PC-cost semantics remain separate open work. This does not qualify real inventory, role or caster lifecycle races.',
  f'{count} original methods independently inverse-retained;270 no-init classes;39 new detached Source character/item split/preAim/role-life contracts plus1668 inherited same-SHA pure cases;4608 inherited Source failure comparisons and896 signed factory comparisons,73 Source files/133 bounded references/20 SQL classes and frozen byte-identical replay. Actual bound inventory/cast/selection/cost/RNG/HP/role/IO/Main/database/game0.'
 ]
 remaining=['Object request next:30 typed object parsers plus two original object and one Blind post-Context/pre-Aim paths now keep original caster proof and five raw item-selector IDs. Along with previous45 attack parsers, this is request projection only, not full75 spell models/58 materials qualification. Complete remaining coordinate/clan/self/summon/bookmark packet meanings and individual self/header/geometry/status/class/ghost/zone/hidden/corpse/empty-PC cost routes, full NPC factory-AI/252 controls/formal isolated composition, then concentrated backend/readonly Chinese MySQL/cursor acceptance.',*old['remaining']]
if areaclassselection:
 scope=[
  'Twelve original per-target probability/d100 iterators in four area roots (silence, wind, BATTLE sleep and BATTLE mass-slow) now apply reject-only known Source class clauses after original material/action acceptance. CFG proof includes exception edges; audience-only iterators and all original cost/MP/RNG/numeric/HP/locks/Main/WORLD bodies remain. This preserves Source PC empty-list consumption instead of rejecting the cast before material admission.',
  'Known Doll, unbreakable maxHP0/1 Door, natural-primary PC/Pet/Summon exclusion, non-Guard NPC-primary Guard exclusion, exact eight none+attack NPC classes and class-specific tame/zombie/restore clauses match91476 frozen Source class-only combinations. Operational ports cover selected four original models only; earlier self/zone/ghost/LOS/hidden/dead/target-bit/PK checks and full skill/type semantics are not replaced or qualified by this oracle.',
  'Existing Quest/Time Snapshot class proof must match its original actor birth value; legacy or same-ID replacement metadata is not freshly adopted. Actual BattleSleepTarget wrappers are unwrapped read-only and returned unchanged; original selected order/duplicates/unknown fallback and known controlled kind remain. One field-free policy and synchronous bounded iterator, no new actor discovery/snapshot/life capture/Context/Frame registry/worker/SQL ownership.',
  f'{count} complete original methods independently inverse-retained;272 no-init classes;30 new detached class/Snapshot/wrapper/order/memory contracts plus1707 inherited same-SHA pure cases;91476 new exact Source class-only comparisons plus4608 inherited failure/896 factory comparisons;73 Source files/133 bounded references/20 SQL classes and frozen byte-identical replay. Actual bound cast/cost/RNG/mutation/HP/role/life/IO/Main/database/game0.'
 ]
 remaining=['Area class selection next: twelve admitted calculation-loop ports now apply exact known Source class clauses without changing audience iterators or PC empty-list cost. Complete single-target and other area/damage/healing roots, all earlier self/zone/ghost/LOS/hidden/dead/target-bit/PK and full status/numerical/type semantics; qualify actual bound failure/cost/RNG/HP/role/life/IO/Main later. Full58 materials, NPC factory-AI, all252 controls/formal isolated composition and concentrated backend/readonly Chinese MySQL/cursor acceptance remain open.',*old['remaining']]
if damagehealingselection:
 scope=[
  'Thirteen original damage/healing calculation iterators across targeted area, centered area, group healing and Nature roots now apply known Source class-only clauses before their original per-target rolls. The original MP debit dominates these iterators including exception paths. Audience/HP-application-only loops, all original formulas/RNG/HP/FIFO/cost/locking/Main/WORLD bodies remain independently inverse-retained; no new material or HP algorithm.',
  'Original centered and targeted controlled wrappers expose their existing Snapshot without constructing a new one; same-kind noncontrolled replacements cannot supply original birth proof, while known actual Pet/Summon evolution retains its fixed class and original actor birth value. Selected raw object order/duplicates/unknown fallback stay original.',
  'One already decoded targeted-area request projects none at original body entry, and one original Action constructor data port normalizes none primary using the existing captured source proof. This avoids an unrelated original NPC primary gating a self cast; physical and pointed requests, original argument arrays and constructor/target-discovery/lock order are unchanged. Scope/member/all other root selection and full Source lifecycle remain unqualified.',
  f'{count} original methods independently inverse-retained;274 no-init classes;33 new detached wrapper/birth/evolution/none-primary/order contracts plus1737 inherited same-SHA pure cases;91476 inherited Source class-only/4608 failure/896 factory comparisons;73 Source files/133 bounded references/20 SQL classes and frozen byte-identical replay. Actual bound cast/cost/RNG/HP/mutation/role/life/IO/Main/database/game0.'
 ]
 remaining=['Damage/healing selection next: thirteen post-original-MP per-target roll ports and two original none-primary data ports are wired; original HP/application/audience loops remain. Complete single-target class/failure costs, direct guardian-tower/bot branches and missing factory metadata, all early/full isTarget/visibility/LOS/hidden/corpse/PK/type/status/math semantics, all58 materials/NPC factory-AI/252 controls/formal isolated composition, then concentrated backend/readonly Chinese MySQL/cursor acceptance. Actual bound role/life/IO/Main/material/RNG/HP remains deferred.',*old['remaining']]
remaining=list(dict.fromkeys(remaining))
v=dict(status='program-updated-doll-commit-order-flags-display-partial-lifecycle-full-Source-open',runtime=runtime,build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],goal=inv['goal'],system_goal=pin(goal),previous_program_progress=frozen,previous_qualified_runtime=old['previous_qualified_runtime'],implemented_scope=scope,program_checks=dict(JVM_no_initialization_classes=13,original_methods_independently_inverted=count,byte_identical_frozen_rebuild=True,existing_changed_class_entries=3,new_helper_class_entries=3,new_workers_TLS_registries=False),new_game_runtime_cases=0,new_database_connections=0,actual_role_and_cursor_validation_deferred_by_user=True,readonly_SQL_consumers_byte_retained=dict(base=sqlbase,program=runtime,entries=retained),previous_real_MySQL_evidence=old['previous_real_MySQL_evidence'],remaining=remaining,all_owned_JVMs_terminal=True,editable_inputs=[pin(p)for p in inputs.iterdir()],protected=proof['protected'],additional_protected=old.get('additional_protected',[]),all_doll_paths_complete=False,all_material_skills_complete=False,all_Source_consumers_wired=False,same_SHA_full_regression_passed=False,actual_client_cursor_verified=False,user_report_fixed=False,full_goal_achieved=False,qualified_for_release=False,formal_deployment_changed=False,new_baseline_created=False,Source_or_player_SQL_or_WAL_written=False)
if accepted:v['status']='program-updated-PC-doll-accepted-physical-completion-full-Source-open';v['program_checks']['new_helper_class_entries']=2
if npc:v['status']='program-updated-natural-controlled-NPC-doll-accepted-lifecycle-full-Source-open';v['program_checks'].update(JVM_no_initialization_classes=16,existing_changed_class_entries=4,new_helper_class_entries=2)
if action:v['status']='program-updated-physical-ingress-doll-summon-provenance-full-Source-open';v['program_checks'].update(JVM_no_initialization_classes=15,existing_changed_class_entries=4,new_helper_class_entries=3,pure_summon_identity_scenarios=11)
if skills:v['status']='program-updated-generic-doll-SKILL-metadata-slow-proc-spr-full-Source-open';v['program_checks'].update(JVM_no_initialization_classes=16,existing_changed_class_entries=3,new_helper_class_entries=4,pure_readonly_metadata_slow_view_scenarios=18,new_SELECTs_on_existing_readonly_startup_transaction=1,new_connection_ownership=False)
if output:v['status']='program-updated-doll-final-frame-accepted-speed-role-reset-full-Source-open';v['program_checks'].update(JVM_no_initialization_classes=22,existing_changed_class_entries=9,new_helper_class_entries=6,pure_detached_speed_and_wire_scenarios=15,original_speed_expiry_fields_made_volatile=8,new_connection_ownership=False)
if slow:v['status']='program-updated-doll29-zero-long-innate-haste-epoch-full-Source-open';v['program_checks'].update(JVM_no_initialization_classes=23,existing_changed_class_entries=7,new_helper_class_entries=1,pure_Source_slow_scenarios=25,identity_only_epoch_fields=5,new_connection_ownership=False)
if defense:v['status']='program-updated-doll-defense-original-summon-accepted-final-frame-full-Source-open';v['program_checks'].update(JVM_no_initialization_classes=31,existing_changed_class_entries=6,new_helper_class_entries=5,pure_defense_audience_scenarios=10,new_connection_ownership=False)
if passive:
 v['status']='program-updated-captured-actor-doll-passive-power-selection-full-Source-open';v['program_checks'].update(JVM_no_initialization_classes=37,existing_changed_class_entries=2,new_helper_class_entries=2,pure_captured_power_selection_scenarios=10,new_connection_ownership=False);v['doll_consumers']=consumers['manifest']
if cube:
 v['status']='program-updated-cube-effect-status-provenance-HP-consumers-open-full-Source-open';v['program_checks'].update(JVM_no_initialization_classes=22,existing_changed_class_entries=9,new_helper_class_entries=8,pure_cube_provenance_scenarios=15,original_roster_encounter_life_schema_verified_without_initialization=True,new_connection_ownership=False);v.update(all_cube_HP_sources_wired=False,cube_PC_actual_effect_commit_wired=False,cube_NPC_actual_effect_commit_wired=False,cube_final_receiver_wired=False)
if cubeplayer:
 v['status']='program-updated-cube-PC-effect-NPC-HP-accepted-final-receiver-full-Source-open';v['program_checks'].update(JVM_no_initialization_classes=35,existing_changed_class_entries=9,new_helper_class_entries=4,pure_cube_PC_contract_scenarios=22,new_connection_ownership=False);v.update(all_cube_HP_sources_wired=False,cube_PC_actual_effect_commit_wired=True,cube_PC_final_receiver_program_wired=True,cube_NPC_actual_effect_commit_wired=False,cube_final_receiver_wired=False,cube_status_MP_and_aura_role_paths_complete=False)
if cubencplease:
 v['status']='program-updated-cube-natural-NPC-stale-lease-retirement-full-Source-open';v['program_checks'].update(JVM_no_initialization_classes=36,existing_changed_class_entries=1,new_helper_class_entries=1,pure_cube_NPC_lease_contract_scenarios=27,new_locks=False,new_connection_ownership=False);v.update(all_cube_HP_sources_wired=False,cube_PC_actual_effect_commit_wired=True,cube_PC_final_receiver_program_wired=True,cube_NPC_stale_lease_retirement_program_wired=True,cube_NPC_actual_effect_commit_wired=False,cube_final_receiver_wired=False,cube_status_MP_and_aura_role_paths_complete=False)
if cubenpc:
 v['status']='program-updated-cube-natural-NPC-actual-effect-HP-master-final-receiver-full-Source-open';v['program_checks'].update(JVM_no_initialization_classes=41,existing_changed_class_entries=6,new_helper_class_entries=5,pure_cube_NPC_contract_scenarios=37,original_HP_publication_siblings=5,new_connection_ownership=False);v.update(all_cube_HP_sources_wired=False,cube_PC_actual_effect_commit_wired=True,cube_PC_final_receiver_program_wired=True,cube_NPC_stale_lease_retirement_program_wired=True,cube_NPC_actual_effect_commit_wired=True,cube_NPC_creator_independent_HP_dispatch_wired=True,cube_NPC_original_master_attribution_program_wired=True,cube_NPC_final_receiver_program_wired=True,cube_NPC_Source_async_FIFO_complete=False,cube_source_master_AI_paths_complete=False,cube_final_receiver_wired=False,cube_status_MP_and_aura_role_paths_complete=False)
if cubenpcqueue:
 v['status']='program-updated-cube-NPC-existing-target-FIFO-claimed-event-atomic-admission-full-Source-open';v['program_checks'].update(JVM_no_initialization_classes=52,existing_changed_class_entries=7,new_helper_class_entries=3,pure_cube_NPC_queue_contract_scenarios=33,new_connection_ownership=False,retained_non_damage_lifecycle_credit_writer_methods=7,producer_inline_HP_removed=True,existing_shared_NPC_drainer_reused=True);v.update(all_cube_HP_sources_wired=False,cube_PC_actual_effect_commit_wired=True,cube_PC_final_receiver_program_wired=True,cube_NPC_stale_lease_retirement_program_wired=True,cube_NPC_actual_effect_commit_wired=True,cube_NPC_creator_independent_HP_dispatch_wired=True,cube_NPC_original_master_attribution_program_wired=True,cube_NPC_final_receiver_program_wired=True,cube_NPC_nonblocking_target_FIFO_program_wired=True,cube_NPC_claimed_event_guard_program_wired=True,shared_PC_NPC_damage_capacity_CAS_program_wired=True,all_NPC_HP_producers_share_FIFO=False,Source_damage_capacity_domain_complete=False,Source_dequeue_credit_timing_program_matched=True,cube_NPC_Source_async_FIFO_complete=False,cube_source_master_AI_paths_complete=False,cube_final_receiver_wired=False,cube_status_MP_and_aura_role_paths_complete=False)
if capacity:
 v['status']='program-updated-damage-only-application-capacity-PC-NPC-release-shutdown-fence-full-Source-open';v['program_checks'].update(JVM_no_initialization_classes=53,existing_changed_class_entries=4,new_helper_class_entries=1,pure_damage_capacity_contract_scenarios=16,inherited_same_SHA_queue_contract_scenarios=33,new_connection_ownership=False,retained_non_damage_lifecycle_credit_writer_methods=7,new_original_owner_atomic_counters=1);v.update(all_cube_HP_sources_wired=False,cube_PC_actual_effect_commit_wired=True,cube_PC_final_receiver_program_wired=True,cube_NPC_stale_lease_retirement_program_wired=True,cube_NPC_actual_effect_commit_wired=True,cube_NPC_creator_independent_HP_dispatch_wired=True,cube_NPC_original_master_attribution_program_wired=True,cube_NPC_final_receiver_program_wired=True,cube_NPC_nonblocking_target_FIFO_program_wired=True,cube_NPC_claimed_event_guard_program_wired=True,shared_PC_NPC_damage_capacity_CAS_program_wired=True,queued_PC_NPC_damage_only_domain_program_wired=True,damage_ledger_idle_and_terminated_fences_program_wired=True,Source_damage_capacity_domain_complete=False,Source_dequeue_credit_timing_program_matched=True,all_NPC_HP_producers_share_FIFO=False,cube_NPC_Source_async_FIFO_complete=False,cube_source_master_AI_paths_complete=False,cube_final_receiver_wired=False,cube_status_MP_and_aura_role_paths_complete=False)
if poisonnpc:
 v['status']='program-updated-natural-NPC-poison-shared-FIFO-original-attacker-accepted-final-receiver-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=62,existing_changed_class_entries=10,new_helper_class_entries=7,pure_poison_NPC_queue_contract_scenarios=15,inherited_same_SHA_queue_contract_scenarios=33,inherited_same_SHA_damage_capacity_contract_scenarios=16,original_HP_publication_siblings=5,new_connection_ownership=False,existing_shared_NPC_drainer_reused=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_')):v[key]=value
 v.update(natural_NPC_poison_original_installation_scene_program_wired=True,natural_NPC_poison_shared_FIFO_program_wired=True,natural_NPC_poison_source_death_independent_HP_program_wired=True,natural_NPC_poison_admitted_status_independent_HP_program_wired=True,natural_NPC_poison_accepted_final_receiver_program_wired=True,all_NPC_HP_producers_share_FIFO=False)
if physicalnpc:
 v['status']='program-updated-natural-NPC-physical-scoped-doll-shared-FIFO-accepted-original-proc-final-receiver-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=76,existing_changed_class_entries=8,new_helper_class_entries=6,pure_physical_NPC_queue_contract_scenarios=30,inherited_same_SHA_queue_contract_scenarios=33,inherited_same_SHA_damage_capacity_contract_scenarios=16,inherited_same_SHA_poison_queue_contract_scenarios=15,original_HP_publication_siblings=5,original_body_wrappers=4,synchronous_frozen_audience_Context_constructors=1,new_connection_ownership=False,existing_shared_NPC_drainer_reused=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_poison_')):v[key]=value
 v.update(natural_NPC_physical_nonblocking_shared_FIFO_program_wired=True,natural_NPC_physical_accepted_rolled_proc_program_wired=True,natural_NPC_scoped_doll_skill_bonus_shared_FIFO_program_wired=True,natural_NPC_physical_original_audience_reconstruction_program_wired=True,natural_NPC_physical_all_Source_side_effects_complete=False,natural_NPC_physical_actual_bound_lifecycle_qualified=False,all_NPC_HP_producers_share_FIFO=False)
if magicnpc:
 v['status']='program-updated-natural-NPC-area-magic-frozen-cast-shared-FIFO-accepted-exact-erase-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=81,existing_changed_class_entries=5,new_helper_class_entries=4,pure_magic_NPC_identity_erase_contract_scenarios=16,inherited_same_SHA_physical_queue_contract_scenarios=30,inherited_same_SHA_queue_contract_scenarios=33,inherited_same_SHA_damage_capacity_contract_scenarios=16,inherited_same_SHA_poison_queue_contract_scenarios=15,original_area_HP_ports=4,original_area_erase_ports=4,original_body_wrappers=1,new_connection_ownership=False,existing_shared_NPC_drainer_reused=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_')):v[key]=value
 v.update(natural_NPC_area_magic_prewait_snapshot_program_wired=True,natural_NPC_area_magic_shared_FIFO_program_wired=True,natural_NPC_area_magic_exact_erase_after_acceptance_program_wired=True,natural_NPC_area_magic_nonpositive_mutation_program_wired=True,natural_NPC_area_magic_original_audience_program_wired=True,all_natural_NPC_magic_producers_share_FIFO=False,all_NPC_HP_producers_share_FIFO=False)
if singlenpc:
 v['status']='program-updated-natural-NPC-single-drain-confusion-thunder-shared-FIFO-once-accepted-before-wave-full-Source-open'
 v['goal_reset_request']=read(D/'source-goal-reset-request-current.json');verify(v['goal_reset_request'])
 v['program_checks'].update(JVM_no_initialization_classes=84,existing_changed_class_entries=4,new_helper_class_entries=3,pure_single_NPC_accepted_input_contract_scenarios=14,inherited_same_SHA_magic_identity_erase_contract_scenarios=16,inherited_same_SHA_physical_queue_contract_scenarios=30,inherited_same_SHA_queue_contract_scenarios=33,inherited_same_SHA_damage_capacity_contract_scenarios=16,inherited_same_SHA_poison_queue_contract_scenarios=15,original_single_HP_branch_prefixes=2,original_accepted_finally_prefixes=3,exact_capped_heal_siblings=1,new_connection_ownership=False,existing_shared_NPC_drainer_reused=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_')):v[key]=value
 v.update(natural_NPC_single_ordinary_drain_confusion_thunder_shared_FIFO_program_wired=True,natural_NPC_single_before_body_original_identity_program_wired=True,natural_NPC_single_mutation_inputs_frozen_program_wired=True,natural_NPC_single_once_only_accepted_heal_program_wired=True,natural_NPC_single_before_wave_accepted_effects_program_wired=True,natural_NPC_single_heal_final_receiver_program_wired=True,natural_NPC_single_mind_bone_arm_accepted_plan_program_wired=False,all_single_NPC_Source_numerical_RNG_paths_complete=False,all_natural_NPC_magic_producers_share_FIFO=False,all_NPC_HP_producers_share_FIFO=False)
if targetnpc:
 v['status']='program-updated-natural-NPC-directional-freeze-shock-accepted-target-plans-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=90,existing_changed_class_entries=6,new_helper_class_entries=4,pure_target_NPC_status_contract_scenarios=18,inherited_same_SHA_single_contract_scenarios=14,inherited_same_SHA_magic_identity_erase_contract_scenarios=16,inherited_same_SHA_physical_queue_contract_scenarios=30,inherited_same_SHA_queue_contract_scenarios=33,inherited_same_SHA_damage_capacity_contract_scenarios=16,inherited_same_SHA_poison_queue_contract_scenarios=15,original_target_HP_branch_prefixes=10,original_body_guards=5,original_status_expiry_writers=18,bounded_actual_actor_status_lease_fields=4,volatile_actual_actor_expiry_fields=4,new_connection_ownership=False,existing_shared_NPC_drainer_reused=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(natural_NPC_directional_freeze_shock_shared_FIFO_program_wired=True,natural_NPC_target_mutation_original_snapshot_program_wired=True,natural_NPC_target_mutation_actual_acceptance_program_wired=True,natural_NPC_target_mutation_same_expiry_ABA_guard_program_wired=True,natural_NPC_target_mutation_final_frame_program_wired=True,natural_NPC_Shock_duration_accepted_phase_program_wired=True,all_natural_NPC_magic_producers_share_FIFO=False,all_NPC_HP_producers_share_FIFO=False,all_NPC_Source_numerical_RNG_paths_complete=False)
if specialnpc:
 v['status']='program-updated-natural-NPC-Mind-Bone-Arm-original-pre-HP-phase-and-FIFO-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=100,existing_changed_class_entries=5,new_helper_class_entries=4,pure_special_NPC_phase_contract_scenarios=24,inherited_same_SHA_target_contract_scenarios=18,inherited_same_SHA_single_contract_scenarios=14,inherited_same_SHA_magic_identity_erase_contract_scenarios=16,inherited_same_SHA_physical_queue_contract_scenarios=30,inherited_same_SHA_queue_contract_scenarios=33,inherited_same_SHA_damage_capacity_contract_scenarios=16,inherited_same_SHA_poison_queue_contract_scenarios=15,exact_original_pre_HP_bodies=2,original_Arm_leaves=2,original_Bone_output_ports=2,original_Bone_expiry_writers=8,bounded_actual_actor_Bone_lease_fields=2,volatile_actual_actor_Bone_expiry_fields=2,bounded_Context_native_result_carriers=1,new_connection_ownership=False,existing_shared_NPC_drainer_reused=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.pop('natural_NPC_single_mind_bone_arm_accepted_plan_program_wired',None)
 v.update(natural_NPC_single_mind_bone_arm_Source_pre_HP_phase_program_wired=True,natural_NPC_single_mind_bone_arm_actual_native_result_program_wired=True,natural_NPC_single_mind_bone_arm_shared_FIFO_program_wired=True,natural_NPC_single_mind_bone_arm_stale_precondition_drop_program_wired=True,natural_NPC_Bone_final_frame_exact_lease_program_wired=True,natural_NPC_single_mind_bone_arm_accepted_only_side_effect_plan_applicability='not-applicable-fixed-Source-skillmode-pre-HP-phase',all_natural_NPC_magic_producers_share_FIFO=False,all_NPC_HP_producers_share_FIFO=False,all_NPC_Source_numerical_RNG_paths_complete=False)
if poisonpc:
 v['status']='program-updated-scoped-PC-DAMAGE-poison-without-attacker-peer-gate-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=105,existing_changed_class_entries=1,new_helper_class_entries=1,new_PC_poison_game_or_pure_contract_cases=0,inherited_same_SHA_special_contract_scenarios=24,inherited_same_SHA_target_contract_scenarios=18,inherited_same_SHA_single_contract_scenarios=14,inherited_same_SHA_magic_identity_erase_contract_scenarios=16,inherited_same_SHA_physical_queue_contract_scenarios=30,inherited_same_SHA_queue_contract_scenarios=33,inherited_same_SHA_damage_capacity_contract_scenarios=16,inherited_same_SHA_poison_queue_contract_scenarios=15,original_poison_Action_peer_argument_ports=1,new_connection_ownership=False)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(scoped_PC_DAMAGE_poison_original_clock_peer_program_wired=True,scoped_PC_DAMAGE_poison_target_HP_authority_program_preserved=True,all_PC_poison_lifecycle_paths_complete=False,all_PC_poison_original_attribution_complete=False,all_natural_NPC_magic_producers_share_FIFO=False,all_NPC_HP_producers_share_FIFO=False,all_NPC_Source_numerical_RNG_paths_complete=False)
if rawnpc:
 v['status']='program-updated-natural-NPC-raw-wand-feedback-shared-FIFO-detached-original-source-policy-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=108,existing_changed_class_entries=4,new_helper_class_entries=3,pure_raw_NPC_admission_contract_scenarios=12,inherited_same_SHA_special_contract_scenarios=24,inherited_same_SHA_target_contract_scenarios=18,inherited_same_SHA_single_contract_scenarios=14,inherited_same_SHA_magic_identity_erase_contract_scenarios=16,inherited_same_SHA_physical_queue_contract_scenarios=30,inherited_same_SHA_queue_contract_scenarios=33,inherited_same_SHA_damage_capacity_contract_scenarios=16,inherited_same_SHA_poison_queue_contract_scenarios=15,original_raw_POP_callers=6,final_detached_Origin_source_fields=1,original_Origin_record_components=7,original_live_HPbar_policy_ports=2,new_connection_ownership=False,existing_shared_NPC_drainer_reused=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(raw_NPC_wand_Mortal_Joy_shared_FIFO_program_wired=True,raw_NPC_original_source_policy_program_wired=True,raw_NPC_original_audience_program_wired=True,raw_NPC_live_HPbar_and_dead_cleanup_policy_program_wired=True,raw_NPC_no_ordinary_figures_program_preserved=True,all_natural_NPC_raw_roots_complete=False,all_raw_Source_semantics_complete=False,all_natural_NPC_magic_producers_share_FIFO=False,all_NPC_HP_producers_share_FIFO=False,all_NPC_Source_numerical_RNG_paths_complete=False)
if refrawnpc:
 v['status']='program-updated-accepted-natural-Ref-feedback-shared-raw-FIFO-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=109,existing_changed_class_entries=1,new_helper_class_entries=1,pure_accepted_Ref_ABI_contract_scenarios=12,inherited_same_SHA_raw_admission_contract_scenarios=12,inherited_same_SHA_special_contract_scenarios=24,inherited_same_SHA_target_contract_scenarios=18,inherited_same_SHA_single_contract_scenarios=14,inherited_same_SHA_magic_identity_erase_contract_scenarios=16,inherited_same_SHA_physical_queue_contract_scenarios=30,inherited_same_SHA_queue_contract_scenarios=33,inherited_same_SHA_damage_capacity_contract_scenarios=16,inherited_same_SHA_poison_queue_contract_scenarios=15,original_validated_natural_Ref_reflective_ports=1,new_connection_ownership=False,existing_shared_NPC_drainer_reused=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(raw_NPC_accepted_Ref_dynamic_port_program_wired=True,raw_NPC_accepted_Ref_original_frame_and_actor_program_wired=True,raw_NPC_accepted_Ref_same_FIFO_program_wired=True,raw_NPC_death_confirmation_end_ordering_qualified=False,all_natural_Ref_HP_paths_complete=False,all_natural_NPC_raw_roots_complete=False,all_raw_Source_semantics_complete=False,all_natural_NPC_magic_producers_share_FIFO=False,all_NPC_HP_producers_share_FIFO=False,all_NPC_Source_numerical_RNG_paths_complete=False)
if hitreaction:
 v['status']='program-updated-hit-reaction-Source-readonly-startup-control-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=112,existing_changed_class_entries=2,new_helper_class_entries=1,pure_hit_reaction_JDBC_contract_scenarios=14,inherited_same_SHA_pure_contract_scenarios=190,existing_catalog_reaction_call_sites=8,new_SELECTs_on_existing_readonly_startup_transaction=1,new_connection_ownership=False)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(hit_reaction_Source_readonly_startup_program_wired=True,hit_reaction_existing_catalog_consumers_program_wired=True,hit_reaction_Source_membership_and_SQL_failure_program_matched=True,all_hit_reaction_Source_consumers_wired=False,hit_reaction_real_MySQL_and_game_qualified=False)
if npcreaction:
 v['status']='program-updated-NPC-counter-reaction-original-Ref-control-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=113,existing_changed_class_entries=2,new_helper_class_entries=1,pure_NPC_reaction_Ref_snapshot_contract_scenarios=16,inherited_same_SHA_pure_contract_scenarios=204,original_reaction_send_guard_ports=2,original_private_live_bridge_instructions=4,new_connection_ownership=False)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(hit_reaction_NPC_counter_original_Ref_send_guards_program_wired=True,hit_reaction_NPC_counter_typed_live_gfx_program_wired=True,all_hit_reaction_Source_consumers_wired=False,hit_reaction_real_MySQL_and_game_qualified=False)
if wandreaction:
 v['status']='program-updated-wand-natural-NPC-reaction-original-Action-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=114,existing_changed_class_entries=1,new_helper_class_entries=1,pure_wand_NPC_metadata_packet_contract_scenarios=16,inherited_same_SHA_pure_contract_scenarios=220,original_wand_packet_wrapper_ports=1,new_connection_ownership=False)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(hit_reaction_wand_natural_NPC_original_Action_program_wired=True,hit_reaction_wand_existing_zero_packet_final_output_program_preserved=True,all_hit_reaction_Source_consumers_wired=False,hit_reaction_real_MySQL_and_game_qualified=False)
if groundorigin:
 v['status']='program-updated-original-ground-lease-pulse-tick-PC-raw-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=125,existing_changed_class_entries=9,new_helper_class_entries=6,pure_ground_original_lease_pulse_tick_contract_scenarios=16,inherited_same_SHA_pure_contract_scenarios=236,original_PC_firewall_HP_leaf_ports=1,original_lease_constructor_ports=2,original_pulse_constructor_ports=1,original_tick_constructor_ports=2,original_completion_cleanup_ports=1,new_bounded_owned_metadata_fields=8,new_connection_ownership=False)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(ground_original_lease_pulse_tick_provenance_program_wired=True,ground_PC_original_pair_pre_RNG_guard_program_wired=True,ground_PC_raw_false_true_shared_FIFO_program_wired=True,ground_PC_final_output_scene_program_wired=True,ground_synchronous_proof_original_close_cleanup_program_wired=True,ground_queued_HP_detached_scene_lease_proof_complete=False,all_ground_Source_HP_paths_complete=False,ground_Source_numerical_interval_presentation_complete=False,ground_real_role_Main_matrix_qualified=False)
if groundqueued:
 v['status']='program-updated-ground-detached-queued-scene-original-HP-frame-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=126,existing_changed_class_entries=3,new_helper_class_entries=1,pure_ground_queued_lease_scene_tuple_contract_scenarios=18,inherited_same_SHA_pure_contract_scenarios=252,original_raw_plan_binding_ports=1,original_pre_defense_rejection_fact_locals=1,original_actual_resolved_caster_HP_guard_ports=1,original_final_Frame_guard_ports=1,new_bounded_owned_metadata_fields=1,new_connection_ownership=False)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(ground_queued_HP_original_scene_proof_program_wired=True,ground_queued_HP_actual_resolved_caster_program_wired=True,ground_queued_HP_pre_defense_rejection_program_wired=True,ground_queued_HP_actual_precommit_guard_program_wired=True,ground_queued_HP_accepted_life_final_Frame_program_wired=True,ground_queued_HP_individual_expiry_independent_program_wired=True,ground_queued_HP_detached_scene_lease_proof_complete=False,all_ground_Source_HP_paths_complete=False,ground_Source_numerical_interval_presentation_complete=False,ground_real_role_Main_matrix_qualified=False)
if groundplacement:
 v['status']='program-updated-ground-material-production-staged-live-entity-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=131,existing_changed_class_entries=3,new_helper_class_entries=2,pure_ground_material_staged_live_entity_contract_scenarios=18,inherited_same_SHA_pure_contract_scenarios=270,original_current_Batch_peek_bridges=1,original_material_staged_copy_ports=2,original_material_install_ports=1,original_live_Entity_factory_bridges=1,new_bounded_owned_metadata_fields=0,new_connection_ownership=False)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(ground_material_actual_production_factory_program_wired=True,ground_material_original_prewait_creator_binding_program_wired=True,ground_material_original_preflight_cost_recalc_install_program_preserved=True,ground_material_staged_lease_live_engine_program_wired=True,ground_material_rejected_live_scene_unpublished_program_preserved=True,ground_material_actual_cost_rollback_qualified=False,ground_queued_HP_detached_scene_lease_proof_complete=False,all_ground_Source_HP_paths_complete=False,ground_Source_numerical_interval_presentation_complete=False,ground_real_role_Main_matrix_qualified=False)
if groundnpc:
 v['status']='program-updated-natural-NPC-firewall-original-FIFO-and-detached-life-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=135,existing_changed_class_entries=6,new_helper_class_entries=4,pure_ground_natural_NPC_contract_scenarios=26,inherited_same_SHA_pure_contract_scenarios=288,new_bounded_owned_metadata_fields=2,new_connection_ownership=False,existing_shared_NPC_drainer_reused=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(ground_natural_NPC_original_actor_scan_program_wired=True,ground_natural_NPC_original_tick_dispatch_program_wired=True,ground_natural_NPC_pre_RNG_identity_and_freeze_earth_bind_program_wired=True,ground_natural_NPC_Source_fire_resistance_INT_program_wired=True,ground_natural_NPC_original_raw_FIFO_mode0_program_wired=True,ground_natural_NPC_reaction_before_HP_program_wired=True,ground_natural_NPC_admitted_target_movement_preserved=True,ground_natural_NPC_detached_scene_final_output_program_wired=True,all_ground_Source_HP_paths_complete=False,ground_Source_numerical_interval_presentation_complete=False,ground_real_role_Main_matrix_qualified=False)
if groundplayer:
 v['status']='program-updated-PC-firewall-Source-SKD3-effective-stats-and-final-reaction-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=136,existing_changed_class_entries=2,new_helper_class_entries=1,pure_ground_PC_Source_SKD3_contract_scenarios=35,inherited_same_SHA_pure_contract_scenarios=314,new_bounded_owned_metadata_fields=0,original_PC_pulse_prefix_ports=1,original_ground_final_Frame_range_ports=1,new_connection_ownership=False,existing_shared_PC_queue_reused=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(ground_PC_Source_effective_INT_firewall_program_wired=True,ground_PC_Source_SKD3_fixed_active_keys_program_wired=True,ground_PC_pre_RNG_freeze_barrier_bind_program_wired=True,ground_PC_current_competition_canAttack_program_wired=True,ground_PC_original_receiveDamage_reductions_program_preserved=True,ground_PC_final_reaction_native_screen_program_wired=True,all_ground_Source_HP_paths_complete=False,ground_Source_numerical_interval_presentation_complete=False,ground_real_role_Main_matrix_qualified=False)
if cubeplacement:
 v['status']='program-updated-cube-actual-material-entity-factory-and-Source-scan-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=138,existing_changed_class_entries=4,new_helper_class_entries=2,pure_cube_material_factory_cadence_contract_scenarios=20,inherited_same_SHA_pure_contract_scenarios=349,actual_material_cube_Hub_factory_ports=2,actual_material_cube_metadata_constructor_ports=2,original_effect_scan_interval_ports=2,new_bounded_owned_metadata_fields=0,new_connection_ownership=False)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(cube_actual_material_Ignition_Harmony_entity_factory_program_wired=True,cube_original_prewait_creator_binding_program_wired=True,cube_rejected_material_live_scene_unpublished_program_preserved=True,cube_Source_effect_scan_500ms_program_wired=True,cube_original_status_initial900_period1000_program_preserved=True,cube_actual_material_cost_rollback_qualified=False,all_cube_Source_HP_paths_complete=False,cube_full_Source_lifecycle_qualified=False)
if cubestatus:
 v['status']='program-updated-Quake-Shock-material-status-secondary-life-and-final-output-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=151,existing_changed_class_entries=14,new_helper_class_entries=3,pure_cube_Quake_Shock_status_contract_scenarios=24,inherited_same_SHA_pure_contract_scenarios=369,new_bounded_owned_metadata_fields=21,original_secondary_visibility_fields=3,new_connection_ownership=False,existing_original_raw_publication_reused=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(cube_actual_material_Quake_Shock_entity_factory_program_wired=True,cube_Quake_Shock_original_target_primary_secondary_program_wired=True,cube_Quake_Shock_immutable_final_identity_index_program_wired=True,cube_Quake_Shock_stale_numeric_query_guard_program_wired=True,cube_Quake_natural_actual_actor_freeze_program_wired=True,cube_Shock_Source_marker_only_program_wired=True,cube_Quake_Shock_original_raw_final_output_program_wired=True,cube_actual_material_cost_rollback_qualified=False,all_cube_Source_HP_paths_complete=False,cube_full_Source_lifecycle_qualified=False)
if cuberemainder:
 v['status']='program-updated-Ignition-Harmony-original-status-MP-acceptance-and-final-output-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=154,existing_changed_class_entries=9,new_helper_class_entries=3,pure_cube_Ignition_Harmony_MP_status_contract_scenarios=25,inherited_same_SHA_pure_contract_scenarios=393,new_bounded_owned_metadata_fields=4,original_NPC_MP_siblings=2,new_connection_ownership=False,existing_original_raw_publication_reused=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(cube_Ignition_Harmony_exact_final_identity_index_program_wired=True,cube_Ignition_Harmony_stale_attribute_status_query_program_wired=True,cube_Ignition_Harmony_original_raw_status_final_output_program_wired=True,cube_Harmony_PC_original_MP_store_accepted_final_output_program_wired=True,cube_Harmony_natural_actual_original_actor_MP_program_wired=True,cube_actual_material_cost_rollback_qualified=False,all_cube_Source_HP_paths_complete=False,cube_status_MP_and_aura_role_paths_complete=False,cube_full_Source_lifecycle_qualified=False)
if cubepresentation:
 v['status']='program-updated-cube-Source-config-GFX-original-relation-output-order-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=157,existing_changed_class_entries=5,new_helper_class_entries=1,pure_cube_Source_presentation_relation_contract_scenarios=27,inherited_same_SHA_pure_contract_scenarios=418,new_bounded_owned_metadata_fields=1,original_typed_status_bridge=1,new_connection_ownership=False,existing_original_raw_publication_reused=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(cube_Source_skill_table_primary_secondary_GFX_program_wired=True,cube_Harmony_original_installed_relation_program_wired=True,cube_Source_ally_attribute_before_effect_program_wired=True,cube_Source_zero_GFX_and_native_positive_mapping_program_wired=True,cube_Quake_Shock_waited_expiry_query_output_guard_program_wired=True,cube_actual_material_cost_rollback_qualified=False,all_cube_Source_HP_paths_complete=False,cube_status_MP_and_aura_role_paths_complete=False,cube_full_Source_lifecycle_qualified=False)
if cuberelation:
 v['status']='program-updated-cube-original-Source-role-self-and-match-bounded-new-status-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=161,existing_changed_class_entries=4,new_helper_class_entries=1,pure_cube_original_Source_identity_match_relation_contract_scenarios=33,inherited_same_SHA_pure_contract_scenarios=445,new_bounded_owned_metadata_fields=0,new_connection_ownership=False,original_Binding_source_qualified_identity_bytecode_verified=True,original_installed_HP_clocks_cost_RNG_output_preserved=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(cube_original_Binding_Source_identity_relation_program_wired=True,cube_exact_original_creator_target_self_program_wired=True,cube_new_hostile_match_PvP_original_force_program_wired=True,cube_new_scan_original_role_life_scene_admission_program_wired=True,cube_installed_HP_creator_retirement_independence_program_preserved=True,cube_actual_material_cost_rollback_qualified=False,all_cube_Source_HP_paths_complete=False,cube_status_MP_and_aura_role_paths_complete=False,cube_full_Source_lifecycle_qualified=False)
if cubeclock:
 v['status']='program-updated-cube-actual-Source-bootstrap-global1000-phase-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=163,existing_changed_class_entries=6,new_helper_class_entries=2,pure_cube_actual_bootstrap_global_phase_contract_scenarios=33,inherited_same_SHA_pure_contract_scenarios=478,unchanged_inherited_pure_expectations=477,corrected_inherited_bootstrap_cadence_oracle_cases=1,new_bounded_owned_metadata_fields=4,new_connection_ownership=False,new_scheduled_tasks=False,original_status_HP_clocks_cost_RNG_output_preserved=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(cube_Source_effect_scan_500ms_program_wired=False,cube_500ms_leaf_start_as_actual_bootstrap_assumption_superseded=True,cube_Source_actual_bootstrap_initial0_period1000_program_wired=True,cube_Source_global_phase_original_Hub_clock_program_wired=True,cube_Source_global_phase_all_rooms_placements_program_wired=True,cube_Source_global_phase_original_close_cleanup_program_wired=True,cube_Source_independent_status900_period1000_program_preserved=True,cube_actual_material_cost_rollback_qualified=False,all_cube_Source_HP_paths_complete=False,cube_status_MP_and_aura_role_paths_complete=False,cube_full_Source_lifecycle_qualified=False)
if cubevisibility:
 v['status']='program-updated-cube-effect-original-output-perceive-post-write-known-role-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=169,existing_changed_class_entries=10,new_helper_class_entries=4,pure_cube_effect_visibility_output_contract_scenarios=39,inherited_same_SHA_pure_contract_scenarios=511,new_bounded_owned_metadata_fields=4,existing_engine_snapshot_lock_reused=1,new_connection_ownership=False,new_scheduled_tasks=False,original_HP_cost_RNG_encoders_other_outputs_preserved=True,compiler_visibility_stubs_excluded_from_runtime=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(cube_effect_initial_audience_frozen_program_wired=True,cube_effect_rebuild_binding_before_Hub_wait_program_wired=True,cube_effect_movement_perceive_program_wired=True,cube_effect_original_output_independent_creator_program_wired=True,cube_effect_actual_post_write_known_pair_program_wired=True,cube_effect_same_id_role_expiry_remove_guard_program_wired=True,cube_effect_current_native_resource_encoders_preserved=True,cube_Source_NPC_template_and_visibility_control_parity_complete=False,cube_actual_material_cost_rollback_qualified=False,all_cube_Source_HP_paths_complete=False,cube_status_MP_and_aura_role_paths_complete=False,cube_full_Source_lifecycle_qualified=False)
if cubetemplate:
 v['status']='program-updated-cube-original-static-template-initial-presentation-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=175,existing_changed_class_entries=8,new_helper_class_entries=3,pure_cube_template_data_contract_scenarios=371,inherited_same_SHA_pure_contract_scenarios=550,new_bounded_owned_metadata_fields=5,new_connection_ownership=False,new_scheduled_tasks=False,original_HP_cost_combat_RNG_other_outputs_preserved=True,executable_frozen_Source_presentation_oracle=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(cube_static_template_original_pre_cost_freeze_program_wired=True,cube_initial_presentation_original_effect_owned_program_wired=True,cube_Source_initial_GFX_status_exp_lawful_name_program_wired=True,cube_Source_chaLight_not_template_light_corrected=True,cube_Source_NPC_custom_factory_hooks_complete=False,cube_Source_NPC_template_and_visibility_control_parity_complete=False,cube_full_Source_lifecycle_qualified=False)
if fatalitydamage:
 v['status']='program-updated-Fatality-paired-explicit-null-attacker-raw-lifecycle-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=179,existing_changed_class_entries=3,new_helper_class_entries=4,pure_fatality_explicit_null_attacker_contract_scenarios=32,inherited_same_SHA_pure_contract_scenarios=921,new_bounded_owned_metadata_fields=1,new_connection_ownership=False,new_scheduled_tasks=False,original_native_Fatality_catalog_RNG_and_other_HP_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(fatality_original_pre_wait_role_pose_program_wired=True,fatality_original_die_inside_ordered_pair_program_wired=True,fatality_target_only_queued_HP_authority_program_wired=True,fatality_explicit_NONE_attacker_Calc_program_wired=True,fatality_detached_final_receiver_program_wired=True,fatality_Source_catalog_parity_complete=False,fatality_full_lifecycle_qualified=False,all_actual_attacker_sources_classified=False)
if npcplayer:
 v['status']='program-updated-NPC-PC-original-target-defense-raw-FIFO-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=186,existing_changed_class_entries=6,new_helper_class_entries=3,pure_NPC_PC_provenance_contract_scenarios=32,inherited_same_SHA_pure_contract_scenarios=953,new_bounded_owned_metadata_fields=2,new_connection_ownership=False,new_scheduled_tasks=False,original_AI_RNG_queue_Main_and_other_HP_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(NPC_PC_original_tick_target_before_lookup_program_wired=True,NPC_PC_six_actual_target_pair_and_defense_scope_program_wired=True,NPC_PC_Source_physical_false_magic_true_program_wired=True,NPC_PC_target_only_queued_HP_authority_program_wired=True,NPC_PC_exact_original_ingress_release_program_wired=True,raw_HP_final_Frame_exact_accepted_life_program_wired=True,NPC_PC_upstream_full_Source_math_AI_complete=False,NPC_PC_all_producers_complete=False,NPC_PC_full_lifecycle_qualified=False)
if npcmagiceffects:
 v['status']='program-updated-NPC-magic-accepted-drain-exact-erase-meter-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=194,existing_changed_class_entries=7,new_helper_class_entries=4,pure_NPC_magic_accepted_contract_scenarios=33,inherited_same_SHA_pure_contract_scenarios=985,new_bounded_owned_metadata_fields=2,bounded_original_heal_siblings=2,new_connection_ownership=False,new_scheduled_tasks=False,original_AI_cost_RNG_FIFO_Main_and_fallback_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(NPC_magic_original_accepted_drain_10_28_program_wired=True,NPC_magic_nonterminal_exact_erase_program_wired=True,NPC_magic_original_actor_long_safe_heal_program_wired=True,NPC_magic_existing_tracked_viewer_post_write_meter_program_wired=True,NPC_magic_ALT_ATKMSG_diagnostic_complete=False,NPC_magic_all_accepted_skill_mutations_complete=False,NPC_magic_full_lifecycle_qualified=False)
if npcmagicdirect:
 v['status']='program-updated-NPC-nonpositive-direct-erase-no-zero-HP-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=196,existing_changed_class_entries=1,new_helper_class_entries=2,pure_NPC_magic_direct_contract_scenarios=37,inherited_same_SHA_pure_contract_scenarios=1018,new_bounded_owned_metadata_fields=0,new_connection_ownership=False,new_scheduled_tasks=False,all_other_parent_entries_byte_retained=True,original_AI_cost_RNG_FIFO_Main_and_fallback_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(NPC_magic_zero_direct_erase_no_HP_program_wired=True,NPC_magic_negative_direct_erase_post_submission_program_wired=True,NPC_magic_direct_existing_Plan_exact_release_program_wired=True,NPC_magic_signed_HP_healing_complete=False,NPC_magic_mana_only_and_all_direct_mutations_complete=False,NPC_magic_full_lifecycle_qualified=False)
if signedrecovery:
 v['status']='program-updated-shared-signed-recovery-original-FIFO-target-only-life-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=201,existing_changed_class_entries=1,new_helper_class_entries=2,pure_signed_recovery_contract_scenarios=31,additional_frozen_Source_HP_arithmetic_comparisons=420,inherited_same_SHA_pure_contract_scenarios=1055,new_bounded_owned_metadata_fields=0,complete_original_fallback_siblings=1,new_connection_ownership=False,new_scheduled_tasks=False,all_other_parent_entries_byte_retained=True,original_AI_cost_RNG_FIFO_Main_and_fallback_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(signed_recovery_shared_BATTLE_original_FIFO_program_wired=True,signed_recovery_existing_input_provenance_program_wired=True,signed_recovery_target_only_queued_HP_authority_program_wired=True,signed_recovery_exact_Source_HP_arithmetic_offline_matched=True,signed_recovery_all_outer_cast_target_prewait_paths_complete=False,signed_recovery_all_healing_undead_producers_complete=False,signed_recovery_full_lifecycle_qualified=False)
if healingcast:
 v['status']='program-updated-healing-original-participants-pre-cost-life-pair-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=203,existing_changed_class_entries=3,new_helper_class_entries=2,pure_healing_cast_contract_scenarios=32,inherited_same_SHA_pure_contract_scenarios=1086,additional_inherited_exact_Source_HP_arithmetic_comparisons=420,new_bounded_owned_metadata_fields=1,complete_original_root_siblings=3,new_connection_ownership=False,new_scheduled_tasks=False,original_cost_RNG_HP_receivers_WORLDbodies_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(healing_cast_three_actual_roots_program_wired=True,healing_cast_original_participants_before_target_action_wait_program_wired=True,healing_cast_single_pre_cost_RNG_role_life_program_wired=True,healing_cast_individual_secondary_original_list_program_wired=True,healing_cast_existing_signed_member_provenance_program_wired=True,healing_cast_existing_scope_lock_cleanup_program_wired=True,healing_cast_first_Hub_discovery_lifetime_complete=False,healing_cast_three_direct_PC_HP_FIFOs_complete=False,healing_cast_accepted_final_PC_output_complete=False,healing_cast_full_lifecycle_qualified=False)
if healinghp:
 v['status']='program-updated-three-healing-PC-signed-FIFO-negative-completion-accepted-output-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=205,existing_changed_class_entries=3,new_helper_class_entries=2,pure_healing_HP_contract_scenarios=42,inherited_same_SHA_pure_contract_scenarios=1118,additional_inherited_exact_Source_HP_arithmetic_comparisons=420,new_bounded_owned_metadata_fields=1,actual_PC_heal_store_ports=3,actual_PC_reversal_ports=3,original_send_ports_retained_with_preview_filter=22,PC_preview_ratio_ports=3,original_single_bot_ratio_port_retained=True,common_original_negative_input_proof_ports=1,actual_original_BATTLE_recovery_completion_ports=1,new_connection_ownership=False,new_scheduled_tasks=False,original_cost_RNG_status_NPC_bot_WORLDbodies_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(healing_PC_three_actual_requested_signed_HP_ports_program_wired=True,healing_PC_three_reversal_Source_magic_flags_program_wired=True,healing_PC_target_only_queued_HP_authority_program_wired=True,healing_PC_common_negative_input_recovery_proof_program_wired=True,healing_PC_nonpositive_accepted_HP_only_completion_program_wired=True,healing_PC_legacy_preview_HP_ratio_suppressed_program_wired=True,healing_PC_original_cost_NPC_bot_output_preserved=True,healing_PC_original_raw_final_Frame_program_wired=True,healing_PC_full_Source_formula_type_undead_complete=False,healing_PC_all_producers_complete=False,healing_PC_full_lifecycle_qualified=False)
if healingformula:
 v['status']='program-updated-readonly-INT-healing-growth-scoped-signed-formula-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=213,existing_changed_class_entries=7,new_helper_class_entries=2,pure_healing_formula_contract_scenarios=25,inherited_same_SHA_pure_contract_scenarios=1160,additional_exact_Source_status_comparisons=160,additional_exact_Source_lawful_comparisons=30,additional_inherited_exact_Source_HP_arithmetic_comparisons=420,new_bounded_owned_metadata_fields=0,new_SELECTs_on_existing_readonly_startup_transaction=1,new_connection_ownership=False,new_scheduled_tasks=False,original_RNG_count_order_bounds_cost_WORLDbodies_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(healing_formula_existing_readonly_INT_snapshot_program_wired=True,healing_formula_Chinese_bonus_and_Source_fallback_program_matched=True,healing_formula_actual_shared_INT_lookup_program_wired=True,healing_formula_scoped_signed_status_and_lawful_program_wired=True,healing_formula_original_RNG_callback_order_count_preserved=True,healing_formula_actual_MySQL_growth_reload_qualified=False,healing_formula_all_effective_stat_leverage_paths_complete=False,healing_formula_full_Source_type_undead_complete=False,healing_formula_full_lifecycle_qualified=False)
if healingtype:
 v['status']='program-updated-controlled-healing-actual-target-type-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=214,existing_changed_class_entries=2,new_helper_class_entries=1,pure_healing_type_contract_scenarios=40,inherited_same_SHA_pure_contract_scenarios=1185,additional_exact_Source_type_delta_comparisons=112,additional_controlled_Source_type_delta_comparisons=84,additional_modeled_monster_arithmetic_regressions=28,additional_inherited_exact_Source_status_comparisons=160,additional_inherited_exact_Source_lawful_comparisons=30,additional_inherited_exact_Source_HP_arithmetic_comparisons=420,new_bounded_owned_metadata_fields=0,new_SELECTs=0,new_connection_ownership=False,new_scheduled_tasks=False,original_HP_RNG_cost_locks_lifetime_Main_WORLDbodies_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_','healing_formula_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(healing_type_three_actual_controlled_undead_reads_program_wired=True,healing_type_original_selected_record_and_owned_actor_kind_program_wired=True,healing_type_stateless_no_new_lookup_program_matched=True,healing_type_full_natural_factory_classification_complete=False,healing_type_tower_door_bot_semantics_complete=False,healing_type_full_controlled_positive_HP_lifecycle_qualified=False,healing_type_full_Source_target_type_complete=False,healing_type_full_lifecycle_qualified=False)
if controlledhealing:
 v['status']='program-updated-controlled-healing-original-life-HP-accepted-meter-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=218,existing_changed_class_entries=6,new_helper_class_entries=4,pure_controlled_healing_contract_scenarios=28,inherited_same_SHA_pure_contract_scenarios=1225,additional_exact_Source_controlled_positive_HP_comparisons=90,additional_inherited_exact_Source_type_delta_comparisons=112,additional_inherited_exact_Source_status_comparisons=160,additional_inherited_exact_Source_lawful_comparisons=30,additional_inherited_exact_Source_HP_arithmetic_comparisons=420,new_bounded_owned_metadata_fields=1,new_synchronized_original_HP_siblings=1,new_SELECTs=0,new_connection_ownership=False,new_scheduled_tasks=False,existing_completion_state_fixture_initialized=True,no_unbound_completion_cleanup_errors=True,original_RNG_cost_locks_status_Main_WORLDbodies_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_','healing_formula_','healing_type_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(healing_controlled_three_actual_positive_HP_calls_program_wired=True,healing_controlled_prewait_original_owner_actor_scope_program_wired=True,healing_controlled_synchronized_long_safe_original_HP_sibling_program_wired=True,healing_controlled_detached_accepted_meter_actual_final_frame_program_wired=True,healing_controlled_old_preview_ratios_program_suppressed=True,healing_controlled_full_signed_and_owner_meter_semantics_complete=False,healing_controlled_bound_HP_role_IO_Main_qualified=False,healing_controlled_full_lifecycle_qualified=False)
if firstplayers:
 v['status']='program-updated-first-Hub-player-life-signed-healing-consumers-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=221,existing_changed_class_entries=2,new_helper_class_entries=3,pure_first_player_discovery_contract_scenarios=26,inherited_same_SHA_pure_contract_scenarios=1253,inherited_exact_Source_controlled_positive_HP_comparisons=90,inherited_exact_Source_type_delta_comparisons=112,inherited_exact_Source_status_comparisons=160,inherited_exact_Source_lawful_comparisons=30,inherited_exact_Source_HP_arithmetic_comparisons=420,new_bounded_owned_metadata_fields=1,new_SELECTs=0,new_connection_ownership=False,new_scheduled_tasks=False,no_unbound_completion_cleanup_errors=True,original_earlier_ingress_priority_HP_RNG_cost_locks_Main_WORLDbodies_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_','healing_formula_','healing_type_','healing_controlled_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v.update(first_player_discovery_original_Hub_life_before_binding_program_wired=True,first_player_discovery_immutable_original_Context_copy_program_wired=True,first_player_discovery_actual_signed_healing_final_fallback_program_wired=True,first_player_discovery_earlier_raw_doll_magic_cast_precedence_preserved=True,first_player_discovery_bound_Hub_role_revival_qualified=False,first_player_discovery_all_Source_factories_consumed=False,first_player_discovery_full_lifecycle_qualified=False)
if playerfactories:
 v['status']='program-updated-Context-player-factories-original-source-life-pose-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=227,existing_changed_class_entries=10,new_helper_class_entries=1,pure_player_factories_contract_scenarios=34,inherited_same_SHA_pure_contract_scenarios=1279,inherited_exact_Source_controlled_positive_HP_comparisons=90,inherited_exact_Source_type_delta_comparisons=112,inherited_exact_Source_status_comparisons=160,inherited_exact_Source_lawful_comparisons=30,inherited_exact_Source_HP_arithmetic_comparisons=420,new_bounded_owned_metadata_fields=0,new_SELECTs=0,new_connection_ownership=False,new_scheduled_tasks=False,actual_peer_LambdaMetafactory_shape_executed=True,no_unbound_completion_cleanup_errors=True,original_accepted_frame_priority_HP_RNG_cost_Ctrl_locks_Main_WORLDbodies_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_','healing_formula_','healing_type_','healing_controlled_','first_player_discovery_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 verify(contract['player_proof_factory_census']);v['player_factory_remaining_constructor_census']=contract['player_proof_factory_census']
 v.update(player_factory_pre_Hub_source_life_program_wired=True,player_factory_complete_roster_before_magic_snapshot_program_wired=True,player_factory_five_ordinary_member_factories_program_wired=True,player_factory_explicit_and_shared_Actor_life_pose_program_wired=True,player_factory_material_primary_player_initial_life_program_wired=True,player_factory_raw_synchronous_peer_context_program_wired=True,player_factory_full_accepted_and_explicit_ingress_complete=False,player_factory_all_NPC_first_discovery_complete=False,player_factory_bound_cast_role_IO_Main_qualified=False,player_factory_full_Source_lifecycle_qualified=False)
if acceptedplayers:
 v['status']='program-updated-HP-owned-accepted-participant-values-Actor-pose-ingress-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=230,existing_changed_class_entries=9,new_helper_class_entries=3,pure_accepted_players_contract_scenarios=28,inherited_same_SHA_pure_contract_scenarios=1313,inherited_exact_Source_controlled_positive_HP_comparisons=90,inherited_exact_Source_type_delta_comparisons=112,inherited_exact_Source_status_comparisons=160,inherited_exact_Source_lawful_comparisons=30,inherited_exact_Source_HP_arithmetic_comparisons=420,new_bounded_owned_metadata_fields=1,new_explicit_pose_Member_constructors=1,original_HP_drain_queue_completion_CAS_admission_byte_retained=True,new_SELECTs=0,new_connection_ownership=False,new_scheduled_tasks=False,no_unbound_completion_cleanup_errors=True,original_HP_RNG_cost_Ctrl_locks_Main_WORLDbodies_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_','healing_formula_','healing_type_','healing_controlled_','first_player_discovery_','player_factory_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 verify(contract['player_proof_factory_census']);v['player_factory_remaining_constructor_census']=contract['player_proof_factory_census']
 v.update(accepted_player_actual_vital_lock_once_value_capture_program_wired=True,accepted_player_two_raw_participant_factories_program_wired=True,accepted_player_approved_corpse_and_old_source_pose_program_wired=True,accepted_player_five_explicit_Actor_pose_conversions_program_wired=True,accepted_player_primary_NPC_and_Fatality_life_before_binding_program_wired=True,accepted_player_all_unknown_provenance_qualified=False,accepted_player_all_NPC_viewer_master_ingress_complete=False,accepted_player_bound_HP_role_IO_Main_qualified=False,accepted_player_full_Source_lifecycle_qualified=False)
if npcorigins:
 v['status']='program-updated-NPC-first-roster-weak-original-master-Ref-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=233,existing_changed_class_entries=3,new_helper_class_entries=2,pure_npc_origins_contract_scenarios=27,inherited_same_SHA_pure_contract_scenarios=1341,inherited_exact_Source_controlled_positive_HP_comparisons=90,inherited_exact_Source_type_delta_comparisons=112,inherited_exact_Source_status_comparisons=160,inherited_exact_Source_lawful_comparisons=30,inherited_exact_Source_HP_arithmetic_comparisons=420,new_bounded_owned_metadata_fields=1,new_immutable_Ref_constructors=1,new_Source_only_original_origin_siblings=1,original_HP_Ref_live_queue_completion_CAS_admission_byte_retained=True,new_SELECTs=0,new_connection_ownership=False,new_scheduled_tasks=False,new_strong_player_back_references=False,no_unbound_completion_cleanup_errors=True,original_Ref_identity_HP_AI_RNG_cost_Ctrl_locks_Main_WORLDbodies_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_','healing_formula_','healing_type_','healing_controlled_','first_player_discovery_','player_factory_','accepted_player_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 verify(contract['player_proof_factory_census']);v['player_factory_remaining_constructor_census']=contract['player_proof_factory_census']
 v.update(npc_origin_Source_roster_Hub_owned_life_before_binding_program_wired=True,npc_origin_older_primary_and_original_map_program_wired=True,npc_origin_weak_immutable_pre_Ref_wait_master_program_wired=True,npc_origin_actual_companion_master_ingress_program_wired=True,npc_origin_original_queued_HP_Ref_live_independence_preserved=True,npc_origin_owner_birth_and_all_manual_Ref_factories_complete=False,npc_origin_all_actual_NPC_actor_first_discovery_complete=False,npc_origin_bound_Ref_AI_HP_role_IO_Main_qualified=False,npc_origin_full_Source_lifecycle_qualified=False)
if npcfirstlife:
 v['status']='program-updated-NPC-initial-targets-original-discovery-life-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=234,existing_changed_class_entries=4,new_helper_class_entries=1,pure_npc_first_life_contract_scenarios=30,inherited_same_SHA_pure_contract_scenarios=1368,inherited_exact_Source_controlled_positive_HP_comparisons=90,inherited_exact_Source_type_delta_comparisons=112,inherited_exact_Source_status_comparisons=160,inherited_exact_Source_lawful_comparisons=30,inherited_exact_Source_HP_arithmetic_comparisons=420,new_bounded_owned_metadata_fields=0,initial_life_ports=6,original_HP_Ref_live_queue_completion_CAS_admission_byte_retained=True,new_SELECTs=0,new_connection_ownership=False,new_scheduled_tasks=False,no_unbound_completion_cleanup_errors=True,original_accepted_seal_live_revalidation_filters_Ctrl_locks_Main_WORLD_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_','healing_formula_','healing_type_','healing_controlled_','first_player_discovery_','player_factory_','accepted_player_','npc_origin_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 verify(contract['player_proof_factory_census']);v['player_factory_remaining_constructor_census']=contract['player_proof_factory_census']
 v['goal_reset_request']=read(D/'source-goal-reset-attempt-current.json')['receipt'];verify(v['goal_reset_request'])
 v.update(npc_first_life_six_initial_metadata_ports_program_wired=True,npc_first_life_original_record_actor_master_binding_program_wired=True,npc_first_life_natural_epoch_controlled_Ref_owner_program_wired=True,npc_first_life_older_master_player_proof_program_wired=True,npc_first_life_original_accepted_death_seals_preserved=True,npc_first_life_all_NPC_actor_discovery_factories_complete=False,npc_first_life_bound_AI_HP_role_IO_Main_qualified=False,npc_first_life_full_Source_lifecycle_qualified=False)
if materialfirstlife:
 v['status']='program-updated-material-original-primary-master-receiver-life-controlled-attachment-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=243,existing_changed_class_entries=8,new_helper_class_entries=1,pure_material_first_life_contract_scenarios=39,inherited_same_SHA_pure_contract_scenarios=1398,inherited_exact_Source_controlled_positive_HP_comparisons=90,inherited_exact_Source_type_delta_comparisons=112,inherited_exact_Source_status_comparisons=160,inherited_exact_Source_lawful_comparisons=30,inherited_exact_Source_HP_arithmetic_comparisons=420,new_bounded_owned_metadata_fields=0,initial_life_fields=5,validation_and_final_frame_gates=11,controlled_final_attachment_ports=3,new_SELECTs=0,new_connection_ownership=False,new_scheduled_tasks=False,no_unbound_completion_cleanup_errors=True,original_selection_Ctrl_area_cost_RNG_locks_HP_Main_WORLD_inverse_retained=True,observer_receiver_eligibility_preserved=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_','healing_formula_','healing_type_','healing_controlled_','first_player_discovery_','player_factory_','accepted_player_','npc_origin_','npc_first_life_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 verify(contract['player_proof_factory_census']);v['player_factory_remaining_constructor_census']=contract['player_proof_factory_census']
 v.update(material_first_life_five_initial_fields_program_wired=True,material_first_life_eleven_ingress_position_final_frame_gates_program_wired=True,material_first_life_original_source_primary_master_receiver_program_wired=True,material_first_life_three_native_single_or_tamed_final_attachments_program_wired=True,material_first_life_original_observer_and_WORLD_receiver_rules_preserved=True,material_first_life_all_secondary_and_material_semantics_complete=False,material_first_life_bound_cast_status_HP_role_IO_Main_qualified=False,material_first_life_full_Source_lifecycle_qualified=False)
if areaplayerproof:
 v['status']='program-updated-shared-area-original-player-roster-prebody-source-life-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=245,existing_changed_class_entries=1,new_helper_class_entries=1,pure_area_player_proof_contract_scenarios=25,inherited_same_SHA_pure_contract_scenarios=1437,inherited_exact_Source_controlled_positive_HP_comparisons=90,inherited_exact_Source_type_delta_comparisons=112,inherited_exact_Source_status_comparisons=160,inherited_exact_Source_lawful_comparisons=30,inherited_exact_Source_HP_arithmetic_comparisons=420,new_bounded_owned_metadata_fields=0,original_bound_roster_predicates=1,pre_body_source_scene_ports=2,new_SELECTs=0,new_connection_ownership=False,new_scheduled_tasks=False,no_unbound_completion_cleanup_errors=True,original_lock_attempts_admission_Hub_try_finally_selection_Ctrl_HP_WORLD_inverse_retained=True,original_observer_dead_movement_receiver_eligibility_preserved=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_','healing_formula_','healing_type_','healing_controlled_','first_player_discovery_','player_factory_','accepted_player_','npc_origin_','npc_first_life_','material_first_life_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 verify(contract['player_proof_factory_census']);v['player_factory_remaining_constructor_census']=contract['player_proof_factory_census']
 verify(build['method_reference_census']);v['area_player_proof_method_reference_census']=build['method_reference_census']
 v.update(area_player_proof_original_bound_roster_predicate_program_wired=True,area_player_proof_two_pre_body_original_source_life_pose_program_wired=True,area_player_proof_approved_corpse_and_original_observer_audience_preserved=True,area_player_proof_original_locks_and_failure_cleanup_inverse_retained=True,area_player_proof_all_method_reference_consumers_complete=False,area_player_proof_full_material_target_semantics_complete=False,area_player_proof_bound_cast_HP_role_IO_Main_qualified=False,area_player_proof_full_Source_lifecycle_qualified=False)
if indirectplayerproof:
 v['status']='program-updated-indirect-roster-exact-linked-freeze-accepted-target-cancellation-final-life-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=257,existing_changed_class_entries=10,new_helper_class_entries=1,pure_indirect_player_proof_contract_scenarios=38,inherited_same_SHA_pure_contract_scenarios=1462,inherited_exact_Source_controlled_positive_HP_comparisons=90,inherited_exact_Source_type_delta_comparisons=112,inherited_exact_Source_status_comparisons=160,inherited_exact_Source_lawful_comparisons=30,inherited_exact_Source_HP_arithmetic_comparisons=420,new_bounded_owned_metadata_fields=0,original_indirect_roster_predicates=10,first_life_fields=2,existing_HP_lock_accepted_target_slot=1,cancellation_final_frame_gate=1,cancellation_native_attachment_port=1,new_SELECTs=0,new_connection_ownership=False,new_scheduled_tasks=False,no_unbound_completion_cleanup_errors=True,original_HP_queue_CAS_source_retirement_lock_Ctrl_status_cost_WORLD_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_','healing_formula_','healing_type_','healing_controlled_','first_player_discovery_','player_factory_','accepted_player_','npc_origin_','npc_first_life_','material_first_life_','area_player_proof_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 verify(contract['player_proof_factory_census']);v['player_factory_remaining_constructor_census']=contract['player_proof_factory_census']
 verify(build['method_reference_census']);v['indirect_player_proof_method_reference_census']=build['method_reference_census']
 v.update(indirect_player_proof_ten_applicable_bound_references_program_wired=True,indirect_player_proof_original_linked_freeze_HP_lock_target_value_program_wired=True,indirect_player_proof_no_visual_Plan_accepted_target_program_wired=True,indirect_player_proof_first_freeze_target_and_cancellation_pet_program_wired=True,indirect_player_proof_cancellation_final_receiver_single_or_tamed_program_wired=True,indirect_player_proof_original_WORLD_only_reference_byte_retained=True,indirect_player_proof_all_direct_and_accepted_lifecycle_paths_complete=False,indirect_player_proof_full_material_Source_semantics_complete=False,indirect_player_proof_bound_cast_HP_role_IO_Main_qualified=False,indirect_player_proof_full_Source_lifecycle_qualified=False)
if npcfactorytype:
 v['status']='program-updated-NPC-scalar-birth-factory-type-two-Source-signed-healing-ports-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=259,existing_changed_class_entries=5,new_helper_class_entries=2,pure_npc_factory_type_contract_scenarios=30,inherited_same_SHA_pure_contract_scenarios=1500,exact_Source_factory_class_signed_comparisons=896,frozen_Source_NPC_class_declarations=32,exact_relocated_Source_factory_oracle_methods=2,frozen_Source_files=73,inherited_exact_Source_controlled_positive_HP_comparisons=90,inherited_exact_Source_type_delta_comparisons=112,inherited_exact_Source_status_comparisons=160,inherited_exact_Source_lawful_comparisons=30,inherited_exact_Source_HP_arithmetic_comparisons=420,new_bounded_owned_metadata_fields=3,original_leaf_constructor_birth_ports=9,existing_signed_healing_math_ports=2,new_SELECTs=0,new_connection_ownership=False,new_scheduled_tasks=False,no_unbound_completion_cleanup_errors=True,original_AI_HP_RNG_cost_lock_life_WORLDbodies_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_','healing_formula_','healing_type_','healing_controlled_','first_player_discovery_','player_factory_','accepted_player_','npc_origin_','npc_first_life_','material_first_life_','area_player_proof_','indirect_player_proof_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 verify(contract['player_proof_factory_census']);v['player_factory_remaining_constructor_census']=contract['player_proof_factory_census']
 verify(build['method_reference_census']);v['npc_factory_type_method_reference_census']=build['method_reference_census'];verify(build['Source_factory_type_oracle']);verify(build['Source_factory_class_hierarchy']);v['npc_factory_type_Source_oracle']=build['Source_factory_type_oracle'];v['npc_factory_type_Source_class_hierarchy']=build['Source_factory_class_hierarchy']
 v.update(npc_factory_type_nine_leaf_scalar_birth_snapshots_program_wired=True,npc_factory_type_actual_owned_and_transferred_controlled_kind_program_wired=True,npc_factory_type_32_Source_classes_exact_signed_type_math_offline_matched=True,npc_factory_type_two_proven_Source_healing_math_ports_program_wired=True,npc_factory_type_original_SQL_AI_HP_RNG_locks_life_WORLD_preserved=True,npc_factory_type_full_restore_rollback_provenance_complete=False,npc_factory_type_actual_Source_factory_AI_qualified=False,npc_factory_type_guardian_tower_bot_periodic_all_immunity_complete=False,npc_factory_type_bound_factory_healing_HP_role_IO_Main_qualified=False,npc_factory_type_full_Source_lifecycle_qualified=False)
if npcfactorymemory:
 v['status']='program-updated-NPC-factory-type-original-Snapshot-memory-restore-transient-identity-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=264,existing_changed_class_entries=7,new_helper_class_entries=1,pure_npc_factory_memory_contract_scenarios=30,inherited_same_SHA_pure_contract_scenarios=1530,inherited_exact_Source_factory_class_signed_comparisons=896,frozen_Source_NPC_class_declarations=32,frozen_Source_files=73,inherited_exact_Source_controlled_positive_HP_comparisons=90,inherited_exact_Source_type_delta_comparisons=112,inherited_exact_Source_status_comparisons=160,inherited_exact_Source_lawful_comparisons=30,inherited_exact_Source_HP_arithmetic_comparisons=420,new_bounded_owned_metadata_fields=3,new_final_transient_snapshot_values=3,new_immutable_snapshot_constructor_siblings=3,original_snapshot_producer_ports=4,original_memory_restore_constructor_ports=4,new_SELECTs=0,new_connection_ownership=False,new_scheduled_tasks=False,no_unbound_completion_cleanup_errors=True,original_record_components_identity_HP_life_inventory_lock_WORLD_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_','healing_formula_','healing_type_','healing_controlled_','first_player_discovery_','player_factory_','accepted_player_','npc_origin_','npc_first_life_','material_first_life_','area_player_proof_','indirect_player_proof_','npc_factory_type_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 verify(contract['player_proof_factory_census']);v['player_factory_remaining_constructor_census']=contract['player_proof_factory_census']
 verify(build['method_reference_census']);v['npc_factory_memory_method_reference_census']=build['method_reference_census'];verify(build['Source_factory_type_oracle']);verify(build['Source_factory_class_hierarchy']);v['npc_factory_type_Source_oracle']=build['Source_factory_type_oracle'];v['npc_factory_type_Source_class_hierarchy']=build['Source_factory_class_hierarchy']
 v.update(npc_factory_memory_three_immutable_transient_snapshot_siblings_program_wired=True,npc_factory_memory_all_four_current_snapshot_producers_program_wired=True,npc_factory_memory_four_original_primary_snapshot_restores_program_wired=True,npc_factory_memory_known_controlled_evolution_without_relookup_program_wired=True,npc_factory_memory_record_identity_and_unknown_no_adoption_preserved=True,npc_factory_memory_full_bound_restore_rollback_lifecycle_qualified=False,npc_factory_memory_all_missing_unknown_factory_policy_complete=False,npc_factory_memory_bound_restore_AI_HP_role_IO_Main_qualified=False,npc_factory_memory_full_Source_lifecycle_qualified=False)
if npctargetfailure:
 v['status']='program-updated-NPC-frozen-target-failure-post-cost-selected-material-paths-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=266,existing_changed_class_entries=5,new_helper_class_entries=2,pure_npc_target_failure_contract_scenarios=37,inherited_same_SHA_pure_contract_scenarios=1560,exact_Source_NPC_failure_class_flag_comparisons=4608,inherited_exact_Source_factory_class_signed_comparisons=896,frozen_Source_NPC_class_declarations=32,frozen_Source_files=73,new_immutable_birth_flag_fields=2,new_immutable_scalar_constructor_siblings=1,original_selected_phase_ports=8,new_SELECTs=0,new_connection_ownership=False,new_scheduled_tasks=False,original_record_identity_HP_life_inventory_lock_WORLD_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_','healing_formula_','healing_type_','healing_controlled_','first_player_discovery_','player_factory_','accepted_player_','npc_origin_','npc_first_life_','material_first_life_','area_player_proof_','indirect_player_proof_','npc_factory_type_','npc_factory_memory_')):v[key]=value
 if 'goal_reset_request'in old:v['goal_reset_request']=old['goal_reset_request'];verify(v['goal_reset_request'])
 v['latest_goal_reset_attempt']=read(D/'source-goal-reset-attempt-current.json')['receipt'];verify(v['latest_goal_reset_attempt'])
 verify(contract['player_proof_factory_census']);v['player_factory_remaining_constructor_census']=contract['player_proof_factory_census']
 for key in ['Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['npc_target_failure_'+key]=build[key]
 v.update(npc_target_failure_exact_Source_class_flags_offline_matched=True,npc_target_failure_eight_selected_phase_ports_program_wired=True,npc_target_failure_original_cost_replay_dispatch_program_wired=True,npc_target_failure_original_PC_Ctrl_competition_and_WORLD_preserved=True,npc_target_failure_all_skill_roots_and_phase_order_complete=False,npc_target_failure_actual_bound_cost_RNG_HP_life_role_IO_Main_qualified=False,npc_target_failure_full_Source_lifecycle_qualified=False)
if negativephase:
 v['status']='program-updated-single-negative-original-Source-material-action-probability-before-MP-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=267,existing_changed_class_entries=1,new_helper_class_entries=1,pure_negative_phase_contract_scenarios=23,inherited_same_SHA_pure_contract_scenarios=1597,inherited_exact_Source_NPC_failure_comparisons=4608,inherited_exact_Source_factory_signed_comparisons=896,frozen_Source_NPC_class_declarations=32,frozen_Source_files=73,original_scalar_flag_ports=6,original_probability_blocks_conditionally_replayed=2,new_per_invocation_boolean_locals=2,new_metadata_fields=0,new_SELECTs=0,new_connection_ownership=False,new_scheduled_tasks=False,original_RNG_formula_fixture_cost_lock_life_Main_WORLD_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_','healing_formula_','healing_type_','healing_controlled_','first_player_discovery_','player_factory_','accepted_player_','npc_origin_','npc_first_life_','material_first_life_','area_player_proof_','indirect_player_proof_','npc_factory_type_','npc_factory_memory_','npc_target_failure_')):v[key]=value
 for key in ['goal_reset_request','latest_goal_reset_attempt']:
  if key in old:v[key]=old[key];verify(v[key])
 verify(contract['player_proof_factory_census']);v['player_factory_remaining_constructor_census']=contract['player_proof_factory_census']
 for key in ['Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['negative_phase_'+key]=build[key]
 v.update(negative_phase_original_blocks_after_material_action_before_MP_program_wired=True,negative_phase_original_PC_Ctrl_competition_and_WORLD_preserved=True,negative_phase_known_birth_class_flags_selected_program_wired=True,negative_phase_full_Source_target_status_numerical_semantics_complete=False,negative_phase_actual_bound_material_RNG_rollback_HP_life_role_IO_Main_qualified=False,negative_phase_full_Source_lifecycle_qualified=False)
if massselftarget:
 v['status']='program-updated-mass-slow-Source-self-primary-selected-secondary-order-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=268,existing_changed_class_entries=2,new_helper_class_entries=1,pure_mass_self_target_contract_scenarios=26,inherited_same_SHA_pure_contract_scenarios=1620,inherited_exact_Source_NPC_failure_comparisons=4608,inherited_exact_Source_factory_signed_comparisons=896,frozen_Source_NPC_class_declarations=32,frozen_Source_files=73,original_selected_self_ports=3,new_original_fields=0,new_SELECTs=0,new_connection_ownership=False,new_scheduled_tasks=False,original_cross_role_competition_Ctrl_cost_RNG_HP_life_lock_Main_WORLD_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_','healing_formula_','healing_type_','healing_controlled_','first_player_discovery_','player_factory_','accepted_player_','npc_origin_','npc_first_life_','material_first_life_','area_player_proof_','indirect_player_proof_','npc_factory_type_','npc_factory_memory_','npc_target_failure_','negative_phase_')):v[key]=value
 for key in ['goal_reset_request','latest_goal_reset_attempt']:
  if key in old:v[key]=old[key];verify(v[key])
 verify(contract['player_proof_factory_census']);v['player_factory_remaining_constructor_census']=contract['player_proof_factory_census']
 for key in ['Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['mass_self_target_'+key]=build[key]
 v.update(mass_self_target_three_selected_self_ports_program_wired=True,mass_self_target_original_secondary_order_and_NPC_failure_preserved=True,mass_self_target_absent_or_replaced_actor_no_adoption_preserved=True,mass_self_target_original_PC_Ctrl_competition_and_WORLD_preserved=True,mass_self_target_full_Source_visibility_and_membership_complete=False,mass_self_target_actual_bound_material_RNG_mutation_HP_life_role_IO_Main_qualified=False,mass_self_target_full_Source_lifecycle_qualified=False)
if nonetargetrequest:
 v['status']='program-updated-Source-none-target-original-caster-45-parsers-four-preAim-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=269,existing_changed_class_entries=5,new_helper_class_entries=1,pure_none_target_request_contract_scenarios=22,inherited_same_SHA_pure_contract_scenarios=1646,inherited_exact_Source_NPC_failure_comparisons=4608,inherited_exact_Source_factory_signed_comparisons=896,frozen_Source_NPC_class_declarations=32,frozen_Source_files=73,original_typed_attack_request_parser_ports=45,original_post_context_pre_Aim_request_ports=4,new_original_fields=0,new_SELECTs=0,new_connection_ownership=False,new_scheduled_tasks=False,original_selected_intent_Ctrl_competition_cost_RNG_HP_life_Main_WORLD_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_','healing_formula_','healing_type_','healing_controlled_','first_player_discovery_','player_factory_','accepted_player_','npc_origin_','npc_first_life_','material_first_life_','area_player_proof_','indirect_player_proof_','npc_factory_type_','npc_factory_memory_','npc_target_failure_','negative_phase_','mass_self_target_')):v[key]=value
 for key in ['goal_reset_request','latest_goal_reset_attempt']:
  if key in old:v[key]=old[key];verify(v[key])
 verify(contract['player_proof_factory_census']);v['player_factory_remaining_constructor_census']=contract['player_proof_factory_census']
 for key in ['Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['none_target_request_'+key]=build[key]
 v.update(none_target_request_45_original_typed_parsers_program_wired=True,none_target_request_four_original_captured_pre_Aim_paths_program_wired=True,none_target_request_original_source_binding_pose_no_fresh_adoption_preserved=True,none_target_request_original_Ctrl_competition_cost_and_WORLD_preserved=True,none_target_request_all_packet_kinds_and_skill_roots_complete=False,none_target_request_NPC_class_empty_PC_cost_semantics_complete=False,none_target_request_actual_bound_cast_cost_RNG_HP_life_role_IO_Main_qualified=False,none_target_request_full_Source_lifecycle_qualified=False)
if objecttargetrequest:
 v['status']='program-updated-Source-object-none-original-caster-raw-item-split-30-parsers-three-preAim-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=270,existing_changed_class_entries=4,new_helper_class_entries=1,pure_object_target_request_contract_scenarios=39,inherited_same_SHA_pure_contract_scenarios=1668,inherited_exact_Source_NPC_failure_comparisons=4608,inherited_exact_Source_factory_signed_comparisons=896,frozen_Source_NPC_class_declarations=32,frozen_Source_files=73,original_typed_object_request_parser_ports=30,original_post_context_object_request_ports=2,original_post_context_blind_attack_request_ports=1,Source_raw_item_selector_skills_preserved=5,new_original_fields=0,new_SELECTs=0,new_connection_ownership=False,new_scheduled_tasks=False,original_inventory_payload_selected_intent_Ctrl_competition_cost_RNG_HP_life_Main_WORLD_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_','healing_formula_','healing_type_','healing_controlled_','first_player_discovery_','player_factory_','accepted_player_','npc_origin_','npc_first_life_','material_first_life_','area_player_proof_','indirect_player_proof_','npc_factory_type_','npc_factory_memory_','npc_target_failure_','negative_phase_','mass_self_target_','none_target_request_')):v[key]=value
 for key in ['goal_reset_request','latest_goal_reset_attempt']:
  if key in old:v[key]=old[key];verify(v[key])
 verify(contract['player_proof_factory_census']);v['player_factory_remaining_constructor_census']=contract['player_proof_factory_census']
 for key in ['Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['object_target_request_'+key]=build[key]
 v.update(object_target_request_30_original_typed_parsers_program_wired=True,object_target_request_two_object_one_blind_original_preAim_paths_program_wired=True,object_target_request_five_exact_Source_raw_item_selectors_preserved=True,object_target_request_original_source_proof_Ctrl_competition_cost_WORLD_preserved=True,object_target_request_all_packet_kinds_and_skill_roots_complete=False,object_target_request_full_inventory_character_selection_semantics_complete=False,object_target_request_actual_bound_inventory_cast_cost_RNG_HP_life_role_IO_Main_qualified=False,object_target_request_full_Source_lifecycle_qualified=False)
if areaclassselection:
 v['status']='program-updated-Source-area-class-selection-twelve-admitted-target-loops-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=272,existing_changed_class_entries=1,new_helper_class_entries=2,pure_area_class_selection_contract_scenarios=30,inherited_same_SHA_pure_contract_scenarios=1707,exact_Source_area_class_only_comparisons=91476,inherited_exact_Source_NPC_failure_comparisons=4608,inherited_exact_Source_factory_signed_comparisons=896,frozen_Source_NPC_class_declarations=32,frozen_Source_files=73,original_admitted_probability_target_iterator_ports=12,original_area_roots=4,normal_and_exception_CFG_admission_dominance_verified=True,audience_iterator_ports=0,new_original_fields=0,new_SELECTs=0,new_connection_ownership=False,new_scheduled_tasks=False,original_audience_Ctrl_competition_cost_RNG_HP_life_Main_WORLD_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_','healing_formula_','healing_type_','healing_controlled_','first_player_discovery_','player_factory_','accepted_player_','npc_origin_','npc_first_life_','material_first_life_','area_player_proof_','indirect_player_proof_','npc_factory_type_','npc_factory_memory_','npc_target_failure_','negative_phase_','mass_self_target_','none_target_request_','object_target_request_')):v[key]=value
 for key in ['goal_reset_request','latest_goal_reset_attempt']:
  if key in old:v[key]=old[key];verify(v[key])
 verify(contract['player_proof_factory_census']);v['player_factory_remaining_constructor_census']=contract['player_proof_factory_census']
 for key in ['Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['area_class_selection_'+key]=build[key]
 v.update(area_class_selection_twelve_admitted_target_loops_program_wired=True,area_class_selection_exact_Source_class_only_matrix_offline_matched=True,area_class_selection_original_snapshot_birth_no_legacy_replacement_adoption_preserved=True,area_class_selection_original_audience_Ctrl_competition_cost_WORLD_preserved=True,area_class_selection_all_Source_isTarget_clauses_and_roots_complete=False,area_class_selection_actual_bound_cast_cost_RNG_HP_life_role_IO_Main_qualified=False,area_class_selection_full_Source_lifecycle_qualified=False)
if damagehealingselection:
 v['status']='program-updated-Source-damage-healing-thirteen-post-MP-target-loops-none-primary-full-Source-open'
 v['program_checks'].update(JVM_no_initialization_classes=274,existing_changed_class_entries=2,new_helper_class_entries=2,pure_damage_healing_selection_contract_scenarios=33,inherited_same_SHA_pure_contract_scenarios=1737,inherited_Source_class_only_comparisons=91476,inherited_exact_Source_NPC_failure_comparisons=4608,inherited_exact_Source_factory_signed_comparisons=896,frozen_Source_NPC_class_declarations=32,frozen_Source_files=73,original_post_MP_target_calculation_iterator_ports=13,original_damage_healing_roots=4,original_none_primary_data_ports=2,normal_and_exception_CFG_MP_debit_dominance_verified=True,audience_HP_application_iterator_ports=0,new_original_fields=0,new_SELECTs=0,new_connection_ownership=False,new_scheduled_tasks=False,original_HP_FIFO_audience_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained=True)
 for key,value in old.items():
  if key.startswith(('cube_','queued_PC_','damage_ledger_','shared_PC_','Source_dequeue_','Source_damage_','all_NPC_','all_cube_','natural_NPC_','all_natural_NPC_','all_single_NPC_','scoped_PC_','all_PC_','raw_NPC_','all_raw_','all_natural_Ref_','hit_reaction_','all_hit_reaction_','ground_','all_ground_','fatality_','all_actual_','NPC_PC_','raw_HP_','NPC_magic_','signed_recovery_','healing_cast_','healing_PC_','healing_formula_','healing_type_','healing_controlled_','first_player_discovery_','player_factory_','accepted_player_','npc_origin_','npc_first_life_','material_first_life_','area_player_proof_','indirect_player_proof_','npc_factory_type_','npc_factory_memory_','npc_target_failure_','negative_phase_','mass_self_target_','none_target_request_','object_target_request_','area_class_selection_')):v[key]=value
 for key in ['goal_reset_request','latest_goal_reset_attempt']:
  if key in old:v[key]=old[key];verify(v[key])
 verify(contract['player_proof_factory_census']);v['player_factory_remaining_constructor_census']=contract['player_proof_factory_census']
 for key in ['damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['damage_healing_selection_'+key]=build[key]
 v.update(damage_healing_selection_thirteen_original_post_MP_target_rolls_program_wired=True,damage_healing_selection_original_wrappers_and_known_evolution_preserved=True,damage_healing_selection_two_none_primary_data_ports_program_wired=True,damage_healing_selection_original_HP_FIFO_Ctrl_competition_cost_WORLD_preserved=True,damage_healing_selection_all_Source_target_and_single_direct_branches_complete=False,damage_healing_selection_actual_bound_cast_cost_RNG_HP_life_role_IO_Main_qualified=False,damage_healing_selection_full_Source_lifecycle_qualified=False)
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=runtime,manifest=pin(run/'manifest.json'),system_goal=pin(goal),new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))
text=work.read_text('utf-8');text=re.sub(r'現行候選：`[0-9a-f]{64}`',f'現行候選：`{digest}`',text);text=re.sub(r'現行JAR：[^\n]*方法、[^\n]*本地呼叫點、',f"現行JAR：{inv['observations']['current_archive_methods']:,}方法、{inv['observations']['current_archive_local_call_sites']:,}本地呼叫點、",text)
text=re.sub(r'(現行JAR：[^\n]*本地呼叫點、)\d+個特定HP',lambda match:match[1]+str(inv['observations']['current_direct_HP_call_sites'])+'個特定HP',text)
artifact_paths={Path(ref['path']).name:Path(ref['path']).as_posix()for ref in inv['artifacts']}
text=re.sub(r'(?<=\()D:/[^)\n]*/source-completion-inventory/[^)\n]*/(skills.tsv|controls.tsv|hp-producers.tsv)(?=\))',lambda match:artifact_paths[match[1]],text)
a=text.index('下一批生產端已凍結');b=text.index('\n\n## 本次已更新的程式',a)
text=text[:a]+f'下一批生產端已凍結{len(read(Path(contract["Source_excerpts"]["path"])))}個Source方法：[Source參考收據]({(D/"source-next-producer-contracts-current.json").as_posix()})。立方體傷害來源是實際效果NPC。娃娃SKILL保留接受後魔法數字與PC true／false；UnifiedProcExecutor則把加傷放在技能之後，獨立扣HP、扣HP前魔法數字及PC false／false。減傷沿用防禦計算；不能把加傷合入物理傷害。PhysicalHitCommitPlan在物理接受後、物理數字前執行proc副作用。'+text[b:]
a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
text=text[:a]+f'''## 本次已更新的程式

娃娃Source專用分支改為技能HP先於技能特效，加傷HP位於技能迴圈之後。技能PC旗標true／false、接受後數字；加傷PC false／false、扣HP前數字，保留防禦反饋及獨立HP事件。原機率與計算本體、WORLD／未載入Source的完整原方法保留。

PC在這次娃娃HP消費端成對等待前固定雙方binding／生命，顯示關閉仍保留扣HP前guard；接受致死時同步預先數字的死亡生命token。Quest／Time技能沿用接受後magic receipt，加傷保留原HP葉節點、預先數字與脫離Context的NPC proof；例外返回仍沿用實際接受時封存的生命token。既有有界Jobs及HPBAR沿用。

程式檢查：{count}原方法獨立inverse、13類JVM不初始化、凍結同位元重建通過；20個SQL class保持原位元；新增遊戲案0、DB連線0。前次毒傷更新保留於候選祖先收據。

**娃娃尚未全部完成**：原物理動作首次等待／機率前捕捉、實際物理命中接受後回呼、受控NPC分支、未支援的SKILL（含29緩速）、每筆proc失敗續作、減傷完整稽核與特效最終接收端仍開放。現在的成對guard只覆蓋娃娃HP消費端，不能當作整次攻擊生命週期合格。

'''+text[b:]
if accepted:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

PC物理HP事件攜帶已算好的娃娃觸發結果，拒絕／丟棄事件不執行娃娃效果；原本送入HP佇列後立即執行的PC回呼，改由既有接受後完成流程在物理數字之前執行。Pending一次領取並清除，不持有Context／Scope／Receipt／Frame／Plan。沿用舊佇列、完成工作者、關閉與退役流程；不重擲娃娃proc機率。

雙方binding／生命／位置在這個物理消費端成對等待前固定，actual HP guard及接受後回呼沿用原證明。致死命中先略過SKILL傷害計算，保留原gfx／加傷。前一層技能先HP後特效、加傷最後獨立HP、PC true／false與false／false及數字時點保留。

{count}原方法獨立inverse、13類JVM不初始化、凍結同位元重建通過；20 SQL class原位元；0新增遊戲案／0DB連線。自然NPC及WORLD原橋接保留。**尚缺**整次攻擊最初等待／RNG前捕捉、自然與受控NPC接受後接線、未支援SKILL（含29）、每筆proc失敗續作、減傷與所有特效接收端；實際關閉與成對等待矩陣集中驗證仍未進行。

'''+text[b:]
if npc:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

自然Quest／Time物理HP沿用最初捕捉的Aim、已算好的娃娃proc與NPC stats；四個原HP返回點在Hub鎖釋放後固定真正接受的生命證明。原命中的死亡／波次／獎勵發布完成後（含例外返回），才依該證明執行娃娃，再發布物理數字。拒絕事件沒有娃娃回呼；換怪物、換場景或退休也不會重新認領數字ID。

受控NPC的原物理方法保留為專用sibling，在原appliedDamage正值／Target.seal處執行娃娃，補上原先直接return漏掉的路徑。保留實際主人／寵物／背包／placement／生命與原成對鎖；技能走既有CombatCompanionDamage.area的寵物倍率及NPC MR，加傷保持raw，避免重複倍率。後續技能／加傷與致死數字沿用相同actor及接受token。

Scope只在同步動作堆疊與Context暫存，離開即復原；延遲數字Plan只保留獨立Proof，不持有Scope／Context／Receipt。{count}原方法inverse、16類JVM不初始化、凍結同位元重建通過；20 SQL class保持原位元；0新增遊戲案／0DB連線。

**尚未完成**：PC整次攻擊最初等待／proc RNG前與實際娃娃實例捕捉、NPC完整免疫／buff語意（Source包含IMMUNE_TO_HARM）、未支援SKILL29等、每筆proc失敗續作、減傷與所有特效最終接收端、實際關閉／成對等待矩陣。全Source與58材料清單仍開放；這次只更新程式，尚非原生或實機資格化。

'''+text[b:]
if action:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

Source BATTLE物理入口先固定施攻者binding／生命／位置、背包實例及娃娃召喚tuple，再建立場景快照；選定的實際玩家／NPC在成對或原受控主人鎖等待前固定。原攻擊本體第一個原指令／RNG前重驗角色與目標。受控目標沿用原owner鎖流程，避免先多拿一組成對鎖。

activeMagicDoll加入volatile發布。娃娃以objectId／背包物件ID／原type／expiresAt識別，位置紀錄改變仍是同次召喚；背包Item只在動作鎖內、proc RNG前首次封存，不在鎖外讀取可變map。原娃娃能力getter保留本體；一次攻擊固定原召喚能力或空能力，換娃娃不會把新能力套上舊攻擊。**到期／收回只使舊娃娃能力或proc失效，不阻擋一般物理HP。**

既有PC Pending與自然／受控Scope只攜帶獨立Stamp；Pending不持有Action／Context／Scope／Receipt。queued proc重驗原娃娃及物件，原HP接受guard與既有完成／關閉工作者保留。{count}原方法inverse、15類JVM不初始化、凍結同位元重建通過；另11個純召喚判定案例涵蓋移動、收回、新召喚、同ID新到期時間與到期邊界。20 SQL class原位元；0新增遊戲案／0DB連線。

Source參考已補到20方法。**尚缺**娃娃泛用SKILL數值與視覺metadata／魔法傷害計算、29緩速、target castgfx先於娃娃攻擊動作、NPC免疫buff、逐筆失敗續作、減傷、所有特效最終接收端與實際入口／成對等待／關閉矩陣。29即使基礎骰為0，Source仍可有暴擊RNG及原始／裝備／天賦傷害，不能只加無傷緩速。完整Source／58材料清單仍開放。

'''+text[b:]
if skills:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

泛用娃娃SKILL改讀同一份不可變Source技能列（實際skill_level／attribute／castgfx／duration／damage／dice／count），不再限於areaAttackSpell模型。讀取只增加既有啟動readonly且autocommit=false交易內的一條SELECT，不擁有新Connection／交易，不在命中時查SQL；失敗不發布半份快照。

Source專用loop依序為加傷gfx、逐筆技能計算／HP提交、目標castgfx、娃娃67動作、29緩速，最後獨立raw加傷。技能失敗繼續下一筆，送包失敗不回滾HP或阻止速度狀態；不走普通施法扣MP／材料／機率檢查。原PC／NPC魔法方法保留sibling，只把推算技能等級改成同一凍結列提供的等級；既有魔法本體尚未涵蓋的Source項目仍明列缺口。

技能29已有PC／受控／自然速度接線；PC純狀態案例驗正常套緩速、加速抵消、已緩速不續期。proc_spr暫存偏好預設true，接到既有Source動作處理入口；目標特效依Source10格棋盤距離及接收者偏好，娃娃動作依既有可見畫面。尚待角色切換重設與實際選單可達性驗收。

{count}原方法inverse、16類JVM不初始化、凍結同位元重建通過；18個純案例涵蓋唯讀／autocommit拒絕、空／重複／NULL／錯誤數值、失敗快照原子保留、速度及proc觀看邊界。20既有SQL class原位元；0新增遊戲案／0實際DB連線。新SELECT仍須後續中文MySQL真實唯讀驗證。

**優先尚缺**：所有proc最終Frame之接收角色／來源／目標生命與原娃娃證明；proc_spr角色重設；PC佇列內多筆技能的真正接受／拒絕與後續proc／物理數字次序。NPC完整免疫buff／SKNPC／PLNPC、原魔法本體未載入的守護／天賦、NPC負傷害回復、0或超長時間與原生加速NPC的29狀態、減傷及完整關閉矩陣也未完成。**這是程式更新，尚非全部娃娃、全Source或實機資格化。**

'''+text[b:]
if output:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

三個娃娃輸出迴圈（目標特效／娃娃動作／速度）固定原接收者binding，沿用既有owned publication scope發布，在真正native Frame重驗角色／生命／場景。特效另驗原娃娃召喚／type／到期、proc_spr與目前可見範圍；最終檢查不拿action／Hub／status／inventory鎖。HP／數字Plan不持有View／Context／Scope／Receipt，沒有新增worker／TLS／排程器／registry。

已接受S_SPEED結果獨立保存實際目標、主人／場景／生命及原加速／緩速到期值；移動不換Actor，收回娃娃不使這份已接受狀態的proof失效，proc_spr也不阻擋速度結果；仍沿用原publication scope的施攻者／peer角色授權，來源退休時的送達行為尚待實跑。四個原Actor類別共8個到期欄位補volatile可見性，不改時間或狀態計算；解除／新狀態／到期後拒絕舊包，目標本人封包在最終Frame更新剩餘秒數。proc_spr在原selected角色邊界與傷害偏好一起、於原動作鎖內重設true。

Source參考已到29方法：DamageCommit.enqueue只表示非阻塞入列，drainer才扣HP並afterAccepted；Source本來不等待每筆子傷害完成就繼續技能特效與下一筆proc。**修正先前「要新增逐筆等待／continuation chain」的假設**，保留Source次序，不加入另一套同步模型；FIFO／拒絕／致死與最終發布仍須同SHA驗證。

{count}原方法inverse、22類JVM不初始化、凍結同位元重建通過；15純案例驗實際獨立timer proof、新lease／解除／到期、加速抵消與線上秒數計算。20 SQL class原位元；0新增遊戲案／0實際DB連線。這些是程式證據，尚非完整Frame／角色／Main實跑資格化。

**尚缺**：NPC完整免疫／SKNPC／PLNPC、原魔法未載入的守護／天賦、NPC負傷害回復、0或超長時間與原生加速NPC的29狀態、減傷等Source數值與消費端；全部58材料及其他HP生產端／外控也仍開放。已接線部分仍需集中驗真正Frame等待、換角色／場景／生命／原娃娃、偏好重設及選單、速度移動／到期／替換／收回、原非阻塞FIFO與關閉矩陣。新技能reader後續驗中文MySQL實際唯讀重載；未部署。

'''+text[b:]
if slow:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

娃娃技能29接到既有status／pet／Hub鎖與計時器：一般速度套緩速、可識別加速只抵消、已緩速不刷新；受控NPC只有原生加速且無可移除技能時保持原狀。自然NPC的完整template原生速度仍未確認，不能從受控NPC推算完成。

依Source的int秒數乘1000選計時器：零或非正結果為永久，有限長時間保留真正期限；速度包依writeH取低16位。PC原同步speed查詢加有界前綴，處理永久值與長期限而不溢位；原取消／到期／角色清除本體保留。四個原timer owner各加一個只持有Object身分的volatile epoch，既有獨立Speed proof保存final epoch並重驗，拒絕解除後重新施加相同永久期限的舊包；不新增actor registry／worker／TLS／排程器。

沿用父版最終Frame的接收角色／生命／場景／狀態proof，更新本人封包的剩餘秒數；仍有效但低16位為0的時間不誤判為到期。來源／peer的原publication scope gate保留，來源退休送達尚待實跑。父版特效／娃娃／proc_spr、原非阻塞子傷害次序與生命週期限制保留。

{count}原方法inverse、23類JVM不初始化、凍結同位元重建與25純案例通過。案例含實際BattleTimedStatus的永久／取消／長期限／到期，以及實際Speed proof的同永久期限替換拒絕；5份Source檔唯讀凍結。20 SQL class原位元；0新增遊戲案／0實際DB連線。這些尚非實機、Main或完整競技場資格化。

**尚缺**：NPC完整免疫／SKNPC／PLNPC、守護／天賦、NPC負傷害回復、減傷、自然NPC原生速度；全部58材料、其他HP生產端／外控與完整同SHA矩陣仍開放。依指示先程式更新，之後集中後端／中文MySQL真角色唯讀／實機游標驗收。目標active，不部署。

'''+text[b:]
if defense:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

娃娃減傷沿用候選既有NativePlayerCommit計算／RNG、真正接受後一次發布及裝備反饋尾段，不新增第二套減傷。原Source BATTLE Context入口固定房內角色binding及各原召喚，子Context沿用；原drawDefense保留既有monitor與迴圈，只在原受擊者動作鎖內封存背包Item。等待期間替換／收回／到期的娃娃不給舊動作新能力，普通HP不因特效proof失效而被拒絕。

既有DefenseVisual新增委派建構子與獨立proof，保存原受擊角色／生命／位置／背包／召喚及接收者binding，不持有Context／Scope／Receipt／Frame。真正HP接受邊界先封存致死生命token，接受後沿用原發布與裝備尾段；目標gfx按proc_spr及10格，娃娃67按原可見畫面，真正Frame再驗接收角色／生命／原娃娃／偏好／範圍。計算至proof建立期間失效有明確拒絕值，不回退抓新娃娃。動態Frame載體finally復原，最終檢查不拿actor／Hub／status／inventory鎖；無新worker／TLS／排程器／registry／SQL。

Source證據增至35個方法，確認減傷先計算入列、零差額事件仍可接受、受傷特效在接受後且reductionOnly保留減傷特效。{count}原方法inverse、31類JVM不初始化、凍結同位元重建與10純觀看範圍／偏好案例通過。原20 SQL class原位元；0新遊戲案／0實際DB連線。以上不代替零差額、致死、Main及真正Frame實跑。

**尚缺**：沒有已證明Context／動作鎖的減傷生產端及被動娃娃數值仍需補等待前召喚覆蓋；NPC完整免疫／SKNPC／PLNPC、守護／天賦、負傷害回復、自然NPC原生速度亦未完成。全部58材料、cube實際效果NPC及其餘HP／AI／外控、同SHA生命週期矩陣仍開放。原無Context、WORLD／未載入路徑保留後備；先程式更新，再集中後端／中文MySQL真角色唯讀／游標驗收。目標active，不部署。

'''+text[b:]
if passive:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

共同娃娃能力getter的原物理Action分支保留，只在無Action的後備分支接到已存在Source BATTLE Context的原角色快照。各Snapshot增加一份final、依角色物件識別的Choice表，大小沿用原房內快照，子Context共享。第一次在該角色原動作鎖內讀取時，固定原召喚的完整不可變Source能力或空能力；後續getter不混入替換娃娃。即使已有快取，角色／生命／位置與背包換代仍拒絕舊能力。未在Context或未持有原角色鎖、WORLD／未載入的讀取保留原getter後備。

已封存的普通數值不因之後特效召喚退休而被重算；第一次讀取前原召喚已失效則固定空能力，不讓新娃娃補給舊動作。Choice由原Context的Snapshot持有；本次未把Choice／Snapshot／Context加入娃娃HP／防禦／特效proof。其他既有保留Context的事件仍沿用父版界限，尚待完整關閉矩陣。沒有新增actor／Hub／status／inventory鎖、RNG、worker、timer、TLS、registry或SQL。ClientSession所有原數值／成本／RNG／HP／queue類別位元保留，僅兩類的getter後備與Snapshot建構子接線改變。

{count}原方法inverse、37類JVM不初始化、凍結同位元重建與10純resolver／真Source能力列案例通過；20 SQL class原位元、0新遊戲案／0實際DB連線。獨立[消費端盤點]({(D/'source-doll-consumers-current.json').as_posix()})從同SHA列出31個共同getter呼叫點（30個方法）、22個直接HP入口方法及兩個原生Fatality明確null參數片段；不把這些數量當成完成案數，也不從方法內未見Context就推斷上游缺失。

**尚缺**：逐一補／證明所有受傷生產端與被動getter的等待前Context／原鎖覆蓋。目前只更新已捕捉Context且持有原角色鎖的領域。Source只在真正attacker非null時做受傷proc計算；cube實際效果NPC、玩家主人、真正空來源與不透明Java null不可混用，須逐入口接線。NPC免疫／SKNPC／PLNPC、守護／天賦、負傷害回復、自然NPC原生速度、全部58材料、其他HP／AI／外控及同SHA生命週期矩陣仍開放。先程式更新，後集中MySQL真角色唯讀與實機游標驗收。目標active，不部署。

'''+text[b:]
if cube:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

燃燒205／和諧220的原Cube物件增加不可變Entity來源資料：保留真正原物件／編號、效果NPC80149／80152、原引擎及clear代次、原Room場景與僅供關係追查的建立者binding。效果NPC仍與玩家建立者分開；**目前尚未改HP事件的施攻者欄位**。

既有GameHub同步安裝與掃描點暫存creator／scan，finally復原；已綁定BATTLE的引擎在原Hub鎖內、等待原engine前，固定PC角色binding／生命，以及Quest／Time真正actor／owner／generation／生命。沒有新增actor或Hub／engine鎖；BOT、歧義或未綁定身分不冒充Source Monster／PC。每個StatusLease先委派原建構子，再保存原Entity及目標來源；週期／到期Event攜帶精確原狀態實例。只有剛安裝的APPLIED事件在原engine鎖內讀剛發布的status列，同ID重裝不把舊tick認成新狀態。

clear增加原有資料代次的替換，原清除本體保留。八秒狀態即使在二十秒cube物件消失後，仍保留原效果物件，符合Source的_effect參考。沒有新增Context／Scope／Receipt／Frame保留、static actor registry、worker、timer、TLS、RNG或SQL。原時鐘／關係／成本／HP指令透過{count}原方法inverse保留；WORLD原引擎行為維持。

22類JVM不初始化載入並核對實際Room／encounter／生命欄位，凍結同位元重建與15個真實cube引擎純案例通過，包含同ID狀態重裝、精確舊到期事件、clear代次與cube消失後的原狀態。20 SQL class原位元、0新遊戲案／0實際DB連線；這些尚非真正BATTLE／角色／Main資格化。

**下一步且尚未完成**：把來源資料接到PC raw false／true與自然NPC HP來源、實際提交及最終接收Frame；保留目標型別／免疫／受傷動作抑制、原施放者關係與無一般魔法數字。現有扣血生產端尚未因這層metadata而修復，Source／原生掃描節奏與建立者退役亦須逐消費端核對。娃娃未覆蓋入口與NPC完整數值、全部58材料、其他HP／AI／外控及同SHA矩陣仍開放。目標active，依指示先程式更新、後集中MySQL真角色唯讀與實機游標驗收，不部署。

'''+text[b:]
if cubeplayer:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

PC燃燒／和諧HP已改接原Entity／Lease／Target。原HP Event使用kind5 effect引用，保存真正cube object id與Effect(Entity, application lifetime)，沒有玩家master／caster。建立者只保留關係資料，不當扣血施攻者或擊殺來源；效果NPC存續與目標HP／八秒狀態分開。原kind1..4及WORLD／未載入後備本體保留。

PC走Source raw10／25、false／true與明確空數字Plan，沿用原防禦／裝備／Avatar及HP佇列，不加一般魔法MR／屬性／暴擊／材料／MP／施放機率。燃燒檢查原零傷害狀態並先呼叫發布受傷動作；和諧不額外加動作或一般魔法數字。發布沿用原owned scope，真正Frame驗接收角色、可見範圍及隱身；邏輯呼叫順序不代替實際網路順序驗收。

原狀態目標身分在self-pair等待前固定，入列前在既有Hub／engine鎖下驗精確狀態；入列後HP proof只驗原效果代次／場景及角色／生命，不因狀態之後到期或cube消失而取消已提交事件，允許同場景移動及原DRAINING完成。既有真正HP precommit與accepted Frame接上typed Ref／Plan／旗標guard，致死接受點先封存接受生命；最終Frame再驗目標與固定接收者binding，不拿actor／Hub／status／inventory鎖。原engine advance於原鎖內退休不再屬於原PC角色／生命的舊lease，避免同ID換代繼承或阻擋新狀態。

Queued Proof／Effect沒有Context／Scope／Receipt／Frame／Plan欄位，沿用原有界佇列與publication，無新worker／timer／TLS／registry／SQL。effect kind不走一般NPC反饋、反射或攻擊者受傷動作，原防禦減傷仍保留。{count}原方法inverse、35類JVM不初始化、凍結同位元重建與22純旗標／引用／身分契約案例通過；20 SQL class原位元、0新遊戲案／0實際DB連線。

**尚未完成**：自然NPC HP／動作抑制／HPBAR仍須使用真正效果NPC及原actor，建立者退出時的非玩家派送亦待接；狀態／MP／aura角色路徑、Source／原生掃描節奏與全同SHA實跑矩陣仍開放。PC這一批尚無實際佇列／Frame／致死／角色／Main驗收，不宣告整體Cube Source完成。娃娃未覆蓋入口、其餘NPC數值、全部58材料與外控／AI目標仍保留。目標active，先程式更新、後集中MySQL真角色唯讀及實機游標驗收，不部署。

'''+text[b:]
if cubencplease:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f"""## 本次已更新的程式

燃燒／和諧的Source自然MONSTER狀態，已在原Hub／engine同步掃描點增加精確退役判定。原StatusLease／Cube實例、效果engine代次與Quest／Time真正actor、owner、生命及encounter代次均須符合先前固定的掃描；同ID換actor、死復活、新owner／代次或退役不繼承舊狀態，也不阻擋新角色安裝新lease。原NPC移動出cube範圍仍保留原狀態，不把cube可見性或建立者在線當成存續條件。

本批只改一個既有helper的退役前綴，沒有新增鎖、carrier、worker、timer、TLS、registry、RNG或SQL。PC／BOT map項目、WORLD／未綁定後備及原到期／節奏／HP／MP／queue／輸出本體保留。{count}原方法獨立inverse、36類JVM不初始化、27個真實引擎狀態map與獨立身分metadata純案例、凍結同位元重建通過；20 SQL class原位元、0遊戲案／0實際DB連線。純案例沒有建立玩家／Hub／socket或冒充BATTLE，不能代替實跑。

父候選00c8f86d6370的PC raw10／25、false／true、實際效果NPC來源、原HP接受及最終接收guard接線完整保留；父候選的35類／22案只作歷史範圍，不加算為本SHA完整回歸。

**下一步且尚未完成**：NPC已產生的callback仍須於實際HP點驗原actor；建立者退出後的非玩家派送、效果NPC原來源、受傷動作抑制與HPBAR仍未完成。MP／狀態／aura消費端、Source／原生掃描節奏、全部58材料／外控／NPC AI、娃娃數值與未覆蓋入口及同SHA角色／生命／Frame／Main實跑矩陣均保留。先程式更新、後集中中文MySQL真角色唯讀及實機游標驗收；不部署。

"""+text[b:]
 text=text.replace('系統目標已於2026-10-24依使用者指示重新建立並啟用（active）。','2026-10-24使用者再次要求重設；目標工具仍回報既有未完成active目標並拒絕建立新目標。本次保留完整範圍與進度，沒有將未完成目標標為完成或宣稱重設成功。')
if cubenpc:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f"""## 本次已更新的程式

燃燒／和諧的兩個原時鐘派送點，已在舊caster-null判定前接入自然NPC typed消費端；兩個舊PC非玩家入口亦有Source BATTLE前綴。HP使用真正Cube／Entity／精確Lease／原Target，固定Quest／Time actor、owner、生命及encounter代次，走raw10／25與兩個保留原Hub同步／死亡／波次／生成／結束規則的受限副本，不再要求建立者在線。Quest於原Cube建構時固定建立者陣營，沿用原同陣營怪物可攻擊限制，不取換角後派系，也不把目標lane直接當新攻擊權限。原WORLD／未載入／BOT後備保留。

新增Source參考確認：L1SkillUse建立Cube時傳入原user，spawnEffect設master；L1Monster先以effect入列，再於接受處理將effect master用於仇恨、HPBAR與死亡歸屬。新Ref使用實際效果NPC編號及原target；HP權限與原master歸屬分開，換角色不能繼承HPBAR／lawful獎勵。Native仇恨／family-link／AI完整接線仍未完成。

原精確lease、燃燒freeze／earth在入列前驗；已admit後不因狀態到期、cube消失或建立者退出而取消，仍驗effect代次／場景／原actor生命。和諧不再誤套普通earth guard；不加一般魔法MR／抗性／暴擊／材料MP／機率或魔法數字。燃燒受傷動作使用既有gfx抑制清單與10格，先於HP發布，IO不持有HP／Hub／engine／admission鎖。

三個原Quest／Time／wave發布方法的static受限副本，以typed Commit替換全部PC欄位／接收呼叫；null參數僅保留原operand位置，不是假的施攻者。死亡／poison清除、波次／新怪／邊界／公告、原獎勵與結束流程保留，接收IO失敗不回滾HP。HPBAR僅送原master且保留設定／畫面限制；沿用有界weak-viewer cache，死亡隱藏既有觀看者，比例運算避免溢位且最終拒絕舊比例。原接受生命與新怪生命在owner內封存，最終Frame驗effect／target／receiver／spawn；dynamic carrier在finally復原。

沿用原application pending計數與executeCommitted因果範圍，已admit的同步工作於finally恰好扣回，DRAINING完成原工作、STOPPED拒絕。沒有新queue／worker／timer／TLS／static registry；Commit／Effect／Output不保留Context／Scope／Receipt／Frame／Plan。**目前保留原生NPC同步HP路徑，尚未完整對齊Source非同步FIFO，仍是必補程式缺口。**

本SHA的{count}原方法／副本獨立inverse、41類JVM不初始化、37個真實引擎事件／身分／派系／旗標／封包純契約及凍結同位元重建通過；45個Source方法已凍結，20 SQL class原位元，0新遊戲案／0DB連線。這些不是實際HP／Main／游標驗收，歷史案數不加算。

**下一步且尚未完成**：Source NPC FIFO與原master AI、MP／狀態／aura跨角色消費端、實際Source受傷抑制表讀取器與掃描節奏、全部58材料／外控／NPC AI、娃娃數值／未覆蓋入口、正式0.6.71隔離合成及同SHA角色／生命／Frame／Main矩陣。先程式更新，後集中中文MySQL真角色唯讀與實機游標；目標active，不部署。

"""+text[b:]
if cubenpcqueue:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f"""## 本次已更新的程式

NPC立方體原prepare／受傷動作之後，已改為非阻塞加入原actor之BattleTimedStatus所擁有的既有FIFO。Ticket保存原Commit與精確原queue，不重查數字ID、不冒充玩家貢獻；原sealed Plan只明確新增這個型別，其他允許型別保留。產生端舊inline HP／手動pending計數已移除，不等待HP。

沿用原encounter proc drainer，在原try/finally內先辨識cube Ticket，再走相同Queue.complete；舊proc body不變，兩者共享同目標順序。原batch256、工作者、期限／拒絕重試、清列及Main drain／stop流程保留。HP點再驗原queue的真正active event、effect編號、raw量、無假玩家欄位及原場景，只有已queued且實際claimed的事件可進Hub HP副本；claimed旗標於finally清除。原效果／target／accepted／final Frame身分guard保留。

NPC入列不再持queue等待admission；PC／NPC傷害容量使用同一CAS及原GLOBAL值，PC發布失敗扣回預留。NPC在STOPPED最後檢查前註冊原lane，沿用原deadline／clear／claim計數。七個既有AI等非傷害生命週期credit writer位元不變，尚未宣稱整個application pending就是Source傷害專用容量。

原clear／拒絕耗盡／terminal stop在原queue monitor下取消排隊Ticket，不做IO／HP、不拿Hub／actor／admission。active事件保持原complete所有權；producer finally不提前清除排隊事件或重複扣計數。原master獎勵輸出失敗被記錄後繼續原死亡／波次／結束尾段。沒有新queue／worker／timer／TLS／static registry，queued物件沒有Context／Scope／Receipt／Frame／Runnable欄位。

Source實際processPendingEvents在dequeue後、commitInternal前釋放pending名額；本批已凍結此依據，保留的native claim／queued-only target計數時點相符，不需要改成HP完成才釋放。**尚須讓其他直接NPC HP入口加入同一FIFO，以及核對Source傷害專用容量／設定與既有AI工作計數的關係。**

本SHA的{count}原方法獨立inverse、52類JVM不初始化、33個純CAS／並行容量／發布失敗／真實未綁定queue與混合原drainer／清列案例、凍結同位元重建通過；50個Source方法已凍結，20 SQL class原位元，0新遊戲案／0DB連線。純queue案例沒有建立PC／Hub／socket／BATTLE或HP權限；不算實際HP／Main／游標驗收。

**下一步且尚未完成**：其餘自然NPC生產端／接受計畫／共用FIFO、Source容量domain、MP／狀態／aura／原master AI／抑制表reader／掃描節奏、全部58材料與完整外控／娃娃／NPC路徑、正式0.6.71隔離合成及同SHA生命週期矩陣。先程式更新、後集中中文MySQL真角色唯讀與實機游標；完整目標active，不部署。

"""+text[b:]
if capacity:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f"""## 本次已更新的程式

Source傷害專用容量已接到原application lifetime的一個final AtomicLong；它在原super建構後、其他owned helper可見this之前初始化。PC／NPC排隊傷害共用此ledger及原Source GLOBAL設定，七個原AI等非傷害credit writer與work pending維持原位元，Main繼續使用原工作計數。

PC原隊列先以父版CAS預留傷害名額；deque發布失敗釋放同一預留，發布成功另加一次原work credit。NPC沿用原target FIFO／lane／deadline／STOPPED最後交握，預留傷害ledger並於成功發布後另計work。兩種queue的dequeue與queued-drop精確原邊界，同步釋放對應傷害名額；原work釋放保持不變。Source在HP前dequeue釋放pending，本版沿用此時點與queued-only target限制，原lineage.damage.maxTargetPending／maxGlobalPending鍵、上限與clamp不變。

idle與terminated均新增傷害名額歸零的reject-only檢查，涵蓋預留與發布之間的短暫窗口；原Main drain／stop／worker／retry與AI生命週期本體保留。ledger拒絕錯誤重複／負數釋放；無新registry／queue／worker／timer／TLS／連線或持久化。前版混合計數offer保留為零呼叫前身別名，live入口已轉傷害專用helper。

本SHA的{count}原方法獨立inverse、53類JVM不初始化、16個純容量隔離／並行／清列／發布失敗案例，以及同SHA重跑的33個真實未綁定queue／drainer契約通過，凍結重建同位元。50個Source方法參考、20 SQL class原位元，0新遊戲／0DB連線；沒有建立life／PC／Hub／socket／BATTLE或實際HP權限，不算Main實跑資格化。

**下一步且尚未完成**：把其他直接NPC物理／娃娃／raw／毒傷／週期／治療／AI生產端接入相同FIFO與detached accepted plan，保留原RNG／成本／目標權限／一次proc及最終接收。排隊PC／NPC的容量domain已程式接線，真實owner容量／設定／關閉矩陣仍待集中驗收；MP／狀態／aura／master AI／表reader／掃描節奏、全部58材料與完整外控、正式0.6.71隔離合成及實機游標仍開放。完整目標active，先程式更新，不部署。

"""+text[b:]
if poisonnpc:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f"""## 本次已更新的程式

自然Quest／Time毒傷於原Lease安裝點固定Room／實際actor／owner／生命／generation及原派系。原tick／到期／機率／成本本體保留，扣HP改為非阻塞入同一個目標status-owned FIFO，與cube及原proc共用原worker／drainer／容量／dequeue／drop／Main。sealed Plan明確新增毒傷Ticket，原try/finally中分流並由原Queue.complete一次完成；無新queue／worker／timer／TLS／registry／SQL。

真正HP只接受精確原queue.active事件、queued／claimed及原場景／target生命。Source L1DamagePoison保留原_attacker，不要求其仍存活；本版HP不再以施毒者sourceLife／在線gate撤銷，已入列後毒狀態到期／替換也不撤銷。施毒者binding另供HPBAR／lawful，不讓新同ID角色繼承；接受後固定fatal生命，接收者／角色／場景／spawn於最後Frame再驗。到期palette另驗精確現行lease，與已入列HP權限分離。

五個有界HP／發布sibling保留原poison=true扣血／屍體／波次／spawn／邊界／AI-arm／獎勵／結束；不借用PC或cube當施攻者，無一般魔法數字或額外hurt action。既有WORLD／未載入Source後備保留。{count}原方法inverse、62類JVM不初始化、15毒傷混合原queue／drainer／drop＋同SHA33佇列與16容量純案例、凍結同位元重建通過；57個Source方法凍結，20 SQL class原位元；**0真實遊戲／HP／Main／DB案例**。

尚缺其他直接NPC生產端共用FIFO及接受計畫、完整Source仇恨／AI／型別免疫數值、PC毒傷跨角色生命週期、其餘58材料／外控／正式0.6.71隔離合成，以及後續集中同SHA後端／中文MySQL唯讀／實機游標矩陣。完整目標active；不部署、不寫玩家SQL／schema／WAL。

"""+text[b:]
if physicalnpc:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f"""## 本次已更新的程式

Source BATTLE自然Quest／Time物理命中及其同步Scope的娃娃SKILL／加傷，改入原actor status-owned FIFO，與cube／毒傷／原proc共用舊worker／drainer／queue及傷害ledger。原兩個物理呼叫端都直接POP回傳值，新入口不假造同步接受結果。原攻擊計算／機率／成本／critical／奇古獸路徑位元保留；actual active event及claimed Ticket重驗原來源／target生命／位置／場景／roster，原娃娃失效不拒絕普通HP。

獨立Origin／Commit保留原角色binding、Room／實際actor／owner／generation／生命、已擲AttackProc、原召喚Stamp及觀眾binding；HP票據無Context／Scope／Receipt／Runnable。五個原HP／發布有界sibling保留屍體／死亡／波次／spawn／邊界／AI-arm／獎勵／結束，再一次執行接受後娃娃、發布物理數字。接受後才以新同步Context固定原觀眾／binding及接受的原NPC；原Stamp／能力wrapper拒絕替換娃娃或背包授權。Context只在同步／既有輸出scope內，未存入HP票據。

娃娃子SKILL／加傷非阻塞加入共用尾端，無等待或inline HP；SKILL接受後數字、加傷HP前數字與物理proc後數字保留。fatal生命同步封存至獨立數字proof；子事件只保留原父數字Plan，接受時核對相同actor／owner／generation／前代life／pose才延伸父數字生命，拒絕替換怪物授權。結構輸出於HP／來源動作／admission鎖釋放後執行，娃娃輸出沿原owned publication，最後Frame重驗原接收者／場景／target／spawn；原OPEN根入口、DRAINING因果子事件、STOPPED／deadline／drop／Main限制保留，無新queue／worker／timer／TLS／registry／SQL。

本SHA的{count}方法inverse、76類JVM不初始化、30因果數字生命／混合四型別／巢狀原FIFO案例＋同SHA33佇列／16容量／15毒傷純案例與凍結同位元重建通過；58個Source參考方法、20 SQL class原位元。**0真實HP／遊戲／Main／DB案例**。一般魔法／raw／週期／受控與AI生產端、完整Source物理接受後吸MP／HP／耐久／天賦、NPC數值／仇恨AI／全部58材料外控／正式0.6.71隔離合成與集中驗收仍未完成。目標active，不部署，不寫玩家SQL／schema／WAL。

"""+text[b:]
if magicnpc:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f"""## 本次已更新的程式

兩個Source BATTLE範圍施法入口在原等待前建立固定Context快照，保留原caster binding／生命／位置、實際NPC／owner／生命／generation／位置，以及原觀眾／接收binding。子Context沿用快照，不在等待後刷新角色或NPC權限。原primary／Ctrl／範圍關係／型別／距離／成本／RNG與WORLD完整後備經inverse保留。

四個原Quest／Time範圍魔法HP呼叫端都丟棄回傳值，現在以mode2加入原actor FIFO，與物理／娃娃／cube／毒傷／原proc共用既有worker／drainer／容量／claim／drop／Main；不假造同步接受。原erase-map與target-entry的精確lease封成After存入既有Commit，沒有Context／Scope／Receipt／Frame／Runnable或整份Snapshot；同步Admission finally復原。原來源／target生命及最終receiver仍用原綁定，拒絕同ID替換角色／NPC授權。

接受後先邏輯發布魔法數字，再由原accepted callback依真正accepted actor／life／generation消耗精確魔消lease，舊事件不會刪除重施的新lease。原producer的即時consume只在Source BATTLE新範圍路徑抑制，非產品後備保持原樣。固定Source對正傷害等待接受，非正傷害則直接排副作用；新增分支沿原actor／admission／Hub／encounter與原模式／active檢查只消耗舊lease，不造HP／數字，也未聲稱補上NPC負傷害回復。無新queue／worker／timer／TLS／registry／SQL。

{count}方法inverse、81類JVM不初始化、16原身分／真實魔消狀態一次／重施／取消／清除／到期案例＋同SHA30物理／33佇列／16容量／15毒傷純契約與凍結同位元重建通過；64個Source方法凍結，20 SQL class原位元；**0真HP／遊戲／Main／DB案例**。下一步單體吸血／混亂／雷霆、方向／冰凍／暈眩接受計畫及其餘raw／週期／受控／AI／治癒生產端；完整58材料／外控／數值／AI／正式0.6.71隔離合成與集中驗收仍未完成，目標active、不部署、不寫玩家SQL／schema／WAL。

"""+text[b:]
if singlenpc:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text.replace('更新：2026-10-24。','更新：2026-10-25。')
 text=re.sub(r'2026-10-24使用者再次要求重設；[^\n]*', '2026-10-25使用者表示已刪除舊目標並要求重設；本次實際create_goal仍被系統以unfinished goal拒絕，get_goal仍為active。因此保留既有進度並重新確認工作範圍，未將未完成目標標為完成，也未宣稱系統重設成功。[本次目標工具收據]('+Path(v['goal_reset_request']['path']).as_posix()+')。',text)
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f"""## 本次已更新的程式

自然Quest／Time普通單體、吸血、混亂與雷霆NPC葉節點改入既有原target FIFO，沿用範圍版等待前來源／實際NPC／生命位置／generation與觀眾快照；原single body之前再驗原來源與已捕捉NPC。兩個handled-only前綴只在已支援Source BATTLE NPC分支略過原同步HP／尾段；WORLD／未載入Source與心靈破壞／骨頭／護甲破壞仍保留完整原路徑，因其HP前MP／狀態副作用尚未有真正接受計畫，不能只改HP或假造回傳成功。

Single只帶原spell、精確魔消lease、已算混亂／雷霆秒數、原傷害得出的回復量及一次claim flag，不帶Context／Scope／Receipt／Frame／calculator／snapshot。混亂沿用原roll，雷霆以原calculator與原level／boss／fixture／snapshot輸入在入列前算一次；接受後不重擲。固定Source的heal=this._dmg與thunder預先捕捉已凍結。同步carrier finally復原，所有HP仍由原queue.active／claimed與來源／target／Main guard核可。

三個原normal／catch／finally接受邊界都在HP鎖釋放後、native波次／結束發布前釋放一次副作用，故致死目標仍可賺得原吸血回復；拒絕事件不能回復或施加狀態。沿原source動作鎖／admission／owned publication先邏輯數字、消耗舊魔消lease、只對真正接受的原存活NPC施加固定狀態，最後回復。非正傷害採原直接副作用分支，不造HP接受結果。

原applyAcceptedLifeDrain的alive判斷／long加法與上限／原PC monitor透過單一受限static sibling保留，只替換本人HP與旁人ratio輸出。原觀眾／receiver binding及source生命帶到真正Frame，角色／場景／生命換代或封包HP／比例已過時則拒絕；輸出carrier finally復原，receiver IO沿既有owned publication。無新worker／queue／timer／TLS／registry／SQL。

{count}原方法inverse加1原回復sibling、84類JVM不初始化、14固定回復／已算輸入／一次claim與並行／魔消ABA案例＋同SHA110既有純契約與凍結同位元重建通過；66個Source方法、20 SQL class原位元；**0真HP／遊戲／Main／DB案例**。方向／冰凍／暈眩與心靈／骨頭／護甲破壞接受計畫、完整數值機率／型別免疫、其餘NPC生產端／AI／factory／全部58材料外控／正式0.6.71隔離合成與集中驗收仍未完成。目標active、不部署、不寫玩家SQL／schema／WAL。

"""+text[b:]
if targetnpc:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

自然NPC方向、單體／範圍冰凍與兩個保留的Shock BATTLE body，共10個handled-only HP／狀態尾段改入原target FIFO；5個原body前重驗，Shock在原wrapper／子Context／等待前固定快照，其餘沿共同原caster／實際NPC／owner／generation／生命位置／觀眾binding。原正常／Ctrl／型別／距離／成本／計算與PC／受控／WORLD本體經inverse保留，未聲稱全部數值或實機語意已完成。

原freeze chance／d100／boss／erase map轉固定輸入，不再用damage<snapshot.hp推估存活來授權狀態。正傷害在真正接受、HP鎖外且native波次結束前一次發布數字／精確舊魔消／存活原NPC狀態；非正傷害沿Source直接副作用分支，無假造HP接受。Source SHOCK_STUN在接受副作用內抽時間，故保留同階段一次抽取原immutable spell時間，不在入列前先抽、不重擲已完成機率；致死／屍體與完整數值機率仍待集中矩陣核對。

兩個實際Monster類別的18個原expiry寫點都在原monitor下先發布新lease再寫相同scalar值，4個expiry改volatile及4個有界Object lease欄位，啟用／取消／到期／reset即使同clock同expiry也拒絕舊特效。沿原Hub／owner狀態與effect placement；本人／觀眾均帶原receiver binding與source／NPC生命／狀態lease至真正Frame，替換／換角／狀態過時則拒絕。舊特效移除保留原場景及receiver作清理。無queued Context／Scope／Receipt／snapshot，無新queue／worker／timer／TLS／全域registry／SQL；IO沿原owned publication在HP與source monitor外執行。

{count}原方法inverse、90類JVM不初始化、18新固定接受／threshold與實際未綁定actor狀態取消／到期／同值重施ABA案例＋同SHA124既有純契約、凍結同位元重建通過；68個Source方法、20 SQL class原位元。**0綁定HP／遊戲／Main／DB案例**。心靈／骨頭／護甲破壞HP前MP與狀態接受計畫、其餘raw／魔杖／反射／週期／治癒／受控／AI-factory生產端、完整數值型別免疫、全部58材料外控、正式0.6.71隔離合成與集中驗收仍未完成；目標active、不部署、不寫玩家SQL／schema／WAL。

'''+text[b:]
if specialnpc:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

以實際Source四個新參考方法修正上一批假設：MIND_BREAK／BONE_BREAK／ARM_BREAKER的skillmode.start本身先變更MP／狀態，再回傳傷害；L1SkillUse在HP enqueue前呼叫start。心靈破壞須currentMp>requested且實際扣足才計算傷害；骨頭破壞僅未生效時擲骰、86123兩秒；護甲破壞沿原DMGUP2不刷新。因此保留Source原前置階段，未改成接受後才做、未假造MP或HP成功，也不替Source增設後續HP失敗的回滾。

原生Mind／Bone兩個完整本體保留，僅自然NPC的Source BATTLE由typed wrapper進入原來源動作／admission／Hub／實際owner鎖；owned publication scope在取得Hub／owner鎖前建立。等待前原source／NPC／generation／生命位置重驗後才呼叫原native body、回傳真正native result。兩個Arm葉端同樣守原身分並回傳真正Activation。PC／受控／WORLD完整原路徑保留。

既有Context新增一個有界volatile carrier，只帶detached Origin／phase kind／真正immutable result／executed flag，每個原single invocation重設。真正執行的前置階段才開啟既有Single FIFO；stale／未執行前置被handled drop，不能退回同步舊NPC HP。原生accepted／Main／精確魔消／數字沿共同尾段；本批不新增queue／worker／timer／TLS／全域registry／SQL，未讓新事件metadata帶Context／Scope／Receipt／Aim／calculator。

Bone兩個原send端改以原receiver binding與Source／實際NPC生命／狀態lease送至真正Frame；8個原expiry寫點先發布新lease再寫相同scalar，2個expiry volatile及2個有界原actor lease欄位覆蓋重施／取消／到期／reset。原啟用、不刷新、86123 placement與RNG／成本／計算經inverse保留，IO沿原owned publication在Hub／NPC monitor外完成。

{count}原方法inverse、100類JVM不初始化、24個實際未綁定NPC MP嚴格門檻／native receipt／Bone-Arm狀態／拒絕proof案例＋同SHA142既有純契約及凍結同位元重建通過；72個Source方法、20 SQL class原位元；**0綁定HP／遊戲／Main／DB案例**。其餘raw／魔杖／反射／ground／週期／治癒／受控／AI-factory生產端、完整數值機率型別免疫、全部58材料外控、正式0.6.71隔離合成與集中驗收仍未完成；目標active、不部署、不寫玩家SQL／schema／WAL。

'''+text[b:]
if poisonpc:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

Source L1DamagePoison毒傷tick不要求原施毒者仍存活／在線。原生有凍結時鐘的DAMAGE路徑已由目標持有clock／角色檢查，並以null caster及raw false／true送入HP；但外層NativeSourceRawDamage.Action仍把supplied施毒者當成pair peer，可能在原clock本體執行前就因施毒者退役丟棄有效tick。

本批只改原Action建構前的一個peer參數：Source BATTLE、真正Scope.find(target)、clockActive及原frozen DAMAGE kind都成立才不選施毒者為peer。原Action改以自己作peer，原target身分／生命／場景、tickAllowed／contextValid、clock、raw旗標、queue／admission／Main、狀態到期清除與原歸屬本體經inverse保留；沒有跳過目標檢查或重新選新角色作施毒者。

新增helper無欄位，只讀既有context；無新Context／plan／status欄位、queue／worker／timer／TLS／registry／SQL或新HP／輸出端。未載入Source／WORLD、無凍結clock／inactive／其他kind及無Scope維持原peer；nested raw Action與完整PC毒傷歸屬仍待稽核。

{count}個原raw方法inverse、其他原class全位元保留、105類JVM不初始化、同SHA166既有純契約及凍結同位元重建通過；72個Source方法、20 SQL class原位元。**本批新PC毒傷實際或純案例0、綁定HP／遊戲／Main／DB案例0**。此為scoped peer程式修正，未聲稱全部PC毒傷或真實施毒者退役矩陣已驗收。其餘raw／魔杖／反射／ground／週期／治癒／受控／AI-factory、完整數值機率與生命週期、全部58材料外控、正式0.6.71隔離合成與集中驗收仍未完成；目標active、不部署、不寫玩家SQL／schema／WAL。

'''+text[b:]
if rawnpc:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

魔杖／Mortal Body／Joy of Pain的6個自然NPC raw Quest／Time端，原caller皆POP回傳值。Source BATTLE有原raw Action的路徑改以mode0加入同一原actor FIFO／舊worker與claim／容量／admission／Main；return0不假造同步接受。非產品／無Action維持完整原monster body，原caller RNG、成本、排除型別與HP前特效不變；mode0依Source raw規則無普通魔法數字，也不觸發娃娃proc。

Source LegacyDefenseFeedbackPlan在incoming HP接受後才回饋，檢查被反擊的NPC仍存活，沒有要求defender存活。原七component Origin新增1個final Object，只帶原raw Member的Actor／真正accepted life／position／dead規則；typed source policy保留原角色／生命／位置／dead與BATTLE場景，不以普通攻擊HP>0阻擋既有致死反饋。未標記Origin完整沿原普通source policy。只有暫存Admission與相同source Actor／actual NPC／owner／life／generation吻合才附證明，無queued Action／Context／Scope／Receipt／snapshot，不重新取得角色權限。

原等待前觀眾／binding沿共同Snapshot與Commit，carrier finally復原；葉端重驗原root frame／目標，admission沿既有causal scope，不額外加OPEN-only葉端門檻。HPBAR／死亡／波次／獎勵／結束沿共同接受發布；存活HPBAR在producer及最終Frame仍要求來源存活，死亡hide保留獨立清除。無新queue／worker／timer／TLS／registry／SQL。

{count}原方法inverse及6個原POP端、108類JVM不初始化、12個實際Origin／record schema／source-target-owner-life-generation證明案例＋同SHA166既有純契約與凍結同位元重建通過；74個Source方法、20 SQL class原位元。**0綁定raw HP／致死defender／遊戲／Main／DB案例**。未綁定12案僅驗metadata，不代替真正反射／魔杖生命週期矩陣。其餘unscoped／受控raw、PC毒傷其餘路徑、ground／週期／治癒／AI-factory、完整數值機率／hit-reaction suppression／全部58材料外控／正式0.6.71隔離合成與集中驗收仍未完成；目標active、不部署、不寫玩家SQL／schema／WAL。

'''+text[b:]
if refrawnpc:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

追查真正生產鏈：NativePlayerCommit.acceptedTail→NativeCombatAttacker.acceptedFeedback→原已驗證Ref本體→反射Quest／Time HP呼叫。上一批六個legacy／static POP端不能證明這條真正接受回呼已接線；本批補這一個共用兩種自然NPC的深層反射端。

原正傷害／validFrame／live Ref檢查後，將原Ref與已驗證Live local傳給無狀態bridge；Source BATTLE且既有raw Action吻合時，再驗真正accepted非WORLD Frame、原Quest／Time method／descriptor／id／operand型別／正傷害、Ref room／owner及同一實際NPC actor，然後進既有raw mode0 FIFO。stale scoped輸入handled drop，不造接受；unsupported／unscoped完整原fallback保留。原已算機率／結果、受傷動作抑制、reflecting TLS finally、Ref live驗證與受控／WORLD本體經inverse保留；無新欄位、queued Ref／Live／Action／Context、queue／worker／timer／TLS／registry／SQL／輸出端。

原raw Origin source proof／觀眾快照／目標與角色生命、HPBAR、claim／容量／admission／Main及接受發布共用。實際檢視sourcePlayerBattleCommit會設deathConfirmationPending並送死亡確認，沒有直接endBattle／serverEndBattle；未為尚未證實的疑慮新增child wait或結束協調器。真正死亡確認／離場／場景與Main競態仍須集中矩陣，沒有宣稱已完成。

{count}個原Ref dispatcher方法inverse、109類JVM不初始化、12個真實native Ref之Quest-Time ABI／kind／id／operand案例＋同SHA178既有純契約與凍結同位元重建通過；74個Source方法、20 SQL class原位元。**0綁定accepted feedback／致死defender／遊戲／Main／DB案例**。其餘unscoped／受控Ref/raw、PC毒傷其餘路徑、ground／週期／治癒／AI-factory、完整數值機率型別免疫／外控／全部58材料／正式0.6.71隔離合成與集中驗收仍未完成；目標active、不部署、不寫玩家SQL／schema／WAL。

'''+text[b:]
if hitreaction:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

已確認Source受傷動作抑制讀取系統_受擊無動作gfx.gfx_id，候選原為寫死清單。本批在既有readonly／autocommit=false啟動交易、娃娃metadata讀取之後新增一條SELECT；不另開Connection、不擁有交易、不在命中時查SQL。不可變Set原子發布；空表清除固定值，重複列合併，NULL依getInt為0、零與負數仍按Source集合判斷；SQL例外依Source保留空或已讀部分列並繼續啟動。未載入Source前保留原catalog本體作後備。

八個既有直接NPC反應入口（含真正接受Ref反饋與cube）原class位元保留，只將共用catalog接到新清單。PC／HP／成對鎖／actor生命／場景／最終Frame／佇列／admission／Main本體均未變；未更改客戶端target-id／傷害數字協定。其餘NPC ACTION_Damage、一般攻擊target與RO登入同步、多resource database路由仍須逐端稽核；不能宣稱所有抑制消費端完成。

{count}原startup／catalog方法inverse、112類JVM不初始化、14個唯讀JDBC代理／集合／失敗契約＋同SHA190既有純契約及凍結同位元重建通過；80個Source方法凍結、20 SQL class原位元。**0實際MySQL／遊戲／綁定HP／Main案例**。中文欄位真實唯讀重載與集中實機驗收依使用者指示延後；完整Source／58材料／生命週期／正式0.6.71隔離合成仍開放。不部署、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
 text=text.replace('受傷動作抑制消費端仍待稽核與接線','受傷動作抑制已補啟動清單，其餘封包／同步消費端仍待稽核')
if npcreaction:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

Source的NPC反擊屏障與鏡反射各自過濾ACTION_Damage，但候選兩個共用發布器原直接送出。本批僅在publishNpcCounterBarrierTrigger與publishNpcCounterMirrorTrigger的第二個encrypted send前加guard；保留防禦者特效、反射HP、原機率／成本／狀態消耗、觀眾／畫面、原sender及發布順序。

已載入Source的BATTLE沿原ingress Ref。受控／Quest／Time身分不符id即略過反應；吻合時用原private live驗證，取得同一實際snapshot的typed gfx，套用前批唯讀啟動清單。失效Ref／Live不送受傷動作，不依數字ID重新捕捉權限。WORLD／未載入／unscoped／unsupported保留完整後備；無新欄位／queued Ref-Live-Context／worker／queue／TLS／registry／SQL。原驗證鎖在encrypted IO前釋放。

{count}原方法inverse、113類JVM不初始化、16個真實native Ref／typed snapshot契約＋同SHA204既有純契約與凍結重建通過；82個Source方法、20 SQL class原位元。**0綁定反射／HP／最終接收端／Main／遊戲／MySQL案例**。完整Source反射數值／RNG／HP-特效時點、其餘受傷封包、RO target-sync、多resource路由及其餘58材料／生命週期／正式隔離合成仍未完成。集中實機驗收依指示延後，不部署、不寫玩家SQL／schema／WAL。

'''+text[b:]
if wandreaction:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

Source閃電魔杖已算raw HP送入NPC／PC佇列，之後送魔杖與目標特效；僅Monster受傷動作套gfx抑制，扣次數在doWandAction之後。本批補原wandHurt共用封包端：已載入Source的BATTLE、action2與原raw Action保存的自然Quest／Time目標才判斷抑制。

沿原Action.frame與原id／kind／實際private actor class，讀該actor原snapshot的typed gfx，套前批唯讀清單。不額外加入正HP、OPEN-only或扣次後item gate，不重新捕捉目標權限。被抑制／失效輸入回傳空byte，由既有captureOutput零封包處理在加密前略過；原延後送包及allowFrame沿原接收binding／來源與目標生命／場景／Main。PC／受控／unsupported／WORLD／未載入／unscoped及非受傷action保留原後備。HP／扣次／原機率／item／特效／queue／worker／欄位／SQL未改。

{count}原raw方法inverse、114類JVM不初始化、16個實際未綁定NPC metadata／封包ABI契約＋同SHA220既有純契約與凍結重建通過；84個Source方法、20 SQL class原位元。**0綁定魔杖HP／扣次／致死目標／接收端／Main／遊戲／MySQL案例**。完整魔杖數值／型別免疫／成本與HP-封包時序、其餘週期／治癒／AI生產端、58材料／外控／正式隔離合成仍未完成。集中實機驗收依指示延後，不部署、不寫玩家SQL／schema／WAL。

'''+text[b:]
if groundorigin:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

地面效果原只帶角色編號，PC火牢仍走一般HP入口。本批在原PC58施放／原lease工廠保存場景、engine epoch及原施放者Binding；原pulse與GroundTick以final非component欄位攜帶同一lease的證明。原synchronized advance在時鐘與成對等待前固定實際來源／目標角色與生命；舊建構子及record schema保留，複製編號不會取得原證明。

實際PC時鐘端驗原場景／lease membership，進既有raw ordered pair action，等待與admission後在原RNG前重驗原角色。僅PC火牢HP端改接既有raw false／true、空數字plan及共用FIFO；原計算、freeze／cooking／earth gate、反應次序、競技關係、geometry／timer／容量本體inverse保留。NPC／BOT／非58／WORLD／未載入／unscoped保留原後備。

原reaction capture／defer／final Frame加場景證明。單一Context carrier保留至原輸出完成，由原Action.close接點清除，涵蓋未註冊失敗；新queued record不持有Context／Action／Frame。無新clock／queue／worker／TLS／registry／SQL。

{count}原方法inverse、125類JVM不初始化、16個實際未綁定lease／pulse／tick epoch-schema契約＋同SHA236既有純契約與凍結重建通過；91個Source方法、20 SQL class原位元。**0綁定PC HP／角色／成對鎖／接收端／Main／遊戲／MySQL案例**。下一步優先補**已排入HP事件的獨立ground場景／lease證明**；目前只沿既有raw member guard，不宣稱已完成該排隊邊界。自然／受控NPC火牢、BOT／Time-NPC適用性、原effect NPC顯示、Source間隔／placement／數值／SKM0／SKD3／型別免疫與主人死亡語意、58材料與正式隔離合成仍開放；集中實機驗收延後，不部署、不寫玩家SQL／schema／WAL。

'''+text[b:]
if groundqueued:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

已將同一原ground PulseProof以單一volatile Object放入既有raw HP Plan，於原plan建立後、發布前綁定。排隊事件不保存產生它的Context／Action，也無新TLS／registry／queue／worker／SQL；原容量、dequeue信用與release owner保留。

實際drain沿原resolved caster local，在防禦機率前先核對Source專用scene／角色／binding／life。失效事件走原empty-defense支線，並以一個primitive local保留拒絕事實，避免狀態後來改變時跳過防禦RNG卻又取得HP權限。在原實際HP guard再次驗同一caster、false／true旗標、原event／raw member的角色生命與原lease Entity指標／scene epoch／Main；unmarked保留完整原guard與計算。

接受後final Frame沿原acceptedState在HP鎖內更新的精確acceptedLife，保留該致死token的合法輸出，之後死復活／再死亡／換角／場景退役不會借用舊證明。個別lease自然到期不撤回已准入HP；沒有新expiry／lease-list／timestamp／OPEN-only門檻。原DRAINING／STOPPED及Main流程保留，場景clear／停止仍拒絕舊事件。

{count}原方法inverse、126類JVM不初始化、18個實際未綁定lease identity／scene／個別expiry／actor tuple契約＋同SHA252既有純契約及凍結重建通過；91 Source方法、20 SQL class原位元。**0實際綁定HP／致死接受／角色／pair／receiver／Main／遊戲／MySQL案例**。queued scene的程式接線已補，完整場景ownership線性化與首等待前原施放者工廠證據、自然／受控NPC火牢與BOT／Time-NPC適用性、原effect NPC顯示、Source數值／間隔／placement／SKM0／SKD3／型別免疫／主人死亡政策及全58材料／正式隔離合成仍開放；集中實機驗收延後，不部署、不寫玩家SQL／schema／WAL。

'''+text[b:]
if groundplacement:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

**修正生產路徑覆蓋判斷**：前批ground-origin只接到舊GameHub.placeGroundSpell直接工廠；材料實際走placeBattle→GameHub.nativeMaterialPlaceGroundSpell→NativeGroundMaterialAction.castBattle。後者先建立副本、提交成本、再建立第二副本與安裝，新lease原未取得前批Source證明；前批metadata檢查不能代表這條實際材料路徑已接線。

本批peek原NativeLightAction.current Batch，沿用reserve在原player-lock首次等待前保存的accepted Ingress.binding，不重新捕捉目前施放者。於原Hub／action鎖下驗同caster id／binding／room與live engine。原preflight copy、成本callback與Rejected支線、第二次copy／重算、install順序完整保留。

detached Draft把原scene／epoch／creator帶進兩份暫存副本；新lease Entity指向原live engine。Source scene僅在成本接受後的原install點發布，拒絕／未安裝preflight不發布live scene或leases；finally復原原installing slot。Draft／Entity不保存Batch／Ingress／Context／Frame；無新欄位、scope／TLS／registry／queue／worker／SQL。非58／WORLD／未載入／unscoped保留原後備。

{count}原方法inverse、131類JVM不初始化、18個實際未綁定copy／install／live-lease契約＋同SHA270既有純契約與凍結重建通過；91 Source方法、20 SQL class原位元。**0綁定角色／MP／材料／rollback／HP／Main／遊戲／MySQL案例**。實際材料成本／拒絕與關閉競態、完整場景／HP資格化、NPC火牢與BOT／Time-NPC適用性、Source數值／間隔／geometry／型別免疫／主人死亡及effect顯示、其餘58材料與正式隔離合成仍開放；集中實機驗收延後，不部署、不寫玩家SQL／schema／WAL。

'''+text[b:]
if groundnpc:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

自然Quest／Time NPC已接火牢原advance→pulse→GroundTick，於原null-PC捨棄分支前排程／派送。原Hub／encounter下封存實際actor／owner／life／generation／位置與觀眾，原Entity建構時封存競技lane；不將BOT／受控NPC當成自然怪物。原1650ms catalog／lease計時、半徑及PC／BOT後備保留。

施放者動作鎖等待後、亂數前，再驗原來源binding／life／scene、目標身分／位置／lane及freeze／earth-bind。使用原唯讀NPC stats與有效INT，依Source只計正抗火，包含現有屬性弱化lease；負抗火不增傷。原gfx外控與receiver證明於HP前送ACTION_Damage，接原有界NPC FIFO mode0；無一般魔法數字或娃娃proc，無新queue／worker／timer／TLS／SQL。

已准入raw傷害隨原actor移動，不因個別effect到期撤回；原Origin新增final detached proof，scene／epoch／來源生命／Main守衛延伸到HP及最終輸出。使用原接受後NPC生命token，原DRAINING／STOPPED／容量與釋放機制保留。堆疊Context只供建立證明與原觀眾快照讀取，未放入排隊事件；反應IO不持有來源／Hub／encounter鎖。

{count}原方法inverse、135類JVM不初始化、26個Source算式／lane／schema契約＋同SHA288既有純契約及凍結同位元重建通過；93 Source方法／25 Source檔案、20 SQL class原位元。**0綁定HP／角色／主人死亡／接收端／Main／遊戲／MySQL案例**。完整SKM0／型別適用性、PC SKD3與火牢數值、受控／BOT／Time-NPC來源、effect顯示、場景與成本關閉矩陣、其餘材料／外控／NPC AI-factory與正式隔離合成仍開放；集中實機驗收延後，不部署、不寫玩家SQL／schema／WAL。

'''+text[b:]
if groundplayer:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

玩家火牢在原成對raw action內，於亂數前重驗原lease／Entity／場景／角色生命及RUNNING競技canAttack；freeze／絕對屏障／大地屏障提前判斷。改用有效INT與有效抗火，依Source只計正抗火；料理status ID、龍之護鎧／耐力／聖結界現有lease選取真正SKD3固定鍵，逐鍵減傷後才整數除2，取代這條路徑原本的基礎INT與一般護甲減傷。

保留Source receiveDamageInternal仍會執行的reductionOnly防禦proc與化身／武器修正，不誤改成directRaw跳過。原false／true傷害計畫、FIFO、接受／致死生命與Main限制保留；反應使用原觀眾與deferred output，最終ground Frame再驗native screen範圍，IO不增加角色／Hub鎖。非58／未載入／未綁定／WORLD原本體保留，無新欄位／scope／TLS／queue／worker／SQL。

{count}方法inverse、136類JVM不初始化、35個Source算式／真料理及免疫狀態／封包／Source oracle契約＋同SHA314既有純契約與凍結重建通過。Oracle比對固定Source完整31列SKD3及0..5000其餘ID；93 Source方法／26檔、20 SQL class原位元。**0綁定pair／HP／防禦RNG／致死／接收端／競技／Main／遊戲／MySQL案例**。

完整有效數值、Source血盟／安全區對競技場的對應、放置與effect呈現、NPC型別及SKM0、受控／BOT／Time-NPC生產端、其餘58材料外控／NPC AI-factory／正式隔離合成與集中驗收仍開放；目標active，不部署、不寫玩家SQL／schema／WAL。

'''+text[b:]
if cubeplacement:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

**更正實際生產路徑覆蓋**：先前cube-origin接舊GameHub.placeCubeIgnition／Harmony→engine.place；材料實際走NativeCubeCastAction→nativeMaterialPlaceCube→engine.nativeMaterialPlace，原Cube constructor未接Source Entity／faction，原Source HP端會捨棄這些未綁定物件。舊metadata契約不能當作真正材料工廠通過。

本批在兩個真正Hub材料工廠沿原Batch.accepted Ingress.binding（首次player-lock等待前封存），核對原caster／Hub／room／live engine與action所有權；detached Draft不保存Batch／Ingress／Context。兩個原Cube constructor改走既有metadata overload，Entity／faction指向原cube／engine與scene；原geometry／admit／成本／object ID／list順序保留。只有原accepted return發布live scene，rejected不發布；finally復原installing slot。

Source效果NPC掃描500ms；L1Cube獨立狀態仍延遲900ms、每1000ms回呼。只替原Source scene的scan interval local取500，保留燃燒第4次扣10及和諧第4次回MP／第5次扣25、原expiry與WORLD／未載入後備。傷害延用原實際效果NPC、target lease／FIFO／accepted-life／final output；無新fields／queue／timer／worker／TLS／SQL。

{count}方法inverse、138類JVM不初始化、20個實際未綁定cube／epoch／overlap／status-clock與draft／cadence契約＋同SHA349既有純契約及凍結重建；101 Source方法／29檔、20 SQL class原位元。**0綁定角色／MP／材料／rollback／HP／Main／遊戲／MySQL案例**。地裂／衝擊工廠與狀態生命週期、初始全域掃描相位、Source關係／型別／顯示／MP端、其餘材料外控及正式隔離合成集中驗收仍開放；不部署、不寫玩家SQL／schema／WAL。

'''+text[b:]
if cubestatus:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

地裂／衝擊真正材料工廠接原prewait Batch Ingress binding、實際效果NPC80150／80151與faction，沿原geometry／admit／成本／constructor／list順序；accepted才發布scene、finally復原安裝slot，WORLD與未綁定後備保留。

原primary status與bind／MR secondary cell攜帶原目標binding／life／generation與精確來源租約，MR續期時更新來源link。每次原advance發布有界immutable volatile身分索引，數值讀取與final Frame驗原cell／epoch／角色生命；舊到期／recast不能借新cell，清除時換epoch並清空索引。最終輸出不遍歷mutable map或新增角色／Hub鎖。

Source地裂自然NPC回呼接原actor.activateFreeze一秒，保留SKM0前置與競技lane。固定Source衝擊只續四秒標記而未扣MR；Source數值分支依此保留0修正，未綁定／WORLD／BOT原數值後備保留。Source scan500ms與原900／1000 status節拍不混用。原typed tick派送使用實際target、原觀眾bindings與raw self／deferred output／close；新Context carrier只在原同步scope存活，Link／Emission不保存Context／Batch／Frame，無新HP queue／worker／timer／TLS／SQL。

{count}方法inverse、151類JVM不初始化、24個實際未綁定factory／status／secondary／index／clear及target-tuple ABA契約＋同SHA369既有純契約與凍結重建；106 Source方法／32檔、20 SQL class原位元。**0綁定角色／材料rollback／HP／NPC凍結／final IO／Main／遊戲／MySQL案例**。

燃燒／和諧其餘數值／狀態／MP端、全cube設定GFX與型別／關係／初始掃描相位、完整跨角色／recast／接收端／Main矩陣、其餘58材料外控／NPC AI-factory及正式隔離合成集中驗收仍開放；不部署、不寫玩家SQL／schema／WAL。

'''+text[b:]
if cuberemainder:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

燃燒／和諧原advance發布有界immutable volatile cube／status身分索引，原nonHP事件攜實際租約、target role／life／generation與原觀眾binding；原HP來源及佇列保留。屬性與狀態讀取拒絕過期角色／租約，未綁定／WORLD／BOT後備保留。

和諧PC回魔沿原raw ownership與Hub場景鎖呼叫原MP方法，在原playerMp store記錄接受結果；final Frame驗原角色生命、原接收者及實際MP封包一致。接受後租約到期不撤銷已提交回魔通知，較新MP值會拒絕舊封包。自然NPC使用租約封存的原actor／life／generation／faction，沿原admission→Hub→encounter呼叫原recoverMana同體sibling，不借施放者或新ID查找。Source分支用long有界加法，原後備overflow政策保留。

原typed狀態、raw self／deferred output／close承接租約／epoch／場景／Main／target／receiver／range；clear清索引，無新HP queue／worker／timer／TLS／SQL或queued Context。原900／1000 status與500 scan分開保留。

{count}原方法inverse及兩個NPC MP同體sibling、154類不初始化、25新＋同SHA393純契約與凍結位元一致重建；108 Source方法／32檔、20 SQL class原位元。**0綁定角色／真實PC或NPC MP／final IO／Main／遊戲／MySQL案例**。全cube顯示／GFX設定／型別／關係／初始掃描、其餘58材料與完整Source／NPC AI-factory及正式隔離合成集中驗收仍開放；不部署、不寫SQL／schema／WAL。

'''+text[b:]
if cubepresentation:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

全四種立方體的八個PC／自然NPC原primary狀態特效口，改讀既有唯讀SkillsTable immutable metadata的castgfx／castgfx2；技能205／210／215／220，Source自然怪走敵方特效。和諧原advance在actual status constructor後、map發布前封存原已計算relation；不重查現行角色或借新ID／recast改寫舊狀態。

Source友方屬性同步沿typed bridge呼叫原private status body，先於target effect；只有同一Source applied-ally scope略過原尾端重複status。到期及未綁定／WORLD／BOT後備保留。設定GFX0沿原object／word封包傳遞，不代回固定預設；正值保留原資源remapping與wire／objid檢查。

原raw ownership／deferred output／final Frame／close繼續驗原role-life／場景epoch／精確index／receiver／range／Main。地裂／衝擊原installed predicate再拒絕等待後已到期的綁定primary／secondary cell。一個原Harmony status volatile Object及一個無狀態helper；無新queue／worker／timer／TLS／SQL或queued Context。

{count}方法inverse、157類不初始化、27新＋同SHA418純契約及凍結位元一致重建；109 Source方法／33檔、20 SQL class原位元。**0綁定角色／狀態IO／cost／HP／MP／Main／遊戲／MySQL案例**。實際效果NPC顯示與資源選擇、完整目標／競技關係／clan-party-safety／初始掃描相位、其餘58材料與Source／NPC AI-factory及正式隔離合成集中驗收仍開放；不部署、不寫SQL／schema／WAL。

'''+text[b:]
if cuberelation:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

四種立方體原advance的自身比較及typed Alliance口，改用原effect.creator Binding與原prewait target Scan。獨立位元碼確認Binding.source已是sourceCharacterIdentity，account為sourceAccountIdentity、selected為selectedCharacterName；直接沿用原完整Source角色身分查原角色目前血盟／隊伍，不重新組合、不以caster物件ID查目前session。

Source自身要求原PC與完整source／account／selected／owner／phase／scene Binding，並驗原目標生命；相同ID新角色不承接self。新套用狀態沿原Hub／engine ownership驗epoch／Main／OPEN／RUNNING及原target proof，不新增鎖或欄位。Source自身／血盟／隊伍友方條件保留；新敵方PC套用遵守原競技PvP／team規則、effect原封存force與實際target force，不借環境Ctrl。自然NPC保留原kind／life／generation／faction lane；未綁定／未載入／WORLD／BOT保留typed native後備。

變更僅在新primary選擇；原安裝租約／到期／續期、已admit PC／NPC HP／FIFO／accepted life、原成本／RNG／節拍／封包方法保留，不因creator退場撤銷已接受HP。無新Context／queue／timer／worker／TLS／SQL或owner metadata。

{count}方法inverse、161類不初始化、33新＋同SHA445純契約及凍結位元一致重建；109 Source方法／33檔、20 SQL class原位元。**0綁定creator／target換角／clan／scene／admission／HP／cost／final IO／Main／game／MySQL案例**。全域初始掃描相位、實際effect-NPC顯示／資源／可見、其餘58材料／完整Source／NPC AI-factory及正式隔離合成集中驗收仍開放；不部署、不寫SQL／schema／WAL。

'''+text[b:]
if cubeclock:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

**更正先前500ms生產路徑判斷**：原證據只追到EffectCube*Timer.start葉方法；固定Source真正由GameServer呼叫StartTimer_Skill.start，直接safeSchedule四個Runnable為initial0／period1000，並未呼叫那些start500。Source檔案保留原位元；舊500-as-production旗標改為已取代，既有純契約一個cadence預期值依實際入口更正。

原Hub四個有界per-type Phase只存immutable primitive next／scan：原ground task註冊前初始化、每個原同步type collector在任何room迴圈前推進一次、原close釋放。空場／房間clear與不同放置時間不重設全域相位；同一slot供所有房間，延遲回呼只掃目前live effects一次並保留fixed-rate原點。

四個原new-effect deadline比較讀同一Hub／type／tick slot；未綁定／未載入／WORLD原per-lease比較保留。Source cadence修正1000；沿用原100ms owned drainer，觀測仍受其排程解析度影響。無新scheduled task／worker／timer／queue／TLS／lock，Phase不保存Hub／player／actor／Context／Batch／Frame。

只改全域new-effect掃描。原primary到期與獨立900／1000 status回呼、第四MP／傷害與第五HP、原來源／角色／場景／FIFO／accepted life／final receiver、成本／RNG／SQL保留。{count}方法inverse、163 no-init、33新＋同SHA478既有純契約（477原預期＋1更正），凍結位元一致重建；111 Source方法／35檔、20 SQL class原位元。**0實際綁定Hub／startup／多房間／角色／HP／cost／IO／Main／game／MySQL案例**。

實際effect-NPC顯示／資源／可見、剩餘Source／58材料／NPC AI-factory及正式隔離合成集中驗收仍開放；不部署、不寫SQL／schema／WAL。

'''+text[b:]
if cubevisibility:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

四個真正材料放置及四個bootstrap／portal重建物件送包口，帶原effect Entity、constructor時原觀眾binding，或首次Hub等待前的重建接收binding。accepted publish沿原Hub→engine同步既有immutable cube index；原native編碼器及接收者資源rewrite保留，尚非完整Source NPC template數值資格。

Work只存detached effect／receiver／binding／packet與一次claim，實作原Output並使用原Batch.outputs；原loop獨立處理Work，原正常／stale／failure completion尾端救回尚未claim的效果輸出，施放者退場不借新角色，其他root／target／completion規則保留。每接收者IO／runtime失敗隔離，原area Context同步隔離／finally還原，不保存Context／Batch／Action／Frame／Receipt。編譯專用InnerClasses可見性stub未進候選，原Outer／Output class位元保留，實際private Output ABI通過JVM載入。

原移動完成接perceive：進入native screen只顯示未看見的原角色、離開時移除已看見物件；明確畫面重建可重送有效cube。final原加密前驗實際instance／index／epoch／scene／Main／到期、原接收source-account-role與封包object／geometry；**只有實際Protocol.writeFrame成功後**才記錄／移除known binding。到期移除限真正收到該cube的原角色；新角色／未看見／同ID替換不借舊remove。初始觀眾於expiry釋放、seen舊binding修剪，cache僅隨原效果／status／output有界生命存活。

{count}方法inverse、169 no-init、39新＋同SHA511純契約、凍結位元一致重建；113 Source方法／36檔、20 SQL class原位元。新增四個cube cache參照，沒有新Context／Batch欄位／queue／worker／timer／TLS／socket owner；原HP／cost／RNG、global0／1000與status900／1000保留。**0綁定queue／rescue／換角／移動／重建／IO／Main／game／MySQL案例**。完整Source NPC template／GFX與visibility外控對應、其餘58材料／Source／AI及正式隔離合成集中驗收仍開放；不部署、不寫SQL／schema／WAL。

'''+text[b:]
if cubetemplate:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

四種立方體在原pre-cost Draft凍結既有靜態NPC snapshot的純值；缺列／失敗／錯誤L1Effect型別／非法random bound在原材料本體前拒絕。沒有新SQL或借用character來源；不保存mutable template／整個snapshot。真正四個效果物件constructor各自持有一次生成的GFX／動作／經驗值／tempLawful／名稱，隨機順序照Source，固定模板不抽籤；獨立均勻呈現抽籤不改原戰鬥成本或機率RNG。

四個原Protocol encoder依實際effect frozen值編碼，unbound／WORLD保留完整原fallback；原native client tail與接收資源rewrite保留。Source初始heading／move／chaLight為0，title為null；**模板lightSize不是封包chaLight**。原角色、場景、known-role寫出後配對、IO、Main、HP與0／1000、900／1000節拍保留。

{count}方法inverse、175 no-init、371新＋同SHA550純契約與凍結位元一致重建；120 Source方法／38檔、20 SQL class原位元。新增Draft／四cube共5個final Object純值欄位；沒有新actor／Context／Batch／queue／worker／timer／TLS／SQL owner。**0綁定factory／成本rollback／換角／重載／IO／Main／game／MySQL案例**。自訂template work／spawn hook、完整效果屬性與visibility／resource外控、其餘58材料／Source／AI及正式隔離合成集中驗收仍開放；不部署、不寫SQL／schema／WAL。

'''+text[b:]
if fatalitydamage:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

Fatality原single／area入口在首次Context Hub等待前固定trigger binding／life／pose，目標與接收者沿原快照。兩個原die端改在trigger-target ordered pair、既有admission及目標commitSelf中重驗後抽一次並提交；舊post-roll sink只處理一次。原catalog選擇／die bounds-order／native area predicate、成本、Ctrl與WORLD保留；既有provisional分類權重仍未宣稱Source parity。

queued raw HP只帶原target Member與detached NONE Proof，無Context／Root／Batch／Frame／Receipt；入列後HP不依賴trigger仍活著。原fatal接受更新同一Member生命token。只有真正標記且原target／Guard／空PC／空Ref全吻合才略過Source attacker!=null內的防禦／受擊／連續魔法Calc；未知來源、已退場PC binding、NPC與效果Ref不誤當NONE。外層145／149與Avatar放大保留，raw事件不新增普通魔法數字。

原效果送包帶detached root／subject／receiver與生命至final加密前，允許既有GFX rewrite、拒絕換物件／新角色／場景或Main終止。同步Context隔離finally還原，僅原deferred lane；沒有新queue／worker／timer／TLS／SQL owner。

{count}方法inverse、179 no-init、32新＋同SHA921純契約與凍結位元一致重建；120 Source方法／38檔、20 SQL class原位元。原Plan新增1個volatile proof欄位。**0綁定producer／HP／wait／fatal-area／IO／Main／game／MySQL案例**。Fatality真實完整矩陣、其他NONE／unknown來源、NPC／bot／companion AI、healing／undead、58材料／Source外控與正式隔離合成仍未完成；不部署、不寫SQL／schema／WAL。

'''+text[b:]
if npcplayer:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

真正MonsterAiTick canonical constructor在原NPC lookup前固定primary PC binding／life及原房間成員，validTick加原目標生命gate；原source驗證／AI／RNG本體保留。原natural／controlled ingress只存immutable Members、原NPC Ref及detached娃娃防禦snapshot，原leave清除精確scope；沒有新TLS、沒有持有Context／Batch／Frame／Receipt。

六個實際NPC→PC入口（一般／受控、兩area物理、兩magic）接原target action lock、既有admission／commitSelf及原source Ref。固定Source物理false／false、魔法true／false，修正原共同true；未知／無scope／WORLD保留原fallback。原raw Plan Guard只帶原目標角色／生命，沿相同FIFO、actual fatal seal；所有原raw final結果另比對封存accepted-life，封住同角色復活再死亡ABA，不增IO鎖；HP入列後不新增攻擊者活著或master可用性要求。既有娃娃防禦proof／final frame沿原召喚與receiver快照，不借換角後娃娃。

{count}方法inverse、186 no-init、32新＋同SHA953純契約與凍結位元一致重建；122 Source方法／38檔、20 SQL class原位元。新增tick final reference與原thread-owned ingress reference，共2個欄位；沒有新queue／worker／timer／TLS／SQL。**0綁定AI／HP／角色／doll／IO／Main／game／MySQL案例**。上游NPC actor-stat-chance／AI-factory與接受後magic副作用、其他NPC目標生產端、bot／ground pulse／healing／undead、58材料／外控／正式合成仍未完成；不部署、不寫SQL／schema／WAL。

'''+text[b:]
if npcmagiceffects:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

兩個真正NPC→PC magic入口在原paired pre-HP Plan凍結Source寒冰之觸10／吸血鬼之觸28的原requested heal與精確ERASE lease；不是用native compatibility drainsLife只含28當Source分母。原accepted completion once claim，拒絕不回復／不consume，接受致死仍可回復原活NPC，但不執行terminal target mutation。只在proven NPC scope略過舊整批提前加總heal；原catalog／dice／成本／Ctrl／WORLD fallback保持。

accepted heal帶原NPC／owner／life／generation，走既有admission→Hub→encounter檢查及兩個原heal有界sibling，保留dead／invalid／snapshot-result並用long cap，不能借victim或新角色、新NPC。HPBAR只refresh既有weak tracked viewers，global control／receiver binding／range／current ratio／life／Main到final加密前；**actual writeFrame成功後**才記錄新ratio。原completionFrame同步隔離victim Frame，area Context隔離finally還原，無queued Context／Scope／Definition／Frame／Receipt或新queue／worker／timer／TLS／SQL。

{count}方法inverse＋2 heal sibling、194 no-init、33新＋同SHA985純契約與凍結位元一致重建；124 Source方法／40檔、20 SQL class原位元。原Scope與Plan共2個有界欄位。**0綁定HP／heal／角色／IO／Main／game／MySQL案例**。NPC ALT_ATKMSG／其他accepted mutation、上游完整數值AI與其他NPC-target、controlled／bot／ground／healing-undead、58材料／外控／正式合成與集中驗收未完成；不部署、不寫SQL／schema／WAL。

'''+text[b:]
if npcmagicdirect:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

NPC攻擊魔法非正傷害分支依固定Source補齊精確魔消lease：零傷害在原目標actionlock／admission／commitSelf直接執行，略過HP sink，不製造接受事件；負傷害先凍結detached Mutation，保留原HP提交，返回後才執行副作用並finally清除既有Plan carrier。正傷害維持前批accepted-once分支。缺metadata／definition、非攻擊、物理及WORLD保留原路徑。

原來源服／帳號／角色／生命、status owner、OPEN／Main與實際target lock共同驗證；提交間重放／recast、DRAINING及Main關閉不能刪除新lease。未新增field／queue／worker／timer／TLS／SQL／成本或RNG。

{count}個唯一改動class原方法inverse，其餘父JAR條目全位元相同；196類JVM不初始化、37項新離線契約＋同SHA1018項繼承純契約、124個Source方法／40檔及凍結同位元重建通過；20個SQL class保留原位元。**真實HP／角色／IO／Main／遊戲／MySQL均0**。

完整有號HP／治癒／不死系、mana-only、其他技能副作用、ALT_ATKMSG、上游NPC數值AI-factory、其餘材料／外控及正式0.6.71隔離合成仍未完成；全部程式完成後集中驗收，不部署、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if signedrecovery:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

追實際signed路徑確認NPC原CalcTicket／FIFO／NativePlayerVitals已保留有號值；本批修正共用CombatPlayerRecovery負傷害生產端仍直接寫playerHp／立即送HP與ratio的缺口。只在Source BATTLE負值接原target FIFO，正值／WORLD／未載入Source的完整原方法保留為sibling。

入口依既有Context原bindings，缺原目標不採新角色；優先繼承raw Member、較早physical Stamp或magic Snapshot生命。原target／caster ordered pair、OPEN admission及commitPair下重驗後，沿原signed CalcTicket／raw Plan入列；queued Guard只留原target，caster退場／換角不撤回已入列HP。目標舊role／life、Main、STOPPED由原HP／final receiver守門，DRAINING可排空原事件。無新field／queue／worker／timer／TLS／actor／直接HP／直接IO／SQL／成本或RNG。

{count}原方法inverse＋1完整原fallback sibling，其餘父JAR條目全位元相同；201類JVM不初始化、31新離線契約＋同SHA1055繼承純契約、420組精確凍結Source HP區塊算術比對與同位元重建通過。124 Source方法／40檔及20 SQL class保留。**真實HP／治癒／pair／角色／IO／Main／遊戲／MySQL均0**。

所有外層cast第一次等待前的目標生命捕捉、完整治癒／不死系反轉／型別／免疫／數值及各生產端仍未完成；NPC其他mutation／ALT_ATKMSG／上游AI-factory、其餘材料外控、正式0.6.71隔離合成與集中實機驗收繼續開放。不部署、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if healingcast:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

實際單體治癒、集體治癒與大地的祝福仍各有直接PC HP store，尚未被前批共用負傷害FIFO覆蓋。本批先補三個原root的caster pre-Context binding／life／pose，及原房間參與者在角色動作鎖等待前的固定快照。單體PC在原resolve後、cost／RNG前驗證；兩個原潛在目標清單保留順序與party／clan／競技／Ctrl／range／LOS等原判定，逐一排除舊角色／生命／位置及新加入者，不過濾原視覺接收者。

重用既有角色locks的32次try／release及NativePlayerActionScope，先取得角色鎖，再進OPEN admission；不在外層持Hub再等admission。既有commitPair及finish／failure／exception負責原成本、輸出與參與者釋放；原signed member優先取同一Member，不採等待後的新生命。新增1個原Context Object欄位、2個helper，無新queue／worker／timer／TLS／SQL／成本或RNG。

{count}原方法inverse＋3完整原root sibling、203類JVM不初始化、32新離線契約（含實際未綁定locks／scope成功、失效、例外及巢狀父鎖清理）＋同SHA1086純契約／420繼承Source HP算術與凍結重建通過；124 Source方法／40檔、20 SQL class保留。**真實target wait／cost／RNG／heal／HP／IO／Main／game／MySQL均0**。

三個主要治癒的直接PC HP與原預覽HP／ratio輸出仍待改接signed FIFO／accepted／final receiver；首次Hub發現前生命證明、完整治癒／不死系／controlled-NPC-bot／status／數值，以及其餘材料外控AI-factory與正式隔離合成仍開放。全部程式完成後集中驗收，不部署、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if healinghp:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

單體／集體／大地祝福三個PC healing store在Source cast改送原requested heal的signed HP，沿既有CalcTicket／raw Plan／target FIFO／accepted／final Frame；未標記與WORLD保留完整原Math.addExact／IADD表達式。零治癒保留原direct target effects但不造HP事件；三個反轉傷害port依凍結Source L1MagicPc.commitPc用true／false旗標，正傷害保留原接受後數字與致死路徑。

另修正原傷害completion會無條件寫戰鬥／last-damage狀態的問題：common display entry在原Plan新增detached negative-input Recovery proof，不覆蓋NPC After／direct mutation。實際BATTLE completion只對已證明、非致死、非正結果的治癒走HP／ratio；未改變／拒絕不送，正／fatal／unmarked／WORLD保留原方法。queued proof只原target，caster退場／cast close不撤回HP；新角色／生命／Main／STOPPED由原guard拒絕。

22個原send保留原sender鏈，只攔精確PC治癒HP preview；3個PC preview ratio略過，group HP cost／MP、NPC／companion及single bot ratio保留。原競技／Ctrl／party／clan／range／LOS／型別、RNG、water-life／status及成本回滾本體inverse保留。無新HP lane／worker／timer／TLS／SQL。

{count}原方法inverse、205類JVM不初始化、42新離線契約＋同SHA1118繼承純契約／420既有Source HP比較、125 Source方法／42檔、20 SQL class及凍結重建通過。**真實heal／HP／cost／RNG／IO／Main／game／MySQL均0**。

完整治癒／不死系／型別／免疫／數值、首次Hub發現前生命、NPC／controlled／bot／periodic等其他生產端與completion消費端，以及其餘材料Source外控AI-factory／正式隔離合成／集中實機驗收仍未完成。不部署、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if healingformula:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

Source治癒不再只用寫死INT骰加成：實際CombatDatabase.load→NativeSkillTargetMetadata.read原唯讀交易內增加1個有界角色_智力成長SELECT，原metadata驗證成功才發布immutable growth snapshot。讀治癒骰加成中文欄位、不限制10；INT<1為-2，超過127取127列，缺選用欄位／列依固定BaseStatTable規則回退。借用連線不close、不改交易、不新增connection、不寫資料；讀取／schema失敗沿原啟動驗證傳出，真實MySQL重載尚未執行。

標記Source cast下，實際共用INT lookup、兩status函式、三lawful函式及group／nature原抽骰加法／water-life乘法改為Source整數語意；保留原抽骰順序、次數、die bounds、fixture與成本。其他／WORLD後備本體完整inverse保留。正常玩家cast leverage10，其他leverage／生產端／effective-stat／型別免疫與不死系仍未完整。

{count}原方法inverse、213類JVM不初始化、25新離線契約＋同SHA1160繼承純契約、160狀態／30 lawful凍結Source比較及420既有HP比較與同位元重建通過；129 Source方法／43檔、20 SQL class保留。**真實DB／INT列重載／actor／heal／HP／role／IO／Main／game均0**。

全部程式完成後才集中同SHA後端、中文MySQL真裝備與INT成長唯讀重載、游標與實機驗收。其餘材料／Source外控／NPC factory-AI／原生命目標、正式0.6.71隔離合成仍開放；不部署、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if healingtype:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

Source只對實際L1MonsterInstance讀不死系；PET／SUMMON／TAMED不能沿其模板undead1／3反轉或歸零。已修正單體及group／Nature共用受控角色的3個原讀取點：標記Source healing cast內讀原selected record／exact actor的final Kind，不建立新目標／快照／SQL查詢。ARENA／null／未標記／WORLD保留原行為，未知自然factory／bot型別不宣告完成。

只改ClientSession及CombatHealing，新增1個無欄位、無配置的型別helper。原HP、status、成本、RNG、成對鎖定、生命週期、Main及輸出本體inverse保留；這批型別契約不代表受控正治癒接受提交／最終HPBAR生命週期已完整。

{count}原方法inverse、214類JVM不初始化、40新離線契約＋同SHA1185繼承純契約、112型別signed比較（84受控Source＋28建模Monster算術）、160／30／420既有Source status／lawful／HP比較與同位元重建通過；131 Source方法／45檔、20 SQL class保留。**真實actor／heal／HP／role／IO／Main／DB／game均0**。

自然Quest／Time factory、守護塔／門、bot、受控治癒提交與最終輸出、週期治癒、first-Hub生命及全材料／Source／NPC factory-AI與正式0.6.71隔離合成仍開放。程式全部完成後才集中同SHA後端、中文MySQL真裝備唯讀重載及實機游標驗收；不部署、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if controlledhealing:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

受控角色治癒在原玩家鎖等待前、原try/finally保護內固定原owner Member及actor life／canonical bag／placement-master-scene-pose；cast僅1個volatile Object carrier，原close清除。單體與group／Nature共用的3個原heal呼叫驗原施法者／主人action lock、admission、OPEN／Main及原目標生命後，使用原synchronized HP sibling，只將正治癒加法改為Source long-safe max clamp；WORLD／未標記／ARENA完整原heal保留，沒有新FIFO／worker／TLS／registry／SQL或RNG／成本／attack事件。

只攔此cast已固定受控id的原healing預覽ratio；HP實際改變後，以detached原target／receiver送既有scope output，actor／Hub monitor已釋放。沿原generic marker與真正加密前frame檢查主人／召喚獸／觀眾role-life、bag／placement／pose、目前HP及max denominator、hpbar／screen／DRAINING／STOPPED／Main；不保存Cast／Context，其他動畫與成本封包保留。

{count}原方法inverse、218類JVM不初始化、28新離線契約＋同SHA1225繼承純契約、90個pet／summon凍結Source正HP邊界比較及同位元重建通過；133 Source方法／45檔、20 SQL class保留。離線玩家替身補原completion State，確認無null-state cleanup錯誤。**真實bound actor／heal／HP／role／IO／Main／DB／game均0**。

完整signed／傷害／主人meter／週期、自然factory型別、塔門／bot及first-Hub生命仍開放，綁定角色退場／HPBAR／socket實測留待全部程式完成後集中執行。全材料／Source／NPC factory-AI／正式0.6.71隔離合成仍未完成；不部署、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if firstplayers:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

Source BATTLE在原Context的Hub鎖內角色名單Binding.capture取得點，先固定原life，再呼叫原binding capture一次；原caster入Hub前binding、名單／順序／IdentityHashMap.put／原鎖與try/finally保留。若life與binding間死亡復活，保存舊life而非重新接受新生命。第一筆同actor不被重複名單覆蓋；WORLD／未載入維持原capture。

原Context僅新增1個bounded volatile Object carrier，在原建構成功返回時封存identity snapshot；copy沿用同快照。shared signed member的最後fresh fallback改借原Member，缺人／角色不符回null；較早healing cast／raw／doll／magic proof優先順序保留，既有healing target census已使用此共享口。helper無Context／Hub／cast back-reference、新HP／cost／RNG／queue／TLS／registry／worker／SQL。

{count}原方法inverse、221類JVM不初始化、26新離線契約＋同SHA1253繼承純契約及同位元重建通過；繼承90／112／160／30／420 Source比較、133 Source方法／45檔、20 SQL class保留，無離線completion cleanup錯誤。**真實bound Hub discovery／HP／role／IO／Main／DB／game均0**。

這是shared signed／healing的first-discovery程式接線，不代表所有Source factory或實際Hub／換角／復活競態已完成。其餘raw／doll／magic建證口、型別免疫、自然factory-AI、塔門bot／週期／controlled owner-meter、全材料Source及正式0.6.71隔離合成仍開放。程式完成後才集中後端／中文MySQL唯讀／實機游標驗收；不部署、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if playerfactories:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

10個原Context工廠接首次生命／位置：caster在原binding前且等待Hub前固定life；既有magic snapshot前seal／inherit完整名單。5個一般Member點覆蓋raw名單／root／explicit peer、娃娃主目標與Fatality目標；已接受frame的原target／source建證本體保留，不能拿初始快照取代。

magic／controlled的2個Actor、natural／material的3個capture與shared Actor.capture沿原life-pose；較早raw／healing Member或doll／magic Actor仍優先，合法accepted-death token保留。材料主目標PC初始life借原證明，NPC與後續即時token驗證留原。缺名單／binding不符用明確stale非null legacy entry，不能新接活角色；WORLD／unmarked原本體與生命／位置operands保留。

raw peer Function只在原同步computeIfAbsent借既有Action，無新欄位／排隊函式；實際LambdaMetafactory形狀離線執行。{count}原方法inverse、227類JVM不初始化、34新＋同SHA1279繼承純契約、90／112／160／30／420既有Source比較與凍結同位元重建通過；133 Source方法／45檔、20 SQL class保留，無cleanup錯誤。**真bound cast／HP／role／IO／Main／DB／game均0**。

完整剩餘Member constructor census附在next-producer收據；accepted／explicit-life／NPC tick-master／cube／其他來源與pose排序仍待獨立稽核，NPC first-discovery、全材料Source型別免疫／factory-AI／塔門bot週期／owner-meter與正式0.6.71隔離合成仍開放。程式全補齊後才集中後端／中文MySQL唯讀／實機游標驗收；不部署、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if acceptedplayers:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

既有actual HP acceptedState尾端、原Queue vitalLock持有期間，Frame單一volatile Object封一次immutable participant values。target沿此event的Plan.targetLife／exact raw Guard acceptedLife，位置固定於HP接受點；source保持原Guard／Actor生命與位置。資料不反存Frame／Context／Plan／Member，原HP drain／math／enqueue／unlock／completion-CAS-admission原class位元保留。

2個accepted raw Member口複製原接受值，合法屍體token保留，後續復活／共用Guard改值／新位置不能重接；unknown／WORLD原constructor保留且不宣告合格。新增Member explicit-pose constructor保留原初始化，5個doll／signed Actor轉Member口保留原Actor位置，WORLD／unsupported operands留原。NPC canonical主目標與Fatality入口在原binding與後續Hub等待前固定Source life／pose；其他viewer／master／cube／NPC discovery仍待獨立稽核。

{count}原方法inverse、230類JVM不初始化、28新＋同SHA1313繼承純契約、90／112／160／30／420既有Source比較與同位元重建通過；133 Source方法／45檔、20 SQL class保留，無cleanup錯誤。**真bound HP／role／IO／Main／DB／game均0**。

完整constructor census持續更新。unknown accepted provenance、NPC secondary viewer／master、cube／natural-controlled first discovery、全材料Source型別免疫／factory-AI／塔門bot週期／owner-meter與正式0.6.71隔離合成仍開放；程式全補齊後才集中後端／中文MySQL唯讀／實機游標驗收，不部署、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if npcorigins:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

Source NPC origin的bounded sibling在既有Hub內完成原room-members取得及life-before-binding，沿原identity map與較早primary，duplicate不覆寫生命，binding不符明確stale；Hub內沒有新取得player action lock。未載入／WORLD保留完整原方法，原member順序／binding call與其他AI流程保留。

BATTLE companion Ref在原Ref／pet monitor等待前固定主人life／binding／pose，回傳Ref immutable sibling；1個final Object僅保存weak主人證明，不新增強ClientSession／Ref／Frame／Context back-reference。8個原record identity components、equals/hash、ctor與實際NPC owner token/id/epoch/room/map/instance保留。actual controlled ingress沿舊主人資料，換角／復活／weak失效不新接；unsupported原Ref仍走原fallback並標未合格。

已准入HP與通用Ref-live程式不讀新master marker，原queue／completion-CAS／admission／HP／Ref-live class位元保留；不加入來源退場撤回既有HP的規則。{count}方法inverse、233類no-init、27新＋同SHA1341純契約、90／112／160／30／420繼承Source比較及凍結同位元重建通過；133 Source方法／45檔、20 SQL class保留，無cleanup錯誤。**真bound Ref capture／AI／HP／role／IO／Main／DB／game均0**。

主人birth-lifetime、manual／unsupported Ref、NPC actor first-discovery／AI-factory型別免疫、cube／全材料Source／塔門bot週期／owner-meter與正式0.6.71隔離合成仍開放；完整Member／Ref ctor與capture caller census持續更新。全部程式補齊後才集中後端／中文MySQL唯讀／游標驗收；不部署、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if npcfirstlife:
 goalreceipt=read(D/'source-goal-reset-attempt-current.json')['receipt'];text=re.sub(r'2026-10-25使用者表示已刪除舊目標[^\n]*',lambda _:f"2026-10-25使用者再次表示已刪除舊目標並要求重設；本次create_goal仍被系統以unfinished goal拒絕，get_goal仍為active。保留進度繼續完整範圍，未誤標完成，也未宣稱重設成功。[本次目標工具收據]({goalreceipt['path']})。",text)
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

受控NPC傷害顯示、raw NPC、材料Aim與受控治療，共6個初始生命入口沿原Context首次NPC證明：自然Quest／Time沿原Npc.epoch，受控NPC沿原Ref.owner的實際pet生命，核對原record／actor／Ref kind-id／主人binding。受控顯示主人沿較早玩家證明，不用稍後生命補權限。

只更新初始metadata；原live重驗、接受後死亡seal／approved corpse、競技／Ctrl／型別／range／LOS／成本／RNG／鎖／admission／HP／Main及WORLD／unmarked後備保留。1個stateless helper，沒有新增原類別欄位、actor registry／worker／SQL或queued Context。

{count}方法inverse、234類no-init、30新＋同SHA1368繼承純契約、90／112／160／30／420繼承Source比較及凍結同位元重建通過；133 Source方法／45檔、20 SQL class保留。**真bound AI／HP／role／IO／Main／DB／game均0**。

NPC首次actor取得／完整factory-AI型別免疫、主人birth-lifetime／manual-unsupported Ref、其餘全材料／Source／owner-meter／正式0.6.71隔離合成仍開放。全部程式完成後才集中後端／中文MySQL唯讀／實機游標驗收；未部署、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if materialfirstlife:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

負面29／152／153、集體緩速76、風枷167及共用武器buff148／149的4個Aim建構器、5個初始生命欄位，沿原Context較早玩家／受控Ref生命。11個原valid／position／final-frame入口重驗原caster／primary／owner／receiver生命與角色；保留較早source／primary位置，receiver不額外限制觀戰者、HP或移動。

3個受控技能最終附件檢查在已證明Source Context改為原battleCompanion或tamedCompanions，修正單一寵物／召喚獸不在馴服清單而漏發。WORLD／unmarked仍保留原list-only操作。原Ctrl／範圍／次目標選取、成本／RNG／status／成對鎖／HP／Main／DRAINING程式體保留；未新增原欄位、worker／registry／SQL或queued Context。

{count}方法inverse、243類no-init、39新＋同SHA1398繼承純契約、既有90／112／160／30／420 Source比較與凍結同位元重建通過；133 Source方法／45檔、20 SQL class保留。**真bound cast／status／HP／role／IO／Main／DB／game均0**。

全58材料各次目標／直線／範圍／屍體／型別免疫／成本機率、其餘NPC首次取得／factory-AI／manual Ref／owner-meter／完整外控及正式0.6.71隔離合成仍開放。程式全補齊後再集中後端／中文MySQL唯讀／實機游標驗收；未部署、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if areaplayerproof:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

共用area filter的原Context.player方法參照，沿原LambdaMetafactory捕獲形狀借原玩家生命證明；BATTLE不重取token，較早raw的合法accepted屍體生命保留。原名單順序／duplicate／immutable結果與observer／dead／移動接收資格保留，不新增target篩選規則。

nativeCompanionAreaCapturedRun與lambda$run$10兩個pre-body來源scene口核對原caster生命與位置。原32次有界player鎖／scope／admission／Hub-population／try-finally清理完整保留；不全域改Context.player／scene或已接受回呼。新增field-free helper，無token／SQL／新鎖／actor registry或queued Context。另產出目前JAR本地方法參照清單，排除record field Handles，僅作定位而非語意通過數。

{count}方法inverse、245類no-init、25新＋同SHA1437繼承純契約、既有90／112／160／30／420 Source比較與同位元重建通過；133 Source方法／45檔、20 SQL class保留。**真bound cast／HP／role／IO／Main／DB／game均0**。

其餘方法參照之accepted freeze／輸出／相消／壞物／森林／風枷須各自核對；全58材料完整型別／免疫／Ctrl／range／成本機率、其餘Source／NPC factory-AI／manual Ref／owner-meter及正式0.6.71隔離合成仍開放。全部程式補齊後集中驗收；未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if indirectplayerproof:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

其餘10個BATTLE相關bound Context.player參照（accepted-area freeze／area output／相消／盲咒／森林／風枷／壞物輸出）沿原玩家或明確accepted證明。剩下1個原參照在classic-WORLD NPC physical members，兩個caller為classicWorldMonsterPhysicalAreaTargets／nativeNpcWeaponPhysicalBody；原類別位元保留，不新增WORLD產品。

在原Queue vitalLock及Frame once-proof建構點，為原Event constructor/link之freeze Batch補無visual Plan的immutable accepted target；原既有proof優先，不新增未知attacker。後來completion重讀deathLife／同角色復活不能重設HP接受生命。freeze Batch與相消Scope的2個初始life沿原玩家／Ref；相消final-frame核對原actor／master／receiver及原single或tamed附件，保留observer／DRAINING／STOPPED／Main與原HP／queue／CAS／source-retirement／鎖／成本機率／status cleanup。

{count}方法inverse、257類no-init、38新＋同SHA1462繼承純契約、既有90／112／160／30／420 Source比較與同位元重建通過；133 Source方法／45檔、20 SQL class保留。actual Event once-link及無visual Plan之HP-lock proof使用離線metadata測試；**真bound cast／HP／role／IO／Main／DB／game均0**。

直接selection／accepted／final-output之完整語意、freeze排隊HP／input／來源效果政策、全58材料型別免疫／Ctrl／range／成本、NPC factory-AI／manual Ref／owner-meter／完整Source及正式0.6.71隔離合成仍開放。程式全部完成後集中驗收；未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if npcfactorytype:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

Companion／Quest／Time三類實際actor的9個原leaf constructor，各自持有1個final Object純量Source出生型別／template快照；delegating constructor不重取。PET／SUMMON／TAMED及轉移controlled snapshot保留實際Pet／Summon分類，其餘沿原靜態NPC snapshot之id／impl／undead；不開SQL、不保留mutable row／整份snapshot或actor registry。

依固定Source NpcTable選類規則與32個NPC類別宣告，區分Monster／MonsterGuard等非Monster，以及Tower／Door正負治療語意。單體NPC與owned／群補／Nature的2個原signed math口在已證明Source healing Context沿出生證明，unknown／unmarked／WORLD保留原算式。原RNG／成本／HP／鎖／AI／生命週期程式體保留。

{count}方法inverse、259類no-init、30新＋同SHA1500繼承純契約、896個Source類別／不死／正負／MIN_VALUE比較及既有90／112／160／30／420比較與同位元重建通過；73 Source檔／133 bounded參考、20 SQL class保留。type-only oracle使用2個原Source factory方法（僅package relocation）、Source header stub階層與原runSkill分類算式，**不代表原NPC完整constructor／AI已執行**。真bound factory／AI／healing HP／role／IO／Main／DB／game均0。

restore／rollback沿原型別的完整證明、unknown／missing factory政策、實際Source NPC factory-AI／hate-family、獨立守護塔入口／bot／periodic／全技能免疫、全58材料與完整Source及正式0.6.71隔離合成仍開放。全部程式完成後集中驗收；未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if npcfactorymemory:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

三個原Companion／Quest／Time Snapshot各有1個final transient Object與immutable sibling ctor，保留舊constructor、record components／equals／hash／toString。3個actor.snapshot與相消replacement Snapshot copy，共4個producer攜帶純量型別；相消沿新建目標Snapshot，不誤借被替換NPC。

Quest／Time四個Snapshot還原constructor只借primary原快照且驗NPC id，不再查目前模板，也不把暫時馴服的Summon型別套回原自然NPC。原型別在resource snapshot缺少／變更後仍保留；legacy／unknown／id不符維持unknown。已知實際PET／SUMMON類別可在進化後依原native kind調整copy的NPC id，不改birth stamp或讀表；ARENA id變更不能推定新類別。

{count}方法inverse、264類no-init、30新＋同SHA1530繼承純契約與896繼承Source型別比較、既有Source算式及同位元重建通過；73 Source檔／133 bounded參考、20 SQL class保留。新增純量資料不持有actor／row／snapshot／Context／Frame／registry，不變更原HP／life／inventory／鎖／WORLD。**真bound restore／rollback／AI／HP／role／IO／Main／DB／game均0**。

實際rollback／retirement／生命／IO／Main資格化、missing／unknown factory政策、初始template選取一致性、完整Source factory-AI／hate-family、獨立守護塔／bot／periodic／全技能免疫、58材料與完整Source及正式0.6.71隔離合成仍開放。程式全部完成後集中驗收；未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if npctargetfailure:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

原NPC出生Stamp新增兩個final boolean，從同一原template row凍結IsTU／IsErase；保留5個record components、舊constructor及identity，記憶體Snapshot沿原proof還原。固定Source的實際Monster／Tower／Door／其他類別失敗規則完成4,608組比較。

集體緩速保留已知NPC至原扣費階段，再於原post-cost iterator略過失敗者，保留PC順序／重複。風枷在既有post-cost旗標處使用原類別；自然Quest／Time聖光術與魔力吸取的已知失敗沿既有controlledFailure成本重播。PC／Ctrl／競技關係與完整原數值本體、HP／生命／鎖／WORLD均保留；未新增成本算法／RNG／SQL／worker／registry。

{count}方法inverse、266類no-init、37新＋同SHA1560繼承純契約、4,608新增Source比較與896繼承型別比較及同位元重建通過；73 Source檔／133 bounded參考、20 SQL class保留。**真bound cost／RNG／HP／life／role／IO／Main／DB／game均0**。

單體緩速／魔法消除較早的機率計算、集體緩速self-primary及其餘技能root／型別免疫與Source階段仍待補齊。完整58材料、NPC factory／AI、252外控、生命週期資格化與正式0.6.71隔離合成仍開放。程式完成後集中後端／中文MySQL唯讀與游標驗收；未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。

目標服務本輪仍回報舊目標active並拒絕建立新目標；未把舊目標虛報complete。最新要求作為現行工作範圍延續。

'''+text[b:]
if negativephase:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

固定Source正常施法先提交材料／預留動作，再runSkill，最後useConsume扣HP／MP；相關原方法與入口已凍結。原單體緩速29／藤蔓152及魔法消除153的完整計算區塊，在已證明Source BATTLE範圍改至原材料／動作接受及cancelMeditation後、原MP扣除前執行一次。每次呼叫以一個boolean local凍結分支，WORLD／未標記Context保留原位置與原算式。

六個原旗標入口接入Quest／Time原NPC出生型別與IsErase，已知目標通過原選取／成本階段，型別失敗在原機率前略過，也略過後續消魔／睡眠消除等目標副作用。原受控目標失敗、PC／Ctrl／競技關係、fixture範圍／原完整機率算式與RNG順序／成本回滾／成對鎖／生命／Main／WORLD保留。

{count}原ClientSession方法inverse、267類no-init、23新＋同SHA1597繼承純契約與4608繼承Source失敗／896型別比較及同位元重建通過；73 Source檔／133 bounded參考、20 SQL class保留。新增helper無field／registry／worker／SQL，**真bound material／admission／RNG／HP／life／role／IO／Main／DB／game均0**。

完整Source目標／狀態／數值語意、集體緩速self-primary／secondary、其他技能roots、NPC工廠／AI、58材料、252外控與正式0.6.71隔離合成仍開放；不能以這兩個計算區塊宣告完整Source完成。程式全部完成後集中後端／中文MySQL唯讀與實機游標驗收；未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if massselftarget:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

固定Source自選目標的type／targetTo條款允許機率系集體緩速選自己；makeTargetList先加primary，再掃範圍secondary。已證Source BATTLE的完整self-player Aim或原owned controlled Npc record，現在不會把所有已選計算清單換成empty iterator。三個原接線點分別處理原玩家predicate的self identity、self permission及既有計算iterator。

只把已通過原篩選清單的第一個caster移到前面，其餘順序／重複保留；缺席或被篩掉的caster不新增。受控主目標仍走原class failure，既有NPC免疫iterator保留；原跨角色ForcedCombat／Ctrl／競技關係與原phase／HP／invisibility／radius、成本／RNG算式／成對鎖／生命／Main／WORLD不變。helper不持有field／Context／actor／Frame／registry，只有同步且原清單大小範圍內的順序副本。

{count}原方法inverse、268類no-init、26新＋同SHA1620繼承純契約、4608繼承Source失敗／896型別比較及同位元重建通過；73 Source檔／133 bounded參考、20 SQL class保留。**真bound material／RNG／mutation／HP／life／role／IO／Main／DB／game均0**。

self oracle只覆蓋固定Source的self-specific條款，不代表較早class／zone／ghost或完整visibility／屍體／主次目標資格化；原native invisibility仍明確保留待完整Source核對。其他技能root／完整NPC factory-AI／58材料／252外控及正式0.6.71隔離合成仍開放。程式全部完成後集中後端／中文MySQL唯讀與實機游標驗收；未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if nonetargetrequest:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

固定Source的target=none在選取前忽略封包target／x／y，改用施法者本身。45個原ClientSession型別化attack-request parser現在只對已證Source BATTLE及none資料列，沿原caster Binding與更早Member座標投影新immutable request；row／column／skill、原封包及一般指定目標／Ctrl不變。沒有在等待後重抓角色／生命／座標。

MassSlow／negative／Wind／remaining-material四個原run，也在既有Context建立完成後、原Aim／目標lookup前投影同一request；未重排Context構造、未建立新TLS／NPC discovery。已同值request保留identity；missing／closed proof及WORLD／unmarked／missing metadata維持原request，不補新權威。

{count}原方法inverse、269類no-init、22新＋同SHA1646繼承純契約、4608繼承Source失敗／896型別比較及同位元重建通過；73 Source檔／133 bounded參考、20 SQL class保留。**真bound cast／selection／cost／RNG／HP／life／role／IO／Main／DB／game均0**。

另已凍結Source重要成本條件：只有NPC caster因empty target list失敗；PC正常施法即使empty仍可能沿原材料／動作與HPMP消耗。故NPC class排除不能統一放在SelectedPlayerIntent前直接return，須保留其成本／接受語意。此部分與其他object／targeted封包型別、各root自施header／geometry、class／ghost／zone／hidden／屍體／主次目標、NPC factory-AI／58材料／252外控及正式0.6.71隔離合成仍未完。

程式全部完成後集中後端／中文MySQL唯讀與實機游標驗收；未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if objecttargetrequest:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

30個原型別化object-request parser補上Source target=none的角色目標投影，沿原caster Binding與更早Member證明，不重抓角色或生命。固定Source另把原target_id保存在_itemobjid：創造魔法武器73／提煉魔石100／鎧甲護持21／擬似魔法武器12／暗影之牙107，這5種原始物品ID完整保留，不能誤換成角色ID；row／column／skill及原封包不變。

火焰武器／風之神射Target run、controlled cancellation共2個原object request，以及Blind的1個attack request，在既有Context建立後、原Aim／owner lookup前沿同一原角色證明解析。未重排Context構造／原discovery／鎖；一般指定目標／Ctrl／競技、WORLD／unmarked／missing／closed-proof與原消耗／RNG／生命／Main流程保留。獨立oracle直接執行固定Source none區塊與raw item／bookmark switch。

{count}原方法inverse、270類no-init、39新＋同SHA1668繼承純契約、4608繼承Source失敗／896型別比較及同位元重建通過；73 Source檔／133 bounded參考、20 SQL class保留。**真bound inventory／cast／selection／cost／RNG／HP／life／role／IO／Main／DB／game均0**。

目前75個attack／object parser是解析接線數，不能視為75個技能或58材料已完成。座標／clan／self／summon／bookmark等其他封包語意、每root自施header／geometry／status、NPC class／ghost／zone／hidden／屍體與empty-PC成本、完整factory-AI／252外控及正式0.6.71隔離合成仍開放。程式全部完成後集中後端／中文MySQL唯讀與實機游標驗收；未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if areaclassselection:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

四個原area入口（silence／wind／BATTLE sleep／BATTLE mass-slow）的12個原逐目標probability＋d100 iterator，接上已知Source純類別排除。含exception edges的CFG證明原材料／動作接受分支支配這些計算入口；audience-only iterator不變，原成本／MP／RNG算式／HP／鎖／生命／Main／WORLD完整inverse。Source PC empty target list仍沿原消耗，不因類別排除變成材料前免費return。

純類別條款包括Doll、maxHP0／1 Door、自然NPC主目標排PC／Pet／Summon、非Guard NPC主目標排Guard、none＋attack的8種NPC，以及馴服／造屍／復活類別限制，與固定Source 91,476組class-only組合一致；不能把這當完整isTarget（更早self／zone／ghost／LOS／hidden／dead／target bits／PK等尚未資格化）。實際接線只涵蓋這四個入口。

Quest／Time原Snapshot memory proof必須符合原actor birth值；legacy／同ID替換不採新類別。原BattleSleepTarget wrapper只讀拆解，回傳同一原物件；既有排序／重複／unknown fallback、controlled kind保留。helper不持有Context／Frame／row／registry，不新增NPC discovery／snapshot／life捕捉／worker／SQL。

{count}原方法inverse、272類no-init、30新＋同SHA1707繼承純契約、91476新增Source class-only比較及4608失敗／896型別比較與同位元重建通過；73 Source檔／133 bounded參考、20 SQL class保留。**真bound cast／cost／RNG／mutation／HP／life／role／IO／Main／DB／game均0**。

單體與其餘area／damage／healing roots、完整isTarget前後條款／status／數值／type語意、完整NPC factory-AI／58材料／252外控及正式0.6.71隔離合成仍開放。程式全部完成後集中後端／中文MySQL唯讀與實機游標驗收；未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
if damagehealingselection:
 a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
 text=text[:a]+f'''## 本次已更新的程式

targeted area／centered area／group healing／Nature四個原入口的13個逐目標傷害或治癒roll iterator，接上已知Source純類別排除；原MP debit支配這些入口（含exception CFG）。不動audience／HP-application-only loops，原數值／RNG／HP FIFO／成本／鎖／生命／Main／WORLD完整inverse，沒有新增消耗或HP算法。

原centered／targeted controlled wrapper只讀其既有Snapshot，不新建snapshot；原非controlled birth proof不借同kind新actor，實際Pet／Summon進化可沿固定class而不改原actor birth。原物件／order／duplicates／unknown fallback保留。

另一個已解碼targeted-area request在原body入口投影none；原Action constructor的primary long也沿既有caster證明投影none，避免外層仍拿原NPC作自施primary gate。physical／pointed request、原args array、原constructor／targets discovery／lock順序不變；不能將這當所有scope／member／root自施與實機角色生命週期資格化。

{count}原方法inverse、274類no-init、33新＋同SHA1737繼承純契約、91476繼承Source class-only／4608失敗／896型別比較及同位元重建通過；73 Source檔／133 bounded參考、20 SQL class保留。**真bound cast／cost／RNG／HP／mutation／life／role／IO／Main／DB／game均0**。

單體class／failure-cost、direct guardian-tower／bot與missing factory metadata、完整isTarget／visibility／LOS／hidden／屍體／PK／type／status／math語意、完整NPC factory-AI／58材料／252外控及正式0.6.71隔離合成仍開放。程式全部完成後集中後端／中文MySQL唯讀與實機游標驗收；未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。

'''+text[b:]
work.write_text(text,'utf-8')
lines=current.read_text('utf-8').splitlines()
for n,line in enumerate(lines):
 if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：娃娃Source技能先HP後特效、加傷最後獨立扣HP；修正PC旗標及數字時點，補HP消費端成對guard與死亡數字生命token。{count}原方法inverse、13類不初始化及凍結重建通過；**新增遊戲驗證0案，娃娃完整接線仍未完成**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
 if accepted and line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：PC娃娃改由既有物理HP接受後完成流程觸發，保留原proc機率；拒絕事件不回呼。Pending不保留Context／Receipt，致死目標略過技能傷害計算。{count}原方法inverse、13類不初始化及凍結重建通過；**0新遊戲案，完整Source仍未完成**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
 if npc and line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：自然Quest／Time與受控NPC娃娃接到真正物理接受流程，保留實際怪物／主人與原proc機率；技能走NPC／寵物計算，加傷raw、致死數字沿用接受token。{count}原方法inverse、16類不初始化與凍結重建通過；**0遊戲案，完整Source仍未完成**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
 if action and line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：Source物理入口於動作鎖／RNG前固定角色、目標及娃娃召喚，Pending攜帶獨立Stamp；移動不換召喚，到期／收回不阻擋普通物理HP。{count}原方法inverse、15類不初始化、凍結重建及11純判定通過；**0遊戲案，泛用娃娃SKILL及完整Source仍未完成**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
 if skills and line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：泛用娃娃SKILL唯讀metadata、魔法sibling、29速度、逐筆失敗續作及proc_spr觀看控制接線。{count}方法inverse、16類不初始化、凍結重建與18純案例；**0遊戲案／0實際DB連線**。最終Frame、角色重設、巢狀HP接受次序及Source數值缺口仍開放。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
 if output and line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：娃娃最終Frame／接收角色／原召喚與已接受速度proof、角色偏好reset接線。{count}方法inverse、22類不初始化、凍結重建與15純timer／線上秒數案例；**0遊戲案／0實際DB連線**。Source子傷害本來非阻塞，保留原次序；完整Source數值及實跑矩陣仍未完成。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if slow:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：娃娃29零／長時間、受控NPC原生加速與永久狀態代次；最終Frame保留父版生命proof。{count}方法inverse、23類不初始化、凍結重建及25純案例；**0遊戲案／0實際DB連線**。完整Source仍未完成。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if defense:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：娃娃減傷沿用原Calc／RNG／接受後尾段，補Context原召喚、致死接受生命與最終接收Frame proof。{count}方法inverse、31類不初始化、凍結重建及10純觀看案例；**0遊戲案／0實際DB連線**。無Context／被動數值與完整Source仍未完成。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if passive:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：共同娃娃能力getter於已捕捉Context／原角色鎖內固定原能力或空能力，避免替換召喚混入數值。兩類共{count}原方法inverse、37類不初始化、凍結重建及10純resolver案例；**0遊戲案／0實際DB連線**。無Context入口與完整Source仍未完成。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if cube:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：立方體原效果NPC物件／精確狀態／引擎代次與目標來源資料接線，**HP來源與Frame消費端尚未改接**。{count}方法inverse、22類不初始化與實際schema核對、凍結重建及15純引擎案例；**0遊戲案／0實際DB連線**，完整Source仍開放。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if cubeplayer:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：PC立方體raw HP改用真正效果NPC引用、原角色生命、actual commit／accepted death及最終接收Frame；NPC／MP／狀態仍開放。{count}方法inverse、35類不初始化、凍結重建及22純契約案例；**0遊戲案／0實際DB連線**，完整Source未完成。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if cubencplease:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：自然NPC cube原lease依實際actor／owner／生命／代次退役，避免同ID繼承或阻擋；PC接線保留。{count}原方法inverse、36類不初始化、27純案例及凍結重建；**0遊戲／0DB，NPC HP與派送仍未完成**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if cubenpc:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：自然NPC cube實際effect／actor HP、建立者退出派送、固定原派系與master HPBAR／獎勵、最終Frame接線；保留原波次／結束本體。{count}方法inverse、41類不初始化、37純契約與凍結重建；**0遊戲／0DB，Source FIFO／MP／狀態與完整目標仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if cubenpcqueue:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：NPC cube加入原actor FIFO／原proc drainer，移除inline HP，實際claim guard、共用PC-NPC CAS容量與drop／失敗尾段接線。{count}方法inverse、52類不初始化、33純queue／容量案例與凍結重建；**0遊戲／0DB，其他HP生產端、Source容量domain及完整目標仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if capacity:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：PC-NPC傷害專用容量與AI工作計數分開，補原dequeue／drop／失敗釋放及idle／terminated fence；原上限／設定／工作生命週期保留。{count}方法inverse、53類不初始化、16容量＋同SHA33佇列契約及凍結重建；**0遊戲／0DB，其餘NPC FIFO及完整目標仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if poisonnpc:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：自然NPC毒傷加入原target FIFO／既有drainer；原施毒者身分與HP資格分離，補接受／死亡／波次／HPBAR／最終接收端。{count}方法inverse、62類不初始化、15毒傷＋同SHA33佇列／16容量純案例及凍結重建；**0遊戲／0DB，完整目標仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if physicalnpc:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：自然NPC物理及Scope娃娃傷害共用原FIFO／舊drainer，補接受後原proc／數字與固定原觀眾的同步重建；無queued Context。{count}方法inverse、76類不初始化、30因果數字生命／原FIFO＋同SHA33佇列／16容量／15毒傷純案例及凍結重建；**0遊戲／0DB，完整目標仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if magicnpc:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：兩種範圍NPC魔法補等待前原快照／觀眾，共用原FIFO與真正接受後精確魔消lease；保留非正傷害分支。{count}方法inverse、81類不初始化、16新＋同SHA94既有純契約及凍結重建；**0遊戲／0DB，單體／方向／冰凍／暈眩及完整目標仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if singlenpc:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：NPC普通單體／吸血混亂雷霆共用原FIFO，固定輸入一次接受、原回復sibling及最終receiver；其他HP前MP／狀態家族保留原完整路徑待接。{count}方法inverse、84類不初始化、14新＋同SHA110既有純契約及凍結重建；**0遊戲／0DB，完整目標仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if targetnpc:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：方向／單體範圍冰凍／Shock共10個NPC HP端接原FIFO，依真正接受套狀態，補18個原狀態寫點租約與最終Frame。{count}方法inverse、90類不初始化、18新＋同SHA124既有純契約及凍結重建；**0遊戲／0DB，完整目標仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if specialnpc:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：心靈／骨頭／護甲破壞沿Source原HP前MP／狀態階段，實際native result接Single FIFO；stale前置不能退回同步HP，Bone補精確lease與最終Frame。{count}方法inverse、100類不初始化、24新＋同SHA142既有純契約及凍結重建；**0遊戲／0DB，完整目標仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if poisonpc:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：有原凍結clock的PC DAMAGE毒傷不再將supplied施毒者作pair peer；原target／HP／clock／Main本體保留。{count} raw方法inverse、105類不初始化、同SHA166既有純契約與凍結重建；**新毒傷案例0／遊戲0／DB0，完整目標仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if rawnpc:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：魔杖／Mortal／Joy六個自然NPC raw端接原FIFO／mode0，帶原raw來源生命規則，HPBAR存活與死亡清除分開。{count}原方法inverse、108類不初始化、12原身分metadata＋同SHA166既有純契約及凍結重建；**綁定HP／遊戲／DB0，完整目標仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if refrawnpc:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：補真正accepted Ref反饋的自然NPC反射HP端，驗原Frame／Ref／Live actor後接既有raw FIFO。{count}方法inverse、109類不初始化、12 Ref ABI＋同SHA178既有純契約與凍結重建；**綁定HP／遊戲／DB0，完整目標仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if hitreaction:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：受傷動作抑制改接既有唯讀啟動交易的Source表清單，八個原NPC反饋／cube消費端保留。{count}方法inverse、112類不初始化、14 JDBC代理＋同SHA190純契約與凍結重建；**實際MySQL／遊戲／Main0，全Source仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if npcreaction:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：補NPC反擊屏障／鏡反射兩個原受傷send端，沿原Ref／live與typed gfx套Source抑制清單，HP／防禦特效本體保留。{count}方法inverse、113類不初始化、16 Ref-snapshot＋同SHA204純契約與凍結重建；**綁定HP／Main／遊戲／MySQL0，全Source仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if wandreaction:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：魔杖自然NPC受傷動作沿原raw Action／typed gfx套Source抑制，既有空封包與final Frame保留；不改HP／扣次／PC受控後備。{count}方法inverse、114類不初始化、16 metadata-packet＋同SHA220純契約與凍結重建；**綁定HP／Main／遊戲／MySQL0，全Source仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if groundorigin:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：原火牢lease→pulse→tick固定角色／生命，時鐘端進原pair action；PC58 raw false／true與原FIFO，reaction final scene及原close清理。{count}方法inverse、125不初始化、16 metadata＋同SHA236純契約與凍結重建；**queued HP獨立scene proof仍缺，綁定HP／Main／遊戲／MySQL0，全Source開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if groundqueued:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：ground原proof進既有raw Plan，補防禦RNG前拒絕事實、actual resolved-caster HP及acceptedLife final Frame；個別expiry不撤回已准入HP。{count}方法inverse、126不初始化、18 metadata＋同SHA252純契約與凍結重建；**實際HP／Main／遊戲／MySQL0，整體場景資格化與全Source仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if groundplacement:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：修正前批只覆蓋舊direct工廠；實際材料two-copy／成本／install帶原prewait Batch binding，新lease指向live engine，拒絕不發布scene。{count}方法inverse、131不初始化、18 staged-metadata＋同SHA270純契約與凍結重建；**實際MP／材料rollback／HP／Main／遊戲／MySQL0，全Source仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if groundnpc:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：自然NPC火牢原tick→mode0 FIFO、Source抗火／INT／免疫前置、原觀眾反應與detached scene-life final guard；准入後移動不撤回。{count}方法inverse、135不初始化、26新＋同SHA288純契約與凍結重建；**綁定HP／Main／遊戲／MySQL0，完整Source仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if groundplayer:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：PC火牢有效INT／正抗火／完整SKD3鍵、免疫前置與原競技權限；保留receiveDamage減傷及false-true FIFO，原Frame補反應範圍。{count} inverse、136 no-init、35新＋同SHA314純契約與凍結重建；**綁定HP／Main／遊戲／MySQL0，完整Source仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if cubeplacement:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：修正cube真正材料工廠原未綁定；原prewait binding與live Entity／faction、accepted scene及Source500ms scan接線，保留900／1000 status。{count} inverse、138 no-init、20新＋同SHA349純契約與凍結重建；**綁定成本／HP／Main／遊戲／MySQL0，全Source仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if cubestatus:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：地裂／衝擊材料、primary-secondary實際目標與immutable final身分索引、數值／輸出guard；NPC一秒freeze及Source衝擊marker-only。{count} inverse、151 no-init、24新＋同SHA369純契約與凍結重建；**綁定成本／HP／freeze／IO／Main／遊戲／MySQL0，全Source開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if cuberemainder:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：燃燒／和諧狀態身分索引與final Frame；PC原MP store接受通知、自然NPC原actor回魔及Source有界加法。{count} inverse、154 no-init、25新＋同SHA393純契約及凍結重建；**綁定角色／真實MP／IO／Main／遊戲／MySQL0，完整Source仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if cubepresentation:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：八個立方體狀態GFX接Source唯讀metadata、和諧封存原relation、友方屬性先特效、0 GFX及正值資源映射；地裂／衝擊等待後租約到期guard。{count} inverse、157 no-init、27新＋同SHA418純契約與凍結重建；**綁定IO／角色／HP／Main／遊戲／MySQL0，完整Source開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if cuberelation:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：四cube原Source Binding自身／clan-party判定與競技限制；同ID新角色不借舊self，原installed HP不受creator退場撤銷。{count} inverse、161 no-init、33新＋同SHA445純契約、凍結重建；**綁定角色／HP／IO／Main／game／MySQL0，完整Source開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if cubeclock:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：追實際GameServer→StartTimer_Skill，更正500葉方法誤判為global initial0／period1000；原Hub四相位跨房間／放置沿原clock及close，status900／1000不變。{count} inverse、163 no-init、33新＋同SHA478純契約（1個舊cadence預期更正）、凍結重建；**真Hub／HP／Main／game／MySQL0，全Source開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if cubevisibility:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：8個cube物件輸出／重建、原move perceive與Output一次rescue、實際writeFrame後known-role配對；舊角色／同ID／未看見不借舊remove。{count} inverse、169 no-init、39新＋同SHA511純契約與凍結重建；**真queue／角色／IO／Main／game／MySQL0，全Source開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if cubetemplate:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：既有靜態NPC模板pre-cost純值凍結、四效果物件初始GFX／動作／exp／lawful／name與四encoder；區分chaLight和template light。{count} inverse、175 no-init、371新＋同SHA550純契約與凍結重建；**真factory／成本／角色／IO／Main／game／MySQL0，自訂hook及全Source開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if fatalitydamage:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：Fatality single/area原角色pose與paired RNG/raw FIFO、explicit NONE Calc、detached final receiver；其他NPC未知來源不當NONE，原native catalog保留。{count} inverse、179 no-init、32新＋同SHA921純契約／凍結重建；**真HP／角色／IO／Main／game／MySQL0，全Source仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if npcplayer:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：原NPC tick target-life、原ingress娃娃防禦快照與cleanup、6個NPC→PC target lock/raw FIFO；Source物理false/magic true，queued HP不依攻擊者在線。{count} inverse、186 no-init、32新＋同SHA953純契約／凍結重建；**真AI／HP／角色／doll／IO／Main／game／MySQL0，上游數值AI及全Source仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if npcmagiceffects:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：NPC magic真正accepted drain10/28／exact erase、原actor long-safe heal與tracked meter actual post-write cache；拒絕不放副作用。{count} inverse＋2 sibling、194 no-init、33新＋同SHA985純契約／凍結重建；**真HP/heal/角色/IO/Main/game/MySQL0，ALT_ATKMSG／其他mutation與全Source仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if npcmagicdirect:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：NPC零傷害direct erase不建HP事件；負傷害保留HP提交後exact-lease副作用與Plan清除。原角色／life／Main／OPEN守門。{count}方法inverse，其餘父JAR全位元相同、196 no-init、37新＋同SHA1018純契約及凍結重建；**真HP/角色/IO/Main/game/MySQL0，signed HP／其餘Source仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if signedrecovery:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：共用BATTLE負傷害直接寫HP改走原signed FIFO；原target/caster pair與既有來源生命，queued僅原target，施法者退場不撤回已入列HP。{count} inverse＋1 fallback、201 no-init、31新＋同SHA1055純契約、420 Source HP算術與凍結重建；**真HP/heal/pair/角色/IO/Main/game/MySQL0，外層cast／全Source仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if healingcast:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：單體／集體／大地祝福原caster與目標life-pose固定，cost/RNG前原primary／secondary篩選；原locks先於admission，既有scope釋放。{count} inverse＋3 sibling、203 no-init、32新＋同SHA1086純契約／420既有Source HP比較與凍結重建；**真heal/HP/wait/IO/Main/game/MySQL0，三個直接PC HP／預覽輸出與完整Source仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if healinghp:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：三PC治癒／反轉走原signed FIFO，negative-input proof讓治癒completion只送真HP/ratio、不寫攻擊狀態；攔原PC預覽，保留cost/NPC/bot/WORLD。{count} inverse、205 no-init、42新＋同SHA1118純契約/420既有Source HP比較及凍結重建；**真heal/HP/IO/Main/game/MySQL0，完整治癒數值／其他Source仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if healingformula:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：原readonly啟動交易載INT治癒骰中文外控，保留Source cap127／缺欄fallback／專用bonus無上限10；標記治癒signed/status/lawful與原RNG語意。{count} inverse、213 no-init、25新＋同SHA1160純契約/160 status/30 lawful/420既有HP比較與凍結重建；**真DB/HP/IO/Main/game0，其他數值型別／全Source仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if healingtype:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：修正Source受控PET／SUMMON／TAMED的3個模板不死系讀取點，依原selected／actor final Kind；原HP／成本RNG／鎖／生命／Main／WORLD本體保留。{count} inverse、214 no-init、40新＋同SHA1185純契約、112型別比較與凍結重建；**真actor／HP／IO／DB／game0，完整Source仍開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if controlledhealing:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：受控治癒3原HP口接原owner／actor生命、成對鎖與admission，long-safe原synchronized HP sibling；提交後meter有原receiver與真正final frame，攔舊預覽。{count} inverse／218 no-init／28新＋同SHA1225純契約／90 Source HP比較及凍結重建；**真bound HP／IO／DB／game0，全目標開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if firstplayers:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：原Hub名單取得點life-before-binding固定，immutable copy沿用；shared signed／healing最後fallback借原Member、缺人不新接，較早ingress優先序保留。{count} inverse／221 no-init／26新＋同SHA1253純契約與凍結重建；**真bound Hub／HP／IO／DB／game0，全目標開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if playerfactories:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：10原工廠／pre-Hub caster／raw-peer-doll-Fatality／magic-controlled-natural-material／shared Actor借原life-pose，材料PC初始life沿舊證明；accepted frame／WORLD保留。{count} inverse／227 no-init／34新＋同SHA1279純契約與凍結重建；**真bound cast／HP／IO／DB／game0，全目標開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if acceptedplayers:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：原HP-lock接受值一次封Frame，2 raw口沿approved life-pose／corpse，5 Actor轉Member保留位置，NPC primary／Fatality pre-Hub入口life-before-binding；unknown／WORLD留原。{count} inverse／230 no-init／28新＋同SHA1313純契約與凍結重建；**真bound HP／IO／DB／game0，全目標開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if npcorigins:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：NPC roster原Hub內life-before-binding、保留較早primary；Ref immutable weak主人證明於pet wait前固定並接actual ingress，原8 identity與queued HP/Ref-live/WORLD保留。{count} inverse／233 no-init／27新＋同SHA1341純契約與凍結重建；**真bound Ref／AI／HP／IO／DB／game0，全目標開放**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
if npcfirstlife:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：controlled/raw/material/healing 6初始口沿原NPC epoch／Ref.owner與較早主人證明；原accepted死亡seal、live重驗、Ctrl與WORLD保留。{count} inverse／234 no-init／30新＋同SHA1368純契約與凍結重建；**真AI／HP／role／IO／Main／DB／game均0**，完整Source仍開放，不部署。 |'
if materialfirstlife:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：negative／mass-slow／wind／weapon-buff 5初始life欄位沿原proof，11 ingress／position／final-frame gate；3受控最終附件支援原single或tamed。{count} inverse／243 no-init／39新＋同SHA1398純契約與凍結重建；**真cast／status／HP／role／IO／Main／DB／game0**，完整Source仍開放，不部署。 |'
if areaplayerproof:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：共用area原bound predicate沿首次玩家生命，2 pre-body來源scene沿原life-pose，保留approved corpse／observer audience與原鎖清理。{count} inverse／245 no-init／25新＋同SHA1437純契約及凍結重建；**真cast／HP／role／IO／Main／DB／game0**，完整Source開放，不部署。 |'
if indirectplayerproof:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：10 indirect roster proof、原HP-lock freeze accepted target（無visual Plan可用）、freeze／相消初始生命與相消final single-or-tamed附件；剩1原WORLD callback留原。{count} inverse／257 no-init／38新＋同SHA1462純契約與凍結重建；**真cast／HP／role／IO／Main／DB／game0**，完整Source開放，不部署。 |'
if npcfactorytype:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：9 actor leaf捕捉純量出生型別，2 Source healing signed口沿32原NPC類別；MonsterGuard／Tower-Door與template reload分開。{count} inverse／259 no-init／30新＋同SHA1500純契約／896新Source比較與凍結重建；**真factory／AI／HP／role／IO／Main／DB／game0**，完整Source仍開放，不部署。 |'
if npcfactorymemory:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：3 transient Snapshot sibling／4 producer／4 restore沿原型別，不查新模板，保留record identity與已知pet進化。{count} inverse／264 no-init／30新＋同SHA1530純契約／896繼承Source型別比較與凍結重建；**真restore／rollback／AI／HP／role／IO／Main／DB／game0**，完整Source開放，不部署。 |'
if npctargetfailure:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：2出生旗標／8目標失敗phase ports；{count} inverse／266 no-init／37新＋同SHA1560純契約／4608 Source比較與凍結重建。**真bound cost／RNG／HP／life／role／IO／Main／DB／game0**；完整Source仍開放，不部署。 |'
if negativephase:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：29／152／153原機率區塊置於材料／動作接受後、MP扣除前；6出生型別旗標ports。{count} inverse／267 no-init／23新＋同SHA1597純契約／凍結重建；真bound material／RNG／HP／life／role／IO／Main／DB／game0，完整Source未完、不部署。 |'
if massselftarget:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：集體緩速3 self-primary ports，保留已選secondary並僅將已選self-PC primary置前。{count} inverse／268 no-init／26新＋同SHA1620純契約／凍結重建；真bound material／RNG／mutation／HP／life／role／IO／Main／DB／game0，完整Source未完、不部署。 |'
if nonetargetrequest:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：none投影原caster／pose，45 typed parsers＋4原post-Context pre-Aim ports。{count} inverse／269 no-init／22新＋同SHA1646純契約／凍結重建；真bound cast／cost／RNG／HP／life／role／IO／Main／DB／game0，完整Source未完、不部署。 |'
if objecttargetrequest:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：30 object parser＋2 object／1 Blind pre-Aim ports，保留5個Source raw item IDs與原caster證明。{count} inverse／270 no-init／39新＋同SHA1668純契約／凍結重建；真bound inventory／cast／cost／RNG／HP／life／role／IO／Main／DB／game0，完整Source未完、不部署。 |'
if areaclassselection:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：4 area roots／12 admitted target RNG iterators排已知Source類別，audience不變。{count} inverse／272 no-init／30新＋同SHA1707純契約／91476 class-only Source比較與凍結重建；真bound cost／RNG／HP／life／role／IO／Main／DB／game0，完整Source未完、不部署。 |'
if damagehealingselection:
 for n,line in enumerate(lines):
  if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 候選 **`{digest[:12]}…`**：13 post-MP damage／healing target roll loops＋2 none primary data ports，原HP FIFO／audience不變。{count} inverse／274 no-init／33新＋同SHA1737純契約／凍結重建；真bound cost／RNG／HP／life／role／IO／Main／DB／game0，完整Source未完、不部署。 |'
current.write_text('\n'.join(lines)+'\n','utf-8')
marker='\n\n2026-10-24 娃娃Source提交順序續作：'
text=doc.read_text('utf-8').replace('舊目標工具拒絕替代未完成目標，未將舊目標虛報完成。','系統目標已依使用者指示重新建立並啟用。')
doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`只在Source BATTLE走新分支；技能HP／特效及加傷順序依UnifiedProcExecutor，PC技能true／false、加傷false／false；加傷保持獨立HP與預先數字。原RNG／計算／WORLD完整原方法透過{count}方法inverse保留。PC消費端雙方成對前快照與actual HP guard、致死數字token；自然NPC以實際receipt接受token，proof不持有Context／Receipt。13類不初始化、凍結同位元重建，20 SQL class原位元；0新遊戲案／0DB連線。首次物理捕捉、接受後proc回呼、受控NPC、SKILL29等與減傷完整路徑仍未完成；先更新程式後集中驗收，不部署。\n','utf-8')
if accepted:
 marker='\n\n2026-10-24 PC娃娃物理接受後續作：'
 # Preserve the earlier stage paragraph with its own digest; it remains historical scope.
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`沿用原rolled proc，PC物理事件攜帶無Context／Scope／Receipt的Pending，由既有accepted completion一次領取並清除，再執行娃娃、發布物理數字；拒絕事件沒有回呼。成對消費端與actual HP guard、fatal dead-target SKILL在magic RNG前略過，沿用既有工作者／關閉流程。{count}原方法inverse、13類不初始化及凍結同位元重建，20 SQL class原位元；0遊戲案／0DB連線。首次動作捕捉、自然／受控NPC、SKILL29、減傷與特效最終接收端仍未完整；目標active，不部署。\n','utf-8')
if npc:
 marker='\n\n2026-10-24 自然及受控NPC娃娃接受流程續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`自然Quest／Time四個HP返回點於Hub鎖後固定receipt，原命中死亡／波次／獎勵發布完成後才執行娃娃，避免巢狀proc改變原結算；再發布物理數字。受控NPC原physical本體保留sibling，只在原正值appliedDamage及Target.seal處接proc。SKILL走既有寵物／NPC計算、bonus raw；同actor的後續接受生命token更新數字Proof，不重新查數字ID。同步Scope不進延遲Plan，不新增worker／TLS／SQL；{count}原方法inverse、16類不初始化及凍結同位元重建，20 SQL class原位元。0遊戲案／0DB連線；最初PC攻擊／娃娃實例、NPC免疫buff、SKILL29、減傷、特效Frame與生命週期矩陣仍未完整。目標active，不部署。\n','utf-8')
if action:
 marker='\n\n2026-10-24 物理入口與娃娃召喚身分續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`Source BATTLE物理入口於action-lock等待前固定來源與選定目標；原攻擊body/RNG前重驗。activeMagicDoll volatile、召喚tuple識別位置紀錄；背包Item在動作鎖內proc RNG前封存，能力getter沿用原本體fallback並固定本次原召喚或空能力。娃娃到期／收回不拒絕普通物理HP；queued proc攜帶獨立Stamp，不持有Context／Scope／Receipt。受控目標保持原owner鎖流程。{count}方法inverse、15類不初始化、凍結同位元重建及11純判定，20 SQL class原位元、0遊戲案／0DB連線。20個Source參考方法揭露泛用娃娃SKILL數值／metadata與29魔法計算依賴，尚未宣告29或全部娃娃完成；外控、減傷、NPC免疫、最終Frame與完整同SHA矩陣仍開放。不部署。\n','utf-8')
if skills:
 marker='\n\n2026-10-24 泛用娃娃SKILL及proc觀看續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`同一既有readonly啟動交易新增1條SELECT，發布不可變技能metadata；泛用SKILL不再只收area模型，原魔法sibling只改用同列技能等級。loop逐筆傷害／目標castgfx／娃娃67／29速度，最後raw加傷；失敗續作、不加普通材料MP／機率；proc_spr暫存偏好預設true並接既有Source action，目標10格／娃娃可見畫面。{count}原方法inverse、16類不初始化、凍結同位元重建與18純案例，20 SQL class原位元、0遊戲案／0真實DB連線。未宣告29全路徑或泛用魔法數值完成：最終Frame、角色reset、PC多事件接受次序、NPC免疫buff／負傷害、守護天賦等仍開放；新reader需後續中文MySQL唯讀重載。目標active，不部署。\n','utf-8')
if output:
 marker='\n\n2026-10-24 娃娃最終Frame與已接受速度續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`三迴圈固定viewer binding，沿用owned scope於實際Frame驗角色／生命／原娃娃／偏好／範圍；速度結果獨立綁接受的實際target與加速／緩速到期，允許同場景移動，status proof不依賴仍存續的娃娃或proc_spr，但原scope的施攻者／peer角色發佈gate仍保留，來源退役送達尚待實跑。8個既有timer欄位volatile、原狀態本體不變；角色selected邊界reset proc_spr、本人速度包最終秒數更新。Output Proof無Context／Scope／Receipt，無新worker／TLS／SQL，Frame不拿actor／Hub／status鎖。{count}方法inverse、22類不初始化、凍結同位元重建及15純timer／wire案例；20 SQL class原位元、0遊戲／0真實DB連線。29方法Source證據修正子HP假設：enqueue為非阻塞入列，Source不逐筆等待；不新增同步／continuation模型，改驗原FIFO與接受guard。NPC完整Source數值及真正Frame／角色／生命／Main矩陣仍開放。不部署。\n','utf-8')
if slow:
 marker='\n\n2026-10-24 娃娃29時間與狀態代次續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`原Source BATTLE娃娃29使用既有status／pet／Hub鎖與timer scalar；零／非正int毫秒為永久、長期限有限狀態與低16位writeH分開，原PC speed同步查詢補有界前綴。受控NPC原生加速無可移除技能則保持原狀；既有取消／到期／生命清理本體保留。4個owner的volatile Object代次與Speed proof的final代次拒絕解除後重施相同永久期限的舊包，無角色／Context保留、無新registry／worker／SQL。Frame沿用原receiver／角色／生命／場景與速度proof，只更新封包本地秒數；有效0與到期分開。{count}方法inverse、23類不初始化、凍結同位元重建、25純案例及20 SQL class原位元；0遊戲案／0真實DB連線。完整NPC數值／免疫／自然原生速度、減傷、其餘材料／外控及實跑矩陣仍開放。目標active，不部署。\n','utf-8')
if defense:
 marker='\n\n2026-10-24 娃娃減傷原召喚與最終Frame續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`保留NativePlayerCommit原減傷Calc／RNG及接受後一次發布／裝備尾段；Source BATTLE原Context固定各角色原召喚與binding，原drawDefense於既有受擊動作鎖下封存Item，不給舊動作替換娃娃能力。DefenseVisual只攜獨立身分／召喚／觀眾proof，真HP接受點封存fatal生命，最終Frame重驗角色／原娃娃／proc_spr／10格目標gfx或娃娃可見畫面；失效不拒絕普通HP、不中斷原裝備尾段。動態Frame載體finally復原，無新worker／TLS／SQL、無最終actor鎖。{count}方法inverse、31類不初始化、凍結同位元重建與10純觀看案例；20 SQL class原位元、0遊戲／0真實DB連線。Source35方法證實零差額仍接受、減傷特效在接受後。無Context／無已證明動作鎖入口與被動數值尚待覆蓋；完整Source數值／NPC／材料／外控及實跑矩陣未完成。目標active，不部署。\n','utf-8')
if passive:
 marker='\n\n2026-10-24 被動娃娃原角色能力選定續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`原物理Action powers分支保留；共同getter後備在已捕捉Source BATTLE Context且持有原角色鎖時，按原角色Snapshot的Choice固定完整Source能力或空能力。第一次後不混替換娃娃、未封存前失效固定空；角色／生命／位置／背包換代仍拒絕。Snapshot內Choice表依原參與者有界、子Context共用，本次娃娃proof未增加Choice／Snapshot／Context欄位；其他既有Context事件仍待關閉矩陣；無新鎖／RNG／worker／TLS／SQL。原ClientSession數值／成本／HP／queue位元不變；兩類{count}原方法inverse、37類不初始化、凍結重建與10純resolver案例，20 SQL class原位元、0遊戲／0真實DB連線。盤點31 getter呼叫點及22直接HP方法不代表覆蓋；真正Source attacker與Java參數null須逐入口區分，cube需實際效果NPC。無Context入口、NPC完整數值、58材料／外控／AI及實跑矩陣未完成；目標active，不部署。\n','utf-8')
if cube:
 marker='\n\n2026-10-24 立方體原效果與精確狀態來源續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`Cube205／220保留原Cube Entity及NPC80149／80152、engine clear代次、原Room與建立者關係binding；原Hub掃描於engine等待前固定PC／Quest／Time角色或actor生命，未綁定BOT不冒充Source Monster。StatusLease與Event攜帶精確原實例，同ID重裝及舊到期分開；只有剛安裝列讀既有鎖內map，八秒狀態保留cube消失後的_effect物件。臨時安裝／scan載體finally復原，無新鎖／worker／TLS／SQL，原數值／時鐘／關係／HP指令經{count}方法inverse保留。22類不初始化與實際schema核對、凍結重建及15純引擎案例，20 SQL class原位元、0遊戲／0真實DB連線。**這是來源資料基礎，HP施攻者及PC／NPC提交／最終Frame仍未改接，不宣告Cube Source完成。**完整材料／外控／NPC／AI及同SHA矩陣仍開放；目標active，不部署。\n','utf-8')
if cubeplayer:
 marker='\n\n2026-10-24 PC立方體原效果NPC與HP提交續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`Source BATTLE PC cube205／220使用Ref kind5 Effect(Entity,lifetime)，cube id為實際來源，無玩家master／caster。raw10／25 false／true走原typed HP Calc／防禦／Avatar／佇列與空數字Plan；燃燒先邏輯發布受傷動作，和諧不增一般魔法數字。精確原lease於入列前驗，入列後允許狀態到期／cube消失及同場景移動，仍驗效果代次／角色／生命／Main；原實際precommit、fatal接受封存與最終receiver Frame守衛接線。queued proof無Context／Scope／Receipt／Frame／Plan，無新worker／TLS／SQL；原engine鎖內退休已換代PC lease，kind5不觸發反應型回授。{count}方法inverse、35類不初始化、凍結重建與22純契約案例；20 SQL class原位元、0遊戲／0真實DB連線。實際wire／佇列／Main矩陣未驗，NPC HP／建立者退役派送／MP／aura／狀態與完整材料／外控仍開放；目標active，不部署。\n','utf-8')
if cubencplease:
 marker='\n\n2026-10-24 自然NPC立方體狀態退役續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`原Hub／engine掃描點將MONSTER狀態依原實際StatusLease／Cube、Quest／Time actor／owner／生命／generation退役；同ID換代不繼承或阻擋新lease，移動出範圍不取消原狀態。PC／BOT與WORLD／未綁定後備及既有HP／queue／輸出本體保留，無新鎖／worker／carrier／TLS／SQL。{count}原方法inverse、36類不初始化、27個真實engine-map與metadata純案例、凍結同位元重建及20 SQL class原位元；0遊戲／0DB。這不是實際NPC HP提交guard：已發出callback、建立者退役派送、HPBAR／動作、MP／狀態／aura與完整材料／外控及同SHA Main矩陣仍未完成。重設請求被目標工具以既有unfinished goal拒絕，既有完整目標仍active，未虛報重設或完成，不部署。\n','utf-8')
if cubenpc:
 marker='\n\n2026-10-24 自然NPC立方體實際HP與原master續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`兩個時鐘與兩個舊非玩家入口接原effect／精確lease／Quest-Time actor，HP raw10／25不依建立者在線，Quest安裝時固定派系。原master另驗binding供HPBAR／lawful，Source45方法確認effect先入列後解析master；原仇恨／AI仍待接。Hub HP兩個與發布三個受限副本保留原死亡／波次／spawn／邊界／公告／結束；無借用PC，接收IO不回滾HP。燃燒10格受傷動作走既有gfx抑制catalog，和諧不加普通earth／魔法公式。HPBAR比例／死亡hide與最終receiver／target／spawn guard、accepted life封存及carrier finally接線；原pending計數／executeCommitted納入同步in-flight，無新worker／queue／timer／TLS／registry／SQL。{count}方法inverse、41類不初始化、37純契約、凍結重建及20 SQL class原位元；0遊戲／0DB。尚未完整對齊Source NPC非同步FIFO，MP／狀態／aura／表reader／AI／全部材料外控與同SHA Main矩陣仍開放；目標active、不部署。\n','utf-8')
if cubenpcqueue:
 marker='\n\n2026-10-24 自然NPC立方體原FIFO與claim續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`原cube producer之prepare／hurt後只非阻塞入原actor status-owned queue，原sealed Plan明確加Ticket，原proc drainer混合派送並走相同try/finally complete；無新queue／worker／timer／TLS／registry。HP guard要求精確原queue active event／queued-claimed旗標及effect／target生命，producer finally不早退役或重複扣queue名額。PC-NPC使用原GLOBAL及同一CAS，失敗PC發布釋放預留，NPC queue不再等admission；七個原非傷害credit writer不變。清列取消Ticket、獎勵IO失敗仍走結束尾段；queued欄位無Context／Scope／Receipt／Frame／Runnable。Source50方法確認pending在dequeue、HP前釋放，保留native claim時點；Source容量domain與其他直接NPC HP仍待統一。{count}方法inverse、52類不初始化、33純CAS／真實未綁定FIFO與混合drainer／drop案例、凍結重建及20 SQL class原位元；0遊戲／0DB。MP／狀態／aura／AI／全材料外控／正式合成及同SHA Main矩陣未完成，完整目標active、不部署。\n','utf-8')
if capacity:
 marker='\n\n2026-10-24 傷害專用容量與Main歸零邊界續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`原lifetime新增一個final AtomicLong傷害ledger，super後即初始化；PC-NPC CAS只預留此domain，成功發布仍加原work credit。精確原dequeue／drop／PC發布失敗釋放對應名額，七個原非傷害credit writer及AI程式位元不變；原Source設定鍵／GLOBAL／queued-only target限制保留。idle／terminated加傷害歸零fence以涵蓋admission窗口，原Main／worker／retry／close本體透過{count}方法inverse保留；無新registry／queue／worker／timer／TLS／SQL。53類不初始化、16容量隔離／並行／失敗案例＋同SHA33未綁定queue契約、凍結重建及20 SQL class原位元，0遊戲／0DB。其他直接NPC HP仍須共用FIFO及接受計畫，真實owner容量／設定／關閉、材料外控／MP狀態／AI／正式合成／游標矩陣未完成；完整目標active，不部署。\n','utf-8')
if poisonnpc:
 marker='\n\n2026-10-24 自然NPC毒傷共用FIFO與原施毒者續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`原poison Lease固定原Room／actor／owner／生命／generation，原tick不直接HP；typed毒傷Ticket共用cube／原proc的同一target FIFO、舊worker／drainer／try-finally complete／傷害ledger與Main。實際claim重驗目標及場景，HP不依賴施毒者仍存活，已入列後不因毒到期／替換撤銷；原binding另控HPBAR／lawful與最終receiver，不給同ID新角色權限。五個有界HP／發布sibling保留poison=true、屍體／波次／spawn／AI-arm／獎勵／結束，fatal生命及status到期proof分開，無queued Context／Scope／Receipt或新queue／worker／timer／TLS／SQL。{count}方法inverse、62類不初始化、15毒傷混合queue／drainer／drop＋同SHA33佇列／16容量純案例、57 Source方法凍結與同位元重建、20 SQL class原位元；0遊戲／真HP／Main／DB。其餘直接NPC FIFO、PC毒傷、仇恨AI／數值／全部58材料外控／正式合成及集中驗收未完成；目標active、不部署。\n','utf-8')
if physicalnpc:
 marker='\n\n2026-10-24 自然NPC物理及娃娃原FIFO接受後續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`自然Quest／Time物理與Scope娃娃SKILL／加傷共用cube／毒傷／原proc的同一target FIFO及舊drainer；原物理兩端POP回傳值，改非阻塞入列不假造同步接受。原來源／NPC生命位置／場景及roster於actual active claimed Ticket再驗；原已擲AttackProc／召喚Stamp／觀眾binding獨立保留，HP票據無Context／Scope／Receipt。五個有界HP／發布sibling保留死亡／波次／spawn／AI-arm／獎勵／結束後，一次重建固定原觀眾的同步Context並執行原proc，再發布物理數字；子HP非阻塞共用尾端，原SKILL／bonus數字時點與fatal proof保留。結構輸出移至HP／來源動作／admission鎖外，娃娃沿既有owned publication、最終Frame重驗；原OPEN／因果DRAINING／STOPPED／Main／容量／drop本體不變。{count}方法inverse、76類不初始化、30因果數字生命／混合／巢狀原FIFO＋同SHA33佇列／16容量／15毒傷純案例、58 Source方法凍結與同位元重建、20 SQL class原位元，0真HP／遊戲／Main／DB。一般魔法與其他NPC生產端、Source全部物理接受後副作用／數值／AI／58材料外控／正式合成及集中驗收仍未完成；目標active、不部署。\n','utf-8')
if magicnpc:
 marker='\n\n2026-10-25 自然NPC範圍魔法原快照與接受魔消續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`兩個Source BATTLE範圍入口於原等待前固定caster／實際NPC／owner／生命位置／generation及原觀眾binding，子Context沿用，不刷新權限；四個原POP回傳的NPC魔法HP入同一原FIFO／舊worker-drainer，精確erase-map lease存既有Commit的獨立After，無queued Context／Scope／Receipt／Frame／Runnable或整份Snapshot。原accepted callback先邏輯發布魔法數字、再以actual accepted actor／life／generation消耗舊lease，保留重施ABA。固定Source正傷害等接受、非正傷害原副作用分支只沿舊Hub模式／active與原生命guard消耗精確lease，不造HP／數字；producer temporary finally復原，WORLD／原primary-Ctrl／型別距離／成本RNG／Main保留。{count}方法inverse、81類不初始化、16身分／真實狀態契約＋同SHA30物理／33佇列／16容量／15毒傷純案例、64 Source方法與凍結同位元重建、20 SQL class原位元；0真HP／遊戲／Main／DB。單體吸血／混亂雷霆、方向／冰凍／暈眩接受計畫、其他NPC生產端／全58材料外控／完整數值AI／正式合成與集中驗收仍未完成；目標active、不部署。\n','utf-8')
if singlenpc:
 marker='\n\n2026-10-25 自然NPC單體吸血混亂雷霆接受續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`自然NPC普通單體／吸血混亂雷霆葉節點共用原FIFO及等待前原Source／NPC／觀眾快照，body前重驗原身分；心靈／骨頭／護甲破壞仍留完整同步舊路，待HP前MP／狀態轉真正接受計畫，不假造結果。Single只帶原spell／lease、算好秒數與原傷害回復量、一次claim，無queued Context／Scope／Receipt／Frame／calculator／snapshot；混亂沿原roll，雷霆入列前原calculator一次，接受後不重擲。三個原接受finally邊界在HP鎖外、native波次結束前一次發布固定副作用，致死HP仍可回復；原alive／long上限／PC monitor的回復sibling只改本人HP及peer ratio輸出，原receiver／Source生命與封包現值於Frame重驗，owned publication及carrier finally、Main／容量原機制保留。{count}方法inverse＋1回復sibling、84類不初始化、14新＋同SHA110純契約、66 Source方法與凍結同位元重建、20 SQL class原位元；0真HP／遊戲／Main／DB。方向／冰凍／暈眩、其他前置副作用／數值機率／NPC AI-factory／58材料外控／正式合成與集中驗收未完成，目標active、不部署。\n','utf-8')
if targetnpc:
 marker='\n\n2026-10-25 自然NPC方向冰凍暈眩接受續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`方向／單體及範圍冰凍／兩個Shock BATTLE body的10個NPC HP端接原FIFO，5個body前重驗，保留等待前原source／actual NPC／生命位置／generation及觀眾快照。原計算chance／roll／boss固定，取消snapshot HP存活推估，真正接受後且native波次結束前一次狀態；Source暈眩時間保留接受副作用階段抽一次。18個原expiry寫點、4個volatile expiry及4個有界lease欄位覆蓋啟用／取消／到期／reset同值ABA，舊特效／palette輸出帶原receiver／Source／NPC生命與精確狀態至最終Frame，IO走既有owned publication。無新queue／worker／TLS／SQL，無queued Context／Scope／Receipt／snapshot；成本／RNG計算／PC／受控／WORLD原本體inverse保留，完整數值機率／屍體語意仍待稽核。{count}方法inverse、90類不初始化、18新＋同SHA124純契約、68 Source方法與凍結同位元重建、20 SQL class原位元；0綁定HP／遊戲／Main／DB。心靈骨頭護甲破壞前置副作用、其他生產端／全58材料外控／正式合成與集中驗收仍未完成，目標active、不部署。\n','utf-8')
if specialnpc:
 marker='\n\n2026-10-25 自然NPC心靈骨頭護甲破壞Source前置階段續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`四個Source新方法確認三技skillmode先扣MP／套狀態再回傳傷害，L1SkillUse在HP前呼叫，修正原待辦的accepted-only假設。Mind／Bone精確原本體在原Source動作／admission／Hub／實際owner下重驗後回傳真正native result，Arm兩葉同守原身分；owned publication scope先於Hub／owner建立。單一有界Context carrier每cast重設，只讓真正執行前置的自然NPC HP進原Single FIFO；stale／未執行直接handled drop，無假成功或同步HP fallback，不替Source增設前置回滾。Bone8原expiry寫點、2個volatile scalar／2個有界lease及2個原輸出端補最終Frame；PC／受控／WORLD及原RNG計算成本inverse保留。{count}方法inverse、100類不初始化、24新＋同SHA142純契約、72 Source方法與凍結同位元重建、20 SQL class原位元；0綁定HP／遊戲／Main／DB。其餘生產端／完整數值與外控／全58材料／正式合成與集中驗收仍未完成，目標active、不部署。\n','utf-8')
if poisonpc:
 marker='\n\n2026-10-25 PC凍結DAMAGE毒傷原施毒者peer續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`Source不以施毒者存活／在線撤回target-owned tick；原scoped DAMAGE本體已null caster及raw false／true，外層Action仍supplied caster作pair peer。只改一個建構參數，Source BATTLE且Scope.find(target)原context／clockActive／frozen DAMAGE才無施毒者peer。原target／role／life／scene、clock／HP旗標／queue／admission／Main／cleanup／歸屬本體保留，無新欄位／worker／registry／SQL／HP／輸出。{count}原raw方法inverse、105類不初始化、同SHA166既有純契約、72 Source方法與同位元凍結重建、20 SQL class原位元；新PC毒傷案例0、綁定HP／遊戲／Main／DB0。無Scope／其他kind／nested與完整歸屬、其餘生產端／全材料外控／正式合成與集中驗收仍開放，目標active、不部署。\n','utf-8')
if rawnpc:
 marker='\n\n2026-10-25 自然NPC raw魔杖反射反饋原佇列續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`魔杖／Mortal／Joy六個原POP自然NPC raw葉端共用原FIFO／worker／claim／容量／admission／Main與mode0，無普通數字／娃娃proc。原Origin七component保留，只新增final detached raw Source proof，沿原Member accepted life／role／position／dead規則，保留Source致死defender已接受反饋；普通未標記Origin原source policy不變。精確source Actor／actual target／owner／life／generation匹配暫存Admission，觀眾原快照、finally復原，無queued Action／Context／Scope／Receipt／snapshot；不額外加OPEN-only葉端檢查。共同accepted HPBAR／死亡波次／獎勵結束，存活HPBAR producer及Frame保留來源存活，死亡hide獨立。{count}原方法inverse、108類不初始化、12metadata＋同SHA166既有純契約、74 Source方法與凍結同位元重建、20 SQL class原位元；綁定raw HP／致死defender／遊戲／Main／DB0。其餘raw／AI／治癒／全技能外控／正式合成及集中驗收仍開放，目標active、不部署。\n','utf-8')
if refrawnpc:
 marker='\n\n2026-10-25 真正接受Ref反饋自然NPC反射端續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`acceptedTail→acceptedFeedback→validated Ref→Quest/Time反射HP，六個legacy/static POP不是本生產端證據。本批只補一個深層native反射端，原validFrame／live後傳同Ref／Live，Source BATTLE匹配raw Action再驗accepted非WORLD Frame、原method／descriptor／id／operands／room／owner／same actor，再進既有raw mode0 FIFO；stale handled drop，unscoped／受控／WORLD原fallback，RNG／已算結果／反應抑制／reflecting finally及原live本體保留，無新欄位／worker／queue／registry／SQL／queued Ref-Context。原PC HP尾段直接送死亡確認，無直接endBattle，未新增child wait，真實確認／場景／Main矩陣仍待驗。{count}方法inverse、109類不初始化、12 Ref ABI＋同SHA178純契約、74 Source方法與凍結同位元重建、20 SQL class原位元；綁定反饋／致死defender／遊戲／Main／DB0。其餘生產端／技能外控／正式合成與集中驗收仍開放，目標active、不部署。\n','utf-8')
if hitreaction:
 marker='\n\n2026-10-25 Source受傷動作抑制唯讀啟動接線：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`於既有readonly且autocommit=false交易新增Source gfx表SELECT，無新Connection或交易／命中SQL。immutable Set依Source空表／重複／NULL=>0／int集合及SQLException空或部分列行為發布；未載入後備本體保留。八個既有NPC reaction caller與全部HP／生命／Frame／Main原class保留，其他ACTION_Damage／RO target-sync／多resource路由未完成。{count}原方法inverse、112不初始化、14 JDBC代理＋同SHA190純契約、80 Source方法及凍結重建，20 SQL class原位元；實際MySQL／遊戲／綁定HP／Main0。完整Source與材料、正式合成及集中驗收仍開放，不部署、不寫玩家SQL／schema／WAL。\n','utf-8')
if npcreaction:
 marker='\n\n2026-10-25 NPC反擊屏障鏡反射原身分受傷send接線：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`補兩個shared NPC publisher的第二個受傷encrypted send，Source BATTLE沿原companion／Quest／Time ingress Ref，id不符drop，原private live與typed snapshot gfx套唯讀清單。WORLD／未載入／unscoped／unsupported後備、HP／防禦特效／機率成本狀態／觀眾畫面／sender／順序inverse保留，無新欄位／retained Ref-Live-Context／worker／queue／SQL，驗證鎖於IO前釋放。{count}方法inverse、113不初始化、16 Ref-snapshot＋同SHA204純契約、82 Source方法及凍結重建，20 SQL class原位元；綁定反射HP／接收端／Main／遊戲／MySQL0。Source完整數值與phase仍開放，其他封包同步／多resource／全技能與正式合成未完，不部署、不寫玩家SQL／schema／WAL。\n','utf-8')
if wandreaction:
 marker='\n\n2026-10-25 魔杖自然NPC原raw動作受傷外控接線：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`只在Source BATTLE action2原自然Quest／Time raw Action中，沿原frame與id／kind／exact actor／snapshot typed gfx套唯讀抑制；空byte沿原captureOutput加密前略過，原defer／final Frame保持binding／來源目標life／scene／Main。無正HP／OPEN-only／扣次後item新gate，PC／受控／unsupported／WORLD／unscoped／unloaded原後備；HP／扣次RNG／item／特效／queue／worker／SQL原位元。{count}原方法inverse、114不初始化、16 unbound metadata-packet＋同SHA220純契約、84 Source方法與凍結重建，20 SQL class原位元；綁定wand HP／扣次／致死目標／接收端／Main／遊戲／MySQL0。其餘Source數值成本時序／週期治癒AI／全材料外控／正式合成與集中驗收開放，不部署、不寫玩家SQL／schema／WAL。\n','utf-8')
if groundorigin:
 marker='\n\n2026-10-25 原地面租約週期事件與PC火牢raw接線：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`原PC58工廠綁scene／epoch／creator Binding，原lease／pulse／GroundTick final detached metadata，在synchronized advance固定角色life。原時鐘PC端驗同lease後走既有ordered raw pair，RNG前重驗；HP葉端raw false／true共用FIFO，原reaction final Frame加scene carrier、原Action.close在原輸出完成後清理。競技／原geometry-timer／計算gate／非Source-WORLD-BOT-NPC後備inverse保留，無新worker／queue／clock／TLS／registry／SQL。{count}方法inverse、125不初始化、16 unbound metadata＋同SHA236純契約、91 Source與凍結重建，20 SQL class原位元；綁定PC HP／pair／角色／receiver／Main／遊戲／MySQL0。下一步補queued HP獨立scene-lease metadata，現只沿raw member guard；完整NPC火牢、Source數值間隔型別免疫／主人死亡／effect顯示及全技能正式合成未完成，不部署、不寫玩家SQL／schema／WAL。\n','utf-8')
if groundqueued:
 marker='\n\n2026-10-25 ground排隊HP原場景與接受後生命證明接線：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`原raw HP Plan單一volatile detached PulseProof，不保存Context／Action。drain用原resolved caster在防禦RNG前檢查並保留primitive拒絕local，原actual HP guard再驗actor binding-life／false-true／原lease Entity指標-scene-epoch-Main；unmarked原行為保留。final Frame用原HP鎖內acceptedState之target acceptedLife保留合法致死輸出，拒絕後續life ABA／role／scene；不以個別expiry或新OPEN-only撤回已准入HP，原DRAINING與STOPPED／Main保留。{count}方法inverse、126不初始化、18 unbound metadata＋同SHA252純契約、91 Source與凍結重建，20 SQL class原位元；實際HP／致死／pair／角色／receiver／Main／遊戲／MySQL0。完整場景線性化與first-wait factory、NPC火牢與Source數值顯示／全技能正式合成仍未資格化，不部署、不寫玩家SQL／schema／WAL。\n','utf-8')
if groundplacement:
 marker='\n\n2026-10-25 ground真正材料副本成本安裝工廠接線更正：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`前批只覆蓋舊direct GameHub.placeGroundSpell，實際材料走nativeMaterialPlaceGroundSpell→NativeGroundMaterialAction.castBattle兩副本／成本／install，新lease原未有Source metadata。本批沿原current Batch.accepted Ingress.binding（reserve於首次player-lock等待前保存），驗同原caster／room／live engine，不fresh capture。detached Draft進兩暫存copy，new Entity指向live engine；live scene僅在原accepted install發布，原cost callback／Rejected／recalc／install順序inverse保留，finally還原installing slot，無retained Batch-Ingress-Context／新TLS-registry-worker-SQL。{count}方法inverse、131不初始化、18 unbound staged-metadata＋同SHA270純契約、91 Source與凍結重建、20 SQL class原位元；綁定角色／MP-reagent-rollback／HP／Main／遊戲／MySQL0。完整Source與NPC火牢／全材料／正式合成／集中驗收仍開放，不部署、不寫玩家SQL／schema／WAL。\n','utf-8')
if groundnpc:
 marker='\n\n2026-10-25 自然NPC火牢原tick與raw FIFO接線：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`原ground advance追加自然Quest／Time actor／owner／life-generation／pose與原觀眾，Entity固定原creator lane，pulse／原Tick在null-PC分支前排程派送；正常PC／BOT後備保留。原來源lock後、RNG前驗原場景生命／NPC位置與freeze-earth-bind，唯讀stats與有效INT按Source正抗火／屬性弱化計算；反應先於原mode0 FIFO raw HP，無普通魔法數字／娃娃proc。HP及最終輸出沿detached Origin proof／原接受後生命，保留移動與個別到期後的已准入傷害及DRAINING／STOPPED／Main；不新增queue-worker-TLS-SQL或保留Context。{count} inverse、135 no-init、26新＋同SHA288純契約、93 Source方法／25檔、凍結重建及20 SQL class原位元；0綁定HP／角色／Main／遊戲／MySQL。完整型別-SKM0-PC SKD3／其他來源／顯示／材料外控及正式合成集中驗收仍開放，不部署、不寫玩家SQL-schema-WAL。\n','utf-8')
if groundplayer:
 marker='\n\n2026-10-25 PC火牢Source SKD3有效數值與反應範圍：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`原paired raw action的PC58 prefix在RNG前驗精確lease／scene／source-target role-life與競技RUNNING canAttack、freeze-barrier-bind；有效INT／正抗火後依料理及dragon-patience-immune原lease選Source SKD3固定鍵，先加減再整數half。保留原receiveDamageInternal reductionOnly防禦proc／武器-化身修正，原raw false-true FIFO／accepted fatal-life／Main，未啟用directRaw。反應使用原frozen audience／deferred scope且原ground final Frame再驗native screen，無新增fields-scope-TLS-queue-worker-SQL。{count} inverse、136 no-init、35新（含Source31列及其餘ID oracle）＋同SHA314純契約、93 Source方法／26檔、凍結重建及20 SQL class原位元；0綁定pair-HP-RNG-fatal-receiver-competition-Main-game-MySQL。完整Source／材料／型別／競技對應與正式隔離合成集中驗收仍開放，未部署或寫SQL-schema-WAL。\n','utf-8')
if cubeplacement:
 marker='\n\n2026-10-25 cube真正材料工廠與Source掃描節拍更正：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`前批cube-origin只接legacy place；真正NativeCubeCastAction→Hub.nativeMaterialPlaceCube→engine.nativeMaterialPlace用plain ctor原未有Entity／faction，Source HP端因而捨棄。本批兩Hub口用原prewait Batch Ingress.binding與detached Draft，兩原ctor接既有metadata overload，原geometry-admit-cost-ID-list保持；accepted return才發布scene、finally復原slot。只改Source scene scan500ms，原L1Cube900／1000 status／4th ignition-MP與5th harmony HP保持，原實際effect-NPC HP及role-life-FIFO-final-output保持。{count} inverse、138 no-init、20新＋同SHA349純契約、101 Source方法／29檔、凍結重建及20 SQL class原位元；0綁定MP-material-rollback-HP-role-Main-game-MySQL。Quake／Shock工廠、完整Source／材料／顯示MP端／正式合成與集中驗收仍開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if cubestatus:
 marker='\n\n2026-10-25 地裂衝擊原材料狀態與跨角色secondary生命：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`原Quake／Shock材料口接prewait Batch binding與actual Entity／faction、accepted scene；原成本geometry不變。primary／secondary租約存原target role-life-generation及MR refresh link，原advance發布bounded immutable volatile索引供數值與final Frame重驗；舊expiry／recast不借新cell，clear換epoch清索引。NPC地裂接原actor1秒freeze；固定Source衝擊只有4000 marker、MR0，WORLD／BOT／未綁定原數值保留；500 scan與900／1000 status保留。原typed clock與raw self-output-close承接原receiver、scene／epoch／life／index／range，最終不遍歷mutable map／新增locks，無新HP queue-worker-timer-TLS-SQL或queued Context。{count} inverse、151 no-init、24新＋同SHA369純契約、106 Source方法／32檔及凍結重建、20 SQL class原位元；0綁定cost-HP-freeze-IO-Main-game-MySQL。完整Source／材料／MP與顯示／正式合成集中驗收仍開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if cuberemainder:
 marker='\n\n2026-10-25 燃燒和諧原狀態與MP接受生命：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`燃燒／和諧原advance有界immutable identity索引、nonHP實際租約／target／原receiver；屬性／狀態／final Frame拒絕舊角色或替換cell。PC回魔原store接受tag與實際MP agreement；接受後到期不撤銷已提交通知。自然NPC用原actor／life／generation／faction與原admission-Hub-encounter回魔sibling，不借caster或新ID。Source long bounded加法、原後備overflow及原HP FIFO／成本／節拍保留。原raw close清同步proof；無新queue-worker-timer-TLS-SQL。{count} inverse、154 no-init、25新＋同SHA393純契約、108 Source方法／32檔、凍結重建及20 SQL class原位元；0綁定角色／實際MP／IO／Main／遊戲／MySQL。完整Source與集中驗收仍開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if cubepresentation:
 marker='\n\n2026-10-25 立方體Source狀態GFX與原關係：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`八個PC／自然NPC原狀態GFX口讀既有唯讀metadata castgfx／castgfx2；和諧原status發布前存實際relation，same-id／關係變更不改舊cell，Source自然怪選敵方特效。Source友方屬性經typed bridge原status body先於effect，略過同scope原尾端重複通知；到期／未綁定／WORLD／BOT保留。Source0 GFX沿原object-word，正值資源remapping／界限保留；原raw／final Frame／close與HP成本節拍不變，Quake／Shock installed加等待後到期拒絕。一個原status volatile metadata與無狀態helper，無新queue-worker-timer-TLS-SQL。{count} inverse、157 no-init、27新＋同SHA418純契約、109 Source方法／33檔、凍結重建及20 SQL class原位元；0綁定角色／IO／HP／MP／Main／game／MySQL。完整Source及集中驗收仍開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if cuberelation:
 marker='\n\n2026-10-25 立方體原Source角色與競技新租約：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`四cube advance原self／Alliance口沿原creator Binding及prewait target Scan；獨立確認Binding.source已是qualified Source角色，不再caster ID查當前session或重複組合。self驗原source-account-selected-owner-role與target life，same-id新角色不承接舊self。原clan-party ally保留，新hostile遵守原PvP／team及原force、實際target force，不借Ctrl；新套用沿原Hub／engine驗epoch-Main-OPEN-RUNNING，natural kind/life/gen/faction及BOT／WORLD後備保留。原installed status／queued HP／cost／RNG／clock／output不變；無新鎖／欄位／queue-worker-timer-TLS-SQL。{count} inverse、161 no-init、33新＋同SHA445純契約、109 Source方法／33檔、凍結重建及20 SQL class原位元；0綁定角色／HP／IO／Main／game／MySQL。完整Source及集中驗收仍開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if cubeclock:
 marker='\n\n2026-10-25 立方體真正Source啟動與全域相位更正：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`固定Source GameServer→StartTimer_Skill直接排四Runnable initial0／period1000；先前start500葉方法當生產路徑的判斷取代，1個既有cadence oracle更正，Source檔不改。原Hub四個bounded primitive Phase於ground task註冊前init、原type collector在rooms前推進一次、close清空；空場／room-clear／不同placement保留服務相位，同slot跨room，延遲掃live一次且保留fixed-rate origin。四原new-effect compare接slot、Source cadence1000；原100ms owned drainer與900／1000 status、HP／FIFO／cost／RNG／output保留，無新schedule-worker-timer-TLS-queue-lock或actor／Context保留。{count} inverse、163 no-init、33新＋同SHA478純契約（477原預期＋1更正）、111 Source方法／35檔、凍結重建及20 SQL class原位元；0綁定Hub／startup／多場／角色／HP／IO／Main／game／MySQL。全Source與集中驗收仍開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if cubevisibility:
 marker='\n\n2026-10-25 立方體原輸出與寫出後known-role配對：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`四實際放置＋四重建口帶原觀眾／pre-Hub receiver與effect；accepted publish沿原Hub-engine seal index。detached Work走原Output，原loop及三completion尾端once rescue，creator退場不借新角色；其他output／completion保留。原move perceive管native screen進出、明確重建可refresh，final加密前驗instance／epoch／index／角色／到期／range／object-geometry；實際writeFrame成功後才mark／remove known。expiry只remove真正收到的原角色，same-id／新角色／unseen不借舊事件，初始audience釋放與stale seen prune。Work無Context-Batch-Action-Frame-Receipt、同步Context隔離finally還原；原encoders／資源rewrite／HP／成本／RNG／0-1000與900-1000節拍保留，無新queue-worker-timer-TLS；compiler-only visibility stubs未入jar。{count} inverse、169 no-init、39新＋同SHA511純契約、113 Source方法／36檔、凍結重建及20 SQL class原位元；0綁定queue／rescue／角色／IO／Main／game／MySQL。Source template／GFX／visibility外控等完整目標開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if cubetemplate:
 marker='\n\n2026-10-25 立方體原靜態模板初始呈現續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`原pre-cost Draft凍結既有snapshot純值，四cube一次生成Source initial GFX/status/exp/tempLawful/name，四encoder走actual effect owned值、原native tail與資源rewrite；模板lightSize不誤當初始chaLight0。缺列／錯型／失敗／非法bound先拒絕，無new SQL或actor，5個final純值欄位隨原生命。呈現random順序與Source executable oracle對照，原戰鬥cost/chance RNG／HP／角色known-output／Main位元保留。{count} inverse、175 no-init、371新＋同SHA550純契約、120 Source方法／38檔、凍結重建及20 SQL class原位元；0綁定factory／成本／角色／reload／IO／Main／game／MySQL。custom work/spawn、完整屬性與visibility/resource外控等全目標開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if fatalitydamage:
 marker='\n\n2026-10-25 Fatality原角色成對抽籤與明確空攻擊者續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`single/area pre-Hub binding-life-pose與原target/receiver快照，兩die在ordered pair／admission／target commitSelf重驗後一次抽籤並raw入原FIFO，舊sink handled-only。queued Plan只加detached NONE Proof，HP不依trigger活著；fatal接受沿原Member life seal。只有原target-guard／caster-null／Ref-null明確proof略過attacker-specific Calc，PC舊binding／NPC-effect／未知來源不轉NONE，外層145/149及Avatar保留。原deferred cosmetic帶detached root/subject/receiver到final加密前，resource GFX header相容，Context同步隔離finally還原；無新queue/worker/timer/TLS/SQL，原native catalog/RNG/predicate/Ctrl/WORLD保留。{count} inverse、179 no-init、32新＋同SHA921純契約、120 Source方法／38檔、凍結重建及20 SQL class原位元；0真HP／角色／wait／fatal-area／IO／Main／game／MySQL。native provisional catalog Source mapping與全材料外控AI／生命週期集中驗收仍開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if npcplayer:
 marker='\n\n2026-10-25 NPC對玩家原目標與防禦快照續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`原canonical tick在NPC lookup前固定target binding-life與room audience，validTick加舊目標gate；原ingress帶detached Members／Ref／defense snapshot，leave清精確scope，不持有Context或新TLS。6個真正NPC-PC sink走target actionlock／原admission-commitSelf／同FIFO，Source物理false/false與magic true/false；raw Guard原target生命／actual fatal seal保留，所有raw final結果另驗accepted-life防復活再死ABA，queued HP不另要求source NPC或master活著。既有doll proof-final frame用原scope召喚與receiver；原AI／RNG／WORLD fallback／成本Ctrl保持。{count} inverse、186 no-init、32新＋同SHA953純契約、122 Source方法／38檔、凍結重建及20 SQL class原位元；0真AI／HP／角色／doll／IO／Main／game／MySQL。上游NPC完整數值AI-factory／magic accepted plan、bot-ground-heal與全材料外控集中驗收仍開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if npcmagiceffects:
 marker='\n\n2026-10-25 NPC魔法接受後吸血魔消與追蹤HPBAR續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`兩magic原paired Plan凍結requested heal與erase，Source10/28常數依固定Source，不採native僅28compatibility；原accepted completion once，拒絕不執行，fatal可heal但不mutation，proven scope原aggregate heal略過。原NPC/owner/life/gen在admission-Hub-encounter下走2個原heal sibling long cap，dead不復活；只既有tracked weak viewers/control/range/ratio/life/receiver final IO，actual writeFrame後記cache，原completionFrame與area Context隔離finally還原。無queued Context/Scope/Definition/Frame/Receipt或新queue/worker/timer/TLS/SQL。{count} inverse＋2 sibling、194 no-init、33新＋同SHA985純契約、124 Source方法／40檔、凍結重建及20 SQL class原位元；0真HP/heal/角色/IO/Main/game/MySQL。ALT_ATKMSG與其他mutation／全數值AI-factory／全材料外控集中驗收仍開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if npcmagicdirect:
 marker='\n\n2026-10-25 NPC非正傷害精確魔消與零HP事件修正：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`fixed Source正傷害才gate acceptance，零damage不commitHP，非正傷害direct target mutation。原paired target lock/admission/commitSelf前置handled gate：已知攻擊零damage執行exact erase並return，不產生HP/After；signed negative捕捉lease於既有Plan，原owned HP返回後apply再finally清除。正值accepted路徑與不明metadata/非attack/物理/WORLD完整保留。Direct重驗原source-account-role-life/status/hub/target alive/OPEN/Main；recast/close/draining提交間替換不刪新狀態。無新field/queue/worker/timer/TLS/SQL/RNG/cost。{count}原方法inverse、其餘父archive全位元一致、196 no-init、37新＋同SHA1018純契約、124 Source方法/40檔、凍結重建及20 SQL class保留；0真HP/角色/IO/Main/game/MySQL。有號HP/治癒/不死系/mana-only/其他mutation/ALT_ATKMSG/上游數值AI-factory/全材料外控與集中驗收仍開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if signedrecovery:
 marker='\n\n2026-10-25 共用負傷害治癒生產端與原目標FIFO續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`實際NPC signed CalcTicket/NativePlayerVitals原已支援；缺口為CombatPlayerRecovery Source BATTLE負值仍直接HP/IO。本批private receive gate保留完整原fallback sibling；negative捕捉原bindings並繼承raw Member/physical Stamp/magic Snapshot life，target/caster ordered pair/OPEN admission/commitPair重驗，原signed calc/raw Plan入原target FIFO。queued Guard只target，來源退休換角不撤回已入列HP；target role-life/STOPPED/Main及final Frame沿原guard，DRAINING排空。無新field/queue/worker/timer/TLS/actor/SQL/RNG/cost/directHP/directIO。{count}原方法inverse＋1 fallback、其餘父archive全位元相同、201 no-init、31新＋同SHA1055純契約、420 exact compiled Source HP比較、124 Source方法/40檔與20 SQL class保持、凍結重建；0真HP/heal/pair/角色/IO/Main/game/MySQL。外層cast prewait target-life與全治癒/不死系/數值、NPC mutation/ALT_ATKMSG/全材料外控AI-factory/正式隔離合成及集中驗收仍開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if healingcast:
 marker='\n\n2026-10-25 三個主要治癒原參與者與cost前生命續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`確認單體/集體/大地祝福3個直接PC HP尚未經前批shared signed FIFO；本批三root原caster pre-Context binding-life-pose、原room participants在player-action wait前固定。單體原selected-PC resolve後cost/RNG前gate，兩target list逐一排除舊secondary/new entrant，保留原順序/競技/Ctrl/party/clan/range/type/LOS與視覺receivers。既有locks32次try-release/all-owned scope，actions先於OPEN admission，不持外層Hub等admission；原commitPair/finish/failure/exception清members/field。signed member沿同一原Member；無新queue/worker/timer/TLS/SQL/RNG/cost。{count} inverse＋3完整root sibling、203 no-init、32新＋同SHA1086純契約/420既有Source HP比較、124 Source方法/40檔/20 SQL class及凍結重建；0真target-wait/cost/RNG/heal/HP/IO/Main/game/MySQL。三直接PC HP與預覽輸出、first Hub discovery生命、全治癒不死系/controlled/NPC/bot/數值及全材料Source外控AI-factory/formal隔離合成集中驗收仍開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if healinghp:
 marker='\n\n2026-10-25 三主要PC治癒signed FIFO與接受後HP輸出續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`三healing stores在proven cast改送原requested signed HP，原fallback加法保留；zero不造HP，三reversal依compiled Source PC magic true/false。common display entry在既有Plan加Recovery原negative-input proof，不覆蓋NPC mutation；actual BATTLE completion對非正/非fatal recovery只真HP/ratio，unchanged/rejected無輸出，避免原markCombat/lastDamage污染，positive/fatal/unmarked/WORLD全留。target-only raw Guard跨caster退休/cast close保留，target role-life/Main/STOPPED沿原final Frame。22 send只filter精確PC preview，3 PC ratio攔截，HP/MP cost及NPC/companion/single bot ratio保留。無新HP lane/worker/timer/TLS/SQL；原成本RNG/status/競技Ctrl/原計算本體保留。{count} inverse、205 no-init、42新＋同SHA1118純契約/420既有Source HP比較、125 Source方法/42檔與20 SQL class保持、凍結重建；0真heal/HP/cost/RNG/IO/Main/game/MySQL。全治癒不死系/數值型別免疫/first Hub life/其他NPC-controlled-bot-periodic與Source外控AI-factory/formal隔離合成集中驗收仍開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if healingformula:
 marker='\n\n2026-10-25 治癒INT中文成長唯讀快照與Source整數公式續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`實際CombatDatabase.load傳入原readonly/non-autocommit conn，在NativeSkillTargetMetadata.read借同交易1個有界INT growth SELECT，原驗證成功才install immutable純值snapshot；專用治癒骰無cap10、INT<1 -2/127 clamp、缺欄/列依Source getter fallback，無新conn/tx/write。標記Source healing cast的shared INT/two statuses/three lawful與two die sums/one water multiplication匹配int wrap/negative shift；原RNG順序次數bounds/fixture/cost與WORLD本體inverse。{count} inverse、213 no-init、25新＋同SHA1160純契約/160 status/30 lawful/420既有HP比較、129 Source方法/43檔/20 SQL class與凍結重建；0真DB/INT reload/actor/heal/HP/role/IO/Main/game。其他effective stats/leverage/producer/undead/type/immunity/first Hub life與全材料外控NPC AI-factory/formal隔離合成集中驗收仍開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if healingtype:
 marker='\n\n2026-10-25 治癒實際受控型別與模板不死系續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`Source只對L1MonsterInstance讀undead，pet／summon／tamed不繼承模板1／3反轉免疫。修正單體＋共用group／Nature的3原讀取點，限原Source healing cast，使用actual selected record／actor final Kind；ARENA／null／unmarked／WORLD原本體保留。2原class、1無欄位無配置helper，無新lookup／SQL／RNG／HP／鎖／worker；原成本／status／HP／lifetime／Main與output inverse保留。{count} inverse、214 no-init、40新＋同SHA1185純契約、112型別signed比較（84受控Source＋28建模Monster）、131 Source方法／45檔、20 SQL class及凍結重建。0真actor／heal／HP／role／IO／Main／DB／game；自然factory／tower-door／bot、受控正治癒提交final output與週期、first-Hub生命、全材料Source及正式隔離合成集中驗收仍開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if controlledhealing:
 marker='\n\n2026-10-25 受控治癒原角色生命與接受後HPBAR續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`原cast try/finally內、玩家鎖等待前固定原owner Member與actor life／bag／placement／pose，1個bounded volatile Object carrier在close清除。3原正heal口驗原source-owner pair／admission／OPEN／Main後用原synchronized sibling，僅long-safe sum，原WORLD／ARENA／unmarked保留。接受changed HP排既有scope output，detached proof／原receiver沿generic marker與真正final frame，驗owner-actor-receiver生命與bag／pose／HP-max／hpbar／screen／DRAINING／STOPPED／Main；原預覽ratio按固定id攔截，其他cost／status／RNG／visual留原本體，無新FIFO／worker／TLS／SQL。{count} inverse、218 no-init、28新＋同SHA1225純契約、90 Source HP比較、133方法／45檔／20 SQL class及凍結重建。離線替身初始化原completion State，無null-state cleanup錯誤。0真bound actor／heal／HP／role／IO／Main／DB／game；full signed／owner-meter／週期／自然factory／tower-door-bot／first-Hub及全材料Source、正式隔離合成集中驗收仍開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if firstplayers:
 marker='\n\n2026-10-25 原Hub玩家名單生命先於綁定與治癒共用續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`原Context Hub內玩家binding點Source BATTLE先讀life再原capture一次；原caster pre-Hub／名單put順序／鎖與finally保留。1個bounded Object carrier，原constructor成功封immutable identity map、copy沿同snapshot；shared signed最末fresh fallback借原Member／missing-mismatch null，較早healing/raw/doll/magic優先序與unmarked/WORLD原體保留。{count} inverse／221 no-init／26新＋同SHA1253純契約、90／112／160／30／420繼承Source比較、133方法／45檔／20 SQL class與凍結重建，0真bound Hub／HP／role／IO／Main／DB／game。其他factory自行建證口與full first-Hub競態仍未完成，全材料Source／自然factory-AI／塔門bot週期／controlled owner-meter／正式隔離合成與集中驗收仍開放；不部署、不寫SQL-schema-WAL。\n','utf-8')
if playerfactories:
 marker='\n\n2026-10-25 原Context各玩家工廠與生命位置續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`pre-Hub caster先life後原binding，原magic snapshot前seal-inherit；5一般Member（raw roster/root/peer、doll、Fatality）、2 Actor、3 natural/material capture、shared Actor與material PC initial-token接原first proof，較早raw/cast/doll/magic pose與accepted-death token優先，missing明確stale不新接。原accepted-frame及WORLD、HP／Ctrl／cost／RNG／鎖／Main本體inverse；raw peer只原同步compute借Action，實際LambdaMetafactory形狀執行，無新欄位／queue／worker／TLS／registry／SQL。{count} inverse／227 no-init／34新＋同SHA1279純契約、133方法／45檔／20 SQL class及凍結重建；0真bound cast／HP／role／IO／Main／DB／game。剩餘constructor全盤點附收據，accepted／explicit-life／NPC tick-master／cube／其他ingress-pose及NPC discovery、全材料Source／factory-AI／正式隔離合成集中驗收仍開放；不部署、不寫SQL-schema-WAL。\n','utf-8')
if acceptedplayers:
 marker='\n\n2026-10-25 HP接受玩家值與原Actor位置續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`原acceptedState HP Queue vitalLock持有時Frame once封immutable values，target沿event targetLife／exact Guard acceptedLife與HP接受pose，source沿原Guard／Actor。2 raw accepted口複製原值與approved corpse token，5 doll/signed Actor轉Member留原pose；新增explicit-pose constructor完整原初始化inverse。NPC主目標及Fatality pre-Hub入口life／pose先於binding；unknown／WORLD原fallback，原HP drain／queue／completion-CAS／admission class位元、cost／RNG／Ctrl／鎖／Main保留。{count} inverse／230 no-init／28新＋同SHA1313純契約、133方法／45檔／20 SQL class及凍結重建；0真bound HP／role／IO／Main／DB／game。無Frame／Context／Plan／Member back-reference、新worker／SQL；unknown provenance／NPC viewer-master／cube／NPC discovery／全材料Source與正式隔離合成集中驗收仍開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if npcorigins:
 marker='\n\n2026-10-25 NPC名單與weak原主人Ref續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`Source-only origin sibling原Hub內名單／life-before-binding，沿原map與較早primary，無Hub內新player lock；WORLD／unloaded留完整原體。Ref/pet wait前主人life-binding-pose封immutable Ref sibling，1 final Object僅weak主人，原8 identity／equals-hash／ctor／NPC owner token空間保留；actual master ingress沿原證明、換角復活weak失效不新接。既有queued HP／Ref-live不讀新marker，core class位元保留，無source退場新撤回／queue／worker／TLS／registry／SQL。{count} inverse／233 no-init／27新＋同SHA1341純契約、133方法／45檔／20 SQL class與凍結重建；0真bound Ref／AI／HP／role／IO／Main／DB／game。birth-lifetime／manual Ref／NPC discovery-factory-AI／全材料Source與正式隔離合成集中驗收仍開放，不部署、不寫SQL-schema-WAL。\n','utf-8')
if npcfirstlife:
 marker='\n\n2026-10-25 NPC初始目標沿首次生命續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`controlled/raw/material/healing 6初始口沿原Context record與actor、自然epoch／controlled Ref.owner，主人沿舊玩家證明；unsupported stale。原accepted死亡seal、live重驗／Ctrl／鎖／HP／Main／WORLD保留。{count} inverse／234 no-init／30新＋同SHA1368純契約與同位元重建通過，133 Source方法／45檔與20 SQL class保留。真bound AI／HP／role／IO／Main／DB／game0；完整Source與factory／owner-birth/manual-Ref仍開放，程式全補齊再集中驗收；未部署、不寫Source／玩家SQL／schema／WAL。\n','utf-8')
if materialfirstlife:
 marker='\n\n2026-10-25 材料主目標主人接收端首次生命續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`negative29/152/153、mass-slow76、wind167及buff148/149四Aim的5初始life沿原Context較早玩家／Ref；11 valid／position／final-frame gate核對原source／primary／master／receiver，receiver保留observer／HP／移動資格。3 final attachment支持原battleCompanion或tamed；WORLD保留原list-only。{count} inverse／243 no-init／39新＋同SHA1398純契約／既有Source比較與同位元重建通過，133 Source方法／45檔、20 SQL class保留。真bound cast／status／HP／role／IO／Main／DB／game0，完整Source／58材料／factory-AI／owner-meter／正式隔離合成開放，程式全部完成後再集中驗收；未部署、不寫Source／玩家SQL／schema／WAL。\n','utf-8')
if areaplayerproof:
 marker='\n\n2026-10-25 共用範圍玩家名單與施法前來源生命續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`area原bound Context.player predicate借較早玩家／accepted corpse證明，保留原順序／duplicate／observer-dead-moving audience；2 pre-body scene沿原caster life-pose，不全域改Context或accepted回呼。原32 lock attempts／scope／admission／Hub-population／try-finally inverse保留。{count} inverse／245 no-init／25新＋同SHA1437純契約與同位元重建通過，133 Source方法／45檔、20 SQL class保留；新增本地方法參照定位census，非完成案數。真bound cast／HP／role／IO／Main／DB／game0，完整Source／58材料／其餘accepted-output method refs／factory-AI／正式隔離合成仍開放；程式全補齊後集中驗收，不部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。\n','utf-8')
if indirectplayerproof:
 marker='\n\n2026-10-25 間接名單與凍結接受目標生命續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`10 applicable indirect roster借原玩家／accepted freeze生命；剩1 classic-WORLD NPC physical members原位元保留，不擴WORLD。原Frame HP-vital-lock once-proof為原Event Linked freeze補無visual Plan accepted target，已知proof優先、unknown attacker不捏造；later deathLife不能重設accepted生命。freeze／cancellation 2初始life與相消final source-owner-actor-receiver／single-or-tamed附件接原proof，原HP／queue／CAS／source-retirement／locks／Ctrl／成本／status／Main／WORLD留原。{count} inverse／257 no-init／38新＋同SHA1462純契約／既有Source比較與同位元重建，133 Source方法／45檔、20 SQL class保留。真bound cast／HP／role／IO／Main／DB／game0；完整Source／58材料／freeze政策／factory-AI／正式隔離合成開放，程式完成後集中驗收；未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。\n','utf-8')
if npcfactorytype:
 marker='\n\n2026-10-25 NPC出生工廠型別與signed治療續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`3 actor各1 final Object／9 leaf ctor保存純量Source impl-undead；delegator不重取，controlled與transfer沿Pet／Summon。2 Source healing signed口沿出生型別，32原類別與Source factory/type-only oracle896比較，保留MonsterGuard非Monster、Tower-Door只擋治療方向、int wrap。{count} inverse／259 no-init／30新＋同SHA1500純契約／既有Source比較與同位元重建，73 Source檔／133 bounded參考及20 SQL class保留。原RNG／成本／HP／鎖／AI／生命／WORLD留原；不保留row／snapshot／registry。原完整factory／AI／restore-rollback／獨立守護塔／bot／periodic／免疫與全Source仍開放，真bound factory／AI／HP／role／IO／Main／DB／game0，程式完成後集中驗收；未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。\n','utf-8')
if npcfactorymemory:
 marker='\n\n2026-10-25 NPC型別記憶體快照與原還原續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`3原Snapshot各final transient Object＋immutable ctor sibling，原record components／identity留原。4 producer（含相消新目標Snapshot copy）與4 Quest-Time restore借primary型別／NPC id，不查目前模板／借暫時tame class，legacy未知不promote。已知controlled pet進化只調整copy id，不改birth型別／讀表。{count} inverse／264 no-init／30新＋同SHA1530純契約與896繼承Source型別比較及同位元重建，73 Source檔／133 bounded參考、20 SQL class保留。原HP／life／inventory／lock／WORLD完整inverse；無actor／Context／Frame／table registry，真bound restore／rollback／AI／HP／role／IO／Main／DB／game0。完整factory政策／AI／守護塔／bot／免疫／Source／正式隔離合成仍開放；程式全部完成後集中驗收，未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。\n','utf-8')
if npctargetfailure:
 marker='\n\n2026-10-25 NPC原型別免疫與失敗階段續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`同原row新增2個immutable出生旗標，原record identity及Snapshot記憶體還原留原；8個原phase ports涵蓋集體緩速／風枷／自然聖光與魔力吸取選定路徑。{count} inverse／266 no-init／37新＋同SHA1560純契約／4608 exact Source比較及凍結同位元重建，73 Source檔／133 bounded參考，20 SQL class保留。原PC／Ctrl／競技／成本重播／RNG／HP／生命／鎖／WORLD留原；真bound cost／RNG／HP／life／role／IO／Main／MySQL／game0。單體緩速／消魔早期RNG、mass-slow self-primary、其餘root／full factory-AI／58材料／完整Source與正式隔離合成未完成。全部程式完成後集中驗收；未部署／不連MySQL／不寫Source或玩家SQL／schema／WAL。本輪目標服務拒絕新目標，舊目標仍active，沒有虛報complete。\n','utf-8')
if negativephase:
 marker='\n\n2026-10-25 單體負面技能材料接受與機率階段續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`固定Source材料／動作先接受，runSkill後才扣HP／MP。29／152／153兩個原完整RNG區塊只在已證Source BATTLE移至原admit與cancelMeditation後、MP debit前，boolean local凍結分支；WORLD／unmarked原位置保留。6原flags接已知Quest-Time出生型別／IsErase，失敗不擲機率也不消目標狀態。原受控失敗／PC／Ctrl／競技／fixture／公式／成本回滾／鎖／生命／Main保留。{count} inverse／267 no-init／23新＋同SHA1597純契約／4608繼承Source比較與同位元重建，73 Source檔／133 bounded參考、20 SQL class保留；真bound material／admission／RNG／HP／life／role／IO／Main／DB／game0。完整Source／mass-slow self-primary／其餘技能／factory-AI／外控／正式隔離合成仍開放，程式完成後集中驗收；未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。\n','utf-8')
if massselftarget:
 marker='\n\n2026-10-25 集體緩速自選主目標與範圍清單續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`3原ports讓已證BATTLE的完整self-PC或原owned-controlled primary保留已選計算清單，不再全部empty。已選self-PC primary置前、secondary與重複維持原順序；缺席caster不新增，替換Npc record不採認。原PC／Ctrl／競技、phase／HP／invisibility／radius、NPC／controlled class failure、成本／RNG／成對鎖／生命／Main／WORLD保留。{count} inverse／268 no-init／26新＋同SHA1620純契約／4608繼承Source比較與凍結重建，73 Source檔／133 bounded參考、20 SQL class保留。self-specific Source oracle不覆蓋完整visibility／早期class-zone-ghost／主次目標，其他技能／factory-AI／外控／正式隔離合成仍開放；真bound material／RNG／mutation／HP／life／role／IO／Main／DB／game0。程式完成後集中驗收，未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。\n','utf-8')
if nonetargetrequest:
 marker='\n\n2026-10-25 Source none目標原施法者投影續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`45個原typed attack-request parser與4原post-Context／pre-Aim request沿原caster Binding／earlier Member pose，只改Source BATTLE的target=none；row／column／skill及原封包／Ctrl／指定目標不變，missing／closed／WORLD／unmarked不採新角色或生命。{count} inverse／269 no-init／22新＋同SHA1646純契約／4608繼承Source比較及同位元重建，73 Source檔／133 bounded參考、20 SQL class保留。固定Source PC empty target list仍可能消耗，NPC class veto須保原成本，不能前置免費return；其他封包kind／root自施header／geometry／class-ghost-zone-hidden-corpse／factory-AI／外控／正式隔離合成仍未完，真bound cast／selection／cost／RNG／HP／life／role／IO／Main／DB／game0。程式完成後集中驗收，未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。\n','utf-8')
if objecttargetrequest:
 marker='\n\n2026-10-25 Source物件封包角色與原物品ID分流續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`30原typed object parser投影none角色目標，2原object／1 Blind captured pre-Aim解析沿原caster證明；固定Source5個_itemobjid selector（73／100／21／12／107）保留原始物品ID，獨立Source none＋raw item／bookmark switch oracle通過。原row／column／skill／封包／一般目標／Ctrl／競技／消耗／RNG／生命／鎖／Main／WORLD保留，不新採角色／生命或inventory ownership。{count} inverse／270 no-init／39新＋同SHA1668純契約／4608繼承Source比較與凍結重建，73 Source檔／133 bounded參考、20 SQL class保留。75 parser ports不是75技能／58材料完成；其他packet kinds／root自施與geometry／NPC class-empty-PC成本／完整Source／factory-AI／外控／正式隔離合成仍開放。真bound inventory／cast／cost／RNG／HP／life／role／IO／Main／DB／game0，程式完成後集中驗收，未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。\n','utf-8')
if areaclassselection:
 marker='\n\n2026-10-25 範圍計算目標Source純類別排除續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`4原area roots／12原probability＋d100 target iterator接已知Source純類別排除，原accepted material/action支配入口（含exception CFG），audience-only iterator不改。Doll／unbreakable Door／primary natural-vs-PC-Pet-Summon／Guard／none-attack8類及tame-zombie-restore純類別比較91476組。原Snapshot birth proof須一致，legacy／同ID replacement不採新class；Sleep wrapper同物件、原order／duplicates保留。{count} inverse／272 no-init／30新＋同SHA1707純契約與同位元重建，73 Source檔／133 bounded參考、20 SQL class保留；原Ctrl／競技／成本／RNG／HP／生命／鎖／Main／WORLD留原，PC empty list不免消耗。這是class-only子集，不代表完整isTarget或全部roots，真bound cost／RNG／mutation／HP／life／role／IO／Main／DB／game0；完整Source／factory-AI／58材料／外控／正式隔離合成未完，程式完成後集中驗收，未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。\n','utf-8')
if damagehealingselection:
 marker='\n\n2026-10-25 傷害與治癒原計算清單類別排除續作：'
 text=(run/'previous-lifecycle.md').read_text('utf-8')
 doc.write_text(text.split(marker)[0]+marker+f'`{digest[:12]}…`4原damage／healing roots／13原post-MP per-target roll iterator用已知Source純類別排除（含exception CFG），原audience／HP-application／FIFO不動。原centered／targeted wrapper沿既有Snapshot與birth proof，legacy／同kind替換不借新class，已知Pet／Summon進化沿固定class。2 none資料ports（原targeted body request與原Action constructor primary）同用原caster證明，避免原NPC gate攔自施；原physical／pointed／args／constructor／discovery／lock順序保留。{count} inverse／274 no-init／33新＋同SHA1737純契約及91476繼承class-only比較與同位元重建，73 Source檔／133 bounded參考、20 SQL class保留。原Ctrl／競技／成本／RNG／HP／生命／Main／WORLD不變，真bound cost／RNG／mutation／HP／life／role／IO／Main／DB／game0；單體／guardian-tower／bot／完整Source／factory-AI／58材料／外控／正式隔離合成未完，程式完成後集中驗收，未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。\n','utf-8')
continuation=read(C);audit=read(auditpath)
for obj in [continuation,audit,proof]:freeze_old(obj)
fresh=dict(index=pin(index),runtime=runtime,native_runtime_qualified=False,complete=False)
continuation['source_program_update_progress']=fresh
continuation['source_damage_healing_selection_progress'if damagehealingselection else'source_area_class_selection_progress'if areaclassselection else'source_object_target_request_progress'if objecttargetrequest else'source_none_target_request_progress'if nonetargetrequest else'source_mass_self_target_progress'if massselftarget else'source_negative_phase_progress'if negativephase else'source_npc_target_failure_progress'if npctargetfailure else'source_npc_factory_memory_progress'if npcfactorymemory else'source_npc_factory_type_progress'if npcfactorytype else'source_indirect_player_proof_progress'if indirectplayerproof else'source_area_player_proof_progress'if areaplayerproof else'source_material_first_life_progress'if materialfirstlife else'source_npc_first_life_progress'if npcfirstlife else'source_npc_origins_progress'if npcorigins else'source_accepted_players_progress'if acceptedplayers else'source_player_factories_progress'if playerfactories else'source_first_players_progress'if firstplayers else'source_controlled_healing_progress'if controlledhealing else'source_healing_type_progress'if healingtype else'source_healing_formula_progress'if healingformula else'source_healing_HP_progress'if healinghp else'source_healing_cast_progress'if healingcast else'source_signed_recovery_progress'if signedrecovery else'source_npc_magic_direct_progress'if npcmagicdirect else'source_npc_magic_effects_progress'if npcmagiceffects else'source_npc_player_progress'if npcplayer else'source_fatality_damage_progress'if fatalitydamage else'source_cube_template_progress'if cubetemplate else'source_cube_visibility_progress'if cubevisibility else'source_cube_clock_progress'if cubeclock else'source_cube_relation_progress'if cuberelation else'source_cube_presentation_progress'if cubepresentation else'source_cube_remainder_progress'if cuberemainder else'source_cube_status_progress'if cubestatus else'source_cube_placement_progress'if cubeplacement else'source_ground_player_progress'if groundplayer else'source_ground_npc_progress'if groundnpc else'source_ground_placement_progress'if groundplacement else'source_ground_queued_progress'if groundqueued else'source_ground_origin_progress'if groundorigin else'source_wand_reaction_progress'if wandreaction else'source_npc_reaction_progress'if npcreaction else'source_hit_reaction_progress'if hitreaction else'source_ref_raw_npc_progress'if refrawnpc else'source_raw_npc_progress'if rawnpc else'source_poison_pc_progress'if poisonpc else'source_special_npc_progress'if specialnpc else'source_target_npc_progress'if targetnpc else'source_single_npc_progress'if singlenpc else'source_magic_npc_progress'if magicnpc else'source_physical_npc_progress'if physicalnpc else'source_poison_npc_progress'if poisonnpc else'source_damage_capacity_progress'if capacity else'source_cube_npc_queue_progress'if cubenpcqueue else'source_cube_npc_progress'if cubenpc else'source_cube_npc_lease_progress'if cubencplease else'source_cube_player_progress'if cubeplayer else'source_cube_origin_progress'if cube else'source_doll_passive_progress'if passive else'source_doll_defense_progress'if defense else'source_doll_slow_progress'if slow else'source_doll_output_progress'if output else'source_doll_skills_progress'if skills else'source_doll_action_progress'if action else'source_doll_npc_progress'if npc else'source_doll_accepted_progress'if accepted else'source_doll_commit_progress']=dict(**fresh,program_wired='Source-damage-healing-thirteen-post-MP-target-loops-none-primary-program-only'if damagehealingselection else'Source-area-class-only-twelve-admitted-target-iterators-program-only'if areaclassselection else'Source-object-none-original-caster-five-item-selector-split-program-only'if objecttargetrequest else'Source-none-original-caster-45-typed-parser-four-preAim-program-only'if nonetargetrequest else'mass-slow-self-primary-selected-secondary-original-order-program-only'if massselftarget else'single-negative-exact-original-RNG-after-material-action-before-MP-program-only'if negativephase else'NPC-class-failure-eight-selected-original-phase-ports-program-only'if npctargetfailure else'NPC-first-roster-weak-original-master-Ref-program-only'if npcorigins else'HP-owned-accepted-participant-values-Actor-pose-ingress-program-only'if acceptedplayers else'Context-player-factories-original-source-life-pose-program-only'if playerfactories else'first-Hub-player-life-signed-healing-consumers-program-only'if firstplayers else'controlled-healing-original-life-HP-accepted-meter-program-only'if controlledhealing else'controlled-healing-actual-target-type-program-only'if healingtype else'readonly-INT-growth-scoped-signed-healing-formula-program-only'if healingformula else'three-healing-PC-signed-FIFO-negative-input-completion-accepted-output-program-only'if healinghp else'healing-original-participants-pre-cost-life-pair-program-only'if healingcast else'shared-signed-recovery-original-target-FIFO-program-only'if signedrecovery else'NPC-magic-nonpositive-direct-erase-no-zero-HP-program-only'if npcmagicdirect else'NPC-magic-accepted-drain-exact-erase-meter-program-only'if npcmagiceffects else'NPC-PC-original-target-defense-and-raw-FIFO-program-only'if npcplayer else'Fatality-paired-explicit-null-attacker-raw-final-life-program-only'if fatalitydamage else'Source-cube-original-static-template-initial-presentation-program-only'if cubetemplate else'Source-cube-original-output-perceive-post-write-known-role-pairing-program-only'if cubevisibility else'Source-cube-actual-bootstrap-global1000-phase-original-Hub-clock-status-independent-program-only'if cubeclock else'Source-cube-original-Binding-self-relation-match-bounded-new-status-program-only'if cuberelation else'Source-cube-eight-config-GFX-original-relation-attribute-order-final-life-program-only'if cubepresentation else'Ignition-Harmony-original-status-MP-store-acceptance-natural-actor-final-life-program-only'if cuberemainder else'Quake-Shock-original-factory-primary-secondary-numeric-final-role-life-index-program-only'if cubestatus else'actual-cube-material-factory-prewait-binding-live-entity-Source-effect-scan-program-only'if cubeplacement else'PC-firewall-Source-SKD3-effective-stats-original-pair-raw-HP-final-reaction-program-only'if groundplayer else'natural-NPC-firewall-original-tick-raw-FIFO-and-detached-final-scene-program-only'if groundnpc else'actual-material-two-copy-cost-install-original-prewait-binding-live-engine-Entity-program-only'if groundplacement else'ground-detached-queued-scene-actual-HP-and-accepted-life-final-Frame-program-only'if groundqueued else'original-ground-lease-pulse-tick-prewait-PC-raw-and-final-reaction-program-only-queued-scene-proof-open'if groundorigin else'wand-natural-NPC-original-raw-Action-typed-gfx-existing-final-output-program-only'if wandreaction else'NPC-counter-reaction-two-send-original-Ref-live-typed-gfx-program-only'if npcreaction else'Source-hit-reaction-owned-readonly-startup-existing-NPC-consumers-program-only'if hitreaction else'accepted-natural-Ref-feedback-original-raw-FIFO-program-only'if refrawnpc else'natural-NPC-raw-wand-Mortal-Joy-original-FIFO-source-proof-program-only'if rawnpc else'scoped-PC-DAMAGE-poison-original-clock-without-attacker-peer-program-only'if poisonpc else'natural-NPC-Mind-Bone-Arm-original-pre-HP-native-results-and-single-FIFO-program-only'if specialnpc else'natural-NPC-directional-freeze-shock-actual-accepted-target-plan-final-frame-program-only'if targetnpc else'natural-NPC-single-drain-confusion-thunder-accepted-before-wave-original-heal-final-receiver-program-only'if singlenpc else'natural-NPC-area-magic-frozen-cast-shared-FIFO-accepted-exact-erase-program-only'if magicnpc else'natural-NPC-physical-scoped-doll-original-FIFO-accepted-proc-frozen-audience-program-only'if physicalnpc else'natural-NPC-poison-original-attacker-target-FIFO-accepted-final-receiver-program-only'if poisonnpc else'queued-PC-NPC-damage-only-application-ledger-release-shutdown-fence-program-only'if capacity else'cube-NPC-original-target-FIFO-actual-claim-shared-CAS-program-only-all-producers-open'if cubenpcqueue else'natural-NPC-actual-effect-HP-master-final-receiver-program-only-Source-FIFO-open'if cubenpc else'natural-NPC-exact-original-cube-lease-retirement-only-HP-open'if cubencplease else'cube-PC-actual-effect-raw-HP-accepted-final-receiver-program-only'if cubeplayer else'cube-original-effect-exact-status-provenance-only-HP-open'if cube else'Context-original-actor-passive-powers-selection-program-only'if passive else'Context-original-defender-summon-accepted-final-frame-program-only'if defense else'Source-doll29-duration-innate-haste-epoch-program-only'if slow else'final-frame-and-accepted-speed-proof-role-reset'if output else'generic-SKILL-metadata-slow-proc-spr-partial'if skills else'pre-action-ingress-and-detached-summon-proof'if action else'natural-controlled-NPC-accepted-callback'if npc else'PC-accepted-physical-callback'if accepted else'partial-typed-HP-consumers',all_doll_paths_complete=False)
continuation['source_completion_work']=dict(inventory=pin(D/'source-completion-inventory-current.json'),work=pin(work),goal=inv['goal'],complete=False)
continuation['source_next_producer_contracts']=dict(index=pin(D/'source-next-producer-contracts-current.json'),runtime=runtime,program_wired='partial-Source-damage-healing-target-loops-none-primary-and-prior-phases'if damagehealingselection else'partial-Source-area-class-selection-twelve-target-iterators-and-prior-phases'if areaclassselection else'partial-Source-object-none-item-selector-split-and-prior-phases'if objecttargetrequest else'partial-Source-none-original-caster-request-and-prior-phases'if nonetargetrequest else'partial-mass-slow-self-primary-selected-secondary-and-prior-phases'if massselftarget else'partial-single-negative-Source-material-action-before-RNG-and-prior-phases'if negativephase else'partial-NPC-target-class-failure-flags-eight-selected-phase-ports-and-prior-phases'if npctargetfailure else'partial-NPC-factory-type-original-Snapshot-memory-restore-and-prior-phases'if npcfactorymemory else'partial-NPC-birth-factory-type-two-Source-signed-healing-ports-and-prior-phases'if npcfactorytype else'partial-indirect-roster-linked-freeze-accepted-target-cancellation-final-life-and-prior-phases'if indirectplayerproof else'partial-area-original-roster-prebody-source-life-and-prior-phases'if areaplayerproof else'partial-material-original-primary-master-receiver-life-controlled-attachment-and-prior-phases'if materialfirstlife else'partial-NPC-initial-target-original-discovery-life-and-prior-phases'if npcfirstlife else'partial-NPC-first-roster-weak-original-master-Ref-and-prior-phases'if npcorigins else'partial-HP-owned-accepted-participant-values-Actor-pose-ingress-and-prior-phases'if acceptedplayers else'partial-Context-player-factories-original-source-life-pose-and-prior-phases'if playerfactories else'partial-first-Hub-player-life-signed-healing-consumers-and-prior-phases'if firstplayers else'partial-controlled-healing-original-life-HP-accepted-meter-and-prior-phases'if controlledhealing else'partial-controlled-healing-actual-target-type-and-prior-phases'if healingtype else'partial-readonly-INT-growth-scoped-healing-formula-and-prior-phases'if healingformula else'partial-three-healing-PC-signed-FIFO-accepted-output-and-prior-phases'if healinghp else'partial-healing-original-participants-pre-cost-life-pair-and-prior-phases'if healingcast else'partial-shared-signed-recovery-original-target-FIFO-and-prior-phases'if signedrecovery else'partial-NPC-magic-nonpositive-direct-erase-no-zero-HP-and-prior-phases'if npcmagicdirect else'partial-NPC-magic-accepted-drain-exact-erase-meter-and-prior-phases'if npcmagiceffects else'partial-NPC-PC-original-target-defense-raw-FIFO-and-prior-phases'if npcplayer else'partial-Fatality-paired-explicit-null-attacker-raw-and-prior-phases'if fatalitydamage else'partial-Source-cube-original-static-template-initial-presentation-and-prior-phases'if cubetemplate else'partial-Source-cube-output-perceive-known-role-pair-and-prior-phases'if cubevisibility else'partial-Source-cube-actual-bootstrap-global1000-phase-and-prior-phases'if cubeclock else'partial-Source-cube-original-role-identity-self-match-new-status-and-prior-phases'if cuberelation else'partial-Source-cube-config-GFX-original-relation-output-order-and-prior-phases'if cubepresentation else'partial-Ignition-Harmony-status-MP-acceptance-final-life-and-prior-Source-phases'if cuberemainder else'partial-Quake-Shock-material-status-secondary-life-and-final-output'if cubestatus else'partial-actual-cube-material-entity-factory-and-Source-effect-scan'if cubeplacement else'partial-PC-firewall-SKD3-and-prior-natural-NPC-Source-phases'if groundplayer else'partial-natural-NPC-firewall-raw-FIFO-and-prior-Source-phases'if groundnpc else'partial-actual-material-ground-staged-factory-and-prior-HP-Source-phases'if groundplacement else'partial-ground-detached-queued-scene-HP-frame-and-prior-Source-phases'if groundqueued else'partial-original-ground-origin-PC-raw-and-prior-HP-control-Source-phases'if groundorigin else'partial-wand-natural-NPC-reaction-and-prior-HP-control-Source-phases'if wandreaction else'partial-NPC-counter-reaction-original-Ref-and-prior-HP-control-Source-phases'if npcreaction else'partial-hit-reaction-readonly-control-and-prior-HP-Source-phases'if hitreaction else'partial-accepted-Ref-feedback-and-raw-NPC-FIFO-Source-phases'if refrawnpc else'partial-raw-NPC-wand-feedback-FIFO-and-prior-Source-phases'if rawnpc else'partial-scoped-PC-poison-peer-and-original-NPC-Source-phases-FIFO'if poisonpc else'partial-NPC-Mind-Bone-Arm-original-pre-HP-and-directional-single-area-FIFO'if specialnpc else'partial-NPC-directional-freeze-shock-single-area-target-plans-and-existing-FIFO'if targetnpc else'partial-NPC-single-area-accepted-target-effects-and-existing-shared-FIFO'if singlenpc else'partial-NPC-area-magic-accepted-erase-and-physical-doll-poison-cube-proc-FIFO'if magicnpc else'partial-NPC-physical-doll-poison-cube-proc-FIFO-and-queued-damage-domain'if physicalnpc else'partial-NPC-poison-cube-proc-FIFO-and-queued-damage-domain'if poisonnpc else'partial-HP-FIFO-consumers-and-queued-damage-capacity-domain'if capacity else'partial-doll-cube-PC-NPC-HP-FIFO-consumers'if cubenpcqueue else'partial-doll-and-cube-PC-natural-NPC-HP-consumers'if cubenpc else'partial-doll-only',complete=False)
audit['source_program_update_progress']=dict(**fresh,remaining=remaining);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
for ref in v['additional_protected']:verify(ref)
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=runtime,original_methods_inverted=count,new_game_runtime_cases=0,new_database_connections=0)))
