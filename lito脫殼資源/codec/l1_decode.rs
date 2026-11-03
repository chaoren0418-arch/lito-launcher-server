mod resource_cipher;
use std::{fs,path::{Path,PathBuf},io};
fn decode(mode:&str,input:&str,output:&str)->io::Result<()> {
    let root=std::env::current_exe()?.parent().unwrap().parent().unwrap().canonicalize()?;
    let out=PathBuf::from(output);let parent=out.parent().unwrap().canonicalize()?;
    if !parent.starts_with(&root)||out.exists()&&out.is_symlink(){return Err(io::Error::other("Output must remain in owned task directory"));}
    let mut b=fs::read(input)?;if b.len()>16*1024*1024{return Err(io::Error::other("Input budget exceeded"));}
    match mode {"idx"=>{if b.len()<4{return Err(io::Error::other("Count truncated"));}resource_cipher::decode_payload_in_place(&mut b[4..]);},"payload"=>resource_cipher::decode_payload_in_place(&mut b),_=>return Err(io::Error::other("Unknown mode"))};fs::write(out,b)
}
fn main()->io::Result<()> {
    let a:Vec<String>=std::env::args().collect();
    if a.len()==3&&a[1]=="batch"{for row in fs::read_to_string(&a[2])?.lines(){let x:Vec<&str>=row.split('\t').collect();if x.len()!=3{return Err(io::Error::other("Invalid batch row"));}decode(x[0],x[1],x[2])?;}Ok(())}
    else if a.len()==4{decode(&a[1],&a[2],&a[3])}
    else {Err(io::Error::other("Usage: l1_decode idx|payload INPUT OUTPUT, or batch TSV"))}
}