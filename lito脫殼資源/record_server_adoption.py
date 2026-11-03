"""Read the fixed server adoption evidence, then update owned status only."""
from audit import *
from seal_delta import seal

def main():
    source=ROOT.parent/'lito伺服器'/'server-packet-adoption.json';pin=record(source)
    assert pin['sha256']=='6EA8706EF3FC9A225BE7FD8CCE0DED85DB1DF1F16FB7B36C5CCB641484F7430F'
    report=json.loads(source.read_text(encoding='utf-8'));assert report['status']=='ADOPTED_EXISTING_CONTRACT_NO_PRODUCT_CHANGE'
    snapshot=ROOT/'server-packet-evidence/server-adoption-source.txt';snapshot.write_bytes(source.read_bytes())
    ack={'updated_at':stamp(),'source_report':pin,'preserved_source_report':record(snapshot),'adopted_submitted_packet_delta_sha256':report['incoming_packet_delta']['sha256'],'status':report['status'],'candidate':report['accepted_candidate'],'comparisons':report['comparison'],'offline_validation':report['offline_validation'],'product_changes':report['product_changes'],'GUI_accepted':False,'verification_method':'Expected adoption-report SHA checked; server results read and adopted, no server test execution here.'}
    write('server-adoption-ack.json',ack)
    delta=json.loads((ROOT/'server-packet-delta.json').read_text(encoding='utf-8'));status=delta['consumer_status'];status.update(handoff_status='DELIVERED_CONFIRMED',notification_status='DELIVERED_CONFIRMED_BY_SERVER_ADOPTION',sync_status='ADOPTED_EXISTING_CONTRACT_NO_PRODUCT_CHANGE',adoption_status='ADOPTED_EXISTING_CONTRACT_NO_PRODUCT_CHANGE',adoption_evidence=record(ROOT/'server-adoption-ack.json'),adopted_submitted_sha256=report['incoming_packet_delta']['sha256'])
    delta['summary']['meaning']='本批新增opcode0、線上格式更正0；F3/F4/CDH既知契約补证已由伺服器採用。來源/79A7格式一致，24golden+10邊界各通過，無產品變更，GUI未驗收。'
    delta['adoption_ack']=record(ROOT/'server-adoption-ack.json')
    for packet in delta['packets']:
        packet['java_comparison']={'status':'COMPARED_BY_SERVER_CONSISTENT_VALID_PRODUCER_DOMAIN','specific_differences':[],'comparison_evidence':record(ROOT/'server-adoption-ack.json'),'scope':'F3 count0..10 emits0/FFFFFFFF; F4 index0..9, port1..65535. Native accepts other non-sentinel DWORDs and has no local bounds clamps; producer constraints are not native rejection proof.','SQL_change_established':False}
    delta['updated_at']=stamp();write('server-packet-delta.json',delta)
    handoff=json.loads((ROOT/'handoff.json').read_text(encoding='utf-8'));handoff.update(updated_at=stamp(),phase='BATCH3_SKILL_INITIALIZATION_GAPS_AUDITING');handoff['server']={'packet_delta':record(ROOT/'server-packet-delta.json'),'consumer_status':status,'adoption_ack':record(ROOT/'server-adoption-ack.json')};write('handoff.json',handoff)
    reader=ROOT/'閱讀入口.md';text=reader.read_text(encoding='utf-8');text=text.split('\n伺服器交接：')[0]+'\n伺服器交接：F3/F4/CDH補證已由伺服器採用；來源/79A7格式一致，24golden+10邊界各通過，無產品變更，GUI未驗收。證據在server-adoption-ack.json；不再等待轉交。\n';reader.write_text(text,encoding='utf-8')
    handoff['entrypoint']=record(reader);write('handoff.json',handoff)
    progress=json.loads((ROOT/'監製進度.json').read_text(encoding='utf-8'));progress.update(updated_at=stamp(),phase=handoff['phase'],current_task='Finite audit of class4 eight remaining slots and S1E owner/class-order readiness before/after first lobby.',server=status,packet_delta_summary=delta['summary']);progress['handoffs']['server']='Adopted existing F3/F4 contracts; server-adoption-ack.json; no product change, GUI not accepted';progress['next_steps']=['Close class4 slot producer/initial-value qualification','Bound S1E owner/class-order readiness and distinguish S1F/SDA reset','Server independently checks S1E sender timing'];write('監製進度.json',progress)
    n=seal(['record_server_adoption.py','server-adoption-ack.json','server-packet-evidence/server-adoption-source.txt','server-packet-delta.json','handoff.json','閱讀入口.md'],Path(__file__))
    print(json.dumps({'server_status':status['adoption_status'],'adoption_report_sha256':pin['sha256'],'new_files_checked':n}))

if __name__=='__main__':main()
