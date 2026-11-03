"""Seal the finite static handoff; never launches or patches the game."""
from audit import *

def load(name):
    return json.loads((ROOT/name).read_text(encoding='utf-8'))

def replace_text(obj, pairs):
    if isinstance(obj,str):
        for old,new in pairs: obj=obj.replace(old,new)
        return obj
    if isinstance(obj,list): return [replace_text(x,pairs) for x in obj]
    if isinstance(obj,dict): return {k:replace_text(v,pairs) for k,v in obj.items()}
    return obj

def main():
    pe=pefile.PE(str(PE))
    assertions={0x492AD4:'a294f67500',0x43F8D6:'891dcc8e6400',0x47C9D0:'bf74ad6400'}
    for va,raw in assertions.items(): assert pe.get_data(va-0x400000,len(bytes.fromhex(raw))).hex()==raw
    pairs=[('at492AD8','at492AD4'),('at43F8DD','at43F8D6'),('skill metadata tablepointer + count','skill metadata count32 + tablepointer32'),('64_7E20 metadata table and64_7E24 count','64_7E20 count32 and64_7E24 tablepointer32'),('constructorregistration444? subsequent43F6C0 populates','constructor-registration linkage is unclosed;43F6C0 populates')]
    for name in ['skill_initializer_audit.py','lifecycle_audit.py']:
        p=ROOT/name;p.write_text(replace_text(p.read_text(encoding='utf-8'),pairs),encoding='utf-8')
    for name in ['launcher-skill-initialization.json','host-alias-lifecycle.json','skill-initialization/ranges.json']:
        doc=replace_text(load(name),pairs)
        doc['editorial_correction']={'updated_at':stamp(),'proof':'Fixed PE raw bytes verify stores492AD4/43F8D6;647E20 is count,647E24 is returned pointer.'}
        write(name,doc)
    host=load('reconnect-host-audit.json');host['writer_evidence']=record(ROOT/'host-alias-lifecycle.json');write('reconnect-host-audit.json',host)
    modern=load('modern-F4-contract.json');modern['limits']=[x for x in modern['limits'] if x!='Hostname initializer64AD74 remains unknown'];modern['host_initializer_evidence']=record(ROOT/'host-alias-lifecycle.json');modern['limits'].append('Direct host copy and startup inputs established; private/transitive execution and actual runtime host remain unproved.');write('modern-F4-contract.json',modern)
    icons=load('skill-initialization/shared-icon-resources.json');icons['generator']=record(ROOT/'skill_initializer_audit.py');write('skill-initialization/shared-icon-resources.json',icons)
    status=load('resources/validation.json');native=load('resources/LTicon-decode-manifest.json');meta=load('skill-initialization/LTSpellDesc-c-indexed.json')
    shared_unqualified=icons['unqualified']
    completeness={
        'updated_at':stamp(),'scope':'Finite read-only static evidence and resource delivery. No runnable executable restoration performed.',
        'baseline':load('baseline.json')['sources'],
        'PE':{'parseable':True,'snapshot_entry_VA':'004F162A','portable_or_runnable_proven':False,'standard_imports_parsed':0,'import_directory_invalid':True,'all_IAT_or_external_targets_restored':False,'original_TLS_and_reloc_directories_already_absent':True,'ASM_input_PE_differs_from_current':True,'authority':'Current PE raw bytes; ASM provides separately pinned supporting references.'},
        'resource_validation':status,'native_LTicon':{'decoded':native['layout_validated'],'unqualified':native['unqualified'],'declared_range':native['list_metadata']['declared_range'],'missing_ids':native['list_metadata']['missing_declared_LBT_ids'],'contact_sheet_visually_reviewed':True,'native_render_parity':False},
        'shared_skill_icons':{'members':len(icons['outputs']),'missing':icons['missing'],'unqualified':shared_unqualified,'namespace':'Shared SpriteNN/Sprite, distinct from native LTicon'},
        'spell_metadata':{'declared_count':meta['declared_count'],'nonempty_rows':meta['nonempty_rows'],'NULL_slots':len(meta['null_indices']),'count_global':'00647E20','table_pointer_global':'00647E24'},
        'overall_unpacking_complete':False,'GUI_accepted':False,'original_game_launched':False,'process_handle_opened':False,
        'remaining':['Dump-time DLL hashes and private target050104CD body are unavailable','All external branches/IAT/initializers have not been reconstructed','No same-epoch typed host/class/order/metadata/cache owner observations','Current T73 contains142frames withoutS1E; three human GUI failures have no established unpacking root cause','Class4 retains eight unclosed order slots; native graphics alpha/offset parity is unproved'],
        'superseded_outputs':['resources/archive-manifest.partial.json if present','lifecycle/VA-00492E70.bin if present (unaligned old slice); use492F07/full4928C0 instead'],
        'stage_generator_warning':'audit.py,resolve_priority.py,return_api_audit.py publish earlier-stage status; run finish_batch.py last to seal the current handoff.'}
    write('completeness-audit.json',completeness)
    readme='''目前這批靜態證據與資源已可交付；完整脫殼、可攜執行與 GUI 驗收尚未完成。

Modern 先讀 modern-F4-contract.json、priority-api-resolution.json、host-alias-lifecycle.json。F4 的 format 是 502F2C 內嵌 cdh\\0；優先 API 已依固定 PE 分支、歷史 module base 與本機 x86 匯出 RVA 對上。當次 dump 的 DLL hash 缺失，不能將本機 DLL 本體稱為歷史本體。api-vectors.json 保存 12 組本機純 Winsock 測試，測試使用目前 x64 DLL，未操作遊戲或網路連線。

64AD74 的直接寫入已核實：47C9D0..47C9DF 把啟動主機 64B7E4 複製到 saved host 64AD74，含 NUL。啟動預設地址、首個 positional argument、provider/COM 覆寫來源已列出；前置 private050104CD 與完整執行鏈仍未閉合。選角別名另存 64AD54（28 bytes），其後是 port64AD70；兩個欄位不同。固定 snapshot 的空值不能當成真人應送的地址或名稱。

登入器先讀 launcher-return-api.json、launcher-skill-initialization.json。46F094/46F1AF 指向 msvcrt!rand，現存 IAT4F7254 亦保存同一 target；兩個 return_mode 路徑為 rand()%10+6930。技能 class byte 寫入492AD4，icon count 寫入43F8D6；metadata 的647E20是 count32，647E24是 tablepointer32。此批只補原始初始化與資源來源，不重做 T74/T75 owner/reset/teardown 分析。

資源入口是 resources/archive-manifest.json：24 個 archive、305295 筆索引全數通過長度與範圍驗證；首批594個 member 加19個 loose 檔案已提取。另有149個來源引用的共享技能圖示（來源、shard route、解碼資格見 skill-initialization/shared-icon-resources.json）。索引逐 row 保存身份；共享 Text 同名不得默認取第一或最後一筆。

Native LTicon 的226個 LBT 已解析並產出 PNG；LTlist.ico 是文字宣告233，缺少173..179七個 ID，保留缺口。resources/LTicon-contact-sheet.png 已目視檢查。LTSpellDesc-c.tbl 宣告176槽位，147筆非空資料，29個依原載入規則保留NULL；詳細索引在 skill-initialization/LTSpellDesc-c-indexed.json。預覽的原生 alpha、offset、blit parity 尚未驗收。

重現提取：以已安裝 Python 執行 extract_member.py --archive LTtext --row N（N取自索引 CSV）。工具先驗證原 idx/pak SHA256，輸出 raw/decoded/provenance 到本目錄 resources/on-demand。Rust codec 原始碼、可執行檔及編譯來源都在 codec；只接受已驗證的格式與16MB member 上限。

完整性判定見 completeness-audit.json、pe-audit.json。固定 PE 的 import directory 無法正常解析，尚未全面重建 IAT、外部分支與初始化；原始 BIN 原本就沒有 TLS/relocation 目錄，不能將目錄缺省一概稱作丟失。BIN/snapshot 各37個 embedded resource leaf 已保留，34個 bytes 不同。ASM 的輸入 PE 指紋不同，關鍵結論以固定 PE raw bytes 為準。

T73 舊會話已退出，142 frames 沒有S1E且未取得 typed skill owner。首次大廳技能空、返回不能再入、頻道返回無作用的真人FAIL，尚未歸因為脫殼缺失。後續需要由登入器的新隔離會話取得同一 epoch 的 host/class/order/metadata/cache/owner 與封包證據；本批沒有启动原遊戲、開 process handle 或變更 provider。

機器入口 handoff.json；持續狀態 監製進度.json；artifact-manifest.json 封存本批檔案大小及SHA256，verify_delivery.py 可讀取核對。除明列 superseded 舊 slice/partial 外，以新 manifest 及其引用為準。每個早期產生器可能回寫早期進度，重新產生後須最後執行 finish_batch.py。
'''
    (ROOT/'閱讀入口.md').write_text(readme,encoding='utf-8')
    modern_names=['modern-F4-contract.json','priority-api-resolution.json','priority-complete-tails.json','host-alias-lifecycle.json','reconnect-host-audit.json','api-vectors.json']
    launcher_names=['launcher-return-api.json','launcher-skill-initialization.json','skill-initialization/ranges.json','skill-initialization/shared-icon-resources.json','skill-initialization/LTSpellDesc-c-indexed.json','host-alias-lifecycle.json','pe-audit.json']
    launcher_names +=[n for n in ['class4-slot-provenance.json','S1E-initialization-gates.json','skill-gates/ranges.json','skill-gates/new-evidence-verification.json'] if (ROOT/n).is_file()]
    shared_names=['baseline.json','completeness-audit.json','resources/archive-manifest.json','resources/validation.json','resources/LTicon-decode-manifest.json','codec/build.json','extract_member.py']
    shared_names=[n for n in shared_names if (ROOT/n).is_file()]
    handoff={'schema':2,'updated_at':stamp(),'phase':'BATCH2_STATIC_HANDOFF_SEALED','entrypoint':record(ROOT/'閱讀入口.md'),'modern':[record(ROOT/n) for n in modern_names],'launcher':[record(ROOT/n) for n in launcher_names],'shared':[record(ROOT/n) for n in shared_names],'artifact_manifest':str(ROOT/'artifact-manifest.json'),'finite_batch_complete':True,'overall_complete':False,'GUI_accepted':False,'next_steps':completeness['remaining']}
    if (ROOT/'server-packet-delta.json').is_file():
        server=load('server-packet-delta.json')
        handoff['server']={'packet_delta':record(ROOT/'server-packet-delta.json'),'consumer_status':server['consumer_status']}
        with (ROOT/'閱讀入口.md').open('a',encoding='utf-8') as f:f.write('\n伺服器交接：server-packet-delta.json；方向／opcode／欄位／端序／狀態／來源及Java比較、同步、採用狀態均在清單。\n')
        handoff['entrypoint']=record(ROOT/'閱讀入口.md')
    if (ROOT/'S1E-initialization-gates.json').is_file():
        handoff['phase']='BATCH3_SKILL_GAPS_STATIC_SEALED';handoff['finite_skill_gap_audit_complete']=True;handoff['skill_runtime_complete']=False
        with (ROOT/'閱讀入口.md').open('a',encoding='utf-8') as f:f.write('\n第三批技能初始化：class4-slot-provenance.json及S1E-initialization-gates.json列出32格producer/8格runtime未知、metadata fallback91、owner/class-order依賴及S1F/SDA placeholder區分。资源存在不等于技能填入。\n')
        handoff['entrypoint']=record(ROOT/'閱讀入口.md')
    write('handoff.json',handoff)
    excluded={'監製進度.json','artifact-manifest.json','delivery-verification.json'}
    files=[p for p in ROOT.rglob('*') if p.is_file() and '__pycache__' not in p.parts and p.name not in excluded and not p.name.endswith('.tmp') and '.partial.' not in p.name and p.name!='VA-00492E70.bin']
    write('artifact-manifest.json',{'updated_at':stamp(),'generation':record(Path(__file__)),'source_provenance':'Individual JSON manifests retain source PE/archive/index/member offset/row/hash and bounded VA/RVA/fileoffset evidence. This manifest seals local delivery files.','entries':[record(p) for p in sorted(files)],'excluded_mutable_status':sorted(excluded),'superseded':completeness['superseded_outputs']})
    progress={'updated_at':stamp(),'phase':handoff['phase'],'current_task':'Finite static batch completed and sealed; no active game work.','actual_artifacts':[record(ROOT/n) for n in ['handoff.json','artifact-manifest.json']+shared_names+modern_names+launcher_names],'verification':{'three_baselines_match':True,'native_started':False,'process_opened':False,'GUI_accepted':False,'finite_batch_complete':True},'gaps':completeness['remaining'],'handoffs':{'modern':modern_names,'launcher':launcher_names},'next_steps':['Consumer reads this sealed batch','New isolated-session runtime observations remain the launcher group responsibility']}
    if 'server' in handoff:
        progress['server']=handoff['server']['consumer_status'];progress['packet_delta_summary']=server['summary']
        progress['actual_artifacts'].append(record(ROOT/'server-packet-delta.json'))
        progress['handoffs']['server']='server-packet-delta.json; supervisor forwards, consumer adoption tracked separately'
    write('監製進度.json',progress)
    print(json.dumps({'files_sealed':len(files),'shared_icons':len(icons['outputs']),'shared_unqualified':shared_unqualified,'baseline_match':all(x['verified'] for x in completeness['baseline'])}))

if __name__=='__main__':main()
