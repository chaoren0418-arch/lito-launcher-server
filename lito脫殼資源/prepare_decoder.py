"""Freeze the existing consumer's L1 codec into this task's independently callable CLI."""
from audit import *
import subprocess
source=ROOT.parent/'lito重建版'/'LiTo381-Modern'/'src'/'modern_lito'/'resource_cipher.rs'
dest=ROOT/'codec'/'resource_cipher.rs';dest.parent.mkdir(exist_ok=True);dest.write_bytes(source.read_bytes())
write('codec/source-provenance.json',{'source':record(source),'copy':record(dest),'copy_exact':source.read_bytes()==dest.read_bytes(),'reason':'Reuse existing source-qualified L1 format codec. No consumer build/tests or original processes run.'})
wrapper=r'''mod resource_cipher;
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
}'''
(ROOT/'codec'/'l1_decode.rs').write_text(wrapper,encoding='utf-8')
r=subprocess.run(['rustc','--edition=2021','-O',str(ROOT/'codec'/'l1_decode.rs'),'-o',str(ROOT/'codec'/'l1_decode.exe')],cwd=ROOT,capture_output=True)
(ROOT/'codec'/'build.stdout.log').write_bytes(r.stdout);(ROOT/'codec'/'build.stderr.log').write_bytes(r.stderr)
write('codec/build.json',{'compiler':subprocess.run(['rustc','--version'],capture_output=True,text=True).stdout.strip(),'command':'rustc --edition=2021 -O codec/l1_decode.rs -o codec/l1_decode.exe','exit_code':r.returncode,'executable':record(ROOT/'codec'/'l1_decode.exe') if r.returncode==0 else None,'dependency':record(dest),'source':record(ROOT/'codec'/'l1_decode.rs')})
if r.returncode:raise RuntimeError(r.stderr.decode(errors='replace'))
print('Owned L1 CLI built successfully')
