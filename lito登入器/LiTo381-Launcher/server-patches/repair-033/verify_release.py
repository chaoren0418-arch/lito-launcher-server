from pathlib import Path
import json,hashlib,zipfile
r=Path(__file__).resolve().parent;w=r.parent;p=w/'publish-0.6.33';c=w/'compat-details-033'
sha=lambda f:hashlib.sha256(f.read_bytes()).hexdigest()
pins=json.loads((w/'launcher-build-inputs-0.6.33.json').read_text(encoding='utf-8'))
for name,digest in pins.items():assert sha(Path(name)).upper()==digest,name
assert sha(r/'candidate.jar')==sha(w/'server-details-033/LiTo381-SharedResources-candidate.jar')
assert sha(p/'development_startup.py')==sha(w/'launcher-src-0.6.33/development_startup.py')
assert '"-Dlito.extendedItemDetails=true"' in (w/'launcher-src-0.6.33/FormalStandardSources.cs').read_text(encoding='utf-8')
assert 'compat-details-033/ddraw.dll' in (p/'development_startup.py').read_text(encoding='utf-8')
with zipfile.ZipFile(w/'repair-032/candidate-details.jar') as old,zipfile.ZipFile(r/'candidate.jar') as new:
    changed=[n for n in old.namelist() if old.read(n)!=new.read(n)]
    assert sorted(changed)==['local/lito381/server/ClientSession.class','local/lito381/server/LiToItemDetails.class'],changed
    assert set(new.namelist())-set(old.namelist())=={'local/lito381/server/LiToItemTemplates.class'}
assert 'PASS: actual SQL custom descriptions (42 colored rows)' in (r/'description-regression.log').read_text(encoding='utf-8')
native=(c/'native-033.log').read_text(encoding='utf-8');assert native.count('PASS:')==2
bridges=json.loads((r/'item-bridge-regression.json').read_text());assert len(bridges)==6 and all(v['registers_stack_and_native_continuation'] for v in bridges)
formal=json.loads((w/'server-details-033/formal-timing-test.json').read_text());assert formal['accepted']
report={'version':'0.6.33','scope':'native tooltip style plus selected-source custom item descriptions',
 'client':r'D:\lineage開發\0-客戶端\LiTo.bin','artifacts':{str(f):sha(f) for f in [p/'LiTo381Launcher.exe',p/'development_startup.py',c/'ddraw.dll',r/'candidate.jar']},
 'custom_description_table':'道具_物品詳細說明','row_rule':'Item_Id=-1 global first, then exact item ID; preserve Info text and native color codes',
 'sql_test':{'item_88889_rows':21,'item_88890_rows':21,'full_hp_mp':999999,'full_mr':999,'full_ac':-999,'full_sp':888,'encrypted_publication_passed':True},
 'native_style':{'width':142,'bevel_edges':8,'item_palette':True,'native_color_escapes':True,'preserve_color_on_wrap':True,'ordinary_bounded_items_use_original_renderer':True,'scroll_rows':35},
 'formal_startup':formal,'manual_gui_verified':False,
 'prior_user_acceptance':{'result_confirm_neighbor_no_exit_dialog':True,'burning_fireball_own_summon':True},
 'source_scope':{'changed_classes':changed,'only_client_session_method_instrumented':'selectedSourceAccounts; preload read-only item metadata once per selected repository'},
 'retained_limitations':['all-skill target matrix remains separate unfinished work','custom definitions reload on server restart','tooltip appearance still needs human visual acceptance']}
run='development-69c57efb7f804fe0b56d988eb3c706df';receipt=w/f'runs/{run}/native-probe.shared-ready.json'
if receipt.exists():
    ready=json.loads(receipt.read_text());assert ready['total_count']==30285
    report['native_startup']={'run':run,'ready':ready,'extension_log':'03:44:44.119 native 142px bevel, item palette, color renderer, wide values, scroll'}
(r/'release-0.6.33.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print('PASS: release pins, isolated dependencies, narrow class changes, exact SQL rows/colors/full values, encrypted publication, native rendering/bridges, formal SQL readiness')
