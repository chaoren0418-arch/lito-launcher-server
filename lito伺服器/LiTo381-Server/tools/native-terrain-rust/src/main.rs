//! Read-only format experiment against unchanged Modern source modules.
//! This does not register a Native resource namespace in the client.
#![allow(dead_code)]
#[path = "D:/lineage開發/3-lito開發/lito重建版/LiTo381-Modern/src/modern_lito/resource_cipher.rs"]
mod resource_cipher;
#[path = "D:/lineage開發/3-lito開發/lito重建版/LiTo381-Modern/src/modern_lito/resources.rs"]
mod resources;
#[path = "D:/lineage開發/3-lito開發/lito重建版/LiTo381-Modern/src/modern_lito/map.rs"]
mod map;
#[path = "D:/lineage開發/3-lito開發/lito重建版/LiTo381-Modern/src/modern_lito/world_map.rs"]
mod world_map;
#[path = "D:/lineage開發/3-lito開發/lito重建版/LiTo381-Modern/src/modern_lito/tile.rs"]
mod tile;
use std::{collections::{BTreeMap, BTreeSet}, fs, io::{Read,Seek,SeekFrom}, path::Path, sync::atomic::AtomicBool};
use sha2::{Digest, Sha256};
use serde_json::json;
fn hex(bytes: &[u8]) -> String { bytes.iter().map(|b|format!("{b:02x}")).collect() }
fn readonly(path:&Path)->Result<fs::File,Box<dyn std::error::Error>> {
    resources::reject_reparse_chain(path)?;
    let mut options=fs::OpenOptions::new(); options.read(true);
    #[cfg(windows)] { use std::os::windows::fs::OpenOptionsExt; options.share_mode(1); }
    Ok(options.open(path)?)
}
fn native_tiles(root:&Path,shared:&Path,names:&BTreeSet<String>)->Result<serde_json::Value,Box<dyn std::error::Error>> {
    // Explicit Native archive adapter experiment. No SharedArchive fallback or archive rename.
    let mut idx=Vec::new();readonly(&root.join("LTtile.idx"))?.read_to_end(&mut idx)?;
    if idx.len()<4{return Err("truncated Native index".into());}
    let count=u32::from_le_bytes(idx[..4].try_into()?);
    if count>250_000||idx.len()!=4+count as usize*28{return Err("Native index is not bounded plain old28".into());}
    let mut pak=readonly(&root.join("LTtile.pak"))?;let size=pak.metadata()?.len();let mut entries=BTreeMap::new();
    for record in idx[4..].chunks_exact(28) {
        let raw=&record[4..24];let end=raw.iter().position(|b|*b==0).unwrap_or(20);
        if raw[end..].iter().any(|b|*b!=0){return Err("nonzero Native index name padding".into());}
        let name=std::str::from_utf8(&raw[..end])?.to_ascii_lowercase();
        if name.is_empty()||!name.bytes().all(|b|b.is_ascii_alphanumeric()||b==b'.'||b==b'_'||b==b'-'){return Err("invalid Native archive name".into());}
        let offset=u32::from_le_bytes(record[..4].try_into()?) as u64;let len=u32::from_le_bytes(record[24..].try_into()?) as u64;
        if len>64*1024*1024||offset.checked_add(len).ok_or("entry overflow")?>size||entries.insert(name,(offset,len)).is_some(){return Err("invalid Native archive entry bounds/duplicate".into());}
    }
    let mut shared_archive=resources::SharedArchive::open(shared,resources::SharedArchiveKind::Tile)?;let mut rows=Vec::new();
    for name in names {
        let &(offset,len)=entries.get(name).ok_or("Native Tile dependency absent")?;
        pak.seek(SeekFrom::Start(offset))?;let mut bytes=vec![0;len as usize];pak.read_exact(&mut bytes)?;
        let digest=hex(&Sha256::digest(&bytes));
        let decoded=match tile::decode_classic_til(&bytes){Ok(d)=>json!({"status":"passed","subtiles":d.tiles.len()}),Err(e)=>json!({"status":"failed","error":format!("{e:?}")})};
        let shared_sha=shared_archive.read_entry(name).ok().map(|b|hex(&Sha256::digest(&b)));
        rows.push(json!({"name":name,"native_offset":offset,"bytes":len,"native_payload_sha256":digest,
            "unchanged_classic_til_decoder":decoded,"shared_same_name_sha256":shared_sha,"payload_equal":shared_sha.as_ref()==Some(&digest)}));
    }
    Ok(json!({"namespace":"NATIVE_LITO_LTTILE_V1","index_format":"observed plain old28; bounded exact count/name/bounds/duplicate checks passed", "index_sha256":hex(&Sha256::digest(&idx)),"index_records":count,"pak_bytes":size,"dependencies":rows,
        "scope":"raw Native LTtile entries via standalone adapter and unchanged pure TIL decoder; actual Rust SharedArchive has no NativeTile kind; no compositor acceptance"}))
}
fn main() -> Result<(), Box<dyn std::error::Error>> {
    let args: Vec<_> = std::env::args().collect();
    if args.len()==3 && args[1]=="--tile-repro" {
        let data=fs::read(&args[2])?;
        if data.len()!=40295 || hex(&Sha256::digest(&data))!="ab2a6c4a3b490c058589df36ca8dac7a57ac4edefdd606e7d69290a213656a31" {return Err("exact Native 1026.til failure payload required".into());}
        let error=tile::decode_classic_til(&data).err().ok_or("expected decoder rejection absent")?;
        println!("{}",json!({"scope":"minimal exact Native tile decoder failure reproduction","bytes":data.len(),"sha256":hex(&Sha256::digest(&data)),"decode_success":false,"error":format!("{error:?}")}));
        return Ok(());
    }
    if args.len()!=3 { return Err("pinned Native properties and owned output required".into()); }
    let config = fs::read_to_string(&args[1])?;
    let p: BTreeMap<_,_> = config.lines().filter_map(|line|line.split_once('=')).collect();
    if p["source.namespace"]!="NATIVE_LITO_LTMAP_V1" { return Err("explicit namespace required".into()); }
    let root=Path::new(p["source.root"]); let cancel=AtomicBool::new(false); let mut rows=Vec::new();
    for id in p["maps"].split(',').map(str::parse::<u32>) {
        let id=id?; let mut inputs=Vec::new(); let mut sector_rows=Vec::new(); let mut tiles=BTreeSet::new();
        for token in p[format!("map.{id}.sectors").as_str()].split(',') {
            if token.len()!=8 || !token.bytes().all(|b|b.is_ascii_hexdigit()) { return Err("invalid sector key".into()); }
            let key=resources::SharedMapSectorKey{map_id:id,sector_x:u16::from_str_radix(&token[..4],16)?,sector_y:u16::from_str_radix(&token[4..],16)?};
            let path=root.join(id.to_string()).join(key.file_name());
            resources::reject_reparse_chain(&path)?;
            let pin=p[format!("map.{id}.{token}").as_str()].split_once(':').ok_or("invalid pin")?;
            let expected=pin.0.parse::<u64>()?;
            if expected>resources::MAX_SHARED_MAP_SECTOR_BYTES || fs::metadata(&path)?.len()!=expected { return Err("pinned size changed".into()); }
            let bytes=fs::read(&path)?;
            if bytes.len() as u64!=expected || !hex(&Sha256::digest(&bytes)).eq_ignore_ascii_case(pin.1) { return Err("pinned SHA changed".into()); }
            let s=map::parse_s32(&bytes)?;
            for t in &s.floor { tiles.insert(t.entry_name()); }
            for t in &s.overlays { tiles.insert(t.tile.entry_name()); }
            for group in &s.objects { for t in &group.tiles { if let Some(t)=t.shared_tile_reference(){tiles.insert(t.entry_name());} } }
            for &t in &s.tile_dependencies { if t>=0 {tiles.insert(format!("{t}.til"));} }
            sector_rows.push(json!({"sector":token,"raw_sha256":hex(&Sha256::digest(&bytes)),"bytes":bytes.len(),
                "layers":s.layer_ranges.iter().map(|r|r.as_ref().map(|r|json!({"start":r.start,"end":r.end}))).collect::<Vec<_>>(),
                "floor":s.floor.len(),"overlays":s.overlays.len(),"object_groups":s.objects.len(),"object_tiles":s.object_tile_count(),
                "opacity":s.opacity.len(),"dependencies":s.tile_dependencies.len(),"portals":s.portals.len(),"effects":s.effects.len(),
                "unknown_tail_bytes":s.unknown_trailing.len(),"negative_object_tiles":s.objects.iter().flat_map(|g|&g.tiles).filter(|t|t.tile_id<0).count(),
                "effect_records":s.effects.iter().map(|e|json!({"sprite_id":e.sprite_id,"x":e.x,"y":e.y,"extended":e.extended})).collect::<Vec<_>>()}));
            inputs.push(world_map::SectorInput{key,bytes});
        }
        let world=world_map::WorldMap::from_sector_bytes(inputs,&cancel)?;
        let rejection=world_map::WorldMap::load_shared(root,&world.bindings(),&cancel).err().ok_or("unexpected Shared loader admission")?;
        let mut standing=0;
        for binding in world.bindings(){let (x,y)=binding.key.world_origin();for yy in y..y+64 {for xx in x..x+64 {if world.standing(world_map::WorldCell{x:xx,y:yy}){standing+=1;}}}}
        let art=if id==564{Some(native_tiles(root.parent().ok_or("Native parent")?,root.parent().and_then(Path::parent).ok_or("Shared parent")?,&tiles)?)}else{None};
        rows.push(json!({"map":id,"canonical_sha256":hex(&world.canonical_digest()),"standable_cells":standing,"native_tile_audit":art,
            "existing_shared_loader":format!("{rejection:?}"),"sectors":sector_rows,"tile_entry_names":tiles,
            "tile_reference_archive_kind":"SharedArchiveKind::Tile (current behavior; NativeTile routing absent)"}));
    }
    fs::write(&args[2],serde_json::to_vec_pretty(&json!({"namespace":p["source.namespace"],"scope":"unchanged Rust pure parser and canonical geometry; existing Shared namespace rejection; no rendering or gameplay acceptance","maps":rows,"gui_acceptance":false}))?)?;
    println!("[native-terrain-rust] pinned formats and canonical geometry recorded; no runtime namespace admission");
    Ok(())
}
