from pathlib import Path
from datetime import datetime,timezone
import json,hashlib
R=Path(__file__).resolve().parent;S=R.parents[1];D=R/'build';I=D/'integration-regression';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
formal=S.parent.parent/'lito登入器/LiTo381-Launcher/app/server/LiTo381-SharedResources-candidate.jar';assert sha(formal)=='75da69b0d2f7f9d394ee6bf2fdb396b153c75d9b938069b0999ed8dac4f50371'
full=read(I/'qualification-current.json');affected=read(I/'affected-current.json');solid=read(R/'solid-control/build/tests/result.json');assert full['status'].startswith('passed-') and affected['status'].startswith('passed-') and solid['status']=='passed'
for ref in [full['manifest'],affected['manifest'],solid['runtime']]:assert sha(Path(ref['path']))==ref['sha256']
repro=read(R/'solid-control/build-repro/build.json');assert solid['runtime']['sha256']==repro['runtime']['sha256'];assert sha(Path(repro['runtime']['path']))==repro['runtime']['sha256']
layers=[]
for name in ['solid-control','invisibility-control','triple-control','bind-control','invisibility-leases']:
 build=read(R/name/'build/build.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256']
 row={'name':name,'runtime':build['runtime'],'build':pin(R/name/'build/build.json')}
 for key,path in [('targeted',R/name/'build/tests/result.json'),('reproduction',R/name/'build-repro/build.json')]:
  if path.exists():
   value=read(path);row[key]=dict(receipt=pin(path),status=value['status'])
   if key=='reproduction':assert value['runtime']['sha256']==build['runtime']['sha256']
 layers.append(row)
current={}
for key,name in [('existing_20','qualification-current.json'),('affected_18','affected-current.json')]:
 p=D/'control-regression'/name
 if p.exists():
  receipt=read(p)
  if receipt['status']=='running':current[key]=dict(status='running',path=str(p),unsealed=True)
  else:
   assert sha(Path(receipt['manifest']['path']))==receipt['manifest']['sha256'];current[key]=dict(status=receipt['status'],receipt=pin(p),manifest=receipt['manifest'])
latest={}
for key,name in [('existing_20','qualification-current.json'),('affected_18','affected-current.json')]:
 p=D/'lease-regression'/name
 if p.exists():
  receipt=read(p)
  if receipt['status']=='running':latest[key]=dict(status='running',path=str(p),unsealed=True)
  else:
   assert sha(Path(receipt['manifest']['path']))==receipt['manifest']['sha256'];data=read(Path(receipt['manifest']['path']));assert data['runtime']==layers[-1]['runtime'];latest[key]=dict(status=receipt['status'],receipt=pin(p),manifest=receipt['manifest'])
qualified=len(latest)==2 and all(x['status'].startswith('passed-') for x in latest.values())
v={'time_utc':datetime.now(timezone.utc).isoformat(),'status':'candidate-targeted-progress-full-objective-open','formal':pin(formal),'latest_candidate':layers[-1]['runtime'],'latest_candidate_full_regression_passed':qualified,'latest_candidate_regression':latest,'layers':layers,'composed_parent_qualified_tests':{'runtime':read(I/'build-current.json')['runtime'],'existing_20':full['manifest'],'affected_18':affected['manifest'],'historical_formal_guard_reconciliation':pin(I/'replay-reconciliation.json')},'controls_candidate_regression':{'runtime':layers[-2]['runtime'],**current},'prior_audit':pin(R/'prior-evidence-audit.json'),'source_selector_requalification':pin(D/'source-drift/selection-requalification.json'),'remaining_material_census':pin(I/'remaining-census/receipt.json'),'remaining_controls':[],'controls_final_qualification_pending':[] if qualified else ['SOLID_CARRIAGE_MODE','INVIS_CAST_BREAK','Triple_Arrow_Dmg','SLAY_BREAK_NOT_TELEPORT'],'remaining_source_requalification':['Inventory persistence/quarantine/deadline differences outside the four verified selector blocks'],'remaining':['34 material-bearing skills beyond the reviewed 24; 45 methods/46 legacy material calls remain in static census (census is not proof that every call is incorrect)','full class/target/Ctrl/area/trigger and state-lifetime qualification','actual SQL/native UI/shutdown validation of final completed candidate before publication'],'formal_deployment_changed':False,'source_database_mutated':False,'full_goal_achieved':False}
elemental=R/'elemental-material/build/build.json'
if elemental.exists():
 e=read(elemental);v['next_material_stage']={'build':pin(elemental),'status':e['status'],'runtime':e.get('runtime'),'skill_ids':[154,162],'counted_as_completed':False}
 tests=R/'elemental-material/build/tests/result.json'
 if tests.exists():v['next_material_stage']['targeted_tests']={'receipt':pin(tests),'status':read(tests)['status']}
stages=[]
for name,ids,receipts in [
 ('elemental-material',[154,162],['build/tests/result.json','build/boundaries/result.json','build/lifecycle/result.json']),
 ('elemental-lifetime',[154,162],['build/tests/result.json','build/elemental-regression/result.json']),
 ('summon-material',[51],['build/tests/result.json','build/boundaries/result.json','build/choices/result.json']),
 ('summon-parity',[51],['build/tests/result.json','build/boundaries/result.json','build/choices/result.json','build/packets/result.json']),
 ('summon-ring',[51],['build/tests/result.json','build/boundaries/result.json','build/choices/result.json','build/packets/result.json']),
 ('water-life-material',[170],['build/tests/result.json','build/boundaries/result.json']),
 ('protection-material',[68],['build/tests/result.json','build/boundaries/result.json']),
 ('armor-material',[112],['build/tests/result.json','build/boundaries/result.json']),
 ('armor-state',[112],['build/tests/result.json','build/boundaries/result.json','build/state-locks/result.json','build/targets/result.json']),
 ('negative-material',[29,152,153],['build/tests/result.json','build/boundaries/result.json','build/companions/result.json','build/world-npcs/result.json']),
 ('weakness-material',[47,56],['build/tests/result.json','build/boundaries/result.json','build/world-npcs/result.json']),
 ('ground-material',[58,63],['build/tests/result.json','build/boundaries/result.json','build/clock/result.json','build/placement/result.json','build/numeric/result.json']),
 ('silence-material',[64],['build/tests/result.json','build/boundaries/result.json','build/world-npcs/result.json']),
 ('decay-material',[71],['build/tests/result.json','build/boundaries/result.json','build/world-npcs/result.json']),
 ('world-potion',[],['build/tests/result.json']),
 ('target-negative-material',[173,174],['build/tests/result.json','build/boundaries/result.json','build/world-npcs/result.json']),
 ('blind-material',[40,103],['build/tests/result.json','build/boundaries/result.json','build/world-npcs/result.json']),
 ('resurrection-material',[61,75],['build/tests/result.json','build/boundaries/result.json','build/numeric/result.json','build/answer-audit/result.json']),
 ('resurrection-lifetime',[61,75],['build/answers/result.json','build/answer-races/result.json','build/tests/result.json','build/boundaries/result.json','build/numeric/result.json','build/world-npc/result.json','build/companions/result.json','build/companion-audit/result.json']),
 ('resurrection-companion',[61,75],['build/companion-audit/result.json','build/companions/result.json']),
 ('resurrection-ingress',[61,75],['build/companion-audit/result.json','build/companions/result.json','build/answers/result.json','build/answer-races/result.json','build/tests/result.json','build/boundaries/result.json','build/numeric/result.json','build/world-npc/result.json']),
 ('poison-material',[11,33],['build/tests/result.json','build/boundaries/result.json','build/world-npcs/result.json','build/clock-audit/result.json']),
 ('poison-scope',[11,33],['build/tests/result.json','build/boundaries/result.json','build/world-npcs/result.json','build/clock-audit/result.json','build/tick-audit/result.json']),
 ('poison-owner',[11,33],[]),
 ('poison-tick',[11,33],['build/tests/result.json','build/boundaries/result.json','build/clock-audit/result.json','build/tick-audit/result.json']),
 ('poison-retirement',[11,33],['build/boundaries/result.json','build/consumer/result.json']),
 ('poison-publication',[11,33],['build/tests/result.json','build/boundaries/result.json','build/world-npcs/result.json','build/clock-audit/result.json','build/consumer/result.json','build/timers/result.json','build/curse-consumer/result.json']),
 ('earth-bind-material',[157],['build/tests/result.json','build/boundaries/result.json','build/world-npcs/result.json']),
 ('polymorph-material',[67],['build/tests/result.json','build/boundaries/result.json','build/menu/result.json','build/pet-replay/current.json']),
 ('return-nature-material',[145],['build/tests/result.json','build/boundaries/result.json','build/owner-transfer-audit/result.json']),
 ('return-nature-transfer',[145],['build/tests/result.json','build/edges/result.json','build/pet-replay/current.json']),
 ('polymorph-source',[67],['build/load/result.json','build/consumers/current.json']),
 ('pet-material',[36,41],['build/tests/result.json','build/pet-replay/current.json']),
 ('pet-world-lifetime',[36,41],['build/tests/result.json','build/material-replay/current.json','build/pet-replay/current.json','build/composed-replay/current.json']),
 ('pet-world-publication',[36,41],['build/tests/result.json','build/lifetime-replay/current.json','build/material-replay/current.json','build/pet-replay/current.json']),
 ('weapon-break-numeric',[27],['build/plan/result.json','build/consumer/result.json']),
 ('npc-physical-source',[],['build/load/result.json','build/lifetime-replay/current.json','build/material-replay/current.json','build/pet-replay/current.json']),
 ('weapon-break-material',[27],['build/tests/result.json','build/boundaries/result.json']),
 ('pet-world-policy',[36,41],['build/tests/result.json']),
 ('weapon-break-lifetime',[27],['build/replay/current.json','build/target-fault/result.json']),
 ('npc-physical-size',[],['build/load/result.json','build/composed-replay/current.json']),
 ('cancellation-material',[44],['build/tests/result.json','build/boundaries/result.json','build/immunity/result.json','build/sql-audit/result.json']),
 ('cancellation-speed-immunity',[44],['build/tests/result.json','build/material-replay/current.json']),
 ('cancellation-debuff-immunity',[44],['build/tests/result.json','build/material-replay/current.json','build/speed-replay/result.json']),
 ('weight-material',[14],['build/tests/result.json','build/boundaries/result.json'])]:
 p=R/name/'build/build.json'
 if not p.exists():continue
 stage=read(p);assert sha(Path(stage['runtime']['path']))==stage['runtime']['sha256']
 row=dict(name=name,skill_ids=ids,build=pin(p),runtime=stage['runtime'],tests=[],counted_as_completed=False)
 repro=R/name/'build-repro/build.json'
 if repro.exists():
  rv=read(repro);assert rv['runtime']['sha256']==stage['runtime']['sha256'];assert sha(Path(rv['runtime']['path']))==rv['runtime']['sha256'];row['reproduction']=pin(repro)
 for relative in receipts:
  p=R/name/relative
  if p.exists():
   result=read(p);assert result['runtime']==stage['runtime'];row['tests'].append(dict(status=result['status'],receipt=pin(p)))
 stages.append(row)
elemental_regression={}
for key,name in [('existing_20','qualification-current.json'),('affected_18','affected-current.json')]:
 p=D/'elemental-regression'/name
 if not p.exists():continue
 result=read(p)
 if result['status']=='running':elemental_regression[key]=dict(status='running',path=str(p),unsealed=True)
 else:
  assert sha(Path(result['manifest']['path']))==result['manifest']['sha256']
  manifest=read(Path(result['manifest']['path']));assert manifest['runtime']==stages[1]['runtime']
  elemental_regression[key]=dict(status=result['status'],receipt=pin(p),manifest=result['manifest'])
v['material_stages']=stages;v['elemental_candidate_regression']=elemental_regression
elemental_qualified=len(stages)>=2 and len(stages[0]['tests'])==3 and len(stages[1]['tests'])==2 and len(elemental_regression)==2 and all(x['status'].startswith('passed-') for x in elemental_regression.values()) and all(x['status']=='passed' for stage in stages[:2] for x in stage['tests'])
if elemental_qualified:
 for stage in stages[:2]:stage['counted_as_completed']=True
 v.update(latest_candidate=stages[1]['runtime'],latest_candidate_full_regression_passed=True,latest_candidate_regression=elemental_regression)
 v['remaining'][0]='32 material-bearing skills beyond the reviewed 26; static legacy-call census is not proof that every remaining call is incorrect'
 v['remaining_material_census']=pin(R/'elemental-lifetime/build/census/result.json')
 v['next_material_stage']=stages[-1]
v['material_reviewed_count']=26 if elemental_qualified else 24
summon=next((x for x in stages if x['name']=='summon-ring'),None)
summon_regression={}
if summon:
 for key,name in [('existing_20','qualification-current.json'),('affected_18','affected-current.json')]:
  p=D/'summon-regression'/name
  if not p.exists():continue
  result=read(p)
  if result['status']=='running':summon_regression[key]=dict(status='running',path=str(p),unsealed=True)
  else:
   assert sha(Path(result['manifest']['path']))==result['manifest']['sha256']
   manifest=read(Path(result['manifest']['path']));assert manifest['runtime']==summon['runtime']
   summon_regression[key]=dict(status=result['status'],receipt=pin(p),manifest=result['manifest'])
 v['summon_candidate_regression']=summon_regression
 v['summon_alternate_ring_fixture_scope']='120284 identity predicate and native cast tested with a fixture wire view; generic item grant/import for 120284 is not qualified by this test'
 if elemental_qualified and len(summon['tests'])==4 and all(x['status']=='passed' for x in summon['tests']) and len(summon_regression)==2 and all(x['status'].startswith('passed-') for x in summon_regression.values()):
  summon['counted_as_completed']=True
  v.update(latest_candidate=summon['runtime'],latest_candidate_full_regression_passed=True,latest_candidate_regression=summon_regression,material_reviewed_count=27)
  v['remaining'][0]='31 material-bearing skills beyond the reviewed 27; census is not proof that every remaining call is incorrect'
review=D/'source-drift/inventory-drift-review.json'
if review.exists():
 v['source_inventory_drift_review']=pin(review)
 v['remaining_source_requalification']=['Timed-item import/equipment/expiry behavior and non-material uncertain transaction outcomes; reviewed Source SQL writers intentionally excluded from LiTo memory-only runtime']
controls=D/'control-inventory/result.json'
if controls.exists():v['configured_control_inventory']=pin(controls)
pair=R/'protection-material/build/water-life-replay/current.json'
if pair.exists():
 result=read(pair);assert sha(Path(result['receipt']['path']))==result['receipt']['sha256']
 v['pair_material_composed_replay']=dict(status=result['status'],receipt=pin(pair),results=result['receipt'])
 checked=[x for x in stages if x['name'] in ['water-life-material','protection-material']]
 if result['status']=='passed' and len(checked)==2 and all(len(x['tests'])==2 and all(t['status']=='passed' for t in x['tests']) for x in checked):
  v['latest_targeted_candidate']=result['runtime'];v['material_targeted_reviewed_count']=29
  v['targeted_candidate_full_regression_passed']=False
  v['latest_targeted_material_census']=pin(R/'protection-material/build/census/result.json')
  v['remaining_material_skill_ids']=[11,27,29,33,36,40,41,44,47,56,63,64,66,67,69,71,75,76,103,112,145,152,153,157,161,165,167,173,174]
armor=next((x for x in stages if x['name']=='armor-state'),None)
if armor:
 pair=R/'armor-state/build/pair-replay/current.json';bounded=D/'armor-regression/qualification-current.json'
 if pair.exists() and bounded.exists():
  pr=read(pair);br=read(bounded)
  for ref in [pr['receipt'],br['manifest']]:assert sha(Path(ref['path']))==ref['sha256']
  assert pr['runtime']==armor['runtime'] and read(Path(br['manifest']['path']))['runtime']==armor['runtime']
  v['armor_composed_replay']=dict(status=pr['status'],receipt=pin(pair),results=pr['receipt'])
  v['armor_bounded_existing_regression']=dict(status=br['status'],receipt=pin(bounded),manifest=br['manifest'],full_suite=False)
  if pr['status']=='passed' and br['status'].startswith('passed-eight-selected-') and len(armor['tests'])>=3 and all(x['status']=='passed' for x in armor['tests'][:3]):
   v['latest_targeted_candidate']=armor['runtime'];v['material_targeted_reviewed_count']=30
   v['targeted_candidate_full_regression_passed']=False
   v['latest_targeted_material_census']=pin(R/'armor-state/build/census/result.json')
   v['remaining_material_skill_ids']=[x for x in v['remaining_material_skill_ids'] if x!=112]
   v['armor_remaining_scope']=['Selector-only adjacent fallback and rejected NPC/companion eligibility require additional explicit boundary coverage; preserved original filter is not an exhaustive target test']
   if len(armor['tests'])==4 and armor['tests'][3]['status']=='passed':
    v['armor_remaining_scope']=['64 selector-boundary cases passed; the NPC DTO filter case does not qualify every live NPC factory or mode']
for name,ids,required_count,targeted_count in [('negative-material',[29,152,153],4,33),('weakness-material',[47,56],3,35),('ground-material',[63],5,36),('silence-material',[64],3,37),('decay-material',[71],3,38),('world-potion',[71],1,38),('target-negative-material',[173,174],3,40),('blind-material',[40,103],3,42),('resurrection-ingress',[61,75],8,43),('poison-publication',[11,33],7,45),('earth-bind-material',[157],3,46),('polymorph-material',[67],4,47),('return-nature-transfer',[145],3,48)]:
 stage=next((x for x in stages if x['name']==name),None)
 if not stage:continue
 pair=R/name/'build/composed-replay/current.json';composition=None
 if pair.exists():
  result=read(pair);assert result['runtime']==stage['runtime'];assert sha(Path(result['receipt']['path']))==result['receipt']['sha256']
  composition=dict(status=result['status'],receipt=pin(pair),results=result['receipt']);v[name+'_composed_replay']=composition
 tested=len(stage['tests'])==required_count and all(x['status']=='passed' for x in stage['tests'])
 if tested and composition and composition['status']=='passed':
  v.update(latest_targeted_candidate=stage['runtime'],material_targeted_reviewed_count=targeted_count,targeted_candidate_full_regression_passed=False)
  v['remaining_material_skill_ids']=[x for x in v['remaining_material_skill_ids'] if x not in ids]
  census=R/name/'build/census/result.json'
  if census.exists():v['latest_targeted_material_census']=pin(census)
 regressions={}
 for key,filename in [('existing_20','qualification-current.json'),('affected_18','affected-current.json')]:
  p=D/(name.removesuffix('-material')+'-regression')/filename
  if not p.exists():continue
  result=read(p)
  if result['status']=='running':regressions[key]=dict(status='running',path=str(p),unsealed=True)
  else:
   assert sha(Path(result['manifest']['path']))==result['manifest']['sha256'];assert read(Path(result['manifest']['path']))['runtime']==stage['runtime']
   regressions[key]=dict(status=result['status'],receipt=pin(p),manifest=result['manifest'])
 v[name+'_regression']=regressions
 if tested and composition and composition['status']=='passed' and len(regressions)==2 and all(x['status'].startswith('passed-') for x in regressions.values()):
  stage['counted_as_completed']=True
  v.update(latest_candidate=stage['runtime'],latest_candidate_full_regression_passed=True,latest_candidate_regression=regressions,material_reviewed_count=targeted_count,targeted_candidate_full_regression_passed=True)
  v['remaining'][0]=str(58-targeted_count)+' material-bearing skills beyond the reviewed '+str(targeted_count)+'; targeted review is not proof of full target/scene/skill equivalence'
v['negative_remaining_scope']=['Quest/Time Attack NPC routes and NPC/companion identity/lifetime replacement races remain unqualified','Weakness/Disease original roots do not resolve owned companions; separate target ingress requires an evidence audit']
v['polymorph_remaining_scope']=['Shared random weights/selected metadata now pass real read-only SQL loading and 272 native consumer cases on 32d88c6b; cause exclusion still fails startup instead of Source per-cast no-change response; controlled menu metadata and polymorph projectile metadata remain unqualified. Final composed/regression/publication gates remain open.']
v['return_nature_remaining_scope']=['Caster material, entity lifetime and SUMMON owner inventory failure/overflow/STOPPED targeted tests, 5164 composed cases and 241 pet-control replays passed on 54a781b5; final regression pending.','TAMED world/Quest/TimeAttack origin release transactions remain outside the SUMMON transfer fix; do not infer full skill145 qualification.']
v['pet_material_remaining_scope']=['Caster material migration for 36/41 is a separate candidate; WORLD-only probes and capacity replays do not qualify NPC identity replacement, corpse conversion races, or Quest/TimeAttack paths. Do not count these skills completed until those gates pass.']
v['additional_candidate_regressions']={}
for name in ['return-nature-transfer','polymorph-source','pet-material','npc-physical-source','npc-physical-size']:
 result={}
 for label,filename in [('existing_20','qualification-current.json'),('affected_18','affected-current.json')]:
  p=D/(name+'-regression')/filename
  if not p.exists():continue
  receipt=read(p)
  if receipt['status']=='running':result[label]=dict(status='running',path=str(p),unsealed=True)
  else:
   assert sha(Path(receipt['manifest']['path']))==receipt['manifest']['sha256']
   assert read(Path(receipt['manifest']['path']))['runtime']==read(R/name/'build/build.json')['runtime']
   result[label]=dict(status=receipt['status'],receipt=pin(p),manifest=receipt['manifest'])
 if result:
  v['additional_candidate_regressions'][name]=result
  if len(result)==2 and all(row['status'].startswith('passed-') for row in result.values()):
   v['latest_20_plus_18_candidate']=dict(runtime=read(R/name/'build/build.json')['runtime'],regressions=result,full_goal_achieved=False,final_sql_ui_shutdown_qualified=False)
other=D/'control-inventory/other-controls-scope.json'
if other.exists():v['additional_control_scope_audit']=pin(other)
display=D/'control-inventory/display-consumer-audit.json'
if display.exists():v['additional_control_consumer_audit']=pin(display)
resource=D/'control-inventory/display-resource-audit.json'
if resource.exists():v['additional_control_resource_audit']=pin(resource)
retained=D/'baseline-retention/current.json'
if retained.exists():v['accepted_buff_and_paging_retention']=pin(retained)
retained=D/'candidate-retention/current.json'
if retained.exists():v['candidate_buff_byte_retention']=pin(retained)
pet_audit=D/'pet-control-audit/current.json'
if pet_audit.exists():v['pet_control_consumer_audit']=pin(pet_audit)
remaining_roots=D/'remaining-material-consumers/current.json'
if remaining_roots.exists():v['remaining_material_consumer_audit']=pin(remaining_roots)
earth_gate=R/'earth-bind-material/build/boundaries/source-action-gate-review.json'
if earth_gate.exists():v['earth_bind_source_action_gate_review']=pin(earth_gate)
pet_build=R/'pet-controls/build/build.json'
if pet_build.exists():
 pet=read(pet_build)
 if 'runtime' in pet:
  assert sha(Path(pet['runtime']['path']))==pet['runtime']['sha256']
  v['pet_control_candidate']=dict(build=pin(pet_build),runtime=pet['runtime'],counted_as_completed=False,qualification='Six targeted control tests passed, including WORLD collar/taming/zombie consumers; full candidate regression, material36/41 and role/entity lifetime remain pending',tests=[pin(R/'pet-controls/build'/name/'result.json') for name in ['plan','load','capacity','collar','taming','zombie'] if (R/'pet-controls/build'/name/'result.json').exists()])
  seal=R/'pet-controls/build/qualification-current.json'
  if seal.exists():
   qualification=read(seal);assert qualification['runtime']==pet['runtime'] and qualification['status']=='passed-targeted-pet-controls-full-release-open'
   v['pet_control_candidate']['qualification_receipt']=pin(seal)
for key,relative in [('additional_control_pure_plan','display-controls/build/plan/result.json'),('additional_control_native_call_audit','display-controls/build/ingress/result.json'),('polymorph_source_pure_plan','polymorph-source/build/plan/result.json')]:
 p=R/relative
 if p.exists():v[key]=pin(p)
if stages:v['next_material_stage']=stages[-1]
for key,relative in [('latest_static_material_census','weight-material/build/material-census/receipt.json'),('cancellation_source_semantics','cancellation-material/build/source-control-audit/source-semantics-current.json'),('cancellation_probability_uninstalled_draft','cancellation-probability/draft.json')]:
 p=R/relative
 if p.exists():v[key]=pin(p)
(R/'continuation-current.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps({'status':v['status'],'candidate_sha256':v['latest_candidate']['sha256'],'formal_changed':False}))
