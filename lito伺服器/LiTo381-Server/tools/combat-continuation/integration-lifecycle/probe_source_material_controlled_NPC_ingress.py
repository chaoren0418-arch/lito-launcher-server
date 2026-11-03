"""Read frozen material/model inventory and execute bounded native ingress on strict memory fixtures."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,csv,io,re,subprocess,shutil,os,sys,collections
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def checked(ref):assert pin(ref['path'])==ref;return Path(ref['path'])
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
index=read(D/'source-program-update-progress-current.json');base=checked(index['runtime']);progress=read(checked(index['manifest']));build=read(checked(progress['build']))
inv_index=read(D/'source-completion-inventory-current.json');inv=read(checked(inv_index['manifest']));assert inv_index['runtime']==index['runtime'];skill_ref=next(x for x in inv['artifacts']if Path(x['path']).name=='skills.tsv')
cat_index=read(D/'material-catalog-audit-current.json');cat=read(checked(cat_index['manifest']));catalog=checked(cat['catalog']);costs={int(x[1]):x[2:5]for line in catalog.read_text('utf-8').splitlines()if (x:=line.split('\t'))[0]=='MATERIAL'}
paths_index=read(D/'all-targeted-source-paths-current.json');paths=read(checked(paths_index['manifest']));layout_basis=paths['matrix'];layout_rows={int(r['skill_id']):r for r in csv.DictReader(io.StringIO(checked(layout_basis).read_text('utf-8')),delimiter='\t')}
rows=[]
for row in csv.DictReader(io.StringIO(checked(skill_ref).read_text('utf-8')),delimiter='\t'):
 if row['material']=='True':
  id=int(row['skill_id']);getter=re.fullmatch(r'\[([A-Za-z0-9]+):[A-Za-z0-9]+\]',row['historical_native_models'])[1];parser=re.search(r'Protocol\.([A-Za-z0-9]+)\(',layout_rows[id]['native_static_parser_candidates'])[1];rows.append([str(id),getter,*costs[id],parser])
assert len(rows)==58 and len(costs)==58
run=D/'source-material-controlled-NPC-ingress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in [Path(__file__).name,'SourceMaterialControlledNpcIngressPlan.java']:shutil.copyfile(R/name,inputs/name)
with (inputs/'material-cases.tsv').open('w',encoding='utf-8',newline='')as f:w=csv.writer(f,delimiter='\t');w.writerow(['id','getter','reagent','count','mp','parser']);w.writerows(rows)
metadata=inputs/'target-metadata.tsv'
with metadata.open('w',encoding='utf-8',newline='')as f:
 w=csv.writer(f,delimiter='\t');w.writerow(['id','target','targetTo','range','area','through','type','nativeId'])
 for id,row in sorted(layout_rows.items()):w.writerow([id,row['target'],row['target_to'],row['range'],row['area'],row['through'],row['source_type'],row['native_id']])
entry=S/'config/combat.properties';paths={}
for line in entry.read_text('utf-8').splitlines():
 if line.strip()and not line.lstrip().startswith('#')and '='in line:
  k,v=line.split('=',1);paths[k.strip()]=v.strip()
source_controls=[]
for key,name in [('source.skill.file','skill.properties'),('source.damage.file','damage.properties'),('source.character.file','character.properties')]:
 p=(entry.parent/paths[key]).resolve();source_controls.append(pin(p));shutil.copyfile(p,inputs/name)
cp=';'.join(map(str,[run/'plan' ,base,base.parent/'plan',S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
args=['-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',str(run/'plan'),str(inputs/'SourceMaterialControlledNpcIngressPlan.java')];argfile=run/'javac.args';argfile.write_text('\n'.join(json.dumps(x,ensure_ascii=False)for x in args),'utf-8')
with (run/'probe.log').open('w',encoding='utf-8')as log:
 subprocess.run([str(J/'javac.exe'),'@'+str(argfile)],env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=30)
 subprocess.run([str(J/'java.exe'),'-Xverify:all','-cp',cp,'local.lito381.server.SourceMaterialControlledNpcIngressPlan',str(inputs/'material-cases.tsv'),str(run/'results.tsv'),str(metadata),str(inputs),*sys.argv[1:]],env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=120)
with (run/'results.tsv').open(encoding='utf-8')as f:results=list(csv.DictReader(f,delimiter='\t'))
v=dict(status='inventoried-actual-material-native-ingress-outcomes-not-complete-semantics',runtime=index['runtime'],basis=index['manifest'],candidate_build=progress['build'],material_cost_basis=cat['catalog'],target_metadata_basis=layout_basis,Source_readonly_control_inputs=source_controls,fixture_overrides=dict(DragonKnightAwakeClassicMode=0,source_map_directory=None),decoded_input_entry='ForcedCombat.dispatch -> original ClientSession.handleBeforeForcedCombat',Ctrl_authority_source='native 8-byte LFC1 selected-object extension; no direct ThreadLocal injection',non_object_Ctrl_cases='reported as ctrl-not-object-layout; not silently treated as normal',normal_and_Ctrl_entry_qualification_only=True,controlled_NPC_fixture='canonical active summon777/profile123; original owner, pre-input own-owner fixture, native Honor same-force owner; non-NPC layouts explicitly skipped',friendly_fixture='native HONOR_MATCH map510, two original actors on force1; asserted canAttack=false outside Ctrl',model_name_basis=skill_ref,inputs=[pin(p)for p in inputs.iterdir()],attempts=len(results),status_counts=dict(collections.Counter(r['status']for r in results)),error_types=dict(collections.Counter(r['error_type']for r in results if r['error_type'])),artifacts=[pin(run/'results.tsv'),pin(run/'probe.log')],full_material_semantics_qualified=False,actual_Ctrl_or_cursor_qualified=False,new_SQL_connections=0,real_backend_started=False,formal_deployment_changed=False,full_goal_achieved=False)
write(run/'manifest.json',v);write(D/'source-material-controlled-NPC-ingress-current.json',dict(status=v['status'],runtime=index['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],attempts=v['attempts'],status_counts=v['status_counts'],error_types=v['error_types'],manifest=pin(run/'manifest.json')),ensure_ascii=False))
