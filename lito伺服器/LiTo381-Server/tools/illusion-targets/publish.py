"""Publish 0.6.51 only after server and owned-native regression evidence exists."""
from pathlib import Path
import hashlib,json,re,shutil,zipfile
R=Path(__file__).resolve().parent;S=R.parents[1];L=S.parent.parent/'lito登入器/LiTo381-Launcher'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
native=json.loads((L/'docs/illusion-targets-native.json').read_text('utf-8'))
assert native['picker_ids']==21 and native['native_target_packet_bytes']==11 and len(native['summary_cycles'])==2
assert 'PASS: ArenaBot Bone Break' in (S/'runtime/illusion-target-regression.log').read_text('utf-8')
baseline=json.loads((R/'baseline.json').read_text('utf-8'))
target=L/'app/server/LiTo381-SharedResources-candidate.jar'
assert sha(target)==baseline['app/server/LiTo381-SharedResources-candidate.jar']
shutil.copyfile(R/'build/candidate.jar',target)
shutil.copyfile(L/'native/ddraw.dll',L/'app/compat/ddraw.dll')
shutil.copyfile(L/'native/ddraw.dll',L.parent.parent.parent/'0-客戶端/ddraw.dll')
jarsha=sha(target)
p=L/'app/server/Start-Shared-Formal.py';t=p.read_text('utf-8');t=re.sub(r"JAR_SHA256\s*=\s*['\"][a-fA-F0-9]+['\"]",f"JAR_SHA256 = '{jarsha}'",t);p.write_text(t,encoding='utf-8')
p=S/'deployment/release-inputs.json';pins=json.loads(p.read_text('utf-8-sig'));pins['server_sha256']=jarsha;p.write_text(json.dumps(pins,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for rel in ('src/MainForm.cs','src/LiTo381Launcher.csproj','tools/build_current.py'):
 p=L/rel;t=p.read_text('utf-8');assert '0.6.50' in t;p.write_text(t.replace('0.6.50','0.6.51'),encoding='utf-8')
changes=['src/main/java/local/lito381/server/'+n+'.java' for n in ('ClientSession','DragonSkillRequests')]
added='src/main/java/local/lito381/server/IllusionSkillRequests.java'
archive=L/'backups/before-illusion-targets-0.6.50.zip'
report={'version':'0.6.51','baseline_server_sha256':baseline['app/server/LiTo381-SharedResources-candidate.jar'],
 'published_sha256':jarsha,'native_dll_sha256':sha(L/'app/compat/ddraw.dll'),
 'source_changes':{rel:sha(S/rel) for rel in changes},'previous_source_hashes':{rel:baseline['server-source/'+Path(rel).name] for rel in changes},
 'added_sources':{added:sha(S/added)},'rollback_archive':archive.relative_to(L).as_posix(),'rollback_archive_sha256':sha(archive),
 'native_sources':{rel:sha(L/rel) for rel in ('native/class_skills.inc','native/ddraw_proxy.cpp')},
 'server_regression_passed':True,'dragon_presentation_regression_passed':True,'status_and_empty_melee_regression_passed':True,
 'native_test':native,'no_player_sql_writes':True,'manual_test_pending':True,
 'evidence_run':'development-7cd8a04ebd784461a733565c09ac3d29',
 'changes':['14 illusion target skills use native object picker and cccdhh packet','Server validates explicit selected object; missing/unknown/dead/far targets never fall back to self or nearby actors','Existing skill target types, range, costs and effects retained; request ThreadLocal cleared in finally'],
 'lobby_observation':'User confirmed battle/result screen after live capture; no persistent lobby freeze reproduced. Two owned-native result/summary/Confirm/lobby/world cycles tested without changing summary logic.'}
(S/'docs/maintenance/illusion-targets-0.6.51.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
p=S/'tools/Verify-ProjectLayout.py';t=p.read_text('utf-8')
t=t.replace("assert pins['server_sha256'].lower() == illusion['published_sha256']", """targets = json.loads((S / 'docs/maintenance/illusion-targets-0.6.51.json').read_text('utf-8'))
assert targets['baseline_server_sha256'] == illusion['published_sha256']
assert pins['server_sha256'].lower() == targets['published_sha256']
assert sha(L / targets['rollback_archive']) == targets['rollback_archive_sha256']
for rel, expected in {**targets['source_changes'], **targets['added_sources']}.items(): assert sha(S / rel) == expected, rel
with zipfile.ZipFile(L / targets['rollback_archive']) as z:
    for rel, expected in targets['previous_source_hashes'].items():
        assert hashlib.sha256(z.read('server-source/' + Path(rel).name)).hexdigest() == expected
        assert expected == {**latest['added_sources'], **illusion['source_changes']}[rel]
""")
t=t.replace("for rel, expected in {**illusion['source_changes'], **illusion['added_sources']}.items(): assert sha(S / rel) == expected, rel", "for rel, expected in {**illusion['source_changes'], **illusion['added_sources']}.items(): assert sha(S / rel) == targets['source_changes'].get(rel, expected), rel")
t=t.replace("def current_source(rel, previous): return illusion['source_changes'].get(rel, previous)","def current_source(rel, previous): return targets['source_changes'].get(rel, illusion['source_changes'].get(rel, previous))")
t=t.replace(", *illusion['added_sources']}",", *illusion['added_sources'], *targets['added_sources']}")
p.write_text(t,encoding='utf-8')
p=S/'README.zh-TW.md';t=p.read_text('utf-8').replace('現行部署：**登入器 0.6.50','現行部署：**登入器 0.6.51')
t+='''
### 0.6.51 幻術師指定目標

補上 14 個幻術師技能的原生指向游標及包含物件編號的施法封包。伺服器只處理實際選取且符合原有技能規則的單位；缺少、失效或超距目標不再自動改成自己、上一目標或附近單位。自身及立方塊技能維持原本流程，技能租期、成本與範圍驗證保留。

已驗證 ArenaBot 骷髏毀壞傷害、玩家指定 BUFF、召喚物指定技能、九種狀態圖示及原生目標封包；原生客戶端連續兩輪結算、確定返回大廳及再進戰鬥通過。本次未重現持續大廳卡住，未額外修改結算接線。證據：[0.6.51 紀錄](docs/maintenance/illusion-targets-0.6.51.json)。玩家實測待確認。
''';p.write_text(t,encoding='utf-8')
p=L/'README.zh-TW.md';t=p.read_text('utf-8');t=re.sub(r'^# LiTo .*','# LiTo 0.6.51',t,count=1);t+='\n目前 0.6.51：幻術師 14 個指向技能使用原生選取與明確目標封包；原生結算返回再進戰鬥已通過兩輪隔離測試。詳見 LiTo381-Server/docs/maintenance/illusion-targets-0.6.51.json。\n';p.write_text(t,encoding='utf-8')
print('Published candidate inputs',jarsha)
