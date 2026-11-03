"""Seal batch9 finite layer contract, preserving all earlier primary pins."""
from audit import *
from seal_delta import seal
def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    report=load('skill-pane-layer-composition-contract.json');verify=load('skill-pane-layer-composition/new-evidence-verification.json')
    cases=load('skill-pane-layer-composition/code-derived-cases.json');extraction=load('skill-pane-layer-composition/layer-extraction.json')
    assert verify['raw_ranges']==16 and verify['hardcoded_instruction_assertions']==54 and len(cases['cases'])==12
    assert verify['range_bytes_and_pins_match']and verify['all_witness_rawbytes_and_decoded_operands_match_fixed_PE']
    assert all(record(Path(x['path']))['sha256']==x['expected_sha256']for x in verify['prior_primary_pins'])
    assert all(record(Path(x['path']))==x for x in report['reusable_outputs'])
    assert extraction['new_pak_members_read_or_extracted']==2 and not extraction['934_replayed']
    pin=record(ROOT/'skill-pane-layer-composition-contract.json');packet=record(ROOT/'server-packet-delta.json')
    readme=ROOT/'skill-pane-layer-composition/READ-ME.txt'
    readme.write_text('''第九批：技能面板圖層與 dense IMG 合成契約

主要入口：../skill-pane-layer-composition-contract.json
本批完成有限靜態追蹤並停在明確 external/surface 邊界；不代表 native GUI 或完整935 numeric LUT 已驗收。
新增16段raw（11 code、5 data），54項固定expected的指令斷言，另101項decoded operand witnesses，共155項指令bytes/operands核對。所有code完整decode。5份既有renderer/initializer/loader/converter/caller raw直接沿用，未重跑其舊cases。12個新synthetic composition cases與6個renderer trigger/order cases。固定PE SHA4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9。

4CCB20 renderer 順序
1. 933：child origin+G+(8,10)。Full DWORD6328C0==0不畫；==31經4B9F60 opaque copy；其他非零經[83154C]，傳alpha=DWORD&FFFF。Payload NULL則跳過該層。
2. 934：child origin+G，payload nonNULL經4BA220 keyed copy，與6328C0獨立。
3. child controls：41B690。
4. owner virtual+24：既有技能grid/selection/time合約。
5. 935：child origin+G+(2,10)，payload nonNULL經[831558]；stack rect為[0,0,signedIMGwidth,signedIMGheight]，無6328C0 gate。
初始table lookup失敗的error branch停在renderer4CCB57/4CCBD1/4CCC50/4CCCD2→private05AD0986，不填補body。所有座標都仍依batch8的live table/payload/G epoch条件。

直接入口與格式
4B9F60、4BA220是cdecl，args為IMG,surface,x,y。83154C入口ECX=IMG、EDX=surface，stack x,y,alpha，ret12；831558入口ECX=IMG、EDX=&rect，stack surface,x,y，ret12。
沿用第七批4C18E0：641298非零時83154C=4BD810→4BD3C0、831558=4BD880→4BE870（565）；為零則4BD7D0→4BD1B0、4BD850→4BEA80（555）。入口thin wrapper與唯一必要body已取完整raw，沒有繼續遞迴surface實作。固定snapshot dispatch pointers為0，live初始化順序/成功不由dump证明。

IMG header與透明
Dense header：signed16 width/height，WORDflag/key，pixels從+8以LE16緊密排列。Draw bodies全部不讀IMG+4 flag。Opaque不比較key，逐row複製width*2 bytes（movsd再movsb），保留zero/bit15，不讀destination。Keyed與兩種blend都無條件比較+6 key；相等就不寫，不相等才copy或讀destination blend。沒有draw-time alpha/key flag開關、縮放或coverage mask。
4BA220不等key的WORD原樣覆寫；不作555/565通道轉換。兩種blend以5-bit B/G/R查table；565 green用(word>>6)&31，舍去green低bit，輸出該bit為0；555忽略bit15。這與opaque/keyed verbatim copy不同。

Native933/935各一次新抽取
LTsurf933：row1556、offset91134715、104676 bytes；191x274、flag0、key4E73。
Member SHA3B49C6E154334E3252BEDCD1FF101D5362F787F5892FF49EE3191FE73EF978E7。
LTsurf935：row1558、offset91359045、105974 bytes；203x261、flag1、key0000。
Member SHA479EFD1EC7E4838FA33881CC0C9E5C387B7D98B4491995733D0F7C53A5191F26。
共同IDX SHA C699CCF22C5F59EB37E8936D939D2632C15AC381980A6FE3F26F2565185193BF；PAK SHA42E676C255B248C295A71097A53B1B67BF73A0C2C3DC0FE11D12BDB9EE79621E。來源hash一次驗證後各slice一次，後續只讀owned extracted files。Exact size=8+width*height*2，來源pixels皆无bit15。933/935 source previews已視覺查看，只是bitmap資源預覽，不是native畫面或合成oracle。
既有sealed indices的default shards為Sprite10/12，未有這些成員；default fallback nativeLTsurf優先於sharedSprite。933c/933h存在sharedSprite，loader先試language variant，所以它們可能在default933之前成功。候選與source pins列在layer-extraction.json；未抽取shared/variant payload。
933/934/935的qualified relative extents分别[8,10,199,284]、[0,0,207,289]、[2,10,205,271]。934尺寸沿用第八批207x289，未重讀934 payload/重新測試。layer-geometry.json的433初始origin數字仅為候選成功载入、drag/G=0的条件结果。

555→565 loader差異
沿用batch8的4D4F20→4BB450：641298非零时所有pixels轉v+(v&7FE0)，header key只在flag!=0才轉，dimensions/flag不变。
Exact933的key4E73有92个source WORD；555 alpha branch會跳過92个。933flag0使565 key保持4E73，但source pixels全部轉，轉後0个WORD等於該key；565 alpha branch因此不因這個key跳過pixel。FullDWORD31 opaque branch兩種格式都複製全部pixels，包括這92個位置。
Exact935有50008个zero-key WORD；flag1使key0仍為0，兩種rect branch均跳過。這些是candidate/static条件，实际variant/cache/format epoch/返回surface未live觀測。

933 alpha table可採用的條件模型
Wrapper選alpha<<11 bytes slice：blue7DC09C、green7EC09C、red7FC09C。每slice有1024 WORD，index=32*D5+S5。Initializer可見整數producer：q=floor((D5*(31-a)+S5*a)/31)，a/D5/S5均0..31；signed reciprocal84210843模型全32768項對得上floor。三個channels按555的0/5/10或565的0/6/11 shift。六份LE16輸出在RGB555/RGB565資料夾與933-alpha-tables.json。
Live採用仍以initializer正常完成、外部helper返回、641298與資源/表/surface一致為條件。FullDWORD觸發與low16 alpha不同：10000h走alpha0，1001Fh走alpha31而非opaque；alpha>=32未clamp，已初始化slice範圍僅0..31，不能推為合法透明值。
非key pixel讀destination，以table合成；每row destY+row<0跳過、每pixel destX+col<0跳過，沒有right check。FullDWORD31 opaque branch不跳key、無這組lower guards，所以不能以alpha31 shader完全替代。

935 table lookup已閉合，integer rounding未閉合
Rect body對非key pixel以32*D5+S5查blue81089C、green81109C、red81189C，混入當時destination。表initializer的FPU expression顺序為(31-max)*double(1/31)*min+max，double constants在4FA328/330。理想数学式為D+S-D*S/31。
4C1972呼叫4F1592，固定PE thunk4F1593直接jmp7741AC20，destination不raw-backed。所缺是浮点轉整數body/捨入/FPU precision state；明确停在此，不用floor、round或preview猜native结果。935-screen-expression-boundary.json仅列入參數與expression examples，native integer_result為null；沒有產生冒稱native的935 LE16 tables。Synthetic rect cases使用明示q=D的seeded tables，僅驗證caller rect、key skip、lookup/coverage與positions，不驗證native935 table值。

Pitch/bounds/surface边界
Surface virtual+64填0x6C descriptor，body讀height+8/pitch+10/buffer+24。所有六個body以2*(signed pitch>>1)作stride，奇數positive pitch向下取偶數。831560被寫成buffer+surfaceHeight*stride。
Unsigned32 bottom pointer guard：buffer+(destY+drawHeight)*stride<=buffer+surfaceHeight*stride；不通過整個draw返回，沒有partial bottom clip，且在加x之前比較。Rect drawHeight=bottom-top，source从IMG+8+2*(top*IMGwidth+left)，dest仍從傳入x/y开始。沒有right/sourceRect bounds check，opaque/keyed/rect無lower x/y guard；只有933alpha逐row/pixel lower guard。Synthetic negative/right/odd-pitch/bottom例用padded owned buffer，不宣稱原生任意座標安全。
Surface virtual+80/+64/+6C實作與live6476D0 vtable/descriptor/lock成功均缺，逐callsite VA記錄於主報告。Negative-result分支可restore/retry和呼叫private05091863，最後retry結果未檢查就用descriptor，不能推成功。Private/surface bodies不擴張。

封存範圍
第九批有限audit完成，935 rounding留精確VA邊界。第三至第八批10個primary/ranges/snapshot/packet pins保持；前批歷史unknown不回寫。沒有934或其他PASS抽樣重跑、整pak批量抽取、native/observer/live input/injection/screenshot、其他組檔案修改或S0政策/历史接管。真正newwire0/newopcode0；GUI/首大廳/整體脫殼仍未驗收。完成此批即停止擴分支。
''',encoding='utf-8')
    reader=ROOT/'閱讀入口.md';text=reader.read_text(encoding='utf-8').split('\n第九批技能面板layer composition：')[0]
    reader.write_text(text+'\n第九批技能面板layer composition：skill-pane-layer-composition-contract.json + skill-pane-layer-composition/READ-ME.txt。16段新raw、54固定expected斷言／155指令bytes核對、12新synthetic cases。933條件層→934主框→controls/grid→935 rect層；native933/935各一次精確抽取。933 alpha整數tables可条件重建，935 lookup/source rect已閉合但4F1593→7741AC20的整数rounding缺body，未猜nativeLUT。Pitch向下取偶數、whole-draw bottom guard、各入口key/lower/right bounds差異明列。保留3..8與packet pins；no934 replay、newwire0、noGUI。\n',encoding='utf-8')
    summary={'evidence':pin,'finite_static_layer_audit_complete':True,'new_raw_ranges':16,'hardcoded_instruction_assertions':54,'instruction_byte_witnesses':155,
      'new_synthetic_cases':12,'new_members_extracted':2,'native_default933_size':[191,274],'native_default935_size':[203,261],
      'renderer_order_and_direct_bodies_closed':True,'933_alpha_integer_tables_conditional_source_closed':True,'935_native_integer_rounding_closed':False,
      '935_external_conversion_boundary':'004F1593→7741AC20','native_GUI_accepted':False,'new_wire_differences':0,'new_opcodes':0}
    complete=load('completeness-audit.json');complete['skill_pane_layer_composition_batch']=summary;write('completeness-audit.json',complete)
    handoff=load('handoff.json');handoff.update(updated_at=stamp(),phase='BATCH9_SKILL_PANE_LAYER_COMPOSITION_STATIC_CONTRACT_SEALED',entrypoint=record(reader),
      skill_pane_layer_composition_contract=pin,finite_skill_pane_layer_composition_audit_complete=True)
    added=[pin,record(readme)]+report['reusable_outputs']
    for group in ['modern','launcher']:
        mapped={x['path']:x for x in handoff[group]};mapped.update({x['path']:x for x in added});handoff[group]=[mapped[k]for k in sorted(mapped)]
    handoff['shared']=[record(ROOT/'completeness-audit.json')if x['path']==str(ROOT/'completeness-audit.json')else x for x in handoff['shared']]
    handoff['next_steps']=[
      'Supervisor/Modern consume batch9 renderer order/direct dense IMG semantics, exactnative933/935 assets and conditional933 alpha outputs with preserved batches6–8.',
      '935 lookup/source rectangle is static-closed but integer table rounding stops at4F1593→7741AC20 outside fixed PE; seeded cases are not native935 LUT or GUI proof.',
      'Preserve batches3–8 and packet SHA; no newwire/opcodes, no934 or other oldPASS replay and no further member extraction in this completed finite batch.',
      'Live variant/cache/format epoch/surface calls/input/GUI remain unaccepted. This finite batch stops here; no autonomous branch expansion.']
    assert handoff['server']['packet_delta']==packet;write('handoff.json',handoff)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=handoff['phase'],current_task='Batch9 finite static layer audit sealed:933/934/controls/grid/935 order and dense IMG contracts;935 integer rounding precisely bounded at external7741AC20.',skill_pane_layer_composition_summary=summary)
    progress['handoffs']['modern']='skill-pane-layer-composition-contract.json + exactnative933/935 + conditional933 alpha tables;935 rounding unresolved; Modern source unchanged'
    progress['next_steps']=handoff['next_steps'];write('監製進度.json',progress)
    assert record(ROOT/'server-packet-delta.json')==packet
    names=['skill_pane_layer_composition_audit.py','extract_skill_pane_layers.py','publish_skill_pane_layer_composition.py','skill-pane-layer-composition-contract.json','completeness-audit.json','handoff.json','閱讀入口.md']+[str(p.relative_to(ROOT))for p in(ROOT/'skill-pane-layer-composition').rglob('*')if p.is_file()]+[str(Path(x['path']).relative_to(ROOT))for x in verify['new_exact_member_pins']]
    count=seal(names,Path(__file__))
    print(json.dumps({'report':pin,'delta_files_checked':count,'prior_primary_pins_preserved':len(verify['prior_primary_pins']),'delivery':record(ROOT/'delivery-verification.json'),
      'packet_unchanged':packet,'new_wire_differences':0,'935_numeric_rounding_closed':False,'native_GUI_accepted':False},ensure_ascii=False))

if __name__=='__main__':main()
