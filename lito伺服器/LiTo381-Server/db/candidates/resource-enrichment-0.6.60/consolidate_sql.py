"""未匯入、未接線。Generate isolated review SQL from already extracted JSON.
No SQL execution, no DB driver, no production writes. MySQL5.5 utf8/LONGTEXT.
"""
import sys
sys.dont_write_bytecode=True
import re,json
from pathlib import Path
import extract_validate as b
P=Path(__file__).resolve().parent
DOMAINS={'maps':('lito_cand_map',['map_id']),'portals':('lito_cand_portal',['source_map_id','source_x','source_y']),'skills':('lito_cand_skill',['skill_id','binding']),'zones':('lito_cand_zone',['zone_key']),'map_items':('lito_cand_map_item',['item_id']),'rental_offers':('lito_cand_rental_offer',['resource_index']),'npc_identities':('lito_cand_npc_identity',['resource_index']),
 'skill_phases':('lito_cand_skill_phase',['presentation_id']),'projectiles':('lito_cand_projectile',['projectile_key']),'buffs':('lito_cand_buff',['skill_id']),'map_pages':('lito_cand_map_page',['wire_page']),'rental_mappings':('lito_cand_rental_mapping',['resource_index']),'rental_currency':('lito_cand_rental_currency',['currency_key']),
 'config_references':('lito_cand_config_reference',['reference_id']),'map_effects':('lito_cand_map_effect',['effect_record_id']),'item_resources':('lito_cand_item_resource',['resource_index']),'npc_services':('lito_cand_npc_service',['service_id']),'skill_requests':('lito_cand_skill_request',['skill_id']),'lineage_groups':('lito_cand_lineage_group',['group_id'])}
NEW={
 'lito_cand_skill_phase':'presentation_id INT UNSIGNED PRIMARY KEY, skill_id SMALLINT UNSIGNED NOT NULL, binding VARCHAR(96) NOT NULL, phase VARCHAR(96) NOT NULL, profile VARCHAR(96) NOT NULL, effect_id SMALLINT UNSIGNED NULL, role TEXT NOT NULL, resource_version VARCHAR(191) NOT NULL, source_id INT UNSIGNED NOT NULL, enabled TINYINT NOT NULL DEFAULT 0',
 'lito_cand_projectile':'projectile_key VARCHAR(96) PRIMARY KEY, skill_id SMALLINT UNSIGNED NULL, projectile_gfx SMALLINT UNSIGNED NOT NULL, packet_opcode TINYINT UNSIGNED NOT NULL, flight_type TINYINT UNSIGNED NOT NULL, resource_version VARCHAR(96) NOT NULL, impact_gfx SMALLINT UNSIGNED NULL, enabled TINYINT NOT NULL DEFAULT 0',
 'lito_cand_buff':'skill_id SMALLINT UNSIGNED PRIMARY KEY, icon_id SMALLINT UNSIGNED NOT NULL, skill_panel_icon SMALLINT UNSIGNED NULL, status_slot SMALLINT UNSIGNED NOT NULL, packet_opcode TINYINT UNSIGNED NOT NULL, packet_selector SMALLINT UNSIGNED NOT NULL, server_duration_seconds INT UNSIGNED NOT NULL, resource_duration_seconds INT UNSIGNED NULL, off_icon_id SMALLINT UNSIGNED NULL, clear_duration_seconds INT UNSIGNED NOT NULL, mapping_kind VARCHAR(191) NOT NULL, verification TEXT NOT NULL, resource_version VARCHAR(191) NOT NULL, asset LONGTEXT NULL, resource_named_icon SMALLINT UNSIGNED NULL, semantic_conflict TINYINT NULL, enabled TINYINT NOT NULL DEFAULT 0',
 'lito_cand_map_page':'wire_page TINYINT UNSIGNED PRIMARY KEY, zero_based_index TINYINT UNSIGNED NOT NULL, image_resource_id SMALLINT UNSIGNED NOT NULL, display_map_id SMALLINT UNSIGNED NOT NULL, asset LONGTEXT NOT NULL, pe_source_id INT UNSIGNED NOT NULL, table_virtual_address VARCHAR(32) NOT NULL, enabled TINYINT NOT NULL DEFAULT 0',
 'lito_cand_rental_mapping':'resource_index SMALLINT UNSIGNED PRIMARY KEY, retained_source_item_id INT UNSIGNED NOT NULL, native_description VARCHAR(191) NOT NULL, native_wire_name_id SMALLINT UNSIGNED NOT NULL, icon_asset LONGTEXT NULL, same_name_candidates LONGTEXT NOT NULL, mapped_shared_item_id INT UNSIGNED NULL, mapping_proven TINYINT NOT NULL, enabled TINYINT NOT NULL DEFAULT 0',
 'lito_cand_rental_currency':'currency_key VARCHAR(96) PRIMARY KEY, retained_item_id INT UNSIGNED NOT NULL, native_resource_index SMALLINT UNSIGNED NOT NULL, native_wire_name_id SMALLINT UNSIGNED NOT NULL, native_description VARCHAR(191) NOT NULL, mapped_shared_item_id INT UNSIGNED NULL, same_name_shared_candidates LONGTEXT NOT NULL, official_crosswalk_confirmed TINYINT NOT NULL, enabled TINYINT NOT NULL DEFAULT 0',
 'lito_cand_config_reference':'reference_id INT UNSIGNED PRIMARY KEY, target_table VARCHAR(64) NOT NULL, target_key VARCHAR(191) NOT NULL, observed_at_utc VARCHAR(48) NOT NULL, payload LONGTEXT NOT NULL, origin_kind VARCHAR(128) NOT NULL, validation LONGTEXT NOT NULL, source_id INT UNSIGNED NOT NULL, enabled TINYINT NOT NULL DEFAULT 0',
 'lito_cand_map_effect':'effect_record_id INT UNSIGNED PRIMARY KEY, map_id SMALLINT UNSIGNED NOT NULL, sector VARCHAR(8) NOT NULL, sprite_id SMALLINT UNSIGNED NOT NULL, raw_x SMALLINT UNSIGNED NOT NULL, raw_y SMALLINT UNSIGNED NOT NULL, x SMALLINT UNSIGNED NULL, y SMALLINT UNSIGNED NULL, extended_value INT NULL, actor_role VARCHAR(64) NULL, record_offset INT UNSIGNED NOT NULL, source_id INT UNSIGNED NOT NULL, verification VARCHAR(191) NOT NULL, enabled TINYINT NOT NULL DEFAULT 0',
 'lito_cand_item_resource':'resource_index SMALLINT UNSIGNED PRIMARY KEY, wire_name_id SMALLINT UNSIGNED NOT NULL, native_description VARCHAR(191) NOT NULL, icon_asset LONGTEXT NULL, mapped_shared_item_id INT UNSIGNED NULL, classification VARCHAR(96) NOT NULL, shared_name_text LONGTEXT NULL, source_id INT UNSIGNED NOT NULL, enabled TINYINT NOT NULL DEFAULT 0',
 'lito_cand_npc_service':'service_id INT UNSIGNED PRIMARY KEY, npc_resource_index SMALLINT UNSIGNED NOT NULL, role VARCHAR(64) NOT NULL, html_member VARCHAR(96) NULL, action_text VARCHAR(191) NULL, native_description VARCHAR(191) NOT NULL, service_status VARCHAR(191) NOT NULL, actor_gfx SMALLINT UNSIGNED NULL, x SMALLINT UNSIGNED NULL, y SMALLINT UNSIGNED NULL, source_id INT UNSIGNED NOT NULL, enabled TINYINT NOT NULL DEFAULT 0',
 'lito_cand_skill_request':'skill_id SMALLINT UNSIGNED PRIMARY KEY, raw_selector_index SMALLINT UNSIGNED NOT NULL, dispatch_class TINYINT UNSIGNED NOT NULL, handler_va VARCHAR(32) NOT NULL, packet_shape VARCHAR(32) NULL, target_route VARCHAR(191) NOT NULL, direct_cast_conditional TINYINT NULL, availability_verified TINYINT NULL, source_id INT UNSIGNED NOT NULL, resource_version VARCHAR(96) NOT NULL, enabled TINYINT NOT NULL DEFAULT 0',
 'lito_cand_lineage_group':'group_id SMALLINT UNSIGNED PRIMARY KEY, target_table VARCHAR(64) NOT NULL, group_name VARCHAR(64) NOT NULL, field_name VARCHAR(64) NOT NULL, interpretation VARCHAR(191) NOT NULL',
}
APPEND={
 'lito_cand_skill':['usable_skill TINYINT NULL','presentation_status VARCHAR(191) NULL'],
 'lito_cand_map_item':['native_page_verified TINYINT NULL','native_image_resource SMALLINT UNSIGNED NULL','native_display_map_id SMALLINT UNSIGNED NULL','original_item_page_binding_confirmed TINYINT NULL','resource_text VARCHAR(191) NULL','text_source_id INT UNSIGNED NULL','text_locator VARCHAR(96) NULL','shared_icon_asset LONGTEXT NULL'],
 'lito_cand_rental_offer':['mapped_shared_item_id INT UNSIGNED NULL','mapping_proven TINYINT NULL'],
}
def main():
    data=b.readjson(P/'candidate-data.json');sources=b.readjson(P/'sources.json');prov=b.readjson(P/'provenance.json');validation=b.readjson(P/'validation.json')
    schema=(P/'01-candidate-schema.sql').read_text(encoding='utf-8');defs={m[1]:m[2] for m in re.finditer(r'CREATE TABLE `([^`]+)` \((.*?)\) ENGINE=InnoDB DEFAULT CHARSET=utf8;',schema)}
    for domain,(table,_) in DOMAINS.items():
        if domain in data and table in NEW:defs[table]=NEW[table]
    for table,cols in APPEND.items():
        for col in cols:
            if not re.search(r'(?:^|, )'+col.split()[0]+r' ',defs[table]):defs[table]+=', '+col
    rows={'lito_cand_source':sources,'lito_cand_provenance':prov}
    for domain,(table,keys) in DOMAINS.items():
        if domain not in data:continue
        rows[table]=data[domain];b.unique(rows[table],keys)
    assert set(defs)==set(rows)
    fields={k:re.findall(r'(?:^|, )([a-z_]+) (?:INT|SMALLINT|TINYINT|BIGINT|VARCHAR|CHAR|TEXT|LONGTEXT)',v) for k,v in defs.items()}
    header='-- 未匯入、未接線。2026-10-14 補強候選；MySQL5.5.22；不可直接對正式庫執行。\n-- 所有候選 enabled=0；NULL為未知；現行設定payload僅 dated SELECT 參考。\n-- 不指定USE、不建資料庫、不覆寫正式表。SQL僅經離線詞法/數據驗證，未交給MySQL執行。\nSET NAMES utf8;\n'
    ddl=header+'\n'.join('CREATE TABLE `'+k+'` ('+v+') ENGINE=InnoDB DEFAULT CHARSET=utf8;' for k,v in defs.items())+'\n'
    dml=header+'START TRANSACTION;\n'+''.join(b.insert(k,rows[k],fields[k]) for k in defs)+'COMMIT;\n'
    assert not re.search(r'^\s*(DROP|TRUNCATE|DELETE|UPDATE|REPLACE|ALTER|USE|CREATE DATABASE)\b',ddl+dml,re.M|re.I)
    assert all(ord(c)<65536 for c in ddl+dml) and '\ufffd' not in ddl+dml
    (P/'01-candidate-schema.sql').write_text(ddl,encoding='utf-8');(P/'02-candidate-data.sql').write_text(dml,encoding='utf-8')
    b.writejson('schema-map.json',dict(tables={table:dict(domain=domain,primary_key=keys,fields=fields[table]) for domain,(table,keys) in DOMAINS.items() if table in defs},sql_executed=False))
    validation['counts']={k:len(v) for k,v in data.items() if isinstance(v,list)}|{'sources':len(sources),'provenance':len(prov)};b.writejson('validation.json',validation)
    print('candidate tables',len(defs),'SQL rows',sum(len(r) for r in rows.values()),'; SQL NOT executed')
if __name__=='__main__':main()
