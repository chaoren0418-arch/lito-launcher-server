"""Publish the validated native-only profile presentation correction."""
from pathlib import Path
import hashlib,json,re,shutil
L=Path(__file__).resolve().parents[2];S=L.parent.parent/'lito伺服器/LiTo381-Server'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
test=json.loads((L/'docs/profile-location-native.json').read_text('utf-8'))
assert len(test['locations'])==4 and len(test['summary_cycles'])==2
assert [x['display_room']==0 for x in test['locations']]==[True,False,True,True]
prior=json.loads((S/'docs/maintenance/illusion-targets-0.6.51.json').read_text('utf-8'))
assert sha(L/'app/compat/ddraw.dll')==prior['native_dll_sha256']
assert sha(L/'app/server/LiTo381-SharedResources-candidate.jar')==prior['published_sha256']
shutil.copyfile(L/'native/ddraw.dll',L/'app/compat/ddraw.dll')
shutil.copyfile(L/'native/ddraw.dll',L.parent.parent.parent/'0-客戶端/ddraw.dll')
for rel in ('src/MainForm.cs','src/LiTo381Launcher.csproj','tools/build_current.py'):
 p=L/rel;t=p.read_text('utf-8');assert '0.6.51' in t;p.write_text(t.replace('0.6.51','0.6.52'),encoding='utf-8')
backup=L/'backups/before-profile-location-0.6.51.zip'
report={'version':'0.6.52','native_dll_sha256':sha(L/'app/compat/ddraw.dll'),'previous_native_dll_sha256':prior['native_dll_sha256'],
 'server_sha256_unchanged':prior['published_sha256'],'native_sources':{r:sha(L/r) for r in ['native/profile_location.inc','native/class_compat.inc','native/ddraw_proxy.cpp']},
 'rollback_archive':backup.relative_to(L).as_posix(),'rollback_archive_sha256':sha(backup),'native_test':test,'manual_test_pending':True,
 'cause':'Original profile renderer reads copied participant room dword. Native S_D0 switches phase but deliberately leaves room protocol context at 0x64ABF4.',
 'change':'Resolve only the drawn location for self: phase1 lobby=0; phase2 room=active room context. Other participants retain row room. Native room state, resource strings, format and colors unchanged.',
 'prior_manual_validation':'User confirmed illusion targeted casting works in 0.6.51.'}
(L/'docs/profile-location-0.6.52.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
p=L/'README.zh-TW.md';t=p.read_text('utf-8');t=re.sub(r'^# LiTo .*','# LiTo 0.6.52',t,count=1);t+='\n目前 0.6.52：右上角自身位置依原生頁面狀態顯示大廳或實際戰鬥室房號，保留其他玩家位置及原生文字樣式；不改寫原生房間通訊狀態。測試與回復檔案見 docs/profile-location-0.6.52.json。0.6.51 幻術師指向施法已由玩家確認成功。\n';p.write_text(t,encoding='utf-8')
p=S/'README.zh-TW.md';t=p.read_text('utf-8').replace('現行部署：**登入器 0.6.51','現行部署：**登入器 0.6.52');t+='\n0.6.52 僅修正登入器 DLL 的原生角色面板位置顯示，伺服器 JAR 與 SQL 均維持 0.6.51。幻術師指向施法已由玩家實測確認。\n';p.write_text(t,encoding='utf-8')
print('Published 0.6.52 native DLL',report['native_dll_sha256'])
