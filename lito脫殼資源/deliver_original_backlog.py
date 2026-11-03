"""Deliver two qualified RGBA buffers and reconcile the original task backlog.

No original containers are scanned, no private bodies are expanded, and all
writes stay beside this script. Run with Python -B.
"""
import sys
sys.dont_write_bytecode = True
from audit import ROOT, record, write, stamp, sha
from extract_resources import decode_img
from seal_delta import seal
from PIL import Image
import json


def load(name):
    return json.loads((ROOT / name).read_text(encoding='utf-8'))


def main():
    manifest = load('resources/archive-manifest.json')
    manifest_pin = record(ROOT / 'resources/archive-manifest.json')
    assert manifest_pin['sha256'] == '57D933561A95F78D4EEC8ADB6327A51CE48119044E5B9CDA81D052B7914E9ECB'
    packet_before = record(ROOT / 'server-packet-delta.json')
    incident_before = record(ROOT / 'CD1-mode-step/scope-write-correction.json')
    provider_before = record(ROOT / 'Native-provider-writer-contract.json')
    assert provider_before['sha256'] == 'F989981DFF8FF08C98E6B880CD88BD69C0DB420388FF41F957BCB1D06AA027B8'
    output_dir = ROOT / 'resources' / 'modern-rgba'
    output_dir.mkdir(exist_ok=True)
    assets, checks = [], []
    for asset_id in (6314, 7482):
        matches = [m for m in manifest['selected_members'] if m['archive'] == 'LTsurf' and m['name'] == f'{asset_id}.img']
        assert len(matches) == 1
        member = matches[0]
        raw_path = ROOT / 'resources' / 'raw' / 'LTsurf' / f"row{member['row']:05d}-{member['name']}"
        preview_path = ROOT / 'resources' / 'previews' / 'LTsurf' / (raw_path.name + '.png')
        assert record(raw_path) == member['raw']
        assert record(preview_path) == member['preview']['output']
        raw = raw_path.read_bytes()
        image, details = decode_img(raw, True)
        rgba = image.tobytes()
        assert (image.width, image.height, len(rgba)) == (640, 480, 1228800)
        assert details['flag'] == 0 and details['key'] == 26320
        assert sha(rgba) == member['preview']['rgba_sha256']
        with Image.open(preview_path) as reference:
            reference.load()
            assert reference.size == image.size
            assert reference.convert('RGBA').tobytes() == rgba
        destination = output_dir / f'{asset_id}.rgba'
        assert not destination.exists(), 'Do not replay normalization of an existing delivered buffer'
        destination.write_bytes(rgba)
        pin = record(destination)
        assert pin['bytes'] == 640 * 480 * 4 and pin['sha256'] == details['rgba_sha256']
        assets.append({
            'id': asset_id, 'namespace': 'native', 'archive': 'LTsurf',
            'row': member['row'], 'member_name': member['name'],
            'source_offset': member['offset'], 'source_length': member['bytes'],
            'source_index_sha256': member['source_index_sha256'],
            'source_pak_sha256': member['source_pak_sha256'],
            'provenance': 'Previously sealed bounded PAK member extraction; original containers not reread in this delivery',
            'raw': member['raw'], 'reference_png': member['preview']['output'],
            'rgba': pin, 'width': 640, 'height': 480, 'stride_bytes': 2560,
            'byte_order': 'R,G,B,A', 'row_order': 'top-to-bottom', 'header_bytes': 0,
            'alpha': 'straight; color-key pixels become 0,0,0,0; other pixels alpha255',
            'decode': details, 'scene_binding': None, 'draw_position': None,
            'native_render_parity': False,
        })
        checks.append({'id': asset_id, 'raw_and_reference_file_pins_match': True,
                       'qualified_header_and_exact_pixel_payload': True,
                       'every_output_byte_equals_existing_PNG_RGBA': True,
                       'persisted_output_length_and_sha_match': True})
    write('resources/modern-rgba/manifest.json', {
        'updated_at': stamp(), 'schema': 'native-qualified-rgba-v1',
        'generator': record(__file__), 'decoder': record(ROOT / 'extract_resources.py'),
        'archive_manifest': manifest_pin, 'assets': assets, 'targeted_verification': checks,
        'new_archive_member_extractions': 0, 'new_rgba_buffers': 2,
        'scope': 'Additional directly loadable buffer representation of two existing assets; no new asset identity, scene binding or runtime proof',
        'consumer_note': 'Read exactly width*height*4 bytes, verify SHA256, then use RGBA8 with stride2560. Existing Modern decode_img can also read the original IMG; normalization does not close the scene/GUI blocker.',
        'scene_gate': 'Need an accepted mapping of 64AFA0/scene owner and callsite to a named scene before assigning either image to Directory',
    })
    (output_dir / 'README.md').write_text('''# Native RGBA 載入資料

6314／7482 各為 640×480、1,228,800 bytes 的無 header RGBA8，順序 R,G,B,A，每列 2560 bytes，由上往下。來源是既有 LTsurf 原始 IMG；本次沒有重新抽取 archive。完整 row、offset、length、來源 SHA、色鍵與輸出 SHA 在 manifest.json。

兩個輸出逐 byte 等於既有 PNG 的 RGBA 像素。RGB555 以 `(v<<3)|(v>>2)` 展開；色鍵 26320 轉成全零透明像素，其餘 alpha255，為 straight alpha。這沿用既有參考解碼，尚未證明 Native renderer 一致性。Modern 既有 decode_img 也能直接解碼原始 IMG，這份資料省去消費端再次解碼，沒有解除場景映射阻塞。

接收方：Modern 資源組。讀取 manifest 的 rgba 路徑，核對 byte 數及 SHA 後可交給 RGBA compositor。尚缺 64AFA0／scene owner 到具名子場景的獨立對應證據；draw_position 與 scene_binding 保留 null。不可只因 640×480 就指定為 Directory 背景。
''', encoding='utf-8')

    # Each unresolved original deliverable names the exact next independent input
    # and a receiving group. A historical finite PASS is not overall completion.
    rows = [
        ('PE/OEP／可攜執行檔', 'Snapshot 可解析、entryVA004F162A；37 embedded leaves 已交付。未交付可攜執行檔。',
         '完整匯入與外部跳轉來源版本、loader/initializer 需求、修復後可重現的啟動驗收；OEP snapshot 不能取代這些證據。', '登入器／PE 組', '本組不改 PE/BIN、不執行 Native。'),
        ('IAT／外部函式', 'standard imports0、import directory invalid；四個 priority API 為歷史 base＋本機 exportRVA 的有限比對。',
         'Dump 同期 DLL 原始檔及 SHA；每個未解析 thunk/外部目標的模組、export/forwarder 與版本對應清單，才可重建 IAT。', '登入器／PE 組', '本機 DLL 不等於 dump 同期 DLL；不重跑既有 Winsock PASS。'),
        ('Host／啟動初始化', '64AD74 writer、64AD54 alias 與64AD70 port已分離；私有050104CD前提與整段實際執行未閉合。',
         '已接受的050104CD前提/回傳契約，或已有同一期 host 初始化、alias選擇、owner與執行順序記錄。', '登入器組＋監製', '本次不展開私有body、不新增runtime capture。'),
        ('Skill／class/order/metadata owner', '32個明確class4 producer與176 metadata slots已交付；147非空/29NULL；早期T73缺S1E與後續單一S1E均不是owner證據。',
         '同一期完整 S1E payload、class、order、metadata/cache owner及reset/teardown順序；class4 slots21/22/23/29/30/37/38/39 的實際producer證據。', '登入器組＋Modern組；監製整合同期證據', '不得把較晚frame存在提升為typed owner已驗收。'),
        ('Native 圖形／skill pane 一致性', '226 LTicon PNG、149 shared skill icons、933/934/935及座標/色表/層次契約已交付。',
         '935 conversion邊界4F1593→7741AC20的同期模組/rounding契約；實際初始化LUT、pitch、signed offset、alpha/blit與對照像素驗收。', '登入器／Native驗收組＋Modern渲染組', '參考解碼與靜態色表不等於Native GUI parity。'),
        ('LTicon 173..179', 'LTlist.ico宣告233，qualified source實際decoded226；七個ID缺少qualified原始member。',
         '每個缺ID所屬來源版本、namespace、明確member row/offset/length/encoding及原始blob SHA，或已接受的缺失/替代政策。', '資源組接收證據；監製決定替代政策', '不跨版本猜圖、不掃全archive當成進度。'),
        ('選角人物PNG／IMG79/80 provider', '兩個bank建立、leaf ctor、filename route與Sprite/Sprite03 constructor stem已閉合；實際backing identity未閉合。',
         '一筆已接受provider→實際檔案/來源SHA→精確member offset/length/encoding→原始blob契約，先選一個ordinal即可；matching stem不足。', '監製接收獨立來源證據；資源組有來源後抽取；Modern組綁定', '42E550/42E6D0/42EA50保持STOPPED；不逐批向下展開。'),
        ('背景／class variant／動畫生命周期', '四個已綁按鈕與12表48states既有；6314/7482 raw+PNG已交付，本次補2個RGBA buffer。',
         '64AFA0 writer/scene owner到具名子場景映射；class/variant語義、selected+3D啟動/到期/reset與timer/prefetch redraw的獨立契約。', 'Modern UI組＋登入器證據組；監製整合', 'RGBA完成，scene_binding/draw_position仍null；不把variant自行命名sex。'),
        ('Map 候選到 runtime mapping', '五個既有LTmapstr的完整token/row與離線候選index已交付。',
         'Runtime key/formatter/private05040E1D與外部773F5200輸出對實際candidate來源/row的已接受binding記錄。', 'Modern map組＋監製', '離線candidate不是已載入地圖；不重跑完整候選解析。'),
        ('Modern 採用／真連線／GUI', 'Batch33/34有限source comparison及8個候選UI可達已交付；R526 D2/E1由監製報告完成，非本組重開缺口。',
         'Modern組封存R527結果、目前實際採用/合併清單及default入口證據；完成受授權的真連線與GUI驗收後回交監製。', 'Modern組＋監製', '本組未驗收R527完成，不編譯/改Modern、不干擾在途工作。'),
        ('Server wire 接收', 'F3/F4已有adoption ack；後續有限wire/source契約交付且本次wire差異0。',
         '只有發現新的精確wire差異才產生有opcode/欄位/byte witness的通知；真連線對比由監製/既有執行組提供。', '監製＋伺服器組', '沒有新wire不發通知；不承接伺服器被拒工作。'),
        ('其餘 archive／未qualified格式', '24 archives/305295 records範圍已驗證，594初始members/19loose＋後續限定輸出；不是全量GUI語義解碼。',
         '具體consumer需求與可驗證namespace/member row/offset/length/encoding、缺少的輸出格式；有已qualified來源才做最小抽取。', '資源組；Modern提出具體consumer契約', '現在無其他已證明未抽取的必要member；不批量抽全部305295 records。'),
    ]
    backlog = {'updated_at': stamp(), 'overall_complete': False, 'review_scope': 'Original PE/OEP/IAT/external/init/resources against existing Batch1–38 handoffs; not just latest finite stage',
               'new_usable_outputs': record(output_dir / 'manifest.json'),
               'closed_in_this_delivery': 'Two RGBA8 buffers and descriptors from existing known IMG sources',
               'new_unextracted_qualified_members_found': 0,
               'adoption_limit': 'R526 adoption status inherited from supervisor; no fresh R527 runtime or GUI acceptance claimed',
               'remaining': [{'deliverable': a, 'already_delivered': b, 'missing_independent_evidence': c, 'receiver': d, 'boundary': e} for a,b,c,d,e in rows],
               'retained_stopped_private_branches': ['42E550', '42E6D0', '42EA50'],
               'scope_write_correction': incident_before, 'server_packet': packet_before,
               'server_notification_requested': False, 'GUI_accepted': False}
    write('original-delivery-backlog.json', backlog)
    text = '# 原始交付總待辦：整體未完成\n\n最新有限階段完成，不代表原始 PE/OEP/IAT、動態初始化、資源綁定與 GUI 驗收全部完成。本次實際補齊兩張既有背景的直接載入 RGBA 與來源描述；未發現其他已 qualified、必要但未抽取的 member。\n\n'
    text += '|原始交付|已交付／採用|仍缺的獨立證據|接收組別／邊界|\n|---|---|---|---|\n'
    text += '\n'.join('|'+ '|'.join((a,b,c,d+'；'+e))+'|' for a,b,c,d,e in rows)
    text += '\n\n本次無新wire差異，不通知伺服器。Private backing三支仍停止。既有scope-write事件保持open，先前外部pyc清理的自動批准審查以「blocked by policy」拒絕；不重試、不轉交繞過。\n'
    (ROOT / 'original-delivery-backlog.md').write_text(text, encoding='utf-8')
    next_steps = ['以original-delivery-backlog.json/.md管理完整原始交付，不以最新finite_batch_complete代表overall_complete。',
                  'Modern可接收resources/modern-rgba/manifest.json與6314/7482.rgba；原始IMG仍可由既有decoder讀取。先取得scene mapping再指定背景用途。',
                  '資源下一筆實作需具體consumer缺口＋qualified來源/offset/length/encoding；当前無其他已證明未抽取的必要member。',
                  '登入器/PE組接收同期DLL/IAT、host/init及skill owner證據需求；Modern組封存目前採用與R527/GUI結果，監製整合。',
                  '42E550/42E6D0/42EA50保持STOPPED；scope-write事件及被拒清理維持原紀錄，不重試；無新wire不通知伺服器。']
    complete = load('completeness-audit.json')
    complete.update(updated_at=stamp(), original_delivery_backlog=record(ROOT / 'original-delivery-backlog.json'),
                    remaining=[a+'：'+c+' 接收：'+d for a,b,c,d,e in rows],
                    modern_rgba_delivery=record(output_dir / 'manifest.json'), overall_unpacking_complete=False)
    write('completeness-audit.json', complete)
    phase = 'ORIGINAL_DELIVERY_RECONCILED_TWO_RGBA_BUFFERS_DELIVERED_OVERALL_OPEN_PRIVATE_BRANCHES_STOPPED'
    h = load('handoff.json')
    new_names = ['resources/modern-rgba/6314.rgba','resources/modern-rgba/7482.rgba','resources/modern-rgba/manifest.json','resources/modern-rgba/README.md','original-delivery-backlog.json','original-delivery-backlog.md']
    for key in ('modern', 'launcher', 'shared'):
        entries = {p['path']:p for p in h[key]}
        selected = new_names if key == 'modern' else new_names[-2:]
        for name in selected: entries[str(ROOT/name)] = record(ROOT/name)
        if str(ROOT/'completeness-audit.json') in entries: entries[str(ROOT/'completeness-audit.json')] = record(ROOT/'completeness-audit.json')
        h[key] = list(entries.values())
    h.update(updated_at=stamp(), phase=phase, next_steps=next_steps, overall_complete=False,
             original_delivery_backlog=record(ROOT/'original-delivery-backlog.json'), modern_rgba_delivery=record(output_dir/'manifest.json'))
    assert h['scope_write_correction'] == incident_before
    write('handoff.json', h)
    entry = ROOT/'閱讀入口.md'
    prefix = '''# 目前總交付狀態：整體未完成

完整原始待辦見 `original-delivery-backlog.md`／`.json`，逐項包含已交付、缺少獨立證據與接收組別。不要把下方歷史 Batch1–38 的有限階段完成當成整体完成。

本次補齊 `resources/modern-rgba/6314.rgba`、`7482.rgba` 與 `manifest.json`：已知 LTsurf 來源的直接載入 RGBA8，逐 byte 符合既有預覽；不授 scene/Directory 映射或 Native GUI parity。没有新增archive member抽取。Private42E550/42E6D0/42EA50仍停止。

---

'''
    entry.write_text(prefix+entry.read_text(encoding='utf-8'), encoding='utf-8')
    progress = load('監製進度.json')
    progress.update(updated_at=stamp(), phase=phase,
                    current_task='原始交付總待辦已重整；本次2個已qualified背景RGBA buffer已交付；PE/IAT/同期初始化/資源runtime binding/GUI整體仍未完成。',
                    next_steps=next_steps, gaps=complete['remaining'], overall_complete=False,
                    original_delivery_backlog=record(ROOT/'original-delivery-backlog.json'),
                    modern_rgba_delivery=record(output_dir/'manifest.json'))
    progress['handoffs']['modern'] = 'resources/modern-rgba/manifest.json＋2個RGBA buffer；original-delivery-backlog；保留已交付B1–38，R527未干擾。'
    write('監製進度.json', progress)
    assert record(ROOT/'server-packet-delta.json') == packet_before
    assert record(ROOT/'CD1-mode-step/scope-write-correction.json') == incident_before
    assert record(ROOT/'Native-provider-writer-contract.json') == provider_before
    count = seal(new_names+['deliver_original_backlog.py','completeness-audit.json','handoff.json','閱讀入口.md'], ROOT/'deliver_original_backlog.py')
    print(json.dumps({'changed_files_verified':count,'RGBA_buffers':2,'RGBA_bytes_total':sum(a['rgba']['bytes'] for a in assets),
                      'per_asset_pixel_count':640*480,'backlog_deliverables':len(rows),'overall_complete':False,
                      'manifest':record(output_dir/'manifest.json'),'backlog':record(ROOT/'original-delivery-backlog.json')},ensure_ascii=True))


if __name__ == '__main__':
    main()
