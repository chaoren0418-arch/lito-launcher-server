"""Pin the actual candidate and enumerate open work, without game/SQL startup.

Historical catalog facts are retained as historical facts; no semantic or runtime
pass is inferred from a model, method name, wrapper or static edge.
"""
from pathlib import Path
from datetime import datetime, timezone
import csv, hashlib, json, os, shutil, subprocess, sys, re

R=Path(__file__).resolve().parent; D=R/'build'; S=R.parents[2]
read=lambda p:json.loads(p.read_text('utf-8'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def checked(p):
 assert sha(Path(p['path']))==p['sha256'],p['path']
 return Path(p['path'])
def receipt(name):
 p=D/(name+'-current.json'); a=read(p); m=read(checked(a['manifest']))
 return a,m
def table(p):
 with checked(p).open(encoding='utf-8',newline='')as f:return list(csv.DictReader(f,delimiter='\t'))

candidate,cm=receipt(sys.argv[1] if len(sys.argv)>1 else 'source-raw-producer-build'); jar=checked(candidate['runtime'])
progress,pm=receipt('source-program-update-progress');assert progress['runtime']==candidate['runtime'] or cm.get('base')==progress['runtime'] or ('build'in pm and read(checked(pm['build'])).get('base')==cm.get('base'))
for p in pm['protected']:checked(p)
catalog,cat=receipt('material-catalog-audit'); catalog_rows=cat['ids'];checked(cat['catalog'])
paths,sp=receipt('all-targeted-source-paths'); rows=table(sp['matrix'])
assert len(rows)==807 and len(catalog_rows)==58
run=D/'source-completion-inventory'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True)
for p in [Path(__file__),R/'SourceCompletionCensus.java']:shutil.copyfile(p,run/p.name)
source_config=Path('D:/lineage開發/1-伺服器/src/com/lineage/config');source_inputs=[];source_keys=[]
for name in ['ConfigSkill','ConfigPcDamage','ConfigCharSetting','ConfigOther','ConfigAlt','ConfigDe','ConfigEnchant','ConfigGeneration']:
 p=source_config/(name+'.java');source_inputs.append(pin(p));text=p.read_text('utf-8')
 for match in re.finditer(r'(?:getProperty\(\s*|_get(?:Int|Double|Boolean)\(\s*p\s*,\s*)"([^"\r\n]+)"',text):source_keys.append(dict(source_file=name,line=text.count('\n',0,match.start())+1,key=match[1]))
(run/'property-keys.txt').write_text('\n'.join(sorted({r['key']for r in source_keys}))+'\n','utf-8')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
for p in cm['asm']:checked(p)
cp=';'.join(map(str,[run/'tools',*[x['path']for x in cm['asm']]]))
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
with (run/'audit.log').open('w',encoding='utf-8')as log:
 for args in [[J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',run/'tools',run/'SourceCompletionCensus.java'],[J/'java.exe','-Xverify:all','-cp',cp,'SourceCompletionCensus',jar,run/'call-sites.tsv',run/'methods.tsv',run/'property-keys.txt',run/'property-literals.tsv']]:
  subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=180)
with (run/'methods.tsv').open(encoding='utf-8',newline='')as f:methods=list(csv.DictReader(f,delimiter='\t'))
with (run/'call-sites.tsv').open(encoding='utf-8',newline='')as f:edges=list(csv.DictReader(f,delimiter='\t'))
with (run/'property-literals.tsv').open(encoding='utf-8',newline='')as f:literals=list(csv.DictReader(f,delimiter='\t'))
controls=[]
for row in source_keys:
 located=sorted({r['owner'].split('/')[-1]+'.'+r['method']for r in literals if r['Source_property_key']==row['key']})
 controls.append(dict(**row,candidate_literal_locators=';'.join(located),status='literal-reference-needs-consumer-review'if located else'no-exact-key-literal-needs-alias-or-missing-consumer-review',semantic_qualified=False))
with (run/'controls.tsv').open('w',encoding='utf-8',newline='')as f:
 w=csv.DictWriter(f,fieldnames=list(controls[0]),delimiter='\t');w.writeheader();w.writerows(controls)
hp_names={'takePlayerDamage','takeClassicWorldPlayerDamage','applyQuestMonsterDamage','applyTimeAttackActorDamage','applyBattleCompanionFeedbackDamage','poisonDamage','applyDamage'}
hp=[r for r in edges if r['callee']in hp_names and r['callee_owner'].split('/')[-1]in ['ClientSession','BattleCompanion','BattleQuestEncounter','BattleTimeAttackEncounter','BattleQuestEncounter$Monster','BattleTimeAttackEncounter$Monster']]
with (run/'hp-producers.tsv').open('w',encoding='utf-8',newline='')as f:
 w=csv.DictWriter(f,fieldnames=list(hp[0]),delimiter='\t');w.writeheader();w.writerows(hp)
names={x['method'] for x in methods if x['owner']=='local/lito381/server/ClientSession'}
matrix=[]
for row in rows:
 roots=[x.strip() for x in row['native_entry_locators'].strip('[]').split(',')if x.strip()]
 matrix.append(dict(skill_id=int(row['skill_id']),name=row['name'],material=int(row['skill_id'])in catalog_rows,Source_symbols=row['Source_symbols'],Source_executors=row['Source_registry_executors'],historical_native_models=row['native_models'],historical_roots=';'.join(roots),current_method_names_located=';'.join(x for x in roots if x in names),status='program-and-Source-consumer-review-open'if roots else'static-row-needs-applicability-classification',same_SHA_semantics_qualified=False,actual_cursor_qualified=False))
with (run/'skills.tsv').open('w',encoding='utf-8',newline='')as f:
 w=csv.DictWriter(f,fieldnames=list(matrix[0]),delimiter='\t');w.writeheader();w.writerows(matrix)
groups=[
 ('P0-HP','NPC poison / equipment / cube / doll / periodic damage','implementation-open','Enumerate actual HP producers; preserve Source raw vs ordinary accepted-hit figures; freeze actual owners before waits and revalidate at commit/final receiver.'),
 ('P0-MATERIAL','58 reagent-positive rows and all applicable Source skill models','implementation-and-semantic-review-open','Per skill: Source class/target/normal/Ctrl/primary/secondary/range/LOS/immunity, costs/probability/rollback and pre/post-commit lifecycle. Historical 34 is not a complete denominator.'),
 ('P0-AI','NPC factory/AI, population and hit reaction controls','implementation-review-open','Trace configuration readers through actual producer/consumer paths, including natural and controlled actors; preserve product BATTLE restrictions.'),
 ('P0-LIFE','Cross-role identity, admission, HP/MP/equipment, effects and cleanup','implementation-and-concentrated-validation-open','Actual source/account/role/actor identity; pair locks, death/revival, role swap, scene/pose, DRAINING/STOPPED/Main, receiver IO and bounded memory.'),
 ('P1-COMPOSE','Formal 0.6.71 isolated composition','pending-implementation-completion','Preserve formal resources/icons/special-load DLL/GM7200, existing safety and SQL classes; experimental ancestor cannot replace formal as a whole.'),
 ('P1-NATIVE','Same SHA full backend regression','deferred-until-program-complete','Old SHA case counts never qualify this candidate. No new game fixtures requested at this program-update stage.'),
 ('P1-SQL','Real equipped-character Chinese MySQL readonly reload','deferred-by-user','Keep b85 static/empty-query evidence scoped; actual equipped row mapping remains open, no migration/write.'),
 ('P1-CURSOR','Source26 no-Ctrl / full cursor and foreground acceptance','deferred-until-program-complete','Only BATTLE product; real cursor, packet path, multiwindow/lobby after all updates. Synthetic self packets are insufficient.')]
v=dict(status='inventoried-actual-candidate-open-work-no-completion-claims',runtime=candidate['runtime'],goal=dict(request='reset-and-complete-missing-work',system_goal_reset='rejected-unfinished-goal-exists',objective='完整補齊 LITO 競技候選之材料技能、Source 外控與跨角色生命週期；先更新程式與可重現檢查，之後集中同 SHA 後端、唯讀 MySQL 與實機游標驗收；禁止部署及 Source/玩家 SQL/WAL 寫入。'),prior_program_progress=progress['manifest'],historical_material_catalog=cat['catalog'],historical_source_matrix=sp['matrix'],observations=dict(static_rows=807,reagent_positive_rows=58,historical_native_models=180,historical_Source_symbol_rows=185,historical_parser_paths=234,current_archive_methods=len(methods),current_archive_local_call_sites=len(edges),current_direct_HP_call_sites=len(hp),Source_configuration_literal_key_rows=len(controls),Source_configuration_exact_literal_located_rows=sum(bool(r['candidate_literal_locators'])for r in controls),same_SHA_qualified_game_cases=0),Source_configuration_scope='Eight combat-related Source config classes, literal getProperty and _getInt/_getDouble/_getBoolean(p,key,default) calls only. Key presence does not prove a reader, consumer or semantic equivalence. SQL controls and computed keys require separate review.',Source_configuration_inputs=source_inputs,queue=[dict(id=i,scope=s,status=t,completion_gate=g)for i,s,t,g in groups],artifacts=[pin(run/n)for n in ['skills.tsv','controls.tsv','hp-producers.tsv','methods.tsv','call-sites.tsv','property-keys.txt','property-literals.tsv','audit.log','SourceCompletionCensus.java',Path(__file__).name]],protected=pm['protected'],new_SQL_connections=0,created_Main=False,created_Hub=False,game_class_initialization=False,full_goal_achieved=False,qualified_for_release=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
goalpath=D/'source-completion-goal-reset-current.json'
if not goalpath.exists():goalpath=D/'source-completion-goal-current.json'
if goalpath.exists():
 g=read(goalpath)
 if g.get('status')=='active':v['goal']=dict(system_goal_reset=g.get('system_goal_reset','created-active'),status='active',objective=g['goal']['objective'],receipt=pin(goalpath))
for p in source_inputs:checked(p)
for p in pm['protected']:checked(p)
write(run/'manifest.json',v);write(D/'source-completion-inventory-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'],manifest=pin(run/'manifest.json'))))
