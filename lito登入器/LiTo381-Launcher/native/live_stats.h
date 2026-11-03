#pragma once
#include <string>
#include <array>
#include <charconv>
#include <algorithm>
#include <cstring>
#include <cstdint>
struct LiToLiveStats { std::string source,raw,display; std::array<int,14> values{}; };
inline bool ParseLiToLiveStats(const char* text,size_t length,LiToLiveStats& out){
 if(length>512||length<10||std::memcmp(text,"LITOSTAT1:",10))return false;
 std::string s(text+10,length-10);size_t at=0;
 for(auto target:{&out.source,&out.raw,&out.display}){
  size_t end=s.find(':',at);if(end==std::string::npos||end==at||(end-at)%2||end-at>126)return false;
  target->clear();for(size_t i=at;i<end;i+=2){unsigned b{};auto r=std::from_chars(s.data()+i,s.data()+i+2,b,16);if(r.ec!=std::errc{}||r.ptr!=s.data()+i+2||!b)return false;target->push_back(static_cast<char>(b));}at=end+1;
 }
 if(out.raw.size()>27||out.display.size()>27)return false;
 for(size_t i=0;i<14;i++){
  size_t end=i==13?s.size():s.find(',',at);if(end==std::string::npos||end==at)return false;
  auto r=std::from_chars(s.data()+at,s.data()+end,out.values[i]);if(r.ec!=std::errc{}||r.ptr!=s.data()+end||(i<9&&out.values[i]<0))return false;at=end+1;
 }
 return static_cast<int64_t>(out.values[1])+out.values[2]==out.values[0]&&out.values[3]<=out.values[2];
}
inline void ApplyLiToLiveStats(unsigned char* row,unsigned char* profile,const LiToLiveStats& s){
 auto d=[](unsigned char* p,int v){std::memcpy(p,&v,4);};auto w=[](unsigned char* p,int v){uint16_t x=static_cast<uint16_t>(std::min(v,65535));std::memcpy(p,&x,2);};
 if(row){
  const int stats[]={0x60,0x64,0x68,0x58};for(int i=0;i<4;i++)d(row+stats[i],s.values[i]);
  const int ranks[]={0x48,0x44,0x50,0x4c,0x54},moves[]={0x84,0x80,0x8c,0x88,0x90};
  for(int i=0;i<5;i++){d(row+ranks[i],s.values[4+i]);d(row+moves[i],s.values[9+i]);}
 }
 if(profile){
  for(int i=0;i<4;i++)w(profile+0x38+i*2,s.values[i]);
  profile[0x42]=s.values[0]?static_cast<unsigned char>(static_cast<int64_t>(s.values[1])*100/s.values[0]):0;
  const int ranks[]={0x8c,0x94,0x98,0x90,0x9c},moves[]={0xa0,0xa8,0xac,0xa4,0xb0};
  for(int i=0;i<5;i++){d(profile+ranks[i],s.values[4+i]);d(profile+moves[i],s.values[9+i]);}
 }
}
