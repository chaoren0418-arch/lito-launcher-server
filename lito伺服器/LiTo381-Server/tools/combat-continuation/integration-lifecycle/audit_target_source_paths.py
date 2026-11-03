"""Join all807 rows to exact Source symbols/executors and candidate classfile parser paths. No startup/SQL."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,subprocess,os,csv,re,collections
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';T=Path('D:/lineage開發/1-伺服器/src/com/lineage/server')
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
seed=read(D/'all-targeted-skill-inventory-current.json');m=read(Path(seed['manifest']['path']));matrix=Path(m['matrix']['path']);assert sha(matrix)==m['matrix']['sha256'];build=read(D/'source-wind-target-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];bm=read(Path(build['manifest']['path']))
run=D/'all-targeted-source-paths'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for p in [Path(__file__),R/'TargetParserCallInventory.java']:shutil.copyfile(p,inputs/p.name)
sources=[];frozen=run/'Source';frozen.mkdir()
paths=[T/'model/skill/L1SkillId.java',T/'model/skill/L1SkillMode.java',T/'model/skill/L1SkillUse.java',T/'clientpackets/C_UseSkill.java',T/'templates/L1Skills.java']
for p in paths:
 if p.exists():q=frozen/p.name;shutil.copyfile(p,q);sources.append(dict(original=pin(p),frozen=pin(q)))
ids=(T/'model/skill/L1SkillId.java').read_text('utf-8');use=(T/'model/skill/L1SkillUse.java').read_text('utf-8');registry=(T/'model/skill/L1SkillMode.java').read_text('utf-8')
symbols=collections.defaultdict(list)
for symbol,value in re.findall(r'(?:public|private)\s+static\s+final\s+int\s+(\w+)\s*=\s*(\d+)\s*;',ids):symbols[int(value)].append(symbol)
executors={symbol:cls for symbol,cls in re.findall(r'_skillMode\.put\(\s*(\w+)\s*,\s*new\s+(\w+)\s*\(',registry)}
for cls in sorted(set(executors.values())):
 p=T/'model/skill/skillmode'/(cls+'.java');assert p.exists(),cls;q=frozen/(cls+'.java');shutil.copyfile(p,q);sources.append(dict(original=pin(p),frozen=pin(q)))
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');tcp=';'.join(map(str,[run/'tools',*[x['path']for x in bm['asm']]]));log=run/'audit.log';env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8')
v=dict(status='running',runtime=build['runtime'],static_inventory=seed['manifest'],inputs=[pin(p)for p in inputs.iterdir()],Source=sources,created_Main=False,created_Hub=False,created_SQL_connection=False,native_runtime_cases=0,all_skill_target_coverage_complete=False,all_target_intent_policy_qualified=False,actual_client_cursor_verified=False,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=240)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',run/'tools',inputs/'TargetParserCallInventory.java']);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'TargetParserCallInventory',jar,matrix,run/'parser-paths.tsv'])
 with (run/'parser-paths.tsv').open(encoding='utf-8',newline='')as f:parserrows=list(csv.DictReader(f,delimiter='\t'))
 parsers=collections.defaultdict(set)
 for row in parserrows:
  if row['parser']!='<none>':parsers[row['root']].add(row['parser'])
 with matrix.open(encoding='utf-8',newline='')as f:rows=list(csv.DictReader(f,delimiter='\t'))
 for row in rows:
  skill=int(row['skill_id']);syms=symbols[skill];classes=sorted({executors[x]for x in syms if x in executors});roots=[x.strip()for x in row['native_entry_locators'].strip('[]').split(',')if x.strip()]
  row['Source_symbols']=';'.join(syms);row['Source_registry_executors']=';'.join(classes);row['Source_use_symbol_lines']=';'.join(str(n)for n,line in enumerate(use.splitlines(),1)if any(re.search(r'\b'+re.escape(sym)+r'\b',line)for sym in syms))
  row['native_static_parser_candidates']=';'.join(sorted(set().union(*(parsers[root]for root in roots))))
  row['Source_special_target_review']='corpse-only:Source declared resurrection/zombie ids; kind/death validation still required'if any(x in syms for x in ['CREATE_ZOMBIE','RESURRECTION','GREATER_RESURRECTION','CALL_OF_NATURE'])else'item-selector/coordinate/me target requires its own request parser'if row['target']in ['item','none','buff','attack']and int(row['target_to'])==0 else'per-skill Source isTarget/isTargetCalc/isTargetFailure + executor review pending'
  row['path_evidence_limit']='static references only; aliases, skill-id branches, reflection and actual dispatch acceptance unproved; no cursor claim'
 assert len(rows)==807 and len({x['skill_id']for x in rows})==807
 with (run/'target-matrix.tsv').open('w',encoding='utf-8',newline='')as f:w=csv.DictWriter(f,fieldnames=list(rows[0]),delimiter='\t');w.writeheader();w.writerows(rows)
 v.update(status='mapped807-Source-symbol-executor-and-static-native-parser-candidates-not-target-qualification',observations=dict(rows=len(rows),rows_with_Source_symbol=sum(bool(x['Source_symbols'])for x in rows),rows_with_Source_executor=sum(bool(x['Source_registry_executors'])for x in rows),rows_with_native_static_parser_candidate=sum(bool(x['native_static_parser_candidates'])for x in rows),parser_path_rows=len(parserrows)),matrix=pin(run/'target-matrix.tsv'),parser_paths=pin(run/'parser-paths.tsv'))
 for row in sources:assert sha(Path(row['original']['path']))==row['original']['sha256']
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'all-targeted-source-paths-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
