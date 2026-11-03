"""Publish batch10 S0C37 known-contract corroboration; no server product delta."""
from audit import *
from seal_delta import seal
def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    report=load('S0C-status-owner-contract.json');v=load('S0C-status-owner/new-evidence-verification.json')
    assert v['raw_ranges']==21 and len(v['instruction_assertions'])==70 and v['range_bytes_and_pins_match']
    assert all(record(Path(x['path']))['sha256']==x['expected_sha256']for x in v['prior_primary_pins'])
    assert all(record(Path(x['path']))==x for x in report['reusable_outputs'])
    assert report['wire']['body_including_opcode']==37 and report['server_handoff']['new_wire_differences']==0
    packet=record(ROOT/'server-packet-delta.json');pin=record(ROOT/'S0C-status-owner-contract.json')
    readme=ROOT/'S0C-status-owner/READ-ME.txt'
    readme.write_text('''第十批：S0C37 狀態欄位／技能 owner 邊界

主要入口：../S0C-status-owner-contract.json
固定PE SHA4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9。
21段新raw（11 code、10 data），70項固定expected指令斷言；全code完整decode，逐指令raw bytes/operands對照PE。8份既有receive/selection/owner raw沿用，未重跑producer或舊PASS。兩筆既有AD368 frame唯讀解碼，4個新static欄位cases；沒有新native/Java/SQL/runtime/capture。

Dispatch與消費長度
Mode64BBD4==4且signed phase64AF98>=3用primary map4B3470；其餘走fallback4B36B4。Opcode0C分別map slot4/5，entry4B3218/4B3584均為4B2A13；4B2A15呼叫4A36F0，ECX仍為完整body pointer。兩個分支沒有這個opcode的skill owner/readiness gate。
如果debug byte64AFA2非零，dispatcher prefix在selector之前有private/external logging calls，停在其VA，不推其實際成功。既有receive pump完整body同步分派，不能等同handler完成。
4A36F0將ESI=body+1，格式6342CC為dcdcccccchhhhcdcchcccc。22 fields合計36參數bytes，加opcode=37 bodybytes。4A2A50的c/h/d numeric cases以LE組字，寫exact1/2/4bytes，回傳input+36；handler未檢查這個return。Numeric reader無body-length參數/guard，37是結構預期，不是native exact-size或malformed-frame拒絕驗收。新static validator的36/38-byte及wrong-opcode拒絕只屬audit model。

欄位（offset從含opcode的body起算）
offset  bytes  格式  既有server語義                Native destination
1       4      d     objectID u32                  75AA48；parse前另傳protected831738
5       1      c     level u8                      local ebp-0D→byte8348CA
6       4      d     experience u32                DWORD8348C4
10      1      c     STR u8                        byte8348B0
11      1      c     INT u8                        byte8348B1
12      1      c     WIS u8                        byte8348B2
13      1      c     DEX u8                        byte8348B3
14      1      c     CON u8                        byte8348B4
15      1      c     CHA u8                        byte8348B5
16      2      h     currentHP u16                 local ebp-14→maskFFFF→protected8315F0
18      2      h     maxHP u16                     WORD8348BC
20      2      h     currentMP u16                 local ebp-18→maskFFFF→protected8315E0
22      2      h     maxMP u16                     WORD8348BA
24      1      c     AC i8                         byte8348B6
25      4      d     gameTime u32                  DWORD8348AC
29      1      c     food gauge u8                 8348CC低byte，upper3bytes不覆寫
30      1      c     weight gauge u8               byte8348CB
31      2      h     lawful i16                    WORD8348B8
33      1      c     fire resistance i8            byte8348C1
34      1      c     water resistance i8           byte8348C0
35      1      c     wind resistance i8            byte8348BF
36      1      c     earth resistance i8           byte8348BE
Signed AC/lawful/resist的語義來自既有Java validation/domain，Native primitive reader僅copy bitpatterns，沒有sign-extension；不能推所有Native下游consumer都採同樣signed interpretation。Current HP/MP則在handler明確maskFFFF。fields.json含每個push VA/RVA/raw/SHA及numeric case mapping，無string/encoding欄位。

Protected value與直接side effects
4A3718在parse之前將[body+1] DWORD傳4C3E90，ECX831738；HP/MP分别傳8315F0/8315E0。4C3E90操作object+4 storage、+8 key、16DWORDstorage slots與+0 encoded index，value不等於直接讀object base。其4C3CA0、773DC650 random/opaque helper缺本批下游語義，live storage/pointers/成功保持unknown；external773DC650不raw-backed，未填補body。欄位decode可供Modern typed state使用，不得偽造已建立Native protected objects。
Level byte若actor834EE4存在、level!=1、與前次8348CA不同：分配140bytes，44EDE0建立vtable4F84C0的effect，sprite084F，位置來自actor+2C/+30，然後virtual+4。這不是skill owner構造；allocator thunk4F118D→773C4C20、44EE55→77406880與virtual callback停在精確邊界。無allocation/actor/effect成功的live結論。
GameTime DWORD先經4D0570 tailjmp4D0470。直接body寫834898/9C/A0/A4、可更新protected8316A0，並呼叫下一層4D02F0；到該callee停止。831790==0時4A3859另呼叫4C51B0，其直接body會改831790/798/79C、6352F8/FC、831748，有特定map/time條件下C59 send callsites。4C4B10/447B50/485800、indirect random等不再展開；這不是本批新的C59 wire差異。

834888：精確且有限的結論
完整S0C handler與本批直接callee bodies都無834888 operand，也無call4C9190；S0C不直接測試或要求skill owner，不直接建立/發布它。它要求的protected storage或existingactor是其他對象，不能與834888混用。
已接受第三／四批普通4928C0 selection：492D80→4C9190建立owner，492D91發布834888，492F02較晚才送C_C9；reuse path492B61→4CD1C0。可見source order不依賴response S0C來建立owner，實際callback成功/current owner epoch仍未觀測。
不宣稱所有indirect aliases、private/virtual或更深callbacks完全不涉及owner；直接absence與全程式absence分開。S1E仍需既有owner/table/class/order prerequisites。S0C decoded/status objectID相符，不等同actor205、skill owner、選角owner初始化、首大廳或player ready。

R509封存frames／既有server方法比較
讀取r509-character-selection-channel-handoff.json及其hash-pinned frames.jsonl：SHA E6840F9A98E51188DF4651D49C7A189F77D595A3A727BEEBB381AF5B0CDD7EED。
line43：S0C body37、wire_length39，object10201、level52，跟在user C_CA2→S_C9 success→S_CA之後。
line73：S0C body37、wire_length39，object10001、level52，跟在user C_CA1→S_C9 success→S_CA之後。
兩筆均解到EXP0、STR18/INT8/WIS9/DEX13/CON18/CHA12、HP200/max200、MP100/max100、AC10、time0、food13、weight/lawful/resists0。只讀sealed logs，没有capture/producer重跑。R509把它們保留為first unsupported；本批沒有替它宣稱Native handler或Modern runtime接受。
Protocol.characterStatus原方法已用opcode0C並寫這22欄位，HP/maxHP/MP/maxMP與STR/INT/WIS/DEX/CON/CHA順序一致。Server method現在寫gameTime d0，但native有真DWORD field與side effects，不當成reserved。Food225/weight240以(clamp(v,0,max)*29+max//2)//max投影0..29 gauge，不直接當raw retained units。
既有NumericBreakthrough opt-in schema1/2/4會將body增至48/66/78，需對應patched client；不在fixed native37契約。封存frame37明確符合baseline shape，不推整個server的current環境選項。所有Java source/excerpts僅唯讀pin，未執行tests或改動。
分類為KNOWN_CONTRACT_CORROBORATION：原版raw、既有producer、兩筆actual37-body一致，沒有真wire差異或新opcode。server-packet-delta.json保持原SHA，不製造server更新；新的receiver/global/owner補證供監製轉交Modern。

封存與未驗收
保留第三至第九批與packet共11個primary/ranges/snapshot pins。Native/observer/實機/input/injection/screenshot、Java/SQL/producer PASS、新archive payload讀取與其他組修改均0。Live protected objects、actor、owner epoch、helper/virtual completion、原生GUI／player ready均false。沒有S0尾政策推測、被攔截server工作接管或全程式遞迴。本有限batch完成即停止。
''',encoding='utf-8')
    reader=ROOT/'閱讀入口.md';text=reader.read_text(encoding='utf-8').split('\n第十批S0C37 status owner：')[0]
    reader.write_text(text+'\n第十批S0C37 status owner：S0C-status-owner-contract.json + S0C-status-owner/READ-ME.txt。21段raw／70項指令斷言；兩個dispatch分支→4A36F0，22欄位36參數bytes+opcode=37，LE與typed/global destinations完整交接。兩筆既有AD368 frame及server characterStatus原方法一致，屬known補證，newwire0，packet檔不改。S0C無直接834888存取/測試/構造，選角owner publication先於C_C9；不宣稱private/virtual下游完全不涉及，也不推owner/player ready。4新static cases；no producerPASS/native/runtime重跑。保留3..9 pins。\n',encoding='utf-8')
    summary={'evidence':pin,'finite_static_S0C37_contract_complete':True,'new_raw_ranges':21,'instruction_assertions':70,'field_count':22,'body_bytes':37,
      'existing_actual_AD368_frames_decoded':2,'new_synthetic_checks':4,'classification':'KNOWN_CONTRACT_CORROBORATION',
      'S0C_explicit_skill_owner_create_test_access':False,'indirect_owner_absence_proven':False,'skill_owner_or_player_ready':False,
      'native_GUI_accepted':False,'new_wire_differences':0,'new_opcodes':0,'server_packet_primary_modified':False}
    complete=load('completeness-audit.json');complete['S0C_status_owner_batch']=summary;write('completeness-audit.json',complete)
    handoff=load('handoff.json');handoff.update(updated_at=stamp(),phase='BATCH10_S0C37_STATUS_OWNER_STATIC_CONTRACT_SEALED',entrypoint=record(reader),
      S0C_status_owner_contract=pin,finite_S0C_status_owner_audit_complete=True)
    added=[pin,record(readme)]+report['reusable_outputs']
    for group in ['modern','launcher']:
        mapped={x['path']:x for x in handoff[group]};mapped.update({x['path']:x for x in added});handoff[group]=[mapped[k]for k in sorted(mapped)]
    handoff['shared']=[record(ROOT/'completeness-audit.json')if x['path']==str(ROOT/'completeness-audit.json')else x for x in handoff['shared']]
    handoff['server']['S0C_known_contract_corroboration']=pin
    handoff['next_steps']=[
      'Supervisor/Modern consume S0C37 fields, destination globals and explicit skill-owner boundary for the R509 first unsupported message; typed decode must not publish owner/player ready.',
      'Existing AD368 S_STATUS37, Protocol.characterStatus baseline and fixed receiver agree; newwire0/newopcode0. Preserve original packet_delta; server can read corroboration without a product change.',
      'Preserve batches3–9 and packet pins; do not rerun producer/oldPASS or invent private/virtual/protected storage results.',
      'This finite batch stops at listed direct/depth boundaries; live helpers/protected objects/actor/owner epoch/input/GUI remain unaccepted.']
    assert handoff['server']['packet_delta']==packet;write('handoff.json',handoff)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=handoff['phase'],current_task='Batch10 finite S0C37 receiver fields/globals and explicit owner boundary sealed; known wire corroboration, no owner/player ready inference.',S0C_status_owner_summary=summary)
    progress['handoffs']['modern']='S0C-status-owner-contract.json + fields + two existingAD36837 frame decodes; no owner/player readiness from status'
    progress['next_steps']=handoff['next_steps'];write('監製進度.json',progress)
    assert record(ROOT/'server-packet-delta.json')==packet
    names=['S0C_status_owner_audit.py','publish_S0C_status_owner.py','S0C-status-owner-contract.json','completeness-audit.json','handoff.json','閱讀入口.md']+[str(p.relative_to(ROOT))for p in(ROOT/'S0C-status-owner').rglob('*')if p.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'report':pin,'delta_files_checked':count,'prior_pins_preserved':len(v['prior_primary_pins']),'delivery':record(ROOT/'delivery-verification.json'),
      'packet_unchanged':packet,'new_wire_differences':0,'skill_owner_or_player_ready':False},ensure_ascii=False))

if __name__=='__main__':main()
