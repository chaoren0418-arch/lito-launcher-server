from pathlib import Path
import json,hashlib,datetime
R=Path(__file__).resolve().parent;S=R.parents[1];L=S.parent.parent/'lito登入器/LiTo381-Launcher';B=R/'build'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
build=json.loads((B/'build-manifest.json').read_text('utf-8'))
smoke=json.loads((L/'docs/startup-smoke.json').read_text('utf-8'))
assert smoke['native_ready'] and smoke['cleanup_verified'] and smoke['exit_code']==0
assert sha(L/'app/server/LiTo381-SharedResources-candidate.jar')==build['candidate_sha256']
previous=json.loads((L/'docs/maintenance/release-0.6.70.json').read_text('utf-8'))
dll=L/'app/compat/litoddraw.dll'
assert sha(dll)==next(x['sha256'] for x in previous['runtime'] if x['path']==str(dll))
paths=[L/x for x in ['release/current/LiTo381Launcher.exe','release/current/LiTo381Launcher.dll','release/current/development_startup.py','app/server/LiTo381-SharedResources-candidate.jar','app/server/Start-Shared-World.py','app/compat/litoddraw.dll','app/runtime-pins.json']]
paths+=[S/'deployment/release-inputs.json',R/'inputs/base-0.6.70.jar',B/'PolyMeRegression.log',B/'TravelRegression.log',S/'db/lito_gm_polyme.sql',S/'src/main/java/local/lito381/server/GmPolyMe.java',S/'src/main/java/local/lito381/server/GmTravel.java',S/'src/main/java/local/lito381/server/ClientSession.java',R/'build.py',R/'Patch.java',R/'PolyMeRegression.java']
report={'version':'0.6.71','status':'published-and-verified','at':datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).isoformat(),'feature':'L1PolyMe / .polyme gfx / 7200 seconds','runtime_and_evidence':[{'path':str(p),'bytes':p.stat().st_size,'sha256':sha(p)} for p in paths],'build':build,'native_startup':smoke,'bat_sql_preflight':True,'database_change':{'database':'380_lito','table':'commands','inserted_rows':1,'name':'polyme','class_name':'L1PolyMe','access_level':200,'system':0},'source_database_modified':False,'client_dll_changed':False,'client_bin_modified':False,'baseline_retained':'0.6.66','shortcut':str(Path.home()/'Desktop/LiTo 0.6.71 正式啟動器.lnk'),'backup':str(L/'backups/pre-0.6.71-changed-files.zip'),'tests':['34 GM polymorph checks','existing loc/move/Storm_Walk regression','native start/owned stop and cleanup'],'limits':['Actual in-game GM morph appearance awaits user testing.','GM morph does not add Source plugin polymorph bonus abilities.','Not a completion claim for all combat skill work.']}
(L/'docs/maintenance/release-0.6.71.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n','utf-8')
(B/'publication.json').write_text(json.dumps({'version':'0.6.71','status':'published-and-verified','receipt':str(L/'docs/maintenance/release-0.6.71.json'),'jar_sha256':build['candidate_sha256']},ensure_ascii=False,indent=2)+'\n','utf-8')
print('PASS publication receipt, inherited DLL unchanged, native startup and cleanup verified.')
