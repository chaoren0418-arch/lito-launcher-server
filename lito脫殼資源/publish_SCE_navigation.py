"""Publish batch14 bounded S_CE evidence; retain prior contracts and guard."""
from audit import *
from seal_delta import seal

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    report=load('SCE-navigation-contract.json');v=report['verification'];pin=record(ROOT/'SCE-navigation-contract.json')
    assert v['instruction_assertions']==107 and v['new_code_ranges']==19 and v['new_data_ranges']==8
    assert v['inherited_raw_ranges']==7 and v['code_derived_models']==10 and len(report['prior_primary_pins'])==15
    for p in report['prior_primary_pins']:assert record(Path(p['path']))['sha256']==p['expected_sha256']
    for p in report['existing_evidence'].values():assert record(Path(p['path']))==p
    for p in report['reusable_outputs']:assert record(Path(p['path']))==p
    review=load('SCE-navigation/independent-review.json');assert review['all_match'] and review['primary']==pin
    packet=record(ROOT/'server-packet-delta.json');assert packet==v['server_packet_unchanged']
    readme=ROOT/'SCE-navigation/READ-ME.txt'
    readme.write_text('''第十四批：S_CE 原版 dispatch／room stores／navigation direct helper 邊界

主入口：../SCE-navigation-contract.json
本批是已知 S_CE 契約補證與有限導航缺口定位，不是新 opcode 或 wire 更正。Modern navigation guard 保留；沒有原版 GUI/player/owner 驗收。
固定 PE SHA 4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9。
原 BIN SHA CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292。
ASM SHA 2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A，只當導航；固定 raw 才是指令權威。

既有證據與本批缺口
舊 NATIVE-JOIN-RESULT-EVIDENCE 已有完整20-field成功形狀／非零result分支及failure-only形狀，本批引用原檔SHA、不重跑舊PASS。
7份raw沿用：4A2A50 reader、4C6AE0 sender、reader jump/map、450230、469CA0前24bytes、5025A8 c格式。
只補19code／8data，共27range；469CA0只新增469CB8..469CC8 selector gap及mode2分支469EC3..469FCA，其他scene mode不展開。
107項固定expected指令斷言；20個format argument位址；2個封存frame、20個reply欄位、10個有限code-derived models。independent-review另逐raw／指令／固定frame byte offsets讀回核對。

dispatch 有條件
mode64BBD4==4且signed phase64AF98>=3：CE在primary map4B353E=99hex(slot153)，jump4B346C→4B3123 default；不進4A8BA0。
其餘fallback：CE map4B3782=30hex(slot48)，jump4B3630→4B3199；4B319B ECX=ESI body→4A8BA0。
Primary default止於4B3135→private05AD0986；debugprefix／sender／private重啟是否返回仍不驗收。能dispatch不等於owned intent／reply合法。

handler與room stores
4A8BA0..4A8FDD complete body。第一個c從body+1讀result，MOVSX i8後unsigned JA>21，負值或超過21走cleanup；只有result0讀成功record。
成功格式634448 = cdscshhchcccccsccccc+NUL：
result local；room_id64ABF4 d；room_name64AB70 s(cap90bytes)；attributes64ABE6 c；password64ABE8 s(cap5)；users64ABEE h；capacity64ABF0 h；mode64ACCA c；map local h；summary五c依序64ACCC/64ACD0/64ACD3/64ACD1/64ACD2；label64ABCA s(cap28)；options四c64ACD4..D7；count local c。
Mode／summary雖有IDA word/DWORD label，c只寫低1byte。字串是opaque8bit+NUL；local cap不等於incoming frame length bound或codepage證明。Reader s止於77409B70 copy和774095E0 seek外部target；forced destination[last]=0與EAX+1 cursor只為本地可見行為，原DLL SHA／targetname／成功讀入仍未閉合。
Selections固定初始化8slot64ACD8..DF；signedcount負值全zero，正值只讀最多8；>8 tail不consume，沒有native malformed-frame拒絕證明。
4A9030是RET。Map rawWORDFFFF／FF9C保留AL，其餘只ADD AL,0C、8bitwrap，結果寫64ACC9；frame66 map564→lowbyte64。

兩個list與direct helper的重要細節
四個optional UI list64A950/64A94C/64A968/64A96C經450230；它可能vtable[0] destructor(1)，target效果未知，local只清object+2C／+28。
4A8BFD與4A8C57呼叫4B4BB0時ECX==EDX==old list end；helper走empty range→return supplied begin，所以重設64AB68=64AB64、64AB58=64AB54，沒有record copy。4B3B80是RET8，不能猜destroy/free效果。

phase2與callback order
469CA0 ECX2：62C21C==0只寫64AF98=2便返回。若非零，mode2先寫phase2、執行有條件object/UI/virtual操作，再callbacks62C9B8=46D640/BC=46D8A0/C0=46D630，最後75A998/75A99C=0。
必要直接UI helper有raw：41B410無本地null/bounds guard childlookup；41AA90／41AAA0 child+14=1／0；41B6C0 object+8=1且6414A4/A0=0；41B6E0 object+8=0/A0=0但可能64149C vtable+18、6414A4 vtable+10；4765F0參數0寫child+30=0/+16=1/+20=[child+38]。
mode2對64AD08 children0..7寫+14=1，然後1/5寫0；對64AD04 child2呼叫4765F0(0)。UI pointer/array/vtable實際初始化都未觀測。
Handler若64AFC7!=0，在469CA0 RETURN之後才把當時62C9B8/BC/C0存到64A9B4/B8/BC，再安裝47B6F0/47B700/469BD0。不要把這個備份當作必然是S_CE開始前的callbacks。62C21C==0時helper沒換callbacks，備份內容又不同。
458830清5200bytes[6494A0,64A8F0)，寫50words在649506+i*68(i0..49)，來源503478低16；64949C=0、649490=1、649498=signed remainder((50-62BCC0) wrapped32)/50。
Protected8316B0(index XOR B104FA87)/B4 pointer/B8 key解碼若nonzero返回；zero才格式化room_id／通知。沒有把這項值命名為owner/player，沒有初始化它。

不能解除guard的直接邊界
mode2 virtual：64AD1C +50／+4C，64A990／64A98C +24，64B09C／64B098／64A904 +20；UI cleanup與41B6E0 virtual target亦未知。
4A8DDB→external773F5200與4A8DF6→458BB0 notification depth；46A650→4980F0 alert depth；sender4C6AE0已沿用但下一層format/transport/成功send未新增驗收。
缺runtime62C21C／64AFC7／protectedcell／objectgraph、callback target執行、owned connection/control revision、完整virtual/private effects。所有callsite分類見direct-call-boundaries.json。

封存capture與C_CC／C_CF intent
沿用5ED原Host-GAME frame65 C_CC body21/wire23 → frame66 S_CE body64/wire66；frames.json SHA A9BCA0ECC611F76EE1BD2D6BE6F3B512DC5ABD169C60DACEB6FC74C6D7E0D69F。沒有新capture。
Frame65 compact cs+c+s+h：BattleSkillRoom、attrs08、empty password、capacity2；沒有room_id，也沒有option/count tail。
Frame66 result0、room_id1、BattleSkillRoom、attrs28、empty pass、users2/cap2、mode1/map564、summary5zero、label BattleHost(BattleMoon)、options4zero/count0，正好consume64bytes。
Current Java只做read-only比較：C_CC CHANNEL guard→allocate room→synthetic C_CF cdroomId+spassword→sharedjoin；attrs OR20。Direct C_CF讀room_id/password且CHANNEL guard；sharedwriter成功後Java phaseROOM/currentRoomId。這不是把current source當成frozen AD368 JAR的新增provenance。
此capture只有C_CC intent；不能拿不存在的C_CF frame替代，也不能從ASCII label推player object。64ABCA在此Javawriter來自room owner display name；舊native報告local_player是欄位名稱，不是actor identity驗收。
Native handler沒有requestnonce／C_CC或C_CF原requestop／opaquecontrolrevision／ownedepoch檢查。Modern應保留completedwrite、explicit create/join intent、sameownedepoch、revision、mismatch/stale/unsolicited refusal，C_CF匹配requestedroom_id、C_CC匹配ownedcreateintent與name/settings；這些是consumer policy而非新發現native nonce。
既有R513已知structuraldecode後在native_city_runtime.rs:273 refusal、retireepoch/checkpointpreserved；本批沒有修改guard、代跑Modern或代改server。

交接與停止範圍
可交Modern的是明確room slots、phase2/callback順序、directUI/reset helper可見行為、virtual/private prerequisite清单，不能宣告navigation effects全閉合或player/GUI ready。
newwire0/newopcode0/wirecorrection0；server-packet-delta SHA B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C原封不動。
15個prior primary/ranges/snapshot pins保持，包括batch13 004F07E...；historical prebatch manifest/delivery只是歷史彙總。
S_D0 frame69／participant roster／room world／results只列future boundary，本批不decode、不展開handler或callbacks。native/observer/liveprocess/input/injection/screenshot、Java/SQL/BIN他組writes、oldPASS、producer replay均0。有限批次封存即停。
''',encoding='utf-8')
    summary={'evidence':pin,'finite_SCE_navigation_boundary_complete':True,'Modern_navigation_guard_retained':True,
      'all_navigation_side_effects_qualified':False,'new_code_ranges':19,'new_data_ranges':8,'inherited_raw_ranges':7,
      'instruction_assertions':107,'wire_argument_witnesses':20,'sealed_frames':2,'code_derived_models':10,
      'primary_vs_fallback_dispatch_distinguished':True,'callback_backup_after_helper':True,'list_empty_range_confirmed':True,
      'new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,'prior_primary_pins_preserved':15,'server_packet_modified':False,
      'native_GUI_player_owner_accepted':False,'S_D0_results_expanded':False}
    complete=load('completeness-audit.json');complete['SCE_navigation_boundary_batch']=summary;write('completeness-audit.json',complete)
    reader=ROOT/'閱讀入口.md';text=reader.read_text(encoding='utf-8').split('\n第十四批S_CE navigation：')[0]
    reader.write_text(text+'\n第十四批S_CE navigation：SCE-navigation-contract.json + SCE-navigation/READ-ME.txt。Fallback dispatch→4A8BA0、primary CE→default；20-field slots、phase2／callback保存發生在helper返回後、兩次listhelper empty-range、directUI/reset helpers与virtual/private prerequisites分級。107指令／19code+8data／7沿用raw／封存frame65-66。Modernguard保留、15priorpins、newwire0／packet不改；S_D0/results future-only。\n',encoding='utf-8')
    handoff=load('handoff.json');handoff.update(updated_at=stamp(),phase='BATCH14_SCE_NAVIGATION_BOUNDARY_SEALED_GUARD_RETAINED',entrypoint=record(reader),SCE_navigation_contract=pin,finite_SCE_navigation_boundary_complete=True)
    added=[pin]+[record(p)for p in sorted((ROOT/'SCE-navigation').iterdir())if p.is_file()]
    for group in ['modern','launcher']:
        mapped={x['path']:x for x in handoff[group]};mapped.update({x['path']:x for x in added});handoff[group]=[mapped[k]for k in sorted(mapped)]
    handoff['shared']=[record(ROOT/'completeness-audit.json')if x['path']==str(ROOT/'completeness-audit.json')else x for x in handoff['shared']]
    handoff['server']['SCE_known_contract_corroboration']=pin
    handoff['next_steps']=[
      'Modern consume S_CE roomslot/phase2/callback-order boundaries; guard retained until initialized UI/virtual/private effects andownedintent/currentepoch/controlrevision prerequisites are closed.',
      'Primary CE map defaults whenmode4/phase>=3; fallback alonecalls4A8BA0. Dispatchcompatibility isnotownedreply authorization.',
      'Onlyoriginal5ED Host-GAME frame65C_CC→frame66S_CE corroborated; native_label isdisplaybytes notplayer identity; C_CFintent needsrequestedroom_id match independently.',
      'Native listcalls haveECX==EDX oldend andreturnbegin;callbackbackup occursafter469CA0 returns. Private/virtual/depthstops remainexplicitunknown.',
      'S_D0frame69/participant-roster/world/results remainfutureboundary; no newwire/serverpacketchange/nativeGUI/playerclaim. Finitebatchsealed; stop.']
    assert handoff['server']['packet_delta']==packet;write('handoff.json',handoff)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=handoff['phase'],current_task='Batch14 finiteSCE dispatch/roomstores/scene-mode2 directhelper boundariessealed. Modernguard retained; virtual/privateeffects andownedintent readiness unqualified.',SCE_navigation_summary=summary)
    progress['handoffs']['modern']='SCE-navigation-contract.json: fallback-onlyhandler,20roomslots,mode2/callbackorder,UI/reset directhelpers,virtual/private prerequisite boundaries; keep navigationguard'
    progress['next_steps']=handoff['next_steps'];write('監製進度.json',progress)
    for p in report['prior_primary_pins']:assert record(Path(p['path']))['sha256']==p['expected_sha256']
    for p in report['existing_evidence'].values():assert record(Path(p['path']))==p
    names=['SCE_navigation_audit.py','publish_SCE_navigation.py','SCE-navigation-contract.json','completeness-audit.json','handoff.json','閱讀入口.md']+[str(p.relative_to(ROOT))for p in(ROOT/'SCE-navigation').iterdir()if p.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'primary':pin,'delta_files_checked':count,'prior_pins_preserved':15,'delivery':record(ROOT/'delivery-verification.json'),'packet_unchanged':packet,'Modern_guard_retained':True},ensure_ascii=False))

if __name__=='__main__':main()
