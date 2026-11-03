"""Bound actual current getter/HP consumers and explicit native null arguments; no gameplay qualification."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,csv,subprocess,os,re,shutil
R=Path(__file__).resolve().parent;D=R/'build';P='local/lito381/server/'
read=lambda p:json.loads(Path(p).read_text('utf-8'));sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest();pin=lambda p:dict(path=str(Path(p).resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def checked(ref):assert sha(ref['path'])==ref['sha256'],ref['path'];return Path(ref['path'])
inventory=read(D/'source-completion-inventory-current.json');im=read(checked(inventory['manifest']));jar=checked(inventory['runtime']);calls=next(x for x in im['artifacts']if Path(x['path']).name=='call-sites.tsv')
with checked(calls).open(encoding='utf-8',newline='')as f:rows=list(csv.DictReader(f,delimiter='\t'))
getter=[r for r in rows if r['callee_owner']==P+'ClientSession'and r['callee']=='activeMagicDollPowers']
hp=[r for r in rows if r['callee_owner']==P+'ClientSession'and r['callee']=='takePlayerDamage']
by_method={}
for row in rows:by_method.setdefault((row['owner'],row['method'],row['descriptor']),[]).append(row)
run=D/'source-doll-consumers'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);shutil.copyfile(__file__,run/Path(__file__).name)
def table(p,values):
 with p.open('w',encoding='utf-8',newline='')as f:
  w=csv.DictWriter(f,fieldnames=list(values[0]),delimiter='\t');w.writeheader();w.writerows(values)
table(run/'common-getter-consumers.tsv',getter)
consumers=[]
for row in hp:
 method=by_method[(row['owner'],row['method'],row['descriptor'])]
 entries=sorted({r['callee_owner']+'.'+r['callee']for r in method if r['callee_owner'].startswith(P+'Native')and r['callee'].startswith(('enter','owned','run','begin'))})
 consumers.append(dict(**row,local_native_ingress_edges=';'.join(entries),context_and_first_wait_coverage='needs-actual-upstream-and-owned-lock-review',semantic_qualified=False))
table(run/'direct-player-HP-consumers.tsv',consumers)
javap=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin/javap.exe')
result=subprocess.run([str(javap),'-p','-c','-cp',str(jar),P.replace('/','.')+'ClientSession'],capture_output=True,check=True,timeout=90,env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -Dfile.encoding=UTF-8'))
text=result.stdout.decode('utf-8').replace('\r','');parts=re.split(r'(?=^  [^\s].*\)[^;]*;\s*$)',text,flags=re.M);excerpts=[]
for name in ['applyFatalityAreaDamage','applyFatalitySingleDamage']:
 found=[p for p in parts if re.search(r'\b'+name+r'\(',p.split('\n')[0])];assert len(found)==1,name
 lines=[line for line in found[0].splitlines()if re.match(r'\s*\d+:',line)];sites=[i for i,line in enumerate(lines)if '// Method takePlayerDamage:'in line];assert len(sites)==1 and 'aconst_null'in lines[sites[0]-1],name
 excerpts.append(dict(owner=P+'ClientSession',method=name,explicit_native_Java_caster_argument='aconst_null',original_native_NPC_or_effect_ingress='not-proven-by-this-local-argument',bytecode=found[0]))
write(run/'actual-native-null-call-excerpts.json',excerpts)
references=[Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model/Instance/L1PcInstance.java'),Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model/L1Cube.java'),R.parents[2]/'src/main/java/local/lito381/server/BattleDeathmatchFatalityCatalog.java']
for p in references:shutil.copyfile(p,run/('reference-'+p.name))
v=dict(status='inventoried-actual-doll-getter-and-direct-incoming-roots-no-runtime-coverage-claim',runtime=inventory['runtime'],inventory=inventory['manifest'],input=pin(run/Path(__file__).name),Source_and_native_references=[dict(original=pin(p),frozen=pin(run/('reference-'+p.name)))for p in references],artifacts=[pin(run/n)for n in ['common-getter-consumers.tsv','direct-player-HP-consumers.tsv','actual-native-null-call-excerpts.json']],common_getter_static_calls=len(getter),common_getter_static_methods=len({(r['owner'],r['method'],r['descriptor'])for r in getter}),direct_HP_static_calls=len(hp),direct_HP_static_methods=len({(r['owner'],r['method'],r['descriptor'])for r in hp}),findings=[
 'All common getter call sites are enumerated from this exact archive, not from stale ClientSession source. The Source passive patch covers the existing loaded-BATTLE Context and original-actor-lock domain; static getter presence does not prove every caller enters that domain.',
 'Direct HP callers include forwarding overloads and retained legacy/unloaded siblings. A missing local enter edge is not proof of a missing upstream Context; chase the actual upstream and owned action locks separately. Reflection and generic helper consumers are not fully counted by this direct-call table.',
 'The two native Fatality damage methods explicitly push Java null for the caster. This proves that argument only, not absence of an ambient original NativeCombatAttacker ingress. Source receiveDamageInternal gates defense/incoming-hit Calc on actual attacker!=null; cube supplies its effect NPC, so null, a player owner and an effect NPC cannot be collapsed into one source type.',
 'Retained native Fatality category/Heal weights are documented as provisional compatibility policy by its existing catalog. Preserve formal native behavior and classify its Source applicability rather than inventing a fixed-Source floor-glyph implementation.'
],new_game_runtime_cases=0,new_database_connections=0,all_incoming_producers_qualified=False,all_Source_consumers_wired=False,full_goal_achieved=False,qualified_for_release=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
write(run/'manifest.json',v);write(D/'source-doll-consumers-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],common_getter_calls=len(getter),common_getter_methods=v['common_getter_static_methods'],direct_HP_calls=len(hp),direct_HP_methods=v['direct_HP_static_methods'],runtime=v['runtime']['sha256'])))
